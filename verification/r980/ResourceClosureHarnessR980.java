package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.util.*;
import org.json.*;

/** Real product owners and SQLite. Input report tables retain their original values.
 * Startup/Android are adapted; no assertion represents a phone/UI/network test. */
public final class ResourceClosureHarnessR980 {
    static int checks, failures;
    static void check(boolean ok,String text) {
        checks++; if(!ok)failures++;
        System.out.println((ok?"PASS ":"FAIL ")+text);
    }
    static long scalar(SQLiteDatabase db,String sql) {
        try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}
    }
    static void importTable(SQLiteDatabase db,Path file)throws Exception {
        JSONObject source=new JSONObject(Files.readString(file));
        String table=source.getString("table");
        if(!table.matches("[A-Za-z0-9_]+"))throw new IllegalArgumentException("TABLE");
        JSONArray columns=source.getJSONArray("columns"),rows=source.getJSONArray("rows");
        StringJoiner names=new StringJoiner(","),marks=new StringJoiner(",");
        for(int i=0;i<columns.length();i++){names.add(columns.getString(i));marks.add("?");}
        for(int i=0;i<rows.length();i++) {
            JSONObject row=rows.getJSONObject(i);Object[] args=new Object[columns.length()];
            for(int k=0;k<args.length;k++){String col=columns.getString(k);args[k]=row.isNull(col)?null:row.getString(col);}
            db.execSQL("INSERT INTO "+table+"("+names+") VALUES("+marks+")",args);
        }
        System.out.println("IMPORTED "+table+" rows="+rows.length()+" source_raw_sha256="+source.getString("raw_sha256"));
    }
    static String replacement(SQLiteDatabase db,String parent)throws Exception {
        try {Method m=ResearchPopulationR966.class.getDeclaredMethod("replacementForDevelopment",SQLiteDatabase.class,String.class);m.setAccessible(true);return (String)m.invoke(null,db,parent);}
        catch(NoSuchMethodException beforeFix){return ResearchPopulationR966.replacement(db);}
    }
    static void reportPopulation(String bridge,Path fixtures)throws Exception {
        SQLiteDatabase db=new SQLiteDatabase(bridge);
        try {
            DbStore store=DevelopmentHarnessR978.setup(db);
            db.beginTransaction();
            try {
                importTable(db,fixtures.resolve("v57_algorithm_genome_r908.json"));
                importTable(db,fixtures.resolve("research_lifecycle_r932.json"));
                String parent="";
                try(Cursor c=db.rawQuery("SELECT genome_id FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" WHERE future_n=0 AND state_updates=0 ORDER BY genome_id LIMIT 1",null)){if(c.moveToFirst())parent=c.getString(0);}
                check(ResearchPopulationR966.active(db)==64,"REPORT_REAL_ACTIVE_POPULATION_64");
                check(ResearchPopulationR966.replacement(db)==null,"REPORT_EXISTING_REPLACEMENT_HAS_NO_ELIGIBLE_VICTIM");
                check(!parent.isEmpty()&&parent.equals(replacement(db,parent)),"TRAINED_UNOBSERVED_PARENT_CAN_HAND_OFF_OWN_RESOURCE_SLOT");
                // A candidate with even one genuine future observation must never enter this route.
                String observed="";try(Cursor c=db.rawQuery("SELECT genome_id FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" WHERE future_n>0 LIMIT 1",null)){if(c.moveToFirst())observed=c.getString(0);}
                check(replacement(db,observed)==null,"OBSERVED_PARENT_NOT_RETIRED_BY_BOOTSTRAP_ESCAPE");
                check(scalar(db,"SELECT COUNT(*) FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE)==0,"RESOURCE_QUERY_MINTS_NO_CERTIFICATE");
                db.execSQL("INSERT INTO "+CandidateCalibrationR932.OBS+" VALUES('R980_SYNTHETIC_PENDING_GUARD','202609220900',?,'structure','bundle','selector',0.3,1,1,2,3,'frozen-proof',1,-1)",new Object[]{"algorithm:"+parent});
                check(replacement(db,parent)==null,"EVEN_UNSETTLED_FROZEN_CALIBRATION_PROTECTS_PARENT");
                db.execSQL("DELETE FROM "+CandidateCalibrationR932.OBS+" WHERE study_hash='R980_SYNTHETIC_PENDING_GUARD'");
                check(parent.equals(replacement(db,parent)),"OWN_HANDOFF_AVAILABLE_WHEN_NO_FROZEN_EVIDENCE");
                db.setTransactionSuccessful();
            }finally{db.endTransaction();}
        }finally{db.shutdown();}
    }
    static void saturatedControlQueue(String bridge)throws Exception {
        SQLiteDatabase db=new SQLiteDatabase(bridge);
        try {
            DbStore store=HostProductR974.store(db);HostProductR974.schema(db,store);
            db.beginTransaction();try {
                for(int i=0;i<4096;i++) {
                    String id="legacy-backlog-"+i;
                    db.execSQL("INSERT INTO "+ResearchTaskQueueR967.TABLE+"(task_id,kind,candidate_id,target_issue,stream_seq,input_hash,authority_ref,status,created_at,updated_at) VALUES(?,'POST_TRAIN',?,'202609220001','1','fixture-input','fixture-source','PENDING',1,1)",new Object[]{id,id});
                }
                boolean blocked=false;
                try {ResearchTaskQueueR967.enqueue(db,ResearchTaskQueueR967.POST_TRAIN,"overflow","202609220002","1","x","x");}
                catch(ResearchTaskQueueR967.Capacity expected){blocked=true;}
                check(blocked,"PAYLOAD_BACKPRESSURE_STILL_ENFORCED");
                int admitted=0;
                try {for(int i=0;i<22;i++){ResearchTaskQueueR967.enqueue(db,ResearchTaskQueueR967.MAINTENANCE,"lane:"+i,"202609220002","1","fixture","fixture");admitted++;}}
                catch(ResearchTaskQueueR967.Capacity baselineDeadlock){System.out.println("REPRO "+baselineDeadlock.getMessage());}
                check(admitted==22,"FULL_LEGACY_DATA_QUEUE_CANNOT_BLOCK_ITS_DRAIN_OWNERS");
                check(scalar(db,"SELECT COUNT(*) FROM "+ResearchTaskQueueR967.TABLE+" WHERE kind='POST_TRAIN' AND status='PENDING'")==4096,"BACKLOG_PRESERVED_NO_FAKE_ACK_OR_DELETION");
                boolean invalid=false;
                try{ResearchTaskQueueR967.enqueue(db,ResearchTaskQueueR967.MAINTENANCE,"lane:22","202609220002","1","x","x");}
                catch(IllegalArgumentException expected){invalid=true;}
                catch(ResearchTaskQueueR967.Capacity oldCapacity){/* old code has no identity validation */}
                check(invalid,"CONTROL_RESERVATION_CANNOT_CREATE_A_23RD_OWNER");
                db.setTransactionSuccessful();
            }finally{db.endTransaction();}
        }finally{db.shutdown();}
    }
    public static void main(String[] args)throws Exception {
        android.os.SystemClock.setForTest(1000000,0);
        reportPopulation(args[0],Path.of(args[1]));saturatedControlQueue(args[0]);
        System.out.println("R980_RESOURCE_CLOSURE checks="+checks+" failures="+failures+" device=NOT_RUN");
        if(failures>0)throw new AssertionError("FAILURES="+failures);
    }
}
