package org.hydra2s.noire.virtual;

//

import java.nio.ByteBuffer;

import static org.lwjgl.system.MemoryUtil.memSlice;

//
public class VirtualVertexArrayHeapCInfo extends VirtualGLRegistryCInfo {
    public long maxVertexArrayCount = 1024L;
    public long memoryAllocator = 0L;

    //
    public static final int maxBindings = 8;
    public static final int vertexArrayPayload = 256; // there is also can to be had index buffer
    public static final int vertexBindingStride = 32;
    public static final int vertexArrayStride = vertexBindingStride * maxBindings + vertexArrayPayload;

    // TODO: payload data support
    static public class VirtualVertexArrayCInfo extends VirtualGLObjCInfo {
        public ByteBuffer payloadData;
    }

    // byte-based structure data
    // every vertex array binding by stride is (8 + 8 + 4 + 4 + 4 + 4) bytes
    // DO NOT overflow such limit!
    // TODO: use host memory too (directly, zero-copy)
    public static class VertexBinding {
        public long bufferAddress = 0L;
        public long bufferSize = 0L;
        public int relativeOffset = 0;
        public int stride = 0;
        public int format = 0;
        public int unknown = 0;
        public int location = 0;

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
}
