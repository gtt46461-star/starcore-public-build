# 全量覆盖口径和逐项处理入口

|集合|数量|完整文件|含义|
|---|---:|---|---|
|检测|96|CHECK_ACTIONS_96.json / check_inventory.json|保留原契约、源码方法、health/state及原件，逐项关联问题；EXECUTED不等于PASS|
|指标|44|METRIC_ACTIONS_44.json / metric_check_coverage.json|保留36工程+8质量指标及测量源/分母/不足原因，当前0 PASS|
|对象|165|object_coverage_165.json|82能力+20矩阵+18研究图+29旧图+16表；82能力包含20矩阵别名|
|图表/逻辑表|63|chart_source_mapping_63.json / .csv|47图+16表的真实producer/query/consumer/渲染验收；不能把8非空表当8个已确认UI|
|数据库表|801|table_counts_801.json|当前快照全表count/state，不用几个大表代表全部数据|
|原始施工需求|1832|../inheritance/CURRENT_REPORT_CROSSWALK_1832.json|逐条保留原账和未核验状态，不擅自宣告全部已实现或未实现|
|原始文件卡|299|../inheritance/CURRENT_REPORT_CROSSWALK_299.json|逐行原文和源路径对应，不与旧295卡混计|

96项health为43 OBSERVED、21 NOT_PROVEN、16 NOT_APPLICABLE、8 WARN、4 PASS、4 FAIL；细分state/domain_outcome另存原文，不能强行统一成PASS/FAIL。

44指标为35 INSUFFICIENT、5 RECORDED_UNVALIDATED、1 BLOCKED、1 UNKNOWN_SOURCE_MISSING、1 NOT_DEPLOYED、1 UNKNOWN_STALE_SYNTHETIC_SOURCE。状态缺证据继续保留；下一报告必须保持相同定义/角色/时间窗身份后再比较。

本清单提供完整枚举与处理去向，未将静态覆盖等同全部业务实测。严格逐图渲染、逐源生产和逐父需求验收的实际证据在后续批次取得。

修订2将生产责任与汇总验收分开：每个指标有独立implementation_owner_batch、真实source binding、正反例；每条检测保留完整原始结果。见[逐项责任复核](RESPONSIBILITY_REVIEW.md)。使用`tools/verify_responsibility.py`核对原报告全部metric字段和原check值，不能删sourceSnapshot或将缺证据改PASS。M16现有报告取消链需执行B11补充测量工单R343-B11-M16，缺测不是确认代码损坏。
