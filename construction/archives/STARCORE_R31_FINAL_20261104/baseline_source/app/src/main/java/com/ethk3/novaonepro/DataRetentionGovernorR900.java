package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.content.Context;
import android.os.Looper;
import android.system.Os;
import android.system.OsConstants;
import java.io.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/** One retention Owner. Selection/file preparation are outside the existing writer. */
public final class DataRetentionGovernorR900 {
    private static final java.util.concurrent.atomic.AtomicBoolean ACTIVE=new java.util.concurrent.atomic.AtomicBoolean();
    private static final String INFLIGHT="retention_cp12_inflight_request",TABLE_CURSOR="retention_cp12_table_cursor";
    private DataRetentionGovernorR900(){}
    public static String trim(DbStore store){return trim(store,50L);}
    public static String trim(final DbStore store,long budgetMs){
        if(!ACTIVE.compareAndSet(false,true))return "RETENTION_OWNER_BUSY";
        try{return trimOwnedCP12(store,budgetMs);}finally{ACTIVE.set(false);}
    }
    private static String trimOwnedCP12(final DbStore store,long budgetMs){
        if(store==null)return "READ_ERROR:STORE_NULL";
        if(Looper.myLooper()==Looper.getMainLooper())throw new IllegalStateException("RETENTION_MAIN_THREAD");
        if(RealtimePressureGateR811.hasActiveCriticalWork())return "YIELD_ACTIVE_CRITICAL";
        Context app=store.coordinatorR36().appContextR925();File files=app.getFilesDir();
        try{
            String cleanup=store.getState("retention_cp12_cleanup_request","");if(!cleanup.isEmpty()){RetentionArchiveProtocolCP12.hash(cleanup);cleanupConsumedFilesCP12(files,cleanup);store.setState("retention_cp12_cleanup_request","");return "RETENTION_PROOF_METADATA_CLEANED:"+cleanup;}
            long retryUntil=0;try{retryUntil=Long.parseLong(store.getState("retention_cp12_retry_until","0"));}catch(NumberFormatException invalid){throw new IOException("RETENTION_RETRY_CURSOR");}
            if(System.currentTimeMillis()<retryUntil)return "RETENTION_IO_BACKOFF";
            String current=store.getState(INFLIGHT,"");
            if(!current.isEmpty()){
                RetentionArchiveProtocolCP12.hash(current);
                File activeRequest=new File(new File(files,"retention-requests-cp12"),current+".json");
                if(!activeRequest.isFile())throw new IOException("RETENTION_REQUEST_MISSING");
                RetentionArchiveProtocolCP12.Page activePage=RetentionArchiveProtocolCP12.Page.parse(ReportJobJournalCP12.readAtomic(activeRequest,65536));
                if(!current.equals(activePage.requestId))throw new IOException("RETENTION_REQUEST_IDENTITY");
                if(!ReleaseManifestR926.MANIFEST_SHA256.equals(activePage.releaseHash)){
                    final String staleRequest=current;
                    Boolean retired=store.coordinatorR36().runAtomicWrite("RETENTION_CP12_STALE_RELEASE",new DatabaseCoordinator.WriteCallable<Boolean>(){
                        public Boolean call(SQLiteDatabase db){return retryFailedRequestWithinWriteCP12(db,staleRequest,"STALE_PAGE_RELEASE_CHANGED",System.currentTimeMillis());}
                    },Boolean.FALSE);
                    return Boolean.TRUE.equals(retired)?"RETENTION_STALE_RELEASE_REQUEUED":"RETENTION_STALE_RELEASE_WRITE_DEFERRED";
                }
                File reply=new File(new File(files,"retention-replies-cp12"),current+".receipt");
                if(!reply.isFile()){
                    File ack=new File(files,"r981_reports/requests/"+current+".ack.json");
                    if(ack.exists()||new File(ack.getPath()+".bak").exists()){
                        org.json.JSONObject result=readAckCP12(ack);if(!current.equals(result.optString("requestId","")))throw new IOException("RETENTION_ACK_IDENTITY");
                        String stage=result.optString("stage","");if(stage.equals("FAILED")||stage.equals("CANCELLED")){
                            String reason=result.optString("reason","RETENTION_DIAGNOSTIC_FAILURE");if(reason.length()>512)throw new IOException("RETENTION_ACK_REASON_BOUND");
                            final String failedRequest=current,failedReason=reason;final long failedAt=System.currentTimeMillis();
                            Boolean applied=store.coordinatorR36().runAtomicWrite("RETENTION_CP12_TERMINAL_IO",new DatabaseCoordinator.WriteCallable<Boolean>(){
                                public Boolean call(SQLiteDatabase db){return retryFailedRequestWithinWriteCP12(db,failedRequest,failedReason,failedAt);}
                            },Boolean.FALSE);
                            return Boolean.TRUE.equals(applied)?"RETENTION_IO_RETRY:"+reason:"RETENTION_TERMINAL_IO_WRITE_DEFERRED";
                        }
                    }
                    File request=new File(new File(files,"retention-requests-cp12"),current+".json");
                    if(!request.isFile())throw new IOException("RETENTION_REQUEST_MISSING");
                    ReportExportServiceCP4.requestRetentionPage(app,request);return "RETENTION_WAIT_IO:"+current;
                }
                try(com.ethk3.novaonepro.r952.MemoryBudgetCP4.Lease proofMemory=RuntimeMemoryGuardR811.algorithmModelBudget().acquire(2L<<20)){
                RetentionArchiveProtocolCP12.Receipt claimed=RetentionArchiveIoCP12.readReceipt(reply);
                if(!claimed.page.requestId.equals(current))throw new IOException("RETENTION_REPLY_REQUEST");
                File cas=RetentionArchiveProtocolCP12.casFile(files,claimed.archiveHash),lockPath=new File(cas.getParentFile(),claimed.archiveHash+".lock");
                try(RandomAccessFile lock=new RandomAccessFile(lockPath,"rw")){
                    FileLock lease;try{lease=lock.getChannel().tryLock(0,Long.MAX_VALUE,true);}catch(OverlappingFileLockException busy){return "RETENTION_CAS_BUSY";}
                    if(lease==null)return "RETENTION_CAS_BUSY";
                    try{
                        RetentionArchiveProtocolCP12.Receipt verified=RetentionArchiveIoCP12.validatePublishedProof(files,reply,claimed);
                        if(!verified.archiveHash.equals(claimed.archiveHash)||!verified.manifestHash.equals(claimed.manifestHash)||verified.byteCount!=claimed.byteCount)throw new IOException("RETENTION_RECEIPT_CONTENT");
                        final long sliceMs=Math.max(1,Math.min(50,budgetMs));
                        Integer deleted=store.coordinatorR36().runAtomicWrite("RETENTION_CP12_PUBLISH_PRUNE",new DatabaseCoordinator.WriteCallable<Integer>(){
                            public Integer call(SQLiteDatabase db){return publishAndPruneCP12(db,verified,UUID.randomUUID().toString(),TimeSourceR811.monoNowMs()+sliceMs);}
                        },Integer.valueOf(-1));
                        if(deleted==null||deleted==-1)return "RETENTION_WRITE_DEFERRED";
                        if(deleted==-2)return "RETENTION_PUBLICATION_YIELDED";
                        store.setState(INFLIGHT,"");store.setState("r900_retention_truth","RETENTION_PAGE_APPLIED:"+deleted+":"+verified.archiveId);return "RETENTION_PAGE_APPLIED:"+deleted;
                    }finally{lease.release();}
                }
                }
            }
            // The rolling draw cache reserves its own slot and pins referenced inputs.
            // Its optional archive uses the SAME fair table cursor below. An evicted
            // old draw must not repeatedly jump ahead of every large evidence table.
            // Retry draws deferred by an unfinished training/settlement dependency.
            int retried=store.replayRetentionDeferredDrawsCP12(8);if(retried>0)return "RETENTION_DEFERRED_DRAWS_APPLIED:"+retried;
            String archiveExpiry=expireOneArchiveCP12(store,files);if(!archiveExpiry.equals("RETENTION_EXPIRY_NONE"))return archiveExpiry;
            final long expiryDeadline=TimeSourceR811.monoNowMs()+Math.max(1,Math.min(50,budgetMs));
            Integer expired=store.coordinatorR36().runAtomicWrite("RETENTION_CP12_EXPIRE_POST_TRAIN",new DatabaseCoordinator.WriteCallable<Integer>(){
                public Integer call(SQLiteDatabase db){Cutoff cutoff=readCutoff(db,4999);return cutoff==null?0:ResearchTaskQueueR967.expirePostTrainBeforeCP12(db,cutoff.issue,expiryDeadline);}
            },Integer.valueOf(-1));
            if(expired==null||expired<0)return "RETENTION_EXPIRY_WRITE_DEFERRED";
            final long indexDeadline=TimeSourceR811.monoNowMs()+Math.max(1,Math.min(50,budgetMs));
            RevisionSweepCP12 indexSweep=store.coordinatorR36().runAtomicWrite("RETENTION_CP12_REVISION_INDEX",new DatabaseCoordinator.WriteCallable<RevisionSweepCP12>(){public RevisionSweepCP12 call(SQLiteDatabase db){return sweepOrphanRevisionIndexCP12(db,indexDeadline);}},null);
            if(indexSweep==null)return "RETENTION_INDEX_WRITE_DEFERRED";
            // Revision cleanup is housekeeping, not an exclusive retention lane. A large
            // orphan backlog must not prevent verified raw pages from being archived.
            if(indexSweep.deleted>0||!indexSweep.complete)store.setState("r979_retention_due_at","0");
            int cursor;try{cursor=Integer.parseInt(store.getState(TABLE_CURSOR,"0"));}catch(NumberFormatException invalid){cursor=0;}
            for(int scanned=0;scanned<RetentionTableRegistryCP12.ALL_INSTALLED.size();scanned++){
                if(ResearchWorkBudgetR927.remainingMs()<20){store.setState(TABLE_CURSOR,Integer.toString(((cursor%RetentionTableRegistryCP12.ALL_INSTALLED.size())+scanned+RetentionTableRegistryCP12.ALL_INSTALLED.size())%RetentionTableRegistryCP12.ALL_INSTALLED.size()));store.setState("r979_retention_due_at","0");return "RETENTION_SELECTION_YIELDED";}
                int selected=((cursor%RetentionTableRegistryCP12.ALL_INSTALLED.size())+scanned+RetentionTableRegistryCP12.ALL_INSTALLED.size())%RetentionTableRegistryCP12.ALL_INSTALLED.size();
                RetentionTableRegistryCP12.Rule rule=RetentionTableRegistryCP12.ALL_INSTALLED.get(selected);
                if(!rule.archiveAndPrune())continue;
                RetentionArchiveProtocolCP12.Page page=prepareArchivePageCP12(store.readable(),rule.table);
                if(page==null)continue;
                File request=persistRequestCP12(files,page);
                store.setState(TABLE_CURSOR,Integer.toString((selected+1)%RetentionTableRegistryCP12.ALL_INSTALLED.size()));
                store.setState(INFLIGHT,page.requestId);ReportExportServiceCP4.requestRetentionPage(app,request);return "RETENTION_IO_QUEUED:"+page.requestId;
            }
            // A live-only prefix can exhaust the scan budget without deleting anything.
            // Do not turn that unexamined suffix into the owner's empty-backlog cooldown.
            return indexSweep.complete?"RETENTION_NO_ELIGIBLE_ROWS":"RETENTION_INDEX_SCAN_PROGRESS";
        }catch(InterruptedIOException cancelled){return "RETENTION_YIELDED";}
        catch(IllegalStateException pressure){if("ALGORITHM_MEMORY_BUDGET".equals(pressure.getMessage())){store.setState("r900_retention_truth","RETENTION_MEMORY_PRESSURE");store.setState("r979_retention_due_at","0");return "RETENTION_MEMORY_PRESSURE";}throw pressure;}
        catch(IOException failure){store.setState("r900_retention_truth","RETENTION_IO_ERROR:"+failure.getMessage());return "RETENTION_IO_ERROR:"+failure.getMessage();}
    }
    public static RetentionArchiveProtocolCP12.Page prepareArchivePageCP12(SQLiteDatabase db,String table){
        if(db==null||db.inTransaction()||Looper.myLooper()==Looper.getMainLooper())throw new IllegalStateException("RETENTION_PREPARE_THREAD");
        RetentionTableRegistryCP12.Rule rule=RetentionTableRegistryCP12.require(table);if(!rule.archiveAndPrune())return null;
        Cutoff cutoff;String unarchived="";
        if(table.equals(DbHelper.TABLE_DRAWS)){
            long count;try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM draws WHERE dirty=0",null)){if(!c.moveToFirst())throw new IllegalStateException("RETENTION_DRAW_COUNT");count=c.getLong(0);}
            if(count<4872)return null;if(count>Integer.MAX_VALUE)throw new IllegalStateException("RETENTION_DRAW_COUNT_RANGE");
            cutoff=readCutoff(db,(int)count-129);if(cutoff==null)return null;
            String proof="SELECT 1 FROM research_evidence_archive_member_v2 m JOIN research_evidence_archive_v2 a ON a.archive_id=m.archive_id WHERE m.source_table='draws' AND m.source_rowid=draws.rowid AND m.source_generation=COALESCE((SELECT r.generation FROM research_row_revision_v2 r WHERE r.source_table='draws' AND r.source_rowid=draws.rowid),0) AND a.state IN('VERIFIED','REFERENCED','PRUNED')";
            // Archive only eligible immutable rows; pending consumers retain their input.
            String[] drawCutoffArgs=rule.args(cutoff.issue,cutoff.openTime);
            try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM draws WHERE dirty=0 AND ("+rule.eligibleSql+") AND EXISTS("+proof+")",drawCutoffArgs)){if(c.moveToFirst()&&c.getLong(0)>=32)return null;}
            unarchived=" AND NOT EXISTS("+proof+")";
        }else{cutoff=readCutoff(db,4999);if(cutoff==null)return null;}
        // ORDER BY rowid below can make SQLite scan millions of current protected rows.
        // The leading range is a necessary condition only. A positive probe grants no
        // deletion authority; every dependency/pin/version check still runs unchanged.
        if(!hasRowsBeforeCutoffR77(db,rule,cutoff.issue,cutoff.openTime))return null;
        ArrayList<RetentionArchiveProtocolCP12.RowVersion> rows=new ArrayList<>();int keyBytes=0;LiveKeyShapeCP12 keyShape=new LiveKeyShapeCP12(db,table);
        String sql="SELECT \""+table+"\".rowid,COALESCE((SELECT r.generation FROM research_row_revision_v2 r WHERE r.source_table=? AND r.source_rowid=\""+table+"\".rowid),0) FROM \""+table+"\" WHERE "+rule.eligibleSql+unarchived+" ORDER BY \""+table+"\".rowid LIMIT 128";
        String[] predicate=rule.args(cutoff.issue,cutoff.openTime),args=new String[predicate.length+1];args[0]=table;System.arraycopy(predicate,0,args,1,predicate.length);
        try(Cursor c=db.rawQuery(sql,args)){while(c.moveToNext()){int bytes=canonicalLiveKeyCP12(db,keyShape,c.getLong(0),"0000000000000000000000000000000000000000000000000000000000000000").length;if(keyBytes>RetentionArchiveProtocolCP12.MAX_KEY_TOTAL-bytes)break;keyBytes+=bytes;rows.add(new RetentionArchiveProtocolCP12.RowVersion(c.getLong(0),c.getLong(1)));if(ResearchWorkBudgetR927.remainingMs()<20)break;}}
        if(rows.isEmpty())return null;
        return new RetentionArchiveProtocolCP12.Page(table,cutoff.issue,cutoff.openTime,schemaHashCP12(db,table),ReleaseManifestR926.MANIFEST_SHA256,RetentionTableRegistryCP12.hashForTable(table),rows);
    }
    static boolean hasRowsBeforeCutoffR77(SQLiteDatabase db,RetentionTableRegistryCP12.Rule rule,String issue,long time){
        String column=rule.issueColumn.isEmpty()?rule.timeColumn:rule.issueColumn;
        if(column.isEmpty()||!column.matches("[A-Za-z_][A-Za-z0-9_]*"))return true;
        String ref="\""+rule.table+"\".\""+column+"\"";
        String prefix=rule.issueColumn.isEmpty()?ref+">0 AND "+ref+"<?":"length("+ref+")=12 AND "+ref+"<?";
        // Generated rules may use overflow or special predicates. Only a verified
        // leading conjunct can be factored out without changing eligibility semantics.
        if(!rule.eligibleSql.equals(prefix)&&(!rule.eligibleSql.startsWith(prefix+" AND (")||rule.eligibleSql.contains(" OR ")))return true;
        try(Cursor c=db.rawQuery("SELECT 1 FROM \""+rule.table+"\" WHERE "+ref+"<? LIMIT 1",
                new String[]{rule.issueColumn.isEmpty()?Long.toString(time):issue})){
            return c.moveToFirst();
        }
    }

