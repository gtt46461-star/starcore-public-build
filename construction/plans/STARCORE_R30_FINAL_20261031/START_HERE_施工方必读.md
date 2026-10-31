# 施工方必读（直接照做，不需要另写方案）

你负责执行 STARCORE R30。仓库 `gtt46461-star/starcore-public-build`。方案目录 `construction/plans/STARCORE_R30_FINAL_20261031/`（下称 `$PLAN`）。

## 0. 开工前（一次）

```bash
PLAN=construction/plans/STARCORE_R30_FINAL_20261031
git fetch origin
git switch -c construction/r30-20261031 bb607cf5ab69f7286f859e43f87ada85a5bc456d
git checkout origin/main -- construction/plans/STARCORE_R30_FINAL_20261031     # 把方案带进施工分支（只读）
python3 $PLAN/tools/validate_plan.py                                            # 8/8 PASS 才继续
```
注意：**不要**从 `construction/r29-20261029`（9ff38ff）开工，也不要合并它的任何提交。

## 1. 每张卡怎么做

1. 打开 `$PLAN/docs/Bxx_*.md`，找到本卡（编号顺序即施工顺序，前一卡未 DONE 不得开始下一卡）。
2. 只改“独占文件”表里的文件。
3. ADD：从 `$PLAN/code/main/...` 原样复制到 `app/src/main/java/...` 同路径；测试从 `$PLAN/code/test/...` 复制到 `app/src/test/java/...`。
4. 有补丁的卡：`git am $PLAN/code/patches/000N-*.patch`（四个补丁已在 bb607cf 上按序验证可应用）。
5. 有“整段替换代码”的卡（B01-04）：按 `docs/B01` 第4.1节替换指定方法，其余方法一行不动。
6. ADD_SPEC：按卡里给的类名、方法签名和行为编写；签名不得改。
7. 编译 + 单测 → 提交（信息以卡号开头）→ `python3 tools/r30/ownership_check.py --card <卡号> --base <上一卡提交>` → 推送 → 等 r30.yml 全绿。
8. 更新 `construction/r30/STATUS.md`：DONE（提交、CI 链接）/ NOT_RUN（缺设备）/ BLOCKED（原因）。

## 2. 每批最后

- 按 `docs/01` 第2节改 versionCode，跑 `tools/r29/rebind_identity.py`；
- 两条签名通道各出一个 APK，核对证书指纹、包名、versionCode、源码提交；
- 在 CP06 数据与 CP16 数据上各覆盖安装一次，冷启动、二次启动无崩溃；
- 交付：提交链接、两个 APK 直链、CI 日志、STATUS.md、已验证/未验证清单。

## 3. 绝对禁止

- 删除类、DROP 表、清空模型/训练数据；
- 放宽任何时间、数据、统计门槛；把模拟器/HOST 结果写成真机结果；
- 持有数据库写锁时做网络、sleep、等待其他线程、模型计算；
- 追期、加倍、马丁、亏损后加注、为凑频率补出手；用 SHADOW 冒充 REAL；
- 改未登记的文件（先登记子卡）。

## 4. 遇到方案没写到的情况

停下，在 STATUS.md 记 BLOCKED，写清：哪张卡、哪个文件哪一行、现象、两个可选做法。不要自行发明新的跨文件机制——那正是 R29 卡死的原因。
