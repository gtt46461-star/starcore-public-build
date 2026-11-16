# R32.5.1-C1 033 正式签名 APK

本 APK 已用原 032 安装证书签名，包名不变，versionCode 从 577311032 增至 577311033。API34 上已实测原签名 032→033 覆盖安装及 033 同版重装，安装字节、UID 和实际测试数据库保全通过。此证据不代替用户原真机数据库验证。请使用系统覆盖安装，保留应用数据。

冻结构建源码：`bb7b49bbcc9266bad548ec37e71403256fc06a4d`。APK SHA-256 见 SHA256SUMS.txt。私钥、口令和实际数据库均未放入本交付分支。

P1 已通过 655 项单元测试，P2—P5 每批已通过 659 项单元测试及当前源码编译、测试编译和 Lint。Android29、34 完整矩阵通过；Android35 首轮仅一个未来冻结断言失败；原七项交付协议已按原 360000ms 限制全部复验通过，原失败保留，完整原矩阵正在复跑。33 条外部协议、222 条完整业务验收、真机24/72小时与自然质量另列，不宣称全部通过。REAL 开关未被开启。

当前交付包括实际 APK 与身份回执，完整证据 ZIP 在收尾核对后补入同一分支。

源码 bundle 已附，前置为原精确基线。先克隆本仓库并取得基线后执行：

```bash
git bundle verify STARCORE-R3251-C1-bb7b49bb-source.bundle
git fetch STARCORE-R3251-C1-bb7b49bb-source.bundle refs/heads/construction/r3251-c1-attachment-final-20261116
git switch --detach bb7b49bbcc9266bad548ec37e71403256fc06a4d
```

bundle 中四个批次工程验收 ref 保留当批实际源码和回执；APK 编译提交固定为 bb7b49bb。
