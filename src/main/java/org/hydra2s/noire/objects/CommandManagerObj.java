package org.hydra2s.noire.objects;

//
import org.hydra2s.noire.descriptors.BufferCInfo;
import org.hydra2s.noire.descriptors.CommandManagerCInfo;
import org.hydra2s.noire.descriptors.MemoryAllocationCInfo;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static java.lang.Math.min;
import static org.hydra2s.noire.descriptors.UtilsCInfo.vkCheckStatus;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.KHRAccelerationStructure.VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK13.*;
import static org.lwjgl.vulkan.VK13.VK_ACCESS_2_MEMORY_READ_BIT;

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
            vmaVirtualFree(this.virtualBlock, this.allocId.get(0));
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
        public ArrayList<Function<VkCommandBuffer, VkCommandBuffer>> callers = null;
        public ArrayList<VirtualAllocation> allocations = null;

        //
        public CommandWriter(CommandManagerObj manager, CommandWriterCInfo cInfo) {
            this.manager = manager;
            this.callers = new ArrayList<>();
            this.allocations = new ArrayList<>();
        }

        // TODO: correct naming
        public CommandWriter cmdAdd(Function<VkCommandBuffer, VkCommandBuffer> caller) {
            if (caller != null) {
                this.callers.add(this.callers.size(), caller);
            }
            return this;
        }

        //
        public CommandWriter cmdCopyFromHostToImage(ByteBuffer data, Callable<HostImageStage> imageInfoLazy, boolean lazy) throws Exception {
            AtomicReference<VirtualAllocation> allocation_ = new AtomicReference<>();
            AtomicReference<HostImageStage> imageInfo_ = new AtomicReference<>();
            var payloadBackup = memAlloc(data.remaining());
            memCopy(data, payloadBackup);


            Callable<Integer> tempOp = ()-> {
                imageInfo_.set(imageInfoLazy.call());
                allocation_.set(new VirtualAllocation(this.manager.virtualBlock.get(0), data.remaining()));
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
            this.cmdAdd((cmdBuf)->{
                if (lazy) {
                    try {
                        status.set(tempOp.call());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                };
                var allocation = allocation_.get();
                if (status.get() == 0) {
                    var allocOffset = allocation.offset.get(0);
                    var imageInfo = imageInfo_.get();
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
            AtomicReference<VirtualAllocation> allocation_ = new AtomicReference<>();
            AtomicReference<VkDescriptorBufferInfo> bufferRange_ = new AtomicReference<>();
            var payloadBackup = memAlloc(data.remaining());
            memCopy(data, payloadBackup);

            Callable<Integer> tempOp = ()-> {
                bufferRange_.set(bufferRangeLazy.call());
                allocation_.set(new VirtualAllocation(this.manager.virtualBlock.get(0), min(data.remaining(), bufferRange_.get().range())));
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
            this.cmdAdd((cmdBuf)->{
                if (lazy) {
                    try {
                        status.set(tempOp.call());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                };
                if (status.get() == 0) {
                    var bufferRange = bufferRange_.get();
                    var allocation = allocation_.get();
                    var allocOffset = allocation.offset.get(0);
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
        public CommandWriter freeResources() {
            this.allocations.forEach(VirtualAllocation::free);
            this.allocations.clear();
            return this;
        }

        //
        public CommandWriter cmdWrite(VkCommandBuffer commandBuffer) {
            callers.forEach((run)->{
                run.apply(commandBuffer);
            });
            callers.clear();
            return this;
        }

        //
        public DeviceObj.FenceProcess submitOnce(DeviceObj.SubmitCmd cmd) throws Exception {
            if (cmd.onDone == null) {
                cmd.onDone = new Promise();
            }
            cmd.onDone.thenApply((status)->{
                freeResources();
                return status;
            });
            var fence = manager.deviceObj.submitOnce(cmd, (cmdBuf)->{
                cmdWrite(cmdBuf);
                return null;
            });
            manager.deviceObj.doPolling();
            return fence;
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
