package org.hydra2s.noire.virtual;

//

import org.hydra2s.noire.descriptors.BufferCInfo;
import org.hydra2s.noire.descriptors.MemoryAllocationCInfo;
import org.hydra2s.noire.objects.BufferObj;
import org.hydra2s.noire.objects.CommandUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vma.VmaVirtualAllocationCreateInfo;
import org.lwjgl.util.vma.VmaVirtualBlockCreateInfo;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;

import static java.lang.Math.abs;
import static org.hydra2s.noire.descriptors.UtilsCInfo.vkCheckStatus;
import static org.lwjgl.system.MemoryUtil.memAllocLong;
import static org.lwjgl.system.MemoryUtil.memAllocPointer;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.EXTDescriptorBuffer.VK_BUFFER_USAGE_RESOURCE_DESCRIPTOR_BUFFER_BIT_EXT;
import static org.lwjgl.vulkan.KHRAccelerationStructure.VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR;
import static org.lwjgl.vulkan.VK10.*;

// Will uses large buffer concept with virtual allocation (VMA)
// When used draw collector system, recommended to use host-based memory
// TODO: needs more correctly reusing same buffer-data (by morton codes)
public class VirtualMutableBufferHeap extends VirtualGLRegistry {

    // TODO: make as an OBJ
    public static class VirtualMemoryHeap extends VirtualGLObj {
        public VirtualMutableBufferHeap bound = null;
        public PointerBuffer virtualBlock = memAllocPointer(1).put(0, 0L);
        public VmaVirtualBlockCreateInfo vbInfo = VmaVirtualBlockCreateInfo.calloc().flags(VMA_VIRTUAL_ALLOCATION_CREATE_STRATEGY_MIN_OFFSET_BIT | VMA_VIRTUAL_ALLOCATION_CREATE_STRATEGY_MIN_TIME_BIT | VMA_VIRTUAL_ALLOCATION_CREATE_STRATEGY_MIN_MEMORY_BIT);
        public BufferObj bufferHeap = null;

        //
        public ArrayList<Long> toFree = null;

        //
        public VirtualMemoryHeap(Handle base, VirtualMutableBufferHeapCInfo.VirtualMemoryHeapCInfo cInfo, long $memoryAllocator) {
            super(base, cInfo);

            // TODO: add support for ResizableBAR! It's necessary!
            this.toFree = new ArrayList<>();
            this.bufferHeap = new BufferObj(base, new BufferCInfo() {{
                size = cInfo.bufferHeapSize;
                usage = VK_BUFFER_USAGE_RESOURCE_DESCRIPTOR_BUFFER_BIT_EXT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT | VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR;
                memoryAllocator = $memoryAllocator;
                memoryAllocationInfo = new MemoryAllocationCInfo() {{
                    isHost = cInfo.isHost;
                    isDevice = !cInfo.isHost;
                }};
            }});

            //
            vkCheckStatus(vmaCreateVirtualBlock(vbInfo.size(cInfo.bufferHeapSize), this.virtualBlock = memAllocPointer(1).put(0, 0L)));
        }

        //
        public VirtualMemoryHeap garbage() {
            this.toFree.forEach((alloc)->{
                vmaVirtualFree(virtualBlock.get(0), alloc);
            });
            this.toFree.clear();
            return this;
        }

        //
        public VirtualMemoryHeap clear() {
            vmaClearVirtualBlock(this.virtualBlock.get(0));
            return this;
        }

        //
        public long getAddress() {
            return this.bufferHeap.getDeviceAddress();
        }

        //
        public VkDescriptorBufferInfo getBufferRange() {
            return VkDescriptorBufferInfo.calloc().set(this.bufferHeap.getHandle().get(), 0, ((BufferCInfo)this.bufferHeap.cInfo).size);
        }
    }

    //
    public ArrayList<VirtualMemoryHeap> memoryHeaps = null;

    //
    public VirtualMutableBufferHeap(Handle base, Handle handle) {
        super(base, handle);
    }

