package com.ethk3.novaonepro.strategyv2;

import java.nio.file.*;
import java.util.*;

/** IPM parity: the Java policy makes exactly the decisions of reference/replay/mainline_ipm.py (fixture r13_ipm). */
public final class MainlinePolicySU2Test {
    public static void main(String[] a) throws Exception {
        Path dir = Paths.get(a[0]); int bad = 0, total = 0;
        for (String name : new String[]{"keep_xi", "switch", "demote", "insample", "lowcov"}) {
            List<String> L = Files.readAllLines(dir.resolve(name + ".txt"));
            String[] h = L.get(0).trim().split(" "); int T = Integer.parseInt(h[0]), window = Integer.parseInt(h[1]);
            int m = Integer.parseInt(L.get(1).trim()); List<MainlinePolicySU2.Member> league = new ArrayList<>();
            for (int i = 0; i < m; i++) {
                String[] f = L.get(2 + i).split(" "); boolean[] act = new boolean[T], hit = new boolean[T];
                for (int t = 0; t < T; t++) { act[t] = f[3].charAt(t) == '1'; hit[t] = f[4].charAt(t) == '1'; }
                league.add(new MainlinePolicySU2.Member(f[0], f[0], f[1].equals("1"), act, hit, Integer.parseInt(f[2])));
            }
            int steps = Integer.parseInt(L.get(2 + m).trim()); String cur = null; int mism = 0; List<String> events = new ArrayList<>();
            for (int i = 0; i < steps; i++) {
                String[] e = L.get(3 + m + i).split(" "); int s = Integer.parseInt(e[0]);
                List<MainlinePolicySU2.Member> live = new ArrayList<>(); for (MainlinePolicySU2.Member x : league) if (x.birth <= s) live.add(x);
                MainlinePolicySU2.Choice ch = MainlinePolicySU2.choose(live, cur, s, window);
                if (!ch.member.id.equals(e[1]) || !ch.reason.equals(e[2])) mism++;
                if (!ch.reason.equals("KEEP")) events.add(s + ":" + ch.member.id + ":" + ch.reason);
                cur = ch.member.id; total++;
            }
            bad += mism;
            System.out.println(name + " steps=" + steps + " mismatches=" + mism + " final=" + cur + " events=" + events);
        }
        System.out.println(bad == 0 ? "R13_IPM_PARITY_PASS steps=" + total : "R13_IPM_PARITY_FAIL mismatches=" + bad);
        if (bad != 0) System.exit(1);
    }
}
