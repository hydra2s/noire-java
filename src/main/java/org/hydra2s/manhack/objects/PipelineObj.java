package org.hydra2s.manhack.objects;

public class PipelineObj extends BasicObj  {
    public PipelineObj(Handle base, Handle handler) {
        super(base, handler);
    }

    public static class ComputePipelineObj extends PipelineObj {

        public ComputePipelineObj(Handle base, Handle handler) {
            super(base, handler);
            //TODO Auto-generated constructor stub
        }
        
    }

    public static class GraphicsPipelineObj extends PipelineObj {

        public GraphicsPipelineObj(Handle base, Handle handler) {
            super(base, handler);
            //TODO Auto-generated constructor stub
        }
        
    }
}
