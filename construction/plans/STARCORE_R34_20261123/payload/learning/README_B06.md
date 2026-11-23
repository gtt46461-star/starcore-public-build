# B06：针对实际 095 的冷启重生参考实现

基线：`273641e838e0b8dd842b42a67a674e184942a30a`，`construction/r335-field-095`。本目录为施工方案附带代码，未写入或发布应用源码。

## 文件

- `app/src/main/java/.../ColdRebirthContractR34.java`：纯合同、身份和 SQL。
- `app/src/main/java/.../ColdRebirthSchemaR34.java`：实际七个 DDL、准确对象验证。
- `app/src/main/java/.../ColdRebirthR34.java`：真实原 owner 冷启 handler。
- `patches/B06_cold_rebirth_integration.patch`：Lifecycle migration、RuntimeCoordinator 调度/健康、TaskContinuation、ModelReplay、ResearchTaskRegistry，以及 Foundry/WarmCandidate/Retention 三处生命周期保护。
- `integration_postimages/`：八个修改文件的完整 095 后像，用于审阅；**不得覆盖累积 B01–B05 的变更**。
- `tools/r34/`：宿主测试与执行器。
- `verification/`：本次实际执行记录，范围有限且明确。
- `B06_FILE_MANIFEST.json`：原/后像与新增源码字节哈希。

## 集成顺序

1. 在 095 派生的累计施工分支完成 writer、迁移和资源前置修复。
2. 加入三个新类；按补丁 anchor 合并八个原文件的集成点。原始 095 上 `git apply --check patches/B06_cold_rebirth_integration.patch` 已通过。累计分支发生 context 变化时应按 owner 合并，禁止整文件覆盖。
3. 七个 DDL 只由 `LifecycleSchemaR31` 现有 MIGRATION owner 逐对象安装。冷启任务仅调用 `require`，不做业务 DDL。核查 schema ready 后任务才能进入实际处理。
4. 将新增类和受改源文件准确纳入 R967 controlled dependency 角色；更新 R82 exact invalidation 的实际变更清单和审查理由，保持固定 baseline 原字节及 predecessors=[]。运行仓库现有身份/来源/调用索引生成器，再执行其 check；禁止手填新 hash 逃过验证。
5. 不把本目录复制进 app assets，测试报告不能作为自然质量证据。
6. 运行宿主测试、Android 完整编译，再执行真机原 owner 故障矩阵和新对象被既有训练器消费的验证。

## 运行宿主测试

```bash
python3 tools/r34/verify_cold_rebirth.py --source-root /absolute/path/to/actual095 --output verification
```

在方案仓库位置运行时，工具自行解析 payload 内 Java 路径，只需要指定包含原始 `RuntimeSchemaR26.java` 的 source-root。执行器编译实际 `ColdRebirthContractR34.java`，并解析它的实际 SQL 常量与新增 schema DDL，在 Python 自带 SQLite 上执行。另开子进程后 `os._exit(74)` 验证未提交事务恢复，未用文本模拟进程崩溃。

本环境 JDK 21 暴露 compiler，但缺 `ct.sym`；使用 `-source 17 -target 17` 编译，不等于 Java 17 标准库兼容证明，更不等于 Android 编译。完整 handler 的 Android 类型、调度器、DatabaseCoordinator 和真实数值模型生命周期必须由后续构建与设备测试验证。

本次结果：纯合同 33 个断言通过；SQLite/跨语言/进程恢复 44 项检查通过；原始 095 补丁校验通过。状态 `PASS_LIMITED_HOST_SCOPE`。实际 JDK compiler 另解析了全部 11 个交付 Java 源文件，语法通过；仅 parse，不做 Android/业务依赖类型检查。这些测试不产生任何自然 certificate/REAL，也不证明胜率。

## 必须补齐的设备验收

- 老 ABI 保留父→新 ABI child，fresh state 和样本为 0，原父与原证书原字节不改。
- 连续重试和进程退出后同一幂等键仅一个子对象；writer fence 过期不得提交。
- Foundry / ExpertWorkshop 真正消费 child，cursor/产物前进且重启保持；只 birth 不算训练成功。
- 数据 generation 变化、source 过期、错 source hash 必须拒绝提交。
- 内存不足、取消、磁盘满、artifact 准备后事务失败正确释放/等待；左模型已构造、右模型构造失败时不得泄漏。此项是 **B06 启用前必须完成的 Pair 生命周期修复**：B06-01/02 必须显式修改 `ExpertExperimentR26.Pair` 及相关 `LiveStateR26` 构造失败释放，并加入“左成功、右失败”的资源负例；当前参考代码未补改这两个原构造器，不得把正常 pair 的 finally 关闭解释为半构造路径已验证。
- 队列 16 个当前 ABI 且有效 source 的可训练 child 时停止新增；毒性父局部退避不饿死其他父；候选与 expert 公平轮转。
- 检查新任务实际被 P2 调度且能从 schema pending 恢复；report 对象必须联接到精确 child 主键。

本实现的出生类别为 ABI_MIGRATION_NOT_QUALITY_IMPROVEMENT。旧权重不转换、不继承；结构可保留，数值从零重新训练。其后自然资格仍走原独立 owner 和原合同。

## 已修复的生命周期集成断口

源清理原 SQL 会误删没有 candidate 的 EXPERT_CONTINUATION_SOURCE，即使仍被 RUNNING expert 引用。补丁保护真实 active expert；原 source TTL 仍执行。WarmCandidate action=3、Foundry候选清理和 Retention 无源 expert 清理都暂保无 successor 的零训练 cold child 元数据。handler 支持过期/缺源零训练 child 作为新 parent，一次一个新 source/identity/receipt；新后继提交后旧 child 状态标为 COLD_UNTRAINED_SUPERSEDED，原字节/哈希/指标/证书/birth receipt不变。已有数值样本的 child 走原 warm continuation。

同批登记 typed task（POLICY，training=false），birth与typed progress同事务；健康待处理数从主表读取，不能用心跳生成产物。测试覆盖 active source保护、raw到期而metadata保留、source已缺时保留zero child、单后继幂等、16个source全部到期后恢复admission容量。

`tools/r34/build_integration.py --source-root /absolute/path/to/actual095` 可以重建本目录准确后像/patch/manifest，输入必须是原095提交。它是审阅材料重建器，不是累计分支覆盖工具。
