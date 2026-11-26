package com.ethk3.novaonepro.r952;

import com.ethk3.novaonepro.strategyv2.FailureDispositionR31;

public final class TrainingInfrastructurePolicyR343Test {
    static int cases;
    static class Typed extends IllegalStateException implements FailureDispositionR31.ClassifiedFailure {
        final String category,outcome;
        Typed(String category,String outcome,Throwable cause){super("untrusted diagnostic text",cause);this.category=category;this.outcome=outcome;}
        public String failureCategory(){return category;}
        public String operation(){return "CP12_TRAIN_FEATURE_PAGE";}
        public String phase(){return "ADMISSION";}
        public String commitOutcomeName(){return outcome;}
    }
    static void check(String expected,Throwable error){
        String actual=TrainingInfrastructurePolicyR343.reason(error);
        if(!expected.equals(actual))throw new AssertionError(expected+" != "+actual);
        cases++;
    }
    public static void main(String[] args){
        check("INFRA_DEPENDENCY",new Typed("DEPENDENCY","ROLLED_BACK",new IllegalStateException("R31_WRITER_HOLD_FAULT_BACKLOG_PENDING_DURABLE_DRAIN")));
        check("INFRA_DB_BUSY",new Typed("LOCK_BUSY","ROLLED_BACK",null));
        check("INFRA_TIMEOUT",new Typed("TEMP_TIMEOUT","NOT_SUBMITTED",null));
        check("INFRA_CANCELLED",new Typed("CANCELLED","ROLLED_BACK",null));
        check("INFRA_MEMORY",new Typed("RESOURCE_MEMORY","ROLLED_BACK",null));
        check("INFRA_QUEUE",new Typed("RESOURCE_QUEUE","ROLLED_BACK",null));
        check("INFRA_STALE_OWNER",new Typed("STALE_OWNER","ROLLED_BACK",null));
        check("INFRA_MISSING_SOURCE",new Typed("MISSING_SOURCE","ROLLED_BACK",null));
        check("INFRA_STORAGE_FULL",new Typed("DISK_FULL","ROLLED_BACK",null));
        check("INFRA_COMMIT_RESOLUTION_REQUIRED",new Typed("DEPENDENCY","UNKNOWN",null));
        check("INFRA_COMMITTED_ACK_RECOVERY",new Typed("DEPENDENCY","COMMITTED",null));
        check("INFRA_COMMITTED_ACK_RECOVERY",new Typed("DEPENDENCY","COMMITTED_DIAGNOSTIC_FAILED",null));
        check("",new IllegalStateException("DEPENDENCY|ROLLED_BACK"));
        check("",new Typed("OTHER","ROLLED_BACK",null));
        check("",new Typed("DEPENDENCY","OTHER",null));
        check("",new Typed("INTEGRITY","ROLLED_BACK",null));
        check("",new Typed("BUG","ROLLED_BACK",null));
        check("",new Typed("DB_CORRUPT","UNKNOWN",null));
        check("",new Typed("DEPENDENCY","ROLLED_BACK",new Typed("INTEGRITY","ROLLED_BACK",null)));
        check("",new Typed("DEPENDENCY","ROLLED_BACK",new Typed("BUG","ROLLED_BACK",null)));
        check("",new Typed("DEPENDENCY","ROLLED_BACK",new AssertionError("fatal")));
        Throwable cyclic=new RuntimeException(){@Override public synchronized Throwable getCause(){return this;}};
        check("",new Typed("DEPENDENCY","ROLLED_BACK",cyclic));
        Throwable unreadable=new RuntimeException(){@Override public synchronized Throwable getCause(){throw new IllegalStateException("unreadable");}};
        check("",new Typed("DEPENDENCY","ROLLED_BACK",unreadable));
        Throwable deep=new Typed("DEPENDENCY","ROLLED_BACK",null);
        for(int i=0;i<32;i++)deep=new RuntimeException(deep);
        check("",deep);
        check("INFRA_DEPENDENCY",new RuntimeException(new Typed("DEPENDENCY","ROLLED_BACK",null)));
        System.out.println("PASS "+cases+" typed infrastructure classification cases; platform-independent, not Android execution");
    }
}
