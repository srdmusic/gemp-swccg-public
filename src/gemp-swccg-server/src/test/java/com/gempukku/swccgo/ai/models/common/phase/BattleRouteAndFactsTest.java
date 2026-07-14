package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.decision.FactValue;
import com.gempukku.swccgo.common.BattleDecisionWire;
import com.gempukku.swccgo.common.DecisionActionSemantic;
import com.gempukku.swccgo.common.DecisionOrigin;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Closed BATTLE route matrix and immutable candidate identity parsing. */
public class BattleRouteAndFactsTest {

    @Test
    public void exactEngineStampsOwnFourCurrentBattleShapes() {
        assertRoute(BattleWindowRoute.INITIATE,
                AwaitingDecisionType.CARD_ACTION_CHOICE,
                BattleTestFixtures.initiation());
        assertRoute(BattleWindowRoute.FIRE,
                AwaitingDecisionType.CARD_ACTION_CHOICE,
                BattleTestFixtures.fire());
        assertRoute(BattleWindowRoute.TACTIC,
                AwaitingDecisionType.CARD_ACTION_CHOICE,
                BattleTestFixtures.tactic());
        assertRoute(BattleWindowRoute.TACTIC,
                AwaitingDecisionType.CARD_SELECTION,
                BattleTestFixtures.forfeit(false));
        assertRoute(BattleWindowRoute.ADD_DESTINY,
                AwaitingDecisionType.MULTIPLE_CHOICE,
                BattleTestFixtures.power());
        assertRoute(BattleWindowRoute.TACTIC,
                AwaitingDecisionType.ARBITRARY_CARDS,
                BattleTestFixtures.destinySelection());
    }

    @Test
    public void allSevenWireShapesAreCoveredWithoutPhaseOrPromptOvercapture() {
        assertRoute(BattleWindowRoute.INITIATE,
                AwaitingDecisionType.CARD_ACTION_CHOICE,
                BattleTestFixtures.initiation());
        assertRoute(BattleWindowRoute.LEGACY_UNOWNED,
                AwaitingDecisionType.ACTION_CHOICE,
                BattleTestFixtures.actionChoice());
        assertRoute(BattleWindowRoute.TACTIC,
                AwaitingDecisionType.CARD_SELECTION,
                BattleTestFixtures.forfeit(false));
        assertRoute(BattleWindowRoute.TACTIC,
                AwaitingDecisionType.ARBITRARY_CARDS,
                BattleTestFixtures.destinySelection());
        assertRoute(BattleWindowRoute.LEGACY_UNOWNED,
                AwaitingDecisionType.INTEGER,
                BattleTestFixtures.integer());
        assertRoute(BattleWindowRoute.ADD_DESTINY,
                AwaitingDecisionType.MULTIPLE_CHOICE,
                BattleTestFixtures.power());
        assertRoute(BattleWindowRoute.LEGACY_UNOWNED,
                AwaitingDecisionType.EMPTY,
                BattleTestFixtures.empty());
    }

    @Test
    public void initiationFactsRetainRawOrderAndPhysicalTargetIdentity() {
        Map<String, String[]> params = BattleTestFixtures.initiation();
        BattleFacts facts = BattleTestFixtures.facts(
                AwaitingDecisionType.CARD_ACTION_CHOICE,
                params, BattleWindowRoute.INITIATE);

        assertEquals(List.of("0", "1", "2"),
                facts.candidates().stream().map(
                        BattleFacts.Candidate::wireId).toList());
        assertEquals(List.of(0, 1, 2),
                facts.candidates().stream().map(
                        BattleFacts.Candidate::ordinal).toList());
        assertEquals(BattleCandidateRole.INITIATE,
                facts.candidates().get(1).role());
        assertEquals(DecisionActionSemantic.BATTLE_INITIATE,
                facts.candidates().get(1).semantic());
        assertEquals(301,
                BattleAssessment.from(facts).initiationAt(1).targetCardId());
    }

    @Test
    public void deployIntentBindsOnlyToItsPhysicalInitiationTarget() {
        Map<String, String[]> params = BattleTestFixtures.initiation();
        BattleTestFixtures.put(params, DecisionActionSemantic.WIRE_PARAMETER,
                DecisionActionSemantic.UNKNOWN.name(),
                DecisionActionSemantic.BATTLE_INITIATE.name(),
                DecisionActionSemantic.BATTLE_INITIATE.name());
        BattleFacts facts = BattleTestFixtures.facts(
                AwaitingDecisionType.CARD_ACTION_CHOICE,
                params, BattleWindowRoute.INITIATE);
        BattleDeployIntent intent = new BattleDeployIntent(
                BattleDeployIntent.Kind.OVERPOWER, 301, 7, "attempt-1");

        BattleAssessment assessment = BattleAssessment.from(facts, intent);

        assertNull(assessment.initiationAt(0));
        assertEquals(intent, assessment.initiationAt(1).deployIntent());
        assertEquals(BattleDeployIntent.none(),
                assessment.initiationAt(2).deployIntent());
    }

