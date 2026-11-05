# R31 原始检测报告复核账本

输入是报告作业 `ba5bcb7f83bf3f6113ed6d8d4490c38d6015b45a5f877d6b645d4795374e338c` 的48个解码成员。源码身份为 `5237b265a27fe5ad27fd842901c445d5f8c27749`。本目录仅复核输入报告，不声称已经修复App或完成真机测试。

## 复跑

```bash
python3 summarize_evidence.py --input /path/to/report/expanded --output /path/to/output
```

只需Python3标准库，无网络、无需Android SDK。成功返回0，并断言713表、60任务、44指标、96检查、12章、13项R27任务均隔离、55张keep表当前全空、5000开奖范围与排列有效、48个解码成员哈希与manifest正文哈希一致。原报告升级或改变后计数断言故意失败，不可静默套用本次结论。

## 文件导航

| 文件 | 内容与边界 |
|---|---|
| summary.json | 当前报告精确计数与状态分布 |
| tables_713.csv / tables_schema.json | 713张表的行数、字段、状态分组；行数不等于能力达标 |
| table_columns.csv / table_states.csv | 字段级模式、分组状态及原JSON路径 |
| schema_objects_and_sql.json | catalog中表/索引/触发器及SQL片段，保留原行号；不执行SQL |
| tasks_60.csv / .json | 调度状态与typed liveness分别保留；二者不能合并为一个健康标志 |
| metrics_44.csv / .json | 44项指标定义、状态、值、分子/分母、缺失原因、原修复卡及证据路径 |
| checks_96.csv / .json | REPORT_DETAILS中96条原始检查，包括完整evidence文本 |
| chapters_12.csv / .json | 12章状态、采集动作和来源错误 |
| selected_rowsets_83.csv | 83组逐行元数据的范围声明，说明哪些ALL_ROWS、哪些LAST_5000 |
| draws_5000.csv / .json | 按原open_time升序的5000期完整开奖元数据（见下） |
| draws_validation.json | 连续性、重复、排列、原JSON匹配验证 |
| shadow_observations_73.csv | 73条SHADOW预测元数据；72已结算，1未结算 |
| shadow_validation.json | 重算72条SHADOW命中20次，校验记录时间边界；不是REAL或新未来验证 |
| settlement_tasks_5519.csv / settlement_summary.json | 5519条结算任务及状态/任务类别分布；识别旧心跳与新业务状态差异 |
| storage_facts.json / runtime_facts.json | 精确存储数值、采样范围和runtime原始观测 |
| archive_integrity.json / archive_members_and_parts.csv | 48个解码成员校验；P*.bin编码块缺失明确标注 |
| legacy_requirements_1832.csv | 保留1832条历史要求原文字段与状态，不能升级为已完成 |
| legacy_file_cards_299.csv | 保留299张历史文件卡身份、路径、状态和引用；完整原卡仍在原报告 |
| input_inventory.json | 全部输入文件字节数/SHA256及全部JSON顶层结构 |
| evidence_findings.md | 事实、参考方案纠偏、推断和缺失边界 |

CSV采用UTF-8 BOM便于中文Excel读取。JSON为UTF-8；NULL仍为NULL，CSV空值不能理解成0。嵌套对象在CSV中以JSON字符串保存。

## 5000开奖数据字段与PIT边界

保留原 `_id`、`issue`、`open_time`、`fetch_time`、`first_seen_at`、`source_line`、`n1`…`n10`、`raw_json`、`dirty`、`dirty_reason`、`draw_hash_version`、`draw_hash_v2`。这些原字段沿用字符串类型，避免CSV浮点损失。新增 `chronological_index_0`（0到4999，便于按时间前推）、`source_row_index`（原JSON数组下标）、`source`（原JSON Pointer）。不得把_id、数据库插入顺序或source_row_index作为时间顺序。

原JSON Pointer示例：`DIAGNOSTIC_SCOPE.json#/recentEvidence/0/rows/4355`。`open_time/fetch_time/first_seen_at`以原始Unix毫秒保存。`raw_json.openTime`是不带时区的服务端字符串，不据它重新解释时间。主运行使用原毫秒值；展示时明确用户所选时区。

校验范围：202610300771到202611030010，共5000条，4999个open_time间隔均为60000ms；无重复期号或开奖时刻；每期为1..10的排列，号码与原raw_json.openNumber一致，dirty全0。这是内部一致性核验，不能证明上游开奖源真实性或独立均匀性。

历史补录的`first_seen_at`、`fetch_time`晚于相应`open_time`。可以做只读过去行的时间顺序研究回放，但不能把历史回放伪装为手机当时已冻结、真实出手或独立未来证据。5000期里反复选最高分候选会带来选择偏差；模型挑选、阈值调参、最终留出必须分开，已经看过的留出数据不再是全新确认数据。
