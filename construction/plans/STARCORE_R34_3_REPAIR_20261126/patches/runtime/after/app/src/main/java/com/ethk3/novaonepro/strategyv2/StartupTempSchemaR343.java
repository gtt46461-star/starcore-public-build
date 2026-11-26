package com.ethk3.novaonepro.strategyv2;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Exact contracts of connection-local migration workspaces, never model data. */
public final class StartupTempSchemaR343 {
    private StartupTempSchemaR343() {}

    private static final Map<String,String> TABLES;
    static {
        Map<String,String> tables = new LinkedHashMap<>();
        tables.put("r31q_ddl_cache_flush", "CREATE TABLE r31q_ddl_cache_flush(x INTEGER)");
        tables.put("r335_extension_catalog", "CREATE TABLE r335_extension_catalog(original_id INTEGER PRIMARY KEY,type,name,tbl_name,sql)");
        tables.put("r335_extension_seal", "CREATE TABLE r335_extension_seal(domain TEXT,revision INTEGER,contract_hash TEXT,fingerprint TEXT,state TEXT)");
        tables.put("r335_extension_journal", "CREATE TABLE r335_extension_journal(step_id TEXT,status TEXT,domain TEXT,revision INTEGER)");
        TABLES = Collections.unmodifiableMap(tables);
    }

    public static boolean allowed(String type, String name, String table, String sql) {
        if (!"table".equals(type) || name == null || !name.equals(table) || sql == null) return false;
        String expected = TABLES.get(name);
        // sqlite_master omits TEMP/IF NOT EXISTS in normal SQLite builds; accept
        // either emitted form but no changed columns, constraints or table names.
        String actual = sql.trim().replaceFirst("^CREATE TEMP TABLE ", "CREATE TABLE ")
                .replaceFirst("^CREATE TABLE IF NOT EXISTS ", "CREATE TABLE ");
        return expected != null && expected.equals(actual);
    }
}
