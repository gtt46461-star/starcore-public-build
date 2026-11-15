# R27 回滚方案（R27-058）

适用版本：R27-2026.10.28（versionCode 577271030）→ 回滚到 R26-2026.10.28-reportfix2（versionCode 577271029，源码 1713d114ebbd07ebd6544a29acfbce1bd883215c）。

## 1. 为什么可以回滚且数据不丢

- R27 只做**增量迁移**：所有语句都是 `CREATE ... IF NOT EXISTS`，或带守卫的 `ALTER TABLE r26_task ADD COLUMN`。没有任何 `DROP` 或 `DELETE`，数据库 `user_version` 仍为 36（`SchemaR27.java`；HOST 测试 `tools/r27/test_r27_migration.py` 的 M1–M4 已验证）。
- 1713d11 版本不读取 `r27_*` 表，也不读取 `r26_task` 的新增列。SQLite 允许旧代码忽略多出来的列和表，所以回滚后旧版本按原样运行，R27 的表原样保留。
- 回滚后再次升级到 R27，R27 证据（影子、e 过程、谱系、代际记录）从保留的表中继续，不清零。

## 2. 回滚步骤

1. 取得与当前安装**同一签名**的 1713d11 APK（正式渠道：`fix/r26-report-export-20261028` 发布的 APK）。
2. versionCode 较低，必须用降级安装：`adb install -r -d <1713d11.apk>`（仅限可调试包或 ADB 降级）。普通用户界面不允许降级时，先导出完整报告和数据库备份，再卸载重装（**卸载会删除数据**，只有在已备份时才这样做）。
3. 启动应用，确认进程存活、无 FATAL；在“数据库健康”页确认表数量不少于升级前。
4. 需要时重新安装 R27 APK（`adb install -r`），R27 证据继续累积。

## 3. 已有的证据与缺口

| 项目 | 证据 | 状态 |
|---|---|---|
| 增量迁移、无数据丢失、可重复执行 | `evidence/r27/host/migration.log`（M1–M6） | HOST 通过 |
| 升级 → 回滚 → 再升级（同一签名） | CI 模拟器任务 `tools/r27/emulator_r27.sh`，回执 `evidence/r27/ci/emulator-api34/receipt.json` | 模拟器（EMULATOR），不计为真机验证 |
| 真机覆盖安装与回滚 | docs/12 设备矩阵 | **未执行**：本次施工环境没有真机 |
