package org.hydra2s.noire.objects;

//

import org.hydra2s.noire.descriptors.SemaphoreCInfo;
import org.hydra2s.noire.descriptors.UtilsCInfo;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.*;

import static org.hydra2s.noire.descriptors.UtilsCInfo.vkCheckStatus;
import static org.lwjgl.BufferUtils.createLongBuffer;
import static org.lwjgl.BufferUtils.createPointerBuffer;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRExternalSemaphoreWin32.VK_STRUCTURE_TYPE_SEMAPHORE_GET_WIN32_HANDLE_INFO_KHR;
import static org.lwjgl.vulkan.KHRExternalSemaphoreWin32.vkGetSemaphoreWin32HandleKHR;
import static org.lwjgl.vulkan.KHRTimelineSemaphore.VK_STRUCTURE_TYPE_SEMAPHORE_TYPE_CREATE_INFO_KHR;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.vkCreateSemaphore;
import static org.lwjgl.vulkan.VK11.*;
import static org.lwjgl.vulkan.VK12.*;
import static org.lwjgl.vulkan.VK13.VK_PIPELINE_STAGE_2_NONE;
import static org.lwjgl.vulkan.VK13.VK_STRUCTURE_TYPE_SEMAPHORE_SUBMIT_INFO;

//
public class SemaphoreObj extends BasicObj {

    public VkSemaphoreTypeCreateInfo timelineInfo;
    public VkSemaphoreCreateInfo createInfo;
    public VkSemaphoreSubmitInfo submitInfo;
    public long[] timeline = null;

    //
    public long lastTimeline = 0;
    public long prevTimeline = 0;

    //
    public SemaphoreObj(UtilsCInfo.Handle base, SemaphoreCInfo cInfo) {
        super(base, cInfo);

        //
        if (cInfo.isTimeline && cInfo.initialValue <= 0L) { cInfo.initialValue = 1; };

        //
        this.prevTimeline = cInfo.initialValue;
        this.lastTimeline = cInfo.initialValue;
        this.deleted = false;
        this.timeline = new long[]{cInfo.initialValue};
        this.timelineInfo = VkSemaphoreTypeCreateInfo.create().sType(VK_STRUCTURE_TYPE_SEMAPHORE_TYPE_CREATE_INFO_KHR).semaphoreType(cInfo.isTimeline ? VK_SEMAPHORE_TYPE_TIMELINE : VK_SEMAPHORE_TYPE_BINARY).initialValue(lastTimeline);
        vkCheckStatus(vkCreateSemaphore(deviceObj.device, this.createInfo = VkSemaphoreCreateInfo.create().pNext(VkExportSemaphoreCreateInfoKHR.create().pNext(this.timelineInfo.address()).sType(VK_STRUCTURE_TYPE_EXPORT_SEMAPHORE_CREATE_INFO).handleTypes(VK_EXTERNAL_SEMAPHORE_HANDLE_TYPE_OPAQUE_WIN32_BIT ).address()).sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO), null, (this.handle = new UtilsCInfo.Handle("Semaphore")).ptr()));
        vkCheckStatus(vkGetSemaphoreWin32HandleKHR(deviceObj.device, VkSemaphoreGetWin32HandleInfoKHR.create().sType(VK_STRUCTURE_TYPE_SEMAPHORE_GET_WIN32_HANDLE_INFO_KHR).semaphore(this.handle.get()).handleType(VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_WIN32_BIT), this.Win32Handle = createPointerBuffer(1)));

        if (cInfo.doRegister) {
            deviceObj.handleMap.put$(this.handle, this);
        }

