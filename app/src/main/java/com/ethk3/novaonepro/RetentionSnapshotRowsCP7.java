package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.nio.channels.*;
import java.util.*;

/** Read-only adapter for SCROW1. Owns no VERIFIED receipt, live row, or prune authority.
 * Source is a private, hash-verified immutable snapshot held by the caller's job lease.
 * Native incremental blob I/O handles both TEXT and BLOB without whole-value SQL casts.
 */
public final class RetentionSnapshotRowsCP7 implements RetentionRowCodecCP5.RowSource,Closeable {
    public final RetentionRowCodecCP5.TableShape shape;
    private final ReportNativeCP4.RowReader reader;
    private final String alias;
    private final VerifiedSnapshot snapshot;
    private boolean started,finished,closed,hasRow;
    private long rowid;
    private final ReportNativeCP4.RowReader.Cell[] cells;
    private int activeBlob=-1;
    private long[] selectedRowIds;private int selectedCursor;
    private RetentionSnapshotRowsCP7(RetentionRowCodecCP5.TableShape shape,String alias,ReportNativeCP4.RowReader reader,VerifiedSnapshot snapshot){
        this.shape=shape;this.alias=alias;this.reader=reader;this.snapshot=snapshot;cells=new ReportNativeCP4.RowReader.Cell[shape.orderedColumns.size()];
    }
    /** Hash once per snapshot, then reuse for all table readers under the same lease. */
    public static final class VerifiedSnapshot implements Closeable {
        public final String sha256;public final long byteCount;
        private final File file;private final RandomAccessFile lockFile;private final FileLock lease;
        private final long modified;private boolean closed;private int readers;private final Thread owner=Thread.currentThread();
        private VerifiedSnapshot(File file,String hash,RandomAccessFile lockFile,FileLock lease){this.file=file;sha256=hash;byteCount=file.length();modified=file.lastModified();this.lockFile=lockFile;this.lease=lease;}
        /** In-process proof only, never deserialized from a journal. The sole diagnostics
         * owner keeps the private file immutable; replacement/expiry invalidates this proof. */
        static final class ReuseProof {
            private final File file;private final String hash;private final long bytes,modified;
            private ReuseProof(VerifiedSnapshot source){file=source.file;hash=source.sha256;bytes=source.byteCount;modified=source.modified;}
            private boolean matches(File candidate,String expected){return file.equals(candidate)&&hash.equals(expected)&&candidate.isFile()&&candidate.length()==bytes&&candidate.lastModified()==modified;}
        }
        synchronized ReuseProof reuseProof()throws IOException {check();return new ReuseProof(this);}
        synchronized void requireFile(File candidate)throws IOException {check();if(!file.equals(candidate.getCanonicalFile()))throw new IOException("ARCHIVE_SNAPSHOT_IDENTITY");}
        public static VerifiedSnapshot verify(File jobDirectory,File snapshot,String expectedSha256,ReportNativeCP4.Control control)throws IOException {
            return verifyReusable(jobDirectory,snapshot,expectedSha256,control,null);
        }
        static VerifiedSnapshot verifyReusable(File jobDirectory,File snapshot,String expectedSha256,ReportNativeCP4.Control control,ReuseProof proof)throws IOException {
            if(jobDirectory==null||snapshot==null||expectedSha256==null||!expectedSha256.matches("[0-9a-f]{64}")||control==null)throw new IOException("ARCHIVE_SNAPSHOT_IDENTITY");
            File file=snapshot.getCanonicalFile();String root=jobDirectory.getCanonicalPath()+File.separator;
            if(!file.getPath().startsWith(root)||!file.isFile())throw new IOException("ARCHIVE_SNAPSHOT_PATH");
            File lockPath=new File(file.getPath()+".lock");if(!lockPath.getCanonicalPath().equals(lockPath.getAbsolutePath()))throw new IOException("ARCHIVE_LOCK_PATH");
            RandomAccessFile lock=new RandomAccessFile(lockPath,"rw");FileLock lease=null;SQLiteDatabase db=null;
            try {
                try{lease=lock.getChannel().tryLock(0,Long.MAX_VALUE,true);}catch(OverlappingFileLockException busy){throw new IOException("ARCHIVE_SNAPSHOT_LEASE_BUSY",busy);}
                if(lease==null)throw new IOException("ARCHIVE_SNAPSHOT_LEASE_BUSY");
                if(!control.shouldContinue())throw new InterruptedIOException("ARCHIVE_CANCELLED");
                if(proof==null||!proof.matches(file,expectedSha256)){
                    long size=file.length(),modified=file.lastModified();MessageDigest hash=sha();byte[] b=new byte[8192];
                    try(InputStream in=new FileInputStream(file)){int n;while((n=in.read(b))!=-1){if(!control.shouldContinue())throw new InterruptedIOException("ARCHIVE_CANCELLED");hash.update(b,0,n);}}
                    if(file.length()!=size||file.lastModified()!=modified||!hex(hash.digest()).equals(expectedSha256))throw new IOException("ARCHIVE_SNAPSHOT_HASH");
                }
                db=SQLiteDatabase.openDatabase(file.getPath(),null,SQLiteDatabase.OPEN_READONLY|SQLiteDatabase.NO_LOCALIZED_COLLATORS);
                if(!db.isReadOnly())throw new IOException("ARCHIVE_READONLY_SNAPSHOT_REQUIRED");
                db.close();db=null;
                return new VerifiedSnapshot(file,expectedSha256,lock,lease);
            }catch(IOException|RuntimeException|Error e){if(db!=null)db.close();if(lease!=null)lease.release();lock.close();throw e;}
        }
        private synchronized void check()throws IOException {if(Thread.currentThread()!=owner)throw new IOException("ARCHIVE_THREAD_OWNER");if(closed||file.length()!=byteCount||file.lastModified()!=modified)throw new IOException("ARCHIVE_SNAPSHOT_CHANGED");}
        public synchronized void close()throws IOException {if(Thread.currentThread()!=owner)throw new IOException("ARCHIVE_THREAD_OWNER");if(closed)return;if(readers!=0)throw new IOException("ARCHIVE_SNAPSHOT_READER_OPEN");closed=true;try{lease.release();}finally{lockFile.close();}}
    }
    private static final class ShapeCP8 {
        final RetentionRowCodecCP5.TableShape shape;final String alias;
        ShapeCP8(RetentionRowCodecCP5.TableShape shape,String alias){this.shape=shape;this.alias=alias;}
    }
    /** Close Android SQLite metadata connection before opening the separately budgeted native one. */
    private static ShapeCP8 readShapeCP8(VerifiedSnapshot verified,String table,ReportNativeCP4.Control control)throws IOException {
        try(SQLiteDatabase snapshot=SQLiteDatabase.openDatabase(verified.file.getPath(),null,
                SQLiteDatabase.OPEN_READONLY|SQLiteDatabase.NO_LOCALIZED_COLLATORS)){
            if(!snapshot.isReadOnly())throw new IOException("ARCHIVE_READONLY_SNAPSHOT_REQUIRED");
        String ddl;
        try(Cursor c=snapshot.rawQuery("SELECT type,length(CAST(sql AS BLOB)) FROM sqlite_master WHERE name=?",new String[]{table})){
            if(!c.moveToFirst()||!"table".equals(c.getString(0))||c.isNull(1)||c.getLong(1)>65536)throw new IOException("ARCHIVE_SCHEMA_BOUND");
        }
        try(Cursor c=snapshot.rawQuery("SELECT sql FROM sqlite_master WHERE type='table' AND name=?",new String[]{table})){
            if(!c.moveToFirst()||(ddl=c.getString(0))==null)throw new IOException("ARCHIVE_TABLE_MISSING");
        }
        if(ddl.toUpperCase(Locale.ROOT).matches("(?s).*\\bWITHOUT\\s+ROWID\\b.*"))throw new IOException("ARCHIVE_WITHOUT_ROWID_UNSUPPORTED");
        String encoding;
        try(Cursor c=snapshot.rawQuery("PRAGMA encoding",null)){if(!c.moveToFirst())throw new IOException("ARCHIVE_ENCODING_MISSING");encoding=c.getString(0);}
        ArrayList<String> columns=new ArrayList<String>(),pk=new ArrayList<String>();HashSet<String> folded=new HashSet<String>();
        MessageDigest digest=sha();DataOutputStream signature=new DataOutputStream(new DigestOutputStream(new OutputStream(){public void write(int b){}public void write(byte[] b,int o,int n){}},digest));
        signature.writeInt(0x53435331);string(signature,ddl);string(signature,encoding);
        try(Cursor c=snapshot.rawQuery("PRAGMA table_info("+quote(table)+")",null)){
            while(c.moveToNext()){
                if(!control.shouldContinue())throw new InterruptedIOException("ARCHIVE_CANCELLED");
                if(columns.size()>=1024||c.getInt(0)!=columns.size())throw new IOException("ARCHIVE_COLUMN_BOUND");
                String name=c.getString(1);ascii(name);columns.add(name);folded.add(name.toLowerCase(Locale.ROOT));if(c.getInt(5)>0)pk.add(name);
                signature.writeByte(1);signature.writeInt(c.getInt(0));string(signature,name);string(signature,c.getString(2));signature.writeInt(c.getInt(3));string(signature,c.isNull(4)?null:c.getString(4));signature.writeInt(c.getInt(5));
            }
        }
        signature.writeByte(0);signature.flush();
        if(columns.isEmpty())throw new IOException("ARCHIVE_COLUMNS_MISSING");
        String rowidAlias=null;for(String option:new String[]{"_rowid_","rowid","oid"})if(!folded.contains(option)){rowidAlias=option;break;}
        if(rowidAlias==null)throw new IOException("ARCHIVE_ROWID_SHADOWED");
        RetentionRowCodecCP5.TableShape shape=new RetentionRowCodecCP5.TableShape(table,hex(digest.digest()),encoding,columns,pk,verified.sha256);
        return new ShapeCP8(shape,rowidAlias);
        }
    }
    public static RetentionSnapshotRowsCP7 open(VerifiedSnapshot verified,String table,
            long nativeBudget,ReportNativeCP4.Control control)throws IOException {
        if(verified==null)throw new IOException("ARCHIVE_SNAPSHOT_IDENTITY");verified.check();
        synchronized(verified){if(verified.readers!=0)throw new IOException("ARCHIVE_ONE_READER_REQUIRED");}
        if(control==null||!control.shouldContinue())throw new InterruptedIOException("ARCHIVE_CANCELLED");
        ascii(table);if(table.startsWith("sqlite_"))throw new IOException("ARCHIVE_INTERNAL_TABLE_USE_DATABASE_SNAPSHOT");
        ShapeCP8 metadata=readShapeCP8(verified,table,control);verified.check();
        ReportNativeCP4.RowReader nativeReader=ReportNativeCP4.openRows(verified.file,nativeBudget,control);
        try{RetentionSnapshotRowsCP7 rows=new RetentionSnapshotRowsCP7(metadata.shape,metadata.alias,nativeReader,verified);synchronized(verified){verified.check();verified.readers++;}return rows;}catch(IOException|RuntimeException|Error e){try{nativeReader.close();}catch(IOException close){e.addSuppressed(close);}throw e;}
    }
    public boolean next()throws IOException {
        checkOpen();if(finished)return false;
        Long next=selectedRowIds==null?reader.nextRow(shape.tableName,alias,!started,rowid):selectedCursor==selectedRowIds.length?null:Long.valueOf(selectedRowIds[selectedCursor++]);
        started=true;activeBlob=-1;Arrays.fill(cells,null);
        if(next==null){finished=true;hasRow=false;return false;}
        if(hasRow&&next.longValue()<=rowid)throw new IOException("ARCHIVE_ROW_ORDER");
        rowid=next.longValue();hasRow=true;
        if(selectedRowIds!=null)cell(0); // Missing requested rows fail; never silently shrink the archive scope.
        return true;
    }
    public long snapshotRowid()throws IOException {checkRow();return rowid;}
    public byte[] canonicalKey()throws IOException {
        checkRow();if(shape.primaryKeyColumns.isEmpty())return RetentionRowCodecCP5.snapshotLocalKey(shape,rowid);
        for(String key:shape.primaryKeyColumns)if(type(shape.orderedColumns.indexOf(key))==RetentionRowCodecCP5.NULL)
            return RetentionRowCodecCP5.snapshotLocalKey(shape,rowid);
        return RetentionRowCodecCP5.primaryKey(shape,this);
    }
    private ReportNativeCP4.RowReader.Cell cell(int column)throws IOException {
        checkColumn(column);
        if(cells[column]==null){cells[column]=reader.cell(shape.tableName,shape.orderedColumns.get(column),alias,rowid);activeBlob=cells[column].type>=3?column:-1;}
        return cells[column];
    }
    public int type(int column)throws IOException{return cell(column).type;}
    public long length(int column)throws IOException{return cell(column).length;}
    public long scalarBits(int column)throws IOException {ReportNativeCP4.RowReader.Cell c=cell(column);if(c.type!=1&&c.type!=2)throw new IOException("ARCHIVE_NONSCALAR");return c.scalarBits;}
    public int readBytes(int column,long offset,byte[] buffer,int maximum)throws IOException {
        ReportNativeCP4.RowReader.Cell c=cell(column);
        if(c.type<3||buffer==null||maximum<1||maximum>8192||maximum>buffer.length||offset<0||offset>c.length-maximum)throw new IOException("ARCHIVE_CHUNK_BOUND");
        if(activeBlob!=column){ReportNativeCP4.RowReader.Cell current=reader.cell(shape.tableName,shape.orderedColumns.get(column),alias,rowid);
            if(current.type!=c.type||current.length!=c.length)throw new IOException("ARCHIVE_SNAPSHOT_CHANGED");activeBlob=column;}
        return reader.read(offset,buffer,maximum);
    }
    public long nativePeakBytes()throws IOException {checkOpen();return reader.nativePeakBytes();}
    private void checkOpen()throws IOException {if(closed)throw new IOException("ARCHIVE_SOURCE_CLOSED");snapshot.check();}
    private void checkRow()throws IOException {checkOpen();if(!hasRow||finished)throw new IOException("ARCHIVE_ROW_NOT_POSITIONED");}
    private void checkColumn(int column)throws IOException {checkRow();if(column<0||column>=cells.length)throw new IOException("ARCHIVE_COLUMN_INDEX");}
    public void close()throws IOException {if(!closed){closed=true;hasRow=false;Arrays.fill(cells,null);try{reader.close();}finally{synchronized(snapshot){snapshot.readers--;}}}}
    /** Convenience adapter used by the retention preparation worker; output stays STAGING. */
    public static RetentionRowCodecCP5.ArchiveDigest write(VerifiedSnapshot snapshot,String table,
            long nativeBudget,ReportNativeCP4.Control control,OutputStream staging)throws IOException {
        try(RetentionSnapshotRowsCP7 rows=open(snapshot,table,nativeBudget,control)){
            return RetentionRowCodecCP5.writeTable(rows.shape,rows,staging,new RetentionRowCodecCP5.CancelCheck(){
                public void check()throws IOException{if(!control.shouldContinue())throw new InterruptedIOException("ARCHIVE_CANCELLED");}
            });
        }
    }
    public static RetentionSnapshotRowsCP7 openSelected(VerifiedSnapshot snapshot,String table,long[] rowIds,
            long nativeBudget,ReportNativeCP4.Control control)throws IOException {
        if(rowIds==null||rowIds.length<1||rowIds.length>128)throw new IOException("ARCHIVE_PAGE_BOUND");
        long[] copy=rowIds.clone();Arrays.sort(copy);for(int i=1;i<copy.length;i++)if(copy[i]==copy[i-1])throw new IOException("ARCHIVE_DUPLICATE_ROWID");
        RetentionSnapshotRowsCP7 rows=open(snapshot,table,nativeBudget,control);rows.selectedRowIds=copy;return rows;
    }
    private static String quote(String s){return "\""+s.replace("\"","\"\"")+"\"";}
    private static void ascii(String s)throws IOException {if(s==null||s.isEmpty()||s.length()>256)throw new IOException("ARCHIVE_IDENTIFIER");for(int i=0;i<s.length();i++)if(s.charAt(i)<32||s.charAt(i)>126)throw new IOException("ARCHIVE_IDENTIFIER_ENCODING");}
    private static void string(DataOutputStream out,String s)throws IOException {if(s==null){out.writeInt(-1);return;}byte[] b=s.getBytes(StandardCharsets.UTF_8);if(b.length>65536)throw new IOException("ARCHIVE_SCHEMA_BOUND");out.writeInt(b.length);out.write(b);}
    private static MessageDigest sha(){try{return MessageDigest.getInstance("SHA-256");}catch(NoSuchAlgorithmException e){throw new AssertionError(e);}}
    private static String hex(byte[] b){StringBuilder s=new StringBuilder();for(byte v:b){s.append("0123456789abcdef".charAt((v&255)>>>4));s.append("0123456789abcdef".charAt(v&15));}return s.toString();}
}
