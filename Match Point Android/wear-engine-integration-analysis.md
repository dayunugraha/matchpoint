# Wear Engine Integration Analysis — Match Point Android

Read-only analysis, no files changed. Note: this session already added Wear Engine
support earlier — this reports current state, not a greenfield project.

## 1–7. Build config

- applicationId: `com.matchpoint.app`
- namespace: `com.matchpoint.app`
- minSdk: 35
- targetSdk: 35
- compileSdk: 35
- Kotlin: 2.0.20 (`org.jetbrains.kotlin.android` / `plugin.compose`)
- AGP: `com.android.application` 8.6.0
- Gradle wrapper: 8.9
- KSP: 2.0.20-1.0.25

## 8. Architecture

Compose UI, manual service locator (`AppContainer`, no DI framework), single-`Activity`
nav via Navigation-Compose (`RootNavGraph`), Room persistence, `ViewModel` per screen.

## 9. Live match state

`app/src/main/java/com/matchpoint/app/ui/match/LiveMatchViewModel.kt` — `uiState` combines
repo flows, replays `TennisScoringEngine.state(...)` on every event.
`SessionRepository.recordPoint(matchId, side)` is the actual write path (persists a
`ScoreEvent`); phone remains sole source of truth, no shadow state anywhere.

## 10. POINT_A / POINT_B handling

Already wired, not raw/unhandled:

- `app/src/main/java/com/matchpoint/app/wear/RemoteCommand.kt` — 5-value enum (`POINT_A`,
  `POINT_B`, `PREVIOUS`, `NEXT`, `OK`), matches the watch project's vocabulary exactly.
- `app/src/main/java/com/matchpoint/app/wear/RemoteCommandHandler.kt` — dispatch object,
  exposes `onPointA`/`onPointB`/`onPrevious`/`onNext`/`onOk` callback slots plus
  `lastReceived: StateFlow`. No scoring logic inside it.
- `app/src/main/java/com/matchpoint/app/ui/match/LiveMatchScreen.kt` — `DisposableEffect`
  registers `onPointA = { viewModel.recordPoint(Side.A) }`, `onPointB = { viewModel.recordPoint(Side.B) }`,
  `onPrevious = { viewModel.undoLastPoint() }`, only while the screen is on screen (same
  lifecycle pattern as the existing `VolumeKeyBridge` for Bluetooth shutter remotes).
  `NEXT`/`OK` are received but unbound — no current UI action to map them to.

## 11. Wear Engine integration point

Already built at `app/src/main/java/com/matchpoint/app/wear/WearEngineManager.kt` — sole
class touching `com.huawei.wearengine.*` (AuthClient/DeviceClient/P2pClient), decodes
inbound `Message` into a `RemoteCommand`, then calls `RemoteCommandHandler.handle()`. Owned
by `AppContainer`, `connect()` called at app start. This is the correct seam:

```
watch → Wear Engine → WearEngineManager → RemoteCommandHandler → LiveMatchViewModel → repository/domain scoring
```

No second scoring engine exists or is planned.

## Huawei dependencies

Already present:

- `settings.gradle.kts`: Huawei Maven repo `https://developer.huawei.com/repo/`
- `app/build.gradle.kts`: `com.huawei.hms:wearengine:5.0.1.302`
- `AndroidManifest.xml`: `INTERNET` permission,
  `<queries><package android:name="com.huawei.health"/></queries>`, and a
  `com.huawei.hms.client.appid` meta-data (placeholder value, not yet filled).

## Gradle/Kotlin compatibility

None found. AGP 8.6.0 + Kotlin 2.0.20 + Gradle 8.9 + wearengine 5.0.1.302 built and
assembled clean (`./gradlew :app:assembleDebug` succeeded) earlier this session. Only open
item is non-code: real AppGallery Connect app ID + fingerprints, tracked in
`Match Point Watch/README.md`.
