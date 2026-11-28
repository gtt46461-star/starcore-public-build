# 第二轮定向执行性审查：启动命令、批次交接与报告状态

## 结论和边界

本轮发现 **5项需要修正的执行合同缺口，另有1项B00身份边界建议写明**。三项行为已在隔离Git夹具实际复现：前置SHA校验失败仍继续解包并最终退出0；从稀疏计划工作树创建的干净baseline仍缺应用源码；HEAD及git status均符合预期时，被assume-unchanged隐藏的工作文件仍可能与Git对象不同。另确认B01完整专项验收包含B02才实现的fault排空正例，形成验收循环；B01“取消、低空间导出均可分享”的文字与211原状态机冲突。

审查对象为协调方给出的已发布commit `7947177eb26a5508e8c34c1159cb179ccbe15a89` 对应本地文件。审阅时 `MANIFEST.json` SHA为 `c0fe9f689b6ba8c492c7c36e69ba9b801cff70b222f6792d74f77d06b0d8499e`，`CODEX_EXECUTE.md` SHA为 `8d93093b8f39f7969f07a7da6c2fab6cbc2e91eb558d9dd7ad16078a9b6a222f`。下列行号和JSON路径针对这个快照，之后修订应按内容重新定位。

本地 `publication` 是分发暂存目录，没有 `.git`，因此没有谎称已直接在真正的7947177完整仓库clone上执行。命令复现使用复制实际计划文件的隔离干净Git仓库；稀疏行为另用极小Git夹具。临时目录已清理。准确211的报告源码通过只读GitHub文件接口取得。**未实跑Android、未编译/签名APK、未安装或操作用户设备；未改publication或应用。** 本轮只新增此文及机器可读审计JSON，不重复已完成的369链接、33出版检查，也不重复协调方正在处理的assert、输出覆盖及mandatory清单问题。

## SR01：启动脚本没有失败即停，前置失败被最后成功命令掩盖

### 准确证据

`CODEX_EXECUTE.md:29–38` 与 `42–50` 两段Bash均无 `set -e` 或逐步检查返回码。第一段顺序是准备原件、核MANIFEST摘要、运行资料验证、解包报告；最后一个成功解包命令会决定整段shell返回0。

实际反例：把当前完整计划复制到独立Git夹具，仅把 `MANIFEST.sha256` 改成64个0后提交，执行前 `git status` 干净。逐字提取第一段Bash，以没有额外错误处理的 `bash --noprofile --norc -x -c` 执行。观察到：

|观察|实际结果|
|---|---|
|摘要检查|`MANIFEST.json: FAILED`，sha256sum发出校验失败警告|
|后续资料验证|仍执行；当时验证器输出 `verification_pass=true`、source未运行|
|后续解包|仍执行，报告目录实际创建，56载荷验证输出出现|
|整段Bash退出码|**0**|

该反例只证明文档命令的错误传播缺口，不是声称真正发布包的摘要错误，也不是产品测试。

### 最小修正

将两段初始化代码合为**同一Bash执行单元**，首行 `set -euo pipefail`，或每一步失败明确退出。不要依赖两次独立工具调用之间隐式保留 `REPO_DIR/PLAN_DIR/EVIDENCE_DIR` 等shell变量；若必须跨进程，显式保存并验证初始化上下文再传入。

验收只需窄反例：摘要故意错误时非0退出，后面的verify/unpack均不执行；缺原件、缺source commit或源基线失败同理。保留第一个失败原件，不删除输入来“重新开始”。

## SR02：实际代码只是记录当前HEAD，没有证明预期计划pin

`CODEX_EXECUTE.md:21`要求固定准确计划commit，但`:32`执行 `PLAN_COMMIT=$(git rev-parse HEAD)` 后没有把它与发布入口给出的独立预期值比较。夹具的非发布commit也被直接当作PLAN_COMMIT。整个文件没有实际的expected-versus-observed门，也没有在执行这些计划内Python文件前验证其受控工作树字节属于该pin。

