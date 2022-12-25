package org.hydra2s.noire.objects;

//

import com.lodborg.intervaltree.IntervalTree;
import org.hydra2s.noire.descriptors.BasicCInfo;
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
    protected Handle base = new Handle("Unknown", 0);
    protected Handle handle = new Handle("Unknown", 0);
    protected BasicCInfo cInfo = null;

    //
    public static HashMap<Long, BasicObj> globalHandleMap = new HashMap<Long, BasicObj>();

    // TODO: make correct hashmap
    public HashMap<Handle, BasicObj> handleMap = new HashMap<Handle, BasicObj>();

    // We prefer interval maps, for getting buffers, acceleration structures, etc. when it really needed...
    public IntervalTree<Long> addressMap = new IntervalTree<>();
    public HashMap<Long, Long> rootMap = new HashMap<Long, Long>();

    // WARNING! May fail up to null
    public long getHandleByAddress(long deviceAddress) {
        var interval = addressMap.query(deviceAddress);
        var handle = rootMap.get(interval.stream().findFirst().orElse(null).getStart());
        return handle;
    }

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

        @Override
        public boolean equals(Object o) {
            return this.handle.get(0) == ((Handle)o).get() && this.type.equals(((Handle)o).getType());
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + this.type.hashCode();
            result = prime * result + Long.hashCode(this.get());
            return result;
        }
    }

}