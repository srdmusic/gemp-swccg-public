# Batch Nine source law (extracted 2026-07-27, K-2 read-only packet)

Worktree: k2/consolidated-2026-07-27 post-1c6c352bd. Full citations verified against card Java.

## QMC 109_4 / 109_4_BACK (Quiet Mining Colony / Independent Operation) — LIGHT

FRONT setup (L51-68): required deploy Bespin_system (free, no reshuffle); required deploy
Cloud_City_battleground_site (free, no reshuffle).
FRONT pull (L71-94): once per YOUR deploy phase, 1 Force, deploy to Bespin system any
site-or-cloud-sector (reshuffle). NOT Cloud-City-restricted.
FRONT modifier (L97-103): opponent force loss from owner's drains at Bespin_location capped at 1.
FRONT flip (isTableChanged L123), allOf:
  - canBeFlipped
  - NOT opponent controls Bespin_location (SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE) (L125)
  - owner controls Bespin_Cloud_City (IEFB) (L126)
  - anyOf: owner controls >=2 Cloud_City_site (L127)
          OR (Lando-or-Lobot on Cloud City (any controller, IEFB, L128) AND owner controls >=1
              Cloud_City_site (L129))
  CORRECTED NUANCE (verified against Card5_077.java + Filters.java L17875/L18019): Bespin:
  Cloud City is a SECTOR (AbstractSector, Keyword.CLOUD_SECTOR), NOT a Cloud_City_site
  (CLOUD_CITY_LOCATION + SITE subtype). The Lando/Lobot alternative's >=1-controlled-site
  leg is therefore real and independent of the mandatory sector control. An earlier
  extraction claimed the leg was auto-satisfied; that claim was wrong. The sector DOES
  count in the back side's or(Cloud_City_site, Bespin_cloud_sector) pool via
  Bespin_cloud_sector = partOfSystem(Bespin) + CLOUD_SECTOR.
FRONT hard-loss (L111-120): Bespin system blown away -> objective OUT OF PLAY.

BACK modifiers (L51-63): aliens/cloud cars/[Independent] starships immune to attrition <4;
at Bespin locations controlled-with-alien: opponent may not modify/cancel owner drains;
[Independent] starships deploy -1.
BACK pull (L66-85): once per YOUR deploy phase, FREE, deploy ANY docking_bay (reshuffle).
BACK flip-back (isTableChanged L105), anyOf:
  - opponent controls Bespin_system (IEFB) (L107)
  - opponent controls >=3 of or(Cloud_City_site, Bespin_cloud_sector) (L108)
BACK hard-loss (L93-102): Bespin blown away -> OUT OF PLAY.

## CITC 301_2 / 301_2_BACK (City In The Clouds / You Truly Belong Here With Us) — LIGHT

FRONT setup (L44-67): required Bespin_system (free); required Cloud_City_site with
specialLocationConditions=battleground (free); optional 0-1 Weather_Vane (free).
FRONT pull (L70-93): once per TURN (not per deploy phase), 1 Force, deploy any
Cloud_City_location that will be battleground (reshuffle).
FRONT flip (isTableChanged L101), allOf:
  - owner controls >=2 Cloud_City_battleground_site (IEFB) (L103)
  - owner occupies Bespin_system (INCLUDE_CAPTIVE_AND_EXCLUDED_FROM_BATTLE — captives count) (L104)
  - NOT opponent controls Cloud_City_site (INCLUDE_CAPTIVE_AND_EXCLUDED_FROM_BATTLE) (L105)
FRONT hard-loss: NONE. No blown-away handler on either side (differs from QMC).

BACK payoffs: once/game deploy Cloud City Celebration (7_55) from Reserve (L56-71); once per YOUR
control phase 2 Force -> any Interrupt to hand (L73-92); once/turn react-move a character when
opponent initiates battle/drain at a Cloud_City_site (L98-139). No while-active modifiers.
BACK flip-back (isTableChanged L147-160): count opponent-controlled Cloud_City_site STRICTLY >
owner-controlled count (IEFB). Strict comparator confirmed.

## Supporting cards
Bespin system printings: LIGHT 5_76, DARK 5_164, DARK 223_8 (all match Filters.Bespin_system =
SYSTEM + title). Bespin: Cloud City: LIGHT 5_77, DARK 5_165; Filters.Bespin_Cloud_City is
title-matched; it is a SECTOR, not a site (see corrected nuance above). Weather Vane 5_30. Cloud City Celebration 7_55.
Lando = persona LANDO, Lobot = persona LOBOT; OnCloudCityCondition spots any matching card on
Cloud City including permanent-aboard, no ownership restriction.

## Discrepancies vs takeover §9 P3 claims
1. QMC's Lando/Lobot route still requires one separately controlled Cloud City site because
   Bespin: Cloud City is a sector, not a site (see NUANCE above).
2. CITC has NO blown-away hard-loss on either side; the takeover's family-wide implication is wrong.
3. CITC front setup optionally deploys Weather Vane (omitted from takeover summary).
4. CITC captive-counting: occupy-Bespin and opponent-zero legs use INCLUDE_CAPTIVE_AND_EXCLUDED;
   the two-site control leg uses only INCLUDE_EXCLUDED_FROM_BATTLE.

## Runtime state
Profiles exist and are loader-enabled for both (Objective_Playbook_Facts_2026-07-08.json plus
the runtime objective_playbooks.json; inventoryRows 12 and 29). The QMC runtime profile now
represents all three front-side legs, including the independent one-site floor and any-owner
Lando/Lobot-at-a-site route, plus both back-side thresholds. CITC's three front-side legs and
strict back-side comparator are also encoded. Audit records/dossiers remain source evidence;
decision tests, native trigger tests, and live play remain the behavioral proof.