    /** Called only with the diagnostics independent SCROW proof consumed under its shared CAS lease. */
    public static int publishAndPruneCP12(SQLiteDatabase db,RetentionArchiveProtocolCP12.Receipt receipt,String session,long deadline){
        if(db==null||!db.inTransaction()||receipt==null||session==null||session.isEmpty())throw new IllegalStateException("RETENTION_WRITER_REQUIRED");
        if(!receipt.page.registryHash.equals(RetentionTableRegistryCP12.hashForTable(receipt.page.table))||!receipt.page.releaseHash.equals(ReleaseManifestR926.MANIFEST_SHA256))throw new IllegalStateException("RETENTION_RELEASE_CHANGED");
        RetentionTableRegistryCP12.Rule rule=RetentionTableRegistryCP12.require(receipt.page.table);if(!rule.archiveAndPrune())throw new IllegalStateException("RETENTION_PERMANENT_TABLE");
        if(!schemaHashCP12(db,rule.table).equals(receipt.page.schemaHash))throw new IllegalStateException("RETENTION_LIVE_SCHEMA_CHANGED");
        try(Cursor c=db.rawQuery("PRAGMA foreign_keys",null)){if(!c.moveToFirst()||c.getInt(0)!=1)throw new IllegalStateException("RETENTION_FOREIGN_KEYS_DISABLED");}
        try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM research_retention_permit_v2",null)){if(!c.moveToFirst()||c.getLong(0)!=0)throw new IllegalStateException("RETENTION_ORPHAN_PERMIT");}
        // Range fields empty until proven; selection cutoff belongs to summary, not fake source range.
        String summary="{\"revision\":\"RETENTION_PAGE_CP12_1\",\"requestId\":\""+receipt.page.requestId+"\",\"cutoffIssue\":\""+receipt.page.cutoffIssue+"\",\"rowCount\":"+receipt.members.size()+"}";
        db.execSQL("INSERT OR IGNORE INTO research_evidence_archive_v2(archive_id,source_table,first_issue,last_issue,row_count,archive_hash,relative_path,state,source_schema_hash,manifest_hash,summary_json,summary_hash,created_at,verified_at) VALUES(?,?,'','',?,?,?,'VERIFIED',?,?,?,?,?,?)",new Object[]{receipt.archiveId,rule.table,receipt.members.size(),receipt.archiveHash,receipt.relativePath,receipt.page.schemaHash,receipt.manifestHash,summary,RetentionArchiveProtocolCP12.sha(summary.getBytes(StandardCharsets.UTF_8)),receipt.verifiedAt,receipt.verifiedAt});
        try(Cursor c=db.rawQuery("SELECT source_table,row_count,archive_hash,relative_path,source_schema_hash,manifest_hash,state FROM research_evidence_archive_v2 WHERE archive_id=?",new String[]{receipt.archiveId})){
            if(!c.moveToFirst()||!rule.table.equals(c.getString(0))||c.getInt(1)!=receipt.members.size()||!receipt.archiveHash.equals(c.getString(2))||!receipt.relativePath.equals(c.getString(3))||!receipt.page.schemaHash.equals(c.getString(4))||!receipt.manifestHash.equals(c.getString(5))||!Arrays.asList("VERIFIED","REFERENCED","PRUNED").contains(c.getString(6)))throw new IllegalStateException("RETENTION_ARCHIVE_CONFLICT");
        }
        String cursorKey="retention_cp12_member_cursor_"+receipt.page.requestId;int memberCursor=0;
        try(Cursor c=db.rawQuery("SELECT state_value FROM app_state WHERE state_key=?",new String[]{cursorKey})){if(c.moveToFirst())try{memberCursor=Integer.parseInt(c.getString(0));}catch(NumberFormatException corrupt){throw new IllegalStateException("RETENTION_PUBLICATION_CURSOR",corrupt);}}
        if(memberCursor<0||memberCursor>receipt.members.size())throw new IllegalStateException("RETENTION_PUBLICATION_CURSOR");
        int memberStart=memberCursor;
        for(;memberCursor<receipt.members.size();memberCursor++){
            if(deadline<=0||RealtimePressureGateR811.hasActiveCriticalWork()||(memberCursor>memberStart&&TimeSourceR811.monoNowMs()>=deadline))break;
            RetentionArchiveProtocolCP12.Member m=receipt.members.get(memberCursor);
            db.execSQL("INSERT OR IGNORE INTO research_evidence_archive_member_v2(archive_id,source_table,source_key_hash,row_hash,key_canonical,source_rowid,source_generation) VALUES(?,?,?,?,?,?,?)",new Object[]{receipt.archiveId,rule.table,m.keyHash,m.rowHash,m.key(),m.rowId,m.generation});
            try(Cursor c=db.rawQuery("SELECT source_key_hash,row_hash,source_generation FROM research_evidence_archive_member_v2 WHERE archive_id=? AND source_table=? AND source_rowid=?",new String[]{receipt.archiveId,rule.table,Long.toString(m.rowId)})){if(!c.moveToFirst()||!m.keyHash.equals(c.getString(0))||!m.rowHash.equals(c.getString(1))||m.generation!=c.getLong(2))throw new IllegalStateException("RETENTION_MEMBER_CONFLICT");}
        }
        db.execSQL("INSERT OR REPLACE INTO app_state(state_key,state_value,updated_at) VALUES(?,?,?)",new Object[]{cursorKey,Integer.toString(memberCursor),System.currentTimeMillis()});
        if(memberCursor<receipt.members.size())return -2;
        Cutoff cutoff=readCutoff(db,AppConfig.MAX_LOCAL_HISTORY_SAVE-1);if(cutoff==null)return 0; // asynchronous pruning never shrinks the required live history tail
        String pruneKey="retention_cp12_prune_cursor_"+receipt.page.requestId;int pruneCursor=0;
        try(Cursor c=db.rawQuery("SELECT state_value FROM app_state WHERE state_key=?",new String[]{pruneKey})){if(c.moveToFirst())try{pruneCursor=Integer.parseInt(c.getString(0));}catch(NumberFormatException corrupt){throw new IllegalStateException("RETENTION_PRUNE_CURSOR",corrupt);}}
        if(pruneCursor<0||pruneCursor>receipt.members.size())throw new IllegalStateException("RETENTION_PRUNE_CURSOR");
        LiveKeyShapeCP12 keyShape=new LiveKeyShapeCP12(db,rule.table);
        int deleted=0,pruneStart=pruneCursor;
        for(;pruneCursor<receipt.members.size();pruneCursor++){
            RetentionArchiveProtocolCP12.Member m=receipt.members.get(pruneCursor);
            if(deadline<=0||RealtimePressureGateR811.hasActiveCriticalWork()||(pruneCursor>pruneStart&&TimeSourceR811.monoNowMs()>=deadline))break;
            if(!eligibleVersionCP12(db,rule,m.rowId,m.generation,cutoff))continue;
            if(!Arrays.equals(m.key(),canonicalLiveKeyCP12(db,keyShape,m.rowId,receipt.snapshotHash)))throw new IllegalStateException("RETENTION_MEMBER_LIVE_KEY_MISMATCH");
            db.execSQL("INSERT INTO research_retention_permit_v2(source_table,source_rowid,archive_id,row_hash,process_session,created_at,source_generation) VALUES(?,?,?,?,?,?,?)",new Object[]{rule.table,m.rowId,receipt.archiveId,m.rowHash,session,System.currentTimeMillis(),m.generation});
            String[] predicate=rule.args(cutoff.issue,cutoff.openTime),args=new String[predicate.length+3];args[0]=Long.toString(m.rowId);args[1]=rule.table;args[2]=Long.toString(m.generation);System.arraycopy(predicate,0,args,3,predicate.length);
            int changed=db.delete(rule.table,"rowid=? AND COALESCE((SELECT r.generation FROM research_row_revision_v2 r WHERE r.source_table=? AND r.source_rowid=\""+rule.table+"\".rowid),0)=CAST(? AS INTEGER) AND "+rule.eligibleSql,args);
            db.execSQL("DELETE FROM research_retention_permit_v2 WHERE source_table=? AND source_rowid=? AND process_session=?",new Object[]{rule.table,m.rowId,session});
            if(changed<0||changed>1)throw new IllegalStateException("RETENTION_DELETE_CAS_COUNT");deleted+=changed;
        }
        db.execSQL("INSERT OR REPLACE INTO app_state(state_key,state_value,updated_at) VALUES(?,?,?)",new Object[]{pruneKey,Integer.toString(pruneCursor),System.currentTimeMillis()});
        if(pruneCursor<receipt.members.size())return -2;
        db.execSQL("DELETE FROM app_state WHERE state_key IN (?,?)",new Object[]{cursorKey,pruneKey});
        String state=hasLiveArchiveMemberCP12(db,rule,receipt.archiveId)?"VERIFIED":"PRUNED";
        try(Cursor c=db.rawQuery("SELECT 1 FROM research_evidence_locator_v2 WHERE archive_id=? LIMIT 1",new String[]{receipt.archiveId})){if(c.moveToFirst())state="REFERENCED";}
        db.execSQL("UPDATE research_evidence_archive_v2 SET state=?,expires_at=? WHERE archive_id=? AND state<>'EXPIRED_SUMMARIZED'",new Object[]{state,System.currentTimeMillis()+604800000L,receipt.archiveId});
        db.execSQL("INSERT OR REPLACE INTO app_state(state_key,state_value,updated_at) VALUES('retention_cp12_cleanup_request',?,?)",new Object[]{receipt.page.requestId,System.currentTimeMillis()});
        db.execSQL("DELETE FROM app_state WHERE state_key=? AND state_value=?",new Object[]{INFLIGHT,receipt.page.requestId});
        db.execSQL("INSERT OR REPLACE INTO app_state(state_key,state_value,updated_at) VALUES('r979_retention_due_at','0',?)",new Object[]{System.currentTimeMillis()});
        return deleted;
    }
    /** Terminal diagnostics ACK was validated off-writer; record cleanup+retry atomically before retiring inflight. */
    public static boolean retryFailedRequestWithinWriteCP12(SQLiteDatabase db,String requestId,String reason,long now){
        if(db==null||!db.inTransaction()||reason==null||reason.length()>512||now<0)throw new IllegalStateException("RETENTION_RETRY_WRITER");
        RetentionArchiveProtocolCP12.hash(requestId);
        try(Cursor c=db.rawQuery("SELECT state_value FROM app_state WHERE state_key=?",new String[]{INFLIGHT})){if(!c.moveToFirst()||!requestId.equals(c.getString(0)))return false;}
        try(Cursor c=db.rawQuery("SELECT state_value FROM app_state WHERE state_key='retention_cp12_cleanup_request'",null)){if(c.moveToFirst()&&!c.getString(0).isEmpty()&&!requestId.equals(c.getString(0)))throw new IllegalStateException("RETENTION_CLEANUP_PENDING");}
        db.execSQL("INSERT OR REPLACE INTO app_state(state_key,state_value,updated_at) VALUES('retention_cp12_cleanup_request',?,?)",new Object[]{requestId,now});
        db.execSQL("DELETE FROM app_state WHERE state_key=? AND state_value=?",new Object[]{INFLIGHT,requestId});
        db.execSQL("INSERT OR REPLACE INTO app_state(state_key,state_value,updated_at) VALUES('retention_cp12_retry_until',?,?)",new Object[]{Long.toString(now+(reason.contains("STALE_PAGE")?1000L:60000L)),now});
        db.execSQL("INSERT OR REPLACE INTO app_state(state_key,state_value,updated_at) VALUES('r900_retention_truth',?,?),('r979_retention_due_at','0',?)",new Object[]{"RETENTION_IO_RETRY:"+reason,now,now});
        return true;
    }

