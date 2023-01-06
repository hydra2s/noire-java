package org.hydra2s.noire.virtual;

//
import org.hydra2s.noire.objects.BasicObj;
import org.hydra2s.noire.objects.PipelineLayoutObj;

// TODO: use virtual GL registry per heaps
// TODO: add support for virtual GL indices
// TODO: GL virtual index is always (descriptor index) + 1
// TODO: default descriptor index is -1
// TODO: getting virtual GL vertex array with +1 index shift
public class VirtualGLRegistry extends BasicObj {

    public PipelineLayoutObj.OutstandingArray<VirtualGLObj> registry = null;

    public VirtualGLRegistry(Handle base, Handle handle) {
        super(base, handle);

        this.registry = new PipelineLayoutObj.OutstandingArray<>();
    }

    public VirtualGLRegistry(Handle base, VirtualGLRegistryCInfo cInfo) {
        super(base, cInfo);

        this.registry = new PipelineLayoutObj.OutstandingArray<>();
    }

    // TODO: add base constructor with `cInfo`
    static public class VirtualGLObj extends BasicObj {
        // virtual GL is always is `DSC_ID`+1
        public int DSC_ID = -1; // for shaders
        public int virtualGL = 0; // for virtual OpenGL

        public VirtualGLRegistry bound = null;

        public VirtualGLObj(Handle base, Handle handle) {
            super(base, handle);
        }

        public VirtualGLObj(Handle base, VirtualGLRegistryCInfo.VirtualGLObjCInfo cInfo) {
            super(base, cInfo);
        }
    }

}
