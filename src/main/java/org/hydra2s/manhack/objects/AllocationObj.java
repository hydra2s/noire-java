package org.hydra2s.manhack.objects;

public class AllocationObj extends BasicObj  {

    public AllocationObj(Handle base, Handle handle) {
        super(base, handle);
    }

    static public class BufferObj extends AllocationObj {

        public BufferObj(Handle base, Handle handle) {
            super(base, handle);
        }
    }

    static public class ImageObj extends AllocationObj {

        public ImageObj(Handle base, Handle handle) {
            super(base, handle);
        }
    }

}
