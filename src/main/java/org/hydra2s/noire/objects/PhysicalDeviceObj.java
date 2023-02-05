package org.hydra2s.noire.objects;

//

import org.hydra2s.noire.descriptors.UtilsCInfo;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;

import static org.lwjgl.system.MemoryUtil.memAllocInt;
import static org.lwjgl.vulkan.EXTDescriptorBuffer.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DESCRIPTOR_BUFFER_FEATURES_EXT;
import static org.lwjgl.vulkan.EXTDescriptorBuffer.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DESCRIPTOR_BUFFER_PROPERTIES_EXT;
import static org.lwjgl.vulkan.EXTExtendedDynamicState2.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_EXTENDED_DYNAMIC_STATE_2_FEATURES_EXT;
import static org.lwjgl.vulkan.EXTExtendedDynamicState3.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_EXTENDED_DYNAMIC_STATE_3_FEATURES_EXT;
import static org.lwjgl.vulkan.EXTFragmentShaderInterlock.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FRAGMENT_SHADER_INTERLOCK_FEATURES_EXT;
import static org.lwjgl.vulkan.EXTGraphicsPipelineLibrary.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_GRAPHICS_PIPELINE_LIBRARY_FEATURES_EXT;
import static org.lwjgl.vulkan.EXTImage2dViewOf3d.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_IMAGE_2D_VIEW_OF_3D_FEATURES_EXT;
import static org.lwjgl.vulkan.EXTIndexTypeUint8.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_INDEX_TYPE_UINT8_FEATURES_EXT;
import static org.lwjgl.vulkan.EXTMemoryBudget.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MEMORY_BUDGET_PROPERTIES_EXT;
import static org.lwjgl.vulkan.EXTMemoryPriority.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MEMORY_PRIORITY_FEATURES_EXT;
import static org.lwjgl.vulkan.EXTMeshShader.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MESH_SHADER_FEATURES_EXT;
import static org.lwjgl.vulkan.EXTMultiDraw.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MULTI_DRAW_FEATURES_EXT;
import static org.lwjgl.vulkan.EXTMutableDescriptorType.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MUTABLE_DESCRIPTOR_TYPE_FEATURES_EXT;
import static org.lwjgl.vulkan.EXTPageableDeviceLocalMemory.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PAGEABLE_DEVICE_LOCAL_MEMORY_FEATURES_EXT;
import static org.lwjgl.vulkan.EXTPipelineRobustness.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PIPELINE_ROBUSTNESS_FEATURES_EXT;
import static org.lwjgl.vulkan.EXTRobustness2.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_ROBUSTNESS_2_FEATURES_EXT;
import static org.lwjgl.vulkan.EXTShaderAtomicFloat.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SHADER_ATOMIC_FLOAT_FEATURES_EXT;
import static org.lwjgl.vulkan.EXTShaderAtomicFloat2.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SHADER_ATOMIC_FLOAT_2_FEATURES_EXT;
import static org.lwjgl.vulkan.EXTShaderImageAtomicInt64.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SHADER_IMAGE_ATOMIC_INT64_FEATURES_EXT;
import static org.lwjgl.vulkan.EXTTransformFeedback.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_TRANSFORM_FEEDBACK_FEATURES_EXT;
import static org.lwjgl.vulkan.EXTVertexInputDynamicState.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VERTEX_INPUT_DYNAMIC_STATE_FEATURES_EXT;
import static org.lwjgl.vulkan.KHRAccelerationStructure.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_ACCELERATION_STRUCTURE_FEATURES_KHR;
import static org.lwjgl.vulkan.KHRFragmentShaderBarycentric.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FRAGMENT_SHADER_BARYCENTRIC_FEATURES_KHR;
import static org.lwjgl.vulkan.KHRGetSurfaceCapabilities2.*;
import static org.lwjgl.vulkan.KHRGlobalPriority.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_GLOBAL_PRIORITY_QUERY_FEATURES_KHR;
import static org.lwjgl.vulkan.KHRRayQuery.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_RAY_QUERY_FEATURES_KHR;
import static org.lwjgl.vulkan.KHRRayTracingMaintenance1.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_RAY_TRACING_MAINTENANCE_1_FEATURES_KHR;
import static org.lwjgl.vulkan.KHRShaderClock.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SHADER_CLOCK_FEATURES_KHR;
import static org.lwjgl.vulkan.KHRWorkgroupMemoryExplicitLayout.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_WORKGROUP_MEMORY_EXPLICIT_LAYOUT_FEATURES_KHR;
import static org.lwjgl.vulkan.NVRepresentativeFragmentTest.VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_REPRESENTATIVE_FRAGMENT_TEST_FEATURES_NV;
import static org.lwjgl.vulkan.VK11.*;
import static org.lwjgl.vulkan.VK12.*;
import static org.lwjgl.vulkan.VK13.*;

