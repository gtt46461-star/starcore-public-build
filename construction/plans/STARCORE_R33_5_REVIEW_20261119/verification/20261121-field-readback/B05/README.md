# R335-RE14 研究图的原件读回

Android 10 和 Android 15 上，原测试分别把 5000 期标明为受控输入的序列送入生产分析器，原生研究工作台的 R01 返回 STATUS。两个工作流、原测试输出摘要和堆栈见 original-android-failures.json。

生产者 ResearchFrameBoundR30R1.project 将不可变原件摘要写进 fullMachineFrameSha256，消费者读取 originalMachineFrameSha256。相同的真实 SQL 写入和工作台 load 路径在 SQLite 上复现 R01、R02、R03 均失败。补正后得到 120 点热图、17 点折线、100 点网络，变造缓存摘要继续返回 READ_ERROR。新回归覆盖旧字段和冲突摘要，原有文件大小及单页显示限制照常执行。完整前后态、回归原件与两个 Android 失败原件均保留。