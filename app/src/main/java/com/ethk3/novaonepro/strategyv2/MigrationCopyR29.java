package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Resumable, typed research baseline backup in an independent SQLite file. Each table is copied only while its
 * source generation is unchanged, then the destination bytes are independently rehashed. Old owners/tables stay
 * authoritative. VERIFIED_BASELINE is an immutable backup, NOT a cutover or a consistent cross-table snapshot.
 * R29-004 cutover still requires catch-up, all reader routes, and the R29-005 certificate projection. */
public final class MigrationCopyR29 {
    private MigrationCopyR29() {}
    public static final int PAGE = 32, MAX_ROW_BYTES = 8 << 20;
    public static final long SLICE_NS = 20_000_000L;
    public static final String SOURCE_WATERMARK = "CREATE TABLE IF NOT EXISTS r29_source_watermark(table_name TEXT PRIMARY KEY,generation INTEGER NOT NULL DEFAULT 0)";
    public static final String[] DESTINATION_DDL = {
        "CREATE TABLE IF NOT EXISTS r29_copy_manifest(table_name TEXT PRIMARY KEY,schema_json TEXT NOT NULL,schema_hash TEXT NOT NULL,state TEXT NOT NULL,attempt INTEGER NOT NULL DEFAULT 0,source_generation INTEGER NOT NULL DEFAULT 0,last_rowid INTEGER,copied_rows INTEGER NOT NULL DEFAULT 0,copy_hash TEXT NOT NULL,source_rows INTEGER,verify_rowid INTEGER,verified_rows INTEGER NOT NULL DEFAULT 0,verify_hash TEXT NOT NULL,verified_at INTEGER NOT NULL DEFAULT 0,touched_at INTEGER NOT NULL,retry_at INTEGER NOT NULL DEFAULT 0,reason TEXT NOT NULL DEFAULT '')",
        "CREATE TABLE IF NOT EXISTS r29_copy_rows(table_name TEXT NOT NULL,attempt INTEGER NOT NULL,source_rowid INTEGER NOT NULL,payload BLOB NOT NULL,row_hash TEXT NOT NULL,PRIMARY KEY(table_name,attempt,source_rowid))",
        "CREATE TRIGGER IF NOT EXISTS r29_copy_verified_no_update BEFORE UPDATE ON r29_copy_rows WHEN EXISTS(SELECT 1 FROM r29_copy_manifest m WHERE m.table_name=OLD.table_name AND m.attempt=OLD.attempt AND m.verified_at>0) BEGIN SELECT RAISE(ABORT,'R29_VERIFIED_BACKUP_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS r29_copy_verified_no_delete BEFORE DELETE ON r29_copy_rows WHEN EXISTS(SELECT 1 FROM r29_copy_manifest m WHERE m.table_name=OLD.table_name AND m.attempt=OLD.attempt AND m.verified_at>0) BEGIN SELECT RAISE(ABORT,'R29_VERIFIED_BACKUP_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS r29_copy_verified_no_insert BEFORE INSERT ON r29_copy_rows WHEN EXISTS(SELECT 1 FROM r29_copy_manifest m WHERE m.table_name=NEW.table_name AND m.attempt=NEW.attempt AND m.verified_at>0) BEGIN SELECT RAISE(ABORT,'R29_VERIFIED_BACKUP_IMMUTABLE'); END"
    };
    public interface Body { void run(SQLiteDatabase destination) throws Exception; }
    public interface Transaction { void write(Body body) throws Exception; }

    public static final class Table {
        public final String name, schemaJson, schemaHash, createSql;
        public final List<String> columns;
        Table(String name, String createSql, List<String> columns, String schemaJson) {
            this.name = name; this.createSql = createSql; this.columns = columns; this.schemaJson = schemaJson;
            this.schemaHash = StatsR27.sha256(schemaJson);
        }
        public String seed() { return StatsR27.sha256("R29_TYPED_ROW_CHAIN_V1|" + name + "|" + schemaHash); }
    }

