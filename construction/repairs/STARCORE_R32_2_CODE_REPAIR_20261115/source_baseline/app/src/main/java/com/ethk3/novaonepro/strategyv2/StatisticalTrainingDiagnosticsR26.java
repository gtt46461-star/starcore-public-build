package com.ethk3.novaonepro.strategyv2;

/** Measured statistical updates. A sufficient-statistic change is never represented as a zero gradient. */
public interface StatisticalTrainingDiagnosticsR26 {
    final class Step {
        public final long update;
        public final double lossBefore,lossAfter,statisticDeltaNorm,parameterDeltaNorm;
        public final String inputHash,objective,statisticName;
        public Step(long update,double before,double after,double statistic,double parameter,String inputHash,String objective,String name){
            if(update<1||!Double.isFinite(before)||!Double.isFinite(after)||!Double.isFinite(statistic)||!Double.isFinite(parameter)||statistic<0||parameter<0||inputHash==null||!inputHash.matches("[0-9a-f]{64}")||objective==null||objective.isEmpty()||name==null||name.isEmpty())throw new IllegalArgumentException("R26_STATISTICAL_TRAINING_DIAGNOSTICS");
            this.update=update;lossBefore=before;lossAfter=after;statisticDeltaNorm=statistic;parameterDeltaNorm=parameter;this.inputHash=inputHash;this.objective=objective;statisticName=name;
        }
    }
    Step lastStatisticalTrainingStep();
    long completedTrainingUpdates();
    String optimizerProtocol();
}
