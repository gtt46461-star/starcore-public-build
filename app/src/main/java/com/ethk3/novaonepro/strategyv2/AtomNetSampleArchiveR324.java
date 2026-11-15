package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.DatabaseCoordinator;
import java.io.*;
import java.util.*;

/** Complete, immutable sample segments move from SQLite to the existing CAS.
 * Readers preserve the exact dataset, index, feature bits, labels and digest.
 * Retirement releases reusable SQLite pages, not claimed physical file bytes.
 */
public final class AtomNetSampleArchiveR324 {
    private AtomNetSampleArchiveR324() {}
    public static final int MAGIC=0x324A5301,MAX_ROWS=120,MAX_FEATURE_BYTES=32768,MAX_BYTES=5<<20;
    public static final String TABLE="r324_atomnet_sample_archive";
    public static final String ATTEMPTS="r325_atomnet_archive_attempt";
    static final long FIRST_RETRY_MS=60_000L,MAX_RETRY_MS=1_800_000L;
    static final String[] SUPPORTED_OPERATORS={"R31_ATOMNET_PUBLICATION_1","R32_ATOMNET_COMPLETE_PUBLICATION_2"};
    public static final String SAMPLE_DELETE_GUARD="CREATE TRIGGER IF NOT EXISTS r31_atomnet_stage_published_no_delete BEFORE DELETE ON r31_atomnet_sample_stage WHEN NOT EXISTS(SELECT 1 FROM r324_atomnet_sample_retire_permit t JOIN r324_atomnet_sample_archive a ON a.publication_id=t.publication_id AND a.payload_hash=t.archive_hash JOIN r31_atomnet_publication p ON p.publication_id=a.publication_id WHERE t.publication_id=OLD.publication_id AND p.status='PUBLISHED' AND p.sample_digest=a.sample_digest AND OLD.sample_index>=a.first_sample AND OLD.sample_index<a.first_sample+a.row_count) BEGIN SELECT RAISE(ABORT,'R31_ATOM_PUBLISHED_SAMPLE_PROTECTED'); END";
    public static final String[] DDL={
        "CREATE TABLE IF NOT EXISTS r325_atomnet_archive_attempt(publication_id TEXT PRIMARY KEY,attempts INTEGER NOT NULL CHECK(attempts>=1),last_attempt_at INTEGER NOT NULL CHECK(last_attempt_at>=0),next_attempt_at INTEGER NOT NULL CHECK(next_attempt_at>=0),status TEXT NOT NULL CHECK(status IN ('CLAIMED','FAULT_PRESERVED','ARCHIVED')),failure_class TEXT NOT NULL,failure_code TEXT NOT NULL,last_success_at INTEGER NOT NULL CHECK(last_success_at>=0))",
        "CREATE INDEX IF NOT EXISTS idx_r325_atom_archive_due ON r325_atomnet_archive_attempt(next_attempt_at,publication_id)",
        "CREATE TABLE IF NOT EXISTS r324_atomnet_sample_archive(publication_id TEXT PRIMARY KEY,archive_ref BLOB NOT NULL,payload_hash TEXT NOT NULL,row_count INTEGER NOT NULL CHECK(row_count BETWEEN 1 AND 120),first_sample INTEGER NOT NULL CHECK(first_sample>=0),sample_digest TEXT NOT NULL,archived_at INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS r324_atomnet_sample_retire_permit(publication_id TEXT PRIMARY KEY,archive_hash TEXT NOT NULL)",
        "CREATE TRIGGER IF NOT EXISTS r324_atomnet_archive_immutable_update BEFORE UPDATE ON r324_atomnet_sample_archive BEGIN SELECT RAISE(ABORT,'R324_ATOM_ARCHIVE_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS r324_atomnet_archive_immutable_delete BEFORE DELETE ON r324_atomnet_sample_archive BEGIN SELECT RAISE(ABORT,'R324_ATOM_ARCHIVE_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS r324_atomnet_archived_sample_no_insert BEFORE INSERT ON r31_atomnet_sample_stage WHEN EXISTS(SELECT 1 FROM r324_atomnet_sample_archive WHERE publication_id=NEW.publication_id) BEGIN SELECT RAISE(ABORT,'R324_ATOM_SAMPLE_ALREADY_ARCHIVED'); END"
    };
    public static String[][] schemaObjects(){return new String[][]{{"table",ATTEMPTS},{"index","idx_r325_atom_archive_due"},{"table",TABLE},{"table","r324_atomnet_sample_retire_permit"},{"trigger","r324_atomnet_archive_immutable_update"},{"trigger","r324_atomnet_archive_immutable_delete"},{"trigger","r324_atomnet_archived_sample_no_insert"}};}
    public static void requireReady(SQLiteDatabase db){DatabaseCoordinator.requireSchemaObjectsR31(db,"R324_ATOM_SAMPLE_ARCHIVE",new String[][]{{ATTEMPTS,"publication_id,attempts,last_attempt_at,next_attempt_at,status,failure_class,failure_code,last_success_at"},{TABLE,"publication_id,archive_ref,payload_hash,row_count,first_sample,sample_digest,archived_at"},{"r324_atomnet_sample_retire_permit","publication_id,archive_hash"}},new String[][]{{"index","idx_r325_atom_archive_due"},{"trigger","r324_atomnet_archive_immutable_update"},{"trigger","r324_atomnet_archive_immutable_delete"},{"trigger","r324_atomnet_archived_sample_no_insert"}});}
    static final class Row {
        final int index,position,hit,number;final String issue,hash;final byte[] features;
        Row(int index,String issue,int position,byte[] features,int hit,int number,String hash){this.index=index;this.issue=issue;this.position=position;this.features=features;this.hit=hit;this.number=number;this.hash=hash;}
        void validate(String dataset)throws IOException {
            if(index<0||issue==null||issue.length()>512||position!=Math.floorMod(index,10)+1||features==null||features.length<8||features.length>MAX_FEATURE_BYTES||features.length%8!=0||hit<0||hit>1||number<0||number>9||hash==null||!hash.equals(AtomNetPublicationR31.sampleHash(new Object[]{dataset,index,issue,position,features,hit,number})))throw new IOException("R324_SAMPLE_ARCHIVE_ROW_IDENTITY");
        }
    }
    static final class Segment {
        final String publication,dataset,operator,digest,state;final int first;final List<Row> rows;
        Segment(String publication,String dataset,String operator,String digest,String state,int first,List<Row> rows){this.publication=publication;this.dataset=dataset;this.operator=operator;this.digest=digest;this.state=state;this.first=first;this.rows=Collections.unmodifiableList(new ArrayList<>(rows));}
        void validate()throws IOException {
            if(publication==null||!publication.matches("[0-9a-f]{64}")||dataset==null||!dataset.matches("[0-9a-f]{64}")||state==null||!state.matches("[0-9a-f]{64}")||operator==null||!Arrays.asList(SUPPORTED_OPERATORS).contains(operator)||first<0||rows.isEmpty()||rows.size()>MAX_ROWS)throw new IOException("R324_SAMPLE_ARCHIVE_IDENTITY");
            StringBuilder hashes=new StringBuilder(operator);for(int i=0;i<rows.size();i++){Row row=rows.get(i);row.validate(dataset);if(row.index!=(long)first+i)throw new IOException("R324_SAMPLE_ARCHIVE_CONTIGUITY");hashes.append('|').append(row.hash);}
            if(!StatsR27.sha256(hashes.toString()).equals(digest))throw new IOException("R324_SAMPLE_ARCHIVE_DIGEST");
        }
    }
    static byte[] encode(Segment segment)throws IOException {
        segment.validate();ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(DataOutputStream out=new DataOutputStream(bytes)){out.writeInt(MAGIC);out.writeUTF(segment.publication);out.writeUTF(segment.dataset);out.writeUTF(segment.operator);out.writeUTF(segment.digest);out.writeUTF(segment.state);out.writeInt(segment.first);out.writeInt(segment.rows.size());
            for(Row row:segment.rows){out.writeInt(row.index);out.writeUTF(row.issue);out.writeInt(row.position);out.writeInt(row.features.length);out.write(row.features);out.writeInt(row.hit);out.writeInt(row.number);out.writeUTF(row.hash);}}
        if(bytes.size()>MAX_BYTES)throw new IOException("R324_SAMPLE_ARCHIVE_SIZE");return bytes.toByteArray();
    }
    static Segment decode(byte[] bytes)throws IOException {
        if(bytes==null||bytes.length<1||bytes.length>MAX_BYTES)throw new IOException("R324_SAMPLE_ARCHIVE_SIZE");
        try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes))){if(in.readInt()!=MAGIC)throw new IOException("R324_SAMPLE_ARCHIVE_VERSION");String id=in.readUTF(),dataset=in.readUTF(),operator=in.readUTF(),digest=in.readUTF(),state=in.readUTF();int first=in.readInt(),count=in.readInt();if(count<1||count>MAX_ROWS)throw new IOException("R324_SAMPLE_ARCHIVE_COUNT");List<Row> rows=new ArrayList<>();
            for(int i=0;i<count;i++){int index=in.readInt();String issue=in.readUTF();int position=in.readInt(),size=in.readInt();if(size<8||size>MAX_FEATURE_BYTES||size%8!=0)throw new IOException("R324_SAMPLE_ARCHIVE_FEATURE_SIZE");byte[] features=new byte[size];in.readFully(features);rows.add(new Row(index,issue,position,features,in.readInt(),in.readInt(),in.readUTF()));}
            if(in.read()!=-1)throw new IOException("R324_SAMPLE_ARCHIVE_TRAILING_BYTES");Segment result=new Segment(id,dataset,operator,digest,state,first,rows);result.validate();return result;}
    }
    /** R32.5: archiving has its own pool task and durable per-object retry.
     * A damaged old publication never blocks restoring/training the live learner.
     * Only the exact source/codec errors below are isolated. Unknown errors,
     * cancellation, schema/storage errors and uncertain commits still propagate. */
    interface ArchiveFiles { CheckpointArtifactsR26 open() throws IOException; }
    interface ArchiveWriter { void submit(String operation,ResearchWriteQueueR27.Batch batch,String identity)throws Exception; }
    static ResearchPoolR27.Outcome maintenance(ResearchPoolR27.Worker worker)throws Exception {
        return maintenance(worker.ro,()->CheckpointArtifactsR26.forStore(worker.store()),
                (operation,batch,identity)->RuntimeR27.submit(worker,operation,batch,false,identity),
                System.currentTimeMillis());
    }
    private static final class Candidate {
        final String id;final long attempts,last;
        Candidate(String id,long attempts,long last){this.id=id;this.attempts=attempts;this.last=last;}
    }
    private static Candidate next(SQLiteDatabase db,long now){
        // Enumerate the hot sample identities, not the unbounded publication history.
        // DISTINCT preserves this as a bounded current-working-set subquery.
        // A due retry orders by its persisted deadline, while new work orders by
        // its publication time. Old failed IDs cannot repeatedly monopolize LIMIT 1.
        // A backwards wall clock permits one fresh bounded claim, not an endless wait.
        String query="SELECT p.publication_id,coalesce(a.attempts,0),coalesce(a.last_attempt_at,0) FROM (SELECT DISTINCT publication_id FROM r31_atomnet_sample_stage) hot JOIN r31_atomnet_publication p ON p.publication_id=hot.publication_id LEFT JOIN "+ATTEMPTS+" a ON a.publication_id=p.publication_id WHERE p.status='PUBLISHED' AND p.operator_version IN (?,?) AND p.sample_count>0 AND NOT EXISTS(SELECT 1 FROM r324_atomnet_sample_archive a WHERE a.publication_id=p.publication_id) AND NOT EXISTS(SELECT 1 FROM r29_model_checkpoint c WHERE c.state_hash=p.state_hash AND c.owner=p.owner) AND (a.publication_id IS NULL OR a.next_attempt_at<=CAST(? AS INTEGER) OR a.last_attempt_at>CAST(? AS INTEGER)) ORDER BY coalesce(a.next_attempt_at,p.published_at,p.created_at),p.publication_id LIMIT 1";
        try(Cursor c=db.rawQuery(query,new String[]{SUPPORTED_OPERATORS[0],SUPPORTED_OPERATORS[1],Long.toString(now),Long.toString(now)})){
            if(!c.moveToFirst())return null;String id=c.getString(0);long attempts=c.getLong(1),last=c.getLong(2);
            if(id==null||!id.matches("[0-9a-f]{64}")||attempts<0||last<0)throw new IllegalStateException("R325_ARCHIVE_ATTEMPT_IDENTITY");
            return new Candidate(id,attempts,last);
        }
    }
    static long retryDelay(long attempt){int shift=(int)Math.min(5,Math.max(0,attempt-1));return Math.min(MAX_RETRY_MS,FIRST_RETRY_MS<<shift);}
    private static long nextDeadline(long now,long attempt){long delay=retryDelay(attempt);return now>Long.MAX_VALUE-delay?Long.MAX_VALUE:now+delay;}
    private static boolean attemptMatches(SQLiteDatabase db,String id,long attempts,long at,String status){
        try(Cursor c=db.rawQuery("SELECT attempts,last_attempt_at,status FROM "+ATTEMPTS+" WHERE publication_id=?",new String[]{id})){
            if(!c.moveToFirst())return attempts==0&&status==null;
            return c.getLong(0)==attempts&&c.getLong(1)==at&&(status==null||status.equals(c.getString(2)));
        }
    }
    private static boolean eligible(SQLiteDatabase db,String id){
        try(Cursor c=db.rawQuery("SELECT 1 FROM r31_atomnet_publication p WHERE p.publication_id=? AND p.status='PUBLISHED' AND p.operator_version IN (?,?) AND p.sample_count>0 AND NOT EXISTS(SELECT 1 FROM r324_atomnet_sample_archive a WHERE a.publication_id=p.publication_id) AND NOT EXISTS(SELECT 1 FROM r29_model_checkpoint c WHERE c.state_hash=p.state_hash AND c.owner=p.owner)",new String[]{id,SUPPORTED_OPERATORS[0],SUPPORTED_OPERATORS[1]})){return c.moveToFirst();}
    }
    private static long archivedCount(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT count(*) FROM r324_atomnet_sample_archive",null)){if(!c.moveToFirst())throw new IllegalStateException("R325_ARCHIVE_COUNT_MISSING");return c.getLong(0);}}
    static boolean localObjectFailure(Exception error){
        if(!(error instanceof IOException)||error instanceof InterruptedIOException||Thread.currentThread().isInterrupted())return false;
        FailureDispositionR31.Decision disposition=FailureDispositionR31.classify(error);
        if(disposition.disposition==FailureDispositionR31.Disposition.FATAL||disposition.disposition==FailureDispositionR31.Disposition.CANCELLED||disposition.disposition==FailureDispositionR31.Disposition.STALE_OWNER||disposition.requiresCommitResolution||disposition.alreadyCommitted)return false;
        String code=error.getMessage();if(code==null)return false;
        return Arrays.asList("R324_SAMPLE_ARCHIVE_RESERVATION","R324_SAMPLE_ARCHIVE_SOURCE_COUNT","R324_SAMPLE_ARCHIVE_SOURCE_INCOMPLETE","R324_SAMPLE_ARCHIVE_SOURCE_MISSING","R324_SAMPLE_ARCHIVE_ROW_IDENTITY","R324_SAMPLE_ARCHIVE_IDENTITY","R324_SAMPLE_ARCHIVE_CONTIGUITY","R324_SAMPLE_ARCHIVE_DIGEST","R324_SAMPLE_ARCHIVE_SIZE").contains(code);
    }
    static ResearchPoolR27.Outcome maintenance(SQLiteDatabase read,ArchiveFiles files,ArchiveWriter writer,long now)throws Exception {
        if(now<0)throw new IllegalArgumentException("R325_ARCHIVE_CLOCK");requireReady(read);
        final Candidate candidate=next(read,now);
        if(candidate==null)return ResearchPoolR27.Outcome.idle("NO_NEW_EVENT:ARCHIVE_WAITING_DUE_OR_NO_OLD_SEGMENT",archivedCount(read),-1);
        final long attempt=Math.min(Long.MAX_VALUE-1,candidate.attempts)+1;
        ResearchWriteQueueR27.Batch claim=new ResearchWriteQueueR27.Batch();
        claim.guardBefore(db->attemptMatches(db,candidate.id,candidate.attempts,candidate.last,null)&&eligible(db,candidate.id)?0:-1);
        claim.add("INSERT OR REPLACE INTO "+ATTEMPTS+" VALUES(?,?,?,?,'CLAIMED','','',0)",candidate.id,attempt,now,nextDeadline(now,attempt));
        writer.submit("ATOMNET_R325_ARCHIVE_ATTEMPT",claim,candidate.id+"|ATTEMPT|"+attempt+"|"+now);
        // Persist the deadline before decoding/IO. Process death and unexpected
        // exceptions leave this object delayed while unrelated due objects advance.
        try{
            archive(read,files,writer,candidate.id,attempt,now);
            return ResearchPoolR27.Outcome.progress(1,archivedCount(read));
        }catch(Exception failure){
            if(!localObjectFailure(failure))throw failure;
            final String code=failure.getMessage(),kind=failure.getClass().getName();
            ResearchWriteQueueR27.Batch note=new ResearchWriteQueueR27.Batch();
            note.guardBefore(db->attemptMatches(db,candidate.id,attempt,now,"CLAIMED")?0:-1);
            note.add("UPDATE "+ATTEMPTS+" SET status='FAULT_PRESERVED',failure_class=?,failure_code=? WHERE publication_id=? AND attempts=? AND last_attempt_at=?",kind,code,candidate.id,attempt,now);
            note.guardAfter(db->attemptMatches(db,candidate.id,attempt,now,"FAULT_PRESERVED")?0:-1);
            writer.submit("ATOMNET_R325_ARCHIVE_OBJECT_FAILURE",note,candidate.id+"|PRESERVED|"+attempt+"|"+now);
            return ResearchPoolR27.Outcome.idle("DEPENDENCY(ARCHIVE_OBJECT_PRESERVED:"+candidate.id+":"+code+")",archivedCount(read),-1);
        }
    }
    private static void archive(SQLiteDatabase read,ArchiveFiles files,ArchiveWriter writer,String id,long attempt,long now)throws Exception {
        long reserved;
        try(Cursor c=read.rawQuery("SELECT sample_bytes FROM r31_atomnet_publication WHERE publication_id=?",new String[]{id})){if(!c.moveToFirst()||c.getLong(0)<1||c.getLong(0)>MAX_BYTES)throw new IOException("R324_SAMPLE_ARCHIVE_RESERVATION");reserved=c.getLong(0);}
        try(MemoryBudgetR31.Lease memory=CheckpointArtifactsR26.readReservationR31("sample-archive",5L*reserved+65536)){
        final Segment segment=source(read,id);final byte[] bytes=encode(segment);final String payload=StatsR27.sha256(bytes);
        try(CheckpointArtifactsR26.Prepared file=files.open().prepareLease(bytes)){
            byte[] original=CheckpointArtifactsR26.resolveSerializedR31(read,file.reference());if(!Arrays.equals(bytes,original))throw new IOException("R324_SAMPLE_ARCHIVE_READBACK");decode(original);
            final byte[] reference=file.reference();ResearchWriteQueueR27.Batch batch=new ResearchWriteQueueR27.Batch();
            batch.guardBefore(db->{requireReady(db);if(!attemptMatches(db,id,attempt,now,"CLAIMED"))return -1;
                try(Cursor archived=db.rawQuery("SELECT payload_hash FROM r324_atomnet_sample_archive WHERE publication_id=?",new String[]{segment.publication})){if(archived.moveToFirst()){if(!payload.equals(archived.getString(0)))throw new IllegalStateException("R324_SAMPLE_ARCHIVE_CONFLICT");return -1;}}
                try(Cursor parent=db.rawQuery("SELECT p.state_hash,p.sample_digest,p.first_sample,p.sample_count,(SELECT count(*) FROM r31_atomnet_sample_stage s WHERE s.publication_id=p.publication_id),(SELECT count(*) FROM r29_model_checkpoint c WHERE c.state_hash=p.state_hash AND c.owner=p.owner),p.dataset_hash,p.operator_version FROM r31_atomnet_publication p WHERE p.publication_id=? AND p.status='PUBLISHED'",new String[]{segment.publication})){if(!parent.moveToFirst()||!segment.state.equals(parent.getString(0))||!segment.digest.equals(parent.getString(1))||segment.first!=parent.getInt(2)||segment.rows.size()!=parent.getInt(3)||segment.rows.size()!=parent.getInt(4)||parent.getInt(5)!=0||!segment.dataset.equals(parent.getString(6))||!segment.operator.equals(parent.getString(7)))return -1;}return 0;});
            batch.add("INSERT INTO r324_atomnet_sample_archive VALUES(?,?,?,?,?,?,?)",segment.publication,reference,payload,segment.rows.size(),segment.first,segment.digest,now);
            batch.add("INSERT INTO r324_atomnet_sample_retire_permit VALUES(?,?)",segment.publication,payload);
            batch.add("DELETE FROM r31_atomnet_sample_stage WHERE publication_id=?",segment.publication);
            batch.add("DELETE FROM r324_atomnet_sample_retire_permit WHERE publication_id=?",segment.publication);
            batch.add("UPDATE "+ATTEMPTS+" SET status='ARCHIVED',failure_class='',failure_code='',last_success_at=? WHERE publication_id=? AND attempts=? AND last_attempt_at=?",now,segment.publication,attempt,now);
            final RuntimeCoordinatorR26.Claim owner=RuntimeCoordinatorR26.currentClaimR30();
            batch.guardAfter(db->{try(Cursor c=db.rawQuery("SELECT (SELECT count(*) FROM r31_atomnet_sample_stage WHERE publication_id=?),(SELECT count(*) FROM r324_atomnet_sample_retire_permit WHERE publication_id=?)",new String[]{segment.publication,segment.publication})){if(!c.moveToFirst()||c.getInt(0)!=0||c.getInt(1)!=0||!attemptMatches(db,id,attempt,now,"ARCHIVED"))return -1;}
                if(owner!=null&&ResearchStoreR30R1.ready(db)){long count=archivedCount(db);ResearchStoreR30R1.commitProgress(db,owner.task,owner.owner,owner.fence,ResearchStoreR30R1.calendar(db),new ResearchStoreR30R1.Facts(count,count,null,null,null,payload));}return 0;});
            writer.submit("ATOMNET_R324_ARCHIVE",batch,segment.publication+"|ARCHIVE|"+payload+"|ATTEMPT|"+attempt);
        }
        }
    }
    public static Map<String,Object> maintenanceHealth(SQLiteDatabase db){
        requireReady(db);Map<String,Object> out=new LinkedHashMap<>();out.put("task","r27-atomnet-archive");out.put("learnerPrerequisite",false);out.put("maxRetryMs",MAX_RETRY_MS);
        List<Map<String,Object>> totals=new ArrayList<>();try(Cursor c=db.rawQuery("SELECT status,count(*),max(attempts) FROM "+ATTEMPTS+" GROUP BY status ORDER BY status",null)){while(c.moveToNext()){Map<String,Object> row=new LinkedHashMap<>();row.put("status",c.getString(0));row.put("objects",c.getLong(1));row.put("maxAttempts",c.getLong(2));totals.add(row);}}out.put("statusCounts",totals);
        List<Map<String,Object>> faults=new ArrayList<>();try(Cursor c=db.rawQuery("SELECT publication_id,attempts,last_attempt_at,next_attempt_at,failure_class,failure_code FROM "+ATTEMPTS+" WHERE status='FAULT_PRESERVED' ORDER BY next_attempt_at,publication_id LIMIT 16",null)){while(c.moveToNext()){Map<String,Object> row=new LinkedHashMap<>();row.put("publicationId",c.getString(0));row.put("attempts",c.getLong(1));row.put("lastAttemptAt",c.getLong(2));row.put("nextAttemptAt",c.getLong(3));row.put("failureClass",c.getString(4));row.put("failureCode",c.getString(5));row.put("originalsPreserved",true);faults.add(row);}}out.put("boundedFailureExamples",faults);out.put("failureExampleLimit",16);return out;
    }
    private static Segment source(SQLiteDatabase db,String id)throws IOException {
        String dataset,operator,digest,state;int first,count;
        try(Cursor c=db.rawQuery("SELECT dataset_hash,operator_version,sample_digest,state_hash,first_sample,sample_count FROM r31_atomnet_publication WHERE publication_id=? AND status='PUBLISHED'",new String[]{id})){if(!c.moveToFirst())throw new IOException("R324_SAMPLE_ARCHIVE_SOURCE_MISSING");dataset=c.getString(0);operator=c.getString(1);digest=c.getString(2);state=c.getString(3);first=c.getInt(4);count=c.getInt(5);}
        if(count<1||count>MAX_ROWS)throw new IOException("R324_SAMPLE_ARCHIVE_SOURCE_COUNT");List<Row> rows=new ArrayList<>();
        try(Cursor c=db.rawQuery("SELECT sample_index,issue,position,CASE WHEN length(features) BETWEEN 8 AND 32768 THEN features ELSE NULL END,hit,number_class,payload_hash FROM r31_atomnet_sample_stage WHERE publication_id=? ORDER BY sample_index LIMIT 121",new String[]{id})){while(c.moveToNext()){if(rows.size()>=count)throw new IOException("R324_SAMPLE_ARCHIVE_SOURCE_COUNT");rows.add(new Row(c.getInt(0),c.getString(1),c.getInt(2),c.getBlob(3),c.getInt(4),c.getInt(5),c.getString(6)));}}
        if(rows.size()!=count)throw new IOException("R324_SAMPLE_ARCHIVE_SOURCE_INCOMPLETE");Segment result=new Segment(id,dataset,operator,digest,state,first,rows);result.validate();return result;
    }
    interface Consumer {void accept(Row row);}
    /** All matching segments participate, even when a hot copy was already
     * read, so conflicting evidence cannot be hidden by a cache hit. */
    static void read(SQLiteDatabase db,String dataset,Set<Integer> requested,Consumer consumer){
        if(requested.isEmpty())return;requireReady(db);int low=Collections.min(requested),high=Collections.max(requested),segments=0;
        try(Cursor c=db.rawQuery("SELECT a.publication_id,a.archive_ref,a.payload_hash,a.row_count,a.first_sample,a.sample_digest,p.state_hash,p.operator_version,p.sample_bytes FROM r324_atomnet_sample_archive a JOIN r31_atomnet_publication p ON p.publication_id=a.publication_id WHERE p.dataset_hash=? AND p.status='PUBLISHED' AND a.first_sample<=? AND a.first_sample+a.row_count>CAST(? AS INTEGER) ORDER BY a.first_sample,a.publication_id",new String[]{dataset,Integer.toString(high),Integer.toString(low)})){
            while(c.moveToNext()){
                // A wide prefetch can span many intervals; skip non-requested intervals
                // before loading bytes. At most 128 overlapping sources are admitted.
                int first=c.getInt(4),count=c.getInt(3);boolean needed=false;for(int index:requested)if(index>=first&&(long)index<((long)first+count)){needed=true;break;}if(!needed)continue;if(++segments>128)throw new IllegalStateException("R324_SAMPLE_ARCHIVE_OVERLAPPING_SOURCE_BOUND");
                long reserved=c.getLong(8);if(reserved<1||reserved>MAX_BYTES)throw new IOException("R324_SAMPLE_ARCHIVE_RESERVATION");
                try(MemoryBudgetR31.Lease memory=CheckpointArtifactsR26.readReservationR31("sample-archive-read",4L*reserved+65536)){
                byte[] bytes=CheckpointArtifactsR26.resolveSerializedR31(db,c.getBlob(1));if(bytes.length>reserved+65536||!StatsR27.sha256(bytes).equals(c.getString(2)))throw new IOException("R324_SAMPLE_ARCHIVE_CHECKSUM_OR_BOUND");Segment segment=decode(bytes);
                if(!segment.publication.equals(c.getString(0))||!segment.dataset.equals(dataset)||!segment.state.equals(c.getString(6))||!segment.operator.equals(c.getString(7))||segment.first!=first||segment.rows.size()!=count||!segment.digest.equals(c.getString(5)))throw new IOException("R324_SAMPLE_ARCHIVE_PUBLICATION_BINDING");
                for(Row row:segment.rows)if(requested.contains(row.index))consumer.accept(row);
                }
            }
        }catch(IOException invalid){throw new IllegalStateException("R324_SAMPLE_ARCHIVE_READ_FAILED",invalid);}
    }
}
