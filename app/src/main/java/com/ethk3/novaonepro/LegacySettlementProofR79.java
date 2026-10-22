package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.json.JSONObject;

/** Immutable classifications of ORIGINAL research captures. This is neither a proof producer nor
 * a settlement owner. A receipt cannot authorize REAL, reconstruct a missing proof, or prove a
 * previously trained mutable asset head. The existing Research schema owner installs this table. */
public final class LegacySettlementProofR79 {
    public static final String TABLE="research_legacy_settlement_classification_r79";
    private static final String CREATE="CREATE TABLE IF NOT EXISTS "+TABLE+"(receipt_hash TEXT PRIMARY KEY,candidate_key TEXT NOT NULL,target_issue TEXT NOT NULL,original_identity_hash TEXT NOT NULL,record_hash TEXT NOT NULL,proof_hash TEXT NOT NULL,capture_event_hash TEXT NOT NULL,frozen_at INTEGER NOT NULL,execution_at INTEGER NOT NULL,capture_at INTEGER NOT NULL,draw_hash TEXT NOT NULL,draw_open_time INTEGER NOT NULL,disposition TEXT NOT NULL,reason TEXT NOT NULL,observed_at INTEGER NOT NULL)";
    private static final String[] NAMES={TABLE,"r79_legacy_classification_no_update","r79_legacy_classification_no_delete","r79_legacy_classification_no_replace","idx_r79_legacy_classification_candidate"};
    private static final String[] DDL={CREATE,
        "CREATE TRIGGER IF NOT EXISTS "+NAMES[1]+" BEFORE UPDATE ON "+TABLE+" BEGIN SELECT RAISE(ABORT,'R79_ORIGINAL_CLASSIFICATION_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS "+NAMES[2]+" BEFORE DELETE ON "+TABLE+" BEGIN SELECT RAISE(ABORT,'R79_ORIGINAL_CLASSIFICATION_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS "+NAMES[3]+" BEFORE INSERT ON "+TABLE+" WHEN EXISTS(SELECT 1 FROM "+TABLE+" WHERE receipt_hash=NEW.receipt_hash) BEGIN SELECT RAISE(ABORT,'R79_ORIGINAL_CLASSIFICATION_NO_REPLACE'); END",
        "CREATE INDEX IF NOT EXISTS "+NAMES[4]+" ON "+TABLE+"(candidate_key,target_issue,observed_at,receipt_hash)"};
    public static final String HEAD_AUDIT="research_legacy_asset_head_audit_r80";
    public static final String HEAD_BASELINE="research_legacy_asset_head_baseline_r80";
    public static final String HEAD_SNAPSHOT="research_legacy_asset_head_snapshot_r80";
    private static final int AUDIT_STEP_ROWS=8;
    private static final String[] AUDIT_DDL={
        "CREATE TABLE IF NOT EXISTS "+HEAD_AUDIT+"(asset_id TEXT PRIMARY KEY,input_hash TEXT NOT NULL,cursor_n INTEGER NOT NULL,replay_state TEXT NOT NULL,status TEXT NOT NULL,reason TEXT NOT NULL,last_ms INTEGER NOT NULL,last_issue TEXT NOT NULL,generation INTEGER NOT NULL,copy_issue TEXT NOT NULL)",
        "CREATE TABLE IF NOT EXISTS "+HEAD_BASELINE+"(asset_id TEXT PRIMARY KEY,input_hash TEXT NOT NULL,state_hash TEXT NOT NULL,update_credit INTEGER NOT NULL,settled_rows INTEGER NOT NULL,trained_until_issue TEXT NOT NULL)",
        "CREATE TRIGGER IF NOT EXISTS r80_legacy_baseline_no_update BEFORE UPDATE ON "+HEAD_BASELINE+" BEGIN SELECT RAISE(ABORT,'R80_LEGACY_BASELINE_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS r80_legacy_baseline_no_delete BEFORE DELETE ON "+HEAD_BASELINE+" BEGIN SELECT RAISE(ABORT,'R80_LEGACY_BASELINE_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS r80_legacy_baseline_no_replace BEFORE INSERT ON "+HEAD_BASELINE+" WHEN EXISTS(SELECT 1 FROM "+HEAD_BASELINE+" WHERE asset_id=NEW.asset_id) BEGIN SELECT RAISE(ABORT,'R80_LEGACY_BASELINE_NO_REPLACE'); END"};
    private static final String[] AUDIT_NAMES={HEAD_AUDIT,HEAD_BASELINE,"r80_legacy_baseline_no_update","r80_legacy_baseline_no_delete","r80_legacy_baseline_no_replace"};

