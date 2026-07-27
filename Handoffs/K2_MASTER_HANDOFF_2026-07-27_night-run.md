# K-2 Master Handoff — 2026-07-27 night run (session "Objective flip condition audit")

Written by the outgoing K-2 lead at Steve's request. You are the fresh K-2 lead session.
Read this, the memory index (auto-loads), and the rando-objective-behavior-audit skill.
This file supersedes K2_MASTER_ONBOARDING_2026-07-27.md's queue state; that file still owns
identity, comms machinery, and the older laws. Codex returns 2026-08-01; you are the ONE
writer until then. Mailbox history through m01469.

## 1. HARD STANDING ORDERS (memory-backed, non-negotiable)

- DEPLOY FREEZE: no server deploy/restart without Steve's EXPLICIT go for that deploy
  (feedback_deploy_only_on_explicit_go). When authorized: freeze table creation via the
  shutdown switch FIRST, verify hall has ZERO tables of ANY status (count `<table `
  elements, never just playing="true"), then swap jar + force-recreate + operational +
  four switches. If the freeze call 401s, fix auth before proceeding, never skip.
- No uncommitted work in /private/tmp worktrees (a reboot wiped two uncommitted trees
  this session). Durable worktree: /Users/steve/gemp-k2-shield-repairs on branch
  k2/consolidated-2026-07-27. Commit WIP locally early; squash into one batch commit.
