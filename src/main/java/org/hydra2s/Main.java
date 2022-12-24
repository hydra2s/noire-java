package org.hydra2s;

import org.hydra2s.manhack.descriptors.DeviceCInfo;
import org.hydra2s.manhack.descriptors.InstanceCInfo;
import org.hydra2s.manhack.descriptors.MemoryAllocatorCInfo;
import org.hydra2s.manhack.descriptors.PipelineLayoutCInfo;
import org.hydra2s.manhack.objects.DeviceObj;
import org.hydra2s.manhack.objects.InstanceObj;
import org.hydra2s.manhack.objects.MemoryAllocatorObj;
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
        var memAllocatorCInfo = new MemoryAllocatorCInfo();

        //
        var logicalDevice = new DeviceObj(physicalDevice.getHandle(), logicalDeviceCInfo);
        var memoryAllocator = new MemoryAllocatorObj(logicalDevice.getHandle(), memAllocatorCInfo);

        //
        var descriptorSetCInfo = new PipelineLayoutCInfo();
        descriptorSetCInfo.memoryAllocator = memoryAllocator.getHandle().get();
        var descriptorSet = new PipelineLayoutObj(logicalDevice.getHandle(), descriptorSetCInfo);

        //
        System.out.println(descriptorSet.getHandle().get());
    }

}
