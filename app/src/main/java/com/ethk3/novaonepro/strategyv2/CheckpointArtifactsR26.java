package com.ethk3.novaonepro.strategyv2;

import android.content.Context;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import com.ethk3.novaonepro.DbStore;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/** Immutable private checkpoint files. prepare -> fsync -> rename -> directory fsync -> owner DB CAS. */
public final class CheckpointArtifactsR26 {
    public static final int MAX_BYTES=32<<20;
    private static final String PREFIX="R26CAS1:";
    private static final Object FILES_LOCK=new Object();
    private static final Map<String,Integer> IN_FLIGHT=new HashMap<>();
    private static final class Usage {long bytes,reserved;int files,reservedFiles;}
    private static final Map<String,Usage> USAGE=new HashMap<>();
    private static final long ROOT_LIMIT=256L<<20;
    private static final int FILE_LIMIT=8192;
    private final File root;
    private Usage usage(){return USAGE.get(root.getPath());}
    public final class Prepared implements AutoCloseable {
        private final String hash;private boolean closed;
        private Prepared(String hash){this.hash=hash;}
        public byte[] reference(){return (PREFIX+hash).getBytes(StandardCharsets.US_ASCII);}
        public String hash(){return hash;}
        @Override public void close(){synchronized(FILES_LOCK){if(closed)return;closed=true;String key=root.getPath()+"/"+hash;int count=IN_FLIGHT.getOrDefault(key,1)-1;if(count==0)IN_FLIGHT.remove(key);else IN_FLIGHT.put(key,count);}}
    }
    public CheckpointArtifactsR26(File directory)throws IOException {
        root=directory.getCanonicalFile();
        if(!root.isDirectory()){
            if(!root.mkdirs()&&!root.isDirectory())throw new IOException("R26_ARTIFACT_DIRECTORY");
            syncDirectory(root.getParentFile());
        }
        synchronized(FILES_LOCK){if(!USAGE.containsKey(root.getPath())){Usage u=new Usage();File[] all=root.listFiles();if(all==null)throw new IOException("R26_ARTIFACT_LIST");for(File file:all)if(file.isFile()){u.bytes+=file.length();u.files++;}USAGE.put(root.getPath(),u);}}
    }
    public static CheckpointArtifactsR26 forStore(DbStore store)throws IOException {
        return new CheckpointArtifactsR26(new File(store.contextForR26().getFilesDir(),"r26-checkpoints"));
    }
    private static void syncDirectory(File directory)throws IOException {
        java.io.FileDescriptor fd=null;
        try{fd=Os.open(directory.getPath(),OsConstants.O_RDONLY,0);Os.fsync(fd);}
        catch(ErrnoException failed){throw new IOException("R26_ARTIFACT_DIRECTORY_SYNC",failed);}
        finally{if(fd!=null)try{Os.close(fd);}catch(ErrnoException ignored){}}
    }
    public static String sha(byte[] bytes){
        try{byte[] digest=MessageDigest.getInstance("SHA-256").digest(bytes);char[] hex="0123456789abcdef".toCharArray(),out=new char[64];for(int i=0;i<digest.length;i++){out[2*i]=hex[(digest[i]>>>4)&15];out[2*i+1]=hex[digest[i]&15];}return new String(out);}
        catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
    }
    public static boolean reference(byte[] value){return value!=null&&value.length==PREFIX.length()+64&&new String(value,0,PREFIX.length(),StandardCharsets.US_ASCII).equals(PREFIX);}
    private File path(String hash)throws IOException {if(hash==null||!hash.matches("[0-9a-f]{64}"))throw new IOException("R26_ARTIFACT_HASH");return new File(root,hash+".bin");}
    public Prepared prepareLease(byte[] bytes)throws IOException {
        if(bytes==null||bytes.length==0||bytes.length>MAX_BYTES)throw new IOException("R26_ARTIFACT_SIZE");String hash=sha(bytes);Prepared lease=new Prepared(hash);
        synchronized(FILES_LOCK){String key=root.getPath()+"/"+hash;IN_FLIGHT.put(key,IN_FLIGHT.getOrDefault(key,0)+1);}
        try{prepare(bytes);return lease;}catch(IOException|RuntimeException failed){lease.close();throw failed;}
    }
    /** Standalone preparation is used only by file-recovery tests; production keeps a lease through DB commit. */
    public byte[] prepare(byte[] bytes)throws IOException {
        if(bytes==null||bytes.length==0||bytes.length>MAX_BYTES)throw new IOException("R26_ARTIFACT_SIZE");
        String hash=sha(bytes);File target=path(hash);
        if(target.exists()){if(!Arrays.equals(readHash(hash),bytes))throw new IOException("R26_ARTIFACT_EXISTING_CONFLICT");syncDirectory(root);return (PREFIX+hash).getBytes(StandardCharsets.US_ASCII);}
        synchronized(FILES_LOCK){Usage u=usage();if(u.bytes+u.reserved+bytes.length>ROOT_LIMIT||u.files+u.reservedFiles>=FILE_LIMIT)throw new IOException("R26_ARTIFACT_CAPACITY_WAIT_FOR_RETENTION");u.reserved+=bytes.length;u.reservedFiles++;}
        boolean reserved=true;File temporary=new File(root,".tmp-"+UUID.randomUUID());
        try{
            try(FileOutputStream out=new FileOutputStream(temporary)){out.write(bytes);out.flush();out.getFD().sync();}
            if(!sha(readBounded(temporary)).equals(hash))throw new IOException("R26_ARTIFACT_PREPARE_HASH");
            synchronized(FILES_LOCK){Usage u=usage();if(!target.exists()){if(!temporary.renameTo(target))throw new IOException("R26_ARTIFACT_ATOMIC_RENAME");u.bytes+=bytes.length;u.files++;}u.reserved-=bytes.length;u.reservedFiles--;reserved=false;}
            if(!sha(readBounded(target)).equals(hash))throw new IOException("R26_ARTIFACT_FINAL_HASH");syncDirectory(root);
            return (PREFIX+hash).getBytes(StandardCharsets.US_ASCII);
        }finally{if(temporary.exists())temporary.delete();if(reserved)synchronized(FILES_LOCK){Usage u=usage();u.reserved-=bytes.length;u.reservedFiles--;}}
    }
    private static byte[] readBounded(File file)throws IOException {
        long length=file.length();if(length<1||length>MAX_BYTES)throw new IOException("R26_ARTIFACT_SIZE");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream((int)length);
        try(InputStream in=new FileInputStream(file)){byte[] buffer=new byte[32768];int n;while((n=in.read(buffer))!=-1){if(bytes.size()+n>MAX_BYTES)throw new IOException("R26_ARTIFACT_SIZE");ExecutionBudgetR26.poll();bytes.write(buffer,0,n);}}
        if(bytes.size()!=length)throw new IOException("R26_ARTIFACT_CHANGED_DURING_READ");return bytes.toByteArray();
    }
    public byte[] readHash(String hash)throws IOException {byte[] bytes=readBounded(path(hash));if(!sha(bytes).equals(hash))throw new IOException("R26_ARTIFACT_CORRUPT");return bytes;}
    /** Old inline SQLite checkpoints remain readable during additive migration. */
    public byte[] resolve(byte[] value)throws IOException {if(!reference(value))return value;return readHash(new String(value,PREFIX.length(),64,StandardCharsets.US_ASCII));}
    public static byte[] resolve(DbStore store,byte[] value)throws IOException {return reference(value)?forStore(store).resolve(value):value;}
    public static Prepared prepareLease(DbStore store,byte[] value)throws IOException {return forStore(store).prepareLease(value);}
    /** Called with DB references frozen by the P3 owner. New, not-yet-committed files have an age grace. */
    public long prune(Set<String> references,long now,long graceMillis,int limit)throws IOException {return prune(()->references,now,graceMillis,limit);}
    public long prune(java.util.function.Supplier<Set<String>> referenceSource,long now,long graceMillis,int limit)throws IOException {
        synchronized(FILES_LOCK){Set<String> references=referenceSource.get();File[] files=root.listFiles();if(files==null)throw new IOException("R26_ARTIFACT_LIST");Arrays.sort(files,Comparator.comparingLong(File::lastModified));long removed=0;int count=0;
        for(File file:files){if(count>=limit)break;String name=file.getName();if(now-file.lastModified()<graceMillis)continue;
            boolean temporary=name.startsWith(".tmp-");if(temporary&&IN_FLIGHT.keySet().stream().anyMatch(k->k.startsWith(root.getPath()+"/")))continue;String hash=name.endsWith(".bin")?name.substring(0,name.length()-4):"";
            if(!temporary&&(references.contains(hash)||IN_FLIGHT.containsKey(root.getPath()+"/"+hash)||!hash.matches("[0-9a-f]{64}")))continue;
            long size=file.length();if(file.delete()){removed+=size;count++;Usage u=usage();u.bytes=Math.max(0,u.bytes-size);u.files=Math.max(0,u.files-1);}}
        if(count>0)syncDirectory(root);return removed;}
    }
    public long bytes(){synchronized(FILES_LOCK){Usage u=usage();return u.bytes+u.reserved;}}
    public static String referenceHash(byte[] value){return reference(value)?new String(value,PREFIX.length(),64,StandardCharsets.US_ASCII):"";}
}
