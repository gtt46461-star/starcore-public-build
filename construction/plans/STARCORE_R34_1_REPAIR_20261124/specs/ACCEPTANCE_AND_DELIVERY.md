# R34.1｜工程、真机、自然资格与交付契约

本规范是后续Codex实际施工的签收规则。本轮产物为施工方案；尚未修改应用、生成新APK或执行新设备测试。必须完成用户已授权的全部工程，产出可核验APK和源码；不能把方案、脚本存在或编译成功单独称为软件修好。

## 1. 三类验收独立，状态不能互相替代

每条`acceptance`必须有`level`，值仅为`ENGINEERING`、`DEVICE`或`NATURAL`。所有原始日志附sourceCommit、plan hash、scenarioId、输入hash、工具版本、运行时间、退出码及观察范围。测试夹具/回放/模拟器使用明确SYNTHETIC标记，不能回流自然成绩。

| 验收层 | 可以证明 | 通过条件 | 不具备条件时 |
|---|---|---|---|
| ENGINEERING | 接口、状态机、迁移、数值变更、重试、并发、端到端受控链及真实产物身份 | 对应批次全部必需场景实际执行并有有效回执 | 继续解决可修复失败；环境缺项标BLOCKED_ENV，不虚构PASS |
| DEVICE | 用户实际安装可覆盖、真实设备前后台持续推进、24/72h | 原签名新包真实安装、真实墙钟完整观察及全部机会账本 | FIELD_PENDING/NOT_RUN，列明确缺件与可执行采集命令 |
| NATURAL | 未参与优化的真实未来样本支持优势与稳定性 | B09冻结协议全部条件实际满足，证据可回放 | NATURAL_PENDING、REJECTED或REVOKED，不能自动升级 |

工程批次本地签收不依赖未来批替它证明自己，也不依赖250未来ACT或72h真实墙钟。B01至B11的工程验收必须立即完成。B12再集成，真实设备/自然结果通过后追加各自签收；有FIELD_PENDING时不得称“全软件全部验收完成”。无合格资产不等同工程没有实现；有一个合格资产也不等于后台/存储/报告全部健康。

## 2. 代码基线与计划封印

应用施工从`construction/r34-20261123`准确commit`5562f9b19aa09f9f7d9e69ca085db56c537967fc`创建隔离工作树。计划文档所在main可能保留更早应用源代码；只按方案导入清单导入新plan、evidence与两个根入口文件，不能把main应用源码合并覆盖现场基线。

新descriptor是`control/PLAN_IDENTITY.json`，其字段为：

```json
{
  "schema": "STARCORE_R341_PLAN_DESCRIPTOR_1",
  "planRevision": "R34.1",
  "application_baseline_commit": "5562f9b19aa09f9f7d9e69ca085db56c537967fc",
  "application_baseline_branch": "construction/r34-20261123",
  "repository": "gtt46461-star/starcore-public-build",
  "prior_plan_revision": "R34",
  "prior_plan_commit": "4e1606e9c20c5f01a82cf495b8d39b2630c766f0"
}
```

`MANIFEST_SHA256.json`按根计划实际schema列出受控成员的hash与bytes。它不包含自身、`control/PLAN_SEAL.json`、`SELF_CHECK`及外层ZIP；这些是明确的固定排除项，不能扩展成任意忽略文件。`control/PLAN_SEAL.json`单独绑定manifest hash。后续构建回执在源树外生成，不能回写已经封存的计划。实际plan commit必须等方案提交后读取并固定，不预填猜测值。

## 3. B01先打通构建桥，B12独立复验

现有`app/build.gradle`在配置期仍要求`STARCORE_R33_PLAN_ROOT`及R33.5 descriptor。`tools/r33/verify_plan_binding.py::identity`看到R34 binding会调用`tools/r34/build_identity.py::identity`；该函数内部同时验证旧R34封印与仅`R34-B00..B12`的版本名。R34.1不能直接用旧正则，也不能删旧gate。

