package android.app;

import android.content.Context;
import android.content.Intent;

/** API-28 constant values and method descriptors. */
public class PendingIntent {
    public static final int FLAG_ONE_SHOT = 1 << 30;
    public static final int FLAG_NO_CREATE = 1 << 29;
    public static final int FLAG_CANCEL_CURRENT = 1 << 28;
    public static final int FLAG_UPDATE_CURRENT = 1 << 27;
    public static final int FLAG_IMMUTABLE = 1 << 26;

    public void cancel() {}
    public static PendingIntent getActivity(Context context, int requestCode, Intent intent, int flags) { return new PendingIntent(); }
    public static PendingIntent getService(Context context, int requestCode, Intent intent, int flags) { return new PendingIntent(); }
    public static PendingIntent getForegroundService(Context context, int requestCode, Intent intent, int flags) { return new PendingIntent(); }
    public static PendingIntent getBroadcast(Context context, int requestCode, Intent intent, int flags) { return new PendingIntent(); }
}
