package org.hydra2s.noire.objects;

//

import org.hydra2s.noire.descriptors.BufferCInfo;
import org.hydra2s.noire.descriptors.CommandManagerCInfo;
import org.hydra2s.noire.descriptors.MemoryAllocationCInfo;
import org.hydra2s.noire.descriptors.UtilsCInfo;
import org.hydra2s.utils.Promise;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vma.VmaVirtualAllocationCreateInfo;
import org.lwjgl.util.vma.VmaVirtualBlockCreateInfo;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkExtent3D;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static java.lang.Math.min;
import static org.hydra2s.noire.descriptors.UtilsCInfo.vkCheckStatus;
import static org.hydra2s.noire.virtual.VirtualMutableBufferHeap.VirtualMutableBufferObj.roundUp;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.KHRAccelerationStructure.VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR;
import static org.lwjgl.vulkan.VK10.*;

// TODO: planned class in gen-v3 or gen-v2 part IV.
// What planned?
// - Generation one big command buffers.
// - Better memory management (virtual buffers).
// - Memory pre-allocations.
// - Built-in host memory (for staging).
// - CommandPool and DeviceObj pin-up.
// - Command buffer management library.
public class CommandManagerObj extends BasicObj {

    //
    public static class CommandWriterCInfo {

    };

    // TODO: using buffer binding with virtual allocation
    public static class VirtualAllocation {
        public long virtualBlock = 0L;
        public long range = 0L;
        public PointerBuffer allocId = memAllocPointer(1).put(0, 0L);
        public LongBuffer offset = memAllocLong(1).put(0, 0L);
        protected VmaVirtualAllocationCreateInfo createInfo = null;
        protected int status = -2;

        //
        public VirtualAllocation(long virtualBlock, long range) {

            //
            if (range <= 0L) {
                System.out.println("Command Writer Resource Allocation Failed, zero or less sized, not supported...");
                throw new RuntimeException("Command Writer Resource Allocation Failed, zero or less sized, not supported...");
            }

            //
            final var MEM_BLOCK = 16;
            range = roundUp(range, MEM_BLOCK) * MEM_BLOCK;

            //
            if (range <= 0L) {
                System.out.println("Command Writer Resource Allocation Failed, zero or less sized, not supported...");
                throw new RuntimeException("Command Writer Resource Allocation Failed, zero or less sized, not supported...");
            }

            //
            this.virtualBlock = virtualBlock;
            this.allocId = memAllocPointer(1).put(0, 0L);
            this.offset = memAllocLong(1).put(0, 0L);
            this.range = range;
            this.status = vkCheckStatus(vmaVirtualAllocate(virtualBlock, this.createInfo = VmaVirtualAllocationCreateInfo.calloc().alignment(16L).size(range), this.allocId, this.offset));
        }

        //
        public int getStatus() {
            return this.status;
        }

        //
        public VirtualAllocation free() {
            if (this.allocId.get(0) != 0L) {
                vmaVirtualFree(this.virtualBlock, this.allocId.get(0));
            }
            this.allocId.put(0, 0L);
            return this;
        }
    }

    //
    public static class HostImageStage {
        // specific
        public CommandUtils.SubresourceLayers image = null;
        public VkExtent3D extent3D = null;
        //public VkOffset3D offset3D = VkOffset3D.calloc().set(0, 0, 0);
        public int rowLength = 0;
        public int imageHeight = 0;
    }

    //
    public static class CommandWriter {
        public CommandManagerObj manager = null;
        public ArrayList<UtilsCInfo.Pair<String, Function<VkCommandBuffer, VkCommandBuffer>>> callers = null;
        public ArrayList<VirtualAllocation> allocations = null;
        public ArrayList<Runnable> toFree = null;

        //
        public CommandWriter(CommandManagerObj manager, CommandWriterCInfo cInfo) {
            this.manager = manager;
            this.callers = new ArrayList<>();
            this.allocations = new ArrayList<>();
            this.toFree = new ArrayList<>();
        }