    /** Runs only after durable publication/prune completion or a consumed terminal diagnostics ACK; marker survives partial unlink. */
    public static void cleanupConsumedFilesCP12(File filesDir,String requestId)throws IOException {
        RetentionArchiveProtocolCP12.hash(requestId);File base=filesDir.getCanonicalFile();
        String[] paths={"retention-requests-cp12/"+requestId+".json","retention-requests-cp12/"+requestId+".partial","retention-replies-cp12/"+requestId+".receipt","retention-replies-cp12/"+requestId+".receipt.partial","r981_reports/requests/"+requestId+".json","r981_reports/requests/"+requestId+".json.bak","r981_reports/requests/"+requestId+".ack.json","r981_reports/requests/"+requestId+".ack.json.bak","r981_reports/requests/"+requestId+".json.new","r981_reports/requests/"+requestId+".ack.json.new"};
        HashSet<File> parents=new HashSet<>();for(String path:paths){File exact=new File(base,path);if(!exact.getCanonicalFile().equals(exact))throw new IOException("RETENTION_CLEANUP_PATH");if(exact.exists()){if(!exact.isFile()||!exact.delete())throw new IOException("RETENTION_CLEANUP_UNLINK");parents.add(exact.getParentFile());}}
        for(File parent:parents)syncDirectory(parent);
    }

