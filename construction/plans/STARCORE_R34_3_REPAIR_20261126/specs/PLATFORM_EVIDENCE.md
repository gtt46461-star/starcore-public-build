# 平台约束与本次源码结论的边界

核验日期：2026-11-26（Asia/Shanghai）。平台资料只解释相应运行约束；本次实际问题仍以安装173的源码和原报告为证据。

## SQLite TEMP与主数据库

SQLite官方说明：[Temporary Files Used By SQLite，TEMP Databases](https://sqlite.org/tempfiles.html#temp_databases)指出TEMP表仅对创建它的连接可见，并位于连接的临时数据库。官方[ATTACH DATABASE](https://sqlite.org/lang_attach.html)定义`main`与`temp`分别为主数据库和临时数据库。

据此，本次不能假定连接池内准备读连接与提交写连接具有同一TEMP目录。报告的`R31_STARTUP_CHECKPOINT_TEMP_SCHEMA`与源码中第四张自有TEMP表相互印证；runtime补丁精确允许本版本实际创建的四种合同，并显式限定主数据查询，不开放任意临时表。平台资料不能代替真实Android连接池与现场SQLite版本的集成回归。

## 前台服务、用户停止与设备连续运行

Android官方[Handle user-initiated stopping of apps running foreground services](https://developer.android.com/develop/background-work/services/fgs/handle-user-stopping)说明，从Android 13起，用户可以在“正在运行的应用”管理界面停止有前台服务的应用，且该行为不取决于应用targetSdk。整个应用进程会停止，不能把有前台服务通知等同永不被停止。

官方[Restrictions on starting a foreground service from the background](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start)中的一般后台启动限制明确针对targetSdk 31及以上；本次报告是runtimeSdk 34、targetSdk 28。不能仅因为手机Android 14，就把该targetSdk门槛当成173版已经触发的确定根因。如果后续单独提升targetSdk，需要重新核验适用规则。

本次`runtime-observation-backgroundRuntimeCapabilityR31.txt`记录前台服务、通知、电池豁免、精确闹钟等条件当前可用，但24h/72h均`NOT_RUN`。本方案据此保留B04/B12真机退出门，修复后台业务owner及持续进展，不让Activity打开承担核心计算触发。权限当前可用、服务alive、一次恢复成功和整段时间持续无漏期必须分别计量。

1秒抓取是本应用的目标调度合同。报告中的目标时隙、真实唤醒、HTTP退避、断网、上一轮阶段阻塞和调度延迟都须保留原分母及原因；不能只看到`targetPeriodMs=1000`就判实际频率达标。
