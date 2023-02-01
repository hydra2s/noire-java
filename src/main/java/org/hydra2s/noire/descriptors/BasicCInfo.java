package org.hydra2s.noire.descriptors;

//
import org.hydra2s.utils.Promise;
import org.lwjgl.vulkan.*;

//
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;

//
import static org.lwjgl.system.MemoryUtil.memAllocInt;
import static org.lwjgl.vulkan.EXTConditionalRendering.VK_BUFFER_USAGE_CONDITIONAL_RENDERING_BIT_EXT;
import static org.lwjgl.vulkan.EXTTransformFeedback.VK_BUFFER_USAGE_TRANSFORM_FEEDBACK_BUFFER_BIT_EXT;
import static org.lwjgl.vulkan.EXTTransformFeedback.VK_BUFFER_USAGE_TRANSFORM_FEEDBACK_COUNTER_BUFFER_BIT_EXT;
import static org.lwjgl.vulkan.HUAWEIInvocationMask.VK_ACCESS_2_INVOCATION_MASK_READ_BIT_HUAWEI;
import static org.lwjgl.vulkan.HUAWEIInvocationMask.VK_PIPELINE_STAGE_2_INVOCATION_MASK_BIT_HUAWEI;
import static org.lwjgl.vulkan.IMGFormatPVRTC.*;
import static org.lwjgl.vulkan.KHRAccelerationStructure.VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_BUILD_INPUT_READ_ONLY_BIT_KHR;
import static org.lwjgl.vulkan.KHRAccelerationStructure.VK_BUFFER_USAGE_ACCELERATION_STRUCTURE_STORAGE_BIT_KHR;
import static org.lwjgl.vulkan.KHRRayTracingPipeline.VK_BUFFER_USAGE_SHADER_BINDING_TABLE_BIT_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
import static org.lwjgl.vulkan.KHRSynchronization2.*;
import static org.lwjgl.vulkan.VK10.*;
import static org.lwjgl.vulkan.VK11.*;
import static org.lwjgl.vulkan.VK12.*;
import static org.lwjgl.vulkan.VK13.*;

//
public class BasicCInfo {
    //
    public int queueGroupIndex = -1;
    public boolean doRegister = true;




}
