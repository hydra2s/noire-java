package org.hydra2s.noire.objects;

import com.lodborg.intervaltree.Interval;
import com.lodborg.intervaltree.LongInterval;
import org.hydra2s.noire.descriptors.BufferCInfo;
import org.hydra2s.utils.Promise;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;

import static org.hydra2s.noire.descriptors.UtilsCInfo.vkCheckStatus;
import static org.lwjgl.system.MemoryUtil.memAddress;
import static org.lwjgl.system.MemoryUtil.memLongBuffer;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK11.*;
import static org.lwjgl.vulkan.VK12.*;

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
    public MemoryAllocationObj allocationObj = null;

    //
    public BufferObj(Handle base, Handle handle) {
        super(base, handle);
    }

    // TODO: create buffer by allocator (such as VMA)
    public BufferObj(Handle base, BufferCInfo cInfo) {
        super(base, cInfo);

        
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
            .sharingMode(VK_SHARING_MODE_CONCURRENT)
            .pQueueFamilyIndices(deviceObj.queueFamilyIndices);

        //
        int status = VK_NOT_READY;
        if (cInfo.buffer == null || cInfo.buffer.get(0) == 0) {
            status = vkCreateBuffer(deviceObj.device, this.createInfo, null, memLongBuffer(memAddress((this.handle = new Handle("Buffer")).ptr(), 0), 1));
        } else {
            status = VK_SUCCESS;
            this.handle = new Handle("Buffer", cInfo.buffer.get(0));
        }
        vkCheckStatus(status);
        deviceObj.handleMap.put$(this.handle, this);

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
            var memoryAllocatorObj = (MemoryAllocatorObj) BasicObj.globalHandleMap.get(cInfo.memoryAllocator).orElse(null);
            allocationObj = new MemoryAllocationObj(this.base, cInfo.memoryAllocationInfo);
            memoryAllocatorObj.allocateMemory(cInfo.memoryAllocationInfo, allocationObj);

            //
            this.allocationHandle = allocationObj.getHandle().get();
        }
    }



    //
    public ByteBuffer map() {
        return allocationObj.map(this.createInfo.size(), 0);
    }

    //
    public ByteBuffer map(long byteLength, long byteOffset) {
        return allocationObj.map(byteLength, byteOffset);
    }

    //
    public void unmap() {
        allocationObj.unmap();
    }



    //
    public long getDeviceAddress() {
        if (this.deviceAddress == 0) {
            
            this.deviceAddress = vkGetBufferDeviceAddress(deviceObj.device, VkBufferDeviceAddressInfo.calloc().pNext(0L).sType(VK_STRUCTURE_TYPE_BUFFER_DEVICE_ADDRESS_INFO).buffer(this.handle.get()));
            deviceObj.addressMap.add(new LongInterval(this.deviceAddress, this.deviceAddress + this.createInfo.size(), Interval.Bounded.CLOSED));
            deviceObj.rootMap.put$(this.deviceAddress, this.handle.get());
        }
        return this.deviceAddress;
    }

    //
    public BufferObj flushMapped() {
        allocationObj.flushMapped();

        return this;
    }

    //
    public BufferObj invalidateMapped() {
        allocationObj.invalidateMapped();
        return this;
    }

    // for `map()` or copy operations
    // necessary after `unmap()` op
    // Resizable BAR!
    public BufferObj cmdSynchronizeFromHost(VkCommandBuffer cmdBuf) {
        CommandUtils.cmdSynchronizeFromHost(cmdBuf, VkDescriptorBufferInfo.calloc().set(this.handle.get(), 0, VK_WHOLE_SIZE));
        return this;
    }

    /*@Override // TODO: multiple queue family support (and Promise.all)
    public BufferObj delete() throws Exception {
        var handle = this.handle;

        deviceObj.submitOnce(new DeviceObj.SubmitCmd(){{
            queueGroupIndex = cInfo.queueGroupIndex;
            onDone = new Promise<>().thenApply((result)->{
                vkDestroyBuffer(deviceObj.device, handle.get(), null);
                deviceObj.handleMap.remove(handle);

                // TODO: Use Shared PTR (alike C++)
                allocationObj.deleteDirectly();

                //
                return null;
            });
        }}, (cmdBuf)->{
            return cmdBuf;
        });


        return this;
    }*/

    @Override // TODO: multiple queue family support (and Promise.all)
    public BufferObj deleteDirectly() {
        var handle = this.handle;

        //
        vkDestroyBuffer(deviceObj.device, handle.get(), null);
        deviceObj.handleMap.remove(handle);

        // TODO: Use Shared PTR (alike C++)
        allocationObj.deleteDirectly();

        return this;
    }

    //
    public VkDescriptorBufferInfo getBufferRange() {
        return VkDescriptorBufferInfo.calloc().set(this.handle.get(), 0, this.createInfo.size());
    }
}