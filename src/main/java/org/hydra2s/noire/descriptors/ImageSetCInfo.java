package org.hydra2s.noire.descriptors;

//
import com.mojang.blaze3d.platform.GlStateManager;
import net.vulkanmod.vulkan.Pipeline;
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

    public static class DepthBias {
        //
        public boolean enabled = false;
         public float units = 0.F;
         public float factor = 0.F;

        //
        public DepthBias(boolean enabled, float units, float factor) {
            this.enabled = enabled;
            this.units = units;
            this.factor = factor;
        }
    }

    //
    public static class DepthState {
        //
        public boolean depthTest;
        public boolean depthMask;
        public int function;

        public DepthState(boolean depthTest, boolean depthMask, int function) {
            this.depthTest = depthTest;
            this.depthMask = depthMask;
            this.function = function;//glToVulkan(function);
        }

        public static int glToVulkan(int value) {
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
        public int srcRgbFactor;
        public int dstRgbFactor;
        public int srcAlphaFactor;
        public int dstAlphaFactor;
        public final int blendOp = 0;

        public BlendState(int srcRgb, int dstRgb, int srcAlpha, int dstAlpha) {
            this(true, glToVulkan(srcRgb), glToVulkan(dstRgb), glToVulkan(srcAlpha), glToVulkan(dstAlpha));
        }

        public BlendState(boolean enabled, int srcRgb, int dstRgb, int srcAlpha, int dstAlpha) {
            this.enabled = enabled;
            this.srcRgbFactor = srcRgb;
            this.dstRgbFactor = dstRgb;
            this.srcAlphaFactor = srcAlpha;
            this.dstAlphaFactor = dstAlpha;
        }

        public void setBlendFunction(int srcRgb, int dstRgb, int srcAlpha, int dstAlpha) {
            this.srcRgbFactor = srcRgb;
            this.dstRgbFactor = dstRgb;
            this.srcAlphaFactor = srcAlpha;
            this.dstAlphaFactor = dstAlpha;
        }

        public static int glToVulkan(int value) {
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

        public static int glToVulkan(int value) {
            return switch (value) {
                case 5377 -> VK_LOGIC_OP_AND;
                case 5380 -> VK_LOGIC_OP_AND_INVERTED;
                case 5378 -> VK_LOGIC_OP_AND_REVERSE;
                case 5376 -> VK_LOGIC_OP_CLEAR;
                case 5379 -> VK_LOGIC_OP_COPY;
                case 5388 -> VK_LOGIC_OP_COPY_INVERTED;
                case 5385 -> VK_LOGIC_OP_EQUIVALENT;
                case 5386 -> VK_LOGIC_OP_INVERT;
                case 5390 -> VK_LOGIC_OP_NAND;
                case 5381 -> VK_LOGIC_OP_NO_OP;
                case 5384 -> VK_LOGIC_OP_NOR;
                case 5383 -> VK_LOGIC_OP_OR;
                case 5389 -> VK_LOGIC_OP_OR_INVERTED;
                case 5387 -> VK_LOGIC_OP_OR_REVERSE;
                case 5391 -> VK_LOGIC_OP_SET;
                case 5382 -> VK_LOGIC_OP_XOR;
                default -> throw new RuntimeException("unknown logic op..");
            };
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
    public static class ColorMask {
        public final int colorMask;

        public ColorMask(boolean r, boolean g, boolean b, boolean a) {
            this.colorMask = (r ? VK_COLOR_COMPONENT_R_BIT : 0) | (g ? VK_COLOR_COMPONENT_G_BIT : 0) | (b ? VK_COLOR_COMPONENT_B_BIT : 0) | (a ? VK_COLOR_COMPONENT_A_BIT : 0);
        }

        public ColorMask(int mask) {
            this.colorMask = mask;
        }

        public static int getColorMask(boolean r, boolean g, boolean b, boolean a) {
            return (r ? VK_COLOR_COMPONENT_R_BIT : 0) | (g ? VK_COLOR_COMPONENT_G_BIT : 0) | (b ? VK_COLOR_COMPONENT_B_BIT : 0) | (a ? VK_COLOR_COMPONENT_A_BIT : 0);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ColorMask colorMask = (ColorMask) o;
            return this.colorMask == colorMask.colorMask;
        }
    }

    public static final LogicOpState DEFAULT_LOGICOP_STATE = new LogicOpState(false, 0);
    public static final ColorMask DEFAULT_COLORMASK = new ColorMask(true, true, true, true);
    public static final BlendState NO_BLEND_STATE = new BlendState(false, 0, 0, 0, 0);
    public static final BlendState DEFAULT_BLEND_STATE = new BlendState(false, VK_BLEND_FACTOR_SRC_ALPHA, VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA, VK_BLEND_FACTOR_ONE, VK_BLEND_FACTOR_ZERO);

    //
    static public class FBLayout extends ImageSetCInfo {

        // planned a auto-detection
        public int depthStencilFormat = 0;
        public VkRenderingAttachmentInfo depthStencilAttachmentInfo = null;
        public VkRect2D scissor = null;
        public VkViewport viewport = null;

        //
        public VkRenderingAttachmentInfo.Buffer attachmentInfos = null;
        public DepthState depthState = new DepthState(false, false, VK_COMPARE_OP_ALWAYS);
        public DepthBias depthBias = new DepthBias(false, 0.F, 0.F);

        public ArrayList<BlendState> blendStates = new ArrayList<>(){{
            add(new BlendState(false, 0, 0, 0, 0));
        }};
        public LogicOpState logicOp = new LogicOpState(false, 0);
        public ArrayList<ColorMask> colorMask = new ArrayList<>(){{
            add(new ColorMask(true, true, true, true));
        }};

        // TODO: bound with vertex state
        public boolean cullState;

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
