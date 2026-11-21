# 文档导航

本 Fork 由一人维护。这里按**用途**查资料，不把 CI 运行日志、Agent 交接和历史任务备忘当作架构文档。项目介绍与安装入口在 [README](../README.md)，Fork 的功能边界在 [COMMUNITY](../COMMUNITY.md)，Agent 行为规则在 [AGENTS](../AGENTS.md)。

## 开发操作（How-to）

| 文档 | 何时阅读 |
| --- | --- |
| [本地构建](development/build.md) | 准备 Android 环境、修改源码、运行测试 |
| [CI 检查](development/ci.md) | 定位 PR 失败的具体检查，理解 Required Gate |
| [发布个人 Beta](development/release.md) | 改版本、合并、从 GitHub Pre-release 下载 APK |

## 长期技术参考（Reference）

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

真实行为最终以代码、测试和 Actions 为准。需要调查一次故障时优先使用 PR/Issue/Actions 和提交历史，不新增 `handoff-*.md`、`project-status.md` 或“最终设计”文件。该索引只列仍在维护的资料，避免一次变更在多个地方重复记载。