**最小修正：** 从发布入口或调用参数提供 `EXPECTED_PLAN_COMMIT`，核40位hex、HEAD一致和受控跟踪字节一致；不能用HEAD产生expected。补齐原诊断只允许固定路径/固定hash，不把任意工作树写入当作“原件物化”。把预期/实际commit、MANIFEST SHA和原件验证结果写入外部初始化上下文，再进入source基线及施工步骤。

这项与SR01应一起修，但原因不同：失败即停负责传播错误，独立pin负责识别错误输入。准确计划commit与211 source commit仍必须分开。

### 对协调方新§3的追加核查

协调方随后已经加入外部expected、HEAD相等和限定计划/package目录的git status门；这关闭了“拿当前HEAD自证expected”的文字缺口，但**git status仍不是字节证明**。第三个实际Git夹具将跟踪的计划工具标为assume-unchanged后编辑；HEAD仍等于expected，限定目录status为空，文件内容却不同于HEAD。这里没有修改真正的出版文件或其index。

执行任何计划Python前，应从预期commit的可信Git对象核MANIFEST以及启动脚本和依赖模块的实际字节，或者批量核plan/package所有跟踪blob。不能仅信工作树自带MANIFEST摘要和status。此检查只读，不需要reset、修改索引标记或丢弃用户文件；不匹配时保留现场并拒绝执行。追加反例只需证明assume-unchanged/skip-worktree隐藏的更改仍被字节门拒绝。

## SR03：干净的稀疏工作树不等于完整211基线

`CODEX_EXECUTE.md:47–49` 从方案checkout直接 `git worktree add --detach`，没有关闭新baseline的稀疏检出。计划专用checkout常只展开construction；完整应用可能仍在Git树中，却不在工作目录。

已在Git 2.56.1实际复现：隔离仓库含construction、app/src和tools，设置只检出construction并保持干净；按文档命令创建baseline。新baseline的 `git status` 仍干净，但 `sparse-checkout list` 为construction，Java文件不存在。执行 `git -C <baseline> sparse-checkout disable` 后Java文件出现，工作树仍干净。

**最小修正：** 创建 `BASELINE_DIR` 后显式执行 `git -C "$BASELINE_DIR" sparse-checkout disable`，再做1407Java/源锚hash核对。新施工树也应确认完整展开；后代保全策略不变。不能通过放宽基线文件集合或hash要求来接受缺文件。

这个复现只用小Git夹具确认真实worktree行为，未冒称已经检出了项目1407文件并通过其源核验。

## SR04：B01验收提前要求B02故障排空，必须拆清阶段断言

### 环的形成

- `CODEX_EXECUTE.md:103` 将完整10组报告专项设为B01、B02及以后相关修改都要执行的门。
- `B00_EXECUTION_ADDENDUM.md:155` 的fault满场景要求“排空/归档实际前进”；`contracts/EXPORT_AND_STORAGE.md:83`同义。
- `BATCHES.json#/batches/2/steps/2`、`batches/B02.md:42`把故障控制槽、共享热配额和mutable producer退役修复交给B02，B02退出还要求真实pending下降。
- B02的依赖包含B01。

于是，如果B01发布必须先通过“fault满时排空真实前进”，B01就需先实施B02，而B02又在等B01工程交接。这是**跨文档验收环**，不是DAG数组本身出现环。不能通过删除DAG边、伪装排空或让B01一并改完后批所有源码解决。

### 两层验收的完整文本

建议保留同一个“故障队列满”场景和最终强验收，只拆分阶段断言：

> **B01：报告降级与保全。** 在fault满/受保护配额阻塞夹具下，报告如实显示资源等待或来源不可用，用户可以取消，报告和遥测不递归放大故障，不删除活引用或唯一原件，也不伪造排空成功。已有合法READY仍按原引用合同可读。排空/归档真实前进属于B02子断言，在B01记 `PENDING_B02`，不算该子断言PASS，也不阻B01工程交接。
>
> **B02：真实排空与完整闭环。** 故障原件按既有owner封存、readback和登记，控制槽实际执行，pending在不丢原件、不清队列的条件下下降；再用B02同一签名release执行报告读回，闭合此前挂起子断言。不得把B01降级原件改名成B02排空证据。B02以后的相关修改同时保留这两层回归。

