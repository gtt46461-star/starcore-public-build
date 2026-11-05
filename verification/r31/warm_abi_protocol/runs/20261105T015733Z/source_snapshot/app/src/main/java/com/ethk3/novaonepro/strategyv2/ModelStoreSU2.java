package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.*;
import java.util.*;
import java.util.zip.*;

/** Persistence for SU2 (P06/P07/P23/P30). Every write runs inside the single research writer transaction. */
public final class ModelStoreSU2 {
    private ModelStoreSU2() {}

    static void writer(SQLiteDatabase db) { if (!db.inTransaction()) throw new IllegalStateException("SU2_WRITER_TRANSACTION_REQUIRED"); }

    static boolean exists(SQLiteDatabase db, String sql, String... args) { try (Cursor c = db.rawQuery(sql, args)) { return c.moveToFirst(); } }

    // ------------------------------------------------------------- genomes / lineage
    /** R14: a committee champion is stored like a genome (its canonical text starts with SU2C|) and joins the league. */
    public static void saveCommittee(SQLiteDatabase db, CommitteeSU2 c, String tier, String campaign, long seq, double judge, long now) {
        writer(db);PolicyIdentityLedgerR26.record(db,c.text(),c.key());
        db.execSQL("INSERT OR IGNORE INTO su2_model_genome(genome_key,canonical_text,formula,tier,origin_op,parent_key,repair_note,born_campaign,born_sequence,dev_fitness,val_fitness,state,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)",
            new Object[]{c.key(), c.text(), c.formula(), tier, "COMMITTEE", null, "", campaign, seq, null, judge, "LEAGUE", now});
    }

    public static void saveGenome(SQLiteDatabase db, ModelEvolutionSU2.Ranked r, String campaign, long seq, double dev, double val, String state, long now) {
        writer(db);PolicyIdentityLedgerR26.record(db,r.g.text(),r.g.key());
        // minSdk 21 ships SQLite 3.8: no UPSERT. Insert once, then update scores; LEAGUE/RETIRED are sticky.
        db.execSQL("INSERT OR IGNORE INTO su2_model_genome(genome_key,canonical_text,formula,tier,origin_op,parent_key,repair_note,born_campaign,born_sequence,dev_fitness,val_fitness,state,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)",
            new Object[]{r.g.key(), r.g.text(), r.g.formula(), ModelEvolutionSU2.tierOf(r.g), r.origin, r.parent.isEmpty() ? null : r.parent,
                r.note.length() > 512 ? r.note.substring(0, 512) : r.note, campaign, seq, dev, val, state, now});
        db.execSQL("UPDATE su2_model_genome SET dev_fitness=?,val_fitness=?,state=CASE WHEN state IN ('RETIRED','LEAGUE') THEN state ELSE ? END WHERE genome_key=?",
            new Object[]{dev, val, state, r.g.key()});
        if (!r.parent.isEmpty()) db.execSQL("INSERT OR IGNORE INTO su2_lineage(child_key,parent_key,op,campaign_id,note) VALUES(?,?,?,?,?)", new Object[]{r.g.key(), r.parent, r.origin, campaign, r.note});
    }

