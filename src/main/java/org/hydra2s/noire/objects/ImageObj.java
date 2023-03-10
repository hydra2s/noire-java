package org.hydra2s.noire.objects;


import org.hydra2s.noire.descriptors.ImageCInfo;
import org.hydra2s.noire.descriptors.UtilsCInfo;
import org.lwjgl.vulkan.VkExternalMemoryImageCreateInfo;
import org.lwjgl.vulkan.VkImageCreateInfo;
import org.lwjgl.vulkan.VkImageMemoryRequirementsInfo2;
import org.lwjgl.vulkan.VkMemoryRequirements2;

import static org.hydra2s.noire.descriptors.UtilsCInfo.vkCheckStatus;
import static org.lwjgl.BufferUtils.createIntBuffer;
import static org.lwjgl.vulkan.EXTImage2dViewOf3d.VK_IMAGE_CREATE_2D_VIEW_COMPATIBLE_BIT_EXT;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK11.*;

//
public class ImageObj extends BasicObj {
    public long allocationHandle = 0L;

    public VkImageCreateInfo createInfo = null;
    public ImageObj(UtilsCInfo.Handle base, UtilsCInfo.Handle handle) {
        super(base, handle);
    }

    //
    public VkMemoryRequirements2 memoryRequirements2 = null;
    public MemoryAllocationObj allocationObj = null;

    // TODO: create buffer by allocator (such as VMA)
    public ImageObj(UtilsCInfo.Handle base, ImageCInfo cInfo) {
        super(base, cInfo);

        var extent = cInfo.extent3D;
        var imageType = cInfo.extent3D.depth() > 1 ? VK_IMAGE_TYPE_3D : (cInfo.extent3D.height() > 1 ? VK_IMAGE_TYPE_2D : VK_IMAGE_TYPE_1D);
        var arrayLayers = Math.max(cInfo.arrayLayers, 1);

        //
        var pQueueFamilyIndices = createIntBuffer(deviceObj.queueFamilyIndices.length); pQueueFamilyIndices.put(0, deviceObj.queueFamilyIndices);
        this.createInfo = VkImageCreateInfo.create()
            .pNext(VkExternalMemoryImageCreateInfo.create()
                    .pNext(0L)
                    .sType(VK_STRUCTURE_TYPE_EXTERNAL_MEMORY_IMAGE_CREATE_INFO)
                    .handleTypes(VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_WIN32_BIT )
                    .address())
            .sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
            .flags(
                    (imageType == VK_IMAGE_TYPE_3D ? (VK_IMAGE_CREATE_2D_ARRAY_COMPATIBLE_BIT | VK_IMAGE_CREATE_2D_VIEW_COMPATIBLE_BIT_EXT) : 0) |
                    (((arrayLayers % 6) == 0 && extent.height() == extent.width()) ? VK_IMAGE_CREATE_CUBE_COMPATIBLE_BIT : 0))
            .extent(cInfo.extent3D)
            .imageType(imageType)
            .usage(cInfo.usage | VK_IMAGE_USAGE_TRANSFER_SRC_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT)
            .mipLevels(cInfo.mipLevels)
            .arrayLayers(cInfo.arrayLayers)
            .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
            .sharingMode(VK_SHARING_MODE_CONCURRENT)
            .samples(cInfo.samples)
            .format(cInfo.format)
            .tiling(cInfo.tiling)
            .pQueueFamilyIndices(pQueueFamilyIndices);

        //
        int status = VK_NOT_READY;
        if (cInfo.image == null || cInfo.image.get(0) == 0) {
            status = vkCreateImage(deviceObj.device, this.createInfo, null, (this.handle = new UtilsCInfo.Handle("Image")).ptr());
        } else {
            status = VK_SUCCESS;
            this.handle = new UtilsCInfo.Handle("Image", cInfo.image.get(0));
        }
        vkCheckStatus(status);
        deviceObj.handleMap.put$(this.handle, this);

        //
        vkGetImageMemoryRequirements2(
                deviceObj.device,
                VkImageMemoryRequirementsInfo2.create()
                        .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_REQUIREMENTS_INFO_2)
                        .pNext(0L)
                        .image(this.handle.get()),
                this.memoryRequirements2 = VkMemoryRequirements2.create()
                        .sType(VK_STRUCTURE_TYPE_MEMORY_REQUIREMENTS_2)
                        .pNext(0L)
        );

        //
        if (cInfo.memoryAllocator != 0) {
            cInfo.memoryAllocationInfo.memoryRequirements2 = this.memoryRequirements2;
            cInfo.memoryAllocationInfo.memoryRequirements = cInfo.memoryAllocationInfo.memoryRequirements2.memoryRequirements();
            cInfo.memoryAllocationInfo.image = this.handle.ptr();

            //
            var memoryAllocatorObj = (MemoryAllocatorObj) BasicObj.globalHandleMap.get(cInfo.memoryAllocator).orElse(null);
            allocationObj = new MemoryAllocationObj(this.base, cInfo.memoryAllocationInfo);
            assert memoryAllocatorObj != null;
            memoryAllocatorObj.allocateMemory(cInfo.memoryAllocationInfo, allocationObj);

            //
            this.allocationHandle = allocationObj.getHandle().get();
        }
    }

    /*@Override // TODO: multiple queue family support (and Promise.all)
    public ImageObj delete() throws Exception {
        var handle = this.handle;

        deviceObj.submitOnce(new DeviceObj.SubmitCmd(){{
            queueGroupIndex = cInfo.queueGroupIndex;
            onDone = new Promise<>().thenApply((result)->{
                vkDestroyImage(deviceObj.device, handle.get(), null);
                deviceObj.handleMap.put$(handle, null);

                // TODO: Use Shared PTR (alike C++)
                allocationObj.deleteDirectly();

                return null;
            });
        }}, (cmdBuf)->{
            return cmdBuf;
        });

        return this;
    }*/

    @Override // TODO: multiple queue family support (and Promise.all)
    public ImageObj deleteDirectly() {
        var handle = this.handle;

        //
        vkDestroyImage(deviceObj.device, handle.get(), null);
        deviceObj.handleMap.remove(handle);

        // TODO: Use Shared PTR (alike C++)
        vkDeviceWaitIdle(deviceObj.device);
        allocationObj.deleteDirectly();

        //
        return this;
    }
}
