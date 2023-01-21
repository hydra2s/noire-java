package org.hydra2s.noire.objects;

//

import org.hydra2s.noire.descriptors.BufferCInfo;
import org.hydra2s.noire.descriptors.ImageViewCInfo;
import org.hydra2s.noire.descriptors.MemoryAllocationCInfo;
import org.hydra2s.noire.descriptors.PipelineLayoutCInfo;
import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.EXTDescriptorBuffer.*;
import static org.lwjgl.vulkan.EXTMutableDescriptorType.VK_DESCRIPTOR_TYPE_MUTABLE_EXT;
import static org.lwjgl.vulkan.EXTMutableDescriptorType.VK_STRUCTURE_TYPE_MUTABLE_DESCRIPTOR_TYPE_CREATE_INFO_EXT;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK12.*;
import static org.lwjgl.vulkan.VK13.VK_DESCRIPTOR_TYPE_INLINE_UNIFORM_BLOCK;

// TODO: fallback into traditional binding model!
public class PipelineLayoutObj extends BasicObj  {
    protected LongBuffer pipelineLayout = memAllocLong(1);
    protected PointerBuffer descriptorLayout = memAllocPointer(3);
    protected VkPushConstantRange.Buffer pConstRange = null;

    // TODO: make group by type
    protected VkDescriptorSetLayoutBindingFlagsCreateInfoEXT resourceDescriptorSetLayoutCreateInfoBindingFlags = null;
    protected VkDescriptorSetLayoutBindingFlagsCreateInfoEXT samplerDescriptorSetLayoutCreateInfoBindingFlags = null;
    protected VkDescriptorSetLayoutBindingFlagsCreateInfoEXT uniformDescriptorSetLayoutCreateInfoBindingFlags = null;
    protected VkDescriptorSetLayoutBindingFlagsCreateInfoEXT pipelineDescriptorSetLayoutCreateInfoBindingFlags = null;

    // TODO: make group by type
    protected VkDescriptorSetLayoutCreateInfo resourceDescriptorSetLayoutCreateInfo = null;
    protected VkDescriptorSetLayoutCreateInfo samplerDescriptorSetLayoutCreateInfo = null;
    protected VkDescriptorSetLayoutCreateInfo uniformDescriptorSetLayoutCreateInfo = null;
    protected VkDescriptorSetLayoutCreateInfo pipelineDescriptorSetLayoutCreateInfo = null;

    //
    protected VkMutableDescriptorTypeCreateInfoEXT mutableDescriptorInfo = null;
    protected VkMutableDescriptorTypeListEXT.Buffer mutableDescriptorLists = null;
    protected IntBuffer mutableDescriptorTypes = memAllocInt(1).put(0, 0);

    // TODO: make group by type
    protected IntBuffer resourceDescriptorSetBindingFlags = memAllocInt(1).put(0, 0);
    protected IntBuffer samplerDescriptorSetBindingFlags = memAllocInt(1).put(0, 0);
    protected IntBuffer uniformDescriptorSetBindingFlags = memAllocInt(1).put(0, 0);
    protected IntBuffer pipelineDescriptorSetBindingFlags = memAllocInt(1).put(0, 0);

    //
    static final protected long uniformBufferSize = 8192;

    // TODO: make group by type
    protected VkDescriptorSetLayoutBinding.Buffer resourceDescriptorSetBindings = null;
    protected VkDescriptorSetLayoutBinding.Buffer samplerDescriptorSetBindings = null;
    protected VkDescriptorSetLayoutBinding.Buffer uniformDescriptorSetBindings = null;
    protected VkDescriptorSetLayoutBinding.Buffer pipelineDescriptorSetBindings = null;

    //
    protected BufferObj resourceDescriptorBuffer = null;
    protected BufferObj samplerDescriptorBuffer = null;
    public BufferObj uniformDescriptorBuffer = null;

    //
    protected LongBuffer offsets = memAllocLong(4);
    protected LongBuffer sizes = memAllocLong(4);

    //
    static public class OutstandingArray<E> extends ArrayList<E> {
        protected ArrayList<Integer> empty = new ArrayList<Integer>();

