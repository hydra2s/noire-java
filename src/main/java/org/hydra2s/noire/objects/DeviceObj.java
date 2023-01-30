package org.hydra2s.noire.objects;

//
import org.hydra2s.noire.descriptors.BasicCInfo;
import org.hydra2s.noire.descriptors.DeviceCInfo;
import org.hydra2s.noire.descriptors.DeviceCInfo.QueueFamilyCInfo;
import org.hydra2s.noire.descriptors.SemaphoreCInfo;
import org.hydra2s.utils.Promise;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

//
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.*;
import java.util.function.Function;

//
import static java.lang.Math.max;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.KHRSwapchain.VK_STRUCTURE_TYPE_PRESENT_INFO_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkQueuePresentKHR;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK13.*;

//
// TODO: implement await all fences and do all actions
public class DeviceObj extends BasicObj {

    public IntBuffer queueFamilyIndices = null;
    //
    protected IntBuffer layersAmount = memAllocInt(1).put(0, 0);

    // TODO: use per queue family dedicate
    public ArrayList<Function<LongBuffer, Integer>> whenDone = new ArrayList<Function<LongBuffer, Integer>>();

    //
    public PointerBuffer layers = null;
    public PointerBuffer extensions = null;

    //
    public VkLayerProperties.Buffer availableLayers = null;
    public VkExtensionProperties.Buffer availableExtensions = null;
    public VkDeviceCreateInfo deviceInfo = null;
    public VkDevice device = null;
    public VkDeviceQueueCreateInfo.Buffer queueFamiliesCInfo = null;
    protected IntBuffer extensionAmount = memAllocInt(1).put(0, 0);


    //
    public ArrayList<SemaphoreObj> reusableSemaphoreStack = null;
    public long lastSubmit = 0;

    //
    public static class QueueInfo {
        public ArrayList<VkSemaphoreSubmitInfo> waitSemaphoresInfo = null;
        public ArrayList<SemaphoreObj> waitSemaphores = null;
    };

    //
    public static class SubmitCmd extends BasicCInfo {
        public VkCommandBuffer cmdBuf = null;
        public Promise<Integer> onDone = null;
        public int queueGroupIndex = 0;
        public int commandPoolIndex = 0;

        //
        public ArrayList<VkSemaphoreSubmitInfo> waitSemaphoreSubmitInfo = null;
        public ArrayList<VkSemaphoreSubmitInfo> signalSemaphoreSubmitInfo = null;

        //
        public int whatQueueGroupWillWait = -1;
        public long whatWaitBySemaphore = VK_PIPELINE_STAGE_2_NONE;
    };

    //
    public static class CommandPoolInfo extends BasicCInfo {
        public ArrayList<VkCommandBuffer> cmdBufCache = null;
        public PointerBuffer cmdBufBuffer = null;
        public int cmdBufIndex = 0;
    };



    // TODO: replace by queue group instead of queue family
    // TODO: add queue indices support
    // TODO: add timeline semaphore support
    public static class QueueFamily {
        public int index = 0;
        public LongBuffer cmdPool = memAllocLong(10).put(0, 0);
        public QueueFamilyCInfo cInfo = new QueueFamilyCInfo();

        //
        public ArrayList<QueueInfo> queueInfos = null;
        public ArrayList<CommandPoolInfo> commandPoolInfo = null;
    };

    //
    public CombinedMap<Integer, QueueFamily> queueFamilies = new CombinedMap<Integer, QueueFamily>(16);
    public ArrayList<DeviceCInfo.QueueGroup> queueGroups = null;


