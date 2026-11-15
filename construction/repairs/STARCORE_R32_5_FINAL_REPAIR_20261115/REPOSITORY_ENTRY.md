# 当前唯一入口：R32.5.1

本目录是发布入口，必须先恢复整个方案。当前施工代码及产品候选仍R32.5/033，交付工具修订为R32.5.1。旧入口的文档及PASS不覆盖本次复核结果。

1. 运行 `python3 restore_package.py --extract R32_5_1_restored`，进入其中的 `STARCORE_R32_5_FINAL_REPAIR_20261115/`。
2. 完整读取README、FINAL_REVIEW_R3251、CODEX_PROMPT及五批合同。运行本包verify_package --sealed和verify_report_inputs。
3. 使用源码基线7424eeae6354eb6599dd8498e123c059bd410c90的独立工作树。保全已有修改，先由当前inspect_resume定位，不直接把main发布页当作应用源码基线。
4. 顺序执行P1→P5，每批在本批当前源码上应用、生成身份、新编译、测试及记录；实际模式不符、未知内容或中断journal冲突必须保留现场。
5. 在正式Android构建/签名环境继续构建和覆盖验证。自然证据、设备时长和实际缺件各自验收，不阻止能执行的工程工作继续推进。

[详细复核](FINAL_REVIEW_R3251.md) · [当前验证](VERIFICATION.json) · [总包身份](DELIVERY.json) · [完整提示词](CODEX_PROMPT.txt) · [恢复与下载说明](README.md)
