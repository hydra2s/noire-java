package org.hydra2s.noire.objects;

//

import com.lodborg.intervaltree.IntervalTree;
import org.hydra2s.noire.descriptors.BasicCInfo;
import org.hydra2s.noire.descriptors.DeviceCInfo;
import org.hydra2s.noire.descriptors.DeviceCInfo.QueueFamilyCInfo;
import org.hydra2s.noire.descriptors.SemaphoreCInfo;
import org.hydra2s.noire.descriptors.UtilsCInfo;
import org.hydra2s.utils.Promise;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static java.lang.System.currentTimeMillis;
import static org.hydra2s.noire.descriptors.UtilsCInfo.vkCheckStatus;
import static org.lwjgl.BufferUtils.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.KHRSwapchain.VK_STRUCTURE_TYPE_PRESENT_INFO_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkQueuePresentKHR;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK13.*;

//
// TODO: implement await all fences and do all actions
public class DeviceObj extends BasicObj {

    public int[] queueFamilyIndices = null;
    //
    protected int[] layersAmount = new int[]{0};

    // TODO: use per queue family dedicate
    public ArrayList<Function<Integer, Integer>> whenDone = new ArrayList<Function<Integer, Integer>>();

    //
    public PointerBuffer layers = null;
    public PointerBuffer extensions = null;

    //
    public VkLayerProperties.Buffer availableLayers = null;
    public VkExtensionProperties.Buffer availableExtensions = null;
    public VkDeviceCreateInfo deviceInfo = null;
    public VkDevice device = null;
    public VkDeviceQueueCreateInfo.Buffer queueFamiliesCInfo = null;
    protected int[] extensionAmount = new int[]{0};

    //
    public ArrayList<SemaphoreObj> reusableSemaphoreStack = null;
    public long lastSubmit = 0;

    //
    public static class QueueInfo {
        public ArrayList<UtilsCInfo.Pair<SemaphoreObj, VkSemaphoreSubmitInfo>> waitSemaphores = null;
        public ArrayList<SemaphoreObj> querySemaphoreObj = null;
        public int semaphoreId = 0;
        public VkQueue queue = null;
    };

    //
    public static class SubmitCmd extends BasicCInfo {
        public VkCommandBuffer cmdBuf = null;
        public Promise<Integer> onDone = null;
        public int queueGroupIndex = -1;
        public int commandPoolIndex = 0;

        //
        public ArrayList<VkSemaphoreSubmitInfo> waitSemaphoreSubmitInfo = null;
        public ArrayList<VkSemaphoreSubmitInfo> signalSemaphoreSubmitInfo = null;

        //
        public int[] whatQueueGroupWillWait = {};
        public long whatWaitBySemaphore = VK_PIPELINE_STAGE_2_NONE;
        public long[] fence = {};

        public Function<VkCommandBuffer, VkCommandBuffer> writable = null;
    };

    //
    public static class CommandPoolInfo extends BasicCInfo {
        public ArrayList<VkCommandBuffer> cmdBufCache = null;
        public PointerBuffer cmdBufBlock = null;
        public int cmdBufIndex = 0;
        //public ArrayList<PointerBuffer> cmdBufferBlocks = null;
        public ArrayList<UtilsCInfo.Pair<long[], VkCommandBuffer>> onceCmdBuffers = null;
    };



    // TODO: replace by queue group instead of queue family
    // TODO: add queue indices support
    // TODO: add timeline semaphore support
    public static class QueueFamily {
        public int index = 0;
        public QueueFamilyCInfo cInfo = new QueueFamilyCInfo();
        public ArrayList<QueueInfo> queueInfos = null;
    };

    //
    public UtilsCInfo.CombinedMap<Integer, QueueFamily> queueFamilies = new UtilsCInfo.CombinedMap<Integer, QueueFamily>(16);
    public ArrayList<DeviceCInfo.QueueGroup> queueGroups = null;


