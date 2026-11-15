package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * R31-B12-C02#A3a HOST full-chain fault injection on ONE shared native SQLite database and one source commit.
 * Combinations (each runs on the same file, in this order, and is re-entered after a real JVM halt):
 *   B01+B02  production claim/fence + immutable owner submit under writer contention and a stale late commit
 *   B03      production settlement (LiveShadowsR27.settle) with out-of-order draws and an injected failed commit
 *   B05+B10  persistent mainline choice + the production DISPLAY-only cache GC with an injected disk-full commit
 *   B08+B09  research product consumption: one consumer product per result, duplicate and late labels refused
 * Invariants after every phase: no duplicate owner commit, no duplicate settlement, no out-of-range prediction,
 * no protected-asset deletion. Fault ids are the ones in tools/r31/fault_injection_manifest.json.
 * Engineering HOST evidence only: not ART, not a device, not natural quality.
 */
public final class R31FullChainFaultHost {
    static int checks; static final List<String> facts = new ArrayList<>();
    static void need(boolean ok, String why) { checks++; if (!ok) throw new AssertionError(why); }
    static long n(SQLiteDatabase db, String sql, Object... args) {
        String[] a = new String[args.length]; for (int i = 0; i < a.length; i++) a[i] = String.valueOf(args[i]);
        try (Cursor c = db.rawQuery(sql, a)) { return c.moveToFirst() ? c.getLong(0) : -1; }
    }
    static String s(SQLiteDatabase db, String sql, Object... args) {
        String[] a = new String[args.length]; for (int i = 0; i < a.length; i++) a[i] = String.valueOf(args[i]);
        try (Cursor c = db.rawQuery(sql, a)) { return c.moveToFirst() ? c.getString(0) : null; }
    }
    static void write(R31OwnedRuntimeFixture f, String op, DatabaseCoordinator.WriteCallable<Integer> body) {
        f.coordinator.runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.MIGRATION, op), body);
    }
    static final String TASK = "r27-live", TASK_B = "r27-atoms";
    static final String[] TARGETS = {"202611080101", "202611080102", "202611080103", "202611080104"};
    static final String POLICY = StatsR27.sha256("full-chain-policy");

    // ------------------------------------------------------------------ fixture data (idempotent)
    static void baseSchema(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS draws(issue TEXT PRIMARY KEY,dirty INT,open_time INTEGER,n1 INT,n2 INT,n3 INT,n4 INT,n5 INT,n6 INT,n7 INT,n8 INT,n9 INT,n10 INT)");
    }
    static void fixtures(R31OwnedRuntimeFixture f) {
        write(f, "R31_FULL_CHAIN_FIXTURE", db -> {
            db.execSQL("CREATE TABLE IF NOT EXISTS host_full_chain_artifact(task_id TEXT NOT NULL,seq INTEGER NOT NULL,fence INTEGER NOT NULL,PRIMARY KEY(task_id,seq))");
            db.execSQL("CREATE TABLE IF NOT EXISTS host_full_chain_consumed(consume_key TEXT PRIMARY KEY,product_hash TEXT NOT NULL)");
            for (int i = 0; i < TARGETS.length; i++) {
                long ord = TargetIssueResolver.sequenceOrdinalSU1(TARGETS[i]);
                byte[] p = new byte[80]; for (int k = 0; k < p.length; k++) p[k] = (byte) (k + i);
                db.execSQL("INSERT OR IGNORE INTO r27_live_frozen(policy_hash,target_ordinal,target_issue,position,probabilities,n1,n2,n3,act,status,frozen_at,deadline_at) VALUES(?,?,?,?,?,?,?,?,1,'FROZEN',?,?)",
                        new Object[] {POLICY, ord, TARGETS[i], 1 + i, p, 1 + i, 2 + i, 3 + i, 1_000L + i, 2_000L + i});
            }
            db.execSQL("INSERT OR IGNORE INTO r27_certificate(policy_hash,status,gates_json,evidence_digest,issued_at,expires_at,gate_version) VALUES(?,'CERTIFIED','{}',?,1,9999999999999,'R31_FIXTURE')", new Object[] {POLICY, StatsR27.sha256("evidence")});
            for (String panel : new String[] {"p01_three_scores", "p13_runtime_health"})
                for (int k = 0; k < 3; k++) db.execSQL("INSERT OR REPLACE INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES(?,?,?,?)", new Object[] {panel, "k" + k, "{}", 10 + k});
            db.execSQL("INSERT OR REPLACE INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES('mainline','choice','{\"policy\":\"" + POLICY + "\"}',1)");
            return 0;
        });
    }
    static void draw(R31OwnedRuntimeFixture f, String issue, int first) {
        write(f, "R31_FULL_CHAIN_DRAW", db -> {
            Object[] v = new Object[13]; v[0] = issue; v[1] = 0; v[2] = 1_700_000_000_000L;
            for (int k = 0; k < 10; k++) v[3 + k] = 1 + (first - 1 + k) % 10;
            db.execSQL("INSERT OR IGNORE INTO draws(issue,dirty,open_time,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)", v);
            return 0;
        });
    }

    // ------------------------------------------------------------------ invariants
    static Map<String, String> protectedAssets(SQLiteDatabase db) {
        Map<String, String> m = new TreeMap<>();
        m.put("draws", s(db, "SELECT count(*)||':'||coalesce(group_concat(issue||'='||n1||n2||n3||n4||n5||n6||n7||n8||n9||n10,','),'') FROM (SELECT * FROM draws ORDER BY issue)"));
        m.put("certificate", s(db, "SELECT count(*)||':'||coalesce(group_concat(policy_hash||'='||status||evidence_digest,','),'') FROM (SELECT * FROM r27_certificate ORDER BY policy_hash)"));
        m.put("frozen", s(db, "SELECT count(*)||':'||coalesce(group_concat(target_ordinal||'='||hex(probabilities)||n1||n2||n3||frozen_at,','),'') FROM (SELECT * FROM r27_live_frozen ORDER BY target_ordinal)"));
        m.put("mainlineChoice", s(db, "SELECT count(*)||':'||coalesce(group_concat(choice_id||'='||policy_hash,','),'') FROM (SELECT * FROM r31_mainline_choice ORDER BY choice_id)"));
        m.put("legacyProjection", s(db, "SELECT count(*) FROM r27_chart_cache WHERE panel_id='mainline' AND key='choice'"));
        return m;
    }
    static void invariants(SQLiteDatabase db, String phase, Map<String, String> protectedBefore) {
        need(n(db, "SELECT count(*)-count(DISTINCT task_id||'|'||owner_epoch||'|'||fence||'|'||chunk_seq) FROM r31_commit_receipt") == 0, phase + ": duplicate owner commit receipt");
        need(n(db, "SELECT count(*)-count(DISTINCT task_id||'|'||seq) FROM host_full_chain_artifact") == 0, phase + ": duplicate owner artifact");
        need(n(db, "SELECT count(*) FROM host_full_chain_artifact a WHERE NOT EXISTS(SELECT 1 FROM r31_commit_receipt r WHERE r.task_id=a.task_id AND r.fence=a.fence)") == 0, phase + ": artifact without its owner receipt");
        need(n(db, "SELECT count(*) FROM r27_live_frozen WHERE position NOT BETWEEN 1 AND 10 OR n1 NOT BETWEEN 1 AND 10 OR n2 NOT BETWEEN 1 AND 10 OR n3 NOT BETWEEN 1 AND 10 OR n1 IN (n2,n3) OR n2=n3 OR length(probabilities)=0 OR hit NOT IN (-2,-1,0,1)") == 0, phase + ": out-of-range prediction");
        // a settled prediction has exactly the hit its frozen numbers and the drawn number imply
        need(n(db, "SELECT count(*) FROM r27_live_frozen f JOIN draws d ON d.issue=f.target_issue AND d.dirty=0 WHERE f.hit IN (0,1) AND f.hit<>(CASE WHEN (CASE f.position WHEN 1 THEN d.n1 WHEN 2 THEN d.n2 WHEN 3 THEN d.n3 WHEN 4 THEN d.n4 ELSE -1 END) IN (f.n1,f.n2,f.n3) THEN 1 ELSE 0 END)") == 0, phase + ": wrong settlement");
        need(n(db, "SELECT count(*)-count(DISTINCT consume_key) FROM host_full_chain_consumed") == 0, phase + ": duplicate consumption");
        if (protectedBefore != null) {
            Map<String, String> now = protectedAssets(db);
            for (String k : new String[] {"certificate", "frozen", "legacyProjection"})
                need(now.get(k).equals(protectedBefore.get(k)), phase + ": protected asset changed or deleted: " + k);
            for (String k : new String[] {"draws", "mainlineChoice"})
                need(Long.parseLong(now.get(k).split(":", 2)[0]) >= Long.parseLong(protectedBefore.get(k).split(":", 2)[0]), phase + ": protected rows deleted: " + k);
        }
    }

    // ------------------------------------------------------------------ owners
    interface Phase { void run(R31OwnedRuntimeFixture f) throws Exception; }
    static DbStore store(R31OwnedRuntimeFixture f) throws Exception { Field field = R31OwnedRuntimeFixture.class.getDeclaredField("store"); field.setAccessible(true); return (DbStore) field.get(f); }
    /** A new owner takes the task only through the production fenced retry (never by editing a lease). */
    static void asNewOwner(SQLiteDatabase db, String task, Phase body) throws Exception {
        try (R31OwnedRuntimeFixture f = new R31OwnedRuntimeFixture(db, task)) { ResearchPoolR27.retry(store(f), task); body.run(f); }
    }

    // ------------------------------------------------------------------ B01+B02: owner protocol
    static void ownerCommits(R31OwnedRuntimeFixture f, String task, int upTo) throws Exception {
        while (true) {
            long next = n(f.db, "SELECT coalesce(max(seq)+1,0) FROM host_full_chain_artifact WHERE task_id=?", task);
            if (next >= upTo) return;
            f.owned(() -> {
                RuntimeCoordinatorR26.Claim claim = RuntimeCoordinatorR26.currentClaimR30();
                ResearchWriteQueueR27.Batch b = new ResearchWriteQueueR27.Batch();
                b.add("INSERT INTO host_full_chain_artifact(task_id,seq,fence) VALUES(?,?,?)", task, next, claim.fence);
                RuntimeR27.submit(f.worker, "R31_FULL_CHAIN_OWNER_COMMIT", b, false, "FULL_CHAIN_SEQ_" + next);
                return 0;
            });
        }
    }
    /** F01 writer contention: two owners, two connections, one sole writer; each logical commit lands exactly once. */
    static void contention(Path file, R31OwnedRuntimeFixture a) throws Exception {
        try (Connection second = DriverManager.getConnection("jdbc:sqlite:" + file)) {
            SQLiteDatabase db2 = new SQLiteDatabase(second); db2.execSQL("PRAGMA busy_timeout=20000");
            try (R31OwnedRuntimeFixture b = new R31OwnedRuntimeFixture(db2, TASK_B)) {
                ResearchPoolR27.retry(store(b), TASK_B);
                ExecutorService pool = Executors.newFixedThreadPool(2);
                try {
                    Future<?> x = pool.submit(() -> { ownerCommits(a, TASK, 24); return null; });
                    Future<?> y = pool.submit(() -> { ownerCommits(b, TASK_B, 24); return null; });
                    x.get(300, TimeUnit.SECONDS); y.get(300, TimeUnit.SECONDS);
                } finally { pool.shutdownNow(); }
            }
        }
        need(n(a.db, "SELECT count(*) FROM host_full_chain_artifact WHERE task_id=?", TASK) == 24 && n(a.db, "SELECT count(*) FROM host_full_chain_artifact WHERE task_id=?", TASK_B) == 24, "F01 both owners committed every sequence once");
        facts.add("F01_LOCK_CONTENTION two owners x24 commits, one writer");
    }
    /** F03/F09: an owner superseded by the production fenced retry cannot land a late commit. */
    static void staleLateCommit(R31OwnedRuntimeFixture a) throws Exception {
        final RuntimeCoordinatorR26.Claim[] old = new RuntimeCoordinatorR26.Claim[1];
        a.owned(() -> { old[0] = RuntimeCoordinatorR26.currentClaimR30(); return 0; });
        long before = n(a.db, "SELECT count(*) FROM host_full_chain_artifact WHERE task_id=?", TASK);
        asNewOwner(a.db, TASK, b -> ownerCommits(b, TASK, (int) before + 1));  // the new owner (fresh fence) commits the next sequence
        boolean refused = false; String why = "";
        try {
            a.coordinator.runAtomicWriteOrThrowF01(RuntimeCoordinatorR26.controlContextR31("R31_FULL_CHAIN_LATE_COMMIT"), db -> {
                RuntimeCoordinatorR26.requireFence(db, old[0]);
                db.execSQL("INSERT INTO host_full_chain_artifact(task_id,seq,fence) VALUES(?,?,?)", new Object[] {TASK, 99, old[0].fence});
                return 0;
            });
        } catch (RuntimeException expected) {
            why = String.valueOf(expected.getMessage());
            refused = FailureDispositionR31.classify(expected).disposition == FailureDispositionR31.Disposition.STALE_OWNER || why.contains("FENCE") || why.contains("STALE");
        }
        need(refused, "F03 stale owner late commit refused: " + why);
        need(n(a.db, "SELECT count(*) FROM host_full_chain_artifact WHERE task_id=?", TASK) == before + 1 && n(a.db, "SELECT count(*) FROM host_full_chain_artifact WHERE seq=99") == 0, "F09 late commit left nothing; only the new owner's commit landed");
        facts.add("F03_STALE_FENCE/F09_LATE_COMMIT refused after production fenced retry: " + why);
    }

    // ------------------------------------------------------------------ B03: settlement
    static Map<String, Long> settledAt(SQLiteDatabase db) {
        Map<String, Long> m = new TreeMap<>();
        try (Cursor c = db.rawQuery("SELECT target_issue,settled_at FROM r27_live_frozen WHERE hit IN (0,1)", null)) { while (c.moveToNext()) m.put(c.getString(0), c.getLong(1)); }
        return m;
    }
    static void settle(R31OwnedRuntimeFixture f, long latest) throws Exception {
        f.owned(() -> { LiveShadowsR27.settle(f.runtime, f.worker, latest); return 0; });
    }
    static void settlement(R31OwnedRuntimeFixture f) throws Exception {
        long last = TargetIssueResolver.sequenceOrdinalSU1(TARGETS[TARGETS.length - 1]);
        // F05 out of order: the later draws arrive first
        draw(f, TARGETS[3], 4); draw(f, TARGETS[2], 3);
        settle(f, last);
        Map<String, Long> first = settledAt(f.db);
        need(first.containsKey(TARGETS[3]) && first.containsKey(TARGETS[2]) && !first.containsKey(TARGETS[0]), "F05 the available out-of-order draws settle; the missing ones wait: " + first);
        // an injected failure immediately before the next settlement COMMIT rolls the whole chunk back
        draw(f, TARGETS[0], 1); draw(f, TARGETS[1], 7);
        AtomicInteger injected = new AtomicInteger();
        f.db.beforeSuccessful = () -> { if (RuntimeCoordinatorR26.currentClaimR30() != null && injected.getAndIncrement() == 0) throw new IllegalStateException("HOST_INJECTED_SETTLEMENT_COMMIT_FAILURE"); };
        try { settle(f, last); } catch (RuntimeException expectedOrRetried) { facts.add("F02_SETTLEMENT_COMMIT_FAILURE " + expectedOrRetried.getMessage()); }
        finally { f.db.beforeSuccessful = null; }
        need(injected.get() >= 1, "settlement commit failure was injected");
        settle(f, last); settle(f, last);  // re-entry, then a repeated pass: nothing is settled twice
        Map<String, Long> all = settledAt(f.db);
        need(all.size() == TARGETS.length, "every target with a draw settled: " + all);
        for (Map.Entry<String, Long> e : first.entrySet()) need(e.getValue().equals(all.get(e.getKey())), "no duplicate settlement (settled_at unchanged) " + e.getKey());
        facts.add("F05_OUT_OF_ORDER/B03 settlement once per prediction after injected commit failure");
    }

    // ------------------------------------------------------------------ B05 GC + B10 persistent choice
    static void gcAndChoice(R31OwnedRuntimeFixture f) {
        write(f, "R31_FULL_CHAIN_MAINLINE_CHOICE", db -> {
            if (n(db, "SELECT count(*) FROM r31_mainline_choice") == 0)
                MainlineChoiceStoreR31.appendChoice(db, POLICY, "exp-full-chain", "GATE_V2", StatsR27.sha256("evidence"), StatsR27.sha256("model"), 1, TargetIssueResolver.sequenceOrdinalSU1(TARGETS[0]), 1, 1);
            return 0;
        });
        MainlineChoiceStoreR31.Choice before = MainlineChoiceStoreR31.current(f.db);
        need(before != null && POLICY.equals(before.policyHash), "B10 choice persisted");
        Map<String, String> assets = protectedAssets(f.db);
        // F04 disk full during the GC commit: rolled back, nothing partially deleted
        f.db.beforeSuccessful = () -> { throw new android.database.sqlite.SQLiteException("database or disk is full (code 13 SQLITE_FULL)"); };
        boolean full = false;
        try { f.coordinator.runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.RETENTION, "R26_ARTIFACT_RETENTION"), db -> { db.execSQL(ResearchRetentionStoreR30.visualCacheDeleteSql()); return 0; }); }
        catch (RuntimeException expected) { full = true; }
        finally { f.db.beforeSuccessful = null; }
        need(full, "F04 disk-full GC commit surfaced");
        need(n(f.db, "SELECT count(*) FROM r27_chart_cache WHERE panel_id='p01_three_scores'") >= 3, "F04 rolled-back GC deleted nothing");
        f.coordinator.runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.RETENTION, "R26_ARTIFACT_RETENTION"), db -> { db.execSQL(ResearchRetentionStoreR30.visualCacheDeleteSql()); return 0; });
        need(n(f.db, "SELECT count(*) FROM r27_chart_cache WHERE panel_id IN ('p01_three_scores','p13_runtime_health')") == 0, "B05 DISPLAY cache collected");
        need(n(f.db, "SELECT count(*) FROM r27_chart_cache WHERE panel_id='mainline' AND key='choice'") == 1, "B05 GC never touches the mainline projection");
        MainlineChoiceStoreR31.Choice after = MainlineChoiceStoreR31.current(f.db);
        need(after != null && after.choiceId.equals(before.choiceId), "B10 persistent choice survives GC");
        need(protectedAssets(f.db).equals(assets), "B05 GC changed no protected asset");
        facts.add("F04_DISK_FULL/B05+B10 GC display-only; choice, certificate, draws, frozen kept");
    }

    // ------------------------------------------------------------------ B08 production + B09 consumption
    static void productConsumption(R31OwnedRuntimeFixture f) {
        Map<String, Object> receipts = new LinkedHashMap<>();
        receipts.put("source_run", StatsR27.sha256("run")); receipts.put("input_hash", StatsR27.sha256("in")); receipts.put("role", "SELECT"); receipts.put("abi", "ABI-1"); receipts.put("source_revision", 3L);
        receipts.put("consumer_version", "ResearchContextR30R1.position@R31"); receipts.put("before_hash", StatsR27.sha256("before")); receipts.put("after_hash", StatsR27.sha256("after")); receipts.put("budget", 256L); receipts.put("product_hash", StatsR27.sha256("product"));
        ResearchResultContractR31.Expect expect = new ResearchResultContractR31.Expect("D", 3, "ABI-1", "POSITION", "P", 1000);
        ResearchResultContractR31.Result ok = new ResearchResultContractR31.Result("R03", ResearchResultContractR31.ProductState.PRODUCED, "D", 3, "ABI-1", "SELECT", "POSITION", "P", 999, receipts);
        ResearchResultContractR31.Result late = new ResearchResultContractR31.Result("R03", ResearchResultContractR31.ProductState.PRODUCED, "D", 3, "ABI-1", "SELECT", "POSITION", "P", 1000, receipts);
        for (int round = 0; round < 2; round++) {
            Set<String> consumed = new HashSet<>();
            try (Cursor c = f.db.rawQuery("SELECT consume_key FROM host_full_chain_consumed", null)) { while (c.moveToNext()) consumed.add(c.getString(0)); }
            String code;
            try {
                String key = ResearchResultContractR31.validateForConsumption(ok, expect, "consumer", "child", consumed);
                write(f, "R31_FULL_CHAIN_CONSUME", db -> { db.execSQL("INSERT INTO host_full_chain_consumed VALUES(?,?)", new Object[] {key, receipts.get("product_hash")}); return 0; });
                code = "ACCEPTED";
            } catch (ResearchResultContractR31.Rejected r) { code = r.code; }
            need(round == 0 ? (code.equals("ACCEPTED") || code.equals("R31_RESULT_DUPLICATE_CONSUME")) : code.equals("R31_RESULT_DUPLICATE_CONSUME"), "B09 consume round " + round + ": " + code);
            String lateCode;
            try { ResearchResultContractR31.validateForConsumption(late, expect, "consumer", "child", consumed); lateCode = "ACCEPTED"; } catch (ResearchResultContractR31.Rejected r) { lateCode = r.code; }
            need(lateCode.startsWith("R31_RESULT_LATE_LABEL"), "B08 late label refused");
        }
        need(n(f.db, "SELECT count(*) FROM host_full_chain_consumed") == 1, "B08+B09 one consumer product per result across re-entry");
        facts.add("F10_DUPLICATE_CONSUME/B08+B09 one product per result; late label refused");
    }

    // ------------------------------------------------------------------ whole chain, killable
    static int commitsSeen;
    static Runnable haltHook;
    static void phase(SQLiteDatabase db, String task, Phase body) throws Exception {
        asNewOwner(db, task, f -> { f.db.beforeSuccessful = haltHook; try { body.run(f); } finally { f.db.beforeSuccessful = null; } });
    }
    /** The complete chain on one database. haltAfterCommits>0 halts the JVM immediately before that native COMMIT. */
    static void chain(SQLiteDatabase db, Path file, boolean full) throws Exception {
        try (R31OwnedRuntimeFixture f = new R31OwnedRuntimeFixture(db, TASK)) { fixtures(f); }
        Map<String, String> assets = protectedAssets(db);
        invariants(db, "start", null);
        phase(db, TASK, f -> ownerCommits(f, TASK, 12)); invariants(db, "B01B02", assets);
        if (full) {
            phase(db, TASK, f -> contention(file, f)); invariants(db, "F01", assets);
            phase(db, TASK, R31FullChainFaultHost::staleLateCommit); invariants(db, "F03", assets);
        }
        phase(db, TASK, full ? R31FullChainFaultHost::settlement : f -> { draw(f, TARGETS[3], 4); settle(f, TargetIssueResolver.sequenceOrdinalSU1(TARGETS[3])); });
        invariants(db, "B03", null);
        phase(db, TASK, R31FullChainFaultHost::gcAndChoice); invariants(db, "B05B10", null);
        phase(db, TASK, R31FullChainFaultHost::productConsumption); invariants(db, "B08B09", null);
    }
    static void child(Path file, int haltAfterCommits) throws Exception {
        try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + file)) {
            SQLiteDatabase db = new SQLiteDatabase(c); baseSchema(db); db.execSQL("PRAGMA journal_mode=WAL"); db.execSQL("PRAGMA busy_timeout=20000");
            haltHook = haltAfterCommits <= 0 ? null : () -> { if (++commitsSeen == haltAfterCommits) { System.out.println("HALT_BEFORE_COMMIT|" + commitsSeen); System.out.flush(); Runtime.getRuntime().halt(137); } };
            chain(db, file, haltAfterCommits <= 0);
            System.out.println("CHAIN_DONE|checks=" + checks + "|" + facts);
        }
    }

    public static void main(String[] args) throws Exception {
        Class.forName("org.sqlite.JDBC");
        if (args.length > 0 && args[0].equals("child")) { child(Paths.get(args[1]), Integer.parseInt(args[2])); System.out.flush(); System.exit(0); }
        Map<String, Object> receipt = new LinkedHashMap<>();
        // 1) the uninterrupted chain on one database
        Path file = Files.createTempFile("r31-full-chain-", ".db");
        try { child(file, 0); } finally { for (String x : new String[] {"", "-wal", "-shm"}) Files.deleteIfExists(Paths.get(file + x)); }
        receipt.put("uninterrupted", new ArrayList<>(facts));
        // 2) F02 process death: the chain halts the JVM immediately before the k-th native COMMIT, the parent confirms the exit,
        //    reopens the same file and runs the full chain to completion; every invariant must still hold.
        List<Object> kills = new ArrayList<>();
        for (int k : new int[] {1, 3, 6, 10, 14, 18, 22}) {
            Path db = Files.createTempFile("r31-full-chain-kill-", ".db");
            try {
                List<String> cmd = Arrays.asList(Paths.get(System.getProperty("java.home"), "bin", "java").toString(), "-Xmx512m", "-cp", System.getProperty("java.class.path"), R31FullChainFaultHost.class.getName(), "child", db.toString(), String.valueOf(k));
                Process child = new ProcessBuilder(cmd).redirectErrorStream(true).start();
                String log = new String(child.getInputStream().readAllBytes(), StandardCharsets.UTF_8); int exit = child.waitFor();
                boolean halted = exit == 137 && log.contains("HALT_BEFORE_COMMIT|" + k);
                need(halted || (exit == 0 && log.contains("CHAIN_DONE|")), "kill " + k + " child exit " + exit + ": " + log.substring(Math.max(0, log.length() - 3000)));
                try (Connection c = DriverManager.getConnection("jdbc:sqlite:" + db)) {
                    SQLiteDatabase reopened = new SQLiteDatabase(c); baseSchema(reopened); reopened.execSQL("PRAGMA busy_timeout=20000");
                    need("ok".equals(s(reopened, "PRAGMA integrity_check")), "kill " + k + " integrity");
                    if (n(reopened, "SELECT count(*) FROM sqlite_master WHERE name='r26_task'") > 0 && n(reopened, "SELECT count(*) FROM r26_task WHERE task_id=? AND owner LIKE ?", TASK, "host-owned-" + child.pid() + "-%") > 0)
                        try (R31OwnedRuntimeFixture f = new R31OwnedRuntimeFixture(reopened, TASK)) { f.recoverAfterConfirmedProcessExit(child.pid()); }
                    if (n(reopened, "SELECT count(*) FROM sqlite_master WHERE name='host_full_chain_artifact'") > 0) invariants(reopened, "after-kill-" + k, null);
                    haltHook = null;
                    chain(reopened, db, false);  // the same chain resumes on the same file
                    Map<String, Long> once = settledAt(reopened);
                    phase(reopened, TASK, f -> settle(f, TargetIssueResolver.sequenceOrdinalSU1(TARGETS[3])));
                    need(once.equals(settledAt(reopened)), "kill " + k + ": settlement repeated after resume");
                    invariants(reopened, "resumed-" + k, null);
                    need(n(reopened, "SELECT count(*) FROM host_full_chain_artifact WHERE task_id=?", TASK) == 12, "kill " + k + ": owner sequence complete exactly once");
                    need(n(reopened, "SELECT count(*) FROM host_full_chain_consumed") == 1, "kill " + k + ": one consumer product");
                }
                Map<String, Object> row = new LinkedHashMap<>(); row.put("haltBeforeCommit", k); row.put("halted", halted); kills.add(row);
            } finally { for (String x : new String[] {"", "-wal", "-shm"}) Files.deleteIfExists(Paths.get(db + x)); }
        }
        receipt.put("processDeath", kills);
        receipt.put("checks", checks); receipt.put("layer", "HOST_NATIVE_SQLITE_ONE_DB_ONE_COMMIT_NOT_ART_NOT_DEVICE"); receipt.put("android", "NOT_RUN"); receipt.put("naturalQuality", "NOT_PROVEN");
        receipt.put("status", "PASS");
        System.out.println("R31_FULL_CHAIN_FAULT_HOST|" + JsonR27.write(receipt));
        System.exit(0);
    }
}
