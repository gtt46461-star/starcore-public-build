# R75 代码修复与验收边界

输入报告：`10.13检测报告.zip`，SHA-256 `8494c6053291bb95d3afd1e92bc262594dfbdd861035c4509c912de7968e5789`。该报告来自 R74，96项检查执行、0查询错误，6 FAIL、8 WARN、20 NOT_PROVEN。

本目录记录真实产品代码修改、旧版本失败复现与主机回归。**主机结果不代表手机安装、现场时效、自然前瞻资格或全部1—9批次业务验收。APK交付条件尚未满足。**

本轮最终生产源提交 `452854cc8a82933d5547e1b81789beb22dbe4c49` 已通过正常 strict release 构建，原签名、版本、ABI、资产及生成身份收据在 `evidence/final_build_*`；51 项单元测试零失败/错误/跳过，lint 仅 2 个 Hint。锁定该构建字节码的下列主机回归均已通过。上一轮未交付 APK；用户随后在现场验收包提议后回复“继续”，本次提供同一构建的现场验收 APK，见 `FIELD_ACCEPTANCE.md` 和 `evidence/final_field_delivery_identity.json`。这不改变最终业务交付条件尚未满足的判定。

Android CI `38359778200` 已结束，源码身份与上述提交一致。GitHub 步骤回执确认：编译/native/D8/lint、测试包构建、实际 Android 滚动/回滚、独立运行验证及日志证据备份全部成功；仅第 13 步验收文件上传失败，artifact 列表为空，因此整个 workflow 仍为 **FAILURE**。完整步骤回执见 `evidence/final_android_ci_step_receipt.json`。日志接口两次返回 `Transport closed`，尚未取回原始测试收据和上传错误详情，不能猜测上传失败原因或编造 Android 单项数据。日志备份步骤已成功，可从该任务既有日志恢复 ZIP，并按 manifest 校验 SHA-256；目前该恢复尚未完成。上述 Android 模拟器步骤成功不代表用户手机、自然资格或完整九批验收通过。

已从完整 Git index 及固定原提交 `0dba7800a68f33dbe8617b71d1ce5f6318dfe16e` 核对后续 optimized299 的 O01–O09 交接与 O08/O09 交付原件，九批方案和应用记录确实存在。较早 docs 七批不能替代后续九批，也不能据部分 checkout 宣称第 8、9 批材料缺失。历史九批 APPLIED 与旧工程/手机/自然资格的未闭合状态分开记录；592 行整条关闭修正表也不代表本轮全量验收通过。身份和范围见 `construction_scope.md` 及 JSON 的 `construction_plan_provenance`。

## 本轮修复

| 问题 | 修改 | 验证范围 |
|---|---|---|
| 主线路旧一期仍被接受，遮蔽更快备用源 | 到实际下一期开奖截止后继续备用源；覆盖冷启动、秒/毫秒 | 真实 ApiClient 调用链，受控HTTP响应；不证明服务端固定5秒发布 |
| 清理占满研究父任务预算 | 持久研究/归档/研究/回收轮盘 | 真实Service回调、SQLite；注入工作耗时/故障 |
| 训练和导入反复获得不足预算，游标却前移 | 准入前保留未执行owner；执行后保持公平轮转 | 持久游标、过期lease、数值artifact和反馈、报告NEURAL训练 |
| 百万revision索引清理全扫、抢占归档 | 128条有界keyset扫描、主键删除、游标事务恢复 | 旧百万行失败复现保留；锁定452854cc最终release字节码20+42项PASS，无产品overlay；真实SQLite/native归档哈希、恢复、取消、幂等 |
| 已满捕获容量仍扫描候选；坏图阻断后续 | 满容量早返，确定性负分只退休对应预约 | 隔离输入驱动真实产品连续241期捕获/结算/拟合、跨JVM恢复、异常隔离；不是自然前瞻样本 |
| 真实目录中DISCOVERED原子被变异器全部排除 | 允许已校验定义用于研究提案，原子state/绑定准入不变 | 原始DISCOVERED状态下真实变异；无绑定OOS资格仍拒绝 |
| 校准失败无法生成针对性进化 | 独立校准诊断动作、真实变异、新子训练与冻结；DEV截止与诊断选择边界分离 | 119项最终字节码检查+2项旧基线复现；17项续跑验证真实Owner完成480训练/120验证、独立artifact/receipt及FIXED_GRAPH冻结；合成主机输入，不继承校准/Future/证书 |
| 现代矩阵定义未自动登记，报告spec/checkpoint均为0 | 持久交替 registration-first / consumption-first，每个发现 quantum 最多8项；继续由原source/projection/ACK Owner生产；lag相关COUNT改为一次独立索引阈值读取 | 最终release字节码551专项（393公平/57窗口/101源）+canonical76+67公共训练冻结Owner延续通过；18类拉取源有spec→checkpoint→consumer，Freeze/Failure走原事件链；手机及自然质量仍未验 |
| 冷bundle 5.57秒无法区分SQLite等待位置 | BEGIN/body/END有界独立观测 | 真实写/读锁、提交/回滚错误、矩阵日志兼容；不是该手机根因已确定 |

