package com.ethk3.novaonepro.r952;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.Arrays;
import java.util.UUID;

/** Immutable, bounded local research projection. It never owns a database writer. */
final class ResearchSnapshotFileR82 {
    static final int MAX_ROWS = 131072;
    static final int MAX_ROW_BYTES = 128 * 1024;
    static final long MAX_FILE_BYTES = 64L * 1024L * 1024L;
    static final long MAX_TOTAL_BYTES = 96L * 1024L * 1024L;
    private static final int MAGIC = 0x52393532;
    private static final int VERSION = 1;
    private static final String PREFIX = "r952-snapshot-";

    static final class Record {
        final String page, key, title, detail;
        Record(String page, String key, String title, String detail) {
            this.page = nonNull(page); this.key = nonNull(key);
            this.title = nonNull(title); this.detail = nonNull(detail);
        }
    }

    interface Cancellation { void check(); }

    static final class Writer implements Closeable {
        private final File pending;
        private final FileOutputStream raw;
        private final DataOutputStream out;
        private final MessageDigest digest;
        private final File pinFile;
        private final RandomAccessFile pin;
        private final FileLock pinLock;
        private final RandomAccessFile quota;
        private final FileLock quotaLock;
        private final long otherBytes;
        private long[] offsets = new long[4096];
        private int count;
        private long bytes = 8;
        private boolean closed, transferred;
        private File published;

        Writer(File directory) throws Exception { this(directory,null); }
        Writer(File directory,Cancellation cancellation) throws Exception {
            if (directory == null) throw new IOException("SNAPSHOT_CACHE_DIR_REQUIRED");
            if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("SNAPSHOT_CACHE_DIR_UNAVAILABLE");
            if (!directory.isDirectory()) throw new IOException("SNAPSHOT_CACHE_DIR_UNAVAILABLE");
            quota = new RandomAccessFile(new File(directory,"r952-cache-quota.lock"),"rw");
            FileLock quotaHeld = null;
            try {
                while (quotaHeld == null) {
                    if(cancellation!=null)cancellation.check();
                    if (Thread.currentThread().isInterrupted()) throw new IOException("SNAPSHOT_BUILD_INTERRUPTED");
                    try { quotaHeld = quota.getChannel().tryLock(); }
                    catch (OverlappingFileLockException busy) { }
                    if (quotaHeld == null) try { Thread.sleep(10L); }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IOException("SNAPSHOT_BUILD_INTERRUPTED",e); }
                }
            } catch (Exception failure) { quota.close(); throw failure; }
            quotaLock = quotaHeld;
            cleanupOrphansLocked(directory);
            long occupied=0L;
            File[] contents=directory.listFiles();
            if(contents==null) { quotaLock.release();quota.close();throw new IOException("SNAPSHOT_CACHE_LIST_FAILED"); }
            for(File entry:contents) if(isData(entry)) occupied+=entry.length();
            otherBytes=occupied;
            pending = new File(directory, PREFIX + UUID.randomUUID().toString() + ".pending");
            pinFile = new File(directory,pending.getName()+".pin");
            FileOutputStream opened = null; RandomAccessFile openedPin=null; FileLock heldPin=null;
            try {
                if (!pending.createNewFile()) throw new IOException("SNAPSHOT_FILE_COLLISION");
                openedPin=new RandomAccessFile(pinFile,"rw");heldPin=openedPin.getChannel().lock();
                pin=openedPin;pinLock=heldPin;
                opened = new FileOutputStream(pending, false);
                raw = opened;
                out = new DataOutputStream(opened);
                digest = MessageDigest.getInstance("SHA-256");
                if(otherBytes+8L>MAX_TOTAL_BYTES)throw new IOException("SNAPSHOT_CACHE_QUOTA_EXCEEDED");
                out.writeInt(MAGIC); out.writeInt(VERSION);
            } catch (Exception e) {
                if (opened != null) try { opened.close(); } catch (IOException ignored) { }
                if(heldPin!=null)try{heldPin.release();}catch(IOException ignored){}
                if(openedPin!=null)try{openedPin.close();}catch(IOException ignored){}
                pending.delete();pinFile.delete();quotaLock.release();quota.close();throw e;
            }
        }

