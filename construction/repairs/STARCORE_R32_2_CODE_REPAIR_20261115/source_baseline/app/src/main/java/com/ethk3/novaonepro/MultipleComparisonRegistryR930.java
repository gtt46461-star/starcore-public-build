package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.Arrays;
import java.util.Locale;

/**
 * C06 global experiment registry and multiplicity protocol.
 * Identity never contains a filesystem path or display name; copying, renaming and archiving cannot
 * reset the statistical denominator. Alpha is debited in the registration transaction and only a
 * cancellation before the first observation may refund it.
 */
public final class MultipleComparisonRegistryR930 {
    public static final String REVISION="R930_C06_MULTIPLE_COMPARISON_V2";
    public static final double DEFAULT_FAMILY_ALPHA=0.05d;
    private MultipleComparisonRegistryR930(){}

    public enum State { RESERVED,OBSERVING,ACCEPTED,REJECTED,CANCELLED_BEFORE_OBSERVATION,ARCHIVED }

    public static final class Registration {
        public final String gameCode,schemaSha256,sourceSha256,statisticalFamily;
        public final String candidateStructuralSha256,hyperParameterSha256,domainProtocolSha256,repairParentSha256;
        public final String windowId,windowStartIssue,windowEndIssue,plannedLookId,hypothesisId;
        public Registration(String game,String schema,String source,String family,String candidate,String hyper,
                            String domain,String repairParent,String window,String start,String end,String look){
            gameCode=req(game,"GAME");schemaSha256=hash(schema,"SCHEMA");sourceSha256=hash(source,"SOURCE");
            statisticalFamily=req(family,"FAMILY");candidateStructuralSha256=hash(candidate,"CANDIDATE");
            hyperParameterSha256=hash(hyper,"HYPER");domainProtocolSha256=hash(domain,"DOMAIN");
            repairParentSha256=optionalHash(repairParent,"REPAIR_PARENT");windowId=req(window,"WINDOW");
            windowStartIssue=issue(start,"WINDOW_START");windowEndIssue=issue(end,"WINDOW_END");
            if(Batch34EvidenceContractR929.compareIssue(windowStartIssue,windowEndIssue)>0)throw new IllegalArgumentException("C06_WINDOW_ORDER_INVALID");
            plannedLookId=req(look,"PLANNED_LOOK");
            hypothesisId=V51ChecksumEngine.sha256(REVISION+"|IDENTITY|"+gameCode+'|'+schemaSha256+'|'+sourceSha256+'|'+statisticalFamily+'|'+candidateStructuralSha256+'|'+hyperParameterSha256+'|'+domainProtocolSha256+'|'+repairParentSha256+'|'+windowId+'|'+windowStartIssue+'|'+windowEndIssue+'|'+plannedLookId);
        }
    }

    public static final class Snapshot {
        public final String hypothesisId,family,windowId,evidenceSha256;public final long globalIndex;
        public final double alpha,rawP;public final State state;
        Snapshot(String id,String family,long index,double alpha,String window,State state,double p,String evidence){
            hypothesisId=id;this.family=family;globalIndex=index;this.alpha=alpha;windowId=window;this.state=state;rawP=p;evidenceSha256=evidence==null?"":evidence;
        }
    }

    public static final class Budget {
        public final String family;public final double initialAlpha,availableAlpha,permanentlySpentAlpha;
        public final long registeredHypotheses;public final boolean conserved;
        Budget(String family,double initial,double available,long n,double spent,boolean conserved){this.family=family;initialAlpha=initial;availableAlpha=available;registeredHypotheses=n;permanentlySpentAlpha=spent;this.conserved=conserved;}
    }

