package org.hydra2s.noire.virtual;

import org.lwjgl.vulkan.VkDescriptorBufferInfo;

import java.nio.ByteBuffer;

import static org.lwjgl.vulkan.VK10.VK_INDEX_TYPE_UINT16;
import static org.lwjgl.vulkan.VK10.VK_WHOLE_SIZE;

public class VirtualDrawCallCollectorCInfo extends VirtualGLRegistryCInfo {

    //
    public int vertexAverageStride = 36;
    public int vertexAverageCount = 512;

    // uniform data + VAO bindings + inbound payload
    public int drawCallUniformStride = 64;

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

    //
    static public class VirtualDrawCallCInfo extends VirtualGLObjCInfo {
        // i.e. push constant
        public ByteBuffer uniformData = null;

        // 0 = temporary, 1 = direct
        // in temp mode => copy, in direct mode just use it
        public int vertexCount = 0; public long vertexAddress = 0L; public VkDescriptorBufferInfo vertexRange = null;
        public int indexCount = 0; public long indexAddress = 0L; public VkDescriptorBufferInfo indexRange = null;
        public int indexType = VK_INDEX_TYPE_UINT16;

        //
        public VirtualVertexArrayHeap.VirtualVertexArrayObj vertexArray = null;
    }


}
