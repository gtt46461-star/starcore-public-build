# STARCORE R31 唯一施工入口

当前方案：**R31.2-SEQUENTIAL-20261105，22个顺序批次（S00→S21），56张原卡+5张工程卡**。批次序号就是施工顺序，一批关门后才开下一批。冻结方案与应用源码分别读取，禁止把main当作最新应用源码。

- [R31.2顺序批次施工合同（根因、规则、22批总表）](https://github.com/gtt46461-star/starcore-public-build/blob/main/construction/STARCORE_R31_FINAL_20261104/docs/15_R31.2顺序批次施工合同.md)
- [逐批施工页 batches/S00–S21](https://github.com/gtt46461-star/starcore-public-build/tree/main/construction/STARCORE_R31_FINAL_20261104/batches) · [方案总入口](https://github.com/gtt46461-star/starcore-public-build/blob/main/construction/STARCORE_R31_FINAL_20261104/README.md)
- [应用源码与施工记录：construction/r31-final-20261104](https://github.com/gtt46461-star/starcore-public-build/tree/construction/r31-final-20261104) · [下一步](https://github.com/gtt46461-star/starcore-public-build/blob/construction/r31-final-20261104/construction/r31/NEXT_ACTION.json)

## 怎么施工

在施工分支根目录：

```bash
G="python3 construction/STARCORE_R31_FINAL_20261104/tools/r312_gate.py"
$G status                   # 当前批次与未绑定的验收项
$G build --batch Sxx        # 开发构建四任务 + HOST注册表
$G close --batch Sxx        # 机器判定是否关门，写RECEIPT.json
```

第一批是**S00**（施工通道与在制品盘点），第二批**S01**专门把已合入在制品造成的全量失败修绿（Certificate指纹、内存准入相容）。此后每批关门都要求全量单元/HOST/lint为绿。

## 为什么改成R31.2

R31.1的P01关门要求全量测试和Android联合协议全过，但树内已合入21卡、7个批次的在制品，失败恰恰来自被禁止推进的后续阶段——门永远关不上。另外卡片验收混入真机72h等项，任何卡都无法在无设备会话中关闭；B编号又不是施工顺序；每次改代码都重绑发布身份；没有统一关门命令。R31.2逐项修正，见施工合同第1节。

## 验收边界

关门只看LOCAL项（198项，JVM单元/HOST/静态/编译/lint），每项必须绑定实际通过的测试。Android模拟器项（8项）只在S20集中执行；真机/时长项（24项）和自然质量（1项）延后，任何批次都不得记PASS。阈值配置字节不变，不降断言、不加堆、不排除测试。

方案自检：`check-plan`、`validate_package.py`（1338项）与`test_r312_gate.py`（17项关门工具反例/正例）全部通过。方案修订不表示Android故障已修复；当前完整工程与成品仍待按批施工。R31.1控制文件归档于`construction/archives/STARCORE_R31_1_EXECUTION_20261105/`。
