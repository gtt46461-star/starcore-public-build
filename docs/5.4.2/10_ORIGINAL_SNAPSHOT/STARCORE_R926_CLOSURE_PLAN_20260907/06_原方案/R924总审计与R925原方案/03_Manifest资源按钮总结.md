# Manifest / 资源 / 按钮额外审计结论

- Java生产文件：545
- Android资源文件：71
- phone activity_main IDs：47
- sw600dp activity_main IDs：46
- 差异：sw600dp缺 `btn_background_reliability`
- sw600dp状态标题仍写“R906 运行真值状态”，phone已写“R919 十六项运行真值状态”
- XML Button审计：13个，均有Java R.id引用
- Java/资源引用静态扫描未发现普通R资源缺失；`android.R.id.content`属于系统ID，不是缺资源
- 两组launcher/round图标内容hash相同，这是可接受的重复，不是功能故障
- APK既有验收：resource ID parity=0 mismatch、Main bindings PASS、ZIP/signature/zipalign PASS
- 但当前手机检测报告本身没有独立AndroidManifest/资源/按钮/布局变体审计章节，因此“APK能编译”和“所有界面资源都已完整检测”不能画等号。
