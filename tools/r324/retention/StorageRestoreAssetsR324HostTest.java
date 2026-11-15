package com.ethk3.novaonepro;

import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.strategyv2.*;
import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.sql.DriverManager;
import java.util.*;

/** Exercises actual private-clone streaming copy/verify and the production
 * restore reference consumer; not a Binder/device restore proof. */
public final class StorageRestoreAssetsR324HostTest {
    private static int assertions;
    private static void check(boolean okay,String label){if(!okay)throw new AssertionError(label);assertions++;System.out.println("PASS "+label);}
    interface Checked{void run()throws Exception;}
    private static void rejected(Checked action,String label)throws Exception{boolean failed=false;try{action.run();}catch(IOException expected){failed=true;}check(failed,label);}
    private static final ColdCheckpointR324.DirectorySync SYNC=dir->{try(FileChannel channel=FileChannel.open(dir.toPath(),StandardOpenOption.READ)){channel.force(true);}};
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");Path work=Files.createTempDirectory(Paths.get(args[0]),"restore-assets-");File original=work.resolve("original").toFile(),clone=work.resolve("clone").toFile();
        File hot=new File(original,"files/r26-checkpoints");hot.mkdirs();clone.mkdir();SQLiteDatabase db=new SQLiteDatabase(DriverManager.getConnection("jdbc:sqlite:"+work.resolve("restore.db")));
        // An otherwise unknown legacy BLOB table must still participate in restore closure.
        db.execSQL("CREATE TABLE legacy_private_evidence(id INTEGER PRIMARY KEY,checkpoint BLOB)");final int total=17011;String first="";
        db.beginTransaction();try{for(int i=0;i<total;i++){byte[] bytes=("restore-source-"+i).getBytes("UTF-8");String hash=StatsR27.sha256(bytes);if(i==0)first=hash;Files.write(new File(hot,hash+".bin").toPath(),bytes);db.execSQL("INSERT INTO legacy_private_evidence VALUES(?,?)",new Object[]{i,("R26CAS1:"+hash).getBytes("US-ASCII")});}db.setTransactionSuccessful();}finally{db.endTransaction();}
        ColdCheckpointR324.archive(hot,first,SYNC);File preparations=new File(ColdCheckpointR324.directory(hot),"prepare");Files.write(new File(preparations,"unfinished.tmp").toPath(),new byte[]{1,2,3});
        StorageRestoreAssetsR324.Totals copied=StorageRestoreAssetsR324.copy(original,clone,new String[]{"files/r26-checkpoints","files/r26-checkpoints-cold-r324"});
        check(copied.files==total&&copied.files>16384,"actual_stream_copy_exceeds_16384_files");
        check(!new File(clone,"files/r26-checkpoints-cold-r324/prepare").exists(),"uncommitted_cold_preparation_is_not_a_restore_asset");
        File manifest=new File(clone,StorageRestoreAssetsR324.MEMBER);StorageRestoreAssetsR324.Totals verified=StorageRestoreAssetsR324.verify(clone,manifest);
        check(verified.files==copied.files&&verified.bytes==copied.bytes,"stream_manifest_verifies_every_copied_body_and_totals");
        String originalDb=StatsR27.sha256(Files.readAllBytes(work.resolve("restore.db")));long closure=StorageRestoreBusinessProbeR31.verifyReferences(db,new File(clone,"files"));
        check(closure==total,"actual_restore_consumer_reads_more_than_16384_generic_blob_roots");
        check(originalDb.equals(StatsR27.sha256(Files.readAllBytes(work.resolve("restore.db")))),"restore_closure_does_not_mutate_source_database");
        File coldRoot=new File(clone,"files/r26-checkpoints");check(ColdCheckpointR324.isCold(coldRoot,first),"copied_cold_original_resolves_without_hot_recreation");
        byte[] manifestBytes=Files.readAllBytes(manifest.toPath());Files.write(manifest.toPath(),Arrays.copyOf(manifestBytes,manifestBytes.length-4));
        rejected(()->StorageRestoreAssetsR324.verify(clone,manifest),"truncated_stream_manifest_cannot_pass_restore");Files.write(manifest.toPath(),manifestBytes);
        File cold=ColdCheckpointR324.coldFile(coldRoot,first);byte[] saved=Files.readAllBytes(cold.toPath());byte[] corrupt=saved.clone();corrupt[corrupt.length-4]^=1;Files.write(cold.toPath(),corrupt);
        rejected(()->StorageRestoreAssetsR324.verify(clone,manifest),"changed_cold_body_rejected_by_stream_manifest");Files.write(cold.toPath(),saved);
        File cancelled=work.resolve("cancelled").toFile();cancelled.mkdir();Thread.currentThread().interrupt();try{rejected(()->StorageRestoreAssetsR324.copy(original,cancelled,new String[]{"files/r26-checkpoints"}),"interrupted_copy_never_reports_complete");}finally{Thread.interrupted();}
        File sourceLink=new File(original,"files/external-link");Files.createSymbolicLink(sourceLink.toPath(),manifest.toPath().toAbsolutePath());File refused=work.resolve("refused").toFile();refused.mkdir();
        rejected(()->StorageRestoreAssetsR324.copy(original,refused,new String[]{"files/external-link"}),"symlink_source_not_followed_during_private_copy");
        check(StorageRestoreAssetsR324.removePrivateTree(clone,clone,0)&&!clone.exists(),"stream_workspace_cleanup_removes_more_than_16384_files");
        check(ColdCheckpointR324.exists(hot,first)&&new File(hot,StatsR27.sha256("restore-source-1".getBytes("UTF-8"))+".bin").isFile(),"private_cleanup_preserves_all_original_source_tiers");
        db.close();System.out.println("RESTORE_ASSETS_ASSERTIONS="+assertions);System.out.println("ACTUAL_COPIED_FILES="+copied.files);
    }
}
