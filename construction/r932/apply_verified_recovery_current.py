#!/usr/bin/env python3
"""Integrate the already-verified paid observation generation onto the current stronger closeout tree.

This is a one-shot construction script executed in CI before compile/regression.  It preserves the
current shared-authorizer lifecycle and champion-CAS work while importing only the additive recovery,
current-generation accounting, and complete-link scheduling semantics verified on the sibling branch.
"""
from pathlib import Path
import hashlib, json, difflib

R=Path(__file__).resolve().parents[2]
changed={}

def load(path):
    p=R/path
    b=p.read_bytes(); changed.setdefault(path,b); return b.decode('utf-8')

def put(path,text):
    (R/path).write_text(text,encoding='utf-8')

def one(text,old,new,label):
    count=text.count(old)
    if count!=1: raise RuntimeError('R932_RECOVERY_ANCHOR_'+label+':'+str(count))
    return text.replace(old,new,1)

def require(path,needle,label):
    if needle not in (R/path).read_text(encoding='utf-8'):
        raise RuntimeError('R932_RECOVERY_REQUIRE_'+label)

# The sibling branch contributes these exact, previously-verified files.  Workflow imports them
# before invoking this script; refuse a partial import.
for p in [
    'app/src/main/java/com/ethk3/novaonepro/ObservationGenerationR932.java',
    'verification/r932/ObservationRecoveryHarnessR932.java',
    'verification/r932/LifecycleIntegrityHarnessR932.java']:
    if not (R/p).is_file(): raise RuntimeError('R932_RECOVERY_IMPORTED_FILE_MISSING:'+p)

# Preserve the stronger current lifecycle authority implementation.
require('app/src/main/java/com/ethk3/novaonepro/ResearchLifecycleR932.java',
        'ResearchPlatformStoreR929.authorizeCurrent(store,CertificateBundleServiceR929.Consumer.valueOf(consumer)',
        'SHARED_AUTHORIZER')
require('app/src/main/java/com/ethk3/novaonepro/PairedResearchChallengeR932.java',
        'R932_REPLACEMENT_INCUMBENT_CAS_FAILED','CHAMPION_CAS')

# ---------- FixedResearchWindow: additive generations and paid recovery ----------
p='app/src/main/java/com/ethk3/novaonepro/FixedResearchWindowR932.java'; s=load(p)
s=one(s,'    public static void schema(SQLiteDatabase db) {',
      '    public static void schema(SQLiteDatabase db) {\n        ObservationGenerationR932.schema(db);','WINDOW_SCHEMA')
a=s.index('        String window=null;int number=1;')
b=s.index('        CandidateCalibrationR932.Probability calibrated=null;',a)
s=s[:a]+'''        String window=null;int number=1;boolean recovery=false;
        try(Cursor c=db.rawQuery("SELECT window_id,structural_hash,champion_hash,selection_hash,first_issue,last_issue,state,window_no FROM "+WINDOW+" WHERE candidate_key=? ORDER BY window_no DESC LIMIT 1",new String[]{key})) {
            if(c.moveToFirst()) {
                String state=c.getString(6);number=c.getInt(7);window=c.getString(0);
                if(!"OPEN".equals(state)&&!"PASS".equals(state)&&!ObservationGenerationR932.isSystemPause(state))return;
                String frozen="";try(Cursor dependency=db.rawQuery("SELECT contract_hash FROM "+CONTRACT+" WHERE window_id=?",new String[]{window})){if(dependency.moveToFirst())frozen=dependency.getString(0);}
                boolean changed=!frozen.equals(contract(db))||!structural.equals(c.getString(1))||!bundle.equals(c.getString(2))||!selector.equals(c.getString(3));
                if("OPEN".equals(state)&&changed){
                    db.execSQL("UPDATE "+WINDOW+" SET state='PAUSED',reason='ATOM_SCHEMA_SOURCE_DEPENDENCY_CHANGED' WHERE window_id=? AND state='OPEN'",new Object[]{window});return;
                }
                if(ObservationGenerationR932.isSystemPause(state)||"PASS".equals(state)&&changed){
                    if(target.compareTo(c.getString(5))<=0)return;
                    // Never reset old denominators. Recovery starts a separately-paid observation generation.
                    recovery=true;number++;window=null;
                }else if("PASS".equals(state)){
                    if(ObservationGenerationR932.of(db,key,window,number).stage>=5||target.compareTo(c.getString(5))<=0)return;
                    number++;window=null;
                }else if(target.compareTo(c.getString(4))<0||target.compareTo(c.getString(5))>0)return;
            }
        }
''' + s[b:]
s=one(s,'        if(window==null)window=register(db,key,structural,bundle,selector,target,new int[]{120,240,240,240,360}[number-1],number);','''        if(window==null){
            if(recovery){
                if(calibrated==null)return;
                window=ObservationGenerationR932.recover(db,key,structural,bundle,selector,target,calibrated.hash);
                if(window==null)return;
            }else{
                ObservationGenerationR932.Chain next=ObservationGenerationR932.next(db,key,number,target);
                window=register(db,key,structural,bundle,selector,target,new int[]{120,240,240,240,360}[next.stage-1],number);
            }
        }''','WINDOW_RECOVERY')
