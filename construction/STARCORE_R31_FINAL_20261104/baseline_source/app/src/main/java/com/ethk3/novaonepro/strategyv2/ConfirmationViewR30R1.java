package com.ethk3.novaonepro.strategyv2;

/** verified=false is not necessarily a malfunction: a completed negative control comparison is valid evidence. */
public final class ConfirmationViewR30R1 {
    private ConfirmationViewR30R1(){}
    public static String state(String taskState,String resultStatus,boolean verified,long registeredAt){
        if("PENDING".equals(taskState))return registeredAt>0?"DATA_CONFLICT_REGISTERED_WITHOUT_DONE":"PENDING_COMPUTATION";
        if(!"DONE".equals(taskState))return "UNKNOWN_OR_FAILED_COMPUTATION";
        if(registeredAt>0&&!verified)return "DATA_CONFLICT_REGISTERED_NEGATIVE_RESULT";
        if(!"COMPLETED".equals(resultStatus))return "COMPLETED_WITH_INSUFFICIENT_EVIDENCE";
        if(!verified)return "COMPLETED_NO_VERIFIED_IMPROVEMENT";
        return registeredAt>0?"VERIFIED_AND_REGISTERED":"VERIFIED_WAITING_REGISTRATION";
    }
}
