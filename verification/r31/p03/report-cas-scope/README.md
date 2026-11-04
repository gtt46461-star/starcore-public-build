# 报告copy的实际CAS来源作用域

生产只改 `ReportExportServiceCP4.java`，与History报告计数提交a815分开。

实际FULL_REPORT已有 `ReportInputsLeaseCP12.acquire(getFilesDir())` 保护文件输入，copy资源原来放在任务目录而非标准databases路径。新scope在copy之后声明，使用同一个明确的 `getFilesDir()/r26-checkpoints`，在AttachmentClosure／Catalog检查／R26／R27／R30消费者之前注册。Java try-with-resources在正常、异常和取消路径都先关闭该scope，再关闭copy；外层真实文件pin保持到原来的报告输入／导出作用域结束。没有往未发生CAS读取的ReportSnapshot硬塞映射。

scope只管理Checkpoint owner的有界路径映射。目录不存在时不mkdir，不根据临时DB位置推断目录，记录 `DEPENDENCY_CAS_SOURCE_DIRECTORY_UNAVAILABLE`，保留inline可读；实际reference reader仍通过owner的typed DEPENDENCY报告缺源。目录存在也只报告注册，`checkpointObjectsVerified`保持NULL，不把路径或目录当模型正确性证据。该来源观察单列在R30 source manifest，明确不属于SQL水印。

`runs/final-readonly-and-scopes/RECEIPT.json` 使用root真实4459类Android bootstrap，SHA `582f7475b43d218c50d34f8f24d4399ae4916b53579807c22c162bd25b7fea56`。该jar当时尚无新scope API，实际编译Checkpoint owner冻结源 `984da23fe1ee9efa1cc5cb931d94d008672d02f76ce584574c9e8e24d11c2789` 在前，没有伪造接口或把旧jar说成包含新API。ExportService、真实ReportInputsLease、真实Checkpoint源一起在Android SDK上编译；源码快照及SHA、实际class SHA、命令和原始日志都在收据中。

HOST实际运行170条断言、6组：

- 用实际ChunkedCheckpoint/CAS prepare写入131071字节工程payload，通过真实reference resolver完整读回；native SQLite自己拒绝对readonly连接的写入。
- 无原报告file pin拒绝注册；缺目录不创建，inline保持原字节，reference缺源是DEPENDENCY；同一DB映射不同根明确拒绝。
- 嵌套、幂等关闭后仍正确引用计数，metadata scope结束时copy还开着，原文件pin没有提前释放。
- 消费者异常进入finally；40个连续报告路径实际读回并关闭，未耗尽32映射上限。
- 真正修改fixture CAS对象字节触发INTEGRITY，恢复原字节后真实读回，区别于缺源。
- 实际线程interrupt后有限join确认线程退出；两条实际线程／两个native readonly连接读同一个DB路径，第一scope结束后第二reader仍能读回，最后reader退出后新连接再读才变DEPENDENCY。原file pin的exclusive尝试在实际reader退出前始终拿不到，退出后才能取得。

平台适配仅对HOST提供真正SQLite连接及readonly open flag、真实JDK时钟／PID、真实FileChannel.force目录fsync。ReportInputsLease使用真正FileLock，业务scope、映射、reference与校验都为实际生产代码。payload是工程字节回环，不能当模型学习／准确率证明。

这是**实际static生产helper和资源语义的HOST证据**。完整Android Service、ART、Android CancellationSignal、真机、24／72小时和自然质量均未运行，不能填写PASS。root仍需与Checkpoint最终refs同版集成并运行完整APK构建／仪器测试。CI入口已增加同一native runner，使用新完整app classes及实际app Checkpoint源。

未重跑401／322ms写时序以挑更快结果；这里没有设备时延或自然质量结论。没有改签名、applicationId、版本、模型算法或Checkpoint owner文件。
