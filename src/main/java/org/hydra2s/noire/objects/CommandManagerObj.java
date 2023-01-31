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
import org.lwjgl.vulkan.VkBufferCopy2;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static java.lang.Math.min;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.KHRAccelerationStructure.VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK13.VK_STRUCTURE_TYPE_BUFFER_COPY_2;

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

        //
        public VirtualAllocation(long virtualBlock, long range) {
            this.virtualBlock = virtualBlock;
            this.allocId = memAllocPointer(1).put(0, 0L);
            this.offset = memAllocLong(1).put(0, 0L);
            this.createInfo = VmaVirtualAllocationCreateInfo.calloc().alignment(16L);
            this.range = range;
            vmaVirtualAllocate(virtualBlock, this.createInfo.size(range), this.allocId, this.offset);
        }

        //
        public VirtualAllocation free() {
            vmaVirtualFree(this.virtualBlock, this.allocId.get(0));
            return this;
        }
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

        //
        public CommandWriter cmdCopyFromHostToBuffer(ByteBuffer data, VkDescriptorBufferInfo bufferRange, boolean lazy) {
            AtomicReference<VirtualAllocation> allocation_ = null;

            Runnable tempOp = ()-> {
                allocation_.set(new VirtualAllocation(this.manager.virtualBlock.get(0), min(data.remaining(), bufferRange.range())));
                var allocation = allocation_.get();
                this.allocations.add(allocation);
                memCopy(data, manager.bufferHeap.map(allocation.range, allocation.offset.get(0)));
            };

            if (!lazy) { tempOp.run(); };
            callers.add((cmdBuf)->{
                if (lazy) { tempOp.run(); };
                var allocation = allocation_.get();
                CommandUtils.cmdCopyVBufferToVBuffer(cmdBuf, VkDescriptorBufferInfo.calloc().set(manager.bufferHeap.getHandle().get(), allocation.offset.get(0), allocation.range), bufferRange);
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
        public CommandWriter submitOnce(DeviceObj.SubmitCmd cmd) throws Exception {
            if (cmd.onDone == null) {
                cmd.onDone = new Promise();
            }
            cmd.onDone.thenApply((status)->{
                freeResources();
                return status;
            });
            manager.deviceObj.submitOnce(cmd, (cmdBuf)->{
                cmdWrite(cmdBuf);
                return null;
            });
            return this;
        }
    };

    //
    public PointerBuffer virtualBlock = null;
    public BufferObj bufferHeap = null;
    public ArrayList<CommandWriter> writers = null;

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
        VmaVirtualBlockCreateInfo vbInfo = VmaVirtualBlockCreateInfo.calloc().flags(VMA_VIRTUAL_ALLOCATION_CREATE_STRATEGY_MIN_OFFSET_BIT | VMA_VIRTUAL_ALLOCATION_CREATE_STRATEGY_MIN_TIME_BIT | VMA_VIRTUAL_ALLOCATION_CREATE_STRATEGY_MIN_MEMORY_BIT);
        vmaCreateVirtualBlock(vbInfo.size(bufferHeapSize), this.virtualBlock = memAllocPointer(1).put(0, 0L));

        //
        this.handle = new Handle("CommandManager", MemoryUtil.memAddress(memAllocLong(1)));
        deviceObj.handleMap.put$(this.handle, this);
        memoryAllocatorObj.handleMap.put$(this.handle, this);
    }

    //
    public CommandWriter makeCmdWriter(CommandWriterCInfo cInfo) {
        return new CommandWriter(this, cInfo);
    }

}
