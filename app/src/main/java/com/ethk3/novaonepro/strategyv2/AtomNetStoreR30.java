package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.*;
import java.util.*;
import java.util.zip.*;

/** Additive, source-keyed input cache and versioned learner state on the existing database/writer. */
public final class AtomNetStoreR30 {
    private AtomNetStoreR30(){}
    public static final String[] DDL={
        "CREATE TABLE IF NOT EXISTS r30_atomnet_forecast(target_issue TEXT NOT NULL,position INTEGER NOT NULL,model_hash TEXT NOT NULL,trained_through_ordinal INTEGER NOT NULL,p_class_json TEXT NOT NULL,p_hit REAL NOT NULL,frozen_at INTEGER NOT NULL,deadline_at INTEGER NOT NULL,PRIMARY KEY(target_issue,position))",
        "CREATE TABLE IF NOT EXISTS r30_atomnet_dataset(dataset_hash TEXT PRIMARY KEY,payload BLOB NOT NULL,payload_hash TEXT NOT NULL,recorded_at INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS r30_atomnet_sample(dataset_hash TEXT NOT NULL,sample_index INTEGER NOT NULL,issue TEXT NOT NULL,position INTEGER NOT NULL,features BLOB NOT NULL,hit INTEGER NOT NULL,number_class INTEGER NOT NULL,PRIMARY KEY(dataset_hash,sample_index))"
    };
    public static void dataset(ResearchWriteQueueR27.Batch b,ReplayDatasetR27 data,long now){
        try{
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            try(DataOutputStream o=new DataOutputStream(new GZIPOutputStream(bytes))){if(!data.legacyIdentityR31)o.write(data.originalBytesR31());else{o.writeInt(data.size());for(int i=0;i<data.size();i++){o.writeUTF(data.issues[i]);o.writeLong(data.ordinals[i]);for(int n:data.draws[i])o.writeByte(n);}}}
            byte[] blob=bytes.toByteArray();b.add("INSERT OR IGNORE INTO r30_atomnet_dataset VALUES(?,?,?,?)",data.datasetHash,blob,StatsR27.sha256(blob),now);
        }catch(IOException e){throw new IllegalStateException("R30_TRAIN_DATASET_ENCODE",e);}
    }
    public static ReplayDatasetR27 dataset(SQLiteDatabase ro,String hash){
        try(Cursor c=ro.rawQuery("SELECT payload,payload_hash FROM r30_atomnet_dataset WHERE dataset_hash=?",new String[]{hash})){
            if(!c.moveToFirst())throw new IllegalStateException("R30_TRAIN_DATASET_MISSING");byte[] bytes=CheckpointArtifactsR26.resolveSerializedR31(ro,c.getBlob(0));
            if(!StatsR27.sha256(bytes).equals(c.getString(1)))throw new IllegalStateException("R30_TRAIN_DATASET_CHECKSUM");
            try(DataInputStream d=new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(bytes)))){
                int n=d.readInt();ReplayDatasetR27 result;
                if(n==ReplayDatasetR27.ORIGINAL_MAGIC_R31){ByteArrayOutputStream raw=new ByteArrayOutputStream();new DataOutputStream(raw).writeInt(n);byte[] chunk=new byte[8192];int count;while((count=d.read(chunk))!=-1){if(count>ReplayDatasetR27.MAX_ORIGINAL_BYTES_R31-raw.size())throw new IOException("original bound");raw.write(chunk,0,count);}result=ReplayDatasetR27.originalBytesR31(raw.toByteArray());}
                else{if(n<1||n>100_000)throw new IOException("rows");List<ReplayDatasetR27.Row> rows=new ArrayList<>();for(int i=0;i<n;i++){String issue=d.readUTF();long ordinal=d.readLong();int[] draw=new int[10];for(int p=0;p<10;p++)draw[p]=d.readUnsignedByte();rows.add(new ReplayDatasetR27.Row(issue,ordinal,draw));}result=ReplayDatasetR27.of(rows);}
                if(d.read()!=-1||!result.datasetHash.equals(hash))throw new IOException("content hash");return result;
            }catch(IOException e){throw new IllegalStateException("R30_TRAIN_DATASET_DECODE",e);}
        }
    }
    static byte[] features(double[] x){
        try{ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream o=new DataOutputStream(bytes);for(double v:x)o.writeDouble(v);o.close();return bytes.toByteArray();}
        catch(IOException e){throw new IllegalStateException(e);}
    }
    public static void sample(ResearchWriteQueueR27.Batch b,String dataset,int index,String issue,int position,double[] x,int y,int cls){
        b.add("INSERT OR IGNORE INTO r30_atomnet_sample VALUES(?,?,?,?,?,?,?)",dataset,index,issue,position,features(x),y,cls);
    }
    public static AtomNetSessionR30.Samples samples(SQLiteDatabase ro,AtomNetSessionR30 session){
        return new AtomNetSessionR30.Samples(){
            final Map<Integer,AtomNetTrainerR27.Sample> prepared=new HashMap<>();
            public void prefetch(int[] indices){
                if(indices.length>64)throw new IllegalArgumentException("R30_SAMPLE_PREFETCH_BOUND");
                prepared.clear();if(indices.length==0)return;
                LinkedHashSet<Integer> unique=new LinkedHashSet<>();for(int i:indices){if(i<0||i>=session.materializedSamples||i>=session.trainCount+session.selectCount)throw new IllegalArgumentException("R30_SAMPLE_PROTECTED_OR_OUTSIDE_RESEARCH_SPLITS");unique.add(i);}
                StringBuilder slots=new StringBuilder();for(int i:unique){if(slots.length()>0)slots.append(',');slots.append('?');}
                StringBuilder sql=new StringBuilder("SELECT sample_index,issue,position,features,hit,number_class,NULL FROM r30_atomnet_sample WHERE dataset_hash=? AND sample_index IN (");
                boolean publishedSource=publicationSchemaPresentR31(ro);String[] args=new String[(publishedSource?2:1)*(unique.size()+1)];args[0]=session.datasetHash;int at=1;
                for(int i:unique){if(at>1)sql.append(',');sql.append('?');args[at++]=Integer.toString(i);}sql.append(')');if(publishedSource){sql.append(" UNION ALL SELECT s.sample_index,s.issue,s.position,s.features,s.hit,s.number_class,s.payload_hash FROM r31_atomnet_sample_stage s JOIN r31_atomnet_publication p ON p.publication_id=s.publication_id WHERE p.dataset_hash=? AND p.status='PUBLISHED' AND s.sample_index IN (").append(slots).append(')');args[at++]=session.datasetHash;for(int i:unique)args[at++]=Integer.toString(i);}
                try(Cursor c=ro.rawQuery(sql.toString(),args)){while(c.moveToNext()){AtomNetTrainerR27.Sample decoded=decode(c),old=prepared.putIfAbsent(c.getInt(0),decoded);if(old!=null&&(!old.issue.equals(decoded.issue)||old.y!=decoded.y||old.cls!=decoded.cls||!Arrays.equals(old.x,decoded.x)))throw new IllegalStateException("R31_ATOM_SAMPLE_PUBLICATION_CONFLICT");}}
                if(prepared.size()!=unique.size())throw new IllegalStateException("R30_TRAIN_SAMPLE_MISSING_IN_PREFETCH");
            }
            AtomNetTrainerR27.Sample decode(Cursor c){
                byte[] blob=c.getBlob(3);if(!c.isNull(6)&&!c.getString(6).equals(AtomNetPublicationR31.sampleHash(new Object[]{session.datasetHash,c.getInt(0),c.getString(1),c.getInt(2),blob,c.getInt(4),c.getInt(5)})))throw new IllegalStateException("R31_ATOM_SAMPLE_CHECKSUM");if(blob.length!=session.input*8)throw new IllegalStateException("R30_TRAIN_SAMPLE_SHAPE");
                double[] x=new double[session.input];try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(blob))){for(int i=0;i<x.length;i++){x[i]=in.readDouble();if(!Double.isFinite(x[i]))throw new IllegalStateException("R30_TRAIN_SAMPLE_NONFINITE");}}
                catch(IOException e){throw new IllegalStateException(e);}
                int position=c.getInt(2),hit=c.getInt(4),number=c.getInt(5);if(position<1||position>10||hit<0||hit>1||number<0||number>9||position!=Math.floorMod(c.getInt(0),10)+1)throw new IllegalStateException("R30_TRAIN_SAMPLE_IDENTITY");
                return new AtomNetTrainerR27.Sample(x,hit,number,AtomNetSessionR30.eventKey(c.getString(1),position));
            }
            public AtomNetTrainerR27.Sample get(int index){
                AtomNetTrainerR27.Sample s=prepared.get(index);if(s==null){prefetch(new int[]{index});s=prepared.get(index);}return s;
            }
        };
    }

    public static AtomNetSessionR30 restore(SQLiteDatabase ro){
        try(Cursor c=ro.rawQuery("SELECT feature_abi,state_hash,artifact,dataset_hash,trained_through FROM r29_model_checkpoint WHERE owner IN (?,?) ORDER BY CASE WHEN owner=? THEN 0 ELSE 1 END LIMIT 1",new String[]{AtomNetSessionR30.OWNER,AtomNetSessionR30.LEGACY_OWNER,AtomNetSessionR30.OWNER})){
            if(!c.moveToFirst())return null;if(!AtomNetSessionR30.ABI.equals(c.getString(0)))throw new IllegalStateException("R30_TRAIN_FEATURE_ABI");
            AtomNetSessionR30 state=AtomNetSessionR30.decode(ro,c.getBlob(2),c.getString(1));
            if(!state.datasetHash.equals(c.getString(3))||!(state.completeIssue.isEmpty()?state.observedThrough:state.trainedThrough()).equals(c.getString(4)))throw new IllegalStateException("R30_TRAIN_CHECKPOINT_CURSOR");return state;
        }
    }
    public static ModelSurfaceProviderR30.FrozenModelRef publicationModelR31(SQLiteDatabase ro,AtomNetSessionR30 state){
        if(!state.ready())return null;if(publicationSchemaPresentR31(ro))try(Cursor c=ro.rawQuery("SELECT p.model_hash,p.weights_ref,p.through_ordinal,p.model_available_at FROM r31_atomnet_publication p JOIN r29_model_checkpoint c ON c.state_hash=p.state_hash AND c.owner=? WHERE p.status='PUBLISHED' ORDER BY p.published_at DESC LIMIT 1",new String[]{AtomNetSessionR30.OWNER})){if(c.moveToFirst())return ModelSurfaceProviderR30.FrozenModelRef.archived(ro,c.getString(0),c.getBlob(1),c.getLong(2),c.getLong(3));}
        try(Cursor c=ro.rawQuery("SELECT artifact FROM r29_model_checkpoint WHERE owner=?",new String[]{AtomNetSessionR30.OWNER})){if(c.moveToFirst()&&CheckpointArtifactsR26.reference(c.getBlob(0)))throw new AtomNetPublicationR31.Dependency("PUBLISHED_MODEL_MANIFEST_MISSING");}
        return new ModelSurfaceProviderR30.FrozenModelRef(state.trainer.net.encode(),state.trainedThroughOrdinal(),System.currentTimeMillis());
    }
    private static boolean publicationSchemaPresentR31(SQLiteDatabase ro){try(Cursor c=ro.rawQuery("SELECT count(*) FROM sqlite_master WHERE type='table' AND name IN ('r31_atomnet_publication','r31_atomnet_sample_stage')",null)){return c.moveToFirst()&&c.getInt(0)==2;}}
    public static void checkpoint(ResearchWriteQueueR27.Batch b,AtomNetSessionR30 state,long now){checkpointWithIdentity(b,state,now);}
    public static String checkpointWithIdentity(ResearchWriteQueueR27.Batch b,AtomNetSessionR30 state,long now){
        byte[] bytes=state.encode();return checkpointReferenceR31(b,state,StatsR27.sha256(bytes),bytes,now);
    }
    static String checkpointReferenceR31(ResearchWriteQueueR27.Batch b,AtomNetSessionR30 state,String identity,byte[] stored,long now){
        // Exact old bytes/checksum are archived before the first outer-format migration, in an existing immutable table.
        b.add("INSERT OR IGNORE INTO r29_legacy_model_checkpoint(owner,artifact_hash,artifact,trained_through,online_steps,learning_rate,last_check_logloss,archived_at,reason) SELECT owner,state_hash,artifact,trained_through,-1,-1,NULL,?,'R30_REPAIR_ORIGINAL_OUTER_V1_V2_BYTES_EXACT; SIDE_COUNTERS_UNKNOWN_SEE_ORIGINAL_ARTIFACT' FROM r29_model_checkpoint old WHERE owner IN (?,?) AND substr(artifact,1,4) IN (x'30A70001',x'30A70002') AND NOT EXISTS(SELECT 1 FROM r29_legacy_model_checkpoint saved WHERE saved.owner=old.owner AND saved.artifact_hash=old.state_hash)",now,AtomNetSessionR30.OWNER,AtomNetSessionR30.LEGACY_OWNER);
        long work=state.materializedSamples+state.pretrainSamples+state.trainer.onlineSteps;
        long materializeTotal=(long)state.trainCount+state.selectCount+state.holdoutCount;
        Long pending=null;if(state.materializedSamples<materializeTotal)pending=Long.valueOf(materializeTotal-state.materializedSamples);else if(state.phase.equals("TRAIN"))pending=Long.valueOf(Math.max(0,state.trainCount-state.offset)); // Current known sample queue only; no invented future early-stop visits.
        ResearchStoreR30R1.appendOwnedProgress(b,-1,new ResearchStoreR30R1.Facts(work,work,pending,null,state.ready()?state.trainedThroughOrdinal():null,identity));
        b.add("INSERT OR REPLACE INTO r29_model_checkpoint(owner,feature_abi,dataset_hash,trained_through,state_hash,artifact,updated_at) VALUES(?,?,?,?,?,?,?)",AtomNetSessionR30.OWNER,AtomNetSessionR30.ABI,state.datasetHash,state.completeIssue.isEmpty()?state.observedThrough:state.trainedThrough(),identity,stored,now);
        b.add("INSERT OR REPLACE INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES('atomnet','ten-position-r30',?,?)",JsonR27.write(state.report()),now);
        for(AtomNetTrainerR27.Epoch e:state.receipts)b.add("INSERT OR IGNORE INTO r27_atomnet_epoch VALUES(?,?,?,?,?,?)","R30_"+state.datasetHash.substring(0,16),e.epoch,e.split,Double.isFinite(e.logloss)?e.logloss:null,Double.isFinite(e.brier)?e.brier:null,now);
        ResearchProvenanceGraphR30R1.gradient(b,state,identity,now);return identity;
    }
}
