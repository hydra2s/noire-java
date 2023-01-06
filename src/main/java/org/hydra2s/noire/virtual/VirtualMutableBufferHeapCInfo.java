package org.hydra2s.noire.virtual;

public class VirtualMutableBufferHeapCInfo extends VirtualGLRegistryCInfo {

    public long bufferHeapSize = 1024L * 1024L * 32L;
    public long memoryAllocator = 0L;
    public boolean isHost = false;

    public static class VirtualMutableBufferCInfo extends VirtualGLObjCInfo {
        public long vertexBufferOffset = 0L;
        public long indexBufferOffset = 0L;

    }

}
