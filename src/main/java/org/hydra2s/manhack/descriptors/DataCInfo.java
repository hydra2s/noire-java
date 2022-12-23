package org.hydra2s.manhack.descriptors;

import static org.lwjgl.vulkan.VK10.VK_FORMAT_R32G32B32_SFLOAT;

public class DataCInfo extends BasicCInfo {
    public long address = 0L;
    public int vertexCount = 0;

    public static class VertexBindingCInfo extends DataCInfo {
        public int format = VK_FORMAT_R32G32B32_SFLOAT;
        public int stride = 16;
    }

    public static class IndexBindingCInfo extends DataCInfo {
        public int type = 0;
    }

    public static class InstanceBindingCInfo extends DataCInfo {

    }

}
