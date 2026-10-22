package com.ethk3.novaonepro;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public final class R36RuntimeChainTest {
    @Test public void immutableRedlinesRemainLocked(){
        assertEquals("Binance1-PK10",V51BuildManifest.GAME_CODE);
        assertEquals(1200,K3ActionSpaceEngine.TOTAL_ACTIONS);
        assertEquals(120,K3ActionSpaceEngine.COMBINATIONS_PER_POSITION);
        assertFalse(V51BuildManifest.MODEL_PROVEN);
        // Current releases admit individually qualified candidates through the
        // sole DecisionGate, without granting the transport bridge BET rights.
        assertFalse(V51BuildManifest.SHADOW_ONLY);
        assertFalse(V51BuildManifest.REAL_BET_HARD_DISABLED);
        assertFalse(AppConfig.R36_REAL_BET_UNLOCK);
        assertFalse(V51AutoFetchBridge.canCreateBet());
        assertFalse(V51AutoFetchBridge.canSettle());
    }

    @Test public void exactIssueContractIsStable(){
        assertTrue(TargetIssueResolver.exact("202608101440","202608110001").valid);
        assertFalse(TargetIssueResolver.exact("202608101440","202608101441").valid);
    }
}
