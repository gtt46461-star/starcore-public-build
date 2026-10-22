package com.ethk3.novaonepro;

import android.content.Context;

/** Bounded read presenter. Each failed section retains its own visible error. */
public final class OperationalDashboardR925 {
    private OperationalDashboardR925() {}
    public static String[] capture(Context context,DbStore store){
        String[] result=new String[3];
        for(int i=0;i<3;i++)try{
            if(store==null)throw new IllegalStateException("DB_NOT_READY");
            if(i==0){
                BackgroundRuntimeCapabilityR811.Snapshot p=BackgroundRuntimeCapabilityR811.snapshot(context);
                result[i]="后台能力 · 点击设置（能力齐备不等于息屏已验收）\n电池无限制="+(p.ignoringBatteryOptimizations?"是":"否")
                    +"｜精确闹钟="+(p.exactAlarmRuntimeGranted?"是":"否")+"\n状态="+p.status
                    +"\n厂商自启动：需在手机设置核对；24小时后台：待设备验证\n"
                    +AutoFetchService.b13Diagnostics(store)+"\n"+RuntimeObservabilityR925.epochs(store);
            }else if(i==1)result[i]=RuntimeObservabilityR925.shadow(store);
            else result[i]="最近20期：抓取 → 请求 → 出号 → 结算 → FULL\n"+RuntimeObservabilityR925.pipeline(store,20,false)+"\n"+BacklogConvergenceR925.report(store);
        }catch(Throwable failure){result[i]="读取失败："+failure.getClass().getSimpleName()+"｜"+failure.getMessage();}
        return result;
    }
}
