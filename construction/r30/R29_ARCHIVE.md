# R29 B01 封存与可复用件登记（R30-B00-02）

- 封存标签：`archive/r29-b01-cp16` → `9ff38ffe7063bc938aededfa3271187f9140a0fe`（construction/r29-20261029 CP16 头，只打标签，不合并、不删分支）。
- 标签状态：本地已创建；本施工会话的 git 代理拒绝推送 tag 引用（HTTP 403），远端标签需仓库所有者执行 `git push origin 9ff38ffe7063bc938aededfa3271187f9140a0fe:refs/tags/archive/r29-b01-cp16` 补推。在此之前，原分支 construction/r29-20261029 仍指向 9ff38ff，未删除。
- R30 分支不含 bb607cf 之后的任何 R29 提交（`git log bb607cf..construction/r30-20261031` 只有 R30-* 提交）。

## 3. R29 B01 资产处置表（B00-02 照抄进 construction/r30/R29_ARCHIVE.md）

| 类别 | 类 | 处置 |
|---|---|---|
| 路由/切库核心 | ResearchRouteR29、ResearchCutoverR29、MigrationCopyR29、ResearchDatabaseR29、LegacyResearchScopeR29、ResearchRowsR29、ResearchInputMirrorR29、CriticalInputMirrorR29、RetainedFormalCopiesR29、LegacyDomainUpgradeR29、LegacyDomainExtensionR29 等 29 个 | 不并入。被 B11（条件批次）的 OfflineCutoverR30 取代 |
| 不依赖路由 | FetchCadenceR29、FetchObservationR29、ExperimentRegistrationR29、EvolutionDatasetPinR29 | 不整文件复制；B02-02 / B05-01 施工时可审读参考 |
| 所有权声明 | LegacyResearchOwnershipR29 | 不并入；表所有权以 scan_table_writers.py 静态清单 + 运行时 sqlite_master 为准 |
| 已在 R29 后期包装到手机上的副作用 | Root 上 r29_copy_track_* / r29_critical_track_* / r29_critical_input_write_guard* 等触发器；r29_database_route=ACTIVE；starcore-r29-research.db | **B00-03 R29FoldBackR30 在首次启动时回收**（guard 触发器会让 CP06 代码写入直接 ABORT，SQL 合同 T05 已复现） |

