package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;
import org.json.JSONObject;

/** C05 acceptance: family champion changes only after a new paid 120-issue paired challenge. */
public final class FamilyChampionHarnessR932 {
    static String hash(String s){return V51ChecksumEngine.sha256(s);}
    static void check(boolean b,String s){if(!b)throw new AssertionError(s);System.out.println(s+"=PASS");}
    static String scalar(SQLiteDatabase db,String sql,String... args){try(Cursor c=db.rawQuery(sql,args.length==0?null:args)){return c.moveToFirst()?c.getString(0):"";}}
    static String previous(String issue){TargetIssueResolver.ParsedIssue p=TargetIssueResolver.parse(issue);if(p==null||p.sequence<=1)throw new IllegalArgumentException("C05_PREVIOUS_ISSUE");return p.date+String.format(Locale.US,"%04d",p.sequence-1);}
    static CertificateBundleServiceR929.EvidenceInput evidence(){return new CertificateBundleServiceR929.EvidenceInput(true,true,true,360,360,2,.44,.38,.85,.18,.02,.04,.01,true,true,hash("FAMILY_CHAMP_EVIDENCE"));}

    static void installBundle(SQLiteDatabase db){
        ResearchPlatformStoreR929.createSchema(db);
        String bundle=ModelBundleRuntimeR932.current().hash,schema=V51ActionMetaFeatureEngine.featureManifestHash(),code=SourceManifestR815.SOURCE_TREE_SHA256,h=hash("FAMILY_CHAMP_BUNDLE_ROW");
        db.execSQL("INSERT OR IGNORE INTO "+ResearchPlatformStoreR929.TABLE_BUNDLE+" VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{bundle,"fixture-family-champion",V51Repository.GAME_CODE,schema,code,h,"TEST_KEY","RSA",1,6,100,"202609100001","202609100001","ACTIVE","SYNTHETIC_FIXTURE_NOT_RELEASE",1,1,"",h});
    }
    static String certificate(DbStore store,SQLiteDatabase db,String key,String structure,String family){
        String trial=hash("TRIAL|"+key),schema=V51ActionMetaFeatureEngine.featureManifestHash(),code=SourceManifestR815.SOURCE_TREE_SHA256;
        db.execSQL("INSERT OR IGNORE INTO "+ResearchPlatformStoreR929.TABLE_TRIAL+"(trial_id,identity_hash,stage,status,dataset_hash,schema_hash,source_hash,hyper_hash,family_id,structural_hash,split_hash,seed,registered_at,finished_at,record_hash) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{trial,trial,"EVALUATE","COMPLETED",hash("DATA|"+key),schema,code,hash("HYPER|"+key),family,structure,hash("SPLIT|"+key),1,1,1,trial});
        String id=key.substring(key.indexOf(':')+1);
        CertificateBundleServiceR929.CandidateKey ck=new CertificateBundleServiceR929.CandidateKey(CertificateBundleServiceR929.Consumer.INVENTED_ALGORITHM,"TEST",family,id,structure);
        CertificateBundleServiceR929.Evaluation ev=CertificateBundleServiceR929.evaluate(evidence());
        CertificateBundleServiceR929.Certificate cert=new CertificateBundleServiceR929.Certificate(ck,ev.level,CertificateBundleServiceR929.CertificateState.ACTIVE,
            ModelBundleRuntimeR932.current().hash,schema,AtomCaptureR932.graphHash(store),CertificateDependenciesR932.modelHash(),CertificateDependenciesR932.component("POLICY"),CertificateDependenciesR932.component("CALIBRATION"),DomainRouterR929.installedSchemaHash(),trial,CertificateDependenciesR932.selectionHash(),code,"202609100001","202609101400",2,.05,ev.gates,"C05_FAMILY_CHAMPION_FIXTURE");
        check(ResearchPlatformStoreR929.putCertificate(store,cert,hash("CERT_EVIDENCE|"+key)),"CERTIFICATE_INSTALLED_"+id.toUpperCase(Locale.ROOT));
        return cert.certificateHash;
    }
    static void calibration(SQLiteDatabase db,String key,String structure,boolean hit)throws Exception{
        String study=CandidateCalibrationR932.study(db,key,structure,ModelBundleRuntimeR932.current().hash,SelectionEvidenceContractR929.installedPolicy().hash);
        double[] scores=new double[241],hits=new double[241];Arrays.fill(scores,1);if(hit)Arrays.fill(hits,1);
        JSONObject m=ResearchLogisticCalibrationR932.fit(scores,hits);ResearchLogisticCalibrationR932.validate(m,scores,hits);
        String json=m.toString();db.execSQL("INSERT INTO "+CandidateCalibrationR932.MODEL+" VALUES(?,?, '202609100120','202609100241',?,?,1)",new Object[]{study,key,json,hash(json)});
    }
    static void freeze(SQLiteDatabase db,String key,String structure,String base,String target,boolean hit){
        int a=hit?1:2,b=hit?2:3,c=hit?3:4,selected=K3ActionSpaceEngine.globalOrdinal(1,a,b,c);
        CandidateExecutionAuditR932.Builder proof=new CandidateExecutionAuditR932.Builder();for(int i=0;i<1200;i++)proof.add(i,i==selected?2:1,i==selected?2:1,i);
        String prediction=hash(key+'|'+target);
        proof.finish(selected,"SYNTHETIC_PROGRAM","SYNTHETIC_PROGRAM","STATE","STATE","STATE").freeze(db,key,structure,hash("SURFACE|"+target),base,target,prediction);
        ResearchCaptureLedgerR932.record(db,target,key,"FROZEN","C05_FULL1200",prediction,1,true);
        FixedResearchWindowR932.freezePair(db,key,structure,target,prediction,1,a,b,c,1);
    }
    static final class Fixture {String family,parent,child,ps,cs,parentCert,childCert,id,first,last;}
    static Fixture prepare(DbStore store,SQLiteDatabase db,String suffix,String first)throws Exception{
        Fixture f=new Fixture();f.family="C05_FAMILY_"+suffix;f.parent="algorithm:c05-parent-"+suffix;f.child="algorithm:c05-child-"+suffix;f.ps=hash(f.parent);f.cs=hash(f.child);f.first=first;
        db.beginTransaction();try{
            ResearchMultiplicityR932.registerCandidate(db,f.parent,f.family,f.ps,"202609100001");ResearchMultiplicityR932.registerCandidate(db,f.child,f.family,f.cs,"202609100001");
            ResearchLifecycleR932.enroll(db,f.parent,f.ps,"202609100001","B");ResearchLifecycleR932.enroll(db,f.child,f.cs,"202609100001","B");
            ResearchCohortR932.register(db,f.parent,f.ps,hash("SEM|"+f.parent),"GLOBAL_ALL_POSITIONS","202609100001");ResearchCohortR932.register(db,f.child,f.cs,hash("SEM|"+f.child),"GLOBAL_ALL_POSITIONS","202609100001");
            calibration(db,f.parent,f.ps,false);calibration(db,f.child,f.cs,true);db.setTransactionSuccessful();
        }finally{db.endTransaction();}
        f.parentCert=certificate(store,db,f.parent,f.ps,f.family);f.childCert=certificate(store,db,f.child,f.cs,f.family);
        db.beginTransaction();try{
            db.execSQL("INSERT INTO "+ResearchCohortR932.CHAMPION+" VALUES(?,?,?,?,?,?)",new Object[]{f.family,f.parent,f.ps,f.parentCert,hash("PARENT_EVIDENCE|"+suffix),first});
            f.id=PairedResearchChallengeR932.request(db,"FAMILY_REPLACEMENT",f.parent,f.child,f.ps,f.cs,hash("REPLACE_REASON|"+suffix));
            PairedResearchChallengeR932.activate(db,f.id,f.parent,f.child,f.ps,f.cs,first);db.setTransactionSuccessful();
        }finally{db.endTransaction();}
        f.last=scalar(db,"SELECT last_issue FROM "+PairedResearchChallengeR932.TABLE+" WHERE challenge_id=?",f.id);
        check("OPEN".equals(scalar(db,"SELECT state FROM "+PairedResearchChallengeR932.TABLE+" WHERE challenge_id=?",f.id)),"FAMILY_REPLACEMENT_OPEN_"+suffix);
        return f;
    }
    static void populate120(DbStore store,SQLiteDatabase db,Fixture f){
        String target=f.first,base=previous(target);
        for(int i=0;i<120;i++){
            db.beginTransaction();try{
                db.execSQL("INSERT OR IGNORE INTO "+FixedResearchWindowR932.CHAMPION+" VALUES(?,?,?,?,1,2,3,4,?,1)",new Object[]{target,ModelBundleRuntimeR932.current().hash,hash("SURFACE|"+target),SelectionEvidenceContractR929.installedPolicy().hash,hash("BASE_CHAMP|"+target)});
                freeze(db,f.parent,f.ps,base,target,false);freeze(db,f.child,f.cs,base,target,true);db.setTransactionSuccessful();
            }finally{db.endTransaction();}
            db.execSQL("INSERT INTO "+DbHelper.TABLE_DRAWS+" VALUES(?,1,2,3,4,5,6,7,8,9,10,0,0,0)",new Object[]{target});
            base=target;target=TargetIssueResolver.next(target).targetIssue;if(i%8==7)FixedResearchWindowR932.maintain(store);
        }
        for(int i=0;i<20;i++)FixedResearchWindowR932.maintain(store);
        check("240".equals(scalar(db,"SELECT COUNT(*) FROM "+PairedResearchChallengeR932.PREDICTION+" WHERE challenge_id=? AND hit>=0",f.id)),"FAMILY_REPLACEMENT_120_PAIRS_SETTLED_"+f.family);
    }
    static void evaluate(SQLiteDatabase db,Fixture f){db.beginTransaction();try{PairedResearchChallengeR932.evaluate(db,f.id,f.last);db.setTransactionSuccessful();}finally{db.endTransaction();}}

    public static void main(String[] args)throws Exception{
        SQLiteDatabase db=new SQLiteDatabase(args[0]);
        try(ModelBundleRuntimeR932.Scope scope=ModelBundleRuntimeR932.open(null,"202609101300",System.currentTimeMillis())){
            DbStore store=new DbStore(db);FixedResearchWindowR932.schema(db);FixedWindowCertificatePublisherR932.schema(db);ResearchLifecycleR932.schema(db);ResearchCohortR932.schema(db);installBundle(db);
            db.execSQL("CREATE TABLE "+DbHelper.TABLE_DRAWS+"(issue TEXT PRIMARY KEY,n1 INTEGER,n2 INTEGER,n3 INTEGER,n4 INTEGER,n5 INTEGER,n6 INTEGER,n7 INTEGER,n8 INTEGER,n9 INTEGER,n10 INTEGER,dirty INTEGER,open_time INTEGER DEFAULT 0,fetch_time INTEGER DEFAULT 0)");

            Fixture good=prepare(store,db,"GOOD","202609100300");
            boolean early=false;db.beginTransaction();try{PairedResearchChallengeR932.evaluate(db,good.id,previous(good.last));}catch(IllegalStateException expected){early=true;}finally{db.endTransaction();}
            check(early,"FAMILY_CHAMPION_CANNOT_REPLACE_BEFORE_FIXED_ENDPOINT");
            populate120(store,db,good);evaluate(db,good);
            check("PASSED".equals(scalar(db,"SELECT state FROM "+PairedResearchChallengeR932.TABLE+" WHERE challenge_id=?",good.id)),"FAMILY_REPLACEMENT_PASSES_ONLY_AFTER_NEW_120_PAIRED");
            String result=scalar(db,"SELECT result_hash FROM "+PairedResearchChallengeR932.TABLE+" WHERE challenge_id=?",good.id),json=scalar(db,"SELECT result_json FROM "+PairedResearchChallengeR932.TABLE+" WHERE challenge_id=?",good.id);
            JSONObject proof=new JSONObject(json);check("FAMILY_REPLACEMENT".equals(proof.getString("purpose"))&&proof.getInt("paired")==120&&!proof.getBoolean("may_authorize_real")&&proof.getString("predictions_hash").matches("[0-9a-f]{64}"),"FAMILY_REPLACEMENT_RESULT_IS_AUDITABLE_AND_NO_REAL_AUTHORITY");
            String selected=TargetIssueResolver.next(good.last).targetIssue;
            check(good.child.equals(scalar(db,"SELECT candidate_key FROM "+ResearchCohortR932.CHAMPION+" WHERE family=?",good.family))&&good.cs.equals(scalar(db,"SELECT structural_hash FROM "+ResearchCohortR932.CHAMPION+" WHERE family=?",good.family))&&good.childCert.equals(scalar(db,"SELECT certificate_hash FROM "+ResearchCohortR932.CHAMPION+" WHERE family=?",good.family))&&result.equals(scalar(db,"SELECT evidence_hash FROM "+ResearchCohortR932.CHAMPION+" WHERE family=?",good.family))&&selected.equals(scalar(db,"SELECT selected_issue FROM "+ResearchCohortR932.CHAMPION+" WHERE family=?",good.family)),"FAMILY_CHAMPION_ROW_TRACES_RESULT_CERTIFICATE_AND_EFFECTIVE_ISSUE");
            evaluate(db,good);check(result.equals(scalar(db,"SELECT evidence_hash FROM "+ResearchCohortR932.CHAMPION+" WHERE family=?",good.family)),"COMPLETED_REPLACEMENT_REPLAY_IS_IDEMPOTENT");

            Fixture stale=prepare(store,db,"STALE","202609100700");populate120(store,db,stale);
            db.beginTransaction();try{db.execSQL("UPDATE "+ResearchCohortR932.CHAMPION+" SET candidate_key=?,structural_hash=? WHERE family=?",new Object[]{stale.child,stale.cs,stale.family});db.setTransactionSuccessful();}finally{db.endTransaction();}
            evaluate(db,stale);check("PAUSED_DEPENDENCY".equals(scalar(db,"SELECT state FROM "+PairedResearchChallengeR932.TABLE+" WHERE challenge_id=?",stale.id))&&stale.child.equals(scalar(db,"SELECT candidate_key FROM "+ResearchCohortR932.CHAMPION+" WHERE family=?",stale.family)),"STALE_PARENT_CANNOT_CAS_REPLACE_FAMILY_CHAMPION");

            Fixture revoked=prepare(store,db,"REVOKED","202609101100");populate120(store,db,revoked);db.execSQL("INSERT INTO "+ResearchPlatformStoreR929.TABLE_REVOCATION+" VALUES(?,'C05_NEGATIVE_REVOKED_CHILD',3)",new Object[]{revoked.childCert});
            boolean rejected=false;try{evaluate(db,revoked);}catch(IllegalStateException expected){rejected=true;}
            check(rejected&&"OPEN".equals(scalar(db,"SELECT state FROM "+PairedResearchChallengeR932.TABLE+" WHERE challenge_id=?",revoked.id))&&revoked.parent.equals(scalar(db,"SELECT candidate_key FROM "+ResearchCohortR932.CHAMPION+" WHERE family=?",revoked.family)),"REVOKED_CHILD_CERTIFICATE_ROLLS_BACK_REPLACEMENT_ATOMICALLY");

            System.out.println("R932_FAMILY_CHAMPION_PASS|checks=8|scope=PRODUCTION_CLASSES_REAL_SQLITE_SYNTHETIC_120_PAIRED_NOT_DEVICE_NOT_REAL_FUTURE");
        }finally{db.shutdown();}
    }
}
