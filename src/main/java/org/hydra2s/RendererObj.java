package org.hydra2s;

import org.hydra2s.noire.descriptors.*;
import org.hydra2s.noire.objects.*;
import org.hydra2s.utils.Generator;
import org.hydra2s.utils.Promise;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.io.IOException;
import java.nio.LongBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.Future;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.KHRAccelerationStructure.VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR;
import static org.lwjgl.vulkan.KHRAccelerationStructure.VK_BUILD_ACCELERATION_STRUCTURE_MODE_BUILD_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
import static org.lwjgl.vulkan.VK10.*;

// the main rendering class!!!
// TODO: finish up projecting
public class RendererObj extends BasicObj {
    public PhysicalDeviceObj physicalDevice;
    public InstanceObj instance;
    public DeviceObj logicalDevice;
    public MemoryAllocatorObj memoryAllocator;
    public PipelineLayoutObj pipelineLayout;
    public WindowObj window;
    public SwapChainObj swapchain;
    public Generator<Integer> processor;

    public LongBuffer fences;
    public ArrayList<DeviceObj.FenceProcess> promises = new ArrayList<DeviceObj.FenceProcess>();
    public ArrayList<VkCommandBuffer> commandBuffers = new ArrayList<VkCommandBuffer>();
    public Iterator<Integer> process;
    public PipelineObj.ComputePipelineObj finalComp;
    public PipelineObj.GraphicsPipelineObj trianglePipeline;
    public ImageSetCInfo.FBLayout fbLayout;
    public ImageSetObj.FramebufferObj framebuffer;
    public AccelerationStructureObj.TopAccelerationStructureObj topLvl;
    public AccelerationStructureObj.BottomAccelerationStructureObj bottomLvl;

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
        return this;
    }

    //
    public RendererObj createPipelines() throws IOException {
        var finalCompSpv = Files.readAllBytes(Path.of("./shaders/final.comp.spv"));
        var _pipelineLayout = this.pipelineLayout;
        var _memoryAllocator = memoryAllocator;

        //
        this.fbLayout = new ImageSetCInfo.FBLayout(){{
            memoryAllocator = _memoryAllocator.getHandle().get();
            pipelineLayout = _pipelineLayout.getHandle().get();

            extents = new ArrayList<>(){{
                add(VkExtent3D.calloc().width(1280).height(720).depth(1));
            }};
            formats = memAllocInt(1).put(0, VK_FORMAT_R32G32B32A32_SFLOAT);
            layerCounts = new ArrayList<>(){{
                add(1);
            }};

            //
            blendStates = new ArrayList<>(){{
                add(new BlendState(false, VK_BLEND_FACTOR_SRC_ALPHA, VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA, VK_BLEND_FACTOR_ONE, VK_BLEND_FACTOR_ZERO));
            }};

            //
            attachmentInfos = VkRenderingAttachmentInfo.calloc(1);
            attachmentInfos.get(0)
                    .clearValue(VkClearValue.calloc().color(VkClearColorValue.calloc()
                            .float32(memAllocFloat(4)
                                    .put(0, 0.0F)
                                    .put(1, 0.0F)
                                    .put(2, 0.0F)
                                    .put(3, 1.0F)
                            )));

            // TODO: support only depth or only stencil
            depthStencilAttachmentInfo = VkRenderingAttachmentInfo.calloc()
                    .clearValue(VkClearValue.calloc().depthStencil(VkClearDepthStencilValue.calloc()
                            .depth(1.0F)
                            .stencil(0)));

            // TODO: support only depth or only stencil
            depthStencilFormat = VK_FORMAT_D32_SFLOAT_S8_UINT;
            scissor = VkRect2D.calloc()
                    .extent(VkExtent2D.calloc().width(1280).height(720))
                    .offset(VkOffset2D.calloc().x(0).y(0));
            viewport = VkViewport.calloc()
                    .x(0.F).y(0.F)
                    .width(1280.F).height(720.F)
                    .minDepth(0.F).maxDepth(1.F);
        }};

        //
        this.framebuffer = new ImageSetObj.FramebufferObj(this.logicalDevice.getHandle(), this.fbLayout);

        //
        var _fbLayout = this.fbLayout;

        //
        this.finalComp = new PipelineObj.ComputePipelineObj(logicalDevice.getHandle(), new PipelineCInfo.ComputePipelineCInfo(){{
            memoryAllocator = _memoryAllocator.getHandle().get();
            pipelineLayout = _pipelineLayout.getHandle().get();
            computeCode = memAlloc(finalCompSpv.length).put(0, finalCompSpv);
        }});

        //
        var fragSpv = Files.readAllBytes(Path.of("./shaders/triangle.frag.spv"));
        var vertSpv = Files.readAllBytes(Path.of("./shaders/triangle.vert.spv"));
        this.trianglePipeline = new PipelineObj.GraphicsPipelineObj(logicalDevice.getHandle(), new PipelineCInfo.GraphicsPipelineCInfo(){{
            memoryAllocator = _memoryAllocator.getHandle().get();
            pipelineLayout = _pipelineLayout.getHandle().get();
            fbLayout = _fbLayout;
            sourceMap = new HashMap<>(){{
                put(VK_SHADER_STAGE_FRAGMENT_BIT, memAlloc(fragSpv.length).put(0, fragSpv));
                put(VK_SHADER_STAGE_VERTEX_BIT, memAlloc(vertSpv.length).put(0, vertSpv));
            }};

        }});

        return this;
    }

    //
    public RendererObj createAccelerationStructures() {
        var _pipelineLayout = this.pipelineLayout;
        var _memoryAllocator = memoryAllocator;

        //
        var triangleBuffer = new BufferObj(this.logicalDevice.getHandle(), new BufferCInfo(){{
            size = 16 * 3;
            usage = VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT | VK_BUFFER_USAGE_STORAGE_BUFFER_BIT;
            memoryAllocator = _memoryAllocator.getHandle().get();
            memoryAllocationInfo = new MemoryAllocationCInfo(){{
                isHost = true;
                isDevice = true;
            }};
        }});

        //
        var tmapped = triangleBuffer.map(16*3, 0);
        tmapped.asFloatBuffer().put(0, new float[]{
                0.5F, -0.5F, 0.F, 1.F,
                -0.5F, -0.5F, 0.F, 1.F,
                0.0F,  0.5F, 0.F, 1.F
        });
        triangleBuffer.unmap();

        //
        this.bottomLvl = new AccelerationStructureObj.BottomAccelerationStructureObj(this.logicalDevice.getHandle(), new AccelerationStructureCInfo.BottomAccelerationStructureCInfo(){{
            memoryAllocator = _memoryAllocator.getHandle().get();
            geometries = new ArrayList<DataCInfo.TriangleGeometryCInfo>(){{
                add(new DataCInfo.TriangleGeometryCInfo(){{
                    vertexBinding = new DataCInfo.VertexBindingCInfo(){{
                        address = triangleBuffer.getDeviceAddress();
                        stride = 16;
                        format = VK_FORMAT_R32G32B32_SFLOAT;
                        vertexCount = 3;
                    }};
                }});
            }};
        }});

        //
        var instanceBuffer = new BufferObj(this.logicalDevice.getHandle(), new BufferCInfo(){{
            size = VkAccelerationStructureInstanceKHR.SIZEOF;
            usage = VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT | VK_BUFFER_USAGE_STORAGE_BUFFER_BIT;
            memoryAllocator = _memoryAllocator.getHandle().get();
            memoryAllocationInfo = new MemoryAllocationCInfo(){{
                isHost = true;
                isDevice = true;
            }};
        }});

        //
        var imapped = instanceBuffer.map(VkAccelerationStructureInstanceKHR.SIZEOF, 0);
        var instanceInfo = VkAccelerationStructureInstanceKHR.create(memAddress(imapped));
        instanceInfo.mask(0xFF);
        instanceInfo.accelerationStructureReference(this.bottomLvl.getDeviceAddress());
        instanceInfo.flags(0);
        instanceInfo.transform(VkTransformMatrixKHR.calloc().matrix(memAllocFloat(12).put(0, new float[]{
                1.0F, 0.0F, 0.0F, 0.0F,
                0.0F, 1.0F, 0.0F, 0.0F,
                0.0F, 0.0F, 1.0F, 0.0F
        })));
        instanceBuffer.unmap();

        this.topLvl = new AccelerationStructureObj.TopAccelerationStructureObj(this.logicalDevice.getHandle(), new AccelerationStructureCInfo.TopAccelerationStructureCInfo(){{
            memoryAllocator = _memoryAllocator.getHandle().get();
            instances = new DataCInfo.InstanceGeometryCInfo(){{
                instanceBinding = new DataCInfo.InstanceBindingCInfo(){{
                    address = instanceBuffer.getDeviceAddress();
                    vertexCount = 1;
                }};
            }};
        }});

        //
        this.logicalDevice.submitOnce(logicalDevice.getCommandPool(0), new BasicCInfo.SubmitCmd(){{
            queue = logicalDevice.getQueue(0, 0);
        }}, (cmdBuf)->{
            triangleBuffer.cmdSynchronizeFromHost(cmdBuf);
            this.bottomLvl.cmdBuild(cmdBuf, VkAccelerationStructureBuildRangeInfoKHR.calloc(1)
                            .primitiveCount(1)
                            .firstVertex(0)
                            .primitiveOffset(0)
                            .transformOffset(0),
                    VK_BUILD_ACCELERATION_STRUCTURE_MODE_BUILD_KHR);

            instanceBuffer.cmdSynchronizeFromHost(cmdBuf);
            this.topLvl.cmdBuild(cmdBuf, VkAccelerationStructureBuildRangeInfoKHR.calloc(1)
                            .primitiveCount(1)
                            .firstVertex(0)
                            .primitiveOffset(0)
                            .transformOffset(0),
                    VK_BUILD_ACCELERATION_STRUCTURE_MODE_BUILD_KHR);

            return VK_SUCCESS;
        });

        //
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
            this.process = this.generateRenderThread().iterator();
        }

        //
        return this;
    };

    public Generator<Integer> generateRenderThread() {
        return (this.processor = new Generator<Integer>() {
            @Override
            protected void run() throws InterruptedException {
                var imageIndex = swapchain.acquireImageIndex(swapchain.semaphoreImageAvailable.getHandle().get());
                var promise = promises.get(imageIndex);

                //
                while (promise.promise.state().equals(Future.State.RUNNING)) {
                    this.yield(VK_NOT_READY);
                };

                //
                var _queue = logicalDevice.getQueue(0, 0);
                promises.set(imageIndex, logicalDevice.submitCommand(new BasicCInfo.SubmitCmd(){{
                    waitSemaphores = memLongBuffer(memAddress(swapchain.semaphoreImageAvailable.getHandle().ptr(), 0), 1);
                    signalSemaphores = memLongBuffer(memAddress(swapchain.semaphoreRenderingAvailable.getHandle().ptr(), 0), 1);
                    dstStageMask = memAllocInt(1).put(0, VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT);
                    queue = _queue;
                    cmdBuf = commandBuffers.get(imageIndex);
                }}));
                swapchain.present(_queue, memLongBuffer(memAddress(swapchain.semaphoreRenderingAvailable.getHandle().ptr(), 0), 1));
            }
        });
    }

    //
    public RendererObj createResultCommands() {

        for (var I=0;I<this.swapchain.getImageCount();I++) {
            var cmdBuf = this.logicalDevice.allocateCommand(this.logicalDevice.getCommandPool(0));
            var pushConst = memAllocInt(4);
            pushConst.put(0, swapchain.getImageView(I).DSC_ID);
            pushConst.put(1, framebuffer.writingImageViews.get(0).DSC_ID);
            memLongBuffer(memAddress(pushConst, 2), 1).put(0, this.topLvl.getDeviceAddress());

            //
            this.logicalDevice.writeCommand(cmdBuf, (_cmdBuf_)->{
                this.trianglePipeline.cmdDraw(cmdBuf, new PipelineObj.GraphicsDrawInfo() {{
                    multiDraw = VkMultiDrawInfoEXT.calloc(1).put(0, VkMultiDrawInfoEXT.calloc().vertexCount(3).firstVertex(0));
                    pushConstRaw = memByteBuffer(pushConst);
                    imageSet = framebuffer.getHandle().get();
                }});
                this.finalComp.cmdDispatch(cmdBuf, new PipelineObj.ComputeDispatchInfo(){{
                    dispatch = VkExtent3D.calloc().width(1280/32).height(720/6).depth(1);
                    pushConstRaw = memByteBuffer(pushConst);
                }});
                return VK_SUCCESS;
            });

            //
            this.commandBuffers.add(cmdBuf);
        }

        return this;
    }

    //
    public RendererObj prepareToRendering() {

        return this;
    }

    //
    public RendererObj createWindow() {
        var _pipelineLayout = pipelineLayout;
        var _memoryAllocator = memoryAllocator;

        //
        this.window = new WindowObj(this.instance.getHandle(), new WindowCInfo(){{
            size = VkExtent2D.calloc().width(1280).height(720);
            pipelineLayout = _pipelineLayout.getHandle().get();
        }});

        //
        this.swapchain = new SwapChainObj(this.logicalDevice.getHandle(), new SwapChainCInfo(){{
            pipelineLayout = _pipelineLayout.getHandle().get();
            queueFamilyIndex = 0;
            memoryAllocator = _memoryAllocator.getHandle().get();
            extent = window.getWindowSize();
            surface = window.getSurface();
        }});

        //
        this.fences = memAllocLong(this.swapchain.getImageCount());
        this.promises = new ArrayList<DeviceObj.FenceProcess>();

        // EXAMPLE!
        for (var I=0;I<fences.remaining();I++) {
            this.promises.add(this.logicalDevice.makeSignaled());
        }

        //
        this.logicalDevice.submitOnce(logicalDevice.getCommandPool(0), new BasicCInfo.SubmitCmd(){{
            queue = logicalDevice.getQueue(0, 0);
        }}, (cmdBuf)->{
            this.swapchain.imageViews.forEach((img)->{
                img.cmdTransitionBarrier(cmdBuf, VK_IMAGE_LAYOUT_PRESENT_SRC_KHR, true);
            });
            this.framebuffer.processCurrentImageViews((img)->{
                img.cmdTransitionBarrier(cmdBuf, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL, true);
                return 0;
            });
            this.framebuffer.processWritingImageViews((img)->{
                img.cmdTransitionBarrier(cmdBuf, VK_IMAGE_LAYOUT_GENERAL, true);
                return 0;
            });
            this.framebuffer.currentDepthStencilImageView.cmdTransitionBarrier(cmdBuf, VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL, true);
            return 0;
        });

        //
        return this;
    }

    //
    public RendererObj(Handle base, Handle handle) throws IOException {
        super(base, handle);

        this.initializer();
        this.createPipelines();
        this.createWindow();
        this.prepareToRendering();
        this.createAccelerationStructures();
        this.createResultCommands();
    }

    //
    public RendererObj(Handle base, RendererCInfo cInfo) throws IOException {
        super(base, cInfo);

        this.initializer();
        this.createPipelines();
        this.createWindow();
        this.prepareToRendering();
        this.createAccelerationStructures();
        this.createResultCommands();
    }

}
