# R31 同身份 CI 准备记录

本目录是只读审查、未安装的工作流草案和核验记录。尚未修改签名分支、恢复签名缓存、触发构建、下载 R31 APK 或运行远程模拟器。两个 request TEMPLATE 保留 `null`，必须由最终已提交 R31 源码和实际版本填入后才能使用。

## 现有工作流的真实合同

- 已读取远端 R27 分支的 `r30-final-r27.yml`、`r30-build.yml`、`r29-build.yml` 和官方分支的 `r30-final-official.yml`、`r30-official.yml`。这些签名工作流均只有 push 触发，没有 `workflow_dispatch`，也没有 `source_commit` input。因此不能直接使用 `gh workflow run ... -f source_commit=...` 调用。
- `r30-final-r27.yml` 的 guard 为 `git merge-base --is-ancestor HEAD origin/construction/r30-repair-20261101`，要求请求源码是旧 R30 分支的祖先。R31 是基线的后代，不会因这一关系反向通过。连接器实际读取的 R30 ref 仍指向 `5237b265a27fe5ad27fd842901c445d5f8c27749`，见 `r30-live-ref.json`；本地退出码证明见 `ancestry-local-proof.json`。
- `r27.yml` 自身虽有 dispatch，但输入是 `emulator_minutes`，它没有任意源码 SHA 的签名接口。不能用它替代 R31 的源码绑定流程。
- 旧 final-r27 工作流会编辑旧 002 release，其他旧签名流程会自动重新绑定并提交源码身份。本次草案没有复制这些操作，也没有 `git commit`、`git push`、`gh release` 或新建签名的命令。

## 最小同仓库 CI 支持变更

R27 通道在 `construction/r27-20261028` 仅需新增 `.github/workflows/r31-build-r27.yml`、`.github/r31-build-request.json`，并应用 `r27-avoid-unrelated-push.patch`。官方通道在 `construction/su1-20261022` 对应新增 `.github/workflows/r31-build-official.yml`、`.github/r31-build-official-request.json`，并应用 `official-avoid-unrelated-push.patch`。这两个补丁只扩展旧广泛 push 工作流的 paths-ignore，避免新 R31 请求触发旧产品构建或自动提交任务。

源码始终从 `construction/r31-final-20261104` 的明确完整 SHA checkout。工作流要求它属于该分支且是固定 5237 基线的后代，核对提交中的公有 applicationId、版本和固定签名通道。身份索引必须在源提交中已经冻结，工作流不会修补产品字节。首次 CI 支持提交应与填妥的真实 request 一起推送，避免空占位请求触发失败任务。

R27 草案复用 `starcore-r27-ci-temp-signing-v1` 缓存和 `DUOQU_API_KEY` 仓库 secret。官方草案复用 `starcore-su1-api-transport-v1` 缓存与现有解密加载器。所有缓存 miss、文件缺失、证书不符、API 配置缺失均明确失败，不生成新 key。只把同一既有 key 的环境绑定映射到兼容变量；不导出或公开私钥、密码、令牌。

已知公有证书：

- EXISTING_R27：`d8d8cee7f406267b2bf18fc9aef0c6ab760423cf205ccd6613dd44085bcc74e7`
- OFFICIAL_R13：`9e6d5dc79d29d4ba5b435a171e6535386f8a56ea6fad71a6cc77aeb10ade3eca`

原始报告未提供用户设备的实际证书指纹，因此两者都不能自动称为该设备的同签升级。APK 检查和设备已安装应用的证书检查必须分开。

## 草案实际安排

构建 job 使用已提交身份运行现有验证器、canonical、lint、release/debug/androidTest 编译和 13 组既有生产字节码 / 原生 SQLite host 回归。预构建日志、回执、候选包始终写到 `RUNNER_TEMP`；legacy host 在同 SHA 的临时完整 clone 中运行，读取本次实际编译的 classes。产品 checkout 保持干净，禁止通过忽略 dirty gate 获得发行。

