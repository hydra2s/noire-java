package org.hydra2s.noire.objects;

//

import com.lodborg.intervaltree.Interval;
import com.lodborg.intervaltree.LongInterval;
import org.hydra2s.noire.descriptors.AccelerationStructureCInfo;
import org.hydra2s.noire.descriptors.BufferCInfo;
import org.hydra2s.noire.descriptors.MemoryAllocationCInfo;
import org.hydra2s.noire.descriptors.UtilsCInfo;
import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.util.stream.IntStream;

import static org.hydra2s.noire.descriptors.UtilsCInfo.vkCheckStatus;
import static org.lwjgl.BufferUtils.createPointerBuffer;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.KHRAccelerationStructure.*;
import static org.lwjgl.vulkan.KHRSynchronization2.*;
import static org.lwjgl.vulkan.VK13.*;

// TODO: deferred allocation support
public class AccelerationStructureObj extends BasicObj {
    public VkAccelerationStructureBuildSizesInfoKHR buildSizeInfo = null;
    public VkAccelerationStructureGeometryDataKHR.Buffer geometryData = null;
    public VkAccelerationStructureGeometryKHR.Buffer geometryInfo = null;
    public VkAccelerationStructureBuildGeometryInfoKHR.Buffer geometryBuildInfo = null;
    public int ASLevel = VK_ACCELERATION_STRUCTURE_TYPE_TOP_LEVEL_KHR;
    public int[] primitiveCount = null;
    public BufferObj ASStorageBuffer = null;
    public VkBufferMemoryBarrier2 ASStorageBarrier = null;
    public BufferObj ASScratchBuffer = null;
    public VkBufferMemoryBarrier2 ASScratchBarrier = null;
    public long deviceAddress = 0L;

    // TODO: deferred allocation support
    public AccelerationStructureObj(UtilsCInfo.Handle base, UtilsCInfo.Handle handle) {
        super(base, handle);
    }

