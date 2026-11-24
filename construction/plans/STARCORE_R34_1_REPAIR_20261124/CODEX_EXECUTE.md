# 给施工 Codex 的完整执行指令

从实际 122 底座开工。按 B01–B12 顺序真实修改生产源码、运行所属用例、构建每批 APK 并记录提交。先从服务取到逐期业务推进，再从工厂取到第一个有公式和首次实训的出生，随后追到跨重启的连续几轮和合格时的唯一主链交易。每批对照 FILE_VERIFICATION 的本批条目，可由同一次生产执行导出多行独立证据。历史资产质量不足时继续施工研发与隔离工程正例。

本次执行 R34.1 第 6 版。先读[持续创造多种策略的施工和验收](FIELD6.md)，然后读[第 5 版施工变更](FIELD5.md)，再读第 4 版的[持续创造和真实采用的施工要求](FIELD4.md)、[逐文件施工与验收](FILE_WIRING.md)，再读第 3 版的 [本次施工变更和命令](FIELD3.md)、[升级方向逐条施工表](UPGRADE_ACTIONS.md) 和 [两代联调](specs/field3/chain.md)，再逐批执行含复核编号的工单。旧封印回执保留原版本身份；当前封印按各用例绑定方向重新审核，每个对象保留正、反观测原件。若此前尚未开工，从 B01 开始。

以下内容是可直接执行的施工任务。先读取全部入口和相关工单，再逐批修改真正应用源码。当前任务完成标准是实际工程交付；不能仅复述方案、创建空类、改状态或再写一份方案后停止。

## 1. 固定身份并建立工作树

- 仓库：`gtt46461-star/starcore-public-build`。
- 精确应用底座：`5562f9b19aa09f9f7d9e69ca085db56c537967fc`，分支 `construction/r34-20261123`，对应现场577311122。
- 当前施工方案：`construction/plans/STARCORE_R34_1_REPAIR_20261124`。
- 从仓库新入口获得承载方案的精确40位提交；记录为 `PLAN_COMMIT`，读取该提交的 `control/PLAN_IDENTITY.json` 和封印。不能把文件内的 `plan_repository_parent` 当成本方案提交。
- `main`是方案交付分支，不是122应用工作树。创建新的施工分支时从精确应用底座开始，只导入新方案、对应证据和两份入口，禁止整树合并main的旧应用目录。
- 已有R34.1施工工作树时，先核对真实提交、未提交用户修改和已签收回执，再从首个未完成批次续工；不reset、清库、覆盖前批、重做已验证工作。

从仓库获取上述精确提交。开工器核对选定 Git 对象中的方案成员和原报告。已有本地施工分支时，预检它的来源与本机改动；发生同名冲突时保留原件，读出冲突路径再合并。以下命令在实施者已设置的本地路径上运行。[开工及无损续工](specs/field5/build.md)。

```bash
p=construction/plans/STARCORE_R34_1_REPAIR_20261124
python3 -B "$PLAN_ROOT/tools/verify_plan.py" --plan "$PLAN_ROOT"
plan_manifest_sha=$(python3 -c 'import hashlib,pathlib,sys; print(hashlib.sha256(pathlib.Path(sys.argv[1]).read_bytes()).hexdigest())' "$PLAN_ROOT/MANIFEST_SHA256.json")
python3 -B "$PLAN_ROOT/tools/prepare_worktree.py" --repo "$REPO_ROOT" --plan-ref "$PLAN_COMMIT" --expected-manifest-sha256 "$plan_manifest_sha" --worktree "$R341_WORKTREE"
python3 -B "$PLAN_ROOT/tools/prepare_worktree.py" --repo "$REPO_ROOT" --plan-ref "$PLAN_COMMIT" --expected-manifest-sha256 "$plan_manifest_sha" --worktree "$R341_WORKTREE" --create
python3 -B "$R341_WORKTREE/$p/tools/verify_plan.py" --plan "$R341_WORKTREE/$p"
git -C "$R341_WORKTREE" diff --cached --name-status
```