//
public class PhysicalDeviceObj extends BasicObj {

    public static class PhysicalDeviceFeatures {
        public VkPhysicalDeviceRepresentativeFragmentTestFeaturesNV representativeFragmentTestNV = null;
        public VkPhysicalDeviceFragmentShaderInterlockFeaturesEXT fragmentInterlocking = null;
        public VkPhysicalDeviceGlobalPriorityQueryFeaturesKHR globalPriority = null;
        public VkPhysicalDevicePageableDeviceLocalMemoryFeaturesEXT pageableMemory = null;
        public VkPhysicalDeviceMemoryPriorityFeaturesEXT memoryPriority = null;
        public VkPhysicalDeviceGraphicsPipelineLibraryFeaturesEXT graphicsPipelineLibrary = null;
        public VkPhysicalDeviceExtendedDynamicState2FeaturesEXT dynamicState2 = null;
        public VkPhysicalDeviceExtendedDynamicState3FeaturesEXT dynamicState3 = null;
        public VkPhysicalDeviceIndexTypeUint8FeaturesEXT indexUint8 = null;
        public VkPhysicalDeviceImage2DViewOf3DFeaturesEXT image2DViewOf3D = null;
        public VkPhysicalDeviceTransformFeedbackFeaturesEXT transformFeedback = null;
        public VkPhysicalDeviceMutableDescriptorTypeFeaturesEXT mutableDescriptor = null;
        public VkPhysicalDeviceWorkgroupMemoryExplicitLayoutFeaturesKHR workgroupMemoryExplicit = null;
        public VkPhysicalDeviceRayTracingMaintenance1FeaturesKHR rayTracingMaintenance1 = null;
        public VkPhysicalDeviceShaderClockFeaturesKHR shaderClock = null;
        public VkPhysicalDeviceShaderImageAtomicInt64FeaturesEXT imageAtomicInt64 = null;
        public VkPhysicalDeviceRobustness2FeaturesEXT robustness2 = null;
        public VkPhysicalDeviceVertexInputDynamicStateFeaturesEXT vertexInput = null;
        public VkPhysicalDeviceDescriptorBufferFeaturesEXT descriptorBuffer = null;
        public VkPhysicalDeviceMeshShaderFeaturesEXT meshShader = null;
        public VkPhysicalDeviceFragmentShaderBarycentricFeaturesKHR barycentric = null;
        public VkPhysicalDeviceMultiDrawFeaturesEXT multiDraw = null;
        public VkPhysicalDevicePipelineRobustnessFeaturesEXT pipelineRobustness = null;
        public VkPhysicalDeviceShaderAtomicFloatFeaturesEXT atomicFloat = null;
        public VkPhysicalDeviceShaderAtomicFloat2FeaturesEXT atomicFloat2 = null;
        public VkPhysicalDeviceRayQueryFeaturesKHR rayQuery = null;
        public VkPhysicalDeviceAccelerationStructureFeaturesKHR accelerationStructure = null;
        public VkPhysicalDeviceVulkan11Features vulkan11 = null;
        public VkPhysicalDeviceVulkan12Features vulkan12 = null;
        public VkPhysicalDeviceVulkan13Features vulkan13 = null;
        public VkPhysicalDeviceFeatures2 features = null;

