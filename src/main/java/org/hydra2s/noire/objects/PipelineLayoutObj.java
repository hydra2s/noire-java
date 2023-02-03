package org.hydra2s.noire.objects;

//

import org.hydra2s.noire.descriptors.BufferCInfo;
import org.hydra2s.noire.descriptors.ImageViewCInfo;
import org.hydra2s.noire.descriptors.MemoryAllocationCInfo;
import org.hydra2s.noire.descriptors.PipelineLayoutCInfo;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;

import static org.hydra2s.noire.descriptors.UtilsCInfo.vkCheckStatus;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.EXTDescriptorBuffer.*;
import static org.lwjgl.vulkan.EXTGraphicsPipelineLibrary.VK_PIPELINE_LAYOUT_CREATE_INDEPENDENT_SETS_BIT_EXT;
import static org.lwjgl.vulkan.EXTMutableDescriptorType.VK_DESCRIPTOR_TYPE_MUTABLE_EXT;
import static org.lwjgl.vulkan.EXTMutableDescriptorType.VK_STRUCTURE_TYPE_MUTABLE_DESCRIPTOR_TYPE_CREATE_INFO_EXT;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK12.*;
import static org.lwjgl.vulkan.VK13.VK_DESCRIPTOR_TYPE_INLINE_UNIFORM_BLOCK;
import static org.lwjgl.vulkan.VK13.VK_PIPELINE_CACHE_CREATE_EXTERNALLY_SYNCHRONIZED_BIT;

// TODO: fallback into traditional binding model!
public class PipelineLayoutObj extends BasicObj  {
    // TODO: exact descriptor types
    public static class MutableTypeInfo {
        public VkMutableDescriptorTypeCreateInfoEXT createInfo = null;
        public VkMutableDescriptorTypeListEXT.Buffer descriptorTypeLists = null;
        public IntBuffer descriptorTypes = memAllocInt(1).put(0, 0);

        //
        public MutableTypeInfo() {
            this.descriptorTypes = memAllocInt(2).put(0, VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE).put(1, VK_DESCRIPTOR_TYPE_STORAGE_IMAGE);
            this.descriptorTypeLists = VkMutableDescriptorTypeListEXT.calloc(1).pDescriptorTypes(this.descriptorTypes);
            this.createInfo = VkMutableDescriptorTypeCreateInfoEXT.calloc().sType(VK_STRUCTURE_TYPE_MUTABLE_DESCRIPTOR_TYPE_CREATE_INFO_EXT).pMutableDescriptorTypeLists(this.descriptorTypeLists);
        }
    }

    //
    public static final boolean useLegacyBindingSystem = true;

    //
    public static class DescriptorSetLayoutInfo {
        public MutableTypeInfo mutableType = null;

        //
        public VkDescriptorSetLayoutCreateInfo createInfo = null;
        public VkDescriptorSetLayoutBindingFlagsCreateInfoEXT createInfoBindingFlags = null;
        public VkDescriptorSetLayoutBinding.Buffer bindings = null;
        public IntBuffer bindingFlags = memAllocInt(1).put(0, 0);

