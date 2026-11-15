#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
: "${R31_PHONE_OUT:?fresh output directory required}"
: "${JAVA_HOME:?activate build environment}"
test ! -e "$R31_PHONE_OUT"
mkdir -p "$R31_PHONE_OUT/runtime" "$R31_PHONE_OUT/product" "$R31_PHONE_OUT/drivers"
phone_classes=${CLASSES:-app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes}
phone_sdk=${ANDROID_HOME:-${ANDROID_SDK_ROOT:?}}
phone_native=${REPORT_NATIVE_DIR:-$R31_PHONE_OUT/native}
if [ -n "${REPORT_NATIVE_DIR:-}" ]; then
    python3 verification/batch01/build_host_report_native.py --verify-reuse "$phone_native"
else
    python3 verification/batch01/build_host_report_native.py --jdk "$JAVA_HOME" --output-dir "$phone_native" > "$R31_PHONE_OUT/native-build.log" 2>&1
fi
mapfile -t phone_json < <(find verification/r932/android-json -name '*.java' -type f | sort)
mapfile -t phone_platform < <(find tools/r31/storage_host/platform -name '*.java' -type f | sort)
javac -encoding UTF-8 -d "$R31_PHONE_OUT/runtime" "${phone_json[@]}" verification/canonical/adapters/android/util/AtomicFile.java
javac -encoding UTF-8 -cp "$phone_sdk/platforms/android-36/android.jar" -d "$R31_PHONE_OUT/runtime" "${phone_platform[@]}"
phone_sources=();for name in ReportExportServiceCP4 ReportControlBudgetR31 ReportIoR31 ReportJobJournalCP12 CompleteReportFileR927;do phone_sources+=("app/src/main/java/com/ethk3/novaonepro/$name.java");done
javac -encoding UTF-8 -proc:none -cp "$phone_sdk/platforms/android-36/android.jar:$phone_classes" -sourcepath "$R31_PHONE_OUT/no-source" -d "$R31_PHONE_OUT/product" "${phone_sources[@]}"
phone_cp="$R31_PHONE_OUT/runtime:$R31_PHONE_OUT/product:$phone_classes:$phone_sdk/platforms/android-36/android.jar"
javac -encoding UTF-8 -proc:none -cp "$phone_cp" -d "$R31_PHONE_OUT/drivers" tools/r31/storage_host/R31PhoneReportHostTest.java
python3 - "$R31_PHONE_OUT" <<'PY'
import hashlib,json,sqlite3,sys
from pathlib import Path
out=Path(sys.argv[1]);source=out/'source.db';target=165824
with sqlite3.connect(source) as db:
    db.execute('PRAGMA page_size=4096');db.execute('CREATE TABLE synthetic_large(payload BLOB)')
    db.execute('INSERT INTO synthetic_large VALUES(zeroblob(?))',((target-2)*4092,));db.commit()
    pages=db.execute('PRAGMA page_count').fetchone()[0]
    while pages<target:
        db.execute('INSERT INTO synthetic_large VALUES(zeroblob(?))',(max(1,(target-pages-1)*4092),));db.commit()
        pages=db.execute('PRAGMA page_count').fetchone()[0]
    assert pages==target,(pages,target)
    assert db.execute('PRAGMA quick_check').fetchall()==[('ok',)]
(out/'fixture.json').write_text(json.dumps({'scope':'HOST_SYNTHETIC_NOT_USER_DATABASE','pages':pages,'pageSize':4096,'bytes':source.stat().st_size})+'\n')
PY
java -Xmx256m -Djava.library.path="$phone_native" -cp "$R31_PHONE_OUT/drivers:$phone_cp" com.ethk3.novaonepro.R31PhoneReportHostTest "$R31_PHONE_OUT/app" "$R31_PHONE_OUT/source.db" | tee "$R31_PHONE_OUT/phone-report.log"
python3 - "$R31_PHONE_OUT" <<'PY'
import hashlib,json,sqlite3,subprocess,sys
from pathlib import Path
out=Path(sys.argv[1]);job=out/'app/files/r981_reports/jobs'/format(1,'064x');snapshot=job/'snapshot.db'
def sha(p):
    h=hashlib.sha256()
    with p.open('rb') as f:
        while b:=f.read(1<<20):h.update(b)
    return h.hexdigest()
with sqlite3.connect('file:'+str(snapshot)+'?mode=ro',uri=True) as db:
    assert db.execute('PRAGMA integrity_check').fetchall()==[('ok',)]
    assert db.execute('PRAGMA page_count').fetchone()[0]==165824
receipt={'status':'PASS','sourceHead':subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip(),'scope':'HOST_REAL_PRODUCTION_CALLBACKS_NATIVE_SQLITE_AND_FILES; FRAMEWORK_TRANSPORT_ADAPTED; SYNTHETIC_DATABASE; NOT_ANDROID_OR_HANDSET','fixture':json.loads((out/'fixture.json').read_text()),'snapshotBytes':snapshot.stat().st_size,'snapshotSha256':sha(snapshot),'checks':['both-progress-files-fail','final-callback-fails-sample','post-backup-content-durable','sample-retry-throttled','real-refund-failure-preserves-first-cause-on-next-stage','real-admission-rejection','user-cancel','ack-only-identity-history','recovery-size-sha-crc','corrupt-and-missing-recovery-ack-fallback','credential-redaction','read-only-no-rebackup'],'physicalDevice':'NOT_RUN'}
(out/'receipt.json').write_text(json.dumps(receipt,indent=2)+'\n');print(json.dumps(receipt))
PY