    public static Snapshot reserve(final DbStore store,final Registration r,final double allocation,final double initialAlpha){
        if(r!=null && "CP3_ALL_PRODUCTION".equals(r.statisticalFamily))throw new IllegalArgumentException("CP4_USE_LIFETIME_RESERVATION");
        if(store==null||r==null)throw new IllegalArgumentException("C06_RESERVE_INPUT_NULL");
        if(!finite(allocation)||allocation<=0||!finite(initialAlpha)||initialAlpha<=0||allocation>initialAlpha)throw new IllegalArgumentException("C06_ALPHA_ALLOCATION_INVALID");
        final Snapshot[] out=new Snapshot[1];
        boolean committed=store.coordinatorR36().runAtomicWrite("R930_C06_RESERVE",new DatabaseCoordinator.WriteRunnable(){public void run(SQLiteDatabase db){
            Batch34SchemaR930.ensure(db);Snapshot old=read(db,r.hypothesisId);if(old!=null){if(!old.family.equals(r.statisticalFamily)||!old.windowId.equals(r.windowId)||Double.doubleToLongBits(old.alpha)!=Double.doubleToLongBits(allocation))throw new IllegalStateException("C06_HYPOTHESIS_IDENTITY_CONFLICT");out[0]=old;return;}
            Cursor c=db.rawQuery("SELECT initial_alpha,available_alpha,global_index FROM "+Batch34SchemaR930.TABLE_ALPHA_LEDGER+" WHERE family=?",new String[]{r.statisticalFamily});
            double initial=initialAlpha,available=initialAlpha;long index=0;try{if(c.moveToFirst()){initial=c.getDouble(0);available=c.getDouble(1);index=c.getLong(2);if(Math.abs(initial-initialAlpha)>1e-12)throw new IllegalStateException("C06_FAMILY_ALPHA_REDEFINITION_FORBIDDEN");}}finally{c.close();}
            if(available+1e-15<allocation)throw new IllegalStateException("C06_ALPHA_BUDGET_EXHAUSTED");long next=index+1,now=System.currentTimeMillis();
            ContentValues ledger=new ContentValues();ledger.put("family",r.statisticalFamily);ledger.put("initial_alpha",initial);ledger.put("available_alpha",available-allocation);ledger.put("global_index",next);ledger.put("updated_at",now);
            if(db.insertWithOnConflict(Batch34SchemaR930.TABLE_ALPHA_LEDGER,null,ledger,SQLiteDatabase.CONFLICT_REPLACE)<0)throw new IllegalStateException("C06_ALPHA_LEDGER_WRITE_FAILED");
            ContentValues h=new ContentValues();h.put("hypothesis_id",r.hypothesisId);h.put("family",r.statisticalFamily);h.put("global_index",next);h.put("alpha",allocation);h.put("window_id",r.windowId);h.put("state",State.RESERVED.name());h.putNull("p_value");h.put("evidence_sha256","");h.put("reserved_at",now);h.put("decided_at",0L);
            if(db.insertWithOnConflict(Batch34SchemaR930.TABLE_HYPOTHESIS,null,h,SQLiteDatabase.CONFLICT_ABORT)<0)throw new IllegalStateException("C06_HYPOTHESIS_INSERT_FAILED");
            appendEvent(db,r,"REGISTER",r.plannedLookId,Double.NaN,V51ChecksumEngine.sha256("C06_RESERVED|"+r.hypothesisId));out[0]=new Snapshot(r.hypothesisId,r.statisticalFamily,next,allocation,r.windowId,State.RESERVED,Double.NaN,"");
        }});
        if(!committed||out[0]==null)throw new IllegalStateException("C06_RESERVE_NOT_COMMITTED");return out[0];
    }

    /** Only the pre-registered terminal look may decide; exact replay is idempotent. */
    public static Snapshot observeAndDecide(final DbStore store,final Registration r,final String look,final double rawP,final String evidenceHash){
        if(store==null||r==null)throw new IllegalArgumentException("C06_OBSERVE_INPUT_NULL");if(!r.plannedLookId.equals(req(look,"LOOK")))throw new IllegalArgumentException("C06_UNREGISTERED_LOOK_REJECTED");if(!prob(rawP))throw new IllegalArgumentException("C06_RAW_P_RANGE");final String evidence=hash(evidenceHash,"EVIDENCE");final Snapshot[] out=new Snapshot[1];
        boolean committed=store.coordinatorR36().runAtomicWrite("R930_C06_DECIDE",new DatabaseCoordinator.WriteRunnable(){public void run(SQLiteDatabase db){
            Batch34SchemaR930.ensure(db);Snapshot old=read(db,r.hypothesisId);if(old==null)throw new IllegalStateException("C06_HYPOTHESIS_NOT_REGISTERED");
            if(old.state==State.ACCEPTED||old.state==State.REJECTED||old.state==State.ARCHIVED){if(Double.doubleToLongBits(old.rawP)!=Double.doubleToLongBits(rawP)||!old.evidenceSha256.equals(evidence))throw new IllegalStateException("C06_TERMINAL_RESULT_IMMUTABLE");out[0]=old;return;}
            if(old.state==State.CANCELLED_BEFORE_OBSERVATION)throw new IllegalStateException("C06_CANCELLED_HYPOTHESIS_CANNOT_OBSERVE");State terminal=rawP<=old.alpha?State.ACCEPTED:State.REJECTED;ContentValues v=new ContentValues();v.put("state",terminal.name());v.put("p_value",rawP);v.put("evidence_sha256",evidence);v.put("decided_at",System.currentTimeMillis());
            int rows=db.update(Batch34SchemaR930.TABLE_HYPOTHESIS,v,"hypothesis_id=? AND state IN (?,?)",new String[]{r.hypothesisId,State.RESERVED.name(),State.OBSERVING.name()});if(rows!=1)throw new IllegalStateException("C06_DECISION_CAS_FAILED");appendEvent(db,r,"TERMINAL_LOOK",look,rawP,evidence);out[0]=new Snapshot(old.hypothesisId,old.family,old.globalIndex,old.alpha,old.windowId,terminal,rawP,evidence);
        }});if(!committed||out[0]==null)throw new IllegalStateException("C06_DECISION_NOT_COMMITTED");return out[0];
    }