    private static java.util.LinkedHashMap<String,String> auditFences() {
        java.util.LinkedHashMap<String,String> ddl=new java.util.LinkedHashMap<>();
        ddl.put(HEAD_SNAPSHOT,"CREATE TABLE IF NOT EXISTS "+HEAD_SNAPSHOT+"(asset_id TEXT NOT NULL,generation INTEGER NOT NULL,target_issue TEXT NOT NULL,settled_ms INTEGER NOT NULL,row_blob TEXT NOT NULL,original_identity TEXT NOT NULL,PRIMARY KEY(asset_id,generation,target_issue))");
        ddl.put("idx_r80_asset_snapshot_order","CREATE INDEX IF NOT EXISTS idx_r80_asset_snapshot_order ON "+HEAD_SNAPSHOT+"(asset_id,generation,settled_ms,target_issue)");
        ddl.put("idx_r80_asset_audit_terminal","CREATE INDEX IF NOT EXISTS idx_r80_asset_audit_terminal ON "+HEAD_AUDIT+"(status,asset_id)");
        String[] tables={AssetGenesisEngineR915.TABLE_ASSET_GENOME,AssetGenesisEngineR915.TABLE_ASSET_FREEZE,CandidateExecutionAuditR932.TABLE,ResearchCaptureLedgerR932.TABLE,ResearchCaptureLedgerR932.EVENT,"draws",DrawDependencyInputCP12.TABLE,DbHelper.TABLE_R811_DRAW_CANONICAL};
        for(int i=0;i<tables.length;i++)for(String op:new String[]{"INSERT","UPDATE","DELETE"}) {
            String name="r80_asset_audit_fence_"+i+"_"+op.toLowerCase(java.util.Locale.US);
            String predicate="";
            for(String ref:("UPDATE".equals(op)?new String[]{"OLD","NEW"}:new String[]{"DELETE".equals(op)?"OLD":"NEW"})) {
                String match=i<2?"asset_id="+ref+".asset_id":i<5?"asset_id=substr("+ref+".candidate_key,9) AND "+ref+".candidate_key LIKE 'genesis:%'":
                    "asset_id IN(SELECT asset_id FROM "+AssetGenesisEngineR915.TABLE_ASSET_FREEZE+" WHERE target_issue="+ref+(i==7?".target_issue": ".issue")+" AND hit_label IN(0,1))";
                predicate+=(predicate.isEmpty()?"":" OR ")+"("+match+")";
            }
            ddl.put(name,"CREATE TRIGGER IF NOT EXISTS "+name+" AFTER "+op+" ON "+tables[i]+" BEGIN UPDATE "+HEAD_AUDIT+" SET status='STALE',reason='INPUT_HEAD_CHANGED' WHERE status IN('COPYING','RUNNING') AND ("+predicate+"); END");
        }
        return ddl;
    }

