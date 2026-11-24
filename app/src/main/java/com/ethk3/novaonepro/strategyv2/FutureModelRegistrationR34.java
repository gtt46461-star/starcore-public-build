package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.Map;

/** An independently observed neural experiment owns one immutable, already published model. */
final class FutureModelRegistrationR34 {
    static final String TABLE="r34_r27_future_model";
    static final String COLUMNS="experiment_id,publication_id,model_hash,weights_hash,feature_abi,registered_at";
    static final String DEFINITION="CREATE TABLE IF NOT EXISTS "+TABLE+"(experiment_id TEXT PRIMARY KEY,publication_id TEXT NOT NULL,model_hash TEXT NOT NULL,weights_hash TEXT NOT NULL,feature_abi TEXT NOT NULL,registered_at INTEGER NOT NULL)";
    static final String TRIGGER="r34_r27_future_model_immutable";
    static final String GUARD="CREATE TRIGGER IF NOT EXISTS "+TRIGGER+" BEFORE UPDATE ON "+TABLE+" BEGIN SELECT RAISE(ABORT,'R34_FUTURE_MODEL_IMMUTABLE'); END";
    private static final String PUBLICATION="SELECT p.publication_id,p.state_hash,p.model_hash,p.weights_hash,"
        +"p.through_ordinal,p.model_available_at,p.published_at,p.weights_ref FROM "+TABLE+" m JOIN r31_atomnet_publication p"
        +" ON p.publication_id=m.publication_id AND p.model_hash=m.model_hash AND p.weights_hash=m.weights_hash"
        +" WHERE m.experiment_id=? AND m.feature_abi=? AND p.status='PUBLISHED'"
        +" AND p.published_at<=m.registered_at AND p.model_available_at<=p.published_at";
    private FutureModelRegistrationR34(){}

    static boolean register(SQLiteDatabase db,String experiment,ModelSurfaceProviderR30.FrozenModelRef model,long now){
        if(model==null)return false;
        try(Cursor existing=open(db,experiment)){
            if(existing.moveToFirst())return model.modelHash.equals(existing.getString(2))
                    &&model.weightsHash.equals(existing.getString(3))&&model.trainedThroughOrdinal==existing.getLong(4)
                    &&model.availableAt==existing.getLong(5);
        }
        try(Cursor c=db.rawQuery("SELECT publication_id FROM r31_atomnet_publication WHERE model_hash=? AND weights_hash=?"
            +" AND through_ordinal=? AND model_available_at=? AND status='PUBLISHED' AND model_available_at<=published_at"
            +" AND published_at<=? ORDER BY published_at,publication_id LIMIT 1",
            new String[]{model.modelHash,model.weightsHash,Long.toString(model.trainedThroughOrdinal),
                Long.toString(model.availableAt),Long.toString(now)})){
            if(!c.moveToFirst())return false;
            db.execSQL("INSERT INTO "+TABLE+" VALUES(?,?,?,?,?,?)",
                new Object[]{experiment,c.getString(0),model.modelHash,model.weightsHash,AtomNetSessionR30.ABI,now});
            return true;
        }
    }
    static boolean fillBinding(SQLiteDatabase db,String experiment,Map<String,Object> result){
        try(Cursor c=open(db,experiment)){
            if(!c.moveToFirst())return false;
            result.put("publicationId",c.getString(0));result.put("learnerStateHash",c.getString(1));
            result.put("modelHash",c.getString(2));result.put("weightsHash",c.getString(3));
            result.put("trainedThroughOrdinal",c.getLong(4));result.put("modelAvailableAt",c.getLong(5));
            result.put("publishedAt",c.getLong(6));result.put("modelExperimentScope","REGISTERED_IMMUTABLE_PUBLICATION");
            return true;
        }
    }
    static ModelSurfaceProviderR30.FrozenModelRef read(SQLiteDatabase db,String experiment){
        try(Cursor c=open(db,experiment)){
            if(!c.moveToFirst())return null;
            ModelSurfaceProviderR30.FrozenModelRef ref=ModelSurfaceProviderR30.FrozenModelRef.archived(
                db,c.getString(2),c.getBlob(7),c.getLong(4),c.getLong(5));
            if(!ref.weightsHash.equals(c.getString(3)))throw new ModelSurfaceProviderR30.Blocked("R34_FUTURE_MODEL_HASH");
            return ref;
        }
    }
    private static Cursor open(SQLiteDatabase db,String experiment){
        return db.rawQuery(PUBLICATION,new String[]{experiment,AtomNetSessionR30.ABI});
    }
}
