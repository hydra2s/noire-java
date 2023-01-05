package org.hydra2s.noire.objects;

import com.lodborg.intervaltree.Interval;
import com.lodborg.intervaltree.LongInterval;
import org.hydra2s.noire.descriptors.BufferCInfo;
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

    // for `map()` or copy operations
    // necessary after `unmap()` op
    // Resizable BAR!
    public BufferObj cmdSynchronizeFromHost(VkCommandBuffer cmdBuf) {
        // for `map()` or copy operations
        var bufferMemoryBarrier = VkBufferMemoryBarrier2.calloc(1)
                .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER_2)
                .srcStageMask(VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT | VK_PIPELINE_STAGE_2_HOST_BIT)
                .srcAccessMask(VK_ACCESS_2_TRANSFER_READ_BIT | VK_ACCESS_2_TRANSFER_WRITE_BIT | VK_ACCESS_2_HOST_READ_BIT | VK_ACCESS_2_HOST_WRITE_BIT)
                .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
                .dstAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .buffer(this.handle.get())
                .offset(0L)
                .size(VK_WHOLE_SIZE); // TODO: support partial synchronization

        //
        vkCmdPipelineBarrier2(cmdBuf, VkDependencyInfoKHR.calloc().sType(VK_STRUCTURE_TYPE_DEPENDENCY_INFO).pBufferMemoryBarriers(bufferMemoryBarrier));

        return this;
    }

    // unrecommended if you have ResizableBAR support
    // mostly, usable for uniform data and buffers
    // also, support partial synchronization
    public BufferObj cmdUpdateBuffer(VkCommandBuffer cmdBuf, ByteBuffer data, long byteOffset) {

        var bufferMemoryBarrier = VkBufferMemoryBarrier2.calloc(1)
                .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER_2)
                .srcStageMask(VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT | VK_PIPELINE_STAGE_2_HOST_BIT)
                .srcAccessMask(VK_ACCESS_2_TRANSFER_READ_BIT | VK_ACCESS_2_TRANSFER_WRITE_BIT | VK_ACCESS_2_HOST_READ_BIT | VK_ACCESS_2_HOST_WRITE_BIT)
                .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
                .dstAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .buffer(this.handle.get())
                .offset(byteOffset)
                .size(data.remaining());

        //
        vkCmdUpdateBuffer(cmdBuf, this.handle.get(), byteOffset, data);
        vkCmdPipelineBarrier2(cmdBuf, VkDependencyInfoKHR.calloc().sType(VK_STRUCTURE_TYPE_DEPENDENCY_INFO).pBufferMemoryBarriers(bufferMemoryBarrier));
        return this;
    }
}