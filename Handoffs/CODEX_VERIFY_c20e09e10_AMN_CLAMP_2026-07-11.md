# Codex Verification: c20e09e10 AMN Clamp

Date: 2026-07-11
Verifier: Codex/Alfred, plus work-verifier subagent
Commit: `c20e09e10a06790e6d97350e40d9f439a7593e81`
Scope: K-2 mailbox m00111 verification

## Verdict

| Item | Result | Evidence |
|---|---|---|
| Commit compile | PASS | In-container `mvn -q -pl gemp-swccg-server -am compile`: `MVN_EXIT=0`, `0` Maven `[ERROR]` lines. Detached verifier compile also `MVN_EXIT=0`. |
| Rando/chosenone clamp parity | PASS | `DecisionSafety` clamp chunks are source-identical, 5146 chars locally and 5078 chars in detached verifier slice. |
| Rando/chosenone V22.7 parity | PASS | `CardSelectionEvaluator` V22.7 routing chunks are source-identical. |
| Compiled class markers | PASS | Both `DecisionSafety.class` files contain `SAFETY CLAMP` count 1. Both `CardSelectionEvaluator.class` files contain `V22.7` count 5, `into hand` count 4, `prison` count 1. |
| Type-by-API grep | PASS | No forbidden generic `getTitle().contains(...)` matches in edited files. |
| Already-valid answer path | PASS | `DecisionSafety.java:224` to `:237` only returns a fixed response when `keptList` differs from the original token list; otherwise returns the original response unchanged. |
| AMSD routing | PASS static | `simultaneously deploy aboard` branch still precedes the V22.7 catch-all at `CardSelectionEvaluator.java:412` to `:414`; the V22.7 exclusion only blocks texts containing `into hand` or `prison`. |
| Runtime log proof of new clamp | NOT YET | Current `logs/gemp-swccg.log` has no `SAFETY CLAMP` hit after the commit. Needs a new AMN game/replay to prove branch firing. |

## Replay And Log Checks

| Source | Check | Result |
|---|---|---|
| `replays/asdf/2jg1sj0l3qrlgy6a.xml.gz` | Classic AMN hang replay | Confirms `109_7` revealed and played as Starting Interrupt, then both players canceled. No post-play selection decision is recorded in the replay. |
| `logs/gemp-swccg.log:16497` to `:16511` | Pre-fix bad selection class | Shows a `temp33` selection from an ARBITRARY_CARDS decision with only one selectable card and terrible score. This matches the class of failure the clamp defends against. |
| `logs/gemp-swccg.log` | Post-fix clamp firing | No `SAFETY CLAMP` entry found yet. Compile/source/class proof only, not runtime proof. |
| `resources/evidence/game_log_latest.txt` (relocated 2026-07-13; was `src/.../ai/models/rando/game_log_latest.txt`, deleted at e5b393955) | AMSD text sanity | Existing AMSD messages are `matching Star Destroyer` and `deploy both simultaneously`, not `into hand` or `prison`, so the V22.7 exclusion should not block them. |

## Source Refs

| Topic | File |
|---|---|
| Clamp implementation | `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/DecisionSafety.java:176` to `:237`; chosenone mirror same region |
| V22.7 routing fix | `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/evaluators/CardSelectionEvaluator.java:415` to `:427`; chosenone mirror same region |
| Classic AMN source | `src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/cards/set109/dark/Card109_007.java:67` to `:143` |
| AMN valid minimum | `Card109_007.java:109` to `:133`, valid at 2 cards, optional matching weapon/starship at 3 or 4 |
| AMN virtual contrast | `src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/cards/set206/dark/Card206_013.java:39` to `:121`, different card and not the hang source |

## Warning

| Warning | Detail |
|---|---|
| Main worktree is dirty | There is an unrelated post-commit uncommitted edit in `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/evaluators/CardSelectionEvaluator.java`. |
| Rando-only drift | That dirty edit is Rando-only and not mirrored to chosenone in the main worktree. If K-2 intends to ship it, mirror it before commit. |
| Current dirty compile | Current main worktree also compiled in-container with `MVN_EXIT=0`, but it is not pure `c20e09e10`. |

## Bottom Line

`c20e09e10` passes commit-level verification with one runtime caveat: no new post-fix AMN replay/log has shown `SAFETY CLAMP` firing yet. To finish behavior proof, run a fresh AMN setup game and grep `logs/gemp-swccg.log` for `SAFETY CLAMP`, then confirm the replay advances beyond AMN prison/bounty-hunter selection.
