# R903 覆盖安装签名状态

R903 发布 APK 已使用原 STARCORE V57 发布私钥签名，并与 R902 基包完成证书连续性比较。

- package：`com.ethk3.novaonepro`
- versionCode：`577200903`
- versionName：`V57.16.3-R4.0.3-903`
- APK SHA-256：`7869d9bac682fe1fe8803f389608000f69552a5eff2600e5c842f618f22aee8d`
- 新 APK 证书 SHA-256：`ae7e1b4a5a71f1f299d4a0b2472c6a36513873514e78190d42d6cfba3340116f`
- R902 基包证书 SHA-256：`ae7e1b4a5a71f1f299d4a0b2472c6a36513873514e78190d42d6cfba3340116f`
- v1/v2/v3 验签：PASS
- ZIP 完整性：PASS
- 结论：`SIGNATURE_CONTINUITY=PASS_OVERWRITE_INSTALL`

发布私钥和口令属于高敏感签名资产，没有放进分发总包；总包中的 APK 已完成签名，可直接覆盖安装。后续重新构建仍需原签名材料，`scripts/r903_build_release.sh` 会强制比较证书，不一致就失败。

