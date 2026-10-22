package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** Synthetic issue stream through actual CAL, windows, execution ledger and certificate publisher. */
public final class WindowPublicationHarnessR932 {
    static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);System.out.println(label+"=PASS");}
    static String scalar(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):"";}}
    static CandidateExecutionAuditR932.Proof proof(){
        CandidateExecutionAuditR932.Builder a=new CandidateExecutionAuditR932.Builder();
        for(int i=0;i<1200;i++)a.add(i,1200-i,1200-i,i);
        return a.finish(0,"FIXTURE_PROGRAM","FIXTURE_PROGRAM","FIXTURE_STATE","FIXTURE_STATE","FIXTURE_STATE");
    }
    static void advance(SQLiteDatabase db,String next){db.beginTransaction();try{FixedWindowCertificatePublisherR932.advance(db,next);db.setTransactionSuccessful();}finally{db.endTransaction();}}
    static void freeze(SQLiteDatabase db,String base,String issue,String bundle,String selector,String structure){
        CandidateExecutionAuditR932.Proof p=proof();String prediction=V51ChecksumEngine.sha256("FIXTURE|"+issue);
        db.beginTransaction();try{
            db.execSQL("INSERT INTO "+FixedResearchWindowR932.CHAMPION+" VALUES(?,?,?,?,?,?,?,?,?,?)",new Object[]{issue,bundle,bundle,selector,1,2,3,4,V51ChecksumEngine.sha256(issue+"CHAMPION"),1});
            p.freeze(db,"algorithm:publication",structure,V51ChecksumEngine.sha256("INPUT|"+issue),base,issue,prediction);
            ResearchCaptureLedgerR932.record(db,issue,"algorithm:publication","FROZEN","SYNTHETIC_SERIALIZATION_FIXTURE",prediction,Math.max(1,p.elapsedNs/1000000),true);
            FixedResearchWindowR932.freezePair(db,"algorithm:publication",structure,issue,prediction,1,1,2,3,1.0);
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
        db.execSQL("INSERT INTO "+DbHelper.TABLE_DRAWS+" VALUES(?,1,2,3,4,5,6,7,8,9,10,0)",new Object[]{issue});
    }
    public static void main(String[] args)throws Exception{
        SQLiteDatabase db=new SQLiteDatabase(args[0]);
        try(ModelBundleRuntimeR932.Scope scope=ModelBundleRuntimeR932.open(null,"202609100603",System.currentTimeMillis())){
            DbStore store=new DbStore(db);FixedResearchWindowR932.schema(db);FixedWindowCertificatePublisherR932.schema(db);
            db.execSQL("CREATE TABLE "+DbHelper.TABLE_DRAWS+"(issue TEXT PRIMARY KEY,n1 INTEGER,n2 INTEGER,n3 INTEGER,n4 INTEGER,n5 INTEGER,n6 INTEGER,n7 INTEGER,n8 INTEGER,n9 INTEGER,n10 INTEGER,dirty INTEGER)");
            String bundle=ModelBundleRuntimeR932.current().hash,selector=SelectionEvidenceContractR929.installedPolicy().hash,structure=V51ChecksumEngine.sha256("PUBLICATION_FIXTURE_STRUCTURE");
            db.beginTransaction();try{
                ResearchPlatformStoreR929.activateRuntimeBundle(db,ModelBundleRuntimeR932.current(),"202609100001",1);
                ResearchMultiplicityR932.registerCandidate(db,"algorithm:publication","family:publication",structure,"202609100002");
                db.setTransactionSuccessful();
            }finally{db.endTransaction();}
            String base="202609100001",issue="202609100002";
            for(int i=0;i<601;i++){
                freeze(db,base,issue,bundle,selector,structure);base=issue;issue=TargetIssueResolver.next(issue).targetIssue;
                if(i%8==7||i==240||i==360||i==600){for(int j=0;j<16;j++)FixedResearchWindowR932.maintain(store);}
            }
            check(scalar(db,"SELECT COUNT(*) FROM "+FixedResearchWindowR932.WINDOW+" WHERE state='PASS'").equals("2"),"REAL_CAL_AND_TWO_FIXED_WINDOWS_REACH_TERMINAL_PASS");
            String first=scalar(db,"SELECT first_issue FROM "+FixedResearchWindowR932.WINDOW+" WHERE window_no=1");
            // Missing full execution and absent opportunity must block even when statistical metrics pass.
            db.beginTransaction();try{
                db.execSQL("DELETE FROM "+CandidateExecutionAuditR932.TABLE+" WHERE target_issue=?",new Object[]{first});
                FixedWindowCertificatePublisherR932.advance(db,issue);
                check(scalar(db,"SELECT state FROM "+FixedWindowCertificatePublisherR932.TABLE).equals("BLOCKED"),"STATISTICAL_PASS_WITHOUT_PREDRAW_EXECUTION_CANNOT_PUBLISH");
            }finally{db.endTransaction();}
            db.beginTransaction();try{
                db.execSQL("DELETE FROM "+ResearchCaptureLedgerR932.TABLE+" WHERE target_issue=?",new Object[]{first});
                FixedWindowCertificatePublisherR932.advance(db,issue);
                check(scalar(db,"SELECT state FROM "+FixedWindowCertificatePublisherR932.TABLE).equals("BLOCKED"),"MISSING_CAPTURE_TERMINAL_IS_BLOCKED");
            }finally{db.endTransaction();}
            db.beginTransaction();try{
                db.execSQL("UPDATE "+CandidateExecutionAuditR932.TABLE+" SET payload='{}' WHERE target_issue=?",new Object[]{first});
                FixedWindowCertificatePublisherR932.advance(db,issue);
                check(scalar(db,"SELECT reason FROM "+FixedWindowCertificatePublisherR932.TABLE).equals("EXECUTION_PROOF_HASH_MISMATCH"),"TAMPERED_EXECUTION_PAYLOAD_REJECTED");
            }finally{db.endTransaction();}
            db.beginTransaction();try{
                FixedWindowCertificatePublisherR932.advance(db,issue);
                check(scalar(db,"SELECT verified_n FROM "+FixedWindowCertificatePublisherR932.TABLE).equals("24"),"AUDIT_PAGE_BOUNDED_TO_24");
            }finally{db.endTransaction();}
            check(scalar(db,"SELECT COUNT(*) FROM "+FixedWindowCertificatePublisherR932.TABLE).equals("0"),"INTERRUPTED_AUDIT_ROLLS_BACK_CURSOR_AND_PROOF_CHAIN");
            for(int i=0;i<4;i++)advance(db,issue);
            db.execSQL("CREATE TRIGGER fail_publication BEFORE INSERT ON "+ResearchPlatformStoreR929.TABLE_CERT_HISTORY+" BEGIN SELECT RAISE(ABORT,'INJECTED_CERTIFICATE_FAILURE'); END");
            boolean rejected=false;try{advance(db,issue);}catch(IllegalStateException expected){rejected=true;}
            check(rejected&&scalar(db,"SELECT verified_n FROM "+FixedWindowCertificatePublisherR932.TABLE).equals("96")&&scalar(db,"SELECT COUNT(*) FROM "+ResearchPlatformStoreR929.TABLE_TRIAL).equals("0"),"CERTIFICATE_FAILURE_ROLLS_BACK_TRIAL_AND_LAST_AUDIT_PAGE");
            db.execSQL("DROP TRIGGER fail_publication");advance(db,issue);
            check(scalar(db,"SELECT maximum_weight FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE).equals("0.0"),"FIRST_120_WINDOW_PUBLISHES_L5_WITH_ZERO_WEIGHT");
            check(scalar(db,"SELECT all_gates_pass FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE).equals("0"),"R954_REAL_PUBLISHER_L5_NATURALLY_HAS_ZERO_INFLUENCE_GATE");
            try(ModelBundleRuntimeR932.Scope shadowRequest=ModelBundleRuntimeR932.open(null,issue,System.currentTimeMillis()+1)){
                ResearchPlatformStoreR929.CertificateStage shadow=ResearchPlatformStoreR929.currentStage(store,CertificateBundleServiceR929.Consumer.INVENTED_ALGORITHM,"publication",structure,issue);
                check(shadow.valid&&shadow.is("L5_SHADOW"),"R954_REAL_PUBLISHER_L5_REACHES_VISIBLE_SHADOW_STAGE");
                check(!ResearchPlatformStoreR929.authorizeCurrent(store,CertificateBundleServiceR929.Consumer.INVENTED_ALGORITHM,"publication",structure,issue,.05).authorized,"R954_REAL_PUBLISHER_L5_STILL_CANNOT_INFLUENCE_MAINLINE");
            }
            for(int i=0;i<10;i++)advance(db,issue);
            check(scalar(db,"SELECT COUNT(*) FROM "+FixedWindowCertificatePublisherR932.TABLE+" WHERE state='PUBLISHED'").equals("2"),"BOTH_INDEPENDENT_WINDOWS_PUBLISH_FROM_FROZEN_EXECUTION");
            check(Double.parseDouble(scalar(db,"SELECT maximum_weight FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE))==.05,"360_PAIRED_FUTURE_SAMPLES_PUBLISH_FIVE_PERCENT_CAP");
            // A new request sees the just-issued certificate; an already captured request must not.
            try(ModelBundleRuntimeR932.Scope later=ModelBundleRuntimeR932.open(null,issue,System.currentTimeMillis()+1)){
                ResearchPlatformStoreR929.Authorization auth=ResearchPlatformStoreR929.authorizeCurrent(store,CertificateBundleServiceR929.Consumer.INVENTED_ALGORITHM,"publication",structure,issue,.30);
                check(auth.authorized&&auth.grantedWeight==.05&&!auth.mayCreateReal,"PUBLISHED_WINDOW_CERTIFICATE_REACHES_EXISTING_AUTHORIZER");
            }
            String count=scalar(db,"SELECT COUNT(*) FROM "+ResearchPlatformStoreR929.TABLE_CERT_HISTORY);advance(db,issue);
            check(count.equals(scalar(db,"SELECT COUNT(*) FROM "+ResearchPlatformStoreR929.TABLE_CERT_HISTORY)),"COMPLETED_PUBLICATION_IS_IDEMPOTENT");
            rejected=false;db.beginTransaction();try{proof().freeze(db,"algorithm:publication",structure,V51ChecksumEngine.sha256("INPUT"),"202609100001","202609100002",V51ChecksumEngine.sha256("PREDICTION"));}catch(IllegalStateException expected){rejected=true;}finally{db.endTransaction();}
            check(rejected,"CANNOT_BACKFILL_EXECUTION_AFTER_DRAW");
            rejected=false;try{CandidateExecutionAuditR932.Builder bad=new CandidateExecutionAuditR932.Builder();bad.add(0,1,2,0);}catch(IllegalStateException expected){rejected=true;}
            check(rejected,"ACTUAL_SERIALIZATION_PARITY_FAILURE_REJECTED");
            System.out.println("R932_WINDOW_PUBLICATION_PASS|scope=SYNTHETIC_REAL_SQLITE_601_ISSUES_NOT_DEVICE_OR_REAL_FUTURE");
        }finally{db.shutdown();}
    }
}
