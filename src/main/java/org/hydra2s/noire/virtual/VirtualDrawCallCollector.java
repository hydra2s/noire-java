package org.hydra2s.noire.virtual;

//
import org.hydra2s.noire.descriptors.BufferCInfo;
import org.hydra2s.noire.descriptors.DataCInfo;
import org.hydra2s.noire.descriptors.MemoryAllocationCInfo;
import org.hydra2s.noire.objects.*;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.*;

//
import java.util.ArrayList;

//
import static java.lang.Math.min;
import static org.lwjgl.system.MemoryUtil.memAllocLong;
import static org.lwjgl.vulkan.EXTIndexTypeUint8.VK_INDEX_TYPE_UINT8_EXT;
import static org.lwjgl.vulkan.KHRAccelerationStructure.VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK13.VK_STRUCTURE_TYPE_BUFFER_COPY_2;

// Will collect draw calls data for building acceleration structures
// TODO: needs add sorting support (per morton-code)
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

    //
    public VirtualDrawCallCollector resetDrawCalls() {
        this.registry.clear();
        this.vertexDataBudget.reset();
        this.indexDataBudget.reset();
        this.uniformDataBudget.reset();
        return this;
    }

    //
    public VirtualDrawCallObj collectDrawCall(VirtualDrawCallCollectorCInfo.VirtualDrawCallCInfo drawCallInfo) {
        return new VirtualDrawCallObj(this.base, drawCallInfo);
    }

    // TODO: needs to add sorting and morton code support
    public VirtualDrawCallCollector finishCollection() {
        this.geometries = new ArrayList<>();
        this.ranges = VkAccelerationStructureBuildRangeInfoKHR.calloc(this.registry.size());
        this.multiDraw = VkMultiDrawInfoEXT.calloc(this.registry.size());

        //
        var memoryAllocatorObj = (MemoryAllocatorObj)BasicObj.globalHandleMap.get(this.base.get());
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(memoryAllocatorObj.getBase().get());
        var physicalDeviceObj = (PhysicalDeviceObj)BasicObj.globalHandleMap.get(deviceObj.getBase().get());

        //
        for (var I=0;I<this.registry.size();I++) {
            var drawCall = (VirtualDrawCallObj)this.registry.get(I);
            var drawCallCInfo = (VirtualDrawCallCollectorCInfo.VirtualDrawCallCInfo)drawCall.cInfo;
            var geometryInfo = new DataCInfo.TriangleGeometryCInfo();
            var vertexArrayHeap = (VirtualVertexArrayHeap)deviceObj.handleMap.get(new Handle("VirtualVertexArrayHeap", drawCallCInfo.vertexArrayHeapHandle));
            var vertexArrayObj = (VirtualVertexArrayHeap.VirtualVertexArrayObj)vertexArrayHeap.registry.get(drawCallCInfo.vertexArrayObjectId);
            var vertexBinding = vertexArrayObj.bindings.get(0);

            //
            if (drawCall != null) {
                //
                this.multiDraw.get(I).set(0, (int) drawCallCInfo.vertexCount);
                this.ranges.get(I).set((int) (drawCallCInfo.vertexCount/3), 0, 0, 0);

                //
                geometryInfo.transformAddress = drawCall.uniformBuffer.address;
                geometryInfo.indexBinding = new DataCInfo.IndexBindingCInfo(){{
                    address = drawCallCInfo.indexData.address;
                    type = drawCallCInfo.indexData.type;
                    vertexCount = (int) drawCallCInfo.vertexCount;
                }};
                geometryInfo.vertexBinding = new DataCInfo.VertexBindingCInfo() {{
                    address = vertexBinding.bufferAddress;
                    stride = vertexBinding.stride;
                    vertexCount = (int) drawCallCInfo.vertexCount;
                    format = vertexBinding.format;
                }};
            }

            //
            geometries.add(geometryInfo);
        }

        return this;
    }

    // TODO: needs add sorting support (per morton-code)
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
            var vertexArrayHeap = (VirtualVertexArrayHeap)deviceObj.handleMap.get(new Handle("VirtualVertexArrayHeap", cInfo.vertexArrayHeapHandle));
            var vertexArrayObj = (VirtualVertexArrayHeap.VirtualVertexArrayObj)vertexArrayHeap.registry.get(cInfo.vertexArrayObjectId);
            var vertexBinding = vertexArrayObj.bindings.get(0);

            //
            this.bound = virtualDrawCallCollector;

            //
            this.DSC_ID = this.bound.registry.push(this);
            this.virtualGL = this.DSC_ID+1;

            //
            this.vertexBuffer = new VirtualDrawCallCollectorCInfo.BufferRange(){{
                offset = virtualDrawCallCollector.vertexDataBudget.offset;
                stride = vertexBinding.stride;
                range = vertexBinding.bufferSize;
                address = virtualDrawCallCollector.vertexDataBudget.bufferObj.getDeviceAddress() + virtualDrawCallCollector.vertexDataBudget.offset;
                handle = virtualDrawCallCollector.vertexDataBudget.bufferObj.getHandle().get();
            }};

            //
            this.indexBuffer = new VirtualDrawCallCollectorCInfo.BufferRange(){{
                offset = virtualDrawCallCollector.indexDataBudget.offset;
                stride = (cInfo.indexData.type == VK_INDEX_TYPE_UINT32 ? 4 : (cInfo.indexData.type == VK_INDEX_TYPE_UINT16 ? 2 : (cInfo.indexData.type == VK_INDEX_TYPE_UINT8_EXT ? 1 : 0)));
                range = cInfo.vertexCount * stride;
                address = virtualDrawCallCollector.indexDataBudget.bufferObj.getDeviceAddress() + virtualDrawCallCollector.indexDataBudget.offset;
                handle = virtualDrawCallCollector.indexDataBudget.bufferObj.getHandle().get();
            }};

            //
            this.uniformBuffer = new VirtualDrawCallCollectorCInfo.BufferRange(){{
                offset = virtualDrawCallCollector.uniformDataBudget.offset;
                stride = drawCallUniformStride;
                range = drawCallUniformStride;
                address = virtualDrawCallCollector.uniformDataBudget.bufferObj.getDeviceAddress() + virtualDrawCallCollector.uniformDataBudget.offset;
                handle = virtualDrawCallCollector.uniformDataBudget.bufferObj.getHandle().get();
            }};

            //
            virtualDrawCallCollector.vertexDataBudget.offset += this.vertexBuffer.range;
            virtualDrawCallCollector.indexDataBudget.offset += this.indexBuffer.range;
            virtualDrawCallCollector.uniformDataBudget.offset += this.uniformBuffer.range;
        }

        //
        public VirtualDrawCallObj cmdCopyFromSource(VkCommandBuffer cmdBuf) {
            var cInfo = (VirtualDrawCallCollectorCInfo.VirtualDrawCallCInfo)this.cInfo;
            var memoryAllocatorObj = (MemoryAllocatorObj)BasicObj.globalHandleMap.get(this.base.get());
            var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(memoryAllocatorObj.getBase().get());
            var physicalDeviceObj = (PhysicalDeviceObj)BasicObj.globalHandleMap.get(deviceObj.getBase().get());
            var virtualDrawCallCollector = (VirtualDrawCallCollector)deviceObj.handleMap.get(new Handle("VirtualDrawCallCollector", cInfo.registryHandle));
            var vertexArrayHeap = (VirtualVertexArrayHeap)deviceObj.handleMap.get(new Handle("VirtualVertexArrayHeap", cInfo.vertexArrayHeapHandle));
            var vertexArrayObj = (VirtualVertexArrayHeap.VirtualVertexArrayObj)vertexArrayHeap.registry.get(cInfo.vertexArrayObjectId);
            var vertexBinding = vertexArrayObj.bindings.get(0);

            //
            var vRange = vertexArrayHeap.getBufferRange();
            var vaoRange = vertexArrayObj.getBufferRange();
            var uniRange = cInfo.uniformRange;
            var indexRange = cInfo.indexData;

            //
            MemoryAllocationObj.cmdCopyBufferToBuffer(cmdBuf, vRange.buffer(), vertexBuffer.handle,
                VkBufferCopy2.calloc(1).sType(VK_STRUCTURE_TYPE_BUFFER_COPY_2).srcOffset(vRange.offset()).dstOffset(vertexBuffer.offset).size(min(vRange.range(), vertexBuffer.range))
            );
            MemoryAllocationObj.cmdCopyBufferToBuffer(cmdBuf, indexRange.handle, indexBuffer.handle,
                VkBufferCopy2.calloc(1).sType(VK_STRUCTURE_TYPE_BUFFER_COPY_2).srcOffset(indexRange.offset).dstOffset(indexBuffer.offset).size(min(indexRange.range, indexBuffer.range))
            );
            MemoryAllocationObj.cmdCopyBufferToBuffer(cmdBuf, vaoRange.buffer(), uniformBuffer.handle,
                VkBufferCopy2.calloc(1).sType(VK_STRUCTURE_TYPE_BUFFER_COPY_2).srcOffset(vaoRange.offset()).dstOffset(uniformBuffer.offset).size(min(vaoRange.range(), uniformBuffer.range))
            );
            MemoryAllocationObj.cmdCopyBufferToBuffer(cmdBuf, uniRange.buffer(), uniformBuffer.handle,
                VkBufferCopy2.calloc(1).sType(VK_STRUCTURE_TYPE_BUFFER_COPY_2).srcOffset(uniRange.offset()).dstOffset(uniformBuffer.offset + vaoRange.range()).size(min(uniRange.range(), uniformBuffer.range))
            );

            //
            return this;
        }
    }

}
