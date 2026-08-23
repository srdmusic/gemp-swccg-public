package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Locale;

// ═══════════════════════════════════════════════════════════
// ═══ T2 MOVE #3 (2026-07-06): SHARED SHIELD/OCCUPATION FACTS ═══
// The ONE home for the engine-based occupation predicates the SHIELDS family
// consults (audit shields-response-2). Before this class, ShieldStrategy.
// The fourth-slot trigger A ran its OWN getTotalPowerAtLocation power>0 scan
// while CardSelectionEvaluator's V51/V112 gates used the engine
// occupies-predicate (occupiesBothTheaters, added in commit ee0a1b435) — the
// two disagreed on boards where occupation carries zero power (an unpiloted
// ship occupies a system but has power 0 per Power.java:49-51), so the card's
// own condition (Card13_054 Battle Order OccupiesCondition) could be LIVE
// while the 4th-slot gate said no. Homed in common/strategy per the T2 helper
// spec placement decision (CharacterDeploySiteEvaluator / MovePredicates /
// MaintenanceFacts precedent) so BOTH bots (rando + chosenone) share one copy.
//
// occupiesBothTheaters is the verbatim body of the former evaluator-private
// predicate. V205 routes both evaluators through this shared owner and also
// homes the V117/V124 on-table count and fourth-slot board reads here.
// ═══════════════════════════════════════════════════════════
public final class ShieldFacts {

    private static final Logger LOG = LogManager.getLogger(ShieldFacts.class);

    private ShieldFacts() { /* static-only */ }

    public record FourthSlotFacts(boolean occupiesBothTheaters,
                                  boolean occupiesAnyBattleground,
                                  int opponentBattlegroundCount,
                                  boolean opponentHasDrainBonus,
                                  int ownBattlegroundCount,
                                  boolean opponentCanDrainThreePlus,
                                  boolean opponentDrainsNonBattleground,
                                  boolean battleOrderPlanLive,
                                  boolean battleOrderPlanEquivalentOnTable,
                                  boolean nonBattlegroundShieldWindowLive) {
        public FourthSlotFacts(
                boolean occupiesBothTheaters,
                boolean occupiesAnyBattleground,
                int opponentBattlegroundCount,
                boolean opponentHasDrainBonus,
                int ownBattlegroundCount,
                boolean opponentCanDrainThreePlus,
                boolean opponentDrainsNonBattleground) {
            this(occupiesBothTheaters, occupiesAnyBattleground,
                    opponentBattlegroundCount, opponentHasDrainBonus,
                    ownBattlegroundCount, opponentCanDrainThreePlus,
                    opponentDrainsNonBattleground, occupiesBothTheaters, false,
                    occupiesAnyBattleground && opponentBattlegroundCount < 2);
        }

        public FourthSlotFacts(
                boolean occupiesBothTheaters,
                boolean occupiesAnyBattleground,
                int opponentBattlegroundCount,
                boolean opponentHasDrainBonus,
                int ownBattlegroundCount,
                boolean opponentCanDrainThreePlus,
                boolean opponentDrainsNonBattleground,
                boolean battleOrderPlanEquivalentOnTable) {
            this(occupiesBothTheaters, occupiesAnyBattleground,
                    opponentBattlegroundCount, opponentHasDrainBonus,
                    ownBattlegroundCount, opponentCanDrainThreePlus,
                    opponentDrainsNonBattleground, occupiesBothTheaters,
                    battleOrderPlanEquivalentOnTable,
                    occupiesAnyBattleground && opponentBattlegroundCount < 2);
        }

        public FourthSlotFacts(
                boolean occupiesBothTheaters,
                boolean occupiesAnyBattleground,
                int opponentBattlegroundCount,
                boolean opponentHasDrainBonus,
                int ownBattlegroundCount,
                boolean opponentCanDrainThreePlus,
                boolean opponentDrainsNonBattleground,
                boolean battleOrderPlanEquivalentOnTable,
                boolean nonBattlegroundShieldWindowLive) {
            this(occupiesBothTheaters, occupiesAnyBattleground,
                    opponentBattlegroundCount, opponentHasDrainBonus,
                    ownBattlegroundCount, opponentCanDrainThreePlus,
                    opponentDrainsNonBattleground, occupiesBothTheaters,
                    battleOrderPlanEquivalentOnTable,
                    nonBattlegroundShieldWindowLive);
        }
    }

