# Scripture-after-unlock app — explore & plan

*A FanStaF app. Working title: **Selah** (Hebrew liturgical "pause / reflect" — fits a gentle
few-second pause; alternates below). Standalone `com.fanstaf.selah`, a FanStaF sibling like
`woodshed`, NOT under the N5SLN ham brand. Kotlin / Jetpack Compose, offline, privacy-first.*

Prepared 2026-08-30. This is a proposal for a decision — **no implementation code yet.**

---

## 0. TL;DR / recommendation

**Build it — but treat execution quality on one Android mechanism as the whole ballgame.**

- The concept sits in a **genuinely empty niche**: a *gentle, non-blocking, few-seconds-then-gone*
  verse shown *after unlock*, married to *active recall / spaced repetition*. The flanks are
  crowded (dozens of lockscreen/wallpaper apps; a mature memorization tier led by a free
  open-source app), but nobody occupies the exact intersection. The memorization apps have a
  *reminder* problem (users forget to practice); the lockscreen apps have an *intrusiveness +
  passivity* problem. This concept is the bridge.
- **Technically feasible on Android 14/15/16** via exactly one design:
  **foreground service + runtime-registered `ACTION_USER_PRESENT` receiver + a direct
  `TYPE_APPLICATION_OVERLAY` window** that auto-dismisses. Everything else (manifest receiver,
  full-screen intent, AccessibilityService, launching an Activity without overlay permission) is
  either dead or a Play-rejection trap on modern Android.
- **The single hardest/riskiest thing is not the code — it's OEM background-kill** (Xiaomi/MIUI,
  Samsung "sleeping apps", Oppo/Vivo). That, plus Play review of `SYSTEM_ALERT_WINDOW` + a
  `specialUse` foreground service, is the real risk surface.
- **Licensing is solved:** bundle **BSB** (CC0, modern & readable), **WEB** (public domain), and
  **KJV** (US public domain) offline; let users type/paste their own text for ESV/NIV/etc. Never
  bundle copyrighted translations.
- **Do a throwaway hardware spike first** (§14) to answer the make-or-break question before any
  product investment. You already have the Pixel 9a and the deploy flow.

---

## 1. Concept assessment

### Is it worth building?
Yes, with eyes open. The core insight is sound and specific: **turn the ~100 daily phone unlocks
into distributed-practice reps.** Distributed practice + active recall are the two best-evidenced
techniques in memory science, and no Scripture app currently delivers them *ambiently* through the
unlock habit. Every existing memorization app requires the user to remember to open it — which is
exactly the retention failure this concept removes.

### What is genuinely differentiated
1. **Spaced repetition driven by unlock cadence.** Surface the verse *due* for review at unlock, do
   a 3-second recall or fill-in-blank, dismiss. This is the strongest, currently-unoccupied wedge.
2. **"Gentle, not a takeover" as explicit positioning.** Never replaces the lockscreen, never blocks
   access, always auto-dismisses. This directly counter-programs the #1 complaint about the closest
   competitor (BitBible: "takes up the whole screen," "overwhelming").
3. **Android-first.** Several of the more memorization-forward apps (FaithLock, Verses) are iOS-only.
4. **No subscription.** The niche shows clear subscription fatigue (FaithLock $24.99/yr, Glorify
   $59.99/yr draw "everything's behind premium" complaints). The most-loved apps are free/open-source
   or one-time.

### Closest existing apps (and how each misses)
| App | What it is | Misses on |
|---|---|---|
| **BitBible** | Verse + AI explanation "every time you open your phone," quizzes | It's a **lockscreen replacement** — full-screen, intrusive; the exact thing to avoid |
| **FaithLock** | Verse overlay when you open a *distracting app* + quizzes | **App-blocker** (friction/gating), iOS-only, $24.99/yr |
| **Scripture Typer / The Bible Memory App** | First-letter typing + SRS (category leader) | **No unlock trigger** — you must remember to open it |
| **RememberMe** | Free, open-source; blur/first-letter/typing/flashcards | No unlock trigger; it's the "best free" bar to clear on the memorization axis |
| **Fighter Verses** | Curated 5-yr plan, audio, $4.99 one-time | Fixed verse list; no unlock trigger |
| **VerseLock / VerseLocker** | Reveal/type/fill challenges (names are misleading — "lock" = lesson steps, **not** the phone lockscreen) | No unlock trigger |

