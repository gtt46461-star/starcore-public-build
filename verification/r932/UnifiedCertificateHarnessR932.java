package com.ethk3.novaonepro;
import java.util.*;
import android.database.sqlite.SQLiteDatabase;

/** Fixture-only observations, passed through the actual shared evaluator and persisted authorizer. */
public final class UnifiedCertificateHarnessR932 {
    static void check(boolean ok,String code){if(!ok)throw new AssertionError(code);System.out.println(code+"=PASS");}
    static CertificateBundleServiceR929.EvidenceInput input(int n,int windows){return new CertificateBundleServiceR929.EvidenceInput(true,true,true,n,n,windows,.44,.38,.85,.18,.02,.04,.01,true,true,V51ChecksumEngine.sha256("SYNTHETIC_WINDOW_"+n));}
    public static void main(String[] args)throws Exception{
        SQLiteDatabase db=new SQLiteDatabase(args[0]);try(ModelBundleRuntimeR932.Scope request=ModelBundleRuntimeR932.open(null,"200",System.currentTimeMillis())){
            DbStore store=new DbStore(db);ResearchPlatformStoreR929.createSchema(db);
            String h=V51ChecksumEngine.sha256("FIXTURE_ONLY"),bundle=ModelBundleRuntimeR932.current().hash,schema=V51ActionMetaFeatureEngine.featureManifestHash(),code=SourceManifestR815.SOURCE_TREE_SHA256;
            db.execSQL("INSERT INTO "+ResearchPlatformStoreR929.TABLE_BUNDLE+" VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{bundle,"fixture","Binance1-PK10",schema,code,h,"TEST_KEY","RSA",1,6,100,"100","101","ACTIVE","SYNTHETIC_FIXTURE_NOT_VERIFIED_PRODUCTION",1,1,"",h});
            check(CertificateBundleServiceR929.validatedInfluenceCap(Collections.<CertificateBundleServiceR929.GateObservation>emptyList())==0,"EMPTY_GATES_NO_AUTHORITY");
            check(CertificateBundleServiceR929.evaluate(input(240,1)).level==CertificateBundleServiceR929.Level.L5_SHADOW,"240_TOTAL_CANNOT_REACH_L6");
            check(CertificateBundleServiceR929.validatedInfluenceCap(CertificateBundleServiceR929.evaluate(input(360,2)).gates)==.05,"120_PLUS_NEW_240_FIVE_PERCENT");
            check(CertificateBundleServiceR929.validatedInfluenceCap(CertificateBundleServiceR929.evaluate(input(600,3)).gates)==.12,"NEXT_INDEPENDENT_WINDOW_TWELVE_PERCENT");
            check(CertificateBundleServiceR929.validatedInfluenceCap(CertificateBundleServiceR929.evaluate(input(840,4)).gates)==.20,"NEXT_INDEPENDENT_WINDOW_TWENTY_PERCENT");
            check(CertificateBundleServiceR929.validatedInfluenceCap(CertificateBundleServiceR929.evaluate(input(1200,5)).gates)==.30,"1200_FIVE_WINDOWS_THIRTY_PERCENT_MAXIMUM");
            check(CertificateBundleServiceR929.validatedInfluenceCap(CertificateBundleServiceR929.evaluate(input(1200,2)).gates)==.05,"TOTAL_SAMPLES_CANNOT_REPLACE_INDEPENDENT_WINDOWS");
            db.execSQL("INSERT INTO "+ResearchPlatformStoreR929.TABLE_TRIAL+"(trial_id,identity_hash,stage,status,dataset_hash,schema_hash,source_hash,hyper_hash,family_id,structural_hash,split_hash,seed,registered_at,finished_at,record_hash) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{h,h,"EVALUATE","COMPLETED",h,schema,code,h,"TEST_FAMILY",h,h,1,1,1,h});
            int i=0;for(CertificateBundleServiceR929.Consumer consumer:CertificateBundleServiceR929.Consumer.values()){
                String id="fixture_"+i++;CertificateBundleServiceR929.CandidateKey key=new CertificateBundleServiceR929.CandidateKey(consumer,"TEST","TEST_FAMILY",id,h);
                CertificateBundleServiceR929.Evaluation eval=CertificateBundleServiceR929.evaluate(input(360,2));
                CertificateBundleServiceR929.Certificate cert=new CertificateBundleServiceR929.Certificate(key,eval.level,CertificateBundleServiceR929.CertificateState.ACTIVE,
                    bundle,schema,AtomCaptureR932.graphHash(store),CertificateDependenciesR932.modelHash(),CertificateDependenciesR932.component("POLICY"),CertificateDependenciesR932.component("CALIBRATION"),DomainRouterR929.installedSchemaHash(),h,CertificateDependenciesR932.selectionHash(),code,"101","300",2,.05,eval.gates,"SYNTHETIC_TEST_ONLY");
                check(ResearchPlatformStoreR929.putCertificate(store,cert,h),consumer+"_PERSISTED_CERTIFICATE");
                ResearchPlatformStoreR929.Authorization granted=ResearchPlatformStoreR929.authorizeCurrent(store,consumer,id,h,"200",.30);
                check(granted.authorized&&granted.grantedWeight==.05&&!granted.mayCreateReal,consumer+"_IDENTICAL_AUTHORIZATION_NO_REAL");
                check(PromotionWindowR925.authorize(store,consumer,id,h,"200").weight==.05,consumer+"_PRODUCTION_ADAPTER_USES_SHARED_AUTHORITY");
                check(!ResearchPlatformStoreR929.authorizeCurrent(store,consumer,id,h,"301",.30).authorized,consumer+"_EXPIRED_REJECTED");
                db.execSQL("UPDATE "+ResearchPlatformStoreR929.TABLE_CERTIFICATE+" SET model_hash=? WHERE candidate_id=?",new Object[]{h,id});
                check(!ResearchPlatformStoreR929.authorize(store,consumer,id,h,h,"200",.30).authorized,consumer+"_OLD_ENTRY_CANNOT_BYPASS_MODEL_DEPENDENCIES");
                db.execSQL("UPDATE "+ResearchPlatformStoreR929.TABLE_CERTIFICATE+" SET model_hash=? WHERE candidate_id=?",new Object[]{CertificateDependenciesR932.modelHash(),id});
                db.execSQL("UPDATE "+ResearchPlatformStoreR929.TABLE_TRIAL+" SET status='RUNNING'");
                check(!ResearchPlatformStoreR929.authorizeCurrent(store,consumer,id,h,"200",.30).authorized,consumer+"_TRIAL_RECORD_MUST_REMAIN_FROZEN");
                db.execSQL("UPDATE "+ResearchPlatformStoreR929.TABLE_TRIAL+" SET status='COMPLETED'");
                check(ResearchPlatformStoreR929.revoke(store,cert.certificateHash,"FIXTURE_NEW_DEPENDENCY_AUDIT_REQUIRED"),consumer+"_REVOCATION_APPENDED");
                check(!ResearchPlatformStoreR929.putCertificate(store,cert,h),consumer+"_OLD_CERTIFICATE_CANNOT_RESURRECT");
                db.execSQL("UPDATE "+ResearchPlatformStoreR929.TABLE_CERTIFICATE+" SET selection_hash=? WHERE candidate_id=?",new Object[]{h,id});
                check(!ResearchPlatformStoreR929.authorizeCurrent(store,consumer,id,h,"200",.30).authorized,consumer+"_SELECTION_DRIFT_ZERO_WEIGHT");
            }
            db.execSQL("UPDATE "+ResearchPlatformStoreR929.TABLE_CERTIFICATE+" SET contract_revision='',all_gates_pass=1");
            check(!ResearchPlatformStoreR929.authorizeCurrent(store,CertificateBundleServiceR929.Consumer.INVENTED_ALGORITHM,"fixture_0",h,"200",.30).authorized,"LEGACY_ALL_GATES_FLAG_CANNOT_GRANT_AUTHORITY");
            System.out.println("R932_UNIFIED_CERTIFICATE_PASS|scope=SYNTHETIC_CERTIFICATES_REAL_SQLITE");
        }finally{db.shutdown();}
    }
}
