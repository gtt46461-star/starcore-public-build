# STARCORE R32.5 当前施工方案入口

**当前唯一施工方案为 `STARCORE_R32_5_FINAL_REPAIR_20261115`。** 本轮交付完整可施工方案与逐批、逐问题、逐文件的完整后态代码：5批、18个联合问题、312个唯一owner受控路径、408份完整文件卡；实际改动120件，其中新增64、替换56，另192件按原字节保全。

## 取得完整总包

[下载完整总包 STARCORE_R32_5_FINAL_REPAIR_20261115.zip](https://chatgpt.com/library?file_id=libfile_fde3_f-853dd39521948191baaa8fc3a27ca34e_6c5a0994ebec81918210221125116f06&account_id=personal)

完整ZIP大小 **282,379,138 bytes**，包含 **1,747个外层成员**；内含完整基线、候选源码快照及全部问题卡和验收材料。完整ZIP下载后直接解压，无需合并分片。

SHA-256：

```text
32655095d37039dd1db57b4c22190c77804ee5127905548fa63d79787d8d4907
```

可核对[完整包摘要](PACKAGE.sha256)、[交付回执](DELIVERY.json)与[精确计数](contracts/COUNTS.json)。

## 从仓库分片恢复

仓库另保存34个字节分片：前33片各8MiB，最后一片5,555,074 bytes。它们是同一完整ZIP的顺序切片，不是34个施工批次，也不能单独安装。顺序、大小和每片SHA见[分片清单](PACKAGE_PARTS.json)，[恢复脚本](restore_package.py)会检查分片、完整ZIP SHA及CRC，再解包。

取得此发布目录内的脚本、清单和全部 `parts/` 文件后，在该目录执行；需要Python 3.8或更新版本，无第三方依赖：

```bash
python3 restore_package.py --extract R32_5_restored
cd R32_5_restored/STARCORE_R32_5_FINAL_REPAIR_20261115
```

`R32_5_restored`必须是尚不存在的新目录。若从仓库根目录开始，先进入 `construction/repairs/STARCORE_R32_5_FINAL_REPAIR_20261115/`。恢复完成后，在上面的完整包目录打开 `START_HERE.html`，再按 `CODEX_PROMPT.txt` 和五批合同执行。直接下载完整ZIP的用户，解压后进入 `STARCORE_R32_5_FINAL_REPAIR_20261115/` 即可。

## 当前可直接阅读的发布文件

- [仓库概览与恢复说明](README.md)
- [Codex施工提示词](CODEX_PROMPT.txt)
- [最终复核与补漏](FINAL_REVIEW.md)
- [问题、根因和升级方向](DIAGNOSIS_AND_UPGRADE.md)
- [算法与资产质量升级合同](ALGORITHM_UPGRADE_CONTRACT.md)
- [外部条件与未签收范围](OPEN_ITEMS.md)
- [五批宿主复演说明](HOST_REHEARSAL.md)与[验证总账](VERIFICATION.json)
- [精确候选源码回执](contracts/SOURCE_CANDIDATE_RECEIPT.json)

本仓库发布目录只提供导读、回执和完整包恢复材料。`START_HERE.html`、`batches/`、`issues/`、`code/`、`tools/`、完整 `verification/` 等导航都在恢复后的完整包中；请先恢复再使用这些包内路径。

## 施工与验收边界

精确施工基线为 `7424eeae6354eb6599dd8498e123c059bd410c90`。R31与此前R32版本保留为需求和历史证据，不再作为并行施工入口。按P1→P2→P3→P4→P5和唯一owner推进，在已授权范围内保全用户修改、执行原身份与验收门。

本包的工程宿主与封包回执不代表正式APK已构建、设备已升级、24/72小时实机已通过或自然策略已取得高胜率。新资产进入真实主链仍须独立证据、原资格和用户实际允许；源码交付不能代替这些条件。当前状态与剩余条件以[验证总账](VERIFICATION.json)和[外部条件](OPEN_ITEMS.md)为准。
