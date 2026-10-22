package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.Collections;

/** Synthetic records through the production lifecycle, certificate evaluator and real SQLite. */
public final class LifecycleAuthorizationHarnessR932 {
    static String hash(String x){return V51ChecksumEngine.sha256(x);}
    static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);System.out.println(name+"=PASS");}
    static String scalar(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):"";}}
    static void review(DbStore store,SQLiteDatabase db,String key){
        db.beginTransaction();try{ResearchLifecycleR932.review(store,db,key,"202609100249");db.setTransactionSuccessful();}finally{db.endTransaction();}
    }
    static void observe(SQLiteDatabase db,String key){
        db.beginTransaction();try{ResearchLifecycleR932.transition(db,key,ResearchLifecycleR932.State.OBSERVING,"FIXTURE_RESEARCH_RESET","202609100249",hash("OBSERVATION"),0,0);db.setTransactionSuccessful();}finally{db.endTransaction();}
    }
    public static void main(String[] args)throws Exception{
        SQLiteDatabase db=new SQLiteDatabase(args[0]);
        try(ModelBundleRuntimeR932.Scope scope=ModelBundleRuntimeR932.open(null,"202609100250",System.currentTimeMillis())){
            DbStore store=new DbStore(db);GlobalResearchScheduleR932.schema(db);FixedResearchWindowR932.schema(db);FixedWindowCertificatePublisherR932.schema(db);ResearchPlatformStoreR929.createSchema(db);
            db.execSQL("CREATE TABLE "+DbHelper.TABLE_DRAWS+"(issue TEXT PRIMARY KEY,n1 INTEGER,n2 INTEGER,n3 INTEGER,n4 INTEGER,n5 INTEGER,n6 INTEGER,n7 INTEGER,n8 INTEGER,n9 INTEGER,n10 INTEGER,dirty INTEGER)");
            String key="algorithm:lifecycle-auth",structure=hash(key),bundle=ModelBundleRuntimeR932.current().hash,schema=V51ActionMetaFeatureEngine.featureManifestHash(),code=SourceManifestR815.SOURCE_TREE_SHA256,trial=hash("SYNTHETIC_LIFECYCLE_TRIAL"),window;
            db.execSQL("INSERT INTO "+ResearchPlatformStoreR929.TABLE_BUNDLE+" VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{bundle,"fixture","Binance1-PK10",schema,code,trial,"TEST_KEY","RSA",1,6,100,"202609100001","202609100002","ACTIVE","SYNTHETIC_NOT_PRODUCTION_MODEL",1,1,"",trial});
            db.execSQL("INSERT INTO "+ResearchPlatformStoreR929.TABLE_TRIAL+"(trial_id,identity_hash,stage,status,dataset_hash,schema_hash,source_hash,hyper_hash,family_id,structural_hash,split_hash,seed,registered_at,finished_at,record_hash) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{trial,trial,"EVALUATE","COMPLETED",trial,schema,code,trial,"LIFECYCLE_FIXTURE",structure,trial,1,1,1,trial});
            db.beginTransaction();try{
                ResearchMultiplicityR932.registerCandidate(db,key,"LIFECYCLE_FIXTURE",structure,"202609100001");
                ResearchLifecycleR932.enroll(db,key,structure,"202609100001","B");
                window=FixedResearchWindowR932.register(db,key,structure,bundle,SelectionEvidenceContractR929.installedPolicy().hash,"202609100001",120,1);
                db.setTransactionSuccessful();
            }finally{db.endTransaction();}
            db.execSQL("INSERT INTO "+DbHelper.TABLE_DRAWS+" VALUES('202609100249',1,2,3,4,5,6,7,8,9,10,0)");
            CertificateBundleServiceR929.CandidateKey identity=new CertificateBundleServiceR929.CandidateKey(CertificateBundleServiceR929.Consumer.INVENTED_ALGORITHM,"TEST","LIFECYCLE_FIXTURE","lifecycle-auth",structure);
            CertificateBundleServiceR929.Evaluation evaluation=CertificateBundleServiceR929.evaluate(UnifiedCertificateHarnessR932.input(360,2));
            CertificateBundleServiceR929.Certificate certificate=new CertificateBundleServiceR929.Certificate(identity,evaluation.level,CertificateBundleServiceR929.CertificateState.ACTIVE,bundle,schema,AtomCaptureR932.graphHash(store),CertificateDependenciesR932.modelHash(),CertificateDependenciesR932.component("POLICY"),CertificateDependenciesR932.component("CALIBRATION"),DomainRouterR929.installedSchemaHash(),trial,CertificateDependenciesR932.selectionHash(),code,"202609100002","202609110001",2,.05,evaluation.gates,"SYNTHETIC_LIFECYCLE_TEST_ONLY");
            check(ResearchPlatformStoreR929.putCertificate(store,certificate,trial),"LIFECYCLE_FIXTURE_USES_SHARED_CERTIFICATE_WRITER");
            // R954 closes the remaining R953 gap with certificates produced through the real
            // writer. Below-L6 stages are intentionally zero-weight, therefore all_gates_pass=0
            // is expected and must not hide legitimate research lifecycle progress.
            CertificateBundleServiceR929.EvidenceInput l4Input=new CertificateBundleServiceR929.EvidenceInput(true,true,true,120,120,0,.35,.31,.85,.22,.03,.01,.20,true,true,hash("R954_L4_WINDOW"));
            CertificateBundleServiceR929.Evaluation l4Eval=CertificateBundleServiceR929.evaluate(l4Input);
            check(l4Eval.level==CertificateBundleServiceR929.Level.L4_CHALLENGER,"R954_REAL_EVALUATOR_PRODUCES_L4_CHALLENGER");
            CertificateBundleServiceR929.Certificate l4Certificate=new CertificateBundleServiceR929.Certificate(identity,l4Eval.level,CertificateBundleServiceR929.CertificateState.ACTIVE,bundle,schema,AtomCaptureR932.graphHash(store),CertificateDependenciesR932.modelHash(),CertificateDependenciesR932.component("POLICY"),CertificateDependenciesR932.component("CALIBRATION"),DomainRouterR929.installedSchemaHash(),trial,CertificateDependenciesR932.selectionHash(),code,"202609100002","202609110001",3,0.0,l4Eval.gates,"SYNTHETIC_R954_L4_TEST_ONLY");
            check(ResearchPlatformStoreR929.putCertificate(store,l4Certificate,trial),"R954_REAL_L4_CERTIFICATE_PERSISTS");
            check(scalar(db,"SELECT all_gates_pass FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE).equals("0"),"R954_L4_ZERO_WEIGHT_NATURALLY_HAS_NO_INFLUENCE_GATE");
            ResearchPlatformStoreR929.CertificateStage challenger=ResearchPlatformStoreR929.currentStage(store,CertificateBundleServiceR929.Consumer.INVENTED_ALGORITHM,"lifecycle-auth",structure,"202609100250");
            check(challenger.valid&&challenger.is("L4_CHALLENGER"),"L4_ZERO_WEIGHT_CERTIFICATE_HAS_VISIBLE_SHARED_STAGE");
            check(!ResearchPlatformStoreR929.authorizeCurrent(store,CertificateBundleServiceR929.Consumer.INVENTED_ALGORITHM,"lifecycle-auth",structure,"202609100250",.05).authorized,"L4_ZERO_WEIGHT_CERTIFICATE_CANNOT_AUTHORIZE_INFLUENCE");
            review(store,db,key);
            check(scalar(db,"SELECT state FROM "+ResearchLifecycleR932.TABLE).equals("CHALLENGER"),"L4_SHARED_STAGE_PROJECTS_CHALLENGER_LIFECYCLE");
            // A newly changed certificate must not wait behind a large oldest-first lifecycle queue.
            long certUpdated=System.currentTimeMillis()-10L;
            db.execSQL("UPDATE "+ResearchPlatformStoreR929.TABLE_CERTIFICATE+" SET updated_at=?",new Object[]{certUpdated});
            db.execSQL("UPDATE "+ResearchLifecycleR932.TABLE+" SET state='OBSERVING',reason='R954_PRIORITY_FIXTURE',reviewed_at=? WHERE candidate_key=?",new Object[]{certUpdated-1L,key});
            for(int i=0;i<8;i++){
                String dummy="algorithm:r954-old-"+i, dummyStructure=hash(dummy);
                db.execSQL("INSERT OR REPLACE INTO "+ResearchLifecycleR932.TABLE+" VALUES(?,?, 'OBSERVING','R954_OLDER_QUEUE','',0,0,?,?,0)",new Object[]{dummy,dummyStructure,hash("R954_OLDER_"+i),"202609100001"});
            }
            check(ResearchLifecycleR932.maintain(store)>0,"R954_BOUNDED_LIFECYCLE_MAINTAIN_RUNS");
            check(scalar(db,"SELECT state FROM "+ResearchLifecycleR932.TABLE+" WHERE candidate_key='"+key+"'").equals("CHALLENGER"),"R954_NEW_CERTIFICATE_PRIORITY_BEATS_OLDER_REVIEW_QUEUE");

            CertificateBundleServiceR929.Evaluation l5Eval=CertificateBundleServiceR929.evaluate(UnifiedCertificateHarnessR932.input(240,1));
            check(l5Eval.level==CertificateBundleServiceR929.Level.L5_SHADOW,"R954_REAL_EVALUATOR_PRODUCES_L5_SHADOW");
            CertificateBundleServiceR929.Certificate l5Certificate=new CertificateBundleServiceR929.Certificate(identity,l5Eval.level,CertificateBundleServiceR929.CertificateState.ACTIVE,bundle,schema,AtomCaptureR932.graphHash(store),CertificateDependenciesR932.modelHash(),CertificateDependenciesR932.component("POLICY"),CertificateDependenciesR932.component("CALIBRATION"),DomainRouterR929.installedSchemaHash(),trial,CertificateDependenciesR932.selectionHash(),code,"202609100002","202609110001",4,0.0,l5Eval.gates,"SYNTHETIC_R954_L5_TEST_ONLY");
            check(ResearchPlatformStoreR929.putCertificate(store,l5Certificate,trial),"R954_REAL_L5_CERTIFICATE_PERSISTS");
            check(scalar(db,"SELECT all_gates_pass FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE).equals("0"),"R954_L5_ZERO_WEIGHT_NATURALLY_HAS_NO_INFLUENCE_GATE");
            ResearchPlatformStoreR929.CertificateStage shadow=ResearchPlatformStoreR929.currentStage(store,CertificateBundleServiceR929.Consumer.INVENTED_ALGORITHM,"lifecycle-auth",structure,"202609100250");
            check(shadow.valid&&shadow.is("L5_SHADOW"),"L5_ZERO_WEIGHT_CERTIFICATE_HAS_VISIBLE_SHARED_STAGE");
            check(!ResearchPlatformStoreR929.authorizeCurrent(store,CertificateBundleServiceR929.Consumer.INVENTED_ALGORITHM,"lifecycle-auth",structure,"202609100250",.05).authorized,"L5_ZERO_WEIGHT_CERTIFICATE_CANNOT_AUTHORIZE_INFLUENCE");
            review(store,db,key);
            check(scalar(db,"SELECT state FROM "+ResearchLifecycleR932.TABLE+" WHERE candidate_key='"+key+"'").equals("SHADOW"),"L5_SHARED_STAGE_PROJECTS_SHADOW_LIFECYCLE");

            // Do not solve the zero-weight problem by making stage reads permissive: mandatory
            // integrity observations remain fail-closed even for research-only L4/L5 visibility.
            db.execSQL("UPDATE "+ResearchPlatformStoreR929.TABLE_CERT_GATE+" SET pass=0 WHERE certificate_hash=? AND gate_id='PIT'",new Object[]{l5Certificate.certificateHash});
            ResearchPlatformStoreR929.CertificateStage failed=ResearchPlatformStoreR929.currentStage(store,CertificateBundleServiceR929.Consumer.INVENTED_ALGORITHM,"lifecycle-auth",structure,"202609100250");
            check(!failed.valid&&failed.reason.equals("CERTIFICATE_RESEARCH_STAGE_GATE_FAILED"),"FAILED_HARD_GATE_CERTIFICATE_CANNOT_PROJECT_STAGE");
            review(store,db,key);
            check(scalar(db,"SELECT state FROM "+ResearchLifecycleR932.TABLE+" WHERE candidate_key='"+key+"'").equals("OBSERVING"),"FAILED_HARD_GATE_CERTIFICATE_DROPS_BACK_TO_OBSERVING");
            db.execSQL("UPDATE "+ResearchPlatformStoreR929.TABLE_CERT_GATE+" SET pass=1 WHERE certificate_hash=? AND gate_id='PIT'",new Object[]{l5Certificate.certificateHash});

            CertificateBundleServiceR929.Evaluation l6Eval=CertificateBundleServiceR929.evaluate(UnifiedCertificateHarnessR932.input(360,2));
            CertificateBundleServiceR929.Certificate l6Certificate=new CertificateBundleServiceR929.Certificate(identity,l6Eval.level,CertificateBundleServiceR929.CertificateState.ACTIVE,bundle,schema,AtomCaptureR932.graphHash(store),CertificateDependenciesR932.modelHash(),CertificateDependenciesR932.component("POLICY"),CertificateDependenciesR932.component("CALIBRATION"),DomainRouterR929.installedSchemaHash(),trial,CertificateDependenciesR932.selectionHash(),code,"202609100002","202609110001",5,.05,l6Eval.gates,"SYNTHETIC_R954_L6_TEST_ONLY");
            check(ResearchPlatformStoreR929.putCertificate(store,l6Certificate,trial),"R954_REAL_L6_CERTIFICATE_RESTORES_INFLUENCE_AUTHORITY");
            check(scalar(db,"SELECT all_gates_pass FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE).equals("1"),"R954_L6_REQUIRES_ALL_INFLUENCE_GATES");
            check(ResearchPlatformStoreR929.authorizeCurrent(store,CertificateBundleServiceR929.Consumer.INVENTED_ALGORITHM,"lifecycle-auth",structure,"202609100250",.05).authorized,"VALID_CURRENT_CERTIFICATE_STARTS_AUTHORIZED");
            db.execSQL("UPDATE "+FixedResearchWindowR932.WINDOW+" SET state='PAUSED_CAPTURE',reason='SYNTHETIC_SYSTEM_MISSING' WHERE window_id=?",new Object[]{window});
            review(store,db,key);
            check(scalar(db,"SELECT state FROM "+ResearchLifecycleR932.TABLE).equals("PAUSED"),"OLD_ACTIVE_CERTIFICATE_CANNOT_OVERWRITE_SYSTEM_PAUSE");
            check(!ResearchPlatformStoreR929.authorizeCurrent(store,CertificateBundleServiceR929.Consumer.INVENTED_ALGORITHM,"lifecycle-auth",structure,"202609100250",.05).authorized,"PAUSED_PROJECTION_AND_PRODUCTION_AUTHORIZATION_AGREE");
            check(scalar(db,"SELECT first_prune_reason FROM "+ResearchLifecycleR932.TABLE).isEmpty(),"SYSTEM_PAUSE_DOES_NOT_INVENT_QUALITY_FAILURE");
            String events=scalar(db,"SELECT COUNT(*) FROM "+ResearchLifecycleR932.EVENT);review(store,db,key);
            check(events.equals(scalar(db,"SELECT COUNT(*) FROM "+ResearchLifecycleR932.EVENT)),"REPEATED_REVIEW_DOES_NOT_DUPLICATE_PAUSE_TRANSITION");
            db.execSQL("UPDATE "+FixedResearchWindowR932.WINDOW+" SET state='OPEN',reason='' WHERE window_id=?",new Object[]{window});observe(db,key);
            db.execSQL("UPDATE "+ResearchPlatformStoreR929.TABLE_CERTIFICATE+" SET model_hash=?",new Object[]{hash("STALE_MODEL")});review(store,db,key);
            check(scalar(db,"SELECT state FROM "+ResearchLifecycleR932.TABLE).equals("OBSERVING"),"STALE_MODEL_CERTIFICATE_CANNOT_PROJECT_INFLUENCE");
            db.execSQL("UPDATE "+ResearchPlatformStoreR929.TABLE_CERTIFICATE+" SET model_hash=?",new Object[]{CertificateDependenciesR932.modelHash()});review(store,db,key);
            check(scalar(db,"SELECT state FROM "+ResearchLifecycleR932.TABLE).equals("INFLUENCE"),"CURRENT_SHARED_AUTHORITY_PROJECTS_CONTROLLED_INFLUENCE");
            db.execSQL("INSERT INTO "+ResearchCohortR932.CHAMPION+" VALUES(?,?,?,?,?,?)",new Object[]{"LIFECYCLE_FIXTURE",key,structure,l6Certificate.certificateHash,trial,"202609100200"});review(store,db,key);
            check(scalar(db,"SELECT state FROM "+ResearchLifecycleR932.TABLE).equals("CHAMPION"),"CURRENT_FAMILY_INCUMBENT_PROJECTION_IS_RETAINED");
            db.execSQL("UPDATE "+ResearchPlatformStoreR929.TABLE_CERTIFICATE+" SET valid_until_issue='202609100249'");review(store,db,key);
            check(scalar(db,"SELECT state FROM "+ResearchLifecycleR932.TABLE).equals("OBSERVING"),"EXPIRED_CERTIFICATE_DOES_NOT_SURVIVE_LIFECYCLE_REVIEW");
            boolean rejected=false;try{ResearchLifecycleR932.transition(db,key,ResearchLifecycleR932.State.PAUSED,"OUTSIDE_TRANSACTION","202609100250",hash("BAD"),0,0);}catch(IllegalStateException expected){rejected=true;}
            check(rejected,"LIFECYCLE_TRANSITION_REQUIRES_EXISTING_OWNER_TRANSACTION");
            db.execSQL("CREATE TRIGGER reject_lifecycle_update BEFORE UPDATE ON "+ResearchLifecycleR932.TABLE+" WHEN NEW.state='PAUSED' BEGIN SELECT RAISE(IGNORE); END");
            events=scalar(db,"SELECT COUNT(*) FROM "+ResearchLifecycleR932.EVENT);rejected=false;
            db.beginTransaction();try{ResearchLifecycleR932.transition(db,key,ResearchLifecycleR932.State.PAUSED,"FAULT_INJECTION","202609100250",hash("FAULT"),0,0);db.setTransactionSuccessful();}catch(IllegalStateException expected){rejected=true;}finally{db.endTransaction();}
            check(rejected&&events.equals(scalar(db,"SELECT COUNT(*) FROM "+ResearchLifecycleR932.EVENT))&&scalar(db,"SELECT state FROM "+ResearchLifecycleR932.TABLE).equals("OBSERVING"),"CAS_ZERO_ROWS_ROLLS_BACK_EVENT_AND_STATE");
            db.execSQL("DROP TRIGGER reject_lifecycle_update");
            rejected=false;db.beginTransaction();try{ResearchLifecycleR932.transition(db,key,ResearchLifecycleR932.State.PAUSED,"INVALID_SUPPORT","202609100250",hash("BAD_SUPPORT"),1,2);}catch(IllegalArgumentException expected){rejected=true;}finally{db.endTransaction();}
            check(rejected,"PAIRED_SUPPORT_CANNOT_EXCEED_FUTURE_SUPPORT");
            db.execSQL("INSERT INTO "+ResearchLifecycleR932.ELITE+" VALUES(?,?,30,.4,.2,.03,.01,'TEST',?,1)",new Object[]{key,structure,trial});
            db.beginTransaction();try{ResearchLifecycleR932.transition(db,key,ResearchLifecycleR932.State.QUARANTINED,"SYNTHETIC_PIT_FAILURE","202609100250",hash("PIT_FAILURE"),30,30);db.setTransactionSuccessful();}finally{db.endTransaction();}
            check(scalar(db,"SELECT COUNT(*) FROM "+ResearchLifecycleR932.ELITE).equals("0"),"QUARANTINED_PARENT_LEAVES_ELITE_IN_THE_SAME_TRANSACTION");
            check(!ResearchPlatformStoreR929.authorizeCurrent(store,CertificateBundleServiceR929.Consumer.INVENTED_ALGORITHM,"lifecycle-auth",structure,"202609100250",.05).authorized,"QUARANTINED_RESEARCH_CANNOT_RESTORE_INFLUENCE");
            System.out.println("R932_LIFECYCLE_AUTHORIZATION_PASS|scope=SYNTHETIC_REAL_SQLITE_NOT_REAL_FUTURE_OR_DEVICE");
        }finally{db.shutdown();}
    }
}
