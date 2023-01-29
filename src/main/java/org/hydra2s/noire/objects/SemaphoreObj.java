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

    // TODO: globalize such flag
    public boolean deleted = false;
    public long lastTimeline = 1;
    public long prevTimeline = 0;

    public SemaphoreObj(Handle base, SemaphoreCInfo cInfo) {
        super(base, cInfo);

        //
        this.prevTimeline = cInfo.initialValue;
        this.lastTimeline = cInfo.initialValue;
        this.deleted = false;
        this.timeline = memAllocLong(1).put(0, cInfo.initialValue);
        this.timelineInfo = VkSemaphoreTypeCreateInfo.calloc().sType(VK_STRUCTURE_TYPE_SEMAPHORE_TYPE_CREATE_INFO_KHR).semaphoreType(cInfo.isTimeline ? VK_SEMAPHORE_TYPE_TIMELINE : VK_SEMAPHORE_TYPE_BINARY).initialValue(lastTimeline);
        vkCreateSemaphore(deviceObj.device, this.createInfo = VkSemaphoreCreateInfo.calloc().pNext(VkExportSemaphoreCreateInfoKHR.calloc().pNext(this.timelineInfo.address()).sType(VK_STRUCTURE_TYPE_EXPORT_SEMAPHORE_CREATE_INFO).handleTypes(VK_EXTERNAL_SEMAPHORE_HANDLE_TYPE_OPAQUE_WIN32_BIT ).address()).sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO), null, memLongBuffer(memAddress((this.handle = new Handle("Semaphore")).ptr(), 0), 1));
        vkGetSemaphoreWin32HandleKHR(deviceObj.device, VkSemaphoreGetWin32HandleInfoKHR.calloc().sType(VK_STRUCTURE_TYPE_SEMAPHORE_GET_WIN32_HANDLE_INFO_KHR).semaphore(this.handle.get()).handleType(VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_WIN32_BIT), this.Win32Handle = memAllocPointer(1));
        deviceObj.handleMap.put$(this.handle, this);
    }

    @Override // TODO: multiple queue family support
    public SemaphoreObj deleteDirectly() throws Exception {
        super.deleteDirectly();
        var handle = this.handle;

        // needs semaphore reusing mechanism
        vkDestroySemaphore(deviceObj.device, handle.get(), null);

        //
        deviceObj.handleMap.remove(handle);
        this.deleted = true;
        return this;
    }

    @Override // TODO: multiple queue family support
    public SemaphoreObj delete() throws Exception {
        super.delete();
        var handle = this.handle;

        deviceObj.submitOnce(new BasicCInfo.SubmitCmd(){{
            queueGroupIndex = cInfo.queueGroupIndex;
            onDone = new Promise<>().thenApply((result)->{

                // needs semaphore reusing mechanism
                vkDestroySemaphore(deviceObj.device, handle.get(), null);

                //
                deviceObj.handleMap.remove(handle);
                deleted = true;
                return null;
            });
        }}, (cmdBuf)->{
            return VK_SUCCESS;
        });
        return this;
    }

    //
    public VkSemaphoreSubmitInfo makeSubmissionTimeline(long stageMask) {
        prevTimeline = lastTimeline; lastTimeline++;
        return VkSemaphoreSubmitInfo.calloc()
            .sType(VK_STRUCTURE_TYPE_SEMAPHORE_SUBMIT_INFO)
            .semaphore(this.handle.get())
            .value(lastTimeline)
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
        if (((SemaphoreCInfo)cInfo).isTimeline) {
            vkGetSemaphoreCounterValue(deviceObj.device, this.handle.get(), this.timeline);
        }
        return this.timeline.get(0);
    }

    //
    public SemaphoreObj signalTimeline() {
        if (((SemaphoreCInfo)cInfo).isTimeline) {
            prevTimeline = lastTimeline;
            lastTimeline++;
            vkSignalSemaphore(deviceObj.device, VkSemaphoreSignalInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_SEMAPHORE_SIGNAL_INFO)
                .semaphore(this.handle.get())
                .value(lastTimeline));
        }
        return this;
    }

    // UNSAFE!
    public SemaphoreObj waitTimeline(long lastTimeline, boolean any) {
        if (((SemaphoreCInfo)cInfo).isTimeline) {
            vkWaitSemaphores(deviceObj.device, VkSemaphoreWaitInfo.calloc()
                .sType(VK_STRUCTURE_TYPE_SEMAPHORE_SIGNAL_INFO)
                .flags(any ? VK_SEMAPHORE_WAIT_ANY_BIT : 0)
                .pSemaphores(memAllocLong(1).put(0, this.handle.get()))
                .pValues(memAllocLong(1).put(0, lastTimeline)), 1024L * 1024L * 1024L);
        }
        return this;
    }
}
