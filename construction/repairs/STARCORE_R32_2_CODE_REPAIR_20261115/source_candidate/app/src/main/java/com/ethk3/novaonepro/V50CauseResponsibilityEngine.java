package com.ethk3.novaonepro;

/** Outcome-time responsibility classification from frozen predictions. */
public final class V50CauseResponsibilityEngine {
    public V50Enums.AttributionType classify(
            V50Models.PositionAction selected,
            V50Models.PositionAction[] allActions,
            int actualAtSelectedPosition,
            boolean highTransitionRisk,
            boolean teacherDominant,
            boolean relationDominant,
            double selectedHitP,
            int[] actualByPosition) {
        if (selected == null) return V50Enums.AttributionType.UNKNOWN;
        boolean selectedHit = contains(selected.top3, actualAtSelectedPosition);
        if (selectedHit) {
            return selectedHitP < 0.20d
                    ? V50Enums.AttributionType.LUCKY_HIT
                    : V50Enums.AttributionType.STRUCTURAL_HIT;
        }
        if (highTransitionRisk) return V50Enums.AttributionType.REGIME_SWITCH;
        if (teacherDominant) return V50Enums.AttributionType.TEACHER_MISLEAD;
        if (relationDominant) return V50Enums.AttributionType.RELATION_MISLEAD;
        if (selectedHitP > 0.42d) {
            return V50Enums.AttributionType.CALIBRATION_OVERCONFIDENT;
        }
        if (selected.thirdMargin < 0.002d) {
            return V50Enums.AttributionType.THIRD_BOUNDARY_MISS;
        }
        if (anyOtherPositionHit(allActions, selected.position, actualByPosition)) {
            return V50Enums.AttributionType.POSITION_RANK_MISS;
        }
        return V50Enums.AttributionType.NUMBER_MODEL_MISS;
    }

    private static boolean anyOtherPositionHit(
            V50Models.PositionAction[] actions, int selectedPosition, int[] actualByPosition) {
        if (actions == null || actualByPosition == null || actualByPosition.length != 10)
            return false;
        for (int i = 0; i < actions.length; i++) {
            V50Models.PositionAction action = actions[i];
            if (action == null
                    || action.position == selectedPosition
                    || action.position < 1
                    || action.position > 10) continue;
            if (contains(action.top3, actualByPosition[action.position - 1])) return true;
        }
        return false;
    }

    private static boolean contains(int[] values, int number) {
        if (values == null) return false;
        for (int i = 0; i < values.length; i++) if (values[i] == number) return true;
        return false;
    }
}
