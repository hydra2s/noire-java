package org.hydra2s.noire.virtual;

//

import org.hydra2s.noire.descriptors.BufferCInfo;
import org.hydra2s.noire.descriptors.DataCInfo;
import org.hydra2s.noire.descriptors.MemoryAllocationCInfo;
import org.hydra2s.noire.objects.*;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkAccelerationStructureBuildRangeInfoKHR;
import org.lwjgl.vulkan.VkMultiDrawInfoEXT;

import java.util.ArrayList;

import static org.lwjgl.system.MemoryUtil.memAllocLong;
import static org.lwjgl.vulkan.KHRAccelerationStructure.VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR;
import static org.lwjgl.vulkan.VK10.*;

// Will collect draw calls data for building acceleration structures
public class VirtualDrawCallCollector extends VirtualGLRegistry {

    //
    public final static int vertexAverageStride = 32;
    public final static int vertexAverageCount = 768;
    public final static int drawCallUniformStride = 384;

    //
    public VkMultiDrawInfoEXT.Buffer multiDraw = null;
    public VkAccelerationStructureBuildRangeInfoKHR.Buffer ranges = null;
    public ArrayList<DataCInfo.TriangleGeometryCInfo> geometries = null;

    // use a registry as Virtual Draw Call collection
    //public ArrayList<VirtualDrawCallObj> drawCalls = null;

    // recommended to store in device memory
    protected BufferObj vertexDataBuffer = null;
    protected BufferObj indexDataBuffer = null;
    protected BufferObj uniformDataBuffer = null;

    //
    protected long vertexBufferOffset = 0L;
    protected long indexBufferOffset = 0L;
    protected long uniformBufferOffset = 0L;

    //
    public VirtualDrawCallCollector(Handle base, VirtualDrawCallCollectorCInfo cInfo) throws Exception {
        super(base, cInfo);

        //
        var memoryAllocatorObj = (MemoryAllocatorObj) BasicObj.globalHandleMap.get(this.base.get());
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(memoryAllocatorObj.getBase().get());
        var physicalDeviceObj = (PhysicalDeviceObj)BasicObj.globalHandleMap.get(deviceObj.getBase().get());

        //
        this.handle = new Handle("VirtualDrawCallCollector", MemoryUtil.memAddress(memAllocLong(1)));
        deviceObj.handleMap.put(this.handle, this);

        //
        this.vertexBufferOffset = 0L;
        this.indexBufferOffset = 0L;
        this.uniformBufferOffset = 0L;

        //
        this.vertexDataBuffer = new BufferObj(this.base, new BufferCInfo() {{
            size = cInfo.maxDrawCalls * vertexAverageStride * vertexAverageCount;
            usage = VK_BUFFER_USAGE_VERTEX_BUFFER_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT | VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR;
            memoryAllocator = cInfo.memoryAllocator;
            memoryAllocationInfo = new MemoryAllocationCInfo(){{
                isHost = false;
                isDevice = true;
            }};
        }});

        //
        this.indexDataBuffer = new BufferObj(this.base, new BufferCInfo() {{
            size = cInfo.maxDrawCalls * vertexAverageCount * 4;
            usage = VK_BUFFER_USAGE_VERTEX_BUFFER_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT | VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR;
            memoryAllocator = cInfo.memoryAllocator;
            memoryAllocationInfo = new MemoryAllocationCInfo(){{
                isHost = false;
                isDevice = true;
            }};
        }});

        //
        this.uniformDataBuffer = new BufferObj(this.base, new BufferCInfo() {{
            size = cInfo.maxDrawCalls * drawCallUniformStride;
            usage = VK_BUFFER_USAGE_VERTEX_BUFFER_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT | VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR;
            memoryAllocator = cInfo.memoryAllocator;
            memoryAllocationInfo = new MemoryAllocationCInfo(){{
                isHost = false;
                isDevice = true;
            }};
        }});
    }

    public VirtualDrawCallCollector resetDrawCalls() {
        this.registry.clear();
        return this;
    }

    public VirtualDrawCallCollector collectDrawCall(VirtualDrawCallCollectorCInfo.VirtualDrawCallCInfo drawCallInfo) {
        return this;
    }

    public VirtualDrawCallCollector finishCollection() {
        this.geometries = new ArrayList<>();
        this.ranges = VkAccelerationStructureBuildRangeInfoKHR.calloc(this.registry.size());
        this.multiDraw = VkMultiDrawInfoEXT.calloc(this.registry.size());
        return this;
    }

    //
    public static class VirtualDrawCallObj extends VirtualGLRegistry.VirtualGLObj {


        public VirtualDrawCallObj(Handle base, VirtualDrawCallCollectorCInfo.VirtualDrawCallCInfo cInfo) {
            super(base, cInfo);

            //
            var memoryAllocatorObj = (MemoryAllocatorObj)BasicObj.globalHandleMap.get(this.base.get());
            var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(memoryAllocatorObj.getBase().get());
            var physicalDeviceObj = (PhysicalDeviceObj)BasicObj.globalHandleMap.get(deviceObj.getBase().get());
            var virtualDrawCallCollector = (VirtualDrawCallCollector)deviceObj.handleMap.get(new Handle("VirtualDrawCallCollector", cInfo.registryHandle));

            //
            this.bound = virtualDrawCallCollector;

            //
            this.DSC_ID = this.bound.registry.push(this);
            this.virtualGL = this.DSC_ID+1;
        }


    }

}
