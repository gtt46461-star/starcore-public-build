package com.ethk3.novaonepro.strategyv2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** R27-027 MAP-Elites diversity archive (docs/05 5.2): act-rate band (5) × main information source (8) ×
 * correlation-with-champion band (4) = 160 cells. Each cell keeps the best H_SELECT paired-u policy. */
public final class EliteArchiveR27 {
    public static final int CELLS = 5 * 8 * 4;
    public static final class Elite { public final PolicySpecR27 spec; public final double u, tau; public Elite(PolicySpecR27 s, double u, double tau) { spec = s; this.u = u; this.tau = tau; } }
    private final Map<Integer, Elite> cells = new TreeMap<>();

    public static int actBand(double rate) { return rate < 0.05 ? 0 : rate < 0.10 ? 1 : rate < 0.15 ? 2 : rate < 0.25 ? 3 : 4; }
    public static int corrBand(double jaccard) { return jaccard < 0.25 ? 0 : jaccard < 0.5 ? 1 : jaccard < 0.75 ? 2 : 3; }
    public static int cell(double actRate, int source, double corr) { return (actBand(actRate) * 8 + source) * 4 + corrBand(corr); }

    /** Returns true when the policy took (or kept) the cell. */
    public synchronized boolean offer(int cell, PolicySpecR27 spec, double u, double tau) {
        Elite e = cells.get(cell);
        if (e == null || u > e.u || (u == e.u && spec.policyHash().compareTo(e.spec.policyHash()) < 0)) { cells.put(cell, new Elite(spec, u, tau)); return true; }
        return e.spec.policyHash().equals(spec.policyHash());
    }
    public synchronized boolean occupies(String policyHash) { for (Elite e : cells.values()) if (e.spec.policyHash().equals(policyHash)) return true; return false; }
    public synchronized List<PolicySpecR27> members() { List<PolicySpecR27> out = new ArrayList<>(); for (Elite e : cells.values()) out.add(e.spec); return out; }
    public synchronized Map<Integer, Elite> snapshot() { return new TreeMap<>(cells); }
    public synchronized int filled() { return cells.size(); }
    public synchronized void put(int cell, Elite e) { cells.put(cell, e); }
}
