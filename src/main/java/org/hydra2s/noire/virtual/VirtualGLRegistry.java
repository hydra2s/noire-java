package org.hydra2s.noire.virtual;

//
import org.hydra2s.noire.objects.BasicObj;

// TODO: use virtual GL registry per heaps
// TODO: add support for virtual GL indices
// TODO: GL virtual index is always (descriptor index) + 1
// TODO: default descriptor index is -1
public class VirtualGLRegistry extends BasicObj {

    // TODO: add base constructor with `cInfo`
    public VirtualGLRegistry(Handle base, Handle handle) {
        super(base, handle);
    }

    // TODO: add base constructor with `cInfo`
    static public class VirtualGLObj extends BasicObj {

        public VirtualGLObj(Handle base, Handle handle) {
            super(base, handle);
        }
    }

}
