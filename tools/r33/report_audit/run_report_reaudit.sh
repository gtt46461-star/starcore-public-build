#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../../.."
: "${R33_DEPENDENCY_CLASSES:?whole-app compiled dependency classes}"
: "${R33_ANDROID_JAR:?Android API jar}"
: "${R33_SQLITE_JDBC:?SQLite JDBC native distribution jar}"
: "${R33_SLF4J_JAR:?SLF4J API jar}"
: "${R33_INPUT_DIR:?directory containing report326_input.json and report425_input.json from the unmodified original reports}"
: "${R33_REPORT_OUT:?new absolute output directory}"
if [ -e "$R33_REPORT_OUT" ]; then echo 'R33_OUTPUT_ALREADY_EXISTS' >&2;exit 1;fi
mkdir -p "$R33_REPORT_OUT/classes"
r33_base_cp="$R33_DEPENDENCY_CLASSES:$R33_ANDROID_JAR"
r33_sources=(ReportEvidenceProjectionR33 ReportAssessmentR33 CapabilityRuntimeEvidenceR33 ReportObjectIndexR30 ReportChartsR30 ReportR29 RepairDiagnosisR32 ResearchTableAuditR32 ReportSnapshotR30 MetricEvaluatorR29 MetricDefinitionsR29 Q08ReportProjectionR325 Q08EvidenceReaderR325 AtomNetPublicationR31)
r33_files=();for r33_name in "${r33_sources[@]}";do r33_files+=("app/src/main/java/com/ethk3/novaonepro/strategyv2/$r33_name.java");done
r33_files+=(app/src/main/java/com/ethk3/novaonepro/ReportSemanticVerifierR31.java app/src/main/java/com/ethk3/novaonepro/ReportExportServiceCP4.java)
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -proc:none -cp "$r33_base_cp" -d "$R33_REPORT_OUT/classes" "${r33_files[@]}" tools/r33/report_audit/ReportReauditR33HostTest.java tools/r33/report_audit/ReportReRenderR33.java > "$R33_REPORT_OUT/compile.log" 2>&1
r33_cp="$R33_REPORT_OUT/classes:$r33_base_cp:$R33_SQLITE_JDBC:$R33_SLF4J_JAR"
for r33_number in 326 425;do
 java -XX:-UsePerfData -Xmx64m -cp "$r33_cp" com.ethk3.novaonepro.strategyv2.ReportReRenderR33 "$R33_INPUT_DIR/report${r33_number}_input.json" "$R33_REPORT_OUT/report${r33_number}_r33" > "$R33_REPORT_OUT/report${r33_number}.log"
done
java -XX:-UsePerfData -Xmx128m -cp "$r33_cp" com.ethk3.novaonepro.strategyv2.ReportReauditR33HostTest "$R33_REPORT_OUT/report425_r33.json" > "$R33_REPORT_OUT/host_regression.log" 2>&1
python3 - "$R33_REPORT_OUT" "${r33_files[@]}" <<'PY'
import json,hashlib,sys,pathlib
out=pathlib.Path(sys.argv[1]);s={p:hashlib.sha256(pathlib.Path(p).read_bytes()).hexdigest() for p in sys.argv[2:]+['tools/r33/report_audit/ReportReauditR33HostTest.java','tools/r33/report_audit/ReportReRenderR33.java','tools/r33/report_audit/run_report_reaudit.sh']};log=(out/'host_regression.log').read_text();assert 'ASSERTIONS=37|PASS' in log
r={'format':'R33_REPORT_HOST_REAUDIT_RECEIPT_1','status':'PASS','assertions':37,'scope':'PRODUCTION_SOURCE_SUBSET_PLUS_EXPLICIT_DEPENDENCY_CLASSES; ACTUAL_HOST_SQLITE; OFFLINE_CAPTURED_REPORT_RERENDER; NOT_ANDROID_OR_NATURAL_QUALITY','sourceSha256':s,'rerenders':{n:json.loads((out/f'report{n}_r33_receipt.json').read_text()) for n in ['326','425']}}
(out/'receipt.json').write_text(json.dumps(r,ensure_ascii=False,indent=2)+'\n');print('R33_REPORT_REAUDIT_HOST|37_ASSERTIONS|TWO_ORIGINAL_REPORT_RERENDERS|64M_HEAP_RENDER|PASS|NOT_ANDROID_OR_NATURAL')
PY
