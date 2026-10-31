package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.DatabaseCoordinator;
import java.util.ArrayList;
import java.util.List;

/**
 * R30 通用整表复制（B00 回收R29独立库、B11 条件切库共用）。
 *
 * 不使用 ATTACH：Android 在 WAL 库上执行 ATTACH 会自动关闭 WAL（SQLiteDatabase.executeSql 对 ATTACH 的处理），
 * 因此源库以独立只读连接打开，按 rowid 分页读出，在目标库的短事务里 INSERT OR REPLACE，每页 ≤ 500 行。
 * 列按名称对齐：只复制两侧都存在的列；目标缺失的列记 MISMATCH，不静默丢弃（写入 r30_copy_log）。
 */
public final class TableCopierR30 {
    public static final int PAGE_ROWS = 500;
    static final int CELL_CHUNK_BYTES_R30=64*1024,INLINE_ROW_BYTES_R30=64*1024;
    static final long PAGE_BYTES_R30=1024*1024;
    public static final String LOG_DDL = "CREATE TABLE IF NOT EXISTS r30_copy_log(run_id TEXT NOT NULL,table_name TEXT NOT NULL,direction TEXT NOT NULL,"
            + "source_rows INTEGER NOT NULL,copied_rows INTEGER NOT NULL,missing_columns TEXT NOT NULL,state TEXT NOT NULL,finished_at INTEGER NOT NULL,"
            + "PRIMARY KEY(run_id,table_name,direction))";
    private TableCopierR30() {}

    /** 目标写入口：B00 回收用 DatabaseCoordinator（Root 唯一写者）；B11 用研究库自身单写者。 */
    public interface Writer { void tx(String operation, DatabaseCoordinator.WriteRunnable body) throws Exception; }

    public static final class Outcome {
        public final String table; public final long sourceRows, copiedRows; public final List<String> missingColumns; public final String state;
        Outcome(String t, long s, long c, List<String> m, String st) { table = t; sourceRows = s; copiedRows = c; missingColumns = m; state = st; }
    }

    public static List<String> columns(SQLiteDatabase db, String table) {
        List<String> out = new ArrayList<>();
        try (Cursor c = db.rawQuery("PRAGMA table_info(" + q(table) + ")", null)) { while (c.moveToNext()) out.add(c.getString(1)); }
        return out;
    }

    public static boolean hasRowid(SQLiteDatabase db, String table) {
        try (Cursor c = db.rawQuery("SELECT sql FROM sqlite_master WHERE type='table' AND name=?", new String[] {table})) {
            return c.moveToFirst() && !String.valueOf(c.getString(0)).toUpperCase(java.util.Locale.ROOT).replaceAll("\\s+", " ").contains("WITHOUT ROWID");
        }
    }

    public static long count(SQLiteDatabase db, String table) {
        try (Cursor c = db.rawQuery("SELECT count(*) FROM " + q(table), null)) { return c.moveToFirst() ? c.getLong(0) : 0; }
    }

