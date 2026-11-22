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

`.github/workflows/android-release.yml` 分为两个 job：

1. **Prepare verified unsigned Beta**：`contents: read`，没有签名 Secrets。它检出已经通过 main CI 的精确 SHA，确认该 SHA 仍是 `main`、版本未发布，构建 unsigned Release，使用 `aapt` 验证包名/版本，并把 APK 与发布说明上传为 1 天短期 artifact。
2. **Sign and create GitHub Pre-release**：只有第一阶段成功且确实需要发布时才运行。它下载前一阶段 artifact，安装签名工具，读取固定签名 Secrets，执行 `zipalign` / `apksigner`、再次验证 APK 元数据并生成 `SHA256SUMS.txt`，最后才使用 `contents: write` 创建 Tag 和 Pre-release。

第二个 job **不重新运行 Gradle**；被签名的就是前一只读 job 构建并验证过的 artifact。

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
