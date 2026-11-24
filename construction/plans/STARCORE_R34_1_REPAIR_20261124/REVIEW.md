# R34.1 第 2 版施工索引

本页保留第 2 版修订历史。继续施工请读 [当前第 3 版](FIELD3.md)。

本次使用安装版本 577311122 的检测原包，逐项核对固定源码 `5562f9b19aa09f9f7d9e69ca085db56c537967fc` 和上一版方案 `00d53219ce7047c4abb5b55320ae140247cb5f87`。下列 32 项为现有工单新增的实施与测量要求。

[构建、迁移与交付](specs/review/build.md) · [后台、冻结与结算](specs/review/runtime.md) · [创造、训练、进化与资格](specs/review/algorithm.md) · [图表、缓存与页面](specs/review/charts.md) · [存储、导出与恢复](specs/review/storage.md) · [原报告与九层诊断](specs/review/report.md)

| 批次 | 复核编号 | 要解决的问题 | 工单 / 工程用例 |
|---|---|---|---|
| [B01](batches/B01.md) | REV-B01-01 | 把三个方案根分别绑定到实际构建输入和产物 | R341-B01-01 / R341-B01-01-T01 |
| [B02](batches/B02.md) | REV-B02-01 | 在 B10 存储策略交付前独立签收写入期限 | R341-B02-01 / R341-B02-01-T02 |
| [B03](batches/B03.md) | REV-B03-01 | 在 B03 验证迁移边界和当前会话可见性 | R341-B03-02 / R341-B03-02-T02 |
| [B04](batches/B04.md) | REV-B04-01 | S0 双事务赛前证据与逐期失败归属 | R341-B04-04 / R341-B04-04-T02 |
| [B04](batches/B04.md) | REV-B04-02 | 满表后的来源与事务返回证据 | R341-B04-06 / R341-B04-06-T02 |
| [B05](batches/B05.md) | REV-B05-01 | 服务单次预约与持久开奖重派 | R341-B05-01 / R341-B05-01-T01 |
| [B05](batches/B05.md) | REV-B05-02 | 恢复真实所有者与投递失败重试 | R341-B05-02 / R341-B05-02-T02 |
| [B05](batches/B05.md) | REV-B05-03 | 每期影子和 1 秒/5 秒的实测分母 | R341-B05-03 / R341-B05-03-T01 |
| [B06](batches/B06.md) | REV-B06-01 | 旧父代遇到空 ABI 时可计数、可恢复 | R341-B06-01 / R341-B06-01-A03 |
| [B07](batches/B07.md) | REV-B07-01 | 在线回放不能再抽中本模型的开发验证样本 | R341-B07-01 / R341-B07-01-A03 |
| [B07](batches/B07.md) | REV-B07-02 | 十位置概率面与同目标1200个动作排序建立可复算关系 | R341-B07-03 / R341-B07-03-A01 |
| [B08](batches/B08.md) | REV-B08-01 | 进化卡在老代时可安全替换待用数据缓存 | R341-B08-01 / R341-B08-01-A01 |
| [B08](batches/B08.md) | REV-B08-02 | 时序矩阵按本次数据版本读取模型对照 | R341-B08-03 / R341-B08-03-A02 |
| [B08](batches/B08.md) | REV-B08-03 | 将 S4 的连续形态处方闭合至 S5 影子回读 | R341-B08-04 / R341-B08-04-A01 |
| [B08](batches/B08.md) | REV-B08-04 | 逐图以有值原件或正式链原件验收产出 | R341-B08-02 / R341-B08-02-A01 |
| [B08](batches/B08.md) | REV-B08-05 | 把视图产品消费定位到同源实际动作 | R341-B08-04 / R341-B08-04-A01 |
| [B08](batches/B08.md) | REV-B08-06 | RSEC-066 六个窗口、归因与边界子试验回到真实消费者 | R341-B08-05 / R341-B08-05-A01 |
| [B09](batches/B09.md) | REV-B09-01 | 冻结资格比较对象及适用区间 | R341-B09-01 / R341-B09-01-A01 |
| [B09](batches/B09.md) | REV-B09-02 | 同一期分批冻结、重试和超时后机会总数守恒 | R341-B09-02 / R341-B09-02-A01 |
| [B09](batches/B09.md) | REV-B09-03 | 区分数学门、实际发证与附加质量验收 | R341-B09-03 / R341-B09-03-A02 |
| [B09](batches/B09.md) | REV-B09-04 | 选择后校准与模型/策略独立取证 | R341-B09-03 / R341-B09-03-A01 |
| [B10](batches/B10.md) | REV-B10-01 | 满容量取证热表持续接纳新请求 | R341-B10-02 / A-B10-02c |
| [B10](batches/B10.md) | REV-B10-02 | 为大库一致快照独立预约实际峰值 | R341-B10-01 / A-B10-01a |
| [B10](batches/B10.md) | REV-B10-03 | 先证明交付件可恢复，再清理原始工作区 | R341-B10-02 / A-B10-02b |
| [B11](batches/B11.md) | REV-B11-01 | 修正图表缓存及报告索引的零值和归因 | R341-B11-01 / A-B11-01b |
| [B11](batches/B11.md) | REV-B11-02 | 统一探索影子、预测趋势与正式主链的图表角色 | R341-B11-02 / A-B11-02b |
| [B11](batches/B11.md) | REV-B11-03 | 47个图和16个表从真实页面逐ID回读对账 | R341-B11-04 / A-B11-04a |
| [B11](batches/B11.md) | REV-B11-04 | 将分页负例改为真实可达的数据库状态 | R341-B11-04 / A-B11-04b |
| [B11](batches/B11.md) | REV-B11-05 | 报告与赛前服务并行，并验手机端持久保存 | R341-B11-06 / A-B11-06b |
| [B11](batches/B11.md) | REV-B11-06 | RSEC-066/069/070/071 的三项编译占位与六项自由文本改为可重算来源 | R341-B11-03 / A-B11-03b |
| [B11](batches/B11.md) | REV-B11-07 | 96项执行、44指标、165案例逐键回读和分流 | R341-B11-05 / A-B11-05a |
| [B12](batches/B12.md) | REV-B12-01 | 从施工交付地址独立取回产物并绑定实际安装身份 | R341-B12-06 / A-B12-06b |

