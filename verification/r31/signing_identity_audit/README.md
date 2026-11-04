# 5237 公有基线签名/安装身份审计（只读）

结论：指定提交 `5237b265a27fe5ad27fd842901c445d5f8c27749` 的实际已发布 R30 APK 使用 **EXISTING_R27/d8d8** 证书。因此本次R31复用该既有cache延续了指定公有基线的签名身份。用户设备当前已安装证书与版本未取得，仍是 **UNKNOWN**；不能保证覆盖任意同包名安装。

| 项目 | 实际证据 |
| --- | --- |
| 公有 applicationId | `com.starcore.research.r13`，基线manifest与CI实际APK回执相同 |
| 基线版本 | `577311009` / `R30-REPAIR1-2026.11.02-009` |
| 基线证书SHA256 | `d8d8cee7f406267b2bf18fc9aef0c6ab760423cf205ccd6613dd44085bcc74e7` |
| 基线APK SHA256 / bytes | `da42eaf3f6458bbf78ef256cfbe5667e29a18a51a94a02730a1d2f46b9c05749` / `28060658` |
| signer ref / source ref | signer commit `e1f5cb33fce444e2cd611072b50077bbe835fdf2` 的请求锁定完整 `5237...` 源码；不是拿signer分支HEAD冒充产品源码 |
| 当前R31源manifest观察 | 相同 applicationId，`577311011` / `R31-CHECKPOINT-P02-2026.11.04-011`；实际CI6成品仍须再核验 |

历史实际 [run41420785370](https://github.com/gtt46461-star/starcore-public-build/actions/runs/41420785370) 成功。build job `125195540441` 的实际输出记录精确源码、APK包名、版本、证书和hash；publish job `125213700282` 实际发布 [R30 009基线release](https://github.com/gtt46461-star/starcore-public-build/releases/tag/r30-repair1-577311009-5237b265a27f)。当前公开release metadata的target与APK digest与该回执一致。证据摘录保留原行号/时间与完整decoded UTF8日志SHA，见 actual-build-excerpts.json / actual-publish-excerpts.json。此旧APK仅用于身份审计，**不是R31交付物**。

同设备覆盖成立的条件是：实际安装包也是该公有 applicationId、实际签名为d8d8，且版本条件允许；最终由本次成品与真实安装验证确认。现场所需最小只读材料是已安装包的版本和证书指纹（例如读取pm path对应的base.apk并仅apksigner验证公有证书），不需要数据库、私钥或令牌。当前没有该设备材料，所以device-installed cert/version及真实覆盖安装均未验证。

旧official R13证书 `9e6d5dc79d29d4ba5b435a171e6535386f8a56ea6fad71a6cc77aeb10ade3eca` 与d8d8不同。既有R27日志明确“official R13 upgrade BLOCKED”，当前未发现经验证的9e6d→d8d8签名轮换谱系；本lane不能宣称跨签覆盖。若未来实际installed cert证明为9e6d，应另核验同源码复用既有official key的产物，不能凭相同包名假报兼容，也不能以卸载清库充当保数据升级。

root已决定本轮仅沿指定公有基线的d8d8既有cache，不开启第二lane。本审计未修改任何production/tools/远端ref、未触发workflow、未新建签名、未读取或导出签名私钥/GitHub凭据。没有把“cache配置存在”“job成功”单独算作安装通过；本机未重新运行历史APK的apksigner，证书结论来自历史实际测量回执与匹配发布asset。CI6实际APK的签名/身份/hash仍是独立必需门。
