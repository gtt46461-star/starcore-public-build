package com.ethk3.novaonepro.strategyv2;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.security.*;
import java.util.*;

/** Executes the production cold codec/archiver, including its real fsync and
 * rename paths on the Linux host. This is not an Android crash/soak claim. */
public final class ColdCheckpointR324HostTest {
    private static int assertions;
    private static void check(boolean value,String name){if(!value)throw new AssertionError(name);assertions++;System.out.println("PASS "+name);}
    private interface Checked {void run()throws Exception;}
    private static void fails(Checked action,String name)throws Exception {boolean failed=false;try{action.run();}catch(IOException expected){failed=true;}check(failed,name);}
    private static String sha(byte[] bytes)throws Exception {StringBuilder s=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes))s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();}
    private static final ColdCheckpointR324.DirectorySync SYNC=dir->{try(FileChannel channel=FileChannel.open(dir.toPath(),StandardOpenOption.READ)){channel.force(true);}};
    private static File root(Path parent,String name)throws Exception {Path p=parent.resolve(name);Files.createDirectories(p);return p.toFile();}
    private static void hot(File root,String hash,byte[] bytes)throws Exception {Files.write(new File(root,hash+".bin").toPath(),bytes);}
    public static void main(String[] args)throws Exception {
        Path parent=Files.createTempDirectory(Paths.get(args[0]),"cold-production-test-");
        byte[] bytes=new byte[262144];for(int i=0;i<bytes.length;i++)bytes[i]=(byte)(i%37);String hash=sha(bytes);File first=root(parent,"ordinary");hot(first,hash,bytes);
        long originalModified=ColdCheckpointR324.rawModified(first,hash);check(Arrays.equals(bytes,ColdCheckpointR324.read(first,hash)),"hot_exact_read");
        ColdCheckpointR324.Archived result=ColdCheckpointR324.archive(first,hash,SYNC);
        check(result.hotBytesRemoved==bytes.length,"retired_hot_byte_accounting");check(result.coldBytes>0&&result.coldBytes<bytes.length,"actual_compressed_file_size");
        check(result.netPhysicalBytesReleased==bytes.length-result.coldBytes,"net_physical_release_includes_new_cold_copy");
        check(!ColdCheckpointR324.hotFile(first,hash).exists()&&ColdCheckpointR324.isCold(first,hash),"hot_unlinked_only_after_cold_publication");
        check(ColdCheckpointR324.rawSize(first,hash)==bytes.length,"original_length_preserved");check(ColdCheckpointR324.rawModified(first,hash)==originalModified,"original_mtime_preserved");
        check(Arrays.equals(bytes,ColdCheckpointR324.read(first,hash)),"restart_style_cold_exact_read");
        check(ColdCheckpointR324.archive(first,hash,SYNC).hotBytesRemoved==0,"archive_retry_idempotent");
        try(InputStream in=ColdCheckpointR324.open(first,hash)){check(in.skip(1000)==1000&&in.read()==(bytes[1000]&255),"verified_stream_skip_hashes_skipped_bytes");}
        for(String phase:new String[]{"AFTER_FILE_SYNC","AFTER_COLD_PUBLISH","AFTER_READBACK","AFTER_HOT_UNLINK"}){
            File r=root(parent,"fault-"+phase);hot(r,hash,bytes);
            fails(()->ColdCheckpointR324.archive(r,hash,SYNC,at->{if(at.equals(phase))throw new IOException("INJECT_"+phase);}),"injected_"+phase);
            check(Arrays.equals(bytes,ColdCheckpointR324.read(r,hash)),"original_survives_"+phase);
            check(phase.equals("AFTER_HOT_UNLINK")||ColdCheckpointR324.hotFile(r,hash).isFile(),"hot_preserved_before_retirement_"+phase);
            ColdCheckpointR324.archive(r,hash,SYNC);check(Arrays.equals(bytes,ColdCheckpointR324.read(r,hash)),"retry_after_"+phase);
        }
        byte[] random=new byte[65536];new Random(324).nextBytes(random);String randomHash=sha(random);File r=root(parent,"random");hot(r,randomHash,random);ColdCheckpointR324.archive(r,randomHash,SYNC);check(Arrays.equals(random,ColdCheckpointR324.read(r,randomHash)),"arbitrary_state_bits_roundtrip");
        File corrupt=root(parent,"corrupt");hot(corrupt,hash,bytes);ColdCheckpointR324.archive(corrupt,hash,SYNC);File packed=ColdCheckpointR324.coldFile(corrupt,hash);byte[] altered=Files.readAllBytes(packed.toPath());altered[altered.length-7]^=7;Files.write(packed.toPath(),altered);fails(()->ColdCheckpointR324.read(corrupt,hash),"corrupt_cold_payload_rejected");
        File wrong=root(parent,"wrong-header");hot(wrong,hash,bytes);ColdCheckpointR324.archive(wrong,hash,SYNC);try(RandomAccessFile a=new RandomAccessFile(ColdCheckpointR324.coldFile(wrong,hash),"rw")){a.seek(8);a.writeLong(Long.MAX_VALUE);}fails(()->ColdCheckpointR324.read(wrong,hash),"decompression_bomb_header_rejected");
        File truncated=root(parent,"truncated");hot(truncated,hash,bytes);ColdCheckpointR324.archive(truncated,hash,SYNC);try(RandomAccessFile a=new RandomAccessFile(ColdCheckpointR324.coldFile(truncated,hash),"rw")){a.setLength(10);}fails(()->ColdCheckpointR324.rawSize(truncated,hash),"truncated_header_rejected");
        File mismatch=root(parent,"mismatch");hot(mismatch,hash,random);fails(()->ColdCheckpointR324.archive(mismatch,hash,SYNC),"wrong_original_hash_not_archived");check(ColdCheckpointR324.hotFile(mismatch,hash).exists(),"wrong_original_not_deleted");
        File missing=root(parent,"missing");fails(()->ColdCheckpointR324.read(missing,hash),"missing_original_not_fabricated");check(!ColdCheckpointR324.directory(missing).exists(),"read_does_not_create_missing_archive");
        try{ColdCheckpointR324.rawSize(missing,hash);throw new AssertionError("missing size accepted");}
        catch(FileNotFoundException failure){check(failure.getMessage().contains(ColdCheckpointR324.hotFile(missing,hash).getPath())&&failure.getMessage().contains(ColdCheckpointR324.coldFile(missing,hash).getPath()),"missing_size_preserves_hot_and_cold_original_paths");check(failure.getCause() instanceof FileNotFoundException,"missing_size_preserves_original_cause");check(failure.getMessage().startsWith("R26_ARTIFACT_UNAVAILABLE|"),"missing_original_retains_legacy_artifact_error_identity");}
        try(InputStream ignored=ColdCheckpointR324.open(missing,hash)){throw new AssertionError("missing stream accepted");}
        catch(FileNotFoundException failure){check(failure.getMessage().contains(ColdCheckpointR324.hotFile(missing,hash).getPath())&&failure.getMessage().contains(ColdCheckpointR324.coldFile(missing,hash).getPath()),"missing_stream_preserves_hot_and_cold_original_paths");check(failure.getCause() instanceof FileNotFoundException,"missing_stream_preserves_original_cause");}
        File symlink=root(parent,"symlink");Path target=parent.resolve("external.bin");Files.write(target,bytes);Files.createSymbolicLink(new File(symlink,hash+".bin").toPath(),target);fails(()->ColdCheckpointR324.read(symlink,hash),"symlink_read_refused");fails(()->ColdCheckpointR324.archive(symlink,hash,SYNC),"symlink_archive_refused");check(Files.exists(target),"external_file_preserved");
        fails(()->ColdCheckpointR324.open(first,"../bad"),"invalid_identity_refused");
        long physical=ColdCheckpointR324.physicalBytes(first,hash);check(physical==result.coldBytes,"physical_census_uses_compressed_size");
        long removed=ColdCheckpointR324.deleteCopiesAfterGcClaim(first,hash,SYNC);check(removed==physical&&!ColdCheckpointR324.exists(first,hash),"claimed_gc_reports_actual_cold_bytes");
        check(ColdCheckpointR324.deleteCopiesAfterGcClaim(first,hash,SYNC)==0,"claimed_gc_crash_retry_idempotent");
        File preparation=new File(ColdCheckpointR324.directory(first),"prepare");long now=System.currentTimeMillis();
        File old=new File(preparation,".prepare-"+hash+"-"+UUID.randomUUID());Files.write(old.toPath(),new byte[17]);check(old.setLastModified(now-120000),"orphan_fixture_age_set");
        File recent=new File(preparation,".prepare-"+hash+"-"+UUID.randomUUID());Files.write(recent.toPath(),new byte[23]);
        File protectedName=new File(preparation,"unrecognized-original.bin");Files.write(protectedName.toPath(),bytes);protectedName.setLastModified(now-120000);
        ColdCheckpointR324.Cleanup clean=ColdCheckpointR324.cleanupPreparations(first,now,60000,8,SYNC);
        check(clean.removedFiles==1&&clean.releasedBytes==17&&!old.exists(),"aged_orphan_removed_with_actual_bytes");
        check(recent.exists(),"recent_preparation_preserved");check(protectedName.exists(),"unknown_name_preserved");
        for(int i=0;i<5;i++){File orphan=new File(preparation,".prepare-"+hash+"-"+UUID.randomUUID());Files.write(orphan.toPath(),new byte[]{1});orphan.setLastModified(now-120000);}
        ColdCheckpointR324.Cleanup limited=ColdCheckpointR324.cleanupPreparations(first,now,60000,2,SYNC);check(limited.removedFiles==2,"cleanup_unlink_budget_enforced");
        ColdCheckpointR324.Cleanup continued=ColdCheckpointR324.cleanupPreparations(first,now,60000,8,SYNC);check(continued.removedFiles==3,"cleanup_next_slice_progresses");
        System.out.println("COLD_HOST_ASSERTIONS="+assertions);
    }
}
