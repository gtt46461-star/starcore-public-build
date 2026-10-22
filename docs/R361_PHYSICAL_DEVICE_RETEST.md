# R3.6.1 物理设备复测矩阵

## 测试前

1. 备份应用数据库和旧完整报告。
2. 覆盖安装 R3.6.1，不卸载旧包。
3. 确认 versionCode=577180813。
4. 打开通知、后台运行、电池优化豁免和精确闹钟权限。
5. 启动前台 Service，复制一份初始完整报告。

## P0 启动与数据库验证

| 编号 | 操作 | 必须结果 | 失败证据 |
|---|---|---|---|
| D01 | 冷启动应用 | 无 SQLiteOpenHelper `Object` 构造器异常 | logcat + 完整报告 |
| D02 | 启动前台 Service | actualStart>0，Service 心跳更新 | 完整报告第 6 节 |
| D03 | 等待一次抓取 | fetch 时间更新，主/备源状态明确 | 抓取日志 + 报告 |
| D04 | 等待一次分析 | maxConcurrency≤1，目标期精确下一期 | Runtime ledger + 报告 |
| D05 | 开奖后结算 | SettlementEngine 唯一结算，OOS 模型调用=0 | task/OOS 行导出 |
| D06 | 数据读取 | 不再全局 STORE_NULL；真实无样本显示 NO_DATA | 完整报告所有硬门 |

## R3.6 设备矩阵

| 原编号 | 场景 | 时长/次数 | 通过条件 |
|---|---|---:|---|
| T29 | 亮屏前台/后台运行 | 120 分钟 | 无崩溃、无并发>1、目标期连续 |
| T30 | 熄屏运行 | 120 分钟 | 抓取/分析/结算继续或被系统恢复，证据连续 |
| T31 | 从最近任务划掉 | 30 分钟 | cooperative recovery 生效，仅一个 Owner |
| T32 | 进程杀死矩阵 | 至少 5 次 | Alarm/Job/Receiver 计数分离，重启后任务补偿一次 |
| T33 | 主源/备源/断网恢复 | 各 3 次 | 不伪造开奖，不重复分析，不跨期污染 |
| T34 | 综合浸泡 | 24 小时 | OOM=0、并发≤1、无重复 BET/结算、Coverage 可枚举 |

## 运行硬门

- K1 Coverage：读取必须 OK；预热期显示 RETEST_REQUIRED，不能把 -1 或缺失当 PASS。
- K2 maxConcurrency≤1。
- K3 request-to-commit P95≤5000ms。
- K4 OOM=0。
- K5 canonical 首写冻结且每目标期唯一。
- K6 严格 OOS 只读冻结 canonical + 实际开奖，模型调用=0。
- K9 当前 SHADOW 模式 BET 观察行必须为 0。
- K10 模型证明未完成时 REAL 永不解锁。

## 回传材料

复测后保存：完整报告全文、报告 SHA-256、APK SHA-256、设备型号/Android 版本、每个测试起止时间、logcat、数据库只读导出。没有这些材料时，`PHYSICAL_DEVICE_RUNTIME` 必须保持 `RETEST_REQUIRED`。
