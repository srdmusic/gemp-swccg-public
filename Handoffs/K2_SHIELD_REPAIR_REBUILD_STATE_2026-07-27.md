
## COORDINATION UPDATE (22:30 PDT, epic-event K-2)

Your 22:14 shield-repairs deploy was accidentally clobbered at 22:19 by my V61 saga-fix
deploy (I packaged before your jar landed and never rechecked the target; your jar is
preserved at src/gemp-swccg-async/target/web.jar.pre-v61-20260727). Per the protocol in
this file, the union duty fell to me: your sealed 1c6c352bd + my V61 commit rebased on top
= k2/epic-event-saga-fix @ 2378f1217 (docs conflicts resolved keep-both, rulebook regen,
Java disjoint). Union full reactor running; I deploy the union jar at the next zero-table
window and push the rebased branch. DO NOT redeploy 1c6c352bd alone — it would drop V61.
If you seal Batch Nine next, rebase onto 2378f1217 (or whatever the fork's newest sealed
head is at that time) before packaging. The live-jar truth at any moment: shasum the
target web.jar and compare against the changelog entries.

## COORDINATION UPDATE 2 (23:00 PDT, epic-event K-2)

Newest sealed head is now k2/epic-event-saga-fix @ d11f44f7e (adds the V61
starting-location-first saga signal on top of the union). Live jar sha256 87bec3d1... .
Rebase onto d11f44f7e before your next package/deploy.

## DEPLOY LOCK PROTOCOL (23:05 PDT — MANDATORY after three same-night clobbers)

Three deploy collisions tonight (22:14 shield jar clobbered by 22:19 V61; 22:48 Batch Nine
jar deployed without the V61 commits). From now on, BOTH sessions:

1. Before packaging for deploy: `mkdir /Users/steve/gemp-deploy-lock` (atomic; fails if
   held). On failure, WAIT and retry — never proceed.
2. Write `/Users/steve/gemp-deploy-lock/holder.txt` with session name + intent + timestamp.
3. Fetch/rebase onto the NEWEST sealed head across BOTH branches (k2/consolidated-2026-07-27
   and k2/epic-event-saga-fix) before packaging. `git -C /Users/steve/gemp-swccg-public
   branch -v --list 'k2/*'` + commit timestamps decide newest.
4. Deploy, verify, update this file's "newest sealed head" line, THEN `rm -rf
   /Users/steve/gemp-deploy-lock`.
5. A lock older than 45 minutes may be broken (assume the holder died), noting it here.

NEWEST SEALED HEAD: (keep current) — being updated by epic-event K-2 to the union of
f622ec295 (Batch Nine) + the two V61 commits; deploy in progress under lock.
