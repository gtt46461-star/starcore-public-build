#!/usr/bin/env python3
"""逐卡累积编译检查：按卡序，只把“到本卡为止已 ADD 的 R30 纯 Java 类 + 本卡测试”与 bb607cf 源码一起编译，
证明任何一张卡都不引用后面卡才加入的类（避免施工中途编译失败）。Android 侧类不在此检查（需 SDK），由 CI 编译覆盖。
用法：python3 tools/check_card_compile_order.py <bb607cf 检出根> [--out evidence/card_compile_order.json]"""
import json, os, shutil, subprocess, sys, tempfile
PLAN = os.path.dirname(os.path.dirname(os.path.abspath(__file__))); REPO = sys.argv[1]
cards = json.load(open(os.path.join(PLAN, "registries", "cards.json")))["cards"]
ANDROID = {"TableCopierR30", "R29FoldBackR30", "OfflineCutoverR30", "HoldLedgerFlushR30", "IssueTimelineWriterR30", "CoordinatorTxR30"}
work = tempfile.mkdtemp(); src = os.path.join(work, "src", "com", "ethk3", "novaonepro", "strategyv2"); os.makedirs(src)
stub = os.path.join(work, "stub", "com", "ethk3", "novaonepro"); os.makedirs(stub)
shutil.copy(os.path.join(PLAN, "tools", "host", "ResearchSemanticsGeneratedR967_HOST_STUB.java"), os.path.join(stub, "ResearchSemanticsGeneratedR967.java"))
res = []; ok_all = True
for c in cards:
    tests = []
    for f in c["files"]:
        name = os.path.basename(f["path"])[:-5] if f["path"].endswith(".java") else None
        if f["action"] == "ADD" and name and name not in ANDROID and "/main/java/" in f["path"]:
            shutil.copy(os.path.join(PLAN, "code", "main", "com", "ethk3", "novaonepro", "strategyv2", name + ".java"), src)
        if f["action"] == "ADD" and name and "/test/java/" in f["path"]:
            tests.append(os.path.join(PLAN, "code", "test", "com", "ethk3", "novaonepro", "strategyv2", name + ".java"))
    if not tests: continue
    out = os.path.join(work, "out-" + c["id"]); os.makedirs(out)
    files = [os.path.join(src, x) for x in os.listdir(src)] + tests
    r = subprocess.run(["javac", "-nowarn", "-cp", out, "-d", out, "-sourcepath",
                        os.pathsep.join([os.path.join(REPO, "app/src/main/java"), os.path.join(work, "stub"), os.path.join(PLAN, "tools/host/junit_shim")])] + files,
                       capture_output=True, text=True, cwd=work)
    ok = r.returncode == 0; ok_all &= ok
    res.append({"card": c["id"], "tests": [os.path.basename(t) for t in tests], "r30_classes_available": sorted(os.listdir(src)), "compiled": ok, "stderr": r.stderr[-800:] if not ok else ""})
    print(("PASS " if ok else "FAIL ") + c["id"] + " " + ",".join(os.path.basename(t) for t in tests))
if "--out" in sys.argv: json.dump({"pass": ok_all, "cards": res}, open(sys.argv[sys.argv.index("--out") + 1], "w"), ensure_ascii=False, indent=1)
sys.exit(0 if ok_all else 1)