    @Test
    public void optionalImmuneForfeitRequiresExactEngineBoolean() {
        BattleFacts optional = BattleTestFixtures.facts(
                AwaitingDecisionType.CARD_SELECTION,
                BattleTestFixtures.forfeit(true), BattleWindowRoute.TACTIC);
        BattleFacts required = BattleTestFixtures.facts(
                AwaitingDecisionType.CARD_SELECTION,
                BattleTestFixtures.forfeit(false), BattleWindowRoute.TACTIC);

        assertTrue(optional.optionalImmuneForfeit());
        assertTrue(BattleAssessment.from(optional).optionalImmuneForfeit());
        assertFalse(required.optionalImmuneForfeit());

        Map<String, String[]> missing = BattleTestFixtures.forfeit(true);
        missing.remove(BattleDecisionWire.OPTIONAL_IMMUNE_FORFEIT);
        assertRoute(BattleWindowRoute.LEGACY_UNOWNED,
                AwaitingDecisionType.CARD_SELECTION, missing);

        Map<String, String[]> malformed = BattleTestFixtures.forfeit(true);
        BattleTestFixtures.put(malformed,
                BattleDecisionWire.OPTIONAL_IMMUNE_FORFEIT, "sometimes");
        assertRoute(BattleWindowRoute.LEGACY_UNOWNED,
                AwaitingDecisionType.CARD_SELECTION, malformed);
    }

    @Test
    public void malformedOrMisalignedMetadataFallsThroughLegacy() {
        Map<String, String[]> missingOrigin = BattleTestFixtures.initiation();
        missingOrigin.remove(DecisionOrigin.WIRE_PARAMETER);
        assertRoute(BattleWindowRoute.LEGACY_UNOWNED,
                AwaitingDecisionType.CARD_ACTION_CHOICE, missingOrigin);

        Map<String, String[]> duplicateAction = BattleTestFixtures.initiation();
        BattleTestFixtures.put(duplicateAction, "actionId", "0", "0", "2");
        assertRoute(BattleWindowRoute.LEGACY_UNOWNED,
                AwaitingDecisionType.CARD_ACTION_CHOICE, duplicateAction);

        Map<String, String[]> misaligned = BattleTestFixtures.initiation();
        BattleTestFixtures.put(misaligned,
                DecisionActionSemantic.WIRE_PARAMETER,
                DecisionActionSemantic.BATTLE_INITIATE.name());
        assertRoute(BattleWindowRoute.LEGACY_UNOWNED,
                AwaitingDecisionType.CARD_ACTION_CHOICE, misaligned);

        Map<String, String[]> unknownSemantic = BattleTestFixtures.initiation();
        BattleTestFixtures.put(unknownSemantic,
                DecisionActionSemantic.WIRE_PARAMETER,
                DecisionActionSemantic.UNKNOWN.name(), "NOT_A_SEMANTIC",
                DecisionActionSemantic.UNKNOWN.name());
        assertRoute(BattleWindowRoute.LEGACY_UNOWNED,
                AwaitingDecisionType.CARD_ACTION_CHOICE, unknownSemantic);

        Map<String, String[]> duplicateCard = BattleTestFixtures.forfeit(false);
        BattleTestFixtures.put(duplicateCard, "cardId", "501", "501");
        assertRoute(BattleWindowRoute.LEGACY_UNOWNED,
                AwaitingDecisionType.CARD_SELECTION, duplicateCard);
    }

    @Test
    public void snapshotAndRouteInputMustDescribeTheSameRawDecision() {
        Map<String, String[]> raw = BattleTestFixtures.initiation();
        BattleRouteInput captured = BattleTestFixtures.input(
                AwaitingDecisionType.CARD_ACTION_CHOICE, raw);
        Map<String, String[]> changed = BattleTestFixtures.initiation();
        BattleTestFixtures.put(changed, "cardId", "100", "999", "302");

        FactValue<BattleFacts> facts = BattleFacts.parse(
                BattleTestFixtures.snapshot(
                        AwaitingDecisionType.CARD_ACTION_CHOICE, changed),
                captured, BattleWindowRoute.INITIATE);
        assertTrue(facts.isUnknown());
    }

    private static void assertRoute(BattleWindowRoute expected,
                                    AwaitingDecisionType type,
                                    Map<String, String[]> params) {
        assertEquals(expected, BattleRouteResolver.resolve(
                BattleTestFixtures.input(type, params)));
    }
}
