package org.hydra2s.noire.objects;

//
import org.hydra2s.noire.descriptors.MemoryAllocationCInfo;
import org.hydra2s.noire.descriptors.UtilsCInfo;

// TODO: VMA memory allocator and allocation
public class MemoryAllocationVma extends MemoryAllocationObj {
    public MemoryAllocationVma(UtilsCInfo.Handle base, UtilsCInfo.Handle handle) {
        super(base, handle);
    }

    public MemoryAllocationVma(UtilsCInfo.Handle base, MemoryAllocationCInfo cInfo) {
        super(base, cInfo);
    }
}
