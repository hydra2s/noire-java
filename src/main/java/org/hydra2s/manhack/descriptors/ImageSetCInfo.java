package org.hydra2s.manhack.descriptors;

//
import org.lwjgl.vulkan.*;

//
import java.nio.IntBuffer;
import java.util.ArrayList;

//
import static org.lwjgl.vulkan.VK10.VK_QUEUE_FAMILY_IGNORED;
import static org.lwjgl.vulkan.VK13.*;
import static org.lwjgl.vulkan.VK13.VK_ACCESS_2_MEMORY_READ_BIT;

//
public class ImageSetCInfo extends BasicCInfo  {
    //
    public int layerCount = 1;
    public long pipelineLayout = 0L;
    public long memoryAllocator = 0L;

    //
    public IntBuffer formats = null;
    public VkRenderingAttachmentInfo.Buffer attachmentInfos = null;
    public VkPipelineColorBlendAttachmentState.Buffer blendAttachments = null;
    public ArrayList<VkExtent3D> extents = new ArrayList<VkExtent3D>();

    //
    public VkImageMemoryBarrier2 attachmentBarrier = VkImageMemoryBarrier2.create()
        .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER_2)
        .srcStageMask(VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT)
        .srcAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
        .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
        .dstAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
        .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
        .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);

    //
    static public class FBLayout extends ImageSetCInfo {

        // planned a auto-detection
        public int depthStencilFormat = 0;
        public VkRenderingAttachmentInfo depthStencilAttachmentInfo = null;
        public VkRect2D scissor = null;
        public VkViewport viewport = null;

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
        public FBLayout() {

        }

    }

}