    /** Refund is legal only before observation; the immutable hypothesis row remains. */
    public static Snapshot cancelBeforeObservation(final DbStore store,final Registration r,final String reasonHash){
        if(store==null||r==null)throw new IllegalArgumentException("C06_CANCEL_INPUT_NULL");final String reason=hash(reasonHash,"CANCEL_REASON");final Snapshot[] out=new Snapshot[1];
        boolean committed=store.coordinatorR36().runAtomicWrite("R930_C06_CANCEL",new DatabaseCoordinator.WriteRunnable(){public void run(SQLiteDatabase db){Batch34SchemaR930.ensure(db);Snapshot old=read(db,r.hypothesisId);if(old==null)throw new IllegalStateException("C06_HYPOTHESIS_NOT_REGISTERED");if(old.state==State.CANCELLED_BEFORE_OBSERVATION){out[0]=old;return;}if(old.state!=State.RESERVED)throw new IllegalStateException("C06_ALPHA_REFUND_AFTER_OBSERVATION_FORBIDDEN");ContentValues v=new ContentValues();v.put("state",State.CANCELLED_BEFORE_OBSERVATION.name());v.put("evidence_sha256",reason);v.put("decided_at",System.currentTimeMillis());if(db.update(Batch34SchemaR930.TABLE_HYPOTHESIS,v,"hypothesis_id=? AND state=?",new String[]{r.hypothesisId,State.RESERVED.name()})!=1)throw new IllegalStateException("C06_CANCEL_CAS_FAILED");db.execSQL("UPDATE "+Batch34SchemaR930.TABLE_ALPHA_LEDGER+" SET available_alpha=MIN(initial_alpha,available_alpha+?),updated_at=? WHERE family=?",new Object[]{Double.valueOf(old.alpha),Long.valueOf(System.currentTimeMillis()),old.family});appendEvent(db,r,"CANCEL_BEFORE_OBSERVATION",r.plannedLookId,Double.NaN,reason);out[0]=new Snapshot(old.hypothesisId,old.family,old.globalIndex,old.alpha,old.windowId,State.CANCELLED_BEFORE_OBSERVATION,Double.NaN,reason);}});
        if(!committed||out[0]==null)throw new IllegalStateException("C06_CANCEL_NOT_COMMITTED");return out[0];
    }

