# 贡献指南

Lumi Community 是基于上游 Lumi_Books 的非官方 OSS Fork。提交代码前先阅读 [COMMUNITY.md](COMMUNITY.md) 了解产品边界，并阅读 [架构总览](docs/architecture/overview.md) 确认代码应该放在哪里。

## 开发流程

1. 从最新的 `main` 创建分支，保持改动聚焦于一个可说明的目标。
2. 优先修改已有 feature/layer；不要为了社区版再复制一套数据库、Repository、导航或 UI 根目录。
3. 为行为变化添加真实的 Kotlin/JUnit/Robolectric/Android instrumentation 测试。不要用读取源码/YAML 后 `assertIn` 某段文本的方式替代行为测试。
4. 本地至少执行与改动相关的 Gradle 任务。完整参考见 [构建与测试](docs/development/build.md)。
5. 文档按职责更新：用户说明在 README，架构在 `docs/architecture/`，操作手册在 `docs/development/`，长期实现参考在 `docs/reference/`。
6. 提交 PR 后，以 GitHub Actions 的真实编译、Lint、JUnit 和 emulator 结果为准。

## 常用验证

```bash
./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
./gradlew :app:assembleDebug
```

CI 还使用 actionlint、ShellCheck、zizmor、markdownlint-cli2 和 Lychee 检查工作流与文档。这些通用检查应由社区工具维护，不在仓库里复制一套 Python 文本匹配测试。

## 代码放置

- 共享业务代码：`app/src/main/java/com/huangder/lumibooks/`
- 社区版实现：`app/src/oss/java/com/huangder/lumibooks/`
- UI 继续按 feature 放在 `ui/<feature>/`；`edition/` 只承担版本能力适配，不承载大型页面或业务实现。
- 数据访问放在 `data/`，稳定业务模型/接口放在 `domain/`，阅读日志等独立业务核心放在现有 feature/domain 包。
- 测试镜像生产代码的职责，优先验证公开行为、持久化结果和 UI 语义，而不是实现文本。

## 发布相关改动

不要在 PR 中加入签名材料，也不要修改工作流去读取 PR Secrets。Beta 发布只由成功的 `main` CI 触发，详细流程见 [发布指南](docs/development/release.md)。

## 安全问题

安全漏洞不要携带利用细节提交到公开 Issue；见 [SECURITY.md](SECURITY.md)。
