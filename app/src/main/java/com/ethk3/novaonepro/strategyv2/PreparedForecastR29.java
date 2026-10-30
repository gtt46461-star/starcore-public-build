package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.TimeSourceR811;
import com.ethk3.novaonepro.TargetIssueResolver;
import java.util.*;

/** Prepared prediction payloads are durable before a separate post-commit receipt makes them FUTURE-eligible.
 * PREPARED rows never enter qualification or realtime projection. A crash before that receipt leaves explicit
 * unproven evidence, not a reconstructed predraw timestamp. All members and the manifest share one transaction. */
public final class PreparedForecastR29 {
    private PreparedForecastR29(){}
    public static final String MANIFEST="CREATE TABLE IF NOT EXISTS r29_freeze_manifest(manifest_hash TEXT PRIMARY KEY,target_ordinal INTEGER NOT NULL,target_issue TEXT NOT NULL,input_issue TEXT NOT NULL,input_hash TEXT NOT NULL,history_hash TEXT NOT NULL,source_seq INTEGER NOT NULL,session_id TEXT NOT NULL,prepared_wall INTEGER NOT NULL,prepared_mono INTEGER NOT NULL,deadline_wall INTEGER NOT NULL,deadline_mono INTEGER NOT NULL,candidates INTEGER NOT NULL,valid INTEGER NOT NULL,phase TEXT NOT NULL,first_committed_wall INTEGER NOT NULL DEFAULT 0,first_committed_mono INTEGER NOT NULL DEFAULT 0,reason TEXT NOT NULL DEFAULT '')";
    public static final String MEMBERS="CREATE TABLE IF NOT EXISTS r29_freeze_member(manifest_hash TEXT NOT NULL,policy_hash TEXT NOT NULL,target_ordinal INTEGER NOT NULL,payload_hash TEXT NOT NULL,desired_status TEXT NOT NULL,PRIMARY KEY(manifest_hash,policy_hash),UNIQUE(policy_hash,target_ordinal))";
    public static final String MEMBERS_NO_UPDATE="CREATE TRIGGER IF NOT EXISTS r29_freeze_member_no_update BEFORE UPDATE ON r29_freeze_member BEGIN SELECT RAISE(ABORT,'R29_FREEZE_MEMBER_IMMUTABLE'); END";
    public static final String MEMBERS_NO_DELETE="CREATE TRIGGER IF NOT EXISTS r29_freeze_member_no_delete BEFORE DELETE ON r29_freeze_member BEGIN SELECT RAISE(ABORT,'R29_FREEZE_MEMBER_IMMUTABLE'); END";
    public static final String SEALED="CREATE TRIGGER IF NOT EXISTS r29_freeze_manifest_sealed BEFORE UPDATE ON r29_freeze_manifest WHEN OLD.phase<>'PREPARED_PENDING_COMMIT_RECEIPT' BEGIN SELECT RAISE(ABORT,'R29_FREEZE_MANIFEST_SEALED'); END";
    public static final String REJECTS="CREATE TABLE IF NOT EXISTS r29_freeze_reject(target_ordinal INTEGER NOT NULL,input_issue TEXT NOT NULL,reason TEXT NOT NULL,first_at INTEGER NOT NULL,last_at INTEGER NOT NULL,attempts INTEGER NOT NULL DEFAULT 1,PRIMARY KEY(target_ordinal,input_issue,reason))";
    public static final String FROZEN_IMMUTABLE="CREATE TRIGGER IF NOT EXISTS r29_frozen_payload_immutable BEFORE UPDATE OF policy_hash,target_ordinal,target_issue,position,probabilities,n1,n2,n3,act,frozen_at,deadline_at,manifest_hash,input_through,commit_verified_at,commit_verified_mono,commit_session ON r27_live_frozen WHEN OLD.status<>'PREPARED' BEGIN SELECT RAISE(ABORT,'R29_FROZEN_PAYLOAD_IMMUTABLE'); END";
    static final class Forecast{
        String policy,status;int position,n1,n2,n3,act;byte[] probabilities;long frozenAt;
        String hash(){return StatsR27.sha256(policy+"|"+position+"|"+n1+"|"+n2+"|"+n3+"|"+act+"|"+status+"|"+frozenAt+"|"+StatsR27.sha256(probabilities));}
    }
    static final class Frame{
        final IssueCommitR29.Fence fence;final String history;final List<Forecast> forecasts=new ArrayList<>();final long target;final String issue;boolean opportunity;
        Frame(IssueCommitR29.Fence fence,String history,boolean opportunity){this.fence=fence;this.history=history;this.opportunity=opportunity;target=fence.inputOrdinal+1;issue=TargetIssueResolver.fromSequenceOrdinalSU1(target);}
        String hash(){StringBuilder b=new StringBuilder("R29_PREPARED_FRAME_1|").append(fence.inputHash).append('|').append(history).append('|').append(fence.sourceSeq).append('|').append(fence.session).append('|').append(fence.preparedWall).append('|').append(fence.preparedMono).append('|').append(target).append('|').append(fence.deadlineWall);for(Forecast f:forecasts)b.append('|').append(f.hash());return StatsR27.sha256(b.toString());}
        int valid(){int n=0;for(Forecast f:forecasts)if(f.status.equals("FROZEN"))n++;return n;}
        int acts(){int n=0;for(Forecast f:forecasts)if(f.status.equals("FROZEN"))n+=f.act;return n;}
    }
    static ResearchWriteQueueR27.Batch pending(Frame frame,SQLiteDatabase realtime){
        ResearchWriteQueueR27.Batch b=new ResearchWriteQueueR27.Batch();String hash=frame.hash();IssueCommitR29.Fence f=frame.fence;
        b.before=db->{f.verify(realtime,System.currentTimeMillis(),TimeSourceR811.monoNowMs());return 0;};b.after=b.before;
        b.add("INSERT INTO r29_freeze_manifest(manifest_hash,target_ordinal,target_issue,input_issue,input_hash,history_hash,source_seq,session_id,prepared_wall,prepared_mono,deadline_wall,deadline_mono,candidates,valid,phase) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,'PREPARED_PENDING_COMMIT_RECEIPT')",hash,frame.target,frame.issue,f.inputIssue,f.inputHash,frame.history,f.sourceSeq,f.session,f.preparedWall,f.preparedMono,f.deadlineWall,f.deadlineMono,frame.forecasts.size(),frame.valid());
        for(Forecast p:frame.forecasts){
            b.add("INSERT INTO r27_live_frozen(policy_hash,target_ordinal,target_issue,position,probabilities,n1,n2,n3,act,status,frozen_at,deadline_at,manifest_hash,input_through,commit_session) VALUES(?,?,?,?,?,?,?,?,?,'PREPARED',?,?,?,?,?)",p.policy,frame.target,frame.issue,p.position,p.probabilities,p.n1,p.n2,p.n3,p.act,p.frozenAt,f.deadlineWall,hash,f.inputOrdinal,f.session);
            b.add("INSERT INTO r29_freeze_member VALUES(?,?,?,?,?)",hash,p.policy,frame.target,p.hash(),p.status);
        }
        return b;
    }
    static ResearchWriteQueueR27.Batch receipt(Frame frame,long committedWall,long committedMono,boolean usable,String reason,SQLiteDatabase realtime){
        ResearchWriteQueueR27.Batch b=new ResearchWriteQueueR27.Batch();String hash=frame.hash();IssueCommitR29.Fence f=frame.fence;
        b.before=db->{
            try(Cursor c=db.rawQuery("SELECT phase,candidates FROM r29_freeze_manifest WHERE manifest_hash=?",new String[]{hash})){
                if(!c.moveToFirst()||!"PREPARED_PENDING_COMMIT_RECEIPT".equals(c.getString(0))||c.getInt(1)!=frame.forecasts.size())throw new IllegalStateException("R29_FREEZE_RECEIPT_REQUIRES_COMPLETE_PENDING_MANIFEST");
            }
            if(MigrationCopyR29.scalar(db,"SELECT count(*) FROM r29_freeze_member WHERE manifest_hash=?",new String[]{hash})!=frame.forecasts.size())throw new IllegalStateException("R29_FREEZE_MEMBER_COUNT_CONFLICT");
            Map<String,Forecast> expected=new HashMap<>();for(Forecast p:frame.forecasts)if(expected.put(p.policy,p)!=null)throw new IllegalStateException("R29_DUPLICATE_PREPARED_POLICY");
            int count=0;
            try(Cursor c=db.rawQuery("SELECT m.policy_hash,m.payload_hash,m.desired_status,f.position,f.probabilities,f.n1,f.n2,f.n3,f.act,f.frozen_at,f.deadline_at,f.input_through FROM r29_freeze_member m JOIN r27_live_frozen f ON f.policy_hash=m.policy_hash AND f.target_ordinal=m.target_ordinal WHERE m.manifest_hash=? AND f.manifest_hash=? AND f.status='PREPARED'",new String[]{hash,hash})){
                while(c.moveToNext()){
                    Forecast p=expected.get(c.getString(0));
                    if(p==null||!p.hash().equals(c.getString(1))||!p.status.equals(c.getString(2))||c.getInt(3)!=p.position||!Arrays.equals(c.getBlob(4),p.probabilities)||c.getInt(5)!=p.n1||c.getInt(6)!=p.n2||c.getInt(7)!=p.n3||c.getInt(8)!=p.act||c.getLong(9)!=p.frozenAt||c.getLong(10)!=f.deadlineWall||c.getLong(11)!=f.inputOrdinal)throw new IllegalStateException("R29_FREEZE_COMMITTED_PAYLOAD_CONFLICT");count++;
                }
            }
            if(count!=frame.forecasts.size())throw new IllegalStateException("R29_FREEZE_COMMITTED_MEMBER_MISSING");
            if(usable){f.clock(committedWall,committedMono);f.verify(realtime,System.currentTimeMillis(),TimeSourceR811.monoNowMs());}return 0;
        };
        b.add("UPDATE r27_live_frozen SET status=CASE WHEN ?=1 THEN (SELECT desired_status FROM r29_freeze_member m WHERE m.manifest_hash=r27_live_frozen.manifest_hash AND m.policy_hash=r27_live_frozen.policy_hash) ELSE 'COMMIT_UNPROVEN' END,commit_verified_at=?,commit_verified_mono=? WHERE manifest_hash=? AND status='PREPARED'",usable?1:0,committedWall,committedMono,hash);
        b.add("UPDATE r29_freeze_manifest SET phase=?,first_committed_wall=?,first_committed_mono=?,reason=? WHERE manifest_hash=? AND phase='PREPARED_PENDING_COMMIT_RECEIPT'",usable?"USABLE":"COMMIT_UNPROVEN",committedWall,committedMono,reason,hash);
        if(frame.opportunity)b.add("INSERT OR REPLACE INTO r27_opportunity(target_ordinal,target_issue,scheduled,frozen,acted,settled,deadline_missed,recorded_at) VALUES(?,?,?,?,?,0,?,?)",frame.target,frame.issue,frame.forecasts.size(),usable?frame.valid():0,usable?frame.acts():0,usable?0:frame.forecasts.size(),System.currentTimeMillis());
        return b;
    }
    static long publish(ResearchPoolR27.Worker worker,Frame frame)throws Exception{
        ResearchWriteQueueR27.Batch write=pending(frame,worker.realtimeRo());
        while(!worker.writes().submit(frame.opportunity?"LIVE_PREPARED":"OBSERVE_PREPARED",write,worker::cancelled,true)){if(worker.cancelled())return 0;CooperativeContinuationR26.poll();Thread.sleep(20);}
        // Captured only after the actual SQLite transaction returned, including its commit/fsync.
        long committedWall=System.currentTimeMillis(),committedMono=TimeSourceR811.monoNowMs();
        boolean usable=true;String reason="";
        try{frame.fence.verify(worker.realtimeRo(),committedWall,committedMono);}catch(IssueCommitR29.Rejected late){usable=false;reason=late.reason;}
        ResearchWriteQueueR27.Batch ack=receipt(frame,committedWall,committedMono,usable,reason,worker.realtimeRo());
        for(;;){
            try{while(!worker.writes().submit("FREEZE_COMMIT_RECEIPT",ack,worker::cancelled,true)){if(worker.cancelled())return 0;CooperativeContinuationR26.poll();Thread.sleep(20);}return frame.forecasts.size();}
            catch(RuntimeException failed){IssueCommitR29.Rejected late=IssueCommitR29.rejected(failed);if(late==null)throw failed;ack=receipt(frame,committedWall,committedMono,false,late.reason,worker.realtimeRo());}
        }
    }
    static long recoverUnproven(ResearchPoolR27.Worker worker)throws Exception{
        List<String> pending=new ArrayList<>();try(Cursor c=worker.ro.rawQuery("SELECT manifest_hash FROM r29_freeze_manifest WHERE phase='PREPARED_PENDING_COMMIT_RECEIPT' ORDER BY prepared_wall LIMIT 32",null)){while(c.moveToNext())pending.add(c.getString(0));}
        if(pending.isEmpty())return 0;
        RuntimeR27.submit(worker,"FREEZE_UNPROVEN_RECOVERY",recover(pending));return pending.size();
    }
    static ResearchWriteQueueR27.Batch recover(List<String> pending){ResearchWriteQueueR27.Batch b=new ResearchWriteQueueR27.Batch();for(String hash:pending){b.add("UPDATE r27_live_frozen SET status='RECOVERY_UNPROVEN' WHERE manifest_hash=? AND status='PREPARED'",hash);b.add("UPDATE r29_freeze_manifest SET phase='RECOVERY_UNPROVEN',reason='No post-commit clock receipt survived; never reconstruct FUTURE evidence' WHERE manifest_hash=? AND phase='PREPARED_PENDING_COMMIT_RECEIPT'",hash);}return b;}
    static void rejected(ResearchPoolR27.Worker worker,String input,long target,String reason)throws Exception{
        long now=System.currentTimeMillis();ResearchWriteQueueR27.Batch b=new ResearchWriteQueueR27.Batch();
        b.add("INSERT OR IGNORE INTO r29_freeze_reject VALUES(?,?,?,?,?,0)",target,input,reason,now,now);
        b.add("UPDATE r29_freeze_reject SET attempts=attempts+1,last_at=? WHERE target_ordinal=? AND input_issue=? AND reason=?",now,target,input,reason);
        RuntimeR27.submit(worker,"FREEZE_REJECTED",b);
    }
    public static Map<String,Object> diagnostics(SQLiteDatabase db){
        Map<String,Object> out=new LinkedHashMap<>();out.put("scope","Predictions require a complete immutable frame and an actual post-commit predraw receipt; source publication time remains UNKNOWN");Map<String,Object> phases=new LinkedHashMap<>();
        try(Cursor c=db.rawQuery("SELECT phase,count(*) FROM r29_freeze_manifest GROUP BY phase ORDER BY phase",null)){while(c.moveToNext())phases.put(c.getString(0),c.getLong(1));out.put("status","RECORDED");}
        catch(android.database.sqlite.SQLiteException missing){if(!String.valueOf(missing.getMessage()).contains("no such table"))throw missing;out.put("status","SCHEMA_PENDING");}
        out.put("phases",phases);out.put("sourcePublicationTime","UNKNOWN");out.put("code","IssueCommitR29.Fence / PreparedForecastR29.pending, receipt, recover");out.put("cards",Arrays.asList("R29-010","R29-011","R29-012"));return out;
    }
}
