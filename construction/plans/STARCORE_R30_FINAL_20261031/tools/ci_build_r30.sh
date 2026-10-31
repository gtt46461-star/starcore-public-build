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
javac -nowarn -d "$TMP" -cp "$CLASSES" tools/r30/host/SqlContractDumpR30.java
java -cp "$TMP:$CLASSES" SqlContractDumpR30 > "$LOG/sql_contracts.json"
python3 tools/r30/sql_contract_tests.py "$LOG/sql_contracts.json" --out "$LOG/sql_contract_results.json"
CARD=$(git log -1 --format=%s | grep -oE '^R30-B[0-9]{2}-[0-9]{2}' || true)
if [ -n "$CARD" ]; then
  python3 tools/r30/ownership_check.py --card "$CARD" --base HEAD~1 --registry "$PLAN/registries/file_ownership.json" | tee "$LOG/ownership_$CARD.json"
fi
python3 tools/r30/scan_table_writers.py . "$LOG/r27_writers.json" --prefix r27_ | tee "$LOG/scan.txt"
echo "R30_EXTRA_GATES_OK"
