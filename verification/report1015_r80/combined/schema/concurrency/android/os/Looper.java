package android.os;
/** Host API adapter only: opt-in main-thread identity for guard regression. */
public final class Looper {
 private static final Looper MAIN=new Looper();
 private static final ThreadLocal<Boolean> IS_MAIN=new ThreadLocal<>();
 public static Looper getMainLooper(){return MAIN;}
 public static Looper myLooper(){return Boolean.TRUE.equals(IS_MAIN.get())?MAIN:null;}
 public static void setMainForTest(boolean enabled){if(enabled)IS_MAIN.set(true);else IS_MAIN.remove();}
}
