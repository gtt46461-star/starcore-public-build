package com.ethk3.novaonepro;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import androidx.test.platform.app.InstrumentationRegistry;
import com.ethk3.novaonepro.strategyv2.JsonR27;
import org.json.JSONObject;
import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Exact production owners exercised on the B01 signed candidate in dedicated Android data scopes. */
public final class Field173B01ContractTest {
    private static final String BASELINE = "4d1de9d83692635b5c5ce39144fece1c8a31c8ac";
    private static final String COVER_PROOF = "r343_upgrade_probe";
    private static final String PROBE_FILE = "field173-upgrade-original.txt";

    @Test public void identityPositive() throws Exception { runIdentity("POS"); }
    @Test public void identityNegative() throws Exception { runIdentity("NEG"); }
    @Test public void identityPreservation() throws Exception { runIdentity("KEEP"); }
    @Test public void bindingPositive() throws Exception { runBinding("POS"); }
    @Test public void bindingNegative() throws Exception { runBinding("NEG"); }
    @Test public void bindingPreservation() throws Exception { runBinding("KEEP"); }
    @Test public void observationsPositive() throws Exception { runObservation("POS"); }
    @Test public void observationsNegative() throws Exception { runObservation("NEG"); }
    @Test public void observationsPreservation() throws Exception { runObservation("KEEP"); }

