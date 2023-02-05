package org.hydra2s.noire.objects;

//

import com.lodborg.intervaltree.IntervalTree;
import org.hydra2s.noire.descriptors.BasicCInfo;
import org.hydra2s.noire.descriptors.UtilsCInfo;
import org.lwjgl.PointerBuffer;

import java.nio.IntBuffer;
import java.util.Optional;

import static org.lwjgl.system.MemoryUtil.memAllocPointer;

//
// DO NOT `wrap`! Use `memAlloc#Type()` with `put(pos, val)`!
// DO NOT `allocate(N)`, use `memAlloc#Type()`
//

//
public class BasicObj {
    public InstanceObj instanceObj;
    public DeviceObj deviceObj;
    public PhysicalDeviceObj physicalDeviceObj;
    public MemoryAllocatorObj memoryAllocatorObj;

    // TODO: globalize such flag
    public boolean deleted = false;
    public long sharedPtr = 0;

    //
    public Handle getHandle() {
        return handle;
    }

    //
    public PointerBuffer Win32Handle = null;//memAllocPointer(1).put(0, 0);
    public int[] FdHandle = {};//memAllocInt(1).put(0, 0);

    //
    protected PointerBuffer ptr = null;
    protected Handle base = null;//new Handle("Unknown", 0);
    protected Handle handle = null;//new Handle("Unknown", 0);
    public BasicCInfo cInfo = null;

    //
    public static UtilsCInfo.CombinedMap<Long, BasicObj> globalHandleMap = new UtilsCInfo.CombinedMap<Long, BasicObj>();

    // TODO: make correct hashmap
    public UtilsCInfo.CombinedMap<Handle, BasicObj> handleMap = null;

    // We prefer interval maps, for getting buffers, acceleration structures, etc. when it really needed...
    public IntervalTree<Long> addressMap = null;
    public UtilsCInfo.CombinedMap<Long, Long> rootMap = null;

    // WARNING! May fail up to null
    public Optional<Long> getHandleByAddress(long deviceAddress) {
        var interval = addressMap.query(deviceAddress);
        var handle = rootMap.get(interval.stream().findFirst().orElse(null).getStart());
        return handle;
    }

    //
    public BasicObj(Handle base, Handle handle) {
        this.base = base;
        this.handle = handle;
        this.ptr = memAllocPointer(1);

        if (base != null) {
            if (base.getType() == "Device") {
                this.deviceObj = (DeviceObj) globalHandleMap.get(base.get()).orElse(null);
            }

            if (base.getType() == "PhysicalDevice") {
                this.physicalDeviceObj = (PhysicalDeviceObj) globalHandleMap.get(base.get()).orElse(null);
            }

            if (base.getType() == "Instance") {
                this.instanceObj = (InstanceObj) globalHandleMap.get(base.get()).orElse(null);
            }

            if (base.getType() == "MemoryAllocator") {
                this.memoryAllocatorObj = (MemoryAllocatorObj) globalHandleMap.get(base.get()).orElse(null);
            }
        }

        if (this.memoryAllocatorObj != null) {
            this.deviceObj = this.memoryAllocatorObj.deviceObj;
        }
        if (this.deviceObj != null) {
            this.physicalDeviceObj = this.deviceObj.physicalDeviceObj;
        }
        if (this.physicalDeviceObj != null) {
            this.instanceObj = this.physicalDeviceObj.instanceObj;
        }
    }

    //
    public BasicObj(Handle base, BasicCInfo cInfo) {
        this.base = base;
        this.cInfo = cInfo;
        this.ptr = memAllocPointer(1);//memAllocPointer(1);

        if (base != null) {
            if (base.getType() == "Device") {
                this.deviceObj = (DeviceObj) globalHandleMap.get(base.get()).orElse(null);
            }

            if (base.getType() == "PhysicalDevice") {
                this.physicalDeviceObj = (PhysicalDeviceObj) globalHandleMap.get(base.get()).orElse(null);
            }

            if (base.getType() == "Instance") {
                this.instanceObj = (InstanceObj) globalHandleMap.get(base.get()).orElse(null);
            }

            if (base.getType() == "MemoryAllocator") {
                this.memoryAllocatorObj = (MemoryAllocatorObj) globalHandleMap.get(base.get()).orElse(null);
            }

        }

        if (this.memoryAllocatorObj != null) {
            this.deviceObj = this.memoryAllocatorObj.deviceObj;
        }
        if (this.deviceObj != null) {
            this.physicalDeviceObj = this.deviceObj.physicalDeviceObj;
        }
        if (this.physicalDeviceObj != null) {
            this.instanceObj = this.physicalDeviceObj.instanceObj;
        }
    }

    //
    public Handle getBase() {
        return base;
    }

    public static class Handle {
        protected long[] handle = {};
        protected String type = "unknown";
        private int cached = 0;

        public Handle(String type) {
            this.handle = new long[]{0L};
            this.type = type;
            this.cached = 0;
        }

        public Handle(String type, long[] handle2) {
            this.handle = handle2;
            this.type = type;
            this.cached = 0;
        }

        public Handle(String type, long handle) {
            this.handle = new long[]{handle};
            this.type = type;
            this.cached = 0;
        }

        public Handle(String type, PointerBuffer ptr) {
            this.handle = new long[]{ptr.get(0)};
            this.type = type;
            this.cached = 0;
        }

        public String getType() {
            return this.type;
        }

        public long get() {
            return this.handle[0];
        }

        public long[] ptr() {
            return handle;
        }

        //public long address() {
            //return this.handle.address(0);
        //}

        @Override
        public boolean equals(Object o) {
            return (this.hashCode() == o.hashCode());
            //return this.handle.get(0) == ((Handle)o).get() && this.type.equals(((Handle)o).getType());
        }

        @Override
        public int hashCode() {
            if (cached == 0) {
                final int prime = 31;
                cached = 1;
                cached = prime * cached + this.type.hashCode();
                cached = prime * cached + Long.hashCode(this.get());
            }
            return cached;
        }
    }

    // TODO: add destructors support
    // TODO: add parameters support
    /*public BasicObj delete() throws Exception {
        return this;
    }*/

    public BasicObj deleteDirectly() /*throws Exception*/ {
        if (sharedPtr > 0) { sharedPtr--; }
        //if (sharedPtr <= 0) { memFree(this.ptr); };
        return this;
    }

}