## 最终主机证据索引

各组使用的输入、适配器和负例基线不同，不将检查数相加冒充完整业务验收分母。

| 最终验证组 | 结果和准确范围 | 收据 |
|---|---|---|
| 抓取与研究回调 | 29抓取 + 45显式注入协作者工作 + 33完整产品Owner回调；后者空历史、Android适配，不证明训练吞吐或手机启动 | `evidence/final_fetch_summary.json` |
| 保留与归档 | 20有界revision + 42真实SQLite/native归档；无产品overlay | `evidence/final_retention_manifest.json` |
| 捕获、校准修复与变异调用 | 33主链捕获 + 119最终校准修复（另2旧基线）+13 Dirichlet；241隔离校准观测拟合但质量不合格 | `evidence/final_mainline_calrepair_dirichlet_ledger.json` |
| 训练与冻结 | 66预算、10 outbox、11普通family协议字节一致、17维修子代训练冻结；另真实报告AST/历史在隔离库完成NEURAL 1320行特征（1200 train/120 validation）和MLP冻结 | `evidence/final_training_manifest.json` |
| 矩阵 | 551专项 +76 canonical +67公共训练冻结Owner延续；最终产品类无overlay，前后身份一致 | `evidence/final_matrix_result.json`、`final_matrix_class_identity.json`、`final_matrix_owner_summary.json` |
| 事务观测 | 36真实SQLite争用、回滚、外部事务、异常保持及矩阵日志兼容检查 | `evidence/final_bundle_result.json` |
| 既有跨域与主链回归 | r71运行8组、r72领域9组、证书→DecisionGate→trade→settlement合成5组通过；合成REAL仅存在隔离测试库，不是用户手机真实出手 | `evidence/final_regression_manifest.json` |

## 不得据此宣称完成的项目

- 没有连接用户手机，未完成当前版本覆盖安装、连续自然开奖、后台/息屏/任务移除验收。
- 报告校准模型、有效证书和REAL应用均为0；隔离测试不填充用户数据库，也不能成为真实资格。
- 241期隔离测试得到不合格校准结果，证明门禁仍拦截未达标候选，不能当作胜率、稳定性或下一代必然改善证据。
- 校准修复119项最终检查（另2项旧基线）加17项续跑，证明合成主机输入下新FIXED_GRAPH子代已完成开发训练/验证和冻结；不证明其独立校准、正式Future、自然增益或实际出手，也不代表全部模型族已完成。
- 矩阵551专项、76 canonical及67 Owner延续已完成最终release字节码复跑；仍需手机持续运行和自然源证明。缺历史、赛前绑定、教师、共同Future或质量资格时继续等待，不能把隔离源fixture的18类拉取路径及原事件链通过冒充手机20类全闭环。
- 旧施工台账有未知项；本轮没有将未知项批量标为通过。见 `report_acceptance_r74_input.json` 和施工范围说明。
- 主机证据中有合成的证书、决策和交易结算，证明既有资格输入满足时调用链可执行；这些输入没有写入用户数据库，不能称为已获得自然资格、已在用户手机真实出手或保证胜率。
