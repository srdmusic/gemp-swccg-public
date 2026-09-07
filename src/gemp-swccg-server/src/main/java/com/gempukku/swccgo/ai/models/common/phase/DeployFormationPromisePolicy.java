package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.strategy.FormationSafety;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Shared non-additive owner for a funded same-site formation promise.
 *
 * A weak first body may deploy only because the phase plan names an exact,
 * affordable companion. Once that first body reaches the promised site, a
 * mid-phase replan must not strand it before the companion follows.
 */
public final class DeployFormationPromisePolicy {

    private DeployFormationPromisePolicy() {
    }

    public record PlannedInstruction(int permanentCardId,
                                     int currentCardId,
                                     String targetLocationId) {
    }

    public record Promise(int anchorPermanentCardId,
                          int anchorCurrentCardId,
                          int companionPermanentCardId,
                          int companionCurrentCardId,
                          String targetLocationId,
                          String companionName,
                          int companionCost) {
    }

    public record FundedPair(int firstPermanentCardId,
                             int firstCurrentCardId,
                             int companionPermanentCardId,
                             int companionCurrentCardId,
                             String targetLocationId,
                             int firstCost,
                             int companionCost) {
    }

    /** Detect an exact deployed anchor plus its cheapest exact planned companion. */
    public static Promise detect(SwccgGame game,
                                 String playerId,
                                 int forceAvailable,
                                 List<PlannedInstruction> instructions) {
        return detect(game, playerId, forceAvailable, 0, instructions);
    }

    /** Detect a deployed first piece while preserving the battle reserve. */
    public static Promise detect(SwccgGame game,
                                 String playerId,
                                 int forceAvailable,
                                 int forceReserved,
                                 List<PlannedInstruction> instructions) {
        if (game == null || playerId == null || instructions == null
                || instructions.size() < 2 || game.getGameState() == null
                || game.getModifiersQuerying() == null) {
            return null;
        }
        GameState gameState = game.getGameState();
        for (PlannedInstruction anchorInstruction : instructions) {
            PhysicalCard anchor = exactCard(
                    gameState, anchorInstruction.permanentCardId(),
                    anchorInstruction.currentCardId());
            PhysicalCard target = exactTarget(
                    gameState, anchorInstruction.targetLocationId());
            if (!isExactAnchorAtSite(
                    game, playerId, anchor, target,
                    anchorInstruction.targetLocationId())) {
                continue;
            }

            Promise best = null;
            for (PlannedInstruction companionInstruction : instructions) {
                if (companionInstruction == anchorInstruction
                        || !anchorInstruction.targetLocationId().equals(
                                companionInstruction.targetLocationId())) {
                    continue;
                }
                PhysicalCard companion = exactCard(
                        gameState, companionInstruction.permanentCardId(),
                        companionInstruction.currentCardId());
                Integer cost = exactAffordableCompanionCost(
                        game, playerId, companion, target,
                        spendableForce(forceAvailable, forceReserved));
                if (cost == null) continue;
                if (best == null || cost < best.companionCost()) {
                    best = new Promise(
                            anchorInstruction.permanentCardId(),
                            anchorInstruction.currentCardId(),
                            companionInstruction.permanentCardId(),
                            companionInstruction.currentCardId(),
                            anchorInstruction.targetLocationId(),
                            companion.getTitle(), cost);
                }
            }
            if (best != null) return best;
        }
        return null;
    }

    /** Revalidate a promise on every later decision in the same deploy phase. */
    public static boolean shouldContinue(SwccgGame game,
                                         String playerId,
                                         int forceAvailable,
                                         Promise promise) {
        return shouldContinue(
                game, playerId, forceAvailable, 0, promise);
    }

    /** Revalidate without spending the Force reserved for the coming battle. */
    public static boolean shouldContinue(SwccgGame game,
                                         String playerId,
                                         int forceAvailable,
                                         int forceReserved,
                                         Promise promise) {
        if (game == null || playerId == null || promise == null
                || game.getGameState() == null
                || game.getModifiersQuerying() == null) {
            return false;
        }
        GameState gameState = game.getGameState();
        PhysicalCard anchor = exactCard(
                gameState, promise.anchorPermanentCardId(),
                promise.anchorCurrentCardId());
        PhysicalCard target = exactTarget(
                gameState, promise.targetLocationId());
        if (!isExactAnchorAtSite(
                game, playerId, anchor, target,
                promise.targetLocationId())) {
            return false;
        }
        PhysicalCard companion = exactCard(
                gameState, promise.companionPermanentCardId(),
                promise.companionCurrentCardId());
        return exactAffordableCompanionCost(
                game, playerId, companion, target,
                spendableForce(forceAvailable, forceReserved)) != null;
    }

