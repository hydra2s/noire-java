package org.hydra2s.manhack.objects;

import org.hydra2s.manhack.descriptors.MemoryAllocationCInfo;
import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.*;

import java.util.stream.IntStream;

import static org.lwjgl.system.MemoryUtil.memAllocPointer;
import static org.lwjgl.vulkan.EXTImage2dViewOf3d.VK_IMAGE_CREATE_2D_VIEW_COMPATIBLE_BIT_EXT;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_TRANSFER_DST_BIT;
import static org.lwjgl.vulkan.VK11.vkGetBufferMemoryRequirements2;
import static org.lwjgl.vulkan.VK11.vkGetImageMemoryRequirements2;
import static org.lwjgl.vulkan.VK12.VK_BUFFER_USAGE_SHADER_DEVICE_ADDRESS_BIT;
import static org.lwjgl.vulkan.VK12.vkGetBufferDeviceAddress;
import static org.lwjgl.vulkan.VK13.*;

public class MemoryAllocationObj extends BasicObj {

    //
    public long memoryOffset = 0;
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



    // TODO: special support for ImageView
    public MemoryAllocationObj cmdCopyImageToImage(VkCommandBuffer cmdBuf, long image, int srcImageLayout, int dstImageLayout, VkImageCopy2.Buffer regions) {
        //
        var readMemoryBarrierTemplate = VkImageMemoryBarrier2.create()
                .srcStageMask(VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT)
                .srcAccessMask(0)
                .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
                .dstAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);

        //
        var writeMemoryBarrierTemplate = VkImageMemoryBarrier2.create()
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
        var imageMemoryBarrier = VkImageMemoryBarrier2.create(regions.capacity()*2);

        // TODO: support a correct buffer size
        IntStream.range(0, regions.capacity()).forEachOrdered((I)->{
            imageMemoryBarrier.put(I*2+0, writeMemoryBarrierTemplate).image(dstImage).oldLayout(dstImageLayout).newLayout(dstImageLayout).subresourceRange(VkImageSubresourceRange.create()
                    .aspectMask(regions.get(I).dstSubresource().aspectMask())
                    .baseArrayLayer(regions.get(I).dstSubresource().baseArrayLayer())
                    .baseMipLevel(regions.get(I).dstSubresource().mipLevel())
                    .layerCount(regions.get(I).dstSubresource().layerCount())
                    .levelCount(1)
            );
            imageMemoryBarrier.put(I*2+1, readMemoryBarrierTemplate).image(srcImage).oldLayout(srcImageLayout).newLayout(srcImageLayout).subresourceRange(VkImageSubresourceRange.create()
                    .aspectMask(regions.get(I).srcSubresource().aspectMask())
                    .baseArrayLayer(regions.get(I).srcSubresource().baseArrayLayer())
                    .baseMipLevel(regions.get(I).srcSubresource().mipLevel())
                    .layerCount(regions.get(I).srcSubresource().layerCount())
                    .levelCount(1)
            );
        });

        //
        vkCmdCopyImage2(cmdBuf, VkCopyImageInfo2.create().dstImage(dstImage).dstImageLayout(dstImageLayout).srcImage(srcImage).srcImageLayout(srcImageLayout).pRegions(regions));
        vkCmdPipelineBarrier2(cmdBuf, VkDependencyInfoKHR.create().pImageMemoryBarriers(imageMemoryBarrier));

