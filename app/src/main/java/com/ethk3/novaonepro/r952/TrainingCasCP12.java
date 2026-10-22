package com.ethk3.novaonepro.r952;

import java.io.*;
import java.nio.channels.*;
import java.security.*;
import java.util.*;
import com.ethk3.novaonepro.*;

/** Private immutable CAS representation. This class has no database write authority.
 * All publishers/readers retain a shared publication lock until their caller commits.
 * Retention must obtain the exclusive lock before removing any unreferenced CAS file. */
public final class TrainingCasCP12 {
    private TrainingCasCP12() {}
    public static final String INPUT_SUFFIX=".scdev",BOUND_SUFFIX=".scbind";
    public static final long DISK_LIMIT_CP12=128L<<20;
    public static final class LockedFile implements AutoCloseable {
        public final File file;public final String hash;public final long bytes;
        private final PublicationLease publication;
        private boolean closed;
        private LockedFile(File root,String hash,String suffix)throws IOException {
            need(hash!=null&&hash.matches("[0-9a-f]{64}")&&(suffix.equals(INPUT_SUFFIX)||suffix.equals(BOUND_SUFFIX)),"TRAIN_CAS_NAME");
            File canonical=root.getCanonicalFile();file=new File(canonical,hash+suffix).getCanonicalFile();
            need(canonical.equals(file.getParentFile()),"TRAIN_CAS_PATH");this.hash=hash;
            publication=retain(canonical);
            try {need(file.isFile()&&file.length()>0&&file.length()<=DevelopmentInputCP9.MAX_BYTES,"TRAIN_CAS_MISSING");
                bytes=file.length();verify(file,hash);
            }catch(IOException|RuntimeException|Error error){publication.close();throw error;}
        }
        public synchronized void requireOpen(){need(!closed,"TRAIN_CAS_CLOSED");}
        public synchronized InputStream open()throws IOException {requireOpen();return new FileInputStream(file);}
        public synchronized void close()throws IOException {if(!closed){closed=true;publication.close();}}
    }
    public static LockedFile open(File root,String hash,String suffix)throws IOException{return new LockedFile(root,hash,suffix);}
    public interface Producer { String write(FileOutputStream output)throws IOException; }
    /** fsync -> digest verification -> same-directory atomic rename -> directory fsync.
     * The publication shared lock is acquired before exposing the resulting file. */
    public static synchronized LockedFile publish(File root,String suffix,String expectedHash,long expectedBytes,Producer producer)throws IOException {
        need(root!=null&&producer!=null&&expectedHash!=null&&expectedHash.matches("[0-9a-f]{64}")&&expectedBytes>0&&expectedBytes<=DevelopmentInputCP9.MAX_BYTES&&(suffix.equals(INPUT_SUFFIX)||suffix.equals(BOUND_SUFFIX)),"TRAIN_CAS_PUBLISH");
        File canonical=root.getCanonicalFile();if(!canonical.isDirectory()&&!canonical.mkdirs())throw new IOException("TRAIN_CAS_DIRECTORY");
        File partial=new File(canonical,"pending-"+UUID.randomUUID()+".partial");String hash;
        try(PublicationLease live=retain(canonical);
            RandomAccessFile admission=new RandomAccessFile(new File(canonical,"admission.lock"),"rw");
            FileLock admissionLock=admission.getChannel().lock()) {
            // Publication lock preserves file lifetime; the separate exclusive admission lock
            // makes the 128MiB physical CAS check atomic across publishers/processes.
            long existingBytes=com.ethk3.novaonepro.ReportNativeCP4.trainingCasBytesCP12(canonical.getPath(),System.currentTimeMillis(),true);
            File target=new File(canonical,expectedHash+suffix);
            if(target.exists()){
                need(target.isFile()&&target.length()==expectedBytes,"TRAIN_CAS_EXISTING_SIZE");verify(target,expectedHash);
                return new LockedFile(canonical,expectedHash,suffix); // no partial / disk charge on replay.
            }
            if(existingBytes>DISK_LIMIT_CP12-expectedBytes)throw new DevelopmentProductionCP12.Wait("TRAINING_INPUT_STORAGE_CAPACITY");
            try(FileOutputStream output=new BoundedOutputCP12(partial,expectedBytes)) {hash=producer.write(output);output.flush();output.getFD().sync();}
            need(expectedHash.equals(hash)&&partial.length()==expectedBytes,"TRAIN_CAS_PUBLISH_IDENTITY");verify(partial,expectedHash);
            if(!partial.renameTo(target))throw new IOException("TRAIN_CAS_RENAME");
            fsyncDirectory(canonical);
            return new LockedFile(canonical,expectedHash,suffix);
        }finally{if(partial.exists()&&!partial.delete())partial.deleteOnExit();}
    }
    private static final class BoundedOutputCP12 extends FileOutputStream {
        long bytes;final long declared;BoundedOutputCP12(File file,long declared)throws IOException{super(file);this.declared=declared;}
        private void charge(int n){if(n<0||bytes>declared-n)throw new IllegalArgumentException("TRAIN_CAS_PREDECLARED_BYTES");bytes+=n;}
        @Override public void write(int b)throws IOException{charge(1);super.write(b);}
        @Override public void write(byte[] b)throws IOException{charge(b.length);super.write(b,0,b.length);}
        @Override public void write(byte[] b,int off,int n)throws IOException{charge(n);super.write(b,off,n);}
    }
    /** API21 native readdir: bounded direct-child scan, no Java directory array.
     * Unknown/symlink/special entries fail; all hashCAS/partial physical bytes count. */
    private static long diskBytesCP12(File root)throws IOException {
        return com.ethk3.novaonepro.ReportNativeCP4.trainingCasBytesCP12(root.getCanonicalPath(),System.currentTimeMillis(),false);
    }
    private static final Map<String,SharedPublication> SHARED=new HashMap<>();
    private static final class SharedPublication {
        final RandomAccessFile file;final FileLock lock;int references;
        SharedPublication(File root)throws IOException {file=new RandomAccessFile(new File(root,"publication.lock"),"rw");
            try{lock=file.getChannel().lock(0L,Long.MAX_VALUE,true);}catch(IOException|RuntimeException e){file.close();throw e;}}
    }
    private static final class PublicationLease implements AutoCloseable {
        final String root;private boolean closed;PublicationLease(String root){this.root=root;}
        public void close()throws IOException {synchronized(TrainingCasCP12.class){if(closed)return;closed=true;
            SharedPublication p=SHARED.get(root);if(p==null||p.references<1)throw new IllegalStateException("TRAIN_CAS_LOCK_ACCOUNTING");
            if(--p.references==0){SHARED.remove(root);try{p.lock.release();}finally{p.file.close();}}}}
    }
    private static synchronized PublicationLease retain(File root)throws IOException {
        String key=root.getCanonicalPath();SharedPublication live=SHARED.get(key);
        if(live==null){live=new SharedPublication(root);SHARED.put(key,live);}live.references++;return new PublicationLease(key);
    }
    static void fsyncDirectory(File directory)throws IOException {
        try {java.io.FileDescriptor fd=android.system.Os.open(directory.getPath(),android.system.OsConstants.O_RDONLY,0);
            try{android.system.Os.fsync(fd);}finally{android.system.Os.close(fd);}
        }catch(android.system.ErrnoException e){throw new IOException("TRAIN_CAS_DIRECTORY_FSYNC",e);}
    }
    public static void verify(File file,String expected)throws IOException {
        MessageDigest digest=sha();long n=0;
        try(InputStream input=new FileInputStream(file)){byte[] page=new byte[32768];int read;
            while((read=input.read(page))!=-1){n+=read;if(n>DevelopmentInputCP9.MAX_BYTES)throw new IOException("TRAIN_CAS_LIMIT");digest.update(page,0,read);}}
        need(n>0&&hex(digest.digest()).equals(expected),"TRAIN_CAS_HASH");
    }
    static MessageDigest sha(){try{return MessageDigest.getInstance("SHA-256");}catch(NoSuchAlgorithmException e){throw new AssertionError(e);}}
    static String hex(byte[] bytes){StringBuilder b=new StringBuilder(64);for(byte v:bytes)b.append(Character.forDigit((v&255)>>>4,16)).append(Character.forDigit(v&15,16));return b.toString();}
    static void need(boolean ok,String reason){if(!ok)throw new IllegalArgumentException(reason);}
}
