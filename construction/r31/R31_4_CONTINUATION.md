# R31.4 施工接续（K06 进行中）

更新：2026-11-07。分支 `construction/r31-final-20261104`。流程照 `R31_3_接续施工说明.md` 第四节（改代码 → `repin_scope.py` → 本地快检 → push → `ci_wait.sh` → close）。

## 已关门

| 批 | 收据 | 单测 | LOCAL | 本批修掉的已知失败 |
|---|---|---|---|---|
| K01 | R31.3 | 500 | 6 | （登记基线） |
| K02 | `construction/r31/batches/K02/RECEIPT.json` | 509/509 | 10/10 + 结转2 | 11 CapacityWait、verify_family_admission、verify_baseline_selftest |
| K03 | K03/RECEIPT.json | 514/514 | 18/18 | run_writer_hold_fault_host、r30repair_commit_failure |
| K04 | K04/RECEIPT.json | 521/521 | 29/29 | — |
| K05 | K05/RECEIPT.json | 534/534 | 12/12 | — |

剩余已知失败：`HOST:verify_ast_family_scope`(K06)、`HOST:run_legacy_qualification_contract`(K08)、`HOST:r30repair_retention`(K09)。

## K06 当前进度

- `d3e1c85e` 推送全部 24 项 LOCAL 的实现与测试，BINDINGS.json 已建（24/24 绑定，scope_extensions：CreationBudgetR31、EvalContextR27）。
- 新类：CreationBudgetR31、VerifiedImprovementR31、ResearchCapabilityExperimentR31、ResearchHypothesisR31、RegimeHypothesisR31。
- CI 身份失败根因：取消的运行提交了只含 Gradle 开场横幅的 15 行 gradle.log，下一轮配置期日志与之逐字相同 → source_dirty 先 False 后 True。workflow 已在构建前 `git rm --cached` 本批 logs。
- 待：等 CI（ci_wait.sh K06），按失败修，summary 新鲜后 close。

## 本轮做法要点（已验证有效）

1. 本地无 SDK：纯 Java 类可用 `javac -sourcepath app/src/main/java:<桩目录>` 加 junit 快检；Android 依赖类只能交 CI。
2. 提交前一律 `repin_scope.py --batch Kxx [--role 新文件=角色] && r967 --check && verify_baseline --identity-only`，任一步失败不提交（`71a036f8` 曾因 repin 失败仍推送，已在 `f9926c39` 补登记）。
3. 不在 TABLE 的生产文件改动：先写 BINDINGS.json `scope_extensions`（理由≥10字）；受控输入新增需 `--role`。
4. 每次 push 会取消上一轮 CI（concurrency），尽量一批改完再推。
5. 两个内存 HOST（verify_pin_capacity、verify_resource_recovery）各偶发失败过一次，重跑通过；若复现需深查。

## K11 前须用户决定

- K11 需在 CI 增加模拟器任务（EMU 8 项只在 K11 跑）。
- 签名密钥放置位置由用户决定。
