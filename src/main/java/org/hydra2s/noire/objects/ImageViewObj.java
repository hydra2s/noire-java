package org.hydra2s.noire.objects;

//

import org.hydra2s.noire.descriptors.BasicCInfo;
import org.hydra2s.noire.descriptors.CopyInfoCInfo;
import org.hydra2s.noire.descriptors.ImageViewCInfo;
import org.hydra2s.noire.descriptors.SwapChainCInfo;
import org.hydra2s.utils.Promise;
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
        var imageObj = (ImageObj)deviceObj.handleMap.get(new Handle("Image", cInfo.image));
        var imageT = imageObj.createInfo.imageType();
        var format = imageObj.createInfo.format();

        //
        var imageViewType = VK_IMAGE_VIEW_TYPE_3D;
        if (imageT == VK_IMAGE_TYPE_1D) { imageViewType = (cInfo.subresourceRange.layerCount() > 1 ? VK_IMAGE_VIEW_TYPE_1D_ARRAY   : VK_IMAGE_VIEW_TYPE_1D  ); };
        if (imageT == VK_IMAGE_TYPE_2D) { imageViewType = (cInfo.subresourceRange.layerCount() > 1 ? VK_IMAGE_VIEW_TYPE_2D_ARRAY   : VK_IMAGE_VIEW_TYPE_2D  ); };
        if (cInfo.isCubemap)            { imageViewType = (cInfo.subresourceRange.layerCount() > 6 ? VK_IMAGE_VIEW_TYPE_CUBE_ARRAY : VK_IMAGE_VIEW_TYPE_CUBE); };

        //
        vkCreateImageView(deviceObj.device, this.createInfo = VkImageViewCreateInfo.calloc().sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO).image(cInfo.image).format(format).viewType(imageViewType).subresourceRange(cInfo.subresourceRange).components(cInfo.compontentMapping), null, memLongBuffer(memAddress((this.handle = new Handle("ImageView")).ptr(), 0), 1));
        deviceObj.handleMap.put(this.handle, this);

        //
        if (cInfo.pipelineLayout != 0) {
            var descriptorsObj = (PipelineLayoutObj)deviceObj.handleMap.get(new Handle("PipelineLayout", cInfo.pipelineLayout));
            this.DSC_ID = descriptorsObj.resources.push(VkDescriptorImageInfo.calloc().imageView(this.handle.get()).imageLayout(cInfo.imageLayout));
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
        return VkImageSubresourceLayers.calloc()
            .aspectMask(subresourceRange.aspectMask())
            .mipLevel(subresourceRange.baseMipLevel() + mipLevel)
            .baseArrayLayer(subresourceRange.baseArrayLayer())
            .layerCount(subresourceRange.layerCount());
    }

    //
    public static void cmdCopyBufferToImageView(
        VkCommandBuffer cmdBuf,
        CopyInfoCInfo.BufferRangeCopyInfo srcBufferRange,
        CopyInfoCInfo.ImageViewCopyInfo dstImageView,
        VkExtent3D extent
    ) {
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(dstImageView.device);
        var dstImageViewObj = (ImageViewObj)deviceObj.handleMap.get(new Handle("ImageView", dstImageView.imageView));

        //
        MemoryAllocationObj.cmdCopyBufferToImage(cmdBuf, srcBufferRange.buffer, ((ImageViewCInfo)dstImageViewObj.cInfo).image,
            ((ImageViewCInfo)dstImageViewObj.cInfo).imageLayout, VkBufferImageCopy2.calloc(1)
                .sType(VK_STRUCTURE_TYPE_BUFFER_IMAGE_COPY_2)
                .bufferOffset(srcBufferRange.offset)
                .imageOffset(dstImageView.offset).imageExtent(extent)
                .imageSubresource(dstImageViewObj.subresourceLayers(dstImageView.mipLevel))
        );
    }

    //
    public static void cmdCopyImageViewToBuffer(
        VkCommandBuffer cmdBuf,
        CopyInfoCInfo.ImageViewCopyInfo srcImageView,
        CopyInfoCInfo.BufferRangeCopyInfo dstBufferRange,
        VkExtent3D extent
    ) {
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(srcImageView.device);
        var srcImageViewObj = (ImageViewObj)deviceObj.handleMap.get(new Handle("ImageView", srcImageView.imageView));

        //
        MemoryAllocationObj.cmdCopyImageToBuffer(cmdBuf, ((ImageViewCInfo)srcImageViewObj.cInfo).image, dstBufferRange.buffer,
            ((ImageViewCInfo)srcImageViewObj.cInfo).imageLayout, VkBufferImageCopy2.calloc(1)
                .sType(VK_STRUCTURE_TYPE_BUFFER_IMAGE_COPY_2)
                .bufferOffset(dstBufferRange.offset)
                .imageOffset(srcImageView.offset).imageExtent(extent)
                .imageSubresource(srcImageViewObj.subresourceLayers(srcImageView.mipLevel))
        );
    }

    //
    public static void cmdCopyImageViewToImageView(
        VkCommandBuffer cmdBuf,
        CopyInfoCInfo.ImageViewCopyInfo srcImageView,
        CopyInfoCInfo.ImageViewCopyInfo dstImageView,
        VkExtent3D extent
    ) {
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(srcImageView.device);
        var srcImageViewObj = (ImageViewObj)deviceObj.handleMap.get(new Handle("ImageView", srcImageView.imageView));

        //
        ImageViewObj.cmdCopyImageToImageView(cmdBuf, new CopyInfoCInfo.ImageCopyInfo(){{
            image = ((ImageViewCInfo)srcImageViewObj.cInfo).image;
            imageLayout = srcImageViewObj.getImageLayout();
            offset = srcImageView.offset;
            subresource = srcImageViewObj.subresourceLayers(srcImageView.mipLevel);
        }}, dstImageView, extent);
    }

    //
    public static void cmdCopyImageViewToImage(
        VkCommandBuffer cmdBuf,
        CopyInfoCInfo.ImageCopyInfo dstImage,
        CopyInfoCInfo.ImageViewCopyInfo srcImageView,
        VkExtent3D extent
    ) {
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(srcImageView.device);
        var srcImageViewObj = (ImageViewObj)deviceObj.handleMap.get(new Handle("ImageView", srcImageView.imageView));

        //
        MemoryAllocationObj.cmdCopyImageToImage(cmdBuf, ((ImageViewCInfo)srcImageViewObj.cInfo).image, dstImage.image, ((ImageViewCInfo)srcImageViewObj.cInfo).imageLayout, dstImage.imageLayout,
            VkImageCopy2.calloc(1)
                .sType(VK_STRUCTURE_TYPE_IMAGE_COPY_2)
                .extent(extent)
                .srcOffset(srcImageView.offset)
                .dstOffset(dstImage.offset)
                .srcSubresource(srcImageViewObj.subresourceLayers(srcImageView.mipLevel))
                .dstSubresource(dstImage.subresource));
    }

    //
    public static void cmdCopyImageToImageView(
        VkCommandBuffer cmdBuf,
        CopyInfoCInfo.ImageCopyInfo srcImage,
        CopyInfoCInfo.ImageViewCopyInfo dstImageView,
        VkExtent3D extent
    ) {
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(dstImageView.device);
        var dstImageViewObj = (ImageViewObj)deviceObj.handleMap.get(new Handle("ImageView", dstImageView.imageView));

        //
        MemoryAllocationObj.cmdCopyImageToImage(cmdBuf, srcImage.image, ((ImageViewCInfo)dstImageViewObj.cInfo).image, srcImage.imageLayout, ((ImageViewCInfo)dstImageViewObj.cInfo).imageLayout,
            VkImageCopy2.calloc(1)
                .sType(VK_STRUCTURE_TYPE_IMAGE_COPY_2)
                .extent(extent)
                .dstOffset(dstImageView.offset)
                .srcOffset(srcImage.offset)
                .dstSubresource(dstImageViewObj.subresourceLayers(dstImageView.mipLevel))
                .srcSubresource(srcImage.subresource));
    }

    public int getImageLayout() {
        return ((ImageViewCInfo)cInfo).imageLayout;
    }

    //
    public ImageViewObj cmdTransitionBarrierFromInitial(VkCommandBuffer cmdBuf) {
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get());
        var physicalDeviceObj = (PhysicalDeviceObj)BasicObj.globalHandleMap.get(deviceObj.base.get());
        var dstImageObj = (ImageObj)deviceObj.handleMap.get(new Handle("Image", ((ImageViewCInfo)cInfo).image));

        //
        dstImageObj.cmdTransitionBarrier(cmdBuf, VK_IMAGE_LAYOUT_UNDEFINED, ((ImageViewCInfo)cInfo).imageLayout, ((ImageViewCInfo)cInfo).subresourceRange);

        //
        return this;
    }

    // simpler than traditional image
    public ImageViewObj cmdTransitionBarrier(VkCommandBuffer cmdBuf, int dstImageLayout, boolean fromInitial) {
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get());
        var physicalDeviceObj = (PhysicalDeviceObj)BasicObj.globalHandleMap.get(deviceObj.base.get());
        var dstImageObj = (ImageObj)deviceObj.handleMap.get(new Handle("Image", ((ImageViewCInfo)cInfo).image));

        //
        if (fromInitial) { this.cmdTransitionBarrierFromInitial(cmdBuf); };
        dstImageObj.cmdTransitionBarrier(cmdBuf, ((ImageViewCInfo)cInfo).imageLayout, dstImageLayout, ((ImageViewCInfo)cInfo).subresourceRange);
        ((ImageViewCInfo)cInfo).imageLayout = dstImageLayout;

        //
        return this;
    }

    @Override // TODO: multiple queue family support
    public ImageViewObj delete() {
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get());
        var handle = this.handle;
        var cInfo = (ImageViewCInfo)this.cInfo;
        var self = this;
        deviceObj.submitOnce(deviceObj.getCommandPool((cInfo).queueFamilyIndex), new BasicCInfo.SubmitCmd(){{
            queue = deviceObj.getQueue((cInfo).queueFamilyIndex, 0);
            onDone = new Promise<>().thenApply((result)-> {
                //
                if (cInfo.pipelineLayout != 0) {
                    var descriptorsObj = (PipelineLayoutObj)deviceObj.handleMap.get(new Handle("PipelineLayout", cInfo.pipelineLayout));
                    descriptorsObj.resources.removeIndex(self.DSC_ID);
                    self.DSC_ID = -1;
                }

                vkDestroyImageView(deviceObj.device, handle.get(), null);
                deviceObj.handleMap.remove(handle);
                return null;
            });
        }}, (cmdBuf)->{
            return VK_SUCCESS;
        });
        return this;
    }
}
