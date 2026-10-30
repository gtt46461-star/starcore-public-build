package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.sql.DriverManager;
import java.util.*;

/** Real SQLite pagination: no window-function dependency, no key hashes and no user data. */
public final class ResearchRowsR29HostTest {
    static int checks;
    static void check(String name,boolean valid){System.out.println("R29_ROWS|"+name+"|"+(valid?"OK":"FAIL"));if(!valid)throw new AssertionError(name);checks++;}
    static MigrationCopyR29.Table table(SQLiteDatabase db,String name){String ddl;try(Cursor c=db.rawQuery("SELECT sql FROM sqlite_master WHERE type='table' AND name=?",new String[]{name})){if(!c.moveToFirst())throw new AssertionError(name);ddl=c.getString(0);}List<String> columns=new ArrayList<>();try(Cursor c=db.rawQuery("PRAGMA table_info("+MigrationCopyR29.id(name)+")",null)){while(c.moveToNext())columns.add(c.getString(1));}return new MigrationCopyR29.Table(name,ddl,columns,"HOST_TYPED_SCHEMA_FIXTURE");}
    static List<String> page(SQLiteDatabase db,MigrationCopyR29.Table table,Long after,int limit){
        return page(db,table,after,limit,false);
    }
    static List<String> page(SQLiteDatabase db,MigrationCopyR29.Table table,Long after,int limit,boolean portable){
        List<String> rows=new ArrayList<>();StringBuilder fields=new StringBuilder("rowid");for(String column:table.columns)fields.append(',').append(MigrationCopyR29.id(column));
        try(Cursor c=db.rawQuery("SELECT "+fields+" FROM "+(portable?ResearchRowsR29.cursorSourcePortable(db,table):ResearchRowsR29.cursorSource(db,table))+(after==null?"":" WHERE rowid>?")+" ORDER BY rowid LIMIT "+limit,after==null?null:new String[]{String.valueOf(after)})){
            while(c.moveToNext()){StringBuilder row=new StringBuilder(Long.toString(c.getLong(0)));for(int i=1;i<=table.columns.size();i++)row.append('|').append(c.isNull(i)?"NULL":c.getType(i)==Cursor.FIELD_TYPE_BLOB?Arrays.toString(c.getBlob(i)):c.getString(i));rows.add(row.toString());}
        }return rows;
    }
    public static void main(String[] args)throws Exception{
        try(SQLiteDatabase db=new SQLiteDatabase(DriverManager.getConnection("jdbc:sqlite::memory:"))){
            db.execSQL("CREATE TABLE ordinary(id INTEGER PRIMARY KEY,t TEXT DEFAULT 'WITHOUT ROWID')");db.execSQL("INSERT INTO ordinary(id) VALUES(-9),(0),(17)");MigrationCopyR29.Table ordinary=table(db,"ordinary");
            check("W1_LITERAL_DOES_NOT_CHANGE_TABLE_KIND",!ResearchRowsR29.withoutRowid(ordinary));
            check("W2_PHYSICAL_NEGATIVE_INTEGER_ROWIDS_ARE_PRESERVED",page(db,ordinary,null,9).get(0).startsWith("-9|-9|"));
            check("W3_PHYSICAL_CURSOR_ACCEPTS_ANDROID_TEXT_BINDINGS",page(db,ordinary,-9L,9).size()==2);
            db.execSQL("CREATE TABLE text_key(k TEXT PRIMARY KEY,v TEXT) WITHOUT ROWID");db.execSQL("INSERT INTO text_key VALUES('z','last'),('a','first'),('汉字','unicode'),('a''b','quote')");MigrationCopyR29.Table text=table(db,"text_key");
            check("W4_WITHOUT_ROWID_OPTION_DETECTED",ResearchRowsR29.withoutRowid(text));
            List<String> all=page(db,text,null,10);check("W5_STRING_KEYS_HAVE_STABLE_DISTINCT_INTEGER_RANKS",all.equals(Arrays.asList("1|a|first","2|a'b|quote","3|z|last","4|汉字|unicode")));
            List<String> paged=new ArrayList<>();Long after=null;for(;;){List<String> next=page(db,text,after,1);if(next.isEmpty())break;paged.addAll(next);after=Long.valueOf(next.get(0).split("\\|",2)[0]);}
            check("W6_TEXT_BOUND_VIRTUAL_CURSOR_NEITHER_DROPS_NOR_REPEATS_ROWS",paged.equals(all));
            db.execSQL("CREATE TABLE composite(a TEXT,b INTEGER,v BLOB,PRIMARY KEY(b,a)) WITHOUT ROWID");db.execSQL("INSERT INTO composite VALUES('z',-20,X'00FF'),('a',-20,NULL),('a',5,X'01'),('b',0,X'')");MigrationCopyR29.Table composite=table(db,"composite");
            check("W7_COMPOSITE_PRIMARY_KEY_ORDINAL_ORDER_IS_USED",page(db,composite,null,10).equals(Arrays.asList("1|a|-20|NULL","2|z|-20|[0, -1]","3|b|0|[]","4|a|5|[1]")));
            check("W8_SECOND_COMPOSITE_PAGE_IS_EXACT",page(db,composite,2L,2).equals(page(db,composite,null,10).subList(2,4)));
            db.execSQL("CREATE TABLE empty_table(k TEXT PRIMARY KEY) WITHOUT ROWID");check("W9_EMPTY_WITHOUT_ROWID_HAS_NO_INVENTED_ROW",page(db,table(db,"empty_table"),null,9).isEmpty());
            db.execSQL("CREATE TABLE case_key(k TEXT COLLATE NOCASE PRIMARY KEY COLLATE BINARY,v TEXT) WITHOUT ROWID");db.execSQL("INSERT INTO case_key VALUES('a','lower'),('A','upper')");
            check("W10_BINARY_TIE_BREAKER_PREVENTS_COLLATION_RANK_COLLISIONS",page(db,table(db,"case_key"),null,9).equals(Arrays.asList("1|A|upper","2|a|lower")));
            db.execSQL("CREATE TABLE blob_key(k BLOB PRIMARY KEY) WITHOUT ROWID");db.execSQL("INSERT INTO blob_key VALUES(X'FF'),(X'00'),(X'0001')");
            check("W11_BLOB_PRIMARY_KEYS_ARE_NOT_TRUNCATED_OR_HASHED",page(db,table(db,"blob_key"),null,9).equals(Arrays.asList("1|[0]","2|[0, 1]","3|[-1]")));
            db.execSQL("CREATE TABLE reserved(rowid TEXT PRIMARY KEY) WITHOUT ROWID");boolean rejected=false;try{ResearchRowsR29.cursorSource(db,table(db,"reserved"));}catch(IllegalStateException expected){rejected=expected.getMessage().startsWith("R29_VIRTUAL_ROWID_COLUMN_CONFLICT");}
            check("W12_AMBIGUOUS_USER_ROWID_COLUMN_FAILS_CLOSED",rejected);
            check("W13_MODERN_SQLITE_USES_WINDOW_RANKING",ResearchRowsR29.cursorSource(db,text).contains("ROW_NUMBER() OVER"));
            boolean same=true;for(MigrationCopyR29.Table t:Arrays.asList(ordinary,text,composite,table(db,"empty_table"),table(db,"case_key"),table(db,"blob_key")))same&=page(db,t,null,20).equals(page(db,t,null,20,true));
            check("W14_PORTABLE_AND_MODERN_RANKS_MATCH_ALL_TYPED_FIXTURES",same);
            check("W15_PORTABLE_TEXT_BOUND_CURSOR_HAS_IDENTICAL_SECOND_PAGE",page(db,text,2L,2).equals(page(db,text,2L,2,true))&&!ResearchRowsR29.cursorSourcePortable(db,text).contains("ROW_NUMBER"));
            check("W16_VERSION_GUARD_KEEPS_OLDER_ANDROID_ON_PORTABLE_SQL",!ResearchRowsR29.windowFunctionsAvailable("3.24.2")&&!ResearchRowsR29.windowFunctionsAvailable("3.9.0")&&!ResearchRowsR29.windowFunctionsAvailable("unknown")&&ResearchRowsR29.windowFunctionsAvailable("3.25.0")&&ResearchRowsR29.windowFunctionsAvailable("4.0.0"));
        }
        System.out.println("R29_ROWS|ALL_PASS|checks="+checks+"|HOST_SQL_ONLY_ANDROID_CUTOVER_PENDING");
    }
}
