package com.ethk3.novaonepro;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;

/** Explicitly synthetic fixture for testing the offline replay command; never a user backup. */
public final class R927BackupReplayFixture {
    public static void main(String[] args)throws Exception {
        SQLiteDatabase db=new SQLiteDatabase(args[0],args[1]);
        try {
            V51Repository.ensure(db);DbHelper.ensureR35Schema(db);DbHelper.ensureR36Schema(db);
            db.execSQL("ALTER TABLE "+DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB+" ADD COLUMN core_materialized INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE "+DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB+" ADD COLUMN core_next_ordinal INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE "+DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB+" ADD COLUMN next_action_ordinal INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE "+DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB+" ADD COLUMN cursor_version INTEGER NOT NULL DEFAULT 1");
            RuntimeEventLedgerR926.ensureSchema(db);
            db.execSQL("CREATE TABLE IF NOT EXISTS app_state(state_key TEXT PRIMARY KEY,state_value TEXT,updated_at INTEGER)");
            ArrayList<DeferredEvidenceSnapshotR36.NumberRow> numbers=new ArrayList<DeferredEvidenceSnapshotR36.NumberRow>();
            ArrayList<DeferredEvidenceSnapshotR36.ActionRow> actions=new ArrayList<DeferredEvidenceSnapshotR36.ActionRow>();
            String hash=V51ChecksumEngine.sha256("SYNTHETIC_REPLAY_FIXTURE");
            for(int p=1;p<=10;p++)for(int n=1;n<=10;n++)numbers.add(new DeferredEvidenceSnapshotR36.NumberRow(p,n,.1,.1,n,"state","domain",hash));
            StringBuilder payload=new StringBuilder();
            for(int p=1;p<=10;p++)for(int a=1;a<=8;a++)for(int b=a+1;b<=9;b++)for(int c=b+1;c<=10;c++) {
                int rank=actions.size()+1,n4=1;while(n4==a||n4==b||n4==c)n4++;String id=p+":"+a+":"+b+":"+c,fp=V51ChecksumEngine.sha256(id);
                actions.add(new DeferredEvidenceSnapshotR36.ActionRow(p,a,b,c,n4,.3,.4,.3,rank,fp,"",fp,V51ChecksumEngine.sha256("record:"+id)));
                payload.append(K3ActionSpaceEngine.canonicalPayloadRow(p,a,b,c,n4,.3,.4,rank,fp,fp));
            }
            String actionHash=K3ActionSpaceEngine.actionSpaceHashFromPayloadBody(payload.toString(),1200);
            String record=V51ChecksumEngine.sha256("R36_SNAPSHOT|202609100080|"+hash+'|'+hash+'|'+hash+'|'+actionHash);
            DeferredEvidenceSnapshotR36.Snapshot s=new DeferredEvidenceSnapshotR36.Snapshot("202609100080","202609100079",hash,hash,hash,actionHash,record,"",hash,0,0,0,true,false,"",numbers,actions,new ArrayList<String>(),new ArrayList<RuntimeTraceR36.Span>());
            byte[] blob=DeferredEvidenceSnapshotR36.encode(s);
            db.execSQL("INSERT INTO "+DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB+"(target_issue,job_type,status,snapshot_blob,snapshot_hash,cursor_version,created_at,updated_at,record_hash) VALUES(?,?,?,?,?,1,1,1,?)",new Object[]{s.targetIssue,"FULL","PENDING",blob,DeferredEvidenceSnapshotR36.sha256(blob),record});
            System.out.println("SYNTHETIC_REPLAY_FIXTURE_CREATED");
        }finally{db.shutdown();}
    }
}
