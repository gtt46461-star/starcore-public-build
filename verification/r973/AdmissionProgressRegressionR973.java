package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.StringWriter;
public final class AdmissionProgressRegressionR973 {
    static int n;
    static void ok(boolean value,String name){if(!value)throw new AssertionError(name);n++;System.out.println("PASS "+name);}
    static CertificateBundleServiceR929.Evaluation eval(int count,double rate,double brier,double ece,double coverage,boolean pit){
        return CertificateBundleServiceR929.evaluate(new CertificateBundleServiceR929.EvidenceInput(pit,true,true,count,count,2,rate,.30,coverage,brier,ece,.01,.04,true,true,V51ChecksumEngine.sha256("TEST_FIXTURE_NOT_PRODUCTION_EVIDENCE")));
    }
    static long count(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){c.moveToFirst();return c.getLong(0);}}
    public static void main(String[] args)throws Exception{
        ok(eval(360,.34,.24,.04,.15,true).weightCap>0,"INCLUSIVE_34_PERCENT_AND_QUALITY_BOUNDARIES");
        ok(eval(1000,.339,.20,.02,.9,true).weightCap==0,"339_OF_1000_NOT_ROUNDED_UP");
        ok(eval(0,.8,.20,.02,1,true).weightCap==0,"TRAINING_SUMMARY_WITHOUT_FUTURE_N_CANNOT_AUTHORIZE");
        ok(eval(359,.40,.20,.02,.9,true).weightCap==0,"INDEPENDENT_SAMPLE_MINIMUM_RETAINED");
        ok(eval(1000,.35,.241,.02,.9,true).weightCap==0,"BRIER_FAILURE_ZERO_AUTHORITY");
        ok(eval(1000,.35,.20,.041,.9,true).weightCap==0,"ECE_FAILURE_ZERO_AUTHORITY");
        ok(eval(1000,.35,.20,.02,.9,false).weightCap==0,"PIT_FAILURE_ZERO_AUTHORITY");
        ok(eval(1000,.35,.20,.02,1,true).weightCap>0,"100_PERCENT_EVIDENCE_COVERAGE_NOT_UPPER_CAPPED");
        ok(CertificateBundleServiceR929.validatedInfluenceCap(java.util.Collections.emptyList())==0,"EMPTY_GATES_NOT_CERTIFICATE");
        ok(!ResearchReadinessR924.missing(360,.34,.30,.24,.04,.15,0,0,8,360,.01,1).contains("未来样本="),"UI_SHARED_360_SAMPLE_BOUNDARY");
        ok(ResearchReadinessR924.missing(359,.34,.30,.24,.04,.15,0,0,8,359,.01,1).contains("未来样本="),"UI_MISSING_SAMPLE_REASON");
        SQLiteDatabase db=new SQLiteDatabase(args[0]);try{
            StringWriter absent=new StringWriter();ResearchStageProgressR973.write(db,absent);
            ok(absent.toString().contains("result=UNKNOWN"),"MISSING_SCHEMA_NOT_FAKE_ZERO");
            ok(count(db,"SELECT COUNT(*) FROM sqlite_master")==0,"DIAGNOSTICS_PERFORMS_NO_SCHEMA_WRITES");
            db.execSQL("CREATE TABLE r952_artifact(candidate_id TEXT,target_issue TEXT,artifact_hash TEXT,payload_hash TEXT,numeric_updates INTEGER)");
            db.execSQL("INSERT INTO r952_artifact VALUES('model-A','202609210123','real-receipt','payload',7)");
            long changes=count(db,"SELECT total_changes()");
            StringWriter present=new StringWriter();ResearchStageProgressR973.write(db,present);
            ok(present.toString().contains("candidate_id=model-A|target_issue=202609210123|artifact_hash=real-receipt"),"REPORT_BINDS_LAST_TRAIN_SUCCESS_TO_MODEL_AND_ISSUE");
            ok(count(db,"SELECT total_changes()")==changes,"REPORT_PERFORMS_NO_MUTATION");
            ok(present.toString().contains("NOT_ESTABLISHED"),"WORLD_REGISTRY_NOT_MISREPORTED_AS_END_TO_END_PROOF");
        }finally{db.shutdown();}
        System.out.println("R973_ADMISSION_PROGRESS_PASS checks="+n+"; fixture_certificates_only; REAL_ROUTING_NOT_TESTED; DEVICE_NOT_RUN");
    }
}
