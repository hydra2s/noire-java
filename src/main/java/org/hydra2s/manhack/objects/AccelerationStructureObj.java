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
import static org.lwjgl.vulkan.KHRSynchronization2.*;
import static org.lwjgl.vulkan.VK12.VK_STRUCTURE_TYPE_BUFFER_DEVICE_ADDRESS_INFO;
import static org.lwjgl.vulkan.VK12.vkGetBufferDeviceAddress;
import static org.lwjgl.vulkan.VK13.*;
import static org.lwjgl.vulkan.VK13.VK_STRUCTURE_TYPE_DEPENDENCY_INFO;

// TODO: deferred allocation support
public class AccelerationStructureObj extends BasicObj {
    public VkAccelerationStructureBuildSizesInfoKHR buildSizeInfo = null;
    public VkAccelerationStructureGeometryDataKHR.Buffer geometryData = null;
    public VkAccelerationStructureGeometryKHR.Buffer geometryInfo = null;
    public VkAccelerationStructureBuildGeometryInfoKHR.Buffer geometryBuildInfo = null;
    public int ASLevel = VK_ACCELERATION_STRUCTURE_TYPE_TOP_LEVEL_KHR;
    public IntBuffer primitiveCount = null;
    public MemoryAllocationObj.BufferObj ASStorageBuffer = null;
    public VkBufferMemoryBarrier2 ASStorageBarrier = null;
    public MemoryAllocationObj.BufferObj ASScratchBuffer = null;
    public VkBufferMemoryBarrier2 ASScratchBarrier = null;
    public long deviceAddress = 0L;

    // TODO: deferred allocation support
    public AccelerationStructureObj(Handle base, Handle handle) {
        super(base, handle);
    }

