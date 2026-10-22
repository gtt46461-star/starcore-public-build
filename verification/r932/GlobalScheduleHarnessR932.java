package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;

public final class GlobalScheduleHarnessR932 {
    static String scalar(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):"";}}
    static void check(boolean pass,String name){if(!pass)throw new AssertionError(name);System.out.println(name+"=PASS");}
    static void freeze(SQLiteDatabase db,String target,String input,List<GlobalResearchScheduleR932.Candidate> rows){
        db.beginTransaction();try{GlobalResearchScheduleR932.freeze(db,target,input,rows);db.setTransactionSuccessful();}finally{db.endTransaction();}
    }
    public static void main(String[] args)throws Exception{
        SQLiteDatabase db=new SQLiteDatabase(args[0]);DbStore store=new DbStore(db);
        try{
            db.execSQL("CREATE TABLE "+DbHelper.TABLE_DRAWS+"(issue TEXT PRIMARY KEY)");GlobalResearchScheduleR932.schema(db);
            String target="202609100001",hash=V51ChecksumEngine.sha256("SYNTHETIC_SNAPSHOT");
            List<GlobalResearchScheduleR932.Candidate> rows=new ArrayList<GlobalResearchScheduleR932.Candidate>();
            for(String tier:Arrays.asList("A","B","C"))for(int family=0;family<8;family++)for(int i=0;i<20;i++){
                String key=tier+":"+family+":"+i;
                rows.add(new GlobalResearchScheduleR932.Candidate(key,"F"+family,V51ChecksumEngine.sha256(key),tier,""));
            }
            freeze(db,target,hash,rows);
            check(scalar(db,"SELECT COUNT(*) FROM "+GlobalResearchScheduleR932.TASK+" WHERE tier='A' AND selected=1").equals("160")&&scalar(db,"SELECT COUNT(*) FROM "+GlobalResearchScheduleR932.TASK+" WHERE tier='B' AND selected=1").equals("160")&&scalar(db,"SELECT COUNT(*) FROM "+GlobalResearchScheduleR932.TASK+" WHERE tier='C' AND selected=1").equals("160"),"ALL_480_EXECUTABLE_CANDIDATES_SCHEDULED");
            check(scalar(db,"SELECT COUNT(DISTINCT family) FROM "+GlobalResearchScheduleR932.TASK+" WHERE tier='A' AND selected=1").equals("8"),"EIGHT_FAMILIES_RECEIVE_ONE_ELITE_SLOT_EACH");
            String roster=scalar(db,"SELECT roster_hash FROM "+GlobalResearchScheduleR932.PLAN);
            Collections.reverse(rows);freeze(db,target,hash,rows);
            check(scalar(db,"SELECT COUNT(*) FROM "+GlobalResearchScheduleR932.TASK).equals("480")&&scalar(db,"SELECT roster_hash FROM "+GlobalResearchScheduleR932.PLAN).equals(roster),"RETRY_RESTART_CANNOT_REDRAW_ROSTER_OR_DENOMINATORS");
            check(scalar(db,"SELECT COUNT(*) FROM "+ResearchCaptureLedgerR932.TABLE+" WHERE eligible=1 AND selected=0").equals("0"),"NO_ELIGIBLE_CANDIDATE_DROPPED_BY_CAP");
            boolean reject=false;try{freeze(db,target,V51ChecksumEngine.sha256("DRIFT"),rows);}catch(Exception expected){reject=true;}check(reject,"SAME_ISSUE_CHANGED_SURFACE_REJECTED");
            GlobalResearchScheduleR932.Candidate original=rows.get(0);
            List<GlobalResearchScheduleR932.Candidate> changed=Collections.singletonList(new GlobalResearchScheduleR932.Candidate(original.key,"REBRANDED",original.structure,original.tier,""));
            reject=false;try{freeze(db,"202609100002",hash,changed);}catch(Exception expected){reject=true;}check(reject&&scalar(db,"SELECT COUNT(*) FROM "+GlobalResearchScheduleR932.PLAN).equals("1"),"FAMILY_CHANGE_CANNOT_RESET_IDENTITY_AND_ATOMIC_ROLLBACK");
            String proxy=scalar(db,"SELECT candidate_key FROM "+GlobalResearchScheduleR932.TASK+" WHERE tier='C' AND selected=1 LIMIT 1");
            check(!GlobalResearchScheduleR932.proxy(store,target,proxy)&&GlobalResearchScheduleR932.saveProxy(store,target,proxy,hash,20,.42),"FULL_EVALUATION_DEFAULT_LEGACY_PROXY_EXPLICIT");
            GlobalResearchScheduleR932.saveProxy(store,target,proxy,hash,20,.42);
            check(scalar(db,"SELECT COUNT(*) FROM "+GlobalResearchScheduleR932.PROXY).equals("1")&&scalar(db,"SELECT full1200 FROM "+ResearchCaptureLedgerR932.TABLE+" WHERE candidate_key='"+proxy+"'").equals("0"),"PROXY_RETRY_IS_ONCE_AND_CANNOT_CLAIM_FULL1200");
            db.execSQL("CREATE TRIGGER fixture_queue_failure BEFORE INSERT ON "+GlobalResearchScheduleR932.PLAN+" BEGIN SELECT RAISE(ABORT,'INJECTED'); END");
            reject=false;try{freeze(db,"202609100003",hash,rows);}catch(Exception expected){reject=true;}
            check(reject&&scalar(db,"SELECT COUNT(*) FROM "+GlobalResearchScheduleR932.TASK+" WHERE target_issue='202609100003'").equals("0"),"TASKS_IDENTITY_AND_ROSTER_COMMIT_TOGETHER");
            db.execSQL("DROP TRIGGER fixture_queue_failure");
            db.execSQL("INSERT INTO "+DbHelper.TABLE_DRAWS+" VALUES('202609100004')");
            reject=false;try{freeze(db,"202609100004",hash,rows);}catch(Exception expected){reject=true;}check(reject,"POSTDRAW_ROSTER_CREATION_REJECTED");
            System.out.println("R932_GLOBAL_SCHEDULE_PASS|scope=REAL_SQLITE_SYNTHETIC_ROSTER_NO_DEVICE_LATENCY_CLAIM");
        }finally{db.shutdown();}
    }
}
