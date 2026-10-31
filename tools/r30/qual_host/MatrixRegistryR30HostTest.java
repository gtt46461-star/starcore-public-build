package com.ethk3.novaonepro.strategyv2;

import android.database.sqlite.SQLiteDatabase;
import java.sql.*;
import java.util.*;

/** Registry reads original snapshot sources, including no schema, no samples and retained observations. */
public final class MatrixRegistryR30HostTest {
    static void require(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    @SuppressWarnings("unchecked")static Map<String,Object> row(Map<String,Object> report,String id){for(Object value:(List<?>)report.get("rows")){Map<String,Object> r=(Map<String,Object>)value;if(id.equals(r.get("id")))return r;}throw new AssertionError("MATRIX_ROW_MISSING:"+id);}
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");try(Connection c=DriverManager.getConnection("jdbc:sqlite::memory:")){
            SQLiteDatabase db=new SQLiteDatabase(c);Map<String,Object> missing=MatrixRegistryStoreR30.report(db);require(((List<?>)missing.get("rows")).size()==20,"MISSING_SCHEMA_LOST_CATALOG");
            require(row(missing,MatrixRegistryR30.SU2_ID).get("retainedSnapshotCount")==null,"MISSING_SOURCE_FABRICATED_ZERO");
            try(Statement s=c.createStatement()){
                s.execute("CREATE TABLE research_matrix_snapshot_r930(kind TEXT,target_issue TEXT,frozen_at INTEGER)");
                s.execute("CREATE TABLE su2_matrix_snapshot(checkpoint_sequence INTEGER,updated_at INTEGER)");
            }
            Map<String,Object> empty=MatrixRegistryStoreR30.report(db);require("TRACEABLE".equals(empty.get("status")),"BINDINGS_NOT_VERIFIED:"+JsonR27.write(empty));
            require(((Number)row(empty,MatrixRegistryR30.SU2_ID).get("retainedSnapshotCount")).longValue()==0,"EMPTY_SOURCE_NOT_MEASURED_ZERO");require("NOT_RUN".equals(row(empty,"R930:EVIDENCE_TEACHER").get("evidenceState")),"EMPTY_SOURCE_MARKED_EFFECTIVE");
            try(Statement s=c.createStatement()){
                s.execute("INSERT INTO research_matrix_snapshot_r930 VALUES('EVIDENCE_TEACHER','202610300101',1000),('EVIDENCE_TEACHER','202610300102',2000),('REVERSE_REASONING','202610300102',2000)");
                s.execute("INSERT INTO su2_matrix_snapshot VALUES(5000,3000),(5100,4000)");
            }
            Map<String,Object> actual=MatrixRegistryStoreR30.report(db),teacher=row(actual,"R930:EVIDENCE_TEACHER"),su2=row(actual,MatrixRegistryR30.SU2_ID);
            require(((Number)teacher.get("retainedSnapshotCount")).longValue()==2&&teacher.get("lastProducedIssue").equals("202610300102"),"NATIVE_SOURCE_WATERMARK_MISMATCH");
            require(((Number)su2.get("lastProducedCheckpoint")).longValue()==5100&&((Number)su2.get("lastProducedAt")).longValue()==4000,"SU2_SOURCE_WATERMARK_MISMATCH");
            require(su2.get("lastProducedIssue")==null,"CHECKPOINT_SEQUENCE_INVENTED_CALENDAR_ISSUE");
            require(teacher.get("qualityEffective")==null&&"RECORDED_UNVALIDATED".equals(teacher.get("evidenceState")),"STORED_SNAPSHOT_CALLED_QUALITY");
            require(((Number)actual.get("objectsWithRetainedSnapshots")).longValue()==3&&((Number)actual.get("verifiedBindings")).longValue()==20,"INVENTORY_COUNTS_WRONG");
            System.out.println("R30_MATRIX_SQLITE|20_BINDINGS_VERIFIED|19_NATIVE_AND_EXISTING_SU2_BUNDLE|MISSING_UNKNOWN_NOT_ZERO|EMPTY_NOT_RUN|ACTUAL_RETAINED_COUNTS_AND_WATERMARKS|NO_EFFICACY_CLAIM|PASS");
        }
    }
}
