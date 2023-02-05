package org.hydra2s.noire.descriptors;

//

import org.lwjgl.vulkan.VkMemoryRequirements;
import org.lwjgl.vulkan.VkMemoryRequirements2;

//
public class MemoryAllocationCInfo extends BasicCInfo {
    public VkMemoryRequirements memoryRequirements = null;
    public VkMemoryRequirements2 memoryRequirements2 = null;

    // TODO: replace by VMA ideology
    // TODO: add Resizable BAR for VMA support
    public boolean isHost = true;
    public boolean isDevice = true;

    // for dedicated buffer allocation
    public long[] buffer = {};
    // for dedicated image allocation
    public long[] image = {};

    //
    public long memoryAllocator = 0L;

    //
    public MemoryAllocationCInfo() {
        this.buffer = null;//memAllocPointer(1).put(0, 0);
        this.image = null;//memAllocPointer(1).put(0, 0);
    }
}
