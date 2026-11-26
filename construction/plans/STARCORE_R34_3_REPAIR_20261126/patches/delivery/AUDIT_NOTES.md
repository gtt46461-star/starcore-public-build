# 173 现场修复的覆盖安装、工程证据、打包与发布闭合补丁

基线：`4d1de9d83692635b5c5ce39144fece1c8a31c8ac`，已安装 versionCode `577311173`。本组依赖 `field_binding_patch`。**代码必须在 B01 建立 field pin 时一并安装；B12 做最终交付复验。APK 系列继续使用 R34.2，计划身份是额外的 fieldRepair173。**

## 1. 确定断点与改动

| 文件 | 已安装代码的问题 | 精确修改后的行为 |
|---|---|---|
| `tools/r342/ci_candidate_tests.py` | 覆盖安装只使用152 APK，baseline报告source固定为152提交；工程receipt只认旧R342案例 | 有新field身份时选173 APK与173源码，保留UID/实际安装hash检验；新case通过独立driver执行，旧回归保持单列；receipt附完整field身份和新证据引用 |
| `tools/r33/run_release_build.py` | 构建receipt没有新field身份 | 原构建前后完整identity不变校验保留，receipt另保留原样 `fieldRepair173` |
| `tools/r33/package_ci_candidate.py` | 强制152当前安装对照；只转存r342身份；新field证据可缺失 | 新field要求173准确APK/同签名更高版本、新工程原件；逐case验证runner/input/output原件；DELIVERY保留完整field身份，APK名仍R342 |
| `tools/r342/release_delivery.py` | 发布只检查旧SEAL/旧工程案例，总包固定baseline152名字 | 从已检出的源码重新验证field pin/plan；重新核验新工程原件与173覆盖；包成员改为baseline173；新plan作为公开index当前plan，旧R342封印另存 `legacyR342Plan` |
| `tools/r341/publish_delivery_manifest.py` | 下载复验结果丢掉field身份 | 原字节下载/重读不变，新field身份、工程证据引用、旧封印身份一并传递 |
| `.github/workflows/build-apk.yml` | 调用者仍传baseline152；只接受旧施工源ref；发布checkout缺新owner/pin/plan | 新field打包传173；新增固定授权施工ref `construction/r343-repair-20261126`；发布检出新owner、release pin、完整新plan，读取精确plan commit；旧无pin流程和其他jobs不变 |
| `tools/r343/field_delivery_contract.py` | 无新工程证据机器门禁 | 同一纯验证器被CI、打包、发布三处消费；缺字段、错源、旧案例、空断言、丢原件都拒绝 |
| `tools/r343/run_field_engineering.py` | 新计划列出案例不代表真正执行 | 执行本批已提交runner清单，保留真实命令/输入/输出，收到实际断言完成事件才写新receipt |
| `tools/r343/test_field_delivery_contract.py` | 缺少上述边界负例 | 19组主机门禁测试，含真实启动合成Python进程；不执行Android或业务策略 |

原 `r342` 身份仍指向历史152基线及旧封印，这是继承记录；当前修复以 `fieldRepair173` 的173源、计划commit/manifest、报告hash、当前source/tree/batch/APK为准。不能覆盖、重写旧封印伪装新结果。APK内已有完整 `build-identity-r334.json` 会保存整个identity对象，`package.embedded()` 对完整对象逐值比较，因此field不能在嵌入时被丢掉；不新增一个错误命名的R34.3 APK系列。

## 2. 实际新矩阵与本批runner清单

已读取新方案实际 `ACCEPTANCE_MATRIX.json`：138条，132工程、4设备、2自然前瞻。工程批次数是 B01=9、B02=18、B03=15、B04=18、B05=15、B06=3、B07=9、B08=9、B09=6、B10=21、B11=6、B12=3。运行时按封印矩阵和当前batch动态取精确集合，不硬编码此计数替代源校验。

每批施工必须提交 `release/field173_test_runners.json`，字段完全如下：

```json
{
  "schema": "STARCORE_FIELD173_TEST_RUNNERS_1",
  "batch": "B01",
  "cases": {
    "R343-001-POS": {
      "argv": ["python3", "-B", "tools/r343/cases/r343_001_pos.py"],
      "inputFiles": ["tools/r343/cases/fixtures/r343_001.json"],
      "timeoutSeconds": 120
    }
  }
}
```

上例仅解释schema，不是可通过的B01清单：实际必须有B01的全部9个ID，其他批次必须匹配自己的完整工程集合。`argv` 是参数数组，直接执行，不经过shell；允许调用已经实现的Gradle/JUnit/native wrapper，但wrapper必须在原生产Owner上验证本卡断言，再输出下面的事件。不得绑定到只打印PASS、只检查helper或只检查文件存在的程序。`inputFiles` 至少1个、最多64个，全部是当前source commit的真实tracked输入；每个输入、日志、输出原件最大16MiB。输入文件从Git blob按commit校验并另存原件。

**本审计没有生成132条假测试或占位PASS runner。实际生产测试由各问题对应施工卡实现/复用，必须先进入相同source commit；缺runner清单、缺case、错case都阻止工程APK发布。主机合约fixture不允许登记为这些生产案例。**

## 3. 真实runner接口

driver通过以下三个环境变量交付实际输入，runner不得自己填造身份：

- `STARCORE_FIELD173_CASE_CONTEXT`：本次case输入JSON绝对路径。
- `STARCORE_FIELD173_CANDIDATE_APK`：本次真实release APK路径。
- `STARCORE_FIELD173_CASE_OUTPUT`：runner需写入的输出清单绝对路径。

