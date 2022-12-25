package org.hydra2s.manhack.descriptors;

//
import org.lwjgl.vulkan.VkSamplerCreateInfo;

//
public class SamplerCInfo extends BasicCInfo  {
    public VkSamplerCreateInfo createInfo = null;
    public long pipelineLayout = 0L;
}
