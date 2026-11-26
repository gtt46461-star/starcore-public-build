# B07 分类回执独立复核

## 发现并修复的确定问题

1. 真实 `TypedAtomMoleculeGraphR930.operation` 的 `SIGNED_PRODUCT` 为 `copySign(abs(a*b), a)`。例如 `(-2,3)` 得负值，`(3,-2)` 得正值，所以不能把两父节点排序为同一形状。已从 COMMUTATIVE 精确集合删除；ADD/MULTIPLY等数学交换结构仍保持保守归并。
2. 原分类器对最多4096个 registry atom 全部重新解析 canonical JSON；两个候选的 AST 各至多128个节点，无需引入无关原件作为附加失败依赖。现在先提取 parent/child 引用的 atomId，只解析这些 spec；扫描registry本身仍有原4096边界，不新建数据库读取owner。
3. 分类计算在 writer 外进行，之后增加一次既有 `check(deadlineNs)`，防止分类结束后越过原时间片直接提交。

本包以`patches/creation/after/`完整后态和PATCHSET前后SHA为权威；旧临时生成脚本不属于施工依赖。

## 没有改变的语义

- `variation_class` 只给 NO_OP、参数/来源迭代、未分类、未验证结构变化/交叉/融合；所有 receipt 一律 `novel_mechanism_status=NOT_EVALUATED`、`quality_status=NOT_EVALUATED`。
- graph shape 不是 ABI、不是数学创新证明、不是质量统计。窗口/lag/标签/epoch/teacher来源变化不能单凭hash不同记为新机制。
- 真实child创建仍由原 `ResearchDirectorR952.commitPreparedCandidateCP5` 执行；新 KnowledgeLedger receipt 在同一个 `CP12_EVOLUTION_CHILD_TRAIN_COMMIT` 事务内、child成功后、原action任务COMPLETE前写入。Director 返回 `NO_OP:` 时不添加child分类回执。
- 分类没有改 generation 数量权威、候选状态、模型、统计门、REAL权重或主链选择。事务发生写入错误仍整体回滚，不能单独保留child成功或task完成。

## 已执行

`tools/run_host_checks.py`调用随包`host_validation/creation_patch/tests/test_creation_classifier.py`：40个断言通过。实际编译生产 classifier、StrictCanonicalJson、AtomSpecCP4、SHA；SIGNED_PRODUCT表达式从真实生产内核提取，AST/平台只是测试元数据适配。覆盖交换/非交换顺序、窗口仅参数迭代、NO_OP、未知/缺spec、融合/交叉未验证、来源hash不授新颖性，以及4092个无关坏JSON完全不参与分类。另静态检查分类在原事务外计算且deadline核对在writer之前，receipt仍在原child与task提交之间。

`git apply --check` 对173基线通过，两个Java完成语法解析。未执行完整 Android SDK 编译、真实设备 child事务强杀/资源门、策略质量或创新度评价；不得将40个host断言写为软件验收或新颖性证明。