    /** A bounded continuation audit, never a proof producer or a second settlement owner.
     * The original outcomes bind every replay state. A snapshot change invalidates the cursor;
     * an explicit stale cursor restarts from the original zero state. Ambiguous histories stay closed. */
    static String auditAssetHead(SQLiteDatabase db,String id) {
        if(!db.inTransaction())throw new IllegalStateException("R80_ASSET_AUDIT_WRITER_REQUIRED");
        if(assetHeadEligible(db,id))return "ALREADY_ELIGIBLE";
        String spec,stateBlob,trained,earliest;long updates;
        try(Cursor g=db.rawQuery("SELECT genome_spec,state_blob,state_updates,trained_until_issue,earliest_use_issue FROM "+AssetGenesisEngineR915.TABLE_ASSET_GENOME+" WHERE asset_id=?",new String[]{id})) {
            if(!g.moveToFirst())return "HEAD_MISSING";
            spec=g.getString(0);stateBlob=g.getString(1);updates=g.getLong(2);trained=safe(g.getString(3));earliest=safe(g.getString(4));
        }
        String input=V51ChecksumEngine.sha256("R80_ORIGINAL_HEAD"+frame(id)+frame(spec)+frame(stateBlob)+frame(String.valueOf(updates))+frame(trained)+frame(earliest));
        int cursor=0;long lastMs=0,generation=1;String lastIssue="",copyIssue="",phase="COPYING";GenesisAssetVmR915.State replay=new GenesisAssetVmR915.State();
        try(Cursor a=db.rawQuery("SELECT input_hash,cursor_n,replay_state,status,reason,last_ms,last_issue,generation,copy_issue FROM "+HEAD_AUDIT+" WHERE asset_id=?",new String[]{id})) {
            if(a.moveToFirst()) {
                generation=a.getLong(7);
                if("STALE".equals(a.getString(3))) {
                    generation++;
                    db.execSQL("UPDATE "+HEAD_AUDIT+" SET input_hash=?,cursor_n=0,replay_state=?,status='COPYING',reason='RESTARTED_CHANGED_INPUT',last_ms=0,last_issue='',generation=?,copy_issue='' WHERE asset_id=?",new Object[]{input,replay.encode(),generation,id});
                } else {
                    phase=a.getString(3);if(!"RUNNING".equals(phase)&&!"COPYING".equals(phase))return a.getString(4);
                    copyIssue=a.getString(8);
                    if(!input.equals(a.getString(0)))return auditStop(db,id,a.getString(0),"INPUT_HEAD_CHANGED");
                    cursor=a.getInt(1);replay=GenesisAssetVmR915.State.decode(a.getString(2));lastMs=a.getLong(5);lastIssue=a.getString(6);
                    if(!replay.encode().equals(a.getString(2))||cursor<0)return auditStop(db,id,input,"CURSOR_INVALID");
                }
            } else db.execSQL("INSERT INTO "+HEAD_AUDIT+" VALUES(?,?,0,?,'COPYING','PENDING',0,'',1,'')",new Object[]{id,input,replay.encode()});
        }
        // Old temporary generations are cleaned in bounded chunks; originals are never deleted.
        db.execSQL("DELETE FROM "+HEAD_SNAPSHOT+" WHERE rowid IN(SELECT rowid FROM "+HEAD_SNAPSHOT+" WHERE asset_id=? AND generation<? LIMIT 8)",new Object[]{id,generation});
        if("COPYING".equals(phase)) {
            int copied=0;boolean more=false;
            try(Cursor c=db.rawQuery("SELECT target_issue,context_blob,score,actual_number,hit_label,base_hit,brier,settled_ms,outcome_hash,record_hash FROM "+AssetGenesisEngineR915.TABLE_ASSET_FREEZE+" WHERE asset_id=? AND target_issue>? ORDER BY target_issue LIMIT "+(AUDIT_STEP_ROWS+1),new String[]{id,copyIssue})) {
                while(c.moveToNext()) {
                    if(copied==AUDIT_STEP_ROWS){more=true;break;}
                    ResearchWorkBudgetR927.checkpoint();
                    copied++;copyIssue=c.getString(0);
                    if(c.getInt(4)<0)continue;
                    if(c.getLong(7)<=0)return auditStop(db,id,input,"SETTLEMENT_CLOCK_INVALID");
                    org.json.JSONObject payload=new org.json.JSONObject();
                    try{for(int i=0;i<10;i++)payload.put(String.valueOf(i),safe(c.getString(i)));}catch(org.json.JSONException invalid){throw new IllegalStateException(invalid);}
                    copyIssue=c.getString(0);Original original=original(db,"genesis:"+id,copyIssue);
                    db.execSQL("INSERT INTO "+HEAD_SNAPSHOT+" VALUES(?,?,?,?,?,?)",new Object[]{id,generation,copyIssue,c.getLong(7),payload.toString(),original.identity});
                }
            }
            db.execSQL("UPDATE "+HEAD_AUDIT+" SET copy_issue=?,status=? WHERE asset_id=?",new Object[]{copyIssue,more?"COPYING":"RUNNING",id});
            return more?"COPYING":"RUNNING";
        }
        java.util.ArrayList<String[]> rows=new java.util.ArrayList<>();
        try(Cursor c=db.rawQuery("SELECT row_blob,original_identity FROM "+HEAD_SNAPSHOT+" WHERE asset_id=? AND generation=? AND (settled_ms,target_issue)>(?,?) ORDER BY settled_ms,target_issue LIMIT "+(AUDIT_STEP_ROWS+1),new String[]{id,String.valueOf(generation),String.valueOf(lastMs),lastIssue})) {
            while(c.moveToNext()) {
                String[] row=new String[11];
                try{org.json.JSONObject payload=new org.json.JSONObject(c.getString(0));for(int i=0;i<10;i++)row[i]=payload.getString(String.valueOf(i));}catch(org.json.JSONException invalid){return auditStop(db,id,input,"SNAPSHOT_INVALID");}
                row[10]=c.getString(1);rows.add(row);
            }
        }
        GenesisAssetGenomeR915 genome;
        try{genome=GenesisAssetGenomeR915.parse(spec);}catch(RuntimeException invalid){return auditStop(db,id,input,"GENOME_INVALID");}
        int processed=0;
        for(int end=Math.min(rows.size(),AUDIT_STEP_ROWS);processed<end;processed++) {
            ResearchWorkBudgetR927.checkpoint();String[] r=rows.get(processed);
            long at=Long.parseLong(r[7]);
            if(at<=0||(cursor>0&&at==lastMs)||(processed+1<rows.size()&&at==Long.parseLong(rows.get(processed+1)[7])))return auditStop(db,id,input,"SETTLEMENT_ORDER_UNAVAILABLE");
            Models.Draw draw=auditDraw(db,r[0]);Original original=original(db,"genesis:"+id,r[0]);
            String reason=original.identity.equals(r[10])?verifyAssetSettlement(db,id,draw,original):"SNAPSHOT_ORIGINAL_CHANGED";
            if(!"VALID".equals(reason)){record(db,original,draw,"REJECTED",reason);return auditStop(db,id,input,reason);}
            if(at<draw.openTime||at>System.currentTimeMillis())return auditStop(db,id,input,"SETTLEMENT_CLOCK_INVALID");
            GenesisAssetVmR915.PredrawContext context=GenesisAssetVmR915.PredrawContext.decodeFrozen(r[1]);
            int actual=draw.getNumberAtPosition(context.position),hit=(actual==context.n1||actual==context.n2||actual==context.n3)?1:0;
            if(actual!=Integer.parseInt(r[3])||hit!=Integer.parseInt(r[4]))return auditStop(db,id,input,"ORIGINAL_LABEL_MISMATCH");
            boolean outcomeOnly=false;
            try(Cursor prior=db.rawQuery("SELECT 1 FROM "+TABLE+" WHERE candidate_key=? AND target_issue=? AND original_identity_hash=? AND disposition='HISTORICAL_OUTCOME_ONLY' AND reason='VALID'",new String[]{"genesis:"+id,r[0],original.identity})){outcomeOnly=prior.moveToFirst();}
            GenesisAssetVmR915.SettlementResult result=outcomeOnly?new GenesisAssetVmR915.SettlementResult(false,"EXACTLY_ONCE_OR_ORDER_REJECT",replay.hash(),replay.hash()):GenesisAssetVmR915.settle(genome,replay,context,r[0],actual,hit,Double.parseDouble(r[2]));
            if(!result.updated&&!"EXACTLY_ONCE_OR_ORDER_REJECT".equals(result.code))return auditStop(db,id,input,"REPLAY_UPDATE_INVALID");
            String outcome=V51ChecksumEngine.sha256("R915_ASSET_OUTCOME|"+r[9]+'|'+r[0]+'|'+actual+'|'+hit+'|'+Integer.parseInt(r[5])+'|'+Double.parseDouble(r[6])+'|'+result.afterHash);
            if(!outcome.equals(r[8]))return auditStop(db,id,input,"ORIGINAL_OUTCOME_STATE_MISMATCH");
            record(db,original,draw,result.updated?"AUDITED_HISTORICAL_UPDATE":"AUDITED_HISTORICAL_LATE","VALID");
            lastMs=at;lastIssue=r[0];cursor++;
        }
        db.execSQL("UPDATE "+HEAD_AUDIT+" SET cursor_n=?,replay_state=?,last_ms=?,last_issue=? WHERE asset_id=?",new Object[]{cursor,replay.encode(),lastMs,lastIssue,id});
        if(rows.size()>AUDIT_STEP_ROWS)return "RUNNING";
        if(cursor==0)return auditStop(db,id,input,"NO_ORIGINAL_SETTLEMENT_CHAIN");
        if(!replay.encode().equals(stateBlob)||replay.updates!=updates||!replay.lastSettledIssue.equals(trained)||!TargetIssueResolver.next(trained).targetIssue.equals(earliest))return auditStop(db,id,input,"FINAL_STATE_WATERMARK_MISMATCH");
        long classifiedUpdates=0;
        try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM "+TABLE+" WHERE candidate_key=? AND disposition='UPDATED_HEAD' AND reason='VALID'",new String[]{"genesis:"+id})){if(c.moveToFirst())classifiedUpdates=c.getLong(0);}
        if(classifiedUpdates>updates)return auditStop(db,id,input,"UPDATE_CREDIT_INVALID");
        db.execSQL("INSERT INTO "+HEAD_BASELINE+" VALUES(?,?,?,?,?,?)",new Object[]{id,input,replay.hash(),updates-classifiedUpdates,cursor,trained});
        db.execSQL("UPDATE "+HEAD_AUDIT+" SET status='VERIFIED',reason='VALID' WHERE asset_id=?",new Object[]{id});
        return "VERIFIED";
    }
    /** Generation rows duplicate preserved originals. Once terminal, the permanent baseline,
     * original classification receipts and audit verdict remain; only temporary copies expire. */
    static void cleanupTerminalSnapshots(SQLiteDatabase db) {
        if(!db.inTransaction())throw new IllegalStateException("R80_ASSET_AUDIT_WRITER_REQUIRED");
        String id="";
        try(Cursor c=db.rawQuery("SELECT a.asset_id FROM "+HEAD_AUDIT+" a WHERE a.status IN('VERIFIED','UNAVAILABLE') AND EXISTS(SELECT 1 FROM "+HEAD_SNAPSHOT+" s WHERE s.asset_id=a.asset_id) ORDER BY a.status,a.asset_id LIMIT 1",null)){if(c.moveToFirst())id=c.getString(0);}
        if(!id.isEmpty())db.execSQL("DELETE FROM "+HEAD_SNAPSHOT+" WHERE rowid IN(SELECT rowid FROM "+HEAD_SNAPSHOT+" WHERE asset_id=? LIMIT 8)",new Object[]{id});
    }

