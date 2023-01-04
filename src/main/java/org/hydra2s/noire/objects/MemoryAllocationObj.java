package org.hydra2s.noire.objects;

//
import com.lodborg.intervaltree.Interval;
import com.lodborg.intervaltree.LongInterval;
import org.hydra2s.noire.descriptors.MemoryAllocationCInfo;
import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.stream.IntStream;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.EXTImage2dViewOf3d.VK_IMAGE_CREATE_2D_VIEW_COMPATIBLE_BIT_EXT;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK11.vkGetBufferMemoryRequirements2;
import static org.lwjgl.vulkan.VK11.vkGetImageMemoryRequirements2;
import static org.lwjgl.vulkan.VK12.VK_BUFFER_USAGE_SHADER_DEVICE_ADDRESS_BIT;
import static org.lwjgl.vulkan.VK12.vkGetBufferDeviceAddress;
import static org.lwjgl.vulkan.VK13.*;

// currently, is a part of device memory object
// in the future, probably, planned to use modular system with buffer or image, instead of inheritance
// not sure about that
public class MemoryAllocationObj extends BasicObj {

    // important for VMA or other allocators
    public long memoryOffset = 0L;

    // reserved for future usage
    //public long memorySize = 0L;

    // reserved for VMA, or any other
    // reserved due unable to store as main handle
    // as handle already used vulkan buffer or image
    // in the future, probably, planned to use modular system with buffer or image, instead of inheritance
    // not sure about that
    public long allocationHandle = 0L;
    public Object allocationInfo = null;
    public long allocationInfoAddress = 0L;

    // probably, will be replaced by modules
    // not sure about that
    public boolean isBuffer = false;
    public boolean isImage = false;

    //
    public PointerBuffer deviceMemory = memAllocPointer(1);
    public VkMemoryRequirements2 memoryRequirements2 = null;

    //
    public MemoryAllocationObj(Handle base, Handle handle) {
        super(base, handle);
    }
    public MemoryAllocationObj(Handle base, MemoryAllocationCInfo handle) {
        super(base, handle);
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

        // TODO: for OpenGL needs semaphores
        /*if (this.isBuffer) {
            // TODO: fix queue family indices support
            deviceObj.submitOnce(deviceObj.getCommandPool(0), new BasicCInfo.SubmitCmd() {{
                queue = deviceObj.getQueue(0, 0);

            }}, (cmdBuf) -> {
                ((BufferObj)this).cmdSynchronizeFromHost(cmdBuf);
                return null;
            });
        }*/
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
    public MemoryAllocationObj cmdCopyImageToImage(VkCommandBuffer cmdBuf, long image, int srcImageLayout, int dstImageLayout, VkImageCopy2.Buffer regions) {
        //
        var readMemoryBarrierTemplate = VkImageMemoryBarrier2.calloc()
            .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER_2)
            .srcStageMask(VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT)
            .srcAccessMask(0)
            .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
            .dstAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
            .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);

