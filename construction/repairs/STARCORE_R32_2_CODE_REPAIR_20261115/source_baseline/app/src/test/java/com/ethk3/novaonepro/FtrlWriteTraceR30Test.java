package com.ethk3.novaonepro;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public final class FtrlWriteTraceR30Test {
    static long observed(){return ((Number)FtrlWriteTraceR30.snapshot().get("observedTransactions")).longValue();}
    @SuppressWarnings("unchecked") @Test public void publishesAfterTransactionScopeAndRetainsNegativeOutcome(){
        long before=observed();try(FtrlWriteTraceR30.Trace trace=FtrlWriteTraceR30.open("SYNTHETIC_ISSUE","SYNTHETIC_LEARNING_ID",1234)){
            long start=FtrlWriteTraceR30.start();FtrlWriteTraceR30.end(FtrlWriteTraceR30.Phase.ARTIFACT_INSERT,start);trace.outcome("ROLLED_BACK_OR_REJECTED");assertEquals(before,observed());
            try(FtrlWriteTraceR30.Trace nested=FtrlWriteTraceR30.open("NESTED","NESTED",0)){assertEquals(before,observed());}assertEquals(before,observed());
        }assertEquals(before+1,observed());Map<String,Object> snapshot=FtrlWriteTraceR30.snapshot();List<Map<String,Object>> rows=(List<Map<String,Object>>)snapshot.get("recent");Map<String,Object> row=rows.get(rows.size()-1);assertEquals("ROLLED_BACK_OR_REJECTED",row.get("outcome"));assertEquals(1234L,row.get("encodedBytes"));Map<?,?> phase=(Map<?,?>)((Map<?,?>)row.get("phases")).get("ARTIFACT_INSERT");assertEquals(1L,phase.get("calls"));assertTrue(((Number)phase.get("totalNanos")).longValue()>=0);
        row.put("outcome","UNAUTHORIZED_MUTATION");assertFalse(FtrlWriteTraceR30.snapshot().toString().contains("UNAUTHORIZED_MUTATION"));
    }
    @Test public void boundedObservationNeverBecomesAnAllTimeOrSloClaim(){for(int i=0;i<8;i++)try(FtrlWriteTraceR30.Trace trace=FtrlWriteTraceR30.open("SYNTHETIC_"+i,"SYNTHETIC",i)){trace.outcome("COMMITTED");}Map<String,Object> snapshot=FtrlWriteTraceR30.snapshot();assertEquals(5L,((Number)snapshot.get("shown")).longValue());assertTrue(((Number)snapshot.get("omitted")).longValue()>=3);assertTrue(snapshot.get("scope").toString().contains("NOT_24H_SLO"));}
}
