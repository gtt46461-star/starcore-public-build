# 星核创研 SU1 工程交付

当前构建版本 `SU1-2026.10.22.3`，包名 `com.starcore.research.su1`，名称“星核创研 SU1”，使用独立星形图标，可与原软件共存。已签名研究 debug APK 保持 SU1 签名缓存，具体签名/兼容系统/CPU/校验值以包内报告为准。原软件的私有数据库、key、模型及资格不会跨包继承。

主接口 `https://api.duoqu.online/data`、备用 `https://api1.duoqu.online/data`、游戏代码 `Binance1-PK10` 均写在生产代码和 APK 内。服务另需有效访问 key：工作流从 Actions secret `DUOQU_API_KEY` 注入已有 BuildConfig/KeyProvider。设置页和研究页显示地址及真实凭据状态；没有 key 的构建可安装测试，但无法自动取数，不能声称开箱可用。最终安装包发布同时要求 Android 运行验证通过和凭据配置就绪；配置就绪时还实际请求最新开奖并验证格式。密钥不提交公开源码或打印日志。

源码分支 `construction/su1-20261022`，草稿 PR：https://github.com/gtt46461-star/starcore-public-build/pull/1 。公开基线 `f3e249ce3aef9a8975abe0be38cbe2d1183e4078`；方案私有基线 `9ec590cafbd7cbcd1265a11b63a59df53ba78fcd`。完整受控输入见 `release/strategy_su1_reviewed_inputs.json`，旧合同对账见 [compatibility-delivered.json](compatibility-delivered.json)。身份变化退休旧数值资格，保留证据和 alpha 账本，不迁移为新资格。

本轮实际生产实现详见 [CONTINUATION.md](CONTINUATION.md)。包括稀疏增量条件关系及持久审计、静态资源合同、九组参数 RELSTATE 的联合目标/BPTT、十八组参数 RELGRAPH 的角色超边注意力/BPTT/Adam、完整优化器续训及手机权重消费、训练门控融合、情境研究统筹、双父谱系与传递归档 pin、单一合格主线与祖先回退、五组持久历史对照、真实冻结/结算/缺失分母和研究界面。

验证分层：标准 Android 构建/单测/lint、原源码身份和变异自测、生产 Java/独立 Python/SQLite 数值合同，以及 Android 模拟器的 18 项迁移、结算、界面、权重/梯度和接口合同。每次的实际结果和被测 SHA 以 Actions 为准。CI 的完整 JDK17 运行原 Java8 对拍；本机精简 JDK21 缺少 ct.sym，不能将本机该项失败误报为通过。

V8 完整生产数值引擎历史对照使用 5000 行输入，搜索3560期，锁定冠军后再开1440期：真实438/1440（30.42%）、时间打乱429/1440（29.79%）、植入规律467/1440（32.43%）、禁用统筹421/1440（29.24%）、禁用研究记忆438/1440（30.42%）。每组88候选分配、相同阶段和切片上限，实际计算成本不同且原样报告。本轮未证明稳定预测优势或记忆增益。V8 使用后来拟合图模型导入扩展之前的独立已执行快照；[原始报告和字节码快照](validation/phone-controls-v8/provenance.json)单独固定，不能充当最终 APK 的自然前瞻回执。

[requirements-delivered.csv](requirements-delivered.csv)逐项记录100条要求：70项已交付且验收范围明确，22项仍为PARTIAL，6项代码就绪等待自然样本，2项等待实体设备或属于用户已授权的独立安装变体。**整个方案尚未完成全部验收，不声称100%完成。** PARTIAL 明确包括通用状态 DSL/结构搜索、所有关系删除/替换和模型记忆消融、图模型完整联合目标、低保真训练阶段、全面失败归因、持续数据库/文件资源合同、全部旧资产重测与所有新增算子的端到端 Python 一致性。实体手机持续负载、自然资格和卸载后跨沙箱的终身 alpha 账本不能由历史实验替代。

安装与使用见 [INSTALL.txt](INSTALL.txt)。主线默认关闭，真实未来验证、校准、对照和资源检查缺失时保持 WAIT，不自动提高权限或伪造收益。
