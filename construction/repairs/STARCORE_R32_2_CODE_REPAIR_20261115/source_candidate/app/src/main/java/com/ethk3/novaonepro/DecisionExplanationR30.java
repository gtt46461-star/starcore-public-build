package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.strategyv2.*;
import java.util.*;

/** Read-only collection after the original owner returns; the only writes append diagnostic receipts. */
public final class DecisionExplanationR30 {
    private DecisionExplanationR30() {}
    public static void afterV51(DbStore store,V51CompiledActionContract compiled,V51Models.AuthorizedDecision original) {
        if(store==null||compiled==null||compiled.selected==null||compiled.selected.context==null)return;
        V51Models.PredictionContext c=compiled.selected.context;
        try {
            Map<String,Object> sources=new LinkedHashMap<>();
            DecisionChainR30.Facts facts=collect(store,c.baseIssue,c.targetIssue,c.historyMaxIssue,c.historyHash,c.historyRows,compiled.certifiedPrediction,sources);
            if(compiled.certifiedPrediction!=null) {
                CertifiedPredictionR974.Proof proof=compiled.certifiedPrediction;
                String verification=CertifiedPredictionR974.verifyCertificate(store,proof);
                if(!verification.isEmpty())facts.certificateActive=false;
                if(!proof.matches(compiled.selected))facts.freezeComplete=false;
                Object candidate=sources.get("candidate"),certificate=sources.get("certificate"),model=sources.get("modelHash");
                boolean same=proof.candidate.equals(candidate)&&proof.certificate.equals(certificate)&&proof.modelHash.equals(model);
                if(!same){facts.selectorConfirmed=false;facts.modelAvailable=false;}
                sources.put("typedProofHash",proof.hash);sources.put("typedCertificateVerification",verification.isEmpty()?"PASS":verification);
            }
            record(store,c.targetIssue,"V51_B12",compiled.checksum,original!=null&&original.action==V51Enums.DecisionAction.BET,original==null?"NO_AUTHORIZED_DECISION":original.rejectCode.name(),facts,sources);
        }catch(RuntimeException error){failed("V51_B12",c.targetIssue,error);}
    }
    public static void afterV50(DbStore store,V50CompiledActionContract compiled,V50Models.AuthorizedDecision original) {
        if(store==null||compiled==null||compiled.envelope==null||compiled.envelope.context==null)return;
        V50Models.PredictionContext c=compiled.envelope.context;
        try {
            Map<String,Object> sources=new LinkedHashMap<>();
            DecisionChainR30.Facts facts=collect(store,c.baseIssue,c.targetIssue,c.historyMaxIssue,c.historyHash,-1,null,sources);
            sources.put("typedAuthorizationScope","V50_ORIGINAL_OWNER");
            record(store,c.targetIssue,"V50",compiled.checksum,original!=null&&original.action==V50Enums.DecisionAction.BET,original==null?"NO_AUTHORIZED_DECISION":original.rejectCode.name(),facts,sources);
        }catch(RuntimeException error){failed("V50",c.targetIssue,error);}
    }
    static DecisionChainR30.Facts collect(DbStore store,String base,String target,String historyMax,String historyHash,int historyRows,CertifiedPredictionR974.Proof proof,Map<String,Object> sources) {
        SQLiteDatabase db=store.readable();
        if(db.inTransaction())throw new IllegalStateException("R30_EXPLAIN_READ_IN_WRITER");
        Long before=generation(db,sources);long now=System.currentTimeMillis();
        long seq=TargetIssueResolver.sequenceOrdinalSU1(target);
        MainlineBridgeSU2.ProjectionR30 p=MainlineBridgeSU2.projectionR30(db,seq,now,proof==null?null:proof.candidate);
        sources.putAll(p.sources);sources.put("member",p.member);sources.put("candidate",p.candidate);sources.put("certificate",p.certificate);sources.put("modelHash",p.modelHash);sources.put("sourceGeneration",before);
        sources.put("historyHash",historyHash);sources.put("baseIssue",base);sources.put("historyRows",historyRows);
        DecisionChainR30.Facts facts=new DecisionChainR30.Facts();
        facts.certificateActive=p.certificateActive;facts.freezeComplete=p.freezeComplete;facts.modelAvailable=p.modelAvailable;facts.selectorConfirmed=p.selectorConfirmed;facts.actRuleFired=p.actRuleFired;
        facts.dataHealthy=TargetIssueResolver.exact(base,target).valid&&base.equals(historyMax)&&historyHash!=null&&!historyHash.isEmpty();
        try(Cursor c=db.rawQuery("SELECT issue,dirty,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10 FROM draws ORDER BY issue DESC LIMIT 1",null)){
            if(!c.moveToFirst())facts.dataHealthy=false;
            else {boolean[] seen=new boolean[11];boolean valid=c.getInt(1)==0&&base.equals(c.getString(0));for(int i=2;i<12;i++){int n=c.getInt(i);if(n<1||n>10||seen[n])valid=false;else seen[n]=true;}facts.dataHealthy=facts.dataHealthy&&valid;}
        }catch(RuntimeException missing){facts.dataHealthy=null;sources.put("drawSourceError",missing.getClass().getSimpleName());}
        if(Boolean.TRUE.equals(facts.dataHealthy)) {
            if(historyRows<=0||historyRows>5000){facts.dataHealthy=null;sources.put("historyScope","UNKNOWN_OUTSIDE_HOT_SNAPSHOT");}
            else try(Cursor c=db.rawQuery("SELECT issue FROM draws WHERE dirty=0 AND issue<=? ORDER BY issue DESC LIMIT "+historyRows,new String[]{base})){
                int n=0;long expected=TargetIssueResolver.sequenceOrdinalSU1(base);while(c.moveToNext()){if(TargetIssueResolver.sequenceOrdinalSU1(c.getString(0))!=expected-n)facts.dataHealthy=false;n++;}
                if(n<historyRows){facts.dataHealthy=null;sources.put("historyScope","UNKNOWN_REQUIRES_ARCHIVED_INPUT");}
            }catch(RuntimeException missing){facts.dataHealthy=null;sources.put("historySourceError",missing.getClass().getSimpleName());}
        }
        try{facts.onePerIssue=store.getTradeByTargetIssue(target)==null;}catch(RuntimeException missing){sources.put("tradeSourceError",missing.getClass().getSimpleName());}
        try(Cursor c=db.rawQuery("SELECT 1 FROM "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+" WHERE target_issue=? AND decision_action IN ('BET','REAL','REAL_TRADE') LIMIT 1",new String[]{target})){
            if(c.moveToFirst())facts.onePerIssue=false;sources.put("duplicateScope","LEGACY_TRADE_AND_CANONICAL_REAL");
        }catch(RuntimeException missing){if(!Boolean.FALSE.equals(facts.onePerIssue))facts.onePerIssue=null;sources.put("canonicalRealSourceError",missing.getClass().getSimpleName());}
        try{facts.userRealEnabled="1".equals(store.getState(RealAuthoritySU2.USER_SWITCH,"0"));}catch(RuntimeException missing){sources.put("userSwitchSourceError",missing.getClass().getSimpleName());}
        // The typed authorization has no amount/loss/streak input; the unit comes from the existing fixed constant.
        facts.stakeFixed=Double.isFinite(AppConfig.UNIT_STAKE)&&AppConfig.UNIT_STAKE==1.0d;
        sources.put("fixedUnit",AppConfig.UNIT_STAKE);sources.put("stakeSource","AppConfig.UNIT_STAKE; typed contract carries no amount or loss/streak adjustment");
        Long after=generation(db,sources);if(before==null||after==null){facts.dataHealthy=null;sources.put("readConflict","UNKNOWN_SOURCE_GENERATION");}
        else if(!before.equals(after)){facts.dataHealthy=false;facts.freezeComplete=false;sources.put("readConflict","SOURCE_CHANGED_DURING_EXPLANATION");}
        return facts;
    }
    static Long generation(SQLiteDatabase db,Map<String,Object> sources){try{return EventFrameBuilderSU1.generation(db);}catch(RuntimeException missing){sources.put("sourceGenerationError",missing.getClass().getSimpleName());return null;}}
    static void record(DbStore store,String issue,String entry,String identity,boolean originalAllow,String reason,DecisionChainR30.Facts facts,Map<String,Object> sources) {
        DecisionExplanationStoreR30.Observation observation=DecisionExplanationStoreR30.observe(issue,entry,identity,originalAllow,reason,facts,sources,System.currentTimeMillis());
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R30_DECISION_EXPLAIN",db->{DecisionExplanationStoreR30.persistWithin(db,observation);return 0;});
    }
    static void failed(String entry,String issue,RuntimeException error){android.util.Log.e("STARCORE_R30","R30_DECISION_EXPLAIN_FAILED|"+entry+"|"+issue+"|"+error.getClass().getSimpleName(),error);}
}
