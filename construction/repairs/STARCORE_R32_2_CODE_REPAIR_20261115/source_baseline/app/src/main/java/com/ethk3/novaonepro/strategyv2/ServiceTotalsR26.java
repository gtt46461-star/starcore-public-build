package com.ethk3.novaonepro.strategyv2;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.BuildConfig;
import com.ethk3.novaonepro.ResearchSemanticsGeneratedR967;
import org.json.*;
/** Durable totals for committed research receipts; the bounded diagnostic ring is not a lifetime counter. */
public final class ServiceTotalsR26 {
    private ServiceTotalsR26(){}
    private static boolean installed(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name='r26_service_total'",null)){return c.moveToFirst();}}
    public static void record(SQLiteDatabase db,String owner,long cpu,long wall,boolean capacity){
        if(!db.inTransaction()||owner==null||owner.isEmpty()||cpu<0||wall<0)throw new IllegalArgumentException("R26_SERVICE_TOTAL_OWNER_OR_VALUES");
        db.execSQL("INSERT OR IGNORE INTO r26_service_total(session_id,source_commit,numerical_abi,api_level,device_model) VALUES(?,?,?,?,?)",new Object[]{owner,BuildConfig.SOURCE_COMMIT_R26,ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256,android.os.Build.VERSION.SDK_INT,android.os.Build.MODEL});
        db.execSQL("UPDATE r26_service_total SET slices=slices+1,cpu_ms=cpu_ms+?,wall_ms=wall_ms+?,capacity_failures=capacity_failures+? WHERE session_id=?",new Object[]{cpu,wall,capacity?1:0,owner});
    }
    public static long failures(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT coalesce(sum(capacity_failures),0)+(SELECT coalesce(sum(folded),0) FROM r26_retention_total WHERE kind='service_capacity_failures') FROM r26_service_total",null)){return c.moveToFirst()?c.getLong(0):0;}}
    public static void foldExpired(SQLiteDatabase db){
        if(!db.inTransaction())throw new IllegalStateException("R26_SERVICE_TOTAL_RETENTION_WRITER");if(!installed(db))return;String expired="SELECT t.session_id FROM r26_service_total t WHERE NOT EXISTS(SELECT 1 FROM r26_session s WHERE s.session_id=t.session_id) ORDER BY t.session_id LIMIT 32";
        String[] names={"service_slices","service_cpu_ms","service_wall_ms","service_capacity_failures"};long[] values=new long[4];try(Cursor c=db.rawQuery("SELECT coalesce(sum(slices),0),coalesce(sum(cpu_ms),0),coalesce(sum(wall_ms),0),coalesce(sum(capacity_failures),0) FROM r26_service_total WHERE session_id IN ("+expired+")",null)){if(c.moveToFirst())for(int i=0;i<4;i++)values[i]=c.getLong(i);}
        for(int i=0;i<4;i++)if(values[i]>0){db.execSQL("INSERT OR IGNORE INTO r26_retention_total VALUES(?,0)",new Object[]{names[i]});db.execSQL("UPDATE r26_retention_total SET folded=folded+? WHERE kind=?",new Object[]{values[i],names[i]});}db.execSQL("DELETE FROM r26_service_total WHERE session_id IN ("+expired+")");
    }
    public static JSONObject snapshot(SQLiteDatabase db)throws JSONException{
        if(!installed(db))return new JSONObject().put("availability","NOT_INSTALLED").put("committedCapacityFailuresSinceInstallation",JSONObject.NULL).put("scope","No durable telemetry was installed for this snapshot; absence is not zero failures");
        JSONArray sessions=new JSONArray();long count=0;try(Cursor c=db.rawQuery("SELECT count(*) FROM r26_service_total",null)){if(c.moveToFirst())count=c.getLong(0);}try(Cursor c=db.rawQuery("SELECT session_id,source_commit,numerical_abi,api_level,device_model,slices,cpu_ms,wall_ms,capacity_failures FROM r26_service_total ORDER BY session_id LIMIT 64",null)){while(c.moveToNext())sessions.put(new JSONObject().put("sessionId",c.getString(0)).put("sourceCommit",c.getString(1)).put("numericalAbi",c.getString(2)).put("apiLevel",c.getInt(3)).put("deviceModel",c.getString(4)).put("committedSlices",c.getLong(5)).put("researchCpuMs",c.getLong(6)).put("serviceWallMs",c.getLong(7)).put("capacityFailures",c.getLong(8)));}
        JSONObject folded=new JSONObject();try(Cursor c=db.rawQuery("SELECT kind,folded FROM r26_retention_total WHERE kind IN ('service_slices','service_cpu_ms','service_wall_ms','service_capacity_failures') ORDER BY kind",null)){while(c.moveToNext())folded.put(c.getString(0),c.getLong(1));}
        return new JSONObject().put("scope","Committed research service receipts since telemetry installation; failed receipt commits are excluded; rows bind source/runtime/device; historical folds span sources; not P0 or a full opportunity SLO").put("sessions",sessions).put("totalSessionRows",count).put("omittedSessionRows",Math.max(0,count-sessions.length())).put("historicalFoldedTotals",folded).put("committedCapacityFailuresSinceInstallation",failures(db));
    }
}
