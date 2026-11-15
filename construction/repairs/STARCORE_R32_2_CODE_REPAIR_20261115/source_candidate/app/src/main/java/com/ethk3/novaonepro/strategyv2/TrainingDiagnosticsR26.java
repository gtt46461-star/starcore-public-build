package com.ethk3.novaonepro.strategyv2;

/** Optional measured update diagnostics. Absence is never a zero gradient or completed contract. */
public interface TrainingDiagnosticsR26 {
    final class Step {
        public final long update;
        public final double lossBefore,lossAfter,gradientNorm,parameterDeltaNorm;
        public final String inputHash,objective;
        public Step(long update,double before,double after,double gradient,double delta,String inputHash,String objective){
            if(update<1||!Double.isFinite(before)||!Double.isFinite(after)||!Double.isFinite(gradient)||!Double.isFinite(delta)||gradient<0||delta<0||inputHash==null||!inputHash.matches("[0-9a-f]{64}")||objective==null||objective.isEmpty())throw new IllegalArgumentException("R26_TRAINING_DIAGNOSTICS");
            this.update=update;lossBefore=before;lossAfter=after;gradientNorm=gradient;parameterDeltaNorm=delta;this.inputHash=inputHash;this.objective=objective;
        }
    }
    /** Null until a complete update in this process; transient diagnostics are not restored as new work. */
    Step lastTrainingStep();
    long completedTrainingUpdates();
    String optimizerProtocol();
}
