package org.hydra2s.manhack.objects;

//
import org.hydra2s.manhack.descriptors.DeviceCInfo;
import org.hydra2s.manhack.descriptors.DeviceCInfo.QueueFamilyCInfo;

//
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

//
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VK10;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkDeviceCreateInfo;
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo;
import org.lwjgl.vulkan.VkExtensionProperties;
import org.lwjgl.vulkan.VkLayerProperties;

//
public class DeviceObj extends BasicObj {

    //
    protected IntBuffer layersAmount = IntBuffer.wrap(new int[]{0});
    protected IntBuffer extensionAmount = IntBuffer.wrap(new int[]{0});

    //
    public PointerBuffer layers = null;
    public PointerBuffer extensions = null;

    //
    public VkLayerProperties.Buffer availableLayers = null;
    public VkExtensionProperties.Buffer availableExtensions = null;
    public VkDeviceCreateInfo deviceInfo = null;
    public VkDevice device = null;
    public VkDeviceQueueCreateInfo.Buffer queueFamiliesCInfo = null;

    //
    public static class QueueFamily {
        int index = 0;
        LongBuffer cmdPool = LongBuffer.wrap(new long[]{0});
        QueueFamilyCInfo cInfo = new QueueFamilyCInfo();
    }

    //
    public HashMap<Integer, QueueFamily> queueFamilies = new HashMap<Integer, QueueFamily>();
    public IntBuffer queueFamilyIndices = IntBuffer.wrap(new int[]{});

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
    public DeviceObj(Handle base, DeviceCInfo cInfo) {
        super(base, cInfo);

        //
        var physicalDeviceObj = (PhysicalDeviceObj)BasicObj.globalHandleMap.get(base.get());
        List<Long> extbuf = Arrays.asList(new Long[]{
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
        });

        //
        var deviceExtensions = extbuf.stream().filter((E)->{
            Boolean found = false;
            for (int K=0;K<physicalDeviceObj.extensions.capacity();K++) {
                String X = physicalDeviceObj.extensions.get(K).extensionName().toString();
                if (X.compareTo(MemoryUtil.memASCII(E)) >= 0) { found = true; break; };
            }
            return found;
        });

        // Extensions
        this.extensions = PointerBuffer.create(MemoryUtil.memAddress(LongBuffer.wrap(unboxed((Long[])deviceExtensions.toArray()))), extbuf.size());
        this.queueFamiliesCInfo = org.lwjgl.vulkan.VkDeviceQueueCreateInfo.create(cInfo.queueFamilies.length);
        this.queueFamilyIndices = IntBuffer.allocate(cInfo.queueFamilies.length);

        //
        for (int Q=0;Q<cInfo.queueFamilies.length;Q++) {
            this.queueFamiliesCInfo.get(Q).pQueuePriorities(FloatBuffer.wrap(cInfo.queueFamilies[Q].priorities));
            this.queueFamiliesCInfo.get(Q).queueFamilyIndex(cInfo.queueFamilies[Q].index);

            //
            var qf = new QueueFamily();
            qf.index = cInfo.queueFamilies[Q].index;
            qf.cInfo = cInfo.queueFamilies[Q];
            this.queueFamilies.put(cInfo.queueFamilies[Q].index, qf);
            this.queueFamilyIndices.put(Q, qf.index);
        }

        // TODO: Handle VkResult!!
        VK10.vkCreateDevice(physicalDeviceObj.physicalDevice, this.deviceInfo = VkDeviceCreateInfo.create(1)
            .pQueueCreateInfos(this.queueFamiliesCInfo)
            .ppEnabledExtensionNames(this.extensions)
            .ppEnabledLayerNames(this.layers).get(), null, (this.handle = new Handle(0)).ptr());
        this.device = new VkDevice(this.handle.get(), physicalDeviceObj.physicalDevice, this.deviceInfo);
        BasicObj.globalHandleMap.put(this.handle.get(), this);

        //
        for (int Q=0;Q<cInfo.queueFamilies.length;Q++) {
            var qfi = this.queueFamilyIndices.get(Q);
            VK10.vkCreateCommandPool(this.device, org.lwjgl.vulkan.VkCommandPoolCreateInfo.create().queueFamilyIndex(qfi), null, this.queueFamilies.get(qfi).cmdPool);
        }
    }

    // TODO: pre-compute queues in families
    PointerBuffer getQueue(int queueFamilyIndex, int queueIndex) {
        var queue = PointerBuffer.allocateDirect(1);
        VK10.vkGetDeviceQueue(this.device, queueFamilyIndex, queueIndex, queue);
        return queue;
    }

}
