package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.AtomReplayR27;
import java.util.*;

/** Provenance and model consumption use the same existing short transaction as the actual policy freeze. */
public final class ModelSurfaceStoreR30 {
    private ModelSurfaceStoreR30(){}
    public static final String[] DDL={
        "CREATE TABLE IF NOT EXISTS r30_model_artifact(model_hash TEXT PRIMARY KEY,feature_abi TEXT NOT NULL,weights BLOB NOT NULL,trained_through_ordinal INTEGER NOT NULL,available_at INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS r30_model_surface(target_ordinal INTEGER NOT NULL,position INTEGER NOT NULL,model_hash TEXT NOT NULL,history_hash TEXT NOT NULL,probabilities BLOB NOT NULL,probability_hash TEXT NOT NULL,trained_through_ordinal INTEGER NOT NULL,model_available_at INTEGER NOT NULL,feature_through_ordinal INTEGER NOT NULL,feature_available_at INTEGER NOT NULL,prepared_at INTEGER NOT NULL,frozen_at INTEGER NOT NULL,deadline_at INTEGER NOT NULL,PRIMARY KEY(target_ordinal,position,model_hash,history_hash))",
        "CREATE TABLE IF NOT EXISTS r30_model_call(call_key TEXT PRIMARY KEY,policy_hash TEXT NOT NULL,model_hash TEXT,target_ordinal INTEGER NOT NULL,position INTEGER NOT NULL,status TEXT NOT NULL,reason TEXT NOT NULL,probability_hash TEXT,required_count INTEGER NOT NULL,called_at INTEGER NOT NULL,frozen_at INTEGER)"
    };
    public static void surface(ResearchWriteQueueR27.Batch b,ModelSurfaceProviderR30.Surface s,ModelSurfaceProviderR30.FrozenModelRef ref){
        if(ref!=null)b.add("INSERT OR IGNORE INTO r30_model_artifact VALUES(?,?,?,?,?)",ref.modelHash,AtomNetSessionR30.ABI,ref.weights(),ref.trainedThroughOrdinal,ref.availableAt);
        b.add("INSERT OR IGNORE INTO r30_model_surface VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)",s.targetOrdinal,s.position,s.modelHash,s.historyHash,StoreR27.pack(s.probabilities()),s.probabilityHash,s.trainedThroughOrdinal,s.modelAvailableAt,s.featureThroughOrdinal,s.featureAvailableAt,s.preparedAt,s.frozenAt,s.deadlineAt);
    }
    static final class Prepared {
        final Map<Long,ModelSurfaceProviderR30.Surface> rows=new HashMap<>();final Map<Long,String> blocked=new HashMap<>();
        final Map<String,ModelSurfaceProviderR30.FrozenModelRef> models=new HashMap<>();
        void bind(FeatureCacheR27 cache){cache.bindModelSurfaces(ModelSurfaceProviderR30.source(rows,blocked));}
    }
    static Prepared archived(SQLiteDatabase ro,ReplayDatasetR27 data){
        Prepared p=new Prepared();if(data.size()==0)return p;
        Map<Long,Integer> indices=new HashMap<>();Map<Integer,String> hashes=new HashMap<>();for(int i=0;i<data.size();i++)indices.put(data.ordinals[i],i);
        try(Cursor c=ro.rawQuery("SELECT s.target_ordinal,s.position,s.model_hash,s.history_hash,s.probabilities,s.probability_hash,s.trained_through_ordinal,s.model_available_at,s.feature_through_ordinal,s.feature_available_at,s.prepared_at,s.frozen_at,s.deadline_at FROM r30_model_surface s JOIN r30_model_artifact a ON a.model_hash=s.model_hash WHERE s.target_ordinal>=? AND s.target_ordinal<=? ORDER BY s.frozen_at ASC",new String[]{Long.toString(data.ordinals[0]),Long.toString(data.ordinals[data.size()-1])})){
            while(c.moveToNext()){
                Integer row=indices.get(c.getLong(0));if(row==null)continue;int position=c.getInt(1);long key=ModelSurfaceProviderR30.key(row,position);if(p.rows.containsKey(key))continue;
                String hash=hashes.get(row);if(hash==null){hash=ModelSurfaceProviderR30.historyHash(data,row);hashes.put(row,hash);}
                if(!hash.equals(c.getString(3))){p.blocked.put(key,"ARCHIVED_HISTORY_MISMATCH");continue;}
                try{
                    ModelSurfaceProviderR30.Surface s=ModelSurfaceProviderR30.archived(c.getString(2),hash,StoreR27.unpack(c.getBlob(4)),c.getLong(0),c.getLong(6),c.getLong(7),c.getLong(8),c.getLong(9),position,c.getLong(10),c.getLong(11),c.getLong(12));
                    if(!s.probabilityHash.equals(c.getString(5)))throw new ModelSurfaceProviderR30.Blocked("ARCHIVED_PROBABILITY_HASH");
                    p.rows.put(key,s);
                }catch(RuntimeException invalid){p.blocked.put(key,"INVALID_ARCHIVED_SURFACE:"+invalid.getMessage());}
            }
        }catch(RuntimeException missingSchema){p.blocked.put(-1L,"MODEL_SCHEMA_UNAVAILABLE");}
        return p;
    }
    public static void bindHistorical(SQLiteDatabase ro,FeatureCacheR27 cache){archived(ro,cache.data).bind(cache);}
    static Prepared prepareLive(RuntimeR27 rt,ResearchPoolR27.Worker w,FeatureCacheR27 cache,long deadline){
        Prepared p=archived(w.ro,cache.data);int row=cache.data.size()-1;
        AtomNetSessionR30 state=rt.atomSessionR30;String unavailable=state==null?"ATOMNET_NOT_TRAINED":!state.ready()?"ATOMNET_CHECKPOINT_NOT_READY":null;
        if(unavailable!=null){for(int pos=1;pos<=10;pos++)p.blocked.put(ModelSurfaceProviderR30.key(row,pos),unavailable);p.bind(cache);return p;}
        long available=System.currentTimeMillis();
        ModelSurfaceProviderR30.FrozenModelRef ref=new ModelSurfaceProviderR30.FrozenModelRef(state.trainer.net.encode(),state.trainedThroughOrdinal(),available);
        AtomReplayR27.Frame frame=AtomReplayR27.build(cache.data,row,row+1);String history=ModelSurfaceProviderR30.historyHash(cache.data,row);
        for(int pos=1;pos<=10;pos++){
            long key=ModelSurfaceProviderR30.key(row,pos);if(p.rows.containsKey(key))continue;
            try{
                if(frame.values[pos-1]==null)throw new ModelSurfaceProviderR30.Blocked("INSUFFICIENT_FEATURE_HISTORY");
                double[] x=AtomNetR27.positionedInput(AtomReplayR27.features(frame.values[pos-1],frame.present[pos-1],true),pos);long now=System.currentTimeMillis();
                ModelSurfaceProviderR30.Pit pit=new ModelSurfaceProviderR30.Pit(cache.data.ordinals[row],row>0?cache.data.ordinals[row-1]:cache.data.ordinals[row],pos,history,x,now,now,now,deadline);
                ModelSurfaceProviderR30.Surface surface=ModelSurfaceProviderR30.predict(pit,ref);p.rows.put(key,surface);p.models.put(ref.modelHash,ref);
            }catch(RuntimeException invalid){p.blocked.put(key,invalid.getMessage());}
        }
        p.bind(cache);return p;
    }
    static void calls(ResearchWriteQueueR27.Batch b,String policy,PolicySpecR27 spec,long target,FeatureCacheR27 cache,Prepared prepared,boolean valid,String reason){
        int count=Integer.bitCount(spec.ast.modelPositions);if(count==0)return;Set<String> addedModels=new HashSet<>();int row=cache==null?-1:cache.data.size()-1;long now=System.currentTimeMillis();
        for(int pos=1;pos<=10;pos++)if((spec.ast.modelPositions&(1<<(pos-1)))!=0){
            ModelSurfaceProviderR30.Surface s=cache==null?null:cache.modelSurface(row,pos);
            if(valid&&s!=null){
                surface(b,s,prepared==null||!addedModels.add(s.modelHash)?null:prepared.models.get(s.modelHash));
                b.add("INSERT OR IGNORE INTO r30_model_call SELECT ?,?,?,?,?,'MATCHED','',?,?,?,frozen_at FROM r27_live_frozen WHERE policy_hash=? AND target_ordinal=? AND status='FROZEN'",
                        "MATCHED:"+policy+":"+target+":"+pos,policy,s.modelHash,target,pos,s.probabilityHash,count,now,policy,target);
            }else{
                String why=s==null&&cache!=null?cache.modelBlockReason(row,pos):reason;if(why==null||why.isEmpty())why="POLICY_PREDICTION_UNAVAILABLE";
                String key="BLOCKED:"+policy+":"+target+":"+pos+":"+now+":"+StatsR27.sha256(why);
                b.add("INSERT OR IGNORE INTO r30_model_call VALUES(?,?,?,?,?,'BLOCKED',?,?,?,?,NULL)",key,policy,s==null?null:s.modelHash,target,pos,why,s==null?null:s.probabilityHash,count,now);
            }
        }
    }
    static long count(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){if(!c.moveToFirst())throw new IllegalStateException("MODEL_METRIC_UNAVAILABLE");return c.getLong(0);}}
    public static Map<String,Object> report(SQLiteDatabase db){
        Map<String,Object> m=new LinkedHashMap<>();
        try{
            long matched=count(db,"SELECT count(*) FROM r30_model_call WHERE status='MATCHED'"),blocked=count(db,"SELECT count(*) FROM r30_model_call WHERE status='BLOCKED'");
            long orphan=count(db,"SELECT count(*) FROM r30_model_call c LEFT JOIN r27_live_frozen f ON f.policy_hash=c.policy_hash AND f.target_ordinal=c.target_ordinal AND f.status='FROZEN' WHERE c.status='MATCHED' AND (f.policy_hash IS NULL OR c.frozen_at<>f.frozen_at OR NOT EXISTS(SELECT 1 FROM r30_model_artifact a WHERE a.model_hash=c.model_hash) OR NOT EXISTS(SELECT 1 FROM r30_model_surface s WHERE s.target_ordinal=c.target_ordinal AND s.position=c.position AND s.model_hash=c.model_hash AND s.probability_hash=c.probability_hash))");
            long missing=count(db,"SELECT count(*) FROM r27_live_frozen f JOIN r27_live_shadow s ON s.policy_hash=f.policy_hash WHERE f.status='FROZEN' AND s.spec_json LIKE '%model_surface%' AND (SELECT count(*) FROM r30_model_call c WHERE c.policy_hash=f.policy_hash AND c.target_ordinal=f.target_ordinal AND c.status='MATCHED')<>coalesce((SELECT max(required_count) FROM r30_model_call c WHERE c.policy_hash=f.policy_hash AND c.target_ordinal=f.target_ordinal AND c.status='MATCHED'),1)");
            m.put("matchedCalls",matched);m.put("blockedCalls",blocked);m.put("orphanCalls",orphan);m.put("missingCalls",missing);m.put("matchingRatio",matched>0?(matched-orphan)/(double)matched:null);
            m.put("status",orphan+missing>0?"FAIL":matched>0?"MATCHED":blocked>0?"BLOCKED":"INSUFFICIENT");m.put("quality","MODEL_CONSUMPTION_ONLY_NO_QUALITY_CLAIM");
            List<Object> reasons=new ArrayList<>();try(Cursor c=db.rawQuery("SELECT reason,count(*) FROM r30_model_call WHERE status='BLOCKED' GROUP BY reason ORDER BY count(*) DESC LIMIT 20",null)){while(c.moveToNext()){Map<String,Object> r=new LinkedHashMap<>();r.put("reason",c.getString(0));r.put("count",c.getLong(1));reasons.add(r);}}m.put("blockedReasons",reasons);
        }catch(RuntimeException unavailable){m.put("status","INSUFFICIENT");m.put("reason",unavailable.getClass().getSimpleName());}return m;
    }
}
