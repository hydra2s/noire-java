package org.hydra2s.noire.descriptors;

//
import org.lwjgl.vulkan.VkExtent2D;
import java.nio.IntBuffer;

import static org.lwjgl.vulkan.VK10.VK_FORMAT_R8G8B8A8_UNORM;

//
public class SwapChainCInfo extends BasicCInfo  {
    public long pipelineLayout = 0L;
    public long memoryAllocator = 0L;
    public long surface = 0L;

    public VkExtent2D extent = null;

    public int imageCount = 4;
    public int layerCount = 1;
    public int format = VK_FORMAT_R8G8B8A8_UNORM;

    public static class VirtualSwapChainCInfo extends SwapChainCInfo {


    }

}
