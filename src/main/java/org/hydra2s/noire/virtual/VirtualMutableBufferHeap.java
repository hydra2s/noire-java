package org.hydra2s.noire.virtual;

//
import org.hydra2s.noire.descriptors.BasicCInfo;
import org.hydra2s.noire.descriptors.BufferCInfo;
import org.hydra2s.noire.descriptors.MemoryAllocationCInfo;
import org.hydra2s.noire.descriptors.SwapChainCInfo;
import org.hydra2s.noire.objects.*;
import org.hydra2s.utils.Promise;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vma.VmaVirtualAllocationCreateInfo;
import org.lwjgl.util.vma.VmaVirtualBlockCreateInfo;
import org.lwjgl.vulkan.VkBufferCopy2;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;

import static java.lang.Math.abs;
import static java.lang.Math.min;
import static org.lwjgl.system.MemoryUtil.memAllocLong;
import static org.lwjgl.system.MemoryUtil.memAllocPointer;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.KHRAccelerationStructure.VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK13.VK_STRUCTURE_TYPE_BUFFER_COPY_2;

// Will uses large buffer concept with virtual allocation (VMA)
// When used draw collector system, recommended to use host-based memory
// TODO: needs more correctly reusing same buffer-data (by morton codes)
public class VirtualMutableBufferHeap extends VirtualGLRegistry {

    // TODO: make as an OBJ
    public static class VirtualMemoryHeap {
        public VirtualMutableBufferHeap bound = null;
        public PointerBuffer virtualBlock = memAllocPointer(1).put(0, 0L);
        public VmaVirtualBlockCreateInfo vbInfo = VmaVirtualBlockCreateInfo.calloc().flags(VMA_VIRTUAL_ALLOCATION_CREATE_STRATEGY_MIN_OFFSET_BIT | VMA_VIRTUAL_ALLOCATION_CREATE_STRATEGY_MIN_TIME_BIT | VMA_VIRTUAL_ALLOCATION_CREATE_STRATEGY_MIN_MEMORY_BIT);
        public BufferObj bufferHeap = null;

        //
        public VirtualMemoryHeap(Handle base, VirtualMutableBufferHeapCInfo.VirtualMemoryHeapCInfo cInfo, long $memoryAllocator) {
            // TODO: add support for ResizableBAR! It's necessary!
            this.bufferHeap = new BufferObj(base, new BufferCInfo() {{
                size = cInfo.bufferHeapSize;
                usage = VK_BUFFER_USAGE_VERTEX_BUFFER_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT | VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR;
                memoryAllocator = $memoryAllocator;
                memoryAllocationInfo = new MemoryAllocationCInfo() {{
                    isHost = cInfo.isHost;
                    isDevice = !cInfo.isHost;
                }};
            }});

            //
            vmaCreateVirtualBlock(vbInfo.size(cInfo.bufferHeapSize), this.virtualBlock = memAllocPointer(1).put(0, 0L));
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
        var memoryAllocatorObj = (MemoryAllocatorObj)BasicObj.globalHandleMap.get(cInfo.memoryAllocator);
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(memoryAllocatorObj.getBase().get());
        var physicalDeviceObj = (PhysicalDeviceObj)BasicObj.globalHandleMap.get(deviceObj.getBase().get());

        //
        this.handle = new Handle("VirtualMutableBufferHeap", MemoryUtil.memAddress(memAllocLong(1)));
        deviceObj.handleMap.put(this.handle, this);

        // TODO: multiple heaps, one registry
        // TODO: morton coding support
        this.memoryHeaps = new ArrayList<VirtualMemoryHeap>();
        for (var I=0;I<cInfo.heapCInfo.size();I++) {
            memoryHeaps.add(new VirtualMemoryHeap(this.base, cInfo.heapCInfo.get(I), cInfo.memoryAllocator));
        }
    }

    //
    public VirtualMutableBufferObj createBuffer(int heapId_, int $queueFamilyIndex) {
        return new VirtualMutableBufferObj(this.base, new VirtualMutableBufferHeapCInfo.VirtualMutableBufferCInfo(){{
            heapId = heapId_;
            registryHandle = handle.get();
            queueFamilyIndex = $queueFamilyIndex;
        }});
    }

    //
    public VirtualMutableBufferObj createBuffer(int heapId_, long size, int $queueFamilyIndex) throws Exception {
        return new VirtualMutableBufferObj(this.base, new VirtualMutableBufferHeapCInfo.VirtualMutableBufferCInfo(){{
            heapId = heapId_;
            registryHandle = handle.get();
            queueFamilyIndex = $queueFamilyIndex;
        }}).allocate(size);
    }

    @Override
    public VirtualMutableBufferHeap clear() {
        super.clear();
        var cInfo = (VirtualMutableBufferHeapCInfo)this.cInfo;
        for (var I=0;I<memoryHeaps.size();I++) {
            memoryHeaps.get(I).clear();
        }
        return this;
    }

