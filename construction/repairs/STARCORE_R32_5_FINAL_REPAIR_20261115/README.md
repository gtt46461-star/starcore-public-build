# STARCORE R32.5 仓库发布概览

**当前唯一施工方案：`STARCORE_R32_5_FINAL_REPAIR_20261115`。** 本目录提供完整返修总包的持久下载入口、可核对回执、34个仓库分片及恢复脚本。完整方案包含全部逐批、逐问题、逐文件代码，下载或恢复后即可审阅和按原门施工。

[直接下载完整ZIP](https://chatgpt.com/library?file_id=libfile_fde3_f-853dd39521948191baaa8fc3a27ca34e_6c5a0994ebec81918210221125116f06&account_id=personal) · [当前施工入口](REPOSITORY_ENTRY.md) · [Codex施工提示词](CODEX_PROMPT.txt) · [最终复核](FINAL_REVIEW.md)

## 交付规模与身份

| 项目 | 本次交付 |
|---|---:|
| 施工批次 / 联合问题 | 5 / 18 |
| 唯一owner受控文件路径 | 312 |
| 逐问题逐文件完整代码卡 | 408 |
| 实际改动 | 120：新增64、替换56 |
| 原字节保全引用 | 192 |
| 完整ZIP外层成员 | 1,747 |
| 完整ZIP字节数 | 282,379,138 |
| 分片 | 34；前33片各8MiB，最后一片5,555,074 bytes |

同一文件可以被多个问题引用，但只有一个写入owner；408张文件卡不代表408个不同文件或408项行为验收。原字节保全引用也不计为新增功能。精确分配见[计数清单](contracts/COUNTS.json)。

精确施工基线：`7424eeae6354eb6599dd8498e123c059bd410c90`。完整ZIP文件名为 `STARCORE_R32_5_FINAL_REPAIR_20261115.zip`，SHA-256为：

```text
32655095d37039dd1db57b4c22190c77804ee5127905548fa63d79787d8d4907
```

[PACKAGE.sha256](PACKAGE.sha256)、[交付回执](DELIVERY.json)、[候选源码回执](contracts/SOURCE_CANDIDATE_RECEIPT.json)分别记录总包和包内候选源码的实际身份。总包摘要不等于APK摘要。

## 方式一：直接下载完整ZIP

[下载 STARCORE_R32_5_FINAL_REPAIR_20261115.zip](https://chatgpt.com/library?file_id=libfile_fde3_f-853dd39521948191baaa8fc3a27ca34e_6c5a0994ebec81918210221125116f06&account_id=personal)，核对上述SHA后直接解压，无需下载或合并仓库分片。进入解压得到的 `STARCORE_R32_5_FINAL_REPAIR_20261115/`，打开 `START_HERE.html`；该入口包含五批合同和全部逐文件代码导航。

## 方式二：从仓库分片恢复

取得仓库 `gtt46461-star/starcore-public-build` 中的 `construction/repairs/STARCORE_R32_5_FINAL_REPAIR_20261115/` 整个发布目录，保留 `parts/` 内全部34片、[PACKAGE_PARTS.json](PACKAGE_PARTS.json)及[restore_package.py](restore_package.py)。分片是完整ZIP的字节切分，不是施工批次。

从仓库根目录执行以下三行；需要Python 3.8或更新版本，无第三方依赖：

```bash
cd construction/repairs/STARCORE_R32_5_FINAL_REPAIR_20261115
python3 restore_package.py --extract R32_5_restored
cd R32_5_restored/STARCORE_R32_5_FINAL_REPAIR_20261115
```

脚本按清单顺序重组，核每片大小和SHA、完整ZIP SHA及全部ZIP CRC；默认在清单目录生成 `STARCORE_R32_5_FINAL_REPAIR_20261115.zip`，并解包到新目录 `R32_5_restored`。该目录必须不存在。脚本拒绝覆盖未知ZIP或已有解包目录；解包失败后保留的部分目录不作为成功结果，重试应指定另一个新目录。

此时工作目录已经是完整方案目录。打开 `START_HERE.html`，阅读 `CODEX_PROMPT.txt`，依次使用 `batches/P1.md` 至 `batches/P5.md`；这些路径均位于恢复后的完整包内。

## 本次五批施工范围

| 批次 | 责任范围 | 联合问题 | 唯一owner路径 |
|---|---|---:|---:|
| P1 | 旧库、配额、后台、实时事件及因果接口 | 6 | 58 |
| P2 | 历史训练、创造工厂、原子矩阵、迭代、冷热存储与恢复 | 6 | 148 |
| P3 | 独立未来、资格证书、唯一主链与撤销 | 2 | 25 |
| P4 | 检测报告、逐对象处方、图表、原件出口 | 3 | 54 |
| P5 | 集成回归、设备采证、版本与覆盖交付 | 1 | 27 |

跨文件合同按唯一owner同批交付；工程关门只依赖已施工文件与已有接口。设备时长、缺失原件和自然未来分别留证，不由后批替前批补代码或补签。

## 本目录可以直接阅读的材料

| 文件 | 用途 |
|---|---|
| [REPOSITORY_ENTRY.md](REPOSITORY_ENTRY.md) | 当前权威入口和最简恢复步骤 |
| [CODEX_PROMPT.txt](CODEX_PROMPT.txt) | 配套施工提示词 |
| [FINAL_REVIEW.md](FINAL_REVIEW.md) | 最终查缺补漏、确认修复及剩余条件 |
| [DIAGNOSIS_AND_UPGRADE.md](DIAGNOSIS_AND_UPGRADE.md) | 原报告事实、问题根因及具体升级方向 |
| [ALGORITHM_UPGRADE_CONTRACT.md](ALGORITHM_UPGRADE_CONTRACT.md) | 创造、训练、淘汰、独立验证和主链应用合同 |
| [OPEN_ITEMS.md](OPEN_ITEMS.md) | 原库、缺件、设备和自然效果的外部证据条件 |
| [HOST_REHEARSAL.md](HOST_REHEARSAL.md) | 在完整包内重跑五阶段宿主合同的方法 |
| [VERIFICATION.json](VERIFICATION.json) | 实际验证结果与范围 |
| [contracts/COUNTS.json](contracts/COUNTS.json) | 精确批次、路径、动作及代码卡计数 |
| [contracts/SOURCE_CANDIDATE_RECEIPT.json](contracts/SOURCE_CANDIDATE_RECEIPT.json) | 包内候选源码的字节身份 |
| [DELIVERY.json](DELIVERY.json) | 完整交付回执 |
| [PACKAGE.sha256](PACKAGE.sha256) | 完整ZIP摘要 |
| [PACKAGE_PARTS.json](PACKAGE_PARTS.json) | 34片的顺序、大小和摘要 |
| [restore_package.py](restore_package.py) | 完整包重组、校验和解包 |

本目录未展开整个总包。上述文档中引用的 `issues/`、`batches/`、完整 `contracts/`、`tools/`、`verification/` 和源码快照，须在恢复后的完整包内打开；不能将仓库发布目录当成已经展开的施工工作区。

## 施工授权与结果边界

这是完整可施工方案和代码交付。执行时遵守用户已授权的施工范围，保全用户修改，按原身份及验收门推进；R31及旧R32材料保留为来源，不与当前R32.5同时指挥写入。

累计创造不设固定业务总量上限，活动资源与物理存储有界；淘汰无用候选时保全有效谱系和原件。新子代必须独立训练、取得原协议要求的未来证据，经原资格和用户实际允许后进入唯一真实主链，不能继承父证书或用候选数量冒充质量。

本轮工程宿主通过不代表正式Android构建/签名APK、设备覆盖升级或24/72小时长测通过，也不证明自然高胜率或全部原业务条款通过。APK、设备和自然效果分别按真实证据签收；详见[验证总账](VERIFICATION.json)及[外部条件](OPEN_ITEMS.md)。
