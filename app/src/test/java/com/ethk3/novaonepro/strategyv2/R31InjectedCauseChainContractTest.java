package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import com.ethk3.novaonepro.DatabaseCoordinator;
import java.util.*;
import org.junit.Test;

/** R31-B00-C02#A2: one injection each of nested busy, owner change, SQL syntax error, disk full and user cancel.
 *  Cause chains/categories stay distinguishable; failures are neither all retried nor all quarantined. */
public final class R31InjectedCauseChainContractTest {
    // Production SQLite types (android.database.sqlite) are injected, nested under ordinary wrappers.
    private static Throwable nestedBusy(){return new IllegalStateException("writer body",new RuntimeException("prepared slice",new android.database.sqlite.SQLiteDatabaseLockedException("database is locked (code 5 SQLITE_BUSY)")));}
    private static Throwable ownerChange(){return new IllegalStateException("R30_PREPARED_OWNER_CHANGED:task=r27-evolution");}
    private static Throwable syntax(){return new android.database.sqlite.SQLiteException("near \"SELEC\": syntax error (code 1 SQLITE_ERROR)");}
    private static Throwable diskFull(){return new IllegalStateException("commit",new android.database.sqlite.SQLiteFullException("database or disk is full (code 13 SQLITE_FULL)"));}
    private static Throwable userCancel(){return new IllegalStateException("cancel requested",new InterruptedException("user stop"));}

    @Test public void fiveInjectedCausesHaveDistinctCategoriesAndDispositions(){
        Map<String,Throwable> injected=new LinkedHashMap<>();
        injected.put("nestedBusy",nestedBusy());injected.put("ownerChange",ownerChange());injected.put("syntax",syntax());
        injected.put("diskFull",diskFull());injected.put("userCancel",userCancel());
        Map<String,String> category=new LinkedHashMap<>();Map<String,FailureDispositionR31.Decision> decision=new LinkedHashMap<>();
        for(Map.Entry<String,Throwable> e:injected.entrySet()){category.put(e.getKey(),DatabaseCoordinator.failureCategoryR31(e.getValue()));decision.put(e.getKey(),FailureDispositionR31.classify(e.getValue()));}
        assertEquals("LOCK_BUSY",category.get("nestedBusy"));assertEquals("STALE_OWNER",category.get("ownerChange"));
        assertEquals("BUG",category.get("syntax"));assertEquals("DISK_FULL",category.get("diskFull"));assertEquals("CANCELLED",category.get("userCancel"));
        assertEquals("five injections keep five distinguishable categories",5,new HashSet<>(category.values()).size());
        assertEquals(FailureDispositionR31.Disposition.RETRY_LATER,decision.get("nestedBusy").disposition);
        assertEquals(FailureDispositionR31.Disposition.STALE_OWNER,decision.get("ownerChange").disposition);
        assertEquals(FailureDispositionR31.Disposition.FAULT,decision.get("syntax").disposition);
        assertEquals(FailureDispositionR31.Disposition.FATAL,decision.get("diskFull").disposition);
        assertEquals(FailureDispositionR31.Disposition.CANCELLED,decision.get("userCancel").disposition);
        Set<FailureDispositionR31.Disposition> dispositions=new HashSet<>();int faultBudget=0;
        for(FailureDispositionR31.Decision d:decision.values()){dispositions.add(d.disposition);if(d.consumesFaultBudget())faultBudget++;}
        assertEquals("not all retried and not all quarantined",5,dispositions.size());
        assertEquals("only the real program fault consumes the task fault/quarantine budget",1,faultBudget);
        assertTrue(decision.get("syntax").consumesFaultBudget());
    }
}
