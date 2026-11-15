package com.ethk3.novaonepro;

import java.util.List;

/**
 * Typed V50 number-expert contract. Experts only emit evidence; they cannot mutate final
 * probability, rank, lane, authorization or Trade state.
 */
public interface V50NumberExpert {
    String id();

    V50Models.ExpertEvidence evaluate(
            V50Models.NumberFeatureSnapshot features,
            V50Models.StateTrajectorySnapshot[] states,
            List<Models.Draw> pointInTimeHistory,
            DbStore store);
}
