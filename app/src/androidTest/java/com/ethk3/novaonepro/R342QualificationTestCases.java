package com.ethk3.novaonepro;

import static org.junit.Assert.*;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

public final class R342QualificationTestCases {
    private R342QualificationTestCases() {}

    public static void scheduledCalibrationFence(Context ignored) {
        try (SQLiteDatabase db=SQLiteDatabase.create(null)) {
            db.execSQL("CREATE TABLE draws(issue TEXT PRIMARY KEY,dirty INTEGER NOT NULL,open_time INTEGER,"
                    +"first_seen_at INTEGER,n1 INTEGER,n2 INTEGER,n3 INTEGER,n4 INTEGER,n5 INTEGER,n6 INTEGER,"
                    +"n7 INTEGER,n8 INTEGER,n9 INTEGER,n10 INTEGER)");
            String target="202611250031";
            assertEquals("R342_CAL_PIT_BASE_UNKNOWN",assertThrows(IllegalStateException.class,
                    ()->CandidateCalibrationR932.requirePredrawClockR342(db,target,1201L)).getMessage());
            db.execSQL("INSERT INTO draws VALUES('202611250030',0,1000,1200,1,2,3,4,5,6,7,8,9,10)");
            assertEquals(61000L,CandidateCalibrationR932.requirePredrawClockR342(db,target,60999L));
            assertEquals("R342_CAL_SCHEDULED_CUTOFF_PASSED",assertThrows(IllegalStateException.class,
                    ()->CandidateCalibrationR932.requirePredrawClockR342(db,target,61000L)).getMessage());
            assertEquals("R342_CAL_PIT_BASE_UNKNOWN",assertThrows(IllegalStateException.class,
                    ()->CandidateCalibrationR932.requirePredrawClockR342(db,target,1199L)).getMessage());
            db.execSQL("UPDATE draws SET dirty=1");
            assertEquals("R342_CAL_PIT_BASE_UNKNOWN",assertThrows(IllegalStateException.class,
                    ()->CandidateCalibrationR932.requirePredrawClockR342(db,target,60000L)).getMessage());
        }
    }

    public static void settledIdentityFence(Context ignored) {
        assertTrue(ResearchMainlineContributionAuditR922.exactPredrawClockR342(1000,1050,1100));
        assertFalse(ResearchMainlineContributionAuditR922.exactPredrawClockR342(1050,1050,1100));
        assertFalse(ResearchMainlineContributionAuditR922.exactPredrawClockR342(1000,1050,999));
        assertFalse(ResearchMainlineContributionAuditR922.exactPredrawClockR342(1000,0,1100));
        String draw=V51ChecksumEngine.sha256("exact draw original");
        String label=V51ChecksumEngine.sha256("exact frozen label");
        String actual=V51ChecksumEngine.sha256("R36_REAL|101|"+draw+"|3|1");
        String terminal=V51ChecksumEngine.sha256("R36_SETTLEMENT|"+draw+'|'+label+'|'+actual+"|COMPLETED");
        assertTrue(ResearchMainlineContributionAuditR922.matchesTerminal(101L,3,1,"WIN",label,draw,terminal));
        assertFalse(ResearchMainlineContributionAuditR922.matchesTerminal(102L,3,1,"WIN",label,draw,terminal));
        assertFalse(ResearchMainlineContributionAuditR922.matchesTerminal(101L,4,1,"WIN",label,draw,terminal));
        assertFalse(DecisionGateEngine.b12K3(1,1,3));
    }
}