    // TODO: deferred allocation support
    public AccelerationStructureObj(UtilsCInfo.Handle base, AccelerationStructureCInfo cInfo) {
        super(base, cInfo);

        // TODO: auto-detect AS level
        this.ASLevel = cInfo.instances != null ? VK_ACCELERATION_STRUCTURE_TYPE_TOP_LEVEL_KHR : VK_ACCELERATION_STRUCTURE_TYPE_BOTTOM_LEVEL_KHR;


        //
        this.geometryBuildInfo = VkAccelerationStructureBuildGeometryInfoKHR.create(1)
            .sType(VK_STRUCTURE_TYPE_ACCELERATION_STRUCTURE_BUILD_GEOMETRY_INFO_KHR)
            .pNext(0L)
            .type(this.ASLevel)
            .mode(VK_BUILD_ACCELERATION_STRUCTURE_MODE_BUILD_KHR)
            .flags(VK_BUILD_ACCELERATION_STRUCTURE_PREFER_FAST_TRACE_BIT_KHR)
            .ppGeometries(null);

        //
        this.recallGeometryInfo();

        //
        this.buildSizeInfo = VkAccelerationStructureBuildSizesInfoKHR.create().sType(VK_STRUCTURE_TYPE_ACCELERATION_STRUCTURE_BUILD_SIZES_INFO_KHR);
        vkGetAccelerationStructureBuildSizesKHR(deviceObj.device, VK_ACCELERATION_STRUCTURE_BUILD_TYPE_DEVICE_KHR, this.geometryBuildInfo.get(0), this.primitiveCount, this.buildSizeInfo);

        //
        this.ASStorageBuffer = new BufferObj(this.base, new BufferCInfo() {{
            memoryAllocator = cInfo.memoryAllocator;
            size = buildSizeInfo.accelerationStructureSize();
            usage = VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_STORAGE_BIT_KHR;
            memoryAllocationInfo = new MemoryAllocationCInfo(){{
                isHost = false;
                isDevice = true;
            }};
        }});
        this.ASStorageBarrier = VkBufferMemoryBarrier2.create()
            .pNext(0L)
            .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER_2)
            .srcStageMask(VK_PIPELINE_STAGE_2_ACCELERATION_STRUCTURE_BUILD_BIT_KHR)
            .srcAccessMask(VK_ACCESS_2_ACCELERATION_STRUCTURE_WRITE_BIT_KHR | VK_ACCESS_2_ACCELERATION_STRUCTURE_READ_BIT_KHR)
            .dstStageMask(VK_PIPELINE_STAGE_2_COMPUTE_SHADER_BIT | VK_PIPELINE_STAGE_2_ALL_GRAPHICS_BIT | VK_PIPELINE_STAGE_2_ACCELERATION_STRUCTURE_BUILD_BIT_KHR)
            .dstAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT | VK_ACCESS_2_ACCELERATION_STRUCTURE_READ_BIT_KHR | VK_ACCESS_2_ACCELERATION_STRUCTURE_READ_BIT_KHR | VK_ACCESS_2_SHADER_READ_BIT)
            .srcQueueFamilyIndex(~0)
            .dstQueueFamilyIndex(~0)
            .buffer(this.ASStorageBuffer.handle.get())
            .offset(0)
            .size(this.buildSizeInfo.accelerationStructureSize());

        //
        var scratchSize = Math.max(this.buildSizeInfo.buildScratchSize(), this.buildSizeInfo.updateScratchSize());
        this.ASScratchBuffer = new BufferObj(this.base, new BufferCInfo(){{
            memoryAllocator = cInfo.memoryAllocator;
            size = scratchSize;
            usage = VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_STORAGE_BIT_KHR | VK_BUFFER_USAGE_STORAGE_BUFFER_BIT;
            memoryAllocationInfo = new MemoryAllocationCInfo(){{
                isHost = false;
                isDevice = true;
            }};
        }});
        this.ASScratchBarrier = VkBufferMemoryBarrier2.create()
            .pNext(0L)
            .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER_2)
            .srcStageMask(VK_PIPELINE_STAGE_2_ACCELERATION_STRUCTURE_BUILD_BIT_KHR)
            .srcAccessMask(VK_ACCESS_2_ACCELERATION_STRUCTURE_WRITE_BIT_KHR | VK_ACCESS_2_ACCELERATION_STRUCTURE_READ_BIT_KHR)
            .dstStageMask(VK_PIPELINE_STAGE_2_COMPUTE_SHADER_BIT | VK_PIPELINE_STAGE_2_ALL_GRAPHICS_BIT | VK_PIPELINE_STAGE_2_ACCELERATION_STRUCTURE_BUILD_BIT_KHR)
            .dstAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT | VK_ACCESS_2_MEMORY_READ_BIT | VK_ACCESS_2_ACCELERATION_STRUCTURE_READ_BIT_KHR | VK_ACCESS_2_SHADER_READ_BIT | VK_ACCESS_2_ACCELERATION_STRUCTURE_READ_BIT_KHR)
            .srcQueueFamilyIndex(~0)
            .dstQueueFamilyIndex(~0)
            .buffer(this.ASScratchBuffer.handle.get())
            .offset(0)
            .size(scratchSize);

        //
        vkCheckStatus(vkCreateAccelerationStructureKHR(deviceObj.device, VkAccelerationStructureCreateInfoKHR.create().sType(VK_STRUCTURE_TYPE_ACCELERATION_STRUCTURE_CREATE_INFO_KHR).type(this.ASLevel).size(this.buildSizeInfo.accelerationStructureSize()).offset(0).buffer(this.ASStorageBuffer.handle.get()), null, (this.handle = new UtilsCInfo.Handle("AccelerationStructure")).ptr()));
        deviceObj.handleMap.put$(this.handle, this);

        //
        this.deviceAddress = this.getDeviceAddress();
        this.geometryBuildInfo.dstAccelerationStructure(this.handle.get());
        this.geometryBuildInfo.scratchData(VkDeviceOrHostAddressKHR.create().deviceAddress(this.ASScratchBuffer.getDeviceAddress()));
    }

    //
    public AccelerationStructureObj recallGeometryInfo() {
        var cInfo = (AccelerationStructureCInfo)this.cInfo;

        //
        this.geometryData = VkAccelerationStructureGeometryDataKHR.create(this.ASLevel == VK_ACCELERATION_STRUCTURE_TYPE_TOP_LEVEL_KHR ? 1 : cInfo.geometries.size());
        this.primitiveCount = new int[this.geometryData.remaining()];
        this.geometryInfo = VkAccelerationStructureGeometryKHR.create(this.geometryData.remaining())
            .sType(VK_STRUCTURE_TYPE_ACCELERATION_STRUCTURE_GEOMETRY_KHR)
            .geometryType(this.ASLevel == VK_ACCELERATION_STRUCTURE_TYPE_TOP_LEVEL_KHR ? VK_GEOMETRY_TYPE_INSTANCES_KHR : VK_GEOMETRY_TYPE_TRIANGLES_KHR);

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
            this.primitiveCount[0] = cInfo.instances.instanceBinding.vertexCount;
        } else
        {
            IntStream.range(0, cInfo.geometries.size()).forEachOrdered((I)->{
                var geometryI = cInfo.geometries.get(I);
                var triangles = VkAccelerationStructureGeometryTrianglesDataKHR.create()
                        .sType(VK_STRUCTURE_TYPE_ACCELERATION_STRUCTURE_GEOMETRY_TRIANGLES_DATA_KHR)
                        .indexType(VK_INDEX_TYPE_NONE_KHR)
                        .transformData(VkDeviceOrHostAddressConstKHR.create().deviceAddress(geometryI.transformAddress))
                        .indexData(VkDeviceOrHostAddressConstKHR.create().deviceAddress(0L));

                if (geometryI.vertexBinding != null) {
                    triangles
                        .vertexData(VkDeviceOrHostAddressConstKHR.create().deviceAddress(geometryI.vertexBinding.address))
                        .vertexFormat(geometryI.vertexBinding.format)
                        .vertexStride(geometryI.vertexBinding.stride)
                        .maxVertex(geometryI.vertexBinding.vertexCount);
                }

                if (geometryI.indexBinding != null) {
                    triangles
                        .indexData(VkDeviceOrHostAddressConstKHR.create().deviceAddress(geometryI.indexBinding.address))
                        .indexType(geometryI.indexBinding.type)
                        .maxVertex(geometryI.indexBinding.vertexCount);
                }

                //
                this.primitiveCount[I] = triangles.maxVertex()/3;
                this.geometryInfo.get(I)
                    .sType(VK_STRUCTURE_TYPE_ACCELERATION_STRUCTURE_GEOMETRY_KHR)
                    .geometryType(this.ASLevel == VK_ACCELERATION_STRUCTURE_TYPE_TOP_LEVEL_KHR ? VK_GEOMETRY_TYPE_INSTANCES_KHR : VK_GEOMETRY_TYPE_TRIANGLES_KHR)
                    .geometry(this.geometryData.get(I).triangles(triangles))
                    .flags((geometryI.opaque ? VK_GEOMETRY_OPAQUE_BIT_KHR : 0) | VK_GEOMETRY_NO_DUPLICATE_ANY_HIT_INVOCATION_BIT_KHR);
            });
        }

        //
        this.geometryBuildInfo
            .geometryCount(this.geometryInfo.remaining())
            .pGeometries(this.geometryInfo);

        //
        return this;
    }

    //
    public long getDeviceAddress() {
        if (this.deviceAddress == 0) {
            
            this.deviceAddress = vkGetAccelerationStructureDeviceAddressKHR(deviceObj.device, VkAccelerationStructureDeviceAddressInfoKHR.create().pNext(0L).sType(VK_STRUCTURE_TYPE_ACCELERATION_STRUCTURE_DEVICE_ADDRESS_INFO_KHR).accelerationStructure(this.handle.get()));
            deviceObj.addressMap.add(new LongInterval(this.deviceAddress, this.deviceAddress + this.buildSizeInfo.accelerationStructureSize(), Interval.Bounded.CLOSED));
            deviceObj.rootMap.put$(this.deviceAddress, this.handle.get());
        }
        return this.deviceAddress;
    }

    //
    public AccelerationStructureObj cmdBuild(VkCommandBuffer cmdBuf, VkAccelerationStructureBuildRangeInfoKHR.Buffer buildRanges, int mode) {
        this.geometryBuildInfo.mode(mode);
        this.geometryBuildInfo.srcAccelerationStructure(mode == VK_BUILD_ACCELERATION_STRUCTURE_MODE_UPDATE_KHR ? this.handle.get() : 0);

        //
        vkCmdBuildAccelerationStructuresKHR(cmdBuf, this.geometryBuildInfo, createPointerBuffer(1).put(0, buildRanges.address()));
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

        public TopAccelerationStructureObj(UtilsCInfo.Handle base, UtilsCInfo.Handle handle) {
            super(base, handle);
        }

        public TopAccelerationStructureObj(UtilsCInfo.Handle base, AccelerationStructureCInfo.TopAccelerationStructureCInfo cInfo) {
            super(base, cInfo);
        }
    }

    //
    static public class BottomAccelerationStructureObj extends AccelerationStructureObj {
        public int ASLevel = VK_ACCELERATION_STRUCTURE_TYPE_BOTTOM_LEVEL_KHR;

        public BottomAccelerationStructureObj(UtilsCInfo.Handle base, UtilsCInfo.Handle handle) {
            super(base, handle);
        }

        public BottomAccelerationStructureObj(UtilsCInfo.Handle base, AccelerationStructureCInfo.BottomAccelerationStructureCInfo cInfo) {
            super(base, cInfo);
        }

    }
}
