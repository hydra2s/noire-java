package org.hydra2s.noire.objects;

//

import org.hydra2s.noire.descriptors.BasicCInfo;
import org.hydra2s.noire.descriptors.DeviceCInfo;
import org.hydra2s.noire.descriptors.DeviceCInfo.QueueFamilyCInfo;
import org.hydra2s.utils.Promise;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.*;
import java.util.function.Function;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.VK10.*;

//
public class DeviceObj extends BasicObj {

    public IntBuffer queueFamilyIndices = null;
    //
    protected IntBuffer layersAmount = memAllocInt(1).put(0, 0);

    //
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
    public HashMap<Integer, QueueFamily> queueFamilies = new HashMap<Integer, QueueFamily>();


    //
    public DeviceObj(Handle base, DeviceCInfo cInfo) {
        super(base, cInfo);

        //
        var physicalDeviceObj = (PhysicalDeviceObj) BasicObj.globalHandleMap.get(base.get());
        List<Long> extbuf = Arrays.asList(
            MemoryUtil.memAddress(MemoryUtil.memUTF8("VK_KHR_swapchain")),
            MemoryUtil.memAddress(MemoryUtil.memUTF8("VK_KHR_deferred_host_operations")),
            MemoryUtil.memAddress(MemoryUtil.memUTF8("VK_KHR_acceleration_structure")),
            MemoryUtil.memAddress(MemoryUtil.memUTF8("VK_KHR_ray_query")),
            MemoryUtil.memAddress(MemoryUtil.memUTF8("VK_EXT_conservative_rasterization")),
            MemoryUtil.memAddress(MemoryUtil.memUTF8("VK_EXT_extended_dynamic_state3")),
            MemoryUtil.memAddress(MemoryUtil.memUTF8("VK_EXT_robustness2")),
            MemoryUtil.memAddress(MemoryUtil.memUTF8("VK_EXT_vertex_input_dynamic_state")),
            MemoryUtil.memAddress(MemoryUtil.memUTF8("VK_EXT_descriptor_buffer")), // needs termination code here
            MemoryUtil.memAddress(MemoryUtil.memUTF8("VK_EXT_multi_draw")),
            MemoryUtil.memAddress(MemoryUtil.memUTF8("VK_KHR_fragment_shader_barycentric")),
            MemoryUtil.memAddress(MemoryUtil.memUTF8("VK_EXT_mesh_shader")),
            MemoryUtil.memAddress(MemoryUtil.memUTF8("VK_EXT_pipeline_robustness")),
            MemoryUtil.memAddress(MemoryUtil.memUTF8("VK_EXT_shader_image_atomic_int64")),
            MemoryUtil.memAddress(MemoryUtil.memUTF8("VK_EXT_shader_atomic_float")),
            MemoryUtil.memAddress(MemoryUtil.memUTF8("VK_KHR_shader_clock")),
            MemoryUtil.memAddress(MemoryUtil.memUTF8("VK_KHR_ray_tracing_maintenance1")),
            MemoryUtil.memAddress(MemoryUtil.memUTF8("VK_KHR_workgroup_memory_explicit_layout")),
            MemoryUtil.memAddress(MemoryUtil.memUTF8("VK_EXT_mutable_descriptor_type")),
            MemoryUtil.memAddress(MemoryUtil.memUTF8("VK_EXT_transform_feedback")),
            MemoryUtil.memAddress(MemoryUtil.memUTF8("VK_EXT_shader_atomic_float2")), // broken support in NVIDIA
            MemoryUtil.memAddress(MemoryUtil.memUTF8("VK_EXT_memory_budget")),
            MemoryUtil.memAddress(MemoryUtil.memUTF8("VK_EXT_image_2d_view_of_3d")),
            MemoryUtil.memAddress(MemoryUtil.memUTF8("VK_EXT_index_type_uint8")),
            MemoryUtil.memAddress(MemoryUtil.memUTF8("VK_VALVE_mutable_descriptor_type")),

            MemoryUtil.memAddress(MemoryUtil.memUTF8("VK_KHR_external_semaphore")),
            MemoryUtil.memAddress(MemoryUtil.memUTF8("VK_KHR_external_semaphore_win32")),
            MemoryUtil.memAddress(MemoryUtil.memUTF8("VK_KHR_external_semaphore_fd")),
            MemoryUtil.memAddress(MemoryUtil.memUTF8("VK_KHR_external_memory")),
            MemoryUtil.memAddress(MemoryUtil.memUTF8("VK_KHR_external_memory_win32")),
            MemoryUtil.memAddress(MemoryUtil.memUTF8("VK_KHR_external_memory_fd"))
        );

        //
        var deviceExtensions = (List<Long>)(extbuf.stream().filter((Ep) -> {
            Boolean found = false;
            for (int K = 0; K < physicalDeviceObj.extensions.remaining(); K++) {
                String X = MemoryUtil.memUTF8(physicalDeviceObj.extensions.get(K).extensionName());
                String E = MemoryUtil.memUTF8(Ep);
                if (X.contains(E)) {
                    found = true;
                    break;
                }
            }
            return found;
        }).toList());

        // Extensions
        this.extensions = PointerBuffer.allocateDirect(deviceExtensions.size());
        for (int I = 0; I < this.extensions.remaining(); I++) {
            this.extensions.put(I, deviceExtensions.get(I));
        }
        this.queueFamiliesCInfo = org.lwjgl.vulkan.VkDeviceQueueCreateInfo.calloc(cInfo.queueFamilies.size());
        this.queueFamilyIndices = memAllocInt(cInfo.queueFamilies.size());

        //
        for (int Q = 0; Q < cInfo.queueFamilies.size(); Q++) {
            this.queueFamiliesCInfo.get(Q).sType(VK10.VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO);
            this.queueFamiliesCInfo.get(Q).pQueuePriorities(MemoryUtil.memAllocFloat(1).put(0, 1.0f));
            this.queueFamiliesCInfo.get(Q).queueFamilyIndex(cInfo.queueFamilies.get(Q).index);

            //
            var qf = new QueueFamily();
            qf.index = cInfo.queueFamilies.get(Q).index;
            qf.cInfo = cInfo.queueFamilies.get(Q);
            this.queueFamilies.put(cInfo.queueFamilies.get(Q).index, qf);
            this.queueFamilyIndices.put(Q, qf.index);
        }

        // TODO: Handle VkResult!!
        var result = VK10.vkCreateDevice(physicalDeviceObj.physicalDevice, this.deviceInfo = VkDeviceCreateInfo.calloc()
                .pNext(physicalDeviceObj.deviceFeatures.address())
                .sType(VK10.VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                .pQueueCreateInfos(this.queueFamiliesCInfo)
                .ppEnabledExtensionNames(this.extensions)
                .ppEnabledLayerNames(this.layers)
                , null, (this.handle = new Handle("Device")).ptr());
        BasicObj.globalHandleMap.put(this.handle.get(), this);

        //
        this.device = new VkDevice(this.handle.get(), physicalDeviceObj.physicalDevice, this.deviceInfo);

        //
        for (int Q = 0; Q < cInfo.queueFamilies.size(); Q++) {
            var qfi = this.queueFamilyIndices.get(Q);
            VK10.vkCreateCommandPool(this.device, org.lwjgl.vulkan.VkCommandPoolCreateInfo.calloc().sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO).queueFamilyIndex(qfi), null, this.queueFamilies.get(qfi).cmdPool);
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
    public static class QueueFamily {
        int index = 0;
        LongBuffer cmdPool = memAllocLong(1).put(0, 0);
        QueueFamilyCInfo cInfo = new QueueFamilyCInfo();
    }

    // TODO: pre-compute queues in families
    public VkQueue getQueue(int queueFamilyIndex, int queueIndex) {
        var queue = PointerBuffer.allocateDirect(1);
        VK10.vkGetDeviceQueue(this.device, queueFamilyIndex, queueIndex, queue);
        return new VkQueue(queue.get(0), this.device);
    }

    //
    public long getCommandPool(int queueFamilyIndex) {
        return this.queueFamilies.get(queueFamilyIndex).cmdPool.get(0);
    }

    // use it when a polling
    public DeviceObj doPolling() {
        var _list = (ArrayList<Function<LongBuffer, Integer>>)this.whenDone.clone();
        _list.stream().forEach((F)->F.apply(null));
        //for (var I=0;I<_list.size();I++) {var F =_list.get(I);}
        return this;
    }

    public VkCommandBuffer writeCommand(VkCommandBuffer cmdBuf, Function<VkCommandBuffer, Integer> fn) {
        vkBeginCommandBuffer(cmdBuf, VkCommandBufferBeginInfo.calloc()
            .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
            .flags(VK_COMMAND_BUFFER_USAGE_SIMULTANEOUS_USE_BIT));
        fn.apply(cmdBuf);
        vkEndCommandBuffer(cmdBuf);
        return cmdBuf;
    }

    //
    public LongBuffer submitCommand(BasicCInfo.SubmitCmd cmd) {
        LongBuffer fence = memAllocLong(1);
        vkCreateFence(this.device, VkFenceCreateInfo.calloc().sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO), null, fence);
        vkQueueSubmit(cmd.queue, VkSubmitInfo.calloc(1)
            .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
            .pCommandBuffers(memAllocPointer(1).put(0, cmd.cmdBuf.address()))
            .pWaitDstStageMask(cmd.dstStageMask)
            .pSignalSemaphores(cmd.signalSemaphores)
            .pWaitSemaphores(cmd.waitSemaphores)
            .waitSemaphoreCount(cmd.waitSemaphores != null ? cmd.waitSemaphores.remaining() : 0), fence.get(0));

        //
        var ref = new Object() { Function<LongBuffer, Integer> deallocProcess = null; };
        if (cmd.onDone == null) { cmd.onDone = new Promise(); };
        this.whenDone.add(ref.deallocProcess = (_null_)->{
            int status = vkGetFenceStatus(this.device, fence.get(0));
            if (status != VK_NOT_READY) {
                vkDestroyFence(this.device, fence.get(0), null); fence.put(0, 0);
                this.whenDone.remove(ref.deallocProcess);
                cmd.onDone.fulfill(status);
                return status;
            }
            return status;
        });

        //
        return fence;
    }

    //
    public VkCommandBuffer allocateCommand(long commandPool) {
        PointerBuffer commands = memAllocPointer(1);
        vkAllocateCommandBuffers(this.device, VkCommandBufferAllocateInfo.calloc()
            .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
            .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
            .commandPool(commandPool)
            .commandBufferCount(1), commands);
        var cmdBuf = new VkCommandBuffer(commands.get(0), this.device);
        return cmdBuf;
    }

    //
    public LongBuffer submitOnce(long commandPool, BasicCInfo.SubmitCmd submitCmd, Function<VkCommandBuffer, Integer> fn) {
        //
        vkBeginCommandBuffer(submitCmd.cmdBuf = this.allocateCommand(commandPool), VkCommandBufferBeginInfo.calloc()
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
