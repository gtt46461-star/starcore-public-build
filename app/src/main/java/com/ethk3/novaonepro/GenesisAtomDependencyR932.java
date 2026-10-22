package com.ethk3.novaonepro;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * Exact atom dependency closure for executable GENESIS_NEW candidates.
 *
 * <p>Only production-score-eligible pre-draw atoms may enter a new candidate. Historical contexts
 * without an observation mask remain readable, but no fresh execution may silently consume a
 * quarantined, missing, proxy, audit-only or post-draw atom.</p>
 */
public final class GenesisAtomDependencyR932 {
    public static final String REVISION = "R932_GENESIS_ATOM_DEPENDENCY_V2";

    private GenesisAtomDependencyR932() {}

    public static int[] dependencies(GenesisAssetGenomeR915 genome, boolean materializedShape) {
        if (genome == null) throw new IllegalArgumentException("R932_GENESIS_DEPENDENCY_GENOME");
        TreeSet<Integer> out = new TreeSet<Integer>();
        for (int index : genome.atomIndices) out.add(Integer.valueOf(production(index)));

        // Inputs and the fixed risk/stability envelope used by every compact score.
        out.add(Integer.valueOf(production(172)));
        out.add(Integer.valueOf(production(15)));
        out.add(Integer.valueOf(production(154)));
        out.add(Integer.valueOf(production(155)));
        out.add(Integer.valueOf(production(157)));
        out.add(Integer.valueOf(production(173)));

        switch (genome.outputType) {
            case RESIDUAL_FIELD:
            case CALIBRATION_RESIDUAL:
                out.add(Integer.valueOf(residualSemantic()));
                break;
            case RELATION_FIELD:
                out.add(Integer.valueOf(relationSemantic()));
                break;
            case TRANSITION_FIELD:
                out.add(Integer.valueOf(transitionSemantic()));
                break;
            case MOTIF_FIELD:
                out.add(Integer.valueOf(motifSemantic()));
                break;
            case REGIME_FIELD:
                out.add(Integer.valueOf(regimeSemantic()));
                break;
            default:
                break;
        }

        if (materializedShape && genome.outputType.shaped) {
            for (int i = 0; i < genome.outputType.flatSize; i++) {
                switch (genome.outputType) {
                    case MATRIX_FIELD:
                        out.add(Integer.valueOf(shapeSignal(genome, i)));
                        out.add(Integer.valueOf(shapeTopology(i)));
                        break;
                    case GRAPH_FIELD:
                    case TENSOR_FIELD:
                        out.add(Integer.valueOf(shapeSignal(genome, i)));
                        out.add(Integer.valueOf(shapeTopology(i)));
                        out.add(Integer.valueOf(shapeTime(i)));
                        break;
                    case SEQUENCE_FIELD:
                    case PROTOTYPE_LIBRARY:
                        out.add(Integer.valueOf(shapeSignal(genome, i)));
                        out.add(Integer.valueOf(shapeTime(i)));
                        break;
                    case STATE_MACHINE_FIELD:
                        out.add(Integer.valueOf(shapeMemory(i)));
                        break;
                    default:
                        // Vector/distribution/default shapes use only primary + action geometry.
                        break;
                }
            }
        }

        int[] values = new int[out.size()];
        int at = 0;
        for (Integer index : out) values[at++] = index.intValue();
        return values;
    }

