package org.hydra2s.noire.objects;

//
import org.hydra2s.noire.descriptors.BasicCInfo;
import org.hydra2s.noire.descriptors.ImageViewCInfo;
import org.hydra2s.noire.descriptors.MemoryAllocationCInfo;
import org.hydra2s.noire.descriptors.SwapChainCInfo;
import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.*;

//
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.KHRExternalMemoryWin32.vkGetMemoryWin32HandleKHR;
import static org.lwjgl.vulkan.KHRExternalSemaphoreWin32.VK_STRUCTURE_TYPE_SEMAPHORE_GET_WIN32_HANDLE_INFO_KHR;
import static org.lwjgl.vulkan.KHRExternalSemaphoreWin32.vkGetSemaphoreWin32HandleKHR;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK11.*;

//
public class SwapChainObj extends BasicObj  {
    public LongBuffer images = null;
    public IntBuffer amountOfImagesInSwapchain = memAllocInt(1);
    //
    public ArrayList<Integer> presentModes = new ArrayList<>(Arrays.asList(VK_PRESENT_MODE_IMMEDIATE_KHR, VK_PRESENT_MODE_FIFO_RELAXED_KHR, VK_PRESENT_MODE_FIFO_KHR, VK_PRESENT_MODE_MAILBOX_KHR));
    public ArrayList<Integer> surfaceFormats = new ArrayList<>(Arrays.asList(VK_FORMAT_A2B10G10R10_UNORM_PACK32, VK_FORMAT_B8G8R8A8_UNORM));
    public VkSwapchainCreateInfoKHR createInfo = null;
    public VkImageViewCreateInfo imageViewInfo = null;

    //
    public ArrayList<MemoryAllocationObj.ImageObj> imagesObj = new ArrayList<MemoryAllocationObj.ImageObj>();
    public ArrayList<ImageViewObj> imageViews = new ArrayList<ImageViewObj>();

    //
    public LongBuffer semaphoreImageAvailable = null;
    public LongBuffer semaphoreRenderingAvailable = null;
    public IntBuffer imageIndex = memAllocInt(1).put(0,0);

    //
    public PointerBuffer SemImageWin32Handle = memAllocPointer(1).put(0, 0);
    public PointerBuffer SemRenderWin32Handle = memAllocPointer(1).put(0, 0);

    //
    public SwapChainObj(Handle base, Handle handle) {
        super(base, handle);
    }

    public SwapChainObj generateImages(SwapChainCInfo cInfo) {
        //
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get());
        var physicalDeviceObj = (PhysicalDeviceObj)BasicObj.globalHandleMap.get(deviceObj.base.get());
        var descriptorsObj = (PipelineLayoutObj)deviceObj.handleMap.get(new Handle("PipelineLayout", cInfo.pipelineLayout));
        var surfaceInfo = physicalDeviceObj.getSurfaceInfo(cInfo.surface, cInfo.queueFamilyIndex);

        //
        var presentMode = presentModes.get(0);
        for (var I=0;I<presentModes.size();I++) { var PM = presentModes.get(I);
            if (List.of(surfaceInfo.presentModes).contains(PM)) {
                presentMode = PM; break;
            }
        }

        //
        var format = surfaceFormats.get(0);
        var colorSpace = VK_COLOR_SPACE_SRGB_NONLINEAR_KHR;

        //
        for (var I=0;I<surfaceFormats.size();I++) { var F = surfaceFormats.get(I);
            Integer finalFormat = format;
            var ID = surfaceInfo.formats2.stream().toList().indexOf(surfaceInfo.formats2.stream().filter((F2)->(F2.surfaceFormat().format() == finalFormat)).findFirst().orElse(null));
            if (ID >= 0) {
                format = surfaceInfo.formats2.get(ID).surfaceFormat().format();
                colorSpace = surfaceInfo.formats2.get(ID).surfaceFormat().colorSpace();
                break;
            }
        }

