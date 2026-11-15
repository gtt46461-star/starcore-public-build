# 本项目不启用混淆。保留反射读取的模型与引擎字段，避免未来开启混淆时字段链断裂。
-keep class com.ethk3.novaonepro.** { *; }
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

