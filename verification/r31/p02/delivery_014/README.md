# 014 实际签名检查版

本包包含提交 `961a6b8a74b9624a2b195e9562fbdfc9283b29fc` 的实际 APK，版本 `577311014 / R31-CHECKPOINT-P02-2026.11.04-014`，应用身份 `com.starcore.research.r13`。使用公有版本已有签名，证书 SHA256 为 `d8d8cee7f406267b2bf18fc9aef0c6ab760423cf205ccd6613dd44085bcc74e7`。

[下载包含 release/debug APK 和构建身份的 ZIP](https://github.com/gtt46461-star/starcore-public-build/actions/runs/41889513067/artifacts/13355297633)；[取得同一提交完整源码](https://github.com/gtt46461-star/starcore-public-build/archive/961a6b8a74b9624a2b195e9562fbdfc9283b29fc.zip)。GitHub Actions 下载可能需要登录，制品有保留期限。APK、ZIP 的大小和 SHA256 见 `DELIVERY-RECEIPT.json`。

这是已生成的检查版，完整 Android 验收失败，不能作为 R31 最终成品：

- 实际完整单测 481 项通过，零失败、零跳过；Java、Android 测试源码、两个变体 lint、身份校验及 24 组 HOST 检查通过。
- API29/35 模拟器各 2 项家族协议通过；完整故障矩阵各 14 格失败。故障注入尚未开始，前置模型包捕获即因 `DEPENDENCY(SCHEMA_PENDING):R929_PLATFORM` 回滚。两份原始 Android 日志及构建日志保留于本目录，失败没有改成通过。
- 后续 P01 的 SourceJournal、Atom、CSV、Evolution、Qualification、F23 修改尚未包含于这个提交，不能用本 APK 证明它们已集成。
- 无已连接真机，真机、24 小时初测、72 小时持续运行均为 `NOT_RUN`；自然未来质量为 `NOT_PROVEN`，未来样本数为 0。

本版维持数据库 `user_version=36`，按既有分域迁移执行，不清空数据库。P05 持久主链选择尚未正式切换，禁止清理 `r27_chart_cache/mainline/choice`。旧 APK 直接降级兼容未验证；如需回退，必须使用匹配版本、完整数据库和私有文件的已验证备份，不能只还原数据库文件。

当前施工门仍为 P01/B01+B02 联合验收，P03 尚未开工。完整范围完成率和具体未完成项见 `construction/r31/EXECUTION_LEDGER.json`；构建通过不自动关闭施工卡。
