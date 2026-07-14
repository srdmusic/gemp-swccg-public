# Domain Registry Amendment Gate: `f2bb32e95`

Date: 2026-07-13
Reviewer: Codex/Alfred
Baseline registry amendment: `31b9f697c`
Immediate parent: `e447d306d`
Verdict: registry amendment `ADVANCE`; aggregate deployment `HOLD`

## Scope

- Commit `f2bb32e95` changes only `resources/DOMAIN_REGISTRY_2026-07-12.md`.
- Commit diff: `+5/-5`.
- `git diff --check f2bb32e95^..f2bb32e95`: clean.
- No Java, test, configuration, database, or deployment file changed.

## Independent Proof

- Section 2 has 22 domain headings whose live counts sum to 367.
- Section 7's per-domain list independently sums to 367.
- Section 5 has exactly 24 rows: 23 LIVE and one INERT V37.4-empty-check row.
- The three V27 siblings remain first-class rows with the verified source contributions:
  - maintenance Pass: `+25/+50`, one marker per bot;
  - maintenance Move: `-80`, one marker per bot;
  - buddy-protect Move: `-150/-250/-400`, two marker hits per bot as documented.
- The five stale 364-era statements are gone:
  - authority-table wording now distinguishes 23 LIVE plus one INERT row;
  - section 5 title covers the 21 ex-AMBIG arms plus three V27 siblings;
  - stable-marker sweep says 367 arms;
  - solo-formation is 15 and force-budget is 11;
  - MoveEvaluator is 37 and PassEvaluator is 4, with no prose saying the V27 rows are pending.
- Stale-text scan found no surviving `24 live arms`, `357-arm`, `solo-formation 14`,
  `force-budget 9`, `ME 35`, `PE 3`, or pending-prose statement.

## Boundary

`resources/DOMAIN_REGISTRY_2026-07-12.md` may advance as the current migration registry. This does
not retire any live arm. The document still records 344 arms without verified stable markers and
many TODO fixtures; each owner migration or deletion remains gated by its named route fixture,
Rando/ChosenOne parity, boundary math, and zero-contribution proof. Deployment remains separately
held.
