package org.hydra2s.noire.objects;

//

import org.hydra2s.noire.descriptors.MemoryAllocationCInfo;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.stream.IntStream;

import static org.lwjgl.system.MemoryUtil.memAllocLong;
import static org.lwjgl.system.MemoryUtil.memAllocPointer;
import static org.lwjgl.vulkan.VK10.VK_QUEUE_FAMILY_IGNORED;
import static org.lwjgl.vulkan.VK10.VK_WHOLE_SIZE;
import static org.lwjgl.vulkan.VK13.*;

// currently, is a part of device memory object
// in the future, probably, planned to use modular system with buffer or image, instead of inheritance
// not sure about that
public class MemoryAllocationObj extends BasicObj {
    public Object allocationInfo = null;
    public long allocationInfoAddress = 0L;

    // important for VMA or other allocators
    public long memoryOffset = 0L;
    public long memorySize = 0L;

    // probably, will be replaced by modules
    // not sure about that
    //public BufferObj bufferObj = null;
    //public ImageObj imageObj = null;

    //
    public PointerBuffer deviceMemory = memAllocPointer(1);

    //
    public MemoryAllocationObj(Handle base, Handle handle) {
        super(base, handle);
    }

    //
    public MemoryAllocationObj(Handle base, MemoryAllocationCInfo cInfo) {
        super(base, cInfo);

        //
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get());
        var physicalDeviceObj = (PhysicalDeviceObj)BasicObj.globalHandleMap.get(deviceObj.base.get());
        var deviceMemoryObj = (MemoryAllocatorObj.DeviceMemoryObj)deviceObj.handleMap.get(new Handle("DeviceMemory", this.deviceMemory));

