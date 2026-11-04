package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;
import java.util.function.BooleanSupplier;

/** R27-018 offline history import (docs/03 3.1). Each CSV row is compared with the database row of the same issue:
 * identical permutation → MATCH (nothing imported, the database stays the source); different → CONFLICT (listed,
 * never used); absent from the database → IMPORTED into r27_history_import, a replay-only table (the draws table,
 * settlement and P0 are untouched). Replay datasets use imported rows only for issues older than the database. */
public final class HistoryImportR27 {
    private HistoryImportR27() {}
    public static final String DDL = "CREATE TABLE IF NOT EXISTS r27_history_import(issue TEXT PRIMARY KEY,ordinal INTEGER NOT NULL,numbers TEXT NOT NULL,status TEXT NOT NULL CHECK(status IN ('IMPORTED','CONFLICT','MATCH')),db_numbers TEXT,source TEXT NOT NULL,imported_at INTEGER NOT NULL)";
    public static final int R31_PUBLICATION_REVISION = 1;
    public static final String R31_IMPORT_ABI = "R31_HISTORY_CSV_V1";
    public static final String R31_ROLE = "REPLAY_ONLY_CANONICAL_OLDER";
    public static final String R31_RUN = "r31_history_import_run", R31_ROW = "r31_history_import_row";
    /** Existing schema owner only. Business import and readers never execute these statements. */
    public static final String[] R31_PUBLICATION_DDL = {
        "CREATE TABLE IF NOT EXISTS r31_history_import_run(job_id TEXT PRIMARY KEY,source_hash TEXT NOT NULL,parser_abi TEXT NOT NULL,source_path TEXT NOT NULL,source_bytes INTEGER NOT NULL,data_role TEXT NOT NULL CHECK(data_role='REPLAY_ONLY_CANONICAL_OLDER'),status TEXT NOT NULL CHECK(status IN ('STAGING','COMPLETE')),owner_epoch TEXT NOT NULL,fence INTEGER NOT NULL,owner_pid INTEGER NOT NULL,owner_boot TEXT NOT NULL,lease_elapsed_ms INTEGER NOT NULL,cursor_line INTEGER NOT NULL DEFAULT 0,chunk_seq INTEGER NOT NULL DEFAULT 0,invalid_rows INTEGER NOT NULL DEFAULT 0,manifest_hash TEXT,row_count INTEGER NOT NULL DEFAULT 0,match_rows INTEGER NOT NULL DEFAULT 0,conflict_rows INTEGER NOT NULL DEFAULT 0,imported_rows INTEGER NOT NULL DEFAULT 0,duplicate_rows INTEGER NOT NULL DEFAULT 0,duplicate_conflicts INTEGER NOT NULL DEFAULT 0,gaps INTEGER NOT NULL DEFAULT 0,missing_issues INTEGER NOT NULL DEFAULT 0,first_ordinal INTEGER,last_ordinal INTEGER,publish_seq INTEGER NOT NULL DEFAULT 0,created_at INTEGER NOT NULL,completed_at INTEGER NOT NULL DEFAULT 0,UNIQUE(source_hash,parser_abi))",
        "CREATE TABLE IF NOT EXISTS r31_history_import_row(job_id TEXT NOT NULL,issue TEXT NOT NULL CHECK(length(issue)=12),ordinal INTEGER NOT NULL,numbers TEXT NOT NULL CHECK(length(numbers)=20),status TEXT NOT NULL CHECK(status IN ('IMPORTED','CONFLICT','MATCH')),db_numbers TEXT,source_line INTEGER NOT NULL,duplicates INTEGER NOT NULL DEFAULT 0,duplicate_conflicts INTEGER NOT NULL DEFAULT 0,last_conflict_line INTEGER NOT NULL DEFAULT 0,row_hash TEXT NOT NULL,PRIMARY KEY(job_id,issue),FOREIGN KEY(job_id) REFERENCES r31_history_import_run(job_id))",
        "CREATE INDEX IF NOT EXISTS r31_history_import_order ON r31_history_import_row(job_id,ordinal,issue)",
        "CREATE INDEX IF NOT EXISTS r31_history_import_issue ON r31_history_import_row(issue,job_id)",
        "CREATE UNIQUE INDEX IF NOT EXISTS r31_history_import_publication ON r31_history_import_run(publish_seq) WHERE status='COMPLETE'",
        "CREATE TRIGGER IF NOT EXISTS r31_history_import_run_immutable BEFORE UPDATE ON r31_history_import_run WHEN OLD.status='COMPLETE' BEGIN SELECT RAISE(ABORT,'HISTORY_IMPORT_COMPLETE_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS r31_history_import_run_keep BEFORE DELETE ON r31_history_import_run WHEN OLD.status='COMPLETE' BEGIN SELECT RAISE(ABORT,'HISTORY_IMPORT_EVIDENCE_PROTECTED'); END",
        "CREATE TRIGGER IF NOT EXISTS r31_history_import_row_no_insert BEFORE INSERT ON r31_history_import_row WHEN EXISTS(SELECT 1 FROM r31_history_import_run j WHERE j.job_id=NEW.job_id AND j.status='COMPLETE') BEGIN SELECT RAISE(ABORT,'HISTORY_IMPORT_COMPLETE_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS r31_history_import_row_no_update BEFORE UPDATE ON r31_history_import_row WHEN EXISTS(SELECT 1 FROM r31_history_import_run j WHERE j.job_id=OLD.job_id AND j.status='COMPLETE') BEGIN SELECT RAISE(ABORT,'HISTORY_IMPORT_COMPLETE_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS r31_history_import_row_keep BEFORE DELETE ON r31_history_import_row WHEN EXISTS(SELECT 1 FROM r31_history_import_run j WHERE j.job_id=OLD.job_id AND j.status='COMPLETE') BEGIN SELECT RAISE(ABORT,'HISTORY_IMPORT_EVIDENCE_PROTECTED'); END"
    };
    public static void ensureR31PublicationSchema(SQLiteDatabase db) {
        if (!db.inTransaction()) throw new IllegalStateException("HISTORY_IMPORT_MIGRATION_OWNER_REQUIRED");
        for (String sql : R31_PUBLICATION_DDL) db.execSQL(sql);
    }
    /** One read-only relation: append-only complete jobs override only issues actually present in that CSV. */
    public static String publishedRowsSqlR31() {
        String newer = "SELECT 1 FROM r31_history_import_row n JOIN r31_history_import_run k ON k.job_id=n.job_id WHERE k.status='COMPLETE' AND n.duplicate_conflicts=0 AND n.issue=";
        return "(SELECT l.issue,l.ordinal,l.numbers,l.status,l.db_numbers FROM r27_history_import l WHERE NOT EXISTS(" + newer + "l.issue) UNION ALL SELECT r.issue,r.ordinal,r.numbers,r.status,r.db_numbers FROM r31_history_import_row r JOIN r31_history_import_run j ON j.job_id=r.job_id WHERE j.status='COMPLETE' AND r.duplicate_conflicts=0 AND NOT EXISTS(" + newer + "r.issue AND k.publish_seq>j.publish_seq))";
    }
    /** Scalar expression for ReportSnapshot's one-SELECT contract, never a second detached database read. */
    public static String publishedCountSqlR31() { return "(SELECT count(*) FROM " + publishedRowsSqlR31() + ")"; }
    public static boolean publicationSchemaReadyReadOnlyR31(SQLiteDatabase db) {
        try (Cursor run = db.rawQuery("SELECT source_hash,parser_abi,source_path,data_role,status,owner_epoch,fence,cursor_line,chunk_seq,manifest_hash,publish_seq FROM r31_history_import_run LIMIT 0", null);
             Cursor rows = db.rawQuery("SELECT job_id,issue,ordinal,numbers,status,db_numbers,source_line,duplicates,duplicate_conflicts,last_conflict_line,row_hash FROM r31_history_import_row LIMIT 0", null);
             Cursor legacy = db.rawQuery("SELECT issue,ordinal,numbers,status,db_numbers FROM r27_history_import LIMIT 0", null)) {
            if (run.getColumnCount() != 11 || rows.getColumnCount() != 11 || legacy.getColumnCount() != 5) return false;
            for (String expected : R31_PUBLICATION_DDL) {
                java.util.regex.Matcher name = java.util.regex.Pattern.compile("^CREATE (?:UNIQUE )?(?:TABLE|INDEX|TRIGGER) IF NOT EXISTS (\\w+)").matcher(expected);
                if (!name.find()) throw new IllegalStateException("HISTORY_SCHEMA_DECLARATION");
                try (Cursor sql = db.rawQuery("SELECT sql FROM sqlite_master WHERE name=?", new String[] {name.group(1)})) {
                    if (!sql.moveToFirst() || !canonicalDdl(expected).equals(canonicalDdl(sql.getString(0)))) return false;
                }
            }
            return true;
        } catch (RuntimeException missing) { return false; }
    }
    private static String canonicalDdl(String sql) { return sql == null ? "" : sql.replace(" IF NOT EXISTS ", " ").trim().replaceAll("\\s+", " "); }

