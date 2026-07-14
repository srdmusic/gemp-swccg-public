# Phase Execution Manifest

Purpose: one compact operational index. Read the active packet only. Do not reread the full mailbox or historical audit set for each phase.

Authoritative order: `CODEX_PHASE_CUTOVER_ORDER_2026-07-13.md` SHA256 `f8480bd7e77365ad2b4315c60f635660147935a8b143ac70c15bf3dbd9ada851`.

| Step | Phase | Packet | SHA256 | State |
|---|---|---|---|---|
| 1 | Accepted-response runtime | `CODEX_FINALIZER_RUNTIME_PREREQUISITE_PACKET_2026-07-13.md` | `bc430fee3d76ba83b1ed20600a6f58067d5234c3a6653013854ca9124125f1bb` | RELEASED, K-2 editing |
| 1b | V44/V67j finalizer pilot | `K2_V44V67J_FINALIZER_PILOT_PACKET_DRAFT_2026-07-13.md` | `7b7bc814e343884efad643342d206c7d78384da62f6aa0b3514c14447edf8c98` | FROZEN, waits step 1 gate |
| 2 | ACTIVATE+CONTROL | `CODEX_ACTIVATE_CONTROL_PHASE_B_PACKET_2026-07-13.md` | `637ae1de6d670880b40bdf70e6e074db713430d68d0c0dbc42952c7df90e0e1b` | FROZEN, waits step 1b gate |
| 3 | DRAW | `CODEX_DRAW_PHASE_PACKET_2026-07-13.md` | `9f2a40acf54e81081335d3caa666ed5a3e4d43ab495122c7ef8951155a1a70cd` | FROZEN, waits step 2 gate |
| 4 | PULL/SEARCH | `CODEX_PULL_PHASE_PACKET_2026-07-13.md` | `37897bf32f5de97f4dd3ea41f979005f9cfb6b0d8685e34db1f3241465b15406` | FROZEN, waits step 3 gate |
| 5 | Objective facts/adapters | `CODEX_OBJECTIVE_FACTS_ADAPTER_PHASE_PACKET_2026-07-13.md` | `afda65ec75911aa6274bd49a9d9deeb6e9275e832977179eb6bec2a0eda03720` | FROZEN, waits step 4 gate |
| 6 | DEPLOY owner | `CODEX_DEPLOY_PHASE_PACKET_2026-07-13.md` | `6ba53824b27c09e97284654e69c68d734781d37a2e7292d9f9b0db012b86fe09` | FROZEN, waits step 5 gate |
| 7 | BATTLE | `CODEX_BATTLE_PHASE_PACKET_2026-07-13.md` | `85498b28364acfaf8994813cffc1c26fd5f0d0fa16cbf32fbf32eab1459b778e` | FROZEN, waits step 6 gate |
| 8 | MOVE | `CODEX_MOVE_PHASE_PACKET_2026-07-13.md` | `9daebb9d96f03dbd41beb3ea8b214d00f2f2b0fdc20863ac91fa3e6809593217` | FROZEN, waits step 7 gate |
| 9 | SETUP | `CODEX_SETUP_PHASE_PACKET_2026-07-13.md` | `0d56f67c0b89675c2f5358b4695a3d938365ee4ea22201c26b7d0fc93831c546` | FROZEN, waits step 8 gate |
| 10 | Interceptor/legacy retirement | `CODEX_INTERCEPTOR_LEGACY_RETIREMENT_PACKET_2026-07-13.md` | `1777d2f838dddeb787a7806598e5326f253d0a9b77611c9f78c7ee0cfb97556d` | FROZEN, waits step 9 gate |
| 11 | Deploy weights/solo-plan | `CODEX_DEPLOY_WEIGHT_CONSOLIDATION_CONTRACT_2026-07-13.md` | `2f705cc4e7b2cd042535c0774ca735d2dc66c18b41b6946a55f8e47d77f93aac` | FROZEN contract, waits step 10 gate |
| 12 | Aggregate gate/review/deploy | `CODEX_FINAL_AGGREGATE_DEPLOY_GATE_2026-07-13.md` | `62e7566b5ac371945f3768739fdac2fcc779a5129174e6b56d58a37f948589be` | FROZEN, waits all phase commits |

## Cadence

For each step: release exact packet hash, make one coherent edit including shadow comparison and exclusive owner selection, run no tests during edits, then run the active packet's single focused verification and compile or package gate. Do not broaden a packet that explicitly forbids a full-module or package run. Create one phase commit, perform one independent Codex gate, then release the next step. Step 10 is the narrow exception: retire one independently proven group per coherent commit because the post-2.8 boundary forbids combining unrelated deletion groups. Physical legacy deletion waits for step 10, except the explicitly frozen V44/V67j pilot. Never push. No game/browser/live deployment work before step 12.

## Final Lock

Step 12 requires all three:

1. Aggregate offline tests and static owner/deletion proof pass.
2. Fresh Fable review agrees with the final aggregate diff and handoff.
3. Independent Codex/work-verifier gate passes.

Only then build, load, verify, and deploy using `resources/BUILD_AND_DEPLOY.md`.
