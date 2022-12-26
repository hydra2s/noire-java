package org.hydra2s;

//
import org.hydra2s.noire.descriptors.*;
import org.hydra2s.noire.objects.*;

//
import java.io.IOException;

import static org.lwjgl.glfw.GLFW.*;


//
public class Main {

    public static void main(String[] args) throws IOException {
        var renderer = new RendererObj(null, new RendererCInfo());

        // EXAMPLE!
        while (!glfwWindowShouldClose(renderer.window.getHandle().get())) {
            glfwPollEvents();
            renderer.tickRendering();
        }
    }

}
