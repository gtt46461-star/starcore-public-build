package com.ethk3.novaonepro.strategyv2;

/** Index ranges are inclusive; persistent absolute ordinals are assigned by the dataset adapter. */
public final class EraManagerSU1 {
    private EraManagerSU1() {}
    public static final int MIN_SEALED=1440,VAL_LEN=1000,DEV_LEN=3200,WARMUP=1500;
    public static final class Eras {
        public final long devFirst,devEnd,valFirst,valEnd,sealedFirst,sealedEnd;
        Eras(long a,long b,long c,long d,long e,long f){devFirst=a;devEnd=b;valFirst=c;valEnd=d;sealedFirst=e;sealedEnd=f;}
        public boolean hasSealed(){return sealedFirst>=0;}
    }
    public static Eras plan(long through,long lastOpened){
        long first=Math.max(lastOpened+1,through-2125+1);boolean sealed=through-first+1>=MIN_SEALED;
        long valEnd=sealed?first-1:through,valFirst=valEnd-VAL_LEN+1,devEnd=valFirst-1,devFirst=Math.max(WARMUP,devEnd-DEV_LEN+1);
        if(devEnd-devFirst+1<300)throw new IllegalArgumentException("SU1_INSUFFICIENT_ERA_HISTORY");
        return new Eras(devFirst,devEnd,valFirst,valEnd,sealed?first:-1,sealed?through:-1);
    }
}
