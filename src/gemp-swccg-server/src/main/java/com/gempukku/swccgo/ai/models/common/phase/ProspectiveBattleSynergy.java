package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.ModifierType;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Exact, fail-open facts for fixed character pairings that add battle destiny.
 *
 * This intentionally reports only additional draws. It does not infer minimum
 * draws, variable draws, destiny values, draw limits, or interrupts from card
 * text. Unsupported card text remains neutral.
 */
public final class ProspectiveBattleSynergy {
    public enum Relation {
        PARTICIPATING_WITH,
        COLOCATED_WITH
    }

    private enum Kind {
        PASSIVE_ADD,
        ACTION_ADD
    }

    public enum RuleId {
        TARKIN_V_WITH_VADER_OR_IMPERIAL_LEADER,
        BOBA_FETT_V_WITH_VADER,
        BOBA_FETT_WITH_BLASTER_RIFLE_WITH_HAN_OR_JABBA,
        HAN_WITH_HEAVY_BLASTER_PISTOL_WITH_LUKE_OR_CHEWIE,
        LEIA_WITH_BLASTER_RIFLE_WITH_HAN,
        FOUR_LOM_WITH_CONCUSSION_RIFLE_WITH_JABBA_OR_ZUCKUSS,
        CHEWIE_WITH_BLASTER_RIFLE_WITH_C3PO,
        QUI_GON_WITH_LIGHTSABER_WITH_MAUL,
        PRINCESS_LEIA_V_WITH_HAN_OR_VADER,
        SOLO_V_WITH_CHEWIE_OR_REY,
        HAN_SOLO_V_WITH_CHEWIE,
        SIM_ALOO_WITH_EMPEROR,
        NIEN_NUNB_WITH_GENERAL_CALRISSIAN
    }

    public record Projection(
            String playerId,
            Collection<PhysicalCard> participants,
            Collection<PhysicalCard> cardsAtLocation) {
    }

    public record Match(
            RuleId ruleId,
            int sourcePermanentCardId,
            String sourceBlueprintId,
            int additionalBattleDestinyDraws) {
    }

    public record Assessment(
            int additionalBattleDestinyDraws,
            List<Match> matches) {
        private static Assessment none() {
            return new Assessment(0, List.of());
        }

        public Assessment {
            matches = List.copyOf(matches);
        }
    }

    private record Rule(
            RuleId id,
            Set<String> sourceBlueprintIds,
            Kind kind,
            Relation relation,
            Filter partner,
            int addedDraws) {
    }

    private static final List<Rule> RULES = List.of(
            actionRule(RuleId.TARKIN_V_WITH_VADER_OR_IMPERIAL_LEADER,
                    Set.of("200_82", "601_86"),
                    Relation.PARTICIPATING_WITH,
                    Filters.or(Filters.Vader, Filters.Imperial_leader)),
            rule(RuleId.BOBA_FETT_V_WITH_VADER,
                    Set.of("206_9"), Relation.COLOCATED_WITH,
                    Filters.Vader),
            rule(RuleId.BOBA_FETT_WITH_BLASTER_RIFLE_WITH_HAN_OR_JABBA,
                    Set.of("108_5"), Relation.COLOCATED_WITH,
                    Filters.or(Filters.Han, Filters.Jabba)),
            rule(RuleId.HAN_WITH_HEAVY_BLASTER_PISTOL_WITH_LUKE_OR_CHEWIE,
                    Set.of("108_1"), Relation.COLOCATED_WITH,
                    Filters.or(Filters.Luke, Filters.Chewie)),
            rule(RuleId.LEIA_WITH_BLASTER_RIFLE_WITH_HAN,
                    Set.of("108_2"), Relation.COLOCATED_WITH,
                    Filters.Han),
            rule(RuleId.FOUR_LOM_WITH_CONCUSSION_RIFLE_WITH_JABBA_OR_ZUCKUSS,
                    Set.of("109_6"), Relation.COLOCATED_WITH,
                    Filters.or(Filters.Jabba, Filters.Zuckuss)),
            rule(RuleId.CHEWIE_WITH_BLASTER_RIFLE_WITH_C3PO,
                    Set.of("109_1"), Relation.COLOCATED_WITH,
                    Filters.C3PO),
            rule(RuleId.QUI_GON_WITH_LIGHTSABER_WITH_MAUL,
                    Set.of("14_27"), Relation.COLOCATED_WITH,
                    Filters.Maul),
            rule(RuleId.PRINCESS_LEIA_V_WITH_HAN_OR_VADER,
                    Set.of("201_5"), Relation.COLOCATED_WITH,
                    Filters.or(Filters.Han, Filters.Vader)),
            rule(RuleId.SOLO_V_WITH_CHEWIE_OR_REY,
                    Set.of("204_11"), Relation.COLOCATED_WITH,
                    Filters.or(Filters.Chewie, Filters.Rey)),
            rule(RuleId.HAN_SOLO_V_WITH_CHEWIE,
                    Set.of("224_16"), Relation.COLOCATED_WITH,
                    Filters.Chewie),
            rule(RuleId.SIM_ALOO_WITH_EMPEROR,
                    Set.of("9_120"), Relation.COLOCATED_WITH,
                    Filters.Emperor),
            rule(RuleId.NIEN_NUNB_WITH_GENERAL_CALRISSIAN,
                    Set.of("9_28"), Relation.COLOCATED_WITH,
                    Filters.General_Calrissian));

