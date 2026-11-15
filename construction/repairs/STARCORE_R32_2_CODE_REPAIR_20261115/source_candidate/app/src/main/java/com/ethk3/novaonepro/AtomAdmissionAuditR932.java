package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;
import org.json.*;

/** Predraw replay audits. Recovery resets observation support; it never restores a certificate. */
public final class AtomAdmissionAuditR932 {
    public static final String AUDIT="research_atom_audit_r932", RECOVERY="research_atom_recovery_r932", ARCHIVE="research_atom_quality_archive_r932";
    public static final int RESTORE_SUPPORT=20;
    private AtomAdmissionAuditR932() {}
    static void schema(SQLiteDatabase db) {
        AtomCaptureR932.schema(db);
        db.execSQL("CREATE TABLE IF NOT EXISTS "+AUDIT+"(target_issue TEXT PRIMARY KEY,revision TEXT NOT NULL,context_hash TEXT NOT NULL,result TEXT NOT NULL,result_hash TEXT NOT NULL,created_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+RECOVERY+"(atom_id TEXT PRIMARY KEY,revision TEXT NOT NULL,last_issue TEXT NOT NULL,pass_streak INTEGER NOT NULL,generation INTEGER NOT NULL,last_source_issue TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+ARCHIVE+"(atom_id TEXT NOT NULL,generation INTEGER NOT NULL,quality_json TEXT NOT NULL,archived_at INTEGER NOT NULL,PRIMARY KEY(atom_id,generation))");
    }
    static String revision(){return V51ChecksumEngine.sha256("R932_ATOM_AUDIT_V1|"+SourceManifestR815.SOURCE_TREE_SHA256+'|'+GenesisSchemaRegistryR915.fingerprint());}
    /** Two fresh evaluations of the same immutable raw context, before any quarantine mask. */
    static boolean record(SQLiteDatabase db,GenesisAssetVmR915.PredrawContext first,GenesisAssetVmR915.PredrawContext replay) {
        if(!db.inTransaction())throw new IllegalStateException("R932_ATOM_AUDIT_REQUIRES_TRANSACTION");
        if(first==null||replay==null||first.observation==null||replay.observation==null)throw new IllegalArgumentException("R932_ATOM_AUDIT_INPUT");
        schema(db);String issue=first.targetIssue, revision=revision();
        try(Cursor c=db.rawQuery("SELECT 1 FROM "+DbHelper.TABLE_DRAWS+" WHERE issue>=? AND dirty=0 LIMIT 1",new String[]{issue})){if(c.moveToFirst())throw new IllegalStateException("R932_ATOM_AUDIT_POSTDRAW");}
        JSONArray results=new JSONArray();String[] failures=new String[240];
        boolean pit=TargetIssueResolver.compare(first.observation.maxSourceIssue,issue)<0&&TargetIssueResolver.compare(replay.observation.maxSourceIssue,issue)<0;
        boolean context=first.baseIssue.equals(replay.baseIssue)&&first.targetIssue.equals(replay.targetIssue)&&first.position==replay.position&&first.n1==replay.n1&&first.n2==replay.n2&&first.n3==replay.n3;
        for(int i=0;i<240;i++) {
            AtomCaptureR932.Values a=first.observation,b=replay.observation;
            String failure=!pit?"PIT_VIOLATION":!context?"REPLAY_CONTEXT_MISMATCH":
                "NONFINITE".equals(a.missingReasons[i])||"NONFINITE".equals(b.missingReasons[i])?"NONFINITE":
                a.present[i]!=b.present[i]||!Objects.equals(a.missingReasons[i],b.missingReasons[i])||
                Double.doubleToLongBits(a.values[i])!=Double.doubleToLongBits(b.values[i])?"NONDETERMINISTIC":"";
            failures[i]=failure;results.put(failure.length()==0?"PASS":failure);
        }
        String result=results.toString(), hash=V51ChecksumEngine.sha256(revision+'|'+first.digest()+'|'+result);
        try(Cursor c=db.rawQuery("SELECT result_hash FROM "+AUDIT+" WHERE target_issue=?",new String[]{issue})) {
            if(c.moveToFirst()){if(!hash.equals(c.getString(0)))throw new IllegalStateException("R932_ATOM_AUDIT_REPLAY_DRIFT");return !result.contains("VIOLATION")&&!result.contains("MISMATCH")&&!result.contains("NONFINITE")&&!result.contains("NONDETERMINISTIC");}
        }
        db.execSQL("INSERT INTO "+AUDIT+" VALUES(?,?,?,?,?,?)",new Object[]{issue,revision,first.digest(),result,hash,System.currentTimeMillis()});
        Map<String,Object[]> previous=new HashMap<String,Object[]>();
        try(Cursor c=db.rawQuery("SELECT atom_id,revision,last_issue,pass_streak,generation,last_source_issue FROM "+RECOVERY,null)){while(c.moveToNext())previous.put(c.getString(0),new Object[]{c.getString(1),c.getString(2),c.getInt(3),c.getInt(4),c.getString(5)});}
        Set<String> quarantined=new HashSet<String>();
        try(Cursor c=db.rawQuery("SELECT atom_id FROM "+AtomCaptureR932.QUALITY+" WHERE state='QUARANTINED'",null)){while(c.moveToNext())quarantined.add(c.getString(0));}
        for(int i=0;i<240;i++) {
            String id=GenesisSchemaRegistryR915.atoms().get(i).id, failure=failures[i];Object[] prior=previous.get(id);
            if(failure.length()==0&&!quarantined.contains(id))continue;
            if(prior!=null&&issue.compareTo((String)prior[1])<=0)continue;
            int generation=prior==null?0:(Integer)prior[3];
            int streak=prior!=null&&revision.equals(prior[0])?(Integer)prior[2]:0;
            if(failure.length()>0) {
                generation++;streak=0;
                db.execSQL("INSERT OR IGNORE INTO "+AtomCaptureR932.QUALITY+" VALUES(?,0,0,0,0,0,0,?,'WATCH','NEW_OBSERVATION')",new Object[]{id,issue});
                db.execSQL("UPDATE "+AtomCaptureR932.QUALITY+" SET state='QUARANTINED',reason=? WHERE atom_id=?",new Object[]{failure+"|audit="+hash,id});
            } else if(prior!=null&&first.observation.maxSourceIssue.compareTo((String)prior[4])<=0) {
                continue; // New target IDs with the same old raw history are not fresh audits.
            } else if(++streak>=RESTORE_SUPPORT) {
                // Retain the complete old observation matrix before restarting support at zero.
                try(Cursor c=db.rawQuery("SELECT n,available_n,mean,m2,total_cost_ns,max_cost_ns,last_issue,state,reason FROM "+AtomCaptureR932.QUALITY+" WHERE atom_id=?",new String[]{id})) {
                    if(c.moveToFirst()){JSONArray old=new JSONArray();for(int k=0;k<9;k++)old.put(c.getString(k));db.execSQL("INSERT INTO "+ARCHIVE+" VALUES(?,?,?,?)",new Object[]{id,generation,old.toString(),System.currentTimeMillis()});}
                }
                db.execSQL("UPDATE "+AtomCaptureR932.QUALITY+" SET n=0,available_n=0,mean=0,m2=0,total_cost_ns=0,max_cost_ns=0,last_issue=?,state='WATCH',reason=? WHERE atom_id=? AND state='QUARANTINED'",new Object[]{issue,"FRESH_AUDIT_RECOVERY_20|"+hash,id});
                generation++;streak=0;
            }
            db.execSQL("INSERT OR REPLACE INTO "+RECOVERY+" VALUES(?,?,?,?,?,?)",new Object[]{id,revision,issue,streak,generation,first.observation.maxSourceIssue});
        }
        for(String failure:failures)if(failure.length()>0)return false;
        return true;
    }
    static String generationHash(SQLiteDatabase db) {
        StringBuilder value=new StringBuilder(revision());
        try(Cursor c=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",new String[]{RECOVERY})){if(!c.moveToFirst())return V51ChecksumEngine.sha256(value.toString());}
        try(Cursor c=db.rawQuery("SELECT atom_id,generation FROM "+RECOVERY+" ORDER BY atom_id",null)){while(c.moveToNext())value.append('|').append(c.getString(0)).append(':').append(c.getInt(1));}
        return V51ChecksumEngine.sha256(value.toString());
    }
}
