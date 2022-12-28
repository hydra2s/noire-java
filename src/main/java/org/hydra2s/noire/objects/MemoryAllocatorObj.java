package org.hydra2s.noire.objects;

//
import org.hydra2s.noire.descriptors.MemoryAllocationCInfo;
import org.hydra2s.noire.descriptors.MemoryAllocatorCInfo;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

//
import java.nio.ByteBuffer;

//
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.KHRBufferDeviceAddress.VK_MEMORY_ALLOCATE_DEVICE_ADDRESS_BIT_KHR;
import static org.lwjgl.vulkan.KHRExternalMemoryWin32.VK_STRUCTURE_TYPE_MEMORY_GET_WIN32_HANDLE_INFO_KHR;
import static org.lwjgl.vulkan.KHRExternalMemoryWin32.vkGetMemoryWin32HandleKHR;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK11.*;

//
public class MemoryAllocatorObj extends BasicObj  {
    static public class DeviceMemoryObj extends BasicObj  {
        protected VkMemoryAllocateInfo allocInfo = null;
        protected boolean mapped = false;

        public DeviceMemoryObj(Handle base, Handle handle) {
            super(base, handle);

            //
            var memoryAllocatorObj = (MemoryAllocatorObj)BasicObj.globalHandleMap.get(this.base.get());
            var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(memoryAllocatorObj.base.get());
            var physicalDeviceObj = (PhysicalDeviceObj)BasicObj.globalHandleMap.get(deviceObj.base.get());

            //
            this.handle = new Handle("DeviceMemory", MemoryUtil.memAddress(memAllocLong(1)));
            deviceObj.handleMap.put(this.handle, this);
            memoryAllocatorObj.handleMap.put(this.handle, this);
        }

        public DeviceMemoryObj(Handle base, MemoryAllocationCInfo cInfo) {
            super(base, cInfo);

            //
            var memoryAllocatorObj = (MemoryAllocatorObj)BasicObj.globalHandleMap.get(this.base.get());
            var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(memoryAllocatorObj.base.get());
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
                    .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                    .memoryTypeIndex(memoryTypeIndex)
                    .allocationSize(cInfo.memoryRequirements.size())
                    .pNext(VkMemoryAllocateFlagsInfo.create()
                        .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_FLAGS_INFO)
                        .flags(cInfo.buffer != null ? VK_MEMORY_ALLOCATE_DEVICE_ADDRESS_BIT_KHR : 0)
                        .pNext(VkMemoryDedicatedAllocateInfo.create()
                            .pNext(VkExportMemoryAllocateInfo.create()
                                .sType(VK_STRUCTURE_TYPE_EXPORT_MEMORY_ALLOCATE_INFO)
                                .handleTypes(VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_WIN32_BIT).address())
                            .sType(VK_STRUCTURE_TYPE_MEMORY_DEDICATED_ALLOCATE_INFO).address())
                        .address()), null, memLongBuffer(memAddress((this.handle = new Handle("DeviceMemory")).ptr(), 0), 1));

            // TODO: Linux support
            vkGetMemoryWin32HandleKHR(deviceObj.device, VkMemoryGetWin32HandleInfoKHR.create().sType(VK_STRUCTURE_TYPE_MEMORY_GET_WIN32_HANDLE_INFO_KHR).memory(this.handle.get()).handleType(VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_WIN32_BIT), Win32Handle = memAllocPointer(1));

            //
            deviceObj.handleMap.put(this.handle, this);
        }

        public ByteBuffer map(long byteLength, long byteOffset) {
            var memoryAllocatorObj = (MemoryAllocatorObj)BasicObj.globalHandleMap.get(this.base.get());
            var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(memoryAllocatorObj.base.get());
            var physicalDeviceObj = (PhysicalDeviceObj)BasicObj.globalHandleMap.get(deviceObj.base.get());

            //
            PointerBuffer dataPtr = memAllocPointer(1);
            long BO = byteOffset;
            long BS = (byteLength != 0 || byteLength == VK_WHOLE_SIZE) ? byteLength : Math.min(((VkMemoryAllocateInfo)this.allocInfo).allocationSize(), byteLength);
            long BM = BS == VK_WHOLE_SIZE || BS < 0 ? ((VkMemoryAllocateInfo)this.allocInfo).allocationSize() : BS;

            //
            if (mapped) { this.unmap(); };
            vkMapMemory(deviceObj.device, this.handle.get(), BO, BS, 0, dataPtr); mapped = true;
            return memByteBuffer(dataPtr.get(0), (int)BM);
        }

        public void unmap() {
            var memoryAllocatorObj = (MemoryAllocatorObj)BasicObj.globalHandleMap.get(this.base.get());
            var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(memoryAllocatorObj.base.get());
            var physicalDeviceObj = (PhysicalDeviceObj)BasicObj.globalHandleMap.get(deviceObj.base.get());

            //
            vkUnmapMemory(deviceObj.device, this.handle.get()); mapped = false;
        }
    }

    //
    public MemoryAllocatorObj(Handle base, Handle handle) {
        super(base, handle);
        BasicObj.globalHandleMap.put(this.handle.get(), this);
    }

    //
    public MemoryAllocatorObj(Handle base, MemoryAllocatorCInfo cInfo) {
        super(base, cInfo);
        this.handle = new Handle("MemoryAllocator", MemoryUtil.memAddress(memAllocLong(1)));
        BasicObj.globalHandleMap.put(this.handle.get(), this);
    };

    //
    public MemoryAllocationObj allocateMemory(MemoryAllocationCInfo cInfo, MemoryAllocationObj memoryAllocationObj) {
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get());
        var physicalDeviceObj = (PhysicalDeviceObj)BasicObj.globalHandleMap.get(deviceObj.base.get());

        //
        if (memoryAllocationObj.isBuffer) { cInfo.buffer = memoryAllocationObj.handle.ptr(); };
        if (memoryAllocationObj.isImage) { cInfo.image = memoryAllocationObj.handle.ptr(); };
        cInfo.memoryRequirements2 = memoryAllocationObj.memoryRequirements2;
        cInfo.memoryRequirements = cInfo.memoryRequirements2.memoryRequirements();

        //
        memoryAllocationObj.memoryOffset = 0L;
        memoryAllocationObj.deviceMemory = (new DeviceMemoryObj(this.handle, cInfo)).handle.ptr();

        //
        if (memoryAllocationObj.isBuffer) { vkBindBufferMemory(deviceObj.device, memoryAllocationObj.handle.get(), memoryAllocationObj.deviceMemory.get(0), memoryAllocationObj.memoryOffset); };
        if (memoryAllocationObj.isImage) { vkBindImageMemory(deviceObj.device, memoryAllocationObj.handle.get(), memoryAllocationObj.deviceMemory.get(0), memoryAllocationObj.memoryOffset); };

        //
        //memoryAllocationObj.Win32Handle

        //
        return memoryAllocationObj;
    }
}
