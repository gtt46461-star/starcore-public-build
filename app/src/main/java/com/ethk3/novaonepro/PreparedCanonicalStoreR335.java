package com.ethk3.novaonepro;

import java.io.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import android.util.AtomicFile;

/** Bounded retry cache for an exact, already computed prediction. It never creates a decision.
 * The request-derived path is the durable pointer: no cross-store pointer transaction is needed.
 * Only the sole canonical transaction may publish it. Expired derived caches are disposable.
 */
public final class PreparedCanonicalStoreR335 {
    private static final int MAGIC=0x50333335, MAX_FILE=16*1024*1024, MAX_SLOTS=4;
    private static final String DIRECTORY="r335-prepared-canonical";
    private PreparedCanonicalStoreR335(){}
    public static final class Prepared {
        public final V51RuntimeResult result; public final byte[] snapshot; public final String snapshotHash;
        public final long deadlineMono; public final String storedBoot;
        Prepared(V51RuntimeResult r,byte[] bytes,String hash,long mono,String boot){result=r;snapshot=bytes;snapshotHash=hash;deadlineMono=mono;storedBoot=boot;}
        void requireReusable(AnalysisSingleFlightR36.Request r)throws IOException {
            if(!storedBoot.equals(boot()))throw new IOException("R335_PREPARED_BOOT_CHANGED");
            if(deadlineMono<=0||TimeSourceR811.monoNowMs()>=deadlineMono||System.currentTimeMillis()>=r.deadlineAt)throw new IOException("R335_PREPARED_EXPIRED");
        }
    }
    private static String boot(){return com.ethk3.novaonepro.strategyv2.IssueTimelineWriterR30.bootId();}
    private static File directory(DbStore store,boolean create)throws IOException {
        if(store.contextForR26()==null)throw new IOException("R335_PREPARED_CONTEXT");
        File files=store.contextForR26().getFilesDir().getCanonicalFile();File dir=new File(files,DIRECTORY);
        if(!dir.getAbsoluteFile().equals(dir.getCanonicalFile()))throw new IOException("R335_PREPARED_SYMLINK");
        if(!dir.exists()&&!create)return dir;
        if(!dir.isDirectory()&&(!create||!dir.mkdirs()))throw new IOException("R335_PREPARED_DIRECTORY");return dir;
    }
    private static String name(AnalysisSingleFlightR36.Request r)throws IOException {
        if(!r.valid()||!r.requestHash.matches("[0-9a-f]{64}")||r.generation<1||r.deadlineAt<=0)throw new IOException("R335_PREPARED_REQUEST");
        return r.deadlineAt+"-"+r.requestHash+"-"+r.generation+".bin";
    }
    private static final class Lock implements Closeable {
        final RandomAccessFile file;final FileLock lock;
        Lock(File dir)throws IOException {
            File path=new File(dir,"owner.lock");if(!path.getCanonicalFile().equals(path.getAbsoluteFile()))throw new IOException("R335_PREPARED_LOCK_SYMLINK");
            file=new RandomAccessFile(path,"rw");FileLock acquired=null;
            try{acquired=file.getChannel().tryLock();}catch(OverlappingFileLockException busy){/* typed dependency below */}
            catch(IOException|RuntimeException failure){file.close();throw failure;}
            if(acquired==null){file.close();throw new StorageBudgetR31.Rejected("OWNER_LEASE_BUSY").context("r335-prepared",0,"LOCK");}lock=acquired;
        }
        public void close()throws IOException{try{lock.release();}finally{file.close();}}
    }
    private static void expire(File dir,String keep)throws IOException {
        File[] entries=dir.listFiles();if(entries==null)throw new IOException("R335_PREPARED_LIST");
        if(entries.length>32)throw new IOException("R335_PREPARED_UNEXPECTED_ENTRIES");int slots=0;long now=System.currentTimeMillis();
        for(File f:entries){String n=f.getName();if(!f.getCanonicalFile().equals(f.getAbsoluteFile()))throw new IOException("R335_PREPARED_SYMLINK");
            if(!n.matches("[0-9]{1,19}-[0-9a-f]{64}-[0-9]+\\.bin(?:\\.bak|\\.new)?"))continue;
            long deadline;try{deadline=Long.parseLong(n.substring(0,n.indexOf('-')));}catch(NumberFormatException bad){throw new IOException("R335_PREPARED_NAME",bad);}
            if(deadline<=now&&!n.startsWith(keep)){if(!f.delete()&&f.exists())throw new IOException("R335_PREPARED_EXPIRE");}
            else if(n.endsWith(".bin")||n.endsWith(".bak"))slots++;
        }
        if(slots>=MAX_SLOTS&&!new File(dir,keep).exists()&&!new File(dir,keep+".bak").exists())throw new StorageBudgetR31.Rejected("SCOPE_ENTRY_BUDGET").context("r335-prepared",0,"SLOT");
    }
    static byte[] checkedBody(byte[] file)throws IOException {
        if(file==null||file.length<40||file.length>MAX_FILE)throw new IOException("R335_PREPARED_SIZE");
        byte[] body=Arrays.copyOf(file,file.length-32),expected=Arrays.copyOfRange(file,file.length-32,file.length);
        try{if(!java.security.MessageDigest.isEqual(expected,java.security.MessageDigest.getInstance("SHA-256").digest(body)))throw new IOException("R335_PREPARED_SHA256");}
        catch(java.security.NoSuchAlgorithmException impossible){throw new IOException(impossible);}return body;
    }
    private static Prepared decode(DataInputStream in,AnalysisSingleFlightR36.Request r)throws IOException {
        if(in.readInt()!=MAGIC)throw new IOException("R335_PREPARED_MAGIC");String storedBoot=in.readUTF();
        if(!in.readUTF().equals(r.requestHash)||in.readLong()!=r.generation||!in.readUTF().equals(r.baseIssue)||!in.readUTF().equals(r.targetIssue)
                ||!in.readUTF().equals(r.historyHash)||in.readLong()!=r.requestAt||in.readLong()!=r.deadlineAt)
            throw new IOException("R335_PREPARED_IDENTITY");
        long originalMono=in.readLong();
        byte[] resultBytes=part(in,PreparedCanonicalCodecR335.MAX_BYTES);byte[] snapshot=part(in,MAX_FILE);
        String snapshotHash=in.readUTF();if(in.read()!=-1||!snapshotHash.equals(DeferredEvidenceSnapshotR36.sha256(snapshot)))throw new IOException("R335_PREPARED_SNAPSHOT_HASH");
        V51RuntimeResult result=PreparedCanonicalCodecR335.decode(resultBytes);
        if(result.context==null||!result.context.baseIssue.equals(r.baseIssue)||!result.context.targetIssue.equals(r.targetIssue)||!result.context.historyHash.equals(r.historyHash))throw new IOException("R335_PREPARED_RESULT_CONTEXT");
        return new Prepared(result,snapshot,snapshotHash,originalMono,storedBoot);
    }
    /** A checked envelope is split in a second streaming pass, without full-file/body copies. */
    private static Prepared readPrepared(AtomicFile file,AnalysisSingleFlightR36.Request r)throws IOException {
        byte[] expected=verifyFileHash(file);
        try(FileInputStream raw=file.openRead()){
            long length=raw.getChannel().size()-32;if(length<8||length>MAX_FILE-32)throw new IOException("R335_PREPARED_SIZE");
            java.security.MessageDigest digest=sha256();final long[] left={length};
            InputStream bounded=new InputStream(){
                public int read()throws IOException{if(left[0]==0)return -1;int value=raw.read();if(value<0)throw new EOFException();left[0]--;digest.update((byte)value);return value;}
                public int read(byte[] b,int off,int count)throws IOException{if(count==0)return 0;if(left[0]==0)return -1;int n=raw.read(b,off,(int)Math.min(count,left[0]));if(n<1)throw new EOFException();left[0]-=n;digest.update(b,off,n);return n;}
                public int available(){return (int)left[0];}
            };
            Prepared prepared=decode(new DataInputStream(bounded),r);byte[] footer=new byte[32];new DataInputStream(raw).readFully(footer);
            if(raw.read()!=-1||!java.security.MessageDigest.isEqual(expected,footer)||!java.security.MessageDigest.isEqual(expected,digest.digest()))throw new IOException("R335_PREPARED_CHANGED_DURING_READ");
            return prepared;
        }
    }
    private static byte[] part(DataInputStream in,int maximum)throws IOException {
        int n=in.readInt();if(n<1||n>maximum||n>in.available())throw new IOException("R335_PREPARED_PART_BOUND");byte[] b=new byte[n];in.readFully(b);return b;
    }
    public static Prepared load(DbStore store,AnalysisSingleFlightR36.Request r)throws IOException {
        File dir=directory(store,false);String n=name(r);File f=new File(dir,n);
        // A request without a retry cache requires no new directory, lock file or writable storage.
        if(!dir.exists()||(!f.exists()&&!new File(dir,n+".bak").exists()))return null;
        try(Lock lock=new Lock(dir)){
            if(!f.getCanonicalFile().equals(f.getAbsoluteFile()))throw new IOException("R335_PREPARED_SYMLINK");
            return readPrepared(new AtomicFile(f),r);
        }
    }
    public static void save(DbStore store,AnalysisSingleFlightR36.Request r,V51RuntimeResult result,DeferredEvidenceSnapshotR36.EncodedSnapshot snapshot)throws IOException {
        if(snapshot==null||snapshot.bytes==null)throw new IOException("R335_PREPARED_SNAPSHOT_REQUIRED");
        byte[] resultBytes=PreparedCanonicalCodecR335.encode(result);
        if((long)resultBytes.length+snapshot.bytes.length>MAX_FILE-4096L)throw new IOException("R335_PREPARED_FILE_BOUND");
        if(!snapshot.sha256.equals(DeferredEvidenceSnapshotR36.sha256(snapshot.bytes)))throw new IOException("R335_PREPARED_SNAPSHOT_HASH");
        if(result.context==null||!r.baseIssue.equals(result.context.baseIssue)||!r.targetIssue.equals(result.context.targetIssue)||!r.historyHash.equals(result.context.historyHash))throw new IOException("R335_PREPARED_RESULT_CONTEXT");
        File dir=directory(store,true);String n=name(r);File f=new File(dir,n);
        try(Lock lock=new Lock(dir)){
            expire(dir,n);AtomicFile atomic=new AtomicFile(f);
            if(f.exists()||new File(dir,n+".bak").exists()){
                Prepared existing=readPrepared(atomic,r);
                if(!existing.snapshotHash.equals(snapshot.sha256)||!Arrays.equals(resultBytes,PreparedCanonicalCodecR335.encode(existing.result)))throw new IOException("R335_PREPARED_IDENTITY_CONFLICT");return;
            }
            File files=store.contextForR26().getFilesDir().getCanonicalFile();StorageBudgetR31 budget=StorageBudgetR31.forFiles(files);
            long bound=(long)resultBytes.length+snapshot.bytes.length+4096L;
            try(StorageBudgetR31.Reservation grant=budget.reserve("r335-prepared:"+r.requestHash,r.generation,StorageBudgetR31.Category.CACHE,"files/"+DIRECTORY,bound,bound,true)){
                grant.beforeChunk(bound);FileOutputStream sink=null;byte[] expected;
                try{
                    if(TimeSourceR811.monoNowMs()>=r.deadlineMonoAt||System.currentTimeMillis()>=r.deadlineAt)throw new IOException("R335_PREPARED_EXPIRED");
                    sink=atomic.startWrite();java.security.MessageDigest digest=sha256();
                    DataOutputStream out=new DataOutputStream(new java.security.DigestOutputStream(sink,digest));
                    out.writeInt(MAGIC);out.writeUTF(boot());out.writeUTF(r.requestHash);out.writeLong(r.generation);
                    out.writeUTF(r.baseIssue);out.writeUTF(r.targetIssue);out.writeUTF(r.historyHash);out.writeLong(r.requestAt);out.writeLong(r.deadlineAt);out.writeLong(r.deadlineMonoAt);
                    out.writeInt(resultBytes.length);out.write(resultBytes);out.writeInt(snapshot.bytes.length);out.write(snapshot.bytes);out.writeUTF(snapshot.sha256);out.flush();
                    expected=digest.digest();sink.write(expected);sink.getFD().sync();atomic.finishWrite(sink);sink=null;syncDirectory(dir);
                } finally{if(sink!=null)atomic.failWrite(sink);}
                if(!java.security.MessageDigest.isEqual(expected,verifyFileHash(atomic)))throw new IOException("R335_PREPARED_READBACK");
            }
        }
    }
    private static java.security.MessageDigest sha256()throws IOException {
        try{return java.security.MessageDigest.getInstance("SHA-256");}catch(java.security.NoSuchAlgorithmException impossible){throw new IOException(impossible);}
    }
    /** Readback uses a fixed 16 KiB buffer; it does not materialize a second model graph. */
    private static byte[] verifyFileHash(AtomicFile file)throws IOException {
        try(FileInputStream in=file.openRead()){
            long remaining=in.getChannel().size()-32;if(remaining<8||remaining>MAX_FILE-32)throw new IOException("R335_PREPARED_SIZE");
            java.security.MessageDigest digest=sha256();byte[] block=new byte[16384];
            while(remaining>0){if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("R335_PREPARED_CANCELLED");int n=in.read(block,0,(int)Math.min(block.length,remaining));if(n<1)throw new EOFException("R335_PREPARED_SHORT");digest.update(block,0,n);remaining-=n;}
            byte[] footer=new byte[32];new DataInputStream(in).readFully(footer);
            byte[] actual=digest.digest();if(in.read()!=-1||!java.security.MessageDigest.isEqual(footer,actual))throw new IOException("R335_PREPARED_SHA256");return actual;
        }
    }
    private static void syncDirectory(File dir)throws IOException {
        java.io.FileDescriptor fd=null;try{fd=android.system.Os.open(dir.getPath(),android.system.OsConstants.O_RDONLY,0);android.system.Os.fsync(fd);}
        catch(android.system.ErrnoException failure){throw new IOException("R335_PREPARED_DIRECTORY_SYNC",failure);}
        finally{if(fd!=null)try{android.system.Os.close(fd);}catch(android.system.ErrnoException failure){throw new IOException("R335_PREPARED_DIRECTORY_CLOSE",failure);}}
    }
    /** Once original canonical durability is established, cleanup failure cannot undo that commit. */
    public static void clearAfterCommit(final DbStore store,final AnalysisSingleFlightR36.Request r) {
        // Queue saturation leaves an expired derived cache for the next bounded sweep; no commit is lost.
        PostCanonicalObservationR33.offer(()->{
            try{File dir=directory(store,false);String n=name(r);
                if(!dir.exists()||(!new File(dir,n).exists()&&!new File(dir,n+".bak").exists()&&!new File(dir,n+".new").exists()))return;
                try(Lock lock=new Lock(dir)){new AtomicFile(new File(dir,n)).delete();syncDirectory(dir);}}
            catch(IOException failure){CrashRecorder.recordCaught(store.contextForR26(),failure,"R335_PREPARED_POST_COMMIT_CLEANUP");}
        });
    }
}
