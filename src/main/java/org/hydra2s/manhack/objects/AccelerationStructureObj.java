package org.hydra2s.manhack.objects;

//
import org.hydra2s.manhack.descriptors.AccelerationStructureCInfo;
import org.hydra2s.manhack.descriptors.MemoryAllocationCInfo;
import org.lwjgl.vulkan.*;

//
import java.nio.IntBuffer;
import java.util.stream.IntStream;

//
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.EXTDescriptorBuffer.VK_BUFFER_USAGE_RESOURCE_DESCRIPTOR_BUFFER_BIT_EXT;
import static org.lwjgl.vulkan.KHRAccelerationStructure.*;
import static org.lwjgl.vulkan.VK12.VK_STRUCTURE_TYPE_BUFFER_DEVICE_ADDRESS_INFO;
import static org.lwjgl.vulkan.VK12.vkGetBufferDeviceAddress;

// TODO: deferred allocation support
public class AccelerationStructureObj extends BasicObj {
    public VkAccelerationStructureBuildSizesInfoKHR buildSizeInfo = null;
    public VkAccelerationStructureGeometryDataKHR.Buffer geometryData = null;
    public VkAccelerationStructureGeometryKHR.Buffer geometryInfo = null;
    public VkAccelerationStructureBuildGeometryInfoKHR geometryBuildInfo = null;
    public int ASLevel = VK_ACCELERATION_STRUCTURE_TYPE_TOP_LEVEL_KHR;
    public IntBuffer primitiveCount = null;
    public MemoryAllocationObj.BufferObj ASStorageBuffer = null;
    public MemoryAllocationObj.BufferObj ASScratchBuffer = null;
    public long deviceAddress = 0L;

    // TODO: deferred allocation support
    public AccelerationStructureObj(Handle base, Handle handle) {
        super(base, handle);
    }

