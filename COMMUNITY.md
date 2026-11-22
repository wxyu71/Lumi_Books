# Lumi Community（非官方个人 Fork）

本项目由单人维护和使用，源自 [Huangder 的 Lumi_Books](https://github.com/huangder/Lumi_Books)，不是原项目的官方发行版。保留 `LICENSE`、`NOTICE`、上游版权声明和第三方许可。

## 版本与功能边界

- Android 包名：`io.github.wxyu71.lumibooks`，与上游应用隔离安装；公开 OSS 代码固定使用 `app/src/oss/`，不导入私有 Premium 源码。
- 当前版本以 `gradle.properties` 的 `lumi.communityVersionName` / `lumi.communityVersionCode` 为准，应用更新检查只读本 Fork 已公开的 GitHub Pre-release。
- 保留本地标签和分组、可选阅读日志、TXT/Markdown 标注导出、WebDAV/OPDS 等公开功能；不加入会员购买、私有阅读报告、旧云服务、捐赠二维码和桌面小组件。
- 原作者不自动成为本 Fork 的维护者或贡献者；署名通过许可证和 README 保留。

## 开发与发布

代码组织和 source-set 边界见 [架构总览](docs/architecture/overview.md)，开发命令见 [构建指南](docs/development/build.md)，贡献要求见 [CONTRIBUTING.md](CONTRIBUTING.md)。

PR 与 `main` 使用同一套 Android Community Beta CI。新版 `main` 全部 CI 成功后，发布 workflow 自动处理目标版本：

1. 只读、无签名 Secret 的 build job 构建并验证 unsigned Release APK；
2. 独立 publish job 下载该 artifact，使用固定 Community Beta PKCS12 签名并创建 `vX.Y.Z-beta.N` GitHub Pre-release。

同一版本不会重复发布；过时的 main SHA 不允许发布。发布 APK 始终复用同一固定签名，后续相同包名/签名且更高 `versionCode` 的 Beta 可正常覆盖升级。首次从 Debug/临时签名切换时需备份后卸载重装。

下载入口仅为 [本 Fork GitHub Releases](https://github.com/wxyu71/Lumi_Books/releases)。详细签名和 Beta1 发布检查见 [发布指南](docs/development/release.md)。

## 文档与安全

[docs/README.md](docs/README.md) 是技术文档导航；[SECURITY.md](SECURITY.md) 说明私密漏洞与密钥问题的报告方式；[AGENTS.md](AGENTS.md) 只约束自动化 Agent。

不因每个 PR、CI 失败或下一位 Agent 创建 handoff、状态总结或大型扁平化设计文件；一次性证据留在 GitHub PR、Issue、Actions 和提交历史中。