    private static String auditStop(SQLiteDatabase db,String id,String input,String reason) {
        db.execSQL("INSERT OR IGNORE INTO "+HEAD_AUDIT+" VALUES(?,?,0,?,'UNAVAILABLE',?,0,'',1,'')",new Object[]{id,safe(input),new GenesisAssetVmR915.State().encode(),reason});
        db.execSQL("UPDATE "+HEAD_AUDIT+" SET status='UNAVAILABLE',reason=? WHERE asset_id=? AND status IN('COPYING','RUNNING')",new Object[]{reason,id});return reason;
    }
    private static Models.Draw auditDraw(SQLiteDatabase db,String issue) {
        try(Cursor c=db.rawQuery("SELECT open_time,dirty,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10 FROM "+DrawDependencyInputCP12.SOURCE+" WHERE issue=?",new String[]{issue})) {
            if(!c.moveToFirst())return null;Models.Draw d=new Models.Draw();d.issue=issue;d.openTime=c.getLong(0);d.dirty=c.getInt(1);d.numbers=new int[10];for(int i=0;i<10;i++)d.numbers[i]=c.getInt(i+2);return d;
        }
    }

    private LegacySettlementProofR79() {}
    static void schema(SQLiteDatabase db) {
        if(!db.inTransaction())throw new IllegalStateException("R79_LEGACY_SCHEMA_WRITER_REQUIRED");
        for(String ddl:DDL)db.execSQL(ddl);
        for(String ddl:AUDIT_DDL)db.execSQL(ddl);
        for(String ddl:auditFences().values())db.execSQL(ddl);
        if(!ready(db))throw new IllegalStateException("R79_LEGACY_SCHEMA_FINGERPRINT");
    }
    static boolean ready(SQLiteDatabase db) {
        for(int i=0;i<NAMES.length;i++)try(Cursor c=db.rawQuery("SELECT sql FROM sqlite_master WHERE name=?",new String[]{NAMES[i]})) {
            if(!c.moveToFirst()||!normalize(DDL[i]).equals(normalize(c.getString(0))))return false;
        }
        for(int i=0;i<AUDIT_NAMES.length;i++)try(Cursor c=db.rawQuery("SELECT sql FROM sqlite_master WHERE name=?",new String[]{AUDIT_NAMES[i]})) {
            if(!c.moveToFirst()||!normalize(AUDIT_DDL[i]).equals(normalize(c.getString(0))))return false;
        }
        for(java.util.Map.Entry<String,String> e:auditFences().entrySet())try(Cursor c=db.rawQuery("SELECT sql FROM sqlite_master WHERE name=?",new String[]{e.getKey()})) {
            if(!c.moveToFirst()||!normalize(e.getValue()).equals(normalize(c.getString(0))))return false;
        }
        return true;
    }
    private static String normalize(String text){return safe(text).replace("IF NOT EXISTS ","").replaceAll("\\s+"," ").trim();}

