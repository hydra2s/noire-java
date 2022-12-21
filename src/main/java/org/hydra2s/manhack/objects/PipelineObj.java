package org.hydra2s.manhack.objects;

public class PipelineObj extends BasicObj  {
    public PipelineObj(Handle base, Handle handle) {
        super(base, handle);
    }

    public static class ComputePipelineObj extends PipelineObj {

        public ComputePipelineObj(Handle base, Handle handle) {
            super(base, handle);
            //TODO Auto-generated constructor stub
        }
        
    }

    public static class GraphicsPipelineObj extends PipelineObj {

        public GraphicsPipelineObj(Handle base, Handle handle) {
            super(base, handle);
            //TODO Auto-generated constructor stub
        }
        
    }
}
