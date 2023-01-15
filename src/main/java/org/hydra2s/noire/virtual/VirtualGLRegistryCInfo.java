package org.hydra2s.noire.virtual;

//
import org.hydra2s.noire.descriptors.BasicCInfo;

// TODO: 3D-attach technology (based on sorting and morton codes)
public class VirtualGLRegistryCInfo extends BasicCInfo {

    //
    static public class VirtualGLObjCInfo extends BasicCInfo {
        public long registryHandle = 0L;

        // required for sorting in BLAS chunks (reduce disorder when draw)
        public long mortonCode = 0L;
    }

}
