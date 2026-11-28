# 运行/导出专项对总链路合同的独立复核

复核文件：construction/plans/STARCORE_R34_4_REPAIR_20261128/contracts/PIPELINE_AND_PRESERVATION.md。

结论：主链唯一所有者、PIT、版本底座、原件保全、真实资格不降门槛设计正确。尚需以下3处措辞/合同补强；均能增量修订，不要求推翻整套结构。

1. 第3节将“报告生成在有界研究切片中推进”改为“训练、报告等各自在既有有界后台owner推进；报告继续独立diagnostics/ReportExportServiceCP4，不并入ResearchWriteQueue的研究事务”。避免施工方把当前正常导出迁到未打通的研究owner，从而再破坏导出。
2. “不把失败任务原记录改为成功”应明确“不改写原始失败证据”。已有report job允许按原状态机在新revision重试，真实成功后进入READY并保留失败历史。否则会与已有FAILED→INDEXING恢复转移矛盾。
3. 增加报告/存储专节：完整scope JSON不可走UI截断器；依赖缓存只限同snapshot+definition+parameters；取消须等runner实际退出；FAILED快照需恢复同请求profile的精确替代产物并验证，再按原cleanRaw规则退役；READY清理失败不能降级；最终ZIP/index/manifest/semantic receipt/share继续保留。

未发现可支持“全部40超时都是同一SQL”“所有训练完全从未执行”“本版本发生7319次crash”“5份快照全部可删”这些说法，任何主文都不应使用。

SQL候选验证是SQLite主机夹具，scope候选验证是原件/源码合同与字符串模拟，均非Android编译或真机完成。真机长跑和自然质量应保持未验证状态。

## 第二轮：EXPORT_AND_STORAGE 专项合同

复核文件：construction/plans/STARCORE_R34_4_REPAIR_20261128/contracts/EXPORT_AND_STORAGE.md。

结论：**当前版本的报告与存储合同通过专项设计复核，可以进入逐文件施工；这不等于应用补丁已编译或真机已通过。** 前述主链文档的报告独立owner和失败attempt语义已经增量修正。导出专项保留原96项、原发布/分享接口、失败也可导出、严格区分诊断包和完整原件包；未要求删慢检查或改READ_ERROR为PASS。

第二轮唯一提出的歧义也已消除：第4节新增明确段落，DIAGNOSTIC作业在同一快照真实恢复成READY并完整满足原请求后，继续按既有requireRawCoverageR31/cleanRaw退役工作快照；不强迫默认诊断包携带5GB原库。FULL_RAW或仍有独立原件恢复引用才要求精确原库替代证明。因此“摘要不能代替原库”和“默认诊断成功后允许释放工作快照”同时成立，未引入无限保留作为安全的替代。

保全检查结果：

- `ReportPublicationCP12.cleanRawOwned:255–277` 的ZIP、语义回执、遗留archive核验、跨进程分享锁和保留文件清单继续作为基础；清理失败不降级READY。
- `requireRawCoverageR31:280–287` 仍按原request/manifest profile判定是否需要原库；FULL_RAW必须同SHA同字节数，未知profile继续保护，不能默认为诊断。
- FAILED唯一输入不会按24h、目录大小、另一个不同快照成功直接删除；先真正恢复原请求或提供精确替代物，再持久化引用释放/清理回执。
- memo失败只限本快照、本参数、本attempt；新context不继承旧取消状态。取消后等待runner实际停止使用连接；未来异步实现不能只凭Future.cancel确认退出。
- 快照pin、CAS原件、活动reader和正式结算/训练引用分别核实；清理只触及精确许可文件，不扩大到原工作区或其他作业。
- 10项导出回归覆盖普通导出、慢检查、取消重试、进程死亡、分享、低磁盘、故障队列、scope、5GB库与既有模式；Android/目标设备场景仍是施工后的待验证门槛。

新增RT18/RT19已独立记录，防止把观察缺口或结算历史债混入“实时推理完全没有运行”：结算163非终态均LIVE_RETRY且无当前shadow行，162实际draw已经不在5000热表；另有2899延后证据非终态。当前冻结writer投影缺独立SQL pin锚，修复300字符截断不会使它自动具备时钟锚；旧报告维持UNKNOWN是正确语义。
