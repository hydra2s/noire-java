package org.hydra2s.manhack.descriptors;

import java.util.ArrayList;

public class AccelerationStructureCInfo extends BasicCInfo {

    public static class InstanceGeometryCInfo {
        public boolean opaque = false;
        public DataCInfo.InstanceBindingCInfo instanceBinding = null;
    }

    public static class TriangleGeometryCInfo {
        public boolean opaque = false;
        public DataCInfo.VertexBindingCInfo vertexBinding = null;
        public DataCInfo.IndexBindingCInfo indexBinding = null;
    }

    //
    public InstanceGeometryCInfo instances = null;
    public ArrayList<TriangleGeometryCInfo> geometries = null;
    public long memoryAllocator = 0;

    //
    public AccelerationStructureCInfo () {
        
    }

    //
    public static class TopAccelerationStructureCInfo extends AccelerationStructureCInfo {
        public TopAccelerationStructureCInfo() {

        }
    }

    //
    public static class BottomAccelerationStructureCInfo extends AccelerationStructureCInfo {
        public BottomAccelerationStructureCInfo() {

        }
    }


}
