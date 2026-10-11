# HexKeyNote

Android용 오프라인 암호화 노트 앱입니다. 현재 저장소에는 Kotlin, Jetpack Compose, Room 기반의 앱 골격이 준비되어 있으며 암호화와 노트 사용 흐름은 아직 구현되지 않았습니다.

## 기술 구성

- Kotlin 및 Jetpack Compose
- Room 로컬 저장소. 노트 본문은 암호화된 payload로 저장하도록 데이터 모델을 분리했습니다.
- 최소 지원 버전: Android 8.0 (API 26)
- Android 백업 비활성화
- MVP는 마스터 비밀번호 잠금과 수동 암호화 내보내기·가져오기를 목표로 합니다. 생체 인증과 다중 기기 동기화는 제외합니다.
- 비밀번호 재설정이나 복구 키는 제공하지 않습니다. 비밀번호를 잊으면 노트와 내보내기 파일을 복구할 수 없습니다.

## 빌드 및 테스트

Android SDK 36과 JDK 17 이상이 필요합니다. 프로젝트를 Android Studio에서 열어 Gradle 동기화를 마친 뒤 실행할 수 있습니다.

```bash
./gradlew assembleDebug
./gradlew testDebugUnitTest
./gradlew connectedDebugAndroidTest
```

암호화 알고리즘, 키 파생·보관 및 잠금 흐름은 [PLAN.md](PLAN.md)의 보안 설계를 확정한 다음 구현합니다.
