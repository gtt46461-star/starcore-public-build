checkpoint07（577291007）施工验证中，尚未发布或验收。

新增真实调用链：开奖入库同事务outbox、研究inbox与幂等ack、实际在线模型/结算完成回执；独立文件分片基线备份，按原SQLite类型编码、源变更watermark检测、目标独立重哈希和崩溃续跑。原数据库仍是唯一活跃权威，尚未追平或切换读写路由，不能称为完整分库。HOST新增18项事件及15项迁移检查；Android/正式APK以本CP07后续真实回执更新，不能预先写PASS。

checkpoint06 已发布并完成独立模拟器验证（2026-10-30）

构建源码 bb607cf5ab69f7286f859e43f87ada85a5bc456d；原R13/R26与既有R27两种固定签名各有release APK，须按旧安装证书选择。两种签名的debug/release四组API34模拟器均通过同签名保留数据覆盖升级、完整性检查、清装和两次启动。正式交付回执见delivery/CP06.json与DELIVERY_CHECKPOINT06.zh.txt。main文档入口已指向R29真实源码；QA/回执提交未改变该构建的产品源码。

本地canonical集成139项JUnit零失败/错误/跳过、8项语义反证；20项真实SQLite场景输出检查通过。lint无新增问题，保留51项既有基线错误、7个未匹配条目及Gradle9弃用警告。首轮失败证据保留。

完整批次0/11、整卡验收0/52、完整指标0/44；实现账本3 IMPLEMENTED、13 PARTIAL、36 NOT_STARTED。7项指标仅PARTIAL。真机/60min/6h/72h均NOT_RUN，自然质量INSUFFICIENT，未验证达标策略，REAL_BLOCKED。下文为保留的历史施工记录，历史“待CI”等状态由本段与对应CP06回执更新。

R29.1 实施中，尚未达到 ENGINEERING_READY，未签收任何完整批次。

checkpoint06（577291006）为本轮交付候选。在CP05之上增加旧版不完整AtomNet状态的不可变归档：SchemaR27先在同一迁移事务保留原artifact/hash/游标/学习率，再允许初次完整状态重训；重复迁移幂等，已有完整检查点时不继续复制，原副本禁止UPDATE/DELETE。Q16使用真实SQLite验证原字节保留和触发器。原R13与R27各自的固定签名分别构建相同源码，不能互相冒称可覆盖；实际安装与质量仍按各自回执。

checkpoint05（577291005）：在checkpoint04基础上修复周期评分epoch回执编号的恢复，完整检查点升级为有兼容读取的二版编码；旧CP04缺失编号从原数据库评分回执恢复下界。不会清空优化器/缓冲、不会覆盖旧评分行。原“调度源码字符串必须等于旧实现”回归检出1/135失败，原始失败日志/XML保全，改为保留P0接线检查并增加8线程实际争用测试验证1/2并发上限；已重跑针对性回归。完整最终套件、签名APK及模拟器结果见本次delivery回执，不预先写通过。

2026-10-30 checkpoint04（versionCode 577291004）：开工重新核对远端，使用最新 R29 源 df09ef5 延续，未使用 main 旧应用源码。新增真实 Android 修复已提交：研究批次前置检查/全部结果/末尾游标整体提交；冻结写完后再检查5秒边界；资格按登记日历阻断完全缺失的冻结期；旧跳期状态留审计后撤销并重建；校准非法概率拒绝且稳定性固定前三完整时间块；AtomNet/Adam/FTRL/RNG/回放缓冲与训练游标完整恢复，提交前不发布内存模型，滚动数据窗口不再清零学习状态；两worker起步、热压缩到一worker并滞回恢复，真正执行持久隔离状态。

本地实测：R29 JUnit 22项零失败；真实SQLite生产事务/SQL测试19项输出检查通过（Q1—Q15，Q2含5种乱序种子），包括1203写批次回滚重试、缺首期/中期/末期冻结、训练检查点与游标同事务回滚。完整旧JUnit首轮通过；最终全套、lint及官方签名CI仍以对应本轮原始回执为准。默认exec的Gradle网络接口限制及首次未提交输入拒绝保留在evidence/r29/checkpoint04，未当源码测试通过。

