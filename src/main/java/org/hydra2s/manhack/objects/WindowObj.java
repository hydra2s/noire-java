package org.hydra2s.manhack.objects;

//
import org.hydra2s.manhack.descriptors.WindowCInfo;
import org.lwjgl.vulkan.VkExtent2D;

//
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

//
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface;
import static org.lwjgl.system.MemoryUtil.*;

//
public class WindowObj extends BasicObj  {
    public LongBuffer surface = memAllocLong(1);

    public WindowObj(Handle base, Handle handle) {
        super(base, handle);
    }

    public WindowObj(Handle base, WindowCInfo cInfo) {
        super(base, cInfo);

        var instanceObj = (InstanceObj) BasicObj.globalHandleMap.get(base.get());

        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        glfwCreateWindowSurface(instanceObj.instance, (this.handle = new Handle("Window", glfwCreateWindow(cInfo.size.width(), cInfo.size.height(), "ManhackWindow", 0L, 0L))).get(), null, this.surface = memAllocLong(1));
        BasicObj.globalHandleMap.put(handle.get(), this);
    }

    public FloatBuffer getDPI() {
        glfwGetWindowContentScale(this.handle.get(), ((WindowCInfo)this.cInfo).scale.slice(0, 1), ((WindowCInfo)this.cInfo).scale.slice(1, 1));
        return ((WindowCInfo)this.cInfo).scale;
    }

    public long getWindow() {
        return this.handle.get();
    }

    public long getSurface() {
        return this.surface.get(0);
    }

    public VkExtent2D getWindowSize() {
        IntBuffer size = memAllocInt(2);
        glfwGetWindowSize(this.handle.get(), size.slice(0, 1), size.slice(1, 1));
        return (((WindowCInfo)this.cInfo).size = VkExtent2D.create().width(size.get(0)).height(size.get(1)));
    }
}
