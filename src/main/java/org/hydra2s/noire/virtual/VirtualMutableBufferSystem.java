package org.hydra2s.noire.virtual;

//
import org.hydra2s.noire.objects.BasicObj;

// Will uses large buffer concept with virtual allocation (VMA)
public class VirtualMutableBufferSystem extends BasicObj {

    //
    public VirtualMutableBufferSystem(Handle base, Handle handle) {
        super(base, handle);
    }

    // But before needs to create such system
    //public VirtualMutableBufferSystem(Handle base, VirtualMutableBufferSystemCInfo cInfo) {
        //super(base, cInfo);
    //}

    // Will be able to deallocate and re-allocate again
    public static class VirtualMutableBufferObj extends BasicObj {

        public VirtualMutableBufferObj(Handle base, Handle handle) {
            super(base, handle);
        }
    }

}
