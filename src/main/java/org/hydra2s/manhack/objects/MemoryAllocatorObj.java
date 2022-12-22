package org.hydra2s.manhack.objects;

//
import org.hydra2s.manhack.descriptors.MemoryAllocationCInfo;
import org.hydra2s.manhack.descriptors.MemoryAllocatorCInfo;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkMemoryAllocateFlagsInfo;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryDedicatedAllocateInfo;

//
import static org.lwjgl.system.MemoryUtil.memAllocLong;
import static org.lwjgl.vulkan.KHRBufferDeviceAddress.VK_MEMORY_ALLOCATE_DEVICE_ADDRESS_BIT_KHR;
import static org.lwjgl.vulkan.VK10.*;

//
public class MemoryAllocatorObj extends BasicObj  {
    static public class DeviceMemoryObj extends BasicObj  {
        protected VkMemoryAllocateInfo allocInfo = null;

        public DeviceMemoryObj(Handle base, Handle handle) {
            super(base, handle);

            //
            this.handle = new Handle("DeviceMemory", MemoryUtil.memAddress(memAllocLong(1)));

            //
            var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get());
            var physicalDeviceObj = (PhysicalDeviceObj)BasicObj.globalHandleMap.get(deviceObj.base.get());

            //
            deviceObj.handleMap.put(this.handle, this);
        }

        public DeviceMemoryObj(Handle base, MemoryAllocationCInfo cInfo) {
            super(base, cInfo);

            //
            var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get());
            var physicalDeviceObj = (PhysicalDeviceObj)BasicObj.globalHandleMap.get(deviceObj.base.get());

            //
            var isBAR = (cInfo.isHost && cInfo.isDevice);
            var hostBased = (cInfo.isHost && !cInfo.isDevice);
            var propertyFlag = isBAR ? (
                VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT|
                VK_MEMORY_PROPERTY_HOST_COHERENT_BIT|
                VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT
            ) : (hostBased ?
            (
                VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT |
                VK_MEMORY_PROPERTY_HOST_COHERENT_BIT |
                VK_MEMORY_PROPERTY_HOST_CACHED_BIT
            ) : VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT);

            //
            var memoryTypeIndex = physicalDeviceObj.getMemoryTypeIndex(cInfo.memoryRequirements.memoryTypeBits(), propertyFlag, hostBased ? VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT : 0, cInfo.memoryRequirements.size());

            // host memory fallback (but FPS will drop), especially due for budget end
            if (memoryTypeIndex < 0) {
                memoryTypeIndex = physicalDeviceObj.getMemoryTypeIndex(cInfo.memoryRequirements.memoryTypeBits(),
                    (isBAR ? true : !hostBased) ?
                    (
                        VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT |
                        VK_MEMORY_PROPERTY_HOST_COHERENT_BIT |
                        VK_MEMORY_PROPERTY_HOST_CACHED_BIT
                    ) : VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT,
                    0,
                    cInfo.memoryRequirements.size()
                );
            };

            //
            vkAllocateMemory(deviceObj.device, this.allocInfo =
                VkMemoryAllocateInfo.create()
                    .memoryTypeIndex(memoryTypeIndex)
                    .allocationSize(cInfo.memoryRequirements.size())
                    .pNext(VkMemoryAllocateFlagsInfo.create()
                        .flags(cInfo.buffer != null ? VK_MEMORY_ALLOCATE_DEVICE_ADDRESS_BIT_KHR : 0)
                        .pNext(VkMemoryDedicatedAllocateInfo.create().address())
                        .address()), null, (this.handle = new Handle("DeviceMemory")).ptr().getLongBuffer(1));

            //
            deviceObj.handleMap.put(this.handle, this);
        }
    }

    public MemoryAllocatorObj(Handle base, Handle handle) {
        super(base, handle);
    }

    public MemoryAllocatorObj(Handle base, MemoryAllocatorCInfo cInfo) {
        super(base, cInfo);

        //
        this.handle = new Handle("MemoryAllocator", MemoryUtil.memAddress(memAllocLong(1)));

    };

    //
    public MemoryAllocationObj allocateMemory(MemoryAllocationCInfo cInfo, MemoryAllocationObj memoryAllocationObj) {
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get());
        var physicalDeviceObj = (PhysicalDeviceObj)BasicObj.globalHandleMap.get(deviceObj.base.get());

        //
        if (memoryAllocationObj.isBuffer) { cInfo.buffer = memoryAllocationObj.handle.ptr(); };
        if (memoryAllocationObj.isImage) { cInfo.image = memoryAllocationObj.handle.ptr(); };

        //
        var deviceMemory = new DeviceMemoryObj(this.base, cInfo);

        //
        memoryAllocationObj.memoryOffset = 0L;
        memoryAllocationObj.deviceMemory = deviceMemory.handle.ptr();

        //
        if (memoryAllocationObj.isBuffer) { vkBindBufferMemory(deviceObj.device, memoryAllocationObj.handle.get(), deviceMemory.handle.get(), memoryAllocationObj.memoryOffset); };
        if (memoryAllocationObj.isImage) { vkBindImageMemory(deviceObj.device, memoryAllocationObj.handle.get(), deviceMemory.handle.get(), memoryAllocationObj.memoryOffset); };

        //
        return memoryAllocationObj;
    }
}
