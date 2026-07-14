# Finalizer F3 Correction Gate for 4a5e7d6b8

Date: 2026-07-13
Commit: `4a5e7d6b84df42623e4c274a89dcf1c7a8151a53`
Parent: `c6695168be29225e14f13eab36bc88314741d141`
Verdict: `F3 ADVANCE AS INERT CONTRACT`

## Scope

The commit changes six files: the pure finalizer service, response contract, finalized-response
record, focused contract test, and the two required changelog/history files. It adds no runtime
consumer and changes no legacy safety, interceptor, tracker, mediator, engine, or phase-owner path.

## Independent Proof

- `git diff --check 4a5e7d6b8^ 4a5e7d6b8`: pass.
- Detached parent affected-module package: pass.
- Detached candidate affected-module package: pass.
- Focused corpus: 140 reported, 139 pass, zero failures/errors, one named post-F1 bounds test skip.
  `ResponseFinalizerContractTest` contributes 20/20 passing cases.
- No production source outside `ai/models/common/finalization/` references `ResponseFinalizer`,
  `FinalizedResponse`, `ResponseContract`, or `ResponseIntent`.
- Every `FORCED` production path goes through the single `forced(...)` helper. The record constructor
  requires `ForcedChoice` for `FORCED` and forbids it for every other status.

## Contract Review

The two findings in `CODEX_FINALIZER_F0F3_GATE_92965934B_2026-07-13.md` are corrected:

1. `Pass` now consumes `policyPassAllowed`.
   - policy allowed plus empty wire accepted: `ACCEPTED` empty;
   - policy allowed but empty wire rejected: `FORCED/PASS_NOT_WIRE_ENCODABLE`;
   - policy denied: `FORCED/POLICY_PASS_DENIED`, or typed `REJECTED` when no legal fallback exists.
2. `Acknowledge` is accepted only for the declared `EMPTY` and `CARD_ACTION_CHOICE` shapes. It cannot
   bypass pass policy for card-selection, arbitrary-card, action, multiple-choice, or integer shapes.
3. Every `FORCED` result carries a typed reason. Tests assert both construction directions and the
   exact reason for seeded candidate, deterministic card-fill, and integer-default paths.

The forced card-fill path does not consult `maximum` directly, but this is legal for all contracts
derived from the real engine. `CARD_SELECTION` and `ARBITRARY_CARDS` never emit `noPass`; their policy
denial therefore requires `minimum > 0`. `DecisionFacts` enforces `maximum >= minimum`, so a forced
target of at least one cannot exceed a zero maximum. Real `0..0` viewing shapes remain policy-pass
allowed and finalize as empty rather than entering the forced path.

## Holds

This verdict clears the narrow F3 correction only. F1 checked engine bounds and F2 bounded mediator
retry/clock repair remain ahead of any runtime use. Trace Stage 4 mutation cardinality, interceptor
cutover, duplicated safety/tracker retirement, aggregate deployment, and push all remain held.