    /**
     * Proves that the selected first character and one exact same-site planned
     * companion are both legal and affordable after the battle reserve.
     */
    public static FundedPair detectFundedPair(
            SwccgGame game, String playerId,
            int forceAvailable, int forceReserved,
            int firstPermanentCardId, int firstCurrentCardId,
            String targetLocationId,
            List<PlannedInstruction> instructions) {
        if (game == null || playerId == null || targetLocationId == null
                || instructions == null || instructions.size() < 2
                || game.getGameState() == null
                || game.getModifiersQuerying() == null) {
            return null;
        }
        GameState gameState = game.getGameState();
        PhysicalCard target = exactTarget(gameState, targetLocationId);
        PhysicalCard first = exactCard(
                gameState, firstPermanentCardId, firstCurrentCardId);
        PlannedInstruction firstInstruction = null;
        for (PlannedInstruction instruction : instructions) {
            if (instruction.permanentCardId() == firstPermanentCardId
                    && instruction.currentCardId() == firstCurrentCardId
                    && targetLocationId.equals(
                            instruction.targetLocationId())) {
                firstInstruction = instruction;
                break;
            }
        }
        if (firstInstruction == null) return null;

        int spendable = spendableForce(forceAvailable, forceReserved);
        Integer firstCost = exactAffordableCompanionCost(
                game, playerId, first, target, spendable);
        if (firstCost == null) return null;

        FundedPair best = null;
        for (PlannedInstruction instruction : instructions) {
            if (instruction == firstInstruction
                    || !targetLocationId.equals(
                            instruction.targetLocationId())) {
                continue;
            }
            PhysicalCard companion = exactCard(
                    gameState, instruction.permanentCardId(),
                    instruction.currentCardId());
            Integer companionCost = exactAffordableCompanionCost(
                    game, playerId, companion, target,
                    spendable - firstCost);
            if (companionCost == null) continue;
            if (best == null
                    || companionCost < best.companionCost()) {
                best = new FundedPair(
                        firstPermanentCardId, firstCurrentCardId,
                        instruction.permanentCardId(),
                        instruction.currentCardId(), targetLocationId,
                        firstCost, companionCost);
            }
        }
        return best;
    }

    /**
     * Retarget one uncommitted planned character to a newly deployed, off-plan
     * single anchor at an opponent-occupied battleground. Existing funded pairs
     * stay intact, while an original buddy of the new anchor wins first choice.
     */
    public static Promise detectLiveContestedAnchor(
            SwccgGame game, String playerId,
            int forceAvailable, int forceReserved,
            List<PlannedInstruction> instructions,
            Set<Integer> baselineAnchorPermanentIds) {
        if (game == null || playerId == null || instructions == null
                || instructions.isEmpty() || game.getGameState() == null
                || game.getModifiersQuerying() == null) {
            return null;
        }
        GameState gameState = game.getGameState();
        String opponentId = gameState.getOpponent(playerId);
        if (opponentId == null) return null;
        Set<Integer> baseline = baselineAnchorPermanentIds == null
                ? Set.of() : baselineAnchorPermanentIds;
        int spendable = spendableForce(forceAvailable, forceReserved);
        Promise best = null;
        boolean bestOriginalBuddy = false;
        float bestCombatValue = Float.NEGATIVE_INFINITY;

        List<PhysicalCard> locations = gameState.getLocationsInOrder();
        if (locations == null) return null;
        for (PhysicalCard location : locations) {
            PhysicalCard anchor = singleFriendlyContestedAnchor(
                    game, playerId, opponentId, location);
            if (anchor == null
                    || baseline.contains(anchor.getPermanentCardId())) {
                continue;
            }
            String locationId = Integer.toString(location.getCardId());
            PlannedInstruction anchorInstruction = exactInstructionFor(
                    instructions, anchor.getPermanentCardId(),
                    anchor.getCardId());
            if (anchorInstruction != null
                    && locationId.equals(
                            anchorInstruction.targetLocationId())) {
                continue;
            }

            for (PlannedInstruction instruction : instructions) {
                if (instruction.permanentCardId()
                            == anchor.getPermanentCardId()
                        && instruction.currentCardId()
                            == anchor.getCardId()) {
                    continue;
                }
                PhysicalCard companion = exactCard(
                        gameState, instruction.permanentCardId(),
                        instruction.currentCardId());
                if (companion == null
                        || isMemberOfFundedHandPair(
                                game, playerId, forceAvailable,
                                forceReserved, instruction,
                                instructions)) {
                    continue;
                }
                Integer cost = exactAffordableCompanionCost(
                        game, playerId, companion, location, spendable);
                if (cost == null) continue;
                boolean originalBuddy = anchorInstruction != null
                        && anchorInstruction.targetLocationId() != null
                        && anchorInstruction.targetLocationId().equals(
                                instruction.targetLocationId());
                float combatValue = printedCombatValue(companion);
                if (best == null
                        || originalBuddy && !bestOriginalBuddy
                        || originalBuddy == bestOriginalBuddy
                            && combatValue > bestCombatValue
                        || originalBuddy == bestOriginalBuddy
                            && combatValue == bestCombatValue
                            && cost < best.companionCost()) {
                    best = new Promise(
                            anchor.getPermanentCardId(),
                            anchor.getCardId(),
                            instruction.permanentCardId(),
                            instruction.currentCardId(), locationId,
                            companion.getTitle(), cost);
                    bestOriginalBuddy = originalBuddy;
                    bestCombatValue = combatValue;
                }
            }
        }
        return best;
    }

