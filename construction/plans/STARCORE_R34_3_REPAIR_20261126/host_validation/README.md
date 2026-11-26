# 可重复的主机合同验证

在计划目录外的全新输出目录执行：

```bash
python3 -B tools/run_host_checks.py --plan <本计划绝对目录> --source-repo <含173对象的仓库> --out <全新计划外输出目录>
```

runner从Git精确173提取测试所需原依赖，将附带补丁/测试放入隔离工作目录；不修改应用工作树和封印。需要Python 3.12+、git和带jdk.compiler的JDK。输出实际argv、退出码、完整日志和SHA256；任何非零退出停止并明确FAIL。

测试涵盖TEMP/恢复探针/异常分类、物理预留边界、真实Java生成结算SQL、训练继任DDL/谓词、rolling不可变/CAS、独立冻结SQL、绑定父源SQL、6个实际CP9数值kernel的恢复、768KiB预算和图表receipt/SQL、交付门及全新增Java语法。部分测试明确使用合成SQLite或平台stub；只验证对应合同，不能签Android构建、生产Owner调用、132个完整工程场景、真机或自然质量。

Math/CP9数值内核和SQL由实际源抽取，不复制一份简化公式来测试自身。测试脚本中旧audit相对目录由runner在隔离目录重建，不要求用户机器存在本次临时路径。
