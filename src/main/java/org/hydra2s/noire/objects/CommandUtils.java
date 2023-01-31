package org.hydra2s.noire.objects;

import org.hydra2s.noire.descriptors.*;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.stream.IntStream;

import static java.lang.Math.min;
import static org.lwjgl.system.MemoryUtil.memAllocInt;
import static org.lwjgl.vulkan.EXTExtendedDynamicState2.vkCmdSetLogicOpEXT;
import static org.lwjgl.vulkan.EXTExtendedDynamicState3.*;
import static org.lwjgl.vulkan.EXTExtendedDynamicState3.vkCmdSetLogicOpEnableEXT;
import static org.lwjgl.vulkan.EXTMultiDraw.vkCmdDrawMultiEXT;
import static org.lwjgl.vulkan.EXTVertexInputDynamicState.vkCmdSetVertexInputEXT;
import static org.lwjgl.vulkan.VK10.VK_QUEUE_FAMILY_IGNORED;
import static org.lwjgl.vulkan.VK10.VK_WHOLE_SIZE;
import static org.lwjgl.vulkan.VK13.*;
import static org.lwjgl.vulkan.VK13.VK_STRUCTURE_TYPE_DEPENDENCY_INFO;

// GRIB!
// TODO: support for queue families
abstract public class CommandUtils {

    //
    public static class DirectAccessInfo {
        public DeviceObj deviceObj;
        public PipelineObj pipelineObj;
        public PipelineLayoutObj pipelineLayoutObj;
        public ImageSetObj.FramebufferObj framebufferObj;
        public long pipelineLayout = 0L;
    };

    //
    public static class ComputeDispatchInfo {
        public long device = 0L;
        public long pipelineLayout = 0L;
        public long pipeline = 0L;
        public VkExtent3D dispatch = VkExtent3D.calloc().width(1).height(1).depth(1);
        public ByteBuffer pushConstRaw = null;
        public int pushConstByteOffset = 0;
    }

    //
    public static class GraphicsDrawInfo {
        public long device = 0L;
        public long pipelineLayout = 0L;
        public long pipeline = 0L;
        public long imageSet = 0L;
        public ImageSetCInfo.FBLayout fbLayout = null;
        public VkMultiDrawInfoEXT.Buffer multiDraw = null;
        public ByteBuffer pushConstRaw = null;
        public int pushConstByteOffset = 0;
        public boolean clearColor = true;
        public boolean clearDepthStencil = true;
    }


    public static class ImageViewCopyInfo {
        // TODO: fix device requirements issues
        public long device = 0L;
        public long imageView = 0L;
        public VkOffset3D offset = VkOffset3D.calloc().set(0, 0, 0);
        public int mipLevel = 0;
    }

    public static class BufferRangeCopyInfo {
        public long buffer;
        public long offset = 0L;
        public long range = VK_WHOLE_SIZE;

        // specific
        public int rowLength = 0;
        public int imageHeight = 0;
    }

    public static class ImageCopyInfo {
        public long image = 0L;
        public int imageLayout = VK_IMAGE_LAYOUT_GENERAL;
        public VkOffset3D offset = VkOffset3D.calloc().set(0, 0, 0);
        public VkImageSubresourceLayers subresource = VkImageSubresourceLayers.calloc().set(VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1);
    }


    //
    public static void cmdCopyBufferToImageView(
        VkCommandBuffer cmdBuf,
        BufferRangeCopyInfo srcBufferRange,
        ImageViewCopyInfo dstImageView,
        VkExtent3D extent
    ) {
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(dstImageView.device).orElse(null);
        var dstImageViewObj = (ImageViewObj)deviceObj.handleMap.get(new BasicObj.Handle("ImageView", dstImageView.imageView)).orElse(null);

        //
        CommandUtils.cmdCopyBufferToImage(cmdBuf, srcBufferRange.buffer, ((ImageViewCInfo)dstImageViewObj.cInfo).image,
            ((ImageViewCInfo)dstImageViewObj.cInfo).imageLayout, VkBufferImageCopy2.calloc(1)
                .sType(VK_STRUCTURE_TYPE_BUFFER_IMAGE_COPY_2)
                .bufferOffset(srcBufferRange.offset)
                .bufferRowLength(srcBufferRange.rowLength)
                .bufferImageHeight(srcBufferRange.imageHeight)
                .imageOffset(dstImageView.offset).imageExtent(extent)
                .imageSubresource(dstImageViewObj.subresourceLayers(dstImageView.mipLevel))
        );
    }

