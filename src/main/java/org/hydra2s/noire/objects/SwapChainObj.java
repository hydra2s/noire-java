package org.hydra2s.noire.objects;

//

import org.hydra2s.noire.descriptors.*;
import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.VkExtent3D;
import org.lwjgl.vulkan.VkImageSubresourceRange;
import org.lwjgl.vulkan.VkImageViewCreateInfo;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hydra2s.noire.descriptors.UtilsCInfo.vkCheckStatus;
import static org.lwjgl.BufferUtils.createIntBuffer;
import static org.lwjgl.BufferUtils.createPointerBuffer;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

//
public class SwapChainObj extends BasicObj  {
    public long[] images = {};
    public int[] amountOfImagesInSwapchain = {};

    //
    public ArrayList<Integer> presentModes = new ArrayList<>(Arrays.asList(VK_PRESENT_MODE_IMMEDIATE_KHR, VK_PRESENT_MODE_FIFO_RELAXED_KHR, VK_PRESENT_MODE_FIFO_KHR, VK_PRESENT_MODE_MAILBOX_KHR));
    public ArrayList<Integer> surfaceFormats = new ArrayList<>(Arrays.asList(VK_FORMAT_A2B10G10R10_UNORM_PACK32, VK_FORMAT_B8G8R8A8_UNORM));
    public VkSwapchainCreateInfoKHR createInfo = null;
    public VkImageViewCreateInfo imageViewInfo = null;

    //
    public ArrayList<ImageObj> imagesObj = new ArrayList<ImageObj>();
    public ArrayList<ImageViewObj> imageViews = new ArrayList<ImageViewObj>();

    //
    public SemaphoreObj semaphoreImageAvailable = null;
    public SemaphoreObj semaphoreRenderingAvailable = null;
    public int[] imageIndex = {};

    //
    public PointerBuffer SemImageWin32Handle = createPointerBuffer(1).put(0, 0);
    public PointerBuffer SemRenderWin32Handle = createPointerBuffer(1).put(0, 0);

    //
    public SwapChainObj(UtilsCInfo.Handle base, UtilsCInfo.Handle handle) {
        super(base, handle);
    }

