package org.hydra2s.noire.virtual;

//
import org.hydra2s.noire.objects.BasicObj;

//
import java.nio.ByteBuffer;
import java.util.ArrayList;

//
import static org.lwjgl.system.MemoryUtil.memSlice;

// Will be used in buffer based registry
// Will uses outstanding array
// Bindings depends on shaders
public class VirtualVertexArraySystem extends BasicObj {

    //
    public static final int vertexArrayStride = 256;
    public static final int vertexBindingStride = 32;

    //
    public VirtualVertexArraySystem(Handle base, Handle handle) {
        super(base, handle);
    }

    // But before needs to create such system
    public VirtualVertexArraySystem(Handle base, VirtualVertexArraySystemCInfo cInfo) {
        super(base, cInfo);
    }

    // byte-based structure data
    // every vertex array binding by stride is (8 + 8 + 4 + 4 + 4 + 4) bytes
    // DO NOT overflow such limit!
    public static class VertexBinding {
        public long bufferAddress = 0L;
        public long bufferSize = 0L;
        public int relativeOffset = 0;
        public int stride = 0;
        public int format = 0;
        public int unknown = 0;

        //
        public VertexBinding writeData(ByteBuffer bindingsMapped, long offset) {
            memSlice(bindingsMapped, (int) (offset + 0), 8).putLong(0, bufferAddress);
            memSlice(bindingsMapped, (int) (offset + 8), 8).putLong(0, bufferSize);
            memSlice(bindingsMapped, (int) (offset + 16), 4).putInt(0, relativeOffset);
            memSlice(bindingsMapped, (int) (offset + 20), 4).putInt(0, stride);
            memSlice(bindingsMapped, (int) (offset + 24), 4).putInt(0, format);
            memSlice(bindingsMapped, (int) (offset + 28), 4).putInt(0, unknown);
            return this;
        }
    }

    // also, after draw, vertex and/or index buffer data can/may be changed.
    // for BLAS-based recommended to use a fixed data.
    public static class VirtualVertexArrayObj extends BasicObj {
        // If you planned to use with AS
        public long BLASHandle = 0L;
        public long BLASAddress = 0L;
        public ArrayList<VertexBinding> bindings = null;
        public ByteBuffer bindingsMapped = null;

        //
        public VirtualVertexArrayObj(Handle base, Handle handle) {
            super(base, handle);
        }
        public VirtualVertexArrayObj(Handle base, VirtualVertexArraySystemCInfo.VirtualVertexArrayCInfo cInfo) {
            super(base, cInfo);
        }


    }

}
