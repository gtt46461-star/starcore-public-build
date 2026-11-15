package com.ethk3.novaonepro.strategyv2;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.*;
import org.json.*;

/** Report decoding of one identified frozen event. Never infer or fill a missing predraw record. */
public final class FrozenSurfaceStoreR26 {
    private FrozenSurfaceStoreR26() {}
    public static String exportJson(SQLiteDatabase db){
        try{return json(db).toString(2);}catch(Exception failure){
            try{JSONArray chain=new JSONArray();for(Throwable cause=failure;cause!=null;cause=cause.getCause())chain.put(cause.getClass().getName()+":"+cause.getMessage());return new JSONObject().put("schema","R26_FROZEN_ACTION_SURFACE_1").put("status","ERROR").put("reason",failure.toString()).put("errorChain",chain).put("expectedSurfaces",100).put("exportedSurfaces",0).put("expectedActions",1200).put("exportedActions",0).put("omittedActions",1200).put("qualityQualificationGranted",false).toString(2);}catch(JSONException impossible){throw new IllegalStateException(impossible);}
        }
    }
    public static JSONObject json(SQLiteDatabase db)throws JSONException,IOException {
        JSONObject result=new JSONObject().put("schema","R26_FROZEN_ACTION_SURFACE_1").put("scope","ONE_IMMUTABLE_PREDRAW_EVENT").put("independentOpportunities",1);
        if(!RuntimeSchemaR26.ready(db))return result.put("status","NOT_RUN").put("reason","R26_SCHEMA_UNAVAILABLE");
        try(Cursor c=db.rawQuery("SELECT f.policy_hash,f.target_ordinal,f.input_through,f.checkpoint_hash,f.state_hash,f.source_hash,f.source_generation,f.frozen_at,f.deadline_at,f.probability_table,d.position,d.number_mask,d.act,d.phat FROM r26_frozen f JOIN su2_live_decision d ON d.genome_key=f.policy_hash AND d.sequence_ordinal=f.target_ordinal WHERE f.input_through<f.target_ordinal AND f.frozen_at<f.deadline_at ORDER BY f.target_ordinal DESC,f.policy_hash LIMIT 1",null)){
            if(!c.moveToFirst())return result.put("status","INSUFFICIENT").put("reason","NO_BOUND_FROZEN_EVENT");
            byte[] encoded=c.getBlob(9);if(encoded.length!=400)throw new IOException("R26_FROZEN_SURFACE_BYTES");float[][] table=new float[10][10];try(DataInputStream data=new DataInputStream(new ByteArrayInputStream(encoded))){for(int p=0;p<10;p++)for(int n=0;n<10;n++)table[p][n]=data.readFloat();}
            FrozenSurfaceR26 surface=new FrozenSurfaceR26(table,c.getInt(10),c.getInt(11));
            result.put("policyHash",c.getString(0)).put("targetOrdinal",c.getLong(1)).put("inputThrough",c.getLong(2)).put("checkpointHash",c.getString(3)).put("stateHash",c.getString(4)).put("sourceHash",c.getString(5)).put("sourceGeneration",c.getLong(6)).put("frozenAt",c.getLong(7)).put("deadlineAt",c.getLong(8)).put("selectedAct",c.getInt(12)==1).put("selectedRawProbability",c.isNull(13)?JSONObject.NULL:c.getDouble(13));
            result.put("probabilityTableHash",LiveStateR26.hash(encoded)).put("tableHashProtocol","SHA256_BASE64_IEEE754_F32_BIG_ENDIAN").put("renderingTimeIsFreezeTime",false).put("delayedDecoding",true).put("newInferencePerformed",false);
            JSONArray cells=new JSONArray(),actions=new JSONArray();for(int p=0;p<10;p++)for(int n=0;n<10;n++)cells.put(new JSONObject().put("position",p+1).put("number",n+1).put("probability",table[p][n]));
            for(FrozenSurfaceR26.Action a:surface.actions)actions.put(new JSONObject().put("ordinal",a.ordinal).put("position",a.position).put("numbers",new JSONArray(new int[]{a.a,a.b,a.c})).put("numberMask",a.mask).put("rank",a.rank).put("k3MarginalMass",a.probability));
            return result.put("status","PASS").put("reason","EXACT_FROZEN_TABLE_DECODING_ONLY").put("surfaces",cells).put("expectedSurfaces",100).put("exportedSurfaces",cells.length()).put("actions",actions).put("expectedActions",1200).put("exportedActions",actions.length()).put("omittedActions",0).put("qualityQualificationGranted",false);
        }
    }
}
