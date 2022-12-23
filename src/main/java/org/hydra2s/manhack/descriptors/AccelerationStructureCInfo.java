package org.hydra2s.manhack.descriptors;

//
import java.util.ArrayList;

//
public class AccelerationStructureCInfo extends BasicCInfo {
    //
    public DataCInfo.InstanceGeometryCInfo instances = null;
    public ArrayList<DataCInfo.TriangleGeometryCInfo> geometries = null;
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
