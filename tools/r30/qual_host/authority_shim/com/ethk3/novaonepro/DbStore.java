package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
/** Read-only HOST adapter for the real authority bytecode; never compiled into the app. */
public final class DbStore {
    private final SQLiteDatabase db;
    public DbStore(SQLiteDatabase db){this.db=db;}
    public SQLiteDatabase readable(){return db;}
    public String getState(String key,String fallback){try(Cursor c=db.rawQuery("SELECT value FROM host_state WHERE key=?",new String[]{key})){return c.moveToFirst()?c.getString(0):fallback;}}
    public Models.Trade getTradeByTargetIssue(String target){try(Cursor c=db.rawQuery("SELECT 1 FROM trades WHERE target_issue=? AND action='BET'",new String[]{target})){return c.moveToFirst()?new Models.Trade():null;}}
}