        public OutstandingArray() {
            super();

            this.empty = new ArrayList<Integer>();
        }

        //
        public int push(E member) {
            int index = -1; if (this.empty.size() > 0) { index = this.empty.get(this.empty.size()-1); this.empty.remove(this.empty.size()-1); this.set(index, member); } else { index = this.size(); super.add(member); }; return index;
        }

        //
        public E removeIndex(int index) {
            var member = this.get(index);
            if (this.get(index) != null) {
                this.set(index, null);
                this.empty.add(index);
            }
            return member;
        }

        //
        public int removeMem(E member) {
            int index = this.indexOf(member);
            if (index >= 0) { this.removeIndex(index); };
            return index;
        }

        //
        @Override
        public void clear() {
            super.clear();
            this.empty.clear();
        }
    }

    //
    protected OutstandingArray<VkDescriptorImageInfo> resources = new OutstandingArray<VkDescriptorImageInfo>();
    protected OutstandingArray<LongBuffer> samplers = new OutstandingArray<LongBuffer>();

    //
    public PipelineLayoutObj(Handle base, PipelineLayoutCInfo cInfo) {
        super(base, cInfo);

        //
        var deviceObj = (DeviceObj) BasicObj.globalHandleMap.get(base.get());
        var physicalDeviceObj = (PhysicalDeviceObj) BasicObj.globalHandleMap.get(deviceObj.base.get());

        //
        this.resources = new OutstandingArray<VkDescriptorImageInfo>();
        this.samplers = new OutstandingArray<LongBuffer>();
        this.descriptorLayout = memAllocPointer(4);

        //
        this.mutableDescriptorTypes = memAllocInt(2).put(0, VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE).put(1, VK_DESCRIPTOR_TYPE_STORAGE_IMAGE);
        this.mutableDescriptorLists = VkMutableDescriptorTypeListEXT.calloc(1).pDescriptorTypes(this.mutableDescriptorTypes);
        this.mutableDescriptorInfo = VkMutableDescriptorTypeCreateInfoEXT.calloc().sType(VK_STRUCTURE_TYPE_MUTABLE_DESCRIPTOR_TYPE_CREATE_INFO_EXT).pMutableDescriptorTypeLists(this.mutableDescriptorLists);

        // TODO: make group by type
        this.resourceDescriptorSetBindings = VkDescriptorSetLayoutBinding.calloc(1).binding(0).stageFlags(VK_SHADER_STAGE_ALL).descriptorType(VK_DESCRIPTOR_TYPE_MUTABLE_EXT).descriptorCount(1024);
        this.resourceDescriptorSetBindingFlags = memAllocInt(1).put(0, VK_DESCRIPTOR_BINDING_PARTIALLY_BOUND_BIT | VK_DESCRIPTOR_BINDING_UPDATE_UNUSED_WHILE_PENDING_BIT);
        this.resourceDescriptorSetLayoutCreateInfoBindingFlags = VkDescriptorSetLayoutBindingFlagsCreateInfoEXT.calloc().sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_BINDING_FLAGS_CREATE_INFO).pNext(this.mutableDescriptorInfo.address()).pBindingFlags(this.resourceDescriptorSetBindingFlags);
        this.resourceDescriptorSetLayoutCreateInfo = VkDescriptorSetLayoutCreateInfo.calloc().flags(VK_DESCRIPTOR_SET_LAYOUT_CREATE_DESCRIPTOR_BUFFER_BIT_EXT).sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO).pNext(this.resourceDescriptorSetLayoutCreateInfoBindingFlags.address()).pBindings(this.resourceDescriptorSetBindings);
        vkCreateDescriptorSetLayout(deviceObj.device, this.resourceDescriptorSetLayoutCreateInfo, null, memLongBuffer(memAddress(this.descriptorLayout, 0), 1));

        // TODO: make group by type
        this.samplerDescriptorSetBindings = VkDescriptorSetLayoutBinding.calloc(1).binding(0).stageFlags(VK_SHADER_STAGE_ALL).descriptorType(VK_DESCRIPTOR_TYPE_SAMPLER).descriptorCount(256);
        this.samplerDescriptorSetBindingFlags = memAllocInt(1).put(0, VK_DESCRIPTOR_BINDING_PARTIALLY_BOUND_BIT | VK_DESCRIPTOR_BINDING_UPDATE_UNUSED_WHILE_PENDING_BIT);
        this.samplerDescriptorSetLayoutCreateInfoBindingFlags = VkDescriptorSetLayoutBindingFlagsCreateInfoEXT.calloc().sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_BINDING_FLAGS_CREATE_INFO).pBindingFlags(this.samplerDescriptorSetBindingFlags);
        this.samplerDescriptorSetLayoutCreateInfo = VkDescriptorSetLayoutCreateInfo.calloc().flags(VK_DESCRIPTOR_SET_LAYOUT_CREATE_DESCRIPTOR_BUFFER_BIT_EXT).sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO).pNext(this.samplerDescriptorSetLayoutCreateInfoBindingFlags.address()).pBindings(this.samplerDescriptorSetBindings);
        vkCreateDescriptorSetLayout(deviceObj.device, this.samplerDescriptorSetLayoutCreateInfo, null, memLongBuffer(memAddress(this.descriptorLayout, 1), 1));

        // TODO: make group by type
        this.uniformDescriptorSetBindings = VkDescriptorSetLayoutBinding.calloc(1).binding(0).stageFlags(VK_SHADER_STAGE_ALL).descriptorType(VK_DESCRIPTOR_TYPE_INLINE_UNIFORM_BLOCK).descriptorCount((int) this.uniformBufferSize);
        this.uniformDescriptorSetBindingFlags = memAllocInt(1).put(0, VK_DESCRIPTOR_BINDING_PARTIALLY_BOUND_BIT | VK_DESCRIPTOR_BINDING_UPDATE_UNUSED_WHILE_PENDING_BIT);
        this.uniformDescriptorSetLayoutCreateInfoBindingFlags = VkDescriptorSetLayoutBindingFlagsCreateInfoEXT.calloc().sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_BINDING_FLAGS_CREATE_INFO).pBindingFlags(this.uniformDescriptorSetBindingFlags);
        this.uniformDescriptorSetLayoutCreateInfo = VkDescriptorSetLayoutCreateInfo.calloc().flags(VK_DESCRIPTOR_SET_LAYOUT_CREATE_DESCRIPTOR_BUFFER_BIT_EXT).sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO).pNext(this.uniformDescriptorSetLayoutCreateInfoBindingFlags.address()).pBindings(this.uniformDescriptorSetBindings);
        vkCreateDescriptorSetLayout(deviceObj.device, this.uniformDescriptorSetLayoutCreateInfo, null, memLongBuffer(memAddress(this.descriptorLayout, 2), 1));

        // TODO: make group by type
        this.pipelineDescriptorSetBindings = VkDescriptorSetLayoutBinding.calloc(1).binding(0).stageFlags(VK_SHADER_STAGE_ALL).descriptorType(VK_DESCRIPTOR_TYPE_INLINE_UNIFORM_BLOCK).descriptorCount((int) 512);
        this.pipelineDescriptorSetBindingFlags = memAllocInt(1).put(0, VK_DESCRIPTOR_BINDING_PARTIALLY_BOUND_BIT | VK_DESCRIPTOR_BINDING_UPDATE_UNUSED_WHILE_PENDING_BIT);
        this.pipelineDescriptorSetLayoutCreateInfoBindingFlags = VkDescriptorSetLayoutBindingFlagsCreateInfoEXT.calloc().sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_BINDING_FLAGS_CREATE_INFO).pBindingFlags(this.pipelineDescriptorSetBindingFlags);
        this.pipelineDescriptorSetLayoutCreateInfo = VkDescriptorSetLayoutCreateInfo.calloc().flags(VK_DESCRIPTOR_SET_LAYOUT_CREATE_DESCRIPTOR_BUFFER_BIT_EXT).sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO).pNext(this.pipelineDescriptorSetLayoutCreateInfoBindingFlags.address()).pBindings(this.pipelineDescriptorSetBindings);
        vkCreateDescriptorSetLayout(deviceObj.device, this.pipelineDescriptorSetLayoutCreateInfo, null, memLongBuffer(memAddress(this.descriptorLayout, 3), 1));

        //
        this.pConstRange = VkPushConstantRange.calloc(1).stageFlags(VK_SHADER_STAGE_ALL).offset(0).size(256);
        vkCreatePipelineLayout(deviceObj.device, VkPipelineLayoutCreateInfo.calloc().sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO).pSetLayouts(memLongBuffer(memAddress(this.descriptorLayout), 4)).pPushConstantRanges(this.pConstRange), null, memLongBuffer(memAddress((this.handle = new Handle("PipelineLayout")).ptr()), 1));
        deviceObj.handleMap.put(this.handle, this);

        //
        this.offsets = memAllocLong(4).put(0, 0).put(1, 0).put(2, 0).put(3, 0);
        this.sizes = memAllocLong(4).put(0, 0).put(1, 0).put(2, 0).put(3, 0);

        //
        for (int i = 0; i < 4; i++) {
            vkGetDescriptorSetLayoutSizeEXT(deviceObj.device, this.descriptorLayout.get(i), memSlice(sizes, i, 1));
            vkGetDescriptorSetLayoutBindingOffsetEXT(deviceObj.device, this.descriptorLayout.get(i), 0, memSlice(offsets, i, 1));
        }

        //
        this.resources = new OutstandingArray<VkDescriptorImageInfo>();
        this.samplers = new OutstandingArray<LongBuffer>();

        //
        this.resourceDescriptorBuffer = new BufferObj(this.base, new BufferCInfo(){{
            size = sizes.get(0);
            usage = VK_BUFFER_USAGE_RESOURCE_DESCRIPTOR_BUFFER_BIT_EXT;
            memoryAllocator = cInfo.memoryAllocator;
            memoryAllocationInfo = new MemoryAllocationCInfo(){{
                isHost = true;
                isDevice = true;
            }};
        }});
        this.samplerDescriptorBuffer = new BufferObj(this.base, new BufferCInfo(){{
            size = sizes.get(1);
            usage = VK_BUFFER_USAGE_SAMPLER_DESCRIPTOR_BUFFER_BIT_EXT;
            memoryAllocator = cInfo.memoryAllocator;
            memoryAllocationInfo = new MemoryAllocationCInfo(){{
                isHost = true;
                isDevice = true;
            }};
        }});
        this.uniformDescriptorBuffer = new BufferObj(this.base, new BufferCInfo() {{
            size = uniformBufferSize;
            usage = VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT | VK_BUFFER_USAGE_RESOURCE_DESCRIPTOR_BUFFER_BIT_EXT;
            memoryAllocator = cInfo.memoryAllocator;
            memoryAllocationInfo = new MemoryAllocationCInfo(){{
                isHost = false;
                isDevice = true;
            }};
        }});

        //
        this.writeDescriptors();
    }

    //
    public PipelineLayoutObj writeDescriptors () {
        //
        var deviceObj = (DeviceObj) BasicObj.globalHandleMap.get(base.get());
        var physicalDeviceObj = (PhysicalDeviceObj)BasicObj.globalHandleMap.get(deviceObj.base.get());

        //
        var RSIZE = (int) Math.max(physicalDeviceObj.deviceDescriptorBufferProperties.storageImageDescriptorSize(), physicalDeviceObj.deviceDescriptorBufferProperties.sampledImageDescriptorSize());
        var SSIZE = (int) physicalDeviceObj.deviceDescriptorBufferProperties.samplerDescriptorSize();

        //
        ByteBuffer SMAP = this. samplerDescriptorBuffer.map(sizes.get(1), 0);
        ByteBuffer RMAP = this.resourceDescriptorBuffer.map(sizes.get(0), 0);

        //
        for (var I=0;I<Math.min(this.resources.size(), 1024);I++) {
            if (this.resources.get(I) != null && this.resources.get(I).imageView() != 0) {
                vkGetDescriptorEXT(deviceObj.device, VkDescriptorGetInfoEXT.calloc()
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_GET_INFO_EXT)
                    .type(((ImageViewCInfo) deviceObj.handleMap.get(new Handle("ImageView", this.resources.get(I).imageView())).cInfo).type == "storage" ? VK_DESCRIPTOR_TYPE_STORAGE_IMAGE : VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE)
                    .data(VkDescriptorDataEXT.calloc().pSampledImage(this.resources.get(I))), RMAP.slice((int) (this.offsets.get(0) + RSIZE * I), RSIZE));
            }
        }

        //
        for (var I=0;I<Math.min(this.samplers.size(), 256);I++) {
            if (this.samplers.get(I) != null && this.samplers.get(I).get(0) != 0) {
                vkGetDescriptorEXT(deviceObj.device, VkDescriptorGetInfoEXT.calloc()
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_GET_INFO_EXT)
                    .type(VK_DESCRIPTOR_TYPE_SAMPLER)
                    .data(VkDescriptorDataEXT.calloc().pSampler(this.samplers.get(I))), SMAP.slice((int) (this.offsets.get(1) + SSIZE * I), SSIZE));
            }
        }

        //
        this. samplerDescriptorBuffer.unmap();
        this.resourceDescriptorBuffer.unmap();

        //
        return this;
    }

    //
    public PipelineLayoutObj cmdBindBuffers(VkCommandBuffer cmdBuf, int pipelineBindPoint, long uniformBufferHandle) {
        var deviceObj = (DeviceObj) BasicObj.globalHandleMap.get(base.get());
        var physicalDeviceObj = (PhysicalDeviceObj)BasicObj.globalHandleMap.get(deviceObj.base.get());
        var uniformBuffer = uniformBufferHandle != 0 ? (BufferObj)deviceObj.handleMap.get(new Handle("Buffer", uniformBufferHandle)) : null;

        // TODO: better binding system
        var bufferBindings = VkDescriptorBufferBindingInfoEXT.calloc(uniformBuffer != null ? 4 : 3).sType(VK_STRUCTURE_TYPE_DESCRIPTOR_BUFFER_BINDING_INFO_EXT);
        bufferBindings.get(0).sType(VK_STRUCTURE_TYPE_DESCRIPTOR_BUFFER_BINDING_INFO_EXT).address$(this.resourceDescriptorBuffer.getDeviceAddress()).usage(VK_BUFFER_USAGE_RESOURCE_DESCRIPTOR_BUFFER_BIT_EXT);
        bufferBindings.get(1).sType(VK_STRUCTURE_TYPE_DESCRIPTOR_BUFFER_BINDING_INFO_EXT).address$(this.samplerDescriptorBuffer.getDeviceAddress()).usage(VK_BUFFER_USAGE_SAMPLER_DESCRIPTOR_BUFFER_BIT_EXT);
        bufferBindings.get(2).sType(VK_STRUCTURE_TYPE_DESCRIPTOR_BUFFER_BINDING_INFO_EXT).address$(this.uniformDescriptorBuffer.getDeviceAddress()).usage(VK_BUFFER_USAGE_RESOURCE_DESCRIPTOR_BUFFER_BIT_EXT);

        //
        if (uniformBuffer != null) {
            bufferBindings.get(3).sType(VK_STRUCTURE_TYPE_DESCRIPTOR_BUFFER_BINDING_INFO_EXT).address$(uniformBuffer != null ? uniformBuffer.getDeviceAddress() : 0L).usage(VK_BUFFER_USAGE_RESOURCE_DESCRIPTOR_BUFFER_BIT_EXT);
        }

        //
        IntBuffer bufferIndices = memAllocInt(4).put(0, 0).put(1,1).put(2,2).put(3, uniformBuffer != null ? 3 : 2);
        LongBuffer offsets = memAllocLong(4).put(0, 0).put(1, 0).put(2, 0).put(3, 0);

        //
        vkCmdBindDescriptorBuffersEXT(cmdBuf, bufferBindings);
        vkCmdSetDescriptorBufferOffsetsEXT(cmdBuf, pipelineBindPoint, this.handle.get(), 0, bufferIndices, offsets);

        //
        return this;
    }

}
