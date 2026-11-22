package com.ethk3.novaonepro;

import android.database.sqlite.SQLiteDatabase;
import android.os.CancellationSignal;
import com.ethk3.novaonepro.strategyv2.ResearchGraphSpecsR30R1;
import com.ethk3.novaonepro.strategyv2.ResearchTableAdaptersR30R1;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Counts from the installed production readers. The observer opens only an existing database read-only. */
public final class InstalledChartReadback {
    private static void panel(String id, ResearchVisualDataR82.Panel data) {
        String status = data.status.matches("[A-Z0-9_]{1,100}") ? data.status : "OTHER";
        System.out.println("PANEL " + id + " " + status + " " + data.points.size() + " "
                + data.edges.size() + " " + data.matrixRows.size() + " " + data.matrixColumns.size());
    }

    public static void main(String[] args) {
        CancellationSignal signal = new CancellationSignal();
        try (SQLiteDatabase db = SQLiteDatabase.openDatabase(args[0], null, SQLiteDatabase.OPEN_READONLY)) {
            if (!db.isReadOnly()) throw new IllegalStateException("OBSERVATION_REQUIRES_READONLY");
            List<String> ids = ChartRegistryR31.visibleLegacyIds();
            List<ResearchVisualDataR82.Panel> legacy = ChartsRepositoryR27.load(db, signal);
            if (ids.size()!=29 || legacy.size()!=ids.size()) throw new AssertionError("LEGACY_MEMBERSHIP");
            for (int i=0; i<ids.size(); i++) panel(ids.get(i),legacy.get(i));
            for (int group=0; group<3; group++) {
                List<ResearchGraphSpecsR30R1.Spec> specs = ResearchGraphSpecsR30R1.group(group);
                List<ResearchVisualDataR82.Panel> views = ResearchWorkbenchRepositoryR30R1.load(db,group,signal);
                if (specs.size()!=6 || views.size()!=specs.size()) throw new AssertionError("RESEARCH_MEMBERSHIP");
                for (int i=0; i<specs.size(); i++) panel(specs.get(i).id,views.get(i));
            }
            if (ResearchTableAdaptersR30R1.ids().size()!=16) throw new AssertionError("TABLE_MEMBERSHIP");
            for (String id : ResearchTableAdaptersR30R1.ids()) {
                try {
                    Map<String,Object> page = ResearchTableAdaptersR30R1.readPageR31(db,id,Collections.emptyMap(),0,8,signal);
                    System.out.println("TABLE " + id + " " + page.get("status") + " " + page.get("shown")
                            + " " + page.get("totalMatchingRows") + " " + page.get("hasMore"));
                } catch (RuntimeException e) {
                    System.out.println("TABLE " + id + " READ_ERROR_" + e.getClass().getSimpleName() + " null null null");
                }
            }
        }
    }
}
