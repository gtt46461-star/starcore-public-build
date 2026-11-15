package com.ethk3.novaonepro.strategyv2;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public final class R30DecisionTraceTest {
    @Test public void everyRequiredFactIndependentlyBlocksAndUnknownIsPreserved()throws Exception{
        for(String name:DecisionChainR30.ORDER){DecisionChainR30.Facts facts=R30DecisionChainTest.allPass();facts.getClass().getField(name).set(facts,false);DecisionChainR30.Verdict verdict=DecisionChainR30.explain(facts);assertFalse(verdict.allowReal);assertEquals(name+":FAIL",verdict.firstBlocker);facts.getClass().getField(name).set(facts,null);assertEquals("UNKNOWN",DecisionChainR30.explain(facts).items.get(name));}
    }
    @Test public void mismatchesKeepTheirActualDirectionAndReceiptIdentity(){
        DecisionChainR30.Facts facts=R30DecisionChainTest.allPass();Map<String,Object> sources=Collections.singletonMap("scope","SYNTHETIC_MECHANISM_TEST");
        DecisionExplanationStoreR30.Observation stricter=DecisionExplanationStoreR30.observe("202610310002","V51_B12","contract",false,"MODEL_NOT_PROVEN",facts,sources,1);
        assertEquals("ORIGINAL_STRICTER",stricter.direction);assertFalse(stricter.originalAllow);assertTrue(stricter.explainAllow);
        assertEquals(stricter.key,DecisionExplanationStoreR30.observe("202610310002","V51_B12","contract",false,"MODEL_NOT_PROVEN",facts,sources,2).key);
        facts.userRealEnabled=null;
        DecisionExplanationStoreR30.Observation unsafe=DecisionExplanationStoreR30.observe("202610310002","V51_B12","contract",true,"NONE",facts,sources,3);
        assertEquals("ORIGINAL_MORE_PERMISSIVE",unsafe.direction);assertTrue(unsafe.originalAllow);assertFalse(unsafe.explainAllow);assertEquals("userRealEnabled:UNKNOWN",unsafe.firstBlocker);assertNotEquals(stricter.key,unsafe.key);
    }
}