    /** Snapshot anchors that already existed when a plan was accepted. */
    public static Set<Integer> snapshotLiveContestedAnchors(
            SwccgGame game, String playerId) {
        Set<Integer> result = new HashSet<>();
        if (game == null || playerId == null
                || game.getGameState() == null
                || game.getModifiersQuerying() == null) {
            return result;
        }
        GameState gameState = game.getGameState();
        String opponentId = gameState.getOpponent(playerId);
        List<PhysicalCard> locations = gameState.getLocationsInOrder();
        if (opponentId == null || locations == null) return result;
        for (PhysicalCard location : locations) {
            PhysicalCard anchor = singleFriendlyContestedAnchor(
                    game, playerId, opponentId, location);
            if (anchor != null) {
                result.add(anchor.getPermanentCardId());
            }
        }
        return result;
    }

    /** Exact predicate used to keep V201 from deferring behind a V64 veto. */
    public static boolean isEmptyMapuzoNonJediTrap(
            SwccgGame game, String playerId,
            SwccgCardBlueprint deployingBlueprint,
            PhysicalCard target) {
        if (game == null || playerId == null
                || deployingBlueprint == null || target == null
                || game.getGameState() == null
                || game.getModifiersQuerying() == null
                || deployingBlueprint.getCardCategory()
                    != CardCategory.CHARACTER
                || deployingBlueprint.hasKeyword(Keyword.JEDI_SURVIVOR)
                || target.getTitle() == null
                || !target.getTitle().toLowerCase(java.util.Locale.ROOT)
                    .contains("mapuzo")) {
            return false;
        }
        try {
            String opponentId = game.getGameState()
                    .getOpponent(playerId);
            return opponentId != null
                    && game.getModifiersQuerying()
                    .getTotalPowerAtLocation(
                            game.getGameState(), target, opponentId,
                            false, false) <= 0.0f;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean isExactAnchorAtSite(
            SwccgGame game, String playerId, PhysicalCard anchor,
            PhysicalCard target, String targetLocationId) {
        if (anchor == null || target == null || targetLocationId == null
                || anchor.getBlueprint() == null
                || target.getBlueprint() == null
                || anchor.getBlueprint().getCardCategory()
                        != CardCategory.CHARACTER
                || target.getBlueprint().getCardSubtype()
                        != CardSubtype.SITE
                || !playerId.equals(anchor.getOwner())
                || anchor.isUndercover()
                || anchor.getZone() == null
                || !anchor.getZone().isInPlay()) {
            return false;
        }
        try {
            GameState gameState = game.getGameState();
            PhysicalCard actualLocation = game.getModifiersQuerying()
                    .getLocationThatCardIsAt(gameState, anchor);
            if (actualLocation == null
                    || actualLocation.getCardId() != target.getCardId()
                    || FormationSafety.countFriendlyNonUndercoverCharacters(
                            gameState.getCardsAtLocation(target), playerId) != 1) {
                return false;
            }
            return Integer.toString(target.getCardId())
                    .equals(targetLocationId);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static Integer exactAffordableCompanionCost(
            SwccgGame game, String playerId, PhysicalCard companion,
            PhysicalCard target, int forceAvailable) {
        if (companion == null || target == null
                || companion.getBlueprint() == null
                || companion.getBlueprint().getCardCategory()
                        != CardCategory.CHARACTER
                || !playerId.equals(companion.getOwner())
                || companion.getZone() != Zone.HAND
                || companion.isUndercover()) {
            return null;
        }
        try {
            GameState gameState = game.getGameState();
            if (!Filters.deployableToLocation(
                    companion, Filters.sameCardId(target), false, 0.0f)
                    .accepts(gameState, game.getModifiersQuerying(),
                            companion)) {
                return null;
            }
            float exactCost = game.getModifiersQuerying().getDeployCost(
                    gameState, companion, companion, target,
                    false, null, false, 0.0f, null, true);
            if (!Float.isFinite(exactCost)) return null;
            int payable = (int) Math.ceil(Math.max(0.0f, exactCost));
            return payable <= forceAvailable ? payable : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static int spendableForce(
            int forceAvailable, int forceReserved) {
        return Math.max(0, forceAvailable - Math.max(0, forceReserved));
    }

    private static PlannedInstruction exactInstructionFor(
            List<PlannedInstruction> instructions,
            int permanentCardId, int currentCardId) {
        for (PlannedInstruction instruction : instructions) {
            if (instruction.permanentCardId() == permanentCardId
                    && instruction.currentCardId() == currentCardId) {
                return instruction;
            }
        }
        return null;
    }

    private static boolean isMemberOfFundedHandPair(
            SwccgGame game, String playerId,
            int forceAvailable, int forceReserved,
            PlannedInstruction instruction,
            List<PlannedInstruction> instructions) {
        return detectFundedPair(
                game, playerId, forceAvailable, forceReserved,
                instruction.permanentCardId(),
                instruction.currentCardId(),
                instruction.targetLocationId(), instructions) != null;
    }

    private static PhysicalCard singleFriendlyContestedAnchor(
            SwccgGame game, String playerId, String opponentId,
            PhysicalCard location) {
        if (location == null || location.getBlueprint() == null
                || location.getBlueprint().getCardSubtype()
                    != CardSubtype.SITE) {
            return null;
        }
        try {
            GameState gameState = game.getGameState();
            if (!game.getModifiersQuerying().isBattleground(
                    gameState, location, null)
                    || !game.getModifiersQuerying().occupiesLocation(
                            gameState, location, opponentId)) {
                return null;
            }
            PhysicalCard anchor = null;
            int friendlyCount = 0;
            for (PhysicalCard card : gameState.getCardsAtLocation(location)) {
                if (card == null || card.getBlueprint() == null
                        || card.getBlueprint().getCardCategory()
                            != CardCategory.CHARACTER
                        || card.isUndercover()) {
                    continue;
                }
                if (playerId.equals(card.getOwner())) {
                    friendlyCount++;
                    anchor = card;
                }
            }
            return friendlyCount == 1 ? anchor : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static float printedCombatValue(PhysicalCard card) {
        if (card == null || card.getBlueprint() == null) return 0.0f;
        float power = card.getBlueprint().hasPowerAttribute()
                && card.getBlueprint().getPower() != null
                ? card.getBlueprint().getPower() : 0.0f;
        float ability = card.getBlueprint().hasAbilityAttribute()
                && card.getBlueprint().getAbility() != null
                ? card.getBlueprint().getAbility() : 0.0f;
        return power + ability;
    }

    private static PhysicalCard exactCard(
            GameState gameState, int permanentCardId, int currentCardId) {
        PhysicalCard card = gameState.findCardByPermanentId(permanentCardId);
        return card != null && card.getCardId() == currentCardId
                ? card : null;
    }

    private static PhysicalCard exactTarget(
            GameState gameState, String targetLocationId) {
        if (targetLocationId == null) return null;
        try {
            return gameState.findCardById(
                    Integer.parseInt(targetLocationId));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
