package org.hydra2s.noire.objects;

import org.hydra2s.noire.descriptors.ImageSetCInfo;
import org.hydra2s.noire.descriptors.PipelineCInfo;
import org.hydra2s.noire.descriptors.UtilsCInfo;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.util.Collections;

import static java.lang.Math.min;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.EXTExtendedDynamicState2.vkCmdSetLogicOpEXT;
import static org.lwjgl.vulkan.EXTExtendedDynamicState3.*;
import static org.lwjgl.vulkan.EXTMultiDraw.vkCmdDrawMultiEXT;
import static org.lwjgl.vulkan.EXTVertexInputDynamicState.vkCmdSetVertexInputEXT;
import static org.lwjgl.vulkan.VK10.VK_QUEUE_FAMILY_IGNORED;
import static org.lwjgl.vulkan.VK13.*;
import static org.lwjgl.vulkan.VK13.vkCmdSetStencilOp;

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
        public VkExtent3D dispatch = VkExtent3D.create().width(1).height(1).depth(1);
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

    /*public static class ImageViewCopyInfo {
        // TODO: fix device requirements issues
        public long device = 0L;
        public long imageView = 0L;
        public VkOffset3D offset = VkOffset3D.create().set(0, 0, 0);
        public int mipLevel = 0;
    }*/

    public static class BufferCopyInfo {
        public long buffer = 0L;
        public long offset = 0L;
        public long range = 0L;//VK_WHOLE_SIZE;

        // specific
        public int rowLength = 0;
        public int imageHeight = 0;
    }

    public static class ImageViewInfo {
        public long image = 0L;
        public long imageView = 0L;
        public int imageLayout = VK_IMAGE_LAYOUT_GENERAL;
        public int DSC_ID = -1;
        public ImageViewInfo setImageLayout(int imageLayout) {
            this.imageLayout = imageLayout;
            return this;
        }
    }

    public static class SubresourceRange {
        public ImageViewInfo imageViewInfo = null;
        public VkImageSubresourceRange subresource = VkImageSubresourceRange.create().set(VK_IMAGE_ASPECT_COLOR_BIT, 0, 1, 0, 1);
        public VkOffset3D offset3D = VkOffset3D.create().set(0, 0, 0);
        public VkImageSubresourceLayers getSubresourceLayers(int mipLevel) {
            return VkImageSubresourceLayers.create().set(subresource.aspectMask(), mipLevel, subresource.baseArrayLayer(), subresource.layerCount());
        }
        public SubresourceRange setOffset3D(VkOffset3D offset3D) {
            if (this.offset3D == null) { this.offset3D = offset3D; } else { this.offset3D.set(offset3D); };
            return this;
        }
        public SubresourceRange setImageLayout(int imageLayout) {
            this.imageViewInfo.imageLayout = imageLayout;
            return this;
        }
    }

    public static class SubresourceLayers {
        public ImageViewInfo imageViewInfo = null;
        public VkImageSubresourceLayers subresource = VkImageSubresourceLayers.create().set(VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1);
        public VkOffset3D offset3D = VkOffset3D.create().set(0, 0, 0);
        public VkImageSubresourceRange getSubresourceRange() {
            return VkImageSubresourceRange.create().set(subresource.aspectMask(), subresource.mipLevel(), 1, subresource.baseArrayLayer(), subresource.layerCount());
        }
        public SubresourceLayers setOffset3D(VkOffset3D offset3D) {
            if (this.offset3D == null) { this.offset3D = offset3D; } else { this.offset3D.set(offset3D); };
            return this;
        }
        public SubresourceLayers setImageLayout(int imageLayout) {
            this.imageViewInfo.imageLayout = imageLayout;
            return this;
        }
    }

    //
    static public void cmdCopyImageToImage(VkCommandBuffer cmdBuf, SubresourceLayers srcImage, SubresourceLayers dstImage, VkExtent3D extent3D) {
        try ( MemoryStack stack = stackPush() ) {
            //
            var readMemoryBarrierTemplate = VkImageMemoryBarrier2.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER_2)
                .srcStageMask(VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT | VK_PIPELINE_STAGE_2_HOST_BIT | VK_PIPELINE_STAGE_2_COPY_BIT)
                .srcAccessMask(VK_ACCESS_2_MEMORY_READ_BIT | VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_TRANSFER_READ_BIT | VK_ACCESS_2_TRANSFER_WRITE_BIT)
                .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
                .dstAccessMask(VK_ACCESS_2_MEMORY_READ_BIT | VK_ACCESS_2_MEMORY_WRITE_BIT)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .oldLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                .newLayout(srcImage.imageViewInfo.imageLayout);

            //
            var writeMemoryBarrierTemplate = VkImageMemoryBarrier2.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER_2)
                .srcStageMask(VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT | VK_PIPELINE_STAGE_2_COPY_BIT)
                .srcAccessMask(VK_ACCESS_2_MEMORY_READ_BIT | VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_TRANSFER_READ_BIT | VK_ACCESS_2_TRANSFER_WRITE_BIT)
                .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
                .dstAccessMask(VK_ACCESS_2_MEMORY_READ_BIT | VK_ACCESS_2_MEMORY_WRITE_BIT)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .oldLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                .newLayout(dstImage.imageViewInfo.imageLayout);

            //
            var preReadMemoryBarrierTemplate = VkImageMemoryBarrier2.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER_2)
                .srcStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
                .srcAccessMask(VK_ACCESS_2_MEMORY_READ_BIT | VK_ACCESS_2_MEMORY_WRITE_BIT)
                .dstStageMask(VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT | VK_PIPELINE_STAGE_2_HOST_BIT | VK_PIPELINE_STAGE_2_COPY_BIT)
                .dstAccessMask(VK_ACCESS_2_MEMORY_READ_BIT | VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_TRANSFER_READ_BIT | VK_ACCESS_2_TRANSFER_WRITE_BIT)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .oldLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                .newLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);

            //
            var preWriteMemoryBarrierTemplate = VkImageMemoryBarrier2.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER_2)
                .srcStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
                .srcAccessMask(VK_ACCESS_2_MEMORY_READ_BIT | VK_ACCESS_2_MEMORY_WRITE_BIT)
                .dstStageMask(VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT | VK_PIPELINE_STAGE_2_COPY_BIT)
                .dstAccessMask(VK_ACCESS_2_MEMORY_READ_BIT | VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_TRANSFER_READ_BIT | VK_ACCESS_2_TRANSFER_WRITE_BIT)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .oldLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                .newLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);

            //
            var imageMemoryBarrier = VkImageMemoryBarrier2.calloc(2, stack);
            imageMemoryBarrier.get(0).set(readMemoryBarrierTemplate).image(srcImage.imageViewInfo.image).subresourceRange(srcImage.getSubresourceRange());
            imageMemoryBarrier.get(1).set(writeMemoryBarrierTemplate).image(dstImage.imageViewInfo.image).subresourceRange(dstImage.getSubresourceRange());

            //
            var preImageMemoryBarrier = VkImageMemoryBarrier2.calloc(2, stack);
            preImageMemoryBarrier.get(0).set(preReadMemoryBarrierTemplate).image(srcImage.imageViewInfo.image).subresourceRange(srcImage.getSubresourceRange());
            preImageMemoryBarrier.get(1).set(preWriteMemoryBarrierTemplate).image(dstImage.imageViewInfo.image).subresourceRange(dstImage.getSubresourceRange());

            //
            var imageCopyRegion = VkImageCopy2.calloc(1, stack).sType(VK_STRUCTURE_TYPE_IMAGE_COPY_2).dstSubresource(dstImage.subresource).srcSubresource(srcImage.subresource).srcOffset(srcImage.offset3D).dstOffset(dstImage.offset3D).extent(extent3D);
            vkCmdPipelineBarrier2(cmdBuf, VkDependencyInfoKHR.calloc(stack).sType(VK_STRUCTURE_TYPE_DEPENDENCY_INFO).pImageMemoryBarriers(preImageMemoryBarrier));
            vkCmdCopyImage2(cmdBuf, VkCopyImageInfo2.calloc(stack).sType(VK_STRUCTURE_TYPE_COPY_IMAGE_INFO_2).dstImage(dstImage.imageViewInfo.image).dstImageLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL).srcImageLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL).srcImage(srcImage.imageViewInfo.image).pRegions(imageCopyRegion));
            vkCmdPipelineBarrier2(cmdBuf, VkDependencyInfoKHR.calloc(stack).sType(VK_STRUCTURE_TYPE_DEPENDENCY_INFO).pImageMemoryBarriers(imageMemoryBarrier));
        }
    }

    //
    static public void cmdCopyImageToBuffer(VkCommandBuffer cmdBuf, SubresourceLayers srcImage, BufferCopyInfo dstBuffer, VkExtent3D extent3D) {
        try ( MemoryStack stack = stackPush() ) {
            //
            var readMemoryBarrierTemplate = VkImageMemoryBarrier2.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER_2)
                .srcStageMask(VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT | VK_PIPELINE_STAGE_2_HOST_BIT | VK_PIPELINE_STAGE_2_COPY_BIT)
                .srcAccessMask(VK_ACCESS_2_MEMORY_READ_BIT | VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_TRANSFER_READ_BIT | VK_ACCESS_2_TRANSFER_WRITE_BIT)
                .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
                .dstAccessMask(VK_ACCESS_2_MEMORY_READ_BIT | VK_ACCESS_2_MEMORY_WRITE_BIT)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .oldLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                .newLayout(srcImage.imageViewInfo.imageLayout);

            //
            var writeMemoryBarrierTemplate = VkBufferMemoryBarrier2.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER_2)
                .srcStageMask(VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT | VK_PIPELINE_STAGE_2_COPY_BIT)
                .srcAccessMask(VK_ACCESS_2_MEMORY_READ_BIT | VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_TRANSFER_READ_BIT | VK_ACCESS_2_TRANSFER_WRITE_BIT)
                .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
                .dstAccessMask(VK_ACCESS_2_MEMORY_READ_BIT | VK_ACCESS_2_MEMORY_WRITE_BIT)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);

            //
            var preReadMemoryBarrierTemplate = VkImageMemoryBarrier2.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER_2)
                .srcStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
                .srcAccessMask(VK_ACCESS_2_MEMORY_READ_BIT | VK_ACCESS_2_MEMORY_WRITE_BIT)
                .dstStageMask(VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT | VK_PIPELINE_STAGE_2_HOST_BIT | VK_PIPELINE_STAGE_2_COPY_BIT)
                .dstAccessMask(VK_ACCESS_2_MEMORY_READ_BIT | VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_TRANSFER_READ_BIT | VK_ACCESS_2_TRANSFER_WRITE_BIT)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .oldLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                .newLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);

            //
            var imageMemoryBarrier = VkImageMemoryBarrier2.calloc(1, stack);
            var bufferMemoryBarrier = VkBufferMemoryBarrier2.calloc(1, stack);

            //
            imageMemoryBarrier.get(0).set(readMemoryBarrierTemplate).image(srcImage.imageViewInfo.image).subresourceRange(srcImage.getSubresourceRange());
            bufferMemoryBarrier.get(0).set(writeMemoryBarrierTemplate).buffer(dstBuffer.buffer).offset(dstBuffer.offset).size(dstBuffer.range);

            //
            var preImageMemoryBarrier = VkImageMemoryBarrier2.calloc(1, stack);
            preImageMemoryBarrier.get(0).set(preReadMemoryBarrierTemplate).image(srcImage.imageViewInfo.image).subresourceRange(srcImage.getSubresourceRange());

            //
            var imageBufferCopyRegion = VkBufferImageCopy2.calloc(1, stack).sType(VK_STRUCTURE_TYPE_BUFFER_IMAGE_COPY_2).imageSubresource(srcImage.subresource).imageOffset(srcImage.offset3D).imageSubresource(srcImage.subresource).bufferOffset(dstBuffer.offset).bufferRowLength(dstBuffer.rowLength).bufferImageHeight(dstBuffer.imageHeight).imageExtent(extent3D);
            vkCmdPipelineBarrier2(cmdBuf, VkDependencyInfoKHR.calloc(stack).sType(VK_STRUCTURE_TYPE_DEPENDENCY_INFO).pImageMemoryBarriers(preImageMemoryBarrier));
            vkCmdCopyImageToBuffer2(cmdBuf, VkCopyImageToBufferInfo2.calloc(stack).sType(VK_STRUCTURE_TYPE_COPY_IMAGE_TO_BUFFER_INFO_2).srcImageLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL).srcImage(srcImage.imageViewInfo.image).dstBuffer(dstBuffer.buffer).pRegions(imageBufferCopyRegion));
            vkCmdPipelineBarrier2(cmdBuf, VkDependencyInfoKHR.calloc(stack).sType(VK_STRUCTURE_TYPE_DEPENDENCY_INFO).pBufferMemoryBarriers(bufferMemoryBarrier).pImageMemoryBarriers(imageMemoryBarrier));
        }
    }

    //
    static public void cmdCopyBufferToImage(VkCommandBuffer cmdBuf, BufferCopyInfo srcBuffer, SubresourceLayers dstImage,VkExtent3D extent3D) {
        try ( MemoryStack stack = stackPush() ) {
            //
            var readMemoryBarrierTemplate = VkBufferMemoryBarrier2.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER_2)
                .srcStageMask(VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT | VK_PIPELINE_STAGE_2_HOST_BIT | VK_PIPELINE_STAGE_2_COPY_BIT)
                .srcAccessMask(VK_ACCESS_2_MEMORY_READ_BIT | VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_TRANSFER_READ_BIT | VK_ACCESS_2_TRANSFER_WRITE_BIT)
                .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
                .dstAccessMask(VK_ACCESS_2_MEMORY_READ_BIT | VK_ACCESS_2_MEMORY_WRITE_BIT)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);

            //
            var writeMemoryBarrierTemplate = VkImageMemoryBarrier2.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER_2)
                .srcStageMask(VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT | VK_PIPELINE_STAGE_2_COPY_BIT)
                .srcAccessMask(VK_ACCESS_2_MEMORY_READ_BIT | VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_TRANSFER_READ_BIT | VK_ACCESS_2_TRANSFER_WRITE_BIT)
                .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
                .dstAccessMask(VK_ACCESS_2_MEMORY_READ_BIT | VK_ACCESS_2_MEMORY_WRITE_BIT)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .oldLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                .newLayout(dstImage.imageViewInfo.imageLayout);

            //
            var preWriteMemoryBarrierTemplate = VkImageMemoryBarrier2.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER_2)
                .srcStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
                .srcAccessMask(VK_ACCESS_2_MEMORY_READ_BIT | VK_ACCESS_2_MEMORY_WRITE_BIT)
                .dstStageMask(VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT | VK_PIPELINE_STAGE_2_COPY_BIT)
                .dstAccessMask(VK_ACCESS_2_MEMORY_READ_BIT | VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_TRANSFER_READ_BIT | VK_ACCESS_2_TRANSFER_WRITE_BIT)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .oldLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                .newLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL);

            //
            var imageMemoryBarrier = VkImageMemoryBarrier2.calloc(1, stack);
            var bufferMemoryBarrier = VkBufferMemoryBarrier2.calloc(1, stack);

            //
            imageMemoryBarrier.get(0).set(writeMemoryBarrierTemplate).image(dstImage.imageViewInfo.image).subresourceRange(dstImage.getSubresourceRange());
            bufferMemoryBarrier.get(0).set(readMemoryBarrierTemplate).buffer(srcBuffer.buffer).offset(srcBuffer.offset).size(srcBuffer.range);

            //
            var preImageMemoryBarrier = VkImageMemoryBarrier2.calloc(1, stack);
            preImageMemoryBarrier.get(0).set(preWriteMemoryBarrierTemplate).image(dstImage.imageViewInfo.image).subresourceRange(dstImage.getSubresourceRange());

            //
            var imageBufferCopyRegion = VkBufferImageCopy2.calloc(1, stack).sType(VK_STRUCTURE_TYPE_BUFFER_IMAGE_COPY_2).imageOffset(dstImage.offset3D).imageSubresource(dstImage.subresource).imageSubresource(dstImage.subresource).bufferOffset(srcBuffer.offset).bufferRowLength(srcBuffer.rowLength).bufferImageHeight(srcBuffer.imageHeight).imageExtent(extent3D);
            vkCmdPipelineBarrier2(cmdBuf, VkDependencyInfoKHR.calloc(stack).sType(VK_STRUCTURE_TYPE_DEPENDENCY_INFO).pImageMemoryBarriers(preImageMemoryBarrier));
            vkCmdCopyBufferToImage2(cmdBuf, VkCopyBufferToImageInfo2.calloc(stack).sType(VK_STRUCTURE_TYPE_COPY_BUFFER_TO_IMAGE_INFO_2).dstImageLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL).dstImage(dstImage.imageViewInfo.image).srcBuffer(srcBuffer.buffer).pRegions(imageBufferCopyRegion));
            vkCmdPipelineBarrier2(cmdBuf, VkDependencyInfoKHR.calloc(stack).sType(VK_STRUCTURE_TYPE_DEPENDENCY_INFO).pBufferMemoryBarriers(bufferMemoryBarrier).pImageMemoryBarriers(imageMemoryBarrier));
        }
    }

    //
    static public void cmdCopyBufferToBuffer(VkCommandBuffer cmdBuf, BufferCopyInfo srcBuffer, BufferCopyInfo dstBuffer) {
        try ( MemoryStack stack = stackPush() ) {
            var readMemoryBarrierTemplate = VkBufferMemoryBarrier2.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER_2)
                .srcStageMask(VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT | VK_PIPELINE_STAGE_2_HOST_BIT | VK_PIPELINE_STAGE_2_COPY_BIT)
                .srcAccessMask(VK_ACCESS_2_MEMORY_READ_BIT | VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_TRANSFER_READ_BIT | VK_ACCESS_2_TRANSFER_WRITE_BIT)
                .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
                .dstAccessMask(VK_ACCESS_2_MEMORY_READ_BIT | VK_ACCESS_2_MEMORY_WRITE_BIT)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);

            //
            var writeMemoryBarrierTemplate = VkBufferMemoryBarrier2.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER_2)
                .srcStageMask(VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT | VK_PIPELINE_STAGE_2_COPY_BIT)
                .srcAccessMask(VK_ACCESS_2_MEMORY_READ_BIT | VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_TRANSFER_READ_BIT | VK_ACCESS_2_TRANSFER_WRITE_BIT)
                .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
                .dstAccessMask(VK_ACCESS_2_MEMORY_READ_BIT | VK_ACCESS_2_MEMORY_WRITE_BIT)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);

            //
            var bufferMemoryBarrier = VkBufferMemoryBarrier2.calloc(2, stack);
            ;
            bufferMemoryBarrier.get(0).set(readMemoryBarrierTemplate).buffer(srcBuffer.buffer).offset(srcBuffer.offset).size(srcBuffer.range);
            bufferMemoryBarrier.get(1).set(writeMemoryBarrierTemplate).buffer(dstBuffer.buffer).offset(dstBuffer.offset).size(dstBuffer.range);

            //
            var bufferCopyRegion = VkBufferCopy2.calloc(1, stack).sType(VK_STRUCTURE_TYPE_BUFFER_COPY_2).srcOffset(srcBuffer.offset).dstOffset(dstBuffer.offset).size(min(dstBuffer.range, srcBuffer.range));
            vkCmdCopyBuffer2(cmdBuf, VkCopyBufferInfo2.calloc(stack).sType(VK_STRUCTURE_TYPE_COPY_BUFFER_INFO_2).srcBuffer(srcBuffer.buffer).dstBuffer(dstBuffer.buffer).pRegions(bufferCopyRegion));
            vkCmdPipelineBarrier2(cmdBuf, VkDependencyInfoKHR.calloc(stack).sType(VK_STRUCTURE_TYPE_DEPENDENCY_INFO).pBufferMemoryBarriers(bufferMemoryBarrier));
        }
    }

    // TODO: support for queue families
    public static void cmdTransitionBarrier(VkCommandBuffer cmdBuf, SubresourceRange image) {
        try ( MemoryStack stack = stackPush() ) {
            // get correct access mask by image layouts
            var dstAccessMask = UtilsCInfo.getCorrectAccessMaskByImageLayout(image.imageViewInfo.imageLayout);
            var srcAccessMask = UtilsCInfo.getCorrectAccessMaskByImageLayout(VK_IMAGE_LAYOUT_UNDEFINED);

            // if undefined, use memory mask
            if (dstAccessMask == 0) { dstAccessMask = VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT; }
            ;
            if (srcAccessMask == 0) { srcAccessMask = VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT; }
            ;

            //
            var srcStageMask = UtilsCInfo.getCorrectPipelineStagesByAccessMask(srcAccessMask);
            var dstStageMask = UtilsCInfo.getCorrectPipelineStagesByAccessMask(dstAccessMask);

            //
            if (srcStageMask == 0) { srcStageMask = VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT; }
            ;
            if (dstStageMask == 0) { dstStageMask = VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT; }
            ;

            // barrier by image layout
            // TODO: support for queue families
            var memoryBarrier = VkImageMemoryBarrier2.calloc(1, stack)
                .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER_2)
                .srcStageMask(srcStageMask)
                .srcAccessMask(srcAccessMask)
                .dstStageMask(dstStageMask)
                .dstAccessMask(dstAccessMask)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .oldLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                .newLayout(image.imageViewInfo.imageLayout)
                .subresourceRange(image.subresource)
                .image(image.imageViewInfo.image);

            //
            vkCmdPipelineBarrier2(cmdBuf, VkDependencyInfoKHR.calloc(stack).sType(VK_STRUCTURE_TYPE_DEPENDENCY_INFO).pImageMemoryBarriers(memoryBarrier));
        }
    }

    // virtual buffer copying version
    public static void cmdCopyVBufferToVBuffer(VkCommandBuffer cmdBuf, VkDescriptorBufferInfo src, VkDescriptorBufferInfo dst) {
        CommandUtils.cmdCopyBufferToBuffer(cmdBuf, new BufferCopyInfo(){{
            buffer = src.buffer();
            offset = src.offset();
            range = src.range();
        }}, new BufferCopyInfo(){{
            buffer = dst.buffer();
            offset = dst.offset();
            range = dst.range();
        }});
    }

    // TODO: support for queue families
    public static void cmdSynchronizeFromHost(VkCommandBuffer cmdBuf, VkDescriptorBufferInfo range) {
        // for `map()` or copy operations
        // TODO: support for queue families
        try ( MemoryStack stack = stackPush() ) {
            var bufferMemoryBarrier = VkBufferMemoryBarrier2.calloc(1, stack)
                .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER_2)
                .srcStageMask(VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT | VK_PIPELINE_STAGE_2_HOST_BIT | VK_PIPELINE_STAGE_2_COPY_BIT)
                .srcAccessMask(VK_ACCESS_2_TRANSFER_WRITE_BIT | VK_ACCESS_2_HOST_WRITE_BIT)
                .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
                .dstAccessMask(VK_ACCESS_2_MEMORY_READ_BIT)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .buffer(range.buffer())
                .offset(range.offset())
                .size(range.range()); // TODO: support partial synchronization

            //
            vkCmdPipelineBarrier2(cmdBuf, VkDependencyInfoKHR.calloc(stack).sType(VK_STRUCTURE_TYPE_DEPENDENCY_INFO).pBufferMemoryBarriers(bufferMemoryBarrier));
        }
    }

    //
    public static void cmdDispatch(VkCommandBuffer cmdBuf, ComputeDispatchInfo cmdInfo) {
        var $deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(cmdInfo.device).orElse(null);;
        var $pipelineObj = /*(cmdInfo.pipelineLayout == 0 || cmdInfo.fbLayout == null) ?*/ (PipelineObj)$deviceObj.handleMap.get(new UtilsCInfo.Handle("Pipeline", cmdInfo.pipeline)).orElse(null) /*: null*/;
        var $pipelineLayout = cmdInfo.pipelineLayout != 0 ? cmdInfo.pipelineLayout : ((PipelineCInfo.ComputePipelineCInfo)$pipelineObj.cInfo).pipelineLayout;
        var $pipelineLayoutObj = (PipelineLayoutObj)$deviceObj.handleMap.get(new UtilsCInfo.Handle("PipelineLayout", $pipelineLayout)).orElse(null);;

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
            directInfo.pipelineLayoutObj.cmdBindBuffers(cmdBuf, VK_PIPELINE_BIND_POINT_COMPUTE, /*directInfo.pipelineObj.uniformDescriptorSet != null ? directInfo.pipelineObj.uniformDescriptorSet :*/ null);
        }

        //
        vkCmdBindPipeline(cmdBuf, VK_PIPELINE_BIND_POINT_COMPUTE, cmdInfo.pipeline);
        vkCmdDispatch(cmdBuf, cmdInfo.dispatch.width(), cmdInfo.dispatch.height(), cmdInfo.dispatch.depth());
        /*vkCmdPipelineBarrier2(cmdBuf, VkDependencyInfoKHR.calloc(stack).sType(VK_STRUCTURE_TYPE_DEPENDENCY_INFO).pMemoryBarriers(VkMemoryBarrier2.calloc(1, stack)
            .sType(VK_STRUCTURE_TYPE_MEMORY_BARRIER_2)
            .srcStageMask(VK_PIPELINE_STAGE_2_COMPUTE_SHADER_BIT)
            .srcAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
            .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
            .dstAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)));*/
    }

    //
    public static void preInitializeFb(long device, long imageSet, ImageSetCInfo.FBLayout fbLayout) {
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(device).orElse(null);;
        var framebufferObj = (ImageSetObj.FramebufferObj)deviceObj.handleMap.get(new UtilsCInfo.Handle("ImageSet", imageSet)).orElse(null);;

        //
        fbLayout.attachmentInfos = fbLayout.attachmentInfos != null ? fbLayout.attachmentInfos : VkRenderingAttachmentInfo.create(fbLayout.formats.length);
        var Fs = fbLayout.formats.length;
        for (var I=0;I<Fs;I++) {
            fbLayout.attachmentInfos.get(I)
                .sType(VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO)
                .imageLayout(framebufferObj.writingImageViews.get(I).getImageLayout())
                .imageView(framebufferObj.writingImageViews.get(I).handle.get())
                .loadOp(VK_ATTACHMENT_LOAD_OP_LOAD)
                .storeOp(VK_ATTACHMENT_STORE_OP_STORE);
        }

        //
        boolean hasDepthStencil = fbLayout.depthStencilFormat != VK_FORMAT_UNDEFINED;
        boolean hasDepth = fbLayout.depthStencilFormat != VK_FORMAT_UNDEFINED;
        boolean hasStencil = fbLayout.depthStencilFormat != VK_FORMAT_UNDEFINED;

        //
        if (hasDepthStencil) {
            fbLayout.depthStencilAttachmentInfo
                .sType(VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO)
                .imageView(framebufferObj.readingDepthStencilImageView.getHandle().get())
                .imageLayout(framebufferObj.readingDepthStencilImageView.getImageLayout())
                .loadOp(VK_ATTACHMENT_LOAD_OP_LOAD)
                .storeOp(VK_ATTACHMENT_STORE_OP_STORE);
        };
    }

    //
    public static void cmdDraw(VkCommandBuffer cmdBuf, GraphicsDrawInfo cmdInfo) {
        var $deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(cmdInfo.device).orElse(null);;
        var $pipelineObj = /*(cmdInfo.pipelineLayout == 0 || cmdInfo.fbLayout == null) ?*/ (PipelineObj)$deviceObj.handleMap.get(new UtilsCInfo.Handle("Pipeline", cmdInfo.pipeline)).orElse(null) /*: null*/;
        var $pipelineLayout = cmdInfo.pipelineLayout != 0 ? cmdInfo.pipelineLayout : ((PipelineCInfo.ComputePipelineCInfo)$pipelineObj.cInfo).pipelineLayout;
        var $pipelineLayoutObj = (PipelineLayoutObj)$deviceObj.handleMap.get(new UtilsCInfo.Handle("PipelineLayout", $pipelineLayout)).orElse(null);;
        var $framebufferObj = (ImageSetObj.FramebufferObj)$deviceObj.handleMap.get(new UtilsCInfo.Handle("ImageSet", cmdInfo.imageSet)).orElse(null);;

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
        try ( MemoryStack stack = stackPush() ) {
            // corrupted
            if (cmdInfo.multiDraw != null && cmdInfo.multiDraw.remaining() <= 0) { return; }

            //
            var fbLayout = cmdInfo.fbLayout != null ? cmdInfo.fbLayout : ((PipelineCInfo.GraphicsPipelineCInfo) directInfo.pipelineObj.cInfo).fbLayout;
            int layerCount = Collections.min(fbLayout.layerCounts);
            var physicalDeviceObj = directInfo.deviceObj.physicalDeviceObj;

            //
            boolean hasDepthStencil = fbLayout.depthStencilFormat != VK_FORMAT_UNDEFINED;
            boolean hasDepth = fbLayout.depthStencilFormat != VK_FORMAT_UNDEFINED;
            boolean hasStencil = fbLayout.depthStencilFormat != VK_FORMAT_UNDEFINED;

            //
            vkCmdBeginRendering(cmdBuf, VkRenderingInfoKHR.calloc(stack)
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
                directInfo.pipelineLayoutObj.cmdBindBuffers(cmdBuf, VK_PIPELINE_BIND_POINT_GRAPHICS, /*directInfo.pipelineObj != null && directInfo.pipelineObj.uniformDescriptorSet != null ? directInfo.pipelineObj.uniformDescriptorSet :*/ null);
            }

            //
            //assert directInfo.pipelineObj != null;
            var pipeline = directInfo.pipelineObj != null ? ((PipelineObj.GraphicsPipelineObj)directInfo.pipelineObj).getStatePipeline(fbLayout) : 0L;
            if (pipeline != 0) {
                vkCmdBindPipeline(cmdBuf, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline);

                //
                vkCmdSetCullMode(cmdBuf, fbLayout.cullState ? VK_CULL_MODE_BACK_BIT : VK_CULL_MODE_NONE);
                vkCmdSetDepthBiasEnable(cmdBuf, fbLayout.depthBias.enabled);
                vkCmdSetDepthBias(cmdBuf, fbLayout.depthBias.units, 0.0f, fbLayout.depthBias.factor);

                // boss fight
                vkCmdSetStencilTestEnable(cmdBuf, fbLayout.stencilState.enabled);
                vkCmdSetStencilOp(cmdBuf, VK_STENCIL_FACE_FRONT_AND_BACK, fbLayout.stencilState.stencilOp[0], fbLayout.stencilState.stencilOp[1], fbLayout.stencilState.stencilOp[2], fbLayout.stencilState.compareOp) ;
                vkCmdSetStencilReference(cmdBuf, VK_STENCIL_FACE_FRONT_AND_BACK, fbLayout.stencilState.reference);
                vkCmdSetStencilCompareMask(cmdBuf, VK_STENCIL_FACE_FRONT_AND_BACK, fbLayout.stencilState.compareMask);
                vkCmdSetStencilWriteMask(cmdBuf, VK_STENCIL_FACE_FRONT_AND_BACK, fbLayout.stencilState.writeMask);

                //
                vkCmdSetDepthWriteEnable(cmdBuf, fbLayout.depthState.depthMask);
                vkCmdSetDepthTestEnable(cmdBuf, fbLayout.depthState.depthTest);
                vkCmdSetDepthCompareOp(cmdBuf, fbLayout.depthState.function);

                //
                vkCmdSetScissorWithCount(cmdBuf, VkRect2D.calloc(1, stack).put(0, fbLayout.scissor));
                vkCmdSetViewportWithCount(cmdBuf, VkViewport.calloc(1, stack).put(0, fbLayout.viewport));
                vkCmdSetFrontFace(cmdBuf, VK_FRONT_FACE_COUNTER_CLOCKWISE);

                //
                var Bs = fbLayout.blendStates.size();
                var blendEquation = VkColorBlendEquationEXT.calloc(Bs, stack);
                var blendAttachment = stack.callocInt(Bs);
                var colorMask = stack.callocInt(Bs);
                for (var I = 0; I < Bs; I++) {
                    blendAttachment.put(I, fbLayout.blendStates.get(I).enabled ? 1 : 0);
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
                // TODO: tracking changes
                if (physicalDeviceObj.features.dynamicState3.extendedDynamicState3ColorBlendEquation()) {
                    vkCmdSetColorBlendEquationEXT(cmdBuf, 0, blendEquation);
                }
                if (physicalDeviceObj.features.dynamicState3.extendedDynamicState3ColorBlendEnable()) {
                    vkCmdSetColorBlendEnableEXT(cmdBuf, 0, blendAttachment);
                }
                if (physicalDeviceObj.features.dynamicState3.extendedDynamicState3ColorWriteMask()) {
                    vkCmdSetColorWriteMaskEXT(cmdBuf, 0, colorMask);
                }
                if (physicalDeviceObj.features.vertexInput.vertexInputDynamicState()) {
                    vkCmdSetVertexInputEXT(cmdBuf, null, null);
                }
                if (physicalDeviceObj.features.dynamicState3.extendedDynamicState3LogicOpEnable()) {
                    vkCmdSetLogicOpEnableEXT(cmdBuf, fbLayout.logicOp.enabled);
                }
                if (physicalDeviceObj.features.dynamicState2.extendedDynamicState2LogicOp()) {
                    vkCmdSetLogicOpEXT(cmdBuf, fbLayout.logicOp.getLogicOp());
                }
            }

            if (cmdInfo.multiDraw != null && cmdInfo.pipeline != 0) {
                // use classic draw if one instance
                if (cmdInfo.multiDraw.remaining() <= 1) {
                    vkCmdDraw(cmdBuf, cmdInfo.multiDraw.vertexCount(), 1, cmdInfo.multiDraw.firstVertex(), 0);
                } else {
                    vkCmdDrawMultiEXT(cmdBuf, cmdInfo.multiDraw, 1, 0, VkMultiDrawInfoEXT.SIZEOF);
                }
            } else {
                var fbClearC = VkClearAttachment.calloc(fbLayout.formats.length, stack);
                var Fs = fbLayout.formats.length;
                for (var I = 0; I < Fs; I++) {
                    fbClearC.get(I).clearValue(fbLayout.attachmentInfos.get(I).clearValue());
                    fbClearC.get(I).aspectMask(directInfo.framebufferObj.writingImageViews.get(I).subresourceLayers(0).subresource.aspectMask());
                    fbClearC.get(I).colorAttachment(I);
                }

                if (cmdInfo.clearColor) {
                    vkCmdClearAttachments(cmdBuf, fbClearC, VkClearRect.calloc(1, stack).baseArrayLayer(0).layerCount(layerCount).rect(VkRect2D.calloc(stack).set(fbLayout.scissor)));
                }
                if (hasDepthStencil && cmdInfo.clearDepthStencil) {
                    vkCmdClearAttachments(cmdBuf, VkClearAttachment.calloc(1, stack)
                        .clearValue(fbLayout.depthStencilAttachmentInfo.clearValue())
                        .aspectMask(directInfo.framebufferObj.readingDepthStencilImageView.subresourceLayers(0).subresource.aspectMask())
                        .colorAttachment(0), VkClearRect.calloc(1, stack).baseArrayLayer(0).layerCount(layerCount).rect(VkRect2D.calloc(stack).set(fbLayout.scissor)));
                }
            }

            //
            vkCmdEndRendering(cmdBuf);
        /*vkCmdPipelineBarrier2(cmdBuf, VkDependencyInfoKHR.calloc(stack).sType(VK_STRUCTURE_TYPE_DEPENDENCY_INFO).pMemoryBarriers(VkMemoryBarrier2.create(1, stack)
            .sType(VK_STRUCTURE_TYPE_MEMORY_BARRIER_2)
            .srcStageMask(VK_PIPELINE_STAGE_2_ALL_GRAPHICS_BIT | VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT)
            .srcAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
            .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
            .dstAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)));*/
        }
    }



}
