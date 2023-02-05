package org.hydra2s.noire.descriptors;

//

import org.lwjgl.vulkan.VkExtent2D;

import java.nio.FloatBuffer;

import static org.lwjgl.system.MemoryUtil.memAllocFloat;

public class WindowCInfo extends BasicCInfo {
    public VkExtent2D size = null;
    public FloatBuffer scale = null;//memAllocFloat(2).put(0, 1.0F).put(1, 1.0F);
    public long pipelineLayout = 0L;
}