    //
    public DeviceObj(Handle base, DeviceCInfo cInfo) {
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
            //"VK_EXT_device_address_binding_report",

            "VK_KHR_external_semaphore",
            "VK_KHR_external_semaphore_win32",
            "VK_KHR_external_semaphore_fd",
            "VK_KHR_external_memory",
            "VK_KHR_external_memory_win32",
            "VK_KHR_external_memory_fd"
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
        this.extensions = PointerBuffer.allocateDirect(deviceExtensions.size());
        var Es = this.extensions.remaining();
        for (int I = 0; I < Es; I++) {
            this.extensions.put(I, memUTF8(deviceExtensions.get(I)));
        }

        //
        var Qs = cInfo.queueFamilies.size();
        this.queueFamiliesCInfo = org.lwjgl.vulkan.VkDeviceQueueCreateInfo.calloc(Qs);
        this.queueFamilyIndices = memAllocInt(Qs);

        //
        for (var Q = 0; Q < Qs; Q++) {
            var cQF = cInfo.queueFamilies.get(Q);
            var qf = new QueueFamily();
            qf.index = cQF.index;
            qf.cInfo = cQF;
            qf.queueInfos = new ArrayList<>();
            qf.cmdPool = memAllocLong(cQF.commandPoolCount);
            qf.commandPoolInfo = new ArrayList<>();

            //
            for (var C=0;C<cQF.commandPoolCount;C++) {
                qf.commandPoolInfo.add(new CommandPoolInfo(){{
                    cmdBufCache = new ArrayList<>();
                }});
            }

            //
            var queuePriorities = memAllocFloat(cQF.priorities.length);
            var Qp = queuePriorities.remaining();
            for (var I=0;I<Qp;I++) {
                queuePriorities.put(I, cQF.priorities[I]);
                qf.queueInfos.add(new QueueInfo(){{
                    waitSemaphoresInfo = new ArrayList<>();
                    waitSemaphores = new ArrayList<>();
                }});
            }

            //
            this.queueFamiliesCInfo.get(Q).sType(VK10.VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO).pQueuePriorities(queuePriorities).queueFamilyIndex(cQF.index);
            this.queueFamilyIndices.put(Q, qf.index);
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
        var result = VK10.vkCreateDevice(physicalDeviceObj.physicalDevice, this.deviceInfo = VkDeviceCreateInfo.calloc()
                .pNext(physicalDeviceObj.features.features.address())
                .sType(VK10.VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                .pQueueCreateInfos(this.queueFamiliesCInfo)
                .ppEnabledExtensionNames(this.extensions)
                .ppEnabledLayerNames(this.layers)
                , null, (this.handle = new Handle("Device")).ptr());
        BasicObj.globalHandleMap.put$(this.handle.get(), this);

        //
        this.device = new VkDevice(this.handle.get(), physicalDeviceObj.physicalDevice, this.deviceInfo);

        //
        for (int Q = 0; Q < Qs; Q++) {
            var qfi = this.queueFamilyIndices.get(Q);
            var cmdPoolBuf = this.queueFamilies.get(qfi).orElse(null).cmdPool;
            for (var C=0;C<cmdPoolBuf.remaining();C++) {
                VK10.vkCreateCommandPool(this.device, org.lwjgl.vulkan.VkCommandPoolCreateInfo.calloc().sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO).flags(0).queueFamilyIndex(qfi), null, memSlice(cmdPoolBuf, C, 1));
            }
        }

    }

    //
    public DeviceObj(Handle base, Handle handle) {
        super(base, handle);
    }

    //
    public long createShaderModule(ByteBuffer shaderSrc){
        var shaderModuleInfo = VkShaderModuleCreateInfo.calloc().sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO).pCode(shaderSrc);
        var shaderModule = memAllocLong(1);
        vkCreateShaderModule(device, shaderModuleInfo, null, shaderModule);
        return shaderModule.get(0);
    }

    //
    public DeviceObj freeCommandsByQueueFamily(int queueFamilyIndex, int commandPoolIndex, ArrayList<VkCommandBuffer> commandBuffers) {
        var commandPool = this.getCommandPool(queueFamilyIndex, commandPoolIndex);
        commandBuffers.forEach((cmdBuf)->{
            vkFreeCommandBuffers(device, commandPool, cmdBuf);
        });
        return this;
    }

    //
    public DeviceObj freeCommandsByQueueGroup(int queueGroupIndex, int commandPoolIndex, ArrayList<VkCommandBuffer> commandBuffers) {
        return freeCommandsByQueueFamily(this.queueGroups.get(queueGroupIndex).queueFamilyIndex, commandPoolIndex, commandBuffers);
    }

