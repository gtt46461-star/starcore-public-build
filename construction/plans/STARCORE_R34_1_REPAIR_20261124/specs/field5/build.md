# B01 开工、换版和断点续工

第 4 版命令先校验执行者给的本地方案根，再调用 prepare_worktree。后者对指定 Git 提交只检查四处入口存在和 descriptor 中的底座 SHA。用真实临时 Git 复现：在选定提交插入一份未被封印的脚本，旧工具仍返回 PLAN_IMPORT_STAGED；把实机原件提交在施工分支对应证据目录后升级方案，旧工具的整目录 restore 将原件暂存为删除；本地被 Git 忽略的文件若恰与新版新增证据同名，旧工具会用发布内容覆盖本机字节。另一个复现分支是已存在施工分支，但用户改用新的工作目录。其差异必须在创建工作树前检查。

本版工具先从指定 plan commit 的 Git blob 取出方案，按实际 manifest 成员与 SHA 核对封印，并将第一份原始报告单独与封印内 BASELINE_FACTS 的 SHA-256 对齐。第二份报告按既有说明复查，开工时可使用库内派生证据，再按 baseline、该提交及历史发布提交辨认此前由方案管理的路径。已有应用提交、同证据目录另存的施工现场原件、与导入无交叉的应用工作可保留。只把上一次发布和这一次发布之间实际变化的已登记资料路径暂存；正式删除的旧成员也按原始归属处理。同时核对 staged、unstaged、untracked（含 ignored）同名冲突。碰撞时在任何导入操作前报出具体路径，在新位置保留原件或将冲突字节永久归档、核实，再重试。若已有分支已导入当前版本，换工作树时返回 EXISTING_PLAN_VERIFIED，无需空提交。

先在承载方案的准确 Git 提交设置 PLAN_ROOT 和 PLAN_COMMIT。先从此根完整执行 verify_plan，再传同一份 manifest 的内容摘要给 prepare_worktree，最后从实际应用工作树重跑校验。

```bash
p=construction/plans/STARCORE_R34_1_REPAIR_20261124
python3 -B "$PLAN_ROOT/tools/verify_plan.py" --plan "$PLAN_ROOT"
plan_manifest_sha=$(python3 -c 'import hashlib,pathlib,sys; print(hashlib.sha256(pathlib.Path(sys.argv[1]).read_bytes()).hexdigest())' "$PLAN_ROOT/MANIFEST_SHA256.json")
python3 -B "$PLAN_ROOT/tools/prepare_worktree.py" --repo "$REPO_ROOT" --plan-ref "$PLAN_COMMIT" --expected-manifest-sha256 "$plan_manifest_sha" --worktree "$R341_WORKTREE"
python3 -B "$PLAN_ROOT/tools/prepare_worktree.py" --repo "$REPO_ROOT" --plan-ref "$PLAN_COMMIT" --expected-manifest-sha256 "$plan_manifest_sha" --worktree "$R341_WORKTREE" --create
python3 -B "$R341_WORKTREE/$p/tools/verify_plan.py" --plan "$R341_WORKTREE/$p"
git -C "$R341_WORKTREE" diff --cached --name-status
```

PLAN_IMPORT_STAGED 或 PLAN_ALREADY_STAGED_VERIFY_AND_COMMIT 后检查暂存的是本次资料，再提交。EXISTING_PLAN_VERIFIED 只确认工作树方案字节，真实产品改动、构建和已签收批次按各自的回执继续检查。工程执行台账和原始回执放到施工分支专用位置；方案目录下的初始台账属于封印模板。

B01 的 R341-B01-01-T01 用临时 Git 重放首开工、正常续工、跳过重做、缺失/额外封印成员、第一份原报告错包或缺包、换工作树、已提交的施工证据、同名 ignored 文件、旧正式成员删除。保留运行前后提交、缓存区和仅变动的实际路径，以及校验的准确方案提交和 manifest SHA。运行本方案宿主测试后，Android 施工仍独立运行真实 Gradle/安装/构建验证。
