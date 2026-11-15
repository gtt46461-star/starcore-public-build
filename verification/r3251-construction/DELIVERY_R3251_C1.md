# R32.5.1-C1 实际工程交付记录

五批源码已落地于隔离分支 `construction/r3251-c1-ownerfix-20261115`，各批自己的源码均完成完整开发 Gradle 检查。312 个原受控路径与4个阶段身份输出已核对 Git 已提交字节和权限。原总包完整恢复，SHA-256 为 `ceb5e3516452524de2519781bff04ba9f844bb95f95052bd4c2778928b856966`。

正式 APK 尚未生成：实际 CI 的既有签名缓存 `starcore-r27-ci-temp-signing-v1` 未命中；本地 `assembleRelease` 实际返回 `R995_RELEASE_DEFAULT_API_KEY_REQUIRED`。CI 在签名失败后跳过 API 绑定，因此没有断言 CI API 凭据不存在。没有创建替代签名、改包名或打开真实出手开关。

| 批次 | 实际构建源码提交 | 当前单测通过 | 具名宿主入口 | 原验收 ID |
|---|---|---:|---:|---:|
| P1 | `8a8ee55cf63c9e7faf6383a2b477b00a9dadbf9a` | 655 | 2 | 10 |
| P2 | `062d68e639e9de32537973cb391ed8b413494956` | 659 | 23 | 79 |
| P3 | `3a934918df69a33a162d69f70930dfd7d48f2154` | 659 | 10 | 22 |
| P4 | `78004d4dbd407aa6457ad84ad4aa4f652fbb03b0` | 659 | 13 | 63 |
| P5 | `a2efd722b3d7266c6fc87ae267a3547ef7712bd1` | 659 | 7 | 48 |

每批执行了 `compileDebugJavaWithJavac`、`compileDebugAndroidTestJavaWithJavac`、`testDebugUnitTest`、`lintDebug`，零单测失败、零跳过。Lint 沿用原基线，未新增基线豁免；现有基线仍过滤51项，详细报告保留。P2 三个方法使用有具体物理字节准入理由的局部 `UsableSpace` 注解，运行字节码经核对未变。正式 Release Java、三 ABI native 与 Release lint 另有实际通过日志。上述记录不能替代 APK、Android 设备执行或覆盖安装回执。

189 条原验收的全部具名工程入口已通过，33 条外部项保留未执行/未证明。所有222条均保留原文、原文摘要、拆分前卡文本、旧归属、入口结果、失败历史和未完成项；未用组级 PASS 自动签收整条业务语义。

- [222条逐项账本](delivery/CURRENT_ACCEPTANCE_222_EXECUTION.json)、[CSV](delivery/ACCEPTANCE_222.csv)
- [33条逐项外部阻断和原操作规程](delivery/EXTERNAL_33_CURRENT_BLOCKERS.json)
- [165对象逐项绑定与效果边界](delivery/OBJECTS_165_CURRENT_EVIDENCE.json)、[CSV](delivery/OBJECTS_165.csv)
- [1832条原需求及当前逐项缺口](delivery/REQUIREMENTS_1832_CURRENT_EVIDENCE.json)、[CSV](delivery/REQUIREMENTS_1832.csv)
- [511条重分类原记录](delivery/RECLASSIFIED_511_CURRENT_EVIDENCE.json)
- [312个原受控文件实际摘要与权限](delivery/CONTROLLED_FILES_312.json)
- [各批构建索引](delivery/STAGE_BUILD_INDEX.json)、[完整交付状态](delivery/DELIVERY_STATUS.json)
- [完整未完成项](delivery/OPEN_ITEMS.md)、[原33条外部规程](delivery/EXTERNAL_ACCEPTANCE_PROTOCOLS_33.md)
- [P1原始账本和构建日志](P1/)、[P2](owner4/P2/)、[P3](owner4/P3/)、[P4](owner4/P4/)、[P5](owner4/P5/)
- [CI实际阻断原件](owner4/P5/P5-ci-signing-block/BLOCKER.json)、[实际CI运行](https://github.com/gtt46461-star/starcore-public-build/actions/runs/43744142741)
- [经审阅的子包完整字节覆盖层](owner4/P5/reviewed-package-delta/DELTA.json)、[原封包恢复证据](delivery/PACKAGE_RESTORATION.json)
- [后续证据提交与实际构建源码的关系](delivery/CURRENT_SOURCE_RELATION.json)

原封包没有被改写。P2/P4 实际发现的生产缺陷在对应 owner 修复并生成新阶段身份；原失败、旧身份、旧 journal 和其摘要保留。当前子包为显式审阅覆盖层，不能冒称原封包 SHA。P3 宿主健康调度/合成日历夹具、P5 当前静态退役清单属于附加验证设施，原P1清单和生成器保持原字节，批准删除的数据对象为零。

原312路径中实际变更120个，原样保全192个；410张问题—文件卡、18个联合问题和5批划分保持。原样保全和附加账本不计新增实现。165对象逐项检查只证明对应源码字节与绑定一致，没有把材料增长、静态方法或总体计数作为训练和独立效果证明。

原032数据库、556件丢失原件、历史覆盖证明/两份旧catalog、同签名覆盖、Android模拟器/真机流程、24/72小时和独立自然质量仍未签收。两份报告中的032安装身份可以交叉核对，但不能替代原数据库或原APK字节。新的实际高命中、独立未来资格和原6臂×20种子完整Q08效果也未由报告重渲染产生。

工程证据ZIP和增量Git bundle会在 `release/deliveries/R3251_C1/` 提供，附SHA-256。它们包含实际源码提交和验证材料，不含APK；成品APK与包含同字节APK的ZIP仍为缺项。Git bundle 以前述精确源码基线为前置提交，可在已获取基线的仓库执行 `git bundle verify` 后 `git fetch <bundle> refs/heads/construction/r3251-c1-ownerfix-20261115` 检出。仓库的签名构建工作流保留原签名校验，安全环境恢复后才会继续成品打包和原设备协议。