    private static JSONObject installedIdentity(Context app) throws Exception {
        try (java.io.InputStream asset = app.getAssets().open("diagnostics/build-identity-r334.json")) {
            return new JSONObject(new String(asset.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private static long version(PackageInfo info) {
        return Build.VERSION.SDK_INT >= 28 ? info.getLongVersionCode() : info.versionCode;
    }

    private void runIdentity(String scenario) throws Exception {
        Field173NativeEvidence.run((app, checks) -> {
            PackageInfo info = app.getPackageManager().getPackageInfo(app.getPackageName(), 0);
            JSONObject artifact = installedIdentity(app);
            checks.equal(ReleaseManifestR926.APPLICATION_ID, info.packageName, "Installed app package");
            checks.equal((long) ReleaseManifestR926.VERSION_CODE, version(info), "Installed version owner");
            checks.equal(ReleaseManifestR926.VERSION_NAME, info.versionName, "Installed version name");
            checks.equal(SourceManifestR815.observationSourceCommit(), artifact.getString("source_commit"),
                    "Installed app asset and observation owner");
            if ("NEG".equals(scenario)) {
                checks.require(!BASELINE.equals(artifact.getString("source_commit")),
                        "Candidate does not claim the historical source commit");
            } else if ("KEEP".equals(scenario)) {
                checks.equal("DecisionGateEngine_ONLY", ReleaseManifestR926.DECISION_OWNER,
                        "Decision owner identity");
                checks.equal("SettlementEngine_ONLY", ReleaseManifestR926.SETTLEMENT_OWNER,
                        "Settlement owner identity");
            }
            checks.noDatabase("INSTALLED_RELEASE_IDENTITY", "Android PackageManager and installed build provenance");
            checks.record("package", info.packageName);
            checks.record("versionCode", version(info));
            checks.record("versionName", info.versionName);
            checks.record("sourceCommit", artifact.getString("source_commit"));
            checks.record("sourceTree", artifact.getString("source_tree"));
        });
    }

    private void runBinding(String scenario) throws Exception {
        Field173NativeEvidence.run((app, checks) -> {
            JSONObject asset = installedIdentity(app);
            JSONObject current = asset.getJSONObject("fieldRepair173");
            checks.equal(BASELINE, current.getString("sourceBaseline"), "Historical installed source");
            checks.equal(577311173, current.getJSONObject("sourceApkEvidenceRef").getInt("versionCode"),
                    "Historical installed version");
            checks.equal("B01", current.getString("batch"), "Current field batch");
            checks.equal(SourceManifestR815.observationSourceCommit(), current.getString("sourceCommit"),
                    "Current field source matches observation source");
            checks.equal(asset.getString("source_tree"), current.getString("sourceTree"), "Source tree");
            checks.equal(ReleaseManifestR926.VERSION_CODE, current.getInt("versionCode"), "Current version");
            checks.require(current.getInt("versionCode") > 577311173, "Strict installed version increase");
            if ("NEG".equals(scenario)) {
                checks.require(!BASELINE.equals(current.getString("sourceCommit")), "No source identity alias");
                checks.require(!asset.getJSONObject("r342").getString("planCommit").equals(current.getString("planCommit")),
                        "Historical plan is an independent identity");
            } else if ("KEEP".equals(scenario)) {
                checks.equal("R34.2", current.getString("apkSeries"), "Existing version series");
                JSONObject previous = asset.getJSONObject("r342").getJSONObject("legacyPlanSeals");
                checks.require(previous.has("r33_5") && previous.has("r34") && previous.has("r341"),
                        "Prior owners still embedded in the R342 series identity");
            }
            checks.noDatabase("INSTALLED_FIELD_BINDING", "Embedded build identity from the exact signed release APK. Cover-install DB evidence is separate.");
            checks.record("fieldIdentity", current);
        });
    }

    private void runObservation(String scenario) throws Exception {
        Field173NativeEvidence.run((app, checks) -> {
            File leaf = app.getDatabasePath("field173-b01-scope.sqlite");
            try (SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(leaf, null)) {
                db.beginTransaction();
                try {
                    RuntimeObservationScopeR341.installWithinTransaction(db);
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            }
            String firstHash = hashFile(leaf);
            try (SQLiteDatabase db = SQLiteDatabase.openDatabase(leaf.getPath(), null, SQLiteDatabase.OPEN_READONLY)) {
                String id = RuntimeObservationScopeR341.databaseIdentity(db);
                RuntimeObservationScopeR341 first = RuntimeObservationScopeR341.capture(app, id, firstHash,
                        RuntimeObservationScopeR341.SYSTEM);
                RuntimeObservationScopeR341 second = RuntimeObservationScopeR341.capture(app, id, firstHash,
                        RuntimeObservationScopeR341.SYSTEM);
                checks.require(first.identified(), "Exact observation scope");
                checks.equal("COMPATIBLE", first.validateJoin(second), "Readback same immutable identity");
                checks.equal(SourceManifestR815.observationSourceCommit(), first.sourceCommit, "Installed observation source");

                String original = MainWriterFaultProjectionR342.capture(app, db, null);
                checks.require(original != null, "Actual writer production capture");
                Map<?, ?> outer = (Map<?, ?>) JsonR27.parse(original);
                Map<?, ?> body = (Map<?, ?>) JsonR27.parse((String) outer.get("original"));
                int producer = ((Number) body.get("producerPid")).intValue();
                long wall = ((Number) body.get("sampleElapsedMs")).longValue();
                Map<String, Object> actual = MainWriterFaultProjectionR342.validate(original, 1, producer,
                        (String) body.get("processEpoch"), (String) body.get("bootId"), id, wall);
                checks.equal("OBSERVED_MAIN_OWNER", actual.get("observationState"), "Projection readback recognizes original owner");
                if ("NEG".equals(scenario)) {
                    File changed = app.getDatabasePath("field173-b01-altered-scope.sqlite");
                    Files.copy(leaf.toPath(), changed.toPath());
                    try (SQLiteDatabase altered = SQLiteDatabase.openDatabase(changed.getPath(), null,
                            SQLiteDatabase.OPEN_READWRITE)) {
                        altered.execSQL("CREATE TABLE another_snapshot_marker(tx INTEGER)");
                    }
                    String otherHash = hashFile(changed);
                    checks.require(!firstHash.equals(otherHash), "Separate physical SQLite data");
                    RuntimeObservationScopeR341 differentImage = RuntimeObservationScopeR341.capture(app, id, otherHash,
                            RuntimeObservationScopeR341.SYSTEM);
                    checks.equal("SQL_WATERMARK_CHANGED", first.validateJoin(differentImage), "Image mixing rejected");
                    RuntimeObservationScopeR341 differentRelease = new RuntimeObservationScopeR341(first.releaseId,
                            BASELINE, first.processEpoch, first.bootId, id, first.sampleWallAt, first.sampleMonoAt, firstHash);
                    checks.equal("RELEASE_CHANGED", first.validateJoin(differentRelease), "Release mixing rejected");
                    Map<String, Object> forgedBody = new LinkedHashMap<>();
                    for (Map.Entry<?, ?> entry : body.entrySet()) {
                        forgedBody.put((String) entry.getKey(), entry.getValue());
                    }
                    forgedBody.put("sourceCommit", BASELINE);
                    String forged = JsonR27.write(forgedBody);
                    String rehashed = JsonR27.write(Map.of("original", forged, "sha256", V51ChecksumEngine.sha256(forged)));
                    Map<String, Object> rejected = MainWriterFaultProjectionR342.validate(rehashed, 1, producer,
                            (String) body.get("processEpoch"), (String) body.get("bootId"), id, wall);
                    checks.equal("UNKNOWN", rejected.get("observationState"), "Valid checksum remains insufficient");
                    checks.equal("MAIN_PROJECTION_OWNER_CHANGED", rejected.get("reason"), "Owner mismatch remains visible");
                    checks.record("rejectedProjection", rejected);
                } else if ("KEEP".equals(scenario)) {
                    RuntimeObservationScopeR341 inProcess = RuntimeObservationScopeR341.capture(app, id, null,
                            RuntimeObservationScopeR341.SYSTEM);
                    checks.equal("SNAPSHOT_AND_PROCESS_SAMPLE_DIFFER", first.validateJoin(inProcess),
                            "Process diagnostics retain independent denominator");
                    Map<String, Object> expired = MainWriterFaultProjectionR342.validate(original, 1, producer,
                            (String) body.get("processEpoch"), (String) body.get("bootId"), id, wall + 12_001L);
                    checks.equal("UNKNOWN", expired.get("observationState"), "Stale projection remains unknown");
                    checks.equal("MAIN_PROJECTION_EXPIRED_OR_CLOCK_UNVERIFIED", expired.get("reason"),
                            "Stale reason retained");
                    checks.record("staleProjection", expired);
                }
                checks.databaseWithoutTarget("DIAGNOSTIC_SQL_SCOPE", id, "Database provenance has no draw target or PIT cutoff");
                checks.record("firstObservationScope", first.exportEnvelope());
                checks.record("secondObservationScope", second.exportEnvelope());
                checks.record("projectionReadback", actual);
            }
        });
    }

    private static String hashFile(File file) throws Exception {
        byte[] raw = java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file.toPath()));
        StringBuilder hash = new StringBuilder(64);
        for (byte value : raw) hash.append(String.format(java.util.Locale.ROOT, "%02x", value & 255));
        return hash.toString();
    }

    @Test public void recordBeforeSignedCover() throws Exception {
        runSignedCoverProbe(true);
    }

    @Test public void readAfterSignedCover() throws Exception {
        runSignedCoverProbe(false);
    }

    private void runSignedCoverProbe(boolean prepare) throws Exception {
        android.app.Instrumentation runner = InstrumentationRegistry.getInstrumentation();
        Context app = runner.getTargetContext().getApplicationContext();
        String nonce = InstrumentationRegistry.getArguments().getString("field173CoverProbe");
        assertTrue(nonce != null && nonce.matches("[0-9a-f]{32}"));
        assertTrue(app.getApplicationInfo().dataDir.matches("/data/(user/0|data)/com\\.starcore\\.research\\.r13"));
        PackageInfo info = app.getPackageManager().getPackageInfo(app.getPackageName(), 0);
        File custodied = new File(app.getNoBackupFilesDir(), PROBE_FILE);
        ServiceHeartbeatStore switches = new ServiceHeartbeatStore(app);
        SQLiteDatabase db = DatabaseCoordinator.get(app).writable();
        if (prepare) {
            assertEquals(577311173L, version(info));
            switches.setUserEnabled(true);
            assertTrue(switches.isUserEnabled());
            switches.setUserEnabled(false);
            db.beginTransaction();
            try {
                RuntimeObservationScopeR341.installWithinTransaction(db);
                db.execSQL("CREATE TABLE IF NOT EXISTS " + COVER_PROOF + "(singleton INTEGER PRIMARY KEY, original TEXT NOT NULL)");
                db.execSQL("INSERT OR REPLACE INTO " + COVER_PROOF + " VALUES (1,?)", new String[]{nonce});
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            Files.write(custodied.toPath(), nonce.getBytes(StandardCharsets.UTF_8));
        } else {
            assertTrue(version(info) > 577311173L);
        }
        assertFalse(switches.isUserEnabled());
        assertEquals(nonce, new String(Files.readAllBytes(custodied.toPath()), StandardCharsets.UTF_8));
        String database = RuntimeObservationScopeR341.databaseIdentity(db);
        assertNotNull(database);
        assertTrue(database.matches("[a-f0-9-]{36}"));
        try (Cursor row = db.rawQuery("SELECT original FROM " + COVER_PROOF + " WHERE singleton=1", null)) {
            assertTrue(row.moveToFirst());
            assertEquals(nonce, row.getString(0));
        }
        JSONObject proof = new JSONObject().put("executionId", nonce).put("phase", prepare ? "before" : "after")
                .put("uid", app.getApplicationInfo().uid).put("packageName", info.packageName)
                .put("versionCode", version(info)).put("dbUserVersion", db.getVersion())
                .put("databaseIdentity", database).put("original", nonce).put("userEnabled", switches.isUserEnabled());
        android.os.Bundle status = new android.os.Bundle();
        status.putString("field173CoverProbe", proof.toString());
        runner.sendStatus(2, status);
    }
}
