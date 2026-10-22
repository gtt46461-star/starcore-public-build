# SU1 继续施工记录（2026-10-22）

本轮增加实际生产代码：类型/量纲/事实与估计合同、稀疏条件转移的增量更新和支持度审计；完整语法的静态资源清单；真实多项 BPTT 目标；冻结 RELSTATE10 参数的桌面导出、手机导入和推理消费；SGD 续训工件；所有数值层、九组梯度及更新的独立 NumPy 对拍；共同开发探针、错误互补筛选和门控/温度联合训练；按情境/成本更新的研究统筹和检索索引；冻结/结算/拒绝/覆盖/区间界面；单一发布主线及有证书祖先回退；活动谱系的传递 pin。

本机纯 Java 编译和数值合同通过。Android 编译、迁移、UI、完整源身份/lint 检查须以本轮 CI 的实际结果为准。主机的原始 Python Java8 对拍在该精简 JDK21 环境缺少 ct.sym，完整 CI 使用 JDK17执行原测试，不改变或跳过它。

## 接口打包

主接口 `https://api.duoqu.online/data`，备用 `https://api1.duoqu.online/data`，代码 `Binance1-PK10`，均在 BinanceGameContract 内置。服务还要求有效 API key。上一版 debug 构建未提供 DUOQU_API_KEY，因此虽然 URL 已内置，首次安装仍不能自动取数。此轮工作流从 Actions secret `DUOQU_API_KEY` 注入既有 BuildConfig/KeyProvider，不把密钥写入源码或日志。首次安装的 Android 合同检查编译配置与运行 readiness 一致，最终安装包发布必须同时通过 Android 运行检查且凭据非空。没有凭据时可继续编译测试，但不能标为无需填写即可使用的交付。

## 验收边界

本轮尚在施工；原 requirements-delivered.csv 的 PARTIAL 结论不会仅因类名增加而批量改为完成。进一步待补的工程项包括通用关系图模型、完整生产状态 DSL、活动控制组/消融、全部旧资产重测和持续资源合同。实体手机结果、自然前瞻资格及独立卸载后全局终身 alpha 保留，均不能用历史或模拟器结果替代。本记录不声称方案100%完成或具有真实预测优势。