    public static boolean ownedResearch(String table) {
        return table != null && (table.matches("r(?:27|29)_[a-z0-9_]+")||LegacyResearchOwnershipR29.owned(table))
                && !realtimeOnly(table)
                && !table.equals("r27_fetch_reject") && !table.equals("r27_chart_cache");
    }
    static final java.util.Set<String> REALTIME_ONLY=new java.util.HashSet<>(java.util.Arrays.asList(
            "r29_event_outbox","r29_source_watermark","r29_registration_authority","r29_database_route","r29_fetch_observation",
            "r29_scheduler_owner","r29_legacy_checkpoint_pin","r29_migration_publication","r29_migration_owner_fence",
            "r29_projection_applied","r29_research_heartbeat","r29_legacy_live_projection",
            "r29_critical_input_clock","r29_critical_input_change","r29_critical_input_schema",
            "r29_legacy_authority_frame","r29_legacy_authority_projection"));
    public static boolean realtimeOnly(String table){return REALTIME_ONLY.contains(table);}

    /** The registry explicitly keeps realtime authority and recomputable cache outside the baseline backup. */
    public static List<Table> tables(SQLiteDatabase source) {
        List<Table> out = new ArrayList<>();
        try (Cursor c = source.rawQuery("SELECT name,sql FROM sqlite_master WHERE type='table' ORDER BY name", null)) {
            while (c.moveToNext()) {
                String name = c.getString(0);
                if (ownedResearch(name)) out.add(table(source, name, c.getString(1)));
            }
        }
        return out;
    }

    static Table table(SQLiteDatabase source, String name, String sql) {
        List<String> names = new ArrayList<>(); List<Object> columns = new ArrayList<>();
        try (Cursor c = source.rawQuery("PRAGMA table_info(" + id(name) + ")", null)) {
            while (c.moveToNext()) {
                Map<String,Object> col = new LinkedHashMap<>();
                col.put("cid", c.getInt(0)); col.put("name", c.getString(1)); col.put("type", c.getString(2));
                col.put("notNull", c.getInt(3)); col.put("default", c.isNull(4) ? null : c.getString(4)); col.put("primaryKeyOrder", c.getInt(5));
                names.add(c.getString(1)); columns.add(col);
            }
        }
        if (names.isEmpty() || sql == null)
            throw new IllegalStateException("R29_MIGRATION_UNSUPPORTED_TABLE:" + name);
        Map<String,Object> schema = new LinkedHashMap<>(); schema.put("abi", "SQLITE_TYPED_ROW_V1");
        schema.put("table", name); schema.put("createSql", sql); schema.put("columns", columns);
        return new Table(name, sql, names, JsonR27.write(schema));
    }

    /** Short realtime DDL transaction BEFORE copying; triggers count inserts, updates and deletes by every old writer.
     * Only a per-table generation is changed: no unbounded source journal is added. No source row is rewritten. */
    public static void trackSource(SQLiteDatabase source, Table table) {
        if (!source.inTransaction()) throw new IllegalStateException("R29_MIGRATION_SOURCE_TRANSACTION_REQUIRED");
        if (sourceTracked(source, table)) return;
        source.execSQL(SOURCE_WATERMARK);
        source.execSQL("INSERT OR IGNORE INTO r29_source_watermark(table_name) VALUES(?)", new Object[] {table.name});
        for (String event : new String[] {"INSERT", "UPDATE", "DELETE"}) {
            source.execSQL(trackerSql(table, event));
        }
        if (!sourceTracked(source, table)) throw new IllegalStateException("R29_MIGRATION_TRACKER_INSTALL_FAILED:" + table.name);
    }

