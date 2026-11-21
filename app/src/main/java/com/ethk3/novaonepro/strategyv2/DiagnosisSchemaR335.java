package com.ethk3.novaonepro.strategyv2;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.DatabaseCoordinator;
/** Only LifecycleSchemaR31 installs these originals. No business path executes DDL. */
public final class DiagnosisSchemaR335 {
    private DiagnosisSchemaR335() {}
    public static final int MAX_RAW_BYTES=4*1024*1024,MAX_GZIP_BYTES=MAX_RAW_BYTES+65536;
    public static final String[] DDL={
        "CREATE TABLE IF NOT EXISTS r335_s5_refit_original(case_id TEXT PRIMARY KEY NOT NULL CHECK(length(case_id)=64),singleton INTEGER NOT NULL DEFAULT 1 UNIQUE CHECK(singleton=1),original_hash TEXT NOT NULL CHECK(length(original_hash)=64),checkpoint_hash TEXT NOT NULL CHECK(length(checkpoint_hash)=64),raw_bytes INTEGER NOT NULL CHECK(raw_bytes BETWEEN 1 AND 4194304),compressed BLOB NOT NULL CHECK(length(compressed) BETWEEN 1 AND 4259840),compressed_hash TEXT NOT NULL CHECK(length(compressed_hash)=64),created_at INTEGER NOT NULL CHECK(created_at>=0)) WITHOUT ROWID",
        "CREATE TRIGGER IF NOT EXISTS r335_s5_refit_no_update BEFORE UPDATE ON r335_s5_refit_original BEGIN SELECT RAISE(ABORT,'R335_S5_ORIGINAL_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS r335_s5_refit_no_delete BEFORE DELETE ON r335_s5_refit_original BEGIN SELECT RAISE(ABORT,'R335_S5_ORIGINAL_PROTECTED'); END"
    };
    public static boolean declares(String name){return "r335_s5_refit_original".equals(name)||"r335_s5_refit_no_update".equals(name)||"r335_s5_refit_no_delete".equals(name);}
    public static void requireReady(SQLiteDatabase db){DatabaseCoordinator.requireSchemaObjectsR31(db,"R335_SU2_DIAG_ORIGINAL",new String[][]{{"r335_s5_refit_original","case_id,singleton,original_hash,checkpoint_hash,raw_bytes,compressed,compressed_hash,created_at"}},new String[][]{{"trigger","r335_s5_refit_no_update"},{"trigger","r335_s5_refit_no_delete"}});}
}