    /** Archive never refunds alpha or removes a registered hypothesis. */
    public static Snapshot archive(final DbStore store,final Registration r,final String evidenceHash){
        if(store==null||r==null)throw new IllegalArgumentException("C06_ARCHIVE_INPUT_NULL");final String evidence=hash(evidenceHash,"ARCHIVE_EVIDENCE");final Snapshot[] out=new Snapshot[1];
        boolean committed=store.coordinatorR36().runAtomicWrite("R930_C06_ARCHIVE",new DatabaseCoordinator.WriteRunnable(){public void run(SQLiteDatabase db){Batch34SchemaR930.ensure(db);Snapshot old=read(db,r.hypothesisId);if(old==null)throw new IllegalStateException("C06_HYPOTHESIS_NOT_REGISTERED");if(old.state==State.RESERVED||old.state==State.OBSERVING)throw new IllegalStateException("C06_ACTIVE_HYPOTHESIS_CANNOT_ARCHIVE_TO_ESCAPE_DENOMINATOR");if(old.state==State.ARCHIVED){out[0]=old;return;}ContentValues v=new ContentValues();v.put("state",State.ARCHIVED.name());v.put("evidence_sha256",evidence);if(db.update(Batch34SchemaR930.TABLE_HYPOTHESIS,v,"hypothesis_id=? AND state IN (?,?,?)",new String[]{r.hypothesisId,State.ACCEPTED.name(),State.REJECTED.name(),State.CANCELLED_BEFORE_OBSERVATION.name()})!=1)throw new IllegalStateException("C06_ARCHIVE_CAS_FAILED");appendEvent(db,r,"ARCHIVE_WITHOUT_DENOMINATOR_DELETE",r.plannedLookId,old.rawP,evidence);out[0]=new Snapshot(old.hypothesisId,old.family,old.globalIndex,old.alpha,old.windowId,State.ARCHIVED,old.rawP,evidence);}});
        if(!committed||out[0]==null)throw new IllegalStateException("C06_ARCHIVE_NOT_COMMITTED");return out[0];
    }

    public static Budget budget(final DbStore store,final String familyName){
        if(store==null)throw new IllegalArgumentException("C06_BUDGET_STORE_NULL");final String family=req(familyName,"FAMILY");final Budget[] out=new Budget[1];
        boolean committed=store.coordinatorR36().runAtomicWrite("R930_C06_BUDGET_READ",new DatabaseCoordinator.WriteRunnable(){public void run(SQLiteDatabase db){Batch34SchemaR930.ensure(db);Cursor c=db.rawQuery("SELECT initial_alpha,available_alpha,global_index FROM "+Batch34SchemaR930.TABLE_ALPHA_LEDGER+" WHERE family=?",new String[]{family});double initial=0,available=0;long n=0;try{if(c.moveToFirst()){initial=c.getDouble(0);available=c.getDouble(1);n=c.getLong(2);}}finally{c.close();}Cursor s=db.rawQuery("SELECT COALESCE(SUM(alpha),0) FROM "+Batch34SchemaR930.TABLE_HYPOTHESIS+" WHERE family=? AND state<>?",new String[]{family,State.CANCELLED_BEFORE_OBSERVATION.name()});double spent=0;try{if(s.moveToFirst())spent=s.getDouble(0);}finally{s.close();}boolean conserved=initial>=-1e-12&&available>=-1e-12&&available<=initial+1e-12&&spent<=initial+1e-12&&available+spent<=initial+1e-9;out[0]=new Budget(family,initial,available,n,spent,conserved);}});
        if(!committed||out[0]==null)throw new IllegalStateException("C06_BUDGET_READ_FAILED");return out[0];
    }

    public static double bonferroniAdjustedP(double rawP,int familySize){if(!prob(rawP)||familySize<1)throw new IllegalArgumentException("C06_BONFERRONI_INPUT");return Math.min(1.0d,rawP*familySize);}

    /** BH q-values are returned separately and never mislabeled as FWER-adjusted p-values. */
    public static double[] benjaminiHochbergQ(final double[] rawP){
        if(rawP==null||rawP.length==0)throw new IllegalArgumentException("C06_BH_INPUT_EMPTY");Integer[] order=new Integer[rawP.length];for(int i=0;i<rawP.length;i++){if(!prob(rawP[i]))throw new IllegalArgumentException("C06_BH_P_RANGE");order[i]=Integer.valueOf(i);}Arrays.sort(order,(a,b)->{int c=Double.compare(rawP[a.intValue()],rawP[b.intValue()]);return c!=0?c:Integer.compare(a.intValue(),b.intValue());});double[] sorted=new double[rawP.length],out=new double[rawP.length];double running=1;for(int rank=rawP.length;rank>=1;rank--){running=Math.min(running,Math.min(1.0d,rawP[order[rank-1].intValue()]*rawP.length/rank));sorted[rank-1]=running;}for(int rank=0;rank<order.length;rank++)out[order[rank].intValue()]=sorted[rank];return out;
    }

    private static Snapshot read(SQLiteDatabase db,String id){Cursor c=db.rawQuery("SELECT hypothesis_id,family,global_index,alpha,window_id,state,p_value,evidence_sha256 FROM "+Batch34SchemaR930.TABLE_HYPOTHESIS+" WHERE hypothesis_id=?",new String[]{id});try{if(!c.moveToFirst())return null;double p=c.isNull(6)?Double.NaN:c.getDouble(6);return new Snapshot(c.getString(0),c.getString(1),c.getLong(2),c.getDouble(3),c.getString(4),State.valueOf(c.getString(5)),p,c.getString(7));}finally{c.close();}}

