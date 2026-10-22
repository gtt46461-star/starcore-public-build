package android.view;

public class View {
    public static final int VISIBLE = 0, INVISIBLE = 4, GONE = 8;
    public static final int SYSTEM_UI_FLAG_VISIBLE = 0;

    public interface OnClickListener { void onClick(View v); }
    public interface OnLongClickListener { boolean onLongClick(View v); }

    public void setOnClickListener(OnClickListener l) {}
    public void setOnLongClickListener(OnLongClickListener l) {}
    public void setVisibility(int v) {}
    public int getVisibility() { return VISIBLE; }
    public void setEnabled(boolean b) {}
    public void setSelected(boolean b) {}
    public void setAlpha(float f) {}
    public void setBackgroundResource(int r) {}
    public void setPadding(int a, int b, int c, int d) {}
    public void setTag(Object tag) {}
    public Object getTag() { return null; }
    public <T extends View> T findViewById(int id) { return null; }
    public ViewTreeObserver getViewTreeObserver() { return new ViewTreeObserver(); }
    public void setSystemUiVisibility(int flags) {}
    public void setLayoutParams(ViewGroup.LayoutParams params) {}
 public void setBackgroundColor(int color){} }
