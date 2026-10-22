# R3.6 → R3.6.1 根因与返修证据

## 现场故障

上一 R3.6 运行报告的首个确定性 P0 是：

`No direct method <init>(Context,String,Object,int) in SQLiteOpenHelper`

随后出现 `SERVICE_NEVER_STARTED`、`actualStart=0`、`STORE_NULL`，并连锁造成 Runtime、OOS、Coverage、Settlement、BetObservation 全部 READ_ERROR。后续 READ_ERROR 是 Service/数据库入口崩溃后的结果，不能先当成模型问题处理。

## 根因

上一包不是 Java 源文件里的 DbHelper 逻辑写错，而是无真实 Android SDK 时使用的本地 API 声明不精确，导致 javac/dx 把若干 Android 框架调用写成设备上不存在的 DEX 方法描述符。最致命的是 SQLiteOpenHelper 第三个参数被编成 `Object`，真实系统要求 `SQLiteDatabase.CursorFactory`。

同一问题还涉及：

| API | 上一包错误描述符 | R3.6.1 正确描述符 |
|---|---|---|
| ContentValues.containsKey | `(Object)boolean` | `(String)boolean` |
| ContentValues.get | `(Object)Object` | `(String)Object` |
| ContentValues.put | `(Object,Object)Object` | Android 类型化 put 重载 |
| SQLiteOpenHelper 构造器 | 第三参数 `Object` | 第三参数 `SQLiteDatabase.CursorFactory` |
| ListView.setAdapter | `BaseAdapter` | `ListAdapter` |
| TextView.setTypeface | `Object,int` | `Typeface,int` |

PendingIntent 的常量值也被校正为 Android 实值：UPDATE_CURRENT=`0x08000000`、IMMUTABLE=`0x04000000`、组合=`0x0c000000`。

## 返修内容

1. 重建受影响的 Android API 签名声明，补齐 Adapter/ListAdapter、Typeface、ResolveInfo、AudioAttributes 等真实类型。
2. 新增 APK 级 ABI 门：先反编译最终 `classes.dex`，拒绝六类已知坏描述符，要求六类正确描述符存在。
3. 将最终 APK 的全部 Android 方法调用与“真实 SDK 构建的 V51 APK”调用集合比较；新增调用逐条按 API 28 AOSP 声明复核。
4. 校验最终 APK 不包含任何 `android/*` 编译声明类，防止本地声明被误打进安装包。
5. 提升 versionCode 到 577180813，并保持原证书连续性，允许安全覆盖升级。
6. 新增 Java 17 全源码编译门和主机行为回放门。
7. 修正 B10 漏斗运行/持久化不一致：统一为精确嵌套 `9→8→7→6→5→4→3`，并把同一份 Audit 传入 RuntimeResult 与 B10 证明账本。
8. 重建 K3ActionSpaceEngine：精确枚举 `10*C(10,3)=1200`，验证身份唯一性、组合序号 0..119、全局序号 0..1199、概率与冠军冻结边界。
9. 按 B14/B15 合同重建诊断 DAG 与完整报告核心，加入确定性回放、章节索引、全文/摘要/JSON/分段、复制脱敏和 DB 报告持久化。

## 最终 APK 硬证据

- APK SHA-256：`c925e8bf721778386f49f1153e6b29891115128ab5d1a85848c5a6919cf1c2a8`
- Android 方法描述符：234
- 已知错误 ABI 描述符：0
- PendingIntent 常量：PASS
- ZIP/zipalign：PASS
- APK v1/v2/v3 签名：PASS
- 升级证书连续性：PASS
- package/versionCode/versionName：PASS

这些证据证明“本次 APK 不再包含已确认的框架 ABI 错编译”。它不能替代真机启动与长时间运行，所以物理设备状态仍标记 RETEST_REQUIRED。
