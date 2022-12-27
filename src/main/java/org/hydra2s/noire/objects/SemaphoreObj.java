package org.hydra2s.noire.objects;

import org.hydra2s.noire.descriptors.BasicCInfo;
import org.hydra2s.noire.descriptors.SemaphoreCInfo;
import org.lwjgl.vulkan.VkExportSemaphoreCreateInfoKHR;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;
import org.lwjgl.vulkan.VkSemaphoreGetWin32HandleInfoKHR;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.KHRExternalSemaphoreWin32.VK_STRUCTURE_TYPE_SEMAPHORE_GET_WIN32_HANDLE_INFO_KHR;
import static org.lwjgl.vulkan.KHRExternalSemaphoreWin32.vkGetSemaphoreWin32HandleKHR;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.vkCreateSemaphore;
import static org.lwjgl.vulkan.VK11.*;

public class SemaphoreObj extends BasicObj {

    public SemaphoreObj(Handle base, SemaphoreCInfo cInfo) {
        super(base, cInfo);
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get());
        vkCreateSemaphore(deviceObj.device, VkSemaphoreCreateInfo.create().pNext(VkExportSemaphoreCreateInfoKHR.create().sType(VK_STRUCTURE_TYPE_EXPORT_SEMAPHORE_CREATE_INFO).handleTypes(VK_EXTERNAL_SEMAPHORE_HANDLE_TYPE_OPAQUE_WIN32_BIT ).address()).sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO), null, memLongBuffer(memAddress((this.handle = new Handle("Semaphore")).ptr(), 0), 1));
        vkGetSemaphoreWin32HandleKHR(deviceObj.device, VkSemaphoreGetWin32HandleInfoKHR.create().sType(VK_STRUCTURE_TYPE_SEMAPHORE_GET_WIN32_HANDLE_INFO_KHR).semaphore(this.handle.get()).handleType(VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_WIN32_BIT), this.Win32Handle = memAllocPointer(1));
    }
}
