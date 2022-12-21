package org.hydra2s.manhack.descriptors;

public class DeviceCInfo extends BasicCInfo {

    //
    public static class QueueFamilyCInfo {
        public int index = 0;
        public float[] priorities = new float[]{};
    }

    //
    public QueueFamilyCInfo[] queueFamilies = new QueueFamilyCInfo[]{};

}
