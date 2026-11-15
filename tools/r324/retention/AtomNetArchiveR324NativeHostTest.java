package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.SchemaDomainCoordinatorR926;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.sql.DriverManager;
import java.util.*;

/** Actual production schema, codec, quota and read adapters on native SQLite.
 * Transaction failure cases exercise the exact production guard DDL; the host
 * drives the four retirement statements directly, without an Android worker.
 */
public final class AtomNetArchiveR324NativeHostTest {
    private static int assertions;
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);assertions++;System.out.println("PASS "+label);}
    private interface Checked {void run()throws Exception;}
    private static void rejected(Checked work,String label)throws Exception {boolean failed=false;try{work.run();}catch(Exception expected){failed=true;}check(failed,label);}
    private static long scalar(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){if(!c.moveToFirst())throw new AssertionError("scalar");return c.getLong(0);}}
    private static void tx(SQLiteDatabase db,Checked body)throws Exception {db.beginTransaction();try{body.run();db.setTransactionSuccessful();}finally{db.endTransaction();}}
    private static final ColdCheckpointR324.DirectorySync SYNC=dir->{try(FileChannel channel=FileChannel.open(dir.toPath(),StandardOpenOption.READ)){channel.force(true);}};
    private static final String DATA=StatsR27.sha256("archive-fixture-dataset"),STATE=StatsR27.sha256("archive-fixture-state");
    private static byte[] features(int index)throws Exception {ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(DataOutputStream out=new DataOutputStream(bytes)){for(int i=0;i<11;i++)out.writeDouble(i==0?-0.0:(index+1.0)/(i+1.0));}return bytes.toByteArray();}
    private static AtomNetSampleArchiveR324.Segment segment(String operator,int first)throws Exception {
        String id=StatsR27.sha256(operator+"-"+first);List<AtomNetSampleArchiveR324.Row> rows=new ArrayList<>();StringBuilder digest=new StringBuilder(operator);
        for(int i=first;i<first+10;i++){byte[] features=features(i);String issue="20261115000"+(i/10),hash=AtomNetPublicationR31.sampleHash(new Object[]{DATA,i,issue,i%10+1,features,i%2,i%10});rows.add(new AtomNetSampleArchiveR324.Row(i,issue,i%10+1,features,i%2,i%10,hash));digest.append('|').append(hash);}
        return new AtomNetSampleArchiveR324.Segment(id,DATA,operator,StatsR27.sha256(digest.toString()),STATE,first,rows);
    }
    private static void insert(SQLiteDatabase db,AtomNetSampleArchiveR324.Segment segment)throws Exception {
        long bytes=0;for(AtomNetSampleArchiveR324.Row row:segment.rows)bytes+=row.features.length+3L*row.issue.length()+128;
        db.execSQL("INSERT INTO r31_atomnet_publication(publication_id,operator_version,owner,dataset_hash,previous_state_hash,state_hash,state_ref,through_ordinal,materialized_samples,first_sample,sample_count,sample_bytes,sample_digest,status,created_at,published_at) VALUES(?,?,?,?,?,?,?,100,?,?,?,?,?,'PUBLISHED',1,2)",new Object[]{segment.publication,segment.operator,AtomNetSessionR30.OWNER,DATA,"",STATE,("R26CAS1:"+STATE).getBytes("US-ASCII"),segment.first+segment.rows.size(),segment.first,segment.rows.size(),bytes,segment.digest});
        for(AtomNetSampleArchiveR324.Row row:segment.rows)db.execSQL("INSERT INTO r31_atomnet_sample_stage VALUES(?,?,?,?,?,?,?,?)",new Object[]{segment.publication,row.index,row.issue,row.position,row.features,row.hit,row.number,row.hash});
    }
    private static void receipt(SQLiteDatabase db,AtomNetSampleArchiveR324.Segment segment,byte[] reference,String payload,String digest)throws Exception {db.execSQL("INSERT INTO r324_atomnet_sample_archive VALUES(?,?,?,?,?,?,?)",new Object[]{segment.publication,reference,payload,segment.rows.size(),segment.first,digest,3});}
    private static void retirement(SQLiteDatabase db,AtomNetSampleArchiveR324.Segment segment,byte[] reference,String payload)throws Exception {
        receipt(db,segment,reference,payload,segment.digest);db.execSQL("INSERT INTO r324_atomnet_sample_retire_permit VALUES(?,?)",new Object[]{segment.publication,payload});db.execSQL("DELETE FROM r31_atomnet_sample_stage WHERE publication_id=?",new Object[]{segment.publication});db.execSQL("DELETE FROM r324_atomnet_sample_retire_permit WHERE publication_id=?",new Object[]{segment.publication});
    }
    public static void main(String[] args)throws Exception {
        Class.forName("org.sqlite.JDBC");Path work=Files.createTempDirectory(Paths.get(args[0]),"sample-native-");SQLiteDatabase db=new SQLiteDatabase(DriverManager.getConnection("jdbc:sqlite:"+work.resolve("native.db")));
        tx(db,()->{
            // Install the actual legacy unconditional guard, then run the production upgrade.
            for(String sql:AtomNetPublicationR31.DDL)db.execSQL(sql);
            db.execSQL("DROP TRIGGER r31_atomnet_stage_published_no_delete");db.execSQL("CREATE TRIGGER r31_atomnet_stage_published_no_delete BEFORE DELETE ON r31_atomnet_sample_stage BEGIN SELECT RAISE(ABORT,'R31_ATOM_PUBLISHED_SAMPLE_PROTECTED'); END");
            AtomNetPublicationR31.ensureSchemaR31(db);
            for(String sql:AtomNetStoreR30.DDL)db.execSQL(sql);
            db.execSQL("CREATE TABLE r29_model_checkpoint(owner TEXT,state_hash TEXT,dataset_hash TEXT,artifact BLOB)");
        });
        AtomNetPublicationR31.requireReady(db);check(scalar(db,"SELECT count(*) FROM sqlite_master WHERE name='r324_atomnet_sample_archive'")==1,"production_migration_installs_archive_and_upgrades_known_guard");
        tx(db,()->{for(String ddl:CheckpointGcStoreR31.DDL)db.execSQL(ddl);SchemaDomainCoordinatorR926.verifyAndStamp(db,SchemaDomainCoordinatorR926.r31AtomNetPublicationContract());});
        int attachments=0;for(String ddl:CheckpointGcStoreR31.referenceGuards(db))if(ddl.contains(" ON \"r324_atomnet_sample_archive\" ")){
            check(SchemaDomainCoordinatorR926.isR31PublicationAttachment(db,ddl),"new_archive_gc_guard_has_exact_domain_owner_"+attachments);
            tx(db,()->SchemaDomainCoordinatorR926.installR31PublicationAttachment(db,ddl));attachments++;
        }
        check(attachments==2,"both_insert_update_archive_reference_guards_installed");
        check(SchemaDomainCoordinatorR926.matches(db,SchemaDomainCoordinatorR926.r31AtomNetPublicationContract()),"gc_attachments_preserve_authenticated_atomnet_domain");
        tx(db,()->SchemaDomainCoordinatorR926.migrateR31AtomNetPublication(db));check(SchemaDomainCoordinatorR926.matches(db,SchemaDomainCoordinatorR926.r31AtomNetPublicationContract()),"next_startup_migration_keeps_archive_domain_ready");
        AtomNetSampleArchiveR324.Segment old=segment("R31_ATOMNET_PUBLICATION_1",0),fresh=segment(AtomNetPublicationR31.VERSION,10);
        for(AtomNetSampleArchiveR324.Segment source:new AtomNetSampleArchiveR324.Segment[]{old,fresh}){
            byte[] encoded=AtomNetSampleArchiveR324.encode(source);AtomNetSampleArchiveR324.Segment decoded=AtomNetSampleArchiveR324.decode(encoded);
            check(decoded.digest.equals(source.digest)&&decoded.operator.equals(source.operator),"original_operator_digest_roundtrip_"+source.operator);
            for(int i=0;i<source.rows.size();i++)check(Arrays.equals(source.rows.get(i).features,decoded.rows.get(i).features),"sample_feature_bits_preserved_"+source.operator+"_"+i);
        }
        byte[] malformed=AtomNetSampleArchiveR324.encode(old);malformed[malformed.length-2]^=1;rejected(()->AtomNetSampleArchiveR324.decode(malformed),"damaged_sample_digest_rejected");
        AtomNetSampleArchiveR324.Segment unknown=new AtomNetSampleArchiveR324.Segment(old.publication,old.dataset,"UNREGISTERED_OPERATOR",old.digest,old.state,old.first,old.rows);rejected(()->AtomNetSampleArchiveR324.encode(unknown),"unknown_operator_codec_not_guessed");
        tx(db,()->{insert(db,old);insert(db,fresh);});
        rejected(()->tx(db,()->db.execSQL("DELETE FROM r31_atomnet_sample_stage")),"unpermitted_original_delete_rejected");check(scalar(db,"SELECT count(*) FROM r31_atomnet_sample_stage")==20,"unpermitted_delete_preserves_all_rows");
        File files=work.resolve("r26-checkpoints").toFile();files.mkdirs();CheckpointArtifactsR26 artifacts=new CheckpointArtifactsR26(files);CheckpointArtifactsR26.registerDatabaseRootR31(db,files);
        byte[] payloadBytes=AtomNetSampleArchiveR324.encode(old),reference=artifacts.prepare(payloadBytes);String payload=StatsR27.sha256(payloadBytes);
        rejected(()->tx(db,()->{receipt(db,old,reference,payload,old.digest);db.execSQL("DELETE FROM r31_atomnet_sample_stage WHERE publication_id=?",new Object[]{old.publication});}),"receipt_without_scoped_permit_rejected");
        check(scalar(db,"SELECT count(*) FROM r324_atomnet_sample_archive")==0,"failed_retirement_receipt_rolls_back");
        rejected(()->tx(db,()->{receipt(db,old,reference,payload,old.digest);db.execSQL("INSERT INTO r324_atomnet_sample_retire_permit VALUES(?,?)",new Object[]{old.publication,StatsR27.sha256("wrong")});db.execSQL("DELETE FROM r31_atomnet_sample_stage WHERE publication_id=?",new Object[]{old.publication});}),"wrong_archive_identity_permit_rejected");
        rejected(()->tx(db,()->{receipt(db,old,reference,payload,StatsR27.sha256("wrong-digest"));db.execSQL("INSERT INTO r324_atomnet_sample_retire_permit VALUES(?,?)",new Object[]{old.publication,payload});db.execSQL("DELETE FROM r31_atomnet_sample_stage WHERE publication_id=?",new Object[]{old.publication});}),"wrong_publication_digest_receipt_rejected");
        db.beginTransaction();try{retirement(db,old,reference,payload);}finally{db.endTransaction();} // process-death style uncommitted transaction
        check(scalar(db,"SELECT count(*) FROM r31_atomnet_sample_stage")==20&&scalar(db,"SELECT count(*) FROM r324_atomnet_sample_archive")==0,"death_after_delete_rolls_back_original_rows_and_receipt");
        AtomNetSessionR30 state=new AtomNetSessionR30(DATA,"202611150000",100,11,20,10,10);state.materializedSamples=20;
        AtomNetSessionR30.Samples before=AtomNetStoreR30.samples(db,state);before.prefetch(new int[]{0,9,10,19});Map<Integer,double[]> bits=new HashMap<>();for(int index:new int[]{0,9,10,19})bits.put(index,before.get(index).x.clone());
        tx(db,()->retirement(db,old,reference,payload));
        check(scalar(db,"SELECT count(*) FROM r31_atomnet_sample_stage")==10&&scalar(db,"SELECT count(*) FROM r324_atomnet_sample_archive")==1,"verified_permitted_retirement_commits_exact_segment");
        check(scalar(db,"SELECT count(*) FROM r324_atomnet_sample_retire_permit")==0,"one_transaction_permit_not_left_live");
        check(AtomNetPublicationR31.reservationR32(db,DATA,"").samples==10,"archived_samples_leave_active_reservation");
        check(scalar(db,"SELECT count(*) FROM r31_atomnet_publication")==2,"original_publication_identities_preserved");
        AtomNetSessionR30.Samples after=AtomNetStoreR30.samples(db,state);after.prefetch(new int[]{0,9,10,19});
        for(int index:new int[]{0,9,10,19})check(Arrays.equals(bits.get(index),after.get(index).x),"hot_archive_mixed_reader_equivalence_"+index);
        check(Double.doubleToRawLongBits(after.get(0).x[0])==Double.doubleToRawLongBits(-0.0),"signed_zero_feature_bit_preserved");
        int leaseBefore=((Number)MemoryBudgetR31.processBudget().snapshot().get("activeLeaseCount")).intValue();boolean[] leaseHeld={false};
        AtomNetSampleArchiveR324.read(db,DATA,new HashSet<>(Arrays.asList(0)),sample->{leaseHeld[0]=((Number)MemoryBudgetR31.processBudget().snapshot().get("activeLeaseCount")).intValue()>leaseBefore;});
        check(leaseHeld[0],"sample_decode_consumer_runs_inside_memory_reservation");
        check(((Number)MemoryBudgetR31.processBudget().snapshot().get("activeLeaseCount")).intValue()==leaseBefore,"sample_decode_memory_reservation_released_after_consumer");
        ColdCheckpointR324.archive(files,payload,SYNC);AtomNetSessionR30.Samples cold=AtomNetStoreR30.samples(db,state);cold.prefetch(new int[]{0,9});check(Arrays.equals(bits.get(9),cold.get(9).x),"archived_sample_reader_after_physical_cold_move");
        rejected(()->tx(db,()->db.execSQL("UPDATE r324_atomnet_sample_archive SET payload_hash='changed'")),"archived_receipt_immutable_update");
        rejected(()->tx(db,()->db.execSQL("DELETE FROM r324_atomnet_sample_archive")),"archived_receipt_immutable_delete");
        AtomNetSampleArchiveR324.Row row=old.rows.get(0);rejected(()->tx(db,()->db.execSQL("INSERT INTO r31_atomnet_sample_stage VALUES(?,?,?,?,?,?,?,?)",new Object[]{old.publication,row.index,row.issue,row.position,row.features,row.hit,row.number,row.hash})),"retired_hot_rows_cannot_be_reintroduced");
        tx(db,()->db.execSQL("INSERT INTO r30_atomnet_sample VALUES(?,?,?,?,?,?,?)",new Object[]{DATA,row.index,row.issue,row.position,row.features,1-row.hit,row.number}));
        rejected(()->AtomNetStoreR30.samples(db,state).prefetch(new int[]{0}),"conflicting_legacy_and_archive_evidence_rejected");tx(db,()->db.execSQL("DELETE FROM r30_atomnet_sample"));
        Path packed=ColdCheckpointR324.coldFile(files,payload).toPath();byte[] packedBytes=Files.readAllBytes(packed),broken=packedBytes.clone();broken[broken.length-4]^=5;Files.write(packed,broken);
        rejected(()->AtomNetStoreR30.samples(db,state).prefetch(new int[]{0}),"corrupt_cold_sample_archive_is_not_fabricated");Files.write(packed,packedBytes);
        check(((Number)MemoryBudgetR31.processBudget().snapshot().get("activeLeaseCount")).intValue()==leaseBefore,"failed_archive_read_releases_memory_reservation");
        Files.delete(packed);rejected(()->AtomNetStoreR30.samples(db,state).prefetch(new int[]{0}),"missing_cold_sample_archive_is_explicit_failure");Files.write(packed,packedBytes);
        // Existing unknown protection SQL must never be silently replaced by a migration.
        tx(db,()->{db.execSQL("DROP TRIGGER r31_atomnet_stage_published_no_delete");db.execSQL("CREATE TRIGGER r31_atomnet_stage_published_no_delete BEFORE DELETE ON r31_atomnet_sample_stage BEGIN SELECT RAISE(ABORT,'UNREGISTERED_GUARD'); END");});
        rejected(()->tx(db,()->AtomNetPublicationR31.ensureSchemaR31(db)),"unknown_guard_migration_drift_refused");
        check(scalar(db,"SELECT count(*) FROM r31_atomnet_sample_stage")==10,"unknown_migration_preserves_remaining_originals");
        System.out.println("SAMPLE_NATIVE_ASSERTIONS="+assertions);
    }
}
