package com.ethk3.novaonepro.r952;

import com.ethk3.novaonepro.Models;
import com.ethk3.novaonepro.TargetIssueResolver;
import com.ethk3.novaonepro.V51ChecksumEngine;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Structural proof for a bounded ordered page of authoritative draw rows. */
public final class HistoryContinuityR341 {
    public static final class Gap {
        public final String through, expected, observed;

        Gap(String through, String observed) {
            this.through = through;
            this.expected = TargetIssueResolver.next(through).targetIssue;
            this.observed = observed;
        }

        public String reason() { return "SOURCE_GAP:" + through + ":" + observed + ":EXPECTED=" + expected; }
    }

    public static final class Inspection {
        public final List<Gap> gaps;
        public final int contiguousSuffixStart;
        public final String sourceHash;

        Inspection(List<Gap> gaps, int start, String sourceHash) {
            this.gaps = Collections.unmodifiableList(gaps);
            this.contiguousSuffixStart = start;
            this.sourceHash = sourceHash;
        }
    }

    public static Inspection inspect(List<Models.Draw> ordered) {
        if (ordered == null || ordered.size() > 5000) throw new IllegalArgumentException("HISTORY_PAGE_BOUND");
        List<Gap> gaps = new ArrayList<>();
        String previous = "";
        StringBuilder identity = new StringBuilder("R341_AUTHORITATIVE_DRAW_PAGE");
        int start = 0;
        for (int i = 0; i < ordered.size(); i++) {
            Models.Draw row = ordered.get(i);
            if (row == null || !row.isValid() || !TargetIssueResolver.isValid(row.issue)
                    || (!previous.isEmpty() && TargetIssueResolver.compare(previous, row.issue) >= 0)) {
                throw new IllegalArgumentException("HISTORY_SOURCE_ORDER_OR_ROW_INVALID");
            }
            Gap gap = gap(previous, row.issue);
            if (gap != null) { gaps.add(gap); start = i; }
            identity.append('|').append(row.issue).append(':').append(row.fetchTime);
            for (int digit : row.numbers) identity.append(':').append(digit);
            previous = row.issue;
        }
        return new Inspection(gaps, start, V51ChecksumEngine.sha256(identity.toString()));
    }

    static Gap gap(String through, String observed) {
        return through.isEmpty() || TargetIssueResolver.exact(through, observed).valid ? null : new Gap(through, observed);
    }

    private HistoryContinuityR341() {}
}