        void append(Record record) throws Exception {
            if (closed) throw new IOException("SNAPSHOT_WRITER_CLOSED");
            if (count >= MAX_ROWS) throw new IOException("SNAPSHOT_ROW_LIMIT_EXCEEDED");
            byte[] data = encode(record);
            if (data.length > MAX_ROW_BYTES) throw new IOException("SNAPSHOT_ROW_TOO_LARGE");
            if (bytes + 4L + data.length > MAX_FILE_BYTES) throw new IOException("SNAPSHOT_BYTE_LIMIT_EXCEEDED");
            if (otherBytes + bytes + 4L + data.length > MAX_TOTAL_BYTES) throw new IOException("SNAPSHOT_CACHE_QUOTA_EXCEEDED");
            if (count == offsets.length) offsets = Arrays.copyOf(offsets, Math.min(MAX_ROWS, offsets.length * 2));
            offsets[count++] = bytes;
            out.writeInt(data.length); out.write(data);
            digest.update(new byte[]{(byte)(data.length >>> 24),(byte)(data.length >>> 16),(byte)(data.length >>> 8),(byte)data.length});
            digest.update(data); bytes += 4L + data.length;
        }

        Handle finish() throws Exception {
            if (closed) throw new IOException("SNAPSHOT_WRITER_CLOSED");
            out.flush(); raw.getFD().sync(); out.close(); closed = true;
            File target = new File(pending.getParentFile(), pending.getName().replace(".pending", ".bin"));
            if (target.exists() || !pending.renameTo(target)) {
                pending.delete(); throw new IOException("SNAPSHOT_PUBLISH_FAILED");
            }
            // Keep the original unique pin inode for the entire immutable snapshot lifetime.
            // Its UUID is never reused, so cleanup cannot acquire a replacement lock inode.
            published=target;quotaLock.release();quota.close();transferred = true;
            return new Handle(target, Arrays.copyOf(offsets, count), bytes, hex(digest.digest()),pinFile,pin,pinLock);
        }

