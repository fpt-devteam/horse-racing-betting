# Horse Racing Betting Game — Android (Java)

## Tech Baseline
- **Language:** Java 8+
- **Min SDK:** 24 (Android 7.0)
- **UI:** Single `MainActivity` + Fragments (`StartFragment`, `BetFragment`, `RaceFragment`, `ResultFragment`, `HelpFragment`)
- **State:** `ViewModel` + `LiveData` (Java), simple finite state in `GameViewModel`
- **Storage:** `SharedPreferences` (username, coins, settings), optional `Room` (bet history)
- **Graphics:** `SurfaceView` (or `Canvas`) *or* simple `ObjectAnimator` on `ImageView`s (pick one in task)
- **Audio:** `SoundPool` (SFX), `MediaPlayer` (BGM)
- **Assets:** `res/drawable` (icons, track), `res/raw` (audio), `res/anim` (button/race start)

---

## EPIC 0 — Project Setup (Android)
- [ ] Create Android project (`Empty Views Activity`) — **package:** `com.yourco.horserace`
- [ ] Add modules/packages: `ui/`, `audio/`, `engine/`, `model/`, `data/`, `util/`
- [ ] Dependencies: AppCompat, Fragment, Lifecycle ViewModel/LiveData, RecyclerView, ConstraintLayout
- [ ] App theme & colors, fonts; enable view binding
- [ ] Navigation: single `MainActivity` hosting fragments (manual fragment transactions)
- [ ] App icons (adaptive), splash screen

**Accept:** App builds/launches, empty `StartFragment` shown.

---

## EPIC 1 — Initialize Game & Player
- [ ] `GamePrefs` (SharedPreferences) with: `username`, `coins`, `mute`, `firstRun`
- [ ] `UsernameDialogFragment` on first run → save to prefs
- [ ] Init wallet = **100 coins** if `firstRun = true`
- [ ] “Restart Game” in overflow menu → confirm dialog → reset coins to 100, keep username
- [ ] Wallet display component (`WalletView`) observing `GameViewModel.coins`

**Accept:** Username persists; coins persist across app restarts; restart resets to 100.

---

## EPIC 2 — Horse Racing Core (Engine + UI)
- [ ] **Choose animation path:**
  - A) **Simple**: 4 `ImageView` horses move along a straight lane using `ObjectAnimator` + lap loop
  - B) **Gamey**: `RaceSurfaceView` renders track & horses with a timed game loop
  - *(Pick A for v1 simplicity)*
- [ ] `RaceConfig`: laps (1–3), horses list (ids, names, icon res)
- [ ] `RaceEngine` (in `engine/`): random speed ticks + mild variance per horse; lap counting; finish order callback
- [ ] Countdown (3–2–1) overlay animation; disable inputs during race
- [ ] `RaceFragment` UI: track bg, start line, finish line, horses at start
- [ ] Emit placements (1st–4th) to `GameViewModel`

**Accept:** Start → countdown → horses run → placements produced reliably.

---

## EPIC 3 — Betting & Payouts
- [ ] `Bet` model: `{horseId, amount, laps}`; `BetAdapter` (RecyclerView) to list multi-bets
- [ ] `BetFragment` UI: amount (EditText + +/-), horse (Spinner), laps (Spinner), “Add Bet” → list; “Remove” per row
- [ ] Validation: total stake ≤ wallet; positive int; clamp to wallet
- [ ] Lock bets on race start; deduct staked amount temporarily
- [ ] `Payouts`: table (v1) — 1st: ×2.0, 2nd: ×1.3, 3rd: ×1.1, 4th: 0
- [ ] Resolve after race: compute per-bet result, update wallet, add to optional `Room` history

**Accept:** Multi-bet flows; wallet updates with correct delta; errors shown inline.

---

## EPIC 4 — Audio, Results & Help
- [ ] Add assets: BGM loop (`res/raw/bgm.mp3`), SFX (`click`, `beep`, `whinny`, `fanfare`)
- [ ] `AudioManager` (singleton): preload SoundPool, MediaPlayer (bgm); mute toggle persists
- [ ] Hook sounds: click, countdown beeps, start, finish fanfare
- [ ] `ResultFragment` (DialogFragment): podium icons, placements, win/loss summary, net ± coins, “Race Again”
- [ ] `HelpFragment` (DialogFragment): how to play, payout table, controls with small illustrations
- [ ] Icon swap: place 2–3 horse skin sets under `drawable` and map by config

**Accept:** Sounds at right times, mute works & persists; results/guide dialogs look clean.

---

## UI Structure (Fragments)
- **StartFragment**: Username/wallet summary, “Bet Now”, “Help”, “Settings (Mute/Restart)”
- **BetFragment**: Bet form + list, “Start Race”
- **RaceFragment**: Track + horses + countdown
- **ResultFragment** (dialog): placements + payout summary
- **HelpFragment** (dialog): instructions & payout table

---

## Concrete Technical Tasks
- [ ] `model/` → `Horse`, `Bet`, `Placement`, `RaceResult`
- [ ] `data/GamePrefs.java` (SP wrapper): `getCoins()`, `setCoins()`, `getUsername()`, `setMute()`, etc.
- [ ] `engine/RaceEngine.java`: `start(laps, horses)`, `tick(frameMs)`, `isFinished()`, `getPlacements()`
- [ ] `engine/Randomizer.java`: jitter helpers (seed optional)
- [ ] `logic/Payouts.java`: `resolve(List<Bet>, List<Placement>) → PayoutSummary`
- [ ] `audio/AudioManager.java`: `init(Context)`, `playSfx(id)`, `playBgm()`, `setMuted(boolean)`
- [ ] `ui/common/CountdownView.java`: draws 3-2-1 with fade/scale anim
- [ ] `ui/bet/BetAdapter.java`: RecyclerView, remove callback
- [ ] `ui/result/ResultDialogFragment.java`: shows podium & deltas
- [ ] `GameViewModel.java`: `state (IDLE/COUNTDOWN/RUNNING/RESULT)`, `coins`, `bets`, `placements`, `events`
- [ ] `util/Format.java`: coin formatter, input filters (integer, min/max)
- [ ] **Animation path A**: `ObjectAnimator` per horse along X (or Path) + lap loops; finish detection via distance
  - or **Path B**: `RaceSurfaceView` with `Handler` loop @ 60fps

---

## Acceptance / QA Checklist
- [ ] First launch asks username; shows 100 coins
- [ ] Add/remove multiple bets; cannot over-stake
- [ ] Start → countdown blocks inputs; race produces deterministic ranking
- [ ] Payouts per table; wallet updates; negative/positive deltas correct
- [ ] Mute toggle persists across restarts; SFX/BGM mapped correctly
- [ ] Restart resets coins to 100; username unchanged
- [ ] Rotation (optional) doesn’t crash; minSdk devices run smoothly

---

## Notes on Implementation Choices
- **Keep it simple for v1:** use **ObjectAnimator** on a straight lane; add curve/loop later with `Path` or `SurfaceView`.
- **Storage:** `SharedPreferences` is enough for username/coins; add `Room` only if you want bet history.
- **Java ViewModel:** Use `androidx.lifecycle:lifecycle-viewmodel` (Java interop ok).
