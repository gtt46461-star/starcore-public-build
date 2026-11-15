# P1 原主链保护与退休清单的当前合同回归

## 执行入口

```bash
python3 "$REPO/tools/r32/p1_contracts/run_selected_contracts.py" \
  --repo "$REPO" --classes "$P1_COMPILED_CLASSES" \
  --sdk "$ANDROID_JAR" --out "$NEW_EXTERNAL_RESULT_DIR"
```

classes必须由同一P1阶段全部生产Java编译得到。测试在该阶段classes上直接调用原 `R27QualityTest.h15MainlineRedLines`、原 `R31RetirementManifestTest` 三方法及新增两组篡改负例，共6个测试方法。它使用仓库的Test注解与失败即抛AssertionError的主机断言API，生产逻辑未被替换。缺失selector也失败。输出目录必须是新目录并置于源码工作树外。

本入口记录所有测试、断言适配器、实际DbStore、两份精确review、退休清单、runner和支持classes的SHA；运行期间任何字节变化均拒绝出PASS回执。这是原测试方法的主机执行，不是完整JUnit平台、Gradle或Android设备通过。

## 本次修复的范围

`verification/r32/p1_dbstore_review.json` 逐字固定5个已审DbStore差分片。H15先严格校验此review的固定SHA，再逆向应用5片，将当前DbStore恢复为7424基线的698116…源码，接着执行未放松的R31/R30归一化与原9edd…主链锚点检查。原主链全文件期望未改；未登记改动、修改被审谓词/列、重复片段、review篡改和片段外PnL改变都被拒绝。

`tools/r31/retirement_manifest.json` 由未修改的原 `retirement_inventory.py` 从纯P1源码重生成，仍不批准删除任何资产或方法。独立确认该生成结果与最终候选逐字相同，因此后批不需要再写这份P1清单。退休测试保留当前DbStore SHA与方法覆盖校验，并加强核对实际声明的方法名/顺序。旧SHA、缺方法、同数量重复方法、伪造删除授权、任意源字节改变均拒绝。

## 已观测证据

同一7424基线的旧H15与退休三例为4 PASS / 0 FAIL。仅应用P1而不更新合同后为2 PASS / 2 FAIL，准确拒绝未登记DbStore差分及旧退休SHA。修复上述测试合同后为6 PASS / 0 FAIL，使用纯P1的1309个生产Java编译classes，不依赖P2—P5代码。本次没有修改任何生产Java或降低数值资格门。

需要重跑基线对照时，可传两个 `--selector`：`com.ethk3.novaonepro.strategyv2.R27QualityTest#h15MainlineRedLines` 和 `com.ethk3.novaonepro.R31RetirementManifestTest#*`。新R32负例仅存在于修复后的P1测试源码；基线无该方法，不能用默认6例签收基线。
