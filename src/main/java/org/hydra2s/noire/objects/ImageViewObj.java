package org.hydra2s.noire.objects;

//

import org.hydra2s.noire.descriptors.ImageViewCInfo;
import org.lwjgl.vulkan.*;

import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.system.MemoryUtil.memLongBuffer;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK13.VK_STRUCTURE_TYPE_BUFFER_IMAGE_COPY_2;
import static org.lwjgl.vulkan.VK13.VK_STRUCTURE_TYPE_IMAGE_COPY_2;

// aka, known as ImageSubresourceRange
public class ImageViewObj extends BasicObj {
    public VkImageViewCreateInfo createInfo = null;
    public int DSC_ID = -1;

    // aka, known as ImageSubresourceRange
    public ImageViewObj(Handle base, Handle handle) {
        super(base, handle);
    }

    // aka, known as ImageSubresourceRange
    public ImageViewObj(Handle base, ImageViewCInfo cInfo) {
        super(base, cInfo);

        //
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get());
        var physicalDeviceObj = (PhysicalDeviceObj)BasicObj.globalHandleMap.get(deviceObj.base.get());
        var imageObj = (MemoryAllocationObj.ImageObj)deviceObj.handleMap.get(new Handle("Image", cInfo.image));
        var imageT = imageObj.createInfo.imageType();
        var format = imageObj.createInfo.format();

        //
        var imageViewType = VK_IMAGE_VIEW_TYPE_3D;
        if (imageT == VK_IMAGE_TYPE_1D) { imageViewType = (cInfo.subresourceRange.layerCount() > 1 ? VK_IMAGE_VIEW_TYPE_1D_ARRAY   : VK_IMAGE_VIEW_TYPE_1D  ); };
        if (imageT == VK_IMAGE_TYPE_2D) { imageViewType = (cInfo.subresourceRange.layerCount() > 1 ? VK_IMAGE_VIEW_TYPE_2D_ARRAY   : VK_IMAGE_VIEW_TYPE_2D  ); };
        if (cInfo.isCubemap)            { imageViewType = (cInfo.subresourceRange.layerCount() > 6 ? VK_IMAGE_VIEW_TYPE_CUBE_ARRAY : VK_IMAGE_VIEW_TYPE_CUBE); };

