package org.hydra2s.noire.objects;

//

import it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import org.hydra2s.noire.descriptors.ImageSetCInfo;
import org.hydra2s.noire.descriptors.PipelineCInfo;
import org.hydra2s.noire.descriptors.UtilsCInfo;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hydra2s.noire.descriptors.UtilsCInfo.vkCheckStatus;
import static org.lwjgl.BufferUtils.createFloatBuffer;
import static org.lwjgl.BufferUtils.createIntBuffer;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.EXTConservativeRasterization.VK_CONSERVATIVE_RASTERIZATION_MODE_DISABLED_EXT;
import static org.lwjgl.vulkan.EXTConservativeRasterization.VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_CONSERVATIVE_STATE_CREATE_INFO_EXT;
import static org.lwjgl.vulkan.EXTDescriptorBuffer.VK_PIPELINE_CREATE_DESCRIPTOR_BUFFER_BIT_EXT;
import static org.lwjgl.vulkan.EXTExtendedDynamicState2.VK_DYNAMIC_STATE_LOGIC_OP_EXT;
import static org.lwjgl.vulkan.EXTExtendedDynamicState3.*;
import static org.lwjgl.vulkan.EXTGraphicsPipelineLibrary.*;
import static org.lwjgl.vulkan.EXTPipelineRobustness.*;
import static org.lwjgl.vulkan.EXTVertexInputDynamicState.VK_DYNAMIC_STATE_VERTEX_INPUT_EXT;
import static org.lwjgl.vulkan.NVRepresentativeFragmentTest.VK_STRUCTURE_TYPE_PIPELINE_REPRESENTATIVE_FRAGMENT_TEST_STATE_CREATE_INFO_NV;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK13.*;

//
public class PipelineObj extends BasicObj  {


    public PipelineObj(UtilsCInfo.Handle base, UtilsCInfo.Handle handle) {
        super(base, handle);
    }
    public PipelineObj(UtilsCInfo.Handle base, PipelineCInfo cInfo) {
        super(base, cInfo);
    }

