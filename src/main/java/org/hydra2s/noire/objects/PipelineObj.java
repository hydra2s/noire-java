package org.hydra2s.noire.objects;

//
import org.hydra2s.noire.descriptors.*;
import org.hydra2s.utils.Promise;
import org.lwjgl.vulkan.*;

//
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicInteger;

//
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.EXTConservativeRasterization.VK_CONSERVATIVE_RASTERIZATION_MODE_DISABLED_EXT;
import static org.lwjgl.vulkan.EXTConservativeRasterization.VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_CONSERVATIVE_STATE_CREATE_INFO_EXT;
import static org.lwjgl.vulkan.EXTDescriptorBuffer.VK_BUFFER_USAGE_RESOURCE_DESCRIPTOR_BUFFER_BIT_EXT;
import static org.lwjgl.vulkan.EXTDescriptorBuffer.VK_PIPELINE_CREATE_DESCRIPTOR_BUFFER_BIT_EXT;
import static org.lwjgl.vulkan.EXTExtendedDynamicState2.VK_DYNAMIC_STATE_LOGIC_OP_EXT;
import static org.lwjgl.vulkan.EXTExtendedDynamicState2.vkCmdSetLogicOpEXT;
import static org.lwjgl.vulkan.EXTExtendedDynamicState3.*;
import static org.lwjgl.vulkan.EXTMultiDraw.vkCmdDrawMultiEXT;
import static org.lwjgl.vulkan.EXTVertexInputDynamicState.VK_DYNAMIC_STATE_VERTEX_INPUT_EXT;
import static org.lwjgl.vulkan.EXTVertexInputDynamicState.vkCmdSetVertexInputEXT;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK13.*;
import static org.lwjgl.vulkan.VK13.vkCmdSetDepthWriteEnable;

//
public class PipelineObj extends BasicObj  {
    public PipelineObj(Handle base, Handle handle) {
        super(base, handle);
    }
    public PipelineObj(Handle base, PipelineCInfo cInfo) {
        super(base, cInfo);
    }

