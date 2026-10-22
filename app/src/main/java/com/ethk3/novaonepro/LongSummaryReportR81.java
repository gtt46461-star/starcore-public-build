package com.ethk3.novaonepro;
import android.database.Cursor;
import com.ethk3.novaonepro.r952.*;
import java.util.*;
/** Read-only snapshot rendering. No model, action, migration, capture or qualification writer. */
public final class LongSummaryReportR81 {
    private LongSummaryReportR81() {}
    /** Explicit full-report snapshot observation; it grants no gate or efficacy. */
    public static void appendSnapshotReportR81(ReportReadContextCP4 ctx,ReportTextCP12 out){
        Set<String> installed=new HashSet<>();try(Cursor c=ctx.select("SELECT name FROM sqlite_master WHERE type='table' AND name IN(?,?,?,?,?,?,?)",new String[]{LongScaleSummaryR80.EPOCH,LongScaleSummaryR80.HEAD,LongScaleSummaryR80.SNAPSHOT,LongScaleSummaryR80.PUBLICATION,LongCandidateLifecycleR81.CREATION,LongCandidateLifecycleR81.SPEC,LongCandidateLifecycleR81.FROZEN_SOURCE})){while(c.moveToNext())installed.add(c.getString(0));}
        out.append("\nLONG_SUMMARY_R81|scope=SNAPSHOT_OBSERVATION_NO_LONG_HORIZON_EFFICACY|scales=1440,10080,43200|raw_tail_limit=5000");
        if(installed.size()!=7){out.append("|schema=").append(installed.isEmpty()?"NOT_INSTALLED":"PARTIAL_SCHEMA").append("|present=").append(installed.size()).append("/7");return;}
        try(Cursor c=ctx.select("SELECT h.epoch_hash,e.first_issue,h.through_issue,h.generation,h.status,h.last_error,h.state_hash,h.source_chain FROM "+LongScaleSummaryR80.HEAD+" h JOIN "+LongScaleSummaryR80.EPOCH+" e ON e.epoch_hash=h.epoch_hash WHERE h.singleton=1",null)){
            if(!c.moveToFirst())out.append("|head=NOT_STARTED");else{
                int span=TargetIssueResolver.distance(c.getString(1),c.getString(2));need(span>=0,"LONG_REPORT_HEAD_ISSUE");
                out.append("|head=").append(c.getString(4)).append("|epoch=").append(c.getString(0)).append("|first=").append(c.getString(1)).append("|through=").append(c.getString(2)).append("|generation=").append(c.getLong(3)).append("|state_hash=").append(c.getString(6)).append("|source_chain=").append(c.getString(7)).append("|reason=").append(c.getString(5));
                for(int window:LongScaleCountsR80.windows())out.append("|accepted_support_").append(window).append('=').append(Math.min((long)span+1,window));
            }
        }
        out.append("\nLONG_SOURCE_COUNTS|hot_snapshots=").append(ctx.scalarLong("SELECT COUNT(*) FROM "+LongScaleSummaryR80.SNAPSHOT,null)).append("|published=").append(ctx.scalarLong("SELECT COUNT(*) FROM "+LongScaleSummaryR80.PUBLICATION,null)).append("|dynamic_definitions=").append(ctx.scalarLong("SELECT COUNT(*) FROM "+LongCandidateLifecycleR81.SPEC,null)).append("|pinned_atom_reference_rows=").append(AtomCatalogPinnedCP12.ROWS).append("|pinned_seed_reference_rows=").append(ModernSeedFactoryCP12.TOTAL);
        out.append("\nLONG_CHILD_STATES");try(Cursor c=ctx.select("SELECT state,COUNT(*) FROM "+LongCandidateLifecycleR81.CREATION+" GROUP BY state ORDER BY state",null)){while(c.moveToNext())out.append('|').append(c.getString(0)).append('=').append(c.getLong(1));}
        out.append("\nLONG_TASK_STATES");try(Cursor c=ctx.select("SELECT q.kind,q.status,COUNT(*) FROM v57_research_task_r967 q JOIN "+LongCandidateLifecycleR81.CREATION+" l ON l.candidate_id=q.candidate_id WHERE q.kind IN('TRAIN_CANDIDATE','FREEZE_CANDIDATE') GROUP BY q.kind,q.status ORDER BY q.kind,q.status",null)){while(c.moveToNext())out.append('|').append(c.getString(0)).append(':').append(c.getString(1)).append('=').append(c.getLong(2));}
    }
    private static void need(boolean value,String reason){if(!value)throw new ReportReadContextCP4.ReportReadFailureCP4(reason);}
}
