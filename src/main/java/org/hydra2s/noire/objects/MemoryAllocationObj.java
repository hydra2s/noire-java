package org.hydra2s.noire.objects;

//

import org.hydra2s.noire.descriptors.MemoryAllocationCInfo;
import org.hydra2s.noire.descriptors.UtilsCInfo;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

import static org.lwjgl.BufferUtils.createLongBuffer;

// currently, is a part of device memory object
// TODO: planned indirect copy support (for NVIDIA)
public class MemoryAllocationObj extends BasicObj {
    public Object allocationInfo = null;
    public long allocationInfoAddress = 0L;

    // important for VMA or other allocators
    public long memoryOffset = 0L;
    public long memorySize = 0L;

    //
    public long[] deviceMemory = {};
    public MemoryAllocatorObj.DeviceMemoryObj deviceMemoryObj = null;

    //
    public MemoryAllocationObj(UtilsCInfo.Handle base, UtilsCInfo.Handle handle) {
        super(base, handle);

        //
        //deviceMemoryObj = (MemoryAllocatorObj.DeviceMemoryObj)deviceObj.handleMap.get(new Handle("DeviceMemory", this.deviceMemory)).orElse(null);

        //
        this.handle = new UtilsCInfo.Handle("MemoryAllocation", MemoryUtil.memAddress(createLongBuffer(1)));
        deviceObj.handleMap.put$(this.handle, this);
    }

    //
    public MemoryAllocationObj(UtilsCInfo.Handle base, MemoryAllocationCInfo cInfo) {
        super(base, cInfo);

        //
        //deviceMemoryObj = (MemoryAllocatorObj.DeviceMemoryObj)deviceObj.handleMap.get(new Handle("DeviceMemory", this.deviceMemory)).orElse(null);

        //
        this.handle = new UtilsCInfo.Handle("MemoryAllocation", MemoryUtil.memAddress(createLongBuffer(1)));
        deviceObj.handleMap.put$(this.handle, this);
    }

    //
    public ByteBuffer map(long byteLength, long byteOffset) {
        // TODO: add support for synchronize to host
        return deviceMemoryObj.map(byteLength, this.memoryOffset + byteOffset);
    }

    public void unmap() {
        //
        deviceMemoryObj.unmap();
    }

    public MemoryAllocationObj flushMapped() {
        deviceMemoryObj.flushMapped(this.memorySize, this.memoryOffset);
        return this;
    }

    public MemoryAllocationObj invalidateMapped() {
        deviceMemoryObj.invalidateMapped(this.memorySize, this.memoryOffset);
        return this;
    }

    public PointerBuffer getWin32Handle() {
        return deviceMemoryObj.Win32Handle;
    }

    public int[] getFdHandle() {
        return deviceMemoryObj.FdHandle;
    }



    /*@Override // TODO: multiple queue family support
    public MemoryAllocationObj delete() throws Exception {
        var handle = this.handle;
        

        // TODO: Use Shared PTR (alike C++)
        deviceMemoryObj.delete();
        return this;
    }*/

    @Override // TODO: multiple queue family support
    public MemoryAllocationObj deleteDirectly() {
        var handle = this.handle;
        

        // TODO: Use Shared PTR (alike C++)
        deviceMemoryObj.deleteDirectly();
        return this;
    }

}