`PENDING_B02`只属于施工台账的验收状态，不得加入应用的catalog执行枚举。10个报告场景不增不减，fault场景拥有分阶段断言和各自source/APK/执行原件。

### 必须同步的位置

|文件/位置|应同步内容|
|---|---|
|`CODEX_EXECUTE.md:103`|10场景覆盖保留，说明B01验降级/保全，排空正例转交B02；不把后批实现反设为前批门。|
|`B00_EXECUTION_ADDENDUM.md` C07表格行、4.2标题/说明、fault满行|“完整专项”明确阶段含义，加入上面的两层文本；最终B02排空门不放松。|
|`contracts/EXPORT_AND_STORAGE.md` 第6节fault满行及表前说明|保留最终真实排空要求，并写明B01阶段允许真实等待及B02补验。|
|`BATCHES.json#/batches/1/exit`、`batches/B01.md`退出条件|加B01降级/保全签收与PENDING_B02交接边界；同时按SR05拆清取消/低空间语义。|
|`BATCHES.json#/batches/2/exit`、`batches/B02.md:45–49`|明确B02需关闭该挂起断言，使用B02的真实source/APK及新执行原件。|
|实际执行台账的`batches[*].signed_release_export_evidence`|在同一场景下记录B01、B02两次执行和交接引用。模板若需字段说明可增量补充，但不覆写旧结果，不将pending当PASS。|

原R344故障问题的B02正例、fault原件保护、关键事务准入及pending下降门均保留；无需削弱 `ACCEPTANCE_MATRIX` 中原问题验收或改变46卡计数。

## SR05：取消、资源等待与可分享READY被写成同一个要求

`BATCHES.json#/batches/1/exit/1` 与 `batches/B01.md:45`要求“正常、取消、超时、重试、低空间导出均保持可分享”。按字面执行，会要求生成中的用户取消作业也产生新可分享包，或磁盘完全不足时仍完成归档。

准确211源码与该文字冲突：

- `ReportExportServiceCP4.java:555–568`：用户取消进入CANCELLED，资源等待进入PAUSED。[1]
- `ReportJobJournalCP12.java:476–491`：CANCELLED恢复结果是TERMINAL，且不能转到其他stage。[2]
- `ReportPublicationCP12.java:27,140`：公开job和share grant要求READY_TO_SHARE；`:131–134`取消清理只对已有published.json保留已发布归档。[3]

### 最小替换文本

> 正常完成和局部检查失败/超时仍可交付诚实诊断。用户取消生成中的作业时，确认worker/读连接/流退出并持久化CANCELLED，不发布半成品，也不要求该取消作业生成新可分享ZIP；取消后重试走现有合法新请求/作业路径，不把终态CANCELLED强行改回INDEXING或READY。低空间进入真实资源等待/PAUSED或明确失败，条件恢复后再续跑。已有合法READY按原引用合同仍可读，不因后续ack/cleanup失败倒退。

需同步上述JSON出口和B01卡。专项合同的取消行本来写“取消态明确、旧worker停止、重试独立attempt”，应保留这一正确方向，并补一句不要复活终态取消job即可。不能把取消误归为普通依赖READ_ERROR，不能为交付包忽略用户取消。

这项是原状态机保全，不是要求新建复杂状态机或新增外部枚举。

## SC01：B00同APK身份门需明确不提前依赖B01的scope/clock修复

这里**不认定已有必现硬环**：`B00_EXECUTION_ADDENDUM.md:132`已明确B00不要求提前修完B01，原READ_ERROR/UNKNOWN仍可导出；报告合同`:94`也允许旧job无sidecar继续导出。

