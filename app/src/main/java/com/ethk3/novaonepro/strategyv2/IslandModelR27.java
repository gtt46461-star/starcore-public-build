package com.ethk3.novaonepro.strategyv2;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** R27-027 island model (docs/05 5.2): one island per idea family I01–I13. Every 5 generations each island's best two
 * (by H_SELECT paired u) migrate to the next island on the ring. Islands are views over the population. */
public final class IslandModelR27 {
    private IslandModelR27() {}
    public static final String[] ISLANDS = {"I01", "I02", "I03", "I04", "I05", "I06", "I07", "I08", "I09", "I10", "I11", "I12", "I13"};
    public static final int MIGRATE_EVERY = 5, MIGRANTS = 2;

    public static int index(String island) { for (int i = 0; i < ISLANDS.length; i++) if (ISLANDS[i].equals(island)) return i; return 8; }
    public static String next(String island) { return ISLANDS[(index(island) + 1) % ISLANDS.length]; }

    public interface Member { String island(); double selectU(); String hash(); }

    /** @return pairs {memberHash, destinationIsland} to apply. */
    public static <M extends Member> List<String[]> migrations(List<M> population) {
        List<String[]> out = new ArrayList<>();
        for (String isl : ISLANDS) {
            List<M> on = new ArrayList<>();
            for (M m : population) if (m.island().equals(isl)) on.add(m);
            on.sort(Comparator.<M>comparingDouble(m -> -m.selectU()).thenComparing(Member::hash));
            for (int i = 0; i < Math.min(MIGRANTS, on.size()); i++) out.add(new String[] {on.get(i).hash(), next(isl)});
        }
        return out;
    }

    /** The receiving island keeps its existing behavior; accepted arrivals also reserve their signature. */
    public static <M extends Member> List<String[]> migrations(List<M> population, java.util.Map<String,String> fingerprints, BehaviorSignatureR30.Rejected rejected) {
        java.util.Map<String,java.util.Map<String,String>> receiving = new java.util.HashMap<>();
        for (M member : population) {
            String fingerprint = fingerprints.get(member.hash());
            if (fingerprint != null) receiving.computeIfAbsent(member.island(), k -> new java.util.LinkedHashMap<>()).putIfAbsent(fingerprint, member.hash());
        }
        List<String[]> out = new ArrayList<>();
        for (String[] move : migrations(population)) {
            String fingerprint = fingerprints.get(move[0]);
            if (fingerprint == null) continue;
            java.util.Map<String,String> destination = receiving.computeIfAbsent(move[1], k -> new java.util.LinkedHashMap<>());
            String kept = destination.get(fingerprint);
            if (kept != null && !kept.equals(move[0])) rejected.duplicate(fingerprint, kept, move[0]);
            else { out.add(move); destination.put(fingerprint, move[0]); }
        }
        return out;
    }

    /** Diversity metric: mean pairwise behavior Jaccard between island champions (reported; >0.9 = collapse). */
    public static double championSimilarity(List<int[]> championSignatures) {
        double s = 0; int n = 0;
        for (int i = 0; i < championSignatures.size(); i++) for (int j = i + 1; j < championSignatures.size(); j++) { s += BirthGateR27.jaccard(championSignatures.get(i), championSignatures.get(j)); n++; }
        return n == 0 ? Double.NaN : s / n;
    }
}
