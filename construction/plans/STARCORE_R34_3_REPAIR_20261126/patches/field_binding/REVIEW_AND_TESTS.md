# Field173 版本准备事务独立复核

## 确定问题与对应修复

原 wrapper 调用 `advance.prepare` 写出完整四文件 JOURNAL，然后才保存 FIELD173_PLAN。如果进程在这两步之间退出，旧 `--resume` 直接读不存在的 FIELD173_PLAN，无法恢复这份本来完整且尚未应用的版本日志。原 wrapper 对 existing pin 的校验在repo版本锁外；resume也未把JOURNAL声明的版本/系列与after manifest真实字节核对。

现在保留原 `advance_release_version` 的 `repo_version_lock`、`prepare`、`atomic` 和 `apply_locked`。没有新增版本生成格式、版本算法或第二个四文件事务。wrapper增加只读预检：

1. 锁内重新核对 binding 的安全普通路径与精确 pin；B01已有的相同pin允许B02继续使用，不覆盖不同pin。
2. 对原 JOURNAL 精确4文件、schema/repo/batch、真实整数升级versionCode、R34.2系列/有效日期/同code版本名、所有before/after hash、当前4文件属于before或after集合逐项校验。
3. after manifest的 applicationId/versionCode/versionName 必须与JOURNAL一致。使用原release generator从after manifest重生properties/Java/host BuildConfig并逐字节比对，全部通过后才写binding或版本。
4. FIELD173_PLAN缺失时，仅在全部4文件仍逐字节等于before、没有APPLIED完成声明的情况下，允许把本次已经通过完整plan/git seal校验的pin写入原日志；此前相同plan的binding可继续存在。这修复prepare结束至wrapper计划原子保存之间的明确中断点。FIELD记录使用原fsync+rename+目录fsync的atomic，而非仅文件fsync。
5. 如果FIELD记录缺失而已有任一controlled文件改变，拒绝并保留所有字节；不能凭未知来源的半截状态猜测计划。如果原prepare自身在完整JOURNAL出现之前被中断，也拒绝，不删除或重建不完整日志，使用新的external journal另作准备。
6. FIELD存在且匹配的正常半应用，继续由原apply_locked收敛四个文件；任何用户不同字节都会被保护并报 `IDENTITY_DIVERGED_RETAIN_USER_EDIT`。最终真实manifest与APPLIED/原JOURNAL再核对，才打印准备完成。

本包以`patches/field_binding/after/`完整后态和PATCHSET前后SHA为权威；未随包提供的旧临时生成脚本不是施工依赖。wrapper仍不commit、不签名、不安装、不宣称设备验收。

## 已执行测试

`tools/run_host_checks.py`调用随包`host_validation/field_binding_patch/tests/test_prepare_transaction.py` 运行真实旧advance API及原manifest generator，在临时Git仓库与外部journals上通过23个断言：

- 完整JOURNAL但无FIELD记录的中断点恢复至577311174。
- 已有相同pin时下一批B02及其相同中断点恢复至577311175。
- 在真实四文件atomic写入两项后注入进程中断；以原日志resume后四项与原generator一致，APPLIED只在全部完成后存在。
- 用户编辑、伪造JOURNAL版本声明、错误R33.5系列、after properties虽配套改hash但不符合原generator、锁内不同pin冲突均在任何身份覆盖之前拒绝。
- FIELD来源缺失且已有部分文件改变时拒绝并保留现场。

Python语法编译、`git apply --check`通过。测试仅覆盖wrapper与原版本事务，使用的pin/plan值是fixture；没有冒称验证真实远程plan commit授权、CI签名/APK、覆盖安装或设备质量。真实plan seal/git身份仍由main中的原校验执行，未跳过。