    private static void syncDirectory(File directory)throws IOException {java.io.FileDescriptor fd=null;try{fd=Os.open(directory.getPath(),OsConstants.O_RDONLY,0);Os.fsync(fd);}catch(Exception e){throw new IOException("RETENTION_CLEANUP_DIRECTORY_FSYNC",e);}finally{if(fd!=null)try{Os.close(fd);}catch(Exception e){throw new IOException("RETENTION_CLEANUP_DIRECTORY_CLOSE",e);}}}

    private static boolean hasLiveArchiveMemberCP12(SQLiteDatabase db,RetentionTableRegistryCP12.Rule rule,String archive){
        try(Cursor c=db.rawQuery("SELECT 1 FROM research_evidence_archive_member_v2 m JOIN \""+rule.table+"\" live ON live.rowid=m.source_rowid WHERE m.archive_id=? AND m.source_table=? AND m.source_generation=COALESCE((SELECT r.generation FROM research_row_revision_v2 r WHERE r.source_table=m.source_table AND r.source_rowid=live.rowid),0) LIMIT 1",new String[]{archive,rule.table})){return c.moveToFirst();}
    }
    /** Prepared SQL phase; one archive, at most128 members. Global CAS lease is held by caller. */
    public static boolean summarizeArchiveWithinWriteCP12(SQLiteDatabase db,String archive,long now){
        if(db==null||!db.inTransaction())throw new IllegalStateException("RETENTION_EXPIRY_WRITER");
        String table,state;long expires;
        try(Cursor c=db.rawQuery("SELECT source_table,state,expires_at FROM research_evidence_archive_v2 WHERE archive_id=?",new String[]{archive})){if(!c.moveToFirst())return false;table=c.getString(0);state=c.getString(1);expires=c.getLong(2);}
        if(state.equals("EXPIRED_SUMMARIZED"))return true;
        if(!state.equals("PRUNED")||expires<=0||expires>now)return false;
        RetentionTableRegistryCP12.Rule rule=RetentionTableRegistryCP12.require(table);
        if(hasLiveArchiveMemberCP12(db,rule,archive))return false;
        try(Cursor c=db.rawQuery("SELECT 1 FROM research_evidence_locator_v2 WHERE archive_id=? UNION ALL SELECT 1 FROM research_task_input_binding_v2 WHERE archive_id=? UNION ALL SELECT 1 FROM research_retention_permit_v2 WHERE archive_id=? LIMIT 1",new String[]{archive,archive,archive})){if(c.moveToFirst())return false;}
        db.execSQL("UPDATE research_evidence_archive_v2 SET state='EXPIRED_SUMMARIZED' WHERE archive_id=? AND state='PRUNED' AND expires_at=?",new Object[]{archive,expires});
        try(Cursor c=db.rawQuery("SELECT changes()",null)){if(!c.moveToFirst()||c.getInt(0)!=1)return false;}
        db.execSQL("DELETE FROM research_evidence_archive_member_v2 WHERE archive_id=?",new Object[]{archive});return true;
    }
    private static String expireOneArchiveCP12(final DbStore store,File files)throws IOException{
        File directory=new File(files,"retention-cas-cp12");if(!directory.isDirectory())return "RETENTION_EXPIRY_NONE";
        try(ReportInputsLeaseCP12 publication=ReportInputsLeaseCP12.tryAcquireExclusive(files)){
            if(publication==null)return "RETENTION_EXPIRY_NONE";
                final String archive,hash;
                try(Cursor c=store.readable().rawQuery("SELECT a.archive_id,a.archive_hash FROM research_evidence_archive_v2 a WHERE (a.state='EXPIRED_SUMMARIZED' AND NOT EXISTS(SELECT 1 FROM app_state k WHERE k.state_key='retention_cp12_unlinked_'||a.archive_id AND k.state_value='1')) OR (a.state='PRUNED' AND a.expires_at>0 AND a.expires_at<=? AND NOT EXISTS(SELECT 1 FROM research_evidence_locator_v2 l WHERE l.archive_id=a.archive_id) AND NOT EXISTS(SELECT 1 FROM research_task_input_binding_v2 b WHERE b.archive_id=a.archive_id)) ORDER BY CASE WHEN a.state='EXPIRED_SUMMARIZED' THEN 0 ELSE 1 END,a.expires_at,a.archive_id LIMIT 1",new String[]{Long.toString(System.currentTimeMillis())})){if(!c.moveToFirst())return "RETENTION_EXPIRY_NONE";archive=c.getString(0);hash=c.getString(1);RetentionArchiveProtocolCP12.hash(hash);}
                File cas=RetentionArchiveProtocolCP12.casFile(files,hash);
                try(RandomAccessFile lock=new RandomAccessFile(new File(directory,hash+".lock"),"rw")){
                    FileLock lease;try{lease=lock.getChannel().tryLock();}catch(OverlappingFileLockException busy){return "RETENTION_EXPIRY_NONE";}if(lease==null)return "RETENTION_EXPIRY_NONE";
                    try{
                        Boolean summarized=store.coordinatorR36().runAtomicWrite("RETENTION_CP12_SUMMARIZE_ARCHIVE",new DatabaseCoordinator.WriteCallable<Boolean>(){public Boolean call(SQLiteDatabase db){return summarizeArchiveWithinWriteCP12(db,archive,System.currentTimeMillis());}},Boolean.FALSE);
                        if(!Boolean.TRUE.equals(summarized))return "RETENTION_EXPIRY_NONE";
                        if(cas.exists()&&!cas.delete())throw new IOException("RETENTION_CAS_UNLINK");
                        java.io.FileDescriptor fd=null;try{fd=Os.open(directory.getPath(),OsConstants.O_RDONLY,0);Os.fsync(fd);}catch(Exception e){throw new IOException("RETENTION_CAS_UNLINK_DURABILITY",e);}finally{if(fd!=null)try{Os.close(fd);}catch(Exception e){throw new IOException("RETENTION_CAS_DIRECTORY_CLOSE",e);}}
                        // Header hash/manifest/summary remain permanent evidence of explicit expiry.
                        store.setState("retention_cp12_unlinked_"+archive,"1");return "RETENTION_ARCHIVE_SUMMARIZED:"+archive;
                    }finally{lease.release();}
                }
        }
    }

