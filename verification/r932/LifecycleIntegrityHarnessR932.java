package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;

/** Exercises actual production methods with the real SQLite bridge, never a parallel model. */
public final class LifecycleIntegrityHarnessR932 {
    private static int checks;
    private static String hash(String v){return V51ChecksumEngine.sha256(v);}
    private static void check(boolean b,String n){if(!b)throw new AssertionError(n);checks++;System.out.println(n+"=PASS");}
    private static String scalar(SQLiteDatabase db,String sql,String... args){try(Cursor c=db.rawQuery(sql,args.length==0?null:args)){return c.moveToFirst()?c.getString(0):"";}}
    private static String structure(String k){return hash("INTEGRITY_STRUCTURE|"+k);}
    private static String state(SQLiteDatabase db,String k){return scalar(db,"SELECT state FROM "+ResearchLifecycleR932.TABLE+" WHERE candidate_key=?",k);}
    private static void enroll(SQLiteDatabase db,String k){
        ResearchLifecycleR932.enroll(db,k,structure(k),"202609100001","B");
        ResearchMultiplicityR932.registerCandidate(db,k,"INTEGRITY_TEST_FAMILY",structure(k),"202609100001");
    }
    private static String certificate(SQLiteDatabase db,String k){
        String family="INTEGRITY_TEST_FAMILY",structural=structure(k),trial=hash(k+"|trial"),cert=hash(k+"|certificate_hash");
        db.execSQL("INSERT INTO "+ResearchPlatformStoreR929.TABLE_TRIAL+"(trial_id,identity_hash,stage,status,dataset_hash,schema_hash,source_hash,hyper_hash,family_id,structural_hash,split_hash,seed,registered_at,finished_at,record_hash) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                new Object[]{trial,trial,"EVALUATE","COMPLETED",hash(k+"|dataset"),V51ActionMetaFeatureEngine.featureManifestHash(),SourceManifestR815.SOURCE_TREE_SHA256,hash(k+"|hyper"),family,structural,hash(k+"|split"),1,1,2,trial});
        ContentValues v=new ContentValues();
        for(String f:new String[]{"candidate_key","namespace","family_id","candidate_id","structural_hash","evidence_hash","bundle_hash","schema_hash","atom_graph_hash","model_hash","policy_hash","calibration_hash","domain_hash","trial_hash","selection_hash","code_hash","gate_hash","reason","certificate_hash"})v.put(f,hash(k+'|'+f));
        v.put("consumer","INVENTED_ALGORITHM");v.put("candidate_id",k.substring(k.indexOf(':')+1));v.put("family_id",family);v.put("structural_hash",structural);
        v.put("bundle_hash",ModelBundleRuntimeR932.current().hash);v.put("schema_hash",V51ActionMetaFeatureEngine.featureManifestHash());
        v.put("atom_graph_hash",AtomCaptureR932.graphHash(db));v.put("model_hash",CertificateDependenciesR932.modelHash());
        v.put("policy_hash",CertificateDependenciesR932.component("POLICY"));v.put("calibration_hash",CertificateDependenciesR932.component("CALIBRATION"));
        v.put("domain_hash",DomainRouterR929.installedSchemaHash());v.put("trial_hash",trial);v.put("selection_hash",CertificateDependenciesR932.selectionHash());v.put("code_hash",SourceManifestR815.SOURCE_TREE_SHA256);
        v.put("certificate_hash",cert);v.put("level","L6_CONTROLLED_INFLUENCE");v.put("state","ACTIVE");v.put("valid_from_issue","202609100001");v.put("valid_until_issue","");
        v.put("maximum_weight",.05);v.put("all_gates_pass",1);v.put("issued_at",3L);v.put("updated_at",3L);v.put("contract_revision","R932_REQUIRED_GATES_V1");
        if(db.insertWithOnConflict(ResearchPlatformStoreR929.TABLE_CERTIFICATE,null,v,0)<0)throw new IllegalStateException("R932_FIXTURE_CERTIFICATE_INSERT");
        return cert;
    }
    private static String window(SQLiteDatabase db,String k){return FixedResearchWindowR932.register(db,k,structure(k),ModelBundleRuntimeR932.current().hash,SelectionEvidenceContractR929.installedPolicy().hash,"202609100001",120,1);}
    private static void pair(SQLiteDatabase db,String w,String k,String issue,int hit,double p){
        db.execSQL("INSERT INTO "+FixedResearchWindowR932.PAIR+" VALUES(?,?,?,?,1,1,2,3,?,?,1,?,0,2)",new Object[]{w,issue,hash(k+'|'+issue),hash("BASE|"+issue),p,hash("CAL|"+k),hit});
    }
    private static List<GlobalResearchScheduleR932.Candidate> pool(String... keys){List<GlobalResearchScheduleR932.Candidate> out=new ArrayList<GlobalResearchScheduleR932.Candidate>();for(String k:keys)out.add(new GlobalResearchScheduleR932.Candidate(k,"INTEGRITY_TEST_FAMILY",structure(k),"B",""));return out;}
    public static void main(String[] args)throws Exception{
        SQLiteDatabase db=new SQLiteDatabase(args[0]);try(ModelBundleRuntimeR932.Scope request=ModelBundleRuntimeR932.open(null,"202609100121",System.currentTimeMillis())){
            DbStore store=new DbStore(db);
            GlobalResearchScheduleR932.schema(db);FixedResearchWindowR932.schema(db);FixedWindowCertificatePublisherR932.schema(db);
            db.execSQL("CREATE TABLE "+DbHelper.TABLE_DRAWS+"(issue TEXT PRIMARY KEY,n1 INTEGER,n2 INTEGER,n3 INTEGER,n4 INTEGER,n5 INTEGER,n6 INTEGER,n7 INTEGER,n8 INTEGER,n9 INTEGER,n10 INTEGER,dirty INTEGER)");
            db.beginTransaction();try{
                ResearchPlatformStoreR929.activateRuntimeBundle(db,ModelBundleRuntimeR932.current(),"202609100001",1);
                String key="algorithm:paused";enroll(db,key);certificate(db,key);String w=window(db,key);
                db.execSQL("UPDATE "+FixedResearchWindowR932.WINDOW+" SET state='PAUSED_CAPTURE',reason='SYSTEM_CAPTURE_GAP' WHERE window_id=?",new Object[]{w});
                ResearchLifecycleR932.review(store,db,key,"202609100120");
                check(state(db,key).equals("PAUSED"),"OLD_ACTIVE_CERTIFICATE_CANNOT_OVERRIDE_PAUSED_CAPTURE");
                String events=scalar(db,"SELECT COUNT(*) FROM "+ResearchLifecycleR932.EVENT);
                ResearchLifecycleR932.review(store,db,key,"202609100120");
                check(events.equals(scalar(db,"SELECT COUNT(*) FROM "+ResearchLifecycleR932.EVENT)),"REPEATED_PAUSED_REVIEW_DOES_NOT_DUPLICATE_STATE_EVENT");
                db.execSQL("UPDATE "+FixedResearchWindowR932.WINDOW+" SET state='OPEN' WHERE window_id=?",new Object[]{w});
                ResearchLifecycleR932.review(store,db,key,"202609100120");
                check(state(db,key).equals("PAUSED"),"REVIEW_CANNOT_CANCEL_PAUSE_WITHOUT_EXPLICIT_RECOVERY");
                for(String variant:new String[]{"revoked","future","expired","bundle","source","graph","gate","valid"}){
                    String k="algorithm:"+variant;enroll(db,k);String cert=certificate(db,k),table=ResearchPlatformStoreR929.TABLE_CERTIFICATE;
                    if(variant.equals("revoked"))db.execSQL("INSERT INTO "+ResearchPlatformStoreR929.TABLE_REVOCATION+" VALUES(?,'TEST_REVOCATION',2)",new Object[]{cert});
                    if(variant.equals("future"))db.execSQL("UPDATE "+table+" SET valid_from_issue='202609100500' WHERE certificate_hash=?",new Object[]{cert});
                    if(variant.equals("expired"))db.execSQL("UPDATE "+table+" SET valid_until_issue='202609100010' WHERE certificate_hash=?",new Object[]{cert});
                    if(variant.equals("bundle"))db.execSQL("UPDATE "+table+" SET bundle_hash=? WHERE certificate_hash=?",new Object[]{hash("OTHER_BUNDLE"),cert});
                    if(variant.equals("source"))db.execSQL("UPDATE "+table+" SET code_hash=? WHERE certificate_hash=?",new Object[]{hash("OTHER_SOURCE"),cert});
                    if(variant.equals("graph"))db.execSQL("UPDATE "+table+" SET atom_graph_hash=? WHERE certificate_hash=?",new Object[]{hash("OTHER_ATOM_GRAPH"),cert});
                    if(variant.equals("gate"))db.execSQL("UPDATE "+table+" SET all_gates_pass=0 WHERE certificate_hash=?",new Object[]{cert});
                    ResearchLifecycleR932.review(store,db,k,"202609100120");
                    check(state(db,k).equals(variant.equals("valid")?"INFLUENCE":"OBSERVING"),"CERTIFICATE_PROJECTION_"+variant.toUpperCase(Locale.ROOT));
                }
                String counts="algorithm:counts";enroll(db,counts);String cw=window(db,counts);
                pair(db,cw,counts,"202609100010",1,.5);pair(db,cw,counts,"202609100130",1,.5);pair(db,cw,counts,"202609100011",1,1.1);pair(db,cw,counts,"202609100012",2,.5);
                ResearchLifecycleR932.review(store,db,counts,"202609100120");
                check(scalar(db,"SELECT future_n FROM "+ResearchLifecycleR932.TABLE+" WHERE candidate_key=?",counts).equals("1"),"FUTURE_OUT_OF_WINDOW_AND_INVALID_VALUES_NOT_COUNTED_AS_SUPPORT");
                db.execSQL("UPDATE "+FixedResearchWindowR932.CONTRACT+" SET contract_hash=? WHERE window_id=?",new Object[]{hash("OLD_CONTRACT"),cw});
                ResearchLifecycleR932.review(store,db,counts,"202609100120");
                check(scalar(db,"SELECT future_n FROM "+ResearchLifecycleR932.TABLE+" WHERE candidate_key=?",counts).equals("0"),"STALE_WINDOW_CONTRACT_CANNOT_SUPPLY_LIFECYCLE_OR_ELITE_SUPPORT");
                String[] chain={"algorithm:chain-a","algorithm:chain-b","algorithm:chain-c"};
                for(int j=0;j<3;j++){
                    String k=chain[j];enroll(db,k);ResearchCohortR932.register(db,k,structure(k),structure(k),"GLOBAL_ALL_POSITIONS","202609100001");String ww=window(db,k);
                    for(int i=0;i<120;i++){
                        double theta=2*Math.PI*i/120,sn=Math.sin(theta),cs=Math.cos(theta);
                        double value=j==0?sn:j==1?.96*sn+.28*cs:(2*.96*.96-1)*sn+2*.96*.28*cs;
                        pair(db,ww,k,String.format(Locale.ROOT,"20260910%04d",i+1),0,.5+.2*value);
                    }
                }
                ResearchCohortR932.review(db,chain[0],chain[1],"202609100120");ResearchCohortR932.review(db,chain[1],chain[2],"202609100120");ResearchCohortR932.review(db,chain[0],chain[2],"202609100120");
                check(scalar(db,"SELECT classification FROM "+ResearchCohortR932.COMPARISON+" WHERE left_key=? AND right_key=?",chain[0],chain[2]).equals("DISTINCT"),"NONTRANSITIVE_FIXTURE_USES_ACTUAL_PAIRED_RESIDUAL_REVIEW");
                Map<String,String> clusters=ResearchCohortR932.clusters(db,pool(chain));
                check(clusters.get(chain[0]).equals(clusters.get(chain[1]))&&!clusters.get(chain[0]).equals(clusters.get(chain[2])),"COMPLETE_LINK_KEEPS_DISTINCT_C_DESPITE_A_B_C_CORRELATION_CHAIN");
                check(clusters.equals(ResearchCohortR932.clusters(db,pool(chain[2],chain[1],chain[0]))),"COMPLETE_LINK_IS_INDEPENDENT_OF_CANDIDATE_INPUT_ORDER");
                String denominator=scalar(db,"SELECT COUNT(*) FROM "+ResearchMultiplicityR932.HYPOTHESIS);
                ResearchCohortR932.clusters(db,pool(chain));
                check(denominator.equals(scalar(db,"SELECT COUNT(*) FROM "+ResearchMultiplicityR932.HYPOTHESIS)),"COMPLETE_LINK_NEVER_REWRITES_HYPOTHESIS_DENOMINATOR");
                db.setTransactionSuccessful();
            }finally{db.endTransaction();}
            String before=state(db,"algorithm:valid");boolean rejected=false;
            try{ResearchLifecycleR932.transition(db,"algorithm:valid",ResearchLifecycleR932.State.PAUSED,"OUTSIDE_TRANSACTION","202609100121",hash("BAD"),0,0);}catch(IllegalStateException expected){rejected=true;}
            check(rejected&&before.equals(state(db,"algorithm:valid")),"LIFECYCLE_OUTSIDE_TRANSACTION_REJECTED_WITH_NO_PARTIAL_EVENT");
            rejected=false;try{ResearchLifecycleR932.review(store,db,"algorithm:valid","202609100120");}catch(IllegalStateException expected){rejected=true;}
            check(rejected,"LIFECYCLE_REVIEW_REQUIRES_EXISTING_WRITE_TRANSACTION");
            String events=scalar(db,"SELECT COUNT(*) FROM "+ResearchLifecycleR932.EVENT);
            db.beginTransaction();try{ResearchLifecycleR932.transition(db,"algorithm:valid",ResearchLifecycleR932.State.PAUSED,"INTERRUPTED","202609100121",hash("INTERRUPTED"),0,0);}finally{db.endTransaction();}
            check(before.equals(state(db,"algorithm:valid"))&&events.equals(scalar(db,"SELECT COUNT(*) FROM "+ResearchLifecycleR932.EVENT)),"INTERRUPTED_TRANSITION_ROLLS_BACK_STATE_AND_EVENT_TOGETHER");
            System.out.println("R932_LIFECYCLE_INTEGRITY_PASS|checks="+checks+"|scope=PRODUCTION_CLASSES_REAL_SQLITE_SYNTHETIC_FIXTURES|may_authorize_real=false");
        }finally{db.shutdown();}
    }
}
