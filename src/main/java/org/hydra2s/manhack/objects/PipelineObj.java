package org.hydra2s.manhack.objects;

//
import org.hydra2s.manhack.descriptors.PipelineCInfo;
import org.lwjgl.vulkan.*;

//
import java.nio.ByteBuffer;
import java.util.logging.Handler;

//
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.EXTDescriptorBuffer.VK_PIPELINE_CREATE_DESCRIPTOR_BUFFER_BIT_EXT;
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
        }

        public ComputePipelineObj cmdDispatch(VkCommandBuffer cmdBuf, VkExtent3D dispatch, ByteBuffer pushConstRaw, int pushConstByteOffset) {
            if (pushConstRaw != null) {
                vkCmdPushConstants(cmdBuf, ((PipelineCInfo.ComputePipelineCInfo)this.cInfo).pipelineLayout, VK_SHADER_STAGE_ALL, pushConstByteOffset, pushConstRaw);
            }

            //
            var deviceObj = (DeviceObj) BasicObj.globalHandleMap.get(base.get());
            var physicalDeviceObj = (PhysicalDeviceObj) BasicObj.globalHandleMap.get(deviceObj.base.get());
            var pipelineLayoutObj = (PipelineLayoutObj)deviceObj.handleMap.get(new Handle("PipelineLayout", ((PipelineCInfo.ComputePipelineCInfo)this.cInfo).pipelineLayout));

            //
            pipelineLayoutObj.cmdBindBuffers(cmdBuf, VK_PIPELINE_BIND_POINT_COMPUTE);

            //
            vkCmdBindPipeline(cmdBuf, VK_PIPELINE_BIND_POINT_COMPUTE, this.handle.get());
            vkCmdDispatch(cmdBuf, dispatch.width(), dispatch.height(), dispatch.depth());
            vkCmdPipelineBarrier2(cmdBuf, VkDependencyInfoKHR.create().pMemoryBarriers(VkMemoryBarrier2.create(1)
                    .srcStageMask(VK_PIPELINE_STAGE_2_COMPUTE_SHADER_BIT)
                    .srcAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
                    .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
                    .dstAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)));

            //
            return this;
        }
        
    }

    public static class GraphicsPipelineObj extends PipelineObj {

        public GraphicsPipelineObj(Handle base, Handle handle) {
            super(base, handle);
            //TODO Auto-generated constructor stub
        }
        
    }
}