B01在第一批APK之前完成下列工程；B12只独立复验、封包与现场覆盖，不重新占有这些方法修改：

| 既有落点 | B01必须完成 |
|---|---|
| `app/build.gradle`的`generateR334PlanIdentity`、`verifyFinalR334PlanIdentity` | 显式生成/复验R341资产，所有assemble/package finalizer覆盖；保留原R31/R33/R34历史门禁 |
| `tools/r33/verify_plan_binding.py::identity` | 当前revision明确分派；R33旧descriptor原封保留；读取独立R341 binding |
| `tools/r34/build_identity.py::identity` | 从现有函数内部抽取历史seal验证为B01新函数，旧R34构建语义不变；R341不冒充当前R34版本 |
| 新`tools/r341/build_identity.py`及`release/r341_plan_binding.json` | 读取`STARCORE_R341_PLAN_ROOT`、新descriptor/plan seal，校验5562祖先与clean实际工作字节 |
| `tools/r33/advance_release_version.py::prepare/main` | 显式接受R34.1系列与B01..B12；保持四文件原子可恢复更新 |
| `tools/r33/run_release_build.py::build_environment/main` | 明确传递并核对R33、R34、R341各自封印root，保留构建前后身份相同校验 |
| `tools/r33/package_ci_candidate.py::validate_pair/embedded/main` | 原033历史对照继续可查，新增实际122/当前安装覆盖对照；检查r341嵌入身份，产物series不误贴R34 |

新binding只绑定已经提交的plan commit/manifest与固定应用baseline，不把其自身source commit写进同一提交制造自引用。真正sourceCommit/sourceTree从clean HEAD读取，APK嵌入构建输出；APK hash在构建完成后外置记录。

B12负例必须逐个改变旧R33文件、旧R34文件、新R341文件、descriptor字段、source工作字节、APK嵌入身份，确认全部拒绝；不能仅测试“正确路径通过”。封印只读复验失败回到B01修桥，不放宽regex或忽略字节检查。

## 4. 版本、签名和实际122覆盖

包名固定`com.starcore.research.r13`。原签名证书SHA256为`d8d8cee7f406267b2bf18fc9aef0c6ab760423cf205ccd6613dd44085bcc74e7`。这只是公钥证书指纹，不是私钥；凭据从原构建秘密环境读取，不能公布`R13_SIGNING_P12`内容、密码、DUOQU API令牌或秘密环境路径。

每批版本码为`max(577311122, installedVersion, highestPublishedVersion, currentManifestVersion)+1`。`installedVersion`和`highestPublishedVersion`必须实际读取，读不到则声明未知并避免冲突；不得只把123写死。版本名统一`R34.1-Bxx-YYYY-MM-DD-<versionCode>`，四份现有版本身份由原生成器/外置journal一次更新并提交。版本码不得超过Android允许上限。

覆盖验证必须读取设备实际包的版本、签名与APK hash：对选定设备执行`pm path`、提取当前base.apk并核验。若用户已安装更高版本，按实际安装版本作为当前对照，保留122历史身份，不能降回122方便测试。旧packager仅核033的通过记录不足以宣称122无损覆盖。

实际升级执行`adb install -r <verified.apk>`，不卸载、不clear data、不加`-d`降版本。安装前后核对保护原件、开奖事实、用户设置、迁移回执与新进程身份。签名不符或版本冲突直接拒绝，不能换key、改包名或清库绕过。无法取得原签名则release产物状态BUILD_BLOCKED；缺设备但能合法构建签名包时仍交付APK，安装验收保持FIELD_PENDING。

现有`-Pr312Development=true`明确禁止打包/签名APK；它不能替代release。复用现有Java17/SDK36/NDK/CMake/ABI配置，本轮不为了规避构建错误擅自升级工具链或降低原门禁。构建原日志保留但对秘密做安全处理；扫描命中只报告规则与相对文件名，不把秘密值回显。

