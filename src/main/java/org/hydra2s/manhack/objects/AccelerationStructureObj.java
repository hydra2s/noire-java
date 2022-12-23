package org.hydra2s.manhack.objects;

public class AccelerationStructureObj extends BasicObj {
    public AccelerationStructureObj(Handle base, Handle handle) {
        super(base, handle);
    }


    static public class TopAccelerationStructureObj extends AccelerationStructureObj {

        public TopAccelerationStructureObj(Handle base, Handle handle) {
            super(base, handle);
        }
    }

    static public class BottomAccelerationStructureObj extends AccelerationStructureObj {

        public BottomAccelerationStructureObj(Handle base, Handle handle) {
            super(base, handle);
        }
    }
}
