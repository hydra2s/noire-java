package org.hydra2s.noire.virtual;

import java.util.ArrayList;

public class VirtualMutableBufferHeapCInfo extends VirtualGLRegistryCInfo {

    public long memoryAllocator = 0L;
    public ArrayList<VirtualMemoryHeapCInfo> heapCInfo = null;

    public static class VirtualMemoryHeapCInfo extends VirtualGLObjCInfo {
        public long bufferHeapSize = 1024L * 1024L * 32L;
        public boolean isHost = false;
    }

    public static class VirtualMutableBufferCInfo extends VirtualGLObjCInfo {
        public int heapId = 0;
    }

}