    private static final class LiveKeyShapeCP12 {
        final String table,query;final int[] ordinals;
        LiveKeyShapeCP12(SQLiteDatabase db,String table){
            this.table=RetentionTableRegistryCP12.require(table).table;ArrayList<Integer> orders=new ArrayList<>();StringBuilder fields=new StringBuilder();
            try(Cursor c=db.rawQuery("PRAGMA table_info(\""+table+"\")",null)){while(c.moveToNext()){if(c.getInt(0)>=1024)throw new IllegalStateException("RETENTION_KEY_SCHEMA_BOUND");if(c.getInt(5)<=0)continue;String name=c.getString(1);if(name.indexOf('"')>=0)throw new IllegalStateException("RETENTION_KEY_IDENTIFIER");orders.add(c.getInt(0));if(fields.length()>0)fields.append(',');String col="\""+name+"\"";fields.append("typeof(").append(col).append("),length(CAST(").append(col).append(" AS BLOB)),").append(col).append(",CASE WHEN length(CAST(").append(col).append(" AS BLOB))<=16384 THEN CAST(").append(col).append(" AS BLOB) ELSE NULL END");}}
            ordinals=new int[orders.size()];for(int i=0;i<ordinals.length;i++)ordinals[i]=orders.get(i);query="SELECT "+(fields.length()==0?"rowid":fields.toString())+" FROM \""+table+"\" WHERE rowid=?";
        }
    }
    /** Bounded primary-key identity only; never reads full payload on writer. */
    public static byte[] canonicalLiveKeyCP12(SQLiteDatabase db,String table,long rowid,String snapshotHash){return canonicalLiveKeyCP12(db,new LiveKeyShapeCP12(db,table),rowid,snapshotHash);}
    private static byte[] canonicalLiveKeyCP12(SQLiteDatabase db,LiveKeyShapeCP12 shape,long rowid,String snapshotHash){
        RetentionArchiveProtocolCP12.hash(snapshotHash);
        try(Cursor c=db.rawQuery(shape.query,new String[]{Long.toString(rowid)})){
            if(!c.moveToFirst())throw new IllegalStateException("RETENTION_LIVE_KEY_ROW_MISSING");boolean local=shape.ordinals.length==0;
            for(int i=0;i<shape.ordinals.length;i++)if(c.getString(i*4).equals("null"))local=true;
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream key=new DataOutputStream(bytes);
            if(local){key.writeByte(2);for(int i=0;i<64;i+=2)key.writeByte(Integer.parseInt(snapshotHash.substring(i,i+2),16));key.writeLong(rowid);}
            else{key.writeByte(1);key.writeShort(shape.ordinals.length);for(int i=0;i<shape.ordinals.length;i++){
                int cell=i*4;String type=c.getString(cell);int tag;long length;byte[] value=null;
                if(type.equals("integer")){tag=RetentionRowCodecCP5.INTEGER;length=8;}
                else if(type.equals("real")){tag=RetentionRowCodecCP5.REAL;length=8;}
                else if(type.equals("text")||type.equals("blob")){tag=type.equals("text")?RetentionRowCodecCP5.TEXT:RetentionRowCodecCP5.BLOB;length=c.getLong(cell+1);if(length>16384||length<0||length>16384-bytes.size()-11)throw new IllegalStateException("RETENTION_LIVE_KEY_BOUND");value=c.getBlob(cell+3);if(value==null||value.length!=length)throw new IllegalStateException("RETENTION_LIVE_KEY_BYTES");}
                else throw new IllegalStateException("RETENTION_LIVE_KEY_TYPE");
                key.writeShort(shape.ordinals[i]);key.writeByte(tag);key.writeLong(length);if(tag==RetentionRowCodecCP5.INTEGER)key.writeLong(c.getLong(cell+2));else if(tag==RetentionRowCodecCP5.REAL)key.writeLong(Double.doubleToRawLongBits(c.getDouble(cell+2)));else key.write(value);
                if(bytes.size()>16384)throw new IllegalStateException("RETENTION_LIVE_KEY_BOUND");
            }}key.flush();return bytes.toByteArray();
        }catch(IOException impossible){throw new AssertionError(impossible);}
    }

