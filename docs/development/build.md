# 构建与测试

这是 `wxyu71/Lumi_Books` 的**独立 OSS Fork**，唯一的 app ID 是 `io.github.wxyu71.lumibooks`。本仓库固定使用 `app/src/oss/`，不需要原作者私有 Premium 仓库。

## 环境

CI 使用 Ubuntu 24.04、JDK 21、Android SDK API 37、Build Tools 37.0.0、NDK 28.0.13004108、CMake 3.22.1。开发者本地使用这些版本可以最大限度复现 CI。Gradle Wrapper 在根目录。

## 常用命令

```bash
python3 tools/check_community_policy.py
python3 tools/ci_test_inventory.py
python3 -m unittest discover -s tools/tests -p 'test_*.py'
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```

Android APK 通常位于 `app/build/oss/outputs/apk/debug/`。CI 在 APK 打包前先编译并运行 Android 13+（API 33/35/36）JVM 分片；历史 SDK 测试保留在每周扩展 CI。**本地 `testDebugUnitTest` 仍是原有完整测试套件**，不会因 PR 的 SDK 筛选丢失旧版行为验证。涉及 Android 仪器测试时参考 [CI 检查](ci.md)。

版本名和版本号以根目录 `gradle.properties` 为准。由于改用单人发布模式，不再维护官方/社区双编译分支，也不再把旧的 `devdocs/editions.md` 当作操作依据。
