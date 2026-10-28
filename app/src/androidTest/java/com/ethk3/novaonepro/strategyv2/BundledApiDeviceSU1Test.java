package com.ethk3.novaonepro.strategyv2;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.ethk3.novaonepro.BinanceGameContract;
import com.ethk3.novaonepro.BuildConfig;
import com.ethk3.novaonepro.KeyProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Fresh-sandbox endpoint packaging and startup UI; configured builds must also fetch validated live data. */
@RunWith(AndroidJUnit4.class)
public final class BundledApiDeviceSU1Test {
    @Test public void endpointsAndBuildCredentialAreActuallyPackaged() throws Exception {
        assertEquals("https://api.duoqu.online/data", BinanceGameContract.primary().endpoint());
        assertEquals("https://api1.duoqu.online/data", BinanceGameContract.backup().endpoint());
        assertEquals("Binance1-PK10", BinanceGameContract.GAME_CODE);
        assertEquals(com.ethk3.novaonepro.AppConfig.API_PRIMARY_BASE, BinanceGameContract.PRIMARY_BASE);
        assertEquals(com.ethk3.novaonepro.AppConfig.API_BACKUP_BASE, BinanceGameContract.BACKUP_BASE);
        String startup=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getTargetContext().getString(com.ethk3.novaonepro.R.string.placeholder_api);
        assertTrue("Startup hides the packaged primary endpoint",startup.contains(BinanceGameContract.primary().endpoint()));
        assertTrue("Startup hides the packaged backup endpoint",startup.contains(BinanceGameContract.backup().endpoint()));
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
            org.json.JSONObject receipt=new org.json.JSONObject().put("scope","PACKAGED_API_REAL_CLIENT_ATTEMPT_NOT_STRATEGY_QUALITY").put("sourceCommit",BuildConfig.SOURCE_COMMIT_R26).put("at",System.currentTimeMillis()).put("configured",true).put("responsePresent",result!=null).put("success",result!=null&&(result.success||result.ok)).put("validatedDraw",result!=null&&result.latestDraw!=null&&result.latestDraw.isValid());
            if(result!=null){String code=KeyProvider.redact(result.errorCode==null?"":result.errorCode);receipt.put("code",code.matches("[A-Z0-9_]{1,100}")?code:"UNCLASSIFIED");String message=KeyProvider.redact(result.errorMessage==null?"":result.errorMessage);String[] parts=message.split("\\|",4);java.util.Set<String> outcomes=new java.util.HashSet<>();for(com.ethk3.novaonepro.ApiTransportGovernanceEngine.Outcome outcome:com.ethk3.novaonepro.ApiTransportGovernanceEngine.Outcome.values())outcomes.add(outcome.name());receipt.put("outcome",parts.length>0&&outcomes.contains(parts[0])?parts[0]:"UNCLASSIFIED");if(parts.length>1&&parts[1].matches("[A-Z0-9_]{1,100}"))receipt.put("publicCode",parts[1]);java.util.regex.Matcher http=java.util.regex.Pattern.compile("\\bHTTP_([1-5][0-9]{2})\\b").matcher(message);if(http.find())receipt.put("httpStatus",Integer.parseInt(http.group(1)));for(String known:new String[]{"SSLHandshakeException","UnknownHostException","SocketTimeoutException","ConnectException","CertificateException"})if(message.contains(known))receipt.put("transportException",known);}
            java.io.File file=new java.io.File(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getTargetContext().getCacheDir(),"r26-bundled-api-environment.json");try(java.io.FileOutputStream out=new java.io.FileOutputStream(file)){out.write(receipt.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));out.getFD().sync();}System.out.println("R26_PACKAGED_API_ATTEMPT="+receipt.toString());
            assertNotNull("Bundled API returned no response",result);
            assertTrue("Bundled API did not return current validated data through the actual app client; "+receipt.toString(),result.success||result.ok);
            assertNotNull("Bundled API returned no latest draw",result.latestDraw);
            assertTrue("Bundled API returned invalid numbers",result.latestDraw.isValid());
        }
    }
}
