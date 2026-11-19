# STARCORE R33.5 完整原包：分批上传入口

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

先读取原方案 `00_START_HERE.html`、`01_主链与开工手册.md`、`CODEX_EXECUTE.txt` 和本目录的后续自检记录，再按其真实批次清单施工。不要把本仓库当前应用源码误认为 033 施工基线；基线以原包所附身份清单为准。

完整上传和内容自检分别记账；断点以提交后的 UPLOAD_PROGRESS.json 为准。
