package com.ethk3.novaonepro;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Sealed holdout/future-live registry preventing repeated final-confirmation reuse. */
public final class ResearchHoldoutRegistryR811 {
    private final LinkedHashMap<String, Holdout> holdouts = new LinkedHashMap<String, Holdout>();

    public synchronized Holdout seal(String candidateId, String datasetHash, String freezeIssue) {
        if (safe(candidateId).length() == 0 || safe(datasetHash).length() != 64 || !TargetIssueResolver.isValid(freezeIssue)) throw new IllegalArgumentException("R811_HOLDOUT_INPUT");
        String id = V51ChecksumEngine.sha256(candidateId + '|' + datasetHash + '|' + freezeIssue);
        Holdout old = holdouts.get(id); if (old != null) return old;
        Holdout created = new Holdout(id, candidateId, datasetHash, freezeIssue, "SEALED", 0, 0L); holdouts.put(id, created); return created;
    }

    public synchronized Holdout consume(String holdoutId, String proofIssue) {
        Holdout old = holdouts.get(safe(holdoutId)); if (old == null) throw new IllegalArgumentException("R811_HOLDOUT_MISSING");
        if (!eligibleFutureLive(proofIssue, old.freezeIssue)) throw new IllegalStateException("R811_HOLDOUT_NOT_FUTURE_LIVE");
        if ("CONSUMED".equals(old.status)) {
            Holdout reused = new Holdout(old.id,old.candidateId,old.datasetHash,old.freezeIssue,old.status,old.consumeAttempts+1,old.consumedAt);
            holdouts.put(old.id,reused);
            throw new IllegalStateException("R811_HOLDOUT_ALREADY_CONSUMED");
        }
        Holdout consumed = new Holdout(old.id,old.candidateId,old.datasetHash,old.freezeIssue,"CONSUMED",1,TimeSourceR811.wallNowMs()); holdouts.put(old.id,consumed); return consumed;
    }

    public static boolean eligibleFutureLive(String targetIssue, String freezeIssue) { return TargetIssueResolver.isValid(targetIssue) && TargetIssueResolver.isValid(freezeIssue) && TargetIssueResolver.compare(targetIssue, freezeIssue) > 0; }
    public synchronized Audit auditReuse() { int reused=0, consumed=0; for(Holdout h:holdouts.values()){if("CONSUMED".equals(h.status))consumed++;if(h.consumeAttempts>1)reused++;} return new Audit(reused==0,reused,consumed,holdouts.size()); }
    public synchronized Map<String,Holdout> snapshot(){return Collections.unmodifiableMap(new LinkedHashMap<String,Holdout>(holdouts));}
    public static final class Holdout { public final String id,candidateId,datasetHash,freezeIssue,status; public final int consumeAttempts; public final long consumedAt; Holdout(String id,String c,String d,String f,String s,int a,long at){this.id=id;candidateId=c;datasetHash=d;freezeIssue=f;status=s;consumeAttempts=a;consumedAt=at;} }
    public static final class Audit { public final boolean passed; public final int reuseCount,consumedCount,total; Audit(boolean p,int r,int c,int t){passed=p;reuseCount=r;consumedCount=c;total=t;} }
    private static String safe(String x){return x==null?"":x.trim();}
}
