package org.hydra2s.noire.objects;

import com.lodborg.intervaltree.Interval;
import com.lodborg.intervaltree.LongInterval;
import org.hydra2s.noire.descriptors.BasicCInfo;
import org.hydra2s.noire.descriptors.BufferCInfo;
import org.hydra2s.noire.descriptors.SwapChainCInfo;
import org.hydra2s.utils.Promise;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;

import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.system.MemoryUtil.memLongBuffer;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK11.*;
import static org.lwjgl.vulkan.VK12.*;
import static org.lwjgl.vulkan.VK13.*;

//
// DEFAULT IS ResizableBAR!
// probably, will be replaced by modules for allocation
// not sure about that
public class BufferObj extends BasicObj {
    public long allocationHandle = 0L;
    public Object allocationInfo = null;
    public long allocationInfoAddress = 0L;

    public VkBufferCreateInfo createInfo = null;
    public long deviceAddress = 0L;

    //
    public VkMemoryRequirements2 memoryRequirements2 = null;

    //
    public BufferObj(Handle base, Handle handle) {
        super(base, handle);
    }

    // TODO: create buffer by allocator (such as VMA)
    public BufferObj(Handle base, BufferCInfo cInfo) {
        super(base, cInfo);

        //
        var deviceObj = (DeviceObj) BasicObj.globalHandleMap.get(this.base.get());
        var physicalDeviceObj = (PhysicalDeviceObj) BasicObj.globalHandleMap.get(deviceObj.base.get());

        //
        this.createInfo = VkBufferCreateInfo.calloc()
                .pNext(VkExternalMemoryBufferCreateInfo.calloc()
                        .pNext(0L)
                        .sType(VK_STRUCTURE_TYPE_EXTERNAL_MEMORY_BUFFER_CREATE_INFO)
                        .handleTypes(VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_WIN32_BIT).address()
                )
                .flags(0)
                .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                .size(Math.max(cInfo.size, 0L))
                .usage(cInfo.usage | VK_BUFFER_USAGE_SHADER_DEVICE_ADDRESS_BIT | VK_BUFFER_USAGE_TRANSFER_SRC_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT)
                .sharingMode(VK_SHARING_MODE_EXCLUSIVE)
                .pQueueFamilyIndices(null);

        //
        int status = VK_NOT_READY;
        if (cInfo.buffer == null || cInfo.buffer.get(0) == 0) {
            status = vkCreateBuffer(deviceObj.device, this.createInfo, null, memLongBuffer(memAddress((this.handle = new Handle("Buffer")).ptr(), 0), 1));
        } else {
            this.handle = new Handle("Buffer", cInfo.buffer.get(0));
        }
        deviceObj.handleMap.put(this.handle, this);

        //
        vkGetBufferMemoryRequirements2(
                deviceObj.device,
                VkBufferMemoryRequirementsInfo2.calloc()
                        .pNext(0L)
                        .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_REQUIREMENTS_INFO_2)
                        .buffer(this.handle.get()),
                this.memoryRequirements2 = VkMemoryRequirements2.calloc()
                        .sType(VK_STRUCTURE_TYPE_MEMORY_REQUIREMENTS_2)
                        .pNext(0L)
        );

