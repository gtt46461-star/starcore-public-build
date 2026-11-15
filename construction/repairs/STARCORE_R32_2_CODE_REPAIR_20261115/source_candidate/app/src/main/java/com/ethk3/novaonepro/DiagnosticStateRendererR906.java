package com.ethk3.novaonepro;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Exclusive, row-diffing owner of all sixteen explicit diagnostic state boxes. */
public final class DiagnosticStateRendererR906 {
    private static final int BOX_COUNT = 16;
    private final Context context;
    private final LinearLayout host;
    private final String[] lastRowKeys = new String[BOX_COUNT];

    public DiagnosticStateRendererR906(Context context, LinearLayout host) {
        this.context = context;
        this.host = host;
    }

    public void render(RuntimeTruthSnapshotR906 snapshot) {
        if (host == null || snapshot == null) return;
        ensureRows();
        for (int i = 0; i < BOX_COUNT; i++) {
            RuntimeTruthSnapshotR906.StateBox box =
                    i < snapshot.stateBoxes.length ? snapshot.stateBoxes[i] : null;
            String title = box == null ? (i + 1) + ". 状态" : box.ordinal + ". " + box.title;
            String value = box == null ? "不可用｜原因=NO_DATA" : box.value;
            String source = box == null
                    ? "source=R915_TRUTH｜status=NO_DATA"
                    : "source=" + box.source
                            + (box.targetIssue.length() > 0 ? "｜target=" + box.targetIssue : "")
                            + "｜status=" + box.status;
            String key = title + '\u0001' + value + '\u0001' + source;
            if (key.equals(lastRowKeys[i])) continue;
            View row = host.getChildAt(i);
            ((TextView) row.findViewById(R.id.txt_diag_state_title)).setText(title);
            ((TextView) row.findViewById(R.id.txt_diag_state_value)).setText(value);
            ((TextView) row.findViewById(R.id.txt_diag_state_source)).setText(source);
            lastRowKeys[i] = key;
        }
    }

    private void ensureRows() {
        if (host.getChildCount() == BOX_COUNT && "R915_DIAG_HOST".equals(host.getTag())) return;
        host.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(context);
        for (int i = 0; i < BOX_COUNT; i++) {
            host.addView(inflater.inflate(R.layout.row_diagnostic_state_r906, host, false));
            lastRowKeys[i] = null;
        }
        host.setTag("R915_DIAG_HOST");
    }
}