    private static void appendEvent(SQLiteDatabase db,Registration r,String type,String look,double p,String evidence){String detail=REVISION+"|event="+type+"|look="+look+"|raw_p="+(finite(p)?String.format(Locale.US,"%.12g",p):"N/A")+"|window="+r.windowId+"|start="+r.windowStartIssue+"|end="+r.windowEndIssue;String id=V51ChecksumEngine.sha256(r.hypothesisId+'|'+type+'|'+look+'|'+evidence+'|'+detail);ContentValues v=new ContentValues();v.put("evidence_id",id);v.put("game_code",r.gameCode);v.put("target_issue",r.windowEndIssue);v.put("evidence_type","C06_"+type);v.put("owner",r.hypothesisId);v.put("payload_hash",evidence);v.put("detail",detail);v.put("created_at",System.currentTimeMillis());long inserted=db.insertWithOnConflict(Batch34SchemaR930.TABLE_EVIDENCE_LINK,null,v,SQLiteDatabase.CONFLICT_IGNORE);if(inserted<0){Cursor c=db.rawQuery("SELECT payload_hash,detail FROM "+Batch34SchemaR930.TABLE_EVIDENCE_LINK+" WHERE evidence_id=?",new String[]{id});try{if(!c.moveToFirst()||!evidence.equals(c.getString(0))||!detail.equals(c.getString(1)))throw new IllegalStateException("C06_EVENT_IDEMPOTENCY_CONFLICT");}finally{c.close();}}}
    private static boolean prob(double x){return finite(x)&&x>=0&&x<=1;}private static boolean finite(double x){return !Double.isNaN(x)&&!Double.isInfinite(x);}private static String issue(String x,String f){String v=req(x,f);if(!Batch34EvidenceContractR929.validIssue(v))throw new IllegalArgumentException("C06_"+f+"_INVALID");return v;}private static String optionalHash(String x,String f){String v=clean(x).toLowerCase(Locale.US);if(v.length()==0)return "";if(!Batch34EvidenceContractR929.isSha256(v))throw new IllegalArgumentException("C06_"+f+"_INVALID");return v;}private static String hash(String x,String f){String v=req(x,f).toLowerCase(Locale.US);if(!Batch34EvidenceContractR929.isSha256(v))throw new IllegalArgumentException("C06_"+f+"_INVALID");return v;}private static String req(String x,String f){String v=clean(x);if(v.length()==0)throw new IllegalArgumentException("C06_"+f+"_EMPTY");return v;}private static String clean(String x){return x==null?"":x.trim();}

