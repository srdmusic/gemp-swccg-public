package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.AiDecisionResult;
import com.gempukku.swccgo.ai.models.common.decision.DecisionSnapshot;
import com.gempukku.swccgo.ai.models.common.finalization.FinalizedResponse;
import com.gempukku.swccgo.ai.models.common.finalization.RejectionHistory;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Exact compatibility wire through one typed BATTLE finalizer. */
public class BattlePhaseOwnerTest {

    @Test
    public void optionalImmuneForfeitBypassesCompatibilityAndPasses() {
        Owned owned = parsed(AwaitingDecisionType.CARD_SELECTION,
                BattleTestFixtures.forfeit(true), BattleWindowRoute.TACTIC);
        AtomicInteger calls = new AtomicInteger();

        AiDecisionResult result = BattlePhaseOwner.decide(
                owned.snapshot(), RejectionHistory.empty(), owned.facts(),
                owned.assessment(), (facts, assessment) -> {
                    calls.incrementAndGet();
                    return "501";
                });

        assertAccepted(result, "");
        assertEquals(0, calls.get());
    }

    @Test
    public void requiredForfeitInvokesCompatibilityExactlyOnce() {
        Owned owned = parsed(AwaitingDecisionType.CARD_SELECTION,
                BattleTestFixtures.forfeit(false), BattleWindowRoute.TACTIC);
        AtomicInteger calls = new AtomicInteger();

        AiDecisionResult result = BattlePhaseOwner.decide(
                owned.snapshot(), RejectionHistory.empty(), owned.facts(),
                owned.assessment(), (facts, assessment) -> {
                    calls.incrementAndGet();
                    return "502";
                });

        assertAccepted(result, "502");
        assertEquals(1, calls.get());
    }

    @Test
    public void ownedCurrentRoutesPreserveExactWire() {
        assertAccepted(decide(parsed(AwaitingDecisionType.CARD_ACTION_CHOICE,
                BattleTestFixtures.initiation(), BattleWindowRoute.INITIATE), "1"), "1");
        assertAccepted(decide(parsed(AwaitingDecisionType.CARD_ACTION_CHOICE,
                BattleTestFixtures.fire(), BattleWindowRoute.FIRE), "0"), "0");
        assertAccepted(decide(parsed(AwaitingDecisionType.MULTIPLE_CHOICE,
                BattleTestFixtures.power(), BattleWindowRoute.ADD_DESTINY), "0"), "0");
        assertAccepted(decide(parsed(AwaitingDecisionType.ARBITRARY_CARDS,
                BattleTestFixtures.destinySelection(), BattleWindowRoute.TACTIC),
                "temp1,temp2"), "temp1,temp2");
    }

    @Test
    public void finalizerSupportsAllSevenBattleWindowWireContracts() {
        assertAccepted(decide(manual(AwaitingDecisionType.CARD_ACTION_CHOICE,
                BattleTestFixtures.initiation(), BattleWindowRoute.INITIATE), "1"), "1");
        assertAccepted(decide(manual(AwaitingDecisionType.ACTION_CHOICE,
                BattleTestFixtures.actionChoice(), BattleWindowRoute.TACTIC), "1"), "1");
        assertAccepted(decide(manual(AwaitingDecisionType.CARD_SELECTION,
                BattleTestFixtures.forfeit(false), BattleWindowRoute.TACTIC), "501"), "501");
        assertAccepted(decide(manual(AwaitingDecisionType.ARBITRARY_CARDS,
                BattleTestFixtures.destinySelection(), BattleWindowRoute.TACTIC),
                "temp0,temp2"), "temp0,temp2");
        assertAccepted(decide(manual(AwaitingDecisionType.INTEGER,
                BattleTestFixtures.integer(), BattleWindowRoute.TACTIC), "3"), "3");
        assertAccepted(decide(manual(AwaitingDecisionType.MULTIPLE_CHOICE,
                BattleTestFixtures.power(), BattleWindowRoute.ADD_DESTINY), "1"), "1");
        assertAccepted(decide(manual(AwaitingDecisionType.EMPTY,
                BattleTestFixtures.empty(), BattleWindowRoute.TACTIC), ""), "");
    }

    @Test
    public void unknownDuplicateAndMismatchedWiresRejectClosed() {
        assertRejected(decide(parsed(AwaitingDecisionType.CARD_SELECTION,
                BattleTestFixtures.forfeit(false), BattleWindowRoute.TACTIC), "999"));
        assertRejected(decide(parsed(AwaitingDecisionType.ARBITRARY_CARDS,
                BattleTestFixtures.destinySelection(), BattleWindowRoute.TACTIC),
                "temp1,temp1"));

        Owned initiation = parsed(AwaitingDecisionType.CARD_ACTION_CHOICE,
                BattleTestFixtures.initiation(), BattleWindowRoute.INITIATE);
        BattleAssessment mismatched = new BattleAssessment(
                BattleWindowRoute.TACTIC, List.of(), false);
        assertRejected(BattlePhaseOwner.decide(
                initiation.snapshot(), RejectionHistory.empty(), initiation.facts(),
                mismatched, (facts, assessment) -> "1"));
    }

    private static AiDecisionResult decide(Owned owned, String wire) {
        return BattlePhaseOwner.decide(
                owned.snapshot(), RejectionHistory.empty(), owned.facts(),
                owned.assessment(), (facts, assessment) -> wire);
    }

    private static Owned parsed(AwaitingDecisionType type,
                                Map<String, String[]> params,
                                BattleWindowRoute route) {
        DecisionSnapshot snapshot = BattleTestFixtures.snapshot(type, params);
        BattleFacts facts = BattleFacts.parse(snapshot,
                BattleTestFixtures.input(type, params), route).value();
        return new Owned(snapshot, facts, BattleAssessment.from(facts));
    }

    private static Owned manual(AwaitingDecisionType type,
                                Map<String, String[]> params,
                                BattleWindowRoute route) {
        DecisionSnapshot snapshot = BattleTestFixtures.snapshot(type, params);
        BattleFacts facts = new BattleFacts(
                String.valueOf(BattleTestFixtures.DECISION_ID),
                com.gempukku.swccgo.common.Phase.BATTLE,
                type, route, List.of(), false);
        return new Owned(snapshot, facts, BattleAssessment.from(facts));
    }

    private static void assertAccepted(AiDecisionResult result, String wire) {
        assertEquals(AiDecisionResult.Status.WIRE_RESPONSE, result.status());
        assertEquals(wire, result.wireResponse());
        assertEquals(AiDecisionResult.MutationMode.OUTER_COMMON,
                result.mutationMode());
        assertTrue(result.fromTypedFinalizer());
        assertNotNull(result.trackerMutation());
    }

    private static void assertRejected(AiDecisionResult result) {
        assertEquals(AiDecisionResult.Status.TYPED_REJECTION, result.status());
        assertEquals(FinalizedResponse.RejectReason.CONTRACT_FACT_UNKNOWN,
                result.rejectionCode());
        assertNull(result.wireResponse());
        assertNull(result.trackerMutation());
    }

    private record Owned(DecisionSnapshot snapshot,
                         BattleFacts facts,
                         BattleAssessment assessment) {
    }
}