    public SwapChainObj generateImages(SwapChainCInfo cInfo) {
        
        
        var descriptorsObj = (PipelineLayoutObj)deviceObj.handleMap.get(new UtilsCInfo.Handle("PipelineLayout", cInfo.pipelineLayout)).orElse(null);

        //
        if (cInfo.surface != 0) {
            var Ps = presentModes.size();
            var surfaceInfo = physicalDeviceObj.getSurfaceInfo(cInfo.surface, cInfo.queueFamilyIndex);
            var presentMode = presentModes.get(0);
            for (var I = 0; I < Ps; I++) {
                var PM = presentModes.get(I);
                if (List.of(surfaceInfo.presentModes).contains(PM)) {
                    presentMode = PM;
                    break;
                }
            }

            //
            var format = surfaceFormats.get(0);
            var colorSpace = VK_COLOR_SPACE_SRGB_NONLINEAR_KHR;

            //
            var Ss = surfaceFormats.size();
            for (var I = 0; I < Ss; I++) {
                var F = surfaceFormats.get(I);
                Integer finalFormat = format;
                var ID = surfaceInfo.formats2.stream().toList().indexOf(surfaceInfo.formats2.stream().filter((F2) -> (F2.surfaceFormat().format() == finalFormat)).findFirst().orElse(null));
                if (ID >= 0) {
                    format = surfaceInfo.formats2.get(ID).surfaceFormat().format();
                    colorSpace = surfaceInfo.formats2.get(ID).surfaceFormat().colorSpace();
                    break;
                }
            }

            //
            var pQueueFamilyIndices = createIntBuffer(deviceObj.queueFamilyIndices.length); pQueueFamilyIndices.put(0, deviceObj.queueFamilyIndices);
            this.createInfo = VkSwapchainCreateInfoKHR.create()
                    .sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
                    .surface(cInfo.surface)
                    .minImageCount(Math.min(Math.max(cInfo.imageCount, surfaceInfo.capabilities2.surfaceCapabilities().minImageCount()), surfaceInfo.capabilities2.surfaceCapabilities().maxImageCount()))
                    .imageFormat(format)
                    .imageColorSpace(colorSpace)
                    .imageExtent(cInfo.extent)
                    .imageArrayLayers(cInfo.layerCount)
                    .imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT | VK_IMAGE_USAGE_STORAGE_BIT | VK_IMAGE_USAGE_SAMPLED_BIT | VK_IMAGE_USAGE_TRANSFER_SRC_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT)
                    .imageSharingMode(VK_SHARING_MODE_CONCURRENT)
                    .pQueueFamilyIndices(pQueueFamilyIndices)
                    .preTransform(surfaceInfo.capabilities2.surfaceCapabilities().currentTransform())
                    .compositeAlpha(surfaceInfo.capabilities2.surfaceCapabilities().supportedCompositeAlpha())
                    .presentMode(presentMode)
                    .clipped(true)
                    .oldSwapchain(0L);

            //
            vkCheckStatus(vkCreateSwapchainKHR(deviceObj.device, this.createInfo, null, (this.handle = new UtilsCInfo.Handle("SwapChain")).ptr()));
            vkCheckStatus(vkGetSwapchainImagesKHR(deviceObj.device, this.handle.get(), this.amountOfImagesInSwapchain = new int[]{0}, null));
            vkCheckStatus(vkGetSwapchainImagesKHR(deviceObj.device, this.handle.get(), this.amountOfImagesInSwapchain, this.images = new long[this.amountOfImagesInSwapchain[0]]));
            this.imagesObj = new ArrayList<>();
            this.imageViews = new ArrayList<>();

            //
            for (var I = 0; I < this.amountOfImagesInSwapchain[0]; I++) {
                var finalI = I;
                this.imagesObj.add(new ImageObj(this.base, new ImageCInfo() {{
                    image = createPointerBuffer(1).put(0, images[finalI]);
                    arrayLayers = createInfo.imageArrayLayers();
                    format = createInfo.imageFormat();
                    mipLevels = 1;
                    extent3D = VkExtent3D.create().width(createInfo.imageExtent().width()).height(createInfo.imageExtent().height()).depth(1);
                    tiling = VK_IMAGE_TILING_OPTIMAL;
                    samples = VK_SAMPLE_COUNT_1_BIT;
                    usage = VK_IMAGE_USAGE_SAMPLED_BIT | VK_IMAGE_USAGE_STORAGE_BIT | VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
                }}));
                this.images[finalI] = this.imagesObj.get(finalI).getHandle().get();

                //
                this.imageViews.add(new ImageViewObj(this.base, new ImageViewCInfo() {{
                    image = images[finalI];
                    subresourceRange = VkImageSubresourceRange.create().layerCount(1).baseArrayLayer(0).levelCount(1).baseMipLevel(0).aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                    pipelineLayout = cInfo.pipelineLayout;
                    imageLayout = VK_IMAGE_LAYOUT_GENERAL;
                    type = "storage";
                }}));
            }
        } else {
            //
            var memoryAllocatorObj = (MemoryAllocatorObj) BasicObj.globalHandleMap.get(cInfo.memoryAllocator).orElse(null);

            this.imageViews = new ArrayList<>();
            this.imagesObj = new ArrayList<>();
            this.images = new long[cInfo.imageCount];
            for (var I=0;I<cInfo.imageCount;I++) {
                var finalI = I;
                this.imagesObj.add(new ImageObj(this.base, new ImageCInfo(){{
                    arrayLayers = cInfo.layerCount;
                    format = cInfo.format;
                    mipLevels = 1;
                    extent3D = VkExtent3D.create().width(cInfo.extent.width()).height(cInfo.extent.height()).depth(1);;
                    tiling = VK_IMAGE_TILING_OPTIMAL;
                    samples = VK_SAMPLE_COUNT_1_BIT;
                    usage = VK_IMAGE_USAGE_SAMPLED_BIT | VK_IMAGE_USAGE_STORAGE_BIT | VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
                    memoryAllocator = memoryAllocatorObj.getHandle().get();
                    memoryAllocationInfo = new MemoryAllocationCInfo(){{
                        isHost = false;
                        isDevice = true;
                    }};
                }}));
                this.images[finalI] = this.imagesObj.get(finalI).getHandle().get();
                this.imageViews.add(new ImageViewObj(this.base, new ImageViewCInfo(){{
                    image = images[finalI];
                    subresourceRange = VkImageSubresourceRange.create().layerCount(1).baseArrayLayer(0).levelCount(1).baseMipLevel(0).aspectMask(VK_IMAGE_ASPECT_COLOR_BIT);
                    pipelineLayout = cInfo.pipelineLayout;
                    imageLayout = VK_IMAGE_LAYOUT_GENERAL;
                    type = "storage";
                }}));
            }

            //
            this.amountOfImagesInSwapchain = new int[cInfo.imageCount];
        }

