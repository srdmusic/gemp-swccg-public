package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.common.AiCardHelper;
import com.gempukku.swccgo.ai.common.AiPriorityCards;
import com.gempukku.swccgo.ai.models.common.strategy.ObjectiveAnalyzer;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Immutable physical-candidate facts consumed by the shared BATTLE-3 policy. */
public final class BattleForfeitFacts {

    public record ImmunityFacts(float exactImmunity,
                                float lessThanImmunity) {
        public static ImmunityFacts none() {
            return new ImmunityFacts(0.0f, 0.0f);
        }

        public boolean immuneTo(int attrition) {
            if (exactImmunity > 0.0f) {
                return exactImmunity == attrition;
            }
            return lessThanImmunity > attrition;
        }
    }

    public record SoloPowerFacts(boolean available,
                                 int friendlyCharacterCount,
                                 float opponentPower,
                                 float friendlyPower) {
        public static SoloPowerFacts unavailable() {
            return new SoloPowerFacts(false, 0, 0.0f, 0.0f);
        }

        public boolean isSolo() {
            return available && friendlyCharacterCount <= 1;
        }

        public float opponentPowerGap() {
            return opponentPower - friendlyPower;
        }
    }

    public record CandidateFacts(String actionId,
                                 boolean blueprintPresent,
                                 CardCategory category,
                                 float forfeitValue,
                                 boolean hit,
                                 boolean dead,
                                 boolean forceLossOption,
                                 boolean attachedHostHit,
                                 boolean armed,
                                 ImmunityFacts immunity,
                                 SoloPowerFacts soloPower,
                                 Float power,
                                 Float ability,
                                 boolean capitalShip,
                                 boolean priorityCard) {
        public CandidateFacts {
            Objects.requireNonNull(actionId, "actionId");
            Objects.requireNonNull(immunity, "immunity");
            Objects.requireNonNull(soloPower, "soloPower");
            if (actionId.isBlank()) {
                throw new IllegalArgumentException("candidate actionId must be nonblank");
            }
        }

        public boolean weapon() {
            return category == CardCategory.WEAPON;
        }

        public boolean character() {
            return category == CardCategory.CHARACTER;
        }

        public boolean gameWinner() {
            return power != null && power >= 6.0f
                    && ability != null && ability >= 4.0f;
        }
    }

    public record ObjectiveFlags(boolean requiredForFlip,
                                 boolean pullable) {
        public static ObjectiveFlags none() {
            return new ObjectiveFlags(false, false);
        }
    }

    public record CandidateSetFacts(boolean hasHitCandidates,
                                    boolean hasDeadCandidates,
                                    Optional<String> bestHitActionId,
                                    float bestHitForfeit) {
        public CandidateSetFacts {
            Objects.requireNonNull(bestHitActionId, "bestHitActionId");
        }

        public static CandidateSetFacts empty() {
            return new CandidateSetFacts(false, false, Optional.empty(),
                    Float.MAX_VALUE);
        }
    }

    public record DecisionFacts(int attritionRemaining,
                                int damageRemaining,
                                CandidateSetFacts candidateSet) {
        public DecisionFacts {
            Objects.requireNonNull(candidateSet, "candidateSet");
        }

        public boolean smallPureDamage() {
            return damageRemaining > 0 && damageRemaining <= 2
                    && attritionRemaining <= 0;
        }
    }

    public record FlipGateFormationSelectionFacts(
            Map<String, ObjectiveAnalyzer.FlipGateFormationRole> rolesByActionId,
            boolean hasUnprotectedLegalAlternative) {
        public FlipGateFormationSelectionFacts {
            Objects.requireNonNull(rolesByActionId, "rolesByActionId");
            rolesByActionId = Collections.unmodifiableMap(
                    new LinkedHashMap<>(rolesByActionId));
        }

        public ObjectiveAnalyzer.FlipGateFormationRole roleFor(
                String actionId) {
            return rolesByActionId.getOrDefault(actionId,
                    ObjectiveAnalyzer.FlipGateFormationRole.NONE);
        }
    }

    private BattleForfeitFacts() {
    }

