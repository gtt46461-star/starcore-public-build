package com.ethk3.novaonepro.strategyv2;
import com.ethk3.novaonepro.DbStore;
public final class HoldFlushRetryTest {
 static void due()throws Exception{java.lang.reflect.Field f=HoldLedgerFlushR30.class.getDeclaredField("lastFlush");f.setAccessible(true);f.setLong(null,0L);}
 public static void main(String[] args)throws Exception{
  WriterHoldLedgerR30.rotate();DbStore store=new DbStore();for(int i=0;i<12;i++)WriterHoldLedgerR30.record("old",false,1,1,false);
  HoldLedgerFlushR30.maybeFlush(store);if(!store.coordinator.db.rows.isEmpty())throw new AssertionError("failure wrote rows");
  for(int i=0;i<3;i++)WriterHoldLedgerR30.record("new",false,1,1,false);
  due();HoldLedgerFlushR30.maybeFlush(store);long first=0;for(long n:store.coordinator.db.rows.values())first+=n;if(first!=12)throw new AssertionError("pending lost "+first);
  due();HoldLedgerFlushR30.maybeFlush(store);long all=0;for(long n:store.coordinator.db.rows.values())all+=n;if(all!=15)throw new AssertionError("new window lost "+all);
  System.out.println("HOLD_FLUSH_RETRY_HOST_PASS old=12 new=3 total=15 retry_pending=true scope=real_flush_logic_fake_DB_not_Android_SQLite");
 }
}
