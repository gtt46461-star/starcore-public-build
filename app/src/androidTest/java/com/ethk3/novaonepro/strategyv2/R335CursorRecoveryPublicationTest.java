package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** Exercises the installed journal trigger through the evolution compiler and SQLite. */
public final class R335CursorRecoveryPublicationTest {
    private static final String INSERT = "INSERT OR IGNORE INTO r30r1_analysis_run VALUES(?,?,?,?,?,?,?,?,?,?,?,?)";
    private static final String CAPTURE = "CREATE TRIGGER r31_pin_source_capture AFTER INSERT ON r30r1_analysis_run WHEN NEW.status IN ('INPUT_DATASET_PINNED','ONLINE_MODEL_ROLE_REGISTERED') BEGIN INSERT OR IGNORE INTO r31_pin_source_fence VALUES(NEW.run_id,COALESCE((SELECT seq FROM sqlite_sequence WHERE name='r31_source_event'),0)); END";
    private static final String BAD_TRIGGER = "CANONICAL_TRIGGER_REQUIRES_EXPLICIT_OWNER_PLAN:r30r1_analysis_run";

    private static Object[] record(String id, Object status) {
        return new Object[]{id,"dataset","policy","SELECT","commit",0,1,7,"contract",status,"original-"+id,42};
    }

    private static int count(SQLiteDatabase db, String sql) {
        try (Cursor rows=db.rawQuery(sql,null)) {
            if (!rows.moveToFirst()) throw new AssertionError(sql);
            return rows.getInt(0);
        }
    }

    private static void refuses(Runnable test) {
        try { test.run(); }
        catch (IllegalStateException error) {
            if (String.valueOf(error.getMessage()).contains(BAD_TRIGGER)) return;
            throw error;
        }
        throw new AssertionError("publication accepted an operation with an unproved side effect");
    }

    private static void verify(SQLiteDatabase db) {
        db.execSQL(ResearchSchemaR30R1.DDL[0]);
        db.execSQL("CREATE TABLE r31_source_event(event_seq INTEGER PRIMARY KEY AUTOINCREMENT)");
        db.execSQL("INSERT INTO r31_source_event DEFAULT VALUES");
        db.execSQL("CREATE TABLE r31_pin_source_fence(run_id TEXT PRIMARY KEY,event_seq INTEGER NOT NULL)");
        db.execSQL(CAPTURE);
        ResearchWriteQueueR27.Batch batch=new ResearchWriteQueueR27.Batch();
        batch.add(INSERT,record("legacy","LEGACY_CURSOR_BOUNDARY_RECOVERY"));
        batch.add(INSERT,record("invalid","INVALID_INPUT_CURSOR_PRESERVED"));
        EvolutionPublicationSqlR31.Plan plan=new EvolutionPublicationSqlR31.Plan(db,batch.ops);
        plan.requireCurrentTriggers(db);
        StringBuilder ddl=new StringBuilder("CREATE TABLE r31_evolution_op(publication_id TEXT,kind INTEGER,op_seq INTEGER");
        for(int i=0;i<12;i++) ddl.append(",a").append(i);
        db.execSQL(ddl.append(")").toString());
        for(int i=0;i<batch.ops.size();i++) {
            Object[] args=new Object[15];
            args[0]="recovery";args[1]=0;args[2]=i;
            System.arraycopy(batch.ops.get(i).args,0,args,3,12);
            db.execSQL("INSERT INTO r31_evolution_op VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",args);
        }
        String sql=plan.shapes.get(0).compile("recovery",0);
        db.beginTransaction();
        try { plan.requireCurrentTriggers(db); db.execSQL(sql); }
        finally { db.endTransaction(); }
        if(count(db,"SELECT count(*) FROM r30r1_analysis_run")!=0) throw new AssertionError("rollback");
        db.beginTransaction();
        try {
            plan.requireCurrentTriggers(db);db.execSQL(sql);db.execSQL(sql);
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
        if(count(db,"SELECT count(*) FROM r30r1_analysis_run WHERE summary_json='original-'||run_id")!=2
                || count(db,"SELECT count(*) FROM r31_pin_source_fence")!=0) throw new AssertionError("recovery originals or duplicate");
        for(Object value:new Object[]{"INPUT_DATASET_PINNED","ONLINE_MODEL_ROLE_REGISTERED","UNKNOWN",null,1L,new byte[]{1}}) {
            ResearchWriteQueueR27.Batch mixed=new ResearchWriteQueueR27.Batch();
            mixed.add(INSERT,record("ok","INVALID_INPUT_CURSOR_PRESERVED")).add(INSERT,record("other",value));
            refuses(()->new EvolutionPublicationSqlR31.Plan(db,mixed.ops));
        }
        db.execSQL(INSERT,record("pin","INPUT_DATASET_PINNED"));
        db.execSQL(INSERT,record("model","ONLINE_MODEL_ROLE_REGISTERED"));
        if(count(db,"SELECT count(*) FROM r31_pin_source_fence WHERE run_id IN ('pin','model') AND event_seq=1")!=2)
            throw new AssertionError("registered pins lost their original source fence");
        db.execSQL("DROP TRIGGER r31_pin_source_capture");
        db.execSQL(CAPTURE.replace("NEW.status IN ('INPUT_DATASET_PINNED','ONLINE_MODEL_ROLE_REGISTERED')","1"));
        refuses(()->plan.requireCurrentTriggers(db));
        refuses(()->new EvolutionPublicationSqlR31.Plan(db,batch.ops));
    }

    @org.junit.Test public void recoveryOriginsRemainAtomicAndPinsKeepTheirSourceFences() {
        try(SQLiteDatabase db=SQLiteDatabase.create(null)) { verify(db); }
    }
    public static void main(String[] args) {
        try {
            new R335CursorRecoveryPublicationTest().recoveryOriginsRemainAtomicAndPinsKeepTheirSourceFences();
        } catch (Throwable failure) {
            failure.printStackTrace(System.out);
            System.out.flush();
            System.exit(2);
        }
        System.out.println("RECOVERY_PUBLICATION_OK originals=2 pinFences=2 mixedRefused=6 rollback=true duplicateSafe=true changedGuardRefused=true");
    }
}
