package org.hydra2s.manhack.objects;

import org.lwjgl.PointerBuffer;
import org.lwjgl.vulkan.*;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryUtil.*;
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

    protected VkDescriptorSetLayoutCreateInfo resourceDescriptorSetLayoutCreateInfo = null;
    protected VkDescriptorSetLayoutCreateInfo samplerDescriptorSetLayoutCreateInfo = null;
    protected VkDescriptorSetLayoutCreateInfo uniformDescriptorSetLayoutCreateInfo = null;

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
    public PipelineLayoutObj(Handle base, Handle handle) {
        super(base, handle);

        //
        var deviceObj = (DeviceObj) BasicObj.globalHandleMap.get(base.get());

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
        vkCreatePipelineLayout(deviceObj.device, VkPipelineLayoutCreateInfo.create().pSetLayouts(this.descriptorLayout.getLongBuffer(3)).pPushConstantRanges(this.pConstRange), null, (this.handle = new Handle(3)).ptr().getLongBuffer(1));

        //

    }

}