    //
    public static void cmdCopyImageViewToBuffer(
        VkCommandBuffer cmdBuf,
        ImageViewCopyInfo srcImageView,
        BufferRangeCopyInfo dstBufferRange,
        VkExtent3D extent
    ) {
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(srcImageView.device).orElse(null);
        var srcImageViewObj = (ImageViewObj)deviceObj.handleMap.get(new BasicObj.Handle("ImageView", srcImageView.imageView)).orElse(null);

        //
        CommandUtils.cmdCopyImageToBuffer(cmdBuf, ((ImageViewCInfo)srcImageViewObj.cInfo).image, dstBufferRange.buffer,
            ((ImageViewCInfo)srcImageViewObj.cInfo).imageLayout, VkBufferImageCopy2.calloc(1)
                .sType(VK_STRUCTURE_TYPE_BUFFER_IMAGE_COPY_2)
                .bufferOffset(dstBufferRange.offset)
                .bufferRowLength(dstBufferRange.rowLength)
                .bufferImageHeight(dstBufferRange.imageHeight)
                .imageOffset(srcImageView.offset).imageExtent(extent)
                .imageSubresource(srcImageViewObj.subresourceLayers(srcImageView.mipLevel))
        );
    }

    //
    public static void cmdCopyImageViewToImageView(
        VkCommandBuffer cmdBuf,
        ImageViewCopyInfo srcImageView,
        ImageViewCopyInfo dstImageView,
        VkExtent3D extent
    ) {
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(srcImageView.device).orElse(null);
        var srcImageViewObj = (ImageViewObj)deviceObj.handleMap.get(new BasicObj.Handle("ImageView", srcImageView.imageView)).orElse(null);

        //
        CommandUtils.cmdCopyImageToImageView(cmdBuf, new ImageCopyInfo(){{
            image = ((ImageViewCInfo)srcImageViewObj.cInfo).image;
            imageLayout = srcImageViewObj.getImageLayout();
            offset = srcImageView.offset;
            subresource = srcImageViewObj.subresourceLayers(srcImageView.mipLevel);
        }}, dstImageView, extent);
    }

