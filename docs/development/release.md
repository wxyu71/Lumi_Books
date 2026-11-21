# 发布个人 Beta（GitHub Pre-release）

本仓库仅一人维护，不申请 Release Environment，不保管长期 keystore，也不人工推送 Tag。发布工作流是 `.github/workflows/android-release.yml`；下载页面是 [本 Fork 的 Releases](https://github.com/wxyu71/Lumi_Books/releases)。

## 操作步骤

1. 在功能分支上修改 `gradle.properties`：`lumi.communityVersionName=X.Y.Z-beta.N`，`lumi.communityVersionCode` 递增；同时更新 `app/src/main/assets/changelog.md`。
2. 将版本改动正常更新到 `main`（可合并已验证 PR，或由维护者直接推送）；`main` 的 Android CI 仍必须成功。不要按文件逐次 push，也不必额外创建 Tag。
3. `main` 的 `Android Community Beta` 工作流全绿后，发布工作流自动检查**刚通过的精确提交 SHA 仍为 main HEAD**、版本格式及已有 Release。如果同版本已经发布就跳过。
4. 对新增版本构建未签名的优化型 `:app:assembleRelease`，CI 当场生成临时签名密钥、签 APK、验证包名/版本/签名及 `SHA256SUMS.txt`，随后自动创建 `vX.Y.Z-beta.N` Tag 和 GitHub **Pre-release**。
5. 下载 `Lumi-Community-vX.Y.Z-beta.N-universal.apk`，解压/下载校验文件并可执行 `sha256sum -c SHA256SUMS.txt`。如 Actions 失败，先修正代码和验证结果；不创建人工 Release 掩盖失败。

## 重要限制：临时签名不支持覆盖更新

**每一个 Beta 使用不同签名证书。** Android 要求更新 APK 与已安装包具有匹配签名，所以本流程发布的下一版无法直接覆盖前一版。

在应用中先**备份阅读数据**（包含书籍和书签/笔记等必要内容），确认备份可用后，再卸载旧版并安装新版。卸载通常删除私有应用数据。应用内“检查更新”只提示新版本并打开本 Fork 的 GitHub Releases，**不承诺一键覆盖安装**。

如果将来需要无损覆盖升级，再按需要改为可重复使用的签名身份；对单人个人测试没有必要现在维护四个 Secrets、证书指纹变量或复杂发布审批。

## 权限和故障处理

发布只接受本仓库 `main` 上成功的 `push` CI，不能由外部 PR 直接启动。GitHub Actions 发布 Job 使用 `contents: write` 创建 Tag/Release，普通 CI 仍是只读权限。不使用 `[skip ci]`、GitHub Pages、原作者镜像或者第三方网盘。