    private static String trackerName(Table table, String event) {
        return "r29_copy_track_" + table.name + "_" + event.toLowerCase(java.util.Locale.ROOT);
    }
    private static String trackerSql(Table table, String event) {
        return "CREATE TRIGGER IF NOT EXISTS " + id(trackerName(table, event)) + " AFTER " + event + " ON " + id(table.name)
                + " BEGIN UPDATE r29_source_watermark SET generation=generation+1 WHERE table_name='" + table.name + "'; END";
    }
    private static String ddl(String value) {
        // sqlite_master omits IF NOT EXISTS. Keep identifier/string case intact: changing the
        // table_name literal's case changes which generation row the trigger updates.
        return value == null ? null : value.replaceFirst("(?i)\\bIF\\s+NOT\\s+EXISTS\\s+", "").trim().replaceAll("\\s+", " ");
    }
    private static String objectSql(SQLiteDatabase db, String type, String name) {
        try (Cursor c = db.rawQuery("SELECT type,sql FROM sqlite_master WHERE name=?", new String[] {name})) {
            if (!c.moveToFirst()) return null;
            if (!type.equals(c.getString(0))) throw new IllegalStateException("R29_MIGRATION_TRACKER_OBJECT_TYPE:" + name);
            String sql = c.getString(1);
            if (c.moveToNext()) throw new IllegalStateException("R29_MIGRATION_TRACKER_OBJECT_AMBIGUOUS:" + name);
            return sql;
        }
    }
    /** Fresh read-only proof of a committed installation; never a READY cache. This is also the durable
     * preparation cursor: a retry/restart does not reacquire Root's writer for earlier correct tables.
     * Partial trackers cannot be repaired by resetting generation: writes during the gap are unknowable. */
    public static boolean sourceTracked(SQLiteDatabase source, Table table) {
        if (!table.createSql.equals(objectSql(source, "table", table.name)))
            throw new IllegalStateException("R29_SOURCE_SCHEMA_CHANGED_DURING_TRACKING:" + table.name);
        String watermark = objectSql(source, "table", "r29_source_watermark");
        if (watermark != null && !ddl(SOURCE_WATERMARK).equals(ddl(watermark)))
            throw new IllegalStateException("R29_MIGRATION_WATERMARK_SCHEMA_MISMATCH");
        boolean row = false;
        if (watermark != null) try (Cursor c = source.rawQuery("SELECT generation,typeof(generation) FROM r29_source_watermark WHERE table_name=?", new String[] {table.name})) {
            row = c.moveToFirst();
            if (row && (!"integer".equals(c.getString(1)) || c.getLong(0) < 0))
                throw new IllegalStateException("R29_MIGRATION_WATERMARK_INVALID:" + table.name);
        }
        int installed = 0;
        for (String event : new String[] {"INSERT", "UPDATE", "DELETE"}) {
            String sql = objectSql(source, "trigger", trackerName(table, event));
            if (sql != null) {
                if (!ddl(trackerSql(table, event)).equals(ddl(sql)))
                    throw new IllegalStateException("R29_MIGRATION_TRACKER_SQL_MISMATCH:" + table.name + ":" + event);
                installed++;
            }
        }
        if (!row && installed == 0) return false;
        if (!row || installed != 3)
            throw new IllegalStateException("R29_MIGRATION_TRACKER_INCOMPLETE:" + table.name);
        return true;
    }

    /** Record the complete catalog without acquiring Root's writer. PENDING is not a copied or verified
     * baseline. Only the table actually selected by step needs a tracker transaction before its first copy. */
    public static void prepare(List<Table> tables, Transaction destinationWriter) throws Exception {
        if (Thread.currentThread().isInterrupted()) throw new InterruptedException("R29_MIGRATION_CANCELLED");
        destinationWriter.write(db -> register(db, tables));
    }

    public static void register(SQLiteDatabase destination, List<Table> tables) {
        if (!destination.inTransaction()) throw new IllegalStateException("R29_MIGRATION_DEST_TRANSACTION_REQUIRED");
        long now = System.currentTimeMillis();
        for (Table t : tables) {
            destination.execSQL("INSERT OR IGNORE INTO r29_copy_manifest(table_name,schema_json,schema_hash,state,copy_hash,verify_hash,touched_at) VALUES(?,?,?,'PENDING',?,?,?)",
                    new Object[] {t.name, t.schemaJson, t.schemaHash, t.seed(), t.seed(), now});
            try (Cursor c = destination.rawQuery("SELECT schema_hash FROM r29_copy_manifest WHERE table_name=?", new String[] {t.name})) {
                if (!c.moveToFirst() || !t.schemaHash.equals(c.getString(0)))
                    throw new IllegalStateException("R29_SOURCE_SCHEMA_CHANGED_REQUIRES_NEW_MANIFEST:" + t.name);
            }
        }
    }

