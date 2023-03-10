package org.hydra2s.noire.objects;

//

import org.hydra2s.noire.descriptors.InstanceCInfo;
import org.hydra2s.noire.descriptors.UtilsCInfo;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.util.ArrayList;

import static org.hydra2s.noire.descriptors.UtilsCInfo.vkCheckStatus;
import static org.lwjgl.BufferUtils.createPointerBuffer;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.glfwVulkanSupported;
import static org.lwjgl.vulkan.EXTDebugUtils.*;
import static org.lwjgl.vulkan.EXTDeviceAddressBindingReport.VK_DEBUG_UTILS_MESSAGE_TYPE_DEVICE_ADDRESS_BINDING_BIT_EXT;

// An America!
public class InstanceObj extends BasicObj {
    public InstanceObj(UtilsCInfo.Handle base, UtilsCInfo.Handle handle) {
        super(base, handle);
    }

    //
    public VkInstance instance = null;
    public VkDebugUtilsMessengerCallbackEXT callbackEXT = null;
    public long[] messagerEXT = {};

    //
    public VkExtensionProperties.Buffer availableExtensions = null;
    public PointerBuffer extensions = null;
    public PointerBuffer physicalDevices = null;

    //
    public VkLayerProperties.Buffer availableLayers = null;
    public PointerBuffer layers = null;
    public ArrayList<PhysicalDeviceObj> physicalDevicesObj = null;

    //
    protected PointerBuffer glfwExt = null;
    public VkInstanceCreateInfo instanceInfo = null;
    public VkApplicationInfo.Buffer appInfo = null;
    protected int[] extensionAmount = new int[]{0};
    protected int[] layersAmount = new int[]{0};
    //
    protected int[] physicalDeviceAmount = new int[]{0};

    public void PrintMessageType(int messageTypes) {
        //
        switch(messageTypes) {
            case VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT:
                System.out.println("Vulkan General Message...");
                break;

            case VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT:
                System.out.println("Vulkan Performance Message...");
                break;

            case VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT:
                System.out.println("Vulkan Validation Message...");
                break;

            case VK_DEBUG_UTILS_MESSAGE_TYPE_DEVICE_ADDRESS_BINDING_BIT_EXT:
                System.out.println("Vulkan Device Address Binding Message...");
                break;
        }
    }

    //
    public InstanceObj(UtilsCInfo.Handle base, InstanceCInfo cInfo) {
        super(base, cInfo);

        //
        if (!glfwInit()) {
            throw new RuntimeException("Failed to initialize GLFW");
        }
        if (!glfwVulkanSupported()) {
            throw new AssertionError("GLFW failed to find the Vulkan loader");
        }

        //
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API);

        // Layers
        this.layers = createPointerBuffer(2);
        //this.layers.put(0, MemoryUtil.memAddress(MemoryUtil.memUTF8("VK_LAYER_LUNARG_gfxreconstruct")));
        this.layers.put(0, MemoryUtil.memAddress(MemoryUtil.memUTF8("VK_LAYER_KHRONOS_validation")));
        this.layers.put(1, MemoryUtil.memAddress(MemoryUtil.memUTF8("VK_LAYER_KHRONOS_synchronization2")));

        //
        this.layersAmount = new int[]{1};
        VK10.vkEnumerateInstanceLayerProperties(this.layersAmount, null);
        VK10.vkEnumerateInstanceLayerProperties(this.layersAmount, this.availableLayers = VkLayerProperties.create(this.layersAmount[0]));

        // Extensions
        this.glfwExt = GLFWVulkan.glfwGetRequiredInstanceExtensions();
        this.extensions = createPointerBuffer(this.glfwExt.limit() + 1);
        this.extensions.put(0, MemoryUtil.memAddress(MemoryUtil.memUTF8("VK_KHR_get_surface_capabilities2")));
        //this.extensions.put(1, MemoryUtil.memAddress(MemoryUtil.memUTF8("VK_EXT_debug_utils")));
        for (int i=0;i<this.glfwExt.limit();i++) {
            this.extensions.put(i+1, MemoryUtil.memAddress(MemoryUtil.memUTF8(this.glfwExt.getStringUTF8(i))));
        }

        //
        this.extensionAmount = new int[]{0};
        VK10.vkEnumerateInstanceExtensionProperties("", this.extensionAmount, null);
        VK10.vkEnumerateInstanceExtensionProperties("", this.extensionAmount, this.availableExtensions = VkExtensionProperties.create(this.extensionAmount[0]));

