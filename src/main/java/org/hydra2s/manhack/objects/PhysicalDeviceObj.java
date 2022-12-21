package org.hydra2s.manhack.objects;

//
import java.nio.IntBuffer;
import org.lwjgl.vulkan.*;

//
public class PhysicalDeviceObj extends BasicObj  {

    //
    public VkPhysicalDeviceTransformFeedbackFeaturesEXT deviceTransformFeedbackFeatures;
    public VkPhysicalDeviceMutableDescriptorTypeFeaturesEXT deviceMutableDescriptorFeatures;
    public VkPhysicalDeviceWorkgroupMemoryExplicitLayoutFeaturesKHR deviceWorkgroupMemoryExplicitFeatures;
    public VkPhysicalDeviceRayTracingMaintenance1FeaturesKHR deviceRayTracingMaintenance1Features;
    public VkPhysicalDeviceShaderClockFeaturesKHR deviceShaderClockFeatures;
    public VkPhysicalDeviceShaderImageAtomicInt64FeaturesEXT deviceImageAtomicInt64Features;
    public VkPhysicalDeviceRobustness2FeaturesEXT deviceRobustness2Features;
    public VkPhysicalDeviceVertexInputDynamicStateFeaturesEXT deviceVertexInputFeatures;
    public VkPhysicalDeviceDescriptorBufferFeaturesEXT deviceDescriptorBufferFeatures;
    public VkPhysicalDeviceMeshShaderFeaturesEXT deviceMeshShaderFeatures;
    public VkPhysicalDeviceFragmentShaderBarycentricFeaturesKHR deviceBarycentricFeatures;
    public VkPhysicalDeviceMultiDrawFeaturesEXT deviceMultiDrawFeatures;
    public VkPhysicalDevicePipelineRobustnessFeaturesEXT devicePipelineRobustnessFeatures;
    public VkPhysicalDeviceShaderAtomicFloatFeaturesEXT deviceAtomicFloatFeatures;
    public VkPhysicalDeviceShaderAtomicFloat2FeaturesEXT deviceAtomicFloat2Features;
    public VkPhysicalDeviceRayQueryFeaturesKHR deviceRayQueryFeatures;
    public VkPhysicalDeviceAccelerationStructureFeaturesKHR deviceAccelerationStructureFeaturs;
    public VkPhysicalDeviceVulkan11Features deviceFeatures11;
    public VkPhysicalDeviceVulkan12Features deviceFeatures12;
    public VkPhysicalDeviceVulkan13Features deviceFeatures13;
    public VkPhysicalDeviceFeatures2 deviceFeatures;
    public VkPhysicalDeviceDescriptorBufferPropertiesEXT deviceDescriptorBufferProperties;
    public VkPhysicalDeviceProperties2 deviceProperties;
    public VkQueueFamilyProperties.Buffer queueFamilyProperties = null;

    // 
    public VkLayerProperties.Buffer layers = null;
    public VkExtensionProperties.Buffer extensions = null;
    public VkPhysicalDevice physicalDevice = null;

    //
    protected IntBuffer queueFamilyCount;
    protected IntBuffer extensionCount;

    //
    public static class SurfaceCapability {
        protected IntBuffer presentModeCount = IntBuffer.allocate(1);
        protected IntBuffer formatCount = IntBuffer.allocate(1);
        public IntBuffer surfaceSupport = IntBuffer.allocate(1);
        public IntBuffer presentModes = null;
        public VkSurfaceCapabilities2KHR capabilities2 = null;
        public org.lwjgl.vulkan.VkSurfaceFormat2KHR.Buffer formats2 = null;
        public SurfaceCapability() {};
    };

