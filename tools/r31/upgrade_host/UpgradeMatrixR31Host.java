package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.strategyv2.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * R31-B12-C01 HOST upgrade matrix on native SQLite (not ART, not a device, not an old APK).
 *
 * <p>seed: builds a predecessor install with the real production schema methods (legacy base, Critical stamp,
 * R929 platform, R26 runtime, SU1/SU2, the R27-era tables) and writes protected roots: draws, models
 * (r27_policy, r27_live_shadow, r27_atomnet_state), frozen predictions and certificate roots.
 * upgrade: runs the production startup upgrade (RuntimeCoordinatorR26.startupUpgradeSpecR31) through the
 * production journal; with a kill index the JVM halts (no finally, no commit) at that durable boundary.
 * verify: prints root hashes over the seed columns, the upgrade journal, every R926 step attempt and the schema.</p>
 */
public final class UpgradeMatrixR31Host {
    interface Body { void run() throws Exception; }
    static void tx(SQLiteDatabase db, Body body) throws Exception { db.beginTransaction(); try { body.run(); db.setTransactionSuccessful(); } finally { db.endTransaction(); } }
    static long count(SQLiteDatabase db, String sql) { try (Cursor c = db.rawQuery(sql, null)) { return c.moveToFirst() ? c.getLong(0) : -1; } }

    static SchemaDomainCoordinatorR926.WriterR31 writer(SQLiteDatabase db) {
        return (label, body) -> { db.beginTransaction(); try { body.run(db); db.setTransactionSuccessful(); } finally { db.endTransaction(); } SQLiteDatabase.trace.clear(); };
    }

    // ---- predecessor install + protected roots ------------------------------------------------------------
    static void seed(Path file, String scenario) throws Exception {
        MainChainSchemaR31Host.Owner owner = new MainChainSchemaR31Host.Owner(file); SQLiteDatabase db = owner.db;
        try {
            MainChainSchemaR31Host.createLegacyBase(owner);
            tx(db, () -> { SchemaDomainCoordinatorR926.ensureInfrastructure(db); SchemaDomainCoordinatorR926.verifyAndStamp(db, SchemaDomainCoordinatorR926.criticalContract()); });
            if (scenario.equals("EMPTY")) { SQLiteDatabase.trace.clear(); return; }  // first overlay onto a fresh base install: no R26/R27 yet
            tx(db, () -> SchemaDomainCoordinatorR926.migrateR31TransportTiming(db));
            tx(db, () -> SchemaDomainCoordinatorR926.migrateR31PlatformPrerequisites(db));
            tx(db, () -> RuntimeSchemaR26.ensure(db));
            { int guard = 0; while (!StrategySchemaSU1.advance(db)) if (++guard > SqlStatementsSU1.SQL.length + 16) throw new AssertionError("SU1_NOT_CONVERGED"); }
            tx(db, () -> StrategySchemaSU2.ensure(db));
            tx(db, () -> { for (String sql : SchemaR27.DDL) db.execSQL(sql); });  // R27-era tables only; the R30/R31 domains come with the upgrade
            int draws = scenario.equals("STRESS") ? 20000 : 600, models = scenario.equals("STRESS") ? 2000 : 60, frozen = scenario.equals("STRESS") ? 6000 : 240;
            int blob = scenario.equals("STRESS") ? 4 << 20 : 256 << 10;
            tx(db, () -> {
                for (int i = 0; i < draws; i++) {
                    String issue = String.format(Locale.ROOT, "2026%04d%04d", 101 + i / 400, 1 + i % 400);
                    Object[] v = new Object[13]; v[0] = issue; v[1] = 1_700_000_000_000L + 180_000L * i; v[2] = 0;
                    for (int k = 0; k < 10; k++) v[3 + k] = 1 + (k + i) % 10;
                    db.execSQL("INSERT INTO draws(issue,open_time,dirty,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)", v);
                    if (i % 2000 == 1999) SQLiteDatabase.trace.clear();
                }
                rows(db, "r27_policy", models, 0); rows(db, "r27_live_shadow", Math.max(4, models / 10), 2048);
                rows(db, "r27_live_frozen", frozen, 128); rows(db, "r27_certificate", Math.max(8, models / 5), 0);
                rows(db, "r27_atomnet_state", 2, blob);
            });
            SQLiteDatabase.trace.clear();
        } finally { db.close(); }
    }
    /** Deterministic rows for every column of a real table (NOT NULL columns all filled; defaults kept where declared). */
    static void rows(SQLiteDatabase db, String table, int n, int blobBytes) {
        List<String[]> cols = new ArrayList<>();
        try (Cursor c = db.rawQuery("PRAGMA table_info(" + table + ")", null)) { while (c.moveToNext()) cols.add(new String[] {c.getString(1), String.valueOf(c.getString(2)).toUpperCase(Locale.ROOT), c.getString(4)}); }
        StringBuilder names = new StringBuilder(), marks = new StringBuilder(); List<String[]> used = new ArrayList<>();
        for (String[] col : cols) { if (col[2] != null) continue; if (names.length() > 0) { names.append(','); marks.append(','); } names.append(col[0]); marks.append('?'); used.add(col); }
        for (int i = 0; i < n; i++) {
            Object[] v = new Object[used.size()];
            for (int k = 0; k < v.length; k++) {
                String type = used.get(k)[1], name = used.get(k)[0];
                if (type.contains("BLOB")) { byte[] b = new byte[Math.max(1, blobBytes)]; for (int x = 0; x < b.length; x++) b[x] = (byte) (x * 31 + i * 7 + k); v[k] = b; }
                else if (type.contains("INT")) v[k] = (long) (i * 3 + k);
                else if (type.contains("REAL")) v[k] = .25 + i * .001 + k;
                else v[k] = name + "-" + table + "-" + i;
            }
            db.execSQL("INSERT INTO " + table + "(" + names + ") VALUES(" + marks + ")", v);
        }
    }

