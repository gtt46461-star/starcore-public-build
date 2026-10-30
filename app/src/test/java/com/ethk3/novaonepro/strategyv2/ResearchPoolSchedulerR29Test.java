package com.ethk3.novaonepro.strategyv2;

import java.lang.reflect.*;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

/** Production selector regression: a multiplexed legacy dispatcher cannot receive only one turn per huge replay.
 * This exercises scheduling decisions, not numerical training or device timing. */
public final class ResearchPoolSchedulerR29Test {
    static final class Task implements ResearchPoolR27.Task{
        final String id;final boolean service;boolean enabled=true,urgent;
        Task(String id,boolean service){this.id=id;this.service=service;}
        public String id(){return id;}public boolean serviceDispatcher(){return service;}public boolean enabled(){return enabled;}
        public boolean urgent(ResearchPoolR27.Worker worker,long now){return urgent;}
        public ResearchPoolR27.Outcome step(ResearchPoolR27.Worker worker){return ResearchPoolR27.Outcome.idle("SYNTHETIC",0,-1);}
    }
    static ResearchPoolR27.Task next(ResearchPoolR27 pool,ResearchPoolR27.Worker worker)throws Exception{Method m=ResearchPoolR27.class.getDeclaredMethod("next",ResearchPoolR27.Worker.class);m.setAccessible(true);return (ResearchPoolR27.Task)m.invoke(pool,worker);}
    @SuppressWarnings("unchecked") static void release(ResearchPoolR27 pool,String id)throws Exception{Field f=ResearchPoolR27.class.getDeclaredField("running");f.setAccessible(true);((Map<String,Boolean>)f.get(pool)).put(id,false);}
    @Test public void dedicatedServiceWorkerCannotBeCapturedByLongReplay()throws Exception{
        ResearchPoolR27 pool=new ResearchPoolR27(null,"HOST_SELECTOR",2);
        try{
            Task service=new Task("legacy",true),replay=new Task("long-replay",false);pool.register(service);pool.register(replay);
            ResearchPoolR27.Worker first=pool.new Worker(null,null,"first");first.serviceWorker=true;ResearchPoolR27.Worker second=pool.new Worker(null,null,"second");
            assertSame(replay,next(pool,second));
            for(int i=0;i<64;i++){assertSame(service,next(pool,first));release(pool,service.id);}
            assertNull("the same replay must not be claimed by two workers",next(pool,second));
        }finally{pool.close();}
    }
    @Test public void committedEventsCanPreemptTheServiceDispatcher()throws Exception{
        ResearchPoolR27 pool=new ResearchPoolR27(null,"HOST_SELECTOR",2);
        try{
            Task service=new Task("legacy",true),event=new Task("event",false);event.urgent=true;pool.register(service);pool.register(event);
            ResearchPoolR27.Worker worker=pool.new Worker(null,null,"first");worker.serviceWorker=true;
            assertSame(event,next(pool,worker));event.urgent=false;release(pool,event.id);assertSame(service,next(pool,worker));
        }finally{pool.close();}
    }
    @Test public void privateContinuationResumesSoonWithoutClaimingProgress(){
        assertEquals(10,ResearchPoolR27.retryDelay("COOPERATIVE_NUMERICAL_STATE_AND_CHECKPOINT",null));
        assertEquals(50,ResearchPoolR27.retryDelay("COOPERATIVE_WRITER_BUSY",null));assertEquals(1000,ResearchPoolR27.retryDelay("NO_NEW_EVENT",null));
        assertNotEquals(WaitKindR27.PROGRESS,WaitKindR27.classify(0,"COOPERATIVE_NUMERICAL_STATE_AND_CHECKPOINT",null,0,20,null,false).kind);
    }
    @Test public void originalOwnerRemainsUntilTheHandoffIsEnabled()throws Exception{
        ResearchPoolR27 pool=new ResearchPoolR27(null,"HOST_SELECTOR",2);
        try{
            Task service=new Task("legacy",true),migration=new Task("migration",false);service.enabled=false;pool.register(service);pool.register(migration);
            ResearchPoolR27.Worker worker=pool.new Worker(null,null,"first");worker.serviceWorker=true;assertSame(migration,next(pool,worker));
        }finally{pool.close();}
    }
}