本轮另加原公有R13签名缓存所在分支的安全构建入口（tools/r29/r29-official-build.yml为源副本），只运行既有load_r13_signing/load_su1_bundled_api，不下载密钥到本环境。预期原证书9e6d5dc79d29d4ba5b435a171e6535386f8a56ea6fad71a6cc77aeb10ade3eca；无缓存则失败，不换签。API34模拟器用真实已发布同签名R26包覆盖升级并检查表/开奖行保全，再清装、两次启动；60min/6h/72h真机长测仍NOT_RUN。

范围限制：双库迁移/独立writer/outbox尚未完成，R29-028十位置网络和R29-029 I08 model_surface仍未实现；Gate V2仅诊断、未启用，不静默替换旧证书。当前仍IMPLEMENTATION_IN_PROGRESS，不以本检查点签收11批/52卡/44指标。全部剩余项及下一命令见continuation.json；逐卡调用链、状态和证据见cards.json及ledger/B01-RECOVERY-04.json。

应用分支 construction/r29-20261029，从 f9bdddb97291d9d96b5aba940b31d03369c220e2 延续；方案固定 96fb4355748a2f28e31927e1f9cb8cb61fac6547。原应用分支与全部原资产保留。CURRENT_FINAL_PLAN.md 及方案目录/ZIP保持方案原字节。

先完成 B00—B02 的指标、独立研究写者、迁移和冻结结算基础。

2026-10-30 B02/B07 子集（R29-010 结算部分、R29-035 连续资格部分，均为 PARTIAL）：已修复上列“迟到结果永久 hit=-2”“乱序资格漏计”“回执/状态/游标跨批次可见”三个缺陷。LiveShadowsR27.settle 只记录结果，可结算行与待补抓行分页（r29_pending_draw），旧 hit=-2 行按原冻结补结算；QualificationInboxR29 是 e-process 唯一写者，按连续顺序消费，ACT 缺开奖则停在该期，回执+状态+游标同一 CAS 事务，结果冲突隔离并阻断证书，无回执的旧状态记录后重建（证书先撤销）。真实 SQLite HOST 测试 tools/r29/run_qual_host.sh Q1—Q7 通过，两项变异对照均被检出；Android 设备回执 NOT_RUN。本卡其余部分（IssueCommitR29 实时短事务、确认队列16锁定）未做。

B00（R29-001—003）已实施并通过本地HOST检查：基线身份（设备实测APK与签名证书）、1832要求/299卡保全账本随APK打包并逐项核对（全部仍为NOT_VERIFIED，未复核不计完成）、M01/M02/M32探针与完整分母（策略×期行数与独立期号分列）。CI修复：身份绑定改为不动点（原流程每次都会因自身改写被钉住的文件而失败），编译后重冻调用索引。52卡及44指标逐项保存在本目录账本；未验证不记 PASS。已发现缺陷：迟到结果永久 hit=-2、乱序资格漏计、研究写批次拆分事务、冻结安全边界不足。

交付仅为可复核的施工检查点，不代表全部52卡完成、真机验证或自然命中率达标。安装/启动/覆盖升级和设备长测以对应回执为准；没有回执时为 NOT_RUN。未产生足够自然证据时 QUALITY_NOT_MET / REAL_BLOCKED。

签名：优先使用仓库既有授权 R13 签名传输缓存。缓存不可用时只允许复用最新 R27 已使用的固定 CI 签名，不生成新密钥；这种包不能覆盖正式 R13，明确 BLOCKED_SIGNING_KEY。禁止以换签或清数据解决升级。

两把既有密钥都在其他分支的 Actions 缓存里（正式R13→construction/su1-20261022，R27 CI→construction/r27-20261028），本分支读不到。可安装APK由 construction/r27-20261028 上的 r29-build.yml（源副本 tools/r29/r29-build.yml）以R27证书 d8d8cee7… 构建本分支提交：可直接覆盖R27.x、保留数据。

恢复命令：git clone --branch construction/r29-20261029 https://github.com/gtt46461-star/starcore-public-build.git && cd starcore-public-build

