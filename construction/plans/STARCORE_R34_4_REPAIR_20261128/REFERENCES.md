# 技术依据

公开源码底座：[211提交](https://github.com/gtt46461-star/starcore-public-build/commit/5c7f1b36eaee6036df280e85835bb0fb4ce0b40d)。原诊断证据仅在用户完整包。

## 技术约束的一手文档

1. [SQLite优化器与CROSS JOIN顺序](https://www.sqlite.org/optoverview.html#manual_control_of_query_plans_using_cross_join)：CROSS JOIN可固定相对循环顺序；这只支撑候选的设计，不证明现场查询已经变快。
2. [SQLite WAL](https://www.sqlite.org/wal.html)：读者与checkpoint、writer的关系；本轮不以一条PRAGMA或VACUUM替代原件保护与完整存储分析。
3. [Android PeriodicWorkRequest](https://developer.android.com/reference/androidx/work/PeriodicWorkRequest)：周期任务有最小间隔，不适合作一秒业务循环。
4. [Android前台服务后台启动限制](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start)：约束须按实际系统与targetSDK适用；本APK target28，不能把所有新target规则写成本次根因。

现场数据的原因判断优先由原报告与精确源码支持；仅推测的性能或调度风险均在问题卡和验收中标出，不把外部常识当已实测结论。
