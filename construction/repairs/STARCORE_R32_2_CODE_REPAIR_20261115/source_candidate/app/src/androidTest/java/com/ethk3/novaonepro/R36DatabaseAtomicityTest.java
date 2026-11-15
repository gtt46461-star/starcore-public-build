package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public final class R36DatabaseAtomicityTest {
    private DatabaseCoordinator owner;

    @Before public void openThroughProductionStartupOwner() {
        // Cold schema initialization has its own production startup deadline.
        // Do not leak a second raw DbHelper, or make a 250ms ordinary writer probe
        // responsible for cold initialization. The process owns this coordinator.
        DbStore store = new DbStore(ApplicationProvider.getApplicationContext());
        owner = store.coordinatorR36();
        assertTrue(owner.isCriticalSchemaReady());
    }

    @Test public void openedDatabaseIsSchema36WithDurableTaskTables(){
        SQLiteDatabase db=owner.writable();
        assertEquals(36,db.getVersion());
        assertTrue(tableExists(db,DbHelper.TABLE_R36_ANALYSIS_REQUEST));
        assertTrue(tableExists(db,DbHelper.TABLE_R36_SETTLEMENT_TASK));
    }

    @Test public void coordinatorRollsBackInjectedCanonicalFailure(){
        final DatabaseCoordinator coordinator=owner;
        assertTrue(coordinator.runWriteOrThrowF01("R36_TEST_TABLE",db -> {
            db.execSQL("CREATE TABLE IF NOT EXISTS r36_atomicity_probe(id INTEGER PRIMARY KEY,value TEXT)");
            db.delete("r36_atomicity_probe",null,null);
            return true;
        }));
        DatabaseCoordinator.TransactionResult<Boolean> result=coordinator.runCanonicalTransactionR36(
                "202608100001","TEST-ATTEMPT-ATOMICITY",new DatabaseCoordinator.TransactionCallable<Boolean>(){
                    @Override public Boolean call(SQLiteDatabase db,DatabaseCoordinator.TransactionContext tx){
                        db.execSQL("INSERT INTO r36_atomicity_probe(id,value) VALUES(1,'before-crash')");
                        throw new IllegalStateException("INJECTED_CRASH");
                    }
                },Boolean.FALSE);
        assertFalse(result.success);
        Cursor c=coordinator.readable().rawQuery("SELECT COUNT(1) FROM r36_atomicity_probe",null);
        try{assertTrue(c.moveToFirst());assertEquals(0,c.getInt(0));}finally{c.close();}
    }

    @Test public void explicitAtomicWriteMatchesProductionBoundaryAndRollsBack() {
        final DatabaseCoordinator c=owner;
        assertFalse(c.runWriteOrThrowF01("R939_WRITER_IS_NOT_TRANSACTION",new DatabaseCoordinator.WriteCallable<Boolean>() {
            public Boolean call(SQLiteDatabase db) { return db.inTransaction(); }
        }));
        assertTrue(c.runAtomicWrite("R939_ATOMIC_PROBE_SCHEMA",new DatabaseCoordinator.WriteRunnable() {
            public void run(SQLiteDatabase db) {
                assertTrue(db.inTransaction());
                db.execSQL("CREATE TABLE IF NOT EXISTS r939_atomic_probe(id INTEGER PRIMARY KEY,value TEXT)");
                db.execSQL("DELETE FROM r939_atomic_probe");
            }
        }));
        assertFalse(c.runAtomicWrite("R939_ATOMIC_PROBE_FAIL",new DatabaseCoordinator.WriteRunnable() {
            public void run(SQLiteDatabase db) {
                db.execSQL("INSERT INTO r939_atomic_probe VALUES(1,'must-rollback')");
                throw new IllegalStateException("R939_INJECTED_SECOND_WRITE_FAILURE");
            }
        }));
        try(Cursor rows=c.readable().rawQuery("SELECT COUNT(*) FROM r939_atomic_probe",null)) {
            assertTrue(rows.moveToFirst());assertEquals(0,rows.getInt(0));
        }
        assertTrue(c.runAtomicWrite("R939_ATOMIC_PROBE_COMMIT",new DatabaseCoordinator.WriteRunnable() {
            public void run(SQLiteDatabase db) { db.execSQL("INSERT INTO r939_atomic_probe VALUES(2,'committed')"); }
        }));
        try(Cursor rows=c.readable().rawQuery("SELECT value FROM r939_atomic_probe WHERE id=2",null)) {
            assertTrue(rows.moveToFirst());assertEquals("committed",rows.getString(0));
        }
    }

    private static boolean tableExists(SQLiteDatabase db,String table){Cursor c=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",new String[]{table});try{return c.moveToFirst();}finally{c.close();}}
}
