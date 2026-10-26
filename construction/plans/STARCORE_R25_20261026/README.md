# STARCORE R25 最终施工方案

这是用户确认上传的R25最终施工方案。原包为130,371,679字节，SHA256：

`894937603b41dbe61140b6b47708e55b129b23140692d0d536f9bed61e10739b`

## 施工入口

1. [主方案全文](plan/upgrade_r25/docs/01_最终施工蓝图.md)
2. [统一80卡施工顺序](plan/upgrade_r25/docs/03_统一80卡施工顺序.md)
3. [新增32张施工卡](plan/upgrade_r25/docs/02_32张施工卡.md)
4. [源码文件与方法定位](plan/upgrade_r25/docs/05_文件方法改动定位.md)
5. [CI验收与交付运行手册](plan/upgrade_r25/docs/08_CI验收与交付运行手册.md)

完整保留原1832项需求、299张历史卡、24个家族、16个专家以及R24全部材料。80卡是原R24的48卡加新增32卡，不替代299张历史卡的要求分母。

## 文件下载

- [轻量施工包，3,898,003字节](downloads/STARCORE_R25_LIGHT.zip)
- [16页Word主方案，59,474字节](downloads/STARCORE_R25_BLUEPRINT.docx)
- [完整原包的32个无损分卷与校验清单](original_parts/)

`plan/`是已经展开的轻量施工包，当前施工文档、要求表、源码证据、补丁和测试记录均可直接阅读。它仅省略两份历史原始ZIP；省略清单见`plan/OMITTED_ORIGINALS.json`。完整原包与其中的历史原件均实际存于仓库分卷中，没有用外部链接代替文件内容。

## 还原完整原包

在仓库根目录执行：

```bash
python3 construction/plans/STARCORE_R25_20261026/restore_original.py
```

程序逐卷校验，合并为`construction/plans/STARCORE_R25_20261026/restored/STARCORE_R25_FULL.zip`，验证原包SHA256和ZIP CRC。成功后可正常解压。分卷必须先合并，不能单独作为ZIP解压。

## 给Codex的施工指令

请先读取本目录及`plan/README.md`，按`plan/upgrade_r25/registries/unified_80_cards.csv`从R25-B01开始施工，保留全部历史要求。先核对实际施工源码与计划中引用的SHA，未匹配的补丁不得强行套用；按卡输出具体修改、实际测试日志、未完成项和回滚入口。每批是否具备APK交付条件须如实记录，HOST测试不可替代Android与真机验收。

本次提交只上传方案资料，应用源码没有因此自动升级。计划审计的源码基线为`cd7f058a8345de7c9c89c308c7c91416a1596760`，与本次承载方案的main分支源码分别核对。自然前瞻策略质量仍须实施验证。
