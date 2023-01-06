package org.hydra2s.noire.virtual;

//

import org.hydra2s.noire.descriptors.BufferCInfo;
import org.hydra2s.noire.descriptors.MemoryAllocationCInfo;
import org.hydra2s.noire.objects.*;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vma.VmaVirtualAllocationCreateInfo;
import org.lwjgl.util.vma.VmaVirtualBlockCreateInfo;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;

import static java.lang.Math.abs;
import static org.lwjgl.system.MemoryUtil.memAllocLong;
import static org.lwjgl.system.MemoryUtil.memAllocPointer;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.KHRAccelerationStructure.VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR;
import static org.lwjgl.vulkan.VK10.*;

// Will uses large buffer concept with virtual allocation (VMA)
// When used draw collector system, recommended to use host-based memory
// TODO: add support for multiple buffer heaps into one virtual GL registry
public class VirtualMutableBufferHeap extends VirtualGLRegistry {

    //
    protected PointerBuffer virtualBlock = memAllocPointer(1).put(0, 0L);
    protected VmaVirtualBlockCreateInfo vbInfo = VmaVirtualBlockCreateInfo.calloc().flags(VMA_VIRTUAL_ALLOCATION_CREATE_STRATEGY_MIN_OFFSET_BIT | VMA_VIRTUAL_ALLOCATION_CREATE_STRATEGY_MIN_TIME_BIT | VMA_VIRTUAL_ALLOCATION_CREATE_STRATEGY_MIN_MEMORY_BIT);
    protected BufferObj bufferHeap = null;

    //
    public VirtualMutableBufferHeap(Handle base, Handle handle) {
        super(base, handle);
    }

    // But before needs to create such system
    public VirtualMutableBufferHeap(Handle base, VirtualMutableBufferHeapCInfo cInfo) {
        super(base, cInfo);

        //
        var memoryAllocatorObj = (MemoryAllocatorObj)BasicObj.globalHandleMap.get(this.base.get());
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(memoryAllocatorObj.getBase().get());
        var physicalDeviceObj = (PhysicalDeviceObj)BasicObj.globalHandleMap.get(deviceObj.getBase().get());

        //
        this.handle = new Handle("VirtualMutableBufferHeap", MemoryUtil.memAddress(memAllocLong(1)));
        deviceObj.handleMap.put(this.handle, this);

        //
        this.bufferHeap = new BufferObj(this.base, new BufferCInfo() {{
            size = cInfo.bufferHeapSize;
            usage = VK_BUFFER_USAGE_VERTEX_BUFFER_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT | VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR;
            memoryAllocator = cInfo.memoryAllocator;
            memoryAllocationInfo = new MemoryAllocationCInfo(){{
                isHost = cInfo.isHost;
                isDevice = !cInfo.isHost;
            }};
        }});

        //
        vmaCreateVirtualBlock(vbInfo.size(cInfo.bufferHeapSize), this.virtualBlock = memAllocPointer(1).put(0, 0L));
    }

    public long getBufferAddress() {
        return this.bufferHeap.getDeviceAddress();
    }

    public VkDescriptorBufferInfo getBufferRange() {
        return VkDescriptorBufferInfo.calloc().set(this.bufferHeap.getHandle().get(), 0, ((BufferCInfo)this.bufferHeap.cInfo).size);
    }

    // Will be able to deallocate and re-allocate again
    // TODO: add sub-buffer copying support (for command buffers)
    // TODO: getter for buffer ranges (handle-based)
    public static class VirtualMutableBufferObj extends VirtualGLObj {
        protected PointerBuffer allocId = memAllocPointer(1).put(0, 0L);
        protected LongBuffer bufferOffset = memAllocLong(1).put(0, 0L);
        protected long bufferSize = 0L;
        protected long blockSize = 0L;
        protected long address = 0L;
        protected ByteBuffer mapped = null;
        protected VmaVirtualAllocationCreateInfo allocCreateInfo = null;

        //
        public VirtualMutableBufferObj(Handle base, Handle handle) {
            super(base, handle);
        }

