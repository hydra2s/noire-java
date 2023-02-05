package org.hydra2s.noire.descriptors;

import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.VkExtent3D;

import static org.lwjgl.system.MemoryUtil.memAllocPointer;
import static org.lwjgl.vulkan.VK10.*;

public class ImageCInfo extends BasicCInfo {
    public VkExtent3D extent3D = VkExtent3D.calloc().width(1).height(1).depth(1);
    public int usage = VK_IMAGE_USAGE_SAMPLED_BIT | VK_IMAGE_USAGE_STORAGE_BIT;
    public int mipLevels = 1;
    public int arrayLayers = 1;
    public int samples = VK_SAMPLE_COUNT_1_BIT;
    public int tiling = VK_IMAGE_TILING_OPTIMAL;
    public int format = VK_FORMAT_R8G8B8A8_UNORM;
    public PointerBuffer image = null;

    //
    public long memoryAllocator = 0L;
    public MemoryAllocationCInfo memoryAllocationInfo = null;

    //
    public ImageCInfo() {
        this.image = null;//memAllocPointer(1).put(0, 0);
    }
}
