package org.hydra2s.noire.objects;

//

import com.lodborg.intervaltree.IntervalTree;
import org.hydra2s.noire.descriptors.MemoryAllocationCInfo;
import org.hydra2s.noire.descriptors.MemoryAllocatorCInfo;
import org.hydra2s.noire.descriptors.UtilsCInfo;
import org.hydra2s.utils.Promise;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import static org.hydra2s.noire.descriptors.UtilsCInfo.vkCheckStatus;
import static org.lwjgl.BufferUtils.createLongBuffer;
import static org.lwjgl.BufferUtils.createPointerBuffer;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.EXTMemoryPriority.VK_STRUCTURE_TYPE_MEMORY_PRIORITY_ALLOCATE_INFO_EXT;
import static org.lwjgl.vulkan.KHRBufferDeviceAddress.VK_MEMORY_ALLOCATE_DEVICE_ADDRESS_BIT_KHR;
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
        public DeviceMemoryObj(UtilsCInfo.Handle base, UtilsCInfo.Handle handle) {
            super(base, handle);

            //
            this.handle = new UtilsCInfo.Handle("DeviceMemory", MemoryUtil.memAddress(createLongBuffer(1)));
            this.allocations = new ArrayList<MemoryAllocationObj>();

            deviceObj.handleMap.put$(this.handle, this);
            memoryAllocatorObj.handleMap.put$(this.handle, this);
        }

        //
        public DeviceMemoryObj(UtilsCInfo.Handle base, MemoryAllocationCInfo cInfo) {
            super(base, cInfo);

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
            vkCheckStatus(vkAllocateMemory(deviceObj.device, this.allocInfo =
                VkMemoryAllocateInfo.create()
                    .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                    .memoryTypeIndex(memoryTypeIndex)
                    .allocationSize(cInfo.memoryRequirements.size())
                    .pNext(VkMemoryAllocateFlagsInfo.create()
                        .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_FLAGS_INFO)
                        .flags(cInfo.buffer != null ? VK_MEMORY_ALLOCATE_DEVICE_ADDRESS_BIT_KHR : 0)
                        .pNext(VkMemoryDedicatedAllocateInfo.create()
                            .pNext(VkExportMemoryAllocateInfo.create()
                                .pNext(VkMemoryPriorityAllocateInfoEXT.create()
                                    .sType(VK_STRUCTURE_TYPE_MEMORY_PRIORITY_ALLOCATE_INFO_EXT)
                                    .priority(1.F)
                                    .address())
                                .sType(VK_STRUCTURE_TYPE_EXPORT_MEMORY_ALLOCATE_INFO)
                                .handleTypes(VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_WIN32_BIT).address())
                            .sType(VK_STRUCTURE_TYPE_MEMORY_DEDICATED_ALLOCATE_INFO).address())
                        .address()), null, (this.handle = new UtilsCInfo.Handle("DeviceMemory")).ptr()));

            // TODO: Linux support
            //vkGetMemoryWin32HandleKHR(deviceObj.device, VkMemoryGetWin32HandleInfoKHR.create().sType(VK_STRUCTURE_TYPE_MEMORY_GET_WIN32_HANDLE_INFO_KHR).memory(this.handle.get()).handleType(VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_WIN32_BIT), Win32Handle = memAllocPointer(1));

            //
            deviceObj.handleMap.put$(this.handle, this);
            this.mappedPtr = createPointerBuffer(1);
            this.allocations = new ArrayList<MemoryAllocationObj>();
        }

        //
        public ByteBuffer map(long byteLength, long byteOffset) {
            //var cInfo = (MemoryAllocatorCInfo.DeviceMemoryCInfo)this.cInfo;
            var cInfo = (MemoryAllocationCInfo)this.cInfo;

            //
            long BO = byteOffset;
            long BS = Math.min(byteLength, this.allocInfo.allocationSize() - byteOffset);

            //
            //if (mapped) { this.unmap(); };
            if (!mapped) {
                //vkMapMemory(deviceObj.device, this.handle.get(), BO, BS, 0, dataPtr);
                vkCheckStatus(vkMapMemory(deviceObj.device, this.handle.get(), 0, VK_WHOLE_SIZE, 0, this.mappedPtr));
                mapped = true;
            }

            // WARNING! Limited up to 2Gb, due negative integer
            //return memSlice(memByteBuffer(this.mappedPtr.get(0), (int) ((VkMemoryAllocateInfo)this.allocInfo).allocationSize()), (int) BO, (int) Math.min(BS, 0x7FFFFFFFL));
            return memByteBuffer(this.mappedPtr.get(0) + BO, (int) Math.min(BS, 0x7FFFFFFFL));
        }

        public DeviceMemoryObj flushMapped(long size, long offset) {
            vkCheckStatus(vkFlushMappedMemoryRanges(deviceObj.device, VkMappedMemoryRange.create()
                .sType(VK_STRUCTURE_TYPE_MAPPED_MEMORY_RANGE)
                .offset(offset)
                .memory(this.handle.get())
                .size(size)
            ));
            return this;
        }

        public DeviceMemoryObj invalidateMapped(long size, long offset) {
            vkCheckStatus(vkInvalidateMappedMemoryRanges(deviceObj.device, VkMappedMemoryRange.create()
                .sType(VK_STRUCTURE_TYPE_MAPPED_MEMORY_RANGE)
                .offset(offset)
                .memory(this.handle.get())
                .size(size)
            ));
            return this;
        }

        //
        public DeviceMemoryObj unmap() {
            vkUnmapMemory(deviceObj.device, this.handle.get()); mapped = false;
            return this;
        }

        /*@Override // TODO: multiple queue family support
        public DeviceMemoryObj delete() throws Exception {
            var handle = this.handle;
            
            deviceObj.submitOnce(new DeviceObj.SubmitCmd(){{
                queueGroupIndex = cInfo.queueGroupIndex;
                onDone = new Promise<>().thenApply((result)->{
                    vkFreeMemory(deviceObj.device, handle.get(), null);
                    deviceObj.handleMap.remove(handle);
                    return null;
                });
            }}, (cmdBuf)->{
                return cmdBuf;
            });
            return this;
        }*/

        @Override // TODO: multiple queue family support
        public DeviceMemoryObj deleteDirectly() {
            var handle = this.handle;

            vkDeviceWaitIdle(deviceObj.device);
            vkFreeMemory(deviceObj.device, handle.get(), null);
            deviceObj.handleMap.remove(handle);
            return null;
        }
    }

    //
    public MemoryAllocatorObj(UtilsCInfo.Handle base, UtilsCInfo.Handle handle) {
        super(base, handle);
        BasicObj.globalHandleMap.put$(this.handle.get(), this);
        handleMap = new UtilsCInfo.CombinedMap<UtilsCInfo.Handle, BasicObj>();
        rootMap = new UtilsCInfo.CombinedMap<Long, Long>();
        addressMap = new IntervalTree<>();
    }

    //
    public MemoryAllocatorObj(UtilsCInfo.Handle base, MemoryAllocatorCInfo cInfo) {
        super(base, cInfo);
        this.handle = new UtilsCInfo.Handle("MemoryAllocator", MemoryUtil.memAddress(createLongBuffer(1)));
        BasicObj.globalHandleMap.put$(this.handle.get(), this);
    };

    //
    public MemoryAllocationObj allocateMemory(MemoryAllocationCInfo cInfo, MemoryAllocationObj memoryAllocationObj) {
        
        

        //
        var deviceMemory = new DeviceMemoryObj(this.handle, cInfo);
        memoryAllocationObj.memoryOffset = 0L;
        memoryAllocationObj.deviceMemory = deviceMemory.handle.ptr();
        memoryAllocationObj.memorySize = cInfo.memoryRequirements.size();
        memoryAllocationObj.deviceMemoryObj = deviceMemory;

        // register allocation
        // TODO: shared pointer support (alike C++)
        deviceMemory.allocations.add(memoryAllocationObj);

        //
        if (cInfo.buffer != null && cInfo.buffer[0] != 0) { vkBindBufferMemory(deviceObj.device, cInfo.buffer[0], memoryAllocationObj.deviceMemory[0], memoryAllocationObj.memoryOffset); };
        if (cInfo.image != null && cInfo.image[0] != 0) { vkBindImageMemory(deviceObj.device, cInfo.image[0], memoryAllocationObj.deviceMemory[0], memoryAllocationObj.memoryOffset); };

        //
        return memoryAllocationObj;
    }
}
