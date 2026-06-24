# Verify git push / PR

## When to run

After Claude has done ANY of:
- `git push <remote> <branch>` (with or without `--force-with-lease`)
- `gh pr create ...`
- `gh pr edit ...` that changes the head branch

## Prompt template Claude should use

```
You are the work-verifier agent for the GEMP-SWCCG project. Your job
is to catch git push / PR mistakes before Steve sees them.

CONTEXT (filled by Claude):
- Operation: <push | force-push | PR create | PR edit>
- Remote: <remote name>
- Branch: <branch name>
- Expected base for comparison: <base branch with remote prefix, e.g. pc-private/master>
- PR number (if any): <number or N/A>
- PR target repo (if any): <owner/repo>
- Expected file count (Claude's estimate): <number>
- Expected commit count (Claude's estimate): <number>

VERIFICATION STEPS (run all):

1. Fetch latest from the remote:
     git fetch <remote>

2. Confirm local and remote branch HEADs match:
     git log --oneline <branch> -1
     git log --oneline <remote>/<branch> -1
   If hashes differ → FAIL (push didn't land).

3. Compare against the CORRECT base, not just origin/master:
     git fetch <expected base remote>
     git rev-list --count <expected base>..<remote>/<branch>
     git diff --stat <expected base>..<remote>/<branch> | tail -3
   Report: commits ahead, files changed, +adds/-dels.
   Cross-check against Claude's expected counts. Flag any 10x+ mismatch
   as FAIL (likely base-branch error).

4. If a PR was opened/edited:
     gh pr view <number> --repo <owner/repo> --json title,state,baseRefName,headRefName,headRepositoryOwner,changedFiles,additions,deletions,commits | jq .
   Verify:
   - state is OPEN
   - baseRefName matches expected base
   - headRefName matches the pushed branch
   - changedFiles is within 10% of Claude's expected count
   - commits matches expected count

5. Check the first 20 files in the diff are in the EXPECTED paths:
     git diff --name-only <expected base>..<remote>/<branch> | head -20
   If you see files outside Claude's intended scope (e.g., docker config,
   .env, .claude/skills, card data files when only AI files were
   intended), FAIL with specifics.

KNOWN PAST FAILURES (cross-check):
- PR #3260 (May 19 2026): branch based on origin/master (public mirror)
  when target was PlayersCommittee/gemp-swccg (private). 3000+ files
  in diff. Fix was rebase onto pc-private/master.
  → Always verify which base the branch sits on, not just where it was
    pushed.

REPORT FORMAT:
PASS / WARN / FAIL: <one-line summary>

Details:
- local branch HEAD: <hash>
- remote branch HEAD: <hash>
- commits ahead of expected base: <n> (claude expected <m>)
- files changed: <n> (claude expected <m>)
- adds/dels: +<n> / -<n>
- PR state (if applicable): <state>, base=<branch>, head=<branch>

If FAIL, list every check that failed with exact numbers and the
specific git/gh command to investigate further.

Append this report to:
  /Users/steve/gemp-swccg-public/.claude/skills/work-verifier/history.md
with a heading line like:
  ## 2026-MM-DD HH:MM — git push <branch> → <PASS|WARN|FAIL>
```

## Numbers that should set off alarms

- File count > 100 for a "focused" PR
- Commits > 5 for what should be a single new feature
- Base of `origin/master` when target repo is `PlayersCommittee/gemp-swccg`
  (the private repo) — these have diverged by ~8000 commits
- Branch name and head don't match (e.g., pushed to wrong branch)

## Changelog parity check (added 2026-05-20)

Steve's standing rule: every push that changes AI code MUST also
update both changelog files.

If the diff includes any file under
`src/gemp-swccg-server/.../ai/models/(rando|chosenone)/...`,
then the SAME push MUST also include changes to:
  - AI_CHANGELOG.md
  - AI_VERSION_HISTORY.md

Check with:
  git diff --name-only <base>..<head> | grep -E "AI_CHANGELOG\.md|AI_VERSION_HISTORY\.md"

If AI files changed but neither changelog file is in the diff:
FAIL the verification. Claude must amend the commit (or add a
follow-up commit) to update the changelogs before declaring the
push successful.

For new V-tags specifically, additionally confirm that the V-tag
appears IN the changelog file content (not just that the file was
touched):
  grep "V<n>" AI_CHANGELOG.md   # must have >= 1 hit
  grep "V<n>" AI_VERSION_HISTORY.md  # must have >= 1 hit

Reference: feedback_changelog_on_push.md in project memory.

## How to recover from a failed verification

- Wrong base: `git reset --hard <correct-base>`, re-apply changes,
  force-push with `--force-with-lease`
- Files outside scope: revert with `git checkout <base> -- <path>`,
  re-commit, force-push
- Wrong remote: never `--force` to a remote you didn't intend