首次资料暂存后核对并提交资料导入，已导入同一版本则核验当前工作树并从相应批次继续。方案校验工具在每个开工节点只使用选定提交的封印。

## 2. 首批先解决可构建前置

B01先按 [BUILD_PREREQUISITES.md](specs/BUILD_PREREQUISITES.md) 准备并核对原R33.5、R34、新R34.1三个实际方案根。`prepare_worktree.py`不会自动下载626MB旧原包，也不会自动设置旧环境变量；如果已有合法展开原件，可直接逐字节核验后复用。旧分片目录未齐，不能把它当作可恢复完整包。

B01实现R34.1身份桥、版本系列分派及当前122覆盖基线取证接口。保留 `STARCORE_R33_PLAN_ROOT` 对R33.5旧封印的原义和旧R34封印，新增 `STARCORE_R341_PLAN_ROOT`；旧变量不得指向新目录以绕过验证。

新增方案绑定应填写真实 `planCommit`、封印摘要和sourceBaseline，不可写占位SHA。版本号须高于577311122、实际安装版本和已发布索引；四个版本文件用既有journal原子变更。使用现有签名环境及公钥指纹；不在仓库、日志、回执中输出签名秘密、API密钥。

现有命令 `tools/r33/advance_release_version.py` 还不支持R34.1，必须先完成B01修改和相应测试，再调用新series。现有构建入口 `tools/r33/run_release_build.py --repo … --plan <原R33.5根> --out …` 的 `--plan`仍保留旧义，新root由明确新参数/环境传入。精确新增CLI、默认和错误码必须写入B01工程回执及 `--help`，不要调用未实现的参数假装已构建。

## 3. 按12批真正施工

按 `WORK_ORDERS.json` 和 `BATCH_INDEX.md` 执行B01到B12，遵守依赖顺序。每条工单同时读取：证据、诊断边界、文件/方法、实施步骤、验收、回滚、禁止事项、旧要求承接。

每批应完成：

1. 固定 `source_before`，确认前批接口和数据状态；读取对应FILE_CARDS共享编辑顺序。
2. 用首错/源码/真实场景验证具体因果分支。未知根因必须按决策树取得原件，不臆造现场异常。
3. 修改正式生产owner。新增业务类接入真实调用链、结构所有者、语义ABI清单、构建和报告；不能只被测试调用。离线质量实验与验证工具由明确CLI调用真实生产算法即可，不应接入设备实时调度。
4. 事务外准备，短事务内检查owner/fence/输入身份并提交。每个重试/恢复/消费有唯一键和收据，异常不得吞成成功。
5. 实现本批 `ACCEPTANCE_MATRIX.json` 指定的场景。复用现存足够测试并补缺少的行为；旧R34/R33.5原断言全部按承接表复验。测试使用独立可写目录和隔离数据，禁止fixture进入生产资格。
6. 运行本批真实工程场景和必要回归，输出原始日志及测量断言。保存当前源码提交、测试工具摘要、输入摘要、结果和原件。
7. 递增版本、提交本批源代码，在干净发布源构建签名APK。核验包名、签名、版本、嵌入源码与方案身份、APK摘要和产物下载；可用设备上验证覆盖安装。
8. 更新实际执行台账。填好回执后运行 `tools/verify_receipt.py`；其结构通过后仍需检查真实测试实现与原件是否支持业务结论。
9. 继续下一批，不因一项没有设备或自然样本而放弃其余已授权工程；明确保留受影响的DEVICE/NATURAL未完成项。

共享文件须顺序编辑，不得把旧方案整文件“后态”覆盖当前代码。新增影响文件先登记工单与回归，不得偷偷撤销前批约束。本方案附带的候选patch若存在，仅可作为经过指定范围核验的起点，仍必须联合适配前批和完成Android验收；没有参考patch的工单按方法合同实现。

