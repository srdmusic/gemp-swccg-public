package com.gempukku.swccgo.ai.models.chosenone.evaluators;

import com.gempukku.swccgo.ai.models.chosenone.RandoConfig;
import com.gempukku.swccgo.ai.common.AiPriorityCards;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Action Text Evaluator
 *
 * Handles text-based action ranking by pattern matching action text.
 * Ported from Python action_text_evaluator.py (~1350 lines)
 *
 * This evaluator provides baseline rankings for common SWCCG actions
 * based on analyzing the action text.
 */
public class ActionTextEvaluator extends ActionEvaluator {

    // Rank deltas (from Python)
    private static final float VERY_GOOD_DELTA = 50.0f;
    private static final float GOOD_DELTA = 30.0f;
    private static final float BAD_DELTA = -30.0f;
    private static final float VERY_BAD_DELTA = -50.0f;

    // Pattern for extracting blueprint ID from action text HTML
    private static final Pattern BLUEPRINT_PATTERN = Pattern.compile("value='([^']+)'");

    // Track barriered targets to avoid playing multiple barriers on same card
    private Set<String> barrieredTargets = new HashSet<>();
    private int barrierTurn = 0;

    // V169 UPDATED 2026-07-06 (audit cross-brain-1): per-turn budget of soft excusals for a
    // blocked endangered mover's retreat. Now that the soft block is small enough to beat
    // Pass (see the V169 branch in evaluate()), a retreat whose destination step keeps
    // cancelling must eventually fall back to the V163 hard veto, or the Keder-style
    // re-pick loop returns. Budget resets each turn (blockedResponses is turn-scoped too).
    private static final int V169_SOFT_RETRY_BUDGET = 3;
    private final Map<String, Integer> v169SoftRetryCounts = new HashMap<>();
    private int v169SoftRetryTurn = -1;

    public ActionTextEvaluator() {
        super("ActionText");
    }

    @Override
    public boolean canEvaluate(DecisionContext context) {
        String decisionType = context.getDecisionType();

        // Handle CARD_ACTION_CHOICE and ACTION_CHOICE
        if ("CARD_ACTION_CHOICE".equals(decisionType) || "ACTION_CHOICE".equals(decisionType)) {
            return true;
        }

        // Also handle MULTIPLE_CHOICE for capacity slot decisions, Epic Event choices,
        // and the critical "not activated Force" confirmation
        if ("MULTIPLE_CHOICE".equals(decisionType)) {
            String decisionText = context.getDecisionText();
            if (decisionText != null) {
                String dtLower = decisionText.toLowerCase();
                if (dtLower.contains("capacity slot") || dtLower.contains("choose an option")
                    || dtLower.contains("not activated force") || dtLower.contains("have not activated")) {
                    return true;
                }
                // V79 (Steve, 2026-05-15): Death Star hyperspace destination decisions.
                // After Rando picks "Move using hyperspeed" the engine asks:
                //   1. "Choose parsec to move to " — options are parsec numbers
                //   2. "Choose destination for Death Star at parsec X" — orbit options
                if (dtLower.contains("choose parsec to move to")
                    || (dtLower.contains("choose destination for") && dtLower.contains("parsec"))) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public List<EvaluatedAction> evaluate(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        GameState gameState = context.getGameState();
        SwccgGame game = context.getGame();
        List<String> actionIds = context.getActionIds();
        List<String> actionTexts = context.getActionTexts();
        List<String> cardIds = context.getCardIds();
        Set<String> blocked = context.getBlockedResponses();

        for (int i = 0; i < actionIds.size(); i++) {
            String actionId = actionIds.get(i);
            String actionText = i < actionTexts.size() ? actionTexts.get(i) : "";
            String cardId = i < cardIds.size() ? cardIds.get(i) : null;
            String textLower = actionText.toLowerCase();

            EvaluatedAction action = new EvaluatedAction(actionId, ActionType.UNKNOWN, 0.0f, actionText);

            // ═══════════════════════════════════════════════════════════
            // ═══ REGION: SVC-SAFETY — loop-prevention veto trio (reorg 2026-07-06) ═══
            // Owns: blocked-response handling: V163 hard veto -100000; V167 phase-fundamental soft -200
            // (Activate Force is NEVER hard-vetoed); V169 endangered-mover soft -250 with V169_SOFT_RETRY_BUDGET=3
            // per turn, then falls back to the V163 hard veto. Magnitudes FROZEN (plan: do not retune before T4).
            // Absorbs (dead, commented below/nearby — revert path, do not delete): V169 old single-shot -400.
            // Cross-refs: SVC-SAFETY peers DecisionSafety (V148 all-bad pass), MOVE (V169-retreat +600 pairing). See resources/RANDO_REORG_PLAN_2026-07-02.md §3 + Rando_Section_Manifest_2026-07-06.xlsx.
            // ═══════════════════════════════════════════════════════════
            // Check if this response is blocked (loop prevention)
            // V163 (2026-06): HARD VETO, not a nudge. The old additive -200 got
            // swamped by later positive rules (e.g. V35.4 spy-flee +250), so a
            // known loop-causing action stayed on top and the game looped forever:
            // Chosen One "Keder The Black" move-loop, turn 10 — Move-using-landspeed
            // scored +250 (V35.4 "flee undercover spy"), its ONLY destination was
            // V41-wrong-direction-blocked, so the target step cancelled, the action
            // re-offered, and -200 < +250 meant Move kept winning. 1000+ iterations.
            // Loop-breakers must DOMINATE (master discipline §2A). Follow the V87
            // hard-block pattern: huge negative + skip all further scoring so no
            // later rule can stack the action back above Pass.
            if (blocked.contains(actionId) || blocked.contains(actionText)) {
                // V167 (Steve, 2026-06): NEVER hard-veto a phase-fundamental action.
                // Live-game regression: "Activate Force" landed in the blocked set (a transient
                // activate-flow cancel-loop) and V163's -100000 hard veto then killed it
                // permanently — Rando passed every Activate phase from turn ~6 on, never got
                // Force in its pile, and stalled (could not deploy or drain) despite a full
                // Reserve Deck. Activating Force is mandatory to play at all; a loop-breaker must
                // never make it impossible. Soft-discourage instead (-200, the pre-V163 value the
                // high-scored Activate action still beats) so the loop is nudged but the bot can
                // still activate. Tactical targets (move/deploy/battle) keep the hard veto.
                String v167tl = actionText != null ? actionText.toLowerCase(java.util.Locale.ROOT) : "";
                // V169 (Steve, 2026-06): a blocked MOVE whose mover is ENDANGERED (outpowered
                // at its current site) must stay attemptable — retreat is how it survives.
                // Replay lk6xgsokjcwrwxuu: Asajj's 'Move using landspeed' was cancel-blocked
                // (V41 had blocked every safe destination), the hard veto made retreat
                // impossible, and she was beaten 6v27 next turn. Non-endangered movers keep
                // the veto (Keder himself wasn't endangered).
                // V169 UPDATED 2026-07-06 (audit cross-brain-1): the old -400 could NEVER let
                // the retreat retry: MoveEvaluator applied a second copy at double strength
                // (ctor -400 PLUS addReasoning -400 = -800, both add per EvaluatedAction) on
                // the same actionId, so the merged score was ~-1050 vs Pass +5. This branch is
                // now the SINGLE owner of the soft block (MoveEvaluator's copy commented out),
                // resized -400 -> -250 so a badly-outmatched retreat can actually win:
                // -250 (here) + V35.4 enemy-presence +150 + MoveEvaluator RETREAT tier +150
                // = +50 > Pass (~5-8). Guarded by V169_SOFT_RETRY_BUDGET per turn: if the
                // destination step keeps cancelling (no safe destination), the V163 hard veto
                // resumes instead of re-looping.
                boolean v169EndangeredMover = false;
                if ((v167tl.contains("move using") || v167tl.contains("transport") || v167tl.contains("relocate"))
                        && cardId != null && context.getGameState() != null && context.getGame() != null
                        && context.getPlayerId() != null) {
                    try {
                        com.gempukku.swccgo.game.state.GameState v169Gs = context.getGameState();
                        String v169Pid = context.getPlayerId();
                        PhysicalCard v169Mover = v169Gs.findCardById(Integer.parseInt(cardId));
                        PhysicalCard v169At = v169Mover != null ? v169Mover.getAtLocation() : null;
                        if (v169At != null) {
                            String v169Opp = v169Gs.getOpponent(v169Pid);
                            float v169Our = context.getGame().getModifiersQuerying()
                                .getTotalPowerAtLocation(v169Gs, v169At, v169Pid, false, false);
                            float v169Their = context.getGame().getModifiersQuerying()
                                .getTotalPowerAtLocation(v169Gs, v169At, v169Opp, false, false);
                            v169EndangeredMover = v169Their > v169Our;
                        }
                    } catch (Exception ignore) { }
                }
                if (v167tl.contains("activate force")) {
                    action.addReasoning("BLOCKED (loop prevention) — soft (V167: Activate Force never hard-vetoed)", -200.0f);
                    logger.warn("V167: soft-block (not hard veto) on essential action: {}", actionText);
                } else if (v169EndangeredMover) {
                    // V169 UPDATED 2026-07-06 (audit cross-brain-1): single owner, -250, retry budget.
                    // action.addReasoning("BLOCKED (loop prevention) — soft (V169: endangered mover, retreat must stay possible)", -400.0f);
                    // logger.warn("V169: soft-block (not hard veto) on endangered mover's action: {}", actionText);
                    if (v169SoftRetryTurn != context.getTurnNumber()) {
                        v169SoftRetryCounts.clear();
                        v169SoftRetryTurn = context.getTurnNumber();
                    }
                    String v169Key = (actionText != null && !actionText.isEmpty()) ? actionText : actionId;
                    int v169Tries = v169SoftRetryCounts.merge(v169Key, 1, Integer::sum);
                    if (v169Tries <= V169_SOFT_RETRY_BUDGET) {
                        action.addReasoning("BLOCKED (loop prevention) — soft (V169: endangered mover, retreat must stay possible)", -250.0f);
                        logger.warn("V169: soft-block (not hard veto) on endangered mover's action: {} (excusal {}/{} this turn)",
                            actionText, v169Tries, V169_SOFT_RETRY_BUDGET);
                    } else {
                        action.addReasoning("BLOCKED (loop prevention) — hard veto (V169 retry budget exhausted: no safe destination materialized)", -100000.0f);
                        logger.warn("V169: retry budget exhausted for '{}' this turn, reverting to V163 hard veto", actionText);
                        actions.add(action);
                        continue;
                    }
                } else {
                    action.addReasoning("BLOCKED (loop prevention) — hard veto", -100000.0f);
                    logger.warn("Blocked action (V163 hard veto): {}", actionText);
                    actions.add(action);
                    continue;
                }
            }

            // ═══════════════════════════════════════════════════════════
            // ═══ REGION: ACTIVATE — activation guards (reorg 2026-07-06) ═══
            // Owns: whether to activate: V168 always-activate +5000 vs V61c destiny-buffer -6000 (reserve<=3 AND
            // battle plausible); with V38.3's +500 confirm (evaluateActivateForce below) this triangle is ONE boundary.
            // Shared predicate DecisionContext.isBattlePlausibleThisTurn() — THREE sites must agree: this block,
            // the ForceActivationEvaluator keep-3 cap, and the V38.3 reserve<=3 carve-out.
            // Absorbs (dead, commented below/nearby — revert path, do not delete): V61c pre-2026-07-06
            // always-on buffer branch.
            // Cross-refs: ACTIVATE (ForceActivationEvaluator owns how MUCH), PULL-ENGINE (V97 pulls fire BEFORE
            // activation and must outrank V168's +5000). See resources/RANDO_REORG_PLAN_2026-07-02.md §3 + Rando_Section_Manifest_2026-07-06.xlsx.
            // ═══════════════════════════════════════════════════════════
            // === V168 (Steve, 2026-06): ALWAYS ACTIVATE FORCE — never pass activation ===
            // Steve: "Rando should always activate force and not pass activating." Activating
            // Force is the bot's entire economy (it pays for deploys, drains, battles); passing
            // it stalls the bot. Guaranteed dominating bonus so "Activate Force" always beats
            // Pass (and the V167 soft loop-block) whenever it is offered. Once the player's max
            // Force is already activated, "Activate Force" is no longer offered, so the bot still
            // passes legitimately at the end of the Activate phase.
            if (textLower.contains("activate force")) {
                // V61c DESTINY BUFFER exception (Steve, 2026-06-29): if the Reserve Deck is
                // already <= 3, PASS activation instead of the usual V168 always-activate.
                // Activating moves Reserve -> Force Pile, so activating the last <=3 drains the
                // destiny buffer that battle/weapon destiny draws need this turn (the engine
                // forces >=1 per activation, so capping the amount alone erodes 3->2->1->0 over
                // turns). Steve: "If Rando intends to battle that turn, he needs to save 3." Score
                // below Pass (~5-8) so the action-choice lands on Pass. Pairs with the V38.3
                // reserve<=3 carve-out below (else the "you have not activated Force" confirm
                // would bounce Rando straight back into activating).
                // V61c UPDATED 2026-07-06: battle-intent bypass — the buffer protection now applies
                // ONLY on turns a battle is plausible (any contested location, per the shared
                // predicate DecisionContext.isBattlePlausibleThisTurn(), same scan V61b uses).
                // Zero contested locations => deploy-and-end turn => normal V168 always-activate.
                // SAME predicate gates the ForceActivationEvaluator keep-3 cap + the V38.3
                // carve-out below so all three sites agree.
                int v61cReserve = context.getReserveDeckSize();
                // V61c pre-2026-07-06 (always-on buffer):
                // if (v61cReserve <= 3) {
                boolean v61cBattlePlausible = context.isBattlePlausibleThisTurn();
                if (v61cReserve <= 3 && !v61cBattlePlausible) {
                    logger.warn("V61c BATTLE-INTENT: no contested location — activating full");
                }
                if (v61cReserve <= 3 && v61cBattlePlausible) {
                    action.addReasoning(
                        "V61c DESTINY BUFFER: reserve <= 3 — pass activation, keep 3 for destiny", -6000.0f);
                    logger.warn("V61c DESTINY BUFFER: reserve={} <= 3 — passing activation (no V168 +5000) on '{}'",
                        v61cReserve, actionText);
                } else {
                    action.addReasoning(
                        "V168 ALWAYS ACTIVATE: never pass Force activation while Force can be activated", 5000.0f);
                    logger.warn("V168 ALWAYS ACTIVATE: +5000 on '{}'", actionText);
                }
            }

            // === V116 (Steve, 2026-05-22): GUARANTEED +100 FLOOR FOR RESERVE-DECK PULLS ===
            // Per Steve: "The game gives players an option to deploy anything from
            // reserve deck should be +100 at least. In the case of the objective,
            // it says it's an option to deploy from reserve. Same with some of the
            // effects. Not sure why they aren't firing when they are lit up green
            // as options to deploy."
            //
            // Safety net: any action whose text indicates a Reserve Deck deploy or
            // download gets an unconditional +100 baseline AT THE TOP of evaluation,
            // before any other rule runs. V60/V67ai/V82 still apply additional
            // positive scoring on top — but even if those handlers fail to reach
            // this action for any reason, the floor guarantees the AI sees it as a
            // positive option. Mirrored in chosenone.
            // === V177 (Steve, 2026-06): DECK ORACLE DEAD-SEARCH GATE ===
            // Replay j6tf75kwbfh83lxo: Rando fired Luke Skywalker, The Last Jedi's
            // "take Force Projection into hand from Reserve Deck" FOUR-PLUS times in one
            // game — Force Projection was never in the deck. Each attempt wasted the
            // action, revealed the Reserve Deck to Steve, and reshuffled. Steve: "A real
            // player knows all the cards in his deck and would never search his reserve
            // deck for a card he knows is not in it."
            // The knowledge already exists — DeckOracle catalogs the full deck by zone at
            // game start — it just was never consulted for game-text searches. Before ANY
            // reserve-deck search/pull/take action: parse the SOURCE card's game text for
            // its pull targets (the V95 parser) and ask the Oracle whether ANY target is
            // still in the Reserve Deck. None -> hard block (-2000) and skip all further
            // scoring so V116's +100 floor can't resurrect it. Parse-empty -> no block
            // (unknown text shapes stay allowed). Multi-target pulls stay alive if ANY
            // named target remains. Category targets ("a blaster", "[Cloud City]
            // corridor") resolve via hasTargetInZone's stripped/type-word matching; a
            // pure-category miss is logged loudly so false blocks are visible.
            if (textLower.contains("from reserve deck")
                    || textLower.contains("[download]")) {
                try {
                    com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle v177Oracle =
                        context.getDeckOracle();
                    com.gempukku.swccgo.game.state.GameState v177Gs = context.getGameState();
                    if (v177Oracle != null && v177Oracle.isAnalyzed()
                            && v177Gs != null && cardId != null) {
                        PhysicalCard v177Src = v177Gs.findCardById(Integer.parseInt(cardId));
                        // BATCH1-CORR (2026-07-13, Codex m00229): side-aware owner — locations
                        // keep pull text in per-side getters, getGameText() alone is blind.
                        String v177Gt = (v177Src != null && v177Src.getBlueprint() != null)
                            ? com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle
                                .getSourceCardFullGameText(v177Src.getBlueprint(), context.getSide()) : null;
                        java.util.List<String> v177Targets =
                            com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle
                                .parseSourceCardPullTargets(v177Gt);
                        if (!v177Targets.isEmpty()) {
                            // V177a: classify each target — ALIVE (in reserve), DEAD (clean
                            // title-like string with no match), or JUNK (parser garbage:
                            // long phrases / digits — e.g. "3 force to take one effect of
                            // any kind"). Block ONLY when no ALIVE, at least one DEAD, and
                            // no JUNK — a parse we can't trust must never block a live pull.
                            // Loose word-rescue: any >=6-char word matching a reserve title
                            // keeps targets like "lightsaber on rey" alive when sabers
                            // remain (the strict matcher's last-word fallback missed it).
                            boolean v177Alive = false, v177AnyDead = false, v177AnyJunk = false;
                            for (String v177T : v177Targets) {
                                if (v177Oracle.hasTargetInZone(
                                        com.gempukku.swccgo.common.Zone.RESERVE_DECK, v177T)) {
                                    v177Alive = true; break;
                                }
                                boolean v177WordHit = false;
                                for (String v177W : v177T.split("[^a-zA-Z']+")) {
                                    // V177 ADJUSTED 2026-07-10 (Rey replay rbujmoc90br3uu4c, incident 5):
                                    // skip GENERIC TYPE WORDS in the word-rescue — "lightsaber" from
                                    // "leia's lightsaber" matched Anakin's Lightsaber in Reserve and
                                    // revived a pull whose real target was in the Force Pile. A type
                                    // word alone is not evidence the NAMED card is pullable.
                                    if (com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle
                                            .mapTypeWordToCategory(v177W) != null) continue;
                                    if (v177W.length() >= 6 && v177Oracle.hasTargetInZone(
                                            com.gempukku.swccgo.common.Zone.RESERVE_DECK, v177W)) {
                                        v177WordHit = true; break;
                                    }
                                }
                                if (v177WordHit) { v177Alive = true; break; }
                                if (v177T.length() > 25 || v177T.matches(".*\\d.*")) v177AnyJunk = true;
                                else v177AnyDead = true;
                            }
                            if (!v177Alive && v177AnyDead && !v177AnyJunk) {
                                // V177 CATEGORY RESCUE (adjusted 2026-07-02, TDIGWATT replay
                                // 7co2xviwqo5q3zac): the raw title matcher above can't match
                                // type-phrases like "interior cloud city site" against titles
                                // ("Cloud City: Dining Room"), so V177 declared I'm Sorry's
                                // site download DEAD every turn (-2000 x19) while V67h's
                                // validatePullFromSourceCard (V82.1 category / V82.2 predicate
                                // fallbacks) said WILL_SUCCEED in the SAME evaluations.
                                // Detection-path mismatch: the dumber matcher ran first and
                                // won. Before blocking, consult the validated path V67h
                                // trusts; WILL_SUCCEED -> no block, action scores naturally.
                                // Any other outcome -> the -2000 block stands unchanged.
                                com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle.PullValidation v177Val =
                                    v177Oracle.validatePullFromSourceCard(
                                        com.gempukku.swccgo.common.Zone.RESERVE_DECK, v177Gt);
                                if (v177Val.outcome ==
                                        com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle.PullOutcome.WILL_SUCCEED) {
                                    logger.warn("V177 CATEGORY RESCUE: '{}' targets {} — {} — not blocking",
                                        actionText, v177Targets, v177Val.reason);
                                } else {
                                    action.addReasoning("V177 DEAD SEARCH: none of " + v177Targets
                                        + " remain in Reserve Deck — a real player never searches for what he knows isn't there",
                                        -2000.0f);
                                    logger.warn("V177 DEAD SEARCH blocked: '{}' targets {} (source '{}') — nothing in Reserve",
                                        actionText, v177Targets,
                                        v177Src != null ? v177Src.getTitle() : "?");
                                    actions.add(action);
                                    continue;
                                }
                            } else if (!v177Alive && v177AnyJunk) {
                                logger.info("V177 PARSE-JUNK pass-through: '{}' targets {} — unparseable, not blocking",
                                    actionText, v177Targets);
                            }
                        }

                        // === V183 (Steve, 2026-06): DECK-TITLE RETOOL ===
                        // The position parser only reads "[download] X" / "X from Reserve
                        // Deck"; Fall Of The Legend names its target as "Search your Reserve
                        // Deck, take one Weather Vane into hand" — no "from Reserve Deck"
                        // clause, so the parser returns NOTHING and the search runs even when
                        // Weather Vane is in hand, failing and revealing the Reserve. Resolve
                        // the target by scanning the source text for our OWN deck titles, then
                        // judge by the real card's ZONE: if every named target is out of the
                        // Reserve (in hand / play / lost), the search is dead — block it.
                        // GATE: only when the position parser found NOTHING (v177Targets empty).
                        // If the parser DID produce tokens, the pull has a parseable (often
                        // MULTI-target) clause we must not second-guess — e.g. Pray I Don't
                        // Alter It Any Further pulls "Bespin system, Bespin: Cloud City, Dark
                        // Deal OR Cloud City Occupation"; a title-scan that matches only the
                        // out-of-Reserve "Bespin" would falsely block a search the other three
                        // targets keep alive. Parser-silent is the only safe place for this.
                        if (v177Targets.isEmpty() && v177Src != null && v177Src.getBlueprint() != null) {
                            String v183Text = com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle
                                .getSourceCardFullGameText(v177Src.getBlueprint(), context.getSide());
                            String v183SrcBp = v177Src.getBlueprintId(true);
                            java.util.List<com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle.DeckCard>
                                v183Named = v177Oracle.namedDeckCardsInText(v183Text, v183SrcBp);
                            if (!v183Named.isEmpty()) {
                                boolean v183AnyInReserve = false;
                                java.util.Set<String> v183Titles = new java.util.LinkedHashSet<>();
                                for (com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle.DeckCard v183Dc : v183Named) {
                                    v183Titles.add(v183Dc.getTitle());
                                    if (com.gempukku.swccgo.common.Zone.RESERVE_DECK.equals(v183Dc.getCurrentZone())) {
                                        v183AnyInReserve = true;
                                    }
                                }
                                if (!v183AnyInReserve) {
                                    action.addReasoning("V183 DEAD SEARCH (title+zone): " + v183Titles
                                        + " named in source text but none is in Reserve — already in hand/play/lost",
                                        -2000.0f);
                                    logger.warn("V183 DEAD SEARCH blocked: '{}' named {} (source '{}') — none in Reserve",
                                        actionText, v183Titles, v177Src.getTitle());
                                    actions.add(action);
                                    continue;
                                }
                            }
                        }
                    }
                } catch (Exception v177E) {
                    logger.debug("V177 dead-search check error: {}", v177E.getMessage());
                }
                // V116 +100 floor ABSORBED by V192 pull scorer 2026-07-06 (T4.2 merge):
                // the scorer's +150 deploy-grade base covers the floor, and it is now
                // VETO-GATED (the old unconditional floor could resurrect pulls the V60
                // guards were trying to kill). V177/V183 above are untouched — their
                // continue still runs before any positive tier.
                // Commented out per feedback_comment_out_old_rules:
                // action.addReasoning(
                //     "V116 RESERVE OPTION: deploy-from-reserve always at least +100", 100.0f);
                // logger.info("V116 RESERVE FLOOR: '{}' → +100 baseline", actionText);
            }

            // === V184 (Steve, 2026-06): FIRE "WHEN DEPLOYED" FREE-VALUE TRIGGERS ===
            // When a card deploys with an optional "when deployed, may ..." trigger granting
            // free value, Rando should fire it, not pass. Replay xc19a289odmogph5: Han Solo,
            // Optimistic General — "When deployed, may reveal the top two cards of your
            // Reserve Deck; take one into hand" — was offered as 'Reveal top two cards of
            // Reserve Deck' and scored NOTHING, so Pass (6.0) won and the free card was
            // thrown away. These aren't "from Reserve Deck" pulls ("OF Reserve Deck" /
            // "retrieve Force"), so V116/V60/V97 miss them. Score them above Pass here,
            // GATED on the value actually existing (don't reveal an empty Reserve, don't
            // retrieve from an empty Lost Pile) so a dead trigger is never fired. Free,
            // optional, upside-only — take it whenever it's live.
            if (gameState != null && context.getPlayerId() != null) {
                try {
                    String v184Pid = context.getPlayerId();
                    boolean v184Reveal = (textLower.contains("reveal") || textLower.contains("look at"))
                            && (textLower.contains("reserve") || textLower.contains("top two")
                                || textLower.contains("top card") || textLower.contains("top of"));
                    boolean v184Retrieve = textLower.contains("retrieve") && textLower.contains("force")
                            && !textLower.contains("use ");   // skip cost-bearing retrieves
                    boolean v184Fire = false; String v184Why = null;
                    if (v184Reveal) {
                        boolean v184HasReserve = false;
                        try { v184HasReserve = gameState.getReserveDeckSize(v184Pid) > 0; } catch (Exception ignore) { }
                        if (v184HasReserve) { v184Fire = true; v184Why = "reveal/look at Reserve, take a card"; }
                    } else if (v184Retrieve) {
                        boolean v184HasLost = false;
                        try {
                            java.util.List<PhysicalCard> v184Lp = gameState.getLostPile(v184Pid);
                            v184HasLost = (v184Lp != null && !v184Lp.isEmpty());
                        } catch (Exception ignore) { }
                        if (v184HasLost) { v184Fire = true; v184Why = "retrieve Force from Lost Pile"; }
                    }
                    if (v184Fire) {
                        action.addReasoning("V184 WHEN-DEPLOYED TRIGGER: free value (" + v184Why
                            + ") — fire it, don't pass", 300.0f);
                        logger.warn("V184 WHEN-DEPLOYED TRIGGER: '{}' → +300 ({})", actionText, v184Why);
                    }
                } catch (Exception e) { logger.debug("V184 error: {}", e.getMessage()); }
            }

            // === V87 (Steve, 2026-05-16): HARD-BLOCK pilot/passenger capacity slot swaps ===
            // Replay tem28wtufcy7d08j: Sil Unch deployed aboard Blockade Flagship as
            // pilot, then Rando got stuck in a 40+ iteration pilot↔passenger swap loop.
            // DecisionTracker didn't catch it because the wrapping decision text varies
            // ("Optional responses" vs "Use 2 Force - Optional responses"), breaking
            // the key-match for loop detection.
            //
            // These capacity-slot swaps gain nothing for the AI — once a pilot is
            // placed, swapping pilot↔passenger doesn't change combat/movement value.
            // Hard-block both directions outright.
            if (textLower.contains("move to passenger capacity slot")
                    || textLower.contains("move to pilot capacity slot")) {
                action.addReasoning(
                    "V87 NO SWAP: pilot↔passenger capacity slot rearrangement is pointless — hard block",
                    -3000.0f);
                logger.warn("V87 NO SWAP blocking: '{}' → -3000", actionText);
                actions.add(action);
                continue;
            }

            // === V95 (Steve, 2026-05-20): SAVE DEAD INTERRUPTS WHEN RESERVES >= 15 ===
            // If an interrupt's pull/upload targets are ALL ALREADY on the table,
            // the primary effect is dead. AND if reserve force (force pile + used
            // pile + reserve deck) >= 15, save the card in hand as future
            // force-loss fodder. Burning it for the no-op effect is double-loss.
            //
            // Example: My Sister Has It uploads Chief Chirpa's Hut or Guest
            // Quarters. With both on the table, the upload is moot. Keep it.
            //
            // Only fires when source card is an INTERRUPT (per Steve's rule
            // scope — Effects, Epic Events, Interrupts, Objectives, but this
            // V95 check focuses specifically on interrupts since those are the
            // ones typically lost as fodder).
            // V95 STANDALONE BLOCK ABSORBED by V192 pull scorer 2026-07-06 (T4.2 merge):
            // moved into the PULL-ENGINE veto chain as a hardBlock (see the V192 region) so the
            // activate-window base (+5500) can NEVER outvote it — as an additive here, the pull
            // pile netted the dead pull to +100 and it FIRED (boundary row 5). Logic verbatim
            // in the new location. Commented out per feedback_comment_out_old_rules:
            // if (cardId != null && context.getGameState() != null) {
                // try {
                    // com.gempukku.swccgo.game.state.GameState v95Gs = context.getGameState();
                    // PhysicalCard v95Src = v95Gs.findCardById(Integer.parseInt(cardId));
                    // if (v95Src != null && v95Src.getBlueprint() != null
                            // && v95Src.getBlueprint().getCardCategory()
                                // == com.gempukku.swccgo.common.CardCategory.INTERRUPT) {
                        // String v95Gt = v95Src.getBlueprint().getGameText();
                        // if (v95Gt != null) {
                            // java.util.List<String> v95Targets =
                                // com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle
                                    // .parseSourceCardPullTargets(v95Gt);
                            // if (!v95Targets.isEmpty()) {
                                // java.util.Collection<PhysicalCard> v95Table = v95Gs.getAllPermanentCards();
                                // boolean allOnTable = true;
                                // for (String t : v95Targets) {
                                    // String tl = t.toLowerCase(java.util.Locale.ROOT);
                                    // boolean found = false;
                                    // for (PhysicalCard tc : v95Table) {
                                        // if (tc == null || tc.getTitle() == null) continue;
                                        // if (tc.getTitle().toLowerCase(java.util.Locale.ROOT).contains(tl)) {
                                            // found = true; break;
                                        // }
                                    // }
                                    // if (!found) { allOnTable = false; break; }
                                // }
                                // if (allOnTable) {
                                    // String v95Pid = context.getPlayerId();
                                    // int reserveForce = v95Gs.getForcePileSize(v95Pid)
                                        // + (v95Gs.getUsedPile(v95Pid) != null ? v95Gs.getUsedPile(v95Pid).size() : 0)
                                        // + v95Gs.getReserveDeckSize(v95Pid);
                                    // if (reserveForce >= 15) {
                                        // action.addReasoning(String.format(
                                            // "V95 DEAD INTERRUPT SAVE: '%s' pull targets %s all on table, reserves=%d — save for force-loss fodder",
                                            // v95Src.getTitle(), v95Targets, reserveForce), -2000.0f);
                                        // logger.warn("V95 DEAD INTERRUPT: blocking {} (targets on table, reserves {})",
                                            // v95Src.getTitle(), reserveForce);
                                    // }
                                // }
                            // }
                        // }
                    // }
                // } catch (NumberFormatException nfe) {
                    // // not numeric cardId
                // } catch (Exception e) {
                    // logger.debug("V95 error: {}", e.getMessage());
                // }
            // }

            // === V134 (Steve, 2026-05-25): ODIN NESLOOR 5-FORCE FLOOR (MOVE phase) ===
            //
            // Steve's standing rule: "must have 5 force in force pile to play
            // Odin Nesloor during move phase." Odin Nesloor & First Aid lets us
            // move multiple characters off one site to another, useful for
            // blocking opponent's force drains next turn — but it costs force
            // and is wasted when force pile is too low to actually drain at
            // the destination next turn.
            //
            // TODO (Steve): fuller proper-use logic later. Should only fire
            // when (a) we plan to block opponent force drain next turn AND
            // (b) we can battle opponent at destination site. The 5-force
            // floor is the simple hotfix; the strategic condition is a
            // future V-tag once we have the gameplay-state predicate.
            //
            // Detection: source card persona is ODIN_NESLOOR. Using a title
            // substring as fallback because the Persona constant may not be
            // registered in the engine enum yet — verify and migrate to
            // Filters.persona(Persona.ODIN_NESLOOR) when available.
            if (cardId != null && gameState != null
                    && context.getPhase() == Phase.MOVE) {
                try {
                    PhysicalCard v134Src = gameState.findCardById(Integer.parseInt(cardId));
                    if (v134Src != null && v134Src.getTitle() != null) {
                        String v134Lower = v134Src.getTitle().toLowerCase(java.util.Locale.ROOT);
                        // BUGFIX 2026-05-28: require the ACTION to actually be a play of
                        // Odin Nesloor (text mentions the card or its transport mechanic),
                        // not a generic move action that happens to carry its cardId.
                        // Same misfire class as the V142/Activate-Force bug.
                        boolean v134ActionMatches = textLower.contains("odin nesloor")
                            || textLower.contains("transport") || textLower.contains("relocate");
                        if (v134Lower.contains("odin nesloor") && v134ActionMatches) {
                            int v134ForcePile = context.getForcePileSize();
                            if (v134ForcePile < 5) {
                                // V134 UPDATED 2026-07-06 T4.1: -9999 raised to the MOVE-ladder veto
                                // class -100000. Its "transport" text co-sums with MoveEvaluator-scored
                                // actions (ME keyword "Transport"), so it must stay veto-class across
                                // the new R2-R4 bands (an R4 transit +20000 would have outvoted -9999).
                                // OLD: action.addReasoning(
                                //     "V134 ODIN NESLOOR FLOOR: only " + v134ForcePile
                                //         + " force in pile (need 5+) — hold the interrupt",
                                //     -9999.0f);
                                action.addReasoning(
                                    "V134 ODIN NESLOOR FLOOR: only " + v134ForcePile
                                        + " force in pile (need 5+) — hold the interrupt (LADDER VETO)",
                                    -100000.0f);
                                logger.warn("V134 ODIN NESLOOR BLOCK: forcePile={} < 5 — block in MOVE phase (-100000)",
                                    v134ForcePile);
                            }
                        }
                    }
                } catch (NumberFormatException nfe) { /* */ }
                catch (Exception e) { logger.debug("V134 error: {}", e.getMessage()); }
            }

            // === V141 (Steve, 2026-05-26): TRANSPORT INTERRUPT 4-FORCE FLOOR ===
            //
            // Elis Helrot (dark), Nabrun Leids (light), Odin Nesloor (light, move-phase
            // variant — already covered by V134) all share the "draw destiny, use that
            // much Force to transport, or place Interrupt in Lost Pile" mechanic.
            // If we play the interrupt without enough force to cover a destiny draw,
            // the interrupt is WASTED (goes to Lost Pile, no transport happens).
            //
            // Steve's rule 2026-05-26: "needs probably 4+ force to move characters
            // with that card." Below 4 force in pile, hard-block the play.
            //
            // Detection: source card title is Elis Helrot OR Nabrun Leids (universal
            // transport — Odin Nesloor handled separately by V134).
            // Generic-text fallback: action text contains "'transport'" AND "destiny"
            // pattern (captures both cards plus any future "transport-style" interrupt).
            if (cardId != null && context.getGameState() != null) {
                try {
                    com.gempukku.swccgo.game.state.GameState v141Gs = context.getGameState();
                    PhysicalCard v141Src = v141Gs.findCardById(Integer.parseInt(cardId));
                    if (v141Src != null && v141Src.getTitle() != null) {
                        String v141TitleLower = v141Src.getTitle().toLowerCase(java.util.Locale.ROOT);
                        boolean v141IsTransport =
                            v141TitleLower.contains("elis helrot")
                            || v141TitleLower.contains("nabrun leids");
                        // Generic fallback: check source game text for the transport
                        // mechanic (any card with "draw destiny" + "transport" + Lost Pile fallback)
                        if (!v141IsTransport && v141Src.getBlueprint() != null) {
                            String gt = v141Src.getBlueprint().getGameText();
                            if (gt != null) {
                                String gtLower = gt.toLowerCase(java.util.Locale.ROOT);
                                if (gtLower.contains("'transport'")
                                        && gtLower.contains("draw destiny")
                                        && gtLower.contains("place interrupt in lost pile")) {
                                    v141IsTransport = true;
                                }
                            }
                        }
                        // BUGFIX 2026-05-26: V141 was firing on "Activate Force" actions
                        // when the cardId happened to be Elis Helrot/Nabrun Leids in hand.
                        // Action text "Activate Force" has nothing to do with playing the
                        // transport interrupt — but V141 only checked source card title.
                        // Result: -1500 penalty on Activate Force → Rando skipped activate
                        // phase entirely.
                        // Fix: also require the action text to mention the card or its
                        // transport mechanic (so we only block ACTUAL plays of the card).
                        boolean v141ActionMatches = false;
                        if (v141IsTransport) {
                            v141ActionMatches = textLower.contains(v141TitleLower)
                                || textLower.contains("transport")
                                || textLower.contains("relocate");
                        }
                        if (v141IsTransport && v141ActionMatches) {
                            int v141ForcePile = context.getForcePileSize();
                            int v141Reserve = context.getReserveDeckSize();
                            // Per Steve 2026-05-26: must have at least 1 card in
                            // reserve to draw destiny. Empty reserve = can't draw =
                            // transport fails. Combine with the 4-force floor.
                            if (v141ForcePile < 4 || v141Reserve < 1) {
                                String v141Why = v141ForcePile < 4
                                    ? "only " + v141ForcePile + " force in pile (need 4+ to cover destiny draw)"
                                    : "reserve deck empty — cannot draw destiny";
                                action.addReasoning(
                                    "V141 TRANSPORT INTERRUPT BLOCK: " + v141Why + " — hold the interrupt",
                                    -2000.0f);
                                logger.warn("V141 TRANSPORT BLOCK: {} forcePile={} reserve={} → -2000",
                                    v141Src.getTitle(), v141ForcePile, v141Reserve);
                            }
                        }
                    }
                } catch (NumberFormatException nfe) { /* not numeric */ }
                catch (Exception e) { logger.debug("V141 error: {}", e.getMessage()); }
            }

            // === V142 (Steve, 2026-05-26): WMAOP MODE-SPECIFIC GATING ===
            //
            // We Must Accelerate Our Plans (WMAOP) has three modes:
            //   1. Use 3 Force to take one Effect of any kind from Reserve into hand
            //   2. Deploy a Blockade Flagship site from Reserve Deck
            //   3. Take one Interrupt with the word 'Podracer(s)' from Reserve into hand
            //
            // V29.7 used to hardcode "Blockade Flagship site is the ONLY good use."
            // That was removed earlier this session for over-restricting other modes.
            // Now V142 replaces it with deck-aware preconditions:
            //
            //   - Deploy phase only (avoid using outside deploy phase per Steve 2026-05-26)
            //   - Mode 2 (deploy BFS): only if Blockade Flagship site is NOT yet on
            //     table AND a BFS is in our reserve deck
            //   - Mode 3 (Podracer interrupt): only if a Podracer interrupt is in
            //     reserve (DeckOracle check)
            //   - Mode 1 (Effect pull): only if at least one Effect is in reserve
            //     (DeckOracle check)
            //
            // If no precondition matches → hard block. We don't fire WMAOP just
            // because we can; we fire when it actually delivers value.
            if (cardId != null && context.getGameState() != null) {
                try {
                    com.gempukku.swccgo.game.state.GameState v142Gs = context.getGameState();
                    PhysicalCard v142Src = v142Gs.findCardById(Integer.parseInt(cardId));
                    if (v142Src != null && v142Src.getTitle() != null
                            && v142Src.getTitle().toLowerCase(java.util.Locale.ROOT)
                                .contains("accelerate our plans")) {
                        boolean v142Block = false;
                        String v142Reason = null;

                        // Mode detection via action text
                        boolean v142IsLocationMode = textLower.contains("blockade flagship site")
                            || textLower.contains("blockade flagship: ");
                        boolean v142IsEffectMode = textLower.contains("effect of any kind")
                            || (textLower.contains("effect") && textLower.contains("into hand"));
                        boolean v142IsInterruptMode = textLower.contains("podracer")
                            || (textLower.contains("interrupt") && textLower.contains("into hand"));

                        // BUGFIX 2026-05-28: only gate when this action is actually a
                        // WMAOP play. Previously the phase gate fired on ANY action whose
                        // cardId mapped to WMAOP — including the generic "Activate Force"
                        // action (which carried WMAOP's cardId). That blocked force
                        // activation entirely (Rando stopped activating). Require a WMAOP
                        // mode keyword or "accelerate our plans" in the action text.
                        // (Mode-specific blocks below already require their mode flag, so
                        // only the phase gate needed this guard.)
                        boolean v142IsWmaopPlay = v142IsLocationMode || v142IsEffectMode
                            || v142IsInterruptMode
                            || textLower.contains("accelerate our plans");

                        // Phase gate — only for genuine WMAOP plays
                        if (v142IsWmaopPlay && context.getPhase() != Phase.DEPLOY) {
                            v142Block = true;
                            v142Reason = "not deploy phase";
                        }

                        com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle v142Oracle =
                            context.getDeckOracle();

                        if (!v142Block && v142IsLocationMode) {
                            // Block if BFS already on table
                            for (PhysicalCard loc : v142Gs.getTopLocations()) {
                                if (loc == null || loc.getTitle() == null) continue;
                                if (loc.getTitle().toLowerCase(java.util.Locale.ROOT)
                                        .contains("blockade flagship")) {
                                    v142Block = true;
                                    v142Reason = "Blockade Flagship site already on table";
                                    break;
                                }
                            }
                        }

                        if (!v142Block && v142IsEffectMode && v142Oracle != null
                                && v142Oracle.isAnalyzed()) {
                            // Block if no effects in reserve
                            java.util.List<com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle.DeckCard> v142Effects =
                                v142Oracle.getCardsByCategory(
                                    com.gempukku.swccgo.common.CardCategory.EFFECT,
                                    com.gempukku.swccgo.common.Zone.RESERVE_DECK);
                            if (v142Effects == null || v142Effects.isEmpty()) {
                                v142Block = true;
                                v142Reason = "no Effects in reserve for Effect-pull mode";
                            }
                        }

                        if (!v142Block && v142IsInterruptMode && v142Oracle != null
                                && v142Oracle.isAnalyzed()) {
                            // Block if no podracer interrupts in reserve
                            boolean v142HasPodracer = false;
                            java.util.List<com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle.DeckCard> v142Interrupts =
                                v142Oracle.getCardsByCategory(
                                    com.gempukku.swccgo.common.CardCategory.INTERRUPT,
                                    com.gempukku.swccgo.common.Zone.RESERVE_DECK);
                            if (v142Interrupts != null) {
                                for (com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle.DeckCard dc : v142Interrupts) {
                                    if (dc == null) continue;
                                    String dcText = dc.getGameText() != null
                                        ? dc.getGameText().toLowerCase(java.util.Locale.ROOT) : "";
                                    if (dcText.contains("podracer")) {
                                        v142HasPodracer = true;
                                        break;
                                    }
                                }
                            }
                            if (!v142HasPodracer) {
                                v142Block = true;
                                v142Reason = "no Podracer interrupts in reserve";
                            }
                        }

                        if (v142Block) {
                            action.addReasoning(
                                "V142 WMAOP BLOCK: " + v142Reason + " — hold the interrupt",
                                -2000.0f);
                            logger.warn("V142 WMAOP BLOCK: {} ({})",
                                v142Src.getTitle(), v142Reason);
                        }
                    }
                } catch (NumberFormatException nfe) { /* not numeric */ }
                catch (Exception e) { logger.debug("V142 error: {}", e.getMessage()); }
            }

            // === V147 (Steve, 2026-05-28): I AM YOUR FATHER — DON'T SEARCH EMPTY LOST PILE ===
            //
            // I Am Your Father: "Once per turn, may [download] Vader's Lightsaber
            // (or lose 1 Force to deploy it from Lost Pile)."
            //
            // Replay 37orjzqd6feo6igp turn 2: Rando lost 1 Force to deploy Vader's
            // Lightsaber from Lost Pile — but the only card in Lost Pile was Prepared
            // Defenses. Saber was in Reserve. Search failed, 1 Force wasted. He should
            // have used the FREE [download] from Reserve mode (which he did correctly
            // at event 869).
            //
            // V147: if the action is the Lost-Pile deploy mode AND Vader's Lightsaber
            // is NOT actually in our Lost Pile, hard-block it. The Reserve [download]
            // mode is free and preferred anyway.
            if (gameState != null && context.getPlayerId() != null
                    && textLower.contains("from lost pile")
                    && (textLower.contains("vader's lightsaber")
                        || textLower.contains("vader’s lightsaber"))) {
                try {
                    boolean v147SaberInLostPile = false;
                    java.util.List<PhysicalCard> v147Lost = gameState.getLostPile(context.getPlayerId());
                    if (v147Lost != null) {
                        for (PhysicalCard lc : v147Lost) {
                            if (lc == null || lc.getTitle() == null) continue;
                            if (lc.getTitle().toLowerCase(java.util.Locale.ROOT)
                                    .contains("vader's lightsaber")
                                || lc.getTitle().toLowerCase(java.util.Locale.ROOT)
                                    .contains("vader’s lightsaber")) {
                                v147SaberInLostPile = true;
                                break;
                            }
                        }
                    }
                    if (!v147SaberInLostPile) {
                        action.addReasoning(
                            "V147 IAYF: Vader's Lightsaber NOT in Lost Pile — don't waste 1 Force on failed search, use free Reserve download",
                            -2000.0f);
                        logger.warn("V147 IAYF BLOCK: saber not in Lost Pile — failed search would waste 1 Force");
                    }
                } catch (Exception e) { logger.debug("V147 error: {}", e.getMessage()); }
            }

            // === V155 (Steve, 2026-05-28): WELCOME HOME, LORD TYRANUS — SAVE FOR BATTLE ===
            // (Implements the previously-parked "V152" Welcome Home idea.)
            // Welcome Home, Lord Tyranus (Lost Interrupt), 3 modes:
            //   1. (Dooku apprentice) take Petranaki Arena OR The Works into hand from Reserve
            //   2. (Dooku + Sidious on table) cancel Sense
            //   3. ONCE PER GAME: if Darth Tyranus in battle and about to draw battle destiny,
            //      instead use his ABILITY NUMBER — a guaranteed high battle destiny. Premium.
            // Steve: "save this card for battle with Dooku once The Works is in hand or on table.
            // He keeps searching his reserve for The Works after it's already out. Very useful
            // in battle." Screenshot 2026-05-28 (turn 1): Rando fired mode 1 to pull a location
            // while The Works was already on the table — burning a premium battle interrupt on
            // a near no-op.
            // FIX: if the action is the mode-1 location pull AND The Works is already on the
            // table OR in hand, hard-block (-2000) so the card is held for the battle mode.
            // Universal text + title detection — no card-name lists beyond this one card's modes.
            //
            // V155 GATE FIX (Steve 2026-05-29, after replay ss2jc7): the original gate also
            // required "the works"/"petranaki" in the action text, but the play-action text is
            // actually "Take location into hand from Reserve Deck" (generic — names are in the
            // card's game text, not the action text). That made V155 fire 0× in the replay
            // even though The Works was on the table. Gate now keys on:
            //   (a) action is a reserve pull-into-hand ("into hand from reserve")
            //   (b) source card title contains "welcome home" (checked just below)
            // The source-card check is the specific filter — no need to match target names too.
            if (cardId != null && gameState != null
                    && textLower.contains("into hand from reserve")) {
                try {
                    PhysicalCard v155Src = gameState.findCardById(Integer.parseInt(cardId));
                    if (v155Src != null && v155Src.getTitle() != null
                            && v155Src.getTitle().toLowerCase(java.util.Locale.ROOT).contains("welcome home")) {
                        // Use the Deck Oracle to decide if the pull is worth firing. Block
                        // (save the card for battle) when EITHER:
                        //   (a) DEAD PULL — neither Petranaki Arena NOR The Works is currently
                        //       in the Reserve Deck. The Oracle tracks live zones AND the deck
                        //       list, so a location not in the deck at all (e.g. NO Petranaki
                        //       Arena in this deck) reads as not-in-reserve, as does one already
                        //       pulled out. Steve's case: no Petranaki Arena + The Works already
                        //       on table = nothing to fetch.
                        //   (b) SAVE FOR BATTLE — The Works is already on the table or in hand
                        //       (even if Petranaki Arena is still pullable, hold for the battle mode).
                        // Falls back to a gameState table/hand scan for The Works if the Oracle
                        // is unavailable.
                        boolean v155Block = false;
                        String v155Why = null;
                        com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle v155Oracle = context.getDeckOracle();
                        if (v155Oracle != null && v155Oracle.isAnalyzed()) {
                            boolean worksInReserve = v155Oracle.isCardInReserve("The Works");
                            boolean petranakiInReserve = v155Oracle.isCardInReserve("Petranaki Arena");
                            boolean worksOut = v155Oracle.isCardInPlay("The Works") || v155Oracle.isCardInHand("The Works");
                            if (!worksInReserve && !petranakiInReserve) {
                                v155Block = true;
                                v155Why = "DEAD PULL — neither The Works nor Petranaki Arena is in the Reserve Deck (nothing to fetch)";
                            } else if (worksOut) {
                                v155Block = true;
                                v155Why = "The Works already on table/in hand (save for battle, Petranaki still pullable)";
                            }
                        } else {
                            // Fallback (no Oracle): scan table/hand for The Works.
                            boolean worksOut = false;
                            for (PhysicalCard pc : gameState.getAllPermanentCards()) {
                                if (pc == null || pc.getTitle() == null) continue;
                                if (pc.getTitle().toLowerCase(java.util.Locale.ROOT).contains("the works")
                                        && pc.getZone() != null && pc.getZone().isInPlay()) { worksOut = true; break; }
                            }
                            if (!worksOut) {
                                java.util.List<PhysicalCard> v155Hand = gameState.getHand(context.getPlayerId());
                                if (v155Hand != null) {
                                    for (PhysicalCard hc : v155Hand) {
                                        if (hc != null && hc.getTitle() != null
                                                && hc.getTitle().toLowerCase(java.util.Locale.ROOT).contains("the works")) { worksOut = true; break; }
                                    }
                                }
                            }
                            if (worksOut) { v155Block = true; v155Why = "The Works already on table/in hand (no Oracle)"; }
                        }
                        if (v155Block) {
                            action.addReasoning(
                                "V155 WELCOME HOME: " + v155Why + " — SAVE this card for battle (Tyranus ability-number mode), don't waste the pull",
                                -2000.0f);
                            logger.warn("V155 WELCOME HOME BLOCK: {} — save for battle, block mode-1 location pull", v155Why);
                        }
                    }
                } catch (NumberFormatException nfe) { /* not a numeric cardId */ }
                catch (Exception e) { logger.debug("V155 error: {}", e.getMessage()); }
            }

            // === V160 (Steve, 2026-05-29): SHIELD WILL BE DOWN IN MOMENTS — PUSH TARGET THE MAIN GENERATOR ===
            // The deck's flip condition is Main Power Generators "blown away" — and only
            // Target The Main Generator (Epic Event, deploys on Ice Plains) lets the AT-AT
            // Cannon fire at the generators. Without TtMG on the table, the deck CAN'T win.
            // Steve: "He needs to get the epic event on the table so he can blow up the hoth
            // generator." Push any action involving Target The Main Generator when the deck
            // is recognized (covers the deploy and the fire-AT-AT response).
            if (textLower.contains("target the main generator")) {
                com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer v160OA = context.getObjectiveAnalyzer();
                if (v160OA != null && v160OA.isAnalyzed() && v160OA.isShieldWillBeDown()) {
                    action.addReasoning(
                        "V160 PUSH TARGET THE MAIN GENERATOR: deck's flip engine — deploy/fire to enable AT-AT vs Main Power Generators",
                        800.0f);
                    logger.warn("V160 SHIELD WILL BE DOWN: pushing '{}' — Target The Main Generator action (+800)", actionText);
                }
            }

            // === V158 RESERVE-DEPLOY BYPASS GUARD (Steve 2026-05-29, replay ss2jc7) ===
            // The fork's V158 (DeployEvaluator) catches normal weapon-deploy actions but is
            // bypassed when a weapon comes FROM RESERVE via an effect (Evil Is Everywhere
            // deploys [Episode I] lightsaber on Sidious; Sidious' Lightsaber from Reserve
            // on Sidious). Replay: Lord Sidious got Asajj Ventress' Lightsabers (t1) AND
            // Sidious' Lightsaber (t3) — DOUBLE-ARMED, breaking the one-weapon-per-char
            // rule. Defensive guard at the action-text layer: when an action text matches
            // "<weapon-word> from Reserve Deck on <character>", look up the named character
            // on the table and block (-9999) if it already has a weapon attached. Appended
            // into V158 (no new tag) — Steve: "avoid splintering off versions like before."
            if (cardId != null && gameState != null
                    && (textLower.contains("from reserve deck on") || textLower.contains("from reserve on"))
                    && (textLower.contains("lightsaber") || textLower.contains("blaster")
                        || textLower.contains("rifle") || textLower.contains("bowcaster")
                        || textLower.contains("weapon"))) {
                try {
                    int onIdx = textLower.lastIndexOf(" on ");
                    if (onIdx > 0) {
                        String targetSubstr = textLower.substring(onIdx + 4).trim();
                        for (PhysicalCard pc : gameState.getAllPermanentCards()) {
                            if (pc == null || pc.getTitle() == null) continue;
                            if (pc.getBlueprint() == null
                                    || pc.getBlueprint().getCardCategory()
                                        != com.gempukku.swccgo.common.CardCategory.CHARACTER) continue;
                            if (pc.getZone() == null || !pc.getZone().isInPlay()) continue;
                            String pcTitle = pc.getTitle().toLowerCase(java.util.Locale.ROOT);
                            String pcFirstPart = pcTitle.split(",")[0].trim();
                            if (!targetSubstr.contains(pcTitle) && !targetSubstr.contains(pcFirstPart)) continue;
                            // Found the target character — check if armed.
                            boolean v158Armed = false;
                            java.util.List<PhysicalCard> v158Att = gameState.getAttachedCards(pc);
                            if (v158Att != null) {
                                for (PhysicalCard att : v158Att) {
                                    if (att != null && att.getBlueprint() != null
                                            && att.getBlueprint().getCardCategory()
                                                == com.gempukku.swccgo.common.CardCategory.WEAPON) {
                                        v158Armed = true; break;
                                    }
                                }
                            }
                            if (v158Armed) {
                                action.addReasoning(String.format(
                                    "V158 RESERVE-DEPLOY BLOCK: %s already armed — no 2nd weapon (reserve-deploy bypass guard)",
                                    pc.getTitle()), -9999.0f);
                                logger.warn("V158 RESERVE-DEPLOY BLOCK: target {} already armed → -9999", pc.getTitle());
                            }
                            break;
                        }
                    }
                } catch (Exception e) { logger.debug("V158 RESERVE-DEPLOY GUARD error: {}", e.getMessage()); }
            }

            // V158 RESERVE-DEPLOY GUARD — NO-WIELDER branch (appended 2026-05-29, replay
            // filx81 turn 2): the first branch above catches "<weapon> from Reserve Deck
            // on <character>" + character armed. This second branch catches the auto-
            // targeted case where the action text lacks "on X" because the persona is
            // implied by the weapon's name. filx81: Rando pulled Vader's Lightsaber via
            // I Am Your Father (V) on turn 2, but Lord Vader didn't deploy until turn 3
            // — the saber landed in hand, then was lost as force-loss fodder. Pattern
            // detection: action text contains "X's Lightsaber" + "from Reserve". Extract
            // X (the persona word before "'s lightsaber"), check the table; if X isn't
            // present, block -9999 (no wielder = wasted pull). No new V-tag.
            if (cardId != null && gameState != null && textLower != null
                    && textLower.contains("from reserve")
                    && textLower.contains("'s lightsaber")) {
                try {
                    int v158nwIdx = textLower.indexOf("'s lightsaber");
                    if (v158nwIdx > 0) {
                        String v158nwBefore = textLower.substring(0, v158nwIdx).trim();
                        String[] v158nwParts = v158nwBefore.split("[\\s•·]+");
                        String v158nwPersona = v158nwParts.length > 0
                            ? v158nwParts[v158nwParts.length - 1].trim() : "";
                        if (v158nwPersona.length() > 1) {
                            boolean v158nwOnTable = false;
                            for (PhysicalCard pc : gameState.getAllPermanentCards()) {
                                if (pc == null || pc.getTitle() == null) continue;
                                if (pc.getBlueprint() == null
                                        || pc.getBlueprint().getCardCategory()
                                            != com.gempukku.swccgo.common.CardCategory.CHARACTER) continue;
                                if (pc.getZone() == null || !pc.getZone().isInPlay()) continue;
                                // V180 (2026-06): match the wielder by PERSONA, not just printed
                                // title. "Young Skywalker" carries Persona.LUKE but his title has
                                // no "luke" — the old title-only check wrongly fired NO-WIELDER and
                                // blocked Luke's own saber 12x in one game (E1, replay aab2jiaa5sca),
                                // so Luke fought bare-handed all game. Same lesson as senators:
                                // identity lives in the persona set, not always the printed name.
                                if (pc.getTitle().toLowerCase(java.util.Locale.ROOT).contains(v158nwPersona)) {
                                    v158nwOnTable = true; break;
                                }
                                java.util.Set<com.gempukku.swccgo.common.Persona> v158nwPersonas =
                                    pc.getBlueprint().getPersonas();
                                if (v158nwPersonas != null) {
                                    for (com.gempukku.swccgo.common.Persona v158nwP : v158nwPersonas) {
                                        if (v158nwP != null && v158nwP.name()
                                                .toLowerCase(java.util.Locale.ROOT).contains(v158nwPersona)) {
                                            v158nwOnTable = true; break;
                                        }
                                    }
                                    if (v158nwOnTable) break;
                                }
                            }
                            if (!v158nwOnTable) {
                                action.addReasoning(String.format(
                                    "V158 NO-WIELDER BLOCK: %s's Lightsaber from Reserve but no '%s' on table — wasted pull, the saber will sit in hand and bleed out",
                                    v158nwPersona, v158nwPersona), -9999.0f);
                                logger.warn("V158 NO-WIELDER BLOCK: '{}' not on table — block weapon pull -9999", v158nwPersona);
                            }
                        }
                    }
                } catch (Exception e) { logger.debug("V158 NO-WIELDER GUARD error: {}", e.getMessage()); }
            }

            // === V144 (Steve, 2026-05-26): YOU ARE BEATEN MODE GATING ===
            //
            // You Are Beaten (Lost Interrupt) has three modes:
            //   1. Use 2 Force, target char present with our warrior+lightsaber →
            //      can't move/battle until end of our next turn (BATTLE FREEZE)
            //   2. Use 1 Force, search Reserve for I Am Your Father, into hand
            //   3. Cancel Uncontrollable Fury
            //
            // Steve's rule: save You Are Beaten in hand for BATTLE use (Mode 1).
            // Especially block Mode 2 (search IAYF) if I Am Your Father is
            // already on table — that mode is then useless. Also bias against
            // Mode 2 outside of needing IAYF.
            if (cardId != null && context.getGameState() != null) {
                try {
                    com.gempukku.swccgo.game.state.GameState v144Gs = context.getGameState();
                    PhysicalCard v144Src = v144Gs.findCardById(Integer.parseInt(cardId));
                    if (v144Src != null && v144Src.getTitle() != null
                            && v144Src.getTitle().toLowerCase(java.util.Locale.ROOT)
                                .contains("you are beaten")) {
                        // Mode 2 detection: action text mentions "I Am Your Father" search.
                        // Per Steve 2026-05-26: NEVER use You Are Beaten for the IAYF
                        // search mode. The card is for battle freeze (Mode 1) or Cancel
                        // Uncontrollable Fury (Mode 3). Hard-block Mode 2 universally.
                        boolean v144IsIayfSearch = textLower.contains("i am your father")
                            || (textLower.contains("father") && textLower.contains("into hand"));
                        if (v144IsIayfSearch) {
                            action.addReasoning(
                                "V144 YOU ARE BEATEN: Mode 2 (IAYF search) — never use this mode, save for battle freeze or Cancel Uncontrollable Fury",
                                -2000.0f);
                            logger.warn("V144 YOU ARE BEATEN: blocking IAYF search mode universally");
                        }
                        // Mode 1 (battle freeze) — encourage when in battle phase
                        boolean v144IsBattleFreeze = textLower.contains("cannot move or battle")
                            || textLower.contains("target a character present");
                        if (v144IsBattleFreeze && context.getPhase() == Phase.BATTLE) {
                            action.addReasoning(
                                "V144 YOU ARE BEATEN: Battle freeze in battle phase — strong use!",
                                500.0f);
                        }
                    }
                } catch (NumberFormatException nfe) { /* not numeric */ }
                catch (Exception e) { logger.debug("V144 error: {}", e.getMessage()); }
            }

            // === V97 (Steve, 2026-05-20): PULL FROM RESERVE BEFORE ACTIVATING FORCE ===
            // Per Steve: "If Effect, Epic Event, Interrupt, or Objective lets us
            // pull a card before activating, we should do so. Force activation
            // moves cards from Reserve into Force Pile where pulls can't reach
            // them. Pulling first preserves the maximum search pool."
            //
            // Scope: source card must be Effect / Epic Event / Interrupt /
            // Objective AND the action must be a Reserve-Deck pull. Excludes
            // Knowledge And Defense (pulls from stacked cards, not Reserve).
            // Excludes character / starship / vehicle pulls (those have their
            // own timing windows in Deploy Phase).
            // V97 STANDALONE BLOCK ABSORBED by V192 pull scorer 2026-07-06 (T4.2 merge):
            // the scope predicate (Phase==ACTIVATE, static source EFFECT/EPIC_EVENT/INTERRUPT/
            // OBJECTIVE, K&D + Anger, Fear, Aggression excluded) moved VERBATIM into the scorer
            // as the +5500 PULL_BASE_ACTIVATE gate. +1500 was NOT enough: V168 ALWAYS ACTIVATE
            // (+5000) outvoted the whole pull pile (+2000) and pulls fired AFTER activation with
            // a shrunken pool (boundary row 1a, feedback_pull_before_activate).
            // Commented out per feedback_comment_out_old_rules:
            // if (cardId != null && context.getGameState() != null
                    // && context.getPhase() == Phase.ACTIVATE) {
                // try {
                    // com.gempukku.swccgo.game.state.GameState v97Gs = context.getGameState();
                    // PhysicalCard v97Src = v97Gs.findCardById(Integer.parseInt(cardId));
                    // // V129 (Steve, 2026-05-24): Also exclude Anger, Fear, Aggression —
                    // // light-side equivalent of K&D, same stacked-pile mechanic, pulls
                    // // from stack not Reserve. Symmetric with chosenone V97.
                    // if (v97Src != null && v97Src.getBlueprint() != null
                            // && v97Src.getTitle() != null
                            // // EXCLUDE Knowledge And Defense — stacked-card pull, not Reserve.
                            // && !v97Src.getTitle().contains("Knowledge And Defense")
                            // // V129: EXCLUDE Anger, Fear, Aggression — light-side mirror
                            // && !v97Src.getTitle().contains("Anger, Fear, Aggression")) {
                        // com.gempukku.swccgo.common.CardCategory v97Cat =
                            // v97Src.getBlueprint().getCardCategory();
                        // boolean isStaticSource =
                            // v97Cat == com.gempukku.swccgo.common.CardCategory.EFFECT
                            // || v97Cat == com.gempukku.swccgo.common.CardCategory.EPIC_EVENT
                            // || v97Cat == com.gempukku.swccgo.common.CardCategory.INTERRUPT
                            // || v97Cat == com.gempukku.swccgo.common.CardCategory.OBJECTIVE;
                        // if (isStaticSource) {
                            // // Is this action a Reserve-Deck pull? Check action text
                            // // for canonical pull phrasing.
                            // boolean isPull =
                                // textLower.contains("from reserve deck")
                                // || textLower.contains("[upload]")
                                // || textLower.contains("[download]")
                                // || textLower.contains("take") && textLower.contains("into hand");
                            // if (isPull) {
                                // action.addReasoning(String.format(
                                    // "V97 PULL BEFORE ACTIVATE: '%s' (%s) — fire pull now to preserve max Reserve search pool",
                                    // v97Src.getTitle(), v97Cat), 1500.0f);
                                // logger.warn("V97 PULL BEFORE ACTIVATE: {} from {} → +1500",
                                    // actionText, v97Src.getTitle());
                            // }
                        // }
                    // }
                // } catch (NumberFormatException nfe) {
                    // // not numeric cardId
                // } catch (Exception e) {
                    // logger.debug("V97 error: {}", e.getMessage());
                // }
            // }

            // === V100 (Steve, 2026-05-20): LOCATION PULL/DEPLOY BEFORE CHARACTER DEPLOY ===
            // Per Steve: "If an Effect/Interrupt/Objective/EpicEvent lets us pull
            // or deploy a LOCATION from Reserve Deck, fire it BEFORE any character
            // or vehicle deploys in the same deploy phase."
            //
            // Why: locations expand our deployment footprint. Characters deployed
            // before the new location is on table can only land on already-existing
            // sites — leaving the new location empty and forcing a wasted move phase.
            //
            // Companion to V97 (which fires during ACTIVATE phase). V100 fires
            // during DEPLOY phase and is LOCATION-specific.
            //
            // EXCLUDE Knowledge And Defense (stacked-card pull, not Reserve).
            // V100 STANDALONE BLOCK ABSORBED by V192 pull scorer 2026-07-06 (T4.2 merge):
            // its location-noun vocabulary ("planet") merged into the shared isLocationPull
            // predicate (V67l list, V192 region), and its chars-or-vehicles-in-hand check is now
            // the +25 CONTEXT bonus on location tiers during DEPLOY (one tier bonus, not a
            // stacked +1500 — ds-5). Commented out per feedback_comment_out_old_rules:
            // if (cardId != null && context.getGameState() != null
                    // && context.getPhase() == Phase.DEPLOY) {
                // try {
                    // com.gempukku.swccgo.game.state.GameState v100Gs = context.getGameState();
                    // PhysicalCard v100Src = v100Gs.findCardById(Integer.parseInt(cardId));
                    // // V129 (Steve, 2026-05-24): Also exclude Anger, Fear, Aggression —
                    // // light-side equivalent of K&D. Symmetric with chosenone V100.
                    // if (v100Src != null && v100Src.getBlueprint() != null
                            // && v100Src.getTitle() != null
                            // && !v100Src.getTitle().contains("Knowledge And Defense")
                            // && !v100Src.getTitle().contains("Anger, Fear, Aggression")) {
                        // com.gempukku.swccgo.common.CardCategory v100Cat =
                            // v100Src.getBlueprint().getCardCategory();
                        // boolean isStaticSource =
                            // v100Cat == com.gempukku.swccgo.common.CardCategory.EFFECT
                            // || v100Cat == com.gempukku.swccgo.common.CardCategory.EPIC_EVENT
                            // || v100Cat == com.gempukku.swccgo.common.CardCategory.INTERRUPT
                            // || v100Cat == com.gempukku.swccgo.common.CardCategory.OBJECTIVE;
                        // if (isStaticSource) {
                            // // Match the SOURCE card's game text for a location-pull pattern.
                            // // Detect by location nouns (docking bay/location/system/site/sector/planet)
                            // // + a Reserve-Deck verb (deploy from reserve / take into hand from reserve).
                            // String v100Gt = v100Src.getBlueprint().getGameText();
                            // String v100GtLower = v100Gt != null
                                // ? v100Gt.toLowerCase(Locale.ROOT) : "";
                            // boolean mentionsReserve = v100GtLower.contains("from reserve deck")
                                // || v100GtLower.contains("from your reserve deck");
                            // boolean mentionsLocationNoun =
                                // v100GtLower.contains("docking bay")
                                // || v100GtLower.contains(" location")
                                // || v100GtLower.contains("location ")
                                // || v100GtLower.contains(" system")
                                // || v100GtLower.contains("system ")
                                // || v100GtLower.contains(" site")
                                // || v100GtLower.contains("site ")
                                // || v100GtLower.contains(" sector")
                                // || v100GtLower.contains("sector ")
                                // || v100GtLower.contains(" planet")
                                // || v100GtLower.contains("planet ");
                            // // Verify this specific action is the pull (not a different
                            // // ability on the same card). Action text must mention deploy/take
                            // // from Reserve.
                            // boolean actionIsPull =
                                // textLower.contains("from reserve deck")
                                // || textLower.contains("[upload]")
                                // || textLower.contains("[download]")
                                // || (textLower.contains("deploy") && textLower.contains("reserve"))
                                // || (textLower.contains("take") && textLower.contains("into hand")
                                    // && textLower.contains("reserve"));
                            // if (mentionsReserve && mentionsLocationNoun && actionIsPull) {
                                // // Check we still have characters/vehicles in hand to deploy
                                // boolean haveCharOrVehicleInHand = false;
                                // java.util.List<com.gempukku.swccgo.game.PhysicalCard> v100Hand =
                                    // context.getHand();
                                // if (v100Hand != null) {
                                    // for (com.gempukku.swccgo.game.PhysicalCard hc : v100Hand) {
                                        // if (hc == null || hc.getBlueprint() == null) continue;
                                        // com.gempukku.swccgo.common.CardCategory hCat =
                                            // hc.getBlueprint().getCardCategory();
                                        // if (hCat == com.gempukku.swccgo.common.CardCategory.CHARACTER
                                                // || hCat == com.gempukku.swccgo.common.CardCategory.VEHICLE) {
                                            // haveCharOrVehicleInHand = true;
                                            // break;
                                        // }
                                    // }
                                // }
                                // if (haveCharOrVehicleInHand) {
                                    // action.addReasoning(String.format(
                                        // "V100 LOCATION PULL BEFORE CHARACTERS: '%s' (%s) — fire location pull"
                                            // + " now so chars can deploy to the new site this phase",
                                        // v100Src.getTitle(), v100Cat), 1500.0f);
                                    // logger.warn("V100 LOCATION PULL FIRST: {} from {} → +1500",
                                        // actionText, v100Src.getTitle());
                                // }
                            // }
                        // }
                    // }
                // } catch (NumberFormatException nfe) {
                    // // not numeric cardId
                // } catch (Exception e) {
                    // logger.debug("V100 error: {}", e.getMessage());
                // }
            // }

            // V79 (Steve, 2026-05-15): VERGE — DEATH STAR PARSEC / ORBIT MULTIPLE_CHOICE
            // After picking "Move using hyperspeed" the engine fires a
            // MULTIPLE_CHOICE: "Choose parsec to move to ". Options are parsec
            // numbers as strings. Score the one closest to 7 (Scarif).
            // Then a second MULTIPLE_CHOICE may fire: "Choose destination for
            // Death Star at parsec X" with orbit options. Pick Scarif.
            //
            // V103 (Steve, 2026-05-20): MULTIPLE_CHOICE Verge detection was failing.
            // Bug: v79Verge returned false even when the DeployEvaluator scan correctly
            // identified Verge on the same turn. Fix:
            //   - mirror DeployEvaluator's pZone.isInPlay() guard so we skip cards in
            //     piles (which can carry titles that match objectives but aren't active).
            //   - log v79Verge/v79AtScarif + iteration count for fast debug next time.
            //   - loosen owner match (~Rando_Cal vs Rando_Cal can differ); also treat a
            //     pure decision-text fallback ("Choose parsec to move to" + any Death
            //     Star we own) as Verge.
            //   - ALWAYS produce a scored action so the engine doesn't fall back to
            //     option-0 (parsec 2). If V79 path doesn't fire, score by distance to
            //     parsec 7 anyway (smaller bonus +300).
            {
                String v79DtLower = context.getDecisionText() != null
                    ? context.getDecisionText().toLowerCase() : "";
                boolean v79IsParsecChoice = v79DtLower.contains("choose parsec to move to");
                boolean v79IsDestChoice = v79DtLower.contains("choose destination for")
                    && v79DtLower.contains("parsec");
                if ((v79IsParsecChoice || v79IsDestChoice) && gameState != null
                        && context.getPlayerId() != null) {
                    // Confirm Verge of Greatness active + Death Star not at Scarif
                    boolean v79Verge = false;
                    boolean v79AtScarif = false;
                    boolean v79HaveDeathStar = false;
                    int v79IteratedCards = 0;
                    String v79PlayerId = context.getPlayerId();
                    String v79PidNorm = v79PlayerId != null
                        ? v79PlayerId.replace("~", "") : "";
                    try {
                        for (PhysicalCard pc : gameState.getAllPermanentCards()) {
                            if (pc == null) continue;
                            // V103: loosen owner match — accept ~Rando_Cal vs Rando_Cal,
                            // and accept null owner (objective-on-table edge cases).
                            String pOwner = pc.getOwner();
                            String pOwnerNorm = pOwner != null
                                ? pOwner.replace("~", "") : "";
                            boolean ownerMatches = pOwner == null
                                || pOwner.equals(v79PlayerId)
                                || pOwnerNorm.equals(v79PidNorm);
                            if (!ownerMatches) continue;
                            if (pc.getBlueprint() == null) continue;
                            // V103: skip cards not in play (mirror DeployEvaluator guard)
                            com.gempukku.swccgo.common.Zone pZone = pc.getZone();
                            if (pZone == null || !pZone.isInPlay()) continue;
                            v79IteratedCards++;
                            String t = pc.getTitle() != null
                                ? pc.getTitle().toLowerCase(java.util.Locale.ROOT) : "";
                            if (t.contains("on the verge of greatness")
                                    || t.contains("taking control of the weapon")) {
                                v79Verge = true;
                            }
                            if (t.contains("death star")
                                    && pc.getBlueprint().getCardCategory() == CardCategory.LOCATION) {
                                v79HaveDeathStar = true;
                                // V79 UPDATED 2026-07-07 (VERGE post-flip fix, Game9f3c46b00681):
                                // getAtLocation() is ALWAYS null for the Death Star mobile-system
                                // LOCATION card, so v79AtScarif never went true and this arm kept
                                // paying to steer a parked Death Star (from orbit the parsec-7 pick
                                // is the DEEP-SPACE EXIT — the engine excludes the orbited system,
                                // MoveMobileSystemUsingHyperspeedAction:82). Use the engine's orbit
                                // primitive getSystemOrbited() (same check as the flip condition,
                                // Filters.isOrbiting(Title.Scarif), Card216_011:122). With
                                // v79AtScarif true the steering branch below is skipped and the V103
                                // PARSEC FALLBACK's closest-to-7 pick IS the stay pick (Scarif =
                                // parsec 7) — no extra !flipped gate needed; post-flip deep-space
                                // recovery steering intentionally stays live so the DS re-orbits.
                                // PhysicalCard dsLoc = pc.getAtLocation();
                                // if (dsLoc != null && dsLoc.getTitle() != null
                                //         && dsLoc.getTitle().toLowerCase(java.util.Locale.ROOT).contains("scarif")) {
                                //     v79AtScarif = true;
                                // }
                                String dsOrbited = pc.getSystemOrbited();
                                if (dsOrbited != null
                                        && dsOrbited.toLowerCase(java.util.Locale.ROOT).contains("scarif")) {
                                    v79AtScarif = true;
                                }
                            }
                        }
                    } catch (Exception e) { /* ignore */ }

                    logger.warn("V103 PARSEC DETECT: verge={} atScarif={} haveDeathStar={} iterated={} dt='{}'",
                        v79Verge, v79AtScarif, v79HaveDeathStar, v79IteratedCards,
                        context.getDecisionText());

                    // V103: fallback — if scan didn't find Verge but we DO own a Death Star
                    // and the decision text is the parsec/destination prompt, treat as Verge.
                    if (!v79Verge && v79HaveDeathStar) {
                        v79Verge = true;
                        logger.warn("V103 PARSEC FALLBACK: Verge implied by Death Star ownership + parsec prompt");
                    }

                    if (v79Verge && !v79AtScarif) {
                        if (v79IsParsecChoice) {
                            // actionText is the parsec number (e.g., "2" or "6")
                            Integer parsec = null;
                            try { parsec = Integer.parseInt(actionText.trim()); }
                            catch (Exception e) {
                                // Some implementations might prefix the number
                                java.util.regex.Matcher pm = java.util.regex.Pattern
                                    .compile("(\\d+)").matcher(actionText);
                                if (pm.find()) {
                                    try { parsec = Integer.parseInt(pm.group(1)); }
                                    catch (Exception ee) { /* ignore */ }
                                }
                            }
                            if (parsec != null) {
                                int dist = Math.abs(parsec - 7);
                                if (dist == 0) {
                                    action.addReasoning("V79 PARSEC 7 (Scarif!) — pick this", 1500.0f);
                                    logger.warn("V79 PARSEC CHOICE: parsec 7 (Scarif) → +1500");
                                } else if (dist == 1) {
                                    action.addReasoning("V79 PARSEC " + parsec + " (1 hop from Scarif)", 1200.0f);
                                    logger.warn("V79 PARSEC CHOICE: parsec {} → +1200", parsec);
                                } else if (parsec > 4) {
                                    action.addReasoning("V79 PARSEC " + parsec + " (toward Scarif)", 800.0f);
                                    logger.warn("V79 PARSEC CHOICE: parsec {} → +800", parsec);
                                } else {
                                    action.addReasoning("V79 PARSEC " + parsec + " — WRONG DIRECTION", -800.0f);
                                    logger.warn("V79 PARSEC CHOICE WRONG WAY: parsec {} → -800", parsec);
                                }
                            }
                        } else if (v79IsDestChoice) {
                            // actionText is the destination — pick Scarif over deep space
                            if (textLower.contains("scarif")) {
                                action.addReasoning("V79 ORBIT SCARIF — must take!", 1500.0f);
                                logger.warn("V79 DESTINATION: orbit Scarif → +1500");
                            } else {
                                action.addReasoning("V79 destination not Scarif — avoid", -200.0f);
                                logger.warn("V79 DESTINATION: '{}' (not Scarif) → -200", actionText);
                            }
                        }
                        // V79 (Steve, 2026-05-15): MUST add action to output list.
                        // The default ActionTextEvaluator flow only appends actions
                        // when a specific pattern branch matches the action text.
                        // For parsec-number action texts ("2", "6") none of those
                        // pattern branches match, so the action would be dropped
                        // and the engine would report "No evaluators produced actions".
                        actions.add(action);
                        continue;
                    }

                    // V103 PARSEC FALLBACK: if no Verge/DS detected but the engine
                    // is still asking us to choose a parsec, score by distance to 7
                    // anyway so the AI picks the better option instead of defaulting
                    // to the first option (typically parsec 2).
                    if (v79IsParsecChoice) {
                        Integer fparsec = null;
                        try { fparsec = Integer.parseInt(actionText.trim()); }
                        catch (Exception e) {
                            java.util.regex.Matcher pm = java.util.regex.Pattern
                                .compile("(\\d+)").matcher(actionText);
                            if (pm.find()) {
                                try { fparsec = Integer.parseInt(pm.group(1)); }
                                catch (Exception ee) { /* ignore */ }
                            }
                        }
                        if (fparsec != null) {
                            int fdist = Math.abs(fparsec - 7);
                            float fbonus = Math.max(0, 300 - (fdist * 50));
                            action.addReasoning(String.format(
                                "V103 PARSEC FALLBACK: parsec %d (dist %d to Scarif) → +%.0f",
                                fparsec, fdist, fbonus), fbonus);
                            logger.warn("V103 PARSEC FALLBACK: parsec {} dist {} → +{}",
                                fparsec, fdist, (int)fbonus);
                            actions.add(action);
                            continue;
                        }
                    }
                }
            }

            // V67bi FORCE LIGHTNING SELF-TARGET HARD-BLOCK (Steve, 2026-05-10)
            // ===================================================================
            // Hard-block Force Lightning if there's no opponent character in
            // play to target. The engine already requires the granting card
            // (Emperor or equivalent) to be present for the action to even
            // appear, so we don't need to look for Emperor — we just verify a
            // valid OPPONENT target exists. Otherwise Rando burns 5 force to
            // hit his own character.
            //
            // Pattern extends to any "target a character" Sith damage interrupt
            // (Force Push, Lightsaber Combat, etc.) — add per card-title as they
            // surface in replays.
            {
                GameState v67biGs = context.getGameState();
                String v67biPid = context.getPlayerId();
                if (cardId != null && v67biGs != null && v67biPid != null) {
                    try {
                        PhysicalCard v67biSource = v67biGs.findCardById(Integer.parseInt(cardId));
                        if (v67biSource != null && v67biSource.getTitle() != null
                                && v67biSource.getTitle().toLowerCase(java.util.Locale.ROOT)
                                       .contains("force lightning")) {
                            int v67biOpps = 0;
                            for (PhysicalCard pc : v67biGs.getAllPermanentCards()) {
                                if (pc == null || pc.getBlueprint() == null) continue;
                                if (v67biPid.equals(pc.getOwner())) continue;
                                if (pc.getBlueprint().getCardCategory()
                                        != com.gempukku.swccgo.common.CardCategory.CHARACTER) continue;
                                if (pc.getZone() == null || !pc.getZone().isInPlay()) continue;
                                v67biOpps++;
                                if (v67biOpps > 0) break;
                            }
                            if (v67biOpps == 0) {
                                action.addReasoning(
                                    "V67bi FORCE LIGHTNING BLOCK: no opponent character in play — never self-target!",
                                    -9999.0f);
                                logger.warn("V67bi FORCE LIGHTNING BLOCK: 0 opponent chars in play — hard-block '{}'",
                                    actionText);
                            } else {
                                logger.info("V67bi FORCE LIGHTNING OK: opponent char(s) in play — allow targeting");
                            }
                        }
                    } catch (NumberFormatException nfe) { /* ignore */ }
                      catch (Exception e) {
                        logger.debug("V67bi check error: {}", e.getMessage());
                    }
                }
            }

            // ========== V38.3: "Not activated Force" — ALWAYS go back and activate ==========
            // The game asks "You have not activated Force. Do you want to Pass?"
            // Options: "Yes" (pass without activating) and "No" (go back and activate)
            // ALWAYS choose "No" — Force is essential for deploying characters.
            {
                String decisionTextCheck = context.getDecisionText() != null
                    ? context.getDecisionText().toLowerCase() : "";
                if (decisionTextCheck.contains("not activated force") || decisionTextCheck.contains("have not activated")) {
                    // V61c DESTINY BUFFER carve-out (Steve, 2026-06-29): when the Reserve Deck is
                    // already <= 3, Rando deliberately passed activation (V168 exception above) to
                    // keep 3 cards for battle/weapon destiny. Here the engine confirms "you have not
                    // activated Force — pass?"; honor the pass ("Yes") instead of the usual V38.3
                    // bounce-back, or the buffer protection is undone.
                    // V61c UPDATED 2026-07-06: battle-intent bypass — honor the pass ONLY when a
                    // battle is plausible (shared predicate DecisionContext.isBattlePlausibleThisTurn(),
                    // same gate as the V168 carve-out above + the ForceActivationEvaluator keep-3
                    // cap). Zero contested locations => normal V38.3 bounce-back ("No", go activate).
                    int v38cReserve = context.getReserveDeckSize();
                    // V61c pre-2026-07-06 (always-on buffer):
                    // if (v38cReserve <= 3) {
                    boolean v38cBattlePlausible = context.isBattlePlausibleThisTurn();
                    if (v38cReserve <= 3 && !v38cBattlePlausible && textLower.equals("no")) {
                        // Logged once (on the "No" option, which the bypass flips to +9999).
                        logger.warn("V61c BATTLE-INTENT: no contested location — activating full");
                    }
                    if (v38cReserve <= 3 && v38cBattlePlausible) {
                        if (textLower.equals("yes")) {
                            action.addReasoning("V61c DESTINY BUFFER: reserve <= 3 — confirm pass, keep 3 for destiny", 9999.0f);
                            logger.warn("V61c DESTINY BUFFER: reserve={} <= 3 — confirming pass (skip activation)", v38cReserve);
                        } else if (textLower.equals("no")) {
                            action.addReasoning("V61c DESTINY BUFFER: reserve <= 3 — do not go back and activate", -9999.0f);
                        }
                    } else if (textLower.equals("no")) {
                        action.addReasoning("V38.3 MUST ACTIVATE: Go back and activate Force!", 9999.0f);
                        logger.warn("V38.3 MUST ACTIVATE: Choosing 'No' to go back and activate Force");
                    } else if (textLower.equals("yes")) {
                        action.addReasoning("V38.3 NEVER SKIP ACTIVATION: Do not pass without activating!", -9999.0f);
                        logger.warn("V38.3 BLOCKED: Refusing to skip Force activation");
                    }
                }
            }

            // ========== V53c: BLOCK WOKLING EFFECT SEARCH (EARLY CHECK) ==========
            // Wokling (V) costs 3 Force to search for an Effect from Reserve Deck.
            // Action text: "Take an Effect into hand from Reserve Deck"
            // MUST check EARLY before V29.7 PULL FIRST gives it +250.
            // Check source card ID — if it's Wokling (bp 200_47), hard block.
            if (textLower.contains("effect") && textLower.contains("reserve deck")
                && textLower.contains("take")) {
                boolean isWoklingSource = false;
                if (cardId != null && gameState != null) {
                    try {
                        PhysicalCard wokSrc = gameState.findCardById(Integer.parseInt(cardId));
                        if (wokSrc != null && wokSrc.getTitle() != null
                            && wokSrc.getTitle().toLowerCase(Locale.ROOT).contains("wokling")) {
                            isWoklingSource = true;
                        }
                        // Also check blueprint ID
                        if (wokSrc != null && wokSrc.getBlueprintId(true) != null
                            && wokSrc.getBlueprintId(true).equals("200_47")) {
                            isWoklingSource = true;
                        }
                    } catch (Exception e) { /* ignore */ }
                }
                if (isWoklingSource && context.getTurnNumber() <= 3) {
                    action.setScore(-9999.0f);
                    action.addReasoning("V53c BLOCK WOKLING: Turns 1-3 — save force for deploys, don't search!", -9999.0f);
                    logger.warn("V53c WOKLING BLOCKED: Turn {} — 3 force too precious, HARD BLOCK!", context.getTurnNumber());
                    actions.add(action);
                    continue; // Skip all further evaluation
                }
            }

            // ========== Skip ALL Deploy Actions ==========
            // Deploy actions should be handled EXCLUSIVELY by DeployEvaluator.
            if (actionText.equals("Deploy") ||
                (actionText.startsWith("Deploy ") && !textLower.contains("from"))) {
                // Skip this action - let DeployEvaluator handle it
                continue;
            }

            // ========== V24.4: LOCATIONS FIRST — DEPLOY LOCATIONS BEFORE ANYTHING ELSE ==========
            // Locations MUST be deployed before activating effects (AMSD, K&D, etc.).
            // If the bot has ANY location in hand, penalize all non-deploy actions heavily
            // so that deploy actions (handled by DeployEvaluator) always win priority.
            if (gameState != null && context.getPhase() == Phase.DEPLOY) {
                java.util.List<com.gempukku.swccgo.game.PhysicalCard> hand = context.getHand();
                if (hand != null) {
                    boolean hasLocationInHand = false;
                    for (com.gempukku.swccgo.game.PhysicalCard handCard : hand) {
                        if (handCard != null && handCard.getBlueprint() != null &&
                            handCard.getBlueprint().getCardCategory() == com.gempukku.swccgo.common.CardCategory.LOCATION) {
                            hasLocationInHand = true;
                            break;
                        }
                    }
                    if (hasLocationInHand) {
                        // Check if this action is a search that PULLS locations (TDIGWATT, I'm Sorry, etc.)
                        // Those are OK — they help GET locations. But effect activations like AMSD should wait.
                        // V24.9: Added "sorry" — I'm Sorry deploys interior CC sites from reserve!
                        boolean isLocationSearch = textLower.contains("bespin") || textLower.contains("location")
                            || textLower.contains("cloud city") || textLower.contains("site")
                            || textLower.contains("sorry");
                        // V24.15: Exempt AMSD from LOCATIONS FIRST penalty!
                        // AMSD deploys a Star Destroyer — it's effectively a deploy action, not an "effect".
                        // When Bespin is already on the table, AMSD should fire immediately to get Executor there.
                        boolean isAmsdAction = textLower.contains("alert my star destroyer") ||
                            textLower.contains("amsd") ||
                            (textLower.contains("reveal") && textLower.contains("pilot") && textLower.contains("star destroyer")) ||
                            (textLower.contains("star destroyer") && textLower.contains("deploy both"));
                        // V60 RESERVE PULL EXEMPTION: NEVER penalize Reserve Deck pulls.
                        // Steve's rule (feedback_reserve_deck_pulls.md): "[Download]" and
                        // "from Reserve Deck" actions are free value — thin the deck, bring
                        // key cards into play. Fire them every turn. They complement location
                        // deploys, they don't replace them. FIXES Issue #D from peaceful-pike
                        // replay: Sai'torr Kal Fas never fired Obi-Wan's Lightsaber because
                        // V24.4 blocked `[Download] a matching weapon` at -800.
                        boolean isReservePull = textLower.contains("[download]")
                            || textLower.contains("from reserve deck")
                            || textLower.contains("take an effect into hand")
                            || textLower.contains("take a character into hand");
                        // V67ba (Steve, 2026-05-08): EXEMPT generic deploy-from-hand actions.
                        // Action text "Play a card" / "Deploy" / "Deploy a card" is the ENTRY
                        // POINT to the deploy-from-hand sub-decision (CARD_SELECTION among
                        // hand cards). Penalizing it -800 means Rando never picks it, so
                        // the location in hand never gets deployed — the very thing V24.4
                        // is trying to force. FIXES 115yinsdp3t7t2q1.xml.gz: turn 2 had
                        // only 'Play a card' + 'Take Imperial Decree' as options; V24.4
                        // penalized 'Play a card' to -840, Pass scored -168, Rando passed.
                        boolean isDeployEntry = textLower.equals("play a card")
                            || textLower.equals("deploy")
                            || textLower.equals("deploy a card")
                            || textLower.startsWith("deploy ")
                            || textLower.startsWith("play a card ");
                        if (!isLocationSearch && !isAmsdAction && !isReservePull && !isDeployEntry) {
                            action.addReasoning("V24.4 LOCATIONS FIRST: Deploy locations in hand before activating effects!", -800.0f);
                            logger.warn("V24.4 LOCATIONS FIRST: Penalizing '{}' — location in hand needs deploying first! (-800)", actionText);
                        } else if (isAmsdAction) {
                            logger.warn("V24.15 AMSD EXEMPT: Not penalizing AMSD with LOCATIONS FIRST — AMSD deploys a Star Destroyer!");
                        } else if (isReservePull) {
                            logger.warn("V60 RESERVE PULL EXEMPT: '{}' is a Reserve Deck pull — NEVER penalize, always fire!", actionText);
                        } else if (isDeployEntry) {
                            logger.warn("V67ba DEPLOY-ENTRY EXEMPT: '{}' is the deploy-from-hand entry point — NEVER penalize!", actionText);
                        }
                    }
                }
            }

            // ========== V23: EMPTY PILE GUARD ==========
            // Block interrupts/actions that search piles which are empty.
            // Sith Fury on turn 1 wastes 4 force searching an empty Lost Pile.
            if (gameState != null) {
                String pid = context.getPlayerId();

                // === V29.14: NO ESCAPE — "Take top card of Lost Pile into hand" ===
                // This is FREE card advantage (not a search), works with any pile size >= 1.
                // Must be checked BEFORE the V23 empty pile guard so it doesn't get penalized.
                if (textLower.contains("take top card") && textLower.contains("lost pile")) {
                    int lostSize = gameState.getLostPile(pid).size();
                    if (lostSize > 0) {
                        action.addReasoning("V29.14 NO ESCAPE: Free card from Lost Pile — always take it!", 200.0f);
                        logger.warn("V29.14 NO ESCAPE: '{}' — Lost Pile has {} cards, taking top card!", actionText, lostSize);
                        actions.add(action);
                        continue;
                    }
                }

                // Lost Pile searches
                if (textLower.contains("lost pile") && (textLower.contains("take") ||
                    textLower.contains("search") || textLower.contains("retrieve"))) {
                    int lostSize = gameState.getLostPile(pid).size();
                    if (lostSize == 0) {
                        action.addReasoning("V23 EMPTY PILE: Lost Pile is empty — search will fail!", -300.0f);
                        logger.warn("V23 EMPTY PILE GUARD: Blocking '{}' — Lost Pile is empty!", actionText);
                        actions.add(action);
                        continue;
                    } else if (lostSize <= 2) {
                        action.addReasoning("V23 LOW PILE: Lost Pile only has " + lostSize + " cards — risky search", -100.0f);
                        logger.warn("V23 LOW PILE: '{}' — Lost Pile only has {} cards", actionText, lostSize);
                    }
                }
                // Used Pile searches
                if (textLower.contains("used pile") && (textLower.contains("take") ||
                    textLower.contains("search"))) {
                    int usedSize = gameState.getUsedPile(pid).size();
                    if (usedSize == 0) {
                        action.addReasoning("V23 EMPTY PILE: Used Pile is empty — search will fail!", -300.0f);
                        logger.warn("V23 EMPTY PILE GUARD: Blocking '{}' — Used Pile is empty!", actionText);
                        actions.add(action);
                        continue;
                    }
                }
            }

            // ========== V24: AMSD BESPIN GATE ==========
            // Alert My Star Destroyer needs a system location to deploy the Star Destroyer to.
            // If Bespin isn't on the table yet, AMSD has nowhere to send the ship — block it.
            if (gameState != null && (textLower.contains("alert my star destroyer") ||
                textLower.contains("amsd") ||
                (textLower.contains("star destroyer") && textLower.contains("deploy both")) ||
                (textLower.contains("star destroyer") && textLower.contains("pilot") && textLower.contains("deploy")) ||
                (textLower.contains("reveal") && textLower.contains("pilot") && textLower.contains("star destroyer")))) {
                boolean bespinSystemOnTable = false;
                try {
                    for (com.gempukku.swccgo.game.PhysicalCard loc : gameState.getLocationsInOrder()) {
                        if (loc != null && loc.getTitle() != null &&
                            loc.getTitle().toLowerCase(java.util.Locale.ROOT).contains("bespin") &&
                            loc.getBlueprint() != null && loc.getBlueprint().getCardSubtype() != null &&
                            loc.getBlueprint().getCardSubtype() == com.gempukku.swccgo.common.CardSubtype.SYSTEM) {
                            bespinSystemOnTable = true;
                            break;
                        }
                    }
                } catch (Exception e) {
                    logger.debug("V24 AMSD gate: Error checking Bespin: {}", e.getMessage());
                }
                if (!bespinSystemOnTable) {
                    action.addReasoning("V24 AMSD BLOCKED: No Bespin system on table — Star Destroyer has nowhere to deploy!", -9999.0f);
                    logger.warn("V24 AMSD GATE: HARD BLOCKING AMSD — Bespin system not on table yet! (-9999)");
                    actions.add(action);
                    continue;
                }
            }

            // ========== V24.10: AMSD — PIETT + EXECUTOR ONLY ==========
            // AMSD should ONLY fire when Piett is the pilot AND Executor is in reserve.
            // No other pilot (Chiraneau, Ozzel, Motti, Evazan, etc.) should use AMSD.
            // If Piett isn't the target or Executor isn't in reserve, block AMSD entirely.
            // AMSD can only be used TWICE per game — never waste an attempt!
            if (gameState != null && (textLower.contains("alert my star destroyer") ||
                textLower.contains("amsd") ||
                (textLower.contains("star destroyer") && textLower.contains("deploy both")) ||
                (textLower.contains("star destroyer") && textLower.contains("pilot") && textLower.contains("deploy")) ||
                (textLower.contains("reveal") && textLower.contains("pilot") && textLower.contains("star destroyer")))) {

                com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle amsdOracle = context.getDeckOracle();
                int currentTurn = context.getTurnNumber();

                // V24.10: Check if AMSD already failed this turn — don't waste a second attempt.
                // AMSD can only be used twice per game, so every attempt must count.
                // If it failed, Piett/Executor aren't in the right zones yet.
                // Wait for recirculation on the next turn.
                if (amsdOracle != null && amsdOracle.hasAmsdFailedThisTurn(currentTurn)) {
                    action.addReasoning("V24.10 AMSD BLOCKED: Already failed this turn — save for next turn after recirculation!", -9999.0f);
                    logger.warn("V24.10 AMSD RETRY BLOCK: AMSD already failed on turn {} — don't waste another attempt!", currentTurn);
                    actions.add(action);
                    continue;
                }

                // V24.10: AMSD pilot check — two scenarios:
                // 1. Action text names a specific pilot (e.g., "deploy Piett's matching Star Destroyer")
                //    → Check if it's Piett. Block if not.
                // 2. Action text is generic (e.g., "Reveal pilot or Star Destroyer from hand")
                //    → Check DeckOracle: is Piett in hand AND Executor in reserve? If so, ALLOW.
                //    The actual pilot selection happens in CardSelectionEvaluator's AMSD guard.
                boolean isGenericReveal = textLower.contains("reveal") && !textLower.contains("piett")
                    && !textLower.contains("vader") && !textLower.contains("chiraneau")
                    && !textLower.contains("ozzel") && !textLower.contains("motti");

                if (isGenericReveal) {
                    // Generic "Reveal pilot or Star Destroyer from hand" — use DeckOracle to decide
                    if (amsdOracle != null && amsdOracle.isAnalyzed()) {
                        boolean piettInHand = amsdOracle.isCardInHand("Admiral Piett") || amsdOracle.isCardInHand("Piett");
                        boolean executorInReserve = amsdOracle.isCardInReserve("Executor") ||
                            amsdOracle.isCardInReserve("Flagship Executor");
                        // V29.4: AMSD deploys Star Destroyer from HAND or RESERVE DECK!
                        // Previous code blocked when Executor was in hand — that was WRONG.
                        // AMSD is actually the BEST way to deploy Executor from hand because
                        // it deploys Piett+Executor simultaneously to the same system.
                        boolean executorInHand = amsdOracle.isCardInHand("Executor") ||
                            amsdOracle.isCardInHand("Flagship Executor");
                        boolean executorAvailable = executorInReserve || executorInHand;

                        // V29.4: Diagnostic logging — trace exactly what DeckOracle sees
                        logger.warn("V29.4 AMSD DIAGNOSTIC: piettInHand={}, executorInReserve={}, executorInHand={}, executorAvailable={}",
                            piettInHand, executorInReserve, executorInHand, executorAvailable);

                        if (piettInHand && executorAvailable) {
                            // V45: Check if we have enough force to pay for Piett + Executor
                            int amsdForceAvail = context.getForcePileSize();
                            int amsdMinForce = 7;
                            if (amsdForceAvail < amsdMinForce) {
                                action.addReasoning(String.format(
                                    "V45 AMSD UNAFFORDABLE: Need %d force for Piett+Executor but only %d available!",
                                    amsdMinForce, amsdForceAvail), -9999.0f);
                                logger.warn("V45 AMSD UNAFFORDABLE: Need {} force but only {} — HARD BLOCK!", amsdMinForce, amsdForceAvail);
                                actions.add(action);
                                continue;
                            }
                            // Piett + Executor available (in hand or reserve). ALLOW AMSD, boost it!
                            // V24.15: On turn 1-2, AMSD is CRITICAL — must fire immediately after Bespin!
                            // Later turns: still high priority but less urgent.
                            float amsdBoost = 500.0f;
                            String source = executorInHand ? "hand" : "reserve";
                            if (currentTurn <= 2) {
                                amsdBoost = 1500.0f;  // V24.15: Mega-boost on early turns — Executor MUST deploy ASAP!
                                action.addReasoning("V24.15 AMSD MEGA PRIORITY: Turn " + currentTurn + " — Executor (from " + source + ") MUST deploy NOW to control Bespin!", amsdBoost);
                                logger.warn("V24.15 AMSD MEGA PRIORITY: Turn {} — Piett in hand + Executor in {} — mega-boost +{} to ensure AMSD fires!", currentTurn, source, amsdBoost);
                            } else {
                                action.addReasoning("V24.10 AMSD APPROVED: Piett + Executor (from " + source + ") ready — fire AMSD!", amsdBoost);
                                logger.warn("V24.10 AMSD: Generic reveal — Piett in hand, Executor in {} — APPROVED (+{})!", source, amsdBoost);
                            }
                        } else if (!piettInHand) {
                            action.addReasoning("V24.10 AMSD BLOCKED: Piett NOT in hand — can't use AMSD!", -9999.0f);
                            logger.warn("V24.10 AMSD BLOCK: Generic reveal but Piett not in hand — block!");
                            amsdOracle.recordAmsdFailedOnTurn(currentTurn);
                            actions.add(action);
                            continue;
                        } else {
                            // V29.4: Executor not in hand OR reserve — truly unavailable
                            // Could be in force pile, used pile, lost pile, or not in deck
                            action.addReasoning("V29.4 AMSD BLOCKED: Piett in hand but Executor NOT in hand or reserve (may be in force/used pile)!", -9999.0f);
                            logger.warn("V29.4 AMSD BLOCK: Piett in hand but Executor not available (not in hand or reserve) — might be activated to force pile!");
                            amsdOracle.recordAmsdFailedOnTurn(currentTurn);
                            actions.add(action);
                            continue;
                        }
                    }
                    // If oracle unavailable, allow generic reveal (best guess)
                } else if (!textLower.contains("piett")) {
                    // Specific pilot named in action text but it's NOT Piett — hard block
                    action.addReasoning("V24.10 AMSD BLOCKED: Only Piett may use AMSD — " +
                        "this action targets a different pilot!", -9999.0f);
                    logger.warn("V24.10 AMSD HARD BLOCK: Action does NOT target Piett — only Piett + Executor allowed!");
                    if (amsdOracle != null) {
                        amsdOracle.recordAmsdFailedOnTurn(currentTurn);
                    }
                    actions.add(action);
                    continue;
                } else {
                    // Action specifically names Piett — verify Piett in hand AND Executor available
                    if (amsdOracle != null && amsdOracle.isAnalyzed()) {
                        boolean piettInHand = amsdOracle.isCardInHand("Admiral Piett") || amsdOracle.isCardInHand("Piett");
                        boolean executorInReserve = amsdOracle.isCardInReserve("Executor") ||
                            amsdOracle.isCardInReserve("Flagship Executor");
                        // V29.4: AMSD deploys from HAND or RESERVE — check both!
                        boolean executorInHand = amsdOracle.isCardInHand("Executor") ||
                            amsdOracle.isCardInHand("Flagship Executor");
                        boolean executorAvailable = executorInReserve || executorInHand;

                        // V29.4: Diagnostic logging
                        logger.warn("V29.4 AMSD DIAGNOSTIC (specific): piettInHand={}, executorInReserve={}, executorInHand={}, executorAvailable={}",
                            piettInHand, executorInReserve, executorInHand, executorAvailable);

                        if (!piettInHand) {
                            action.addReasoning("V24.10 AMSD BLOCKED: Piett is NOT in hand — can't use AMSD!", -9999.0f);
                            logger.warn("V24.10 AMSD GATE: Piett not in hand — HARD BLOCK");
                            amsdOracle.recordAmsdFailedOnTurn(currentTurn);
                            actions.add(action);
                            continue;
                        }
                        if (!executorAvailable) {
                            // V29.4: Executor not in hand OR reserve — truly unavailable
                            action.addReasoning("V29.4 AMSD BLOCKED: Piett in hand but Executor NOT in hand or reserve!", -9999.0f);
                            logger.warn("V29.4 AMSD GATE: Piett in hand but Executor not available — HARD BLOCK");
                            amsdOracle.recordAmsdFailedOnTurn(currentTurn);
                            actions.add(action);
                            continue;
                        }
                        // V45: Check if we have enough force to pay for Piett + Executor
                        int amsdForceAvailSpec = context.getForcePileSize();
                        int amsdMinForceSpec = 7;
                        if (amsdForceAvailSpec < amsdMinForceSpec) {
                            action.addReasoning(String.format(
                                "V45 AMSD UNAFFORDABLE: Need %d force for Piett+Executor but only %d available!",
                                amsdMinForceSpec, amsdForceAvailSpec), -9999.0f);
                            logger.warn("V45 AMSD UNAFFORDABLE: Need {} force but only {} — HARD BLOCK!", amsdMinForceSpec, amsdForceAvailSpec);
                            actions.add(action);
                            continue;
                        }
                        // Both confirmed — boost AMSD priority!
                        // V24.15: On turn 1-2, mega-boost to ensure Executor deploys ASAP
                        String source = executorInHand ? "hand" : "reserve";
                        float amsdBoostSpecific = (currentTurn <= 2) ? 1500.0f : 500.0f;
                        if (currentTurn <= 2) {
                            action.addReasoning("V24.15 AMSD MEGA PRIORITY: Turn " + currentTurn + " — Executor (from " + source + ") MUST deploy NOW!", amsdBoostSpecific);
                            logger.warn("V24.15 AMSD MEGA PRIORITY (specific): Turn {} — Executor in {} — +{} mega-boost!", currentTurn, source, amsdBoostSpecific);
                        } else {
                            action.addReasoning("V24.10 AMSD APPROVED: Piett + Executor (from " + source + ") ready!", amsdBoostSpecific);
                            logger.warn("V24.10 AMSD APPROVED: Piett in hand + Executor in {} — +{}!", source, amsdBoostSpecific);
                        }
                    }
                    // V29.4: If oracle unavailable, allow AMSD (best guess — don't block without data)
                }
            }

            // ========== V24: TDIGWATT EXHAUSTED SEARCH GUARD ==========
            // TDIGWATT searches for "Cloud City Occupation, Dark Deal, Vader's Bounty, or Bespin".
            // Once all targets have been pulled, every search fails — stop wasting the action.
            if (textLower.contains("cloud city occupation") && textLower.contains("dark deal") &&
                textLower.contains("bespin")) {
                com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle tdigOracle = context.getDeckOracle();
                if (tdigOracle != null && tdigOracle.isAnalyzed()) {
                    boolean anyTargetInReserve =
                        tdigOracle.isCardInReserve("Bespin") ||
                        tdigOracle.isCardInReserve("Dark Deal") ||
                        tdigOracle.isCardInReserve("Cloud City Occupation") ||
                        tdigOracle.isCardInReserve("Vader's Bounty");
                    if (!anyTargetInReserve) {
                        action.addReasoning("V24 TDIGWATT: All targets already pulled — search will fail!", -400.0f);
                        logger.warn("V24 TDIGWATT EXHAUSTED: All 4 targets (Bespin, Dark Deal, CC Occupation, Vader's Bounty) already pulled — blocking search!");
                        actions.add(action);
                        continue;
                    } else {
                        logger.info("V24 TDIGWATT: Targets still in reserve — search OK");
                    }
                }
            }

            // ========== V24.6B: I'M SORRY LOCATION PULL — USE UNTIL CC SITES EXHAUSTED ==========
            // I'm Sorry (V) deploys interior Cloud City sites from reserve deck.
            // Use EVERY turn until all CC interior sites are pulled from reserve.
            // DeckOracle tracks what's left — stop wasting the action when reserve is empty.
            if (textLower.contains("sorry") || textLower.contains("i'm sorry") ||
                (textLower.contains("interior") && textLower.contains("cloud city") && textLower.contains("site"))) {
                com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer sorryObjAnalyzer =
                    context.getObjectiveAnalyzer();
                if (sorryObjAnalyzer != null && sorryObjAnalyzer.isAnalyzed()
                    && sorryObjAnalyzer.needsBespinSystemPresence()) {
                    // Use DeckOracle to check if any CC interior sites remain in reserve
                    com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle sorryOracle = context.getDeckOracle();
                    boolean ccSitesInReserve = true; // default to true if oracle unavailable
                    if (sorryOracle != null && sorryOracle.isAnalyzed()) {
                        ccSitesInReserve = sorryOracle.isCardInReserve("Cloud City: Upper Walkway")
                            || sorryOracle.isCardInReserve("Cloud City: Carbonite Chamber")
                            || sorryOracle.isCardInReserve("Cloud City: Dining Room")
                            || sorryOracle.isCardInReserve("Cloud City: Lower Corridor")
                            || sorryOracle.isCardInReserve("Cloud City: Security Tower")
                            || sorryOracle.isCardInReserve("Cloud City: West Gallery")
                            || sorryOracle.isCardInReserve("Cloud City: North Corridor")
                            || sorryOracle.isCardInReserve("Cloud City: Platform")
                            || sorryOracle.isCardInReserve("Cloud City: Incinerator")
                            || sorryOracle.isCardInReserve("Cloud City: Guest Quarters")
                            || sorryOracle.isCardInReserve("Cloud City")  // partial match catches any CC site
                            ;
                        logger.warn("V24.6 I'M SORRY: CC interior sites still in reserve? {}", ccSitesInReserve);
                    }
                    if (ccSitesInReserve) {
                        action.addReasoning("V24.6 I'M SORRY: CC sites still in reserve — pull one NOW for more drains + occupation!", 250.0f);
                        logger.warn("V24.6 I'M SORRY: Boosting +250 — CC interior sites available in reserve!");
                    } else {
                        action.addReasoning("V24.6 I'M SORRY: All CC interior sites already pulled — search will fail!", -300.0f);
                        logger.warn("V24.6 I'M SORRY: BLOCKING — no more CC interior sites in reserve deck! (-300)");
                    }
                }
            }

            // ========== V29.7 WMAOP: STIPULATIONS REMOVED 2026-05-26 (Steve) ==========
            // Original V29.7 hardcoded "WMAOP is for Blockade Flagship site ONLY,"
            // penalizing all other modes -400 and adding -400 for "BFS already on
            // table" + -500 for "no locations in reserve." That worked for one
            // specific Blockade-Flagship-themed deck but blocked WMAOP universally
            // in any other deck that had it (Podracer interrupt mode, Effect pull
            // mode, or location pull mode targeting non-BFS sites).
            //
            // Steve's directive: "This logic must work for all decks regardless of
            // objective. Remove the stipulations so it fires for all decks."
            //
            // Current behavior: V29.7 no longer adds any scoring for WMAOP. Generic
            // rules carry the load:
            //   - V100 LOCATION PULL BEFORE CHARACTERS: +1500 when WMAOP's location-
            //     pull mode fires during deploy phase with chars/vehicles in hand.
            //   - V67ai TIERED LOCATION DEPLOY ORDER: scales by source category.
            //   - V67ak KEY-CHARACTER PULL: scales by named-persona match.
            //   - V60 RESERVE PULL: +150 generic for any reserve pull.
            //
            // If a deck-specific WMAOP rule is needed later (e.g., Blockade-themed
            // decks should prefer location mode), reintroduce here with DeckOracle
            // gating instead of hardcoding card titles.

            // ========== V29 / V67u: FORCE PUSH — BATTLE USE ONLY ==========
            // Force Push has two modes:
            //   1. BATTLE: "use 2 Force to target your Dark Jedi and opponent's character...
            //      Both targets are excluded from battle" — GOOD, removes threat
            //   2. FORCE PILE EXCHANGE: "Exchange two cards from hand with any one card
            //      from Force Pile" — BAD, especially in DRAW PHASE: you'd draw those
            //      cards anyway, and you're trading 2 hand cards for 1.
            //
            // V67u FIX (Steve, 2026-05-03): The OLD V29 check was `textLower.contains("force push")`
            // — but the action text for the exchange is just "Exchange cards with card in
            // Force Pile" which does NOT contain "force push". So V29 never fired and Rando
            // happily played the exchange during draw phase, wasting Force.
            //
            // New V67u: detect by SOURCE CARD title (when cardId resolvable) OR by action
            // text mentioning "force pile" + "exchange" (which uniquely identifies this
            // wasteful action regardless of source).
            String v67uSourceTitle = null;
            if (cardId != null && gameState != null) {
                try {
                    PhysicalCard srcPc = gameState.findCardById(Integer.parseInt(cardId));
                    if (srcPc != null && srcPc.getTitle() != null) {
                        v67uSourceTitle = srcPc.getTitle().toLowerCase(java.util.Locale.ROOT);
                    }
                } catch (Exception e) { /* ignore */ }
            }
            boolean v67uIsForcePushSource = v67uSourceTitle != null
                && v67uSourceTitle.contains("force push");
            boolean v67uIsExchangeAction = textLower.contains("exchange")
                && (textLower.contains("force pile") || textLower.contains("hand"));
            boolean v67uIsBattleAction = textLower.contains("exclude") && textLower.contains("battle");

            if (textLower.contains("force push") || v67uIsForcePushSource) {
                if (v67uIsBattleAction && !v67uIsExchangeAction) {
                    action.addReasoning("V29 FORCE PUSH: Battle exclusion — remove threat! Good use.", 80.0f);
                    logger.info("V29 FORCE PUSH: Battle use — exclude characters from battle (+80)");
                } else if (v67uIsExchangeAction) {
                    action.addReasoning("V67u FORCE PUSH BLOCK: Exchange w/ Force Pile is WASTE — those cards come to hand on draw anyway. NEVER play during draw phase!",
                        -500.0f);
                    logger.warn("V67u FORCE PUSH BLOCKED: '{}' source='{}' — exchange is waste, especially in draw phase (-500)",
                        actionText, v67uSourceTitle);
                }
            }
            // V67u: catch source-detected Force Push exchange even when neither outer
            // condition matched (defense in depth)
            else if (v67uIsForcePushSource && v67uIsExchangeAction) {
                action.addReasoning("V67u FORCE PUSH BLOCK (source-detect): exchange action from Force Push — waste!",
                    -500.0f);
                logger.warn("V67u FORCE PUSH BLOCKED (source): '{}' from {} — wasted force (-500)",
                    actionText, v67uSourceTitle);
            }

            // ═══════════════════════════════════════════════════════════
            // ═══ REGION: DEPLOY-3 — weapon-pull criteria gate (reorg 2026-07-06) ═══
            // Owns: V120 universal weapon-pull criteria block (-9999 when no in-play character satisfies the weapon's
            // OWN matching filter; V125 contains() fix + 2026-06-29 strict-match fix folded in). Deliberately SEPARATE
            // from the V185 oracle-side attach gate in DeckOracle — keep both.
            // Absorbs (dead, commented below/nearby — revert path, do not delete): none.
            // Cross-refs: DEPLOY-3 (V158/V115/V67aq in DeployEvaluator), SVC-ORACLE (V185). See resources/RANDO_REORG_PLAN_2026-07-02.md §3 + Rando_Section_Manifest_2026-07-06.xlsx.
            // ═══════════════════════════════════════════════════════════
            // ========== V120 (Steve, 2026-05-22): UNIVERSAL WEAPON-PULL CRITERIA BLOCK ==========
            // Per Steve: "We need to hard block deploy from reserve deck or with an
            // interrupt when a character already has a weapon."
            //
            // V115 closed the hand-deploy gap via V67aq criteria-awareness in DeployEvaluator.
            // V120 closes the FIFTH gap that the four-way one-weapon stack still missed:
            // Effect/Interrupt/Objective top-level actions that deploy a weapon FROM RESERVE
            // (e.g. "Deploy Vader's Lightsaber from Reserve Deck using •I Am Your Father (V)").
            // These score in ActionTextEvaluator and never reach V67aq, V70, V67ar, or V115.
            //
            // Logic: parse the weapon's title from the action text, find its blueprint anywhere
            // in Rando's known cards (hand/reserve/used/lost/table — gameState.getAllPermanentCards
            // covers all of these), extract its deploy criteria via the V70 helper, and count
            // criteria-matching armed/unarmed friendlies. Block when matchingUnarmed == 0.
            // Hunt Down replay ig4n5m5nzc4gronn: Rando fired IAYF four times trying to pull
            // Vader's Lightsaber from reserve while Vader was already armed with two Dark Jedi
            // Lightsabers (V115 was added the same session). Each attempt revealed the reserve.
            if (textLower.contains("from reserve") && actionText != null) {
                try {
                    // Parse weapon title from "Deploy <NAME> from Reserve" pattern
                    java.util.regex.Matcher v120m = java.util.regex.Pattern.compile(
                        "(?i)deploy\\s+([\\w'\\.\\(\\) -]+?)\\s+from\\s+reserve"
                    ).matcher(actionText);
                    if (v120m.find()) {
                        String v120WeaponName = v120m.group(1).trim();
                        // Strip leading bullet/dot markers
                        v120WeaponName = v120WeaponName.replaceAll("^[•·∙\\.]+\\s*", "").trim();
                        // V125 (Steve, 2026-05-22): V120 EXACT-MATCH BUG FIX — use contains() not equals().
                        // V120's original equals() comparison silently failed when the action text
                        // says "Vader's Lightsaber" but the actual card title is "•Darth Vader's
                        // Lightsaber (V)" (uniqueness bullet + Darth prefix + (V) suffix). Replay
                        // liuorncol0ku2qva 2026-05-22 confirmed: V120 never logged for IAYF's
                        // weapon-pull attempt. Switch to bidirectional contains() match — title
                        // contains action-text-name OR action-text-name contains title (handles
                        // both abbreviated and prefixed titles).
                        SwccgCardBlueprint v120WeaponBp = null;
                        if (gameState != null) {
                            String v120WeaponLower = v120WeaponName.toLowerCase(java.util.Locale.ROOT);
                            for (PhysicalCard pc : gameState.getAllPermanentCards()) {
                                if (pc == null || pc.getBlueprint() == null) continue;
                                if (!context.getPlayerId().equals(pc.getOwner())) continue;
                                if (pc.getBlueprint().getCardCategory() != CardCategory.WEAPON) continue;
                                String pTitle = pc.getTitle();
                                if (pTitle == null) continue;
                                String pTitleLower = pTitle.toLowerCase(java.util.Locale.ROOT);
                                // V120 FIX (#1, Steve 2026-06-29): the loose "title contains parsed-name"
                                // match caught a CHARACTER pull whose name sits inside a weapon title —
                                // "Deploy Vader from Reserve Deck" parses "vader", and "darth vader's
                                // lightsaber".contains("vader") = true, so the Vader CHARACTER pull was
                                // mis-blocked as a weapon pull (-9999), losing Steve the Hunt Down game.
                                // A real weapon pull names the WEAPON (its noun, e.g. "...lightsaber");
                                // a character pull names only the owner ("vader"). So for the loose
                                // direction require the parsed name to cover the weapon title's last
                                // significant word (the noun), not just the owner portion. The exact /
                                // parsed-name-contains-full-title directions (the V125 abbreviated/
                                // prefixed-title cases) are unchanged.
                                boolean v120Match;
                                if (pTitleLower.equals(v120WeaponLower)
                                        || v120WeaponLower.contains(pTitleLower)) {
                                    v120Match = true;
                                } else if (pTitleLower.contains(v120WeaponLower)) {
                                    String v120TitleCore = pTitleLower
                                        .replaceAll("\\([^)]*\\)", " ").replaceAll("\\s+", " ").trim();
                                    String v120Noun = v120TitleCore.contains(" ")
                                        ? v120TitleCore.substring(v120TitleCore.lastIndexOf(' ') + 1)
                                        : v120TitleCore;
                                    v120Match = v120Noun.length() >= 4
                                        && v120WeaponLower.contains(v120Noun);
                                } else {
                                    v120Match = false;
                                }
                                if (v120Match) {
                                    v120WeaponBp = pc.getBlueprint();
                                    break;
                                }
                            }
                        }
                        if (v120WeaponBp != null) {
                            String v120Criteria = com.gempukku.swccgo.ai.models.chosenone.evaluators
                                .CardSelectionEvaluator.v70ExtractDeployCriteria(v120WeaponBp.getGameText());
                            if (v120Criteria != null) {
                                int v120MatchArmed = 0, v120MatchUnarmed = 0;
                                for (PhysicalCard tc : gameState.getAllPermanentCards()) {
                                    if (tc == null || tc.getBlueprint() == null) continue;
                                    if (!context.getPlayerId().equals(tc.getOwner())) continue;
                                    if (tc.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                                    com.gempukku.swccgo.common.Zone tz = tc.getZone();
                                    if (tz == null || !tz.isInPlay()) continue;
                                    boolean v120Match = false;
                                    try {
                                        v120Match = com.gempukku.swccgo.ai.models.chosenone.evaluators
                                            .CardSelectionEvaluator.v70CharacterMatchesCriteria(
                                                game, gameState, tc, v120Criteria);
                                    } catch (Exception ignore) { /* false */ }
                                    if (!v120Match) continue;
                                    boolean v120Armed = false;
                                    java.util.List<PhysicalCard> v120Atts = gameState.getAttachedCards(tc);
                                    if (v120Atts != null) {
                                        for (PhysicalCard a : v120Atts) {
                                            if (a != null && a.getBlueprint() != null
                                                    && a.getBlueprint().getCardCategory() == CardCategory.WEAPON) {
                                                v120Armed = true;
                                                break;
                                            }
                                        }
                                    }
                                    if (v120Armed) v120MatchArmed++; else v120MatchUnarmed++;
                                }
                                if (v120MatchUnarmed == 0) {
                                    String v120Why = v120MatchArmed > 0
                                        ? String.format("every '%s' friendly (%d) already armed",
                                            v120Criteria, v120MatchArmed)
                                        : String.format("no '%s' friendly on table — deploy will fail",
                                            v120Criteria);
                                    action.addReasoning(
                                        "V120 WEAPON-PULL BLOCK: '" + v120WeaponName + "' — "
                                            + v120Why + " (will reveal reserve)",
                                        -9999.0f);
                                    logger.warn("V120 WEAPON-PULL BLOCK: '{}' (weapon '{}', criteria '{}') matchArmed={} matchUnarmed={} → HARD BLOCK (-9999)",
                                        actionText, v120WeaponName, v120Criteria, v120MatchArmed, v120MatchUnarmed);
                                }
                            }
                        }
                    }
                } catch (Exception e120) {
                    logger.debug("V120 weapon-pull check error: {}", e120.getMessage());
                }
            }

            // ========== V29.8: IAYF VADER-ON-TABLE CHECK (ANY SOURCE) ==========
            // IAYF can deploy Vader's Lightsaber from RESERVE or LOST PILE.
            // The reserve-only check below misses the Lost Pile case.
            // This broader check catches both: if source is IAYF and action involves
            // lightsaber, Vader MUST be on table.
            if (textLower.contains("lightsaber") && cardId != null && gameState != null) {
                try {
                    PhysicalCard iaySourceCard = gameState.findCardById(Integer.parseInt(cardId));
                    if (iaySourceCard != null && iaySourceCard.getTitle() != null
                        && iaySourceCard.getTitle().toLowerCase(java.util.Locale.ROOT).contains("i am your father")) {
                        com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer iayObj = context.getObjectiveAnalyzer();
                        boolean vaderPresent = iayObj != null && iayObj.isVaderOnTable(gameState, context.getPlayerId());
                        if (!vaderPresent) {
                            action.addReasoning("V29.8 IAYF: Vader NOT on table — can't deploy lightsaber from ANY source!", -500.0f);
                            logger.warn("V29.8 IAYF BLOCKED: Vader not on table — lightsaber deploy from {} impossible!",
                                textLower.contains("lost") ? "Lost Pile" : "Reserve/other");
                        }
                    }
                } catch (Exception iayE) {
                    logger.debug("V29.8: Error checking IAYF vader: {}", iayE.getMessage());
                }
            }

            // ========== V29.8: SENSE & UNCERTAIN — BLOCK REDRAW HAND USAGE ==========
            // Sense & Uncertain Is The Future has two functions:
            //   1. As Sense: cancel an opponent's interrupt (GOOD — save for this!)
            //   2. As Uncertain: make each player redraw hand (TERRIBLE — helps opponent too,
            //      costs 3 Force, loses cards currently in hand, is a Lost Interrupt)
            // Rando must NEVER use the redraw hand function. Save Sense for defense.
            if (textLower.contains("redraw") && textLower.contains("hand")) {
                action.addReasoning("V29.8 SENSE REDRAW BLOCKED: NEVER redraw hand — save Sense for canceling opponent interrupts! Costs 3 Force AND helps opponent!", -600.0f);
                logger.warn("V29.8 SENSE REDRAW BLOCKED: Attempted to redraw hand — massive penalty (-600)");
            }
            // Also catch the "make each player" variant
            if (textLower.contains("each player") && (textLower.contains("redraw") || textLower.contains("shuffle"))) {
                action.addReasoning("V29.8 SENSE UNCERTAIN BLOCKED: Don't make both players redraw — helps opponent!", -600.0f);
                logger.warn("V29.8 SENSE UNCERTAIN BLOCKED: Attempted mutual redraw — massive penalty (-600)");
            }

            // ========== V29.7: UNIVERSAL RESERVE DECK PULL VALIDATION ==========
            // PROBLEM: Many cards produce GENERIC action texts like "Deploy card from Reserve Deck"
            // or "Take card into hand from Reserve Deck". The V25 checks looked for card names
            // like "crush the rebellion" in action text — but those names were NEVER in the text!
            // FIX: Look up the SOURCE CARD via cardId to identify what's generating the action,
            // then check DeckOracle for valid targets based on the source card's identity.
            if (textLower.contains("from reserve") && cardId != null && gameState != null) {
                String sourceTitle = null;
                try {
                    PhysicalCard sourceCard = gameState.findCardById(Integer.parseInt(cardId));
                    if (sourceCard != null && sourceCard.getTitle() != null) {
                        sourceTitle = sourceCard.getTitle();
                    }
                } catch (Exception e) { /* ignore parse errors */ }

                if (sourceTitle != null) {
                    String sourceLower = sourceTitle.toLowerCase(java.util.Locale.ROOT);
                    com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle pullOracle = context.getDeckOracle();

                    // --- CRUSH THE REBELLION: pulls I Have You Now or Evader ---
                    if (sourceLower.contains("crush") && sourceLower.contains("rebellion")) {
                        if (pullOracle != null && pullOracle.isAnalyzed()) {
                            boolean hasTarget = pullOracle.isCardInReserve("I Have You Now")
                                || pullOracle.isCardInReserve("Evader");
                            if (!hasTarget) {
                                action.addReasoning("V29.7 CRUSH: No I Have You Now or Evader in reserve — WILL FAIL!", -400.0f);
                                logger.warn("V29.7 CRUSH BLOCKED: No targets in reserve (source: {})", sourceTitle);
                            }
                            // V29.9: Check if IHYN/Evader already in hand — don't pull duplicates!
                            boolean ihynInHand = pullOracle.isCardInHand("I Have You Now");
                            boolean evaderInHand = pullOracle.isCardInHand("Evader");
                            boolean ihynInReserve = pullOracle.isCardInReserve("I Have You Now");
                            boolean evaderInReserve = pullOracle.isCardInReserve("Evader");
                            if (ihynInHand && evaderInHand) {
                                // Both targets already in hand — this pull is useless
                                action.addReasoning("V29.9 CRUSH DUPLICATE: Both IHYN and Evader already in hand — pulling another is wasteful!", -300.0f);
                                logger.warn("V29.9 CRUSH DUPLICATE: Both targets in hand — blocking (-300)");
                            } else if (ihynInHand && !evaderInReserve) {
                                // IHYN in hand and no Evader in reserve — would pull a second IHYN
                                action.addReasoning("V29.9 CRUSH DUPLICATE: IHYN already in hand, no Evader in reserve — save Crush!", -250.0f);
                                logger.warn("V29.9 CRUSH DUPLICATE: IHYN in hand, no Evader in reserve — blocking (-250)");
                            } else if (evaderInHand && !ihynInReserve) {
                                // Evader in hand and no IHYN in reserve — would pull a second Evader
                                action.addReasoning("V29.9 CRUSH DUPLICATE: Evader already in hand, no IHYN in reserve — save Crush!", -250.0f);
                                logger.warn("V29.9 CRUSH DUPLICATE: Evader in hand, no IHYN in reserve — blocking (-250)");
                            }
                        }
                    }

                    // --- I AM YOUR FATHER: deploys Vader's Lightsaber ---
                    // V35.8: IAYF can pull from Reserve Deck (free) OR Lost Pile (lose 1 Force).
                    // Both should score EXTREMELY high when Vader is on table unarmed.
                    // The Lost Pile retrieval is a KEY mechanic of Hunt Down — Vader throws
                    // his lightsaber every battle, then retrieves it for the next battle.
                    else if (sourceLower.contains("i am your father")) {
                        com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer objA = context.getObjectiveAnalyzer();
                        boolean vaderOnTable = objA != null && objA.isVaderOnTable(gameState, context.getPlayerId());

                        if (!vaderOnTable && textLower.contains("lightsaber")) {
                            action.addReasoning("V29.7 IAYF: Vader NOT on table — can't deploy lightsaber!", -500.0f);
                            logger.warn("V29.7 IAYF BLOCKED: Vader not on table");
                        } else if (vaderOnTable && textLower.contains("lightsaber")) {
                            // V37: USE DECKORACLE to check WHERE the lightsaber actually is!
                            // IAYF can pull from Reserve Deck (free) or Lost Pile (lose 1 Force).
                            // The action text tells us which zone — don't try Reserve if it's in Lost.
                            boolean pullFromReserve = textLower.contains("reserve");
                            boolean pullFromLost = textLower.contains("lost");

                            boolean saberInReserve = false;
                            boolean saberInLost = false;
                            try {
                                com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle iayOracle = context.getDeckOracle();
                                if (iayOracle != null && iayOracle.isAnalyzed()) {
                                    saberInReserve = iayOracle.isCardInReserve("Darth Vader's Lightsaber");
                                    saberInLost = iayOracle.isCardLost("Darth Vader's Lightsaber");
                                    logger.info("V37 IAYF ZONE CHECK: saber in reserve={}, in lost={}, action={}",
                                        saberInReserve, saberInLost, pullFromReserve ? "RESERVE" : pullFromLost ? "LOST" : "UNKNOWN");
                                }
                            } catch (Exception e) { /* ignore */ }

                            // V37: Block if trying to pull from wrong zone
                            if (pullFromReserve && !saberInReserve) {
                                action.addReasoning("V37 IAYF: Lightsaber NOT in Reserve Deck — WILL FAIL! Check Lost Pile instead.", -600.0f);
                                logger.warn("V37 IAYF BLOCKED: Trying reserve but saber not there! (in lost={})", saberInLost);
                            } else if (pullFromLost && !saberInLost) {
                                action.addReasoning("V37 IAYF: Lightsaber NOT in Lost Pile — check Reserve instead.", -400.0f);
                                logger.warn("V37 IAYF BLOCKED: Trying lost pile but saber not there! (in reserve={})", saberInReserve);
                            } else {
                                // Lightsaber IS in the target zone — check if Vader is armed
                                boolean vaderArmed = false;
                                try {
                                    String iayPid = context.getPlayerId();
                                    for (PhysicalCard tc : gameState.getAllPermanentCards()) {
                                        if (tc == null || !iayPid.equals(tc.getOwner())) continue;
                                        if (tc.getBlueprint() == null) continue;
                                        String tcTitle = tc.getTitle() != null ? tc.getTitle().toLowerCase(Locale.ROOT) : "";
                                        if (!tcTitle.contains("vader")) continue;
                                        if (tc.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                                        com.gempukku.swccgo.common.Zone tcZ = tc.getZone();
                                        if (tcZ == null || !tcZ.isInPlay()) continue;
                                        java.util.List<PhysicalCard> atts = gameState.getAttachedCards(tc);
                                        if (atts != null) {
                                            for (PhysicalCard att : atts) {
                                                if (att != null && att.getBlueprint() != null
                                                    && att.getBlueprint().getCardCategory() == CardCategory.WEAPON) {
                                                    vaderArmed = true;
                                                    break;
                                                }
                                            }
                                        }
                                        break;
                                    }
                                } catch (Exception e) { /* ignore */ }

                                if (!vaderArmed) {
                                    action.addReasoning(String.format(
                                        "V37 IAYF: Vader UNARMED — retrieve lightsaber from %s NOW!",
                                        pullFromLost ? "Lost Pile" : "Reserve"), 600.0f);
                                    logger.warn("V37 IAYF: Vader unarmed, saber in {} — TOP PRIORITY (+600)",
                                        pullFromLost ? "Lost" : "Reserve");
                                } else {
                                    action.addReasoning("V35.8 IAYF: Vader armed — spare lightsaber retrieval", 50.0f);
                                }
                            }
                        }
                    }

                    // --- YOU ARE BEATEN: pulls IAYF or specific card from reserve ---
                    else if (sourceLower.contains("you are beaten")) {
                        if (pullOracle != null && pullOracle.isAnalyzed()) {
                            boolean hasIAYF = pullOracle.isCardInReserve("I Am Your Father");
                            if (!hasIAYF) {
                                action.addReasoning("V29.7 YOU ARE BEATEN: No I Am Your Father in reserve — WILL FAIL!", -400.0f);
                                logger.warn("V29.7 YOU ARE BEATEN BLOCKED: No IAYF in reserve (source: {})", sourceTitle);
                            }
                        }
                    }

                    // --- BLAST POINTS: pulls Ghhhk or Hyperwave Scan ---
                    else if (sourceLower.contains("blast points")) {
                        if (pullOracle != null && pullOracle.isAnalyzed()) {
                            boolean hasTarget = pullOracle.isCardInReserve("Ghhhk")
                                || pullOracle.isCardInReserve("Hyperwave Scan");
                            if (!hasTarget) {
                                action.addReasoning("V29.7 BLAST POINTS: No Ghhhk or Hyperwave Scan in reserve — WILL FAIL!", -400.0f);
                                logger.warn("V29.7 BLAST POINTS BLOCKED: No targets in reserve (source: {})", sourceTitle);
                            }
                        }
                    }

                    // --- HUNT DOWN (objective): deploys location from reserve ---
                    else if (sourceLower.contains("hunt down") && textLower.contains("location")) {
                        if (pullOracle != null && pullOracle.isAnalyzed()) {
                            java.util.List<com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle.DeckCard> locsInReserve =
                                pullOracle.getCardsByCategory(com.gempukku.swccgo.common.CardCategory.LOCATION,
                                    com.gempukku.swccgo.common.Zone.RESERVE_DECK);
                            if (locsInReserve.isEmpty()) {
                                action.addReasoning("V29.7 HUNT DOWN: No locations left in reserve — WILL FAIL!", -400.0f);
                                logger.warn("V29.7 HUNT DOWN BLOCKED: No locations in reserve (source: {})", sourceTitle);
                            }
                        }
                    }

                    // --- IMPERIAL COMMAND: pulls admiral or general ---
                    else if (sourceLower.contains("imperial command")) {
                        if (pullOracle != null && pullOracle.isAnalyzed()) {
                            boolean hasTarget = pullOracle.hasTargetInReserve("admiral", "general");
                            if (!hasTarget) {
                                action.addReasoning("V29.7 IMPERIAL COMMAND: No admirals/generals in reserve — WILL FAIL!", -400.0f);
                                logger.warn("V29.7 IMPERIAL COMMAND BLOCKED: No targets in reserve (source: {})", sourceTitle);
                            }
                        }
                    }

                    // --- ENDOR SHIELD: pulls admiral ---
                    else if (sourceLower.contains("endor shield")) {
                        if (pullOracle != null && pullOracle.isAnalyzed()) {
                            boolean hasTarget = pullOracle.hasTargetInReserve("admiral");
                            if (!hasTarget) {
                                action.addReasoning("V29.7 ENDOR SHIELD: No admirals in reserve — WILL FAIL!", -400.0f);
                                logger.warn("V29.7 ENDOR SHIELD BLOCKED: No admirals in reserve (source: {})", sourceTitle);
                            }
                        }
                    }

                    // --- VISAGE OF THE EMPEROR: pulls lightsaber ---
                    else if (sourceLower.contains("visage") && textLower.contains("lightsaber")) {
                        if (pullOracle != null && pullOracle.isAnalyzed()) {
                            boolean hasTarget = pullOracle.hasTargetInReserve("lightsaber");
                            if (!hasTarget) {
                                action.addReasoning("V29.7 VISAGE: No lightsabers in reserve — WILL FAIL!", -400.0f);
                                logger.warn("V29.7 VISAGE BLOCKED: No lightsabers in reserve (source: {})", sourceTitle);
                            }
                        }
                    }

                    // --- KIR KANOS: pulls Royal Guard ---
                    else if (sourceLower.contains("kir kanos")) {
                        if (pullOracle != null && pullOracle.isAnalyzed()) {
                            boolean hasTarget = pullOracle.hasTargetInReserve("royal guard", "kanos", "kyneugh");
                            if (!hasTarget) {
                                action.addReasoning("V29.7 KIR KANOS: No Royal Guards in reserve — WILL FAIL!", -400.0f);
                                logger.warn("V29.7 KIR KANOS BLOCKED: No Royal Guards in reserve (source: {})", sourceTitle);
                            }
                        }
                    }

                    // === V37: UNIVERSAL RESERVE SEARCH SAFETY NET ===
                    // Any "from reserve" action that wasn't caught by a specific rule above
                    // should still be cautious. Failed searches give opponent free deck intel.
                    // If DeckOracle shows reserve deck is very small, penalize searches
                    // because they reveal more information proportionally.
                    if (pullOracle != null && pullOracle.isAnalyzed()) {
                        java.util.List<com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle.DeckCard> reserveCards =
                            pullOracle.getCardsInZone(com.gempukku.swccgo.common.Zone.RESERVE_DECK);
                        if (reserveCards.size() <= 3) {
                            action.addReasoning("V37 RESERVE INTEL RISK: Only " + reserveCards.size() +
                                " cards in reserve — search reveals almost everything to opponent!", -200.0f);
                            logger.warn("V37 RESERVE RISK: {} cards in reserve — search gives opponent too much intel (-200)",
                                reserveCards.size());
                        } else if (reserveCards.size() <= 8) {
                            action.addReasoning("V37 RESERVE CAUTION: " + reserveCards.size() +
                                " cards in reserve — opponent will see deck composition", -50.0f);
                        }
                    }
                }
            }

            // ========== V24.9: MASTERFUL MOVE EARLY-GAME GUARD ==========
            // Masterful Move searches reserve for Ghhhk (damage cancel combo card).
            // On turns 1-3, force should go to deploying Executor + characters, NOT searching for Ghhhk.
            // Only play Masterful Move when characters are on the table and need protecting.
            if (textLower.contains("masterful move")) {
                int mmTurn = context.getTurnNumber();
                boolean hasCharsOnTable = false;
                if (gameState != null) {
                    try {
                        for (PhysicalCard loc : gameState.getLocationsInOrder()) {
                            java.util.List<PhysicalCard> cardsHere = gameState.getCardsAtLocation(loc);
                            if (cardsHere != null) {
                                for (PhysicalCard c : cardsHere) {
                                    if (c != null && context.getPlayerId().equals(c.getOwner()) &&
                                        c.getBlueprint() != null &&
                                        c.getBlueprint().getCardCategory() == com.gempukku.swccgo.common.CardCategory.CHARACTER) {
                                        hasCharsOnTable = true;
                                        break;
                                    }
                                }
                            }
                            if (hasCharsOnTable) break;
                        }
                    } catch (Exception e) {
                        logger.debug("V24.9 MM guard: Error scanning for characters: {}", e.getMessage());
                    }
                }
                if (!hasCharsOnTable) {
                    action.addReasoning("V24.9 MASTERFUL MOVE: No characters on table — Ghhhk has nothing to protect! Save force for deployment!", -500.0f);
                    logger.warn("V24.9 MASTERFUL MOVE: BLOCKED — no characters on table, save force for Executor! (-500)");
                } else if (mmTurn <= 2) {
                    action.addReasoning("V24.9 MASTERFUL MOVE: Too early (turn " + mmTurn + ") — prioritize getting Executor out!", -300.0f);
                    logger.warn("V24.9 MASTERFUL MOVE: Penalized on turn {} — save force for Executor deployment! (-300)", mmTurn);
                }
            }

            // ========== Capacity Slot Selection (Pilot vs Passenger) ==========
            if (textLower.contains("capacity slot")) {
                if (textLower.contains("pilot capacity slot")) {
                    action.setScore(100.0f);
                    action.addReasoning("Pilot slot adds power to ship!", 100.0f);
                    action.setActionType(ActionType.MOVE);
                    logger.info("PILOT SLOT: Strongly preferring pilot capacity (+100)");
                } else if (textLower.contains("passenger capacity slot")) {
                    action.setScore(VERY_BAD_DELTA);
                    action.addReasoning("Passenger gives NO power bonus!", VERY_BAD_DELTA);
                    action.setActionType(ActionType.MOVE);
                    logger.warn("PASSENGER SLOT: Penalizing - no power contribution ({})", VERY_BAD_DELTA);
                }
                actions.add(action);
                continue;
            }

            // ═══════════════════════════════════════════════════════════
            // ═══ REGION: SETUP — saga choice (reorg 2026-07-06) ═══
            // Owns: V29.15 The Force Is Strong In My Family saga pick keyed by deck name
            // (+1000 correct / -500 wrong / +500 default to I Have It when no deck name).
            // Absorbs (dead, commented below/nearby — revert path, do not delete): none.
            // Cross-refs: SETUP (CardSelectionEvaluator turn-0 block), PLAYBOOKS (V54/V61-saga deck scripts). See resources/RANDO_REORG_PLAN_2026-07-02.md §3 + Rando_Section_Manifest_2026-07-06.xlsx.
            // ═══════════════════════════════════════════════════════════
            // ========== V29.15 Epic Event Saga Choice ==========
            // "The Force Is Strong In My Family" presents choices:
            //   "My Father Has It", "I Have It", "You Have That Power, Too"
            // The correct choice depends on the deck name:
            //   Luke deck → "I Have It"
            //   Anakin deck → "My Father Has It"
            //   Rey deck → "You Have That Power, Too"
            if (textLower.contains("i have it") || textLower.contains("my father has it")
                || textLower.contains("you have that power")) {
                String deckName = context.getDeckName();
                String deckLower = (deckName != null) ? deckName.toLowerCase(java.util.Locale.ROOT) : "";
                boolean isCorrectChoice = false;

                if (deckLower.contains("luke") && textLower.contains("i have it")
                    && !textLower.contains("my father has it")) {
                    isCorrectChoice = true;
                } else if (deckLower.contains("anakin") && textLower.contains("my father has it")) {
                    isCorrectChoice = true;
                } else if (deckLower.contains("rey") && textLower.contains("you have that power")) {
                    isCorrectChoice = true;
                }

                if (isCorrectChoice) {
                    action.addReasoning("V29.15 EPIC EVENT: Correct saga choice for '" + deckName + "' deck!", 1000.0f);
                    logger.warn("V29.15 EPIC EVENT: Choosing '{}' — correct for deck '{}'", actionText, deckName);
                } else if (!deckLower.isEmpty()) {
                    action.addReasoning("V29.15 EPIC EVENT: Wrong saga choice for '" + deckName + "' deck", -500.0f);
                    logger.warn("V29.15 EPIC EVENT: Penalizing '{}' — wrong for deck '{}'", actionText, deckName);
                } else {
                    // No deck name available — default to "I Have It" (most common Luke deck)
                    if (textLower.contains("i have it") && !textLower.contains("my father has it")) {
                        action.addReasoning("V29.15 EPIC EVENT: Default to 'I Have It' (no deck name)", 500.0f);
                        logger.warn("V29.15 EPIC EVENT: No deck name — defaulting to 'I Have It'");
                    }
                }
                actions.add(action);
                continue;
            }

            // ========== Force Activation ==========
            if (actionText.equals("Activate Force")) {
                action.setActionType(ActionType.ACTIVATE_FORCE);
                try {
                    evaluateActivateForce(action, context);
                } catch (Exception e) {
                    // V29.13: NEVER skip activation due to exceptions.
                    // Default to high score so Rando always activates Force.
                    logger.warn("V29.13: Exception in evaluateActivateForce, defaulting to ACTIVATE: {}", e.getMessage());
                    action.addReasoning("V29.13 SAFE DEFAULT: Always activate Force", VERY_GOOD_DELTA);
                }
            }

            // ========== V53b: STACK JEDI HERE — Save Jedi Survivors ==========
            // Fallen Order lets you lose 1 force to stack a Jedi Survivor back on it,
            // saving them from being lost. ALWAYS do this — losing 1 force to save a
            // Jedi is the best trade in the game. They can redeploy next turn.
            else if (textLower.contains("stack") && textLower.contains("here")
                     && (textLower.contains("jedi") || textLower.contains("obi-wan")
                         || textLower.contains("quinlan") || textLower.contains("kelleran")
                         || textLower.contains("cal kestis") || textLower.contains("ezra")
                         || textLower.contains("ahsoka") || textLower.contains("cere")
                         || textLower.contains("sabine") || textLower.contains("luke"))) {
                action.addReasoning("V53b SAVE JEDI: Stack Jedi on Fallen Order — lose 1 force to save them!", 500.0f);
                logger.warn("V53b SAVE JEDI: '{}' — +500, always save Jedi Survivors!", actionText);
            }

            // ========== V53: BLOCK WOKLING EFFECT SEARCH ==========
            // Wokling (V) costs 3 Force to search for an Effect from Reserve Deck.
            // This wastes force — the search often fails (no valid targets) and even
            // when it succeeds, 3 force is better spent deploying characters.
            // Block Wokling from searching for effects entirely.
            else if (textLower.contains("effect") && textLower.contains("reserve deck")
                     && textLower.contains("deploy cost")) {
                // Check if source card is Wokling
                boolean isWokling = textLower.contains("wokling");
                if (!isWokling && cardId != null && gameState != null) {
                    try {
                        PhysicalCard wokSrc = gameState.findCardById(Integer.parseInt(cardId));
                        if (wokSrc != null && wokSrc.getTitle() != null
                            && wokSrc.getTitle().toLowerCase(Locale.ROOT).contains("wokling")) {
                            isWokling = true;
                        }
                    } catch (Exception e) { /* ignore */ }
                }
                if (isWokling) {
                    action.addReasoning("V53 BLOCK WOKLING: Don't waste 3 force searching for effects!", -9999.0f);
                    logger.warn("V53 WOKLING BLOCKED: Wokling Effect search — 3 force wasted, HARD BLOCK!");
                } else {
                    action.addReasoning("Search for Effect from Reserve Deck", GOOD_DELTA);
                }
            }

            // ========== Force Drain ==========
            else if (actionText.equals("Force drain")) {
                action.setActionType(ActionType.FORCE_DRAIN);
                evaluateForceDrain(action, context, cardId);
            }

            // ========== Race Destiny ==========
            else if (actionText.equals("Draw race destiny")) {
                action.setActionType(ActionType.RACE_DESTINY);
                action.addReasoning("Race destiny always high priority", VERY_GOOD_DELTA);
            }

            // ========== Play a Card ==========
            // V29.1: If the source card is Knowledge And Defense (V), this is a shield play.
            // Apply shield pacing — play 2 shields on turn 1, hold the rest to scout opponent.
            else if (actionText.equals("Play a card")) {
                action.setActionType(ActionType.PLAY_CARD);
                // V129 (Steve, 2026-05-24): Renamed isKnDShieldPlay → isStackedPileShieldPlay
                // and expanded detection to include AFA (Anger, Fear, Aggression — light
                // side equivalent of K&D, same stacked-pile mechanic). Symmetric with
                // chosenone — both bots now apply V102 (activation cap) and V124 (4th-slot
                // hard-block) regardless of which stacked-pile source they are running.
                boolean isStackedPileShieldPlay = false;
                if (cardId != null && gameState != null) {
                    try {
                        PhysicalCard sourceCard = gameState.findCardById(Integer.parseInt(cardId));
                        if (sourceCard != null) {
                            String sourceTitle = sourceCard.getTitle();
                            if (sourceTitle != null
                                    && (sourceTitle.toLowerCase().contains("knowledge and defense")
                                        || sourceTitle.toLowerCase().contains("anger, fear, aggression"))) {
                                isStackedPileShieldPlay = true;
                            }
                        }
                    } catch (Exception e) {
                        // Ignore — fall through to generic evaluation
                    }
                }
                if (isStackedPileShieldPlay) {
                    com.gempukku.swccgo.ai.models.chosenone.strategy.ShieldStrategy shieldStrat = context.getShieldStrategy();
                    int turnNum = context.getTurnNumber();
                    // === V124 (Steve, 2026-05-22): HARD-BLOCK K&D PARENT ACTION AT 4TH SLOT ===
                    // Real incident: replay liuorncol0ku2qva (2026-05-22). All 4 shields
                    // deployed by turn 2 via K&D, even though V105/V107 in the sub-decision
                    // path correctly hard-blocked all 4th-slot candidates at -5000. Problem:
                    // the parent "Play a card" action scored +50 ("slots available"), so the
                    // AI committed to playing K&D, then the sub-decision was FORCED to pick
                    // the least-bad shield from the stack (Resistance at -5050).
                    // V124 blocks at the PARENT action: count friendly shields on table; if
                    // 3+ AND ShieldStrategy.prefers4thSlot() returns null (no V105/V107
                    // trigger), hard-block "Play a card" with -3000 so the AI never starts
                    // the sub-decision.
                    int v124ShieldsOnTable = 0;
                    boolean v124HasV105V107Trigger = false;
                    try {
                        GameState v124Gs = gameState;
                        String v124Pid = context.getPlayerId();
                        if (v124Gs != null && v124Pid != null) {
                            for (PhysicalCard sc : v124Gs.getAllPermanentCards()) {
                                if (sc == null || sc.getBlueprint() == null) continue;
                                if (!v124Pid.equals(sc.getOwner())) continue;
                                if (sc.getBlueprint().getCardCategory() != CardCategory.DEFENSIVE_SHIELD) continue;
                                com.gempukku.swccgo.common.Zone sz = sc.getZone();
                                if (sz == null || !sz.isInPlay()) continue;
                                v124ShieldsOnTable++;
                            }
                            if (v124ShieldsOnTable >= 3 && shieldStrat != null) {
                                String v124Preferred = shieldStrat.prefers4thSlot(
                                    v124Gs, context.getGame(), v124Pid);
                                v124HasV105V107Trigger = (v124Preferred != null);
                            }
                        }
                    } catch (Exception e) {
                        logger.debug("V124 shield-count check error: {}", e.getMessage());
                    }
                    if (v124ShieldsOnTable >= 3 && !v124HasV105V107Trigger) {
                        action.addReasoning(
                            "V124 K&D 4TH-SLOT BLOCK: " + v124ShieldsOnTable
                                + " shields already on table, no V105/V107 trigger — don't activate K&D for 4th shield",
                            -3000.0f);
                        logger.warn("V124 K&D 4TH-SLOT BLOCK: {} shields on table, no trigger active — parent action blocked",
                            v124ShieldsOnTable);
                    }
                    // V102 (Steve, 2026-05-20): K&D ACTIVATION CAP — hard block beyond per-turn cap.
                    // Replaces the previous -40 soft-pace penalty (which Rando still overrode).
                    // shieldsAllowedThisTurn returns 2 turn 1, 3 turn 2, 4 turn 3+ — beyond this
                    // we MUST stop K&D activations or we burn the stack on weak shields.
                    if (shieldStrat != null && shieldStrat.atKnDActivationCap(turnNum)) {
                        action.addReasoning(
                            "V102 K&D ACTIVATION CAP: " + shieldStrat.knDActivationsThisTurn(turnNum)
                                + " activations already this turn (turn " + turnNum + ") — hold remaining",
                            -2000.0f);
                        logger.warn("V102 K&D ACTIVATION CAP: turn {} count {} — hard block",
                            turnNum, shieldStrat.knDActivationsThisTurn(turnNum));
                    } else if (shieldStrat != null && shieldStrat.atPacingCap(turnNum)) {
                        action.addReasoning("V29.1 K&D SHIELD PACING: Holding shield slot — scout opponent first (turn " + turnNum + ")", -40.0f);
                    } else {
                        action.addReasoning("K&D: Play defensive shield (slots available)", VERY_GOOD_DELTA);
                    }
                } else {
                    evaluatePlayCard(action, context);
                }
            }

            // ═══════════════════════════════════════════════════════════
            // ═══ REGION: BATTLE-2 — weapons-segment window (reorg 2026-07-06) ═══
            // Owns: the weapons-segment dispatch head: fire-before-throw (V29.12 fire +300 must beat throw's 200-250),
            // Add Battle Destiny, V29.10 lightsaber throw. The wider battle-interrupt suite (V35.x hatred lifecycle,
            // V144, V155, V175, V67u Force Push) sits scattered ABOVE in this file — same section, one owner.
            // KIND mix (BATTLE-2 overall): 11 VETO / 5 BANDED / 2 ORDERING.
            // Absorbs (dead, commented below/nearby — revert path, do not delete): none.
            // Cross-refs: BATTLE-1 (BattleEvaluator + this file's V25 power-tier block — the SUM is the
            // behavior), TARGETING (V36 weapon targeting in CardSelectionEvaluator). See resources/RANDO_REORG_PLAN_2026-07-02.md §3 + Rando_Section_Manifest_2026-07-06.xlsx.
            // ═══════════════════════════════════════════════════════════
            // ========== Fire Weapons ==========
            // V29.12: Fire MUST score higher than throw (250) so Rando fires the
            // lightsaber BEFORE throwing it. Throwing sacrifices the weapon (places it
            // in Lost Pile), so if throw happens first, fire becomes impossible.
            // Fire first = hit target + THEN throw for attrition destiny = double trouble.
            else if (actionText.contains("Fire")) {
                action.setActionType(ActionType.FIRE_WEAPON);
                // Check if there are valid (non-HIT) targets before firing
                // Ported from Python action_text_evaluator.py - don't fire at already-hit targets
                boolean hasValidTargets = checkForValidWeaponTargets(context);
                if (hasValidTargets) {
                    if (context.getPhase() == Phase.BATTLE) {
                        // V29.12: In battle, fire weapons BEFORE throw — score must beat throw's 200
                        action.addReasoning("V29.12 FIRE WEAPON: Fire FIRST in battle — hit target before throwing!", 300.0f);
                        logger.warn("V29.12 FIRE WEAPON: Battle phase fire — must happen before throw (+300)");
                    } else {
                        action.addReasoning("Firing weapons at valid targets", VERY_GOOD_DELTA);
                    }
                } else {
                    action.addReasoning("All targets already HIT - save weapon", BAD_DELTA);
                    logger.debug("Skipping weapon fire - no valid (unhit) targets");
                }
            }

            // ========== Add Battle Destiny ==========
            else if (textLower.contains("add") && textLower.contains("battle destiny")) {
                action.setActionType(ActionType.BATTLE_DESTINY);
                action.addReasoning("Adding battle destiny is great", VERY_GOOD_DELTA);
            }

            // ========== V29.10/V29.12: LIGHTSABER THROW — ADD DESTINY TO ATTRITION ==========
            // After firing a lightsaber, Vader can also 'throw' it to add destiny to attrition.
            // This is a SEPARATE action from firing — both can be done in the same battle.
            // The throw adds extra attrition damage which can be decisive.
            // Action text: "'Throw' to add destiny to attrition"
            //
            // V29.12 CRITICAL: Throw MUST score LOWER than Fire (300).
            // Throwing places the lightsaber in Lost Pile — if Rando throws first,
            // he can NEVER fire it. The correct sequence is:
            //   1. FIRE lightsaber at target (hit them, reduce forfeit) — score 300
            //   2. THROW lightsaber (sacrifice it for attrition destiny) — score 200
            // This gives "double trouble" — hit + extra attrition in the same battle.
            if (textLower.contains("throw") && textLower.contains("add destiny to attrition")) {
                if (context.getPhase() == Phase.BATTLE) {
                    action.addReasoning("V29.12 LIGHTSABER THROW: Add destiny to attrition — do AFTER firing!", 200.0f);
                    logger.warn("V29.12 LIGHTSABER THROW: Battle phase throw (+200, below fire's +300)");
                } else {
                    action.addReasoning("V29.10 LIGHTSABER THROW: Throw lightsaber to add destiny to attrition!", 150.0f);
                }
            }

            // ========== V29.10: HATRED CARD — CANCEL OPPONENT GAME TEXT ==========
            // Stacking a Hatred Card on an opponent's character cancels their game text.
            // This is CRITICAL because it removes attrition immunity and other protections.
            // Without Hatred, winning a battle does NOTHING if opponent is immune to attrition.
            // Action text variants:
            //   "Stack a 'Hatred Card'" (previous game)
            //   "USED: Stack 'Hatred' card on opponent's character" (this game)
            // BEST TIMING: Deploy phase — stack Hatred BEFORE initiating battle.
            // This way opponent's immunities are already gone when battle starts.
            if (textLower.contains("hatred")) {
                // V37.1: Only place hatred on OUR turn — placing during opponent's turn
                // wastes it because we can't follow up with a battle this turn.
                if (gameState != null && !context.isMyTurn()) {
                    action.addReasoning("V37.1 HATRED: Not our turn — save hatred for our deploy phase!", -600.0f);
                    logger.warn("V37.1 HATRED: Opponent's turn — blocking hatred placement (-600)");
                } else {

                String decisionText = context.getDecisionText() != null
                    ? context.getDecisionText().toLowerCase(Locale.ROOT) : "";
                boolean isDeployPhase = context.getPhase() == Phase.DEPLOY
                    || decisionText.contains("deploy");
                boolean isBattlePhase = context.getPhase() == Phase.BATTLE
                    || decisionText.contains("battle") || decisionText.contains("weapons segment");

                // V35.3: STRICT hatred scoring — ONLY place hatred when Vader or Inquisitor
                // is at the SAME SITE as an opponent character. No proactive/remote hatred.
                boolean v35VaderOrInqWithOpponents = false;
                boolean v35InqOnTable = false;
                boolean v35JediAtSameSite = false;
                try {
                    if (gameState != null) {
                        String v35Pid = context.getPlayerId();
                        String v35Oid = gameState.getOpponent(v35Pid);
                        for (PhysicalCard tCard : gameState.getAllPermanentCards()) {
                            if (tCard == null || !v35Pid.equals(tCard.getOwner())) continue;
                            if (tCard.getBlueprint() == null) continue;
                            if (tCard.getBlueprint().getCardCategory() != com.gempukku.swccgo.common.CardCategory.CHARACTER) continue;
                            com.gempukku.swccgo.common.Zone tz = tCard.getZone();
                            if (tz == null || !tz.isInPlay()) continue;
                            String tTitle = tCard.getTitle() != null ? tCard.getTitle().toLowerCase(Locale.ROOT) : "";
                            // V35.7: Hatred requires INQUISITOR only (NOT Vader alone).
                            // The card "There Are Many Hunting You Now" requires "your Inquisitor"
                            // at the same location. Vader alone cannot use hatred.
                            if (isInquisitor(tTitle)) {
                                v35InqOnTable = true;
                                PhysicalCard charLoc = tCard.getAtLocation();
                                if (charLoc != null) {
                                    float oppPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                        gameState, charLoc, v35Oid, false, false);
                                    if (oppPower > 0) {
                                        v35VaderOrInqWithOpponents = true;
                                        for (PhysicalCard lc : gameState.getCardsAtLocation(charLoc)) {
                                            if (lc == null || !v35Oid.equals(lc.getOwner())) continue;
                                            String lcT = lc.getTitle() != null ? lc.getTitle().toLowerCase(Locale.ROOT) : "";
                                            if (isJediOrPadawan(lcT)) { v35JediAtSameSite = true; break; }
                                        }
                                    }
                                }
                                if (v35VaderOrInqWithOpponents) break;
                            }
                        }
                    }
                } catch (Exception e) { /* ignore */ }

                if (!v35InqOnTable) {
                    // V35.7: No Inquisitor on table — hatred requires Inquisitor, BLOCK
                    action.addReasoning("V35.7 HATRED: No Inquisitor on table — hatred requires Inquisitor!", -500.0f);
                    logger.warn("V35.7 HATRED: No Inquisitor — hard block (-500)");
                } else if (v35VaderOrInqWithOpponents) {
                    // V35.7: Inquisitor AT SAME SITE as opponent — hatred is useful!
                    float hatredScore = isDeployPhase ? (float) RandoConfig.SCORE_HATRED_WITH_INQUISITOR : 350.0f;
                    if (v35JediAtSameSite) hatredScore += 150.0f;
                    action.addReasoning(String.format(
                        "V35.7 HATRED: Inquisitor WITH opponents%s — cancel game text! (+%.0f)",
                        v35JediAtSameSite ? " + JEDI" : "", hatredScore), hatredScore);
                    logger.warn("V35.7 HATRED: Inquisitor with opponents (jedi={}) — score +{}",
                        v35JediAtSameSite, (int)hatredScore);
                } else {
                    // V35.3: Vader/Inquisitor NOT at same site as any opponent — DON'T waste hatred
                    action.addReasoning("V35.3 HATRED: Vader/Inquisitor not at same site as opponents — save for later!", -300.0f);
                    logger.warn("V35.3 HATRED: No Vader/Inq co-located with opponents — blocked (-300)");
                }
            } // end V37.1 isMyTurn else block
            }

            // ========== V29.9: I HAVE YOU NOW — PLAY DURING BATTLE ==========
            // IHYN adds extra battle destiny draws when Vader is in the battle.
            // This is DEVASTATING — 2-3 extra destiny draws can turn any battle into a win.
            // Must be played DURING a battle. Check if we're in battle phase and Vader is present.
            // Also catch "i have you now" in source card check for generic action texts.
            if (textLower.contains("i have you now") || textLower.contains("ihyn")) {
                if (context.getPhase() == Phase.BATTLE) {
                    // In battle — check if Vader is participating
                    boolean vaderInBattle = false;
                    try {
                        if (gameState != null && gameState.getBattleState() != null) {
                            PhysicalCard battleLoc = gameState.getBattleState().getBattleLocation();
                            if (battleLoc != null) {
                                String ihynPlayerId = context.getPlayerId();
                                for (PhysicalCard bCard : gameState.getCardsAtLocation(battleLoc)) {
                                    if (bCard == null || !ihynPlayerId.equals(bCard.getOwner())) continue;
                                    if (bCard.getTitle() != null && bCard.getTitle().toLowerCase(java.util.Locale.ROOT).contains("vader")) {
                                        vaderInBattle = true;
                                        break;
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.debug("V29.9 IHYN: Error checking Vader in battle: {}", e.getMessage());
                    }

                    if (vaderInBattle) {
                        action.addReasoning("V29.9 IHYN: Vader in battle — PLAY I HAVE YOU NOW for devastating extra destiny draws!", 300.0f);
                        logger.warn("V29.9 IHYN: Vader in battle — mega boost (+300) for I Have You Now!");
                    } else {
                        // Still good even without Vader — adds destiny draws
                        action.addReasoning("V29.9 IHYN: Play I Have You Now for extra battle destiny!", 100.0f);
                        logger.info("V29.9 IHYN: Playing during battle without Vader (+100)");
                    }
                } else {
                    // Not in battle — save IHYN for when we need it
                    action.addReasoning("V29.9 IHYN: Save I Have You Now for battle!", -200.0f);
                    logger.info("V29.9 IHYN: Not in battle — save for later (-200)");
                }
            }
            // Also check source card for IHYN when action text is generic
            else if (context.getPhase() == Phase.BATTLE && cardId != null && gameState != null) {
                try {
                    PhysicalCard ihynSource = gameState.findCardById(Integer.parseInt(cardId));
                    if (ihynSource != null && ihynSource.getTitle() != null
                        && ihynSource.getTitle().toLowerCase(java.util.Locale.ROOT).contains("i have you now")) {
                        action.addReasoning("V29.9 IHYN: Play I Have You Now during battle — extra destiny draws!", 200.0f);
                        logger.warn("V29.9 IHYN (source): I Have You Now detected via source card — boost +200");
                    }
                } catch (Exception e) { /* ignore */ }
            }

            // ========== V35: FAR MORE FRIGHTENING THAN DEATH ==========
            // FMFTD has two modes:
            // USED: Stack hatred on opponent's leader/ability>3 at battleground
            // LOST: Add 1-2 battle destiny if Inquisitor with Jedi/Padawan/Hatred
            // Detect via testingTexts or action text containing "far more frightening"
            if (textLower.contains("far more frightening") || textLower.contains("fmftd")) {
                boolean isFmftdBattle = context.getPhase() == Phase.BATTLE;
                boolean isFmftdUsedMode = textLower.contains("stack") || (textLower.contains("hatred") && !textLower.contains("destiny"));
                boolean isFmftdLostMode = textLower.contains("destiny") || textLower.contains("add");

                if (isFmftdLostMode && isFmftdBattle) {
                    // LOST mode during battle — check for Inquisitor + Jedi + Hatred synergy
                    boolean v35FmInq = false;
                    boolean v35FmJedi = false;
                    boolean v35FmHatred = false;
                    try {
                        if (gameState != null && gameState.getBattleState() != null) {
                            PhysicalCard fmBattleLoc = gameState.getBattleState().getBattleLocation();
                            if (fmBattleLoc != null) {
                                String fmPid = context.getPlayerId();
                                String fmOid = gameState.getOpponent(fmPid);
                                for (PhysicalCard bc : gameState.getCardsAtLocation(fmBattleLoc)) {
                                    if (bc == null) continue;
                                    String bcTitle = bc.getTitle() != null ? bc.getTitle().toLowerCase(Locale.ROOT) : "";
                                    if (fmPid.equals(bc.getOwner()) && isInquisitor(bcTitle)) v35FmInq = true;
                                    if (fmOid != null && fmOid.equals(bc.getOwner())) {
                                        if (isJediOrPadawan(bcTitle)) v35FmJedi = true;
                                        java.util.List<PhysicalCard> st = gameState.getStackedCards(bc);
                                        if (st != null && !st.isEmpty()) v35FmHatred = true;
                                    }
                                }
                            }
                        }
                    } catch (Exception e) { /* ignore */ }

                    int synCount = (v35FmInq ? 1 : 0) + (v35FmJedi ? 1 : 0) + (v35FmHatred ? 1 : 0);
                    if (synCount >= 3) {
                        action.addReasoning("V35 FMFTD LOST: Inquisitor + Jedi + Hatred — ADD 2 BATTLE DESTINY!", (float) RandoConfig.SCORE_FMFTD_FULL_SYNERGY);
                        logger.warn("V35 FMFTD: Full synergy! +{}", RandoConfig.SCORE_FMFTD_FULL_SYNERGY);
                    } else if (synCount >= 2) {
                        action.addReasoning("V35 FMFTD LOST: Inquisitor with Jedi or Hatred — add 1 battle destiny!", 350.0f);
                    } else if (v35FmInq) {
                        action.addReasoning("V35 FMFTD LOST: Inquisitor in battle — add destiny!", 200.0f);
                    } else {
                        action.addReasoning("V35 FMFTD LOST: No Inquisitor in battle — limited value", 50.0f);
                    }
                } else if (isFmftdUsedMode) {
                    // USED mode — place hatred card
                    if (context.getPhase() == Phase.DEPLOY || context.getPhase() == Phase.MOVE) {
                        action.addReasoning("V35 FMFTD USED: Place hatred on opponent — cancel game text!", 350.0f);
                    } else {
                        action.addReasoning("V35 FMFTD USED: Place hatred — decent timing", 150.0f);
                    }
                } else if (isFmftdBattle) {
                    // Generic FMFTD during battle — likely the LOST mode
                    action.addReasoning("V35 FMFTD: Play during battle for extra destiny!", 250.0f);
                } else {
                    action.addReasoning("V35 FMFTD: Save for battle if possible", -100.0f);
                }
            }

            // ========== V35: VADER SELF-RECALL (Hunt Down V once-per-game) ==========
            // "Take Vader into hand" — allows redeploying Vader to hunt Jedi elsewhere
            // "Return an Inquisitor here to hand" — Eighth Brother repositioning
            else if (textLower.contains("take vader into hand") || textLower.contains("return") && textLower.contains("inquisitor") && textLower.contains("hand")) {
                if (textLower.contains("vader")) {
                    // Vader self-recall — check if there are Jedi elsewhere to hunt
                    boolean v35JediElsewhere = false;
                    try {
                        if (gameState != null) {
                            String v35Oid = gameState.getOpponent(context.getPlayerId());
                            for (PhysicalCard loc : gameState.getTopLocations()) {
                                if (loc == null) continue;
                                for (PhysicalCard c : gameState.getCardsAtLocation(loc)) {
                                    if (c == null || !v35Oid.equals(c.getOwner())) continue;
                                    String ct = c.getTitle() != null ? c.getTitle().toLowerCase(Locale.ROOT) : "";
                                    if (isJediOrPadawan(ct)) { v35JediElsewhere = true; break; }
                                }
                                if (v35JediElsewhere) break;
                            }
                        }
                    } catch (Exception e) { /* ignore */ }

                    if (v35JediElsewhere) {
                        action.addReasoning("V35 VADER RECALL: Take Vader into hand — Jedi elsewhere to hunt! Redeploy!", 300.0f);
                        logger.warn("V35 VADER RECALL: Jedi detected elsewhere — recalling Vader to redeploy (+300)");
                    } else {
                        action.addReasoning("V35 VADER RECALL: Take Vader into hand — no clear target, keep him deployed", -100.0f);
                    }
                } else {
                    // V35.1: Inquisitor recall — DON'T recall if opponents are nearby!
                    // Eighth Brother's ability returns an Inquisitor to hand. Only do this
                    // if there are NO opponents at adjacent sites. If opponents are nearby,
                    // keep the Inquisitor to fight!
                    boolean opponentsNearby = false;
                    try {
                        if (gameState != null) {
                            String recallPid = context.getPlayerId();
                            String recallOid = gameState.getOpponent(recallPid);
                            for (PhysicalCard loc : gameState.getTopLocations()) {
                                if (loc == null) continue;
                                float oppPwr = game.getModifiersQuerying().getTotalPowerAtLocation(
                                    gameState, loc, recallOid, false, false);
                                if (oppPwr > 0) { opponentsNearby = true; break; }
                            }
                        }
                    } catch (Exception e) { /* ignore */ }

                    if (opponentsNearby) {
                        action.addReasoning("V35.1 INQUISITOR RECALL BLOCK: Opponents on the board — KEEP Inquisitor to fight!", -400.0f);
                        logger.warn("V35.1 INQUISITOR RECALL BLOCKED: Opponents present — don't pull back (-400)");
                    } else {
                        action.addReasoning("V35 INQUISITOR RECALL: No opponents on board — safe to reposition", 100.0f);
                    }
                }
            }

            // ========== V37.2: STUNNING LEADER — DEFENSIVE ONLY ==========
            // Stunning Leader excludes characters from battle. Good when DEFENDING
            // against a stronger opponent (saves Vader from certain death).
            // BAD when WE initiated (we started the fight to WIN).
            else if (textLower.contains("stunning leader") || textLower.contains("exclude") && textLower.contains("from battle")) {
                if (context.getPhase() == Phase.BATTLE && gameState != null) {
                    try {
                        com.gempukku.swccgo.game.state.BattleState bState = gameState.getBattleState();
                        if (bState != null) {
                            String slPlayerId = context.getPlayerId();
                            String slInitiator = bState.getPlayerInitiatedBattle();
                            boolean weInitiated = slPlayerId != null && slPlayerId.equals(slInitiator);

                            if (weInitiated) {
                                // WE started this battle — NEVER cancel our own attack!
                                action.addReasoning("V37.2 STUNNING LEADER: WE initiated — fight to WIN!", -9999.0f);
                                logger.warn("V37.2 STUNNING LEADER: HARD BLOCK — we initiated this battle!");
                            } else {
                                // Opponent initiated — check if we're outmatched
                                PhysicalCard slBattleLoc = bState.getBattleLocation();
                                if (slBattleLoc != null) {
                                    String slOpp = gameState.getOpponent(slPlayerId);
                                    float slOurPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                        gameState, slBattleLoc, slPlayerId, false, false);
                                    float slTheirPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                        gameState, slBattleLoc, slOpp, false, false);
                                    if (slTheirPower > slOurPower * 1.5f) {
                                        // Badly outmatched — Stunning Leader saves our characters!
                                        action.addReasoning(String.format(
                                            "V37.2 STUNNING LEADER: Outmatched %.0f vs %.0f — exclude to survive!",
                                            slOurPower, slTheirPower), 300.0f);
                                        logger.warn("V37.2 STUNNING LEADER: Defensive use — saving characters from {} vs {}",
                                            (int)slOurPower, (int)slTheirPower);
                                    } else {
                                        // Close fight — fight it out instead of excluding
                                        action.addReasoning("V37.2 STUNNING LEADER: Close fight — battle instead!", -300.0f);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.debug("V37.2 STUNNING LEADER: Error: {}", e.getMessage());
                    }
                } else {
                    action.addReasoning("V37.2 STUNNING LEADER: Not in battle — save!", -200.0f);
                }
            }

            // ========== V35.4: YOU ARE BEATEN — DON'T WASTE ON UNDERCOVER SPIES ==========
            // You Are Beaten targets opponent characters. But undercover spies appear on OUR side
            // and aren't valid targets for combat effects. Don't waste this interrupt.
            // Also: only use during battle or when it will lead to meaningful attrition.
            else if (textLower.contains("you are beaten")) {
                if (context.getPhase() == Phase.BATTLE) {
                    action.addReasoning("V35.4 YOU ARE BEATEN: During battle — use for attrition!", 150.0f);
                } else {
                    // Outside battle — this is usually a waste
                    action.addReasoning("V35.4 YOU ARE BEATEN: Not in battle — save for combat!", -200.0f);
                    logger.info("V35.4 YOU ARE BEATEN: Not in battle — penalizing (-200)");
                }
            }

            // ========== Battle Destiny Modifier (+1 to battle destiny) ==========
            else if ((actionText.contains("+1") || actionText.contains("+ 1") || textLower.contains("add 1"))
                     && textLower.contains("battle destiny")) {
                action.setActionType(ActionType.BATTLE_DESTINY);
                action.addReasoning("+1 to battle destiny - always use!", VERY_GOOD_DELTA);
            }

            // ========== V24.2: Force Drain Modifier (+1 to force drain) ==========
            // Cards like Lord Maul With Lightsaber add +1 to force drain as an optional response.
            // This should ALWAYS be accepted — free extra damage!
            else if ((actionText.contains("+1") || actionText.contains("+ 1") || textLower.contains("add 1"))
                     && textLower.contains("force drain")) {
                action.setActionType(ActionType.FORCE_DRAIN);
                action.addReasoning("V24.2 FORCE DRAIN BONUS: +1 to force drain — always use!", VERY_GOOD_DELTA + 30.0f);
                logger.warn("V24.2 DRAIN BONUS: Accepting +1 force drain — '{}'", actionText);
            }

            // ========== Weapon Destiny Modifier ==========
            else if (textLower.contains("weapon destiny") &&
                     (actionText.contains("+3") || actionText.contains("+2") || textLower.contains("add"))) {
                action.setActionType(ActionType.FIRE_WEAPON);
                action.addReasoning("Boost weapon destiny - increases hit chance!", VERY_GOOD_DELTA);
            }

            // ========== Protect Battle Destiny Draws ==========
            else if (textLower.contains("prevent") && textLower.contains("cancel") &&
                     textLower.contains("battle destiny") && textLower.contains("draw")) {
                action.setActionType(ActionType.BATTLE_DESTINY);
                evaluateDestinyProtection(action, context);
            }

            // ========== Prevent Opponent Adding Battle Destiny ==========
            else if (textLower.contains("prevent") && textLower.contains("battle destiny") &&
                     !textLower.contains("cancel")) {
                action.setActionType(ActionType.BATTLE_DESTINY);
                action.addReasoning("Prevent opponent battle destiny - denies their draw!", VERY_GOOD_DELTA);
            }

            // ========== Take Admiral/General Into Hand ==========
            // For TDIGWATT/Bespin objectives: an Imperial admiral pulled here is likely
            // a pilot (e.g., Admiral Chiraneau). That pilot enables deploying the Executor
            // to Bespin cheaply — the Executor + pilot simultaneous deploy is the critical
            // Turn 1 play for Cloud City objectives. Prioritise VERY highly when we have
            // no ship at Bespin yet.
            else if (textLower.contains("take") && textLower.contains("into hand") &&
                     (textLower.contains("admiral") || textLower.contains("general"))) {
                // Check if we're running a Bespin/Cloud City objective with no ship there yet
                boolean bespinChainActive = false;
                try {
                    com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer objAnalyzer =
                        context.getObjectiveAnalyzer();
                    if (objAnalyzer != null && objAnalyzer.isAnalyzed() &&
                        objAnalyzer.needsBespinSystemPresence()) {
                        // Check if we already have a ship at Bespin system
                        boolean hasBespinShip = false;
                        if (gameState != null) {
                            String pid = context.getPlayerId();
                            for (com.gempukku.swccgo.game.PhysicalCard loc : gameState.getLocationsInOrder()) {
                                if (loc != null && loc.getTitle() != null &&
                                    loc.getTitle().toLowerCase(java.util.Locale.ROOT).contains("bespin") &&
                                    loc.getBlueprint() != null &&
                                    loc.getBlueprint().getCardSubtype() ==
                                        com.gempukku.swccgo.common.CardSubtype.SYSTEM) {
                                    float ourPower = context.getGame().getModifiersQuerying()
                                        .getTotalPowerAtLocation(gameState, loc, pid, false, false);
                                    if (ourPower > 0) hasBespinShip = true;
                                    break;
                                }
                            }
                        }
                        if (!hasBespinShip) {
                            bespinChainActive = true;
                        }
                    }
                } catch (Exception e) {
                    // Ignore — fall back to default scoring
                }

                // V29.7: Check if there are actually admirals/generals left in Reserve
                boolean hasValidTarget = true;
                try {
                    com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle oracle = context.getDeckOracle();
                    if (oracle != null && oracle.isAnalyzed()) {
                        hasValidTarget = oracle.hasTargetInReserve("admiral", "general");
                        if (!hasValidTarget) {
                            logger.warn("V29.7 PULL CHECK: No admirals/generals left in Reserve — blocking pull!");
                        }
                    }
                } catch (Exception pullCheckE) {
                    // Can't check — assume target exists
                }

                if (!hasValidTarget) {
                    // No valid targets in Reserve — don't waste Force on this!
                    action.addReasoning("V29.7 NO TARGET: No admirals/generals in Reserve Deck — skip!", -300.0f);
                } else if (bespinChainActive) {
                    // Admiral pilot → Executor chain: this is Turn 1 critical for TDIGWATT.
                    // Score it as high as AMSD itself so we never skip this pull.
                    action.addReasoning(
                        "CRITICAL: Admiral pilot enables Executor deploy to Bespin — must pull T1!", 300.0f);
                    logger.warn("EXECUTOR CHAIN: Admiral pull with no Bespin ship — boosting to 300 (enables Executor pipeline)");
                } else {
                    // V29.7: Pulls ALWAYS fire before locations and characters.
                    // Getting cards into hand first means better deploy choices later.
                    action.addReasoning("V29.7 PULL FIRST: Retrieve admiral/general into hand before deploying!", 250.0f);
                }
            }

            // ========== V175: KILL SHOT — "Make <character> lost" ==========
            // Steve (live ROTS Dooku games): "Rando almost never uses interrupts
            // offensively during battle." Log forensics: the engine offered FIVE
            // kill-shots on Steve's characters ("Make Yoda lost", "Make Rey lost",
            // "Make Ben Solo lost", "Make Anakin lost", "Make Han lost" — the
            // Sniper / Dark Strike / 'hit'-follow-up class) and ALL FIVE scored 0.0
            // "Unknown action type" and lost to Pass (7-16). Now: parse the target,
            // check ownership — an OPPONENT character scores a big kill-shot bonus
            // scaled by its power+forfeit; our OWN character (some windows list
            // self-targets / sacrifice modes) gets a small negative so Pass wins.
            else if (textLower.contains("make ") && textLower.endsWith(" lost")) {
                try {
                    int v175Mi = textLower.indexOf("make ");
                    int v175Li = textLower.lastIndexOf(" lost");
                    String v175Target = actionText.substring(v175Mi + 5, v175Li).trim();
                    com.gempukku.swccgo.game.state.GameState v175Gs = context.getGameState();
                    String v175Pid = context.getPlayerId();
                    PhysicalCard v175Found = null;
                    if (v175Gs != null && !v175Target.isEmpty()) {
                        for (PhysicalCard v175C : v175Gs.getAllPermanentCards()) {
                            if (v175C != null && v175C.getTitle() != null
                                    && v175C.getTitle().equalsIgnoreCase(v175Target)) {
                                v175Found = v175C; break;
                            }
                        }
                    }
                    if (v175Found != null && v175Pid != null
                            && !v175Pid.equals(v175Found.getOwner())) {
                        float v175Pow = 0f, v175Forf = 0f;
                        SwccgCardBlueprint v175Bp = v175Found.getBlueprint();
                        if (v175Bp != null) {
                            if (v175Bp.hasPowerAttribute() && v175Bp.getPower() != null) v175Pow = v175Bp.getPower();
                            if (v175Bp.hasForfeitAttribute() && v175Bp.getForfeit() != null) v175Forf = v175Bp.getForfeit();
                        }
                        float v175Score = Math.min(900f, 400f + v175Pow * 40f + v175Forf * 20f);
                        action.addReasoning(String.format(
                            "V175 KILL SHOT: make %s lost (power %.0f, forfeit %.0f) — take it!",
                            v175Target, v175Pow, v175Forf), v175Score);
                        logger.warn("V175 KILL SHOT: '{}' (pow={} forf={}) -> +{}",
                            v175Target, (int) v175Pow, (int) v175Forf, (int) v175Score);
                    } else if (v175Found != null) {
                        action.addReasoning("V175: target is OUR character — don't make our own lost", -100.0f);
                    } else {
                        action.addReasoning("V175: make-lost target not found on table — unknown", 0.0f);
                    }
                } catch (Exception v175E) {
                    logger.debug("V175 kill-shot parse error: {}", v175E.getMessage());
                }
            }

            // ========== Substitute Destiny ==========
            else if (textLower.contains("substitute destiny")) {
                action.setActionType(ActionType.SUBSTITUTE_DESTINY);
                // V175 (Steve, 2026-06): score the DELTA, not a flat +30. Welcome Home
                // substitutes Tyranus's ability 7 for a just-drawn destiny — brilliant
                // when the draw was a 1, a waste when it was a 6. The just-drawn card
                // sits in unresolved destiny draws (its printed destiny = drawn value);
                // the substitute value is approximated by our best ability in the
                // battle. delta*60 (a 6-point swing = +360 > Pass); non-positive delta
                // -> -50 (save the card). Falls back to the old flat +30 when either
                // value is unreadable.
                float v175Drawn = -1f, v175BestAb = -1f;
                try {
                    com.gempukku.swccgo.game.state.GameState v175SGs = context.getGameState();
                    String v175SPid = context.getPlayerId();
                    if (v175SGs != null && v175SPid != null) {
                        PhysicalCard v175DrawnCard = v175SGs.getTopOfUnresolvedDestinyDraws(v175SPid);
                        if (v175DrawnCard != null && v175DrawnCard.getBlueprint() != null
                                && v175DrawnCard.getBlueprint().getDestiny() != null) {
                            v175Drawn = v175DrawnCard.getBlueprint().getDestiny();
                        }
                        PhysicalCard v175BLoc = v175SGs.getBattleLocation();
                        if (v175BLoc != null) {
                            for (PhysicalCard v175BC : v175SGs.getCardsAtLocation(v175BLoc)) {
                                if (v175BC != null && v175SPid.equals(v175BC.getOwner())
                                        && v175BC.getBlueprint() != null
                                        && v175BC.getBlueprint().hasAbilityAttribute()
                                        && v175BC.getBlueprint().getAbility() != null) {
                                    v175BestAb = Math.max(v175BestAb, v175BC.getBlueprint().getAbility());
                                }
                            }
                        }
                    }
                } catch (Exception ignore) { }
                if (v175Drawn >= 0f && v175BestAb > 0f) {
                    float v175Delta = v175BestAb - v175Drawn;
                    if (v175Delta > 0f) {
                        action.addReasoning(String.format(
                            "V175 SUBSTITUTE DELTA: drawn %.0f -> ability %.0f (+%.0f gain)",
                            v175Drawn, v175BestAb, v175Delta), v175Delta * 60f);
                        logger.warn("V175 SUBSTITUTE: drawn={} bestAbility={} -> +{}",
                            (int) v175Drawn, (int) v175BestAb, (int) (v175Delta * 60f));
                    } else {
                        action.addReasoning(String.format(
                            "V175 SUBSTITUTE SKIP: drawn %.0f already >= ability %.0f — save the card",
                            v175Drawn, v175BestAb), -50.0f);
                    }
                } else {
                    action.addReasoning("Substituting destiny is good", GOOD_DELTA);
                }
            }

            // ========== React ==========
            else if (textLower.contains("react")) {
                action.setActionType(ActionType.REACT);
                action.addReasoning("Avoid reacts (bot doesn't understand timing)", BAD_DELTA);
            }

            // ========== Steal ==========
            else if (textLower.contains("steal")) {
                action.setActionType(ActionType.STEAL);
                action.addReasoning("Stealing is good", GOOD_DELTA);
            }

            // ========== Sabacc ==========
            else if (textLower.contains("play sabacc")) {
                action.setActionType(ActionType.SABACC);
                action.addReasoning("Playing sabacc", GOOD_DELTA);
            }

            // ========== Cancel Own Cards (Bad!) ==========
            else if (textLower.contains("cancel your")) {
                action.setActionType(ActionType.CANCEL);
                action.addReasoning("Never cancel own cards", VERY_BAD_DELTA);
            }

            // ========== Cancel Opponent's Interrupt (Sense/Control) ==========
            else if (textLower.contains("cancel") &&
                     (textLower.contains("interrupt") || textLower.contains("sense") ||
                      textLower.contains("alter") || textLower.contains("effect") ||
                      textLower.contains("force drain")) &&
                     !textLower.contains("your")) {
                action.setActionType(ActionType.CANCEL);
                evaluateSenseCancel(action, context, actionText);
            }

            // ========== V37: Cancel/Redraw Destiny — CHECK CURRENT VALUE FIRST ==========
            // Imperial Enforcement and similar cards cancel a destiny draw and cause a redraw.
            // Only use if the current destiny is LOW (< 3). A 6-destiny character draw is
            // essentially the best possible — NEVER cancel that.
            // Use DeckOracle average to decide if redraw is likely to improve.
            else if (textLower.contains("cancel") && textLower.contains("redraw") && textLower.contains("destiny")) {
                // Try to extract the current destiny value from the action text
                // Format often includes the drawn card name — check for high destiny numbers
                float currentDestinyDrawn = -1;
                try {
                    // The action text often says "cancel X's battle destiny draw of <CardName>"
                    // We can check DeckOracle for average destiny to decide
                    com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle redrawOracle = context.getDeckOracle();
                    double avgDest = 3.0;
                    if (redrawOracle != null && redrawOracle.isAnalyzed()) {
                        avgDest = redrawOracle.getAverageDestinyInReserve();
                    }

                    // Extract destiny number from action text if present (e.g., "draw of X as a 6")
                    java.util.regex.Matcher destMatcher = java.util.regex.Pattern.compile("as a (\\d+)").matcher(textLower);
                    if (destMatcher.find()) {
                        currentDestinyDrawn = Float.parseFloat(destMatcher.group(1));
                    }

                    if (currentDestinyDrawn >= 0) {
                        if (currentDestinyDrawn >= 3) {
                            // Good destiny draw — do NOT cancel! Average would likely be worse.
                            action.addReasoning(String.format(
                                "V37 DON'T REDRAW: Current destiny %.0f is GOOD (avg %.1f) — keep it!",
                                currentDestinyDrawn, avgDest), -300.0f);
                            logger.warn("V37 REDRAW BLOCKED: Destiny {} is >= 3 (avg {}) — don't cancel!",
                                (int)currentDestinyDrawn, String.format("%.1f", avgDest));
                        } else {
                            // Low destiny — redraw is likely to improve
                            action.addReasoning(String.format(
                                "V37 REDRAW: Current destiny %.0f is LOW (avg %.1f) — try for better!",
                                currentDestinyDrawn, avgDest), 100.0f);
                        }
                    } else {
                        // Couldn't determine current value — use average as guide
                        if (avgDest >= 3.5) {
                            action.addReasoning("Redraw destiny — good average in reserve", GOOD_DELTA);
                        } else {
                            action.addReasoning("Redraw destiny — risky, low average in reserve", -50.0f);
                        }
                    }
                } catch (Exception e) {
                    action.addReasoning("Redraw destiny", GOOD_DELTA);
                }
            }

            // ========== Cancel Weapon Targeting ==========
            else if (textLower.contains("cancel") && textLower.contains("weapon") && textLower.contains("target")) {
                action.setActionType(ActionType.CANCEL);
                action.addReasoning("Cancel weapon targeting - protect our characters!", VERY_GOOD_DELTA);
            }

            // ========== Immune to Attrition ==========
            else if (textLower.contains("immune to attrition")) {
                action.addReasoning("Make character immune to attrition - valuable protection!", VERY_GOOD_DELTA);
            }

            // ========== Protect Forfeit ==========
            else if (textLower.contains("forfeit") &&
                     (textLower.contains("protect") || textLower.contains("preserved"))) {
                action.addReasoning("Protect forfeit value during battle", GOOD_DELTA + 10.0f);
            }

            // ========== Re-target Weapon ==========
            else if (textLower.contains("re-target") || textLower.contains("retarget")) {
                action.addReasoning("Re-target weapon at enemy - turn their weapon against them!", VERY_GOOD_DELTA);
            }

            // ========== Cancel Battle Damage (Houjix/Ghhhk) ==========
            else if (actionText.contains("Cancel all remaining battle damage")) {
                action.setActionType(ActionType.CANCEL_DAMAGE);
                evaluateHoujixGhhhk(action, context);
            }

            // ========== V67af: RETURN-OWN-CHARACTER-TO-HAND BOUNCE BLOCK ==========
            // Steve's report: Rando deploys General Grievous, then uses Grievous's
            // 'Lose 1 Force to return Grievous to hand' game text to bounce him —
            // wasting both the deploy cost AND the bounce cost. V29.7 BOUNCE only
            // fires for 'Take X into hand' actions; Grievous and similar cards say
            // 'Return X to hand', which V29.7 misses entirely.
            //
            // Rule: when an action says 'Return <X> to hand' AND the source card is
            // a character we own AND the action requires losing force, hard-block.
            // The tactical use case (escape death) is too rare to justify Rando's
            // pattern of deploy-then-bounce loops.
            else if (textLower.contains("return") && textLower.contains("to hand")
                    && cardId != null && gameState != null && context.getPlayerId() != null) {
                boolean v67afBlock = false;
                String v67afDetail = null;
                try {
                    PhysicalCard srcPc = gameState.findCardById(Integer.parseInt(cardId));
                    if (srcPc != null && srcPc.getBlueprint() != null
                            && context.getPlayerId().equals(srcPc.getOwner())
                            && srcPc.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                        v67afBlock = true;
                        v67afDetail = srcPc.getTitle();
                    }
                } catch (Exception e) { /* ignore */ }
                if (v67afBlock) {
                    action.addReasoning(String.format(
                        "V67af RETURN-TO-HAND BLOCK: bouncing %s wastes deploy cost — DON'T undo your deploy!",
                        v67afDetail), -9999.0f);
                    logger.warn("V67af RETURN BLOCK: source={} action='{}' — HARD BLOCK (-9999)",
                        v67afDetail, actionText);
                } else {
                    // Default: still discourage but lighter touch (handles edge cases
                    // like opponent-effect-induced returns we haven't classified yet).
                    action.addReasoning("V67af RETURN-TO-HAND: unclassified return action — light penalty",
                        -150.0f);
                    logger.info("V67af RETURN-TO-HAND: '{}' source unclassifiable — -150",
                        actionText);
                }
            }

            // ========== V67an (Steve, 2026-05-07): WEAPON SWAP TO FREE MATCHING SLOT ==========
            //
            // Steve's rule: if Rando has a non-unique/non-matching weapon attached to
            // a character (e.g., generic Dark Jedi Lightsaber on Vader) AND has a
            // unique persona-matched weapon for that character in hand (e.g., Vader's
            // Lightsaber), Rando should TRANSFER the wrong weapon to a buddy at the
            // same site. After the transfer the matching character is unarmed, so the
            // V67ad two-weapon hard-block lifts and the matching unique weapon can
            // deploy on its persona — net result: 2 characters armed, persona bonuses
            // active for the matching weapon (immune, fire-for-free, +power, etc.).
            //
            // Detection: action text starts with "Transfer" (rules-level transfer) or
            // contains "Transfer device" / "Transfer weapon".
            //
            // Bonus +400 fires when:
            //   - The transfer source weapon is NOT unique OR has no matchingCharacter
            //     filter pointing at its current attachee
            //   - Rando has another weapon in hand whose matchingCharacter filter
            //     DOES target the current attachee (or whose title matches the persona)
            //
            // If we can't determine matchingCharacter unambiguously, fall back to
            // a milder +150 ('transfers usually mean tactical swap').
            else if (actionText.contains("Transfer")
                    && (actionText.contains("weapon") || actionText.contains("device")
                        || textLower.startsWith("transfer "))) {
                action.setActionType(ActionType.UNKNOWN);
                float v67anBonus = 150.0f;
                String v67anReason = "transfer action — usually a tactical swap";
                try {
                    if (cardId != null && gameState != null && context.getPlayerId() != null) {
                        PhysicalCard transferSrc = gameState.findCardById(Integer.parseInt(cardId));
                        if (transferSrc != null && transferSrc.getBlueprint() != null
                                && transferSrc.getBlueprint().getCardCategory() == CardCategory.WEAPON) {
                            // Identify current attachee
                            PhysicalCard attachee = transferSrc.getAttachedTo();
                            if (attachee != null && attachee.getBlueprint() != null
                                    && context.getPlayerId().equals(attachee.getOwner())) {
                                String attacheeTitleLower = attachee.getTitle() != null
                                    ? attachee.getTitle().toLowerCase(java.util.Locale.ROOT) : "";
                                // Is the current weapon non-unique OR not matched to attachee?
                                boolean weaponIsNonUnique = transferSrc.getBlueprint().getUniqueness()
                                    != com.gempukku.swccgo.common.Uniqueness.UNIQUE;
                                boolean weaponMatchesAttachee = false;
                                try {
                                    com.gempukku.swccgo.filters.Filter mcFilter =
                                        transferSrc.getBlueprint().getMatchingCharacterFilter();
                                    if (mcFilter != null) {
                                        weaponMatchesAttachee = mcFilter.accepts(context.getGame(), attachee);
                                    }
                                } catch (Exception e) { /* ignore */ }

                                // Do we have a UNIQUE matching weapon for the attachee in hand?
                                // Steve's clarification: only swap when a UNIQUE persona-matched
                                // weapon is waiting (e.g. Ahsoka's Shoto Lightsaber for Ahsoka,
                                // Luke's Hunting Rifle for Luke). Generic-for-generic swaps don't
                                // gain anything.
                                boolean haveMatchingInHand = false;
                                String matchingTitle = null;
                                try {
                                    for (PhysicalCard hc : gameState.getHand(context.getPlayerId())) {
                                        if (hc == null || hc.getBlueprint() == null) continue;
                                        if (hc.getBlueprint().getCardCategory() != CardCategory.WEAPON) continue;
                                        // STRICT: only consider UNIQUE weapons.
                                        if (hc.getBlueprint().getUniqueness()
                                                != com.gempukku.swccgo.common.Uniqueness.UNIQUE) continue;
                                        // Persona-match the hand weapon to the attachee.
                                        try {
                                            com.gempukku.swccgo.filters.Filter handMc =
                                                hc.getBlueprint().getMatchingCharacterFilter();
                                            if (handMc != null
                                                    && handMc.accepts(context.getGame(), attachee)) {
                                                haveMatchingInHand = true;
                                                matchingTitle = hc.getTitle();
                                                break;
                                            }
                                        } catch (Exception e) { /* ignore */ }
                                        // Persona-name fallback for unique weapons whose
                                        // matchingCharacterFilter we couldn't query (rare).
                                        // E.g. "Ahsoka's Shoto Lightsaber" title contains "ahsoka"
                                        // → matches an Ahsoka attachee.
                                        if (!haveMatchingInHand && hc.getTitle() != null
                                                && !attacheeTitleLower.isEmpty()) {
                                            String htl = hc.getTitle().toLowerCase(java.util.Locale.ROOT);
                                            String[] parts = attacheeTitleLower.split("\\s+");
                                            for (String p : parts) {
                                                if (p.length() >= 4 && htl.contains(p)) {
                                                    haveMatchingInHand = true;
                                                    matchingTitle = hc.getTitle();
                                                    break;
                                                }
                                            }
                                            if (haveMatchingInHand) break;
                                        }
                                    }
                                } catch (Exception e) { /* ignore */ }

                                if ((weaponIsNonUnique || !weaponMatchesAttachee) && haveMatchingInHand) {
                                    v67anBonus = 400.0f;
                                    v67anReason = String.format(
                                        "transfer wrong/generic weapon off %s (have matching '%s' in hand) — frees slot for persona-matched deploy!",
                                        attachee.getTitle(), matchingTitle);
                                }

                                // V72 (Steve, 2026-05-15): WEAPON REDISTRIBUTION.
                                // If the source character has 2+ weapons attached AND there's an
                                // unarmed friendly at the same site, transferring redistributes
                                // weapons across the team. Massively preferred over swap-from-hand
                                // because it directly fixes the "one char has 2 lightsabers,
                                // others have none" pattern.
                                try {
                                    int weaponsOnAttachee = 0;
                                    java.util.List<PhysicalCard> atts = gameState.getAttachedCards(attachee);
                                    if (atts != null) {
                                        for (PhysicalCard a : atts) {
                                            if (a != null && a.getBlueprint() != null
                                                    && a.getBlueprint().getCardCategory() == CardCategory.WEAPON) {
                                                weaponsOnAttachee++;
                                            }
                                        }
                                    }
                                    if (weaponsOnAttachee >= 2) {
                                        // Look for unarmed friendly at same site
                                        PhysicalCard attacheeLocation = attachee.getAtLocation();
                                        boolean unarmedBuddyExists = false;
                                        String buddyTitle = null;
                                        if (attacheeLocation != null) {
                                            java.util.Collection<PhysicalCard> sameSiteCards =
                                                gameState.getCardsAtLocation(attacheeLocation);
                                            if (sameSiteCards != null) {
                                                for (PhysicalCard sc : sameSiteCards) {
                                                    if (sc == null || sc.getBlueprint() == null) continue;
                                                    if (sc == attachee) continue;
                                                    if (!context.getPlayerId().equals(sc.getOwner())) continue;
                                                    if (sc.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                                                    // Check if this char is unarmed
                                                    boolean scArmed = false;
                                                    java.util.List<PhysicalCard> scAtts = gameState.getAttachedCards(sc);
                                                    if (scAtts != null) {
                                                        for (PhysicalCard sa : scAtts) {
                                                            if (sa != null && sa.getBlueprint() != null
                                                                    && sa.getBlueprint().getCardCategory() == CardCategory.WEAPON) {
                                                                scArmed = true;
                                                                break;
                                                            }
                                                        }
                                                    }
                                                    if (!scArmed) {
                                                        unarmedBuddyExists = true;
                                                        buddyTitle = sc.getTitle();
                                                        break;
                                                    }
                                                }
                                            }
                                        }
                                        if (unarmedBuddyExists) {
                                            v67anBonus = 500.0f;
                                            v67anReason = String.format(
                                                "V72 REDISTRIBUTE: %s has %d weapons; transfer one to unarmed buddy '%s' at same site",
                                                attachee.getTitle(), weaponsOnAttachee, buddyTitle);
                                        }
                                    }
                                } catch (Exception e) { logger.debug("V72 redistribute check error: {}", e.getMessage()); }
                            }
                        }
                    }
                } catch (Exception e) { logger.debug("V67an error: {}", e.getMessage()); }
                action.addReasoning("V67an WEAPON SWAP: " + v67anReason, v67anBonus);
                logger.warn("V67an WEAPON TRANSFER: '{}' → +{} ({})",
                    actionText, (int) v67anBonus, v67anReason);
            }

            // ========== Take Card Into Hand ==========
            // V192 (2026-07-06): reserve-deck takes now FALL THROUGH to the merged pull
            // scorer branch below (single owner — the old routing sent "Take X into hand
            // from Reserve Deck" here, so the pull branch never saw it and the V29.7 +250
            // fired instead of the tier table). Non-reserve takes (V29.7 BOUNCE class,
            // lost/used/force pile) keep routing here. Old dispatch commented out:
            // else if (actionText.contains("Take") && actionText.contains("into hand")) {
            else if (actionText.contains("Take") && actionText.contains("into hand")
                     && !textLower.contains("reserve deck") && !textLower.contains("[upload]")) {
                evaluateTakeIntoHand(action, context, actionText, textLower);
            }

            // ========== Prevent Battle/Move (Barrier Cards) ==========
            else if (actionText.contains("Prevent") && actionText.contains("from battling or moving")) {
                evaluateBarrier(action, context, actionText);
            }

            // ========== Monnok-type (Reveal Hand) ==========
            else if (actionText.contains("LOST: Reveal opponent's hand")) {
                int theirHandSize = gameState != null ? gameState.getHand(context.getOpponentId()).size() : 0;
                if (theirHandSize > 6) {
                    action.addReasoning("Opponent has many cards - reveal worth it", VERY_GOOD_DELTA);
                } else {
                    action.addReasoning("Opponent has few cards - save reveal", VERY_BAD_DELTA);
                }
            }

            // ========== Dangerous Cards ==========
            else if (textLower.contains("stardust") || textLower.contains("on the edge")) {
                action.addReasoning("Known dangerous card", VERY_BAD_DELTA);
            }

            // ========== Draw Card Into Hand ==========
            else if (actionText.equals("Draw card into hand from Force Pile")) {
                action.setActionType(ActionType.DRAW);
                action.addReasoning("Draw option (see DrawEvaluator)", 0.0f);
            }

            // ═══════════════════════════════════════════════════════════
            // ═══ REGION: MOVE — movement guards + dispatch (reorg 2026-07-06) ═══
            // Owns: V67ae 'move to here' drain guard (-300) + the Movement Actions dispatch below (V35.4 spy-flee,
            // landspeed/shuttle scoring). The full stay/flee/hunt ladder lives in MoveEvaluator (V136<->V137 parity).
            // NOTE: the V79 parse in MoveEvaluator is INERT; live parsec steering = V79b in RandoCalAi
            // (+ the V103 fallback near line ~1216 in this file).
            // Absorbs (dead, commented below/nearby — revert path, do not delete): none.
            // Cross-refs: MOVE (MoveEvaluator), SVC-SAFETY (V169 endangered movers), CONTROL (drain-before-move
            // interleave: moving a participant first forfeits that card's drain). See resources/RANDO_REORG_PLAN_2026-07-02.md §3 + Rando_Section_Manifest_2026-07-06.xlsx.
            // ═══════════════════════════════════════════════════════════
            // ========== V67ae: GAME-TEXT 'MOVE TO HERE' DRAIN GUARD ==========
            // Steve's report: Rando moved Vader from CC Lower Corridor (3-drain
            // battleground) to Mustafar: Vader's Castle (0 drain) using Castle's
            // 'may move character to here' game-text action. V67g MOVE-FROM-DRAIN
            // didn't fire because that's wired to landspeed/CardSelectionEvaluator,
            // not card-action moves through ActionTextEvaluator.
            //
            // Rule: if the source card's location has zero drain potential (no opp
            // icons) AND it's a 'move <character> to here' action, penalize. The
            // 'free move' attractiveness shouldn't outweigh losing drain pressure.
            else if ((textLower.contains("move from") && textLower.contains("to here"))
                    || textLower.contains("move to here")
                    || textLower.contains("relocate to here")) {
                action.setActionType(ActionType.MOVE);
                if (cardId != null && gameState != null && context.getPlayerId() != null) {
                    try {
                        PhysicalCard srcLoc = gameState.findCardById(Integer.parseInt(cardId));
                        if (srcLoc != null && srcLoc.getBlueprint() != null) {
                            // The destination IS the source card's location (it's a site itself)
                            String oppId = gameState.getOpponent(context.getPlayerId());
                            int destOppIcons = 0;
                            try {
                                if (context.getSide() == com.gempukku.swccgo.common.Side.LIGHT) {
                                    destOppIcons = srcLoc.getBlueprint().getIconCount(com.gempukku.swccgo.common.Icon.DARK_FORCE);
                                } else {
                                    destOppIcons = srcLoc.getBlueprint().getIconCount(com.gempukku.swccgo.common.Icon.LIGHT_FORCE);
                                }
                            } catch (Exception e) { /* ignore */ }

                            if (destOppIcons == 0) {
                                // V67ae ADJUSTED 2026-07-10 (Rey replay rbujmoc90br3uu4c, T5: Lando
                                // trapped at 6v11-armed Lower Corridor; the only escape was taxed -300
                                // and Pass won): mirror V33 BUDDY BREAK's "hopelessly outgunned →
                                // allow retreat" exemption (MoveEvaluator: gap >= 6 = site doomed).
                                // Location-sourced move actions never reach MoveEvaluator's threat
                                // tiers, so the exemption must live HERE: if ANY friendly-occupied
                                // location (other than this destination) is doomed, a retreat to a
                                // 0-drain site is legitimate — skip the penalty.
                                boolean v67aeRetreatExempt = false;
                                String v67aeDoomedLoc = null;
                                try {
                                    if (context.getGame() != null) {
                                        // ADJUSTED 2026-07-10b (Codex m00137 hole 2 + m00128): (a) scope the
                                        // scan to the DESTINATION's system — the mover of a location-sourced
                                        // "move to here" comes from a related site, so an unrelated doomed
                                        // site must not exempt this move; (b) weapon-adjust the enemy side
                                        // (raw 6v8 hid the armed 6v11+ reality — same V29.7 heuristic).
                                        String v67aeDestSys = srcLoc.getPartOfSystem();
                                        for (PhysicalCard rl : gameState.getTopLocations()) {
                                            if (rl == null || rl.getCardId() == srcLoc.getCardId()) continue;
                                            if (v67aeDestSys != null && rl.getPartOfSystem() != null
                                                    && !v67aeDestSys.equals(rl.getPartOfSystem())) continue;
                                            float rOur = context.getGame().getModifiersQuerying()
                                                .getTotalPowerAtLocation(gameState, rl, context.getPlayerId(), false, false);
                                            if (rOur <= 0) continue;
                                            float rOpp = context.getGame().getModifiersQuerying()
                                                .getTotalPowerAtLocation(gameState, rl, oppId, false, false);
                                            try {
                                                for (PhysicalCard rc : gameState.getCardsAtLocation(rl)) {
                                                    if (rc == null || rc.getBlueprint() == null) continue;
                                                    if (rc.getBlueprint().getCardCategory() != com.gempukku.swccgo.common.CardCategory.CHARACTER) continue;
                                                    if (!oppId.equals(rc.getOwner())) continue;
                                                    java.util.List<PhysicalCard> rAtts = gameState.getAttachedCards(rc);
                                                    if (rAtts != null) {
                                                        for (PhysicalCard att : rAtts) {
                                                            if (att == null || att.getBlueprint() == null) continue;
                                                            if (att.getBlueprint().getCardCategory() == com.gempukku.swccgo.common.CardCategory.WEAPON) {
                                                                String wt = att.getTitle() != null ? att.getTitle().toLowerCase(java.util.Locale.ROOT) : "";
                                                                rOpp += wt.contains("lightsaber") ? 5.0f : 3.0f;
                                                            }
                                                        }
                                                    }
                                                    String rgt = rc.getBlueprint().getGameText();
                                                    if (rgt != null && rgt.toLowerCase(java.util.Locale.ROOT).contains("permanent weapon")) {
                                                        String rct = rc.getTitle() != null ? rc.getTitle().toLowerCase(java.util.Locale.ROOT) : "";
                                                        rOpp += rct.contains("lightsaber") ? 5.0f : 3.0f;
                                                    }
                                                }
                                            } catch (Exception we) { /* raw power */ }
                                            if (rOpp - rOur >= 6f) {
                                                v67aeRetreatExempt = true;
                                                v67aeDoomedLoc = rl.getTitle();
                                                break;
                                            }
                                        }
                                    }
                                } catch (Exception e) { /* fail-open: no exemption */ }
                                if (v67aeRetreatExempt) {
                                    action.addReasoning(String.format(
                                        "V67ae RETREAT EXEMPT: '%s' hopelessly outgunned (gap >= 6, V33 standard) — retreat to non-drain allowed",
                                        v67aeDoomedLoc), 0.0f);
                                    logger.warn("V67ae RETREAT EXEMPT: doomed={} dest={} — skipping -300",
                                        v67aeDoomedLoc, srcLoc.getTitle());
                                } else {
                                    action.addReasoning(String.format(
                                        "V67ae MOVE-TO-NON-DRAIN: '%s' destination has 0 opp icons — losing drain pressure for a 'safe' retreat!",
                                        srcLoc.getTitle()), -300.0f);
                                    logger.warn("V67ae MOVE-TO-NON-DRAIN: action='{}' dest={} 0-drain — penalize free retreat (-300)",
                                        actionText, srcLoc.getTitle());
                                }
                            }
                        }
                    } catch (Exception e) { logger.debug("V67ae error: {}", e.getMessage()); }
                }
                action.addReasoning("V67ae move-to-here action — see drain analysis", 0.0f);
            }

            // ========== Movement Actions ==========
            else if (actionText.contains("Move using") || actionText.contains("Shuttle") ||
                     actionText.contains("Docking bay transit") || actionText.contains("Transport")) {
                action.setActionType(ActionType.MOVE);
                action.addReasoning("Movement option (see MoveEvaluator)", 0.0f);

                // === V35.4: BOOST MOVEMENT WHEN ENEMY SPY/PRESENCE BLOCKS OUR DRAIN ===
                // If our character is at ANY location where an opponent (including undercover spy)
                // has presence, our force drain is blocked. Moving away lets us drain elsewhere.
                // Undercover spies deploy on OUR side but count as opponent presence!
                // V35.4 UPDATED 2026-07-06 (audit row move-7): two fixes —
                //   1. OWNERSHIP: an opponent undercover spy is owner == OPPONENT &&
                //      isUndercover (the engine never flips owner on undercover). The old
                //      test flagged OUR OWN spy (owner == us) as the "opponent spy", so our
                //      V170 drain-block spy paid +250 to EVERY move action on the table,
                //      including its own move-away (fighting V53 SPY STAY -300 and losing).
                //   2. SCOPE: bonus only for actions whose MOVER (this action's cardId) is
                //      at the blocked location, and never for an undercover mover (V53/V170
                //      doctrine: the spy stays put). If the mover can't be resolved, fall
                //      back to the old any-location scan (keeps the rule alive).
                if (gameState != null && context.getPlayerId() != null) {
                    try {
                        String opponentId = gameState.getOpponent(context.getPlayerId());
                        // Resolve this action's mover and its current location (may stay null)
                        PhysicalCard v354Mover = null;
                        PhysicalCard v354MoverLoc = null;
                        if (cardId != null) {
                            try {
                                v354Mover = gameState.findCardById(Integer.parseInt(cardId));
                            } catch (NumberFormatException nfe) { /* temp id — mover unknown */ }
                        }
                        if (v354Mover != null) {
                            v354MoverLoc = v354Mover.getAtLocation();
                            if (v354MoverLoc == null && v354Mover.getAttachedTo() != null) {
                                v354MoverLoc = v354Mover.getAttachedTo().getAtLocation();
                            }
                        }
                        boolean v354MoverIsUndercover = v354Mover != null && v354Mover.isUndercover();
                        // V35.4 UPDATED 2026-07-07 (VERGE post-flip fix, Game9f3c46b00681): the mover
                        // can be a mobile-system LOCATION card (the Death Star). 'Move away to drain
                        // elsewhere' is character/starship doctrine — a location neither drains nor
                        // unblocks a drain by moving, yet this +150 attached to the Death Star's
                        // hyperspeed move at T4/T5 and cemented the pointless orbit-exit toggles
                        // (boundary: with V79's +500 orbit-gated, +40 base +150 would still beat
                        // Pass ~+28). Skip location movers entirely.
                        boolean v354MoverIsLocation = v354Mover != null && v354Mover.getBlueprint() != null
                            && v354Mover.getBlueprint().getCardCategory() == CardCategory.LOCATION;
                        if (!v354MoverIsUndercover && !v354MoverIsLocation) {
                            for (com.gempukku.swccgo.game.PhysicalCard loc : gameState.getLocationsInOrder()) {
                                if (loc == null || loc.getTitle() == null) continue;
                                // Scope to the mover's own location when we know it
                                if (v354MoverLoc != null && loc.getCardId() != v354MoverLoc.getCardId()) continue;

                                boolean weHavePresence = false;
                                boolean oppHasPresence = false;
                                boolean oppHasUndercoverSpy = false;
                                for (com.gempukku.swccgo.game.PhysicalCard card : gameState.getCardsAtLocation(loc)) {
                                    if (card == null) continue;
                                    if (context.getPlayerId().equals(card.getOwner())) {
                                        weHavePresence = true;
                                    } else if (opponentId != null && opponentId.equals(card.getOwner())) {
                                        oppHasPresence = true;
                                        // V35.4 UPDATED 2026-07-06: opponent spy = OPPONENT-owned
                                        // undercover card at a location we occupy
                                        if (card.isUndercover()) {
                                            oppHasUndercoverSpy = true;
                                        }
                                    }
                                }

                                // If opponent has presence (or undercover spy) at our location, drain is blocked
                                if (weHavePresence && (oppHasPresence || oppHasUndercoverSpy)) {
                                    float spyBonus = oppHasUndercoverSpy ? 250.0f : 150.0f;
                                    action.addReasoning(String.format(
                                        "V35.4: %s blocking drain at %s — move away to drain elsewhere!",
                                        oppHasUndercoverSpy ? "UNDERCOVER SPY" : "Enemy presence",
                                        loc.getTitle()), spyBonus);
                                    logger.warn("V35.4: {} at {} blocking our drain — boosting movement (+{})",
                                        oppHasUndercoverSpy ? "UNDERCOVER SPY" : "Enemy",
                                        loc.getTitle(), (int)spyBonus);
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.debug("V35.4: Error checking spy-blocked sites: {}", e.getMessage());
                    }
                }

                // === V29.7: VADER'S CASTLE RETREAT PENALTY ===
                // Mustafar: Vader's Castle can teleport Vader back to Mustafar.
                // This is TERRIBLE when Vader is at a location where he can force drain!
                // Mustafar has 0 opponent icons = no drain value. Moving Vader there
                // means losing a turn of draining at the current location.
                // Only allow Castle retreat if Vader is outnumbered and about to die.
                if ((textLower.contains("vader") && textLower.contains("castle")) ||
                    textLower.contains("mustafar")) {
                    try {
                        // Find Vader's current location and check drain potential
                        String pid = context.getPlayerId();
                        if (gameState != null && pid != null) {
                            for (PhysicalCard card : gameState.getAllPermanentCards()) {
                                if (card == null || !pid.equals(card.getOwner())) continue;
                                com.gempukku.swccgo.common.Zone zone = card.getZone();
                                if (zone == null || !zone.isInPlay()) continue;
                                String cTitle = card.getTitle();
                                if (cTitle == null || !cTitle.toLowerCase(java.util.Locale.ROOT).contains("vader")) continue;
                                if (card.getBlueprint() == null || card.getBlueprint().getCardCategory() != com.gempukku.swccgo.common.CardCategory.CHARACTER) continue;

                                // Found Vader — check his current location
                                PhysicalCard vaderLoc = card.getAtLocation();
                                if (vaderLoc == null && card.getAttachedTo() != null) {
                                    // Vader might be aboard a vehicle/starship — get the vehicle's location
                                    vaderLoc = card.getAttachedTo().getAtLocation();
                                }
                                if (vaderLoc != null && vaderLoc.getTitle() != null) {
                                    String vLocTitle = vaderLoc.getTitle().toLowerCase(java.util.Locale.ROOT);
                                    if (vLocTitle.contains("mustafar")) {
                                        // Vader is already at Mustafar — this is a move OUT, which is fine
                                        break;
                                    }
                                    // Vader is at a non-Mustafar location — check if it has drain value
                                    SwccgCardBlueprint locBp = vaderLoc.getBlueprint();
                                    if (locBp != null) {
                                        int oppIcons = 0;
                                        if (context.getSide() == Side.DARK) {
                                            oppIcons = locBp.getIconCount(com.gempukku.swccgo.common.Icon.LIGHT_FORCE);
                                        } else {
                                            oppIcons = locBp.getIconCount(com.gempukku.swccgo.common.Icon.DARK_FORCE);
                                        }
                                        if (oppIcons > 0) {
                                            // Vader is at a location with drain value — DON'T retreat!
                                            action.addReasoning("V29.7 VADER RETREAT: Vader is draining " + oppIcons +
                                                " at " + vaderLoc.getTitle() + " — DON'T retreat to Mustafar!", -300.0f);
                                            logger.warn("V29.7 VADER RETREAT BLOCKED: Vader at {} with {} drain — retreating to Mustafar is terrible! (-300)",
                                                vaderLoc.getTitle(), oppIcons);
                                        }
                                    }
                                }
                                break; // Found Vader, done
                            }
                        }
                    } catch (Exception e) {
                        logger.debug("V29.7: Error checking Vader retreat: {}", e.getMessage());
                    }
                }
            }
            else if (actionText.equals("Take off") || actionText.equals("Land")) {
                action.setActionType(ActionType.MOVE);
                action.addReasoning("Take off/Land option (see MoveEvaluator)", 0.0f);
            }

            // ========== Make Opponent Lose Force ==========
            else if (actionText.contains("Make opponent lose")) {
                action.addReasoning("Making opponent lose force", GOOD_DELTA);
            }

            // ========== V29.7: Deploy Docking Bay — Smart Strategy ==========
            // Docking bays are SHARED — opponent can deploy characters to YOUR docking bays!
            // Only deploy a docking bay if we don't already have empty ones on the table.
            // Empty docking bays = free locations for the opponent.
            else if (actionText.contains("Deploy docking bay") || textLower.contains("deploy a docking bay")) {
                boolean hasEmptyDockingBay = false;
                int emptyBayCount = 0;
                int totalOurBays = 0;
                GameState bayGs = context.getGameState();
                String bayPlayerId = context.getPlayerId();
                if (bayGs != null && bayPlayerId != null) {
                    try {
                        for (PhysicalCard loc : bayGs.getTopLocations()) {
                            if (loc == null || loc.getTitle() == null) continue;
                            String locTitle = loc.getTitle().toLowerCase(java.util.Locale.ROOT);
                            // Check if this is a docking bay we own
                            if (locTitle.contains("docking bay") || locTitle.contains("landing platform")) {
                                // Check if we control it (our card)
                                if (bayPlayerId.equals(loc.getOwner())) {
                                    totalOurBays++;
                                    // Check if any of OUR characters are there
                                    boolean hasFriendlyChar = false;
                                    java.util.List<PhysicalCard> cardsHere = bayGs.getCardsAtLocation(loc);
                                    if (cardsHere != null) {
                                        for (PhysicalCard pc : cardsHere) {
                                            if (pc != null && bayPlayerId.equals(pc.getOwner())
                                                && pc.getBlueprint() != null
                                                && pc.getBlueprint().getCardCategory() == com.gempukku.swccgo.common.CardCategory.CHARACTER) {
                                                hasFriendlyChar = true;
                                                break;
                                            }
                                        }
                                    }
                                    if (!hasFriendlyChar) {
                                        hasEmptyDockingBay = true;
                                        emptyBayCount++;
                                    }
                                }
                            }
                        }
                    } catch (Exception e) { /* ignore */ }
                }

                if (hasEmptyDockingBay) {
                    // Already have empty docking bays — deploying MORE just gives opponent more free locations!
                    action.addReasoning("V29.7 DOCKING BAY: Already have " + emptyBayCount
                        + " empty bay(s) — deploy characters there first, don't give opponent more locations!", -200.0f);
                    logger.warn("V29.7 DOCKING BAY BLOCKED: {} empty bay(s) on table — don't deploy more (-200)", emptyBayCount);
                } else if (totalOurBays >= 2) {
                    // Already have 2+ bays with characters — probably don't need more
                    action.addReasoning("V29.7 DOCKING BAY: Already have " + totalOurBays + " bays — enough for transit", -50.0f);
                } else if (totalOurBays == 0) {
                    // V29.7: FIRST docking bay — VERY high priority! This creates a battleground
                    // location where our characters can safely deploy. Must fire BEFORE character
                    // deploys so characters have a friendly BG location to go to.
                    action.addReasoning("V29.7 FIRST DOCKING BAY: Deploy FIRST to create battleground for characters!", 200.0f);
                    logger.warn("V29.7 FIRST BAY: No bays on table — high priority deploy (+200)");
                } else {
                    // Have 1 manned bay — second bay OK for transit network
                    action.addReasoning("V29.7 DOCKING BAY: Deploy second bay for transit network", GOOD_DELTA);
                }
            }

            // ========== V25: HUNT DOWN V — VADER CASTLE DEPLOY ACTION ==========
            // If the action deploys Vader from Reserve Deck (via Vader's Castle), and
            // Hunt Down V is the objective, this is THE most important action in the game.
            // Vader must be on table for the deck to function.
            else if (actionText.contains("Deploy Vader from Reserve Deck") || actionText.contains("Deploy Vader here")) {
                com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer vaderObjAnalyzer =
                    context.getObjectiveAnalyzer();
                if (vaderObjAnalyzer != null && vaderObjAnalyzer.isAnalyzed() && vaderObjAnalyzer.isHuntDownV()) {
                    boolean vaderOnTable = false;
                    int forceAvailable = 0;
                    if (context.getGame() != null && context.getGame().getGameState() != null) {
                        com.gempukku.swccgo.game.state.GameState vaderGs = context.getGame().getGameState();
                        vaderOnTable = vaderObjAnalyzer.isVaderOnTable(vaderGs, context.getPlayerId());
                        forceAvailable = vaderGs.getForcePileSize(context.getPlayerId());
                    }
                    if (!vaderOnTable) {
                        // V25: Don't attempt Vader Castle deploy if not enough Force
                        // Vader's deploy cost is typically 6. This is a once-per-game action
                        // so we must NOT waste it when we can't afford to deploy him.
                        if (forceAvailable < 6) {
                            action.addReasoning("V25 HUNT DOWN: NOT ENOUGH FORCE for Vader! Need 6, have " + forceAvailable + ". SAVE Castle action!", -500.0f);
                            logger.warn("V25 HUNT DOWN: Vader Castle deploy BLOCKED — only {} Force available (need 6)", forceAvailable);
                        } else {
                            action.addReasoning("V25 HUNT DOWN: DEPLOY VADER NOW! Have " + forceAvailable + " Force, deck cannot function without him!", VERY_GOOD_DELTA + 500.0f);
                            logger.warn("V25 HUNT DOWN: Vader Castle deploy action — TOP PRIORITY (+{}) with {} Force", (int)(VERY_GOOD_DELTA + 500.0f), forceAvailable);
                        }
                    } else {
                        action.addReasoning("Vader already on table — Castle deploy not urgent", 0.0f);
                    }
                } else {
                    action.addReasoning("Deploy Vader from reserve", VERY_GOOD_DELTA);
                }
            }

            // ========== V26/V29.6: Dining Room — Deploy Lando (TDIGWATT) ==========
            // Dining Room's game text deploys Lando from Reserve Deck — a key TDIGWATT piece.
            // DeployEvaluator can't find the card (it's in reserve, not hand), so we boost
            // here in ActionTextEvaluator.
            //
            // V29.6 FIX: Check if Lando would be ALONE at Dining Room. If no friendly
            // characters are already there, deploying Lando alone is suicide — opponent
            // will drop a character + weapon and kill him immediately. Defer until we
            // have a buddy at Dining Room first.
            else if ((textLower.contains("dining room") || textLower.contains("deploy lando"))
                     && textLower.contains("reserve")) {
                com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer drLandoAnalyzer =
                    context.getObjectiveAnalyzer();

                // V29.6: Check if there are friendly characters at Dining Room
                boolean friendliesAtDiningRoom = false;
                int friendlyCountAtDR = 0;
                try {
                    GameState drGameState = context.getGameState();
                    String drPlayerId = context.getPlayerId();
                    if (drGameState != null && drPlayerId != null) {
                        // Find Dining Room on the table
                        java.util.List<PhysicalCard> allLocs = drGameState.getTopLocations();
                        PhysicalCard diningRoomCard = null;
                        if (allLocs != null) {
                            for (PhysicalCard loc : allLocs) {
                                if (loc != null && loc.getTitle() != null
                                    && loc.getTitle().toLowerCase(java.util.Locale.ROOT).contains("dining room")) {
                                    diningRoomCard = loc;
                                    break;
                                }
                            }
                        }
                        if (diningRoomCard != null) {
                            java.util.List<PhysicalCard> cardsAtDR = drGameState.getCardsAtLocation(diningRoomCard);
                            if (cardsAtDR != null) {
                                for (PhysicalCard c : cardsAtDR) {
                                    if (c != null && drPlayerId.equals(c.getOwner())
                                        && c.getBlueprint() != null
                                        && c.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                                        friendlyCountAtDR++;
                                    }
                                }
                            }
                            friendliesAtDiningRoom = (friendlyCountAtDR > 0);
                        }
                    }
                } catch (Exception e) {
                    logger.debug("V29.6 DINING ROOM: Error checking friendlies at DR: {}", e.getMessage());
                }

                if (drLandoAnalyzer != null && drLandoAnalyzer.isAnalyzed()
                    && drLandoAnalyzer.needsBespinSystemPresence()) {
                    if (friendliesAtDiningRoom) {
                        // Buddies present — safe to deploy Lando!
                        action.addReasoning("V29.6 DINING ROOM: Deploy Lando with " + friendlyCountAtDR + " friendlies — safe!", 150.0f);
                        logger.warn("V29.6 DINING ROOM: Lando deploy with {} friendlies at DR — +150", friendlyCountAtDR);
                    } else {
                        // Lando would be ALONE — defer until we have a buddy there.
                        // Small positive so it's still considered but won't beat deploying a character first.
                        action.addReasoning("V29.6 DINING ROOM: Lando would be ALONE — deploy a buddy first!", -30.0f);
                        logger.warn("V29.6 DINING ROOM: Lando deploy DEFERRED — no friendlies at DR, penalty -30");
                    }
                } else {
                    if (friendliesAtDiningRoom) {
                        action.addReasoning("Dining Room: Deploy Lando from reserve (friendlies present)", GOOD_DELTA);
                    } else {
                        action.addReasoning("V29.6 Dining Room: Lando alone — risky!", -20.0f);
                        logger.info("V29.6 DINING ROOM: Non-TDIGWATT Lando deploy deferred — alone at DR");
                    }
                }
            }

            // ========== Deploy From Reserve (Risky) ==========
            // V114 (Steve, 2026-05-21): DELETED the generic "Deploy ... from ..." catch-all.
            // It assigned -10 to ALL "Deploy X from Y" action texts before the V60/V67ai
            // block (line 3120) could award the +2000 OBJECTIVE-tier location-pull bonus.
            // This caused Rando to IGNORE Hunt Down V's once-per-turn "Deploy a [Cloud City]
            // or Malachor battleground site from Reserve Deck" every single turn in
            // replay dc8n6dl9s88rqycz (2026-05-12). Same bug affected EVERY non-specific
            // reserve/lost/used/stacked-pile deploy action.
            //
            // Per Steve: pull actions are net positive — they thin the deck, bring value
            // into play, and are usually free or low-cost (feedback_reserve_deck_pulls.md).
            // No "Deploy X from Y" action deserves an unconditional -10. Specific bad
            // cases are already handled upstream (V67u Force Push exchange, V35.2 weapon
            // rack outside battle, etc.). Letting these actions fall through to V60/V67ai
            // gives the correct +150 baseline plus +2000 objective bonus for Hunt Down V
            // and similar location/site/character pulls.
            // (Mirrored in chosenone ActionTextEvaluator.java)

            // ========== Embark ==========
            else if (actionText.contains("Embark")) {
                action.setActionType(ActionType.MOVE);
                evaluateEmbark(action, context, actionText, cardId);
            }

            // ========== Disembark/Relocate/Transfer ==========
            else if (actionText.contains("Disembark") || actionText.contains("Relocate") ||
                     actionText.contains("Transfer")) {
                action.setActionType(ActionType.MOVE);
                action.addReasoning("Usually avoid disembark/relocate/transfer", VERY_BAD_DELTA);
            }

            // ========== Ship-dock ==========
            else if (actionText.contains("Ship-dock")) {
                action.addReasoning("Avoid ship-docking", VERY_BAD_DELTA);
            }

            // ========== Place in Lost Pile ==========
            else if (actionText.contains("Place in Lost Pile")) {
                action.addReasoning("Avoid losing cards", VERY_BAD_DELTA);
            }

            // ========== Grab ==========
            else if (actionText.contains("Grab")) {
                evaluateGrab(action, context, actionText);
            }

            // ========== Break Cover ==========
            else if (actionText.contains("Break cover")) {
                evaluateBreakCover(action, context, actionText);
            }

            // ========== Retrieve Force ==========
            else if (textLower.contains("retrieve") || actionText.contains("Place out of play to retrieve")) {
                int lostPileSize = gameState != null ? gameState.getLostPile(context.getPlayerId()).size() : 0;
                if (lostPileSize > 15) {
                    action.addReasoning("High lost pile - retrieve worth it", GOOD_DELTA);
                } else {
                    action.addReasoning("Low lost pile - save retrieve", BAD_DELTA);
                }
            }

            // ========== Defensive Shields ==========
            // V29.1: Shield pacing — don't burn all 4 shield slots immediately.
            // Play 2 shields on turn 1 to get basic protection, then WAIT to see
            // what the opponent is running before committing the remaining slots.
            // This lets us pick targeted counters instead of generic shields.
            else if (actionText.contains("Play a Defensive Shield")) {
                if (!context.isMyTurn()) {
                    action.addReasoning("Defensive shield during opponent's turn - prefer pass", -10.0f);
                } else {
                    // Check shield pacing via ShieldStrategy
                    com.gempukku.swccgo.ai.models.chosenone.strategy.ShieldStrategy shieldStrat = context.getShieldStrategy();
                    int turnNum = context.getTurnNumber();
                    if (shieldStrat != null && shieldStrat.atPacingCap(turnNum)) {
                        // We've played enough shields for this turn — hold remaining slots
                        action.addReasoning("V29.1 SHIELD PACING: Holding shield slot — wait to scout opponent (turn " + turnNum + ")", -40.0f);
                    } else {
                        action.addReasoning("Defensive shield", VERY_GOOD_DELTA);
                    }
                }
            }

            // ========== Deploy on table/location ==========
            else if (actionText.startsWith("Deploy on")) {
                if (textLower.contains("projection") && textLower.contains("side")) {
                    action.addReasoning("Never put projection on side of table", VERY_BAD_DELTA);
                } else {
                    action.addReasoning("Deploy on location/table", GOOD_DELTA);
                }
            }

            // ========== Deploy unique ==========
            else if (actionText.startsWith("Deploy unique")) {
                action.addReasoning("Special battleground deploy", GOOD_DELTA);
            }

            // ========== USED: Peek at top ==========
            else if (actionText.startsWith("USED: Peek at top")) {
                action.addReasoning("Peek for card advantage", GOOD_DELTA);
            }

            // ========== Force Drain Cancellation ==========
            else if (actionText.contains("Cancel Force drain")) {
                if (context.isMyTurn()) {
                    action.addReasoning("V52 NEVER SELF-CANCEL: Don't cancel own force drain!", -9999.0f);
                    logger.warn("V52 SELF-CANCEL BLOCKED: Cancel Force drain on own turn — HARD BLOCKED!");
                } else {
                    action.addReasoning("Cancel opponent's force drain", GOOD_DELTA);
                }
            }

            // ========== V74: Maintenance Cost Satisfaction (replaces V22.3) ==========
            // When a maintenance card's upkeep is due, Rando gets a choice:
            //   "Use X Force"          (pay — KEEP the card)
            //   "Lose X Force ... Used Pile" (recyclable — keep blueprint, lose card from table)
            //   "Place out of play"   (PERMANENT loss — worst option)
            //
            // V22.3's old check applied to the ACTION text, which is short
            // ("Use 1 Force" / "Place out of play") and never contains
            // "maintenance" — so V22.3 never fired. Replay May 15 showed Rando
            // picking "Place out of play" for Lando every turn (4 times).
            //
            // V74 fix: detect maintenance context from the DECISION text
            // (which DOES contain "maintenance"), then score each action's
            // OWN text accordingly.
            else if (context.getDecisionText() != null
                     && context.getDecisionText().toLowerCase(java.util.Locale.ROOT)
                        .contains("maintenance")) {
                if (textLower.contains("use ") && textLower.contains(" force")) {
                    // PAY option — strongly prefer
                    action.addReasoning("V74 MAINTENANCE PAY: keep the card alive!", 400.0f);
                    logger.warn("V74 MAINTENANCE PAY: '{}' → +400", actionText);
                } else if (textLower.contains("out of play")) {
                    // PERMANENT LOSS — avoid heavily
                    action.addReasoning("V74 MAINTENANCE SACRIFICE: place out of play is PERMANENT loss!", -800.0f);
                    logger.warn("V74 MAINTENANCE SACRIFICE: '{}' → -800", actionText);
                } else if (textLower.contains("lose ") && textLower.contains(" force")
                           && (textLower.contains("used pile") || textLower.contains("place in used"))) {
                    // Recyclable — better than out-of-play but worse than paying
                    action.addReasoning("V74 MAINTENANCE USED-PILE: lose card to used pile, keep blueprint", -200.0f);
                    logger.warn("V74 MAINTENANCE USED-PILE: '{}' → -200", actionText);
                } else if (textLower.contains("sacrifice")) {
                    action.addReasoning("V74 MAINTENANCE SACRIFICE: avoid", -800.0f);
                    logger.warn("V74 MAINTENANCE SACRIFICE: '{}' → -800", actionText);
                }
            }

            // ========== Use/Lose Force Actions ==========
            else if (textLower.startsWith("use ") && textLower.contains(" force ")) {
                // V22.3: Check if this might be a maintenance payment
                // Maintenance decisions often just say "Use X Force" without "maintenance" keyword
                // If the decision context involves a maintenance card, prefer paying
                if (textLower.contains("cost") || textLower.contains("upkeep")) {
                    action.addReasoning("V22.3 MAINTENANCE: Pay upkeep cost!", 150.0f);
                    logger.warn("V22.3 MAINTENANCE: Likely upkeep payment - '{}'", actionText);
                } else {
                    // V24.5: No randomness — generic use force should be avoided
                    action.addReasoning("'Use Force' action — prefer not to use force unnecessarily", -20.0f);
                }
            }
            else if (textLower.startsWith("lose ") && textLower.contains(" force ")) {
                // V24.5: No randomness — losing force is almost always bad
                action.addReasoning("'Lose Force' action — avoid losing force", -30.0f);
            }
            // V22.3: Catch generic sacrifice options that aren't tagged as maintenance
            else if (textLower.contains("sacrifice") || textLower.contains("place out of play")) {
                action.addReasoning("V22.3: Avoid sacrificing cards — prefer alternatives", -150.0f);
                logger.info("V22.3 SACRIFICE PENALTY: '{}'", actionText);
            }

            // ========== V22.5: Alert My Star Destroyer / Ship Deployment Priority ==========
            // "Alert My Star Destroyer" deploys Executor + pilot for cheap.
            // This is CRITICAL for TDIGWATT — Bespin system occupation enables Dark Deal
            // and Cloud City Occupation, which are the deck's primary damage engines.
            else if (textLower.contains("reveal") && (textLower.contains("star destroyer") || textLower.contains("pilot"))) {
                // Check if we have a ship at Bespin system already
                boolean hasBespinShip = false;
                if (gameState != null) {
                    try {
                        String pid = context.getPlayerId();
                        for (com.gempukku.swccgo.game.PhysicalCard loc : gameState.getLocationsInOrder()) {
                            if (loc != null && loc.getTitle() != null &&
                                loc.getTitle().toLowerCase(java.util.Locale.ROOT).contains("bespin") &&
                                loc.getBlueprint() != null && loc.getBlueprint().getCardSubtype() != null &&
                                loc.getBlueprint().getCardSubtype() == com.gempukku.swccgo.common.CardSubtype.SYSTEM) {
                                float ourPower = context.getGame().getModifiersQuerying().getTotalPowerAtLocation(
                                    gameState, loc, pid, false, false);
                                if (ourPower > 0) hasBespinShip = true;
                                break;
                            }
                        }
                    } catch (Exception e) {
                        // Ignore
                    }
                }
                if (!hasBespinShip) {
                    action.addReasoning("V22.5 CRITICAL: Deploy ship to Bespin! Enables Dark Deal + CC Occupation!", 300.0f);
                    logger.warn("V22.5 BESPIN PRIORITY: Alert My Star Destroyer — no ship at Bespin yet! (+300)");
                } else {
                    action.addReasoning("V22.5: Deploy ship (Bespin already occupied)", 100.0f);
                    logger.info("V22.5: Alert My Star Destroyer — Bespin already has ship presence");
                }
            }
            // V22.5: Generic "deploy simultaneously" or ship+pilot combos
            else if (textLower.contains("deploy") && textLower.contains("simultaneously")) {
                action.addReasoning("V22.5: Deploy pilot+ship combo - efficient!", 120.0f);
                logger.info("V22.5: Simultaneous deploy detected");
            }

            // ========== V25: INITIATE BATTLE ==========
            // Battle initiation was previously unhandled (fell to default 0.0f) which
            // meant Rando NEVER chose to initiate battles because other actions always
            // outscored them. Now we evaluate the specific location's power differential.
            else if (actionText.contains("Initiate battle") || actionText.contains("initiate battle")) {
                action.setActionType(ActionType.BATTLE);
                boolean battleScored = false;

                SwccgGame battleGame = context.getGame();
                if (battleGame != null && context.getGame().getGameState() != null) {
                    com.gempukku.swccgo.game.state.GameState bGs = battleGame.getGameState();
                    String bPlayerId = context.getPlayerId();
                    String bOpponentId = bGs.getOpponent(bPlayerId);

                    if (bOpponentId != null) {
                        try {
                            // Find which location this battle targets
                            for (PhysicalCard bLoc : bGs.getTopLocations()) {
                                String bLocTitle = bLoc.getTitle();
                                if (bLocTitle != null && actionText.contains(bLocTitle)) {
                                    float ourPower = battleGame.getModifiersQuerying().getTotalPowerAtLocation(
                                        bGs, bLoc, bPlayerId, false, false);
                                    float theirPower = battleGame.getModifiersQuerying().getTotalPowerAtLocation(
                                        bGs, bLoc, bOpponentId, false, false);
                                    float ourAbility = battleGame.getModifiersQuerying().getTotalAbilityAtLocation(
                                        bGs, bPlayerId, bLoc);
                                    float theirAbility = battleGame.getModifiersQuerying().getTotalAbilityAtLocation(
                                        bGs, bOpponentId, bLoc);
                                    float powerDiff = ourPower - theirPower;
                                    float abilityDiff = ourAbility - theirAbility;
                                    // Ability matters: each point of ability = roughly 2.5 power via destiny draws
                                    float effectiveDiff = powerDiff + (abilityDiff * 2.5f);

                                    logger.warn("V25 BATTLE EVAL at {}: our power={} ability={}, their power={} ability={}, effectiveDiff={}",
                                        bLocTitle, (int)ourPower, (int)ourAbility, (int)theirPower, (int)theirAbility, (int)effectiveDiff);

                                    if (theirPower <= 0) {
                                        // No opponent here — can't battle
                                        action.addReasoning("V25 BATTLE: No opponent at " + bLocTitle, -100.0f);
                                    } else if (theirPower > ourPower * 2 && theirPower > 6) {
                                        // Suicidal — hard block
                                        action.addReasoning(String.format("V25 BATTLE SUICIDE: %.0f vs %.0f at %s — NEVER!",
                                            ourPower, theirPower, bLocTitle), -500.0f);
                                    } else if (effectiveDiff >= 8) {
                                        // Crushing advantage
                                        action.addReasoning(String.format("V25 BATTLE CRUSH at %s: %.0f vs %.0f — ATTACK!",
                                            bLocTitle, ourPower, theirPower), 200.0f);
                                    } else if (effectiveDiff >= 5) {
                                        // Strong advantage
                                        action.addReasoning(String.format("V25 BATTLE FAVORABLE at %s: %.0f vs %.0f",
                                            bLocTitle, ourPower, theirPower), 120.0f);
                                    } else if (effectiveDiff >= 2) {
                                        // Marginal advantage
                                        action.addReasoning(String.format("V25 BATTLE MARGINAL at %s: %.0f vs %.0f",
                                            bLocTitle, ourPower, theirPower), 60.0f);
                                    } else if (effectiveDiff >= -2) {
                                        // Even — slight positive to encourage aggression
                                        action.addReasoning(String.format("V25 BATTLE EVEN at %s: %.0f vs %.0f — risky but worth trying",
                                            bLocTitle, ourPower, theirPower), 20.0f);
                                    } else {
                                        // Unfavorable
                                        float penalty = -60.0f;
                                        if (effectiveDiff < -8) penalty = -120.0f;
                                        if (effectiveDiff < -15) penalty = -250.0f;
                                        action.addReasoning(String.format("V25 BATTLE UNFAVORABLE at %s: %.0f vs %.0f — avoid!",
                                            bLocTitle, ourPower, theirPower), penalty);
                                    }
                                    battleScored = true;
                                    break;
                                }
                            }
                        } catch (Exception e) {
                            logger.warn("V25 BATTLE: Error evaluating battle: {}", e.getMessage());
                        }
                    }
                }

                if (!battleScored) {
                    // Fallback: give modest positive score to encourage battling
                    action.addReasoning("V25 BATTLE: Initiate battle (no location data)", 30.0f);
                }

                // Check reserve for destiny draws
                int battleReserve = 0;
                if (context.getGame() != null && context.getGame().getGameState() != null) {
                    battleReserve = context.getGame().getGameState().getReserveDeckSize(context.getPlayerId());
                }
                if (battleReserve < 3) {
                    action.addReasoning("V25 BATTLE: Low reserve (" + battleReserve + ") — bad destiny draws!", -50.0f);
                }

                logger.warn("V25 BATTLE: '{}' scored {}", actionText.length() > 60 ? actionText.substring(0,60) + "..." : actionText,
                    String.format("%.1f", action.getScore()));
            }

            // ========== V29.6/V29.11: BLASTER RACK — ONLY RACK TO SAVE WEAPONS FROM DYING CHARACTERS ==========
            // Blaster Rack stacks a weapon on it. This is ONLY useful at the END of a battle
            // when a character carrying the weapon has been HIT or is about to be forfeited
            // to satisfy attrition/battle damage. Proactively racking weapons outside of battle
            // damage resolution is terrible — it strips characters of weapons before they can fire.
            // Example: Vader had lightsaber, Rando racked it, Vader went to battle unarmed.
            // Action text can be "Stack character weapon" OR contain "rack" + "stack"
            else if ((textLower.contains("rack") && textLower.contains("stack"))
                || (textLower.contains("stack") && textLower.contains("character weapon"))) {
                Phase rackPhase = context.getPhase();
                // Check if we're in battle damage/attrition resolution
                // During battle damage, the decision text often references damage/attrition/forfeit
                boolean duringBattleDamage = false;
                try {
                    GameState rackGs = context.getGameState();
                    if (rackGs != null && rackGs.isDuringBattle()) {
                        // We're in a battle — check if damage is being resolved
                        // If the game is asking us to use rack during battle, it's likely
                        // because we're about to lose the character carrying the weapon.
                        duringBattleDamage = true;
                    }
                } catch (Exception e) {
                    logger.debug("V29.6 RACK: Error checking battle state: {}", e.getMessage());
                }

                if (duringBattleDamage) {
                    // V35.2: During battle — but ONLY rack weapons from characters AT the battle!
                    // Bug: Rando racked Vader's Lightsaber from Mustafar while battle was at Mos Eisley.
                    boolean weaponCharAtBattle = false;
                    try {
                        GameState rackGs2 = context.getGameState();
                        if (rackGs2 != null && rackGs2.getBattleState() != null) {
                            PhysicalCard battleLoc = rackGs2.getBattleState().getBattleLocation();
                            if (battleLoc != null) {
                                String rackPid = context.getPlayerId();
                                for (PhysicalCard tableCard : rackGs2.getAllPermanentCards()) {
                                    if (tableCard == null || !rackPid.equals(tableCard.getOwner())) continue;
                                    if (tableCard.getBlueprint() == null) continue;
                                    if (tableCard.getBlueprint().getCardCategory() != com.gempukku.swccgo.common.CardCategory.WEAPON) continue;
                                    com.gempukku.swccgo.common.Zone wz = tableCard.getZone();
                                    if (wz == null || !wz.isInPlay()) continue;
                                    String wTitle = tableCard.getTitle() != null ? tableCard.getTitle().toLowerCase(Locale.ROOT) : "";
                                    if (wTitle.isEmpty() || !textLower.contains(wTitle)) continue;
                                    PhysicalCard parentChar = tableCard.getAttachedTo();
                                    if (parentChar != null) {
                                        PhysicalCard charLoc = parentChar.getAtLocation();
                                        if (charLoc != null && charLoc == battleLoc) {
                                            weaponCharAtBattle = true;
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.debug("V35.2 RACK: Error checking weapon location: {}", e.getMessage());
                        weaponCharAtBattle = true; // Default to allow if check fails
                    }

                    if (weaponCharAtBattle) {
                        action.addReasoning("V35.2 RACK: Character in battle — save weapon!", 80.0f);
                        logger.warn("V35.2 RACK: Weapon's character AT battle — saving '{}'", actionText);
                    } else {
                        action.addReasoning("V35.2 RACK: Character NOT in this battle — do NOT rack!", -500.0f);
                        logger.warn("V35.2 RACK: BLOCKED — weapon's character not at battle! '{}'", actionText);
                    }
                } else {
                    // Outside battle — proactive racking is TERRIBLE
                    action.addReasoning("V29.6 BLASTER RACK: Do NOT rack weapons outside battle — characters need them!", -500.0f);
                    logger.warn("V29.6 BLASTER RACK: BLOCKED proactive racking outside battle — '{}'", actionText);
                }
            }

            // ========== V60 HIDDEN PATH TRANSIT — Underground Corridor game text ==========
            // "Move Jedi Survivor here to a site" is Underground Corridor's game-text action
            // that transits Jedi Survivors from Corridor to a Jabiim site or opponent's
            // battleground. This is THE action that flips the Hidden Path objective.
            // Previously scored 0.0 ("Unknown action type") while landspeed (which goes
            // backward to Safehouse) got +9999 from V53b. FIXES Issue #C from peaceful-pike.
            else if (textLower.contains("move jedi survivor here to a site")
                     || (textLower.contains("move jedi") && textLower.contains("to a site"))) {
                com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer hpTransit =
                    context.getObjectiveAnalyzer();
                boolean onHiddenPath = hpTransit != null && hpTransit.isAnalyzed()
                    && hpTransit.getObjectiveTitle() != null
                    && hpTransit.getObjectiveTitle().toLowerCase(Locale.ROOT).contains("hidden path");
                if (onHiddenPath) {
                    // V60 UPDATED 2026-07-06 T4.1: +9999 raised to +20000 = the MOVE-ladder R4
                    // MANDATORY TRANSIT band, so both transit arms (this game-text action and
                    // MoveEvaluator's V53b landspeed arms) share one band and beat any ME R3
                    // stack (≤ ~12000+2800+550) by construction instead of by statement order
                    // (move-3e boundary). V60 keeps ONLY this Hidden Path transit arm (ruling P3).
                    // OLD: action.addReasoning("V60 HIDDEN PATH TRANSIT: Move Jedi OUT of Corridor — flips objective!", 9999.0f);
                    action.addReasoning("V60 HIDDEN PATH TRANSIT: Move Jedi OUT of Corridor — flips objective! (R4 band)", 20000.0f);
                    logger.warn("V60 HIDDEN PATH TRANSIT: '{}' — +20000 (R4 band; CORRECT outward move, unlike landspeed)", actionText);
                } else {
                    action.addReasoning("Move Jedi transit action — tactical mobility", 200.0f);
                }
            }

            // ═══════════════════════════════════════════════════════════
            // ═══ REGION: PULL-ENGINE — V192 merged pull scorer + dead-search (reorg 2026-07-06) ═══
            // V192 (Steve + council, T4.2 pull-engine merge, 2026-07-06): ONE scorer for every
            // reserve-deck pull, hub-tag precedent V136/V153/V158/V159. Vetoes run FIRST and
            // short-circuit (hardBlocked); only then ONE positive line is emitted:
            //   BASE (+150 deploy-grade; +5500 PULL_BASE_ACTIVATE under the old V97 scope,
            //         with the P1 stand-down when V61c holds the destiny buffer)
            //   + TYPE TIER (location 1500/1400/1300/1200 by source cat; weapon 600; device 400)
            //   + CONTEXT (+50 [download]; +25 chars-in-hand during DEPLOY, the V100 rationale)
            //   clamped 1750 deploy-grade / 7100 activate-grade.
            // Absorbed tags (all old code commented in place — revert path, do not delete):
            //   V60-pull baseline, V82 +2500 grant, V95 dead-interrupt (folded as hardBlock),
            //   V97 +1500, V100 +1500, V116 +100 floor, V67l/V67ai location tiers,
            //   V67m/V67am weapon/device grants, V29.7 generic PULL FIRST +250.
            // V60 keeps ONLY its Hidden Path transit arm (+20000 R4, branch above). Veto-chain
            // lines keep their historical V-tags (V60 guards, V66, V67h, V67ac, V95, V131,
            // V67ar/V67ao/V149) for replay-grep continuity.
            // Dead-search verdicts still come from SVC-ORACLE (V177 -2000 skip-all upstream,
            // V131 tiers, V82.1-.3 rescue).
            // Cross-refs: ACTIVATE (V192 base 5500 must outrank V168 +5000; P1 stand-down uses
            // the SAME DecisionContext.isBattlePlausibleThisTurn() predicate as V61c),
            // SVC-ORACLE (facts), DEPLOY-3 (V120/V185 weapon gates). Boundary math:
            // resources/T4_Boundary_Tables_2026-07-06.md §T4.2. See also
            // resources/RANDO_REORG_PLAN_2026-07-02.md §3 + Rando_Section_Manifest_2026-07-06.xlsx.
            // ═══════════════════════════════════════════════════════════
            // ========== V192 RESERVE DECK PULLS — merged scorer (was V60 always-fire) ==========
            // Steve's rule (feedback_reserve_deck_pulls.md): Reserve Deck pull effects
            // are FREE VALUE — thin the deck, bring key cards into play. Always try them.
            // Covers [Download] actions (Sai'torr Kal Fas → matching weapon, Visage of
            // Emperor → lightsaber) and generic "X from Reserve Deck" / "Take X into hand"
            // actions (Mining Village → Tala Durith, Malachor STE → Padawan, IMBATS, etc.)
            // that weren't caught by earlier specific handlers.
            // Hard-block only when:
            //   1. DeckOracle confirms target NOT in Reserve (avoids deck reveal)
            //   2. Force can't cover the action cost (defer to next turn)
            //   3. This action has failed 2x in a row (shouldAvoidPulling)
            // V192 TRIGGER WIDENED 2026-07-06 to the union of the absorbed V97/V95/V116
            // triggers ("[upload]" + generic take-into-hand) so no absorbed rule loses
            // coverage. The Take-into-hand dispatch above now excludes reserve-deck takes
            // so they fall through to here (single owner). Old trigger commented out:
            // else if (textLower.contains("[download]")
            //          || (textLower.contains("from reserve deck") && !textLower.contains("shuffle"))
            //          || textLower.contains("take an effect into hand")
            //          || textLower.contains("take a character into hand")) {
            else if (textLower.contains("[download]")
                     || (textLower.contains("from reserve deck") && !textLower.contains("shuffle"))
                     || textLower.contains("[upload]")
                     || (textLower.contains("take") && textLower.contains("into hand"))) {

                // === V82 (Steve, 2026-05-16): EXPLICIT SOURCE-CARD SITE-PULL TRIGGER ===
                // Catches the case where the action text is GENERIC ("Deploy card from
                // Reserve Deck") but the SOURCE CARD'S game text describes a site /
                // location / battleground pull. This is what V67l's fallback was meant
                // to do via DeckOracle.parseSourceCardPullTargets — but in the Invasion
                // game (replay jdyn9tx3peavh6gd, 2026-05-16), action text was
                // "Deploy card from Reserve Deck" with no Naboo/site keyword, the
                // parser pipeline produced no firing, and Rando never used Invasion's
                // once-per-deploy-phase Naboo-site pull.
                //
                // V82 reads the source card blueprint directly and pattern-matches
                // "(site|location|battleground) [...] from reserve". When it
                // matches, score +2500 — dominates V60+V67l and any competing
                // deploy action. Per Steve: "If an effect lets rando pull a
                // location from his deck that should be a universal positive
                // points move."
                //
                // V82.1 (Steve, 2026-05-16): Dropped the (deploy|download|take)
                // verb anchor. Per Steve: "Just do 'from reserve deck' and don't
                // search for the text 'deploy' at all. This will cover any deploy
                // from reserve." Matches any pull phrasing.
                //
                // V82 UPDATED 2026-07-06: scoring MOVED below the V60 guards, inside the
                // if (!hardBlocked) region. Here it ran BEFORE hardBlocked even existed,
                // so Guard 1 (reserve <= 2, reveal risk) fired -400 and the +2500 still
                // outvoted it — Rando revealed his last 2 reserve cards (audit row
                // deploy-sequencing-1). OLD placement commented out per house rules:
                // {
                //     GameState v82Gs = context.getGameState();
                //     if (cardId != null && v82Gs != null) {
                //         try {
                //             PhysicalCard srcCard = v82Gs.findCardById(Integer.parseInt(cardId));
                //             if (srcCard != null && srcCard.getBlueprint() != null) {
                //                 String srcGt = srcCard.getBlueprint().getGameText();
                //                 if (srcGt != null) {
                //                     // V82.2 (Steve, 2026-05-16): added "docking bay" and "system|sector"
                //                     // — all are LOCATION-type pulls and deserve the +2500 boost.
                //                     // Begin Landing Your Troops pulls Episode I docking bays.
                //                     java.util.regex.Matcher v82m = java.util.regex.Pattern.compile(
                //                         "\\b(site|location|battleground|docking\\s+bay|system|sector)\\b[^.;]*?\\bfrom\\s+reserve",
                //                         java.util.regex.Pattern.CASE_INSENSITIVE).matcher(srcGt);
                //                     if (v82m.find()) {
                //                         String matched = v82m.group(1);
                //                         action.addReasoning(
                //                             "V82 SITE PULL: source '" + srcCard.getTitle()
                //                             + "' pulls a " + matched + " from Reserve — must take this every turn!",
                //                             2500.0f);
                //                         logger.warn("V82 SITE PULL: '{}' (src '{}', matched '{}') → +2500",
                //                             actionText, srcCard.getTitle(), matched);
                //                     }
                //                 }
                //             }
                //         } catch (NumberFormatException nfe) {
                //             // cardId not numeric (temp ID, etc.) — skip V82
                //         } catch (Exception e) {
                //             logger.debug("V82 SITE PULL error: {}", e.getMessage());
                //         }
                //     }
                // }

                com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle pullOracle = context.getDeckOracle();
                GameState pullGs = context.getGameState();
                boolean hardBlocked = false;

                // Guard 1: Reserve deck nearly empty (< 3 cards) — reveal risk
                if (pullGs != null && !hardBlocked) {
                    try {
                        int reserveSize = pullGs.getReserveDeckSize(context.getPlayerId());
                        if (reserveSize <= 2) {
                            // V60 UPDATED 2026-07-06: -400 → -9999, matching the DeployEvaluator
                            // copy of this exact guard (V60 RESERVE RISK, reserve <= 2). -400 was
                            // outvoted by the ungated V116 +100 / V100 +1500 / V97 +1500 stack
                            // (audit row deploy-sequencing-1); -9999 dominates all of them.
                            action.addReasoning("V60 RESERVE RISK: Reserve deck has " + reserveSize
                                + " cards — pull would reveal almost everything!", -9999.0f);
                            // OLD magnitude commented out 2026-07-06 (feedback_comment_out_old_rules):
                            // action.addReasoning("V60 RESERVE RISK: Reserve deck has " + reserveSize
                            //     + " cards — pull would reveal almost everything!", -400.0f);
                            logger.warn("V60 RESERVE RISK: '{}' — reserve {} cards — too risky (-9999)",
                                actionText, reserveSize);
                            hardBlocked = true;
                        }
                    } catch (Exception e) { /* ignore */ }
                }

                // Guard 2: Failed 2x in a row — stop pulling this specific action
                if (pullOracle != null && !hardBlocked) {
                    String failKey = "action:" + actionText;
                    if (pullOracle.shouldAvoidPulling(failKey)) {
                        action.addReasoning("V60 RESERVE FAIL-STOP: '" + actionText
                            + "' failed 2x — stop trying this game!", -9999.0f);
                        logger.warn("V60 RESERVE FAIL-STOP: '{}' has failed 2+ times — hard-blocked",
                            actionText);
                        hardBlocked = true;
                    }
                }

                // Guard 3: Named-target downloads (e.g., "Deploy Tala Durith from Reserve Deck")
                // — if DeckOracle shows the specific target is NOT in reserve, hard-block.
                // Only blocks MULTI-WORD proper-noun targets (case-sensitive match).
                // Generic placeholders like "card", "a farm", "a Padawan" are NOT blocked.
                if (!hardBlocked && pullOracle != null) {
                    java.util.regex.Matcher nameMatch = java.util.regex.Pattern.compile(
                        "(?:Deploy|Take) ([A-Z][A-Za-z']+ [A-Z][A-Za-z' -]+?) (?:from Reserve|into hand from Reserve)")
                        .matcher(actionText);
                    if (nameMatch.find()) {
                        String targetName = nameMatch.group(1).trim();
                        if (!pullOracle.hasTargetInReserve(targetName.split(" "))) {
                            action.addReasoning("V60 RESERVE MISS: '" + targetName
                                + "' is NOT in Reserve Deck — pull will fail and reveal deck!", -9999.0f);
                            logger.warn("V60 RESERVE MISS: Target '{}' not in reserve — hard-blocked (reveal risk)",
                                targetName);
                            hardBlocked = true;
                        }
                    }
                }

                // V66 MEMORY AUDIT: Unified hand/in-play check for [Download] and pull actions.
                // Catches the common "already deployed" case that Guards 1-3 miss.
                // Example: Sai'torr Kal Fas "[Download] a matching weapon" when the matching
                // weapon (Obi-Wan's Lightsaber) is already attached to Obi-Wan — search fails
                // and reveals reserve.
                if (!hardBlocked && pullOracle != null && pullOracle.isAnalyzed()) {
                    com.gempukku.swccgo.common.Zone v66Zone =
                        com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle.parseSourceZone(actionText);
                    if (v66Zone != null) {
                        String[] v66Keywords = null;
                        java.util.regex.Matcher v66Named = java.util.regex.Pattern.compile(
                            "(?:Deploy|Take) ([A-Z][A-Za-z']+ [A-Z][A-Za-z' -]+?) "
                                + "(?:from Reserve|from Lost|from Used|from Force|into hand from)")
                            .matcher(actionText);
                        if (v66Named.find()) {
                            v66Keywords = v66Named.group(1).trim().split(" ");
                        } else {
                            java.util.regex.Matcher v66Gen = java.util.regex.Pattern.compile(
                                "(?:Deploy|Take|\\[Download\\]) an? ([a-z]+) ?")
                                .matcher(actionText);
                            if (v66Gen.find()) {
                                String kw = v66Gen.group(1).trim();
                                // === V123 (Steve, 2026-05-22): V66 STOPWORD GUARD ===
                                // Generic category nouns are NOT card titles. Without this
                                // guard, V66 extracts "location" from "Deploy a location from
                                // Reserve Deck" (Hunt Down V's site-pull action), looks for
                                // a card LITERALLY titled "location" in reserve, finds none,
                                // and hard-blocks the action with -9999 — silently killing
                                // every Hunt Down V site pull for the entire game.
                                // The validatePull call only succeeds for named cards. For
                                // generic category words like "location"/"site"/"weapon"/
                                // "lightsaber", let downstream V67ai (tiered objective bonus)
                                // and V82 (source-card site-pull match) do the criteria-aware
                                // validation that V66's title-lookup can't.
                                java.util.Set<String> v66Stopwords = new java.util.HashSet<>(java.util.Arrays.asList(
                                    "location", "site", "battleground", "system", "sector",
                                    "ship", "starship", "vehicle", "transport", "fighter",
                                    "weapon", "lightsaber", "blaster", "bowcaster", "device",
                                    "character", "alien", "droid", "jedi", "sith", "padawan",
                                    "inquisitor", "senator", "pilot", "warrior", "soldier",
                                    "leader", "admiral", "general", "trooper", "officer",
                                    "rebel", "imperial", "scout", "spy",
                                    "effect", "interrupt", "objective", "epic", "shield",
                                    "card"
                                ));
                                if (v66Stopwords.contains(kw.toLowerCase(java.util.Locale.ROOT))) {
                                    logger.info("V123 V66 STOPWORD: '{}' is a generic category — skip V66 title lookup, defer to V67ai/V82",
                                        kw);
                                } else if (kw.length() >= 3) {
                                    v66Keywords = new String[] { kw };
                                }
                            }
                        }
                        if (v66Keywords != null && v66Keywords.length > 0) {
                            com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle.PullValidation v66Result =
                                pullOracle.validatePull(v66Zone, v66Keywords);
                            if (v66Result.outcome ==
                                com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle.PullOutcome.WILL_FAIL) {
                                action.addReasoning("V66 MEMORY: " + v66Result.reason, -9999.0f);
                                logger.warn("V66 MEMORY WILL_FAIL: '{}' — {}", actionText, v66Result.reason);
                                hardBlocked = true;
                            } else if (v66Result.outcome ==
                                com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle.PullOutcome.WASTEFUL) {
                                action.addReasoning("V66 MEMORY: " + v66Result.reason, -800.0f);
                                logger.warn("V66 MEMORY WASTEFUL: '{}' — {} (-800)", actionText, v66Result.reason);
                            }
                        }
                    }

                    // V67h: When the action is generic, use the SOURCE CARD's game text
                    // to determine what the filter targets. Catches cases where the regex
                    // can't extract a useful keyword from the displayed action text.
                    if (!hardBlocked && cardId != null && pullGs != null) {
                        try {
                            PhysicalCard sourceCard = pullGs.findCardById(Integer.parseInt(cardId));
                            if (sourceCard != null && sourceCard.getBlueprint() != null) {
                                String v67hGT = com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle
                                    .getSourceCardFullGameText(sourceCard.getBlueprint(), context.getSide());
                                if (v67hGT != null) {
                                    com.gempukku.swccgo.common.Zone v67hZone =
                                        com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle.parseSourceZone(actionText);
                                    if (v67hZone != null) {
                                        com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle.PullValidation v67hRes =
                                            pullOracle.validatePullFromSourceCard(v67hZone, v67hGT);
                                        if (v67hRes.outcome ==
                                            com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle.PullOutcome.WILL_FAIL) {
                                            action.addReasoning("V67h MEMORY (game-text): " + v67hRes.reason, -9999.0f);
                                            logger.warn("V67h MEMORY WILL_FAIL: source={} — {}",
                                                sourceCard.getTitle(), v67hRes.reason);
                                            hardBlocked = true;
                                        } else if (v67hRes.outcome ==
                                            com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle.PullOutcome.WILL_SUCCEED) {
                                            // V185 MIRROR (ADJUSTED 2026-07-10, Rey replay rbujmoc90br3uu4c):
                                            // twin drift — DeployEvaluator's V185 no-holder weapon veto had NO
                                            // counterpart here, so V192's additive bonus (+1575) diluted the
                                            // Deploy-side -2000 to net -375 and a vetoed dead saber pull nearly
                                            // fired. Run the SAME gate on this route: a WILL_SUCCEED whose
                                            // Reserve targets are all unattachable weapons is a dead pull.
                                            boolean v185Mirror = false;
                                            try {
                                                java.util.List<String> v185T =
                                                    com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle
                                                        .parseSourceCardPullTargets(v67hGT);
                                                v185Mirror = v67hZone == com.gempukku.swccgo.common.Zone.RESERVE_DECK
                                                    && context.getGame() != null && context.getPlayerId() != null
                                                    && pullOracle.reserveTargetsAreAllUnattachableWeapons(
                                                        context.getGame(), context.getPlayerId(), v185T);
                                            } catch (Exception v185E) { /* fail-open */ }
                                            if (v185Mirror) {
                                                action.addReasoning("V185 (ATE mirror): all Reserve targets are "
                                                    + "weapons with no legal holder — dead pull", -9999.0f);
                                                logger.warn("V185 ATE MIRROR: source={} — blocking dead weapon pull",
                                                    sourceCard.getTitle());
                                                hardBlocked = true;
                                            } else {
                                                logger.info("V67h MEMORY OK: source={} — {}",
                                                    sourceCard.getTitle(), v67hRes.reason);
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (NumberFormatException nfe) { /* ignore */ }
                        catch (Exception e) { logger.debug("V67h: error: {}", e.getMessage()); }
                    }
                }

                // V67ac (Steve, 2026-05-04): FORCE-COST GUARD for card-action reserve pulls.
                // Symptom: Rando used Vader's Castle's 'deploy Vader from Reserve Deck'
                // action with only 4 force in pile. Vader costs 7 (6 with Castle reduction).
                // Action FAILED but the search revealed Rando's reserve deck to opponent.
                // V67h validates target EXISTS in zone but doesn't validate AFFORDABILITY.
                //
                // Approach: scan Rando's reserve deck for cards matching source card's
                // parsed targets. Find the cheapest match. If even the cheapest exceeds
                // available force pile size, hard-block (action would fail + leak reserve).
                if (!hardBlocked && cardId != null && pullGs != null) {
                    try {
                        PhysicalCard ssrc = pullGs.findCardById(Integer.parseInt(cardId));
                        if (ssrc != null && ssrc.getBlueprint() != null) {
                            String ssrcGT = com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle
                                .getSourceCardFullGameText(ssrc.getBlueprint(), context.getSide());
                            String ssrcTitleLower = ssrc.getTitle() != null
                                ? ssrc.getTitle().toLowerCase(java.util.Locale.ROOT) : "";
                            if (ssrcGT != null) {
                                java.util.List<String> ssrcTargets =
                                    com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle
                                        .parseSourceCardPullTargets(ssrcGT);
                                if (!ssrcTargets.isEmpty() && context.getPlayerId() != null) {
                                    // Available force = force pile size (cards lost top-down)
                                    int availForce = 0;
                                    try { availForce = pullGs.getForcePileSize(context.getPlayerId()); }
                                    catch (Exception e) { /* ignore */ }

                                    // Find cheapest matching target in reserve deck.
                                    Integer cheapestCost = null;
                                    java.util.List<PhysicalCard> reserve = null;
                                    try { reserve = pullGs.getReserveDeck(context.getPlayerId()); }
                                    catch (Exception e) { /* ignore */ }
                                    if (reserve != null) {
                                        for (PhysicalCard rc : reserve) {
                                            if (rc == null || rc.getBlueprint() == null) continue;
                                            String rcTitleLower = rc.getTitle() != null
                                                ? rc.getTitle().toLowerCase(java.util.Locale.ROOT) : "";
                                            // Match: any target keyword (icon-stripped) matches title
                                            boolean matches = false;
                                            for (String t : ssrcTargets) {
                                                String tl = t.toLowerCase(java.util.Locale.ROOT);
                                                String stripped = tl.replaceAll("\\[[^\\]]*\\]", " ")
                                                    .replaceAll("\\s+", " ").trim();
                                                if (rcTitleLower.contains(tl)
                                                        || (!stripped.isEmpty() && rcTitleLower.contains(stripped))) {
                                                    matches = true;
                                                    break;
                                                }
                                            }
                                            if (!matches) continue;
                                            try {
                                                Float dc = rc.getBlueprint().getDeployCost();
                                                if (dc != null) {
                                                    int icost = dc.intValue();
                                                    // Apply common -1 reduction if source card text says so
                                                    if (ssrcGT.toLowerCase(java.util.Locale.ROOT).contains("less force")
                                                            || ssrcGT.toLowerCase(java.util.Locale.ROOT).contains("deploy -1")) {
                                                        icost = Math.max(0, icost - 1);
                                                    }
                                                    if (cheapestCost == null || icost < cheapestCost) {
                                                        cheapestCost = icost;
                                                    }
                                                }
                                            } catch (Exception ee) { /* card type may not support deploy cost */ }
                                        }
                                    }

                                    if (cheapestCost != null && cheapestCost > availForce) {
                                        action.addReasoning(String.format(
                                            "V67ac CAN'T AFFORD: '%s' would deploy a target costing %d Force, only %d available — search reveals reserve!",
                                            ssrc.getTitle(), cheapestCost, availForce), -9999.0f);
                                        logger.warn("V67ac FORCE-COST BLOCK: source={} cheapestTargetCost={} availForce={} — block (would leak reserve)",
                                            ssrc.getTitle(), cheapestCost, availForce);
                                        hardBlocked = true;
                                    }
                                }
                            }
                        }
                    } catch (Exception e) { logger.debug("V67ac error: {}", e.getMessage()); }
                }

                // === V95 (Steve, 2026-05-20; folded into the V192 veto chain 2026-07-06):
                // SAVE DEAD INTERRUPTS WHEN RESERVES >= 15 ===
                // Moved from the standalone top-of-loop block (commented out near line ~470)
                // into this chain as a hardBlock. Old placement was ADDITIVE: the pull pile
                // (V116+V97+V60 dl+V29.7 = +2100) outvoted the -2000 to +100 and the dead
                // pull FIRED, revealing reserve (boundary row 5). As a hardBlock the -2000
                // stands alone and Pass wins. Logic verbatim from the old block.
                if (!hardBlocked && cardId != null && pullGs != null) {
                    try {
                        PhysicalCard v95Src = pullGs.findCardById(Integer.parseInt(cardId));
                        if (v95Src != null && v95Src.getBlueprint() != null
                                && v95Src.getBlueprint().getCardCategory()
                                    == com.gempukku.swccgo.common.CardCategory.INTERRUPT) {
                            String v95Gt = com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle
                                .getSourceCardFullGameText(v95Src.getBlueprint(), context.getSide());
                            if (v95Gt != null) {
                                java.util.List<String> v95Targets =
                                    com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle
                                        .parseSourceCardPullTargets(v95Gt);
                                if (!v95Targets.isEmpty()) {
                                    java.util.Collection<PhysicalCard> v95Table = pullGs.getAllPermanentCards();
                                    boolean v95AllOnTable = true;
                                    for (String t : v95Targets) {
                                        String tl = t.toLowerCase(java.util.Locale.ROOT);
                                        boolean found = false;
                                        for (PhysicalCard tc : v95Table) {
                                            if (tc == null || tc.getTitle() == null) continue;
                                            if (tc.getTitle().toLowerCase(java.util.Locale.ROOT).contains(tl)) {
                                                found = true; break;
                                            }
                                        }
                                        if (!found) { v95AllOnTable = false; break; }
                                    }
                                    if (v95AllOnTable) {
                                        String v95Pid = context.getPlayerId();
                                        int v95ReserveForce = pullGs.getForcePileSize(v95Pid)
                                            + (pullGs.getUsedPile(v95Pid) != null ? pullGs.getUsedPile(v95Pid).size() : 0)
                                            + pullGs.getReserveDeckSize(v95Pid);
                                        if (v95ReserveForce >= 15) {
                                            action.addReasoning(String.format(
                                                "V95 DEAD INTERRUPT SAVE: '%s' pull targets %s all on table, reserves=%d — save for force-loss fodder",
                                                v95Src.getTitle(), v95Targets, v95ReserveForce), -2000.0f);
                                            logger.warn("V95 DEAD INTERRUPT: blocking {} (targets on table, reserves {}) — hardBlock in V192 chain",
                                                v95Src.getTitle(), v95ReserveForce);
                                            hardBlocked = true;
                                        }
                                    }
                                }
                            }
                        }
                    } catch (NumberFormatException nfe) {
                        // not numeric cardId
                    } catch (Exception e) {
                        logger.debug("V95 error: {}", e.getMessage());
                    }
                }

                if (!hardBlocked) {
                    // ═══ V192 PULL SCORER (Steve + council, 2026-07-06) ═══
                    // All positives below feed ONE addReasoning at the end of this block
                    // (see "V192 SINGLE EMIT"). Detection predicates (V67l keywords, V82
                    // regex, V67m/V67am keywords, V131 deck-aware gate) survive as inputs;
                    // their old standalone grants are commented out in place.
                    boolean isFreeDownload = textLower.contains("[download]");
                    float v192Tier = 0.0f;
                    String v192TierDesc = "none";
                    boolean v192IsLocationTier = false;
                    // V60 baseline commented out 2026-07-06 (absorbed into V192 base —
                    // feedback_comment_out_old_rules):
                    // float baseline = isFreeDownload ? 250.0f : 150.0f;
                    // action.addReasoning("V60 RESERVE PULL: '" + actionText
                    //     + "' — thin deck, bring value into play!", baseline);
                    // logger.warn("V60 RESERVE PULL: '{}' scored +{} — pull every turn!",
                    //     actionText, (int)baseline);

                    // === V82 EXPLICIT SOURCE-CARD SITE-PULL TRIGGER ===
                    // V82 UPDATED 2026-07-06 (earlier today): moved here from ABOVE the V60
                    // guards so it respects hardBlocked. Full rationale lives in the original
                    // V82/V82.1/V82.2 comment block above the guards.
                    // V82 UPDATED AGAIN 2026-07-06 (V192 merge): the standalone +2500 grant is
                    // ABSORBED — the regex now only feeds the shared isLocationPull predicate
                    // (V67l keyword list ∪ THIS regex ∪ V100's "planet"), and the LOCATION tier
                    // value comes from the single V192 tier table below. Old grant commented out.
                    boolean v192LocByV82 = false;
                    String v192V82Noun = null;
                    String v192V82SrcTitle = null;
                    {
                        GameState v82Gs = context.getGameState();
                        if (cardId != null && v82Gs != null) {
                            try {
                                PhysicalCard srcCard = v82Gs.findCardById(Integer.parseInt(cardId));
                                if (srcCard != null && srcCard.getBlueprint() != null) {
                                    String srcGt = com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle
                                        .getSourceCardFullGameText(srcCard.getBlueprint(), context.getSide());
                                    if (srcGt != null) {
                                        // V82.2 (Steve, 2026-05-16): added "docking bay" and "system|sector"
                                        // — all are LOCATION-type pulls and deserve the LOCATION tier.
                                        // Begin Landing Your Troops pulls Episode I docking bays.
                                        java.util.regex.Matcher v82m = java.util.regex.Pattern.compile(
                                            "\\b(site|location|battleground|docking\\s+bay|system|sector)\\b[^.;]*?\\bfrom\\s+reserve",
                                            java.util.regex.Pattern.CASE_INSENSITIVE).matcher(srcGt);
                                        if (v82m.find()) {
                                            v192LocByV82 = true;
                                            v192V82Noun = v82m.group(1);
                                            v192V82SrcTitle = srcCard.getTitle();
                                            logger.info("V82 SITE PULL (V192 predicate): '{}' (src '{}', matched '{}') — feeds LOCATION tier",
                                                actionText, srcCard.getTitle(), v192V82Noun);
                                            // Old +2500 grant commented out 2026-07-06 (V192 merge):
                                            // action.addReasoning(
                                            //     "V82 SITE PULL: source '" + srcCard.getTitle()
                                            //     + "' pulls a " + matched + " from Reserve — must take this every turn!",
                                            //     2500.0f);
                                            // logger.warn("V82 SITE PULL: '{}' (src '{}', matched '{}') → +2500",
                                            //     actionText, srcCard.getTitle(), matched);
                                        }
                                    }
                                }
                            } catch (NumberFormatException nfe) {
                                // cardId not numeric (temp ID, etc.) — skip V82
                            } catch (Exception e) {
                                logger.debug("V82 SITE PULL error: {}", e.getMessage());
                            }
                        }
                    }

                    // V67l UNIVERSAL LOCATION-PULL PRIORITY (mirrors DeployEvaluator V67i)
                    // Steve's rule: "If an effect lets rando pull a location from his deck
                    // that should be a universal positive points move. He should do this
                    // as the first part of his deploy phase."
                    // Detection: action text or source-card game text contains a location
                    // keyword in its target list. V192 (2026-07-06): this detection is now
                    // the shared isLocationPull predicate (V67l list ∪ V82 regex ∪ V100's
                    // "planet") feeding the single LOCATION tier — no standalone bonus.
                    boolean v67lAddsLocation = false;
                    String v67lReason = null;
                    String[] v67lLocationKeywords = new String[] {
                        "site", "battleground", "location", "system", "farm",
                        "cantina", "mos eisley", "tatooine", "endor", "hoth",
                        "dagobah", "naboo", "yavin", "bespin", "cloud city",
                        "mustafar", "malachor", "mapuzo", "jabiim", "coruscant",
                        "kashyyyk", "kessel", "kamino", "geonosis", "alderaan",
                        "docking bay", "spaceport", "city", "palace", "temple",
                        "safehouse", "corridor", "village", "outpost",
                        // V192 (2026-07-06): union with V82 regex nouns + V100 vocabulary —
                        // "sector" (V82.2) and "planet" (V100) were detectable by the absorbed
                        // rules but missing from this list.
                        "sector", "planet"
                    };
                    for (String kw : v67lLocationKeywords) {
                        if (textLower.contains(kw)) {
                            v67lAddsLocation = true;
                            v67lReason = "actionText contains location keyword '" + kw + "'";
                            break;
                        }
                    }
                    // Fallback: parse source card game text
                    if (!v67lAddsLocation && cardId != null && pullGs != null) {
                        try {
                            PhysicalCard sc = pullGs.findCardById(Integer.parseInt(cardId));
                            if (sc != null && sc.getBlueprint() != null) {
                                String gt = com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle
                                    .getSourceCardFullGameText(sc.getBlueprint(), context.getSide());
                                if (gt != null) {
                                    java.util.List<String> tgts = com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle
                                        .parseSourceCardPullTargets(gt);
                                    for (String t : tgts) {
                                        for (String kw : v67lLocationKeywords) {
                                            if (t.contains(kw)) {
                                                v67lAddsLocation = true;
                                                v67lReason = "source card '" + sc.getTitle()
                                                    + "' game text targets location-like '" + t + "'";
                                                break;
                                            }
                                        }
                                        if (v67lAddsLocation) break;
                                    }
                                }
                            }
                        } catch (Exception e) { /* ignore */ }
                    }
                    // V192 (2026-07-06): merge the V82 regex hit into the shared predicate so
                    // V131's deck-aware gate covers it too (before the merge, a V82-only match
                    // bypassed V131 entirely — the +2500 fired even on satisfied targets).
                    if (!v67lAddsLocation && v192LocByV82) {
                        v67lAddsLocation = true;
                        v67lReason = "V82 source-text regex: '" + v192V82SrcTitle
                            + "' pulls a " + v192V82Noun + " from Reserve";
                    }
                    // === V131 (Steve, 2026-05-25): DECK-AWARE PULL DETECTION (three-tier) ===
                    //
                    // Gates the LOCATION tier (was V67ai's bonus) on actual deck-state checks:
                    //   Tier 1 HARD BLOCK -9999: target proven not in deck at all
                    //     (countMatchingInDeck == 0). The effect would fail and reveal
                    //     reserve. Example: Emperor's "search reserve for Force Lightning"
                    //     when Force Lightning was never in this deck.
                    //   Tier 2 SOFT DOWNGRADE: target IS in deck but already satisfied
                    //     (countMatchingInHandOrTable >= 1). Pulling more is wasted.
                    //     Subtract bonus to net zero.
                    //   Tier 3 EXISTING: target needed → V67ai bonus fires as before.
                    //
                    // Also CORRECTS V67l's substring-match misfire: only fire LOCATION
                    // tier when parsed target actually resolves to a location-family
                    // noun (not weapon, character, etc). Bug 5 (Cunning Warrior pulled
                    // a lightsaber while game text mentioned "Cloud City Corridor")
                    // was caused by this misclassification.
                    //
                    // FAIL-OPEN: noun unparseable, filter null, deck data missing → fall
                    // through to existing behavior. NEVER hard-block on ambiguity.
                    boolean v131GateOpen = v67lAddsLocation;  // mirror current state
                    boolean v131HardBlock = false;
                    boolean v131DowngradeBonus = false;
                    String v131Reason = null;
                    if (v67lAddsLocation && cardId != null && pullGs != null
                            && context.getGame() != null && context.getPlayerId() != null) {
                        try {
                            PhysicalCard v131Src = pullGs.findCardById(Integer.parseInt(cardId));
                            if (v131Src != null && v131Src.getBlueprint() != null) {
                                String v131Gt = com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle
                                    .getSourceCardFullGameText(v131Src.getBlueprint(), context.getSide());
                                if (v131Gt != null) {
                                    java.util.List<String> v131Tgts =
                                        com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle
                                            .parseSourceCardPullTargets(v131Gt);
                                    // Location-family nouns. If parsed target contains any of
                                    // these as a SUBSTRING, treat as location pull.
                                    java.util.Set<String> v131LocNouns = java.util.Set.of(
                                        "location", "site", "system", "sector", "battleground",
                                        "docking bay", "outpost", "spaceport", "palace",
                                        "temple", "safehouse", "corridor", "village");
                                    // Weapon-family nouns. If target matches, V67l should NOT
                                    // fire (this is a weapon pull, not location pull).
                                    java.util.Set<String> v131WeaponNouns = java.util.Set.of(
                                        "weapon", "lightsaber", "blaster", "bowcaster",
                                        "rifle", "vibroblade", "vibro-blade");
                                    boolean v131SawLocTarget = false;
                                    boolean v131SawWeaponTarget = false;
                                    com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle v131Oracle =
                                        context.getDeckOracle();
                                    for (String v131T : v131Tgts) {
                                        if (v131T == null) continue;
                                        String v131Tl = v131T.toLowerCase(java.util.Locale.ROOT);
                                        boolean isLoc = false;
                                        for (String n : v131LocNouns) {
                                            if (v131Tl.contains(n)) { isLoc = true; break; }
                                        }
                                        boolean isWeap = false;
                                        for (String n : v131WeaponNouns) {
                                            if (v131Tl.contains(n)) { isWeap = true; break; }
                                        }
                                        if (isWeap) v131SawWeaponTarget = true;
                                        if (isLoc) {
                                            v131SawLocTarget = true;
                                            // Resolve to Filter for deck-aware check
                                            com.gempukku.swccgo.filters.Filter v131F = com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle
                                                .resolveCommonNounToFilter(v131T);
                                            if (v131F != null && v131Oracle != null) {
                                                int v131InDeck = v131Oracle.countMatchingInDeck(
                                                    context.getGame(), context.getPlayerId(), v131F);
                                                if (v131InDeck == 0) {
                                                    v131HardBlock = true;
                                                    v131Reason = "target '" + v131T + "' not in deck at all";
                                                    break;
                                                }
                                                int v131InHandTable = v131Oracle.countMatchingInHandOrTable(
                                                    context.getGame(), context.getPlayerId(), v131F);
                                                if (v131InHandTable >= 1) {
                                                    v131DowngradeBonus = true;
                                                    v131Reason = "target '" + v131T + "' already satisfied (" + v131InHandTable + " in hand+table)";
                                                }
                                            }
                                        }
                                    }
                                    // V131 gate: if parsed targets are weapon-only (no location),
                                    // V67l misclassified — close the gate so V67ai doesn't fire.
                                    if (v131SawWeaponTarget && !v131SawLocTarget) {
                                        v131GateOpen = false;
                                        v131Reason = "parsed targets are weapon-only; V67l mis-detected";
                                        logger.info("V131 GATE: closing v67lAddsLocation — {}", v131Reason);
                                    }
                                }
                            }
                        } catch (Exception ev131) { /* fail-open */ }
                    }
                    if (v131HardBlock) {
                        action.addReasoning(
                            "V131 DECK-AWARE HARD BLOCK: " + v131Reason
                                + " — pull would fail and reveal reserve",
                            -9999.0f);
                        logger.warn("V131 DECK-AWARE HARD BLOCK: '{}' → -9999 ({})",
                            actionText, v131Reason);
                        v131GateOpen = false;  // also suppress the LOCATION tier
                        // V192 (2026-07-06): joins the veto chain — structurally suppresses
                        // ALL scorer positives, not just the location tier.
                        hardBlocked = true;
                    }
                    if (v131DowngradeBonus) {
                        // V131 Tier 2 RE-WIRED 2026-07-06 (V192 merge): the old additive -2000
                        // was DROWNED by the +8000 pile (boundary row 3: downgraded pull still
                        // scored +5200..+6000 and burned the once-per-turn download on a
                        // satisfied target). Now the downgrade flag makes the V192 SINGLE EMIT
                        // below suppress ALL positives and emit -200 instead — the pull loses
                        // to Pass by arithmetic, not by a constant race. Old additive penalty
                        // commented out (feedback_comment_out_old_rules):
                        // action.addReasoning(
                        //     "V131 DECK-AWARE SOFT DOWNGRADE: " + v131Reason
                        //         + " — neutralize LOCATION bonus",
                        //     -2000.0f);
                        logger.info("V131 DECK-AWARE DOWNGRADE (V192 structural): '{}' → positives suppressed ({})",
                            actionText, v131Reason);
                    }
                    if (v131GateOpen && !hardBlocked) {
                        // V67ai (Steve, 2026-05-07): TIERED LOCATION DEPLOY ORDER — ABSORBED
                        // into the V192 tier table 2026-07-06 (T4.2 pull-engine merge).
                        //
                        // Steve's rule: 'Rando should never under any circumstances avoid
                        // deploying locations.' Location-pull cards keep a strict priority
                        // order (Objective → Effect → Interrupt → Hand), but the values are
                        // RE-SIZED so the deploy-window pull total (base 150 + tier + ctx <=
                        // 1750) stays BELOW the hand-location anchor 1950 (DE base 50 + V162
                        // +500 + V67ai Tier4 HAND +1400, untouched in DeployEvaluator — the
                        // V179 lesson: never let a download outrank a held location).
                        //   Tier 1: Objective pull   → +1500 (was +2000)
                        //   Tier 2: Effect/Location  → +1400 (was +1800)
                        //   Tier 3: Interrupt pull   → +1300 (was +1600)
                        //   unknown source           → +1200 (was +1500, legacy V67l value)
                        //
                        // Determine source category from the source card's blueprint.
                        int v67aiTier = 0;
                        String v67aiTierName = "unclassified";
                        if (cardId != null && pullGs != null) {
                            try {
                                PhysicalCard srcPc = pullGs.findCardById(Integer.parseInt(cardId));
                                if (srcPc != null && srcPc.getBlueprint() != null) {
                                    com.gempukku.swccgo.common.CardCategory srcCat =
                                        srcPc.getBlueprint().getCardCategory();
                                    if (srcCat == com.gempukku.swccgo.common.CardCategory.OBJECTIVE) {
                                        v67aiTier = 1; v67aiTierName = "OBJECTIVE";
                                    } else if (srcCat == com.gempukku.swccgo.common.CardCategory.EFFECT) {
                                        v67aiTier = 2; v67aiTierName = "EFFECT";
                                    } else if (srcCat == com.gempukku.swccgo.common.CardCategory.INTERRUPT) {
                                        v67aiTier = 3; v67aiTierName = "INTERRUPT";
                                    } else if (srcCat == com.gempukku.swccgo.common.CardCategory.LOCATION) {
                                        // A location pulling another location (e.g., Vader's Castle's
                                        // 'download' a Hoth/Endor sub-site). Treat as effect-tier.
                                        v67aiTier = 2; v67aiTierName = "LOCATION-EFFECT";
                                    }
                                }
                            } catch (Exception e) { /* fall through to default */ }
                        }
                        // Old V67ai magnitudes commented out 2026-07-06 (V192 re-size):
                        // switch (v67aiTier) {
                        //     case 1: v67aiBonus = 2000.0f; break;
                        //     case 2: v67aiBonus = 1800.0f; break;
                        //     case 3: v67aiBonus = 1600.0f; break;
                        //     default: v67aiBonus = 1500.0f; break;  // legacy V67l score for unknown sources
                        // }
                        switch (v67aiTier) {
                            case 1: v192Tier = 1500.0f; break;
                            case 2: v192Tier = 1400.0f; break;
                            case 3: v192Tier = 1300.0f; break;
                            default: v192Tier = 1200.0f; break;
                        }
                        v192IsLocationTier = true;
                        v192TierDesc = String.format("LOCATION Tier %d %s — %s",
                            v67aiTier, v67aiTierName, v67lReason);
                        // Old standalone emit commented out 2026-07-06 (absorbed into the
                        // single V192 line below):
                        // action.addReasoning(
                        //     String.format("V67ai LOCATION DEPLOY ORDER [Tier %d %s]: %s — ALWAYS pull locations FIRST, in order: Objective → Effect → Interrupt → Hand!",
                        //         v67aiTier, v67aiTierName, v67lReason), v67aiBonus);
                        // logger.warn("V67ai LOCATION TIER {} [{}]: '{}' → +{} ({})",
                        //     v67aiTier, v67aiTierName, actionText, (int) v67aiBonus, v67lReason);
                    }

                    // === V67m UNIVERSAL WEAPON-PULL PRIORITY ===
                    // Steve's rule: "There are other cards that pull weapons from reserve,
                    // after location pulls and character deploys, we should use those
                    // effects to deploy weapons from reserve with positive points."
                    //
                    // Score +200 — positive but below character deploy peaks (+300-500)
                    // so chars deploy first. Same dual-source detection as V67l.
                    boolean v67mAddsWeapon = false;
                    String v67mReason = null;
                    String[] v67mWeaponKeywords = new String[] {
                        "weapon", "lightsaber", "saber", "blaster",
                        "rifle", "pistol", "cannon", "bowcaster",
                        "thermal detonator", "vibroblade", "vibro-",
                        "force pike", "electrostaff"
                    };
                    for (String kw : v67mWeaponKeywords) {
                        if (textLower.contains(kw)) {
                            v67mAddsWeapon = true;
                            v67mReason = "actionText contains weapon keyword '" + kw + "'";
                            break;
                        }
                    }
                    // Fallback: source card game text
                    if (!v67mAddsWeapon && cardId != null && pullGs != null) {
                        try {
                            PhysicalCard sc = pullGs.findCardById(Integer.parseInt(cardId));
                            if (sc != null && sc.getBlueprint() != null) {
                                String gt = com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle
                                    .getSourceCardFullGameText(sc.getBlueprint(), context.getSide());
                                if (gt != null) {
                                    java.util.List<String> tgts = com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle
                                        .parseSourceCardPullTargets(gt);
                                    for (String t : tgts) {
                                        for (String kw : v67mWeaponKeywords) {
                                            if (t.contains(kw)) {
                                                v67mAddsWeapon = true;
                                                v67mReason = "source card '" + sc.getTitle()
                                                    + "' game text targets weapon-like '" + t + "'";
                                                break;
                                            }
                                        }
                                        if (v67mAddsWeapon) break;
                                    }
                                }
                            }
                        } catch (Exception e) { /* ignore */ }
                    }
                    // V67am (Steve, 2026-05-07): Bumped V67m weapon-pull bonus +200 → +600.
                    //
                    // Steve's order: 'pull weapon from reserve via effect/interrupt/objective
                    // FIRST, then deploy from hand.' Old V67m at +200 was below hand-deploy
                    // bonuses (V29.11 LIGHTSABER +400-500), inverting Steve's priority.
                    //
                    // +600 ensures pull-from-reserve actions outscore hand-deploy of the
                    // same weapon class. Once-per-game/turn pull effects are precious — fire
                    // them first while available; hand cards can deploy any turn.
                    if (v67mAddsWeapon && !v67lAddsLocation) {
                        // V67ar (Steve, 2026-05-08): UNIVERSAL ONE-WEAPON RULE for pull path.
                        // Mirrors V67aq's logic from DeployEvaluator. Count UNARMED Rando
                        // characters on table — if zero unarmed (every char already armed),
                        // hard-block the pull because it would put a 2nd weapon on someone.
                        // Also blocks the 'no chars at all' case (V67ao original intent).
                        //
                        // No hardcoded character names. The same rule fires regardless of
                        // which weapon (Sidious' Lightsaber, Ventress' Lightsabers,
                        // Vader's Lightsaber, anything) and which character the pull would
                        // target.
                        // V149 (Steve, 2026-05-28, REVISED): lightsaber pull needs a
                        // capable wielder = unarmed [Warrior] icon AND ability >= 4.
                        // Steve: "warriors have icons indicating they can carry weapons —
                        // warrior type with ability >= 4." Jedi/Sith carry [Warrior];
                        // cantina aliens don't. Mirror of DeployEvaluator V149.
                        boolean v149IsLightsaberPull = false;
                        if (v67mReason != null) {
                            String v149r = v67mReason.toLowerCase(java.util.Locale.ROOT);
                            v149IsLightsaberPull = v149r.contains("lightsaber") || v149r.contains("saber");
                        }

                        int v67arUnarmed = 0;
                        int v67arArmed = 0;
                        int v149AbilityCapableUnarmed = 0;
                        if (pullGs != null && context.getPlayerId() != null) {
                            try {
                                for (PhysicalCard pc : pullGs.getAllPermanentCards()) {
                                    if (pc == null || pc.getBlueprint() == null) continue;
                                    if (!context.getPlayerId().equals(pc.getOwner())) continue;
                                    com.gempukku.swccgo.common.Zone z = pc.getZone();
                                    if (z == null || !z.isInPlay()) continue;
                                    if (pc.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                                    boolean armed = false;
                                    java.util.List<PhysicalCard> atts = pullGs.getAttachedCards(pc);
                                    if (atts != null) {
                                        for (PhysicalCard a : atts) {
                                            if (a != null && a.getBlueprint() != null
                                                    && a.getBlueprint().getCardCategory() == CardCategory.WEAPON) {
                                                armed = true;
                                                break;
                                            }
                                        }
                                    }
                                    if (armed) v67arArmed++;
                                    else {
                                        v67arUnarmed++;
                                        // V149: lightsaber wielder = [Warrior] icon AND ability >= 4
                                        if (v149IsLightsaberPull
                                                && pc.getBlueprint().hasIcon(com.gempukku.swccgo.common.Icon.WARRIOR)
                                                && pc.getBlueprint().hasAbilityAttribute()) {
                                            Float v149ab = pc.getBlueprint().getAbility();
                                            if (v149ab != null && v149ab >= 4f) {
                                                v149AbilityCapableUnarmed++;
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) { /* ignore */ }
                        }
                        if (v67arUnarmed == 0 && v67arArmed > 0) {
                            action.addReasoning(String.format(
                                "V67ar UNIVERSAL BLOCK: every Rando character (%d) already armed — pulled weapon would stack a 2nd weapon (forbidden)!",
                                v67arArmed), -9999.0f);
                            logger.warn("V67ar UNIVERSAL BLOCK (pull): '{}' — all {} chars armed, no 2nd weapon allowed",
                                actionText, v67arArmed);
                            hardBlocked = true;  // V192 (2026-07-06): joins the veto chain
                        } else if (v67arUnarmed == 0) {
                            action.addReasoning(
                                "V67ao ORDER GATE: weapon pull blocked — no Rando character on table to hold the weapon. Deploy a character first!",
                                -9999.0f);
                            logger.warn("V67ao ORDER GATE: weapon pull '{}' blocked (no chars on table)",
                                actionText);
                            hardBlocked = true;  // V192 (2026-07-06): joins the veto chain
                        } else if (v149IsLightsaberPull && v149AbilityCapableUnarmed == 0) {
                            action.addReasoning(
                                "V149 NO LIGHTSABER WIELDER: no unarmed [Warrior] ability-4+ character on table — don't pull a lightsaber nobody can wield",
                                -2000.0f);
                            logger.warn("V149 NO LIGHTSABER WIELDER (pull): '{}' — 0 unarmed [Warrior] ability-4+ chars → -2000",
                                actionText);
                            hardBlocked = true;  // V192 (2026-07-06): joins the veto chain
                        } else {
                            // V67am +600 grant ABSORBED into the V192 tier table 2026-07-06
                            // (single emit below; value unchanged, post-gates as before):
                            // action.addReasoning(String.format(
                            //     "V67am WEAPON PULL (universal, tier 1): %d unarmed character(s) on table — pull weapon from reserve!",
                            //     v67arUnarmed), 600.0f);
                            // logger.warn("V67am WEAPON PULL: '{}' adds weapon ({}) → +600 ({} unarmed targets)",
                            //     actionText, v67mReason, v67arUnarmed);
                            v192Tier = 600.0f;
                            v192TierDesc = String.format("WEAPON (V67am value, %d unarmed target(s) — %s)",
                                v67arUnarmed, v67mReason);
                        }
                    }

                    // === V67am (Steve, 2026-05-07): UNIVERSAL DEVICE-PULL PRIORITY (tier 3) ===
                    //
                    // Devices are similar to weapons but mostly defensive. Order:
                    //   tier 1: Weapon pull from reserve  +600 (V67am)
                    //   tier 2: Weapon from hand          (V29.11/V67ad: +400-500)
                    //   tier 3: Device pull from reserve  +400 (THIS BLOCK)
                    //   tier 4: Device from hand          (DeployEvaluator default scoring)
                    //
                    // Detection: action/source-card text mentions device-class keyword.
                    // Same dual-source pattern as V67m.
                    boolean v67amAddsDevice = false;
                    String v67amDeviceReason = null;
                    String[] v67amDeviceKeywords = new String[] {
                        "device", "comlink", "bionic", "sensor", "lockblade",
                        "restraints", "macrobinoculars", "scanner", "datapad",
                        "tool kit", "fusion cutter", "bowcaster"  // bowcaster is dual-classed
                    };
                    for (String kw : v67amDeviceKeywords) {
                        if (textLower.contains(kw)) {
                            v67amAddsDevice = true;
                            v67amDeviceReason = "actionText contains device keyword '" + kw + "'";
                            break;
                        }
                    }
                    if (!v67amAddsDevice && cardId != null && pullGs != null) {
                        try {
                            PhysicalCard sc = pullGs.findCardById(Integer.parseInt(cardId));
                            if (sc != null && sc.getBlueprint() != null) {
                                String gt = com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle
                                    .getSourceCardFullGameText(sc.getBlueprint(), context.getSide());
                                if (gt != null) {
                                    java.util.List<String> tgts =
                                        com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle
                                            .parseSourceCardPullTargets(gt);
                                    for (String t : tgts) {
                                        for (String kw : v67amDeviceKeywords) {
                                            if (t.contains(kw)) {
                                                v67amAddsDevice = true;
                                                v67amDeviceReason = "source card '" + sc.getTitle()
                                                    + "' targets device-like '" + t + "'";
                                                break;
                                            }
                                        }
                                        if (v67amAddsDevice) break;
                                    }
                                }
                            }
                        } catch (Exception e) { /* ignore */ }
                    }
                    if (v67amAddsDevice && !v67lAddsLocation && !v67mAddsWeapon) {
                        // V67ar (mirror): same UNIVERSAL ONE-DEVICE-PER-CHARACTER rule.
                        // Count UNARMED-by-device characters. (Most cards allow only one
                        // device per character; for safety we use the same all-armed gate
                        // as weapons since a single Rando char rarely needs both.)
                        int v67arDevUnarmed = 0;
                        int v67arDevArmed = 0;
                        if (pullGs != null && context.getPlayerId() != null) {
                            try {
                                for (PhysicalCard pc : pullGs.getAllPermanentCards()) {
                                    if (pc == null || pc.getBlueprint() == null) continue;
                                    if (!context.getPlayerId().equals(pc.getOwner())) continue;
                                    com.gempukku.swccgo.common.Zone z = pc.getZone();
                                    if (z == null || !z.isInPlay()) continue;
                                    if (pc.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                                    boolean hasDevice = false;
                                    java.util.List<PhysicalCard> atts = pullGs.getAttachedCards(pc);
                                    if (atts != null) {
                                        for (PhysicalCard a : atts) {
                                            if (a != null && a.getBlueprint() != null
                                                    && a.getBlueprint().getCardCategory() == CardCategory.DEVICE) {
                                                hasDevice = true;
                                                break;
                                            }
                                        }
                                    }
                                    if (hasDevice) v67arDevArmed++;
                                    else v67arDevUnarmed++;
                                }
                            } catch (Exception e) { /* ignore */ }
                        }
                        if (v67arDevUnarmed == 0 && v67arDevArmed > 0) {
                            action.addReasoning(String.format(
                                "V67ar UNIVERSAL BLOCK: every Rando character (%d) already has a device — second device on ANY character is wasteful!",
                                v67arDevArmed), -9999.0f);
                            logger.warn("V67ar UNIVERSAL BLOCK (device pull): '{}' — all {} chars have devices",
                                actionText, v67arDevArmed);
                            hardBlocked = true;  // V192 (2026-07-06): joins the veto chain
                        } else if (v67arDevUnarmed == 0) {
                            action.addReasoning(
                                "V67ao ORDER GATE: device pull blocked — no Rando character on table to host the device. Deploy a character first!",
                                -9999.0f);
                            logger.warn("V67ao ORDER GATE: device pull '{}' blocked (no chars on table)",
                                actionText);
                            hardBlocked = true;  // V192 (2026-07-06): joins the veto chain
                        } else {
                            // V67am DEVICE +400 grant ABSORBED into the V192 tier table
                            // 2026-07-06 (single emit below; value unchanged, post-gates):
                            // action.addReasoning(
                            //     "V67am DEVICE PULL (universal, tier 3): pull device from reserve via card text — defensive support, fires after weapons.",
                            //     400.0f);
                            // logger.warn("V67am DEVICE PULL: '{}' adds device ({}) → +400",
                            //     actionText, v67amDeviceReason);
                            v192Tier = 400.0f;
                            v192TierDesc = "DEVICE (V67am value — " + v67amDeviceReason + ")";
                        }
                    }

                    // === V67ak (Steve, 2026-05-07): KEY-CHARACTER PULL PRIORITY ===
                    //
                    // If the source-card pull's parsed targets include a strategy-key
                    // character name (matched against ObjectiveAnalyzer tokens from
                    // objective + epic events), give the pull action a strong priority bonus.
                    // Mirrors V67ak in DeployEvaluator for hand deploys.
                    //
                    // Skip if the matched character is already on table — repeated key-char
                    // pulls would be wasteful (e.g., uniqueness blocks second Vader).
                    // V192 (2026-07-06): now also gated on !hardBlocked (a weapon-gate or V131
                    // veto suppresses this positive too) and !v131DowngradeBonus (a downgraded
                    // pull must lose to Pass — no +800 resurrection).
                    if (!hardBlocked && !v131DowngradeBonus
                            && cardId != null && pullGs != null && context.getObjectiveAnalyzer() != null
                            && context.getObjectiveAnalyzer().isAnalyzed()) {
                        try {
                            PhysicalCard srcPc = pullGs.findCardById(Integer.parseInt(cardId));
                            if (srcPc != null && srcPc.getBlueprint() != null) {
                                String srcGT = com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle
                                    .getSourceCardFullGameText(srcPc.getBlueprint(), context.getSide());
                                if (srcGT != null) {
                                    java.util.List<String> srcTargets =
                                        com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle
                                            .parseSourceCardPullTargets(srcGT);
                                    com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer akObj =
                                        context.getObjectiveAnalyzer();
                                    java.util.Set<String> akTokens =
                                        akObj.getStrategyCharacterTokens(
                                            context.getGame(), context.getPlayerId());
                                    String matchedTok = null;
                                    for (String t : srcTargets) {
                                        String tl = t.toLowerCase(java.util.Locale.ROOT);
                                        for (String tok : akTokens) {
                                            if (tl.contains(tok)) { matchedTok = tok; break; }
                                        }
                                        if (matchedTok != null) break;
                                    }
                                    if (matchedTok != null) {
                                        // Check if persona role already filled on table
                                        boolean filled = false;
                                        for (PhysicalCard ex : pullGs.getAllPermanentCards()) {
                                            if (ex == null || ex.getBlueprint() == null) continue;
                                            if (!context.getPlayerId().equals(ex.getOwner())) continue;
                                            com.gempukku.swccgo.common.Zone ez = ex.getZone();
                                            if (ez == null || !ez.isInPlay()) continue;
                                            if (ex.getBlueprint().getCardCategory()
                                                    != com.gempukku.swccgo.common.CardCategory.CHARACTER) continue;
                                            String et = ex.getTitle();
                                            if (et != null && et.toLowerCase(java.util.Locale.ROOT).contains(matchedTok)) {
                                                filled = true; break;
                                            }
                                        }
                                        if (!filled) {
                                            action.addReasoning(String.format(
                                                "V67ak KEY-CHARACTER PULL: '%s' would pull '%s' (named in objective/epic-event) — flip-critical!",
                                                srcPc.getTitle(), matchedTok), 800.0f);
                                            logger.warn("V67ak KEY-CHARACTER PULL: source={} pulls token={} → +800",
                                                srcPc.getTitle(), matchedTok);
                                        } else {
                                            logger.info("V67ak KEY-CHARACTER PULL skip: token={} already filled on table",
                                                matchedTok);
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) { logger.debug("V67ak (pull) error: {}", e.getMessage()); }
                    }

                    // V67ao: Per Steve, no soft penalties for character pulls when locations
                    // are in hand. The V192 location tier (+1200 to +1500) already outscores
                    // character pulls (V67ak +800, others lower), so Combined Evaluator picks
                    // locations first naturally. The hard-block ordering gates only apply
                    // where the action would actually FAIL (weapon/device pull with no
                    // character host — see V67ao gates inside V67am blocks).

                    // ═══ V192 SINGLE EMIT (Steve + council, 2026-07-06) ═══
                    // Exactly ONE positive pull-scorer line per action (acceptance test:
                    // grep per actionId finds one). Absorbs V60-pull/V82/V95/V97/V100/V116/
                    // V67l/V67ai/V67am/V29.7-generic.
                    if (hardBlocked) {
                        // A veto fired inside the scorer (V131 hard block / weapon-holder
                        // gates) — its own negative line is already on the action; emit no
                        // positives (structural suppression, boundary row 4: zero
                        // resurrection surface).
                        logger.info("V192 PULL SCORER: '{}' veto'd inside scorer — no positive emitted", actionText);
                    } else if (v131DowngradeBonus) {
                        // V131 Tier 2 structural downgrade: positives suppressed + flat -200
                        // so the downgraded pull loses to Pass (~+6) by arithmetic
                        // (boundary row 3; also < BAD_ACTION_THRESHOLD -100 → bucket-skipped).
                        action.addReasoning(
                            "V192 PULL SCORER: V131 already-satisfied → positives suppressed ("
                                + v131Reason + ")", -200.0f);
                        logger.warn("V192 PULL SCORER: V131 already-satisfied → positives suppressed (-200) on '{}' ({})",
                            actionText, v131Reason);
                    } else {
                        // BASE: +150 deploy-grade, or +5500 PULL_BASE_ACTIVATE when the old
                        // V97 scope holds: Phase==ACTIVATE, static source (EFFECT/EPIC_EVENT/
                        // INTERRUPT/OBJECTIVE), title not Knowledge And Defense and not
                        // Anger, Fear, Aggression (both pull from stacked cards, not Reserve
                        // — V129). 5500 chains to V168 ALWAYS ACTIVATE +5000 (ACTIVATE region
                        // above) and MUST stay strictly above it so pulls fire BEFORE
                        // activation (feedback_pull_before_activate; boundary row 1a: the old
                        // pile totalled +2000 and LOST to V168 by 3000).
                        float v192Base = 150.0f;
                        boolean v192ActivateBase = false;
                        if (context.getPhase() == Phase.ACTIVATE && cardId != null && pullGs != null) {
                            try {
                                PhysicalCard v192Src = pullGs.findCardById(Integer.parseInt(cardId));
                                if (v192Src != null && v192Src.getBlueprint() != null
                                        && v192Src.getTitle() != null
                                        && !v192Src.getTitle().contains("Knowledge And Defense")
                                        && !v192Src.getTitle().contains("Anger, Fear, Aggression")) {
                                    com.gempukku.swccgo.common.CardCategory v192Cat =
                                        v192Src.getBlueprint().getCardCategory();
                                    boolean v192StaticSource =
                                        v192Cat == com.gempukku.swccgo.common.CardCategory.EFFECT
                                        || v192Cat == com.gempukku.swccgo.common.CardCategory.EPIC_EVENT
                                        || v192Cat == com.gempukku.swccgo.common.CardCategory.INTERRUPT
                                        || v192Cat == com.gempukku.swccgo.common.CardCategory.OBJECTIVE;
                                    if (v192StaticSource) {
                                        // P1 STAND-DOWN (orchestrator ruling 2026-07-06): when
                                        // the V61c destiny buffer is holding activation
                                        // (reserve <= 3 AND battle plausible — the SAME shared
                                        // predicate, DecisionContext.isBattlePlausibleThisTurn(),
                                        // never a second copy), the pull must not fire at
                                        // activate grade either: it erodes the same 3-card
                                        // destiny buffer activation is protecting. Stand down
                                        // to the deploy-grade base. Guard 1 above still kills
                                        // pulls outright at reserve <= 2.
                                        if (context.getReserveDeckSize() <= 3
                                                && context.isBattlePlausibleThisTurn()) {
                                            logger.warn("V192 PULL SCORER STAND-DOWN (V61c destiny buffer): reserve={} <= 3, battle plausible — deploy-grade base on '{}'",
                                                context.getReserveDeckSize(), actionText);
                                        } else {
                                            v192Base = 5500.0f;
                                            v192ActivateBase = true;
                                        }
                                    }
                                }
                            } catch (NumberFormatException nfe) { /* not numeric cardId */ }
                            catch (Exception e) { logger.debug("V192 base error: {}", e.getMessage()); }
                        }
                        // CONTEXT: [download]/free +50; chars-or-vehicles-in-hand during
                        // DEPLOY +25 on location pulls (the old V100 rationale: land the
                        // location first so the chars can deploy to it this phase).
                        float v192Ctx = 0.0f;
                        StringBuilder v192CtxDesc = new StringBuilder();
                        if (isFreeDownload) {
                            v192Ctx += 50.0f;
                            v192CtxDesc.append("+50 [download]");
                        }
                        if (v192IsLocationTier && context.getPhase() == Phase.DEPLOY) {
                            boolean v192CharsInHand = false;
                            java.util.List<PhysicalCard> v192Hand = context.getHand();
                            if (v192Hand != null) {
                                for (PhysicalCard hc : v192Hand) {
                                    if (hc == null || hc.getBlueprint() == null) continue;
                                    CardCategory v192HCat = hc.getBlueprint().getCardCategory();
                                    if (v192HCat == CardCategory.CHARACTER
                                            || v192HCat == CardCategory.VEHICLE) {
                                        v192CharsInHand = true;
                                        break;
                                    }
                                }
                            }
                            if (v192CharsInHand) {
                                v192Ctx += 25.0f;
                                if (v192CtxDesc.length() > 0) v192CtxDesc.append(", ");
                                v192CtxDesc.append("+25 chars-in-hand (V100)");
                            }
                        }
                        // CLAMP: 1750 deploy-grade / 7100 activate-grade. Safety rail only —
                        // the tier arithmetic tops out at 1725 / 7050. A CLAMP log line means
                        // someone fattened a constant without redoing the boundary math
                        // (deploy anchor: hand-location 1950 MUST keep winning).
                        float v192Total = v192Base + v192Tier + v192Ctx;
                        float v192Clamp = v192ActivateBase ? 7100.0f : 1750.0f;
                        if (v192Total > v192Clamp) {
                            logger.warn("V192 CLAMP: '{}' total {} > {} — clamped (constants need rebalancing)",
                                actionText, (int) v192Total, (int) v192Clamp);
                            v192Total = v192Clamp;
                        }
                        action.addReasoning(String.format(
                            "V192 PULL SCORER (%s): base %d + tier %d [%s] + ctx %d = %d [absorbs V60-pull/V82/V95/V97/V100/V116/V67l/V67ai/V67am/V29.7]",
                            v192ActivateBase ? "ACTIVATE" : "DEPLOY-GRADE",
                            (int) v192Base, (int) v192Tier, v192TierDesc, (int) v192Ctx, (int) v192Total),
                            v192Total);
                        logger.warn("V192 PULL SCORER ({}): base {} + tier {} [{}] + ctx {} ({}) = {} on '{}'",
                            v192ActivateBase ? "ACTIVATE" : "DEPLOY-GRADE",
                            (int) v192Base, (int) v192Tier, v192TierDesc, (int) v192Ctx,
                            v192CtxDesc.length() > 0 ? v192CtxDesc : "-", (int) v192Total, actionText);

                        // FORMATION SAFETY pull-route (2026-07-12b, replay ocffe8duo7yxh7fh):
                        // Krennic was pulled SOLO to Scarif: Command Center THREE times via the
                        // location's own "[download] Krennic here" game text and died each time —
                        // this pull route forces the destination and never passes the CS deploy-site
                        // guard, so L3/L4 never ran (winning scores 330/350 vs epilogue floor 50).
                        // When the SOURCE is a LOCATION pulling a CHARACTER "here", run the same
                        // deploy safety with destination = the source location.
                        // EXEMPTION (Steve's objective caveat): allow when the objective is NOT yet
                        // flipped AND the pulled character is named in the flip condition — the pull
                        // IS the flip plan (Krennic's FIRST pull flipped it; the re-pulls after his
                        // death bought nothing because another leader already held the site).
                        try {
                            if (cardId != null && gameState != null && context.getGame() != null
                                    && context.getPlayerId() != null) {
                                PhysicalCard fsSrc = gameState.findCardById(Integer.parseInt(cardId));
                                if (fsSrc != null && fsSrc.getBlueprint() != null
                                        && fsSrc.getBlueprint().getCardCategory() == com.gempukku.swccgo.common.CardCategory.LOCATION) {
                                    // BATCH1-CORR (2026-07-13, Codex m00229): the Krennic site's pull text
                                    // lives ONLY in the location's dark-side text — getGameText() is null,
                                    // which made this whole guard unreachable on the real card.
                                    String fsGt = com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle
                                        .getSourceCardFullGameText(fsSrc.getBlueprint(), context.getSide());
                                    String fsGtl = fsGt != null ? fsGt.toLowerCase(java.util.Locale.ROOT) : "";
                                    if (fsGtl.matches("(?s).*(?:\\[download\\]|deploy|take)[^.;]*\\bhere\\b.*")) {
                                        // Resolve the pulled CHARACTER from Reserve via the parsed targets.
                                        java.util.List<String> fsT = com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle
                                            .parseSourceCardPullTargets(fsGt);
                                        PhysicalCard fsPulled = null;
                                        for (PhysicalCard rc : gameState.getReserveDeck(context.getPlayerId())) {
                                            if (rc == null || rc.getBlueprint() == null || rc.getTitle() == null) continue;
                                            if (rc.getBlueprint().getCardCategory() != com.gempukku.swccgo.common.CardCategory.CHARACTER) continue;
                                            String rt = rc.getTitle().toLowerCase(java.util.Locale.ROOT);
                                            for (String t : fsT) {
                                                if (t == null || t.isEmpty()) continue;
                                                // BATCH1-CORR (2026-07-13, Codex m00225 #3): the here/there
                                                // suffix strip moved INTO DeckOracle.parseSourceCardPullTargets
                                                // (one normalization owner) — targets arrive clean here.
                                                if (rt.contains(t) || t.contains(rt)) { fsPulled = rc; break; }
                                            }
                                            if (fsPulled != null) break;
                                        }
                                        if (fsPulled != null) {
                                            // Flip-plan exemption check.
                                            boolean fsFlipPlan = false;
                                            com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer fsOa =
                                                context.getObjectiveAnalyzer();
                                            if (fsOa != null && fsOa.isAnalyzed() && !fsOa.isFlipped()
                                                    && fsOa.getFlipConditionText() != null) {
                                                // BATCH1-CORR (2026-07-13, Codex m00225 #1): first-name token
                                                // regressed 'Director Orson Krennic' (first token = 'director').
                                                // Match the blueprint's TYPED Personas against the flip text —
                                                // shared helper, pure-tested (m00262 fixture requirement).
                                                String fsFlip = fsOa.getFlipConditionText().toLowerCase(java.util.Locale.ROOT);
                                                try {
                                                    fsFlipPlan = com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle
                                                        .personaNamedInText(fsPulled.getBlueprint().getPersonas(), fsFlip);
                                                } catch (Exception fsPex) { /* no personas — no exemption */ }
                                            }
                                            if (!fsFlipPlan) {
                                                SwccgCardBlueprint fsBp = fsPulled.getBlueprint();
                                                float fsForce = gameState.getForcePileSize(context.getPlayerId());
                                                Float fsCost = fsBp.getDeployCost();
                                                Float fsBuddy = null;
                                                for (PhysicalCard fsH : gameState.getHand(context.getPlayerId())) {
                                                    if (fsH == null || fsH.getBlueprint() == null) continue;
                                                    if (fsH.getBlueprint().getCardCategory() != com.gempukku.swccgo.common.CardCategory.CHARACTER) continue;
                                                    Float c = fsH.getBlueprint().getDeployCost();
                                                    if (c == null) continue;
                                                    if (fsBuddy == null || c < fsBuddy) fsBuddy = c;
                                                }
                                                String fsV = com.gempukku.swccgo.ai.models.common.strategy.FormationSafety
                                                    .vetoCharacterDeploy(context.getGame(), gameState, context.getPlayerId(),
                                                        fsPulled,
                                                        fsBp.hasPowerAttribute() ? fsBp.getPower() : null,
                                                        fsBp.hasAbilityAttribute() ? fsBp.getAbility() : null,
                                                        false, fsSrc, fsForce, fsCost, fsBuddy, null);
                                                if (fsV != null) {
                                                    action.hardVeto(fsV);
                                                    logger.warn("FORMATION SAFETY (pull-route): {}", fsV);
                                                } else if (com.gempukku.swccgo.ai.models.common.strategy.FormationSafety
                                                        .weakSoloNoPlan(context.getGame(), gameState, context.getPlayerId(),
                                                            fsBp.hasAbilityAttribute() ? fsBp.getAbility() : null,
                                                            false, fsSrc, fsBuddy)) {
                                                    action.addReasoning(
                                                        "L3 NO-PLAN SOLO (pull-route): weak character would be pulled alone to "
                                                            + fsSrc.getTitle() + " with no buddy plan", -800.0f);
                                                    logger.warn("FORMATION SAFETY (pull-route): L3 NO-PLAN SOLO -800 at {}", fsSrc.getTitle());
                                                }
                                            } else {
                                                logger.warn("FORMATION SAFETY (pull-route): flip-plan exemption — '{}' named in unflipped objective flip condition", fsPulled.getTitle());
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (Exception fsPe) { /* fail-open */ }
                    }
                }
            }

            // ========== Default/Unknown ==========
            else {
                action.addReasoning("Unknown action type", 0.0f);
                logger.trace("Unrecognized action: {}", actionText);
            }

            actions.add(action);
        }

        return actions;
    }

    // ========== Helper Methods ==========

    private void evaluateActivateForce(EvaluatedAction action, DecisionContext context) {
        // V38.3: ALWAYS activate Force. ALWAYS. No exceptions.
        // Force is the currency for deploying characters. Without Force, Rando
        // can't deploy, can't fight, and slowly loses by attrition.
        // The old code had a Force pile cap of 20 and reserve-low checks that
        // caused Rando to skip activation entirely, leading to death spirals.
        // The ForceActivationEvaluator (INTEGER handler) now manages how MUCH
        // to activate. This function just needs to score the ACTION highly.
        action.addReasoning("V38.3 ALWAYS ACTIVATE: Force is currency — activate it!", 500.0f);
        logger.info("V38.3 ACTIVATE FORCE: Scored +500 — always activate");
    }

    // ═══════════════════════════════════════════════════════════
    // ═══ REGION: CONTROL — force drain scoring (reorg 2026-07-06) ═══
    // Owns: drain go/no-go and sizing: V52-drain +50 drain-anyway (+100..300 multi-site), V48 early-turn
    // deferral -50, V189 net -1 drain budget gate (turn spend forecast), V104 / V24.15 zero-drain guards,
    // V140 ordering, V29.9 Hunt Down drain priority. Drain-before-move interleave rule: each card drains
    // once/turn — moving a participant first forfeits the drain.
    // KIND mix (CONTROL overall): 5 VETO / 4 BANDED / 1 ORDERING.
    // Absorbs (dead, commented below/nearby — revert path, do not delete): none.
    // Cross-refs: MOVE (interleave), PLAYBOOKS (V24.x TDIGWATT drain rules), RESPONSE (the two
    // drain-response timings). See resources/RANDO_REORG_PLAN_2026-07-02.md §3 + Rando_Section_Manifest_2026-07-06.xlsx.
    // ═══════════════════════════════════════════════════════════
    private void evaluateForceDrain(EvaluatedAction action, DecisionContext context, String locationCardId) {
        // Force drains are generally good unless under Battle Order rules
        // Ported from Python action_text_evaluator.py lines 351-493

        GameState gameState = context.getGameState();
        String playerId = context.getPlayerId();

        // ========== V24.15: NEVER force drain at 0! ==========
        // Draining for 0 does nothing but opens us up to Surprise Assault and other traps.
        // Check the actual drain amount at the location before committing.
        if (gameState != null && locationCardId != null) {
            try {
                PhysicalCard drainLocation = gameState.findCardById(Integer.parseInt(locationCardId));
                if (drainLocation != null) {
                    SwccgGame drainGame = context.getGame();
                    if (drainGame != null) {
                        float drainAmount = drainGame.getModifiersQuerying().getForceDrainAmount(
                            gameState, drainLocation, playerId);
                        if (drainAmount <= 0) {
                            action.addReasoning("V24.15 DRAIN BLOCK: Force drain would be 0 — pointless and opens us to Surprise Assault!", -9999.0f);
                            logger.warn("V24.15 DRAIN BLOCK: Force drain at {} would be {} — HARD BLOCKING to avoid Surprise Assault trap!",
                                drainLocation.getTitle(), drainAmount);
                            return;
                        } else {
                            logger.info("V24.15 DRAIN CHECK: Force drain at {} will be {} — proceeding", drainLocation.getTitle(), drainAmount);
                        }
                        // === V189 (Steve, 2026-07-04): NET-VALUE DRAIN GATE ===
                        // "He should not have paid to drain for 1 with battle plan or
                        // battle order on the board." Game 20jqtseod148of4y: Rando paid
                        // 3 Force to drain 1 at Audience Chamber, twice. Ask the engine
                        // what initiating costs (getInitiateForceDrainCost sums every
                        // INITIATE_FORCE_DRAIN_COST modifier — Battle Order, Battle
                        // Plan, anything future — the same query the engine charges via
                        // PayInitiateForceDrainCostEffect). Boundary: cost 0 games
                        // completely unaffected (0 is never > drain, drainAmount > 0
                        // past V24.15); net 0 (pay 3 drain 3) ALLOWED — 3 permanent
                        // Life Force damage for 3 recycling Force. -2000 blocks land
                        // exactly -2000 (nothing scores a drain before this point),
                        // losing to Pass (+5) by ~2005, above the -9999 trap tier.
                        // UPDATED 2026-07-06 in place (Steve, 2026-07-04): "We should
                        // still allow drain 2 for 3 force if there is enough force to
                        // deploy and move everything that rando wants to do that turn."
                        // Two tiers now. Net <= -2 (pay 3 drain 1, the original
                        // offender) stays flat-blocked. Net -1 (pay 3 drain 2) is
                        // BUDGET-GATED: allowed only when forcePile - cost still covers
                        // the live deployables in hand + a 2-Force move allowance.
                        // Drains are CONTROL phase (before Deploy/Move), so the budget
                        // is a forecast, recomputed from live gameState at EVERY drain
                        // decision — automatically re-checked whenever Force is spent
                        // (the DeckOracle.refresh freshness pattern; no spend event
                        // exists to hook, and priming DeployPhasePlanner at Control
                        // would cache a stale over-budgeted plan into the V38.4
                        // hold-back machinery — see AI_CHANGELOG 2026-07-06). This
                        // restores V52's old "net -1 marginal but worth it" stance
                        // ONLY while the turn plan stays funded. Under-forecast gaps
                        // (Effects/weapons/devices/pull costs uncounted) err toward
                        // allowing a marginal drain — bounded, listed in changelog.
                        float v189Cost = drainGame.getModifiersQuerying().getInitiateForceDrainCost(
                            gameState, drainLocation, playerId);
                        if (v189Cost > drainAmount) {
                            if (v189Cost - drainAmount >= 2.0f) {
                                action.addReasoning(String.format(
                                    "V189 DRAIN NET-VALUE BLOCK: initiate cost %.0f > drain %.0f at %s — net <= -2, never worth it",
                                    v189Cost, drainAmount, drainLocation.getTitle()), -2000.0f);
                                logger.warn("V189 DRAIN NET-VALUE BLOCK: initiate cost {} > drain {} at {} — net <= -2 → -2000",
                                    (int)v189Cost, (int)drainAmount, drainLocation.getTitle());
                                return;
                            }
                            // Net -1 tier: TURN SPEND FORECAST — live deployable hand
                            // costs (persona-dead cards excluded, same isDeadCard test
                            // DeployPhasePlanner uses) + flat 2-Force move allowance.
                            int v189ForcePile = gameState.getForcePileSize(playerId);
                            int v189PlannedSpend = 0;
                            List<PhysicalCard> v189Hand = gameState.getHand(playerId);
                            if (v189Hand != null) {
                                for (PhysicalCard v189Hc : v189Hand) {
                                    if (v189Hc == null || v189Hc.getBlueprint() == null) continue;
                                    CardCategory v189Cat = v189Hc.getBlueprint().getCardCategory();
                                    if (v189Cat == CardCategory.CHARACTER || v189Cat == CardCategory.STARSHIP
                                            || v189Cat == CardCategory.VEHICLE) {
                                        if (com.gempukku.swccgo.ai.common.AiCardHelper.isDeadCard(v189Hc, drainGame, playerId)) continue;
                                        Float v189DepCost = v189Hc.getBlueprint().getDeployCost();
                                        if (v189DepCost != null && v189DepCost > 0) v189PlannedSpend += v189DepCost.intValue();
                                    }
                                }
                            }
                            final int v189MoveAllowance = 2;
                            if (v189ForcePile - v189Cost < v189PlannedSpend + v189MoveAllowance) {
                                action.addReasoning(String.format(
                                    "V189 DRAIN NET-VALUE BLOCK: net -1 but budget fails — %d Force - %.0f cost < %d planned deploys + %d move allowance at %s",
                                    v189ForcePile, v189Cost, v189PlannedSpend, v189MoveAllowance, drainLocation.getTitle()), -2000.0f);
                                logger.warn("V189 DRAIN NET-1 BUDGET BLOCK: force {} - cost {} < plan {} + moves {} at {} → -2000",
                                    v189ForcePile, (int)v189Cost, v189PlannedSpend, v189MoveAllowance, drainLocation.getTitle());
                                return;
                            }
                            logger.warn("V189 NET -1 DRAIN ALLOWED: cost {} drain {} at {} — force {} covers plan {} + moves {} — proceeding",
                                (int)v189Cost, (int)drainAmount, drainLocation.getTitle(), v189ForcePile, v189PlannedSpend, v189MoveAllowance);
                        } else if (v189Cost > 0) {
                            logger.info("V189 DRAIN NET-VALUE CHECK: cost {} <= drain {} at {} — worth paying, proceeding",
                                (int)v189Cost, (int)drainAmount, drainLocation.getTitle());
                        }
                    }
                }
            } catch (Exception e) {
                logger.debug("V24.15: Error checking drain amount: {}", e.getMessage());
            }
        }

        // === V25: SIMPLE TRICKS AND NONSENSE — avoid draining at non-battleground sites ===
        // Simple Tricks cancels Force drains at non-battleground sites. If opponent has it
        // on table, draining at non-battleground sites is pointless (gets cancelled).
        // Check for this BEFORE spending resources on the drain.
        if (gameState != null && locationCardId != null) {
            try {
                PhysicalCard drainLoc = gameState.findCardById(Integer.parseInt(locationCardId));
                if (drainLoc != null) {
                    SwccgCardBlueprint locBp = drainLoc.getBlueprint();
                    // Check if the drain location is a non-battleground site
                    // V25 UPDATED 2026-07-06: ask the ENGINE for DYNAMIC battleground status
                    // (modifiersQuerying.isBattleground — the same call pattern V140 used
                    // before its 2026-07-04 rework). The old static printed-icon check said
                    // "battleground" for dual-icon sites dynamically made non-battleground
                    // (cancelled force icons, NONBATTLEGROUND modifiers, Senate/Audience
                    // Chamber class cards), so the -9999 block below never fired and Rando
                    // drained into a guaranteed Simple Tricks cancel (audit row
                    // control-drain-5). Static icons kept ONLY as fallback when the game
                    // object is unavailable (pre-2026-07-06 behavior).
                    boolean isBattlegroundSite = false;
                    if (context.getGame() != null) {
                        isBattlegroundSite = context.getGame().getModifiersQuerying()
                            .isBattleground(gameState, drainLoc, null);
                    } else if (locBp != null) {
                        // OLD static detection (now fallback-only), pre-2026-07-06:
                        // A battleground site typically has force icons from both sides
                        isBattlegroundSite = locBp.hasIcon(com.gempukku.swccgo.common.Icon.DARK_FORCE)
                            && locBp.hasIcon(com.gempukku.swccgo.common.Icon.LIGHT_FORCE);
                    }

                    if (!isBattlegroundSite) {
                        // Check if opponent has Simple Tricks And Nonsense on table
                        String opponentId = gameState.getOpponent(playerId);
                        boolean simpleTricksOnTable = false;
                        if (opponentId != null) {
                            for (PhysicalCard card : gameState.getAllPermanentCards()) {
                                if (card == null || !opponentId.equals(card.getOwner())) continue;
                                com.gempukku.swccgo.common.Zone zone = card.getZone();
                                if (zone == null || !zone.isInPlay()) continue;
                                String cardTitle = card.getTitle();
                                if (cardTitle != null && cardTitle.contains("Simple Tricks")) {
                                    simpleTricksOnTable = true;
                                    break;
                                }
                            }
                        }

                        if (simpleTricksOnTable) {
                            action.addReasoning("V25 SIMPLE TRICKS: Non-battleground drain will be CANCELLED by Simple Tricks And Nonsense!", -9999.0f);
                            logger.warn("V25 SIMPLE TRICKS: BLOCKING drain at non-battleground {} — opponent has Simple Tricks!",
                                drainLoc.getTitle());
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                logger.debug("V25 Simple Tricks check error: {}", e.getMessage());
            }
        }

        // Check if we're under Battle Order rules (force drains cost +3 extra)
        // Battle Order is typically triggered when opponent has mains + specific cards
        boolean underBattleOrder = false;
        com.gempukku.swccgo.ai.models.chosenone.strategy.StrategyController strategyController = context.getStrategyController();
        if (strategyController != null) {
            underBattleOrder = strategyController.isUnderBattleOrderRules();
        }

        // Get available force
        int forceAvailable = 0;
        if (gameState != null) {
            forceAvailable = gameState.getForcePileSize(playerId);
        }

        // Check if we have any deployable cards in hand
        boolean hasDeployableCard = false;
        int cheapestDeployCost = Integer.MAX_VALUE;
        if (gameState != null) {
            List<PhysicalCard> hand = gameState.getHand(playerId);
            if (hand != null) {
                for (PhysicalCard card : hand) {
                    if (card.getBlueprint() != null) {
                        CardCategory category = card.getBlueprint().getCardCategory();
                        if (category == CardCategory.CHARACTER || category == CardCategory.STARSHIP ||
                            category == CardCategory.VEHICLE) {
                            hasDeployableCard = true;
                            Float deployCost = card.getBlueprint().getDeployCost();
                            if (deployCost != null && deployCost < cheapestDeployCost) {
                                cheapestDeployCost = deployCost.intValue();
                            }
                        }
                    }
                }
            }
        }

        if (underBattleOrder) {
            // Under Battle Order rules - force drains cost extra (+3)
            int battleOrderCost = 3;

            // If we can't afford the drain (need 3+ force), skip it
            if (forceAvailable < battleOrderCost) {
                action.addReasoning("Under Battle Order but can't afford drain (need " + battleOrderCost + ", have " + forceAvailable + ")", VERY_BAD_DELTA);
                return;
            }

            // Check if we have deployable cards - if yes, save force for them
            if (hasDeployableCard && cheapestDeployCost < Integer.MAX_VALUE) {
                int forceAfterDrain = forceAvailable - battleOrderCost;
                if (forceAfterDrain < cheapestDeployCost) {
                    action.addReasoning("Under Battle Order - saving force for deploy (cost " + cheapestDeployCost + ")", VERY_BAD_DELTA);
                    return;
                }
            }

            // If NO deployable cards - drains are our only pressure! Boost them!
            if (!hasDeployableCard) {
                action.addReasoning("Under Battle Order but NO deployable cards - drain is our only pressure!", VERY_GOOD_DELTA + 20.0f);
                logger.info("🔥 FORCE DRAIN BOOST: No deployable cards under Battle Order");
                return;
            }

            // V140 (Steve, 2026-05-26): BATTLE ORDER COST-WAIVER CHECK
            //
            // Battle Order's text: "must first use 3 Force UNLESS that player
            // occupies a battleground site (except a holosite) AND a battleground
            // system." Also waived if Battle Plan is on table.
            //
            // Previous V104/V48 logic treated Battle Order as ALWAYS imposing the
            // 3-force cost. Steve flagged 2026-05-26: "If he satisfies battle order
            // or battle plan he does not need to pay three force to drain." Adding
            // the waiver check so V104/V48 only fire when the cost is actually due.
            // V140 UPDATED 2026-07-04: engine aggregate initiate-cost is the sole
            // waiver authority. Card8_118 stands down while Battle Plan is on table,
            // but Card8_035 imposes its own 3-Force tax unless the player occupies
            // a battleground site and a battleground system.
            // A queried cost of 0 receives +60 and returns; positive cost falls
            // through to V104/V52/V48. Query failure also falls through.
            // The wrong hand-rolled predecessor was removed 2026-07-13 after source proof.
            boolean v140CostWaived = false;
            try {
                if (gameState != null && locationCardId != null && context.getGame() != null) {
                    PhysicalCard v140Loc = gameState.findCardById(Integer.parseInt(locationCardId));
                    if (v140Loc != null) {
                        v140CostWaived = context.getGame().getModifiersQuerying()
                            .getInitiateForceDrainCost(gameState, v140Loc, playerId) <= 0f;
                    }
                }
            } catch (Exception v140e) {
                logger.debug("V140 cost-waiver check error: {}", v140e.getMessage());
            }
            if (v140CostWaived) {
                action.addReasoning(
                    "V140 BATTLE ORDER COST WAIVED: engine initiate-cost is 0 — drain is FREE!",
                    VERY_GOOD_DELTA + 10.0f);
                logger.warn("V140 BATTLE ORDER COST WAIVED: engine initiate-cost 0 — drain is free, skipping V104/V48 penalties");
                // No further BO-specific scoring; drain is free, treat like normal drain
                return;
            }

            // V104 (Steve, 2026-05-20): UNDER BATTLE ORDER — HARD BLOCK DRAIN ≤ 1.
            // (Skipped when V140 cost-waiver fires above.)
            // NOTE (2026-07-04): V189 upstream (net-value gate at the V24.15 check)
            // now fronts every V104 case with the engine-true cost; V104 remains as
            // a backstop for the rare case where the engine cost query throws.
            boolean v104HardBlock = false;
            try {
                if (gameState != null && locationCardId != null && context.getGame() != null) {
                    PhysicalCard v104Loc = gameState.findCardById(Integer.parseInt(locationCardId));
                    if (v104Loc != null) {
                        float v104Amt = context.getGame().getModifiersQuerying()
                            .getForceDrainAmount(gameState, v104Loc, playerId);
                        if (v104Amt <= 1f) {
                            action.addReasoning(String.format(
                                "V104 BATTLE ORDER + DRAIN <= 1: drain %.0f at %s, pay 3 = net %.0f — hard block",
                                v104Amt, v104Loc.getTitle(), v104Amt - 3f), -2000.0f);
                            logger.warn("V104 BATTLE ORDER + DRAIN <= 1: drain {} at {} → -2000",
                                (int)v104Amt, v104Loc.getTitle());
                            v104HardBlock = true;
                        }
                    }
                } else {
                    logger.debug("V104: cannot determine drain value (gameState/location/game missing)");
                }
            } catch (Exception v104e) {
                logger.debug("V104: drain value check error: {}", v104e.getMessage());
            }

            // V52: After Turn 3, ALWAYS drain even under Battle Order.
            int drainTurn = context.getTurnNumber();
            if (v104HardBlock) {
                // Skip V52 boost — V104 already hard-blocked.
            } else if (drainTurn >= 3) {
                action.addReasoning("V52 DRAIN ANYWAY: Turn " + drainTurn + " — any drain is damage, pay the Battle Order cost!", VERY_GOOD_DELTA);
                logger.warn("V52 DRAIN ANYWAY: Turn {} under Battle Order — draining anyway! 1 damage > 0 damage!", drainTurn);
            } else {
                // Turns 1-2: save force for deploys, Battle Order drain is too expensive early
                action.addReasoning("V48 BATTLE ORDER EARLY: Turn " + drainTurn + " — save force for deploys", VERY_BAD_DELTA);
                logger.warn("V48 BATTLE ORDER EARLY: Turn {} — skipping drain to save for deploys", drainTurn);
            }

        } else {
            // Not under Battle Order - drain is generally good
            if (!hasDeployableCard) {
                // NO deployable cards - drains are our only pressure!
                action.addReasoning("Force drain (no deployable cards - our only pressure!)", VERY_GOOD_DELTA + 20.0f);
                logger.info("🔥 FORCE DRAIN BOOST: No deployable cards");
            } else {
                action.addReasoning("Force drain is good", VERY_GOOD_DELTA);
            }
        }

        // === V52 FIX 14: MULTI-SITE DRAIN — Prioritize draining at multiple sites ===
        // Count how many drain-capable sites we occupy and rank this drain by amount.
        // Draining at 3+ sites per turn is how Steve wins in 4 turns.
        if (gameState != null && locationCardId != null) {
            try {
                SwccgGame drainGame14 = context.getGame();
                if (drainGame14 != null) {
                    // Count sites where we can drain (we have presence + drain > 0)
                    int drainCapableSites = 0;
                    float thisDrainAmount = 0;
                    PhysicalCard thisDrainLoc = gameState.findCardById(Integer.parseInt(locationCardId));

                    for (PhysicalCard loc14 : gameState.getTopLocations()) {
                        if (loc14 == null) continue;
                        try {
                            float ourPower14 = drainGame14.getModifiersQuerying().getTotalPowerAtLocation(
                                gameState, loc14, playerId, false, false);
                            if (ourPower14 > 0) {
                                float drainAmt14 = drainGame14.getModifiersQuerying().getForceDrainAmount(
                                    gameState, loc14, playerId);
                                if (drainAmt14 > 0) {
                                    drainCapableSites++;
                                }
                            }
                        } catch (Exception e) { /* ignore */ }
                    }

                    if (thisDrainLoc != null) {
                        try {
                            thisDrainAmount = drainGame14.getModifiersQuerying().getForceDrainAmount(
                                gameState, thisDrainLoc, playerId);
                        } catch (Exception e) { /* ignore */ }
                    }

                    // Give bonus based on drain amount ranking (higher drain = higher bonus)
                    if (thisDrainAmount >= 3) {
                        action.addReasoning("V52 MULTI-DRAIN: Drain " + (int)thisDrainAmount + " — top priority drain site!", 300.0f);
                        logger.warn("V52 MULTI-DRAIN: {} drains {} — +300 (top tier)", thisDrainLoc != null ? thisDrainLoc.getTitle() : "?", (int)thisDrainAmount);
                    } else if (thisDrainAmount >= 2) {
                        action.addReasoning("V52 MULTI-DRAIN: Drain " + (int)thisDrainAmount + " — high value drain!", 200.0f);
                        logger.warn("V52 MULTI-DRAIN: {} drains {} — +200", thisDrainLoc != null ? thisDrainLoc.getTitle() : "?", (int)thisDrainAmount);
                    } else if (drainCapableSites >= 2) {
                        action.addReasoning("V52 MULTI-DRAIN: " + drainCapableSites + " drain sites — drain everywhere!", 100.0f);
                        logger.warn("V52 MULTI-DRAIN: {} — {} drain-capable sites +100", thisDrainLoc != null ? thisDrainLoc.getTitle() : "?", drainCapableSites);
                    }
                }
            } catch (Exception e) {
                logger.debug("V52 MULTI-DRAIN: Error: {}", e.getMessage());
            }
        }

        // === V29.9: HUNT DOWN FORCE DRAIN PRIORITY ===
        // For Hunt Down (V or regular), force drains are extra valuable because:
        // 1. Visage Of The Emperor adds +1 to each drain while we occupy a battleground
        // 2. Vader's presence at battleground locations enables draining
        // 3. Hunt Down V gives bonus force loss from lightsaber combat
        // Boost force drains significantly when running Hunt Down objective.
        com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer drainObjAnalyzer = context.getObjectiveAnalyzer();
        if (drainObjAnalyzer != null && drainObjAnalyzer.isAnalyzed() && drainObjAnalyzer.isHuntDownV()) {
            // Check if we're draining at a location with opponent icons (actual drain value)
            boolean highValueDrain = false;
            if (gameState != null && locationCardId != null) {
                try {
                    PhysicalCard drainLoc = gameState.findCardById(Integer.parseInt(locationCardId));
                    if (drainLoc != null && drainLoc.getBlueprint() != null) {
                        int oppIcons = drainLoc.getBlueprint().getIconCount(com.gempukku.swccgo.common.Icon.LIGHT_FORCE);
                        if (oppIcons >= 2) {
                            highValueDrain = true;
                            action.addReasoning("V29.9 HUNT DOWN DRAIN: High-value drain location (" + oppIcons + " opponent icons)!", 40.0f);
                        }
                    }
                } catch (Exception e) { /* ignore */ }
            }
            // General Hunt Down drain boost
            action.addReasoning("V29.9 HUNT DOWN: Force drains are critical — Visage adds +1, keep pressure on!", 30.0f);
        }
    }

    private void evaluatePlayCard(EvaluatedAction action, DecisionContext context) {
        int forcePile = context.getForcePileSize();
        if (forcePile == 0) {
            action.addReasoning("No Force available - can't play cards!", VERY_BAD_DELTA);
        } else if (forcePile <= 1) {
            action.addReasoning("Very low Force (" + forcePile + ") - unlikely to afford cards", BAD_DELTA);
        } else {
            // V24.5: No randomness — slight positive for playing cards when force available
            action.addReasoning("Generic play card — moderate priority", 5.0f);
        }
    }

    private void evaluateDestinyProtection(EvaluatedAction action, DecisionContext context) {
        Phase phase = context.getPhase();
        int turnNumber = context.getTurnNumber();

        // These cards only useful if battle is coming
        if (turnNumber <= 1) {
            action.addReasoning("SAVE for battle turn! Turn 1 rarely battles", VERY_BAD_DELTA);
        } else if (phase == Phase.BATTLE) {
            action.addReasoning("Protect destiny draws - IN BATTLE NOW!", VERY_GOOD_DELTA);
        } else if (phase == Phase.ACTIVATE || phase == Phase.CONTROL || phase == Phase.DEPLOY) {
            action.addReasoning("Protect destiny draws - battle opportunity exists", GOOD_DELTA);
        } else {
            action.addReasoning("Save destiny protection for clear battle turn", BAD_DELTA);
        }
    }

    private void evaluateSenseCancel(EvaluatedAction action, DecisionContext context, String actionText) {
        String textLower = actionText.toLowerCase();
        boolean isDestinyBased = textLower.contains("draw destiny") || textLower.contains("if destiny");

        // V37.3: NEVER cancel your OWN interrupts!
        // Rando played FMFTD then Sensed his own FMFTD — self-sabotage.
        // Check if the interrupt being canceled was played by US.
        // Clue: if the action text mentions a card that we just played this turn,
        // or if we're the active player and the interrupt belongs to us.
        GameState senseGs = context.getGameState();
        if (senseGs != null) {
            try {
                String sensePid = context.getPlayerId();
                // Check if the interrupt target name matches one of OUR cards
                // Hunt Down specific: FMFTD, Force Lightning, Force Push are ours
                String[] ourInterrupts = {"far more frightening", "force lightning", "force push",
                    "stunning leader", "i have you now", "sniper", "dark strike",
                    "we must accelerate", "ghhhk", "force field", "no escape"};
                for (String ourInt : ourInterrupts) {
                    if (textLower.contains(ourInt)) {
                        action.addReasoning("V37.3 SENSE SELF-CANCEL: NEVER cancel our OWN interrupt!", -9999.0f);
                        logger.warn("V37.3 SENSE SELF-CANCEL: Tried to cancel our own '{}' — HARD BLOCKED!", ourInt);
                        return;
                    }
                }
            } catch (Exception e) { /* ignore */ }
        }

        // Check priority cards system for target value
        AiPriorityCards.SenseTargetResult senseResult = AiPriorityCards.getSenseTargetValue(actionText);

        if (isDestinyBased) {
            if (senseResult.isHighValue && senseResult.score >= 80) {
                action.addReasoning("Destiny cancel critical target: " + senseResult.matchedPattern, 10.0f);
            } else {
                action.addReasoning("Destiny-based cancel (unreliable, skip)", -10.0f);
            }
        } else if (senseResult.isHighValue && senseResult.score >= 80) {
            action.addReasoning("Cancel CRITICAL target: " + senseResult.matchedPattern + "!", VERY_GOOD_DELTA + 20.0f);
        } else if (senseResult.isHighValue && senseResult.score >= 60) {
            action.addReasoning("Cancel high-value target: " + senseResult.matchedPattern, VERY_GOOD_DELTA);
        } else if (senseResult.isHighValue) {
            action.addReasoning("Cancel valuable target: " + senseResult.matchedPattern, GOOD_DELTA + 15.0f);
        } else if (textLower.contains("force drain")) {
            // V52: NEVER cancel your OWN force drain! Surprise Assault on own drain = self-sabotage.
            if (context.isMyTurn()) {
                action.addReasoning("V52 NEVER SELF-CANCEL DRAIN: Canceling own force drain is suicide!", -9999.0f);
                logger.warn("V52 SELF-CANCEL BLOCKED: Tried to cancel OWN force drain — HARD BLOCKED!");
            } else {
                action.addReasoning("Cancel opponent's force drain", GOOD_DELTA + 5.0f);
            }
        } else if (!context.isMyTurn()) {
            action.addReasoning("Cancel opponent interrupt (their turn)", GOOD_DELTA);
        } else {
            action.addReasoning("Cancel opponent interrupt (our turn)", 15.0f);
        }
    }

    private void evaluateHoujixGhhhk(EvaluatedAction action, DecisionContext context) {
        // These are CRITICAL survival cards
        // For now, give moderate positive score - ideally we'd check damage remaining
        action.addReasoning("Cancel battle damage - valuable survival card", GOOD_DELTA);

        // TODO: Add proper damage analysis when we have access to battle state
        // Check attrition/damage remaining and cards available to forfeit
    }

    private void evaluateTakeIntoHand(EvaluatedAction action, DecisionContext context, String actionText, String textLower) {
        if (textLower.contains("palpatine")) {
            action.addReasoning("Avoid taking Palpatine", BAD_DELTA);
            return;
        }

        // V29.7: Detect RETURN-TO-HAND (bouncing own card from table) vs RETRIEVE (from deck).
        // Retrieval actions always specify the source: "from Reserve Deck", "from Force Pile", etc.
        // If no source pile is mentioned, the card is being RETURNED from table — that's BAD!
        // Example: Corporal Vandolay's "Take an ISB agent into hand" = bounce deployed character.
        // EXCEPTION: "destiny" / "re-draw" actions are battle destiny management, NOT bounces.
        boolean isFromDeck = textLower.contains("from reserve") || textLower.contains("from force pile")
            || textLower.contains("from used pile") || textLower.contains("from lost pile");
        boolean isDestinyAction = textLower.contains("destiny") || textLower.contains("re-draw")
            || textLower.contains("redraw");

        if (!isFromDeck && !isDestinyAction) {
            // This is a bounce/return from table — VERY bad! We just paid to deploy that character.
            action.addReasoning("V29.7 BOUNCE: Return own card from table to hand — DON'T undo your deploy!", -300.0f);
            logger.warn("V29.7 BOUNCE BLOCKED: '{}' would return deployed card to hand (-300)", actionText);
        } else if (isFromDeck && textLower.contains("from reserve")) {
            // V29.7: PULL FIRST RULE — retrievals from Reserve Deck are FREE actions
            // from effects like Endor Shield, Mobilization Points, etc.
            // These should ALWAYS fire before locations (+200) and characters.
            // Getting cards into hand first = better deploy decisions.
            // V192 (2026-07-06): generic +250 ABSORBED into the V192 pull scorer base —
            // "Reserve Deck" takes now route to the PULL-ENGINE branch (dispatch gate),
            // so this arm only sees leftover "from Reserve" phrasings without "Deck".
            // Grant commented out (feedback_comment_out_old_rules); the TDIGWATT-specific
            // admiral/general +250/+300 branch earlier in the chain is untouched.
            // action.addReasoning("V29.7 PULL FIRST: Get cards into hand before deploying!", 250.0f);
            logger.info("V29.7 PULL FIRST (absorbed by V192): '{}' — no standalone +250", actionText);
        } else if (isFromDeck && textLower.contains("from lost pile")) {
            // V63 LOST PILE GUARD: "take a character into hand from Lost Pile"
            // (Jedi Levitation etc.) needs a matching card in Lost Pile. If there
            // isn't one, the search FAILS and opponent sees our entire Lost Pile.
            // FIXES djme704a2jn60z5c replay: Rando fired Jedi Levitation twice
            // with no character in Lost Pile — wasted 8 force + revealed deck.
            GameState lpGs = context.getGameState();
            String lpPid = context.getPlayerId();
            if (lpGs != null && lpPid != null) {
                int matchingInLostPile = 0;
                try {
                    java.util.List<PhysicalCard> lp = lpGs.getLostPile(lpPid);
                    if (lp != null) {
                        boolean wantsCharacter = textLower.contains("character");
                        boolean wantsJedi = textLower.contains("jedi");
                        boolean wantsAlien = textLower.contains("alien");
                        for (PhysicalCard c : lp) {
                            if (c == null || c.getBlueprint() == null) continue;
                            CardCategory cat = c.getBlueprint().getCardCategory();
                            if (wantsCharacter && cat != CardCategory.CHARACTER) continue;
                            matchingInLostPile++;
                        }
                    }
                } catch (Exception e) { /* ignore */ }
                if (matchingInLostPile == 0) {
                    action.addReasoning(
                        "V63 LOST PILE EMPTY: no matching target in Lost Pile — search will FAIL and reveal our pile!",
                        -9999.0f);
                    logger.warn("V63 LOST PILE EMPTY: '{}' has 0 matching targets — hard-blocked", actionText);
                    return;
                }
                logger.info("V63 LOST PILE OK: '{}' — {} matching targets in Lost Pile",
                    actionText, matchingInLostPile);
            }
            action.addReasoning("Take card into hand from Lost Pile", GOOD_DELTA);
        } else {
            // From force pile, used pile, or destiny management — normal priority
            action.addReasoning("Take card into hand", GOOD_DELTA);
        }
    }

    /**
     * Evaluate barrier card (Imperial/Rebel Barrier) usage.
     * Ported from Python action_text_evaluator.py lines 973-1055
     *
     * Use barriers when:
     *   - Location IS contested (both players present)
     *   - Target is a significant threat (high power)
     *   - We're not already winning overwhelmingly
     * Save barriers when:
     *   - Location not contested (no point)
     *   - We're already dominating the location
     *   - Target already has a barrier on it this turn!
     */
    private void evaluateBarrier(EvaluatedAction action, DecisionContext context, String actionText) {
        String targetCardName = extractCardNameFromPreventText(actionText);
        int currentTurn = context.getTurnNumber();

        // Reset barrier tracking on new turn
        if (currentTurn != barrierTurn) {
            barrieredTargets.clear();
            barrierTurn = currentTurn;
        }

        // Check if we already barriered this target
        if (targetCardName != null && barrieredTargets.contains(targetCardName.toLowerCase())) {
            action.addReasoning("Already barriered " + targetCardName + " this turn - wasteful!", VERY_BAD_DELTA);
            return;
        }

        // V35.1: NEVER barrier our OWN characters! "You Are Beaten" can target any character,
        // but preventing our OWN character from battling/moving is self-sabotage.
        // Check if the target belongs to us — if so, HARD BLOCK.
        GameState gameState = context.getGameState();
        String playerId = context.getPlayerId();
        if (gameState != null && playerId != null && targetCardName != null) {
            String targetLower = targetCardName.toLowerCase();
            for (PhysicalCard card : gameState.getAllPermanentCards()) {
                if (card == null || card.getTitle() == null) continue;
                if (card.getTitle().toLowerCase().contains(targetLower) || targetLower.contains(card.getTitle().toLowerCase())) {
                    if (playerId.equals(card.getOwner())) {
                        action.addReasoning(String.format(
                            "V35.1 SELF-BARRIER BLOCK: %s is OUR character — NEVER prevent our own from battling!",
                            targetCardName), -9999.0f);
                        logger.warn("V35.1 SELF-BARRIER: Blocking barrier on OWN character {} (-9999)", targetCardName);
                        return;
                    }
                    break;
                }
            }
        }
        float targetPower = 0;
        float ourPower = 0;
        float theirPower = 0;
        boolean locationContested = false;

        if (gameState != null && playerId != null && targetCardName != null) {
            String opponentId = gameState.getOpponent(playerId);

            // Find the target card and analyze location
            for (PhysicalCard card : gameState.getAllPermanentCards()) {
                if (card == null) continue;
                String title = card.getTitle();
                if (title == null) continue;

                // Match by name
                if (title.toLowerCase().contains(targetCardName.toLowerCase()) ||
                    targetCardName.toLowerCase().contains(title.toLowerCase())) {

                    // Found the target - check its power
                    SwccgCardBlueprint blueprint = card.getBlueprint();
                    if (blueprint != null && blueprint.hasPowerAttribute()) {
                        Float power = blueprint.getPower();
                        if (power != null) {
                            targetPower = power;
                        }
                    }

                    // Find location and calculate power
                    PhysicalCard location = card.getAtLocation();
                    if (location != null) {
                        boolean hasOurPresence = false;
                        boolean hasTheirPresence = false;

                        for (PhysicalCard locCard : gameState.getCardsAtLocation(location)) {
                            if (locCard == null) continue;
                            String owner = locCard.getOwner();
                            SwccgCardBlueprint bp = locCard.getBlueprint();
                            if (bp == null) continue;

                            // Check presence
                            if (playerId.equals(owner)) {
                                hasOurPresence = true;
                                if (bp.hasPowerAttribute()) {
                                    Float power = bp.getPower();
                                    if (power != null) ourPower += power;
                                }
                            } else if (opponentId != null && opponentId.equals(owner)) {
                                hasTheirPresence = true;
                                if (bp.hasPowerAttribute()) {
                                    Float power = bp.getPower();
                                    if (power != null) theirPower += power;
                                }
                            }
                        }
                        locationContested = hasOurPresence && hasTheirPresence;
                    }
                    break;
                }
            }
        }

        logger.debug("🚧 Barrier analysis: {} (power {}) contested={}, our={}, their={}",
            targetCardName, targetPower, locationContested, ourPower, theirPower);

        // V48: Check if WE have any presence at the target's location
        // Barrier prevents battling/moving. If we have nobody there, it serves no purpose.
        boolean weHavePresence = ourPower > 0;

        // Apply scoring based on situation
        if (!weHavePresence) {
            // V48: We have NOBODY at this location — barrier is completely useless!
            action.addReasoning("V48 BARRIER USELESS: No friendly presence at location — serves no purpose!", -9999.0f);
            logger.warn("V48 BARRIER BLOCK: No friendly presence at target location — HARD BLOCK!");
        } else if (!locationContested) {
            // Location NOT contested - save barrier for when we need it
            action.addReasoning("Save barrier - location not contested", BAD_DELTA);
        } else if (ourPower >= theirPower + 8) {
            // We're already dominating - don't waste the barrier
            action.addReasoning("Save barrier - already dominating (" + (int)ourPower + " vs " + (int)theirPower + ")", BAD_DELTA);
        } else if (targetPower >= 5) {
            // High-power target at contested location - VERY valuable!
            action.addReasoning("Barrier on HIGH POWER target (" + (int)targetPower + ")!", VERY_GOOD_DELTA);
            if (targetCardName != null) {
                barrieredTargets.add(targetCardName.toLowerCase());
            }
        } else if (theirPower >= ourPower) {
            // They're winning or tied - barrier is valuable
            action.addReasoning("Barrier to protect (losing " + (int)ourPower + " vs " + (int)theirPower + ")", GOOD_DELTA + 10.0f);
            if (targetCardName != null) {
                barrieredTargets.add(targetCardName.toLowerCase());
            }
        } else {
            // We're ahead but not dominating - still useful
            action.addReasoning("Barrier at contested location", GOOD_DELTA);
            if (targetCardName != null) {
                barrieredTargets.add(targetCardName.toLowerCase());
            }
        }
    }

    private void evaluateEmbark(EvaluatedAction action, DecisionContext context, String actionText, String cardId) {
        // 2026-06-01 EMBARK BOOST (Steve, Walker games):
        // "Rando already had pilots on the same site. He's not embarking them
        // onto the walkers or vehicles." The engine offers an 'Embark' action
        // per pilot at a site where one of Rando's vehicles/ships is parked;
        // the prior placeholder scored it 0 and other moves (or pass) won.
        // Fix: when the embarker is a pilot (Icon.PILOT or Keyword.TROOPER)
        // AND at least one unmanned VEHICLE/STARSHIP shares the site with
        // them, +500. Puts the embark above generic moves and pass so Rando
        // gets the walker manned.
        try {
            com.gempukku.swccgo.game.state.GameState embarkGs = context.getGameState();
            com.gempukku.swccgo.game.SwccgGame embarkGame = context.getGame();
            if (embarkGs == null || embarkGame == null || cardId == null) {
                action.addReasoning("Embark action (no context)", 0.0f);
                return;
            }
            com.gempukku.swccgo.game.PhysicalCard embarker = null;
            try {
                embarker = embarkGs.findCardById(Integer.parseInt(cardId));
            } catch (NumberFormatException nfe) { /* temp ids — skip */ }
            if (embarker == null || embarker.getBlueprint() == null) {
                action.addReasoning("Embark action (no card)", 0.0f);
                return;
            }
            SwccgCardBlueprint embarkerBp = embarker.getBlueprint();
            boolean embarkerIsPilot =
                embarkerBp.hasIcon(com.gempukku.swccgo.common.Icon.PILOT)
                || embarkerBp.hasKeyword(com.gempukku.swccgo.common.Keyword.TROOPER);
            if (!embarkerIsPilot) {
                // Non-pilot character embarking is usually a passenger move — neutral.
                action.addReasoning("Embark action (non-pilot)", 0.0f);
                return;
            }
            // 2026-06-01 POWER-3 GATE (Steve): "If pilot is power 4 or more
            // let's leave them disembarked from vehicles. Likely better as
            // ground troops. Regular pilots are usually power 3 or less."
            // Skip the embark boost for power-4+ characters — they're more
            // valuable hitting people on the ground than crewing a vehicle.
            Float embarkerPower = embarkerBp.hasPowerAttribute() ? embarkerBp.getPower() : null;
            if (embarkerPower != null && embarkerPower >= 4f) {
                action.addReasoning(
                    "Embark action (skipped: power " + embarkerPower.intValue()
                    + " — better as ground troop)", 0.0f);
                return;
            }
            // Find the embarker's current location.
            com.gempukku.swccgo.game.PhysicalCard embarkLoc = null;
            try {
                embarkLoc = embarkGame.getModifiersQuerying()
                    .getLocationThatCardIsAt(embarkGs, embarker);
            } catch (Exception ignore) { /* */ }
            if (embarkLoc == null) {
                action.addReasoning("Embark action (no location)", 0.0f);
                return;
            }
            // Walk permanents at the same site for an unmanned vehicle/ship owned by us.
            String embarkPid = context.getPlayerId();
            String unmannedTitle = null;
            for (com.gempukku.swccgo.game.PhysicalCard pc : embarkGs.getAllPermanentCards()) {
                if (pc == null || !embarkPid.equals(pc.getOwner())) continue;
                if (pc.getBlueprint() == null) continue;
                com.gempukku.swccgo.common.CardCategory cat = pc.getBlueprint().getCardCategory();
                if (cat != com.gempukku.swccgo.common.CardCategory.VEHICLE
                        && cat != com.gempukku.swccgo.common.CardCategory.STARSHIP) continue;
                com.gempukku.swccgo.game.PhysicalCard pcLoc = null;
                try {
                    pcLoc = embarkGame.getModifiersQuerying()
                        .getLocationThatCardIsAt(embarkGs, pc);
                } catch (Exception ignore) { /* */ }
                if (pcLoc != embarkLoc) continue;
                // Unmanned check via Filters.piloted.
                boolean piloted = com.gempukku.swccgo.filters.Filters.piloted.accepts(
                    embarkGs, embarkGame.getModifiersQuerying(), pc);
                if (!piloted) {
                    unmannedTitle = pc.getTitle();
                    break;
                }
            }
            if (unmannedTitle != null) {
                action.addReasoning(
                    "EMBARK PILOT: '" + embarker.getTitle() + "' boarding unmanned '"
                    + unmannedTitle + "' — vehicle gets power & protection", 500.0f);
                logger.warn("EMBARK PILOT: {} boarding unmanned {} → +500",
                    embarker.getTitle(), unmannedTitle);
            } else {
                action.addReasoning("Embark action (no unmanned target at site)", 0.0f);
            }
        } catch (Exception e) {
            logger.debug("evaluateEmbark error: {}", e.getMessage());
            action.addReasoning("Embark action (error)", 0.0f);
        }
    }

    private void evaluateGrab(EvaluatedAction action, DecisionContext context, String actionText) {
        // V53: Grabber shields (Allegations / A Tragedy) must ONLY grab OPPONENT's interrupts.
        // NEVER grab your own interrupts — that's self-sabotage.
        // Use game state to check card ownership when possible, fall back to name matching.

        Side mySide = context.getSide();
        GameState grabGs = context.getGameState();
        String textLower = actionText.toLowerCase();

        // V53: Try to determine ownership from game state (most reliable)
        boolean confirmedOwnCard = false;
        boolean confirmedOpponentCard = false;
        if (grabGs != null && context.getPlayerId() != null) {
            try {
                // Check if any card IDs in context belong to us
                String pid = context.getPlayerId();
                String oid = grabGs.getOpponent(pid);
                for (String cardId : context.getCardIds()) {
                    PhysicalCard grabCard = grabGs.findCardById(Integer.parseInt(cardId));
                    if (grabCard != null) {
                        if (pid.equals(grabCard.getOwner())) confirmedOwnCard = true;
                        if (oid != null && oid.equals(grabCard.getOwner())) confirmedOpponentCard = true;
                    }
                }
            } catch (Exception e) { /* fall through to name matching */ }
        }

        if (confirmedOwnCard && !confirmedOpponentCard) {
            action.setScore(-9999.0f);
            action.addReasoning("V53 NEVER GRAB OWN: Grabbing own interrupt is suicide!", -9999.0f);
            logger.warn("V53 GRAB BLOCKED: Confirmed own card — HARD BLOCKED! {}", actionText);
            return;
        } else if (confirmedOpponentCard) {
            action.addReasoning("V53 GRAB OPPONENT: Confirmed opponent's interrupt — grab it!", GOOD_DELTA);
            logger.warn("V53 GRAB: Confirmed opponent card — grabbing! {}", actionText);
            return;
        }

        // Fallback: name-based side detection
        boolean looksLightSide = textLower.contains("rebel") || textLower.contains("jedi") ||
                                  textLower.contains("alliance") || textLower.contains("luke") ||
                                  textLower.contains("leia") || textLower.contains("han solo") ||
                                  textLower.contains("chewie") || textLower.contains("yoda") ||
                                  textLower.contains("obi-wan") || textLower.contains("padme");
        boolean looksDarkSide = textLower.contains("imperial") || textLower.contains("sith") ||
                                 textLower.contains("vader") || textLower.contains("emperor") ||
                                 textLower.contains("stormtrooper") || textLower.contains("death star") ||
                                 textLower.contains("maul") || textLower.contains("dooku") ||
                                 textLower.contains("boba fett") || textLower.contains("jango");

        if (mySide == Side.DARK && looksLightSide) {
            action.addReasoning("Grab Light side card (we are Dark)", GOOD_DELTA);
        } else if (mySide == Side.LIGHT && looksDarkSide) {
            action.addReasoning("Grab Dark side card (we are Light)", GOOD_DELTA);
        } else if (mySide == Side.DARK && looksDarkSide) {
            action.setScore(-9999.0f);
            action.addReasoning("V53 NEVER GRAB OWN: Grabbing own Dark card!", -9999.0f);
            logger.warn("V53 GRAB BLOCKED: Likely own Dark card — {}", actionText);
        } else if (mySide == Side.LIGHT && looksLightSide) {
            action.setScore(-9999.0f);
            action.addReasoning("V53 NEVER GRAB OWN: Grabbing own Light card!", -9999.0f);
            logger.warn("V53 GRAB BLOCKED: Likely own Light card — {}", actionText);
        } else {
            // Unknown owner — only grab if it's opponent's turn (their interrupt just played)
            if (!context.isMyTurn()) {
                action.addReasoning("Grab unknown card (opponent's turn — likely theirs)", GOOD_DELTA);
            } else {
                action.addReasoning("V53 GRAB CAUTION: Unknown owner on our turn — avoid!", -200.0f);
                logger.info("V53 GRAB CAUTION: Unknown owner on our turn, avoiding: {}", actionText);
            }
        }
    }

    private void evaluateBreakCover(EvaluatedAction action, DecisionContext context, String actionText) {
        // V53: Breaking spy cover depends on context:
        // - Break OPPONENT's spy: always good (expose their spy)
        // - Break OWN spy when we have a friendly character at that location: +500
        //   (flip the spy to protect our deployed character — instant buddy system)
        // - Break OWN spy when we have NO friendly character there: -500
        //   (don't blow cover for nothing)

        Side mySide = context.getSide();
        GameState gameState = context.getGameState();

        // V59 OWNER RESOLUTION: Look up the spy's actual owner via cardId first.
        // FIXES Issue #6 from peaceful-pike replay: actionText was just "Break cover"
        // with no card name, so regex matching failed and we fell through to
        // the "unknown owner" -30 branch. Now we resolve via PhysicalCard.
        Boolean ownerIsUs = null;  // null = unknown, true = our spy, false = opponent's
        try {
            List<String> ctxCardIds = context.getCardIds();
            if (ctxCardIds != null && !ctxCardIds.isEmpty() && gameState != null) {
                String cardIdStr = ctxCardIds.get(0);
                PhysicalCard spyCard = gameState.findCardById(Integer.parseInt(cardIdStr));
                if (spyCard != null && spyCard.getOwner() != null) {
                    ownerIsUs = spyCard.getOwner().equals(context.getPlayerId());
                    logger.info("V59 BREAK COVER OWNER: spy {} owner={} (we are {})",
                        spyCard.getTitle(), spyCard.getOwner(), context.getPlayerId());
                }
            }
        } catch (Exception e) {
            logger.debug("V59 BREAK COVER: Error resolving owner: {}", e.getMessage());
        }

        // Fallback: Determine side from card name patterns in action text
        String textLower = actionText.toLowerCase();
        boolean looksLightSide = textLower.contains("rebel") || textLower.contains("bothan") ||
                                  textLower.contains("alliance") || textLower.contains("leia") ||
                                  textLower.contains("mon mothma") || textLower.contains("orrimaarko");
        boolean looksDarkSide = textLower.contains("imperial") || textLower.contains("ism-agent") ||
                                 textLower.contains("empire") || textLower.contains("probe droid") ||
                                 textLower.contains("mara jade");

        boolean isOwnSpy = (ownerIsUs != null && ownerIsUs)
            || (ownerIsUs == null && ((mySide == Side.DARK && looksDarkSide) || (mySide == Side.LIGHT && looksLightSide)));
        boolean isOpponentSpy = (ownerIsUs != null && !ownerIsUs)
            || (ownerIsUs == null && ((mySide == Side.DARK && looksLightSide) || (mySide == Side.LIGHT && looksDarkSide)));

        if (isOpponentSpy) {
            action.addReasoning("Break opponent's spy cover — expose them!", GOOD_DELTA);
        } else if (isOwnSpy) {
            // V53: Check if we have a non-spy friendly character at the spy's location.
            // If yes, flip the spy to fight alongside them (+500).
            // If no, don't blow cover for nothing (-500).
            boolean friendlyCharAtSpyLocation = false;
            if (gameState != null) {
                try {
                    String pid = context.getPlayerId();
                    // Find our undercover spies and check their locations for friendly characters
                    for (PhysicalCard loc : gameState.getTopLocations()) {
                        if (loc == null) continue;
                        boolean hasOurSpy = false;
                        boolean hasOurCharacter = false;
                        for (PhysicalCard c : gameState.getCardsAtLocation(loc)) {
                            if (c == null || !pid.equals(c.getOwner())) continue;
                            if (c.isUndercover()) {
                                hasOurSpy = true;
                            } else if (c.getBlueprint() != null
                                && c.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                                hasOurCharacter = true;
                            }
                        }
                        if (hasOurSpy && hasOurCharacter) {
                            friendlyCharAtSpyLocation = true;
                            break;
                        }
                    }
                } catch (Exception e) { /* ignore */ }
            }

            if (friendlyCharAtSpyLocation) {
                action.addReasoning("V53 FLIP SPY: We have a character at spy's location — flip spy to protect them!", 500.0f);
                logger.warn("V53 FLIP SPY: Breaking own spy cover — friendly character present, +500!");
            } else {
                action.addReasoning("V53 KEEP COVER: No friendly character at spy location — don't blow cover!", -500.0f);
                logger.warn("V53 KEEP COVER: No friendly at spy location — blocking break cover, -500");
            }
        } else {
            // Unknown spy - check for friendly presence as tiebreaker
            action.addReasoning("Break cover (spy owner unknown - cautious)", BAD_DELTA);
            logger.info("Break cover owner unknown, avoiding: {}", actionText);
        }
    }

    // ========== Utility Methods ==========

    private String extractBlueprintFromText(String actionText) {
        if (actionText == null) return null;
        Matcher matcher = BLUEPRINT_PATTERN.matcher(actionText);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String extractCardNameFromPreventText(String actionText) {
        // Pattern: "Prevent <CARD NAME> from battling or moving"
        if (actionText != null && actionText.contains("Prevent") &&
            actionText.contains("from battling or moving")) {
            int startIdx = actionText.indexOf("Prevent") + "Prevent ".length();
            int endIdx = actionText.indexOf(" from battling or moving");
            if (startIdx > 0 && endIdx > startIdx) {
                return actionText.substring(startIdx, endIdx).trim();
            }
        }
        return null;
    }

    /**
     * Check if there are valid (non-HIT) weapon targets at the battle location.
     *
     * In SWCCG, firing at already-hit targets is wasteful since they're
     * already damaged. This method returns true only if there are unhit
     * enemy cards at the battle location.
     *
     * Ported from Python action_text_evaluator.py valid target check.
     */
    private boolean checkForValidWeaponTargets(DecisionContext context) {
        GameState gameState = context.getGameState();
        if (gameState == null) {
            return true;  // Default to allowing fire if we can't check
        }

        try {
            // Get the battle location
            PhysicalCard battleLocation = gameState.getBattleLocation();
            if (battleLocation == null) {
                return true;  // Not in battle, allow fire
            }

            // Find enemy cards at battle location
            String playerId = context.getPlayerId();
            String opponentId = gameState.getOpponent(playerId);
            if (opponentId == null) {
                return true;  // Can't determine opponent
            }

            // Check all enemy cards at battle location
            boolean foundUnhitEnemy = false;
            for (PhysicalCard card : gameState.getAllPermanentCards()) {
                if (card == null) continue;

                // Must be enemy card
                if (!opponentId.equals(card.getOwner())) continue;

                // Must be at battle location
                PhysicalCard cardLocation = card.getAtLocation();
                if (cardLocation == null || !cardLocation.equals(battleLocation)) continue;

                // Must be a valid weapon target (character, starship, vehicle)
                SwccgCardBlueprint bp = card.getBlueprint();
                if (bp == null) continue;
                CardCategory cat = bp.getCardCategory();
                if (cat != CardCategory.CHARACTER && cat != CardCategory.STARSHIP && cat != CardCategory.VEHICLE) {
                    continue;
                }

                // Check if this card is NOT hit
                if (!card.isHit()) {
                    foundUnhitEnemy = true;
                    logger.debug("Found unhit enemy target: {}", card.getTitle());
                    break;  // Found at least one valid target
                }
            }

            if (!foundUnhitEnemy) {
                logger.info("🎯 All enemy targets at battle location are HIT - no valid weapon targets");
            }

            return foundUnhitEnemy;

        } catch (Exception e) {
            logger.debug("Error checking weapon targets: {}", e.getMessage());
            return true;  // Default to allowing fire on error
        }
    }
}