    // TODO: deferred allocation support
    public AccelerationStructureObj(Handle base, AccelerationStructureCInfo cInfo) {
        super(base, cInfo);

        // TODO: auto-detect AS level
        // this.ASLevel = ...

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
        this.geometryBuildInfo = VkAccelerationStructureBuildGeometryInfoKHR.create(1).sType(VK_STRUCTURE_TYPE_ACCELERATION_STRUCTURE_BUILD_GEOMETRY_INFO_KHR)
            .type(this.ASLevel)
            .mode(VK_BUILD_ACCELERATION_STRUCTURE_MODE_BUILD_KHR)
            .flags(VK_BUILD_ACCELERATION_STRUCTURE_PREFER_FAST_TRACE_BIT_KHR)
            .geometryCount(this.geometryInfo.capacity())
            .pGeometries(this.geometryInfo);

        //
        this.buildSizeInfo = VkAccelerationStructureBuildSizesInfoKHR.create().sType(VK_STRUCTURE_TYPE_ACCELERATION_STRUCTURE_BUILD_SIZES_INFO_KHR);
        vkGetAccelerationStructureBuildSizesKHR(deviceObj.device, VK_ACCELERATION_STRUCTURE_BUILD_TYPE_DEVICE_KHR, this.geometryBuildInfo.get(1), this.primitiveCount, this.buildSizeInfo);

        //
        var allocationCInfo = new MemoryAllocationCInfo();
        allocationCInfo.isHost = false;
        allocationCInfo.isDevice = true;

        //
        var ASBufferCreateInfo = new MemoryAllocationCInfo.BufferCInfo();
        ASBufferCreateInfo.size = this.buildSizeInfo.accelerationStructureSize();
        ASBufferCreateInfo.usage = VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_STORAGE_BIT_KHR;
        this.ASStorageBuffer = (MemoryAllocationObj.BufferObj) memoryAllocatorObj.allocateMemory(allocationCInfo, new MemoryAllocationObj.BufferObj(this.base, ASBufferCreateInfo));
        this.ASStorageBarrier = VkBufferMemoryBarrier2.create()
            .srcStageMask(VK_PIPELINE_STAGE_2_ACCELERATION_STRUCTURE_BUILD_BIT_KHR)
            .srcAccessMask(VK_ACCESS_2_ACCELERATION_STRUCTURE_WRITE_BIT_KHR | VK_ACCESS_2_ACCELERATION_STRUCTURE_READ_BIT_KHR)
            .dstStageMask(VK_PIPELINE_STAGE_2_COMPUTE_SHADER_BIT | VK_PIPELINE_STAGE_2_ACCELERATION_STRUCTURE_BUILD_BIT_KHR)
            .dstAccessMask(VK_ACCESS_2_ACCELERATION_STRUCTURE_READ_BIT_KHR | VK_ACCESS_2_ACCELERATION_STRUCTURE_READ_BIT_KHR | VK_ACCESS_2_SHADER_READ_BIT)
            .srcQueueFamilyIndex(~0)
            .dstQueueFamilyIndex(~0)
            .buffer(this.ASStorageBuffer.handle.get())
            .offset(0)
            .size(ASBufferCreateInfo.size);

        //
        var ASScratchCreateInfo = new MemoryAllocationCInfo.BufferCInfo();
        ASScratchCreateInfo.size = Math.max(this.buildSizeInfo.buildScratchSize(), this.buildSizeInfo.updateScratchSize());
        ASScratchCreateInfo.usage = VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_STORAGE_BIT_KHR;
        this.ASScratchBuffer = (MemoryAllocationObj.BufferObj) memoryAllocatorObj.allocateMemory(allocationCInfo, new MemoryAllocationObj.BufferObj(this.base, ASScratchCreateInfo));
        this.ASScratchBarrier = VkBufferMemoryBarrier2.create()
            .srcStageMask(VK_PIPELINE_STAGE_2_ACCELERATION_STRUCTURE_BUILD_BIT_KHR)
            .srcAccessMask(VK_ACCESS_2_ACCELERATION_STRUCTURE_WRITE_BIT_KHR)
            .dstStageMask(VK_PIPELINE_STAGE_2_COMPUTE_SHADER_BIT | VK_PIPELINE_STAGE_2_ACCELERATION_STRUCTURE_BUILD_BIT_KHR)
            .dstAccessMask(VK_ACCESS_2_ACCELERATION_STRUCTURE_READ_BIT_KHR | VK_ACCESS_2_SHADER_READ_BIT | VK_ACCESS_2_ACCELERATION_STRUCTURE_READ_BIT_KHR)
            .srcQueueFamilyIndex(~0)
            .dstQueueFamilyIndex(~0)
            .buffer(this.ASScratchBuffer.handle.get())
            .offset(0)
            .size(ASScratchCreateInfo.size);

        //
        vkCreateAccelerationStructureKHR(deviceObj.device, VkAccelerationStructureCreateInfoKHR.create().sType(VK_STRUCTURE_TYPE_ACCELERATION_STRUCTURE_CREATE_INFO_KHR).type(this.ASLevel).size(ASBufferCreateInfo.size).offset(0).buffer(this.ASStorageBuffer.handle.get()), null, memLongBuffer(memAddress((this.handle = new Handle("AccelerationStructure")).ptr(), 0), 1));
        deviceObj.handleMap.put(this.handle, this);
        this.deviceAddress = this.deviceAddress == 0 ? (this.deviceAddress = vkGetAccelerationStructureDeviceAddressKHR(deviceObj.device, VkAccelerationStructureDeviceAddressInfoKHR.create().sType(VK_STRUCTURE_TYPE_ACCELERATION_STRUCTURE_DEVICE_ADDRESS_INFO_KHR).accelerationStructure(this.handle.get()))) : this.deviceAddress;
        this.geometryBuildInfo.dstAccelerationStructure(this.handle.get());
        this.geometryBuildInfo.scratchData(VkDeviceOrHostAddressKHR.create().deviceAddress(this.ASScratchBuffer.getDeviceAddress()));
    }

    //
    public long getDeviceAddress() {
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get());
        return this.deviceAddress == 0 ? (this.deviceAddress = vkGetAccelerationStructureDeviceAddressKHR(deviceObj.device, VkAccelerationStructureDeviceAddressInfoKHR.create().sType(VK_STRUCTURE_TYPE_ACCELERATION_STRUCTURE_DEVICE_ADDRESS_INFO_KHR).accelerationStructure(this.handle.get()))) : this.deviceAddress;
    }

    //
    public AccelerationStructureObj cmdBuild(VkCommandBuffer cmdBuf, VkAccelerationStructureBuildRangeInfoKHR.Buffer buildRanges, int mode) {
        this.geometryBuildInfo.mode(mode);
        this.geometryBuildInfo.srcAccelerationStructure(mode == VK_BUILD_ACCELERATION_STRUCTURE_MODE_UPDATE_KHR ? this.handle.get() : 0);

        //
        vkCmdBuildAccelerationStructuresKHR(cmdBuf, this.geometryBuildInfo, memAllocPointer(1).put(0, buildRanges.address()));
        vkCmdPipelineBarrier2(cmdBuf, VkDependencyInfoKHR.create().sType(VK_STRUCTURE_TYPE_DEPENDENCY_INFO).pBufferMemoryBarriers(VkBufferMemoryBarrier2.create(2)
            .put(0, this.ASStorageBarrier)
            .put(1, this.ASScratchBarrier)
        ));

        //
        return this;
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
