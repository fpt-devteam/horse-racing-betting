# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Horse Racing Betting Game is an Android application written in Java that allows users to place bets on horse races and watch animated race outcomes. The app uses a single-activity architecture with fragments and follows MVVM pattern with ViewModel + LiveData.

**Package:** `com.example.horse_racing_betting`
**Min SDK:** 26 (Android 8.0)
**Target SDK:** 36
**Language:** Java 11

## Build Commands

The project uses Gradle with Kotlin DSL for build configuration:

- **Build the app:** `./gradlew build`
- **Clean build:** `./gradlew clean build`
- **Build debug APK:** `./gradlew assembleDebug`
- **Build release APK:** `./gradlew assembleRelease`
- **Install debug on connected device:** `./gradlew installDebug`
- **Run tests:** `./gradlew test`
- **Run instrumented tests:** `./gradlew connectedAndroidTest`

## Architecture

### Core Components

1. **MainActivity** (`MainActivity.java`)
   - Single activity hosting all fragments
   - Manages `GameViewModel` instance (activity-scoped)
   - Controls `AudioManager` lifecycle (BGM/SFX)
   - Handles audio focus during app lifecycle (onResume/onPause/onDestroy)
   - Fragment navigation via `replaceFragment(Fragment)`

2. **GameViewModel** (`viewmodel/GameViewModel.java`)
   - Central state manager using AndroidViewModel + LiveData
   - Manages game state machine: `STATE_IDLE` → `STATE_COUNTDOWN` → `STATE_RUNNING` → `STATE_RESULT`
   - Handles betting logic, coin management, race simulation
   - Persists data via SharedPreferences: username, coins, firstRun flag
   - Enforces single bet per horse via `picked` map (horse number → boolean)
   - Race engine: simulates horse movement with random increments every 100ms until position >= 100

3. **Fragment Flow**
   - `StartFragment` → Initial screen with username setup, wallet display
   - `BetFragment` → Multi-bet interface with RecyclerView, validates total stake ≤ coins
   - `RaceFragment` → Race visualization using SeekBars as horse lanes with animated thumbs
   - `ResultFragment` → Dialog showing race results, winnings, net change
   - `HelpFragment` → Instructions dialog
   - `SettingsFragment` → Mute toggles for SFX/BGM

### Key Systems

**Audio System** (`audio/AudioManager.java`)
- Singleton managing all audio playback
- BGM: MediaPlayer with manual looping and error recovery
- SFX: SoundPool with preloaded samples
- Audio focus handling (GAIN, LOSS, TRANSIENT, DUCK) for Android system integration
- Race SFX: looping gallop sound + random vocal effects (whinny/neigh) every 2-6 seconds
- Mute states persist via SharedPreferences
- Start/stop methods: `startBgm()`, `pauseBgm()`, `playSfx(resId)`, `startRaceSfx()`, `stopRaceSfx()`

**Race Animation** (`fragment/RaceFragment.java`)
- Uses SeekBars (0-100 progress) to represent horse positions
- Custom animated thumbs via `FrameSequenceDrawable` cycling through horse sprite frames
- Per-horse animation: idle frame (tile00) when stopped, running frames (tile01-tile06) during race
- Frame folders: `black_horse`, `yellow_horse`, `brown_horse`, `white_horse`
- Animation starts on STATE_RUNNING, stops on STATE_RESULT or horse finish

**Betting System**
- Users can place multiple bets but only ONE bet per horse (enforced in GameViewModel:115-165)
- Bet validation: total stake must not exceed current coins
- Payout multipliers: 1st=2.0x, 2nd=1.3x, 3rd=1.1x, 4th=0x
- Coins deducted on race start, winnings added after race finishes
- Net change = totalWinnings - totalLosses

### Data Models

- **Horse** (`model/Horse.java`): number, position (0-100), isFinished, finishPosition
- **Bet** (`model/Bet.java`): horseNumber, amount
- **RaceResult** (`model/RaceResult.java`): finishOrder, totalWinnings, totalLosses, netChange, newBalance

### State Management

Game states flow in strict order:
1. **IDLE**: Menu/betting phase
2. **COUNTDOWN**: 3-2-1 countdown with beep SFX, BGM paused
3. **RUNNING**: Race in progress, gallop SFX active, horses animate
4. **RESULT**: Race finished, fanfare plays, ResultFragment dialog shown after 2s delay

State transitions trigger audio changes in MainActivity's observer (lines 28-40).

## Important Implementation Details

### Audio Behavior
- BGM pauses during COUNTDOWN and RUNNING states (MainActivity:28-40)
- BGM resumes only when not racing and ResultFragment dialog is dismissed (MainActivity:62-88)
- Race SFX (gallop loop + vocals) start on STATE_RUNNING, stop on state exit
- All audio resources in `res/raw/`: bgm.ogg, fanfare.ogg, horse_galloping.ogg, horse_neigh.ogg, horse_whinny.ogg, mouse_click.ogg, race_start_beeps.ogg

### Betting Constraints
- Cannot bet on same horse multiple times (see GameViewModel:127-165)
- `picked` map tracks which horses have bets, cleared on `returnToMainMenu()`
- Bet removal updates `picked` map to allow re-betting on that horse (GameViewModel:167-180)

### Race Simulation
- Horse movement: random float between 0.5 and 2.5 per tick (GameViewModel:246)
- Race ends when ≤1 horse remains unfinished (GameViewModel:266)
- Finish order determined by sorting horses by position descending (GameViewModel:283-291)

### Resource Management
- AudioManager must be released on Activity finish to avoid memory leaks (MainActivity:98-103)
- FrameSequenceDrawable animations should stop in Fragment's onDestroyView (RaceFragment:286-293)
- SharedPreferences keys: GamePrefs (username, coins, firstRun), AudioPrefs (mute_sfx, mute_bgm)

## Common Patterns

### Adding New Fragments
1. Create fragment class extending Fragment
2. Register navigation in parent fragment (call `((MainActivity) requireActivity()).replaceFragment(new YourFragment())`)
3. Access GameViewModel: `((MainActivity) requireActivity()).getGameViewModel()`
4. Access AudioManager: `((MainActivity) requireActivity()).getAudioManager()`

### Adding Sound Effects
1. Place .ogg file in `app/src/main/res/raw/`
2. Load in AudioManager's `preloadDefaultSfx()` method
3. Play via `audioManager.playSfx(R.raw.your_sound)`

### Modifying Payout Logic
Edit multipliers in GameViewModel (lines 36-39) and `getMultiplierForPosition()` method (lines 329-337).

## Testing

- Unit tests: `app/src/test/java/com/example/horse_racing_betting/`
- Instrumented tests: `app/src/androidTest/java/com/example/horse_racing_betting/`
- Test runner: AndroidJUnitRunner

## Dependencies

Key dependencies from `app/build.gradle.kts`:
- androidx.lifecycle:lifecycle-viewmodel:2.7.0
- androidx.lifecycle:lifecycle-livedata:2.7.0
- androidx.fragment:fragment:1.6.2
- androidx.recyclerview:recyclerview:1.3.2
- androidx.cardview:cardview:1.0.0
- androidx.media3:media3-common (for logging utilities)

## Git

Recent commits show active features:
- Multiple bet prevention per horse (8bece4d)
- Horse gallop animation with sprite frames (58f1f3a)
- Audio features: race start beeps, background music, end game sounds (9f4f261)