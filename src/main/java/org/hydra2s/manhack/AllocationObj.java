package org.hydra2s.manhack;

public class AllocationObj extends BasicObj  {

    public AllocationObj(Handle base, Handle handler) {
        super(base, handler);
    }

    public class BufferObj extends AllocationObj {

        public BufferObj(Handle base, Handle handler) {
            super(base, handler);
        }
    }

    public class ImageObj extends AllocationObj {

        public ImageObj(Handle base, Handle handler) {
            super(base, handler);
        }
    }

}
