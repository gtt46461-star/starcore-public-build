package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.DatabaseCoordinator;
import com.ethk3.novaonepro.RuntimeObservationScopeR341;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Append-only commit observations in a verified installation process. */
public final class RepairEffectLedgerR341 {
    public static final String INSTALL = "r341_repair_install";
    public static final String EFFECT = "r341_repair_effect";
    public static final String OBSERVATION = "r342_repair_observation";
    public static final String OBSERVATION_INDEX = "idx_r342_repair_observation_latest";
    private static final int[] R341_BATCH_COUNTS = {5,4,4,6,4,5,5,7,5,5,6,6};
    private static volatile String processInstall;

    public static void installWithin(SQLiteDatabase db) {
        if(!db.inTransaction())throw new IllegalStateException("R341_REPAIR_SCHEMA_OWNER");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+INSTALL+"(install_id TEXT PRIMARY KEY,source_commit TEXT NOT NULL,"
                +"apk_sha256 TEXT NOT NULL,version_code INTEGER NOT NULL,process_epoch TEXT NOT NULL,"
                +"database_identity TEXT NOT NULL,started_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+EFFECT+"(install_id TEXT NOT NULL,issue_id TEXT NOT NULL,operation TEXT NOT NULL,"
                +"commit_key TEXT NOT NULL,payload_hash TEXT NOT NULL,observed_at INTEGER NOT NULL,PRIMARY KEY(install_id,issue_id))");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+OBSERVATION+"(install_id TEXT NOT NULL REFERENCES "+INSTALL+"(install_id),"
                +"issue_id TEXT NOT NULL,operation TEXT NOT NULL,commit_key TEXT NOT NULL,payload_hash TEXT NOT NULL,"
                +"observed_at INTEGER NOT NULL,PRIMARY KEY(install_id,issue_id,commit_key))");
        db.execSQL("CREATE INDEX IF NOT EXISTS "+OBSERVATION_INDEX+" ON "+OBSERVATION
                +"(install_id,issue_id,observed_at DESC,commit_key DESC)");
    }

    public static void recordInstall(DatabaseCoordinator owner, Map<String,Object> identity) {
        if(processInstall!=null||!"VERIFIED_INSTALLATION".equals(identity.get("state")))return;
        owner.submitBackgroundWrite("R341_REPAIR_INSTALL",db->{
            RuntimeObservationScopeR341 scope=RuntimeObservationScopeR341.capture(null,RuntimeObservationScopeR341.databaseIdentity(db),null,RuntimeObservationScopeR341.SYSTEM);
            if(!scope.identified()||!scope.sourceCommit.equals(identity.get("sourceCommit")))return;
            String installation=StatsR27.sha256(scope.sourceCommit+"|"+identity.get("apkSha256")+"|"+scope.processEpoch+"|"+scope.databaseIdentity);
            db.execSQL("INSERT OR IGNORE INTO "+INSTALL+" VALUES(?,?,?,?,?,?,?)",new Object[]{installation,scope.sourceCommit,
                    identity.get("apkSha256"),identity.get("installedVersionCode"),scope.processEpoch,scope.databaseIdentity,scope.sampleWallAt});
            processInstall=installation;
        });
    }

    private static String issueFor(String operation) {
        if("R335_CHECKPOINT_RECOVERY_RECEIPT".equals(operation))return "R341-B10-03";
        if("R31_ATOMNET_PUBLISH".equals(operation))return "R341-B07-04";
        return null;
    }

    public static void appendObservation(DatabaseCoordinator owner, String operation, DatabaseCoordinator.CommitIdentityR31 receipt) {
        String scope=processInstall, issue=issueFor(operation);
        if(scope==null||issue==null)return;
        owner.submitBackgroundWrite("R341_REPAIR_EFFECT",db->appendVerified(db,scope,operation,receipt.commitKey,receipt.payloadHash));
    }

    static void appendVerified(SQLiteDatabase db,String install,String operation,String key,String payload) {
        String issue=issueFor(operation);
        if(issue==null)return;
        String readback=" SELECT i.install_id,?,r.operation,r.commit_key,r.payload_hash,r.committed_at FROM "+INSTALL
                +" i JOIN r31_commit_receipt r ON r.committed_at>=i.started_at WHERE i.install_id=? AND r.commit_key=?"
                +" AND r.payload_hash=? AND r.operation=?";
        Object[] bindings={issue,install,key,payload,operation};
        db.execSQL("INSERT OR IGNORE INTO "+OBSERVATION+readback,bindings);
        db.execSQL("INSERT OR IGNORE INTO "+EFFECT+readback,bindings);
    }

    private static Map<String,Map<String,Object>> issueRegistry() {
        Map<String,Map<String,Object>> issues=new LinkedHashMap<>();
        for(int batch=1;batch<=R341_BATCH_COUNTS.length;batch++){
            for(int n=1;n<=R341_BATCH_COUNTS[batch-1];n++){
                String id=String.format(java.util.Locale.ROOT,"R341-B%02d-%02d",batch,n);
                Map<String,Object> row=new LinkedHashMap<>();
                row.put("issueId",id);
                row.put("effectState","PRODUCTION_EFFECT_NOT_VERIFIED");
                row.put("engineering","DELIVERY_RECEIPT_REQUIRED");
                row.put("field","MATCHED_FIELD_ORIGINAL_REQUIRED");
                row.put("natural","INDEPENDENT_FUTURE_RECEIPT_IF_APPLICABLE");
                row.put("observationCount",null);
                issues.put(id,row);
            }
        }
        return issues;
    }

    static Map<String,Object> read(SQLiteDatabase db,Map<?,?> installation,String process,android.os.CancellationSignal cancellation) {
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("engineering","INDEPENDENT_DELIVERY_LEDGER");result.put("field","FIELD_PENDING");
        result.put("natural","NATURAL_PENDING");result.put("processEpoch",process);
        Map<String,Map<String,Object>> registry=issueRegistry();
        result.put("registryVersion","R342_R341_ISSUE_SCOPE_1");
        result.put("issues",new ArrayList<>(registry.values()));
        List<Map<String,Object>> observed=new ArrayList<>();result.put("observations",observed);
        if(process==null||!"VERIFIED_INSTALLATION".equals(installation.get("state"))){result.put("sourceState","UNVERIFIED_INSTALLATION_OR_PROCESS");return result;}
        try(Cursor rows=db.rawQuery("SELECT e.issue_id,e.operation,e.commit_key,e.payload_hash,e.observed_at,i.started_at FROM "+INSTALL
                +" i JOIN "+EFFECT+" e ON e.install_id=i.install_id WHERE i.source_commit=? AND i.apk_sha256=? AND i.process_epoch=? ORDER BY e.issue_id LIMIT 62",
                new String[]{String.valueOf(installation.get("sourceCommit")),String.valueOf(installation.get("apkSha256")),process},cancellation)){
            while(rows.moveToNext()){
                Map<String,Object> row=new LinkedHashMap<>();row.put("issueId",rows.getString(0));row.put("commitOperation",rows.getString(1));
                row.put("commitReceiptKey",rows.getString(2));row.put("commitPayloadHash",rows.getString(3));row.put("observedAt",rows.getLong(4));
                row.put("effectScopeStartedAt",rows.getLong(5));observed.add(row);
                Map<String,Object> issue=registry.get(rows.getString(0));
                if(issue!=null)issue.put("firstCommitObservation",row);
            }
            result.put("sourceState",observed.isEmpty()?"OBSERVED_EMPTY":"CURRENT_PROCESS_PRODUCTION_ROWS");
        }catch(android.os.OperationCanceledException stopped){throw stopped;}
        catch(RuntimeException missing){result.put("sourceState","SOURCE_UNAVAILABLE");result.put("reason",missing.getClass().getSimpleName());}
        try(Cursor rows=db.rawQuery("SELECT o.issue_id,o.operation,o.commit_key,o.payload_hash,o.observed_at,"
                +"(SELECT count(*) FROM "+OBSERVATION+" c WHERE c.install_id=i.install_id AND c.issue_id=o.issue_id)"
                +" FROM "+INSTALL+" i JOIN "+OBSERVATION+" o ON o.install_id=i.install_id WHERE i.source_commit=?"
                +" AND i.apk_sha256=? AND i.process_epoch=? AND o.commit_key=(SELECT l.commit_key FROM "+OBSERVATION
                +" l WHERE l.install_id=i.install_id AND l.issue_id=o.issue_id ORDER BY l.observed_at DESC,l.commit_key DESC LIMIT 1)"
                +" ORDER BY o.issue_id LIMIT 62",new String[]{String.valueOf(installation.get("sourceCommit")),
                        String.valueOf(installation.get("apkSha256")),process},cancellation)){
            while(rows.moveToNext()){
                Map<String,Object> issue=registry.get(rows.getString(0));
                if(issue==null)continue;
                Map<String,Object> event=new LinkedHashMap<>();
                event.put("operation",rows.getString(1));event.put("receiptKey",rows.getString(2));
                event.put("payloadHash",rows.getString(3));event.put("observedAt",rows.getLong(4));
                event.put("scope","COMMITTED_OPERATION_ONLY");
                issue.put("latestCommitObservation",event);
                issue.put("observationCount",rows.getLong(5));
                issue.put("effectState","COMMIT_OBSERVED_EFFECT_UNVERIFIED");
            }
            result.put("appendSourceState","EXACT_INSTALLATION_READBACK");
        }catch(android.os.OperationCanceledException stopped){throw stopped;}
        catch(RuntimeException missing){
            result.put("appendSourceState","SOURCE_UNAVAILABLE");
            result.put("appendSourceReason",missing.getClass().getSimpleName());
        }
        return result;
    }

    private RepairEffectLedgerR341() {}
}
