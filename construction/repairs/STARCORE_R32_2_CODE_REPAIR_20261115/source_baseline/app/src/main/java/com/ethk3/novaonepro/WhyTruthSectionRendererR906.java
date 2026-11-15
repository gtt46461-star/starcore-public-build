package com.ethk3.novaonepro;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Exclusive, row-diffing owner of the four mechanical hit/miss/no-bet/settlement truth cards. */
public final class WhyTruthSectionRendererR906 {
    private static final int ROWS = 4;
    private final Context context;
    private final LinearLayout host;
    private final String[] lastRowKeys = new String[ROWS];

    public WhyTruthSectionRendererR906(Context context, LinearLayout host) {
        this.context = context;
        this.host = host;
    }

    public void render(RuntimeTruthSnapshotR906 snapshot) {
        if (host == null || snapshot == null) return;
        ensureRows();
        for (int i = 0; i < ROWS; i++) {
            RuntimeTruthSnapshotR906.TruthCard card =
                    i < snapshot.whyCards.length ? snapshot.whyCards[i] : null;
            String title = card == null ? "真值" : card.title;
            String detail = card == null ? "暂无样本" : card.detail;
            String source = card == null
                    ? "source=R920_TRUTH｜status=NO_DATA"
                    : "source=" + card.source
                            + (card.targetIssue.length() > 0 ? "｜target=" + card.targetIssue : "")
                            + "｜status=" + card.status;
            String key = title + '\u0001' + detail + '\u0001' + source;
            if (key.equals(lastRowKeys[i])) continue;
            View row = host.getChildAt(i);
            ((TextView) row.findViewById(R.id.txt_why_truth_title)).setText(title);
            ((TextView) row.findViewById(R.id.txt_why_truth_detail)).setText(detail);
            ((TextView) row.findViewById(R.id.txt_why_truth_source)).setText(source);
            lastRowKeys[i] = key;
        }
    }

    private void ensureRows() {
        if (host.getChildCount() == ROWS && "R920_WHY_HOST".equals(host.getTag())) return;
        host.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(context);
        for (int i = 0; i < ROWS; i++) {
            host.addView(inflater.inflate(R.layout.row_why_truth_r906, host, false));
            lastRowKeys[i] = null;
        }
        host.setTag("R920_WHY_HOST");
    }
}
