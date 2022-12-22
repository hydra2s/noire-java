package org.hydra2s.manhack.descriptors;

import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.VkExtent3D;
import org.lwjgl.vulkan.VkMemoryRequirements;
import org.lwjgl.vulkan.VkMemoryRequirements2;

import static org.lwjgl.system.MemoryUtil.memAllocPointer;

public class AllocationCInfo extends BasicCInfo {
    public VkMemoryRequirements memoryRequirements = null;
    public VkMemoryRequirements memoryRequirements2 = null;

    public boolean isHost = true;
    public boolean isDevice = true;

    public PointerBuffer buffer = null;
    public PointerBuffer image = null;

    public class BufferCInfo extends AllocationCInfo {
        public long size = 0;


        public BufferCInfo() {
            this.buffer = memAllocPointer(1);
        }
    }

    public class ImageCInfo extends AllocationCInfo  {
        public VkExtent3D extent3D = VkExtent3D.create().width(1).height(1).depth(1);


        public ImageCInfo() {
            this.image = memAllocPointer(1);
        }
    }

}

