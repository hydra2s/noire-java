package org.hydra2s.noire.objects;

//

import com.lodborg.intervaltree.IntervalTree;
import org.hydra2s.noire.descriptors.BasicCInfo;
import org.lwjgl.PointerBuffer;

import java.nio.IntBuffer;
import java.util.HashMap;

import static org.lwjgl.system.MemoryUtil.memAllocInt;
import static org.lwjgl.system.MemoryUtil.memAllocPointer;

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
    public PointerBuffer Win32Handle = memAllocPointer(1).put(0, 0);
    public IntBuffer FdHandle = memAllocInt(1).put(0, 0);

    //
    protected Handle base = new Handle("Unknown", 0);
    protected Handle handle = new Handle("Unknown", 0);
    public BasicCInfo cInfo = null;

    //
    public static HashMap<Long, BasicObj> globalHandleMap = new HashMap<Long, BasicObj>(128);

    // TODO: make correct hashmap
    public HashMap <Handle, BasicObj> handleMap = new HashMap<Handle, BasicObj>(1024);

    // We prefer interval maps, for getting buffers, acceleration structures, etc. when it really needed...
    public IntervalTree<Long> addressMap = new IntervalTree<>();
    public HashMap<Long, Long> rootMap = new HashMap<Long, Long>(1024);

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

    // TODO: add destructors support
    // TODO: add parameters support
    public BasicObj delete() throws Exception {

        return this;
    }

    public BasicObj deleteDirectly() throws Exception {

        return this;
    }

}
