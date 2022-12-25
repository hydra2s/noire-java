package org.hydra2s.manhack.descriptors;

import org.hydra2s.manhack.objects.BasicObj;
import org.lwjgl.vulkan.VkSamplerCreateInfo;

public class SamplerCInfo extends BasicCInfo  {
    public VkSamplerCreateInfo createInfo = null;
    public long pipelineLayout = 0L;
}
