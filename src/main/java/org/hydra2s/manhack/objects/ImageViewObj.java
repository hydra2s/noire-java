package org.hydra2s.manhack.objects;

//
import org.hydra2s.manhack.descriptors.ImageViewCInfo;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkImageSubresourceLayers;
import org.lwjgl.vulkan.VkImageViewCreateInfo;

//
import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.system.MemoryUtil.memLongBuffer;
import static org.lwjgl.vulkan.VK10.*;

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
    public VkImageSubresourceLayers subresourceLayers(int mipLevel) {
        return VkImageSubresourceLayers.create()
                .aspectMask(this.createInfo.subresourceRange().aspectMask())
                .mipLevel(mipLevel)
                .baseArrayLayer(this.createInfo.subresourceRange().baseArrayLayer())
                .layerCount(this.createInfo.subresourceRange().layerCount());
    }

    // TODO: copy between ImageView and buffers, images or imageViews
    // TODO: transition image layouts in ImageView (subresourceRange)
}
