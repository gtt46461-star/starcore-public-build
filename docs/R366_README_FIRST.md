# 星核天枢 V57.15.3-R3.6.6 先读说明

## 交付身份

- 包名：`com.ethk3.novaonepro`
- 修订：`V57.15.3-R3.6.6`
- versionCode：`577180818`
- versionName：`V57.15.3-tianshu-b01-b15-runtime-closure-hf6-20260811`
- 数据库：`binance_k3_nova_one_pro_v57.db`，`DB_VERSION=36`，不清库
- 构建：Java 17 classfile major 61、官方 Android 36 API、D8 9.2.23
- 安全姿态：`SHADOW_ONLY=true`、`MODEL_PROVEN=false`、REAL 硬锁

## R3.6.5 为什么仍然不出号

设备已给出完整确定证据：请求成功进入唯一执行链，但三次都在提交前以
`java.lang.IllegalStateException:R36_ACTION_INVALID` 终止；因此运行事件、canonical、100 号码和
1200 动作始终为零。源码对应的必现错误是：动作生成器把 `n4` 定义为前三个固定 K3 号码之外的最佳
边界号（范围 1–10），快照验证器却把任何非零 `n4` 判为非法。所有 1200 个合法动作都会失败。

深挖同一提交边界还发现第二个必现故障：生产端保存 `K3ActionSpaceEngine.actionSpaceHash`，快照端却用
另一套简化字符串复算。只修 n4 后会继续报动作宇宙哈希不匹配。R3.6.6 同时关闭两处漂移，并把
字段、行号、K3、n4、rank 和哈希写入失败明细。

## 安装

为保留现有数据库，直接覆盖安装，不要先卸载：

```bash
adb install -r Starcore-Tianshu-V57.15.3-R3.6.6-release.apk
```

用手机文件管理器时，解压总包后直接点击
`APK/Starcore-Tianshu-V57.15.3-R3.6.6-release.apk` 并选择更新即可。其余目录用于源码、签名连续性、
工具链和离线复建证明，不需要复制到手机。

## 安装后验收

从覆盖安装后的第一个仍未开奖目标期开始，应依次看到：

1. `ANALYSIS_RUNNING` 后进入 `ANALYSIS_CANONICAL_COMMITTED`；
2. 运行账本 `events>=1`，`last=ANALYSIS_CANONICAL_COMMITTED`；
3. 同步核心 `issue=1`、`champion=1`，延后证据随后达到 `number=100`、`full=1200`；
4. 不再出现 `R36_ACTION_INVALID`、`R366_ACTION_SPACE_HASH_MISMATCH`；
5. 下一期开奖后由唯一 Settlement Owner 写严格 OOS，且 `modelCalls=0`。

旧版本没有生成的 pre-draw canonical 不能在开奖后伪造回补，所以既有 missing 列表不会立即清零；判断
修复是否生效，应观察安装后的新目标是否开始新增合法覆盖。

## 真实性边界

T45 主机回归证明实际 100/1200 运行结果可完整生成、压缩编码、解码、复算动作哈希并保留合法 n4；
签名、ABI 和离线复建证明交付产物一致。物理设备的亮屏、息屏、划掉任务、杀进程、网络切换和 24 小时
运行仍需安装后实测。模型 Artifact 仍未训练且严格 OOS 样本不足 120，因此不允许真实下注、动态 K、
多位置 REAL、追期、马丁、倍投或频率债务。
