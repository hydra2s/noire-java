package org.hydra2s.manhack.objects;

//
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;

//
import org.bytedeco.javacpp.tools.PointerBufferPoolMXBean;
import org.hydra2s.manhack.descriptors.InstanceCInfo;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.*;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

// An America!
public class InstanceObj extends BasicObj  {
    public InstanceObj(Handle base, Handle handle) {
        super(base, handle);
    }

    //
    public VkInstance instance;

    //
    protected VkExtensionProperties.Buffer availableExtensions = null;
    protected PointerBuffer extensions = null;
    protected IntBuffer extensionAmount = null;

    //
    protected VkLayerProperties.Buffer availableLayers = null;
    protected PointerBuffer layers = null;
    protected IntBuffer layersAmount = null;

    //
    protected PointerBuffer glfwExt = null;
    protected VkInstanceCreateInfo instanceInfo = null;
    protected VkApplicationInfo.Buffer appInfo = null;

    //
    public InstanceObj(Handle base, InstanceCInfo cInfo) {
        super(base, cInfo);

        // Layers
        this.layers = PointerBuffer.allocateDirect(1);
        this.layers.put(0, MemoryUtil.memAddress(MemoryUtil.memASCII("VK_LAYER_KHRONOS_validation")));

        //
        this.layersAmount = IntBuffer.allocate(1);
        VK10.vkEnumerateInstanceLayerProperties(this.layersAmount, null);
        VK10.vkEnumerateInstanceLayerProperties(this.layersAmount, this.availableLayers = VkLayerProperties.create(this.layersAmount.get(0)));

        // Extensions
        this.extensions = PointerBuffer.allocateDirect(this.glfwExt.limit()+1);
        this.extensions.put(0, MemoryUtil.memAddress(MemoryUtil.memASCII("VK_KHR_get_surface_capabilities2")));
        for (int i=0;i<this.glfwExt.limit();i++) {
            this.extensions.put(i+1, MemoryUtil.memAddress(MemoryUtil.memASCII(this.glfwExt.getStringUTF8(i))));
        }

        //
        this.glfwExt = GLFWVulkan.glfwGetRequiredInstanceExtensions();
        this.extensionAmount = IntBuffer.allocate(1);
        VK10.vkEnumerateInstanceExtensionProperties("", this.extensionAmount, null);
        VK10.vkEnumerateInstanceExtensionProperties("", this.extensionAmount, this.availableExtensions = VkExtensionProperties.create(this.extensionAmount.get(0)));

        // TODO: Handle VkResult!!
        VK10.vkCreateInstance(this.instanceInfo = VkInstanceCreateInfo.create(1)
            .pApplicationInfo((this.appInfo = VkApplicationInfo.create(1)
                .sType(VK10.VK_STRUCTURE_TYPE_APPLICATION_INFO)
                .pApplicationName(MemoryUtil.memASCII("ManhackTest"))
                .pEngineName(MemoryUtil.memASCII("Manhack"))
                .apiVersion(VK13.VK_API_VERSION_1_3)
                .engineVersion(VK10.VK_MAKE_VERSION(1, 0, 0))
                .applicationVersion(VK10.VK_MAKE_VERSION(1, 0, 0))
            ).get())
            .ppEnabledExtensionNames(this.extensions)
            .ppEnabledLayerNames(this.layers).get(), null, (this.handle = new Handle(0)).ptr());
        
        //
        BasicObj.globalHandleMap.put(this.handle.get(), this);
        this.instance = new VkInstance(this.handle.get(), this.instanceInfo);
    }
}
