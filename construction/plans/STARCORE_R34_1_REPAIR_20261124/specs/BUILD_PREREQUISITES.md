# 可构建前置：三个方案根、真实Git祖先和原签名

这份预检是B01的一部分。环境缺项须具体登记；继续不受该缺项影响的工程，不删除旧校验来假装环境齐备。

## 1. R33.5原封存根

实际122源码的 `release/r33_plan_binding.json` 要求：

| 身份 | 固定摘要 |
|---|---|
| `verification/SHA256.json` | `eefe7128ed2a13065c95c31c2f424a42d3c5c3bc780fbc18ab0db1fcd6e0a43c` |
| `control/32_gate_contracts.json` | `37cd13d1e8e80bed606b6a6d40e28269e8983a9b99a218442ccba2ce6dad29da` |
| `control/required_scenarios_R334.json` | `018e2cb8541de672f7c1e99e97a39dab8db699ce4bf0577d637b4c8b10c35535` |
| 原构建源码祖先 | `bb7b49bbcc9266bad548ec37e71403256fc06a4d` |

已有完整原件时，调用现有 `tools/r33/verify_plan_binding.py` 中的 `verify_plan` 按原绑定核对整个成员集合及每项字节；仅3个摘要相同不能替代全成员校验。`STARCORE_R33_PLAN_ROOT`指向通过原验证的 `STARCORE_R33_5_20261118/`。

没有原件时，已核到的完整来源为仓库Release `r33.5-plan` 的 [33.5.zip](https://github.com/gtt46461-star/starcore-public-build/releases/download/r33.5-plan/33.5.zip)，626,717,930字节，SHA256 `01af8e98ae4d0917b7dd3abe4bff2c44ef8e47cd80136df635cbb0441d6b3a79`。取得后先核完整字节和摘要，再使用固定提交 `4e1606e9c20c5f01a82cf495b8d39b2630c766f0` 下的 `construction/plans/STARCORE_R33_5_REVIEW_20261119/prepare.py`：

```bash
python3 -B "$R335_REVIEW_ROOT/prepare.py" "$R335_ARCHIVE" "$NEW_PREPARED_ROOT"
```

该准备过程只在独立工作目录恢复原方案并核验第一卷差量，读其生成的START_HERE定位PLAN；本轮**不得**执行旧R33.5 stage安装器或把旧production/images覆盖122。恢复后的原方案按上表现存绑定完整校验。仓库 `STARCORE_R33_5_CONSTRUCTION_20261118/parts`只留历史7/75分片，不作为完整取件入口。

本次方案设计核对了Release元数据、当前取件说明和122构建校验源码，未重新下载626MB旧包，也不把这项元数据核对称为构建前置已验收。施工者实际取得/复用后必须保留全成员校验回执。

## 2. R34旧封存根

从固定提交 `4e1606e9c20c5f01a82cf495b8d39b2630c766f0` 取得 `construction/plans/STARCORE_R34_20261123/` 完整目录，放在独立只读位置，作为 `STARCORE_R34_PLAN_ROOT`。

现存 `release/r34_plan_binding.json` 的planCommit为上述提交，planSha256为 `6b8156c404fdcf13f9699ca888b1c44626d91c7c439130ec8de6137718e9ff05`，sourceBaseline为 `273641e838e0b8dd842b42a67a674e184942a30a`。原 `MANIFEST_SHA256.json` 和所有成员必须通过当前 `tools/r34/build_identity.py` 所要求的完整集合校验。

B01把历史封印验证与当前R34版本名校验拆开，使R34.1能保留旧封印而使用新的版本系列。旧绑定和旧目录不修改；若旧目录中的文件被施工污染，重新取得原字节并查明污染，不重算旧seal给篡改内容签字。

## 3. R34.1新根及应用身份

`STARCORE_R341_PLAN_ROOT`指向本目录完整封印副本；运行 `tools/verify_plan.py`。其descriptor与旧R33.5格式不同，不能互换。应用基线仍为122的 `5562f9b19aa09f9f7d9e69ca085db56c537967fc`。Git历史至少包含旧构建验证依赖的祖先和本轮准确源提交；source ZIP之外的可恢复bundle按B12提供。

原工具链沿用实际源码锁定的Java17、Android SDK/NDK/CMake、Gradle及依赖配置。B01读取Gradle和构建wrapper的真实要求并逐项预检，记录工具版本和缺项；不在此另列一套未经构建核验的新版本来替换它们。

原签名私钥、口令与API凭据仅从已有授权秘密环境注入。公开指纹、包名、版本及APK摘要可入回执，秘密值不可回显。缺原签名会影响正式覆盖交付，但不阻止继续可做的源码与测试工作。所有旧root、工具链和签名是否实际就绪，最终以B01本次执行原件为准。
