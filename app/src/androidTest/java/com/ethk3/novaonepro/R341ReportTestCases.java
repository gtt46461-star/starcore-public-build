package com.ethk3.novaonepro;

import static org.junit.Assert.*;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.strategyv2.RepairEffectLedgerR341;
import com.ethk3.novaonepro.strategyv2.ResearchTableAdaptersR30R1;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class R341ReportTestCases {
    public static void missingSourcesReachOverview(Context app) {
        List<Map<String,Object>> rows=new ArrayList<>();
        for(int i=0;i<3;i++)rows.add(ReportSourceStateR341.observation("SOURCE"+i,"snapshot",
                "READ_ERROR","no such table: repair_fixture_"+i,null,null));
        rows.add(ReportSourceStateR341.observation("actual-zero","snapshot","READ_OK","",1,1L));
        Map<String,Object> merged=ReportSourceStateR341.merge(rows);
        assertEquals(3,merged.get("unavailable"));
        assertEquals(1,merged.get("ok"));
        assertEquals("UNKNOWN_SOURCE_UNAVAILABLE",merged.get("health"));
        assertNull(rows.get(0).get("totalRows"));
    }

    public static void sourceReasonsStayDistinct(Context app) {
        assertEquals("TIMED_OUT",ReportSourceStateR341.reason("read timeout"));
        assertEquals("PERMISSION_DENIED",ReportSourceStateR341.reason("permission denied"));
        assertEquals("PARTIAL",ReportSourceStateR341.reason("truncated"));
        assertEquals("OBSERVED_EMPTY",ReportSourceStateR341.reason("OBSERVED_EMPTY"));
        assertEquals("",ReportSourceStateR341.reason("READ_OK"));
    }

    public static void coverageUsesSameSqlWindow(Context app) {
        try(SQLiteDatabase db=SQLiteDatabase.create(null)){
            db.execSQL("CREATE TABLE "+DbHelper.TABLE_DRAWS+"(issue TEXT PRIMARY KEY,dirty INTEGER)");
            db.execSQL("CREATE TABLE "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+"(target_issue TEXT PRIMARY KEY,pre_draw_frozen INTEGER,settled INTEGER)");
            db.execSQL("CREATE TABLE "+DbHelper.TABLE_R35_OOS_LABEL+"(target_issue TEXT PRIMARY KEY,strict_oos INTEGER)");
            db.execSQL("CREATE TABLE "+DbHelper.TABLE_R36_SETTLEMENT_TASK+"(target_issue TEXT,status TEXT)");
            db.execSQL("INSERT INTO "+DbHelper.TABLE_DRAWS+" VALUES('202611240002',0),('202611240003',0),('202611240004',1)");
            db.execSQL("INSERT INTO "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+" VALUES('202611240001',1,1),('202611240002',1,1),('202611240003',1,0),('202611240004',1,1)");
            db.execSQL("INSERT INTO "+DbHelper.TABLE_R35_OOS_LABEL+" VALUES('202611240001',1),('202611240002',1),('202611240004',1)");
            db.execSQL("INSERT INTO "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" VALUES('202611240001','COMPLETED'),('202611240002','COMPLETED'),('202611240003','PENDING')");
            SettlementCoverageTruthR906 truth=RuntimeTruthSnapshotR906.coverage(db);
            assertEquals("READ_OK",truth.sourceStatus);assertEquals(2,truth.predictedEligible);assertEquals(1,truth.settledPredictions);
            assertEquals(Integer.valueOf(2),truth.historicalSettledOutsideWindow);
            assertEquals("202611240002",truth.fromIssue);assertEquals("202611240003",truth.throughIssue);
            assertEquals(.5,truth.settlementCoverage,0);
            db.execSQL("DROP TABLE "+DbHelper.TABLE_R35_OOS_LABEL);
            truth=RuntimeTruthSnapshotR906.coverage(db);
            assertEquals("SOURCE_UNAVAILABLE",truth.sourceStatus);assertTrue(Double.isNaN(truth.settlementCoverage));
        }
    }

    private static ResearchVisualDataR82.Panel panel(String state,List<ResearchVisualDataR82.Point> points) {
        return new ResearchVisualDataR82.Panel("research","C01","LINE","actual_table","current","fixture",state,points,List.of());
    }

    public static void chartUnknownDoesNotPresentZero(Context app) {
        assertEquals("EMPTY",panel("OBSERVED_EMPTY",List.of()).displayState);
        List<ResearchVisualDataR82.Point> zero=List.of(new ResearchVisualDataR82.Point("valid",0,Double.NaN,Double.NaN,1,""));
        assertEquals("HAS_DATA",panel("READY",zero).displayState);
        assertEquals("UNAVAILABLE",panel("STALE_SOURCE_WATERMARK",zero).displayState);
        assertEquals("STALE",panel("STALE_SOURCE_WATERMARK",zero).reasonCode);
        assertEquals("UNAVAILABLE",panel("READ_TIMEOUT",List.of()).displayState);
        assertEquals("TIMED_OUT",panel("READ_TIMEOUT",List.of()).reasonCode);
        assertEquals("WAITING_SOURCE",panel("WAITING_SOURCE",List.of()).reasonCode);
        assertEquals("HAS_DATA",panel("READY",List.of(new ResearchVisualDataR82.Point("valid",3,Double.NaN,Double.NaN,1,""))).displayState);
    }

    public static void tablePageKeepsItsExactDenominator(Context app) {
        try(SQLiteDatabase db=SQLiteDatabase.create(null)){
            db.execSQL("CREATE TABLE r27_expert_proposal(generation INTEGER,expert TEXT,proposal_json TEXT,policy_hash TEXT,recorded_at INTEGER)");
            for(int i=0;i<29;i++)db.execSQL("INSERT INTO r27_expert_proposal VALUES(1,'test','{}',?,?)",new Object[]{"policy-"+i,i});
            Map<String,Object> first=ResearchTableAdaptersR30R1.readPageR31(db,"T08",Map.of(),0,8,null);
            assertEquals(8,first.get("shown"));assertEquals(29L,first.get("totalRows"));
            assertEquals(Boolean.TRUE,first.get("hasMore"));
            Map<String,Object> beyond=ResearchTableAdaptersR30R1.readPageR31(db,"T08",Map.of(),29,8,null);
            assertEquals(0,beyond.get("shown"));assertEquals(29L,beyond.get("totalRows"));
            assertEquals(Boolean.FALSE,beyond.get("hasMore"));
            db.execSQL("DROP TABLE r27_expert_proposal");
            Map<String,Object> unavailable=ResearchTableAdaptersR30R1.readPageR31(db,"T08",Map.of(),0,8,null);
            assertNull(unavailable.get("totalRows"));assertEquals(Boolean.FALSE,unavailable.get("totalKnown"));
            assertEquals("WAITING_SOURCE",((Map<?,?>)unavailable.get("sourceObservationR341")).get("reasonCode"));
        }
    }

    public static void effectMigrationDoesNotPrepopulatePass(Context app) {
        try(SQLiteDatabase db=SQLiteDatabase.create(null)){
            db.beginTransaction();
            try {
                RepairEffectLedgerR341.installWithin(db);
                RepairEffectLedgerR341.installWithin(db);
                try(Cursor rows=db.rawQuery("SELECT count(*) FROM "+RepairEffectLedgerR341.EFFECT,null)){
                    assertTrue(rows.moveToFirst());assertEquals(0,rows.getInt(0));
                }
                db.setTransactionSuccessful();
            }finally{db.endTransaction();}
        }
    }
}