    //
    public static void cmdCopyImageViewToImage(
        VkCommandBuffer cmdBuf,
        ImageCopyInfo dstImage,
        ImageViewCopyInfo srcImageView,
        VkExtent3D extent
    ) {
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(srcImageView.device).orElse(null);
        var srcImageViewObj = (ImageViewObj)deviceObj.handleMap.get(new BasicObj.Handle("ImageView", srcImageView.imageView)).orElse(null);

        //
        CommandUtils.cmdCopyImageToImage(cmdBuf, ((ImageViewCInfo)srcImageViewObj.cInfo).image, dstImage.image, ((ImageViewCInfo)srcImageViewObj.cInfo).imageLayout, dstImage.imageLayout,
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
        ImageCopyInfo srcImage,
        ImageViewCopyInfo dstImageView,
        VkExtent3D extent
    ) {
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(dstImageView.device).orElse(null);
        var dstImageViewObj = (ImageViewObj)deviceObj.handleMap.get(new BasicObj.Handle("ImageView", dstImageView.imageView)).orElse(null);

        //
        CommandUtils.cmdCopyImageToImage(cmdBuf, srcImage.image, ((ImageViewCInfo)dstImageViewObj.cInfo).image, srcImage.imageLayout, ((ImageViewCInfo)dstImageViewObj.cInfo).imageLayout,
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
        // TODO: reuse same barrier info (i.e. template)
        var readMemoryBarrierTemplate = VkImageMemoryBarrier2.calloc()
            .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER_2)
            .srcStageMask(VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT | VK_PIPELINE_STAGE_2_HOST_BIT)
            .srcAccessMask(VK_ACCESS_2_TRANSFER_READ_BIT | VK_ACCESS_2_TRANSFER_WRITE_BIT | VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
            .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
            .dstAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
            .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            .oldLayout(VK_IMAGE_LAYOUT_UNDEFINED)
            .newLayout(srcImageLayout);

        // TODO: reuse same barrier info (i.e. template)
        var writeMemoryBarrierTemplate = VkImageMemoryBarrier2.calloc()
            .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER_2)
            .srcStageMask(VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT)
            .srcAccessMask( VK_ACCESS_2_TRANSFER_READ_BIT | VK_ACCESS_2_TRANSFER_WRITE_BIT | VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
            .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
            .dstAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
            .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            .oldLayout(VK_IMAGE_LAYOUT_UNDEFINED)
            .newLayout(dstImageLayout);

        //
        var imageMemoryBarrier = VkImageMemoryBarrier2.calloc(regions.remaining()*2);

        // TODO: support a correct buffer size
        IntStream.range(0, regions.remaining()).forEachOrdered((I)->{
            imageMemoryBarrier.put(I*2+0, writeMemoryBarrierTemplate);
            imageMemoryBarrier.get(I*2+0)
                .image(dstImage).oldLayout(VK_IMAGE_LAYOUT_UNDEFINED).newLayout(dstImageLayout).subresourceRange(VkImageSubresourceRange.calloc()
                .aspectMask(regions.get(I).dstSubresource().aspectMask())
                .baseArrayLayer(regions.get(I).dstSubresource().baseArrayLayer())
                .baseMipLevel(regions.get(I).dstSubresource().mipLevel())
                .layerCount(regions.get(I).dstSubresource().layerCount())
                .levelCount(1)
            );
            imageMemoryBarrier.put(I*2+1, readMemoryBarrierTemplate);
            imageMemoryBarrier.get(I*2+1)
                .image(srcImage).oldLayout(VK_IMAGE_LAYOUT_UNDEFINED).newLayout(srcImageLayout).subresourceRange(VkImageSubresourceRange.calloc()
                .aspectMask(regions.get(I).srcSubresource().aspectMask())
                .baseArrayLayer(regions.get(I).srcSubresource().baseArrayLayer())
                .baseMipLevel(regions.get(I).srcSubresource().mipLevel())
                .layerCount(regions.get(I).srcSubresource().layerCount())
                .levelCount(1)
            );
        });

        // TODO: correct image layout, and dual side image barrier
        vkCmdCopyImage2(cmdBuf, VkCopyImageInfo2.calloc().sType(VK_STRUCTURE_TYPE_COPY_IMAGE_INFO_2).dstImage(dstImage).dstImageLayout(dstImageLayout).srcImageLayout(srcImageLayout).srcImage(srcImage).pRegions(regions));
        vkCmdPipelineBarrier2(cmdBuf, VkDependencyInfoKHR.calloc().sType(VK_STRUCTURE_TYPE_DEPENDENCY_INFO).pImageMemoryBarriers(imageMemoryBarrier));

        //
        //return this;
    }

    //
    static public void cmdCopyImageToBuffer(VkCommandBuffer cmdBuf, long srcImage, long dstBuffer, int imageLayout, VkBufferImageCopy2.Buffer regions) {
        // TODO: reuse same barrier info (i.e. template)
        var readMemoryBarrierTemplate = VkImageMemoryBarrier2.calloc()
            .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER_2)
            .srcStageMask(VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT | VK_PIPELINE_STAGE_2_HOST_BIT)
            .srcAccessMask(VK_ACCESS_2_TRANSFER_READ_BIT | VK_ACCESS_2_TRANSFER_WRITE_BIT | VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
            .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
            .dstAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
            .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            .oldLayout(VK_IMAGE_LAYOUT_UNDEFINED)
            .newLayout(imageLayout);

        // TODO: reuse same barrier info (i.e. template)
        var writeMemoryBarrierTemplate = VkBufferMemoryBarrier2.calloc()
            .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER_2)
            .srcStageMask(VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT)
            .srcAccessMask( VK_ACCESS_2_TRANSFER_READ_BIT | VK_ACCESS_2_TRANSFER_WRITE_BIT | VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
            .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
            .dstAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
            .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);

        //
        var imageMemoryBarrier = VkImageMemoryBarrier2.calloc(regions.remaining());
        var bufferMemoryBarrier = VkBufferMemoryBarrier2.calloc(regions.remaining());

        // TODO: support a correct buffer size
        IntStream.range(0, regions.remaining()).forEachOrdered((I)->{
            imageMemoryBarrier.put(I, readMemoryBarrierTemplate);
            imageMemoryBarrier.get(I)
                .image(srcImage).oldLayout(VK_IMAGE_LAYOUT_UNDEFINED).newLayout(imageLayout).subresourceRange(VkImageSubresourceRange.calloc()
                .aspectMask(regions.get(I).imageSubresource().aspectMask())
                .baseArrayLayer(regions.get(I).imageSubresource().baseArrayLayer())
                .baseMipLevel(regions.get(I).imageSubresource().mipLevel())
                .layerCount(regions.get(I).imageSubresource().layerCount())
                .levelCount(1)
            );
            bufferMemoryBarrier.put(I, writeMemoryBarrierTemplate);
            bufferMemoryBarrier.get(I).offset(regions.bufferOffset()).size(VK_WHOLE_SIZE).buffer(dstBuffer);
        });

        // TODO: correct image layout, and dual side image barrier
        vkCmdCopyImageToBuffer2(cmdBuf, VkCopyImageToBufferInfo2.calloc().sType(VK_STRUCTURE_TYPE_COPY_IMAGE_TO_BUFFER_INFO_2).srcImageLayout(imageLayout).srcImage(srcImage).dstBuffer(dstBuffer).pRegions(regions));
        vkCmdPipelineBarrier2(cmdBuf, VkDependencyInfoKHR.calloc().sType(VK_STRUCTURE_TYPE_DEPENDENCY_INFO).pBufferMemoryBarriers(bufferMemoryBarrier).pImageMemoryBarriers(imageMemoryBarrier));

        //
        //return this;
    }

    //
    static public void cmdCopyBufferToImage(VkCommandBuffer cmdBuf, long srcBuffer, long dstImage, int imageLayout, VkBufferImageCopy2.Buffer regions) {
        // TODO: reuse same barrier info (i.e. template)
        var readMemoryBarrierTemplate = VkBufferMemoryBarrier2.calloc()
            .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER_2)
            .srcStageMask(VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT | VK_PIPELINE_STAGE_2_HOST_BIT)
            .srcAccessMask(VK_ACCESS_2_TRANSFER_READ_BIT | VK_ACCESS_2_TRANSFER_WRITE_BIT | VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
            .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
            .dstAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
            .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);

        // TODO: reuse same barrier info (i.e. template)
        var writeMemoryBarrierTemplate = VkImageMemoryBarrier2.calloc()
            .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER_2)
            .srcStageMask(VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT)
            .srcAccessMask( VK_ACCESS_2_TRANSFER_READ_BIT | VK_ACCESS_2_TRANSFER_WRITE_BIT | VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
            .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
            .dstAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
            .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            .oldLayout(VK_IMAGE_LAYOUT_UNDEFINED)
            .newLayout(imageLayout);

        //
        var imageMemoryBarrier = VkImageMemoryBarrier2.calloc(regions.remaining());
        var bufferMemoryBarrier = VkBufferMemoryBarrier2.calloc(regions.remaining());

        // TODO: support a correct buffer size
        IntStream.range(0, regions.remaining()).forEachOrdered((I)->{
            imageMemoryBarrier.put(I, writeMemoryBarrierTemplate);
            imageMemoryBarrier.get(I)
                .image(dstImage).oldLayout(VK_IMAGE_LAYOUT_UNDEFINED).newLayout(imageLayout).subresourceRange(VkImageSubresourceRange.calloc()
                .aspectMask(regions.get(I).imageSubresource().aspectMask())
                .baseArrayLayer(regions.get(I).imageSubresource().baseArrayLayer())
                .baseMipLevel(regions.get(I).imageSubresource().mipLevel())
                .layerCount(regions.get(I).imageSubresource().layerCount())
                .levelCount(1)
            );
            bufferMemoryBarrier.put(I, readMemoryBarrierTemplate);
            bufferMemoryBarrier.get(I).offset(regions.bufferOffset()).size(VK_WHOLE_SIZE).buffer(srcBuffer);
        });

        // TODO: correct image layout, and dual side image barrier
        vkCmdCopyBufferToImage2(cmdBuf, VkCopyBufferToImageInfo2.calloc().sType(VK_STRUCTURE_TYPE_COPY_BUFFER_TO_IMAGE_INFO_2).dstImageLayout(imageLayout).srcBuffer(srcBuffer).dstImage(dstImage).pRegions(regions));
        vkCmdPipelineBarrier2(cmdBuf, VkDependencyInfoKHR.calloc().sType(VK_STRUCTURE_TYPE_DEPENDENCY_INFO).pBufferMemoryBarriers(bufferMemoryBarrier).pImageMemoryBarriers(imageMemoryBarrier));

        //
        //return this;
    }

