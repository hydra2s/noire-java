package org.hydra2s.noire.virtual;

//
import org.hydra2s.noire.descriptors.UtilsCInfo;
import org.hydra2s.noire.objects.BasicObj;
import org.hydra2s.noire.objects.LightBasicObj;
import org.hydra2s.noire.objects.PipelineLayoutObj;

import java.util.TreeMap;

// TODO: needs to add morton coding support (for registry)
// TODO: also, needs to add ordering support (by morton code)
// TODO: 3D-attach technology (based on sorting and morton codes)
public class VirtualGLRegistry extends BasicObj {

    //
    protected PipelineLayoutObj.OutstandingArray<VirtualGLObj> registry = null;
    protected TreeMap<Long, VirtualGLObj> sorted = null;

    //
    public VirtualGLRegistry(UtilsCInfo.Handle base, UtilsCInfo.Handle handle) {
        super(base, handle);

        this.registry = new PipelineLayoutObj.OutstandingArray<>();
        this.sorted = new TreeMap<Long, VirtualGLObj>();
    }

    //
    public VirtualGLRegistry(UtilsCInfo.Handle base, VirtualGLRegistryCInfo cInfo) {
        super(base, cInfo);

        this.registry = new PipelineLayoutObj.OutstandingArray<>();
        this.sorted = new TreeMap<Long, VirtualGLObj>();
    }

    // return by `dscId` + 1
    public VirtualGLObj removeByGlId(int glId) {
        return registry.removeIndex(glId-1);
    }
    public VirtualGLObj removeByDscId(int dscId) {
        return registry.removeIndex(dscId);
    }

    // return by `dscId` + 1
    public VirtualGLObj getByGlId(int glId) {
        return registry.get(glId-1);
    }
    public VirtualGLObj getByDscId(int dscId) {
        return registry.get(dscId);
    }

    // return by `dscId` + 1
    public int glIndexOf(VirtualGLObj obj) {
        return registry.indexOf(obj)+1;
    }
    public int dscIndexOf(VirtualGLObj obj) {
        return registry.indexOf(obj);
    }

    //
    public VirtualGLRegistry clear() {
        this.registry.clear();
        return this;
    }

    // sorting by morton-codes
    public TreeMap<Long, VirtualGLObj> applyOrdering() {
        sorted.clear();
        registry.stream().forEach((R)->{
            if (R != null) { sorted.put(((VirtualGLRegistryCInfo.VirtualGLObjCInfo) R.cInfo).mortonCode, R); }
        });
        return sorted;
    }

    //
    static public class VirtualGLObj extends LightBasicObj {
        // virtual GL is always is `DSC_ID`+1
        public int DSC_ID = -1; // for shaders
        public int virtualGL = 0; // for virtual OpenGL

        //
        public VirtualGLRegistry bound = null;
        public VirtualGLObj(UtilsCInfo.Handle handle) {
            super(handle);
        }
        public VirtualGLObj(VirtualGLRegistryCInfo.VirtualGLObjCInfo cInfo) {
            super(cInfo);
        }
    }

}