context的字段为：`schema=STARCORE_FIELD173_CASE_CONTEXT_1`、`caseId`、`sourceCommit`、`sourceTree`、`planCommit`、`planSha256`、`batch`、`apkSha256`、`buildRun`、随机 `executionId` 和 `inputHashes`。执行者按真实测试路径把这些字段传入native runner；重试必须新建原件目录，不能读取上次遗留成功日志。

stdout中必须出现且仅出现两个同identity事件，顺序固定。每行前缀精确为 `FIELD173_ENGINEERING_EVENT `，后面JSON包含context中的身份字段及：

1. `event=CASE_ENTER`，仅表示真实测试进入。
2. `event=CASE_ASSERTIONS_COMPLETE,status=PASS,assertionCount=<实际成功执行的断言数>`。实际断言数必须为整数，且至少等于当前case在封印矩阵中的assertions数量。断言失败或异常必须返回非0，不得发完成事件。

driver不会替runner生成这些事件；只有进程退出0、两事件身份完全一致、执行断言数达到要求、全部输入/输出原件可回读，才形成case PASS。日志里的普通“PASS”、旧R342 marker、别的source/执行ID、0、小数或boolean计数都不能通过。

输出清单必须为下面的准确结构；身份值全部来自实际context：

```json
{
  "schema": "STARCORE_FIELD173_CASE_OUTPUT_1",
  "caseId": "R343-001-POS",
  "sourceCommit": "<context实际值>",
  "sourceTree": "<context实际值>",
  "planCommit": "<context实际值>",
  "planSha256": "<context实际值>",
  "batch": "B01",
  "apkSha256": "<context实际值>",
  "buildRun": "<context实际值>",
  "executionId": "<context实际值>",
  "dataScope": {
    "role": "ENGINEERING",
    "target": null,
    "cutoff": null,
    "databaseIdentity": null,
    "notApplicableReason": "这个例子是静态身份验证，没有数据库和开奖target；生产case应填实际域。"
  },
  "artifacts": [
    {
      "file": "field173-R343-001-POS-observed.json",
      "kind": "OUTPUT",
      "sha256": "<真实原件的SHA256>"
    }
  ]
}
```

原件置于输出清单相同目录，使用该case前缀的平面文件名。实际数据库案例必须填真实database identity，并至少有一个 `kind=READBACK` 的数据库/Owner结果读回原件；无数据库案例可使用OUTPUT。target/cutoff/database任意不适用时必须给明确原因，不能默认为0冒充真实数据。输出原件至少一个、不能为空；输出清单、每个原件和输入都由门禁再计算hash。

## 4. 三层签收与调用次序

1. `verify_plan_binding` 先得到当前完整field身份；`run_release_build` 把同一个对象原样附到真实CI构建receipt。
2. `ci_candidate_tests` 真实安装173→当前APK，检查实际包hash/version/UID。B12的旧baseline报告source也改成173准确commit。旧152仍在历史兼容回归里保留。
3. 旧R342回归照常完成后，driver执行当前新矩阵工程case；输出 `field173-engineering-receipt.json`。外层 `batch-receipt.json` 仍保存旧工程口径，另加完整field对象与 `fieldEngineeringEvidence`。新receipt列明全部6项DEVICE/NATURAL为 `NOT_RUN_BY_ENGINEERING_DRIVER`。
4. `package_ci_candidate` 拒绝缺新receipt、runner原件、输入原件、输出读回、准确173覆盖或嵌入身份；总包保留新字段以及baseline173证书/包信息。
5. `release_delivery` 从源重新验证新pin和plan，逐项重验全部新原件；public index当前plan指新field，旧R342封印留在 `legacyR342Plan`。公开下载后再次检查包内相同原件和身份，最终 `DELIVERY_R342.json` 保留field信息。

供施工使用的直接入口（生产测试清单已提交且同批build已完成时）：

```bash
python3 -B tools/r343/run_field_engineering.py \
  --repo "$PWD" \
  --out "$RUNNER_TEMP/engineering" \
  --build-receipt "$RUNNER_TEMP/build/CI_BUILD_RECEIPT.json" \
  --apk /absolute/path/to/current-release.apk
```

正式CI已由修改后的 `ci_candidate_tests.py` 自动调用，不能在同一原件目录再执行一次。所有新流程仍保留 `physicalDevice=NOT_INFERRED`、`naturalQuality=NOT_PROVEN`，工程APK发布不等于软件全部验收。

## 5. 验证及剩余真实施工义务

已通过：8个Python文件语法；9个补丁文件的 `git apply --cached --check`；工作流YAML解析且非r342构建/发布jobs逐对象不变；19组主机fixture测试；新矩阵132工程与6独立现场条目精确读取。fixture真实启动三个Python子进程检验协议，但数据、候选字节、日志和Git仓库都是明确的合成fixture。

未运行：Android编译/instrumentation、真实173 APK覆盖、132个生产case、vivo实机长稳、自然前瞻质量、公网发布与下载。当前不能填这些项目的通过原件。各问题的实际生产测试不是本合约测试的替代品。

`field_binding_patch.prepare_field_batch` 必须同时更新原4文件版本组，而非仅release_manifest；应复用既有原子版本事务，并以前次已知最高code为下界。此项由field_binding组修复，本组不重写其文件。

完整after文件在 `after/`，精确diff在 `delivery.diff`，文件哈希在 `manifest.json`，本轮验证范围与日志哈希在 `verification/verification.json`。不修改共享173 baseline，不上传APK，不改变真实出手开关。
