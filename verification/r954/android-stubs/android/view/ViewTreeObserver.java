package android.view;

public class ViewTreeObserver {
    public interface OnScrollChangedListener { void onScrollChanged(); }
    public boolean isAlive() { return true; }
    public void addOnScrollChangedListener(OnScrollChangedListener listener) {}
    public void removeOnScrollChangedListener(OnScrollChangedListener listener) {}
}
