package org.hydra2s.noire.objects;

import org.hydra2s.noire.descriptors.CopyInfoCInfo;
import org.hydra2s.noire.descriptors.ImageViewCInfo;
import org.lwjgl.vulkan.*;

import java.util.stream.IntStream;

import static org.lwjgl.vulkan.VK10.VK_QUEUE_FAMILY_IGNORED;
import static org.lwjgl.vulkan.VK10.VK_WHOLE_SIZE;
import static org.lwjgl.vulkan.VK13.*;
import static org.lwjgl.vulkan.VK13.VK_STRUCTURE_TYPE_DEPENDENCY_INFO;

// GRIB!
abstract public class CopyUtilObj {

    //
    public static void cmdCopyBufferToImageView(
        VkCommandBuffer cmdBuf,
        CopyInfoCInfo.BufferRangeCopyInfo srcBufferRange,
        CopyInfoCInfo.ImageViewCopyInfo dstImageView,
        VkExtent3D extent
    ) {
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(dstImageView.device);
        var dstImageViewObj = (ImageViewObj)deviceObj.handleMap.get(new BasicObj.Handle("ImageView", dstImageView.imageView));

        //
        CopyUtilObj.cmdCopyBufferToImage(cmdBuf, srcBufferRange.buffer, ((ImageViewCInfo)dstImageViewObj.cInfo).image,
            ((ImageViewCInfo)dstImageViewObj.cInfo).imageLayout, VkBufferImageCopy2.calloc(1)
                .sType(VK_STRUCTURE_TYPE_BUFFER_IMAGE_COPY_2)
                .bufferOffset(srcBufferRange.offset)
                .imageOffset(dstImageView.offset).imageExtent(extent)
                .imageSubresource(dstImageViewObj.subresourceLayers(dstImageView.mipLevel))
        );
    }

    //
    public static void cmdCopyImageViewToBuffer(
        VkCommandBuffer cmdBuf,
        CopyInfoCInfo.ImageViewCopyInfo srcImageView,
        CopyInfoCInfo.BufferRangeCopyInfo dstBufferRange,
        VkExtent3D extent
    ) {
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(srcImageView.device);
        var srcImageViewObj = (ImageViewObj)deviceObj.handleMap.get(new BasicObj.Handle("ImageView", srcImageView.imageView));

        //
        CopyUtilObj.cmdCopyImageToBuffer(cmdBuf, ((ImageViewCInfo)srcImageViewObj.cInfo).image, dstBufferRange.buffer,
            ((ImageViewCInfo)srcImageViewObj.cInfo).imageLayout, VkBufferImageCopy2.calloc(1)
                .sType(VK_STRUCTURE_TYPE_BUFFER_IMAGE_COPY_2)
                .bufferOffset(dstBufferRange.offset)
                .imageOffset(srcImageView.offset).imageExtent(extent)
                .imageSubresource(srcImageViewObj.subresourceLayers(srcImageView.mipLevel))
        );
    }

    //
    public static void cmdCopyImageViewToImageView(
        VkCommandBuffer cmdBuf,
        CopyInfoCInfo.ImageViewCopyInfo srcImageView,
        CopyInfoCInfo.ImageViewCopyInfo dstImageView,
        VkExtent3D extent
    ) {
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(srcImageView.device);
        var srcImageViewObj = (ImageViewObj)deviceObj.handleMap.get(new BasicObj.Handle("ImageView", srcImageView.imageView));

        //
        CopyUtilObj.cmdCopyImageToImageView(cmdBuf, new CopyInfoCInfo.ImageCopyInfo(){{
            image = ((ImageViewCInfo)srcImageViewObj.cInfo).image;
            imageLayout = srcImageViewObj.getImageLayout();
            offset = srcImageView.offset;
            subresource = srcImageViewObj.subresourceLayers(srcImageView.mipLevel);
        }}, dstImageView, extent);
    }

    //
    public static void cmdCopyImageViewToImage(
        VkCommandBuffer cmdBuf,
        CopyInfoCInfo.ImageCopyInfo dstImage,
        CopyInfoCInfo.ImageViewCopyInfo srcImageView,
        VkExtent3D extent
    ) {
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(srcImageView.device);
        var srcImageViewObj = (ImageViewObj)deviceObj.handleMap.get(new BasicObj.Handle("ImageView", srcImageView.imageView));

        //
        CopyUtilObj.cmdCopyImageToImage(cmdBuf, ((ImageViewCInfo)srcImageViewObj.cInfo).image, dstImage.image, ((ImageViewCInfo)srcImageViewObj.cInfo).imageLayout, dstImage.imageLayout,
            VkImageCopy2.calloc(1)
                .sType(VK_STRUCTURE_TYPE_IMAGE_COPY_2)
                .extent(extent)
                .srcOffset(srcImageView.offset)
                .dstOffset(dstImage.offset)
                .srcSubresource(srcImageViewObj.subresourceLayers(srcImageView.mipLevel))
                .dstSubresource(dstImage.subresource));
    }

    //
    public static void cmdCopyImageToImageView(
        VkCommandBuffer cmdBuf,
        CopyInfoCInfo.ImageCopyInfo srcImage,
        CopyInfoCInfo.ImageViewCopyInfo dstImageView,
        VkExtent3D extent
    ) {
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(dstImageView.device);
        var dstImageViewObj = (ImageViewObj)deviceObj.handleMap.get(new BasicObj.Handle("ImageView", dstImageView.imageView));

        //
        CopyUtilObj.cmdCopyImageToImage(cmdBuf, srcImage.image, ((ImageViewCInfo)dstImageViewObj.cInfo).image, srcImage.imageLayout, ((ImageViewCInfo)dstImageViewObj.cInfo).imageLayout,
            VkImageCopy2.calloc(1)
                .sType(VK_STRUCTURE_TYPE_IMAGE_COPY_2)
                .extent(extent)
                .dstOffset(dstImageView.offset)
                .srcOffset(srcImage.offset)
                .dstSubresource(dstImageViewObj.subresourceLayers(dstImageView.mipLevel))
                .srcSubresource(srcImage.subresource));
    }

    //
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

    //
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

    //
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


    //
    public static void cmdTransitionBarrier(VkCommandBuffer cmdBuf, long image, int oldLayout, int newLayout, VkImageSubresourceRange subresourceRange) {
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
            .image(image);

        //
        vkCmdPipelineBarrier2(cmdBuf, VkDependencyInfoKHR.calloc().sType(VK_STRUCTURE_TYPE_DEPENDENCY_INFO).pImageMemoryBarriers(memoryBarrier));
    }

}
