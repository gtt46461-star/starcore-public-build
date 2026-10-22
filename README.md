# STARCORE Public Build

STARCORE 完整软件源码的公开构建副本，迁移日期：2026-10-22。

来源施工分支：`construction/c021-c024-20261021`。来源提交：`9ec590cafbd7cbcd1265a11b63a59df53ba78fcd`。

当前应用标识：`com.ethk3.novaonepro`；版本：`V57.21.2.0.95-R5.4.2.95-1021-C024`；versionCode：`577251054`。

## 包含内容

保留完整 `app` Java/C/C++ 源码、Android 资源、运行资产、Gradle wrapper、构建脚本、训练工具、验证测试、release 身份清单、源调用索引和构建依赖的施工清单。逐文件来源、Git blob 哈希和 SHA-256 见 `SOURCE_COPY_MANIFEST.json`。

原私有库历史工作流保存在 `ci/legacy-workflows/`，便于查看原构建流程。当前可执行的工作流为 `.github/workflows/build-apk.yml`，只通过手动运行启动。

未迁入签名私钥、密码、实际 API 凭据、APK/AAB、私有 Git 历史，以及大型历史运行输出、重复源码备份和施工方案包。应用功能源码保持来源提交的原始字节；仅扩展 `.gitignore`，设置 `gradlew` 可执行权限，并调整历史工作流存放位置。

## 本地构建

需要 JDK 17、Android SDK 36、Build Tools 36.0.0、NDK 28.2.13676358、CMake 3.22.1、Python 3。

```sh
cp local.properties.example local.properties
# 按本机 SDK 路径修改 local.properties。
./gradlew --no-daemon :app:assembleDebug
```

也可以在 GitHub 的 **Actions → Build APK → Run workflow** 中构建 Debug APK。

Release 构建保持原项目的外部配置与原签名校验要求：`DUOQU_API_KEY`、`STARCORE_SIGNING_P12`、`STARCORE_SIGNING_PASSWORD`、`STARCORE_SIGNING_ALIAS`、`STARCORE_EXPECTED_CERT_SHA256`，以及按需使用的 `STARCORE_SIGNING_TYPE` / `STARCORE_SIGNING_KEY_PASSWORD`。这些值由构建环境私下提供，不能提交进公开仓库。Debug 签名与原安装版签名不同，不能作为原签名覆盖安装包。

## 已完成的迁移校验

4,809 个来源文件全部通过原始 Git blob 哈希校验。原子/分子目录、R967 语义标识、源调用索引的现有生成校验均通过。扫描覆盖来源文件和 347 个压缩归档成员，未发现需要迁入的真实凭据。本次为源码迁移，不代表 C024 的软件运行验收或现有 lint 问题已经完成。
