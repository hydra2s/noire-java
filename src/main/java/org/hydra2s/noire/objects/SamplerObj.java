package org.hydra2s.noire.objects;

//
import org.hydra2s.noire.descriptors.SamplerCInfo;
import org.lwjgl.vulkan.VkSamplerCreateInfo;

//
import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.system.MemoryUtil.memLongBuffer;
import static org.lwjgl.vulkan.VK10.vkCreateSampler;

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
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get());
        var physicalDeviceObj = (PhysicalDeviceObj)BasicObj.globalHandleMap.get(deviceObj.base.get());

        //
        this.createInfo = cInfo.createInfo;
        vkCreateSampler(deviceObj.device, this.createInfo, null, memLongBuffer(memAddress((this.handle = new Handle("Sampler")).ptr(), 0), 1));

        //
        if (cInfo.pipelineLayout > 0) {
            var descriptorsObj = (PipelineLayoutObj)deviceObj.handleMap.get(new Handle("PipelineLayout", cInfo.pipelineLayout));
            this.DSC_ID = descriptorsObj.samplers.push(memLongBuffer(this.handle.get(), 1));
            descriptorsObj.writeDescriptors();
        }
    }
}
