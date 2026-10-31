package com.ethk3.novaonepro;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
/** Certificate namespace binding only: no qualification or REAL authorization is issued here. */
public final class R30SelectiveScopeIdentityTest {
    static String h(char c){char[] x=new char[64];Arrays.fill(x,c);return new String(x);}
    static String scope(String candidate,String structure,String model,String selector){return CertifiedPredictionR974.exactScopePolicyHash(candidate,structure,model,selector);}
    static void rejects(Runnable action){try{action.run();fail("Invalid scope accepted");}catch(IllegalArgumentException|IllegalStateException expected){}}
    @Test public void originalR958VectorAndIdentityFenceRemainExact(){
        String candidate="R958-F-"+h('a');String expected="3ad9a64affed3c557cd4b3bf4526dcbb72d3c164286a63d2815f63dbe1c38863";
        assertEquals(expected,CalibrationRuntimeIdentityF04.exactScope(candidate,h('1'),h('2'),h('3')));
        assertEquals(expected,scope(candidate,h('1'),h('2'),h('3')));
        rejects(()->CalibrationRuntimeIdentityF04.exactScope("SU1-F-"+h('a'),h('1'),h('2'),h('3')));
        rejects(()->CalibrationRuntimeIdentityF04.exactScope("SU2-F-"+h('a'),h('1'),h('2'),h('3')));
    }
    @Test public void selectiveNamespacesCannotReplayEachOtherOrTheR958Scope(){
        Set<String> distinct=new HashSet<>();for(String prefix:new String[]{"R958-F-","SU1-F-","SU2-F-"}){
            String digest=scope(prefix+h('a'),h('1'),h('2'),h('3'));assertTrue(digest.matches("[0-9a-f]{64}"));assertTrue(distinct.add(digest));
        }
        assertEquals(3,distinct.size());
    }
    @Test public void everySelectiveIdentityFieldIsBound(){
        for(String prefix:new String[]{"SU1-F-","SU2-F-"}){
            String original=scope(prefix+h('a'),h('1'),h('2'),h('3'));
            assertNotEquals(original,scope(prefix+h('b'),h('1'),h('2'),h('3')));
            assertNotEquals(original,scope(prefix+h('a'),h('4'),h('2'),h('3')));
            assertNotEquals(original,scope(prefix+h('a'),h('1'),h('5'),h('3')));
            assertNotEquals(original,scope(prefix+h('a'),h('1'),h('2'),h('6')));
        }
    }
    @Test public void absentMalformedOrUnrecognizedIdentitiesNeverGainAScope(){
        for(String id:new String[]{null,"","R27:"+h('a'),"SU2-F-","SU1-F-not-a-hash"})rejects(()->scope(id,h('1'),h('2'),h('3')));
        for(String prefix:new String[]{"SU1-F-","SU2-F-"}){
            rejects(()->scope(prefix+h('a'),null,h('2'),h('3')));
            rejects(()->scope(prefix+h('a'),h('1'),"bad-model",h('3')));
            rejects(()->scope(prefix+h('a'),h('1'),h('2'),h('Z')));
        }
        rejects(()->CalibrationRuntimeIdentityF04.selectiveCertificateScopeR30("R958-F-"+h('a'),h('1'),h('2'),h('3')));
    }
}
