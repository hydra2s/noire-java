package org.hydra2s.noire.virtual;

import org.hydra2s.noire.descriptors.BasicCInfo;

public class VirtualMutableBufferSystemCInfo extends BasicCInfo {

    public long bufferHeapSize = 1024L * 1024L * 32L;
    public long memoryAllocator = 0L;
    public boolean isHost = false;

    public class VirtualMutableBufferCInfo extends BasicCInfo {
        public long bufferHeapHandle = 0L;

    }

}
