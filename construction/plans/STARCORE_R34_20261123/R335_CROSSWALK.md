# R33.5原66工单承接

所有项保留原问题、测试入口和材料hash。字节相同/不同仅是来源比对；当前功能必须重新验收。每批的语义身份、旧32复核要求继续执行。

|旧ID|原问题|R34责任卡|继承合同|
|---|---|---|---|
|R335-D01|自动施工只涵盖关键差异，完整新增/替换文件未统一交接|[R34-B00-02](batches/B00/issues/R34-B00-02.md)、[R34-B12-03](batches/B12/issues/R34-B12-03.md)|细项：新增/替换/保留完整材料清单与后态hash，非仅关键patch|
|R335-D02|签名预检所需公钥证书环境项漏配|[R34-B00-05](batches/B00/issues/R34-B00-05.md)、[R34-B12-01](batches/B12/issues/R34-B12-01.md)|明确：公钥指纹与所需构建环境项逐个预检|
|R335-D03|新方案身份仍硬编码R33.4，跨文件修订易漂移|[R34-B00-03](batches/B00/issues/R34-B00-03.md)|明确：多文件plan身份同源生成|
|R335-D04|发布身份只有修改指令，没有完整一致后态|[R34-B00-03](batches/B00/issues/R34-B00-03.md)|IR13：完整一致后态且避免commit自引用|
|R335-D08|历史宿主收据不能代表本次代码且完整构建环境缺项未逐项列出|[R34-B00-05](batches/B00/issues/R34-B00-05.md)、[R34-B12-03](batches/B12/issues/R34-B12-03.md)|明确：当前源码当前环境，不借旧宿主收据|
|R335-D09|并行宿主测试共享可写工作树可互相覆盖或回退新代码|[R34-B00-05](batches/B00/issues/R34-B00-05.md)、[R34-B12-03](batches/B12/issues/R34-B12-03.md)|细项：每测试独立可写目录，禁止共享树被测试改写|
|R335-D13|新增源代码未接原r967封闭依赖和r82失效证明，预构建会拒绝或错误复用旧语义身份|[R34-B00-04](batches/B00/issues/R34-B00-04.md)|明确且每批：新增数值依赖/失效证明|
|R335-D14|preBuild读取033冻结调用索引导致新源码必报过期，freeze又依赖编译且修改构建输入|[R34-B00-03](batches/B00/issues/R34-B00-03.md)、[R34-B00-04](batches/B00/issues/R34-B00-04.md)|细项：preBuild/compile/index freeze DAG无循环|
|R335-ID-P00|本阶段数值依赖身份与旧训练状态精确失效|[R34-B00-04](batches/B00/issues/R34-B00-04.md)|每批重算数值依赖，不在B00签完后忽略|
|R335-C06|承接真实报告ZIP验收、可取消quick_check与有界取消锁|[R34-B05-04](batches/B05/issues/R34-B05-04.md)|细项：真实ZIP验收、quick_check可取消、取消锁有界|
|R335-ID-B01|本阶段数值依赖身份与旧训练状态精确失效|[R34-B00-04](batches/B00/issues/R34-B00-04.md)、[R34-B01-01](batches/B01/issues/R34-B01-01.md)、[R34-B01-02](batches/B01/issues/R34-B01-02.md)、[R34-B01-03](batches/B01/issues/R34-B01-03.md)、[R34-B01-04](batches/B01/issues/R34-B01-04.md)、[R34-B01-05](batches/B01/issues/R34-B01-05.md)、[R34-B01-06](batches/B01/issues/R34-B01-06.md)|每批失效证明与旧状态保护|
|R335-RT01|资源等待被计作功能失败，30次后终止原导出作业|[R34-B01-02](batches/B01/issues/R34-B01-02.md)、[R34-B05-04](batches/B05/issues/R34-B05-04.md)|细项：同report job资源等待100次不耗功能失败上限|
|R335-RT02|同一次资源准入拒绝原票据无耐久可读观察|[R34-B01-05](batches/B01/issues/R34-B01-05.md)、[R34-B05-04](batches/B05/issues/R34-B05-04.md)|细项：准入拒绝的原票据耐久可读，不能只进内存/日志|
|R335-RT03|故障归档仅有 Worker 接口，保护债无物理退役通路|[R34-B05-03](batches/B05/issues/R34-B05-03.md)|细项：故障原件真实归档/物理退役/恢复校验，不只是Worker接口|
|R335-RT04|缺失checkpoint仅有合同，无原身份恢复调度/耐久状态|[R34-B05-02](batches/B05/issues/R34-B05-02.md)|明确：按原hash的恢复任务和耐久状态|
|R335-RT10|r926逐份原件的完整外部保全、fresh隔离恢复、可撤销单件退役与灾难导入闭环|[R34-B05-03](batches/B05/issues/R34-B05-03.md)、[R34-B05-05](batches/B05/issues/R34-B05-05.md)|细项：fresh隔离恢复、逐件撤销退役、灾难导入闭包|
|R335-C01|承接原服务独立排债、启动分离和真实耐久时点修复|[R34-B01-01](batches/B01/issues/R34-B01-01.md)、[R34-B01-06](batches/B01/issues/R34-B01-06.md)、[R34-B03-01](batches/B03/issues/R34-B03-01.md)、[R34-B04-06](batches/B04/issues/R34-B04-06.md)|明确：独立排债、启动分离、耐久时点|
|R335-ID-B02|本阶段数值依赖身份与旧训练状态精确失效|[R34-B00-04](batches/B00/issues/R34-B00-04.md)、[R34-B02-01](batches/B02/issues/R34-B02-01.md)、[R34-B02-02](batches/B02/issues/R34-B02-02.md)、[R34-B02-03](batches/B02/issues/R34-B02-03.md)、[R34-B02-04](batches/B02/issues/R34-B02-04.md)、[R34-B02-05](batches/B02/issues/R34-B02-05.md)、[R34-B03-01](batches/B03/issues/R34-B03-01.md)、[R34-B03-02](batches/B03/issues/R34-B03-02.md)、[R34-B03-03](batches/B03/issues/R34-B03-03.md)、[R34-B03-04](batches/B03/issues/R34-B03-04.md)、[R34-B03-05](batches/B03/issues/R34-B03-05.md)、[R34-B04-01](batches/B04/issues/R34-B04-01.md)、[R34-B04-02](batches/B04/issues/R34-B04-02.md)、[R34-B04-03](batches/B04/issues/R34-B04-03.md)、[R34-B04-04](batches/B04/issues/R34-B04-04.md)、[R34-B04-05](batches/B04/issues/R34-B04-05.md)、[R34-B04-06](batches/B04/issues/R34-B04-06.md)|每批语义身份|
|R335-P01|完整结果codec缺失，快照不能恢复原canonical bundle|[R34-B03-01](batches/B03/issues/R34-B03-01.md)|明确：完整canonical codec，不遗漏字段|
|R335-P02|提交失败后的原结果没有同身份耐久缓存及精确恢复|[R34-B03-01](batches/B03/issues/R34-B03-01.md)、[R34-B01-05](batches/B01/issues/R34-B01-05.md)|明确：同身份耐久结果复用|
|R335-P03|canonical事务异常包装丢失原始可重试原因|[R34-B01-02](batches/B01/issues/R34-B01-02.md)、[R34-B01-05](batches/B01/issues/R34-B01-05.md)|明确：保留结构化cause链/阶段/可重试类别|
|R335-RE21|全主链真实启用窗口缺少独立原件，成功冻结不能充当机会分母|[R34-B04-01](batches/B04/issues/R34-B04-01.md)|IR04：启用机会不可由成功冻结反推|
|R335-RT05|补丁修复了未安装SQL，实际原生冻结反馈漏检固定deadline|[R34-B04-03](batches/B04/issues/R34-B04-03.md)|明确：实际原生反馈也检固定deadline，非只测未安装SQL|
|R335-RT06|无预测开奖被结算任务JOIN挡住；补扫/消费者首批毒记录饥饿|[R34-B04-04](batches/B04/issues/R34-B04-04.md)、[R34-B03-05](batches/B03/issues/R34-B03-05.md)、[R34-B04-03](batches/B04/issues/R34-B04-03.md)|明确：JOIN空集仍终态，毒首记录不挡后续|
|R335-RT07|最小结算反馈rows=0被当成功；newest-first让旧issue长期不重试|[R34-B04-05](batches/B04/issues/R34-B04-05.md)、[R34-B03-05](batches/B03/issues/R34-B03-05.md)|IR03：rows=0有可说明receipt；旧期公平重试|
|R335-RT08|热开奖淘汰未保护原生反馈未完成输入|[R34-B05-01](batches/B05/issues/R34-B05-01.md)|明确：淘汰热draw先保原生未消费反馈输入|
|R335-C02|承接可取消历史回放、输入pin、ABI隔离与独立出生|[R34-B05-01](batches/B05/issues/R34-B05-01.md)、[R34-B06-01](batches/B06/issues/R34-B06-01.md)、[R34-B07-04](batches/B07/issues/R34-B07-04.md)|细项：可取消历史回放与输入pin回收，中断后同游标|
|R335-ID-B03|本阶段数值依赖身份与旧训练状态精确失效|[R34-B00-04](batches/B00/issues/R34-B00-04.md)、[R34-B06-01](batches/B06/issues/R34-B06-01.md)、[R34-B06-02](batches/B06/issues/R34-B06-02.md)、[R34-B06-03](batches/B06/issues/R34-B06-03.md)、[R34-B06-04](batches/B06/issues/R34-B06-04.md)、[R34-B06-05](batches/B06/issues/R34-B06-05.md)|每批语义身份|
|R335-C03|承接训练计数、完整十位置模型发布与消费者契约|[R34-B07-01](batches/B07/issues/R34-B07-01.md)、[R34-B07-02](batches/B07/issues/R34-B07-02.md)、[R34-B07-03](batches/B07/issues/R34-B07-03.md)|明确：训练计数、完整十位置发布、精确消费者|
|R335-ID-B04|本阶段数值依赖身份与旧训练状态精确失效|[R34-B00-04](batches/B00/issues/R34-B00-04.md)、[R34-B07-01](batches/B07/issues/R34-B07-01.md)、[R34-B07-02](batches/B07/issues/R34-B07-02.md)、[R34-B07-03](batches/B07/issues/R34-B07-03.md)、[R34-B07-04](batches/B07/issues/R34-B07-04.md)、[R34-B07-05](batches/B07/issues/R34-B07-05.md)|每批语义身份|
|R335-RE16|正确hash的损坏模型游标可恢复，consumed与真实增量不一致|[R34-B07-02](batches/B07/issues/R34-B07-02.md)|明确：正确hash不保证合法游标；distinct consumed要复算|
|R335-C04|承接正式研究schema、纯策略边界与配置回执|[R34-B01-03](batches/B01/issues/R34-B01-03.md)、[R34-B06-05](batches/B06/issues/R34-B06-05.md)、[R34-B11-04](batches/B11/issues/R34-B11-04.md)|细项：正式schema、纯策略边界、配置ACK与出生分离|
|R335-C05|承接原S4检查点结构与S5框架，逐缺口补齐原始证据|[R34-B08-01](batches/B08/issues/R34-B08-01.md)、[R34-B11-02](batches/B11/issues/R34-B11-02.md)|IR10：S4检查点和S5真实处理保留，非统称新创造|
|R335-ID-B05|本阶段数值依赖身份与旧训练状态精确失效|[R34-B00-04](batches/B00/issues/R34-B00-04.md)、[R34-B08-01](batches/B08/issues/R34-B08-01.md)、[R34-B08-02](batches/B08/issues/R34-B08-02.md)、[R34-B08-03](batches/B08/issues/R34-B08-03.md)、[R34-B08-04](batches/B08/issues/R34-B08-04.md)、[R34-B08-05](batches/B08/issues/R34-B08-05.md)、[R34-B11-01](batches/B11/issues/R34-B11-01.md)、[R34-B11-02](batches/B11/issues/R34-B11-02.md)、[R34-B11-03](batches/B11/issues/R34-B11-03.md)、[R34-B11-04](batches/B11/issues/R34-B11-04.md)、[R34-B11-05](batches/B11/issues/R34-B11-05.md)、[R34-B11-06](batches/B11/issues/R34-B11-06.md)|每批语义身份|
|R335-RE01|固定前8策略造成后续学生饥饿及坏学生阻断源独立研究|[R34-B03-05](batches/B03/issues/R34-B03-05.md)、[R34-B06-04](batches/B06/issues/R34-B06-04.md)|明确：>8学生公平轮转，坏学生不挡SOURCE_ONLY|
|R335-RE02|研究视图/矩阵固定首项循环与无输入变化反复写源|[R34-B03-05](batches/B03/issues/R34-B03-05.md)、[R34-B11-02](batches/B11/issues/R34-B11-02.md)|明确：轮转及同输入不反复写帧|
|R335-RE03|时间先后矩阵ONLINE原件被标成SELECT|[R34-B07-04](batches/B07/issues/R34-B07-04.md)、[R34-B11-02](batches/B11/issues/R34-B11-02.md)|细项：TEMPORAL_PRECEDENCE的ONLINE原件不得改SELECT|
|R335-RE04|R01/R04有原件而结构和交集生成器没有实际消费|[R34-B06-04](batches/B06/issues/R34-B06-04.md)、[R34-B11-01](batches/B11/issues/R34-B11-01.md)|IR02：R01/R04实际结构/交集生成器消费在B06先实现|
|R335-RE05|R05失败处方仅有描述无法定位可替换叶|[R34-B08-01](batches/B08/issues/R34-B08-01.md)|明确：失败处方定位实际可替换AST叶|
|R335-RE06|R07无加法父策略和R12互补图没有实际融合fallback|[R34-B08-03](batches/B08/issues/R34-B08-03.md)|细项：无加法父/无可融合父时fallback及明确WAIT，不假融合|
|R335-RE07|R08机制多样性和R09专家记数未改变实际份额|[R34-B06-03](batches/B06/issues/R34-B06-03.md)、[R34-B08-05](batches/B08/issues/R34-B08-05.md)|细项：R08/R09实际改变份额，测试从申请到owner应用读回|
|R335-RE08|R10理论权重未接原进化owner且回执可能没有真实读回|[R34-B08-02](batches/B08/issues/R34-B08-02.md)、[R34-B08-05](batches/B08/issues/R34-B08-05.md)、[R34-B01-05](batches/B01/issues/R34-B01-05.md)|明确方向：R10权重接实际进化owner且耐久读回|
|R335-RE09|R13分箱缺原父身份与原ACT事件绑定|[R34-B07-05](batches/B07/issues/R34-B07-05.md)、[R34-B09-01](batches/B09/issues/R34-B09-01.md)、[R34-B09-02](batches/B09/issues/R34-B09-02.md)、[R34-B09-03](batches/B09/issues/R34-B09-03.md)、[R34-B09-04](batches/B09/issues/R34-B09-04.md)、[R34-B09-05](batches/B09/issues/R34-B09-05.md)|细项：R13分箱绑定原父身份、原ACT事件和样本role|
|R335-RE10|R15输入漂移报警无法进入真实预登记在线比较|[R34-B09-04](batches/B09/issues/R34-B09-04.md)|细项：R15不仅报警/暂停，还生成真实预登记在线比较|
|R335-RE11|R16未接资源owner、CPU重复溢出与回执误报请求量子|[R34-B08-05](batches/B08/issues/R34-B08-05.md)|细项：R16实际CPU增量、溢出/重复防护、请求量子≠已应用量子|
|R335-RE12|R17负贡献方向若错用会跳过较好的移除臂|[R34-B08-02](batches/B08/issues/R34-B08-02.md)、[R34-B08-04](batches/B08/issues/R34-B08-04.md)|细项：负贡献方向用更好移除臂正反例固定符号|
|R335-RE13|同帧重算新时间和旧不可变原件混用|[R34-B11-01](batches/B11/issues/R34-B11-01.md)、[R34-B11-02](batches/B11/issues/R34-B11-02.md)|细项：同frame重读保留原createdAt，不混新时间与旧不可变原件|
|R335-RE14|18图真实入口未核原机器帧，旧缓存可冒充当前|[R34-B11-01](batches/B11/issues/R34-B11-01.md)、[R34-B11-03](batches/B11/issues/R34-B11-03.md)|明确：核真实机器帧、hash/ABI/source，不借旧缓存|
|R335-RE15|16逻辑表满页误判hasMore、过滤串页和后台取消不恢复|[R34-B11-03](batches/B11/issues/R34-B11-03.md)|细项：n=pageSize与n=pageSize+1正确hasMore；取消后resume|
|R335-RE17|有效原子评分集变空后内存仍保留旧权重|[R34-B07-03](batches/B07/issues/R34-B07-03.md)、[R34-B08-04](batches/B08/issues/R34-B08-04.md)|细项：有效原子集变空必须清内存旧权重/版本缓存|
|R335-RE18|出生消费与非出生配置回执未分列显示|[R34-B06-05](batches/B06/issues/R34-B06-05.md)、[R34-B11-01](batches/B11/issues/R34-B11-01.md)、[R34-B11-03](batches/B11/issues/R34-B11-03.md)|细项：出生ACK、配置ACK、只读分析三种计数分别显示|
|R335-RE20|原13项仅METHOD_DESIGN_ONLY与报告索引覆盖失真|[R34-B11-01](batches/B11/issues/R34-B11-01.md)、[R34-B11-04](batches/B11/issues/R34-B11-04.md)|明确方向：METHOD_DESIGN_ONLY不得当生产/运行通过|
|R335-RE22|实际CONTMAT成功refit原件与持久计数缺失，旧期数估算被误当执行证据|[R34-B07-02](batches/B07/issues/R34-B07-02.md)、[R34-B11-02](batches/B11/issues/R34-B11-02.md)、[R34-B11-05](batches/B11/issues/R34-B11-05.md)|细项：CONTMAT实际成功refit原件与耐久计数，非期数估算|
|R335-RE23|S5缺父/满槽后漏续收、无真实独立ACK与同案耐久恢复链|[R34-B03-05](batches/B03/issues/R34-B03-05.md)、[R34-B06-05](batches/B06/issues/R34-B06-05.md)、[R34-B08-01](batches/B08/issues/R34-B08-01.md)|IR10：S5缺父/满槽等待续收、独立真实ACK、同案恢复|
|R335-RE24|诊断未采实际配对、算子尝试、原子教师、拒因与存储水位，快照截断还丢S5|[R34-B11-05](batches/B11/issues/R34-B11-05.md)、[R34-B11-06](batches/B11/issues/R34-B11-06.md)|细项：配对/算子/教师/存储/拒因/S5不被报告截断丢失|
|R335-RE25|CONTMAT旧/新ABI接受非法环、计数与verified状态，恢复后真实崩溃|[R34-B07-02](batches/B07/issues/R34-B07-02.md)、[R34-B11-02](batches/B11/issues/R34-B11-02.md)|细项：CONTMAT旧/新ABI非法环/计数/verified状态必须拒绝|
|R335-RE19|C11逐文件已有安全边界与待验项被混为待重写|[R34-B09-01](batches/B09/issues/R34-B09-01.md)、[R34-B09-02](batches/B09/issues/R34-B09-02.md)、[R34-B09-03](batches/B09/issues/R34-B09-03.md)、[R34-B09-04](batches/B09/issues/R34-B09-04.md)、[R34-B09-05](batches/B09/issues/R34-B09-05.md)、[R34-B10-01](batches/B10/issues/R34-B10-01.md)、[R34-B10-02](batches/B10/issues/R34-B10-02.md)、[R34-B10-03](batches/B10/issues/R34-B10-03.md)、[R34-B10-04](batches/B10/issues/R34-B10-04.md)|明确：保留现有资格安全边界，不为NO_BET硬改阈值|
|R335-D10|诊断把调用者布尔值和相等字符串当作已执行隔离复演|[R34-B11-05](batches/B11/issues/R34-B11-05.md)|明确：caller boolean/同字符串不构成隔离复演证明|
|R335-D11|报告案例→真实施工差异→下份报告缺可执行索引和证据闭包|[R34-B11-06](batches/B11/issues/R34-B11-06.md)|明确：原案例→源码差异→下一报告索引|
|R335-D12|旧模块数值复演与主链K3诊断归一化漂移且缺独立可执行入口|[R34-B07-05](batches/B07/issues/R34-B07-05.md)、[R34-B11-05](batches/B11/issues/R34-B11-05.md)|细项：ModuleReplay与K3诊断相同归一化/原值独立入口|
|R335-ID-B07|本阶段数值依赖身份与旧训练状态精确失效|[R34-B00-04](batches/B00/issues/R34-B00-04.md)、[R34-B11-01](batches/B11/issues/R34-B11-01.md)、[R34-B11-02](batches/B11/issues/R34-B11-02.md)、[R34-B11-03](batches/B11/issues/R34-B11-03.md)、[R34-B11-04](batches/B11/issues/R34-B11-04.md)、[R34-B11-05](batches/B11/issues/R34-B11-05.md)、[R34-B11-06](batches/B11/issues/R34-B11-06.md)|每批语义身份|
|R335-RT09|真实冷热原件和恢复状态在后续报告必须可见；不能把构造trace当根因复现|[R34-B05-02](batches/B05/issues/R34-B05-02.md)、[R34-B05-03](batches/B05/issues/R34-B05-03.md)、[R34-B11-04](batches/B11/issues/R34-B11-04.md)、[R34-B11-05](batches/B11/issues/R34-B11-05.md)|细项：实际冷热原件、恢复状态可见，构造trace不是现场复现|
|R335-C07|固定32组96断言288场景的同构建终审与反作弊|[R34-B12-03](batches/B12/issues/R34-B12-03.md)、[R34-B12-04](batches/B12/issues/R34-B12-04.md)|细项：保留原32组96断言288场景来源与同构建反作弊，可追加不可静默删减|
|R335-D05|源码ZIP不含Git历史，不能独立复原已有构建基线门|[R34-B00-05](batches/B00/issues/R34-B00-05.md)、[R34-B12-01](batches/B12/issues/R34-B12-01.md)|明确：源码交付含可恢复Git历史/基线闭包|
|R335-D06|独立主链验收漏核机会日历与冻结截止一致|[R34-B04-01](batches/B04/issues/R34-B04-01.md)、[R34-B04-03](batches/B04/issues/R34-B04-03.md)、[R34-B12-02](batches/B12/issues/R34-B12-02.md)|IR04：独立启用机会日历与固定截止一致|
|R335-D07|独立APK验证器“更高版本”比较未覆盖033基线|[R34-B00-03](batches/B00/issues/R34-B00-03.md)、[R34-B12-01](batches/B12/issues/R34-B12-01.md)|明确：APK版本高于实际095及远端最高，不只比033|