    private ProspectiveBattleSynergy() {
    }

    /**
     * Builds a prospective location projection. Existing cards must be able to
     * participate in a battle there; prospective cards are assumed legal by the
     * caller's deploy or move gate.
     */
    public static Projection projectAtLocation(
            SwccgGame game,
            String playerId,
            PhysicalCard location,
            Collection<PhysicalCard> prospectiveParticipants) {
        if (game == null || playerId == null || location == null
                || game.getGameState() == null
                || game.getModifiersQuerying() == null) {
            return new Projection(playerId, List.of(), List.of());
        }

        GameState gameState = game.getGameState();
        ModifiersQuerying modifiers = game.getModifiersQuerying();
        List<PhysicalCard> colocated = new ArrayList<>();
        List<PhysicalCard> participants = new ArrayList<>();
        Collection<PhysicalCard> current =
                gameState.getCardsAtLocation(location);
        if (current != null) {
            colocated.addAll(current);
            Filter canParticipate =
                    Filters.canParticipateInBattleAt(location, playerId);
            for (PhysicalCard card : current) {
                try {
                    if (card != null && canParticipate.accepts(
                            gameState, modifiers, card)) {
                        participants.add(card);
                    }
                } catch (RuntimeException ignored) {
                    // Unknown current participation is not a proven synergy.
                }
            }
        }
        if (prospectiveParticipants != null) {
            colocated.addAll(prospectiveParticipants);
            participants.addAll(prospectiveParticipants);
        }
        return new Projection(
                playerId,
                distinctCards(participants),
                distinctCards(colocated));
    }

    public static Assessment assess(
            SwccgGame game, Projection projection) {
        if (game == null || projection == null
                || projection.playerId() == null
                || projection.participants() == null
                || projection.cardsAtLocation() == null
                || game.getGameState() == null
                || game.getModifiersQuerying() == null) {
            return Assessment.none();
        }

        GameState gameState = game.getGameState();
        ModifiersQuerying modifiers = game.getModifiersQuerying();
        List<PhysicalCard> participants =
                distinctCards(projection.participants());
        List<PhysicalCard> colocated =
                distinctCards(projection.cardsAtLocation());
        List<Match> matches = new ArrayList<>();
        int total = 0;

        for (PhysicalCard source : participants) {
            if (source == null
                    || !projection.playerId().equals(source.getOwner())) {
                continue;
            }
            String sourceBlueprintId;
            try {
                sourceBlueprintId = source.getBlueprintId(true);
                if (sourceBlueprintId == null
                        || sourceUnavailable(
                                gameState, modifiers, source)) {
                    continue;
                }
            } catch (RuntimeException ignored) {
                continue;
            }

            for (Rule rule : RULES) {
                if (!rule.sourceBlueprintIds()
                        .contains(sourceBlueprintId)) {
                    continue;
                }
                if (!sourceSupportsRule(
                        game, gameState, modifiers, source, rule)) {
                    continue;
                }
                Collection<PhysicalCard> partnerPool =
                        rule.relation() == Relation.PARTICIPATING_WITH
                                ? participants : colocated;
                if (!hasDifferentPartner(
                        gameState, modifiers, source,
                        partnerPool, rule.partner())) {
                    continue;
                }
                matches.add(new Match(
                        rule.id(), source.getPermanentCardId(),
                        sourceBlueprintId, rule.addedDraws()));
                total += rule.addedDraws();
            }
        }
        return matches.isEmpty()
                ? Assessment.none()
                : new Assessment(total, matches);
    }

