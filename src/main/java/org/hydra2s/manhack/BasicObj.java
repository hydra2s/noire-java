package org.hydra2s.manhack;

//
import java.nio.LongBuffer;
import java.util.HashMap;

//
public class BasicObj {

    public static class Handle {
        protected long handle = 0;
        protected int type = 0;

        public Handle(LongBuffer buf, int type) {
            this.handle = buf.get(0);
            this.type = type;
        }

        public Handle(long handle, int type) {
            this.handle = handle;
            this.type = type;
        }

        public int getType() {return this.type; };
        public long get() { return this.handle; }
        public LongBuffer buffer() { long[] tmp = new long[1]; tmp[0] = this.handle; return LongBuffer.wrap(tmp); }

    }

    protected Handle base = new Handle(0, 0);
    protected Handle handler = new Handle(0, 0);

    protected HashMap<Handle, BasicObj> handleMap = new HashMap<Handle, BasicObj>();

    public BasicObj(Handle base, Handle handler) {
        this.base = base;
        this.handler = handler;
    }

}
