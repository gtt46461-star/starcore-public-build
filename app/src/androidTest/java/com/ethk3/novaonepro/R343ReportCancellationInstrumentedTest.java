package com.ethk3.novaonepro;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

@RunWith(AndroidJUnit4.class)
public final class R343ReportCancellationInstrumentedTest {
    @Test
    public void preservesCancellationTimeAtDurableRestartBoundary() throws Exception {
        Field173NativeEvidence.run((app, checks) -> {
            File files = new File(app.getCacheDir(), "r343-cancel-boundary");
            JSONObject result = R343ReportCancellationCases.resumeCancelledReport(files);
            JSONArray phases = result.getJSONArray("phases");
            checks.equal(3, phases.length(), "All three durable boundaries exercised");
            checks.require(result.getInt("assertions") >= 54, "Readback and preservation assertions executed");
            for (int i = 0; i < phases.length(); i++) {
                JSONObject outcome = phases.getJSONObject(i);
                checks.require(outcome.getBoolean("resourcesReleased"), "Receipt confirms cleanup");
                checks.record(outcome.getString("initialPhase"), outcome);
            }
            checks.noDatabase("ENGINEERING_SYNTHETIC", "Real report journal control and filesystem with synthetic task state");
        });
    }
}
