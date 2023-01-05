package org.hydra2s.noire.virtual;

//
import org.hydra2s.noire.descriptors.BasicCInfo;

//
public class VirtualVertexArrayHeapCInfo extends BasicCInfo {
    public long bufferHeapSize = 256L * 1024L;
    public long memoryAllocator = 0L;

    //
    public class VirtualVertexArrayCInfo extends BasicCInfo {
        public long bufferHeapHandle = 0L;
    }

}
