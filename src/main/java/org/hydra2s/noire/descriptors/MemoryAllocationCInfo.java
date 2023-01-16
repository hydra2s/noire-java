package org.hydra2s.noire.descriptors;

//
import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.VkMemoryRequirements;
import org.lwjgl.vulkan.VkMemoryRequirements2;

import static org.lwjgl.system.MemoryUtil.memAllocPointer;

//
public class MemoryAllocationCInfo extends BasicCInfo {
    public VkMemoryRequirements memoryRequirements = null;
    public VkMemoryRequirements2 memoryRequirements2 = null;

    // TODO: replace by VMA ideology
    // TODO: add Resizable BAR for VMA support
    public boolean isHost = true;
    public boolean isDevice = true;

    // for dedicated buffer allocation
    public PointerBuffer buffer = null;
    // for dedicated image allocation
    public PointerBuffer image = null;

    //
    public long memoryAllocator = 0L;

    //
    public MemoryAllocationCInfo() {
        this.buffer = memAllocPointer(1).put(0, 0);
        this.image = memAllocPointer(1).put(0, 0);
    }
}
