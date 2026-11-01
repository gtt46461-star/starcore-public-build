package com.ethk3.novaonepro;
import android.database.sqlite.SQLiteDatabase;
public final class DbStore {
 public interface Write<T>{T run(SQLiteDatabase db);}
 public final Coordinator coordinator=new Coordinator();
 public Coordinator coordinatorForResearchR952(){return coordinator;}
 public static final class Coordinator {public boolean fail=true;public final SQLiteDatabase db=new SQLiteDatabase();public <T>T runAtomicWriteOrThrowF01(String name,Write<T> work){if(fail){fail=false;throw new IllegalStateException("INJECTED_BUSY");}return work.run(db);}}
}
