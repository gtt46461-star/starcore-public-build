package com.ethk3.novaonepro.strategyv2;
import java.util.*;
/** Explicit gap policies for known kernels; an unsupported legacy model is never silently reseeded. */
public final class TemporalResetR26 {
    private TemporalResetR26() {}
    public static boolean supported(OnlineModelsSU2.Model m){return m instanceof TemporalModelR26||m instanceof OnlineModelsSU2.Hmm||m instanceof OnlineModelsSU2.RegMem||m instanceof OnlineModelsSU2.Bocpd||m instanceof OnlineModelsSU2.RelBayes||m instanceof OnlineModelsSU2.MsDir||m instanceof OnlineModelsSU2.LogLin||m instanceof OnlineModelsSU2.RelMem;}
    public static void reset(OnlineModelsSU2.Model m){
        if(m instanceof TemporalModelR26){((TemporalModelR26)m).resetTemporalContext();return;}
        if(m instanceof OnlineModelsSU2.Hmm){OnlineModelsSU2.Hmm x=(OnlineModelsSU2.Hmm)m;Arrays.fill(x.a,1.0/x.K);Arrays.fill(x.pi,0);Arrays.fill(x.ll,0);return;}
        if(m instanceof OnlineModelsSU2.RegMem){OnlineModelsSU2.RegMem x=(OnlineModelsSU2.RegMem)m;Arrays.fill(x.a,1.0/x.S1);Arrays.fill(x.pi,0);Arrays.fill(x.ll,0);return;}
        if(m instanceof OnlineModelsSU2.Bocpd){OnlineModelsSU2.Bocpd x=(OnlineModelsSU2.Bocpd)m;Arrays.fill(x.r,0);Arrays.fill(x.C,0);Arrays.fill(x.nC,0);Arrays.fill(x.l,0);for(int p=0;p<10;p++)x.r[p*x.R]=1;return;}
        if(m instanceof OnlineModelsSU2.RelBayes){OnlineModelsSU2.RelBayes x=(OnlineModelsSU2.RelBayes)m;x.seen=0;for(int[] r:x.recent)Arrays.fill(r,0);Arrays.fill(x.pred,0);return;}
        if(m instanceof OnlineModelsSU2.LogLin){OnlineModelsSU2.LogLin x=(OnlineModelsSU2.LogLin)m;x.seen=0;for(int[] r:x.recent)Arrays.fill(r,0);return;}
        if(m instanceof OnlineModelsSU2.RelMem){OnlineModelsSU2.RelMem x=(OnlineModelsSU2.RelMem)m;x.seen=0;for(int[] r:x.recent)Arrays.fill(r,0);Arrays.fill(x.a,1.0/x.S1);Arrays.fill(x.pi,0);return;}
        if(m instanceof OnlineModelsSU2.MsDir)return;
        throw new IllegalArgumentException("R26_GAP_POLICY_UNSUPPORTED:"+m.getClass().getName());
    }
    public static boolean reset(ModelLiveSU2.Hub hub,ModelLiveSU2 live){for(OnlineModelsSU2.Model m:hub.models.values())if(!supported(m))return false;for(OnlineModelsSU2.Model m:hub.models.values())reset(m);hub.predicted=false;hub.p.clear();hub.st.clear();hub.pc.clear();live.resetGateContextR26();return true;}
}
