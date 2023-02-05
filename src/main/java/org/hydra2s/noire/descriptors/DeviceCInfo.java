package org.hydra2s.noire.descriptors;

//

import org.hydra2s.noire.objects.DeviceObj;

import java.util.ArrayList;

//
public class DeviceCInfo extends BasicCInfo {

    //
    public static class QueueGroup {
        public int queueFamilyIndex = 0;
        public int commandPoolCount = 1;
        public ArrayList<Integer> queueIndices = null;
        public ArrayList<Integer> queueBusy = null;


        // UNUSED!
        // TODO: add in gen-v2 part III or IV
        public ArrayList<Integer> commandPoolIndices = null;

        //
        public long[] cmdPool = {};//memAllocLong(10).put(0, 0);
        public ArrayList<DeviceObj.CommandPoolInfo> commandPoolInfo = null;
    };

    //
    public static class QueueFamilyCInfo {
        public int index = 0;
        public int commandPoolCount = 1;
        public float[] priorities = new float[]{};
    };

    //
    public ArrayList<QueueFamilyCInfo> queueFamilies = null;//new ArrayList<QueueFamilyCInfo>();
    public ArrayList<QueueGroup> queueGroups = null;//new ArrayList<QueueGroup>();

}
