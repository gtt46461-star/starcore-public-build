# STARCORE 当前唯一施工方案：R32.5

当前施工入口为 **`construction/repairs/STARCORE_R32_5_FINAL_REPAIR_20261115/`**，适用仓库 `gtt46461-star/starcore-public-build`。旧R31和旧R32版本保留为历史需求及证据，不再作为并行施工指令。

- [打开当前施工入口](construction/repairs/STARCORE_R32_5_FINAL_REPAIR_20261115/REPOSITORY_ENTRY.md)
- [查看仓库发布概览与恢复说明](construction/repairs/STARCORE_R32_5_FINAL_REPAIR_20261115/README.md)
- [读取配套Codex施工提示词](construction/repairs/STARCORE_R32_5_FINAL_REPAIR_20261115/CODEX_PROMPT.txt)
- [查看最终复核](construction/repairs/STARCORE_R32_5_FINAL_REPAIR_20261115/FINAL_REVIEW.md)
- [查看实际验证范围](construction/repairs/STARCORE_R32_5_FINAL_REPAIR_20261115/VERIFICATION.json)
- [查看外部条件与未签收范围](construction/repairs/STARCORE_R32_5_FINAL_REPAIR_20261115/OPEN_ITEMS.md)
- [直接下载完整ZIP](https://chatgpt.com/library?file_id=libfile_fde3_f-853dd39521948191baaa8fc3a27ca34e_6c5a0994ebec81918210221125116f06&account_id=personal)

本次交付为**5批、18个联合问题、312个唯一owner受控路径、408份完整文件卡**；120件实际改动（64新增、56替换），另192件原字节保全。精确施工基线为 `7424eeae6354eb6599dd8498e123c059bd410c90`。完整ZIP为282,379,138 bytes，包含1,747个外层成员；[交付回执](construction/repairs/STARCORE_R32_5_FINAL_REPAIR_20261115/DELIVERY.json)与[精确计数](construction/repairs/STARCORE_R32_5_FINAL_REPAIR_20261115/contracts/COUNTS.json)给出可核对身份。

## 取得并进入完整方案

完整ZIP可以直接下载并解压，无需合并分片；进入解压得到的 `STARCORE_R32_5_FINAL_REPAIR_20261115/`，打开 `START_HERE.html`。

若使用仓库内34个分片，从仓库根目录运行以下命令。需要Python 3.8或更新版本；恢复目录必须尚不存在：

```bash
cd construction/repairs/STARCORE_R32_5_FINAL_REPAIR_20261115
python3 restore_package.py --extract R32_5_restored
cd R32_5_restored/STARCORE_R32_5_FINAL_REPAIR_20261115
```

[恢复脚本](construction/repairs/STARCORE_R32_5_FINAL_REPAIR_20261115/restore_package.py)按[分片清单](construction/repairs/STARCORE_R32_5_FINAL_REPAIR_20261115/PACKAGE_PARTS.json)顺序合并并验证每片、最终SHA及ZIP CRC。完整ZIP的SHA-256为：

```text
32655095d37039dd1db57b4c22190c77804ee5127905548fa63d79787d8d4907
```

`START_HERE.html`、五批合同、逐问题逐文件代码、身份工具和完整验证原件都在恢复后的总包内。仓库发布页仅展开导读、回执和恢复材料，不应从未展开的发布目录寻找 `issues/` 或 `batches/`。

## 执行边界

本交付是完整可施工方案与代码，仍按用户已授权范围、原身份和原验收门执行。工程宿主通过不代表正式APK已生成、实际设备已升级或自然策略已取得高胜率。新资产真实进入主链须具备独立证据、原资格及用户实际允许；缺失原件、设备长测和自然效果保持各自真实状态，不以工程回执代签。