Android 工程用例使用矩阵中修正后的运行器命令。先用 `python3 -B "$PLAN_ROOT/tools/dispatch5.py" --plan "$PLAN_ROOT" --case R341-B01-02-T01` 生成一次带独立沙箱的 argv，由当前施工工作树执行。每个用例保存运行器进入和测量完成的日志、设备信息及 JUnit 原件。B05 的进程死亡用例先由外部宿主独立观察同一工作键在正常服务重启后的唯一提交，再运行 JUnit 核验采集结果。[Android 启动与隔离](specs/field5/dispatch.md)。

每批工程回执通过后，在同一施工提交构建本批 APK，按[逐批构建和交付](specs/field5/apk.md)核验到当前批的前缀。B01 无需等待 B12。B12 对每批原件、原签名与最终下载字节复核。工程通过但装配、签名、下载待核对的项目单列在台账。

**第 6 版专项。** B06 起所有生产回执区分参数版、无父本新根、结构编辑、真正双亲融合、已执行代、独立质量改善。按[多路线工厂](specs/field6/foundry.md)先完成每批自己的工程用例；原子、分子和矩阵要接到实际生成器。按[多资产质量](specs/field6/quality.md)逐候选统计，合格时走唯一主链。B12 从正常服务导出三代与多根原件，按[输入格式和逐号码复算](specs/field6/witness.md)执行，构建并交付各批与最终 APK。仅换窗口或权重的批次不得以完成结构创造、变异或融合签收。

## 4. 不可削弱的功能边界

- 保留单位置K3、唯一真实决策/结算入口和原PIT时点。不能用K4、多位置REAL、追期或倍增填出号率。
- 候选出生、影子、训练和研究不依赖先有自然证书。真实入口无资格时NO_BET/HOLD，但基础业务仍持续。
- `freezeWithinTransaction`和`settleDraw`两处热DDL须与critical迁移联合处理；先有真实结构，再移除热路径DDL。
- 历史缺原件/坏父代/断档仅阻塞依赖它的对象，独立合法任务继续。禁止删唯一原件或把缺源当空集。
- 18图、20矩阵按对象合同分别生产；只读图保持只读，缺support不可生成全零完成占位。
- 训练和创造必须有真实数值状态、优化器/游标、执行产物与消费者；生成名字、累计计数、心跳不是完成。
- 原V1证书条件保持。质量投影与覆盖目标分别报告；不得把尚未部署的REAL频率作为首次合法部署前提。新增统计规则必须新版本新实验，不能回写旧资格。
- 不承诺必然产生高胜率资产；通过工程链不等于自然未来合格。负结果、资格不足和有效NO_BET都要保留原件。
- 同签名覆盖使用原授权签名环境。签名缺失时完成可做工程和候选构建，明确阻塞正式覆盖；禁止更换签名伪造成功或降低versionCode。

## 5. 回执、最终产物和完成定义

按 `control/RECEIPT_TEMPLATE.json` 填写真实数据，不得把模板直接改PASS。该模板的null、空数组和NOT_RUN均不满足验证器。

```bash
python3 "$PLAN_ROOT/tools/verify_receipt.py" --plan "$PLAN_ROOT" --receipt "$BATCH_RECEIPT" --evidence-root "$BATCH_EVIDENCE" --repo "$R341_WORKTREE" --level ENGINEERING
```

ENGINEERING、DEVICE、NATURAL使用独立回执。每批无设备测试或无自然样本应明确 `NOT_RUN/NOT_OBSERVED`；不得将它们合写为完整通过。验收工具从不运行回执自带命令，执行者应先真实运行受审阅的测试并提交原件。

最终交付需要：所有工单实际状态、全案继承回归结果、每批及最终APK身份、真实源提交和可恢复Git历史、签名公钥指纹、覆盖安装证据、设备24/72h完整机会记录、自然观察状态及资格原件、报告与原件清单。实际回下载文件再核摘要。只上传文字、空ZIP、无Git祖先源码包或改过标题的旧日志均不能签收。

环境阻塞时写清缺的是哪个SDK/签名/设备/原件、已完成哪些实质修改、阻塞哪些验收和下一条恢复命令。不得为了给出“全部完成”清零数据、缩减分母、删除门槛或伪造回执。
