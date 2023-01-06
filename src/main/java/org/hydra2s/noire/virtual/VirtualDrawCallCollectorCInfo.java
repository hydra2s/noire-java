package org.hydra2s.noire.virtual;

public class VirtualDrawCallCollectorCInfo extends VirtualGLRegistryCInfo {

    //
    public long maxDrawCalls = 1024L;
    public long memoryAllocator = 0L;

    //
    static public class VirtualDrawCallCInfo extends VirtualGLObjCInfo {
        // use direct buffer access instead of
        public boolean directBufferMode = false;
        public long vertexArrayHeapHandle = 0L;

        // use DSC_ID for access to vertexArrayObj
        public int vertexArrayObjectId = -1;
    }


}