        //
        var writeMemoryBarrierTemplate = VkImageMemoryBarrier2.calloc()
            .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER_2)
            .srcStageMask(VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT)
            .srcAccessMask(0)
            .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
            .dstAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
            .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);

        //
        var dstImage = image;
        var srcImage = this.handle.get();

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
        return this;
    }

    // TODO: special support for ImageView
    public MemoryAllocationObj cmdCopyImageToBuffer(VkCommandBuffer cmdBuf, long buffer, int imageLayout, VkBufferImageCopy2.Buffer regions) {
        //
        var readMemoryBarrierTemplate = VkImageMemoryBarrier2.calloc()
            .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER_2)
            .srcStageMask(VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT)
            .srcAccessMask(0)
            .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
            .dstAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
            .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);

        //
        var writeMemoryBarrierTemplate = VkBufferMemoryBarrier2.calloc()
            .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER_2)
            .srcStageMask(VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT)
            .srcAccessMask(0)
            .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
            .dstAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
            .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);

        //
        var dstBuffer = buffer;
        var srcImage = this.handle.get();

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
        return this;
    }

    // TODO: special support for ImageView
    public MemoryAllocationObj cmdCopyBufferToImage(VkCommandBuffer cmdBuf, long image, int imageLayout, VkBufferImageCopy2.Buffer regions) {
        //
        var readMemoryBarrierTemplate = VkBufferMemoryBarrier2.calloc()
            .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER_2)
            .srcStageMask(VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT)
            .srcAccessMask(0)
            .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
            .dstAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
            .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);

        //
        var writeMemoryBarrierTemplate = VkImageMemoryBarrier2.calloc()
            .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER_2)
            .srcStageMask(VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT)
            .srcAccessMask(0)
            .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
            .dstAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
            .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);

        //
        var srcBuffer = this.handle.get();
        var dstImage = image;

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
        return this;
    }

    //
    public MemoryAllocationObj cmdCopyBufferToBuffer(VkCommandBuffer cmdBuf, long buffer, VkBufferCopy2.Buffer regions) {
        //
        var writeMemoryBarrierTemplate = VkBufferMemoryBarrier2.calloc()
            .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER_2)
            .srcStageMask(VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT)
            .srcAccessMask(VK_ACCESS_2_TRANSFER_READ_BIT | VK_ACCESS_2_TRANSFER_WRITE_BIT | VK_ACCESS_2_HOST_READ_BIT | VK_ACCESS_2_HOST_WRITE_BIT)
            .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
            .dstAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
            .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);

        //
        var readMemoryBarrierTemplate = VkBufferMemoryBarrier2.calloc()
            .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER_2)
            .srcStageMask(VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT)
            .srcAccessMask(VK_ACCESS_2_TRANSFER_READ_BIT | VK_ACCESS_2_TRANSFER_WRITE_BIT | VK_ACCESS_2_HOST_READ_BIT | VK_ACCESS_2_HOST_WRITE_BIT)
            .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
            .dstAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
            .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);

        //
        var srcBuffer = this.handle.get();
        var dstBuffer = buffer;

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
        return this;
    }

    //
    // DEFAULT IS ResizableBAR!
    // probably, will be replaced by modules for allocation
    // not sure about that
    static public class BufferObj extends MemoryAllocationObj {
        public VkBufferCreateInfo createInfo = null;
        public long deviceAddress = 0L;

        public BufferObj(Handle base, Handle handle) {
            super(base, handle); this.isBuffer = true;
        }

        public BufferObj(Handle base, MemoryAllocationCInfo.BufferCInfo cInfo) {
            super(base, cInfo);
            this.isBuffer = true;

            //
            var deviceObj = (DeviceObj) BasicObj.globalHandleMap.get(this.base.get());
            var physicalDeviceObj = (PhysicalDeviceObj) BasicObj.globalHandleMap.get(deviceObj.base.get());

            //
            this.createInfo = VkBufferCreateInfo.calloc()
                .pNext(VkExternalMemoryBufferCreateInfo.calloc()
                    .pNext(0L)
                    .sType(VK_STRUCTURE_TYPE_EXTERNAL_MEMORY_BUFFER_CREATE_INFO)
                    .handleTypes(VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_WIN32_BIT).address()
                )
                .flags(0)
                .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                .size(Math.max(cInfo.size, 0L))
                .usage(cInfo.usage | VK_BUFFER_USAGE_SHADER_DEVICE_ADDRESS_BIT | VK_BUFFER_USAGE_TRANSFER_SRC_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT)
                .sharingMode(VK_SHARING_MODE_EXCLUSIVE)
                .pQueueFamilyIndices(null);

            // TODO: direct create with direct allocation
            int status = VK_NOT_READY;
            if (cInfo.buffer == null || cInfo.buffer.get(0) == 0) {
                status = vkCreateBuffer(deviceObj.device, this.createInfo, null, memLongBuffer(memAddress((this.handle = new Handle("Buffer")).ptr(), 0), 1));
            } else {
                this.handle = new Handle("Buffer", cInfo.buffer.get(0));
            }
            deviceObj.handleMap.put(this.handle, this);

            //
            vkGetBufferMemoryRequirements2(
                    deviceObj.device,
                    VkBufferMemoryRequirementsInfo2.calloc()
                            .pNext(0L)
                            .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_REQUIREMENTS_INFO_2)
                            .buffer(this.handle.get()),
                    this.memoryRequirements2 = VkMemoryRequirements2.calloc()
                            .sType(VK_STRUCTURE_TYPE_MEMORY_REQUIREMENTS_2)
                            .pNext(0L)
            );

            // TODO: direct create with direct allocation
            if (cInfo.memoryAllocator != 0) {
                var memoryAllocatorObj = (MemoryAllocatorObj) BasicObj.globalHandleMap.get(cInfo.memoryAllocator);
                memoryAllocatorObj.allocateMemory(cInfo, this);
            }
        }

        //
        public long getDeviceAddress() {
            var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get());
            if (this.deviceAddress == 0) {
                this.deviceAddress = vkGetBufferDeviceAddress(deviceObj.device, VkBufferDeviceAddressInfo.calloc().pNext(0L).sType(VK_STRUCTURE_TYPE_BUFFER_DEVICE_ADDRESS_INFO).buffer(this.handle.get()));
                deviceObj.addressMap.add(new LongInterval(this.deviceAddress, this.deviceAddress + this.createInfo.size(), Interval.Bounded.CLOSED));
                deviceObj.rootMap.put(this.deviceAddress, this.handle.get());
            }
            return this.deviceAddress;
        }

        // for `map()` or copy operations
        // Resizable BAR!
        public BufferObj cmdSynchronizeFromHost(VkCommandBuffer cmdBuf) {
            // for `map()` or copy operations
            var bufferMemoryBarrier = VkBufferMemoryBarrier2.calloc(1)
                .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER_2)
                .srcStageMask(VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT | VK_PIPELINE_STAGE_2_HOST_BIT)
                .srcAccessMask(VK_ACCESS_2_TRANSFER_READ_BIT | VK_ACCESS_2_TRANSFER_WRITE_BIT | VK_ACCESS_2_HOST_READ_BIT | VK_ACCESS_2_HOST_WRITE_BIT)
                .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
                .dstAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .buffer(this.handle.get())
                .offset(0L)
                .size(VK_WHOLE_SIZE); // TODO: support partial synchronization
            vkCmdPipelineBarrier2(cmdBuf, VkDependencyInfoKHR.calloc().sType(VK_STRUCTURE_TYPE_DEPENDENCY_INFO).pBufferMemoryBarriers(bufferMemoryBarrier));

            return this;
        }
    }

    //
    // probably, will be replaced by modules for allocation
    // not sure about that
    static public class ImageObj extends MemoryAllocationObj {
        public VkImageCreateInfo createInfo = null;
        public ImageObj(Handle base, Handle handle) {
            super(base, handle); this.isImage = true;
        }


        public ImageObj(Handle base, MemoryAllocationCInfo.ImageCInfo cInfo) {
            super(base, cInfo);
            this.isImage = true;

            //
            var deviceObj = (DeviceObj) BasicObj.globalHandleMap.get(this.base.get());
            var physicalDeviceObj = (PhysicalDeviceObj) BasicObj.globalHandleMap.get(deviceObj.base.get());
            var extent = cInfo.extent3D;
            var imageType = cInfo.extent3D.depth() > 1 ? VK_IMAGE_TYPE_3D : (cInfo.extent3D.height() > 1 ? VK_IMAGE_TYPE_2D : VK_IMAGE_TYPE_1D);
            var arrayLayers = Math.max(cInfo.arrayLayers, 1);

            //
            this.createInfo = VkImageCreateInfo.calloc()
                .pNext(VkExternalMemoryImageCreateInfo.calloc()
                    .pNext(0L)
                    .sType(VK_STRUCTURE_TYPE_EXTERNAL_MEMORY_IMAGE_CREATE_INFO)
                    .handleTypes(VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_WIN32_BIT )
                    .address())
                .sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
                .flags(VK_IMAGE_CREATE_2D_VIEW_COMPATIBLE_BIT_EXT |
                    (((imageType == VK_IMAGE_TYPE_3D)) ? VK_IMAGE_CREATE_2D_ARRAY_COMPATIBLE_BIT : 0) |
                    (((arrayLayers % 6) == 0 && extent.height() == extent.width()) ? VK_IMAGE_CREATE_CUBE_COMPATIBLE_BIT : 0))
                .extent(cInfo.extent3D)
                .imageType(imageType)
                .usage(cInfo.usage | VK_IMAGE_USAGE_TRANSFER_SRC_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT)
                .mipLevels(cInfo.mipLevels)
                .arrayLayers(cInfo.arrayLayers)
                .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                .sharingMode(VK_SHARING_MODE_EXCLUSIVE)
                .samples(cInfo.samples)
                .format(cInfo.format)
                .tiling(cInfo.tiling);

            // TODO: direct create with direct allocation
            int status = VK_NOT_READY;
            if (cInfo.image == null || cInfo.image.get(0) == 0) {
                status = vkCreateImage(deviceObj.device, this.createInfo, null, memLongBuffer(memAddress(cInfo.image = (this.handle = new Handle("Image")).ptr()), 1));
            } else {
                this.handle = new Handle("Image", cInfo.image.get(0));
            }
            deviceObj.handleMap.put(this.handle, this);

            //
            vkGetImageMemoryRequirements2(
                    deviceObj.device,
                    VkImageMemoryRequirementsInfo2.calloc()
                            .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_REQUIREMENTS_INFO_2)
                            .pNext(0L)
                            .image(this.handle.get()),
                    this.memoryRequirements2 = VkMemoryRequirements2.calloc()
                            .sType(VK_STRUCTURE_TYPE_MEMORY_REQUIREMENTS_2)
                            .pNext(0L)
            );

            // TODO: direct create with direct allocation
            if (cInfo.memoryAllocator != 0) {
                var memoryAllocatorObj = (MemoryAllocatorObj) BasicObj.globalHandleMap.get(cInfo.memoryAllocator);
                memoryAllocatorObj.allocateMemory(cInfo, this);
            }
        }

        // TODO: special support for ImageView
        public ImageObj cmdTransitionBarrier(VkCommandBuffer cmdBuf, int oldLayout, int newLayout, VkImageSubresourceRange subresourceRange) {
            // TODO: correct stage and access per every imageLayout, it should increase some FPS
            var memoryBarrier = VkImageMemoryBarrier2.calloc(1)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER_2)
                    .srcStageMask(VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT)
                    .srcAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
                    .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
                    .dstAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
                    .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .oldLayout(oldLayout)
                    .newLayout(newLayout)
                    .subresourceRange(subresourceRange)
                    .image(this.handle.get());

            //
            vkCmdPipelineBarrier2(cmdBuf, VkDependencyInfoKHR.calloc().sType(VK_STRUCTURE_TYPE_DEPENDENCY_INFO).pImageMemoryBarriers(memoryBarrier));
            return this;
        }
    }

}
