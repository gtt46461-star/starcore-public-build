# R3.6.3 数据库与 Service 启动闭环

## 现场根因链

`foreground_notification_at > 0` 与 `on_start_command_at > 0` → Service 已进入；
`last_start_at=0` 与 `actual_process_start_count=0` → 旧计数点位于重初始化之后；
UI `DbStore=null` → UI 与 Service 同时等待同一个超重 Schema 门；
`recovery=OK:start_evaluated` → Receiver 覆盖了实际 init 结果；
所有业务表读取为 `STORE_NULL` → 应先关闭启动/读路径，不能显示 0/PASS。

## R3.6.3 施工

| 问题 | 代码关闭方式 | 验证 |
|---|---|---|
| DB35→DB36 重跑历史全量迁移 | `DatabaseUpgradePlanR363` 精确选择 additive-only 路径 | T38 |
| 构造期全量历史 Schema | `DatabaseCoordinator.ensureCriticalSchemaOrThrow` 只保证 R35/R36/V51/B03 | Java 17 compile + DEX presence |
| 历史 Schema 并发/静默失败 | 单独 process-single-flight，失败返回 false 并带因果链 | 静态门 |
| 每列 ALTER 后全缓存失效 | 成功后只把该列加入该表缓存 | 静态门 |
| BuildIdentity 暗触发全 Link Schema | `ensureModuleRegistryR363` 只建登记表及 5 列 | 静态门 |
| 前台进入误报从未启动 | `ServiceStartupStateR363` 分离 STARTING/FAILED/STALLED/READY | T37 |
| 重试叠加线程 Owner | 失败后取消回调、退出并清空半初始化线程 | 全源码编译 |
| Receiver 覆盖失败证据 | evaluation 使用独立字段，actual result 保留到 READY/FAILED | 报告字段门 |
| UI 过早生成 STORE_NULL 报告 | 后台解析真实 Store 后再生成；失败输出精确 startup truth | 全源码编译 |

## 不变项

数据库名称和版本保持不变；迁移不 DROP、不清表、不重写历史账本。固定 K3、单位置、每期最多
一个 REAL、DecisionGateEngine 唯一授权、SettlementEngine 唯一结算、SHADOW/REAL 分账、
模型未证明与 REAL 硬锁全部保持。

## 设备验收

安装后报告应依次观察到 `FOREGROUND_ENTERED`、`DB_CRITICAL_OPEN`、`DB_CRITICAL_READY`、
`ENGINE_GRAPH`、`COMMAND_REPLAY_PENDING`、`BUSINESS_READY`。若失败，报告必须给出失败 stage 与
cause；若 45 秒未 READY，应为 `SERVICE_INIT_STALLED[stage]`，不能为 `SERVICE_NEVER_STARTED`。
