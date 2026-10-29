package com.ethk3.novaonepro.strategyv2;

/** Explicit input-scale declarations and current causal availability. Bits index
 * ScaleEncoderR26.SCALES. Lifetime history length is not a declaration of support. */
public interface ScaleInputR26 {
    int configuredScaleMask();
    int availableScaleMask();
}