        //
        public DescriptorSetLayoutInfo(int descriptorType, int bindingCount) {
            this.bindings = VkDescriptorSetLayoutBinding.calloc(1).binding(0).stageFlags(VK_SHADER_STAGE_ALL).descriptorType(descriptorType).descriptorCount(bindingCount);
            this.bindingFlags = memAllocInt(1).put(0, VK_DESCRIPTOR_BINDING_PARTIALLY_BOUND_BIT | VK_DESCRIPTOR_BINDING_UPDATE_UNUSED_WHILE_PENDING_BIT);
            this.createInfoBindingFlags = VkDescriptorSetLayoutBindingFlagsCreateInfoEXT.calloc().sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_BINDING_FLAGS_CREATE_INFO).pBindingFlags(this.bindingFlags);
            this.createInfo = VkDescriptorSetLayoutCreateInfo.calloc().flags((useLegacyBindingSystem?VK_DESCRIPTOR_SET_LAYOUT_CREATE_UPDATE_AFTER_BIND_POOL_BIT:VK_DESCRIPTOR_SET_LAYOUT_CREATE_DESCRIPTOR_BUFFER_BIT_EXT)).sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO).pNext(this.createInfoBindingFlags.address()).pBindings(this.bindings);
        }

        //
        public DescriptorSetLayoutInfo(int descriptorType, int bindingCount, MutableTypeInfo mutableType) {
            this.mutableType = mutableType;
            this.bindings = VkDescriptorSetLayoutBinding.calloc(1).binding(0).stageFlags(VK_SHADER_STAGE_ALL).descriptorType(descriptorType).descriptorCount(bindingCount);
            this.bindingFlags = memAllocInt(1).put(0, VK_DESCRIPTOR_BINDING_PARTIALLY_BOUND_BIT | VK_DESCRIPTOR_BINDING_UPDATE_UNUSED_WHILE_PENDING_BIT);
            this.createInfoBindingFlags = VkDescriptorSetLayoutBindingFlagsCreateInfoEXT.calloc().sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_BINDING_FLAGS_CREATE_INFO).pNext(mutableType.createInfo.address()).pBindingFlags(this.bindingFlags);
            this.createInfo = VkDescriptorSetLayoutCreateInfo.calloc().flags(useLegacyBindingSystem?VK_DESCRIPTOR_SET_LAYOUT_CREATE_UPDATE_AFTER_BIND_POOL_BIT:VK_DESCRIPTOR_SET_LAYOUT_CREATE_DESCRIPTOR_BUFFER_BIT_EXT).sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO).pNext(this.createInfoBindingFlags.address()).pBindings(this.bindings);
        }

        //
        public DescriptorSetLayoutInfo createDescriptorSetLayout(VkDevice device, LongBuffer where) {
            vkCreateDescriptorSetLayout(device, this.createInfo, null, where);
            return this;
        }
    }

    //
    protected VkPushConstantRange.Buffer pConstRange = null;

    //
    protected MutableTypeInfo resourceMutableTypes = null;
    protected ArrayList<DescriptorSetLayoutInfo> descriptorSetLayoutsInfo = null;
    protected LongBuffer descriptorSetLayouts = null;

    //
    protected LongBuffer offsets = memAllocLong(4);
    protected LongBuffer sizes = memAllocLong(4);

    //
    static final protected long uniformBufferSize = 8192;

    //
    protected BufferObj resourceDescriptorBuffer = null;
    protected BufferObj samplerDescriptorBuffer = null;

    //
    protected LongBuffer descriptorSets = null;
    public BufferObj uniformDescriptorBuffer = null;
    protected LongBuffer uniformDescriptorSet = null;

    //
    public LongBuffer pipelineCache = null;
    public LongBuffer descriptorPool = null;
    public VkDescriptorPoolCreateInfo descriptorPoolCreateInfo = null;
    public VkDescriptorPoolSize.Buffer descriptorPoolSizes = null;

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
    public static final int resourceCount = 4096;
    public static final int samplerCount = 1024;

    //
    public PipelineLayoutObj(Handle base, PipelineLayoutCInfo cInfo) {
        super(base, cInfo);

        //
        this.resourceMutableTypes = new MutableTypeInfo();

        //
        if (useLegacyBindingSystem) {
            this.descriptorPoolSizes = VkDescriptorPoolSize.calloc(5);
            this.descriptorPoolSizes.get(0)
                .type(VK_DESCRIPTOR_TYPE_MUTABLE_EXT)
                .descriptorCount(resourceCount);

            this.descriptorPoolSizes.get(1)
                .type(VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE)
                .descriptorCount(resourceCount);

            this.descriptorPoolSizes.get(2)
                .type(VK_DESCRIPTOR_TYPE_STORAGE_IMAGE)
                .descriptorCount(resourceCount);

            this.descriptorPoolSizes.get(3)
                .type(VK_DESCRIPTOR_TYPE_SAMPLER)
                .descriptorCount(samplerCount);

            this.descriptorPoolSizes.get(4)
                .type(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                .descriptorCount(64);

            this.descriptorPoolCreateInfo =
                VkDescriptorPoolCreateInfo.calloc()
                    .pNext(this.resourceMutableTypes.createInfo.address())
                    .flags(VK_DESCRIPTOR_POOL_CREATE_UPDATE_AFTER_BIND_BIT)
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
                    .pPoolSizes(this.descriptorPoolSizes)
                    .maxSets(128);

            //
            vkCheckStatus(vkCreateDescriptorPool(deviceObj.device, this.descriptorPoolCreateInfo, null, this.descriptorPool = memAllocLong(1)));
        }

        //
        this.resources = new OutstandingArray<VkDescriptorImageInfo>();
        this.samplers = new OutstandingArray<LongBuffer>();
        this.descriptorSetLayouts = memAllocLong(3);

        //
        var resourceDescriptorSetLayout = new DescriptorSetLayoutInfo(VK_DESCRIPTOR_TYPE_MUTABLE_EXT, resourceCount, this.resourceMutableTypes);
        var samplerDescriptorSetLayout = new DescriptorSetLayoutInfo(VK_DESCRIPTOR_TYPE_SAMPLER, samplerCount);
        var uniformDescriptorSetLayout = new DescriptorSetLayoutInfo(useLegacyBindingSystem ? VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER : VK_DESCRIPTOR_TYPE_INLINE_UNIFORM_BLOCK, 512);
        //var pipelineDescriptorSetLayout = new DescriptorSetLayoutInfo(useLegacyBindingSystem ? VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER : VK_DESCRIPTOR_TYPE_INLINE_UNIFORM_BLOCK, 512);

        //
        this.descriptorSetLayoutsInfo = new ArrayList<>(){{
            add(resourceDescriptorSetLayout);
            add(samplerDescriptorSetLayout);
            add(uniformDescriptorSetLayout);
            //add(pipelineDescriptorSetLayout);
        }};

        //
        var dS = this.descriptorSetLayoutsInfo.size();
        for (var I=0;I<dS;I++) {
            this.descriptorSetLayoutsInfo.get(I).createDescriptorSetLayout(deviceObj.device, memSlice(this.descriptorSetLayouts, I, 1));
        }

        //
        this.resources = new OutstandingArray<VkDescriptorImageInfo>();
        this.samplers = new OutstandingArray<LongBuffer>();
        this.pConstRange = VkPushConstantRange.calloc(1).stageFlags(VK_SHADER_STAGE_ALL).offset(0).size(256);
        vkCheckStatus(vkCreatePipelineCache(deviceObj.device, VkPipelineCacheCreateInfo.calloc().sType(VK_STRUCTURE_TYPE_PIPELINE_CACHE_CREATE_INFO).flags(VK_PIPELINE_CACHE_CREATE_EXTERNALLY_SYNCHRONIZED_BIT ), null, this.pipelineCache = memAllocLong(1)));
        vkCheckStatus(vkCreatePipelineLayout(deviceObj.device, VkPipelineLayoutCreateInfo.calloc().flags(VK_PIPELINE_LAYOUT_CREATE_INDEPENDENT_SETS_BIT_EXT).sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO).pSetLayouts(this.descriptorSetLayouts).pPushConstantRanges(this.pConstRange), null, memLongBuffer(memAddress((this.handle = new Handle("PipelineLayout")).ptr()), 1)));
        deviceObj.handleMap.put$(this.handle, this);

        //
        this.uniformDescriptorBuffer = new BufferObj(this.base, new BufferCInfo() {{
            size = uniformBufferSize;
            usage = VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT | VK_BUFFER_USAGE_RESOURCE_DESCRIPTOR_BUFFER_BIT_EXT;
            memoryAllocator = cInfo.memoryAllocator;
            memoryAllocationInfo = new MemoryAllocationCInfo() {{
                isHost = false;
                isDevice = true;
            }};
        }});

        //
        this.uniformDescriptorSet = memAllocLong(1);

        //
        if (!useLegacyBindingSystem) {
            this.uniformDescriptorSet.put(0, this.uniformDescriptorBuffer.getDeviceAddress());

            //
            this.offsets = memAllocLong(dS).put(0, 0).put(1, 0).put(2, 0);//.put(3, 0);
            this.sizes = memAllocLong(dS).put(0, 0).put(1, 0).put(2, 0);//.put(3, 0);

            //
            for (var I = 0; I < dS; I++) {
                vkGetDescriptorSetLayoutSizeEXT(deviceObj.device, this.descriptorSetLayouts.get(I), memSlice(sizes, I, 1));
                vkGetDescriptorSetLayoutBindingOffsetEXT(deviceObj.device, this.descriptorSetLayouts.get(I), 0, memSlice(offsets, I, 1));
            }

            //
            this.resourceDescriptorBuffer = new BufferObj(this.base, new BufferCInfo() {{
                size = sizes.get(0);
                usage = VK_BUFFER_USAGE_RESOURCE_DESCRIPTOR_BUFFER_BIT_EXT;
                memoryAllocator = cInfo.memoryAllocator;
                memoryAllocationInfo = new MemoryAllocationCInfo() {{
                    isHost = true;
                    isDevice = true;
                }};
            }});
            this.samplerDescriptorBuffer = new BufferObj(this.base, new BufferCInfo() {{
                size = sizes.get(1);
                usage = VK_BUFFER_USAGE_SAMPLER_DESCRIPTOR_BUFFER_BIT_EXT;
                memoryAllocator = cInfo.memoryAllocator;
                memoryAllocationInfo = new MemoryAllocationCInfo() {{
                    isHost = true;
                    isDevice = true;
                }};
            }});

        } else {
            this.descriptorSets = memAllocLong(3);

            //
            vkCheckStatus(vkAllocateDescriptorSets(deviceObj.device,
                VkDescriptorSetAllocateInfo.calloc()
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
                    .pSetLayouts(this.descriptorSetLayouts)
                    .descriptorPool(this.descriptorPool.get(0))
                , this.descriptorSets));

            //
            this.uniformDescriptorSet.put(0, this.descriptorSets.get(2));
        }

        //
        this.writeDescriptors();
    }

    /*
    //
    public PipelineLayoutObj createDescriptorSetForUniformBuffer(BufferObj uniformBufferObj, LongBuffer descriptorSet) {
        if (useLegacyBindingSystem) {
            if (descriptorSet.get(0) == 0L) {
                vkAllocateDescriptorSets(deviceObj.device,
                    VkDescriptorSetAllocateInfo.calloc()
                        .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
                        .pSetLayouts(memSlice(this.descriptorSetLayouts, 3, 1))
                        .descriptorPool(this.descriptorPool.get(0))
                    , descriptorSet);
            }

            //
            VkDescriptorBufferInfo.Buffer uniformWrite = VkDescriptorBufferInfo.calloc(1);
            uniformWrite.get(0).set(uniformBufferObj.getBufferRange());

            //
            var writeDescriptorSets = VkWriteDescriptorSet.calloc(1);
            writeDescriptorSets.get(0)
                .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                .descriptorCount(1)
                .dstArrayElement(0)
                .dstBinding(0)
                .dstSet(descriptorSet.get(0))
                .pBufferInfo(uniformWrite);

            //
            vkUpdateDescriptorSets(deviceObj.device, writeDescriptorSets, null);
        } else {
            descriptorSet.put(0, uniformBufferObj.getDeviceAddress());
        }

        return this;
    }

    //
    public PipelineLayoutObj createDescriptorSetForUniformBuffer(VirtualMutableBufferHeap.VirtualMutableBufferObj uniformBufferObj, LongBuffer descriptorSet) {
        if (useLegacyBindingSystem) {
            if (descriptorSet.get(0) == 0L) {
                vkAllocateDescriptorSets(deviceObj.device,
                    VkDescriptorSetAllocateInfo.calloc()
                        .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
                        .pSetLayouts(memSlice(this.descriptorSetLayouts, 3, 1))
                        .descriptorPool(this.descriptorPool.get(0))
                    , descriptorSet);
            }

            //
            VkDescriptorBufferInfo.Buffer uniformWrite = VkDescriptorBufferInfo.calloc(1);
            uniformWrite.get(0).set(uniformBufferObj.getBufferRange());

            //
            var writeDescriptorSets = VkWriteDescriptorSet.calloc(1);
            writeDescriptorSets.get(0)
                .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                .descriptorCount(1)
                .dstArrayElement(0)
                .dstBinding(0)
                .dstSet(descriptorSet.get(0))
                .pBufferInfo(uniformWrite);

            //
            vkUpdateDescriptorSets(deviceObj.device, writeDescriptorSets, null);
        } else {
            descriptorSet.put(0, uniformBufferObj.getBufferAddress());
        }

        return this;
    }*/

    //
    public PipelineLayoutObj deallocateDescriptorSets(ArrayList<Long> descriptorSets) {
        LongBuffer descriptorSetsBuf = memAllocLong(descriptorSets.size());
        for (var I=0;I<descriptorSets.size();I++) {
            descriptorSetsBuf.put(I, descriptorSets.get(I));
        }
        return deallocateDescriptorSets(descriptorSetsBuf);
    };

    //
    public PipelineLayoutObj deallocateDescriptorSets(LongBuffer descriptorSets) {
        if (useLegacyBindingSystem) {
            vkCheckStatus(vkFreeDescriptorSets(deviceObj.device, descriptorPool.get(0), descriptorSets));
        }
        return this;
    };

    //
    public PipelineLayoutObj writeDescriptors () {
        if (!useLegacyBindingSystem) {
            var RSIZE = (int) Math.max(physicalDeviceObj.properties.descriptorBuffer.storageImageDescriptorSize(), physicalDeviceObj.properties.descriptorBuffer.sampledImageDescriptorSize());
            var SSIZE = (int) physicalDeviceObj.properties.descriptorBuffer.samplerDescriptorSize();

            //
            ByteBuffer SMAP = this.samplerDescriptorBuffer.map(sizes.get(1), 0);
            ByteBuffer RMAP = this.resourceDescriptorBuffer.map(sizes.get(0), 0);

            //
            for (var I = 0; I < Math.min(this.resources.size(), resourceCount); I++) {
                if (this.resources.get(I) != null && this.resources.get(I).imageView() != 0) {
                    vkGetDescriptorEXT(deviceObj.device, VkDescriptorGetInfoEXT.calloc()
                        .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_GET_INFO_EXT)
                        .type(((ImageViewCInfo) deviceObj.handleMap.get(new Handle("ImageView", this.resources.get(I).imageView())).orElse(null).cInfo).type == "storage" ? VK_DESCRIPTOR_TYPE_STORAGE_IMAGE : VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE)
                        .data(VkDescriptorDataEXT.calloc().pSampledImage(this.resources.get(I))), RMAP.slice((int) (this.offsets.get(0) + RSIZE * I), RSIZE));
                }
            }

            //
            for (var I = 0; I < Math.min(this.samplers.size(), samplerCount); I++) {
                if (this.samplers.get(I) != null && this.samplers.get(I).get(0) != 0) {
                    vkGetDescriptorEXT(deviceObj.device, VkDescriptorGetInfoEXT.calloc()
                        .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_GET_INFO_EXT)
                        .type(VK_DESCRIPTOR_TYPE_SAMPLER)
                        .data(VkDescriptorDataEXT.calloc().pSampler(this.samplers.get(I))), SMAP.slice((int) (this.offsets.get(1) + SSIZE * I), SSIZE));
                }
            }

            //
            this.samplerDescriptorBuffer.unmap();
            this.resourceDescriptorBuffer.unmap();
        } else {
            // TODO: don't fill empty resources!
            var resourceDescriptorSets = VkWriteDescriptorSet.calloc(Math.min(this.resources.size(), resourceCount));
            var samplerDescriptorSets = VkWriteDescriptorSet.calloc(1);
            var uniformDescriptorSets = VkWriteDescriptorSet.calloc(1);

            //
            var resourceInfo = VkDescriptorImageInfo.calloc(Math.min(this.resources.size(), resourceCount));
            var samplerInfo = VkDescriptorImageInfo.calloc(Math.min(this.samplers.size(), samplerCount));

            // TODO: don't fill empty resources!
            for (var I = 0; I < Math.min(this.resources.size(), resourceCount); I++) {
                if (this.resources.get(I) != null && this.resources.get(I).imageView() != 0) {
                    var imageViewInfo = (ImageViewCInfo) deviceObj.handleMap.get(new Handle("ImageView", this.resources.get(I).imageView())).orElse(null).cInfo;
                    resourceInfo.get(I).set(this.resources.get(I));
                    resourceDescriptorSets.get(I)
                        .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                        .descriptorType(imageViewInfo.type == "storage" ? VK_DESCRIPTOR_TYPE_STORAGE_IMAGE : VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE)
                        .descriptorCount(1)
                        .dstArrayElement(I)
                        .dstBinding(0)
                        .dstSet(this.descriptorSets.get(0))
                        .pImageInfo(resourceInfo.slice(I, 1));
                }
            }

            //
            for (var I = 0; I < Math.min(this.samplers.size(), samplerCount); I++) {
                if (this.samplers.get(I) != null && this.samplers.get(I).get(0) != 0) {
                    samplerInfo.get(I).sampler(this.samplers.get(I).get(0));
                }
            }

            //
            samplerDescriptorSets.get(0)
                .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                .descriptorType(VK_DESCRIPTOR_TYPE_SAMPLER)
                .descriptorCount(Math.min(this.samplers.size(), samplerCount))
                .dstArrayElement(0)
                .dstBinding(0)
                .dstSet(this.descriptorSets.get(1))
                .pImageInfo(samplerInfo);

            //
            VkDescriptorBufferInfo.Buffer uniformWrite = VkDescriptorBufferInfo.calloc(1);
            uniformWrite.get(0).set(this.uniformDescriptorBuffer.getBufferRange());
            uniformDescriptorSets.get(0)
                .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                .descriptorCount(1)
                .dstArrayElement(0)
                .dstBinding(0)
                .dstSet(this.descriptorSets.get(2))
                .pBufferInfo(uniformWrite);

            //
            if (this.resources.size() > 0) {
                vkUpdateDescriptorSets(deviceObj.device, resourceDescriptorSets, null);
            }
            if (this.samplers.size() > 0) {
                vkUpdateDescriptorSets(deviceObj.device, samplerDescriptorSets, null);
            }
            vkUpdateDescriptorSets(deviceObj.device, uniformDescriptorSets, null);
        }

        //
        return this;
    }

    //
    public PipelineLayoutObj cmdBindBuffers(VkCommandBuffer cmdBuf, int pipelineBindPoint, LongBuffer uniformBufferAddress) {
        if (!useLegacyBindingSystem) {
            // TODO: better binding system
            //var bufferBindings = VkDescriptorBufferBindingInfoEXT.calloc(uniformBufferAddress != null ? 4 : 3).sType(VK_STRUCTURE_TYPE_DESCRIPTOR_BUFFER_BINDING_INFO_EXT);
            var bufferBindings = VkDescriptorBufferBindingInfoEXT.calloc(3).sType(VK_STRUCTURE_TYPE_DESCRIPTOR_BUFFER_BINDING_INFO_EXT);
            bufferBindings.get(0).sType(VK_STRUCTURE_TYPE_DESCRIPTOR_BUFFER_BINDING_INFO_EXT).address$(this.resourceDescriptorBuffer.getDeviceAddress()).usage(VK_BUFFER_USAGE_RESOURCE_DESCRIPTOR_BUFFER_BIT_EXT);
            bufferBindings.get(1).sType(VK_STRUCTURE_TYPE_DESCRIPTOR_BUFFER_BINDING_INFO_EXT).address$(this.samplerDescriptorBuffer.getDeviceAddress()).usage(VK_BUFFER_USAGE_SAMPLER_DESCRIPTOR_BUFFER_BIT_EXT);
            bufferBindings.get(2).sType(VK_STRUCTURE_TYPE_DESCRIPTOR_BUFFER_BINDING_INFO_EXT).address$(this.uniformDescriptorBuffer.getDeviceAddress()).usage(VK_BUFFER_USAGE_RESOURCE_DESCRIPTOR_BUFFER_BIT_EXT);

            //
            //if (uniformBufferAddress != null) {
                //bufferBindings.get(3).sType(VK_STRUCTURE_TYPE_DESCRIPTOR_BUFFER_BINDING_INFO_EXT).address$(uniformBufferAddress.get(0)).usage(VK_BUFFER_USAGE_RESOURCE_DESCRIPTOR_BUFFER_BIT_EXT);
            //}

            //
            IntBuffer bufferIndices = memAllocInt(3).put(0, 0).put(1, 1).put(2, 2);//.put(3, uniformBufferAddress != null ? 3 : 2);
            LongBuffer offsets = memAllocLong(3).put(0, 0).put(1, 0).put(2, 0);//.put(3, 0);

            //
            vkCmdBindDescriptorBuffersEXT(cmdBuf, bufferBindings);
            vkCmdSetDescriptorBufferOffsetsEXT(cmdBuf, pipelineBindPoint, this.handle.get(), 0, bufferIndices, offsets);
        } else {
            // TODO: test descriptor set bound
            //this.descriptorSets.put(3, uniformBufferAddress != null ? uniformBufferAddress.get(0) : this.uniformDescriptorSet.get(0));
            vkCmdBindDescriptorSets(cmdBuf, pipelineBindPoint, this.handle.get(), 0, this.descriptorSets, null);
        }

        //
        return this;
    }

}
