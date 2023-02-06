package org.hydra2s.noire.objects;

//

import org.hydra2s.noire.descriptors.*;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;

import static org.hydra2s.noire.descriptors.UtilsCInfo.vkCheckStatus;
import static org.lwjgl.BufferUtils.createIntBuffer;
import static org.lwjgl.BufferUtils.createLongBuffer;
import static org.lwjgl.system.MemoryStack.stackPush;
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
        public IntBuffer descriptorTypes = createIntBuffer(1).put(0, 0);

        //
        public MutableTypeInfo() {
            this.descriptorTypes = createIntBuffer(2).put(0, VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE).put(1, VK_DESCRIPTOR_TYPE_STORAGE_IMAGE);
            this.descriptorTypeLists = VkMutableDescriptorTypeListEXT.create(1).pDescriptorTypes(this.descriptorTypes);
            this.createInfo = VkMutableDescriptorTypeCreateInfoEXT.create().sType(VK_STRUCTURE_TYPE_MUTABLE_DESCRIPTOR_TYPE_CREATE_INFO_EXT).pMutableDescriptorTypeLists(this.descriptorTypeLists);
        }
    }

    //
    public static final boolean useLegacyBindingSystem = false;

    //
    public static class DescriptorSetLayoutInfo {
        public MutableTypeInfo mutableType = null;

        //
        public VkDescriptorSetLayoutCreateInfo createInfo = null;
        public VkDescriptorSetLayoutBindingFlagsCreateInfoEXT createInfoBindingFlags = null;
        public VkDescriptorSetLayoutBinding.Buffer bindings = null;
        public IntBuffer bindingFlags = createIntBuffer(1).put(0, 0);

        //
        public DescriptorSetLayoutInfo(int descriptorType, int bindingCount) {
            this.bindings = VkDescriptorSetLayoutBinding.create(1).binding(0).stageFlags(VK_SHADER_STAGE_ALL).descriptorType(descriptorType).descriptorCount(bindingCount);
            this.bindingFlags = createIntBuffer(1).put(0, VK_DESCRIPTOR_BINDING_PARTIALLY_BOUND_BIT | VK_DESCRIPTOR_BINDING_UPDATE_UNUSED_WHILE_PENDING_BIT);
            this.createInfoBindingFlags = VkDescriptorSetLayoutBindingFlagsCreateInfoEXT.create().sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_BINDING_FLAGS_CREATE_INFO).pBindingFlags(this.bindingFlags);
            this.createInfo = VkDescriptorSetLayoutCreateInfo.create().flags((useLegacyBindingSystem?VK_DESCRIPTOR_SET_LAYOUT_CREATE_UPDATE_AFTER_BIND_POOL_BIT:VK_DESCRIPTOR_SET_LAYOUT_CREATE_DESCRIPTOR_BUFFER_BIT_EXT)).sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO).pNext(this.createInfoBindingFlags.address()).pBindings(this.bindings);
        }

        //
        public DescriptorSetLayoutInfo(int descriptorType, int bindingCount, MutableTypeInfo mutableType) {
            this.mutableType = mutableType;
            this.bindings = VkDescriptorSetLayoutBinding.create(1).binding(0).stageFlags(VK_SHADER_STAGE_ALL).descriptorType(descriptorType).descriptorCount(bindingCount);
            this.bindingFlags = createIntBuffer(1).put(0, VK_DESCRIPTOR_BINDING_PARTIALLY_BOUND_BIT | VK_DESCRIPTOR_BINDING_UPDATE_UNUSED_WHILE_PENDING_BIT);
            this.createInfoBindingFlags = VkDescriptorSetLayoutBindingFlagsCreateInfoEXT.create().sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_BINDING_FLAGS_CREATE_INFO).pNext(mutableType.createInfo.address()).pBindingFlags(this.bindingFlags);
            this.createInfo = VkDescriptorSetLayoutCreateInfo.create().flags(useLegacyBindingSystem?VK_DESCRIPTOR_SET_LAYOUT_CREATE_UPDATE_AFTER_BIND_POOL_BIT:VK_DESCRIPTOR_SET_LAYOUT_CREATE_DESCRIPTOR_BUFFER_BIT_EXT).sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO).pNext(this.createInfoBindingFlags.address()).pBindings(this.bindings);
        }

        //
        public DescriptorSetLayoutInfo createDescriptorSetLayout(VkDevice device, long[] where) {
            vkCreateDescriptorSetLayout(device, this.createInfo, null, where);
            return this;
        }
    }

    //
    protected VkPushConstantRange.Buffer pConstRange = null;

    //
    protected MutableTypeInfo resourceMutableTypes = null;
    protected ArrayList<DescriptorSetLayoutInfo> descriptorSetLayoutsInfo = null;
    protected long[] descriptorSetLayouts = {};

    //
    protected long[] offsets = {};
    protected long[] sizes = {};

    //
    static final protected long uniformBufferSize = 8192;

    //
    protected BufferObj resourceDescriptorBuffer = null;
    protected BufferObj samplerDescriptorBuffer = null;

    //
    protected long[] descriptorSets = {};
    public BufferObj uniformDescriptorBuffer = null;
    protected long[] uniformDescriptorSet = {};

    //
    public long[] pipelineCache = {};
    public long[] descriptorPool = {};
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
    public PipelineLayoutObj(UtilsCInfo.Handle base, PipelineLayoutCInfo cInfo) {
        super(base, cInfo);

        //
        this.resourceMutableTypes = new MutableTypeInfo();

        //
        if (useLegacyBindingSystem) {
            this.descriptorPoolSizes = VkDescriptorPoolSize.create(5);
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
                VkDescriptorPoolCreateInfo.create()
                    .pNext(this.resourceMutableTypes.createInfo.address())
                    .flags(VK_DESCRIPTOR_POOL_CREATE_UPDATE_AFTER_BIND_BIT)
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
                    .pPoolSizes(this.descriptorPoolSizes)
                    .maxSets(128);

            //
            vkCheckStatus(vkCreateDescriptorPool(deviceObj.device, this.descriptorPoolCreateInfo, null, this.descriptorPool = new long[]{0L}));
        }

        //
        this.resources = new OutstandingArray<VkDescriptorImageInfo>();
        this.samplers = new OutstandingArray<LongBuffer>();
        this.descriptorSetLayouts = new long[3];

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
        LongBuffer pLayouts = createLongBuffer(dS);
        for (var I=0;I<dS;I++) {
            long[] layout = new long[]{0L};
            this.descriptorSetLayoutsInfo.get(I).createDescriptorSetLayout(deviceObj.device, layout);
            this.descriptorSetLayouts[I] = layout[0];
            pLayouts.put(I, layout[0]);
        }

        //
        this.resources = new OutstandingArray<VkDescriptorImageInfo>();
        this.samplers = new OutstandingArray<LongBuffer>();
        this.pConstRange = VkPushConstantRange.create(1).stageFlags(VK_SHADER_STAGE_ALL).offset(0).size(256);
        vkCheckStatus(vkCreatePipelineCache(deviceObj.device, VkPipelineCacheCreateInfo.create().sType(VK_STRUCTURE_TYPE_PIPELINE_CACHE_CREATE_INFO).flags(VK_PIPELINE_CACHE_CREATE_EXTERNALLY_SYNCHRONIZED_BIT ), null, this.pipelineCache = new long[]{0L}));
        vkCheckStatus(vkCreatePipelineLayout(deviceObj.device, VkPipelineLayoutCreateInfo.create().flags(VK_PIPELINE_LAYOUT_CREATE_INDEPENDENT_SETS_BIT_EXT).sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO).pSetLayouts(pLayouts).pPushConstantRanges(this.pConstRange), null, (this.handle = new UtilsCInfo.Handle("PipelineLayout")).ptr()));
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
        this.uniformDescriptorSet = new long[]{0L};

        //
        if (!useLegacyBindingSystem) {
            this.uniformDescriptorSet[0] = this.uniformDescriptorBuffer.getDeviceAddress();

            //
            this.offsets = new long[3];//createLongBuffer(dS).put(0, 0).put(1, 0).put(2, 0);//.put(3, 0);
            this.sizes = new long[3];//createLongBuffer(dS).put(0, 0).put(1, 0).put(2, 0);//.put(3, 0);

            //
            for (var I = 0; I < dS; I++) {
                var size = new long[1]; vkGetDescriptorSetLayoutSizeEXT(deviceObj.device, this.descriptorSetLayouts[I], size); this.sizes[I] = size[0];
                var offset = new long[1]; vkGetDescriptorSetLayoutBindingOffsetEXT(deviceObj.device, this.descriptorSetLayouts[I], 0, offset); this.offsets[I] = offset[0];
            }

            //
            this.resourceDescriptorBuffer = new BufferObj(this.base, new BufferCInfo() {{
                size = sizes[0];
                usage = VK_BUFFER_USAGE_RESOURCE_DESCRIPTOR_BUFFER_BIT_EXT;
                memoryAllocator = cInfo.memoryAllocator;
                memoryAllocationInfo = new MemoryAllocationCInfo() {{
                    isHost = true;
                    isDevice = true;
                }};
            }});
            this.samplerDescriptorBuffer = new BufferObj(this.base, new BufferCInfo() {{
                size = sizes[1];
                usage = VK_BUFFER_USAGE_SAMPLER_DESCRIPTOR_BUFFER_BIT_EXT;
                memoryAllocator = cInfo.memoryAllocator;
                memoryAllocationInfo = new MemoryAllocationCInfo() {{
                    isHost = true;
                    isDevice = true;
                }};
            }});

        } else {
            this.descriptorSets = new long[3];

            //
            vkCheckStatus(vkAllocateDescriptorSets(deviceObj.device,
                VkDescriptorSetAllocateInfo.create()
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
                    .pSetLayouts(pLayouts)
                    .descriptorPool(this.descriptorPool[0])
                , this.descriptorSets));

            //
            this.uniformDescriptorSet[0] = this.descriptorSets[2];
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
                    VkDescriptorSetAllocateInfo.create()
                        .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
                        .pSetLayouts(memSlice(this.descriptorSetLayouts, 3, 1))
                        .descriptorPool(this.descriptorPool.get(0))
                    , descriptorSet);
            }

            //
            VkDescriptorBufferInfo.Buffer uniformWrite = VkDescriptorBufferInfo.create(1);
            uniformWrite.get(0).set(uniformBufferObj.getBufferRange());

            //
            var writeDescriptorSets = VkWriteDescriptorSet.create(1);
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
                    VkDescriptorSetAllocateInfo.create()
                        .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
                        .pSetLayouts(memSlice(this.descriptorSetLayouts, 3, 1))
                        .descriptorPool(this.descriptorPool.get(0))
                    , descriptorSet);
            }

            //
            VkDescriptorBufferInfo.Buffer uniformWrite = VkDescriptorBufferInfo.create(1);
            uniformWrite.get(0).set(uniformBufferObj.getBufferRange());

            //
            var writeDescriptorSets = VkWriteDescriptorSet.create(1);
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
    /*public PipelineLayoutObj deallocateDescriptorSets(ArrayList<Long> descriptorSets) {
        LongBuffer descriptorSetsBuf = createLongBuffer(descriptorSets.size());
        for (var I=0;I<descriptorSets.size();I++) {
            descriptorSetsBuf.put(I, descriptorSets.get(I));
        }
        return deallocateDescriptorSets(descriptorSetsBuf);
    };

    //
    public PipelineLayoutObj deallocateDescriptorSets(LongBuffer descriptorSets) {
        if (useLegacyBindingSystem) {
            vkCheckStatus(vkFreeDescriptorSets(deviceObj.device, descriptorPool[0], descriptorSets));
        }
        return this;
    };*/

    //
    public PipelineLayoutObj writeDescriptors () {
        if (!useLegacyBindingSystem) {
            var RSIZE = (int) Math.max(physicalDeviceObj.properties.descriptorBuffer.storageImageDescriptorSize(), physicalDeviceObj.properties.descriptorBuffer.sampledImageDescriptorSize());
            var SSIZE = (int) physicalDeviceObj.properties.descriptorBuffer.samplerDescriptorSize();

            //
            ByteBuffer SMAP = this.samplerDescriptorBuffer.map(sizes[1], 0);
            ByteBuffer RMAP = this.resourceDescriptorBuffer.map(sizes[0], 0);

            //
            for (var I = 0; I < Math.min(this.resources.size(), resourceCount); I++) {
                if (this.resources.get(I) != null && this.resources.get(I).imageView() != 0) {
                    vkGetDescriptorEXT(deviceObj.device, VkDescriptorGetInfoEXT.create()
                        .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_GET_INFO_EXT)
                        .type(((ImageViewCInfo) deviceObj.handleMap.get(new UtilsCInfo.Handle("ImageView", this.resources.get(I).imageView())).orElse(null).cInfo).type == "storage" ? VK_DESCRIPTOR_TYPE_STORAGE_IMAGE : VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE)
                        .data(VkDescriptorDataEXT.create().pSampledImage(this.resources.get(I))), RMAP.slice((int) (this.offsets[0] + RSIZE * I), RSIZE));
                }
            }

            //
            for (var I = 0; I < Math.min(this.samplers.size(), samplerCount); I++) {
                if (this.samplers.get(I) != null && this.samplers.get(I).get(0) != 0) {
                    vkGetDescriptorEXT(deviceObj.device, VkDescriptorGetInfoEXT.create()
                        .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_GET_INFO_EXT)
                        .type(VK_DESCRIPTOR_TYPE_SAMPLER)
                        .data(VkDescriptorDataEXT.create().pSampler(this.samplers.get(I))), SMAP.slice((int) (this.offsets[1] + SSIZE * I), SSIZE));
                }
            }

            //
            this.samplerDescriptorBuffer.unmap();
            this.resourceDescriptorBuffer.unmap();
        } else {
            // TODO: don't fill empty resources!
            var resourceDescriptorSets = VkWriteDescriptorSet.create(Math.min(this.resources.size(), resourceCount));
            var samplerDescriptorSets = VkWriteDescriptorSet.create(1);
            var uniformDescriptorSets = VkWriteDescriptorSet.create(1);

            //
            var resourceInfo = VkDescriptorImageInfo.create(Math.min(this.resources.size(), resourceCount));
            var samplerInfo = VkDescriptorImageInfo.create(Math.min(this.samplers.size(), samplerCount));

            // TODO: don't fill empty resources!
            for (var I = 0; I < Math.min(this.resources.size(), resourceCount); I++) {
                if (this.resources.get(I) != null && this.resources.get(I).imageView() != 0) {
                    var imageViewInfo = (ImageViewCInfo) deviceObj.handleMap.get(new UtilsCInfo.Handle("ImageView", this.resources.get(I).imageView())).orElse(null).cInfo;
                    resourceInfo.get(I).set(this.resources.get(I));
                    resourceDescriptorSets.get(I)
                        .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                        .descriptorType(imageViewInfo.type == "storage" ? VK_DESCRIPTOR_TYPE_STORAGE_IMAGE : VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE)
                        .descriptorCount(1)
                        .dstArrayElement(I)
                        .dstBinding(0)
                        .dstSet(this.descriptorSets[0])
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
                .dstSet(this.descriptorSets[1])
                .pImageInfo(samplerInfo);

            //
            VkDescriptorBufferInfo.Buffer uniformWrite = VkDescriptorBufferInfo.create(1);
            uniformWrite.get(0).set(this.uniformDescriptorBuffer.getBufferRange());
            uniformDescriptorSets.get(0)
                .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                .descriptorType(VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER)
                .descriptorCount(1)
                .dstArrayElement(0)
                .dstBinding(0)
                .dstSet(this.descriptorSets[2])
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
    public PipelineLayoutObj cmdBindBuffers(VkCommandBuffer cmdBuf, int pipelineBindPoint, long[] uniformBufferAddress) {
        if (!useLegacyBindingSystem) {
            // TODO: better binding system
            try ( MemoryStack stack = stackPush() ) {
                //var bufferBindings = VkDescriptorBufferBindingInfoEXT.calloc(uniformBufferAddress != null ? 4 : 3, stack).sType(VK_STRUCTURE_TYPE_DESCRIPTOR_BUFFER_BINDING_INFO_EXT);
                var bufferBindings = VkDescriptorBufferBindingInfoEXT.calloc(3, stack).sType(VK_STRUCTURE_TYPE_DESCRIPTOR_BUFFER_BINDING_INFO_EXT);
                bufferBindings.get(0).sType(VK_STRUCTURE_TYPE_DESCRIPTOR_BUFFER_BINDING_INFO_EXT).address$(this.resourceDescriptorBuffer.getDeviceAddress()).usage(VK_BUFFER_USAGE_RESOURCE_DESCRIPTOR_BUFFER_BIT_EXT);
                bufferBindings.get(1).sType(VK_STRUCTURE_TYPE_DESCRIPTOR_BUFFER_BINDING_INFO_EXT).address$(this.samplerDescriptorBuffer.getDeviceAddress()).usage(VK_BUFFER_USAGE_SAMPLER_DESCRIPTOR_BUFFER_BIT_EXT);
                bufferBindings.get(2).sType(VK_STRUCTURE_TYPE_DESCRIPTOR_BUFFER_BINDING_INFO_EXT).address$(this.uniformDescriptorBuffer.getDeviceAddress()).usage(VK_BUFFER_USAGE_RESOURCE_DESCRIPTOR_BUFFER_BIT_EXT);

                //
                //if (uniformBufferAddress != null) {
                //bufferBindings.get(3).sType(VK_STRUCTURE_TYPE_DESCRIPTOR_BUFFER_BINDING_INFO_EXT).address$(uniformBufferAddress.get(0)).usage(VK_BUFFER_USAGE_RESOURCE_DESCRIPTOR_BUFFER_BIT_EXT);
                //}

                //
                //IntBuffer bufferIndices = createIntBuffer(3).put(0, 0).put(1, 1).put(2, 2);//.put(3, uniformBufferAddress != null ? 3 : 2);
                //LongBuffer offsets = createLongBuffer(3).put(0, 0).put(1, 0).put(2, 0);//.put(3, 0);
                int[] bufferIndices = new int[]{0, 1, 2};
                long[] offsets = new long[]{0L, 0L, 0L};

                //
                vkCmdBindDescriptorBuffersEXT(cmdBuf, bufferBindings);
                vkCmdSetDescriptorBufferOffsetsEXT(cmdBuf, pipelineBindPoint, this.handle.get(), 0, bufferIndices, offsets);
            }
        } else {
            // TODO: test descriptor set bound
            //this.descriptorSets.put(3, uniformBufferAddress != null ? uniformBufferAddress.get(0) : this.uniformDescriptorSet.get(0));
            vkCmdBindDescriptorSets(cmdBuf, pipelineBindPoint, this.handle.get(), 0, this.descriptorSets, null);
        }

        //
        return this;
    }

}