    /**
     * Reads the exact Invasion formation role for every offered loss. An
     * optional pass is an alternative. A Force-loss card is an alternative
     * only when no attrition remains, because Force loss cannot satisfy attrition.
     */
    public static FlipGateFormationSelectionFacts readFlipGateFormationSelection(
            List<String> actionIds,
            GameState gameState,
            SwccgGame game,
            String playerId,
            ObjectiveAnalyzer objectiveAnalyzer,
            boolean passLegal,
            int attritionRemaining) {
        Map<String, ObjectiveAnalyzer.FlipGateFormationRole> roles =
                new LinkedHashMap<>();
        boolean hasAlternative = passLegal;
        if (actionIds == null || gameState == null) {
            return new FlipGateFormationSelectionFacts(
                    roles, hasAlternative);
        }

        for (String actionId : actionIds) {
            if (actionId == null || actionId.isBlank()) continue;
            ObjectiveAnalyzer.FlipGateFormationRole role =
                    ObjectiveAnalyzer.FlipGateFormationRole.NONE;
            PhysicalCard card = null;
            try {
                card = gameState.findCardById(Integer.parseInt(actionId));
                if (card != null && objectiveAnalyzer != null) {
                    role = objectiveAnalyzer
                            .classifyGateFormationPieceIfRemoved(
                                    game, playerId, card);
                    if (role == null) {
                        role = ObjectiveAnalyzer.FlipGateFormationRole.NONE;
                    }
                }
            } catch (Exception ignored) {
                // Unknown candidates cannot prove a safe alternative.
            }
            roles.put(actionId, role);

            if (card != null
                    && role == ObjectiveAnalyzer.FlipGateFormationRole.NONE
                    && (!ForceLossFacts.isForceLossZone(card)
                        || attritionRemaining <= 0)) {
                hasAlternative = true;
            }
        }
        return new FlipGateFormationSelectionFacts(roles, hasAlternative);
    }

