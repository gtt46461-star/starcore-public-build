# 怎样验收每份文件确实被使用

第 3 版的 289 张文件卡中，有 73 个路径没有列入任何工单的 files。它们包括变异算子、工厂、主链选择记录和兼容工具。原签收器只要求每个运行引用至少一个属于该工单的文件，同时每批至少一个计划中的生产文件有改动。两者都可能成立，而遗漏的生产者仍然无人调用。

第 4 版为所有文件建立[逐文件验证表](../../FILE_WIRING.md)。生产文件、配置、报告、构建及离线工具逐一指到一个有权验收的工程用例；测试入口由其源码及正常执行回执单独核对。既有功能行为符合合同时直接运行回归，保留源码。已有替代入口的历史文件，登记原路径并运行实际替代件。静态常量在使用它的正常调用者处观察，构建/配置文件由读取它的构建步骤观察，审计专用组件按只读职责签收。后续批次改到共享行为时，复跑受影响的已绑定用例。每批可在自己的工程输入下签收。

执行原件增加 `owner_probe_results`，每行含 `key, contract_sha256, positive_metric, negative_metric`。合同从验收矩阵的 `inherited_path_probes` 读取，字段沿用同格式覆盖全部文件验证行。正反指标引用在源码中登记的 oracle，测量行使用 `direction_key, contract_sha256, polarity, case_id, source_commit`。指针必须指向实际业务值。正例另写 `effective_path, effective_blob_sha, business_id, producer, consumer, payload_artifact`。上下游引用的格式为 `{"artifact":"events.json","json_pointer":"/events/0/event_id"}`。两端分别保留不同的 event_id，同一非空业务键、用例及提交，至少一端带有效文件及其当前 Git blob。输出与输入各自记录 `output_digest`、`input_digest`，均应等于本次实际传递内容的 SHA-256。将原始载荷和观测文件全部列入回执 artifacts。可批量从同一次真实运行提取行；重复使用同一观察冒充多个文件或拿文件名当测量会被拒绝。源码里运行不到的必需路径须接回应用，旧路若有替代则验证权威路径及隔离行为。未获证时让 REAL 保持禁止，工程夹具可在隔离安装中验证具备授权时的主链。
