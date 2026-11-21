# 发布个人 Beta（固定签名、GitHub Pre-release）

本仓库由一人维护。保持现有便捷发布方式：**新 Beta 版本更新到 `main` → 正常 Required CI 全绿 → 自动构建、签名并创建 GitHub Pre-release**。不需要人工 Tag、二次完整 CI 或额外发布审批 Environment；但要首次配置**固定签名**。工作流在 `.github/workflows/android-release.yml`，下载地址是 [本 Fork 的 Releases](https://github.com/wxyu71/Lumi_Books/releases)。

## 首次配置（只做一次）

在自己可信的电脑上准备 JDK，**在仓库之外**运行：

```bash
keytool -genkeypair \
  -keystore lumi-community-release.p12 \
  -storetype PKCS12 \
  -alias lumi-community \
  -keyalg RSA \
  -keysize 3072 \
  -validity 10000 \
  -dname "CN=Lumi Community"
```

按提示设置密码。PKCS12 文件使用同一个存储/私钥密码；工作流中别名固定为 `lumi-community`。推荐使用 ASCII 密码，减少跨平台签名工具的编码问题。**不要在聊天、Issues、提交、PR 或 Actions 日志中共享私钥和密码。**

再把文件转为 Base64（这是编码，不是加密）：

```bash
python3 -c "import base64; from pathlib import Path; print(base64.b64encode(Path('lumi-community-release.p12').read_bytes()).decode())"
```

在 [仓库 Actions Secrets 设置](https://github.com/wxyu71/Lumi_Books/settings/secrets/actions) 新建两个 **Repository secrets**：

| Secret | 值 |
| --- | --- |
| `LUMI_BETA_KEYSTORE_BASE64` | 上一条命令的完整 Base64 内容 |
| `LUMI_BETA_KEYSTORE_PASSWORD` | 创建 PKCS12 时输入的密码 |

**不要上传 `.p12` 到仓库**，不用配置 `beta-release` Environment、单独的 alias Secret 或证书指纹变量。CI 只在成功的 `main` 发布任务中解码临时密钥文件，使用 `zipalign` / `apksigner` 签名，验证 APK 和 SHA-256 后发布。

将原始 `lumi-community-release.p12` 文件及其密码至少分别安全备份一份。**以后要重用这份原密钥，而不是重新运行 `keytool` 生成“同名密钥”。**

如需检查证书公钥指纹（非机密；无需上传成变量）：

```bash
keytool -list -v -keystore lumi-community-release.p12 -storetype PKCS12 -alias lumi-community
```

## 每次发布

1. 在功能分支或 `main` 中更新 `gradle.properties` 的 `lumi.communityVersionName=X.Y.Z-beta.N`，并严格递增 `lumi.communityVersionCode`；同步更新 `app/src/main/assets/changelog.md`。
2. 正常推送或合并到 `main`；`Android Community Beta` 的完整 Required CI 全绿后自动触发发布 Job。
3. 发布 Job 验证该成功的 SHA 仍是 `main` HEAD，且该版本未曾发布。它构建优化后的 OSS Release APK，使用**上面配置的同一签名密钥**，自动创建 `vX.Y.Z-beta.N` Tag 和 GitHub Pre-release。
4. 在 [Releases](https://github.com/wxyu71/Lumi_Books/releases) 下载 `Lumi-Community-vX.Y.Z-beta.N-universal.apk` 和 `SHA256SUMS.txt`；可用 `sha256sum -c SHA256SUMS.txt` 校验。
5. 下一次发布仍用相同密钥、相同 `applicationId` 和更高 `versionCode`，通常能在 Android 上直接覆盖安装旧 Community Release Beta，保留本地应用数据。

**一次性迁移注意**：如果手机已安装先前用 Debug 密钥或临时密钥签发的测试包，它**不能**直接覆盖升级到这个新固定密钥版本。第一次迁移前先备份书籍、笔记及设置，再卸载旧包安装新包；之后固定签名的版本即可正常覆盖升级。

## 缺少 Secret 或 CI 失败

任何一个 Secret 缺失、密码错误、签名不成功或 APK 信息不匹配，发布任务会失败，**不会发布未签名或临时签名的安装包**。普通 PR CI 不读取密钥且继续保持只读权限。如首次设置晚于某轮成功的 `main` CI，需要在配置后对**尚未发布的新版**正常触发一次 `main` CI；不使用空提交绕过版本控制，也不要手工创建未验证 Release。