    // ---- production upgrade with a kill switch -----------------------------------------------------------
    static int cuts;
    static void upgrade(Path file, int killAt, long freeBytes, boolean tamper) throws Exception {
        MainChainSchemaR31Host.Owner owner = new MainChainSchemaR31Host.Owner(file); SQLiteDatabase db = owner.db;
        SchemaDomainCoordinatorR926.installCutPointsR31((domain, step, point) -> {
            cuts++;
            if (killAt == 0) System.out.println("CUT|" + cuts + "|" + domain + "|" + step + "|" + point);
            if (cuts == killAt) { System.out.println("KILLED_AT|" + cuts + "|" + domain + "|" + step + "|" + point); System.out.flush(); Runtime.getRuntime().halt(137); }
        });
        SchemaDomainCoordinatorR926.UpgradeSpecR31 production = RuntimeCoordinatorR26.startupUpgradeSpecR31("host-upgrade-owner");
        SchemaDomainCoordinatorR926.Step transform = tamper ? d -> { production.transform.run(d); d.execSQL("UPDATE r27_certificate SET status='HOST_TAMPER' WHERE rowid=(SELECT min(rowid) FROM r27_certificate)"); } : production.transform;
        SchemaDomainCoordinatorR926.UpgradeSpecR31 spec = new SchemaDomainCoordinatorR926.UpgradeSpecR31(production.id, production.revision, production.contractHash,
                production.roots, production.requiredFreeBytes, freeBytes < 0 ? null : d -> freeBytes, transform, production.cleanup);
        try {
            SchemaDomainCoordinatorR926.UpgradeOutcomeR31 out = SchemaDomainCoordinatorR926.runUpgradeR31(db, writer(db), spec);
            System.out.println("UPGRADE|" + out.state + "|" + out.domain + "|cuts=" + cuts + "|attempts=" + out.attempts + "|forward=" + out.forwardCompatibility);
        } catch (Exception failure) {
            System.out.println("UPGRADE_FAILED|" + failure.getMessage());
        } finally { db.close(); }
    }