    /** 复制一张表；目标表不存在时用源表 DDL 建表。返回结果并写 r30_copy_log。 */
    public static Outcome copy(final SQLiteDatabase source, final SQLiteDatabase targetRo, Writer target, final String table,
                               final String runId, final String direction) throws Exception {
        final List<String> src = columns(source, table);
        List<String> dst = columns(targetRo, table);
        if (dst.isEmpty()) {
            final String ddl;
            try (Cursor c = source.rawQuery("SELECT sql FROM sqlite_master WHERE type='table' AND name=?", new String[] {table})) {
                ddl = c.moveToFirst() ? c.getString(0) : null;
            }
            if (ddl == null) throw new IllegalStateException("R30_COPY_SOURCE_DDL_MISSING:" + table);
            target.tx("R30_COPY_CREATE", db -> db.execSQL(ddl));
            dst = columns(targetRo, table);
        }
        final List<String> common = new ArrayList<>(), missing = new ArrayList<>();
        for (String c : src) if (dst.contains(c)) common.add(c); else missing.add(c);
        if (common.isEmpty()) throw new IllegalStateException("R30_COPY_NO_COMMON_COLUMNS:" + table);
        StringBuilder cols=new StringBuilder(),sizes=new StringBuilder(),types=new StringBuilder(),values=new StringBuilder();
        for(int i=0;i<common.size();i++){
            String col=q(common.get(i));if(i>0){cols.append(',');sizes.append('+');types.append(',');values.append(',');}
            cols.append(col);sizes.append("coalesce(length(CAST(").append(col).append(" AS BLOB)),0)");types.append("typeof(").append(col).append(')');
            // Raw UTF-8 bytes plus the metadata type preserve TEXT, including embedded NUL/invalid UTF-8.
            values.append("CASE WHEN typeof(").append(col).append(")='text' THEN CAST(").append(col).append(" AS BLOB) ELSE ").append(col).append(" END");
        }
        final String insertPrefix="INSERT OR REPLACE INTO "+q(table)+"("+cols+") VALUES(";
        String availableRowid=null;if(hasRowid(source,table))for(String alias:new String[]{"rowid","_rowid_","oid"})if(!src.contains(alias)){availableRowid=alias;break;}
        final String rowidName=availableRowid;final boolean rowid=rowidName!=null;
        final String order=rowid?rowidName:pkOrder(source,table);
        final long sourceRows = count(source, table);
        long copied=0,lastRowid=0,offset=0;boolean haveLast=false;
        while (true) {
            String where=rowid&&haveLast?" WHERE "+rowidName+">?":"";
            String[] args=rowid&&haveLast?new String[]{Long.toString(lastRowid)}:null;
            String tail=" FROM "+q(table)+where+" ORDER BY "+order;
            String metadata="SELECT "+(rowid?rowidName:"0")+","+sizes+","+types+tail+" LIMIT "+PAGE_ROWS+(rowid?"":" OFFSET "+offset);
            final List<RowR30> refs=new ArrayList<>(PAGE_ROWS);long bytes=0;
            try(Cursor c=source.rawQuery(metadata,args)){
                while(c.moveToNext()){
                    long size=c.getLong(1);if(!refs.isEmpty()&&size>PAGE_BYTES_R30-bytes)break;
                    String[] storage=new String[common.size()];for(int i=0;i<storage.length;i++)storage[i]=c.getString(i+2);
                    refs.add(new RowR30(c.getLong(0),offset+refs.size(),size,storage));bytes+=size;
                }
            }
            if(refs.isEmpty())break;
            boolean smallPage=true;for(RowR30 ref:refs)if(ref.bytes>INLINE_ROW_BYTES_R30){smallPage=false;break;}
            final List<Object[]> page = new ArrayList<>(PAGE_ROWS);
            if(refs.size()==1&&refs.get(0).bytes>INLINE_ROW_BYTES_R30){
                RowR30 ref=refs.get(0);String locate=rowid?" WHERE "+rowidName+"=?":" ORDER BY "+order+" LIMIT 1 OFFSET "+ref.offset;
                String[] locateArgs=rowid?new String[]{Long.toString(ref.id)}:null;
                Object[] row=new Object[common.size()];for(int i=0;i<row.length;i++)row[i]=boundedCellR30(source,table,common.get(i),locate,locateArgs,ref.types[i]);page.add(row);
            }else if(smallPage){
                String select="SELECT "+(rowid?rowidName:"0")+","+values+tail+" LIMIT "+refs.size()+(rowid?"":" OFFSET "+offset);
                try(Cursor c=source.rawQuery(select,args)){
                    int at=0;while(c.moveToNext()){
                        if(at>=refs.size()||rowid&&c.getLong(0)!=refs.get(at).id)throw new IllegalStateException("R30_COPY_SOURCE_PAGE_CHANGED:"+table);
                        Object[] row=new Object[common.size()];for(int i=0;i<row.length;i++)row[i]="text".equals(refs.get(at).types[i])?new TextBytesR30(c.getBlob(i+1)):value(c,i+1);page.add(row);at++;
                    }
                }
            }else{
                // A large row may follow several small ones in the same bounded page. Read cells outside the writer.
                for(RowR30 ref:refs){String locate=rowid?" WHERE "+rowidName+"=?":" ORDER BY "+order+" LIMIT 1 OFFSET "+ref.offset;String[] locateArgs=rowid?new String[]{Long.toString(ref.id)}:null;Object[] row=new Object[common.size()];for(int i=0;i<row.length;i++)row[i]=boundedCellR30(source,table,common.get(i),locate,locateArgs,ref.types[i]);page.add(row);}
            }
            if(page.size()!=refs.size())throw new IllegalStateException("R30_COPY_SOURCE_PAGE_MISSING:"+table);
            target.tx("R30_COPY_PAGE",db->{for(Object[] row:page){StringBuilder marks=new StringBuilder();Object[] bind=row.clone();for(int i=0;i<row.length;i++){if(i>0)marks.append(',');if(row[i] instanceof TextBytesR30){marks.append("CAST(? AS TEXT)");bind[i]=((TextBytesR30)row[i]).bytes;}else marks.append('?');}db.execSQL(insertPrefix+marks+")",bind);}});
            copied += page.size();
            offset += page.size();
            if(rowid){lastRowid=refs.get(refs.size()-1).id;haveLast=true;}
        }
        final String state = copied != sourceRows ? "COUNT_MISMATCH" : missing.isEmpty() ? "COPIED" : "COPIED_WITH_MISSING_COLUMNS";
        final long copiedFinal = copied;
        target.tx("R30_COPY_LOG", db -> {
            db.execSQL(LOG_DDL);
            db.execSQL("INSERT OR REPLACE INTO r30_copy_log VALUES(?,?,?,?,?,?,?,?)",
                    new Object[] {runId, table, direction, sourceRows, copiedFinal, String.join(",", missing), state, System.currentTimeMillis()});
        });
        return new Outcome(table, sourceRows, copied, missing, state);
    }

