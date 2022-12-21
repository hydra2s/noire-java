package org.hydra2s.manhack.objects;

//
import java.nio.LongBuffer;
import java.util.HashMap;

import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.*;

import org.hydra2s.manhack.descriptors.BasicCInfo;

//
public class BasicObj {

    public static class Handle {
        protected PointerBuffer handle = null;
        protected int type = 0;

        public Handle(int type) {
            this.handle = PointerBuffer.allocateDirect(1);
            this.type = type;
        }

        public Handle(PointerBuffer handle2, int type) {
            this.handle = handle2;
            this.type = type;
        }

        public Handle(long handle, int type) {
            this.handle = PointerBuffer.create(handle, 1);
            this.type = type;
        }

        public int getType() {return this.type; };
        public long get() { return this.handle.get(); }
        public PointerBuffer ptr() { return handle; }
    }

    //
    protected Handle base = new Handle(0, 0);
    protected Handle handler = new Handle(0, 0);
    protected BasicCInfo cInfo = null;

    // 
    public static HashMap<Long, BasicObj> globalHandleMap = new HashMap<Long, BasicObj>();

    // TODO: make correct hashmap
    public HashMap<Handle, BasicObj> handleMap = new HashMap<Handle, BasicObj>();

    //
    public BasicObj(Handle base, Handle handler) {
        this.base = base;
        this.handler = handler;
    }

    //
    public BasicObj(Handle base, BasicCInfo cInfo) {
        this.base = base;
        this.cInfo = cInfo;
    }

}