a=s.index('    static String register(SQLiteDatabase db,')
b=s.index('    public static void maintain(final DbStore store)',a)
s=s[:a]+'''    static String register(SQLiteDatabase db,String key,String structural,String bundle,String selector,String first,int planned,int number) {
        ObservationGenerationR932.Chain chain=ObservationGenerationR932.next(db,key,number,first);
        return persistWindow(db,key,structural,bundle,selector,first,planned,number,chain);
    }
    static String persistWindow(SQLiteDatabase db,String key,String structural,String bundle,String selector,String first,int planned,int number,ObservationGenerationR932.Chain chain){
        if(!db.inTransaction())throw new IllegalStateException("R932_WINDOW_REGISTRATION_TRANSACTION");
        int[] sizes={120,240,240,240,360};
        if(chain.stage<1||chain.stage>5||planned!=sizes[chain.stage-1]||number!=chain.start+chain.stage-1||!TargetIssueResolver.isValid(first))throw new IllegalArgumentException("R932_FIXED_WINDOW_SIZE");
        if(drawExists(db,first))throw new IllegalStateException("R932_WINDOW_REGISTRATION_POSTDRAW");
        String last=first;for(int i=1;i<planned;i++)last=TargetIssueResolver.next(last).targetIssue;
        String id=V51ChecksumEngine.sha256("R932_WINDOW|"+key+'|'+structural+'|'+bundle+'|'+selector+'|'+first+'|'+last+'|'+planned+'|'+number);
        db.execSQL("INSERT OR IGNORE INTO "+WINDOW+"(window_id,candidate_key,structural_hash,champion_hash,selection_hash,first_issue,last_issue,planned_n,window_no,registered_at,state,reason) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{id,key,structural,bundle,selector,first,last,planned,number,System.currentTimeMillis(),"OPEN","FIXED_ENDPOINT_NO_PEEK"});
        try(Cursor c=db.rawQuery("SELECT window_id FROM "+WINDOW+" WHERE candidate_key=? AND window_no=?",new String[]{key,String.valueOf(number)})){if(!c.moveToFirst()||!id.equals(c.getString(0)))throw new IllegalStateException("R932_WINDOW_REGISTRATION_CONFLICT");}
        db.execSQL("INSERT OR IGNORE INTO "+CONTRACT+" VALUES(?,?)",new Object[]{id,contract(db)});
        ObservationGenerationR932.bind(db,id,key,chain);
        ResearchMultiplicityR932.registerLook(db,chain.claim,id,chain.stage,first,last);
        return id;
    }
''' + s[b:]
put(p,s)

# ---------- certificate owner: transaction-local revocation for atomic recovery ----------
p='app/src/main/java/com/ethk3/novaonepro/ResearchPlatformStoreR929.java'; s=load(p)
s=one(s,'''            long at=System.currentTimeMillis();db.execSQL("INSERT OR IGNORE INTO "+TABLE_REVOCATION+" VALUES(?,?,?)",new Object[]{certificateHash,reason,at});
            db.execSQL("UPDATE "+TABLE_CERTIFICATE+" SET state='REVOKED',reason=?,updated_at=? WHERE certificate_hash=?",new Object[]{reason,at,certificateHash});''','''            revokeInTransaction(db,certificateHash,reason,System.currentTimeMillis());''','REVOKE_OWNER')