        this.submitInfo = VkSemaphoreSubmitInfo.create()
            .pNext(0)
            .sType(VK_STRUCTURE_TYPE_SEMAPHORE_SUBMIT_INFO)
            .semaphore(this.handle.get())
            .stageMask(VK_PIPELINE_STAGE_2_NONE)
            .value(lastTimeline);
    }

    @Override // TODO: multiple queue family support
    public SemaphoreObj deleteDirectly() /*throws Exception*/ {
        super.deleteDirectly();
        var handle = this.handle;

        // needs semaphore reusing mechanism
        if (handle.get() == 0) {
            System.out.println("Trying to destroy already destroyed semaphore.");
            throw new RuntimeException("Trying to destroy already destroyed semaphore.");
        };
        if (sharedPtr <= 0) {
            vkDeviceWaitIdle(deviceObj.device);
            vkDestroySemaphore(deviceObj.device, handle.get(), null);
            if (cInfo.doRegister) {
                deviceObj.handleMap.remove(handle);
            }
            this.deleted = true;
        };

        return this;
    }

    /*@Override // TODO: multiple queue family support
    public SemaphoreObj delete() throws Exception {
        super.delete();
        var handle = this.handle;

        deviceObj.submitOnce(new DeviceObj.SubmitCmd(){{
            queueGroupIndex = cInfo.queueGroupIndex;
            onDone = new Promise<>().thenApply((result)->{
                // needs semaphore reusing mechanism
                if (sharedPtr > 0) { sharedPtr--; }
                if (sharedPtr <= 0) {
                    if (handle.get() == 0) {
                        System.out.println("Trying to destroy already destroyed semaphore.");
                        try {
                            throw new Exception("Trying to destroy already destroyed semaphore.");
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                    vkDestroySemaphore(deviceObj.device, handle.get(), null);
                    if (cInfo.doRegister) {
                        deviceObj.handleMap.remove(handle);
                    }
                    handle.ptr().put(0, 0L);
                    deleted = true;
                };
                return null;
            });
        }}, (cmdBuf)->{
            return cmdBuf;
        });
        return this;
    }*/

    //
    public VkSemaphoreSubmitInfo makeSubmissionTimeline(long stageMask, boolean forWait) throws Exception {
        if (handle.get() == 0) {
            System.out.println("Invalid or destroyed semaphore making info.");
            throw new Exception("Invalid or destroyed semaphore making info.");
        }
        prevTimeline = lastTimeline; if (!forWait) { lastTimeline++; };
        return this.submitInfo.value(lastTimeline).stageMask(stageMask);
    }

    //
    public VkSemaphoreSubmitInfo makeSubmissionBinary(long stageMask) throws Exception {
        if (handle.get() == 0) {
            System.out.println("Invalid or destroyed semaphore making info.");
            throw new Exception("Invalid or destroyed semaphore making info.");
        }
        return this.submitInfo.value(0).stageMask(stageMask);
    }

    //
    public long getTimeline() /*throws Exception*/ {
        if (((SemaphoreCInfo)cInfo).isTimeline) {
            if (handle.get() == 0) {
                System.out.println("Trying to get timeline from destroyed or invalid semaphore.");
                throw new RuntimeException("Trying to get timeline from destroyed or invalid semaphore.");
            }
            if (handle.get() != 0) {
                vkCheckStatus(vkGetSemaphoreCounterValue(deviceObj.device, this.handle.get(), this.timeline));
            }
        }
        return this.timeline[0];
    }

    //
    public SemaphoreObj signalTimeline() throws Exception {
        if (((SemaphoreCInfo)cInfo).isTimeline) {
            if (handle.get() == 0) {
                System.out.println("Trying to signal timeline by destroyed or invalid semaphore.");
                throw new Exception("Trying to signal timeline by destroyed or invalid semaphore.");
            }
            prevTimeline = lastTimeline; lastTimeline++;
            try ( MemoryStack stack = stackPush() ) {
                vkCheckStatus(vkSignalSemaphore(deviceObj.device, VkSemaphoreSignalInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SEMAPHORE_SIGNAL_INFO)
                    .semaphore(this.handle.get())
                    .value(lastTimeline)));
            }
        }
        return this;
    }

    //
    public SemaphoreObj waitTimeline(boolean any) throws Exception {
        if (((SemaphoreCInfo)cInfo).isTimeline) {
            if (handle.get() == 0) {
                System.out.println("Trying to wait timeline a destroyed or invalid semaphore.");
                throw new Exception("Trying to wait timeline a destroyed or invalid semaphore.");
            }
            try ( MemoryStack stack = stackPush() ) {
                vkCheckStatus(vkWaitSemaphores(deviceObj.device, VkSemaphoreWaitInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SEMAPHORE_WAIT_INFO)
                    .flags(any ? VK_SEMAPHORE_WAIT_ANY_BIT : 0)
                    .pSemaphores(stack.callocLong(1).put(0, this.handle.get()))
                    .semaphoreCount(1)
                    .pValues(stack.callocLong(1).put(0, prevTimeline = lastTimeline)), 9007199254740991L));
            }
        }
        return this;
    }

    //
    public SemaphoreObj incrementShared() {
        this.sharedPtr++;
        return this;
    }
}
