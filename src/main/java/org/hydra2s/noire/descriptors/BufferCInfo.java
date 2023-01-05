package org.hydra2s.noire.descriptors;

import org.lwjgl.PointerBuffer;

import static org.lwjgl.system.MemoryUtil.memAllocPointer;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_STORAGE_BUFFER_BIT;

public class BufferCInfo extends BasicCInfo {
    public long size = 0;
    public int usage = VK_BUFFER_USAGE_STORAGE_BUFFER_BIT;
    public PointerBuffer buffer = null;

    //
    public long memoryAllocator = 0L;
    public MemoryAllocationCInfo memoryAllocationInfo = null;

    public BufferCInfo() {
        this.buffer = memAllocPointer(1).put(0, 0);
    }
}
