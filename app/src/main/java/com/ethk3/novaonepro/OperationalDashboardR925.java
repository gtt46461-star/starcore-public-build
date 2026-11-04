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
                BackgroundRuntimeCapabilityR811.RuntimeObservationR31 p=BackgroundRuntimeCapabilityR811.observeRuntimeR31(context);
                result[i]="后台状态="+p.status+"\n原因="+p.reason+"\n点击查看系统能力、停止记录与恢复条件\n"+p.userSummary()+"\n"+LifecycleHealthR963.interruptionSummaryR31(context)+"\n"
                    +AutoFetchService.b13Diagnostics(store)+"\n"+RuntimeObservabilityR925.epochs(store);
            }else if(i==1)result[i]=RuntimeObservabilityR925.shadow(store);
            else result[i]="最近20期：抓取 → 请求 → 出号 → 结算 → FULL\n"+RuntimeObservabilityR925.pipeline(store,20,false)+"\n"+BacklogConvergenceR925.report(store);
        }catch(Throwable failure){Error fatal=com.ethk3.novaonepro.strategyv2.FailureDispositionR31.processFatalCause(failure);if(fatal!=null)throw fatal;result[i]="读取失败："+failure.getClass().getSimpleName()+"｜"+failure.getMessage();}
        return result;
    }
}
