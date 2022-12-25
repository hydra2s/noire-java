package org.hydra2s.noire.objects;

//
import org.hydra2s.noire.descriptors.BasicCInfo;
import org.lwjgl.vulkan.*;

//
import java.nio.IntBuffer;

//
import static org.lwjgl.system.MemoryUtil.memAllocInt;
import static org.lwjgl.vulkan.EXTDescriptorBuffer.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DESCRIPTOR_BUFFER_FEATURES_EXT;
import static org.lwjgl.vulkan.EXTImage2dViewOf3d.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_IMAGE_2D_VIEW_OF_3D_FEATURES_EXT;
import static org.lwjgl.vulkan.EXTMemoryBudget.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MEMORY_BUDGET_PROPERTIES_EXT;
import static org.lwjgl.vulkan.EXTMeshShader.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MESH_SHADER_FEATURES_EXT;
import static org.lwjgl.vulkan.EXTMultiDraw.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MULTI_DRAW_FEATURES_EXT;
import static org.lwjgl.vulkan.EXTMutableDescriptorType.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MUTABLE_DESCRIPTOR_TYPE_FEATURES_EXT;
import static org.lwjgl.vulkan.EXTPipelineRobustness.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PIPELINE_ROBUSTNESS_FEATURES_EXT;
import static org.lwjgl.vulkan.EXTRobustness2.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_ROBUSTNESS_2_FEATURES_EXT;
import static org.lwjgl.vulkan.EXTShaderAtomicFloat.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SHADER_ATOMIC_FLOAT_FEATURES_EXT;
import static org.lwjgl.vulkan.EXTShaderAtomicFloat2.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SHADER_ATOMIC_FLOAT_2_FEATURES_EXT;
import static org.lwjgl.vulkan.EXTShaderImageAtomicInt64.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SHADER_IMAGE_ATOMIC_INT64_FEATURES_EXT;
import static org.lwjgl.vulkan.EXTTransformFeedback.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_TRANSFORM_FEEDBACK_FEATURES_EXT;
import static org.lwjgl.vulkan.EXTVertexInputDynamicState.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VERTEX_INPUT_DYNAMIC_STATE_FEATURES_EXT;
import static org.lwjgl.vulkan.KHRAccelerationStructure.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_ACCELERATION_STRUCTURE_FEATURES_KHR;
import static org.lwjgl.vulkan.KHRFragmentShaderBarycentric.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FRAGMENT_SHADER_BARYCENTRIC_FEATURES_KHR;
import static org.lwjgl.vulkan.KHRRayQuery.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_RAY_QUERY_FEATURES_KHR;
import static org.lwjgl.vulkan.KHRRayTracingMaintenance1.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_RAY_TRACING_MAINTENANCE_1_FEATURES_KHR;
import static org.lwjgl.vulkan.KHRShaderClock.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SHADER_CLOCK_FEATURES_KHR;
import static org.lwjgl.vulkan.KHRWorkgroupMemoryExplicitLayout.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_WORKGROUP_MEMORY_EXPLICIT_LAYOUT_FEATURES_KHR;
import static org.lwjgl.vulkan.VK11.*;
import static org.lwjgl.vulkan.VK12.*;
import static org.lwjgl.vulkan.VK13.VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_3;
import static org.lwjgl.vulkan.VK13.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_3_FEATURES;

//
public class PhysicalDeviceObj extends BasicObj {