s=one(s,'    /** Existing maintenance lane appends revocations; production authorization itself stays read-only. */','''    /** Reuse the one revocation owner when observation recovery and revocation must commit together. */
    static void revokeInTransaction(SQLiteDatabase db,String certificateHash,String reason,long at){
        if(!db.inTransaction())throw new IllegalStateException("R932_CERTIFICATE_REVOCATION_TRANSACTION");
        requireHash(certificateHash,"REVOKE_HASH");required(reason,"REVOKE_REASON");
        db.execSQL("INSERT OR IGNORE INTO "+TABLE_REVOCATION+" VALUES(?,?,?)",new Object[]{certificateHash,reason,at});
        db.execSQL("UPDATE "+TABLE_CERTIFICATE+" SET state='REVOKED',reason=?,updated_at=? WHERE certificate_hash=?",new Object[]{reason,at,certificateHash});
    }

    /** Existing maintenance lane appends revocations; production authorization itself stays read-only. */''','REVOKE_HELPER')
put(p,s)

# ---------- lifecycle: count only current paid generation and fully valid pair rows ----------
p='app/src/main/java/com/ethk3/novaonepro/ResearchLifecycleR932.java'; s=load(p)
old='''        try(Cursor c=db.rawQuery("SELECT p.target_issue,p.candidate_hit,p.champion_hit,p.selected_hit_p,p.candidate_hash FROM "+FixedResearchWindowR932.WINDOW+" w JOIN "+FixedResearchWindowR932.PAIR+" p ON p.window_id=w.window_id WHERE w.candidate_key=? AND w.structural_hash=? AND w.champion_hash=? AND w.selection_hash=? AND EXISTS(SELECT 1 FROM "+FixedResearchWindowR932.CONTRACT+" wc WHERE wc.window_id=w.window_id AND wc.contract_hash=?) AND p.target_issue<=? AND p.candidate_hit IN (0,1) AND p.selected_hit_p>=0 ORDER BY p.target_issue DESC LIMIT 1200",new String[]{key,structure,ModelBundleRuntimeR932.current().hash,SelectionEvidenceContractR929.installedPolicy().hash,FixedResearchWindowR932.contract(db),latest})){'''
new='''        try(Cursor c=db.rawQuery("SELECT p.target_issue,p.candidate_hit,p.champion_hit,p.selected_hit_p,p.candidate_hash FROM "+FixedResearchWindowR932.WINDOW+" w JOIN "+FixedResearchWindowR932.PAIR+" p ON p.window_id=w.window_id WHERE w.candidate_key=? AND w.window_no>=? AND w.structural_hash=? AND w.champion_hash=? AND w.selection_hash=? AND EXISTS(SELECT 1 FROM "+FixedResearchWindowR932.CONTRACT+" wc WHERE wc.window_id=w.window_id AND wc.contract_hash=?) AND p.candidate_hit IN (0,1) AND p.champion_hit IN (0,1) AND p.selected_hit_p>=0 AND p.selected_hit_p<=1 AND p.target_issue<=? AND p.target_issue>=w.first_issue AND p.target_issue<=w.last_issue ORDER BY p.target_issue DESC LIMIT 1200",new String[]{key,String.valueOf(ObservationGenerationR932.firstSerial(db,key)),structure,ModelBundleRuntimeR932.current().hash,SelectionEvidenceContractR929.installedPolicy().hash,FixedResearchWindowR932.contract(db),latest})){'''
s=one(s,old,new,'LIFECYCLE_GENERATION')
put(p,s)

# ---------- cohort residual support: current generation only; cluster by complete-link ----------
p='app/src/main/java/com/ethk3/novaonepro/ResearchCohortR932.java'; s=load(p)
s=one(s,'WHERE w.candidate_key=? AND w.structural_hash=? AND v.structural_hash=? AND w.champion_hash',
      'WHERE w.candidate_key=? AND w.window_no>=? AND v.window_no>=? AND w.structural_hash=? AND v.structural_hash=? AND w.champion_hash','COHORT_GENERATION_WHERE')
