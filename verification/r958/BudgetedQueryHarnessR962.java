package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.CancellationSignal;
import android.os.OperationCanceledException;
import java.lang.reflect.Proxy;

/** Injects lazy cursor latency into the production wrapper; not Android performance proof. */
public final class BudgetedQueryHarnessR962 {
    static int checks;
    static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);checks++;System.out.println("PASS "+name);}
    static final class SlowDatabase extends SQLiteDatabase {
        volatile boolean closed;
        SlowDatabase(String bridge)throws Exception{super(bridge);}
        @Override public synchronized Cursor rawQuery(final String sql,String[] args,final CancellationSignal signal){
            lastSignal=signal;closed=false;
            return (Cursor)Proxy.newProxyInstance(Cursor.class.getClassLoader(),new Class[]{Cursor.class},(proxy,method,a)->{
                String name=method.getName();
                if(name.equals("close")){closed=true;return null;}
                if(name.equals("moveToFirst")||name.equals("getCount")){
                    if(sql.equals("EXTERNAL_CANCEL"))throw new OperationCanceledException();
                    long until=System.nanoTime()+180000000L;
                    while(System.nanoTime()<until){if(signal.isCanceled())throw new OperationCanceledException();Thread.sleep(2);}
                    return name.equals("getCount")?Integer.valueOf(1):Boolean.TRUE;
                }
                throw new UnsupportedOperationException(name);
            });
        }
    }
    public static void main(String[] args)throws Exception {
        SlowDatabase db=new SlowDatabase(args[0]);
        try {
            ResearchWorkBudgetR927.Scope scope=ResearchWorkBudgetR927.enter(1500);
            try(Cursor c=BudgetedQueryR927.query(db,"SLOW_VALID",null,1000)){
                check(c.moveToFirst(),"LAZY_QUERY_OVER_100MS_WITHIN_CALLER_BUDGET_COMPLETES");
            }finally{ResearchWorkBudgetR927.leave(scope);}
            check(db.closed,"SUCCESS_CLOSES_UNDERLYING_CURSOR");
            scope=ResearchWorkBudgetR927.enter(1000);boolean yielded=false;
            try(Cursor c=BudgetedQueryR927.query(db,"SLOW_VALID",null,35)){c.getCount();}
            catch(ResearchWorkBudgetR927.Yield expected){yielded=expected.getMessage().contains("SQL_DEADLINE_EXHAUSTED");}
            finally{ResearchWorkBudgetR927.leave(scope);}
            check(yielded&&db.closed,"CALLER_DEADLINE_CANCELS_LAZY_FILL_AS_RETRYABLE_YIELD_AND_CLOSES");
            scope=ResearchWorkBudgetR927.enter(35);yielded=false;
            try(Cursor c=BudgetedQueryR927.query(db,"SLOW_VALID",null,1000)){c.moveToFirst();}
            catch(ResearchWorkBudgetR927.Yield expected){yielded=true;}
            finally{ResearchWorkBudgetR927.leave(scope);}
            check(yielded,"ENCLOSING_RESEARCH_BUDGET_STILL_PREEMPTS_LONG_QUERY");
            scope=ResearchWorkBudgetR927.enter(1000);boolean external=false;
            try(Cursor c=BudgetedQueryR927.query(db,"EXTERNAL_CANCEL",null,800)){c.moveToFirst();}
            catch(OperationCanceledException expected){external=true;}
            finally{ResearchWorkBudgetR927.leave(scope);}
            check(external,"EXTERNAL_CANCELLATION_NOT_RELABELED_AS_OUR_TIMEOUT");
            scope=ResearchWorkBudgetR927.enter(1000);
            try(Cursor c=BudgetedQueryR927.query(db,"SLOW_VALID",null,40)){}
            finally{ResearchWorkBudgetR927.leave(scope);}
            CancellationSignal closedSignal=db.lastSignal;Thread.sleep(80);
            check(!closedSignal.isCanceled(),"CLOSED_CURSOR_REMOVES_TIMEOUT");
            System.out.println("R962_QUERY_BUDGET_PASS|checks="+checks);
        }finally{db.shutdown();}
    }
}
