package com.ethk3.novaonepro.strategyv2;
/** Only a DEV scout uses the cheaper fit. Live, VAL and sealed execution always use FULL. */
public enum TrainingFidelitySU1 {
    FULL(32,8,32,2),SCOUT(8,2,8,1);
    public final int stateExamples,graphExamples,fusionExamples,fusionEpochs;
    public final String hash;
    TrainingFidelitySU1(int state,int graph,int fusion,int epochs){stateExamples=state;graphExamples=graph;fusionExamples=fusion;fusionEpochs=epochs;hash=HashingSU1.fields("SU1_TRAINING_FIDELITY_V1",name(),String.valueOf(state),String.valueOf(graph),String.valueOf(fusion),String.valueOf(epochs));}
}