    static final class Meta {
        String table, state, copyHash, verifyHash; long attempt, generation, rows, verified, verifiedAt; Long cursor, verifyCursor;
        static Meta read(Cursor c) {
            Meta m = new Meta(); m.table=c.getString(0);m.state=c.getString(1);m.attempt=c.getLong(2);m.generation=c.getLong(3);
            m.cursor=c.isNull(4)?null:c.getLong(4);m.rows=c.getLong(5);m.copyHash=c.getString(6);
            m.verifyCursor=c.isNull(7)?null:c.getLong(7);m.verified=c.getLong(8);m.verifyHash=c.getString(9);m.verifiedAt=c.getLong(10);return m;
        }
    }
    public static final class Result {
        public final String table, phase; public final long rows;
        Result(String table, String phase, long rows) { this.table=table;this.phase=phase;this.rows=rows; }
    }

    public static Result step(SQLiteDatabase source, SQLiteDatabase destination, List<Table> tables, Transaction tx, long now) throws Exception {
        return step(source, destination, tables, null, tx, now);
    }

    /** The optional source writer installs only a genuinely unused selected table's trackers. It commits
     * before reading that table's generation/rows or starting a destination transaction. Existing callers
     * without a source writer remain strict: incomplete/missing trackers can never authorize a copy. */
    public static Result step(SQLiteDatabase source, SQLiteDatabase destination, List<Table> tables,
                              Transaction sourceWriter, Transaction tx, long now) throws Exception {
        Meta m;
        try (Cursor c = destination.rawQuery("SELECT table_name,state,attempt,source_generation,last_rowid,copied_rows,copy_hash,verify_rowid,verified_rows,verify_hash,verified_at FROM r29_copy_manifest WHERE state<>'VERIFIED_BASELINE' AND retry_at<=? ORDER BY CASE state WHEN 'VERIFYING' THEN 0 WHEN 'COPYING' THEN 1 ELSE 2 END,touched_at,table_name LIMIT 1", new String[] {String.valueOf(now)})) {
            if (!c.moveToFirst()) return new Result(null,"IDLE_BASELINE_OR_RETRY",0);
            m=Meta.read(c);
        }
        Table table = null; for (Table t : tables) if (t.name.equals(m.table)) { table=t;break; }
        if (table == null) throw new IllegalStateException("R29_SOURCE_TABLE_DISAPPEARED:"+m.table);
        final Table t = table;
        String sql;
        try (Cursor c = source.rawQuery("SELECT sql FROM sqlite_master WHERE type='table' AND name=?", new String[] {t.name})) {
            sql = c.moveToFirst()?c.getString(0):null;
        }
        if (!t.createSql.equals(sql)) throw new IllegalStateException("R29_SOURCE_SCHEMA_CHANGED_DURING_COPY:"+t.name);
        if (!sourceTracked(source, t)) {
            if (sourceWriter == null) throw new IllegalStateException("R29_SOURCE_NOT_TRACKED:" + t.name);
            boolean unused = m.state.equals("PENDING") && m.attempt == 0 && m.generation == 0
                    && m.cursor == null && m.rows == 0 && m.verifyCursor == null && m.verified == 0
                    && m.verifiedAt == 0 && t.seed().equals(m.copyHash) && t.seed().equals(m.verifyHash);
            if (!unused) throw new IllegalStateException("R29_MIGRATION_TRACKERS_LOST_AFTER_COPY_STARTED:" + t.name);
            sourceWriter.write(db -> trackSource(db, t));
            if (!sourceTracked(source, t)) throw new IllegalStateException("R29_MIGRATION_TRACKER_INSTALL_FAILED:" + t.name);
        }
        if (m.state.equals("PENDING")) {
            long generation = generation(source,t.name);
            tx.write(db -> {
                fence(db,m);
                // Only incomplete destination rows may be discarded. Original source and verified backups are retained.
                db.execSQL("DELETE FROM r29_copy_rows WHERE table_name=? AND attempt=?", new Object[]{t.name,m.attempt});
                db.execSQL("UPDATE r29_copy_manifest SET state='COPYING',attempt=attempt+1,source_generation=?,last_rowid=NULL,copied_rows=0,copy_hash=?,source_rows=NULL,verify_rowid=NULL,verified_rows=0,verify_hash=?,touched_at=?,retry_at=0,reason='' WHERE table_name=? AND state='PENDING' AND attempt=?",
                        new Object[]{generation,t.seed(),t.seed(),now,t.name,m.attempt});
            });
            return new Result(t.name,"COPY_STARTED",0);
        }
        if (m.state.equals("COPYING")) {
            if (generation(source,t.name)!=m.generation) return retry(tx,m,now,"SOURCE_CHANGED_BEFORE_PAGE");
            List<Row> page = readPage(source,t,m.cursor);
            if (generation(source,t.name)!=m.generation) return retry(tx,m,now,"SOURCE_CHANGED_DURING_PAGE");
            if (page.isEmpty()) {
                long rows = scalar(source,"SELECT count(*) FROM "+id(t.name),null);
                if (generation(source,t.name)!=m.generation) return retry(tx,m,now,"SOURCE_CHANGED_AT_WATERMARK");
                if (rows!=m.rows) throw new IllegalStateException("R29_COPY_ROW_COUNT_MISMATCH:"+t.name);
                tx.write(db -> { fence(db,m);db.execSQL("UPDATE r29_copy_manifest SET state='VERIFYING',source_rows=?,touched_at=? WHERE table_name=?",new Object[]{rows,now,t.name}); });
                return new Result(t.name,"SOURCE_COPY_COMPLETE",0);
            }
            String chain=m.copyHash;for(Row row:page) chain=chain(chain,row.rowid,row.hash);
            final String hash=chain; final long last=page.get(page.size()-1).rowid;
            tx.write(db -> {
                fence(db,m);
                for(Row row:page) {
                    db.execSQL("INSERT OR IGNORE INTO r29_copy_rows(table_name,attempt,source_rowid,payload,row_hash) VALUES(?,?,?,?,?)",new Object[]{t.name,m.attempt,row.rowid,row.payload,row.hash});
                    try(Cursor c=db.rawQuery("SELECT row_hash FROM r29_copy_rows WHERE table_name=? AND attempt=? AND source_rowid=?",new String[]{t.name,String.valueOf(m.attempt),String.valueOf(row.rowid)})) {
                        if(!c.moveToFirst()||!row.hash.equals(c.getString(0)))throw new IllegalStateException("R29_COPY_ROW_CONFLICT");
                    }
                }
                db.execSQL("UPDATE r29_copy_manifest SET last_rowid=?,copied_rows=copied_rows+?,copy_hash=?,touched_at=? WHERE table_name=?",new Object[]{last,page.size(),hash,now,t.name});
            });
            return new Result(t.name,"COPY_PAGE",page.size());
        }
        if (m.state.equals("VERIFYING")) {
            List<Row> page=new ArrayList<>();long started=System.nanoTime();
            String where=m.verifyCursor==null?"":" AND source_rowid>?";
            String[] args=m.verifyCursor==null?new String[]{t.name,String.valueOf(m.attempt)}:new String[]{t.name,String.valueOf(m.attempt),String.valueOf(m.verifyCursor)};
            try(Cursor c=destination.rawQuery("SELECT source_rowid,length(payload),row_hash,rowid FROM r29_copy_rows WHERE table_name=? AND attempt=?"+where+" ORDER BY source_rowid LIMIT "+PAGE,args)) {
                while(c.moveToNext()) {
                    long length=c.getLong(1);if(length<0||length>MAX_ROW_BYTES)throw new IllegalStateException("R29_MIGRATION_ROW_BUDGET");byte[] bytes=ResearchRowsR29.readBlob(destination,"r29_copy_rows","payload","rowid=?",new String[]{String.valueOf(c.getLong(3))},(int)length,()->{});String hash=StatsR27.sha256(bytes);
                    if(!hash.equals(c.getString(2)))throw new IllegalStateException("R29_DESTINATION_ROW_CHECKSUM:"+t.name);
                    page.add(new Row(c.getLong(0),bytes,hash));if(System.nanoTime()-started>=SLICE_NS)break;
                }
            }
            if(page.isEmpty()) {
                if(m.verified!=m.rows||!m.verifyHash.equals(m.copyHash))throw new IllegalStateException("R29_DESTINATION_CHAIN_MISMATCH:"+t.name);
                tx.write(db->{fence(db,m);db.execSQL("UPDATE r29_copy_manifest SET state='VERIFIED_BASELINE',verified_at=?,touched_at=?,reason='STAGED_BASELINE_NOT_CUTOVER' WHERE table_name=?",new Object[]{Math.max(1,now),now,t.name});});
                return new Result(t.name,"VERIFIED_BASELINE_NOT_CUTOVER",0);
            }
            String chain=m.verifyHash;for(Row row:page)chain=chain(chain,row.rowid,row.hash);
            final String hash=chain;final long last=page.get(page.size()-1).rowid;
            tx.write(db->{fence(db,m);db.execSQL("UPDATE r29_copy_manifest SET verify_rowid=?,verified_rows=verified_rows+?,verify_hash=?,touched_at=? WHERE table_name=?",new Object[]{last,page.size(),hash,now,t.name});});
            return new Result(t.name,"VERIFY_PAGE",page.size());
        }
        throw new IllegalStateException("R29_MIGRATION_UNKNOWN_PHASE:"+m.state);
    }

