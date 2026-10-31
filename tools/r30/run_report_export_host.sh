#!/usr/bin/env bash
# Original production JNI, compiled product owners, AOSP JSON and real files; no Android runtime/phone claim.
set -euo pipefail
cd "$(dirname "$0")/../.."
report_out=${REPORT_EXPORT_OUT:?fresh evidence output directory required}
product_classes=${CLASSES:-app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes}
android_sdk=${ANDROID_HOME:-${ANDROID_SDK_ROOT:?}}
mkdir -p "$report_out/runtime" "$report_out/drivers"
if [ -n "${REPORT_NATIVE_DIR:-}" ]; then
  report_native=$REPORT_NATIVE_DIR
  python3 - "$report_native/receipt.json" <<'PY'
import sys,json,hashlib
from pathlib import Path
receipt=json.loads(Path(sys.argv[1]).read_text())
for item in receipt['source_files']:
    if hashlib.sha256(Path(item['path']).read_bytes()).hexdigest()!=item['sha256']:
        raise SystemExit('HOST_NATIVE_SOURCE_CHANGED_REBUILD_REQUIRED')
if hashlib.sha256(Path(receipt['library_path']).read_bytes()).hexdigest()!=receipt['library_sha256']:
    raise SystemExit('HOST_NATIVE_LIBRARY_CHANGED')
PY
else
  report_native="$report_out/native"
  python3 verification/batch01/build_host_report_native.py --jdk "$JAVA_HOME" --output-dir "$report_native" > "$report_out/native-build.log" 2>&1
fi
javac -nowarn -encoding UTF-8 -d "$report_out/runtime" $(rg --files verification/r932/android-json -g '*.java') verification/canonical/adapters/android/util/AtomicFile.java > "$report_out/runtime-compile.log" 2>&1
javac -nowarn -encoding UTF-8 -proc:none -cp "$report_out/runtime:$product_classes:$android_sdk/platforms/android-36/android.jar" -d "$report_out/drivers" tools/r30/qual_host/ReportDeliveryR30HostTest.java > "$report_out/driver-compile.log" 2>&1
java -Xmx128m -Djava.library.path="$report_native" -cp "$report_out/runtime:$report_out/drivers:$product_classes:$android_sdk/platforms/android-36/android.jar" com.ethk3.novaonepro.ReportDeliveryR30HostTest "$report_out/files" > "$report_out/execution.log" 2>&1
cat "$report_out/execution.log"
python3 - "$report_out" "${REPORT_SCOPE:-ACTUAL_COMPILED_APP_CLASSES}" <<'PY'
import sys,json,hashlib,subprocess,shutil
from pathlib import Path
root=Path(sys.argv[1]);names=['ReportPublicationCP12','ReportTaskControlR69','ReportJobJournalCP12','ReportExportServiceCP4','CompleteReportFileR927','DiagnosticReportProviderR923']
paths=[Path('app/src/main/java/com/ethk3/novaonepro')/(n+'.java') for n in names]
paths += [Path('tools/r30/qual_host/ReportDeliveryR30HostTest.java')]
(root/'receipt.json').write_text(json.dumps({'source':subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip(),'status':'PASS','scope':sys.argv[2]+'; original JNI x86_64 and real filesystem, synthetic data; NOT_ANDROID_OR_PHONE_OR_NATURAL','productSourceSha256':{str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in paths}},indent=2)+'\n')
# Preserve the small actual proofs. Large incompressible inputs are synthetic fixtures, not evidence to upload.
# This block is reached only after the whole Java driver passes; failures retain their fixture files.
fixtures=root/'files';sentinel=fixtures/'original-model-protected.bin'
if fixtures.is_symlink() or not fixtures.is_dir() or sentinel.read_text()!='PRESERVE_ORIGINAL_PRODUCT_EVIDENCE':
    raise SystemExit('SYNTHETIC_FIXTURE_CLEANUP_IDENTITY')
proofs=root/'file-proofs';proofs.mkdir()
for relative in ('delivery/index.json','cancel-receipt-r30.json'):
    for path in (fixtures/'r981_reports/jobs').glob('*/'+relative):
        shutil.copyfile(path,proofs/(path.relative_to(fixtures/'r981_reports/jobs').as_posix().replace('/','-')))
shutil.rmtree(fixtures)
PY
