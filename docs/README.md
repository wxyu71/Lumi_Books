# 文档导航

项目介绍与安装入口在 [README](../README.md)，Fork 身份与功能边界在 [COMMUNITY](../COMMUNITY.md)。贡献流程见 [CONTRIBUTING](../CONTRIBUTING.md)，安全报告见 [SECURITY](../SECURITY.md)，自动化 Agent 约束见 [AGENTS](../AGENTS.md)。

## Architecture

| 文档 | 内容 |
| --- | --- |
| [架构总览](architecture/overview.md) | 模块/source set、代码分层、Community feature 放置、测试和发布边界 |

## Development

| 文档 | 何时阅读 |
| --- | --- |
| [本地构建](development/build.md) | 准备 Android 环境、修改源码、运行真实 Gradle 测试 |
| [CI](development/ci.md) | 理解检查分层和失败定位 |
| [发布 Community Beta](development/release.md) | 版本、固定签名、两阶段 Pre-release 发布 |

## Reference

| 领域 | 当前实现与边界 |
| --- | --- |
| [UI 与动效](reference/ui-motion.md) | 主题、视觉令牌、组件和交互模式 |
| [社区扩展 UI](reference/community-ui.md) | 标签、阅读日志、导出的人机交互约束 |
| [Glass 采样](reference/glass-sampling.md) | 图层采样模式 |
| [Kindle 转换](reference/kindle-native.md) | libmobi JNI 边界 |
| [EPUB 双页](reference/epub-spreads.md) | 原排版布局 |
| [PDF/漫画载入](reference/raster-loading.md) | 渲染与内存约束 |
| [WebDAV 内容身份](reference/webdav-identity.md) | 同步兼容与对象身份 |
| [Markdown 阅读](reference/markdown-reading.md) | 本地渲染与存储 |
| [PDF 文字标注](reference/pdf-annotations.md) | PDF 选词与标注路径 |

长期文档只描述仍在维护的架构、操作和参考。单次故障、handoff、临时研究和 CI 运行证据留在 Git 历史、PR、Issue 与 Actions；不要为一次任务新增平行的“最终设计”或状态文件。