    static Result retry(Transaction tx, Meta m, long now, String reason) throws Exception {
        tx.write(db->{fence(db,m);db.execSQL("UPDATE r29_copy_manifest SET state='PENDING',touched_at=?,retry_at=?,reason=? WHERE table_name=?",new Object[]{now,now+1000,reason,m.table});});
        return new Result(m.table,reason,0);
    }
    static void fence(SQLiteDatabase db,Meta m) {
        if(!db.inTransaction())throw new IllegalStateException("R29_COPY_TRANSACTION_REQUIRED");
        try(Cursor c=db.rawQuery("SELECT state,attempt,source_generation,last_rowid,copied_rows,copy_hash,verify_rowid,verified_rows,verify_hash FROM r29_copy_manifest WHERE table_name=?",new String[]{m.table})) {
            if(!c.moveToFirst()||!m.state.equals(c.getString(0))||m.attempt!=c.getLong(1)||m.generation!=c.getLong(2)
                    ||!same(m.cursor,c,3)||m.rows!=c.getLong(4)||!m.copyHash.equals(c.getString(5))
                    ||!same(m.verifyCursor,c,6)||m.verified!=c.getLong(7)||!m.verifyHash.equals(c.getString(8)))
                throw new IllegalStateException("R29_STALE_MIGRATION_PAGE");
        }
    }
    static boolean same(Long value,Cursor c,int i){return value==null?c.isNull(i):!c.isNull(i)&&value==c.getLong(i);}
    static long generation(SQLiteDatabase source,String table) {
        try(Cursor c=source.rawQuery("SELECT generation FROM r29_source_watermark WHERE table_name=?",new String[]{table})) {
            if(!c.moveToFirst())throw new IllegalStateException("R29_SOURCE_NOT_TRACKED:"+table);return c.getLong(0);
        }
    }
    static long scalar(SQLiteDatabase db,String sql,String[] args){try(Cursor c=db.rawQuery(sql,args)){if(!c.moveToFirst())throw new IllegalStateException("R29_EXPECTED_SCALAR");return c.getLong(0);}}
    static String id(String value){if(value==null||!value.matches("[A-Za-z_][A-Za-z_0-9]*"))throw new IllegalArgumentException("R29_SQL_IDENTIFIER");return "\""+value+"\"";}
    static String chain(String previous,long rowid,String hash){return StatsR27.sha256(previous+":"+rowid+":"+hash);}
    static final class Row {final long rowid;final byte[] payload;final String hash;Row(long rowid,byte[] payload,String hash){this.rowid=rowid;this.payload=payload;this.hash=hash;}}
    static List<Row> readPage(SQLiteDatabase source,Table table,Long after) {
        return ResearchRowsR29.readPage(source,table,after,"1=1",PAGE,2L<<20,SLICE_NS,()->{});
    }

