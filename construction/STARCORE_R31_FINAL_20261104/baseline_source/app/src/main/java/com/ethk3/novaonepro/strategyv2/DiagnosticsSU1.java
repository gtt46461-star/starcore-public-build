package com.ethk3.novaonepro.strategyv2;
import java.util.*;

public final class DiagnosticsSU1 {
    private DiagnosticsSU1() {}
    public static String cause(Throwable failure,int limit){
        if(limit<32||limit>8192)throw new IllegalArgumentException("SU1_DIAGNOSTIC_LIMIT");StringBuilder text=new StringBuilder();Set<Throwable> seen=Collections.newSetFromMap(new IdentityHashMap<>());
        for(Throwable t=failure;t!=null&&seen.add(t)&&seen.size()<=8;t=t.getCause()){if(text.length()>0)text.append(" <- ");text.append(t.getClass().getSimpleName()).append(':').append(t.getMessage());if(text.length()>=limit)break;}
        return text.substring(0,Math.min(text.length(),limit));
    }
}
