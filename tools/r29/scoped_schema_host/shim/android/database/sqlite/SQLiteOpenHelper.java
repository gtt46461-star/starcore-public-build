package android.database.sqlite;

import android.content.Context;
import java.util.Map;
import java.util.WeakHashMap;

/** HOST framework helper for an already installed real JDBC fixture. Explicit binding skips framework
 * construction/startup only; product DbHelper and coordinator methods remain their actual production code.
 * This class does not simulate Android SQLiteSession or claim Android connection-pool behavior. */
public abstract class SQLiteOpenHelper implements java.io.Closeable {
    private static final Map<SQLiteOpenHelper,SQLiteDatabase> BOUND=new WeakHashMap<>();
    private Context context;private String name;private boolean wal;
    protected SQLiteOpenHelper(){}
    public SQLiteOpenHelper(Context context,String name,SQLiteDatabase.CursorFactory factory,int version){this.context=context;this.name=name;}
    public static void bind(SQLiteOpenHelper helper,SQLiteDatabase database){if(helper==null||database==null)throw new IllegalArgumentException("HOST_HELPER_BIND_REQUIRED");synchronized(BOUND){BOUND.put(helper,database);}}
    public synchronized SQLiteDatabase getReadableDatabase(){synchronized(BOUND){SQLiteDatabase db=BOUND.get(this);if(db!=null&&db.isOpen())return db;}
        if(context==null||name==null)throw new IllegalStateException("HOST_HELPER_MUST_BIND_INSTALLED_FIXTURE");
        java.io.File path=context.getDatabasePath(name);if(!path.isFile())throw new IllegalStateException("HOST_HELPER_REQUIRES_INSTALLED_DATABASE:"+path);
        SQLiteDatabase db=SQLiteDatabase.openDatabase(path.getPath(),null,SQLiteDatabase.OPEN_READWRITE);if(wal)db.enableWriteAheadLogging();bind(this,db);return db;
    }
    public synchronized SQLiteDatabase getWritableDatabase(){SQLiteDatabase db=getReadableDatabase();if(db.isReadOnly())throw new SQLiteException("HOST_BOUND_DATABASE_READ_ONLY");return db;}
    public Context getContext(){return context;}
    public String getDatabaseName(){return name;}
    public void setWriteAheadLoggingEnabled(boolean enabled){wal=enabled;synchronized(BOUND){SQLiteDatabase db=BOUND.get(this);if(db!=null&&enabled)db.enableWriteAheadLogging();}}
    public void onConfigure(SQLiteDatabase db){}
    public abstract void onCreate(SQLiteDatabase db);
    public abstract void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion);
    public void onDowngrade(SQLiteDatabase db,int oldVersion,int newVersion){throw new SQLiteException("HOST_DOWNGRADE_UNSUPPORTED");}
    public void onOpen(SQLiteDatabase db){}
    public synchronized void close(){SQLiteDatabase db;synchronized(BOUND){db=BOUND.remove(this);}if(db!=null)db.close();}
}
