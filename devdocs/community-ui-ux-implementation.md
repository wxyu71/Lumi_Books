# Lumi Community · UI 与人机交互实现规范

> 本文针对社区扩展 PR #1，补充而不替代 `devdocs/lumi-ui-motion-design-system.md`（2026-10-27 现行基线）。设计优先级：用户明确要求 → 上游现行 UI 规范 → 公共组件代码 → 旧文档。不能因为社区改造复制新的视觉组件。

## 基础视觉规则

- 三套主题 Lumi / Material 3 / Liquid Glass 共用业务页面和交互状态，不复制布局；通过 `AppColors`、`AppType`、`AppSpace`、`AppRadius` 和 `AppPresentation` 实现响应性样式。
- 设置列表复用 `GroupedCard`、`GroupedRowDivider`、`LumiRow`；书籍/标签/日志展示是稳定内容材质，不额外叠玻璃。弹层使用 `AppBottomSheet`、`AppSheetHeader`、`LiquidGlassAlertDialog`、`AppDialogAction`，在 Liquid Glass 保留合法光学效果和对比度。
- 44dp 及以上触控目标；标题使用 `AppType.Section` 等系统字阶，错误使用 `AppColors.Destructive`，不手写一套设计色表。低动效、电子墨水和 RTL 回退交给公共组件。
- 信息分层：普通操作直接完成，可能影响全局数据的操作需二次确认；Dialog 的关闭不等价于向前导航；不得在数据写入期间可重复点击造成双重提交。

## 标签管理

**入口**：设置中的书籍标签和标注标签入口、跨书笔记列表标题栏和单书笔记标题栏。界面使用 `CommunityTagManagementSheet`，绝不重新创建标签数据库。

**分组语义**：

- 未分组永远是一个可见的列表分组；新增标签默认在未分组，用户移动时可选择已有分组或未分组。
- 组管理重命名只改变组名；删除组解除关联并保留组内标签/书籍/笔记。组名冲突、非法名称保留编辑态并反馈可见错误。
- 标签重命名和删除经 `TagManagementRepository` 原子事务更新各实体，失败不乐观移除 UI；删除标签须明确说明会影响所有已关联记录，不可撤销。
- 批量选择时仅改变内存的本地选择集；保存才写数据库。后台同步/全局重命名删除应剔除已经失效的 key。移动和删除支持批量操作，进行中禁用二次提交。
- 大字体下按钮文字不可强制截断关键动词；无障碍应准确描述“选择标签、重命名、删除、移动”。需真机回归验证 TalkBack、RTL、复杂分组列表和窗口变化。

## 阅读日志与海报

- 原始事件的持久化走 `ReadingJournalRepository` / `ReadingJournalRecorder` / Room；禁用时不记录活动，`OssAccess.start()` 仅启动观察者。无账号、无遥测、无社区数据服务器。
- 首次启用弹出明确说明：本机保留阅读时段与关联书籍，用户主动发起的备份和 WebDAV 同步可能包含日志；未确认不开始记录。
- 停止：保留现有事件但不追加；清除：另有毁灭性确认且只删日志，不删书籍、进度、标注。
- 可选保留期限 3 个月、1 年、永久；缩短期限会导致立即删除过期日志，UI 应说明这一点（正式发布前需补充警示与回归）。
- 通过 Room `ORDER BY ... LIMIT 300` 查询最近事件，避免先加载整库再裁剪；书籍详情改为独立的 `observeRecentForBook()` 查询，避免一本书较早的日志因其他书更近的 300 条而消失。按本地日期分组，隐藏书籍不展示；完整记录仍按原保留策略储存在 Room。
- 点击有阅读时长的事件进入既有 `BookShareActivity` CHECK_IN 预览与分享，不新增云端生成引擎、不复制图片渲染算法。无数据时显示状态说明，不制造虚假签到。

## 笔记导出

