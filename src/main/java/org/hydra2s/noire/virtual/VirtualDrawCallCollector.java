package org.hydra2s.noire.virtual;

//
import org.hydra2s.noire.descriptors.AccelerationStructureCInfo;
import org.hydra2s.noire.descriptors.BufferCInfo;
import org.hydra2s.noire.descriptors.DataCInfo;
import org.hydra2s.noire.descriptors.MemoryAllocationCInfo;
import org.hydra2s.noire.objects.*;
import org.lwjgl.vulkan.*;

//
import java.util.ArrayList;

//
import static java.lang.Math.max;
import static java.lang.Math.min;
import static org.hydra2s.noire.virtual.VirtualVertexArrayHeap.vertexArrayStride;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.EXTIndexTypeUint8.VK_INDEX_TYPE_UINT8_EXT;
import static org.lwjgl.vulkan.KHRAccelerationStructure.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK13.VK_STRUCTURE_TYPE_BUFFER_COPY_2;

// Will collect draw calls data for building acceleration structures
// TODO: needs add sorting support (per morton-code)
// What does or should to do morton code?
// - Partially control of memory order
// - Partially control randomization when draw call
// - Partially fix some dis-orders
// - Partially fix acceleration structure memory re-using
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
    public final static int drawCallUniformStride = 384 + vertexArrayStride;

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
    public AccelerationStructureObj.BottomAccelerationStructureObj bottomLvl = null;
    public AccelerationStructureObj.TopAccelerationStructureObj topLvl = null;
    public BufferObj instanceBuffer = null;
    public VkAccelerationStructureInstanceKHR instanceInfo = null;

    //
    public VirtualDrawCallCollector(Handle base, VirtualDrawCallCollectorCInfo cInfo) throws Exception {
        super(base, cInfo);

        //
        var memoryAllocatorObj = (MemoryAllocatorObj) BasicObj.globalHandleMap.get(this.base.get());
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(memoryAllocatorObj.getBase().get());
        var physicalDeviceObj = (PhysicalDeviceObj)BasicObj.globalHandleMap.get(deviceObj.getBase().get());

        //
        this.handle = new Handle("VirtualDrawCallCollector", memAddress(memAllocLong(1)));
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

        //
        this.bottomLvl = new AccelerationStructureObj.BottomAccelerationStructureObj(deviceObj.getHandle(), new AccelerationStructureCInfo.BottomAccelerationStructureCInfo(){{
            memoryAllocator = memoryAllocatorObj.getHandle().get();
            geometries = new ArrayList<>() {{
                for (int I = 0; I < cInfo.maxDrawCalls; I++) {
                    add(new DataCInfo.TriangleGeometryCInfo() {{
                        vertexBinding = new DataCInfo.VertexBindingCInfo() {{
                            stride = 48;
                            vertexCount = (int) (vertexAverageCount * 3);
                            format = VK_FORMAT_R32G32B32_SFLOAT;
                        }};
                        indexBinding = new DataCInfo.IndexBindingCInfo() {{
                            vertexCount = (int) (vertexAverageCount * 3);
                            type = VK_INDEX_TYPE_UINT32;
                        }};
                    }});
                }
            }};
        }});

        //
        this.instanceBuffer = new BufferObj(deviceObj.getHandle(), new BufferCInfo(){{
            memoryAllocationInfo = new MemoryAllocationCInfo() {{
                isHost = true;
                isDevice = true;
            }};
            size = VkAccelerationStructureInstanceKHR.SIZEOF;
            usage = VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT | VK_BUFFER_USAGE_STORAGE_BUFFER_BIT;
            memoryAllocator = cInfo.memoryAllocator;
        }});

        //
        this.instanceInfo = VkAccelerationStructureInstanceKHR.create(memAddress(instanceBuffer.map(VkAccelerationStructureInstanceKHR.SIZEOF, 0)));
        this.instanceInfo.mask(0xFF).accelerationStructureReference(bottomLvl.getDeviceAddress()).flags(0);

        // will be changed into camera position shifting
        this.instanceInfo.transform(VkTransformMatrixKHR.calloc().matrix(memAllocFloat(12).put(0, new float[]{
            1.0F, 0.0F, 0.0F, 0.0F,
            0.0F, 1.0F, 0.0F, 0.0F,
            0.0F, 0.0F, 1.0F, 0.0F
        })));
    }

    // TODO: reusing same memory support
    // Also, using similar of transform feedback
    @Override
    public VirtualDrawCallCollector clear() {
        super.clear();
        this.vertexDataBudget.reset();
        this.indexDataBudget.reset();
        this.uniformDataBudget.reset();
        return this;
    }

    //
    public VirtualDrawCallObj collectDrawCall(VirtualDrawCallCollectorCInfo.VirtualDrawCallCInfo drawCallInfo) {
        return new VirtualDrawCallObj(this.base, drawCallInfo);
    }

    // TODO: multiple instance support
    public VirtualDrawCallCollector cmdBuildAccelerationStructure(VkCommandBuffer cmdBuf) {
        this.bottomLvl.recallGeometryInfo();
        this.bottomLvl.cmdBuild(cmdBuf, this.ranges, VK_BUILD_ACCELERATION_STRUCTURE_MODE_BUILD_KHR);
        this.instanceBuffer.cmdSynchronizeFromHost(cmdBuf);
        this.topLvl.cmdBuild(cmdBuf, VkAccelerationStructureBuildRangeInfoKHR.calloc(1)
                .primitiveCount(1)
                .firstVertex(0)
                .primitiveOffset(0)
                .transformOffset(0),
            VK_BUILD_ACCELERATION_STRUCTURE_MODE_BUILD_KHR);
        return this;
    }

    // TODO: needs to add sorting and morton code support
    // TODO: reusing same memory support
    public VirtualDrawCallCollector finishCollection() {
        this.applyOrdering();

        //
        this.geometries = new ArrayList<>();
        this.ranges = VkAccelerationStructureBuildRangeInfoKHR.calloc(this.registry.size());
        this.multiDraw = VkMultiDrawInfoEXT.calloc(this.registry.size());

        //
        var memoryAllocatorObj = (MemoryAllocatorObj)BasicObj.globalHandleMap.get(this.base.get());
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(memoryAllocatorObj.getBase().get());
        var physicalDeviceObj = (PhysicalDeviceObj)BasicObj.globalHandleMap.get(deviceObj.getBase().get());

        // use sorted data for draw (and possible, culling)
        for (var I=0;I<this.sorted.size();I++) {
            var drawCall = (VirtualDrawCallObj)this.sorted.get(I);
            var drawCallCInfo = (VirtualDrawCallCollectorCInfo.VirtualDrawCallCInfo)drawCall.cInfo;

            var vertexArrayHeap = (VirtualVertexArrayHeap)deviceObj.handleMap.get(new Handle("VirtualVertexArrayHeap", drawCallCInfo.vertexArrayHeapHandle));
            var vertexArrayObj = (VirtualVertexArrayHeap.VirtualVertexArrayObj)vertexArrayHeap.registry.get(drawCallCInfo.vertexArrayObjectId);
            var vertexBinding0 = vertexArrayObj.bindings.get(0);

            //
            this.multiDraw.get(I).set(0, (int) drawCallCInfo.vertexCount);
            this.ranges.get(I).set((int) (drawCallCInfo.vertexCount/3), 0, 0, 0);
            this.geometries.add(new DataCInfo.TriangleGeometryCInfo() {{
                transformAddress = drawCall.uniformBuffer.address + vertexArrayStride;
                indexBinding = new DataCInfo.IndexBindingCInfo(){{
                    address = drawCallCInfo.indexData.address;
                    type = drawCallCInfo.indexData.address != 0 ? drawCallCInfo.indexData.type : VK_INDEX_TYPE_NONE_KHR;
                    vertexCount = (int) drawCallCInfo.vertexCount;
                }};
                vertexBinding = new DataCInfo.VertexBindingCInfo() {{
                    address = vertexBinding0.bufferAddress;
                    stride = vertexBinding0.stride;
                    vertexCount = (int) drawCallCInfo.vertexCount;
                    format = vertexBinding0.format;
                }};
            }});

            //
            this.topLvl = new AccelerationStructureObj.TopAccelerationStructureObj(deviceObj.getHandle(), new AccelerationStructureCInfo.TopAccelerationStructureCInfo(){{
                memoryAllocator = memoryAllocatorObj.getHandle().get();
                instances = new DataCInfo.InstanceGeometryCInfo(){{
                    instanceBinding = new DataCInfo.InstanceBindingCInfo(){{
                        address = instanceBuffer.getDeviceAddress();
                        vertexCount = 1;
                    }};
                }};
            }});
        }

        //
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

            // TODO: use virtual allocation and morton code (i.e. reusing data)
            this.vertexBuffer = new VirtualDrawCallCollectorCInfo.BufferRange(){{
                offset = virtualDrawCallCollector.vertexDataBudget.offset;
                stride = vertexBinding.stride;
                range = vertexBinding.bufferSize;
                address = virtualDrawCallCollector.vertexDataBudget.bufferObj.getDeviceAddress() + virtualDrawCallCollector.vertexDataBudget.offset;
                handle = virtualDrawCallCollector.vertexDataBudget.bufferObj.getHandle().get();
            }};

            // TODO: use virtual allocation and morton code (i.e. reusing data)
            this.indexBuffer = new VirtualDrawCallCollectorCInfo.BufferRange(){{
                offset = virtualDrawCallCollector.indexDataBudget.offset;
                stride = (cInfo.indexData.type == VK_INDEX_TYPE_UINT32 ? 4 : (cInfo.indexData.type == VK_INDEX_TYPE_UINT16 ? 2 : (cInfo.indexData.type == VK_INDEX_TYPE_UINT8_EXT ? 1 : 0)));
                range = cInfo.vertexCount * stride;
                address = virtualDrawCallCollector.indexDataBudget.bufferObj.getDeviceAddress() + virtualDrawCallCollector.indexDataBudget.offset;
                handle = virtualDrawCallCollector.indexDataBudget.bufferObj.getHandle().get();
            }};

            // TODO: use virtual allocation and morton code (i.e. reusing data)
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

        // TODO: add additional meta information about draw call
        public VirtualDrawCallObj cmdCopyFromSource(VkCommandBuffer cmdBuf) {
            var cInfo = (VirtualDrawCallCollectorCInfo.VirtualDrawCallCInfo)this.cInfo;
            var memoryAllocatorObj = (MemoryAllocatorObj)BasicObj.globalHandleMap.get(this.base.get());
            var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(memoryAllocatorObj.getBase().get());
            var vertexArrayHeap = (VirtualVertexArrayHeap)deviceObj.handleMap.get(new Handle("VirtualVertexArrayHeap", cInfo.vertexArrayHeapHandle));
            var vertexArrayObj = (VirtualVertexArrayHeap.VirtualVertexArrayObj)vertexArrayHeap.registry.get(cInfo.vertexArrayObjectId);

            //
            var vRange = vertexArrayHeap.getBufferRange();
            var vaoRange = vertexArrayObj.getBufferRange();
            var uniRange = cInfo.uniformRange;
            var indexRange = cInfo.indexData;

            // copy draw data
            CopyUtilObj.cmdCopyBufferToBuffer(cmdBuf, vRange.buffer(), vertexBuffer.handle, VkBufferCopy2.calloc(1).sType(VK_STRUCTURE_TYPE_BUFFER_COPY_2).srcOffset(vRange.offset()).dstOffset(vertexBuffer.offset).size(min(vRange.range(), vertexBuffer.range)));
            CopyUtilObj.cmdCopyBufferToBuffer(cmdBuf, indexRange.handle, indexBuffer.handle, VkBufferCopy2.calloc(1).sType(VK_STRUCTURE_TYPE_BUFFER_COPY_2).srcOffset(indexRange.offset).dstOffset(indexBuffer.offset).size(min(indexRange.range, indexBuffer.range)));

            // firstly, going VAO data (256 bytes), then uniform (384 bytes)
            // for uniform, firstly should to be going transform matrix
            CopyUtilObj.cmdCopyBufferToBuffer(cmdBuf, vaoRange.buffer(), uniformBuffer.handle, VkBufferCopy2.calloc(1).sType(VK_STRUCTURE_TYPE_BUFFER_COPY_2).srcOffset(vaoRange.offset()).dstOffset(uniformBuffer.offset).size(max(min(vaoRange.range(), uniformBuffer.range), 0L)));
            CopyUtilObj.cmdCopyBufferToBuffer(cmdBuf, uniRange.buffer(), uniformBuffer.handle, VkBufferCopy2.calloc(1).sType(VK_STRUCTURE_TYPE_BUFFER_COPY_2).srcOffset(uniRange.offset()).dstOffset(uniformBuffer.offset + vaoRange.range()).size(max(min(uniRange.range(), uniformBuffer.range-vaoRange.range()), 0L)));

            //
            return this;
        }
    }

}
