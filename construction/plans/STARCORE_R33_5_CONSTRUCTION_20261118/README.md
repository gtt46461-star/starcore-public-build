# STARCORE R33.5 原包和分片记录

## 当前取得与施工方式

从 [GitHub Release 下载 33.5.zip](https://github.com/gtt46461-star/starcore-public-build/releases/download/r33.5-plan/33.5.zip)，按[当前 R33.5 复核入口](../STARCORE_R33_5_REVIEW_20261119/README.md)校验并合并两个展开目录，应用第一卷 79 件差量和本次修订。[Codex 施工提示词](../STARCORE_R33_5_REVIEW_20261119/CODEX_EXECUTE.md)

以下步骤记录最初的分片传输和还原设计。该路线目前留存 7/75 个分片。全包来源和复核状态以当前入口为准。

## 历史分片说明

按用户 2026-11-19 的顺序要求：先上传两份完整原包，全部上传后再统一自检。

当前状态见 [UPLOAD_PROGRESS.json](UPLOAD_PROGRESS.json)。分片未齐时不能还原完整方案。这里的上传状态不代表方案自检、Android 编译、APK 或设备验收通过。已有原包内的历史报告按原字节保留。

## 原包

| 文件 | 字节数 | 分片数 |
|---|---:|---:|
| STARCORE_R33_5_CONSTRUCTION_20261118_part1.zip | 312757715 | 38 |
| STARCORE_R33_5_CONSTRUCTION_20261118_part2.zip | 309104609 | 37 |

两份 ZIP 合计 621862324 字节。文件内容不删减、不重写，以二进制分片保存；[PACKAGE_PARTS.json](PACKAGE_PARTS.json) 记录顺序、每片及完整 ZIP 的 SHA-256。

## 完成上传后的还原

在仓库中执行（Python 3.9+）：

```bash
cd construction/plans/STARCORE_R33_5_CONSTRUCTION_20261118
python3 restore_package.py --output-dir restored_archives --extract restored_plan
```

`restored_archives` 中是两份字节一致的原 ZIP；`restored_plan/STARCORE_R33_5_20261118/` 是合并解压后的原方案，另外保留原包带有的 `R335_PART1_REVIEW_20261119/`。

历史方案内保留 `00_START_HERE.html`、`01_主链与开工手册.md` 和 `CODEX_EXECUTE.txt` 等原说明。要实施当前修订，请从上方复核入口应用最新覆盖并读取其 Codex 提示词。

历史分片断点以提交后的 [UPLOAD_PROGRESS.json](UPLOAD_PROGRESS.json) 为准。
