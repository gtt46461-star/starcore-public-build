package com.ethk3.novaonepro;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

/**
 * Process-start bootstrap for the schema contract.
 *
 * <p>B07R2 hardening: 1) migration/integrity remains the first responsibility; 2) after the
 * database is verified and closed, a user-enabled service is re-armed; 3) bootstrap never predicts,
 * never creates BET, never settles and never learns; 4) all recovery failures are recorded but do
 * not crash application process startup.
 */
public final class V50BootstrapProvider extends ContentProvider {
    @Override
    public boolean onCreate() {
        android.content.Context context =
                getContext() == null ? null : getContext().getApplicationContext();
        if (context == null) return true;
        try {
            CrashRecorder.install(context);
            CrashRecorder.setRuntimeContext("", "PROCESS_BOOTSTRAP_PROVIDER");
            ServiceHeartbeatStore hb = new ServiceHeartbeatStore(context);
            hb.markBusinessStage("PROCESS_BOOTSTRAP_UI_FIRST", AppConfig.B07R32_VERSION);
            if (hb.isUserEnabled()) {
                hb.setLastReason("PROCESS_BOOTSTRAP_DEFER_SERVICE_TO_ENTRYPOINT");
            } else {
                hb.setLastReason("PROCESS_BOOTSTRAP_USER_DISABLED");
            }
        } catch (Throwable error) {
            try {
                new ServiceHeartbeatStore(context)
                        .markRecoveryResult(
                                "process_bootstrap_light", false, error.getClass().getSimpleName());
            } catch (Throwable ignore) {
            }
        }
        return true;
    }

    @Override
    public Cursor query(
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("BOOTSTRAP_PROVIDER_READ_ONLY");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("BOOTSTRAP_PROVIDER_READ_ONLY");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("BOOTSTRAP_PROVIDER_READ_ONLY");
    }
}
