package org.hydra2s.manhack.objects;

//

import org.hydra2s.manhack.descriptors.DeviceCInfo;
import org.hydra2s.manhack.descriptors.DeviceCInfo.QueueFamilyCInfo;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static org.lwjgl.system.MemoryUtil.memAllocInt;
import static org.lwjgl.system.MemoryUtil.memAllocLong;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO;

//
public class DeviceObj extends BasicObj {

    public IntBuffer queueFamilyIndices = null;
    //
    protected IntBuffer layersAmount = memAllocInt(1).put(0, 0);

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
                MemoryUtil.memAddress(MemoryUtil.memASCII("VK_KHR_swapchain")),
                MemoryUtil.memAddress(MemoryUtil.memASCII("VK_KHR_deferred_host_operations")),
                MemoryUtil.memAddress(MemoryUtil.memASCII("VK_KHR_acceleration_structure")),
                MemoryUtil.memAddress(MemoryUtil.memASCII("VK_KHR_ray_query")),
                MemoryUtil.memAddress(MemoryUtil.memASCII("VK_EXT_conservative_rasterization")),
                MemoryUtil.memAddress(MemoryUtil.memASCII("VK_EXT_extended_dynamic_state3")),
                MemoryUtil.memAddress(MemoryUtil.memASCII("VK_EXT_robustness2")),
                MemoryUtil.memAddress(MemoryUtil.memASCII("VK_EXT_vertex_input_dynamic_state")),
                MemoryUtil.memAddress(MemoryUtil.memASCII("VK_EXT_descriptor_buffer")), // needs termination code here
                MemoryUtil.memAddress(MemoryUtil.memASCII("VK_EXT_multi_draw")),
                MemoryUtil.memAddress(MemoryUtil.memASCII("VK_KHR_fragment_shader_barycentric")),
                MemoryUtil.memAddress(MemoryUtil.memASCII("VK_EXT_mesh_shader")),
                MemoryUtil.memAddress(MemoryUtil.memASCII("VK_EXT_pipeline_robustness")),
                MemoryUtil.memAddress(MemoryUtil.memASCII("VK_EXT_shader_image_atomic_int64")),
                MemoryUtil.memAddress(MemoryUtil.memASCII("VK_EXT_shader_atomic_float")),
                MemoryUtil.memAddress(MemoryUtil.memASCII("VK_KHR_shader_clock")),
                MemoryUtil.memAddress(MemoryUtil.memASCII("VK_KHR_ray_tracing_maintenance1")),
                MemoryUtil.memAddress(MemoryUtil.memASCII("VK_KHR_workgroup_memory_explicit_layout")),
                MemoryUtil.memAddress(MemoryUtil.memASCII("VK_EXT_mutable_descriptor_type")),
                MemoryUtil.memAddress(MemoryUtil.memASCII("VK_EXT_transform_feedback")),
                MemoryUtil.memAddress(MemoryUtil.memASCII("VK_EXT_shader_atomic_float2")), // broken support in NVIDIA
                MemoryUtil.memAddress(MemoryUtil.memASCII("VK_EXT_memory_budget"))
        );

        //
        var deviceExtensions = extbuf.stream().filter((Ep) -> {
            Boolean found = false;
            for (int K = 0; K < physicalDeviceObj.extensions.capacity(); K++) {
                String X = MemoryUtil.memASCII(physicalDeviceObj.extensions.get(K).extensionName());
                String E = MemoryUtil.memASCII(Ep);
                if (X.contains(E)) {
                    found = true;
                    break;
                }
            }
            return found;
        }).toList();

        // Extensions
        this.extensions = PointerBuffer.allocateDirect(deviceExtensions.size());
        for (int I = 0; I < this.extensions.capacity(); I++) {
            this.extensions.put(I, deviceExtensions.get(I));
        }
        this.queueFamiliesCInfo = org.lwjgl.vulkan.VkDeviceQueueCreateInfo.create(cInfo.queueFamilies.size());
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
        var result = VK10.vkCreateDevice(physicalDeviceObj.physicalDevice, this.deviceInfo = VkDeviceCreateInfo.create(1)
                .pNext(/*physicalDeviceObj.deviceFeatures*/0)
                .sType(VK10.VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                .pQueueCreateInfos(this.queueFamiliesCInfo)
                .ppEnabledExtensionNames(this.extensions)
                .ppEnabledLayerNames(this.layers)
                .get(), null, (this.handle = new Handle("Device")).ptr());
        BasicObj.globalHandleMap.put(this.handle.get(), this);

        //
        this.device = new VkDevice(this.handle.get(), physicalDeviceObj.physicalDevice, this.deviceInfo);

        //
        for (int Q = 0; Q < cInfo.queueFamilies.size(); Q++) {
            var qfi = this.queueFamilyIndices.get(Q);
            VK10.vkCreateCommandPool(this.device, org.lwjgl.vulkan.VkCommandPoolCreateInfo.create().sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO).queueFamilyIndex(qfi), null, this.queueFamilies.get(qfi).cmdPool);
        }
    }

    //
    public DeviceObj(Handle base, Handle handle) {
        super(base, handle);
    }

    //
    public static long[] unboxed(final Long[] array) {
        return Arrays.stream(array)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .toArray();
    }

    //
    public static class QueueFamily {
        int index = 0;
        LongBuffer cmdPool = memAllocLong(1).put(0, 0);
        QueueFamilyCInfo cInfo = new QueueFamilyCInfo();
    }

    // TODO: pre-compute queues in families
    PointerBuffer getQueue(int queueFamilyIndex, int queueIndex) {
        var queue = PointerBuffer.allocateDirect(1);
        VK10.vkGetDeviceQueue(this.device, queueFamilyIndex, queueIndex, queue);
        return queue;
    }

}