        //
        this.createInfo = VkSwapchainCreateInfoKHR.create()
            .sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
            .surface(cInfo.surface)
            .minImageCount(surfaceInfo.capabilities2.surfaceCapabilities().maxImageCount())
            .imageFormat(format)
            .imageColorSpace(colorSpace)
            .imageExtent(cInfo.extent)
            .imageArrayLayers(cInfo.layerCount)
            .imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT | VK_IMAGE_USAGE_STORAGE_BIT | VK_IMAGE_USAGE_SAMPLED_BIT | VK_IMAGE_USAGE_TRANSFER_SRC_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT)
            .imageSharingMode(VK_SHARING_MODE_EXCLUSIVE)
            .pQueueFamilyIndices(cInfo.queueFamilyIndices != null ? cInfo.queueFamilyIndices : memAllocInt(1).put(0, cInfo.queueFamilyIndex))
            .preTransform(surfaceInfo.capabilities2.surfaceCapabilities().currentTransform())
            .compositeAlpha(surfaceInfo.capabilities2.surfaceCapabilities().supportedCompositeAlpha())
            .presentMode(presentMode)
            .clipped(true)
            .oldSwapchain(0L);

        //
        vkCreateSwapchainKHR(deviceObj.device, this.createInfo, null, memLongBuffer(memAddress((this.handle = new Handle("SwapChain")).ptr(), 0), 1));
        vkGetSwapchainImagesKHR(deviceObj.device, this.handle.get(), this.amountOfImagesInSwapchain = memAllocInt(1), null);
        vkGetSwapchainImagesKHR(deviceObj.device, this.handle.get(), this.amountOfImagesInSwapchain, this.images = memAllocLong(this.amountOfImagesInSwapchain.get(0)));
        this.imagesObj = new ArrayList<>();

        //
        for (var I=0;I<this.amountOfImagesInSwapchain.get(0);I++) {
            var allocationCInfo = new MemoryAllocationCInfo() {{
                isHost = false;
                isDevice = true;
            }};

            var finalI = I;
            var imageCInfo = new MemoryAllocationCInfo.ImageCInfo(){{
                image = memAllocPointer(1).put(0, images.get(finalI));
                arrayLayers = createInfo.imageArrayLayers();
                format = createInfo.imageFormat();
                mipLevels = 1;
                extent3D = VkExtent3D.create().width(createInfo.imageExtent().width()).height(createInfo.imageExtent().height()).depth(1);
                tiling = VK_IMAGE_TILING_OPTIMAL;
                samples = VK_SAMPLE_COUNT_1_BIT;
                usage = VK_IMAGE_USAGE_SAMPLED_BIT | VK_IMAGE_USAGE_STORAGE_BIT | VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
            }};

            //
            var imageViewCInfo = new ImageViewCInfo(){{
                image = images.get(finalI);
                subresourceRange = VkImageSubresourceRange.create().layerCount(1).baseArrayLayer(0).levelCount(1).baseMipLevel(0).aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                pipelineLayout = cInfo.pipelineLayout;
                imageLayout = VK_IMAGE_LAYOUT_GENERAL;
                type = "storage";
            }};

            //
            this.imagesObj.add(new MemoryAllocationObj.ImageObj(this.base, imageCInfo));
            this.imageViews.add(new ImageViewObj(this.base, imageViewCInfo));
        }

        return this;
    }

    //
    public SwapChainObj generateSemaphores() {
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get());

        // useless for pure Vulkan, for test only
        // TODO: semaphore objects
        vkCreateSemaphore(deviceObj.device, VkSemaphoreCreateInfo.create().pNext(VkExportSemaphoreCreateInfoKHR.create().sType(VK_STRUCTURE_TYPE_EXPORT_SEMAPHORE_CREATE_INFO).handleTypes(VK_EXTERNAL_SEMAPHORE_HANDLE_TYPE_OPAQUE_WIN32_BIT ).address()).sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO), null, semaphoreImageAvailable = memAllocLong(1));
        vkCreateSemaphore(deviceObj.device, VkSemaphoreCreateInfo.create().pNext(VkExportSemaphoreCreateInfoKHR.create().sType(VK_STRUCTURE_TYPE_EXPORT_SEMAPHORE_CREATE_INFO).handleTypes(VK_EXTERNAL_SEMAPHORE_HANDLE_TYPE_OPAQUE_WIN32_BIT ).address()).sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO), null, semaphoreRenderingAvailable = memAllocLong(1));

        // TODO: semaphore objects
        // TODO: Linux support
        vkGetSemaphoreWin32HandleKHR(deviceObj.device, VkSemaphoreGetWin32HandleInfoKHR.create().sType(VK_STRUCTURE_TYPE_SEMAPHORE_GET_WIN32_HANDLE_INFO_KHR).semaphore(this.semaphoreImageAvailable.get(0)).handleType(VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_WIN32_BIT), this.SemImageWin32Handle = memAllocPointer(1));
        vkGetSemaphoreWin32HandleKHR(deviceObj.device, VkSemaphoreGetWin32HandleInfoKHR.create().sType(VK_STRUCTURE_TYPE_SEMAPHORE_GET_WIN32_HANDLE_INFO_KHR).semaphore(this.semaphoreRenderingAvailable.get(0)).handleType(VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_WIN32_BIT), this.SemRenderWin32Handle = memAllocPointer(1));

        //
        System.out.println(this.SemImageWin32Handle.get(0));
        System.out.println(this.SemRenderWin32Handle.get(0));

        //
        return this;
    }

    //
    public SwapChainObj(Handle base, SwapChainCInfo cInfo) {
        super(base, cInfo);

        //
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get());
        var physicalDeviceObj = (PhysicalDeviceObj)BasicObj.globalHandleMap.get(deviceObj.base.get());
        var descriptorsObj = (PipelineLayoutObj)deviceObj.handleMap.get(new Handle("PipelineLayout", cInfo.pipelineLayout));
        var surfaceInfo = physicalDeviceObj.getSurfaceInfo(cInfo.surface, cInfo.queueFamilyIndex);

        //
        this.imageIndex = memAllocInt(1).put(0,0);
        this.generateImages(cInfo);
        this.generateSemaphores();
    }

    //
    public int getFormat() { return this.createInfo.imageFormat(); }
    public int getImageCount() { return this.images.capacity(); }
    public int getColorSpace() { return this.createInfo.imageColorSpace(); }
    public LongBuffer getImages() { return this.images; }
    public ArrayList<MemoryAllocationObj.ImageObj> getImagesObj() { return this.imagesObj; }
    public ArrayList<ImageViewObj> getImageViews() { return this.imageViews; }
    public long getImage(int index) { return this.images.get(index); }
    public MemoryAllocationObj.ImageObj getImageObj(int index) { return this.imagesObj.get(index); }
    public ImageViewObj getImageView(int index) { return this.imageViews.get(index); }
    public long getCurrentImage() { return this.images.get(this.imageIndex.get(0)); }
    public MemoryAllocationObj.ImageObj getCurrentImageObj() { return this.imagesObj.get(this.imageIndex.get(0)); }
    public ImageViewObj getCurrentImageView() { return this.imageViews.get(this.imageIndex.get(0)); }

    // TODO: more than one semaphore support
    public int acquireImageIndex(long semaphore) {
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get());
        vkAcquireNextImageKHR(deviceObj.device, this.handle.get(), 9007199254740991L, semaphore != 0 ? semaphore : this.semaphoreImageAvailable.get(0), 0L, this.imageIndex);
        return this.imageIndex.get(0);
    }

    // TODO: more than one semaphore support
    public SwapChainObj present(VkQueue queue, LongBuffer semaphore) {
        vkQueuePresentKHR(queue, VkPresentInfoKHR.create()
            .sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
            .pWaitSemaphores(semaphore != null ? semaphore : memAllocLong(1).put(0, this.semaphoreRenderingAvailable.get(0)))
            .pSwapchains(memAllocLong(1).put(0, this.handle.get())).swapchainCount(1)
            .pImageIndices(this.imageIndex));
        return this;
    }

    // TODO: OpenGL support
    // Virtual SwapChain for rendering in virtual surface
    public static class SwapChainVirtual extends SwapChainObj {
        public SwapChainVirtual(Handle base, SwapChainCInfo.VirtualSwapChainCInfo cInfo) {
            super(base, cInfo);
        }

        //
        @Override
        public SwapChainObj generateSemaphores() {
            var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get());

            // useless for pure Vulkan, for test only
            // TODO: semaphore objects
            vkCreateSemaphore(deviceObj.device, VkSemaphoreCreateInfo.create().pNext(VkExportSemaphoreCreateInfoKHR.create().sType(VK_STRUCTURE_TYPE_EXPORT_SEMAPHORE_CREATE_INFO).handleTypes(VK_EXTERNAL_SEMAPHORE_HANDLE_TYPE_OPAQUE_WIN32_BIT ).address()).sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO), null, semaphoreImageAvailable = memAllocLong(1));
            vkCreateSemaphore(deviceObj.device, VkSemaphoreCreateInfo.create().pNext(VkExportSemaphoreCreateInfoKHR.create().sType(VK_STRUCTURE_TYPE_EXPORT_SEMAPHORE_CREATE_INFO).handleTypes(VK_EXTERNAL_SEMAPHORE_HANDLE_TYPE_OPAQUE_WIN32_BIT ).address()).sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO), null, semaphoreRenderingAvailable = memAllocLong(1));

            // TODO: semaphore objects
            // TODO: Linux support
            vkGetSemaphoreWin32HandleKHR(deviceObj.device, VkSemaphoreGetWin32HandleInfoKHR.create().sType(VK_STRUCTURE_TYPE_SEMAPHORE_GET_WIN32_HANDLE_INFO_KHR).semaphore(this.semaphoreImageAvailable.get(0)).handleType(VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_WIN32_BIT), this.SemImageWin32Handle = memAllocPointer(1));
            vkGetSemaphoreWin32HandleKHR(deviceObj.device, VkSemaphoreGetWin32HandleInfoKHR.create().sType(VK_STRUCTURE_TYPE_SEMAPHORE_GET_WIN32_HANDLE_INFO_KHR).semaphore(this.semaphoreRenderingAvailable.get(0)).handleType(VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_WIN32_BIT), this.SemRenderWin32Handle = memAllocPointer(1));

            return this;
        }

        @Override
        public SwapChainObj generateImages(SwapChainCInfo cInfo) {
            this.imagesObj = new ArrayList<>();

            //
            for (var I=0;I<cInfo.imageCount;I++) {
                var allocationCInfo = new MemoryAllocationCInfo() {{
                    isHost = false;
                    isDevice = true;
                }};

                var finalI = I;
                var imageCInfo = new MemoryAllocationCInfo.ImageCInfo(){{
                    arrayLayers = cInfo.layerCount;
                    format = cInfo.format;
                    mipLevels = 1;
                    extent3D = VkExtent3D.create().width(cInfo.extent.width()).height(cInfo.extent.height()).depth(1);;
                    tiling = VK_IMAGE_TILING_OPTIMAL;
                    samples = VK_SAMPLE_COUNT_1_BIT;
                    usage = VK_IMAGE_USAGE_SAMPLED_BIT | VK_IMAGE_USAGE_STORAGE_BIT | VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
                }};

                //
                var imageViewCInfo = new ImageViewCInfo(){{
                    image = images.get(finalI);
                    subresourceRange = VkImageSubresourceRange.create().layerCount(1).baseArrayLayer(0).levelCount(1).baseMipLevel(0).aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                    pipelineLayout = cInfo.pipelineLayout;
                    imageLayout = VK_IMAGE_LAYOUT_GENERAL;
                    type = "storage";
                }};

                //
                this.imagesObj.add(new MemoryAllocationObj.ImageObj(this.base, imageCInfo));
                this.imageViews.add(new ImageViewObj(this.base, imageViewCInfo));
            }

            return this;
        }

        // TODO: support for OpenGL
        @Override
        public int acquireImageIndex(long semaphore) {
            var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get());
            deviceObj.submitOnce(deviceObj.getCommandPool(((SwapChainCInfo)cInfo).queueFamilyIndex), new BasicCInfo.SubmitCmd(){{
                waitSemaphores = semaphore != 0 ? memAllocLong(1).put(0, semaphore) : memAllocLong(1).put(0, semaphoreImageAvailable.get(0));
                queue = deviceObj.getQueue(((SwapChainCInfo)cInfo).queueFamilyIndex, 0);
            }}, (cmdBuf)->{
                return VK_SUCCESS;
            });
            return this.imageIndex.get(0);
        }

        // TODO: support for OpenGL
        @Override
        public SwapChainObj present(VkQueue queue, LongBuffer semaphore) {
            var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get());
            deviceObj.submitOnce(deviceObj.getCommandPool(((SwapChainCInfo)cInfo).queueFamilyIndex), new BasicCInfo.SubmitCmd(){{
                waitSemaphores = semaphore != null ? semaphore : memAllocLong(1).put(0, semaphoreRenderingAvailable.get(0));
                queue = deviceObj.getQueue(((SwapChainCInfo)cInfo).queueFamilyIndex, 0);
            }}, (cmdBuf)->{
                return VK_SUCCESS;
            });
            return this;
        }
    }

}
