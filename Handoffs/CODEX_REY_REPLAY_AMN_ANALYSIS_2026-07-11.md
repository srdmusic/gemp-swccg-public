# Codex Findings: Rey Replay, Failed Pulls, Force Push, AMN Hang

Date: 2026-07-11
Owner: Codex/Alfred
Requested by: K-2 via mailbox m00109
Scope: independent analysis and source verification only
Java edits: none
Pushes: none

## Source Separation

| Evidence source | Finding |
|---|---|
| `replays/asdf/nw44h6vxr10t0sb6.xml.gz` | Rey replay. Contains Rey/battle-damage incidents, but no `A Cunning Warrior`, no `Clash Of Sabers`, no `Any Methods Necessary` text hits. |
| `replays/asdf/vugpape5lw1bc7rq.xml.gz` | Endor Ops style replay. Contains `Any Methods Necessary` as a Used interrupt and `Force Push`, but not the Cunning/Clash incidents. |
| `replays/asdf/2jg1sj0l3qrlgy6a.xml.gz` | AMN starting-interrupt hang/cancel repro. Classic `109_7`, not virtual `206_13`. |
| `resources/evidence/game_log_latest.txt` (relocated 2026-07-13 from `src/.../ai/models/rando/game_log_latest.txt`, deleted at e5b393955; identical blob, line numbers unchanged) | Current text log with repeated failed pulls, Force Push exchanges, Rey battles, and no-retreat symptoms. |
| `logs/gemp-swccg.log` | Current reasoning log with V-tags, TOPN lines, and source-card choices. |

Do not treat every incident as coming from the same replay. The named replay IDs and current log are not identical evidence streams.

## Card Source Verification

| Card | Source | Verified behavior | AI implication |
|---|---|---|---|
| A Cunning Warrior | `src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/cards/set224/light/Card224_008.java:38` to `:81` | Deploys only if your Skywalker Epic Event is on table. Gives free battles where you have a Skywalker. Once per turn deploys Anakin's Lightsaber or a Cloud City corridor from Reserve. | Starting-effect scoring must not blindly prefer it over A Good Friend unless the Skywalker Epic Event path is active and the Reserve pull is real/useful. |
| Clash Of Sabers | `src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/cards/set5/light/Card5_038.java:40` to `:106` | The Reserve-search mode specifically takes `Uncontrollable Fury` into hand. Other modes prevent a character from moving/battling or cancel Presence Of The Force. | V177/V95/V192 should recognize the exact upload target. If no Uncontrollable Fury is in Reserve, the Reserve-search action is dead. |
| Force Push (V) | `src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/cards/set200/dark/Card200_120.java:43` to `:139` | Two modes: battle exclusion using a Dark Jedi and opponent character, or exchange two hand cards with one Force Pile card. | Battle-exclusion mode can be good. Exchange mode is utility/recycle and should not fire just because it exists. Current -500 is too soft. |
| Any Methods Necessary, classic | `src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/cards/set109/dark/Card109_007.java:37` to `:143` | Starting action takes exactly one prison and one bounty hunter, optionally a matching weapon and/or starship. Valid selection can stop at 2 cards. | The AI needs a deterministic combo selector: required pair first, optional matches only if legal and available, then submit. Do not wait for a perfect 4-card package. |
| Any Methods Necessary (V) | `src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/cards/set206/dark/Card206_013.java:39` to `:121` | Different card. Cancels weapon targeting or uploads/downloads Binders, Jet Pack, Mandalorian Armor, or a blaster rifle. | Do not source the AMN starting-interrupt hang from this virtual card. Wrong set, wrong behavior. |

## Incident Table

