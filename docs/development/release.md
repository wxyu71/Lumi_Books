# 发布 Community Beta

Community Beta 使用固定签名和 GitHub Pre-release。触发链路是：**新版进入 `main` → Android Community Beta CI 成功 → release workflow 自动运行**。不需要手工 Tag，也不会在 PR 中读取发布密钥。

## 首次配置

在可信电脑、仓库目录之外创建并长期备份同一份 PKCS12：

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

将 PKCS12 Base64 编码后配置两个 Repository Secrets：

| Secret | 内容 |
| --- | --- |
| `LUMI_BETA_KEYSTORE_BASE64` | PKCS12 文件的完整 Base64 |
| `LUMI_BETA_KEYSTORE_PASSWORD` | PKCS12 存储/私钥密码 |

Base64 只是编码，不是加密。不要把原始密钥、Base64 或密码提交到 Git、Issue、PR、聊天或 Actions 日志。原始 PKCS12 和密码必须安全备份；以后不能重新生成“同名密钥”替代。

## 发布工作流的权限边界

Beta 发布不再二次构建 APK。主 CI 的 P3 `release` job 在 JVM、Lint 通过后构建唯一的 unsigned universal Release APK，用 `aapt` 核对包名/版本，并在同一 Actions run 中上传名为 `lumi-community-unsigned-release` 的 1 天不可变 artifact。

完整 gate（包括 API 33/35 emulator）通过后，caller 才调用 `.github/workflows/android-release.yml`：

1. **Verify gated Release artifact**：`contents: read` + `actions: read`，没有签名 Secrets。它检出同一 `github.sha`，确认仍是最新 `main`、版本/Tag 可发布，然后下载同一 run 的 P3 artifact。
2. **Sign and create GitHub Pre-release**：只有 verify 成功且确实需要发布时才运行。它再次下载同一个不可变 artifact，才读取固定签名 Secrets，执行 `zipalign` / `apksigner`、验证 APK 元数据并生成 `SHA256SUMS.txt`，最后使用 `contents: write` 创建 Tag 和 Pre-release。

reusable release workflow **完全不运行 Gradle**；最终被签名的就是完整主 CI 已经构建、验证并通过后续 gate 的 Release artifact。

## Beta1 发布前检查

发布 `2.3.12-beta.1` 前确认：

- `gradle.properties` 的 `lumi.communityVersionName` 为目标 `X.Y.Z-beta.N`，`versionCode` 严格高于所有已安装 Community Release；
- `app/src/main/assets/changelog.md` 只描述本次可见变化，且没有 Premium/上游付费营销；
- 两个固定签名 Secrets 已配置，原始 PKCS12 已离线备份；
- 当前 PR 的编译、JVM/Robolectric、Lint、Debug/Diagnostic/Release 和 API 33/35 emulator 全部通过；
- 首次从 Debug/临时签名包迁移的设备已备份数据，明确需要卸载旧签名版本后再安装 Beta1。

## 发布与升级

合并/推送目标版本到 `main` 后，成功的 main CI 会触发发布。工作流只允许仍然是最新 `main` 的已验证 SHA 发布，并拒绝复用已经存在但不匹配的 Tag。

最终 Release 包含：

- `Lumi-Community-vX.Y.Z-beta.N-universal.apk`
- `SHA256SUMS.txt`

相同 applicationId、相同固定签名且更高 `versionCode` 的后续 Beta 通常可直接覆盖升级。Debug 或旧临时密钥签名包与固定 Community Beta 签名不兼容。