    /** Reads one offered physical candidate without changing engine state. */
    public static CandidateFacts readCandidate(String actionId,
                                               PhysicalCard card,
                                               SwccgGame game,
                                               String playerId,
                                               boolean forceLossOption,
                                               int attritionRemaining,
                                               int damageRemaining,
                                               boolean combinedRoute) {
        if (card == null) {
            return new CandidateFacts(actionId, false, null, 0.0f,
                    false, false, forceLossOption, false, false,
                    ImmunityFacts.none(), SoloPowerFacts.unavailable(),
                    null, null, false, false);
        }

        SwccgCardBlueprint blueprint = card.getBlueprint();
        CardCategory category = blueprint != null ? blueprint.getCardCategory() : null;
        Float forfeit = blueprint != null && blueprint.hasForfeitAttribute()
                ? blueprint.getForfeit() : null;
        float forfeitValue = forfeit != null ? forfeit : 0.0f;
        boolean hit = card.isHit();
        PhysicalCard attachedHost = card.getAttachedTo();
        boolean attachedHostHit = attachedHost != null && attachedHost.isHit();
        if (combinedRoute && (forceLossOption || category == CardCategory.WEAPON)) {
            return new CandidateFacts(actionId, blueprint != null, category,
                    forfeitValue, hit, false, forceLossOption, attachedHostHit,
                    false, ImmunityFacts.none(), SoloPowerFacts.unavailable(),
                    null, null, false, false);
        }

        boolean dead = false;
        try {
            if (game != null && playerId != null) {
                dead = AiCardHelper.isDeadCard(card, game, playerId);
            }
        } catch (Exception ignored) {
            // Preserve the legacy not-dead fallback.
        }

        boolean armed = false;
        try {
            GameState liveState = game != null ? game.getGameState() : null;
            if (liveState != null) {
                for (PhysicalCard permanent : liveState.getAllPermanentCards()) {
                    if (permanent != null && permanent.getAttachedTo() == card
                            && permanent.getBlueprint() != null
                            && permanent.getBlueprint().getCardCategory() == CardCategory.WEAPON) {
                        armed = true;
                        break;
                    }
                }
            }
        } catch (Exception ignored) {
            // Preserve the legacy unarmed fallback.
        }

        ImmunityFacts immunity = ImmunityFacts.none();
        if (game != null) {
            try {
                ModifiersQuerying modifiers = game.getModifiersQuerying();
                GameState liveState = game.getGameState();
                if (modifiers != null && liveState != null) {
                    immunity = new ImmunityFacts(
                            modifiers.getImmunityToAttritionOfExactly(liveState, card),
                            modifiers.getImmunityToAttritionLessThan(liveState, card));
                }
            } catch (Exception ignored) {
                // Preserve the legacy no-immunity fallback.
            }
        }

        SoloPowerFacts soloPower = SoloPowerFacts.unavailable();
        if (!hit && !dead && immunity.immuneTo(attritionRemaining)
                && attritionRemaining > 0 && damageRemaining > 0
                && forfeitValue > 0.0f && game != null && playerId != null) {
            try {
                PhysicalCard location = card.getAtLocation();
                GameState liveState = game.getGameState();
                ModifiersQuerying modifiers = game.getModifiersQuerying();
                String opponentId = game.getOpponent(playerId);
                if (location != null && liveState != null
                        && modifiers != null && opponentId != null) {
                    int friendlyCharacters = 0;
                    for (PhysicalCard atLocation : liveState.getCardsAtLocation(location)) {
                        if (atLocation != null && playerId.equals(atLocation.getOwner())
                                && atLocation.getBlueprint() != null
                                && atLocation.getBlueprint().getCardCategory()
                                        == CardCategory.CHARACTER) {
                            friendlyCharacters++;
                        }
                    }
                    soloPower = new SoloPowerFacts(true, friendlyCharacters,
                            modifiers.getTotalPowerAtLocation(
                                    liveState, location, opponentId, false, false),
                            modifiers.getTotalPowerAtLocation(
                                    liveState, location, playerId, false, false));
                }
            } catch (Exception ignored) {
                // Preserve the legacy unavailable-solo fallback.
            }
        }

        Float power = blueprint != null && blueprint.hasPowerAttribute()
                ? blueprint.getPower() : null;
        Float ability = blueprint != null && blueprint.hasAbilityAttribute()
                ? blueprint.getAbility() : null;
        boolean capitalShip = false;
        try {
            capitalShip = blueprint != null && blueprint.getCardSubtype() == CardSubtype.CAPITAL;
        } catch (Exception ignored) {
            // Preserve the legacy non-capital fallback.
        }
        boolean priorityCard = false;
        try {
            priorityCard = card.getTitle() != null
                    && AiPriorityCards.isPriorityCardByTitle(card.getTitle());
        } catch (Exception ignored) {
            // Preserve the legacy non-priority fallback.
        }

        return new CandidateFacts(actionId, blueprint != null, category,
                forfeitValue, hit, dead, forceLossOption, attachedHostHit,
                armed, immunity, soloPower, power, ability, capitalShip,
                priorityCard);
    }

    /**
     * Reproduces the combined-route first pass over adapter-resolved physical candidates.
     * Ties retain the first lowest-forfeit hit, exactly like the legacy strict-less-than scan.
     */
    public static CandidateSetFacts readCandidateSet(List<CandidateFacts> candidates) {
        Objects.requireNonNull(candidates, "candidates");
        boolean hasHit = false;
        boolean hasDead = false;
        String bestHitActionId = null;
        float bestHitForfeit = Float.MAX_VALUE;

        for (CandidateFacts candidate : candidates) {
            Objects.requireNonNull(candidate, "candidate");
            if (candidate.hit()) {
                hasHit = true;
                float forfeit = candidate.blueprintPresent()
                        ? candidate.forfeitValue() : 0.0f;
                if (forfeit < bestHitForfeit) {
                    bestHitForfeit = forfeit;
                    bestHitActionId = candidate.actionId();
                }
            }
            if (candidate.dead()) {
                hasDead = true;
            }
        }

        return new CandidateSetFacts(hasHit, hasDead,
                Optional.ofNullable(bestHitActionId), bestHitForfeit);
    }
}
