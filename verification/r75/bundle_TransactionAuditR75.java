package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Actual coordinator/audit plus real SQLite lock contention; no device latency claim. */
public final class bundle_TransactionAuditR75 {
    static int checks;
    static void ok(boolean value,String name){if(!value)throw new AssertionError(name);checks++;System.out.println("PASS "+name);}
    static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
    static Map<String,String> detail(RuntimeResourceAuditR927.Scope scope,String operation){
        for(String row:scope.transactionDetailsR75.toString().split(";"))if(row.startsWith("DBTX75|operation="+operation+"|")){
            Map<String,String> values=new LinkedHashMap<String,String>();
            for(String field:row.split("\\|")){int eq=field.indexOf('=');if(eq>=0)values.put(field.substring(0,eq),field.substring(eq+1));}
            return values;
        }
        throw new AssertionError("missing transaction detail "+operation);
    }
    static long duration(Map<String,String> row,String phase){return Long.parseLong(row.get(phase+"_ns"));}
    static void mergeBoundaries(SQLiteDatabase db,RuntimeResourceAuditR927.Scope scope)throws Exception{
        String saved=scope.stages.toString(),span="CP12EX1|R75_BOUNDARY|1000000|500000|1|SYNC;";
        com.ethk3.novaonepro.r952.MatrixProjectionCP12.Spec spec=new com.ethk3.novaonepro.r952.MatrixProjectionCP12.Spec(
                com.ethk3.novaonepro.r952.MatrixProjectionCP12.Type.LatencyMatrix,
                "stage:R75_BOUNDARY|unit:WALL_MICROS|device:ANDROID|release:"+BuildConfig.VERSION_CODE,20,V51ChecksumEngine.sha256("R75_BOUNDARY_SPEC"));
        java.lang.reflect.Method prepare=com.ethk3.novaonepro.r952.MatrixSourceRouterCP12.class.getDeclaredMethod("prepareLatencyCP12",SQLiteDatabase.class,com.ethk3.novaonepro.r952.MatrixProjectionCP12.Spec.class);prepare.setAccessible(true);
        db.execSQL("INSERT INTO "+RuntimeResourceAuditR927.TABLE+"(execution_id,attempt_id,target_issue,process_id,release_code,started_at,finished_at,status,budget_hash) VALUES('r75-boundary','r75-boundary','202610130903','fixture',?,1,2,'COMPLETED',?)",new Object[]{BuildConfig.VERSION_CODE,V51ChecksumEngine.sha256("R75_BOUNDARY_BUDGET")});
        String[] originals={"OLD;".repeat(509)+span,"OLD;".repeat(510)+span,"OLD;".repeat(511)+span,
                "x".repeat(65535-span.length()-1)+";"+span,"x".repeat(65536-span.length()-1)+";"+span,"x".repeat(65537-span.length()-1)+";"+span};
        try{
            for(String original:originals){
                scope.stages.setLength(0);scope.stages.append(original);
                String merged=RuntimeResourceAuditR927.stageSamplesWithTransactionsR75(scope);
                int entries=original.split(";",-1).length;
                if(original.length()>65536||entries>512){ok(original.equals(merged),"already excessive original remains byte-identical chars="+original.length()+" entries="+entries);continue;}
                ok(merged.startsWith(original)&&merged.length()<=65536&&merged.split(";",-1).length<=512,"merged observation respects existing matrix bounds chars="+original.length()+" entries="+entries);
                db.execSQL("UPDATE "+RuntimeResourceAuditR927.TABLE+" SET stage_samples=? WHERE execution_id='r75-boundary'",new Object[]{original});
                com.ethk3.novaonepro.r952.MatrixSourceRouterCP12.PreparedInput before=(com.ethk3.novaonepro.r952.MatrixSourceRouterCP12.PreparedInput)prepare.invoke(null,db,spec);
                db.execSQL("UPDATE "+RuntimeResourceAuditR927.TABLE+" SET stage_samples=? WHERE execution_id='r75-boundary'",new Object[]{merged});
                com.ethk3.novaonepro.r952.MatrixSourceRouterCP12.PreparedInput after=(com.ethk3.novaonepro.r952.MatrixSourceRouterCP12.PreparedInput)prepare.invoke(null,db,spec);
                ok(before!=null&&after!=null&&before.ordinal==after.ordinal&&before.receipt.equals(after.receipt)&&before.proof.equals(after.proof),"actual latency matrix retains identical accepted ordinal receipt and proof");
            }
        }finally{scope.stages.setLength(0);scope.stages.append(saved);db.execSQL("DELETE FROM "+RuntimeResourceAuditR927.TABLE+" WHERE execution_id='r75-boundary'");}
    }
    static Process blocker(String file,boolean writer)throws Exception{return blocker(file,writer,".30");}
    static Process blocker(String file,boolean writer,String seconds)throws Exception{
        String code="import sqlite3,sys,time\nc=sqlite3.connect(sys.argv[1]);c.execute('BEGIN IMMEDIATE' if sys.argv[2]=='writer' else 'BEGIN');c.execute('SELECT count(*) FROM r75_timing_value').fetchone();print('READY',flush=True);time.sleep(float(sys.argv[3]));c.rollback();c.close()";
        Process process=new ProcessBuilder("python","-c",code,file,writer?"writer":"reader",seconds).start();
        if(!"READY".equals(new BufferedReader(new InputStreamReader(process.getInputStream())).readLine()))throw new AssertionError("SQLite blocker startup failed");
        return process;
    }
    public static void main(String[] args)throws Exception{
        try(SQLiteDatabase db=new SQLiteDatabase(args[0],args[1])){
            // A rollback journal permits BEGIN IMMEDIATE followed by COMMIT reader contention.
            try(Cursor c=db.rawQuery("PRAGMA journal_mode=DELETE",null)){c.moveToFirst();}
            db.execSQL("PRAGMA foreign_keys=ON");
            db.execSQL("CREATE TABLE r75_timing_value(id INTEGER PRIMARY KEY)");
            db.execSQL("CREATE TABLE r75_timing_parent(id INTEGER PRIMARY KEY)");
            db.execSQL("CREATE TABLE r75_timing_child(id INTEGER REFERENCES r75_timing_parent(id) DEFERRABLE INITIALLY DEFERRED)");
            DbStore store=HostProductR974.store(db);RealtimePressureGateR811.resetForTests();
            long now=System.currentTimeMillis();
            AnalysisSingleFlightR36.Request request=new AnalysisSingleFlightR36.Request("202610130901","202610130902",9002,now,now+60000,"R75_TIMING",V51ChecksumEngine.sha256("R75_TIMING"),"");
            RuntimeResourceAuditR927.Scope scope=RuntimeResourceAuditR927.begin(store,AnalysisSingleFlightR36.Lease.acquire(request));

            Process writer=blocker(args[1],true);
            ok(store.coordinatorR36().runAtomicWrite("R75_WAIT_BEGIN",d->d.execSQL("INSERT INTO r75_timing_value VALUES(1)")),"transaction commits after genuine independent SQLite writer wait");
            writer.waitFor();Map<String,String> begin=detail(scope,"R75_WAIT_BEGIN");
            ok(duration(begin,"begin")>150000000L&&duration(begin,"begin")>duration(begin,"body")*3,"BEGIN lock wait is distinct from callback duration");
            ok("COMMITTED".equals(begin.get("outcome")),"BEGIN wait retains committed outcome");

            try(Cursor c=db.rawQuery("PRAGMA busy_timeout=1",null)){c.moveToFirst();}
            Process busy=blocker(args[1],true,"1.0");final boolean[] callback={false};
            ok(!store.coordinatorR36().runAtomicWrite("R75_BEGIN_FAILURE",d->{callback[0]=true;d.execSQL("INSERT INTO r75_timing_value VALUES(6)");}),"SQLite BEGIN lock timeout returns original fallback");
            busy.waitFor();Map<String,String> failedBegin=detail(scope,"R75_BEGIN_FAILURE");
            ok(!callback[0]&&"BEGIN_FAILED".equals(failedBegin.get("outcome"))&&duration(failedBegin,"body")==-1&&duration(failedBegin,"end")==-1,"failed BEGIN records unexecuted callback and END as unavailable");
            try(Cursor c=db.rawQuery("PRAGMA busy_timeout=5000",null)){c.moveToFirst();}

            ok(store.coordinatorR36().runAtomicWrite("R75_SLOW_BODY",d->{Thread.sleep(220L);d.execSQL("INSERT INTO r75_timing_value VALUES(2)");}),"slow callback still commits");
            Map<String,String> body=detail(scope,"R75_SLOW_BODY");
            ok(duration(body,"body")>180000000L&&duration(body,"body")>duration(body,"begin")*3&&duration(body,"body")>duration(body,"end")*3,"slow callback is distinct from SQLite BEGIN and END");

            Process reader=blocker(args[1],false);
            ok(store.coordinatorR36().runAtomicWrite("R75_WAIT_COMMIT",d->d.execSQL("INSERT INTO r75_timing_value VALUES(3)")),"transaction commits after genuine independent SQLite reader wait");
            reader.waitFor();Map<String,String> end=detail(scope,"R75_WAIT_COMMIT");
            ok(duration(end,"end")>150000000L&&duration(end,"end")>duration(end,"body")*3,"COMMIT wait is distinct from callback duration");

            ok(!store.coordinatorR36().runAtomicWrite("R75_BODY_ROLLBACK",d->{d.execSQL("INSERT INTO r75_timing_value VALUES(4)");throw new IllegalStateException("EXPECTED_BODY_FAILURE");}),"failing callback returns original fallback");
            ok("ROLLED_BACK".equals(detail(scope,"R75_BODY_ROLLBACK").get("outcome"))&&n(db,"SELECT count(*) FROM r75_timing_value WHERE id=4")==0,"failing callback rolls back and records rollback");

            ok(!store.coordinatorR36().runAtomicWrite("R75_END_FAILURE",d->d.execSQL("INSERT INTO r75_timing_child VALUES(1)")),"SQLite deferred foreign-key COMMIT failure returns original fallback");
            ok("END_FAILED".equals(detail(scope,"R75_END_FAILURE").get("outcome")),"actual COMMIT failure is distinguishable from body failure");
            // Host adapter clears inTransaction even on COMMIT failure; release actual SQLite tx.
            db.execSQL("ROLLBACK");
            ok(n(db,"SELECT count(*) FROM r75_timing_child")==0,"failed COMMIT creates no durable child");

            db.beginTransaction();try{
                ok(store.coordinatorR36().runAtomicWrite("R75_EXTERNAL",d->d.execSQL("INSERT INTO r75_timing_value VALUES(5)")),"nested caller-owned transaction executes callback");
                Map<String,String> nested=detail(scope,"R75_EXTERNAL");
                ok("0".equals(nested.get("owns"))&&duration(nested,"begin")==-1&&duration(nested,"end")==-1&&"EXTERNAL_SUCCESS".equals(nested.get("outcome")),"caller-owned transaction marks unexecuted BEGIN/END as unavailable");
            }finally{db.endTransaction();}
            ok(n(db,"SELECT count(*) FROM r75_timing_value WHERE id=5")==0,"observation does not commit caller-owned transaction");
            java.lang.reflect.Field stageField=RuntimeResourceAuditR927.Scope.class.getDeclaredField("transactionDetailsR75");stageField.setAccessible(true);
            StringBuilder savedStages=scope.transactionDetailsR75;stageField.set(scope,null);
            try{
                ok(store.coordinatorR36().runAtomicWrite("R75_AUDIT_FAILURE_COMMIT",d->d.execSQL("INSERT INTO r75_timing_value VALUES(7)")),"injected observation failure preserves committed success");
                final IllegalStateException original=new IllegalStateException("R75_ORIGINAL_BODY_FAILURE");boolean preserved=false;
                try{store.coordinatorR36().runAtomicWriteOrThrowF01("R75_AUDIT_FAILURE_BODY",d->{d.execSQL("INSERT INTO r75_timing_value VALUES(8)");throw original;});}
                catch(IllegalStateException failure){preserved=failure.getCause()==original;}
                ok(preserved,"injected observation failure preserves original callback exception");
            }finally{stageField.set(scope,savedStages);}
            ok(n(db,"SELECT count(*) FROM r75_timing_value WHERE id=7")==1&&n(db,"SELECT count(*) FROM r75_timing_value WHERE id=8")==0,"observation failure preserves durable commit and rollback semantics");

            RuntimeResourceAuditR927.measuredQueueSpanCP12("R75_EXISTING",10,20);
            String beforeBulk=scope.stages.toString();String hugeLabel="R75_LONG|;\n"+"x".repeat(12000);
            for(int i=0;i<10000;i++)RuntimeResourceAuditR927.transactionTimingR75(hugeLabel,true,Long.MAX_VALUE,Long.MAX_VALUE,Long.MAX_VALUE,"COMMITTED");
            String bounded=scope.transactionDetailsR75.toString();int count=0,notices=0;boolean shortLabels=true;
            for(String row:bounded.split(";")){
                if(row.startsWith("DBTX75|")){count++;String label=row.substring("DBTX75|operation=".length(),row.indexOf("|owns="));if(label.length()>96)shortLabels=false;}
                if(row.startsWith("DBTX75_TRUNCATED|"))notices++;
            }
            ok(count==128&&notices==1&&shortLabels,"ten thousand observations retain at most 128 bounded labels and one truncation marker");
            ok(beforeBulk.equals(scope.stages.toString())&&bounded.length()<45000,"new observation cap preserves prior stage evidence and bounds added characters");
            for(int i=0;i<10000;i++)RuntimeResourceAuditR927.transactionTimingR75(hugeLabel,true,1,2,3,"COMMITTED");
            ok(bounded.equals(scope.transactionDetailsR75.toString()),"further observations allocate no additional stage text after cap");
            RuntimeResourceAuditR927.measuredQueueSpanCP12("R75_AFTER_LIMIT",20,30);
            ok(scope.stages.toString().contains("CP12EX1|R75_AFTER_LIMIT|"),"transaction cap does not suppress later existing CP12EX stages");
            RuntimeResourceAuditR927.dbBody(8765L,false);
            ok(scope.dbBodyMax==8765L,"legacy inclusive dbBody metric remains independent");
            mergeBoundaries(db,scope);
            String observed=RuntimeResourceAuditR927.stageSamplesWithTransactionsR75(scope);
            RuntimeResourceAuditR927.finish(store,scope,false,"R75_SYNTHETIC_SQLITE_TIMING");
            try(Cursor c=db.rawQuery("SELECT stage_samples,db_body_max_ms FROM "+RuntimeResourceAuditR927.TABLE+" WHERE execution_id=?",new String[]{scope.executionId})){
                ok(c.moveToFirst()&&observed.equals(c.getString(0))&&c.getLong(1)==8765L,"transaction detail and original aggregate persist in report row");
            }
            Files.writeString(Path.of(args[2]),"{\"status\":\"PASS\",\"checks\":"+checks+",\"scope\":\"ACTUAL_PRODUCT_REAL_SQLITE_HOST_LOCK_CONTENTION_NOT_ANDROID\",\"stages\":\""+observed+"\"}\n");
        }
    }
}
