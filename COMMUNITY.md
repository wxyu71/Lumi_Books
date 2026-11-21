# Lumi Community（非官方个人 Fork）

本项目由单人维护和使用。源自 [Huangder 的 Lumi_Books](https://github.com/huangder/Lumi_Books)，**不是原项目的官方发行版**。保留 `LICENSE`、`NOTICE`、上游版权声明和第三方许可。

## 版本与功能边界

- Android 包名：`io.github.wxyu71.lumibooks`，与上游应用隔离安装；公开 OSS 源集固定为 `app/src/oss/`，不导入私有 Premium 代码。
- 当前版本以 `gradle.properties` 的 `lumi.communityVersionName` / `lumi.communityVersionCode` 为准，应用更新检查只读本 Fork 已公开的 GitHub Pre-release。
- 保留本地标签和分组、可选的阅读日志、TXT/Markdown 标注导出、WebDAV/OPDS 等公开功能；不加入上游的会员购买、私有阅读报告、旧云服务、捐赠二维码和桌面小组件。
- 原作者不自动成为本 Fork 的维护者或贡献者；原作者及第三方署名通过许可证和 README 保留，不通过赞助/开发者排行榜表示社区人员。

## 开发与发布

这是单人维护的项目：可使用 PR 或由维护者直接更新 `main`，均由正常 CI 验证；**没有专门的发布审批团队、签名证书指纹变量或手动 Tag 流程**。首次发布只需配置两个固定签名 Repository Secrets，见 [发布指南](docs/development/release.md)。

- PR 与 `main` 的自动检查定义在 `.github/workflows/android-beta.yml`，保留现有完整 Required Gate。
- `main` 的新版代码通过整个 CI 后，`.github/workflows/android-release.yml` 自动创建对应 `vX.Y.Z-beta.N` Tag 与 Pre-release，并上传优化后的 APK 和 `SHA256SUMS.txt`。同版本已经发布则跳过；仅允许最新的 `main` CI 发布。
- 发布 APK 始终使用同一份个人固定 PKCS12 密钥。相同包名、相同签名且 `versionCode` 递增时，后续 Beta 支持覆盖安装；只有从旧 Debug/临时签名包首次切换到固定签名时，才需要备份、卸载和重装。原密钥要离线备份，不能重新生成替代。
- 下一版需要修改 `gradle.properties` 的版本名和递增的 `versionCode`，更新 `app/src/main/assets/changelog.md`，正常提交到 `main`（可直接推送或合并 PR）。无需额外创建发布说明文档或 Tag。
- [下载入口](https://github.com/wxyu71/Lumi_Books/releases) 只有此 Fork 的 GitHub Releases；应用内不使用原项目网盘、Gitee 或官方 APK。

具体命令见 [构建指南](docs/development/build.md)、[发布指南](docs/development/release.md)，CI 门禁说明见 [CI 参考](docs/development/ci.md)。

## 文档边界

[文档索引](docs/README.md) 是技术文档的唯一导航入口；全局 Agent 约定见 [AGENTS.md](AGENTS.md)。不因每个 PR、CI 失败或下一位 Agent 单独创建 handoff、进度总结和大型扁平化设计文件；单次实验、运行记录及问题证据留在 GitHub PR、Actions 和提交历史中。