    //
    public DeviceObj present(int queueGroupIndex, long SwapChain, long waitSemaphore, IntBuffer imageIndex) {
        var queueGroup = this.queueGroups.get(queueGroupIndex);
        vkQueuePresentKHR(this.getQueue(queueGroup.queueFamilyIndex, queueGroup.queueIndices.get(0)), VkPresentInfoKHR.calloc()
            .sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
            .pWaitSemaphores(waitSemaphore != 0 ? memAllocLong(1).put(0, waitSemaphore) : null)
            .pSwapchains(memAllocLong(1).put(0, SwapChain)).swapchainCount(1)
            .pImageIndices(imageIndex));
        return this;
    }

    // TODO: pre-compute queues in families
    private VkQueue getQueue(int queueFamilyIndex, int queueIndex) {
        var queue = PointerBuffer.allocateDirect(1);
        VK10.vkGetDeviceQueue(this.device, queueFamilyIndex, queueIndex, queue);
        return new VkQueue(queue.get(0), this.device);
    }

    //
    private long getCommandPool(int queueFamilyIndex, int commandPoolIndex) {
        return this.queueFamilies.get(queueFamilyIndex).orElse(null).cmdPool.get(commandPoolIndex);
    }

    //
    public DeviceObj resetCommandPool(int queueGroupIndex, int commandPoolIndex) {
        var queueGroup = this.queueGroups.get(queueGroupIndex);
        var queueFamily = this.queueFamilies.get(queueGroup.queueFamilyIndex).get();
        var commandPoolInfo = queueFamily.commandPoolInfo.get(commandPoolIndex);
        commandPoolInfo.cmdBufIndex = 0;
        commandPoolInfo.cmdBufCache.clear();
        commandPoolInfo.cmdBufBuffer = null;
        vkResetCommandPool(device, getCommandPool(queueGroup.queueFamilyIndex, commandPoolIndex), 0);
        return this;
    }

    // use it when a polling
     public boolean doPolling() {
        this.whenDone = new ArrayList(this.whenDone.stream().filter((F)->{ return F.apply(null) == VK_NOT_READY; }).toList());
        return this.whenDone.isEmpty();
    }

    public VkCommandBuffer writeCommand(VkCommandBuffer cmdBuf, Function<VkCommandBuffer, Integer> fn) {
        vkBeginCommandBuffer(cmdBuf, VkCommandBufferBeginInfo.calloc()
            .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
            .flags(VK_COMMAND_BUFFER_USAGE_SIMULTANEOUS_USE_BIT));
        fn.apply(cmdBuf);
        vkEndCommandBuffer(cmdBuf);
        return cmdBuf;
    }

    // you also needs `device.doPolling` or `device.waitFence`
    public static class FenceProcess {
        public SemaphoreObj timelineSemaphore = null;
        public Function<LongBuffer, Integer> deallocProcess = null;
        public Promise<Integer> promise = null; // for getting status
        public LongBuffer fence = null;
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
        LongBuffer fence_ = memAllocLong(1).put(0, 0L);
        //vkCreateFence(this.device, VkFenceCreateInfo.calloc().sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO).flags(VK_FENCE_CREATE_SIGNALED_BIT), null, fence_);
        var querySignalSemaphore = createTempSemaphore();
        var ref = new FenceProcess() {{
            fence = fence_;
            deallocProcess = (result)->{
                var fence_ = fence.get(0);
                status = querySignalSemaphore.getTimeline() <= (querySignalSemaphore.prevTimeline) ? VK_NOT_READY : VK_SUCCESS;
                if (status != VK_NOT_READY) {
                    backTempSemaphore(querySignalSemaphore);
                    promise.fulfill(status);
                }
                return status;
            };
            promise = new Promise();
        }};

        //
        this.whenDone.add(ref.deallocProcess);
        this.doPolling();

        //
        return ref;
    }

    // TODO: fence registry, and correctly wait by fence
     public int waitFence(FenceProcess process) {
        while (process.status == VK_NOT_READY) {
            this.doPolling();
        };
        return process.status;
    }

