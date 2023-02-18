package org.hydra2s.noire.objects;

//

import com.mojang.brigadier.Command;
import org.hydra2s.noire.descriptors.BufferCInfo;
import org.hydra2s.noire.descriptors.CommandManagerCInfo;
import org.hydra2s.noire.descriptors.MemoryAllocationCInfo;
import org.hydra2s.noire.descriptors.UtilsCInfo;
import org.hydra2s.utils.Promise;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.vma.VmaVirtualAllocationCreateInfo;
import org.lwjgl.util.vma.VmaVirtualBlockCreateInfo;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static java.lang.Math.min;
import static org.hydra2s.noire.descriptors.UtilsCInfo.vkCheckStatus;
import static org.hydra2s.noire.virtual.VirtualMutableBufferHeap.VirtualMutableBufferObj.roundUp;
import static org.lwjgl.BufferUtils.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.util.vma.Vma.*;
import static org.lwjgl.vulkan.EXTConditionalRendering.*;
import static org.lwjgl.vulkan.KHRAccelerationStructure.VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK13.VK_PIPELINE_STAGE_2_NONE;

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

        public long allocId = 0L;//memAllocPointer(1).put(0, 0L);
        public long offset = 0L;//createLongBuffer(1).put(0, 0L);

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
            this.range = range;

            //
            try ( MemoryStack stack = stackPush() ) {
                PointerBuffer $allocId = stack.callocPointer(1);
                LongBuffer $offset = stack.callocLong(1);
                this.status = vkCheckStatus(vmaVirtualAllocate(virtualBlock, this.createInfo = VmaVirtualAllocationCreateInfo.calloc(stack).flags(
                    VMA_VIRTUAL_ALLOCATION_CREATE_STRATEGY_MIN_OFFSET_BIT |
                        VMA_VIRTUAL_ALLOCATION_CREATE_STRATEGY_MIN_MEMORY_BIT |
                        VMA_VIRTUAL_ALLOCATION_CREATE_STRATEGY_MIN_TIME_BIT
                ).alignment(16L).size(range), $allocId, $offset));
                this.offset = $offset.get(0);
                this.allocId = $allocId.get(0);
            }
        }

        //
        public int getStatus() {
            return this.status;
        }

        //
        public VirtualAllocation free() {
            if (this.allocId != 0L) {
                vmaVirtualFree(this.virtualBlock, this.allocId);
            }
            return this;
        }
    }

    //
    public static class HostImageStage {
        // specific
        public CommandUtils.SubresourceLayers image = null;
        public VkExtent3D extent3D = null;
        //public VkOffset3D offset3D = VkOffset3D.create().set(0, 0, 0);
        public int rowLength = 0;
        public int imageHeight = 0;
    }

    //
    public static class CommandWriterBase {
        public CommandManagerObj manager = null;
        public ArrayList<UtilsCInfo.Pair<String, Function<VkCommandBuffer, VkCommandBuffer>>> callers = null;
        public ArrayList<VirtualAllocation> allocations = null;
        public ArrayList<Runnable> toFree = null;
        public ArrayList<CommandAgent> agents = null;
        public DeviceObj.FenceProcess previous = null;

        //
        public CommandWriterBase(CommandManagerObj manager, CommandWriterCInfo cInfo) {
            this.manager = manager;
            this.callers = new ArrayList<>();
            this.allocations = new ArrayList<>();
            this.toFree = new ArrayList<>();
            this.agents = new ArrayList<>();
        }

        // when skip rendering, needs to free unused resources
        public Callable<DeviceObj.FenceProcess> clear$(DeviceObj.SubmitCmd cmd) throws Exception {
            //if (this.callers.size() == 0) { return null; };
            this.callers.clear();
            //if (this.callers.size() == 0) { return null; };

            //
            var toFreeFn = new ArrayList<>(this.toFree); this.toFree.clear();
            var toFreeAlloc = new ArrayList<>(this.allocations);this.allocations.clear();
            var toFreeAgents = new ArrayList<>(this.agents); this.agents.clear();
            var callerCount = this.callers.size();

            //
            cmd.onDone = cmd.onDone != null ? cmd.onDone : new Promise();
            cmd.onDone.thenApply((status)->{
                // debug and profiling info
                //System.out.println("Begin collect data and free command resources.");
                //System.out.println("Command writes: " + callerCount);
                //System.out.println("Command free resources: " + toFreeAlloc.size());
                //System.out.println("Command free commands (include profiling): " + toFreeFn.size());

                // free resources and collect profiling data
                toFreeFn.forEach(Runnable::run); toFreeFn.clear();
                toFreeAlloc.forEach(VirtualAllocation::free); toFreeAlloc.clear();

                // collect timings of commands
                //AtomicLong fullCommandTime = new AtomicLong(0L);
                //toFreeProfilers.forEach((profiler)->{
                //fullCommandTime.addAndGet(profiler.timeDiff);
                //}); toFreeProfilers.clear();

                // debug and profiling info
                //System.out.println("Full commands time: " + ((double)fullCommandTime.get())/(double)(1000*1000) + "in milliseconds.");
                //System.out.println("End collect data and free command resources.");

                //
                return status;
            });

            //
            if (previous != null) {
                if (cmd.waitSemaphoreSubmitInfo == null) { cmd.waitSemaphoreSubmitInfo = new ArrayList<>(); };
                // TODO: currently there is no trivial way to free such sh&t later.
                // TODO: also, this sh&t are needed, due changes of timeline may just freeze game or OS.
                cmd.waitSemaphoreSubmitInfo.add(VkSemaphoreSubmitInfo.create().set(previous.timelineSemaphore.makeSubmissionTimeline(VK_PIPELINE_STAGE_2_NONE, true)));
            }

            //
            return ()->{
                previous = manager.deviceObj.submitOnce(cmd, (cmdBuf)->{
                    return null;
                }).call();
                manager.deviceObj.doPolling();
                return previous;
            };
        }

        // TODO: correct naming
        protected CommandWriterBase cmdAdd$(String typeName, Function<VkCommandBuffer, VkCommandBuffer> caller) {
            if (caller != null) {
                this.callers.add(this.callers.size(), new UtilsCInfo.Pair<>(typeName, caller));
            }
            return this;
        }

        // TODO: correct naming
        public CommandWriterBase cmdAdd(String typeName, Function<VkCommandBuffer, VkCommandBuffer> caller) {
            return this.cmdAdd$(typeName, caller);
        }

        // TODO: correct naming
        public CommandWriterBase cmdAdd(String typeName, Function<VkCommandBuffer, VkCommandBuffer> caller, CommandAgent... nested) {
            if (nested.length <= 1) {
                nested[0].cmdAdd(typeName, caller);
            } else {
                nested[nested.length-1].cmdAdd(typeName, caller, Arrays.copyOf(nested, nested.length-1));
            }
            return this;
        }

        //
        public CommandWriterBase cmdWrite(VkCommandBuffer commandBuffer) {
            callers.forEach((run)->{
                run.second.apply(commandBuffer);
            });
            callers.clear();
            return this;
        }

        //
        public Callable<DeviceObj.FenceProcess> submitOnce(DeviceObj.SubmitCmd cmd) throws Exception {
            //if (this.callers.size() == 0) { return null; };

            //
            var toFreeFn = new ArrayList<>(this.toFree); this.toFree.clear();
            var toFreeAlloc = new ArrayList<>(this.allocations);this.allocations.clear();
            var toFreeAgents = new ArrayList<>(this.agents); this.agents.clear();
            var callerCount = this.callers.size();

            //
            cmd.onDone = cmd.onDone != null ? cmd.onDone : new Promise();
            cmd.onDone.thenApply((status)->{
                // debug and profiling info
                //System.out.println("Begin collect data and free command resources.");
                //System.out.println("Command writes: " + callerCount);
                //System.out.println("Command free resources: " + toFreeAlloc.size());
                //System.out.println("Command free commands (include profiling): " + toFreeFn.size());

                // free resources and collect profiling data
                toFreeFn.forEach(Runnable::run); toFreeFn.clear();
                toFreeAlloc.forEach(VirtualAllocation::free); toFreeAlloc.clear();

                // collect timings of commands
                //AtomicLong fullCommandTime = new AtomicLong(0L);
                //toFreeProfilers.forEach((profiler)->{
                //fullCommandTime.addAndGet(profiler.timeDiff);
                //}); toFreeProfilers.clear();

                // debug and profiling info
                //System.out.println("Full commands time: " + ((double)fullCommandTime.get())/(double)(1000*1000) + "in milliseconds.");
                //System.out.println("End collect data and free command resources.");

                //
                return status;
            });

            //
            if (previous != null) {
                if (cmd.waitSemaphoreSubmitInfo == null) { cmd.waitSemaphoreSubmitInfo = new ArrayList<>(); };
                // TODO: currently there is no trivial way to free such sh&t later.
                // TODO: also, this sh&t are needed, due changes of timeline may just freeze game or OS.
                cmd.waitSemaphoreSubmitInfo.add(VkSemaphoreSubmitInfo.create().set(previous.timelineSemaphore.makeSubmissionTimeline(VK_PIPELINE_STAGE_2_NONE, true)));
            }

            return ()->{
                previous = manager.deviceObj.submitOnce(cmd, (cmdBuf)->{
                    cmdWrite(cmdBuf);
                    return null;
                }).call();
                manager.deviceObj.doPolling();
                return previous;
            };

            // DEBUG MODE!
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

        public CommandWriterBase addToFree(Runnable fn) {
            this.toFree.add(fn);
            return this;
        }

    }

    //
    public static class CommandAgent {
        public CommandManagerObj manager = null;
        public CommandWriter commandWriter = null;

        public CommandAgent cmdAdd(String typeName, Function<VkCommandBuffer, VkCommandBuffer> caller) {
            this.cmdReset().cmdBegin();
            commandWriter.cmdAdd$(typeName, caller);
            this.cmdEnd().cmdResolve();
            return this;
        }

        public CommandAgent cmdBegin() {
            return this;
        }

        public CommandAgent cmdEnd() {
            return this;
        }

        public CommandAgent cmdReset() {
            return this;
        }

        public CommandAgent cmdResolve() {
            commandWriter.agents.add(this);
            return this;
        }

        public CommandAgent cmdAdd(String typeName, Function<VkCommandBuffer, VkCommandBuffer> caller, CommandAgent... nested) {
            if (nested.length <= 1 && nested[0] == this) {
                this.cmdAdd(typeName, caller);
            } else {
                nested[nested.length-1].cmdReset().cmdBegin().cmdAdd(typeName, caller, nested.length == 1 ? new CommandAgent[]{this} : Arrays.copyOf(nested, nested.length-1)).cmdEnd().cmdResolve();
            }
            return this;
        }

        public CommandAgent(CommandManagerObj manager, CommandWriter commandWriter){
            this.manager = manager;
            this.commandWriter = commandWriter;
        }
    }


    //
    public static class CommandOcclusionQuery extends CommandAgent {
        public long[] queryPool = {};
        public VirtualAllocation occlusionBuffer = null;
        public int occlusionCounter = 0;

        public CommandOcclusionQuery(CommandManagerObj manager, CommandWriter commandWriter, int occlusions) {
            super(manager, commandWriter);
            this.queryPool = new long[]{0L};
            this.occlusionCounter = 0;
            this.occlusionBuffer = new VirtualAllocation(manager.virtualBlock.get(0), occlusions * 4L);

            //
            VkQueryPoolCreateInfo queryPoolCreateInfo = VkQueryPoolCreateInfo.create();
            queryPoolCreateInfo.sType(VK_STRUCTURE_TYPE_QUERY_POOL_CREATE_INFO);
            queryPoolCreateInfo.queryType(VK_QUERY_TYPE_OCCLUSION);
            queryPoolCreateInfo.queryCount(occlusions);
            vkCreateQueryPool(manager.deviceObj.device, queryPoolCreateInfo, null, this.queryPool);
        }

        //
        public VkDescriptorBufferInfo getBufferRange() {
            return VkDescriptorBufferInfo.create().buffer(manager.deviceHeap.handle.get()).offset(occlusionBuffer.offset).range(occlusionBuffer.range);
        }

        @Override
        public CommandOcclusionQuery cmdReset() {
            commandWriter.cmdAdd$("", (cmdBuf)->{
                vkCmdFillBuffer(cmdBuf, manager.deviceHeap.handle.get(), occlusionBuffer.offset, occlusionCounter*4L, 0);
                occlusionCounter = 0;
                return cmdBuf;
            });
            return this;
        }

        @Override
        public CommandOcclusionQuery cmdBegin() {
            commandWriter.cmdAdd$("", (cmdBuf)->{
                vkCmdBeginQuery(cmdBuf, queryPool[0], occlusionCounter, 0);
                return cmdBuf;
            });
            return this;
        }

        @Override
        public CommandOcclusionQuery cmdEnd() {
            commandWriter.cmdAdd$("", (cmdBuf)->{
                vkCmdEndQuery(cmdBuf, queryPool[0], occlusionCounter++);
                return cmdBuf;
            });
            return this;
        }

        @Override
        public CommandOcclusionQuery cmdResolve() {
            commandWriter.cmdAdd$("", (cmdBuf)->{
                vkCmdCopyQueryPoolResults(cmdBuf, VK_PIPELINE_STAGE_ALL_COMMANDS_BIT, 0, occlusionCounter, manager.deviceHeap.handle.get(), occlusionBuffer.offset, 4, VK_QUERY_RESULT_WAIT_BIT );
                return cmdBuf;
            });
            commandWriter.addToFree(()->{

            });
            return this;
        }
    }

    //
    public static class CommandConditionalRendering extends CommandAgent {
        public VkDescriptorBufferInfo bufferRange = null;
        public int conditionCounter = 0;

        public CommandConditionalRendering(CommandManagerObj manager, CommandWriter commandWriter, VkDescriptorBufferInfo bufferRange) {
            super(manager, commandWriter);
            this.bufferRange = bufferRange;
        }

        @Override
        public CommandConditionalRendering cmdReset() {
            commandWriter.cmdAdd$("", (cmdBuf)->{
                conditionCounter = 0;
                return cmdBuf;
            });
            return this;
        }

        @Override
        public CommandConditionalRendering cmdBegin() {
            commandWriter.cmdAdd$("", (cmdBuf)->{
                vkCmdBeginConditionalRenderingEXT(cmdBuf, VkConditionalRenderingBeginInfoEXT.create()
                    .sType(VK_STRUCTURE_TYPE_CONDITIONAL_RENDERING_BEGIN_INFO_EXT)
                    .buffer(bufferRange.buffer())
                    .offset(bufferRange.offset() + conditionCounter* 4L)
                    .flags(0));
                return cmdBuf;
            });
            return this;
        }

        @Override
        public CommandConditionalRendering cmdEnd() {
            commandWriter.cmdAdd$("", (cmdBuf)->{
                vkCmdEndConditionalRenderingEXT(cmdBuf);
                conditionCounter++;
                return cmdBuf;
            });
            return this;
        }

        @Override
        public CommandConditionalRendering cmdResolve() {
            commandWriter.cmdAdd$("", (cmdBuf)->{
                conditionCounter = 0;
                return cmdBuf;
            });
            commandWriter.addToFree(()->{

            });
            return this;
        }
    }



    // TODO: command agents support
    public static class CommandProfiler extends CommandAgent {
        public long[] queryPool = {};

        public long timeDiff = 0L;

        public CommandProfiler(CommandManagerObj manager, CommandWriter commandWriter) {
            super(manager, commandWriter);
            this.queryPool = new long[]{0L};

            //
            VkQueryPoolCreateInfo queryPoolCreateInfo = VkQueryPoolCreateInfo.create();
            queryPoolCreateInfo.sType(VK_STRUCTURE_TYPE_QUERY_POOL_CREATE_INFO);
            queryPoolCreateInfo.queryType(VK_QUERY_TYPE_TIMESTAMP);
            queryPoolCreateInfo.queryCount(2);

            //
            vkCreateQueryPool(manager.deviceObj.device, queryPoolCreateInfo, null, this.queryPool);
        }

        @Override
        public CommandProfiler cmdBegin() {
            commandWriter.cmdAdd$("Command Profiler Begin Record", (cmdBuf)->{
                vkCmdWriteTimestamp(cmdBuf, VK_PIPELINE_STAGE_ALL_COMMANDS_BIT, this.queryPool[0], 0);
                return cmdBuf;
            });
            return this;
        }

        @Override
        public CommandProfiler cmdResolve() {
            commandWriter.addToFree(()->{
                var timestamps = new long[]{0L, 0L};
                vkGetQueryPoolResults(manager.deviceObj.device, queryPool[0], 0, 2, timestamps, 8, VK_QUERY_RESULT_64_BIT | VK_QUERY_RESULT_WAIT_BIT);
                vkDestroyQueryPool(manager.deviceObj.device, queryPool[0], null);

                timeDiff = (timestamps[1] - timestamps[0]);
                System.out.println("...command time stamp diff: " + ((double)timeDiff/(double)(1000*1000)) + " in milliseconds.");
            });
            return this;
        }

        @Override
        public CommandProfiler cmdEnd() {
            commandWriter.cmdAdd$("Command Profiler End Record", (cmdBuf)->{
                vkCmdWriteTimestamp(cmdBuf, VK_PIPELINE_STAGE_ALL_COMMANDS_BIT, this.queryPool[0], 1);
                return cmdBuf;
            });
            return this;
        }


    }

    //
    public static class DataDownloader extends CommandWriterBase {

        //
        public DataDownloader(CommandManagerObj manager, CommandWriterCInfo cInfo) {
            super(manager, cInfo);
        }

        //
        public Callable<DeviceObj.FenceProcess> cmdCopyFromImageToHost(Callable<HostImageStage> imageInfoLazy, ByteBuffer data, DeviceObj.SubmitCmd cmd) throws Exception {
            AtomicReference<VirtualAllocation> allocation_ = new AtomicReference<>();
            AtomicReference<HostImageStage> imageInfo_ = new AtomicReference<>();

            //
            Callable<Integer> tempOp = ()-> {
                allocation_.set(new VirtualAllocation(manager.virtualBlock.get(0), data.remaining()));
                var allocation = allocation_.get(); var status = allocation.getStatus();
                allocations.add(allocation);
                if (status == 0) {
                    var allocOffset = allocation.offset;
                } else {
                    System.out.println("Allocation Failed: " + status + ", memory probably ran out...");
                    throw new Exception("Allocation Failed: " + status + ", memory probably ran out...");
                }
                return status;
            };

            //
            boolean lazy = false;

            //
            AtomicInteger status = new AtomicInteger(-2);
            if (!lazy) { status.set(tempOp.call()); };
            this.cmdAdd("Image To Host Copy", (cmdBuf)->{
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
                    var allocOffset = allocation.offset;
                    var imageInfo = imageInfo_.get();

                    if (allocation.range <= 0L) {
                        throw new RuntimeException("Command Writer Error (Image To Host): Bad Dst Range");
                    };

                    //
                    CommandUtils.cmdCopyImageToBuffer(cmdBuf, imageInfo.image, new CommandUtils.BufferCopyInfo() {{
                        buffer = manager.bufferHeap.getHandle().get();
                        offset = allocOffset;
                        range = allocation.range;
                        rowLength = imageInfo.rowLength;
                        imageHeight = imageInfo.imageHeight;
                    }}, imageInfo.extent3D);
                }
                return cmdBuf;
            });

            //
            this.addToFree(()->{
                var allocation = allocation_.get();
                if (status.get() == 0) {
                    var allocOffset = allocation.offset;
                    memCopy(manager.bufferHeap.map(allocation.range, allocOffset), data);
                }
            });

            //
            return this.submitOnce(cmd);
        }
    }

    //
    public static class CommandWriter extends CommandWriterBase {

        public CommandWriter(CommandManagerObj manager, CommandWriterCInfo cInfo) {
            super(manager, cInfo);
        }

        //
        public CommandWriter cmdCopyFromHostToImage(ByteBuffer data,  Callable<HostImageStage> imageInfoLazy, boolean lazy) throws Exception {
            return this.cmdCopyFromHostToImage(data, imageInfoLazy, lazy, false);
        }

        //
        public CommandWriter cmdCopyFromHostToImage(ByteBuffer data, Callable<HostImageStage> imageInfoLazy, boolean lazy, boolean directly) throws Exception {
            AtomicReference<VirtualAllocation> allocation_ = new AtomicReference<>();
            AtomicReference<HostImageStage> imageInfo_ = new AtomicReference<>();
            var payloadBackup = (directly || !lazy) ? data : createByteBuffer(data.remaining()); if (!directly && lazy) { memCopy(data, payloadBackup); };
            memCopy(data, payloadBackup);

            //
            Callable<Integer> tempOp = ()-> {
                allocation_.set(new VirtualAllocation(manager.virtualBlock.get(0), payloadBackup.remaining()));
                var allocation = allocation_.get(); var status = allocation.getStatus();
                allocations.add(allocation);
                if (status == 0) {
                    var allocOffset = allocation.offset;
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
                    var allocOffset = allocation.offset;
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

                    /*vkCmdPipelineBarrier2(cmdBuf, VkDependencyInfoKHR.create().sType(VK_STRUCTURE_TYPE_DEPENDENCY_INFO).pMemoryBarriers(VkMemoryBarrier2.create(1)
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
        public CommandWriter cmdCopyFromHostToBuffer(ByteBuffer data, Function<MemoryStack, VkDescriptorBufferInfo> bufferRangeLazy, boolean lazy) throws Exception {
            return this.cmdCopyFromHostToBuffer(data, bufferRangeLazy, lazy, false);
        }

        //
        public CommandWriter cmdCopyFromHostToBuffer(ByteBuffer data, Function<MemoryStack, VkDescriptorBufferInfo> bufferRangeLazy, boolean lazy, boolean directly) throws Exception {
            AtomicReference<VirtualAllocation> allocation_ = new AtomicReference<>();
            AtomicReference<VkDescriptorBufferInfo> bufferRange_ = new AtomicReference<>();
            var payloadBackup = (directly || !lazy) ? data : createByteBuffer(data.remaining()); if (!directly && lazy) { memCopy(data, payloadBackup); };

            Callable<Integer> tempOp = ()-> {
                allocation_.set(new VirtualAllocation(manager.virtualBlock.get(0), payloadBackup.remaining()));
                var allocation = allocation_.get(); var status = allocation.getStatus();
                allocations.add(allocation);
                if (status == 0) {
                    var allocOffset = allocation.offset;
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
                    var allocation = allocation_.get();
                    var allocOffset = allocation.offset;

                    if (allocation.range <= 0L) {
                        throw new RuntimeException("Command Writer Error (Host To Buffer): Bad Src or Dst Range");
                    };

                    try ( MemoryStack stack = stackPush() ) {
                        CommandUtils.cmdCopyVBufferToVBuffer(cmdBuf, VkDescriptorBufferInfo.calloc(stack).set(manager.bufferHeap.getHandle().get(), allocOffset, allocation.range), bufferRangeLazy.apply(stack));
                    }

                    /*vkCmdPipelineBarrier2(cmdBuf, VkDependencyInfoKHR.create().sType(VK_STRUCTURE_TYPE_DEPENDENCY_INFO).pMemoryBarriers(VkMemoryBarrier2.create(1)
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


    };

    //
    public PointerBuffer virtualBlock = null;
    public BufferObj bufferHeap = null;
    public BufferObj deviceHeap = null;
    public ArrayList<CommandWriter> writers = null;
    protected VmaVirtualBlockCreateInfo vbInfo = null;

    //
    public CommandManagerObj(UtilsCInfo.Handle base, CommandManagerCInfo cInfo) {
        super(base, cInfo);

        //
        var bufferHeapSize = min(min(1024L * 1024L * 1024L * 4L, 0xFFFFFFFFL), physicalDeviceObj.properties.vulkan11.maxMemoryAllocationSize());
        this.bufferHeap = new BufferObj(base, new BufferCInfo() {{
            size = bufferHeapSize;
            usage = VK_BUFFER_USAGE_VERTEX_BUFFER_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT | VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR;
            memoryAllocator = cInfo.memoryAllocator;
            memoryAllocationInfo = new MemoryAllocationCInfo() {{
                isHost = true;
                isDevice = true;
            }};
        }});

        this.deviceHeap = new BufferObj(base, new BufferCInfo() {{
            size = 1024L * 1024L * 4L;
            usage = VK_BUFFER_USAGE_CONDITIONAL_RENDERING_BIT_EXT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT | VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR;
            memoryAllocator = cInfo.memoryAllocator;
            memoryAllocationInfo = new MemoryAllocationCInfo() {{
                isHost = false;
                isDevice = true;
            }};
        }});

        //
        vkCheckStatus(vmaCreateVirtualBlock(this.vbInfo = VmaVirtualBlockCreateInfo.create().size(bufferHeapSize), this.virtualBlock = createPointerBuffer(1).put(0, 0L)));

        //
        this.handle = new UtilsCInfo.Handle("CommandManager", MemoryUtil.memAddress(createLongBuffer(1)));
        deviceObj.handleMap.put$(this.handle, this);
    }

    //
    public CommandWriter makeCmdWriter(CommandWriterCInfo cInfo) {
        return new CommandWriter(this, cInfo);
    }

    //
    public DataDownloader makeCmdDownloader(CommandWriterCInfo cInfo) {
        return new DataDownloader(this, cInfo);
    }

}
