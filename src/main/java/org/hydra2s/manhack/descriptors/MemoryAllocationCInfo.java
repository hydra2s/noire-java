package org.hydra2s.manhack.descriptors;

import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.VkExtent3D;
import org.lwjgl.vulkan.VkMemoryRequirements;
import org.lwjgl.vulkan.VkMemoryRequirements2;

import static org.lwjgl.system.MemoryUtil.memAllocPointer;
import static org.lwjgl.vulkan.VK10.*;

public class MemoryAllocationCInfo extends BasicCInfo {
    public VkMemoryRequirements memoryRequirements = null;
    public VkMemoryRequirements2 memoryRequirements2 = null;

    //
    public boolean isHost = true;
    public boolean isDevice = true;

    //
    public PointerBuffer buffer = null;
    public PointerBuffer image = null;

    //
    public static class BufferCInfo extends MemoryAllocationCInfo {
        public long size = 0;
        public int usage = VK_BUFFER_USAGE_STORAGE_BUFFER_BIT;


        public BufferCInfo() {
            this.buffer = memAllocPointer(1);
        }
    }

    //
    public static class ImageCInfo extends MemoryAllocationCInfo {
        public VkExtent3D extent3D = VkExtent3D.create().width(1).height(1).depth(1);
        public int usage = VK_IMAGE_USAGE_SAMPLED_BIT | VK_IMAGE_USAGE_STORAGE_BIT;
        public int mipLevels = 1;
        public int arrayLayers = 1;
        public int samples = VK_SAMPLE_COUNT_1_BIT;
        public int tiling = VK_IMAGE_TILING_OPTIMAL;
        public int format = VK_FORMAT_R8G8B8A8_UNORM;

        //
        public ImageCInfo() {
            this.image = memAllocPointer(1);
        }
    }

}