    // But before needs to create such system
    public VirtualMutableBufferHeap(Handle base, VirtualMutableBufferHeapCInfo cInfo) {
        super(base, cInfo);

        //
        this.handle = new Handle("VirtualMutableBufferHeap", MemoryUtil.memAddress(memAllocLong(1)));
        deviceObj.handleMap.put$(this.handle, this);

        // TODO: multiple heaps, one registry
        // TODO: morton coding support
        this.memoryHeaps = new ArrayList<VirtualMemoryHeap>();
        var Hs = cInfo.heapCInfo.size();
        for (var I=0;I<Hs;I++) {
            memoryHeaps.add(new VirtualMemoryHeap(this.base, cInfo.heapCInfo.get(I), cInfo.memoryAllocator));
        }
    }

    //
    public VirtualMutableBufferObj createBuffer(int $heapId) {
        return new VirtualMutableBufferObj(this.base, new VirtualMutableBufferHeapCInfo.VirtualMutableBufferCInfo(){{
            heapId = $heapId;
            registryHandle = handle.get();
        }});
    }

    //
    public VirtualMutableBufferObj createBuffer(int $heapId, long size) throws Exception {
        return new VirtualMutableBufferObj(this.base, new VirtualMutableBufferHeapCInfo.VirtualMutableBufferCInfo(){{
            heapId = $heapId;
            registryHandle = handle.get();
        }}).allocate(size);
    }

    @Override
    public VirtualMutableBufferHeap clear() {
        super.clear();
        var cInfo = (VirtualMutableBufferHeapCInfo)this.cInfo;
        var Hs = memoryHeaps.size();
        for (var I=0;I<Hs;I++) {
            memoryHeaps.get(I).clear();
        }
        return this;
    }

    // Will be able to deallocate and re-allocate again
    // TODO: support for TRIM and trimming...
    public static class VirtualMutableBufferObj extends VirtualGLObj {
        protected VirtualMemoryHeap heap = null;
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
        public long getBufferSize() {
            return bufferSize;
        }

        //
        public VirtualMutableBufferObj(Handle base, VirtualMutableBufferHeapCInfo.VirtualMutableBufferCInfo cInfo) {
            super(base, cInfo);

            //
            this.allocCreateInfo = VmaVirtualAllocationCreateInfo.calloc().alignment(16L);
            this.allocId = memAllocPointer(1).put(0, 0L);
            this.bufferOffset = memAllocLong(1).put(0, 0L);
            this.bufferSize = 0;
            this.blockSize = 0;
            this.address = 0;

            // TODO: bound with memoryHeap
            var virtualBufferRegistry = (VirtualMutableBufferHeap)deviceObj.handleMap.get(new Handle("VirtualMutableBufferHeap", cInfo.registryHandle)).orElse(null);

            //
            this.bound = virtualBufferRegistry;
            this.heap = ((VirtualMutableBufferHeap)this.bound).memoryHeaps.get(cInfo.heapId);

            //
            this.DSC_ID = this.bound.registry.push(this);
            this.virtualGL = this.DSC_ID+1;
        }

        //
        public long getBufferAddress() {
            if (this.address == 0) {
                System.out.println("Getting Bad Device Address Of Virtual Mutable Buffer!");
                throw new RuntimeException("Getting Bad Device Address Of Virtual Mutable Buffer!");
            };
            return this.address;
        }

        //
        public VkDescriptorBufferInfo getBufferRange() {
            if (this.bufferSize <= 0L || this.blockSize <= 0L) {
                System.out.println("Bad Buffer Size of Virtual Mutable Buffer!");
                throw new RuntimeException("Bad Buffer Size of Virtual Mutable Buffer!");
            };
            return VkDescriptorBufferInfo.calloc().set(this.heap.getBufferRange().buffer(), this.bufferOffset.get(0), this.bufferSize);
        }

        //
        public static long roundUp(long num, long divisor) {
            int sign = (num > 0 ? 1 : -1) * (divisor > 0 ? 1 : -1);
            return sign * (abs(num) + abs(divisor) - 1) / abs(divisor);
        }

        //
        public ByteBuffer map() {
            if (this.mapped != null) { return this.mapped; };
            return (this.mapped = this.heap.bufferHeap.map(this.bufferSize, this.bufferOffset.get(0)));
        }

