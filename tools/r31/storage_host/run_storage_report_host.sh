#!/usr/bin/env bash
# Source subset + original JNI on Linux. No Android VM, APK, handset, soak or natural-quality PASS.
set -euo pipefail
cd "$(dirname "$0")/../../.."
: "${R31_STORAGE_OUT:?choose a fresh output directory}"
: "${JAVA_HOME:?activate the repository build environment}"
storage_out=$(realpath -m "$R31_STORAGE_OUT")
if [ -e "$storage_out" ]; then echo "OUTPUT_ALREADY_EXISTS" >&2; exit 1; fi
# ripgrep is optional on CI runners; the same file selection falls back to find.
rg_files(){ if command -v rg >/dev/null 2>&1; then rg --files "$1" -g "$2"; else find "$1" -type f -name "$2" | sort; fi; }
trap 'for log in "$storage_out"/*.log; do echo "== $log"; tail -40 "$log"; done' ERR
mkdir -p "$storage_out/runtime" "$storage_out/product" "$storage_out/drivers" "$storage_out/fixtures"
product_classes=${CLASSES:-app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes}
storage_sdk=${ANDROID_HOME:-${ANDROID_SDK_ROOT:?}}
storage_junit=$(rg_files "$GRADLE_USER_HOME/caches/modules-2/files-2.1/junit/junit/4.13.2" '*.jar')
storage_hamcrest=$(rg_files "$GRADLE_USER_HOME/caches/modules-2/files-2.1/org.hamcrest/hamcrest-core/1.3" '*.jar')
storage_annotation=$(rg_files "$GRADLE_USER_HOME/caches/modules-2/files-2.1/androidx.annotation/annotation/1.3.0" '*.jar')
test -n "$storage_annotation" && test -f "$storage_annotation"
if [ -n "${REPORT_NATIVE_DIR:-}" ]; then
    storage_native=$REPORT_NATIVE_DIR
    python3 verification/batch01/build_host_report_native.py --verify-reuse "$storage_native"
    python3 - "$storage_native/receipt.json" <<'PY'
import sys,json,hashlib
from pathlib import Path
row=json.loads(Path(sys.argv[1]).read_text())
for item in row['source_files']:
    if hashlib.sha256(Path(item['path']).read_bytes()).hexdigest()!=item['sha256']:
        raise SystemExit('NATIVE_SOURCE_CHANGED_REBUILD_REQUIRED')
if hashlib.sha256(Path(row['library_path']).read_bytes()).hexdigest()!=row['library_sha256']:
    raise SystemExit('NATIVE_BINARY_CHANGED')
PY
    cp "$storage_native/receipt.json" "$storage_out/native-reused-receipt.json"
else
    storage_native="$storage_out/native"
    python3 verification/batch01/build_host_report_native.py --jdk "$JAVA_HOME" --output-dir "$storage_native" > "$storage_out/native-build.log" 2>&1
