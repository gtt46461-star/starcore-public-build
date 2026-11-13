package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.io.*;
import java.util.*;

/** First production reference-only cutover. The exact reader/producer set is
 * declared only for decision mismatches. All other keep families remain under
 * their existing owner until their own complete contracts are implemented. */
public final class KeepMismatchReferenceR31 {
    private KeepMismatchReferenceR31(){}
    public static final String SOURCE="r30_decision_mismatch",ARCHIVE="r30_keep_"+SOURCE,PREFIX="R31_REFERENCE_ONLY:";
    public static final String COLUMNS="issue,decision_key,original_allow,explain_allow,direction,original_reason,first_blocker,recorded_at";
    public static final String READER_CONTRACT="DecisionExplanationStoreR30.report+ReportSnapshotR30.globalCounters:UNION_ACTIVE_AND_CAS_BY_ISSUE_DECISION_DIRECTION_V1;complete-original-reader:KeepMigrationStoreR31.readOriginal";
    public static final String PRODUCER_CONTRACT="KeepMismatchReferenceR31.step:64KiB_CAS_PREPARE_OUTSIDE_WRITER;EXACT_OLD_ROW_PERMIT;KEEP_5000_ISSUE_TAIL;NO_AGE_DELETE;ORIGINAL_CAS_RETAINED";
    public static final String[] DDL={
        "CREATE TABLE IF NOT EXISTS r31_keep_cutover(source_table TEXT PRIMARY KEY,mode TEXT NOT NULL CHECK(mode IN ('GUARDED_PREPARED','REFERENCE_ONLY','CAS_DUPLICATE_RETIREMENT','REFERENCE_ONLY_GAP')),original_schema_hash TEXT NOT NULL,checkpoint_revision INTEGER NOT NULL,authority_hash TEXT NOT NULL,reader_contract_hash TEXT NOT NULL,producer_contract_hash TEXT NOT NULL,updated_at INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS r31_keep_retire_permit(archive_table TEXT NOT NULL,row_id INTEGER NOT NULL,row_hash TEXT NOT NULL,PRIMARY KEY(archive_table,row_id))",
        "CREATE TABLE IF NOT EXISTS r31_keep_mismatch_original(archive_id INTEGER PRIMARY KEY,row_hash TEXT NOT NULL,issue TEXT NOT NULL,decision_key TEXT NOT NULL,direction TEXT NOT NULL,archived_at INTEGER NOT NULL)",
        "CREATE INDEX IF NOT EXISTS idx_r31_keep_mismatch_identity ON r31_keep_mismatch_original(issue,decision_key,direction)",
        "CREATE TABLE IF NOT EXISTS r31_keep_mismatch_permit(source_rowid INTEGER PRIMARY KEY,archive_id INTEGER NOT NULL,row_hash TEXT NOT NULL,issue TEXT NOT NULL,decision_key TEXT NOT NULL,original_allow INTEGER NOT NULL,explain_allow INTEGER NOT NULL,direction TEXT NOT NULL,original_reason TEXT NOT NULL,first_blocker TEXT,recorded_at INTEGER NOT NULL)"
    };
    private static String q(String value){return ResearchRetentionStoreR30.q(value);}
    private static String lit(String value){return ResearchRetentionStoreR30.lit(value);}
    private static boolean table(SQLiteDatabase db,String name){return ResearchRetentionStoreR30.exists(db,name);}
    public static boolean ready(SQLiteDatabase db){return table(db,"r31_keep_cutover")&&table(db,"r31_keep_retire_permit")&&table(db,"r31_keep_mismatch_original")&&table(db,"r31_keep_mismatch_permit");}
    public static String mode(SQLiteDatabase db){if(!ready(db))return "LEGACY_PROTECTED_COPY";try(Cursor c=db.rawQuery("SELECT mode FROM r31_keep_cutover WHERE source_table=?",new String[]{SOURCE})){return c.moveToFirst()?c.getString(0):"LEGACY_PROTECTED_COPY";}}
    private static void requireSourceContract(SQLiteDatabase db){
        String expected=DecisionExplanationStoreR30.DDL[1];String header="CREATE TABLE IF NOT EXISTS ";expected=expected.startsWith(header)?"CREATE TABLE "+expected.substring(header.length()):expected;
        try(Cursor c=db.rawQuery("SELECT sql FROM sqlite_master WHERE type='table' AND name=?",new String[]{SOURCE})){if(!c.moveToFirst()||!expected.equals(c.getString(0).trim()))throw new DatabaseCoordinator.SchemaPendingR31("R31_KEEP_MISMATCH","DECLARED_SOURCE_COLUMNS_OR_TYPES_CHANGED");}
    }
    public static String effectiveSchemaHash(SQLiteDatabase db,String source,String original){return SOURCE.equals(source)&&"REFERENCE_ONLY".equals(mode(db))?PREFIX+original:GapReferenceR31.effectiveSchemaHash(db,source,original);}
    private static String enabled(){return "EXISTS(SELECT 1 FROM r31_keep_cutover WHERE source_table='"+SOURCE+"' AND mode='REFERENCE_ONLY')";}
    private static String originalSchemaHash(){List<List<String>> columns=new ArrayList<>();String[] types={"TEXT","TEXT","INTEGER","INTEGER","TEXT","TEXT","TEXT","INTEGER"},names=COLUMNS.split(",");for(int i=0;i<names.length;i++)columns.add(Arrays.asList(names[i],types[i],i==0?"1":i==1?"2":"0"));return StatsR27.sha256(JsonR27.write(columns));}
    private static String referenceRequired(){return "(EXISTS(SELECT 1 FROM r31_keep_cutover WHERE source_table='"+SOURCE+"') OR NOT EXISTS(SELECT 1 FROM r30_retention_archive_index WHERE source_table='"+SOURCE+"' AND schema_hash='"+originalSchemaHash()+"'))";}
    public static final String RETIRE_GUARD="r31_keep_mismatch_delete_permit";
    public static final String REFERENCE_GUARD="r31_keep_mismatch_reference_delete";
    public static final String INSERT_GUARD="r31_keep_mismatch_reference_insert";
    public static final String UPDATE_GUARD="r31_keep_mismatch_reference_update";
    private static String oldEqual(){StringJoiner same=new StringJoiner(" AND ");for(String col:COLUMNS.split(","))same.add("p."+q(col)+" IS OLD."+q(col));return same.toString();}
    public static String retireGuardDdl(){return "CREATE TRIGGER IF NOT EXISTS "+RETIRE_GUARD+" BEFORE DELETE ON "+ARCHIVE+" WHEN NOT ("+enabled()+" AND EXISTS(SELECT 1 FROM r31_keep_retire_permit p JOIN r31_keep_original o ON o.archive_table=p.archive_table AND o.row_id=p.row_id AND o.row_hash=p.row_hash WHERE p.archive_table='"+ARCHIVE+"' AND p.row_id=OLD.__r30_id AND o.state='MIGRATED_ORIGINAL_RETAINED')) BEGIN SELECT RAISE(ABORT,'R31_KEEP_ORIGINAL_DELETE_WITHOUT_CAS_PERMIT'); END";}
    public static String referenceGuardDdl(){return "CREATE TRIGGER IF NOT EXISTS "+REFERENCE_GUARD+" BEFORE DELETE ON "+SOURCE+" WHEN "+referenceRequired()+" BEGIN SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM r31_keep_mismatch_permit p JOIN r31_keep_original o ON o.archive_table='"+ARCHIVE+"' AND o.row_id=p.archive_id AND o.row_hash=p.row_hash AND o.state='PRODUCER_PREPARED' WHERE p.source_rowid=OLD.rowid AND "+oldEqual()+") THEN RAISE(ABORT,'R31_KEEP_REFERENCE_PERMIT_REQUIRED') END; INSERT INTO r31_keep_mismatch_original(archive_id,row_hash,issue,decision_key,direction,archived_at) SELECT p.archive_id,p.row_hash,OLD.issue,OLD.decision_key,OLD.direction,CAST((julianday('now')-2440587.5)*86400000 AS INTEGER) FROM r31_keep_mismatch_permit p WHERE p.source_rowid=OLD.rowid; UPDATE r31_keep_original SET state='MIGRATED_ORIGINAL_RETAINED',migrated_at=CAST((julianday('now')-2440587.5)*86400000 AS INTEGER) WHERE archive_table='"+ARCHIVE+"' AND row_id=(SELECT archive_id FROM r31_keep_mismatch_permit WHERE source_rowid=OLD.rowid); UPDATE r30_retention_archive_index SET retained_rows=retained_rows+1 WHERE source_table='"+SOURCE+"'; DELETE FROM r31_keep_mismatch_permit WHERE source_rowid=OLD.rowid; END";}
    public static String insertGuardDdl(){StringJoiner same=new StringJoiner(" AND ");for(String column:COLUMNS.split(","))same.add("prior."+q(column)+" IS NEW."+q(column));return "CREATE TRIGGER IF NOT EXISTS "+INSERT_GUARD+" BEFORE INSERT ON "+SOURCE+" WHEN "+referenceRequired()+" AND EXISTS(SELECT 1 FROM "+SOURCE+" WHERE issue=NEW.issue AND decision_key=NEW.decision_key) BEGIN SELECT CASE WHEN EXISTS(SELECT 1 FROM "+SOURCE+" prior WHERE prior.issue=NEW.issue AND prior.decision_key=NEW.decision_key AND "+same+") THEN RAISE(IGNORE) ELSE RAISE(ABORT,'R31_KEEP_MISMATCH_INSERT_CONFLICT') END; END";}
    public static String updateGuardDdl(){return "CREATE TRIGGER IF NOT EXISTS "+UPDATE_GUARD+" BEFORE UPDATE ON "+SOURCE+" WHEN "+referenceRequired()+" BEGIN SELECT RAISE(ABORT,'R31_KEEP_MISMATCH_APPEND_ONLY'); END";}
    /** Schema owner pre-installs these inactive guards while old guards still
     * protect everything; activation never creates an unprotected DDL window. */
    public static String[] cutoverDefinitionsR31(SQLiteDatabase db){return table(db,SOURCE)&&table(db,ARCHIVE)?new String[]{retireGuardDdl(),referenceGuardDdl(),updateGuardDdl(),insertGuardDdl()}:new String[0];}
    private static boolean hasObject(SQLiteDatabase db,String name){try(Cursor c=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='trigger' AND name=?",new String[]{name})){return c.moveToFirst();}}
    private static void exact(SQLiteDatabase db,String name,String ddl){ResearchRetentionStoreR30.requireGuardDefinitionsR31(db,new String[][]{{name,ddl}},true);}
    /** Add only the declared known attachments. Unknown objects still fail the
     * retention owner's exact archive/source definition checks. */
    static LinkedHashMap<String,String> expectedGuards(SQLiteDatabase db,String source,LinkedHashMap<String,String> legacy){
        if(!SOURCE.equals(source))return legacy;if(mode(db).equals("GUARDED_PREPARED")||mode(db).equals("REFERENCE_ONLY"))requireSourceContract(db);LinkedHashMap<String,String> result=new LinkedHashMap<>(legacy);
        if(hasObject(db,RETIRE_GUARD)){result.put(RETIRE_GUARD,retireGuardDdl());if(!hasObject(db,ARCHIVE+"_immutable_delete")&&(mode(db).equals("GUARDED_PREPARED")||mode(db).equals("REFERENCE_ONLY")))result.remove(ARCHIVE+"_immutable_delete");}
        if(hasObject(db,REFERENCE_GUARD))result.put(REFERENCE_GUARD,referenceGuardDdl());if(hasObject(db,UPDATE_GUARD))result.put(UPDATE_GUARD,updateGuardDdl());if(hasObject(db,INSERT_GUARD))result.put(INSERT_GUARD,insertGuardDdl());return result;
    }
    static boolean recognizedArchiveAttachment(SQLiteDatabase db,String archive,String name){if(!ARCHIVE.equals(archive)||!RETIRE_GUARD.equals(name))return false;exact(db,name,retireGuardDdl());return true;}
    /** Actual authority protocol proof. A legal typed FALLBACK is sufficient;
     * no active certificate or nonzero REAL head is required or fabricated. */
    static String authorityProof(SQLiteDatabase db){
        if(!MainlineChoiceStoreR31.ready(db)||!SchemaDomainCoordinatorR926.matches(db,SchemaDomainCoordinatorR926.r31MainlineChoiceContract())||!"TYPED_READ".equals(MainlineChoiceStoreR31.authorityMode(db))||!"MATCH".equals(MainlineChoiceStoreR31.compareLegacyProjection(db)))return null;
        MainlineChoiceStoreR31.Choice choice=MainlineChoiceStoreR31.current(db);if(choice==null)return null;
        try(Cursor c=db.rawQuery("SELECT evidence FROM r31_mainline_authority WHERE singleton=1",null)){if(!c.moveToFirst()||!c.getString(0).startsWith("DOUBLE_READ_MATCH:"))return null;return StatsR27.sha256(c.getString(0)+"|"+choice.choiceId+"|"+choice.sourceRevision);}
    }
    /** The only narrow destructive schema operation: remove this one exact old
     * immutable-delete trigger after the replacement guard has been installed.
     * The next invocation performs a DML-only atomic authority/copy cutover. */
    @SuppressWarnings("unchecked") public static boolean advanceCutoverWithinMigrationR31(SQLiteDatabase db){
        if(!db.inTransaction())throw new IllegalStateException("R31_KEEP_CUTOVER_MIGRATION_WRITER_REQUIRED");if(!ready(db)||!KeepMigrationStoreR31.ready(db)||!table(db,SOURCE)||!table(db,ARCHIVE)||"REFERENCE_ONLY".equals(mode(db)))return false;
        String authority=authorityProof(db);if(authority==null)return false;requireSourceContract(db);
        exact(db,RETIRE_GUARD,retireGuardDdl());exact(db,REFERENCE_GUARD,referenceGuardDdl());exact(db,UPDATE_GUARD,updateGuardDdl());exact(db,INSERT_GUARD,insertGuardDdl());ResearchRetentionStoreR30.requireKeepMigrationSourceR31(db,ARCHIVE);
        long revision;Map<String,Object> checkpoint;String raw;
        try(Cursor c=db.rawQuery("SELECT revision,state_json FROM r31_keep_checkpoint WHERE archive_table=?",new String[]{ARCHIVE})){if(!c.moveToFirst())return false;revision=c.getLong(0);raw=c.getString(1);checkpoint=(Map<String,Object>)JsonR27.parse(raw);}
        if(!"SWITCH".equals(checkpoint.get("stage"))||((Number)checkpoint.get("upperRowId")).longValue()!=KeepMigrationStoreR31.scalar(db,"SELECT coalesce(max(__r30_id),0) FROM "+ARCHIVE))return false;
        if(KeepMigrationStoreR31.scalar(db,"SELECT count(*) FROM "+ARCHIVE+" k WHERE NOT EXISTS(SELECT 1 FROM r31_keep_original o JOIN r31_keep_mismatch_original m ON m.archive_id=o.row_id AND m.row_hash=o.row_hash WHERE o.archive_table=? AND o.row_id=k.__r30_id AND o.state='MIGRATED_ORIGINAL_RETAINED')",ARCHIVE)!=0)return false;
        String schema;try(Cursor c=db.rawQuery("SELECT schema_hash FROM r30_retention_archive_index WHERE source_table=?",new String[]{SOURCE})){if(!c.moveToFirst())return false;schema=c.getString(0);}
        String old=ARCHIVE+"_immutable_delete";
        if(hasObject(db,old)){
            exact(db,old,"CREATE TRIGGER IF NOT EXISTS "+q(old)+" BEFORE DELETE ON "+q(ARCHIVE)+" BEGIN SELECT RAISE(ABORT,'R30_PROTECTED_ARCHIVE_IMMUTABLE'); END");
            db.execSQL("INSERT OR REPLACE INTO r31_keep_cutover VALUES(?,'GUARDED_PREPARED',?,?,?,?,?,?)",new Object[]{SOURCE,schema,revision,authority,StatsR27.sha256(READER_CONTRACT),StatsR27.sha256(PRODUCER_CONTRACT),System.currentTimeMillis()});
            db.execSQL("DROP TRIGGER "+q(old));return true;
        }
        if(!"GUARDED_PREPARED".equals(mode(db)))throw new IllegalStateException("R31_KEEP_UNDECLARED_GUARD_REMOVAL");
        db.execSQL("UPDATE r31_keep_cutover SET mode='REFERENCE_ONLY',checkpoint_revision=?,authority_hash=?,updated_at=? WHERE source_table=? AND mode='GUARDED_PREPARED'",new Object[]{revision,authority,System.currentTimeMillis(),SOURCE});
        db.execSQL("UPDATE r30_retention_archive_index SET schema_hash=? WHERE source_table=? AND schema_hash=?",new Object[]{PREFIX+schema,SOURCE,schema});
        if(KeepMigrationStoreR31.scalar(db,"SELECT changes()")!=1)throw new IllegalStateException("R31_KEEP_REFERENCE_SWITCH_CAS");
        checkpoint.put("stage","DONE");checkpoint.put("blockedReason",null);checkpoint.put("newWriteDisposition","REFERENCE_ONLY_WITH_EXACT_CAS_PERMIT");checkpoint.put("authorityProof",authority);checkpoint.put("readerContractHash",StatsR27.sha256(READER_CONTRACT));checkpoint.put("producerContractHash",StatsR27.sha256(PRODUCER_CONTRACT));
        db.execSQL("UPDATE r31_keep_checkpoint SET revision=revision+1,state_json=?,updated_at=? WHERE archive_table=? AND revision=? AND state_json=?",new Object[]{JsonR27.write(checkpoint),System.currentTimeMillis(),ARCHIVE,revision,raw});if(KeepMigrationStoreR31.scalar(db,"SELECT changes()")!=1)throw new IllegalStateException("R31_KEEP_CUTOVER_CHECKPOINT_CAS");return true;
    }
    static void indexLegacyWithin(SQLiteDatabase db,long archiveId,String hash,Object[] values){db.execSQL("INSERT OR IGNORE INTO r31_keep_mismatch_original VALUES(?,?,?,?,?,?)",new Object[]{archiveId,hash,values[0],values[1],values[4],System.currentTimeMillis()});try(Cursor c=db.rawQuery("SELECT row_hash,issue,decision_key,direction FROM r31_keep_mismatch_original WHERE archive_id=?",new String[]{Long.toString(archiveId)})){if(!c.moveToFirst()||!hash.equals(c.getString(0))||!Objects.equals(values[0],c.getString(1))||!Objects.equals(values[1],c.getString(2))||!Objects.equals(values[4],c.getString(3)))throw new IllegalStateException("R31_KEEP_MISMATCH_INDEX_CONFLICT");}}
    public static String countSql(SQLiteDatabase db,String direction){
        String active="SELECT issue,decision_key,direction FROM "+SOURCE;if(ready(db))active+=" UNION SELECT issue,decision_key,direction FROM r31_keep_mismatch_original";
        return "SELECT count(*) FROM ("+active+")"+(direction==null?"":" WHERE direction="+lit(direction));
    }
    /** One maintenance producer unit. Return -1 when there is no eligible
     * mismatch work, allowing the other safe keep families to continue. */
    static long step(DbStore store,RuntimeCoordinatorR26.Claim claim,BudgetTokenSU1 budget)throws IOException{
        SQLiteDatabase read=store.readable();if(!ready(read)||!"REFERENCE_ONLY".equals(mode(read)))return -1;budget.checkpointOrYield();
        KeepMigrationStoreR31.Writer writer=(op,work)->store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.RETENTION,op),db->{RuntimeCoordinatorR26.requireFence(db,claim);work.run(db);RuntimeCoordinatorR26.requireFence(db,claim);return 0;});
        File root=new File(store.contextForR26().getFilesDir(),"r31-keep");
        if(retireOne(read,root,writer)){claim.reason("R31_KEEP_MISMATCH_LEGACY_ROW_RETIRED_TO_VERIFIED_CAS");return 1;}
        Models.Draw latest=store.getLatestDraw();if(latest==null||!TargetIssueResolver.isValid(latest.issue))return -1;
        if(archiveOne(read,root,writer,TargetIssueResolver.sequenceOrdinalSU1(latest.issue)-5000)){claim.reason("R31_KEEP_MISMATCH_REFERENCE_ONLY_ORIGINAL_COMMITTED");return 1;}return -1;
    }
    public static boolean retireOne(SQLiteDatabase read,File root,KeepMigrationStoreR31.Writer writer)throws IOException{
        if(!"REFERENCE_ONLY".equals(mode(read)))return false;long id;String hash;
        try(Cursor c=read.rawQuery("SELECT k.__r30_id,o.row_hash FROM "+ARCHIVE+" k JOIN r31_keep_original o ON o.archive_table=? AND o.row_id=k.__r30_id AND o.state='MIGRATED_ORIGINAL_RETAINED' ORDER BY k.__r30_id LIMIT 1",new String[]{ARCHIVE})){if(!c.moveToFirst())return false;id=c.getLong(0);hash=c.getString(1);}
        KeepMigrationStoreR31.readArtifact(root,ARCHIVE,id,hash,COLUMNS.split(","));
        writer.write("R31_KEEP_RETIRE_EXACT_CAS_ORIGINAL",db->{if(authorityProof(db)==null||!"REFERENCE_ONLY".equals(mode(db)))throw new IllegalStateException("R31_KEEP_AUTHORITY_CHANGED");ResearchRetentionStoreR30.requireKeepMigrationSourceR31(db,ARCHIVE);
            db.execSQL("INSERT INTO r31_keep_retire_permit VALUES(?,?,?)",new Object[]{ARCHIVE,id,hash});db.execSQL("DELETE FROM "+ARCHIVE+" WHERE __r30_id=?",new Object[]{id});if(KeepMigrationStoreR31.scalar(db,"SELECT changes()")!=1)throw new IllegalStateException("R31_KEEP_RETIRE_CAS");db.execSQL("DELETE FROM r31_keep_retire_permit WHERE archive_table=? AND row_id=?",new Object[]{ARCHIVE,id});});return true;
    }
    public static boolean archiveOne(SQLiteDatabase read,File root,KeepMigrationStoreR31.Writer writer,long cutoff)throws IOException{
        if(!"REFERENCE_ONLY".equals(mode(read)))return false;requireSourceContract(read);long rowid;Object[] row;
        try(Cursor c=read.rawQuery("SELECT length(CAST(issue AS BLOB)),length(CAST(decision_key AS BLOB)),length(CAST(direction AS BLOB)),length(CAST(original_reason AS BLOB)),length(CAST(first_blocker AS BLOB)) FROM "+SOURCE+" ORDER BY issue,decision_key LIMIT 1",null)){if(!c.moveToFirst())return false;for(int i=0;i<5;i++)if(c.getLong(i)>262144)throw new MemoryBudgetR31.CapacityWait(MemoryBudgetR31.WaitReason.DECLARED_CAPACITY);}
        MemoryBudgetR31.Admission capacity=MemoryBudgetR31.processBudget().tryAcquire("keep-mismatch-producer",MemoryBudgetR31.Kind.CHECKPOINT,8L<<20,()->Thread.currentThread().isInterrupted());if(!capacity.acquired())throw new MemoryBudgetR31.CapacityWait(capacity.reason);
        try(MemoryBudgetR31.Lease memory=capacity.lease){
        try(Cursor c=read.rawQuery("SELECT rowid,"+COLUMNS+" FROM "+SOURCE+" ORDER BY issue,decision_key LIMIT 1",null)){if(!c.moveToFirst())return false;Object[] full=KeepMigrationStoreR31.values(c);rowid=((Number)full[0]).longValue();row=Arrays.copyOfRange(full,1,full.length);}
        if(!(row[0] instanceof String)||!TargetIssueResolver.isValid((String)row[0])||TargetIssueResolver.sequenceOrdinalSU1((String)row[0])>=cutoff)return false;
        long id=-1-Math.abs(Long.parseLong(StatsR27.sha256(JsonR27.write(Arrays.asList(row))).substring(0,15),16));
        String[] names=("__r30_id,__r30_at,__r30_reason,"+COLUMNS).split(",");Object[] original=new Object[row.length+3];original[0]=id;original[1]=System.currentTimeMillis();original[2]="BEFORE_ORIGINAL_DELETE";System.arraycopy(row,0,original,3,row.length);
        KeepMigrationStoreR31.Prepared prepared=KeepMigrationStoreR31.prepareValues(root,ARCHIVE,id,names,original);
        writer.write("R31_KEEP_MISMATCH_REFERENCE_PRODUCER",db->{if(authorityProof(db)==null||!"REFERENCE_ONLY".equals(mode(db)))throw new IllegalStateException("R31_KEEP_AUTHORITY_CHANGED");ResearchRetentionStoreR30.requireKeepMigrationSourceR31(db,ARCHIVE);
            try(Cursor c=db.rawQuery("SELECT "+COLUMNS+" FROM "+SOURCE+" WHERE rowid=?",new String[]{Long.toString(rowid)})){if(!c.moveToFirst()||!Arrays.deepEquals(row,KeepMigrationStoreR31.values(c)))throw new IllegalStateException("R31_KEEP_MISMATCH_SOURCE_CHANGED");}
            db.execSQL("INSERT OR IGNORE INTO r31_keep_original VALUES(?,?,?,?,?,'PRODUCER_PREPARED',?,0)",new Object[]{ARCHIVE,id,prepared.hash,prepared.bytes,"[]",System.currentTimeMillis()});
            try(Cursor c=db.rawQuery("SELECT row_hash,state FROM r31_keep_original WHERE archive_table=? AND row_id=?",new String[]{ARCHIVE,Long.toString(id)})){if(!c.moveToFirst()||!prepared.hash.equals(c.getString(0))||!"PRODUCER_PREPARED".equals(c.getString(1)))throw new IllegalStateException("R31_KEEP_MISMATCH_ORIGINAL_CONFLICT");}
            Object[] permit=new Object[11];permit[0]=rowid;permit[1]=id;permit[2]=prepared.hash;System.arraycopy(row,0,permit,3,row.length);db.execSQL("INSERT INTO r31_keep_mismatch_permit VALUES(?,?,?,?,?,?,?,?,?,?,?)",permit);
            db.execSQL("DELETE FROM "+SOURCE+" WHERE rowid=?",new Object[]{rowid});if(KeepMigrationStoreR31.scalar(db,"SELECT changes()")!=1)throw new IllegalStateException("R31_KEEP_MISMATCH_REFERENCE_CAS");
        });return true;}
    }
}
