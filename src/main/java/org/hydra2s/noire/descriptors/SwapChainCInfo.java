package org.hydra2s.noire.descriptors;

//
import org.lwjgl.vulkan.VkExtent2D;
import java.nio.IntBuffer;

//
public class SwapChainCInfo extends BasicCInfo  {
    public long pipelineLayout = 0L;
    public long memoryAllocator = 0L;
    public long surface = 0L;
    public int queueFamilyIndex = 0;
    public VkExtent2D extent = null;
    public IntBuffer queueFamilyIndices = null;

}
