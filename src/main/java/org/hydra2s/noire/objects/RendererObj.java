package org.hydra2s.noire.objects;

import org.hydra2s.noire.descriptors.*;
import org.hydra2s.utils.Generator;
import org.hydra2s.utils.Promise;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.io.IOException;
import java.nio.LongBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.Future;
import java.util.function.Function;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
import static org.lwjgl.vulkan.VK10.*;

// the main rendering class!!!
// TODO: finish up projecting
public class RendererObj extends BasicObj  {
    public PhysicalDeviceObj physicalDevice;
    public InstanceObj instance;
    public DeviceObj logicalDevice;
    public MemoryAllocatorObj memoryAllocator;
    public PipelineLayoutObj pipelineLayout;
    public WindowObj window;
    public SwapChainObj swapchain;
    public Generator<Integer> processor;

    public LongBuffer fences;
    public ArrayList<Promise<Integer>> promises = new ArrayList<Promise<Integer>>();
    public ArrayList<VkCommandBuffer> commandBuffers = new ArrayList<VkCommandBuffer>();
    public Iterator<Integer> process;
    public PipelineObj.ComputePipelineObj finalComp;


    //
    public RendererObj initializer() throws IOException {
        InstanceObj.globalHandleMap.put((this.handle = new Handle("Renderer", MemoryUtil.memAddress(memAllocLong(1)))).get(), this);

        //
        var instanceCInfo = new InstanceCInfo();
        this.instance = new InstanceObj(null, instanceCInfo);

        //
        var physicalDevices = instance.enumeratePhysicalDevicesObj();
        this.physicalDevice = physicalDevices.get(0);

        //
        var queueCInfo = new DeviceCInfo.QueueFamilyCInfo() {{
            index = 0;
            priorities = new float[]{1.0F};
        }};

        //
        this.logicalDevice = new DeviceObj(physicalDevice.getHandle(), new DeviceCInfo() {{
            queueFamilies.add(queueCInfo);
        }});
        this.memoryAllocator = new MemoryAllocatorObj(logicalDevice.getHandle(), new MemoryAllocatorCInfo(){{}});



        //
        var _memoryAllocator = this.memoryAllocator;
        this.pipelineLayout = new PipelineLayoutObj(logicalDevice.getHandle(), new PipelineLayoutCInfo(){{
            memoryAllocator = _memoryAllocator.getHandle().get();
        }});

        //
        var compSpv = Files.readAllBytes(Path.of("./shaders/final.comp.spv"));
        var _pipelineLayout = this.pipelineLayout;
        this.finalComp = new PipelineObj.ComputePipelineObj(logicalDevice.getHandle(), new PipelineCInfo.ComputePipelineCInfo(){{
            pipelineLayout = _pipelineLayout.getHandle().get();
            computeCode = memAlloc(compSpv.length).put(0, compSpv);
        }});

        //
        return this;
    }

    //
    public RendererObj submitOnce(Function<VkCommandBuffer, Integer> fx) {
        this.logicalDevice.submitOnce(logicalDevice.getCommandPool(0), new BasicCInfo.SubmitCmd(){{
            queue = logicalDevice.getQueue(0, 0);

        }}, fx);
        return this;
    }

    //
    public RendererObj pipelines() {



        return this;
    }

    //
    public RendererObj tickRendering () {
        this.logicalDevice.doPolling();

        //
        if (this.process != null && this.process.hasNext()) {
            this.process.next();
        } else {
            if (this.process != null) { this.process = null; };
            this.process = this.generate().iterator();
        }

        //
        return this;
    };

