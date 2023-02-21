package org.hydra2s.noire.objects;

import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkConditionalRenderingBeginInfoEXT;
import org.lwjgl.vulkan.VkDescriptorBufferInfo;
import org.lwjgl.vulkan.VkQueryPoolCreateInfo;

import java.util.Arrays;
import java.util.function.Function;

import static org.lwjgl.vulkan.EXTConditionalRendering.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_ALL_COMMANDS_BIT;

public class CommandAgentObj {


    //
    public static class CommandAgent {
        public CommandManagerObj manager = null;
        public CommandManagerObj.CommandWriter commandWriter = null;

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

        public CommandAgent(CommandManagerObj manager, CommandManagerObj.CommandWriter commandWriter){
            this.manager = manager;
            this.commandWriter = commandWriter;
        }

        public CommandAgent delete() {
            return this;
        }
        public CommandAgent deleteDirectly() {
            return this;
        }
    }


    //
    public static class CommandOcclusionQuery extends CommandAgent {
        public long[] queryPool = {};
        public CommandManagerObj.VirtualAllocation occlusionBuffer = null;
        public int occlusionCounter = 0;
        public int occlusions = 0;

        public CommandOcclusionQuery(CommandManagerObj manager, CommandManagerObj.CommandWriter commandWriter, int occlusions) {
            super(manager, commandWriter);
            this.queryPool = new long[]{0L};
            this.occlusionCounter = 0;
            this.occlusionBuffer = new CommandManagerObj.VirtualAllocation(manager.virtualBlock.get(0), (this.occlusions = occlusions) * 4L);

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
            var $occlusionCounter = occlusionCounter; occlusionCounter = 0;
            commandWriter.cmdAdd$("", (cmdBuf)->{
                vkCmdFillBuffer(cmdBuf, manager.deviceHeap.handle.get(), occlusionBuffer.offset, occlusionBuffer.range, 0);
                vkCmdResetQueryPool(cmdBuf, queryPool[0], 0, occlusions);
                return cmdBuf;
            });
            return this;
        }

        @Override
        public CommandOcclusionQuery cmdBegin() {
            var $occlusionCounter = occlusionCounter;
            commandWriter.cmdAdd$("", (cmdBuf)->{
                vkCmdBeginQuery(cmdBuf, queryPool[0], $occlusionCounter, 0);
                return cmdBuf;
            });
            return this;
        }

        @Override
        public CommandOcclusionQuery cmdEnd() {
            var $occlusionCounter = occlusionCounter; occlusionCounter++;
            commandWriter.cmdAdd$("", (cmdBuf)->{
                vkCmdEndQuery(cmdBuf, queryPool[0], $occlusionCounter);
                return cmdBuf;
            });
            return this;
        }

        @Override
        public CommandOcclusionQuery cmdResolve() {
            var $occlusionCounter = occlusionCounter;
            commandWriter.cmdAdd$("", (cmdBuf)->{
                vkCmdCopyQueryPoolResults(cmdBuf, queryPool[0], 0, $occlusionCounter, manager.deviceHeap.handle.get(), occlusionBuffer.offset, 4, VK_QUERY_RESULT_WAIT_BIT );
                return cmdBuf;
            });
            commandWriter.addToFree(()->{

            });
            return this;
        }

        @Override
        public CommandOcclusionQuery deleteDirectly() {
            var occlusion = new int[occlusionCounter];
            vkGetQueryPoolResults(manager.deviceObj.device, queryPool[0], 0, occlusionCounter, occlusion, 4, VK_QUERY_RESULT_WAIT_BIT);
            vkDestroyQueryPool(manager.deviceObj.device, queryPool[0], null);
            occlusionCounter = 0;
            occlusionBuffer.free();
            return this;
        }

        @Override
        public CommandOcclusionQuery delete() {
            var $occlusionCounter = occlusionCounter; occlusionCounter = 0;
            commandWriter.addToFree(()->{
                var occlusion = new int[$occlusionCounter];
                vkGetQueryPoolResults(manager.deviceObj.device, queryPool[0], 0, $occlusionCounter, occlusion, 4, VK_QUERY_RESULT_WAIT_BIT);
                vkDestroyQueryPool(manager.deviceObj.device, queryPool[0], null);
                occlusionBuffer.free();
            });
            return this;
        }
    }

    //
    public static class CommandConditionalRendering extends CommandAgent {
        public VkDescriptorBufferInfo bufferRange = null;
        public int conditionCounter = 0;

        public CommandConditionalRendering(CommandManagerObj manager, CommandManagerObj.CommandWriter commandWriter, VkDescriptorBufferInfo bufferRange) {
            super(manager, commandWriter);
            this.bufferRange = bufferRange;
        }

        @Override
        public CommandConditionalRendering cmdReset() {
            var $conditionCounter = conditionCounter; conditionCounter = 0;
            commandWriter.cmdAdd$("", (cmdBuf)->{
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
            var $conditionCounter = conditionCounter; conditionCounter++;
            commandWriter.cmdAdd$("", (cmdBuf)->{
                vkCmdEndConditionalRenderingEXT(cmdBuf);
                return cmdBuf;
            });
            return this;
        }

        @Override
        public CommandConditionalRendering cmdResolve() {
            var $conditionCounter = conditionCounter; conditionCounter = 0;
            commandWriter.cmdAdd$("", (cmdBuf)->{
                return cmdBuf;
            });
            commandWriter.addToFree(()->{

            });
            return this;
        }

        @Override
        public CommandConditionalRendering deleteDirectly() {

            return this;
        }

        @Override
        public CommandConditionalRendering delete() {

            return this;
        }
    }

    // TODO: correct naming
    public static class CommandProfiler extends CommandAgent {
        public long[] queryPool = {};

        public long timeDiff = 0L;

        public CommandProfiler(CommandManagerObj manager, CommandManagerObj.CommandWriter commandWriter) {
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

        @Override
        public CommandProfiler deleteDirectly() {
            vkDestroyQueryPool(manager.deviceObj.device, queryPool[0], null);
            return this;
        }

        @Override
        public CommandProfiler delete() {
            commandWriter.addToFree(()->{
                vkDestroyQueryPool(manager.deviceObj.device, queryPool[0], null);
            });
            return this;
        }


    }

}