        // TODO: Handle VkResult!!
        this.appInfo = VkApplicationInfo.create(1)
                .sType(VK10.VK_STRUCTURE_TYPE_APPLICATION_INFO)
                .pApplicationName(MemoryUtil.memUTF8("NoireTest"))
                .pEngineName(MemoryUtil.memUTF8("Noire"))
                .apiVersion(VK13.VK_API_VERSION_1_3)
                .engineVersion(VK10.VK_MAKE_VERSION(1, 0, 0))
                .applicationVersion(VK10.VK_MAKE_VERSION(1, 0, 0));
        this.instanceInfo = VkInstanceCreateInfo.create(1)
                .sType(VK10.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                .pApplicationInfo(this.appInfo.get(0))
                .ppEnabledExtensionNames(this.extensions)
                // validator is BROKEN!
                .ppEnabledLayerNames(this.layers)
                .get();
        vkCheckStatus(VK10.vkCreateInstance(this.instanceInfo, null, this.ptr = createPointerBuffer(1)));
        this.handle = new UtilsCInfo.Handle("Instance", this.ptr);

        //
        BasicObj.globalHandleMap.put$(this.handle.get(), this);
        this.instance = new VkInstance(this.handle.get(), this.instanceInfo);
        System.out.println("Something wrong with Instance? " + Long.toHexString(this.handle.get()));

        //
        /*vkCheckStatus(vkCreateDebugUtilsMessengerEXT(this.instance, VkDebugUtilsMessengerCreateInfoEXT.create()
                .sType(VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT)
                .messageSeverity(VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT )
                .messageType(VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_TYPE_DEVICE_ADDRESS_BINDING_BIT_EXT)
                .pfnUserCallback(callbackEXT = new VkDebugUtilsMessengerCallbackEXT() {
                    @Override
                    public int invoke(int messageSeverity, int messageTypes, long pCallbackData, long pUserData) {
                        var debugCallbackData = VkDebugUtilsMessengerCallbackDataEXT.create(pCallbackData);

                        //
                        var cmdLabels = debugCallbackData.pCmdBufLabels();
                        if (cmdLabels != null) {
                            cmdLabels.stream().forEach((labelEXT) -> {
                                System.out.println("Command Label: ");
                                System.out.println(labelEXT.pLabelNameString());
                            });
                        }

                        //
                        var queueLabels = debugCallbackData.pQueueLabels();
                        if (queueLabels != null) {
                            queueLabels.stream().forEach((labelEXT) -> {
                                System.out.println("Queue Label: ");
                                System.out.println(labelEXT.pLabelNameString());
                            });
                        }

                        //
                        var objects = debugCallbackData.pObjects();
                        if (objects != null) {
                            objects.stream().forEach((objectNameInfoEXT) -> {
                                var objectName = objectNameInfoEXT.pObjectNameString();
                                if (objectName != null) {
                                    System.out.println("Object Name: ");
                                    System.out.println(objectNameInfoEXT.pObjectNameString());
                                }
                            });
                        }

                        //
                        switch(messageSeverity) {
                            case VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT:
                                PrintMessageType(messageTypes);
                                System.out.println("Vulkan Debug Error!");
                                System.out.println(debugCallbackData.pMessageIdNameString());
                                System.out.println(debugCallbackData.pMessageString());

                                //if (messageTypes != VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT) {
                                throw new RuntimeException(debugCallbackData.pMessageString());
                                //}

                            case VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT:
                                PrintMessageType(messageTypes);
                                System.out.println("Vulkan Debug Warning!");
                                System.out.println(debugCallbackData.pMessageIdNameString());
                                System.out.println(debugCallbackData.pMessageString());
                            break;

                            case VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT:
                                PrintMessageType(messageTypes);
                                System.out.println("Vulkan Debug Info!");
                                System.out.println(debugCallbackData.pMessageIdNameString());
                                System.out.println(debugCallbackData.pMessageString());
                            break;

                            //case VK_DEBUG_UTILS_MESSAGE_SEVERITY_VERBOSE_BIT_EXT:
                                //System.out.println("Vulkan Debug Verbose!");
                                //System.out.println(debugCallbackData.pMessageIdNameString());
                                //System.out.println(debugCallbackData.pMessageString());
                            //break;
                        }
                        return 0;
                    }
                }), null, this.messagerEXT = createLongBuffer(1)));*/
    }

    //
    public ArrayList<PhysicalDeviceObj> enumeratePhysicalDevicesObj() {
        if (this.physicalDeviceAmount == null || this.physicalDeviceAmount[0] <= 0) {
            VK10.vkEnumeratePhysicalDevices(this.instance, this.physicalDeviceAmount = new int[]{0}, null);
            VK10.vkEnumeratePhysicalDevices(this.instance, this.physicalDeviceAmount, this.physicalDevices = createPointerBuffer(this.physicalDeviceAmount[0]));

            this.physicalDevicesObj = new ArrayList<PhysicalDeviceObj>();
            var Ps = this.physicalDeviceAmount.length;
            for (int I = 0; I < Ps; I++) {
                this.physicalDevicesObj.add(new PhysicalDeviceObj(this.handle, new UtilsCInfo.Handle("PhysicalDevice", this.physicalDevices.get(I))));
            }
        }
        return this.physicalDevicesObj;
    }

    //
    public PointerBuffer enumeratePhysicalDevices() {
        this.enumeratePhysicalDevicesObj();
        return this.physicalDevices;
    }

}