s=one(s,'new String[]{right,left,ls,rs,ModelBundleRuntimeR932.current().hash',
      'new String[]{right,left,String.valueOf(ObservationGenerationR932.firstSerial(db,left)),String.valueOf(ObservationGenerationR932.firstSerial(db,right)),ls,rs,ModelBundleRuntimeR932.current().hash','COHORT_GENERATION_ARGS')
s=one(s,'evidence.put("model_bundle_hash",ModelBundleRuntimeR932.current().hash);',
      'evidence.put("model_bundle_hash",ModelBundleRuntimeR932.current().hash);evidence.put("selection_hash",CertificateDependenciesR932.selectionHash());','COHORT_SELECTION_EVIDENCE')
s=one(s,'schema(db);TreeMap<String,String> parent=new TreeMap<String,String>();for(GlobalResearchScheduleR932.Candidate c:candidates)parent.put(c.key,c.key);','''schema(db);TreeMap<String,String> parent=new TreeMap<String,String>();
        HashMap<String,String> structures=new HashMap<String,String>();
        for(GlobalResearchScheduleR932.Candidate c:candidates){parent.put(c.key,c.key);structures.put(c.key,c.structure);}''','COHORT_STRUCTURES')
s=one(s,'String graph=AtomCaptureR932.graphHash(db);','''String graph=AtomCaptureR932.graphHash(db);
        HashSet<String> provenPairs=new HashSet<String>();
        ArrayList<String[]> edges=new ArrayList<String[]>();''','COHORT_EDGE_SET')
s=one(s,"WHERE classification IN ('BEHAVIOR_DUPLICATE','RESIDUAL_DUPLICATE')\",null)",
      "WHERE classification IN ('BEHAVIOR_DUPLICATE','RESIDUAL_DUPLICATE') ORDER BY left_key,right_key\",null)",'COHORT_EDGE_ORDER')
s=one(s,'if(!SourceManifestR815.SOURCE_TREE_SHA256.equals(e.getString("code_hash"))||!graph.equals(e.getString("atom_graph_hash"))||!ModelBundleRuntimeR932.current().hash.equals(e.optString("model_bundle_hash"))||!V51ActionMetaFeatureEngine.featureManifestHash().equals(e.optString("schema_hash")))continue;',
      'if(!SourceManifestR815.SOURCE_TREE_SHA256.equals(e.getString("code_hash"))||!graph.equals(e.getString("atom_graph_hash"))||!ModelBundleRuntimeR932.current().hash.equals(e.optString("model_bundle_hash"))||!V51ActionMetaFeatureEngine.featureManifestHash().equals(e.optString("schema_hash"))||!CertificateDependenciesR932.selectionHash().equals(e.optString("selection_hash"))||!e.optString("left_structure").equals(structures.get(c.getString(0)))||!e.optString("right_structure").equals(structures.get(c.getString(1))))continue;','COHORT_CURRENT_EVIDENCE')
s=one(s,'if(parent.containsKey(c.getString(0))&&parent.containsKey(c.getString(1)))union(parent,c.getString(0),c.getString(1));','''if(parent.containsKey(c.getString(0))&&parent.containsKey(c.getString(1))){
                    provenPairs.add(pairKey(c.getString(0),c.getString(1)));
                    edges.add(new String[]{c.getString(0),c.getString(1)});
                }''','COHORT_COLLECT_EDGES')
s=one(s,'        for(String key:new ArrayList<String>(parent.keySet()))parent.put(key,root(parent,key));return parent;','''        // Correlation/probe similarity is not transitive. Merge only if every cross-cluster pair
        // already carries current, frozen evidence. Exact semantic aliases remain separate evidence.
        for(String[] edge:edges)if(completeLink(parent,edge[0],edge[1],provenPairs))union(parent,edge[0],edge[1]);
        for(String key:new ArrayList<String>(parent.keySet()))parent.put(key,root(parent,key));return parent;''','COHORT_COMPLETE_LINK')
s=one(s,'    private static String root(Map<String,String> p,String key){','''    private static String pairKey(String a,String b){if(a.compareTo(b)>0){String t=a;a=b;b=t;}return a.length()+":"+a+b;}
    private static boolean completeLink(Map<String,String> p,String a,String b,Set<String> pairs){
        String ra=root(p,a),rb=root(p,b);if(ra.equals(rb))return true;
        ArrayList<String> left=new ArrayList<String>(),right=new ArrayList<String>();
        for(String key:p.keySet()){String r=root(p,key);if(r.equals(ra))left.add(key);else if(r.equals(rb))right.add(key);}
        for(String x:left)for(String y:right){ResearchWorkBudgetR927.checkpoint();if(!pairs.contains(pairKey(x,y)))return false;}
        return true;
    }
    private static String root(Map<String,String> p,String key){''','COHORT_COMPLETE_LINK_HELPER')
