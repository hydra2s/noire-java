package org.hydra2s.noire.objects;

//

import org.hydra2s.noire.descriptors.SamplerCInfo;
import org.lwjgl.vulkan.VkSamplerCreateInfo;

import static org.hydra2s.noire.descriptors.UtilsCInfo.vkCheckStatus;
import static org.lwjgl.BufferUtils.createLongBuffer;
import static org.lwjgl.vulkan.VK10.vkCreateSampler;
import static org.lwjgl.vulkan.VK10.vkDestroySampler;

//
public class SamplerObj extends BasicObj  {
    public VkSamplerCreateInfo createInfo = null;
    public int DSC_ID = -1;

    //
    public SamplerObj(Handle base, Handle handle) {
        super(base, handle);
    }
    public SamplerObj(Handle base, SamplerCInfo cInfo) {
        super(base, cInfo);

        //
        this.createInfo = cInfo.createInfo;
        vkCheckStatus(vkCreateSampler(deviceObj.device, this.createInfo, null, (this.handle = new Handle("Sampler")).ptr()));

        // TODO: multiple pipeline layout support
        if (cInfo.pipelineLayout != 0) {
            var descriptorsObj = (PipelineLayoutObj)deviceObj.handleMap.get(new Handle("PipelineLayout", cInfo.pipelineLayout)).orElse(null);
            this.DSC_ID = descriptorsObj.samplers.push(createLongBuffer(1).put(0, this.handle.get()));
            descriptorsObj.writeDescriptors();
        }
    }

    /*@Override // TODO: multiple queue family support (and Promise.all)
    public SamplerObj delete() throws Exception {
        super.delete();
        
        var cInfo = (SamplerCInfo)this.cInfo;
        var pipelineLayoutObj = (PipelineLayoutObj)deviceObj.handleMap.get(new Handle("PipelineLayout", cInfo.pipelineLayout)).orElse(null);
        var self = this;
        deviceObj.submitOnce(new DeviceObj.SubmitCmd(){{
            queueGroupIndex = cInfo.queueGroupIndex;
            onDone = new Promise<>().thenApply((result)-> {
                if (pipelineLayoutObj != null) {
                    pipelineLayoutObj.samplers.removeIndex(DSC_ID);
                    self.DSC_ID = -1;
                }

                vkDestroySampler(deviceObj.device, handle.get(), null);
                deviceObj.handleMap.remove(handle);
                return null;
            });
        }}, (cmdBuf)->{
            return cmdBuf;
        });
        return this;
    }*/

    @Override // TODO: multiple queue family support (and Promise.all)
    public SamplerObj deleteDirectly() /*throws Exception*/ {
        super.deleteDirectly();
        
        var cInfo = (SamplerCInfo)this.cInfo;
        var pipelineLayoutObj = (PipelineLayoutObj)deviceObj.handleMap.get(new Handle("PipelineLayout", cInfo.pipelineLayout)).orElse(null);
        var self = this;

        if (pipelineLayoutObj != null) {
            pipelineLayoutObj.samplers.removeIndex(DSC_ID);
            self.DSC_ID = -1;
        }

        vkDestroySampler(deviceObj.device, handle.get(), null);
        deviceObj.handleMap.remove(handle);

        return this;
    }
}
