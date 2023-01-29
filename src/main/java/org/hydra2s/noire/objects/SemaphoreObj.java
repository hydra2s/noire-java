package org.hydra2s.noire.objects;

//
import org.hydra2s.noire.descriptors.BasicCInfo;
import org.hydra2s.noire.descriptors.SemaphoreCInfo;
import org.hydra2s.noire.descriptors.SwapChainCInfo;
import org.hydra2s.utils.Promise;
import org.lwjgl.vulkan.*;

//
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.vulkan.KHRExternalSemaphoreWin32.VK_STRUCTURE_TYPE_SEMAPHORE_GET_WIN32_HANDLE_INFO_KHR;
import static org.lwjgl.vulkan.KHRExternalSemaphoreWin32.vkGetSemaphoreWin32HandleKHR;
import static org.lwjgl.vulkan.KHRTimelineSemaphore.VK_STRUCTURE_TYPE_SEMAPHORE_TYPE_CREATE_INFO_KHR;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.vkCreateSemaphore;
import static org.lwjgl.vulkan.VK11.*;
import static org.lwjgl.vulkan.VK12.*;
import static org.lwjgl.vulkan.VK13.VK_STRUCTURE_TYPE_SEMAPHORE_SUBMIT_INFO;

//
public class SemaphoreObj extends BasicObj {

    public VkSemaphoreTypeCreateInfo timelineInfo;
    public VkSemaphoreCreateInfo createInfo;
    public LongBuffer timeline = null;

    public SemaphoreObj(Handle base, SemaphoreCInfo cInfo) {
        super(base, cInfo);

        //
        this.timeline = memAllocLong(1);
        this.timelineInfo = VkSemaphoreTypeCreateInfo.calloc().sType(VK_STRUCTURE_TYPE_SEMAPHORE_TYPE_CREATE_INFO_KHR).semaphoreType(cInfo.isTimeline ? VK_SEMAPHORE_TYPE_TIMELINE : VK_SEMAPHORE_TYPE_BINARY);
        vkCreateSemaphore(deviceObj.device, this.createInfo = VkSemaphoreCreateInfo.calloc().pNext(VkExportSemaphoreCreateInfoKHR.calloc().pNext(this.timelineInfo.address()).sType(VK_STRUCTURE_TYPE_EXPORT_SEMAPHORE_CREATE_INFO).handleTypes(VK_EXTERNAL_SEMAPHORE_HANDLE_TYPE_OPAQUE_WIN32_BIT ).address()).sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO), null, memLongBuffer(memAddress((this.handle = new Handle("Semaphore")).ptr(), 0), 1));
        vkGetSemaphoreWin32HandleKHR(deviceObj.device, VkSemaphoreGetWin32HandleInfoKHR.calloc().sType(VK_STRUCTURE_TYPE_SEMAPHORE_GET_WIN32_HANDLE_INFO_KHR).semaphore(this.handle.get()).handleType(VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_WIN32_BIT), this.Win32Handle = memAllocPointer(1));
        deviceObj.handleMap.put$(this.handle, this);
    }

    @Override // TODO: multiple queue family support
    public SemaphoreObj delete() {
        var handle = this.handle;
        
        deviceObj.submitOnce(new BasicCInfo.SubmitCmd(){{
            queueGroupIndex = cInfo.queueGroupIndex;
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

    //
    public VkSemaphoreSubmitInfo makeSubmissionTimeline(long stageMask, long value) {
        return VkSemaphoreSubmitInfo.calloc()
            .sType(VK_STRUCTURE_TYPE_SEMAPHORE_SUBMIT_INFO)
            .semaphore(this.handle.get())
            .value(value)
            .stageMask(stageMask);
    }

    //
    public VkSemaphoreSubmitInfo makeSubmissionBinary(long stageMask) {
        return VkSemaphoreSubmitInfo.calloc()
            .sType(VK_STRUCTURE_TYPE_SEMAPHORE_SUBMIT_INFO)
            .semaphore(this.handle.get())
            .stageMask(stageMask);
    }

    //
    public long getTimeline() {
        vkGetSemaphoreCounterValue(deviceObj.device, this.handle.get(), this.timeline);
        return this.timeline.get(0);
    }

    //
    public SemaphoreObj signalTimeline(long l) {
        vkSignalSemaphore(deviceObj.device, VkSemaphoreSignalInfo.calloc()
            .sType(VK_STRUCTURE_TYPE_SEMAPHORE_SIGNAL_INFO)
            .semaphore(this.handle.get())
            .value(l));
        return this;
    }

    // UNSAFE!
    public SemaphoreObj waitTimeline(long l) {
        vkWaitSemaphores(deviceObj.device, VkSemaphoreWaitInfo.calloc()
            .sType(VK_STRUCTURE_TYPE_SEMAPHORE_SIGNAL_INFO)
            .pSemaphores(this.handle.handle.getLongBuffer(1))
            .pValues(memAllocLong(1).put(0, l)), 1024L * 1024L * 1024L);
        return this;
    }
}
