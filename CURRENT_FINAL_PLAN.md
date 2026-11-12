# STARCORE 唯一施工与交付入口

当前有效原方案：**R31.4-MERGED-11-20261107，K01—K11，56张原卡+2张修复卡**。

**2026-11-12运行验收重开：** K01—K11存在LOCAL关门收据，但V2353A的577311020快检证明业务尚未恢复、报告请求在入队前失败。不得把“11批关门”写成“手机全部正常”。原方案222项验收仍为LOCAL 189、EMU 8、DEVICE 24、NATURAL 1；原阈值和封存方案不改。

- 最新应用源码：[construction/r31-final-20261104](https://github.com/gtt46461-star/starcore-public-build/tree/construction/r31-final-20261104)。main不是最新应用源码。
- 本次代码修复与交付验收：[R31Q21说明](construction/r31q/021/README_ZH.md)。
- 58卡、123个方案路径、44指标、47图、16逻辑表、40项用户要求：[HTML审计](construction/r31q/021/STARCORE_R31Q21_SOURCE_AUDIT.html) · [机器对照](construction/r31q/021/SOURCE_ACCEPTANCE_AUDIT.json)。源码存在不等于功能运行通过。
- [本次修复项](construction/r31q/021/REPAIR_WORK_ITEMS.json) · [源码红线复核](construction/r31q/021/SCHEMA_BLOCK_REVIEW.json) · [执行入口](construction/r31/NEXT_ACTION.json)。
- 原31.4：[17分册](construction/STARCORE_R31_FINAL_20261104/docs/17_R31.4批次合并说明.md) · [完整方案](construction/STARCORE_R31_FINAL_20261104/README.md)。历史收据保持原提交身份。

交付流程：真实代码 → 实际编译调用索引与身份冻结 → 干净提交 → 原单测/lint/注册HOST全套 → Android协议及故障矩阵 → Android34完整报告ZIP发布和分享读取回算 → 真实服务新抓取/新赛前冻结/随后结算 → 同包名、既有签名、递增版本APK。

全部关门后的返修仍执行 `python3 -B construction/STARCORE_R31_FINAL_20261104/tools/r31_gate.py build --batch K11`，只刷新本次构建证据，不伪造新关门。CI须检查摘要源码等于本次提交；缺失、过期、编译/单测/HOST失败不能算绿。

当前R31Q21验收在进行中，以实际CI与交付回执为准。V2353A原670MB数据库未在云端取得；24/72小时真机与自然质量未实测时不得记PASS。