    //
    public DeviceObj(UtilsCInfo.Handle base, DeviceCInfo cInfo) {
        super(base, cInfo);

        //
        List<String> extbuf = Arrays.asList(
            "VK_KHR_swapchain",
            "VK_KHR_deferred_host_operations",
            "VK_KHR_acceleration_structure",
            "VK_KHR_ray_query",
            "VK_EXT_conservative_rasterization",
            "VK_EXT_extended_dynamic_state3",
            "VK_EXT_extended_dynamic_state2",
            "VK_EXT_robustness2",
            "VK_EXT_vertex_input_dynamic_state",
            "VK_EXT_descriptor_buffer", // needs termination code here
            "VK_EXT_multi_draw",
            "VK_KHR_fragment_shader_barycentric",
            "VK_EXT_mesh_shader",
            "VK_EXT_pipeline_robustness",
            "VK_EXT_shader_image_atomic_int64",
            "VK_EXT_shader_atomic_float",
            "VK_KHR_shader_clock",
            "VK_KHR_ray_tracing_maintenance1",
            "VK_KHR_workgroup_memory_explicit_layout",
            "VK_EXT_mutable_descriptor_type",
            "VK_EXT_transform_feedback",
            "VK_EXT_shader_atomic_float2", // broken support in NVIDIA
            "VK_EXT_memory_budget",
            "VK_EXT_image_2d_view_of_3d",
            "VK_EXT_index_type_uint8",
            "VK_VALVE_mutable_descriptor_type",
            "VK_KHR_pipeline_library",
            "VK_EXT_graphics_pipeline_library",
            "VK_EXT_pageable_device_local_memory",
            "VK_EXT_memory_priority",
            "VK_KHR_global_priority",
            "VK_EXT_fragment_shader_interlock",
            "VK_EXT_device_address_binding_report",

            "VK_KHR_external_semaphore",
            "VK_KHR_external_semaphore_win32",
            "VK_KHR_external_semaphore_fd",
            "VK_KHR_external_memory",
            "VK_KHR_external_memory_win32",
            "VK_KHR_external_memory_fd",

            "VK_NV_representative_fragment_test"
        );

        //
        var deviceExtensions = (extbuf.stream().filter((E) -> {
            Boolean found = false;
            var Es = physicalDeviceObj.extensions.remaining();
            for (int K = 0; K < Es; K++) {
                String X = MemoryUtil.memUTF8(physicalDeviceObj.extensions.get(K).extensionName());
                if (X.contains(E)) {
                    found = true;
                    break;
                }
            }
            return found;
        }).toList());

        // Extensions
        this.extensions = createPointerBuffer(deviceExtensions.size());
        var Es = this.extensions.remaining();
        for (int I = 0; I < Es; I++) {
            this.extensions.put(I, memUTF8(deviceExtensions.get(I)));
        }

        //
        var Qs = cInfo.queueFamilies.size();
        this.queueFamiliesCInfo = org.lwjgl.vulkan.VkDeviceQueueCreateInfo.create(Qs);
        this.queueFamilyIndices = new int[Qs];

        //
        for (var Q = 0; Q < Qs; Q++) {
            var cQF = cInfo.queueFamilies.get(Q);
            var qf = new QueueFamily();
            qf.index = cQF.index;
            qf.cInfo = cQF;
            qf.queueInfos = new ArrayList<>();

            //
            var queuePriorities = createFloatBuffer(cQF.priorities.length);
            var Qp = queuePriorities.remaining();
            for (var I=0;I<Qp;I++) {
                queuePriorities.put(I, cQF.priorities[I]);
                qf.queueInfos.add(new QueueInfo(){{
                    waitSemaphores = new ArrayList<>();
                    semaphoreId = 0;
                }});
            }

            //
            this.queueFamiliesCInfo.get(Q).sType(VK10.VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO).pQueuePriorities(queuePriorities).queueFamilyIndex(cQF.index);
            this.queueFamilyIndices[Q] = qf.index;
            this.queueFamilies.put$(cQF.index, qf);
        }

        //
        this.reusableSemaphoreStack = new ArrayList<>();
        this.queueGroups = cInfo.queueGroups;

        //
        if (this.queueGroups == null) {
            this.queueGroups = new ArrayList<>();
            this.queueFamilies.forEach((index, qF)->{
                var group = new DeviceCInfo.QueueGroup();
                group.queueIndices = new ArrayList<>();
                group.queueFamilyIndex = index;
                group.queueBusy = new ArrayList<>();
                for (var I=0;I<qF.get().queueInfos.size();I++) {
                    group.queueIndices.add(I);
                    group.queueBusy.add(0);
                }
                this.queueGroups.add(group);
            });
        }

        //
        this.queueGroups.forEach((group)->{
            if (group.queueBusy == null) {
                group.queueBusy = new ArrayList<>();
                for (var I=0;I<group.queueIndices.size();I++) {
                    group.queueBusy.add(0);
                }
            };
        });

        // TODO: Handle VkResult!!
        var result = vkCheckStatus(VK10.vkCreateDevice(physicalDeviceObj.physicalDevice, this.deviceInfo = VkDeviceCreateInfo.create()
                .pNext(physicalDeviceObj.features.features.address())
                .sType(VK10.VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                .pQueueCreateInfos(this.queueFamiliesCInfo)
                .ppEnabledExtensionNames(this.extensions)
                .ppEnabledLayerNames(this.layers)
                , null, this.ptr = createPointerBuffer(1)));
        this.handle = new UtilsCInfo.Handle("Device", this.ptr);

        BasicObj.globalHandleMap.put$(this.handle.get(), this);
        handleMap = new UtilsCInfo.CombinedMap<UtilsCInfo.Handle, BasicObj>();
        rootMap = new UtilsCInfo.CombinedMap<Long, Long>();
        addressMap = new IntervalTree<>();

        //
        this.device = new VkDevice(this.handle.get(), physicalDeviceObj.physicalDevice, this.deviceInfo);
        this.queueGroups.forEach((group)->{
            //
            group.cmdPool = new long[group.commandPoolCount];
            group.commandPoolInfo = new ArrayList<>();
            for (var C=0;C<group.commandPoolCount;C++) {
                group.commandPoolInfo.add(new CommandPoolInfo(){{
                    cmdBufCache = new ArrayList<>();
                    onceCmdBuffers = new ArrayList<UtilsCInfo.Pair<long[], VkCommandBuffer>>();
                }});

                //
                long cmdPool[] = new long[]{0L};
                vkCheckStatus(VK10.vkCreateCommandPool(this.device, org.lwjgl.vulkan.VkCommandPoolCreateInfo.create().sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO).flags(0).queueFamilyIndex(group.queueFamilyIndex), null, cmdPool));
                group.cmdPool[C] = cmdPool[0];
            }
        });

        //
        this.queueFamilies.forEach((QF, QFI)->{
            for (var I=0;I<QFI.get().queueInfos.size();I++) {
                var queue = createPointerBuffer(1);
                VK10.vkGetDeviceQueue(this.device, QF, I, queue);
                QFI.get().queueInfos.get(I).queue = new VkQueue(queue.get(0), this.device);
            }
        });

    }

