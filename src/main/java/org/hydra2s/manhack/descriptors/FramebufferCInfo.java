package org.hydra2s.manhack.descriptors;

import org.lwjgl.vulkan.VkImageMemoryBarrier2;
import org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState;
import org.lwjgl.vulkan.VkRenderingAttachmentInfo;

import java.nio.IntBuffer;

import static org.lwjgl.vulkan.VK10.VK_QUEUE_FAMILY_IGNORED;
import static org.lwjgl.vulkan.VK13.*;

public class FramebufferCInfo extends BasicCInfo  {

    //
    public long pipelineLayout = 0L;

    //
    static public class FBLayout extends BasicCInfo {
        //
        public IntBuffer colorFormats = null;
        public VkRenderingAttachmentInfo.Buffer colorAttachmentInfos = null;
        public VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachments = null;

        // planned a auto-detection
        public int depthStencilFormat = 0;
        public VkRenderingAttachmentInfo depthStencilAttachmentInfo = null;

        //
        public VkImageMemoryBarrier2 depthStencilBarrier = VkImageMemoryBarrier2.create()
            .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER_2)
            .srcStageMask(VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT)
            .srcAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
            .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
            .dstAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
            .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);

        //
        public VkImageMemoryBarrier2 colorBarrier = VkImageMemoryBarrier2.create()
            .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER_2)
            .srcStageMask(VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT)
            .srcAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
            .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
            .dstAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
            .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
            .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);

        //
        public FBLayout() {

        }

    }

}
