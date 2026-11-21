# STARCORE R33.5 施工与交付

最终 Android 包为 `com.starcore.research.r13`，版本码 `577311058`。APK 内嵌的源码提交是 `69eab914678bc988b71f2d7ce2287ccf89bd49eb`，Git 树是 `1f6452db1d192c98e7463d52a07887e60c5fc64d`。各批原签 APK、源码 ZIP、完整历史 Git bundle、原始 Gradle 日志和总 ZIP 都已从各批 GitHub Release 下载核对。[最终交付表](最终交付.md) · [全部下载地址和 SHA-256](ledgers/deliveries-and-hashes.json) · [九批原验收器回执](ledgers/nine-signed-deliveries.json)

施工从正式 033 的 `bb7b49bbcc9266bad548ec37e71403256fc06a4d` 开始，原审定方案是 `6d23ddc26e2a827daca7efaf55801e90ccd5feb6`。原签现场授权环境独立构建每批版本，接收端核对 APK 证书、内嵌来源、Git 树和升级前安装版本。两份历史 033 原报告记录的 `577311033` 用作源码版本下限。目标 vivo V2353A 本次的实际安装版仍缺现场读取。[最终应用源码](https://github.com/gtt46461-star/starcore-public-build/tree/69eab914678bc988b71f2d7ce2287ccf89bd49eb) · [最终方案修订](https://github.com/gtt46461-star/starcore-public-build/tree/0780c7f4317ddf839e9c78a45fe18fd1859b7c87/construction/plans/STARCORE_R33_5_REVIEW_20261119)

原审定的 66 个工单、365 项附件、164 次操作和 138 条路径均已保留。为处理实际回归，修订后的受控操作共 169 次，覆盖 143 条路径。另行记录了四文件版本事务及 P00 的 AGP 加载修正。B05 修复首行超限阻断后续分页，B07 修复生产报告的矩阵引用在写入调度原件后失效。台账逐单记录源码前后态、方法、编译调用者、测试及尚缺的现场证据。[66 行工单表](ledgers/work-orders.csv) · [带附件的工单明细](ledgers/work-orders.json) · [逐路径真实 Git 差异摘要](raw/B08/real-git-changes.json)

完整模式终审结果、独立通过数和未通过项见五份逐项台账。Android 模拟器上的实际覆盖升级、文件选择器导出、受控故障和实时观测均附运行来源。目标手机 24/72 小时与 R27、SU2 的自然资格分别登记。[终审汇总](ledgers/final-summary.json) · [原终审返回](raw/B08/full-final-review.json) · [恢复与现场命令](恢复与现场验收.md)