    /** CP4 target adapter; one hypothesis for the entire frozen protocol lifetime. */
    static Snapshot reserveCp3LifetimeWithinTransaction(SQLiteDatabase db,Registration r){
        if(db==null||!db.inTransaction()||r==null)throw new IllegalStateException("CP4_REGISTRATION_TX_REQUIRED");
        if(!"CP3_ALL_PRODUCTION".equals(r.statisticalFamily)||!"CP3_PROTOCOL_FINAL".equals(r.plannedLookId))throw new IllegalArgumentException("CP4_REGISTRATION_PROTOCOL");
        Batch34SchemaR930.ensure(db);
        Snapshot old=read(db,r.hypothesisId);
        if(old!=null){if(!old.family.equals(r.statisticalFamily)||!old.windowId.equals(r.windowId))throw new IllegalStateException("CP4_REGISTRATION_IDENTITY");return old;}
        double available=.05;long index=0;
        try(Cursor c=db.rawQuery("SELECT initial_alpha,available_alpha,global_index FROM "+Batch34SchemaR930.TABLE_ALPHA_LEDGER+" WHERE family=?",new String[]{r.statisticalFamily})){
            if(c.moveToFirst()){if(Double.compare(c.getDouble(0),.05)!=0)throw new IllegalStateException("CP4_ALPHA_REDEFINITION");available=c.getDouble(1);index=c.getLong(2);}
        }
        if(index<0||index==Long.MAX_VALUE)throw new IllegalStateException("CP4_ALPHA_INDEX_EXHAUSTED");
        long next=index+1;double alpha=.05/(double)next/((double)next+1d);
        if(!finite(alpha)||alpha<=0||available+1e-15<alpha)throw new IllegalStateException("CP4_ALPHA_BUDGET_EXHAUSTED");
        com.ethk3.novaonepro.strategyv2.AlphaLedgerSU1.reserveLegacy(db,r.hypothesisId,alpha,true);
        long now=System.currentTimeMillis();ContentValues v=new ContentValues();v.put("family",r.statisticalFamily);v.put("initial_alpha",.05);v.put("available_alpha",available-alpha);v.put("global_index",next);v.put("updated_at",now);
        if(db.insertWithOnConflict(Batch34SchemaR930.TABLE_ALPHA_LEDGER,null,v,SQLiteDatabase.CONFLICT_REPLACE)<0)throw new IllegalStateException("CP4_ALPHA_LEDGER_WRITE");
        v=new ContentValues();v.put("hypothesis_id",r.hypothesisId);v.put("family",r.statisticalFamily);v.put("global_index",next);v.put("alpha",alpha);v.put("window_id",r.windowId);v.put("state",State.RESERVED.name());v.putNull("p_value");v.put("evidence_sha256","");v.put("reserved_at",now);v.put("decided_at",0L);
        if(db.insertWithOnConflict(Batch34SchemaR930.TABLE_HYPOTHESIS,null,v,SQLiteDatabase.CONFLICT_ABORT)<0)throw new IllegalStateException("CP4_HYPOTHESIS_WRITE");
        appendEvent(db,r,"REGISTER",r.plannedLookId,Double.NaN,V51ChecksumEngine.sha256("CP4_RESERVED|"+r.hypothesisId));
        return read(db,r.hypothesisId);
    }
    static Snapshot readCp3WithinTransaction(SQLiteDatabase db,String id){return read(db,id);}
    static void markObservingWithinTransactionCp3(SQLiteDatabase db,Registration r,String captureHash){
        if(db==null||!db.inTransaction())throw new IllegalStateException("CP4_OBSERVATION_TX_REQUIRED");
        String evidence=hash(captureHash,"CAPTURE");Snapshot old=read(db,r.hypothesisId);
        if(old==null)throw new IllegalStateException("CP4_HYPOTHESIS_NOT_REGISTERED");
        if(old.state==State.OBSERVING||old.state==State.ACCEPTED)return;
        if(old.state!=State.RESERVED)throw new IllegalStateException("CP4_HYPOTHESIS_NOT_OPEN");
        ContentValues v=new ContentValues();v.put("state",State.OBSERVING.name());
        if(db.update(Batch34SchemaR930.TABLE_HYPOTHESIS,v,"hypothesis_id=? AND state=?",new String[]{r.hypothesisId,State.RESERVED.name()})!=1)throw new IllegalStateException("CP4_OBSERVING_CAS");
        appendEvent(db,r,"FIRST_OBSERVATION",r.plannedLookId,Double.NaN,evidence);
    }
    static Snapshot decideCp3WithinTransaction(SQLiteDatabase db,Registration r,double p,String evidence){
        if(db==null||!db.inTransaction()||!prob(p))throw new IllegalStateException("CP4_DECISION_TX_OR_P");
        evidence=hash(evidence,"EVIDENCE");Snapshot old=read(db,r.hypothesisId);
        if(old==null)throw new IllegalStateException("CP4_HYPOTHESIS_NOT_REGISTERED");
        if(old.state==State.ACCEPTED||old.state==State.REJECTED){if(Double.compare(old.rawP,p)!=0||!old.evidenceSha256.equals(evidence))throw new IllegalStateException("CP4_TERMINAL_IMMUTABLE");return old;}
        if(old.state!=State.OBSERVING)throw new IllegalStateException("CP4_DECISION_NOT_OBSERVING");
        State terminal=p<=old.alpha?State.ACCEPTED:State.REJECTED;ContentValues v=new ContentValues();v.put("state",terminal.name());v.put("p_value",p);v.put("evidence_sha256",evidence);v.put("decided_at",System.currentTimeMillis());
        if(db.update(Batch34SchemaR930.TABLE_HYPOTHESIS,v,"hypothesis_id=? AND state=?",new String[]{r.hypothesisId,State.OBSERVING.name()})!=1)throw new IllegalStateException("CP4_DECISION_CAS");
        appendEvent(db,r,"TERMINAL_LOOK",r.plannedLookId,p,evidence);return read(db,r.hypothesisId);
    }

}
