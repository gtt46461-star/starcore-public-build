package android.widget;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.view.View;

public class TextView extends View {
    public TextView(Context c) {}
    public void setText(CharSequence s) {}
    public CharSequence getText() { return ""; }
    public void setTextSize(float s) {}
    public void setTextColor(int c) {}
    public void setGravity(int g) {}
    public void setMaxLines(int n) {}
    public void setMinLines(int n) {}
    public void setLines(int n) {}
    public int getLineHeight() { return 20; }
    public int getPaddingTop() { return 0; }
    public int getPaddingBottom() { return 0; }
    public void setMinHeight(int h) {}
    public void setMaxHeight(int h) {}
    public void setSingleLine(boolean b) {}
    public void setTypeface(Typeface typeface) {}
    public void setTypeface(Typeface typeface, int style) {}
    public void setTextIsSelectable(boolean b) {}
    public void setIncludeFontPadding(boolean b) {}
    public void setEllipsize(TextUtils.TruncateAt where) {}
}
