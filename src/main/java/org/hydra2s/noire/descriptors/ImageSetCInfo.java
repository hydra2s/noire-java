package org.hydra2s.noire.descriptors;

//

import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Objects;

import static org.lwjgl.vulkan.VK10.VK_QUEUE_FAMILY_IGNORED;
import static org.lwjgl.vulkan.VK13.*;

//
public class ImageSetCInfo extends BasicCInfo  {
    public long pipelineLayout = 0L;
    public long memoryAllocator = 0L;

    //
    public int[] formats = {};
    public ArrayList<Integer> layerCounts = new ArrayList<Integer>();
    public ArrayList<VkExtent3D> extents = new ArrayList<VkExtent3D>();

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

        public DepthBias(DepthBias depthBias) {
            this.enabled = depthBias.enabled;
            this.units = depthBias.units;
            this.factor = depthBias.factor;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DepthBias that = (DepthBias) o;
            return enabled == that.enabled && units == that.units && factor == that.factor;
        }

        @Override
        public int hashCode() {
            return Objects.hash(enabled, units, factor);
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

        public DepthState(DepthState depthState) {
            this.depthTest = depthState.depthTest;
            this.depthMask = depthState.depthMask;
            this.function = depthState.function;//glToVulkan(function);
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
    public static class StencilState {
        public int[] stencilOp = new int[]{ VK_STENCIL_OP_KEEP, VK_STENCIL_OP_KEEP, VK_STENCIL_OP_KEEP };
        public int reference = 2333;
        public int compareOp = VK_COMPARE_OP_ALWAYS;
        public byte writeMask = (byte)0xFF;
        public byte compareMask = (byte)0xFF;
        public boolean enabled = false;

        public StencilState() {

        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            StencilState state = (StencilState)o;
            if(this.enabled != state.enabled) return false;
            return stencilOp == state.stencilOp && reference == state.reference && compareOp == state.compareOp && writeMask == state.writeMask && compareMask == state.compareMask;
        }

        @Override
        public int hashCode() {
            return Objects.hash(enabled, stencilOp, reference, compareOp, writeMask, compareMask);
        }
    }

    //
    public static class BlendState {
        public boolean enabled = false;
        public int srcRgbFactor;
        public int dstRgbFactor;
        public int srcAlphaFactor;
        public int dstAlphaFactor;
        public int blendOp = VK_BLEND_OP_ADD;

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

        public BlendState(BlendState blendState) {
            this.enabled = blendState.enabled;
            this.srcRgbFactor = blendState.srcRgbFactor;
            this.dstRgbFactor = blendState.dstRgbFactor;
            this.srcAlphaFactor = blendState.srcAlphaFactor;
            this.dstAlphaFactor = blendState.dstAlphaFactor;
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

        public BlendState setEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }
    }

    //
    public static class LogicOpState {
        public boolean enabled;
        private int logicOp;

        public LogicOpState(boolean enable, int op) {
            this.enabled = enable;
            this.logicOp = op;
        }

        public LogicOpState(LogicOpState logicOp) {
            this.enabled = logicOp.enabled;
            this.logicOp = logicOp.logicOp;
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
        public int colorMask = VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT;

        public ColorMask(boolean r, boolean g, boolean b, boolean a) {
            this.colorMask =
                (r ? VK_COLOR_COMPONENT_R_BIT : 0) |
                (g ? VK_COLOR_COMPONENT_G_BIT : 0) |
                (b ? VK_COLOR_COMPONENT_B_BIT : 0) |
                (a ? VK_COLOR_COMPONENT_A_BIT : 0);
        }

        public ColorMask(int mask) {
            this.colorMask = mask;
        }

        public ColorMask(ColorMask mask) {
            this.colorMask = mask.colorMask;
        }

        public static int getColorMask(boolean r, boolean g, boolean b, boolean a) {
            return
                (r ? VK_COLOR_COMPONENT_R_BIT : 0) |
                (g ? VK_COLOR_COMPONENT_G_BIT : 0) |
                (b ? VK_COLOR_COMPONENT_B_BIT : 0) |
                (a ? VK_COLOR_COMPONENT_A_BIT : 0);
        }

        //
        @Override
        public int hashCode() {
            return Integer.hashCode(colorMask);
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

        public StencilState stencilState;

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
            add(new BlendState(DEFAULT_BLEND_STATE));
        }};
        public LogicOpState logicOp = new LogicOpState(DEFAULT_LOGICOP_STATE);
        public ArrayList<ColorMask> colorMask = new ArrayList<>(){{
            add(new ColorMask(true, true, true, true));
        }};

        // TODO: bound with vertex state
        public boolean cullState = true;
        //
        public FBLayout() {

        }

        public FBLayout(FBLayout original) {
            // TODO: merge with ImageSet constructor
            this.pipelineLayout = original.pipelineLayout;
            this.memoryAllocator = original.memoryAllocator;

            //
            this.formats = original.formats;//original.formats;
            for (var I=0;I<original.formats.length;I++) {
                this.formats[I] = original.formats[I];
            }

            //
            this.layerCounts = new ArrayList<Integer>(original.layerCounts);
            this.extents = new ArrayList<VkExtent3D>(original.extents);

            // FBLayout statements
            this.cullState = original.cullState;
            this.colorMask = new ArrayList<>();
            original.colorMask.forEach((m)->{
                this.colorMask.add(new ColorMask(m));
            });
            this.blendStates = new ArrayList<>();
            original.blendStates.forEach((b)->{
                this.blendStates.add(new BlendState(b));
            });
            this.logicOp = new LogicOpState(original.logicOp);
            this.depthBias = new DepthBias(original.depthBias);
            this.depthState = new DepthState(original.depthState);
            this.attachmentInfos = VkRenderingAttachmentInfo.create(original.attachmentInfos.remaining());
            for (var I=0;I<original.attachmentInfos.remaining();I++) {
                this.attachmentInfos.put(I, original.attachmentInfos.get(I));
            }
            this.depthStencilFormat = original.depthStencilFormat;
            this.depthStencilAttachmentInfo = VkRenderingAttachmentInfo.create().set(original.depthStencilAttachmentInfo);

            // TODO: stencil state constructor
            this.stencilState = new StencilState();
            this.stencilState.compareMask = original.stencilState.compareMask;
            this.stencilState.compareOp = original.stencilState.compareOp;
            this.stencilState.stencilOp[0] = original.stencilState.stencilOp[0];
            this.stencilState.stencilOp[1] = original.stencilState.stencilOp[1];
            this.stencilState.stencilOp[2] = original.stencilState.stencilOp[2];
            this.stencilState.writeMask = original.stencilState.writeMask;
            this.stencilState.reference = original.stencilState.reference;
            this.stencilState.enabled = original.stencilState.enabled;

            //
            if (original.scissor != null) this.scissor = VkRect2D.create().set(original.scissor);
            if (original.viewport != null) this.viewport = VkViewport.create().set(original.viewport);

        }

        // TODO: full hash with formats
        @Override
        public int hashCode() {
            return Objects.hash(cullState, colorMask, blendStates, logicOp, depthBias, depthState, depthStencilFormat, stencilState);
        }

        // TODO: full hash with formats
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FBLayout layout = (FBLayout) o;
            return
                cullState == layout.cullState &&
                colorMask.equals(layout.colorMask) &&
                blendStates.equals(layout.blendStates) &&
                logicOp.equals(layout.logicOp) &&
                depthBias.equals(layout.depthBias) &&
                depthState.equals(layout.depthState) &&
                depthStencilFormat == layout.depthStencilFormat;
        }

    }

}
