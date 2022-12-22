package org.hydra2s.manhack.descriptors;

import java.util.ArrayList;

public class DeviceCInfo extends BasicCInfo {

    //
    public static class QueueFamilyCInfo {
        public int index = 0;
        public float[] priorities = new float[]{};
    }

    //
    public ArrayList<QueueFamilyCInfo> queueFamilies = new ArrayList<QueueFamilyCInfo>();

}