- **单书**：保留既有 TXT 和 BookNotesExportBuilder；新增 Markdown，标题/章节/标注/笔记/标签/书签从同一快照生成。
- **跨书**：“全部标注与笔记”的下载入口只处理用户当前搜索词、类型、标签/颜色等筛选结果。多个书籍依现有 `notesExportBooks` 保持首见书籍顺序，隐藏书籍与墨迹绘制过滤与原有业务一致。
- 用户先选 TXT / Markdown，再调用 Android Storage Access Framework 创建文件，只有系统返回 URI 后写入；取消选择不会生成或上传临时文件。失败显示错误，按钮防重复；大量导出应允许取消。
- Markdown 仅转义用户内容中的语法特殊字符，防止标签与摘录文本误成标题、表格、链接或 HTML；保留原文换行与 UTF-8。不得将笔记发送至外部格式转换服务。

## 防回归与真机验收矩阵

| 场景 | 期望 |
|---|---|
| Lumi / Material 3 / Liquid Glass | 同一信息与操作结构；仅材质差异 |
| Android 低 API、e-ink、降低动效 | 不依赖背景模糊，按钮、Sheet、内容可见 |
| 大字号 / 横屏 / 折叠屏 / RTL | 分组和按钮可达、文案不裁切、布局不抖动 |
| 标签重名、空名、同步时标签消失 | 保留编辑态和错误；选择集与实际数据一致 |
| 删除标签与删除分组 | 影响范围说明不同；取消不改数据 |
| 日志首次启用、停止、彻底清理 | 必须确认启用，停止不丢历史，清理不删进度 |
| 日志保留期限缩短 | 应明确告知可能删除历史（待真机验证） |
| 笔记跨书筛选、隐藏书、长文本、无条目 | 只导出筛选项；不泄漏隐藏书 |
| SAF 取消、无写入权限、容量不足 | 不丢原数据，提示失败，可重试 |
| 任意阅读过程中网络抓包 | 无新增社区服务器、无后台遥测；第三方联网仅按用户选择 |

## 社区反馈与设计判断

Fork 的 Issues/Discussions 当前关闭，不能把上游工单称为 Fork 社区投票。当前可核对的上游公开反馈包括：

