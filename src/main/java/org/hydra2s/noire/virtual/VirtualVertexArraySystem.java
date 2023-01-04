package org.hydra2s.noire.virtual;

//
import org.hydra2s.noire.objects.BasicObj;

//
import java.nio.ByteBuffer;
import java.util.ArrayList;

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

    // byte-based structure data
    // every vertex array binding by stride is (8 + 8 + 4 + 4 + 4 + 4) bytes
    // DO NOT overflow such limit!
    public static class VertexArrayBinding {
        public long bufferAddress = 0L;
        public long bufferSize = 0L;
        public int relativeOffset = 0;
        public int stride = 0;
        public int format = 0;
        public int unknown = 0;

        //
        public VertexArrayBinding writeData(ByteBuffer bindingsMapped, long offset) {
            return this;
        }
    }

    // also, after draw, vertex and/or index buffer data can/may be changed.
    // for BLAS-based recommended to use a fixed data.
    public static class VirtualVertexArrayObj extends BasicObj {
        // If you planned to use with AS
        public long BLASHandle = 0L;
        public long BLASAddress = 0L;
        public ArrayList<VertexArrayBinding> bindings = null;
        public ByteBuffer bindingsMapped = null;

        //
        public VirtualVertexArrayObj(Handle base, Handle handle) {
            super(base, handle);
        }
    }

}
