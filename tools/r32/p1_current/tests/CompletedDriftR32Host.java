package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.sql.DriverManager;
import java.util.LinkedHashMap;
public final class CompletedDriftR32Host {
    static int checks;
    static void need(boolean value,String description){checks++;if(!value)throw new AssertionError(description);}
    static String value(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){if(!c.moveToFirst())return null;return c.getString(0);}}
    public static void main(String[] args)throws Exception {
        Class.forName("org.sqlite.JDBC");
        try(SQLiteDatabase db=new SQLiteDatabase(DriverManager.getConnection("jdbc:sqlite:"+args[0]))){
            SchemaDomainCoordinatorR926.ensureInfrastructure(db);
            db.execSQL("CREATE TABLE r32_critical_rows(id INTEGER PRIMARY KEY,value TEXT)");
            db.execSQL("INSERT INTO r32_critical_rows VALUES(1,'ORIGINAL')");
            String correct="CREATE INDEX IF NOT EXISTS r32_actual_index ON r32_critical_rows(value)";
            LinkedHashMap<String,Runnable> units=new LinkedHashMap<>();
            units.put("INDEX_OWNER",()->LegacyDdlR31.exec(db,correct));
            for(int i=0;i<5;i++)if(LegacySchemaCoordinatorR926.advanceUnitsR31(db,"R32_AUDIT_",units,false))break;
            need("COMPLETED".equals(value(db,"SELECT status FROM v57_schema_migration_journal_r926 WHERE step_id='R32_AUDIT_INDEX_OWNER'")),"first valid unit completed");
            db.execSQL("DROP INDEX r32_actual_index"); // fixture-only drift, not production retirement authority
            db.execSQL("CREATE INDEX r32_actual_index ON r32_critical_rows(id)");
            RuntimeException original=null;
            try{LegacySchemaCoordinatorR926.advanceUnitsR31(db,"R32_AUDIT_",units,false);}catch(RuntimeException expected){original=expected;}
            need(original!=null&&original.getMessage().startsWith("LEGACY_DDL_DEFINITION_DRIFT:"),"original strict definition error survives");
            need("REPAIR_REQUIRED".equals(value(db,"SELECT status FROM v57_schema_migration_journal_r926 WHERE step_id='R32_AUDIT_INDEX_OWNER'")),"responsible old complete row rearmed");
            need("POSTCONDITION_DRIFT".equals(value(db,"SELECT cursor FROM v57_schema_migration_journal_r926 WHERE step_id='R32_AUDIT_INDEX_OWNER'")),"specific postcondition cursor retained");
            need(value(db,"SELECT error_code FROM v57_schema_migration_journal_r926 WHERE step_id='R32_AUDIT_INDEX_OWNER'").contains("LEGACY_DDL_DEFINITION_DRIFT"),"precise error persisted on owner row");
            need(value(db,"SELECT sql FROM sqlite_master WHERE name='r32_actual_index'").contains("(id)"),"production audit does not silently drop or replace wrong index");
            need("ORIGINAL".equals(value(db,"SELECT value FROM r32_critical_rows WHERE id=1")),"business data intact after refusal");
            boolean refused=false;
            try{LegacySchemaCoordinatorR926.advanceUnitsR31(db,"R32_AUDIT_",units,false);}catch(RuntimeException expected){refused=true;}
            need(refused,"subsequent owner retry still refuses bad definition");
            need(!"COMPLETED".equals(value(db,"SELECT status FROM v57_schema_migration_journal_r926 WHERE step_id='R32_AUDIT_INDEX_OWNER'")),"failed retry never signs complete");
            db.execSQL("DROP INDEX r32_actual_index");db.execSQL(correct); // explicit fixture repair of this exact index
            boolean complete=false;for(int i=0;i<5;i++)if(LegacySchemaCoordinatorR926.advanceUnitsR31(db,"R32_AUDIT_",units,false)){complete=true;break;}
            need(complete,"specific corrected owner resumes without journal reset");
            need("COMPLETED".equals(value(db,"SELECT status FROM v57_schema_migration_journal_r926 WHERE step_id='R32_AUDIT_INDEX_OWNER'")),"verified repair signs exactly responsible unit");
            need("ORIGINAL".equals(value(db,"SELECT value FROM r32_critical_rows WHERE id=1")),"original value preserved across recovery");
            System.out.println("R32_COMPLETED_DRIFT_HOST_PASS assertions="+checks);
        }
    }
}