        //
        return this;
    }

    // TODO: special support for ImageView
    public MemoryAllocationObj cmdCopyImageToBuffer(VkCommandBuffer cmdBuf, long buffer, int imageLayout, VkBufferImageCopy2.Buffer regions) {
        //
        var readMemoryBarrierTemplate = VkImageMemoryBarrier2.create()
                .srcStageMask(VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT)
                .srcAccessMask(0)
                .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
                .dstAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);

        //
        var writeMemoryBarrierTemplate = VkBufferMemoryBarrier2.create()
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
        var imageMemoryBarrier = VkImageMemoryBarrier2.create(regions.capacity());
        var bufferMemoryBarrier = VkBufferMemoryBarrier2.create(regions.capacity());

        // TODO: support a correct buffer size
        IntStream.range(0, regions.capacity()).forEachOrdered((I)->{
            imageMemoryBarrier.put(I, readMemoryBarrierTemplate).image(srcImage).oldLayout(imageLayout).newLayout(imageLayout).subresourceRange(VkImageSubresourceRange.create()
                    .aspectMask(regions.get(I).imageSubresource().aspectMask())
                    .baseArrayLayer(regions.get(I).imageSubresource().baseArrayLayer())
                    .baseMipLevel(regions.get(I).imageSubresource().mipLevel())
                    .layerCount(regions.get(I).imageSubresource().layerCount())
                    .levelCount(1)
            );
            bufferMemoryBarrier.put(I, writeMemoryBarrierTemplate).offset(regions.bufferOffset()).size(VK_WHOLE_SIZE).buffer(dstBuffer);
        });

        //
        vkCmdCopyImageToBuffer2(cmdBuf, VkCopyImageToBufferInfo2.create().srcImage(srcImage).srcImageLayout(imageLayout).dstBuffer(dstBuffer).pRegions(regions));
        vkCmdPipelineBarrier2(cmdBuf, VkDependencyInfoKHR.create().pBufferMemoryBarriers(bufferMemoryBarrier).pImageMemoryBarriers(imageMemoryBarrier));

        //
        return this;
    }

    // TODO: special support for ImageView
    public MemoryAllocationObj cmdCopyBufferToImage(VkCommandBuffer cmdBuf, long image, int imageLayout, VkBufferImageCopy2.Buffer regions) {
        //
        var readMemoryBarrierTemplate = VkBufferMemoryBarrier2.create()
                .srcStageMask(VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT)
                .srcAccessMask(0)
                .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
                .dstAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);

        //
        var writeMemoryBarrierTemplate = VkImageMemoryBarrier2.create()
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
        var imageMemoryBarrier = VkImageMemoryBarrier2.create(regions.capacity());
        var bufferMemoryBarrier = VkBufferMemoryBarrier2.create(regions.capacity());

        // TODO: support a correct buffer size
        IntStream.range(0, regions.capacity()).forEachOrdered((I)->{
            imageMemoryBarrier.put(I, writeMemoryBarrierTemplate).image(dstImage).oldLayout(imageLayout).newLayout(imageLayout).subresourceRange(VkImageSubresourceRange.create()
                .aspectMask(regions.get(I).imageSubresource().aspectMask())
                .baseArrayLayer(regions.get(I).imageSubresource().baseArrayLayer())
                .baseMipLevel(regions.get(I).imageSubresource().mipLevel())
                .layerCount(regions.get(I).imageSubresource().layerCount())
                .levelCount(1)
            );
            bufferMemoryBarrier.put(I, readMemoryBarrierTemplate).offset(regions.bufferOffset()).size(VK_WHOLE_SIZE).buffer(srcBuffer);
        });

        //
        vkCmdCopyBufferToImage2(cmdBuf, VkCopyBufferToImageInfo2.create().srcBuffer(srcBuffer).dstImage(dstImage).dstImageLayout(imageLayout).pRegions(regions));
        vkCmdPipelineBarrier2(cmdBuf, VkDependencyInfoKHR.create().pBufferMemoryBarriers(bufferMemoryBarrier).pImageMemoryBarriers(imageMemoryBarrier));

        //
        return this;
    }

    //
    public MemoryAllocationObj cmdCopyBufferToBuffer(VkCommandBuffer cmdBuf, long buffer, VkBufferCopy2.Buffer regions) {
        //
        var readMemoryBarrierTemplate = VkBufferMemoryBarrier2.create()
                .srcStageMask(VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT)
                .srcAccessMask(0)
                .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
                .dstAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);

        //
        var writeMemoryBarrierTemplate = VkBufferMemoryBarrier2.create()
                .srcStageMask(VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT)
                .srcAccessMask(0)
                .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
                .dstAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);

        //
        var srcBuffer = this.handle.get();
        var dstBuffer = buffer;

        //
        var memoryBarriers = VkBufferMemoryBarrier2.create(regions.capacity()*2);
        IntStream.range(0, regions.capacity()).forEachOrdered((I)->{
            memoryBarriers.put(I*2+0, readMemoryBarrierTemplate).offset(regions.srcOffset()).size(regions.size()).buffer(srcBuffer);
            memoryBarriers.put(I*2+1, writeMemoryBarrierTemplate).offset(regions.dstOffset()).size(regions.size()).buffer(dstBuffer);
        });

        //
        vkCmdCopyBuffer2(cmdBuf, VkCopyBufferInfo2.create().srcBuffer(srcBuffer).dstBuffer(dstBuffer).pRegions(regions));
        vkCmdPipelineBarrier2(cmdBuf, VkDependencyInfoKHR.create().pBufferMemoryBarriers(memoryBarriers));

        //
        return this;
    }



    //
    static public class BufferObjMemory extends MemoryAllocationObj {
        public VkBufferCreateInfo createInfo = null;
        public long deviceAddress = 0L;

        public BufferObjMemory(Handle base, Handle handle) {
            super(base, handle);
        }

        public BufferObjMemory(Handle base, MemoryAllocationCInfo.BufferCInfoMemory cInfo) {
            super(base, cInfo);

            //
            var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get());
            var physicalDeviceObj = (PhysicalDeviceObj)BasicObj.globalHandleMap.get(deviceObj.base.get());

            //
            vkCreateBuffer(deviceObj.device, this.createInfo = VkBufferCreateInfo.create()
                    .size(cInfo.size)
                    .usage(cInfo.usage | VK_BUFFER_USAGE_SHADER_DEVICE_ADDRESS_BIT | VK_BUFFER_USAGE_TRANSFER_SRC_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT)
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE),
                    null,
                    (this.handle = new Handle("Buffer")).ptr().getLongBuffer(1)
            );

            //
            vkGetBufferMemoryRequirements2(deviceObj.device, VkBufferMemoryRequirementsInfo2.create().buffer(this.handle.get()), this.memoryRequirements2 = VkMemoryRequirements2.create());
        }

        public long getDeviceAddress() {
            var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get());
            return this.deviceAddress == 0 ? (this.deviceAddress = vkGetBufferDeviceAddress(deviceObj.device, VkBufferDeviceAddressInfo.create().buffer(this.handle.get()))) : this.deviceAddress;
        }
    }

    //
    static public class ImageObjMemory extends MemoryAllocationObj {
        public VkImageCreateInfo createInfo = null;
        public ImageObjMemory(Handle base, Handle handle) {
            super(base, handle);
        }


        public ImageObjMemory(Handle base, MemoryAllocationCInfo.ImageCInfoMemory cInfo) {
            super(base, cInfo);

            //
            var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get());
            var physicalDeviceObj = (PhysicalDeviceObj)BasicObj.globalHandleMap.get(deviceObj.base.get());

            //
            vkCreateImage(deviceObj.device, this.createInfo = VkImageCreateInfo.create()
                    .flags(VK_IMAGE_CREATE_2D_VIEW_COMPATIBLE_BIT_EXT)
                    .extent(cInfo.extent3D)
                    .imageType(cInfo.extent3D.depth() > 1 ? VK_IMAGE_TYPE_3D : (cInfo.extent3D.height() > 1 ? VK_IMAGE_TYPE_2D : VK_IMAGE_TYPE_1D))
                    .usage(cInfo.usage | VK_IMAGE_USAGE_TRANSFER_SRC_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT)
                    .mipLevels(cInfo.mipLevels)
                    .arrayLayers(cInfo.arrayLayers)
                    .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE)
                    .samples(cInfo.samples)
                    .format(cInfo.format)
                    .tiling(cInfo.tiling),
                    null,
                    (this.handle = new Handle("Image")).ptr().getLongBuffer(1)
            );

            //
            vkGetImageMemoryRequirements2(deviceObj.device, VkImageMemoryRequirementsInfo2.create().image(this.handle.get()), this.memoryRequirements2 = VkMemoryRequirements2.create());
        }

        // TODO: special support for ImageView
        public ImageObjMemory transitionBarrier(VkCommandBuffer cmdBuf, int oldLayout, int newLayout, VkImageSubresourceRange subresourceRange) {
            //
            var memoryBarrier = VkImageMemoryBarrier2.create(1)
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

            vkCmdPipelineBarrier2(cmdBuf, VkDependencyInfoKHR.create().pImageMemoryBarriers(memoryBarrier));
            return this;
        }
    }

}