    private static final int CHUNK_ROWS_R31 = 16, COPY_BYTES_R31 = 8192;
    private static final long LEASE_MS_R31 = 30_000, FILE_PHASE_BYTES_R31 = 1L << 20, DB_PHASE_BYTES_R31 = 2L << 20;
    private static void checkCancelled(BooleanSupplier cancelled) throws InterruptedException {
        if (Thread.currentThread().isInterrupted() || cancelled.getAsBoolean()) throw new InterruptedException("HISTORY_IMPORT_CANCELLED");
    }
    private static String hex(byte[] digest) { StringBuilder s = new StringBuilder(); for (byte b : digest) s.append(Character.forDigit((b >>> 4) & 15, 16)).append(Character.forDigit(b & 15, 16)); return s.toString(); }
    private static File dataRoot(DbStore store) throws IOException { return store.contextForR26().getFilesDir().getCanonicalFile().getParentFile(); }
    private static File privatePath(DbStore store, String relative) throws IOException {
        File root = dataRoot(store), file = new File(root, relative).getAbsoluteFile();
        if (!file.equals(file.getCanonicalFile()) || !file.getPath().startsWith(root.getPath() + File.separator)) throw new IOException("HISTORY_SOURCE_PRIVATE_PATH");
        return file;
    }
    /** One input snapshot remains referenced by its manifest until an authorized reference-aware retirement. */
    private static final class SourceR31 implements AutoCloseable {
        final DbStore store; final String path, hash; final long bytes; final StorageBudgetR31.Reservation reservation;
        SourceR31(DbStore store, String path, String hash, long bytes, StorageBudgetR31.Reservation reservation) { this.store=store;this.path=path;this.hash=hash;this.bytes=bytes;this.reservation=reservation; }
        @Override public void close() throws IOException {
            try {
                boolean referenced = true;
                try (Cursor row = store.readable().rawQuery("SELECT 1 FROM r31_history_import_run WHERE source_path=? LIMIT 1", new String[] {path})) { referenced = row.moveToFirst(); }
                catch (RuntimeException uncertain) { /* An unavailable DB is not proof that the snapshot is unreferenced. */ }
                if (!referenced) {
                    File source = privatePath(store,path);
                    if (source.exists() && !source.delete()) throw new IOException("HISTORY_UNREFERENCED_SOURCE_RELEASE_PENDING");
                    File directory = source.getParentFile(); if (directory.isDirectory() && !directory.delete()) throw new IOException("HISTORY_SOURCE_DIRECTORY_RELEASE_PENDING");
                }
            } finally { reservation.close(); }
        }
    }
    private static SourceR31 snapshot(DbStore store, InputStream input, BooleanSupplier cancelled) throws Exception {
        StorageBudgetR31 budget = new StorageBudgetR31(dataRoot(store));
        String scope = "files/r31-history-inputs/" + UUID.randomUUID(), relative = scope + "/source.csv";
        StorageBudgetR31.Reservation disk = budget.reserve("HISTORY_SOURCE",0,StorageBudgetR31.Category.INPUT_RESEARCH,scope,FILE_PHASE_BYTES_R31,false);
        MemoryBudgetR31.Admission admission = MemoryBudgetR31.processBudget().tryAcquire("HISTORY_SOURCE_BUFFER",MemoryBudgetR31.Kind.RESEARCH,128L << 10,cancelled);
        if (!admission.acquired()) { disk.close(); throw new MemoryBudgetR31.CapacityWait(admission.reason); }
        boolean transferred = false;
        try (MemoryBudgetR31.Lease memory = admission.lease) {
            checkCancelled(cancelled); File file = privatePath(store,relative);
            if (!file.getParentFile().mkdirs() && !file.getParentFile().isDirectory()) throw new IOException("HISTORY_SOURCE_DIRECTORY");
            byte[] buffer = new byte[COPY_BYTES_R31]; memory.allocatedPrimitiveArrayR31(buffer);
            java.security.MessageDigest hash = java.security.MessageDigest.getInstance("SHA-256"); long bytes = 0, admitted = FILE_PHASE_BYTES_R31;
            try (FileOutputStream output = new FileOutputStream(file)) {
                for (int count; (count = input.read(buffer)) != -1;) {
                    checkCancelled(cancelled); if (count == 0) continue;
                    if (bytes + count + 8192 > admitted) { disk.ensureAdditional(FILE_PHASE_BYTES_R31,bytes + FILE_PHASE_BYTES_R31,false); admitted = disk.maximumGrowthBytes(); }
                    disk.beforeChunk(count); output.write(buffer,0,count); hash.update(buffer,0,count); bytes += count;
                }
                output.getFD().sync();
            }
            disk.reconcile(); checkCancelled(cancelled);
            SourceR31 prepared = new SourceR31(store,relative,hex(hash.digest()),bytes,disk); transferred=true; return prepared;
        } finally {
            if (!transferred) {
                try { File file=privatePath(store,relative); if (file.exists() && !file.delete()) throw new IOException("HISTORY_FAILED_SOURCE_RELEASE_PENDING"); File dir=file.getParentFile(); if(dir.exists()&&!dir.delete())throw new IOException("HISTORY_FAILED_SOURCE_DIRECTORY_RELEASE_PENDING"); }
                finally { disk.close(); }
            }
        }
    }
    /** Select only three bounded CSV fields; ignored metadata can be arbitrarily long without a growing line buffer. */
    private static final class CsvR31 implements AutoCloseable {
        final PushbackReader reader;
        CsvR31(InputStream input) { reader = new PushbackReader(new BufferedReader(new InputStreamReader(input,StandardCharsets.UTF_8),8192),1); }
        String[] next(BooleanSupplier cancelled) throws Exception {
            StringBuilder issue=new StringBuilder(),numbers=new StringBuilder(),valid=new StringBuilder();
            int field=0;boolean quote=false,any=false,overflow=false;long chars=0;
            for (;;) {
                int c=reader.read();if(c==-1){if(!any)return null;break;}any=true;if((++chars&8191)==0)checkCancelled(cancelled);
                if(c=='"'){if(quote){int next=reader.read();if(next=='"')c='"';else{quote=false;if(next!=-1)reader.unread(next);continue;}}else{quote=true;continue;}}
                else if(c==','&&!quote){field++;continue;}
                else if((c=='\n'||c=='\r')&&!quote){if(c=='\r'){int next=reader.read();if(next!=-1&&next!='\n')reader.unread(next);}break;}
                StringBuilder target=field==0?issue:field==3?numbers:field==6?valid:null;
                if ((field==3||field==6) && Character.isWhitespace((char)c)) continue;
                if(target!=null){if(target.length()<128)target.append((char)c);else overflow=true;}
            }
            return new String[]{issue.toString(),numbers.toString(),valid.toString(),overflow||quote?"INVALID":"OK"};
        }
        void header(BooleanSupplier cancelled)throws Exception{String[] fields=next(cancelled);if(fields==null||!"period".equals(fields[0]))throw new IOException("R27_CSV_HEADER");}
        @Override public void close()throws IOException{reader.close();}
    }
    private static final class JobR31 {
        String id,source,path,owner,abi,role;long fence,cursor,chunk,created,invalid,sourceBytes;boolean complete;
    }
    private static JobR31 job(SQLiteDatabase db,String id) {
        try(Cursor c=db.rawQuery("SELECT source_hash,source_path,owner_epoch,fence,cursor_line,chunk_seq,created_at,invalid_rows,status,parser_abi,data_role,source_bytes FROM r31_history_import_run WHERE job_id=?",new String[]{id})){
            if(!c.moveToFirst())return null;JobR31 j=new JobR31();j.id=id;j.source=c.getString(0);j.path=c.getString(1);j.owner=c.getString(2);j.fence=c.getLong(3);j.cursor=c.getLong(4);j.chunk=c.getLong(5);j.created=c.getLong(6);j.invalid=c.getLong(7);j.complete="COMPLETE".equals(c.getString(8));j.abi=c.getString(9);j.role=c.getString(10);j.sourceBytes=c.getLong(11);return j;
        }
    }
    private static final class DependencyR31 extends IllegalStateException implements FailureDispositionR31.ClassifiedFailure {
        DependencyR31(String reason) { super("HISTORY_IMPORT_"+reason); }
        public String failureCategory(){return "DEPENDENCY";}public String operation(){return "HISTORY_IMPORT";}public String phase(){return "PREPARE_OR_PUBLICATION";}public String commitOutcomeName(){return "NOT_SUBMITTED";}
    }
    private static final class IntegrityR31 extends IllegalStateException implements FailureDispositionR31.ClassifiedFailure {
        IntegrityR31(String reason){super("HISTORY_IMPORT_INTEGRITY:"+reason);}
        public String failureCategory(){return "INTEGRITY";}public String operation(){return "HISTORY_IMPORT";}public String phase(){return "PREPARE_OR_PUBLICATION";}public String commitOutcomeName(){return "NOT_SUBMITTED";}
    }
    private static JobR31 claim(DbStore store,SourceR31 source,BooleanSupplier cancelled)throws Exception {
        checkCancelled(cancelled);String id=StatsR27.sha256(R31_IMPORT_ABI+"|"+source.hash),epoch=UUID.randomUUID().toString();
        return store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.RESEARCH_CONTROL,"R31_HISTORY_IMPORT_CLAIM").withDeadlineMonoMs(TimeSourceR811.monoNowMs()+200),db->{
            JobR31 prior=job(db,id);if(prior!=null&&(!source.hash.equals(prior.source)||!R31_IMPORT_ABI.equals(prior.abi)||!R31_ROLE.equals(prior.role)))throw new IntegrityR31("SOURCE_IDENTITY_CONFLICT");if(prior!=null&&prior.complete)return prior;
            long now=android.os.SystemClock.elapsedRealtime();String boot=IssueTimelineWriterR30.bootId();
            if(prior==null){try(Cursor active=db.rawQuery("SELECT count(*) FROM r31_history_import_run WHERE status='STAGING' AND owner_boot=? AND lease_elapsed_ms>?",new String[]{boot,String.valueOf(now)})){if(active.moveToFirst()&&active.getLong(0)>=4)throw new DependencyR31("ACTIVE_INPUT_CAPACITY");}
                db.execSQL("INSERT INTO r31_history_import_run(job_id,source_hash,parser_abi,source_path,source_bytes,data_role,status,owner_epoch,fence,owner_pid,owner_boot,lease_elapsed_ms,created_at) VALUES(?,?,?,?,?,?,'STAGING','',0,0,'',0,?)",new Object[]{id,source.hash,R31_IMPORT_ABI,source.path,source.bytes,R31_ROLE,System.currentTimeMillis()});
            }
            try(Cursor owner=db.rawQuery("SELECT owner_boot,lease_elapsed_ms FROM r31_history_import_run WHERE job_id=?",new String[]{id})){if(owner.moveToFirst()&&boot.equals(owner.getString(0))&&owner.getLong(1)>now)throw new DependencyR31("OWNER_DRAINING");}
            db.execSQL("UPDATE r31_history_import_run SET owner_epoch=?,fence=fence+1,owner_pid=?,owner_boot=?,lease_elapsed_ms=? WHERE job_id=? AND status='STAGING'",new Object[]{epoch,android.os.Process.myPid(),boot,now+LEASE_MS_R31,id});
            JobR31 current=job(db,id);if(current==null||!epoch.equals(current.owner)||!source.hash.equals(current.source))throw new DependencyR31("CLAIM_CAS");return current;
        });
    }
    private static void requireOwner(SQLiteDatabase db,JobR31 j,long cursor,long sequence) {
        try(Cursor c=db.rawQuery("SELECT 1 FROM r31_history_import_run WHERE job_id=? AND source_hash=? AND status='STAGING' AND owner_epoch=? AND fence=? AND owner_boot=? AND lease_elapsed_ms>? AND cursor_line=? AND chunk_seq=?",new String[]{j.id,j.source,j.owner,String.valueOf(j.fence),IssueTimelineWriterR30.bootId(),String.valueOf(android.os.SystemClock.elapsedRealtime()),String.valueOf(cursor),String.valueOf(sequence)})){
            if(!c.moveToFirst())throw new DependencyR31("STALE_OWNER_OR_CURSOR");
        }
    }
    private static void pulse(DbStore store,JobR31 j) {
        long now=android.os.SystemClock.elapsedRealtime();
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.RESEARCH_CONTROL,"R31_HISTORY_IMPORT_RENEW").withDeadlineMonoMs(TimeSourceR811.monoNowMs()+200),db->{requireOwner(db,j,j.cursor,j.chunk);db.execSQL("UPDATE r31_history_import_run SET lease_elapsed_ms=? WHERE job_id=? AND owner_epoch=? AND fence=?",new Object[]{now+LEASE_MS_R31,j.id,j.owner,j.fence});return 0;});
    }
    private static void verifySource(DbStore store,JobR31 job,BooleanSupplier work)throws Exception {
        File file=privatePath(store,job.path);if(!file.isFile()||file.length()!=job.sourceBytes)throw new IntegrityR31("REFERENCED_SOURCE_MISSING_OR_CHANGED");
        java.security.MessageDigest hash=java.security.MessageDigest.getInstance("SHA-256");byte[] buffer=new byte[COPY_BYTES_R31];
        try(InputStream input=new FileInputStream(file)){for(int count;(count=input.read(buffer))!=-1;){checkCancelled(work);if(count>0)hash.update(buffer,0,count);}}
        if(!job.source.equals(hex(hash.digest())))throw new IntegrityR31("REFERENCED_SOURCE_HASH");
    }
    private static String canonicalNumbers(SQLiteDatabase db,String issue) {
        try(Cursor c=db.rawQuery("SELECT n1,n2,n3,n4,n5,n6,n7,n8,n9,n10 FROM draws WHERE issue=? AND dirty=0",new String[]{issue})){
            if(!c.moveToFirst())return null;int[] values=new int[10];for(int i=0;i<10;i++)values[i]=c.getInt(i);return join(values);
        }
    }
    private static final class RowR31 {
        final String issue,numbers;final long ordinal,line;String db,status;
        RowR31(String issue,int[] values,long line){this.issue=issue;this.numbers=join(values);this.ordinal=TargetIssueResolver.sequenceOrdinalSU1(issue);this.line=line;}
    }
    private static RowR31 parse(String[] fields,long line) {
        if(!"OK".equals(fields[3])||!TargetIssueResolver.isValid(fields[0]))return null;
        try{String[] values=fields[1].split(",",-1);if(values.length!=10)return null;int[] numbers=new int[10];for(int i=0;i<10;i++)numbers[i]=Integer.parseInt(values[i]);return ReplayDatasetR27.legal(numbers)?new RowR31(fields[0],numbers,line):null;}
        catch(NumberFormatException invalid){return null;}
    }
    private static StorageBudgetR31.Reservation databaseBudget(DbStore store,JobR31 j)throws IOException {
        File root=dataRoot(store),directory=new File(store.readable().getPath()).getCanonicalFile().getParentFile();
        if(directory==null||!directory.getPath().startsWith(root.getPath()+File.separator))throw new IOException("HISTORY_DATABASE_PRIVATE_PATH");
        String relative=directory.getPath().substring(root.getPath().length()+1);
        return new StorageBudgetR31(root).reserve("HISTORY_DB:"+j.id,j.fence,StorageBudgetR31.Category.DATABASE,relative,DB_PHASE_BYTES_R31,false);
    }
    private static void submit(DbStore store,JobR31 job,ResearchWriteQueueR27.Batch draft,String part,long sequence,BooleanSupplier cancelled)throws Exception {
        String operation="HISTORY_IMPORT_R31",key=StatsR27.sha256(R31_IMPORT_ABI+"|"+job.id+"|"+part+"|"+sequence);
        ResearchWriteQueueR27.ImmutableBatch prepared=draft.prepareR31(operation,key,"HISTORY:"+job.id,job.owner,job.fence,job.source,sequence,R31_IMPORT_ABI);
        if(prepared.opCount>64||prepared.bindBytes>Math.min(262144L,Runtime.getRuntime().maxMemory()/512L))throw new IllegalStateException("HISTORY_PREPARED_CHUNK_BOUND");
        ResearchWriteQueueR27 queue=new ResearchWriteQueueR27(store);
        try(StorageBudgetR31.Reservation disk=databaseBudget(store,job)){
            disk.beforeChunk(DB_PHASE_BYTES_R31);
            while(true){checkCancelled(cancelled);pulse(store,job);if(queue.submit(operation,prepared,cancelled))break;Thread.sleep(25);}
            disk.reconcile();
        }
    }
    private static void chunk(DbStore store,JobR31 job,List<RowR31> rows,long through,long invalid,BooleanSupplier cancelled)throws Exception {
        final long cursor=job.cursor,sequence=job.chunk;
        for(RowR31 row:rows){row.db=canonicalNumbers(store.readable(),row.issue);row.status=row.db==null?"IMPORTED":row.db.equals(row.numbers)?"MATCH":"CONFLICT";}
        ResearchWriteQueueR27.Batch b=new ResearchWriteQueueR27.Batch();
        b.guardBefore(db->{requireOwner(db,job,cursor,sequence);for(RowR31 row:rows)if(!java.util.Objects.equals(row.db,canonicalNumbers(db,row.issue)))throw new DependencyR31("SOURCE_CHANGED_REPREPARE");return 0;});
        for(RowR31 row:rows){
            String rowHash=StatsR27.sha256(row.issue+'|'+row.numbers+'|'+row.status+'|'+String.valueOf(row.db)+'|'+row.line);
            b.add("INSERT OR IGNORE INTO r31_history_import_row(job_id,issue,ordinal,numbers,status,db_numbers,source_line,row_hash) VALUES(?,?,?,?,?,?,?,?)",job.id,row.issue,row.ordinal,row.numbers,row.status,row.db,row.line,rowHash);
            b.add("UPDATE r31_history_import_run SET row_count=row_count+changes() WHERE job_id=? AND owner_epoch=? AND fence=? AND status='STAGING'",job.id,job.owner,job.fence);
            b.add("UPDATE r31_history_import_row SET duplicates=duplicates+1,duplicate_conflicts=duplicate_conflicts+CASE WHEN numbers<>? THEN 1 ELSE 0 END,last_conflict_line=CASE WHEN numbers<>? THEN ? ELSE last_conflict_line END WHERE job_id=? AND issue=? AND source_line<?",row.numbers,row.numbers,row.line,job.id,row.issue,row.line);
        }
        b.add("UPDATE r31_history_import_run SET cursor_line=?,chunk_seq=?,invalid_rows=invalid_rows+? WHERE job_id=? AND owner_epoch=? AND fence=? AND cursor_line=? AND chunk_seq=? AND status='STAGING'",through,sequence+1,invalid,job.id,job.owner,job.fence,cursor,sequence);
        b.guardAfter(db->{requireOwner(db,job,through,sequence+1);return 0;});
        submit(store,job,b,"CHUNK",sequence+1,cancelled);job.cursor=through;job.chunk=sequence+1;job.invalid+=invalid;
    }
    private static final class SummaryR31 {
        long distinct,rows,match,conflict,imported,duplicates,duplicateConflicts,gaps,missing,first,last,lastSelfConflictLine;
        String hash,lastSelfConflict="";final StringBuilder conflicts=new StringBuilder();
        String lastConflict(){if(!lastSelfConflict.isEmpty())return lastSelfConflict;String text=conflicts.toString().trim();int split=text.lastIndexOf(' ');return split<0?text:text.substring(split+1);}
    }
    private static SummaryR31 summarize(DbStore store,JobR31 job,BooleanSupplier cancelled)throws Exception {
        SummaryR31 s=new SummaryR31();String after="";java.security.MessageDigest digest=java.security.MessageDigest.getInstance("SHA-256");
        digest.update((R31_IMPORT_ABI+'|'+job.id+'|'+job.source+'|'+job.cursor+'|'+job.invalid+'\n').getBytes(StandardCharsets.UTF_8));
        for(;;){checkCancelled(cancelled);int fetched=0;
            try(Cursor c=store.readable().rawQuery("SELECT issue,ordinal,numbers,status,db_numbers,duplicates,duplicate_conflicts,last_conflict_line,row_hash,source_line FROM r31_history_import_row WHERE job_id=? AND issue>? ORDER BY issue LIMIT 128",new String[]{job.id,after})){
                while(c.moveToNext()){
                    fetched++;after=c.getString(0);long ordinal=c.getLong(1),duplicates=c.getLong(5),bad=c.getLong(6),lastConflict=c.getLong(7);
                    String original=StatsR27.sha256(after+'|'+c.getString(2)+'|'+c.getString(3)+'|'+String.valueOf(c.isNull(4)?null:c.getString(4))+'|'+c.getLong(9));
                    if(!original.equals(c.getString(8)))throw new IntegrityR31("STAGED_ROW_HASH");
                    digest.update((original+'|'+duplicates+'|'+bad+'|'+lastConflict+'\n').getBytes(StandardCharsets.UTF_8));
                    s.distinct++;s.duplicates+=duplicates;s.duplicateConflicts+=bad;
                    if(lastConflict>s.lastSelfConflictLine){s.lastSelfConflictLine=lastConflict;s.lastSelfConflict=after;}
                    if(bad>0)continue;
                    if(s.rows==0)s.first=ordinal;else if(ordinal-s.last>1){s.gaps++;s.missing+=ordinal-s.last-1;}
                    s.last=ordinal;s.rows++;
                    String status=c.getString(3);if("MATCH".equals(status))s.match++;else if("CONFLICT".equals(status)){s.conflict++;if(s.conflicts.length()<400)s.conflicts.append(after).append(' ');}else if("IMPORTED".equals(status))s.imported++;else throw new IllegalStateException("HISTORY_STAGED_STATUS");
                }
            }
            if(fetched==0)break;
        }
        s.hash=hex(digest.digest());return s;
    }
    private static String completedMessage(SQLiteDatabase db,String id) {
        try(Cursor c=db.rawQuery("SELECT match_rows,conflict_rows,imported_rows,invalid_rows,duplicate_rows,duplicate_conflicts,missing_issues,gaps FROM r31_history_import_run WHERE job_id=? AND status='COMPLETE'",new String[]{id})){
            if(!c.moveToFirst())throw new DependencyR31("PUBLICATION_NOT_COMPLETE");
            return "历史CSV：比对一致 "+c.getLong(0)+"；冲突 "+c.getLong(1)+"（不导入）；新增回放专用 "+c.getLong(2)+"；非法行 "+c.getLong(3)+"；重复期 "+c.getLong(4)+"（其中自相矛盾 "+c.getLong(5)+"，不导入）；缺期 "+c.getLong(6)+"（"+c.getLong(7)+" 处断档，回放按段处理）。";
        }
    }
    private static final class ChangesR31 {
        long fence,insertCount,replaceCount,insertFirst,insertLast,replaceFirst,replaceLast;String members;
    }
    /** Reclassify against canonical input outside the writer; each changed page is fenced and bounded. */
    private static ChangesR31 refreshCanonical(DbStore store,JobR31 job,BooleanSupplier cancelled,BooleanSupplier work)throws Exception {
        ChangesR31 evidence=new ChangesR31();evidence.fence=ResearchSourceJournalR31.currentFence(store.readable());
        String after="";long page=0;
        for(;;){checkCancelled(work);List<RowR31> changed=new ArrayList<>();int fetched=0;
            try(Cursor c=store.readable().rawQuery("SELECT issue,numbers,status,db_numbers,source_line FROM r31_history_import_row WHERE job_id=? AND issue>? ORDER BY issue LIMIT 16",new String[]{job.id,after})){
                while(c.moveToNext()){
                    fetched++;after=c.getString(0);String canonical=canonicalNumbers(store.readable(),after),numbers=c.getString(1);
                    String status=canonical==null?"IMPORTED":canonical.equals(numbers)?"MATCH":"CONFLICT";
                    if(status.equals(c.getString(2))&&java.util.Objects.equals(canonical,c.isNull(3)?null:c.getString(3)))continue;
                    String[] parts=numbers.split(",",-1);int[] values=new int[10];for(int n=0;n<10;n++)values[n]=Integer.parseInt(parts[n]);
                    RowR31 row=new RowR31(after,values,c.getLong(4));row.db=canonical;row.status=status;changed.add(row);
                }
            }
            if(fetched==0)break;page++;
            if(!changed.isEmpty()){
                ResearchWriteQueueR27.Batch b=new ResearchWriteQueueR27.Batch();final long cursor=job.cursor,sequence=job.chunk;StringBuilder body=new StringBuilder();
                b.guardBefore(db->{requireOwner(db,job,cursor,sequence);for(RowR31 row:changed)if(!java.util.Objects.equals(row.db,canonicalNumbers(db,row.issue)))throw new DependencyR31("SOURCE_CHANGED_REPREPARE");return 0;});
                for(RowR31 row:changed){String hash=StatsR27.sha256(row.issue+'|'+row.numbers+'|'+row.status+'|'+String.valueOf(row.db)+'|'+row.line);body.append(hash);b.add("UPDATE r31_history_import_row SET status=?,db_numbers=?,row_hash=? WHERE job_id=? AND issue=? AND source_line=?",row.status,row.db,hash,job.id,row.issue,row.line);}
                b.guardAfter(db->{requireOwner(db,job,cursor,sequence);return 0;});
                submit(store,job,b,"CLASSIFY:"+evidence.fence+':'+StatsR27.sha256(body.toString()),page,cancelled);
            }
        }
        rebaseChanges(store,job,evidence,work);return evidence;
    }
    private static ChangesR31 prepareChanges(DbStore store,JobR31 job,SummaryR31 summary,ChangesR31 evidence,BooleanSupplier cancelled,BooleanSupplier work)throws Exception {
        ChangesR31 changes=new ChangesR31();changes.fence=evidence.fence;
        changes.members=StatsR27.sha256(job.id+'|'+summary.hash+"|MEMBERS|"+changes.fence);String after="";long part=0;
        for(;;){checkCancelled(work);List<Long> inserts=new ArrayList<>(),replaces=new ArrayList<>();int fetched=0;
            try(Cursor c=store.readable().rawQuery("SELECT issue,ordinal,numbers,status FROM r31_history_import_row WHERE job_id=? AND duplicate_conflicts=0 AND issue>? ORDER BY issue LIMIT 16",new String[]{job.id,after})){
                while(c.moveToNext()){
                    fetched++;after=c.getString(0);long ordinal=c.getLong(1);String before=null;
                    if(!"IMPORTED".equals(c.getString(3)))continue; // Current canonical data wins in every reader.
                    try(Cursor prior=store.readable().rawQuery("SELECT numbers FROM "+publishedRowsSqlR31()+" WHERE issue=? AND status='IMPORTED' LIMIT 1",new String[]{after})){if(prior.moveToFirst())before=prior.getString(0);}
                    // Different consumers use canonical tails of 544/5000 rows. Preserve their own cutoff;
                    // the shared source journal records changes to the actual published replay candidate pool.
                    String next=c.getString(2);
                    if(java.util.Objects.equals(before,next))continue;
                    if(before==null){inserts.add(ordinal);if(changes.insertCount++==0)changes.insertFirst=ordinal;changes.insertLast=ordinal;}
                    else{replaces.add(ordinal);if(changes.replaceCount++==0)changes.replaceFirst=ordinal;changes.replaceLast=ordinal;}
                }
            }
            if(fetched==0)break;part++;
            if(!inserts.isEmpty()||!replaces.isEmpty()){
                ResearchWriteQueueR27.Batch members=new ResearchWriteQueueR27.Batch();final long cursor=job.cursor,sequence=job.chunk;
                members.guardBefore(db->{requireOwner(db,job,cursor,sequence);return 0;});
                ResearchSourceJournalR31.appendImportMembers(members,changes.members,"INSERT",inserts);
                ResearchSourceJournalR31.appendImportMembers(members,changes.members,"REPLACE",replaces);
                members.guardAfter(db->{requireOwner(db,job,cursor,sequence);return 0;});
                submit(store,job,members,"MEMBERS:"+changes.members,part,cancelled);
            }
        }
        return changes;
    }
    /** Rebase only across proven irrelevant committed source events; all expensive membership reads stay outside the writer. */
    private static long rebaseChanges(DbStore store,JobR31 job,ChangesR31 prepared,BooleanSupplier work)throws Exception {
        for(;;){checkCancelled(work);SQLiteDatabase ro=store.readable();long before=ResearchSourceJournalR31.currentFence(ro);
            if(before==prepared.fence)return before;
            String candidate="SELECT 1 FROM r31_history_import_row r WHERE r.job_id=? AND r.duplicate_conflicts=0 AND ";
            String touched="SELECT 1 FROM r31_source_event e WHERE e.event_seq>? AND ((e.source_name<>'CSV_IMPORT' AND EXISTS("+candidate+"(r.ordinal=e.old_ordinal OR r.ordinal=e.new_ordinal))) OR (e.source_name='CSV_IMPORT' AND EXISTS(SELECT 1 FROM r31_source_member m WHERE m.member_set=e.member_set AND m.operation=CASE e.operation WHEN 'IMPORT_INSERT' THEN 'INSERT' ELSE 'REPLACE' END AND EXISTS("+candidate+"r.ordinal=m.ordinal)))) LIMIT 1";
            try(Cursor c=ro.rawQuery(touched,new String[]{String.valueOf(prepared.fence),job.id,job.id})){if(c.moveToFirst())throw new ResearchStoreR30R1.StaleResearch();}
            if(ResearchSourceJournalR31.currentFence(ro)==before)return before;
        }
    }
    private static boolean rollbackRetry(Throwable failure) {
        if(!(failure instanceof DatabaseCoordinator.WriteRejectedR31)||((DatabaseCoordinator.WriteRejectedR31)failure).commitOutcome!=DatabaseCoordinator.CommitOutcome.ROLLED_BACK)return false;
        for(Throwable cause=failure;cause!=null;cause=cause.getCause())if(cause instanceof ResearchStoreR30R1.StaleResearch||cause instanceof DependencyR31&&String.valueOf(cause.getMessage()).contains("SOURCE_CHANGED_REPREPARE"))return true;
        return false;
    }
    private static void publish(DbStore store,JobR31 job,BooleanSupplier cancelled,BooleanSupplier work)throws Exception {
        for(;;){checkCancelled(work);
            try{
                ChangesR31 evidence=refreshCanonical(store,job,cancelled,work);SummaryR31 s=summarize(store,job,work);
                ChangesR31 changes=prepareChanges(store,job,s,evidence,cancelled,work);
                long fence=rebaseChanges(store,job,changes,work);final long cursor=job.cursor,sequence=job.chunk;
                ResearchWriteQueueR27.Batch b=new ResearchWriteQueueR27.Batch();
                b.guardBefore(db->{requireOwner(db,job,cursor,sequence);ResearchSourceJournalR31.requireFence(db,fence);try(Cursor count=db.rawQuery("SELECT row_count FROM r31_history_import_run WHERE job_id=?",new String[]{job.id})){if(!count.moveToFirst()||count.getLong(0)!=s.distinct)throw new IllegalStateException("HISTORY_MANIFEST_ROW_COUNT");}return 0;});
                b.add("UPDATE r31_history_import_run SET status='COMPLETE',manifest_hash=?,row_count=?,match_rows=?,conflict_rows=?,imported_rows=?,duplicate_rows=?,duplicate_conflicts=?,gaps=?,missing_issues=?,first_ordinal=?,last_ordinal=?,publish_seq=(SELECT COALESCE(MAX(publish_seq),0)+1 FROM r31_history_import_run),completed_at=?,lease_elapsed_ms=0 WHERE job_id=? AND owner_epoch=? AND fence=? AND cursor_line=? AND chunk_seq=? AND status='STAGING'",s.hash,s.rows,s.match,s.conflict,s.imported,s.duplicates,s.duplicateConflicts,s.gaps,s.missing,s.rows==0?null:s.first,s.rows==0?null:s.last,System.currentTimeMillis(),job.id,job.owner,job.fence,cursor,sequence);
                long rejected=s.conflict+s.duplicateConflicts;
                if(rejected>0){b.add("INSERT OR IGNORE INTO r27_fetch_reject(reason,count,last_issue,last_at) VALUES('HISTORY_IMPORT_CONFLICT',0,?,?)",s.lastConflict(),job.created);b.add("UPDATE r27_fetch_reject SET count=count+?,last_issue=?,last_at=? WHERE reason='HISTORY_IMPORT_CONFLICT'",rejected,s.lastConflict(),job.created);}
                if(changes.insertCount>0)ResearchSourceJournalR31.appendPublishedImport(b,"HISTORY:"+job.id+':'+s.hash+":INSERT",changes.members,"INSERT",changes.insertFirst,changes.insertLast,job.created);
                if(changes.replaceCount>0)ResearchSourceJournalR31.appendPublishedImport(b,"HISTORY:"+job.id+':'+s.hash+":REPLACE",changes.members,"REPLACE",changes.replaceFirst,changes.replaceLast,job.created);
                b.guardAfter(db->{try(Cursor complete=db.rawQuery("SELECT manifest_hash,status,row_count FROM r31_history_import_run WHERE job_id=?",new String[]{job.id})){if(!complete.moveToFirst()||!s.hash.equals(complete.getString(0))||!"COMPLETE".equals(complete.getString(1))||complete.getLong(2)!=s.rows)throw new IllegalStateException("HISTORY_PUBLICATION_IDENTITY");}return 0;});
                submit(store,job,b,"PUBLISH:"+changes.members+':'+fence,sequence+1,cancelled);job.complete=true;return;
            }catch(ResearchStoreR30R1.StaleResearch stale){/* Outside-writer preparation raced a real changed input. */}
            catch(RuntimeException failure){if(!rollbackRetry(failure))throw failure;}
        }
    }

    /** R30-B03-02 import governance, pure (host-testable): rows sorted by ordinal (any input order, across days),
     * illegal rows and bad issues dropped, duplicate issues kept once (a differing duplicate is a conflict), gaps counted. */
    public static final class Governed {
        public final List<ReplayDatasetR27.Row> rows = new java.util.ArrayList<>();
        public int invalid, duplicates, duplicateConflicts, gaps, missingIssues;
        public final List<String> conflictIssues = new java.util.ArrayList<>();
    }
    public static Governed govern(List<ReplayDatasetR27.Row> input) {
        Governed g = new Governed(); java.util.Map<String, ReplayDatasetR27.Row> byIssue = new java.util.HashMap<>();
        for (ReplayDatasetR27.Row r : input) {
            if (r == null || !ReplayDatasetR27.legal(r.numbers) || !TargetIssueResolver.isValid(r.issue)) { g.invalid++; continue; }
            ReplayDatasetR27.Row first = byIssue.get(r.issue);
            if (first == null) { byIssue.put(r.issue, r); continue; }
            g.duplicates++;
            if (!java.util.Arrays.equals(first.numbers, r.numbers)) { g.duplicateConflicts++; g.conflictIssues.add(r.issue); }
        }
        for (String bad : g.conflictIssues) byIssue.remove(bad); // a self-contradicting CSV issue is never imported
        g.rows.addAll(byIssue.values());
        java.util.Collections.sort(g.rows, (a, b) -> Long.compare(TargetIssueResolver.sequenceOrdinalSU1(a.issue), TargetIssueResolver.sequenceOrdinalSU1(b.issue)));
        for (int i = 1; i < g.rows.size(); i++) {
            long d = TargetIssueResolver.sequenceOrdinalSU1(g.rows.get(i).issue) - TargetIssueResolver.sequenceOrdinalSU1(g.rows.get(i - 1).issue);
            if (d > 1) { g.gaps++; g.missingIssues += (int) (d - 1); }
        }
        return g;
    }

    public static String importCsv(DbStore store, InputStream in) throws Exception {
        return importCsv(store,in,()->Thread.currentThread().isInterrupted());
    }
    private static void requireSchemas(DbStore store){
        if(!publicationSchemaReadyReadOnlyR31(store.readable())||!ResearchSourceJournalR31.readySchemaR31(store.readable()))throw new DependencyR31("SCHEMA_PENDING");
    }
    /** Explicit recovery UI selects an original persisted task; an active epoch still has to drain/expire. */
    public static String pendingJobIdR31(DbStore store){
        requireSchemas(store);
        try(Cursor c=store.readable().rawQuery("SELECT job_id FROM r31_history_import_run WHERE status='STAGING' ORDER BY created_at,job_id LIMIT 1",null)){return c.moveToFirst()?c.getString(0):null;}
    }
    public static String resumeSavedR31(DbStore store,String jobId,BooleanSupplier cancelled)throws Exception{
        if(jobId==null||cancelled==null)throw new IllegalArgumentException("HISTORY_RECOVERY_ID_REQUIRED");checkCancelled(cancelled);requireSchemas(store);
        JobR31 prior=job(store.readable(),jobId);if(prior==null)throw new DependencyR31("RECOVERY_INPUT_MISSING");
        if(!R31_IMPORT_ABI.equals(prior.abi)||!R31_ROLE.equals(prior.role)||!jobId.equals(StatsR27.sha256(R31_IMPORT_ABI+'|'+prior.source)))throw new IntegrityR31("RECOVERY_IDENTITY");
        if(prior.complete)return completedMessage(store.readable(),jobId);
        File file=privatePath(store,prior.path);if(!file.isFile())throw new DependencyR31("RECOVERY_SNAPSHOT_MISSING");if(file.length()!=prior.sourceBytes)throw new IntegrityR31("RECOVERY_SOURCE_SIZE");
        try(InputStream input=new FileInputStream(file)){return importCsvExpected(store,input,cancelled,prior.source);}
    }
    /** Caller owns its input stream. Cancellation stops work; permits close only after the real work exits. */
    public static String importCsv(DbStore store,InputStream in,BooleanSupplier cancelled)throws Exception {
        return importCsvExpected(store,in,cancelled,null);
    }
    private static String importCsvExpected(DbStore store,InputStream in,BooleanSupplier cancelled,String expectedSource)throws Exception {
        if(in==null||cancelled==null)throw new IllegalArgumentException("HISTORY_IMPORT_INPUT_REQUIRED");
        checkCancelled(cancelled);
        requireSchemas(store);
        // Bounded parser/rows/bind copies plus the common <=256KiB storage-ledger read/decode/write phases.
        MemoryBudgetR31.Admission admission=MemoryBudgetR31.processBudget().tryAcquire("HISTORY_PREPARED_WORK",MemoryBudgetR31.Kind.RESEARCH,2L<<20,cancelled);
        if(!admission.acquired())throw new MemoryBudgetR31.CapacityWait(admission.reason);
        try(MemoryBudgetR31.Lease memory=admission.lease;SourceR31 source=snapshot(store,in,cancelled)){
            if(expectedSource!=null&&!expectedSource.equals(source.hash))throw new IntegrityR31("RECOVERY_SOURCE_HASH");
            // Validate the immutable private snapshot before creating a durable reference to it.
            try(CsvR31 header=new CsvR31(new FileInputStream(privatePath(store,source.path)))){header.header(cancelled);}
            JobR31 job=claim(store,source,cancelled);
            if(job.complete)return completedMessage(store.readable(),job.id);
            final long[] renewed={android.os.SystemClock.elapsedRealtime()};
            BooleanSupplier work=()->{
                if(Thread.currentThread().isInterrupted()||cancelled.getAsBoolean())return true;
                long now=android.os.SystemClock.elapsedRealtime();if(now-renewed[0]>=5000){pulse(store,job);renewed[0]=now;}return false;
            };
            try{
                verifySource(store,job,work);
                try(CsvR31 csv=new CsvR31(new FileInputStream(privatePath(store,job.path)))){
                    csv.header(work);long line=0;
                    while(line<job.cursor){checkCancelled(work);if(csv.next(work)==null)throw new IllegalStateException("HISTORY_CURSOR_PAST_SOURCE");line++;}
                    boolean eof=false;
                    while(!eof){List<RowR31> rows=new ArrayList<>(CHUNK_ROWS_R31);long invalid=0,from=line;
                        for(int count=0;count<CHUNK_ROWS_R31;count++){
                            checkCancelled(work);String[] fields=csv.next(work);if(fields==null){eof=true;break;}line++;
                            if(fields[0].isEmpty()&&fields[1].isEmpty()&&fields[2].isEmpty())continue;
                            if(!"OK".equals(fields[3])){invalid++;continue;}
                            if(!"1".equals(fields[2]))continue; // Preserve the source format's explicit valid=1 role.
                            RowR31 row=parse(fields,line);if(row==null)invalid++;else rows.add(row);
                        }
                        if(line==from)break;
                        for(;;){try{chunk(store,job,rows,line,invalid,cancelled);break;}catch(RuntimeException failure){if(!rollbackRetry(failure))throw failure;checkCancelled(work);}}
                    }
                }
                publish(store,job,cancelled,work);return completedMessage(store.readable(),job.id);
            }finally{
                // A cancelled or failed worker has really left its parser/writer before surrendering this epoch.
                if(!job.complete&&!Thread.currentThread().isInterrupted()){
                    try{store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.RESEARCH_CONTROL,"R31_HISTORY_IMPORT_RELEASE").withDeadlineMonoMs(TimeSourceR811.monoNowMs()+200),db->{
                        db.execSQL("UPDATE r31_history_import_run SET lease_elapsed_ms=0 WHERE job_id=? AND owner_epoch=? AND fence=? AND status='STAGING'",new Object[]{job.id,job.owner,job.fence});return 0;
                    });}catch(RuntimeException releasePending){/* The bounded lease still expires; it is never mistaken for a completed manifest. */}
                }
            }
        }
    }

    static String join(int[] p) { StringBuilder s = new StringBuilder(); for (int i = 0; i < p.length; i++) { if (i > 0) s.append(','); s.append(p[i]); } return s.toString(); }

    /** Imported rows older than the oldest database draw (replay only). */
    public static void appendOlder(SQLiteDatabase ro, List<ReplayDatasetR27.Row> rows, String oldestDbIssue, int limit) {
        if(limit<=0||oldestDbIssue==null)return;
        final String relation;
        if(publicationSchemaReadyReadOnlyR31(ro))relation=publishedRowsSqlR31();
        else{
            try(Cursor present=ro.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='r27_history_import'",null)){if(!present.moveToFirst())return;}
            try(Cursor pending=ro.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name IN ('r31_history_import_run','r31_history_import_row') LIMIT 1",null)){if(pending.moveToFirst())throw new IllegalStateException("HISTORY_IMPORT_PUBLICATION_SCHEMA_INVALID");}
            relation="r27_history_import"; // Existing evidence remains readable before the additive startup migration.
        }
        try(Cursor c=ro.rawQuery("SELECT issue,ordinal,numbers FROM "+relation+" WHERE status='IMPORTED' AND issue<? ORDER BY issue DESC LIMIT "+Math.min(limit,10000),new String[]{oldestDbIssue})){
            while(c.moveToNext()){
                String issue=c.getString(0);String[] values=c.getString(2).split(",",-1);if(values.length!=10||!TargetIssueResolver.isValid(issue))throw new IllegalStateException("HISTORY_PUBLISHED_INPUT_INVALID");
                int[] numbers=new int[10];for(int n=0;n<10;n++)numbers[n]=Integer.parseInt(values[n]);
                if(!ReplayDatasetR27.legal(numbers)||c.getLong(1)!=TargetIssueResolver.sequenceOrdinalSU1(issue))throw new IllegalStateException("HISTORY_PUBLISHED_INPUT_INVALID");
                rows.add(new ReplayDatasetR27.Row(issue,c.getLong(1),numbers));
            }
        }
    }

    public static int retryQuarantined(DbStore store) {
        return store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R27_RETRY_QUARANTINED", db -> {
            int n = 0; try (Cursor c = db.rawQuery("SELECT count(*) FROM r26_task WHERE status='QUARANTINED'", null)) { if (c.moveToFirst()) n = c.getInt(0); }
            db.execSQL("UPDATE r26_task SET status='READY',wait_kind='',error_count=0,error_window_start=0,next_at=0 WHERE status='QUARANTINED'"); return n; });
    }
}
