package org.hydra2s.manhack.descriptors;

import java.nio.ByteBuffer;
import java.util.HashMap;

public class PipelineCInfo extends BasicCInfo  {

    public long pipelineLayout = 0;

    static public class ComputePipelineCInfo extends PipelineCInfo {
        public ByteBuffer computeCode = null;
    }

    static public class GraphicsPipelineCInfo extends PipelineCInfo  {
        public HashMap<Integer, ByteBuffer> sourceMap = new HashMap<Integer, ByteBuffer>();
    }

}
