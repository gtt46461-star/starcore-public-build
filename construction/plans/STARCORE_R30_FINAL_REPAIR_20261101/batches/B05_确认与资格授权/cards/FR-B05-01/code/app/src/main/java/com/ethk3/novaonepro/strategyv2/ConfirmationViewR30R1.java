package com.ethk3.novaonepro.strategyv2;

/** verified=false is not necessarily a malfunction: a completed negative control comparison is valid evidence. */
public final class ConfirmationViewR30R1 {
    private ConfirmationViewR30R1(){}
    public static String state(String taskState,String resultStatus,boolean verified,long registeredAt){
        if(!"DONE".equals(taskState))return "PENDING_COMPUTATION";
        if(!"COMPLETED".equals(resultStatus))return "COMPLETED_WITH_INSUFFICIENT_EVIDENCE";
        if(!verified)return "COMPLETED_NO_VERIFIED_IMPROVEMENT";
        return registeredAt>0?"VERIFIED_AND_REGISTERED":"VERIFIED_WAITING_REGISTRATION";
    }
}
