## 目的与用户场景

<!-- 说明原问题、预期用户行为、相关 Issue/PR；说明与上游公共版的边界 -->

## 实现与风险

- [ ] 沿用现有 UI 令牌和 GroupedCard / Sheet / Dialog；未增加平行视觉组件
- [ ] 本地优先；数据、网络、隐私与取消/撤销语义均清楚
- [ ] 关键隐私和破坏性操作带有确切的确认、错误与幂等路径
- [ ] 兼容已存储数据、旧设备和无权限/离线场景

## 对齐与测试证据

- [ ] `python3 tools/check_community_policy.py` / `python3 tools/ci_test_inventory.py`
- [ ] `./gradlew :app:compileDebugUnitTestKotlin`（本地测试源码编译）
- [ ] `./gradlew :app:testDebugUnitTest`（或指明覆盖的 CI reader/ui/core 分片）
- [ ] `./gradlew :app:assembleDebug`，安装 OSS APK 验证
- [ ] Lumi / Material 3 / Liquid Glass 的核心信息结构与交互一致；附代表性截图或短视频
- [ ] 320dp 窄屏、fontScale 1.3/2.0、浅/深主题、RTL/横屏可用，操作文案未裁切
- [ ] TalkBack 朗读、语义角色/焦点顺序、最小 48dp 新增操作目标、系统返回/取消符合预期
- [ ] 加载/空/失败/重复点击/网络离线/SAF 取消/破坏性确认的交互状态覆盖
- [ ] 说明需要真机检查的 Glass 视觉、E-ink、复杂阅读手势和长列表性能
- [ ] 失败测试的日志与复现方式链接已提供；未用跳过或放宽断言制造假绿灯

## 可复现的验收矩阵

| 场景/设备 | 主题/字体 | 操作步骤 | 预期行为 | 证据链接/结果 |
| --- | --- | --- | --- | --- |
| | | | | |

## 关联资料

- 本仓库：[UI 规范](../devdocs/lumi-ui-motion-design-system.md) · [社区交互与测试契约](../devdocs/community-ui-ux-implementation.md) · [CI 设计](../devdocs/community-ci-test-design.md)
- [Android Compose 测试](https://developer.android.com/develop/ui/compose/testing) · [Android 官方可访问性](https://developer.android.com/guide/topics/ui/accessibility/apps)

**原则**：CI 不通过不合并；设备验收未完成则标明风险，保持 Draft。
