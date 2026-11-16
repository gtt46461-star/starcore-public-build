# 冻结构建源码

正式 APK 精确源码提交 `bb7b49bbcc9266bad548ec37e71403256fc06a4d` 已在独立施工分支公开；以该提交的 Git 树为完整权威源码。

```bash
git clone https://github.com/gtt46461-star/starcore-public-build.git
cd starcore-public-build
git fetch origin bb7b49bbcc9266bad548ec37e71403256fc06a4d
git switch --detach bb7b49bbcc9266bad548ec37e71403256fc06a4d
```

本目录的二进制补丁提供精确基线 `7424eeae6354eb6599dd8498e123c059bd410c90` 到冻结构建的应用、构建、工具与身份文件差异，便于审阅；它不冒充包含所有历史封包/证据的完整仓库。独立源码 bundle 会保留上述基线为前置。正式签名与 API 绑定沿用原授权配置，私钥和口令不在本包。
