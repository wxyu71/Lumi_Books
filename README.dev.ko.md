<!-- Language: ko | Audience: dev -->

<div align="center">

<img src="docs/media/lumi2.webp" alt="Lumi" width="96">

# Lumi 개발자 안내

Android용 로컬 전자책 리더의 기술 문서입니다. 기여자와 2차 개발자를 대상으로 합니다.

[![Version](https://img.shields.io/badge/version-2.3.12--beta.1-coral)](https://github.com/wxyu71/Lumi_Books/releases)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue)](LICENSE)
[![Android](https://img.shields.io/badge/Android-8.0%2B-blue)](https://developer.android.com)

[中文](README.dev.md) · [English](README.dev.en.md) · [日本語](README.dev.ja.md) · **한국어**

[사용자 버전](README.ko.md) · **개발자 버전**

</div>

## 기술 스택

| 분류 | 기술 | 버전 |
|------|------|------|
| 언어 | Kotlin | 2.3.10 |
| 빌드 | Android Gradle Plugin | 8.13.2 |
| UI | Jetpack Compose(BOM) | 2026.06.01 |
| 의존성 주입 | Hilt | 2.58 |
| 데이터베이스 | Room | 2.8.4 |
| 환경설정 저장 | DataStore Preferences | 1.1.1 |
| 내비게이션 | Navigation Compose | 2.8.9 |
| 코루틴 | kotlinx-coroutines-android | 1.9.0 |
| 백그라운드 작업 | WorkManager | 2.10.0 |
| EPUB 출판사 레이아웃 렌더링 | AndroidX WebKit | 1.16.0 |
| HTML 파싱 | Jsoup | 1.22.2 |
| PDF 파싱 | pdfbox-android | 2.0.27.0 |
| Markdown | CommonMark | 0.24.0 |
| 네트워크 | OkHttp | 4.12.0 |
| 이미지 로딩 | Coil | 2.7.0 |
| 흐림 및 유리 효과 | Haze 1.1.1, Backdrop(`third_party/backdrop`) | — |
| SDK | minSdk 26, compileSdk 37, targetSdk 37 | — |
| JVM | Java 17 | — |

## 아키텍처

앱은 MVVM + Repository 아키텍처를 사용하며, 의존성 주입에는 Hilt를 씁니다.

```
ui/        Compose 화면과 ViewModel: home, reader, settings, statistics, bookshelf 등
domain/    도메인 모델과 리포지토리 인터페이스
data/      Room, DataStore, 리포지토리 구현, WebDAV / OPDS 등 원격 데이터 소스, 백업과 동기화
util/      파서(EPUB, PDF, TXT, Markdown, Word, Kindle), 조판과 캐시 유틸리티
tts/       시스템 음성과 외부 음성 인터페이스
widget/    홈 화면 위젯
edition/   공개 버전과 공식 버전 사이의 기능 인터페이스
```

- **화면**: `MainActivity`는 Navigation Compose(`ui/navigation/NavGraph.kt`)로 주요 화면을 관리합니다. 환영 화면, 전체 화면 듣기, 듣기 설정 등은 별도의 Activity입니다.
- **의존성 주입**: `EBookReaderApp`에는 `@HiltAndroidApp`이 붙어 있습니다. ViewModel은 `@HiltViewModel`을 쓰며, 모듈은 `di/`에 있습니다.
- **데이터베이스**: Room의 현재 버전은 28입니다. 모든 마이그레이션은 `di/AppModule.kt`에서 `addMigrations`로 등록합니다. 테이블 구조를 바꿀 때는 버전 번호와 마이그레이션을 함께 추가하세요. 파괴적 마이그레이션에 의존하지 마세요.
- **설정과 비밀 정보**: 사용자 설정은 DataStore에 저장합니다. WebDAV, 번역, 음성, MinerU Token 같은 자격 증명은 Android KeyStore로 암호화해 저장합니다.
- **리더**: EPUB 출판사 레이아웃은 WebView로 렌더링하며, 출판사의 CSS와 글꼴을 그대로 유지합니다. 리더 레이아웃은 Canvas + StaticLayout으로 렌더링하며, 글꼴과 조판을 통일합니다.

## 프로젝트 구조

```
android_books/
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/            공통 코드와 리소스
│       │   ├── java/com/huangder/lumibooks/
│       │   ├── assets/      내장 HTML, 글꼴, 사전, 업데이트 로그
│       │   └── res/         리소스(표시 언어 7종)
│       ├── oss/             공개 버전의 기능 구현
│       ├── premium/         공식 버전의 비공개 소스(Git 추적 제외, 별도 리포지토리, 아래 참고)
│       ├── test/            로컬 단위 테스트
│       ├── androidTest/     계측 테스트
│       └── debug/           디버그용 진입점
│
├── benchmark/               시작과 스크롤 성능 테스트
├── third_party/             저장소에서 관리하는 서드파티 모듈(Backdrop, 자막 뷰, Word 변환 등)
├── tools/                   EPUB와 리더 프로브, 테스트 스크립트, 보조 도구
├── devdocs/                 개발 문서
├── docs/                    프로젝트 사이트(GitHub Pages)
└── .github/                 Pages 배포 워크플로와 Issue 템플릿
```

## 빌드

### 준비 사항

- Android Studio(최신 안정 버전) 또는 명령줄
- JDK 21
- Android SDK: compileSdk 37과 해당 build-tools
- 명령줄로 빌드할 때는 `ANDROID_HOME`을 설정하세요. Windows에서는 `gradlew.bat`을 사용합니다.

### 공개 버전

이 저장소만 클론했다면 빌드 결과는 공개 버전입니다.

```bash
./gradlew :app:assembleDebug
# APK: app/build/oss/outputs/apk/debug/
```

자주 쓰는 확인 명령:

```bash
./gradlew :app:compileDebugKotlin                 # Kotlin 컴파일
./gradlew :app:testDebugUnitTest                  # 로컬 단위 테스트
./gradlew :app:compileDebugAndroidTestKotlin      # 계측 테스트 컴파일 확인
```

### 커뮤니티 빌드와 Beta 배포

이 포크는 공개 `app/src/oss/`만 컴파일하며 앱 ID는 `io.github.wxyu71.lumibooks`입니다. 원본의 비공개 Premium 저장소나 원본 서명 키를 사용하지 않습니다.

`./gradlew :app:assembleDebug`는 로컬 확인용입니다. 정식 Beta는 Required CI를 통과한 후 `main`에 포함된 버전 태그를 통해 `.github/workflows/android-release.yml`에서 서명과 SHA-256 검증을 수행하고 GitHub Pre-release로 공개합니다. 비밀 값과 절차는 [COMMUNITY.md](COMMUNITY.md)를 참고하세요.

## 기여

Issue와 Pull Request를 환영합니다.

- **버그 신고**: [GitHub Issues](https://github.com/wxyu71/Lumi_Books/issues)에 제출해 주세요. 기기 모델, Android 버전, 재현 절차를 함께 적어 주세요.
- **기능 제안**: Issues에서 논의를 시작해 주세요.
- **코드 기여**:
  1. 저장소를 Fork합니다.
  2. 기능 브랜치를 만듭니다(`feature/xxx` 또는 `fix/xxx`).
  3. 개발한 뒤 `./gradlew :app:compileDebugKotlin`이 통과하는지 확인합니다.
  4. `main` 브랜치로 PR을 제출합니다.
- **커밋 규칙**: Conventional Commits를 따릅니다. `feat:`, `fix:`, `refactor:`, `docs:`, `chore:`, `perf:`를 씁니다.
- **코드 스타일**: Kotlin 공식 스타일을 따르고, 기존 프로젝트의 패턴을 맞추세요.

`devdocs/`의 자세한 문서는 현재 대부분 중국어입니다. 다른 언어로의 번역도 환영합니다.

## 문서 안내

- [COMMUNITY.md](COMMUNITY.md): 커뮤니티 범위 및 Beta 릴리스 절차.
- [AGENTS.md](AGENTS.md): Agent 일괄 커밋 및 안전 수칙.
- [CI 설계](devdocs/community-ci-test-design.md): 품질 검증 계약.
- [커뮤니티 UI](devdocs/community-ui-ux-implementation.md), [모션 디자인](devdocs/lumi-ui-motion-design-system.md): 현행 UI 기준.
- [앱 변경 기록](app/src/main/assets/changelog.md): 커뮤니티 변경 사항.

기타 `devdocs/`의 과거 계획과 handoff는 역사적 참고 자료입니다.

## 라이선스

Lumi의 원본 코드는 [Apache License 2.0](LICENSE)으로 공개됩니다.

서드파티 의존성과 개작된 코드는 각자의 라이선스를 따릅니다.

| 구성 요소 | 라이선스 | 비고 |
|------|--------|------|
| [Backdrop](https://github.com/Kyant0/AndroidLiquidGlass) | Apache 2.0 | Liquid Glass 효과, 개작 포함 |
| [Haze](https://github.com/chrisbanes/haze) | Apache 2.0 | 흐림 효과 |
| [PDFBox Android](https://github.com/TomRoush/PdfBox-Android) | Apache 2.0 | PDF 텍스트 추출 |
| [CommonMark](https://github.com/commonmark/commonmark-java) | BSD 2-Clause | Markdown 파싱 |
| [Jsoup](https://jsoup.org/) | MIT | HTML 파싱 |
| [Coil](https://coil-kt.github.io/coil/) | Apache 2.0 | 이미지 로딩 |
| [OkHttp](https://square.github.io/okhttp/) | Apache 2.0 | HTTP 클라이언트 |

서드파티 전체 고지 내용은 [NOTICE](NOTICE)에서, 라이선스 원문은 [app/src/main/assets/licenses/](app/src/main/assets/licenses/)에서 확인할 수 있습니다.

© 2026 Huangder

## 링크

- 커뮤니티 GitHub: [wxyu71/Lumi_Books](https://github.com/wxyu71/Lumi_Books)
- 원본 개발자 사이트: [huangder.top](https://huangder.top)
- 상위 프로젝트: [huangder/Lumi_Books](https://github.com/huangder/Lumi_Books)
- 원본 개발자 이메일: huangder0104@126.com
