# M2：NumberQ + Expert Gate + 深度模型满血施工

## 目标
先得到真正有证据的10×10号码置换概率面，再让1200动作层有可靠输入。

## 当前结构
R924已有：
- 96→64→32→100离线TinyMLP + log-domain Sinkhorn；
- FTRL轻量在线研究；
- 多专家/特征/趋势/形态/关系等证据；
但Production NumberQ/Gate仍未证明。

## R925模型架构
### 1. Base experts（保留既有专家，不直接删除）
建议至少按现有资产归为：
- frequency/window expert
- gap/omission expert
- transition expert
- shape/motif expert
- relation/cross-position expert
- trend/lifecycle expert
- teacher/evidence expert
- FTRL adaptive expert
- TinyDeep/deep expert

### 2. Expert Gate
Gate输入必须是赛前状态特征，而不是结果：
- regime features
- drift/change-point
- expert disagreement
- recent calibration residual prior（只来自已结算过去期）
- sample support
- missingness
- window stability
输出：每个expert权重，权重归一化。

### 3. NumberQ
每个expert输出10×10 logits/probability；
Gate融合后经log-domain Sinkhorn得到双随机10×10 Q。
硬门：
- finite
- row sum≈1
- col sum≈1
- no negative
- deterministic parity

### 4. 深度候选
保留R811 96→64→32→100作为一个expert，不直接替代全部模型。
新增深度候选：
- D0：当前TinyMLP
- D1：残差MLP 96→96→48→100
- D2：双时间尺度MLP（fast/slow state）
- D3：专家蒸馏模型（teacher ensemble → compact student）
只有strict OOS非劣才保留。

## 训练
- expanding chronological folds >=5
- 每fold只训练更早数据
- Gate不能看到同fold标签统计
- hyperparameter selection只用inner folds
- sealed final只一次

## 指标
号码层：
- permutation cross entropy
- top1 number accuracy（分位置）
- top3 number inclusion
- Sinkhorn parity
- position calibration
- Regime分层
但注意：号码层好不等于K3最终好，最终仍由M3/M4证明。

## 在线FTRL/TinyDeep的角色
- 只做SHADOW sidecar/adaptive residual；
- 训练截止issue必须写入artifact；
- `trained_until < target`;
- 不允许同期开奖更新后回写本期；
- 在线更新不能自动获得生产权。

## 晋级
NumberQ Champion只有同时通过：
PIT + OOS + parity + finite + resource + no-regression
才写 `PROVEN=true`。