    // TODO: deferred allocation support
    public AccelerationStructureObj(Handle base, AccelerationStructureCInfo cInfo) {
        super(base, cInfo);

        //
        var deviceObj = (DeviceObj) BasicObj.globalHandleMap.get(this.base.get());
        var physicalDeviceObj = (PhysicalDeviceObj) BasicObj.globalHandleMap.get(deviceObj.base.get());
        var memoryAllocatorObj = (MemoryAllocatorObj) BasicObj.globalHandleMap.get(cInfo.memoryAllocator);

        //
        this.geometryInfo = VkAccelerationStructureGeometryKHR.create(this.geometryData.capacity())
            .sType(VK_STRUCTURE_TYPE_ACCELERATION_STRUCTURE_GEOMETRY_KHR)
            .geometryType(this.ASLevel == VK_ACCELERATION_STRUCTURE_TYPE_TOP_LEVEL_KHR ? VK_GEOMETRY_TYPE_INSTANCES_KHR : VK_GEOMETRY_TYPE_TRIANGLES_KHR);

        //
        this.geometryData = VkAccelerationStructureGeometryDataKHR.create(this.ASLevel == VK_ACCELERATION_STRUCTURE_TYPE_TOP_LEVEL_KHR ? 1 : cInfo.geometries.size());
        this.primitiveCount = memAllocInt(this.geometryData.capacity());

        //
        if (this.ASLevel == VK_ACCELERATION_STRUCTURE_TYPE_TOP_LEVEL_KHR)
        {
            this.geometryData.instances(VkAccelerationStructureGeometryInstancesDataKHR.create()
                .sType(VK_STRUCTURE_TYPE_ACCELERATION_STRUCTURE_GEOMETRY_INSTANCES_DATA_KHR)
                .arrayOfPointers(false)
                .data(VkDeviceOrHostAddressConstKHR.create().deviceAddress(cInfo.instances.instanceBinding.address))
            );
            this.geometryInfo.geometry(this.geometryData.get(0));
            this.geometryInfo.flags((cInfo.instances.opaque ? VK_GEOMETRY_OPAQUE_BIT_KHR : 0) | VK_GEOMETRY_NO_DUPLICATE_ANY_HIT_INVOCATION_BIT_KHR);
        } else
        {
            IntStream.range(0, cInfo.geometries.size()).map((I)->{
                var triangles = VkAccelerationStructureGeometryTrianglesDataKHR.create().sType(VK_STRUCTURE_TYPE_ACCELERATION_STRUCTURE_GEOMETRY_TRIANGLES_DATA_KHR).indexType(VK_INDEX_TYPE_NONE_KHR);
                var geometryI = cInfo.geometries.get(I);
                if (geometryI.vertexBinding != null) {
                    triangles
                        .vertexFormat(geometryI.vertexBinding.format)
                        .vertexStride(geometryI.vertexBinding.stride)
                        .vertexData(VkDeviceOrHostAddressConstKHR.create().deviceAddress(geometryI.vertexBinding.address))
                        .maxVertex(geometryI.vertexBinding.vertexCount);
                }
                if (geometryI.indexBinding != null) {
                    triangles
                        .indexData(VkDeviceOrHostAddressConstKHR.create().deviceAddress(geometryI.indexBinding.address))
                        .indexType(geometryI.indexBinding.type)
                        .maxVertex(geometryI.indexBinding.vertexCount);
                }

                //
                this.primitiveCount.put(I, triangles.maxVertex()/3);
                this.geometryData.get(I).triangles(triangles);
                this.geometryInfo.get(I).geometry(this.geometryData.get(I));
                this.geometryInfo.get(I).flags((geometryI.opaque ? VK_GEOMETRY_OPAQUE_BIT_KHR : 0) | VK_GEOMETRY_NO_DUPLICATE_ANY_HIT_INVOCATION_BIT_KHR);

                //
                return I;
            });
        }

        //
        this.geometryBuildInfo = VkAccelerationStructureBuildGeometryInfoKHR.create().sType(VK_STRUCTURE_TYPE_ACCELERATION_STRUCTURE_BUILD_GEOMETRY_INFO_KHR)
            .type(this.ASLevel)
            .flags(VK_BUILD_ACCELERATION_STRUCTURE_PREFER_FAST_TRACE_BIT_KHR)
            .geometryCount(this.geometryInfo.capacity())
            .pGeometries(this.geometryInfo);

        //
        this.buildSizeInfo = VkAccelerationStructureBuildSizesInfoKHR.create().sType(VK_STRUCTURE_TYPE_ACCELERATION_STRUCTURE_BUILD_SIZES_INFO_KHR);
        vkGetAccelerationStructureBuildSizesKHR(deviceObj.device, VK_ACCELERATION_STRUCTURE_BUILD_TYPE_DEVICE_KHR, this.geometryBuildInfo, this.primitiveCount, this.buildSizeInfo);

        //
        var allocationCInfo = new MemoryAllocationCInfo();
        allocationCInfo.isHost = false;
        allocationCInfo.isDevice = true;

        //
        var ASBufferCreateInfo = new MemoryAllocationCInfo.BufferCInfo();
        ASBufferCreateInfo.size = this.buildSizeInfo.accelerationStructureSize();
        ASBufferCreateInfo.usage = VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_STORAGE_BIT_KHR;
        this.ASStorageBuffer = (MemoryAllocationObj.BufferObj) memoryAllocatorObj.allocateMemory(allocationCInfo, new MemoryAllocationObj.BufferObj(this.base, ASBufferCreateInfo));

        //
        var ASScratchCreateInfo = new MemoryAllocationCInfo.BufferCInfo();
        ASScratchCreateInfo.size = Math.max(this.buildSizeInfo.buildScratchSize(), this.buildSizeInfo.updateScratchSize());
        ASScratchCreateInfo.usage = VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_STORAGE_BIT_KHR;
        this.ASScratchBuffer = (MemoryAllocationObj.BufferObj) memoryAllocatorObj.allocateMemory(allocationCInfo, new MemoryAllocationObj.BufferObj(this.base, ASScratchCreateInfo));

        //
        vkCreateAccelerationStructureKHR(deviceObj.device, VkAccelerationStructureCreateInfoKHR.create().sType(VK_STRUCTURE_TYPE_ACCELERATION_STRUCTURE_CREATE_INFO_KHR).type(this.ASLevel).size(ASBufferCreateInfo.size).offset(0).buffer(this.ASStorageBuffer.handle.get()), null, memLongBuffer(memAddress((this.handle = new Handle("AccelerationStructure")).ptr(), 0), 1));
        deviceObj.handleMap.put(this.handle, this);
        this.deviceAddress = this.deviceAddress == 0 ? (this.deviceAddress = vkGetAccelerationStructureDeviceAddressKHR(deviceObj.device, VkAccelerationStructureDeviceAddressInfoKHR.create().sType(VK_STRUCTURE_TYPE_ACCELERATION_STRUCTURE_DEVICE_ADDRESS_INFO_KHR).accelerationStructure(this.handle.get()))) : this.deviceAddress;
    }

    //
    public long getDeviceAddress() {
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get());
        return this.deviceAddress == 0 ? (this.deviceAddress = vkGetAccelerationStructureDeviceAddressKHR(deviceObj.device, VkAccelerationStructureDeviceAddressInfoKHR.create().sType(VK_STRUCTURE_TYPE_ACCELERATION_STRUCTURE_DEVICE_ADDRESS_INFO_KHR).accelerationStructure(this.handle.get()))) : this.deviceAddress;
    }

    //
    static public class TopAccelerationStructureObj extends AccelerationStructureObj {
        public int ASLevel = VK_ACCELERATION_STRUCTURE_TYPE_TOP_LEVEL_KHR;

        public TopAccelerationStructureObj(Handle base, Handle handle) {
            super(base, handle);
        }

        public TopAccelerationStructureObj(Handle base, AccelerationStructureCInfo.TopAccelerationStructureCInfo cInfo) {
            super(base, cInfo);
        }
    }

    //
    static public class BottomAccelerationStructureObj extends AccelerationStructureObj {
        public int ASLevel = VK_ACCELERATION_STRUCTURE_TYPE_BOTTOM_LEVEL_KHR;

        public BottomAccelerationStructureObj(Handle base, Handle handle) {
            super(base, handle);
        }

        public BottomAccelerationStructureObj(Handle base, AccelerationStructureCInfo.BottomAccelerationStructureCInfo cInfo) {
            super(base, cInfo);
        }

    }
}
