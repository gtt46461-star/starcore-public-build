package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** Transaction-local schema memoization, plus explicit opt-in for audited DDL-only owners.
 * No model, sample, migration outcome, or quality decision is cached here.
 */
public final class TransactionSchemaCacheR941 {
    private static final ThreadLocal<Scope> CURRENT = new ThreadLocal<Scope>();
    private static final Map<SQLiteDatabase, Entry> COMMITTED = new WeakHashMap<SQLiteDatabase, Entry>();
    private TransactionSchemaCacheR941() {}

    private static final class Entry {
        final long version;
        final Set<String> names;
        Entry(long version, Set<String> names) {
            this.version = version;
            this.names = new HashSet<String>(names);
        }
    }
    public static final class Scope {
        final Scope parent;
        final SQLiteDatabase database;
        final Set<String> ready;
        final Map<String, Long> verified;
        Scope(Scope parent, SQLiteDatabase database) {
            this.parent = parent;
            this.database = database;
            boolean nested = parent != null && parent.database == database;
            ready = nested ? parent.ready : new HashSet<String>();
            verified = nested ? parent.verified : new HashMap<String, Long>();
        }
    }
    public static Scope enter(SQLiteDatabase database) {
        Scope scope = new Scope(CURRENT.get(), database);
        CURRENT.set(scope);
        return scope;
    }
    public static void leave(Scope scope) {
        if (scope == null) return;
        if (scope.parent == null) CURRENT.remove(); else CURRENT.set(scope.parent);
    }
    public static boolean ready(SQLiteDatabase database, String name) {
        Scope scope = CURRENT.get();
        return scope != null && scope.database == database && database.inTransaction()
                && scope.ready.contains(name);
    }
    public static void mark(SQLiteDatabase database, String name) {
        Scope scope = CURRENT.get();
        if (scope != null && scope.database == database && database.inTransaction()) scope.ready.add(name);
    }

    /** Only audited, idempotent DDL owners opt in; ordinary ready()/mark() keep their old semantics.
     * Check schema_version even within a transaction: DROP/ALTER after an earlier ensure
     * must invalidate that earlier readiness. Never write PRAGMA schema_version.
     */
    public static boolean readyCommitted(SQLiteDatabase database, String name) {
        Scope scope = CURRENT.get();
        if (scope == null || scope.database != database || !database.inTransaction()) return false;
        long version = schemaVersion(database);
        if (version < 0L) return false;
        Long local = scope.verified.get(name);
        if (local != null && local.longValue() == version) return true;
        synchronized (COMMITTED) {
            Entry entry = COMMITTED.get(database);
            if (entry == null || entry.version != version || !entry.names.contains(name)) return false;
        }
        scope.verified.put(name, Long.valueOf(version));
        return true;
    }
    public static void markCommitted(SQLiteDatabase database, String name) {
        Scope scope = CURRENT.get();
        if (scope == null || scope.database != database || !database.inTransaction()) return;
        long version = schemaVersion(database);
        if (version >= 0L) scope.verified.put(name, Long.valueOf(version));
    }

    /** Called only by the owner of an outer transaction, AFTER endTransaction succeeded.
     * A rollback, nested call, external transaction or failed COMMIT never publishes readiness.
     * The post-commit version must still equal the version verified by the DDL owner.
     */
    static void afterCommit(Scope scope) {
        if (scope == null || scope.verified.isEmpty() || scope.database.inTransaction()) return;
        if (scope.parent != null && scope.parent.database == scope.database) return;
        long version = schemaVersion(scope.database);
        if (version < 0L) return;
        synchronized (COMMITTED) {
            Entry previous = COMMITTED.get(scope.database);
            Set<String> names = previous != null && previous.version == version
                    ? new HashSet<String>(previous.names) : new HashSet<String>();
            for (Map.Entry<String, Long> item : scope.verified.entrySet()) {
                if (item.getValue().longValue() == version) names.add(item.getKey());
            }
            if (names.isEmpty()) COMMITTED.remove(scope.database);
            else COMMITTED.put(scope.database, new Entry(version, names));
        }
    }
    private static long schemaVersion(SQLiteDatabase database) {
        try (Cursor c = database.rawQuery("PRAGMA schema_version", null)) {
            return c.moveToFirst() ? c.getLong(0) : -1L;
        } catch (RuntimeException unavailable) {
            // This is an optimization, never a reason to skip required schema work.
            synchronized (COMMITTED) { COMMITTED.remove(database); }
            return -1L;
        }
    }
}
