package com.ethk3.novaonepro.strategyv2;
import android.database.sqlite.SQLiteDatabase;
import java.sql.*;
import java.util.*;
/** Uses original DDL and production read adapters. Synthetic source rows are not natural Q06 evidence. */
public final class UpgradeEvidenceR30HostTest {
    static void require(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
    static Map<?,?> parse(String[] report){require(report[0].contains("INSUFFICIENT")||report[0].contains("UNKNOWN"),"MARKDOWN_HAS_NO_EVIDENCE_STATE");return(Map<?,?>)JsonR27.parse(report[1]);}
    static Map<?,?> section(List<Map<String,Object>> report,int chapter,String parent,String source){return(Map<?,?>)((Map<?,?>)report.get(chapter).get(parent)).get(source);}
    public static void main(String[] args)throws Exception {
        Class.forName("org.sqlite.JDBC");try(Connection conn=DriverManager.getConnection("jdbc:sqlite::memory:")){
            SQLiteDatabase db=new SQLiteDatabase(conn);
            Map<?,?> missing=parse(DiagnosisStoreSU2.report(db));require("UNKNOWN".equals(missing.get("status"))&&missing.get("records")==null,"MISSING_TABLE_REPORTED_AS_ZERO");
            for(String ddl:SchemaR27.DDL)if(ddl.startsWith("CREATE TABLE"))db.execSQL(ddl);
            for(String ddl:StrategySchemaSU2.DDL)if(ddl.startsWith("CREATE TABLE"))db.execSQL(ddl);
            Map<?,?> empty=parse(DiagnosisStoreSU2.report(db));require("INSUFFICIENT".equals(empty.get("status"))&&Long.valueOf(0).equals(empty.get("records")),"EMPTY_DIAGNOSIS_LOST_REASON_OR_ZERO");
            List<Map<String,Object>> report=ReportR29.chapters(db);
            require("INSUFFICIENT".equals(section(report,8,"upgradeDiagnosisSources","snapshots").get("status")),"EMPTY_UPGRADE_CHAPTER_HAS_NO_STATE");
            require("INSUFFICIENT".equals(section(report,9,"ablationEvidence","policy").get("status")),"EMPTY_R27_ABLATION_HAS_NO_STATE");
            require("UNKNOWN".equals(section(report,9,"ablationEvidence","model").get("status")),"MISSING_SU1_ABLATION_FALSELY_EMPTY");
            require(ReportR27.summaryR30(report).contains("su1_model_ablation · UNKNOWN"),"UI_LOST_MISSING_ABLATION_REASON");
            for(String ddl:SqlStatementsSU1.SQL)if(ddl.startsWith("CREATE TABLE"))db.execSQL(ddl);
            String snapshot="SU2DIAG\t1\ndataset\tHOST_SYNTHETIC\nseq\t2500\n";
            db.execSQL("INSERT INTO su2_diag_snapshot VALUES(2500,?,1)",new Object[]{snapshot});db.execSQL("INSERT INTO su2_diag_state VALUES(1,?,2500,1)",new Object[]{new UpgradeDiagnosisSU2.Ledger().text()});
            db.execSQL("INSERT INTO r27_ablation VALUES(1,'original-champion',0,1,'original-branch',.001,.002)");
            String hash=String.join("",Collections.nCopies(64,"a"));db.execSQL("INSERT INTO su1_model_ablation(source_campaign,dataset_blob,kernel_hash,generation,phase,checkpoint_hash,frozen_hash,report_hash,updated_at) VALUES('HOST_SYNTHETIC',?,?,1,'DONE',?,?,?,1)",new Object[]{hash,hash,hash,hash,hash});
            report=ReportR29.chapters(db);Map<?,?> policy=section(report,9,"ablationEvidence","policy"),model=section(report,9,"ablationEvidence","model");
            require("RECORDED_UNVALIDATED".equals(policy.get("status"))&&"RECORDED_UNVALIDATED".equals(model.get("status")),"STORED_DATA_HIDDEN_OR_FALSELY_QUALIFIED");
            Map<?,?> original=(Map<?,?>)((List<?>)policy.get("rows")).get(0);require(Long.valueOf(1).equals(original.get("removed_child"))&&"original-branch".equals(original.get("branch")),"ORIGINAL_ABLATION_FIELDS_LOST");
            Map<?,?> task=(Map<?,?>)((List<?>)model.get("rows")).get(0);require(hash.equals(task.get("report_hash"))&&hash.equals(task.get("checkpoint_hash"))&&"DONE".equals(task.get("phase")),"ORIGINAL_MODEL_TASK_REFERENCES_LOST");
            require(model.get("qualityEffective")==null&&policy.get("pairedImprovementLower95")==null,"TASK_PRESENCE_PROMOTED_TO_Q06");
            Map<?,?> upgrade=parse(DiagnosisStoreSU2.report(db));require(upgrade.get("r30Sections") instanceof List&&"HOST_SYNTHETIC".equals(upgrade.get("dataset")),"ACTUAL_UPGRADE_ADAPTER_STILL_LEGACY_OR_DROPPED_DATASET");
            Object watermark=report.get(0).get("snapshotWatermark");for(Map<?,?> chapter:report)require(chapter.get("snapshotWatermark")==watermark,"SOURCE_EVIDENCE_REQUERIED_OTHER_VIEW");
            System.out.println("R30_UPGRADE_ABLATION_SQLITE|MISSING_UNKNOWN_EMPTY_INSUFFICIENT|ACTUAL_UPGRADE_JSON_MD_ADAPTER|R27_ORIGINAL_BRANCH_AND_REMOVED_CHILD|SU1_ORIGINAL_REPORT_CHECKPOINT_HASH|SAME_VIEW|NO_SYNTHETIC_Q06_CLAIM|PASS");
        }
    }
}
