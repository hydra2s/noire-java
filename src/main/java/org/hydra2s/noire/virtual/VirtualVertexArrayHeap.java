package org.hydra2s.noire.virtual;

//

import org.hydra2s.noire.descriptors.BufferCInfo;
import org.hydra2s.noire.descriptors.MemoryAllocationCInfo;
import org.hydra2s.noire.objects.BufferObj;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.stream.IntStream;

import static org.hydra2s.noire.virtual.VirtualVertexArrayHeapCInfo.*;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.KHRAccelerationStructure.VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR;
import static org.lwjgl.vulkan.VK10.*;

// Will be used in buffer based registry
// Will uses outstanding array
// Bindings depends on shaders
public class VirtualVertexArrayHeap extends VirtualGLRegistry {
    //
    public BufferObj bufferHeap = null;
    public ByteBuffer hostPayload = null;

    //
    public VirtualVertexArrayHeap(Handle base, Handle handle) {
        super(base, handle);
    }

    // But before needs to create such system
    public VirtualVertexArrayHeap(Handle base, VirtualVertexArrayHeapCInfo cInfo) {
        super(base, cInfo);

        //
        this.handle = new Handle("VirtualVertexArrayHeap", MemoryUtil.memAddress(memAllocLong(1)));
        deviceObj.handleMap.put$(this.handle, this);

        // device memory buffer with older GPU (Turing, etc.) or device memory with `map` and staging ops support.
        this.hostPayload = memAlloc((int) (cInfo.maxVertexArrayCount * vertexArrayStride));
        this.bufferHeap = new BufferObj(this.base, new BufferCInfo() {{
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
    public long getBufferAddress() {
        return this.bufferHeap.getDeviceAddress();
    }

    //
    public VkDescriptorBufferInfo getBufferRange() {
        return VkDescriptorBufferInfo.calloc().set(this.bufferHeap.getHandle().get(), 0, ((BufferCInfo)this.bufferHeap.cInfo).size);
    }

    //
    public VirtualVertexArrayHeap writeVertexArrays() {
        this.registry.stream().forEach((obj)->{
            var VAO = (VirtualVertexArrayObj)obj;
            if (VAO != null) { VAO.writeData(); };
        });
        return this;
    }

    // you need to do it manually!
    //public VirtualVertexArrayHeap cmdSynchronizeFromHost(VkCommandBuffer cmdBuf) {
        //this.bufferHeap.cmdSynchronizeFromHost(cmdBuf);
        //return this;
    //}

    //
    public VirtualVertexArrayHeap cmdClear(VkCommandBuffer cmdBuf) {
        return this;
    }

    // also, after draw, vertex and/or index buffer data can/may be changed.
    // for BLAS-based recommended to use a fixed data.
    public static class VirtualVertexArrayObj extends VirtualGLObj {
        // Will be used in draw collection
        //public long BLASHandle = 0L;
        //public long BLASAddress = 0L;
        public HashMap<Integer, VertexBinding> bindings = null;
        public ByteBuffer bindingsMapped = null;

        //
        protected long bufferOffset = 0L;
        protected long address = 0L;

        //
        public VirtualVertexArrayObj(Handle base, Handle handle) {
            super(base, handle);
        }
        public VirtualVertexArrayObj(Handle base, VirtualVertexArrayHeapCInfo.VirtualVertexArrayCInfo cInfo) {
            super(base, cInfo);

            //
            var virtualVertexArrayHeap = (VirtualVertexArrayHeap)deviceObj.handleMap.get(new Handle("VirtualVertexArrayHeap", cInfo.registryHandle)).orElse(null);

            //
            this.bound = virtualVertexArrayHeap;

            //
            assert this.bound != null;
            this.DSC_ID = this.bound.registry.push(this);
            this.virtualGL = this.DSC_ID+1;

            //
            this.bindings = new HashMap<Integer, VirtualVertexArrayHeapCInfo.VertexBinding>(maxBindings);
            this.bindingsMapped = memSlice(virtualVertexArrayHeap.hostPayload, (int) (this.bufferOffset = this.DSC_ID*vertexArrayStride), vertexArrayStride);//virtualVertexArrayHeap.bufferHeap.map(vertexArrayStride, this.bufferOffset = this.DSC_ID*vertexArrayStride);
            this.address = virtualVertexArrayHeap.bufferHeap.getDeviceAddress() + this.bufferOffset;
            memSet(this.bindingsMapped, 0);
        }

        //
        public long getBufferAddress() {
            if (this.address == 0L) {
                System.out.println("VAO Error: Bad Buffer Address!");
                throw new RuntimeException("VAO Error: Bad Buffer Address!");
            };
            return this.address;
        }

        //
        public VkDescriptorBufferInfo getBufferRange() {
            var heap = ((VirtualVertexArrayHeap)this.bound).bufferHeap;
            return VkDescriptorBufferInfo.calloc().set(heap.getHandle().get(), this.bufferOffset, vertexArrayStride);
        }

        //
        public VirtualVertexArrayObj writeData() {
            memSet(this.bindingsMapped, 0);
            var bindingOffset = 0L;
            var keySet = bindings.keySet().stream().toList();
            IntStream.range(0, bindings.size()).forEach((I)->{
                var index = keySet.get(I);
                var binding = bindings.get(index);
                if (index == 0 && (binding == null || binding.format == 0)) {
                    System.out.println("VAO Error: Bad First Binding");
                    throw new RuntimeException("VAO Error: Bad First Binding");
                }
                if (binding != null) {
                    binding.writeData(bindingsMapped,bindingOffset + vertexBindingStride * index);
                }
            });
            return this;
        }

        // for such games, as Minecraft with VulkanMod or Vanilla
        // TODO: multiple buffer bindings support
        public VirtualVertexArrayObj vertexBufferForAll(long bufferAddress, long bufferSize) {
            if (bufferAddress == 0L || bufferSize == 0L) {
                System.out.println("VAO Error: Zero Device Buffer Address Or Size");
                throw new RuntimeException("VAO Error: Zero Device Buffer Address Or Size");
            };

            var keySet = bindings.keySet().stream().toList();
            IntStream.range(0, bindings.size()).forEach((I)->{
                var index = keySet.get(I);
                var binding = bindings.get(index);
                if (index == 0 && (binding == null || binding.format == 0)) {
                    System.out.println("VAO Error: Bad First Binding");
                    throw new RuntimeException("VAO Error: Bad First Binding");
                }
                if (binding != null && binding.format != 0) {
                    binding.bufferAddress = bufferAddress;
                    binding.bufferSize = bufferSize;
                }
            });
            return this;
        }

        // oh my jesus...
        public VirtualVertexArrayObj clear() {
            this.bindings.clear();
            //for (int i=0;i<8;i++) {
                //this.bindings.put(i, new VirtualVertexArrayHeapCInfo.VertexBinding());
            //}
            return this;
        }

        //
        public VirtualVertexArrayObj vertexBinding(int index, VirtualVertexArrayHeapCInfo.VertexBinding binding) {
            if (index >= 0 && index < maxBindings) {
                bindings.put(index + binding.location, binding);
            }
            return this;
        }

        // de-bloat a re-production of VAO
        public VirtualVertexArrayObj deleteDirectly() {
            this.address = 0L;
            this.bound.registry.removeIndex(this.DSC_ID);
            this.address = 0L;
            this.bindingsMapped = null;
            this.bindings = null;
            return this;
        }
    }

}
