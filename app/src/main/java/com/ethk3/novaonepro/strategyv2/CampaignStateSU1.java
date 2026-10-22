package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import com.ethk3.novaonepro.r952.StrictCanonicalJson;
import java.io.*;
import java.util.*;
import java.util.zip.*;

/** Binds a resumable foundry to an immutable dataset and the single database writer. */
public final class CampaignStateSU1 implements AutoCloseable {
    public final DbStore store;
    public final String campaign,datasetBlob;
    public final long through;
    public final StrategyFoundrySU1 foundry;
    private long generation;
    private String oldHash;
    private CampaignStateSU1(DbStore store,String campaign,String datasetBlob,long through,StrategyFoundrySU1 f,long generation,String hash){this.store=store;this.campaign=campaign;this.datasetBlob=datasetBlob;this.through=through;foundry=f;this.generation=generation;oldHash=hash;}
    private static String dataset(List<Models.Draw> rows){
        try{ByteArrayOutputStream raw=new ByteArrayOutputStream();try(DataOutputStream out=new DataOutputStream(new GZIPOutputStream(raw))){out.writeInt(0x53553144);out.writeInt(rows.size());for(Models.Draw row:rows){out.writeUTF(row.issue);out.writeLong(row.openTime);out.writeLong(row.fetchTime);for(int p=1;p<=10;p++)out.writeByte(row.getNumberAtPosition(p));}}return Base64.getEncoder().encodeToString(raw.toByteArray());}
        catch(IOException impossible){throw new IllegalStateException(impossible);}
    }
    private static int[][] unpack(String payload){
        if(payload==null||payload.length()>1048576)throw new IllegalArgumentException("SU1_DATASET_BYTES");
        try(DataInputStream in=new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(payload))))){
            if(in.readInt()!=0x53553144)throw new IOException("SU1_DATASET_VERSION");int n=in.readInt();if(n<3400||n>20000)throw new IOException("SU1_DATASET_ROWS");int[][] d=new int[n][10];String prior=null;
            for(int i=0;i<n;i++){String issue=in.readUTF();long open=in.readLong(),available=in.readLong();if(!TargetIssueResolver.isValid(issue)||prior!=null&&!TargetIssueResolver.exact(prior,issue).valid||open<=0||available<=0)throw new IOException("SU1_DATASET_SOURCE");prior=issue;for(int p=0;p<10;p++)d[i][p]=in.readUnsignedByte();RelationalStateInference.validateDraw(d[i]);}if(in.read()!=-1)throw new IOException("SU1_DATASET_TAIL");return d;
        }catch(IOException invalid){throw new IllegalArgumentException("SU1_DATASET_CORRUPT",invalid);}
    }
    public static CampaignStateSU1 open(DbStore store){
        if(!ArchiveStoreSU1.canCreate(store.readable()))return null;
        String wire=null,expected=null;long gen=-1;
        try(Cursor c=store.readable().rawQuery("SELECT generation,cursor_json,state_hash FROM su1_burst_checkpoint WHERE singleton=1",null)){if(c.moveToFirst()){gen=c.getLong(0);wire=c.getString(1);expected=c.getString(2);}}
        if(wire!=null){
            if(!HashingSU1.text(wire).equals(expected))throw new IllegalStateException("SU1_CHECKPOINT_HASH");Map<String,Object> state=StrictCanonicalJson.readContextObject(wire);String campaign=(String)state.get("campaign"),blob=(String)state.get("dataset");long through=((Number)state.get("through")).longValue();
            if(!TrainingCodeIdentityR964.SHA256.equals(state.get("kernel")))store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SU1_INVALIDATE_CHECKPOINT_KERNEL",db->{db.execSQL("UPDATE su1_campaign SET status='FAILED',finished_at=? WHERE campaign_id=? AND status='RUNNING'",new Object[]{System.currentTimeMillis(),campaign});return 0;});
            try(Cursor status=store.readable().rawQuery("SELECT status FROM su1_campaign WHERE campaign_id=?",new String[]{campaign})){if(status.moveToFirst()&&status.getString(0).equals("RUNNING")){
                String payload=localBlob(store.readable(),blob);StrategyFoundrySU1 f=StrategyFoundrySU1.restore(unpack(payload),HashingSU1.text(payload),(String)state.get("session"));
                List<String> seen=new ArrayList<>();try(Cursor c=store.readable().rawQuery("SELECT genome_key FROM su1_search_attempt WHERE campaign_id=? AND result='EVALUATED'",new String[]{campaign})){while(c.moveToNext())seen.add(c.getString(0));}f.restoreSeen(seen);DirectorLiveFeedbackSU1.load(store.readable(),f.director);return new CampaignStateSU1(store,campaign,blob,through,f,gen,expected);
            }}
        }
        List<Models.Draw> rows=new ArrayList<>(store.getRecentDraws(5000));Collections.reverse(rows);if(rows.size()<3400)return null;
        String previous=null;for(Models.Draw r:rows){if(r==null||!r.isValid()||r.dirty!=0||r.fetchTime<=0||r.fetchTime>System.currentTimeMillis()||previous!=null&&!TargetIssueResolver.exact(previous,r.issue).valid)throw new IllegalStateException("SU1_HISTORY_NOT_CAUSAL_CONSECUTIVE");previous=r.issue;}
        long through=LiveShadowLoopSU1.sequence(rows.get(rows.size()-1).issue);
        try(Cursor c=store.readable().rawQuery("SELECT max(data_through_sequence) FROM su1_campaign WHERE kind='LIVE_REFRESH' AND status='DONE' AND config_json LIKE '%CONTINUOUS_CREATION%'",null)){if(c.moveToFirst()&&!c.isNull(0)&&through-c.getLong(0)<300)return null;}
        String payload=dataset(rows),hash=HashingSU1.text(payload);int[][] d=unpack(payload);StrategyFoundrySU1 f=new StrategyFoundrySU1(d,hash,through,24,3);
        // Only the continuing REAL research lineage inherits memory. Control runs never read this table.
        try(Cursor c=store.readable().rawQuery("SELECT state_json,state_hash FROM su1_director_state s JOIN su1_campaign c ON c.campaign_id=s.campaign_id ORDER BY c.finished_at DESC LIMIT 1",null)){if(c.moveToFirst()){String memory=c.getString(0);if(!HashingSU1.text(memory).equals(c.getString(1)))throw new IllegalStateException("SU1_MEMORY_HASH");f.inheritMemory(memory);}}
        DirectorLiveFeedbackSU1.load(store.readable(),f.director);
        String config="{\"kind\":\"CONTINUOUS_CREATION\",\"population\":24,\"generations\":3,\"atomCondition\":\"V2_1E12\",\"kernel\":\""+TrainingCodeIdentityR964.SHA256+"\"}";
        String campaign=store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SU1_CAMPAIGN_START",db->{String id=StrategyRepositorySU1.beginCampaign(db,"LIVE_REFRESH",through,config,System.currentTimeMillis());StrategyRepositorySU1.blob(db,payload,"SU1_DATASET_GZIP_BASE64_V1",System.currentTimeMillis());return id;});
        return new CampaignStateSU1(store,campaign,hash,through,f,gen,expected);
    }
    public static String localBlob(SQLiteDatabase db,String hash){try(Cursor c=db.rawQuery("SELECT storage_state,local_locator FROM su1_blob WHERE blob_hash=?",new String[]{HashingSU1.sha(hash)})){if(!c.moveToFirst())throw new IllegalStateException("SU1_BLOB_REQUIRED");if("ARCHIVED".equals(c.getString(0)))return ArchiveStoreSU1.read(db,hash);String p=c.getString(1);if(!HashingSU1.text(p).equals(hash))throw new IllegalStateException("SU1_BLOB_HASH");return p;}}
    public void checkpoint(StrategyFoundrySU1.Attempt attempt){
        Map<String,Object> state=new TreeMap<>();state.put("campaign",campaign);state.put("dataset",datasetBlob);state.put("through",through);state.put("kernel",TrainingCodeIdentityR964.SHA256);state.put("session",foundry.checkpoint());
        String json=StrictCanonicalJson.writeContext(state);if(json.length()>65000)throw new IllegalStateException("SU1_CHECKPOINT_BYTES");String hash=HashingSU1.text(json);long now=System.currentTimeMillis();Models.Draw latest=store.getLatestDraw();
        String experience=attempt==null?null:foundry.knowledge(attempt);
        long cost=attempt==null?0:ProgramContractSU1.compile(ProgramTextSU1.parse(attempt.genome.text)).operationUnits;
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SU1_FOUNDRY_COMMIT",db->{
            if(generation<0){if(StrategyRepositorySU1.exists(db,"SELECT 1 FROM su1_burst_checkpoint WHERE singleton=1"))throw new IllegalStateException("SU1_CHECKPOINT_CAS");db.execSQL("INSERT INTO su1_burst_checkpoint VALUES(1,0,?,?,?,?)",new Object[]{datasetBlob,json,hash,now});}
            else{db.execSQL("UPDATE su1_burst_checkpoint SET generation=generation+1,dataset_hash=?,cursor_json=?,state_hash=?,updated_at=? WHERE singleton=1 AND generation=? AND state_hash=?",new Object[]{datasetBlob,json,hash,now,generation,oldHash});if(StrategyRepositorySU1.count(db,"SELECT changes()")!=1)throw new IllegalStateException("SU1_CHECKPOINT_CAS");}
            if(attempt!=null){
                StrategyFoundrySU1.Genome g=attempt.genome;String id=HashingSU1.fields(campaign,String.valueOf(foundry.phase),String.valueOf(foundry.generation),String.valueOf(foundry.index),g.key());
                if(!StrategyRepositorySU1.exists(db,"SELECT 1 FROM su1_search_attempt WHERE attempt_id=?",id))db.execSQL("INSERT INTO su1_search_attempt(attempt_id,genome_key,campaign_id,program_text,arm,operator,parent_id,dev_fitness,val_fitness,result,detail,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{id,g.key(),campaign,g.text,g.arm,g.operator,g.parent,attempt.ranked==null?null:attempt.ranked.fitness,attempt.ranked==null?null:attempt.ranked.valFitness,attempt.result,attempt.detail,now});
                if(!StrategyRepositorySU1.exists(db,"SELECT 1 FROM su1_research_knowledge WHERE attempt_id=?",id)){
                    String payload=StrategyRepositorySU1.blob(db,experience,"SU1_RESEARCH_EXPERIENCE_V1",now);
                    StrategyRepositorySU1.row(db,"su1_research_knowledge","attempt_id",id,"context_key",foundry.researchContext.key,"hypothesis",g.arm,"action",g.operator,"cost_units",cost,"result",attempt.result,"payload_hash",payload,"recorded_at",now);
                }
                if(attempt.ranked!=null&&attempt.ranked.fitness>-1&&attempt.ranked.valFitness>-1){
                    boolean useful=StrategyRepositorySU1.count(db,"SELECT count(*) FROM su1_research_pool WHERE state='ACTIVE'")<32;
                    if(!useful)try(Cursor c=db.rawQuery("SELECT min(prior_fitness) FROM su1_research_pool WHERE state='ACTIVE'",null)){c.moveToFirst();useful=attempt.ranked.fitness>c.getDouble(0);}
                    String parent=StrategyRepositorySU1.findParent(db,g.parent);if(useful&&(g.parent.isEmpty()||parent!=null))StrategyRepositorySU1.candidate(db,g.text,g.coverage,g.margin,g.arm,parent,campaign,through,datasetBlob,attempt.ranked.fitness,now);
                }
            }
            for(StrategyFoundrySU1.Curve curve:foundry.curves())if(!StrategyRepositorySU1.exists(db,"SELECT 1 FROM su1_campaign_curve WHERE campaign_id=? AND generation=?",campaign,String.valueOf(curve.generation)))StrategyRepositorySU1.row(db,"su1_campaign_curve","campaign_id",campaign,"generation",curve.generation,"dev_fitness",curve.dev,"champion_val_fitness",curve.val,"champion_family",curve.family,"recorded_at",now);
            if(foundry.done()){
                SealedPlannerSU1.plan(db,this,now,latest);persistMacros(db,now);String memory=foundry.memory();db.execSQL("INSERT INTO su1_director_state VALUES(?,?,?)",new Object[]{campaign,memory,HashingSU1.text(memory)});
                db.execSQL("UPDATE su1_campaign SET status='DONE',finished_at=?,programs_evaluated=? WHERE campaign_id=? AND status='RUNNING'",new Object[]{now,foundry.evaluated,campaign});
            }
            return 0;
        });
        generation=generation<0?0:generation+1;oldHash=hash;
    }
    private void persistMacros(SQLiteDatabase db,long now){
        Set<String> active=new HashSet<>();
        for(MacroLibrarySU1.Macro m:foundry.macros.all()){
            active.add(m.id);
            if(!StrategyRepositorySU1.exists(db,"SELECT 1 FROM su1_macro WHERE macro_id=?",m.id))db.execSQL("INSERT INTO su1_macro VALUES(?,?,?,?,?,?,'ACTIVE')",new Object[]{m.id,m.text,m.support,m.fitness,campaign,campaign});
            else db.execSQL("UPDATE su1_macro SET support=max(support,?),best_fitness=max(best_fitness,?),last_campaign=? WHERE macro_id=?",new Object[]{m.support,m.fitness,campaign,m.id});
            if(m.support>=3&&!StrategyRepositorySU1.exists(db,"SELECT 1 FROM su1_emergent_family WHERE macro_id=?",m.id))db.execSQL("INSERT INTO su1_emergent_family VALUES(?,?,?,?)",new Object[]{"EMERGENT:"+m.id,m.id,campaign,now});
        }
        List<String> retire=new ArrayList<>();try(Cursor c=db.rawQuery("SELECT macro_id FROM su1_macro WHERE state='ACTIVE'",null)){while(c.moveToNext())if(!active.contains(c.getString(0)))retire.add(c.getString(0));}
        for(String id:retire)db.execSQL("UPDATE su1_macro SET state='RETIRED' WHERE macro_id=? AND state='ACTIVE'",new Object[]{id});
    }
    @Override public void close(){foundry.close();}
}