- Never touch engine/card Java, the deck library, DB, or client. Never compile/deploy
  from the dirty main tree (/Users/steve/gemp-swccg-public — Codex's WIP hand-merge).
  Push sealed branches to the `steve` remote (srdmusic/gemp-swccg) ONLY.
- Automated proof games: Rando vs Chosen One, never asdf.

## 2. CURRENT STATE (verified at handoff)

- LIVE jar sha256 880b89fd905dec7f276d... = branch commit 1421e9fcf. Contains: consolidated
  lineage (TDIGWATT + Hoth replay repairs + EOP), the Shield four-repair batch (1c6c352bd,
  byte-verified in live bytes per the m01467 packet), Batches Nine-Fourteen (QMC+CITC,
  Massassi, My Lord, Imperial Entanglements, Old Allies + Steve's two TFISMF commits,
  They Have No Idea), and Steve's srdmusic TFISMF saga fixes.
- QUEUED jars (packaged, byte-verified, parked in the worktree root, awaiting Steve's go):
  web.jar.batchfifteen-QUEUED (9b29c098..., 222_27 Empire Knows activation),
  web.jar.batchsixteen-QUEUED (099dc6e7..., Twin Suns), web.jar.batchseventeen-QUEUED
  (426288001d..., Watch Your Step family). DEPLOY THE NEWEST ONLY (each contains its
  predecessors) when Steve says go, with the full freeze procedure.
- Branch tip 072134cd5 pushed to the fork. Prior-jar backups sit next to the live jar as
  web.jar.pre-*-20260727. Full reactor at tip: 2817/0/0/26.
- Server: docker compose stack in /Users/steve/gemp-swccg-public/src, port 17001. After ANY
  restart, flip operational + aitables/privategames/stattracking/newaccounts (asdf/asdf).

## 3. THE BATCH RECIPE (nine clean batches this session; follow it verbatim)

1. Launch a read-only extraction agent for the objective (use the prompt shape from this
   session; template resources/objective_flip_audit/BATCH_NINE_SOURCE_LAW_2026-07-27.md;
   ALWAYS point it at the worktree, never the main repo — agents drift there).
2. Encode: registry keys (fail-closed, in ObjectiveAnalyzer.resolveFilter /
   resolveLocationFilter) + flipLocationRules in
   src/gemp-swccg-server/src/main/resources/objective_playbooks.json. Schema supports:
   control/occupy/controlWith/occupyWith/presentAt/at (location-counting), onTable
   (ACTOR-counting; fold location into the composite filter), utinniEffectCompleted
   (persistent counter, >= and < only), opponentConstraint, referenceController relative
   counts (strict > supported), allOf/anyOf per rule; multiple same-phase rules evaluate
   per-rule (CNF/DNF both work — CNF for OR-of-thresholds, two rules for cross-paired OR).
   IEFB is the only SpotOverride; captive-counting is inexpressible (record it).
3. Contract tests in src/test/.../common/strategy/*ObjectiveEngineContractTest.java,
   modeled on the existing nine. Fixture laws in §4.
4. Focused suite → full reactor (goldens in ObjectiveAnalyzerSharedGoldenTest pin the
   profile counts: currently 59 total / 24 enabled — update on activation/authorship).
5. Both changelogs + rulebook regen (tools/ in worktree) + ONE squashed batch commit +
   push + package in the isolated container (docker run gemp_app with the worktree and
   ~/.m2 mounted) + byte-verify + QUEUE the jar (freeze!) + mailbox note to codex.

## 4. FIXTURE LAWS (each cost a debug cycle; do not relearn)

- Objective setup prompts: answer "On which side" FIRST (its text also contains "deploy");
  ARBITRARY_CARDS card choices next; diagnose stalls with a throwaway dump test, never
  guess prompt text. Marker-ordering: MPG needs a 4th/5th/6th Marker on table (auto-pull).
- Same-title printings CONVERT on-table locations — never seed opponent control with the
  twin printing; use presence at existing sites or distinct titles.
- Raw zone manipulation does NOT pulse isTableChanged; every flip assertion needs a real
  deploy pulse afterward. A board that is flip-complete BEFORE a phase skip jams the
  skipper — complete the last leg after SkipTo.
- Pulse bodies landing on battlegrounds corrupt occupy-count assertions (bit three times).
  Land pulses on non-battlegrounds or remove them before the final count.
- Objectives tax their own fixtures: WYS classic +6 non-smuggler deploys; light Yavin
  interior sites refuse dark deploys (raw-place + separate pulse); activating the entire
  reserve starves the phase skipper.
- Light/dark icon quirks decide battleground status per PRINTING (no Icon.BATTLEGROUND;
  computed from force icons). Rogue One is a Zeta transport, not a corvette. Dark Jedi is
  computed (dark + character + ability >= 6). Senators are keyword-only. BCC 5_77 is a
  SECTOR. Sandwhirl is an Effect. Encode the CODE, never the printed text (601_146's back
  text is an uncoded copy-paste).

## 5. REMAINING QUEUE (P4 tail) + deferrals

- Profit pair 110_4 + 12_180; Local Uprising trio 7_137/7_298/7_299; Hidden Path 226_28;
  Set Your Course 111_6; HDADTJ-V 601_87 (sibling proof first). Expect more false twins.
- DEFERRED with complete packets on the branch: 501_19 (no profile + conquest-driven
  tests; BATCH_17_DEFERRAL doc) and the Rebel Strike Team family (needs three designed
  schema primitives: blown-away event relation, phase-window gate, per-location
  actor-pair minimum; BATCH_18_DEFERRAL doc has the design sketch). Do the primitives as
  a deliberate extension batch.
- Proof ceilings: everything this session is deterministic + (deployed|queued). NO live
  replay claims anywhere. Steve's live tests are the next proof layer; when he reports a
  failure, follow the takeover §12 patch-forward procedure.

## 6. WATCHERS TO RE-ARM EACH SESSION

Mailbox: python3 ~/claude-codex-mailbox/mailbox.py check --as claude --mark, then a
run_in_background poll loop past the latest seq (NEVER an inline &). Hall watcher when a
deploy is pending. The autonomous loop wakeup carries the idle heartbeat.

## 7. TAKEOVER CORRECTIONS LOGGED (trust these over the older docs)

P0 turn-counter REFUTED (evidence doc on branch). Bespin: Cloud City is a sector. 201_39
back title is "No One To Stop Us This Time". 301_4 is DARK. CITC has no blown-away
handler. Watch Your Step, TDIGWATT, and Rebel Strike Team "pairs" are NOT twins. The
m01438 drain-sign notation was inverted (code uses computeNetDrainBalance >= 2).

May the Force be with you. Ship.
