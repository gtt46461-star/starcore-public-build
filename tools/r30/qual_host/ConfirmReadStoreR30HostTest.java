package com.ethk3.novaonepro.strategyv2;

import android.database.sqlite.SQLiteDatabase;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;
import sun.misc.Unsafe;

/** Exercise the production reservation/restore path on SQLite, including process death before replay. */
public final class ConfirmReadStoreR30HostTest {
    static void require(boolean ok, String code) { if (!ok) throw new AssertionError(code); }
    static void mustReject(Runnable r) {
        try { r.run(); throw new AssertionError("REUSED_CONFIRM_ACCEPTED"); }
        catch (IllegalStateException e) {
            StringBuilder chain = new StringBuilder();
            for (Throwable p = e; p != null && p.getCause() != p; p = p.getCause()) chain.append(p.getMessage());
            require(chain.toString().contains(HoldoutLedgerR27.CONFIRM_REUSED), "WRONG_REJECTION:" + chain);
        }
    }
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            Path database = Files.createTempFile("r30-confirm-restart-", ".db");
            try { child("write-and-kill", database); child("restart-and-check", database); }
            finally { Files.deleteIfExists(database); }
            return;
        }
        Class.forName("org.sqlite.JDBC");
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + args[1])) {
            if (args[0].equals("write-and-kill")) try (Statement s = c.createStatement()) {
                s.execute(HoldoutLedgerR27.CONFIRM_DDL);
                s.execute("CREATE TABLE r27_holdout_exposure(block_id INTEGER NOT NULL,policy_hash TEXT NOT NULL,looked_at INTEGER NOT NULL,PRIMARY KEY(block_id,policy_hash))");
                s.execute("INSERT INTO r27_holdout_exposure VALUES(8,'old',1)");
            }
            SQLiteDatabase db = new SQLiteDatabase(c);
            Field f = Unsafe.class.getDeclaredField("theUnsafe"); f.setAccessible(true); Unsafe u = (Unsafe) f.get(null);
            ResearchPoolR27 pool = (ResearchPoolR27) u.allocateInstance(ResearchPoolR27.class);
            ResearchWriteQueueR27 writes = new ResearchWriteQueueR27((op, batch) -> {
                try {
                    c.setAutoCommit(false); ResearchWriteQueueR27.execute(db, batch, op); c.commit(); c.setAutoCommit(true);
                } catch (Exception e) {
                    try { c.rollback(); c.setAutoCommit(true); } catch (Exception ignored) { }
                    throw new IllegalStateException(e);
                }
            }, () -> false);
            u.putObject(pool, u.objectFieldOffset(ResearchPoolR27.class.getDeclaredField("writes")), writes);
            ResearchPoolR27.Worker worker = pool.new Worker(db, "confirm-test");
            if (args[0].equals("write-and-kill")) {
                HoldoutLedgerR27 first = new HoldoutLedgerR27(); ConfirmReadStoreR30.restore(db, first); ConfirmReadStoreR30.bind(worker, first);
                first.confirm("p", "d", 7, 10, 20); first.confirm("control", "d", 7, 10, 20, true);
                try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT count(*) FROM r30_confirm_read")) { r.next(); require(r.getInt(1) == 2, "RESERVATION_NOT_COMMITTED"); }
                System.out.println("R30_CONFIRM_SQLITE|RESERVED_BEFORE_READ|HALT_PROCESS"); System.out.flush();
                // Abrupt process death after the committed reservation, before any confirmation sample is read.
                Runtime.getRuntime().halt(0);
            }
            HoldoutLedgerR27 restarted = new HoldoutLedgerR27(); ConfirmReadStoreR30.restore(db, restarted); ConfirmReadStoreR30.bind(worker, restarted);
            mustReject(() -> restarted.confirm("p", "d", 7, 10, 20));
            mustReject(() -> restarted.confirm("old", "unknown-dataset", 8, 10, 20));
            require(restarted.exposures(7) == 1 && restarted.controlExposures(7) == 1, "CONTROL_EXPOSURE_BUDGET_CHANGED");
            require(restarted.exposures(8) == 1, "LEGACY_EXPOSURE_NOT_RESTORED_TO_BUDGET");
            restarted.confirm("p", "different-dataset", 7, 10, 20);
            // A second in-memory ledger that missed a concurrent commit is stopped by the writer transaction.
            HoldoutLedgerR27 stale = new HoldoutLedgerR27(); ConfirmReadStoreR30.bind(worker, stale);
            mustReject(() -> stale.confirm("p", "d", 7, 10, 20));
            mustReject(() -> stale.confirm("old", "unknown-dataset", 8, 10, 20));
            System.out.println("R30_CONFIRM_SQLITE|COMMIT_BEFORE_READ|PROCESS_RESTART_REJECTS|LEGACY_BLOCKED|STALE_WRITER_REJECTS|CONTROL_BUDGET_PRESERVED|PASS");
        }
    }

    static void child(String mode, Path database) throws Exception {
        Process p = new ProcessBuilder(System.getProperty("java.home") + "/bin/java", "-cp", System.getProperty("java.class.path"),
                ConfirmReadStoreR30HostTest.class.getName(), mode, database.toString()).redirectErrorStream(true).start();
        if (!p.waitFor(30, TimeUnit.SECONDS)) { p.destroyForcibly(); throw new AssertionError("CONFIRM_CHILD_TIMEOUT:" + mode); }
        String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        System.out.print(output);
        require(p.exitValue() == 0, "CONFIRM_CHILD_FAILED:" + mode + ":" + p.exitValue());
    }
}
