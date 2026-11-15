package com.ethk3.novaonepro.strategyv2;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import com.ethk3.novaonepro.BuildConfig;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** R29-001: the source/report/plan baseline this build continues from, and the identity of the installed package
 * as measured on the device (APK bytes and signing certificate). Measured values are never substituted by claims:
 * anything that cannot be read is recorded as null with the reason. */
public final class BaselineIdentityR29 {
    private BaselineIdentityR29() {}
    /** Latest verified R27 application branch head (construction/r27-20261028). */
    public static final String APP_BASELINE = "f9bdddb97291d9d96b5aba940b31d03369c220e2";
    /** Application commit of the 10.29 report the plan was reviewed against. */
    public static final String REPORT_SOURCE = "7d3e0e2271ce2a734ca69f2fca8527c00222bba7";
    /** Report snapshot issue named by the plan (docs/00 §1). */
    public static final String REPORT_SNAPSHOT_ISSUE = "202610290593";
    /** R29.1 plan publication commit (main) and the plan ZIP digest pinned by the package manifest. */
    public static final String PLAN_COMMIT = "96fb4355748a2f28e31927e1f9cb8cb61fac6547";
    public static final String PLAN_BRANCH = "construction/r29-20261029";

    public static Map<String, Object> describe(Context context) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("schema", "R29_BASELINE_IDENTITY_1");
        m.put("appBaseline", APP_BASELINE); m.put("reportSource", REPORT_SOURCE); m.put("reportSnapshotIssue", REPORT_SNAPSHOT_ISSUE);
        m.put("planCommit", PLAN_COMMIT); m.put("constructionBranch", PLAN_BRANCH);
        m.put("sourceCommit", BuildConfig.SOURCE_COMMIT_R26); m.put("applicationId", BuildConfig.APPLICATION_ID);
        m.put("versionCode", (long) BuildConfig.VERSION_CODE); m.put("versionName", BuildConfig.VERSION_NAME);
        m.put("releaseManifestSha256", BuildConfig.RELEASE_MANIFEST_SHA256);
        String apk = null, apkReason = null, cert = null, certReason = null;
        try {
            String path = context.getApplicationInfo().sourceDir;
            try (InputStream in = new FileInputStream(path)) { apk = sha256(in); }
        } catch (Exception e) { apkReason = e.getClass().getSimpleName(); }
        try {
            Signature[] signatures = signatures(context);
            if (signatures == null || signatures.length == 0) certReason = "NO_SIGNATURE"; else cert = hex(MessageDigest.getInstance("SHA-256").digest(signatures[0].toByteArray()));
        } catch (Exception e) { certReason = e.getClass().getSimpleName(); }
        m.put("installedApkSha256", apk); m.put("installedApkReason", apkReason);
        m.put("signingCertificateSha256", cert); m.put("signingCertificateReason", certReason);
        m.put("status", consistent(m) ? "MEASURED" : "INCOMPLETE");
        m.put("note", "设备实测的安装包与签名；与 CI 回执 delivery.json 互证后才算 M01 一致。");
        return m;
    }

    /** Identity only (the certificate is compared with the CI receipt); not used for any trust decision. */
    @android.annotation.SuppressLint("PackageManagerGetSignatures")
    @SuppressWarnings("deprecation")
    static Signature[] signatures(Context context) throws PackageManager.NameNotFoundException {
        PackageManager pm = context.getPackageManager();
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            PackageInfo info = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES);
            return info.signingInfo == null ? null : info.signingInfo.getApkContentsSigners();
        }
        return pm.getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES).signatures;
    }

    /** True when every identity field is present and well-formed (does not prove agreement with the CI receipt). */
    public static boolean consistent(Map<String, Object> m) {
        return hex40(m.get("sourceCommit")) && hex64(m.get("installedApkSha256")) && hex64(m.get("signingCertificateSha256")) && hex64(m.get("releaseManifestSha256"))
                && m.get("versionCode") instanceof Number && ((Number) m.get("versionCode")).longValue() > 577271030L;
    }
    static boolean hex40(Object o) { return o instanceof String && ((String) o).matches("[0-9a-f]{40}"); }
    static boolean hex64(Object o) { return o instanceof String && ((String) o).matches("[0-9a-f]{64}"); }

    static String sha256(InputStream in) throws Exception {
        MessageDigest d = MessageDigest.getInstance("SHA-256"); byte[] b = new byte[65536]; int k;
        while ((k = in.read(b)) > 0) d.update(b, 0, k);
        return hex(d.digest());
    }
    static String hex(byte[] b) { StringBuilder s = new StringBuilder(); for (byte x : b) s.append(String.format(Locale.ROOT, "%02x", x & 0xff)); return s.toString(); }
}