        @Override public void close() throws IOException {
            IOException failure=null;
            if(!closed){closed=true;try{out.close();}catch(IOException e){failure=e;}}
            if(!transferred){
                pending.delete();if(published!=null)published.delete();
                try{if(pinLock.isValid())pinLock.release();}catch(IOException e){failure=add(failure,e);}
                try{pin.close();}catch(IOException e){failure=add(failure,e);}pinFile.delete();
                try{if(quotaLock.isValid())quotaLock.release();}catch(IOException e){failure=add(failure,e);}
                try{quota.close();}catch(IOException e){failure=add(failure,e);}
            }
            if(failure!=null)throw failure;
        }
        private static IOException add(IOException first,IOException next){if(first==null)return next;first.addSuppressed(next);return first;}

    }

    static final class Handle {
        final File file;
        final long[] offsets;
        final long bytes;
        final String hash;
        private final File pinFile;
        private final RandomAccessFile pin;
        private final FileLock pinLock;
        private int readers;
        private boolean retired;
        Handle(File file, long[] offsets, long bytes, String hash,File pinFile,RandomAccessFile pin,FileLock pinLock) {
            this.file=file; this.offsets=offsets; this.bytes=bytes; this.hash=hash;
            this.pinFile=pinFile;this.pin=pin;this.pinLock=pinLock;
        }
        synchronized Reader open() throws IOException {
            if (retired) throw new IOException("SNAPSHOT_EXPIRED");
            RandomAccessFile input = new RandomAccessFile(file, "r");
            try {
                if (input.readInt()!=MAGIC || input.readInt()!=VERSION || input.length()!=bytes)
                    throw new IOException("SNAPSHOT_FILE_INVALID");
                readers++; return new Reader(this,input);
            } catch (IOException e) { input.close(); throw e; }
        }
        synchronized void retire() { retired=true; deleteIfUnused(); }
        private synchronized void release() { if (readers>0) readers--; deleteIfUnused(); }
        private void deleteIfUnused() { if (retired && readers==0) {
            file.delete();
            try { if(pinLock.isValid())pinLock.release(); }catch(IOException ignored){}
            try { pin.close(); }catch(IOException ignored){}
            pinFile.delete();
        } }
    }

    static final class Reader implements Closeable {
        private final Handle handle;
        private RandomAccessFile input;
        Reader(Handle handle, RandomAccessFile input) { this.handle=handle; this.input=input; }
        Record get(int index) throws IOException {
            if (input==null) throw new IOException("SNAPSHOT_READER_CLOSED");
            if (index<0 || index>=handle.offsets.length) throw new IndexOutOfBoundsException();
            input.seek(handle.offsets[index]);
            int length=input.readInt();
            if (length<16 || length>MAX_ROW_BYTES || input.getFilePointer()+length>handle.bytes)
                throw new IOException("SNAPSHOT_ROW_INVALID");
            byte[] bytes=new byte[length]; input.readFully(bytes);
            DataInputStream row=new DataInputStream(new java.io.ByteArrayInputStream(bytes));
            Record result=new Record(readString(row),readString(row),readString(row),readString(row));
            if (row.available()!=0) throw new IOException("SNAPSHOT_ROW_TRAILING_BYTES");
            return result;
        }
        @Override public void close() throws IOException {
            if (input!=null) { RandomAccessFile closing=input; input=null; try { closing.close(); } finally { handle.release(); } }
        }
    }

    private static boolean isData(File file) {
        return file.isFile() && file.getName().startsWith(PREFIX)
                && (file.getName().endsWith(".bin") || file.getName().endsWith(".pending"));
    }
    /** Caller holds the directory quota lock; an active process holds the unique pin lock. */
    private static void cleanupOrphansLocked(File directory) {
        File[] entries=directory.listFiles();if(entries==null)return;
        for(File data:entries)if(isData(data)){
            String pendingName=data.getName().endsWith(".bin")
                    ?data.getName().substring(0,data.getName().length()-4)+".pending":data.getName();
            File marker=new File(directory,pendingName+".pin");
            RandomAccessFile check=null;FileLock lock=null;
            try{
                check=new RandomAccessFile(marker,"rw");
                try{lock=check.getChannel().tryLock();}catch(OverlappingFileLockException active){}
                if(lock!=null && data.delete())marker.delete();
            }catch(IOException ignored){}
            finally{
                if(lock!=null)try{lock.release();}catch(IOException ignored){}
                if(check!=null)try{check.close();}catch(IOException ignored){}
            }
        }
    }
    private static byte[] encode(Record record) throws IOException {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        DataOutputStream out=new DataOutputStream(bytes);
        writeString(out,record.page); writeString(out,record.key); writeString(out,record.title); writeString(out,record.detail);
        out.flush(); return bytes.toByteArray();
    }
    private static void writeString(DataOutputStream out,String value) throws IOException {
        byte[] bytes=value.getBytes("UTF-8");
        if (bytes.length>MAX_ROW_BYTES) throw new IOException("SNAPSHOT_ROW_TOO_LARGE");
        out.writeInt(bytes.length); out.write(bytes);
    }
    private static String readString(DataInputStream input) throws IOException {
        int length=input.readInt();
        if(length<0 || length>MAX_ROW_BYTES || length>input.available()) throw new IOException("SNAPSHOT_STRING_INVALID");
        byte[] bytes=new byte[length];input.readFully(bytes);return new String(bytes,"UTF-8");
    }
    private static String nonNull(String value) { return value==null ? "" : value; }
    private static String hex(byte[] data) {
        StringBuilder result=new StringBuilder(data.length*2);
        for(byte b:data) { int n=b&255; if(n<16)result.append('0');result.append(Integer.toHexString(n)); }
        return result.toString();
    }
    private ResearchSnapshotFileR82() { }
}
