R29.1 实施中，尚未达到 ENGINEERING_READY，未签收任何完整批次。

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
