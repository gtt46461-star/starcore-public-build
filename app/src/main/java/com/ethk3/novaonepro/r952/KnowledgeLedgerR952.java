package com.ethk3.novaonepro.r952;

import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;
import com.ethk3.novaonepro.V51ChecksumEngine;

/** Immutable evidence bodies owned by the existing research transaction. */
public final class KnowledgeLedgerR952 {
    private KnowledgeLedgerR952(){}
    public static String put(SQLiteDatabase db,String issue,String candidate,String kind,String payload){
        if(db==null||!db.inTransaction())throw new IllegalStateException("R958_KNOWLEDGE_TRANSACTION_REQUIRED");
        if(issue==null||!issue.matches("[0-9]{12}")||candidate==null||candidate.isEmpty()||kind==null||kind.isEmpty()||payload==null)
            throw new IllegalArgumentException("R958_KNOWLEDGE_IDENTITY");
        String payloadHash=V51ChecksumEngine.sha256(payload),hash=identity(issue,candidate,kind,payloadHash);
        db.execSQL("INSERT OR IGNORE INTO r952_knowledge(knowledge_hash,target_issue,candidate_id,kind,payload_hash,created_at,payload,payload_format) VALUES(?,?,?,?,?,?,?,'UTF8_V1')",
            new Object[]{hash,issue,candidate,kind,payloadHash,System.currentTimeMillis(),payload});
        if(!payload.equals(require(db,hash)))throw new IllegalStateException("R958_KNOWLEDGE_REPLAY_DRIFT");
        return hash;
    }
    public static String require(SQLiteDatabase db,String hash){
        try(Cursor c=db.rawQuery("SELECT target_issue,candidate_id,kind,payload_hash,payload,payload_format FROM r952_knowledge WHERE knowledge_hash=?",new String[]{hash})){
            if(!c.moveToFirst())throw new IllegalStateException("R958_KNOWLEDGE_NOT_FOUND");
            if(!"UTF8_V1".equals(c.getString(5)))throw new IllegalStateException("R958_LEGACY_HASH_ONLY_NOT_REPLAYABLE");
            String payload=c.getString(4),payloadHash=V51ChecksumEngine.sha256(payload);
            if(!payloadHash.equals(c.getString(3))||!identity(c.getString(0),c.getString(1),c.getString(2),payloadHash).equals(hash))
                throw new IllegalStateException("R958_KNOWLEDGE_HASH_DRIFT");
            return payload;
        }
    }
    private static String identity(String issue,String candidate,String kind,String payloadHash){
        return V51ChecksumEngine.sha256("KNOWLEDGE2|"+issue+'|'+candidate+'|'+kind+'|'+payloadHash);
    }
}
