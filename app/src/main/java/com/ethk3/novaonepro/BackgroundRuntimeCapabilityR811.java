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

    public enum ObservationR31 { YES, NO, UNKNOWN, UNSUPPORTED }
    private interface ReadFlagR31 { boolean read() throws Exception; }
    private static void recordProbeFailureR31(Throwable failure,String source,java.util.Map<String,String> errors){Error fatal=com.ethk3.novaonepro.strategyv2.FailureDispositionR31.processFatalCause(failure);if(fatal!=null)throw fatal;errors.put(source,failure.getClass().getSimpleName());}
    private static ObservationR31 observeR31(boolean supported,ReadFlagR31 getter,String source,java.util.Map<String,String> errors){
        if(!supported)return ObservationR31.UNSUPPORTED;
        try{return getter.read()?ObservationR31.YES:ObservationR31.NO;}
        catch(Exception failure){recordProbeFailureR31(failure,source,errors);return ObservationR31.UNKNOWN;}
    }
    /** Called by the existing UI/report IO owner, never performs a business/database query. */
    public static RuntimeObservationR31 observeRuntimeR31(Context context){
        long wall=System.currentTimeMillis(),mono=android.os.SystemClock.elapsedRealtime();
        java.util.Map<String,String> errors=new java.util.LinkedHashMap<>();
        Context app=context==null?null:context.getApplicationContext();if(app==null)app=context;
        if(app==null)return new RuntimeObservationR31(wall,mono,-1,Build.VERSION.SDK_INT,null,ObservationR31.UNKNOWN,ObservationR31.UNKNOWN,ObservationR31.UNKNOWN,ObservationR31.UNKNOWN,ObservationR31.UNKNOWN,ObservationR31.UNKNOWN,ObservationR31.UNKNOWN,java.util.Collections.singletonMap("context","CONTEXT_NULL"));
        final Context owner=app;final int sdk=Build.VERSION.SDK_INT;int target=-1;Boolean enabled=null;
        try{target=owner.getApplicationInfo().targetSdkVersion;}catch(Exception failure){recordProbeFailureR31(failure,"targetSdk",errors);}
        try{enabled=new ServiceHeartbeatStore(owner).isUserEnabled();}catch(Exception failure){recordProbeFailureR31(failure,"userSession",errors);}
        ObservationR31 battery=observeR31(sdk>=23,()->{PowerManager pm=(PowerManager)owner.getSystemService(Context.POWER_SERVICE);if(pm==null)throw new IllegalStateException("POWER_SERVICE_MISSING");return pm.isIgnoringBatteryOptimizations(owner.getPackageName());},"PowerManager.isIgnoringBatteryOptimizations",errors);
        ObservationR31 idle=observeR31(sdk>=23,()->{PowerManager pm=(PowerManager)owner.getSystemService(Context.POWER_SERVICE);if(pm==null)throw new IllegalStateException("POWER_SERVICE_MISSING");return pm.isDeviceIdleMode();},"PowerManager.isDeviceIdleMode",errors);
        ObservationR31 saver=observeR31(sdk>=21,()->{PowerManager pm=(PowerManager)owner.getSystemService(Context.POWER_SERVICE);if(pm==null)throw new IllegalStateException("POWER_SERVICE_MISSING");return pm.isPowerSaveMode();},"PowerManager.isPowerSaveMode",errors);
        ObservationR31 restricted=observeR31(sdk>=28,()->{android.app.ActivityManager am=(android.app.ActivityManager)owner.getSystemService(Context.ACTIVITY_SERVICE);if(am==null)throw new IllegalStateException("ACTIVITY_SERVICE_MISSING");return am.isBackgroundRestricted();},"ActivityManager.isBackgroundRestricted",errors);
        ObservationR31 notifications=observeR31(sdk>=24,()->{android.app.NotificationManager nm=(android.app.NotificationManager)owner.getSystemService(Context.NOTIFICATION_SERVICE);if(nm==null)throw new IllegalStateException("NOTIFICATION_SERVICE_MISSING");return nm.areNotificationsEnabled();},"NotificationManager.areNotificationsEnabled",errors);
        ObservationR31 alarm=observeR31(sdk>=31,()->{AlarmManager am=(AlarmManager)owner.getSystemService(Context.ALARM_SERVICE);if(am==null)throw new IllegalStateException("ALARM_SERVICE_MISSING");return am.canScheduleExactAlarms();},"AlarmManager.canScheduleExactAlarms",errors);
        ObservationR31 foreground=observeR31(true,()->{android.app.ActivityManager am=(android.app.ActivityManager)owner.getSystemService(Context.ACTIVITY_SERVICE);if(am==null)throw new IllegalStateException("ACTIVITY_SERVICE_MISSING");java.util.List<android.app.ActivityManager.RunningServiceInfo> services=am.getRunningServices(32);if(services==null)throw new IllegalStateException("RUNNING_SERVICES_UNKNOWN");int count=0;for(android.app.ActivityManager.RunningServiceInfo service:services){if(count++==32)break;if(service.service!=null&&"com.ethk3.novaonepro.AutoFetchService".equals(service.service.getClassName()))return service.foreground;}if(services.size()>=32)throw new IllegalStateException("BOUNDED_SERVICE_LIST_TRUNCATED");return false;},"ActivityManager.getRunningServices(own_service,max32)",errors);
        return new RuntimeObservationR31(wall,mono,target,sdk,enabled,battery,alarm,notifications,idle,saver,restricted,foreground,errors);
    }

    /** Pure mapping: absence of a whitelist is setup debt, not proof that all work stopped. */
    public static String operatingStatusR31(Boolean enabled,ObservationR31 battery,ObservationR31 alarm,ObservationR31 notifications,ObservationR31 idle,ObservationR31 saver,ObservationR31 restricted,ObservationR31 foreground){
        if(Boolean.FALSE.equals(enabled))return "USER_DISABLED";
        if(restricted==ObservationR31.YES||idle==ObservationR31.YES&&battery==ObservationR31.NO)return "OS_RESTRICTED";
        if(enabled==null||battery==ObservationR31.UNKNOWN||alarm==ObservationR31.UNKNOWN||notifications==ObservationR31.UNKNOWN||idle==ObservationR31.UNKNOWN||saver==ObservationR31.UNKNOWN||restricted==ObservationR31.UNKNOWN||foreground==ObservationR31.UNKNOWN)return "UNKNOWN";
        if(battery==ObservationR31.NO||alarm==ObservationR31.NO||notifications==ObservationR31.NO||saver==ObservationR31.YES||foreground==ObservationR31.NO)return "DEGRADED";
        return "CAPABILITIES_READY_DEVICE_SOAK_NOT_VERIFIED";
    }

    public static final class RuntimeObservationR31 {
        public final long sampleWallMs,sampleElapsedMs;public final int targetSdk,runtimeSdk;public final Boolean userEnabled;
        public final ObservationR31 batteryExemption,exactAlarm,notifications,deviceIdle,powerSave,backgroundRestricted,foregroundService;
        public final String status,reason,recoveryCondition,snapshotId;public final java.util.Map<String,String> errors;
        RuntimeObservationR31(long wall,long mono,int target,int runtime,Boolean enabled,ObservationR31 battery,ObservationR31 alarm,ObservationR31 notifications,ObservationR31 idle,ObservationR31 saver,ObservationR31 restricted,ObservationR31 foreground,java.util.Map<String,String> errors){
            sampleWallMs=wall;sampleElapsedMs=mono;targetSdk=target;runtimeSdk=runtime;userEnabled=enabled;batteryExemption=battery;exactAlarm=alarm;this.notifications=notifications;deviceIdle=idle;powerSave=saver;backgroundRestricted=restricted;foregroundService=foreground;this.errors=java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(errors));status=operatingStatusR31(enabled,battery,alarm,notifications,idle,saver,restricted,foreground);
            reason="USER_DISABLED".equals(status)?"用户已关闭连续会话":"OS_RESTRICTED".equals(status)?restricted==ObservationR31.YES?"系统报告应用后台受限":"设备处于Doze且应用未获电池优化豁免":"UNKNOWN".equals(status)?"一个或多个系统状态未能读取；未知不等于已授权或运行正常":"DEGRADED".equals(status)?"存在配置缺口、省电模式或未观察到前台服务；详见各项实际值":"可核实能力当前具备；持续运行效果尚未验收";
            recoveryCondition="USER_DISABLED".equals(status)?"由用户再次启动会话":"OS_RESTRICTED".equals(status)?"系统退出限制/Doze，或用户在系统设置允许后台与电池使用；恢复后必须观察真实新产物":"UNKNOWN".equals(status)?"刷新系统观察；确认系统服务可用，保留失败原因后重试":"DEGRADED".equals(status)?"按实际缺口开启通知/精确闹钟、调整电池设置或启动连续会话；能力恢复后仍需实测":"保持用户会话并执行设备持续运行验收";
            snapshotId=V51ChecksumEngine.sha256(wall+"|"+mono+"|"+target+"|"+runtime+"|"+enabled+"|"+battery+"|"+alarm+"|"+notifications+"|"+idle+"|"+saver+"|"+restricted+"|"+foreground+"|"+this.errors);
        }
        public String userSummary(){return "后台状态="+status+"\n原因="+reason+"\n恢复条件="+recoveryCondition+"\n用户会话="+userEnabled+"｜前台服务(OS观察)="+foregroundService+"\nDoze="+deviceIdle+"｜省电模式="+powerSave+"｜后台受限="+backgroundRestricted+"\n电池豁免="+batteryExemption+"｜通知="+notifications+"｜精确闹钟="+exactAlarm+"\n系统API="+runtimeSdk+"｜targetSdk="+targetSdk+"（当前保留28/dataSync；target≥35需另验后台时限）\nOEM自启动=未核实（设置页可打开不等于授权）\n连续24小时=NOT_RUN｜72小时=NOT_RUN（本页面没有已签收的设备证据）\nforce-stop后系统不会保证自启，需用户再次交互；不会补造停止期间赛前预测\n来源=Android系统getter与用户会话配置；sampleWallMs="+sampleWallMs+"｜sampleElapsedMs="+sampleElapsedMs+"\nsnapshotId="+snapshotId+"\n读取缺口="+errors;}
    }

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
            Error fatal=com.ethk3.novaonepro.strategyv2.FailureDispositionR31.processFatalCause(e);if(fatal!=null)throw fatal;
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
