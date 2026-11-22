# 构建与测试

这是 `wxyu71/Lumi_Books` 的独立 OSS Fork，应用 ID 为 `io.github.wxyu71.lumibooks`。代码布局先看 [架构总览](../architecture/overview.md)。

## 环境

CI 使用 Ubuntu 24.04、JDK 21、Android SDK API 37、Build Tools 37.0.0、NDK 28.0.13004108、CMake 3.22.1。根目录 Gradle Wrapper 是构建入口。

## 常用命令

```bash
# 最快的源码/测试源码编译反馈
./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin

# JVM + Robolectric 行为测试
./gradlew :app:testDebugUnitTest

# Android 官方静态分析
./gradlew :app:lintDebug

# 可安装调试 APK
./gradlew :app:assembleDebug

# Community 发行边界（不是行为测试）
python3 tools/check_community_policy.py
```

Android APK 位于 `app/build/oss/outputs/apk/`。本地 `testDebugUnitTest` 不注入 CI 的 modern SDK 限制；CI 的 PR lane 使用 API 33/35/36，历史显式 SDK 用例由每周兼容性 lane 运行。

不要通过新增 Python 源码/YAML 字符串断言来“测试 CI”。工作流、Shell、Markdown 和链接分别由 actionlint、ShellCheck、zizmor、markdownlint-cli2 和 Lychee 验证；产品行为由 Gradle/JUnit/Robolectric/instrumentation 验证。
