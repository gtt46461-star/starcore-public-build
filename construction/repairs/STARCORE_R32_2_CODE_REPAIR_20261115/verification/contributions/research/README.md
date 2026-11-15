# P2研究链路增补：训练期间继续消费已发布完整模型

本增补只实现两个生产文件的真实修复，不声称补齐P2其他34个写路径。前置状态为仓库7424eeae6354eb6599dd8498e123c059bd410c90，对其应用现有P1—P5；这两个文件的实质前态是P2应用后。`MANIFEST.json`记录逐文件前后SHA-256；`research_addendum.patch`是该前态上的增量。`after/`提供完整替换文件，施工使用增量优先，不能覆盖其他已合入修改。

## R32-P2-N01：共享十位置模型已发布，但训练/验证期间不预测

- `AtomNetRuntimeR30.forecast`原先对`!state.ready()`或`partialIssue`直接返回。虽然Runtime在部分位置更新时保留上一次完整模型，真实预测消费者仍不读它。每期十次位置更新中，只有完成边界能够预测；同时在线验证会撤掉引用。
- `RuntimeR27.publishAtomStateR31`现在在训练partial和验证阶段保留**同dataset**上一次完整、已提交的模型。不同输入运行不借用旧引用，冷启动缺少完整引用继续返回空；没有伪造发布。
- `AtomNetRuntimeR30.step`在训练切片前先尝试冻结当前下一期预测。`forecast`从`forecastModelR32`取精确当前publication绑定；号码概率、命中头、训练水位均使用该不可变模型。改变中的learner网络不参加该次预测。冻结期限、sourceRevision、head、PIT、唯一目标期与位置键保持原门。
- FTRL缺少独立旧版不可变引用，因此只有learner完整且权重/水位与当前发布完全相等时才产生其预测。不能把当前partial FTRL伪装为旧AtomNet权重的同源消费。
- 不修改训练目标、优化器、样本角色、64样本训练切片、十位置游标或任何资格/主链门槛。

## 自检与边界

`tests/JUNIT_ALL.log`为本轮主机实测，含新4项以及原十位置训练/消费者测试。新增测试验证partial时消费旧完整引用、预测字节不受新梯度改变；validation保留旧完整引用；不同dataset隔离；冷启动partial/validation仍阻断。生产两文件对SDK36和当前全应用class依赖编译通过。Android安装、真实SQLite冻结事务、手机未来消费覆盖仍需本批设备验证，不能把JUnit当作这些证据。

## 尚未解决的重要阻断

1. `AtomNetPublicationR31.Prepared.stagingBatches`对**全部历史**`r31_atomnet_publication`执行`count>=4096`容量门；现有代码中未找到该表的安全退役路径。逐位置训练和多阶段预训练会累计到此门。这个上限是当前长期容量阻断，不能描述为“终身创造无上限已经落实”。
2. 不能简单移除4096门或删除旧manifest：`MainlineModelBindingR31`、checkpoint CAS、物化样本、报告、证书/冻结引用依赖其原件。需要同批实现引用闭包保护、正文/原件分离、可回读归档及有界迁移许可，才可释放容量；未验证的归档不能计为已回收。
3. 本修复不凭复制文件或更改report标志证明新策略出生、矩阵被消费、父子收益或自然未来资格。现有P2.01/P2.03—P2.06的未落地条目继续留在真实缺口表，不能签“全部功能完成”。
