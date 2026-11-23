# 本轮验证范围及未执行项

## 已完成

- 三用户原ZIP、内层ZIP与150个普通报告成员完整性核验，remote原ZIP重新取回核SHA256一致。
- 095源码和main方案分别取得；1362个生产Java文件建立hash索引。索引覆盖不是声称每行均做完整语义证明。
- 三报告22项取证发现、运行源码根因与算法/训练链审计；13批66问题、逐文件、旧66承接和关键旧负例映射。
- 局部运行修复和候选冷启重生代码后态、对应095补丁应用检查；真实Java与SQLite合同验证，原测试日志随payload保存。
- 本轮新增周期证据/交付守门验证器含真实负例，包内清单/相对链接/来源与新文件状态核验。

精确执行结果以verification目录和各payload/verification机器回执为准，最终文件数与断言数由FINAL_SCOPE.json及verification/SELF_CHECK.json读取，不按手写总数累加不兼容测试。

## 未执行，不能签通过

- 66项生产改动的全量集成、整个Android应用编译、Gradle/NDK/正式签名APK构建。
- 目标vivo V2353A Android14安装、100期连续服务、24小时/72小时耐久。
- 原始1.4GB设备数据库重放、缺失checkpoint真实恢复、未导出的失败HTTP原体重演。
- 自然未来优势、稳定高胜率、盈利、证书达标及真实出手结果。

本环境没有Android SDK；宿主Java21提供编译模块但缺Java17 release签名，因此相关纯核测试采用-source/-target17，在JDK21 API上执行。不能因此声称已验证整个Android/Java17 API兼容性。

## 材料类型

APPLICABLE_PATCH：给定095前态可应用的具体变更；FULL_NEW_SOURCE：完整新增类；REFERENCE_PROTOCOL：已验证协议核，但Android现有owner仍需接线；METHOD_CONTRACT：明确方法/输入/事务/错误/验收的施工合同，尚无完整生产后态。每文件卡明确所属类型。无完整后态的条目不能被施工方当作已改代码或自动完成。

本方案可以明确施工顺序、约束、接口、原件和验收，但不会用“百分之百”代替尚未运行的应用事实。工程闭环和自然有效性分别出结果。
