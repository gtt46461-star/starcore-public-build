package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Reversible typed parent trace for every executable GENESIS_NEW molecule.
 *
 * <p>The trace is derived from the exact production dependency closure used by the VM. It does not
 * invent a second feature graph: explicit genome atoms, fixed semantic atoms and materialized-shape
 * atoms are all expanded through {@link GenesisAtomDependencyR932}. A persisted/new candidate is
 * valid only when every parent resolves to the immutable 240-atom registry and is production-score
 * eligible. This gives E04A a concrete atom <- molecule reverse path without changing any legacy
 * matrix formula.</p>
 */
public final class GenesisMoleculeTraceR932 {
    public static final String REVISION = "R932_GENESIS_MOLECULE_PARENT_TRACE_V1";

    private GenesisMoleculeTraceR932() {}

    public static Trace trace(GenesisAssetGenomeR915 genome, boolean materializedShape) {
        if (genome == null) throw new IllegalArgumentException("R932_MOLECULE_GENOME_NULL");
        int[] indices = GenesisAtomDependencyR932.dependencies(genome, materializedShape);
        ArrayList<Parent> parents = new ArrayList<Parent>(indices.length);
        boolean[] seen = new boolean[GenesisSchemaRegistryR915.atomCount()];
        for (int index : indices) {
            if (index < 0 || index >= seen.length || seen[index])
                throw new IllegalStateException("R932_MOLECULE_PARENT_INDEX:" + index);
            seen[index] = true;
            GenesisSchemaRegistryR915.AtomSpec atom = GenesisSchemaRegistryR915.atoms().get(index);
            if (!atom.productionScoreEligible())
                throw new IllegalStateException("R932_MOLECULE_PARENT_NOT_PRODUCTION:" + atom.id);
            parents.add(new Parent(index, atom.id, atom.group, atom.name,
                    V51ChecksumEngine.sha256(atom.canonical())));
        }
        for (int index : genome.atomIndices)
            if (index < 0 || index >= seen.length || !seen[index])
                throw new IllegalStateException("R932_MOLECULE_EXPLICIT_PARENT_LOST:" + index);
        return new Trace(genome.structuralHash, genome.outputType.name(), materializedShape, parents);
    }

    /** Exact production check used by certified consumers after an atom is quarantined. */
    public static boolean blockedByQuarantine(
            GenesisAssetGenomeR915 genome, boolean materializedShape, Set<Integer> quarantined) {
        if (quarantined == null || quarantined.isEmpty()) return false;
        Trace trace = trace(genome, materializedShape);
        for (Parent parent : trace.parents)
            if (quarantined.contains(Integer.valueOf(parent.index))) return true;
        return false;
    }

    public static final class Parent {
        public final int index;
        public final String atomId, group, name, semanticHash;

        Parent(int index, String atomId, String group, String name, String semanticHash) {
            this.index = index;
            this.atomId = atomId;
            this.group = group;
            this.name = name;
            this.semanticHash = semanticHash;
        }

        public String canonical() {
            return index + ":" + atomId + ':' + group + ':' + name + ':' + semanticHash;
        }
    }

    public static final class Trace {
        public final String assetStructuralHash, outputType, traceHash;
        public final boolean materializedShape;
        public final List<Parent> parents;

        Trace(String structuralHash, String outputType, boolean materializedShape, List<Parent> parents) {
            assetStructuralHash = structuralHash == null ? "" : structuralHash;
            this.outputType = outputType == null ? "" : outputType;
            this.materializedShape = materializedShape;
            this.parents = Collections.unmodifiableList(new ArrayList<Parent>(parents));
            traceHash = V51ChecksumEngine.sha256(canonicalWithoutHash());
        }

        public boolean completeFor(GenesisAssetGenomeR915 genome) {
            if (genome == null || !assetStructuralHash.equals(genome.structuralHash)) return false;
            int[] expected = GenesisAtomDependencyR932.dependencies(genome, materializedShape);
            if (expected.length != parents.size()) return false;
            for (int i = 0; i < expected.length; i++) {
                Parent parent = parents.get(i);
                if (parent.index != expected[i]) return false;
                GenesisSchemaRegistryR915.AtomSpec atom = GenesisSchemaRegistryR915.atoms().get(parent.index);
                if (!parent.atomId.equals(atom.id)
                        || !parent.group.equals(atom.group)
                        || !parent.name.equals(atom.name)
                        || !parent.semanticHash.equals(V51ChecksumEngine.sha256(atom.canonical()))) return false;
            }
            return traceHash.equals(V51ChecksumEngine.sha256(canonicalWithoutHash()));
        }

        public String canonical() {
            return canonicalWithoutHash() + "|trace_hash=" + traceHash;
        }

        private String canonicalWithoutHash() {
            StringBuilder out = new StringBuilder(REVISION)
                    .append("|asset=").append(assetStructuralHash)
                    .append("|output=").append(outputType)
                    .append("|materialized=").append(materializedShape ? 1 : 0)
                    .append("|parents=");
            for (int i = 0; i < parents.size(); i++) {
                if (i > 0) out.append(',');
                out.append(parents.get(i).canonical());
            }
            return out.toString();
        }
    }
}