     //
     public VkCommandBuffer allocateCommand(int queueGroupIndex, int commandPoolIndex) {
        var maxCommandBufferCache = 8;
        var queueGroup = this.queueGroups.get(queueGroupIndex);
        var queueFamily = this.queueFamilies.get(queueGroup.queueFamilyIndex).get();
        var commandPoolInfo = queueFamily.commandPoolInfo.get(commandPoolIndex);
        if (commandPoolInfo.cmdBufCache == null || commandPoolInfo.cmdBufIndex >= commandPoolInfo.cmdBufCache.size()) {
            var commandPool = this.getCommandPool(queueGroup.queueFamilyIndex, commandPoolIndex);
            if (commandPoolInfo.cmdBufCache == null) {
                commandPoolInfo.cmdBufCache = new ArrayList<>(maxCommandBufferCache);
            }
            if (commandPoolInfo.cmdBufBuffer == null) {
                commandPoolInfo.cmdBufBuffer = memAllocPointer(maxCommandBufferCache);
            }
            vkAllocateCommandBuffers(this.device, VkCommandBufferAllocateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                .commandPool(commandPool)
                .commandBufferCount(maxCommandBufferCache), commandPoolInfo.cmdBufBuffer);
            commandPoolInfo.cmdBufCache.clear();
            for (var I=0;I<maxCommandBufferCache;I++) {
                commandPoolInfo.cmdBufCache.add(new VkCommandBuffer(commandPoolInfo.cmdBufBuffer.get(I), device));
            }
            commandPoolInfo.cmdBufIndex = 0;
        }
        return commandPoolInfo.cmdBufCache.get(commandPoolInfo.cmdBufIndex++);
    }

    //
     public FenceProcess submitCommand(SubmitCmd cmd) throws Exception {
        var signalSemaphoreSubmitInfo = (ArrayList<VkSemaphoreSubmitInfo>)(cmd.signalSemaphoreSubmitInfo != null ? cmd.signalSemaphoreSubmitInfo.clone() : new ArrayList<VkSemaphoreSubmitInfo>());
        var waitSemaphoreSubmitInfo = (ArrayList<VkSemaphoreSubmitInfo>)(cmd.waitSemaphoreSubmitInfo != null ? cmd.waitSemaphoreSubmitInfo.clone() : new ArrayList<VkSemaphoreSubmitInfo>());

        //
        var queueGroup = this.queueGroups.get(cmd.queueGroupIndex);
        var queueFamily = this.queueFamilies.get(queueGroup.queueFamilyIndex).get();

        // TODO: shared PTR and semaphore behaviour (and avoid bad memory)
        SemaphoreObj signalSemaphore = createTempSemaphore();
        VkSemaphoreSubmitInfo submitInfo = signalSemaphore.makeSubmissionTimeline(cmd.whatWaitBySemaphore);;
        signalSemaphoreSubmitInfo.add(submitInfo);

        //
        if (cmd.whatQueueGroupWillWait >= 0 && cmd.whatQueueGroupWillWait != cmd.queueGroupIndex) {
            var whatQueueGroup = queueGroups.get(cmd.whatQueueGroupWillWait);
            whatQueueGroup.queueIndices.forEach((idx)->{
                var whatQueueInfo = queueFamilies.get(whatQueueGroup.queueFamilyIndex).get().queueInfos.get(idx);
                whatQueueInfo.waitSemaphores.add(signalSemaphore);
                whatQueueInfo.waitSemaphoresInfo.add(submitInfo);
                signalSemaphore.incrementShared(); // don't delete too early
            });
        }

        // TODO: globalize loading of queue
        var lessBusyCount = Collections.min(queueGroup.queueBusy);
        var lessBusy = queueGroup.queueBusy.indexOf(lessBusyCount);
        var lessBusyQ = queueGroup.queueIndices.get(lessBusy);
        var queueInfo = queueFamily.queueInfos.get(lessBusyQ);

        //
        var querySignalSemaphore = createTempSemaphore();
        var toRemoveSemaphores = new ArrayList<SemaphoreObj>();
        toRemoveSemaphores.addAll(queueInfo.waitSemaphores);
        toRemoveSemaphores.add(querySignalSemaphore);
        signalSemaphoreSubmitInfo.add(querySignalSemaphore.makeSubmissionTimeline(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT));
        var signalSemaphores = VkSemaphoreSubmitInfo.calloc(signalSemaphoreSubmitInfo.size());
        for (var I=0;I<signalSemaphoreSubmitInfo.size();I++) {
            signalSemaphores.get(I).set(signalSemaphoreSubmitInfo.get(I));
        }
        queueInfo.waitSemaphores.clear();

        //
        waitSemaphoreSubmitInfo.addAll(queueInfo.waitSemaphoresInfo);
        var waitSemaphores = VkSemaphoreSubmitInfo.calloc(waitSemaphoreSubmitInfo.size());
        for (var I=0;I<waitSemaphoreSubmitInfo.size();I++) {
            waitSemaphores.get(I).set(waitSemaphoreSubmitInfo.get(I));
        }
        queueInfo.waitSemaphoresInfo.clear();

        //
        var cmdInfo = VkCommandBufferSubmitInfo.calloc(1);
        cmdInfo.get(0).sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_SUBMIT_INFO).commandBuffer(cmd.cmdBuf).deviceMask(0);

