package org.hydra2s.noire.objects;

//

import org.hydra2s.noire.descriptors.ImageViewCInfo;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkImageSubresourceLayers;
import org.lwjgl.vulkan.VkImageViewCreateInfo;

import static org.hydra2s.noire.descriptors.UtilsCInfo.vkCheckStatus;
import static org.lwjgl.vulkan.VK10.*;

// aka, known as ImageSubresourceRange
// TODO: derivative from ImageSubresourceRange with Image
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
        var imageObj = (ImageObj)deviceObj.handleMap.get(new Handle("Image", cInfo.image)).orElse(null);
        var imageT = imageObj.createInfo.imageType();
        var format = imageObj.createInfo.format();

        //
        var imageViewType = VK_IMAGE_VIEW_TYPE_3D;
        if (imageT == VK_IMAGE_TYPE_1D) { imageViewType = (cInfo.subresourceRange.layerCount() > 1 ? VK_IMAGE_VIEW_TYPE_1D_ARRAY   : VK_IMAGE_VIEW_TYPE_1D  ); };
        if (imageT == VK_IMAGE_TYPE_2D) { imageViewType = (cInfo.subresourceRange.layerCount() > 1 ? VK_IMAGE_VIEW_TYPE_2D_ARRAY   : VK_IMAGE_VIEW_TYPE_2D  ); };
        if (cInfo.isCubemap)            { imageViewType = (cInfo.subresourceRange.layerCount() > 6 ? VK_IMAGE_VIEW_TYPE_CUBE_ARRAY : VK_IMAGE_VIEW_TYPE_CUBE); };

        //
        vkCheckStatus(vkCreateImageView(deviceObj.device, this.createInfo = VkImageViewCreateInfo.create().sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO).image(cInfo.image).format(format).viewType(imageViewType).subresourceRange(cInfo.subresourceRange).components(cInfo.compontentMapping), null, (this.handle = new Handle("ImageView")).ptr()));
        deviceObj.handleMap.put$(this.handle, this);

        // TODO: multiple pipeline layout support
        if (cInfo.pipelineLayout != 0) {
            var descriptorsObj = (PipelineLayoutObj)deviceObj.handleMap.get(new Handle("PipelineLayout", cInfo.pipelineLayout)).orElse(null);
            this.DSC_ID = descriptorsObj.resources.push(VkDescriptorImageInfo.create().imageView(this.handle.get()).imageLayout(cInfo.type == "storage" ? VK_IMAGE_LAYOUT_GENERAL : VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL));
            descriptorsObj.writeDescriptors();
        }
    }

    //
    public CommandUtils.SubresourceRange subresourceRange() {
        var $DSC_ID = DSC_ID;
        return new CommandUtils.SubresourceRange() {{
            subresource = ((ImageViewCInfo)cInfo).subresourceRange;
            imageViewInfo = new CommandUtils.ImageViewInfo() {{
                image = ((ImageViewCInfo)cInfo).image;
                imageLayout = ((ImageViewCInfo)cInfo).imageLayout;
                imageView = handle.get();
                DSC_ID = $DSC_ID;
            }};
        }};
    }

    //
    public CommandUtils.SubresourceLayers subresourceLayers(int mipLevel) {
        var subresourceRange = ((ImageViewCInfo)cInfo).subresourceRange;
        var $DSC_ID = DSC_ID;
        return new CommandUtils.SubresourceLayers() {{
            subresource = VkImageSubresourceLayers.create()
                .aspectMask(subresourceRange.aspectMask())
                .mipLevel(subresourceRange.baseMipLevel() + mipLevel)
                .baseArrayLayer(subresourceRange.baseArrayLayer())
                .layerCount(subresourceRange.layerCount());
            imageViewInfo = new CommandUtils.ImageViewInfo() {{
                image = ((ImageViewCInfo)cInfo).image;
                imageLayout = ((ImageViewCInfo)cInfo).imageLayout;
                imageView = handle.get();
                DSC_ID = $DSC_ID;
            }};
        }};
    }

    //
    public int getImageLayout() {
        return ((ImageViewCInfo)cInfo).imageLayout;
    }

    //
    public ImageViewObj cmdTransitionBarrierFromInitial(VkCommandBuffer cmdBuf) {
        CommandUtils.cmdTransitionBarrier(cmdBuf, this.subresourceRange().setImageLayout(((ImageViewCInfo)cInfo).imageLayout));
        return this;
    }

    // simpler than traditional image
    public ImageViewObj cmdTransitionBarrier(VkCommandBuffer cmdBuf, int dstImageLayout, boolean fromInitial) {
        if (fromInitial) { this.cmdTransitionBarrierFromInitial(cmdBuf); };
        CommandUtils.cmdTransitionBarrier(cmdBuf, this.subresourceRange().setImageLayout(((ImageViewCInfo)cInfo).imageLayout = dstImageLayout));
        return this;
    }

    /*@Override // TODO: multiple queue family support (and Promise.all)
    public ImageViewObj delete() throws Exception {
        
        var handle = this.handle;
        var cInfo = (ImageViewCInfo)this.cInfo;
        var self = this;

        // just await last process in queue family
        deviceObj.submitOnce(new DeviceObj.SubmitCmd(){{
            queueGroupIndex = cInfo.queueGroupIndex;
            onDone = new Promise<>().thenApply((result)-> {
                //
                if (cInfo.pipelineLayout != 0) {
                    var descriptorsObj = (PipelineLayoutObj)deviceObj.handleMap.get(new Handle("PipelineLayout", cInfo.pipelineLayout)).orElse(null);
                    descriptorsObj.resources.removeIndex(self.DSC_ID);
                    self.DSC_ID = -1;
                }

                vkDestroyImageView(deviceObj.device, handle.get(), null);
                deviceObj.handleMap.remove(handle);
                return null;
            });
        }}, (cmdBuf)->{
            return cmdBuf;
        });
        return this;
    }*/

    @Override // TODO: multiple queue family support (and Promise.all)
    public ImageViewObj deleteDirectly() {
        
        var handle = this.handle;
        var cInfo = (ImageViewCInfo)this.cInfo;
        var self = this;

        //
        if (cInfo.pipelineLayout != 0) {
            var descriptorsObj = (PipelineLayoutObj)deviceObj.handleMap.get(new Handle("PipelineLayout", cInfo.pipelineLayout)).orElse(null);
            descriptorsObj.resources.removeIndex(self.DSC_ID);
            self.DSC_ID = -1;
        }

        vkDestroyImageView(deviceObj.device, handle.get(), null);
        deviceObj.handleMap.remove(handle);

        return this;
    }

}