仍建议将此界限写到 `CODEX_EXECUTE.md:101` 的逐批身份门附近，防止runner实现时把全部业务观察字段都设成非空/完整门。原211诊断的 `r341BusinessScope` 为300字符截断JSON；`source-manifest.json#/mainWriterFaultsR342` 为 `UNKNOWN/SNAPSHOT_CLOCK_ANCHOR_UNAVAILABLE`、producerPid=null。这分别是B01的R344-006/008。B00更新构建/版本不会自行修复它们。

最小补充：

> B00必须核真实APK/source/包名/签名及已有可核归档身份一致；原业务观察scope截断、生产者未知、SQL pin锚缺失作为已知诊断缺陷如实导出，不造值、不签健康PASS，也不把尚待B01新增/修复的字段设为B00发布前置。B01按对应卡补齐后执行其独立正反验收；旧无锚job仍可按原协议导出。

不能把这段理解成放过APK/source不匹配：**交付身份一致性**与**报告内部业务观察的可证明程度**是两个具体门，前者B00就必须成立，后者允许其真实UNKNOWN并由对应批次修复。

## 已核对且不再扩大的范围

- B00未来runner前置问题已在当前CODEX`:66`及附约`:91`解决：先建立映射/驱动并完成本批，未来批次逐批补齐，不要求先做完B01—B11。
- 外部catalog执行枚举与内部进度已经写清：准确211 `ReportCatalogCP4.java:61–62`只接 `EXECUTED/READ_ERROR/CANCELLED`；合同把UNAVAILABLE映射到READ_ERROR/domain/UNKNOWN，PAUSED/NOT_STARTED留在job/progress。无需扩大或改名该外部枚举。[4]
- 准确211 `ReportReadContextCP4.java:122–153`是同步runner，每次新建CancellationSignal，finally清理。没有依据把本次报告错误说成旧signal复用；未来若异步化，现有合同要求确认worker退出后才复用/关闭连接是正确的，不能仅凭Future.cancel推定退出。[5]
- 每批签名release、debug/host分层、准确211/前批升级、实际下载读回及真机/自然质量边界已足够明确。没有理由新增泛化测试清单、降低签名门或要求没有外部能力时伪造产品PASS。

## 本轮验证记录

机器可读结果见 `audit/second_runtime_review.json`。其保存了审阅文件hash、精确JSON路径、三项实际夹具观察、修复门和未运行边界。三项探针之外，没有重跑出版全检查或尝试Android构建。

对协调方正在修改的新CODEX第3节已只读回看：单一Bash、`set -euo pipefail`、外部expected格式与HEAD校验、先核MANIFEST摘要、外部新输出、显式关闭baseline稀疏、baseline HEAD/status核对和施工树隔离的顺序正确。SR01/SR03已在文字上处理，仍待重新封板后做针对性反例；没有在旧manifest尚待更新时反复运行整个初始化。SR02的字节门补充如上，后续产品状态仍为未执行。

修订后只需针对这些具体风险复核：启动反例首错即停；错误pin拒绝；稀疏计划checkout可完整生成基线；B01/B02场景分层一致；取消/低空间出口与原job状态机一致。只有完成实际应用施工、目标Android和签名APK回归后，才能评价产品层面是否修好。

### 准确211源码来源

1. [ReportExportServiceCP4.java](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/app/src/main/java/com/ethk3/novaonepro/ReportExportServiceCP4.java)
2. [ReportJobJournalCP12.java](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/app/src/main/java/com/ethk3/novaonepro/ReportJobJournalCP12.java)
3. [ReportPublicationCP12.java](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/app/src/main/java/com/ethk3/novaonepro/ReportPublicationCP12.java)
4. [ReportCatalogCP4.java](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/app/src/main/java/com/ethk3/novaonepro/ReportCatalogCP4.java)
5. [ReportReadContextCP4.java](https://github.com/gtt46461-star/starcore-public-build/blob/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d/app/src/main/java/com/ethk3/novaonepro/ReportReadContextCP4.java)