    /** REPLACE can leave revision entries for removed rowids. Scan indexed keyset
     * pages: LIMIT on the DELETE result alone does not bound a NOT EXISTS scan. */
    public static int cleanOrphanRevisionIndexCP12(SQLiteDatabase db,long deadline){
        return sweepOrphanRevisionIndexCP12(db,deadline).deleted;
    }
    private static final class RevisionSweepCP12 {
        final int deleted,examined;final boolean complete;
        RevisionSweepCP12(int deleted,int examined,boolean complete){this.deleted=deleted;this.examined=examined;this.complete=complete;}
    }
    /** A pass spans writer slices. 0=not started, 1=resumable, 2=complete.
     * The registry fingerprint prevents an old bitmap omitting new source tables.
     * sqlite_sequence is monotonic across revision INSERT/REPLACE, including
     * deleted highest generations: MAX(generation) is not a safe change token. */
    private static final String REVISION_PASS_R81="retention_cp12_revision_pass_r81";
    private static long revisionSequenceR81(SQLiteDatabase db){
        try(Cursor c=db.rawQuery("SELECT COALESCE((SELECT seq FROM sqlite_sequence WHERE name='research_row_revision_v2'),0)",null)){
            if(!c.moveToFirst()||c.getLong(0)<0)throw new IllegalStateException("RETENTION_REVISION_SEQUENCE");return c.getLong(0);
        }
    }
    private static RevisionSweepCP12 sweepOrphanRevisionIndexCP12(SQLiteDatabase db,long deadline){
        if(db==null||!db.inTransaction())throw new IllegalStateException("RETENTION_REVISION_WRITER");
        int cursor=0;try(Cursor c=db.rawQuery("SELECT state_value FROM app_state WHERE state_key='retention_cp12_revision_cursor'",null)){if(c.moveToFirst())try{cursor=Integer.parseInt(c.getString(0));}catch(NumberFormatException invalid){throw new IllegalStateException("RETENTION_REVISION_CURSOR",invalid);}}
        int count=0,scanned=0,examined=0,size=RetentionTableRegistryCP12.ALL_INSTALLED.size();
        StringBuilder registryText=new StringBuilder();for(RetentionTableRegistryCP12.Rule rule:RetentionTableRegistryCP12.ALL_INSTALLED)registryText.append(rule.table).append('\n');
        String registry=V51ChecksumEngine.sha256(registryText.toString());long sequence=revisionSequenceR81(db),passSequence=sequence;char[] states=new char[size];Arrays.fill(states,'0');
        try(Cursor c=db.rawQuery("SELECT state_value FROM app_state WHERE state_key=?",new String[]{REVISION_PASS_R81})){
            if(c.moveToFirst()){
                String raw=c.getString(0);if(raw==null||raw.length()>size+128)throw new IllegalStateException("RETENTION_REVISION_PASS");String[] parts=raw.split("\\|",-1);
                if(parts.length!=4||!parts[0].equals("R81")||!parts[1].matches("[0-9a-f]{64}"))throw new IllegalStateException("RETENTION_REVISION_PASS");
                try{passSequence=Long.parseLong(parts[2]);}catch(NumberFormatException invalid){throw new IllegalStateException("RETENTION_REVISION_PASS",invalid);}
                if(passSequence<0||passSequence>sequence)throw new IllegalStateException("RETENTION_REVISION_SEQUENCE");
                if(parts[1].equals(registry)){
                    if(parts[3].length()!=size)throw new IllegalStateException("RETENTION_REVISION_PASS");states=parts[3].toCharArray();for(char value:states)if(value<'0'||value>'2')throw new IllegalStateException("RETENTION_REVISION_PASS");
                }else passSequence=sequence; // changed registry starts a complete new pass
            }
        }
        boolean empty=false,complete=false;
        if(TimeSourceR811.monoNowMs()<deadline&&!RealtimePressureGateR811.hasActiveCriticalWork()){
            try(Cursor c=db.rawQuery("SELECT 1 FROM research_row_revision_v2 LIMIT 1",null)){empty=!c.moveToFirst();}
        }
        final int maximumExaminedR79=2048,pageRowsR79=64;
        for(;!empty&&scanned<size&&examined<maximumExaminedR79&&TimeSourceR811.monoNowMs()<deadline&&!RealtimePressureGateR811.hasActiveCriticalWork();scanned++){
            int ordinal=((cursor%size)+scanned+size)%size;if(states[ordinal]=='2')continue;
            String table=RetentionTableRegistryCP12.ALL_INSTALLED.get(ordinal).table,key="retention_cp12_revision_after_"+table;long after=Long.MIN_VALUE;
            if(states[ordinal]=='1')try(Cursor c=db.rawQuery("SELECT state_value FROM app_state WHERE state_key=?",new String[]{key})){
                if(!c.moveToFirst())throw new IllegalStateException("RETENTION_REVISION_PAGE_CURSOR");
                try{after=Long.parseLong(c.getString(0));}catch(NumberFormatException invalid){throw new IllegalStateException("RETENTION_REVISION_PAGE_CURSOR",invalid);}
            }
            long next=after;boolean tableComplete=false,readPage=false;
            while(examined<maximumExaminedR79&&TimeSourceR811.monoNowMs()<deadline&&!RealtimePressureGateR811.hasActiveCriticalWork()){
                int limit=Math.min(pageRowsR79,maximumExaminedR79-examined),rows=0;long last=next;ArrayList<Long> orphanGenerations=new ArrayList<Long>();
                // Long.MIN_VALUE is a legal SQLite rowid, so the initial page is inclusive.
                String comparison=next==Long.MIN_VALUE?">=":">";
                try(Cursor c=db.rawQuery("SELECT r.source_rowid,r.generation,EXISTS(SELECT 1 FROM \""+table+"\" live WHERE live.rowid=r.source_rowid) FROM research_row_revision_v2 r WHERE r.source_table=? AND r.source_rowid"+comparison+"? ORDER BY r.source_rowid LIMIT "+limit,new String[]{table,Long.toString(next)})){
                    while(c.moveToNext()){rows++;last=c.getLong(0);if(c.getInt(2)==0)orphanGenerations.add(c.getLong(1));}
                }
                readPage=true;examined+=rows;
                if(!orphanGenerations.isEmpty()){
                    StringBuilder marks=new StringBuilder();Object[] generations=new Object[orphanGenerations.size()];for(int i=0;i<generations.length;i++){if(i>0)marks.append(',');marks.append('?');generations[i]=orphanGenerations.get(i);}
                    db.execSQL("DELETE FROM research_row_revision_v2 WHERE generation IN("+marks+") AND NOT EXISTS(SELECT 1 FROM \""+table+"\" live WHERE live.rowid=research_row_revision_v2.source_rowid)",generations);
                    try(Cursor c=db.rawQuery("SELECT changes()",null)){if(c.moveToFirst())count+=c.getInt(0);}
                }
                next=rows<limit?Long.MIN_VALUE:last;if(rows<limit){tableComplete=true;break;}
            }
            if(readPage)states[ordinal]=tableComplete?'2':'1';
            db.execSQL("INSERT OR REPLACE INTO app_state(state_key,state_value,updated_at) VALUES(?,?,?)",new Object[]{key,Long.toString(next),System.currentTimeMillis()});
        }
        boolean all=true;for(char value:states)if(value!='2'){all=false;break;}
        if(empty)complete=true;
        else if(all){
            // A replacement after an already scanned prefix may leave an old-rowid
            // orphan. Never cool down on that mixed-generation pass.
            long current=revisionSequenceR81(db);complete=current==passSequence;passSequence=current;
        }
        if(empty||all){Arrays.fill(states,'0');passSequence=revisionSequenceR81(db);}
        db.execSQL("INSERT OR REPLACE INTO app_state(state_key,state_value,updated_at) VALUES(?,?,?)",new Object[]{REVISION_PASS_R81,"R81|"+registry+'|'+passSequence+'|'+new String(states),System.currentTimeMillis()});
        db.execSQL("INSERT OR REPLACE INTO app_state(state_key,state_value,updated_at) VALUES('retention_cp12_revision_cursor',?,?)",new Object[]{Integer.toString(((cursor%size)+scanned+size)%size),System.currentTimeMillis()});
        db.execSQL("INSERT OR REPLACE INTO app_state(state_key,state_value,updated_at) VALUES('retention_cp12_revision_last',?,?)",new Object[]{"examined="+examined+"|deleted="+count+"|tables="+scanned+"|complete="+(complete?1:0),System.currentTimeMillis()});
        return new RevisionSweepCP12(count,examined,complete);
    }
    private static boolean eligibleVersionCP12(SQLiteDatabase db,RetentionTableRegistryCP12.Rule rule,long rowid,long generation,Cutoff cutoff){
        String[] predicates=rule.args(cutoff.issue,cutoff.openTime),args=new String[predicates.length+3];args[0]=Long.toString(rowid);args[1]=rule.table;args[2]=Long.toString(generation);System.arraycopy(predicates,0,args,3,predicates.length);
        try(Cursor c=db.rawQuery("SELECT 1 FROM \""+rule.table+"\" WHERE rowid=? AND COALESCE((SELECT r.generation FROM research_row_revision_v2 r WHERE r.source_table=? AND r.source_rowid=\""+rule.table+"\".rowid),0)=CAST(? AS INTEGER) AND "+rule.eligibleSql,args)){return c.moveToFirst();}
    }
    /** Reserve one hot-history slot in the same transaction as the incoming draw.
     * The 5000-row draw cache must not wait for asynchronous evidence-file publication.
     * Pending training/settlement input is preserved for its existing owners.
     */
    public static boolean reserveTailSlotWithinWriteCP12(SQLiteDatabase db,String incomingIssue){
        if(db==null||!db.inTransaction()||!TargetIssueResolver.isValid(incomingIssue))throw new IllegalStateException("RETENTION_TAIL_WRITER");
        long count;try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM draws WHERE dirty=0",null)){if(!c.moveToFirst())throw new IllegalStateException("RETENTION_DRAW_COUNT");count=c.getLong(0);}
        if(count<AppConfig.MAX_LOCAL_HISTORY_SAVE)return true;
        if(count>AppConfig.MAX_LOCAL_HISTORY_SAVE)return false; // migration is a separate bounded owner.
        long rowid;String oldest;
        try(Cursor c=db.rawQuery("SELECT rowid,issue FROM draws WHERE dirty=0 ORDER BY issue LIMIT 1",null)){
            if(!c.moveToFirst())throw new IllegalStateException("RETENTION_OLDEST_DRAW_MISSING");
            rowid=c.getLong(0);oldest=c.getString(1);
        }
        if(incomingIssue.compareTo(oldest)<=0)return false;
        // Preserve unfinished consumers' exact input independently of the hot cache.
        // Copy, deletion and incoming insertion share the caller's rollback boundary.
        DrawDependencyInputCP12.releaseConsumedWithinWrite(db,8);
        DrawDependencyInputCP12.preserveIfReferencedWithinWrite(db,rowid,oldest);
        int removed=db.delete("draws","rowid=? AND issue=? AND issue<? AND dirty=0",
                new String[]{Long.toString(rowid),oldest,incomingIssue});
        if(removed==0)return false;
        if(removed!=1)throw new IllegalStateException("RETENTION_TAIL_CAS_LOST");
        db.execSQL("INSERT OR REPLACE INTO app_state(state_key,state_value,updated_at) VALUES('cp12_draw_tail_eviction',?,?)",
                new Object[]{"oldest="+oldest+"|incoming="+incomingIssue+"|countBefore="+count,System.currentTimeMillis()});
        return true;
    }
    private static final class Cutoff {final String issue;final long openTime;Cutoff(String issue,long time){this.issue=issue;openTime=time;}}
    private static Cutoff readCutoff(SQLiteDatabase db,int offset){try(Cursor c=db.rawQuery("SELECT issue,open_time FROM draws WHERE dirty=0 ORDER BY issue DESC LIMIT 1 OFFSET "+offset,null)){if(!c.moveToFirst()||!TargetIssueResolver.isValid(c.getString(0))||c.getLong(1)<=0)return null;return new Cutoff(c.getString(0),c.getLong(1));}}
    public static String schemaHashCP12(SQLiteDatabase db,String table){
        RetentionTableRegistryCP12.require(table);MessageDigest digest=RetentionArchiveProtocolCP12.digest();
        try{DataOutputStream out=new DataOutputStream(new DigestOutputStream(new OutputStream(){public void write(int b){}public void write(byte[] b,int o,int n){}},digest));out.writeInt(0x53435331);
            try(Cursor c=db.rawQuery("SELECT length(CAST(sql AS BLOB)) FROM sqlite_master WHERE type='table' AND name=?",new String[]{table})){if(!c.moveToFirst()||c.isNull(0)||c.getLong(0)>65536)throw new IllegalStateException("RETENTION_SCHEMA_BOUND");}
            try(Cursor c=db.rawQuery("SELECT sql FROM sqlite_master WHERE type='table' AND name=?",new String[]{table})){if(!c.moveToFirst())throw new IllegalStateException("RETENTION_SCHEMA_MISSING");writeString(out,c.getString(0));}
            try(Cursor c=db.rawQuery("PRAGMA encoding",null)){if(!c.moveToFirst())throw new IllegalStateException("RETENTION_ENCODING");writeString(out,c.getString(0));}
            try(Cursor c=db.rawQuery("PRAGMA table_info(\""+table+"\")",null)){int count=0;while(c.moveToNext()){if(count++>=1024||c.getInt(0)!=count-1)throw new IllegalStateException("RETENTION_COLUMNS");out.writeByte(1);out.writeInt(c.getInt(0));writeString(out,c.getString(1));writeString(out,c.getString(2));out.writeInt(c.getInt(3));writeString(out,c.isNull(4)?null:c.getString(4));out.writeInt(c.getInt(5));}}
            out.writeByte(0);out.flush();return RetentionArchiveProtocolCP12.hex(digest.digest());
        }catch(IOException impossible){throw new AssertionError(impossible);}
    }
    private static void writeString(DataOutputStream out,String value)throws IOException{if(value==null){out.writeInt(-1);return;}byte[] bytes=value.getBytes(StandardCharsets.UTF_8);out.writeInt(bytes.length);out.write(bytes);}

