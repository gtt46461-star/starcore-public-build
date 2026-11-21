package com.ethk3.novaonepro.strategyv2;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

public final class MatrixReportReferenceR335Test {
    private static Map<String,Object> values(Object... pairs) {
        Map<String,Object> result=new LinkedHashMap<>();
        for(int i=0;i<pairs.length;i+=2)result.put((String)pairs[i],pairs[i+1]);
        return result;
    }

    @Test public void exportedMatrixReferenceIncludesItsPersistedOwnerTrace() {
        String commit="0123456789abcdef0123456789abcdef01234567";
        Map<String,Object> watermark=values("sourceCommit",commit,"factsReadAt",12345L);
        Map<String,Object> task=values("sourceTable","r26_task","task_id","r27-matrices-r1",
                "status","BLOCKED","last_error","WAIT_FOR_INPUT","blocked_reason","WAIT_FOR_INPUT");
        Map<String,Object> data=values("tasks",Collections.singletonList(task),"r31RuntimeFaults",Collections.emptyList(),
                "capabilities",Collections.singletonList(values("id","MATRIX:R930:EVIDENCE_TEACHER",
                        "implemented",true,"executable",true)));
        data.put("capturedFaultSourcesR335",DiagnosisTraceReaderR33.captureSingleSelect(watermark,data,null));
        Map<String,Object> index=ReportObjectIndexR30.buildProductionR31(Collections.emptyMap(),watermark,data);
        Map<String,Object> document=values("referenceFormat",ReportEvidenceProjectionR33.FORMAT,
                "snapshotWatermark",watermark,"objectIndex",index);
        Map<String,Object> matrix=null;
        for(Object item:(List<?>)((Map<?,?>)index.get("matrices")).get("objects")) {
            Map<String,Object> row=ReportObjectIndexR30.map(item);
            if("R930:EVIDENCE_TEACHER".equals(row.get("objectId")))matrix=row;
        }
        assertNotNull(matrix);
        assertTrue("The originally persisted owner trace must be present",matrix.containsKey("verifiedFaultTraceR33"));
        Map<?,?> alias=(Map<?,?>)((List<?>)((Map<?,?>)index.get("capabilities")).get("objects")).get(0);
        assertNotNull(((Map<?,?>)alias.get("runtimeEvidenceR33")).get("canonicalReference"));
        ReportEvidenceProjectionR33.validateDocument(document);
        Map<String,Object> reopened=ReportObjectIndexR30.map(JsonR27.parse(JsonR27.write(document)));
        ReportEvidenceProjectionR33.validateDocument(reopened);
        matrix.put("verifiedFaultTraceR33",values("changedAfterFreeze",true));
        IllegalStateException error=assertThrows(IllegalStateException.class,
                ()->ReportEvidenceProjectionR33.validateDocument(document));
        assertTrue(error.getMessage(),error.getMessage().contains("R33_REFERENCE_HASH_MISMATCH"));
    }
}