        //
        vkCreateImageView(deviceObj.device, this.createInfo = VkImageViewCreateInfo.create().sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO).image(cInfo.image).format(format).viewType(imageViewType).subresourceRange(cInfo.subresourceRange).components(cInfo.compontentMapping), null, memLongBuffer(memAddress((this.handle = new Handle("ImageView")).ptr(), 0), 1));
        deviceObj.handleMap.put(this.handle, this);

        //
        if (cInfo.pipelineLayout > 0) {
            var descriptorsObj = (PipelineLayoutObj)deviceObj.handleMap.get(new Handle("PipelineLayout", cInfo.pipelineLayout));
            this.DSC_ID = descriptorsObj.resources.push(VkDescriptorImageInfo.create().imageView(this.handle.get()).imageLayout(cInfo.imageLayout));
            descriptorsObj.writeDescriptors();
        }


    }

    //
    public VkImageSubresourceRange subresourceRange() {
        return this.createInfo.subresourceRange();
    }

    //
    public VkImageSubresourceLayers subresourceLayers(int mipLevel) {
        var subresourceRange = this.subresourceRange();
        return VkImageSubresourceLayers.create()
            .aspectMask(subresourceRange.aspectMask())
            .mipLevel(subresourceRange.baseMipLevel() + mipLevel)
            .baseArrayLayer(subresourceRange.baseArrayLayer())
            .layerCount(subresourceRange.layerCount());
    }

    // TODO: unidirectional support
    public ImageViewObj cmdCopyBufferToImageView(
        VkCommandBuffer cmdBuf,

        // TODO: multiple one support
        VkExtent3D extent,
        VkOffset3D dstOffset,
        int dstMipLevel,

        // TODO: structured one support
        long srcBuffer,
        long srcOffset
    ) {
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get());
        var physicalDeviceObj = (PhysicalDeviceObj)BasicObj.globalHandleMap.get(deviceObj.base.get());
        var dstImageObj = (MemoryAllocationObj.ImageObj)deviceObj.handleMap.get(new Handle("Image", ((ImageViewCInfo)cInfo).image));
        var srcBufferObj = (MemoryAllocationObj.BufferObj)deviceObj.handleMap.get(new Handle("Buffer", srcBuffer));

        //
        srcBufferObj.cmdCopyBufferToImage(cmdBuf, ((ImageViewCInfo)cInfo).image,
            ((ImageViewCInfo)cInfo).imageLayout, VkBufferImageCopy2.create(1)
                .sType(VK_STRUCTURE_TYPE_BUFFER_IMAGE_COPY_2)
                .bufferOffset(srcOffset)
                .imageOffset(dstOffset).imageExtent(extent)
                .imageSubresource(this.subresourceLayers(dstMipLevel))
        );

        //
        return this;
    }

    // TODO: unidirectional support
    public ImageViewObj cmdCopyImageViewToImageView(
        VkCommandBuffer cmdBuf,

        // TODO: multiple one support
        VkExtent3D extent,
        VkOffset3D dstOffset,
        int dstMipLevel,

        // TODO: structured one support
        long srcImageView,
        VkOffset3D srcOffset,
        int srcMipLevel
    ) {
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get());
        var physicalDeviceObj = (PhysicalDeviceObj)BasicObj.globalHandleMap.get(deviceObj.base.get());

        //
        var srcImageViewObj = (ImageViewObj)deviceObj.handleMap.get(new Handle("ImageView", srcImageView));
        var srcImageObj = (MemoryAllocationObj.ImageObj)deviceObj.handleMap.get(new Handle("Image", ((ImageViewCInfo)srcImageViewObj.cInfo).image));
        var dstImageObj = (MemoryAllocationObj.ImageObj)deviceObj.handleMap.get(new Handle("Image", ((ImageViewCInfo)cInfo).image));

        //
        this.cmdCopyImageToImageView(cmdBuf, extent, dstOffset, dstMipLevel, ((ImageViewCInfo)srcImageViewObj.cInfo).image, ((ImageViewCInfo)srcImageViewObj.cInfo).imageLayout, srcOffset, srcImageViewObj.subresourceLayers(srcMipLevel));

        //
        return this;
    }

    // TODO: unidirectional support
    public ImageViewObj cmdCopyImageToImageView(
        VkCommandBuffer cmdBuf,

        // TODO: multiple one support
        VkExtent3D extent,
        VkOffset3D dstOffset,
        int dstMipLevel,

        // TODO: structured one support
        long srcImage,
        int srcImageLayout,
        VkOffset3D srcOffset,
        VkImageSubresourceLayers srcSubresource
    ) {
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get());
        var physicalDeviceObj = (PhysicalDeviceObj)BasicObj.globalHandleMap.get(deviceObj.base.get());
        var dstImageObj = (MemoryAllocationObj.ImageObj)deviceObj.handleMap.get(new Handle("Image", ((ImageViewCInfo)cInfo).image));
        var srcImageObj = (MemoryAllocationObj.ImageObj)deviceObj.handleMap.get(new Handle("Image", srcImage));

        //
        srcImageObj.cmdCopyImageToImage(cmdBuf, ((ImageViewCInfo)cInfo).image, srcImageLayout,
            ((ImageViewCInfo)cInfo).imageLayout, VkImageCopy2.create(1)
                .sType(VK_STRUCTURE_TYPE_IMAGE_COPY_2)
                .extent(extent)
                .dstOffset(dstOffset)
                .srcOffset(srcOffset)
                .dstSubresource(this.subresourceLayers(dstMipLevel))
                .srcSubresource(srcSubresource));

        //
        return this;
    }

    public int getImageLayout() {
        return ((ImageViewCInfo)cInfo).imageLayout;
    }

    //
    public ImageViewObj cmdTransitionBarrierFromInitial(VkCommandBuffer cmdBuf) {
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get());
        var physicalDeviceObj = (PhysicalDeviceObj)BasicObj.globalHandleMap.get(deviceObj.base.get());
        var dstImageObj = (MemoryAllocationObj.ImageObj)deviceObj.handleMap.get(new Handle("Image", ((ImageViewCInfo)cInfo).image));

        //
        dstImageObj.cmdTransitionBarrier(cmdBuf, VK_IMAGE_LAYOUT_UNDEFINED, ((ImageViewCInfo)cInfo).imageLayout, ((ImageViewCInfo)cInfo).subresourceRange);

        //
        return this;
    }

    // simpler than traditional image
    public ImageViewObj cmdTransitionBarrier(VkCommandBuffer cmdBuf, int dstImageLayout, boolean fromInitial) {
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get());
        var physicalDeviceObj = (PhysicalDeviceObj)BasicObj.globalHandleMap.get(deviceObj.base.get());
        var dstImageObj = (MemoryAllocationObj.ImageObj)deviceObj.handleMap.get(new Handle("Image", ((ImageViewCInfo)cInfo).image));

        //
        if (fromInitial) { this.cmdTransitionBarrierFromInitial(cmdBuf); };
        dstImageObj.cmdTransitionBarrier(cmdBuf, ((ImageViewCInfo)cInfo).imageLayout, dstImageLayout, ((ImageViewCInfo)cInfo).subresourceRange);
        ((ImageViewCInfo)cInfo).imageLayout = dstImageLayout;

        //
        return this;
    }
}