fi
python3 - "$storage_out" <<'PYSOURCE'
import json,hashlib,sys
from pathlib import Path
paths=[*Path('tools/r31/storage_host').rglob('*'),Path('app/src/main/cpp/report_native_cp4.c'),Path('app/src/test/java/com/ethk3/novaonepro/R31StorageGovernanceTest.java'),Path('app/src/test/java/com/ethk3/novaonepro/RepairStorageCensusR30Test.java'),Path('tools/r30/qual_host/ReportDeliveryR30HostTest.java'),Path('app/src/main/java/com/ethk3/novaonepro/strategyv2/MemoryBudgetR31.java'),Path('app/src/main/java/com/ethk3/novaonepro/strategyv2/FailureDispositionR31.java')]
paths += [Path('app/src/main/java/com/ethk3/novaonepro')/(n+'.java') for n in ['DirectoryCursorR31','StorageBudgetR31','StorageLedgerR31','StorageCensusSU2','ReportIoR31','ReportControlBudgetR31','ReportSemanticVerifierR31','ReportPhaseBudgetR31','ReportCleanupR31','ReportBudgetSU2','ReportPublicationCP12','ReportExportServiceCP4','ReportInputsLeaseCP12','ReportArchiveCP4','ReportArchiveVerifierCP12','ReportNativeCP4','DiagnosticReportProviderR923','ReportAttachmentClosureCP12','ReportCatalogCP4','ReportDiagnosticEvidenceR64','ReportMemberPlanCP12','ReportReadContextCP4','ReportTextCP12','ReportJobJournalCP12','ReportRequestQueueCP12','ReportTaskControlR69','ServiceHeartbeatStore','BackgroundRuntimeCapabilityR811']]
(Path(sys.argv[1])/'source-before.json').write_text(json.dumps({str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in paths if p.is_file()},indent=2)+'\n')
PYSOURCE
javac -encoding UTF-8 -d "$storage_out/runtime" $(rg_files verification/r932/android-json '*.java') verification/canonical/adapters/android/util/AtomicFile.java > "$storage_out/runtime-javac.log" 2>&1
javac -encoding UTF-8 -cp "$storage_sdk/platforms/android-36/android.jar" -d "$storage_out/runtime" $(rg_files tools/r31/storage_host/platform '*.java') >> "$storage_out/runtime-javac.log" 2>&1
storage_sources=(DirectoryCursorR31 StorageBudgetR31 StorageLedgerR31 StorageCensusSU2 ReportIoR31 ReportControlBudgetR31 ReportSemanticVerifierR31 ReportPhaseBudgetR31 ReportCleanupR31 ReportBudgetSU2 ReportPublicationCP12 ReportExportServiceCP4 ReportInputsLeaseCP12 ReportArchiveCP4 ReportArchiveVerifierCP12 ReportNativeCP4 DiagnosticReportProviderR923 ReportAttachmentClosureCP12 ReportCatalogCP4 ReportDiagnosticEvidenceR64 ReportMemberPlanCP12 ReportReadContextCP4 ReportTextCP12 ReportJobJournalCP12 ReportRequestQueueCP12 ReportTaskControlR69 ServiceHeartbeatStore BackgroundRuntimeCapabilityR811)
storage_paths=();for name in "${storage_sources[@]}";do storage_paths+=("app/src/main/java/com/ethk3/novaonepro/$name.java");done
# Dependencies use the prior canonical product classes. This subset compile is not a whole Android build.
javac -encoding UTF-8 -proc:none -cp "$storage_sdk/platforms/android-36/android.jar:$product_classes:$storage_annotation" -sourcepath "$storage_out/no-implicit-source" -d "$storage_out/product" "${storage_paths[@]}" app/src/main/java/com/ethk3/novaonepro/strategyv2/{MemoryBudgetR31,FailureDispositionR31}.java > "$storage_out/product-javac.log" 2>&1
storage_cp="$storage_out/runtime:$storage_out/product:$product_classes:$storage_sdk/platforms/android-36/android.jar:$storage_junit:$storage_hamcrest"
javac -encoding UTF-8 -proc:none -cp "$storage_cp" -d "$storage_out/drivers" app/src/test/java/com/ethk3/novaonepro/R31StorageGovernanceTest.java app/src/test/java/com/ethk3/novaonepro/RepairStorageCensusR30Test.java tools/r31/storage_host/R31StorageProtocolHostTest.java tools/r31/storage_host/R31ReportStorageHostTest.java tools/r31/storage_host/R31ReportSinkHostTest.java tools/r31/storage_host/R31UnifiedReportHostTest.java tools/r31/storage_host/R31LegacyReportHostEntry.java tools/r30/qual_host/ReportDeliveryR30HostTest.java > "$storage_out/driver-javac.log" 2>&1
java -Xmx128m -cp "$storage_out/drivers:$storage_cp" org.junit.runner.JUnitCore com.ethk3.novaonepro.R31StorageGovernanceTest com.ethk3.novaonepro.RepairStorageCensusR30Test > "$storage_out/junit.log" 2>&1
java -Xmx128m -cp "$storage_out/drivers:$storage_cp" com.ethk3.novaonepro.R31StorageProtocolHostTest "$storage_out/fixtures/cross-pid" > "$storage_out/cross-pid.log" 2>&1
python3 - "$storage_out" <<'PY'
import sqlite3,random,re,sys,json
from pathlib import Path
out=Path(sys.argv[1]);source=Path('app/src/main/java/com/ethk3/novaonepro/ReleaseManifestR926.java').read_text()
app=re.search(r'APPLICATION_ID = "(.*?)"',source)[1];db=re.search(r'DATABASE_NAME = "(.*?)"',source)[1]
path=out/'fixtures'/app/'databases'/db;path.parent.mkdir(parents=True)
c=sqlite3.connect(path);c.execute('CREATE TABLE storage_fixture(id INTEGER PRIMARY KEY,payload BLOB NOT NULL)');rng=random.Random(310502)
c.executemany('INSERT INTO storage_fixture(payload) VALUES(?)',((rng.randbytes(8192),) for _ in range(768)));c.commit();c.close()
(out/'fixture-identity.json').write_text(json.dumps({'role':'HOST_SYNTHETIC_RESOURCE_FIXTURE_NOT_NATURAL','applicationId':app,'db':db})+'\n')
PY
storage_app=$(python3 - "$storage_out/fixture-identity.json" <<'PY'
import json,sys
print(json.load(open(sys.argv[1]))['applicationId'])
PY
)
java -Xmx128m -Djava.library.path="$storage_native" -cp "$storage_out/drivers:$storage_cp" com.ethk3.novaonepro.R31ReportStorageHostTest "$storage_out/fixtures/$storage_app" "$storage_out/fixtures/restored.db" > "$storage_out/native-report.log" 2>&1
java -Xmx128m -Djava.library.path="$storage_native" -cp "$storage_out/drivers:$storage_cp" com.ethk3.novaonepro.R31LegacyReportHostEntry "$storage_out/fixtures/r30-regression" > "$storage_out/r30-regression.log" 2>&1
java -Xmx128m -cp "$storage_out/drivers:$storage_cp" com.ethk3.novaonepro.R31ReportSinkHostTest "$storage_out/fixtures/sink-owner" > "$storage_out/report-sinks.log" 2>&1
java -Xmx256m -Djava.library.path="$storage_native" -cp "$storage_out/drivers:$storage_cp" com.ethk3.novaonepro.R31UnifiedReportHostTest "$storage_out/fixtures/unified-owner" > "$storage_out/unified-report.log" 2>&1
python3 - "$storage_out" "$product_classes" <<'PY'
import sys,json,hashlib,subprocess,sqlite3,platform,re
from pathlib import Path
out=Path(sys.argv[1]);proof=out/'file-proofs';proof.mkdir();fixture=json.loads((out/'fixture-identity.json').read_text());source=out/'fixtures'/fixture['applicationId']/'databases'/fixture['db'];restored=out/'fixtures/restored.db'
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
source_before=json.loads((out/'source-before.json').read_text())
assert all(sha(Path(path))==digest for path,digest in source_before.items()),'TESTED_SOURCE_CHANGED_DURING_RUN'
job=out/'fixtures'/fixture['applicationId']/'files/r981_reports/jobs'/format(1,'064x')/'job.json'
assert sha(restored)==json.loads(job.read_text())['snapshotSha'],'RESTORED_PUBLISHED_SNAPSHOT_HASH_MISMATCH'
def logical_rows(path):
    con=sqlite3.connect('file:'+str(path)+'?mode=ro',uri=True);assert con.execute('PRAGMA integrity_check').fetchall()==[('ok',)]
    digest=hashlib.sha256();count=0
    for key,payload in con.execute('SELECT id,payload FROM storage_fixture ORDER BY id'):
        digest.update(key.to_bytes(8,'big'));digest.update(payload);count+=1
    con.close();return count,digest.hexdigest()
