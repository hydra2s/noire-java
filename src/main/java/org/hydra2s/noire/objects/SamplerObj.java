package org.hydra2s.noire.objects;

//
import org.hydra2s.noire.descriptors.BasicCInfo;
import org.hydra2s.noire.descriptors.ImageViewCInfo;
import org.hydra2s.noire.descriptors.SamplerCInfo;
import org.hydra2s.utils.Promise;
import org.lwjgl.vulkan.VkSamplerCreateInfo;

//
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

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
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get()).orElse(null);
        var physicalDeviceObj = (PhysicalDeviceObj)BasicObj.globalHandleMap.get(deviceObj.base.get()).orElse(null);

        //
        this.createInfo = cInfo.createInfo;
        vkCreateSampler(deviceObj.device, this.createInfo, null, memLongBuffer(memAddress((this.handle = new Handle("Sampler")).ptr(), 0), 1));

        //
        if (cInfo.pipelineLayout != 0) {
            var descriptorsObj = (PipelineLayoutObj)deviceObj.handleMap.get(new Handle("PipelineLayout", cInfo.pipelineLayout)).orElse(null);
            this.DSC_ID = descriptorsObj.samplers.push(memAllocLong(1).put(0, this.handle.get()));
            descriptorsObj.writeDescriptors();
        }
    }

    @Override // TODO: multiple queue family support (and Promise.all)
    public SamplerObj delete() throws Exception {
        super.delete();
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get()).orElse(null);
        var cInfo = (SamplerCInfo)this.cInfo;
        var pipelineLayoutObj = (PipelineLayoutObj)deviceObj.handleMap.get(new Handle("PipelineLayout", cInfo.pipelineLayout)).orElse(null);
        var self = this;
        deviceObj.submitOnce(deviceObj.getCommandPool(cInfo.queueFamilyIndex), new BasicCInfo.SubmitCmd(){{
            queueFamilyIndex = cInfo.queueFamilyIndex;
            queue = deviceObj.getQueue(cInfo.queueFamilyIndex, 0);
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
            return VK_SUCCESS;
        });
        return this;
    }

    @Override // TODO: multiple queue family support (and Promise.all)
    public SamplerObj deleteDirectly() throws Exception {
        super.delete();
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get()).orElse(null);
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