        return this;
    }

    //
    public SwapChainObj generateSemaphores() {
        

        // useless for pure Vulkan, for test only
        this.semaphoreImageAvailable = new SemaphoreObj(this.base, new SemaphoreCInfo(){{ isTimeline = false; initialValue = 0; }});
        this.semaphoreRenderingAvailable = new SemaphoreObj(this.base, new SemaphoreCInfo(){{ isTimeline = false; initialValue = 0; }});

        //
        return this;
    }

    //
    public SwapChainObj(UtilsCInfo.Handle base, SwapChainCInfo cInfo) {
        super(base, cInfo);

        //
        this.imageIndex = new int[]{0};
        this.generateImages(cInfo);
        this.generateSemaphores();
    }

    //
    public int getFormat() { return this.createInfo.imageFormat(); }
    public int getImageCount() { return this.images.length; }
    public int getColorSpace() { return this.createInfo.imageColorSpace(); }
    public long[] getImages() { return this.images; }
    public ArrayList<ImageObj> getImagesObj() { return this.imagesObj; }
    public ArrayList<ImageViewObj> getImageViews() { return this.imageViews; }
    public long getImage(int index) { return this.images[index]; }
    public ImageObj getImageObj(int index) { return this.imagesObj.get(index); }
    public ImageViewObj getImageView(int index) { return this.imageViews.get(index); }
    public long getCurrentImage() { return this.images[this.imageIndex[0]]; }
    public ImageObj getCurrentImageObj() { return this.imagesObj.get(this.imageIndex[0]); }
    public ImageViewObj getCurrentImageView() { return this.imageViews.get(this.imageIndex[0]); }

    // TODO: more than one semaphore support
    public int acquireImageIndex(long semaphore) {
        vkAcquireNextImageKHR(deviceObj.device, this.handle.get(), 9007199254740991L, semaphore != 0 ? semaphore : this.semaphoreImageAvailable.getHandle().get(), 0L, this.imageIndex);
        return this.imageIndex[0];
    }

    // TODO: more than one semaphore support
    public SwapChainObj present(int queueGroupIndex, long[] semaphore) {
        this.deviceObj.present(queueGroupIndex, this.handle.get(), semaphore != null ? semaphore[0] : this.semaphoreRenderingAvailable.getHandle().get(), this.imageIndex);
        return this;
    }

    // Virtual SwapChain for rendering in virtual surface
    public static class SwapChainVirtual extends SwapChainObj {
        // Here, probably, should to be image barrier operation
        @Override
        public int acquireImageIndex(long semaphore) {
            var index = this.imageIndex[0];
            this.imageIndex[0] = (index+1)%this.amountOfImagesInSwapchain[0];
            return this.imageIndex[0];
        }

        // Here, probably, should to be image barrier operation
        @Override
        public SwapChainObj present(int queueFamilyIndex, long[] semaphore) {
            return this;
        }

        public SwapChainVirtual(UtilsCInfo.Handle base, SwapChainCInfo.VirtualSwapChainCInfo cInfo) {
            super(base, cInfo);
        }

    }

    /*@Override // TODO: multiple queue family support (and Promise.all)
    public SwapChainObj delete() throws Exception {
        var Is = this.imageViews.size();
        for (var i=0;i<Is;i++) {
            this.imageViews.get(i).delete();
            this.imagesObj.get(i).delete();
        }

        //
        deviceObj.submitOnce(new DeviceObj.SubmitCmd(){{
            queueGroupIndex = cInfo.queueGroupIndex;
            onDone = new Promise<>().thenApply((result)-> {
                vkDestroySwapchainKHR(deviceObj.device, handle.get(), null);
                deviceObj.handleMap.put$(handle, null);
                return null;
            });
        }}, (cmdBuf)->{
            return cmdBuf;
        });

        //
        return this;
    }*/

    @Override // TODO: multiple queue family support (and Promise.all)
    public SwapChainObj deleteDirectly() /*throws Exception*/ {
        var Is = this.imageViews.size();
        for (var i=0;i<Is;i++) {
            this.imageViews.get(i).deleteDirectly();
            this.imagesObj.get(i).deleteDirectly();
        }

        //
        
        vkDestroySwapchainKHR(deviceObj.device, handle.get(), null);
        deviceObj.handleMap.put$(handle, null);

        //
        return this;
    }

}
