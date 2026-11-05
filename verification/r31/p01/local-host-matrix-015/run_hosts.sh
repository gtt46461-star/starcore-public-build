#!/usr/bin/env bash
# NON-AUTHORITATIVE local HOST matrix (JDK21 javac + derived stub android.jar). Mirrors CI list plus newer R31 runners.
set -uo pipefail
W=/tmp/claude-0/wt-host; L=/tmp/claude-0/hostlogs; rm -rf $L; mkdir -p $L
export ANDROID_HOME=/tmp/claude-0/android-home SQLITE_JDBC=/tmp/claude-0/sdk/sqlite-jdbc.jar GRADLE_USER_HOME=$HOME/.gradle
cd $W
run(){ name=$1; shift; s=$(date +%s); if timeout 1800 "$@" > $L/$name.log 2>&1; then r=PASS; else r="FAIL:$?"; fi; echo "$name|$r|$(( $(date +%s)-s ))s" | tee -a $L/SUMMARY.txt; }
for c in diagnosis writer retention timeline commit_failure progress neural qualification input_pin matrix research observatory snapshot; do run r30repair-$c python3 tools/r30-repair/run_${c}_host.py; done
for c in memory pitguard_allocation runtime_memory_control resource_recovery; do run r31-verify-$c python3 tools/r31/verify_$c.py; done
for c in task_runtime write_protocol service_protocol runtime_ddl research_schema; do run r31-$c python3 tools/r31/run_${c}_host.py; done
for k in R31RuntimeHealthHost R31ResourceRecoveryConsumerHost; do R31_RUNTIME_HOST_CLASS=$k R31_RUNTIME_OUTPUT=verification/r31/ci-scoped-$k run r31-$k python3 tools/r31/run_task_runtime_host.py; done
echo "---- not in CI list ----" >> $L/SUMMARY.txt
C=$W/app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes; O=/tmp/claude-0/hostnew; rm -rf $O; mkdir -p $O
run r31new-atom_publication python3 tools/r31/run_atom_publication_host.py --output $O/atom
run r31new-evolution_publication python3 tools/r31/run_evolution_publication_host.py
run r31new-report_cas_scope python3 tools/r31/run_report_cas_scope_host.py --out $O/cas --classes $C
run r31new-report_history_reader python3 tools/r31/run_report_history_reader_host.py --out $O/history --classes $C
run r31new-telemetry_budget python3 tools/r31/run_telemetry_budget_host.py --output $O/telemetry
run r31new-runtime_ddl_writer_regression python3 tools/r31/run_runtime_ddl_writer_regression.py
for c in runtime_observation task_status; do run r31new-verify-$c python3 tools/r31/verify_$c.py; done
echo DONE >> $L/SUMMARY.txt
