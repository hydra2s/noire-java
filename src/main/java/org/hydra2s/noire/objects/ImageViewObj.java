package org.hydra2s.noire.objects;

//
import org.hydra2s.noire.descriptors.BasicCInfo;
import org.hydra2s.noire.descriptors.ImageViewCInfo;
import org.hydra2s.utils.Promise;
import org.lwjgl.vulkan.*;

//
import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.system.MemoryUtil.memLongBuffer;
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
        vkCreateImageView(deviceObj.device, this.createInfo = VkImageViewCreateInfo.calloc().sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO).image(cInfo.image).format(format).viewType(imageViewType).subresourceRange(cInfo.subresourceRange).components(cInfo.compontentMapping), null, memLongBuffer(memAddress((this.handle = new Handle("ImageView")).ptr(), 0), 1));
        deviceObj.handleMap.put$(this.handle, this);

        // TODO: multiple pipeline layout support
        if (cInfo.pipelineLayout != 0) {
            var descriptorsObj = (PipelineLayoutObj)deviceObj.handleMap.get(new Handle("PipelineLayout", cInfo.pipelineLayout)).orElse(null);
            this.DSC_ID = descriptorsObj.resources.push(VkDescriptorImageInfo.calloc().imageView(this.handle.get()).imageLayout(cInfo.imageLayout));
            descriptorsObj.writeDescriptors();
        }
    }

    // TODO: will critically needed in future usage!
    public ImageViewCInfo.CriticalDump getCriticalDump() {
        var $DSC_ID = DSC_ID;
        return new ImageViewCInfo.CriticalDump() {{
            DSC_ID = $DSC_ID;
            image = createInfo.image();
            subresource = subresourceRange();
            imageLayout = getImageLayout();
            imageView = handle.get();
        }};
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
    public int getImageLayout() {
        return ((ImageViewCInfo)cInfo).imageLayout;
    }

    //
    public ImageViewObj cmdTransitionBarrierFromInitial(VkCommandBuffer cmdBuf) {
        CopyUtilObj.cmdTransitionBarrier(cmdBuf, ((ImageViewCInfo)cInfo).image, VK_IMAGE_LAYOUT_UNDEFINED, ((ImageViewCInfo)cInfo).imageLayout, ((ImageViewCInfo)cInfo).subresourceRange);
        return this;
    }

    // simpler than traditional image
    public ImageViewObj cmdTransitionBarrier(VkCommandBuffer cmdBuf, int dstImageLayout, boolean fromInitial) {
        if (fromInitial) { this.cmdTransitionBarrierFromInitial(cmdBuf); };
        CopyUtilObj.cmdTransitionBarrier(cmdBuf, ((ImageViewCInfo)cInfo).image, ((ImageViewCInfo)cInfo).imageLayout, dstImageLayout, ((ImageViewCInfo)cInfo).subresourceRange);
        ((ImageViewCInfo)cInfo).imageLayout = dstImageLayout;
        return this;
    }

    @Override // TODO: multiple queue family support (and Promise.all)
    public ImageViewObj delete() {
        
        var handle = this.handle;
        var cInfo = (ImageViewCInfo)this.cInfo;
        var self = this;

        // just await last process in queue family
        deviceObj.submitOnce(new BasicCInfo.SubmitCmd(){{
            queueFamilyIndex = cInfo.queueFamilyIndex;
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
            return VK_SUCCESS;
        });
        return this;
    }

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
