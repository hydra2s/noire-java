package org.hydra2s.noire.descriptors;

//
import org.lwjgl.vulkan.*;

//
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Objects;

//
import static org.lwjgl.vulkan.VK10.VK_QUEUE_FAMILY_IGNORED;
import static org.lwjgl.vulkan.VK13.*;

//
public class ImageSetCInfo extends BasicCInfo  {
    public long pipelineLayout = 0L;
    public long memoryAllocator = 0L;

    //
    public IntBuffer formats = null;
    public ArrayList<Integer> layerCounts = new ArrayList<Integer>();
    public ArrayList<VkExtent3D> extents = new ArrayList<VkExtent3D>();

    //
    public VkImageMemoryBarrier2 attachmentBarrier = VkImageMemoryBarrier2.calloc()
        .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER_2)
        .srcStageMask(VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT)
        .srcAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
        .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
        .dstAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
        .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
        .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED);

    //
    public static class DepthState {
        public final boolean depthTest;
        public final boolean depthMask;
        public final int function;

        public DepthState(boolean depthTest, boolean depthMask, int function) {
            this.depthTest = depthTest;
            this.depthMask = depthMask;
            this.function = glToVulkan(function);
        }

        private static int glToVulkan(int value) {
            return switch (value) {
                case 515 -> VK_COMPARE_OP_LESS_OR_EQUAL;
                case 519 -> VK_COMPARE_OP_ALWAYS;
                case 516 -> VK_COMPARE_OP_GREATER;
                case 518 -> VK_COMPARE_OP_GREATER_OR_EQUAL;
                case 514 -> VK_COMPARE_OP_EQUAL;
                default -> throw new RuntimeException("unknown blend factor..");
            };
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DepthState that = (DepthState) o;
            return depthTest == that.depthTest && depthMask == that.depthMask && function == that.function;
        }

        @Override
        public int hashCode() {
            return Objects.hash(depthTest, depthMask, function);
        }
    }

    //
    public static class BlendState {
        public final boolean enabled;
        public final int srcRgbFactor;
        public final int dstRgbFactor;
        public final int srcAlphaFactor;
        public final int dstAlphaFactor;
        public final int blendOp = 0;

        public BlendState(int srcRgb, int dstRgb, int srcAlpha, int dstAlpha) {
            this(true, glToVulkan(srcRgb), glToVulkan(dstRgb), glToVulkan(srcAlpha), glToVulkan(dstAlpha));
        }

        protected BlendState(boolean enabled, int srcRgb, int dstRgb, int srcAlpha, int dstAlpha) {
            this.enabled = enabled;
            this.srcRgbFactor = srcRgb;
            this.dstRgbFactor = dstRgb;
            this.srcAlphaFactor = srcAlpha;
            this.dstAlphaFactor = dstAlpha;
        }

        private static int glToVulkan(int value) {
            return switch (value) {
                case 1 -> VK_BLEND_FACTOR_ONE;
                case 0 -> VK_BLEND_FACTOR_ZERO;
                case 771 -> VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;
                case 770 -> VK_BLEND_FACTOR_SRC_ALPHA;
                case 775 -> VK_BLEND_FACTOR_ONE_MINUS_DST_COLOR;
                case 769 -> VK_BLEND_FACTOR_ONE_MINUS_SRC_COLOR;
                case 774 -> VK_BLEND_FACTOR_DST_COLOR;
                case 768 -> VK_BLEND_FACTOR_SRC_COLOR;
                default -> throw new RuntimeException("unknown blend factor..");
            };
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BlendState blendState = (BlendState) o;
            if(this.enabled != blendState.enabled) return false;
            return srcRgbFactor == blendState.srcRgbFactor && dstRgbFactor == blendState.dstRgbFactor && srcAlphaFactor == blendState.srcAlphaFactor && dstAlphaFactor == blendState.dstAlphaFactor && blendOp == blendState.blendOp;
        }

        @Override
        public int hashCode() {
            return Objects.hash(srcRgbFactor, dstRgbFactor, srcAlphaFactor, dstAlphaFactor, blendOp);
        }
    }

    //
    public static class LogicOpState {
        public final boolean enabled;
        private int logicOp;

        public LogicOpState(boolean enable, int op) {
            this.enabled = enable;
            this.logicOp = op;
        }

        public void setLogicOp(int logicOp) {
            this.logicOp = logicOp;
        }

        public int getLogicOp() {
            return logicOp;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LogicOpState logicOpState = (LogicOpState) o;
            if(this.enabled != logicOpState.enabled) return false;
            return logicOp == logicOpState.logicOp;
        }

        public int hashCode() {
            return Objects.hash(enabled, logicOp);
        }
    }

    //
    static public class FBLayout extends ImageSetCInfo {

        // planned a auto-detection
        public int depthStencilFormat = 0;
        public VkRenderingAttachmentInfo depthStencilAttachmentInfo = null;
        public VkRect2D scissor = null;
        public VkViewport viewport = null;

        //
        public VkRenderingAttachmentInfo.Buffer attachmentInfos = null;
        public VkPipelineColorBlendAttachmentState.Buffer blendAttachments = null;

        //
        public VkImageMemoryBarrier2 depthStencilBarrier = VkImageMemoryBarrier2.calloc()
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
