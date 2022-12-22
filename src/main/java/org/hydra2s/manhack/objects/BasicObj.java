package org.hydra2s.manhack.objects;

//

import org.hydra2s.manhack.descriptors.BasicCInfo;
import org.lwjgl.PointerBuffer;

import java.util.HashMap;

//
// DO NOT `wrap`! Use `memAlloc#Type()` with `put(pos, val)`!
// DO NOT `allocate(N)`, use `memAlloc#Type()`
//

//
public class BasicObj {

    //
    public Handle getHandle() {
        return handle;
    }

    //
    protected Handle base = new Handle("unknown", 0);
    protected Handle handle = new Handle("unknown", 0);
    protected BasicCInfo cInfo = null;

    //
    public static HashMap<Long, BasicObj> globalHandleMap = new HashMap<Long, BasicObj>();

    // TODO: make correct hashmap
    public HashMap<Handle, BasicObj> handleMap = new HashMap<Handle, BasicObj>();

    //
    public BasicObj(Handle base, Handle handle) {
        this.base = base;
        this.handle = handle;
    }

    //
    public BasicObj(Handle base, BasicCInfo cInfo) {
        this.base = base;
        this.cInfo = cInfo;
    }

    //
    public Handle getBase() {
        return base;
    }

    public static class Handle {
        protected PointerBuffer handle = null;
        protected String type = "unknown";

        public Handle(String type) {
            this.handle = PointerBuffer.allocateDirect(1);
            this.handle.put(0, 0);
            this.type = type;
        }

        public Handle(String type, PointerBuffer handle2) {
            this.handle = handle2;
            this.type = type;
        }

        public Handle(String type, long handle) {
            this.handle = PointerBuffer.allocateDirect(1);
            this.handle.put(0, handle);
            this.type = type;
        }

        public String getType() {
            return this.type;
        }

        public long get() {
            return this.handle.get(0);
        }

        public PointerBuffer ptr() {
            return handle;
        }

        public long address() {
            return this.handle.address(0);
        }

    }

}
