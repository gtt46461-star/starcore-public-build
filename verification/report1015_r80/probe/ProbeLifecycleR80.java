package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.r952.*;
import java.nio.file.*;
import java.lang.reflect.*;
import java.util.*;
import org.json.JSONObject;
/** Controlled synthetic fixtures; executes actual Gradle product owners and real SQLite. */
public final class ProbeLifecycleR80 {
    static int checks;
    static void ok(boolean v,String s){if(!v)throw new AssertionError(s);checks++;System.out.println("PASS "+s);}
    static long count(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){if(!c.moveToFirst())throw new AssertionError(sql);return c.getLong(0);}}
    static String text(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){if(!c.moveToFirst())throw new AssertionError(sql);return c.getString(0);}}
    static Object invoke(String name,Class<?>[] types,Object... args)throws Exception{Method m=MultiHorizonProbeR80.class.getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(null,args);}
    static String hash(String s){return V51ChecksumEngine.sha256(s);}
    static void guardNoAuthority(SQLiteDatabase db){for(String table:new String[]{"research_certificate_r929","research_candidate_cal_observation_r932",DbHelper.TABLE_TRADES})ok(count(db,"SELECT COUNT(*) FROM "+table)==0,"probe creates no authority in "+table);}
    static String capture(SQLiteDatabase db,DbStore store,String anchor,long end){String status=MultiHorizonProbeR80.captureOptional(store,MainlineCaptureR75.snapshot(anchor,TargetIssueResolver.next(anchor).targetIssue,hash("synthetic probe upstream")),end);System.out.println("OPTIONAL_CAPTURE_STATUS "+status);return status;}
    @SuppressWarnings("unchecked") static MultiHorizonProbeR80.Prepared prepared(SQLiteDatabase db,DbStore store,String anchor)throws Exception {return preparedAt(db,store,anchor,System.currentTimeMillis());}
    @SuppressWarnings("unchecked") static MultiHorizonProbeR80.Prepared preparedAt(SQLiteDatabase db,DbStore store,String anchor,long now)throws Exception {
        List<Models.Draw> history=store.getDrawsBeforeIssue(TargetIssueResolver.next(anchor).targetIssue,960);history=new ArrayList<>(history);history.sort((a,b)->a.issue.compareTo(b.issue));
        Map<String,Object> clock=(Map<String,Object>)invoke("readClock",new Class[]{SQLiteDatabase.class,String.class,long.class},db,anchor,now);
        clock.put("historyRevision",invoke("historyRevision",new Class[]{SQLiteDatabase.class,String.class},db,anchor));
        Map<String,Object> prior=(Map<String,Object>)invoke("priorDiagnostics",new Class[]{SQLiteDatabase.class,String.class,long.class},db,anchor,now);
        return MultiHorizonProbeR80.prepare(anchor,hash("upstream"),MultiHorizonProbeR80.fit(history,anchor,now),clock,prior,now);
    }
    static void actual(SQLiteDatabase db,String target,long open){db.execSQL("INSERT INTO draws(issue,open_time,fetch_time,first_seen_at,dirty,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10) VALUES(?,?,?,?,0,1,2,3,4,5,6,7,8,9,10)",new Object[]{target,open,open+1,open+1});}
    static void terminal(SQLiteDatabase db,String target,long at){db.execSQL("INSERT INTO "+DbHelper.TABLE_R36_SETTLEMENT_TASK+"(target_issue,draw_issue,status,created_at,updated_at,completed_at,draw_hash,settlement_hash) VALUES(?,?,'COMPLETED',?,?,?, ?,?)",new Object[]{target,target,at,at,at,hash("actual:"+target),hash("settlement:"+target)});}
    public static void main(String[] args)throws Exception{
        String mode=args[0];RealtimePressureGateR811.resetForTests();
        try(SQLiteDatabase db=new SQLiteDatabase(args[1],args[2])){
            DbStore store=HostProductR974.store(db);String anchor=text(db,"SELECT issue FROM draws ORDER BY issue DESC LIMIT 1"),target=TargetIssueResolver.next(anchor).targetIssue;
            ok(MultiHorizonProbeR80.readyR80(db),"normal migration installed exact probe schema");
            if(mode.equals("capture")) {
                ok(ResearchCaptureQueueR946.complete(store,target),"existing capture owner reports all mandatory obligations complete");
                String status=capture(db,store,anchor,TimeSourceR811.monoNowMs()+5000L);ok(status.equals("FROZEN_RESEARCH_ONLY"),"actual optional owner freezes four horizons: "+status);
                ok(count(db,"SELECT COUNT(*) FROM r80_probe_forecast")==4,"four original forecasts, no invented earlier consensus");
                ok(capture(db,store,anchor,TimeSourceR811.monoNowMs()+5000L).equals("ALREADY_RECORDED"),"capture replay is idempotent");
                ok(text(db,"SELECT consensus_status FROM r80_probe_opportunity").equals("NOT_RUN_MISSING_PRIOR_HORIZONS"),"missing consensus explicit rather than zero");
                ok(count(db,"SELECT COUNT(*) FROM draws")==960,"probe never expands raw history");
            }else if(mode.equals("resume")) {
                ok(count(db,"SELECT COUNT(*) FROM r80_probe_forecast")==4,"new JVM recovers original immutable captures");
                Map<String,Object> f=StrictCanonicalJson.readObject(text(db,"SELECT receipt_json FROM r80_probe_forecast WHERE horizon=1"));long open=((Number)f.get("targetBoundary")).longValue();target=(String)f.get("targetIssue");
                actual(db,target,open);terminal(db,target,open+2);Models.Draw draw=store.getDrawByIssue(target);
                int n=ResearchDirectorR952.onSettlementCompleted(store,draw,hash("settlement:"+target),open+2);
                ok(n==1,"actual existing authoritative settlement owner settles exact h1");
                ok(ResearchDirectorR952.onSettlementCompleted(store,draw,hash("settlement:"+target),open+2)==0,"authoritative settlement retry idempotent");
                ok(count(db,"SELECT COUNT(*) FROM r80_probe_label WHERE status='SETTLED'")==1,"h2/h4/h8 not mislabeled by h1 draw");
                ok(count(db,"SELECT COUNT(*) FROM r80_probe_pending WHERE settled=0")==3,"other exact horizon targets remain pending");
                Map<String,Object> report=StrictCanonicalJson.readObject(MultiHorizonProbeR80.reportJson(db));ok(Boolean.FALSE.equals(report.get("correlatedHorizonsAreIndependentSamples")),"report rejects pooled correlated N");
                Map<?,?> groups=(Map<?,?>)report.get("separateTargetDenominators");ok(((Number)((Map<?,?>)groups.get("h1")).get("settledTargets")).longValue()==1,"h1 denominator is one target");
                ok(((Map<?,?>)groups.get("h2")).get("hitRate")==null,"unsettled horizon is unavailable, not zero hit rate");
                Map<?,?> before=(Map<?,?>)invoke("priorDiagnostics",new Class[]{SQLiteDatabase.class,String.class,long.class},db,target,open+2);
                ok(((Number)((Map<?,?>)before.get("h1")).get("n")).longValue()==0,"equal settlement timestamp is excluded from prior diagnostics");
                Map<?,?> after=(Map<?,?>)invoke("priorDiagnostics",new Class[]{SQLiteDatabase.class,String.class,long.class},db,target,open+3);
                ok(((Number)((Map<?,?>)after.get("h1")).get("n")).longValue()==1,"strictly prior settled residual becomes diagnostic only");
                ok(count(db,"SELECT COUNT(*) FROM r952_research_outbox")==0,"probe residual never enters SEALED/formal/online training outbox");
            }else if(mode.equals("multihorizon")) {
                for(int horizon:new int[]{1,2,4,8}) {
                    Map<String,Object> f=StrictCanonicalJson.readObject(text(db,"SELECT receipt_json FROM r80_probe_forecast WHERE horizon="+horizon));
                    String exact=(String)f.get("targetIssue");long open=((Number)f.get("targetBoundary")).longValue();actual(db,exact,open);terminal(db,exact,open+2);
                    ok(ResearchDirectorR952.onSettlementCompleted(store,store.getDrawByIssue(exact),hash("settlement:"+exact),open+2)==1,"authoritative owner settles only original h"+horizon+" exact target");
                }
                ok(count(db,"SELECT COUNT(*) FROM r80_probe_label WHERE status='SETTLED'")==4,"all four horizons settle as four separate target records");
                Map<String,Object> report=StrictCanonicalJson.readObject(MultiHorizonProbeR80.reportJson(db));Map<?,?> groups=(Map<?,?>)report.get("separateTargetDenominators");
                for(int h:new int[]{1,2,4,8})ok(((Number)((Map<?,?>)groups.get("h"+h)).get("settledTargets")).longValue()==1,"h"+h+" denominator stays one, never pooled four");
            }else if(mode.equals("rollback")) {
                MultiHorizonProbeR80.Prepared p=prepared(db,store,anchor);db.beginTransaction();try{ok(MultiHorizonProbeR80.commit(db,p,TimeSourceR811.monoNowMs()+5000),"publication test attempts complete batch");}finally{db.endTransaction();}
                for(String table:new String[]{MultiHorizonProbeR80.INPUT,MultiHorizonProbeR80.FORECAST,MultiHorizonProbeR80.OPPORTUNITY})ok(count(db,"SELECT COUNT(*) FROM "+table)==0,"rollback removes partial "+table);
                db.beginTransaction();try{ok(MultiHorizonProbeR80.commit(db,p,TimeSourceR811.monoNowMs()+5000),"same original prepared batch succeeds after rollback");db.setTransactionSuccessful();}finally{db.endTransaction();}
            }else if(mode.equals("prefix")) {
                MultiHorizonProbeR80.Prepared p=prepared(db,store,anchor);db.execSQL("UPDATE draws SET n1=n2,n2=n1 WHERE issue=(SELECT MIN(issue) FROM draws)");
                boolean rejected=false;db.beginTransaction();try{MultiHorizonProbeR80.commit(db,p,TimeSourceR811.monoNowMs()+5000);db.setTransactionSuccessful();}catch(IllegalArgumentException e){rejected=e.getMessage().contains("PREFIX_CHANGED");}finally{db.endTransaction();}
                ok(rejected,"changed prefix generation between computation/publication rejected");ok(count(db,"SELECT COUNT(*) FROM r80_probe_forecast")==0,"changed prefix cannot publish stale model");
            }else if(mode.equals("observation")) {
                MultiHorizonProbeR80.Prepared p=prepared(db,store,anchor);
                db.execSQL("UPDATE draws SET first_seen_at=? WHERE issue=(SELECT MIN(issue) FROM draws)",new Object[]{p.frozenAt+1});
                boolean rejected=false;db.beginTransaction();try{MultiHorizonProbeR80.commit(db,p,TimeSourceR811.monoNowMs()+5000);db.setTransactionSuccessful();}catch(IllegalArgumentException e){rejected=e.getMessage().contains("PREFIX_OBSERVATION_CLOCK");}finally{db.endTransaction();}
                ok(rejected,"prefix first observation after freeze rejects publication even with an earlier fetch clock");
                ok(count(db,"SELECT COUNT(*) FROM r80_probe_forecast")==0,"unobserved historical input cannot become an original forecast");
            }else if(mode.equals("authority")) {
                ok(capture(db,store,anchor,TimeSourceR811.monoNowMs()+5000).equals("FROZEN_RESEARCH_ONLY"),"authority fixture has real optional captures");
                Map<String,Object> f=StrictCanonicalJson.readObject(text(db,"SELECT receipt_json FROM r80_probe_forecast WHERE horizon=1"));long open=((Number)f.get("targetBoundary")).longValue();target=(String)f.get("targetIssue");
                actual(db,target,open);terminal(db,target,open+2);
                boolean rejected=false;db.beginTransaction();try{MultiHorizonProbeR80.settle(db,store.getDrawByIssue(target),hash("wrong receipt"),open+2);db.setTransactionSuccessful();}catch(IllegalArgumentException e){rejected=e.getMessage().contains("AUTHORITY_MISSING");}finally{db.endTransaction();}
                ok(rejected,"wrong authority hash cannot settle a frozen forecast");
                ok(count(db,"SELECT COUNT(*) FROM r80_probe_label")==0,"failed authority leaves originals pending, never zero labels");
                db.execSQL("UPDATE draws SET fetch_time=? WHERE issue=?",new Object[]{open+3,target});
                rejected=false;db.beginTransaction();try{MultiHorizonProbeR80.settle(db,store.getDrawByIssue(target),hash("settlement:"+target),open+2);db.setTransactionSuccessful();}catch(IllegalArgumentException e){rejected=e.getMessage().contains("TERMINAL_CLOCK");}finally{db.endTransaction();}
                ok(rejected,"authority completion before actual fetch clock cannot label");
                ok(count(db,"SELECT COUNT(*) FROM r80_probe_label")==0,"future actual fetch clock leaves original capture pending");
            }else if(mode.equals("retention")) {
                ok(capture(db,store,anchor,TimeSourceR811.monoNowMs()+5000).equals("FROZEN_RESEARCH_ONLY"),"retention fixture has real original forecasts");
                Map<String,Object> f=StrictCanonicalJson.readObject(text(db,"SELECT receipt_json FROM r80_probe_forecast WHERE horizon=1"));long open=((Number)f.get("targetBoundary")).longValue();target=(String)f.get("targetIssue");
                actual(db,target,open);long rowid=count(db,"SELECT rowid FROM draws WHERE issue='"+target+"'");
                db.beginTransaction();try{DrawDependencyInputCP12.preserveIfReferencedWithinWrite(db,rowid,target);db.setTransactionSuccessful();}finally{db.endTransaction();}
                ok(count(db,"SELECT COUNT(*) FROM draw_dependency_input_cp12 WHERE issue='"+target+"'")==1,"pending probe target retains compact authoritative draw input");
                db.beginTransaction();try{ok(DrawDependencyInputCP12.releaseConsumedWithinWrite(db,32)==0,"pending original probe prevents dependency release");db.setTransactionSuccessful();}finally{db.endTransaction();}
                terminal(db,target,open+2);ok(ResearchDirectorR952.onSettlementCompleted(store,store.getDrawByIssue(target),hash("settlement:"+target),open+2)==1,"retained exact target reaches authoritative settlement");
                db.beginTransaction();try{ok(DrawDependencyInputCP12.releaseConsumedWithinWrite(db,32)==1,"terminal probe label releases consumed compact target");db.setTransactionSuccessful();}finally{db.endTransaction();}
                ok(count(db,"SELECT COUNT(*) FROM r80_probe_forecast")==4,"dependency release never deletes original forecasts");
            }else if(mode.equals("archive")) {
                ok(capture(db,store,anchor,TimeSourceR811.monoNowMs()+5000).equals("FROZEN_RESEARCH_ONLY"),"archive fixture starts with genuine prospective originals");
                String eligible=MultiHorizonProbeR80.archiveEligibleR80(MultiHorizonProbeR80.FORECAST,"f","'999999999999'");
                ok(count(db,"SELECT COUNT(*) FROM r80_probe_forecast f WHERE "+eligible)==0,"even old pending originals are not archive eligible");
                Map<String,Object> f=StrictCanonicalJson.readObject(text(db,"SELECT receipt_json FROM r80_probe_forecast WHERE horizon=1"));long open=((Number)f.get("targetBoundary")).longValue();target=(String)f.get("targetIssue");actual(db,target,open);terminal(db,target,open+2);
                ok(ResearchDirectorR952.onSettlementCompleted(store,store.getDrawByIssue(target),hash("settlement:"+target),open+2)==1,"archive fixture settles original h1");
                ok(count(db,"SELECT COUNT(*) FROM r80_probe_forecast f WHERE "+eligible)==0,"last48 settled diagnostic forecasts stay hot independently of age");
                boolean blocked=false;try{db.execSQL("DELETE FROM r80_probe_forecast WHERE horizon=1");}catch(RuntimeException e){blocked=true;}ok(blocked,"no direct prune without verified archive proof");
                String next=target;
                // Structural retention fixture only: these are not claimed as captured/settled efficacy data.
                for(int i=0;i<48;i++){next=TargetIssueResolver.next(next).targetIssue;String fh=hash("synthetic retention forecast "+i),lh=hash("synthetic retention label "+i);
                    db.execSQL("INSERT INTO r80_probe_forecast VALUES(?,1,?,?,?,?,?,?,?,?)",new Object[]{next,next,MultiHorizonProbeR80.ROLE,MultiHorizonProbeR80.PROTOCOL_HASH,hash("synthetic input"),hash("synthetic model"),fh,"{}",open});
                    db.execSQL("INSERT INTO r80_probe_label VALUES(?,?,'SETTLED','SYNTHETIC_RETENTION_ONLY',?,?,?,?,1)",new Object[]{fh,next,hash("synthetic settlement"),open+2,"{}",lh});}
                ok(count(db,"SELECT COUNT(*) FROM r80_probe_forecast f WHERE "+eligible)==1,"old terminal original releases once 48 newer same-horizon diagnostics exist");
                for(String table:new String[]{MultiHorizonProbeR80.FORECAST,MultiHorizonProbeR80.INPUT,MultiHorizonProbeR80.LABEL,MultiHorizonProbeR80.OPPORTUNITY}) {
                    String plan="";try(Cursor c=db.rawQuery("EXPLAIN QUERY PLAN SELECT 1 FROM "+table+" x WHERE x.rowid=1 AND "+MultiHorizonProbeR80.archiveEligibleR80(table,"x","'999999999999'"),null)){while(c.moveToNext())plan+=c.getString(3)+";";}
                    System.out.println("ARCHIVE_PLAN "+table+" "+plan);
                    ok(!plan.contains("SCAN c")&&!plan.contains("SCAN r")&&!plan.contains("SCAN i")&&!plan.contains("SCAN l")&&!plan.contains("SCAN f"),"archive writer dependency checks avoid history-wide scans: "+table);
                }
                String inputEligible=MultiHorizonProbeR80.archiveEligibleR80(MultiHorizonProbeR80.INPUT,"i","'999999999999'");
                ok(count(db,"SELECT COUNT(*) FROM r80_probe_input i WHERE "+inputEligible)==0,"pending sibling horizons preserve shared compact model input");
                java.io.File files=new java.io.File(Path.of(args[3]).getParent().toFile(),"archive-files");java.io.File cas=new java.io.File(files,"retention-cas-cp12");ok(cas.mkdirs(),"archive availability fixture directory");
                for(int i=0;i<3;i++){String content="synthetic original bytes "+i,sha=hash(content);String id=hash("archive availability "+i);
                    if(i!=1)Files.writeString(new java.io.File(cas,sha+".scrow").toPath(),i==2?"corrupt":content);
                    db.execSQL("INSERT INTO research_evidence_archive_v2(archive_id,source_table,first_issue,last_issue,row_count,archive_hash,relative_path,state,created_at) VALUES(?,'r80_probe_forecast',?,?,1,?,?,'VERIFIED',?)",new Object[]{id,anchor,target,sha,"retention-cas-cp12/"+sha+".scrow",open});}
                java.io.File observations=new java.io.File(files,"availability.jsonl");JSONObject availability=ReportAttachmentClosureCP12.observeR80Archives(db,files,observations,()->true);
                ok(availability.getLong("verifiedPages")==1&&availability.getLong("missingOrCorruptPages")==2,"compact report distinguishes verified, missing and corrupt archive bytes");
                String observed=Files.readString(observations.toPath());ok(observed.contains("MISSING_ARCHIVE")&&observed.contains("CORRUPT_HASH_MISMATCH"),"archive availability outcomes exported without raw body duplication");
            }else if(mode.equals("consensus")) {
                for(int h:new int[]{2,4,8}) {
                    String prior=text(db,"SELECT issue FROM draws ORDER BY issue DESC LIMIT 1 OFFSET "+(h-1));
                    long at=count(db,"SELECT open_time FROM draws WHERE issue='"+prior+"'")+1000;
                    MultiHorizonProbeR80.Prepared old=preparedAt(db,store,prior,at);Map<String,Object> input=StrictCanonicalJson.readObject(old.inputJson);
                    // Seed exact product-generated original receipts at their historical clocks.
                    // This is a controlled restart fixture, not a claim of natural device capture.
                    db.execSQL("INSERT INTO r80_probe_input VALUES(?,?,?,?,?,?,?)",new Object[]{old.anchor,MultiHorizonProbeR80.PROTOCOL_HASH,input.get("sourceHash"),old.modelHash,old.inputJson,old.inputHash,old.frozenAt});
                    for(Map<String,Object> f:old.predictions)if(((Number)f.get("horizon")).intValue()==h){String json=StrictCanonicalJson.write(f);db.execSQL("INSERT INTO r80_probe_forecast VALUES(?,?,?,?,?,?,?,?,?,?)",new Object[]{old.anchor,h,f.get("targetIssue"),MultiHorizonProbeR80.ROLE,MultiHorizonProbeR80.PROTOCOL_HASH,old.inputHash,old.modelHash,hash(json),json,old.frozenAt});}
                }
                ok(capture(db,store,anchor,TimeSourceR811.monoNowMs()+5000).equals("FROZEN_RESEARCH_ONLY"),"actual optional capture forms same-target original consensus");
                ok(count(db,"SELECT COUNT(*) FROM r80_probe_forecast WHERE horizon=0")==1,"one consensus only when all original horizons exist");
                Map<String,Object> f=StrictCanonicalJson.readObject(text(db,"SELECT receipt_json FROM r80_probe_forecast WHERE horizon=0"));long open=((Number)f.get("targetBoundary")).longValue();target=(String)f.get("targetIssue");actual(db,target,open);terminal(db,target,open+2);
                ok(ResearchDirectorR952.onSettlementCompleted(store,store.getDrawByIssue(target),hash("settlement:"+target),open+2)==5,"one authoritative target settles four independent originals plus consensus");
                Map<?,?> groups=(Map<?,?>)StrictCanonicalJson.readObject(MultiHorizonProbeR80.reportJson(db)).get("separateTargetDenominators");
                for(String name:new String[]{"h1","h2","h4","h8","fixedSameTargetConsensus"})ok(((Number)((Map<?,?>)groups.get(name)).get("settledTargets")).longValue()==1,"same target retains independent denominator for "+name);
            }else if(mode.equals("budget")) {
                MultiHorizonProbeR80.Prepared p=prepared(db,store,anchor);
                ResearchWorkBudgetR927.Scope exhausted=ResearchWorkBudgetR927.enter(1L);String reason="";
                try{Thread.sleep(5L);db.beginTransaction();try{MultiHorizonProbeR80.commit(db,p,TimeSourceR811.monoNowMs()+5000);db.setTransactionSuccessful();}catch(ResearchWorkBudgetR927.Yield yielded){reason=yielded.getMessage();}finally{db.endTransaction();}}
                finally{ResearchWorkBudgetR927.leave(exhausted);}
                ok(reason.contains("MONOTONIC_SLICE_EXHAUSTED"),"expired existing owner budget returns typed Yield: "+reason);
                for(String table:new String[]{MultiHorizonProbeR80.INPUT,MultiHorizonProbeR80.FORECAST,MultiHorizonProbeR80.OPPORTUNITY,MultiHorizonProbeR80.DIAGNOSTIC_REF})ok(count(db,"SELECT COUNT(*) FROM "+table)==0,"budget yield leaves no partial publication in "+table);
                ResearchWorkBudgetR927.Scope absent=ResearchWorkBudgetR927.enter(1L);String status;try{Thread.sleep(5L);status=capture(db,store,anchor,TimeSourceR811.monoNowMs()+5000);}finally{ResearchWorkBudgetR927.leave(absent);}
                ok(status.equals("NOT_RUN_OWNER_BUDGET_GAP"),"optional entry classifies expired owner budget explicitly");
                ok(count(db,"SELECT COUNT(*) FROM r80_probe_forecast")==0,"typed optional budget deferral never invents a forecast");
                ok(capture(db,store,anchor,TimeSourceR811.monoNowMs()+5000).equals("FROZEN_RESEARCH_ONLY"),"adequate restored owner deadline permits strict positive original capture");
            }else if(mode.equals("pressure")) {
                RealtimePressureGateR811.beginAnalysis();String result;try{result=capture(db,store,anchor,TimeSourceR811.monoNowMs()+5000);}finally{RealtimePressureGateR811.endAnalysis();}
                ok(result.startsWith("NOT_RUN"),"P0 preemption explicit: "+result);ok(count(db,"SELECT COUNT(*) FROM r80_probe_forecast")==0,"P0 never creates probe forecasts");
            }else if(mode.equals("late")) {
                String result=capture(db,store,anchor,TimeSourceR811.monoNowMs()+5000);ok(result.contains("MISSED_ORIGINAL_CLOCK"),"late absent target still refuses fabricated capture: "+result);
                ok(count(db,"SELECT COUNT(*) FROM r80_probe_forecast")==0,"late capture no forecasts");ok(text(db,"SELECT status FROM r80_probe_opportunity").equals("NOT_RUN"),"late opportunity classified explicitly");
            }else if(mode.equals("tamper")) {
                ok(count(db,"SELECT COUNT(*) FROM r80_probe_forecast")==4,"tamper test starts from real capture receipts");
                boolean blocked=false;try{db.execSQL("UPDATE r80_probe_forecast SET receipt_json='{}'");}catch(RuntimeException e){blocked=true;}ok(blocked,"immutable trigger rejects receipt modification");
                blocked=false;try{db.execSQL("INSERT OR REPLACE INTO r80_probe_forecast SELECT * FROM r80_probe_forecast");}catch(RuntimeException e){blocked=true;}ok(blocked,"replacement cannot erase original receipt");
                Map<String,Object> f=StrictCanonicalJson.readObject(text(db,"SELECT receipt_json FROM r80_probe_forecast WHERE horizon=1"));long open=((Number)f.get("targetBoundary")).longValue();target=(String)f.get("targetIssue");
                // Explicit hostile-storage fixture drops one guard to exercise cryptographic verification.
                db.execSQL("DROP TRIGGER r80_probe_forecast_no_update");db.execSQL("UPDATE r80_probe_forecast SET receipt_json='{}' WHERE horizon=1");
                db.beginTransaction();try{MultiHorizonProbeR80.schemaR80(db);db.setTransactionSuccessful();}finally{db.endTransaction();}
                actual(db,target,open);terminal(db,target,open+2);int n=ResearchDirectorR952.onSettlementCompleted(store,store.getDrawByIssue(target),hash("settlement:"+target),open+2);
                ok(n==0&&count(db,"SELECT COUNT(*) FROM r80_probe_label WHERE status='REJECTED'")==1,"tampered receipt rejected, no invented hit/miss");
            }else throw new IllegalArgumentException(mode);
            guardNoAuthority(db);
        }
        Files.writeString(Path.of(args[3]),new JSONObject().put("status","PASS").put("mode",mode).put("checks",checks).put("scope","ACTUAL_PRODUCT_REAL_SQLITE_SYNTHETIC_FIXTURE_NOT_NATURAL_EFFICACY_OR_PHONE_TIMING").toString()+"\n");
    }
}
