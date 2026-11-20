package com.ethk3.novaonepro.strategyv2;

import java.util.Objects;

/** Validates committed numerical/phase advancement, never wall time or scheduler pulses. */
public final class AtomNetProgressR33 {
    private AtomNetProgressR33() {}
    public static final class Frame {
        final String dataset,phase,complete,partial;
        final long materialized,trained,online;
        final int epoch,offset,fill,nextPosition;
        public Frame(String dataset,String phase,long materialized,long trained,long online,int epoch,int offset,int fill,int nextPosition,String complete,String partial) {
            this.dataset=Objects.requireNonNull(dataset);this.phase=Objects.requireNonNull(phase);
            this.materialized=materialized;this.trained=trained;this.online=online;
            this.epoch=epoch;this.offset=offset;this.fill=fill;this.nextPosition=nextPosition;
            this.complete=Objects.requireNonNull(complete);this.partial=Objects.requireNonNull(partial);
        }
    }
    public static String validate(Frame before,Frame after,int consumed) {
        if(!before.dataset.equals(after.dataset))throw new IllegalStateException("R33_ATOMNET_DATASET_CHANGED_IN_SLICE");
        long m=after.materialized-before.materialized,t=after.trained-before.trained,o=after.online-before.online;
        if(consumed<0||consumed>120||m<0||m>120||t<0||t>64||o<0||o>1
                ||(m>0&&(t>0||o>0))||(t>0&&o>0))
            throw new IllegalStateException("R33_ATOMNET_SLICE_COUNTER_CONTRACT");
        if((m>0&&consumed!=m)||(t>0&&consumed!=t)||(o>0&&consumed!=o))
            throw new IllegalStateException("R335_ATOMNET_CONSUMED_COUNTER_MISMATCH");
        boolean structural=before.epoch!=after.epoch||before.offset!=after.offset||before.fill!=after.fill
                ||before.nextPosition!=after.nextPosition||!before.phase.equals(after.phase)
                ||!before.complete.equals(after.complete)||!before.partial.equals(after.partial);
        if(m==0&&t==0&&o==0&&!structural)
            throw new IllegalStateException("R33_ATOMNET_NO_NUMERICAL_OR_PHASE_PROGRESS");
        if(m>0)return "MATERIALIZE";
        if(t>0)return "PRETRAIN_GRADIENT";
        if(o>0)return "ONLINE_GRADIENT";
        return "EVALUATION_OR_PHASE_TRANSITION";
    }
}