        public PhysicalDeviceFeatures() {
            // TODO: unify into one object
            this.representativeFragmentTestNV = VkPhysicalDeviceRepresentativeFragmentTestFeaturesNV.calloc().sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_REPRESENTATIVE_FRAGMENT_TEST_FEATURES_NV);
            this.fragmentInterlocking = VkPhysicalDeviceFragmentShaderInterlockFeaturesEXT.calloc().sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FRAGMENT_SHADER_INTERLOCK_FEATURES_EXT).pNext(this.representativeFragmentTestNV.address());
            this.globalPriority = VkPhysicalDeviceGlobalPriorityQueryFeaturesKHR.calloc().sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_GLOBAL_PRIORITY_QUERY_FEATURES_KHR).pNext(this.fragmentInterlocking.address());
            this.pageableMemory = VkPhysicalDevicePageableDeviceLocalMemoryFeaturesEXT.calloc().sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PAGEABLE_DEVICE_LOCAL_MEMORY_FEATURES_EXT).pNext(this.globalPriority.address());
            this.memoryPriority = VkPhysicalDeviceMemoryPriorityFeaturesEXT.calloc().sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MEMORY_PRIORITY_FEATURES_EXT).pNext(this.pageableMemory.address());
            this.graphicsPipelineLibrary = VkPhysicalDeviceGraphicsPipelineLibraryFeaturesEXT.calloc().sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_GRAPHICS_PIPELINE_LIBRARY_FEATURES_EXT).pNext(this.memoryPriority.address());
            this.dynamicState2 = VkPhysicalDeviceExtendedDynamicState2FeaturesEXT.calloc().sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_EXTENDED_DYNAMIC_STATE_2_FEATURES_EXT).pNext(this.graphicsPipelineLibrary.address());
            this.dynamicState3 = VkPhysicalDeviceExtendedDynamicState3FeaturesEXT.calloc().sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_EXTENDED_DYNAMIC_STATE_3_FEATURES_EXT).pNext(this.dynamicState2.address());
            this.indexUint8 = VkPhysicalDeviceIndexTypeUint8FeaturesEXT.calloc().sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_INDEX_TYPE_UINT8_FEATURES_EXT).pNext(this.dynamicState3.address());
            this.image2DViewOf3D = VkPhysicalDeviceImage2DViewOf3DFeaturesEXT.calloc().sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_IMAGE_2D_VIEW_OF_3D_FEATURES_EXT).pNext(this.indexUint8.address());
            this.transformFeedback = VkPhysicalDeviceTransformFeedbackFeaturesEXT.calloc().sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_TRANSFORM_FEEDBACK_FEATURES_EXT).pNext(this.image2DViewOf3D.address());
            //this.deviceMutableDescriptorFeaturesV = VkPhysicalDeviceMutableDescriptorTypeFeaturesVALVE.calloc().pNext(this.transformFeedback.address());
            this.mutableDescriptor = VkPhysicalDeviceMutableDescriptorTypeFeaturesEXT.calloc().pNext(this.transformFeedback.address()).sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MUTABLE_DESCRIPTOR_TYPE_FEATURES_EXT);
            this.workgroupMemoryExplicit = VkPhysicalDeviceWorkgroupMemoryExplicitLayoutFeaturesKHR.calloc().pNext(this.mutableDescriptor.address()).sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_WORKGROUP_MEMORY_EXPLICIT_LAYOUT_FEATURES_KHR);
            this.rayTracingMaintenance1 = VkPhysicalDeviceRayTracingMaintenance1FeaturesKHR.calloc().pNext(this.workgroupMemoryExplicit.address()).sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_RAY_TRACING_MAINTENANCE_1_FEATURES_KHR);
            this.shaderClock = VkPhysicalDeviceShaderClockFeaturesKHR.calloc().pNext(this.rayTracingMaintenance1.address()).sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SHADER_CLOCK_FEATURES_KHR);
            this.imageAtomicInt64 = VkPhysicalDeviceShaderImageAtomicInt64FeaturesEXT.calloc().pNext(this.shaderClock.address()).sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SHADER_IMAGE_ATOMIC_INT64_FEATURES_EXT);
            this.atomicFloat2 = VkPhysicalDeviceShaderAtomicFloat2FeaturesEXT.calloc().pNext(this.imageAtomicInt64.address()).sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SHADER_ATOMIC_FLOAT_2_FEATURES_EXT);
            this.atomicFloat = VkPhysicalDeviceShaderAtomicFloatFeaturesEXT.calloc().pNext(this.atomicFloat2.address()).sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SHADER_ATOMIC_FLOAT_FEATURES_EXT);
            this.pipelineRobustness = VkPhysicalDevicePipelineRobustnessFeaturesEXT.calloc().pNext(this.atomicFloat.address()).sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PIPELINE_ROBUSTNESS_FEATURES_EXT);
            this.multiDraw = VkPhysicalDeviceMultiDrawFeaturesEXT.calloc().pNext(this.pipelineRobustness.address()).sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MULTI_DRAW_FEATURES_EXT);
            this.barycentric = VkPhysicalDeviceFragmentShaderBarycentricFeaturesKHR.calloc().pNext(this.multiDraw.address()).sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FRAGMENT_SHADER_BARYCENTRIC_FEATURES_KHR);
            this.meshShader = VkPhysicalDeviceMeshShaderFeaturesEXT.calloc().pNext(this.barycentric.address()).sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MESH_SHADER_FEATURES_EXT);
            this.descriptorBuffer = VkPhysicalDeviceDescriptorBufferFeaturesEXT.calloc().pNext(this.meshShader.address()).sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DESCRIPTOR_BUFFER_FEATURES_EXT);
            this.vertexInput = VkPhysicalDeviceVertexInputDynamicStateFeaturesEXT.calloc().pNext(this.descriptorBuffer.address()).sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VERTEX_INPUT_DYNAMIC_STATE_FEATURES_EXT);
            this.robustness2 = VkPhysicalDeviceRobustness2FeaturesEXT.calloc().pNext(this.vertexInput.address()).sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_ROBUSTNESS_2_FEATURES_EXT);
            this.rayQuery = VkPhysicalDeviceRayQueryFeaturesKHR.calloc().pNext(this.robustness2.address()).sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_RAY_QUERY_FEATURES_KHR);
            this.accelerationStructure = VkPhysicalDeviceAccelerationStructureFeaturesKHR.calloc().pNext(this.rayQuery.address()).sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_ACCELERATION_STRUCTURE_FEATURES_KHR);
            this.vulkan11 = VkPhysicalDeviceVulkan11Features.calloc().pNext(this.accelerationStructure.address()).sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_1_FEATURES);
            this.vulkan12 = VkPhysicalDeviceVulkan12Features.calloc().pNext(this.vulkan11.address()).sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_FEATURES);
            this.vulkan13 = VkPhysicalDeviceVulkan13Features.calloc().pNext(this.vulkan12.address()).sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_3_FEATURES);
            this.features = VkPhysicalDeviceFeatures2.calloc().pNext(this.vulkan13.address()).sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2);
        }
    }

    //
    public static class PhysicalDeviceProperties {
        public VkPhysicalDeviceDescriptorBufferPropertiesEXT descriptorBuffer = null;
        public VkPhysicalDeviceVulkan13Properties vulkan13 = null;
        public VkPhysicalDeviceVulkan12Properties vulkan12 = null;
        public VkPhysicalDeviceVulkan11Properties vulkan11 = null;
        public VkPhysicalDeviceProperties2 properties = null;

        public PhysicalDeviceProperties() {
            this.descriptorBuffer = VkPhysicalDeviceDescriptorBufferPropertiesEXT.calloc().sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DESCRIPTOR_BUFFER_PROPERTIES_EXT);
            this.vulkan13 = VkPhysicalDeviceVulkan13Properties.calloc().pNext(this.descriptorBuffer.address()).sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_3_PROPERTIES);
            this.vulkan12 = VkPhysicalDeviceVulkan12Properties.calloc().pNext(this.vulkan13.address()).sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_2_PROPERTIES);
            this.vulkan11 = VkPhysicalDeviceVulkan11Properties.calloc().pNext(this.vulkan12.address()).sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_1_PROPERTIES);
            this.properties = VkPhysicalDeviceProperties2.calloc().pNext(this.vulkan11.address()).sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PROPERTIES_2);
        }
    }

    //
    public PhysicalDeviceFeatures features;
    public PhysicalDeviceProperties properties;



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
        this.physicalDevice = new VkPhysicalDevice(handle.get(), instanceObj.instance);
        BasicObj.globalHandleMap.put$(handle.get(), this);

        //
        VK11.vkGetPhysicalDeviceProperties2(this.physicalDevice, (this.properties = new PhysicalDeviceProperties()).properties);
        VK11.vkGetPhysicalDeviceFeatures2(this.physicalDevice, (this.features = new PhysicalDeviceFeatures()).features);

        //
        VK11.vkGetPhysicalDeviceQueueFamilyProperties(this.physicalDevice, this.queueFamilyCount = memAllocInt(1), null);
        VK11.vkGetPhysicalDeviceQueueFamilyProperties(this.physicalDevice, this.queueFamilyCount, this.queueFamilyProperties = VkQueueFamilyProperties.calloc(this.queueFamilyCount.get(0)));

        //
        VK11.vkEnumerateDeviceExtensionProperties(this.physicalDevice, "", this.extensionCount = memAllocInt(1), null);
        VK11.vkEnumerateDeviceExtensionProperties(this.physicalDevice, "", this.extensionCount, this.extensions = VkExtensionProperties.calloc(this.extensionCount.get(0)));
    }

    //
    public UtilsCInfo.SurfaceCapability getSurfaceInfo(long surface, int queueFamilyIndex) {
        UtilsCInfo.SurfaceCapability capability = new UtilsCInfo.SurfaceCapability();

        //
        org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR(this.physicalDevice, queueFamilyIndex, surface, capability.surfaceSupport = memAllocInt(1));
        if (capability.surfaceSupport.get(0) != 0) {
            var surfaceInfo = VkPhysicalDeviceSurfaceInfo2KHR.calloc().sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_SURFACE_INFO_2_KHR).surface(surface);

            //
            org.lwjgl.vulkan.KHRGetSurfaceCapabilities2.vkGetPhysicalDeviceSurfaceCapabilities2KHR(
                this.physicalDevice, surfaceInfo,
                capability.capabilities2 = VkSurfaceCapabilities2KHR.calloc().sType(VK_STRUCTURE_TYPE_SURFACE_CAPABILITIES_2_KHR)
            );

            //
            org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR(this.physicalDevice, surface, capability.presentModeCount = memAllocInt(1), null);
            org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR(this.physicalDevice, surface, capability.presentModeCount, capability.presentModes = memAllocInt(capability.presentModeCount.get(0)));

            //
            org.lwjgl.vulkan.KHRGetSurfaceCapabilities2.vkGetPhysicalDeviceSurfaceFormats2KHR(
                this.physicalDevice, surfaceInfo, capability.formatCount = memAllocInt(1),
                null);

            //
            var formats = VkSurfaceFormat2KHR.calloc(capability.formatCount.get(0));
            var Fs = formats.remaining();
            for (var I=0;I<Fs;I++) { formats.get(I).sType(VK_STRUCTURE_TYPE_SURFACE_FORMAT_2_KHR); };
            org.lwjgl.vulkan.KHRGetSurfaceCapabilities2.vkGetPhysicalDeviceSurfaceFormats2KHR(
                this.physicalDevice, surfaceInfo, capability.formatCount,
                capability.formats2 = formats);
        }

        //
        return capability;
    };

    //
    public VkQueueFamilyProperties getQueueFamilyProperties(int queueFamilyIndex) {
        return this.queueFamilyProperties.get(queueFamilyIndex);
    }

    //
    public int searchQueueFamilyIndex(int bits) {
        int queueIndex = -1;
        for (int I=0;I<this.queueFamilyCount.get();I++) {
            if ((this.queueFamilyProperties.get(I).queueFlags() & bits) != 0) {
                queueIndex = I; break;
            }
        }
        return queueIndex;
    }

    public int getMemoryTypeIndex(int typeFilter, int propertyFlag, int ignoreFlags, long size) {
        var memoryBudget = VkPhysicalDeviceMemoryBudgetPropertiesEXT.calloc().sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MEMORY_BUDGET_PROPERTIES_EXT);
        var memoryProperties2 = VkPhysicalDeviceMemoryProperties2.calloc().sType(VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MEMORY_PROPERTIES_2).pNext(memoryBudget.address());
        var memoryProperties = memoryProperties2.memoryProperties();
        vkGetPhysicalDeviceMemoryProperties2(this.physicalDevice, memoryProperties2);
        for (var I = 0; I < memoryProperties.memoryTypeCount(); ++I) {
            var prop = memoryProperties.memoryTypes().get(I);
            if (
                    (typeFilter & (1 << I)) != 0 &&
                            (prop.propertyFlags() & propertyFlag) == propertyFlag &&
                            (prop.propertyFlags() & ignoreFlags) == 0 &&
                            memoryBudget.heapBudget().get(prop.heapIndex()) >= size
            ) { return I; }
        };
        return -1;
    }

    //
    public UtilsCInfo.FormatProperties getFormatProperties(int format) {
        var formatProperties = new UtilsCInfo.FormatProperties();
        formatProperties.properties3 = VkFormatProperties3.calloc().sType(VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_3);
        formatProperties.properties2 = VkFormatProperties2.calloc().sType(VK_STRUCTURE_TYPE_FORMAT_PROPERTIES_2);
        vkGetPhysicalDeviceFormatProperties2(this.physicalDevice, format, formatProperties.properties2);
        formatProperties.properties = formatProperties.properties2.formatProperties();
        formatProperties.info = UtilsCInfo.vk_format_table.get(format);
        return formatProperties;
    }

}
