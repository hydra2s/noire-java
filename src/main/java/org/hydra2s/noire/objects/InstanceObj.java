package org.hydra2s.noire.objects;

//
import org.hydra2s.noire.descriptors.InstanceCInfo;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

//
import java.nio.IntBuffer;
import java.util.ArrayList;

//
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.glfwVulkanSupported;
import static org.lwjgl.system.MemoryUtil.memAllocInt;

// An America!
public class InstanceObj extends BasicObj {
    public InstanceObj(Handle base, Handle handle) {
        super(base, handle);
    }

    //
    public VkInstance instance = null;

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
    protected IntBuffer extensionAmount = memAllocInt(1).put(0, 0);
    protected IntBuffer layersAmount = memAllocInt(1).put(0, 0);
    //
    protected IntBuffer physicalDeviceAmount = memAllocInt(1).put(0, 0);

    //
    public InstanceObj(Handle base, InstanceCInfo cInfo) {
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
        this.layers = PointerBuffer.allocateDirect(1);
        this.layers.put(0, MemoryUtil.memAddress(MemoryUtil.memUTF8("VK_LAYER_KHRONOS_validation")));

        //
        this.layersAmount = memAllocInt(1);
        VK10.vkEnumerateInstanceLayerProperties(this.layersAmount, null);
        VK10.vkEnumerateInstanceLayerProperties(this.layersAmount, this.availableLayers = VkLayerProperties.calloc(this.layersAmount.get(0)));

        // Extensions
        this.glfwExt = GLFWVulkan.glfwGetRequiredInstanceExtensions();
        this.extensions = PointerBuffer.allocateDirect(this.glfwExt.limit() + 1);
        this.extensions.put(0, MemoryUtil.memAddress(MemoryUtil.memUTF8("VK_KHR_get_surface_capabilities2")));
        for (int i=0;i<this.glfwExt.limit();i++) {
            this.extensions.put(i+1, MemoryUtil.memAddress(MemoryUtil.memUTF8(this.glfwExt.getStringUTF8(i))));
        }

        //
        this.extensionAmount = memAllocInt(1);
        VK10.vkEnumerateInstanceExtensionProperties("", this.extensionAmount, null);
        VK10.vkEnumerateInstanceExtensionProperties("", this.extensionAmount, this.availableExtensions = VkExtensionProperties.calloc(this.extensionAmount.get(0)));

        // TODO: Handle VkResult!!
        this.appInfo = VkApplicationInfo.calloc(1)
                .sType(VK10.VK_STRUCTURE_TYPE_APPLICATION_INFO)
                .pApplicationName(MemoryUtil.memUTF8("NoireTest"))
                .pEngineName(MemoryUtil.memUTF8("Noire"))
                .apiVersion(VK13.VK_API_VERSION_1_3)
                .engineVersion(VK10.VK_MAKE_VERSION(1, 0, 0))
                .applicationVersion(VK10.VK_MAKE_VERSION(1, 0, 0));
        this.instanceInfo = VkInstanceCreateInfo.calloc(1)
                .sType(VK10.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                .pApplicationInfo(this.appInfo.get(0))
                .ppEnabledExtensionNames(this.extensions)
                .ppEnabledLayerNames(this.layers).get();
        VK10.vkCreateInstance(this.instanceInfo, null, (this.handle = new Handle("Instance")).ptr());

        //
        BasicObj.globalHandleMap.put(this.handle.get(), this);
        this.instance = new VkInstance(this.handle.get(), this.instanceInfo);
        System.out.println("Something wrong with Instance? " + Long.toHexString(this.handle.get()));
    }

    //
    public ArrayList<PhysicalDeviceObj> enumeratePhysicalDevicesObj() {
        if (this.physicalDeviceAmount == null || this.physicalDeviceAmount.get(0) <= 0) {
            VK10.vkEnumeratePhysicalDevices(this.instance, this.physicalDeviceAmount = memAllocInt(1), null);
            VK10.vkEnumeratePhysicalDevices(this.instance, this.physicalDeviceAmount, this.physicalDevices = PointerBuffer.allocateDirect(1));

            this.physicalDevicesObj = new ArrayList<PhysicalDeviceObj>();
            for (int I = 0; I < this.physicalDeviceAmount.remaining(); I++) {
                this.physicalDevicesObj.add(new PhysicalDeviceObj(this.handle, new Handle("PhysicalDevice", this.physicalDevices.get(I))));
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
