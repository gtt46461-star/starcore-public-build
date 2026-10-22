package com.ethk3.novaonepro;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Properties;

/** Offline file protocol. Caller must hold the Coordinator maintenance owner,
 * close SQLite pools, and hold SourceReadLeaseR82's EXCLUSIVE lease throughout.
 * A retained, verified backup does not count as reclaimed disk space. */
public final class StorageMaintenanceFileR82 {
    private StorageMaintenanceFileR82() {}
    public static void recoverIfNeeded(File database)throws IOException {
        if(!journal(database).exists())return;
        System.loadLibrary("starcore_report_cp4");
        try(SourceReadLeaseR82 lease=SourceReadLeaseR82.exclusive(database)){recover(database);}
    }
    private static native String nativeRebuild(String source, String destination);
    private static native void nativeFsyncDirectory(String directory) throws IOException;
    public static final class Receipt {
        public final long originalBytes, activeBytes, backupBytes;
        public final String nativeProof, sourceSha256, activeSha256;
        Receipt(long before,long after,long backup,String proof,String oldHash,String newHash) {
            originalBytes=before;activeBytes=after;backupBytes=backup;
            nativeProof=proof;sourceSha256=oldHash;activeSha256=newHash;
        }
        public String summary() { return "COMPLETE_ACTIVE_REBUILT original_bytes="+originalBytes+
                " active_bytes="+activeBytes+" active_reduction_bytes="+Math.max(0,originalBytes-activeBytes)+
                " retained_backup_bytes="+backupBytes+" physical_free_bytes_released=0"+
                " backup_cleanup=EXPLICIT_SEPARATE_ACTION source_sha256="+sourceSha256+
                " active_sha256="+activeSha256+" native_proof="+nativeProof; }
    }
    private static File sibling(File db,String suffix) throws IOException {
        File c=db.getCanonicalFile();File f=new File(c.getParentFile(),c.getName()+suffix);
        if(!f.getCanonicalFile().equals(f.getAbsoluteFile()))throw new IOException("maintenance path is a symlink");
        return f;
    }
    private static File journal(File db)throws IOException{return sibling(db,".r82-maintenance-journal");}
    private static File candidate(File db)throws IOException{return sibling(db,".r82-rebuilt");}
    private static File backup(File db)throws IOException{return sibling(db,".r82-original-backup");}
    private static File receipt(File db)throws IOException{return sibling(db,".r82-backup-receipt");}
    private static String sha256(File f)throws IOException {
        try { MessageDigest md=MessageDigest.getInstance("SHA-256");byte[] b=new byte[1024*1024];
            try(InputStream in=new FileInputStream(f)){for(int n;(n=in.read(b))!=-1;)md.update(b,0,n);}
            StringBuilder s=new StringBuilder();for(byte v:md.digest())s.append(String.format(java.util.Locale.US,"%02x",v&255));return s.toString();
        }catch(java.security.NoSuchAlgorithmException e){throw new IOException(e);}
    }
    private static void syncParent(File f)throws IOException{nativeFsyncDirectory(f.getParentFile().getCanonicalPath());}
    private static void write(File f,Properties p)throws IOException {
        File t=new File(f.getPath()+".tmp");if(t.exists()&&!t.delete())throw new IOException("cannot clear stale journal temp");
        try(FileOutputStream out=new FileOutputStream(t)){p.store(out,"R82 durable maintenance protocol");out.getFD().sync();}
        if(!t.renameTo(f))throw new IOException("cannot install maintenance journal");syncParent(f);
    }
    private static Properties read(File f)throws IOException {Properties p=new Properties();try(InputStream in=new FileInputStream(f)){p.load(in);}return p;}
    private static void requireHash(File f,String wanted,String role)throws IOException{
        if(wanted==null||wanted.length()!=64||!f.isFile()||!sha256(f).equals(wanted))throw new IOException(role+" is missing or checksum differs");
    }
    private static void move(File from,File to)throws IOException {
        if(to.exists())throw new IOException("rename destination already exists: "+to.getName());
        if(!from.renameTo(to))throw new IOException("rename failed: "+from.getName());syncParent(to);
    }
    private static void remove(File f)throws IOException{if(f.exists()&&!f.delete())throw new IOException("cannot remove "+f.getName());syncParent(f);}
    private static void requireNoSidecars(File db)throws IOException {
        for(String ext:new String[]{"-wal","-journal"}){File f=sibling(db,ext);if(f.exists()&&f.length()!=0)throw new IOException("SQLite pool/checkpoint not quiescent: "+ext);}
    }
    private static void clearClosedSidecars(File db)throws IOException {
        requireNoSidecars(db);
        // The sole owner has checkpointed and closed every source pool, and the
        // exclusive source lease excludes report/UI readers. SHM belongs to the
        // old WAL generation and must not survive a replacement database file.
        for(String suffix:new String[]{"-wal","-shm","-journal"}){File f=sibling(db,suffix);if(f.exists())remove(f);}
    }
    /** Must run before reopening any DB helper after an interrupted maintenance. */
    public static String recover(File database)throws IOException {
        File db=database.getCanonicalFile(),j=journal(db),out=candidate(db),old=backup(db);
        if(!j.exists())return "NO_PENDING_SWAP";
        Properties p=read(j);if(!db.getPath().equals(p.getProperty("database")))throw new IOException("journal database mismatch");
        String oldHash=p.getProperty("source_sha256"),newHash=p.getProperty("candidate_sha256");
        if(!db.exists()) { requireHash(old,oldHash,"original backup");move(old,db);if(out.exists())remove(out);remove(j);return "RECOVERED_ORIGINAL_AFTER_INTERRUPTED_RENAME"; }
        String current=sha256(db);
        if(current.equals(oldHash)) {if(old.exists())throw new IOException("ambiguous duplicate original backup");if(out.exists())remove(out);remove(j);return "RECOVERED_ORIGINAL_BEFORE_SWAP";}
        if(current.equals(newHash)) {requireHash(old,oldHash,"retained original backup");p.setProperty("state","INSTALLED");write(receipt(db),p);if(out.exists())throw new IOException("duplicate candidate after installation");remove(j);return "RECOVERED_INSTALLED_VERIFIED_COPY_BACKUP_RETAINED";}
        throw new IOException("uncertain interrupted swap: preserved all files for recovery");
    }
    // Physical free bytes on the exact database filesystem are a conservative preflight.
    // Quotas/other writers can still reject I/O; verified-copy failures preserve the original.
    @SuppressWarnings("UsableSpace")
    public static Receipt rebuildAndSwap(File database)throws IOException {
        File db=database.getCanonicalFile();recover(db);File out=candidate(db),old=backup(db),j=journal(db);
        if(!db.isFile())throw new IOException("original database missing");
        if(old.exists()||receipt(db).exists())throw new IOException("retained backup must be handled before another rebuild");
        if(out.exists())throw new IOException("unowned rebuilt file exists; preserved");clearClosedSidecars(db);
        long before=db.length();long free=db.getParentFile().getUsableSpace();
        if(free<=0)throw new IOException("filesystem free-byte measurement unavailable or empty; source preserved");
        long required=before>Long.MAX_VALUE/2?Long.MAX_VALUE:before+Math.max(64L*1024*1024,before/10);
        if(free<required)throw new IOException("insufficient free space for verified offline copy: required="+required+" available="+free);
        String oldHash=sha256(db),proof=nativeRebuild(db.getPath(),out.getPath());
        if(proof==null||!proof.startsWith("OK "))throw new IOException("native rebuild rejected: "+proof);
        if(!out.isFile()||out.length()==0)throw new IOException("verified output missing");
        String newHash=sha256(out);requireHash(db,oldHash,"source changed during offline copy");requireNoSidecars(db);
        Properties p=new Properties();p.setProperty("database",db.getPath());p.setProperty("source_sha256",oldHash);
        p.setProperty("candidate_sha256",newHash);p.setProperty("original_bytes",Long.toString(before));
        p.setProperty("active_bytes",Long.toString(out.length()));p.setProperty("native_proof",proof);p.setProperty("state","PREPARED");
        write(j,p);move(db,old);p.setProperty("state","ORIGINAL_RENAMED");write(j,p);
        move(out,db);p.setProperty("state","INSTALLED");write(j,p);write(receipt(db),p);remove(j);
        return new Receipt(before,db.length(),old.length(),proof,oldHash,newHash);
    }
    /** Explicit independent user action after the newly rebuilt DB is accepted. */
    public static long deleteVerifiedBackup(File database)throws IOException {
        File db=database.getCanonicalFile();if(journal(db).exists())throw new IOException("pending swap must recover first");
        File r=receipt(db),old=backup(db);if(!r.isFile())throw new IOException("verified backup receipt missing");
        Properties p=read(r);if(!db.getPath().equals(p.getProperty("database")))throw new IOException("backup receipt database mismatch");
        requireHash(old,p.getProperty("source_sha256"),"original backup");long n=old.length();remove(old);remove(r);return n;
    }
    public static String backupSummary(File database) {
        try{File db=database.getCanonicalFile(),old=backup(db);return "retained_backup_bytes="+(old.isFile()?old.length():0)+
                " pending_swap="+journal(db).exists()+" backup_receipt="+receipt(db).isFile()+" backup_cleanup=EXPLICIT_SEPARATE_ACTION";}
        catch(IOException e){return "BACKUP_STATE_UNKNOWN cause="+e.getClass().getSimpleName();}
    }
}
