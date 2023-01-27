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
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.function.Function;

import static java.lang.Math.abs;
import static java.lang.Math.min;
import static org.lwjgl.system.MemoryUtil.memAllocLong;
import static org.lwjgl.system.MemoryUtil.memAllocPointer;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.KHRAccelerationStructure.VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK13.*;

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
        public VirtualMemoryHeap(Handle base, VirtualMutableBufferHeapCInfo.VirtualMemoryHeapCInfo cInfo, long $memoryAllocator) {
            super(base, cInfo);

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
        this.handle = new Handle("VirtualMutableBufferHeap", MemoryUtil.memAddress(memAllocLong(1)));
        deviceObj.handleMap.put$(this.handle, this);

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
            this.allocCreateInfo = VmaVirtualAllocationCreateInfo.calloc().alignment(16L);
            this.allocId = memAllocPointer(1).put(0, 0L);
            this.bufferOffset = memAllocLong(1).put(0, 0L);
            this.bufferSize = 0;
            this.blockSize = 0;

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
            this.bufferSize = bufferSize; bufferSize = roundUp(bufferSize, MEM_BLOCK) * MEM_BLOCK;

            //
            if (bufferSize == 0 || this.blockSize < bufferSize || abs(bufferSize - this.blockSize) > (MEM_BLOCK * 96L))
            {
                // TODO: copy from old segment
                var oldAlloc = this.allocId.get(0);
                var srcBufferRange = this.getBufferRange();

                //
                boolean earlyMapped = this.mapped != null;
                this.mapped = null;
                this.bufferOffset.put(0, 0L);
                this.allocId.put(0, 0L);

                //
                long finalBufferSize = bufferSize;
                Callable<Integer> memAlloc = ()->{
                    if (this.allocId.get(0) == 0L) {
                        return vmaVirtualAllocate(this.heap.virtualBlock.get(0), this.allocCreateInfo.size(this.blockSize = finalBufferSize), this.allocId, this.bufferOffset);
                    }
                    return VK_SUCCESS;
                };

                // initiate free procedure
                if (oldAlloc != 0) {
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
                        /*
                        if (res != VK_NOT_READY) {
                            var dstBufferRange = this.getBufferRange(); // TODO: correctly using transfer queue!
                            VirtualMutableBufferHeap.cmdCopyVBufferToVBuffer(cmdBuf, srcBufferRange, dstBufferRange, VkBufferCopy2.calloc(1)
                                .sType(VK_STRUCTURE_TYPE_BUFFER_COPY_2)
                                .dstOffset(0)
                                .srcOffset(0)
                                .size(min(srcBufferRange.range(), dstBufferRange.range())));
                        }*/

                        // polyfill buffer
                        /*vkCmdFillBuffer(cmdBuf, srcBufferRange.buffer(), srcBufferRange.offset(), srcBufferRange.range(), 0);
                        vkCmdPipelineBarrier2(cmdBuf, VkDependencyInfoKHR.calloc().sType(VK_STRUCTURE_TYPE_DEPENDENCY_INFO).pBufferMemoryBarriers(VkBufferMemoryBarrier2.calloc(1)
                            .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER_2)
                            .srcStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
                            .srcAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
                            .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
                            .dstAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
                            .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                            .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                            .buffer(srcBufferRange.buffer())
                            .offset(srcBufferRange.offset())
                            .size(min(srcBufferRange.range(), dstBufferRange.range()))
                        ));*/

                        return VK_SUCCESS;
                    });
                }

                // wait when virtual memory will free...
                // WARNING! Your game may LAG! But it's needs for await memory chunk to free.
                // i.e. it's manual, artificial stutter (also, known as micro-freeze).
                if (bufferSize != 0L) {
                    var res = memAlloc.call();
                    do {
                        if (res == VK_SUCCESS) { break; }
                        if (res != VK_SUCCESS && res != -2) {
                            System.out.println("Allocation Failed: " + res);
                            throw new Exception("Allocation Failed: " + res);
                        }
                    } while ((res = memAlloc.call()) == -2 && !deviceObj.doPolling());

                    // if anyways, isn't allocated...
                    if (res != VK_SUCCESS) {
                        System.out.println("Allocation Failed, there is not free memory: " + res);
                        throw new Exception("Allocation Failed, there is not free memory: " + res);
                    }

                    // get device address from
                    this.address = this.heap.bufferHeap.getDeviceAddress() + this.bufferOffset.get(0);
                    if (earlyMapped) {
                        this.mapped = this.heap.bufferHeap.map(this.bufferSize, this.bufferOffset.get(0));
                    }
                }

            }
            return this;
        }

        @Override
        public VirtualMutableBufferObj delete() throws Exception {
            var oldAlloc = this.allocId.get(0);
            if (oldAlloc != 0) {
                var srcBufferRange = this.getBufferRange();
                deviceObj.submitOnce(deviceObj.getCommandPool(cInfo.queueFamilyIndex), new BasicCInfo.SubmitCmd(){{
                    // TODO: correctly handle main queue family
                    whatQueueFamilyWillWait = cInfo.queueFamilyIndex != 0 ? 0 : -1;
                    whatWaitBySemaphore = VK_PIPELINE_STAGE_TRANSFER_BIT;

                    //
                    queueFamilyIndex = cInfo.queueFamilyIndex;
                    queue = deviceObj.getQueue(cInfo.queueFamilyIndex, 0);
                    onDone = new Promise<>().thenApply((result)-> {
                        if (bound != null && oldAlloc != 0) {
                            vmaVirtualFree(heap.virtualBlock.get(0), oldAlloc);
                        }
                        bound.registry.removeIndex(DSC_ID);
                        return null;
                    });
                }}, (cmdBuf)->{
                    /*
                    vkCmdFillBuffer(cmdBuf, srcBufferRange.buffer(), srcBufferRange.offset(), srcBufferRange.range(), 0);
                    vkCmdPipelineBarrier2(cmdBuf, VkDependencyInfoKHR.calloc().sType(VK_STRUCTURE_TYPE_DEPENDENCY_INFO).pBufferMemoryBarriers(VkBufferMemoryBarrier2.calloc(1)
                        .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER_2)
                        .srcStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
                        .srcAccessMask( VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
                        .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
                        .dstAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
                        .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                        .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                        .buffer(srcBufferRange.buffer())
                        .offset(srcBufferRange.offset())
                        .size(srcBufferRange.range())
                    ));*/
                    return VK_SUCCESS;
                });
            } else {
                bound.registry.removeIndex(DSC_ID);
            }

            //
            this.bufferSize = 0L;
            this.blockSize = 0L;
            this.address = 0L;
            this.bufferOffset.put(0, 0L);
            this.allocId.put(0, 0L);
            this.mapped = null;
            return this;
        }

        @Override
        public VirtualMutableBufferObj deleteDirectly() throws Exception {
            var oldAlloc = this.allocId.get(0);
            if (bound != null && oldAlloc != 0) {
                //synchronized(this) {
                    vmaVirtualFree(heap.virtualBlock.get(0), oldAlloc);
                //}
            }
            this.bufferSize = 0L;
            this.blockSize = 0L;
            this.address = 0L;
            this.bufferOffset.put(0, 0L);
            this.allocId.put(0, 0L);
            this.mapped = null;
            this.bound.registry.removeIndex(this.DSC_ID);
            return this;
        }
    }

}
