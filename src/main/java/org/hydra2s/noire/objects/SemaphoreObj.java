package org.hydra2s.noire.objects;

//
import org.hydra2s.noire.descriptors.BasicCInfo;
import org.hydra2s.noire.descriptors.SemaphoreCInfo;
import org.hydra2s.noire.descriptors.SwapChainCInfo;
import org.hydra2s.utils.Promise;
import org.lwjgl.vulkan.VkExportSemaphoreCreateInfoKHR;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;
import org.lwjgl.vulkan.VkSemaphoreGetWin32HandleInfoKHR;

//
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.KHRExternalSemaphoreWin32.VK_STRUCTURE_TYPE_SEMAPHORE_GET_WIN32_HANDLE_INFO_KHR;
import static org.lwjgl.vulkan.KHRExternalSemaphoreWin32.vkGetSemaphoreWin32HandleKHR;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.vkCreateSemaphore;
import static org.lwjgl.vulkan.VK11.*;

//
public class SemaphoreObj extends BasicObj {

    public SemaphoreObj(Handle base, SemaphoreCInfo cInfo) {
        super(base, cInfo);
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get());
        vkCreateSemaphore(deviceObj.device, VkSemaphoreCreateInfo.calloc().pNext(VkExportSemaphoreCreateInfoKHR.calloc().sType(VK_STRUCTURE_TYPE_EXPORT_SEMAPHORE_CREATE_INFO).handleTypes(VK_EXTERNAL_SEMAPHORE_HANDLE_TYPE_OPAQUE_WIN32_BIT ).address()).sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO), null, memLongBuffer(memAddress((this.handle = new Handle("Semaphore")).ptr(), 0), 1));
        vkGetSemaphoreWin32HandleKHR(deviceObj.device, VkSemaphoreGetWin32HandleInfoKHR.calloc().sType(VK_STRUCTURE_TYPE_SEMAPHORE_GET_WIN32_HANDLE_INFO_KHR).semaphore(this.handle.get()).handleType(VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_WIN32_BIT), this.Win32Handle = memAllocPointer(1));
        deviceObj.handleMap.put(this.handle, this);
    }

    @Override // TODO: multiple queue family support
    public SemaphoreObj delete() {
        var handle = this.handle;
        var deviceObj = (DeviceObj)BasicObj.globalHandleMap.get(this.base.get());
        deviceObj.submitOnce(deviceObj.getCommandPool((cInfo).queueFamilyIndex), new BasicCInfo.SubmitCmd(){{
            queue = deviceObj.getQueue((cInfo).queueFamilyIndex, 0);
            onDone = new Promise<>().thenApply((result)->{
                vkDestroySemaphore(deviceObj.device, handle.get(), null);
                deviceObj.handleMap.remove(handle);
                return null;
            });
        }}, (cmdBuf)->{
            return VK_SUCCESS;
        });
        return this;
    }
}
