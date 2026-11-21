# STARCORE R33.5 施工与收件台账

最终应用源提交是 `9e186d256f8de5ec00b5c20c6e509ee1d5bb983f`，最终版本码是 `577311052`。原签 APK、源码 ZIP、可从空库恢复的 Git bundle、原始 Gradle 日志及总 ZIP 随各批 GitHub Release 交付。每个已接收的文件在下载清单中都有地址和 SHA-256。[九批收件及下载索引](ledgers/deliveries-and-hashes.json)

66 行工单表列出问题、所属批次、实际提交、改动路径和证据位置。详细 JSON 保留 365 项原附件的绑定、逐项前后态、原卡验收入口、宿主测试命令及退出码。各批的 `compiled-callers.json.gz` 从实际 APK 的编译索引提取直接调用方和被调用方，附类名、方法描述符、源文件与行号。[工单 CSV](ledgers/work-orders.csv) · [完整工单 JSON](ledgers/work-orders.json)

正式 033 基线为 `bb7b49bbcc9266bad548ec37e71403256fc06a4d`。指定方案为 `6d23ddc26e2a827daca7efaf55801e90ccd5feb6`。施工中保留了原 164 次操作和 138 条路径，因实际 Android 回归另登记了三份资源或测试材料。有效安装清单现有 167 次操作、141 条路径。P00 另有 AGP 应用脚本类路径补正。源码版本事务保留了两份正式 033 原报告记录的安装下限 577311033，现场 vivo 的实际安装版待核。修订后的 B01 至 B08 避开先前实物已分配的版本码，从 045 连续到 052。[发布源码](https://github.com/gtt46461-star/starcore-public-build/tree/9e186d256f8de5ec00b5c20c6e509ee1d5bb983f) · [施工修订及失败原件](https://github.com/gtt46461-star/starcore-public-build/tree/05d33dffcd594c4b3d3aa3ab5456f4257aa47455/construction/plans/STARCORE_R33_5_REVIEW_20261119/verification/20261121-source-repairs)

修订版 Android 构建采用 `46a5f30001f7be28132f07d5a1bec779951b262e` 的已审生产全文和原方案绑定。接收端的 `05d33dffcd594c4b3d3aa3ab5456f4257aa47455` 增补了 B02 探针的测试输入校验，实际应用树及生产安装清单相同。两个时点的 B08 安装器和累计源码回执均保留。[B08 原件目录](raw/B08) · [原签构建](https://github.com/gtt46461-star/starcore-public-build/actions/runs/44848002916) · [Android 连续覆盖升级](https://github.com/gtt46461-star/starcore-public-build/actions/runs/44848002681)

目标现场为 vivo V2353A、Android 14。模拟器回执明确记录设备类型。自然资格按 R27 和 SU2 各自协议审阅。未测样本、未取回原件和未验证的效果在对应台账登记。[现场复核入口](恢复与现场验收.md)
