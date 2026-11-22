package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public final class R335EvolutionRetentionPublicationTest {
    private static final String UPDATE_POLICY = "UPDATE r27_policy SET state=?,state_reason=?,updated_at=? WHERE policy_hash=? AND state NOT IN ('LIVE')";
    private static final String CONSUME = "UPDATE r30r1_proposal SET state='CONSUMED' WHERE proposal_id=? AND state='READY'";
    @org.junit.Test public void disjointRetentionColumnsAllowResearchAndPreserveOriginals() {
        try (SQLiteDatabase db=SQLiteDatabase.create(null)) { verifyControlled(db); }
    }

    static void verifyReadOnly(SQLiteDatabase db) {
        new EvolutionPublicationSqlR31.Shape(db, UPDATE_POLICY);
        new EvolutionPublicationSqlR31.Shape(db, CONSUME);
    }

    static void refuse(SQLiteDatabase db, String sql) {
        try {
            new EvolutionPublicationSqlR31.Shape(db, sql);
        } catch (IllegalStateException expected) {
            if (String.valueOf(expected.getMessage()).contains("CANONICAL_TRIGGER_REQUIRES_EXPLICIT_OWNER_PLAN")) return;
            throw expected;
        }
        throw new AssertionError("ACTIVE_SIDE_EFFECT_MUST_BE_REFUSED");
    }

    static void verifyControlled(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE r27_policy(policy_hash TEXT PRIMARY KEY,spec_json TEXT,state TEXT,state_reason TEXT,updated_at INTEGER)");
        db.execSQL("CREATE TABLE r30r1_proposal(proposal_id TEXT PRIMARY KEY,payload_json TEXT,state TEXT)");
        db.execSQL("CREATE TABLE archived(label TEXT,payload TEXT)");
        db.execSQL("CREATE TRIGGER \"r30_keep_r27_policy_004a8b41e2f9_clear\" BEFORE UPDATE OF \"spec_json\" ON \"r27_policy\" WHEN length(OLD.spec_json)>0 AND length(NEW.spec_json)=0 BEGIN INSERT INTO archived VALUES('spec',OLD.spec_json); END");
        db.execSQL("CREATE TRIGGER \"r30_keep_r30r1_proposal_7d699fb7f1a3_clear\" BEFORE UPDATE OF \"payload_json\" ON \"r30r1_proposal\" WHEN length(OLD.payload_json)>0 AND length(NEW.payload_json)=0 BEGIN INSERT INTO archived VALUES('proposal',OLD.payload_json); END");
        verifyReadOnly(db);
        refuse(db, "UPDATE r27_policy SET SPEC_JSON=? WHERE policy_hash=?");
        refuse(db, "UPDATE r30r1_proposal SET payload_json=? WHERE proposal_id=?");
        db.execSQL("INSERT INTO r27_policy VALUES('p','body','BORN','old',1)");
        db.execSQL("UPDATE r27_policy SET state='ACTIVE' WHERE policy_hash='p'");
        db.execSQL("UPDATE r27_policy SET spec_json='' WHERE policy_hash='p'");
        try (Cursor c=db.rawQuery("SELECT count(*),min(payload) FROM archived",null)) {
            if(!c.moveToFirst()||c.getInt(0)!=1||!"body".equals(c.getString(1))) throw new AssertionError("RETENTION_ORIGINAL");
        }
        ResearchWriteQueueR27.Batch body=new ResearchWriteQueueR27.Batch();
        body.add(UPDATE_POLICY,"ACTIVE","VERIFIED",2,"p");
        EvolutionPublicationSqlR31.Plan staged=new EvolutionPublicationSqlR31.Plan(db,body.ops);
        staged.requireCurrentTriggers(db);
        db.execSQL("CREATE TRIGGER unscoped_effect AFTER UPDATE ON r27_policy BEGIN INSERT INTO archived VALUES('all',NEW.state); END");
        refuse(db, UPDATE_POLICY);
        try {
            staged.requireCurrentTriggers(db);
            throw new AssertionError("CHANGED_TRIGGER_WAS_NOT_RECHECKED");
        } catch (IllegalStateException expected) {
            if (!String.valueOf(expected.getMessage()).contains("CANONICAL_TRIGGER_REQUIRES_EXPLICIT_OWNER_PLAN")) throw expected;
        }
        db.execSQL("DROP TRIGGER unscoped_effect");
        db.execSQL("CREATE TRIGGER \"r30_keep_r27_policy_a1b2c3d4e5f6_clear\" BEFORE UPDATE OF \"SPEC_JSON\", \"state_reason\" ON \"r27_policy\" WHEN 1 BEGIN INSERT INTO archived VALUES('matched',OLD.spec_json); END");
        refuse(db, UPDATE_POLICY);
    }
    @org.junit.Test public void everyBoundPayloadMustExcludeTheArchiveTransition() {
        try (SQLiteDatabase db=SQLiteDatabase.create(null)) { verifyAuthenticatedBindings(db); }
    }

    static void verifyAuthenticatedBindings(SQLiteDatabase db) {
        String sql="UPDATE r30r1_proposal SET payload_json=? WHERE proposal_id=?";
        db.execSQL("CREATE TABLE r30r1_proposal(proposal_id TEXT PRIMARY KEY,payload_json TEXT,state TEXT)");
        db.beginTransaction();
        try {
            ResearchRetentionStoreR30.ensureProtected(db);
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }

        ResearchWriteQueueR27.Batch valid=new ResearchWriteQueueR27.Batch();
        valid.add(sql,"{ }","a").add(sql,new byte[]{1},"b");
        EvolutionPublicationSqlR31.Plan planned=new EvolutionPublicationSqlR31.Plan(db,valid.ops);
        planned.requireCurrentTriggers(db);
        for (Object bad : new Object[]{null,"",new byte[0],"\u0000hidden"}) {
            ResearchWriteQueueR27.Batch mixed=new ResearchWriteQueueR27.Batch();
            mixed.add(sql,"safe","a").add(sql,bad,"b");
            try {
                new EvolutionPublicationSqlR31.Plan(db,mixed.ops);
                throw new AssertionError("ONE_UNSAFE_PAYLOAD_MUST_BLOCK_GROUPING");
            } catch (IllegalStateException expected) {
                if (!String.valueOf(expected.getMessage()).contains("CANONICAL_TRIGGER_REQUIRES_EXPLICIT_OWNER_PLAN")) throw expected;
            }
        }
        String name,ddl;
        try (Cursor row=db.rawQuery("SELECT name,sql FROM sqlite_master WHERE type='trigger' AND tbl_name='r30r1_proposal' AND name LIKE '%_clear'",null)) {
            if (!row.moveToFirst()) throw new AssertionError("MISSING_RELEASE_RETENTION_TRIGGER");
            name=row.getString(0);ddl=row.getString(1);
        }
        db.execSQL("DROP TRIGGER "+ResearchRetentionStoreR30.q(name));
        db.execSQL(ddl.substring(0,ddl.indexOf(" WHEN "))+" WHEN 1 BEGIN SELECT NEW.payload_json; END");
        try {
            planned.requireCurrentTriggers(db);
            throw new AssertionError("MODIFIED_ARCHIVE_PREDICATE");
        } catch (IllegalStateException expected) {
            if (!String.valueOf(expected.getMessage()).contains("CANONICAL_TRIGGER_REQUIRES_EXPLICIT_OWNER_PLAN")) throw expected;
        }
    }
}