    public Generator<Integer> generate() {
        return (this.processor = new Generator<Integer>() {
            @Override
            protected void run() throws InterruptedException {
                var imageIndex = swapchain.acquireImageIndex(swapchain.semaphoreImageAvailable.get(0));
                var promise = promises.get(imageIndex);

                //
                //System.out.println("Is Rendering!");

                //
                do {
                    if (promise.state().equals(Future.State.RUNNING)) {
                        this.yield(VK_NOT_READY);
                    }
                } while(!promise.state().equals(Future.State.RUNNING));

                //
                var _queue = logicalDevice.getQueue(0, 0);
                logicalDevice.submitCommand(new BasicCInfo.SubmitCmd(){{
                    waitSemaphores = swapchain.semaphoreImageAvailable;
                    signalSemaphores = swapchain.semaphoreRenderingAvailable;
                    dstStageMask = memAllocInt(1).put(0, VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
                    queue = _queue;
                    cmdBuf = commandBuffers.get(imageIndex);
                    onDone = promises.get(imageIndex);
                }});
                promises.set(imageIndex, new Promise<>());
                swapchain.present(_queue, swapchain.semaphoreRenderingAvailable);
            }
        });
    }

    //
    public RendererObj rendering() {

        for (var I=0;I<this.swapchain.getImageCount();I++) {
            var cmdBuf = this.logicalDevice.allocateCommand(this.logicalDevice.getCommandPool(0));
            var pushConst = memAllocInt(2);
            pushConst.put(0, swapchain.getImageView(I).DSC_ID);

            // TODO: built-in command forming
            vkBeginCommandBuffer(cmdBuf, VkCommandBufferBeginInfo.create()
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                    .flags(VK_COMMAND_BUFFER_USAGE_SIMULTANEOUS_USE_BIT));

            //
            this.finalComp.cmdDispatch(cmdBuf, VkExtent3D.create().width(1280/32).height(720/6).depth(1), memByteBuffer(pushConst), 0);

            // TODO: built-in command forming
            vkEndCommandBuffer(cmdBuf);
            this.commandBuffers.add(cmdBuf);
        }




        // EXAMPLE!
        //while (!glfwWindowShouldClose(this.window.handle.get())) {
            //this.tickRendering();
        //}

        return this;
    }

    //
    public RendererObj prepare() {


        return this;
    }

    //
    public RendererObj windowed() {
        //
        var _pipelineLayout = pipelineLayout;
        var _memoryAllocator = memoryAllocator;

        //
        this.window = new WindowObj(this.instance.handle, new WindowCInfo(){{
            size = VkExtent2D.create().width(1280).height(720);
            pipelineLayout = _pipelineLayout.handle.get();
        }});

        //
        this.swapchain = new SwapChainObj(this.logicalDevice.handle, new SwapChainCInfo(){{
            pipelineLayout = _pipelineLayout.handle.get();
            queueFamilyIndex = 0;
            memoryAllocator = _memoryAllocator.handle.get();
            extent = window.getWindowSize();
            surface = window.getSurface();
        }});

        //
        this.fences = memAllocLong(this.swapchain.getImageCount());
        this.promises = new ArrayList<Promise<Integer>>();

        // EXAMPLE!
        for (var I=0;I<fences.capacity();I++) {
            vkCreateFence(logicalDevice.device, VkFenceCreateInfo.create().sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO).flags(VK_FENCE_CREATE_SIGNALED_BIT), null, fences.slice(I, 1));
            this.promises.add(new Promise<Integer>());
        }

        //
        this.submitOnce((cmdBuf)->{
            this.swapchain.imageViews.forEach((img)->{
                img.cmdTransitionBarrier(cmdBuf, VK_IMAGE_LAYOUT_PRESENT_SRC_KHR, true);
            });
            return 0;
        });

        //
        return this;
    }

    //
    public RendererObj(Handle base, Handle handle) throws IOException {
        super(base, handle);

        this.initializer();
        this.pipelines();
        this.windowed();
        this.prepare();
        this.rendering();
    }

    //
    public RendererObj(Handle base, RendererCInfo cInfo) throws IOException {
        super(base, cInfo);

        this.initializer();
        this.pipelines();
        this.windowed();
        this.prepare();
        this.rendering();
    }

}
