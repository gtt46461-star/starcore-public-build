package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.DirectoryCursorR31;
import java.io.*;
import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/** Strict snapshot closure with a private, disposable disk index. No business
 * database is opened for writing. RAM is bounded by one manifest and a queue
 * page; the number of historical objects does not become an admission limit.
 * The index is never reused after a crash and never authorizes garbage collection.
 */
public final class CheckpointClosureR324 implements AutoCloseable {
    private static final int PAGE_BYTES=4096,MAX_MANIFEST=4096*80+64,QUEUE_PAGE=64;
    private static final long FREE_RESERVE=64L<<20;
    public interface RootSource { void visit(Consumer<String> sink)throws Exception; }
    public interface Visitor { void verified(String hash,long bytes)throws Exception; }
    public interface GrowthAdmission { void beforeGrowth(File index,long maximumBytes)throws IOException; }
    public static final class Result {
        public final long objects,bytes;
        Result(long objects,long bytes){this.objects=objects;this.bytes=bytes;}
    }
    private final File directory,database,root;
    private final BooleanSupplier continuing;
    private final GrowthAdmission admission;
    private SQLiteDatabase index;
    private long capacityPages;
    private int writesSinceMeasure;
    private boolean traversed,closed;

    public CheckpointClosureR324(File workspace,File checkpointRoot,BooleanSupplier continuing,GrowthAdmission admission)throws IOException {
        this.continuing=Objects.requireNonNull(continuing);this.admission=Objects.requireNonNull(admission);
        File parent=canonical(workspace);if(!parent.isDirectory())throw new IOException("R324_CLOSURE_WORKSPACE_MISSING");
        root=canonical(checkpointRoot);directory=canonical(new File(parent,"checkpoint-closure-r324-"+UUID.randomUUID()));
        if(!directory.mkdir())throw new IOException("R324_CLOSURE_EXCLUSIVE_DIRECTORY");database=new File(directory,"index.sqlite");
        try{
            check();grow(1024); // Explicit four MiB physical ceiling before opening the scratch database.
            index=SQLiteDatabase.openDatabase(database.getPath(),null,SQLiteDatabase.CREATE_IF_NECESSARY|SQLiteDatabase.NO_LOCALIZED_COLLATORS);
            configure("PRAGMA journal_mode=OFF");index.execSQL("PRAGMA synchronous=OFF");index.execSQL("PRAGMA temp_store=FILE");index.execSQL("PRAGMA cache_size=-256");
            index.execSQL("PRAGMA page_size="+PAGE_BYTES);limitPages();
            index.execSQL("CREATE TABLE seen(hash TEXT PRIMARY KEY,done INTEGER NOT NULL DEFAULT 0)");
            index.execSQL("CREATE INDEX pending ON seen(done,hash)");
        }catch(IOException|RuntimeException failed){try{close();}catch(IOException cleanup){failed.addSuppressed(cleanup);}throw failed;}
    }
    private static File canonical(File file)throws IOException{File absolute=file.getAbsoluteFile();if(!absolute.equals(absolute.getCanonicalFile()))throw new IOException("R324_CLOSURE_SYMLINK");return absolute;}
    private void check()throws InterruptedIOException{if(Thread.currentThread().isInterrupted()||!continuing.getAsBoolean())throw new InterruptedIOException("R324_CLOSURE_CANCELLED");}
    private void configure(String sql){try(Cursor c=index.rawQuery(sql,null)){c.moveToFirst();}}
    private long pragma(String sql){try(Cursor c=index.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
    private void limitPages()throws IOException {long actual=pragma("PRAGMA max_page_count="+capacityPages);if(actual!=capacityPages)throw new IOException("R324_CLOSURE_INDEX_CEILING");}
    // Admit only currently usable physical bytes; reclaimable cache is not an allocation or released reserve.
    @android.annotation.SuppressLint("UsableSpace")
    private void grow(long pages)throws IOException {
        check();long extra=Math.multiplyExact(pages-capacityPages,(long)PAGE_BYTES);if(extra<=0)return;
        // The index has no rollback/WAL sidecars (journal_mode=OFF). Its bounded
        // envelope includes one filesystem slab beyond SQLite's own page ceiling.
        long envelope=Math.addExact(extra,65536);if(directory.getUsableSpace()<Math.addExact(envelope,FREE_RESERVE))throw new IOException("R324_CLOSURE_DISK_CAPACITY");
        admission.beforeGrowth(database,envelope);capacityPages=pages;
    }
    private void beforeWrite()throws IOException {
        check();if(writesSinceMeasure++%32!=0)return;
        long used=pragma("PRAGMA page_count");if(used<0||used>capacityPages)throw new IOException("R324_CLOSURE_INDEX_CAPACITY_CHANGED");
        if(capacityPages-used<512){grow(Math.addExact(capacityPages,512));limitPages();}
    }
    private void add(String hash)throws IOException {
        if(hash==null||!hash.matches("[0-9a-f]{64}"))throw new IOException("R324_CLOSURE_HASH");beforeWrite();
        index.execSQL("INSERT OR IGNORE INTO seen(hash,done) VALUES(?,0)",new Object[]{hash});
    }
    public Result traverse(RootSource roots,Visitor visitor)throws Exception {
        if(closed||traversed)throw new IOException("R324_CLOSURE_SINGLE_ATTEMPT");traversed=true;
        try{roots.visit(hash->{try{add(hash);}catch(IOException failed){throw new UncheckedIOException(failed);}});}
        catch(UncheckedIOException failed){throw failed.getCause();}
        long objects=0,bytes=0;
        for(;;){check();List<String> page=new ArrayList<>(QUEUE_PAGE);
            try(Cursor c=index.rawQuery("SELECT hash FROM seen WHERE done=0 ORDER BY hash LIMIT 64",null)){while(c.moveToNext())page.add(c.getString(0));}
            if(page.isEmpty())break;
            for(String hash:page){check();long size=inspect(hash);visitor.verified(hash,size);
                beforeWrite();index.execSQL("UPDATE seen SET done=1 WHERE hash=? AND done=0",new Object[]{hash});
                objects=Math.addExact(objects,1);bytes=Math.addExact(bytes,size);
            }
        }
        return new Result(objects,bytes);
    }
    private long inspect(String hash)throws IOException {
        long expected=ColdCheckpointR324.rawSize(root,hash);if(expected<1||expected>ColdCheckpointR324.MAX_RAW)throw new IOException("R324_CLOSURE_OBJECT_SIZE");
        try(MemoryBudgetR31.Lease memory=CheckpointArtifactsR26.readReservationR31("complete-closure",3L*MAX_MANIFEST+65536);
            InputStream in=ColdCheckpointR324.open(root,hash)){
            byte[] header=new byte[4],buffer=new byte[32768];int head=0;long count=0;
            while(head<4){check();int n=in.read(header,head,4-head);if(n<0)break;head+=n;count+=n;}
            boolean manifest=head==4&&ChunkedCheckpointR26.isManifest(header);
            if(!manifest&&head>=3&&header[0]==0x26&&header[1]==0x43&&header[2]==0x53)throw new IOException("R324_CLOSURE_UNKNOWN_MANIFEST_VERSION");
            if(manifest&&expected>MAX_MANIFEST)throw new IOException("R324_CLOSURE_MANIFEST_BOUND");
            ByteArrayOutputStream bytes=manifest?new ByteArrayOutputStream((int)expected):null;if(bytes!=null)bytes.write(header,0,head);
            int n;while((n=in.read(buffer))!=-1){check();count=Math.addExact(count,n);if(count>expected)throw new IOException("R324_CLOSURE_OBJECT_GREW");if(bytes!=null)bytes.write(buffer,0,n);}
            // Cold.open verifies original SHA and length at EOF; the explicit
            // count protects against changed hot/cold metadata as well.
            if(count!=expected||ColdCheckpointR324.rawSize(root,hash)!=expected)throw new IOException("R324_CLOSURE_OBJECT_CHANGED");
            if(bytes!=null)for(String child:ChunkedCheckpointR26.children(bytes.toByteArray()))add(child);
            return expected;
        }
    }
    @Override public void close()throws IOException {
        if(closed)return;closed=true;RuntimeException failure=null;
        if(index!=null)try{index.close();}catch(RuntimeException failed){failure=failed;}finally{index=null;}
        try{remove(directory,0);}catch(IOException cleanup){if(failure!=null)cleanup.addSuppressed(failure);throw cleanup;}
        if(failure!=null)throw new IOException("R324_CLOSURE_CLOSE",failure);
    }
    private static void remove(File file,int depth)throws IOException {
        canonical(file);if(depth>3)throw new IOException("R324_CLOSURE_CLEANUP_DEPTH");if(!file.exists())return;
        if(file.isDirectory())try(DirectoryCursorR31 children=DirectoryCursorR31.open(file)){File child;while((child=children.next())!=null)remove(child,depth+1);}
        if(!file.delete())throw new IOException("R324_CLOSURE_CLEANUP_FAILED");
    }
}
