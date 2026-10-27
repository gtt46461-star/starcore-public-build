package com.ethk3.novaonepro.strategyv2;

/** Calendar fixed at publication. Missed or failed windows never recycle earlier alpha. */
public final class QualificationWindowR26 {
    private QualificationWindowR26() {}
    public static final String PROTOCOL="R26_PREREGISTERED_ROLLING_LCB_V2";
    public static final int LENGTH=4000, STRIDE=720;
    public static final class Window {
        public final long from,to; public final int look;
        private Window(long from,long to,int look){this.from=from;this.to=to;this.look=look;}
    }
    /** seq is the next unresolved target, so every ordinal strictly below it is available. */
    public static Window completed(long birth,long seq){
        if(birth<0||seq<0)throw new IllegalArgumentException("R26_COHORT_CALENDAR");
        if(seq-birth<LENGTH)return null;
        long index=(seq-birth-LENGTH)/STRIDE;
        if(index>=Integer.MAX_VALUE)throw new IllegalArgumentException("R26_LOOK_EXHAUSTED");
        long from=Math.addExact(birth,Math.multiplyExact(index,(long)STRIDE));
        return new Window(from,Math.addExact(from,LENGTH),(int)index+1);
    }
}
