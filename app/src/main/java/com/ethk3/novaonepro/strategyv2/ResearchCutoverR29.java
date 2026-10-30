package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.StatFs;
import com.ethk3.novaonepro.DbStore;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Verified R27 ownership handoff. Copy, baseline backup and hashing never own the realtime writer. Only the final
 * route receipt uses a short realtime transaction. The original tables and the sealed baseline are retained. */
public final class ResearchCutoverR29 {
    private ResearchCutoverR29(){}
    public static final String PUBLICATION="CREATE TABLE IF NOT EXISTS r29_migration_publication(singleton INTEGER PRIMARY KEY CHECK(singleton=1),epoch INTEGER NOT NULL,candidate_name TEXT NOT NULL,phase TEXT NOT NULL,baseline_name TEXT NOT NULL DEFAULT '',baseline_sha TEXT NOT NULL DEFAULT '',source_root TEXT NOT NULL DEFAULT '',last_error TEXT NOT NULL DEFAULT '',updated_at INTEGER NOT NULL)";
    private static final String CANDIDATE="starcore-r29-research.candidate.db";
    private static final long BUDGET_MS=30_000,RESERVE_BYTES=96L<<20;

    static List<MigrationCopyR29.Table> tables(SQLiteDatabase source){
        List<MigrationCopyR29.Table> out=new ArrayList<>();
        try(Cursor c=source.rawQuery("SELECT name,sql FROM sqlite_master WHERE type='table' ORDER BY name",null)){
            while(c.moveToNext()){
                String name=c.getString(0);
                if(MigrationCopyR29.ownedResearch(name)||name.equals("r27_chart_cache")||name.equals("r27_fetch_reject")||name.equals("draws")||name.equals("su1_source_clock")||name.equals("r26_feedback")||name.equals("app_state")||CriticalInputMirrorR29.tables().contains(name))out.add(MigrationCopyR29.table(source,name,c.getString(1)));
            }
        }
        return out;
    }
    static String predicate(String table){return "1=1";}
    static List<MigrationCopyR29.Row> page(SQLiteDatabase source,MigrationCopyR29.Table table,Long after){
        return ResearchRowsR29.readPage(source,table,after,predicate(table.name),32,2L<<20,Long.MAX_VALUE,()->{});
    }
    private static List<MigrationCopyR29.Row> page(SQLiteDatabase source,MigrationCopyR29.Table table,Long after,long deadline,java.util.function.BooleanSupplier cancelled){
        return ResearchRowsR29.readPage(source,table,after,predicate(table.name),32,2L<<20,Long.MAX_VALUE,()->check(deadline,cancelled));
    }
    static Object[] decode(byte[] bytes,int expected){
        if(bytes==null||bytes.length>MigrationCopyR29.MAX_ROW_BYTES)throw new IllegalArgumentException("R29_COPY_PAYLOAD_BUDGET");
        try{
            DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes));
            if(in.readInt()!=0x29DB0001||in.readInt()!=expected)throw new IOException("ROW_ABI_OR_SHAPE");
            Object[] values=new Object[expected];
            for(int i=0;i<expected;i++)switch(in.readUnsignedByte()){
                case Cursor.FIELD_TYPE_NULL:values[i]=null;break;
                case Cursor.FIELD_TYPE_INTEGER:values[i]=in.readLong();break;
                case Cursor.FIELD_TYPE_FLOAT:values[i]=Double.longBitsToDouble(in.readLong());break;
                case Cursor.FIELD_TYPE_STRING:values[i]=new String(blob(in),StandardCharsets.UTF_8);break;
                case Cursor.FIELD_TYPE_BLOB:values[i]=blob(in);break;
                default:throw new IOException("ROW_TYPE");
            }
            if(in.available()!=0)throw new IOException("ROW_TRAILING_BYTES");return values;
        }catch(IOException invalid){throw new IllegalArgumentException("R29_COPY_PAYLOAD_INVALID",invalid);}
    }
    static byte[] blob(DataInputStream in)throws IOException{int n=in.readInt();if(n<0||n>MigrationCopyR29.MAX_ROW_BYTES||n>in.available())throw new IOException("ROW_CELL_LENGTH");byte[] b=new byte[n];in.readFully(b);return b;}
    static void restorePage(SQLiteDatabase destination,MigrationCopyR29.Table table,List<MigrationCopyR29.Row> rows){
        boolean rowid=!ResearchRowsR29.withoutRowid(table);StringBuilder columns=new StringBuilder(rowid?"rowid":""),binds=new StringBuilder(rowid?"?":"");
        for(String column:table.columns){if(columns.length()>0){columns.append(',');binds.append(',');}columns.append(MigrationCopyR29.id(column));binds.append('?');}
        String sql="INSERT INTO "+MigrationCopyR29.id(table.name)+"("+columns+") VALUES("+binds+")";
        for(MigrationCopyR29.Row row:rows){
            if(!StatsR27.sha256(row.payload).equals(row.hash))throw new IllegalStateException("R29_COPY_SOURCE_CHECKSUM");
            Object[] cells=decode(row.payload,table.columns.size()),values=new Object[cells.length+(rowid?1:0)];if(rowid)values[0]=row.rowid;System.arraycopy(cells,0,values,rowid?1:0,cells.length);destination.execSQL(sql,values);
        }
    }
    static String root(SQLiteDatabase source,List<MigrationCopyR29.Table> tables,long deadline,java.util.function.BooleanSupplier cancelled){
        StringBuilder root=new StringBuilder("R29_ACTIVE_DATA_V1");
        for(MigrationCopyR29.Table t:tables){
            String chain=t.seed();Long after=null;long count=0;
            for(;;){check(deadline,cancelled);List<MigrationCopyR29.Row> page=page(source,t,after,deadline,cancelled);if(page.isEmpty())break;for(MigrationCopyR29.Row row:page){chain=MigrationCopyR29.chain(chain,row.rowid,row.hash);after=row.rowid;count++;}}
            root.append('|').append(t.name).append(':').append(count).append(':').append(chain);
        }
        return StatsR27.sha256(root.toString());
    }
    static void check(long deadline,java.util.function.BooleanSupplier cancelled){if(cancelled.getAsBoolean()||Thread.currentThread().isInterrupted())throw new IllegalStateException("R29_CUTOVER_CANCELLED");if(android.os.SystemClock.elapsedRealtime()>deadline)throw new IllegalStateException("R29_CUTOVER_BUDGET_YIELD");}
    static String schemaHash(List<MigrationCopyR29.Table> tables){StringBuilder b=new StringBuilder("R29_ACTIVE_SCHEMA_V1");for(MigrationCopyR29.Table t:tables)b.append('|').append(t.name).append(':').append(t.schemaHash);return StatsR27.sha256(b.toString());}

    public static String run(DbStore store,ResearchPoolR27.Worker worker)throws Exception{
        if(ResearchRouteR29.active(worker.realtimeRo()))return "ACTIVE";
        if(!RuntimeCoordinatorR26.readyForResearchMigration())return "WAITING_LEGACY_STORAGE_SCHEMA_BEFORE_OWNER_TRANSFER";
        if(!worker.pool().beginCutover(3000))return "WAITING_RESEARCH_QUIESCENCE";
        try(ResearchRouteR29.Handoff handoff=ResearchRouteR29.handoff(store)){
            if(handoff==null)return "WAITING_RESEARCH_WRITER_HANDOFF";
            return copyAndPublish(store,worker);
        }finally{worker.pool().endCutover();}
    }
    private static String copyAndPublish(DbStore store,ResearchPoolR27.Worker worker)throws Exception{
        long deadline=android.os.SystemClock.elapsedRealtime()+BUDGET_MS;
        SQLiteDatabase source=worker.realtimeRo();
        File candidate=store.contextForR26().getDatabasePath(CANDIDATE),published=store.contextForR26().getDatabasePath(ResearchRouteR29.FILE_NAME);
        if(new StatFs(candidate.getParent()).getAvailableBytes()<RESERVE_BYTES)return "BLOCKED_STORAGE_RESERVE";
        final boolean candidateExists=candidate.exists()||published.exists();
        long epoch=store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R29_CUTOVER_RESERVE",db->{
            db.execSQL(PUBLICATION);ResearchRouteR29.ensureRealtime(db);RealtimeResearchProjectionR29.ensureRealtime(db);StrategySchemaDomainR29.ensureRealtime(db);CriticalInputMirrorR29.ensureRealtime(db);LegacyAuthorityProjectionR29.ensureRealtime(db);
            long current=0;try(Cursor c=db.rawQuery("SELECT epoch,candidate_name FROM r29_migration_publication WHERE singleton=1",null)){if(c.moveToFirst()){if(!CANDIDATE.equals(c.getString(1)))throw new IllegalStateException("R29_CANDIDATE_RESERVATION_MISMATCH");current=c.getLong(0);}}
            if(current==0&&candidateExists)throw new IllegalStateException("R29_UNOWNED_CANDIDATE_REQUIRES_REVIEW");
            if(current==0)current=Math.max(1,System.currentTimeMillis());
            db.execSQL("INSERT OR IGNORE INTO r29_migration_publication(singleton,epoch,candidate_name,phase,updated_at) VALUES(1,?,?,'PREPARED',?)",new Object[]{current,CANDIDATE,System.currentTimeMillis()});return current;
        });
        // Only files reserved by this unpublished manifest may be retried; authoritative/original files are untouched.
        if(ResearchRouteR29.active(source))return "ACTIVE";
        removeUnpublished(candidate);removeUnpublished(published);
        List<MigrationCopyR29.Table> tables=tables(source);String schema=schemaHash(tables),copiedRoot;
        long inputEpoch=CriticalInputMirrorR29.sourceEpoch(source),inputSequence=CriticalInputMirrorR29.sourceSequence(source);String inputView=CriticalInputMirrorR29.sourceViewSql(source);
        SQLiteDatabase target=SQLiteDatabase.openOrCreateDatabase(candidate,null);
        try{
            if(!target.enableWriteAheadLogging())throw new IllegalStateException("R29_CANDIDATE_WAL_REQUIRED");target.execSQL("PRAGMA synchronous=FULL");
            target.beginTransactionNonExclusive();try{
                target.execSQL(ResearchRouteR29.OWNER_DDL);
                target.execSQL("INSERT INTO r29_research_owner VALUES(1,'COPYING',?,'',?,?)",new Object[]{schema,epoch,System.currentTimeMillis()});
                for(MigrationCopyR29.Table t:tables)target.execSQL(t.createSql);
                target.setTransactionSuccessful();
            }finally{target.endTransaction();}
            for(MigrationCopyR29.Table t:tables){
                Long after=null;
                for(;;){check(deadline,worker::cancelled);List<MigrationCopyR29.Row> rows=page(source,t,after,deadline,worker::cancelled);if(rows.isEmpty())break;
                    long needed=RESERVE_BYTES;for(MigrationCopyR29.Row row:rows)needed+=row.payload.length;
                    if(new StatFs(candidate.getParent()).getAvailableBytes()<needed)throw new IllegalStateException("R29_CUTOVER_STORAGE_BUDGET_YIELD");
                    target.beginTransactionNonExclusive();try{restorePage(target,t,rows);target.setTransactionSuccessful();}finally{target.endTransaction();}
                    after=rows.get(rows.size()-1).rowid;
                }
            }
            copiedRoot=root(target,tables,deadline,worker::cancelled);
            if(!copiedRoot.equals(root(source,tables,deadline,worker::cancelled)))throw new IllegalStateException("R29_FINAL_CATCHUP_HASH_MISMATCH");
            Set<String> names=new HashSet<>();for(MigrationCopyR29.Table t:tables)names.add(t.name);
            target.beginTransactionNonExclusive();try{
                try(Cursor c=source.rawQuery("SELECT name,tbl_name,sql FROM sqlite_master WHERE type IN ('index','trigger') AND sql IS NOT NULL ORDER BY type,name",null)){
                    while(c.moveToNext())if(names.contains(c.getString(1))&&!c.getString(0).startsWith("r29_copy_track_")&&!c.getString(1).equals("draws")&&!c.getString(1).equals("su1_source_clock")&&!c.getString(1).equals("r26_feedback")&&!c.getString(1).equals("app_state")&&!CriticalInputMirrorR29.tables().contains(c.getString(1)))target.execSQL(c.getString(2));
                }
                ResearchInputMirrorR29.initialized(target,source);
                if(ResearchInputMirrorR29.table(target,"r26_live_model"))LegacyLiveProjectionR29.installResearch(target);
                LegacyAuthorityProjectionR29.installResearch(target);
                CriticalInputMirrorR29.installResearch(target);
                LegacyLearnedInputR29.installResearch(target);
                com.ethk3.novaonepro.LegacyLearningCommitR29.installResearch(target);
                if(inputEpoch==CriticalInputMirrorR29.sourceEpoch(source)&&inputSequence==CriticalInputMirrorR29.sourceSequence(source))CriticalInputMirrorR29.initialized(target,inputEpoch,inputSequence,inputView);
                target.execSQL(LegacyDomainUpgradeR29.DDL);
                target.execSQL("INSERT OR REPLACE INTO r29_legacy_domain_owner VALUES(1,'FULL',?,?,?)",new Object[]{epoch,copiedRoot,System.currentTimeMillis()});
                LegacyDomainExtensionR29.includedInFull(target,epoch,copiedRoot);
                RealtimeResearchProjectionR29.installResearch(target);
                target.execSQL("UPDATE r29_research_owner SET phase='READY',source_root=? WHERE singleton=1",new Object[]{copiedRoot});target.setTransactionSuccessful();
            }finally{target.endTransaction();}
            try(Cursor c=target.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)",null)){if(!c.moveToFirst()||c.getInt(0)!=0)throw new IllegalStateException("R29_CANDIDATE_CHECKPOINT_BUSY");}
        }finally{target.close();}
        sync(candidate);check(deadline,worker::cancelled);
        ResearchRouteR29.verifyPreparedReader(candidate,source.getPath());
        File baseline=new File(candidate.getParentFile(),"starcore-r29-baseline-"+epoch+".db");
        if(!baseline.exists()&&new StatFs(candidate.getParent()).getAvailableBytes()<candidate.length()+RESERVE_BYTES)throw new IllegalStateException("R29_CUTOVER_STORAGE_BUDGET_YIELD");
        if(!baseline.exists())copyBaseline(candidate,baseline,deadline,worker::cancelled);
        final String baselineHash=fileHash(baseline,deadline,worker::cancelled);
        try(Cursor c=source.rawQuery("SELECT baseline_sha FROM r29_migration_publication WHERE singleton=1",null)){if(c.moveToFirst()&&c.getString(0)!=null&&!c.getString(0).isEmpty()&&!baselineHash.equals(c.getString(0)))throw new IllegalStateException("R29_PINNED_BASELINE_CORRUPT");}
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R29_BASELINE_PIN",db->{db.execSQL("UPDATE r29_migration_publication SET phase='VERIFIED',baseline_name=?,baseline_sha=?,source_root=?,updated_at=? WHERE singleton=1 AND epoch=?",new Object[]{baseline.getName(),baselineHash,copiedRoot,System.currentTimeMillis(),epoch});return 0;});
        check(deadline,worker::cancelled);
        if(!candidate.renameTo(published))throw new IllegalStateException("R29_CANDIDATE_ATOMIC_RENAME_FAILED");
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R29_RESEARCH_RENAMED",db->{db.execSQL("UPDATE r29_migration_publication SET phase='RENAMED',updated_at=? WHERE singleton=1 AND epoch=?",new Object[]{System.currentTimeMillis(),epoch});return 0;});
        ResearchRouteR29.publish(store,published,schema,copiedRoot,epoch);worker.pool().reloadResearchReaders();
        return "ACTIVE_INDEPENDENT_RESEARCH_WRITER";
    }
    static void removeUnpublished(File file)throws IOException{for(String suffix:new String[]{"","-wal","-shm"}){File part=new File(file.getPath()+suffix);if(part.exists()&&!part.delete())throw new IOException("R29_UNPUBLISHED_CANDIDATE_DELETE_FAILED");}}
    static void copyBaseline(File source,File baseline,long deadline,java.util.function.BooleanSupplier cancelled)throws Exception{
        File temporary=new File(baseline.getPath()+".partial");
        try(InputStream in=new FileInputStream(source);FileOutputStream out=new FileOutputStream(temporary)){
            byte[] b=new byte[64<<10];for(int n;(n=in.read(b))>=0;){check(deadline,cancelled);out.write(b,0,n);}out.getFD().sync();
        }
        if(!fileHash(source,deadline,cancelled).equals(fileHash(temporary,deadline,cancelled)))throw new IOException("R29_BASELINE_COPY_HASH_MISMATCH");
        if(!temporary.renameTo(baseline))throw new IOException("R29_BASELINE_PUBLISH_FAILED");
    }
    static void sync(File file)throws IOException{try(FileOutputStream out=new FileOutputStream(file,true)){out.getFD().sync();}}
    static String fileHash(File file,long deadline,java.util.function.BooleanSupplier cancelled)throws Exception{
        MessageDigest digest=MessageDigest.getInstance("SHA-256");try(InputStream in=new FileInputStream(file)){byte[] b=new byte[64<<10];for(int n;(n=in.read(b))>=0;){check(deadline,cancelled);digest.update(b,0,n);}}StringBuilder hex=new StringBuilder();for(byte b:digest.digest())hex.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return hex.toString();
    }
}
