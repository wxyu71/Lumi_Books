# 安全策略

## 支持范围

当前维护中的 Lumi Community Beta 和 `main` 分支接受安全修复。上游官方版本的问题请报告给上游项目。

## 报告漏洞

请优先使用 GitHub 仓库的 **Security → Report a vulnerability** 私密报告入口（如果该入口在仓库中可用）。不要在公开 Issue、PR、Actions 日志或讨论区粘贴以下内容：

- 可直接利用的漏洞细节或 PoC；
- API Key、WebDAV/TTS/翻译凭据；
- Community Beta 签名私钥、PKCS12 内容或密码；
- 用户书籍、笔记、备份或其他私密数据。

如果 GitHub 私密漏洞报告入口不可用，请只创建一个不含敏感细节的最小公开 Issue，请求维护者建立私密沟通渠道。

## 签名与发布密钥

Community Beta 使用固定签名身份以支持后续覆盖升级。签名密钥只应存在于维护者的离线备份和 GitHub Actions Secrets 中。发现泄露时应立即停止发布、撤销/轮换相关凭据，并在发布说明中明确迁移影响。

## 依赖与 CI

工作流使用最小权限并将第三方 Action 固定到不可变 commit SHA。通用工作流安全检查由 actionlint 与 zizmor 执行；Android 依赖和应用行为仍以 Gradle、Android Lint 和实际测试结果为准。
