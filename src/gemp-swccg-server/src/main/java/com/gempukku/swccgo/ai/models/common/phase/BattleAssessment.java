package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.strategy.FormationSafety;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Immutable route assessment consumed by the BATTLE response owner. */
public record BattleAssessment(
        BattleWindowRoute route,
        List<BattleInitiationAssessment> initiations,
        boolean optionalImmuneForfeit) {

    @FunctionalInterface
    public interface PredictionSource {
        BattlePredictionAssessment predict(
                float ourPower, float ourWeaponBonus, int ourDestinyDraws,
                float opponentPower, float opponentWeaponBonus,
                int opponentDestinyDraws);
    }

    public BattleAssessment {
        Objects.requireNonNull(route, "route");
        if (route == BattleWindowRoute.LEGACY_UNOWNED) {
            throw new IllegalArgumentException("LEGACY_UNOWNED has no assessment");
        }
        initiations = List.copyOf(initiations);
    }

    public static BattleAssessment from(BattleFacts facts) {
        return from(facts, BattleDeployIntent.none());
    }

    public static BattleAssessment from(BattleFacts facts,
                                        BattleDeployIntent deployIntent) {
        Objects.requireNonNull(deployIntent, "deployIntent");
        ArrayList<BattleInitiationAssessment> initiations = new ArrayList<>();
        for (BattleFacts.Candidate candidate : facts.candidates()) {
            if (candidate.role() != BattleCandidateRole.INITIATE) {
                continue;
            }
            try {
                initiations.add(new BattleInitiationAssessment(
                        candidate.ordinal(), Integer.parseInt(candidate.cardId()),
                        deployIntent));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "initiation target is not a physical card id", e);
            }
        }
        return new BattleAssessment(facts.route(), initiations,
                facts.optionalImmuneForfeit());
    }

    /** Freeze every board read and the one predictor result for each typed initiation. */
    public static BattleAssessment capture(BattleFacts facts,
                                           BattleDeployIntent deployIntent,
                                           SwccgGame game,
                                           GameState gameState,
                                           String playerId,
                                           PredictionSource predictionSource) {
        Objects.requireNonNull(facts, "facts");
        Objects.requireNonNull(deployIntent, "deployIntent");
        ArrayList<BattleInitiationAssessment> initiations = new ArrayList<>();
        for (BattleFacts.Candidate candidate : facts.candidates()) {
            if (candidate.role() != BattleCandidateRole.INITIATE) {
                continue;
            }
            int targetCardId;
            try {
                targetCardId = Integer.parseInt(candidate.cardId());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "initiation target is not a physical card id", e);
            }
            BattleLocationAssessment location = captureLocation(
                    targetCardId, game, gameState, playerId, predictionSource);
            initiations.add(new BattleInitiationAssessment(
                    candidate.ordinal(), targetCardId, deployIntent, location));
        }
        return new BattleAssessment(facts.route(), initiations,
                facts.optionalImmuneForfeit());
    }

    public BattleInitiationAssessment initiationAt(int ordinal) {
        for (BattleInitiationAssessment assessment : initiations) {
            if (assessment.candidateOrdinal() == ordinal) {
                return assessment;
            }
        }
        return null;
    }

    private static BattleLocationAssessment captureLocation(
            int targetCardId,
            SwccgGame game,
            GameState gameState,
            String playerId,
            PredictionSource predictionSource) {
        if (game == null || gameState == null || playerId == null) {
            return BattleLocationAssessment.unknown();
        }
        try {
            String opponentId = gameState.getOpponent(playerId);
            PhysicalCard target = gameState.findCardById(targetCardId);
            if (opponentId == null || target == null
                    || !gameState.getTopLocations().contains(target)) {
                return BattleLocationAssessment.unknown();
            }

            float ourPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                    gameState, target, playerId, false, false);
            float opponentPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                    gameState, target, opponentId, false, false);
            float ourAbility = game.getModifiersQuerying().getTotalAbilityAtLocation(
                    gameState, playerId, target);
            float opponentAbility = game.getModifiersQuerying().getTotalAbilityAtLocation(
                    gameState, opponentId, target);

            float ourWeaponBonus = 0f;
            float opponentWeaponBonus = 0f;
            int ourCharacters = 0;
            int opponentCharacters = 0;
            boolean vader = false;
            boolean vaderArmed = false;
            boolean luke = false;
            boolean jedi = false;
            Set<Integer> friendlyCardIds = new HashSet<>();
            for (PhysicalCard card : gameState.getCardsAtLocation(target)) {
                if (card == null || card.getBlueprint() == null
                        || card.getBlueprint().getCardCategory()
                            != CardCategory.CHARACTER) {
                    continue;
                }
                String title = card.getTitle() != null
                        ? card.getTitle().toLowerCase(java.util.Locale.ROOT) : "";
                float attachedWeaponBonus = attachedWeaponBonus(gameState, card);
                BattleWeaponFacts permanentWeapon = BattleWeaponFacts.from(card);
                if (playerId.equals(card.getOwner())) {
                    ourCharacters++;
                    friendlyCardIds.add(card.getCardId());
                    ourWeaponBonus += attachedWeaponBonus;
                    if (title.contains("vader")) {
                        vader = true;
                        vaderArmed = attachedWeaponBonus > 0f
                                || (permanentWeapon.ownsPermanentWeapon()
                                    && permanentWeapon.canHitCharacter());
                    }
                } else if (opponentId.equals(card.getOwner())) {
                    opponentCharacters++;
                    opponentWeaponBonus += attachedWeaponBonus;
                    if (permanentWeapon.ownsPermanentWeapon()) {
                        opponentWeaponBonus += title.contains("lightsaber") ? 5f : 3f;
                    }
                    luke |= title.contains("luke");
                    jedi |= isJediOrPadawan(title);
                }
            }

            boolean ihyn = false;
            if (vader) {
                for (PhysicalCard handCard : gameState.getHand(playerId)) {
                    String title = handCard != null && handCard.getTitle() != null
                            ? handCard.getTitle().toLowerCase(java.util.Locale.ROOT) : "";
                    if (title.contains("i have you now")) {
                        ihyn = true;
                        ourWeaponBonus += 3f;
                        break;
                    }
                }
            }

            int ourDraws = Math.max(1, Math.min(4, ourCharacters));
            int opponentDraws = Math.max(1, Math.min(4, opponentCharacters));
            boolean destinyEligible = ourAbility
                    >= FormationSafety.DESTINY_ABILITY_THRESHOLD;
            String formationVeto = destinyEligible ? null : String.format(
                    "L2 NO-DESTINY BATTLE: total ability %.0f < %.0f at %s — zero normal battle destiny draws (engine BattleDestiny threshold)",
                    ourAbility, FormationSafety.DESTINY_ABILITY_THRESHOLD,
                    target.getTitle());
            boolean targetOverpower = ourPower > 0f && opponentPower > 0f
                    && ourPower - opponentPower >= 8f;

            BattlePredictionAssessment prediction =
                    BattlePredictionAssessment.unknown();
            if (ourPower > 0f && opponentPower > 0f
                    && predictionSource != null) {
                try {
                    BattlePredictionAssessment predicted = predictionSource.predict(
                            ourPower, ourWeaponBonus, ourDraws,
                            opponentPower, opponentWeaponBonus, opponentDraws);
                    if (predicted != null) {
                        prediction = predicted;
                    }
                } catch (RuntimeException ignored) {
                    prediction = BattlePredictionAssessment.unknown();
                }
            }
            return new BattleLocationAssessment(
                    true, ourPower, opponentPower, ourAbility, opponentAbility,
                    ourWeaponBonus, opponentWeaponBonus, ourDraws, opponentDraws,
                    destinyEligible, formationVeto, targetOverpower,
                    friendlyCardIds, vader, vaderArmed, luke, jedi, ihyn,
                    prediction);
        } catch (RuntimeException e) {
            return BattleLocationAssessment.unknown();
        }
    }

    private static float attachedWeaponBonus(GameState gameState,
                                             PhysicalCard character) {
        float bonus = 0f;
        List<PhysicalCard> attachments = gameState.getAttachedCards(character);
        if (attachments == null) {
            return 0f;
        }
        for (PhysicalCard attachment : attachments) {
            if (attachment == null || attachment.getBlueprint() == null
                    || attachment.getBlueprint().getCardCategory()
                        != CardCategory.WEAPON) {
                continue;
            }
            String title = attachment.getTitle() != null
                    ? attachment.getTitle().toLowerCase(java.util.Locale.ROOT) : "";
            bonus += title.contains("lightsaber") ? 5f : 3f;
        }
        return bonus;
    }

    private static boolean isJediOrPadawan(String title) {
        return title.contains("jedi") || title.contains("padawan")
                || title.contains("luke") || title.contains("obi-wan")
                || title.contains("yoda") || title.contains("ahsoka")
                || title.contains("ezra") || title.contains("kanan")
                || title.contains("rey") || title.contains("sabine");
    }
}
