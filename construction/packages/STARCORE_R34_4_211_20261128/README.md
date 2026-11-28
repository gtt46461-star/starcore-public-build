# 原始完整交付包 · 原字节保留

本目录保存用户明确授权上传的完整 `STARCORE_R34_4_211_20261128_Repair_Plan.zip`，包括原始诊断、完整逐条审计、46卡/12批、C01—C05全部候选、旧公开版及当时交付回执。

为适配上传接口请求大小，原ZIP分为27个二进制片；未重新压缩或修改任何原成员。`restore_package.py`自动按清单拼接，逐片核bytes/SHA256/Git blob SHA，再核完整ZIP身份。无需手工拼接，不需要第三方库或网络。

|原件|精确身份|
|---|---|
|文件名|STARCORE_R34_4_211_20261128_Repair_Plan.zip|
|字节数|20,814,251|
|SHA256|`4770e865a14cb3c4411e24d180368077c4585cdaa40255945a72742d615f9915`|
|分片|27；前26片各786,432 bytes，末片按PACKAGE_PARTS.json|
|ZIP成员|263；complete_plan 155、public_plan 106、包外说明/回执2|
|原诊断外层ZIP|19,471,817 bytes；SHA256 `e0e3ffdb5a58a33175d6ee305d788bfaea3bd6186495c1828a0e1edddb6112b0`|

## 一条命令还原

完整检出仓库后，在仓库根目录执行：

```bash
python3 construction/packages/STARCORE_R34_4_211_20261128/restore_package.py --output /absolute/new/path/STARCORE_R34_4_211_20261128_Repair_Plan.zip
```

将output换为需要保存的新文件位置。不传output时保存到当前工作目录。已有完全相同文件只核验复用；已有不同文件拒绝覆盖。任何分片损坏、缺失、顺序/长度不符都会失败，不生成表面成功的包。

只需核施工方案时，直接运行[完整方案](../../plans/STARCORE_R34_4_REPAIR_20261128_COMPLETE/README.md)的 `tools/prepare_evidence.py`；它临时还原包，仅取固定诊断成员并核其独立SHA。完成后须按完整方案README另运行 `tools/verify_plan.py`；物化成功不等于原报告56个载荷与manifest已全部核验。

## 原始资料与当前指令

原ZIP保持此前交付的每个字节，其中的旧PUBLICATION_RECEIPT、旧四候选公开版和当时说明是历史记录。它们不替代本次用户对完整公开上传的授权，也不表示当前仍排除C04或原报告。

后续施工以[当前完整执行指令](../../plans/STARCORE_R34_4_REPAIR_20261128_COMPLETE/CODEX_EXECUTE.md)及两份新增附约为准。原ZIP提供不可变审计依据；更新指令清晰区分资料上传与后续真实应用施工。本次没有构建新APK或改变用户安装数据。
