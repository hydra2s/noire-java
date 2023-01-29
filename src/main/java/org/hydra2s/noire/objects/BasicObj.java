package org.hydra2s.noire.objects;

//

import com.lodborg.intervaltree.IntervalTree;
import com.perapoch.cache.lru.NativeLRUCache;
import org.hydra2s.noire.descriptors.BasicCInfo;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;

import java.nio.IntBuffer;
import java.util.*;

import static org.lwjgl.system.MemoryUtil.memAllocInt;
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


    public static class CombinedMap <K, V> extends HashMap<K, Optional<V>> {
        public NativeLRUCache<K, V> cache = null;

        public CombinedMap(int capacity) {
            super(1024);
            this.cache = new NativeLRUCache<K, V>(capacity);
        }

        //
        public void put$(K key, V value) {
            cache.put(key, value);
            super.put(key, Optional.ofNullable(value));
        }

        /*@Override
        public Optional<V> get(Object key) {
            return Optional.ofNullable(super.containsKey(key) ? super.get(key).orElse(null) : null);
        }*/

        @Override
        public Optional<V> get(Object key) {
            return cache.containsKey((K) key) ? cache.get((K) key) : Optional.ofNullable(super.containsKey(key) ? super.get(key).orElse(null) : null);
        }

        @Override
        public Optional<V> put(K key, Optional<V> value) {
            cache.put(key, value.orElse(null));
            super.put(key, value);
            return value;
        }

        @Override
        public Optional<V> remove(Object key) {
            var a = cache.remove((K) key);
            var b = Optional.ofNullable(super.containsKey(key) ? super.remove(key).orElse(null) : null);
            return Optional.ofNullable(a.orElse(b.orElse(null)));
        }

        @Override
        public boolean containsKey(Object key) {
            return (cache.containsKey((K) key) || super.containsKey(key));
        }

        @Override
        public int size() {
            return super.size();
        }
    }

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
    public static CombinedMap<Long, BasicObj> globalHandleMap = new CombinedMap<Long, BasicObj>(16);

    // TODO: make correct hashmap
    public CombinedMap<Handle, BasicObj> handleMap = new CombinedMap<Handle, BasicObj>(16);

    // We prefer interval maps, for getting buffers, acceleration structures, etc. when it really needed...
    public IntervalTree<Long> addressMap = new IntervalTree<>();
    public CombinedMap<Long, Long> rootMap = new CombinedMap<Long, Long>(16);

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
        protected PointerBuffer handle = null;
        protected String type = "unknown";
        private int cached = 0;

        public Handle(String type) {
            this.handle = PointerBuffer.allocateDirect(1);
            this.handle.put(0, 0);
            this.type = type;
            this.cached = 0;
        }

        public Handle(String type, PointerBuffer handle2) {
            this.handle = handle2;
            this.type = type;
            this.cached = 0;
        }

        public Handle(String type, long handle) {
            this.handle = PointerBuffer.allocateDirect(1);
            this.handle.put(0, handle);
            this.type = type;
            this.cached = 0;
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
    public BasicObj delete() throws Exception {
        return this;
    }

    public BasicObj deleteDirectly() throws Exception {
        if (sharedPtr > 0) { sharedPtr--; }
        return this;
    }

}
