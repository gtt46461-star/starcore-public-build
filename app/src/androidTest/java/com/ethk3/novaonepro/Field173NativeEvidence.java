package com.ethk3.novaonepro;

import android.app.Instrumentation;
import android.content.Context;
import android.os.Bundle;
import android.util.Base64;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Instrumentation-only assertion counter and evidence bridge. */
public final class Field173NativeEvidence {
    private Field173NativeEvidence() {}

    @FunctionalInterface
    public interface CheckedCase {
        void run(Context app, Checks checks) throws Exception;
    }

    public static final class Checks {
        private int count;
        private final JSONObject readback = new JSONObject();
        private JSONObject scope;

        public void require(boolean condition, String message) {
            if (!condition) throw new AssertionError(message);
            count++;
        }

        public void equal(Object expected, Object actual, String message) {
            require(Objects.equals(expected, actual), message + ": expected " + expected + ", got " + actual);
        }

        public void record(String name, Object value) throws Exception {
            if (readback.has(name)) throw new AssertionError("Duplicate readback key: " + name);
            readback.put(name, JSONObject.wrap(value));
        }

        public void database(String role, String target, long cutoff, String databaseIdentity) throws Exception {
            setScope(role, target, cutoff, databaseIdentity, "");
        }

        public void databaseWithoutTarget(String role, String databaseIdentity, String reason) throws Exception {
            setScope(role, JSONObject.NULL, JSONObject.NULL, databaseIdentity, reason);
        }

        public void noDatabase(String role, String reason) throws Exception {
            setScope(role, JSONObject.NULL, JSONObject.NULL, JSONObject.NULL, reason);
        }

        private void setScope(String role, Object target, Object cutoff, Object database, String reason)
                throws Exception {
            if (scope != null) throw new AssertionError("Scope already recorded");
            scope = new JSONObject().put("role", role).put("target", target).put("cutoff", cutoff)
                    .put("databaseIdentity", database).put("notApplicableReason", reason);
        }
    }

    public static void run(CheckedCase checkedCase) throws Exception {
        Instrumentation android = InstrumentationRegistry.getInstrumentation();
        Bundle args = InstrumentationRegistry.getArguments();
        String encoded = args.getString("field173Context");
        if (encoded == null) throw new AssertionError("Missing FIELD173 instrumentation context");
        JSONObject identity = new JSONObject(new String(Base64.decode(encoded, Base64.URL_SAFE), StandardCharsets.UTF_8));
        Context app = android.getTargetContext().getApplicationContext();
        if (!identity.getString("sourceCommit").equals(BuildConfig.SOURCE_COMMIT_R26)) {
            throw new AssertionError("Instrumented source does not match the bound source");
        }
        String sandbox = args.getString("r30Sandbox");
        if (sandbox == null || !app.getApplicationInfo().dataDir.endsWith(
                "/r30-instrumentation/" + sandbox + "/" + app.getPackageName())) {
            throw new AssertionError("Unexpected instrumentation data directory");
        }

        emit(android, "field173Event", new JSONObject(identity.toString()).put("event", "CASE_ENTER"));
        Checks checks = new Checks();
        checkedCase.run(app, checks);
        if (checks.count == 0 || checks.scope == null || checks.readback.length() == 0) {
            throw new AssertionError("Missing actual assertions, scope or readback");
        }
        JSONObject output = new JSONObject(identity.toString())
                .put("dataScope", checks.scope).put("readback", checks.readback);
        emit(android, "field173Readback", output);
        emit(android, "field173Event", new JSONObject(identity.toString())
                .put("event", "CASE_ASSERTIONS_COMPLETE").put("assertionCount", checks.count).put("status", "PASS"));
    }

    private static void emit(Instrumentation android, String key, JSONObject value) {
        Bundle status = new Bundle();
        status.putString(key, value.toString());
        android.sendStatus(2, status);
    }
}