    //
    public DeviceObj(UtilsCInfo.Handle base, UtilsCInfo.Handle handle) {
        super(base, handle);
    }

    //
    public long createShaderModule(ByteBuffer shaderSrc){
        var shaderModuleInfo = VkShaderModuleCreateInfo.create().sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO).pCode(shaderSrc);
        var shaderModule = createLongBuffer(1);
        vkCheckStatus(vkCreateShaderModule(device, shaderModuleInfo, null, shaderModule));
        return shaderModule.get(0);
    }

    //
    public DeviceObj freeCommandsByQueueGroup(int queueGroupIndex, int commandPoolIndex, ArrayList<VkCommandBuffer> commandBuffers) {
        var commandPool = this.getCommandPool(queueGroupIndex, commandPoolIndex);
        commandBuffers.forEach((cmdBuf)->{
            vkFreeCommandBuffers(device, commandPool, cmdBuf);
        });
        return this;
    }

    //
    public DeviceObj present(int queueGroupIndex, long SwapChain, long waitSemaphore, int[] imageIndex) {
        var queueGroup = this.queueGroups.get(queueGroupIndex);
        try ( MemoryStack stack = stackPush() ) {
            var intBuf = stack.callocInt(1);
            intBuf.put(0, imageIndex[0]);
            vkCheckStatus(vkQueuePresentKHR(this.getQueue(queueGroup.queueFamilyIndex, queueGroup.queueIndices.get(0)), VkPresentInfoKHR.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
                .pWaitSemaphores(waitSemaphore != 0 ? stack.callocLong(1).put(0, waitSemaphore) : null)
                .pSwapchains(stack.callocLong(1).put(0, SwapChain)).swapchainCount(1)
                .pImageIndices(intBuf)));
        }
        return this;
    }

    //
    private VkQueue getQueue(int queueFamilyIndex, int queueIndex) {
        return this.queueFamilies.get(queueFamilyIndex).get().queueInfos.get(queueIndex).queue;
    }

    //
    private long getCommandPool(int queueGroupIndex, int commandPoolIndex) {
        return this.queueGroups.get(queueGroupIndex).cmdPool[commandPoolIndex];
    }

    //
    public DeviceObj resetCommandPool(int queueGroupIndex, int commandPoolIndex) {
        this.doPolling();

        //
        var queueGroup = this.queueGroups.get(queueGroupIndex);
        var commandPoolInfo = queueGroup.commandPoolInfo.get(commandPoolIndex);
        var commandPool = getCommandPool(queueGroupIndex, commandPoolIndex);
        //commandPoolInfo.cmdBufIndex = 0;
        //commandPoolInfo.cmdBufCache.clear();
        //commandPoolInfo.cmdBufBlock = null;

        //
        //pair.first[0] != 0L ? vkWaitForFences(device, pair.first, true, 9007199254740991L) : VK_ERROR_DEVICE_LOST;

        //
        var status = vkCheckStatus(vkWaitForFences(device, commandPoolInfo.onceCmdBuffers.stream().mapToLong((pair)->{
            return pair.first[0];
        }).toArray(), true, 9007199254740991L));

        // impossible to free command buffers, even sent once
        commandPoolInfo.onceCmdBuffers = new ArrayList<UtilsCInfo.Pair<long[], VkCommandBuffer>>(commandPoolInfo.onceCmdBuffers.stream().filter((pair)->{
            if (status != VK_NOT_READY) {
                // TODO: free command buffer allocation
                if (status != VK_SUCCESS) { vkCheckStatus(status); };
                vkDestroyFence(device, pair.first[0], null);
                vkFreeCommandBuffers(device, commandPool, pair.second);
            }
            return status == VK_NOT_READY;
        }).toList());

        // Corrupted and BROKEN function! (currently...)
        //vkResetCommandPool(device, commandPool, /*VK_COMMAND_POOL_RESET_RELEASE_RESOURCES_BIT*/0);

        //
        return this;
    }

    // use it when a polling
     public boolean doPolling() {
        this.whenDone = new ArrayList<Function<Integer, Integer>>(this.whenDone.stream().filter((F)->{ return F.apply(null) == VK_NOT_READY; }).toList());

         // get rid of outdated semaphores
         this.queueFamilies.forEach((QF, QFI)->{
             QFI.get().queueInfos.forEach((QI)->{
                 QI.waitSemaphores = new ArrayList<>(QI.waitSemaphores.stream().filter((semaphore)->{
                     var timeline = semaphore.first.getTimeline();
                     var prevTimeline = semaphore.second.value();//semaphore.first.prevTimeline;
                     semaphore.second.free();
                     if (timeline < 0L) { throw new RuntimeException("Device Lost when tried to get rid of outdated waiting semaphores!"); };
                     boolean ready = timeline >= prevTimeline;
                     return !ready;
                 }).toList());
             });
         });

         //
        return this.whenDone.isEmpty();
    }

    public VkCommandBuffer writeCommand(VkCommandBuffer cmdBuf, Function<VkCommandBuffer, Integer> fn) {
        vkCheckStatus(vkBeginCommandBuffer(cmdBuf, VkCommandBufferBeginInfo.create()
            .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
            .flags(VK_COMMAND_BUFFER_USAGE_SIMULTANEOUS_USE_BIT)));
        fn.apply(cmdBuf);
        vkCheckStatus(vkEndCommandBuffer(cmdBuf));
        return cmdBuf;
    }

    // you also needs `device.doPolling` or `device.waitFence`
    public static class FenceProcess {
        public SemaphoreObj timelineSemaphore = null;
        public Function<Integer, Integer> deallocProcess = null;
        public Promise<Integer> promise = null; // for getting status
        public long[] fence = null;
        public int status = VK_NOT_READY;
    }

    //
    public SemaphoreObj createTempSemaphore() throws Exception {
        SemaphoreObj tempSemaphore = null;
        if (reusableSemaphoreStack.size() > 0) {
            tempSemaphore = reusableSemaphoreStack.remove(0);
            tempSemaphore.waitTimeline(true);
        } else {
            tempSemaphore = new SemaphoreObj(this.getHandle(), new SemaphoreCInfo(){{
                doRegister = false;
                isTimeline = true;
                initialValue = 1;
            }});
        }
        return tempSemaphore.signalTimeline();
    }

    //
     public SemaphoreObj backTempSemaphore(SemaphoreObj tempSemaphore) /*throws Exception*/ {
        // bad-ass idea...
        //tempSemaphore.waitTimeline(true);
        tempSemaphore.deleteDirectly();
        //if (reusableSemaphoreStack.size() < 1024) {
            //reusableSemaphoreStack.add(tempSemaphore);
        //}
        //lastSubmit = max(tempSemaphore.lastTimeline, lastSubmit);
        //tempSemaphore.lastTimeline = lastSubmit++;
        return tempSemaphore;
    }

    // for beginning of rendering
     public FenceProcess makeSignaled() throws Exception {
        var fence_ = new long[]{0L};
        //vkCreateFence(this.device, VkFenceCreateInfo.create().sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO).flags(VK_FENCE_CREATE_SIGNALED_BIT), null, fence_);
        var querySignalSemaphore = createTempSemaphore();
        var prevTimeline = querySignalSemaphore.prevTimeline;
        var ref = new FenceProcess() {{
            fence = fence_;
            deallocProcess = (result)->{
                var fence_ = fence[0];
                var timeline =  querySignalSemaphore.getTimeline();
                status = (timeline <= prevTimeline && timeline >= 0) ? VK_NOT_READY : VK_SUCCESS;
                if (timeline < 0L || timeline == -1L || timeline == 0xffffffffffffffffL) { status = VK_ERROR_DEVICE_LOST; };
                if (status != VK_NOT_READY) {
                    backTempSemaphore(querySignalSemaphore);

                    if (status == VK_SUCCESS) {
                        promise.fulfill(status);
                    } else {
                        promise.fulfillExceptionally(new Exception("Status: " + status + "! Device Lost!"));
                        throw new RuntimeException("Status: " + status + "! Device Lost!");
                    }
                }
                return status;
            };
            promise = new Promise();
        }};

        //
        this.whenDone.add(ref.deallocProcess);
        //this.doPolling();

        //
        return ref;
    }

    // TODO: fence registry, and correctly wait by fence
     public int waitFence(FenceProcess process, long maxMilliseconds) {
         if (process == null) { return VK_SUCCESS; };
         var beginTiming = currentTimeMillis();
        while (process.status == VK_NOT_READY && (currentTimeMillis() - beginTiming) < maxMilliseconds) {
            this.doPolling();
        };
        return process.status;
    }

    public int waitFence(FenceProcess process) {
        return waitFence(process, 10000);
    }

     //
     public VkCommandBuffer allocateCommand(int queueGroupIndex, int commandPoolIndex) {
        var maxCommandBufferCache = 8;
        var queueGroup = this.queueGroups.get(queueGroupIndex);
        var commandPoolInfo = queueGroup.commandPoolInfo.get(commandPoolIndex);
        if (commandPoolInfo.cmdBufCache == null || commandPoolInfo.cmdBufIndex >= commandPoolInfo.cmdBufCache.size()) {
            var commandPool = this.getCommandPool(queueGroupIndex, commandPoolIndex);
            if (commandPoolInfo.cmdBufCache == null) {
                commandPoolInfo.cmdBufCache = new ArrayList<>(maxCommandBufferCache);
            }
            var commandBufferBlock = createPointerBuffer(maxCommandBufferCache);
            //commandPoolInfo.cmdBufferBlocks.add(commandBufferBlock);
            commandPoolInfo.cmdBufBlock = commandBufferBlock;
            vkAllocateCommandBuffers(this.device, VkCommandBufferAllocateInfo.create()
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                .commandPool(commandPool)
                .commandBufferCount(maxCommandBufferCache), commandBufferBlock);
            commandPoolInfo.cmdBufCache.clear();
            for (var I=0;I<maxCommandBufferCache;I++) {
                commandPoolInfo.cmdBufCache.add(new VkCommandBuffer(commandPoolInfo.cmdBufBlock.get(I), device));
            }
            commandPoolInfo.cmdBufIndex = 0;
        }
        return commandPoolInfo.cmdBufCache.get(commandPoolInfo.cmdBufIndex++);
    }

    //
     public FenceProcess submitCommand(SubmitCmd cmd) throws Exception {
         if (cmd.queueGroupIndex < 0) { throw new Exception("Cmd Submission Error - Bad Queue Group Index!"); };
         if (cmd.cmdBuf == null) { throw new Exception("Cmd Submission Error - There Is Not Command Buffer!"); };

         // TODO: don't use clone operation
        //var signalSemaphoreSubmitInfo = (ArrayList<VkSemaphoreSubmitInfo>)(cmd.signalSemaphoreSubmitInfo != null ? cmd.signalSemaphoreSubmitInfo.clone() : new ArrayList<VkSemaphoreSubmitInfo>());
        //var waitSemaphoreSubmitInfo = (ArrayList<VkSemaphoreSubmitInfo>)(cmd.waitSemaphoreSubmitInfo != null ? cmd.waitSemaphoreSubmitInfo.clone() : new ArrayList<VkSemaphoreSubmitInfo>());

         //
         var signalSemaphoreSubmitInfo = new ArrayList<VkSemaphoreSubmitInfo>(cmd.signalSemaphoreSubmitInfo != null ? cmd.signalSemaphoreSubmitInfo : new ArrayList<VkSemaphoreSubmitInfo>());
         var waitSemaphoreSubmitInfo = new ArrayList<VkSemaphoreSubmitInfo>(cmd.waitSemaphoreSubmitInfo != null ? cmd.waitSemaphoreSubmitInfo : new ArrayList<VkSemaphoreSubmitInfo>());

         //
        var queueGroup = this.queueGroups.get(cmd.queueGroupIndex);
        var queueFamily = this.queueFamilies.get(queueGroup.queueFamilyIndex).get();

         // TODO: globalize loading of queue
         var lessBusyCount = Collections.min(queueGroup.queueBusy);
         var lessBusy = queueGroup.queueBusy.indexOf(lessBusyCount);
         var lessBusyQ = queueGroup.queueIndices.get(lessBusy);
         var queueInfo = queueFamily.queueInfos.get(lessBusyQ);
         queueGroup.queueBusy.set(lessBusy, queueGroup.queueBusy.get(lessBusy)+1);

         //
         final int maxSemaphoreQueue = 8;
         if (queueInfo.querySemaphoreObj == null) {
             queueInfo.querySemaphoreObj = new ArrayList<>();
             for (var I=0;I<maxSemaphoreQueue;I++) {
                 queueInfo.querySemaphoreObj.add(new SemaphoreObj(handle, new SemaphoreCInfo() {{
                     doRegister = false;
                     isTimeline = true;
                     initialValue = 1;
                 }}).signalTimeline());
             }
         }

         // WARNING! Needs await previously timeline!
         // TODO: async semaphore system (up to 8 semaphores)!
         var querySignalSemaphore = queueInfo.querySemaphoreObj.get(queueInfo.semaphoreId++).waitTimeline(true); queueInfo.semaphoreId %= maxSemaphoreQueue;//createTempSemaphore();
         //var queryWaitSubmitInfo = querySignalSemaphore.makeSubmissionTimeline(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT, true);
         var querySignalSubmitInfo = querySignalSemaphore.makeSubmissionTimeline(cmd.whatWaitBySemaphore, false);

         //
         var prevTimeline = querySignalSemaphore.prevTimeline;

         //
         //var directionalSemaphore = createTempSemaphore();
         //var directionalSubmitInfo = directionalSemaphore.makeSubmissionTimeline(cmd.whatWaitBySemaphore, false);
         var directionalSemaphore = querySignalSemaphore;
         var directionalSubmitInfo = querySignalSubmitInfo;

         //
         try ( MemoryStack stack = stackPush() ) {
             //
             signalSemaphoreSubmitInfo.add(querySignalSubmitInfo);
             var signalSemaphores = VkSemaphoreSubmitInfo.calloc(signalSemaphoreSubmitInfo.size(), stack);
             for (var I = 0; I < signalSemaphoreSubmitInfo.size(); I++) {
                 signalSemaphores.get(I).set(signalSemaphoreSubmitInfo.get(I));
             }

             //
             queueInfo.waitSemaphores.forEach((pair) -> { waitSemaphoreSubmitInfo.add(pair.second); });
             var waitSemaphores = VkSemaphoreSubmitInfo.calloc(waitSemaphoreSubmitInfo.size(), stack);
             for (var I = 0; I < waitSemaphoreSubmitInfo.size(); I++) {
                 waitSemaphores.get(I).set(waitSemaphoreSubmitInfo.get(I));
             }
             queueInfo.waitSemaphores.forEach((pair) -> { pair.second.free(); });
             queueInfo.waitSemaphores.clear();

             //
             for (int W : cmd.whatQueueGroupWillWait) {
                 if (W >= 0 && W != cmd.queueGroupIndex) {
                     var whatQueueGroup = queueGroups.get(W);
                     whatQueueGroup.queueIndices.forEach((idx)->{
                         var whatQueueInfo = queueFamilies.get(whatQueueGroup.queueFamilyIndex).get().queueInfos.get(idx);
                         whatQueueInfo.waitSemaphores.add(new UtilsCInfo.Pair<>(directionalSemaphore, VkSemaphoreSubmitInfo.calloc().set(directionalSubmitInfo)));
                         //directionalSemaphore.incrementShared(); // don't delete too early
                     });
                 }
             }

             //
             var cmdInfo = VkCommandBufferSubmitInfo.calloc(1, stack);
             cmdInfo.get(0).sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_SUBMIT_INFO).commandBuffer(cmd.cmdBuf).deviceMask(0);

             //
             vkCheckStatus(vkQueueSubmit2(this.getQueue(queueGroup.queueFamilyIndex, lessBusyQ), VkSubmitInfo2.calloc(1, stack)
                 .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO_2)
                 .pCommandBufferInfos(cmdInfo)
                 .pSignalSemaphoreInfos(signalSemaphores)
                 .pWaitSemaphoreInfos(waitSemaphores), cmd.fence[0]));

         }

        //
        if (cmd.onDone == null) { cmd.onDone = new Promise(); };

        //
         var timeline = querySignalSemaphore.getTimeline();
         var status = (timeline <= prevTimeline && timeline >= 0) ? VK_NOT_READY : VK_SUCCESS;
         if (timeline < 0L) { status = VK_ERROR_DEVICE_LOST; }; // bad semaphore
         if (status == VK_ERROR_DEVICE_LOST) { throw new RuntimeException("Status: " + status + "! Device Lost! (Semaphore Timeline: " + timeline + ")"); };

        //
        var ref = new FenceProcess() {{
            fence = cmd.fence;
            timelineSemaphore = querySignalSemaphore;
            deallocProcess = (_null_)->{
                var timeline = querySignalSemaphore.getTimeline();
                status = (timeline <= prevTimeline && timeline >= 0) ? VK_NOT_READY : VK_SUCCESS;
                if (timeline < 0L || timeline == -1L || timeline == 0xffffffffffffffffL) { status = VK_ERROR_DEVICE_LOST; }; // bad semaphore
                if (status != VK_NOT_READY) {
                    queueGroup.queueBusy.set(lessBusy, queueGroup.queueBusy.get(lessBusy)-1);

                    //
                    if (status == VK_SUCCESS) {
                        cmd.onDone.fulfill(status);
                    } else {
                        cmd.onDone.fulfillExceptionally(new Exception("Status: " + status + "! Device Lost! (Semaphore Timeline: " + timeline + ")"));
                        throw new RuntimeException("Status: " + status + "! Device Lost! (Semaphore Timeline: " + timeline + ")");
                    }
                }
                return status;
            };
            promise = cmd.onDone;
        }};

        //
        this.whenDone.add(ref.deallocProcess);
        //this.doPolling();

        //
        return ref;
    }

    //
    public FenceProcess submitOnce(SubmitCmd submitCmd, Function<VkCommandBuffer, VkCommandBuffer> fn) throws Exception {
        if (submitCmd.queueGroupIndex < 0) { throw new Exception("Cmd Submission Error - Bad Queue Group Index!"); };

         // TODO: allocate command buffer with fence
        var queueGroup = this.queueGroups.get(submitCmd.queueGroupIndex);
        var commandPoolInfo = queueGroup.commandPoolInfo.get(submitCmd.commandPoolIndex);

        // TODO: rarer fence creation
        submitCmd.fence = new long[]{0L};
        try ( MemoryStack stack = stackPush() ) {
            vkCheckStatus(vkCreateFence(this.device, VkFenceCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO), null, submitCmd.fence));
            vkCheckStatus(vkBeginCommandBuffer(submitCmd.cmdBuf = this.allocateCommand(submitCmd.queueGroupIndex, submitCmd.commandPoolIndex), VkCommandBufferBeginInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT)));
        }
        var profiling = new ProfilingUtilsObj();
        //profiling.recordBeginTime();
        fn.apply(submitCmd.cmdBuf);
        //profiling.recordDiffTime("Profiler: Writing Command Time: ");
        vkCheckStatus(vkEndCommandBuffer(submitCmd.cmdBuf));

        //
        var pair = new UtilsCInfo.Pair<long[], VkCommandBuffer>(submitCmd.fence, submitCmd.cmdBuf);
        if (submitCmd.onDone == null) { submitCmd.onDone = new Promise<>(); };
        submitCmd.writable = fn;
        submitCmd.onDone.thenApply((status)->{
            commandPoolInfo.onceCmdBuffers.add(pair);
            return status;
        });

        //
        return submitCommand(submitCmd);
    }

}
