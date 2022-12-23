package org.hydra2s;

import org.hydra2s.manhack.descriptors.DeviceCInfo;
import org.hydra2s.manhack.descriptors.InstanceCInfo;
import org.hydra2s.manhack.descriptors.PipelineLayoutCInfo;
import org.hydra2s.manhack.objects.DeviceObj;
import org.hydra2s.manhack.objects.InstanceObj;
import org.hydra2s.manhack.objects.PipelineLayoutObj;

//
public class Main {

    public static void main(String[] args) {
        //
        var instanceCInfo = new InstanceCInfo();
        var instance = new InstanceObj(null, instanceCInfo);

        //
        var physicalDevices = instance.enumeratePhysicalDevicesObj();
        var physicalDevice = physicalDevices.get(0);

        //
        var queueCInfo = new DeviceCInfo.QueueFamilyCInfo();
        queueCInfo.index = 0;
        queueCInfo.priorities = new float[]{1.0F};

        //
        var logicalDeviceCInfo = new DeviceCInfo();
        logicalDeviceCInfo.queueFamilies.add(queueCInfo);

        //
        var logicalDevice = new DeviceObj(physicalDevice.getHandle(), logicalDeviceCInfo);

        //
        var descriptorSetCInfo = new PipelineLayoutCInfo();
        var descriptorSet = new PipelineLayoutObj(logicalDevice.getHandle(), descriptorSetCInfo);

        //

    }

}
