package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.json.*;
import java.util.*;

/** Immutable roster commitment plus resumable registration pages.
 * R948 admits a committed page only against the original complete roster hash. The final PLAN
 * still means all registration pages finished; unfinished pages are not silently counted as work.
 */
public final class StagedResearchRosterR943 {
    public static final String TABLE="research_roster_prepare_r943";
    public static final String MANIFEST="research_roster_manifest_r948";
    private static final int PAGE=32;
    private StagedResearchRosterR943(){}
    static void schema(SQLiteDatabase db){
        db.execSQL("CREATE TABLE IF NOT EXISTS "+TABLE+"(target_issue TEXT PRIMARY KEY,input_hash TEXT NOT NULL,payload TEXT NOT NULL,payload_hash TEXT NOT NULL,cursor INTEGER NOT NULL,created_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+MANIFEST+"(target_issue TEXT PRIMARY KEY,input_hash TEXT NOT NULL,payload_hash TEXT NOT NULL,candidate_count INTEGER NOT NULL,created_at INTEGER NOT NULL)");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS r948_roster_commitment_immutable BEFORE UPDATE ON "+MANIFEST+" BEGIN SELECT RAISE(ABORT,'R948_ROSTER_COMMITMENT_IMMUTABLE'); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS r948_roster_payload_immutable BEFORE UPDATE OF target_issue,input_hash,payload,payload_hash ON "+TABLE+" BEGIN SELECT RAISE(ABORT,'R948_ROSTER_PAYLOAD_IMMUTABLE'); END");
    }
    /** One registration quantum. Consumers may run earlier committed members, never an unbound row. */
    public static boolean prepareSlice(DbStore store,String target,String input) {
        if(!TargetIssueResolver.isValid(target)||input==null||!input.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("R948_ROSTER_INPUT");
        try(Cursor table=store.readable().rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",new String[]{GlobalResearchScheduleR932.PLAN})) {
            if(table.moveToFirst())try(Cursor plan=store.readable().rawQuery("SELECT input_hash FROM "+GlobalResearchScheduleR932.PLAN+" WHERE target_issue=?",new String[]{target})) {
                if(plan.moveToFirst()){if(!input.equals(plan.getString(0)))throw new IllegalStateException("R948_ROSTER_INPUT_DRIFT");return true;}
            }
        }
        return preparePage(store,target,input);
    }
    public static void prepare(final DbStore store,final String target,final String input){
        if(!TargetIssueResolver.isValid(target)||input==null||!input.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("R943_ROSTER_INPUT");
        // The immutable published plan is the fast path for every lane and retry.
        // Merely reading it must not rerun the complete schema dependency graph.
        boolean planExists;
        try(Cursor c=store.readable().rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",new String[]{GlobalResearchScheduleR932.PLAN})){planExists=c.moveToFirst();}
        if(planExists){
            try(Cursor c=store.readable().rawQuery("SELECT input_hash FROM "+GlobalResearchScheduleR932.PLAN+" WHERE target_issue=?",new String[]{target})){
                if(c.moveToFirst()){if(!input.equals(c.getString(0)))throw new IllegalStateException("R943_ROSTER_INPUT_DRIFT");return;}
            }
        }
        // A committed page is progress, not a reason to abandon the remaining legal slice.
        // Each page releases the writer. The enclosing budget and actual draw are rechecked.
        final PageSession session=new PageSession();
        boolean initialize=true;
        while(true){
            ResearchWorkBudgetR927.checkpoint();
            if(preparePage(store,target,input,session,initialize))return;
            initialize=false;
            if(ResearchWorkBudgetR927.remainingMs()<20L)
                throw new ResearchWorkBudgetR927.Yield("ROSTER_PAGE_COMMITTED_CONTINUE");
        }
    }
    private static final class PageSession { String payload,hash; JSONArray rows; }
    static boolean preparePage(final DbStore store,final String target,final String input){
        return preparePage(store,target,input,new PageSession(),true);
    }
    private static boolean preparePage(final DbStore store,final String target,final String input,final PageSession session,final boolean initialize){
        final Exception[] failure = new Exception[1];
        Boolean ready=store.coordinatorR36().runAtomicWrite("R943_RESEARCH_ROSTER_PAGE",new DatabaseCoordinator.WriteCallable<Boolean>(){
            public Boolean call(SQLiteDatabase db)throws Exception{
                try {
                // Startup owns verified research schema. A registration page does not run DDL.
                try(Cursor c=db.rawQuery("SELECT input_hash FROM "+GlobalResearchScheduleR932.PLAN+" WHERE target_issue=?",new String[]{target})){
                    if(c.moveToFirst()){if(!input.equals(c.getString(0)))throw new IllegalStateException("R943_ROSTER_INPUT_DRIFT");return true;}
                }
                if(drawn(db,target))throw new IllegalStateException("R943_ROSTER_POSTDRAW");
                String payload=null,hash=null;int cursor=0;
                try(Cursor c=db.rawQuery("SELECT input_hash,"+(session.rows==null?"payload":"NULL")+",payload_hash,cursor FROM "+TABLE+" WHERE target_issue=?",new String[]{target})){
                    if(c.moveToFirst()){if(!input.equals(c.getString(0)))throw new IllegalStateException("R943_ROSTER_INPUT_DRIFT");payload=c.getString(1);hash=c.getString(2);cursor=c.getInt(3);if(session.rows!=null){if(!hash.equals(session.hash))throw new IllegalStateException("R945_ROSTER_PAGE_CHANGED");payload=session.payload;}}
                }
                if(payload==null){
                    payload=freezeAdmission(db,target,collect(db,target)).toString();hash=V51ChecksumEngine.sha256(payload);
                    db.execSQL("INSERT INTO "+TABLE+" VALUES(?,?,?,?,0,?)",new Object[]{target,input,payload,hash,System.currentTimeMillis()});
                }
                if(session.rows==null){
                    if(!hash.equals(V51ChecksumEngine.sha256(payload)))throw new IllegalStateException("R943_ROSTER_PAYLOAD_HASH");
                    session.rows=new JSONArray(payload);session.payload=payload;session.hash=hash;
                }
                JSONArray rows=session.rows;
                // Bind the complete candidate inventory before publishing any executable page.
                // This is not a bypass of PLAN: membership, structure and the input commitment
                // stay immutable, and each page advances its durable cursor with its task rows.
                db.execSQL("INSERT OR IGNORE INTO "+MANIFEST+" VALUES(?,?,?,?,?)",new Object[]{target,input,hash,rows.length(),System.currentTimeMillis()});
                try(Cursor commitment=db.rawQuery("SELECT input_hash,payload_hash,candidate_count FROM "+MANIFEST+" WHERE target_issue=?",new String[]{target})) {
                    if(!commitment.moveToFirst()||!input.equals(commitment.getString(0))||!hash.equals(commitment.getString(1))||rows.length()!=commitment.getInt(2))throw new IllegalStateException("R948_ROSTER_COMMITMENT_DRIFT");
                }
                int end=Math.min(rows.length(),cursor+PAGE);
                List<Object> taskValues=new ArrayList<Object>();StringBuilder taskSlots=new StringBuilder();
                Map<String,Registered> registered=registered(db,rows,cursor,end);
                for(int i=cursor;i<end;i++){
                    // Release a completed prefix instead of rolling back the same page forever
                    // on a slower phone. A single identity registration is indivisible.
                    try { ResearchWorkBudgetR927.checkpoint(); }
                    catch (ResearchWorkBudgetR927.Yield yielded) { if(i==cursor)throw yielded;end=i;break; }
                    JSONObject row=rows.getJSONObject(i);
                    String key=row.getString("key"),family=row.getString("family"),structure=row.getString("structure"),tier=row.getString("tier");
                    List<GlobalResearchScheduleR932.Candidate> prepared=new ArrayList<GlobalResearchScheduleR932.Candidate>();
                    Registered prior=registered.get(key);
                    if(prior!=null){
                        // Validate every immutable identity, while reusing its already committed
                        // statistical registration. A new issue must not pay to enroll it again.
                        if(!structure.equals(prior.structure)||!structure.equals(prior.lifeStructure)
                                ||!structure.equals(prior.hypothesisStructure)||!structure.equals(prior.cohortStructure)
                                ||!prior.family.equals(prior.hypothesisFamily)||!"GLOBAL_ALL_POSITIONS".equals(prior.domain)
                                ||key.startsWith("algorithm:")&&!row.getString("semantic").equals(prior.semantic))
                            throw new IllegalStateException("R944_REGISTERED_CANDIDATE_DRIFT");
                        tier=prior.tier();
                        if(tier.isEmpty()){
                            if(!PairedResearchChallengeR932.requiresReference(db,key,target))continue;
                            tier="B";
                        }
                        if(tier.equals("A")&&!prior.champion.isEmpty()&&!prior.champion.equals(key))tier="B";
                        prepared.add(new GlobalResearchScheduleR932.Candidate(key,prior.family,structure,tier,""));
                    }else{
                        if(key.startsWith("algorithm:"))ResearchCohortR932.register(db,key,structure,row.getString("semantic"),"GLOBAL_ALL_POSITIONS",target);
                        GlobalResearchScheduleR932.add(db,prepared,target,key,family,structure,tier);
                    }
                    if(prepared.isEmpty())continue;
                    GlobalResearchScheduleR932.Candidate c=prepared.get(0);
                    if(prior==null){
                    try(Cursor old=db.rawQuery("SELECT family,structure_hash FROM "+GlobalResearchScheduleR932.IDENTITY+" WHERE candidate_key=?",new String[]{key})){
                        if(old.moveToFirst()&&(!c.family.equals(old.getString(0))||!structure.equals(old.getString(1))))throw new IllegalStateException("R943_CANDIDATE_IDENTITY_DRIFT");
                    }
                    db.execSQL("INSERT OR IGNORE INTO "+GlobalResearchScheduleR932.IDENTITY+" VALUES(?,?,?,?)",new Object[]{key,c.family,structure,target});
                    ResearchMultiplicityR932.registerCandidate(db,key,c.family,structure,target);
                    ResearchLifecycleR932.enroll(db,key,structure,target,c.tier);
                    try(Cursor old=db.rawQuery("SELECT 1 FROM "+ResearchCohortR932.IDENTITY+" WHERE candidate_key=?",new String[]{key})){
                        if(!old.moveToFirst())ResearchCohortR932.register(db,key,structure,structure,"GLOBAL_ALL_POSITIONS",target);
                    }
                    }
                    boolean bounded=row.optInt("admission_v",0)==1;
                    if(bounded&&!row.getString("family").equals(c.family))throw new IllegalStateException("R959_FROZEN_FAMILY_DRIFT");
                    String frozenTier=bounded?row.getString("tier"):c.tier;
                    int selected=bounded?row.getInt("selected"):1;
                    String mode=bounded&&frozenTier.equals("C")?"PROXY120":"FULL1200";
                    if(taskSlots.length()>0)taskSlots.append(',');taskSlots.append("(?,?,?,?,?,?,?,?)");
                    Collections.addAll(taskValues,target,key,c.family,structure,frozenTier,Integer.valueOf(selected),Integer.valueOf(i),mode);
                }
                if(taskSlots.length()>0){
                    db.execSQL("INSERT INTO "+GlobalResearchScheduleR932.TASK+" VALUES"+taskSlots,taskValues.toArray());
                    ResearchCaptureLedgerR932.selectPage(db,target,cursor,end);
                }
                db.execSQL("UPDATE "+TABLE+" SET cursor=? WHERE target_issue=?",new Object[]{end,target});
                if(end<rows.length())return false;
                if(drawn(db,target))throw new IllegalStateException("R943_ROSTER_DRAWN_BEFORE_PUBLICATION");
                db.execSQL("INSERT INTO "+GlobalResearchScheduleR932.PLAN+" VALUES(?,?,?,?)",new Object[]{target,input,V51ChecksumEngine.sha256(target+'|'+input+'|'+hash),System.currentTimeMillis()});
                return true;
                } catch (Exception bad) { failure[0] = bad; throw bad; }
            }
        },null);
        if(ready==null)throw new IllegalStateException("R943_ROSTER_PAGE_FAILED|target="+target
                +"|input="+input, failure[0]);
        return ready.booleanValue();
    }
    private static final class Registered{
        String family,structure,lifeStructure,hypothesisStructure,hypothesisFamily,cohortStructure,semantic,state,champion,domain,last;
        String tier(){return state.equals("IDEA")||state.equals("NURSERY")?"C":state.equals("INFLUENCE")||state.equals("CHAMPION")?"A":state.equals("QUARANTINED")||(state.equals("QUALITY_PRUNED")||state.equals("RESOURCE_RETIRED"))?"":"B";}
    }
    private static Map<String,Registered> registered(SQLiteDatabase db,JSONArray rows,int start,int end)throws JSONException{
        // Bound each page lookup to its own keys; scanning every historical identity on
        // every page makes registration quadratic as the retained algorithm pool grows.
        if(start==end)return Collections.emptyMap();
        StringBuilder marks=new StringBuilder();String[] keys=new String[end-start];
        for(int i=start;i<end;i++){if(i>start)marks.append(',');marks.append('?');keys[i-start]=rows.getJSONObject(i).getString("key");}
        Map<String,Registered> out=new HashMap<String,Registered>();
        try(Cursor c=db.rawQuery("SELECT i.candidate_key,i.family,i.structure_hash,l.structural_hash,h.structure_hash,h.family,c.structural_hash,c.semantic_hash,l.state,COALESCE(ch.candidate_key,''),c.domain_scope,COALESCE((SELECT MAX(s.target_issue) FROM "+GlobalResearchScheduleR932.TASK+" s WHERE s.candidate_key=i.candidate_key AND s.selected=1),'') FROM "+GlobalResearchScheduleR932.IDENTITY+" i JOIN "+ResearchLifecycleR932.TABLE+" l ON l.candidate_key=i.candidate_key JOIN "+ResearchMultiplicityR932.IDENTITY+" h ON h.candidate_key=i.candidate_key JOIN "+ResearchCohortR932.IDENTITY+" c ON c.candidate_key=i.candidate_key LEFT JOIN "+ResearchCohortR932.CHAMPION+" ch ON ch.family=i.family WHERE i.candidate_key IN ("+marks+")",keys)){
            while(c.moveToNext()){
                Registered r=new Registered();r.family=c.getString(1);r.structure=c.getString(2);r.lifeStructure=c.getString(3);r.hypothesisStructure=c.getString(4);r.hypothesisFamily=c.getString(5);r.cohortStructure=c.getString(6);r.semantic=c.getString(7);r.state=c.getString(8);r.champion=c.getString(9);r.domain=c.getString(10);r.last=c.getString(11);out.put(c.getString(0),r);
            }
        }
        return out;
    }
    /** Persist admission once before page zero; retries cannot redraw caps or priorities. */
    private static JSONArray freezeAdmission(SQLiteDatabase db,String target,JSONArray rows)throws JSONException {
        List<GlobalResearchScheduleR932.Candidate> candidates=new ArrayList<GlobalResearchScheduleR932.Candidate>();
        Map<String,JSONObject> payloads=new HashMap<String,JSONObject>();
        for(int start=0;start<rows.length();start+=PAGE) {
            int end=Math.min(rows.length(),start+PAGE);
            Map<String,Registered> prior=registered(db,rows,start,end);
            for(int i=start;i<end;i++) {
                JSONObject row=rows.getJSONObject(i);String key=row.getString("key"),structure=row.getString("structure"),family=row.getString("family"),tier=row.getString("tier"),last="";
                Registered r=prior.get(key);
                if(r!=null) {
                    if(!structure.equals(r.structure)||!structure.equals(r.lifeStructure)||!structure.equals(r.hypothesisStructure)||!structure.equals(r.cohortStructure)
                            ||!r.family.equals(r.hypothesisFamily)||!"GLOBAL_ALL_POSITIONS".equals(r.domain)
                            ||key.startsWith("algorithm:")&&!row.getString("semantic").equals(r.semantic))throw new IllegalStateException("R944_REGISTERED_CANDIDATE_DRIFT");
                    family=r.family;tier=r.tier();last=r.last;
                    if(tier.isEmpty()){if(!PairedResearchChallengeR932.requiresReference(db,key,target))continue;tier="B";}
                    if(tier.equals("A")&&!r.champion.isEmpty()&&!r.champion.equals(key))tier="B";
                }
                if(r==null) {
                    // A creator may have registered the statistical identity before the full
                    // roster/lifecycle projection exists. Freeze the same permanent family
                    // that add() will consume; an incomplete join is not a new identity.
                    ResearchMultiplicityR932.ResearchIdentity identity=ResearchMultiplicityR932.identity(db,key);
                    if(identity!=null) {
                        if(!structure.equals(identity.structureHash))
                            throw new IllegalStateException("R944_REGISTERED_CANDIDATE_DRIFT");
                        family=identity.family;
                    }
                }
                if(r==null && tier.equals("A"))tier="B";
                row.put("family",family);row.put("tier",tier);row.put("admission_v",1);
                candidates.add(new GlobalResearchScheduleR932.Candidate(key,family,structure,tier,last));payloads.put(key,row);
            }
        }
        final Map<String,Integer> chosen=GlobalResearchScheduleR932.choose(target,candidates);
        Collections.sort(candidates,(a,b)->{int rank=Integer.compare(chosen.containsKey(a.key)?chosen.get(a.key):Integer.MAX_VALUE,chosen.containsKey(b.key)?chosen.get(b.key):Integer.MAX_VALUE);return rank!=0?rank:a.key.compareTo(b.key);});
        JSONArray frozen=new JSONArray();
        for(GlobalResearchScheduleR932.Candidate c:candidates){JSONObject row=payloads.get(c.key);row.put("selected",chosen.containsKey(c.key)?1:0);frozen.put(row);}
        return frozen;
    }
    private static JSONArray collect(SQLiteDatabase db,String target)throws JSONException{
        JSONArray out=new JSONArray();
        try(Cursor c=db.rawQuery("SELECT f.candidate_id,f.structural_hash,f.ast FROM "+ResearchEvaluationRunR967.ALL+" f LEFT JOIN "+ResearchLifecycleR932.TABLE+" l ON l.candidate_key=('algorithm:'||f.candidate_id) WHERE f.created_issue<=? AND (f.run_hash<>'' OR NOT EXISTS(SELECT 1 FROM "+ResearchEvaluationRunR967.RUN+" r WHERE r.learner_id=f.learner_id AND r.first_issue<='"+target+"')) AND (l.state IS NULL OR l.state NOT IN ('QUARANTINED','QUALITY_PRUNED','RESOURCE_RETIRED')) ORDER BY f.candidate_id",new String[]{target})) {
            while(c.moveToNext()){if(!ResearchEvaluationRunR967.consumerCompatible(db,c.getString(0),target))continue;put(out,"algorithm:"+c.getString(0),"algorithm:R958_FIXED_POSITION_SOFTMAX",c.getString(1),"OBSERVING",V51ChecksumEngine.sha256("R958_FIXED_POSITION_SOFTMAX|"+c.getString(2)));}
        }
        try(Cursor c=db.rawQuery("SELECT genome_id,genome_spec,structural_hash,status FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" g WHERE (status IN ('IDEA_WAIT_FUTURE_OOS','OBSERVING_FUTURE_OOS','CHALLENGER_FUTURE_OOS','PAUSED_CAPACITY_R912') OR "+PairedResearchChallengeR932.referenceCondition("'algorithm:'||g.genome_id",target)+") AND (earliest_use_issue='' OR earliest_use_issue<=?) AND (trained_until_issue='' OR trained_until_issue<?) AND "+AlgorithmProofRecoveryR948.freezeReadySql("g.genome_id",target)+" ORDER BY genome_id",new String[]{target,target})){
            while(c.moveToNext()){
                AlgorithmGenomeR908 g=AlgorithmGenomeR908.parse(c.getString(1));TreeSet<String> ops=new TreeSet<String>();
                for(AlgorithmGenomeR908.Insn op:g.scoreProgram)ops.add(op.op.name());for(AlgorithmGenomeR908.Insn op:g.updateProgram)ops.add("update:"+op.op.name());
                put(out,"algorithm:"+c.getString(0),"algorithm:"+V51ChecksumEngine.sha256(ops.toString()),c.getString(2),c.getString(3),ExecutableIdentityR932.semantic(g));
            }
        }
        try(Cursor c=db.rawQuery("SELECT asset_id,family_id,structural_hash,status FROM "+AssetGenesisEngineR915.TABLE_ASSET_GENOME+" WHERE (status IN ('IDEA_WAIT_FUTURE_OOS','OBSERVING_FUTURE_OOS','CERTIFIED_FUTURE_OOS') OR "+PairedResearchChallengeR932.referenceCondition("'genesis:'||asset_id",target)+") AND may_authorize_real=0 AND (earliest_use_issue='' OR earliest_use_issue<=?) AND (trained_until_issue='' OR trained_until_issue<?) ORDER BY asset_id",new String[]{target,target})){
            while(c.moveToNext())put(out,"genesis:"+c.getString(0),"genesis:"+c.getString(1),c.getString(2),c.getString(3),c.getString(2));
        }
        try(Cursor c=db.rawQuery("SELECT asset_id,representation_family,learner_family,artifact_hash,tier FROM "+DbHelper.TABLE_R906_RESEARCH_ASSET+" WHERE runtime_executable=1 AND may_authorize_real=0 AND (tier IN ('IDEA','NURSERY','ACTIVE','CHALLENGER','CERTIFIED_SHADOW') OR "+PairedResearchChallengeR932.referenceCondition("'formula:'||asset_id",target)+") AND (earliest_use_issue='' OR earliest_use_issue<=?) AND (trained_until_issue='' OR trained_until_issue<?) ORDER BY asset_id",new String[]{target,target})){
            while(c.moveToNext())put(out,"formula:"+c.getString(0),"formula:"+c.getString(1)+":"+c.getString(2),c.getString(3),c.getString(4),c.getString(3));
        }
        try(Cursor c=db.rawQuery("SELECT artifact_hash FROM v57_research_artifact_r811 WHERE artifact_type=? AND trained_until_issue<? AND status='SHADOW_RESEARCH_ONLY' ORDER BY trained_until_issue DESC LIMIT 1",new String[]{ResearchTinyDeepLearnerR909.ARTIFACT_TYPE,target})){
            if(c.moveToFirst())put(out,"tiny:"+ResearchTinyDeepLearnerR909.CANDIDATE_ID,"tiny:7x16x8x1",V51ChecksumEngine.sha256("tiny:7x16x8x1"),"OBSERVING",V51ChecksumEngine.sha256("tiny:7x16x8x1"));
        }
        return out;
    }
    private static void put(JSONArray out,String key,String family,String structure,String state,String semantic)throws JSONException{
        JSONObject j=new JSONObject();j.put("key",key);j.put("family",family);j.put("structure",structure);j.put("tier",GlobalResearchScheduleR932.tier(state));j.put("semantic",semantic);out.put(j);
    }
    private static boolean drawn(SQLiteDatabase db,String target){try(Cursor c=db.rawQuery("SELECT 1 FROM "+DbHelper.TABLE_DRAWS+" WHERE issue>=? LIMIT 1",new String[]{target})){return c.moveToFirst();}}
}