    //
    public VkPhysicalDeviceImage2DViewOf3DFeaturesEXT device2DViewOf3DFeatures = null;
    public VkPhysicalDeviceTransformFeedbackFeaturesEXT deviceTransformFeedbackFeatures = null;
    public VkPhysicalDeviceMutableDescriptorTypeFeaturesEXT deviceMutableDescriptorFeatures = null;
    public VkPhysicalDeviceWorkgroupMemoryExplicitLayoutFeaturesKHR deviceWorkgroupMemoryExplicitFeatures = null;
    public VkPhysicalDeviceRayTracingMaintenance1FeaturesKHR deviceRayTracingMaintenance1Features = null;
    public VkPhysicalDeviceShaderClockFeaturesKHR deviceShaderClockFeatures = null;
    public VkPhysicalDeviceShaderImageAtomicInt64FeaturesEXT deviceImageAtomicInt64Features = null;
    public VkPhysicalDeviceRobustness2FeaturesEXT deviceRobustness2Features = null;
    public VkPhysicalDeviceVertexInputDynamicStateFeaturesEXT deviceVertexInputFeatures = null;
    public VkPhysicalDeviceDescriptorBufferFeaturesEXT deviceDescriptorBufferFeatures = null;
    public VkPhysicalDeviceMeshShaderFeaturesEXT deviceMeshShaderFeatures = null;
    public VkPhysicalDeviceFragmentShaderBarycentricFeaturesKHR deviceBarycentricFeatures = null;
    public VkPhysicalDeviceMultiDrawFeaturesEXT deviceMultiDrawFeatures = null;
    public VkPhysicalDevicePipelineRobustnessFeaturesEXT devicePipelineRobustnessFeatures = null;
    public VkPhysicalDeviceShaderAtomicFloatFeaturesEXT deviceAtomicFloatFeatures = null;
    public VkPhysicalDeviceShaderAtomicFloat2FeaturesEXT deviceAtomicFloat2Features = null;
    public VkPhysicalDeviceRayQueryFeaturesKHR deviceRayQueryFeatures = null;
    public VkPhysicalDeviceAccelerationStructureFeaturesKHR deviceAccelerationStructureFeaturs = null;
    public VkPhysicalDeviceVulkan11Features deviceFeatures11 = null;
    public VkPhysicalDeviceVulkan12Features deviceFeatures12 = null;
    public VkPhysicalDeviceVulkan13Features deviceFeatures13 = null;
    public VkPhysicalDeviceFeatures2 deviceFeatures = null;
    public VkPhysicalDeviceDescriptorBufferPropertiesEXT deviceDescriptorBufferProperties = null;
    public VkPhysicalDeviceProperties2 deviceProperties = null;
    public VkQueueFamilyProperties.Buffer queueFamilyProperties = null;


    //
    public VkLayerProperties.Buffer layers = null;
    public VkExtensionProperties.Buffer extensions = null;
    public VkPhysicalDevice physicalDevice = null;

    //
    protected IntBuffer queueFamilyCount;
    protected IntBuffer extensionCount;

