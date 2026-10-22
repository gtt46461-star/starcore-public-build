package android.view;

public interface WindowManager {
    class LayoutParams {
        public static final int SOFT_INPUT_ADJUST_RESIZE = 0x10;
        public static final int FLAG_TRANSLUCENT_STATUS = 0x04000000;
        public static final int FLAG_TRANSLUCENT_NAVIGATION = 0x08000000;
        public static final int FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS = 0x80000000;
    }
}
