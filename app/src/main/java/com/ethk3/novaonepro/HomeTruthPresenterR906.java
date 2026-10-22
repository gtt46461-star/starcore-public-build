package com.ethk3.novaonepro;
import java.util.concurrent.atomic.AtomicLong;
/** Latest-wins generation fence shared by the R906 home truth renderers. */
public final class HomeTruthPresenterR906 {
    private final AtomicLong requested=new AtomicLong();
    private final AtomicLong rendered=new AtomicLong();
    public long nextGeneration(){return requested.incrementAndGet();}
    public boolean accept(long generation,RuntimeTruthSnapshotR906 snapshot){
        if(snapshot==null||generation<requested.get())return false;
        long prev=rendered.get();if(generation<prev)return false;rendered.set(generation);return true;
    }
    public long renderedGeneration(){return rendered.get();}
}
