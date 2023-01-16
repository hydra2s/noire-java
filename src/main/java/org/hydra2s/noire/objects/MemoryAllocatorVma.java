package org.hydra2s.noire.objects;

//
import org.hydra2s.noire.descriptors.MemoryAllocatorCInfo;

// TODO: VMA memory allocator and allocation
public class MemoryAllocatorVma extends MemoryAllocatorObj {
    public MemoryAllocatorVma(Handle base, Handle handle) {
        super(base, handle);
    }

    public MemoryAllocatorVma(Handle base, MemoryAllocatorCInfo cInfo) {
        super(base, cInfo);
    }
}
