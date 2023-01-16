package org.hydra2s.noire.objects;

//
import org.hydra2s.noire.descriptors.MemoryAllocationCInfo;

// TODO: VMA memory allocator and allocation
public class MemoryAllocationVma extends MemoryAllocationObj {
    public MemoryAllocationVma(Handle base, Handle handle) {
        super(base, handle);
    }

    public MemoryAllocationVma(Handle base, MemoryAllocationCInfo cInfo) {
        super(base, cInfo);
    }
}
