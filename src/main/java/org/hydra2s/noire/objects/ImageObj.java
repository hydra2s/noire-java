package org.hydra2s.noire.objects;


import org.hydra2s.noire.descriptors.BasicCInfo;
import org.hydra2s.noire.descriptors.ImageCInfo;
import org.hydra2s.noire.descriptors.SwapChainCInfo;
import org.hydra2s.utils.Promise;
import org.lwjgl.vulkan.*;

import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.system.MemoryUtil.memLongBuffer;
import static org.lwjgl.vulkan.EXTImage2dViewOf3d.VK_IMAGE_CREATE_2D_VIEW_COMPATIBLE_BIT_EXT;
import static org.lwjgl.vulkan.KHRSwapchain.vkDestroySwapchainKHR;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK11.*;
import static org.lwjgl.vulkan.VK13.*;

//
public class ImageObj extends BasicObj {
    public long allocationHandle = 0L;

    public VkImageCreateInfo createInfo = null;
    public ImageObj(Handle base, Handle handle) {
        super(base, handle);
    }

    //
    public VkMemoryRequirements2 memoryRequirements2 = null;

    // TODO: create buffer by allocator (such as VMA)
    public ImageObj(Handle base, ImageCInfo cInfo) {
        super(base, cInfo);

        //
        var deviceObj = (DeviceObj) BasicObj.globalHandleMap.get(this.base.get());
        var physicalDeviceObj = (PhysicalDeviceObj) BasicObj.globalHandleMap.get(deviceObj.base.get());
        var extent = cInfo.extent3D;
        var imageType = cInfo.extent3D.depth() > 1 ? VK_IMAGE_TYPE_3D : (cInfo.extent3D.height() > 1 ? VK_IMAGE_TYPE_2D : VK_IMAGE_TYPE_1D);
        var arrayLayers = Math.max(cInfo.arrayLayers, 1);

        //
        this.createInfo = VkImageCreateInfo.calloc()
                .pNext(VkExternalMemoryImageCreateInfo.calloc()
                        .pNext(0L)
                        .sType(VK_STRUCTURE_TYPE_EXTERNAL_MEMORY_IMAGE_CREATE_INFO)
                        .handleTypes(VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_WIN32_BIT )
                        .address())
                .sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
                .flags(VK_IMAGE_CREATE_2D_VIEW_COMPATIBLE_BIT_EXT |
                        (((imageType == VK_IMAGE_TYPE_3D)) ? VK_IMAGE_CREATE_2D_ARRAY_COMPATIBLE_BIT : 0) |
                        (((arrayLayers % 6) == 0 && extent.height() == extent.width()) ? VK_IMAGE_CREATE_CUBE_COMPATIBLE_BIT : 0))
                .extent(cInfo.extent3D)
                .imageType(imageType)
                .usage(cInfo.usage | VK_IMAGE_USAGE_TRANSFER_SRC_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT)
                .mipLevels(cInfo.mipLevels)
                .arrayLayers(cInfo.arrayLayers)
                .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                .sharingMode(VK_SHARING_MODE_EXCLUSIVE)
                .samples(cInfo.samples)
                .format(cInfo.format)
                .tiling(cInfo.tiling);

        //
        int status = VK_NOT_READY;
        if (cInfo.image == null || cInfo.image.get(0) == 0) {
            status = vkCreateImage(deviceObj.device, this.createInfo, null, memLongBuffer(memAddress(cInfo.image = (this.handle = new Handle("Image")).ptr()), 1));
        } else {
            this.handle = new Handle("Image", cInfo.image.get(0));
        }
        deviceObj.handleMap.put(this.handle, this);

        //
        vkGetImageMemoryRequirements2(
                deviceObj.device,
                VkImageMemoryRequirementsInfo2.calloc()
                        .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_REQUIREMENTS_INFO_2)
                        .pNext(0L)
                        .image(this.handle.get()),
                this.memoryRequirements2 = VkMemoryRequirements2.calloc()
                        .sType(VK_STRUCTURE_TYPE_MEMORY_REQUIREMENTS_2)
                        .pNext(0L)
        );

        //
        if (cInfo.memoryAllocator != 0) {
            cInfo.memoryAllocationInfo.memoryRequirements2 = this.memoryRequirements2;
            cInfo.memoryAllocationInfo.memoryRequirements = cInfo.memoryAllocationInfo.memoryRequirements2.memoryRequirements();
            cInfo.memoryAllocationInfo.image = this.handle.ptr();

            //
            var memoryAllocatorObj = (MemoryAllocatorObj) BasicObj.globalHandleMap.get(cInfo.memoryAllocator);
            var memoryAllocationObj = new MemoryAllocationObj(this.base, cInfo.memoryAllocationInfo);
            memoryAllocatorObj.allocateMemory(cInfo.memoryAllocationInfo, memoryAllocationObj);

            //
            this.allocationHandle = memoryAllocationObj.getHandle().get();
        }
    }

    // TODO: special support for ImageView
    public ImageObj cmdTransitionBarrier(VkCommandBuffer cmdBuf, int oldLayout, int newLayout, VkImageSubresourceRange subresourceRange) {
        // TODO: correct stage and access per every imageLayout, it should increase some FPS
        var memoryBarrier = VkImageMemoryBarrier2.calloc(1)
                .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER_2)
                .srcStageMask(VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT)
                .srcAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
                .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
                .dstAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .oldLayout(oldLayout)
                .newLayout(newLayout)
                .subresourceRange(subresourceRange)
                .image(this.handle.get());

        //
        vkCmdPipelineBarrier2(cmdBuf, VkDependencyInfoKHR.calloc().sType(VK_STRUCTURE_TYPE_DEPENDENCY_INFO).pImageMemoryBarriers(memoryBarrier));
        return this;
    }

    @Override // TODO: multiple queue family support
    public ImageObj delete() {
        var handle = this.handle;
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get());
        deviceObj.submitOnce(deviceObj.getCommandPool(((SwapChainCInfo)cInfo).queueFamilyIndex), new BasicCInfo.SubmitCmd(){{
            queue = deviceObj.getQueue(((SwapChainCInfo)cInfo).queueFamilyIndex, 0);
            onDone = new Promise<>().thenApply((result)->{
                vkDestroyImage(deviceObj.device, handle.get(), null);
                deviceObj.handleMap.remove(handle);
                return null;
            });
        }}, (cmdBuf)->{
            return VK_SUCCESS;
        });

        // TODO: Use Shared PTR (alike C++)
        var allocationObj = (MemoryAllocationObj) deviceObj.handleMap.get(new Handle("MemoryAllocation", this.allocationHandle));
        allocationObj.delete();
        return this;
    }
}
