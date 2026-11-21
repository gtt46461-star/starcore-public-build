import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

/** Size-only fixture on a fresh controlled guest. Every application table and research row stays original. */
public final class SizeFixture {
    static long number(SQLiteDatabase db, String sql) {
        try (Cursor c=db.rawQuery(sql,null)) {
            if (!c.moveToFirst()) throw new IllegalStateException("ROW_MISSING");
            return c.getLong(0);
        }
    }
    static String schema(SQLiteDatabase db)throws Exception {
        MessageDigest d=MessageDigest.getInstance("SHA-256");
        try (Cursor c=db.rawQuery("SELECT name,coalesce(sql,'') FROM sqlite_master ORDER BY type,name",null)) {
            while(c.moveToNext())for(int k=0;k<2;k++){
                byte[] v=c.getString(k).getBytes(StandardCharsets.UTF_8); d.update(v); d.update((byte)0);
            }
        }
        StringBuilder s=new StringBuilder();for(byte b:d.digest())s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();
    }
    public static void main(String[] args)throws Exception {
        String root="/data/user/0/com.starcore.research.r13/";
        File data=new File(args[0]);
        if (!args[0].startsWith(root+"databases/") || !data.isFile())throw new IllegalStateException("UNEXPECTED_GUEST_DATABASE");
        long target=1373483008L;
        try(SQLiteDatabase db=SQLiteDatabase.openDatabase(data.getPath(),null,SQLiteDatabase.OPEN_READWRITE|SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING)) {
            long draws=number(db,"SELECT count(*) FROM draws"),page=number(db,"PRAGMA page_size"),prior=number(db,"PRAGMA page_count")*page;
            if(prior>=target || number(db,"PRAGMA auto_vacuum")!=0)throw new IllegalStateException("UNEXPECTED_FIXTURE_LAYOUT");
            String original=schema(db);
            db.execSQL("CREATE TABLE r335_disposable_size_fixture(p BLOB)");
            while(number(db,"PRAGMA page_count")*page<target)db.execSQL("INSERT INTO r335_disposable_size_fixture VALUES(zeroblob(8388608))");
            db.execSQL("DROP TABLE r335_disposable_size_fixture");
            try(Cursor c=db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)",null)){
                if(!c.moveToFirst() || c.getInt(0)!=0)throw new IllegalStateException("FIXTURE_WAL_BUSY");
            }
            long bytes=number(db,"PRAGMA page_count")*page;
            if(bytes<target || draws!=number(db,"SELECT count(*) FROM draws") || !original.equals(schema(db)))throw new IllegalStateException("FIXTURE_SCHEMA_OR_ROWS_CHANGED");
            System.out.println("SIZE_FIXTURE|"+prior+"|"+bytes+"|"+number(db,"PRAGMA freelist_count")+"|"+page+"|"+draws+"|"+original);
        }
        File protectedFile=new File(root+"files/r335-qa-retained-original");
        if(!protectedFile.createNewFile())throw new IllegalStateException("FRESH_PRIVATE_ORIGINAL_REQUIRED");
        try(RandomAccessFile f=new RandomAccessFile(protectedFile,"rw")){f.setLength(1L<<30);}
    }
}
