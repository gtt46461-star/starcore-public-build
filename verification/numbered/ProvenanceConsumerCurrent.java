package com.ethk3.novaonepro;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.json.JSONObject;

/** Current compiled production consumers with explicit host Android/JSON adapters. */
public final class ProvenanceConsumerCurrent {
    private static int assertions;
    private static void check(boolean condition, String id) {
        if (!condition) throw new AssertionError(id);
        assertions++;
    }
    public static void main(String[] args) throws Exception {
        JSONObject input = new JSONObject(Files.readString(Path.of(args[0]), StandardCharsets.UTF_8));
        check(SourceManifestR815.valid(), "CURRENT_GENERATED_FORMAT");
        check(input.getString("sourceTreeSha256").equals(SourceManifestR815.SOURCE_TREE_SHA256), "CURRENT_SOURCE_BINDING");
        check(input.getString("resourceManifestSha256").equals(SourceManifestR815.RESOURCE_MANIFEST_SHA256), "CURRENT_RESOURCE_BINDING");
        check(input.getString("toolchainSha256").equals(SourceManifestR815.TOOLCHAIN_SHA256), "CURRENT_TOOLCHAIN_BINDING");
        check(input.getString("generatedAtUtc").equals(SourceManifestR815.GENERATED_AT_UTC), "CURRENT_TIMESTAMP_BINDING");
        BuildProvenanceR815.Snapshot report = BuildProvenanceR815.fromGeneratedManifest();
        check(report.valid && report.status.equals("GENERATED_MANIFEST_READY"), "CURRENT_PRODUCTION_REPORT_SNAPSHOT");
        check(report.sourceTreeSha256.equals(SourceManifestR815.SOURCE_TREE_SHA256), "REPORT_USES_CURRENT_SOURCE");
        check(report.resourceManifestSha256.equals(SourceManifestR815.RESOURCE_MANIFEST_SHA256), "REPORT_USES_CURRENT_RESOURCE");
        check(report.toolchainSha256.equals(SourceManifestR815.TOOLCHAIN_SHA256), "REPORT_USES_CURRENT_TOOLCHAIN");
        check(report.apkSha256.isEmpty() && report.classesDexSha256.isEmpty(), "GENERATED_IS_NOT_RUNTIME_APK_VERIFICATION");
        check(!BuildProvenanceR815.verifyRuntime(null).valid, "MISSING_DEVICE_CONTEXT_NOT_PASS");
        JSONObject models = new JSONObject(RuntimeModelContractR932.json());
        check(models.getString("source_sha256").equals(SourceManifestR815.SOURCE_TREE_SHA256), "MODEL_REPORT_CONSUMES_CURRENT_IDENTITY");
        JSONObject stale = new JSONObject();
        stale.put("revision", FrozenModuleInputR932.REVISION);
        stale.put("source_sha256", "90945a8994a1907e9f7a2618ed5623ef715af294fc47189d63fad5b2df657132");
        JSONObject raw = new JSONObject(); raw.put("module_replay", stale);
        raw.put("target_issue", "202610060001"); raw.put("model_bundle_hash", "a".repeat(64));
        boolean rejected = false;
        try { new FrozenModuleInputR932.Input(raw); }
        catch (IllegalArgumentException error) { rejected = error.getMessage().startsWith("R932_MODULE_REPLAY_SOURCE|"); }
        check(rejected, "OLD_SOURCE_FROZEN_INPUT_REJECTED_BY_PRODUCTION_GUARD");
        JSONObject receipt = new JSONObject();
        receipt.put("assertions", assertions); receipt.put("source", report.sourceTreeSha256);
        receipt.put("resource", report.resourceManifestSha256); receipt.put("toolchain", report.toolchainSha256);
        receipt.put("scope", "CURRENT_PRODUCTION_BYTECODE_HOST_ANDROID_JSON_ADAPTERS_NOT_DEVICE_RUNTIME");
        Files.writeString(Path.of(args[1]), receipt.toString(), StandardCharsets.UTF_8);
        System.out.println("CURRENT_PROVENANCE_CONSUMERS=PASS|assertions=" + assertions);
    }
}
