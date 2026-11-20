package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;

/** Compact metadata from the original immutable report database. This projection
 * never fetches the compressed BLOB or grants an S5 ACK. FULL_RAW already contains
 * the complete original row in the native, BEGIN-pinned snapshot.db. */
public final class DiagnosisRefitReportR335 {
    private DiagnosisRefitReportR335() {}
    private static final String TABLE = "r335_s5_refit_original";
    private static final String OP = "R335_S5_RECEIPT";
    private static boolean table(SQLiteDatabase db, String name) {
        try (Cursor cursor = db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?", new String[]{name})) {
            return cursor.moveToFirst();
        }
    }
    @SuppressWarnings("unchecked") private static Map<String,Object> object(Object value) {
        if (!(value instanceof Map)) throw new IllegalStateException("R335_REFIT_REPORT_OBJECT");
        return (Map<String,Object>) value;
    }
    private static Object boundedJson(String text) {
        int depth=0;boolean quoted=false,escaped=false;
        for(int i=0;i<text.length();i++){
            char c=text.charAt(i);
            if(quoted){if(escaped)escaped=false;else if(c=='\\')escaped=true;else if(c=='"')quoted=false;}
            else if(c=='"')quoted=true;
            else if(c=='{'||c=='['){if(++depth>32)throw new IllegalStateException("R335_REFIT_REPORT_JSON_DEPTH");}
            else if(c=='}'||c==']'){if(--depth<0)throw new IllegalStateException("R335_REFIT_REPORT_JSON_SHAPE");}
        }
        if(quoted||escaped||depth!=0)throw new IllegalStateException("R335_REFIT_REPORT_JSON_INCOMPLETE");
        return JsonR27.parse(text);
    }
    private static String identity(Map<String,Object> value,String field) {
        Object found=value.get(field);
        if(!(found instanceof String))throw new IllegalStateException("R335_REFIT_REPORT_IDENTITY:"+field);
        return HashingSU1.sha((String)found);
    }
    private static Map<String,Object> ledgerReceipt(SQLiteDatabase db) {
        if(!table(db,"su2_diag_state"))return null;
        try(Cursor cursor=db.rawQuery("SELECT length(CAST(ledger_text AS BLOB)),CASE WHEN length(CAST(ledger_text AS BLOB))<=262144 THEN ledger_text ELSE NULL END FROM su2_diag_state WHERE singleton=1",null)){
            if(!cursor.moveToFirst())return null;
            if(cursor.isNull(1))throw new IllegalStateException("R335_REFIT_REPORT_LEDGER_DISPLAY_BOUND");
            Map<String,Object> receipt=null;boolean seen=false;
            for(String line:cursor.getString(1).split("\n"))if(line.startsWith("X5\tS5\t\t")){
                if(seen)throw new IllegalStateException("R335_REFIT_REPORT_DUPLICATE_EXTENSION");seen=true;
                Map<String,Object> state=object(boundedJson(line.substring(7)));
                if(state.get("receipt")!=null){receipt=object(state.get("receipt"));
                    if(!HashingSU1.text(JsonR27.write(receipt)).equals(state.get("receiptHash")))throw new IllegalStateException("R335_REFIT_REPORT_RECEIPT_HASH");
                }
            }
            return receipt;
        }
    }
    public static Map<String,Object> capture(SQLiteDatabase db) {
        Map<String,Object> out=new LinkedHashMap<>();
        out.put("sourceTable",TABLE);out.put("migrationOwner","LifecycleSchemaR31 -> DiagnosisSchemaR335");
        out.put("fullOriginal","snapshot.db / r335_s5_refit_original / case_id");
        out.put("compressedPayloadIncludedInline",false);
        out.put("contentVerification","NOT_PERFORMED_BY_METADATA_PROJECTION; DiagnosisShadowReceiptR335 independently reads bounded BLOB chunks and validates original SHA");
        out.put("qualificationGranted",false);out.put("sourceDeletionAuthorized",false);
        out.put("retentionPolicy","UPDATE_AND_DELETE_DENIED_BY_ORIGINAL_LIFECYCLE_TRIGGERS; NO_AGE_OR_FIFO_RETIREMENT");
        if(!table(db,TABLE)){out.put("status","UNKNOWN_SOURCE_SCHEMA_MISSING");out.put("rows",null);return out;}
        List<Map<String,Object>> originals=new ArrayList<>();long total,compressedBytes,rawBytes;
        try(Cursor cursor=db.rawQuery("SELECT count(*),coalesce(sum(length(compressed)),0),coalesce(sum(raw_bytes),0) FROM r335_s5_refit_original",null)){
            if(!cursor.moveToFirst())throw new IllegalStateException("R335_REFIT_REPORT_COUNT_UNKNOWN");
            total=cursor.getLong(0);compressedBytes=cursor.getLong(1);rawBytes=cursor.getLong(2);
        }
        try(Cursor cursor=db.rawQuery("SELECT case_id,singleton,original_hash,checkpoint_hash,raw_bytes,length(compressed),compressed_hash,created_at FROM r335_s5_refit_original ORDER BY case_id LIMIT 2",null)){
            while(cursor.moveToNext()){
                Map<String,Object> row=new LinkedHashMap<>();
                row.put("caseId",cursor.getString(0));row.put("singleton",cursor.getLong(1));
                row.put("originalHash",cursor.getString(2));row.put("checkpointHash",cursor.getString(3));
                row.put("rawBytes",cursor.getLong(4));row.put("compressedBytes",cursor.getLong(5));
                row.put("compressedHash",cursor.getString(6));row.put("createdAt",cursor.getLong(7));
                row.put("locator","snapshot.db / "+TABLE+" / case_id="+cursor.getString(0));
                row.put("hashesAreStoredMetadata",true);originals.add(row);
            }
        }
        long triggerCount;
        try(Cursor cursor=db.rawQuery("SELECT count(*) FROM sqlite_master WHERE type='trigger' AND tbl_name='r335_s5_refit_original' AND name IN ('r335_s5_refit_no_update','r335_s5_refit_no_delete')",null)){
            if(!cursor.moveToFirst())throw new IllegalStateException("R335_REFIT_REPORT_GUARD_UNKNOWN");triggerCount=cursor.getLong(0);
        }
        out.put("rows",total);out.put("rawBytes",rawBytes);out.put("compressedBytes",compressedBytes);
        out.put("originals",originals);out.put("displayedRows",originals.size());out.put("omittedRows",Math.max(0,total-originals.size()));
        out.put("declaredRowCapacity",1);out.put("declaredBoundSatisfied",total<=1);
        out.put("requiredProtectionTriggerNamesPresent",triggerCount==2);
        out.put("guardDefinitionValidation","LifecycleSchemaR31 owns exact DDL; name presence alone is not proof of trigger semantics");
        String status=total>1?"DECLARED_CAPACITY_EXCEEDED":total==0?"INSUFFICIENT_NO_ORIGINAL":"RECORDED_METADATA_ONLY";
        try{
            Map<String,Object> receipt=ledgerReceipt(db);
            if(receipt==null){out.put("currentLedgerReference","UNKNOWN_OR_NOT_YET_RECEIVED");}
            else{
                String key=identity(receipt,"refitOriginalKey");Map<String,Object> source=object(receipt.get("source"));
                String raw=identity(source,"refitOriginalHash"),checkpoint=identity(source,"checkpointHash");
                out.put("currentLedgerCase",key);out.put("currentLedgerReceiptHash",HashingSU1.text(JsonR27.write(receipt)));
                Map<String,Object> found=null;for(Map<String,Object> row:originals)if(key.equals(row.get("caseId")))found=row;
                boolean match=found!=null&&raw.equals(found.get("originalHash"))&&checkpoint.equals(found.get("checkpointHash"));
                out.put("currentLedgerReference",found==null?"ORIGINAL_MISSING":match?"METADATA_MATCH_CONTENT_NOT_READ":"ORIGINAL_IDENTITY_CONFLICT");
                if(found==null)status="REFERENCED_ORIGINAL_MISSING";else if(!match)status="REFERENCED_ORIGINAL_IDENTITY_CONFLICT";
                if(table(db,"su2_lineage"))try(Cursor cursor=db.rawQuery("SELECT op,campaign_id,length(CAST(note AS BLOB)),CASE WHEN length(CAST(note AS BLOB))<=131072 THEN note ELSE NULL END FROM su2_lineage WHERE child_key=? AND parent_key=?",new String[]{identity(receipt,"childKey"),identity(receipt,"parentKey")})){
                    if(!cursor.moveToFirst())out.put("currentLineageReference","ORIGINAL_RECEIPT_MISSING");
                    else if(cursor.isNull(3))out.put("currentLineageReference","UNKNOWN_DISPLAY_BOUND");
                    else{
                        boolean equal=OP.equals(cursor.getString(0))&&Objects.equals(receipt.get("campaign"),cursor.getString(1))&&JsonR27.write(receipt).equals(JsonR27.write(boundedJson(cursor.getString(3))));
                        out.put("currentLineageReference",equal?"SAME_RECEIPT_COPY":"RECEIPT_CONFLICT");
                    }
                }
                else out.put("currentLineageReference","UNKNOWN_SOURCE_SCHEMA_MISSING");
            }
        }catch(RuntimeException bad){out.put("currentLedgerReference","UNKNOWN_REFERENCE_READ_FAILED");out.put("referenceErrorType",bad.getClass().getName());out.put("referenceError",String.valueOf(bad.getMessage()));}
        out.put("status",status);return out;
    }
}
