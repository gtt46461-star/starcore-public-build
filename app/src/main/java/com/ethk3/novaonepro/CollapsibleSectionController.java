package com.ethk3.novaonepro;

import android.view.View;
import android.widget.TextView;

/** Bounded collapsible section: never expands a multi-thousand-line TextView. */
public final class CollapsibleSectionController {
    private CollapsibleSectionController() {}

    public static void bind(final TextView body, final View content, final int collapsedLines) {
        if (body == null || content == null) return;
        body.setMaxLines(Math.max(4, collapsedLines));
        content.setVisibility(View.GONE);
        body.setOnClickListener(
                new View.OnClickListener() {
                    public void onClick(View v) {
                        boolean show = content.getVisibility() != View.VISIBLE;
                        content.setVisibility(show ? View.VISIBLE : View.GONE);
                        body.setMaxLines(
                                show
                                        ? Math.max(8, collapsedLines * 2)
                                        : Math.max(4, collapsedLines));
                    }
                });
    }
}
