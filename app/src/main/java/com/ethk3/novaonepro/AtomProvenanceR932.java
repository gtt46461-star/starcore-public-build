package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Auditable provenance for the 240 registered atoms.
 *
 * <p>This class does not invent dependencies. Sources correspond to the actual A0-A11 dispatch
 * in {@link GenesisAtomicWorldR920}; consumers correspond to production classes that either use
 * an atom in predraw inference or audit/persist its observation. Settlement-only A9 atoms are
 * deliberately not advertised as predraw score inputs.</p>
 */
public final class AtomProvenanceR932 {
    public static final String REVISION = "R932_ATOM_PROVENANCE_V1";
    private static final String[] SOURCE_FUNCTION = {
        "truthAtom", "coordinateAtom", "temporalAtom", "topologyAtom",
        "transformAtom", "informationAtom", "memoryAtom", "riskAtom",
        "actionAtom", "A9_POSTDRAW_BOUNDARY", "runtimeAtom", "metaAtom"
    };

    private AtomProvenanceR932() {}

    public static String sourceOwner(int index) {
        checked(index);
        return "GenesisAtomicWorldR920";
    }

    public static String sourceFunction(int index) {
        checked(index);
        return SOURCE_FUNCTION[index / 20];
    }

    public static String maskContract(int index) {
        checked(index);
        GenesisSchemaRegistryR915.AtomSpec spec = GenesisSchemaRegistryR915.atoms().get(index);
        if (spec.settlementEvidence()) return "POSTDRAW_NOT_AVAILABLE_PREDRAW";
        if (GenesisSchemaRegistryR915.TIER_RESEARCH_PROXY.equals(spec.semanticTier()))
            return "PROXY_SOURCE_UNAVAILABLE_PREDRAW";
        return "PRESENT_IFF_FINITE_AND_HISTORY_SUPPORT_GTE_" + spec.minSupport
                + ";MISSING=NONFINITE_OR_HISTORY_SUPPORT_INSUFFICIENT";
    }

    public static String sourceContractHash(int index) {
        checked(index);
        GenesisSchemaRegistryR915.AtomSpec spec = GenesisSchemaRegistryR915.atoms().get(index);
        return V51ChecksumEngine.sha256(REVISION + '|' + sourceOwner(index) + '|'
                + sourceFunction(index) + '|' + maskContract(index) + '|' + index + '|'
                + spec.canonical());
    }

    /** Ordered and stable so the frozen payload is deterministic. */
    public static List<String> consumers(int index) {
        checked(index);
        GenesisSchemaRegistryR915.AtomSpec spec = GenesisSchemaRegistryR915.atoms().get(index);
        ArrayList<String> out = new ArrayList<String>();
        // Every frozen observation is consumed by persistence/quality/audit owners.
        out.add("AtomAdmissionAuditR932.record");
        out.add("AtomCaptureR932.freeze");
        out.add("AtomCaptureR932.maintain");
        if (spec.predrawComputable()) {
            // GenesisAssetGenome permits every legal predraw atom to enter atomIndices; the VM
            // checks the mask then directly reads context.atoms[index] into its executable inputs.
            out.add("GenesisAssetGenomeR915.atomIndices");
            out.add("GenesisAssetVmR915.evaluateInternal");
        }
        // Explicit fixed semantic consumers in GenesisAssetVmR915 in addition to dynamic genomes.
        switch (index) {
            case 15: out.add("GenesisAssetVmR915.evaluateInternal.risk"); break;
            case 33: out.add("GenesisAssetVmR915.semanticTransform.MOTIF_FIELD"); break;
            case 61: out.add("GenesisAssetVmR915.semanticTransform.TRANSITION_FIELD"); break;
            case 75: out.add("GenesisAssetVmR915.semanticTransform.RELATION_FIELD"); break;
            case 132:
                out.add("GenesisAssetVmR915.regimeBucket");
                out.add("GenesisAssetVmR915.semanticTransform.REGIME_FIELD");
                break;
            case 154: out.add("GenesisAssetVmR915.evaluateInternal.uncertainty"); break;
            case 155: out.add("GenesisAssetVmR915.evaluateInternal.risk"); break;
            case 157: out.add("GenesisAssetVmR915.evaluateInternal.uncertainty"); break;
            case 172: out.add("GenesisAssetVmR915.inputs.selectionMargin"); break;
            case 173: out.add("GenesisAssetVmR915.evaluateInternal.stability"); break;
            case 174:
                out.add("GenesisAssetVmR915.semanticTransform.RESIDUAL_FIELD");
                out.add("GenesisAssetVmR915.semanticTransform.CALIBRATION_RESIDUAL");
                break;
            default: break;
        }
        if (index >= 132 && index <= 139)
            out.add("GenesisAssetVmR915.shaped.STATE_MACHINE_FIELD");
        Collections.sort(out);
        return Collections.unmodifiableList(out);
    }

    public static String consumersHash(int index) {
        StringBuilder b = new StringBuilder(REVISION).append('|').append(index);
        for (String consumer : consumers(index)) b.append('|').append(consumer);
        return V51ChecksumEngine.sha256(b.toString());
    }

    /** Postdraw atoms must never be mislabeled as predraw score inputs. */
    public static boolean hasPredrawScoreConsumer(int index) {
        for (String consumer : consumers(index))
            if (consumer.startsWith("GenesisAssetGenomeR915")
                    || consumer.startsWith("GenesisAssetVmR915")) return true;
        return false;
    }

    private static void checked(int index) {
        if (index < 0 || index >= GenesisSchemaRegistryR915.atomCount())
            throw new IllegalArgumentException("R932_ATOM_PROVENANCE_INDEX");
    }
}