    public static int additionalDrawsGained(
            SwccgGame game,
            Projection before,
            Projection after) {
        return Math.max(0,
                assess(game, after).additionalBattleDestinyDraws()
                        - assess(game, before)
                            .additionalBattleDestinyDraws());
    }

    private static Rule rule(
            RuleId id,
            Set<String> sourceBlueprintIds,
            Relation relation,
            Filter partner) {
        return new Rule(id, sourceBlueprintIds, Kind.PASSIVE_ADD,
                relation, partner, 1);
    }

    private static Rule actionRule(
            RuleId id,
            Set<String> sourceBlueprintIds,
            Relation relation,
            Filter partner) {
        return new Rule(id, sourceBlueprintIds, Kind.ACTION_ADD,
                relation, partner, 1);
    }

    private static boolean sourceSupportsRule(
            SwccgGame game,
            GameState gameState,
            ModifiersQuerying modifiers,
            PhysicalCard source,
            Rule rule) {
        if (rule.kind() == Kind.ACTION_ADD) {
            return true;
        }
        try {
            if (source.getBlueprint() == null) return false;
            List<Modifier> sourceModifiers = source.getBlueprint()
                    .getWhileInPlayModifiers(game, source);
            if (sourceModifiers == null) return false;
            for (Modifier modifier : sourceModifiers) {
                if (modifier != null
                        && modifier.getModifierType()
                            == ModifierType.NUM_BATTLE_DESTINY_DRAWS
                        && Float.compare(
                            modifier.getValue(
                                gameState, modifiers,
                                (PhysicalCard) null),
                            rule.addedDraws()) == 0) {
                    return true;
                }
            }
        } catch (RuntimeException ignored) {
            // Registry drift or incomplete engine facts stay neutral.
        }
        return false;
    }

    private static boolean sourceUnavailable(
            GameState gameState,
            ModifiersQuerying modifiers,
            PhysicalCard source) {
        if (source.getZone() == null
                || !source.getZone().isInPlay()) {
            return false;
        }
        return modifiers.isGameTextCanceled(gameState, source)
                || modifiers.mayNotAddBattleDestinyDraws(
                        gameState, source);
    }

    private static boolean hasDifferentPartner(
            GameState gameState,
            ModifiersQuerying modifiers,
            PhysicalCard source,
            Collection<PhysicalCard> cards,
            Filter partnerFilter) {
        for (PhysicalCard candidate : cards) {
            if (samePhysicalCard(source, candidate)) {
                continue;
            }
            try {
                if (candidate != null && partnerFilter.accepts(
                        gameState, modifiers, candidate)) {
                    return true;
                }
            } catch (RuntimeException ignored) {
                // Unsupported or incomplete filter facts stay neutral.
            }
        }
        return false;
    }

    private static boolean samePhysicalCard(
            PhysicalCard first, PhysicalCard second) {
        if (first == null || second == null) return false;
        if (first == second) return true;
        int firstId = first.getPermanentCardId();
        int secondId = second.getPermanentCardId();
        return firstId > 0 && firstId == secondId;
    }

    private static List<PhysicalCard> distinctCards(
            Collection<PhysicalCard> cards) {
        if (cards == null || cards.isEmpty()) return List.of();
        Set<PhysicalCard> identities = Collections.newSetFromMap(
                new IdentityHashMap<>());
        Set<Integer> permanentIds = new HashSet<>();
        List<PhysicalCard> distinct = new ArrayList<>();
        for (PhysicalCard card : cards) {
            if (card == null || !identities.add(card)) continue;
            int permanentId = card.getPermanentCardId();
            if (permanentId > 0 && !permanentIds.add(permanentId)) {
                continue;
            }
            distinct.add(card);
        }
        return distinct;
    }
}
