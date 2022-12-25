package org.hydra2s.manhack.objects;

//
import org.hydra2s.manhack.descriptors.ImageViewCInfo;
import org.hydra2s.manhack.descriptors.MemoryAllocationCInfo;
import org.hydra2s.manhack.descriptors.SwapChainCInfo;
import org.lwjgl.vulkan.*;

//
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.KHRSurface.*;
import static org.lwjgl.vulkan.KHRSwapchain.*;
import static org.lwjgl.vulkan.VK10.*;

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
    public IntBuffer imageIndex = memAllocInt(1);

    //
    public SwapChainObj(Handle base, Handle handle) {
        super(base, handle);
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
        var presentMode = presentModes.get(0);
        for (var I=0;I<presentModes.size();I++) { var PM = presentModes.get(I);
            if (List.of(surfaceInfo.presentModes.array()).indexOf(PM) >= 0) {
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
            .imageArrayLayers(1)
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

        //
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
                arrayLayers = 1;
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
            }};

            //
            this.imagesObj.add(new MemoryAllocationObj.ImageObj(this.base, imageCInfo));
            this.imageViews.add(new ImageViewObj(this.base, imageViewCInfo));
        }

        //
        this.imageIndex = memAllocInt(1);
        vkCreateSemaphore(deviceObj.device, VkSemaphoreCreateInfo.create().sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO), null, semaphoreImageAvailable = memAllocLong(this.amountOfImagesInSwapchain.get(0)));
        vkCreateSemaphore(deviceObj.device, VkSemaphoreCreateInfo.create().sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO), null, semaphoreRenderingAvailable = memAllocLong(this.amountOfImagesInSwapchain.get(0)));
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
    public int acquireImageIndex(long semaphoreImageAvailable) {
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get());
        vkAcquireNextImageKHR(deviceObj.device, this.handle.get(), 9007199254740991L, semaphoreImageAvailable != 0 ? semaphoreImageAvailable : this.semaphoreImageAvailable.get(0), 0L, this.imageIndex);
        return this.imageIndex.get(0);
    }

    // TODO: more than one semaphore support
    public SwapChainObj present(VkQueue queue, LongBuffer semaphoreRenderingAvailable) {
        vkQueuePresentKHR(queue, VkPresentInfoKHR.create()
            .sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
            .pWaitSemaphores(semaphoreRenderingAvailable != null ? semaphoreRenderingAvailable : memAllocLong(1).put(0, this.semaphoreRenderingAvailable.get(0)))
            .pSwapchains(memAllocLong(1).put(0, this.handle.get()))
            .pImageIndices(this.imageIndex));
        return this;
    }

}
