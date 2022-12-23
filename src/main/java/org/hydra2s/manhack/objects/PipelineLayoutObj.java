package org.hydra2s.manhack.objects;

import org.hydra2s.manhack.descriptors.ImageViewCInfo;
import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.EXTDescriptorBuffer.*;
import static org.lwjgl.vulkan.EXTMutableDescriptorType.VK_DESCRIPTOR_TYPE_MUTABLE_EXT;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK12.VK_DESCRIPTOR_BINDING_PARTIALLY_BOUND_BIT;
import static org.lwjgl.vulkan.VK12.VK_DESCRIPTOR_BINDING_UPDATE_UNUSED_WHILE_PENDING_BIT;
import static org.lwjgl.vulkan.VK13.VK_DESCRIPTOR_TYPE_INLINE_UNIFORM_BLOCK;

//
public class PipelineLayoutObj extends BasicObj  {
    protected LongBuffer pipelineLayout = memAllocLong(1);
    protected PointerBuffer descriptorLayout = memAllocPointer(3);
    protected VkPushConstantRange.Buffer pConstRange = null;
    protected VkDescriptorSetLayoutBindingFlagsCreateInfoEXT resourceDescriptorSetLayoutCreateInfoBindingFlags = null;
    protected VkDescriptorSetLayoutBindingFlagsCreateInfoEXT samplerDescriptorSetLayoutCreateInfoBindingFlags = null;
    protected VkDescriptorSetLayoutBindingFlagsCreateInfoEXT uniformDescriptorSetLayoutCreateInfoBindingFlags = null;

    //
    protected VkDescriptorSetLayoutCreateInfo resourceDescriptorSetLayoutCreateInfo = null;
    protected VkDescriptorSetLayoutCreateInfo samplerDescriptorSetLayoutCreateInfo = null;
    protected VkDescriptorSetLayoutCreateInfo uniformDescriptorSetLayoutCreateInfo = null;

    //
    protected VkMutableDescriptorTypeCreateInfoEXT mutableDescriptorInfo = null;
    protected VkMutableDescriptorTypeListEXT.Buffer mutableDescriptorLists = null;
    protected IntBuffer mutableDescriptorTypes = memAllocInt(1).put(0, 0);
    protected IntBuffer resourceDescriptorSetBindingFlags = memAllocInt(1).put(0, 0);
    protected IntBuffer samplerDescriptorSetBindingFlags = memAllocInt(1).put(0, 0);
    protected IntBuffer uniformDescriptorSetBindingFlags = memAllocInt(1).put(0, 0);
    //
    protected long uniformBufferSize = 65536;

    //
    protected VkDescriptorSetLayoutBinding resourceDescriptorSetBindings = null;
    protected VkDescriptorSetLayoutBinding samplerDescriptorSetBindings = null;
    protected VkDescriptorSetLayoutBinding uniformDescriptorSetBindings = null;

    //
    protected MemoryAllocationObj.BufferObj resourceDescriptorBuffer = null;
    protected MemoryAllocationObj.BufferObj samplerDescriptorBuffer = null;
    protected MemoryAllocationObj.BufferObj uniformDescriptorBuffer = null;

