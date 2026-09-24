# Match Point — Android

Kotlin + Jetpack Compose + Room port of the iOS Match Point app (casual tennis
"mabar" meetup tracker). Domain rules (scoring, matchmaking, session
lifecycle) are ported 1:1 from the iOS app — see
`app/src/main/java/com/matchpoint/app/domain/` for the pure, DB-free logic
and `app/src/main/java/com/matchpoint/app/repository/SessionRepository.kt`
for the single mutator layer.

## Why Android

The original motivation for this port: Bluetooth "camera shutter" remotes
can drive live scoring by triggering the hardware volume buttons, which
Android lets any app intercept directly (`Activity.onKeyDown` for
`KEYCODE_VOLUME_UP`/`DOWN`) — no HID-keyboard-specific hardware required.
See `ui/match/VolumeKeyBridge.kt` and `MainActivity.onKeyDown`: while the
live scoring screen is open, Volume Up scores side A and Volume Down scores
side B; any other screen gets normal volume behavior.

## Opening the project

1. Open this folder in Android Studio (Koala or newer). It will generate
   the Gradle wrapper automatically on first sync — the wrapper jar isn't
   checked in.
2. Sync Gradle, then Run on a device/emulator (minSdk 35 — pinned to a
   Galaxy A54 on Android 15; lower it if you ever target an older device).
   Targets JVM 21
   (`compileOptions`/`kotlinOptions` in `app/build.gradle.kts`) — make sure
   Android Studio's Gradle JDK is set to 21 (Settings → Build, Execution,
   Deployment → Build Tools → Gradle) if sync complains about the JDK.
3. No launcher icon is bundled yet — Android Studio will use a default one;
   add a real one via **Image Asset Studio** whenever you like.

## Structure

```
domain/      pure Kotlin — enums, models, TennisScoringEngine, MatchGenerator
data/        Room entities, DAOs, AppDatabase, type converters
repository/  SessionRepository — the only place that mutates persisted state
ui/          Compose screens + ViewModels (sessions list, session detail, live match)
```

## Known simplifications / things to revisit

- **`SessionPlayer.points` scoring rule** wasn't fully specified in the
  original architecture doc (only that wins/losses/points/games are folded
  in on match completion). This port currently awards **1 point per win,
  0 per loss** — adjust `SessionRepository.adjustStats` if the iOS app
  actually uses a different scoring formula (e.g. points per game won).
- Session badge image, theme palettes (Roland Garros/Wimbledon), and other
  UI-only polish from the iOS app were intentionally left out — current UI
  is a functional placeholder using a generic "clay court" Material 3 theme.
- Editing an already-completed match's score, and manual after-the-fact
  match entry, are implemented in the repository (`updateCompletedMatch`,
  `recordCompletedMatch`) but have no UI screen yet.
- No unit tests yet for `TennisScoringEngine` / `MatchGenerator`, even
  though they're pure and straightforward to test in isolation.
