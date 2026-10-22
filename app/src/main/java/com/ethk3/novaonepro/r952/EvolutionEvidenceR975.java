package com.ethk3.novaonepro.r952;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;

/** Historical legacy review schema/decoder retained for archive compatibility.
 * All review advancement is retired; ResearchFeedbackCP12 owns formal miss episodes. */
public final class EvolutionEvidenceR975 {
    public static final String TABLE="r975_evolution_review";
    public static final int PAGE=8, REQUIRED=120;
    private EvolutionEvidenceR975() {}
    public static void schema(SQLiteDatabase db) {
        if(!db.inTransaction())throw new IllegalStateException("R975_EVOLUTION_SCHEMA_TRANSACTION");
        if(TransactionSchemaCacheR941.ready(db,"R975_EVOLUTION_REVIEW_V1"))return;
        db.execSQL("CREATE TABLE IF NOT EXISTS "+TABLE+"(review_id TEXT PRIMARY KEY,parent_id TEXT NOT NULL,ast_hash TEXT NOT NULL,endpoint TEXT NOT NULL,after_child_issue TEXT NOT NULL,cursor_issue TEXT NOT NULL,verified_n INTEGER NOT NULL DEFAULT 0,recent_misses INTEGER NOT NULL DEFAULT 0,earlier_misses INTEGER NOT NULL DEFAULT 0,payload TEXT NOT NULL,evidence_hash TEXT NOT NULL,state TEXT NOT NULL CHECK(state IN ('ACTIVE','READY','CONSUMED','BLOCKED')),reason TEXT NOT NULL DEFAULT '',child_id TEXT NOT NULL DEFAULT '',updated_at INTEGER NOT NULL)");
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_r975_evolution_active ON "+TABLE+"(parent_id) WHERE state IN ('ACTIVE','READY')");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r975_evolution_work ON "+TABLE+"(state,updated_at)");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS r975_evolution_identity BEFORE UPDATE OF review_id,parent_id,ast_hash,endpoint,after_child_issue ON "+TABLE+" BEGIN SELECT RAISE(ABORT,'R975_EVOLUTION_REVIEW_IDENTITY_IMMUTABLE'); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS r975_evolution_terminal BEFORE UPDATE ON "+TABLE+" WHEN OLD.state IN ('CONSUMED','BLOCKED') BEGIN SELECT RAISE(ABORT,'R975_EVOLUTION_TERMINAL_IMMUTABLE'); END");
        TransactionSchemaCacheR941.mark(db,"R975_EVOLUTION_REVIEW_V1");
    }
    static final class Review {
        final String id,parent,astHash,endpoint,after,cursor,payload,hash,state;
        final int n,recent,earlier;
        Review(Cursor c){id=c.getString(0);parent=c.getString(1);astHash=c.getString(2);endpoint=c.getString(3);after=c.getString(4);cursor=c.getString(5);n=c.getInt(6);recent=c.getInt(7);earlier=c.getInt(8);payload=c.getString(9);hash=c.getString(10);state=c.getString(11);}
    }
    static Review next(SQLiteDatabase db,String endpoint) {
        // CP12 uses immutable formal Future feedback, never legacy training labels.
        return null;
    }
    private static Review verify(Review r){
        if(r.n<0||r.n>REQUIRED||r.recent<0||r.earlier<0||r.recent>Math.min(20,r.n)||r.recent+r.earlier>r.n||!V51ChecksumEngine.sha256(r.payload).equals(r.hash))throw new IllegalStateException("R975_REVIEW_CURSOR_OR_HASH_DRIFT");
        String[] parts=r.payload.split("\\|",-1);
        if(parts.length!=r.n+2||!parts[0].equals("R958_ACTUAL_ONLINE_ERRORS")||!parts[1].equals(r.parent))throw new IllegalStateException("R975_REVIEW_PAYLOAD_COUNT_DRIFT");
        String previous=r.endpoint+"~";int recent=0,earlier=0;
        for(int i=2;i<parts.length;i++){
            String[] row=parts[i].split(":",-1);
            if(row.length!=5||!TargetIssueResolver.isValid(row[0])||row[0].compareTo(previous)>=0||row[0].compareTo(r.after)<=0||!row[4].matches("[0-9a-f]{64}"))throw new IllegalStateException("R975_REVIEW_PAYLOAD_IDENTITY");
            int actual=Integer.parseInt(row[1]),mask=Integer.parseInt(row[2]),hit=Integer.parseInt(row[3]);
            if(actual<1||actual>10||AppConfig.countK3MaskNumbers(mask)!=3||hit!=(AppConfig.maskContainsNumber(mask,actual)?1:0))throw new IllegalStateException("R975_REVIEW_PAYLOAD_LABEL");
            if(hit==0){if(i-2<20)recent++;else earlier++;}previous=row[0];
        }
        if(recent!=r.recent||earlier!=r.earlier||!previous.equals(r.cursor))throw new IllegalStateException("R975_REVIEW_PROGRESS_DOES_NOT_MATCH_PAYLOAD");
        return r;
    }
    static String advance(SQLiteDatabase db,Review r) {
        throw new IllegalStateException("RETIRED_CP12_FORMAL_RESEARCH_OWNER");
    }
    static void finish(SQLiteDatabase db,Review r,String state,String reason,String child){
        throw new IllegalStateException("RETIRED_CP12_FORMAL_RESEARCH_OWNER");
    }
}
