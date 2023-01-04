package org.hydra2s.noire.virtual;

//
import org.hydra2s.noire.objects.BasicObj;

// Will be used in buffer based registry
// Will uses outstanding array
// Bindings depends on shaders
public class VirtualVertexArraySystem extends BasicObj {

    //
    public VirtualVertexArraySystem(Handle base, Handle handle) {
        super(base, handle);
    }

    // But before needs to create such system
    //public VirtualVertexArraySystem(Handle base, VirtualVertexArraySystemCInfo cInfo) {
        //super(base, cInfo);
    //}

    //
    public static class VirtualVertexArrayObj {

    }

}
