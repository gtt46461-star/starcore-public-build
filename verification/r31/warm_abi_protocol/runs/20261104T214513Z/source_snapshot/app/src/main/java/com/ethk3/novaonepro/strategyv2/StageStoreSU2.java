package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.IOException;
import java.util.*;

/** Immutable, content-bound stage bundles. Mutable inference state uses the normal live checkpoint rows. */
public final class StageStoreSU2 {
    private StageStoreSU2() {}
    public static void load(SQLiteDatabase db, String text) {
        String key = StageStrategySU2.keyOf(text);
        try (Cursor c = db.rawQuery("SELECT bundle FROM su2_stage_bundle WHERE genome_key=?", new String[]{key})) {
            if (!c.moveToFirst()) throw new IllegalStateException("SU2_STAGE_BUNDLE_MISSING");
            try (MemoryBudgetR31.Lease workspace=ModelPublicationR31.workspaceR31("stage-reader",2L<<20)) {
                StageStrategySU2.Definition d = StageStrategySU2.Definition.read(ModelPublicationR31.resolve(db,c.getBlob(0)));
                if (!text.equals(d.text()) || !key.equals(d.key())) throw new IllegalStateException("SU2_STAGE_BUNDLE_HASH");
                StageStrategySU2.register(d);
            } catch (IOException invalid) { throw new IllegalStateException("SU2_STAGE_BUNDLE_INVALID", invalid); }
        }
    }
    public static void save(SQLiteDatabase db, List<StageStrategySU2.Definition> defs, String campaign, long through, long now) {
        if (!db.inTransaction()) throw new IllegalStateException("SU2_STAGE_WRITER_REQUIRED");
        for (StageStrategySU2.Definition d : defs) {
            PolicyIdentityLedgerR26.record(db,d.text(),d.key());
            byte[] b = d.bytes();
            db.execSQL("INSERT OR IGNORE INTO su2_stage_bundle(genome_key,bundle,byte_count,created_at) VALUES(?,?,?,?)", new Object[]{d.key(), b, b.length, now});
            db.execSQL("INSERT OR IGNORE INTO su2_model_genome(genome_key,canonical_text,formula,tier,origin_op,repair_note,born_campaign,born_sequence,state,created_at) VALUES(?,?,?,?,'STAGE','',?,?,'LEAGUE',?)",
                new Object[]{d.key(), d.text(), d.formula(), d.tier, campaign, through, now});
            ModelStoreSU2.joinLeague(db, "SU2:" + d.key(), "SU2", d.tier + "@" + through + "·阶段层" + Math.round(100 * d.coverage) + "%", d.key(), through + 1);
        }
        // Latest three per tier and the actual current mainline retain their immutable bundles.
        db.execSQL("UPDATE su2_model_genome SET state='RETIRED' WHERE origin_op='STAGE' AND genome_key NOT IN "
            + "(SELECT substr(member_id,5) FROM su2_mainline_choice ORDER BY from_sequence DESC LIMIT 1) "
            + "AND genome_key NOT IN (SELECT genome_key FROM (SELECT genome_key FROM su2_model_genome WHERE origin_op='STAGE' AND tier='F15' ORDER BY born_sequence DESC,genome_key LIMIT 3)) "
            + "AND genome_key NOT IN (SELECT genome_key FROM (SELECT genome_key FROM su2_model_genome WHERE origin_op='STAGE' AND tier='F30' ORDER BY born_sequence DESC,genome_key LIMIT 3)) "
            + "AND genome_key NOT IN (SELECT genome_key FROM (SELECT genome_key FROM su2_model_genome WHERE origin_op='STAGE' AND tier='F100' ORDER BY born_sequence DESC,genome_key LIMIT 3))");
        db.execSQL("UPDATE su2_league SET state='RETIRED' WHERE genome_key IN (SELECT genome_key FROM su2_model_genome WHERE origin_op='STAGE' AND state='RETIRED')");
        db.execSQL("DELETE FROM su2_stage_bundle WHERE genome_key NOT IN (SELECT genome_key FROM su2_model_genome WHERE origin_op='STAGE' AND state='LEAGUE')");
    }
    static void prepareR31(ModelPublicationR31.Plan plan,List<StageStrategySU2.Definition> defs,String campaign,long through){
        for(StageStrategySU2.Definition d:defs){
            ModelStoreSU2.prepareIdentityR31(plan,d.text(),d.key());byte[] reference=plan.payload(d.bytes());
            plan.put("STAGE",d.key(),d.key(),reference,reference.length,plan.now);
            if(plan.item("GENOME",d.key())==null)plan.put("GENOME",d.key(),d.key(),d.text(),d.formula(),d.tier,"STAGE",null,"",campaign,through,null,null,"LEAGUE",plan.now,0L,0L);
            ModelStoreSU2.prepareLeagueR31(plan,"SU2:"+d.key(),"SU2",d.tier+"@"+through+"·阶段层"+Math.round(100*d.coverage)+"%",d.key(),through+1);
        }
    }
    static void projectPreparedR31(SQLiteDatabase db,ModelPublicationR31.Plan p){if(p.count("STAGE")>0)db.execSQL("INSERT OR IGNORE INTO su2_stage_bundle(genome_key,bundle,byte_count,created_at) "+ModelPublicationR31.select(p,"STAGE","c0,c1,c2,c3"));}
    /** The old latest-three rule runs after complete publication, in finite owner slices. */
    static void pruneR31(com.ethk3.novaonepro.DbStore store,RuntimeCoordinatorR26.Claim claim){
        List<String> keys=new ArrayList<>();String sql="SELECT genome_key FROM su2_model_genome WHERE origin_op='STAGE' AND state<>'RETIRED' AND genome_key NOT IN (SELECT substr(member_id,5) FROM su2_mainline_choice ORDER BY from_sequence DESC LIMIT 1)";
        for(String tier:ModelEvolutionSU2.TIERS)sql+=" AND genome_key NOT IN (SELECT genome_key FROM su2_model_genome WHERE origin_op='STAGE' AND tier='"+tier+"' ORDER BY born_sequence DESC,genome_key LIMIT 3)";
        try(Cursor c=store.readable().rawQuery(sql+" ORDER BY genome_key LIMIT 8",null)){while(c.moveToNext())keys.add(c.getString(0));}if(keys.isEmpty())return;
        final String guard=sql;store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R31_STAGE_RETENTION_SLICE",db->{RuntimeCoordinatorR26.requireFence(db,claim);for(String key:keys){db.execSQL("UPDATE su2_model_genome SET state='RETIRED' WHERE genome_key=? AND genome_key IN ("+guard+")",new Object[]{key});db.execSQL("UPDATE su2_league SET state='RETIRED' WHERE genome_key=? AND EXISTS(SELECT 1 FROM su2_model_genome WHERE genome_key=? AND state='RETIRED')",new Object[]{key,key});db.execSQL("DELETE FROM su2_stage_bundle WHERE genome_key=? AND EXISTS(SELECT 1 FROM su2_model_genome WHERE genome_key=? AND state='RETIRED')",new Object[]{key,key});}RuntimeCoordinatorR26.requireFence(db,claim);return 0;});
    }
    public static String diagnosisLines(SQLiteDatabase db) {
        StringBuilder out = new StringBuilder();
        for (String tier : ModelEvolutionSU2.TIERS) {
            try (MemoryBudgetR31.Lease workspace=ModelPublicationR31.workspaceR31("stage-diagnosis",2L<<20);Cursor c = db.rawQuery("SELECT b.bundle FROM su2_stage_bundle b JOIN su2_model_genome g ON g.genome_key=b.genome_key WHERE g.tier=? AND g.state='LEAGUE' ORDER BY g.born_sequence DESC,g.genome_key LIMIT 1", new String[]{tier})) {
                if (!c.moveToFirst()) continue;
                StageStrategySU2.Definition d = StageStrategySU2.Definition.read(ModelPublicationR31.resolve(db,c.getBlob(0)));
                String base = d.candidates.get(d.baseIndex);
                String baseKey = CommitteeSU2.isCommittee(base) ? CommitteeSU2.parse(base).key() : ModelGenomeSU2.parse(base).key();
                long[] mine = totals(db, d.key()), old = totals(db, baseKey);
                out.append("stage\t").append(d.coverage).append('\t').append(d.stages()).append("\t1\t")
                    .append(rate(d.validationHits, d.validationActs)).append('\t').append(rate(d.baseHits, d.baseActs)).append('\t')
                    .append(rate(mine[1], mine[0])).append('\t').append(mine[0]).append('\t').append(rate(old[1], old[0])).append('\t').append(old[0]).append('\n');
            } catch (IOException invalid) { throw new IllegalStateException("SU2_STAGE_DIAG_BUNDLE", invalid); }
        }
        return out.toString();
    }
    static String rate(long hits, long acts) { return acts == 0 ? "-" : String.valueOf(hits / (double) acts); }
    static long[] totals(SQLiteDatabase db, String key) {
        try (Cursor c = db.rawQuery("SELECT coalesce(sum(actions),0),coalesce(sum(hits),0) FROM su2_track_block WHERE member_id=?", new String[]{key})) {
            return c.moveToFirst() ? new long[]{c.getLong(0), c.getLong(1)} : new long[]{0, 0};
        }
    }
}
