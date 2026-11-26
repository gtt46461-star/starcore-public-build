package com.ethk3.novaonepro.r952;

import com.ethk3.novaonepro.strategyv2.FailureDispositionR31;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/** Scheduling only. No message substring can turn an input/quality failure into a retry. */
public final class TrainingInfrastructurePolicyR343 {
    private TrainingInfrastructurePolicyR343() {}

    /** Empty means this policy has not established an infrastructure retry.
     * Complete cause-chain inspection is required before an outer dependency can win.
     * UNKNOWN/COMMITTED outcomes keep the task fenced for coordinator reconciliation;
     * they never authorize replaying a numerical commit in this method. */
    public static String reason(Throwable failure) {
        Set<Throwable> seen=Collections.newSetFromMap(new IdentityHashMap<Throwable,Boolean>());
        List<FailureDispositionR31.ClassifiedFailure> typed=new ArrayList<>();
        for(Throwable cause=failure;cause!=null;) {
            if(seen.size()>=32||!seen.add(cause))return "";
            String name=cause.getClass().getName();
            if(cause instanceof Error||name.equals("android.database.sqlite.SQLiteDatabaseCorruptException")
                    ||name.equals("android.database.sqlite.SQLiteConstraintException"))return "";
            if(cause instanceof FailureDispositionR31.ClassifiedFailure) {
                FailureDispositionR31.ClassifiedFailure entry=(FailureDispositionR31.ClassifiedFailure)cause;
                String category;
                try{category=entry.failureCategory();}catch(RuntimeException unreadable){return "";}
                if(category==null||category.equals("DB_CORRUPT")||category.equals("INTEGRITY")
                        ||category.equals("BUG"))return "";
                typed.add(entry);
            }
            try{cause=cause.getCause();}catch(RuntimeException unreadable){return "";}
        }
        if(typed.isEmpty())return "";
        FailureDispositionR31.ClassifiedFailure entry=typed.get(0);
        final String outcome,category;
        try{outcome=entry.commitOutcomeName();category=entry.failureCategory();}
        catch(RuntimeException unreadable){return "";}
        if("UNKNOWN".equals(outcome))return "INFRA_COMMIT_RESOLUTION_REQUIRED";
        if("COMMITTED".equals(outcome)||"COMMITTED_DIAGNOSTIC_FAILED".equals(outcome))
            return "INFRA_COMMITTED_ACK_RECOVERY";
        if(!"ROLLED_BACK".equals(outcome)&&!"NOT_SUBMITTED".equals(outcome))return "";
        switch(category) {
            case "DEPENDENCY":return "INFRA_DEPENDENCY";
            case "LOCK_BUSY":return "INFRA_DB_BUSY";
            case "TEMP_TIMEOUT":return "INFRA_TIMEOUT";
            case "CANCELLED":return "INFRA_CANCELLED";
            case "STALE_OWNER":return "INFRA_STALE_OWNER";
            case "RESOURCE_MEMORY":return "INFRA_MEMORY";
            case "RESOURCE_QUEUE":return "INFRA_QUEUE";
            case "MISSING_SOURCE":return "INFRA_MISSING_SOURCE";
            case "DISK_FULL":return "INFRA_STORAGE_FULL";
            default:return "";
        }
    }
}
