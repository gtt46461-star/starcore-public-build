package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.sql.DriverManager;
import java.util.*;

/** Real production disk queue/codec and native SQLite. More than the removed
 * lifecycle cap, not a mock count. Android process scheduling is outside scope. */
public final class CheckpointClosureR324NativeHostTest {
    private static int assertions;
    private static void check(boolean okay,String label){if(!okay)throw new AssertionError(label);assertions++;System.out.println("PASS "+label);}
    interface Checked {void run()throws Exception;}
    private static void rejected(Checked action,String label)throws Exception{boolean failed=false;try{action.run();}catch(IOException expected){failed=true;}check(failed,label);}
    private static final ColdCheckpointR324.DirectorySync SYNC=dir->{try(FileChannel channel=FileChannel.open(dir.toPath(),StandardOpenOption.READ)){channel.force(true);}};
    private static boolean noScratch(File work){String[] names=work.list((dir,name)->name.startsWith("checkpoint-closure-r324-"));return names!=null&&names.length==0;}
    private static String put(File root,byte[] bytes)throws Exception{String hash=StatsR27.sha256(bytes);Files.write(new File(root,hash+".bin").toPath(),bytes);return hash;}
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");Path parent=Files.createTempDirectory(Paths.get(args[0]),"closure-native-");File root=parent.resolve("r26-checkpoints").toFile();root.mkdir();
        SQLiteDatabase source=new SQLiteDatabase(DriverManager.getConnection("jdbc:sqlite:"+parent.resolve("source.db")));
        source.execSQL("CREATE TABLE r31_gc_pin(artifact_hash TEXT PRIMARY KEY)");String first="";final int direct=17037;
        source.beginTransaction();try{for(int i=0;i<direct;i++){String hash=put(root,("original-"+i).getBytes("UTF-8"));if(i==0)first=hash;source.execSQL("INSERT INTO r31_gc_pin VALUES(?)",new Object[]{hash});}source.setTransactionSuccessful();}finally{source.endTransaction();}
        ColdCheckpointR324.archive(root,first,SYNC);final Set<String> chunks=new TreeSet<>();
        byte[] state=new byte[700000];new Random(324).nextBytes(state);
        byte[] manifest=ChunkedCheckpointR26.write(out->out.write(state),new ChunkedCheckpointR26.Store(){public String put(byte[] bytes)throws IOException{String hash=StatsR27.sha256(bytes);Files.write(new File(root,hash+".bin").toPath(),bytes);chunks.add(hash);return hash;}public InputStream open(String hash)throws IOException{return ColdCheckpointR324.open(root,hash);}});
        String parentHash=put(root,manifest);source.execSQL("INSERT INTO r31_gc_pin VALUES(?)",new Object[]{parentHash});
        String sourceBefore=StatsR27.sha256(Files.readAllBytes(parent.resolve("source.db")));long[] admitted={0},visited={0};
        CheckpointClosureR324.Result result;
        try(CheckpointClosureR324 closure=new CheckpointClosureR324(parent.toFile(),root,()->true,(file,bytes)->{if(bytes<=0)throw new AssertionError("positive_admission");admitted[0]=Math.addExact(admitted[0],bytes);})){
            final String duplicate=first;result=closure.traverse(sink->{CheckpointReferencesR26.forEachHashR324(source,sink);sink.accept(duplicate);sink.accept(parentHash);},(hash,bytes)->{if(bytes<1)throw new AssertionError("verified_size");visited[0]++;});
        }
        check(result.objects==direct+1+chunks.size()&&result.objects>16384,"real_complete_closure_exceeds_16384_original_objects");
        check(result.objects==visited[0],"each_exact_identity_visited_once_despite_duplicate_roots");
        check(result.bytes>state.length,"full_original_byte_counts_include_all_chunk_bodies");
        check(admitted[0]>=4L*1024*1024,"scratch_sqlite_growth_admitted_before_storage");
        check(noScratch(parent.toFile()),"successful_private_index_closed_and_removed");
        check(sourceBefore.equals(StatsR27.sha256(Files.readAllBytes(parent.resolve("source.db")))),"business_snapshot_bytes_unchanged_by_closure");
        check(ColdCheckpointR324.isCold(root,first),"cold_original_read_without_rehydrating_hot_duplicate");
        // Corruption must fail before publishing a verified visitor callback.
        String damaged=put(root,"damage-original".getBytes("UTF-8"));Files.write(new File(root,damaged+".bin").toPath(),"changed-original".getBytes("UTF-8"));long[] badVisits={0};
        rejected(()->{try(CheckpointClosureR324 closure=new CheckpointClosureR324(parent.toFile(),root,()->true,(f,b)->{})){closure.traverse(sink->sink.accept(damaged),(h,b)->badVisits[0]++);}},"changed_hash_rejected_by_actual_byte_reader");
        check(badVisits[0]==0,"corrupt_object_never_emitted_as_verified");check(noScratch(parent.toFile()),"failed_private_index_removed");
        String child=chunks.iterator().next();Path childFile=new File(root,child+".bin").toPath();byte[] saved=Files.readAllBytes(childFile);Files.delete(childFile);
        rejected(()->{try(CheckpointClosureR324 closure=new CheckpointClosureR324(parent.toFile(),root,()->true,(f,b)->{})){closure.traverse(sink->sink.accept(parentHash),(h,b)->{});}},"missing_manifest_child_refuses_complete_closure");Files.write(childFile,saved);
        String unknown=put(root,new byte[]{0x26,0x43,0x53,0x03,0x01});
        rejected(()->{try(CheckpointClosureR324 closure=new CheckpointClosureR324(parent.toFile(),root,()->true,(f,b)->{})){closure.traverse(sink->sink.accept(unknown),(h,b)->{});}},"unknown_manifest_version_not_treated_as_leaf");
        rejected(()->{try(CheckpointClosureR324 closure=new CheckpointClosureR324(parent.toFile(),root,()->false,(f,b)->{})){}},"cancelled_before_index_creation_refuses_work");
        rejected(()->{try(CheckpointClosureR324 closure=new CheckpointClosureR324(parent.toFile(),root,()->true,(f,b)->{throw new IOException("HOST_ADMISSION_REFUSED");})){}},"failed_disk_admission_creates_no_working_database");
        check(noScratch(parent.toFile()),"constructor_failure_cleanup_is_complete");
        rejected(()->{try(CheckpointClosureR324 closure=new CheckpointClosureR324(parent.toFile(),root,()->true,(f,b)->{})){closure.traverse(sink->sink.accept("../escape"),(h,b)->{});}},"invalid_root_identity_refused");
        check(sourceBefore.equals(StatsR27.sha256(Files.readAllBytes(parent.resolve("source.db")))),"failure_cases_do_not_modify_business_snapshot");
        int compactAssertions=0;
        if(Boolean.getBoolean("r324.json.enabled")){
            int beforeCompact=assertions;org.json.JSONObject audit;
            try(Writer out=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(parent.resolve("compact.jsonl").toFile()),"UTF-8"))){
                audit=com.ethk3.novaonepro.CheckpointReportAuditR32.observe(root,sink->CheckpointReferencesR26.forEachHashR324(source,sink),out,()->true);
            }
            check(audit.getBoolean("truncated")&&!audit.getBoolean("sourceRootsComplete"),"compact_root_prefix_explicitly_marks_omitted_history");
            check(!audit.getBoolean("sourceClosureVerified"),"compact_prefix_never_claims_complete_source_closure");
            check(audit.isNull("referenceRoots")&&audit.getInt("retainedRootKeys")==16384,"compact_does_not_publish_prefix_as_exact_root_count");
            check(audit.getLong("observedObjects")<=16384&&audit.getLong("bytesRead")<=8L*1024*1024,"compact_memory_and_byte_limits_remain_observable");
            check("CHECKPOINT_VERIFICATION_INCOMPLETE".equals(audit.getString("state")),"compact_truncation_reports_incomplete_state");
            org.json.JSONObject corruptAudit=com.ethk3.novaonepro.CheckpointReportAuditR32.observe(root,Collections.singleton(damaged),new StringWriter(),()->true);
            check("CHECKPOINT_EVIDENCE_GAPS".equals(corruptAudit.getString("state"))&&corruptAudit.getLong("unreadableObjects")>0,"compact_corrupt_original_is_evidence_gap");
            final String coldHash=first;File cold=ColdCheckpointR324.coldFile(root,coldHash);byte[] packed=Files.readAllBytes(cold.toPath());Files.delete(cold.toPath());
            org.json.JSONObject missingAudit=com.ethk3.novaonepro.CheckpointReportAuditR32.observe(root,Collections.singleton(coldHash),new StringWriter(),()->true);
            check(missingAudit.getLong("missingObjects")==1&&!missingAudit.getBoolean("sourceClosureVerified"),"compact_missing_cold_original_is_not_healthy");Files.write(cold.toPath(),packed);
            rejected(()->com.ethk3.novaonepro.CheckpointReportAuditR32.observe(root,sink->sink.accept(coldHash),new StringWriter(),()->false),"compact_root_stream_honors_cancellation");
            compactAssertions=assertions-beforeCompact;
        }
        System.out.println("COMPACT_JSON_ASSERTIONS="+compactAssertions);
        source.close();System.out.println("CLOSURE_NATIVE_ASSERTIONS="+assertions);System.out.println("ACTUAL_VERIFIED_OBJECTS="+result.objects);
    }
}