    public static String unavailable(
            GenesisAssetGenomeR915 genome,
            GenesisAssetVmR915.PredrawContext context,
            boolean materializedShape,
            Set<Integer> quarantined) {
        if (context == null) return "CONTEXT_MISSING";
        if (context.observation == null) return ""; // Historical CTX1 stays readable; no new mask is fabricated.
        AtomCaptureR932.Values observation = context.observation;
        int n = GenesisSchemaRegistryR915.atomCount();
        if (observation.values.length != n || observation.present.length != n
                || observation.missingReasons.length != n || observation.costNs.length != n)
            return "ATOM_MASK_DIMENSION";
        if (!TargetIssueResolver.isValid(observation.maxSourceIssue)
                || TargetIssueResolver.compare(observation.maxSourceIssue, context.targetIssue) >= 0)
            return "ATOM_MAX_SOURCE_NOT_PREDRAW";

        Set<Integer> blocked = quarantined == null
                ? Collections.<Integer>emptySet() : quarantined;
        for (int index : dependencies(genome, materializedShape)) {
            GenesisSchemaRegistryR915.AtomSpec atom = GenesisSchemaRegistryR915.atoms().get(index);
            if (!atom.productionScoreEligible()) return "NON_PRODUCTION_ATOM:" + atom.id;
            if (blocked.contains(Integer.valueOf(index))) return "ATOM_QUARANTINED:" + atom.id;
            if (!observation.present[index])
                return "ATOM_INPUT_MISSING:" + atom.id + ':' + safe(observation.missingReasons[index]);
            if (observation.missingReasons[index] == null || observation.missingReasons[index].length() != 0)
                return "ATOM_MASK_INCONSISTENT:" + atom.id;
            double value = context.atoms[index];
            if (Double.isNaN(value) || Double.isInfinite(value) || value < 0.0d || value > 1.0d)
                return "ATOM_VALUE_NONFINITE_OR_RANGE:" + atom.id;
            if (Double.doubleToLongBits(value) != Double.doubleToLongBits(observation.values[index]))
                return "ATOM_OBSERVATION_VALUE_DRIFT:" + atom.id;
        }
        return "";
    }

    static void require(
            GenesisAssetGenomeR915 genome,
            GenesisAssetVmR915.PredrawContext context,
            boolean materializedShape) {
        String reason = unavailable(genome, context, materializedShape, null);
        if (reason.length() != 0) throw new IllegalStateException("R932_GENESIS_ATOM_INPUT|" + reason);
    }

    static int residualSemantic() { return production(174); }
    static int relationSemantic() { return production(75); }
    static int transitionSemantic() { return production(61); }
    /** Historical index 33 is triple_identity (STRING) and is not a production numeric signal. */
    static int motifSemantic() { return eligibleInGroup(3, 7); }
    static int regimeSemantic() { return production(132); }

    static int shapeSignal(GenesisAssetGenomeR915 genome, int ordinal) {
        if (genome.atomIndices.length == 0) throw new IllegalStateException("R932_SHAPE_SIGNAL_EMPTY");
        int at = Math.floorMod(ordinal * 17 + genome.outputType.ordinal() * 13, genome.atomIndices.length);
        return production(genome.atomIndices[at]);
    }

    static int shapeTopology(int ordinal) { return eligibleInGroup(3, ordinal * 31 + 15); }
    static int shapeTime(int ordinal) { return eligibleInGroup(2, ordinal * 11 + 3); }
    static int shapeMemory(int ordinal) { return eligibleInGroup(6, ordinal + 12); }

    private static int production(int index) {
        if (index < 0 || index >= GenesisSchemaRegistryR915.atomCount())
            throw new IllegalArgumentException("R932_ATOM_DEPENDENCY_INDEX:" + index);
        GenesisSchemaRegistryR915.AtomSpec atom = GenesisSchemaRegistryR915.atoms().get(index);
        if (!atom.productionScoreEligible())
            throw new IllegalStateException("R932_ATOM_DEPENDENCY_NON_PRODUCTION:" + atom.id + ':' + index);
        return index;
    }

    private static int eligibleInGroup(int group, int ordinal) {
        int start = group * 20, end = start + 20, count = 0;
        for (int i = start; i < end; i++)
            if (GenesisSchemaRegistryR915.atoms().get(i).productionScoreEligible()) count++;
        if (count == 0) throw new IllegalStateException("R932_ATOM_GROUP_NO_PRODUCTION_INPUT:" + group);
        int wanted = Math.floorMod(ordinal, count);
        for (int i = start; i < end; i++) {
            if (!GenesisSchemaRegistryR915.atoms().get(i).productionScoreEligible()) continue;
            if (wanted-- == 0) return i;
        }
        throw new IllegalStateException("R932_ATOM_GROUP_INDEX:" + group);
    }

    private static String safe(String value) { return value == null ? "" : value; }
}
