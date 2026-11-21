package com.ethk3.novaonepro;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Validates the original mainline SHADOW/OOS row; never grants independent Future or REAL authority. */
public final class FrozenFeedbackProofR335 {
    private FrozenFeedbackProofR335(){}
    public static final String RESIDUAL_CONSUMER = "R335_FROZEN_RESIDUAL_V3";
    public static boolean observedLabelTime(long opened, long seen, long settled) {
        return opened > 0L && seen > 0L && settled >= Math.max(opened, seen);
    }
    public static void verify(String target,String base,String historyMax,long committed,long deadline,
            long opened,long firstSeen,int position,int[] picks,int[] draw,int actual,int hit,
            String frozen,String snapshot,String action,String history,String drawHash,String provenance,String label){
        if(target==null||base==null||historyMax==null||!target.matches("[0-9]{12}")
                ||!base.matches("[0-9]{12}")||!base.equals(historyMax)||base.compareTo(target)>=0)
            throw new IllegalArgumentException("R335_FEEDBACK_PIT_IDENTITY");
        if(committed<=0||deadline<=0||opened<=0||firstSeen<=0||committed>=deadline
                ||committed>=opened||committed>=firstSeen)
            throw new IllegalArgumentException("R335_FEEDBACK_FIXED_DEADLINE");
        if(position<1||position>10||picks==null||picks.length!=3||draw==null||draw.length!=10)
            throw new IllegalArgumentException("R335_FEEDBACK_ACTION_SHAPE");
        int seen=0,mask=0;
        for(int n:draw){if(n<1||n>10||(seen&(1<<n))!=0)throw new IllegalArgumentException("R335_FEEDBACK_DRAW_PERMUTATION");seen|=1<<n;}
        for(int n:picks){if(n<1||n>10||(mask&(1<<n))!=0)throw new IllegalArgumentException("R335_FEEDBACK_K3");mask|=1<<n;}
        if(actual!=draw[position-1]||hit!=((mask&(1<<actual))!=0?1:0))
            throw new IllegalArgumentException("R335_FEEDBACK_LABEL");
        for(String value:new String[]{frozen,snapshot,action,history,drawHash,provenance,label})
            if(value==null||!value.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("R335_FEEDBACK_HASH_FORMAT");
        StringBuilder identity=new StringBuilder("R815_DRAW_V2|").append(target);
        for(int n:draw)identity.append('|').append(n);
        String exactDraw=sha(identity.toString());
        String exactProvenance=sha("R36_OOS_PROVENANCE|"+frozen+'|'+snapshot+'|'+action+'|'+history+'|'+exactDraw+'|'+firstSeen);
        if(!exactDraw.equals(drawHash)||!exactProvenance.equals(provenance)
                ||!sha(exactProvenance+'|'+actual+'|'+hit).equals(label))
            throw new IllegalArgumentException("R335_FEEDBACK_SOURCE_HASH");
    }
    public static String sha(String text){
        try{byte[] bytes=MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex=new StringBuilder(64);for(byte b:bytes){int n=b&255;hex.append("0123456789abcdef".charAt(n>>>4)).append("0123456789abcdef".charAt(n&15));}return hex.toString();
        }catch(java.security.NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
    }
}
