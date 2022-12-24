package org.hydra2s.manhack.descriptors;

import java.nio.ByteBuffer;

public class PipelineCInfo extends BasicCInfo  {

    public long pipelineLayout = 0;

    static public class ComputePipelineCInfo extends PipelineCInfo {
        public ByteBuffer computeCode = null;
    }

    static public class GraphicsPipelineCInfo extends PipelineCInfo  {
    }

}
