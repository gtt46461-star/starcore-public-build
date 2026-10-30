R29.1 正在按固定最终方案真实源码施工；仅 B00 已工程签收。B01（R29-004—008）仍未验收，先完成其真实安装验收，不新增 B02 源码施工。最新已验收交付仍为 CP06。

唯一依据：固定 96fb4355748a2f28e31927e1f9cb8cb61fac6547 中的 STARCORE_R29_FINAL_20261029，11 批、52 卡、44 指标。R26/R27 是 R29-004—008 指定改造的既有类名。

CP09 产品修复已提交 e3dd104302dc988e1acb0c03996c05fb229ae3e6；577291009 / R29.1-2026.10.30-checkpoint09。六个产品文件改为：先登记完整真实 PENDING 迁移目录、仅为选中的首次复制表安装严格跟踪器；事件写入 fresh 精确核验 schema，避免每行重复 DDL；五处专家纯 SELECT 走独立 Root 只读连接，保留事务读己写和 ACTIVE owner 校验。HOST 迁移准备21及原回归16、事件47、专家只读15、QA32通过。当前源码本地标准构建5分31秒通过：164 JUnit与8语义反证；lint分析执行但报告复用。原生9项实际通过，但完整App再次FAIL：stage登记378表、5表已验证后，restorePage复制动态app_state出现3次主键冲突，最终源哈希漂移，未发布RP。两安全产包仍待回执，四组正式安装NOT_STARTED。原失败见evidence/r29/batch01/failed/app-e3dd1043。正在修复独立Root只读事务快照，不能继承旧源码通过项。

CP07 两证书真实 release 身份验证通过，但 CP06→CP07 的 debug/release×R13/R27 四组实际覆盖升级全部 FAIL：安装、保留标志及两次启动检查通过，实际研究数据库缺失；完整升级数据比较和干净安装 NOT_REACHED。原失败保存在 evidence/r29/batch01/failed/device-1d2f1565。

CP08 源码 2f1e2ab9caf2722647127625076335de523f1244 的独立 native9/9 和两证书包身份通过，但完整 App 仍 stage manifest0、无 RP，出现 E07/E10 容量错误，因此未签收。CP08 四组正式 CP06 安装矩阵 NOT_STARTED。已有验证见 evidence/r29/batch01/validated-2f1e2ab9；独立 sandbox 通过不能替代完整 App。CP08 native 1000写 P99=89ms/最大15141ms保留，不宣称完整 M07/PREDRAW SLO。

仍待验：Android 各迁移阶段 OS 硬杀/WAL 恢复、真实 ENOSPC、OS 热压/前后台、V2353A 前台60分钟/息屏6小时/72小时。HOST kill 实际为异常注入主动 rollback，SQLITE_FULL 来自 SQLite 页上限；Native 协调器恢复为同进程重建。旧外部黄金/私有模型教师依赖缺件，无精确 pin 旧演化状态等待。自然质量 INSUFFICIENT、未知值 null、GateV2 仅诊断、REAL_BLOCKED；44 指标及1832行为未全部实测。原时间和数据门槛未降低。
