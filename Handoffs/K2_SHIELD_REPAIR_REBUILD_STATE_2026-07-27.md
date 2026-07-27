
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

NEWEST SEALED HEAD: k2/epic-event-saga-fix @ bd9e83847 = f622ec295 (Batch Nine) + the two
V61 commits. Live jar sha256 2660420006cf58ace757... deployed 23:15 PDT, union reactor
2782/0/0/26, byte-verified (V61 location + counts, Shield repairs, TDIGWATT, Batch Nine).
Rebase onto bd9e83847 before your next package.

## COORDINATION UPDATE 3 (23:35 PDT, epic-event K-2) — READ BEFORE YOUR NEXT DEPLOY

Your ~23:00 jar write (7b4c4bc2, Batch Nine/Ten lineage, NO V61 commits) raced my full-union
deploy inside the same minute and left disk != running JVM. I preserved your jar as
web.jar.batchX-unreconciled-7b4c4bc2 and restored the running union (2660420006) to disk.
Steve is live-testing the V61 saga fix on it RIGHT NOW — do not touch the server.

Your Batch Nine + Batch Ten commits are integrated: I rebased the two V61 commits onto your
5c890a33e -> new sealed head k2/epic-event-saga-fix @ bdbdde60a (reactor running). I hold
/Users/steve/gemp-deploy-lock until that deploys. AFTER this, ALWAYS: (1) take the lock
BEFORE packaging, (2) rebase onto the newest sealed head across both branches, (3) verify
the CONTAINER hash after restart, (4) update this file, (5) release the lock. Your last two
deploys dropped live behavior (V61) on the floor; the lock protocol is not optional.

## COORDINATION UPDATE 4 (23:45 PDT, epic-event K-2)

Batch Ten union DEPLOYED: k2/epic-event-saga-fix @ bdbdde60a (your 5c890a33e + the two V61
commits), reactor 2786/0/0/26, live+disk jar sha256 26644706e9ee891d217c... container hash
verified after restart. Lock released. This is the newest sealed head — rebase onto
bdbdde60a. Your 7b4c4bc2 jar remains preserved as web.jar.batchX-unreconciled-7b4c4bc2;
diff it against bdbdde60a's build if you think it carried anything not in your branch.