    static final class Original {
        final String key,target,record,proof,event,identity;final long frozenAt,executionAt,captureAt;
        Original(String key,String target,String record,String proof,String event,long frozen,long execution,long capture,String detail) {
            this.key=key;this.target=target;this.record=safe(record);this.proof=safe(proof);this.event=safe(event);
            frozenAt=frozen;executionAt=execution;captureAt=capture;
            identity=V51ChecksumEngine.sha256("R79_ORIGINAL_CAPTURE|"+key+'|'+target+'|'+this.record+'|'+this.proof+'|'+this.event+'|'+frozen+'|'+execution+'|'+capture+'|'+detail);
        }
    }
    static Original original(SQLiteDatabase db,String key,String target) {
        boolean asset=key.startsWith("genesis:");String id=key.substring(key.indexOf(':')+1),record="",proof="",event="",detail="";
        long frozen=0,execution=0,capture=0;
        String table=asset?AssetGenesisEngineR915.TABLE_ASSET_FREEZE:DbHelper.TABLE_R908_ALGORITHM_PREDRAW;
        try(Cursor c=db.rawQuery("SELECT record_hash,"+(asset?"frozen_ms,asset_version_hash,base_issue,history_hash,surface_hash,history_rows,position,n1,n2,n3,action_ordinal,score,confidence,risk,stability,output_type,output_shape,output_blob,output_hash,context_blob,context_digest":"frozen_at,position,n1,n2,n3,selected_ordinal,score,feature_blob,feature_hash")+" FROM "+table+" WHERE "+(asset?"asset_id":"genome_id")+"=? AND target_issue=?",new String[]{id,target})) {
            if(c.moveToFirst()){record=safe(c.getString(0));frozen=c.getLong(1);for(int i=2;i<c.getColumnCount();i++)detail+=frame(c.getString(i));}
        }
        if(!asset)try(Cursor c=db.rawQuery("SELECT phase,position,n1,n2,n3,selected_ordinal,score,probability,formula_hash,input_hash,frozen_at,record_hash FROM "+DbHelper.TABLE_R817_RESEARCH_PREDRAW+" WHERE candidate_id=? AND target_issue=?",new String[]{id,target})) {
            if(c.moveToFirst())for(int i=0;i<c.getColumnCount();i++)detail+=frame(c.getString(i));
        }
        try(Cursor c=db.rawQuery("SELECT proof_hash,frozen_at,payload,structural_hash,input_hash,base_issue,prediction_hash,ranking_hash,score_hash,code_hash,schema_hash,atom_graph_hash,action_count,max_error,elapsed_ns FROM "+CandidateExecutionAuditR932.TABLE+" WHERE candidate_key=? AND target_issue=?",new String[]{key,target})) {
            if(c.moveToFirst()){
                proof=safe(c.getString(0));execution=c.getLong(1);
                detail+=frame(V51ChecksumEngine.sha256(safe(c.getString(2))));
                // Preserve the complete original proof row, including duplicated columns.
                // Correcting a rejected column later cannot retroactively repair the original.
                for(int i=3;i<c.getColumnCount();i++)detail+=frame(c.getString(i));
            }
        }
        try(Cursor c=db.rawQuery("SELECT event_hash,reason,at_ms FROM "+ResearchCaptureLedgerR932.EVENT+" WHERE candidate_key=? AND target_issue=? AND status='FROZEN' AND record_hash=? ORDER BY at_ms,event_hash LIMIT 1",new String[]{key,target,record})) {
            if(c.moveToFirst()){event=safe(c.getString(0));capture=c.getLong(2);detail+=frame(c.getString(1));}
        }
        try(Cursor c=db.rawQuery("SELECT frozen,full1200,status,record_hash,updated_at FROM "+ResearchCaptureLedgerR932.TABLE+" WHERE candidate_key=? AND target_issue=?",new String[]{key,target})) {
            if(c.moveToFirst())for(int i=0;i<5;i++)detail+=frame(c.getString(i));
        }
        return new Original(key,target,record,proof,event,frozen,execution,capture,detail);
    }
    static boolean unchanged(SQLiteDatabase db,Original original) {
        try(Cursor c=db.rawQuery("SELECT original_identity_hash FROM "+TABLE+" WHERE candidate_key=? AND target_issue=? ORDER BY rowid LIMIT 1",new String[]{original.key,original.target})) {
            return !c.moveToFirst()||original.identity.equals(c.getString(0));
        }
    }
    static void record(SQLiteDatabase db,Original original,Models.Draw draw,String disposition,String reason) {
        if(!db.inTransaction())throw new IllegalStateException("R79_CLASSIFICATION_WRITER_REQUIRED");
        String drawHash=draw!=null&&draw.isValid()?DrawHashR36.sha256V2(draw):"";long opened=draw==null?0:draw.openTime;
        String receipt=V51ChecksumEngine.sha256("R79_LEGACY_CLASSIFICATION|"+original.identity+'|'+drawHash+'|'+opened+'|'+disposition+'|'+reason);
        try(Cursor c=db.rawQuery("SELECT 1 FROM "+TABLE+" WHERE receipt_hash=?",new String[]{receipt})){if(c.moveToFirst())return;}
        db.execSQL("INSERT INTO "+TABLE+" VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{receipt,original.key,original.target,original.identity,original.record,original.proof,original.event,original.frozenAt,original.executionAt,original.captureAt,drawHash,opened,disposition,reason,System.currentTimeMillis()});
    }

    /** One shared authoritative read inside the existing owner's writer transaction. */
    static String authoritativeDraw(SQLiteDatabase db,Models.Draw draw) {
        if(draw==null||!draw.isValid()||draw.dirty!=0||draw.openTime<=0||draw.openTime>System.currentTimeMillis()
                ||!DrawCanonicalizerR811.isConfirmedWithinRead(db,draw))return "AUTHORITATIVE_DRAW_REQUIRED";
        try(Cursor d=db.rawQuery("SELECT d.open_time,d.dirty,EXISTS(SELECT 1 FROM draws hot WHERE hot.issue=d.issue),d.n1,d.n2,d.n3,d.n4,d.n5,d.n6,d.n7,d.n8,d.n9,d.n10 FROM "+DrawDependencyInputCP12.SOURCE+" d WHERE d.issue=?",new String[]{draw.issue})) {
            if(!d.moveToFirst()||d.getLong(0)!=draw.openTime||d.getInt(1)!=0)return "EXACT_DRAW_CLOCK_REQUIRED";
            for(int i=0;i<10;i++)if(d.getInt(3+i)!=draw.numbers[i])return "AUTHORITATIVE_DRAW_VALUES_CHANGED";
            if(d.getInt(2)==0&&DrawDependencyInputCP12.verifiedOpenTimeR77(db,draw.issue)!=draw.openTime)return "RETAINED_DRAW_PROOF_REQUIRED";
        }
        return "VALID";
    }

    static boolean currentAlgorithmEligible(SQLiteDatabase db,String id,String target) {
        try(Cursor c=db.rawQuery("SELECT 1 FROM "+DbHelper.TABLE_R908_ALGORITHM_PREDRAW+" p WHERE p.genome_id=? AND p.target_issue=? AND ("+AlgorithmProofRecoveryR948.eligible("p.genome_id","p.target_issue","p.record_hash")+")",new String[]{id,target})) {
            return c.moveToFirst();
        }
    }
    private static String captureTerminal(SQLiteDatabase db,Original original,long before) {
        try(Cursor c=db.rawQuery("SELECT frozen,full1200,status,record_hash,updated_at FROM "+ResearchCaptureLedgerR932.TABLE+" WHERE candidate_key=? AND target_issue=?",new String[]{original.key,original.target})) {
            if(!c.moveToFirst())return "CAPTURE_TERMINAL_MISSING";
            if(c.getInt(0)!=1||c.getInt(1)!=1||!"FROZEN".equals(c.getString(2)))return "FULL1200_TERMINAL_INVALID";
            if(!original.record.equals(c.getString(3)))return "CAPTURE_RECORD_JOIN_MISMATCH";
            if(c.getLong(4)<=0||c.getLong(4)>=before)return "STRICT_CAPTURE_TERMINAL_CLOCK_REQUIRED";
        }
        try(Cursor c=db.rawQuery("SELECT event_hash,reason,at_ms FROM "+ResearchCaptureLedgerR932.EVENT+" WHERE candidate_key=? AND target_issue=? AND status='FROZEN' AND record_hash=? ORDER BY at_ms,event_hash LIMIT 1",new String[]{original.key,original.target,original.record})) {
            if(!c.moveToFirst())return "FROZEN_EVENT_MISSING";
            if(!V51ChecksumEngine.sha256(original.target+'|'+original.key+"|FROZEN|"+c.getString(1)+'|'+original.record).equals(c.getString(0)))return "FROZEN_EVENT_HASH_MISMATCH";
            if(c.getLong(2)<=0||c.getLong(2)>=before)return "STRICT_CAPTURE_EVENT_CLOCK_REQUIRED";
        }
        return "VALID";
    }
    /** Original capture validation for prospective consumers; no labels or classification writes. */
    static String verifyAlgorithmCapture(SQLiteDatabase db,String id,String target) {
        Original original=original(db,"algorithm:"+id,target);
        if(!unchanged(db,original))return "ORIGINAL_CAPTURE_CHANGED_AFTER_CLASSIFICATION";
        String reason=CandidateExecutionAuditR932.verifyAlgorithm(db,id,target);
        if(!"VALID".equals(reason))return reason;
        if(original.frozenAt<=0||original.executionAt<=0||original.frozenAt>System.currentTimeMillis()||original.executionAt>System.currentTimeMillis())return "STRICT_PREDRAW_CLOCK_REQUIRED";
        return captureTerminal(db,original,Long.MAX_VALUE);
    }
    /** Preserve R79's authoritative/retained-draw checks, adding immutable original capture guards. */
    static String verifyAlgorithmSettlement(SQLiteDatabase db,String id,Models.Draw draw,Original original) {
        if(!unchanged(db,original))return "ORIGINAL_CAPTURE_CHANGED_AFTER_CLASSIFICATION";
        String reason=CandidateExecutionAuditR932.verifyAlgorithmSettlementR79(db,id,draw);
        if(!"VALID".equals(reason))return reason;
        reason=authoritativeDraw(db,draw);if(!"VALID".equals(reason))return reason;
        return captureTerminal(db,original,draw.openTime);
    }

    /** Verify original asset bytes and original full1200 proof, without evaluating a model. */
    static String verifyAsset(SQLiteDatabase db,String id,String target) {
        final String key="genesis:"+id;
        try(Cursor f=db.rawQuery("SELECT f.asset_version_hash,f.base_issue,f.history_hash,f.surface_hash,f.history_rows,f.position,f.n1,f.n2,f.n3,f.action_ordinal,f.score,f.confidence,f.risk,f.stability,f.output_type,f.output_shape,f.output_blob,f.output_hash,f.context_blob,f.context_digest,f.record_hash,g.structural_hash,g.genome_spec FROM "+AssetGenesisEngineR915.TABLE_ASSET_FREEZE+" f JOIN "+AssetGenesisEngineR915.TABLE_ASSET_GENOME+" g ON g.asset_id=f.asset_id WHERE f.asset_id=? AND f.target_issue=?",new String[]{id,target})) {
            if(!f.moveToFirst())return "PREDICTION_MISSING";
            try(Cursor p=db.rawQuery("SELECT 1 FROM "+CandidateExecutionAuditR932.TABLE+" WHERE candidate_key=? AND target_issue=?",new String[]{key,target})){if(!p.moveToFirst())return "EXECUTION_PROOF_MISSING";}
            String structure=safe(f.getString(0)),record=safe(f.getString(20));
            if(!hash(structure)||!structure.equals(f.getString(21)))return "STRUCTURE_JOIN_MISMATCH";
            GenesisAssetGenomeR915 genome;
            try{genome=GenesisAssetGenomeR915.parse(f.getString(22));}catch(RuntimeException invalid){return "GENOME_INVALID";}
            if(!id.equals(genome.assetId)||!structure.equals(genome.structuralHash))return "GENOME_IDENTITY_MISMATCH";
            GenesisAssetVmR915.PredrawContext context;
            try{context=GenesisAssetVmR915.PredrawContext.decodeFrozen(f.getString(18));}catch(RuntimeException invalid){return "FROZEN_CONTEXT_INVALID";}
            if(!context.encodeFrozen().equals(f.getString(18))||!context.digest().equals(f.getString(19)))return "FROZEN_CONTEXT_HASH_MISMATCH";
            if(!target.equals(context.targetIssue)||!context.baseIssue.equals(f.getString(1))||!context.historyHash.equals(f.getString(2))||!context.surfaceHash.equals(f.getString(3))||context.historyRows!=f.getInt(4))return "FROZEN_CONTEXT_INPUT_MISMATCH";
            if(!AppConfig.isValidPosition(f.getInt(5))||!DecisionGateEngine.b12K3(f.getInt(6),f.getInt(7),f.getInt(8)))return "FROZEN_ACTION_INVALID";
            int ordinal=K3ActionSpaceEngine.globalOrdinal(f.getInt(5),f.getInt(6),f.getInt(7),f.getInt(8));
            if(context.position!=f.getInt(5)||context.n1!=f.getInt(6)||context.n2!=f.getInt(7)||context.n3!=f.getInt(8)||ordinal!=f.getInt(9)||!DecisionGateEngine.b12K3(f.getInt(6),f.getInt(7),f.getInt(8)))return "FROZEN_ACTION_INVALID";
            // Context actionOrdinal is a historical rank index; the frozen action_ordinal and
            // proof selected_ordinal carry the actual global action identity.
            try {
                GenesisAssetGenomeR915.OutputType type=GenesisAssetGenomeR915.OutputType.valueOf(f.getString(14));
                String[] out=safe(f.getString(16)).split("\\|",-1);
                if(out.length!=2+type.flatSize||!type.name().equals(out[0])||!type.shape.equals(out[1])||!type.shape.equals(f.getString(15))||type!=genome.outputType)return "FROZEN_OUTPUT_SHAPE_MISMATCH";
                for(int i=10;i<=13;i++)if(!Double.isFinite(f.getDouble(i))||f.getDouble(i)<0||f.getDouble(i)>1)return "FROZEN_OUTPUT_INVALID";
                double[] values=new double[out.length-2];for(int i=0;i<values.length;i++){values[i]=Double.longBitsToDouble(Long.parseUnsignedLong(out[i+2],16));if(!Double.isFinite(values[i]))return "FROZEN_OUTPUT_INVALID";}
                GenesisAssetVmR915.AssetValue value=new GenesisAssetVmR915.AssetValue(type,f.getDouble(10),f.getDouble(11),f.getDouble(12),f.getDouble(13),values);
                if(!value.valueHash.equals(f.getString(17))||!value.encodeOutput().equals(f.getString(16)))return "FROZEN_OUTPUT_HASH_MISMATCH";
            }catch(RuntimeException invalid){return "FROZEN_OUTPUT_INVALID";}
            if(!V51ChecksumEngine.sha256("R915_ASSET_FREEZE|"+id+'|'+target+'|'+structure+'|'+f.getString(19)+'|'+f.getString(17)).equals(record))return "FROZEN_RECORD_HASH_MISMATCH";
            try(Cursor e=db.rawQuery("SELECT structural_hash,input_hash,base_issue,prediction_hash,ranking_hash,score_hash,code_hash,schema_hash,atom_graph_hash,action_count,max_error,payload,proof_hash FROM "+CandidateExecutionAuditR932.TABLE+" WHERE candidate_key=? AND target_issue=?",new String[]{key,target})) {
                if(!e.moveToFirst())return "EXECUTION_PROOF_MISSING";
                if(!structure.equals(e.getString(0)))return "STRUCTURE_JOIN_MISMATCH";
                if(!context.surfaceHash.equals(e.getString(1))||!context.baseIssue.equals(e.getString(2)))return "PROOF_INPUT_JOIN_MISMATCH";
                if(!record.equals(e.getString(3)))return "PREDICTION_HASH_JOIN_MISMATCH";
                if(e.getInt(9)!=1200||!Double.isFinite(e.getDouble(10))||e.getDouble(10)<0||e.getDouble(10)>1e-12d)return "FULL1200_TERMINAL_INVALID";
                String payload=e.getString(11);if(payload==null||payload.length()>262144||!V51ChecksumEngine.sha256(payload).equals(e.getString(12)))return "PROOF_PAYLOAD_HASH_MISMATCH";
                try {
                    JSONObject proof=new JSONObject(payload);
                    if(!CandidateExecutionAuditR932.REVISION.equals(proof.getString("revision"))||!key.equals(proof.getString("candidate_key"))||!target.equals(proof.getString("target_issue"))||proof.getBoolean("may_authorize_real")||proof.getInt("selected_ordinal")!=ordinal||proof.getInt("action_count")!=1200||!"FULL1200_SERIALIZED_PROGRAM_STATE_REEXECUTION".equals(proof.getString("parity_scope")))return "PROOF_PAYLOAD_IDENTITY_MISMATCH";
                    for(int i:new int[]{0,1,3,4,5,6,7,8})if(!hash(e.getString(i)))return "PROOF_HASH_FORMAT_INVALID";
                    String[] fields={"structural_hash","input_hash","base_issue","prediction_hash","ranking_hash","score_hash","code_hash","schema_hash","atom_graph_hash"};
                    for(int i=0;i<fields.length;i++)if(!safe(e.getString(i)).equals(proof.getString(fields[i])))return "PROOF_COLUMN_MISMATCH_"+fields[i];
                    if(!V51ChecksumEngine.sha256(genome.canonical()).equals(proof.getString("program_hash"))||!hash(proof.getString("state_hash"))||Double.compare(proof.getDouble("max_error"),e.getDouble(10))!=0)return "PROOF_PIT_OR_STATE_INVALID";
                }catch(org.json.JSONException invalid){return "PROOF_JSON_INVALID";}
            }
            try(Cursor c=db.rawQuery("SELECT frozen,full1200,record_hash,status FROM "+ResearchCaptureLedgerR932.TABLE+" WHERE candidate_key=? AND target_issue=?",new String[]{key,target})) {
                if(!c.moveToFirst())return "CAPTURE_TERMINAL_MISSING";
                if(c.getInt(0)!=1||c.getInt(1)!=1||!"FROZEN".equals(c.getString(3)))return "FULL1200_TERMINAL_INVALID";
                if(!record.equals(c.getString(2)))return "CAPTURE_RECORD_JOIN_MISMATCH";
            }
            try(Cursor c=db.rawQuery("SELECT event_hash,reason FROM "+ResearchCaptureLedgerR932.EVENT+" WHERE candidate_key=? AND target_issue=? AND status='FROZEN' AND record_hash=? ORDER BY at_ms,event_hash LIMIT 1",new String[]{key,target,record})) {
                if(!c.moveToFirst())return "FROZEN_EVENT_MISSING";
                if(!V51ChecksumEngine.sha256(target+'|'+key+"|FROZEN|"+c.getString(1)+'|'+record).equals(c.getString(0)))return "FROZEN_EVENT_HASH_MISMATCH";
            }
            return "VALID";
        }
    }
    static String verifyAssetSettlement(SQLiteDatabase db,String id,Models.Draw draw,Original original) {
        if(!unchanged(db,original))return "ORIGINAL_CAPTURE_CHANGED_AFTER_CLASSIFICATION";
        if(draw==null)return "AUTHORITATIVE_DRAW_REQUIRED";
        String reason=verifyAsset(db,id,draw.issue);if(!"VALID".equals(reason))return reason;
        reason=authoritativeDraw(db,draw);if(!"VALID".equals(reason))return reason;
        if(original.frozenAt<=0||original.executionAt<=0||original.frozenAt>=draw.openTime||original.executionAt>=draw.openTime)return "STRICT_PREDRAW_CLOCK_REQUIRED";
        if(original.captureAt<=0||original.captureAt>=draw.openTime)return "STRICT_CAPTURE_EVENT_CLOCK_REQUIRED";
        try(Cursor c=db.rawQuery("SELECT updated_at FROM "+ResearchCaptureLedgerR932.TABLE+" WHERE candidate_key=? AND target_issue=?",new String[]{original.key,draw.issue})) {
            if(!c.moveToFirst()||c.getLong(0)<=0||c.getLong(0)>=draw.openTime)return "STRICT_CAPTURE_TERMINAL_CLOCK_REQUIRED";
        }
        return "VALID";
    }

    static String assetOutcomeEligibleSql(String id,String issue,String record) {
        return "EXISTS(SELECT 1 FROM "+TABLE+" lp79 WHERE lp79.candidate_key='genesis:'||"+id+" AND lp79.target_issue="+issue+" AND lp79.record_hash="+record+" AND lp79.disposition IN('UPDATED_HEAD','LATE_OUTCOME_ONLY') AND lp79.reason='VALID')";
    }
    /** Previously settled legacy heads are not silently blessed by a new valid pending label. */
    static String assetHeadEligibleSql(String id) {
        return "EXISTS(SELECT 1 FROM "+AssetGenesisEngineR915.TABLE_ASSET_GENOME+" hg79 WHERE hg79.asset_id="+id
            +" AND hg79.state_updates=(SELECT COUNT(*) FROM "+TABLE+" hc79 WHERE hc79.candidate_key='genesis:'||hg79.asset_id AND hc79.disposition='UPDATED_HEAD' AND hc79.reason='VALID')+COALESCE((SELECT hb80.update_credit FROM "+HEAD_BASELINE+" hb80 WHERE hb80.asset_id=hg79.asset_id AND EXISTS(SELECT 1 FROM "+HEAD_AUDIT+" ha80 WHERE ha80.asset_id=hg79.asset_id AND ha80.status='VERIFIED')),0)"
            +" AND NOT EXISTS(SELECT 1 FROM "+AssetGenesisEngineR915.TABLE_ASSET_FREEZE+" hf79 WHERE hf79.asset_id=hg79.asset_id AND hf79.hit_label IN(0,1) AND NOT EXISTS(SELECT 1 FROM "+TABLE+" hr79 WHERE hr79.candidate_key='genesis:'||hf79.asset_id AND hr79.target_issue=hf79.target_issue AND hr79.record_hash=hf79.record_hash AND hr79.reason='VALID' AND (hr79.disposition IN('UPDATED_HEAD','LATE_OUTCOME_ONLY','HISTORICAL_OUTCOME_ONLY') OR (hr79.disposition IN('AUDITED_HISTORICAL_UPDATE','AUDITED_HISTORICAL_LATE') AND EXISTS(SELECT 1 FROM "+HEAD_BASELINE+" hb80 WHERE hb80.asset_id=hg79.asset_id))))))";
    }
    static boolean assetHeadEligible(SQLiteDatabase db,String id) {
        try(Cursor c=db.rawQuery("SELECT 1 WHERE "+assetHeadEligibleSql("?"),new String[]{id})){return c.moveToFirst();}
    }
    private static String frame(String value){String s=safe(value);return "|"+s.length()+":"+s;}
    private static boolean hash(String value){return value!=null&&value.matches("[0-9a-f]{64}");}
    private static String safe(String value){return value==null?"":value;}
}