        //
        public void unmap(int target) {
            this.mapped = null;
        }

        // TODO: morton coding support
        // TODO: support for memory type when allocation, not when create
        public VirtualMutableBufferObj allocate(long bufferSize, VkCommandBuffer cmdBuf) throws Exception {
            final long MEM_BLOCK = 64L;

            //
            VkDescriptorBufferInfo srcBufferRange = null;
            var oldAlloc = this.allocId.get(0);
            if (oldAlloc != 0L) {
                srcBufferRange = this.getBufferRange();
            }

            //
            if (bufferSize <= 0L) {
                System.out.println("Allocation Failed, zero or less sized, not supported...");
                throw new Exception("Allocation Failed, zero or less sized, not supported...");
            }

            //
            this.bufferSize = bufferSize; bufferSize = roundUp(bufferSize, MEM_BLOCK) * MEM_BLOCK;

            //
            if (bufferSize <= 0L) {
                System.out.println("Allocation Failed, zero or less sized, not supported...");
                throw new Exception("Allocation Failed, zero or less sized, not supported...");
            }

            //
            if (bufferSize == 0 || this.blockSize < bufferSize || abs(bufferSize - this.blockSize) > (MEM_BLOCK * 96L))
            {
                //  add to black list
                if (oldAlloc != 0L) {
                    heap.toFree.add(oldAlloc);
                }

                //
                boolean earlyMapped = this.mapped != null;
                this.mapped = null;
                this.bufferOffset.put(0, 0L);
                this.allocId.put(0, 0L);

                //
                int res = vkCheckStatus(vmaVirtualAllocate(this.heap.virtualBlock.get(0), this.allocCreateInfo.size(this.blockSize = bufferSize), this.allocId, this.bufferOffset));

                // if anyways, isn't allocated...
                if (res != VK_SUCCESS) {
                    this.bufferSize = 0L;
                    this.address = 0L;

                    //
                    System.out.println("Allocation Failed, there is not free memory: " + res);
                    throw new Exception("Allocation Failed, there is not free memory: " + res);
                } else {
                    // get device address from
                    this.address = this.heap.bufferHeap.getDeviceAddress() + this.bufferOffset.get(0);
                    if (earlyMapped) {
                        this.mapped = this.heap.bufferHeap.map(this.blockSize, this.bufferOffset.get(0));
                    }

                    //
                    var dstBufferRange = this.getBufferRange();
                    if (oldAlloc != 0 && cmdBuf != null && srcBufferRange.range() > 0 && dstBufferRange.range() > 0) {
                        CommandUtils.cmdCopyVBufferToVBuffer(cmdBuf, srcBufferRange, dstBufferRange);
                    }
                }
            }
            return this;
        }

        public VirtualMutableBufferObj allocate(long bufferSize) throws Exception {
            return this.allocate(bufferSize, null);
        }

        /*@Override
        public VirtualMutableBufferObj delete() throws Exception {
            var oldAlloc = this.allocId.get(0);
            if (oldAlloc != 0L) {
                this.heap.toFree.add(oldAlloc);
            }
            this.bufferSize = 0L;
            this.blockSize = 0L;
            this.address = 0L;
            this.bufferOffset.put(0, 0L);
            this.bufferOffset = null;
            this.allocId.put(0, 0L);
            this.allocId = null;
            this.mapped = null;
            this.bound.registry.removeIndex(this.DSC_ID);
            this.heap = null;
            this.bound = null;
            return this;
        }*/

        @Override
        public VirtualMutableBufferObj deleteDirectly() /*throws Exception*/ {
            var oldAlloc = this.allocId.get(0);
            if (oldAlloc != 0L) {
                this.heap.toFree.add(oldAlloc);
            }
            this.bufferSize = 0L;
            this.blockSize = 0L;
            this.address = 0L;
            this.bufferOffset.put(0, 0L);
            this.bufferOffset = null;
            this.allocId.put(0, 0L);
            this.allocId = null;
            this.mapped = null;
            this.bound.registry.removeIndex(this.DSC_ID);
            this.heap = null;
            this.bound = null;
            return this;
        }
    }

}