    private static org.json.JSONObject readAckCP12(File file)throws IOException{
        try(InputStream in=new android.util.AtomicFile(file).openRead();ByteArrayOutputStream bytes=new ByteArrayOutputStream()){
            byte[] buffer=new byte[2048];int n;while((n=in.read(buffer))!=-1){if(n==0||bytes.size()>8192-n)throw new IOException("RETENTION_ACK_BOUND");bytes.write(buffer,0,n);}return new org.json.JSONObject(new String(bytes.toByteArray(),StandardCharsets.UTF_8));
        }catch(org.json.JSONException invalid){throw new IOException("RETENTION_ACK_JSON",invalid);}
    }
    private static File persistRequestCP12(File files,RetentionArchiveProtocolCP12.Page page)throws IOException{
        File directory=new File(files,"retention-requests-cp12").getCanonicalFile();if(!directory.getPath().startsWith(files.getCanonicalPath()+File.separator))throw new IOException("RETENTION_REQUEST_PATH");if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException("RETENTION_REQUEST_MKDIR");
        File target=new File(directory,page.requestId+".json");if(target.exists()){if(target.length()!=page.requestJson.getBytes(StandardCharsets.UTF_8).length)throw new IOException("RETENTION_REQUEST_COLLISION");try(InputStream in=new FileInputStream(target)){byte[] bytes=new byte[(int)target.length()];int read=0,n;while(read<bytes.length&&(n=in.read(bytes,read,bytes.length-read))>0)read+=n;if(read!=bytes.length||in.read()!=-1||!Arrays.equals(bytes,page.requestJson.getBytes(StandardCharsets.UTF_8)))throw new IOException("RETENTION_REQUEST_COLLISION");}return target;}
        File staging=new File(directory,page.requestId+".partial");try(FileOutputStream out=new FileOutputStream(staging)){out.write(page.requestJson.getBytes(StandardCharsets.UTF_8));out.getFD().sync();}
        java.io.FileDescriptor fd=null;try{Os.rename(staging.getPath(),target.getPath());fd=Os.open(directory.getPath(),OsConstants.O_RDONLY,0);Os.fsync(fd);}catch(Exception e){throw new IOException("RETENTION_REQUEST_PUBLISH",e);}finally{if(fd!=null)try{Os.close(fd);}catch(Exception e){throw new IOException("RETENTION_REQUEST_DIRECTORY_CLOSE",e);}}return target;
    }
    /** Legacy schema migration now uses5000 for every raw table;120 is cache-only. */
    static boolean isHeavy(String table){return false;}
    /** Removed the historical direct delete bypass. */
    static int deletePage(SQLiteDatabase db,String table,String column,String cutoff,int limit){throw new IllegalStateException("RETENTION_REQUIRES_VERIFIED_RECEIPT");}
}