    //
    public PhysicalDeviceObj(Handle base, Handle handle) {
        super(base, handle);

        //
        var instanceObj = (InstanceObj)BasicObj.globalHandleMap.get(base.get());
        this.physicalDevice = new VkPhysicalDevice(handle.get(), instanceObj.instance);
        BasicObj.globalHandleMap.put(handle.get(), this);

        // TODO: unify into one object
        this.deviceTransformFeedbackFeatures = VkPhysicalDeviceTransformFeedbackFeaturesEXT.create();
        //this.deviceMutableDescriptorFeaturesV = VkPhysicalDeviceMutableDescriptorTypeFeaturesVALVE.create().pNext(this.deviceTransformFeedbackFeatures.address());
        this.deviceMutableDescriptorFeatures = VkPhysicalDeviceMutableDescriptorTypeFeaturesEXT.create().pNext(this.deviceTransformFeedbackFeatures.address());
        this.deviceWorkgroupMemoryExplicitFeatures = VkPhysicalDeviceWorkgroupMemoryExplicitLayoutFeaturesKHR.create().pNext(this.deviceMutableDescriptorFeatures.address());
        this.deviceRayTracingMaintenance1Features = VkPhysicalDeviceRayTracingMaintenance1FeaturesKHR.create().pNext(this.deviceWorkgroupMemoryExplicitFeatures.address());
        this.deviceShaderClockFeatures = VkPhysicalDeviceShaderClockFeaturesKHR.create().pNext(this.deviceRayTracingMaintenance1Features.address());
        this.deviceImageAtomicInt64Features = VkPhysicalDeviceShaderImageAtomicInt64FeaturesEXT.create().pNext(this.deviceShaderClockFeatures.address());
        this.deviceAtomicFloat2Features = VkPhysicalDeviceShaderAtomicFloat2FeaturesEXT.create().pNext(this.deviceImageAtomicInt64Features.address());
        this.deviceAtomicFloatFeatures = VkPhysicalDeviceShaderAtomicFloatFeaturesEXT.create().pNext(this.deviceAtomicFloat2Features.address());
        this.devicePipelineRobustnessFeatures = VkPhysicalDevicePipelineRobustnessFeaturesEXT.create().pNext(this.deviceAtomicFloatFeatures.address());
        this.deviceMultiDrawFeatures = VkPhysicalDeviceMultiDrawFeaturesEXT.create().pNext(this.devicePipelineRobustnessFeatures.address());
        this.deviceBarycentricFeatures = VkPhysicalDeviceFragmentShaderBarycentricFeaturesKHR.create().pNext(this.deviceMultiDrawFeatures.address());
        this.deviceMeshShaderFeatures = VkPhysicalDeviceMeshShaderFeaturesEXT.create().pNext(this.deviceBarycentricFeatures.address());
        this.deviceDescriptorBufferFeatures = VkPhysicalDeviceDescriptorBufferFeaturesEXT.create().pNext(this.deviceMeshShaderFeatures.address());
        this.deviceVertexInputFeatures = VkPhysicalDeviceVertexInputDynamicStateFeaturesEXT.create().pNext(this.deviceDescriptorBufferFeatures.address());
        this.deviceRobustness2Features = VkPhysicalDeviceRobustness2FeaturesEXT.create().pNext(this.deviceVertexInputFeatures.address());
        this.deviceRayQueryFeatures = VkPhysicalDeviceRayQueryFeaturesKHR.create().pNext(this.deviceRobustness2Features.address());
        this.deviceAccelerationStructureFeaturs = VkPhysicalDeviceAccelerationStructureFeaturesKHR.create().pNext(this.deviceRayQueryFeatures.address());
        this.deviceFeatures11 = VkPhysicalDeviceVulkan11Features.create().pNext(this.deviceAccelerationStructureFeaturs.address());
        this.deviceFeatures12 = VkPhysicalDeviceVulkan12Features.create().pNext(this.deviceFeatures11.address());
        this.deviceFeatures13 = VkPhysicalDeviceVulkan13Features.create().pNext(this.deviceFeatures12.address());
        this.deviceFeatures = VkPhysicalDeviceFeatures2.create().pNext(this.deviceFeatures13.address());

        //
        this.deviceDescriptorBufferProperties = VkPhysicalDeviceDescriptorBufferPropertiesEXT.create();
        this.deviceProperties = VkPhysicalDeviceProperties2.create().pNext(this.deviceDescriptorBufferProperties.address());

        //
        VK11.vkGetPhysicalDeviceProperties2(this.physicalDevice, this.deviceProperties);
        VK11.vkGetPhysicalDeviceFeatures2(this.physicalDevice, this.deviceFeatures);

        //
        VK11.vkGetPhysicalDeviceQueueFamilyProperties(this.physicalDevice, this.queueFamilyCount = IntBuffer.allocate(1), null);
        VK11.vkGetPhysicalDeviceQueueFamilyProperties(this.physicalDevice, this.queueFamilyCount, this.queueFamilyProperties = VkQueueFamilyProperties.create(this.queueFamilyCount.get(0)));

        //
        VK11.vkEnumerateDeviceExtensionProperties(this.physicalDevice, "", this.extensionCount = IntBuffer.allocate(1), null);
        VK11.vkEnumerateDeviceExtensionProperties(this.physicalDevice, "", this.extensionCount, this.extensions = VkExtensionProperties.create(this.extensionCount.get(0)));
    }

    //
    public SurfaceCapability getSurfaceInfo(long surface, int queueFamilyIndex) {
        SurfaceCapability capability = new SurfaceCapability();

        //
        org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR(this.physicalDevice, queueFamilyIndex, surface, capability.surfaceSupport);
        if (capability.surfaceSupport.get(0) > 0) {
            org.lwjgl.vulkan.KHRGetSurfaceCapabilities2.vkGetPhysicalDeviceSurfaceCapabilities2KHR(this.physicalDevice, VkPhysicalDeviceSurfaceInfo2KHR.create().surface(surface), capability.capabilities2 = VkSurfaceCapabilities2KHR.create());
            org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR(this.physicalDevice, surface, capability.presentModeCount, null);
            org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR(this.physicalDevice, surface, capability.presentModeCount, capability.presentModes = IntBuffer.allocate(capability.presentModeCount.get()));
            org.lwjgl.vulkan.KHRGetSurfaceCapabilities2.vkGetPhysicalDeviceSurfaceFormats2KHR(this.physicalDevice, VkPhysicalDeviceSurfaceInfo2KHR.create().surface(surface), capability.formatCount = IntBuffer.allocate(1), null);
            org.lwjgl.vulkan.KHRGetSurfaceCapabilities2.vkGetPhysicalDeviceSurfaceFormats2KHR(this.physicalDevice, VkPhysicalDeviceSurfaceInfo2KHR.create().surface(surface), capability.formatCount, capability.formats2 = VkSurfaceFormat2KHR.create(capability.formatCount.get(0)));
        }

        //
        return capability;
    }

    public int searchQueueFamilyIndex(int bits) {
        int queueIndex = -1;
        for (int I=0;I<this.queueFamilyCount.get();I++) {
            if ((this.queueFamilyProperties.get(I).queueFlags() & bits) > 0) {
                queueIndex = I; break;
            }
        }
        return queueIndex;
    }

}
