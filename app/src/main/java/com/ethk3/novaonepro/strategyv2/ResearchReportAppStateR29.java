package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.ReportNativeCP4;
import java.io.IOException;
import java.util.*;

/** Only the named R912/Director research cursors are merged into a private report copy. The Root app_state
 * contains user settings and realtime controls, so copying the whole research app_state would be incorrect. */
public final class ResearchReportAppStateR29 {
    private ResearchReportAppStateR29(){}
    private static final List<String> KEYS=keys();
    private static List<String> keys(){
        List<String> out=new ArrayList<>(Arrays.asList("r962_research_next_lane","r971_maintenance_turn","r980_hot_lane_cursor",
            "r912_research_continuity_status","r912_research_continuity_at","r956_continuity_lane_summary",
            "r975_director_priority","r77_director_training_turn","r81_long_research_turn","r65_director_last_slot","r65_director_last_slot_at",
            "r932_d2_replay_status","r914_research_predraw_recovery_status","r914_research_predraw_recovery_target","r914_research_predraw_recovery_at"));
        for(int lane=0;lane<22;lane++)out.add("r941_continuity_lane_"+lane);Collections.sort(out);return Collections.unmodifiableList(out);
    }
    public static boolean researchKey(String key){return Collections.binarySearch(KEYS,key)>=0;}
    static void mergeCopies(SQLiteDatabase db,Map<String,Object> evidence,ReportNativeCP4.Control control)throws IOException{
        if(!db.inTransaction())throw new IllegalStateException("R29_REPORT_APP_STATE_TRANSACTION_REQUIRED");List<String> copied=new ArrayList<>();
        try(Cursor exists=db.rawQuery("SELECT 1 FROM research.sqlite_master WHERE type='table' AND name='app_state'",null)){
            if(!exists.moveToFirst()){evidence.put("researchAppStateKeysMerged",copied);return;}
        }
        String placeholders=String.join(",",Collections.nCopies(KEYS.size(),"?"));String[] args=KEYS.toArray(new String[0]);
        if(MigrationCopyR29.scalar(db,"SELECT count(*) FROM research.app_state WHERE state_key IN("+placeholders+") AND length(CAST(state_value AS BLOB))>65536",args)>0)throw new IOException("REPORT_R29_RESEARCH_APP_STATE_BUDGET");
        db.execSQL("CREATE TABLE IF NOT EXISTS main.app_state(state_key TEXT PRIMARY KEY,state_value TEXT,updated_at INTEGER)");
        try(Cursor c=db.rawQuery("SELECT state_key,state_value,updated_at FROM research.app_state WHERE state_key IN("+placeholders+") ORDER BY state_key",args)){
            while(c.moveToNext()){
                if(!control.shouldContinue())throw new IOException("REPORT_USER_CANCEL");String key=c.getString(0);if(!researchKey(key))throw new IllegalStateException("R29_REPORT_STATE_ALLOWLIST");
                db.execSQL("INSERT OR REPLACE INTO main.app_state(state_key,state_value,updated_at) VALUES(?,?,?)",new Object[]{key,c.getString(1),c.getLong(2)});copied.add(key);
            }
        }
        evidence.put("researchAppStateKeysMerged",copied);evidence.put("researchAppStatePolicy","Explicit R912 lanes 0..21 and Director cursors only; Root user/realtime keys remain authoritative");
    }
}
