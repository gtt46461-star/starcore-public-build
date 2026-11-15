package com.ethk3.novaonepro.strategyv2;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.BuildConfig;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Bounded derived metadata, published by the actual dataset owner. Original
 * rows/roles remain in their immutable pin, never reloaded by the report/UI. */
public final class ReplayKnowledgeStoreR31 {
    private ReplayKnowledgeStoreR31(){}
    public static final String VERSION="R31_REPLAY_KNOWLEDGE_PUBLICATION_1",PANEL="r31-replay-input",KEY="last-published-input";
    public static final int MAX_BYTES=65536;
    private static final Set<String> MODES=new HashSet<>(Arrays.asList("NEW_INPUT_PINNED","RECOVERED_COMMITTED_PIN","RESTORED_PUBLISHED_DATASET"));
    /** Same batch as new pin publication, or the guarded recovery-only metadata
     * batch before the recovered cache becomes visible to the evolution owner. */
    public static void appendR31(ResearchWriteQueueR27.Batch batch,ReplayDatasetR27 data,long revision,String mode,long at){
        if(batch==null||data==null||data.size()<1||revision<0||!MODES.contains(mode))throw new IllegalArgumentException("R31_REPLAY_KNOWLEDGE_PUBLICATION_INPUT");
        if(ChartCacheR27.classR31(PANEL)==ChartCacheR27.CacheClassR31.DISPLAY)throw new IllegalStateException("R31_REPLAY_KNOWLEDGE_NOT_DISPLAY_CACHE");
        Map<String,Object> metadata=new LinkedHashMap<>();metadata.put("format",VERSION);metadata.put("producer","RuntimeR27.DatasetTask");metadata.put("datasetHash",data.datasetHash);metadata.put("sourceRevision",revision);metadata.put("sourceCommit",BuildConfig.SOURCE_COMMIT_R26);metadata.put("sourceWorktreeHash",BuildConfig.SOURCE_WORKTREE_SHA256_R31);metadata.put("firstIssue",data.issues[0]);metadata.put("lastIssue",data.issues[data.size()-1]);metadata.put("firstOrdinal",data.ordinals[0]);metadata.put("throughOrdinal",data.ordinals[data.size()-1]);metadata.put("publicationMode",mode);metadata.put("publishedAt",at);metadata.put("provenance",data.provenanceR31());metadata.put("status",data.legacyIdentityR31?"LEGACY_KNOWLEDGE_TIME_UNKNOWN":"RECORDED_INPUT_METADATA_ONLY");metadata.put("rowSource",data.legacyIdentityR31?"ORIGINAL_LEGACY_PIN_TIME_UNRECORDED":"ORIGINAL_RECORDED_INPUT_ROWS");metadata.put("consumedByCurrentGeneration","NOT_INFERRED_GENERATION_BOUNDARY_UNCHANGED");metadata.put("retroactiveForecastsFabricated",false);metadata.put("authorizesCertificateOrReal",false);metadata.put("naturalQuality","NOT_PROVEN");
        metadata.put("inputValidity","NOT_RECHECKED_DERIVED_METADATA_ONLY");
        metadata.put("metadataHash",StatsR27.sha256(JsonR27.write(metadata)));String payload=JsonR27.write(metadata);
        if(payload.getBytes(StandardCharsets.UTF_8).length>MAX_BYTES)throw new IllegalArgumentException("R31_REPLAY_KNOWLEDGE_PUBLICATION_BOUND");
        batch.add("INSERT OR REPLACE INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES(?,?,?,?)",PANEL,KEY,payload,at);
    }
    /** Exactly one bounded read, no DDL, writes, dataset load or recapture. */
    @SuppressWarnings("unchecked") public static Map<String,Object> report(SQLiteDatabase db){
        Map<String,Object> unavailable=new LinkedHashMap<>();unavailable.put("format",VERSION);unavailable.put("status","UNAVAILABLE_REPLAY_KNOWLEDGE");unavailable.put("datasetHash",null);unavailable.put("provenance",null);unavailable.put("naturalQuality","NOT_PROVEN");unavailable.put("authorizesCertificateOrReal",false);
        try(Cursor c=db.rawQuery("SELECT CASE WHEN length(CAST(payload_json AS BLOB))<=CAST(? AS INTEGER) THEN payload_json ELSE NULL END,length(CAST(payload_json AS BLOB)),updated_at FROM r27_chart_cache WHERE panel_id=? AND key=?",new String[]{""+MAX_BYTES,PANEL,KEY})){
            if(!c.moveToFirst())return unavailable;
            if(c.isNull(0)){unavailable.put("status","UNAVAILABLE_OVERSIZED_REPLAY_KNOWLEDGE");unavailable.put("observedBytes",c.getLong(1));return unavailable;}
            Object parsed=JsonR27.parse(c.getString(0));if(!(parsed instanceof Map))throw new IllegalArgumentException("METADATA_OBJECT");Map<String,Object> metadata=new LinkedHashMap<>((Map<String,Object>)parsed);Object expected=metadata.remove("metadataHash");String actual=StatsR27.sha256(JsonR27.write(metadata));
            if(!Objects.equals(expected,actual)||!VERSION.equals(metadata.get("format"))||!"RuntimeR27.DatasetTask".equals(metadata.get("producer"))||!MODES.contains(metadata.get("publicationMode"))||!"NOT_PROVEN".equals(metadata.get("naturalQuality"))||!Boolean.FALSE.equals(metadata.get("authorizesCertificateOrReal"))||!Boolean.FALSE.equals(metadata.get("retroactiveForecastsFabricated")))throw new IllegalArgumentException("METADATA_IDENTITY");
            if(!(metadata.get("datasetHash") instanceof String)||!((String)metadata.get("datasetHash")).matches("[0-9a-f]{64}")||!(metadata.get("sourceRevision") instanceof Number)||((Number)metadata.get("sourceRevision")).longValue()<0||!(metadata.get("provenance") instanceof Map))throw new IllegalArgumentException("METADATA_SOURCE");
            Map<?,?> provenance=(Map<?,?>)metadata.get("provenance");long rows=((Number)provenance.get("rows")).longValue(),known=((Number)provenance.get("recordedKnownAtRows")).longValue(),unknown=((Number)provenance.get("unknownKnownAtRows")).longValue();
            if(rows<1||rows>10000||known<0||unknown<0||known+unknown!=rows||((Number)metadata.get("firstOrdinal")).longValue()>((Number)metadata.get("throughOrdinal")).longValue()||((Number)metadata.get("publishedAt")).longValue()!=c.getLong(2)||!Arrays.asList("LEGACY_KNOWLEDGE_TIME_UNKNOWN","RECORDED_INPUT_METADATA_ONLY").contains(metadata.get("status")))throw new IllegalArgumentException("METADATA_BOUNDARY");
            metadata.put("metadataHash",actual);metadata.put("readOnly",true);return metadata;
        }catch(RuntimeException failure){
            if(failure instanceof android.os.OperationCanceledException||failure instanceof java.util.concurrent.CancellationException||Thread.currentThread().isInterrupted())throw failure;
            unavailable.put("status",failure instanceof android.database.sqlite.SQLiteException?"UNAVAILABLE_REPLAY_KNOWLEDGE":"INVALID_REPLAY_KNOWLEDGE");return unavailable;
        }
    }
}