        // TODO: correct naming
        public CommandWriter cmdAdd(String typeName, Function<VkCommandBuffer, VkCommandBuffer> caller) {
            if (caller != null) {
                this.callers.add(this.callers.size(), new UtilsCInfo.Pair<>(typeName, caller));
            }
            return this;
        }

        //
        public CommandWriter cmdCopyFromHostToImage(ByteBuffer data,  Callable<HostImageStage> imageInfoLazy, boolean lazy) throws Exception {
            return this.cmdCopyFromHostToImage(data, imageInfoLazy, lazy, false);
        }

        //
        public CommandWriter cmdCopyFromHostToImage(ByteBuffer data, Callable<HostImageStage> imageInfoLazy, boolean lazy, boolean directly) throws Exception {
            AtomicReference<VirtualAllocation> allocation_ = new AtomicReference<>();
            AtomicReference<HostImageStage> imageInfo_ = new AtomicReference<>();
            var payloadBackup = (directly || !lazy) ? data : memAlloc(data.remaining()); if (!directly && lazy) { memCopy(data, payloadBackup); };
            memCopy(data, payloadBackup);

            //
            Callable<Integer> tempOp = ()-> {
                allocation_.set(new VirtualAllocation(this.manager.virtualBlock.get(0), payloadBackup.remaining()));
                var allocation = allocation_.get(); var status = allocation.getStatus();
                this.allocations.add(allocation);
                if (status == 0) {
                    var allocOffset = allocation.offset.get(0);
                    memCopy(payloadBackup, manager.bufferHeap.map(allocation.range, allocOffset));
                } else {
                    System.out.println("Allocation Failed: " + status + ", memory probably ran out...");
                    throw new Exception("Allocation Failed: " + status + ", memory probably ran out...");
                }
                return status;
            };

            AtomicInteger status = new AtomicInteger(-2);
            if (!lazy) { status.set(tempOp.call()); };
            this.cmdAdd("Host To Image Copy", (cmdBuf)->{
                if (lazy) {
                    try {
                        status.set(tempOp.call());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                };
                var allocation = allocation_.get();
                if (status.get() == 0) {
                    try {
                        imageInfo_.set(imageInfoLazy.call());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    var allocOffset = allocation.offset.get(0);
                    var imageInfo = imageInfo_.get();

                    if (allocation.range <= 0L) {
                        throw new RuntimeException("Command Writer Error (Host To Image): Bad Src Range");
                    };

                    CommandUtils.cmdCopyBufferToImage(cmdBuf, new CommandUtils.BufferCopyInfo() {{
                        buffer = manager.bufferHeap.getHandle().get();
                        offset = allocOffset;
                        range = allocation.range;
                        rowLength = imageInfo.rowLength;
                        imageHeight = imageInfo.imageHeight;
                    }}, imageInfo.image, imageInfo.extent3D);

                    /*vkCmdPipelineBarrier2(cmdBuf, VkDependencyInfoKHR.calloc().sType(VK_STRUCTURE_TYPE_DEPENDENCY_INFO).pMemoryBarriers(VkMemoryBarrier2.calloc(1)
                        .sType(VK_STRUCTURE_TYPE_MEMORY_BARRIER_2)
                        .srcStageMask(VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT | VK_PIPELINE_STAGE_2_HOST_BIT | VK_PIPELINE_STAGE_2_COPY_BIT)
                        .srcAccessMask(VK_ACCESS_2_TRANSFER_READ_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
                        .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
                        .dstAccessMask(VK_ACCESS_2_MEMORY_READ_BIT)));*/
                }
                return cmdBuf;
            });

            return this;
        }

        //
        public CommandWriter cmdCopyFromHostToBuffer(ByteBuffer data, Callable<VkDescriptorBufferInfo> bufferRangeLazy, boolean lazy) throws Exception {
            return this.cmdCopyFromHostToBuffer(data, bufferRangeLazy, lazy, false);
        }

        //
        public CommandWriter cmdCopyFromHostToBuffer(ByteBuffer data, Callable<VkDescriptorBufferInfo> bufferRangeLazy, boolean lazy, boolean directly) throws Exception {
            AtomicReference<VirtualAllocation> allocation_ = new AtomicReference<>();
            AtomicReference<VkDescriptorBufferInfo> bufferRange_ = new AtomicReference<>();
            var payloadBackup = (directly || !lazy) ? data : memAlloc(data.remaining()); if (!directly && lazy) { memCopy(data, payloadBackup); };

            Callable<Integer> tempOp = ()-> {
                allocation_.set(new VirtualAllocation(this.manager.virtualBlock.get(0), payloadBackup.remaining()));
                var allocation = allocation_.get(); var status = allocation.getStatus();
                this.allocations.add(allocation);
                if (status == 0) {
                    var allocOffset = allocation.offset.get(0);
                    memCopy(payloadBackup, manager.bufferHeap.map(payloadBackup.remaining(), allocOffset));
                } else {
                    System.out.println("Allocation Failed: " + status + ", memory probably ran out...");
                    throw new Exception("Allocation Failed: " + status + ", memory probably ran out...");
                }
                return status;
            };

            AtomicInteger status = new AtomicInteger(-2);
            if (!lazy) { status.set(tempOp.call()); };
            this.cmdAdd("Host To Buffer Copy", (cmdBuf)->{
                if (lazy) {
                    try {
                        status.set(tempOp.call());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                };
                if (status.get() == 0) {
                    try {
                        bufferRange_.set(bufferRangeLazy.call());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    var bufferRange = bufferRange_.get();
                    var allocation = allocation_.get();
                    var allocOffset = allocation.offset.get(0);

                    if (bufferRange.range() <= 0L || allocation.range <= 0L) {
                        throw new RuntimeException("Command Writer Error (Host To Buffer): Bad Src or Dst Range");
                    };

                    CommandUtils.cmdCopyVBufferToVBuffer(cmdBuf, VkDescriptorBufferInfo.calloc().set(manager.bufferHeap.getHandle().get(), allocOffset, allocation.range), bufferRange);

                    /*vkCmdPipelineBarrier2(cmdBuf, VkDependencyInfoKHR.calloc().sType(VK_STRUCTURE_TYPE_DEPENDENCY_INFO).pMemoryBarriers(VkMemoryBarrier2.calloc(1)
                        .sType(VK_STRUCTURE_TYPE_MEMORY_BARRIER_2)
                        .srcStageMask(VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT | VK_PIPELINE_STAGE_2_HOST_BIT | VK_PIPELINE_STAGE_2_COPY_BIT)
                        .srcAccessMask(VK_ACCESS_2_TRANSFER_READ_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
                        .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
                        .dstAccessMask(VK_ACCESS_2_MEMORY_READ_BIT)));*/
                }
                return cmdBuf;
            });

            return this;
        }

        //
        /*public CommandWriter freeResources() {
            this.allocations.forEach(VirtualAllocation::free);
            this.allocations.clear();
            return this;
        }*/

        //
        public CommandWriter cmdWrite(VkCommandBuffer commandBuffer) {
            callers.forEach((run)->{
                run.second.apply(commandBuffer);
            });
            callers.clear();
            return this;
        }

        //
        public DeviceObj.FenceProcess submitOnce(DeviceObj.SubmitCmd cmd) throws Exception {
            if (this.callers.size() == 0) { return null; };

            //
            var toFreeFn = new ArrayList<Runnable>(this.toFree);
            this.toFree.clear();

            //
            var toFreeAlloc = new ArrayList<VirtualAllocation>(this.allocations);
            this.allocations.clear();

            //
            cmd.onDone = cmd.onDone != null ? cmd.onDone : new Promise();
            cmd.onDone.thenApply((status)->{
                toFreeAlloc.forEach(VirtualAllocation::free); toFreeAlloc.clear();
                toFreeFn.forEach(Runnable::run); toFreeFn.clear();
                return status;
            });

            //
            var fence = manager.deviceObj.submitOnce(cmd, (cmdBuf)->{
                cmdWrite(cmdBuf);
                return null;
            });

            manager.deviceObj.doPolling();
            return fence;


            /*AtomicReference<DeviceObj.FenceProcess> last = new AtomicReference();
            this.callers.forEach((fn)->{
                DeviceObj.FenceProcess fence = null;

                //
                System.out.println("Command Type Name: " + fn.first);
                System.out.println("Command Sequence ID: " + callers.indexOf(fn) + " of " + callers.size());

                //
                try {
                    fence = manager.deviceObj.submitOnce(new DeviceObj.SubmitCmd(){{
                        queueGroupIndex = cmd.queueGroupIndex;
                        commandPoolIndex = cmd.commandPoolIndex;
                        if (callers.indexOf(fn) == (callers.size()-1)) {
                            waitSemaphoreSubmitInfo = cmd.waitSemaphoreSubmitInfo;
                            signalSemaphoreSubmitInfo = cmd.signalSemaphoreSubmitInfo;
                            onDone = cmd.onDone != null ? cmd.onDone : new Promise();
                            onDone.thenApply((status)->{
                                toFreeAlloc.forEach(VirtualAllocation::free); toFreeAlloc.clear();
                                toFreeFn.forEach(Runnable::run); toFreeFn.clear();
                                return status;
                            });
                        }
                    }}, fn.second);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                //
                manager.deviceObj.doPolling();

                //
                if (fence.fence != null && fence.fence.get(0) != 0) {
                    vkCheckStatus(vkWaitForFences(manager.deviceObj.device, fence.fence, true, 9007199254740991L));
                };

                //
                if (callers.indexOf(fn) == (callers.size()-1)) {
                    last.set(fence);
                }
            });
            this.callers.clear();
            return last.get();*/



        }

        public CommandWriter addToFree(Runnable fn) {
            this.toFree.add(fn);
            return this;
        }
    };

    //
    public PointerBuffer virtualBlock = null;
    public BufferObj bufferHeap = null;
    public ArrayList<CommandWriter> writers = null;
    protected VmaVirtualBlockCreateInfo vbInfo = null;

    //
    public CommandManagerObj(Handle base, CommandManagerCInfo cInfo) {
        super(base, cInfo);

        //
        var bufferHeapSize = min(min(1024L * 1024L * 1024L * 4L, 0xFFFFFFFFL), physicalDeviceObj.properties.vulkan11.maxMemoryAllocationSize());
        this.bufferHeap = new BufferObj(base, new BufferCInfo() {{
            size = bufferHeapSize;
            usage = VK_BUFFER_USAGE_VERTEX_BUFFER_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT | VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR;
            memoryAllocator = cInfo.memoryAllocator;
            memoryAllocationInfo = new MemoryAllocationCInfo() {{
                isHost = true;
                isDevice = false;
            }};
        }});

        //
        vkCheckStatus(vmaCreateVirtualBlock(this.vbInfo = VmaVirtualBlockCreateInfo.calloc().flags(VMA_VIRTUAL_ALLOCATION_CREATE_STRATEGY_MIN_OFFSET_BIT | VMA_VIRTUAL_ALLOCATION_CREATE_STRATEGY_MIN_TIME_BIT | VMA_VIRTUAL_ALLOCATION_CREATE_STRATEGY_MIN_MEMORY_BIT).size(bufferHeapSize), this.virtualBlock = memAllocPointer(1).put(0, 0L)));

        //
        this.handle = new Handle("CommandManager", MemoryUtil.memAddress(memAllocLong(1)));
        deviceObj.handleMap.put$(this.handle, this);
    }

    //
    public CommandWriter makeCmdWriter(CommandWriterCInfo cInfo) {
        return new CommandWriter(this, cInfo);
    }

}
