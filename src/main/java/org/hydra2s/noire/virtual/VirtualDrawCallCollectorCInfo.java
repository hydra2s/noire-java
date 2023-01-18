package org.hydra2s.noire.virtual;

import org.lwjgl.vulkan.VkDescriptorBufferInfo;

import static org.hydra2s.noire.virtual.VirtualVertexArrayHeapCInfo.vertexArrayStride;
import static org.lwjgl.vulkan.KHRAccelerationStructure.VK_INDEX_TYPE_NONE_KHR;
import static org.lwjgl.vulkan.VK10.VK_WHOLE_SIZE;

public class VirtualDrawCallCollectorCInfo extends VirtualGLRegistryCInfo {

    //
    public final static int vertexAverageStride = 32;
    public final static int vertexAverageCount = 768;

    // uniform data + VAO bindings + inbound payload
    public final static int drawCallUniformStride = 512 + VirtualVertexArrayHeapCInfo.vertexArrayStride;

    //
    public long maxDrawCalls = 1024L;
    public long memoryAllocator = 0L;

    // TODO: use host memory too (directly, zero-copy)
    public static class BufferRange {
        public long handle = 0L;
        public long offset = 0L;
        public long range = VK_WHOLE_SIZE;
        public long address = 0L;
        public long stride = 0L;
    }

    // TODO: use host memory too (directly, zero-copy)
    static public class IndexRange {
        public long handle = 0L;
        public long offset = 0L;
        public long range = VK_WHOLE_SIZE;
        public long address = 0L;
        public int type = VK_INDEX_TYPE_NONE_KHR;
    }

    //
    static public class VirtualDrawCallCInfo extends VirtualGLObjCInfo {
        // use direct buffer access instead of copying (for device mode, for host recommended a copying)
        public boolean vertexDirectBufferMode = false;
        public boolean indexDirectBufferMode = false;

        //
        public long vertexArrayHeapHandle = 0L;

        // use DSC_ID for access to vertexArrayObj
        public int vertexArrayObjectId = -1;

        //
        public IndexRange indexData = null;
        public long vertexCount = 0;

        //
        public VkDescriptorBufferInfo uniformRange = null;
    }


}
