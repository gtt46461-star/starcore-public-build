实际修复 FamilyStream 复杂模型 constructor lease 的已分配 primitive 重复计账。原 512MiB/default RuntimeProbe 下，F03/F04/F05 独占 primitive 共 6,391,680B；声明仍为 8,024,064B，等待新采样后 pending 降至 1,632,384B。对象 metadata、future margin 和共享 teacher 不记为已分配，实际 owner 退出语义、所有预算及模型数学保持。

最小 before exit1 / after exit0。首轮五整类 42 项中 41 通过、1 个受控 Probe fixture 失败保留；按 Root 授权将实际 F12 构造 primitive 计入该受控 heap，保留全部 43 个原断言调用，然后仅重跑两个受影响完整类，共 10/10 通过。另三整类无相关修改，未重复。完整485、Android、持续运行、自然质量及APK均不在本回执通过范围。

SUMMARY.json 记录全部命令、原始错误和结果入口；BEFORE/AFTER 对照是两个生产源 overlay ahead 精确05391 Android实际4494class，并非最终整个候选源码构建。根集成 owner 负责当前7源稳定候选一次完整回归。
