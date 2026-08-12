# DB72276 correlator self-test

Status: exact identity smoke test passed on 2026-08-12. This file supersedes the original mtime-based self-test. Replay mtime and newest-file selection are not evidence.

Command:

```text
python3 outputs/rando-batch1-2026-08-12/harness/correlate_evidence.py \
  --game-id 72276 \
  --label phase12-existing \
  --no-write
```

Validated identity:

| field | value |
|---|---|
| game_history.id | `72276` |
| winner / loser | `asdf` / `~Rando_Cal` |
| orientation | Steve `LIGHT`, Rando `DARK` |
| DB start UTC | `2026-08-12T04:17:34.345000+00:00` |
| DB end UTC | `2026-08-12T04:42:32.901000+00:00` |
| Steve recording | `replays/asdf/pd4emldbzpvhtduh.xml.gz` |
| Rando recording | `replays/~Rando_Cal/5vgvkjr4wo3ofq8y.xml.gz` |
| final public SHA-256 | `78734b95a21789ce1b98e9a4897da4c9594215662c7dc1a785a0ec7e12cd38ea` |
| winner reason | `Depleted opponent's Life Force` |
| loser reason | `Life Force depleted` |
| exact Dark-Rando start | `logs/gemp-swccg.log:71`, `2026-08-12 04:17:34,417` |

The read-only database query, exact recording ownership, final replay segments, participant messages, terminal winner and loser reasons, matching public fingerprints, DB-bounded log window, and unique Dark-Rando start marker all validated.

This is identity proof only. Phase 1 and Phase 2 acceptance still requires the selected log blocks and replay consequences documented in `controlled_validation_matrix.md`. Tag presence by itself is not outcome proof.