## 5. 每批工程闭包

每批完成顺序：修改生产路径→本批最小充分场景→修复失败→提交clean源码→身份/签名构建→检验APK→登记批次台账→下一批。B01就具备构建能力，B12不是第一次尝试生成APK。可用前批接口/夹具验本批行为，禁止依赖未实现的后批来替自己签收。

批次台账至少含：

```text
batchId, baseCommit, sourceCommit, sourceTree, planCommit, planManifestSha256,
issueIds, changedFiles, productionEntryPoints, scenarioResults,
engineeringStatus, deviceStatus, naturalStatus,
apk {filename, sha256, bytes, package, versionCode, versionName, certificateSha256},
buildReceipt, rawLogs, fieldEvidence, remainingRisks, forwardFixArtifact
```

每个scenario结果含`level`、status、开始结束时间、环境、输入hash、退出码、真实输出hash、原始日志ref。状态不允许默认PASS；NOT_RUN必须有原因。静态源码映射、任务注册、DDL存在分别只是对应层证据，不能代替生产执行/数据产出/消费者消费。

最终受控集成链至少走过：冷创建唯一结构/公式身份→真实训练数值改变→模型发布→PIT输入冻结→计算或准确资格等待→开奖后结算→反馈→研究dataset/18图/20矩阵真实产出→行动决策→新任务/模型消费→下一轮回查。缺学生/缺历史/无证书时须验证等待/恢复路径，不能靠演示数据填满界面。受控链全部标SYNTHETIC，仅证明软件机制，不授予自然资格。

`verify_batch_ledger`检查全12批、全部实际工单ID、依赖顺序与证据文件hash；漏票、空日志、退出码失败、source不符、只有测试脚本未运行都阻止ENGINEERING_COMPLETE。现场或自然未完成独立列出，不能掩盖工程缺项，也不能使真实已验工程被误标“没做”。

## 6. 真机24h/72h campaign

24h/72h使用真实墙钟及单调时钟，不可加速。可在同一个72h campaign的24h截点形成中期回执，最终必须是同APK/source/policy身份下连续可追溯窗口。修复换包后开启新campaign，不拼接旧窗口。

campaign记录设备型号/API/OEM、电池策略、权限、APK、进程/boot、网络状态、真实进度及每次外部干预；设备序列号采用可区分hash，报告不公开个人标识。阶段包含前台正常、home后台、息屏/锁屏、受控Doze、断网重连、普通进程回收、系统重启及恢复；每个阶段起止都留原始操作证据。

采样不得定时打开Activity维持运行。只读adb/log/后台状态接口用于观测，不触发计算；报告采集本身可能影响系统负载时写清采样预算。后台是否继续依赖目标机会账本和训练/研究实际progress，不能只看service alive/心跳。

每个预登记目标机会必须归类为成功、资格等待、deadline_miss、未请求、未开始、网络输入未到、服务不可用、被取代、观测缺失等与B04一致的互斥终态/原因；不能把失败/维护暂停删掉再算成功率。成功P95/P99带成功n，另列全部机会分母。无合格资产时主路等待正确也要给出真实调度/评估证据，S0影子按独立角色计数，不能顶替主路合格出号。

force-stop是明确用户/系统强制停止，与普通后台进程回收不同。报告如实写`USER_STOPPED`并在用户允许重启后验证恢复，不能承诺越过操作系统强制停止限制。进程被正常回收而无force-stop时，B05的恢复应按真实系统条件验证。跨boot单调时钟不能直接相减，墙钟改变不能凭负时长给通过。

无设备或现场权限时，完成仪器/回放负例属于ENGINEERING；真实24/72h仍DEVICE/NOT_RUN。交付全部工程和签名APK，同时给出待执行命令及缺什么证据，不虚构设备截图或运行时长。

