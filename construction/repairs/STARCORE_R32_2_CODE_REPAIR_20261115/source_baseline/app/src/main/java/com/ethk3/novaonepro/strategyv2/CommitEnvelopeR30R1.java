package com.ethk3.novaonepro.strategyv2;

/** One prepared logical step. Caller publishes only the state returned after atomic commit success. */
public final class CommitEnvelopeR30R1 {
    public enum Outcome { COMMITTED, RETRY_LATER, SOURCE_CONFLICT }
    public interface AtomicSink { Outcome commit(String stepId,long fence,long revision,byte[] immutableNextState); }
    private final String id;private final long fence,revision;private final byte[] next;
    private final java.util.concurrent.atomic.AtomicInteger state=new java.util.concurrent.atomic.AtomicInteger();
    public CommitEnvelopeR30R1(String id,long fence,long revision,byte[] next){
        this.id=ResearchContractR30R1.sha(id);if(fence<0||revision<0||next==null)throw new IllegalArgumentException("STEP_FENCE");
        this.fence=fence;this.revision=revision;this.next=next.clone();
    }
    public Outcome attempt(AtomicSink sink){
        int current=state.get();if(current==2)return Outcome.COMMITTED;if(current==3)return Outcome.SOURCE_CONFLICT;
        if(!state.compareAndSet(0,1))return Outcome.RETRY_LATER;
        try {
            Outcome result=java.util.Objects.requireNonNull(sink.commit(id,fence,revision,next.clone()));
            state.set(result==Outcome.COMMITTED?2:result==Outcome.SOURCE_CONFLICT?3:0);return result;
        } catch(RuntimeException|Error failure){state.set(0);throw failure;}
    }
    public byte[] publishedState(){if(state.get()!=2)throw new IllegalStateException("UNCOMMITTED_STATE");return next.clone();}
    public String id(){return id;}
}
