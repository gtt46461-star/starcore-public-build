package com.ethk3.novaonepro.strategyv2;
import com.ethk3.novaonepro.DbStore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
/** Only numerical entry points are replaced, to hold workers at explicitly controlled boundaries. */
final class LifecycleBodies {
    interface Action { long run(long target) throws Exception; }
    static volatile Action action = target -> target;
    static final AtomicInteger entered=new AtomicInteger(), active=new AtomicInteger(), peak=new AtomicInteger();
    static long execute(long target) throws Exception {
        entered.incrementAndGet(); int concurrent=active.incrementAndGet(); peak.accumulateAndGet(concurrent,Math::max);
        try { return action.run(target); } finally { active.decrementAndGet(); }
    }
}
final class StrategyFoundrySU1 { static final class Attempt { } boolean done(){return false;} Attempt next(BudgetTokenSU1 token){return new Attempt();} }
final class CampaignStateSU1 implements AutoCloseable { final StrategyFoundrySU1 foundry=new StrategyFoundrySU1(); static CampaignStateSU1 open(DbStore store){return new CampaignStateSU1();} void checkpoint(StrategyFoundrySU1.Attempt attempt){} public void close(){} }
final class ExpertWorkshopR26 { static long step(DbStore store,Supplier<RuntimeCoordinatorR26.Claim> claim,BudgetTokenSU1 token,long target)throws Exception{return LifecycleBodies.execute(target);} }
final class IncumbentLoopSU2 { static long prepareNext(DbStore store,Supplier<RuntimeCoordinatorR26.Claim> claim,BudgetTokenSU1 token,long target)throws Exception{return LifecycleBodies.execute(target);} }
final class WarmCandidateR26 { static long step(DbStore store,Supplier<RuntimeCoordinatorR26.Claim> claim,BudgetTokenSU1 token)throws Exception{return LifecycleBodies.execute(1);} }
final class LivePoolR26 { static long publishOne(DbStore store,Supplier<RuntimeCoordinatorR26.Claim> claim,BudgetTokenSU1 token)throws Exception{return LifecycleBodies.execute(1);} static long trainOne(DbStore store,Supplier<RuntimeCoordinatorR26.Claim> claim,BudgetTokenSU1 token)throws Exception{return LifecycleBodies.execute(1);} }
final class PreparedForecastStoreR26 { static long step(DbStore store,Supplier<RuntimeCoordinatorR26.Claim> claim,BudgetTokenSU1 token)throws Exception{return LifecycleBodies.execute(1);} }
final class FoundryCampaignR26 { static long step(DbStore store,Supplier<RuntimeCoordinatorR26.Claim> claim,BudgetTokenSU1 token)throws Exception{return LifecycleBodies.execute(1);} }
final class LegacyReplayR26 {
    static long step(DbStore store,Supplier<RuntimeCoordinatorR26.Claim> claim,BudgetTokenSU1 token)throws Exception{return LifecycleBodies.execute(1);}
    static void clearWorking(){if(LifecycleBodies.active.get()!=0)throw new AssertionError("working state cleared under live worker");}
}
