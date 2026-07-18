package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
                                  boolean opponentDrainsNonBattleground) {
    }

    /** Collects the board facts used by the closed-by-default fourth shield slot. */
    public static FourthSlotFacts fourthSlotFacts(GameState gs,
                                                  SwccgGame game,
                                                  String playerId) {
        if (gs == null || game == null || playerId == null) {
            return new FourthSlotFacts(false, false, 0, false,
                    0, false, false);
        }

        boolean bothTheaters = occupiesBothTheaters(game, playerId);
        boolean anyBattleground = occupiesAnyBattleground(game, playerId);
        int opponentBattlegrounds = 0;
        boolean opponentHasDrainBonus = false;
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
                    float opponentPower = 0f;
                    try {
                        opponentPower = modifiers.getTotalPowerAtLocation(
                                gs, location, opponent, false, false);
                    } catch (Exception ignored) {
                    }
                    if (opponentPower > 0f
                            && com.gempukku.swccgo.filters.Filters.battleground
                                .accepts(gs, modifiers, location)) {
                        opponentBattlegrounds++;
                    }
                }
                for (PhysicalCard card : gs.getAllPermanentCards()) {
                    if (card == null || card.getBlueprint() == null) continue;
                    if (!opponent.equals(card.getOwner())) continue;
                    com.gempukku.swccgo.common.Zone zone = card.getZone();
                    if (zone == null || !zone.isInPlay()) continue;
                    if (com.gempukku.swccgo.filters.Filters.lightsaber
                            .accepts(gs, modifiers, card)) {
                        opponentHasDrainBonus = true;
                        break;
                    }
                    String gameText = card.getBlueprint().getGameText();
                    if (gameText == null) continue;
                    String lower = gameText.toLowerCase(Locale.ROOT);
                    if (lower.contains("force drain")
                            && (lower.contains("+1") || lower.contains("+2")
                                || lower.contains("+3"))) {
                        opponentHasDrainBonus = true;
                        break;
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
                float ownPower = 0f;
                try {
                    ownPower = modifiers.getTotalPowerAtLocation(
                            gs, location, playerId, false, false);
                } catch (Exception ignored) {
                }
                if (ownPower > 0f
                        && com.gempukku.swccgo.filters.Filters.battleground
                            .accepts(gs, modifiers, location)) {
                    ownBattlegrounds++;
                }
                if (opponent != null) {
                    try {
                        float drain = modifiers.getForceDrainAmount(gs, location, opponent);
                        if (drain >= 3f) {
                            opponentCanDrainThreePlus = true;
                        }
                        if (drain > 0f
                                && !com.gempukku.swccgo.filters.Filters.battleground
                                    .accepts(gs, modifiers, location)) {
                            opponentDrainsNonBattleground = true;
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception e) {
            LOG.debug("V107 scan error: {}", e.getMessage());
        }

        return new FourthSlotFacts(bothTheaters, anyBattleground,
                opponentBattlegrounds, opponentHasDrainBonus, ownBattlegrounds,
                opponentCanDrainThreePlus, opponentDrainsNonBattleground);
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
            boolean sys = com.gempukku.swccgo.filters.Filters.canSpot(game, null,
                com.gempukku.swccgo.filters.Filters.and(
                    com.gempukku.swccgo.filters.Filters.occupies(playerId),
                    com.gempukku.swccgo.filters.Filters.battleground_system));
            return site && sys;
        } catch (Exception e) {
            LOG.debug("occupiesBothTheaters error: {}", e.getMessage());
            return false;
        }
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
            return com.gempukku.swccgo.filters.Filters.canSpot(game, null,
                com.gempukku.swccgo.filters.Filters.and(
                    com.gempukku.swccgo.filters.Filters.occupies(playerId),
                    com.gempukku.swccgo.filters.Filters.battleground));
        } catch (Exception e) {
            LOG.debug("occupiesAnyBattleground error: {}", e.getMessage());
            return false;
        }
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
