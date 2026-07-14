# Rando Cleanup Proof Gate

Current measured candidates and exclusions are recorded in
`Handoffs/CODEX_RANDO_CLEANUP_INVENTORY_2026-07-13.md`.

Date: 2026-07-13
Owner: Codex/Alfred
Implementer: K-2/Claude
Status: required before deleting legacy residue

## Purpose

Delete superseded residue without changing compiled behavior or concealing an unproven owner
retirement. Cleanup is not a substitute for semantic migration.

## Early cleanup 1.5A

Allowed in a dedicated commit:

- Commented rule bodies and obsolete banners that contain no executable token.
- Explicit compile-time-false superseded blocks after confirming the compiler emits no code.
- Imports made unused only by the approved deletion.
- Non-source backup and generated log artifacts, in a separate artifact-only commit.

Not allowed in 1.5A:

- Live statements, fields, methods, types, annotations, resources, or configuration.
- A row still classified active, delegated, ambiguous, or lacking retirement fixtures.
- A dormant framework merely because it has no current caller.
- Version history, rationale, or revert evidence needed to explain surviving behavior.

## Required inventory

Every deletion row records:

- File and stable source marker.
- V-tag or rule-arm id.
- Current registry state and replacement owner.
- Why the text is non-executable.
- Fixtures that cover the replacement behavior.
- Revert source: commit, backup path, or both.

## 1.5A proof

1. Freeze the baseline commit and deletion inventory.
2. Compile baseline and candidate from isolated clean trees with the same JDK and build command.
3. Compare normalized `javap -p -c -s -constants` output for every affected class. Do not compare
   raw class hashes or debug line tables.
4. Require identical method descriptors, instructions, exception tables, and constants for source
   cleanup. Any semantic bytecode difference moves the item to later owner-retirement work.
5. Run focused fixtures for every named replacement plus the deterministic merge/router suite.
6. Run Rando/ChosenOne source and compiled parity checks for mirrored files.
7. Confirm V191 candidate order, contribution sequence, raw score bits, veto set, winner, and final
   response are unchanged on the covered corpus.
8. Build the complete affected modules. A compiling diff without the earlier checks does not pass.

The gate is `ADVANCE` only when all eight checks pass. Otherwise it is `HOLD`; split out the failing
item instead of weakening the comparator.

## Compiled owner retirement

Removing a method, class, field, or executable branch is a later semantic batch. It additionally
requires:

- Exact source and bytecode caller search.
- Reflection, service-loader, dependency-injection, serialization, resource, and configuration
  search.
- An authoritative replacement owner in the domain registry.
- Route fixtures proving the replacement is reached by both bots where parity applies.
- Shadow traces showing no unapproved contribution, veto, rank, winner, or response delta.
- A separate commit so rollback does not also revert unrelated migration work.

`ObjectiveHandler`, `ActionAudit`, and fallback routes remain until this stronger proof passes.

## Deployment

Pure 1.5A cleanup does not justify a deployment by itself. It may ride with a later advanced batch,
after normal compile, jar-content, load, runtime-fire, idle-game, and rollback gates pass.