两种应用 APK 均通过 `tools/r31/verify_baseline.py --require-clean --apk ...` 检查实际签名、应用身份、版本以及 APK 内嵌 source commit/tree、产品摘要、方案摘要、迁移摘要和严格 `source_dirty=false`。instrumentation APK 单独检查实际签名与 test applicationId。候选 ZIP 包含实际 release APK、交付回执、公有签名、源码明确入口、核验结果和施工账本；构建成功仅记 BUILD_PASS。

API 29 / 35 job 先安装本次 release APK并执行现有启动探针，再安装同证书 debug 与 test APK。随后运行 `com.ethk3.novaonepro.R31RuntimeProtocolInstrumentedTest`，使用 `R30FaultAndroidRunner` 和必需的 `r30Sandbox`，并执行既有故障矩阵。它要求实际 instrumentation 输出含正测试数及成功状态，不能用 adb 命令退出码代替业务通过。所有回执明确写 ANDROID_EMULATOR、physicalDevice NOT_RUN、24/72h NOT_RUN、naturalQuality NOT_PROVEN。

已核实 emulator-runner v2 把多行 script 拆为各自独立的 `sh -c`，因此草案先在 `RUNNER_TEMP` 生成完整 Bash 测试脚本，只给 action 一个 Bash 调用。依赖源码依据见 `emulator-action-contract.json`。

`draft-validation.json` 只证明 YAML 可解析、Bash / 内嵌 Python 语法和静态边界通过，不证明 GitHub Actions schema、缓存恢复、签名、构建、设备或质量已通过。

## GitHub 网络与权限诊断

本轮实际执行标准 `gh api --include repos/gtt46461-star/starcore-public-build/actions/workflows?per_page=100`。默认执行失败为 proxy socket `operation not permitted`；完全相同的正常提权诊断失败为 `Get https://api.github.com/...: Forbidden`，没有收到 GitHub HTTP 响应头。当前 enforced network policy 不包含 `api.github.com`。这是代理连接 / 域名策略层的实际阻塞，尚不能判断 GitHub 凭据或 Actions 写权限是否有效。未更改代理、CA、网络策略或令牌，也没有试图绕过限制。

GitHub 连接器仍可读取工作流、refs、Actions runs 和 artifact 列表。旧 run `41420785370` 的产物列表仅用于验证读取路径，见 `artifact-path-probe-old-r30.json`，没有下载或冒充 R31。新请求采用已授权的 Git push 触发，不依赖 workflow_dispatch；实际 push 权限和新缓存恢复仍需真实运行才知道。

取得新 R31 run 后，可通过连接器 `github_fetch_workflow_run_artifacts` 列出该 run 的实际 artifact ID，再用 `github_download_workflow_artifact` 下载；必须核对 source SHA 和产物哈希。标准 `gh run download RUN_ID --repo gtt46461-star/starcore-public-build --name ARTIFACT_NAME --dir OUTPUT_DIR` 也是既有下载方法，但当前 shell API 域名阻塞尚未解除，不能记为已执行成功。草案只上传候选 / host / Android artifacts，不修改或发布旧 release。

## 本轮真实工具修复

按 root 的明确分工，仅修改 `tools/r31/verify_baseline.py` 及对应 adapter 回归：之前在其他身份字段全部吻合时，APK 内 `source_dirty=true`、缺失、字符串 false 或数字 0 都被放行为 PASS。`apk-dirty-before.log` 保留实际 4 个失败反例。现在只接受 JSON boolean false，并在 APK 回执输出实际 `embedded_source_dirty`；开发构建仍可以如实记录 dirty 状态。

全部 13 项 verifier 回归已执行通过，见 `apk-dirty-after-full-verifier.log`。其中 APK adapter 使用明确标注的 ZIP / 工具响应 fixture，不是已签 APK，也不是设备验收。当前没有本地合法签名绑定，真实 APK 的新增检查必须等本次 CI 产物实际生成后执行。
