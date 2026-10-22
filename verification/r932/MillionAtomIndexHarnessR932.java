package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;
import org.json.*;

/** Million historical index entries plus current full-width atom payloads, on real SQLite. */
public final class MillionAtomIndexHarnessR932 {
    static void check(boolean b,String name){if(!b)throw new AssertionError(name);System.out.println(name+"=PASS");}
    static String scalar(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):"";}}
    public static void main(String[] args)throws Exception{
        SQLiteDatabase db=new SQLiteDatabase(args[0]);try{
            DbStore store=new DbStore(db);AtomCaptureR932.schema(db);
            long start=System.nanoTime();
            // These old rows stress index cardinality; they explicitly carry no mask evidence.
            db.execSQL("WITH RECURSIVE seq(n) AS(SELECT 0 UNION ALL SELECT n+1 FROM seq WHERE n<999999) INSERT INTO "+AtomCaptureR932.TABLE+" SELECT strftime('%Y%m%d','2020-01-01','+'||(n/1440)||' days')||printf('%04d',n%1440+1),'SYNTHETIC_INDEX_ONLY','', 'LEGACY_UNKNOWN_MASK','LEGACY_UNKNOWN_MASK','NO_EVIDENCE',0 FROM seq");
            long build=System.nanoTime()-start;
            check(scalar(db,"SELECT COUNT(*) FROM "+AtomCaptureR932.TABLE).equals("1000000"),"MILLION_HISTORICAL_INDEX_ROWS_CREATED");
            String base="202609100001",target="202609100002";int payloadBytes=0;
            for(int row=0;row<4;row++){
                AtomCaptureR932.Values values=new AtomCaptureR932.Values(240,base);
                for(int atom=0;atom<240;atom++){values.present[atom]=GenesisSchemaRegistryR915.atoms().get(atom).productionScoreEligible();values.values[atom]=.25;values.costNs[atom]=100;values.missingReasons[atom]=values.present[atom]?"":"FIXTURE_UNAVAILABLE_NOT_ZERO";}
                String context=V51ChecksumEngine.sha256("FULL_WIDTH_FIXTURE|"+target),payload=values.payload(target,context);payloadBytes+=payload.getBytes("UTF-8").length;
                db.execSQL("INSERT INTO "+AtomCaptureR932.TABLE+" VALUES(?,?,?,?,?,?,1)",new Object[]{target,context,base,GenesisSchemaRegistryR915.fingerprint(),payload,V51ChecksumEngine.sha256(payload)});
                base=target;target=TargetIssueResolver.next(target).targetIssue;
            }
            String query="SELECT target_issue,payload,payload_hash FROM "+AtomCaptureR932.TABLE+" WHERE target_issue>? ORDER BY target_issue LIMIT 4",cursor="202609100001";StringBuilder plan=new StringBuilder();
            try(Cursor c=db.rawQuery("EXPLAIN QUERY PLAN "+query,new String[]{cursor})){while(c.moveToNext())plan.append(c.getString(3)).append('\n');}
            check(plan.toString().contains("SEARCH")&&plan.toString().contains("INDEX")&&!plan.toString().contains("TEMP B-TREE"),"ACTUAL_INCREMENTAL_QUERY_USES_RANGE_INDEX_WITHOUT_TEMP_SORT");
            long[] timings=new long[20];int total=0;
            for(int run=0;run<timings.length;run++){start=System.nanoTime();int n=0;try(Cursor c=db.rawQuery(query,new String[]{cursor})){while(c.moveToNext()){new JSONObject(c.getString(1)).getJSONArray("atoms");n++;}}timings[run]=System.nanoTime()-start;if(n!=4)throw new AssertionError("PAGE_SIZE");total+=n;}
            Arrays.sort(timings);check(timings[18]<250000000L&&total==80,"MILLION_INDEX_FULL_WIDTH_PAGE_P95_WITHIN_250MS_ON_HOST");
            store.setState("r932_atom_quality_cursor",cursor);start=System.nanoTime();AtomCaptureR932.maintain(store);long update=System.nanoTime()-start;
            check(store.getState("r932_atom_quality_cursor","").equals("202609100005")&&scalar(db,"SELECT COUNT(*) FROM "+AtomCaptureR932.QUALITY+" WHERE n=4").equals("240"),"ACTUAL_MAINTENANCE_PROCESSES_FOUR_ROWS_WITHOUT_SCANNING_MILLION_OLD_ROWS");
            check(update<250000000L,"FULL_INCREMENTAL_QUALITY_TRANSACTION_WITHIN_250MS_ON_HOST");
            JSONObject report=new JSONObject();report.put("index_rows",1000004);report.put("historical_payload_scope","SYNTHETIC_INDEX_ONLY_NO_MASK_EVIDENCE");report.put("recent_payload_bytes",payloadBytes);report.put("page_size",4);report.put("query_plan",plan.toString());report.put("page_p95_ms",timings[18]/1e6);report.put("quality_transaction_ms",update/1e6);report.put("fixture_build_ms",build/1e6);report.put("scope","HOST_REAL_SQLITE_NOT_DEVICE_LATENCY");System.out.println(report.toString());
            System.out.println("R932_MILLION_ATOM_INDEX_PASS|scope=REAL_SQLITE_INDEX_CARDINALITY_FULL_CURRENT_PAYLOADS");
        }finally{db.shutdown();}
    }
}
