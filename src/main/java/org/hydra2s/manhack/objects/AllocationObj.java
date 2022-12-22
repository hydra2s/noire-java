package org.hydra2s.manhack.objects;

import org.hydra2s.manhack.descriptors.AllocationCInfo;
import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.*;

import static org.lwjgl.system.MemoryUtil.memAllocPointer;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_TRANSFER_DST_BIT;
import static org.lwjgl.vulkan.VK11.vkGetBufferMemoryRequirements2;
import static org.lwjgl.vulkan.VK11.vkGetImageMemoryRequirements2;
import static org.lwjgl.vulkan.VK12.VK_BUFFER_USAGE_SHADER_DEVICE_ADDRESS_BIT;
import static org.lwjgl.vulkan.VK12.vkGetBufferDeviceAddress;

public class AllocationObj extends BasicObj {

    //
    public long memoryOffset = 0;
    public boolean isBuffer = false;
    public boolean isImage = false;

    //
    public PointerBuffer deviceMemory = memAllocPointer(1);
    public VkMemoryRequirements2 memoryRequirements2 = null;

    //
    public AllocationObj(Handle base, Handle handle) {
        super(base, handle);
    }
    public AllocationObj(Handle base, AllocationCInfo handle) {
        super(base, handle);
    }

    //
    static public class BufferObj extends AllocationObj {
        public VkBufferCreateInfo createInfo = null;

        public BufferObj(Handle base, Handle handle) {
            super(base, handle);
        }

        public BufferObj(Handle base, AllocationCInfo.BufferCInfo cInfo) {
            super(base, cInfo);

            //
            var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get());
            var physicalDeviceObj = (PhysicalDeviceObj)BasicObj.globalHandleMap.get(deviceObj.base.get());

            //
            vkCreateBuffer(deviceObj.device, this.createInfo = VkBufferCreateInfo.create()
                    .size(cInfo.size)
                    .usage(cInfo.usage | VK_BUFFER_USAGE_SHADER_DEVICE_ADDRESS_BIT | VK_BUFFER_USAGE_TRANSFER_SRC_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT),
                    null,
                    (this.handle = new Handle("Buffer")).ptr().getLongBuffer(1)
            );

            //
            vkGetBufferMemoryRequirements2(deviceObj.device, VkBufferMemoryRequirementsInfo2.create().buffer(this.handle.get()), this.memoryRequirements2 = VkMemoryRequirements2.create());
        }
    }

    //
    static public class ImageObj extends AllocationObj {
        public VkImageCreateInfo createInfo = null;
        public ImageObj(Handle base, Handle handle) {
            super(base, handle);
        }
        public long deviceAddress = 0L;

        public ImageObj(Handle base, AllocationCInfo.ImageCInfo cInfo) {
            super(base, cInfo);

            //
            var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get());
            var physicalDeviceObj = (PhysicalDeviceObj)BasicObj.globalHandleMap.get(deviceObj.base.get());

            //
            vkCreateImage(deviceObj.device, this.createInfo = VkImageCreateInfo.create()
                    .extent(cInfo.extent3D)
                    .usage(cInfo.usage | VK_IMAGE_USAGE_TRANSFER_SRC_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT),
                    null,
                    (this.handle = new Handle("Image")).ptr().getLongBuffer(1)
            );

            //
            vkGetImageMemoryRequirements2(deviceObj.device, VkImageMemoryRequirementsInfo2.create().image(this.handle.get()), this.memoryRequirements2 = VkMemoryRequirements2.create());
        }

        long getDeviceAddress() {
            var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get());
            return this.deviceAddress == 0 ? (this.deviceAddress = vkGetBufferDeviceAddress(deviceObj.device, VkBufferDeviceAddressInfo.create().buffer(this.handle.get()))) : this.deviceAddress;
        }
    }

}
