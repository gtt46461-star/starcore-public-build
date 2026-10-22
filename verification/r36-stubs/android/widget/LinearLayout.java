package android.widget;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

public class LinearLayout extends ViewGroup {
    public static final int VERTICAL = 1, HORIZONTAL = 0;
    public LinearLayout(Context c) {}
    public void setOrientation(int o) {}

    public static class LayoutParams extends ViewGroup.LayoutParams {
        public LayoutParams(int w, int h) { super(w, h); }
        public LayoutParams(int w, int h, float weight) { super(w, h); }
        public void setMargins(int left, int top, int right, int bottom) {
            leftMargin = left;
            topMargin = top;
            rightMargin = right;
            bottomMargin = bottom;
        }
        public int leftMargin, rightMargin, topMargin, bottomMargin;
    }
}