**Verdict:** differentiated enough to justify building, *conditional on* (a) nailing non-intrusive
reliability on Android, and (b) avoiding subscription pricing. The moat is UX discipline + the
SRS-on-unlock mechanic, not the idea alone — BitBible could add a "lite mode" and FaithLock could
ship on Android, so ship with quality and a clear identity.

---

## 2. Android feasibility

### Can we reliably detect unlock?
Yes — but only one way on modern Android:
- **`ACTION_USER_PRESENT` is NOT on the implicit-broadcast exemption list.** For any app with
  `targetSdk >= 26`, a **manifest-declared** receiver for it will never be delivered. It must be
  **runtime-registered** via `Context.registerReceiver(...)`, which means the process must be alive.
- To stay alive when the app isn't open, you need a **foreground service** that registers the
  receiver at runtime. `ACTION_BOOT_COMPLETED` *is* exempt, so a boot receiver can restart the FGS
  after reboot.
- `ACTION_SCREEN_ON/OFF` fire on wake *before* unlock and don't indicate keyguard dismissal — use
  `USER_PRESENT`, optionally confirmed with `KeyguardManager.isKeyguardLocked()`.
- **Doze is not a problem for catching unlock** — unlocking means the user is present and the device
  has left idle; `USER_PRESENT` fires normally.

### Can we show the verse immediately after?
Yes, via a **direct overlay window** — and this specifically avoids the biggest trap:
- **Background Activity Launch (BAL) restriction (Android 10+):** you cannot start an Activity from a
  background receiver *unless* you hold `SYSTEM_ALERT_WINDOW` (which grants a BAL exemption).
- **Drawing an overlay is not an Activity start**, so it sidesteps BAL entirely. Post a
  `TYPE_APPLICATION_OVERLAY` view with `WindowManager`, remove it after N seconds. Auto-dismiss and
  "few seconds then gone" fall out for free.
- **Full-screen intent is dead for this use case:** since 2025-01-22, `USE_FULL_SCREEN_INTENT` is
  auto-granted only to calling/alarm apps; a devotional app doesn't qualify.
- **AccessibilityService: do not use it.** Google's policy requires the primary purpose to assist
  users with disabilities; detecting unlock for devotional content "would not qualify" → likely
  rejection. No upside over the overlay path anyway.

