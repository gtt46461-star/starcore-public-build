# STARCORE 天枢 R914 交付入口

版本：`V57.20.0-R5.3.0-914`（`577240914`）  
包名：`com.ethk3.novaonepro`  
游戏：`Binance1-PK10`  
数据库：`binance_k3_nova_one_pro_v57.db`，`DB_VERSION=36`

## 先看结论

- R912 检测报告暴露的结算饥饿、研究只偶发学习、UI 真值退化和滚动重绑问题，已按唯一主链重构并加入失败即阻断交付的 R914 行为门。
- 512 个 Java 已全量 Java17 编译，生成 2161 个 class；不是只编译新增文件。
- 8 个未引用的常量/重复 facade 已删除，不再用空壳凑文件数。
- 正式 APK 已用早期 `STARCORE-V57-release-signing-kit.zip` 的原证书签名；与 R912 证书 SHA-256 一致，可执行覆盖安装。
- `MODEL_PROVEN=false`、`SHADOW_ONLY=true`、`REAL_BET_HARD_DISABLED=true` 保持不变。R914 不伪造 70%–80% 胜率，也不以历史最好窗口解锁 REAL。
- 物理设备上的息屏 120 期、杀进程恢复、24 小时 soak 和实际滚动帧率仍必须复测；主机测试不能替代这些事实。

## 建议阅读顺序

1. `reports/r914/STARCORE_R914完整施工交付报告.html`：离线浏览器可直接打开的完整中文成册版。
2. `reports/r914/R914_MASTER_DELIVERY_REPORT_ZH.md`：问题、根因、代码施工、结果与限制的 Markdown 权威原稿。
3. `reports/r914/R914_ARCHITECTURE_BLUEPRINT_ZH.md`：整个项目、唯一主链、结算与算法进化 Mermaid 图纸。
4. `reports/r914/R914_REQUIREMENT_TRACEABILITY.csv`：24 条用户要求到代码、证据和状态的逐条追踪。
5. `reports/r914/delivery/R914_CHANGED_FILES_EXACT.csv`：每个变化文件的问题、修改、行/字符与 SHA-256。
6. `reports/r914/final/R914_BASELINE_TO_FINAL_FILE_AUDIT.csv`：520 个基线 Java 到 512 个最终 Java 的逐文件语义核查。
7. `reports/r914/delivery/R914_WHOLE_TREE_FILE_AUDIT.csv`：基线与最终 1,322 个 Git 路径的全树清单。
8. `reports/r914/evidence/behavior/R914_BEHAVIOR_GATE_SUMMARY.txt`：可执行门禁摘要。
9. `reports/r914/evidence/release/R914_RELEASE_BUILD_SUMMARY.txt`：APK 身份与签名连续性。

## 安装

正式 APK：`Starcore-Tianshu-V57.20.0-R5.3.0-914-release.apk`

```bash
adb install -r Starcore-Tianshu-V57.20.0-R5.3.0-914-release.apk
```

不要先卸载；卸载会删除应用数据库。覆盖安装成功只证明签名/包名/版本兼容，不等于真机后台验收已经完成。
