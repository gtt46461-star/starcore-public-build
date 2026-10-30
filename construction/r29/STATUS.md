R29.1 正在真实源码施工，B01 实际安装验收失败，尚未签收。最新已验收交付仍为 CP06；CP07 有真实签名包，但完整 App 分库安装路径未通过。

唯一依据：固定96fb4355748a2f28e31927e1f9cb8cb61fac6547中的STARCORE_R29_FINAL_20261029，11批、52卡、44指标。R26/R27是R29-004—008指定改造的既有类名。工程签收仅B00；先修复并验收B01，不新增B02源码施工。

CP07产品1d2f1565e308659b6e0989f03400213c60dfac1b，577291007 / R29.1-2026.10.30-checkpoint07；安装QA1241859217fbeb8337ca0707d23d13088a0ddaf3。两种既有证书的release包，已独立验证下载SHA、实际公共签名、版本和包内source；见delivery/CP07.json。产包不能代替安装验收。

CP06→CP07的debug/release×R13/R27四组实际API34测试全部FAIL：覆盖安装、保留标志和两次新进程启动检查通过，但实际starcore-r29-research.db不存在；未进入完整升级数据比较和干净安装。更早R26基线自动检查也FAIL：stage manifest为空，没有VERIFIED_BASELINE。全部原始失败保存在evidence/r29/batch01/failed/device-1d2f1565，未降低门槛。

已定位产品阻塞：prepare在数百张表Root tracker全部安装成功后才登记stage；中途writer busy会从头反复DDL。真实App已312个水印而stage仍空，日志有大量CP3_DB_BUSY_DEADLINE。事件任务无限submit重试亦可占住single worker。修复可续准备、已安装DDL核验复用和事件让出中，下一候选版本递增checkpoint08；不停止旧能力、不放宽正式写队列/时间/数据门槛。

工程子集通过：1d本轮CI36HOST、164JUnit（0失败/错误/跳过）、8语义反证、三CLI21/20/32组；lint本轮实际生成新增0、原51基线错误及7未匹配保留。Native源码6d43680的9项及原harness通过，1d的产品/release/generated与其字节相同，但独立Sandbox不能覆盖全App安装失败。Native schema30.805秒、首服务25.841秒、研究错误/容量失败0；1000写P99=64ms/最大17649ms真实保留，不宣称完整M07/PREDRAW SLO。

仍待验：Android各迁移阶段OS硬杀/WAL恢复、真实ENOSPC、OS热压/前后台、V2353A前台60分钟/息屏6小时/72小时。HOST14次kill实际异常注入主动rollback；SQLITE_FULL来自SQLite页上限；温控policy3/3与scheduler7/7通过，不等于真机。Native恢复仅同进程协调器重建。旧外部黄金/私有模型教师依赖缺件，无精确pin旧演化状态等待。自然质量INSUFFICIENT、未知值null、GateV2仅诊断、REAL_BLOCKED；44指标及1832行为未全部实测。
