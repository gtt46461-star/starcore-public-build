package com.ethk3.novaonepro;

/** Monotonic cadence for the existing single-flight fetch owner. Healthy starts are about one second apart;
 * failed transports/data validation back off with bounded jitter. Manual recovery can still request a wake. */
public final class FetchCadenceR29 {
    private long started=-1;private int failures;private long jitter=0x29F37C1L;
    public synchronized void started(long mono){started=mono;}
    public synchronized void success(){failures=0;}
    public synchronized void failure(long mono){if(started<0)started=mono;failures=Math.min(12,failures+1);jitter^=jitter<<13;jitter^=jitter>>>7;jitter^=jitter<<17;}
    public synchronized long nextDelay(long mono){
        long interval=1000;
        if(failures>0){long base=Math.min(30_000L,1000L<<Math.min(5,failures-1));interval=Math.min(30_000L,Math.max(1000L,base+(Math.floorMod(jitter,301)-150)*base/1000));}
        if(started<0||mono<started)return interval;return Math.max(20,interval-(mono-started));
    }
    public synchronized boolean backingOff(){return failures>0;}
}
