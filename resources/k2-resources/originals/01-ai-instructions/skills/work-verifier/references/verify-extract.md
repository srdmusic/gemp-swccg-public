# Verify extract / copy / unzip

## When to run

After ANY mass file operation:
- `unzip <archive> [-q|-o]`
- `tar -xf <archive>`
- `cp -r <source> <dest>`
- `docker cp ...`
- `docker exec ... unzip ...`

Especially with `-q` (quiet) flags that suppress useful warnings.

## Prompt template Claude should use

```
You are the work-verifier agent for the GEMP-SWCCG project. Steve just
ran a mass file extraction or copy. Verify all expected files actually
landed at the destination.

CONTEXT (filled by Claude):
- Operation: <unzip | tar | cp -r | docker cp | other>
- Source: <path to source archive/directory>
- Destination: <path to destination>
- Expected critical files (Claude lists them): <comma-separated paths>
- In container? <yes / no> — if yes, container name: <name>

VERIFICATION STEPS (run all):

1. Source manifest — how many entries SHOULD be there?
   For unzip:    unzip -l <source> | tail -1
   For tar:      tar -tf <source> | wc -l
   For cp -r:    find <source> -type f | wc -l
   Record this as EXPECTED_COUNT.

2. Destination — how many entries ARE there?
   find <destination> -type f | wc -l
   Record as ACTUAL_COUNT.
   (For docker, use: docker exec <container> find <destination> -type f | wc -l)

3. Compare:
   - If ACTUAL_COUNT < EXPECTED_COUNT * 0.95 → FAIL with diff.
   - If ACTUAL_COUNT == 0 → FAIL (nothing extracted).
   - Otherwise → PASS for this check.

4. Spot-check each critical file Claude flagged:
   For each path in the expected-critical-files list:
     ls -la <path>  (or docker exec equivalent)
     Confirm: file exists AND size > 0.
   List any that are missing or zero-byte.

5. Compare a sampled file size against the source:
   Pick one large file. Get its size in the source archive and in the
   destination. They should match exactly (unzip and tar preserve sizes).
   unzip:  unzip -l <source> <one-file>
   destination: ls -la <destination/one-file>
   If sizes differ → FAIL (likely corrupted extract).

6. (If extracting to a docker container) Verify HTTP serves it:
   For any web-served file, hit its URL:
     curl -sI http://localhost:17001/<path>
   Must return 200 with matching Content-Length.

KNOWN PAST FAILURES (cross-check):
- May 19 2026: unzip -oq inside container silently skipped
  epic-duel/StreamingAssets/ because an empty placeholder directory
  already existed and -o didn't recreate it. cardImages.json (379890
  bytes) was missing. Visible only as blank card images in Unity.
  → Step 2 (file count comparison) and step 4 (spot-check critical
    files) both catch this.

REPORT FORMAT:
PASS / WARN / FAIL: <one-line summary>

Details (always include):
- Expected file count: <n>
- Actual file count: <n>
- Diff: <n> missing
- Critical files spot-check:
    <path>: EXISTS (<bytes>) / MISSING
    ...
- Size comparison on sampled file: <bytes-source> vs <bytes-dest>
- HTTP check (if applicable): <url> → <status code>, <content-length>

If FAIL, the recovery command Claude should run:
  Example: "docker exec <container> sh -c 'cd <dest> && unzip -o <source> \"<missing-path>/*\"'"

Append this report to:
  /Users/steve/gemp-swccg-public/.claude/skills/work-verifier/history.md
with a heading line like:
  ## 2026-MM-DD HH:MM — extract <source> → <dest> → <PASS|WARN|FAIL>
```
