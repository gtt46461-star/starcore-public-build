# STARCORE R31 唯一施工入口

> **2026-11-07 接续（先读）**：卡住的原因与修复、每批“改代码→重钉→推送→CI回写→close”流程，见施工分支 [construction/r31/R31_3_接续施工说明.md](https://github.com/gtt46461-star/starcore-public-build/blob/construction/r31-final-20261104/construction/r31/R31_3_接续施工说明.md)。当前开放批次 K02。

当前方案：**R31.3-CONSTRUCTION-20261105，21个顺序批次（K01→K21），56张原卡+2张在制品修复卡**。批次序号就是施工顺序；每批先改代码，批末一次机器关门。冻结方案与应用源码分别读取，禁止把main当作最新应用源码。

- [R31.3施工推进合同（根因、规则、21批总表）](https://github.com/gtt46461-star/starcore-public-build/blob/main/construction/STARCORE_R31_FINAL_20261104/docs/16_R31.3施工推进合同.md)
- [逐批施工页 batches/K01–K21](https://github.com/gtt46461-star/starcore-public-build/tree/main/construction/STARCORE_R31_FINAL_20261104/batches) · [方案总入口](https://github.com/gtt46461-star/starcore-public-build/blob/main/construction/STARCORE_R31_FINAL_20261104/README.md)
- [应用源码与施工记录：construction/r31-final-20261104](https://github.com/gtt46461-star/starcore-public-build/tree/construction/r31-final-20261104) · [下一步](https://github.com/gtt46461-star/starcore-public-build/blob/construction/r31-final-20261104/construction/r31/NEXT_ACTION.json)

## 怎么施工

在施工分支根目录：

```bash
G="python3 construction/STARCORE_R31_FINAL_20261104/tools/r31_gate.py"
$G status                   # 当前批次、未绑定项、本批要修的已知失败
$G quick --batch Kxx        # 改代码过程中：编译+已绑定测试，可反复
$G close --batch Kxx        # 改完后一次：全量构建+棘轮判定+写RECEIPT
```

第一批**K01**：沿用S00已做的绑定，跑一次全量，把现有失败登记为已知失败并分配负责批次（内存CapacityWait→K02，Certificate指纹→K04等）。从K02起每批都是实打实的代码修改。

## 为什么改成R31.3

R31.2把验收门放在施工前面：S00全是工具活且自检要构建两遍互相比对，S01要求整个测试套件0失败后才能开第一张业务卡，而这些失败本来就是业务卡要修的；方案给的CI兜底配置还会40秒失败，方案修订还会让Gradle配置失败。R31.3改为先施工、批末棘轮验收：不得新增失败，本批负责的已知失败必须修好，K20清零；每条本地验收项绑定通过的测试，确实做不完的最多结转2项（带红测试、不晚于K20）。

## 验收边界

LOCAL 189项是关门条件；Android模拟器8项只在K20跑；真机/时长24项与自然质量1项延后，任何批次都不得记PASS。阈值配置字节不变，不降断言、不加堆、不排除测试。

方案自检：`check-plan`、`validate_package.py`与`test_r31_gate.py`（21个关门场景）全部通过。方案修订不表示Android故障已修复。R31.1/R31.2控制文件归档于`construction/archives/`。
