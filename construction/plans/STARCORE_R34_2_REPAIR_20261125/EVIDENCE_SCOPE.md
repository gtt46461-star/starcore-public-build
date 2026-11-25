# 证据范围

事实包外层SHA256为`7b5dba4d24203261f0173c617d79083808377c0e4257b8fd490e523a0ce87cf7`。内层57成员中56载荷的字节数和SHA256逐一核验；manifest自身与外层索引一致。压缩包完整不表示业务全部通过。

本报告96检查按health归一：4 PASS、4 FAIL、7 WARN、21 NOT_PROVEN、44 OBSERVED、16 NOT_APPLICABLE；原domain_outcome细分值逐条保留，不混用。44指标：35 INSUFFICIENT、5 RECORDED_UNVALIDATED、1 BLOCKED、1 UNKNOWN_SOURCE_MISSING、1 NOT_DEPLOYED、1 UNKNOWN_STALE_SYNTHETIC_SOURCE。OBSERVED只说明取到记录，RECORDED_UNVALIDATED也不是通过。

本包未附原始SQLite、安装APK、模型/训练原件；报告来源声明不替代外部重算。13:46运行观测、13:46数据库快照、13:58诊断进程cohort观察、14:10磁盘巡查分别保留，不能混为一个原子时点。JSON投影保留原路径与记录hash；完整原始内容在随总包提供的检测ZIP中。

源码精确来自152提交；计划维护主分支仍为第6版旧底座，分别绑定，不把计划文档更新称为APK已改。
