package android.view;

public class ViewGroup extends View {
    public void addView(View view) {}
    public void addView(View view, int index) {}
    public void addView(View view, LayoutParams params) {}
    public int getChildCount() { return 0; }
    public View getChildAt(int index) { return null; }
    public void removeViewAt(int index) {}
    public void removeAllViews() {}

    public static class LayoutParams {
        public static final int MATCH_PARENT = -1, WRAP_CONTENT = -2;
        public int width, height;
        public LayoutParams(int width, int height) {
            this.width = width;
            this.height = height;
        }
    }
}
