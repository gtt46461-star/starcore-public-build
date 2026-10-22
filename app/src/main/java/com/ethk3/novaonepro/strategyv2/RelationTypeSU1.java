package com.ethk3.novaonepro.strategyv2;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Semantic axes and epistemic status are independent of the numeric tensor shape. */
public final class RelationTypeSU1 {
    public enum Axis { ISSUE, POSITION, NUMBER, SOURCE_POSITION, TARGET_POSITION }
    public enum Unit { CATEGORY, COUNT, PROBABILITY, LOG_LIFT, SCORE, RESIDUAL, COVARIANCE, CONSTRAINT }
    public enum Knowledge { OBSERVED_FACT, STRUCTURAL_CONSTRAINT, STATISTICAL_ESTIMATE, RESEARCH_HYPOTHESIS }

    public final List<Axis> axes;
    public final Unit unit;
    public final Knowledge knowledge;
    public final String event;

    public RelationTypeSU1(Unit unit, Knowledge knowledge, String event, Axis... axes) {
        this.unit = Objects.requireNonNull(unit);
        this.knowledge = Objects.requireNonNull(knowledge);
        if (event == null || !event.matches("[A-Z0-9_]{1,80}") || axes.length > 4)
            throw new IllegalArgumentException("SU1_RELATION_TYPE");
        for (Axis a : axes) Objects.requireNonNull(a);
        this.axes = Collections.unmodifiableList(Arrays.asList(axes.clone()));
        this.event = event;
        if (knowledge == Knowledge.STRUCTURAL_CONSTRAINT && unit != Unit.CONSTRAINT)
            throw new IllegalArgumentException("SU1_CONSTRAINT_UNIT");
    }

    public void requireCompatible(RelationTypeSU1 other) {
        if (!axes.equals(other.axes) || unit != other.unit || !event.equals(other.event))
            throw new IllegalArgumentException("SU1_RELATION_AXIS_UNIT_EVENT_MISMATCH");
    }

    public String canonical() {
        return unit + "|" + knowledge + "|" + event + "|" + axes;
    }
}
