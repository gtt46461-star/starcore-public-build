package android.os;
/** Host schema harness only. The schema worker is deliberately not an Android main thread. */
public final class Looper {
    private static final Looper MAIN = new Looper();
    private Looper() {}
    public static Looper getMainLooper() { return MAIN; }
    public static Looper myLooper() { return null; }
}