| Incident | Evidence | Responsible logic | Adjust-in-place proposal |
|---|---|---|---|
| A Cunning Warrior selected over A Good Friend | `logs/gemp-swccg.log:249` to `:280`: `A Cunning Warrior` scores 1530, `A Good Friend` scores 1030. `V126a` adds +500, `V80` adds +1000. | `CardSelectionEvaluator.java:8347` to `:8445`, plus reserve-pick mirror at `:8854` to `:8865`. | Keep V80, but split the V126a bonus so A Cunning Warrior does not get free-battle +500 unless the current objective/setup actually wants that over A Good Friend. Boundary: current gap is exactly +500. Remove/gate that +500 and A Good Friend wins 1030 vs 1030 by tie/context, or give A Good Friend its own analyzer-driven priority. |
| Clash Of Sabers can search Reserve for a missing target | Deck inventory shows `2x Clash Of Sabers`, one in Reserve, `logs/gemp-swccg.log:136`. Card source shows upload target is only Uncontrollable Fury. | V177/V95/V192 target extraction: `ActionTextEvaluator.java:300` to `:356`, `:4760` to `:4824`, `:5459` to `:5575`. | Add or repair target resolution for `CLASH_OF_SABERS__UPLOAD_UNCONTROLLABLE_FURY`. If Uncontrollable Fury is not in Reserve and no battle/cancel mode is currently useful, hard-block the Reserve-search branch. |
| Repeated failed pulls reveal/verify Reserve | `game_log_latest.txt:147` to `:149`, `:153` to `:155`, `:316` to `:318`, `:1533` to `:1539`, `:2387` to `:2400`, `:3440` to `:3451`. | V192 still rewards some pull actions before failure memory catches them. V177 only blocks when parsed targets are known dead. V22.6 failed-pull avoidance appears later in selection, not parent action choice. | Record source-action failed pulls after verify-without-card or no zone-change. Feed source+target failure memory into ActionTextEvaluator before V192 emits positives. Cool down exact source+target until Reserve changes. Do not add another broad penalty branch. |
| Force Push exchange fires in log despite a block existing | `game_log_latest.txt:91` to `:95`, `:249` to `:253`, `:1938` to `:1942`, `:2990` to `:2994`. Reasoning log shows V67u can block at `logs/gemp-swccg.log:19451` and `:19956`. | `ActionTextEvaluator.java:1980` to `:2003`: exchange mode is only -500. | Make Force Push exchange a structural veto unless the AI can name a wanted Force Pile card and the exchange improves hand quality enough to beat draw/pass. Preserve the battle-exclusion +80 path. |
| Evil Is Everywhere weapon pull beats actual deploys | `logs/gemp-swccg.log:19880` to `:20043`: V192 gives Evil Is Everywhere pull +750, V38.4 adds +80, TOPN picks pull at 900, selection then takes Dooku's Lightsaber. ObjectiveAnalyzer reports no objective at `:19965` to `:19966`. | `ActionTextEvaluator.java:5459` to `:5575` V192 single emit, `DeployEvaluator.java:933` to `:939` V38.4, `DeployEvaluator.java:1033` to `:1046` V40 hold-back gate, `DeployEvaluator.java:3865` to `:3877` weapon-pull ownership. | First fix objective identity, because V40 says `Not TDIGWATT` in a TDIGWATT context. Then gate V192 weapon tier against immediate holder/value: weapon pull should not beat deploying a real card unless the weapon has a valid immediate wielder and strategic need. |
| Initiating / accepting battles with no Reserve destiny | `game_log_latest.txt:1531` to `:1544` deploys into battle, then `:2550` to `:2568` cannot draw battle destiny and loses Vader/Aurra. Same pattern repeats at `:3602` to `:3612`. | `BattleEvaluator.java:61` to `:62`, `:636` to `:675`; `ForceActivationEvaluator.java:190` to `:214`. | V61 reserve guard exists but was too weak or bypassed by overpower logic/context. Initiating battle at Reserve 0 should be hard-blocked unless raw power margin survives expected opponent destiny and attrition. Also stop deploy/reinforce sequences that drain Reserve to 0 immediately before battle. |
| No-retreat / standing in overpowered sites | Rey replay/current log shows opponent stacks Upper Walkway and wins, `game_log_latest.txt:3220` to `:3256`, later final blowout `:3987` to `:4005`. Existing retreat logic exists but did not prevent the state. | `CardSelectionEvaluator.java:5983` to `:6008` V169 retreat mode, `:6179` to `:6188` retreat bonus, `:6351` to `:6415` V67au retreat-to-drain. | Retreat mode currently evaluates mover/destination, but the bot still leaves valuable cards where opponent can reinforce and battle. Expand danger forecast to opponent next deploy+battle at the site, not just current power. Use existing V169/V67au, do not create a parallel retreat system. |
| AMN starting-interrupt hang | `replays/asdf/2jg1sj0l3qrlgy6a.xml.gz` shows `109_7` starting interrupt revealed/played, then both players cancel; no follow-up AI choice is captured. Source selection allows valid 2, 3, or 4 card packages. | Classic source `Card109_007.java:67` to `:143`; likely generic card-combination selection path in Rando. | Add a classic AMN selector keyed by source/action text: choose one prison plus one bounty hunter first, then optional matching weapon/starship only if legal candidates exist. Submit valid 2-card minimum rather than trying to maximize optionals and hanging. |

## Priority For K-2

| Priority | Fix | Why |
|---|---|---|
| P0 | AMN classic starting selector | Hang/cancel blocks game start. Must use `109_7`, not `206_13`. |
| P0 | Failed-pull memory at parent action level | Repeated verifies waste turns and leak Reserve contents. |
| P1 | Force Push exchange structural veto | Current -500 is not robust under additive scoring. |
| P1 | V61/V176 battle-reserve hardening | The bot battled at Reserve 0 and lost key cards. Existing guard did not dominate. |
| P1 | Objective identity for TDIGWATT | V40 says `Not TDIGWATT` while the game context is TDIGWATT, causing wrong deployment posture. |
| P2 | A Cunning Warrior vs A Good Friend boundary | Current +500 tie-break selects Cunning by generic text, not analyzer-owned plan. |
| P2 | Retreat forecast expansion | Existing retreat code is present but under-forecasts next-turn opponent reinforcement. |

## Notes To Avoid Wasting More Time

| Note | Detail |
|---|---|
| Replay format | The `.xml.gz` files are zlib streams, not gzip. `gzip -dc` fails. Use Python `zlib.decompress`. |
| Text/log mismatch | The current `game_log_latest.txt` has Cunning/Clash/Force Push/Rey evidence. The named long replays do not contain all of those text hits. |
| No implementation here | Per m00109, Codex did not edit Java. This handoff is source verification and fix direction for K-2. |
| Build status | No compile run, because no Java or resource behavior was changed by this handoff. |
