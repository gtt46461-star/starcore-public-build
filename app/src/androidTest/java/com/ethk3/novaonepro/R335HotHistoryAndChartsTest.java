package com.ethk3.novaonepro;

import android.database.sqlite.SQLiteDatabase;
import android.os.CancellationSignal;
import static org.junit.Assert.*;
import java.util.Arrays;
import org.junit.Test;

public final class R335HotHistoryAndChartsTest {
    @Test public void rollingCoverageIgnoresHistoricalRowsBeyondEitherEdgeAndDirtyRows() {
        try (SQLiteDatabase db=SQLiteDatabase.create(null)) {
            db.execSQL("CREATE TABLE draws(issue TEXT PRIMARY KEY,dirty INTEGER)");
            for (String issue : Arrays.asList("202611170840","202611170841","202611170842","202611170843","202611210080","202611210081"))
                db.execSQL("INSERT INTO draws VALUES(?,?)",new Object[]{issue,issue.endsWith("0843")?1:0});
            HotHistoryWindowR335 w=HotHistoryWindowR335.read(db,"202611210080");
            assertEquals(4997,w.missingPeriods);
            assertEquals(5,w.dates.size());
            Models.Draw old=new Models.Draw(), start=new Models.Draw(), last=new Models.Draw(), future=new Models.Draw();
            old.issue="202611170840"; start.issue="202611170841"; last.issue="202611210080"; future.issue="202611210081";
            assertEquals(Arrays.asList(start,last),w.inRange(Arrays.asList(old,start,null,last,future)));
        }
    }

    @Test public void legacyChartDistinguishesMissingTableEmptyTableAndInvalidSchema() {
        try (SQLiteDatabase db=SQLiteDatabase.create(null)) {
            CancellationSignal signal=new CancellationSignal();
            ChartsRepositoryR83.ReadContext ctx=new ChartsRepositoryR83.ReadContext(db,signal);
            for (String id:Arrays.asList("r83_algorithm_rank","r83_best_curve","r83_brier20")) {
                ResearchVisualDataR82.Panel p=ChartsRepositoryR83.loadPanel(id,db,signal,ctx);
                assertEquals("SOURCE_NOT_PRESENT",p.status);
                assertFalse(p.source.isEmpty());
                assertTrue(p.points.isEmpty());
            }
            db.execSQL("CREATE TABLE "+DbHelper.TABLE_R908_ALGORITHM_PREDRAW+"(target_issue TEXT,genome_id TEXT,hit_label INTEGER)");
            db.execSQL("CREATE TABLE "+DbHelper.TABLE_R908_ALGORITHM_GENOME+"(genome_id TEXT,display_name_zh TEXT,status TEXT,dev_n INTEGER,dev_hit_rate REAL)");
            assertEquals("EMPTY",ChartsRepositoryR83.loadPanel("r83_algorithm_rank",db,signal,ctx).status);
            db.execSQL("ALTER TABLE "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" RENAME TO historical_source");
            db.execSQL("CREATE TABLE "+DbHelper.TABLE_R908_ALGORITHM_GENOME+"(wrong TEXT)");
            assertEquals("READ_ERROR",ChartsRepositoryR83.loadPanel("r83_algorithm_rank",db,signal,ctx).status);
        }
    }
}
