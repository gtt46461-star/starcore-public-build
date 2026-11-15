package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Dedicated read-side Q08 originals, independent of the 64-row / 16-KiB UI cache.
 * The immutable export connection or explicitly scoped read is reused. No write,
 * registration, experiment, qualification or authority change is performed. */
public final class Q08EvidenceReaderR325 {
    private Q08EvidenceReaderR325() {}
    static final int TRIAL_BYTES=16384, TOTAL_TRIAL_BYTES=262144, NULL_BYTES=262144;
    private static Map<?,?> map(Object x){return x instanceof Map?(Map<?,?>)x:Collections.emptyMap();}
    private static Map<String,Object> receipt(boolean pinned){
        Map<String,Object> r=new LinkedHashMap<>();
        r.put("readOnly",true);r.put("sourceScope",pinned?"PINNED_READ_ONLY_TRANSACTION":"SEPARATE_LIFECYCLE_READS_AFTER_FACT_WATERMARK");
        r.put("readStartedAt",System.currentTimeMillis());return r;
    }
    private static void fault(Map<String,Object> r,RuntimeException e){
        if(e instanceof android.os.OperationCanceledException||e instanceof java.util.concurrent.CancellationException)throw e;
        Error fatal=FailureDispositionR31.processFatalCause(e);if(fatal!=null)throw fatal;
        r.put("status","UNKNOWN_Q08_ORIGINAL_READ");r.put("exceptionClass",e.getClass().getName());
    }
    /** Called once after the existing lifecycle registry capture. Current state
     * and trial original hashes are compared by Q08ReportProjectionR325; separate
     * live reads can therefore never be silently combined into PASS. */
    public static void capture(SQLiteDatabase db,Map<String,Object> data,boolean pinned,android.os.CancellationSignal cancellation){
        Map<?,?> registry=map(map(map(data.get("lifecycleEvidenceR31")).get("evidenceContractR31")).get("value"));
        Map<String,Object> originals=receipt(pinned);
        originals.put("sourceTable","r27_chart_cache");originals.put("panel","r31-risk-contract-trial");
        originals.put("maximumRows",120L);originals.put("maximumBytes",TOTAL_TRIAL_BYTES);
        originals.put("binding",registry.get("binding"));originals.put("sourceCommit",registry.get("sourceCommit"));
        originals.put("protocolHash",registry.get("protocolHash"));originals.put("registryRecordSha256",registry.get("recordSha256"));
        try{
            Object binding=registry.get("binding");
            if(!(binding instanceof String)||!((String)binding).matches("[0-9a-f]{64}"))originals.put("status","UNKNOWN_REGISTRY_BINDING");
            else{
                SortedMap<String,String> rows=new TreeMap<>();int bytes=0;
                String query="SELECT key,CASE WHEN length(CAST(payload_json AS BLOB))<=CAST(? AS INTEGER) THEN payload_json ELSE NULL END FROM r27_chart_cache WHERE panel_id=? AND key>=? AND key<? ORDER BY key LIMIT 121";
                String[] args={Integer.toString(TRIAL_BYTES),"r31-risk-contract-trial",binding+":",binding+";"};
                try(Cursor c=cancellation==null?db.rawQuery(query,args):db.rawQuery(query,args,cancellation)){
                    while(c.moveToNext()){
                        if(cancellation!=null)cancellation.throwIfCanceled();
                        String text=c.getString(1),key=c.getString(0);
                        if(text==null||(bytes+=text.getBytes(StandardCharsets.UTF_8).length)>TOTAL_TRIAL_BYTES||rows.size()>=120||rows.put(key,text)!=null)
                            throw new IllegalStateException("Q08_ORIGINAL_SET_CAPACITY_OR_DUPLICATE_KEY");
                    }
                }
                originals.put("status","READ_OK_NOT_A_HEALTH_VERDICT");originals.put("rows",rows);
                originals.put("originalsSha256",StatsR27.sha256(JsonR27.write(rows)));originals.put("originalBytes",bytes);originals.put("rowCount",rows.size());
            }
        }catch(RuntimeException unavailable){fault(originals,unavailable);}
        originals.put("readFinishedAt",System.currentTimeMillis());data.put("q08FactoryOriginalsR325",originals);
        // Fixed primary keys: a busy chart stream cannot evict these originals
        // from the report. The factory cache is only progress/failure display;
        // PASS is determined from the original registered trial set above.
        data.put("syntheticControl",cache(db,"q08",NULL_BYTES,pinned,cancellation));
        data.put("factoryCapabilityExperiment",cache(db,"q08-factory-r31",TRIAL_BYTES,pinned,cancellation));
    }
    static Map<String,Object> cache(SQLiteDatabase db,String key,int maxBytes,boolean pinned,android.os.CancellationSignal cancellation){
        Map<String,Object> r=receipt(pinned);r.put("source","r30/"+key);
        r.put("scope","DEDICATED_BOUNDED_ORIGINAL_READ; SYNTHETIC_ONLY; NOT_NATURAL_QUALITY_OR_REAL_PERMISSION");
        r.put("maximumOriginalBytes",maxBytes);r.put("fullOriginal","snapshot.db / r27_chart_cache panel_id=r30 key="+key);
        try{
            String query="SELECT CASE WHEN length(CAST(payload_json AS BLOB))<=CAST(? AS INTEGER) THEN payload_json ELSE NULL END,length(CAST(payload_json AS BLOB)),updated_at FROM r27_chart_cache WHERE panel_id='r30' AND key=?";
            String[] args={Integer.toString(maxBytes),key};
            try(Cursor c=cancellation==null?db.rawQuery(query,args):db.rawQuery(query,args,cancellation)){
                if(!c.moveToFirst())r.put("status","UNKNOWN_SOURCE_MISSING");
                else{
                    r.put("originalBytes",c.isNull(1)?null:c.getLong(1));r.put("updatedAt",c.isNull(2)?null:c.getLong(2));
                    if(c.isNull(0)){r.put("status","REFERENCE_ONLY_ORIGINAL_BYTE_BOUND");r.put("payload",null);}
                    else{
                        String text=c.getString(0);Object value=JsonR27.parse(text);
                        if(!(value instanceof Map))throw new IllegalArgumentException("Q08_ORIGINAL_NOT_OBJECT");
                        r.put("status","READ_OK_NOT_A_HEALTH_VERDICT");r.put("payload",value);r.put("originalSha256",StatsR27.sha256(text));
                    }
                    if(c.moveToNext())throw new IllegalStateException("Q08_DUPLICATE_CACHE_PRIMARY_KEY");
                }
            }
        }catch(RuntimeException unavailable){r.remove("payload");fault(r,unavailable);}
        r.put("readFinishedAt",System.currentTimeMillis());return r;
    }
}
