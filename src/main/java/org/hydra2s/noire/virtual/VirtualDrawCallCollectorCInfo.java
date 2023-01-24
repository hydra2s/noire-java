package org.hydra2s.noire.virtual;

import org.lwjgl.vulkan.VkDescriptorBufferInfo;

import java.nio.ByteBuffer;

import static org.hydra2s.noire.virtual.VirtualVertexArrayHeapCInfo.vertexArrayStride;
import static org.lwjgl.vulkan.KHRAccelerationStructure.VK_INDEX_TYPE_NONE_KHR;
import static org.lwjgl.vulkan.VK10.VK_WHOLE_SIZE;

public class VirtualDrawCallCollectorCInfo extends VirtualGLRegistryCInfo {

    //
    public final static int vertexAverageStride = 36;
    public final static int vertexAverageCount = 515;

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

    //
    static public class VirtualDrawCallCInfo extends VirtualGLObjCInfo {
        // use vkCmdUpdateBuffer
        public ByteBuffer uniformData;

        // 0 = temporary, 1 = direct
        // in temp mode => copy, in direct mode just use it
        public int vertexMode = 0; public VirtualMutableBufferHeap.VirtualMutableBufferObj vertexBuffer;
        public int indexMode = 1; public VirtualMutableBufferHeap.VirtualMutableBufferObj indexBuffer;

        // just copy their into registry
        public int vertexCount = 0; public VirtualVertexArrayHeap.VirtualVertexArrayObj vertexArray;

    }


}
