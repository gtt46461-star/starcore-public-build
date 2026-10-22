package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.json.*;

/** Per-atom observations measured by the materializer; latency never changes inference identity. */
public final class AtomCaptureR932 {
    public static final String TABLE="research_atom_context_r932",QUALITY="research_atom_quality_r932";
    private AtomCaptureR932() {}
    public static String graphHash(DbStore store) {return graphHash(store.readable());}
    static String graphHash(SQLiteDatabase db) {
        java.util.TreeSet<Integer> blocked=new java.util.TreeSet<Integer>(quarantined(db));
        return V51ChecksumEngine.sha256("R932_ATOM_GRAPH|"+GenesisSchemaRegistryR915.fingerprint()+"|quarantined="+blocked+"|generation="+AtomAdmissionAuditR932.generationHash(db));
    }
    public static final class Values {
        public final double[] values;
        public final boolean[] present;
        public final long[] costNs;
        public final String[] missingReasons;
        public final String maxSourceIssue;
        Values(int n,String max){values=new double[n];present=new boolean[n];costNs=new long[n];missingReasons=new String[n];maxSourceIssue=max;}
        public String payload(String target,String contextHash) {
            try {
                JSONObject root=new JSONObject();root.put("revision","R932_PER_ATOM_CAPTURE_V2");root.put("provenance_revision",AtomProvenanceR932.REVISION);root.put("target_issue",target);root.put("max_source_issue",maxSourceIssue);
                root.put("context_hash",contextHash);root.put("schema_hash",GenesisSchemaRegistryR915.fingerprint());
                JSONArray atoms=new JSONArray();
                for(int i=0;i<values.length;i++){
                    GenesisSchemaRegistryR915.AtomSpec spec=GenesisSchemaRegistryR915.atoms().get(i);JSONObject atom=new JSONObject();
                    atom.put("id",spec.id);atom.put("semantic_tier",spec.semanticTier());atom.put("present",present[i]);atom.put("missing_reason",missingReasons[i]);
                    atom.put("value",present[i]?values[i]:JSONObject.NULL);atom.put("cost_ns",costNs[i]);
                    atom.put("source_owner",AtomProvenanceR932.sourceOwner(i));atom.put("source_function",AtomProvenanceR932.sourceFunction(i));
                    atom.put("source_contract_hash",AtomProvenanceR932.sourceContractHash(i));atom.put("mask_contract",AtomProvenanceR932.maskContract(i));
                    JSONArray consumers=new JSONArray();for(String consumer:AtomProvenanceR932.consumers(i))consumers.put(consumer);atom.put("consumers",consumers);atom.put("consumers_hash",AtomProvenanceR932.consumersHash(i));
                    atom.put("value_hash",V51ChecksumEngine.sha256(spec.canonical()+'|'+maxSourceIssue+'|'+present[i]+'|'+(present[i]?Double.toHexString(values[i]):missingReasons[i])));
                    atoms.put(atom);
                }
                root.put("atoms",atoms);return root.toString();
            }catch(Exception e){throw new IllegalStateException("R932_ATOM_SERIALIZATION",e);}
        }
    }
    static void schema(SQLiteDatabase db) {
        if (TransactionSchemaCacheR941.ready(db,"AtomCaptureR932")) return;

        db.execSQL("CREATE TABLE IF NOT EXISTS "+TABLE+"(target_issue TEXT PRIMARY KEY,context_hash TEXT NOT NULL,max_source_issue TEXT NOT NULL,schema_hash TEXT NOT NULL,payload TEXT NOT NULL,payload_hash TEXT NOT NULL,frozen_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+QUALITY+"(atom_id TEXT PRIMARY KEY,n INTEGER NOT NULL,available_n INTEGER NOT NULL,mean REAL NOT NULL,m2 REAL NOT NULL,total_cost_ns INTEGER NOT NULL,max_cost_ns INTEGER NOT NULL,last_issue TEXT NOT NULL,state TEXT NOT NULL,reason TEXT NOT NULL)");
    
        TransactionSchemaCacheR941.mark(db,"AtomCaptureR932");
    }
    /** Called inside a selected asset's existing freeze transaction. One issue counts once. */
    static void freeze(SQLiteDatabase db,GenesisAssetVmR915.PredrawContext context) {
        if(context.observation==null)return; // Old fixtures and historical contexts have UNKNOWN masks/provenance; never fabricate them.
        if(!db.inTransaction())throw new IllegalStateException("R932_ATOM_FREEZE_TRANSACTION_REQUIRED");
        if(TargetIssueResolver.compare(context.observation.maxSourceIssue,context.targetIssue)>=0)throw new IllegalStateException("R932_ATOM_PIT_VIOLATION");
        schema(db);AtomicToolWorldR943.freeze(db,context);String payload=context.observation.payload(context.targetIssue,context.digest());
        db.execSQL("INSERT OR IGNORE INTO "+TABLE+" VALUES(?,?,?,?,?,?,?)",new Object[]{context.targetIssue,context.digest(),context.observation.maxSourceIssue,GenesisSchemaRegistryR915.fingerprint(),payload,V51ChecksumEngine.sha256(payload),System.currentTimeMillis()});
    }
    public static java.util.Set<Integer> quarantined(DbStore store) {return quarantined(store.readable());}
    static java.util.Set<Integer> quarantined(SQLiteDatabase db) {
        java.util.Set<Integer> blocked=new java.util.HashSet<Integer>();
        try(Cursor exists=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",new String[]{QUALITY})){if(!exists.moveToFirst())return blocked;}
        try(Cursor c=db.rawQuery("SELECT atom_id FROM "+QUALITY+" WHERE state='QUARANTINED'",null)) {
            while(c.moveToNext())for(int i=0;i<240;i++)if(GenesisSchemaRegistryR915.atoms().get(i).id.equals(c.getString(0)))blocked.add(i);
        }return blocked;
    }
    private static void verifyProvenance(JSONObject root,JSONArray atoms)throws JSONException {
        String revision=root.optString("revision","");
        if(!"R932_PER_ATOM_CAPTURE_V2".equals(revision))return; // Historical V1 stays historical; no fabricated provenance upgrade.
        if(!AtomProvenanceR932.REVISION.equals(root.getString("provenance_revision")))throw new IllegalStateException("R932_ATOM_PROVENANCE_REVISION");
        for(int i=0;i<atoms.length();i++){
            JSONObject atom=atoms.getJSONObject(i);JSONArray consumers=atom.getJSONArray("consumers");
            if(consumers.length()==0||!AtomProvenanceR932.sourceOwner(i).equals(atom.getString("source_owner"))||!AtomProvenanceR932.sourceFunction(i).equals(atom.getString("source_function"))||!AtomProvenanceR932.sourceContractHash(i).equals(atom.getString("source_contract_hash"))||!AtomProvenanceR932.maskContract(i).equals(atom.getString("mask_contract"))||!AtomProvenanceR932.consumersHash(i).equals(atom.getString("consumers_hash")))throw new IllegalStateException("R932_ATOM_PROVENANCE_DRIFT:"+i);
            java.util.List<String> expected=AtomProvenanceR932.consumers(i);if(consumers.length()!=expected.size())throw new IllegalStateException("R932_ATOM_CONSUMER_CARDINALITY:"+i);
            for(int j=0;j<expected.size();j++)if(!expected.get(j).equals(consumers.getString(j)))throw new IllegalStateException("R932_ATOM_CONSUMER_DRIFT:"+i+":"+j);
            if(GenesisSchemaRegistryR915.atoms().get(i).settlementEvidence()&&AtomProvenanceR932.hasPredrawScoreConsumer(i))throw new IllegalStateException("R932_POSTDRAW_ATOM_PREDRAW_CONSUMER:"+i);
        }
    }
    /** Small indexed page; advance quality and cursor in one transaction, retaining all raw rows. */
    public static void maintain(final DbStore store) {
        if(RealtimePressureGateR811.hasActiveCriticalWork())return;
        boolean ok=store.coordinatorR36().runAtomicWrite("R932_ATOM_INCREMENTAL_QUALITY",new DatabaseCoordinator.WriteRunnable(){public void run(SQLiteDatabase db){
            schema(db);String cursor="";try(Cursor c=db.rawQuery("SELECT state_value FROM "+DbHelper.TABLE_APP_STATE+" WHERE state_key='r932_atom_quality_cursor'",null)){if(c.moveToFirst())cursor=c.getString(0);}
            java.util.Map<String,Object[]> quality=new java.util.HashMap<String,Object[]>();
            try(Cursor all=db.rawQuery("SELECT atom_id,n,available_n,mean,m2,total_cost_ns,max_cost_ns,last_issue,state,reason FROM "+QUALITY,null)) {
                while(all.moveToNext())quality.put(all.getString(0),new Object[]{all.getString(0),all.getInt(1),all.getInt(2),all.getDouble(3),all.getDouble(4),all.getLong(5),all.getLong(6),all.getString(7),all.getString(8),all.getString(9)});
            }
            try(Cursor rows=BudgetedQueryR927.query(db,"SELECT target_issue,payload,payload_hash FROM "+TABLE+" WHERE target_issue>? ORDER BY target_issue LIMIT 4",new String[]{cursor})) {
                while(rows.moveToNext()) {
                    ResearchWorkBudgetR927.checkpoint();String issue=rows.getString(0),payload=rows.getString(1);
                    if(!V51ChecksumEngine.sha256(payload).equals(rows.getString(2)))throw new IllegalStateException("R932_ATOM_PAYLOAD_CORRUPT");
                    try {
                        JSONObject root=new JSONObject(payload);JSONArray atoms=root.getJSONArray("atoms");
                        if(atoms.length()!=240||!GenesisSchemaRegistryR915.fingerprint().equals(root.getString("schema_hash")))throw new IllegalStateException("R932_ATOM_SCHEMA");
                        verifyProvenance(root,atoms);
                        java.util.List<Object[]> updates=new java.util.ArrayList<Object[]>();
                        for(int i=0;i<atoms.length();i++) {
                            if((i&15)==0)ResearchWorkBudgetR927.checkpoint();
                            JSONObject atom=atoms.getJSONObject(i);String id=atom.getString("id");int n=0,available=0;double mean=0,m2=0;long total=0,max=0;String state="WATCH",reason="NEW_OBSERVATION";
                            Object[] previous=quality.get(id);if(previous!=null&&issue.compareTo((String)previous[7])<=0)continue;if(previous!=null){n=(Integer)previous[1];available=(Integer)previous[2];mean=(Double)previous[3];m2=(Double)previous[4];total=(Long)previous[5];max=(Long)previous[6];state=(String)previous[8];reason=(String)previous[9];}
                            n++;long cost=atom.getLong("cost_ns");total+=cost;max=Math.max(max,cost);
                            if(atom.getBoolean("present")){available++;double value=atom.getDouble("value"),delta=value-mean;mean+=delta/available;m2+=delta*(value-mean);}
                            if(atom.getString("missing_reason").equals("NONFINITE")){state="QUARANTINED";reason="NONFINITE_REQUIRES_NEW_AUDIT_TO_RESTORE";}
                            if(!state.equals("QUARANTINED")) {state=GenesisSchemaRegistryR915.atoms().get(i).productionScoreEligible()?(available<120?"WATCH":"ACTIVE"):"SHADOW_ONLY";reason=available<120?"SUPPORT_PENDING":"OBSERVATION_NOT_EFFECT_CERTIFICATE";}
                            Object[] updated=new Object[]{id,n,available,mean,m2,total,max,issue,state,reason};updates.add(updated);quality.put(id,updated);
                        }
                        for(int start=0;start<updates.size();start+=30){
                            ResearchWorkBudgetR927.checkpoint();StringBuilder sql=new StringBuilder("INSERT OR REPLACE INTO "+QUALITY+" VALUES ");java.util.List<Object> args=new java.util.ArrayList<Object>();
                            for(int i=start;i<Math.min(updates.size(),start+30);i++){if(i>start)sql.append(',');sql.append("(?,?,?,?,?,?,?,?,?,?)");java.util.Collections.addAll(args,updates.get(i));}
                            db.execSQL(sql.toString(),args.toArray());
                        }
                    }catch(org.json.JSONException e){throw new IllegalStateException("R932_ATOM_INVALID_JSON",e);}
                    java.util.Map<String,String> values=new java.util.LinkedHashMap<String,String>();values.put("r932_atom_quality_cursor",issue);DbStore.putStatesR36(db,values);
                }
            }
        }});
        if(!ok)throw new IllegalStateException("R932_ATOM_QUALITY_COMMIT_FAILED");
    }
}
