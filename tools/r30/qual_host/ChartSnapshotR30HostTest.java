package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.strategyv2.*;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import static com.ethk3.novaonepro.ResearchVisualDataR82.*;

/** Actual cached production reader and SQLite; seeded data tests plumbing, never natural quality. */
public final class ChartSnapshotR30HostTest {
    static void require(boolean ok,String reason){if(!ok)throw new AssertionError(reason);}
    static Map<String,Object> m(Object... pairs){Map<String,Object> out=new LinkedHashMap<>();for(int i=0;i<pairs.length;i+=2)out.put((String)pairs[i],pairs[i+1]);return out;}
    static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){require(c.moveToFirst(),sql);return c.getLong(0);}}
    static void execute(SQLiteDatabase db,ResearchWriteQueueR27.Batch batch)throws Exception{
        Method method=ResearchWriteQueueR27.class.getDeclaredMethod("execute",SQLiteDatabase.class,ResearchWriteQueueR27.Batch.class,String.class);method.setAccessible(true);
        db.jdbc.setAutoCommit(false);try{method.invoke(null,db,batch,"SYNTHETIC_CHART_HOST");db.jdbc.commit();}finally{db.jdbc.rollback();db.jdbc.setAutoCommit(true);}
    }
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");Path file=Files.createTempFile("r30-chart-host-",".db");
        try(Connection connection=DriverManager.getConnection("jdbc:sqlite:"+file)){
            SQLiteDatabase db=new SQLiteDatabase(connection);
            for(String ddl:SchemaR27.DDL)if(ddl.startsWith("CREATE TABLE"))db.execSQL(ddl);
            for(String ddl:RuntimeSchemaR26.DDL)if(ddl.startsWith("CREATE TABLE"))db.execSQL(ddl);
            db.execSQL("CREATE TABLE draws(issue TEXT PRIMARY KEY,dirty INT,n1 INT,n2 INT,n3 INT,n4 INT,n5 INT,n6 INT,n7 INT,n8 INT,n9 INT,n10 INT)");
            db.execSQL("INSERT INTO draws VALUES('202610310001',0,1,2,3,4,5,6,7,8,9,10)");
            Map<String,Object> holdout=m("taskKey","SYNTHETIC_READ_MAPPING_ONLY","datasetHash","synthetic-dataset","confirmationOutsideEvolution",true,"status","COMPLETED","verified",true,"pairedLcbVsPrevious",.02);
            db.execSQL("INSERT INTO r27_generation(generation,loop,dataset_hash,births,rejected,no_op,duplicates,survivors,retired,champion_hash,paired_lcb,verified,detail_json,started_at,finished_at) VALUES(1,'MAIN','synthetic-dataset',1,0,0,0,1,0,'synthetic-policy',.02,1,?,1,2)",new Object[]{JsonR27.write(m("holdout",holdout))});
            ReportR27.R30Input before=ReportSnapshotR30.capture(db);require(before.errors.isEmpty(),"CAPTURE:"+before.errors);
            Map<String,Object> packet=ReportChartsR30.packet(before);List<Map<String,Object>> chapters=ReportR29.fromSnapshotR30(before);
            Map<?,?> curve=(Map<?,?>)((List<?>)packet.get("panels")).get(0),coverage=(Map<?,?>)((List<?>)packet.get("panels")).get(1);
            require(((Number)curve.get("plottedGenerations")).intValue()==1,"ACTUAL_PAIRED_LOWER_NOT_CAPTURED");
            require(((Number)coverage.get("classifiedObjects")).intValue()==82&&coverage.get("validatedEffectiveCount")==null,"CODE_COVERAGE_FALSE_QUALITY");
            require(chapters.get(1).containsValue(curve)&&chapters.get(6).containsValue(coverage),"REPORT_CHARTS_NOT_SAME_CAPTURE");
            require(ReportR27.markdownR30(chapters).contains("p15_paired_improvement_r30")&&JsonR27.write(ReportR29.document(chapters)).contains("p16_capability_coverage_r30"),"JSON_MD_LOST_CHARTS");
            db.execSQL("INSERT INTO draws VALUES('202610310002',0,10,9,8,7,6,5,4,3,2,1)");
            db.execSQL("INSERT INTO r27_chart_cache VALUES('r30-charts','snapshot',?,1)",new Object[]{JsonR27.write(packet)});
            Map<String,Object> cached=ChartsRepositoryR27.snapshotR30(db,null);
            require("202610310001".equals(((Map<?,?>)cached.get("snapshotWatermark")).get("latestIssue")),"CACHED_FRAME_DRIFT");
            Panel paired=ChartsRepositoryR27.snapshotPanelR30(cached,0);require(paired.points.size()==1&&paired.points.get(0).value==.02&&Double.isNaN(paired.points.get(0).lower),"UI_INVENTED_INTERVAL_OR_LOST_LOWER");
            require(ChartsRepositoryR27.headlineSnapshotR30(cached).note.contains("202610310001"),"HEADLINE_REQUERIED_NEWER_DB");
            execute(db,ChartCacheR27.build(db,null));
            require(ChartCacheR27.PANELS.length==14,"LEGACY_PANELS_REMOVED");for(String id:ChartCacheR27.PANELS){try(Cursor c=db.rawQuery("SELECT payload_json FROM r27_chart_cache WHERE panel_id=? AND key='panel'",new String[]{id})){require(c.moveToFirst(),"MISSING_OLD_PANEL:"+id);require(JsonR27.parse(c.getString(0)) instanceof Map,"BAD_OLD_PANEL:"+id);}}
            require(n(db,"SELECT count(*) FROM r27_chart_cache WHERE panel_id='report' AND key IN ('ranking','action_surface')")==2,"RANKING_OR_ACTION_SURFACE_LOST");
            List<Panel> page=ChartsRepositoryR27.load(db,null);require(page.size()>=17,"PAGE_DROPPED_PANELS");require(page.get(15).points.size()==1&&page.get(16).points.size()==5,"NEW_PANELS_NOT_ON_REAL_PAGE");
            require(ChartsRepositoryR27.headlineSnapshotR30(ChartsRepositoryR27.snapshotR30(db,null)).note.contains("202610310002"),"NEXT_VIEW_NOT_ADVANCED");
            db.execSQL("UPDATE r27_chart_cache SET payload_json='broken' WHERE panel_id='r30-charts'");require("READ_ERROR".equals(ChartsRepositoryR27.snapshotPanelR30(ChartsRepositoryR27.snapshotR30(db,null),0).status),"BAD_PACKET_TREATED_AS_HEALTHY_ZERO");
            db.execSQL("DELETE FROM r27_chart_cache WHERE panel_id='r30-charts'");require("INSUFFICIENT".equals(ChartsRepositoryR27.snapshotPanelR30(ChartsRepositoryR27.snapshotR30(db,null),1).status),"MISSING_PACKET_TREATED_AS_ZERO");
            System.out.println("R30_CHART_SQLITE|ORIGINAL_14_KEYS_AND_RANKING_RETAINED|TWO_NEW_PANELS_ON_ACTUAL_PAGE|CAPTURED_PAIRED_LOWER_AND_82_CODE_OBJECTS|ONE_VIEW_JSON_MD_UI|OLD_FRAME_STABLE_NEXT_ADVANCES|BAD_AND_MISSING_NOT_ZERO|HOST_SYNTHETIC_NOT_DEVICE_OR_NATURAL|PASS");
        }finally{Files.deleteIfExists(file);Files.deleteIfExists(Path.of(file+"-wal"));Files.deleteIfExists(Path.of(file+"-shm"));}
    }
}