## 7. 自然资格复用B09协议

自然验收不以“必然产生高胜率资产”作为可实现工程承诺。软件必须能创造、训练、反馈研究和迭代，并对有无优势给出可信结论；真实结果可以是无优势或样本不足。

现有V1 `CertificateGateR27.evaluate`门槛保持：

| 条件 | 门槛/语义 |
|---|---|
| 有效未来ACT | ≥250，独立于训练/研究选择，不能用计划数/历史回放数替代 |
| 命中率 | HR≥0.34 |
| 校准误差 | ECE≤0.04 |
| 最差720块 | worst720Block≥0.26，窗口/分母按原协议 |
| 最大连败 | maxLosingStreak≤6 |
| 实现一致性 | ABI等价、冻结公式/模型/参数与运行身份一致 |
| 原序贯合格 | 原协议成立，不能事后换检验/重置失败序列 |
| 配对优势 | pairedLcbVsI00>0且pairedLcbVsNull>0，遵循原成本及对照语义 |
| 有效期 | 24h，过期/模型变更不能继续借旧资格 |

Q05校准support≥200另验，不能替代ACT250。coverage≥0.15当前仅在futureSummary，不是V1 evaluate新增硬门；预登记FUTURE_ACT_RATE与部署后REAL_ACT_RATE分别报告，部署后指标不能先卡住首次合法证书选择再形成无证可部署的循环。任何新增门槛只能按B09预注册协议版本引入，不能在B12临时放宽或加互锁。

候选更新/变异/融合产生新identity与新未来评估；父项自然证书/命中数不继承。冻结前锁定协议和停止规则，保留全部有效机会与真实成本，对照、多重比较、置信下界、漂移、连败等按B09原定义执行。拒绝历史晚到特征、补造预测、挑选胜利片段或反复重置样本。达到250也只是条件之一，不保证QUALIFIED。

自然交付台账含candidate/model/formulaHash、protocolHash、freezeAt、futureStart、实际有效样本、下一验收条件及`NATURAL_PENDING|QUALIFIED|REJECTED|REVOKED`。源码原生状态如`PENDING_NATURAL`可保留原值并映射规范状态，不能把命名不同当成已完成。

## 8. 最终交付件及无自引用链

必须有可下载的最终release APK、精确Git blob源码ZIP、standalone Git bundle、计划封印、全部批次台账、构建/签名/安装原始回执、24/72h状态与自然状态。源码ZIP的内容、mode和路径集合逐个对齐source commit；bundle在空仓库验证和恢复HEAD/tree，满足旧祖先门禁，不能依赖构建机预先拥有缺失对象。

身份链是单向的：固定plan commit/seal→clean source commit/tree→构建时嵌入身份→实际APK hash→外置交付manifest→发布后下载回执。外层ZIP hash最后生成且放包外；不能把同一包最终hash写进其内部后继续声称hash没变。

交付manifest列每个文件名、bytes、sha256、direct URL、scope及状态。上传到用户已授权仓库/release后，必须从最终URL重新下载，校验bytes/hash、APK签名/版本/嵌入身份、ZIP CRC/成员与源码bundle；网页存在或上传调用成功不能代替下载验证。失效链接、缓存旧字节、缺源码或缺台账须继续修复。

最终说明必须明确：实施了哪些工程、当前APK的source/plan/version、从实际122或更新安装覆盖结果、24/72h是否真正执行、自然资格现状、556历史缺失原件的隔离/恢复进展、仍保留的风险和向前修复方式。没有现场条件时写ENGINEERING_COMPLETE加FIELD_PENDING，不写全软件全部验收通过。

回滚原则为同包同签名更高版本的向前修复，保留兼容迁移及原件。发布修复包用新版本/新manifest，旧包标SUPERSEDED，不原地换文件却继续使用旧hash。不得为了回滚卸载、清库、删除旧证据或替换签名。