    //
    public PhysicalDeviceObj(Handle base, Handle handle) {
        super(base, handle);

        //
        var instanceObj = (InstanceObj) BasicObj.globalHandleMap.get(base.get());
        this.physicalDevice = new VkPhysicalDevice(handle.get(), instanceObj.instance);
        BasicObj.globalHandleMap.put(handle.get(), this);

        // TODO: unify into one object
        this.device2DViewOf3DFeatures = VkPhysicalDeviceImage2DViewOf3DFeaturesEXT.create().sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_IMAGE_2D_VIEW_OF_3D_FEATURES_EXT);
        this.deviceTransformFeedbackFeatures = VkPhysicalDeviceTransformFeedbackFeaturesEXT.create().sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_TRANSFORM_FEEDBACK_FEATURES_EXT).pNext(this.device2DViewOf3DFeatures.address());
        //this.deviceMutableDescriptorFeaturesV = VkPhysicalDeviceMutableDescriptorTypeFeaturesVALVE.create().pNext(this.deviceTransformFeedbackFeatures.address());
        this.deviceMutableDescriptorFeatures = VkPhysicalDeviceMutableDescriptorTypeFeaturesEXT.create().pNext(this.deviceTransformFeedbackFeatures.address()).sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MUTABLE_DESCRIPTOR_TYPE_FEATURES_EXT);
        this.deviceWorkgroupMemoryExplicitFeatures = VkPhysicalDeviceWorkgroupMemoryExplicitLayoutFeaturesKHR.create().pNext(this.deviceMutableDescriptorFeatures.address()).sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_WORKGROUP_MEMORY_EXPLICIT_LAYOUT_FEATURES_KHR);
        this.deviceRayTracingMaintenance1Features = VkPhysicalDeviceRayTracingMaintenance1FeaturesKHR.create().pNext(this.deviceWorkgroupMemoryExplicitFeatures.address()).sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_RAY_TRACING_MAINTENANCE_1_FEATURES_KHR);
        this.deviceShaderClockFeatures = VkPhysicalDeviceShaderClockFeaturesKHR.create().pNext(this.deviceRayTracingMaintenance1Features.address()).sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SHADER_CLOCK_FEATURES_KHR);
        this.deviceImageAtomicInt64Features = VkPhysicalDeviceShaderImageAtomicInt64FeaturesEXT.create().pNext(this.deviceShaderClockFeatures.address()).sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SHADER_IMAGE_ATOMIC_INT64_FEATURES_EXT);
        this.deviceAtomicFloat2Features = VkPhysicalDeviceShaderAtomicFloat2FeaturesEXT.create().pNext(this.deviceImageAtomicInt64Features.address()).sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SHADER_ATOMIC_FLOAT_2_FEATURES_EXT);
        this.deviceAtomicFloatFeatures = VkPhysicalDeviceShaderAtomicFloatFeaturesEXT.create().pNext(this.deviceAtomicFloat2Features.address()).sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SHADER_ATOMIC_FLOAT_FEATURES_EXT);
        this.devicePipelineRobustnessFeatures = VkPhysicalDevicePipelineRobustnessFeaturesEXT.create().pNext(this.deviceAtomicFloatFeatures.address()).sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PIPELINE_ROBUSTNESS_FEATURES_EXT);
        this.deviceMultiDrawFeatures = VkPhysicalDeviceMultiDrawFeaturesEXT.create().pNext(this.devicePipelineRobustnessFeatures.address()).sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MULTI_DRAW_FEATURES_EXT);
        this.deviceBarycentricFeatures = VkPhysicalDeviceFragmentShaderBarycentricFeaturesKHR.create().pNext(this.deviceMultiDrawFeatures.address()).sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FRAGMENT_SHADER_BARYCENTRIC_FEATURES_KHR);
        this.deviceMeshShaderFeatures = VkPhysicalDeviceMeshShaderFeaturesEXT.create().pNext(this.deviceBarycentricFeatures.address()).sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MESH_SHADER_FEATURES_EXT);
        this.deviceDescriptorBufferFeatures = VkPhysicalDeviceDescriptorBufferFeaturesEXT.create().pNext(this.deviceMeshShaderFeatures.address()).sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DESCRIPTOR_BUFFER_FEATURES_EXT);
        this.deviceVertexInputFeatures = VkPhysicalDeviceVertexInputDynamicStateFeaturesEXT.create().pNext(this.deviceDescriptorBufferFeatures.address()).sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VERTEX_INPUT_DYNAMIC_STATE_FEATURES_EXT);
        this.deviceRobustness2Features = VkPhysicalDeviceRobustness2FeaturesEXT.create().pNext(this.deviceVertexInputFeatures.address()).sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_ROBUSTNESS_2_FEATURES_EXT);
        this.deviceRayQueryFeatures = VkPhysicalDeviceRayQueryFeaturesKHR.create().pNext(this.deviceRobustness2Features.address()).sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_RAY_QUERY_FEATURES_KHR);
        this.deviceAccelerationStructureFeaturs = VkPhysicalDeviceAccelerationStructureFeaturesKHR.create().pNext(this.deviceRayQueryFeatures.address()).sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_ACCELERATION_STRUCTURE_FEATURES_KHR);
        this.deviceFeatures11 = VkPhysicalDeviceVulkan11Features.create().pNext(this.deviceAccelerationStructureFeaturs.address()).sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_1_FEATURES);
        this.deviceFeatures12 = VkPhysicalDeviceVulkan12Features.create().pNext(this.deviceFeatures11.address()).sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_FEATURES);
        this.deviceFeatures13 = VkPhysicalDeviceVulkan13Features.create().pNext(this.deviceFeatures12.address()).sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_3_FEATURES);
        this.deviceFeatures = VkPhysicalDeviceFeatures2.create().pNext(this.deviceFeatures13.address()).sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2);

        //
        this.deviceDescriptorBufferProperties = VkPhysicalDeviceDescriptorBufferPropertiesEXT.create().sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DESCRIPTOR_INDEXING_PROPERTIES);
        this.deviceProperties = VkPhysicalDeviceProperties2.create().pNext(this.deviceDescriptorBufferProperties.address()).sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PROPERTIES_2);

        //
        VK11.vkGetPhysicalDeviceProperties2(this.physicalDevice, this.deviceProperties);
        VK11.vkGetPhysicalDeviceFeatures2(this.physicalDevice, this.deviceFeatures);

        //
        VK11.vkGetPhysicalDeviceQueueFamilyProperties(this.physicalDevice, this.queueFamilyCount = memAllocInt(1), null);
        VK11.vkGetPhysicalDeviceQueueFamilyProperties(this.physicalDevice, this.queueFamilyCount, this.queueFamilyProperties = VkQueueFamilyProperties.create(this.queueFamilyCount.get(0)));

        //
        VK11.vkEnumerateDeviceExtensionProperties(this.physicalDevice, "", this.extensionCount = memAllocInt(1), null);
        VK11.vkEnumerateDeviceExtensionProperties(this.physicalDevice, "", this.extensionCount, this.extensions = VkExtensionProperties.create(this.extensionCount.get(0)));
    }



    //
    public BasicCInfo.SurfaceCapability getSurfaceInfo(long surface, int queueFamilyIndex) {
        BasicCInfo.SurfaceCapability capability = new BasicCInfo.SurfaceCapability();

        //
        org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR(this.physicalDevice, queueFamilyIndex, surface, capability.surfaceSupport);
        if (capability.surfaceSupport.get(0) > 0) {
            org.lwjgl.vulkan.KHRGetSurfaceCapabilities2.vkGetPhysicalDeviceSurfaceCapabilities2KHR(this.physicalDevice, VkPhysicalDeviceSurfaceInfo2KHR.create().surface(surface), capability.capabilities2 = VkSurfaceCapabilities2KHR.create());
            org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR(this.physicalDevice, surface, capability.presentModeCount, null);
            org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR(this.physicalDevice, surface, capability.presentModeCount, capability.presentModes = memAllocInt(capability.presentModeCount.get()));
            org.lwjgl.vulkan.KHRGetSurfaceCapabilities2.vkGetPhysicalDeviceSurfaceFormats2KHR(this.physicalDevice, VkPhysicalDeviceSurfaceInfo2KHR.create().surface(surface), capability.formatCount = memAllocInt(1), null);
            org.lwjgl.vulkan.KHRGetSurfaceCapabilities2.vkGetPhysicalDeviceSurfaceFormats2KHR(this.physicalDevice, VkPhysicalDeviceSurfaceInfo2KHR.create().surface(surface), capability.formatCount, capability.formats2 = VkSurfaceFormat2KHR.create(capability.formatCount.get(0)));
        }

        //
        return capability;
    };

    //
    public int searchQueueFamilyIndex(int bits) {
        int queueIndex = -1;
        for (int I=0;I<this.queueFamilyCount.get();I++) {
            if ((this.queueFamilyProperties.get(I).queueFlags() & bits) > 0) {
                queueIndex = I; break;
            }
        }
        return queueIndex;
    }

    public int getMemoryTypeIndex(int typeFilter, int propertyFlag, int ignoreFlags, long size) {
        var memoryBudget = VkPhysicalDeviceMemoryBudgetPropertiesEXT.create().sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MEMORY_BUDGET_PROPERTIES_EXT);
        var memoryProperties2 = VkPhysicalDeviceMemoryProperties2.create().sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MEMORY_PROPERTIES_2).pNext(memoryBudget.address());
        var memoryProperties = memoryProperties2.memoryProperties();
        vkGetPhysicalDeviceMemoryProperties2(this.physicalDevice, memoryProperties2);
        for (var I = 0; I < memoryProperties.memoryTypeCount(); ++I) {
            var prop = memoryProperties.memoryTypes().get(I);
            if (
                    (typeFilter & (1 << I)) > 0 &&
                            (prop.propertyFlags() & propertyFlag) == propertyFlag &&
                            (prop.propertyFlags() & ignoreFlags) == 0 &&
                            memoryBudget.heapBudget().get(prop.heapIndex()) >= size
            ) { return I; }
        };
        return -1;
    }

    //
    public BasicCInfo.FormatProperties getFormatProperties(int format) {
        var formatProperties = new BasicCInfo.FormatProperties();
        formatProperties.properties3 = VkFormatProperties3.create().sType(VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_3);
        formatProperties.properties2 = VkFormatProperties2.create().sType(VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2);
        vkGetPhysicalDeviceFormatProperties2(this.physicalDevice, format, formatProperties.properties2);
        formatProperties.properties = formatProperties.properties2.formatProperties();
        formatProperties.info = BasicCInfo.vk_format_table.get(format);
        return formatProperties;
    }

}
