package org.hydra2s.noire.objects;

//
import org.hydra2s.noire.descriptors.BasicCInfo;
import org.hydra2s.noire.descriptors.MemoryAllocationCInfo;
import org.hydra2s.noire.descriptors.MemoryAllocatorCInfo;
import org.hydra2s.noire.descriptors.SwapChainCInfo;
import org.hydra2s.utils.Promise;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

//
import java.nio.ByteBuffer;
import java.util.ArrayList;

//
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.EXTMemoryPriority.VK_STRUCTURE_TYPE_MEMORY_PRIORITY_ALLOCATE_INFO_EXT;
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
        protected PointerBuffer mappedPtr = null;

        // TODO: interval tree support
        protected ArrayList<MemoryAllocationObj> allocations = null;

        //
        public DeviceMemoryObj(Handle base, Handle handle) {
            super(base, handle);

            //
            var memoryAllocatorObj = (MemoryAllocatorObj)BasicObj.globalHandleMap.get(this.base.get());
            var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(memoryAllocatorObj.base.get());
            var physicalDeviceObj = (PhysicalDeviceObj)BasicObj.globalHandleMap.get(deviceObj.base.get());

            //
            this.handle = new Handle("DeviceMemory", MemoryUtil.memAddress(memAllocLong(1)));
            this.allocations = new ArrayList<MemoryAllocationObj>();

            deviceObj.handleMap.put(this.handle, this);
            memoryAllocatorObj.handleMap.put(this.handle, this);
        }

        //
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
                VkMemoryAllocateInfo.calloc()
                    .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                    .memoryTypeIndex(memoryTypeIndex)
                    .allocationSize(cInfo.memoryRequirements.size())
                    .pNext(VkMemoryAllocateFlagsInfo.calloc()
                        .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_FLAGS_INFO)
                        .flags(cInfo.buffer != null ? VK_MEMORY_ALLOCATE_DEVICE_ADDRESS_BIT_KHR : 0)
                        .pNext(VkMemoryDedicatedAllocateInfo.calloc()
                            .pNext(VkExportMemoryAllocateInfo.calloc()
                                .pNext(VkMemoryPriorityAllocateInfoEXT.calloc()
                                    .sType(VK_STRUCTURE_TYPE_MEMORY_PRIORITY_ALLOCATE_INFO_EXT)
                                    .priority(1.F)
                                    .address())
                                .sType(VK_STRUCTURE_TYPE_EXPORT_MEMORY_ALLOCATE_INFO)
                                .handleTypes(VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_WIN32_BIT).address())
                            .sType(VK_STRUCTURE_TYPE_MEMORY_DEDICATED_ALLOCATE_INFO).address())
                        .address()), null, memLongBuffer(memAddress((this.handle = new Handle("DeviceMemory")).ptr(), 0), 1));

            // TODO: Linux support
            //vkGetMemoryWin32HandleKHR(deviceObj.device, VkMemoryGetWin32HandleInfoKHR.calloc().sType(VK_STRUCTURE_TYPE_MEMORY_GET_WIN32_HANDLE_INFO_KHR).memory(this.handle.get()).handleType(VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_WIN32_BIT), Win32Handle = memAllocPointer(1));

            //
            deviceObj.handleMap.put(this.handle, this);
            this.mappedPtr = memAllocPointer(1);
            this.allocations = new ArrayList<MemoryAllocationObj>();
        }

        //
        public ByteBuffer map(long byteLength, long byteOffset) {
            var memoryAllocatorObj = (MemoryAllocatorObj)BasicObj.globalHandleMap.get(this.base.get());
            var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(memoryAllocatorObj.base.get());
            var physicalDeviceObj = (PhysicalDeviceObj)BasicObj.globalHandleMap.get(deviceObj.base.get());

            //
            //var cInfo = (MemoryAllocatorCInfo.DeviceMemoryCInfo)this.cInfo;
            var cInfo = (MemoryAllocationCInfo)this.cInfo;

            //
            long BO = byteOffset;
            long BS = Math.min(byteLength, this.allocInfo.allocationSize() - byteOffset);

            //
            //if (mapped) { this.unmap(); };
            if (!mapped) {
                //vkMapMemory(deviceObj.device, this.handle.get(), BO, BS, 0, dataPtr);
                vkMapMemory(deviceObj.device, this.handle.get(), 0, VK_WHOLE_SIZE, 0, this.mappedPtr);
                mapped = true;
            }

            // WARNING! Limited up to 2Gb, due negative integer
            //return memSlice(memByteBuffer(this.mappedPtr.get(0), (int) ((VkMemoryAllocateInfo)this.allocInfo).allocationSize()), (int) BO, (int) Math.min(BS, 0x7FFFFFFFL));
            return memByteBuffer(this.mappedPtr.get(0) + BO, (int) Math.min(BS, 0x7FFFFFFFL));
        }

        public DeviceMemoryObj flushMapped(long size, long offset) {
            var memoryAllocatorObj = (MemoryAllocatorObj)BasicObj.globalHandleMap.get(this.base.get());
            var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(memoryAllocatorObj.base.get());
            vkFlushMappedMemoryRanges(deviceObj.device, VkMappedMemoryRange.calloc()
                .sType(VK_STRUCTURE_TYPE_MAPPED_MEMORY_RANGE)
                .offset(offset)
                .memory(this.handle.get())
                .size(size)
            );
            return this;
        }

        public DeviceMemoryObj invalidateMapped(long size, long offset) {
            var memoryAllocatorObj = (MemoryAllocatorObj)BasicObj.globalHandleMap.get(this.base.get());
            var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(memoryAllocatorObj.base.get());
            vkInvalidateMappedMemoryRanges(deviceObj.device, VkMappedMemoryRange.calloc()
                .sType(VK_STRUCTURE_TYPE_MAPPED_MEMORY_RANGE)
                .offset(offset)
                .memory(this.handle.get())
                .size(size)
            );
            return this;
        }

        //
        public DeviceMemoryObj unmap() {
            var memoryAllocatorObj = (MemoryAllocatorObj)BasicObj.globalHandleMap.get(this.base.get());
            var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(memoryAllocatorObj.base.get());
            var physicalDeviceObj = (PhysicalDeviceObj)BasicObj.globalHandleMap.get(deviceObj.base.get());

            //
            vkUnmapMemory(deviceObj.device, this.handle.get()); mapped = false;
            return this;
        }

        @Override // TODO: multiple queue family support
        public DeviceMemoryObj delete() {
            var handle = this.handle;
            var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get());
            deviceObj.submitOnce(deviceObj.getCommandPool(cInfo.queueFamilyIndex), new BasicCInfo.SubmitCmd(){{
                queueFamilyIndex = cInfo.queueFamilyIndex;
                queue = deviceObj.getQueue(cInfo.queueFamilyIndex, 0);
                onDone = new Promise<>().thenApply((result)->{
                    vkFreeMemory(deviceObj.device, handle.get(), null);
                    deviceObj.handleMap.remove(handle);
                    return null;
                });
            }}, (cmdBuf)->{
                return VK_SUCCESS;
            });
            return this;
        }

        @Override // TODO: multiple queue family support
        public DeviceMemoryObj deleteDirectly() {
            var handle = this.handle;
            var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get());
            vkFreeMemory(deviceObj.device, handle.get(), null);
            deviceObj.handleMap.remove(handle);
            return null;
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
        var deviceMemory = new DeviceMemoryObj(this.handle, cInfo);
        memoryAllocationObj.memoryOffset = 0L;
        memoryAllocationObj.deviceMemory = deviceMemory.handle.ptr();
        memoryAllocationObj.memorySize = cInfo.memoryRequirements.size();

        // register allocation
        // TODO: shared pointer support (alike C++)
        deviceMemory.allocations.add(memoryAllocationObj);

        //
        if (cInfo.buffer != null && cInfo.buffer.get(0) != 0) { vkBindBufferMemory(deviceObj.device, cInfo.buffer.get(0), memoryAllocationObj.deviceMemory.get(0), memoryAllocationObj.memoryOffset); };
        if (cInfo.image != null && cInfo.image.get(0) != 0) { vkBindImageMemory(deviceObj.device, cInfo.image.get(0), memoryAllocationObj.deviceMemory.get(0), memoryAllocationObj.memoryOffset); };

        //
        return memoryAllocationObj;
    }
}
