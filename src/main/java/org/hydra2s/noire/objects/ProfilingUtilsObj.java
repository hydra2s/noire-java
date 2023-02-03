package org.hydra2s.noire.objects;

import java.util.ArrayList;

public class ProfilingUtilsObj {

    public ProfilingUtilsObj() {
        this.beginTime = 0L;
        this.recordedTime = 0L;
        this.diffTime = 0L;
        this.sampledDiff = new ArrayList<Long>();
        this.recorded = false;
        this.recorded = false;
    }

    public boolean recorded = false;
    public long beginTime = 0L;
    public long recordedTime = 0L;
    public long diffTime = 0L;
    public ArrayList<Long> sampledDiff = new ArrayList<Long>();

    public long recordBeginTime() {
        recorded = false;
        beginTime = System.nanoTime();
        return beginTime;
    }

    public long recordDiffTime(String beginCode) {
        if (!recorded) {
            recordedTime = System.nanoTime();
            sampledDiff.add(diffTime = recordedTime - beginTime);
            recorded = true;
            System.out.println(beginCode);
            //System.out.println(diffTime + " (nanoseconds)");
            System.out.println(((double)(diffTime)/(double)(1000*1000)) + " (milliseconds)");
        }
        return diffTime;
    }

}
