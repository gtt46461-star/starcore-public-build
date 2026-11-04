# R31 原始源码基线验证

这些记录只验证完整源码提交 `5237b265a27fe5ad27fd842901c445d5f8c27749`。运行目录为独立本地全仓 `/tmp/r31-baseline-5237`，构建结束后 HEAD 未变且 `git status --porcelain` 为空，没有混入并行 R31 修改。

- `baseline-receipt.json`：命令、退出码、日志摘要和边界。
- `canonical-approved.log`：实际 `:app:verifyCanonicalPlanBuildCP3`，8 分 18 秒完成。
- `junit/`、`junit-summary.json`：97 个既有测试套件、351 项测试，失败 / 错误 / 跳过均为 0。
- `lint/`：release lint 未发现新增问题，既有 baseline 过滤了 51 个 errors，另有 7 项失效 baseline 条目。不能据此声称没有 lint 缺陷。
- `production-ddl.log`、`writer-host.log`、`commit-failure-host.log`、`progress-host.log`：既有 DDL、写入、原生 SQLite 提交故障和迁移进度 host 回归。
- `signing-paths.json`：现有合法签名和远程构建路径审查，未读取或复制任何私钥和凭据。

默认沙箱的 Gradle socket 与代理访问失败保留在原始日志和回执中；正常提权后沿用原代理与 CA 执行成功。

本目录不证明当前 R31 已实现、Android 设备集成已通过、签名 APK 已构建或安装、24 / 72 小时运行已完成，也不证明自然未来质量达标。既有 null / planted / synthetic 单元测试仅说明测试合同在该基线上通过。
