package org.hydra2s.noire.descriptors;

import org.lwjgl.vulkan.VkImageSubresourceLayers;
import org.lwjgl.vulkan.VkOffset3D;

import static org.lwjgl.vulkan.VK10.*;

public class CopyInfoCInfo extends BasicCInfo {

    public static class ImageViewCopyInfo {
        // TODO: fix device requirements issues
        public long device = 0L;
        public long imageView = 0L;
        public VkOffset3D offset = VkOffset3D.calloc().set(0, 0, 0);
        public int mipLevel = 0;
    }

    public static class BufferRangeCopyInfo {
        public long buffer;
        public long offset = 0L;
        public long range = VK_WHOLE_SIZE;
    }

    public static class ImageCopyInfo {
        public long image = 0L;
        public int imageLayout = VK_IMAGE_LAYOUT_GENERAL;
        public VkOffset3D offset = VkOffset3D.calloc().set(0, 0, 0);
        public VkImageSubresourceLayers subresource = VkImageSubresourceLayers.calloc().set(VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1);
    }

}
