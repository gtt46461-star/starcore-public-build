package com.ethk3.novaonepro.r952;

import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.V51ChecksumEngine;

public final class RuntimeEventContractR952 {
    public static final String PENDING="PENDING", RUNNING="RUNNING", COMPLETED="COMPLETED";
    public static final String RETRYABLE_FAILED="RETRYABLE_FAILED", TERMINAL_FAILED="TERMINAL_FAILED";
    private RuntimeEventContractR952() {}
    public static String deliveryHash(String target, String candidate, String settlement, String label) {
        return V51ChecksumEngine.sha256("TRAIN|" + safe(target) + '|' + safe(candidate)
                + '|' + safe(settlement) + '|' + safe(label));
    }
    public static String eventHash(String type, String target, String entity, String payloadHash) {
        return V51ChecksumEngine.sha256("R952_EVENT|" + safe(type) + '|' + safe(target)
                + '|' + safe(entity) + '|' + safe(payloadHash));
    }
    public static String record(SQLiteDatabase db,String type,String target,String entity,String payloadHash){
        if(db==null||type==null||type.trim().isEmpty()||target==null||target.trim().isEmpty()||payloadHash==null||!payloadHash.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("R952_RUNTIME_EVENT_CONTRACT");
        String hash=eventHash(type,target,entity,payloadHash);
        db.execSQL("INSERT OR IGNORE INTO r952_runtime_event(event_hash,target_issue,event_type,entity_id,payload_hash,source_commit,created_at) VALUES(?,?,?,?,?,?,?)",
                new Object[]{hash,target,type,safe(entity),payloadHash,BatchBuildIdentityR952.hash(),Long.valueOf(System.currentTimeMillis())});
        return hash;
    }
    static String safe(String value) { return value == null ? "" : value.trim(); }
}
