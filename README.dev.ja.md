<!-- Language: ja | Audience: dev -->

<div align="center">

<img src="docs/media/lumi2.webp" alt="Lumi" width="96">

# Lumi 開発者ガイド

Android 向けのローカル電子書籍リーダーの技術説明です。コントリビューターと二次開発者を対象にしています。

[![Version](https://img.shields.io/badge/version-2.3.12--beta.1-coral)](https://github.com/wxyu71/Lumi_Books/releases)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue)](LICENSE)
[![Android](https://img.shields.io/badge/Android-8.0%2B-blue)](https://developer.android.com)

[中文](README.dev.md) · [English](README.dev.en.md) · **日本語** · [한국어](README.dev.ko.md)

[ユーザー版](README.ja.md) · **開発者版**

</div>

## 技術スタック

| 分類 | 技術 | バージョン |
|------|------|------|
| 言語 | Kotlin | 2.3.10 |
| ビルド | Android Gradle Plugin | 8.13.2 |
| UI | Jetpack Compose（BOM） | 2026.06.01 |
| 依存性注入 | Hilt | 2.58 |
| データベース | Room | 2.8.4 |
| 設定の保存 | DataStore Preferences | 1.1.1 |
| ナビゲーション | Navigation Compose | 2.8.9 |
| コルーチン | kotlinx-coroutines-android | 1.9.0 |
| バックグラウンド処理 | WorkManager | 2.10.0 |
| EPUB 出版社レイアウトの描画 | AndroidX WebKit | 1.16.0 |
| HTML 解析 | Jsoup | 1.22.2 |
| PDF 解析 | pdfbox-android | 2.0.27.0 |
| Markdown | CommonMark | 0.24.0 |
| ネットワーク | OkHttp | 4.12.0 |
| 画像読み込み | Coil | 2.7.0 |
| ぼかしとガラス効果 | Haze 1.1.1、Backdrop（`third_party/backdrop`） | — |
| SDK | minSdk 26、compileSdk 37、targetSdk 37 | — |
| JVM | Java 17 | — |

## アーキテクチャ

アプリは MVVM + Repository アーキテクチャを採用し、依存性注入には Hilt を使っています。

```
ui/        Compose の画面と ViewModel：home、reader、settings、statistics、bookshelf など
domain/    ドメインモデルとリポジトリのインターフェース
data/      Room、DataStore、リポジトリの実装、WebDAV / OPDS などのリモートデータソース、バックアップと同期
util/      パーサー（EPUB、PDF、TXT、Markdown、Word、Kindle）、組版とキャッシュのユーティリティ
tts/       システム音声と外部音声インターフェース
widget/    ホーム画面ウィジェット
edition/   公開版と公式版の間の機能インターフェース
```

- **画面**：`MainActivity` は Navigation Compose（`ui/navigation/NavGraph.kt`）で主要な画面を管理します。ウェルカム画面、全画面の読み上げ、読み上げ設定などは独立した Activity です。
- **依存性注入**：`EBookReaderApp` には `@HiltAndroidApp` が付いています。ViewModel は `@HiltViewModel` を使い、モジュールは `di/` にあります。
- **データベース**：Room の現在のバージョンは 28 です。すべてのマイグレーションは `di/AppModule.kt` で `addMigrations` により登録されています。テーブル構造を変更するときは、バージョン番号とマイグレーションを両方追加してください。破壊的マイグレーションに頼らないでください。
- **設定と機密情報**：ユーザー設定は DataStore に保存されます。WebDAV、翻訳、音声、MinerU の Token などの認証情報は、Android KeyStore で暗号化して保存されます。
- **リーダー**：EPUB の出版社レイアウトは WebView で描画され、出版社の CSS とフォントを保持します。リーダーレイアウトは Canvas + StaticLayout で描画され、フォントと組版を統一します。

## プロジェクト構成

```
android_books/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/            共通のコードとリソース
│       │   ├── java/com/huangder/lumibooks/
│       │   ├── assets/      内蔵の HTML、フォント、辞書、更新ログ
│       │   └── res/         リソース（表示言語 7 種類）
│       ├── oss/             公開版の機能実装
│       ├── premium/         公式版の非公開ソース（Git の管理対象外。別リポジトリ。下記参照）
│       ├── test/            ローカルの単体テスト
│       ├── androidTest/     計測テスト
│       └── debug/           デバッグ用のエントリポイント
│
├── benchmark/               起動とスクロールのパフォーマンステスト
├── third_party/             リポジトリで管理する外部モジュール（Backdrop、字幕ビュー、Word 変換など）
├── tools/                   EPUB とリーダーのプローブ、テストスクリプト、補助ツール
├── devdocs/                 開発ドキュメント
├── docs/                    プロジェクトサイト（GitHub Pages）
└── .github/                 Pages のデプロイワークフローと Issue テンプレート
```

## ビルド

### 環境

- Android Studio（最新の安定版）またはコマンドライン
- JDK 21
- Android SDK：compileSdk 37 とそれに対応する build-tools
- コマンドラインでビルドする場合は `ANDROID_HOME` を設定してください。Windows では `gradlew.bat` を使います。

### 公開版

このリポジトリだけをクローンした場合、ビルド結果は公開版になります。

```bash
./gradlew :app:assembleDebug
# APK：app/build/oss/outputs/apk/debug/
```

よく使うチェックコマンド：

```bash
./gradlew :app:compileDebugKotlin                 # Kotlin のコンパイル
./gradlew :app:testDebugUnitTest                  # ローカルの単体テスト
./gradlew :app:compileDebugAndroidTestKotlin      # 計測テストのコンパイル確認
```

### コミュニティ版のビルドと Beta 配布

本フォークは公開 `app/src/oss/` のみを使用し、アプリ ID は `io.github.wxyu71.lumibooks` です。上流の非公開 Premium リポジトリや元作者の署名鍵には依存しません。

`./gradlew :app:assembleDebug` はローカル検証用です。公開 Beta は Required CI 完了後、`main` 上の版タグによって `.github/workflows/android-release.yml` から署名・SHA-256 検証を行い、GitHub Pre-release として公開します。鍵の設定と手順は [COMMUNITY.md](COMMUNITY.md) を参照してください。

## コントリビューション

Issue と Pull Request を歓迎します。

- **バグ報告**：[GitHub Issues](https://github.com/wxyu71/Lumi_Books/issues) から報告してください。端末の機種、Android のバージョン、再現手順を添えてください。
- **機能の提案**：Issues で議論を始めてください。
- **コードの貢献**：
  1. リポジトリを Fork する
  2. 機能ブランチを作成する（`feature/xxx` または `fix/xxx`）
  3. 開発し、`./gradlew :app:compileDebugKotlin` が通ることを確認する
  4. `main` ブランチに PR を提出する
- **コミットの規則**：Conventional Commits に従ってください。`feat:`、`fix:`、`refactor:`、`docs:`、`chore:`、`perf:` を使います。
- **コードスタイル**：Kotlin の公式スタイルに従い、既存のパターンに合わせてください。

`devdocs/` の詳しいドキュメントは現在ほぼ中国語です。他の言語への翻訳も歓迎します。

## ドキュメント案内

- [COMMUNITY.md](COMMUNITY.md)：コミュニティ版の範囲と Beta 配布手順。
- [AGENTS.md](AGENTS.md)：Agent の一括コミットと安全な作業手順。
- [CI 設計](devdocs/community-ci-test-design.md)：品質チェックの詳細。
- [コミュニティ UI](devdocs/community-ui-ux-implementation.md) と [動作デザイン](devdocs/lumi-ui-motion-design-system.md)：現行 UI 指針。
- [アプリ内変更履歴](app/src/main/assets/changelog.md)：コミュニティ版の変更履歴。

その他の `devdocs/` の旧計画や handoff は歴史的参考資料です。

## ライセンス

Lumi のオリジナルコードは [Apache License 2.0](LICENSE) で公開されています。

サードパーティの依存関係および改変されたコードは、それぞれのライセンスに従います。

| コンポーネント | ライセンス | 備考 |
|------|--------|------|
| [Backdrop](https://github.com/Kyant0/AndroidLiquidGlass) | Apache 2.0 | Liquid Glass 効果。改変あり |
| [Haze](https://github.com/chrisbanes/haze) | Apache 2.0 | ぼかし効果 |
| [PDFBox Android](https://github.com/TomRoush/PdfBox-Android) | Apache 2.0 | PDF のテキスト抽出 |
| [CommonMark](https://github.com/commonmark/commonmark-java) | BSD 2-Clause | Markdown の解析 |
| [Jsoup](https://jsoup.org/) | MIT | HTML の解析 |
| [Coil](https://coil-kt.github.io/coil/) | Apache 2.0 | 画像の読み込み |
| [OkHttp](https://square.github.io/okhttp/) | Apache 2.0 | HTTP クライアント |

サードパーティの完全な表記は [NOTICE](NOTICE) を、ライセンス本文は [app/src/main/assets/licenses/](app/src/main/assets/licenses/) を参照してください。

© 2026 Huangder

## リンク

- コミュニティ GitHub：[wxyu71/Lumi_Books](https://github.com/wxyu71/Lumi_Books)
- 原作者サイト：[huangder.top](https://huangder.top)
- 上流プロジェクト：[huangder/Lumi_Books](https://github.com/huangder/Lumi_Books)
- 原作者メール：huangder0104@126.com
