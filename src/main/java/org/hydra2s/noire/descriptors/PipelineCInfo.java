package org.hydra2s.noire.descriptors;

//
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

//
public class PipelineCInfo extends BasicCInfo  {

    public long pipelineLayout = 0L;
    public long memoryAllocator = 0L;
    public long uniformBufferSize = 2048L;

    static public class ComputePipelineCInfo extends PipelineCInfo {
        public ByteBuffer computeCode = null;
    }

    static public class GraphicsPipelineCInfo extends PipelineCInfo  {
        public ConcurrentHashMap<Integer, ByteBuffer> sourceMap = new ConcurrentHashMap<Integer, ByteBuffer>();
        public ImageSetCInfo.FBLayout fbLayout = new ImageSetCInfo.FBLayout();
    }

}
