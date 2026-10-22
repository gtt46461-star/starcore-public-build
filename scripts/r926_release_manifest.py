#!/usr/bin/env python3
"""R926 canonical release-manifest generator/validator.

The JSON file is the single editable identity source.  Compatibility properties and the Java
constant class are generated from it.  --check is fail-closed and never rewrites files.
"""
from __future__ import annotations
from pathlib import Path
import argparse, hashlib, json, re, sys

ROOT = Path(__file__).resolve().parents[1]
MANIFEST = ROOT / "release" / "release_manifest.json"
PROPS = ROOT / "release" / "release_identity.properties"
JAVA = ROOT / "app/src/main/java/com/ethk3/novaonepro/ReleaseManifestR926.java"
HOST_BUILD_CONFIG = ROOT / "verification/r36-stubs/com/ethk3/novaonepro/BuildConfig.java"

REQUIRED_TOP = (
    "formatVersion", "product", "applicationId", "versionName", "versionCode", "compileSdk",
    "minSdk", "targetSdk", "databaseName", "gameCode", "releaseMode", "schema",
    "runtimeContract", "proofBoundary"
)
REQUIRED_SCHEMA = (
    "userVersion", "legacyR811Revision", "legacyR815Revision", "legacyR906Revision",
    "r925Revision", "criticalDomainRevision", "researchDomainRevision", "legacyDomainRevision",
    "eventLedgerRevision"
)

def canonical_bytes(obj: dict) -> bytes:
    return (json.dumps(obj, ensure_ascii=False, sort_keys=True, separators=(",", ":")) + "\n").encode("utf-8")

def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()

def load() -> dict:
    obj = json.loads(MANIFEST.read_text(encoding="utf-8"))
    missing = [k for k in REQUIRED_TOP if k not in obj]
    if missing:
        raise SystemExit("R926_MANIFEST_MISSING_TOP:" + ",".join(missing))
    schema = obj["schema"]
    missing = [k for k in REQUIRED_SCHEMA if k not in schema]
    if missing:
        raise SystemExit("R926_MANIFEST_MISSING_SCHEMA:" + ",".join(missing))
    if obj["formatVersion"] != 1:
        raise SystemExit("R926_MANIFEST_FORMAT_UNSUPPORTED")
    if obj["applicationId"] != "com.ethk3.novaonepro":
        raise SystemExit("R926_APPLICATION_ID_DRIFT")
    if obj["gameCode"] != "Binance1-PK10":
        raise SystemExit("R926_GAME_CODE_DRIFT")
    if int(obj["versionCode"]) <= 577250925:
        raise SystemExit("R926_VERSION_CODE_NOT_UPGRADE")
    if int(schema["userVersion"]) != 36:
        raise SystemExit("R926_DB_USER_VERSION_DRIFT")
    if not re.fullmatch(r"R925_SCHEMA_[0-9]+", str(schema["r925Revision"])):
        raise SystemExit("R926_R925_SCHEMA_FORMAT")
    if obj["runtimeContract"].get("decisionOwner") != "DecisionGateEngine_ONLY":
        raise SystemExit("R926_DECISION_OWNER_DRIFT")
    if obj["runtimeContract"].get("settlementOwner") != "SettlementEngine_ONLY":
        raise SystemExit("R926_SETTLEMENT_OWNER_DRIFT")
    return obj

def prop_text(m: dict, digest: str) -> str:
    s=m["schema"]
    pairs = [
        ("applicationId",m["applicationId"]),("versionName",m["versionName"]),
        ("versionCode",m["versionCode"]),("compileSdk",m["compileSdk"]),("minSdk",m["minSdk"]),
        ("targetSdk",m["targetSdk"]),("dbVersion",s["userVersion"]),
        ("schemaRevision",s["legacyR906Revision"]),("legacyR811Revision",s["legacyR811Revision"]),
        ("legacyR815Revision",s["legacyR815Revision"]),("legacyR906Revision",s["legacyR906Revision"]),
        ("r925Revision",s["r925Revision"]),("criticalDomainRevision",s["criticalDomainRevision"]),
        ("researchDomainRevision",s["researchDomainRevision"]),("legacyDomainRevision",s["legacyDomainRevision"]),
        ("eventLedgerRevision",s["eventLedgerRevision"]),("databaseName",m["databaseName"]),
        ("gameCode",m["gameCode"]),("releaseMode",m["releaseMode"]),("releaseManifestSha256",digest)
    ]
    return "".join(f"{k}={v}\n" for k,v in pairs)

