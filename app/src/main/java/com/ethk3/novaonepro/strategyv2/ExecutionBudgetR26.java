package com.ethk3.novaonepro.strategyv2;
/** Propagates the current cooperative token into scalar tapes and exact DP inner loops. */
public final class ExecutionBudgetR26 {
    private ExecutionBudgetR26() {}
    private static final ThreadLocal<BudgetTokenSU1> CURRENT=new ThreadLocal<>();
    public static void enter(BudgetTokenSU1 token){CURRENT.set(token);}
    public static void leave(){CURRENT.remove();}
    public static void poll(){BudgetTokenSU1 token=CURRENT.get();if(token!=null)token.checkpointOrYield();}
    public static BudgetTokenSU1 current(){return CURRENT.get();}
}