        //
        this.handle = new Handle("MemoryAllocation", MemoryUtil.memAddress(memAllocLong(1)));
        deviceObj.handleMap.put(this.handle, this);
    }

    //
    public ByteBuffer map(long byteLength, long byteOffset) {
        //
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get());
        var physicalDeviceObj = (PhysicalDeviceObj)BasicObj.globalHandleMap.get(deviceObj.base.get());
        var deviceMemoryObj = (MemoryAllocatorObj.DeviceMemoryObj)deviceObj.handleMap.get(new Handle("DeviceMemory", this.deviceMemory));

        // TODO: add support for synchronize to host
        return deviceMemoryObj.map(byteLength, this.memoryOffset + byteOffset);
    }

    public void unmap() {
        //
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get());
        var physicalDeviceObj = (PhysicalDeviceObj)BasicObj.globalHandleMap.get(deviceObj.base.get());
        var deviceMemoryObj = (MemoryAllocatorObj.DeviceMemoryObj)deviceObj.handleMap.get(new Handle("DeviceMemory", this.deviceMemory));

        deviceMemoryObj.unmap();
    }

    public PointerBuffer getWin32Handle() {
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get());
        var physicalDeviceObj = (PhysicalDeviceObj)BasicObj.globalHandleMap.get(deviceObj.base.get());
        var deviceMemoryObj = (MemoryAllocatorObj.DeviceMemoryObj)deviceObj.handleMap.get(new Handle("DeviceMemory", this.deviceMemory));
        return deviceMemoryObj.Win32Handle;
    }

    public IntBuffer getFdHandle() {
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get());
        var physicalDeviceObj = (PhysicalDeviceObj)BasicObj.globalHandleMap.get(deviceObj.base.get());
        var deviceMemoryObj = (MemoryAllocatorObj.DeviceMemoryObj)deviceObj.handleMap.get(new Handle("DeviceMemory", this.deviceMemory));
        return deviceMemoryObj.FdHandle;
    }

    // TODO: special support for ImageView
    static public void cmdCopyImageToImage(VkCommandBuffer cmdBuf, long srcImage, long dstImage, int srcImageLayout, int dstImageLayout, VkImageCopy2.Buffer regions) {
        //
        var readMemoryBarrierTemplate = VkImageMemoryBarrier2.calloc()
            .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER_2)
            .srcStageMask(VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT | VK_PIPELINE_STAGE_2_HOST_BIT)
            .srcAccessMask(VK_ACCESS_2_TRANSFER_READ_BIT | VK_ACCESS_2_TRANSFER_WRITE_BIT | VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
            .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
            .dstAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
            .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);

        //
        var writeMemoryBarrierTemplate = VkImageMemoryBarrier2.calloc()
            .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER_2)
            .srcStageMask(VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT)
            .srcAccessMask( VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
            .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
            .dstAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
            .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);

        //
        var imageMemoryBarrier = VkImageMemoryBarrier2.calloc(regions.remaining()*2);

        // TODO: support a correct buffer size
        IntStream.range(0, regions.remaining()).forEachOrdered((I)->{
            imageMemoryBarrier.put(I*2+0, writeMemoryBarrierTemplate).image(dstImage).oldLayout(dstImageLayout).newLayout(dstImageLayout).subresourceRange(VkImageSubresourceRange.calloc()
                .aspectMask(regions.get(I).dstSubresource().aspectMask())
                .baseArrayLayer(regions.get(I).dstSubresource().baseArrayLayer())
                .baseMipLevel(regions.get(I).dstSubresource().mipLevel())
                .layerCount(regions.get(I).dstSubresource().layerCount())
                .levelCount(1)
            );
            imageMemoryBarrier.put(I*2+1, readMemoryBarrierTemplate).image(srcImage).oldLayout(srcImageLayout).newLayout(srcImageLayout).subresourceRange(VkImageSubresourceRange.calloc()
                .aspectMask(regions.get(I).srcSubresource().aspectMask())
                .baseArrayLayer(regions.get(I).srcSubresource().baseArrayLayer())
                .baseMipLevel(regions.get(I).srcSubresource().mipLevel())
                .layerCount(regions.get(I).srcSubresource().layerCount())
                .levelCount(1)
            );
        });

        //
        vkCmdCopyImage2(cmdBuf, VkCopyImageInfo2.calloc().sType(VK_STRUCTURE_TYPE_COPY_IMAGE_INFO_2).dstImage(dstImage).dstImageLayout(dstImageLayout).srcImage(srcImage).srcImageLayout(srcImageLayout).pRegions(regions));
        vkCmdPipelineBarrier2(cmdBuf, VkDependencyInfoKHR.calloc().sType(VK_STRUCTURE_TYPE_DEPENDENCY_INFO).pImageMemoryBarriers(imageMemoryBarrier));

        //
        //return this;
    }

    // TODO: special support for ImageView
    static public void cmdCopyImageToBuffer(VkCommandBuffer cmdBuf, long srcImage, long dstBuffer, int imageLayout, VkBufferImageCopy2.Buffer regions) {
        //
        var readMemoryBarrierTemplate = VkImageMemoryBarrier2.calloc()
            .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER_2)
            .srcStageMask(VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT | VK_PIPELINE_STAGE_2_HOST_BIT)
            .srcAccessMask(VK_ACCESS_2_TRANSFER_READ_BIT | VK_ACCESS_2_TRANSFER_WRITE_BIT | VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
            .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
            .dstAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
            .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);

        //
        var writeMemoryBarrierTemplate = VkBufferMemoryBarrier2.calloc()
            .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER_2)
            .srcStageMask(VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT)
            .srcAccessMask( VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
            .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
            .dstAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
            .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);

        //
        var imageMemoryBarrier = VkImageMemoryBarrier2.calloc(regions.remaining());
        var bufferMemoryBarrier = VkBufferMemoryBarrier2.calloc(regions.remaining());

        // TODO: support a correct buffer size
        IntStream.range(0, regions.remaining()).forEachOrdered((I)->{
            imageMemoryBarrier.put(I, readMemoryBarrierTemplate).image(srcImage).oldLayout(imageLayout).newLayout(imageLayout).subresourceRange(VkImageSubresourceRange.calloc()
                .aspectMask(regions.get(I).imageSubresource().aspectMask())
                .baseArrayLayer(regions.get(I).imageSubresource().baseArrayLayer())
                .baseMipLevel(regions.get(I).imageSubresource().mipLevel())
                .layerCount(regions.get(I).imageSubresource().layerCount())
                .levelCount(1)
            );
            bufferMemoryBarrier.put(I, writeMemoryBarrierTemplate).offset(regions.bufferOffset()).size(VK_WHOLE_SIZE).buffer(dstBuffer);
        });

        //
        vkCmdCopyImageToBuffer2(cmdBuf, VkCopyImageToBufferInfo2.calloc().sType(VK_STRUCTURE_TYPE_COPY_IMAGE_TO_BUFFER_INFO_2).srcImage(srcImage).srcImageLayout(imageLayout).dstBuffer(dstBuffer).pRegions(regions));
        vkCmdPipelineBarrier2(cmdBuf, VkDependencyInfoKHR.calloc().sType(VK_STRUCTURE_TYPE_DEPENDENCY_INFO).pBufferMemoryBarriers(bufferMemoryBarrier).pImageMemoryBarriers(imageMemoryBarrier));

        //
        //return this;
    }

    // TODO: special support for ImageView
    static public void cmdCopyBufferToImage(VkCommandBuffer cmdBuf, long srcBuffer, long dstImage, int imageLayout, VkBufferImageCopy2.Buffer regions) {
        //
        var readMemoryBarrierTemplate = VkBufferMemoryBarrier2.calloc()
            .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER_2)
            .srcStageMask(VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT | VK_PIPELINE_STAGE_2_HOST_BIT)
            .srcAccessMask(VK_ACCESS_2_TRANSFER_READ_BIT | VK_ACCESS_2_TRANSFER_WRITE_BIT | VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
            .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
            .dstAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
            .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);

        //
        var writeMemoryBarrierTemplate = VkImageMemoryBarrier2.calloc()
            .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER_2)
            .srcStageMask(VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT)
            .srcAccessMask( VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
            .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
            .dstAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
            .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);

        //
        var imageMemoryBarrier = VkImageMemoryBarrier2.calloc(regions.remaining());
        var bufferMemoryBarrier = VkBufferMemoryBarrier2.calloc(regions.remaining());

        // TODO: support a correct buffer size
        IntStream.range(0, regions.remaining()).forEachOrdered((I)->{
            imageMemoryBarrier.put(I, writeMemoryBarrierTemplate).image(dstImage).oldLayout(imageLayout).newLayout(imageLayout).subresourceRange(VkImageSubresourceRange.calloc()
                .aspectMask(regions.get(I).imageSubresource().aspectMask())
                .baseArrayLayer(regions.get(I).imageSubresource().baseArrayLayer())
                .baseMipLevel(regions.get(I).imageSubresource().mipLevel())
                .layerCount(regions.get(I).imageSubresource().layerCount())
                .levelCount(1)
            );
            bufferMemoryBarrier.put(I, readMemoryBarrierTemplate).offset(regions.bufferOffset()).size(VK_WHOLE_SIZE).buffer(srcBuffer);
        });

        //
        vkCmdCopyBufferToImage2(cmdBuf, VkCopyBufferToImageInfo2.calloc().sType(VK_STRUCTURE_TYPE_COPY_BUFFER_TO_IMAGE_INFO_2).srcBuffer(srcBuffer).dstImage(dstImage).dstImageLayout(imageLayout).pRegions(regions));
        vkCmdPipelineBarrier2(cmdBuf, VkDependencyInfoKHR.calloc().sType(VK_STRUCTURE_TYPE_DEPENDENCY_INFO).pBufferMemoryBarriers(bufferMemoryBarrier).pImageMemoryBarriers(imageMemoryBarrier));

        //
        //return this;
    }

    //
    static public void cmdCopyBufferToBuffer(VkCommandBuffer cmdBuf, long srcBuffer, long dstBuffer, VkBufferCopy2.Buffer regions) {
        //
        var readMemoryBarrierTemplate = VkBufferMemoryBarrier2.calloc()
            .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER_2)
            .srcStageMask(VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT | VK_PIPELINE_STAGE_2_HOST_BIT)
            .srcAccessMask(VK_ACCESS_2_TRANSFER_READ_BIT | VK_ACCESS_2_TRANSFER_WRITE_BIT | VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
            .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
            .dstAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
            .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);

        //
        var writeMemoryBarrierTemplate = VkBufferMemoryBarrier2.calloc()
            .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER_2)
            .srcStageMask(VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT)
            .srcAccessMask( VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
            .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
            .dstAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
            .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);

        //
        var memoryBarriers = VkBufferMemoryBarrier2.calloc(regions.remaining()*2);
        IntStream.range(0, regions.remaining()).forEachOrdered((I)->{
            memoryBarriers.put(I*2+0, readMemoryBarrierTemplate).offset(regions.srcOffset()).size(regions.size()).buffer(srcBuffer);
            memoryBarriers.put(I*2+1, writeMemoryBarrierTemplate).offset(regions.dstOffset()).size(regions.size()).buffer(dstBuffer);
        });

        //
        vkCmdCopyBuffer2(cmdBuf, VkCopyBufferInfo2.calloc().sType(VK_STRUCTURE_TYPE_COPY_BUFFER_INFO_2).srcBuffer(srcBuffer).dstBuffer(dstBuffer).pRegions(regions));
        vkCmdPipelineBarrier2(cmdBuf, VkDependencyInfoKHR.calloc().sType(VK_STRUCTURE_TYPE_DEPENDENCY_INFO).pBufferMemoryBarriers(memoryBarriers));

        //
        //return this;
    }

}