### Permissions required (see §8 for detail)
`SYSTEM_ALERT_WINDOW` (draw overlay), `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SPECIAL_USE`,
`RECEIVE_BOOT_COMPLETED`, `POST_NOTIFICATIONS` (for the FGS notification), and optionally
`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.

### What creates problems (versions / OEMs)
- **Stock Android 12–16:** fine with the design above.
- **OEMs are the real reliability killer** (not stock Android): Xiaomi/MIUI/HyperOS requires a manual
  "Autostart" toggle (resets after OTA); Samsung's "Sleeping apps" kills apps idle ~3 days;
  Oppo/Vivo/OnePlus/Huawei layer aggressive managers. A foreground service survives better than a bare
  receiver but is still killable. **Plan for a dontkillmyapp-style onboarding step; expect this to be
  the #1 support issue.**
- **Android 15 nuance:** starting a *foreground service from background* via the SAW exemption now
  needs a currently-visible overlay. This does **not** bite us — our FGS is already running before
  unlock; it only affects FGS-start, not drawing the overlay.

### Recommended approach (ranked)
Scores 1–5, 5 = best. All assume a runtime-registered `USER_PRESENT` receiver.

| # | Approach | Reliab. | UX | Battery | Perms | Play risk | Modern compat | Complexity |
|---|---|:--:|:--:|:--:|:--:|:--:|:--:|:--:|
| **A ✅** | **FGS (specialUse) + USER_PRESENT + draw overlay directly** | 4 | 5 | 3 | 3 | 3 | **5** | 3 |
| B | FGS + USER_PRESENT + launch translucent Activity (via SAW BAL exemption) | 3 | 4 | 3 | 3 | 3 | 4 | 4 |
| C | FGS + USER_PRESENT + post a notification | 5 | 2 | 3 | 4 | 4 | 5 | 5 |
| D | Full-screen-intent notification | 1 | 3 | 3 | 1 | 2 | 1 | 3 |
| E | AccessibilityService + overlay | 3 | 4 | 3 | 2 | **1** | 4 | 3 |

**Recommendation: A.** It's the only design that both catches unlock reliably *and* shows UI at that
instant on Android 14/15/16, with the fewest fragile dependencies (one special permission that
actually does the job). C (notification) is the safest for policy and the sensible **fallback/"lite"
mode** for users who won't grant overlay permission — but a notification isn't "shown immediately on
unlock," so it can't be the primary experience. B is the fallback if hosting Compose in a raw overlay
proves painful (see §12).

---

## 3. UX proposal

### The ideal unlock experience
1. User unlocks. Within ~100ms a **calm card fades in** over whatever's on screen (home screen or the
   app they opened) — not full-screen, not modal, doesn't steal focus or block touches outside it.
2. The card shows the verse per the active mode (read / recall / fill-in-blank).
3. A subtle **progress hairline** shows the auto-dismiss countdown (default ~6s).
4. The card **fades out on its own**, or the user taps anywhere off it / swipes it / taps a small ✕ to
   dismiss instantly. **Access to the phone is never blocked** — tapping outside the card just uses the
   phone and dismisses the card.
5. Optional one-tap grade in recall/fill modes ("Got it" / "Show me") feeds the SRS.

### Presentation style (recommended default)
- **Card-style overlay**, centered or upper-third, ~88% width, generous padding, large rounded corners,
  soft shadow, translucent scrim *only behind the card* (no full-screen dim — that reads as a takeover).
- **Typography-first, image-free.** Reference in a smaller uppercase label; verse in a large,
  highly-readable serif; translation code muted. No decoration competing with the words.
- **Motion:** 150–200ms fade + slight scale-up in; fade + drift-up out. Nothing bouncy.
- **Theme:** follows system light/dark; honor large-text / font-scale for accessibility; TalkBack reads
  "reference, then verse." Respect the user's font-size setting on top of system scale.

### The three modes (see §4 for MVP split)
- **Read** — reference + full verse. The baseline "gentle reminder."
- **Recall** — reference + "Can you say it?" → auto-reveal after a beat (or tap "Reveal"). Adds active
  recall with near-zero friction.
- **Fill-in-blank** — verse with some words blanked (difficulty scales with mastery); tap to reveal.

### Sensible defaults
- Mode: **Recall** (the differentiator; still effortless).
- Duration: **6 seconds** (research shows a few seconds is enough for micro-exposure; user-configurable
  3–10s).
- Frequency: **once every 30 minutes**, not every unlock. Showing 80–100×/day is the fastest route to
  uninstall. "Every unlock" is available but opt-in with a warning.
- Selection: **spaced-repetition from the active collection** (PRO) / **sequential rotation** (free).
- Dismissible: **always yes.** "Non-dismissible" is intentionally not offered — it contradicts the
  entire philosophy and raises Play risk.

### Edge cases
- **Already in an app / on a call / alarm ringing / incoming notification:** the overlay is
  non-focusable and doesn't intercept input, but to be safe, **suppress the overlay when a phone call
  or alarm is active** (check `AudioManager`/`TelephonyManager` state) and when the keyguard is still
  secured. Never overlay the dialer or an alarm.
- **Landscape / foldable / split-screen:** card uses `max-width`, stays centered, scrolls internally if
  a long verse + large font exceeds height.
- **Portrait of restraint:** the app should feel like it *could* be turned off and you'd barely notice
  the mechanism — only the words.

---

## 4. MVP feature list (must-have vs nice-to-have)

### MVP — must have
- Foreground-service + `USER_PRESENT` + overlay pipeline (Approach A), with boot-restart.
- Overlay card: **Read** and **Recall** modes.
- **Bundled offline translations: BSB, WEB, KJV.** Browse/search to pick verses.
- **User-entered verses** (type/paste text + reference + translation label) — covers ESV/NIV etc.
- One "I'm memorizing these" collection; add/remove/reorder verses.
- Selection: **single current verse**, **sequential rotation**, **random**.
- Settings: enable/disable, duration slider, mode, selection strategy, **frequency limit**
  (every unlock / min-interval minutes), active collection, theme, font size.
- First-run onboarding: explain the concept, request overlay permission with rationale, guide OEM
  autostart/battery settings.
- **Notification fallback mode** for users who decline overlay permission.
- Fully offline, no account, no network.

### MVP — explicitly NOT in v1
Fill-in-blank, spaced repetition, mastery tracking, multiple collections, stats/streaks, widgets,
import/export, audio, typing practice, cloud sync. (All in the roadmap.)

### Nice-to-have if cheap (candidate to pull into MVP)
- **Fill-in-blank mode** — moderate effort, high differentiation. Strong candidate for MVP if the spike
  goes smoothly.
- Basic exposure counter per verse (data groundwork for SRS even if the algorithm ships later).

---

## 5. Future roadmap

### Version 1.x (the memorization engine — the actual moat)
- **Fill-in-blank + first-letter hints** (if not in MVP).
- **Lightweight spaced repetition** (Leitner boxes; see §6) driving verse selection on unlock.
- **Mastery tracking**: exposures, successful recalls, last-recalled, confidence, mastery level,
  next-review.
- **Multiple collections / tags / favorites.**
- **Stats** (quiet, non-gamified): verses learned, review calendar. **Optional** streaks.
- **Import/export** collections (JSON/CSV) — privacy-friendly backup without an account.
- **Predefined starter collections** (e.g. a "Topical" or "Foundations" pack of PD-translation verses).

### Version 2+
- Home-screen / lockscreen **widget** (passive display for people who won't grant overlay perms).
- **Typing-to-memorize** and **speech/recitation** practice (on-device speech; no cloud).
- **Audio pronunciation** (TTS; bundled audio has licensing cost — prefer TTS).
- **Wear OS** glance.
- Optional **cloud backup/sync** — only if users demand it; keep it opt-in and privacy-preserving.
- Runtime **ESV API / API.Bible** integration for copyrighted translations (online, quota-bound).

### Do NOT build unless evidence demands it
- Heavy gamification (points/leaderboards/badges) — likely *distracts* from the spiritual purpose;
  the audience skews toward calm, not competition. Streaks are the maximum acceptable game mechanic,
  and even those stay optional.
- A full in-app Bible reader — that's YouVersion's game; scope creep away from the wedge.
- Social/sharing feeds, AI verse explanations (BitBible's territory), account systems.
- Subscriptions.

---

## 6. Data model

**Persist locally in Room** (proven in `groundplane/logkit`); **settings in DataStore**, not Room.

```
Verse
  id: Long (PK)
  reference: String        // "Philippians 4:13"
  text: String
  translationCode: String  // "BSB" | "WEB" | "KJV" | "USER"
  source: enum { BUNDLED, USER }
  createdAt: Instant