    //
    static public void cmdCopyBufferToBuffer(VkCommandBuffer cmdBuf, long srcBuffer, long dstBuffer, VkBufferCopy2.Buffer regions) {
        // TODO: reuse same barrier info (i.e. template)
        var readMemoryBarrierTemplate = VkBufferMemoryBarrier2.calloc()
            .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER_2)
            .srcStageMask(VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT | VK_PIPELINE_STAGE_2_HOST_BIT)
            .srcAccessMask(VK_ACCESS_2_TRANSFER_READ_BIT | VK_ACCESS_2_TRANSFER_WRITE_BIT | VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
            .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
            .dstAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
            .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);

        // TODO: reuse same barrier info (i.e. template)
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
            memoryBarriers.put(I*2+0, readMemoryBarrierTemplate);
            memoryBarriers.get(I*2+0).offset(regions.srcOffset()).size(regions.size()).buffer(srcBuffer);
            memoryBarriers.put(I*2+1, writeMemoryBarrierTemplate);
            memoryBarriers.get(I*2+1).offset(regions.dstOffset()).size(regions.size()).buffer(dstBuffer);
        });

        //
        vkCmdCopyBuffer2(cmdBuf, VkCopyBufferInfo2.calloc().sType(VK_STRUCTURE_TYPE_COPY_BUFFER_INFO_2).srcBuffer(srcBuffer).dstBuffer(dstBuffer).pRegions(regions));
        vkCmdPipelineBarrier2(cmdBuf, VkDependencyInfoKHR.calloc().sType(VK_STRUCTURE_TYPE_DEPENDENCY_INFO).pBufferMemoryBarriers(memoryBarriers));

        //
        //return this;
    }

    // TODO: support for queue families
    public static void cmdTransitionBarrier(VkCommandBuffer cmdBuf, long image, int oldLayout, int newLayout, VkImageSubresourceRange subresourceRange) {
        // get correct access mask by image layouts
        var dstAccessMask = UtilsCInfo.getCorrectAccessMaskByImageLayout(newLayout);
        var srcAccessMask = UtilsCInfo.getCorrectAccessMaskByImageLayout(oldLayout);

        // if undefined, use memory mask
        if (dstAccessMask == 0) { dstAccessMask = VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT; };
        if (srcAccessMask == 0) { srcAccessMask = VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT; };

        //
        var srcStageMask = UtilsCInfo.getCorrectPipelineStagesByAccessMask(srcAccessMask);
        var dstStageMask = UtilsCInfo.getCorrectPipelineStagesByAccessMask(dstAccessMask);

        //
        if (srcStageMask == 0) { srcStageMask = VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT; };
        if (dstStageMask == 0) { dstStageMask = VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT; };

        // barrier by image layout
        // TODO: support for queue families
        var memoryBarrier = VkImageMemoryBarrier2.calloc(1)
            .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER_2)
            .srcStageMask(srcStageMask)
            .srcAccessMask(srcAccessMask)
            .dstStageMask(dstStageMask)
            .dstAccessMask(dstAccessMask)
            .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            .oldLayout(VK_IMAGE_LAYOUT_UNDEFINED)
            .newLayout(newLayout)
            .subresourceRange(subresourceRange)
            .image(image);

        //
        vkCmdPipelineBarrier2(cmdBuf, VkDependencyInfoKHR.calloc().sType(VK_STRUCTURE_TYPE_DEPENDENCY_INFO).pImageMemoryBarriers(memoryBarrier));
    }

    // virtual buffer copying version
    public static void cmdCopyVBufferToVBuffer(VkCommandBuffer cmdBuf, VkDescriptorBufferInfo src, VkDescriptorBufferInfo dst, VkBufferCopy2.Buffer copies) {
        // TODO: fix VK_WHOLE_SIZE issues!
        var modified = copies.stream().map((cp)-> cp
            .dstOffset(cp.dstOffset()+dst.offset())
            .srcOffset(cp.srcOffset()+src.offset())
            .size(min(src.range(), dst.range()))).toList();
        var Rs = copies.remaining();
        for (var I=0;I<Rs;I++) { copies.get(I).set(modified.get(I)); } // modify copies
        CommandUtils.cmdCopyBufferToBuffer(cmdBuf, src.buffer(), dst.buffer(), copies);
    }

    // TODO: support for queue families
    public static void cmdSynchronizeFromHost(VkCommandBuffer cmdBuf, VkDescriptorBufferInfo range) {
        // for `map()` or copy operations
        // TODO: support for queue families
        var bufferMemoryBarrier = VkBufferMemoryBarrier2.calloc(1)
            .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER_2)
            .srcStageMask(VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT | VK_PIPELINE_STAGE_2_HOST_BIT)
            .srcAccessMask(VK_ACCESS_2_TRANSFER_READ_BIT | VK_ACCESS_2_TRANSFER_WRITE_BIT | VK_ACCESS_2_HOST_READ_BIT | VK_ACCESS_2_HOST_WRITE_BIT)
            .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
            .dstAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
            .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            .buffer(range.buffer())
            .offset(range.offset())
            .size(range.range()); // TODO: support partial synchronization

        //
        vkCmdPipelineBarrier2(cmdBuf, VkDependencyInfoKHR.calloc().sType(VK_STRUCTURE_TYPE_DEPENDENCY_INFO).pBufferMemoryBarriers(bufferMemoryBarrier));
    }

    //
    public static void cmdDispatch(VkCommandBuffer cmdBuf, ComputeDispatchInfo cmdInfo) {
        var $deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(cmdInfo.device).orElse(null);;
        var $pipelineObj = /*(cmdInfo.pipelineLayout == 0 || cmdInfo.fbLayout == null) ?*/ (PipelineObj)$deviceObj.handleMap.get(new BasicObj.Handle("Pipeline", cmdInfo.pipeline)).orElse(null) /*: null*/;
        var $pipelineLayout = cmdInfo.pipelineLayout != 0 ? cmdInfo.pipelineLayout : ((PipelineCInfo.ComputePipelineCInfo)$pipelineObj.cInfo).pipelineLayout;
        var $pipelineLayoutObj = (PipelineLayoutObj)$deviceObj.handleMap.get(new BasicObj.Handle("PipelineLayout", $pipelineLayout)).orElse(null);;

        cmdDispatch(cmdBuf, cmdInfo, new DirectAccessInfo(){{
            deviceObj = $deviceObj;
            pipelineObj = $pipelineObj;
            pipelineLayoutObj = $pipelineLayoutObj;
            pipelineLayout = $pipelineLayout;
        }});
    };

    //
    public static void cmdDispatch(VkCommandBuffer cmdBuf, ComputeDispatchInfo cmdInfo, DirectAccessInfo directInfo) {
        if (cmdInfo.pushConstRaw != null) {
            vkCmdPushConstants(cmdBuf, cmdInfo.pipelineLayout, VK_SHADER_STAGE_ALL, cmdInfo.pushConstByteOffset, cmdInfo.pushConstRaw);
        }

        //
        if (directInfo.pipelineLayoutObj != null) {
            directInfo.pipelineLayoutObj.cmdBindBuffers(cmdBuf, VK_PIPELINE_BIND_POINT_COMPUTE, directInfo.pipelineObj.uniformDescriptorSet != null ? directInfo.pipelineObj.uniformDescriptorSet : null);
        }

        //
        vkCmdBindPipeline(cmdBuf, VK_PIPELINE_BIND_POINT_COMPUTE, cmdInfo.pipeline);
        vkCmdDispatch(cmdBuf, cmdInfo.dispatch.width(), cmdInfo.dispatch.height(), cmdInfo.dispatch.depth());
        vkCmdPipelineBarrier2(cmdBuf, VkDependencyInfoKHR.calloc().sType(VK_STRUCTURE_TYPE_DEPENDENCY_INFO).pMemoryBarriers(VkMemoryBarrier2.calloc(1)
            .sType(VK_STRUCTURE_TYPE_MEMORY_BARRIER_2)
            .srcStageMask(VK_PIPELINE_STAGE_2_COMPUTE_SHADER_BIT)
            .srcAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
            .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
            .dstAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)));
    }

    //
    public static void preInitializeFb(long device, long imageSet, ImageSetCInfo.FBLayout fbLayout) {
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(device).orElse(null);;
        var framebufferObj = (ImageSetObj.FramebufferObj)deviceObj.handleMap.get(new BasicObj.Handle("ImageSet", imageSet)).orElse(null);;

        //
        fbLayout.attachmentInfos = fbLayout.attachmentInfos != null ? fbLayout.attachmentInfos : VkRenderingAttachmentInfo.calloc(fbLayout.formats.remaining());
        var Fs = fbLayout.formats.remaining();
        for (var I=0;I<Fs;I++) {
            fbLayout.attachmentInfos.get(I).sType(VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO);
            fbLayout.attachmentInfos.get(I).imageLayout(framebufferObj.writingImageViews.get(I).getImageLayout());
            fbLayout.attachmentInfos.get(I).imageView(framebufferObj.writingImageViews.get(I).handle.get());
            fbLayout.attachmentInfos.get(I).loadOp(VK_ATTACHMENT_LOAD_OP_LOAD);
            fbLayout.attachmentInfos.get(I).storeOp(VK_ATTACHMENT_STORE_OP_STORE);
        }

        //
        boolean hasDepthStencil = fbLayout.depthStencilFormat != VK_FORMAT_UNDEFINED;
        boolean hasDepth = fbLayout.depthStencilFormat != VK_FORMAT_UNDEFINED;
        boolean hasStencil = fbLayout.depthStencilFormat != VK_FORMAT_UNDEFINED;

        //
        if (hasDepthStencil) {
            fbLayout.depthStencilAttachmentInfo.sType(VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO);
            fbLayout.depthStencilAttachmentInfo.imageView(framebufferObj.writingDepthStencilImageView.getHandle().get());
            fbLayout.depthStencilAttachmentInfo.imageLayout(framebufferObj.writingDepthStencilImageView.getImageLayout());
            fbLayout.depthStencilAttachmentInfo.loadOp(VK_ATTACHMENT_LOAD_OP_LOAD);
            fbLayout.depthStencilAttachmentInfo.storeOp(VK_ATTACHMENT_STORE_OP_STORE);
        };
    }

    //
    public static void cmdDraw(VkCommandBuffer cmdBuf, GraphicsDrawInfo cmdInfo) {
        var $deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(cmdInfo.device).orElse(null);;
        var $pipelineObj = /*(cmdInfo.pipelineLayout == 0 || cmdInfo.fbLayout == null) ?*/ (PipelineObj)$deviceObj.handleMap.get(new BasicObj.Handle("Pipeline", cmdInfo.pipeline)).orElse(null) /*: null*/;
        var $pipelineLayout = cmdInfo.pipelineLayout != 0 ? cmdInfo.pipelineLayout : ((PipelineCInfo.ComputePipelineCInfo)$pipelineObj.cInfo).pipelineLayout;
        var $pipelineLayoutObj = (PipelineLayoutObj)$deviceObj.handleMap.get(new BasicObj.Handle("PipelineLayout", $pipelineLayout)).orElse(null);;
        var $framebufferObj = (ImageSetObj.FramebufferObj)$deviceObj.handleMap.get(new BasicObj.Handle("ImageSet", cmdInfo.imageSet)).orElse(null);;

        //
        cmdDraw(cmdBuf, cmdInfo, new DirectAccessInfo(){{
            deviceObj = $deviceObj;
            pipelineObj = $pipelineObj;
            pipelineLayoutObj = $pipelineLayoutObj;
            framebufferObj = $framebufferObj;
            pipelineLayout = $pipelineLayout;
        }});
    }

    //
    public static void cmdDraw(VkCommandBuffer cmdBuf, GraphicsDrawInfo cmdInfo, DirectAccessInfo directInfo) {
        // corrupted
        if (cmdInfo.multiDraw != null && cmdInfo.multiDraw.remaining() <= 0) { return; };

        //
        var fbLayout = cmdInfo.fbLayout != null ? cmdInfo.fbLayout : ((PipelineCInfo.GraphicsPipelineCInfo)directInfo.pipelineObj.cInfo).fbLayout;
        int layerCount = Collections.min(fbLayout.layerCounts);

        //
        boolean hasDepthStencil = fbLayout.depthStencilFormat != VK_FORMAT_UNDEFINED;
        boolean hasDepth = fbLayout.depthStencilFormat != VK_FORMAT_UNDEFINED;
        boolean hasStencil = fbLayout.depthStencilFormat != VK_FORMAT_UNDEFINED;

        //
        vkCmdBeginRendering(cmdBuf, VkRenderingInfoKHR.calloc()
            .sType(VK_STRUCTURE_TYPE_RENDERING_INFO)
            .pColorAttachments(fbLayout.attachmentInfos)
            .pDepthAttachment(hasDepth ? fbLayout.depthStencilAttachmentInfo : null)
            .pStencilAttachment(hasStencil ? fbLayout.depthStencilAttachmentInfo : null)
            .viewMask(0x0)
            .layerCount(layerCount)
            .renderArea(fbLayout.scissor)
        );

        //
        if (cmdInfo.pushConstRaw != null && directInfo.pipelineLayout != 0) {
            vkCmdPushConstants(cmdBuf, directInfo.pipelineLayout, VK_SHADER_STAGE_ALL, cmdInfo.pushConstByteOffset, cmdInfo.pushConstRaw);
        }

        //
        if (directInfo.pipelineLayoutObj != null && directInfo.pipelineObj != null) {
            directInfo.pipelineLayoutObj.cmdBindBuffers(cmdBuf, VK_PIPELINE_BIND_POINT_GRAPHICS, directInfo.pipelineObj != null && directInfo.pipelineObj.uniformDescriptorSet != null ? directInfo.pipelineObj.uniformDescriptorSet : null);
        }

        //
        if (cmdInfo.pipeline != 0) {
            vkCmdBindPipeline(cmdBuf, VK_PIPELINE_BIND_POINT_GRAPHICS, cmdInfo.pipeline);

            //
            vkCmdSetCullMode(cmdBuf, fbLayout.cullState ? VK_CULL_MODE_BACK_BIT : VK_CULL_MODE_NONE);
            vkCmdSetDepthBiasEnable(cmdBuf, fbLayout.depthBias.enabled);
            vkCmdSetDepthBias(cmdBuf, fbLayout.depthBias.units, 0.0f, fbLayout.depthBias.factor);
            vkCmdSetStencilTestEnable(cmdBuf, false);
            vkCmdSetDepthWriteEnable(cmdBuf, fbLayout.depthState.depthMask);
            vkCmdSetDepthTestEnable(cmdBuf, fbLayout.depthState.depthTest);
            vkCmdSetDepthCompareOp(cmdBuf, fbLayout.depthState.function);
            vkCmdSetScissorWithCount(cmdBuf, VkRect2D.calloc(1).put(0, fbLayout.scissor));
            vkCmdSetViewportWithCount(cmdBuf, VkViewport.calloc(1).put(0, fbLayout.viewport));

            //
            var Bs = fbLayout.blendStates.size();
            var blendEquation = VkColorBlendEquationEXT.calloc(Bs);
            var blendAttachment = memAllocInt(Bs);
            var colorMask = memAllocInt(Bs);
            for (var I = 0; I < Bs; I++) {
                blendAttachment.put(I, fbLayout.blendStates.get(I).enabled?1:0);
                colorMask.put(I, fbLayout.colorMask.get(I).colorMask);
                blendEquation.get(I).set(
                    fbLayout.blendStates.get(I).srcRgbFactor,
                    fbLayout.blendStates.get(I).dstRgbFactor,
                    fbLayout.blendStates.get(I).blendOp, // TODO: support for RGB and alpha blend op
                    fbLayout.blendStates.get(I).srcAlphaFactor,
                    fbLayout.blendStates.get(I).dstAlphaFactor,
                    fbLayout.blendStates.get(I).blendOp  // TODO: support for RGB and alpha blend op
                );
            }

            // not supported by RenderDoc
            // requires dynamic state 3 or Vulkan API 1.4
            vkCmdSetColorBlendEquationEXT(cmdBuf, 0, blendEquation);
            vkCmdSetColorBlendEnableEXT(cmdBuf, 0, blendAttachment);
            vkCmdSetColorWriteMaskEXT(cmdBuf, 0, colorMask);
            vkCmdSetVertexInputEXT(cmdBuf, null, null);
            vkCmdSetLogicOpEnableEXT(cmdBuf, fbLayout.logicOp.enabled);
            vkCmdSetLogicOpEXT(cmdBuf, fbLayout.logicOp.getLogicOp());
        }

        if (cmdInfo.multiDraw != null && cmdInfo.pipeline != 0) {
            // use classic draw if one instance
            if (cmdInfo.multiDraw.remaining() <= 1) {
                vkCmdDraw(cmdBuf, cmdInfo.multiDraw.vertexCount(), 1, cmdInfo.multiDraw.firstVertex(), 0);
            } else {
                vkCmdDrawMultiEXT(cmdBuf, cmdInfo.multiDraw, 1, 0, VkMultiDrawInfoEXT.SIZEOF);
            }
        } else {
            var fbClearC = VkClearAttachment.calloc(fbLayout.formats.remaining());
            var Fs = fbLayout.formats.remaining();
            for (var I=0;I<Fs;I++) {
                fbClearC.get(I).clearValue(fbLayout.attachmentInfos.get(I).clearValue());
                fbClearC.get(I).aspectMask(directInfo.framebufferObj.writingImageViews.get(I).subresourceLayers(0).aspectMask());
                fbClearC.get(I).colorAttachment(I);
            }

            if (cmdInfo.clearColor) {
                vkCmdClearAttachments(cmdBuf, fbClearC, VkClearRect.calloc(1).baseArrayLayer(0).layerCount(layerCount).rect(VkRect2D.calloc().set(fbLayout.scissor)));
            }
            if (hasDepthStencil && cmdInfo.clearDepthStencil) {
                vkCmdClearAttachments(cmdBuf, VkClearAttachment.calloc(1)
                    .clearValue(fbLayout.depthStencilAttachmentInfo.clearValue())
                    .aspectMask(directInfo.framebufferObj.writingDepthStencilImageView.subresourceLayers(0).aspectMask())
                    .colorAttachment(0), VkClearRect.calloc(1).baseArrayLayer(0).layerCount(layerCount).rect(VkRect2D.calloc().set(fbLayout.scissor)));
            }
        }

        //
        vkCmdEndRendering(cmdBuf);
        vkCmdPipelineBarrier2(cmdBuf, VkDependencyInfoKHR.calloc().sType(VK_STRUCTURE_TYPE_DEPENDENCY_INFO).pMemoryBarriers(VkMemoryBarrier2.calloc(1)
            .sType(VK_STRUCTURE_TYPE_MEMORY_BARRIER_2)
            .srcStageMask(VK_PIPELINE_STAGE_2_ALL_GRAPHICS_BIT | VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT)
            .srcAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
            .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
            .dstAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)));
    }



}