    /** Type tags and byte lengths distinguish NULL, the text "NULL", integer/float values and arbitrary BLOBs. */
    static byte[] encode(Cursor row,int offset,int count) {
        try {
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
            out.writeInt(0x29DB0001);out.writeInt(count);
            for(int j=0;j<count;j++) {
                int i=offset+j,type=row.getType(i);out.writeByte(type);
                switch(type) {
                    case Cursor.FIELD_TYPE_NULL: break;
                    case Cursor.FIELD_TYPE_INTEGER: out.writeLong(row.getLong(i));break;
                    case Cursor.FIELD_TYPE_FLOAT: out.writeLong(Double.doubleToLongBits(row.getDouble(i)));break;
                    case Cursor.FIELD_TYPE_STRING: blob(out,row.getString(i).getBytes(StandardCharsets.UTF_8));break;
                    case Cursor.FIELD_TYPE_BLOB: blob(out,row.getBlob(i));break;
                    default: throw new IllegalStateException("R29_SQLITE_VALUE_TYPE");
                }
                if(bytes.size()>MAX_ROW_BYTES)throw new IllegalStateException("R29_MIGRATION_ROW_BUDGET");
            }
            out.close();return bytes.toByteArray();
        }catch(IOException impossible){throw new IllegalStateException("R29_TYPED_ROW_ENCODE",impossible);}
    }
    static void blob(DataOutputStream out,byte[] value)throws IOException{if(value.length>MAX_ROW_BYTES)throw new IllegalStateException("R29_MIGRATION_ROW_BUDGET");out.writeInt(value.length);out.write(value);}

