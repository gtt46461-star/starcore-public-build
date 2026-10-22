package android.text;

public final class TextUtils {
    private TextUtils() {}
    public enum TruncateAt { END }
    public static boolean equals(CharSequence a, CharSequence b) {
        return a == b || (a != null && a.toString().contentEquals(b));
    }
}
