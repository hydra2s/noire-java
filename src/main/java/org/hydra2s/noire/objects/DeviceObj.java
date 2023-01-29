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
    public ArrayList<Function<SemaphoreObj, Integer>> whenDone = new ArrayList<Function<SemaphoreObj, Integer>>();

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
    public static class QueueInfo {
        public ArrayList<VkSemaphoreSubmitInfo> waitSemaphoresInfo = null;
        public ArrayList<SemaphoreObj> waitSemaphores = null;
    };

    // TODO: replace by queue group instead of queue family
    // TODO: add queue indices support
    // TODO: add timeline semaphore support
    public static class QueueFamily {
        public int index = 0;
        public LongBuffer cmdPool = memAllocLong(1).put(0, 0);
        public QueueFamilyCInfo cInfo = new QueueFamilyCInfo();

        //
        public ArrayList<VkCommandBuffer> cmdBufCache = null;
        public PointerBuffer cmdBufBuffer = null;
        public int cmdBufIndex = 0;

        //
        public ArrayList<QueueInfo> queueInfos = null;
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
        this.queueFamiliesCInfo = org.lwjgl.vulkan.VkDeviceQueueCreateInfo.calloc(cInfo.queueFamilies.size());
        this.queueFamilyIndices = memAllocInt(cInfo.queueFamilies.size());

        //
        var Qs = cInfo.queueFamilies.size();
        for (var Q = 0; Q < Qs; Q++) {
            this.queueFamiliesCInfo.get(Q).sType(VK10.VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO);

            //
            var queuePriorities = memAllocFloat(cInfo.queueFamilies.get(Q).priorities.length);
            var Qp = queuePriorities.remaining();
            for (var I=0;I<cInfo.queueFamilies.get(Q).priorities.length;I++) {
                queuePriorities.put(I, cInfo.queueFamilies.get(Q).priorities[I]);
            }

            //
            this.queueFamiliesCInfo.get(Q).pQueuePriorities(queuePriorities);
            this.queueFamiliesCInfo.get(Q).queueFamilyIndex(cInfo.queueFamilies.get(Q).index);

            //
            var qf = new QueueFamily();
            qf.index = cInfo.queueFamilies.get(Q).index;
            qf.cInfo = cInfo.queueFamilies.get(Q);
            qf.queueInfos = new ArrayList<>();

            //
            var Ps = cInfo.queueFamilies.get(Q).priorities.length;
            for (var I=0;I<Ps;I++) {
                qf.queueInfos.add(new QueueInfo(){{
                    waitSemaphoresInfo = new ArrayList<>();
                    waitSemaphores = new ArrayList<>();
                }});
            }

            //
            this.queueFamilies.put$(cInfo.queueFamilies.get(Q).index, qf);
            this.queueFamilyIndices.put(Q, qf.index);
        }

        //
        this.queueGroups = cInfo.queueGroups;

        //
        if (this.queueGroups == null) {
            this.queueGroups = new ArrayList<>();
            this.queueFamilies.forEach((index, qF)->{
                var group = new DeviceCInfo.QueueGroup();
                group.queueIndices = new ArrayList<>();
                for (var I=0;I<qF.get().queueInfos.size();I++) {
                    group.queueIndices.add(I);
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
            VK10.vkCreateCommandPool(this.device, org.lwjgl.vulkan.VkCommandPoolCreateInfo.calloc().sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO).queueFamilyIndex(qfi), null, this.queueFamilies.get(qfi).orElse(null).cmdPool);
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
    public DeviceObj freeCommandsByQueueFamily(int queueFamilyIndex, ArrayList<VkCommandBuffer> commandBuffers) {
        var commandPool = this.getCommandPool(queueFamilyIndex);
        commandBuffers.forEach((cmdBuf)->{
            vkFreeCommandBuffers(device, commandPool, cmdBuf);
        });
        return this;
    }

    //
    public DeviceObj freeCommandsByQueueGroup(int queueGroupIndex, ArrayList<VkCommandBuffer> commandBuffers) {
        return freeCommandsByQueueFamily(this.queueGroups.get(queueGroupIndex).queueFamilyIndex, commandBuffers);
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
    private long getCommandPool(int queueFamilyIndex) {
        return this.queueFamilies.get(queueFamilyIndex).orElse(null).cmdPool.get(0);
    }

    // use it when a polling
    public boolean doPolling() {
        var _list = (ArrayList<Function<LongBuffer, Integer>>)this.whenDone.clone();
        _list.stream().forEach((F)-> {if(F!=null) F.apply(null);});

        // if queue list is overflow, do await before free less than 1024
        // i.e. do intermission for free resources, and avoid overflow
        // TODO: manual queue intermission
        /*while (whenDone.size() >= 1024) {
            _list = (ArrayList<Function<LongBuffer, Integer>>)this.whenDone.clone();
            _list.stream().forEach((F)->F.apply(null));
        }*/
        //for (var I=0;I<_list.size();I++) {var F =_list.get(I);}
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
        public Function<SemaphoreObj, Integer> deallocProcess = null;
        public Promise<Integer> promise = null; // for getting status
    };

    // for beginning of rendering
    public FenceProcess makeSignaled() {
        var querySignalSemaphore = new SemaphoreObj(this.getHandle(), new SemaphoreCInfo(){{
            initialValue = 0;
        }});

        var ref = new FenceProcess() {{
            deallocProcess = (result)->{
                int status = querySignalSemaphore.getTimeline() == 0 ? VK_NOT_READY : VK_SUCCESS;
                if (status != VK_NOT_READY) {
                    whenDone.remove(deallocProcess);
                    try {
                        querySignalSemaphore.deleteDirectly();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    promise.fulfill(status);
                }
                return status;
            };
            promise = new Promise();
        }};

        //
        whenDone.add(ref.deallocProcess);

        //
        querySignalSemaphore.signalTimeline(2L);

        //
        this.doPolling();

        //
        return ref;
    }

    //
    public FenceProcess submitCommand(BasicCInfo.SubmitCmd cmd) {
        var signalSemaphoreSubmitInfo = (ArrayList<VkSemaphoreSubmitInfo>)(cmd.signalSemaphoreSubmitInfo != null ? cmd.signalSemaphoreSubmitInfo.clone() : new ArrayList<VkSemaphoreSubmitInfo>());
        var waitSemaphoreSubmitInfo = (ArrayList<VkSemaphoreSubmitInfo>)(cmd.waitSemaphoreSubmitInfo != null ? cmd.waitSemaphoreSubmitInfo.clone() : new ArrayList<VkSemaphoreSubmitInfo>());

        //
        if (cmd.whatQueueGroupWillWait >= 0) {
            //
            var signalSemaphore = new SemaphoreObj(this.getHandle(), new SemaphoreCInfo(){{

            }});

            //
            var submitInfo = signalSemaphore.makeSubmissionTimeline(cmd.whatWaitBySemaphore, cmd.whatValueBySemaphore);

            //
            signalSemaphoreSubmitInfo.add(submitInfo);

            //
            var queueGroup = queueGroups.get(cmd.whatQueueGroupWillWait);
            queueGroup.queueIndices.forEach((idx)->{
                var queueInfo = queueFamilies.get(queueGroup.queueFamilyIndex).get().queueInfos.get(idx);
                queueInfo.waitSemaphores.add(signalSemaphore);
                queueInfo.waitSemaphoresInfo.add(submitInfo);
            });
        }

        //
        var queueGroup = this.queueGroups.get(cmd.queueGroupIndex);
        var queueFamily = this.queueFamilies.get(queueGroup.queueFamilyIndex).get();

        // TODO: globalize loading of queue
        var lessBusyCount = Collections.min(queueGroup.queueBusy);
        var lessBusy = queueGroup.queueBusy.indexOf(lessBusyCount);
        var lessBusyQ = queueGroup.queueIndices.get(lessBusy);
        var queueInfo = queueFamily.queueInfos.get(lessBusyQ);

        //
        var querySignalSemaphore = new SemaphoreObj(this.getHandle(), new SemaphoreCInfo(){{
            initialValue = 0;
        }});

        //
        var toRemoveSemaphores = (ArrayList<SemaphoreObj>)queueInfo.waitSemaphores.clone();
        toRemoveSemaphores.add(querySignalSemaphore);

        //
        signalSemaphoreSubmitInfo.add(querySignalSemaphore.makeSubmissionTimeline(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT, 2));
        waitSemaphoreSubmitInfo.addAll(queueInfo.waitSemaphoresInfo);

        //
        queueInfo.waitSemaphoresInfo.clear();
        queueGroup.queueBusy.set(lessBusy, queueGroup.queueBusy.get(lessBusy)+1);

        //
        var cmdInfo = VkCommandBufferSubmitInfo.calloc(1);
        var signalSemaphores = VkSemaphoreSubmitInfo.calloc(signalSemaphoreSubmitInfo.size());
        var waitSemaphores = VkSemaphoreSubmitInfo.calloc(waitSemaphoreSubmitInfo.size());

        //
        for (var I=0;I<signalSemaphoreSubmitInfo.size();I++) {
            signalSemaphores.get(I).set(signalSemaphoreSubmitInfo.get(I));
        }

        //
        for (var I=0;I<waitSemaphoreSubmitInfo.size();I++) {
            waitSemaphores.get(I).set(waitSemaphoreSubmitInfo.get(I));
        }

        //
        cmdInfo.get(0).commandBuffer(cmd.cmdBuf).deviceMask(0);

        //
        //LongBuffer fence_ = memAllocLong(1);
        //vkCreateFence(this.device, VkFenceCreateInfo.calloc().sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO), null, fence_);
        vkQueueSubmit2(this.getQueue(queueGroup.queueFamilyIndex, lessBusyQ), VkSubmitInfo2.calloc(1)
            .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO_2)
            .pCommandBufferInfos(cmdInfo)
            .pSignalSemaphoreInfos(signalSemaphores)
            .pWaitSemaphoreInfos(waitSemaphores), 0);

        //
        if (cmd.onDone == null) { cmd.onDone = new Promise(); };

        //
        var ref = new FenceProcess() {{
            timelineSemaphore = querySignalSemaphore;
            deallocProcess = null;
            promise = cmd.onDone;
        }};

        //
        this.whenDone.add(ref.deallocProcess = (_null_)->{
            // TODO: correctly handle a status
            int status = querySignalSemaphore.getTimeline() == 0 ? VK_NOT_READY : VK_SUCCESS;

            //
            if (status != VK_NOT_READY) {
                this.whenDone.remove(ref.deallocProcess);

                //
                toRemoveSemaphores.stream().forEach((semaphoreObj) -> {
                    try {
                        semaphoreObj.deleteDirectly();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

                //
                queueGroup.queueBusy.set(lessBusy, queueGroup.queueBusy.get(lessBusy) - 1);

                // TODO: correctly handle a status
                cmd.onDone.fulfill(status);
            }
            return status;
            //return VK_SUCCESS;
        });

        //
        this.doPolling();

        //
        return ref;
    }

    // TODO: fence registry, and correctly wait by fence
    public int waitFence(FenceProcess process) {
        var status = VK_NOT_READY;
        while((status = process.deallocProcess.apply(process.timelineSemaphore)) == VK_NOT_READY) {
            this.doPolling();
        };
        return status;
    }

    // TODO: support multiple commands
    public VkCommandBuffer allocateCommand(int queueGroupIndex) {
        var maxCommandBufferCache = 8;
        var queueGroup = this.queueGroups.get(queueGroupIndex);
        var queueFamily = this.queueFamilies.get(queueGroup.queueFamilyIndex).get();
        if (queueFamily.cmdBufCache == null || queueFamily.cmdBufIndex >= queueFamily.cmdBufCache.size()) {
            var commandPool = this.getCommandPool(queueGroup.queueFamilyIndex);
            if (queueFamily.cmdBufCache == null) {
                queueFamily.cmdBufBuffer = memAllocPointer(maxCommandBufferCache);
                queueFamily.cmdBufCache = new ArrayList<>(maxCommandBufferCache);
            }
            vkAllocateCommandBuffers(this.device, VkCommandBufferAllocateInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                .commandPool(commandPool)
                .commandBufferCount(maxCommandBufferCache), queueFamily.cmdBufBuffer);
            queueFamily.cmdBufCache.clear();
            for (var I=0;I<maxCommandBufferCache;I++) {
                queueFamily.cmdBufCache.add(new VkCommandBuffer(queueFamily.cmdBufBuffer.get(I), device));
            }
            queueFamily.cmdBufIndex = 0;
        }
        return queueFamily.cmdBufCache.get(queueFamily.cmdBufIndex++);
    }

    //
    public FenceProcess submitOnce(BasicCInfo.SubmitCmd submitCmd, Function<VkCommandBuffer, Integer> fn) {
        var queueGroup = this.queueGroups.get(submitCmd.queueGroupIndex);
        var commandPool = this.getCommandPool(queueGroup.queueFamilyIndex);
        vkBeginCommandBuffer(submitCmd.cmdBuf = this.allocateCommand(submitCmd.queueGroupIndex), VkCommandBufferBeginInfo.calloc()
            .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
            .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT));
        fn.apply(submitCmd.cmdBuf);
        vkEndCommandBuffer(submitCmd.cmdBuf);

        //
        if (submitCmd.onDone == null) { submitCmd.onDone = new Promise(); };
        submitCmd.onDone.thenApply((status)->{
            vkFreeCommandBuffers(this.device, commandPool, submitCmd.cmdBuf);
            return status;
        });

        //
        return submitCommand(submitCmd);
    }

}
