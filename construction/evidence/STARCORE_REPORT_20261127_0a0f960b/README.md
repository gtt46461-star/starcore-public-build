# STARCORE 用户原始检测报告归档（2026-11-27）

## 完整报告下载与还原

原始 ZIP 因上传接口的单次请求大小限制，无损拆分成两个二进制分片。两个分片均为完整报告的一部分，未删减内容，也未重新压缩或修改报告。

1. 下载本目录中的 `restore_report.py`、`REPORT_MANIFEST.json`，以及 `raw/` 下的两个分片，保留目录结构。
2. 在本目录执行 `python3 restore_report.py`（Python 3.9 或更新版本）。
3. 脚本逐一校验分片并还原 `raw/STARCORE-0a0f960b3e7e03c93806dcdc5c5fd49282eef1a2f7b.zip`，还原后再次核对完整 ZIP 的大小和 SHA-256。

- [分片 1](raw/STARCORE-0a0f960b3e7e03c93806dcdc5c5fd49282eef1a2f7b.zip.part001)：9,742,242 字节
- [分片 2](raw/STARCORE-0a0f960b3e7e03c93806dcdc5c5fd49282eef1a2f7b.zip.part002)：9,742,242 字节
- [自动还原脚本](restore_report.py)
- [原包及分片校验清单](REPORT_MANIFEST.json)

## 原始附件信息

- 文件名：`STARCORE-0a0f960b3e7e03c93806dcdc5c5fd49282eef1a2f7b.zip`
- 精确大小：19,484,484 字节
- SHA-256：`e67f3aa15c0d22f0934344b1c38962d992e042d41f8ad79f2c639775135df981`
- Git blob SHA-1：`44cd956c7eea7363a24be498e226f509e1ccfaa4`

外层 ZIP 共 3 个条目，内层 `STARCORE_FULL_REPORT.zip` 共 56 个条目；两层压缩包均已通过 Python `zipfile.testzip()` CRC 完整性检查。

分片可单独使用 `sha256sum -c SHA256SUMS` 校验。日期为本次归档标记。本次提交用于保存用户提供的检测报告，不代表已完成报告分析、代码整改或软件验收。