    //
    protected LongBuffer offsets = memAllocLong(3);
    protected LongBuffer sizes = memAllocLong(3);

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
    }

    //
    protected OutstandingArray<VkDescriptorImageInfo> resources = new OutstandingArray<VkDescriptorImageInfo>();
    protected OutstandingArray<LongBuffer> samplers = new OutstandingArray<LongBuffer>();

    //
    public PipelineLayoutObj(Handle base, Handle handle) {
        super(base, handle);

        //
        var deviceObj = (DeviceObj) BasicObj.globalHandleMap.get(base.get());
        var physicalDeviceObj = (PhysicalDeviceObj)BasicObj.globalHandleMap.get(deviceObj.base.get());

        //
        this.resources = new OutstandingArray<VkDescriptorImageInfo>();
        this.samplers = new OutstandingArray<LongBuffer>();

        //
        this.resourceDescriptorSetBindings = VkDescriptorSetLayoutBinding.create().binding(0).descriptorType(VK_DESCRIPTOR_TYPE_MUTABLE_EXT).descriptorCount(1024);
        this.samplerDescriptorSetBindings = VkDescriptorSetLayoutBinding.create().binding(0).descriptorType(VK_DESCRIPTOR_TYPE_SAMPLER).descriptorCount(256);
        this.uniformDescriptorSetBindings = VkDescriptorSetLayoutBinding.create().binding(0).descriptorType(VK_DESCRIPTOR_TYPE_INLINE_UNIFORM_BLOCK).descriptorCount(1);

        //
        this.resourceDescriptorSetBindingFlags = memAllocInt(1).put(0, VK_DESCRIPTOR_BINDING_PARTIALLY_BOUND_BIT | VK_DESCRIPTOR_BINDING_UPDATE_UNUSED_WHILE_PENDING_BIT);
        this.samplerDescriptorSetBindingFlags = memAllocInt(1).put(0, VK_DESCRIPTOR_BINDING_PARTIALLY_BOUND_BIT | VK_DESCRIPTOR_BINDING_UPDATE_UNUSED_WHILE_PENDING_BIT);
        this.uniformDescriptorSetBindingFlags = memAllocInt(1).put(0, VK_DESCRIPTOR_BINDING_PARTIALLY_BOUND_BIT | VK_DESCRIPTOR_BINDING_UPDATE_UNUSED_WHILE_PENDING_BIT);

        //
        this.mutableDescriptorTypes = memAllocInt(2).put(0, VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE).put(1, VK_DESCRIPTOR_TYPE_STORAGE_IMAGE);
        this.mutableDescriptorLists = VkMutableDescriptorTypeListEXT.create(1).pDescriptorTypes(this.mutableDescriptorTypes);
        this.mutableDescriptorInfo = VkMutableDescriptorTypeCreateInfoEXT.create().pMutableDescriptorTypeLists(this.mutableDescriptorLists);

        //
        this.descriptorLayout = memAllocPointer(3);

        //
        this.resourceDescriptorSetLayoutCreateInfoBindingFlags = VkDescriptorSetLayoutBindingFlagsCreateInfoEXT.create().pNext(mutableDescriptorInfo.address()).pBindingFlags(this.resourceDescriptorSetBindingFlags);
        this.resourceDescriptorSetLayoutCreateInfo = VkDescriptorSetLayoutCreateInfo.create().pNext(this.resourceDescriptorSetLayoutCreateInfoBindingFlags);
        vkCreateDescriptorSetLayout(deviceObj.device, this.resourceDescriptorSetLayoutCreateInfo, null, this.descriptorLayout.getLongBuffer(0, 1));

        //
        this.samplerDescriptorSetLayoutCreateInfoBindingFlags = VkDescriptorSetLayoutBindingFlagsCreateInfoEXT.create().pBindingFlags(this.samplerDescriptorSetBindingFlags);
        this.samplerDescriptorSetLayoutCreateInfo = VkDescriptorSetLayoutCreateInfo.create().pNext(this.samplerDescriptorSetLayoutCreateInfoBindingFlags);
        vkCreateDescriptorSetLayout(deviceObj.device, this.resourceDescriptorSetLayoutCreateInfo, null, this.descriptorLayout.getLongBuffer(1, 1));

        //
        this.uniformDescriptorSetLayoutCreateInfoBindingFlags = VkDescriptorSetLayoutBindingFlagsCreateInfoEXT.create().pBindingFlags(this.uniformDescriptorSetBindingFlags);
        this.uniformDescriptorSetLayoutCreateInfo = VkDescriptorSetLayoutCreateInfo.create().pNext(this.uniformDescriptorSetLayoutCreateInfoBindingFlags);
        vkCreateDescriptorSetLayout(deviceObj.device, this.resourceDescriptorSetLayoutCreateInfo, null, this.descriptorLayout.getLongBuffer(2, 1));

        //
        this.pConstRange = VkPushConstantRange.create(1).stageFlags(VK_SHADER_STAGE_ALL).offset(0).size(256);
        vkCreatePipelineLayout(deviceObj.device, VkPipelineLayoutCreateInfo.create().pSetLayouts(this.descriptorLayout.getLongBuffer(3)).pPushConstantRanges(this.pConstRange), null, (this.handle = new Handle("PipelineLayout")).ptr().getLongBuffer(1));

        //
        offsets = memAllocLong(3);
        sizes = memAllocLong(3);

        //
        for (int i=0;i<3;i++) {
            vkGetDescriptorSetLayoutSizeEXT(deviceObj.device, this.descriptorLayout.get(i), sizes.slice(i, 1));
            vkGetDescriptorSetLayoutBindingOffsetEXT(deviceObj.device, this.descriptorLayout.get(i), 0, offsets.slice(i, 1));
        }

        //
        this.writeDescriptors();

        //
        //return this;
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
            vkGetDescriptorEXT(deviceObj.device, VkDescriptorGetInfoEXT.create()
                .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_GET_INFO_EXT)
                .type(((ImageViewCInfo)deviceObj.handleMap.get(this.resources.get(I)).cInfo).type == "storage" ? VK_DESCRIPTOR_TYPE_STORAGE_IMAGE : VK_DESCRIPTOR_TYPE_SAMPLED_IMAGE)
                .data(VkDescriptorDataEXT.create().pSampledImage(this.resources.get(I))), RMAP.slice(RSIZE*I, RSIZE));
        }

        //
        for (var I=0;I<Math.min(this.samplers.size(), 256);I++) {
            vkGetDescriptorEXT(deviceObj.device, VkDescriptorGetInfoEXT.create()
                .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_GET_INFO_EXT)
                .type(VK_DESCRIPTOR_TYPE_SAMPLER)
                .data(VkDescriptorDataEXT.create().pSampler(this.samplers.get(I))), SMAP.slice(SSIZE*I, SSIZE));
        }

        //
        this. samplerDescriptorBuffer.unmap();
        this.resourceDescriptorBuffer.unmap();

        //
        return this;
    }

    public PipelineLayoutObj cmdBindBuffers(VkCommandBuffer cmdBuf, int pipelineBindPoint) {
        //
        var bufferBindings = VkDescriptorBufferBindingInfoEXT.create(3).sType(VK_STRUCTURE_TYPE_DESCRIPTOR_BUFFER_BINDING_INFO_EXT);
        bufferBindings.get(0).address$(this.resourceDescriptorBuffer.getDeviceAddress()).usage(VK_BUFFER_USAGE_RESOURCE_DESCRIPTOR_BUFFER_BIT_EXT);
        bufferBindings.get(1).address$(this.samplerDescriptorBuffer.getDeviceAddress()).usage(VK_BUFFER_USAGE_SAMPLER_DESCRIPTOR_BUFFER_BIT_EXT);
        bufferBindings.get(2).address$(this.uniformDescriptorBuffer.getDeviceAddress()).usage(VK_BUFFER_USAGE_RESOURCE_DESCRIPTOR_BUFFER_BIT_EXT);

        //
        IntBuffer bufferIndices = memAllocInt(3).put(0, 0).put(1,1).put(2,2);
        LongBuffer offsets = memAllocLong(3).put(0, 0).put(1, 0).put(2, 0);

        //
        vkCmdBindDescriptorBuffersEXT(cmdBuf, bufferBindings);
        vkCmdSetDescriptorBufferOffsetsEXT(cmdBuf, pipelineBindPoint, this.handle.get(), 0, bufferIndices, offsets);

        //
        return this;
    }

}
