package org.hydra2s.noire.objects;

//

import org.hydra2s.noire.descriptors.UtilsCInfo;
import org.hydra2s.noire.descriptors.WindowCInfo;
import org.lwjgl.vulkan.VkExtent2D;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.hydra2s.noire.descriptors.UtilsCInfo.vkCheckStatus;
import static org.lwjgl.BufferUtils.createIntBuffer;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFWVulkan.glfwCreateWindowSurface;

//
public class WindowObj extends BasicObj  {
    public long[] surface = {};

    public WindowObj(UtilsCInfo.Handle base, UtilsCInfo.Handle handle) {
        super(base, handle);
    }

    // the NEW constructor (special for Minecraft)
    public WindowObj(UtilsCInfo.Handle base, long window) {
        super(base, new UtilsCInfo.Handle("Window", window));
        vkCheckStatus(glfwCreateWindowSurface(instanceObj.instance, this.handle.get(), null, (surface = new long[]{0L})));
    }

    public WindowObj(UtilsCInfo.Handle base, WindowCInfo cInfo) {
        super(base, cInfo);

        glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
        vkCheckStatus(glfwCreateWindowSurface(instanceObj.instance, (this.handle = new UtilsCInfo.Handle("Window", glfwCreateWindow(cInfo.size.width(), cInfo.size.height(), "NoireWindow", 0L, 0L))).get(), null, this.surface = new long[]{0L}));
        BasicObj.globalHandleMap.put$(handle.get(), this);
    }

    public FloatBuffer getDPI() {
        glfwGetWindowContentScale(this.handle.get(), ((WindowCInfo)this.cInfo).scale.slice(0, 1), ((WindowCInfo)this.cInfo).scale.slice(1, 1));
        return ((WindowCInfo)this.cInfo).scale;
    }

    public long getWindow() {
        return this.handle.get();
    }

    public long getSurface() {
        return this.surface[0];
    }

    public VkExtent2D getWindowSize() {
        IntBuffer size = createIntBuffer(2);
        glfwGetWindowSize(this.handle.get(), size.slice(0, 1), size.slice(1, 1));
        var extent = VkExtent2D.create().width(size.get(0)).height(size.get(1));
        if (this.cInfo != null) { ((WindowCInfo)this.cInfo).size = extent; };
        return extent;
    }
}
