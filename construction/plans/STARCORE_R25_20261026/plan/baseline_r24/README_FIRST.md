# STARCORE R24 最终施工总包 · 2026-10-25

先读根目录的《STARCORE_R24_最终施工方案.docx》或 `docs/01_最终施工方案.md`。Word包含主文与附录A—D；全部机器明细和原96检查原文在包内另存，不以Word正文替代证据。

## 本包交付什么
这是有现场证据支撑的施工方案，不是已经实施完R24的APK。48张卡明确证据、根因边界、文件/方法、修改、反例、验收及责任；24家族和16专家为升级执行契约，不能把规划条目计作已实现。

目录：`docs`主文及附录；`registries`48卡、28章报告合同和原1832要求/299卡/P001—P124；`evidence`数据、源码覆盖和现场证据；`code/tests`HOST参考与回归；`patches`两个SHA约束窄补丁；`results`本次实际实验；`history`两份用户原包逐字节保留及历史文本。

本次已验证：R23 Python30项、R23 Java104检查、R24 Python27项、Java门槛修补6检查和原缺陷复现。既有CI72项单测是旧工件记录，不是本次重跑。完整Android构建、真机长期运行、全994份源码正文审计、自然前瞻质量证书均未完成。40份SHA匹配正文加若干远程关键文件的阅读不能冒充全仓审计。

## 复核命令
Python 3.10+（建议3.11）。基础契约不需要第三方库；Java回归需要JDK。

```sh
python code/verify_bundle.py
python -m unittest discover -s tests -p 'test_*.py' -v
python code/run_java_regression.py
python code/prepare_inputs.py --out ./work_inputs
python code/audit_inputs_r24.py --plan ./work_inputs/plan/10.25晚上100m总包施工方案/STARCORE_R23_COMPLETE --report ./work_inputs/report --out ./recomputed_evidence
```

最后一条的实际report目录以prepare_inputs输出的 `resolved_inputs.json` 为准：检测原包可能包含内层ZIP；工具自动发现并展开，路径无需手写猜测。更方便的一键复核：

```sh
python code/prepare_inputs.py --out ./work_inputs --audit
```

冻结模型实验需要NumPy及与原检查点兼容的PyTorch，版本见 `results/environment.json`。在 `resolved_inputs.json` 中取plan与report路径，先运行audit，再执行：

```sh
python code/experiments_r24.py --plan <实际plan目录> --evidence ./recomputed_evidence --out ./recomputed_experiments
```

此脚本加载原检查点做63期历史延伸，并公开全部固定对照；不会冒充训练新冠军或产生实盘证书。缺失112期后的30条记录被连续32期输入条件排除。原完整四模型从头训练本次未复跑。

## 补丁应用
`python code/apply_reference_patches.py --repo /实际检出仓库` 默认只检查；显式加 `--apply` 才写入。两个文件before-SHA必须都匹配，否则不修改任何文件。补丁只解决非法动作/概率兜底和15%门槛冲突；不解决全部R24。应用后仍需Android编译、单测及真机测试。不要把本包内旧release APK或CI记录误当R24新版本；本包不另交APK。

## 完整性
运行verify_bundle逐文件核对MANIFEST_SHA256.json。原施工包118274318字节，原报告8199607字节。保留原包是为了不删旧要求和模型状态，不代表大体积本身有更强能力。所有新增研究设计与未实施状态均明确，质量目标未达时不得批量修改为通过。
