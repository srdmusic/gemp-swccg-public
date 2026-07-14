package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.decision.DecisionSnapshot;
import com.gempukku.swccgo.ai.models.common.decision.FactValue;
import com.gempukku.swccgo.common.DecisionActionSemantic;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Immutable typed facts for one BATTLE-owned decision. */
public record BattleFacts(
        String decisionId,
        Phase phase,
        AwaitingDecisionType decisionType,
        BattleWindowRoute route,
        List<Candidate> candidates,
        boolean optionalImmuneForfeit) {

    private static final String PRODUCER = "battle-facts";

    public BattleFacts {
        if (decisionId == null || decisionId.isBlank()) {
            throw new IllegalArgumentException("decisionId must be nonblank");
        }
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(decisionType, "decisionType");
        Objects.requireNonNull(route, "route");
        if (route == BattleWindowRoute.LEGACY_UNOWNED) {
            throw new IllegalArgumentException("LEGACY_UNOWNED has no BattleFacts");
        }
        candidates = List.copyOf(candidates);
        for (int i = 0; i < candidates.size(); i++) {
            if (candidates.get(i).ordinal() != i) {
                throw new IllegalArgumentException(
                        "BATTLE candidates must retain original ordinal order");
            }
        }
        if (optionalImmuneForfeit
                && (decisionType != AwaitingDecisionType.CARD_SELECTION
                    || route != BattleWindowRoute.TACTIC)) {
            throw new IllegalArgumentException(
                    "optional immune forfeit requires the typed CARD_SELECTION tactic route");
        }
    }

    public record Candidate(
            int ordinal,
            String wireId,
            String cardId,
            DecisionActionSemantic semantic,
            BattleCandidateRole role) {

        public Candidate {
            if (ordinal < 0) {
                throw new IllegalArgumentException("ordinal must be >= 0");
            }
            if (wireId == null || wireId.isBlank()) {
                throw new IllegalArgumentException("wireId must be nonblank");
            }
            Objects.requireNonNull(semantic, "semantic");
            Objects.requireNonNull(role, "role");
        }
    }

    public Candidate candidateByWireId(String wireId) {
        for (Candidate candidate : candidates) {
            if (candidate.wireId().equals(wireId)) {
                return candidate;
            }
        }
        return null;
    }

    public static FactValue<BattleFacts> parse(DecisionSnapshot snapshot,
                                                BattleRouteInput input,
                                                BattleWindowRoute route) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(route, "route");
        try {
            if (route == BattleWindowRoute.LEGACY_UNOWNED
                    || BattleRouteResolver.resolve(input) != route) {
                return unknown("route is unowned or typed BATTLE metadata is incomplete");
            }
            if (!snapshot.decisionFacts().decisionId().equals(
                        String.valueOf(input.decisionId()))
                    || snapshot.decisionFacts().decisionType() != input.decisionType()
                    || !Objects.equals(snapshot.decisionFacts().phase(), input.phase())
                    || !rawMatches(snapshot.rawDecision().parameters(), input)) {
                return unknown("snapshot and BATTLE input do not preserve one raw decision");
            }

            List<Candidate> candidates = candidates(input, route);
            boolean optional = Boolean.TRUE.equals(
                    BattleRouteResolver.singletonBoolean(
                            input.optionalImmuneForfeitValues()));
            return FactValue.known(new BattleFacts(
                    snapshot.decisionFacts().decisionId(),
                    input.phase(), input.decisionType(), route,
                    candidates, optional), PRODUCER,
                    "BattleRouteInput + DecisionSnapshot");
        } catch (RuntimeException e) {
            String detail = e.getMessage() != null
                    ? e.getMessage() : e.getClass().getSimpleName();
            return unknown("BATTLE fact parsing failed: " + detail);
        }
    }

    private static List<Candidate> candidates(BattleRouteInput input,
                                               BattleWindowRoute route) {
        ArrayList<Candidate> result = new ArrayList<>();
        if (input.decisionType() == AwaitingDecisionType.CARD_ACTION_CHOICE) {
            for (int i = 0; i < input.actionIds().size(); i++) {
                DecisionActionSemantic semantic = DecisionActionSemantic.fromWire(
                        input.actionSemantics().get(i));
                result.add(new Candidate(i, input.actionIds().get(i),
                        input.cardIds().get(i), semantic,
                        candidateRole(route, semantic)));
            }
        } else if (input.decisionType() == AwaitingDecisionType.CARD_SELECTION
                || input.decisionType() == AwaitingDecisionType.ARBITRARY_CARDS) {
            for (int i = 0; i < input.cardIds().size(); i++) {
                result.add(new Candidate(i, input.cardIds().get(i),
                        input.cardIds().get(i), DecisionActionSemantic.UNKNOWN,
                        BattleCandidateRole.TACTIC));
            }
        } else if (input.decisionType() == AwaitingDecisionType.MULTIPLE_CHOICE) {
            for (int i = 0; i < input.results().size(); i++) {
                result.add(new Candidate(i, String.valueOf(i), null,
                        DecisionActionSemantic.UNKNOWN,
                        route == BattleWindowRoute.ADD_DESTINY
                                ? BattleCandidateRole.ADD_DESTINY
                                : BattleCandidateRole.TACTIC));
            }
        }
        return List.copyOf(result);
    }

    private static BattleCandidateRole candidateRole(
            BattleWindowRoute route, DecisionActionSemantic semantic) {
        return switch (semantic) {
            case BATTLE_INITIATE -> BattleCandidateRole.INITIATE;
            case BATTLE_FIRE -> BattleCandidateRole.FIRE;
            case PULL_DEPLOY_FROM_PILE, PULL_TAKE_INTO_HAND_FROM_PILE ->
                    BattleCandidateRole.DELEGATED_PULL;
            default -> route == BattleWindowRoute.TACTIC
                    || route == BattleWindowRoute.FIRE
                        ? BattleCandidateRole.TACTIC
                        : BattleCandidateRole.GENERIC;
        };
    }

    private static boolean rawMatches(Map<String, List<String>> raw,
                                      BattleRouteInput input) {
        return Objects.equals(raw.get("decisionOrigin"), input.originValues())
                && Objects.equals(raw.get("actionId"), input.actionIds())
                && Objects.equals(raw.get("actionSemantic"), input.actionSemantics())
                && Objects.equals(raw.get("cardId"), input.cardIds())
                && Objects.equals(raw.get("results"), input.results())
                && Objects.equals(raw.get("battleOptionalImmuneForfeit"),
                        input.optionalImmuneForfeitValues());
    }

    private static FactValue<BattleFacts> unknown(String reason) {
        return FactValue.unknown(PRODUCER, "typed BATTLE decision", reason);
    }
}
