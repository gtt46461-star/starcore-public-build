package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;
import com.ethk3.novaonepro.MatrixSnapshotFeedbackR930;
import java.util.*;
import org.junit.Test;

public class R30MatrixRegistryTest {
    @Test public void twentyObjectsCoverEveryNativeKindAndExistingLearnedCheckpoint(){
        List<MatrixRegistryR30.Definition> rows=MatrixRegistryR30.all();assertEquals(20,rows.size());Set<String> ids=new HashSet<>();
        for(MatrixRegistryR30.Definition d:rows){assertTrue(ids.add(d.id));assertFalse(d.producerClass.isEmpty());assertFalse(d.consumerClass.isEmpty());assertFalse(d.table.isEmpty());}
        for(MatrixSnapshotFeedbackR930.MatrixKind kind:MatrixSnapshotFeedbackR930.MatrixKind.values())assertEquals(kind.name(),MatrixRegistryR30.require(MatrixSnapshotFeedbackR930.registryId(kind)).kind);
        assertEquals(19,MatrixSnapshotFeedbackR930.MatrixKind.values().length);assertEquals("SU2",MatrixRegistryR30.require(MatrixRegistryR30.SU2_ID).namespace);
        try{rows.clear();fail("mutable registry");}catch(UnsupportedOperationException expected){}
    }
    @Test public void producerAndConsumerMethodsActuallyExistWithoutStartingModels(){
        for(MatrixRegistryR30.Definition d:MatrixRegistryR30.all()){
            assertTrue(d.producerClass+"#"+d.producerMethod,MatrixRegistryR30.declared(d.producerClass,d.producerMethod));
            for(String method:d.consumerMethods)assertTrue(d.consumerClass+"#"+method,MatrixRegistryR30.declared(d.consumerClass,method));
        }
        assertFalse(MatrixRegistryR30.declared(MatrixRegistryR30.require(MatrixRegistryR30.SU2_ID).producerClass,"madeUpProducer"));
    }
}
