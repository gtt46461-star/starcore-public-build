package com.ethk3.novaonepro;

import java.util.LinkedHashMap;
import java.util.Map;

/** Single-owner registry for every field that can influence a real action. */
public final class V51FieldOwnerRegistry {
    private V51FieldOwnerRegistry() {}

    public static Map<String, String> owners() {
        LinkedHashMap<String, String> out = new LinkedHashMap<String, String>();
        put(out, "PredictionContext", "TargetIssueResolver");
        put(out, "PermutationFeatures", "V51PermutationFeatureEngine");
        put(out, "TrajectoryFeatures", "V51TrajectoryFeatureEngine");
        put(out, "ObservableState", "V51ObservableStateVectorEngine");
        put(out, "StatePrototype", "V51StatePrototypeEngine");
        put(out, "Lifecycle", "V51LifecycleEngine");
        put(out, "ExpertWeight", "V51ExpertGateEngine");
        put(out, "NumberProbability", "V51PermutationNumberModelEngine");
        put(out, "PositionAction", "V51PositionActionBuilder");
        put(out, "Rank", "V51ActionRankerEngine");
        put(out, "SelectedHitP", "V51SelectedActionCalibrationEngine");
        put(out, "Predictability", "V51PredictabilityEngine");
        put(out, "DomainCertificate", "V51DomainCertificateEngine");
        put(out, "Lane", "V51SelectiveRiskController");
        put(out, "Authorization", "DecisionAuthorizationEngine");
        put(out, "RealBet", "DecisionGateEngine");
        put(out, "Settlement", "SettlementEngine");
        return out;
    }

    private static void put(Map<String, String> map, String field, String owner) {
        if (map.containsKey(field)) throw new IllegalStateException("V51_DUPLICATE_OWNER_" + field);
        map.put(field, owner);
    }

    public static String fingerprint() {
        StringBuilder text = new StringBuilder();
        for (Map.Entry<String, String> item : owners().entrySet()) {
            text.append(item.getKey()).append('=').append(item.getValue()).append(';');
        }
        return V51ChecksumEngine.sha256(text.toString());
    }
}
