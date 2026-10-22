package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;
import org.json.*;

/** Immutable statistical family and separate, reversible scheduling equivalence clusters. */
public final class ResearchCohortR932 {
    public static final String IDENTITY="research_semantic_identity_r932",COMPARISON="research_similarity_r932",CHAMPION="research_family_champion_r932";
    public static final int MIN_COMMON_FUTURE=120,MIN_BEHAVIOR_PROBES=3;
    public static final double RESIDUAL_THRESHOLD=.95;
    private ResearchCohortR932(){}
    static void schema(SQLiteDatabase db){
        if (TransactionSchemaCacheR941.ready(db,"ResearchCohortR932")) return;

        CandidateExecutionAuditR932.schema(db);
        db.execSQL("CREATE TABLE IF NOT EXISTS "+IDENTITY+"(candidate_key TEXT PRIMARY KEY,structural_hash TEXT NOT NULL,semantic_hash TEXT NOT NULL,domain_scope TEXT NOT NULL,registered_issue TEXT NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r932_semantic_alias ON "+IDENTITY+"(semantic_hash,candidate_key)");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+COMPARISON+"(left_key TEXT NOT NULL,right_key TEXT NOT NULL,left_structure TEXT NOT NULL,right_structure TEXT NOT NULL,common_n INTEGER NOT NULL,correlation REAL,classification TEXT NOT NULL,evidence_json TEXT NOT NULL,evidence_hash TEXT NOT NULL,last_issue TEXT NOT NULL,checked_at INTEGER NOT NULL,PRIMARY KEY(left_key,right_key))");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+CHAMPION+"(family TEXT PRIMARY KEY,candidate_key TEXT NOT NULL,structural_hash TEXT NOT NULL,certificate_hash TEXT NOT NULL,evidence_hash TEXT NOT NULL,selected_issue TEXT NOT NULL)");
    
        TransactionSchemaCacheR941.mark(db,"ResearchCohortR932");
    }
    static void register(SQLiteDatabase db,String key,String structure,String semantic,String domain,String issue){
        if(!db.inTransaction()||!structure.matches("[0-9a-f]{64}")||!semantic.matches("[0-9a-f]{64}")||domain==null||domain.isEmpty())throw new IllegalArgumentException("R932_SEMANTIC_REGISTRATION");
        schema(db);
        db.execSQL("INSERT OR IGNORE INTO "+IDENTITY+" VALUES(?,?,?,?,?)",new Object[]{key,structure,semantic,domain,issue});
        try(Cursor c=db.rawQuery("SELECT structural_hash,semantic_hash,domain_scope FROM "+IDENTITY+" WHERE candidate_key=?",new String[]{key})){
            if(!c.moveToFirst()||!structure.equals(c.getString(0))||!semantic.equals(c.getString(1))||!domain.equals(c.getString(2)))throw new IllegalStateException("R932_SEMANTIC_IDENTITY_DRIFT");
        }
    }
    /** One comparison per existing maintenance slice. Only common frozen inputs and future pairs. */
    public static void maintain(final DbStore store){
        String currentIssue="";try(Cursor c=store.readable().rawQuery("SELECT issue FROM "+DbHelper.TABLE_DRAWS+" WHERE dirty=0 ORDER BY issue DESC LIMIT 1",null)){if(c.moveToFirst())currentIssue=c.getString(0);}
        if(currentIssue.isEmpty())return;
        String active=store.getState(ModelBundleManagerR929.STATE_ACTIVE,"");if(active.isEmpty())active=ModelBundleRuntimeR932.current().hash;
        try(ModelBundleRuntimeR932.Scope scope=ModelBundleRuntimeR932.forEvidence(active,TargetIssueResolver.next(currentIssue).targetIssue,System.currentTimeMillis())){
        if(!store.coordinatorR36().runAtomicWrite("R932_COHORT_REVIEW",new DatabaseCoordinator.WriteRunnable(){public void run(SQLiteDatabase db){
            schema(db);FixedResearchWindowR932.schema(db);ResearchPlatformStoreR929.createSchema(db);PairedResearchChallengeR932.schema(db);
            String latest="";try(Cursor c=db.rawQuery("SELECT issue FROM "+DbHelper.TABLE_DRAWS+" WHERE dirty=0 ORDER BY issue DESC LIMIT 1",null)){if(c.moveToFirst())latest=c.getString(0);}
            if(latest.isEmpty())return;
            try(Cursor c=db.rawQuery("SELECT a.candidate_key,b.candidate_key FROM "+IDENTITY+" a JOIN "+IDENTITY+" b ON a.candidate_key<b.candidate_key LEFT JOIN "+COMPARISON+" x ON x.left_key=a.candidate_key AND x.right_key=b.candidate_key WHERE (x.left_key IS NULL OR x.last_issue<?) AND EXISTS(SELECT 1 FROM "+CandidateExecutionAuditR932.TABLE+" e JOIN "+CandidateExecutionAuditR932.TABLE+" f ON e.target_issue=f.target_issue AND e.input_hash=f.input_hash WHERE e.candidate_key=a.candidate_key AND f.candidate_key=b.candidate_key LIMIT 1) ORDER BY COALESCE(x.checked_at,0),a.candidate_key,b.candidate_key LIMIT 1",new String[]{latest})){
                if(c.moveToFirst())review(db,c.getString(0),c.getString(1),latest);
            }
            // Scheduling champions are reversible projections, not statistical-family deletion.
            // A revoked/closed/paused incumbent must not block the family forever.
            db.execSQL("DELETE FROM "+CHAMPION+" WHERE NOT EXISTS(SELECT 1 FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE+" c WHERE c.certificate_hash="+CHAMPION+".certificate_hash AND c.state='ACTIVE' AND c.all_gates_pass=1 AND c.maximum_weight>0 AND c.structural_hash="+CHAMPION+".structural_hash AND (CASE c.consumer WHEN 'INVENTED_ALGORITHM' THEN 'algorithm:' WHEN 'GENESIS_ASSET' THEN 'genesis:' ELSE 'formula:' END)||c.candidate_id="+CHAMPION+".candidate_key AND NOT EXISTS(SELECT 1 FROM "+ResearchPlatformStoreR929.TABLE_REVOCATION+" rv WHERE rv.certificate_hash=c.certificate_hash)) OR EXISTS(SELECT 1 FROM "+ResearchLifecycleR932.TABLE+" life WHERE life.candidate_key="+CHAMPION+".candidate_key AND life.state IN ('QUALITY_PRUNED','RESOURCE_RETIRED','QUARANTINED','PAUSED'))");
            // First incumbent requires a published, independently supported certificate. A later
            // challenger cannot replace it by sorting training scores or cumulative hit rates.
            try(Cursor c=db.rawQuery("SELECT c.family_id,c.candidate_id,c.consumer,c.structural_hash,c.certificate_hash,c.evidence_hash FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE+" c WHERE c.state='ACTIVE' AND c.maximum_weight>0 AND c.all_gates_pass=1 AND NOT EXISTS(SELECT 1 FROM "+CHAMPION+" f WHERE f.family=c.family_id) ORDER BY c.issued_at,c.candidate_key LIMIT 4",null)){
                while(c.moveToNext()){
                    String prefix=c.getString(2).equals("INVENTED_ALGORITHM")?"algorithm":c.getString(2).equals("GENESIS_ASSET")?"genesis":"formula";
                    db.execSQL("INSERT OR IGNORE INTO "+CHAMPION+" VALUES(?,?,?,?,?,?)",new Object[]{c.getString(0),prefix+':'+c.getString(1),c.getString(3),c.getString(4),c.getString(5),TargetIssueResolver.next(latest).targetIssue});
                }
            }
            if(!ResearchGatePlanR967.installed().isCp3)try(Cursor c=db.rawQuery("SELECT f.candidate_key,f.structural_hash,c.candidate_id,c.consumer,c.structural_hash,c.evidence_hash FROM "+CHAMPION+" f JOIN "+ResearchPlatformStoreR929.TABLE_CERTIFICATE+" c ON c.family_id=f.family WHERE c.state='ACTIVE' AND c.maximum_weight>0 AND c.all_gates_pass=1 AND NOT EXISTS(SELECT 1 FROM "+ResearchPlatformStoreR929.TABLE_REVOCATION+" rv WHERE rv.certificate_hash=c.certificate_hash) AND ((CASE c.consumer WHEN 'INVENTED_ALGORITHM' THEN 'algorithm:' WHEN 'GENESIS_ASSET' THEN 'genesis:' ELSE 'formula:' END)||c.candidate_id)<>f.candidate_key AND c.structural_hash<>f.structural_hash AND NOT EXISTS(SELECT 1 FROM "+PairedResearchChallengeR932.TABLE+" p WHERE p.purpose='FAMILY_REPLACEMENT' AND p.parent_key=f.candidate_key AND p.child_key=((CASE c.consumer WHEN 'INVENTED_ALGORITHM' THEN 'algorithm:' WHEN 'GENESIS_ASSET' THEN 'genesis:' ELSE 'formula:' END)||c.candidate_id)) ORDER BY c.issued_at,c.candidate_key LIMIT 8",null)){
                while(c.moveToNext()){
                    String prefix=c.getString(3).equals("INVENTED_ALGORITHM")?"algorithm":c.getString(3).equals("GENESIS_ASSET")?"genesis":"formula",key=prefix+':'+c.getString(2);
                    PairedResearchChallengeR932.request(db,"FAMILY_REPLACEMENT",c.getString(0),key,c.getString(1),c.getString(4),c.getString(5));
                }
            }
        }}))throw new IllegalStateException("R932_COHORT_REVIEW_COMMIT");
        }
    }
    static void review(SQLiteDatabase db,String left,String right,String cutoff){
        if(!db.inTransaction())throw new IllegalStateException("R932_SIMILARITY_TRANSACTION");
        String ls="",rs="",ld="",rd="";
        try(Cursor c=db.rawQuery("SELECT candidate_key,structural_hash,domain_scope FROM "+IDENTITY+" WHERE candidate_key IN (?,?)",new String[]{left,right})){
            while(c.moveToNext())if(c.getString(0).equals(left)){ls=c.getString(1);ld=c.getString(2);}else{rs=c.getString(1);rd=c.getString(2);}
        }
        if(ls.isEmpty()||rs.isEmpty())throw new IllegalStateException("R932_SIMILARITY_IDENTITIES_MISSING");
        JSONArray probes=new JSONArray(),pairs=new JSONArray();int behavior=0;boolean ranksEqual=true;
        String source=SourceManifestR815.SOURCE_TREE_SHA256,schema=V51ActionMetaFeatureEngine.featureManifestHash(),atom=AtomCaptureR932.graphHash(db);
        try(Cursor c=db.rawQuery("SELECT a.target_issue,a.input_hash,a.ranking_hash,b.ranking_hash,a.proof_hash,b.proof_hash,a.payload,b.payload FROM "+CandidateExecutionAuditR932.TABLE+" a JOIN "+CandidateExecutionAuditR932.TABLE+" b ON a.target_issue=b.target_issue AND a.input_hash=b.input_hash WHERE a.candidate_key=? AND b.candidate_key=? AND a.structural_hash=? AND b.structural_hash=? AND a.code_hash=? AND b.code_hash=a.code_hash AND a.schema_hash=? AND b.schema_hash=a.schema_hash AND a.atom_graph_hash=? AND b.atom_graph_hash=a.atom_graph_hash AND a.target_issue<=? ORDER BY a.target_issue LIMIT "+MIN_BEHAVIOR_PROBES,new String[]{left,right,ls,rs,source,schema,atom,cutoff})){
            while(c.moveToNext()){
                try {if(!ModelBundleRuntimeR932.current().hash.equals(new JSONObject(c.getString(6)).optString("model_bundle_hash"))||!ModelBundleRuntimeR932.current().hash.equals(new JSONObject(c.getString(7)).optString("model_bundle_hash")))continue;}catch(JSONException bad){throw new IllegalStateException("R932_PROBE_PAYLOAD",bad);}
                behavior++;ranksEqual&=c.getString(2).equals(c.getString(3));JSONArray p=new JSONArray();for(int i=0;i<6;i++)p.put(c.getString(i));probes.put(p);}
        }
        int n=0;double sx=0,sy=0,sxx=0,syy=0,sxy=0;
        // Future windows exclude the 241 calibration rows and proxy nursery. Joins are by actual
        // common issue, never vector index or independently sampled history.
        try(Cursor c=db.rawQuery("SELECT p.target_issue,p.candidate_hit,p.selected_hit_p,q.candidate_hit,q.selected_hit_p,p.candidate_hash,q.candidate_hash FROM "+FixedResearchWindowR932.WINDOW+" w JOIN "+FixedResearchWindowR932.PAIR+" p ON p.window_id=w.window_id JOIN "+FixedResearchWindowR932.WINDOW+" v ON v.candidate_key=? JOIN "+FixedResearchWindowR932.PAIR+" q ON q.window_id=v.window_id AND q.target_issue=p.target_issue WHERE w.candidate_key=? AND w.window_no>=? AND v.window_no>=? AND w.structural_hash=? AND v.structural_hash=? AND w.champion_hash=v.champion_hash AND w.champion_hash=? AND w.selection_hash=v.selection_hash AND w.selection_hash=? AND EXISTS(SELECT 1 FROM "+FixedResearchWindowR932.CONTRACT+" wc JOIN "+FixedResearchWindowR932.CONTRACT+" vc ON vc.contract_hash=wc.contract_hash WHERE wc.window_id=w.window_id AND vc.window_id=v.window_id AND wc.contract_hash=?) AND p.candidate_hit IN (0,1) AND q.candidate_hit IN (0,1) AND p.selected_hit_p>=0 AND q.selected_hit_p>=0 AND p.target_issue<=? ORDER BY p.target_issue LIMIT "+MIN_COMMON_FUTURE,new String[]{right,left,String.valueOf(ObservationGenerationR932.firstSerial(db,left)),String.valueOf(ObservationGenerationR932.firstSerial(db,right)),ls,rs,ModelBundleRuntimeR932.current().hash,SelectionEvidenceContractR929.installedPolicy().hash,FixedResearchWindowR932.contract(db),cutoff})){
            while(c.moveToNext()){ResearchWorkBudgetR927.checkpoint();double x=c.getInt(1)-c.getDouble(2),y=c.getInt(3)-c.getDouble(4);n++;sx+=x;sy+=y;sxx+=x*x;syy+=y*y;sxy+=x*y;JSONArray p=new JSONArray();for(int i=0;i<7;i++)p.put(c.getString(i));pairs.put(p);}
        }
        double vx=n*sxx-sx*sx,vy=n*syy-sy*sy;
        Double correlation=vx>1e-12&&vy>1e-12?(n*sxy-sx*sy)/Math.sqrt(vx*vy):null;
        String type=!ld.equals(rd)?"DOMAIN_COMPLEMENT_RETAINED":behavior==MIN_BEHAVIOR_PROBES&&ranksEqual?"BEHAVIOR_DUPLICATE":n==MIN_COMMON_FUTURE&&correlation!=null&&correlation>RESIDUAL_THRESHOLD?"RESIDUAL_DUPLICATE":n<MIN_COMMON_FUTURE?"WAIT_COMMON_FUTURE":"DISTINCT";
        // Negative correlation is useful complementarity, not a duplicate.
        try{
            JSONObject evidence=new JSONObject();evidence.put("revision","R932_FIXED_COMMON_PROBES_AND_FUTURE_V1");evidence.put("left",left);evidence.put("right",right);evidence.put("left_structure",ls);evidence.put("right_structure",rs);evidence.put("left_domain",ld);evidence.put("right_domain",rd);evidence.put("model_bundle_hash",ModelBundleRuntimeR932.current().hash);evidence.put("selection_hash",CertificateDependenciesR932.selectionHash());evidence.put("code_hash",source);evidence.put("schema_hash",schema);evidence.put("atom_graph_hash",atom);evidence.put("fixed_first_common_probes",probes);evidence.put("fixed_first_common_future_pairs",pairs);evidence.put("correlation",correlation==null?JSONObject.NULL:correlation);evidence.put("classification",type);evidence.put("statistical_family_unchanged",true);
            String json=evidence.toString();db.execSQL("INSERT OR REPLACE INTO "+COMPARISON+" VALUES(?,?,?,?,?,?,?,?,?,?,?)",new Object[]{left,right,ls,rs,n,correlation,type,json,V51ChecksumEngine.sha256(json),cutoff,System.currentTimeMillis()});
        }catch(JSONException e){throw new IllegalStateException("R932_SIMILARITY_JSON",e);}
    }
    static Map<String,String> clusters(SQLiteDatabase db,List<GlobalResearchScheduleR932.Candidate> candidates){
        schema(db);TreeMap<String,String> parent=new TreeMap<String,String>();
        HashMap<String,String> structures=new HashMap<String,String>();
        for(GlobalResearchScheduleR932.Candidate c:candidates){parent.put(c.key,c.key);structures.put(c.key,c.structure);}
        HashMap<String,String> semantics=new HashMap<String,String>();
        try(Cursor c=db.rawQuery("SELECT candidate_key,semantic_hash,domain_scope FROM "+IDENTITY+" ORDER BY candidate_key",null)){
            while(c.moveToNext())if(parent.containsKey(c.getString(0))){String semantic=c.getString(1)+'|'+c.getString(2);String old=semantics.putIfAbsent(semantic,c.getString(0));if(old!=null)union(parent,old,c.getString(0));}
        }
        String graph=AtomCaptureR932.graphHash(db);
        HashSet<String> provenPairs=new HashSet<String>();
        ArrayList<String[]> edges=new ArrayList<String[]>();
        try(Cursor c=db.rawQuery("SELECT left_key,right_key,evidence_json,evidence_hash FROM "+COMPARISON+" WHERE classification IN ('BEHAVIOR_DUPLICATE','RESIDUAL_DUPLICATE') ORDER BY left_key,right_key",null)){
            while(c.moveToNext()){
                if(!V51ChecksumEngine.sha256(c.getString(2)).equals(c.getString(3)))throw new IllegalStateException("R932_SIMILARITY_HASH");
                try{JSONObject e=new JSONObject(c.getString(2));if(!SourceManifestR815.SOURCE_TREE_SHA256.equals(e.getString("code_hash"))||!graph.equals(e.getString("atom_graph_hash"))||!ModelBundleRuntimeR932.current().hash.equals(e.optString("model_bundle_hash"))||!V51ActionMetaFeatureEngine.featureManifestHash().equals(e.optString("schema_hash"))||!CertificateDependenciesR932.selectionHash().equals(e.optString("selection_hash"))||!e.optString("left_structure").equals(structures.get(c.getString(0)))||!e.optString("right_structure").equals(structures.get(c.getString(1))))continue;}catch(JSONException e){throw new IllegalStateException(e);}
                if(parent.containsKey(c.getString(0))&&parent.containsKey(c.getString(1))){
                    provenPairs.add(pairKey(c.getString(0),c.getString(1)));
                    edges.add(new String[]{c.getString(0),c.getString(1)});
                }
            }
        }
        // Correlation/probe similarity is not transitive. Merge only if every cross-cluster pair
        // already carries current, frozen evidence. Exact semantic aliases remain separate evidence.
        for(String[] edge:edges)if(completeLink(parent,edge[0],edge[1],provenPairs))union(parent,edge[0],edge[1]);
        for(String key:new ArrayList<String>(parent.keySet()))parent.put(key,root(parent,key));return parent;
    }
    private static String pairKey(String a,String b){if(a.compareTo(b)>0){String t=a;a=b;b=t;}return a.length()+":"+a+b;}
    private static boolean completeLink(Map<String,String> p,String a,String b,Set<String> pairs){
        String ra=root(p,a),rb=root(p,b);if(ra.equals(rb))return true;
        ArrayList<String> left=new ArrayList<String>(),right=new ArrayList<String>();
        for(String key:p.keySet()){String r=root(p,key);if(r.equals(ra))left.add(key);else if(r.equals(rb))right.add(key);}
        for(String x:left)for(String y:right){ResearchWorkBudgetR927.checkpoint();if(!pairs.contains(pairKey(x,y)))return false;}
        return true;
    }
    private static String root(Map<String,String> p,String key){String value=key;for(int i=0;i<=p.size();i++){String next=p.get(value);if(value.equals(next))return value;value=next;}throw new IllegalStateException("R932_CLUSTER_CYCLE");}
    private static void union(Map<String,String> p,String a,String b){a=root(p,a);b=root(p,b);if(!a.equals(b))p.put(a.compareTo(b)>0?a:b,a.compareTo(b)>0?b:a);}
    static String champion(SQLiteDatabase db,String family){try(Cursor c=db.rawQuery("SELECT candidate_key FROM "+CHAMPION+" WHERE family=?",new String[]{family})){return c.moveToFirst()?c.getString(0):"";}}
}