    public static Map<String,Object> diagnostics(SQLiteDatabase source,SQLiteDatabase destination) {
        Map<String,Object> out=new LinkedHashMap<>();List<Object> tables=new ArrayList<>();
        out.put("state","STAGED_COPY_NOT_CUTOVER");out.put("activeOwner","ORIGINAL_DATABASE");
        out.put("cutoverEnabled",false);out.put("crossTableSnapshot","NOT_ESTABLISHED");
        out.put("realTimeAuthority","draws, decisions, settlements, r29_event_outbox, r27_fetch_reject remain original");
        out.put("recomputableExcluded","r27_chart_cache");
        Map<String,Long> watermarks=new LinkedHashMap<>();
        if(objectSql(source,"table","r29_source_watermark")!=null)
            try(Cursor c=source.rawQuery("SELECT table_name,generation FROM r29_source_watermark",null)){while(c.moveToNext())watermarks.put(c.getString(0),c.getLong(1));}
        try(Cursor c=destination.rawQuery("SELECT table_name,state,source_generation,copied_rows,verified_rows,copy_hash,verified_at,reason,attempt FROM r29_copy_manifest ORDER BY table_name",null)) {
            while(c.moveToNext()) {
                Map<String,Object> t=new LinkedHashMap<>();String name=c.getString(0);Long current=watermarks.get(name);
                boolean started=c.getLong(8)>0||!"PENDING".equals(c.getString(1))||c.getLong(3)>0||c.getLong(4)>0||c.getLong(6)>0;
                if(started&&current==null)throw new IllegalStateException("R29_SOURCE_NOT_TRACKED:"+name);
                // Unselected catalog entries have no admitted Root tracker or snapshot generation yet.
                // Unknown is not zero, and catalog registration never counts as a verified baseline.
                t.put("table",name);t.put("state",c.getString(1));t.put("sourceWatermark",started?c.getLong(2):null);t.put("currentWatermark",current);
                t.put("catchUpRequired",started?current.longValue()!=c.getLong(2):null);t.put("copiedRows",c.getLong(3));t.put("verifiedRows",c.getLong(4));
                t.put("canonicalHash",c.getString(5));t.put("verifiedAt",c.getLong(6)>0?c.getLong(6):null);t.put("reason",c.getString(7));tables.add(t);
            }
        }
        out.put("tables",tables);return out;
    }
}
