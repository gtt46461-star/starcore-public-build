package com.ethk3.novaonepro.strategyv2;

import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.DatabaseCoordinator;
import com.ethk3.novaonepro.DbStore;

/** Explicit legacy task ownership. The original numerical APIs remain executable, but their writer is resolved
 * before acquiring the realtime queue. Continuations enter this scope explicitly; unrelated P0 calls never inherit it. */
public final class LegacyResearchScopeR29 {
    private LegacyResearchScopeR29(){}
    private static final ThreadLocal<Scope> CURRENT=new ThreadLocal<>();
    private static final ThreadLocal<Integer> ROUTING=ThreadLocal.withInitial(()->0),REALTIME=ThreadLocal.withInitial(()->0);
    private static final ThreadLocal<SQLiteDatabase> WRITER=new ThreadLocal<>();
    public static final class Scope implements AutoCloseable {
        final DbStore store;final Scope previous;final java.util.function.BooleanSupplier valid;private boolean closed;
        Scope(DbStore store){if(store==null)throw new IllegalArgumentException("R29_LEGACY_SCOPE_STORE");this.store=store;valid=RuntimeCoordinatorR26.sessionValidity();previous=CURRENT.get();CURRENT.set(this);}
        public void close(){if(closed)return;if(CURRENT.get()!=this)throw new IllegalStateException("R29_LEGACY_SCOPE_ORDER");closed=true;if(previous==null)CURRENT.remove();else CURRENT.set(previous);}
    }
    public static Scope enter(DbStore store){return new Scope(store);}
    public static boolean active(){return CURRENT.get()!=null&&REALTIME.get()==0;}
    private static void requireScope(){for(Scope s=CURRENT.get();s!=null;s=s.previous)if(!s.valid.getAsBoolean())throw new IllegalStateException("R29_CANCELLED_OR_REPLACED_RESEARCH_SCOPE");}
    /** Derived mirror/receipt transactions use the same session and claim fence as their caller. */
    public static void checkWriteFence(SQLiteDatabase db){
        if(!active())return;
        requireScope();RuntimeCoordinatorR26.checkThreadFence(db);
    }
    public static boolean realtimeTransaction(){return REALTIME.get()>0;}
    /** Authority observation must not lease Root's DDL-capable pool just to resolve its route.
     * The coordinator preserves the current transaction handle for callers reading their own writes. */
    public static SQLiteDatabase rawRealtime(DbStore store){return store.coordinatorForResearchR952().realtimeReadOnlyR29();}
    /** Pure observation retains this caller's explicit scope: Root outside research work, RP only
     * within its active scope. The coordinator retains Root's current transaction for own writes. */
    public static SQLiteDatabase observationReader(DbStore store){
        return readable(store,store.coordinatorForResearchR952().realtimeReadOnlyR29());
    }
    private static SQLiteDatabase routedReader(DatabaseCoordinator coordinator,SQLiteDatabase original){
        SQLiteDatabase observation=coordinator.realtimeReadOnlyR29();
        // Resolve ownership on the independent Root pool. Before publication, retain the historical
        // writable-compatible handle: legacy callers may still initialize schema through readable().
        if(!original.getPath().equals(observation.getPath()))return ResearchRouteR29.reader(original);
        return ResearchRouteR29.active(observation)?ResearchRouteR29.reader(observation):original;
    }
    public static SQLiteDatabase readable(DatabaseCoordinator coordinator,SQLiteDatabase root){
        Scope s=CURRENT.get();return s!=null&&s.store.coordinatorForResearchR952()==coordinator&&REALTIME.get()==0?routedReader(coordinator,root):root;
    }
    public static SQLiteDatabase readable(DbStore store,SQLiteDatabase root){
        Scope s=CURRENT.get();return s!=null&&s.store.coordinatorForResearchR952()==store.coordinatorForResearchR952()&&REALTIME.get()==0?routedReader(store.coordinatorForResearchR952(),root):root;
    }
    public static SQLiteDatabase realtime(SQLiteDatabase supplied){
        Scope s=CURRENT.get();if(s!=null)return ResearchRouteR29.realtimeReadOnly(s.store.coordinatorForResearchR952().realtimeReadOnlyR29());
        return ResearchRouteR29.realtimeForWriter(supplied);
    }
    public static boolean routes(DatabaseCoordinator coordinator,String operation){
        Scope s=CURRENT.get();return s!=null&&REALTIME.get()==0&&s.store.coordinatorForResearchR952()==coordinator&&(ROUTING.get()==0||WRITER.get()!=null);
    }
    public static <T>T write(String operation,DatabaseCoordinator.WriteCallable<T> task)throws Exception{
        Scope s=CURRENT.get();if(s==null)throw new IllegalStateException("R29_LEGACY_SCOPE_REQUIRED");
        final boolean stopping="R26_SESSION_STOP".equals(operation);if(!stopping)requireScope();
        RuntimeCoordinatorR26.requireCurrentThreadSession();SQLiteDatabase nested=WRITER.get();
        if(nested!=null){RuntimeCoordinatorR26.checkThreadFence(nested);T value=task.call(nested);RuntimeCoordinatorR26.checkThreadFence(nested);return value;}
        ROUTING.set(ROUTING.get()+1);
        try{
            SQLiteDatabase root=s.store.coordinatorForResearchR952().realtimeReadOnlyR29();
            if(ResearchRouteR29.active(root)&&!stopping){RetainedFormalCopiesR29.ensure(s.store);LegacyDomainUpgradeR29.ensure(s.store);LegacyDomainExtensionR29.ensure(s.store);ResearchInputMirrorR29.refresh(s.store);}
            return ResearchRouteR29.write(s.store,operation,db->{
                boolean independent=ResearchRouteR29.isResearchDatabase(db);if(independent)WRITER.set(db);
                try{if(!stopping)requireScope();RuntimeCoordinatorR26.checkThreadFence(db);T value=task.call(db);RuntimeCoordinatorR26.checkThreadFence(db);if(!stopping)requireScope();return value;}
                finally{if(independent)WRITER.remove();}
            });
        }finally{ROUTING.set(ROUTING.get()-1);}
    }
    /** Used only for an explicitly prepared projection/freeze. Never acquire RT while owning RP's writer. */
    public static <T>T realtimeWrite(DbStore store,String operation,DatabaseCoordinator.WriteCallable<T> task){
        if(WRITER.get()!=null||ResearchRouteR29.writerHeldByCurrentThread())throw new IllegalStateException("R29_CROSS_DOMAIN_WRITE_LOCK_FORBIDDEN");
        requireScope();RuntimeCoordinatorR26.requireCurrentThreadSession();REALTIME.set(REALTIME.get()+1);
        try{return store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(operation,db->{requireScope();RuntimeCoordinatorR26.requireCurrentThreadSession();T value=task.call(db);RuntimeCoordinatorR26.requireCurrentThreadSession();requireScope();return value;});}
        finally{REALTIME.set(REALTIME.get()-1);}
    }
}
