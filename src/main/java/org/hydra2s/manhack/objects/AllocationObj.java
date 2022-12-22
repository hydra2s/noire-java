package org.hydra2s.manhack.objects;

import org.hydra2s.manhack.descriptors.AllocationCInfo;
import org.lwjgl.PointerBuffer;

import static org.lwjgl.system.MemoryUtil.memAllocPointer;

public class AllocationObj extends BasicObj {

    //
    public long memoryOffset = 0;
    public boolean isBuffer = false;
    public boolean isImage = false;

    //
    public PointerBuffer deviceMemory = memAllocPointer(1);

    //
    public AllocationObj(Handle base, Handle handle) {
        super(base, handle);
    }
    public AllocationObj(Handle base, AllocationCInfo handle) {
        super(base, handle);
    }

    //
    static public class BufferObj extends AllocationObj {

        public BufferObj(Handle base, Handle handle) {
            super(base, handle);
        }

        public BufferObj(Handle base, AllocationCInfo.BufferCInfo cInfo) {
            super(base, cInfo);

        }
    }

    //
    static public class ImageObj extends AllocationObj {

        public ImageObj(Handle base, Handle handle) {
            super(base, handle);
        }

        public ImageObj(Handle base, AllocationCInfo.ImageCInfo cInfo) {
            super(base, cInfo);

        }
    }

}