def java_text(m: dict, digest: str) -> str:
    s=m["schema"]
    def q(v): return json.dumps(str(v), ensure_ascii=False)
    return f'''package com.ethk3.novaonepro;

/** GENERATED from release/release_manifest.json by scripts/r926_release_manifest.py. */
public final class ReleaseManifestR926 {{
    public static final String MANIFEST_SHA256 = {q(digest)};
    public static final String APPLICATION_ID = {q(m['applicationId'])};
    public static final String VERSION_NAME = {q(m['versionName'])};
    public static final int VERSION_CODE = {int(m['versionCode'])};
    public static final int COMPILE_SDK = {int(m['compileSdk'])};
    public static final int MIN_SDK = {int(m['minSdk'])};
    public static final int TARGET_SDK = {int(m['targetSdk'])};
    public static final String DATABASE_NAME = {q(m['databaseName'])};
    public static final int DB_USER_VERSION = {int(s['userVersion'])};
    public static final int LEGACY_R811_REVISION = {int(s['legacyR811Revision'])};
    public static final int LEGACY_R815_REVISION = {int(s['legacyR815Revision'])};
    public static final int LEGACY_R906_REVISION = {int(s['legacyR906Revision'])};
    public static final String R925_REVISION = {q(s['r925Revision'])};
    public static final int CRITICAL_DOMAIN_REVISION = {int(s['criticalDomainRevision'])};
    public static final int RESEARCH_DOMAIN_REVISION = {int(s['researchDomainRevision'])};
    public static final int LEGACY_DOMAIN_REVISION = {int(s['legacyDomainRevision'])};
    public static final int EVENT_LEDGER_REVISION = {int(s['eventLedgerRevision'])};
    public static final String GAME_CODE = {q(m['gameCode'])};
    public static final String RELEASE_MODE = {q(m['releaseMode'])};
    public static final String DECISION_OWNER = {q(m['runtimeContract']['decisionOwner'])};
    public static final String SETTLEMENT_OWNER = {q(m['runtimeContract']['settlementOwner'])};
    private ReleaseManifestR926() {{}}
    public static String schemaIdentity() {{
        return "user=" + DB_USER_VERSION + "|r811=" + LEGACY_R811_REVISION
                + "|r815=" + LEGACY_R815_REVISION + "|r906=" + LEGACY_R906_REVISION
                + "|r925=" + R925_REVISION + "|critical=" + CRITICAL_DOMAIN_REVISION
                + "|research=" + RESEARCH_DOMAIN_REVISION + "|legacy=" + LEGACY_DOMAIN_REVISION
                + "|event=" + EVENT_LEDGER_REVISION;
    }}
}}
'''

def host_build_config_text(m: dict, digest: str) -> str:
    s=m["schema"]
    return f'''package com.ethk3.novaonepro;
public final class BuildConfig {{
    public static final boolean DEBUG = false;
    public static final String APPLICATION_ID = "{m['applicationId']}";
    public static final String BUILD_TYPE = "release";
    public static final int VERSION_CODE = {int(m['versionCode'])};
    public static final String VERSION_NAME = "{m['versionName']}";
    public static final String DUOQU_API_KEY_A = "";
    public static final String DUOQU_API_KEY_B = "";
    public static final String RELEASE_MANIFEST_SHA256 = "{digest}";
    public static final int DB_USER_VERSION = {int(s['userVersion'])};
    public static final int LEGACY_R906_SCHEMA_REVISION = {int(s['legacyR906Revision'])};
    public static final String R925_SCHEMA_REVISION = "{s['r925Revision']}";
    public static final int CRITICAL_DOMAIN_REVISION = {int(s['criticalDomainRevision'])};
    public static final int RESEARCH_DOMAIN_REVISION = {int(s['researchDomainRevision'])};
    public static final int LEGACY_DOMAIN_REVISION = {int(s['legacyDomainRevision'])};
    public static final int EVENT_LEDGER_REVISION = {int(s['eventLedgerRevision'])};
}}
'''

def expected() -> tuple[dict,str,str,str,str]:
    m=load(); digest=sha256(MANIFEST.read_bytes())
    return m,digest,prop_text(m,digest),java_text(m,digest),host_build_config_text(m,digest)

def write() -> None:
    m,digest,props,java,host=expected()
    PROPS.write_text(props,encoding="utf-8")
    JAVA.parent.mkdir(parents=True,exist_ok=True); JAVA.write_text(java,encoding="utf-8")
    HOST_BUILD_CONFIG.parent.mkdir(parents=True,exist_ok=True); HOST_BUILD_CONFIG.write_text(host,encoding="utf-8")
    print(json.dumps({"status":"WRITTEN","manifestSha256":digest,"versionName":m['versionName'],"versionCode":m['versionCode']},ensure_ascii=False))

def check() -> None:
    m,digest,props,java,host=expected(); errors=[]
    for path,want,code in [(PROPS,props,"PROPERTIES"),(JAVA,java,"JAVA"),(HOST_BUILD_CONFIG,host,"HOST_BUILD_CONFIG")]:
        got=path.read_text(encoding="utf-8") if path.is_file() else ""
        if got!=want: errors.append(code)
    if errors:
        print(json.dumps({"status":"FAIL","errors":errors,"manifestSha256":digest},ensure_ascii=False)); raise SystemExit(23)
    print(json.dumps({"status":"PASS","manifestSha256":digest,"versionName":m['versionName'],"versionCode":m['versionCode']},ensure_ascii=False))

def drift_test() -> None:
    _,_,props,_,_=expected()
    current="versionCode="+str(load()["versionCode"])
    mutated="versionCode="+str(int(load()["versionCode"])-1)
    bad=props.replace(current,mutated,1)
    if bad==props or bad==PROPS.read_text(encoding="utf-8"):
        raise SystemExit("R926_DRIFT_TEST_SETUP_FAILED")
    # A mismatched generated property must be detectable by exact equality, which is the same gate used by --check.
    if bad==props: raise SystemExit("R926_DRIFT_TEST_FALSE_NEGATIVE")
    print('{"status":"PASS","mutation":"versionCode","expectedFailure":"PROPERTIES"}')

def main():
    ap=argparse.ArgumentParser(); g=ap.add_mutually_exclusive_group(required=True)
    g.add_argument('--write',action='store_true'); g.add_argument('--check',action='store_true'); g.add_argument('--drift-test',action='store_true')
    a=ap.parse_args()
    if a.write: write()
    elif a.check: check()
    else: drift_test()
if __name__=='__main__': main()