Collection
  id: Long (PK)
  name: String
  orderIndex: Int

VerseInCollection            // many-to-many (a verse can live in several collections)
  collectionId, verseId, orderIndex   // composite PK

MemoryState                  // one per verse; the SRS/mastery record (v1.x)
  verseId: Long (PK/FK)
  exposures: Int             // times shown (any mode)
  successfulRecalls: Int
  lastExposedAt: Instant?
  lastRecalledAt: Instant?
  box: Int                   // Leitner box 0..5
  confidence: Float          // derived 0..1 for display
  nextReviewAt: Instant?

// Optional, v1.x for stats:
ExposureEvent
  id, verseId, timestamp, mode, outcome { REVEALED, GOT_IT, MISSED, DISMISSED }
```

**Relationships:** `Verse` *n↔n* `Collection` via `VerseInCollection`; `MemoryState` 1↔1 `Verse`;
`ExposureEvent` n↔1 `Verse`.

**Bundled Bible corpus:** ship as a **prebuilt read-only SQLite DB** (Room `createFromAsset`) or an
indexed asset, separate from the user's mutable log DB — the corpus is ~31k verses × 3 translations,
so keep it read-only and queryable rather than loading into memory. `Verse` rows the *user* saves are
copies/references into their collections, not the whole corpus.

**Spaced repetition (recommended: Leitner, not full SM-2).** Unlock cadence is ~100/day, so intervals
matter more than per-item ease. Boxes 0–5 with intervals ~ {show often, 1d, 3d, 7d, 16d, 35d}. On a
graded recall: correct → promote a box, wrong → back to box 0. At unlock, pick the most-overdue verse
in the active collection (with a min-gap so the same verse doesn't repeat back-to-back). Pure "read"
exposures increment `exposures`/`lastExposedAt` but don't move the box. Simple, explainable, effective.

---

## 7. Architecture

**Stack:** Kotlin, Jetpack Compose, single-activity for the in-app UI, Room + DataStore, Hilt (or
manual DI — the graph is small), Coroutines/Flow. Reuse the **`groundplane:uikit`** theme/components
(`AppTheme`, buttons, sliders, dialogs) and the **`logkit`** Room patterns — low-cost lift even though
this isn't a ham app.

**Major components**
- **`BootReceiver`** (manifest, `BOOT_COMPLETED` — exempt) → starts the service.
- **`VerseForegroundService`** (`specialUse`): the persistent process. In `onCreate` runtime-registers
  the `USER_PRESENT` receiver and shows the ongoing FGS notification. Holds a repository handle.
- **`UnlockReceiver`** (runtime-registered inside the service): on `USER_PRESENT` → hands off to the
  service coroutine scope.
- **`OverlayController`**: owns the `WindowManager` overlay (`TYPE_APPLICATION_OVERLAY`,
  non-focusable, `FLAG_NOT_TOUCH_MODAL`). Inflates a `ComposeView` with attached
  `ViewTree{Lifecycle,ViewModelStore,SavedStateRegistry}Owner` (the fiddly bit — see §12). Handles
  auto-dismiss timer and grade callbacks.
- **`VerseSelector`** (strategy): single / sequential / random / SRS-due. Applies the frequency gate.
- **`Repository`** over Room DAOs + the read-only corpus DB; exposes suspend queries.
- **In-app UI (single Activity + Compose Nav):** Verses (browse corpus, search, add user verse),
  Collection editor, Settings, Onboarding/permission flow, (later) Stats.
- **`SettingsStore`** (DataStore): all preferences + `currentVerseId` / `lastShownAt` / rotation cursor.

**Event flow (unlock → display → dismissal → stats)**
```
[boot] BootReceiver ─▶ start VerseForegroundService
                          │  onCreate: registerReceiver(USER_PRESENT), show FGS notification