put(p,s)

# ---------- publisher: certificate evidence cannot mix old and recovered generations ----------
p='app/src/main/java/com/ethk3/novaonepro/FixedWindowCertificatePublisherR932.java'; s=load(p)
s=one(s,'        String previousLast="";','''        String previousLast="";
        ObservationGenerationR932.Chain generation=ObservationGenerationR932.of(db,key,id,number);''','PUBLISH_GENERATION')
s=one(s,'WHERE w.candidate_key=? AND w.window_no<=? ORDER BY w.window_no LIMIT 5",new String[]{key,String.valueOf(number)})',
      'WHERE w.candidate_key=? AND w.window_no<=? AND w.window_no>=? ORDER BY w.window_no LIMIT 5",new String[]{key,String.valueOf(number),String.valueOf(generation.start)})','PUBLISH_RANGE')
s=one(s,'if(c.getInt(1)!=windows+1||','if(c.getInt(1)!=generation.start+windows||','PUBLISH_SERIAL')
s=one(s,'        if(windows!=number){block(db,id,"INDEPENDENT_WINDOW_CHAIN_INCOMPLETE");return;}',
      '        if(windows!=generation.stage){block(db,id,"INDEPENDENT_WINDOW_CHAIN_INCOMPLETE");return;}','PUBLISH_STAGE')
s=one(s,"                sources.append('|').append(c.getString(0))",'''                ObservationGenerationR932.Chain member=ObservationGenerationR932.of(db,key,c.getString(0),c.getInt(1));
                if(!generation.generation.equals(member.generation)||!generation.claim.equals(member.claim)||member.stage!=windows+1){block(db,id,"OBSERVATION_GENERATION_MIXED");return;}
                sources.append('|').append(c.getString(0))''','PUBLISH_CHAIN')
put(p,s)

# ---------- host harness: run imported production recovery and lifecycle integrity tests ----------
p='scripts/r932_model_bundle_harness.py'; s=load(p)
s=one(s,"production=['AdaptiveShadowLearnerEngine'","production=['ObservationGenerationR932','AdaptiveShadowLearnerEngine'",'HARNESS_PRODUCTION')
s=one(s,"R/'verification/r932/DirectedCreationHarnessR932.java'","R/'verification/r932/ObservationRecoveryHarnessR932.java',R/'verification/r932/LifecycleIntegrityHarnessR932.java',R/'verification/r932/DirectedCreationHarnessR932.java'",'HARNESS_FILES')
s=one(s,"for name,marker,report in [('MillionAtomIndexHarnessR932'","for name,marker,report in [('ObservationRecoveryHarnessR932','R932_OBSERVATION_RECOVERY_PASS','OBSERVATION_RECOVERY.txt'),('LifecycleIntegrityHarnessR932','R932_LIFECYCLE_INTEGRITY_PASS','LIFECYCLE_INTEGRITY.txt'),('MillionAtomIndexHarnessR932'",'HARNESS_RUNS')
put(p,s)

# deterministic ledger
out=R/'reports/r932/recovery_integration'; out.mkdir(parents=True,exist_ok=True)
rows=[]
for path,before in changed.items():
    after=(R/path).read_bytes(); a=before.decode('utf-8'); b=after.decode('utf-8')
    rows.append({'path':path,'before_sha256':hashlib.sha256(before).hexdigest(),'after_sha256':hashlib.sha256(after).hexdigest(),
                 'before_lines':len(a.splitlines()),'after_lines':len(b.splitlines()),'before_chars':len(a),'after_chars':len(b),
                 'changed':before!=after})
(out/'CHANGE_LEDGER.json').write_text(json.dumps({'scope':'VERIFIED_RECOVERY_INTEGRATION_CURRENT_BRANCH','files':rows},ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('R932_VERIFIED_RECOVERY_INTEGRATION_APPLIED files='+str(len(rows)))

