package org.hydra2s.noire.descriptors;

//
import java.util.ArrayList;

//
public class DeviceCInfo extends BasicCInfo {

    //
    public static class QueueGroup {
        public int queueFamilyIndex = 0;
        public ArrayList<Integer> queueIndices = null;
        public ArrayList<Integer> queueBusy = null;
    };

    //
    public static class QueueFamilyCInfo {
        public int index = 0;
        public float[] priorities = new float[]{};
    }

    //
    public ArrayList<QueueFamilyCInfo> queueFamilies = null;//new ArrayList<QueueFamilyCInfo>();
    public ArrayList<QueueGroup> queueGroups = null;//new ArrayList<QueueGroup>();

}
