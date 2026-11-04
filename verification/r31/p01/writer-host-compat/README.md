本次仅修旧HOST工具接线。基线de9a30834025c18ce291c241a722abe437717539，独立worktree/workspace/r31-writer-host-compat。没有生产、schema、版本、身份或账本改动。

原完整HOST在WriterRotate160000 lost0后，旧run_writer_host.py内嵌的DbStore.Coordinator/工作签名与当前真实HoldLedger/Timeline不匹配，实际javac20errors。原错误日志和原runner逐字封存，未删掉失败或改写成PASS。

run_writer_host.py保留原WriterRotateTest.java未改，实际重新执行160000事件。旧12old+3new断言移到既有真实coordinator native harness的legacy-retry场景，不再模拟新事务API：真实共享writeLock竞争制造提交前拒绝，真实native SQLite END前故障把全部SQL及receipt回滚；接着确证原12、原3无重复、open-upper NULL、原histogram协议与process session全部精确。竞争thread有限join并真实退出。

唯一必要fixture物理适配是due()改为操作新的单调轮换时钟；不改变生产60秒政策或原样本。真实coordinator现在也产生自身遥测，原fake adapter没有这些数据，因此原逻辑工作负载按operation old/new仍恰15，额外3个真实coordinator样本也保留，总表18。不是清空或丢掉失败分母来凑15。

零参数的原入口仍可执行；可选--output持久化证据。生产支持依赖优先选择当前完整Android javac classes目录，也可通过R31_TELEMETRY_SUPPORT_JAR或--support-jar提供真实classes目录/jar；无完整依赖时明确失败，不构造空壳新API。本轮实际使用已存在的合法012完整支持jar，digest及所有真实coordinator/telemetry编译源和class SHA见内层receipt。支持目录/jar前后digest一致才完成；未修改root编译classes。

实际复现命令：

    source /workspace/starcore-environment/activate.sh
    python tools/r30-repair/run_writer_host.py --output verification/r31/p01/writer-host-compat/runs/reproduce-001

最终run为runs/final-original-contract。保留首次适配成功run供追溯。每个子进程60秒截止；平台adapter仅JDBC/Android边界和明确故障注入，不实现或伪造业务幂等/CAS。native JSON记录每笔SQL、bind、DDL、真实commit/rollback和生产hold/wait；所有其他原22场景默认路径仍保留，legacy-only只由旧writer入口显式选择。

此结果只修该已失败工具入口，没有声称重新跑过完整24HOST、Android、设备持续运行、SLO或自然质量。原子/分子发布模块暂未随本工具提交落生产码，将继续原owner协议施工。
