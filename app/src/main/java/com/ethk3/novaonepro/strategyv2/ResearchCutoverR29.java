package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.StatFs;
import com.ethk3.novaonepro.DbStore;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Verified R27 ownership handoff. Copy, baseline backup and hashing never own the realtime writer. Only the final
 * route receipt uses a short realtime transaction. The original tables and the sealed baseline are retained. */
public final class ResearchCutoverR29 {
    private ResearchCutoverR29(){}
    public static final String PUBLICATION="CREATE TABLE IF NOT EXISTS r29_migration_publication(singleton INTEGER PRIMARY KEY CHECK(singleton=1),epoch INTEGER NOT NULL,candidate_name TEXT NOT NULL,phase TEXT NOT NULL,baseline_name TEXT NOT NULL DEFAULT '',baseline_sha TEXT NOT NULL DEFAULT '',source_root TEXT NOT NULL DEFAULT '',last_error TEXT NOT NULL DEFAULT '',updated_at INTEGER NOT NULL)";
    static final String FENCE="CREATE TABLE IF NOT EXISTS r29_migration_owner_fence(singleton INTEGER PRIMARY KEY CHECK(singleton=1),epoch INTEGER NOT NULL,protocol TEXT NOT NULL,source_hash TEXT NOT NULL DEFAULT '',created_at INTEGER NOT NULL)";
    static final String FENCE_PROTOCOL="R29_ROOT_RESEARCH_FROZEN_V1";
    private static final String CANDIDATE="starcore-r29-research.candidate.db";
    private static final long BUDGET_MS=30_000,RESERVE_BYTES=96L<<20;

