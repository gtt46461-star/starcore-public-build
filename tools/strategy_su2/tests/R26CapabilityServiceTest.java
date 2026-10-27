package com.ethk3.novaonepro.strategyv2;
import java.util.*;
/** Independent CPU-share and bounded starvation checks with unequal per-task costs. */
public final class R26CapabilityServiceTest {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args){
        CapabilityServiceR26 scheduler=new CapabilityServiceR26();String[] ids={"foundry-su2","live-model-training","expert-E03","expert-E04","legacy-generation"};long[] costs={120,60,15,90,45},served=new long[5];int[] last=new int[5],maxGap=new int[5];
        for(int tick=0;tick<10000;tick++){int best=-1;double score=Double.POSITIVE_INFINITY;for(int n=0;n<ids.length;n++){double candidate=scheduler.score(ids[n],CapabilityServiceR26.weight(ids[n],24,24));if(candidate<score){best=n;score=candidate;}}
            scheduler.served(ids[best],"P2",costs[best]);served[best]+=costs[best];maxGap[best]=Math.max(maxGap[best],tick-last[best]);last[best]=tick;
        }
        long total=Arrays.stream(served).sum();for(int n=0;n<ids.length;n++){double expected=CapabilityServiceR26.weight(ids[n],24,24)/51.0,actual=served[n]/(double)total;check(Math.abs(actual-expected)<.002,"mechanism CPU share "+ids[n]+":"+actual+"/"+expected);check(maxGap[n]<400,"ready task starved: "+ids[n]);}
        CapabilityServiceR26 reserve=new CapabilityServiceR26();reserve.served("schema-base","P1",800);check(reserve.needsP2(),"P2 reserve ignored long P1 work");reserve.served("foundry-su2","P2",199);check(reserve.needsP2(),"P2 reserve rounded up prematurely");reserve.served("foundry-su2","P2",1);check(!reserve.needsP2(),"P2 reserve did not release P1/P3 at 20 percent");
        CapabilityServiceR26 empty=new CapabilityServiceR26();empty.served("empty","P2",0);check(empty.score("empty",1)>empty.score("never-served",1),"empty task can spin without service debit");check(!empty.needsP2(),"unmeasured CPU invented a reserve denominator");
        System.out.println("R26_CAPABILITY_SERVICE_PASS cpu="+Arrays.toString(served)+" maxClaimGaps="+Arrays.toString(maxGap));
    }
}