构建：见 .github/workflows/r29.yml。只有已提交源码才允许构建交付；APK与源码来源、签名、文件hash在 delivery.json 对应。

2026-10-30 CP07 / B01 实际源码集成（尚未验收）：ResearchRouteR29/ResearchCutoverR29 已实现独立库、独立 writer、类型/rowid/行 hash 复核、保留原表和封存基线、发布路由与重启校验；ExperimentRegistrationR29 将首次未来期交给实时 Owner；RealtimeResearchProjectionR29 投递带 epoch/revision 的证书/冻结/状态投影，旧投影不能覆盖撤销；旧任务通过 LegacyCapabilityAdapterR29 在路由发布且旧切片排空后移交 ResearchPool，原 VM/学习状态 Owner 保留。图表及报告读取已接入，报告合并两份私有副本并校验水位，取消会回滚副本事务。HOST 新增注册14项、类型复制/投影14项、调度/旧消息12项、报告9项通过；这不是 Android 冷启动/切库/真机通过。R29DatabaseCutoverDeviceTest 和不发布 APK 的集成 CI 已加入，待运行。旧能力完整清单/导出、剩余 B02—B10 仍未闭环，Gate V2 未启用，工程/质量/主链均不宣称完成。

保留真实失败：默认 exec 的 Gradle socket 初始化被平台阻止，批准流程重试已进入 Gradle；随后仓库 R997_UNCOMMITTED_BUILD_INPUTS_RELEASE_BLOCKED 要求先提交构建输入，尚未进入完整编译。两份日志见 evidence/r29/checkpoint07/failed。首次143项JUnit的1失败及其H15修复记录继续保留；不得把定向通过改写为完整套件通过。

2026-10-30 B01 真正进入 Android：源码140525f833c8f1b0c788c091eed44d26f62fec06 已通过保存的验证包装脚本，143 JUnit / 0失败 / 0跳过、8语义变异通过；lint新增0、既有51基线错误/7未匹配条目保留。7组真实SQLite检查通过。CI41045347342（API34）执行生产切库/连接/写者/配对报告代码，切库273ms、原97行保留、持有实时写者时独立研究提交完成、投递不冒充训练、报告读取合并数据；12.8秒单项测试通过。它只签收这些合成工程路径，尚不签收完整导出、真机长测、自然质量或B01整批。原始回执、日志、构建日志见evidence/r29/checkpoint07/android-140525f与validated-140525f。

继续施工：审计发现旧22路R912入口仍需收敛，现已增加旧服务/新池共享交接锁，旧调用排空后才发布owner，服务旧入口改为唤醒pool插件；原22路算法未删除。新增完整R26学习状态保留引用、不可变pin、GC引用保全、20ms续跑解码与恢复描述；保留原状态digest及CAS digest各自算法，恢复失败记录具体缺件，不新建替代状态。新12项HOST检查通过（原状态恢复/下一学习更新字节一致/坏文件阻断/无资格转移/研究库增量schema无实时权威表/未提交切片不记进度）。这些后续改动的Android完整24家族/16专家、1000次writer压力与重启回归将另行运行，尚未通过。最新可交付APK仍CP06。

2026-10-30 完整Android首次组合回归41048954854失败，原样保存在failed/android-0bbc9a7：切库/独立写者/报告及旧22路交接门单项再次通过（切库376ms）；24家族/16专家在16.1s已各有实际记录，但组合测试在这一时点错误地把异步迁移当成已完成，路由尚未发布，后续1000次压力/重启未执行。修复：测试等真实路由/owner回执（明确60s预算）；迁移优先完成正在COPYING/VERIFYING的表，避免80多表轮询后才有第一份verified基线；旧22路pool插件恢复原入口的R811 schema与SQL契约前置条件。真实SQLite M01—M16通过；原排序负对照仅M16失败，前15项仍通过。修复后的Android回归仍待运行，不能写组合通过。B02未完成源码暂存/workspace/work/r29/b02-saved，所有文件hash和patch保留，B01修复编译/启动CI后恢复继续。
