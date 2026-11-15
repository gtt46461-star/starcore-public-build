#!/usr/bin/env python3
"""Run R32.4 retention invariants on actual compiled production classes.

This creates Android platform adapters from the repository's existing host
adapter factory. No business class is stubbed, edited or compiled by this runner.
The supplied --classes must be a full compile of the candidate --repo.
"""
import argparse
from datetime import datetime, timezone
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import zipfile

BUSINESS = [
    "strategyv2/ColdCheckpointR324", "strategyv2/AtomNetSampleArchiveR324",
    "strategyv2/AtomNetPublicationR31", "strategyv2/AtomNetStoreR30",
    "strategyv2/CheckpointArtifactsR26", "strategyv2/CheckpointGcStoreR31",
    "strategyv2/CheckpointReferencesR26", "SchemaDomainCoordinatorR926",
    "ReportAttachmentClosureCP12", "CheckpointReportAuditR32", "StorageRestoreProofR31",
]


def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def main(args):
    repo = args.repo.resolve()
    classes = args.classes.resolve()
    sdk, jdbc, slf4j = (getattr(args, name).resolve() for name in ("android_jar", "jdbc_jar", "slf4j_jar"))
    out = args.out.resolve()
    if out.exists() and any(out.iterdir()):
        raise ValueError("--out must be a new or empty directory")
    out.mkdir(parents=True, exist_ok=True)
    receipt = {
        "status": "RUNNING", "scope": "PRODUCTION_JAVA_CODEC_GUARDS_AND_READERS_ON_LINUX_NATIVE_SQLITE_HOST",
        "startedAtUtc": datetime.now(timezone.utc).isoformat(), "productionOverrides": [],
        "androidDevice": "NOT_RUN", "processKillOnAndroid": "NOT_RUN", "soak": "NOT_RUN",
        "futureWinRate": "NOT_EVALUATED", "commands": [],
        "transactionBoundary": "Native SQLite rollback and exact production trigger DDL; host drives retirement statements, not Android Worker scheduling",
    }
    def save():
        (out / "RECEIPT.json").write_text(json.dumps(receipt, indent=2, ensure_ascii=False)+"\n")
    def run(argv, logfile):
        result = subprocess.run(list(map(str, argv)), cwd=repo, capture_output=True, text=True, timeout=240)
        (out / logfile).write_text(result.stdout+result.stderr)
        receipt["commands"].append({"argv": list(map(str,argv)), "log": logfile, "exitCode": result.returncode})
        save()
        if result.returncode:
            raise RuntimeError(logfile+": "+(result.stdout+result.stderr)[-4000:])
        return result.stdout
    try:
        for path in (sdk, jdbc, slf4j):
            if not path.is_file():
                raise FileNotFoundError(path)
        receipt["dependencies"] = {name:{"sha256":sha(path),"filename":path.name} for name,path in [("androidSdk",sdk),("sqliteJdbc",jdbc),("slf4jApi",slf4j)]}
        class_hashes = {}
        source_hashes = {}
        jar = zipfile.ZipFile(classes) if classes.is_file() else None
        try:
            for name in BUSINESS:
                rel="com/ethk3/novaonepro/"+name+".class"
                data=jar.read(rel) if jar else (classes/rel).read_bytes()
                class_hashes[rel]=hashlib.sha256(data).hexdigest()
                source=repo/"app/src/main/java"/("com/ethk3/novaonepro/"+name+".java")
                source_hashes[str(source.relative_to(repo))]=sha(source)
        finally:
            if jar:
                jar.close()
        receipt["actualBusinessClassSha256"] = class_hashes
        receipt["productionSourceSha256"] = source_hashes
        factory=repo/"tools/r31/run_atomnet_publication_host.py"
        spec=importlib.util.spec_from_file_location("r324_existing_android_adapters",factory)
        module=importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        definitions=module.platform_sources()
        if any(not name.startswith("android/") for name in definitions):
            raise ValueError("Only Android boundary adapters are permitted")
        platform_src=out/"platform_sources";platform=out/"platform";tests=out/"tests"
        platform.mkdir();tests.mkdir()
        paths=[]
        for name,text in definitions.items():
            path=platform_src/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(text);paths.append(path)
        receipt["platformSourceSha256"]={str(p.relative_to(out)):sha(p) for p in paths}
        compiler=[shutil.which("javac")] if shutil.which("javac") else ["java","-m","jdk.compiler/com.sun.tools.javac.Main"]
        run(compiler+["-encoding","UTF-8","-sourcepath","","-implicit:none","-cp",os.pathsep.join(map(str,(classes,sdk))),"-d",platform,*paths],"platform_compile.log")
        if (platform/"com").exists():
            raise AssertionError("Platform adapter compile generated a business class")
        test_paths=[Path(__file__).resolve().parent/name for name in ("ColdCheckpointR324HostTest.java","AtomNetArchiveR324NativeHostTest.java")]
        receipt["testSourceSha256"]={p.name:sha(p) for p in test_paths}
        cp=os.pathsep.join(map(str,(platform,classes,sdk,jdbc,slf4j)))
        run(compiler+["-encoding","UTF-8","-sourcepath","","-implicit:none","-cp",cp,"-d",tests,*test_paths],"test_compile.log")
        runtime=os.pathsep.join(map(str,(tests,platform,classes,sdk,jdbc,slf4j)))
        cold=run(["java","-Xmx512m","-cp",runtime,"com.ethk3.novaonepro.strategyv2.ColdCheckpointR324HostTest",out],"cold_host.log")
        native=run(["java","-Xmx512m","-cp",runtime,"com.ethk3.novaonepro.strategyv2.AtomNetArchiveR324NativeHostTest",out],"sample_native_host.log")
        receipt["coldFilesystemAssertions"]=int(re.search(r"COLD_HOST_ASSERTIONS=(\d+)",cold).group(1))
        receipt["nativeSqliteSampleAssertions"]=int(re.search(r"SAMPLE_NATIVE_ASSERTIONS=(\d+)",native).group(1))
        receipt["status"]="PASS_HOST_INVARIANTS_ONLY"
        receipt["completedAtUtc"]=datetime.now(timezone.utc).isoformat()
        save();print(json.dumps({k:receipt[k] for k in ("status","coldFilesystemAssertions","nativeSqliteSampleAssertions")},ensure_ascii=False))
        return 0
    except Exception as failure:
        receipt["status"]="FAILED";receipt["failure"]=type(failure).__name__+": "+str(failure);save();raise


if __name__ == "__main__":
    parser=argparse.ArgumentParser(description=__doc__)
    for name in ("repo","classes","android-jar","jdbc-jar","slf4j-jar","out"):
        parser.add_argument("--"+name,type=Path,required=True)
    sys.exit(main(parser.parse_args()))
