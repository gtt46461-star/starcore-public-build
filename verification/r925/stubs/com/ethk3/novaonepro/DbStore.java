package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.LinkedHashMap;
import java.util.Map;

/** Host-only SQLite adapter used by deterministic contract harnesses. */
public class DbStore {
    private final SQLiteDatabase db;

    public DbStore(SQLiteDatabase db) {
        if (db == null) throw new IllegalArgumentException("HOST_DBSTORE_DB_NULL");
        this.db = db;
        db.execSQL("CREATE TABLE IF NOT EXISTS app_state("
                + "state_key TEXT PRIMARY KEY,state_value TEXT NOT NULL,updated_at INTEGER NOT NULL)");
    }

    public Models.Draw getDrawByIssue(String issue) {
        try(Cursor c=db.rawQuery("SELECT n1,n2,n3,n4,n5,n6,n7,n8,n9,n10,dirty FROM "+DbHelper.TABLE_DRAWS+" WHERE issue=?",new String[]{issue})) {
            if(!c.moveToFirst()||c.getInt(10)!=0)return null;
            Models.Draw draw=new Models.Draw();draw.issue=issue;for(int n=0;n<10;n++)draw.numbers[n]=c.getInt(n);return draw;
        }
    }

    public java.util.List<Models.Draw> getRecentDraws(int limit){
        java.util.List<Models.Draw> rows=new java.util.ArrayList<Models.Draw>();
        try(Cursor c=db.rawQuery("SELECT issue,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10 FROM "+DbHelper.TABLE_DRAWS+" WHERE dirty=0 ORDER BY issue DESC LIMIT ?",new String[]{String.valueOf(limit)})){
            while(c.moveToNext()){Models.Draw d=new Models.Draw();d.issue=c.getString(0);for(int i=0;i<10;i++)d.numbers[i]=c.getInt(i+1);rows.add(d);}
        }
        java.util.Collections.reverse(rows);return rows;
    }
    public Models.Draw getLatestDraw(){java.util.List<Models.Draw> rows=getRecentDraws(1);return rows.isEmpty()?null:rows.get(0);}
    public SQLiteDatabase readable() { return db; }
    public SQLiteDatabase writable() { return db; }
    public DatabaseCoordinator coordinatorR36() { return new DatabaseCoordinator(db); }

    public String getState(String key, String defaultValue) {
        if (key == null || key.length() == 0) return defaultValue;
        Cursor cursor = db.rawQuery(
                "SELECT state_value FROM app_state WHERE state_key=? LIMIT 1",
                new String[] {key});
        try {
            return cursor.moveToFirst() ? cursor.getString(0) : defaultValue;
        } finally {
            cursor.close();
        }
    }

    public void setState(String key, String value) {
        LinkedHashMap<String, String> one = new LinkedHashMap<String, String>();
        one.put(key, value);
        setStates(one);
    }

    public void setStates(final Map<String, String> values) {
        if (values == null || values.isEmpty()) return;
        boolean committed = coordinatorR36().runWrite(
                "HOST_DBSTORE_SET_STATES",
                new DatabaseCoordinator.WriteRunnable() {
                    @Override public void run(SQLiteDatabase target) {
                        putStatesR36(target, values);
                    }
                });
        if (!committed) throw new IllegalStateException("HOST_DBSTORE_SET_STATES_NOT_COMMITTED");
    }

    static void putStatesR36(SQLiteDatabase db, Map<String, String> values) {
        if (db == null || values == null || values.isEmpty()) return;
        long now = System.currentTimeMillis();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (entry.getKey() == null || entry.getKey().length() == 0) continue;
            db.execSQL(
                    "INSERT OR REPLACE INTO app_state(state_key,state_value,updated_at) VALUES(?,?,?)",
                    new Object[] {entry.getKey(), entry.getValue() == null ? "" : entry.getValue(), now});
        }
    }
}
