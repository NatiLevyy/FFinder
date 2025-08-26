# Claude.md - Android Project Rules (FFinder)
Platform: Android (Kotlin, Compose)
Architecture: MVVM + Hilt
Min SDK: 24
Target SDK: 34

## Preflight
./gradlew clean assembleDebug
./gradlew :app:testDebugUnitTest

## Always
- Use Gradle Wrapper
- Keep secrets out of VCS
- Tests and lint before merge
- Minimal, topic-focused diffs