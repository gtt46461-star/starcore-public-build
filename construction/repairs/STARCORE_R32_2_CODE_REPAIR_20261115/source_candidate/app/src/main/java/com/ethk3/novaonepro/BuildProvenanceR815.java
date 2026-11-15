package com.ethk3.novaonepro;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Read-only installed-artifact provenance verifier. It never reads credentials, never emits user
 * data, and distinguishes the compile-time deterministic manifest from runtime artifact
 * fingerprints. Final expected APK/DEX digests live in the detached release manifest because an
 * archive cannot truthfully embed a hash of its own final bytes.
 */
public final class BuildProvenanceR815 {
    public static final class Snapshot {
        public final boolean valid;
        public final String status;
        public final String packageName;
        public final String versionName;
        public final int versionCode;
        public final String identityHash;
        public final String sourceTreeSha256;
        public final String resourceManifestSha256;
        public final String toolchainSha256;
        public final String classesDexSha256;
        public final String apkSha256;
        public final String certSha256;

        Snapshot(
                boolean valid,
                String status,
                String packageName,
                String versionName,
                int versionCode,
                String identityHash,
                String sourceTreeSha256,
                String resourceManifestSha256,
                String toolchainSha256,
                String classesDexSha256,
                String apkSha256,
                String certSha256) {
            this.valid = valid;
            this.status = safe(status);
            this.packageName = safe(packageName);
            this.versionName = safe(versionName);
            this.versionCode = versionCode;
            this.identityHash = safe(identityHash);
            this.sourceTreeSha256 = safe(sourceTreeSha256);
            this.resourceManifestSha256 = safe(resourceManifestSha256);
            this.toolchainSha256 = safe(toolchainSha256);
            this.classesDexSha256 = safe(classesDexSha256);
            this.apkSha256 = safe(apkSha256);
            this.certSha256 = safe(certSha256);
        }

        public String summary() {
            return status
                    + "|package="
                    + packageName
                    + "|version="
                    + versionName
                    + "|code="
                    + versionCode
                    + "|identity="
                    + identityHash
                    + "|source="
                    + sourceTreeSha256
                    + "|resource="
                    + resourceManifestSha256
                    + "|toolchain="
                    + toolchainSha256
                    + "|dexRuntime="
                    + classesDexSha256
                    + "|apkRuntime="
                    + apkSha256
                    + "|certRuntime="
                    + certSha256;
        }
    }

    private static volatile Snapshot last = snapshot(false, "NOT_VERIFIED", "", "", 0, "", "", "");

    private BuildProvenanceR815() {}

    /** Returns generated build-manifest state without making a device-install claim. */
    public static Snapshot fromGeneratedManifest() {
        boolean generated = SourceManifestR815.valid();
        return snapshot(
                generated && ReleaseIdentityR815.isConsistent(),
                generated ? "GENERATED_MANIFEST_READY" : "GENERATED_MANIFEST_INVALID",
                ReleaseIdentityR815.APPLICATION_ID,
                ReleaseIdentityR815.VERSION_NAME,
                ReleaseIdentityR815.VERSION_CODE,
                "",
                "",
                "");
    }

    /**
     * Validates install identity, the generated source manifest, and fingerprints the actual APK,
     * all classes*.dex entries, and signing certificate. A value in this snapshot is an observed
     * runtime fingerprint, not a circular self-hash comparison.
     */
    public static Snapshot verifyRuntime(Context context) {
        if (context == null) {
            last = snapshot(false, "CONTEXT_NULL", "", "", 0, "", "", "");
            return last;
        }
        try {
            Context app = context.getApplicationContext();
            Context source = app == null ? context : app;
            PackageManager pm = source.getPackageManager();
            PackageInfo info = packageInfo(pm, source.getPackageName());
            boolean packageOk = ReleaseIdentityR815.APPLICATION_ID.equals(source.getPackageName());
            boolean nameOk = ReleaseIdentityR815.VERSION_NAME.equals(safe(info.versionName));
            boolean codeOk = ReleaseIdentityR815.VERSION_CODE == info.versionCode;
            boolean identityOk = ReleaseIdentityR815.isConsistent();
            boolean generatedOk = SourceManifestR815.valid();
            String apkPath = source.getApplicationInfo() == null ? "" : source.getApplicationInfo().sourceDir;
            String apkSha = sha256File(apkPath);
            String dexSha = sha256DexEntries(apkPath);
            String certSha = signingCertificateSha256(info);
            boolean fingerprintsOk = hash(apkSha) && hash(dexSha) && hash(certSha);
            boolean pass = packageOk && nameOk && codeOk && identityOk && generatedOk && fingerprintsOk;
            last = snapshot(
                    pass,
                    pass ? "PASS_RUNTIME_FINGERPRINT" : "MISMATCH",
                    source.getPackageName(),
                    info.versionName,
                    info.versionCode,
                    dexSha,
                    apkSha,
                    certSha);
            return last;
        } catch (PackageManager.NameNotFoundException e) {
            last = snapshot(false, "PACKAGE_NOT_FOUND", "", "", 0, "", "", "");
            return last;
        } catch (IOException e) {
            last = snapshot(false, "ARTIFACT_IO_ERROR", "", "", 0, "", "", "");
            return last;
        } catch (RuntimeException e) {
            last = snapshot(false, "RUNTIME_ERROR:" + e.getClass().getSimpleName(), "", "", 0, "", "", "");
            return last;
        }
    }

