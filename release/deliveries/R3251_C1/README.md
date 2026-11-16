> 此目录保留首次源码工程包的历史快照。原签名缓存障碍已解决，当前正式 APK、覆盖安装和逐批新证据见 [033 交付入口](../../../R3251_C1_DELIVERY_033.md)。以下“没有正式APK”仅描述此历史 ZIP，不是当前状态。

# R32.5.1-C1 源码与工程证据包

本包包含真实源码提交的增量 Git bundle、逐批原始验证材料、222条逐项验收、1832条原需求、165对象证据边界，以及三种 ABI 的实际 Release native 库。

本包没有正式APK。实际CI因既有签名缓存缺失停止；本地正式打包因原API绑定缺失停止。未创建替代签名。没有完成覆盖升级、设备24/72小时或自然质量签收。

冻结源码提交：`0b43f3ff997426a5938bbe40ab63026f450e043b`。完整P5 Gradle/Release native构建使用 `a2efd722b3d7266c6fc87ae267a3547ef7712bd1`；两者的完整 `app/` Git树一致。后续差异为已审阅宿主夹具、静态清单和证据材料，见 `evidence/delivery/CURRENT_SOURCE_RELATION.json`。

增量bundle以前置提交 `7424eeae6354eb6599dd8498e123c059bd410c90` 为基线。在已克隆本公开仓库的工作目录中执行：

```bash
git fetch origin 7424eeae6354eb6599dd8498e123c059bd410c90
git bundle verify /绝对路径/STARCORE_R3251_C1_0b43f3ff9974.bundle
git fetch /绝对路径/STARCORE_R3251_C1_0b43f3ff9974.bundle refs/heads/construction/r3251-c1-ownerfix-20261115
git switch --detach FETCH_HEAD
```

bundle还包含原封包上对应owner修复的完整提交、原失败与复验历史；原封ZIP和耐久journal未被改写。证据入口是 `evidence/DELIVERY_R3251_C1.md`。SHA256SUMS.txt 核对本ZIP、bundle及DELIVERY.json；ZIP内的ZIP_CONTENTS_SHA256.json逐文件核对内部材料。

公开下载：[工程证据ZIP](https://github.com/gtt46461-star/starcore-public-build/raw/refs/heads/construction/r3251-c1-ownerfix-20261115/release/deliveries/R3251_C1/STARCORE_R3251_C1_0b43f3ff9974_ENGINEERING_EVIDENCE.zip)、[增量Git bundle](https://github.com/gtt46461-star/starcore-public-build/raw/refs/heads/construction/r3251-c1-ownerfix-20261115/release/deliveries/R3251_C1/STARCORE_R3251_C1_0b43f3ff9974.bundle)、[SHA-256清单](https://github.com/gtt46461-star/starcore-public-build/raw/refs/heads/construction/r3251-c1-ownerfix-20261115/release/deliveries/R3251_C1/SHA256SUMS.txt)。
