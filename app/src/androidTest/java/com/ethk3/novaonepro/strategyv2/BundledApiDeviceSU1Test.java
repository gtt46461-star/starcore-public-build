package com.ethk3.novaonepro.strategyv2;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.ethk3.novaonepro.BinanceGameContract;
import com.ethk3.novaonepro.BuildConfig;
import com.ethk3.novaonepro.KeyProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Runs in a fresh app sandbox; no manual entry or network response is needed to inspect packaging. */
@RunWith(AndroidJUnit4.class)
public final class BundledApiDeviceSU1Test {
    @Test public void endpointsAndBuildCredentialAreActuallyPackaged() {
        assertEquals("https://api.duoqu.online/data", BinanceGameContract.primary().endpoint());
        assertEquals("https://api1.duoqu.online/data", BinanceGameContract.backup().endpoint());
        assertEquals("Binance1-PK10", BinanceGameContract.GAME_CODE);
        assertEquals(com.ethk3.novaonepro.AppConfig.API_PRIMARY_BASE, BinanceGameContract.PRIMARY_BASE);
        assertEquals(com.ethk3.novaonepro.AppConfig.API_BACKUP_BASE, BinanceGameContract.BACKUP_BASE);
        String visible=com.ethk3.novaonepro.ApiCredentialSettingsCP12.endpointStatus("");
        assertTrue("Main screen hides the bundled primary endpoint",visible.contains(BinanceGameContract.primary().endpoint()));
        assertTrue("Main screen hides the bundled backup endpoint",visible.contains(BinanceGameContract.backup().endpoint()));
        boolean configured = !(BuildConfig.DUOQU_API_KEY_A + BuildConfig.DUOQU_API_KEY_B).trim().isEmpty();
        assertEquals("Bundled credential readiness disagrees with compiled configuration", configured, KeyProvider.isKeyReady());
        if (configured) {
            assertTrue(KeyProvider.usesReleaseDefault());
            assertEquals("AUTHORIZED_LEGACY_RELEASE_DEFAULT", KeyProvider.credentialSource());
            assertFalse(KeyProvider.redact("key=" + KeyProvider.getApiKey()).contains(KeyProvider.getApiKey()));
            com.ethk3.novaonepro.Models.ApiResult result=new com.ethk3.novaonepro.ApiClient().fetchLatest();
            assertNotNull("Bundled API returned no response",result);
            assertTrue("Bundled API did not return current validated data through the actual app client",result.success||result.ok);
            assertNotNull("Bundled API returned no latest draw",result.latestDraw);
            assertTrue("Bundled API returned invalid numbers",result.latestDraw.isValid());
        }
    }
}
