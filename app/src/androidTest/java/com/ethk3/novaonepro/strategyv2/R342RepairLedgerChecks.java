package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import android.database.sqlite.SQLiteDatabase;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class R342RepairLedgerChecks {
    private R342RepairLedgerChecks() {}

    public static void appendedCommitReadback() {
        try(SQLiteDatabase db=SQLiteDatabase.create(null)){
            db.beginTransaction();
            try{
                RepairEffectLedgerR341.installWithin(db);
                db.execSQL("CREATE TABLE r31_commit_receipt(commit_key TEXT PRIMARY KEY,payload_hash TEXT,"
                        +"operation TEXT,committed_at INTEGER)");
                db.execSQL("INSERT INTO "+RepairEffectLedgerR341.INSTALL+" VALUES('I','source','apk',1,'process','db',100)");
                db.execSQL("INSERT INTO r31_commit_receipt VALUES "
                        +"('old','p0','R31_ATOMNET_PUBLISH',90),('first','p1','R31_ATOMNET_PUBLISH',101),"
                        +"('latest','p2','R31_ATOMNET_PUBLISH',111)");
                RepairEffectLedgerR341.appendVerified(db,"I","R31_ATOMNET_PUBLISH","old","p0");
                RepairEffectLedgerR341.appendVerified(db,"I","R31_ATOMNET_PUBLISH","first","p1");
                RepairEffectLedgerR341.appendVerified(db,"I","R31_ATOMNET_PUBLISH","latest","wrong");
                RepairEffectLedgerR341.appendVerified(db,"I","R31_ATOMNET_PUBLISH","latest","p2");
                RepairEffectLedgerR341.appendVerified(db,"I","R31_ATOMNET_PUBLISH","latest","p2");
                db.setTransactionSuccessful();
            }finally{db.endTransaction();}
            Map<String,Object> installation=Map.of("state","VERIFIED_INSTALLATION","sourceCommit","source","apkSha256","apk");
            Map<String,Object> result=RepairEffectLedgerR341.read(db,installation,"process",null);
            List<?> issues=(List<?>)result.get("issues");
            assertEquals(62,issues.size());
            Map<?,?> row=(Map<?,?>)issues.stream().filter(x->"R341-B07-04".equals(((Map<?,?>)x).get("issueId")))
                    .findFirst().orElseThrow(AssertionError::new);
            assertEquals(2L,row.get("observationCount"));
            assertEquals("first",((Map<?,?>)row.get("firstCommitObservation")).get("commitReceiptKey"));
            assertEquals("latest",((Map<?,?>)row.get("latestCommitObservation")).get("receiptKey"));
            assertEquals("COMMIT_OBSERVED_EFFECT_UNVERIFIED",row.get("effectState"));
            assertEquals(List.of(),RepairEffectLedgerR341.read(db,installation,"another-process",null).get("observations"));
        }
    }

    public static void scopedMetricComparison() {
        Map<String,Object> first=new LinkedHashMap<>();
        for(String key:new String[]{"releaseId","sourceCommit","databaseIdentity","bootId","processEpoch",
                "scopeKind","role","windowDefinition","sqlWatermark"})first.put(key,key+"-fixture");
        Map<String,Object> next=new LinkedHashMap<>(first);
        assertTrue(ReportDataR27.sameObservationCohortR342(first,next));
        next.put("processEpoch","other");
        assertFalse(ReportDataR27.sameObservationCohortR342(first,next));
        next.put("processEpoch",first.get("processEpoch"));
        next.put("sqlWatermark","another-snapshot");
        assertFalse(ReportDataR27.sameObservationCohortR342(first,next));
        assertFalse(ReportDataR27.sameObservationCohortR342(Map.of(),Map.of()));
    }
}
