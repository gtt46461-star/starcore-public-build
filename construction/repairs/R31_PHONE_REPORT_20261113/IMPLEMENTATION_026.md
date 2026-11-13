# 026 报告专项施工

施工基线：`8eb3275af4c3221100f7aaaea32d26f729124f89`。应用源码仍在 `construction/r31-final-20261104`。版本 `577311026`，包名 `com.starcore.research.r13`；签名必须与 025 的 `d8d8cee7f406267b2bf18fc9aef0c6ab760423cf205ccd6613dd44085bcc74e7` 一致。

进度采样通过两个固定文件的独立控制配额写入。仍是原 2 MiB 总额、64 KiB 单条、2048 项扫描边界，计入每个 job 下的两个采样及其 `.bak/.new`；保留路径、磁盘底线和 OS 锁。普通输出和权威 job journal 继续由原空间准入管理。采样成功限频 1 秒，失败至少间隔 5 秒；不为采样错误再写采样。日志记录文件、阶段、任务、失败次数和经过凭据遮挡的原始异常链。采样失败不改变 stopReason。

ActiveControl 保留首个真实停止原因及 Throwable。SQLite 已返回的非 BUSY/LOCKED 错误优先于后续退款核账；核账错误优先于可选采样。进入 SNAPSHOT_HASH 先 checkpoint，不重置 stopReason。磁盘准入、实际写入、取消、超时、快照一致性和发布校验继续生效。

失败入口有界合并 FULL_REPORT job 和独立 ACK，以请求创建时间及请求 ID 确定顺序。job 清理后仍按 ACK 显示失败，排除 RETENTION_PAGE、SAF_TRANSFER 和主动取消。恢复原文需相对路径、请求/任务身份、包字节数、SHA-256 和原文成员 CRC/边界全部通过；缺失或损坏只降级原文，保留 ACK 原因。两个菜单入口复用同一读取和遮挡结果；恢复诊断保持独立分享入口。读取不清理证据、不提交导出请求、不重新备份数据库。生成恢复包之前先判断数据库正文是否允许纳入，再决定是否哈希数据库。

R30 对照为 `5237b265a27fe5ad27fd842901c445d5f8c27749`。025 的 ReportJsonWriterR31、ReportPublicationCP12、ReportSafCopyCP12、ReportSemanticVerifierR31 未变。继续保留流式 JSON/Markdown、64 KiB 批量写入、原始附件、12 章/44 指标/96 检查/47 图/20 矩阵/16 逻辑表/82 能力；不足和业务失败仍是报告内容，不能当作健康通过。

验证分层（下列为初次实现时记录；最终 20 项 HOST 断言、Android 14 实测和下载核验见 [EXECUTION_FINAL_026.json](EXECUTION_FINAL_026.json)）：

- HOST：实际生产 ActiveControl、OS 文件准入和原 JNI；合成库精确 165824 页、679215104 字节。18 项断言通过：两种采样写拒绝（含最后回调）、限频、后续内容和权威日志写入、真实已关闭 grant 退款错误、下一阶段保留原 Throwable、实际块准入拒绝、用户取消、双 ACK/仅 retention job 的身份查找、恢复损坏/缺失回退、凭据遮挡和原证据保留。Android 时间/Context/日志及 stat 的反射传输明确由 HOST 适配，stat 读取 Linux 实际 st_blocks。
- Android 14：待当前签名 release 从真实按钮、系统 Downloads 选择器生成、保存及回读；另从两个真实菜单入口验证合成手机目录布局。执行后另存源码/APK 身份与收据，不能用历史 025 结果代替。
- V2353A 用户原机：NOT_RUN；原库、24/72 小时和自然质量不作通过声明。

本轮不运行无关算法/多平台全量矩阵；当前编译、身份校验、报告相关测试、lint 和上述专项执行分别记录。初次 HOST 合成库页数和可信路径夹具错误只阻塞到该步骤，已修正后继续原生产路径；没有将失败记录算作通过。成品仅在同证书 release、实际 UI/SAF 验收及下载字节核验后登记。

最终交付：577311026，同 025 原证书。应用源码 `3002e30d0ee8d5fe3a8795c2ed3b2f7040920f50`；签名 APK 和安装 ZIP 均已重新从手机直链下载，SHA-256、ZIP CRC、包内外 APK 字节和签名一致。原机 V2353A、原库和 24/72 小时仍为 NOT_RUN。早期检查未就绪、界面自动化及发布配置的原失败记录保留，未改写成当前通过。