    //
    public VkPipelineShaderStageCreateInfo createShaderModuleInfo(long module, int stage, CharSequence pName){
        return VkPipelineShaderStageCreateInfo.calloc().sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO).flags(0).module(module).stage(stage).pName(memUTF8(pName)).pSpecializationInfo(null);
    }

    //
    public BufferObj uniformDescriptorBuffer = null;

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
    }

    //
    public static void cmdDispatch(VkCommandBuffer cmdBuf, ComputeDispatchInfo cmdInfo) {
        if (cmdInfo.pushConstRaw != null) {
            vkCmdPushConstants(cmdBuf, cmdInfo.pipelineLayout, VK_SHADER_STAGE_ALL, cmdInfo.pushConstByteOffset, cmdInfo.pushConstRaw);
        }

        //
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(cmdInfo.device);
        var physicalDeviceObj = (PhysicalDeviceObj) BasicObj.globalHandleMap.get(deviceObj.base.get());

        //
        var pipelineObj = (PipelineObj)deviceObj.handleMap.get(new Handle("Pipeline", cmdInfo.pipeline));
        var pipelineLayoutObj = (PipelineLayoutObj)deviceObj.handleMap.get(new Handle("PipelineLayout", cmdInfo.pipelineLayout != 0 ? cmdInfo.pipelineLayout : ((PipelineCInfo.ComputePipelineCInfo)pipelineObj.cInfo).pipelineLayout));

        //
        if (pipelineLayoutObj != null) {
            pipelineLayoutObj.cmdBindBuffers(cmdBuf, VK_PIPELINE_BIND_POINT_COMPUTE, pipelineObj.uniformDescriptorBuffer != null ? pipelineObj.uniformDescriptorBuffer.getHandle().get() : 0L);
        }

        //
        vkCmdBindPipeline(cmdBuf, VK_PIPELINE_BIND_POINT_COMPUTE, cmdInfo.pipeline);
        vkCmdDispatch(cmdBuf, cmdInfo.dispatch.width(), cmdInfo.dispatch.height(), cmdInfo.dispatch.depth());
        vkCmdPipelineBarrier2(cmdBuf, VkDependencyInfoKHR.calloc().sType(VK_STRUCTURE_TYPE_DEPENDENCY_INFO).pMemoryBarriers(VkMemoryBarrier2.calloc(1).sType(VK_STRUCTURE_TYPE_MEMORY_BARRIER_2)
            .srcStageMask(VK_PIPELINE_STAGE_2_COMPUTE_SHADER_BIT)
            .srcAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
            .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
            .dstAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)));
    }

    //
    public static void cmdDraw(VkCommandBuffer cmdBuf, GraphicsDrawInfo cmdInfo) {
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(cmdInfo.device);
        var physicalDeviceObj = (PhysicalDeviceObj) BasicObj.globalHandleMap.get(deviceObj.base.get());

        //
        var pipelineObj = (PipelineObj)deviceObj.handleMap.get(new Handle("Pipeline", cmdInfo.pipeline));
        var pipelineLayout = cmdInfo.pipelineLayout != 0 ? cmdInfo.pipelineLayout : ((PipelineCInfo.ComputePipelineCInfo)pipelineObj.cInfo).pipelineLayout;
        var pipelineLayoutObj = (PipelineLayoutObj)deviceObj.handleMap.get(new Handle("PipelineLayout", pipelineLayout));
        var framebufferObj = (ImageSetObj.FramebufferObj)deviceObj.handleMap.get(new Handle("ImageSet", cmdInfo.imageSet));

        //
        var fbLayout = cmdInfo.fbLayout != null ? cmdInfo.fbLayout : ((PipelineCInfo.GraphicsPipelineCInfo)pipelineObj.cInfo).fbLayout;
        var fbClearC = VkClearAttachment.calloc(fbLayout.formats.remaining());
        fbLayout.attachmentInfos = fbLayout.attachmentInfos != null ? fbLayout.attachmentInfos : VkRenderingAttachmentInfo.calloc(fbLayout.formats.remaining());
        for (var I=0;I<fbLayout.formats.remaining();I++) {
            fbLayout.attachmentInfos.get(I).sType(VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO);
            fbLayout.attachmentInfos.get(I).imageLayout(framebufferObj.writingImageViews.get(I).getImageLayout());
            fbLayout.attachmentInfos.get(I).imageView(framebufferObj.writingImageViews.get(I).handle.get());
            fbLayout.attachmentInfos.get(I).loadOp(VK_ATTACHMENT_LOAD_OP_LOAD);
            fbLayout.attachmentInfos.get(I).storeOp(VK_ATTACHMENT_STORE_OP_STORE);

            fbClearC.get(I).clearValue(fbLayout.attachmentInfos.get(I).clearValue());
            fbClearC.get(I).aspectMask(framebufferObj.writingImageViews.get(I).subresourceLayers(0).aspectMask());
            fbClearC.get(I).colorAttachment(I);
        }

        //
        int layerCount = fbLayout.layerCounts.stream().min(Integer::compare).get();

        //
        boolean hasDepthStencil = fbLayout.depthStencilFormat != VK_FORMAT_UNDEFINED;
        boolean hasDepth = fbLayout.depthStencilFormat != VK_FORMAT_UNDEFINED;
        boolean hasStencil = fbLayout.depthStencilFormat != VK_FORMAT_UNDEFINED;

        //
        if (hasDepthStencil) {
            fbLayout.depthStencilAttachmentInfo.sType(VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO);
            fbLayout.depthStencilAttachmentInfo.imageView(framebufferObj.currentDepthStencilImageView.getHandle().get());
            fbLayout.depthStencilAttachmentInfo.imageLayout(framebufferObj.currentDepthStencilImageView.getImageLayout());
            fbLayout.depthStencilAttachmentInfo.loadOp(VK_ATTACHMENT_LOAD_OP_LOAD);
            fbLayout.depthStencilAttachmentInfo.storeOp(VK_ATTACHMENT_STORE_OP_STORE);
        };

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
        if (cmdInfo.pushConstRaw != null) {
            vkCmdPushConstants(cmdBuf, pipelineLayout, VK_SHADER_STAGE_ALL, cmdInfo.pushConstByteOffset, cmdInfo.pushConstRaw);
        }

        //
        if (pipelineLayoutObj != null) {
            pipelineLayoutObj.cmdBindBuffers(cmdBuf, VK_PIPELINE_BIND_POINT_GRAPHICS, pipelineObj.uniformDescriptorBuffer != null ? pipelineObj.uniformDescriptorBuffer.getHandle().get() : 0L);
        }

        //
        vkCmdBindPipeline(cmdBuf, VK_PIPELINE_BIND_POINT_GRAPHICS, cmdInfo.pipeline);

        //
        if (cmdInfo.pipeline != 0) {
            vkCmdSetLogicOpEnableEXT(cmdBuf, fbLayout.logicOp.enabled);
            vkCmdSetLogicOpEXT(cmdBuf, fbLayout.logicOp.getLogicOp());

            // TODO: add support cull mode from instance
            //vkCmdSetCullMode(cmdBuf, );

            //
            for (var I = 0; I < fbLayout.blendStates.size(); I++) {
                var blendAttachment = fbLayout.blendStates.get(I);
                var blendEquation = VkColorBlendEquationEXT.calloc(1);
                blendEquation.get(0).set(
                    blendAttachment.srcRgbFactor,
                    blendAttachment.dstRgbFactor,
                    blendAttachment.blendOp, // TODO: support for RGB and alpha blend op
                    blendAttachment.srcAlphaFactor,
                    blendAttachment.dstAlphaFactor,
                    blendAttachment.blendOp  // TODO: support for RGB and alpha blend op
                );

                // requires dynamic state 3 or Vulkan API 1.4
                vkCmdSetColorBlendEquationEXT(cmdBuf, I, blendEquation);
                vkCmdSetColorBlendEnableEXT(cmdBuf, I, new int[]{blendAttachment.enabled ? 1 : 0});
                vkCmdSetColorWriteMaskEXT(cmdBuf, I, new int[]{fbLayout.colorMask.get(I).colorMask});
            }

            //
            vkCmdSetDepthBiasEnable(cmdBuf, fbLayout.depthBias.enabled);
            vkCmdSetDepthBias(cmdBuf, fbLayout.depthBias.units, 0.0f, fbLayout.depthBias.factor);

            // TODO: add stencil support
            vkCmdSetStencilTestEnable(cmdBuf, false);

            //
            vkCmdSetDepthWriteEnable(cmdBuf, hasDepth && fbLayout.depthState.depthTest);
            vkCmdSetDepthTestEnable(cmdBuf, fbLayout.depthState.depthTest);
            vkCmdSetDepthCompareOp(cmdBuf, fbLayout.depthState.function);

            //
            vkCmdSetVertexInputEXT(cmdBuf, null, null);
            vkCmdSetScissorWithCount(cmdBuf, VkRect2D.calloc(1).put(0, fbLayout.scissor));
            vkCmdSetViewportWithCount(cmdBuf, VkViewport.calloc(1).put(0, fbLayout.viewport));
        }

        if (cmdInfo.multiDraw != null && cmdInfo.pipeline != 0) {
            // use classic draw if one instance
            if (cmdInfo.multiDraw.remaining() <= 1) {
                vkCmdDraw(cmdBuf, cmdInfo.multiDraw.vertexCount(), 1, cmdInfo.multiDraw.firstVertex(), 0);
            } else {
                vkCmdDrawMultiEXT(cmdBuf, cmdInfo.multiDraw, 1, 0, 8);
            }
        } else {
            vkCmdClearAttachments(cmdBuf, fbClearC, VkClearRect.calloc(1).baseArrayLayer(0).layerCount(layerCount).rect(VkRect2D.calloc().set(fbLayout.scissor)));
            if (hasDepthStencil) {
                vkCmdClearAttachments(cmdBuf, VkClearAttachment.calloc(1)
                    .clearValue(fbLayout.depthStencilAttachmentInfo.clearValue())
                    .aspectMask(framebufferObj.currentDepthStencilImageView.subresourceLayers(0).aspectMask())
                    .colorAttachment(0), VkClearRect.calloc(1).baseArrayLayer(0).layerCount(layerCount).rect(VkRect2D.calloc().set(fbLayout.scissor)));
            }
        }
        vkCmdEndRendering(cmdBuf);

        // after draw needs to barrier
        vkCmdPipelineBarrier2(cmdBuf, VkDependencyInfoKHR.calloc().sType(VK_STRUCTURE_TYPE_DEPENDENCY_INFO).pMemoryBarriers(VkMemoryBarrier2.calloc(1).sType(VK_STRUCTURE_TYPE_MEMORY_BARRIER_2)
            .srcStageMask(VK_PIPELINE_STAGE_2_ALL_GRAPHICS_BIT)
            .srcAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
            .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
            .dstAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)));
    }

    //
    public static class ComputePipelineObj extends PipelineObj {
        public VkComputePipelineCreateInfo.Buffer createInfo = null;
        public ComputePipelineObj(Handle base, Handle handle) {
            super(base, handle);
        }

        public ComputePipelineObj(Handle base, PipelineCInfo.ComputePipelineCInfo cInfo) {
            super(base, cInfo);

            //
            var deviceObj = (DeviceObj) BasicObj.globalHandleMap.get(base.get());
            var physicalDeviceObj = (PhysicalDeviceObj) BasicObj.globalHandleMap.get(deviceObj.base.get());

            //
            this.createInfo = VkComputePipelineCreateInfo.calloc(1)
                    .sType(VK_STRUCTURE_TYPE_COMPUTE_PIPELINE_CREATE_INFO)
                    .flags(VK_PIPELINE_CREATE_DESCRIPTOR_BUFFER_BIT_EXT)
                    .stage(createShaderModuleInfo(deviceObj.createShaderModule(cInfo.computeCode), VK_SHADER_STAGE_COMPUTE_BIT, "main"))
                    .layout(cInfo.pipelineLayout);

            //
            vkCreateComputePipelines(deviceObj.device, 0L, this.createInfo, null, memLongBuffer(memAddress((this.handle = new Handle("Pipeline")).ptr(), 0), 1));
            deviceObj.handleMap.put(this.handle, this);

            //
            if (cInfo.memoryAllocator != 0) {
                this.uniformDescriptorBuffer = new BufferObj(this.base, new BufferCInfo() {{
                    size = cInfo.uniformBufferSize;
                    usage = VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT | VK_BUFFER_USAGE_RESOURCE_DESCRIPTOR_BUFFER_BIT_EXT;
                    memoryAllocator = cInfo.memoryAllocator;
                    memoryAllocationInfo = new MemoryAllocationCInfo(){{
                        isHost = true;
                        isDevice = true;
                    }};
                }});
            }
        }
    }

    //
    public static class GraphicsPipelineObj extends PipelineObj {
        public VkPipelineShaderStageCreateInfo.Buffer shaderStageInfo = null;
        public VkPipelineVertexInputStateCreateInfo vertexInputInfo = null;
        public VkPipelineInputAssemblyStateCreateInfo inputAssemblyStateInfo = null;
        public VkPipelineViewportStateCreateInfo viewportStateInfo = null;
        public VkPipelineRasterizationConservativeStateCreateInfoEXT conservativeRasterInfo = null;
        public VkPipelineRasterizationStateCreateInfo rasterizationInfo = null;
        public VkPipelineMultisampleStateCreateInfo multisampleInfo = null;
        //public VkPipelineColorBlendAttachmentState.Buffer colorBlendAttachment = null;
        public VkPipelineColorBlendStateCreateInfo colorBlendInfo = null;
        public VkPipelineRenderingCreateInfoKHR dynamicRenderingPipelineInfo = null;
        //public IntBuffer attachmentFormats = null;
        public IntBuffer dynamicStates = null;
        public VkPipelineDynamicStateCreateInfo dynamicStateInfo = null;
        public VkPipelineDepthStencilStateCreateInfo depthStencilState = null;
        public VkGraphicsPipelineCreateInfo.Buffer createInfo = null;

        //
        public GraphicsPipelineObj(Handle base, Handle handle) {
            super(base, handle);
        }

        //
        public GraphicsPipelineObj(Handle base, PipelineCInfo.GraphicsPipelineCInfo cInfo) {
            super(base, cInfo);

            //
            var deviceObj = (DeviceObj) BasicObj.globalHandleMap.get(base.get());
            var physicalDeviceObj = (PhysicalDeviceObj) BasicObj.globalHandleMap.get(deviceObj.base.get());
            var pipelineLayoutObj = (PipelineLayoutObj)deviceObj.handleMap.get(new Handle("PipelineLayout", ((PipelineCInfo.GraphicsPipelineCInfo)this.cInfo).pipelineLayout));

            //
            var fbLayout = ((PipelineCInfo.GraphicsPipelineCInfo)cInfo).fbLayout;
            boolean hasDepthStencil = fbLayout.depthStencilFormat != VK_FORMAT_UNDEFINED;
            boolean hasDepth = fbLayout.depthStencilFormat != VK_FORMAT_UNDEFINED;
            boolean hasStencil = fbLayout.depthStencilFormat != VK_FORMAT_UNDEFINED;

            //
            this.shaderStageInfo = VkPipelineShaderStageCreateInfo.calloc(cInfo.sourceMap.size()).sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO); AtomicInteger N = new AtomicInteger();
            cInfo.sourceMap.forEach((stage, source)->{
                this.shaderStageInfo.put(N.getAndIncrement(), createShaderModuleInfo(deviceObj.createShaderModule(source), stage, "main"));
            });

            //
            this.vertexInputInfo = VkPipelineVertexInputStateCreateInfo.calloc().sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);
            this.inputAssemblyStateInfo = VkPipelineInputAssemblyStateCreateInfo.calloc().sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO);
            this.viewportStateInfo = VkPipelineViewportStateCreateInfo.calloc().sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO);
            this.conservativeRasterInfo = VkPipelineRasterizationConservativeStateCreateInfoEXT.calloc().sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_CONSERVATIVE_STATE_CREATE_INFO_EXT);
            this.rasterizationInfo = VkPipelineRasterizationStateCreateInfo.calloc().sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO);
            this.multisampleInfo = VkPipelineMultisampleStateCreateInfo.calloc().sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO);
            this.colorBlendInfo = VkPipelineColorBlendStateCreateInfo.calloc().sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO);
            this.dynamicRenderingPipelineInfo = VkPipelineRenderingCreateInfoKHR.calloc().sType(VK_STRUCTURE_TYPE_PIPELINE_RENDERING_CREATE_INFO);
            this.dynamicStateInfo = VkPipelineDynamicStateCreateInfo.calloc().sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO);
            this.depthStencilState = VkPipelineDepthStencilStateCreateInfo.calloc().sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO);
            this.createInfo = VkGraphicsPipelineCreateInfo.calloc(1).sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO);
            this.dynamicStates = memAllocInt(18); // HARDCORE!

            //
            this.inputAssemblyStateInfo
                .topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST);

            //
            this.inputAssemblyStateInfo
                .primitiveRestartEnable(false);

            //
            this.conservativeRasterInfo
                .conservativeRasterizationMode(VK_CONSERVATIVE_RASTERIZATION_MODE_DISABLED_EXT);

            //
            this.rasterizationInfo
                .pNext(this.conservativeRasterInfo.address())
                .depthClampEnable(false)
                .rasterizerDiscardEnable(false)
                .polygonMode(VK_POLYGON_MODE_FILL)
                .cullMode(VK_CULL_MODE_NONE)
                .frontFace(VK_FRONT_FACE_CLOCKWISE)
                .depthBiasEnable(false)
                .depthBiasConstantFactor(0.0F)
                .depthBiasClamp(0.0F)
                .depthBiasSlopeFactor(0.0F)
                .lineWidth(1.0F);

            //
            this.multisampleInfo
                .rasterizationSamples(VK_SAMPLE_COUNT_1_BIT)
                .minSampleShading(1.0F)
                .alphaToCoverageEnable(false)
                .alphaToOneEnable(false);

            //
            var blendAttachments = VkPipelineColorBlendAttachmentState.calloc(fbLayout.blendStates.size());
            for (var I=0;I<fbLayout.blendStates.size();I++) {
                var blendState = fbLayout.blendStates.get(I);
                blendAttachments.get(I).set(
                    blendState.enabled,
                    blendState.srcRgbFactor,
                    blendState.dstRgbFactor,
                    blendState.blendOp, // TODO: support for RGB and alpha blend op
                    blendState.srcAlphaFactor,
                    blendState.dstAlphaFactor,
                    blendState.blendOp, // TODO: support for RGB and alpha blend op
                    fbLayout.colorMask.get(I).colorMask
                );
            }

            //
            this.colorBlendInfo.logicOpEnable(false)
                .logicOp(VK_LOGIC_OP_NO_OP)
                .pAttachments(blendAttachments)
                .blendConstants(memAllocFloat(4)
                    .put(0, 0.0F)
                    .put(1, 0.0F)
                    .put(2, 0.0F)
                    .put(3, 0.0F));
            //
            //this.attachmentFormats.put();
            // TODO: depth only or stencil only support
            // TODO: dynamic depth and stencil state
            this.dynamicRenderingPipelineInfo
                .pColorAttachmentFormats(fbLayout.formats)
                .depthAttachmentFormat(fbLayout.depthStencilFormat)
                .stencilAttachmentFormat(fbLayout.depthStencilFormat);

            // TODO: dynamic depth and stencil state
            this.dynamicStates
                .put(0, VK_DYNAMIC_STATE_VIEWPORT_WITH_COUNT)
                .put(1, VK_DYNAMIC_STATE_SCISSOR_WITH_COUNT)
                .put(2, VK_DYNAMIC_STATE_VERTEX_INPUT_EXT)
                .put(3, VK_DYNAMIC_STATE_DEPTH_TEST_ENABLE)
                .put(4, VK_DYNAMIC_STATE_DEPTH_WRITE_ENABLE)
                .put(5, VK_DYNAMIC_STATE_DEPTH_COMPARE_OP)
                .put(6, VK_DYNAMIC_STATE_STENCIL_TEST_ENABLE)
                .put(7, VK_DYNAMIC_STATE_STENCIL_OP)
                .put(8, VK_DYNAMIC_STATE_COLOR_BLEND_ENABLE_EXT)
                .put(9, VK_DYNAMIC_STATE_COLOR_BLEND_EQUATION_EXT)
                .put(10, VK_DYNAMIC_STATE_LOGIC_OP_ENABLE_EXT)
                .put(11, VK_DYNAMIC_STATE_COLOR_WRITE_MASK_EXT)
                .put(12, VK_DYNAMIC_STATE_BLEND_CONSTANTS)
                .put(13, VK_DYNAMIC_STATE_CULL_MODE)
                .put(14, VK_DYNAMIC_STATE_FRONT_FACE)
                .put(15, VK_DYNAMIC_STATE_LOGIC_OP_EXT)
                .put(16, VK_DYNAMIC_STATE_DEPTH_BIAS)
                .put(17, VK_DYNAMIC_STATE_DEPTH_BIAS_ENABLE);
            this.dynamicStateInfo.pDynamicStates(this.dynamicStates);

            //
            this.depthStencilState.depthTestEnable(true)
                .depthWriteEnable(true)
                .depthCompareOp(VK_COMPARE_OP_LESS_OR_EQUAL)
                .depthBoundsTestEnable(true)
                .stencilTestEnable(false)
                .minDepthBounds(0.0F)
                .maxDepthBounds(1.0F);

            //
            this.createInfo
                .pNext(this.dynamicRenderingPipelineInfo)
                .flags(VK_PIPELINE_CREATE_DESCRIPTOR_BUFFER_BIT_EXT)
                .pStages(this.shaderStageInfo)
                .pVertexInputState(this.vertexInputInfo)
                .pColorBlendState(this.colorBlendInfo)
                .pDepthStencilState(this.depthStencilState)
                .pViewportState(this.viewportStateInfo)
                .pInputAssemblyState(this.inputAssemblyStateInfo)
                .pRasterizationState(this.rasterizationInfo)
                .pMultisampleState(this.multisampleInfo)
                .pDynamicState(this.dynamicStateInfo)
                .layout(cInfo.pipelineLayout);

            //
            vkCreateGraphicsPipelines(deviceObj.device, 0L, this.createInfo, null, memLongBuffer(memAddress((this.handle = new Handle("Pipeline")).ptr(), 0), 1));
            deviceObj.handleMap.put(this.handle, this);

            //
            if (cInfo.memoryAllocator != 0) {
                this.uniformDescriptorBuffer = new BufferObj(this.base, new BufferCInfo() {{
                    size = cInfo.uniformBufferSize;
                    usage = VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT | VK_BUFFER_USAGE_RESOURCE_DESCRIPTOR_BUFFER_BIT_EXT;
                    memoryAllocator = cInfo.memoryAllocator;
                    memoryAllocationInfo = new MemoryAllocationCInfo(){{
                        isHost = true;
                        isDevice = true;
                    }};
                }});
            }
        }
    }

    @Override // TODO: multiple queue family support
    public PipelineObj delete() {
        var handle = this.handle;
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get());
        deviceObj.submitOnce(deviceObj.getCommandPool(((SwapChainCInfo)cInfo).queueFamilyIndex), new BasicCInfo.SubmitCmd(){{
            queue = deviceObj.getQueue(((SwapChainCInfo)cInfo).queueFamilyIndex, 0);
            onDone = new Promise<>().thenApply((result)->{
                vkDestroyPipeline(deviceObj.device, handle.get(), null);
                deviceObj.handleMap.remove(handle);
                return null;
            });
        }}, (cmdBuf)->{
            return VK_SUCCESS;
        });
        return this;
    }

}
