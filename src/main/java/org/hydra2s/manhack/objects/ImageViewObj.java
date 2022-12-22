package org.hydra2s.manhack.objects;

//
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_UNDEFINED;

//
public class ImageViewObj extends BasicObj {
    public int imageLayout = VK_IMAGE_LAYOUT_UNDEFINED;
    public ImageViewObj(Handle base, Handle handle) {
        super(base, handle);
    }
}
