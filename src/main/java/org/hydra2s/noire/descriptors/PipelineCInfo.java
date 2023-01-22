package org.hydra2s.noire.descriptors;

//
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;

//
public class PipelineCInfo extends BasicCInfo  {

    public long pipelineLayout = 0L;
    public long memoryAllocator = 0L;
    public long uniformBufferSize = 2048L;

    static public class ComputePipelineCInfo extends PipelineCInfo {
        public ByteBuffer computeCode = null;
    }

    static public class GraphicsPipelineCInfo extends PipelineCInfo  {
        public LinkedHashMap<Integer, ByteBuffer> sourceMap = new LinkedHashMap<Integer, ByteBuffer>();
        public ImageSetCInfo.FBLayout fbLayout = new ImageSetCInfo.FBLayout();
    }

}