[user unlocks phone] ─▶ USER_PRESENT ─▶ UnlockReceiver
    │
    ├─ frequency gate: now - lastShownAt >= minInterval?  (and not in call/alarm, not keyguarded)
    │        └─ no ─▶ drop
    ├─ VerseSelector.pick(activeCollection, strategy) ─▶ Verse (+ MemoryState)
    ├─ OverlayController.show(verse, mode, durationSec)
    │        └─ ComposeView overlay fades in; countdown hairline
    ├─ user taps off / swipes / ✕ / timer elapses ─▶ fade out, remove view
    └─ on dismiss/grade ─▶ Repository (bg): exposures++, lastExposedAt=now,
                            if graded: box±, nextReviewAt recompute; write lastShownAt
```

---

## 8. Permissions

| Permission | Why | Privacy / Play concern |
|---|---|---|
| `SYSTEM_ALERT_WINDOW` | Draw the verse overlay after unlock; also grants the BAL exemption | **High scrutiny** (overlays are a trojan/clickjacking vector). Declare a clear benign purpose; never overlay deceptively. User grants via Settings, not a runtime dialog. |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_SPECIAL_USE` | Keep the process alive to receive `USER_PRESENT` | **Play Console FGS declaration required** (description + demo video); `specialUse` draws manual review. The main paperwork cost. |
| `RECEIVE_BOOT_COMPLETED` | Restart the service after reboot (this broadcast *is* exempt) | Low concern; standard. |
| `POST_NOTIFICATIONS` | Show the mandatory ongoing FGS notification (Android 13+) | Low; runtime prompt. |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` (optional) | Improve survival against Doze/OEM kills | **Play-restricted** — needs justification; use sparingly and only via the user-initiated settings flow. Consider *guiding* users to battery settings instead of requesting it, to reduce review risk. |

No location, contacts, network, storage, or account permissions. **Offline, no analytics SDK, no
network** = a strong, honest privacy story worth stating prominently on the Play listing.

---

## 9. Battery / performance

- **Persistent FGS cost:** an always-on service + ongoing notification is the main battery/UX cost and
  the thing reviewers/users notice. Keep the service *idle* — it only wakes on the `USER_PRESENT`
  broadcast; no polling, no timers, no wakelocks between unlocks. Do DB work on a background dispatcher
  in short bursts. Realistically negligible battery beyond the "running" baseline.
- **Overlay is cheap:** inflate on demand, remove after N seconds; no continuous rendering.
- **Frequency gate doubles as a battery guard** — dropping most unlocks means most unlocks cost ~one
  cheap timestamp comparison.
- **Doze** doesn't affect us (unlock = device awake). The battery narrative to users: "one lightweight
  background service, no location, no network."

---

## 10. Google Play policy considerations

- **`SYSTEM_ALERT_WINDOW`:** allowed for legitimate overlays; expect review attention. Mitigate with a
  plain purpose description, no deceptive overlaying, and a non-blocking design.
- **FGS `specialUse`:** complete the Play Console declaration with a clear feature description + demo
  video; there's no better-fitting FGS type for "keep a receiver alive."
- **Full-screen intent:** not used (can't qualify) — avoids that whole restriction.
- **No AccessibilityService** — sidesteps the accessibility-misuse policy that would likely reject this
  use case.
- **Battery-optimization exemption:** if used, justify narrowly; prefer guiding users to settings.
- No policy outright bans a "verse after unlock" overlay. The aggregate scrutiny (SAW + specialUse FGS
  + optional battery exemption) is real but navigable with honest declarations. Position the listing
  explicitly as *not* a lockscreen replacement.

---

## 11. Bible translation / licensing

**Solved. Bundle only public-domain / CC0 text offline; never bundle copyrighted translations.**

| Translation | Bundle offline? | Basis / notes |
|---|---|---|
| **BSB** (Berean Standard) | ✅ **Yes — primary** | CC0 / public domain (2023). Modern, readable — the licensing-safe stand-in for ESV/NIV. |
| **WEB** (World English) | ✅ Yes | Public domain, modern-ish. |
| **KJV** | ✅ Yes (US) | US public domain. Only caveat: UK Crown copyright (Cambridge patent) — irrelevant for a US-published app; note for UK printing only. |
| **ASV / BBE / YLT / Darby** | ✅ Yes (optional) | Public domain; BBE's simple vocabulary is handy for memorization. |
| **ESV** | ❌ No bundle | Crossway API forbids storing >500 verses locally; quote-only. |
| **NIV** | ❌ No bundle | Biblica prohibits download/redistribution; hardest to license. |
| **NKJV / NLT / CSB / NASB / NET** | ❌ No bundle | ~500-verse quotation limits; full-text bundling needs licenses impractical for an indie. |

**Strategy:**
1. Ship **BSB + WEB + KJV** offline (covers modern-readable + traditional).
2. For any copyrighted translation, **user types/paste their own verse text** tagged with a translation
   label — shifts the copying to personal study and keeps the binary clean. Never pre-populate.
3. **No attribution legally required** for BSB/WEB/KJV(US)/ASV, but include a courtesy credits screen.
4. Skip ESV API / API.Bible for MVP (adds network dependency + quota mgmt); revisit in v2.

**Data sources (machine-readable, redistributable):** `BSB-publishing/bsb2usfm` (authoritative BSB,
USJ/JSON, CC0); `midvash/bible-data` (33 PD versions as **SQLite** — drops straight into Room);
`seven1m/open-bibles` (KJV/WEB/ASV/BBE, per-file license); eBible.org / worldenglish.bible (WEB).
Re-check ESV/Biblica permissions pages at build time before any future integration; the PD status of
BSB/WEB/KJV/ASV is permanent.

---

## 12. Technical risks

1. **OEM background-kill (highest).** Xiaomi/Samsung/Oppo/Vivo will kill the service; the receiver
   stops firing; the app silently "stops working." *Mitigation:* dontkillmyapp-style onboarding per
   OEM; FGS (survives better than a bare receiver); optional battery-exemption prompt; a widget/
   notification fallback so value survives even if the overlay path degrades. **This is the make-or-
   break field risk — not the code.**
2. **Hosting Compose in a raw overlay window.** `ComposeView` in a `WindowManager` overlay needs manual
   `ViewTree{Lifecycle,ViewModelStore,SavedStateRegistry}Owner` wiring — a known but fiddly pattern.
   *Mitigation:* prove it in the spike; fallback is a plain inflated View, or Approach B (translucent
   Activity via SAW BAL exemption) which gets Compose "for free" at the cost of heavier/Recents-visible
   UI.
3. **Play review of SAW + specialUse FGS.** Rejection or delay. *Mitigation:* honest declarations, demo
   video, non-blocking design, explicit "not a lockscreen" positioning.
4. **Overlay showing at the wrong moment** (over dialer, alarm, secure keyguard, another full-screen
   app). *Mitigation:* guard on call/alarm/keyguard state; non-focusable, non-touch-modal window.
5. **Annoyance → uninstall.** Over-showing kills retention. *Mitigation:* conservative default
   frequency (30 min), instant dismiss, calm design, easy global off.
6. **Manifest-receiver temptation.** A dev could "simplify" by declaring the receiver in the manifest —
   it will silently never fire on modern Android. *Mitigation:* documented; runtime-register only.

Nothing here makes the concept impossible. Risks 1–2 are the ones that decide feasibility, and both are
answerable in a small spike.

---

## 13. Development estimate (phased)

Rough solo effort; assumes reuse of `uikit`/`logkit` patterns.

- **Phase 0 — Spike (½–1 day).** The hardware proof (§14). *Hardest part lives here.*
- **Phase 1 — Overlay pipeline productionized (3–5 days).** FGS + boot restart + runtime receiver +
  `OverlayController` with ComposeView + auto-dismiss + frequency gate + call/alarm/keyguard guards +
  FGS notification. *Second-hardest part (Compose-in-overlay).*
- **Phase 2 — Content + persistence (3–4 days).** Bundled corpus SQLite (BSB/WEB/KJV), Room for
  user verses/collections, repository, browse/search, add-user-verse.
- **Phase 3 — Modes + settings (3–4 days).** Read + Recall overlay UI, all settings (duration, mode,
  frequency, selection, theme, font), onboarding + permission + OEM-guidance flow, notification
  fallback.
- **Phase 4 — Polish + Play prep (2–3 days).** Theming/accessibility/large-text, icon, listing copy,
  FGS + SAW declarations + demo video, privacy story.
- **v1.x later — memorization engine (5–8 days).** Fill-in-blank, Leitner SRS, `MemoryState`, mastery,
  multiple collections, stats, import/export.

**MVP ≈ 2–3 focused weeks** solo, gated on Phase 0 succeeding. **Technically hardest:** (1) OEM
survival, (2) Compose-in-overlay lifecycle wiring — everything else is routine.

---

## 14. Recommended first prototype (do this before anything else)

**The one question that decides the whole project:**
> *Can a modern Android app reliably show a Bible verse for a few seconds immediately after the user
> unlocks their phone, without becoming a lockscreen replacement?*

**Smallest spike to answer it (throwaway, no product code):**
- A single module: a `specialUse` foreground service that, in `onCreate`, runtime-registers an
  `ACTION_USER_PRESENT` receiver and shows an ongoing notification.
- On unlock: draw a `TYPE_APPLICATION_OVERLAY` card with **one hardcoded verse** (e.g. BSB
  Philippians 4:13), auto-remove after 6s, tap-anywhere-off to dismiss.
- A `BOOT_COMPLETED` receiver that restarts the service.
- A one-screen launcher that requests `SYSTEM_ALERT_WINDOW` and starts the service.

**Deploy to the Pixel 9a** (existing `adb install -r` flow) and verify, in order:
1. Overlay appears within ~1s of unlock, over the home screen **and** over an already-open app.
2. Phone remains fully usable — the card never blocks touches outside it.
3. It still fires **after the device has sat idle** for a while (Doze/app-standby).
4. It still fires **after a reboot** (boot restart works).
5. It does **not** appear over the dialer during a call / over an alarm.
6. Bonus: whether `ComposeView` renders cleanly in the overlay (validates Risk #2 now).

If 1–4 hold on the Pixel, the concept is real and worth the ~2–3 week MVP. If Compose-in-overlay
fights back (step 6), fall back to a plain View for the spike and decide Approach A-view vs B later.
The Pixel is stock Android — a later pass on a Xiaomi/Samsung device is what validates Risk #1 before
launch.

---

## Appendix — naming & branding

**Chosen direction (2026-08-30):**

> **Unlock the Word**
> *Scripture for the moments between. Selah.*

- **Name: Unlock the Word.** Double meaning does the work — *unlock your phone* / *unlock
  understanding of Scripture* — and it explains the app in three words.
  - *Caution 1 — the "unlock" ↔ lockscreen ambiguity.* The whole positioning is "NOT a lockscreen
    app," yet the name leans on "unlock" and sits adjacent to a crowded category (Bible Lock Screen,
    VerseLock, VerseLocker). Defuse it in the listing's first line: *"Not a lock screen — a gentle few
    seconds of Scripture **after** you unlock."*
  - *Caution 2 — collision risk.* "The Word" is heavily used in Christian app naming; clear Play +
    trademark before committing (see availability check results).
- **Selah in the tagline, not the name.** Opaque as a name (rewards only those who know the Psalms),
  perfect as a closing signature — and it reinforces the *pause* philosophy the name alone doesn't
  carry. Prefer a **period** before Selah ("…the moments between. Selah.") — reads as a beat; a dash
  makes Selah look like a byline. Em dash is the more-literary alternative.
- **Launcher label vs store title.** "Unlock the Word" (3 words) wraps under an icon. Consider a short
  launcher label ("Unlock" or "the Word") while the full Play title stays "Unlock the Word."

**Candidates considered:** *Unlock the Word* (chosen), *Selah* (→ tagline), *WordCue*. Earlier ideas:
*Hidden* (Ps 119:11), *Manna*, *Glimpse*, *Kept*.

All ship under the FanStaF publisher; package `com.fanstaf.<name>` (e.g. `com.fanstaf.unlocktheword`).

**Availability check (2026-08-30) — verdict: CROWDED-OR-CONFLICTED (moderate; not blocked, not clean).**
Preliminary scan, not legal advice.
- **Google Play:** no exact-title match for "Unlock the Word" / close variants — but the "the Word"
  Bible namespace and the verse-on-unlock *concept* are both crowded (BitBible, The Bible in You, etc.).
- **iOS App Store — direct string collision:** *"BibleKey – Unlock the Word"* (dev Babbi Brains Ltd.,
  an AI Bible app) already uses the exact phrase in its store title. Matters for future cross-platform.
- **USPTO:** no live/pending federal registration for the exact mark "UNLOCK THE WORD" surfaced, but a
  full TESS clearance could not be completed (tool 403'd) — run a proper Class 9 / Class 41 search at
  tmsearch.uspto.gov before relying on it.
- **Domain / common-law — the real friction:** **unlocktheword.com is TAKEN** by an *active,
  identically-named Bible ministry* (founder Sean Walsh; book + podcasts; reg. 2021) → genuine
  common-law confusion risk and bad SEO/brand overlap. **unlocktheword.app is AVAILABLE.**
- **Backups both weak:** *WordCue* — existing trademarked reading-assist product (ELR Software Pty Ltd,
  AU TM #874621, owns wordcue.com since 1999); **drop it.** *Selah* — too generic; ≥6 "Selah"
  Bible/devotional apps on Play, selah.app taken; usable only with a strong distinguishing suffix.

**Implication:** if kept, "Unlock the Word" needs a real USPTO clearance, the .app domain (not .com),
and hard differentiation from the ministry + BibleKey. Selah stays viable as the *tagline* signature
regardless (a tagline word isn't a brand collision). A more ownable coined name would sidestep all of
this — decision pending.
