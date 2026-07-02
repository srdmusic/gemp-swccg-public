# K-2 Handoff — 2026-06-23 (verification + version-history + onboarding consolidation)

**From:** the verification / docs K-2 (session fbf0d964). **For:** the other two K-2s + Steve.
**Read alongside:**
- `K2_HANDOFF_2026-06-23.md` — angle 1: collision map for the 3 K-2s, V185/V186 code detail, the two gotchas.
- `K2_HANDOFF_2026-06-23_audit-V185-council.md` — angle 2: backup/breadcrumb audit + council + prioritized gaps.
- `RANDO_MISSING_LOGIC.md` — the 3 genuinely-unbuilt behaviors.
- Onboarding hub: `/Users/steve/gemp-swccg-public/resources/k2-resources/distilled/00-START-HERE.md`.

My angle was NOT writing new Rando logic. It was proving what's real, recording it, and making the onboarding current. I touched zero `src/` code this session.

---

## What I did this session

- **Settled the "did we copy Rando wrong?" scare — by reading actual files, not version labels.** The live install IS the most complete Rando on disk; it matches the `ai-improvements-v91` fork plus V185/V186. The trap that made it look mis-copied: `GEMP ARCHIVE/gemp-swccg-public-OURS-2026-06-20`'s working tree is checked out to `run-devs-pure` (the devs STUB, only `rando`, smaller files). Use `GEMP ARCHIVE/gemp-swccg-public-BACKUP-2026-06-20`'s working tree (the real full fork) as the reference, not OURS's working tree.

- **Content archaeology audit** (read actual code across ~30 branches and copies, not V-tag counts). Confirmed three things: (1) no branch or backup holds any implementing logic the live install lacks; (2) the 3 behaviors in `RANDO_MISSING_LOGIC.md` are genuinely missing by CONTENT everywhere (Mapuzo opponent-clear counter, far-behind save-a-Jedi skip, Levitation/Sith-Fury turn-4 gate); (3) it CAUGHT that V186 WAS built when my own earlier grep and the first sweep both missed it — the +400 Starkiller block matches by blueprint id (`208_51`), not the title "starkiller", so a title grep is blind to it.

- **Verified V186 end-to-end by reading the code** (all three blocks) plus every `.md` in GEMP ARCHIVE/gemp-swccg-public-BACKUP-2026-06-20. The strategy/handoff docs are SILENT on I Want That Map, so the picks rest on Steve's stated strategy plus the card text. Confirmed naming Starkiller is REQUIRED: the system has no battleground icon, so a generic battleground heuristic misses it (my earlier generic idea would have failed). The fix is correct and structurally sound.

- **Recorded V185 + V186 in `AI_VERSION_HISTORY.md`** (both the BACKUP copy and the `k2-resources` archive copy, synced identical, 5358 to 5431 lines, through V186, newest-first above the V184 banner). The chronological history had been two versions behind. The live install uses `AI_CHANGELOG.md` (which already had both); `AI_VERSION_HISTORY.md` lives only in BACKUP + k2-resources by convention.

- **Consolidated the onboarding for a fresh K-2.** Added the auto-loading memory `project_3k2_consolidation_2026-06-23.md` (the START-HERE orientation), updated the `reference_k2_resources_folder` memory + the MEMORY.md index, and freshened the stale deep distilled docs: added V185/V186 to `rando-history-highlights.md`, put currency banners on `K2_ONBOARDING.md` (was ≤V138) and `ai-instructions.md` (was ≤V126). The hub `00-START-HERE.md` was already current (another K-2 updated it 12:39).

(The auto-pass / phase-skip fix and the card-image proxy are this lineage's earlier infra work; they live in `AI_CHANGELOG.md` and angle 1's handoff. Not re-claiming them here.)

## Where I left things

- **V185 + V186:** in source, in `AI_CHANGELOG.md`, and now in `AI_VERSION_HISTORY.md`. NOT compiled, NOT committed. The running jar is still old code.
- **Onboarding:** current. A fresh K-2 opening this project auto-loads the orientation memory, which routes to the hub and all three handoffs.
- **I did NOT touch:** the V185/V186 code, the other K-2s' handoffs, `RANDO_MISSING_LOGIC.md`, or anything under `src/`. Verification + docs/memory only, to stay out of your lanes.

## Open / pending (needs Steve's call)

- **#1 RISK: nothing is committed.** HEAD is the devs base `55c22cf49`. The whole fork plus V185/V186 plus all three handoffs live only in the working tree, and core files (`ObjectiveAnalyzer.java`, `DeckOracle.java`, `ImageProxyRequestHandler.java`, the handoff/`RANDO_MISSING_LOGIC` docs) are git-UNTRACKED. One `git checkout` / `reset --hard` / re-clone wipes all of it. Commit first.
- **Build + deploy V185/V186** so they actually fire: `cd src && mvn -q -pl gemp-swccg-async -am package -DskipTests`, then `bin/gemp restart`, then play a real I Want That Map game and grep the container's `nohup.out` for the V-tags. "Compiles" is not "fires."
- **`RANDO_MISSING_LOGIC.md` Starkiller section is STALE** — it says "never built"; it is now V186. Someone should fix that section.
- **chosenone/ mirror** of V185 and V186 (both are rando-only right now).

## Re-verify my claims (don't trust this doc, run these)

```bash
cd /Users/steve/gemp-swccg-public
R=src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando
grep -nE '════ V18[4-6]' /Users/steve/gemp-swccg-public/resources/k2-resources/originals/02-rando-history/AI_VERSION_HISTORY.md   # history through V186
grep -rn 'V186' "$R"                                                                                   # V186 = 3 blocks, all gated to "i want that map"
grep -rn 'scoreStartingCard\|\.setObjective(' src/ | grep -v ObjectiveHandler.java                     # empty = ObjectiveHandler is dead
git log --oneline -1                                                                                   # 55c22cf49 = devs base = nothing committed
ls /Users/steve/.claude/projects/-Users-steve-gemp-swccg-public/memory/project_3k2_consolidation_2026-06-23.md  # fresh-K2 orientation memory
```
