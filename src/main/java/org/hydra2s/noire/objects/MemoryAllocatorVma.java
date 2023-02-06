package org.hydra2s.noire.objects;

//
import org.hydra2s.noire.descriptors.MemoryAllocatorCInfo;
import org.hydra2s.noire.descriptors.UtilsCInfo;

// TODO: VMA memory allocator and allocation
public class MemoryAllocatorVma extends MemoryAllocatorObj {
    public MemoryAllocatorVma(UtilsCInfo.Handle base, UtilsCInfo.Handle handle) {
        super(base, handle);
    }

    public MemoryAllocatorVma(UtilsCInfo.Handle base, MemoryAllocatorCInfo cInfo) {
        super(base, cInfo);
    }
}
