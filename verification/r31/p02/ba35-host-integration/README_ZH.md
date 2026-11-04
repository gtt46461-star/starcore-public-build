# ba35 固定源码 24 组 HOST 整合实测

源码提交：`ba35b0d6328c98737cbfbd483ddd3dcbf182aaac`。24 组均已实际尝试，首次结果 20 通过、4 失败；服务协议因沙箱禁止 socket，在保留原始拒绝日志后，以相同脚本、源码及冻结依赖获准重跑通过。最终最新结果为 **21 通过、3 失败**，不能宣布 HOST 全整合通过。

失败分别为旧 writer 适配器缺少新协议依赖（20 项编译错误）、qualification 工程 fixture 缺少必需 `r26_evolution` schema、memory 原基线 `PolicyAst` 与当前 `OperatorGolden` ABI 混用。memory 的 55 项内存集成和 6 项既有 PIT/model JUnit 已通过，但基线 golden 及后续子项未完成。逐项命令、退出码、真实时间及日志 SHA 见 MATRIX.json；失败未被跳过或改写。

支持类来自本轮实际 Android debug 编译的 4414 个 class；运行前后与主树原始 class 逐 byte 相同，hash 清单及 after 收据均已保存。旧 bootstrap 支持 jar 与当前 jar 的24项差异只含 source commit/tree 身份字面量，逐 class SHA 和原始 bytes 等价比较结论见 class-byte-differences-summary.json 和 detail；本轮一律使用当前 ba35 冻结 jar，不混用旧身份。

本地完整原 class/支持 jar、app/build 原始备份及复跑工作区由 setup.json 指向 /tmp，未纳入公开源码凭据或签名材料。两个 tracked 生成身份文件原样保全，生产、测试、工具源码及 HEAD 未修改。isolated-artifacts 仅收集独立 tree 本次新建或重写的 verification 产物；不复制旧 PASS 收据来代替本次失败。

实际两分钟观测包含 RuntimeMemoryControl 120201ms，以及垃圾不可达/强引用两种资源恢复 120213ms/120206ms；RuntimeRecoveryConsumer 原生产消费者也实际运行并通过。它们是 HOST 工程观测，使用各工具明确列出的 Android/JDBC 平台适配器。它们不代表 ART、真机、24小时、72小时或自然策略质量验收；这些证据在本矩阵中均 NOT_RUN/NOT_PROVEN。
