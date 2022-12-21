package org.hydra2s.manhack.objects;

import org.hydra2s.manhack.descriptors.DeviceCInfo;

public class DeviceObj extends BasicObj {

    public DeviceObj(Handle base, Handle handle) {
        super(base, handle);
    }

    public DeviceObj(Handle base, DeviceCInfo cInfo) {
        super(base, cInfo);

        
    }

}