    // ---- facts after the run -----------------------------------------------------------------------------
    static void verify(Path file, String seedColumns) throws Exception {
        MainChainSchemaR31Host.Owner owner = new MainChainSchemaR31Host.Owner(file); SQLiteDatabase db = owner.db;
        try {
            Map<String, Object> out = new LinkedHashMap<>();
            String columns = seedColumns.isEmpty() ? SchemaDomainCoordinatorR926.rootColumnsR31(db, SchemaDomainCoordinatorR926.PROTECTED_ROOTS_R31) : seedColumns;
            out.put("rootColumns", columns);
            out.put("roots", SchemaDomainCoordinatorR926.tableHashesR31(db, columns));
            Map<String, Object> journal = new TreeMap<>(), steps = new TreeMap<>(), stamps = new TreeMap<>();
            try (Cursor c = db.rawQuery("SELECT domain,step_id,status,attempt,after_hash FROM " + SchemaDomainCoordinatorR926.TABLE_JOURNAL, null)) {
                while (c.moveToNext()) {
                    Map<String, Object> row = new LinkedHashMap<>(); row.put("status", c.getString(2)); row.put("attempt", c.getLong(3));
                    if (c.getString(0).startsWith(SchemaDomainCoordinatorR926.UPGRADE_PREFIX_R31)) { row.put("hash", c.getString(4)); journal.put(c.getString(1), row); }
                    else steps.put(c.getString(0) + "/" + c.getString(1), row);
                }
            }
            try (Cursor c = db.rawQuery("SELECT domain,state,fingerprint FROM " + SchemaDomainCoordinatorR926.TABLE_DOMAIN, null)) {
                while (c.moveToNext()) stamps.put(c.getString(0).startsWith(SchemaDomainCoordinatorR926.UPGRADE_PREFIX_R31) ? "UPGRADE" : c.getString(0), c.getString(1) + "|" + c.getString(2));
            }
            out.put("upgradeJournal", journal); out.put("steps", steps); out.put("stamps", stamps);
            out.put("critical", SchemaDomainCoordinatorR926.matchReason(db, SchemaDomainCoordinatorR926.criticalContract()));
            StringBuilder schema = new StringBuilder();
            try (Cursor c = db.rawQuery("SELECT type,name,sql FROM sqlite_master WHERE name NOT LIKE 'sqlite_%' ORDER BY type,name", null)) { while (c.moveToNext()) schema.append(c.getString(0)).append(':').append(c.getString(1)).append('=').append(c.getString(2)).append('\n'); }
            out.put("schemaHash", StatsR27.sha256(schema.toString())); out.put("schemaObjects", count(db, "SELECT count(*) FROM sqlite_master"));
            out.put("userVersion", count(db, "PRAGMA user_version")); out.put("integrity", MainChainSchemaR31Host.value(db, "PRAGMA integrity_check"));
            out.put("r26Sessions", count(db, "SELECT count(*) FROM sqlite_master WHERE type='table' AND name='r26_session'") > 0 ? count(db, "SELECT count(*) FROM r26_session WHERE state='RUNNING'") : 0L);
            System.out.println("VERIFY|" + JsonR27.write(out));
        } finally { db.close(); }
    }

    public static void main(String[] args) throws Exception {
        Class.forName("org.sqlite.JDBC");
        Path file = Paths.get(args[1]);
        switch (args[0]) {
            case "seed": seed(file, args[2]); System.out.println("SEEDED|" + args[2]); break;
            case "upgrade": upgrade(file, Integer.parseInt(args[2]), Long.parseLong(args[3]), args.length > 4 && args[4].equals("tamper")); break;
            case "verify": verify(file, args.length > 2 ? new String(Files.readAllBytes(Paths.get(args[2])), StandardCharsets.UTF_8).trim() : ""); break;
            default: throw new IllegalArgumentException(args[0]);
        }
        System.out.flush(); System.exit(0);
    }
}