    /** Collects the board facts used by the closed-by-default fourth shield slot. */
    public static FourthSlotFacts fourthSlotFacts(GameState gs,
                                                  SwccgGame game,
                                                  String playerId) {
        if (gs == null || game == null || playerId == null) {
            return new FourthSlotFacts(false, false, 0, false,
                    0, false, false, false, false, false);
        }

        boolean bothTheaters = occupiesBothTheaters(game, playerId);
        boolean anyBattleground = occupiesAnyBattleground(game, playerId);
        boolean anyCardTextBattleground =
                occupiesAnyCardTextBattleground(game, playerId);
        int opponentBattlegrounds = 0;
        int opponentCardTextBattlegrounds = 0;
        int ownBattlegrounds = 0;
        boolean opponentCanDrainThreePlus = false;
        boolean opponentDrainsNonBattleground = false;
        String opponent = game.getOpponent(playerId);

        try {
            com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying modifiers =
                    game.getModifiersQuerying();
            if (opponent != null) {
                for (PhysicalCard location : gs.getTopLocations()) {
                    if (location == null || location.getBlueprint() == null) continue;
                    if (com.gempukku.swccgo.filters.Filters.occupies(opponent)
                            .accepts(gs, modifiers, location)
                            && isBattleOrderBattleground(gs, game, location, modifiers)) {
                        opponentBattlegrounds++;
                    }
                    if (com.gempukku.swccgo.filters.Filters.occupies(opponent)
                            .accepts(gs, modifiers, location)
                            && isCardTextBattleground(gs, location, modifiers)) {
                        opponentCardTextBattlegrounds++;
                    }
                }
            }
        } catch (Exception e) {
            LOG.debug("V106 scan error: {}", e.getMessage());
        }

        try {
            com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying modifiers =
                    game.getModifiersQuerying();
            for (PhysicalCard location : gs.getTopLocations()) {
                if (location == null || location.getBlueprint() == null) continue;
                if (com.gempukku.swccgo.filters.Filters.occupies(playerId)
                        .accepts(gs, modifiers, location)
                        && isBattleOrderBattleground(gs, game, location, modifiers)) {
                    ownBattlegrounds++;
                }
                if (opponent != null) {
                    try {
                        float drain = ForceDrainProjection.projectedDamage(
                                gs, game, location, opponent);
                        boolean controlsForDrain =
                                com.gempukku.swccgo.filters.Filters
                                        .controlsForForceDrain(opponent)
                                        .accepts(gs, modifiers, location);
                        boolean drainProhibited =
                                modifiers.isProhibitedFromForceDrainingAtLocation(
                                        gs, location, opponent)
                                || com.gempukku.swccgo.filters.Filters.canSpot(
                                        game, null,
                                        com.gempukku.swccgo.filters.Filters.and(
                                                com.gempukku.swccgo.filters.Filters
                                                        .owner(opponent),
                                                com.gempukku.swccgo.filters.Filters
                                                        .at(location),
                                                com.gempukku.swccgo.filters.Filters
                                                        .cannotParticipateInForceDrain));
                        controlsForDrain = controlsForDrain && !drainProhibited;
                        if (controlsForDrain && drain >= 3f) {
                            opponentCanDrainThreePlus = true;
                        }
                        if (controlsForDrain && drain > 0f
                                && !isCardTextBattleground(gs, location, modifiers)) {
                            opponentDrainsNonBattleground = true;
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception e) {
            LOG.debug("V107 scan error: {}", e.getMessage());
        }

        boolean equivalentOnTable = battleOrderPlanEquivalentOnTable(gs);
        boolean battleOrderPlanLive = battleOrderLive(
                game, playerId,
                ForceDrainProjection.netDamageBalance(
                        gs, game, playerId) >= 2);
        return new FourthSlotFacts(bothTheaters, anyBattleground,
                opponentBattlegrounds, false, ownBattlegrounds,
                opponentCanDrainThreePlus, opponentDrainsNonBattleground,
                battleOrderPlanLive, equivalentOnTable,
                anyCardTextBattleground && opponentCardTextBattlegrounds < 2);
    }

    /**
     * Remembers an opponent non-battleground drain after the transient drain
     * window closes. The active state is exact; the same-turn attempted flag
     * covers the next normal bot decision after the drain has resolved.
     */
    public static boolean opponentNonBattlegroundDrainObservedNow(
            GameState gameState, SwccgGame game, String playerId) {
        if (gameState == null || game == null || playerId == null) return false;
        try {
            String opponent = game.getOpponent(playerId);
            if (opponent == null) return false;
            com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying modifiers =
                    game.getModifiersQuerying();
            if (gameState.isDuringForceDrainInitiatedBy(opponent)) {
                PhysicalCard location = gameState.getForceDrainLocation();
                return location != null
                        && !isCardTextBattleground(gameState, location, modifiers);
            }
            if (!opponent.equals(gameState.getCurrentPlayerId())) return false;
            for (PhysicalCard location : gameState.getTopLocations()) {
                if (location != null
                        && modifiers.isForceDrainAttemptedThisTurn(location)
                        && !isCardTextBattleground(gameState, location, modifiers)) {
                    return true;
                }
            }
        } catch (Exception e) {
            LOG.debug("Non-battleground drain observation error: {}", e.getMessage());
        }
        return false;
    }

    /** Returns whether the exact named shield is stacked and engine-playable. */
    public static boolean stackedCardTitlePlayable(GameState gameState,
                                                   SwccgGame game,
                                                   PhysicalCard source,
                                                   String preferredTitle) {
        if (gameState == null || game == null || source == null
                || preferredTitle == null) return false;
        try {
            List<PhysicalCard> stacked = gameState.getStackedCards(source);
            if (stacked == null) return false;
            for (PhysicalCard card : stacked) {
                String title = card != null ? card.getTitle() : null;
                if (title != null && title.equalsIgnoreCase(preferredTitle)
                        && com.gempukku.swccgo.filters.Filters.playable(source)
                                .accepts(gameState, game.getModifiersQuerying(), card)) {
                    return true;
                }
            }
        } catch (Exception e) {
            LOG.debug("Stacked shield scan error: {}", e.getMessage());
        }
        return false;
    }

    public static boolean battleOrderPlanEquivalentOnTable(GameState gs) {
        if (gs == null) return false;
        try {
            List<PhysicalCard> cards = gs.getAllPermanentCards();
            if (cards == null) return false;
            for (PhysicalCard card : cards) {
                if (card == null || card.getZone() == null
                        || !card.getZone().isInPlay()) {
                    continue;
                }
                String title = card.getTitle();
                if (title == null) continue;
                String lower = title.toLowerCase(Locale.ROOT);
                if (lower.contains("battle order")
                        || lower.contains("battle plan")) {
                    return true;
                }
            }
        } catch (Exception e) {
            LOG.debug("Battle Order/Plan equivalent scan error: {}",
                    e.getMessage());
        }
        return false;
    }

    /**
     * V51/V112/V105 shared predicate (2026-07-06, T2 MOVE #3): does playerId
     * occupy BOTH a battleground site AND a battleground system? Mirrors
     * Battle Order / Battle Plan's OWN occupation condition via the engine
     * (canSpot(occupies + battleground_site/system)) instead of any hand-rolled
     * power>0 or owner-present loop, so gate and card can never disagree
     * (the V140-class fix; the Verge game bug was a hand loop missing a Scarif
     * SYSTEM occupation the engine predicate catches). Fails closed on any
     * error (no false deploy). Body verbatim from rando CSE :8746-8762.
     */
    public static boolean occupiesBothTheaters(SwccgGame game, String playerId) {
        if (game == null || playerId == null) return false;
        try {
            boolean site = com.gempukku.swccgo.filters.Filters.canSpot(game, null,
                com.gempukku.swccgo.filters.Filters.and(
                    com.gempukku.swccgo.filters.Filters.occupies(playerId),
                    com.gempukku.swccgo.filters.Filters.battleground_site));
            com.gempukku.swccgo.filters.Filter eligibleSystem =
                    com.gempukku.swccgo.filters.Filters.battleground_system;
            if (isInvasionOnTable(game.getGameState())) {
                eligibleSystem = com.gempukku.swccgo.filters.Filters.and(
                        eligibleSystem,
                        com.gempukku.swccgo.filters.Filters.not(
                                com.gempukku.swccgo.filters.Filters.Naboo_system));
            }
            boolean sys = com.gempukku.swccgo.filters.Filters.canSpot(game, null,
                com.gempukku.swccgo.filters.Filters.and(
                    com.gempukku.swccgo.filters.Filters.occupies(playerId),
                    eligibleSystem));
            return site && sys;
        } catch (Exception e) {
            LOG.debug("occupiesBothTheaters error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * V51/V112 corrected-law input (2026-07-27, Hoth repair #2): does the
     * OPPONENT lack simultaneous battleground site + system occupation?
     * Card13_054 taxes the player initiating the drain unless THAT player
     * occupies both theaters (or Battle Plan is on table), so Battle Order
     * only pays off while the opponent lacks both. Same engine predicate as
     * occupiesBothTheaters (inherits the Invasion Naboo exclusion). Fails
     * closed: false on any error, so the gate stays shut.
     */
    public static boolean opponentLacksBothTheaters(SwccgGame game, String playerId) {
        if (game == null || playerId == null) return false;
        try {
            String opponent = game.getOpponent(playerId);
            if (opponent == null) return false;
            return !occupiesBothTheaters(game, opponent);
        } catch (Exception e) {
            LOG.debug("opponentLacksBothTheaters error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Battle Order / Battle Plan is live while the opponent lacks its
     * two-theater tax exemption and either we are exempt or their projected
     * drain lead is large enough to justify accepting the symmetric tax.
     */
    public static boolean battleOrderLive(SwccgGame game, String playerId,
                                          boolean opponentOutDrainsByTwoPlus) {
        return opponentLacksBothTheaters(game, playerId)
                && (occupiesBothTheaters(game, playerId)
                    || opponentOutDrainsByTwoPlus);
    }

    /**
     * V106 input (2026-07-06, T2 MOVE #3): does playerId occupy ANY battleground?
     * Engine predicate (canSpot(occupies + battleground)) — presence-based, so an
     * unpiloted ship at a battleground system counts (it occupies, power 0),
     * matching the play conditions of the presence-gated shields
     * (feedback_fourth_shield_conditional). Fails closed.
     */
    public static boolean occupiesAnyBattleground(SwccgGame game, String playerId) {
        if (game == null || playerId == null) return false;
        try {
            com.gempukku.swccgo.filters.Filter eligibleBattleground =
                    com.gempukku.swccgo.filters.Filters.battleground;
            if (isInvasionOnTable(game.getGameState())) {
                eligibleBattleground = com.gempukku.swccgo.filters.Filters.and(
                        eligibleBattleground,
                        com.gempukku.swccgo.filters.Filters.not(
                                com.gempukku.swccgo.filters.Filters.Naboo_system));
            }
            return com.gempukku.swccgo.filters.Filters.canSpot(game, null,
                com.gempukku.swccgo.filters.Filters.and(
                    com.gempukku.swccgo.filters.Filters.occupies(playerId),
                    eligibleBattleground));
        } catch (Exception e) {
            LOG.debug("occupiesAnyBattleground error: {}", e.getMessage());
            return false;
        }
    }

    private static boolean occupiesAnyCardTextBattleground(
            SwccgGame game, String playerId) {
        if (game == null || playerId == null) return false;
        try {
            return com.gempukku.swccgo.filters.Filters.canSpot(
                    game, null,
                    com.gempukku.swccgo.filters.Filters.and(
                            com.gempukku.swccgo.filters.Filters.occupies(playerId),
                            com.gempukku.swccgo.filters.Filters.battleground));
        } catch (Exception e) {
            LOG.debug("Card-text battleground occupation error: {}", e.getMessage());
            return false;
        }
    }

    public static boolean shouldReserveEopBattleOrderSlot(
            SwccgGame game, String playerId, PhysicalCard stackedPileSource,
            int shieldsOnTable) {
        if (game == null || playerId == null || stackedPileSource == null
                || shieldsOnTable != 3) return false;
        try {
            GameState gs = game.getGameState();
            if (gs == null) return false;
            boolean eop = com.gempukku.swccgo.filters.Filters.canSpot(
                game, null, com.gempukku.swccgo.filters.Filters.and(
                    com.gempukku.swccgo.filters.Filters.owner(playerId),
                    com.gempukku.swccgo.filters.Filters.Objective,
                    com.gempukku.swccgo.filters.Filters.or(
                        com.gempukku.swccgo.filters.Filters.title("Endor Operations"),
                        com.gempukku.swccgo.filters.Filters.title("Imperial Outpost"))));
            boolean equivalentPlayed =
                com.gempukku.swccgo.filters.Filters.canSpot(
                    game, null, com.gempukku.swccgo.filters.Filters.and(
                        com.gempukku.swccgo.filters.Filters.in_play,
                        com.gempukku.swccgo.filters.Filters.or(
                            com.gempukku.swccgo.filters.Filters.title("Battle Order"),
                            com.gempukku.swccgo.filters.Filters.title("Battle Plan"))));
            boolean site = com.gempukku.swccgo.filters.Filters.canSpot(
                game, null, com.gempukku.swccgo.filters.Filters.and(
                    com.gempukku.swccgo.filters.Filters.occupies(playerId),
                    com.gempukku.swccgo.filters.Filters.battleground_site,
                    com.gempukku.swccgo.filters.Filters.Endor_site));
            boolean system = com.gempukku.swccgo.filters.Filters.canSpot(
                game, null, com.gempukku.swccgo.filters.Filters.and(
                    com.gempukku.swccgo.filters.Filters.occupies(playerId),
                    com.gempukku.swccgo.filters.Filters.Endor_system,
                    com.gempukku.swccgo.filters.Filters.battleground_system));
            String opponent = game.getOpponent(playerId);
            boolean opponentOccupiesEndor = opponent != null
                && com.gempukku.swccgo.filters.Filters.canSpot(
                    game, null, com.gempukku.swccgo.filters.Filters.and(
                        com.gempukku.swccgo.filters.Filters.occupies(opponent),
                        com.gempukku.swccgo.filters.Filters.Endor_system));
            boolean endorSystemOnTable =
                com.gempukku.swccgo.filters.Filters.canSpot(
                    game, null, com.gempukku.swccgo.filters.Filters.and(
                        com.gempukku.swccgo.filters.Filters.Endor_system,
                        com.gempukku.swccgo.filters.Filters.battleground_system));
            boolean battleOrderStacked = false;
            List<PhysicalCard> stacked = gs.getStackedCards(stackedPileSource);
            if (stacked != null) {
                for (PhysicalCard card : stacked) {
                    if (card != null && "13_54".equals(card.getBlueprintId(true))) {
                        battleOrderStacked = true;
                        break;
                    }
                }
            }
            return EndorOperationsTacticalPolicy
                .shouldReserveShieldSlotForBattleOrder(
                    eop, equivalentPlayed, site, system,
                    endorSystemOnTable && !opponentOccupiesEndor
                        && battleOrderStacked);
        } catch (Exception e) {
            LOG.debug("EOP Battle Order reserve scan error: {}", e.getMessage());
            return false;
        }
    }

    /**
     * V276 AI-side rules correction. The 2023 Advanced Rulebook's Invasion entry
     * says Naboo remains a non-battleground system while Invasion is on table,
     * including after the objective flips. The live back-side card source does
     * not currently expose that modifier, so AI Battle Order/Plan accounting
     * enforces the published rule without changing card or engine code.
     */
    static boolean isInvasionNabooSystem(SwccgGame game, PhysicalCard location) {
        if (game == null || location == null) return false;
        try {
            GameState gameState = game.getGameState();
            return isInvasionOnTable(gameState)
                    && com.gempukku.swccgo.filters.Filters.Naboo_system.accepts(
                            gameState, game.getModifiersQuerying(), location);
        } catch (Exception e) {
            LOG.debug("V276 Invasion/Naboo classification error: {}", e.getMessage());
            return false;
        }
    }

    private static boolean isBattleOrderBattleground(
            GameState gameState, SwccgGame game, PhysicalCard location,
            com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying modifiers) {
        return !isInvasionNabooSystem(game, location)
                && com.gempukku.swccgo.filters.Filters.battleground.accepts(
                        gameState, modifiers, location);
    }

    private static boolean isCardTextBattleground(
            GameState gameState, PhysicalCard location,
            com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying modifiers) {
        return com.gempukku.swccgo.filters.Filters.battleground
                .accepts(gameState, modifiers, location);
    }

    // Hoth repair #1 (2026-07-27): package-visible so ObjectiveAnalyzer's
    // theater-package selector reuses the SAME Invasion Naboo exclusion as
    // occupiesBothTheaters, instead of forking a second table scan.
    static boolean isInvasionOnTable(GameState gameState) {
        if (gameState == null) return false;
        try {
            for (PhysicalCard card : gameState.getAllPermanentCards()) {
                if (card == null || card.getZone() == null
                        || !card.getZone().isInPlay()) continue;
                if ("14_113".equals(card.getBlueprintId(true))) return true;
            }
        } catch (Exception e) {
            LOG.debug("V276 Invasion table scan error: {}", e.getMessage());
        }
        return false;
    }

    /**
     * V117/V124 shared count (2026-07-06, T2 MOVE #3): how many of playerId's
     * defensive shields are on table (in play)? Verbatim from the V117 scan
     * (rando CSE :7888-7898): owner match + DEFENSIVE_SHIELD + Zone.isInPlay().
     * Callers keep their own thresholds/weights (>=3 hard-hold etc.).
     */
    public static int shieldsOnTable(GameState gs, String playerId) {
        int count = 0;
        if (gs == null || playerId == null) return count;
        try {
            for (PhysicalCard sc : gs.getAllPermanentCards()) {
                if (sc == null || sc.getBlueprint() == null) continue;
                if (!playerId.equals(sc.getOwner())) continue;
                if (sc.getBlueprint().getCardCategory() != CardCategory.DEFENSIVE_SHIELD) continue;
                com.gempukku.swccgo.common.Zone sz = sc.getZone();
                if (sz == null || !sz.isInPlay()) continue;
                count++;
            }
        } catch (Exception e) {
            LOG.debug("shieldsOnTable error: {}", e.getMessage());
        }
        return count;
    }
}