- [#42 液态玻璃菜单展开/关闭卡顿](https://github.com/huangder/Lumi_Books/issues/42)：组件必须复用现有 Glass Host、能力回退与静态内容表面；本扩展不写第二套背景采样和手势算法。
- [#47 长章节标题右侧被截断](https://github.com/huangder/Lumi_Books/issues/47)：大字号及小屏幕下，优先自动换行和垂直增长，不能截断关键操作名称。
- [#54 纵向阅读误触左右翻页](https://github.com/huangder/Lumi_Books/issues/54)：避免在日志时间线、分组列表和导出确认弹层新增全屏水平手势，与阅读器现有手势隔离。
- [#40 自定义选中文字菜单](https://github.com/huangder/Lumi_Books/issues/40)及[相关实现 #41](https://github.com/huangder/Lumi_Books/pull/41)：保留全局配置驱动的阅读器菜单顺序，不在标签 UI 中重新定义另一套排序或长按协议。
- [#56 后台音乐切歌打断 TTS](https://github.com/huangder/Lumi_Books/issues/56)：属于音频焦点问题；不应通过阅读日志记录/清理操作自行恢复 TTS 或改变音频焦点。

这些是具体用户场景和工程线索，不代表无统计依据的社区共识。需求依据还包括本 PR 的用户要求、项目 UI 规范及现有 Repository 测试。

新功能先保持在 Draft PR 中，由 CI、QA 与用户体验验收后合并；定时上游同步只提 PR，不得自动恢复已删营销、私有服务器和会员限制。
\n## 隐私并发与 Markdown 导出边界（扩展验收）\n\n- 日志记录与里程碑必须比较当前同意周期 `enabledAt`，避免“停止/清除后再次启用”时旧协程重新写入事件。清理保留期限时同步清空可能引用已删除记录的会话缓存。\n- Markdown 用户原文不可以被解析成意外的有序/无序列表：引用块和多行笔记的行首标记必须转义；保留用户的换行与 Unicode。\n
- 标签管理列表采用懒加载分组，降低同步后存在大量分组时的首屏组合成本；每个分组内部仍保留一个连续的 `GroupedCard` 和插入式分隔线，不分裂成视觉上互不相关的浮动卡片。极端单组超大标签数仍需真机压力测试。

## 社区实施质量契约（2026-11-21）

参考 [Now in Android 的测试策略](https://github.com/android/nowinandroid/wiki/Testing-strategy-and-how-to-test)、[AnkiDroid 的 UI 贡献原则](https://github.com/ankidroid/Anki-Android/wiki/User-Interface-contribution)、[Android 官方 Compose 测试](https://developer.android.com/develop/ui/compose/testing)、[Android 无障碍指南](https://developer.android.com/guide/topics/ui/accessibility/apps) 与 [截图测试指南](https://developer.android.com/training/testing/ui-tests/screenshot)。这些用于校验方法和交互约束，不照搬其他产品的视觉风格。

### 可检查的契约与责任边界

| 合约 | 自动化（PR 必须通过） | 真机验收（发布前） |
| --- | --- | --- |
| 隐藏书籍保密 | `AnnotationFeedTest.hiddenBooksNeverEnterTheCrossBookFeedOrItsSearchAndExport` + `NotesExportSourceTest` | 隐藏书籍后，全局标注列表、搜索、标签筛选和导出均不得出现隐藏内容；重新解锁后只能在授权路径出现 |
| 筛选导出正确性 | `NotesExportSourceTest` + `BookNotesExportFormatterTest` | TXT / Markdown 使用当前筛选结果；系统 SAF 取消不写入；长文本、Unicode、换行保留 |
| 日志授权与保留 | `ReadingJournalCoreTest` + 现有 Repository/Recorder 测试 | 默认不开启；明确同意后记录；停用保留历史；清空须二次确认，且旧异步写入不能复活 |
| 主题与阅读交互 | 当前 reader/ui JVM 回归 + device smoke 的 Liquid Glass 控件测试 | 三主题的信息结构、触控语义一致；Glass 硬件渲染只能设备验收，不能用 JVM 软件位图冒充 |
| 语义与自适应 | 保留 Compose 语义而非依赖节点坐标，回归复用现有组件 | 中文/英文、fontScale 1.0/1.3/2.0、窄屏 320dp、横屏/RTL、TalkBack、系统返回、取消与错误状态 |

### 交互、可访问性和视觉基准

- **新建社区操作按钮的有效点击目标至少 48×48dp**，符合 Android 官方触控可访问性建议。原项目的 44dp 约束作为历史视觉下限保留，增大的是交互目标/可增长的容器，而不是任意改写全局圆角与视觉令牌；本 PR 的 `TagManagerAction` 强制最小高度 48dp。
- 测试先验证语义树中的角色、可用状态、选中状态、确认/取消分支、焦点可达性，再验证主题视觉。不得仅凭截图判定数据删除和授权流程成功。
- 对每个高风险变更保留三主题的同场景截图，固定 Android API、分辨率、系统字体、时区、语言和动画设置；动态 Material 色与 Glass 背景捕获须统一测试种子/样本，否则使用人审差异。截图基线需人工审查更新，不能在失败时自动覆盖基准图。
- 验证浅色/深色、Lumi/Material3/Glass、正常/大字号、正常/RTL 的**成对覆盖**，不要求每一维度笛卡尔积；任何影响交互位置或触控目标的改动必须增加至少一组完整设备场景。
- 尽量使用 Compose test semantics、`waitUntil` 或空闲同步替代固定 sleep；动画的几何/光学断言采用与设备/渲染器一致的可解释容差，而不是全局放宽像素误差。
- 删除组/删除标签/清空日志/保留期限缩短/批量导出都要覆盖取消、连续点击、异常重试以及重新打开后的状态一致性；绝不能通过暂时禁用测试掩盖真实回归。
- 设备烟雾测试与耗时 benchmark 继续放在周度或手动流程；未获得真实硬件测试数据时，不对应用流畅性或电耗做量化承诺。

### 合并决策

优先修复 **编译与真实测试失败 → 数据泄漏与破坏性操作 → 无障碍/视觉回归 → 性能基线**。只有 Required CI summary 全绿、关键隐私路径有回归测试、Android 真机关键路径完成验收后才将 Draft 置为 Ready；没有真机验证时在 PR 明示缺口而非推定通过。
