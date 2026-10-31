#!/usr/bin/env bash
# R30 追加门禁（由 tools/r30/ci_build.sh 在 R29 原有全部检查之后调用）。参数：$1 = 日志目录。
# 1) SQL 合同：从已编译的 app 类导出 SQL 原字符串，在 sqlite3 上跑 T01–T06
# 2) 所有权：HEAD 提交信息以 "R30-Bxx-yy" 开头时，检查该提交只改了该卡登记的文件
# 3) 写点扫描：生成 r27_* 写点清单（供 B11 与审查）
set -euo pipefail
LOG="${1:-evidence/r30/ci}"; mkdir -p "$LOG"
PLAN=construction/plans/STARCORE_R30_FINAL_20261031
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes
TMP=$(mktemp -d)
# 合同导出类引用了后续卡（B01-02/B02-01/条件B11-02）才加入应用的类：已加入应用的取已编译应用类，
# 尚未加入的取方案包 code/main 原文（与卡施工时原样复制的字节相同），并记录来源。
STAGE="$TMP/src/com/ethk3/novaonepro/strategyv2"; mkdir -p "$STAGE"; : > "$LOG/sql_contract_sources.txt"
for f in "$PLAN"/code/main/com/ethk3/novaonepro/strategyv2/*.java; do c=$(basename "$f" .java)
  if [ -f "$CLASSES/com/ethk3/novaonepro/strategyv2/$c.class" ]; then echo "$c APP" >> "$LOG/sql_contract_sources.txt"; else cp "$f" "$STAGE/"; echo "$c PLAN_NOT_YET_LANDED" >> "$LOG/sql_contract_sources.txt"; fi
done
javac -nowarn -d "$TMP" -cp "$CLASSES" -sourcepath "$TMP/src:tools/r30/host/android_api_stub" tools/r30/host/SqlContractDumpR30.java
java -cp "$TMP:$CLASSES" SqlContractDumpR30 > "$LOG/sql_contracts.json"
python3 tools/r30/sql_contract_tests.py "$LOG/sql_contracts.json" --out "$LOG/sql_contract_results.json"
# CI 可能在卡提交之上追加生成身份提交（R29: bind ...），因此取最近一个以卡号开头的提交检查其单提交改动。
CARD_SHA=$(git log -50 --format='%H %s' | grep -E '^[0-9a-f]{40} R30-B[0-9]{2}-[0-9]{2}' | head -1 | cut -d' ' -f1 || true)
if [ -n "$CARD_SHA" ]; then
  CARD=$(git log -1 --format=%s "$CARD_SHA" | grep -oE '^R30-B[0-9]{2}-[0-9]{2}')
  python3 tools/r30/ownership_check.py --card "$CARD" --base "$CARD_SHA~1" --head "$CARD_SHA" --registry "$PLAN/registries/file_ownership.json" | tee "$LOG/ownership_$CARD.json"
fi
python3 tools/r30/scan_table_writers.py . "$LOG/r27_writers.json" --prefix r27_ | tee "$LOG/scan.txt"
echo "R30_EXTRA_GATES_OK"
