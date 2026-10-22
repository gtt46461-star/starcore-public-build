package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import com.ethk3.novaonepro.r952.StrictCanonicalJson;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Verified private archives. Files are flushed and replayed before the owner can release local bytes. */
public final class ArchiveStoreSU1 {
    public static final long LOCAL_LIMIT=64L<<20,TOTAL_LIMIT=512L<<20;
    private ArchiveStoreSU1() {}
    private static File directory(SQLiteDatabase db){File parent=new File(db.getPath()).getAbsoluteFile().getParentFile();if(parent==null)throw new IllegalStateException("SU1_ARCHIVE_DATABASE_PATH");return new File(parent,"su1-archives");}
    private static final class Entry {
        final String hash,codec,payload;final int bytes;
        Entry(String h,String c,String p){hash=HashingSU1.sha(h);codec=c;payload=p;bytes=p.getBytes(StandardCharsets.UTF_8).length;if(bytes>2097152||!hash.equals(HashingSU1.text(p)))throw new IllegalArgumentException("SU1_ARCHIVE_CONTENT");}
    }
    private static void write(File file,List<Entry> entries)throws IOException{
        try(FileOutputStream stream=new FileOutputStream(file);DataOutputStream out=new DataOutputStream(new BufferedOutputStream(stream))){out.writeInt(0x53553141);out.writeInt(1);out.writeInt(entries.size());for(Entry e:entries){out.writeUTF(e.hash);out.writeUTF(e.codec);byte[] bytes=e.payload.getBytes(StandardCharsets.UTF_8);out.writeInt(bytes.length);out.write(bytes);}out.flush();stream.getFD().sync();}
    }
    private static List<Entry> readFile(File file)throws IOException{
        if(file.length()>8388608)throw new IOException("SU1_ARCHIVE_BYTE_LIMIT");List<Entry> out=new ArrayList<>();
        try(DataInputStream in=new DataInputStream(new BufferedInputStream(new FileInputStream(file)))){if(in.readInt()!=0x53553141||in.readInt()!=1)throw new IOException("SU1_ARCHIVE_FORMAT");int n=in.readInt();if(n<1||n>8)throw new IOException("SU1_ARCHIVE_ENTRIES");Set<String> unique=new HashSet<>();for(int i=0;i<n;i++){String hash=in.readUTF(),codec=in.readUTF();int bytes=in.readInt();if(bytes<0||bytes>2097152)throw new IOException("SU1_ARCHIVE_ENTRY_BYTES");byte[] data=new byte[bytes];in.readFully(data);Entry e=new Entry(hash,codec,new String(data,StandardCharsets.UTF_8));if(e.bytes!=bytes||!unique.add(hash))throw new IOException("SU1_ARCHIVE_CONTENT");out.add(e);}if(in.read()!=-1)throw new IOException("SU1_ARCHIVE_TAIL");}return out;
    }
    private static String manifest(List<Entry> entries,String graph){Map<String,Object> m=new TreeMap<>();m.put("protocol","SU1_PRIVATE_ARCHIVE_V1");m.put("verification","BYTE_IDENTICAL_CODEC_RESTORE_AND_GRAMMAR_REPLAY");m.put("graph",graph);List<Object> items=new ArrayList<>();for(Entry e:entries)items.add(Arrays.asList(e.hash,e.codec,e.bytes));m.put("entries",items);return StrictCanonicalJson.write(m);}
    public static String read(SQLiteDatabase db,String hash){
        String locator,json,manifestHash,codec;long bytes;
        try(Cursor c=db.rawQuery("SELECT a.archive_locator,a.manifest_json,a.manifest_hash,b.codec,b.byte_count FROM su1_blob b JOIN su1_archive_receipt a ON a.receipt_id=b.archive_receipt_id WHERE b.blob_hash=? AND b.storage_state='ARCHIVED' AND a.replay_verified=1",new String[]{HashingSU1.sha(hash)})){if(!c.moveToFirst())throw new IllegalStateException("SU1_ARCHIVE_RECEIPT_REQUIRED");locator=c.getString(0);json=c.getString(1);manifestHash=c.getString(2);codec=c.getString(3);bytes=c.getLong(4);}
        if(!manifestHash.equals(HashingSU1.text(json))||!locator.equals(manifestHash+".su1a"))throw new IllegalStateException("SU1_ARCHIVE_MANIFEST");
        try{File file=new File(directory(db),locator);if(!file.getCanonicalFile().getParentFile().equals(directory(db).getCanonicalFile()))throw new IOException("SU1_ARCHIVE_PATH");List<Entry> entries=readFile(file);Map<String,Object> m=StrictCanonicalJson.readObject(json);if(!json.equals(manifest(entries,(String)m.get("graph"))))throw new IOException("SU1_ARCHIVE_MANIFEST_CONTENT");for(Entry e:entries)if(e.hash.equals(hash)){if(e.bytes!=bytes||!e.codec.equals(codec))throw new IOException("SU1_ARCHIVE_METADATA");return e.payload;}throw new IOException("SU1_ARCHIVE_MISSING_ENTRY");}catch(IOException invalid){throw new IllegalStateException("SU1_ARCHIVE_RESTORE_FAILED",invalid);}
    }
    private static Set<String> roots(SQLiteDatabase db){
        Set<String> roots=new TreeSet<>();
        String live="v.strategy_id IN(SELECT strategy_id FROM su1_research_pool WHERE state='ACTIVE') OR v.strategy_id IN(SELECT strategy_id FROM su1_eval_cohort c JOIN su1_cohort_runtime r ON r.cohort_id=c.cohort_id WHERE r.state='OPEN') OR v.strategy_id IN(SELECT parent_strategy_id FROM su1_eval_cohort c JOIN su1_cohort_runtime r ON r.cohort_id=c.cohort_id WHERE r.state='OPEN') OR v.strategy_id IN(SELECT strategy_id FROM su1_certificate WHERE expires_at>"+System.currentTimeMillis()+")";
        try(Cursor c=db.rawQuery("SELECT p.typed_ir_hash,m.weights_hash,m.training_report_hash,v.calibration_hash FROM su1_strategy_version v JOIN su1_program p ON p.program_hash=v.program_hash JOIN su1_model_artifact m ON m.artifact_hash=v.artifact_hash WHERE "+live,null)){while(c.moveToNext())for(int i=0;i<4;i++)if(!c.isNull(i))roots.add(c.getString(i));}
        String[] sql={"SELECT dataset_hash FROM su1_burst_checkpoint","SELECT s.payload_hash FROM su1_live_target t JOIN su1_snapshot s ON s.snapshot_hash=t.snapshot_hash WHERE t.completed_at IS NULL","SELECT s.payload_hash FROM su1_prediction p JOIN su1_snapshot s ON s.snapshot_hash=p.snapshot_hash WHERE NOT EXISTS(SELECT 1 FROM su1_settlement x WHERE x.prediction_id=p.prediction_id)","SELECT probability_hash FROM su1_prediction p WHERE NOT EXISTS(SELECT 1 FROM su1_settlement x WHERE x.prediction_id=p.prediction_id)","SELECT period_state_hash FROM su1_prediction p WHERE NOT EXISTS(SELECT 1 FROM su1_settlement x WHERE x.prediction_id=p.prediction_id)","SELECT checkpoint_hash FROM su1_research_task WHERE state='LEASED' AND checkpoint_hash IS NOT NULL"};
        for(String query:sql)try(Cursor c=db.rawQuery(query,null)){while(c.moveToNext())roots.add(c.getString(0));}
        ArrayDeque<String> todo=new ArrayDeque<>(roots);while(!todo.isEmpty()){String id=todo.removeFirst();try(Cursor c=db.rawQuery("SELECT target_id FROM su1_dependency WHERE source_kind='BLOB' AND source_id=? AND target_kind='BLOB'",new String[]{id})){while(c.moveToNext())if(roots.add(c.getString(0)))todo.add(c.getString(0));}if(roots.size()>100000)throw new IllegalStateException("SU1_PIN_GRAPH_BUDGET");}return roots;
    }
    private static long clock(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT generation FROM su1_retention_clock WHERE singleton=1",null)){if(!c.moveToFirst())throw new IllegalStateException("SU1_RETENTION_CLOCK");return c.getLong(0);}}
    public static boolean canCreate(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT coalesce(sum(byte_count),0) FROM su1_blob",null)){c.moveToFirst();return c.getLong(0)<TOTAL_LIMIT;}}
    private static void pin(SQLiteDatabase db,Set<String> roots,long now){
        Set<String> existing=new HashSet<>();List<String> release=new ArrayList<>();try(Cursor c=db.rawQuery("SELECT pin_id,object_id FROM su1_retention_pin WHERE object_kind='BLOB' AND reason='ACTIVE_STRATEGY' AND released_at IS NULL",null)){while(c.moveToNext()){if(roots.contains(c.getString(1)))existing.add(c.getString(1));else release.add(c.getString(0));}}
        for(String id:release)db.execSQL("UPDATE su1_retention_pin SET released_at=? WHERE pin_id=? AND released_at IS NULL",new Object[]{now,id});
        for(String hash:roots)if(!existing.contains(hash))StrategyRepositorySU1.row(db,"su1_retention_pin","pin_id",HashingSU1.fields("SU1_TRANSITIVE_PIN_V1",hash,String.valueOf(now)),"object_kind","BLOB","object_id",hash,"reason","ACTIVE_STRATEGY","created_at",now);
    }
    private static void prepareDaily(DbStore store){
        Models.Draw latest=store.getLatestDraw();if(latest==null)return;long cutoff=TargetIssueResolver.firstFutureOrdinalSU1(latest,System.currentTimeMillis())-10080;
        String strategy=null;int day=0,expectedN=0,expectedK=0;
        try(Cursor c=store.readable().rawQuery("SELECT d.strategy_id,d.day_key,d.actions,d.hits FROM su1_live_daily d WHERE NOT EXISTS(SELECT 1 FROM su1_live_day_archive a WHERE a.strategy_id=d.strategy_id AND a.day_key=d.day_key) AND NOT EXISTS(SELECT 1 FROM su1_live_shadow l WHERE l.strategy_id=d.strategy_id AND l.day_key=d.day_key AND l.sequence_ordinal>=?) AND NOT EXISTS(SELECT 1 FROM su1_live_shadow l JOIN su1_sealed_plan p ON p.strategy_id=l.strategy_id AND l.sequence_ordinal BETWEEN p.first_sequence AND p.last_sequence WHERE l.strategy_id=d.strategy_id AND l.day_key=d.day_key AND NOT EXISTS(SELECT 1 FROM su1_sealed_result r WHERE r.plan_campaign=p.campaign_id) AND NOT EXISTS(SELECT 1 FROM su1_sealed_cancelled x WHERE x.campaign_id=p.campaign_id)) ORDER BY d.day_key,d.strategy_id LIMIT 1",new String[]{String.valueOf(cutoff)})){if(c.moveToFirst()){strategy=c.getString(0);day=c.getInt(1);expectedN=c.getInt(2);expectedK=c.getInt(3);}}if(strategy==null)return;
        List<Object> rows=new ArrayList<>();int hits=0;
        try(Cursor c=store.readable().rawQuery("SELECT sequence_ordinal,position,number_mask,captured_at,target_open_time,hit,settled_at FROM su1_live_shadow WHERE strategy_id=? AND day_key=? ORDER BY sequence_ordinal",new String[]{strategy,String.valueOf(day)})){while(c.moveToNext()){if(c.isNull(5)||c.isNull(6))return;List<Long> row=new ArrayList<>();for(int i=0;i<7;i++)row.add(c.getLong(i));rows.add(row);hits+=c.getInt(5);}}
        if(rows.size()!=expectedN||hits!=expectedK)throw new IllegalStateException("SU1_DAILY_ARCHIVE_AGGREGATE");Map<String,Object> payload=new TreeMap<>();payload.put("protocol","SU1_LIVE_DAY_V1");payload.put("strategy",strategy);payload.put("day",day);payload.put("actions",expectedN);payload.put("hits",expectedK);StringBuilder csv=new StringBuilder();for(Object row:rows){for(Object v:(List<?>)row)csv.append(v).append(',');csv.append('\n');}payload.put("rowsCsv",csv.toString());String wire=StrictCanonicalJson.writeContext(payload);String id=strategy;int key=day,n=expectedN,k=expectedK;long now=System.currentTimeMillis();
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SU1_PREPARE_DAILY_ARCHIVE",db->{if(StrategyRepositorySU1.exists(db,"SELECT 1 FROM su1_live_day_archive WHERE strategy_id=? AND day_key=?",id,String.valueOf(key)))return 0;String hash=StrategyRepositorySU1.blob(db,wire,"SU1_LIVE_DAY_V1",now);StrategyRepositorySU1.row(db,"su1_live_day_archive","strategy_id",id,"day_key",key,"payload_hash",hash,"actions",n,"hits",k,"created_at",now);return 0;});
    }
    private static void compactDaily(DbStore store){
        String id=null,hash=null;int day=0,n=0,k=0;
        try(Cursor c=store.readable().rawQuery("SELECT a.strategy_id,a.day_key,a.payload_hash,a.actions,a.hits FROM su1_live_day_archive a JOIN su1_blob b ON b.blob_hash=a.payload_hash WHERE a.compacted_at IS NULL AND b.storage_state='ARCHIVED' ORDER BY a.day_key LIMIT 1",null)){if(c.moveToFirst()){id=c.getString(0);day=c.getInt(1);hash=c.getString(2);n=c.getInt(3);k=c.getInt(4);}}if(id==null)return;
        Map<String,Object> restored=StrictCanonicalJson.readContextObject(read(store.readable(),hash));String[] rows=((String)restored.get("rowsCsv")).split("\n");long sum=0;for(String row:rows){String[] fields=row.split(",");if(fields.length!=7)throw new IllegalStateException("SU1_DAILY_REPLAY_ROW");int hit=Integer.parseInt(fields[5]);if(hit<0||hit>1)throw new IllegalStateException("SU1_DAILY_REPLAY_LABEL");sum+=hit;}if(rows.length!=n||sum!=k||!id.equals(restored.get("strategy"))||((Number)restored.get("day")).intValue()!=day)throw new IllegalStateException("SU1_DAILY_REPLAY_AGGREGATE");String strategy=id;int key=day,count=n,hits=k;
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SU1_COMPACT_VERIFIED_DAILY",db->{
            try(Cursor c=db.rawQuery("SELECT count(*),coalesce(sum(hit),0) FROM su1_live_shadow WHERE strategy_id=? AND day_key=?",new String[]{strategy,String.valueOf(key)})){c.moveToFirst();if(c.getInt(0)!=count||c.getInt(1)!=hits)throw new IllegalStateException("SU1_DAILY_COMPACTION_CAS");}
            db.execSQL("DELETE FROM su1_live_shadow WHERE strategy_id=? AND day_key=?",new Object[]{strategy,key});db.execSQL("UPDATE su1_live_day_archive SET compacted_at=? WHERE strategy_id=? AND day_key=? AND compacted_at IS NULL",new Object[]{System.currentTimeMillis(),strategy,key});return 0;
        });
    }
    public static void maintain(DbStore store){
        prepareDaily(store);compactDaily(store);
        long epoch=clock(store.readable());Set<String> roots=roots(store.readable());if(epoch!=clock(store.readable()))return;long now=System.currentTimeMillis();store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SU1_TRANSITIVE_PINS",db->{if(clock(db)!=epoch)return 0;pin(db,roots,now);return 0;});
        long local;try(Cursor c=store.readable().rawQuery("SELECT coalesce(sum(byte_count),0) FROM su1_blob WHERE storage_state='LOCAL'",null)){c.moveToFirst();local=c.getLong(0);}if(local<LOCAL_LIMIT&&!StrategyRepositorySU1.exists(store.readable(),"SELECT 1 FROM su1_live_day_archive a JOIN su1_blob b ON b.blob_hash=a.payload_hash WHERE b.storage_state='LOCAL'"))return;
        List<Entry> entries=new ArrayList<>();int total=0;try(Cursor c=store.readable().rawQuery("SELECT blob_hash,codec,local_locator FROM su1_blob b WHERE storage_state='LOCAL' AND (created_at<? OR codec='SU1_LIVE_DAY_V1') AND NOT EXISTS(SELECT 1 FROM su1_retention_pin p WHERE p.object_kind='BLOB' AND p.object_id=b.blob_hash AND p.released_at IS NULL) ORDER BY CASE codec WHEN 'SU1_LIVE_DAY_V1' THEN 0 ELSE 1 END,created_at LIMIT 8",new String[]{String.valueOf(now-86400000)})){while(c.moveToNext()){Entry e=new Entry(c.getString(0),c.getString(1),c.getString(2));if(total+e.bytes>4194304)break;entries.add(e);total+=e.bytes;}}if(entries.isEmpty())return;
        String graph=HashingSU1.text(String.join("|",roots)),json=manifest(entries,graph),hash=HashingSU1.text(json),receipt=HashingSU1.fields("SU1_ARCHIVE_RECEIPT_V1",hash,graph);File dir=directory(store.readable()),dest=new File(dir,hash+".su1a"),temp=new File(dir,hash+".partial");
        try{
            if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("SU1_ARCHIVE_MKDIR");if(!dest.isFile()){write(temp,entries);if(!temp.renameTo(dest))throw new IOException("SU1_ARCHIVE_RENAME");}
            List<Entry> replay=readFile(dest);if(!json.equals(manifest(replay,graph)))throw new IOException("SU1_ARCHIVE_REPLAY_HASH");
            // Re-execute archived grammar definitions on a fixed valid causal prefix,
            // and compare the restored program with the original byte-for-byte execution.
            int[][] fixture=new int[64][10];for(int t=0;t<64;t++)for(int p=0;p<10;p++)fixture[t][p]=(t+p)%10+1;
            for(int i=0;i<entries.size();i++){Entry a=entries.get(i),b=replay.get(i);if(!a.payload.equals(b.payload))throw new IOException("SU1_ARCHIVE_REPLAY_BYTES");if(a.codec.equals("SU1_GRAMMAR_TEXT_V1")){String left=ProgramTextSU1.canonical(a.payload),right=ProgramTextSU1.canonical(b.payload);if(!left.equals(right))throw new IOException("SU1_ARCHIVE_PROGRAM_REPLAY");try(AtomLiftSU1 atoms=new AtomLiftSU1(fixture)){double[][] x=ProgramTreeSU1.parse(left).score(new ProgramTreeSU1.Context(fixture,64,atoms,new ScoreHistoryCacheSU1(64),new BudgetTokenSU1(250,1000000,SettlementExecutionGateR819::isActive)));double[][] y=ProgramTreeSU1.parse(right).score(new ProgramTreeSU1.Context(fixture,64,atoms,new ScoreHistoryCacheSU1(64),new BudgetTokenSU1(250,1000000,SettlementExecutionGateR819::isActive)));if(!Arrays.deepEquals(x,y))throw new IOException("SU1_ARCHIVE_PROGRAM_REPLAY_VALUES");}}}
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SU1_VERIFIED_ARCHIVE_COMMIT",db->{
                if(clock(db)!=epoch)return 0;for(Entry e:entries)if(roots.contains(e.hash))return 0;
                if(!StrategyRepositorySU1.exists(db,"SELECT 1 FROM su1_archive_receipt WHERE receipt_id=?",receipt))StrategyRepositorySU1.row(db,"su1_archive_receipt","receipt_id",receipt,"archive_locator",dest.getName(),"manifest_hash",hash,"dependency_graph_hash",graph,"verified_at",now,"replay_verified",1,"manifest_json",json);
                for(Entry e:entries){db.execSQL("UPDATE su1_blob SET storage_state='ARCHIVED',local_locator=NULL,archive_receipt_id=? WHERE blob_hash=? AND storage_state='LOCAL' AND local_locator=?",new Object[]{receipt,e.hash,e.payload});if(StrategyRepositorySU1.count(db,"SELECT changes()")!=1)throw new IllegalStateException("SU1_ARCHIVE_CONTENT_CAS");}return 0;
            });
        }catch(IOException invalid){throw new IllegalStateException("SU1_ARCHIVE_FAILED",invalid);}finally{if(temp.exists()&&!temp.delete())android.util.Log.w("STARCORE_SU1","archive temporary file retained");}
    }
}
