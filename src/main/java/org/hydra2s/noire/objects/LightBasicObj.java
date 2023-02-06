package org.hydra2s.noire.objects;

//

import com.lodborg.intervaltree.IntervalTree;
import org.hydra2s.noire.descriptors.BasicCInfo;
import org.hydra2s.noire.descriptors.UtilsCInfo;
import org.lwjgl.PointerBuffer;

import java.nio.IntBuffer;
import java.util.Optional;

import static org.lwjgl.BufferUtils.createPointerBuffer;
import static org.lwjgl.system.MemoryUtil.memAllocPointer;

//
// DO NOT `wrap`! Use `memAlloc#Type()` with `put(pos, val)`!
// DO NOT `allocate(N)`, use `memAlloc#Type()`
//

//
public class LightBasicObj {
    // TODO: globalize such flag
    public boolean deleted = false;
    public long sharedPtr = 0;

    //
    public BasicCInfo cInfo = null;
    protected UtilsCInfo.Handle handle;

    //
    public LightBasicObj(UtilsCInfo.Handle handle) {
        this.handle = handle;

    }

    //
    public LightBasicObj(BasicCInfo cInfo) {
        this.cInfo = cInfo;
    }

    public LightBasicObj deleteDirectly() /*throws Exception*/ {
        if (sharedPtr > 0) { sharedPtr--; }
        //if (sharedPtr <= 0) { memFree(this.ptr); };
        return this;
    }

}

