R29.1 实施中，尚未达到 ENGINEERING_READY，未签收任何完整批次。

应用分支 construction/r29-20261029，从 f9bdddb97291d9d96b5aba940b31d03369c220e2 延续；方案固定 96fb4355748a2f28e31927e1f9cb8cb61fac6547。原应用分支与全部原资产保留。CURRENT_FINAL_PLAN.md 及方案目录/ZIP保持方案原字节。

先完成 B00—B02 的指标、独立研究写者、迁移和冻结结算基础。52卡及44指标逐项保存在本目录账本；未验证不记 PASS。已发现缺陷：迟到结果永久 hit=-2、乱序资格漏计、研究写批次拆分事务、冻结安全边界不足。

交付仅为可复核的施工检查点，不代表全部52卡完成、真机验证或自然命中率达标。安装/启动/覆盖升级和设备长测以对应回执为准；没有回执时为 NOT_RUN。未产生足够自然证据时 QUALITY_NOT_MET / REAL_BLOCKED。

签名：优先使用仓库既有授权 R13 签名传输缓存。缓存不可用时只允许复用最新 R27 已使用的固定 CI 签名，不生成新密钥；这种包不能覆盖正式 R13，明确 BLOCKED_SIGNING_KEY。禁止以换签或清数据解决升级。

恢复命令：git clone --branch construction/r29-20261029 https://github.com/gtt46461-star/starcore-public-build.git && cd starcore-public-build

构建：见 .github/workflows/r29.yml。只有已提交源码才允许构建交付；APK与源码来源、签名、文件hash在 delivery.json 对应。
