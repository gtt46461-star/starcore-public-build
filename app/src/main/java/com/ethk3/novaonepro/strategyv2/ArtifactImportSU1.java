package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import com.ethk3.novaonepro.*;

/** A desktop asset enters a fresh research lineage; it cannot import prospective evidence. */
public final class ArtifactImportSU1 {
    private ArtifactImportSU1() {}
    public static String install(DbStore store,String wire){
        PortableArtifactSU1 a=PortableArtifactSU1.decode(wire);if(!StrategySchemaSU1.ready(store.readable()))throw new IllegalStateException("SU1_MIGRATION_REQUIRED");if(!ArchiveStoreSU1.canCreate(store.readable()))throw new IllegalStateException("SU1_STORAGE_BUDGET");
        if(!TargetIssueResolver.isValid(a.throughIssue))throw new IllegalArgumentException("SU1_IMPORT_ISSUE");Models.Draw latest=store.getLatestDraw();if(latest==null||TargetIssueResolver.compare(latest.issue,a.throughIssue)<0)throw new IllegalStateException("SU1_IMPORT_WAIT_LOCAL_HISTORY");
        long through=LiveShadowLoopSU1.sequence(latest.issue),now=System.currentTimeMillis();
        return store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SU1_IMPORT_RESEARCH_ONLY",db->{
            try(Cursor c=db.rawQuery("SELECT strategy_id FROM su1_portable_import WHERE artifact_hash=? AND kernel_hash=?",new String[]{a.hash,TrainingCodeIdentityR964.SHA256})){if(c.moveToFirst())return c.getString(0);}
            String config="{\"kind\":\"DESKTOP_GRAMMAR_IMPORT_RESEARCH_ONLY\",\"artifact\":\""+a.hash+"\",\"kernel\":\""+TrainingCodeIdentityR964.SHA256+"\"}",campaign=StrategyRepositorySU1.beginCampaign(db,"HISTORY_REPLAY",through,config,now);
            String input=StrategyRepositorySU1.blob(db,a.payload,a.fitted==null?PortableArtifactSU1.FORMAT:PortableArtifactSU1.MODEL_FORMAT,now),id=StrategyRepositorySU1.candidate(db,a.program,a.coverage,a.margin,a.fitted==null?"DESKTOP_GRAMMAR":"DESKTOP_FITTED_MODEL",null,campaign,through,a.dataset,-1,now,a.fitted);
            StrategyRepositorySU1.row(db,"su1_portable_import","artifact_hash",input,"kernel_hash",TrainingCodeIdentityR964.SHA256,"strategy_id",id,"source_through_issue",a.throughIssue,"recorded_at",now);
            db.execSQL("UPDATE su1_campaign SET status='DONE',finished_at=?,programs_evaluated=0 WHERE campaign_id=?",new Object[]{now,campaign});return id;
        });
    }
}