    //
    public VkPipelineShaderStageCreateInfo createShaderModuleInfo(long module, int stage, CharSequence pName){
        return VkPipelineShaderStageCreateInfo.create().sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO).flags(0).module(module).stage(stage).pName(memUTF8(pName)).pSpecializationInfo(null);
    }

    //
    //public BufferObj uniformDescriptorBuffer = null;
    //public LongBuffer uniformDescriptorSet = null;


    //
    public static class ComputePipelineObj extends PipelineObj {
        public VkPipelineRobustnessCreateInfoEXT robustness;
        public VkComputePipelineCreateInfo.Buffer createInfo = null;
        public ComputePipelineObj(UtilsCInfo.Handle base, UtilsCInfo.Handle handle) {
            super(base, handle);
        }

        //
        public ComputePipelineObj(UtilsCInfo.Handle base, PipelineCInfo.ComputePipelineCInfo cInfo) {
            super(base, cInfo);

            //
            this.robustness = VkPipelineRobustnessCreateInfoEXT.create().sType(VK_STRUCTURE_TYPE_PIPELINE_ROBUSTNESS_CREATE_INFO_EXT);
            this.robustness
                .storageBuffers(VK_PIPELINE_ROBUSTNESS_BUFFER_BEHAVIOR_DISABLED_EXT)
                .uniformBuffers(VK_PIPELINE_ROBUSTNESS_BUFFER_BEHAVIOR_DISABLED_EXT)
                .vertexInputs(VK_PIPELINE_ROBUSTNESS_BUFFER_BEHAVIOR_DISABLED_EXT)
                .images(VK_PIPELINE_ROBUSTNESS_IMAGE_BEHAVIOR_DISABLED_EXT);

            //
            this.createInfo = VkComputePipelineCreateInfo.create(1)
                .pNext(this.robustness.address())
                .sType(VK_STRUCTURE_TYPE_COMPUTE_PIPELINE_CREATE_INFO)
                .flags((PipelineLayoutObj.useLegacyBindingSystem ? 0 : VK_PIPELINE_CREATE_DESCRIPTOR_BUFFER_BIT_EXT) | VK_PIPELINE_CREATE_LINK_TIME_OPTIMIZATION_BIT_EXT | VK_PIPELINE_CREATE_RETAIN_LINK_TIME_OPTIMIZATION_INFO_BIT_EXT)
                .stage(createShaderModuleInfo(deviceObj.createShaderModule(cInfo.computeCode), VK_SHADER_STAGE_COMPUTE_BIT, "main"))
                .layout(cInfo.pipelineLayout);

            //
            var pipelineLayoutObj = (PipelineLayoutObj)deviceObj.handleMap.get(new UtilsCInfo.Handle("PipelineLayout", cInfo.pipelineLayout)).orElse(null);;
            vkCheckStatus(vkCreateComputePipelines(deviceObj.device, pipelineLayoutObj.pipelineCache[0], this.createInfo, null, (this.handle = new UtilsCInfo.Handle("Pipeline")).ptr()));
            deviceObj.handleMap.put$(this.handle, this);

            //
            /*if (cInfo.memoryAllocator != 0) {
                this.uniformDescriptorBuffer = new BufferObj(this.base, new BufferCInfo() {{
                    size = cInfo.uniformBufferSize;
                    usage = VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT | VK_BUFFER_USAGE_RESOURCE_DESCRIPTOR_BUFFER_BIT_EXT;
                    memoryAllocator = cInfo.memoryAllocator;
                    memoryAllocationInfo = new MemoryAllocationCInfo(){{
                        isHost = true;
                        isDevice = true;
                    }};
                }});

                //
                var $pipelineLayoutObj = (PipelineLayoutObj)deviceObj.handleMap.get(new Handle("PipelineLayout", cInfo.pipelineLayout)).orElse(null);;
                $pipelineLayoutObj.createDescriptorSetForUniformBuffer(this.uniformDescriptorBuffer, this.uniformDescriptorSet = createLongBuffer(1).put(0, 0L));
            }*/
        }
    }

    //
    public static class GraphicsPipelineObj extends PipelineObj {
        public VkPipelineRepresentativeFragmentTestStateCreateInfoNV representativeFragmentTestNV = null;
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
        public VkPipelineRobustnessCreateInfoEXT robustness = null;
        public VkGraphicsPipelineLibraryCreateInfoEXT library = null;

        //
        public GraphicsPipelineObj(UtilsCInfo.Handle base, UtilsCInfo.Handle handle) {
            super(base, handle);
        }

        //
        //public Long2IntOpenHashMap stateMap = new Long2IntOpenHashMap();
        public Int2LongOpenHashMap stateMap = new Int2LongOpenHashMap();

        //
        public long getStatePipeline(ImageSetCInfo.FBLayout fbLayout) {
            // if supported full set, doesn't needed to make absent pipeline
            // TODO: tracking changes
            if (
                this.handle != null &&
                physicalDeviceObj.features.dynamicState3.extendedDynamicState3ColorBlendEnable() &&
                physicalDeviceObj.features.dynamicState3.extendedDynamicState3ColorBlendEquation() &&
                physicalDeviceObj.features.dynamicState3.extendedDynamicState3LogicOpEnable() &&
                physicalDeviceObj.features.dynamicState3.extendedDynamicState3ColorWriteMask() &&
                physicalDeviceObj.features.dynamicState2.extendedDynamicState2LogicOp())
            {
                return this.handle.get();
            }

            //
            if (stateMap.get(fbLayout.hashCode()) <= 0) {
                boolean hasDepthStencil = fbLayout.depthStencilFormat != VK_FORMAT_UNDEFINED;
                boolean hasDepth = fbLayout.depthStencilFormat != VK_FORMAT_UNDEFINED;
                boolean hasStencil = fbLayout.depthStencilFormat != VK_FORMAT_UNDEFINED;

                //
                var cInfo = (PipelineCInfo.GraphicsPipelineCInfo) this.cInfo;
                var pipelineLayoutObj = (PipelineLayoutObj) deviceObj.handleMap.get(new UtilsCInfo.Handle("PipelineLayout", ((PipelineCInfo.GraphicsPipelineCInfo) this.cInfo).pipelineLayout)).orElse(null);
                ;


                //
                this.shaderStageInfo = VkPipelineShaderStageCreateInfo.create(cInfo.sourceMap.size()).sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO);
                AtomicInteger N = new AtomicInteger();
                cInfo.sourceMap.forEach((stage, source) -> {
                    this.shaderStageInfo.put(N.getAndIncrement(), createShaderModuleInfo(deviceObj.createShaderModule(source), stage, "main"));
                });

                //
                this.inputAssemblyStateInfo
                    .topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
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
                    .frontFace(VK_FRONT_FACE_COUNTER_CLOCKWISE)
                    .depthBiasEnable(true)
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
                var blendAttachments = VkPipelineColorBlendAttachmentState.create(fbLayout.blendStates.size());
                for (var I = 0; I < fbLayout.blendStates.size(); I++) {
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
                    .logicOp(fbLayout.logicOp.getLogicOp())
                    .pAttachments(blendAttachments)
                    .blendConstants(createFloatBuffer(4)
                        .put(0, 0.0F)
                        .put(1, 0.0F)
                        .put(2, 0.0F)
                        .put(3, 0.0F));

                //
                this.representativeFragmentTestNV.representativeFragmentTestEnable(physicalDeviceObj.features.representativeFragmentTestNV.representativeFragmentTest());
                this.robustness
                    .pNext(this.library
                        .pNext(physicalDeviceObj.features.representativeFragmentTestNV.representativeFragmentTest() ? this.representativeFragmentTestNV.address() : 0L)
                        .flags(
                            VK_GRAPHICS_PIPELINE_LIBRARY_VERTEX_INPUT_INTERFACE_BIT_EXT |
                                VK_GRAPHICS_PIPELINE_LIBRARY_PRE_RASTERIZATION_SHADERS_BIT_EXT |
                                VK_GRAPHICS_PIPELINE_LIBRARY_FRAGMENT_SHADER_BIT_EXT |
                                VK_GRAPHICS_PIPELINE_LIBRARY_FRAGMENT_OUTPUT_INTERFACE_BIT_EXT
                        ).address())
                    .storageBuffers(VK_PIPELINE_ROBUSTNESS_BUFFER_BEHAVIOR_DISABLED_EXT)
                    .uniformBuffers(VK_PIPELINE_ROBUSTNESS_BUFFER_BEHAVIOR_DISABLED_EXT)
                    .vertexInputs(VK_PIPELINE_ROBUSTNESS_BUFFER_BEHAVIOR_DISABLED_EXT)
                    .images(VK_PIPELINE_ROBUSTNESS_IMAGE_BEHAVIOR_DISABLED_EXT);

                //
                //this.attachmentFormats.put();
                // TODO: depth only or stencil only support
                // TODO: dynamic depth and stencil state
                var formats = createIntBuffer(fbLayout.formats.length);
                for (int I = 0; I < fbLayout.formats.length; I++) { formats.put(I, fbLayout.formats[I]); }
                ;
                this.dynamicRenderingPipelineInfo
                    .pNext(this.robustness.address())
                    .pColorAttachmentFormats(formats)
                    .depthAttachmentFormat(fbLayout.depthStencilFormat)
                    .stencilAttachmentFormat(fbLayout.depthStencilFormat);

                //
                var dynamicStates = new ArrayList<Integer>() {{
                    add(VK_DYNAMIC_STATE_VIEWPORT_WITH_COUNT);
                    add(VK_DYNAMIC_STATE_SCISSOR_WITH_COUNT);
                    add(VK_DYNAMIC_STATE_VERTEX_INPUT_EXT);
                    add(VK_DYNAMIC_STATE_DEPTH_TEST_ENABLE);
                    add(VK_DYNAMIC_STATE_DEPTH_WRITE_ENABLE);
                    add(VK_DYNAMIC_STATE_DEPTH_COMPARE_OP);
                    add(VK_DYNAMIC_STATE_STENCIL_TEST_ENABLE);
                    add(VK_DYNAMIC_STATE_STENCIL_OP);
                    add(VK_DYNAMIC_STATE_BLEND_CONSTANTS);
                    add(VK_DYNAMIC_STATE_CULL_MODE);
                    add(VK_DYNAMIC_STATE_FRONT_FACE);
                    add(VK_DYNAMIC_STATE_DEPTH_BIAS);
                    add(VK_DYNAMIC_STATE_DEPTH_BIAS_ENABLE);

                    // TODO: tracking changes
                    if (physicalDeviceObj.features.dynamicState3.extendedDynamicState3ColorBlendEnable())
                        add(VK_DYNAMIC_STATE_COLOR_BLEND_ENABLE_EXT);
                    if (physicalDeviceObj.features.dynamicState3.extendedDynamicState3ColorBlendEquation())
                        add(VK_DYNAMIC_STATE_COLOR_BLEND_EQUATION_EXT);
                    if (physicalDeviceObj.features.dynamicState3.extendedDynamicState3LogicOpEnable())
                        add(VK_DYNAMIC_STATE_LOGIC_OP_ENABLE_EXT);
                    if (physicalDeviceObj.features.dynamicState3.extendedDynamicState3ColorWriteMask())
                        add(VK_DYNAMIC_STATE_COLOR_WRITE_MASK_EXT);
                    if (physicalDeviceObj.features.dynamicState2.extendedDynamicState2LogicOp())
                        add(VK_DYNAMIC_STATE_LOGIC_OP_EXT);
                }};

                // TODO: dynamic depth and stencil state
                this.dynamicStateInfo.pDynamicStates(this.dynamicStates = createIntBuffer(dynamicStates.size()).put(0, dynamicStates.stream().mapToInt((I) -> I).toArray()));

                //
                this.depthStencilState
                    .depthTestEnable(fbLayout.depthState.depthTest)
                    .depthWriteEnable(fbLayout.depthState.depthMask)
                    .depthCompareOp(fbLayout.depthState.function)
                    .depthBoundsTestEnable(true)
                    .stencilTestEnable(false)
                    .minDepthBounds(0.0F)
                    .maxDepthBounds(1.0F);

                //
                this.createInfo
                    .pNext(this.dynamicRenderingPipelineInfo.address())
                    .flags((PipelineLayoutObj.useLegacyBindingSystem ? 0 : VK_PIPELINE_CREATE_DESCRIPTOR_BUFFER_BIT_EXT) | VK_PIPELINE_CREATE_LINK_TIME_OPTIMIZATION_BIT_EXT | VK_PIPELINE_CREATE_RETAIN_LINK_TIME_OPTIMIZATION_INFO_BIT_EXT)
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

                // TODO: initial pipeline cache
                var pipeline = new long[]{0L};
                assert pipelineLayoutObj != null;
                vkCheckStatus(vkCreateGraphicsPipelines(deviceObj.device, pipelineLayoutObj.pipelineCache[0], this.createInfo, null, pipeline));
                stateMap.put(fbLayout.hashCode(), pipeline[0]);
            }
            return stateMap.get(fbLayout.hashCode());
        }

        //
        public GraphicsPipelineObj(UtilsCInfo.Handle base, PipelineCInfo.GraphicsPipelineCInfo cInfo) {
            super(base, cInfo);

            //
            this.representativeFragmentTestNV = VkPipelineRepresentativeFragmentTestStateCreateInfoNV.create().sType(VK_STRUCTURE_TYPE_PIPELINE_REPRESENTATIVE_FRAGMENT_TEST_STATE_CREATE_INFO_NV);
            this.library = VkGraphicsPipelineLibraryCreateInfoEXT.create().sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_LIBRARY_CREATE_INFO_EXT);
            this.robustness = VkPipelineRobustnessCreateInfoEXT.create().sType(VK_STRUCTURE_TYPE_PIPELINE_ROBUSTNESS_CREATE_INFO_EXT);
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

            //
            var pipelineLayoutObj = (PipelineLayoutObj)deviceObj.handleMap.get(new UtilsCInfo.Handle("PipelineLayout", ((PipelineCInfo.GraphicsPipelineCInfo)this.cInfo).pipelineLayout)).orElse(null);;
            var fbLayout = ((PipelineCInfo.GraphicsPipelineCInfo)cInfo).fbLayout;

            //
            deviceObj.handleMap.put$(this.handle = new UtilsCInfo.Handle("Pipeline", this.getStatePipeline(fbLayout)), this);

            //
            /*if (cInfo.memoryAllocator != 0) {
                this.uniformDescriptorBuffer = new BufferObj(this.base, new BufferCInfo() {{
                    size = cInfo.uniformBufferSize;
                    usage = VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT | VK_BUFFER_USAGE_RESOURCE_DESCRIPTOR_BUFFER_BIT_EXT;
                    memoryAllocator = cInfo.memoryAllocator;
                    memoryAllocationInfo = new MemoryAllocationCInfo(){{
                        isHost = false;
                        isDevice = true;
                    }};
                }});

                //
                var $pipelineLayoutObj = (PipelineLayoutObj)deviceObj.handleMap.get(new Handle("PipelineLayout", cInfo.pipelineLayout)).orElse(null);;
                $pipelineLayoutObj.createDescriptorSetForUniformBuffer(this.uniformDescriptorBuffer, this.uniformDescriptorSet = createLongBuffer(1).put(0, 0L));
            }*/
        }
    }

    @Override // TODO: multiple queue family support
    public PipelineObj deleteDirectly() {
        var handle = this.handle;
        vkDestroyPipeline(deviceObj.device, handle.get(), null);
        deviceObj.handleMap.put$(handle, null);
        return this;
    }

}
