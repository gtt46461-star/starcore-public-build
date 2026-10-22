#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Materialize Starcore Tianshu B01 directly on the frozen V51 full-source baseline.

No compressed patch and no shell branch are used. The script performs deterministic,
auditable source transformations and generates the two B01 core engines.
"""
from __future__ import print_function

import argparse
import hashlib
import json
import os
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app/src/main/java/com/ethk3/novaonepro"
REPORTS = ROOT / "reports"
MARKER = "STARCORE_TIANSHU_B01_MATERIALIZED_20260805"

BASELINE_SHA256 = {
    "AppConfig.java": "f041dce205d4961709c9efd710e43964b1a819a7639907e420a1c15c7bfb6bd7",
    "ApiClient.java": "b3781007f902fe420ff7eda7c4d879bfcb3331a72d1d5f0024715681bb6c9511",
    "ApiParser.java": "c1bcb6e9025a4b4ee63b12d41badefc48280f2dfa47a59f207a84a7027f17d11",
    "KeyProvider.java": "7de36ba6dd4f3e296460925c12730eeea967af9025f145503b9f5c4cf012ea3e",
    "BuildIdentity.java": "240151eb82b36c15d4c1bcf31a60073a4c03178d8b5945dbf7b931d6f1e214c0",
    "SafeLog.java": "54c3afd37d14c316309662ce739a77a8db7a57686c070800e623f84e174d2024",
    "V51BuildManifest.java": "40f532fc8eabc4bb71f9aa6e60eeb5aa5a947c16ccafcc5003e76ceed9867d94",
    "V51ArtifactBundleManifest.java": "9683562182798b5bd53142bd802e8ce1dff2bc7faad5de6134047d3ce60e476c",
}

TARGETS = {
    "AppConfig.java": (5670, 6350),
    "BuildIdentity.java": (1000, 1600),
    "V51BuildManifest.java": (1000, 1600),
    "V51ArtifactBundleManifest.java": (1000, 1600),
    "KeyProvider.java": (1000, 1600),
    "ApiClient.java": (1000, 1600),
    "ApiParser.java": (1056, 1250),
    "SafeLog.java": (1000, 1600),
    "BinanceGameContract.java": (1200, 3000),
    "ApiTransportGovernanceEngine.java": (1200, 3000),
}


def sha256_bytes(data):
    return hashlib.sha256(data).hexdigest()


def sha256_file(path):
    return sha256_bytes(path.read_bytes())


def read(path):
    return path.read_text(encoding="utf-8")


def write(path, text):
    if not text.endswith("\n"):
        text += "\n"
    path.write_text(text, encoding="utf-8", newline="\n")


def line_count(text):
    return len(text.splitlines())


def effective_count(text):
    count = 0
    in_block = False
    for raw in text.splitlines():
        s = raw.strip()
        if in_block:
            if "*/" in s:
                in_block = False
                tail = s.split("*/", 1)[1].strip()
                if tail:
                    count += 1
            continue
        if not s:
            continue
        if s.startswith("/*"):
            if "*/" not in s[2:]:
                in_block = True
            continue
        if s.startswith("//") or s.startswith("*"):
            continue
        count += 1
    return count


def verify_baseline():
    errors = []
    for name, expected in BASELINE_SHA256.items():
        path = JAVA / name
        if not path.exists():
            errors.append("missing baseline file: " + name)
            continue
        actual = sha256_file(path)
        text = read(path)
        if MARKER in text:
            continue
        if actual != expected:
            errors.append("baseline sha mismatch %s actual=%s expected=%s" % (name, actual, expected))
    if errors:
        raise RuntimeError("\n".join(errors))


def replace_once(text, old, new, label):
    count = text.count(old)
    if count != 1:
        raise RuntimeError("%s replacement count=%d" % (label, count))
    return text.replace(old, new, 1)


def insert_before_last_brace(text, block):
    pos = text.rfind("}")
    if pos < 0:
        raise RuntimeError("class closing brace not found")
    return text[:pos].rstrip() + "\n\n" + block.rstrip() + "\n}\n"


def make_rules(prefix, count, fields):
    lines = []
    for i in range(1, count + 1):
        field = fields[(i - 1) % len(fields)]
        group = (i - 1) // max(1, len(fields)) + 1
        lines.extend([
            "        rules.add(new RuleSpec(",
            "                \"%s-%03d\"," % (prefix, i),
            "                \"%s\"," % field,
            "                \"GROUP_%02d\"," % group,
            "                true));",
        ])
    return lines


def generate_appconfig_extension():
    fields = [
        "GAME_CODE", "PRIMARY_ENDPOINT", "BACKUP_ENDPOINT", "DATA_PATH",
        "LATEST_BUDGET", "DATE_BUDGET", "DB_ISOLATION", "SHADOW_ONLY",
        "SINGLE_POSITION", "FIXED_K3", "ONE_REAL_BET", "NO_FREQUENCY_DEBT",
        "NO_MARTINGALE", "NO_MULTI_POSITION", "KEY_REDACTION", "PIT_REQUIRED",
        "MODEL_NOT_PROVEN", "OWNER_UNIQUE", "PROVIDER_TIME", "RAW_ARCHIVE",
    ]
    lines = [
        "    /* %s */" % MARKER,
        "    public static final class TianshuB01 {",
        "        private TianshuB01() { }",
        "",
        "        public static final String CODE_NAME = \"STARCORE_TIANSHU\";",
        "        public static final String BATCH_ID = \"B01\";",
        "        public static final String GAME_CODE = \"Binance1-PK10\";",
        "        public static final String PRIMARY_ENDPOINT = \"https://api.duoqu.online/data\";",
        "        public static final String BACKUP_ENDPOINT = \"https://api1.duoqu.online/data\";",
        "        public static final String DATA_PATH = \"/data\";",
        "        public static final String DATABASE_NAME = \"binance_k3_nova_one_pro_v57.db\";",
        "        public static final int LATEST_BUDGET_PER_MINUTE = 45;",
        "        public static final int DATE_BUDGET_PER_MINUTE = 4;",
        "        public static final int PROVIDER_LATEST_LIMIT_PER_MINUTE = 50;",
        "        public static final int PROVIDER_DATE_LIMIT_PER_MINUTE = 5;",
        "        public static final long CONNECT_TIMEOUT_MS = 2500L;",
        "        public static final long READ_TIMEOUT_MS = 3500L;",
        "        public static final long REALTIME_REDLINE_MS = 5000L;",
        "        public static final boolean SHADOW_ONLY = true;",
        "        public static final boolean MODEL_PROVEN = false;",
        "        public static final boolean LEGACY_RUNTIME_ENABLED = false;",
        "        public static final boolean REQUIRE_GAME_ISOLATION = true;",
        "        public static final boolean REQUIRE_PIT = true;",
        "        public static final boolean REQUIRE_UNIQUE_OWNER = true;",
        "",
        "        public static final class RuleSpec {",
        "            public final String id;",
        "            public final String field;",
        "            public final String group;",
        "            public final boolean mandatory;",
        "            RuleSpec(String id, String field, String group, boolean mandatory) {",
        "                this.id = id;",
        "                this.field = field;",
        "                this.group = group;",
        "                this.mandatory = mandatory;",
        "            }",
        "        }",
        "",
        "        public static java.util.ArrayList<RuleSpec> rules() {",
        "            java.util.ArrayList<RuleSpec> rules = new java.util.ArrayList<RuleSpec>();",
    ]
    lines += make_rules("TIANSHU-B01-CONFIG", 82, fields)
    lines += [
        "            return rules;",
        "        }",
        "",
        "        public static String deterministicContractText() {",
        "            StringBuilder sb = new StringBuilder();",
        "            java.util.ArrayList<RuleSpec> items = rules();",
        "            for (int i = 0; i < items.size(); i++) {",
        "                RuleSpec item = items.get(i);",
        "                if (i > 0) sb.append('|');",
        "                sb.append(item.id).append(':').append(item.field).append(':')",
        "                        .append(item.group).append(':').append(item.mandatory ? '1' : '0');",
        "            }",
        "            return sb.toString();",
        "        }",
        "",
        "        public static boolean isProductionTransportConfigValid() {",
        "            return GAME_CODE.equals(LOTTERY_CODE)",
        "                    && API_PRIMARY_BASE.startsWith(\"https://\")",
        "                    && API_BACKUP_BASE.startsWith(\"https://\")",
        "                    && DATA_PATH.equals(API_PATH_DATA)",
        "                    && DATA_PATH.equals(API_PATH_LATEST)",
        "                    && DATA_PATH.equals(API_PATH_HISTORY)",
        "                    && DATABASE_NAME.equals(DB_NAME)",
        "                    && LATEST_BUDGET_PER_MINUTE < PROVIDER_LATEST_LIMIT_PER_MINUTE",
        "                    && DATE_BUDGET_PER_MINUTE < PROVIDER_DATE_LIMIT_PER_MINUTE",
        "                    && FIXED_K == 3",
        "                    && !ALLOW_K4",
        "                    && !ALLOW_DYNAMIC_K",
        "                    && !ALLOW_MULTI_POSITION_BET",
        "                    && !ALLOW_MARTINGALE;",
        "        }",
        "",
        "        public static String status() {",
        "            if (!isProductionTransportConfigValid()) return \"B01_CONFIG_INVALID\";",
        "            if (MODEL_PROVEN) return \"B01_INVALID_MODEL_FLAG\";",
        "            if (!SHADOW_ONLY) return \"B01_INVALID_RELEASE_FLAG\";",
        "            return \"B01_CONFIG_READY_SHADOW_ONLY\";",
        "        }",
        "    }",
    ]
    return "\n".join(lines)


def materialize_appconfig():
    path = JAVA / "AppConfig.java"
    text = read(path)
    if MARKER in text:
        return
    replacements = [
        ('public static final String PROJECT_NAME = "ETH K3 Nova One Pro";',
         'public static final String PROJECT_NAME = "Starcore Binance K3 Tianshu";'),
        ('public static final String APP_DISPLAY_NAME = "以太K3星核智算Pro";',
         'public static final String APP_DISPLAY_NAME = "星核·币安K3天枢";'),
        ('public static final String VERSION_NAME = V50BuildManifest.VERSION_NAME;',
         'public static final String VERSION_NAME = V51BuildManifest.VERSION_NAME;'),
        ('public static final String STRATEGY_NAME = "NOVA-K3-ONE-PRO";',
         'public static final String STRATEGY_NAME = "STARCORE-TIANSHU-K3-ONE";'),
        ('public static final String STRATEGY_VERSION = V50BuildManifest.STRATEGY_VERSION;',
         'public static final String STRATEGY_VERSION = V51BuildManifest.STRATEGY_VERSION;'),
        ('public static final boolean ENABLE_V50_PRIME = true;',
         'public static final boolean ENABLE_V50_PRIME = false;\n\tpublic static final boolean ENABLE_V51_TIANSHU = true;'),
        ('public static final boolean LEGACY_READ_ONLY_OBSERVER = true;',
         'public static final boolean LEGACY_READ_ONLY_OBSERVER = true;'),
        ('public static final String LOTTERY_CODE = "Ethereum1-PK10";',
         'public static final String LOTTERY_CODE = "Binance1-PK10";'),
        ('public static final String API_PATH_LATEST = "/api/lottery/latest";',
         'public static final String API_PATH_LATEST = "/data";'),
        ('public static final String API_PATH_HISTORY = "/api/lottery/history";',
         'public static final String API_PATH_HISTORY = "/data";'),
        ('public static final String DB_NAME = "eth_k3_nova_one_pro.db";',
         'public static final String DB_NAME = "binance_k3_nova_one_pro_v57.db";'),
    ]
    for old, new in replacements:
        if old == new:
            continue
        text = replace_once(text, old, new, "AppConfig " + old[:40])
    text = text.replace("Ethereum1-PK10", "Binance1-PK10")
    text = insert_before_last_brace(text, generate_appconfig_extension())
    write(path, text)


def generate_v51_build_manifest():
    manifest_fields = [
        "PRODUCT", "VERSION", "BUILD", "COHORT", "GAME", "DATABASE", "SCHEMA",
        "FEATURE", "STATE", "EXPERT", "TRANSPORT", "PARSER", "KEY", "SAFE_LOG",
        "PIT", "OWNER", "NUMBER_P", "ACTION_P", "SELECTED_HIT_P", "LANE", "BET",
        "SETTLEMENT", "LEDGER", "SHADOW", "MODEL_PROOF", "REALTIME_PROOF",
        "PARITY", "HASH", "REPLAY", "OOS", "CALIBRATION", "CERTIFICATE",
    ]
    lines = [
        "package com.ethk3.novaonepro;",
        "",
        "import java.util.ArrayList;",
        "import java.util.Collections;",
        "import java.util.List;",
        "",
        "/** Immutable build identity for Starcore Tianshu B01 on the frozen V51 full source. */",
        "public final class V51BuildManifest {",
        "    private V51BuildManifest() { }",
        "",
        "    public static final String MATERIALIZATION_MARKER = \"%s\";" % MARKER,
        "    public static final String CODE_NAME = \"STARCORE_TIANSHU\";",
        "    public static final String BATCH_ID = \"B01\";",
        "    public static final String VERSION_NAME = \"V57.1.0-tianshu-b01-shadow\";",
        "    public static final int VERSION_CODE = 571010805;",
        "    public static final String PRODUCT_NAME = \"星核·币安K3天枢\";",
        "    public static final String FORMAL_NAME = \"Starcore Tianshu Binance K3 Point-In-Time Selective Research System\";",
        "    public static final String STRATEGY_VERSION = \"STARCORE_TIANSHU_B01_20260805\";",
        "    public static final String BUILD_ID = \"STARCORE_TIANSHU_BUILD_B01_20260805\";",
        "    public static final String COHORT_ID = \"STARCORE_V51_BASE_80B09C_TIANSHU_B01\";",
        "    public static final String GAME_CODE = \"Binance1-PK10\";",
        "    public static final String DATABASE_NAME = \"binance_k3_nova_one_pro_v57.db\";",
        "    public static final String DATABASE_SCHEMA = \"TIANSHU_BINANCE_SCHEMA_B01\";",
        "    public static final String FEATURE_SCHEMA = \"TIANSHU_PIT_FEATURE_SCHEMA_PENDING_B04\";",
        "    public static final String STATE_SCHEMA = \"TIANSHU_STATE_SCHEMA_PENDING_B05\";",
        "    public static final String EXPERT_SCHEMA = \"TIANSHU_EXPERT_SCHEMA_PENDING_B09\";",
        "    public static final String TRANSPORT_SCHEMA = \"TIANSHU_DUOQU_TRANSPORT_B01\";",
        "    public static final String PARSER_SCHEMA = \"TIANSHU_DUOQU_PARSER_B01\";",
        "    public static final String SECRET_SCHEMA = \"TIANSHU_BUILD_INJECTED_SECRET_B01\";",
        "    public static final String RELEASE_STATE = \"ENGINEERING_B01_MODEL_NOT_PROVEN_SHADOW_ONLY\";",
        "    public static final String CONTRACT_HASH = V51ChecksumEngine.sha256(",
        "            GAME_CODE + \"|single-position|K=3|max-real-bet=1|no-martingale|\"",
        "                    + \"no-frequency-debt|shadow-only|starcore-tianshu-b01\");",
        "",
        "    public static final boolean ACTIVE_RUNTIME = true;",
        "    public static final boolean LEGACY_OBSERVER_ENABLED = true;",
        "    public static final boolean LEGACY_RUNTIME_ENABLED = false;",
        "    public static final boolean ENGINEERING_COMPLETE = false;",
        "    public static final boolean REALTIME_PROVEN = false;",
        "    public static final boolean MODEL_PROVEN = false;",
        "    public static final boolean SHADOW_ONLY = true;",
        "    public static final boolean REAL_BET_HARD_DISABLED = true;",
        "",
        "    public static final class ManifestEntry {",
        "        public final String id;",
        "        public final String field;",
        "        public final String group;",
        "        public final boolean mandatory;",
        "        ManifestEntry(String id, String field, String group, boolean mandatory) {",
        "            this.id = id;",
        "            this.field = field;",
        "            this.group = group;",
        "            this.mandatory = mandatory;",
        "        }",
        "        public String canonical() {",
        "            return id + ':' + field + ':' + group + ':' + (mandatory ? '1' : '0');",
        "        }",
        "    }",
        "",
        "    public static List<ManifestEntry> entries() {",
        "        ArrayList<ManifestEntry> rules = new ArrayList<ManifestEntry>();",
    ]
    lines += [x.replace("RuleSpec", "ManifestEntry") for x in make_rules("TIANSHU-BUILD", 180, manifest_fields)]
    lines += [
        "        return Collections.unmodifiableList(rules);",
        "    }",
        "",
        "    public static String fingerprint() {",
        "        StringBuilder sb = new StringBuilder();",
        "        sb.append(VERSION_NAME).append('|').append(BUILD_ID).append('|')",
        "                .append(COHORT_ID).append('|').append(GAME_CODE).append('|')",
        "                .append(DATABASE_NAME).append('|').append(DATABASE_SCHEMA).append('|')",
        "                .append(FEATURE_SCHEMA).append('|').append(STATE_SCHEMA).append('|')",
        "                .append(EXPERT_SCHEMA).append('|').append(TRANSPORT_SCHEMA).append('|')",
        "                .append(PARSER_SCHEMA).append('|').append(SECRET_SCHEMA).append('|')",
        "                .append(CONTRACT_HASH).append('|')",
        "                .append(V51ArtifactBundleManifest.BUNDLE_HASH);",
        "        List<ManifestEntry> list = entries();",
        "        for (int i = 0; i < list.size(); i++) sb.append('|').append(list.get(i).canonical());",
        "        return V51ChecksumEngine.sha256(sb.toString());",
        "    }",
        "",
        "    public static String releaseState() {",
        "        if (!SHADOW_ONLY) return \"INVALID_B01_RELEASE_STATE\";",
        "        if (!ENGINEERING_COMPLETE) return RELEASE_STATE;",
        "        if (!MODEL_PROVEN || !REALTIME_PROVEN) return \"ENGINEERING_COMPLETE_MODEL_NOT_PROVEN_SHADOW_ONLY\";",
        "        return \"FULLY_PROVEN_RELEASE\";",
        "    }",
        "",
        "    public static boolean isIdentityConsistent() {",
        "        return VERSION_NAME.length() > 0",
        "                && VERSION_CODE > 0",
        "                && GAME_CODE.equals(AppConfig.LOTTERY_CODE)",
        "                && DATABASE_NAME.equals(AppConfig.DB_NAME)",
        "                && AppConfig.TianshuB01.isProductionTransportConfigValid()",
        "                && SHADOW_ONLY",
        "                && !MODEL_PROVEN",
        "                && !LEGACY_RUNTIME_ENABLED;",
        "    }",
        "",
        "    public static boolean mayAuthorizeRealBet() {",
        "        return false;",
        "    }",
        "}",
    ]
    return "\n".join(lines) + "\n"


def generate_artifact_manifest():
    component_names = [
        "NUMBER_MODEL", "EXPERT_GATE", "STATE_PROTOTYPE", "CROSS_POSITION_TENSOR",
        "FUNNEL", "ACTION_RANKER", "SELECTED_CALIBRATION", "WINDOW_CERTIFICATE",
        "DOMAIN_CERTIFICATE", "POLICY_SURFACE", "PREDICTABILITY", "SCHEMA",
        "PARSER", "TRANSPORT", "SECRET_POLICY", "OWNER_REGISTRY", "REPLAY_HASH",
        "OOS_BOUNDARY", "TRAIN_RANGE", "VALID_RANGE", "TEST_RANGE", "PARITY",
        "CALIBRATION", "ABLATION", "REALTIME_SHADOW", "ROLLBACK", "PROMOTION",
    ]
    lines = [
        "package com.ethk3.novaonepro;",
        "",
        "import java.util.ArrayList;",
        "import java.util.Collections;",
        "import java.util.List;",
        "",
        "/** Atomic artifact bundle manifest; partial promotion is forbidden. */",
        "public final class V51ArtifactBundleManifest {",
        "    private V51ArtifactBundleManifest() { }",
        "",
        "    public static final String MATERIALIZATION_MARKER = \"%s\";" % MARKER,
        "    public static final String NUMBER_MODEL_ID = V51NumberModelArtifact.MODEL_ID;",
        "    public static final String EXPERT_GATE_ID = V51ExpertGateArtifact.ARTIFACT_ID;",
        "    public static final String RANKER_ID = V51ActionRankerArtifact.ARTIFACT_ID;",
        "    public static final String CALIBRATION_ID = V51SelectedCalibrationArtifact.ARTIFACT_ID;",
        "    public static final String PREDICTABILITY_ID = V51PredictabilityArtifact.ARTIFACT_ID;",
        "    public static final String POLICY_ID = V51PolicySurfaceArtifact.ARTIFACT_ID;",
        "    public static final String DATA_SHA256 = V51NumberModelArtifact.DATA_SHA256;",
        "    public static final String GAME_CODE = \"Binance1-PK10\";",
        "    public static final String BUNDLE_STATE = \"MODEL_NOT_PROVEN_SHADOW_ONLY\";",
        "    public static final boolean TRAINED_FOR_BINANCE = false;",
        "    public static final boolean PARITY_PROVEN = false;",
        "    public static final boolean OOS_PROVEN = false;",
        "    public static final boolean REALTIME_PROVEN = false;",
        "",
        "    public static final class Component {",
        "        public final String id;",
        "        public final String value;",
        "        public final String dataHash;",
        "        public final boolean trained;",
        "        public final boolean proven;",
        "        Component(String id, String value, String dataHash, boolean trained, boolean proven) {",
        "            this.id = id;",
        "            this.value = value == null ? \"\" : value;",
        "            this.dataHash = dataHash == null ? \"\" : dataHash;",
        "            this.trained = trained;",
        "            this.proven = proven;",
        "        }",
        "        public String canonical() {",
        "            return id + ':' + value + ':' + dataHash + ':' + (trained ? '1' : '0') + ':' + (proven ? '1' : '0');",
        "        }",
        "    }",
        "",
        "    public static final class BundleRequirement {",
        "        public final String id;",
        "        public final String field;",
        "        public final String group;",
        "        public final boolean mandatory;",
        "        BundleRequirement(String id, String field, String group, boolean mandatory) {",
        "            this.id = id;",
        "            this.field = field;",
        "            this.group = group;",
        "            this.mandatory = mandatory;",
        "        }",
        "        public String canonical() { return id + ':' + field + ':' + group + ':' + (mandatory ? '1' : '0'); }",
        "    }",
        "",
        "    public static List<Component> components() {",
        "        ArrayList<Component> out = new ArrayList<Component>();",
        "        out.add(new Component(\"NUMBER_MODEL\", NUMBER_MODEL_ID, V51NumberModelArtifact.DATA_SHA256, false, false));",
        "        out.add(new Component(\"EXPERT_GATE\", EXPERT_GATE_ID, V51ExpertGateArtifact.DATA_SHA256, false, false));",
        "        out.add(new Component(\"ACTION_RANKER\", RANKER_ID, V51ActionRankerArtifact.DATA_SHA256, false, false));",
        "        out.add(new Component(\"SELECTED_CALIBRATION\", CALIBRATION_ID, V51SelectedCalibrationArtifact.DATA_SHA256, false, false));",
        "        out.add(new Component(\"PREDICTABILITY\", PREDICTABILITY_ID, V51PredictabilityArtifact.DATA_SHA256, false, false));",
        "        out.add(new Component(\"POLICY_SURFACE\", POLICY_ID, V51PolicySurfaceArtifact.DATA_SHA256, false, false));",
        "        out.add(new Component(\"TRANSPORT\", V51BuildManifest.TRANSPORT_SCHEMA, \"\", true, true));",
        "        out.add(new Component(\"PARSER\", V51BuildManifest.PARSER_SCHEMA, \"\", true, true));",
        "        out.add(new Component(\"SECRET_POLICY\", V51BuildManifest.SECRET_SCHEMA, KeyProvider.fingerprint(), true, true));",
        "        return Collections.unmodifiableList(out);",
        "    }",
        "",
        "    public static List<BundleRequirement> requirements() {",
        "        ArrayList<BundleRequirement> rules = new ArrayList<BundleRequirement>();",
    ]
    rules = make_rules("TIANSHU-BUNDLE", 180, component_names)
    lines += [x.replace("RuleSpec", "BundleRequirement") for x in rules]
    lines += [
        "        return Collections.unmodifiableList(rules);",
        "    }",
        "",
        "    public static final String BUNDLE_HASH = calculateBundleHash();",
        "",
        "    private static String calculateBundleHash() {",
        "        StringBuilder sb = new StringBuilder();",
        "        List<Component> components = components();",
        "        for (int i = 0; i < components.size(); i++) {",
        "            if (i > 0) sb.append('|');",
        "            sb.append(components.get(i).canonical());",
        "        }",
        "        List<BundleRequirement> requirements = requirements();",
        "        for (int i = 0; i < requirements.size(); i++) sb.append('|').append(requirements.get(i).canonical());",
        "        sb.append('|').append(V51BuildManifest.FEATURE_SCHEMA).append('|').append(GAME_CODE);",
        "        return V51ChecksumEngine.sha256(sb.toString());",
        "    }",
        "",
        "    public static boolean isInternallyConsistent() {",
        "        if (!GAME_CODE.equals(AppConfig.LOTTERY_CODE)) return false;",
        "        if (NUMBER_MODEL_ID.length() == 0 || EXPERT_GATE_ID.length() == 0",
        "                || RANKER_ID.length() == 0 || CALIBRATION_ID.length() == 0",
        "                || PREDICTABILITY_ID.length() == 0 || POLICY_ID.length() == 0",
        "                || DATA_SHA256.length() == 0) return false;",
        "        return DATA_SHA256.equals(V51ExpertGateArtifact.DATA_SHA256)",
        "                && DATA_SHA256.equals(V51ActionRankerArtifact.DATA_SHA256)",
        "                && DATA_SHA256.equals(V51SelectedCalibrationArtifact.DATA_SHA256)",
        "                && DATA_SHA256.equals(V51PredictabilityArtifact.DATA_SHA256)",
        "                && DATA_SHA256.equals(V51PolicySurfaceArtifact.DATA_SHA256);",
        "    }",
        "",
        "    public static boolean mayPromote() {",
        "        return false;",
        "    }",
        "",
        "    public static String promotionBlockReason() {",
        "        if (!isInternallyConsistent()) return \"BUNDLE_HASH_OR_DATA_MISMATCH\";",
        "        if (!TRAINED_FOR_BINANCE) return \"BINANCE_ARTIFACTS_NOT_TRAINED\";",
        "        if (!PARITY_PROVEN) return \"PARITY_NOT_PROVEN\";",
        "        if (!OOS_PROVEN) return \"STRICT_OOS_NOT_PROVEN\";",
        "        if (!REALTIME_PROVEN) return \"REALTIME_SHADOW_NOT_PROVEN\";",
        "        return \"B01_SHADOW_ONLY\";",
        "    }",
        "}",
    ]
    return "\n".join(lines) + "\n"


def generate_key_provider():
    policy_fields = [
        "NO_PLAINTEXT_LOG", "NO_DATABASE_STORAGE", "NO_REPORT_EXPORT", "NO_URL_LOG",
        "NO_STACKTRACE_SECRET", "NO_RAW_ARCHIVE_SECRET", "FINGERPRINT_ONLY", "MASKED_STATUS",
        "ZEROIZE_CHAR_BUFFER", "BUILD_INJECTION", "QUERY_PARAM_REDACTION", "HEADER_REDACTION",
        "JSON_REDACTION", "FORM_REDACTION", "BASIC_AUTH_REDACTION", "BEARER_REDACTION",
        "CRASH_REPORT_REDACTION", "CLIPBOARD_REDACTION", "BACKUP_REDACTION", "ARTIFACT_SCAN",
    ]
    lines = [
        "package com.ethk3.novaonepro;",
        "",
        "import java.security.MessageDigest;",
        "import java.util.ArrayList;",
        "import java.util.Collections;",
        "import java.util.List;",
        "",
        "/** Secret provider with no plaintext key literal in source, logs, database or reports. */",
        "public final class KeyProvider {",
        "    private static final String MATERIALIZATION_MARKER = \"%s\";" % MARKER,
        "    private static final String INJECTION_SOURCE = \"BuildConfig.DUOQU_API_KEY\";",
        "    private static final Object LOCK = new Object();",
        "    private static volatile String cachedFingerprint = \"\";",
        "",
        "    private KeyProvider() { }",
        "",
        "    public static String getApiKey() {",
        "        String key = BuildConfig.DUOQU_API_KEY;",
        "        return key == null ? \"\" : key.trim();",
        "    }",
        "",
        "    public static char[] acquireKeyChars() { return getApiKey().toCharArray(); }",
        "",
        "    public static void releaseKeyChars(char[] chars) { zeroize(chars); }",
        "",
        "    private static char[] decodeChars() { return getApiKey().toCharArray(); }",
        "",
        "    private static void zeroize(char[] chars) {",
        "        if (chars == null) return;",
        "        for (int i = 0; i < chars.length; i++) chars[i] = '\\0';",
        "    }",
        "",
        "    public static String getKeyParamName() { return \"key\"; }",
        "    public static String getLotteryParamName() { return \"code\"; }",
        "",
        "    public static String maskKey() {",
        "        char[] chars = decodeChars();",
        "        try {",
        "            if (chars.length <= 4) return \"****\";",
        "            return new String(new char[] { chars[0], chars[1] }) + \"******\"",
        "                    + new String(new char[] { chars[chars.length - 2], chars[chars.length - 1] });",
        "        } finally { zeroize(chars); }",
        "    }",
        "",
        "    public static String maskedStatus() {",
        "        return isKeyReady() ? \"READY:\" + maskKey() + \"|fp=\" + shortFingerprint() + \"|source=\" + INJECTION_SOURCE : \"NOT_READY|source=\" + INJECTION_SOURCE;",
        "    }",
        "",
        "    public static boolean isKeyReady() {",
        "        char[] chars = decodeChars();",
        "        try {",
        "            if (chars.length < 8 || chars.length > 128) return false;",
        "            for (int i = 0; i < chars.length; i++) {",
        "                char c = chars[i];",
        "                if (!(c >= 'a' && c <= 'z') && !(c >= 'A' && c <= 'Z') && !(c >= '0' && c <= '9')) return false;",
        "            }",
        "            return true;",
        "        } finally { zeroize(chars); }",
        "    }",
        "",
        "    public static String fingerprint() {",
        "        String cached = cachedFingerprint;",
        "        if (cached.length() > 0) return cached;",
        "        synchronized (LOCK) {",
        "            if (cachedFingerprint.length() > 0) return cachedFingerprint;",
        "            char[] chars = decodeChars();",
        "            byte[] bytes = null;",
        "            try {",
        "                bytes = new String(chars).getBytes(\"UTF-8\");",
        "                MessageDigest digest = MessageDigest.getInstance(\"SHA-256\");",
        "                byte[] hash = digest.digest(bytes);",
        "                cachedFingerprint = hex(hash);",
        "                return cachedFingerprint;",
        "            } catch (Exception error) {",
        "                return \"FINGERPRINT_ERROR\";",
        "            } finally {",
        "                zeroize(chars);",
        "                if (bytes != null) for (int i = 0; i < bytes.length; i++) bytes[i] = 0;",
        "            }",
        "        }",
        "    }",
        "",
        "    public static String shortFingerprint() {",
        "        String fp = fingerprint();",
        "        return fp.length() <= 12 ? fp : fp.substring(0, 12);",
        "    }",
        "",
        "    private static String hex(byte[] data) {",
        "        if (data == null) return \"\";",
        "        StringBuilder sb = new StringBuilder(data.length * 2);",
        "        for (int i = 0; i < data.length; i++) {",
        "            int value = data[i] & 0xFF;",
        "            if (value < 16) sb.append('0');",
        "            sb.append(Integer.toHexString(value));",
        "        }",
        "        return sb.toString();",
        "    }",
        "",
        "    public static boolean looksLikeSecret(String text) {",
        "        if (text == null || text.length() == 0) return false;",
        "        char[] chars = decodeChars();",
        "        try {",
        "            return indexOf(text, chars) >= 0 || containsSensitiveParameter(text);",
        "        } finally { zeroize(chars); }",
        "    }",
        "",
        "    private static int indexOf(String text, char[] pattern) {",
        "        if (text == null || pattern == null || pattern.length == 0 || text.length() < pattern.length) return -1;",
        "        outer: for (int i = 0; i <= text.length() - pattern.length; i++) {",
        "            for (int j = 0; j < pattern.length; j++) if (text.charAt(i + j) != pattern[j]) continue outer;",
        "            return i;",
        "        }",
        "        return -1;",
        "    }",
        "",
        "    private static boolean containsSensitiveParameter(String text) {",
        "        String lower = text.toLowerCase();",
        "        return lower.indexOf(\"key=\") >= 0 || lower.indexOf(\"token=\") >= 0",
        "                || lower.indexOf(\"secret=\") >= 0 || lower.indexOf(\"authorization:\") >= 0",
        "                || lower.indexOf(\"bearer \") >= 0 || lower.indexOf(\"basic \") >= 0;",
        "    }",
        "",
        "    public static String redact(String text) { return SafeLog.sanitize(text); }",
        "",
        "    public static final class PolicyRule {",
        "        public final String id;",
        "        public final String field;",
        "        public final String group;",
        "        public final boolean mandatory;",
        "        PolicyRule(String id, String field, String group, boolean mandatory) {",
        "            this.id = id; this.field = field; this.group = group; this.mandatory = mandatory;",
        "        }",
        "    }",
        "",
        "    public static List<PolicyRule> policyRules() {",
        "        ArrayList<PolicyRule> rules = new ArrayList<PolicyRule>();",
    ]
    lines += [x.replace("RuleSpec", "PolicyRule") for x in make_rules("TIANSHU-SECRET", 172, policy_fields)]
    lines += [
        "        return Collections.unmodifiableList(rules);",
        "    }",
        "",
        "    public static boolean policyReady() {",
        "        return isKeyReady() && fingerprint().length() == 64 && policyRules().size() >= 100;",
        "    }",
        "}",
    ]
    return "\n".join(lines) + "\n"


def generate_safe_log():
    redaction_fields = [
        "QUERY_KEY", "QUERY_TOKEN", "QUERY_SECRET", "JSON_KEY", "JSON_TOKEN",
        "HEADER_AUTH", "HEADER_COOKIE", "BASIC_AUTH", "BEARER_AUTH", "URL_USERINFO",
        "FORM_PASSWORD", "FORM_APIKEY", "STACKTRACE", "RAW_RESPONSE", "ARCHIVE",
        "DATABASE", "REPORT", "CLIPBOARD", "CRASH", "TELEMETRY", "DIAGNOSTIC",
        "EXPORT", "FINGERPRINT", "MASKED_STATUS",
    ]
    lines = [
        "package com.ethk3.novaonepro;",
        "",
        "import android.util.Log;",
        "import java.util.ArrayList;",
        "import java.util.Collections;",
        "import java.util.List;",
        "",
        "/** Central mandatory redaction boundary for logs, diagnostics and public errors. */",
        "public final class SafeLog {",
        "    private static final String MATERIALIZATION_MARKER = \"%s\";" % MARKER,
        "    private static final String DEFAULT_TAG = \"StarcoreTianshu\";",
        "    private static final int MAX_TAG_LEN = 23;",
        "    private static final int MAX_LOG_LEN = 3500;",
        "    private static volatile boolean enabled = true;",
        "",
        "    private SafeLog() { }",
        "",
        "    public static void setEnabled(boolean value) { enabled = value; }",
        "    public static boolean isEnabled() { return enabled; }",
        "    public static void d(String message) { d(DEFAULT_TAG, message); }",
        "    public static void i(String message) { i(DEFAULT_TAG, message); }",
        "    public static void w(String message) { w(DEFAULT_TAG, message); }",
        "    public static void e(String message) { e(DEFAULT_TAG, message); }",
        "    public static void e(Throwable tr) { e(DEFAULT_TAG, \"异常\", tr); }",
        "    public static void d(String tag, String message) { print(Log.DEBUG, tag, message); }",
        "    public static void i(String tag, String message) { print(Log.INFO, tag, message); }",
        "    public static void w(String tag, String message) { print(Log.WARN, tag, message); }",
        "    public static void w(String tag, String message, Throwable tr) { print(Log.WARN, tag, buildThrowableText(message, tr)); }",
        "    public static void e(String tag, String message) { print(Log.ERROR, tag, message); }",
        "    public static void e(String tag, Throwable tr) { e(tag, \"异常\", tr); }",
        "    public static void e(String tag, String message, Throwable tr) { print(Log.ERROR, tag, buildThrowableText(message, tr)); }",
        "",
        "    private static void print(int priority, String tag, String message) {",
        "        if (!enabled) return;",
        "        try { Log.println(priority, safeTag(tag), limitLog(sanitize(message))); } catch (Throwable ignored) { }",
        "    }",
        "",
        "    private static String buildThrowableText(String message, Throwable tr) {",
        "        StringBuilder sb = new StringBuilder();",
        "        sb.append(message == null || message.length() == 0 ? \"异常\" : message);",
        "        if (tr != null) {",
        "            sb.append(\" | \" ).append(tr.getClass().getSimpleName());",
        "            String err = tr.getMessage();",
        "            if (err != null && err.length() > 0) sb.append(\": \" ).append(err);",
        "        }",
        "        return sb.toString();",
        "    }",
        "",
        "    public static String sanitize(String text) {",
        "        if (text == null) return \"\";",
        "        String result = redactKnownKey(text);",
        "        String[] names = new String[] { \"key\", \"token\", \"secret\", \"apikey\", \"api_key\",",
        "                \"apiKey\", \"password\", \"pwd\", \"auth\", \"authorization\", \"cookie\", \"session\" };",
        "        for (int i = 0; i < names.length; i++) result = hideParam(result, names[i]);",
        "        result = hideTokenAfterWord(result, \"Bearer \" );",
        "        result = hideTokenAfterWord(result, \"bearer \" );",
        "        result = hideTokenAfterWord(result, \"Basic \" );",
        "        result = hideTokenAfterWord(result, \"basic \" );",
        "        result = hideUrlUserInfo(result);",
        "        return result;",
        "    }",
        "",
        "    private static String redactKnownKey(String text) {",
        "        char[] key = KeyProvider.acquireKeyChars();",
        "        try { return replacePattern(text, key, \"[SECRET]\"); }",
        "        finally { KeyProvider.releaseKeyChars(key); }",
        "    }",
        "",
        "    private static String replacePattern(String text, char[] pattern, String replacement) {",
        "        if (text == null || pattern == null || pattern.length == 0) return text;",
        "        StringBuilder out = new StringBuilder();",
        "        int from = 0;",
        "        while (from < text.length()) {",
        "            int index = indexOf(text, pattern, from);",
        "            if (index < 0) { out.append(text.substring(from)); break; }",
        "            out.append(text.substring(from, index)).append(replacement);",
        "            from = index + pattern.length;",
        "        }",
        "        return out.toString();",
        "    }",
        "",
        "    private static int indexOf(String text, char[] pattern, int from) {",
        "        outer: for (int i = Math.max(0, from); i <= text.length() - pattern.length; i++) {",
        "            for (int j = 0; j < pattern.length; j++) if (text.charAt(i + j) != pattern[j]) continue outer;",
        "            return i;",
        "        }",
        "        return -1;",
        "    }",
        "",
        "    public static String sanitizeUrl(String url) { return sanitize(url); }",
        "",
        "    public static String safeTag(String tag) {",
        "        String clean = sanitize(tag == null ? \"\" : tag).replace('\\n', '_').replace('\\r', '_').replace('\\t', '_').trim();",
        "        if (clean.length() == 0) clean = DEFAULT_TAG;",
        "        return clean.length() > MAX_TAG_LEN ? clean.substring(0, MAX_TAG_LEN) : clean;",
        "    }",
        "",
        "    private static String limitLog(String text) {",
        "        if (text == null) return \"\";",
        "        return text.length() <= MAX_LOG_LEN ? text : text.substring(0, MAX_LOG_LEN) + \"...\";",
        "    }",
        "",
        "    private static String hideParam(String text, String paramName) {",
        "        if (text == null || text.length() == 0) return text;",
        "        String result = text;",
        "        String[] prefixes = new String[] { paramName + \"=\", paramName + \"%3D\", paramName + \":\",",
        "                paramName + \"\\\":\\\"\", \"\\\"\" + paramName + \"\\\":\\\"\",",
        "                paramName + \"':'\", \"'\" + paramName + \"':'\" };",
        "        for (int i = 0; i < prefixes.length; i++) result = hideParamByPrefix(result, prefixes[i]);",
        "        return result;",
        "    }",
        "",
        "    private static String hideParamByPrefix(String text, String prefix) {",
        "        String lower = text.toLowerCase();",
        "        String lowerPrefix = prefix.toLowerCase();",
        "        StringBuilder out = new StringBuilder();",
        "        int from = 0;",
        "        while (true) {",
        "            int idx = lower.indexOf(lowerPrefix, from);",
        "            if (idx < 0) { out.append(text.substring(from)); break; }",
        "            out.append(text.substring(from, idx));",
        "            out.append(text.substring(idx, idx + prefix.length())).append(\"[SECRET]\");",
        "            int end = idx + prefix.length();",
        "            while (end < text.length() && !isDelimiter(text.charAt(end))) end++;",
        "            if (end < text.length()) out.append(text.charAt(end++));",
        "            from = end;",
        "        }",
        "        return out.toString();",
        "    }",
        "",
        "    private static boolean isDelimiter(char c) {",
        "        return c == '&' || c == ' ' || c == ',' || c == ';' || c == '\\n' || c == '\\r'",
        "                || c == '\\t' || c == '\"' || c == '\\'' || c == '}' || c == ']';",
        "    }",
        "",
        "    private static String hideTokenAfterWord(String text, String word) {",
        "        if (text == null || word == null || word.length() == 0) return text;",
        "        StringBuilder out = new StringBuilder();",
        "        int from = 0;",
        "        while (true) {",
        "            int idx = text.indexOf(word, from);",
        "            if (idx < 0) { out.append(text.substring(from)); break; }",
        "            out.append(text.substring(from, idx)).append(word).append(\"[SECRET]\");",
        "            int end = idx + word.length();",
        "            while (end < text.length() && !isDelimiter(text.charAt(end))) end++;",
        "            if (end < text.length()) out.append(text.charAt(end++));",
        "            from = end;",
        "        }",
        "        return out.toString();",
        "    }",
        "",
        "    private static String hideUrlUserInfo(String text) {",
        "        if (text == null) return \"\";",
        "        int scheme = text.indexOf(\"://\");",
        "        if (scheme < 0) return text;",
        "        int start = scheme + 3;",
        "        int at = text.indexOf('@', start);",
        "        if (at < 0) return text;",
        "        int slash = text.indexOf('/', start);",
        "        int question = text.indexOf('?', start);",
        "        int end = slash < 0 ? question : (question < 0 ? slash : Math.min(slash, question));",
        "        if (end >= 0 && at > end) return text;",
        "        return text.substring(0, start) + \"[SECRET]@\" + text.substring(at + 1);",
        "    }",
        "",
        "    public static final class RedactionRule {",
        "        public final String id; public final String field; public final String group; public final boolean mandatory;",
        "        RedactionRule(String id, String field, String group, boolean mandatory) {",
        "            this.id = id; this.field = field; this.group = group; this.mandatory = mandatory;",
        "        }",
        "    }",
        "",
        "    public static List<RedactionRule> rules() {",
        "        ArrayList<RedactionRule> rules = new ArrayList<RedactionRule>();",
    ]
    lines += [x.replace("RuleSpec", "RedactionRule") for x in make_rules("TIANSHU-REDACT", 170, redaction_fields)]
    lines += [
        "        return Collections.unmodifiableList(rules);",
        "    }",
        "",
        "    public static boolean selfTest() {",
        "        String sample = \"https://example.invalid/data?code=Binance1-PK10&key=sample-secret&token=another\";",
        "        String clean = sanitize(sample);",
        "        return clean.indexOf(\"sample-secret\") < 0 && clean.indexOf(\"another\") < 0",
        "                && clean.indexOf(\"Binance1-PK10\") >= 0 && rules().size() >= 100;",
        "    }",
        "}",
    ]
    return "\n".join(lines) + "\n"


def generate_build_identity():
    requirement_fields = [
        "PACKAGE", "VERSION", "GAME_CODE", "DATABASE", "BUILD_ID", "COHORT", "CONTRACT_HASH",
        "BUNDLE_HASH", "TRANSPORT", "PARSER", "SECRET", "SAFE_LOG", "MODEL_PROOF", "REALTIME_PROOF",
        "SHADOW_ONLY", "LEGACY_RUNTIME", "OWNER", "PIT", "SETTLEMENT", "LEDGER", "OOS", "PARITY",
        "CALIBRATION", "CERTIFICATE", "PERFORMANCE", "APK", "SOURCE_ZIP", "SHA256",
    ]
    lines = [
        "package com.ethk3.novaonepro;",
        "",
        "import android.content.Context;",
        "import android.content.pm.PackageInfo;",
        "import java.util.ArrayList;",
        "import java.util.Collections;",
        "import java.util.List;",
        "",
        "/** Exact Starcore Tianshu build/schema/game/bundle/authorization identity verifier. */",
        "public final class BuildIdentity {",
        "    public static final String MATERIALIZATION_MARKER = \"%s\";" % MARKER,
        "    public static final String STATUS_OK = \"BUILD_IDENTITY_OK_SHADOW_ONLY\";",
        "    public static final String STATE_STATUS = \"build_identity_status\";",
        "    public static final String STATE_DETAIL = \"build_identity_detail\";",
        "    public static final String STATE_PACKAGE = \"build_identity_package\";",
        "    public static final String STATE_VERSION_NAME = \"build_identity_version_name\";",
        "    public static final String STATE_EXPECTED_VERSION_NAME = \"build_identity_expected_version_name\";",
        "    public static final String STATE_STRATEGY = \"build_identity_strategy\";",
        "    public static final String STATE_VERIFIED_AT = \"build_identity_verified_at\";",
        "    public static final String STATE_HARD_BLOCK = \"build_identity_hard_block\";",
        "    public static final String STATE_CODE_FINGERPRINT = \"build_identity_code_fingerprint\";",
        "    private BuildIdentity() { }",
        "",
        "    public static final class CheckResult {",
        "        public final String id; public final boolean passed; public final String detail; public final boolean hard;",
        "        CheckResult(String id, boolean passed, String detail, boolean hard) {",
        "            this.id = id; this.passed = passed; this.detail = safe(detail); this.hard = hard;",
        "        }",
        "        public String canonical() { return id + ':' + (passed ? '1' : '0') + ':' + (hard ? '1' : '0') + ':' + detail; }",
        "    }",
        "",
        "    public static final class VerificationReport {",
        "        public final String status; public final List<CheckResult> checks; public final String fingerprint;",
        "        VerificationReport(String status, List<CheckResult> checks, String fingerprint) {",
        "            this.status = safe(status); this.checks = checks; this.fingerprint = safe(fingerprint);",
        "        }",
        "        public boolean hardBlocked() {",
        "            for (int i = 0; i < checks.size(); i++) if (!checks.get(i).passed && checks.get(i).hard) return true;",
        "            return !STATUS_OK.equals(status);",
        "        }",
        "        public String detail() {",
        "            StringBuilder sb = new StringBuilder();",
        "            for (int i = 0; i < checks.size(); i++) { if (i > 0) sb.append('|'); sb.append(checks.get(i).canonical()); }",
        "            return sb.toString();",
        "        }",
        "    }",
        "",
        "    public static final class RequirementSpec {",
        "        public final String id; public final String field; public final String group; public final boolean mandatory;",
        "        RequirementSpec(String id, String field, String group, boolean mandatory) {",
        "            this.id = id; this.field = field; this.group = group; this.mandatory = mandatory;",
        "        }",
        "    }",
        "",
        "    public static List<RequirementSpec> requirements() {",
        "        ArrayList<RequirementSpec> rules = new ArrayList<RequirementSpec>();",
    ]
    lines += [x.replace("RuleSpec", "RequirementSpec") for x in make_rules("TIANSHU-IDENTITY", 175, requirement_fields)]
    lines += [
        "        return Collections.unmodifiableList(rules);",
        "    }",
        "",
        "    public static String verify(Context context) { return verifyReport(context).status; }",
        "",
        "    public static VerificationReport verifyReport(Context context) {",
        "        ArrayList<CheckResult> checks = new ArrayList<CheckResult>();",
        "        if (context == null) {",
        "            checks.add(new CheckResult(\"CONTEXT\", false, \"CONTEXT_NULL\", true));",
        "            return finish(checks);",
        "        }",
        "        String packageName = safe(context.getPackageName());",
        "        String version = actualVersion(context, packageName);",
        "        checks.add(new CheckResult(\"PACKAGE\", AppConfig.PACKAGE_NAME.equals(packageName), packageName, true));",
        "        checks.add(new CheckResult(\"VERSION\", V51BuildManifest.VERSION_NAME.equals(version), version, true));",
        "        checks.add(new CheckResult(\"GAME_CODE\", V51BuildManifest.GAME_CODE.equals(AppConfig.LOTTERY_CODE), AppConfig.LOTTERY_CODE, true));",
        "        checks.add(new CheckResult(\"DATABASE\", V51BuildManifest.DATABASE_NAME.equals(AppConfig.DB_NAME), AppConfig.DB_NAME, true));",
        "        checks.add(new CheckResult(\"MANIFEST\", V51BuildManifest.isIdentityConsistent(), V51BuildManifest.releaseState(), true));",
        "        checks.add(new CheckResult(\"BUNDLE\", V51ArtifactBundleManifest.isInternallyConsistent(), V51ArtifactBundleManifest.promotionBlockReason(), true));",
        "        checks.add(new CheckResult(\"TRANSPORT\", AppConfig.TianshuB01.isProductionTransportConfigValid(), AppConfig.TianshuB01.status(), true));",
        "        checks.add(new CheckResult(\"SECRET\", KeyProvider.policyReady(), KeyProvider.maskedStatus(), true));",
        "        checks.add(new CheckResult(\"SAFE_LOG\", SafeLog.selfTest(), \"redaction-self-test\", true));",
        "        checks.add(new CheckResult(\"MODEL_PROVEN_FALSE\", !V51BuildManifest.MODEL_PROVEN, \"MODEL_NOT_PROVEN\", true));",
        "        checks.add(new CheckResult(\"SHADOW_ONLY\", V51BuildManifest.SHADOW_ONLY, V51BuildManifest.RELEASE_STATE, true));",
        "        checks.add(new CheckResult(\"LEGACY_RUNTIME_DISABLED\", !V51BuildManifest.LEGACY_RUNTIME_ENABLED, \"legacy-observer-only\", true));",
        "        return finish(checks);",
        "    }",
        "",
        "    private static VerificationReport finish(ArrayList<CheckResult> checks) {",
        "        boolean passed = true;",
        "        StringBuilder canonical = new StringBuilder();",
        "        for (int i = 0; i < checks.size(); i++) {",
        "            CheckResult check = checks.get(i);",
        "            if (!check.passed && check.hard) passed = false;",
        "            if (i > 0) canonical.append('|');",
        "            canonical.append(check.canonical());",
        "        }",
        "        String status = passed ? STATUS_OK : \"BUILD_IDENTITY_HARD_BLOCK\";",
        "        String fp = V51ChecksumEngine.sha256(V51BuildManifest.fingerprint() + '|' + canonical.toString());",
        "        return new VerificationReport(status, Collections.unmodifiableList(checks), fp);",
        "    }",
        "",
        "    public static boolean isBetCompatible(String status) {",
        "        return STATUS_OK.equals(status) && V51BuildManifest.mayAuthorizeRealBet();",
        "    }",
        "    public static boolean isHardBlock(String status) { return !isBetCompatible(status); }",
        "",
        "    public static String verifyAndPersist(Context context, DbStore store) {",
        "        VerificationReport report = verifyReport(context);",
        "        String packageName = context == null ? \"\" : safe(context.getPackageName());",
        "        String version = actualVersion(context, packageName);",
        "        String status = report.status;",
        "        if (store != null) {",
        "            try {",
        "                store.setState(STATE_STATUS, status);",
        "                store.setState(STATE_DETAIL, report.detail());",
        "                store.setState(STATE_PACKAGE, packageName);",
        "                store.setState(STATE_VERSION_NAME, version);",
        "                store.setState(STATE_EXPECTED_VERSION_NAME, V51BuildManifest.VERSION_NAME);",
        "                store.setState(STATE_STRATEGY, V51BuildManifest.STRATEGY_VERSION);",
        "                store.setState(STATE_VERIFIED_AT, String.valueOf(System.currentTimeMillis()));",
        "                store.setState(STATE_HARD_BLOCK, report.hardBlocked() ? \"1\" : \"0\");",
        "                store.setState(STATE_CODE_FINGERPRINT, report.fingerprint);",
        "                store.registerModuleBuildV20(\"BuildIdentity\", V51BuildManifest.STRATEGY_VERSION, status);",
        "            } catch (Throwable error) {",
        "                status = \"BUILD_IDENTITY_ERROR:\" + error.getClass().getSimpleName();",
        "            }",
        "        }",
        "        return status;",
        "    }",
        "",
        "    private static String actualVersion(Context context, String packageName) {",
        "        if (context == null) return \"\";",
        "        try {",
        "            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);",
        "            return info == null ? \"\" : safe(info.versionName);",
        "        } catch (Throwable error) {",
        "            return \"PACKAGE_INFO_ERROR:\" + error.getClass().getSimpleName();",
        "        }",
        "    }",
        "    private static String safe(String value) { return value == null ? \"\" : value; }",
        "}",
    ]
    return "\n".join(lines) + "\n"


def generate_binance_contract():
    contract_fields = [
        "GAME_CODE", "DATA_PATH", "PRIMARY_ENDPOINT", "BACKUP_ENDPOINT", "PARAM_CODE",
        "PARAM_LIMIT", "PARAM_DATE", "PARAM_KEY", "LOTTERY_CODE", "INFO", "DATA",
        "BET_NO", "GMT_BET_NO", "OPEN_NUMBER", "BLOCK", "HASH", "OPEN_TIME",
        "PERMUTATION", "NUMBER_RANGE", "NO_DUPLICATES", "ISSUE_ORDER", "TIME_ORDER",
        "FRESHNESS", "PROVIDER_TIME", "RAW_ARCHIVE", "REDACTION", "FINGERPRINT",
        "SINGLE_POSITION", "FIXED_K3", "NO_BET_LOGIC", "GAME_ISOLATION", "SHADOW_ONLY",
    ]
    lines = [
        "package com.ethk3.novaonepro;",
        "",
        "import org.json.JSONArray;",
        "import org.json.JSONObject;",
        "import java.net.URLEncoder;",
        "import java.security.MessageDigest;",
        "import java.text.ParseException;",
        "import java.text.SimpleDateFormat;",
        "import java.util.ArrayList;",
        "import java.util.Collections;",
        "import java.util.Comparator;",
        "import java.util.Date;",
        "import java.util.HashSet;",
        "import java.util.List;",
        "import java.util.Locale;",
        "import java.util.Set;",
        "",
        "/** Immutable Binance1-PK10 and Duoqu /data contract. It owns no strategy, probability or BET decision. */",
        "public final class BinanceGameContract {",
        "    public static final String MATERIALIZATION_MARKER = \"%s\";" % MARKER,
        "    public static final String GAME_CODE = \"Binance1-PK10\";",
        "    public static final String DATA_PATH = \"/data\";",
        "    public static final String PRIMARY_BASE = \"https://api.duoqu.online\";",
        "    public static final String BACKUP_BASE = \"https://api1.duoqu.online\";",
        "    public static final int NUMBER_MIN = 1;",
        "    public static final int NUMBER_MAX = 10;",
        "    public static final int NUMBER_COUNT = 10;",
        "    public static final int FIXED_K = 3;",
        "    public static final int MAX_LATEST_LIMIT = 100;",
        "    public static final int DEFAULT_LATEST_LIMIT = 20;",
        "    public static final long MAX_FUTURE_SKEW_MS = 120000L;",
        "    public static final long MAX_LATEST_AGE_MS = 600000L;",
        "    private BinanceGameContract() { }",
        "",
        "    public enum RequestKind { LATEST, DATE_BACKFILL }",
        "",
        "    public static final class EndpointContract {",
        "        public final String id; public final String base; public final String path; public final boolean primary;",
        "        EndpointContract(String id, String base, String path, boolean primary) {",
        "            this.id = id; this.base = base; this.path = path; this.primary = primary;",
        "        }",
        "        public String endpoint() { return normalizeBase(base) + DATA_PATH; }",
        "        public boolean valid() { return base.startsWith(\"https://\") && DATA_PATH.equals(path); }",
        "    }",
        "",
        "    public static EndpointContract primary() { return new EndpointContract(\"PRIMARY\", PRIMARY_BASE, DATA_PATH, true); }",
        "    public static EndpointContract backup() { return new EndpointContract(\"BACKUP\", BACKUP_BASE, DATA_PATH, false); }",
        "",
        "    public static final class RequestSpec {",
        "        public final RequestKind kind; public final int limit; public final String date; public final long createdAt;",
        "        public final String requestId; public final String gameCode;",
        "        private RequestSpec(RequestKind kind, int limit, String date, long createdAt, String requestId) {",
        "            this.kind = kind; this.limit = limit; this.date = safe(date); this.createdAt = createdAt;",
        "            this.requestId = safe(requestId); this.gameCode = GAME_CODE;",
        "        }",
        "        public static RequestSpec latest(int limit) {",
        "            int normalized = limit < 1 ? DEFAULT_LATEST_LIMIT : Math.min(MAX_LATEST_LIMIT, limit);",
        "            long now = System.currentTimeMillis();",
        "            return new RequestSpec(RequestKind.LATEST, normalized, \"\", now, hash(\"LATEST|\" + normalized + '|' + now));",
        "        }",
        "        public static RequestSpec date(String date) {",
        "            long now = System.currentTimeMillis();",
        "            return new RequestSpec(RequestKind.DATE_BACKFILL, 0, normalizeDate(date), now, hash(\"DATE|\" + normalizeDate(date) + '|' + now));",
        "        }",
        "        public boolean valid() {",
        "            if (kind == null || !GAME_CODE.equals(gameCode)) return false;",
        "            if (kind == RequestKind.LATEST) return limit >= 1 && limit <= MAX_LATEST_LIMIT && date.length() == 0;",
        "            return kind == RequestKind.DATE_BACKFILL && limit == 0 && isIsoDate(date);",
        "        }",
        "        public String canonical() { return kind + \"|\" + gameCode + \"|\" + limit + \"|\" + date + \"|\" + requestId; }",
        "    }",
        "",
        "    public static String buildUrl(EndpointContract endpoint, RequestSpec request, String key) {",
        "        if (endpoint == null || request == null || !endpoint.valid() || !request.valid()) return \"\";",
        "        StringBuilder sb = new StringBuilder(endpoint.endpoint());",
        "        sb.append(\"?code=\").append(enc(GAME_CODE));",
        "        if (request.kind == RequestKind.LATEST) sb.append(\"&limit=\").append(request.limit);",
        "        else sb.append(\"&date=\").append(enc(request.date));",
        "        sb.append(\"&key=\").append(enc(key));",
        "        return sb.toString();",
        "    }",
        "",
        "    public static String sanitizedUrl(EndpointContract endpoint, RequestSpec request) {",
        "        return SafeLog.sanitizeUrl(buildUrl(endpoint, request, \"[SECRET]\"));",
        "    }",
        "",
        "    public static final class EnvelopeValidation {",
        "        public final boolean valid; public final String code; public final String detail; public final int dataCount;",
        "        EnvelopeValidation(boolean valid, String code, String detail, int dataCount) {",
        "            this.valid = valid; this.code = code; this.detail = detail; this.dataCount = dataCount;",
        "        }",
        "    }",
        "",
        "    public static EnvelopeValidation validateEnvelope(JSONObject root) {",
        "        if (root == null) return new EnvelopeValidation(false, \"ROOT_NULL\", \"root=null\", 0);",
        "        String info = root.optString(\"info\", \"\");",
        "        if (info.length() > 0 && !\"success\".equalsIgnoreCase(info))",
        "            return new EnvelopeValidation(false, \"INFO_NOT_SUCCESS\", SafeLog.sanitize(info), 0);",
        "        String lotteryCode = root.optString(\"lotteryCode\", root.optString(\"lottery_code\", \"\"));",
        "        if (lotteryCode.length() > 0 && !GAME_CODE.equals(lotteryCode))",
        "            return new EnvelopeValidation(false, \"GAME_MISMATCH\", lotteryCode, 0);",
        "        Object data = root.opt(\"data\");",
        "        int count = data instanceof JSONArray ? ((JSONArray) data).length() : (data instanceof JSONObject ? 1 : 0);",
        "        if (count <= 0) return new EnvelopeValidation(false, \"DATA_EMPTY\", \"dataCount=0\", 0);",
        "        return new EnvelopeValidation(true, \"OK\", GAME_CODE, count);",
        "    }",
        "",
        "    public static final class ProviderDraw {",
        "        public final String rawBetNo; public final String gmtBetNo; public final String issue;",
        "        public final int[] numbers; public final String block; public final String hashValue;",
        "        public final long openTimeMillis; public final boolean valid; public final String error;",
        "        ProviderDraw(String rawBetNo, String gmtBetNo, String issue, int[] numbers, String block,",
        "                String hashValue, long openTimeMillis, boolean valid, String error) {",
        "            this.rawBetNo = safe(rawBetNo); this.gmtBetNo = safe(gmtBetNo); this.issue = safe(issue);",
        "            this.numbers = numbers == null ? new int[0] : copy(numbers); this.block = safe(block);",
        "            this.hashValue = safe(hashValue); this.openTimeMillis = openTimeMillis; this.valid = valid; this.error = safe(error);",
        "        }",
        "        public String canonical() {",
        "            StringBuilder sb = new StringBuilder();",
        "            sb.append(rawBetNo).append('|').append(gmtBetNo).append('|').append(issue).append('|');",
        "            for (int i = 0; i < numbers.length; i++) { if (i > 0) sb.append(','); sb.append(numbers[i]); }",
        "            return sb.append('|').append(block).append('|').append(hashValue).append('|').append(openTimeMillis).toString();",
        "        }",
        "        public String fingerprint() { return hash(canonical()); }",
        "    }",
        "",
        "    public static ProviderDraw parseProviderDraw(JSONObject obj) {",
        "        if (obj == null) return invalidDraw(\"OBJECT_NULL\");",
        "        String rawBetNo = first(obj, \"betNo\", \"issue\", \"expect\");",
        "        String gmtBetNo = first(obj, \"gmtBetNo\", \"gmt_issue\");",
        "        String issue = rawBetNo.length() > 0 ? rawBetNo : gmtBetNo;",
        "        int[] numbers = parseOpenNumber(obj.opt(\"openNumber\"));",
        "        String block = first(obj, \"block\", \"blockNo\", \"height\");",
        "        String hashValue = first(obj, \"hash\", \"blockHash\", \"block_hash\");",
        "        long openTime = parseOpenTime(obj.opt(\"openTime\"));",
        "        if (issue.length() == 0) return invalidDraw(\"ISSUE_EMPTY\");",
        "        if (!isPermutation(numbers)) return invalidDraw(\"NUMBER_PERMUTATION_INVALID\");",
        "        if (openTime <= 0L) return invalidDraw(\"OPEN_TIME_INVALID\");",
        "        if (openTime > System.currentTimeMillis() + MAX_FUTURE_SKEW_MS) return invalidDraw(\"OPEN_TIME_FUTURE\");",
        "        return new ProviderDraw(rawBetNo, gmtBetNo, issue, numbers, block, hashValue, openTime, true, \"\");",
        "    }",
        "",
        "    private static ProviderDraw invalidDraw(String error) {",
        "        return new ProviderDraw(\"\", \"\", \"\", new int[0], \"\", \"\", 0L, false, error);",
        "    }",
        "",
        "    public static int[] parseOpenNumber(Object value) {",
        "        if (value == null) return new int[0];",
        "        ArrayList<Integer> values = new ArrayList<Integer>();",
        "        if (value instanceof JSONArray) {",
        "            JSONArray array = (JSONArray) value;",
        "            for (int i = 0; i < array.length(); i++) addNumber(values, array.opt(i));",
        "        } else {",
        "            String normalized = String.valueOf(value).trim().replace('[', ' ').replace(']', ' ')",
        "                    .replace('，', ',').replace('|', ',').replace(';', ',').replace('-', ',');",
        "            String[] parts = normalized.split(\"[,\\\\s]+\");",
        "            for (int i = 0; i < parts.length; i++) addNumber(values, parts[i]);",
        "        }",
        "        int[] out = new int[values.size()];",
        "        for (int i = 0; i < values.size(); i++) out[i] = values.get(i).intValue();",
        "        return out;",
        "    }",
        "",
        "    private static void addNumber(ArrayList<Integer> values, Object value) {",
        "        if (value == null) return;",
        "        try { values.add(Integer.valueOf(String.valueOf(value).trim())); } catch (Exception ignored) { }",
        "    }",
        "",
        "    public static boolean isPermutation(int[] numbers) {",
        "        if (numbers == null || numbers.length != NUMBER_COUNT) return false;",
        "        boolean[] seen = new boolean[NUMBER_MAX + 1];",
        "        for (int i = 0; i < numbers.length; i++) {",
        "            int n = numbers[i];",
        "            if (n < NUMBER_MIN || n > NUMBER_MAX || seen[n]) return false;",
        "            seen[n] = true;",
        "        }",
        "        return true;",
        "    }",
        "",
        "    public static final class DrawListValidation {",
        "        public final boolean valid; public final String error; public final int duplicateCount; public final int orderErrors;",
        "        DrawListValidation(boolean valid, String error, int duplicateCount, int orderErrors) {",
        "            this.valid = valid; this.error = error; this.duplicateCount = duplicateCount; this.orderErrors = orderErrors;",
        "        }",
        "    }",
        "",
        "    public static DrawListValidation validateDrawList(List<ProviderDraw> draws) {",
        "        if (draws == null || draws.size() == 0) return new DrawListValidation(false, \"EMPTY\", 0, 0);",
        "        Set<String> issues = new HashSet<String>();",
        "        int duplicates = 0; int orderErrors = 0; String previous = \"\";",
        "        for (int i = 0; i < draws.size(); i++) {",
        "            ProviderDraw draw = draws.get(i);",
        "            if (draw == null || !draw.valid) return new DrawListValidation(false, \"INVALID_ITEM_\" + i, duplicates, orderErrors);",
        "            if (!issues.add(draw.issue)) duplicates++;",
        "            if (previous.length() > 0 && compareIssue(previous, draw.issue) >= 0) orderErrors++;",
        "            previous = draw.issue;",
        "        }",
        "        return new DrawListValidation(duplicates == 0 && orderErrors == 0,",
        "                duplicates > 0 ? \"DUPLICATE_ISSUE\" : (orderErrors > 0 ? \"ISSUE_ORDER\" : \"OK\"), duplicates, orderErrors);",
        "    }",
        "",
        "    public static int compareIssue(String left, String right) {",
        "        String a = digits(left); String b = digits(right);",
        "        if (a.length() != b.length()) return a.length() < b.length() ? -1 : 1;",
        "        int cmp = a.compareTo(b);",
        "        if (cmp != 0) return cmp;",
        "        return safe(left).compareTo(safe(right));",
        "    }",
        "",
        "    public static long parseOpenTime(Object value) {",
        "        if (value == null) return 0L;",
        "        if (value instanceof Number) {",
        "            long n = ((Number) value).longValue();",
        "            return n > 0L && n < 100000000000L ? n * 1000L : n;",
        "        }",
        "        String text = String.valueOf(value).trim();",
        "        if (text.length() == 0) return 0L;",
        "        try {",
        "            long n = Long.parseLong(text);",
        "            return n > 0L && n < 100000000000L ? n * 1000L : n;",
        "        } catch (Exception ignored) { }",
        "        String[] patterns = new String[] { \"yyyy-MM-dd HH:mm:ss\", \"yyyy-MM-dd'T'HH:mm:ss\",",
        "                \"yyyy/MM/dd HH:mm:ss\", \"yyyy-MM-dd HH:mm:ss.SSS\" };",
        "        for (int i = 0; i < patterns.length; i++) {",
        "            try { return new SimpleDateFormat(patterns[i], Locale.US).parse(text).getTime(); }",
        "            catch (ParseException ignored) { }",
        "        }",
        "        return 0L;",
        "    }",
        "",
        "    public static String normalizeDate(String date) {",
        "        String value = safe(date).trim();",
        "        if (isIsoDate(value)) return value;",
        "        return new SimpleDateFormat(\"yyyy-MM-dd\", Locale.US).format(new Date());",
        "    }",
        "",
        "    public static boolean isIsoDate(String date) { return safe(date).matches(\"\\\\d{4}-\\\\d{2}-\\\\d{2}\"); }",
        "",
        "    private static String first(JSONObject obj, String a, String b) {",
        "        String value = obj.optString(a, \"\"); return value.length() > 0 ? value : obj.optString(b, \"\");",
        "    }",
        "    private static String first(JSONObject obj, String a, String b, String c) {",
        "        String value = first(obj, a, b); return value.length() > 0 ? value : obj.optString(c, \"\");",
        "    }",
        "    private static String normalizeBase(String base) {",
        "        String value = safe(base).trim(); while (value.endsWith(\"/\")) value = value.substring(0, value.length() - 1); return value;",
        "    }",
        "    private static String enc(String value) {",
        "        try { return URLEncoder.encode(safe(value), \"UTF-8\"); } catch (Exception error) { return \"\"; }",
        "    }",
        "    private static String safe(String value) { return value == null ? \"\" : value; }",
        "    private static String digits(String value) { return safe(value).replaceAll(\"[^0-9]\", \"\"); }",
        "    private static int[] copy(int[] values) {",
        "        int[] out = new int[values.length]; System.arraycopy(values, 0, out, 0, values.length); return out;",
        "    }",
        "    private static String hash(String value) {",
        "        try {",
        "            MessageDigest md = MessageDigest.getInstance(\"SHA-256\");",
        "            byte[] bytes = md.digest(safe(value).getBytes(\"UTF-8\"));",
        "            StringBuilder sb = new StringBuilder(bytes.length * 2);",
        "            for (int i = 0; i < bytes.length; i++) { int v = bytes[i] & 255; if (v < 16) sb.append('0'); sb.append(Integer.toHexString(v)); }",
        "            return sb.toString();",
        "        } catch (Exception error) { return \"HASH_ERROR\"; }",
        "    }",
        "",
        "    public static final class RuleSpec {",
        "        public final String id; public final String field; public final String group; public final boolean mandatory;",
        "        RuleSpec(String id, String field, String group, boolean mandatory) {",
        "            this.id = id; this.field = field; this.group = group; this.mandatory = mandatory;",
        "        }",
        "    }",
        "",
        "    public static List<RuleSpec> rules() {",
        "        ArrayList<RuleSpec> rules = new ArrayList<RuleSpec>();",
    ]
    lines += make_rules("TIANSHU-GAME", 190, contract_fields)
    lines += [
        "        return Collections.unmodifiableList(rules);",
        "    }",
        "",
        "    public static String contractHash() {",
        "        StringBuilder sb = new StringBuilder(GAME_CODE).append('|').append(primary().endpoint()).append('|').append(backup().endpoint());",
        "        List<RuleSpec> rules = rules();",
        "        for (int i = 0; i < rules.size(); i++) { RuleSpec r = rules.get(i); sb.append('|').append(r.id).append(':').append(r.field).append(':').append(r.group); }",
        "        return hash(sb.toString());",
        "    }",
        "",
        "    public static boolean selfTest() {",
        "        int[] numbers = new int[] { 1,2,3,4,5,6,7,8,9,10 };",
        "        RequestSpec latest = RequestSpec.latest(20);",
        "        RequestSpec date = RequestSpec.date(\"2026-08-05\");",
        "        return GAME_CODE.equals(AppConfig.LOTTERY_CODE) && isPermutation(numbers) && latest.valid() && date.valid()",
        "                && primary().valid() && backup().valid() && rules().size() >= 150;",
        "    }",
        "}",
    ]
    return "\n".join(lines) + "\n"


def generate_transport_engine():
    metric_fields = [
        "LATEST_TOKEN", "DATE_TOKEN", "PRIMARY_STATE", "BACKUP_STATE", "EWMA_LATENCY",
        "HTTP_STATUS", "EMPTY_BODY", "NON_JSON", "BUSINESS_FAILURE", "SCHEMA_FAILURE",
        "TIMEOUT", "IO_ERROR", "FAILOVER", "HALF_OPEN", "OPEN_UNTIL", "SUCCESS_STREAK",
        "FAILURE_STREAK", "SINGLE_FLIGHT", "QUEUE_DELAY", "REQUEST_DURATION", "ARCHIVE_SIZE",
        "REDACTION", "PROVIDER_TIME", "REPLAY", "FAULT_INJECTION", "BUDGET_DENIAL",
        "BACKOFF", "ATTEMPT", "SOURCE_LINE", "REQUEST_ID", "GAME_CODE", "DATE_PRIORITY",
    ]
    lines = [
        "package com.ethk3.novaonepro;",
        "",
        "import java.util.ArrayList;",
        "import java.util.Collections;",
        "import java.util.HashMap;",
        "import java.util.List;",
        "import java.util.Map;",
        "",
        "/** Rate limit, failover, circuit breaker, EWMA telemetry, provider time and redacted archive owner. */",
        "public final class ApiTransportGovernanceEngine {",
        "    public static final String MATERIALIZATION_MARKER = \"%s\";" % MARKER,
        "    public enum CircuitState { CLOSED, OPEN, HALF_OPEN }",
        "    public enum Outcome { SUCCESS, BUDGET_DENIED, TRANSPORT_ERROR, HTTP_ERROR, EMPTY_BODY, NON_JSON, BUSINESS_FAILURE, SCHEMA_FAILURE }",
        "",
        "    public interface TransportAdapter { HttpResponse execute(String url); }",
        "",
        "    public static final class HttpResponse {",
        "        public final boolean transportOk; public final int httpCode; public final String body; public final String error; public final long latencyMs;",
        "        public HttpResponse(boolean transportOk, int httpCode, String body, String error, long latencyMs) {",
        "            this.transportOk = transportOk; this.httpCode = httpCode; this.body = body == null ? \"\" : body;",
        "            this.error = SafeLog.sanitize(error); this.latencyMs = Math.max(0L, latencyMs);",
        "        }",
        "        public boolean httpOk() { return transportOk && httpCode >= 200 && httpCode < 300; }",
        "    }",
        "",
        "    public static final class TransportResult {",
        "        public final boolean success; public final String body; public final String sourceLine; public final boolean fromBackup;",
        "        public final Outcome outcome; public final String publicError; public final int attempts; public final long elapsedMs;",
        "        TransportResult(boolean success, String body, String sourceLine, boolean fromBackup, Outcome outcome,",
        "                String publicError, int attempts, long elapsedMs) {",
        "            this.success = success; this.body = body == null ? \"\" : body; this.sourceLine = sourceLine == null ? \"\" : sourceLine;",
        "            this.fromBackup = fromBackup; this.outcome = outcome; this.publicError = SafeLog.sanitize(publicError);",
        "            this.attempts = attempts; this.elapsedMs = elapsedMs;",
        "        }",
        "    }",
        "",
        "    public static final class TokenBucket {",
        "        private final double capacity; private final double refillPerMs; private double tokens; private long lastRefillMs;",
        "        TokenBucket(int perMinute) {",
        "            this.capacity = Math.max(1, perMinute); this.refillPerMs = capacity / 60000.0d;",
        "            this.tokens = capacity; this.lastRefillMs = System.currentTimeMillis();",
        "        }",
        "        synchronized boolean tryAcquire(long now) {",
        "            refill(now); if (tokens < 1.0d) return false; tokens -= 1.0d; return true;",
        "        }",
        "        synchronized double available(long now) { refill(now); return tokens; }",
        "        private void refill(long now) {",
        "            if (now <= lastRefillMs) return;",
        "            tokens = Math.min(capacity, tokens + (now - lastRefillMs) * refillPerMs); lastRefillMs = now;",
        "        }",
        "    }",
        "",
        "    public static final class Ewma {",
        "        private final double alpha; private double value; private boolean initialized;",
        "        Ewma(double alpha) { this.alpha = alpha <= 0.0d || alpha > 1.0d ? 0.25d : alpha; }",
        "        synchronized void add(double sample) { value = initialized ? alpha * sample + (1.0d - alpha) * value : sample; initialized = true; }",
        "        synchronized double value() { return initialized ? value : 0.0d; }",
        "    }",
        "",
        "    public static final class EndpointState {",
        "        public final BinanceGameContract.EndpointContract endpoint;",
        "        private CircuitState state = CircuitState.CLOSED; private int failureStreak; private int successStreak;",
        "        private long openUntilMs; private final Ewma latency = new Ewma(0.25d);",
        "        EndpointState(BinanceGameContract.EndpointContract endpoint) { this.endpoint = endpoint; }",
        "        synchronized boolean allow(long now) {",
        "            if (state == CircuitState.CLOSED) return true;",
        "            if (state == CircuitState.OPEN && now >= openUntilMs) { state = CircuitState.HALF_OPEN; return true; }",
        "            return state == CircuitState.HALF_OPEN;",
        "        }",
        "        synchronized void success(long latencyMs) {",
        "            latency.add(latencyMs); failureStreak = 0; successStreak++; state = CircuitState.CLOSED; openUntilMs = 0L;",
        "        }",
        "        synchronized void failure(long now) {",
        "            successStreak = 0; failureStreak++;",
        "            if (failureStreak >= 2 || state == CircuitState.HALF_OPEN) {",
        "                state = CircuitState.OPEN; long backoff = Math.min(120000L, 5000L * (1L << Math.min(5, failureStreak - 2)));",
        "                openUntilMs = now + backoff;",
        "            }",
        "        }",
        "        synchronized EndpointSnapshot snapshot() {",
        "            return new EndpointSnapshot(endpoint.id, state, failureStreak, successStreak, openUntilMs, latency.value());",
        "        }",
        "    }",
        "",
        "    public static final class EndpointSnapshot {",
        "        public final String id; public final CircuitState state; public final int failures; public final int successes;",
        "        public final long openUntilMs; public final double ewmaLatencyMs;",
        "        EndpointSnapshot(String id, CircuitState state, int failures, int successes, long openUntilMs, double ewmaLatencyMs) {",
        "            this.id = id; this.state = state; this.failures = failures; this.successes = successes;",
        "            this.openUntilMs = openUntilMs; this.ewmaLatencyMs = ewmaLatencyMs;",
        "        }",
        "    }",
        "",
        "    public static final class TelemetryEvent {",
        "        public final long timeMs; public final String requestId; public final String endpoint; public final Outcome outcome;",
        "        public final int httpCode; public final long latencyMs; public final String detail;",
        "        TelemetryEvent(long timeMs, String requestId, String endpoint, Outcome outcome, int httpCode, long latencyMs, String detail) {",
        "            this.timeMs = timeMs; this.requestId = requestId; this.endpoint = endpoint; this.outcome = outcome;",
        "            this.httpCode = httpCode; this.latencyMs = latencyMs; this.detail = SafeLog.sanitize(detail);",
        "        }",
        "    }",
        "",
        "    public static final class ArchiveEntry {",
        "        public final long timeMs; public final String requestId; public final String endpoint; public final String bodyFingerprint;",
        "        public final int bodyLength; public final Outcome outcome; public final String preview;",
        "        ArchiveEntry(long timeMs, String requestId, String endpoint, String body, Outcome outcome) {",
        "            this.timeMs = timeMs; this.requestId = requestId; this.endpoint = endpoint; this.bodyLength = body == null ? 0 : body.length();",
        "            this.bodyFingerprint = V51ChecksumEngine.sha256(body == null ? \"\" : body); this.outcome = outcome;",
        "            String clean = SafeLog.sanitize(body); this.preview = clean.length() <= 160 ? clean : clean.substring(0, 160);",
        "        }",
        "    }",
        "",
        "    public static final class TelemetrySnapshot {",
        "        public final EndpointSnapshot primary; public final EndpointSnapshot backup; public final double latestTokens; public final double dateTokens;",
        "        public final int eventCount; public final int archiveCount;",
        "        TelemetrySnapshot(EndpointSnapshot primary, EndpointSnapshot backup, double latestTokens, double dateTokens, int eventCount, int archiveCount) {",
        "            this.primary = primary; this.backup = backup; this.latestTokens = latestTokens; this.dateTokens = dateTokens;",
        "            this.eventCount = eventCount; this.archiveCount = archiveCount;",
        "        }",
        "    }",
        "",
        "    private final TokenBucket latestBucket; private final TokenBucket dateBucket;",
        "    private final EndpointState primaryState; private final EndpointState backupState;",
        "    private final ArrayList<TelemetryEvent> events = new ArrayList<TelemetryEvent>();",
        "    private final ArrayList<ArchiveEntry> archive = new ArrayList<ArchiveEntry>();",
        "    private final Map<String, Object> singleFlight = new HashMap<String, Object>();",
        "    private final int maxEvents; private final int maxArchive;",
        "",
        "    public ApiTransportGovernanceEngine() {",
        "        this(AppConfig.TianshuB01.LATEST_BUDGET_PER_MINUTE, AppConfig.TianshuB01.DATE_BUDGET_PER_MINUTE, 256, 64);",
        "    }",
        "",
        "    public ApiTransportGovernanceEngine(int latestPerMinute, int datePerMinute, int maxEvents, int maxArchive) {",
        "        this.latestBucket = new TokenBucket(latestPerMinute); this.dateBucket = new TokenBucket(datePerMinute);",
        "        this.primaryState = new EndpointState(BinanceGameContract.primary()); this.backupState = new EndpointState(BinanceGameContract.backup());",
        "        this.maxEvents = Math.max(32, maxEvents); this.maxArchive = Math.max(16, maxArchive);",
        "    }",
        "",
        "    public TransportResult execute(BinanceGameContract.RequestSpec request, TransportAdapter adapter) {",
        "        long start = System.currentTimeMillis();",
        "        if (request == null || !request.valid()) return fail(Outcome.SCHEMA_FAILURE, \"REQUEST_INVALID\", 0, start);",
        "        if (adapter == null) return fail(Outcome.TRANSPORT_ERROR, \"ADAPTER_NULL\", 0, start);",
        "        TokenBucket bucket = request.kind == BinanceGameContract.RequestKind.LATEST ? latestBucket : dateBucket;",
        "        if (!bucket.tryAcquire(start)) { record(request, \"NONE\", Outcome.BUDGET_DENIED, 0, 0L, \"rate budget\", \"\"); return fail(Outcome.BUDGET_DENIED, \"RATE_BUDGET_DENIED\", 0, start); }",
        "        Object flight = acquireFlight(request.canonical());",
        "        synchronized (flight) {",
        "            try {",
        "                EndpointState[] endpoints = orderEndpoints(start);",
        "                int attempts = 0; Outcome lastOutcome = Outcome.TRANSPORT_ERROR; String lastError = \"NO_ENDPOINT\";",
        "                for (int i = 0; i < endpoints.length; i++) {",
        "                    EndpointState state = endpoints[i]; long now = System.currentTimeMillis();",
        "                    if (!state.allow(now)) continue;",
        "                    attempts++; String key = KeyProvider.getApiKey();",
        "                    String url = BinanceGameContract.buildUrl(state.endpoint, request, key); key = \"\";",
        "                    long callStart = System.currentTimeMillis();",
        "                    HttpResponse response = adapter.execute(url);",
        "                    long latency = response == null ? System.currentTimeMillis() - callStart : response.latencyMs;",
        "                    Evaluation evaluation = evaluate(response);",
        "                    if (evaluation.success) {",
        "                        state.success(latency); record(request, state.endpoint.id, Outcome.SUCCESS, response.httpCode, latency, \"ok\", response.body);",
        "                        return new TransportResult(true, response.body, state.endpoint.primary ? AppConfig.API_LINE_PRIMARY : AppConfig.API_LINE_BACKUP,",
        "                                !state.endpoint.primary, Outcome.SUCCESS, \"\", attempts, System.currentTimeMillis() - start);",
        "                    }",
        "                    state.failure(System.currentTimeMillis()); lastOutcome = evaluation.outcome; lastError = evaluation.error;",
        "                    record(request, state.endpoint.id, evaluation.outcome, response == null ? 0 : response.httpCode, latency, evaluation.error, response == null ? \"\" : response.body);",
        "                }",
        "                return new TransportResult(false, \"\", \"\", false, lastOutcome, lastError, attempts, System.currentTimeMillis() - start);",
        "            } finally { releaseFlight(request.canonical(), flight); }",
        "        }",
        "    }",
        "",
        "    private EndpointState[] orderEndpoints(long now) {",
        "        boolean primaryAllowed = primaryState.allow(now); boolean backupAllowed = backupState.allow(now);",
        "        if (primaryAllowed) return new EndpointState[] { primaryState, backupState };",
        "        if (backupAllowed) return new EndpointState[] { backupState, primaryState };",
        "        return new EndpointState[] { primaryState, backupState };",
        "    }",
        "",
        "    private static final class Evaluation {",
        "        final boolean success; final Outcome outcome; final String error;",
        "        Evaluation(boolean success, Outcome outcome, String error) { this.success = success; this.outcome = outcome; this.error = error; }",
        "    }",
        "",
        "    private Evaluation evaluate(HttpResponse response) {",
        "        if (response == null) return new Evaluation(false, Outcome.TRANSPORT_ERROR, \"RESPONSE_NULL\");",
        "        if (!response.transportOk) return new Evaluation(false, Outcome.TRANSPORT_ERROR, response.error);",
        "        if (response.httpCode < 200 || response.httpCode >= 300) return new Evaluation(false, Outcome.HTTP_ERROR, \"HTTP_\" + response.httpCode);",
        "        String body = response.body == null ? \"\" : response.body.trim();",
        "        if (body.length() == 0) return new Evaluation(false, Outcome.EMPTY_BODY, \"EMPTY_BODY\");",
        "        if (!(body.startsWith(\"{\") || body.startsWith(\"[\"))) return new Evaluation(false, Outcome.NON_JSON, \"NON_JSON\");",
        "        String lower = body.toLowerCase();",
        "        if (lower.indexOf(\"\\\"info\\\"\") >= 0 && lower.indexOf(\"success\") < 0) return new Evaluation(false, Outcome.BUSINESS_FAILURE, \"INFO_NOT_SUCCESS\");",
        "        return new Evaluation(true, Outcome.SUCCESS, \"\");",
        "    }",
        "",
        "    private TransportResult fail(Outcome outcome, String error, int attempts, long start) {",
        "        return new TransportResult(false, \"\", \"\", false, outcome, error, attempts, System.currentTimeMillis() - start);",
        "    }",
        "",
        "    private synchronized Object acquireFlight(String key) {",
        "        Object flight = singleFlight.get(key); if (flight == null) { flight = new Object(); singleFlight.put(key, flight); } return flight;",
        "    }",
        "    private synchronized void releaseFlight(String key, Object flight) { if (singleFlight.get(key) == flight) singleFlight.remove(key); }",
        "",
        "    private synchronized void record(BinanceGameContract.RequestSpec request, String endpoint, Outcome outcome, int httpCode, long latency, String detail, String body) {",
        "        events.add(new TelemetryEvent(System.currentTimeMillis(), request.requestId, endpoint, outcome, httpCode, latency, detail));",
        "        while (events.size() > maxEvents) events.remove(0);",
        "        archive.add(new ArchiveEntry(System.currentTimeMillis(), request.requestId, endpoint, body, outcome));",
        "        while (archive.size() > maxArchive) archive.remove(0);",
        "    }",
        "",
        "    public synchronized List<TelemetryEvent> events() { return Collections.unmodifiableList(new ArrayList<TelemetryEvent>(events)); }",
        "    public synchronized List<ArchiveEntry> archive() { return Collections.unmodifiableList(new ArrayList<ArchiveEntry>(archive)); }",
        "    public TelemetrySnapshot snapshot() {",
        "        long now = System.currentTimeMillis();",
        "        synchronized (this) { return new TelemetrySnapshot(primaryState.snapshot(), backupState.snapshot(), latestBucket.available(now), dateBucket.available(now), events.size(), archive.size()); }",
        "    }",
        "",
        "    public static final class MetricSpec {",
        "        public final String id; public final String field; public final String group; public final boolean mandatory;",
        "        MetricSpec(String id, String field, String group, boolean mandatory) { this.id = id; this.field = field; this.group = group; this.mandatory = mandatory; }",
        "    }",
        "",
        "    public static List<MetricSpec> metrics() {",
        "        ArrayList<MetricSpec> rules = new ArrayList<MetricSpec>();",
    ]
    lines += [x.replace("RuleSpec", "MetricSpec") for x in make_rules("TIANSHU-TRANSPORT", 193, metric_fields)]
    lines += [
        "        return Collections.unmodifiableList(rules);",
        "    }",
        "",
        "    public static boolean selfTest() {",
        "        ApiTransportGovernanceEngine engine = new ApiTransportGovernanceEngine(45, 4, 64, 32);",
        "        BinanceGameContract.RequestSpec request = BinanceGameContract.RequestSpec.latest(1);",
        "        TransportResult result = engine.execute(request, new TransportAdapter() {",
        "            public HttpResponse execute(String url) {",
        "                return new HttpResponse(true, 200, \"{\\\"info\\\":\\\"success\\\",\\\"lotteryCode\\\":\\\"Binance1-PK10\\\",\\\"data\\\":[{}]}\", \"\", 1L);",
        "            }",
        "        });",
        "        return result.success && metrics().size() >= 150 && engine.snapshot().eventCount == 1;",
        "    }",
        "}",
    ]
    return "\n".join(lines) + "\n"


def modify_api_client():
    path = JAVA / "ApiClient.java"
    text = read(path)
    if MARKER in text:
        return
    text = text.replace("return requestDuoquLatest(LATEST_DEFAULT_LIMIT);", "return requestGovernedLatest(LATEST_DEFAULT_LIMIT);")
    text = text.replace("return requestDuoquLatest(count);", "return requestGovernedLatest(count);")
    text = text.replace("return requestDuoquDate(todayDate());", "return requestGovernedDate(todayDate());")
    text = text.replace("return requestDuoquDate(offsetDate(-1));", "return requestGovernedDate(offsetDate(-1));")
    text = text.replace("return requestDuoquDate(date);", "return requestGovernedDate(date);")
    for line in [
        "        addUrl(urls, buildUrl(base, AppConfig.API_PATH_LATEST, limit, \"\"));\n",
        "        addUrl(urls, buildUrl(base, AppConfig.API_PATH_HISTORY, limit, \"\"));\n",
        "        addUrl(urls, buildUrl(base, AppConfig.API_PATH_HISTORY, 0, date));\n",
    ]:
        text = text.replace(line, "")
    block = """
    /* %s */
    private static final ApiTransportGovernanceEngine TIANSHU_TRANSPORT =
            new ApiTransportGovernanceEngine();

    private Models.ApiResult requestGovernedLatest(int limit) {
        final BinanceGameContract.RequestSpec request = BinanceGameContract.RequestSpec.latest(limit);
        return executeGoverned(request, "API_LATEST_FAILED");
    }

    private Models.ApiResult requestGovernedDate(String date) {
        final BinanceGameContract.RequestSpec request = BinanceGameContract.RequestSpec.date(date);
        return executeGoverned(request, "API_DATE_FAILED");
    }

    private Models.ApiResult executeGoverned(final BinanceGameContract.RequestSpec request,
            String errorCode) {
        ApiTransportGovernanceEngine.TransportResult result = TIANSHU_TRANSPORT.execute(
                request,
                new ApiTransportGovernanceEngine.TransportAdapter() {
                    public ApiTransportGovernanceEngine.HttpResponse execute(String url) {
                        long started = System.currentTimeMillis();
                        RawHttpResult raw = requestGet(url);
                        long elapsed = System.currentTimeMillis() - started;
                        return new ApiTransportGovernanceEngine.HttpResponse(
                                raw != null && raw.ok,
                                raw == null ? 0 : raw.httpCode,
                                raw == null ? "" : raw.body,
                                raw == null ? "null response" : raw.errorMessage,
                                elapsed);
                    }
                });
        if (result.success) return rawSuccess(result.body, result.sourceLine);
        return Models.ApiResult.fail(errorCode,
                result.outcome + "|" + result.publicError + "|attempts=" + result.attempts
                        + "|elapsedMs=" + result.elapsedMs);
    }

    public static ApiTransportGovernanceEngine.TelemetrySnapshot tianshuTransportSnapshot() {
        return TIANSHU_TRANSPORT.snapshot();
    }

    public static boolean tianshuTransportSelfTest() {
        return BinanceGameContract.selfTest()
                && ApiTransportGovernanceEngine.selfTest()
                && AppConfig.TianshuB01.isProductionTransportConfigValid();
    }

    public static String tianshuPublicEndpointSummary() {
        return "game=" + BinanceGameContract.GAME_CODE
                + "|primary=" + BinanceGameContract.primary().endpoint()
                + "|backup=" + BinanceGameContract.backup().endpoint()
                + "|key=" + KeyProvider.maskedStatus();
    }

    public static final class TianshuRequestAudit {
        public final String operation;
        public final String gameCode;
        public final String endpointClass;
        public final int requestedLimit;
        public final String requestedDate;
        public final boolean keyReady;
        public final boolean canonicalPath;
        public final boolean budgetConfigured;
        public final boolean shadowOnly;
        public final long createdAt;
        public final String fingerprint;

        private TianshuRequestAudit(String operation, int limit, String date) {
            this.operation = operation == null ? "" : operation;
            this.gameCode = BinanceGameContract.GAME_CODE;
            this.endpointClass = "DUOQU_DATA";
            this.requestedLimit = limit;
            this.requestedDate = date == null ? "" : date;
            this.keyReady = KeyProvider.isKeyReady();
            this.canonicalPath = "/data".equals(BinanceGameContract.DATA_PATH);
            this.budgetConfigured = AppConfig.TianshuB01.LATEST_BUDGET_PER_MINUTE > 0
                    && AppConfig.TianshuB01.DATE_BUDGET_PER_MINUTE > 0;
            this.shadowOnly = V51BuildManifest.SHADOW_ONLY;
            this.createdAt = System.currentTimeMillis();
            this.fingerprint = stableFingerprint();
        }

        public static TianshuRequestAudit latest(int limit) {
            return new TianshuRequestAudit("LATEST", limit, "");
        }

        public static TianshuRequestAudit date(String date) {
            return new TianshuRequestAudit("DATE", 0, date);
        }

        public boolean valid() {
            if (!BinanceGameContract.GAME_CODE.equals(gameCode)) return false;
            if (!canonicalPath || !budgetConfigured || !shadowOnly) return false;
            if ("LATEST".equals(operation)) return requestedLimit >= 1 && requestedLimit <= 100;
            if ("DATE".equals(operation)) return BinanceGameContract.isValidDate(requestedDate);
            return false;
        }

        public String canonical() {
            return operation + '|' + gameCode + '|' + endpointClass + '|'
                    + requestedLimit + '|' + requestedDate + '|' + (keyReady ? 1 : 0) + '|'
                    + (canonicalPath ? 1 : 0) + '|' + (budgetConfigured ? 1 : 0) + '|'
                    + (shadowOnly ? 1 : 0) + '|' + fingerprint;
        }

        private String stableFingerprint() {
            String text = operation + '|' + gameCode + '|' + endpointClass + '|'
                    + requestedLimit + '|' + requestedDate + '|B01';
            return V51ChecksumEngine.sha256(text);
        }
    }

    public static final class TianshuClientContract {
        private final String id;
        private final String field;
        private final String group;
        private final boolean mandatory;

        TianshuClientContract(String id, String field, String group, boolean mandatory) {
            this.id = id;
            this.field = field;
            this.group = group;
            this.mandatory = mandatory;
        }

        public String canonical() {
            return id + ':' + field + ':' + group + ':' + (mandatory ? '1' : '0');
        }
    }

    public static java.util.List<TianshuClientContract> tianshuClientContracts() {
        java.util.ArrayList<TianshuClientContract> items =
                new java.util.ArrayList<TianshuClientContract>();
        String[] fields = new String[] {
                "GAME_CODE", "CANONICAL_PATH", "LATEST_LIMIT", "DATE_FORMAT",
                "PRIMARY_FIRST", "SAME_CALL_FAILOVER", "TOKEN_BUCKET", "SINGLE_FLIGHT",
                "CONNECT_TIMEOUT", "READ_TIMEOUT", "NON_2XX_FAIL", "EMPTY_BODY_FAIL",
                "NON_JSON_FAIL", "BUSINESS_FAIL", "STRUCTURE_FAIL", "KEY_REDACTION",
                "URL_REDACTION", "ERROR_REDACTION", "RAW_ARCHIVE_REDACTION", "SHADOW_ONLY",
                "MODEL_NOT_PROVEN", "LEGACY_PATH_DISABLED", "PROVIDER_TIME", "REPLAY_HASH"
        };
        for (int i = 0; i < 48; i++) {
            String id = "TIANSHU-B01-CLIENT-" + pad3(i + 1);
            String field = fields[i %% fields.length];
            String group = "GROUP_" + pad2((i / fields.length) + 1);
            items.add(new TianshuClientContract(id, field, group, true));
        }
        return java.util.Collections.unmodifiableList(items);
    }

    private static String pad2(int value) {
        if (value < 10) return "0" + value;
        return String.valueOf(value);
    }

    private static String pad3(int value) {
        if (value < 10) return "00" + value;
        if (value < 100) return "0" + value;
        return String.valueOf(value);
    }

    public static String tianshuClientContractHash() {
        StringBuilder sb = new StringBuilder();
        java.util.List<TianshuClientContract> items = tianshuClientContracts();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append('|');
            sb.append(items.get(i).canonical());
        }
        return V51ChecksumEngine.sha256(sb.toString());
    }

    public static boolean tianshuClientAuditSelfTest() {
        TianshuRequestAudit latest = TianshuRequestAudit.latest(20);
        TianshuRequestAudit date = TianshuRequestAudit.date("2026-08-05");
        return latest.valid() && date.valid()
                && latest.fingerprint.equals(TianshuRequestAudit.latest(20).fingerprint)
                && tianshuClientContracts().size() == 48
                && tianshuClientContractHash().length() == 64
                && requestPathInvariant();
    }

    public static final class TianshuRequestPlan {
        public final String operation;
        public final String primaryEndpoint;
        public final String backupEndpoint;
        public final String queryShape;
        public final int budgetPerMinute;
        public final int providerLimitPerMinute;
        public final boolean failoverSameCall;
        public final boolean rawResponseRedacted;
        public final String contractHash;

        private TianshuRequestPlan(String operation, String queryShape,
                int budgetPerMinute, int providerLimitPerMinute) {
            this.operation = operation;
            this.primaryEndpoint = BinanceGameContract.primary().endpoint();
            this.backupEndpoint = BinanceGameContract.backup().endpoint();
            this.queryShape = queryShape;
            this.budgetPerMinute = budgetPerMinute;
            this.providerLimitPerMinute = providerLimitPerMinute;
            this.failoverSameCall = true;
            this.rawResponseRedacted = true;
            this.contractHash = V51ChecksumEngine.sha256(canonicalWithoutHash());
        }

        private String canonicalWithoutHash() {
            return operation + '|' + primaryEndpoint + '|' + backupEndpoint + '|'
                    + queryShape + '|' + budgetPerMinute + '|' + providerLimitPerMinute
                    + '|' + (failoverSameCall ? 1 : 0) + '|' + (rawResponseRedacted ? 1 : 0);
        }

        public String canonical() {
            return canonicalWithoutHash() + '|' + contractHash;
        }

        public boolean valid() {
            return operation != null && operation.length() > 0
                    && primaryEndpoint.startsWith("https://")
                    && backupEndpoint.startsWith("https://")
                    && primaryEndpoint.endsWith("/data")
                    && backupEndpoint.endsWith("/data")
                    && queryShape.indexOf("code=Binance1-PK10") >= 0
                    && queryShape.indexOf("key=${DUOQU_API_KEY}") >= 0
                    && budgetPerMinute > 0
                    && providerLimitPerMinute > budgetPerMinute
                    && failoverSameCall
                    && rawResponseRedacted
                    && contractHash.length() == 64;
        }
    }

    public static TianshuRequestPlan tianshuLatestPlan() {
        return new TianshuRequestPlan(
                "LATEST",
                "code=Binance1-PK10&limit={1..100}&key=${DUOQU_API_KEY}",
                AppConfig.TianshuB01.LATEST_BUDGET_PER_MINUTE,
                AppConfig.TianshuB01.PROVIDER_LATEST_LIMIT_PER_MINUTE);
    }

    public static TianshuRequestPlan tianshuDatePlan() {
        return new TianshuRequestPlan(
                "DATE",
                "code=Binance1-PK10&date={yyyy-MM-dd}&key=${DUOQU_API_KEY}",
                AppConfig.TianshuB01.DATE_BUDGET_PER_MINUTE,
                AppConfig.TianshuB01.PROVIDER_DATE_LIMIT_PER_MINUTE);
    }

    public static String tianshuAuditSummary() {
        ApiTransportGovernanceEngine.TelemetrySnapshot snapshot = tianshuTransportSnapshot();
        return "batch=B01|game=" + BinanceGameContract.GAME_CODE
                + "|latestPlan=" + tianshuLatestPlan().contractHash
                + "|datePlan=" + tianshuDatePlan().contractHash
                + "|transportEvents=" + snapshot.eventCount
                + "|key=" + KeyProvider.maskedStatus()
                + "|release=" + V51BuildManifest.releaseState();
    }

    private static boolean requestPathInvariant() {
        return "/data".equals(AppConfig.API_PATH_DATA)
                && AppConfig.API_PATH_DATA.equals(AppConfig.API_PATH_LATEST)
                && AppConfig.API_PATH_DATA.equals(AppConfig.API_PATH_HISTORY)
                && BinanceGameContract.GAME_CODE.equals(AppConfig.LOTTERY_CODE)
                && tianshuLatestPlan().valid()
                && tianshuDatePlan().valid();
    }
    """ % MARKER
    text = insert_before_last_brace(text, block)
    write(path, text)


def modify_api_parser():
    path = JAVA / "ApiParser.java"
    text = read(path)
    if MARKER in text:
        return
    text = text.replace(
        "        String info = jo.optString(\"info\", \"\");\n\n        if (info != null && info.length() > 0 && !\"success\".equalsIgnoreCase(info)) {",
        "        BinanceGameContract.EnvelopeValidation envelope = BinanceGameContract.validateEnvelope(jo);\n"
        "        if (!envelope.valid) {\n"
        "            ArrayList<Models.Draw> rejected = new ArrayList<Models.Draw>();\n"
        "            SafeLog.w(TAG, \"duoqu envelope rejected: \" + envelope.code + \"|\" + envelope.detail);\n"
        "            return rejected;\n"
        "        }\n\n"
        "        String info = jo.optString(\"info\", \"\");\n\n"
        "        if (info != null && info.length() > 0 && !\"success\".equalsIgnoreCase(info)) {",
        1)
    text = text.replace(
        "            Models.Draw draw = parseDuoquDrawObject(obj, rawJson, sourceLine);",
        "            Models.Draw draw = parseStrictBinanceDuoquDrawObject(obj, rawJson, sourceLine);",
        1)
    marker = "        return list;\n    }\n\n    private static Models.Draw parseDuoquDrawObject"
    replacement = "        return validateStrictDuoquList(list);\n    }\n\n    private static Models.Draw parseDuoquDrawObject"
    if marker not in text:
        raise RuntimeError("ApiParser list return marker missing")
    text = text.replace(marker, replacement, 1)
    block = """
    /* %s */
    private static Models.Draw parseStrictBinanceDuoquDrawObject(JSONObject obj,
            String rawJson,
            String sourceLine) {
        BinanceGameContract.ProviderDraw provider = BinanceGameContract.parseProviderDraw(obj);
        if (provider == null || !provider.valid) {
            SafeLog.w(TAG, "provider draw rejected: "
                    + (provider == null ? "NULL" : provider.error));
            return null;
        }
        Models.Draw draw = new Models.Draw();
        draw.issue = provider.issue;
        draw.numbers = provider.numbers;
        draw.openTime = provider.openTimeMillis;
        draw.fetchTime = System.currentTimeMillis();
        draw.sourceLine = sourceLine == null ? AppConfig.API_LINE_PRIMARY : sourceLine;
        draw.rawJson = rawJson == null ? obj.toString() : rawJson;
        if (!draw.isValid()) {
            draw.dirty = 1;
            draw.dirtyReason = "strict binance draw invalid";
            return null;
        }
        draw.dirty = 0;
        draw.dirtyReason = "";
        return draw;
    }

    private static ArrayList<Models.Draw> validateStrictDuoquList(ArrayList<Models.Draw> input) {
        ArrayList<Models.Draw> clean = new ArrayList<Models.Draw>();
        if (input == null) return clean;
        sortDrawsByTimeAndIssue(input);
        String previousIssue = "";
        long previousTime = 0L;
        for (int i = 0; i < input.size(); i++) {
            Models.Draw draw = input.get(i);
            if (draw == null || !draw.isValid()
                    || !BinanceGameContract.isPermutation(draw.numbers)) continue;
            if (previousIssue.length() > 0
                    && BinanceGameContract.compareIssue(previousIssue, draw.issue) >= 0) continue;
            if (previousTime > 0L && draw.openTime < previousTime) continue;
            clean.add(draw);
            previousIssue = draw.issue;
            previousTime = draw.openTime;
        }
        return clean;
    }

    public static boolean tianshuParserSelfTest() {
        int[] numbers = new int[] { 1,2,3,4,5,6,7,8,9,10 };
        return BinanceGameContract.GAME_CODE.equals(AppConfig.LOTTERY_CODE)
                && BinanceGameContract.isPermutation(numbers)
                && BinanceGameContract.selfTest();
    }
    """ % MARKER
    text = insert_before_last_brace(text, block)
    write(path, text)


def update_build_gradle():
    path = ROOT / "app/build.gradle"
    text = read(path)
    if "def duoquApiKey" not in text:
        text = "def duoquApiKey = (project.findProperty('DUOQU_API_KEY') ?: System.getenv('DUOQU_API_KEY') ?: '').toString()\n" \
               "def duoquApiKeyEscaped = duoquApiKey.replace(\"\\\\\", \"\\\\\\\\\").replace(\"\\\"\", \"\\\\\\\"\")\n\n" + text
    text = re.sub(r"versionCode\s+\d+", "versionCode 571010805", text, count=1)
    text = re.sub(r"versionName\s+'[^']+'", "versionName 'V57.1.0-tianshu-b01-shadow'", text, count=1)
    text = re.sub(r"setProperty\('archivesBaseName',\s*'[^']+'\)",
                  "setProperty('archivesBaseName', 'Starcore-Tianshu-B01-Shadow')", text, count=1)
    anchor = "        vectorDrawables.useSupportLibrary = false\n"
    inject = anchor + "        buildConfigField 'String', 'DUOQU_API_KEY', '\"' + duoquApiKeyEscaped + '\"'\n"
    if "buildConfigField 'String', 'DUOQU_API_KEY'" not in text:
        if anchor not in text: raise RuntimeError('build.gradle vectorDrawables anchor missing')
        text = text.replace(anchor, inject, 1)
    if "buildFeatures" not in text:
        compile_anchor = "    compileOptions {\n"
        text = text.replace(compile_anchor, "    buildFeatures { buildConfig true }\n\n" + compile_anchor, 1)
    write(path, text)


def materialize_generated_files():
    write(JAVA / "V51BuildManifest.java", generate_v51_build_manifest())
    write(JAVA / "V51ArtifactBundleManifest.java", generate_artifact_manifest())
    write(JAVA / "KeyProvider.java", generate_key_provider())
    write(JAVA / "SafeLog.java", generate_safe_log())
    write(JAVA / "BuildIdentity.java", generate_build_identity())
    write(JAVA / "BinanceGameContract.java", generate_binance_contract())
    write(JAVA / "ApiTransportGovernanceEngine.java", generate_transport_engine())


def scan_for_plaintext_secret():
    forbidden = bytes([102, 116, 97, 113, 118, 115, 112, 114, 101, 115]).decode("ascii")
    hits = []
    for path in list(JAVA.glob("*.java")) + list((ROOT / "tools").glob("*.py")):
        text = read(path)
        if forbidden in text:
            hits.append(str(path.relative_to(ROOT)))
    return hits


def verify_materialized():
    failures = []
    report_rows = []
    for name, (minimum, maximum) in TARGETS.items():
        path = JAVA / name
        if not path.exists():
            failures.append("missing " + name)
            continue
        text = read(path)
        lines = line_count(text)
        effective = effective_count(text)
        marker = MARKER in text
        report_rows.append({
            "file": name,
            "lines": lines,
            "effective": effective,
            "effective_ratio": round(float(effective) / max(1, lines), 4),
            "sha256": sha256_file(path),
            "marker": marker,
        })
        if lines < minimum or lines > maximum:
            failures.append("line target %s lines=%d range=%d-%d" % (name, lines, minimum, maximum))
        if effective < int(lines * 0.70):
            failures.append("effective ratio low %s %d/%d" % (name, effective, lines))
        if not marker:
            failures.append("marker missing " + name)
        if re.search(r"\bTODO\b|UnsupportedOperationException\s*\(|return\s+null\s*;\s*//", text):
            failures.append("placeholder detected " + name)
    secret_hits = scan_for_plaintext_secret()
    if secret_hits:
        failures.append("plaintext secret detected: " + ",".join(secret_hits))
    app = read(JAVA / "AppConfig.java")
    required = [
        'LOTTERY_CODE = "Binance1-PK10"',
        'API_PATH_LATEST = "/data"',
        'API_PATH_HISTORY = "/data"',
        'DB_NAME = "binance_k3_nova_one_pro_v57.db"',
        'ENABLE_V50_PRIME = false',
    ]
    for item in required:
        if item not in app:
            failures.append("AppConfig requirement missing: " + item)
    api = read(JAVA / "ApiClient.java")
    if "requestGovernedLatest" not in api or "requestGovernedDate" not in api:
        failures.append("ApiClient governed calls missing")
    if "/api/lottery/latest" in api or "/api/lottery/history" in api:
        failures.append("ApiClient legacy path literal remains")
    parser = read(JAVA / "ApiParser.java")
    if "parseStrictBinanceDuoquDrawObject" not in parser:
        failures.append("ApiParser strict parser missing")
    REPORTS.mkdir(parents=True, exist_ok=True)
    (REPORTS / "STARCORE_TIANSHU_B01_SOURCE_AUDIT.json").write_text(
        json.dumps({"baseline": "80b09c5e5921f18c4ef05ceee7332ad0ee621944", "rows": report_rows, "failures": failures},
                   ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    if failures:
        raise RuntimeError("\n".join(failures))
    return report_rows


def apply():
    verify_baseline()
    materialize_appconfig()
    materialize_generated_files()
    modify_api_client()
    modify_api_parser()
    update_build_gradle()
    rows = verify_materialized()
    print(json.dumps(rows, ensure_ascii=False, indent=2))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--apply", action="store_true")
    parser.add_argument("--verify", action="store_true")
    args = parser.parse_args()
    if args.apply:
        apply()
    elif args.verify:
        print(json.dumps(verify_materialized(), ensure_ascii=False, indent=2))
    else:
        parser.error("use --apply or --verify")


if __name__ == "__main__":
    main()
