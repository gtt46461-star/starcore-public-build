package com.ethk3.novaonepro.strategyv2;

import java.util.function.BooleanSupplier;

/** Cooperative, monotonic deadline. Never sleeps or acquires a database lock. */
public final class BudgetTokenSU1 {
    public static final class Yield extends RuntimeException {
        public Yield(String reason) { super(reason); }
    }
    private final long deadlineNanos;
    private final BooleanSupplier preempt;
    private long remainingOperations;
    public BudgetTokenSU1(long millis,long operations,BooleanSupplier preempt) {
        if(millis<1||millis>3600000||operations<1)throw new IllegalArgumentException("SU1_BUDGET");
        deadlineNanos=System.nanoTime()+millis*1000000L;remainingOperations=operations;
        this.preempt=preempt==null?()->false:preempt;
    }
    public void checkpointOrYield() {
        if(Thread.currentThread().isInterrupted()||preempt.getAsBoolean())throw new Yield("SU1_PREEMPTED");
        if(--remainingOperations<0||System.nanoTime()>=deadlineNanos)throw new Yield("SU1_BUDGET_EXHAUSTED");
    }
    public boolean tryConsume(long estimatedMicros) {
        if(estimatedMicros<0)throw new IllegalArgumentException("SU1_COST");
        if(System.nanoTime()+estimatedMicros*1000L>=deadlineNanos)return false;
        checkpointOrYield();return true;
    }
}
