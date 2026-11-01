# FR-B01-01 修复诊断空探针空指针

优先级：P0。状态：设计交付，尚未Android/设备验收。原需求：FIX-02。

前驱：B00。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/UpgradeDiagnosisSU2.java`

动作：**MODIFY**。施工范围：capabilityRows。

问题：PR非空但所有gain为null时bestGainProbe返回null，PR.get(null).gain在770行崩溃。

修改方式：用bg!=null判断存在可评分探针；无增益时保留NA，不补0，不改原统计公式。

原方法锚点：UpgradeDiagnosisSU2.capabilityRows(Diagnosis D) L762–835

基线SHA256：`f1dbe7e4ab22a628bdae3a3ed67c4f3cbff97bb2aace5d5d77035f4ff39f598a`。共享合并顺序：FR-B01-01。

## 实施步骤

1. 应用B01诊断补丁中的本文件hunk。
2. 将空探针、空快照、有效探针和损坏输入分别分类；原始手机输入缺失时不宣称已复现手机同一数据。

## 测试与反证

- 空CONTMAT探针复现基线NPE，修后可导出；16份原诊断MD/JSON逐字节一致。

## 验收

空探针不再崩溃、原有效输入输出保持一致。

## 恢复与回滚

回退单hunk并保留可重放输入，不删探针。

## 关键代码

顺序补丁：`patches/B01_diagnostic_truth.patch`。必须按SERIES顺序应用，一份跨文件补丁只应用一次。

已预写目标文件：`code/app/src/main/java/com/ethk3/novaonepro/strategyv2/UpgradeDiagnosisSU2.java`。它是本包关键补丁叠加后的组合目标，不是允许覆盖其他卡修改的单独发布文件。

### 可直接审阅的关键代码

空探针修复核心分支（完整目标在code目录）：

```java
String bg = bestGainProbe(PR);
if (bg != null) {
    ProbeSum measured = PR.get(bg);
    // 原row渲染使用measured.gain / measured.lo。
} else {
    row(rows, "C.probe", "na", NA, NA, NA, NA);
}
```

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
