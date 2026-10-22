package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** End-to-end software fixture: real CAL -> pause -> paid recovery -> 120/240 -> certificate.
 * Outcomes are deliberately synthetic and are NEVER a live-effect or device assertion.
 */
public final class ObservationRecoveryHarnessR932 {
    private static int checks;
    private static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);checks++;System.out.println(name+"=PASS");}
    private static String scalar(SQLiteDatabase db,String sql){return WindowPublicationHarnessR932.scalar(db,sql);}
    private static void drain(DbStore store){for(int i=0;i<32;i++)FixedResearchWindowR932.maintain(store);}
    private static void review(SQLiteDatabase db,String issue){db.beginTransaction();try{ResearchLifecycleR932.review(db,"algorithm:publication",issue);db.setTransactionSuccessful();}finally{db.endTransaction();}}
    private static String history(SQLiteDatabase db){
        StringBuilder b=new StringBuilder();
        for(String sql:new String[]{
            "SELECT * FROM "+FixedResearchWindowR932.WINDOW+" WHERE window_no<=3 ORDER BY window_no",
            "SELECT p.* FROM "+FixedResearchWindowR932.PAIR+" p JOIN "+FixedResearchWindowR932.WINDOW+" w ON w.window_id=p.window_id WHERE w.window_no<=3 ORDER BY w.window_no,p.target_issue",
            "SELECT l.* FROM "+ResearchMultiplicityR932.LOOK+" l JOIN "+FixedResearchWindowR932.WINDOW+" w ON w.window_id=l.window_id WHERE w.window_no<=3 ORDER BY w.window_no"}){
            try(Cursor c=db.rawQuery(sql,null)){while(c.moveToNext())for(int i=0;i<c.getColumnCount();i++)b.append(c.isNull(i)?"<NULL>":c.getString(i)).append('|');}
        }
        return V51ChecksumEngine.sha256(b.toString());
    }
    public static void main(String[] args)throws Exception{
        SQLiteDatabase db=new SQLiteDatabase(args[0]);
        try(ModelBundleRuntimeR932.Scope scope=ModelBundleRuntimeR932.open(null,"202609100001",System.currentTimeMillis())){
            DbStore store=new DbStore(db);GlobalResearchScheduleR932.schema(db);FixedResearchWindowR932.schema(db);FixedWindowCertificatePublisherR932.schema(db);
            db.execSQL("CREATE TABLE "+DbHelper.TABLE_DRAWS+"(issue TEXT PRIMARY KEY,n1 INTEGER,n2 INTEGER,n3 INTEGER,n4 INTEGER,n5 INTEGER,n6 INTEGER,n7 INTEGER,n8 INTEGER,n9 INTEGER,n10 INTEGER,dirty INTEGER)");
            String key="algorithm:publication",bundle=ModelBundleRuntimeR932.current().hash,selector=SelectionEvidenceContractR929.installedPolicy().hash,structure=V51ChecksumEngine.sha256("RECOVERY_FIXTURE_STRUCTURE");
            db.beginTransaction();try{
                ResearchPlatformStoreR929.activateRuntimeBundle(db,ModelBundleRuntimeR932.current(),"202609100001",1);
                ResearchMultiplicityR932.registerCandidate(db,key,"family:publication",structure,"202609100002");
                ResearchLifecycleR932.enroll(db,key,structure,"202609100002","B");db.setTransactionSuccessful();
            }finally{db.endTransaction();}
            String base="202609100001",issue="202609100002";
            for(int i=0;i<601;i++){
                WindowPublicationHarnessR932.freeze(db,base,issue,bundle,selector,structure);base=issue;issue=TargetIssueResolver.next(issue).targetIssue;
                if(i%8==7||i==240||i==360||i==600)drain(store);
            }
            for(int i=0;i<16;i++)WindowPublicationHarnessR932.advance(db,issue);
            check(Double.parseDouble(scalar(db,"SELECT maximum_weight FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE))==.05,"PRE_RECOVERY_REAL_PRODUCTION_PUBLISHER_HAS_360_SAMPLE_CERTIFICATE");
            String certificate=scalar(db,"SELECT certificate_hash FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE);
            String originalHypothesis=scalar(db,"SELECT hypothesis_hash FROM "+ResearchMultiplicityR932.ALIAS+" WHERE candidate_key='algorithm:publication'");
            // A real absent capture inside a pre-registered 240-issue window, not a forced FAIL.
            for(int i=0;i<240;i++){
                if(i==1)db.execSQL("INSERT INTO "+DbHelper.TABLE_DRAWS+" VALUES(?,1,2,3,4,5,6,7,8,9,10,0)",new Object[]{issue});
                else WindowPublicationHarnessR932.freeze(db,base,issue,bundle,selector,structure);
                base=issue;issue=TargetIssueResolver.next(issue).targetIssue;if(i%8==7)drain(store);
            }
            drain(store);review(db,base);
            check(scalar(db,"SELECT state FROM "+FixedResearchWindowR932.WINDOW+" WHERE window_no=3").equals("PAUSED_CAPTURE"),"MISSING_ACTUAL_CAPTURE_PAUSES_AT_ORIGINAL_FIXED_ENDPOINT");
            check(scalar(db,"SELECT state FROM "+ResearchLifecycleR932.TABLE+" WHERE candidate_key='algorithm:publication'").equals("PAUSED"),"PAUSE_PROJECTS_WITHOUT_REUSING_OLD_INFLUENCE_CERTIFICATE");
            String oldHistory=history(db),hypotheses=scalar(db,"SELECT COUNT(*) FROM "+ResearchMultiplicityR932.HYPOTHESIS);
            db.execSQL("CREATE TRIGGER fail_recovery_pair BEFORE INSERT ON "+FixedResearchWindowR932.PAIR+" WHEN EXISTS(SELECT 1 FROM "+FixedResearchWindowR932.WINDOW+" w WHERE w.window_id=NEW.window_id AND w.window_no=4) BEGIN SELECT RAISE(ABORT,'INJECTED_RECOVERY_PAIR_FAILURE'); END");
            boolean rejected=false;try{WindowPublicationHarnessR932.freeze(db,base,issue,bundle,selector,structure);}catch(IllegalStateException expected){rejected=true;}
            check(rejected&&scalar(db,"SELECT COUNT(*) FROM "+ObservationGenerationR932.RESTART).equals("0")&&scalar(db,"SELECT COUNT(*) FROM "+FixedResearchWindowR932.WINDOW).equals("3"),"FAILED_RECOVERY_PAIR_ROLLS_BACK_NEW_WINDOW_AND_GENERATION");
            check(hypotheses.equals(scalar(db,"SELECT COUNT(*) FROM "+ResearchMultiplicityR932.HYPOTHESIS))&&scalar(db,"SELECT state FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE).equals("ACTIVE")&&scalar(db,"SELECT state FROM "+ResearchLifecycleR932.TABLE+" WHERE candidate_key='algorithm:publication'").equals("PAUSED"),"RECOVERY_FAILURE_ROLLS_BACK_ALPHA_REVOCATION_AND_LIFECYCLE_TOGETHER");
            db.execSQL("DROP TRIGGER fail_recovery_pair");
            WindowPublicationHarnessR932.freeze(db,base,issue,bundle,selector,structure);base=issue;issue=TargetIssueResolver.next(issue).targetIssue;
            check(scalar(db,"SELECT COUNT(*) FROM "+ObservationGenerationR932.RESTART).equals("1")&&scalar(db,"SELECT start_window_no||':'||stage_no FROM "+ObservationGenerationR932.META+" WHERE start_window_no=4").equals("4:1"),"ACTUAL_PREDRAW_CAPTURE_REGISTERS_ONE_NEW_GENERATION");
            check(scalar(db,"SELECT state||':'||future_n||':'||paired_n FROM "+ResearchLifecycleR932.TABLE+" WHERE candidate_key='algorithm:publication'").equals("OBSERVING:0:0"),"RESUMED_GENERATION_STARTS_WITH_ZERO_SUPPORT_NOT_OLD_599");
            check(scalar(db,"SELECT state FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE).equals("REVOKED")&&scalar(db,"SELECT COUNT(*) FROM "+ResearchPlatformStoreR929.TABLE_REVOCATION+" WHERE certificate_hash='"+certificate+"'").equals("1"),"OLD_CERTIFICATE_REVOKED_BY_EXISTING_OWNER_AT_RECOVERY_COMMIT");
            check(Integer.parseInt(scalar(db,"SELECT COUNT(*) FROM "+ResearchMultiplicityR932.HYPOTHESIS))==Integer.parseInt(hypotheses)+1&&originalHypothesis.equals(scalar(db,"SELECT hypothesis_hash FROM "+ResearchMultiplicityR932.ALIAS+" WHERE candidate_key='algorithm:publication'")),"RECOVERY_PAYS_NEW_CLAIM_WITHOUT_RENAMING_OR_REFUNDING_ORIGINAL_HYPOTHESIS");
            check(scalar(db,"SELECT family_id FROM "+ObservationGenerationR932.RESTART).equals("family:publication")&&oldHistory.equals(history(db)),"SAME_FAMILY_AND_ALL_PRIOR_WINDOW_PAIR_LOOK_BYTES_RETAINED");
            for(int i=1;i<120;i++){
                WindowPublicationHarnessR932.freeze(db,base,issue,bundle,selector,structure);base=issue;issue=TargetIssueResolver.next(issue).targetIssue;if(i%8==7)drain(store);
            }
            drain(store);for(int i=0;i<7;i++)WindowPublicationHarnessR932.advance(db,issue);
            check(scalar(db,"SELECT state FROM "+FixedResearchWindowR932.WINDOW+" WHERE window_no=4").equals("PASS"),"NEW_GENERATION_120_ENDPOINT_PASSES_INDEPENDENTLY");
            check(Double.parseDouble(scalar(db,"SELECT maximum_weight FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE))==0,"OLD_360_PLUS_PARTIAL_239_CANNOT_INFLATE_NEW_120_CERTIFICATE");
            for(int i=0;i<240;i++){
                WindowPublicationHarnessR932.freeze(db,base,issue,bundle,selector,structure);base=issue;issue=TargetIssueResolver.next(issue).targetIssue;if(i%8==7)drain(store);
            }
            drain(store);for(int i=0;i<12;i++)WindowPublicationHarnessR932.advance(db,issue);review(db,base);
            check(scalar(db,"SELECT state FROM "+FixedResearchWindowR932.WINDOW+" WHERE window_no=5").equals("PASS")&&scalar(db,"SELECT stage_no FROM "+ObservationGenerationR932.META+" WHERE window_id=(SELECT window_id FROM "+FixedResearchWindowR932.WINDOW+" WHERE window_no=5)").equals("2"),"SERIAL_FIVE_IS_GENERATION_STAGE_TWO_NOT_OLD_STAGE_FIVE");
            check(scalar(db,"SELECT future_n FROM "+ResearchLifecycleR932.TABLE+" WHERE candidate_key='algorithm:publication'").equals("360"),"LIFECYCLE_SUPPORT_COUNTS_ONLY_CURRENT_GENERATION_360");
            try(ModelBundleRuntimeR932.Scope next=ModelBundleRuntimeR932.open(null,issue,System.currentTimeMillis()+1)){
                ResearchPlatformStoreR929.Authorization auth=ResearchPlatformStoreR929.authorizeCurrent(store,CertificateBundleServiceR929.Consumer.INVENTED_ALGORITHM,"publication",structure,issue,.30);
                check(auth.authorized&&auth.grantedWeight==.05&&!auth.mayCreateReal,"NEW_360_CERTIFICATE_REACHES_UNCHANGED_AUTHORIZER_WITHOUT_REAL_AUTHORITY");
            }
            check(oldHistory.equals(history(db)),"OLD_FAILURE_AND_DENOMINATORS_UNCHANGED_AFTER_FULL_RECOVERY_AND_REPUBLICATION");
            String cal=scalar(db,"SELECT model_hash FROM "+CandidateCalibrationR932.MODEL+" WHERE candidate_key='algorithm:publication'");
            rejected=false;db.beginTransaction();try{
                db.execSQL("UPDATE "+FixedResearchWindowR932.WINDOW+" SET state='FAIL_EFFECT' WHERE window_no=5");
                ObservationGenerationR932.recover(db,key,structure,bundle,selector,issue,cal);
            }catch(IllegalStateException expected){rejected=true;}finally{db.endTransaction();}
            check(rejected,"QUALITY_FAILURE_CANNOT_USE_SYSTEM_RECOVERY_TO_RESET_ITS_LOOKS");
            rejected=false;try{db.execSQL("DELETE FROM "+ObservationGenerationR932.RESTART);}catch(IllegalStateException expected){rejected=true;}
            check(rejected,"RECOVERY_GENERATION_AUDIT_CANNOT_BE_DELETED");
            rejected=false;try{db.execSQL("UPDATE "+ObservationGenerationR932.META+" SET stage_no=1 WHERE start_window_no=4");}catch(IllegalStateException expected){rejected=true;}
            check(rejected,"GENERATION_STAGE_IDENTITY_CANNOT_BE_REWRITTEN");
            check(Double.parseDouble(scalar(db,"SELECT SUM(alpha) FROM "+ResearchMultiplicityR932.FAMILY))<=.05,"PAID_RECOVERY_PRESERVES_GLOBAL_ALPHA_CEILING");
            System.out.println("R932_OBSERVATION_RECOVERY_PASS|checks="+checks+"|scope=ACTUAL_PRODUCTION_CAL_WINDOWS_RECOVERY_PUBLICATION_SQLITE_SYNTHETIC_1201_ISSUES|device=false|future_effect=false");
        }finally{db.shutdown();}
    }
}
