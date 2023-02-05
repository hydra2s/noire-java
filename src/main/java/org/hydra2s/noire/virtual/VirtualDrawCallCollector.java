package org.hydra2s.noire.virtual;

//

import org.hydra2s.noire.descriptors.AccelerationStructureCInfo;
import org.hydra2s.noire.descriptors.BufferCInfo;
import org.hydra2s.noire.descriptors.DataCInfo;
import org.hydra2s.noire.descriptors.MemoryAllocationCInfo;
import org.hydra2s.noire.objects.AccelerationStructureObj;
import org.hydra2s.noire.objects.BufferObj;
import org.hydra2s.noire.objects.MemoryAllocatorObj;
import org.lwjgl.vulkan.*;

import java.util.ArrayList;

import static java.lang.Math.min;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.KHRAccelerationStructure.VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR;
import static org.lwjgl.vulkan.KHRAccelerationStructure.VK_BUILD_ACCELERATION_STRUCTURE_MODE_BUILD_KHR;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK13.*;

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
    public VkMultiDrawInfoEXT.Buffer multiDraw = null;
    public VkAccelerationStructureBuildRangeInfoKHR.Buffer ranges = null;
    public ArrayList<DataCInfo.TriangleGeometryCInfo> geometries = null;

    // use a registry as Virtual Draw Call collection
    //public ArrayList<VirtualDrawCallObj> drawCalls = null;

    // recommended to store in device memory
    public BufferBudget uniformDataBudget = null;

    //
    public AccelerationStructureObj.BottomAccelerationStructureObj bottomLvl = null;
    public AccelerationStructureObj.TopAccelerationStructureObj topLvl = null;
    public BufferObj instanceBuffer = null;
    public VkAccelerationStructureInstanceKHR instanceInfo = null;

    //
    static final public boolean enableRayTracing = false;

    //
    public VirtualDrawCallCollector(Handle base, VirtualDrawCallCollectorCInfo cInfo) throws Exception {
        super(base, cInfo);

        //
        this.handle = new Handle("VirtualDrawCallCollector", memAddress(memAllocLong(1)));
        this.memoryAllocatorObj = (MemoryAllocatorObj) globalHandleMap.get(cInfo.memoryAllocator).orElse(null);
        deviceObj.handleMap.put$(this.handle, this);

        //
        this.uniformDataBudget = new BufferBudget(){{
            bufferObj = new BufferObj(base, new BufferCInfo() {{
                size = cInfo.maxDrawCalls * cInfo.drawCallUniformStride;
                usage = VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | VK_BUFFER_USAGE_VERTEX_BUFFER_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT | VK_BUFFER_USAGE_STORAGE_BUFFER_BIT | VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR;
                memoryAllocator = cInfo.memoryAllocator;
                memoryAllocationInfo = new MemoryAllocationCInfo() {{
                    isHost = false;
                    isDevice = true;
                }};
            }});
        }};

        //
        if (enableRayTracing) {
            this.bottomLvl = new AccelerationStructureObj.BottomAccelerationStructureObj(deviceObj.getHandle(), new AccelerationStructureCInfo.BottomAccelerationStructureCInfo() {{
                memoryAllocator = memoryAllocatorObj.getHandle().get();
                geometries = new ArrayList<>() {{
                    for (int I = 0; I < cInfo.maxDrawCalls; I++) {
                        add(new DataCInfo.TriangleGeometryCInfo() {{
                            vertexBinding = new DataCInfo.VertexBindingCInfo() {{
                                stride = 48;
                                vertexCount = (int) (cInfo.vertexAverageCount * 3);
                                format = VK_FORMAT_R32G32B32_SFLOAT;
                            }};
                            indexBinding = new DataCInfo.IndexBindingCInfo() {{
                                vertexCount = (int) (cInfo.vertexAverageCount * 3);
                                type = VK_INDEX_TYPE_UINT32;
                            }};
                        }});
                    }
                }};
            }});

            //
            this.instanceBuffer = new BufferObj(deviceObj.getHandle(), new BufferCInfo() {{
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

            //
            this.topLvl = new AccelerationStructureObj.TopAccelerationStructureObj(deviceObj.getHandle(), new AccelerationStructureCInfo.TopAccelerationStructureCInfo() {{
                memoryAllocator = memoryAllocatorObj.getHandle().get();
                instances = new DataCInfo.InstanceGeometryCInfo() {{
                    instanceBinding = new DataCInfo.InstanceBindingCInfo() {{
                        address = instanceBuffer.getDeviceAddress();
                        vertexCount = 1;
                    }};
                }};
            }});
        }
    }

    // TODO: reusing same memory support
    // Also, using similar of transform feedback
    @Override
    public VirtualDrawCallCollector clear() {
        super.clear();
        this.uniformDataBudget.reset();
        return this;
    }

    //
    public VirtualDrawCallObj collectDrawCall(VirtualDrawCallCollectorCInfo.VirtualDrawCallCInfo drawCallInfo) {
        drawCallInfo.registryHandle = this.handle.get();
        return new VirtualDrawCallObj(this.base, drawCallInfo);
    }

    // TODO: multiple instance support
    public VirtualDrawCallCollector cmdBuildAccelerationStructure(VkCommandBuffer cmdBuf) {
        if (enableRayTracing) {
            this.bottomLvl.recallGeometryInfo();
            this.bottomLvl.cmdBuild(cmdBuf, this.ranges, VK_BUILD_ACCELERATION_STRUCTURE_MODE_BUILD_KHR);
            this.instanceBuffer.cmdSynchronizeFromHost(cmdBuf);
            this.topLvl.cmdBuild(cmdBuf, VkAccelerationStructureBuildRangeInfoKHR.calloc(1)
                    .primitiveCount(1)
                    .firstVertex(0)
                    .primitiveOffset(0)
                    .transformOffset(0),
                VK_BUILD_ACCELERATION_STRUCTURE_MODE_BUILD_KHR);
        }
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

        // use sorted data for draw (and possible, culling)
        var Rs = this.registry.size();
        for (var I=0;I<Rs;I++) {
            var drawCall = (VirtualDrawCallObj)this.registry.get(I);
            var drawCallCInfo = (VirtualDrawCallCollectorCInfo.VirtualDrawCallCInfo)drawCall.cInfo;

            //
            var vertexArrayObj = drawCallCInfo.vertexArray;
            var vertexBinding0 = vertexArrayObj.bindings.get(0);

            //
            this.multiDraw.get(I).set(0, drawCallCInfo.vertexCount);
            this.ranges.get(I).set(drawCallCInfo.vertexCount/3, 0, 0, 0);

            //
            if (enableRayTracing) {
                this.geometries.add(new DataCInfo.TriangleGeometryCInfo() {{
                    transformAddress = drawCall.uniformBuffer.address; //+ vertexArrayStride;
                    indexBinding = new DataCInfo.IndexBindingCInfo() {{
                        address = drawCallCInfo.indexAddress;
                        type = drawCallCInfo.indexType;
                        vertexCount = drawCallCInfo.indexCount;
                    }};
                    vertexBinding = new DataCInfo.VertexBindingCInfo() {{
                        address = vertexBinding0.bufferAddress;
                        stride = vertexBinding0.stride;
                        vertexCount = drawCallCInfo.vertexCount;
                        format = vertexBinding0.format;
                    }};
                }});
            }
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
            var virtualDrawCallCollector = (VirtualDrawCallCollector)deviceObj.handleMap.get(new Handle("VirtualDrawCallCollector", cInfo.registryHandle)).orElse(null);
            var vertexArrayObj = cInfo.vertexArray;
            var vertexBinding0 = vertexArrayObj.bindings.get(0);

            //
            this.bound = virtualDrawCallCollector;
            assert this.bound != null;
            this.DSC_ID = this.bound.registry.push(this);
            this.virtualGL = this.DSC_ID+1;

            //
            this.vertexBuffer = new VirtualDrawCallCollectorCInfo.BufferRange() {{
                offset = cInfo.vertexRange != null ? cInfo.vertexRange.offset() : 0L;
                stride = vertexBinding0.stride;
                range = cInfo.vertexRange != null && vertexBinding0.bufferSize > 0L ? min(vertexBinding0.bufferSize, cInfo.vertexRange.range()) : (vertexBinding0.bufferSize > 0L ? vertexBinding0.bufferSize : cInfo.vertexRange.range());
                address = cInfo.vertexAddress != 0L ? cInfo.vertexAddress : vertexBinding0.bufferAddress;
                assert cInfo.vertexRange != null;
                handle = cInfo.vertexRange.buffer();
            }};

            //
            this.indexBuffer = new VirtualDrawCallCollectorCInfo.BufferRange() {{
                offset = cInfo.indexRange.offset();
                stride = 0;
                range = cInfo.indexRange.range();
                address = cInfo.indexAddress;
                handle = cInfo.indexRange.buffer();
            }};

            //
            this.uniformBuffer = new VirtualDrawCallCollectorCInfo.BufferRange() {{
                offset = virtualDrawCallCollector.uniformDataBudget.offset;
                stride = ((VirtualDrawCallCollectorCInfo)bound.cInfo).drawCallUniformStride;
                range = ((VirtualDrawCallCollectorCInfo)bound.cInfo).drawCallUniformStride;
                address = virtualDrawCallCollector.uniformDataBudget.bufferObj.getDeviceAddress() + virtualDrawCallCollector.uniformDataBudget.offset;
                handle = virtualDrawCallCollector.uniformDataBudget.bufferObj.getHandle().get();
            }};

            //
            virtualDrawCallCollector.uniformDataBudget.offset += this.uniformBuffer.range;
        }

        // DEPRECATED!
        // TODO: Using Command Writer!
        public VirtualDrawCallObj cmdCopyFromSource(VkCommandBuffer cmdBuf) {
            var cInfo = (VirtualDrawCallCollectorCInfo.VirtualDrawCallCInfo)this.cInfo;
            var vertexArrayObj = cInfo.vertexArray;

            //
            var indexRange = cInfo.indexRange;
            var vertexRange = cInfo.vertexRange;

            //
            if (cInfo.vertexRange != null) {
                vertexArrayObj.vertexBufferForAll(cInfo.vertexAddress, vertexRange.range()).writeData();
            }

            // TODO: using new and better methods
            // copy uniform based information from memories
            vkCmdUpdateBuffer(cmdBuf, uniformBuffer.handle, uniformBuffer.offset, cInfo.uniformData);
            vkCmdPipelineBarrier2(cmdBuf, VkDependencyInfoKHR.calloc().sType(VK_STRUCTURE_TYPE_DEPENDENCY_INFO).pBufferMemoryBarriers(VkBufferMemoryBarrier2.calloc(1)
                .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER_2)
                .srcStageMask(VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT | VK_PIPELINE_STAGE_2_HOST_BIT)
                .srcAccessMask(VK_ACCESS_2_TRANSFER_WRITE_BIT | VK_ACCESS_2_HOST_WRITE_BIT)
                .dstStageMask(VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT)
                .dstAccessMask(VK_ACCESS_2_MEMORY_WRITE_BIT)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .buffer(uniformBuffer.handle)
                .offset(uniformBuffer.offset)
                .size(((VirtualDrawCallCollectorCInfo)bound.cInfo).drawCallUniformStride)));

            //
            return this;
        }
    }

}
