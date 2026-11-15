# P4 报告边界验收：20 + 31 条真实断言

本目录可整体复制到候选仓库的 `tools/r324/diagnostic/`。仅需 Python 3.9+ 及带编译器的 JDK 17+；默认通过 `java -m jdk.compiler/com.sun.tools.javac.Main` 编译，可显式指定 `--java`、`--javac`。脚本没有固定工作区路径，也不下载依赖。

## 运行候选源码

```bash
python3 tools/r324/diagnostic/run_diagnostic.py \
  --repo /absolute/path/to/candidate \
  --out /absolute/path/to/new-evidence-directory
```

输出目录必须是新目录或空目录。脚本不清理旧回执，不改生产源码；重复运行应换新目录。Windows 等平台可以直接传相应绝对路径。Java 参数通过 subprocess 列表传递，不经过 shell 拼接。

如需复核精确旧基线加一份独立替换包，可以额外指定 `--overlay /absolute/path/to/replacements`。该目录只有实际存在的同相对路径文件会覆盖 `--repo`，其他文件仍来自基线。回执逐文件记录选取的绝对路径、SHA-256、字节数与实际编译副本位置；最终候选仓库验收建议只传 `--repo`，直接验证落盘后的源码。

## 编译和执行内容

| 测试 | 真正被编译的生产源码 | 断言 |
|---|---|---:|
| PageAuditBoundaryProbe | ResearchTableAuditR32、StatsR27、JsonR27 | 20 |
| AssessmentBoundaryProbe | ReportAssessmentR33、CheckMetricCrosswalkR31、MetricDefinitionsR29、MetricEvaluatorR29、StatsR27、JsonR27 | 31 |

生产文件全部从 `--repo/app/src/main/java/com/ethk3/novaonepro/strategyv2/` 读取（显式使用 `--overlay` 时以回执为准），测试不是重写目标算法。编译器的 sourcepath/classpath 指向隔离空目录，仅编译回执列出的文件，防止意外载入其他 .class。每个输入先复制到输出目录冻结，编译真实副本，运行后重验输入文件未变化。

第一页测试没有依赖替身。第二组仅为无关调用提供以下明确的编译声明/适配，它们不是完整业务实现，不能由本测试验证其运行正确性：

| 编译声明 | 内容/范围 |
|---|---|
| CertificateGateR27 | GATE_VERSION 字符串声明；未运行原资格门。 |
| MetricCardMapR29 | VALUES 字段形状声明；未验证完整修复卡映射。 |
| ReportR27 | R30Input 数据容器和 Collections.unmodifiableMap 包装；未执行报告采集。 |
| DiagnosisRulesR30 | evaluate 签名；调用会抛 AssertionError。 |
| IssueTimelineR30 | Stage/Row 形状；业务方法及 summarize 调用会抛 AssertionError。 |
| ReportEvidenceProjectionR33 | reference 签名；调用会抛 AssertionError。 |
| ReportR29 | probes 签名；调用会抛 AssertionError。 |

四个指标类的被测函数是仓库真实源码，不能替换为这些声明。正例输入是受控的完整指标回执夹具；伪造ID、样本量、顶层值与评估回执不一致、Q08漏项/失败、已知FAIL被缺项遮蔽、M33/M34遗漏、合成/自然角色混用以及缺错误列表均有反例。

## 回执与退出状态

`RESULTS.json` 保存每次编译/执行命令、退出码、完整输出、日志SHA、编译输入SHA、实际断言数、失败数以及运行期间输入变化。只有两组都实际编译并运行、退出码0、输出恰好20/31断言且失败0、输入未变化，才计算 `HOST_ASSERTIONS_PASSED`。编译失败、Java异常、超时、断言失败或输出缺失都会非零退出；脚本不会生成固定PASS代替运行。

R32.3旧基线预期非零：20条中16失败、31条中21失败。这是对已知缺陷的负面对照；不应为了把基线跑绿而改测试。R32.4五生产文件修订的聚焦结果是51条、0失败。

编译用 `-source 17 -target 17`，不等于 `--release 17` API兼容验证。宿主断言不是Android构建、签名安装、设备生命周期、完整工厂合成实验或自然未来质量证明。回执固定保留 `naturalQuality=NOT_PROVEN`、`realAuthorizationGranted=false`。
