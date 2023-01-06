package org.hydra2s.noire.virtual;

//

import org.hydra2s.noire.descriptors.BufferCInfo;
import org.hydra2s.noire.descriptors.MemoryAllocationCInfo;
import org.hydra2s.noire.objects.*;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.stream.IntStream;

import static org.lwjgl.system.MemoryUtil.memAllocLong;
import static org.lwjgl.system.MemoryUtil.memSlice;
import static org.lwjgl.vulkan.KHRAccelerationStructure.VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR;
import static org.lwjgl.vulkan.VK10.*;

// Will be used in buffer based registry
// Will uses outstanding array
// Bindings depends on shaders
public class VirtualVertexArrayHeap extends VirtualGLRegistry {

    //
    public static final int vertexArrayStride = 256;
    public static final int vertexBindingStride = 32;

    //
    protected BufferObj bufferHeap = null;

    //
    public VirtualVertexArrayHeap(Handle base, Handle handle) {
        super(base, handle);
    }

    // But before needs to create such system
    public VirtualVertexArrayHeap(Handle base, VirtualVertexArrayHeapCInfo cInfo) {
        super(base, cInfo);

        //
        var memoryAllocatorObj = (MemoryAllocatorObj)BasicObj.globalHandleMap.get(this.base.get());
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(memoryAllocatorObj.getBase().get());
        var physicalDeviceObj = (PhysicalDeviceObj)BasicObj.globalHandleMap.get(deviceObj.getBase().get());

        //
        this.handle = new Handle("VirtualVertexArrayHeap", MemoryUtil.memAddress(memAllocLong(1)));
        deviceObj.handleMap.put(this.handle, this);

        // device memory buffer with older GPU (Turing, etc.) or device memory with `map` and staging ops support.
        this.bufferHeap = new CompatibleBufferObj(this.base, new BufferCInfo() {{
            size = cInfo.maxVertexArrayCount * vertexArrayStride;
            usage = VK_BUFFER_USAGE_VERTEX_BUFFER_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT | VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR;
            memoryAllocator = cInfo.memoryAllocator;
            memoryAllocationInfo = new MemoryAllocationCInfo(){{
                isHost = false;
                isDevice = true;
            }};
        }});
    }

    //
    public VirtualVertexArrayHeap writeVertexArrays() {
        this.registry.forEach((obj)->{
            var VAO = (VirtualVertexArrayObj)obj;
            if (VAO != null) { VAO.writeData(); };
        });
        return this;
    }

    // byte-based structure data
    // every vertex array binding by stride is (8 + 8 + 4 + 4 + 4 + 4) bytes
    // DO NOT overflow such limit!
    public static class VertexBinding {
        public long bufferAddress = 0L;
        public long bufferSize = 0L;
        public int relativeOffset = 0;
        public int stride = 0;
        public int format = 0;
        public int unknown = 0;

        //
        public VertexBinding writeData(ByteBuffer bindingsMapped, long offset) {
            memSlice(bindingsMapped, (int) (offset + 0), 8).putLong(0, bufferAddress);
            memSlice(bindingsMapped, (int) (offset + 8), 8).putLong(0, bufferSize);
            memSlice(bindingsMapped, (int) (offset + 16), 4).putInt(0, relativeOffset);
            memSlice(bindingsMapped, (int) (offset + 20), 4).putInt(0, stride);
            memSlice(bindingsMapped, (int) (offset + 24), 4).putInt(0, format);
            memSlice(bindingsMapped, (int) (offset + 28), 4).putInt(0, unknown);
            return this;
        }
    }

    // also, after draw, vertex and/or index buffer data can/may be changed.
    // for BLAS-based recommended to use a fixed data.
    public static class VirtualVertexArrayObj extends VirtualGLObj {
        // Will be used in draw collection
        //public long BLASHandle = 0L;
        //public long BLASAddress = 0L;
        public HashMap<Integer, VertexBinding> bindings = null;
        public ByteBuffer bindingsMapped = null;
        public long bufferOffset = 0L;
        public long address = 0L;

        //
        public VirtualVertexArrayObj(Handle base, Handle handle) {
            super(base, handle);
        }
        public VirtualVertexArrayObj(Handle base, VirtualVertexArrayHeapCInfo.VirtualVertexArrayCInfo cInfo) {
            super(base, cInfo);

            //
            var memoryAllocatorObj = (MemoryAllocatorObj)BasicObj.globalHandleMap.get(this.base.get());
            var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(memoryAllocatorObj.getBase().get());
            var physicalDeviceObj = (PhysicalDeviceObj)BasicObj.globalHandleMap.get(deviceObj.getBase().get());

            //
            var virtualVertexArrayHeap = (VirtualVertexArrayHeap)deviceObj.handleMap.get(new Handle("VirtualVertexArrayHeap", cInfo.registryHandle));

            //
            this.bound = virtualVertexArrayHeap;

            //
            this.DSC_ID = this.bound.registry.push(this);
            this.virtualGL = this.DSC_ID+1;

            //
            this.bindingsMapped = virtualVertexArrayHeap.bufferHeap.map(vertexArrayStride, this.bufferOffset = this.DSC_ID*vertexArrayStride);
            this.address = virtualVertexArrayHeap.bufferHeap.getDeviceAddress() + this.bufferOffset;
        }

        //
        public VkDescriptorBufferInfo getBufferRange() {
            var heap = ((VirtualMutableBufferHeap)this.bound).bufferHeap;
            return VkDescriptorBufferInfo.calloc().set(heap.getHandle().get(), this.bufferOffset, vertexArrayStride);
        }

        //
        public VirtualVertexArrayObj writeData() {
            var bindingOffset = 0L;
            var keySet = bindings.keySet().stream().toList();
            IntStream.range(0, bindings.size()).forEach((I)->{
                var index = keySet.get(I);
                var binding = bindings.get(index);
                if (binding != null) {
                    binding.writeData(bindingsMapped,bindingOffset + vertexBindingStride * index);
                }
            });
            return this;
        }

        //
        public VirtualVertexArrayObj vertexBinding(int index, VertexBinding binding) {
            if (index >= 0 && index < 4) {
                bindings.put(index, binding);
            }
            return this;
        }
    }

}
