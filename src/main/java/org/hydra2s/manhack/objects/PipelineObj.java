package org.hydra2s.manhack.objects;

//
import org.hydra2s.manhack.descriptors.PipelineCInfo;
import org.lwjgl.vulkan.*;

//
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicInteger;

//
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.EXTConservativeRasterization.VK_CONSERVATIVE_RASTERIZATION_MODE_DISABLED_EXT;
import static org.lwjgl.vulkan.EXTConservativeRasterization.VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_CONSERVATIVE_STATE_CREATE_INFO_EXT;
import static org.lwjgl.vulkan.EXTDescriptorBuffer.VK_PIPELINE_CREATE_DESCRIPTOR_BUFFER_BIT_EXT;
import static org.lwjgl.vulkan.EXTVertexInputDynamicState.VK_DYNAMIC_STATE_VERTEX_INPUT_EXT;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK13.*;

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
        return VkPipelineShaderStageCreateInfo.create().sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO).flags(0).module(module).stage(stage).pName(memUTF8(pName)).pSpecializationInfo(null);
    }

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
            this.createInfo = VkComputePipelineCreateInfo.create(1)
                    .sType(VK_STRUCTURE_TYPE_COMPUTE_PIPELINE_CREATE_INFO)
                    .flags(VK_PIPELINE_CREATE_DESCRIPTOR_BUFFER_BIT_EXT)
                    .stage(createShaderModuleInfo(deviceObj.createShaderModule(cInfo.computeCode), VK_SHADER_STAGE_COMPUTE_BIT, "main"))
                    .layout(cInfo.pipelineLayout);

            //
            vkCreateComputePipelines(deviceObj.device, 0L, this.createInfo, null, memLongBuffer(memAddress((this.handle = new Handle("Pipeline")).ptr(), 0), 1));
            deviceObj.handleMap.put(this.handle, this);
        }

        public ComputePipelineObj cmdDispatch(VkCommandBuffer cmdBuf, VkExtent3D dispatch, ByteBuffer pushConstRaw, int pushConstByteOffset) {
            if (pushConstRaw != null) {
                vkCmdPushConstants(cmdBuf, ((PipelineCInfo.ComputePipelineCInfo)this.cInfo).pipelineLayout, VK_SHADER_STAGE_ALL, pushConstByteOffset, pushConstRaw);
            }

            //
            var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(base.get());
            var physicalDeviceObj = (PhysicalDeviceObj) BasicObj.globalHandleMap.get(deviceObj.base.get());
            var pipelineLayoutObj = (PipelineLayoutObj)deviceObj.handleMap.get(new Handle("PipelineLayout", ((PipelineCInfo.ComputePipelineCInfo)this.cInfo).pipelineLayout));

            //
            pipelineLayoutObj.cmdBindBuffers(cmdBuf, VK_PIPELINE_BIND_POINT_COMPUTE);

            //
            vkCmdBindPipeline(cmdBuf, VK_PIPELINE_BIND_POINT_COMPUTE, this.handle.get());
            vkCmdDispatch(cmdBuf, dispatch.width(), dispatch.height(), dispatch.depth());
            vkCmdPipelineBarrier2(cmdBuf, VkDependencyInfoKHR.create().sType(VK_STRUCTURE_TYPE_DEPENDENCY_INFO).pMemoryBarriers(VkMemoryBarrier2.create(1).sType(VK_STRUCTURE_TYPE_MEMORY_BARRIER_2)
                .srcStageMask(VK_PIPELINE_STAGE_2_COMPUTE_SHADER_BIT)
                .srcAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
                .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
                .dstAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)));

            //
            return this;
        }
    }

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
            //TODO Auto-generated constructor stub

        }

        //
        public GraphicsPipelineObj(Handle base, PipelineCInfo.GraphicsPipelineCInfo cInfo) {
            super(base, cInfo);

            //
            var deviceObj = (DeviceObj) BasicObj.globalHandleMap.get(base.get());
            var physicalDeviceObj = (PhysicalDeviceObj) BasicObj.globalHandleMap.get(deviceObj.base.get());
            var pipelineLayoutObj = (PipelineLayoutObj)deviceObj.handleMap.get(new Handle("PipelineLayout", ((PipelineCInfo.ComputePipelineCInfo)this.cInfo).pipelineLayout));

            //
            this.shaderStageInfo = VkPipelineShaderStageCreateInfo.create(cInfo.sourceMap.size()).sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO); AtomicInteger N = new AtomicInteger();
            cInfo.sourceMap.forEach((stage, source)->{
                this.shaderStageInfo.put(N.getAndIncrement(), createShaderModuleInfo(deviceObj.createShaderModule(source), stage, "main"));
            });

            //
            this.vertexInputInfo = VkPipelineVertexInputStateCreateInfo.create().sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO);
            this.inputAssemblyStateInfo = VkPipelineInputAssemblyStateCreateInfo.create().sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO);
            this.viewportStateInfo = VkPipelineViewportStateCreateInfo.create().sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO);
            this.conservativeRasterInfo = VkPipelineRasterizationConservativeStateCreateInfoEXT.create().sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_CONSERVATIVE_STATE_CREATE_INFO_EXT);
            this.rasterizationInfo = VkPipelineRasterizationStateCreateInfo.create().sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO);
            this.multisampleInfo = VkPipelineMultisampleStateCreateInfo.create().sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO);
            this.colorBlendInfo = VkPipelineColorBlendStateCreateInfo.create().sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO);
            this.dynamicRenderingPipelineInfo = VkPipelineRenderingCreateInfoKHR.create().sType(VK_STRUCTURE_TYPE_PIPELINE_RENDERING_CREATE_INFO);
            this.dynamicStateInfo = VkPipelineDynamicStateCreateInfo.create().sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO);
            this.depthStencilState = VkPipelineDepthStencilStateCreateInfo.create().sType(VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO);
            this.createInfo = VkGraphicsPipelineCreateInfo.create(1).sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO);
            this.dynamicStates = memAllocInt(3);

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
            this.colorBlendInfo.logicOpEnable(false)
                .logicOp(VK_LOGIC_OP_NO_OP)
                .pAttachments(cInfo.fbLayout.colorBlendAttachments)
                .blendConstants(memAllocFloat(4).put(0, 0.0F).put(1, 0.0F).put(2, 0.0F).put(3, 0.0F));

            //
            //this.attachmentFormats.put();
            this.dynamicRenderingPipelineInfo
                .pColorAttachmentFormats(cInfo.fbLayout.colorFormats);

            //
            this.dynamicStates
                .put(0, VK_DYNAMIC_STATE_VIEWPORT_WITH_COUNT)
                .put(1, VK_DYNAMIC_STATE_SCISSOR_WITH_COUNT)
                .put(2, VK_DYNAMIC_STATE_VERTEX_INPUT_EXT);

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
        }
        
    }
}