    static List<MigrationCopyR29.Table> tables(SQLiteDatabase source){
        List<MigrationCopyR29.Table> out=new ArrayList<>();
        try(Cursor c=source.rawQuery("SELECT name,sql FROM sqlite_master WHERE type='table' ORDER BY name",null)){
            while(c.moveToNext()){
                String name=c.getString(0);
                if(MigrationCopyR29.ownedResearch(name)||name.equals("r27_chart_cache")||name.equals("r27_fetch_reject")||name.equals("draws")||name.equals("su1_source_clock")||name.equals("r26_feedback")||name.equals("app_state")||CriticalInputMirrorR29.tables().contains(name))out.add(MigrationCopyR29.table(source,name,c.getString(1)));
            }
        }
        return out;
    }
    static String predicate(String table){return "1=1";}
    static List<MigrationCopyR29.Row> page(SQLiteDatabase source,MigrationCopyR29.Table table,Long after){
        return ResearchRowsR29.readPage(source,table,after,predicate(table.name),32,2L<<20,Long.MAX_VALUE,()->{});
    }
    private static List<MigrationCopyR29.Row> page(SQLiteDatabase source,MigrationCopyR29.Table table,Long after,long deadline,java.util.function.BooleanSupplier cancelled){
        return ResearchRowsR29.readPage(source,table,after,predicate(table.name),32,2L<<20,Long.MAX_VALUE,()->check(deadline,cancelled));
    }
    static Object[] decode(byte[] bytes,int expected){
        if(bytes==null||bytes.length>MigrationCopyR29.MAX_ROW_BYTES)throw new IllegalArgumentException("R29_COPY_PAYLOAD_BUDGET");
        try{
            DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes));
            if(in.readInt()!=0x29DB0001||in.readInt()!=expected)throw new IOException("ROW_ABI_OR_SHAPE");
            Object[] values=new Object[expected];
            for(int i=0;i<expected;i++)switch(in.readUnsignedByte()){
                case Cursor.FIELD_TYPE_NULL:values[i]=null;break;
                case Cursor.FIELD_TYPE_INTEGER:values[i]=in.readLong();break;
                case Cursor.FIELD_TYPE_FLOAT:values[i]=Double.longBitsToDouble(in.readLong());break;
                case Cursor.FIELD_TYPE_STRING:values[i]=new String(blob(in),StandardCharsets.UTF_8);break;
                case Cursor.FIELD_TYPE_BLOB:values[i]=blob(in);break;
                default:throw new IOException("ROW_TYPE");
            }
            if(in.available()!=0)throw new IOException("ROW_TRAILING_BYTES");return values;
        }catch(IOException invalid){throw new IllegalArgumentException("R29_COPY_PAYLOAD_INVALID",invalid);}
    }
    static byte[] blob(DataInputStream in)throws IOException{int n=in.readInt();if(n<0||n>MigrationCopyR29.MAX_ROW_BYTES||n>in.available())throw new IOException("ROW_CELL_LENGTH");byte[] b=new byte[n];in.readFully(b);return b;}
    static void restorePage(SQLiteDatabase destination,MigrationCopyR29.Table table,List<MigrationCopyR29.Row> rows){
        boolean rowid=!ResearchRowsR29.withoutRowid(table);StringBuilder columns=new StringBuilder(rowid?"rowid":""),binds=new StringBuilder(rowid?"?":"");
        for(String column:table.columns){if(columns.length()>0){columns.append(',');binds.append(',');}columns.append(MigrationCopyR29.id(column));binds.append('?');}
        String sql="INSERT INTO "+MigrationCopyR29.id(table.name)+"("+columns+") VALUES("+binds+")";
        for(MigrationCopyR29.Row row:rows){
            if(!StatsR27.sha256(row.payload).equals(row.hash))throw new IllegalStateException("R29_COPY_SOURCE_CHECKSUM");
            Object[] cells=decode(row.payload,table.columns.size()),values=new Object[cells.length+(rowid?1:0)];if(rowid)values[0]=row.rowid;System.arraycopy(cells,0,values,rowid?1:0,cells.length);destination.execSQL(sql,values);
        }
    }
    static String root(SQLiteDatabase source,List<MigrationCopyR29.Table> tables,long deadline,java.util.function.BooleanSupplier cancelled){
        StringBuilder root=new StringBuilder("R29_ACTIVE_DATA_V1");
        for(MigrationCopyR29.Table t:tables){
            String chain=t.seed();Long after=null;long count=0;
            for(;;){check(deadline,cancelled);List<MigrationCopyR29.Row> page=page(source,t,after,deadline,cancelled);if(page.isEmpty())break;for(MigrationCopyR29.Row row:page){chain=MigrationCopyR29.chain(chain,row.rowid,row.hash);after=row.rowid;count++;}}
            root.append('|').append(t.name).append(':').append(count).append(':').append(chain);
        }
        return StatsR27.sha256(root.toString());
    }
    static void check(long deadline,java.util.function.BooleanSupplier cancelled){if(cancelled.getAsBoolean()||Thread.currentThread().isInterrupted())throw new IllegalStateException("R29_CUTOVER_CANCELLED");if(android.os.SystemClock.elapsedRealtime()>deadline)throw new IllegalStateException("R29_CUTOVER_BUDGET_YIELD");}
    static String schemaHash(List<MigrationCopyR29.Table> tables){StringBuilder b=new StringBuilder("R29_ACTIVE_SCHEMA_V1");for(MigrationCopyR29.Table t:tables)b.append('|').append(t.name).append(':').append(t.schemaHash);return StatsR27.sha256(b.toString());}

    interface SnapshotWork<T>{T run(SQLiteDatabase source)throws Exception;}
    /** A private OPEN_READONLY WAL handle gives Android's transaction session one pinned read snapshot.
     * It deliberately does not set query_only: Android's public nonexclusive transaction starts with
     * BEGIN IMMEDIATE, which query_only rejects even for a physically read-only WAL connection.
     * OPEN_READONLY still forbids every persistent write; the independent Root writer remains live. */
    static <T>T withSourceSnapshot(SQLiteDatabase authority,SnapshotWork<T> body)throws Exception{
        try(SQLiteDatabase source=SQLiteDatabase.openDatabase(authority.getPath(),null,SQLiteDatabase.OPEN_READONLY)){
            if(!source.isReadOnly())throw new IllegalStateException("R29_CUTOVER_SOURCE_MUST_BE_READ_ONLY");
            try(Cursor c=source.rawQuery("PRAGMA journal_mode",null)){
                if(!c.moveToFirst()||!"wal".equalsIgnoreCase(c.getString(0)))throw new IllegalStateException("R29_CUTOVER_SOURCE_WAL_REQUIRED");
            }
            source.beginTransactionNonExclusive();
            try{
                // Prime the source snapshot before schema discovery or the first typed row page.
                try(Cursor c=source.rawQuery("SELECT count(*) FROM sqlite_master",null)){if(!c.moveToFirst())throw new IllegalStateException("R29_CUTOVER_SOURCE_SNAPSHOT_MISSING");}
                return body.run(source);
            }finally{source.endTransaction();}
        }
    }

    public static String run(DbStore store,ResearchPoolR27.Worker worker)throws Exception{
        if(ResearchRouteR29.active(worker.realtimeRo())){ResearchRouteR29.verifyActiveOwner(worker.realtimeRo());return "ACTIVE";}
        if(!RuntimeCoordinatorR26.readyForResearchMigration())return "WAITING_LEGACY_STORAGE_SCHEMA_BEFORE_OWNER_TRANSFER";
        try(LegacyCapabilityAdapterR29.CutoverLease legacy=LegacyCapabilityAdapterR29.drainForCutover()){
        if(legacy==null)return "WAITING_LEGACY_MAINTENANCE_DRAIN";
        if(!worker.pool().beginCutover(3000))return "WAITING_RESEARCH_QUIESCENCE";
        try(ResearchRouteR29.Handoff handoff=ResearchRouteR29.handoff(store)){
            if(handoff==null)return "WAITING_RESEARCH_WRITER_HANDOFF";
            return copyAndPublish(store,worker);
        }catch(RuntimeException failure){recordRecoveryRequired(store,failure);throw failure;
        }finally{worker.pool().endCutover();}
        }
    }
    static List<MigrationCopyR29.Table> researchTables(SQLiteDatabase root){
        List<MigrationCopyR29.Table> out=new ArrayList<>();
        for(MigrationCopyR29.Table table:tables(root))if(MigrationCopyR29.ownedResearch(table.name))out.add(table);
        return out;
    }
    static String researchRoot(SQLiteDatabase root,long deadline,java.util.function.BooleanSupplier cancelled){
        return root(root,researchTables(root),deadline,cancelled);
    }
    static String fenceHash(SQLiteDatabase root,long epoch){
        try(Cursor table=root.rawQuery("SELECT 1 FROM main.sqlite_master WHERE type='table' AND name='r29_migration_owner_fence'",null)){
            if(!table.moveToFirst())throw new IllegalStateException("R29_PENDING_PUBLICATION_RECOVERY_REQUIRED:NO_DURABLE_OWNER_FENCE");
        }
        try(Cursor c=root.rawQuery("SELECT epoch,protocol,source_hash FROM r29_migration_owner_fence WHERE singleton=1",null)){
            if(!c.moveToFirst()||c.getLong(0)!=epoch||!FENCE_PROTOCOL.equals(c.getString(1)))throw new IllegalStateException("R29_PENDING_PUBLICATION_RECOVERY_REQUIRED:OWNER_FENCE_CONFLICT");
            return c.getString(2);
        }
    }
    private static void bindFence(DbStore store,SQLiteDatabase root,long epoch,long deadline,java.util.function.BooleanSupplier cancelled){
        String expected=fenceHash(root,epoch),actual=researchRoot(root,deadline,cancelled);
        if(!expected.isEmpty()){if(!expected.equals(actual))throw new IllegalStateException("R29_PENDING_PUBLICATION_RECOVERY_REQUIRED:ROOT_RESEARCH_ADVANCED_AFTER_SEAL");return;}
        LegacyResearchScopeR29.realtimeWrite(store,"R29_BIND_RESEARCH_OWNER_FENCE",db->{
            db.execSQL("UPDATE r29_migration_owner_fence SET source_hash=? WHERE singleton=1 AND epoch=? AND protocol=? AND source_hash=''",new Object[]{actual,epoch,FENCE_PROTOCOL});
            if(!actual.equals(fenceHash(db,epoch)))throw new IllegalStateException("R29_OWNER_FENCE_HASH_CAS_CONFLICT");return 0;
        });
    }
    static void verifyFence(SQLiteDatabase root,long epoch,long deadline,java.util.function.BooleanSupplier cancelled){
        String expected=fenceHash(root,epoch);
        if(!hash(expected)||!expected.equals(researchRoot(root,deadline,cancelled)))throw new IllegalStateException("R29_PENDING_PUBLICATION_RECOVERY_REQUIRED:ROOT_RESEARCH_ADVANCED_AFTER_SEAL");
    }
    private static void recordRecoveryRequired(DbStore store,RuntimeException failure){
        String reason=String.valueOf(failure.getMessage());if(!reason.startsWith("R29_PENDING_PUBLICATION_RECOVERY_REQUIRED:"))return;
        final String detail=reason.length()>1000?reason.substring(0,1000):reason;
        try{LegacyResearchScopeR29.realtimeWrite(store,"R29_RECORD_OWNER_RECOVERY_REQUIRED",db->{
            db.execSQL("UPDATE r29_migration_publication SET last_error=?,updated_at=? WHERE singleton=1 AND phase IN ('PREPARED','VERIFIED','RENAMED')",new Object[]{detail,System.currentTimeMillis()});return 0;
        });}catch(RuntimeException unavailable){failure.addSuppressed(unavailable);}
    }
    /** Recover the sealed owner before R26 initialization can write sessions or models. No pool/Worker
     * is needed here, so a durable pending fence cannot deadlock startup waiting for its own pool. */
    public static String resumeBeforeRuntime(DbStore store){
        SQLiteDatabase root=store.coordinatorForResearchR952().realtimeReadOnlyR29();
        if(ResearchRouteR29.active(root)||!ResearchRouteR29.pendingHandoff(root))return "NO_PENDING_OWNER_TRANSFER";
        long deadline=android.os.SystemClock.elapsedRealtime()+BUDGET_MS;
        try(LegacyCapabilityAdapterR29.CutoverLease legacy=LegacyCapabilityAdapterR29.drainForCutover();ResearchRouteR29.Handoff handoff=ResearchRouteR29.handoff(store)){
            if(legacy==null||handoff==null)throw new IllegalStateException("CP3_DB_BUSY_RESEARCH_STARTUP_RECOVERY");
            Publication p=publication(root);if(p==null)return "NO_PENDING_OWNER_TRANSFER";
            File candidate=store.contextForR26().getDatabasePath(CANDIDATE),published=store.contextForR26().getDatabasePath(ResearchRouteR29.FILE_NAME);
            File baseline=new File(candidate.getParentFile(),baselineName(p.epoch));
            MigrationCopyR29.Transaction writer=body->LegacyResearchScopeR29.realtimeWrite(store,"R29_STARTUP_CUTOVER_PHASE",db->{body.run(db);return 0;});
            if("PREPARED".equals(p.phase)&&!baseline.exists()){
                if(!p.baselineName.isEmpty()||!p.baselineSha.isEmpty()||!p.sourceRoot.isEmpty()||published.exists())throw new IllegalStateException("R29_PREPARED_PROOF_CONFLICT");
                // No sealed proof exists; Root still owns every original row. Only the disposable,
                // unsealed candidate and this uncompleted reservation may be cancelled.
                removeUnpublished(candidate);writer.write(db->{
                    db.execSQL("DELETE FROM r29_migration_publication WHERE singleton=1 AND epoch=? AND phase='PREPARED' AND baseline_name='' AND baseline_sha=''",new Object[]{p.epoch});
                    if(publication(db)!=null)throw new IllegalStateException("R29_UNSEALED_RESERVATION_CANCEL_CAS_CONFLICT");
                    try(Cursor exists=db.rawQuery("SELECT 1 FROM main.sqlite_master WHERE type='table' AND name='r29_migration_owner_fence'",null)){if(exists.moveToFirst())db.execSQL("DELETE FROM r29_migration_owner_fence WHERE singleton=1 AND epoch=?",new Object[]{p.epoch});}
                });return "UNSEALED_RESERVATION_CANCELLED_ROOT_RETAINED";
            }
            verifyFence(root,p.epoch,deadline,()->Thread.currentThread().isInterrupted());
            Publication ready=resumePublication(root,candidate,published,writer,deadline,()->Thread.currentThread().isInterrupted());
            if(ready==null)throw new IllegalStateException("R29_SEALED_STARTUP_PROOF_REQUIRED");
            ResearchRouteR29.publish(store,published,ready.schemaHash,ready.sourceRoot,ready.epoch);
            return "ACTIVE_RECOVERED_BEFORE_RUNTIME_INITIALIZE";
        }catch(InterruptedException stop){Thread.currentThread().interrupt();throw new IllegalStateException("CP3_DB_BUSY_RESEARCH_STARTUP_INTERRUPTED",stop);}
        catch(RuntimeException failure){recordRecoveryRequired(store,failure);throw failure;}
        catch(Exception failure){throw new IllegalStateException("R29_STARTUP_OWNER_RECOVERY_FAILED",failure);}
    }
    static final class Publication{
        long epoch;String candidateName,phase,baselineName,baselineSha,sourceRoot,schemaHash;
    }
    static Publication publication(SQLiteDatabase root){
        try(Cursor c=root.rawQuery("SELECT epoch,candidate_name,phase,baseline_name,baseline_sha,source_root FROM r29_migration_publication WHERE singleton=1",null)){
            if(!c.moveToFirst())return null;
            Publication p=new Publication();p.epoch=c.getLong(0);p.candidateName=c.getString(1);p.phase=c.getString(2);p.baselineName=c.getString(3);p.baselineSha=c.getString(4);p.sourceRoot=c.getString(5);
            if(p.epoch<=0||!CANDIDATE.equals(p.candidateName)||!Arrays.asList("PREPARED","VERIFIED","RENAMED","PUBLISHED").contains(p.phase))throw new IllegalStateException("R29_PUBLICATION_RECEIPT_INVALID");
            return p;
        }catch(android.database.sqlite.SQLiteException missing){if(String.valueOf(missing.getMessage()).contains("no such table"))return null;throw missing;}
    }
    private static String baselineName(long epoch){return "starcore-r29-baseline-"+epoch+".db";}
    private static boolean hash(String value){return value!=null&&value.matches("[a-f0-9]{64}");}
    private static void sealedWal(File file){File wal=new File(file.getPath()+"-wal");if(wal.exists()&&wal.length()!=0)throw new IllegalStateException("R29_UNPUBLISHED_WAL_NOT_SEALED");}
    private static String[] readyOwner(File file,long epoch,String sourceRoot,String schema){
        if(!file.isFile())throw new IllegalStateException("R29_VERIFIED_RESEARCH_FILE_MISSING");
        try(SQLiteDatabase db=SQLiteDatabase.openDatabase(file.getPath(),null,SQLiteDatabase.OPEN_READONLY)){
            try(Cursor c=db.rawQuery("SELECT phase,schema_hash,source_root,epoch FROM r29_research_owner WHERE singleton=1",null)){
                if(!c.moveToFirst()||!"READY".equals(c.getString(0))||!hash(c.getString(1))||!hash(c.getString(2))||epoch!=c.getLong(3)
                        ||sourceRoot!=null&&!sourceRoot.equals(c.getString(2))||schema!=null&&!schema.equals(c.getString(1)))throw new IllegalStateException("R29_RESEARCH_OWNER_MISMATCH");
                String[] proof={c.getString(1),c.getString(2)};
                try(Cursor q=db.rawQuery("PRAGMA quick_check",null)){if(!q.moveToFirst()||!"ok".equals(q.getString(0))||q.moveToNext())throw new IllegalStateException("R29_RESEARCH_INTEGRITY_FAILED");}
                return proof;
            }
        }
    }
    private static String[] sealedFile(File file,long epoch,String sha,String sourceRoot,String schema,long deadline,java.util.function.BooleanSupplier cancelled)throws Exception{
        sealedWal(file);if(!file.isFile()||!sha.equals(fileHash(file,deadline,cancelled)))throw new IllegalStateException("R29_PINNED_BASELINE_OR_CANDIDATE_CORRUPT");
        return readyOwner(file,epoch,sourceRoot,schema);
    }
    private static void phase(MigrationCopyR29.Transaction writer,Publication before,String after,String baseline,String sha,String root)throws Exception{
        writer.write(db->{
            db.execSQL("UPDATE r29_migration_publication SET phase=?,baseline_name=?,baseline_sha=?,source_root=?,updated_at=? WHERE singleton=1 AND epoch=? AND candidate_name=? AND phase=? AND baseline_name=? AND baseline_sha=? AND source_root=?",new Object[]{after,baseline,sha,root,System.currentTimeMillis(),before.epoch,before.candidateName,before.phase,before.baselineName,before.baselineSha,before.sourceRoot});
            Publication actual=publication(db);
            if(actual==null||actual.epoch!=before.epoch||!after.equals(actual.phase)||!baseline.equals(actual.baselineName)||!sha.equals(actual.baselineSha)||!root.equals(actual.sourceRoot))throw new IllegalStateException("R29_PUBLICATION_PHASE_CAS_CONFLICT");
        });
    }
    /** Resume only the original sealed bytes. No retry re-reads moving Root data after proof is pinned. */
    static Publication resumePublication(SQLiteDatabase root,File candidate,File published,MigrationCopyR29.Transaction writer,long deadline,java.util.function.BooleanSupplier cancelled)throws Exception{
        check(deadline,cancelled);Publication p=publication(root);if(p==null)throw new IllegalStateException("R29_PUBLICATION_RESERVATION_REQUIRED");
        if(!candidate.getName().equals(p.candidateName)||!published.getName().equals(ResearchRouteR29.FILE_NAME)||!candidate.getParentFile().equals(published.getParentFile()))throw new IllegalStateException("R29_PUBLICATION_FILE_PATH_MISMATCH");
        File baseline=new File(candidate.getParentFile(),baselineName(p.epoch));
        if("PREPARED".equals(p.phase)){
            if(!p.baselineName.isEmpty()||!p.baselineSha.isEmpty()||!p.sourceRoot.isEmpty()||published.exists())throw new IllegalStateException("R29_PREPARED_PROOF_CONFLICT");
            if(!baseline.exists()){removeUnpublished(candidate);return null;}
            // A crash after sealing but before Root pinning may retain both complete identical files.
            // Missing or different candidate bytes are ambiguous: preserve them and fail closed.
            sealedWal(baseline);String sha=fileHash(baseline,deadline,cancelled);
            String[] owner=sealedFile(baseline,p.epoch,sha,null,null,deadline,cancelled);
            sealedFile(candidate,p.epoch,sha,owner[1],owner[0],deadline,cancelled);
            phase(writer,p,"VERIFIED",baseline.getName(),sha,owner[1]);p=publication(root);
        }
        if(!baseline.getName().equals(p.baselineName)||!hash(p.baselineSha)||!hash(p.sourceRoot))throw new IllegalStateException("R29_PINNED_BASELINE_RECEIPT_INVALID");
        String[] owner=sealedFile(baseline,p.epoch,p.baselineSha,p.sourceRoot,null,deadline,cancelled);p.schemaHash=owner[0];
        if("PUBLISHED".equals(p.phase)){
            if(!ResearchRouteR29.active(root))throw new IllegalStateException("R29_PUBLISHED_ROUTE_MISSING");
            try(Cursor c=root.rawQuery("SELECT schema_hash,source_root,epoch FROM r29_database_route WHERE singleton=1",null)){if(!c.moveToFirst()||!p.schemaHash.equals(c.getString(0))||!p.sourceRoot.equals(c.getString(1))||p.epoch!=c.getLong(2))throw new IllegalStateException("R29_PUBLISHED_ROUTE_PROOF_CONFLICT");}
            // Published research may contain later legal commits; its owner still binds the old baseline.
            readyOwner(published,p.epoch,p.sourceRoot,p.schemaHash);return p;
        }
        if(published.exists())sealedFile(published,p.epoch,p.baselineSha,p.sourceRoot,p.schemaHash,deadline,cancelled);
        if(candidate.exists())sealedFile(candidate,p.epoch,p.baselineSha,p.sourceRoot,p.schemaHash,deadline,cancelled);
        if(!published.exists()){
            if(!candidate.exists()){
                if(new StatFs(candidate.getParent()).getAvailableBytes()<baseline.length()+RESERVE_BYTES)throw new IllegalStateException("R29_CUTOVER_STORAGE_BUDGET_YIELD");
                copyBaseline(baseline,candidate,deadline,cancelled);
            }
            check(deadline,cancelled);if(!candidate.renameTo(published))throw new IllegalStateException("R29_CANDIDATE_ATOMIC_RENAME_FAILED");
            sealedFile(published,p.epoch,p.baselineSha,p.sourceRoot,p.schemaHash,deadline,cancelled);
        }
        if("VERIFIED".equals(p.phase)){phase(writer,p,"RENAMED",p.baselineName,p.baselineSha,p.sourceRoot);p=publication(root);p.schemaHash=owner[0];}
        if(!"RENAMED".equals(p.phase))throw new IllegalStateException("R29_RESEARCH_RESUME_PHASE_INVALID");
        return p;
    }
    private static String finishPublication(DbStore store,ResearchPoolR27.Worker worker,File published,Publication p,long deadline)throws Exception{
        if(!"PUBLISHED".equals(p.phase))verifyFence(worker.realtimeRo(),p.epoch,deadline,worker::cancelled);
        if(!"PUBLISHED".equals(p.phase))ResearchRouteR29.publish(store,published,p.schemaHash,p.sourceRoot,p.epoch);
        worker.pool().reloadResearchReaders();return "ACTIVE_INDEPENDENT_RESEARCH_WRITER";
    }
    private static String copyAndPublish(DbStore store,ResearchPoolR27.Worker worker)throws Exception{
        long deadline=android.os.SystemClock.elapsedRealtime()+BUDGET_MS;
        SQLiteDatabase source=worker.realtimeRo();
        File candidate=store.contextForR26().getDatabasePath(CANDIDATE),published=store.contextForR26().getDatabasePath(ResearchRouteR29.FILE_NAME);
        if(new StatFs(candidate.getParent()).getAvailableBytes()<RESERVE_BYTES)return "BLOCKED_STORAGE_RESERVE";
        // An existing durable reservation resumes before any repeated Root schema/DDL admission.
        if(publication(source)==null){
            final boolean candidateExists=candidate.exists()||published.exists();
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R29_CUTOVER_RESERVE",db->{
                db.execSQL(PUBLICATION);db.execSQL(FENCE);ResearchRouteR29.ensureRealtime(db);RealtimeResearchProjectionR29.ensureRealtime(db);StrategySchemaDomainR29.ensureRealtime(db);CriticalInputMirrorR29.ensureRealtime(db);LegacyAuthorityProjectionR29.ensureRealtime(db);
                Publication current=publication(db);
                if(current==null&&candidateExists)throw new IllegalStateException("R29_UNOWNED_CANDIDATE_REQUIRES_REVIEW");
                if(current==null){long epoch=Math.max(1,System.currentTimeMillis());
                    db.execSQL("INSERT INTO r29_migration_publication(singleton,epoch,candidate_name,phase,updated_at) VALUES(1,?,?,'PREPARED',?)",new Object[]{epoch,CANDIDATE,System.currentTimeMillis()});
                    db.execSQL("INSERT INTO r29_migration_owner_fence VALUES(1,?,?,?,?)",new Object[]{epoch,FENCE_PROTOCOL,"",System.currentTimeMillis()});
                }
                return 0;
            });
        }
        if(ResearchRouteR29.active(source)){ResearchRouteR29.verifyActiveOwner(source);return "ACTIVE";}
        bindFence(store,source,publication(source).epoch,deadline,worker::cancelled);
        MigrationCopyR29.Transaction writer=body->store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R29_CUTOVER_PHASE",db->{body.run(db);return 0;});
        Publication resumed=resumePublication(source,candidate,published,writer,deadline,worker::cancelled);
        if(resumed!=null)return finishPublication(store,worker,published,resumed,deadline);
        long epoch=publication(source).epoch;
        Prepared prepared=withSourceSnapshot(source,pinned->prepareCandidateSnapshot(worker,pinned,candidate,epoch,deadline));
        // The source read transaction is now closed. File sealing and short Root publication cannot
        // accidentally keep a Root WAL reader pinned while opening/checkpointing publication handles.
        return sealAndPublish(store,worker,candidate,published,prepared,deadline);
    }
    private static final class Prepared{
        final long epoch;final String schema,root;
        Prepared(long epoch,String schema,String root){this.epoch=epoch;this.schema=schema;this.root=root;}
    }
    private static Prepared prepareCandidateSnapshot(ResearchPoolR27.Worker worker,SQLiteDatabase source,File candidate,long epoch,long deadline)throws Exception{
        List<MigrationCopyR29.Table> tables=tables(source);String schema=schemaHash(tables),copiedRoot;
        long inputEpoch=CriticalInputMirrorR29.sourceEpoch(source),inputSequence=CriticalInputMirrorR29.sourceSequence(source);String inputView=CriticalInputMirrorR29.sourceViewSql(source);
        SQLiteDatabase target=SQLiteDatabase.openOrCreateDatabase(candidate,null);
        try{
            if(!target.enableWriteAheadLogging())throw new IllegalStateException("R29_CANDIDATE_WAL_REQUIRED");target.execSQL("PRAGMA synchronous=FULL");
            target.beginTransactionNonExclusive();try{
                target.execSQL(ResearchRouteR29.OWNER_DDL);
                target.execSQL("INSERT INTO r29_research_owner VALUES(1,'COPYING',?,'',?,?)",new Object[]{schema,epoch,System.currentTimeMillis()});
                for(MigrationCopyR29.Table t:tables)target.execSQL(t.createSql);
                target.setTransactionSuccessful();
            }finally{target.endTransaction();}
            for(MigrationCopyR29.Table t:tables){
                Long after=null;
                for(;;){check(deadline,worker::cancelled);List<MigrationCopyR29.Row> rows=page(source,t,after,deadline,worker::cancelled);if(rows.isEmpty())break;
                    long needed=RESERVE_BYTES;for(MigrationCopyR29.Row row:rows)needed+=row.payload.length;
                    if(new StatFs(candidate.getParent()).getAvailableBytes()<needed)throw new IllegalStateException("R29_CUTOVER_STORAGE_BUDGET_YIELD");
                    target.beginTransactionNonExclusive();try{restorePage(target,t,rows);target.setTransactionSuccessful();}finally{target.endTransaction();}
                    after=rows.get(rows.size()-1).rowid;
                }
            }
            copiedRoot=root(target,tables,deadline,worker::cancelled);
            if(!copiedRoot.equals(root(source,tables,deadline,worker::cancelled)))throw new IllegalStateException("R29_FINAL_CATCHUP_HASH_MISMATCH");
            Set<String> names=new HashSet<>();for(MigrationCopyR29.Table t:tables)names.add(t.name);
            target.beginTransactionNonExclusive();try{
                try(Cursor c=source.rawQuery("SELECT name,tbl_name,sql FROM sqlite_master WHERE type IN ('index','trigger') AND sql IS NOT NULL ORDER BY type,name",null)){
                    while(c.moveToNext())if(names.contains(c.getString(1))&&!c.getString(0).startsWith("r29_copy_track_")&&!c.getString(1).equals("draws")&&!c.getString(1).equals("su1_source_clock")&&!c.getString(1).equals("r26_feedback")&&!c.getString(1).equals("app_state")&&!CriticalInputMirrorR29.tables().contains(c.getString(1)))target.execSQL(c.getString(2));
                }
                ResearchInputMirrorR29.initialized(target,source);
                if(ResearchInputMirrorR29.table(target,"r26_live_model"))LegacyLiveProjectionR29.installResearch(target);
                LegacyAuthorityProjectionR29.installResearch(target);
                CriticalInputMirrorR29.installResearch(target);
                LegacyLearnedInputR29.installResearch(target);
                com.ethk3.novaonepro.LegacyLearningCommitR29.installResearch(target);
                // This receipt names the captured snapshot's offset, never the subsequently moving
                // Root head. The live mirror resumes from it after ownership publication.
                CriticalInputMirrorR29.initialized(target,inputEpoch,inputSequence,inputView);
                target.execSQL(LegacyDomainUpgradeR29.DDL);
                target.execSQL("INSERT OR REPLACE INTO r29_legacy_domain_owner VALUES(1,'FULL',?,?,?)",new Object[]{epoch,copiedRoot,System.currentTimeMillis()});
                LegacyDomainExtensionR29.includedInFull(target,epoch,copiedRoot);
                RealtimeResearchProjectionR29.installResearch(target);
                target.execSQL("UPDATE r29_research_owner SET phase='READY',source_root=? WHERE singleton=1",new Object[]{copiedRoot});target.setTransactionSuccessful();
            }finally{target.endTransaction();}
            try(Cursor c=target.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)",null)){if(!c.moveToFirst()||c.getInt(0)!=0)throw new IllegalStateException("R29_CANDIDATE_CHECKPOINT_BUSY");}
        }finally{target.close();}
        return new Prepared(epoch,schema,copiedRoot);
    }
    private static String sealAndPublish(DbStore store,ResearchPoolR27.Worker worker,File candidate,File published,Prepared prepared,long deadline)throws Exception{
        sync(candidate);check(deadline,worker::cancelled);
        readyOwner(candidate,prepared.epoch,prepared.root,prepared.schema);
        ResearchRouteR29.verifyPreparedReader(candidate,worker.realtimeRo().getPath());
        File baseline=new File(candidate.getParentFile(),baselineName(prepared.epoch));
        if(new StatFs(candidate.getParent()).getAvailableBytes()<candidate.length()+RESERVE_BYTES)throw new IllegalStateException("R29_CUTOVER_STORAGE_BUDGET_YIELD");
        copyBaseline(candidate,baseline,deadline,worker::cancelled);
        MigrationCopyR29.Transaction writer=body->store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R29_CUTOVER_PHASE",db->{body.run(db);return 0;});
        Publication resumed=resumePublication(worker.realtimeRo(),candidate,published,writer,deadline,worker::cancelled);
        if(resumed==null)throw new IllegalStateException("R29_COMPLETED_COPY_PROOF_MISSING");
        return finishPublication(store,worker,published,resumed,deadline);
    }
    static void removeUnpublished(File file)throws IOException{for(String suffix:new String[]{"","-wal","-shm"}){File part=new File(file.getPath()+suffix);if(part.exists()&&!part.delete())throw new IOException("R29_UNPUBLISHED_CANDIDATE_DELETE_FAILED");}}
    static void copyBaseline(File source,File baseline,long deadline,java.util.function.BooleanSupplier cancelled)throws Exception{
        if(baseline.exists())throw new IOException("R29_SEALED_DESTINATION_ALREADY_EXISTS");
        File temporary=new File(baseline.getPath()+".partial");
        try(InputStream in=new FileInputStream(source);FileOutputStream out=new FileOutputStream(temporary)){
            byte[] b=new byte[64<<10];for(int n;(n=in.read(b))>=0;){check(deadline,cancelled);out.write(b,0,n);}out.getFD().sync();
        }
        if(!fileHash(source,deadline,cancelled).equals(fileHash(temporary,deadline,cancelled)))throw new IOException("R29_BASELINE_COPY_HASH_MISMATCH");
        if(!temporary.renameTo(baseline))throw new IOException("R29_BASELINE_PUBLISH_FAILED");
    }
    static void sync(File file)throws IOException{try(FileOutputStream out=new FileOutputStream(file,true)){out.getFD().sync();}}
    static String fileHash(File file,long deadline,java.util.function.BooleanSupplier cancelled)throws Exception{
        MessageDigest digest=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(file)){byte[] b=new byte[64<<10];for(int n;(n=in.read(b))>=0;){check(deadline,cancelled);digest.update(b,0,n);}}StringBuilder hex=new StringBuilder();for(byte b:digest.digest())hex.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return hex.toString();
    }
}
