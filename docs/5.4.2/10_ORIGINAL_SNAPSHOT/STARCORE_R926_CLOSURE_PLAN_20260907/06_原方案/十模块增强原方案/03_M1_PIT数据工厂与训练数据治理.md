# M1：PIT数据工厂与训练数据治理

## 现状缺口
R924有严格OOS思想，但训练入口仍存在不同年代V49/V50/V51/R811数据路径并存。必须统一到一个生产数据合同。

## 新增/重构
### Python
- `training/r925_build_pit_dataset.py`
- `training/r925_dataset_contract.py`
- `training/r925_split_registry.py`
- `training/r925_dataset_audit.py`

### Java/Android
- `TrainingArtifactManifestR925.java`
- `PITBoundaryAuditR925.java`
- `FeatureSchemaFingerprintR925.java`

## 数据表
建议新建：
`v57_r925_training_snapshot`
字段：
snapshot_id, created_at, start_issue, end_issue, target_n,
source_hash, schema_hash, feature_hash, split_hash,
train_end_issue, tune_end_issue, calibration_end_issue,
policy_end_issue, sealed_start_issue, sealed_end_issue,
postdraw_feature_count, pit_violation_count, status.

## 必做规则
1. 每个target的所有特征必须记录 `max_source_issue < target_issue`。
2. 任何A9开奖后证据只允许出现在settlement/lifecycle表，不得进入赛前feature vector。
3. feature schema必须包含：
   - datatype
   - range
   - lag
   - window
   - missing policy
   - deterministic flag
   - PIT source
   - cost
   - version
4. 所有训练期号按时间顺序，禁止random issue shuffle。
5. 可以在训练集内部对1200动作行做批处理，但不能打乱issue边界做统计推断。
6. sealed final只能消费一次；一旦被读取，artifact trial必须封存，不允许继续调参。
7. Future Live从artifact冻结后的下一期开始计数。

## 数据质量门
- duplicated issue = 0
- invalid permutation = 0
- PIT violation = 0
- schema mismatch = 0
- target gap必须显式标记，不静默补行
- source hash、row count、issue count全部可复现

## 验收
- 同一源码+同一数据重复构建，dataset hash完全一致。
- 任意挑100个target，自动证明所有输入issue早于target。
- 删除/修改任意源行，hash变化且旧证书自动失效。
