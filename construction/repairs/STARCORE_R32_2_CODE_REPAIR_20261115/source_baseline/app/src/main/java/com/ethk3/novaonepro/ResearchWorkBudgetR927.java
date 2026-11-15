package com.ethk3.novaonepro;

/** One cooperative scope on the existing research worker; no new scheduler or writer. */
public final class ResearchWorkBudgetR927 {
    private static final ThreadLocal<Scope> CURRENT=new ThreadLocal<Scope>();
    private ResearchWorkBudgetR927(){}
    public static Scope enter(long budgetMs){Scope parent=CURRENT.get();Scope s=new Scope(parent,TimeSourceR811.monoNowMs()+Math.max(1L,budgetMs));CURRENT.set(s);return s;}
    public static void leave(Scope scope){if(scope!=null)CURRENT.set(scope.parent);}
    public static long remainingMs(){Scope s=CURRENT.get();return s==null?Long.MAX_VALUE:Math.max(0L,s.deadline-TimeSourceR811.monoNowMs());}
    public static boolean isActive(){return CURRENT.get()!=null;}
    public static void checkpoint(){
        Scope s=CURRENT.get();if(s==null)return;
        if(Thread.currentThread().isInterrupted()||RealtimePressureGateR811.hasActiveCriticalWork()||SettlementExecutionGateR819.isActive())throw new Yield("REALTIME_PREEMPTED");
        if(TimeSourceR811.monoNowMs()>=s.deadline)throw new Yield("MONOTONIC_SLICE_EXHAUSTED");
    }
    /** Only short lease bookkeeping may outlive a yielded compute slice. It cannot
     * publish model results or alter evidence; the original scope is always restored. */
    public static <T> T finishLeaseReceiptR66(java.util.function.Supplier<T> receipt) {
        Scope saved=CURRENT.get();CURRENT.remove();
        try(DatabaseCoordinator.WriterDeadlineScopeCP3 deadline=
                DatabaseCoordinator.withWriterDeadlineCP3(TimeSourceR811.monoNowMs()+250L)) {
            return receipt.get();
        } finally { if(saved==null)CURRENT.remove();else CURRENT.set(saved); }
    }
    public static final class Scope {
        final Scope parent;final long deadline;
        Scope(Scope p,long d){parent=p;deadline=p==null?d:Math.min(d,p.deadline);}
    }
    public static final class Yield extends RuntimeException {Yield(String reason){super("E02A_RESEARCH_YIELD:"+reason);}}
}