    // ------------------------------------------------------------- evolution state
    public static ModelEvolutionSU2.State loadEvolution(SQLiteDatabase db) throws IOException {
        try (MemoryBudgetR31.Lease workspace=ModelPublicationR31.workspaceR31("evolution-reader",24L<<20);Cursor c = db.rawQuery("SELECT state_blob FROM su2_evolution_state WHERE singleton=1", null)) {
            return c.moveToFirst() ? ModelStateCodecSU2.decode(ModelPublicationR31.resolve(db,c.getBlob(0))) : new ModelEvolutionSU2.State();
        }
    }
    static byte[] evolutionBlobR31(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT state_blob FROM su2_evolution_state WHERE singleton=1",null)){return c.moveToFirst()?c.getBlob(0):null;}}
    public static void saveEvolution(SQLiteDatabase db, String campaign, ModelEvolutionSU2.State s, long now) {
        writer(db); byte[] b = ModelStateCodecSU2.encode(s);
        db.execSQL("INSERT OR REPLACE INTO su2_evolution_state(singleton,campaign_id,state_blob,byte_count,updated_at) VALUES(1,?,?,?,?)", new Object[]{campaign, b, b.length, now});
    }

    /** Preparation keeps every original program, score, identity and lineage; no writer work. */
    public static void prepareGenomeR31(ModelPublicationR31.Plan p,ModelEvolutionSU2.Ranked r,String campaign,long seq,double dev,double val,String state){
        prepareIdentityR31(p,r.g.text(),r.g.key());ModelPublicationR31.Item old=p.item("GENOME",r.g.key());
        Object[] values=old==null?new Object[]{r.g.key(),r.g.text(),r.g.formula(),ModelEvolutionSU2.tierOf(r.g),r.origin,r.parent.isEmpty()?null:r.parent,r.note.length()>512?r.note.substring(0,512):r.note,campaign,seq,dev,val,state,p.now,1L,0L}:old.values.clone();
        values[9]=dev;values[10]=val;if(!"LEAGUE".equals(values[11])&&!"RETIRED".equals(values[11]))values[11]=state;values[13]=1L;p.put("GENOME",r.g.key(),values);
        if(!r.parent.isEmpty()&&p.item("LINEAGE",r.g.key()+":"+r.parent)==null)p.put("LINEAGE",r.g.key()+":"+r.parent,r.g.key(),r.parent,r.origin,campaign,null,null,r.note);
    }
    static void prepareIdentityR31(ModelPublicationR31.Plan p,String text,String key){PolicyIdentityLedgerR26.Prepared identity=PolicyIdentityLedgerR26.prepare(text,key);if(identity!=null)p.put("IDENTITY",key,key,identity.canonical,identity.text,identity.abi,p.now);}
    public static void prepareCommitteeR31(ModelPublicationR31.Plan p,CommitteeSU2 c,String tier,String campaign,long seq,double judge){
        prepareIdentityR31(p,c.text(),c.key());if(p.item("GENOME",c.key())==null)p.put("GENOME",c.key(),c.key(),c.text(),c.formula(),tier,"COMMITTEE",null,"",campaign,seq,null,judge,"LEAGUE",p.now,0L,0L);
    }
    static void prepareLeagueR31(ModelPublicationR31.Plan p,String id,String kind,String name,String genome,long seq){
        p.put("LEAGUE",id,id,kind,name,genome,seq,"ACTIVE");if(genome!=null){ModelPublicationR31.Item item=p.item("GENOME",genome);if(item==null)throw new IllegalStateException("R31_MODEL_LEAGUE_WITHOUT_PREPARED_GENOME");Object[] values=item.values.clone();values[14]=1L;if(!"RETIRED".equals(values[11]))values[11]="LEAGUE";p.put("GENOME",genome,values);}
    }
    public static void prepareEvolutionR31(ModelPublicationR31.Plan p,String campaign,ModelEvolutionSU2.State state,byte[] previous){
        p.reserve(24L<<20);byte[] bytes=ModelStateCodecSU2.encode(state),reference=p.payload(bytes);byte[] old=previous==null?null:ModelPublicationR31.isReference(previous)?previous.clone():p.payload(previous);
        p.put("EVOLUTION","1",campaign,reference,reference.length,p.now,old);
    }
    /** Finite projections: each SELECT is bounded by the exact immutable typed manifest.
     * Genomes deliberately retain insert-once metadata and sticky LEAGUE/RETIRED. */
    static void projectPreparedR31(SQLiteDatabase db,ModelPublicationR31.Plan p){
        writer(db);String identities=ModelPublicationR31.select(p,"IDENTITY","c0,c1,c2,c3,c4");
        try(Cursor c=db.rawQuery("SELECT 1 FROM ("+identities+") i JOIN r26_policy_identity old ON old.policy_hash=i.c0 WHERE old.canonical_policy IS NOT i.c1 OR old.genotype_text IS NOT i.c2 OR old.numerical_abi IS NOT i.c3 LIMIT 1",null)){if(c.moveToFirst())throw new IllegalStateException("R26_POLICY_IDENTITY_CONFLICT");}
        if(p.count("IDENTITY")>0)db.execSQL("INSERT OR IGNORE INTO r26_policy_identity(policy_hash,canonical_policy,genotype_text,numerical_abi,created_at) "+identities);
        if(p.count("GENOME")>0){
            String rows=ModelPublicationR31.select(p,"GENOME","c0,c1,c2,c3,c4,c5,c6,c7,c8,c9,c10,c11,c12,c13,c14");
            db.execSQL("INSERT OR IGNORE INTO su2_model_genome(genome_key,canonical_text,formula,tier,origin_op,parent_key,repair_note,born_campaign,born_sequence,dev_fitness,val_fitness,state,created_at) SELECT c0,c1,c2,c3,c4,c5,c6,c7,c8,c9,c10,c11,c12 FROM ("+rows+")");
            String pick=" FROM ("+rows+") prepared WHERE prepared.c0=su2_model_genome.genome_key";
            db.execSQL("UPDATE su2_model_genome SET dev_fitness=CASE WHEN (SELECT c13"+pick+")=1 THEN (SELECT c9"+pick+") ELSE dev_fitness END,val_fitness=CASE WHEN (SELECT c13"+pick+")=1 THEN (SELECT c10"+pick+") ELSE val_fitness END,state=CASE WHEN state='RETIRED' THEN state WHEN (SELECT c14"+pick+")=1 THEN 'LEAGUE' WHEN state='LEAGUE' THEN state WHEN (SELECT c13"+pick+")=1 THEN (SELECT c11"+pick+") ELSE state END WHERE genome_key IN (SELECT c0 FROM ("+rows+"))");
        }
        if(p.count("LINEAGE")>0)db.execSQL("INSERT OR IGNORE INTO su2_lineage(child_key,parent_key,op,campaign_id,parent_dev,child_dev,note) "+ModelPublicationR31.select(p,"LINEAGE","c0,c1,c2,c3,c4,c5,c6"));
        if(p.count("EVOLUTION")>0)db.execSQL("INSERT OR REPLACE INTO su2_evolution_state(singleton,campaign_id,state_blob,byte_count,updated_at) SELECT 1,c0,c1,c2,c3 FROM ("+ModelPublicationR31.select(p,"EVOLUTION","c0,c1,c2,c3")+")");
        if(p.count("LEAGUE")>0)db.execSQL("INSERT OR IGNORE INTO su2_league(member_id,kind,name,genome_key,joined_sequence,state) "+ModelPublicationR31.select(p,"LEAGUE","c0,c1,c2,c3,c4,c5"));
        StageStoreSU2.projectPreparedR31(db,p);MatrixStoreSU2.projectPreparedR31(db,p);
    }

    // ------------------------------------------------------------- live states (checkpointed, P06)
    public static byte[] liveBlob(SQLiteDatabase db, String owner, long[] through) {
        try (Cursor c = db.rawQuery("SELECT through_sequence,state_blob FROM su2_live_state WHERE owner=?", new String[]{owner})) {
            if (!c.moveToFirst()) return null; through[0] = c.getLong(0); return c.getBlob(1);
        }
    }
    public static void saveLive(SQLiteDatabase db, String owner, long through, byte[] blob, long now) {
        writer(db);
        if (blob.length > 16 << 20) throw new IllegalStateException("SU2_LIVE_STATE_TOO_LARGE:" + owner);
        db.execSQL("INSERT OR REPLACE INTO su2_live_state(owner,through_sequence,state_blob,byte_count,updated_at) VALUES(?,?,?,?,?)", new Object[]{owner, through, blob, blob.length, now});
    }
    public static byte[] gzip(byte[] raw) { try { ByteArrayOutputStream b = new ByteArrayOutputStream(); try (GZIPOutputStream z = new GZIPOutputStream(b)) { z.write(raw); } return b.toByteArray(); } catch (IOException e) { throw new IllegalStateException(e); } }
    public static byte[] gunzip(byte[] z) throws IOException { try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(z)); ByteArrayOutputStream out = new ByteArrayOutputStream()) { byte[] buf = new byte[65536]; int n; while ((n = in.read(buf)) > 0) { out.write(buf, 0, n); if (out.size() > 64 << 20) throw new IOException("SU2_LIVE_STATE_INFLATE_LIMIT"); } return out.toByteArray(); } }

    // ------------------------------------------------------------- decisions (every period, acted or not)
    public static void insertDecision(SQLiteDatabase db, String genome, long seq, ModelLiveSU2.Decision d, long frozenAt, long openTime) {
        writer(db);
        if (frozenAt >= openTime) throw new IllegalStateException("SU2_DECISION_AFTER_OPEN");
        db.execSQL("INSERT OR IGNORE INTO su2_live_decision(genome_key,sequence_ordinal,position,number_mask,act,gscore,phat,frozen_at,target_open_time) VALUES(?,?,?,?,?,?,?,?,?)",
            new Object[]{genome, seq, d.position, d.mask, d.act ? 1 : 0, Double.isNaN(d.gscore) ? -1.0 : d.gscore, Double.isNaN(d.phat) ? null : d.phat, frozenAt, openTime});
    }
    /** Inside the settlement owner transaction: every frozen decision of this period gets its label. */
    public static int settle(SQLiteDatabase db, long seq, int[] draw, long completedAt) {
        writer(db);
        Object[] a = new Object[13]; for (int p = 0; p < 10; p++) a[p] = draw[p]; a[10] = completedAt; a[11] = seq; a[12] = completedAt;
        db.execSQL("UPDATE su2_live_decision SET hit=(number_mask>>(CASE position WHEN 1 THEN ? WHEN 2 THEN ? WHEN 3 THEN ? WHEN 4 THEN ? WHEN 5 THEN ? WHEN 6 THEN ? WHEN 7 THEN ? WHEN 8 THEN ? WHEN 9 THEN ? ELSE ? END-1))&1,settled_at=? WHERE sequence_ordinal=? AND hit IS NULL AND target_open_time<=?", a);
        try (Cursor c = db.rawQuery("SELECT changes()", null)) { return c.moveToFirst() ? c.getInt(0) : 0; }
    }

    // ------------------------------------------------------------- league
    public static void joinLeague(SQLiteDatabase db, String memberId, String kind, String name, String genome, long seq) {
        writer(db);
        db.execSQL("INSERT OR IGNORE INTO su2_league(member_id,kind,name,genome_key,joined_sequence) VALUES(?,?,?,?,?)", new Object[]{memberId, kind, name, genome, seq});
        if (genome != null) db.execSQL("UPDATE su2_model_genome SET state='LEAGUE' WHERE genome_key=? AND state<>'RETIRED'", new Object[]{genome});
    }
    public static List<String[]> league(SQLiteDatabase db) {
        List<String[]> out = new ArrayList<>();
        try (Cursor c = db.rawQuery("SELECT member_id,kind,name,coalesce(genome_key,''),joined_sequence FROM su2_league WHERE state='ACTIVE' ORDER BY member_id", null)) { while (c.moveToNext()) out.add(new String[]{c.getString(0), c.getString(1), c.getString(2), c.getString(3), String.valueOf(c.getLong(4))}); }
        return out;
    }
    /** act/hit arrays indexed by (sequence - from); missing periods are "not acted" (MISSING is reported separately). */
    public static boolean[][] record(SQLiteDatabase db, String genome, long from, long to) {
        int n = (int) (to - from); boolean[] act = new boolean[n], hit = new boolean[n];
        try (Cursor c = db.rawQuery("SELECT sequence_ordinal,act,hit FROM su2_live_decision WHERE genome_key=? AND sequence_ordinal>=? AND sequence_ordinal<? AND hit IS NOT NULL",
            new String[]{genome, String.valueOf(from), String.valueOf(to)})) {
            while (c.moveToNext()) { int i = (int) (c.getLong(0) - from); act[i] = c.getInt(1) == 1; hit[i] = c.getInt(2) == 1; }
        }
        return new boolean[][]{act, hit};
    }
    public static void recordChoice(SQLiteDatabase db, long from, MainlinePolicySU2.Choice ch, long windowStart, long now) {
        writer(db);
        db.execSQL("INSERT OR IGNORE INTO su2_mainline_choice(from_sequence,member_id,score,incumbent_score,reason,window_start,chosen_at) VALUES(?,?,?,?,?,?,?)",
            new Object[]{from, ch.member.id, ch.score, Double.isNaN(ch.incumbentScore) ? null : ch.incumbentScore, ch.reason, windowStart, now});
    }
    public static String currentChoice(SQLiteDatabase db, long seq) {
        try (Cursor c = db.rawQuery("SELECT member_id FROM su2_mainline_choice WHERE from_sequence<=? ORDER BY from_sequence DESC LIMIT 1", new String[]{String.valueOf(seq)})) { return c.moveToFirst() ? c.getString(0) : ""; }
    }
}