    public static Snapshot lastSnapshot() {
        return last;
    }

    @SuppressWarnings("deprecation")
    private static PackageInfo packageInfo(PackageManager pm, String packageName)
            throws PackageManager.NameNotFoundException {
        int flags = Build.VERSION.SDK_INT >= 28
                ? PackageManager.GET_SIGNING_CERTIFICATES
                : PackageManager.GET_SIGNATURES;
        return pm.getPackageInfo(packageName, flags);
    }

    @SuppressWarnings("deprecation")
    private static String signingCertificateSha256(PackageInfo info) {
        Signature[] signatures = null;
        if (Build.VERSION.SDK_INT >= 28 && info.signingInfo != null) {
            signatures = info.signingInfo.getApkContentsSigners();
        }
        if ((signatures == null || signatures.length == 0) && info.signatures != null) {
            signatures = info.signatures;
        }
        if (signatures == null || signatures.length == 0 || signatures[0] == null) return "";
        return sha256Bytes(signatures[0].toByteArray());
    }

    private static String sha256File(String path) throws IOException {
        if (path == null || path.length() == 0 || !new File(path).isFile()) return "";
        InputStream in = null;
        try {
            in = new FileInputStream(path);
            return sha256Stream(in, null);
        } finally {
            if (in != null) in.close();
        }
    }

    /** Canonical digest over sorted classes*.dex names and bytes, supporting multidex builds. */
    private static String sha256DexEntries(String apkPath) throws IOException {
        if (apkPath == null || apkPath.length() == 0) return "";
        ZipFile zip = null;
        try {
            zip = new ZipFile(apkPath);
            List<ZipEntry> entries = new ArrayList<ZipEntry>();
            java.util.Enumeration<? extends ZipEntry> enumeration = zip.entries();
            while (enumeration.hasMoreElements()) {
                ZipEntry entry = enumeration.nextElement();
                if (!entry.isDirectory() && entry.getName().matches("classes(\\d*)?\\.dex")) {
                    entries.add(entry);
                }
            }
            Collections.sort(
                    entries,
                    new Comparator<ZipEntry>() {
                        @Override
                        public int compare(ZipEntry a, ZipEntry b) {
                            return a.getName().compareTo(b.getName());
                        }
                    });
            if (entries.isEmpty()) return "";
            MessageDigest digest = digest();
            byte[] separator = new byte[] {0};
            for (ZipEntry entry : entries) {
                digest.update(entry.getName().getBytes("UTF-8"));
                digest.update(separator);
                InputStream in = zip.getInputStream(entry);
                try {
                    byte[] buffer = new byte[32768];
                    int read;
                    while ((read = in.read(buffer)) != -1) digest.update(buffer, 0, read);
                } finally {
                    in.close();
                }
                digest.update(separator);
            }
            return hex(digest.digest());
        } finally {
            if (zip != null) zip.close();
        }
    }

    private static String sha256Bytes(byte[] value) {
        if (value == null || value.length == 0) return "";
        MessageDigest digest = digest();
        digest.update(value);
        return hex(digest.digest());
    }

    private static String sha256Stream(InputStream in, MessageDigest supplied) throws IOException {
        MessageDigest digest = supplied == null ? digest() : supplied;
        byte[] buffer = new byte[32768];
        int read;
        while ((read = in.read(buffer)) != -1) digest.update(buffer, 0, read);
        return hex(digest.digest());
    }

    private static MessageDigest digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static String hex(byte[] data) {
        StringBuilder out = new StringBuilder(data.length * 2);
        for (byte b : data) out.append(String.format(java.util.Locale.US, "%02x", b & 0xff));
        return out.toString();
    }

    private static Snapshot snapshot(
            boolean valid,
            String status,
            String packageName,
            String versionName,
            int versionCode,
            String classesDexSha256,
            String apkSha256,
            String certSha256) {
        return new Snapshot(
                valid,
                status,
                packageName,
                versionName,
                versionCode,
                ReleaseIdentityR815.sha256(),
                SourceManifestR815.SOURCE_TREE_SHA256,
                SourceManifestR815.RESOURCE_MANIFEST_SHA256,
                SourceManifestR815.TOOLCHAIN_SHA256,
                classesDexSha256,
                apkSha256,
                certSha256);
    }

    private static boolean hash(String value) {
        if (value == null || value.length() != 64) return false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'))) return false;
        }
        return true;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