assert logical_rows(source)==logical_rows(restored) and logical_rows(restored)[0]==768,'SQLITE_BACKUP_LOGICAL_ROWS_CHANGED'
# SQLite's backup/journal normalization may change header bytes. The frozen snapshot,
# not the mutable source file's header hash, is the archive's exact-byte identity.
for name in ['cleanup-complete.json','cleanup-protected.json']:(proof/name).write_bytes((out/'fixtures'/name).read_bytes())
junit=(out/'junit.log').read_text();count=int(re.search(r'OK \((\d+) tests\)',junit)[1])
paths=[Path('app/src/main/java/com/ethk3/novaonepro')/(name+'.java') for name in ['DirectoryCursorR31','StorageBudgetR31','StorageLedgerR31','StorageCensusSU2','ReportIoR31','ReportControlBudgetR31','ReportSemanticVerifierR31','ReportPhaseBudgetR31','ReportCleanupR31','ReportBudgetSU2','ReportPublicationCP12','ReportExportServiceCP4','ReportInputsLeaseCP12','ReportArchiveCP4','ReportArchiveVerifierCP12','ReportNativeCP4','DiagnosticReportProviderR923','ReportAttachmentClosureCP12','ReportCatalogCP4','ReportDiagnosticEvidenceR64','ReportMemberPlanCP12','ReportReadContextCP4','ReportTextCP12','ReportJobJournalCP12','ReportRequestQueueCP12','ReportTaskControlR69','ServiceHeartbeatStore','BackgroundRuntimeCapabilityR811']]
paths += [Path('app/src/main/cpp/report_native_cp4.c'),Path('app/src/main/java/com/ethk3/novaonepro/strategyv2/MemoryBudgetR31.java'),Path('app/src/main/java/com/ethk3/novaonepro/strategyv2/FailureDispositionR31.java'),Path('app/src/test/java/com/ethk3/novaonepro/R31StorageGovernanceTest.java'),Path('app/src/test/java/com/ethk3/novaonepro/RepairStorageCensusR30Test.java'),*Path('tools/r31/storage_host').rglob('*'),Path('tools/r30/qual_host/ReportDeliveryR30HostTest.java')]
receipt={'format':'R31_STORAGE_REPORT_HOST_RECEIPT_V1','status':'PASS','sourceHead':subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip(),'sourceDirty':bool(subprocess.check_output(['git','status','--porcelain'],text=True).strip()),'scope':'COMPILED_SOURCE_SUBSET_WITH_EXPLICIT_DEPENDENCY_CLASSPATH; REAL_HOST_FILES_PROCESSES_SQLITE_ZSTD_JNI; NOT_ANDROID_VM_OR_APK_OR_HANDSET_OR_NATURAL_QUALITY','dependencyClasses':sys.argv[2],'dependencyGate':'EXPLICIT_CLASSPATH_API_BINDING_ONLY; WHOLE_ANDROID_SOURCE_GATE_REQUIRED_SEPARATELY','hostArchitecture':platform.machine(),'junitTests':count,'crossPidOsLocksAndKilledOwner':'PASS','nativeBackupChunkAdmissionAndClosedResources':'PASS','nativeDeferredCacheCommitRegression':'PASS','publishedCleanupAndProtectedMissingRaw':'PASS','actualReportSinksAndDynamicPhaseAdmission':'PASS','actualServicePublishImmediateReadyGrantAndLostAckRecovery':'PASS_HOST_FRAMEWORK_TRANSPORT_ADAPTED','semanticZipReadback':'PASS_SYNTHETIC_CHECK_INPUTS_NOT_96_EXECUTED_BUSINESS_CHECKS','typedAdmissionFailures':'PASS','r30Zip20MiBSplitTamperCancelRegression':'PASS','restoredDatabase':{'sha256':sha(restored),'integrity':'ok','fixtureRows':768,'logicalSourceRowsEqual':True},'migration':'NO_NEW_SQL_SCHEMA; FILE_LEDGER_V1','device24h':'NOT_RUN','device72h':'NOT_RUN','original30GB':'UNAVAILABLE','allocatedMeasurement':'ANDROID_OS_STAT_PATH_COMPILED_NOT_RUN; native host Linux stat(st_blocks*512) actual; host Census reports UNKNOWN','fixtureClock':'CONTROLLED_RING_AND_RETRY_NOT_ELAPSED_RUNTIME','sourceSha256':{str(p):sha(p) for p in paths if p.is_file()},'compiledSubsetSha256':{str(p.relative_to(out/'product')):sha(p) for p in (out/'product').rglob('*.class')}}
(out/'receipt.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n');print('R31_STORAGE_REPORT_HOST|JUNIT='+str(count)+'|CROSS_PID_NATIVE_CLEANUP_R30_REGRESSION|PASS|NOT_ANDROID_OR_SOAK_OR_NATURAL')
PY
