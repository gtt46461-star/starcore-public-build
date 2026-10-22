# M6：Champion / Challenger / Paired Certificate

## 为什么当前一直0
存在两套不同层级证书：
- AlgorithmEvidencePolicyR912 / R915：算法Future N≥240等；
- ResearchCertificationEngineR811：更高研究证明，PROVEN要求N≥480、HR≥45%、Wilson≥40%、ECE≤4%等。
再叠加parity/PIT/holdout/FDR，且旧候选大量已PRUNED，所以很容易一直0。

## R925统一“证书梯子”
L0 IDEA
L1 NURSERY_EXECUTABLE
L2 OBSERVING_FUTURE_OOS
L3 EVIDENCE_READY
L4 CHALLENGER
L5 SHADOW_CERTIFIED
L6 PRODUCTION_INFLUENCE_CERTIFIED
L7 PRODUCTION_CHAMPION（仍不等于直接REAL授权）

### L3 EVIDENCE_READY
N≥120；没有基础设施缺口；PIT/parity基本通过。
不要求高胜率，只表示“有资格认真比较”。

### L4 CHALLENGER
建议：
N≥240
HR≥0.36
Wilson lower≥0.30
Brier≤0.24
ECE≤0.12
pairedN≥120
pairedDelta≥0.02
Regime/block/maxMiss/FDR通过
这与现R915保持兼容。

### L5 SHADOW_CERTIFIED
在“成为Challenger之后的全新窗口”再积累，例如≥120 Future issues：
- paired lower bound≥0
- 无校准/Regime退化
- no PIT/parity violation
然后才允许影子重排权重，如5%~12%。

### L6 PRODUCTION_INFLUENCE_CERTIFIED
再新增独立窗口，如≥240：
- paired superiority或预定义non-inferiority + risk improvement
- 资源稳定
- 贡献可解释
允许12%→20%→30%阶梯权重。

## 权重不能一步到30%
现R915 `0.12~0.30` 可保留上限，但R925改为“证书阶段+证据函数”：
- SHADOW_CERTIFIED：只记录，不改生产
- PROD_L1：5%
- PROD_L2：12%
- PROD_L3：20%
- PROD_MAX：30%
任何漂移/证书过期立即回0。

## 配对是核心
同一期同一个Champion、同一个Challenger：
- candidateHit - championHit
- candidateBrier - championBrier
- rank regret delta
按issue成对统计，避免两套模型覆盖期不同造成假提升。
