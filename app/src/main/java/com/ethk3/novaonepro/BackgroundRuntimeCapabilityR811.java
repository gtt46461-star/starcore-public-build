package com.ethk3.novaonepro;

import android.app.AlarmManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.PowerManager;

/** Runtime-observed background capability; manifest declaration is not treated as a grant. */
public final class BackgroundRuntimeCapabilityR811 {
    private BackgroundRuntimeCapabilityR811() {}

    public static boolean deviceCertificateInvalidated(
            int certifiedTargetSdk, int currentTargetSdk) {
        return certifiedTargetSdk <= 0
                || currentTargetSdk <= 0
                || certifiedTargetSdk != currentTargetSdk;
    }

    public static Snapshot snapshot(Context context) {
        if (context == null) return Snapshot.error("CONTEXT_NULL");
        try {
            Context app = context.getApplicationContext() == null ? context : context.getApplicationContext();
            ApplicationInfo info = app.getApplicationInfo();
            int target = info == null ? -1 : info.targetSdkVersion;
            boolean battery = Build.VERSION.SDK_INT < 23;
            if (Build.VERSION.SDK_INT >= 23) {
                PowerManager power = (PowerManager) app.getSystemService(Context.POWER_SERVICE);
                battery = power != null && power.isIgnoringBatteryOptimizations(app.getPackageName());
            }
            boolean exact = Build.VERSION.SDK_INT < 31;
            if (Build.VERSION.SDK_INT >= 31) {
                AlarmManager alarm = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
                exact = alarm != null && alarm.canScheduleExactAlarms();
            }
            PackageInfo pkg = app.getPackageManager().getPackageInfo(app.getPackageName(), PackageManager.GET_PERMISSIONS);
            String[] requested = pkg == null ? null : pkg.requestedPermissions;
            int[] flags = pkg == null ? null : pkg.requestedPermissionsFlags;
            int declared = requested == null ? 0 : requested.length, granted = 0;
            if (flags != null) for (int i = 0; i < flags.length; i++) if ((flags[i] & PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0) granted++;
            boolean notifications = Build.VERSION.SDK_INT < 24
                    || ((android.app.NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE)).areNotificationsEnabled();
            return new Snapshot(36, target, Build.VERSION.SDK_INT, battery, exact, declared, granted,
                    capabilityStatus(battery, exact, notifications));
        } catch (Throwable e) {
            return Snapshot.error(e.getClass().getSimpleName());
        }
    }

    public static String capabilityStatus(boolean battery, boolean exact, boolean notifications) {
        if (!battery) return "SETUP_REQUIRED_BATTERY_RESTRICTED";
        if (!notifications) return "SETUP_REQUIRED_NOTIFICATIONS_DISABLED";
        if (!exact) return "RECOVERY_DEGRADED_EXACT_ALARM_DENIED";
        return "CAPABILITIES_READY_DEVICE_SOAK_NOT_VERIFIED";
    }

    public static final class Snapshot {
        public final int compileSdk, targetSdk, runtimeSdk, declaredPermissions, grantedPermissions;
        public final boolean ignoringBatteryOptimizations, exactAlarmRuntimeGranted;
        public final String status;
        Snapshot(int compile, int target, int runtime, boolean battery, boolean alarm, int declared, int granted, String status) {
            compileSdk=compile;targetSdk=target;runtimeSdk=runtime;ignoringBatteryOptimizations=battery;exactAlarmRuntimeGranted=alarm;declaredPermissions=declared;grantedPermissions=granted;this.status=status;
        }
        static Snapshot error(String code) { return new Snapshot(36,-1,Build.VERSION.SDK_INT,false,false,0,0,"READ_ERROR:"+code); }
        public boolean manifestMatchesBuild() { return compileSdk == 36 && targetSdk == 28; }
        public String certificateFingerprint() {
            return V51ChecksumEngine.sha256(
                    compileSdk
                            + "|"
                            + targetSdk
                            + "|"
                            + runtimeSdk
                            + "|"
                            + ignoringBatteryOptimizations
                            + "|"
                            + exactAlarmRuntimeGranted
                            + "|"
                            + declaredPermissions
                            + "|"
                            + grantedPermissions
                            + "|"
                            + status);
        }
    }
}