        //
        public VirtualMutableBufferObj(Handle base, VirtualMutableBufferHeapCInfo.VirtualMutableBufferCInfo cInfo) {
            super(base, cInfo);

            //
            var memoryAllocatorObj = (MemoryAllocatorObj)BasicObj.globalHandleMap.get(this.base.get());
            var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(memoryAllocatorObj.getBase().get());
            var physicalDeviceObj = (PhysicalDeviceObj)BasicObj.globalHandleMap.get(deviceObj.getBase().get());

            //
            this.allocCreateInfo = VmaVirtualAllocationCreateInfo.calloc().alignment(16L);
            this.allocId = memAllocPointer(1).put(0, 0L);
            this.bufferOffset = memAllocLong(1).put(0, 0L);
            this.bufferSize = 0;
            this.blockSize = 0;

            //
            var virtualBufferHeap = (VirtualMutableBufferHeap)deviceObj.handleMap.get(new Handle("VirtualMutableBufferHeap", cInfo.registryHandle));

            //
            this.bound = virtualBufferHeap;

            //
            this.DSC_ID = this.bound.registry.push(this);
            this.virtualGL = this.DSC_ID+1;
        }

        //
        public long getBufferAddress() {
            return this.address;
        }

        //
        public VkDescriptorBufferInfo getBufferRange() {
            var heap = ((VirtualMutableBufferHeap)this.bound).bufferHeap;
            return VkDescriptorBufferInfo.calloc().set(heap.getHandle().get(), this.bufferOffset.get(0), this.bufferSize);
        }

        //
        public static long roundUp(long num, long divisor) {
            int sign = (num > 0 ? 1 : -1) * (divisor > 0 ? 1 : -1);
            return sign * (abs(num) + abs(divisor) - 1) / abs(divisor);
        }

        //
        public ByteBuffer map() {
            if (this.mapped != null) { return this.mapped; };
            return (this.mapped = ((VirtualMutableBufferHeap)this.bound).bufferHeap.map(this.bufferSize, this.bufferOffset.get(0)));
        }

        //
        public void unmap(int target) {
            ((VirtualMutableBufferHeap)this.bound).bufferHeap.unmap();
            this.mapped = null;
        }

        //
        public VirtualMutableBufferObj allocate(long bufferSize) throws Exception {
            final long MEM_BLOCK = 1024L * 3L;
            bufferSize = roundUp(bufferSize, MEM_BLOCK) * MEM_BLOCK;
            this.bufferSize = bufferSize;
            if (this.blockSize < bufferSize)
            {
                var virtualBufferHeap = ((VirtualMutableBufferHeap)this.bound);
                var oldAlloc = this.allocId.get(0);
                this.bufferOffset.put(0, 0L);
                int res = vmaVirtualAllocate(virtualBufferHeap.virtualBlock.get(0), this.allocCreateInfo.size(this.blockSize = bufferSize), this.allocId.put(0, 0L), this.bufferOffset);
                if (res != VK_SUCCESS) {
                    System.out.println("Allocation Failed: " + res);
                    throw new Exception("Allocation Failed: " + res);
                }

                // get device address from
                this.address = virtualBufferHeap.bufferHeap.getDeviceAddress() + this.bufferOffset.get(0);

                // TODO: copy from old segment
                // Avoid some data corruption
                if (this.bound != null && oldAlloc != 0) {
                    vmaVirtualFree(virtualBufferHeap.virtualBlock.get(0), oldAlloc);
                }
            }
            return this;
        }

        //
        public VirtualMutableBufferObj deallocate() throws Exception {
            if (this.allocId.get(0) != 0) {
                var oldAlloc = this.allocId.get(0);
                if (this.bound != null && oldAlloc != 0) {
                    vmaVirtualFree(((VirtualMutableBufferHeap)this.bound).virtualBlock.get(0), oldAlloc);
                }

                //
                this.bufferSize = 0L;
                this.blockSize = 0L;
                this.address = 0L;
                this.bufferOffset.put(0, 0L);
                this.allocId.put(0, 0L);
            }
            return this;
        }
    }

}