    // virtual buffer copying version
    public static void cmdCopyVBufferToVBuffer(VkCommandBuffer cmdBuf, VkDescriptorBufferInfo src, VkDescriptorBufferInfo dst, VkBufferCopy2.Buffer copies) {
        // TODO: fix VK_WHOLE_SIZE issues!
        var modified = copies.stream().map((cp)-> cp
            .dstOffset(cp.dstOffset()+dst.offset())
            .srcOffset(cp.srcOffset()+src.offset())
            .size(min(src.range(), dst.range()))).toList();
        for (var I=0;I<copies.remaining();I++) { copies.get(I).set(modified.get(I)); } // modify copies
        CopyUtilObj.cmdCopyBufferToBuffer(cmdBuf, src.buffer(), dst.buffer(), copies);
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
            var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(base.get());
            var physicalDeviceObj = (PhysicalDeviceObj)BasicObj.globalHandleMap.get(deviceObj.getBase().get());

            //
            this.allocCreateInfo = VmaVirtualAllocationCreateInfo.calloc().alignment(16L);
            this.allocId = memAllocPointer(1).put(0, 0L);
            this.bufferOffset = memAllocLong(1).put(0, 0L);
            this.bufferSize = 0;
            this.blockSize = 0;

            // TODO: bound with memoryHeap
            var virtualBufferRegistry = (VirtualMutableBufferHeap)deviceObj.handleMap.get(new Handle("VirtualMutableBufferHeap", cInfo.registryHandle));

            //
            this.bound = virtualBufferRegistry;
            this.heap = ((VirtualMutableBufferHeap)this.bound).memoryHeaps.get(cInfo.heapId);

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
        public VirtualMutableBufferObj allocate(long bufferSize) throws Exception {
            final long MEM_BLOCK = 512L;
            bufferSize = roundUp(bufferSize, MEM_BLOCK) * MEM_BLOCK;
            this.bufferSize = bufferSize;
            var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get());

            //
            if (this.blockSize < bufferSize)
            {
                // TODO: copy from old segment
                var oldAlloc = this.allocId.get(0);
                var srcBufferRange = this.getBufferRange();

                //
                this.bufferOffset.put(0, 0L);
                int res = vmaVirtualAllocate(this.heap.virtualBlock.get(0), this.allocCreateInfo.size(this.blockSize = bufferSize), this.allocId.put(0, 0L), this.bufferOffset);
                if (res != VK_SUCCESS) {
                    System.out.println("Allocation Failed: " + res);
                    throw new Exception("Allocation Failed: " + res);
                }

                // get device address from
                this.address = this.heap.bufferHeap.getDeviceAddress() + this.bufferOffset.get(0);
                if (this.mapped != null) {
                    this.mapped = this.heap.bufferHeap.map(this.bufferSize, this.bufferOffset.get(0));
                }

                // pistol (copy from old, and remove such segment)
                if (oldAlloc != 0) {
                    var dstBufferRange = this.getBufferRange(); // TODO: correctly using transfer queue!
                    deviceObj.submitOnce(deviceObj.getCommandPool(cInfo.queueFamilyIndex), new BasicCInfo.SubmitCmd() {{
                        // TODO: correctly handle main queue family
                        whatQueueFamilyWillWait = cInfo.queueFamilyIndex != 0 ? 0 : -1;
                        whatWaitBySemaphore = VK_PIPELINE_STAGE_TRANSFER_BIT;

                        //
                        queueFamilyIndex = cInfo.queueFamilyIndex;
                        queue = deviceObj.getQueue(cInfo.queueFamilyIndex, 0);
                        onDone = new Promise<>().thenApply((result) -> {
                            if (bound != null && oldAlloc != 0) {
                                vmaVirtualFree(heap.virtualBlock.get(0), oldAlloc);
                            }
                            return null;
                        });
                    }}, (cmdBuf) -> {
                        VirtualMutableBufferHeap.cmdCopyVBufferToVBuffer(cmdBuf, srcBufferRange, dstBufferRange, VkBufferCopy2.calloc(1)
                            .sType(VK_STRUCTURE_TYPE_BUFFER_COPY_2)
                            .dstOffset(0)
                            .srcOffset(0)
                            .size(min(srcBufferRange.range(), dstBufferRange.range())));
                        return VK_SUCCESS;
                    });
                }
            }
            return this;
        }

        //
        public VirtualMutableBufferObj deallocate() throws Exception {
            if (this.allocId.get(0) != 0) {
                var oldAlloc = this.allocId.get(0);
                if (this.bound != null && oldAlloc != 0) {
                    // TODO: bound with memoryHeap
                    var cInfo = (VirtualMutableBufferHeapCInfo.VirtualMutableBufferCInfo)this.cInfo;
                    vmaVirtualFree(((VirtualMutableBufferHeap)this.bound).memoryHeaps.get(cInfo.heapId).virtualBlock.get(0), oldAlloc);
                }

                //
                this.bufferSize = 0L;
                this.blockSize = 0L;
                this.address = 0L;
                this.bufferOffset.put(0, 0L);
                this.allocId.put(0, 0L);
                this.mapped = null;
            }
            return this;
        }

        @Override
        public VirtualMutableBufferObj delete() throws Exception {
            var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get());
            deviceObj.submitOnce(deviceObj.getCommandPool(cInfo.queueFamilyIndex), new BasicCInfo.SubmitCmd(){{
                queueFamilyIndex = cInfo.queueFamilyIndex;
                queue = deviceObj.getQueue(cInfo.queueFamilyIndex, 0);
                onDone = new Promise<>().thenApply((result)-> {
                    try {
                        deallocate();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    bound.registry.removeIndex(DSC_ID);
                    return null;
                });
            }}, (cmdBuf)->{
                return VK_SUCCESS;
            });
            return this;
        }

        @Override
        public VirtualMutableBufferObj deleteDirectly() throws Exception {
            this.deallocate();
            this.bound.registry.removeIndex(this.DSC_ID);
            return this;
        }
    }

}
