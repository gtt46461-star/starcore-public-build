package com.ethk3.novaonepro.strategyv2;

import java.io.*;
import java.security.*;
import java.util.*;
import java.util.zip.*;

/** Lossless physical tier for the existing R26CAS1 identities.
 *
 * The hot and cold copies have exactly the same uncompressed SHA-256. No model,
 * publication, frozen decision or original-evidence reference is rewritten.
 * A cold object is a header and one gzip stream, in a deterministic hash shard.
 * Archive is called only under the CAS maintenance gate and exclusive report
 * input lease. Readers need no database connection and never create files.
 */
public final class ColdCheckpointR324 {
    private ColdCheckpointR324() {}
    public static final int MAGIC=0x52334334,VERSION=1,MAX_RAW=32<<20;
    private static final long FREE_RESERVE=64L<<20;
    private static final String SUFFIX=".c.gz";
    public interface DirectorySync { void sync(File directory)throws IOException; }
    public interface FaultHook { void at(String phase)throws IOException; }
    private static final FaultHook NO_FAULT=phase->{};
    public static final class Archived {
        public final long hotBytesRemoved,coldBytes,netPhysicalBytesReleased;
        Archived(long hot,long cold,boolean newlyCreated){hotBytesRemoved=hot;coldBytes=cold;netPhysicalBytesReleased=hot-(newlyCreated?cold:0);}
    }
    private static void hash(String value)throws IOException {if(value==null||!value.matches("[0-9a-f]{64}"))throw new IOException("R324_COLD_HASH_IDENTITY");}
    private static File canonical(File value)throws IOException {File absolute=value.getAbsoluteFile();if(!absolute.equals(absolute.getCanonicalFile()))throw new IOException("R324_COLD_SYMLINK_REFUSED");return absolute;}
    public static File hotFile(File root,String hash)throws IOException {hash(hash);return canonical(new File(canonical(root),hash+".bin"));}
    public static File directory(File hotRoot)throws IOException {File root=canonical(hotRoot);if(root.getParentFile()==null)throw new IOException("R324_COLD_ROOT");return canonical(new File(root.getParentFile(),root.getName()+"-cold-r324"));}
    public static File coldFile(File root,String hash)throws IOException {hash(hash);return canonical(new File(new File(directory(root),hash.substring(0,2)),hash+SUFFIX));}
    private static final class Header {
        final long bytes,modified;
        Header(long bytes,long modified){this.bytes=bytes;this.modified=modified;}
    }
    private static Header header(DataInputStream in,String hash)throws IOException {
        if(in.readInt()!=MAGIC||in.readInt()!=VERSION)throw new IOException("R324_COLD_VERSION");
        long bytes=in.readLong(),modified=in.readLong();String stored=in.readUTF();
        if(bytes<1||bytes>MAX_RAW||modified<0||!hash.equals(stored))throw new IOException("R324_COLD_HEADER_IDENTITY");
        return new Header(bytes,modified);
    }
    private static FileNotFoundException unavailableOriginal(File hot,File cold,FileNotFoundException cause) {
        FileNotFoundException failure=new FileNotFoundException("R26_ARTIFACT_UNAVAILABLE|R324_CHECKPOINT_ORIGINAL_UNAVAILABLE|hot="+hot+"|cold="+cold+"|cause="+cause.getMessage());
        failure.initCause(cause);return failure;
    }
    private static Header coldHeader(File root,String hash)throws IOException {
        File cold=coldFile(root,hash);
        try(DataInputStream in=new DataInputStream(new BufferedInputStream(new FileInputStream(cold)))){return header(in,hash);}
        catch(FileNotFoundException missing){throw unavailableOriginal(hotFile(root,hash),cold,missing);}
    }
    public static boolean exists(File root,String hash)throws IOException {return hotFile(root,hash).isFile()||coldFile(root,hash).isFile();}
    public static boolean isCold(File root,String hash)throws IOException {return !hotFile(root,hash).isFile()&&coldFile(root,hash).isFile();}
    public static long rawSize(File root,String hash)throws IOException {
        File hot=hotFile(root,hash);if(hot.isFile()){long n=hot.length();if(n<1||n>MAX_RAW)throw new IOException("R324_HOT_SIZE");return n;}
        return coldHeader(root,hash).bytes;
    }
    public static long rawModified(File root,String hash)throws IOException {File hot=hotFile(root,hash);return hot.isFile()?hot.lastModified():coldHeader(root,hash).modified;}
    public static long physicalBytes(File root,String hash)throws IOException {return hotFile(root,hash).length()+coldFile(root,hash).length();}
    public static final class Cleanup {
        public long releasedBytes;public int removedFiles,scannedEntries;
    }
    /** Temporary files have no CAS identity and are never graph roots. Call only
     * under the same exclusive report lease and CAS maintenance gate as archive.
     * A bounded streaming scan of the dedicated preparation directory also
     * recovers process death before atomic publication; recent work is retained. */
    public static Cleanup cleanupPreparations(File root,long now,long graceMillis,int maximum,DirectorySync sync)throws IOException {
        if(graceMillis<60000||maximum<1||maximum>64)throw new IllegalArgumentException("R324_COLD_CLEANUP_BOUND");
        Cleanup result=new Cleanup();File dir=canonical(new File(directory(root),"prepare"));if(!dir.isDirectory())return result;
        try(com.ethk3.novaonepro.DirectoryCursorR31 cursor=com.ethk3.novaonepro.DirectoryCursorR31.open(dir)){
            File file;while(result.scannedEntries<128&&(file=cursor.next())!=null){
                result.scannedEntries++;String name=file.getName();if(!name.matches("\\.prepare-[0-9a-f]{64}-[0-9a-f-]{36}"))continue;
                if(!file.getAbsoluteFile().equals(file.getCanonicalFile())||!file.isFile())continue;
                long modified=file.lastModified(),bytes=file.length();if(now<modified||now-modified<graceMillis||bytes>MAX_RAW+(4L<<20))continue;
                if(file.lastModified()!=modified||file.length()!=bytes)continue;
                if(file.delete()){result.releasedBytes+=bytes;if(++result.removedFiles>=maximum)break;}
            }
        }
        if(result.removedFiles>0)sync.sync(dir);return result;
    }
    /** Caller must already hold the durable GC DELETING claim, CAS maintenance
     * gate and exclusive report lease. Cold originals are never removed by the
     * archive/admission path. Returns actual compressed + hot bytes unlinked. */
    public static long deleteCopiesAfterGcClaim(File root,String hash,DirectorySync sync)throws IOException {
        File hot=hotFile(root,hash),cold=coldFile(root,hash);long released=0;
        if(cold.isFile()){verifyCold(cold,hash);long bytes=cold.length();if(!cold.delete())throw new IOException("R324_COLD_GC_UNLINK_FAILED");sync.sync(cold.getParentFile());released+=bytes;}
        if(hot.isFile()){try(InputStream in=new VerifiedInput(new FileInputStream(hot),hot.length(),hash)){byte[] buffer=new byte[32768];while(in.read(buffer)!=-1){}}
            long bytes=hot.length();if(!hot.delete())throw new IOException("R324_HOT_GC_UNLINK_FAILED");sync.sync(canonical(root));released+=bytes;}
        return released;
    }
    private static MessageDigest digest(){try{return MessageDigest.getInstance("SHA-256");}catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}}
    private static String hex(byte[] bytes){char[] digits="0123456789abcdef".toCharArray(),out=new char[bytes.length*2];for(int i=0;i<bytes.length;i++){out[2*i]=digits[(bytes[i]>>>4)&15];out[2*i+1]=digits[bytes[i]&15];}return new String(out);}
    private static final class VerifiedInput extends FilterInputStream {
        private final long expected;private final String hash;private final MessageDigest sha=digest();private long read;private boolean done;
        VerifiedInput(InputStream input,long expected,String hash){super(input);this.expected=expected;this.hash=hash;}
        public int read()throws IOException {byte[] one=new byte[1];int n=read(one,0,1);return n<0?-1:one[0]&255;}
        public int read(byte[] bytes,int offset,int count)throws IOException {
            if(count==0)return 0;if(done)return -1;int n=super.read(bytes,offset,(int)Math.min(count,expected-read+1));
            if(n<0){if(read!=expected||!hash.equals(hex(sha.digest())))throw new IOException("R324_COLD_OR_HOT_HASH_MISMATCH");done=true;return -1;}
            read+=n;if(read>expected)throw new IOException("R324_COLD_EXPANSION_BOUND");sha.update(bytes,offset,n);return n;
        }
        public long skip(long count)throws IOException {long total=0;byte[] buffer=new byte[32768];while(total<count){int n=read(buffer,0,(int)Math.min(buffer.length,count-total));if(n<0)break;total+=n;}return total;}
        public boolean markSupported(){return false;}
        public synchronized void reset()throws IOException {throw new IOException("R324_COLD_NO_RESET");}
    }
    private static InputStream openCold(File cold,String hash)throws IOException {
        DataInputStream in=new DataInputStream(new BufferedInputStream(new FileInputStream(cold),32768));
        try{Header header=header(in,hash);return new VerifiedInput(new GZIPInputStream(in,32768),header.bytes,hash);}
        catch(IOException|RuntimeException failed){in.close();throw failed;}
    }
    public static InputStream open(File root,String hash)throws IOException {
        File hot=hotFile(root,hash);
        if(hot.isFile())try{return new VerifiedInput(new FileInputStream(hot),rawSize(root,hash),hash);}catch(FileNotFoundException moved){/* Cold publication precedes hot unlink. */}
        File cold=coldFile(root,hash);
        try{return openCold(cold,hash);}
        catch(FileNotFoundException missing){throw unavailableOriginal(hot,cold,missing);}
    }
    public static byte[] read(File root,String hash)throws IOException {
        long size=rawSize(root,hash);ByteArrayOutputStream bytes=new ByteArrayOutputStream((int)size);
        try(InputStream in=open(root,hash)){byte[] buffer=new byte[32768];int n;while((n=in.read(buffer))!=-1){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("R324_COLD_READ_CANCELLED");bytes.write(buffer,0,n);}}
        if(bytes.size()!=size)throw new IOException("R324_COLD_READ_SIZE");return bytes.toByteArray();
    }
    private static void verifyCold(File file,String hash)throws IOException {try(InputStream in=openCold(file,hash)){byte[] buffer=new byte[32768];while(in.read(buffer)!=-1)if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("R324_COLD_VERIFY_CANCELLED");}}
    public static Archived archive(File root,String hash,DirectorySync sync)throws IOException {return archive(root,hash,sync,NO_FAULT);}
    /** Failure injection is package visible and used by the real fault/restore test. */
    static Archived archive(File root,String hash,DirectorySync sync,FaultHook fault)throws IOException {
        File hot=hotFile(root,hash),cold=coldFile(root,hash);if(!hot.isFile()){verifyCold(cold,hash);return new Archived(0,cold.length(),false);}
        long bytes=hot.length(),modified=hot.lastModified();if(bytes<1||bytes>MAX_RAW)throw new IOException("R324_COLD_SOURCE_SIZE");
        File shard=cold.getParentFile(),archive=directory(root);
        if(!archive.isDirectory()){if(!archive.mkdir()&&!archive.isDirectory())throw new IOException("R324_COLD_ROOT_CREATE");sync.sync(archive.getParentFile());}
        if(!shard.isDirectory()){if(!shard.mkdir()&&!shard.isDirectory())throw new IOException("R324_COLD_SHARD_CREATE");sync.sync(archive);}
        canonical(shard);boolean made=false;File temporary=null;
        File preparation=canonical(new File(archive,"prepare"));
        if(!preparation.isDirectory()){if(!preparation.mkdir()&&!preparation.isDirectory())throw new IOException("R324_COLD_PREPARATION_CREATE");sync.sync(archive);}
        cleanupPreparations(root,System.currentTimeMillis(),60000,8,sync);
        try{
            if(!cold.exists()){
                long needed=bytes+Math.min(bytes/8+65536,4L<<20)+FREE_RESERVE;if(shard.getUsableSpace()<needed)throw new IOException("R324_COLD_DISK_RESERVE");
                temporary=new File(preparation,".prepare-"+hash+"-"+UUID.randomUUID());
                try(FileOutputStream fd=new FileOutputStream(temporary)){
                    DataOutputStream header=new DataOutputStream(fd);header.writeInt(MAGIC);header.writeInt(VERSION);header.writeLong(bytes);header.writeLong(modified);header.writeUTF(hash);header.flush();
                    GZIPOutputStream gzip=new GZIPOutputStream(fd,32768);
                    try(InputStream in=new VerifiedInput(new FileInputStream(hot),bytes,hash)){byte[] buffer=new byte[32768];int n;while((n=in.read(buffer))!=-1){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("R324_COLD_ARCHIVE_CANCELLED");gzip.write(buffer,0,n);}}
                    gzip.finish();gzip.flush();fd.getFD().sync();
                }
                fault.at("AFTER_FILE_SYNC");verifyCold(temporary,hash);
                if(cold.exists()){verifyCold(cold,hash);}else if(!temporary.renameTo(cold))throw new IOException("R324_COLD_ATOMIC_RENAME");else made=true;
                sync.sync(shard);sync.sync(preparation);fault.at("AFTER_COLD_PUBLISH");
            }
            verifyCold(cold,hash);fault.at("AFTER_READBACK");
            // Recheck immutable hot identity, including complete bytes, immediately before removing its duplicate.
            if(hot.length()!=bytes||hot.lastModified()!=modified)throw new IOException("R324_HOT_CHANGED_DURING_ARCHIVE");
            try(InputStream in=new VerifiedInput(new FileInputStream(hot),bytes,hash)){byte[] buffer=new byte[32768];while(in.read(buffer)!=-1)if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("R324_COLD_FINAL_READBACK_CANCELLED");}
            if(!hot.delete())throw new IOException("R324_HOT_DUPLICATE_UNLINK_FAILED");sync.sync(canonical(root));fault.at("AFTER_HOT_UNLINK");
            return new Archived(bytes,cold.length(),made);
        }finally{if(temporary!=null&&temporary.exists())temporary.delete();}
    }
}
