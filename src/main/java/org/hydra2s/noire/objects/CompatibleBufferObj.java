package org.hydra2s.noire.objects;

import org.hydra2s.noire.descriptors.BufferCInfo;
import org.hydra2s.noire.descriptors.MemoryAllocationCInfo;
import org.lwjgl.vulkan.VkCommandBuffer;

import java.nio.ByteBuffer;

// TODO: add copy operations with auto-synchronization
public class CompatibleBufferObj extends BufferObj {
    public BufferObj hostBuffer = null;

    //
    public CompatibleBufferObj(Handle base, Handle handle) {
        super(base, handle);
    }

    //
    public CompatibleBufferObj(Handle base, BufferCInfo cInfo) {
        super(base, cInfo);

        //
        if (!cInfo.memoryAllocationInfo.isHost) {
            this.hostBuffer = new BufferObj(this.base, new BufferCInfo() {{
                size = cInfo.size;
                usage = 0;
                memoryAllocator = cInfo.memoryAllocator;
                memoryAllocationInfo = new MemoryAllocationCInfo(){{
                    isHost = true;
                    isDevice = false;
                }};
            }});
        }
    }

    //
    // necessary after `unmap()` op
    @Override
    public BufferObj cmdSynchronizeFromHost(VkCommandBuffer cmdBuf) {

        //
        if (this.hostBuffer == null) {
            super.cmdSynchronizeFromHost(cmdBuf);
        } else {
            CommandUtils.cmdCopyBufferToBuffer(cmdBuf, new CommandUtils.BufferCopyInfo(){{
                buffer = hostBuffer.getHandle().get();
                range = createInfo.size();
            }}, new CommandUtils.BufferCopyInfo(){{
                buffer = getHandle().get();
                range = createInfo.size();
            }});
        }

        //
        return this;
    }

    //
    @Override
    public ByteBuffer map(long byteLength, long byteOffset) {
        if (this.hostBuffer == null) {
            return super.map(byteLength, byteOffset);
        } else {
            return this.hostBuffer.map(byteLength, byteOffset);
        }
    }

    //
    @Override
    public void unmap() {
        if (this.hostBuffer == null) {
            super.unmap();
        } else {
            this.hostBuffer.unmap();
        }
    }
}
