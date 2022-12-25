package org.hydra2s.noire.descriptors;

//
import org.lwjgl.vulkan.VkComponentMapping;
import org.lwjgl.vulkan.VkImageSubresourceRange;

//
import static org.lwjgl.vulkan.VK10.*;

//
public class ImageViewCInfo extends BasicCInfo  {

    //
    public long pipelineLayout = 0;
    public long image = 0;
    public VkImageSubresourceRange subresourceRange = null;
    public boolean isCubemap = false;
    public String type = "sampled";
    public int imageLayout = VK_IMAGE_LAYOUT_UNDEFINED;

    //
    public VkComponentMapping compontentMapping = VkComponentMapping.create()
        .r(VK_COMPONENT_SWIZZLE_R)
        .g(VK_COMPONENT_SWIZZLE_G)
        .b(VK_COMPONENT_SWIZZLE_B)
        .a(VK_COMPONENT_SWIZZLE_A);

}
