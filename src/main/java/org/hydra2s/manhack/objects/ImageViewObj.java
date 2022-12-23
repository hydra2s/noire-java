package org.hydra2s.manhack.objects;

//
import org.hydra2s.manhack.descriptors.ImageViewCInfo;
import org.lwjgl.vulkan.VkImageViewCreateInfo;

//
import static org.lwjgl.vulkan.VK10.*;

//
public class ImageViewObj extends BasicObj {
    public VkImageViewCreateInfo createInfo = null;
    public int imageLayout = VK_IMAGE_LAYOUT_UNDEFINED;
    public int DSC_ID = -1;


    public ImageViewObj(Handle base, Handle handle) {
        super(base, handle);
    }

    public ImageViewObj(Handle base, ImageViewCInfo cInfo) {
        super(base, cInfo);

        //
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get());
        var physicalDeviceObj = (PhysicalDeviceObj)BasicObj.globalHandleMap.get(deviceObj.base.get());
        var imageObj = (MemoryAllocationObj.ImageObj)deviceObj.handleMap.get(cInfo.image);
        var imageT = imageObj.createInfo.imageType();
        var format = imageObj.createInfo.format();

        //
        var imageViewType = VK_IMAGE_VIEW_TYPE_3D;
        if (imageT == VK_IMAGE_TYPE_1D) { imageViewType = (cInfo.subresourceRange.layerCount() > 1 ? VK_IMAGE_VIEW_TYPE_1D_ARRAY   : VK_IMAGE_VIEW_TYPE_1D  ); };
        if (imageT == VK_IMAGE_TYPE_2D) { imageViewType = (cInfo.subresourceRange.layerCount() > 1 ? VK_IMAGE_VIEW_TYPE_2D_ARRAY   : VK_IMAGE_VIEW_TYPE_2D  ); };
        if (cInfo.isCubemap)            { imageViewType = (cInfo.subresourceRange.layerCount() > 6 ? VK_IMAGE_VIEW_TYPE_CUBE_ARRAY : VK_IMAGE_VIEW_TYPE_CUBE); };

        //
        vkCreateImageView(deviceObj.device, this.createInfo = VkImageViewCreateInfo.create().sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO).image(cInfo.image).format(format).viewType(imageViewType).subresourceRange(cInfo.subresourceRange).components(cInfo.compontentMapping), null, (this.handle = new Handle("ImageView")).ptr().getLongBuffer(1));
    }
}
