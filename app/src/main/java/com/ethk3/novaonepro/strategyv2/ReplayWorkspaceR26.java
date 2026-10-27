package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

/** Durable member outputs for the original generation engine. Each 64-label chunk commits once.
 * Paused computation is private; process restart restores the last committed model/calibrator and output prefix. */
public final class ReplayWorkspaceR26 implements ModelReplaySU2.Library.DurableOutputs {
    private static final int CHUNK=64,MAGIC=0x26575201;
    private final DbStore store;private final String campaign,source;private final long sourceThrough;
    public ReplayWorkspaceR26(DbStore store,String campaign,String source,long through){this.store=store;this.campaign=campaign;this.source=source;sourceThrough=through;}
    private ModelGenomeSU2.Member bound(ModelGenomeSU2.Member member){return new ModelGenomeSU2(Collections.singletonList(member),"STATIC",false,"MASS",.15,.2).bindBirth(source,sourceThrough).members.get(0);}
    private String key(ModelGenomeSU2.Member member,boolean calibrated){return HashingSU1.fields("R26_REPLAY_MEMBER_V1",source,ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256,member.modelKey(),calibrated?"CAL":"RAW");}
    private static final class Checkpoint {int cursor;byte[] ref;String hash;}
    private Checkpoint checkpoint(String key){try(Cursor c=store.readable().rawQuery("SELECT cursor,state,state_hash FROM r26_member_replay WHERE campaign_id=? AND cache_key=?",new String[]{campaign,key})){if(!c.moveToFirst())return null;Checkpoint p=new Checkpoint();p.cursor=c.getInt(0);p.ref=c.getBlob(1);p.hash=c.getString(2);return p;}}
    private void ensure(ModelGenomeSU2.Member member,String key,boolean cal,int n){
        String canonical=new ModelGenomeSU2(Collections.singletonList(member),"STATIC",false,"MASS",.15,.2).text();
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_REPLAY_REGISTER",db->{db.execSQL("INSERT OR IGNORE INTO r26_member_replay(campaign_id,cache_key,member_text,calibrated,total_samples,numerical_abi) VALUES(?,?,?,?,?,?)",new Object[]{campaign,key,canonical,cal?1:0,n,ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256});return 0;});
    }
    private static byte[] modelState(String key,int cursor,OnlineModelsSU2.Model model,CalibrationSU2.Live cal)throws IOException {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(DataOutputStream out=new DataOutputStream(new GZIPOutputStream(bytes))){out.writeInt(MAGIC);out.writeUTF(key);out.writeInt(cursor);out.writeBoolean(cal!=null);if(cal!=null)cal.write(out);else model.write(out);}return bytes.toByteArray();
    }
    private void restore(String key,Checkpoint state,OnlineModelsSU2.Model model,CalibrationSU2.Live cal)throws IOException {
        if(state==null||state.cursor<0||state.cursor>5000)throw new IOException("R26_REPLAY_STATE_MISSING");if(state.cursor==0&&state.ref==null)return;if(state.ref==null)throw new IOException("R26_REPLAY_STATE_MISSING");byte[] bytes=CheckpointArtifactsR26.resolve(store,state.ref);if(!LiveStateR26.hash(bytes).equals(state.hash))throw new IOException("R26_REPLAY_STATE_HASH");
        try(DataInputStream in=new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(bytes)))){if(in.readInt()!=MAGIC||!key.equals(in.readUTF())||in.readInt()!=state.cursor||in.readBoolean()!=(cal!=null))throw new IOException("R26_REPLAY_STATE_IDENTITY");if(cal!=null)cal.read(in);else model.read(in);if(in.read()!=-1)throw new IOException("R26_REPLAY_STATE_TRAILING");}
    }
    private static byte[] output(String key,OnlineModelsSU2.Output out,int a,int b)throws IOException {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(DataOutputStream data=new DataOutputStream(new GZIPOutputStream(bytes))){data.writeInt(MAGIC);data.writeUTF(key);data.writeInt(a);data.writeInt(b);data.writeInt(out.state[0].length);for(int t=a;t<b;t++){ExecutionBudgetR26.poll();for(int p=0;p<10;p++)for(int n=0;n<10;n++)data.writeFloat(out.p[t][p][n]);for(float v:out.state[t])data.writeFloat(v);}}return bytes.toByteArray();
    }
    private void prefix(String key,OnlineModelsSU2.Output out,int through)throws IOException {
        if(through<0||through>out.p.length)throw new IOException("R26_REPLAY_PREFIX_CAPACITY");int cursor=0;
        try(Cursor c=store.readable().rawQuery("SELECT from_sample,to_sample,artifact_ref FROM r26_replay_output_chunk WHERE campaign_id=? AND cache_key=? ORDER BY from_sample",new String[]{campaign,key})){while(c.moveToNext()){
            int from=c.getInt(0),to=c.getInt(1);if(from!=cursor||to<=from||to>through)throw new IOException("R26_REPLAY_CHUNK_CALENDAR");byte[] bytes=CheckpointArtifactsR26.resolve(store,c.getBlob(2));try(DataInputStream in=new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(bytes)))){
                if(in.readInt()!=MAGIC||!key.equals(in.readUTF())||in.readInt()!=from||in.readInt()!=to||in.readInt()!=out.state[0].length)throw new IOException("R26_REPLAY_CHUNK_IDENTITY");for(int t=from;t<to;t++){ExecutionBudgetR26.poll();for(int p=0;p<10;p++){double sum=0;for(int n=0;n<10;n++){float q=in.readFloat();if(!Float.isFinite(q)||q<0||q>1.00001)throw new IOException("R26_REPLAY_OUTPUT_PROBABILITY");out.p[t][p][n]=q;sum+=q;}if(Math.abs(sum-1)>1e-4)throw new IOException("R26_REPLAY_OUTPUT_ROW");}for(int j=0;j<out.state[t].length;j++){float v=in.readFloat();if(!Float.isFinite(v))throw new IOException("R26_REPLAY_OUTPUT_FEATURE");out.state[t][j]=v;}}if(in.read()!=-1)throw new IOException("R26_REPLAY_CHUNK_TRAILING");}cursor=to;
        }}if(cursor!=through)throw new IOException("R26_REPLAY_CHUNK_MISSING");
    }
    private void commit(String key,OnlineModelsSU2.Output out,int from,int through,OnlineModelsSU2.Model model,CalibrationSU2.Live cal)throws IOException {
        byte[] modelBytes=modelState(key,through,model,cal),chunkBytes=output(key,out,from,through);String stateHash=LiveStateR26.hash(modelBytes),checkpoint=HashingSU1.fields("R26_REPLAY_MODEL_CHECKPOINT_V1",campaign,key,String.valueOf(through),stateHash);
        try(CheckpointArtifactsR26.Prepared state=CheckpointArtifactsR26.prepareLease(store,modelBytes);CheckpointArtifactsR26.Prepared chunk=CheckpointArtifactsR26.prepareLease(store,chunkBytes)){
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_REPLAY_CHUNK_COMMIT",db->{
                db.execSQL("UPDATE r26_member_replay SET cursor=?,state=?,state_hash=?,checkpoint_hash=?,status=CASE WHEN ?=total_samples THEN 'DONE' ELSE 'RUNNING' END WHERE campaign_id=? AND cache_key=? AND cursor=?",new Object[]{through,state.reference(),stateHash,checkpoint,through,campaign,key,from});
                try(Cursor c=db.rawQuery("SELECT changes()",null)){if(!c.moveToFirst()||c.getInt(0)!=1)throw new IllegalStateException("R26_REPLAY_SAMPLE_CAS");}
                db.execSQL("INSERT INTO r26_replay_output_chunk VALUES(?,?,?,?,?,?)",new Object[]{campaign,key,from/CHUNK,from,through,chunk.reference()});
                CheckpointManifestR26.record(db,checkpoint,key,source,"",sourceThrough,state.reference());
                db.execSQL("INSERT OR IGNORE INTO r26_work_total VALUES('legacy-generation',0)");db.execSQL("UPDATE r26_work_total SET completed=completed+? WHERE task_id='legacy-generation'",new Object[]{through-from});
                db.execSQL("UPDATE r26_legacy_campaign SET committed_samples=committed_samples+?,phase=?,updated_at=? WHERE campaign_id=?",new Object[]{through-from,cal==null?"MEMBER_LEARNING":"MEMBER_CALIBRATION",System.currentTimeMillis(),campaign});return 0;});
        }
    }
    @Override public OnlineModelsSU2.Output raw(ModelGenomeSU2.Member member,int[][] draws){
        member=bound(member);String key=key(member,false);ensure(member,key,false,draws.length);OnlineModelsSU2.Model model=ModelReplaySU2.build(member.kind,member.params);OnlineModelsSU2.Output out=new OnlineModelsSU2.Output(draws.length,model.stateWidth());Checkpoint state=checkpoint(key);
        try{restore(key,state,model,null);prefix(key,out,state.cursor);int from=state.cursor;
            for(int t=state.cursor;t<draws.length;t++){ExecutionBudgetR26.poll();model.predict(out.p[t],out.state[t]);model.update(draws[t]);if((t+1)%CHUNK==0||t+1==draws.length){commit(key,out,from,t+1,model,null);from=t+1;}}
            return out;
        }catch(IOException failure){throw new IllegalStateException("R26_DURABLE_RAW_REPLAY",failure);}
    }
    @Override public OnlineModelsSU2.Output calibrated(ModelGenomeSU2.Member member,int[][] draws,OnlineModelsSU2.Output raw){
        // Raw outputs are durable independently; calibrated trajectories retain their own complete fitting state.
        member=bound(member);String key=key(member,true);ensure(member,key,true,draws.length);CalibrationSU2.Live cal=new CalibrationSU2.Live();OnlineModelsSU2.Output out=new OnlineModelsSU2.Output(draws.length,raw.state[0].length);Checkpoint state=checkpoint(key);
        try{restore(key,state,null,cal);prefix(key,out,state.cursor);int from=state.cursor;
            for(int t=state.cursor;t<draws.length;t++){ExecutionBudgetR26.poll();float[][] q=cal.predict(raw.p[t]);for(int p=0;p<10;p++)System.arraycopy(q[p],0,out.p[t][p],0,10);System.arraycopy(raw.state[t],0,out.state[t],0,raw.state[t].length);cal.settle(raw.p[t],draws[t]);if((t+1)%CHUNK==0||t+1==draws.length){commit(key,out,from,t+1,null,cal);from=t+1;}}
            return out;
        }catch(IOException failure){throw new IllegalStateException("R26_DURABLE_CALIBRATED_REPLAY",failure);}
    }
    @Override public OnlineModelsSU2.Model trained(ModelGenomeSU2.Member member,int through){
        member=bound(member);String key=key(member,false);Checkpoint state=checkpoint(key);if(state==null||state.cursor!=through)throw new IllegalStateException("R26_REPLAY_TRAINED_CUTOFF");OnlineModelsSU2.Model model=ModelReplaySU2.build(member.kind,member.params);try{restore(key,state,model,null);return model;}catch(IOException failure){throw new IllegalStateException("R26_REPLAY_TRAINED_STATE",failure);}
    }
    @Override public CalibrationSU2.Live calibrator(ModelGenomeSU2.Member member,int through){
        member=bound(member);String key=key(member,true);Checkpoint state=checkpoint(key);if(state==null||state.cursor!=through)throw new IllegalStateException("R26_REPLAY_CALIBRATOR_CUTOFF");CalibrationSU2.Live cal=new CalibrationSU2.Live();try{restore(key,state,null,cal);return cal;}catch(IOException failure){throw new IllegalStateException("R26_REPLAY_CALIBRATOR_STATE",failure);}
    }
}