## 当批施工与签收

逐批打开工单的实际文件和方法，在对应正常入口重现故障，保留修复前后同一输入、代码提交与输出。跨批共享文件以各批实际提交为基准。工程、APK、设备、自然观察分别留存状态。第2版回执对每个复核编号核对真实测试执行及原始测量。

从仓库根目录运行下列主机验证。需要 Python 3.11 或更新版本。

```bash
p=construction/plans/STARCORE_R34_1_REPAIR_20261124
raw=construction/evidence/STARCORE_R34_1_20261124/raw/STARCORE-9325298a3e033715226cf1126404c5a5273a6928f88.zip
python3 -B "$p/tools/verify_plan.py" --plan "$p"
python3 -B "$p/tools/audit_report.py" --input "$raw" --compare "$p/evidence/BASELINE_FACTS.json"
python3 -B "$p/tools/verify_report_coverage.py" --plan "$p" --input "$raw" --coverage "$p/evidence/REPORT_COVERAGE.json"
python3 -B "$p/tools/test_report_coverage.py" --plan "$p" --input "$raw"
python3 -B "$p/tools/test_receipt_guards.py"
python3 -B "$p/tools/test_prepare_worktree.py"
```

文件卡共 271 份，其中 226 份对应安装底座现有文件。本次逐个比对了这 226 个基线 Git blob 标识。源码清单中 161 个文件另有内容 SHA-256，65 个以 Git 树元数据定位。方法索引中的 token 可用于找代码，实际业务效果仍要通过对应入口复现。

原包未含完整 SQLite、模型二进制和安装 APK。运行证据、自然资格与日后胜率分别由施工或后续观察取得。
