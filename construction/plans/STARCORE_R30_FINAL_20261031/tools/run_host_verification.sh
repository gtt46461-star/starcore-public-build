#!/usr/bin/env bash
# 在任何有 JDK17+ 与 python3 的机器上复现本包的宿主验证（不需要 Android SDK）。
# 用法：bash tools/run_host_verification.sh <bb607cf 检出的仓库根> [输出目录]
set -euo pipefail
export LC_ALL=C.UTF-8 LANG=C.UTF-8
PLAN=$(cd "$(dirname "$0")/.." && pwd); REPO=$(cd "$1" && pwd); OUT=${2:-$PLAN/evidence}
test "$(git -C "$REPO" rev-parse HEAD)" = bb607cf5ab69f7286f859e43f87ada85a5bc456d || { echo "REPO 必须检出 bb607cf"; exit 2; }
W=$(mktemp -d); mkdir -p "$W/stub/com/ethk3/novaonepro" "$W/pure" "$W/android"; cd "$W"
cp "$PLAN/tools/host/ResearchSemanticsGeneratedR967_HOST_STUB.java" "$W/stub/com/ethk3/novaonepro/ResearchSemanticsGeneratedR967.java"
PURE="WriterHoldLedgerR30 ChunkedWriterR30 TaskLivenessR30 IssueTimelineR30 FetchBackoffR30 PitGuardR30 PolicyContractR30 BehaviorSignatureR30 CalibrationR30 DecisionChainR30 StructureWindowR30 SetIntersectionPolicyR30"
SRC=""; for c in $PURE; do SRC="$SRC $PLAN/code/main/com/ethk3/novaonepro/strategyv2/$c.java"; done
# 1) 纯 Java 类 + 测试：对 bb607cf 真实源码编译（只有 Gradle 生成类用宿主桩）
javac -nowarn -cp "$W/pure" -d "$W/pure" -sourcepath "$REPO/app/src/main/java:$W/stub:$PLAN/tools/host/junit_shim" $SRC $PLAN/code/test/com/ethk3/novaonepro/strategyv2/*.java "$PLAN/tools/host/junit_shim/HostJUnitRunner.java" 2>&1 | tee "$OUT/compile_pure_against_bb607cf.log"
TESTS=$(cd "$PLAN/code/test" && ls com/ethk3/novaonepro/strategyv2/*.java | sed "s|/|.|g; s|\.java$||")
java -cp "$W/pure" HostJUnitRunner $TESTS | tee "$OUT/host_junit_r30.log"
# 1b) 逐卡累积编译：任何一张卡都不引用后面卡才加入的类
python3 "$PLAN/tools/check_card_compile_order.py" "$REPO" --out "$OUT/card_compile_order.json" | tee "$OUT/card_compile_order.log"
# 2) Android 侧类：用 Android API 桩编译（桩只含本包用到的 API，签名已对照 bb607cf）
javac -nowarn -d "$W/android" -sourcepath "$PLAN/tools/host/android_api_stub:$PLAN/code/main" $PLAN/code/main/com/ethk3/novaonepro/strategyv2/{TableCopierR30,R29FoldBackR30,OfflineCutoverR30,HoldLedgerFlushR30,IssueTimelineWriterR30,CoordinatorTxR30}.java 2>&1 | tee "$OUT/compile_android_side_with_api_stub.log"
# 3) SQL 合同：从 Java 导出原字符串后在 sqlite3 执行
javac -nowarn -d "$W/android" -cp "$W/android" -sourcepath "$PLAN/tools/host/android_api_stub:$PLAN/code/main" "$PLAN/tools/host/SqlContractDumpR30.java"
java -cp "$W/android" SqlContractDumpR30 > "$OUT/sql_contracts_exported.json"
python3 "$PLAN/tools/sql_contract_tests.py" "$OUT/sql_contracts_exported.json" --out "$OUT/sql_contract_results.json" | tee "$OUT/sql_contract_tests.log"
# 4) 补丁：按序应用到 bb607cf 临时检出
T=$(mktemp -d); git -C "$REPO" worktree add -q --detach "$T/wt" bb607cf5ab69f7286f859e43f87ada85a5bc456d
( cd "$T/wt" && git -c user.email=v@x -c user.name=v am -q "$PLAN"/code/patches/*.patch && git log --oneline -5 ) | tee "$OUT/patch_apply_check.txt"
git -C "$REPO" worktree remove --force "$T/wt"
# 5) 方案自检
python3 "$PLAN/tools/validate_plan.py" --out "$OUT/plan_validation.json" | tee "$OUT/plan_validation.log"
echo HOST_VERIFICATION_DONE