        //
        LongBuffer fence_ = memAllocLong(1).put(0, 0L);
        //vkCreateFence(this.device, VkFenceCreateInfo.calloc().sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO), null, fence_);
        vkQueueSubmit2(this.getQueue(queueGroup.queueFamilyIndex, lessBusyQ), VkSubmitInfo2.calloc(1)
            .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO_2)
            .pCommandBufferInfos(cmdInfo)
            .pSignalSemaphoreInfos(signalSemaphores)
            .pWaitSemaphoreInfos(waitSemaphores), fence_.get(0));

        //
        if (cmd.onDone == null) { cmd.onDone = new Promise(); };
        queueGroup.queueBusy.set(lessBusy, queueGroup.queueBusy.get(lessBusy)+1);

        //
        /*if (queueGroup.queueBusy.size() > 1) {
            System.out.println("Queue Group Offloading: ");
            for (var I = 0; I < queueGroup.queueBusy.size(); I++) {
                System.out.println(I + ": " + queueGroup.queueBusy.get(I));
            }
        }*/

        //
        var ref = new FenceProcess() {{
            fence = fence_;
            timelineSemaphore = querySignalSemaphore;
            deallocProcess = (_null_)->{
                var fence = fence_.get(0);
                //status = fence != 0 ? vkGetFenceStatus(device, fence) : VK_SUCCESS;
                status = querySignalSemaphore.getTimeline() <= (querySignalSemaphore.prevTimeline) ? VK_NOT_READY : VK_SUCCESS;
                if (status != VK_NOT_READY) {
                    toRemoveSemaphores.stream().forEach((semaphoreObj) -> {
                        backTempSemaphore(semaphoreObj);
                    });
                    queueGroup.queueBusy.set(lessBusy, queueGroup.queueBusy.get(lessBusy)-1);
                    //vkDestroyFence(device, fence, null); fence_.put(0, 0L);

                    // TODO: correctly handle a status
                    cmd.onDone.fulfill(status);
                }
                return status;
            };
            promise = cmd.onDone;
        }};

        //
        this.whenDone.add(ref.deallocProcess);
        this.doPolling();

        //
        return ref;
    }

    //
    public FenceProcess submitOnce(SubmitCmd submitCmd, Function<VkCommandBuffer, Integer> fn) throws Exception {
        vkBeginCommandBuffer(submitCmd.cmdBuf = this.allocateCommand(submitCmd.queueGroupIndex, submitCmd.commandPoolIndex), VkCommandBufferBeginInfo.calloc()
            .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
            .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT));
        fn.apply(submitCmd.cmdBuf);
        vkEndCommandBuffer(submitCmd.cmdBuf);

        //
        if (submitCmd.onDone == null) { submitCmd.onDone = new Promise(); };
        return submitCommand(submitCmd);
    }

}
