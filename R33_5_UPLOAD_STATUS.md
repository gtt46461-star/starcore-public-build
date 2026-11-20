# R33.5 原包发布与传输记录

已发布于标签 [r33.5-plan](https://github.com/gtt46461-star/starcore-public-build/releases/tag/r33.5-plan)。该发布附件 `33.5.zip` 为 626,717,930 字节，下载后实测 SHA-256 与 GitHub 记录一致，为 `01af8e98ae4d0917b7dd3abe4bff2c44ef8e47cd80136df635cbb0441d6b3a79`。[下载原始附件](https://github.com/gtt46461-star/starcore-public-build/releases/download/r33.5-plan/33.5.zip)

附件内为两个已展开的目录，共 6,214 个文件。逐个比对两卷原件后，第一卷需补 62 个缺件、替换 17 个旧件，第二卷唯一文件字节相同。合并并应用第一卷差量后，共 6,276 个文件与认证两卷内容一致。

当前施工从 [R33.5 复核入口](construction/plans/STARCORE_R33_5_REVIEW_20261119/README.md) 开始。修订和自检范围以该目录为准。

历史 Git 分片路线已提交 7/75 片，共 58,720,256 字节。历史恢复工具需要全部 75 片。两卷原方案各自的长度、哈希和分片顺序保存在原清单中。[分片进度](construction/plans/STARCORE_R33_5_CONSTRUCTION_20261118/UPLOAD_PROGRESS.json) · [两卷 SHA-256](construction/plans/STARCORE_R33_5_CONSTRUCTION_20261118/SHA256SUMS)

2026-11-19历史复核从 GitHub 下载的附件在全新目录完成[组装核验](construction/verification/STARCORE_R33_5_20261119/assembly.log)，再从组装出的副本在 JDK17 环境执行[完整复核入口的 11 条命令](construction/verification/STARCORE_R33_5_20261119/check-plan.log)。两步退出码均为 0。回执对应的复核清单 SHA-256 为 `bf5a19fc221bc1db3d682f6f249257e29d305d7eb19c39f51e7b98beea117949`。

## 2026-11-20首次复核记录

该次重新下载同一626717930字节的发布原件，SHA256与上文一致。在原认证还原路线之上，加入8份Java修订、累计文件继承检查、完整R967输出和严格标签窗口等补正；当前为161次操作、135条路径。当前16条check_plan命令、十个全源码前缀、九批真实Git回放、报告链与原件检查的收据，统一见[2026-11-20验证目录](construction/verification/STARCORE_R33_5_20261120/README.md)。原发布资产无需更换，当前仓库prepare.py会带出完整修订。上面的11条命令及其seal仅对应历史版。

## 当前第二次复核

原附件按上方SHA验收，安装时配合当前修订目录。有效范围为9阶段、66项工单、164次操作、138条生产路径。取件和逐批安装见[当前总入口](CURRENT_FINAL_PLAN.md)，实际回执见[第二次复核](construction/verification/STARCORE_R33_5_20261120/second-review/README.md)。
