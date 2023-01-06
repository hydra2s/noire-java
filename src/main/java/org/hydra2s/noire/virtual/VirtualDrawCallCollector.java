package org.hydra2s.noire.virtual;

//
import org.hydra2s.noire.descriptors.BufferCInfo;
import org.hydra2s.noire.descriptors.DataCInfo;
import org.hydra2s.noire.descriptors.MemoryAllocationCInfo;
import org.hydra2s.noire.objects.*;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkAccelerationStructureBuildRangeInfoKHR;
import org.lwjgl.vulkan.VkMultiDrawInfoEXT;

//
import java.util.ArrayList;

//
import static org.lwjgl.system.MemoryUtil.memAllocLong;
import static org.lwjgl.vulkan.KHRAccelerationStructure.VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR;
import static org.lwjgl.vulkan.VK10.*;

// Will collect draw calls data for building acceleration structures
public class VirtualDrawCallCollector extends VirtualGLRegistry {

    //
    public static class BufferBudget {
        public BufferObj bufferObj = null;
        public long offset = 0L;

        //
        public BufferBudget reset() {
            this.offset = 0L;
            return this;
        }
    }

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
    protected BufferBudget vertexDataBudget = null;
    protected BufferBudget indexDataBudget = null;
    protected BufferBudget uniformDataBudget = null;

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
        this.vertexDataBudget = new BufferBudget(){{
            bufferObj = new BufferObj(base, new BufferCInfo() {{
                size = cInfo.maxDrawCalls * vertexAverageStride * vertexAverageCount;
                usage = VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT | VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR;
                memoryAllocator = cInfo.memoryAllocator;
                memoryAllocationInfo = new MemoryAllocationCInfo(){{
                    isHost = false;
                    isDevice = true;
                }};
            }});
        }};

        //
        this.indexDataBudget = new BufferBudget(){{
            bufferObj = new BufferObj(base, new BufferCInfo() {{
                size = cInfo.maxDrawCalls * vertexAverageCount * 4;
                usage = VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT | VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR;
                memoryAllocator = cInfo.memoryAllocator;
                memoryAllocationInfo = new MemoryAllocationCInfo() {{
                    isHost = false;
                    isDevice = true;
                }};
            }});
        }};

        //
        this.uniformDataBudget = new BufferBudget(){{
            bufferObj = new BufferObj(base, new BufferCInfo() {{
                size = cInfo.maxDrawCalls * drawCallUniformStride;
                usage = VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT | VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR;
                memoryAllocator = cInfo.memoryAllocator;
                memoryAllocationInfo = new MemoryAllocationCInfo() {{
                    isHost = false;
                    isDevice = true;
                }};
            }});
        }};

    }

    public VirtualDrawCallCollector resetDrawCalls() {
        this.registry.clear();
        return this;
    }

    public VirtualDrawCallCollector collectDrawCall(VirtualDrawCallCollectorCInfo.VirtualDrawCallCInfo drawCallInfo) {
        return this;
    }

    // TODO: finalize dev on such feature, but I'm tired
    public VirtualDrawCallCollector finishCollection() {
        this.geometries = new ArrayList<>();
        this.ranges = VkAccelerationStructureBuildRangeInfoKHR.calloc(this.registry.size());
        this.multiDraw = VkMultiDrawInfoEXT.calloc(this.registry.size());

        for (var I=0;I<this.registry.size();I++) {
            var drawCall = this.registry.get(I);
            var geometryInfo = new DataCInfo.TriangleGeometryCInfo();
            if (drawCall != null) {
                geometryInfo.indexBinding = new DataCInfo.IndexBindingCInfo(){{

                }};
                geometryInfo.vertexBinding = new DataCInfo.VertexBindingCInfo() {{

                }};
            }
        }

        return this;
    }

    // TODO: finalize dev on such feature, but I'm tired
    public static class VirtualDrawCallObj extends VirtualGLRegistry.VirtualGLObj {
        //
        public VirtualDrawCallCollectorCInfo.BufferRange vertexBuffer = null;
        public VirtualDrawCallCollectorCInfo.BufferRange indexBuffer = null;
        public VirtualDrawCallCollectorCInfo.BufferRange uniformBuffer = null;

        //
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