        //
        if (cInfo.memoryAllocator != 0) {
            cInfo.memoryAllocationInfo.memoryRequirements2 = this.memoryRequirements2;
            cInfo.memoryAllocationInfo.memoryRequirements = cInfo.memoryAllocationInfo.memoryRequirements2.memoryRequirements();
            cInfo.memoryAllocationInfo.buffer = this.handle.ptr();

            //
            var memoryAllocatorObj = (MemoryAllocatorObj) BasicObj.globalHandleMap.get(cInfo.memoryAllocator);
            var memoryAllocationObj = new MemoryAllocationObj(this.base, cInfo.memoryAllocationInfo);
            memoryAllocatorObj.allocateMemory(cInfo.memoryAllocationInfo, memoryAllocationObj);

            //
            this.allocationHandle = memoryAllocationObj.getHandle().get();
        }
    }



    //
    public ByteBuffer map(long byteLength, long byteOffset) {
        var deviceObj = (DeviceObj) BasicObj.globalHandleMap.get(this.base.get());
        var physicalDeviceObj = (PhysicalDeviceObj) BasicObj.globalHandleMap.get(deviceObj.base.get());
        var allocationObj = (MemoryAllocationObj) deviceObj.handleMap.get(new Handle("MemoryAllocation", this.allocationHandle));
        return allocationObj.map(byteLength, byteOffset);
    }

    //
    public void unmap() {
        var deviceObj = (DeviceObj) BasicObj.globalHandleMap.get(this.base.get());
        var physicalDeviceObj = (PhysicalDeviceObj) BasicObj.globalHandleMap.get(deviceObj.base.get());
        var allocationObj = (MemoryAllocationObj) deviceObj.handleMap.get(new Handle("MemoryAllocation", this.allocationHandle));
        allocationObj.unmap();
    }



    //
    public long getDeviceAddress() {
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get());
        if (this.deviceAddress == 0) {
            this.deviceAddress = vkGetBufferDeviceAddress(deviceObj.device, VkBufferDeviceAddressInfo.calloc().pNext(0L).sType(VK_STRUCTURE_TYPE_BUFFER_DEVICE_ADDRESS_INFO).buffer(this.handle.get()));
            deviceObj.addressMap.add(new LongInterval(this.deviceAddress, this.deviceAddress + this.createInfo.size(), Interval.Bounded.CLOSED));
            deviceObj.rootMap.put(this.deviceAddress, this.handle.get());
        }
        return this.deviceAddress;
    }

    //
    public BufferObj flushMapped() {
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get());
        var allocationObj = (MemoryAllocationObj) deviceObj.handleMap.get(new Handle("MemoryAllocation", this.allocationHandle));

        allocationObj.flushMapped();

        return this;
    }

    //
    public BufferObj invalidateMapped() {
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get());
        var allocationObj = (MemoryAllocationObj) deviceObj.handleMap.get(new Handle("MemoryAllocation", this.allocationHandle));

        allocationObj.invalidateMapped();

        return this;
    }

    // for `map()` or copy operations
    // necessary after `unmap()` op
    // Resizable BAR!
    public BufferObj cmdSynchronizeFromHost(VkCommandBuffer cmdBuf) {
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get());
        var allocationObj = (MemoryAllocationObj) deviceObj.handleMap.get(new Handle("MemoryAllocation", this.allocationHandle));

        CopyUtilObj.cmdSynchronizeFromHost(cmdBuf, VkDescriptorBufferInfo.calloc().set(this.handle.get(), 0, VK_WHOLE_SIZE));
        return this;
    }

    // unrecommended if you have ResizableBAR support
    // mostly, usable for uniform data and buffers
    // also, support partial synchronization
    // bounds updating data into command buffer
    public BufferObj cmdUpdateBuffer(VkCommandBuffer cmdBuf, ByteBuffer data, long byteOffset) {
        vkCmdUpdateBuffer(cmdBuf, this.handle.get(), byteOffset, data);
        CopyUtilObj.cmdSynchronizeFromHost(cmdBuf, VkDescriptorBufferInfo.calloc().set(this.handle.get(), byteOffset, data.remaining()));
        return this;
    }

    @Override // TODO: multiple queue family support (and Promise.all)
    public BufferObj delete() {
        var handle = this.handle;
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get());
        deviceObj.submitOnce(deviceObj.getCommandPool(((SwapChainCInfo)cInfo).queueFamilyIndex), new BasicCInfo.SubmitCmd(){{
            queue = deviceObj.getQueue(((SwapChainCInfo)cInfo).queueFamilyIndex, 0);
            onDone = new Promise<>().thenApply((result)->{
                vkDestroyBuffer(deviceObj.device, handle.get(), null);
                deviceObj.handleMap.remove(handle);
                return null;
            });
        }}, (cmdBuf)->{
            return VK_SUCCESS;
        });

        // TODO: Use Shared PTR (alike C++)
        var allocationObj = (MemoryAllocationObj) deviceObj.handleMap.get(new Handle("MemoryAllocation", this.allocationHandle));
        allocationObj.delete();
        return this;
    }

    //
    public VkDescriptorBufferInfo getBufferRange() {
        return VkDescriptorBufferInfo.calloc().set(this.handle.get(), 0, this.createInfo.size());
    }
}