    static final class RowR30 {final long id,offset,bytes;final String[] types;RowR30(long id,long offset,long bytes,String[] types){this.id=id;this.offset=offset;this.bytes=bytes;this.types=types;}}
    static final class TextBytesR30 {final byte[] bytes;TextBytesR30(byte[] bytes){if(bytes==null)throw new IllegalStateException("R30_COPY_TEXT_BYTES_MISSING");this.bytes=bytes;}}
    /** Each returned cursor contains at most one 64KiB cell, never a whole large model row. */
    static Object boundedCellR30(SQLiteDatabase source,String table,String column,String locate,String[] args,String expected)throws Exception{
        String col=q(column),from=" FROM "+q(table)+locate;long size;
        try(Cursor c=source.rawQuery("SELECT typeof("+col+"),coalesce(length(CAST("+col+" AS BLOB)),0),CASE WHEN typeof("+col+") IN ('text','blob') THEN NULL ELSE "+col+" END"+from,args)){
            if(!c.moveToFirst()||!expected.equals(c.getString(0)))throw new IllegalStateException("R30_COPY_SOURCE_CELL_CHANGED:"+table+":"+column);
            if(!expected.equals("text")&&!expected.equals("blob"))return value(c,2);size=c.getLong(1);
        }
        if(size<0||size>Integer.MAX_VALUE-8)throw new IllegalStateException("R30_COPY_CELL_LENGTH_UNSUPPORTED:"+table+":"+column+":"+size);
        byte[] all=new byte[(int)size];
        for(int at=0;at<all.length;){int need=Math.min(CELL_CHUNK_BYTES_R30,all.length-at);try(Cursor c=source.rawQuery("SELECT substr(CAST("+col+" AS BLOB),"+((long)at+1)+","+need+")"+from,args)){
            if(!c.moveToFirst()||c.isNull(0))throw new IllegalStateException("R30_COPY_CELL_CHUNK_MISSING:"+table+":"+column);byte[] part=c.getBlob(0);if(part.length!=need)throw new IllegalStateException("R30_COPY_CELL_CHUNK_SHORT:"+table+":"+column);System.arraycopy(part,0,all,at,need);at+=need;
        }}
        return expected.equals("text")?new TextBytesR30(all):all;
    }

    /** WITHOUT ROWID 表按主键列顺序分页（主键列从 PRAGMA table_info 的 pk 序号取得）。 */
    static String pkOrder(SQLiteDatabase db, String table) {
        java.util.TreeMap<Integer, String> pk = new java.util.TreeMap<>();
        try (Cursor c = db.rawQuery("PRAGMA table_info(" + q(table) + ")", null)) { while (c.moveToNext()) if (c.getInt(5) > 0) pk.put(c.getInt(5), q(c.getString(1))); }
        if (pk.isEmpty()) throw new IllegalStateException("R30_COPY_WITHOUT_ROWID_NO_PK:" + table);
        return String.join(",", pk.values());
    }

    static Object value(Cursor c, int i) {
        switch (c.getType(i)) {
            case Cursor.FIELD_TYPE_NULL: return null;
            case Cursor.FIELD_TYPE_INTEGER: return c.getLong(i);
            case Cursor.FIELD_TYPE_FLOAT: return c.getDouble(i);
            case Cursor.FIELD_TYPE_BLOB: return c.getBlob(i);
            default: return c.getString(i);
        }
    }

    static String q(String identifier) { return "\"" + identifier.replace("\"", "\"\"") + "\""; }
}
