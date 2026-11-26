package com.ethk3.novaonepro;

import android.content.Context;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.ethk3.novaonepro.strategyv2.LegacyContinuityR27;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RunWith(AndroidJUnit4.class)
public final class R343LedgerAuditInstrumentedTest {
    @Test
    public void preservesCurrentLedgerIdentityAndRejectsBrokenEvidence() throws Exception {
        Field173NativeEvidence.run((app, checks) -> {
            Map<String, Object> inherited = LegacyContinuityR27.preservation(app);
            checks.equal(1832L, inherited.get("requirements"), "Packaged inherited requirement count");
            checks.equal(299L, inherited.get("fileCards"), "Packaged inherited file-card count");
            checks.equal(0L, inherited.get("unresolvedCardLinks"), "Valid packaged links are all present");
            checks.equal(0L, inherited.get("completionClaimsWithoutReceipt"), "Packaged audit has no false completion");
            checks.equal("RECORDED_NOT_REVERIFIED", inherited.get("status"), "Inherited coverage is not promoted");
            JSONObject generated = new JSONObject(asset(app, "diagnostics/construction-current-IDENTITY.json", 64 << 10));
            checks.equal(BuildConfig.SOURCE_COMMIT_R26, generated.getString("sourceCommit"), "Current report ledger commit");
            checks.equal(BuildConfig.SOURCE_TREE_R31, generated.getString("sourceTree"), "Current report ledger tree");
            checks.equal(1832, generated.getInt("requirements"), "Current report ledger requirements");
            checks.equal(299, generated.getInt("cards"), "Current report ledger file cards");
            checks.equal(0, generated.getJSONObject("states").getInt("已验证"), "No verified parent without verified originals");
            checks.equal(0, generated.getJSONObject("cardStates").getInt("已验证"), "No verified card without verified originals");

            String requirements = asset(app, "diagnostics/construction-current-REQUIREMENTS_1832.csv", 16 << 20);
            String cards = asset(app, "diagnostics/construction-current-CARDS_299.csv", 16 << 20);
            checks.equal(generated.getString("requirementLedgerSha256"), V51ChecksumEngine.sha256(requirements),
                    "Actual packaged report requirements match the current identity");
            checks.equal(generated.getString("cardLedgerSha256"), V51ChecksumEngine.sha256(cards),
                    "Actual packaged report file cards match the current identity");
            String original = asset(app, LegacyContinuityR27.PRESERVATION_ASSET, 4 << 20);
            Map<String, Object> link = R343LedgerAuditCases.unresolvedCardCase(original);
            Map<String, Object> completion = R343LedgerAuditCases.unreceiptedCompletionCase(original);
            checks.equal("FAIL", link.get("status"), "Wrong card input rejected");
            checks.equal("FAIL", completion.get("status"), "Unsupported verification rejected");

            checks.record("inheritedLedgerHash", inherited.get("assetSha256"));
            checks.record("reportRequirementsHash", generated.getString("requirementLedgerSha256"));
            checks.record("reportCardHash", generated.getString("cardLedgerSha256"));
            checks.record("denominators", new JSONObject().put("requirements", inherited.get("requirements"))
                    .put("cards", inherited.get("fileCards")).put("unresolvedTamper", link.get("unresolvedCardLinks"))
                    .put("unreceiptedTamper", completion.get("completionClaimsWithoutReceipt")));
            checks.noDatabase("ENGINEERING", "Reads exact installed APK assets through existing requirement audit");
        });
    }

    private static String asset(Context app, String path, int limit) throws IOException {
        try (InputStream input = app.getAssets().open(path); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] block = new byte[8192];
            int read;
            while ((read = input.read(block)) != -1) {
                if (read > limit - output.size()) throw new IOException("R343_LEDGER_ASSET_BOUND:" + path);
                output.write(block, 0, read);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
