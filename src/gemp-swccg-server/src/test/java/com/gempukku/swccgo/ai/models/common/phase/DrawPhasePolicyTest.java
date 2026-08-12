package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DrawPhasePolicyTest {
    private static final Logger LOGGER = LogManager.getLogger(DrawPhasePolicyTest.class);

    @Test
    public void blockedNullBoardPreservesOperationOrderAndSoftScore() {
        Facts facts = new Facts();
        facts.hasBoard = false;

        List<PolicyOperation> operations = assess(facts, true).operations();

        assertIds(operations, "V167-draw-soft", "DRAW-no-board");
        assertBits(-200.0f, operations.get(0).delta());
        assertBits(0.0f, operations.get(1).delta());
        assertEquals(-200.0f, total(operations), 0.0f);
    }

    @Test
    public void maintenanceFloorPreemptsEmergencyDrawAndEverythingAfterIt() {
        Facts facts = new Facts();
        facts.hand = 2;
        facts.force = 1;
        facts.maintenance = 1;

        List<PolicyOperation> operations = assess(facts, false).operations();

        assertIds(operations, "V58-maintenance-floor");
        assertBits(-150.0f, operations.get(0).delta());
        assertFalse(facts.offensiveBankRead);
        assertFalse(facts.reserveRead);
    }

    @Test
    public void v42PrecedesCriticalLifeReturnAtHandTwo() {
        Facts facts = new Facts();
        facts.hand = 2;
        facts.reserve = 2;
        facts.used = 0;
        facts.force = 1;

        List<PolicyOperation> operations = assess(facts, false).operations();

        assertIds(operations, "V42-draw", "DRAW-critical-life");
        assertBits(200.0f, operations.get(0).delta());
        assertBits(-120.0f, operations.get(1).delta());
        assertFalse(facts.offensiveBankRead);
    }

    @Test
    public void v182RemainsAnAdditiveMinusThreeHundredEarlyReturn() {
        Facts facts = new Facts();
        facts.hand = 4;
        facts.force = 1;
        facts.generation = 4;
        facts.bank = 5;

        List<PolicyOperation> operations = assess(facts, false).operations();

        assertIds(operations, "V182");
        assertBits(-300.0f, operations.get(0).delta());
        assertEquals("ADD", operations.get(0).kind().name());
        assertFalse(facts.reserveRead);
    }

    @Test
    public void fundedResponseStopsStockDrawAtRepairedHand() {
        Facts facts = responseFacts(6, 12, 5);

        List<PolicyOperation> operations = assess(facts, false).operations();

        assertIds(operations, "V182-response-bank");
        assertBits(-300.0f, operations.get(0).delta());
        assertEquals("ADD", operations.get(0).kind().name());
        assertFalse(facts.reserveRead);
    }

    @Test
    public void sequentialHandAndForceBoundaryPreservesRepairAndSurplus() {
        Facts handTwo = responseFacts(2, 5, 5);
        assertFalse(hasId(assess(handTwo, false),
                "V182-response-bank"));
        assertTrue(hasId(assess(handTwo, false), "V42-draw"));

        for (int hand : new int[]{3, 5}) {
            assertTrue(hasId(assess(responseFacts(hand, 5, 5), false),
                    "V182-response-bank"));
            assertFalse(hasId(assess(responseFacts(hand, 6, 5), false),
                    "V182-response-bank"));
        }
        assertTrue(hasId(assess(responseFacts(6, 12, 5), false),
                "V182-response-bank"));
    }

    @Test
    public void missingCurrentResponseProofIsInert() {
        Facts candidate = responseFacts(6, 12, 5);
        candidate.responseBank = null;
        Facts control = responseFacts(6, 12, 5);
        control.behindOnBoard = false;

        List<PolicyOperation> candidateOperations =
                assess(candidate, false).operations();
        List<PolicyOperation> controlOperations =
                assess(control, false).operations();

        assertEquals(controlOperations.stream()
                        .map(operation -> operation.ruleArmId().id()).toList(),
                candidateOperations.stream()
                        .map(operation -> operation.ruleArmId().id()).toList());
        assertEquals(controlOperations.stream()
                        .map(operation -> Float.floatToRawIntBits(
                                operation.delta())).toList(),
                candidateOperations.stream()
                        .map(operation -> Float.floatToRawIntBits(
                                operation.delta())).toList());
    }

    @Test
    public void piettDigExplicitlyBypassesResponseBank() {
        Facts facts = responseFacts(6, 5, 5);
        facts.piett = true;

        PolicyResult result = assess(facts, false);

        assertFalse(hasId(result, "V182-response-bank"));
        assertBits(150.0f, byId(result, "V24.10-dig").delta());
    }

    @Test
    public void maintenanceLifeAndHandLimitKeepEarlierOwnership() {
        Facts maintenance = responseFacts(6, 5, 5);
        maintenance.maintenance = 5;
        assertIds(assess(maintenance, false).operations(),
                "V58-maintenance-floor");

        Facts criticalLife = responseFacts(6, 3, 3);
        criticalLife.reserve = 2;
        assertIds(assess(criticalLife, false).operations(),
                "DRAW-critical-life");

        Facts handLimit = responseFacts(16, 5, 5);
        assertIds(assess(handLimit, false).operations(),
                "DRAW-hand-limit");
    }

    @Test
    public void forceStarvedShortfallReturnsAfterBothLegacyPenalties() {
        Facts facts = new Facts();
        facts.hand = 6;
        facts.force = 1;
        facts.generation = 1;
        facts.expensive = new DrawPhaseFactsReader.ExpensiveCards(6, 0, 1, false);
        facts.starved = new DrawPhaseFactsReader.ForceStarved(6, 10);

        List<PolicyOperation> operations = assess(facts, false).operations();

        assertIds(operations, "DRAW-force-starved-shortfall", "DRAW-force-starved-hand");
        assertBits(-150.0f * 0.6f, operations.get(0).delta());
        assertBits(-20.0f, operations.get(1).delta());
        assertFalse(facts.reserveRead);
    }

    @Test
    public void holdBackExpensivePiettAndBaselineStayInLegacyOrder() {
        Facts facts = new Facts();
        facts.hand = 4;
        facts.force = 7;
        facts.generation = 8;
        facts.holdBack = new DrawPhasePolicy.HoldBack(true, "could not deploy available cards");
        facts.expensive = new DrawPhaseFactsReader.ExpensiveCards(4, 8, 0, true);
        facts.piett = true;
        facts.forceReserve = 7;

        List<PolicyOperation> operations = assess(facts, false).operations();

        assertIds(operations, "DRAW-hold-back", "DRAW-expensive-save", "V24.10-dig",
                "DRAW-baseline", "DRAW-target-hand", "DRAW-small-hand", "DRAW-weak-hand");
        assertBits(55.0f, operations.get(0).delta());
        assertBits(-20.0f, operations.get(1).delta());
        assertBits(150.0f, operations.get(2).delta());
        assertBits(96.0f, operations.get(3).delta());
    }

    @Test
    public void piettTurnBandsPreserveRawFloats() {
        int[] turns = {2, 3, 5};
        float[] expected = {200.0f, 150.0f, 80.0f};
        for (int i = 0; i < turns.length; i++) {
            Facts facts = new Facts();
            facts.turn = turns[i];
            facts.piett = true;
            PolicyOperation operation = byId(assess(facts, false), "V24.10-dig");
            assertBits(expected[i], operation.delta());
        }
    }

    @Test
    public void drawDownCapsAtFourHundredBeforeTailRules() {
        Facts facts = new Facts();
        facts.hand = 7;
        facts.turn = 5;
        facts.force = 12;
        facts.forceReserve = 1;

        List<PolicyOperation> operations = assess(facts, false).operations();

        assertIds(operations, "DRAW-baseline", "V58-draw-down", "DRAW-high-force");
        assertBits(400.0f, operations.get(1).delta());
    }

    @Test
    public void reserveBoundaryPreservesNegativeZeroLowReserveOperation() {
        Facts facts = new Facts();
        facts.hand = 7;
        facts.turn = 5;
        facts.reserve = 6;
        facts.used = 20;
        facts.force = 1;
        facts.forceReserve = 1;

        List<PolicyOperation> operations = assess(facts, false).operations();

        assertIds(operations, "DRAW-baseline", "V58-hold-reserve",
                "DRAW-low-reserve", "DRAW-last-force");
        assertEquals(Float.floatToRawIntBits(-0.0f),
                Float.floatToRawIntBits(operations.get(2).delta()));
    }

    @Test
    public void onePolicyNeverContributesTheSameRuleTwice() {
        Facts facts = new Facts();
        facts.hand = 4;
        facts.force = 12;
        facts.turn = 5;
        facts.holdBack = new DrawPhasePolicy.HoldBack(true, "could not deploy");
        facts.piett = true;
        facts.forceReserve = 1;

        List<PolicyOperation> operations = assess(facts, true).operations();
        Set<String> ids = new HashSet<>();
        for (PolicyOperation operation : operations) {
            assertTrue("duplicate rule " + operation.ruleArmId().id(),
                    ids.add(operation.ruleArmId().id()));
        }
    }

    private static PolicyResult assess(Facts facts, boolean blocked) {
        return DrawPhasePolicy.assess("draw", "Draw card into hand from Force Pile",
                blocked, facts, LOGGER);
    }

    private static PolicyOperation byId(PolicyResult result, String id) {
        return result.operations().stream()
                .filter(operation -> operation.ruleArmId().id().equals(id))
                .findFirst().orElseThrow();
    }

    private static boolean hasId(PolicyResult result, String id) {
        return result.operations().stream().anyMatch(
                operation -> operation.ruleArmId().id().equals(id));
    }

    private static Facts responseFacts(int hand, int force, int cost) {
        Facts facts = new Facts();
        facts.hand = hand;
        facts.force = force;
        facts.behindOnBoard = true;
        facts.responseBank = new PersistentResponsePolicy
                .ResponseBankDetails(3, 1, cost,
                DeployTacticalPolicy.ResponseFormationRoute.V171_WAVE,
                "ground_response");
        return facts;
    }

    private static void assertIds(List<PolicyOperation> operations, String... ids) {
        assertEquals(List.of(ids), operations.stream()
                .map(operation -> operation.ruleArmId().id()).toList());
    }

    private static void assertBits(float expected, float actual) {
        assertEquals(Float.floatToRawIntBits(expected), Float.floatToRawIntBits(actual));
    }

    private static float total(List<PolicyOperation> operations) {
        float total = 0.0f;
        for (PolicyOperation operation : operations) {
            total += operation.delta();
        }
        return total;
    }

    private static final class Facts implements DrawPhasePolicy.Facts {
        private boolean hasBoard = true;
        private int hand = 7;
        private int reserve = 20;
        private int used;
        private int force;
        private int turn = 3;
        private int maintenance;
        private int generation = 8;
        private int bank;
        private DrawPhasePolicy.HoldBack holdBack = DrawPhasePolicy.HoldBack.none();
        private DrawPhaseFactsReader.ExpensiveCards expensive =
                new DrawPhaseFactsReader.ExpensiveCards(7, 0, 1, false);
        private DrawPhaseFactsReader.ForceStarved starved =
                new DrawPhaseFactsReader.ForceStarved(0, 999);
        private boolean piett;
        private int forceReserve;
        private boolean offensiveBankRead;
        private boolean reserveRead;
        private boolean ordinaryStockDraw = true;
        private boolean behindOnBoard;
        private PersistentResponsePolicy.ResponseBankDetails responseBank;

        @Override public boolean hasBoardState() { return hasBoard; }
        @Override public int handSize() { return hand; }
        @Override public int reserveDeckSize() { return reserve; }
        @Override public int usedPileSize() { return used; }
        @Override public int forcePileSize() { return force; }
        @Override public int turnNumber() { return turn; }
        @Override public int maxHandSize() { return 16; }
        @Override public int handSoftCap() { return 12; }
        @Override public int maintenanceObligation() { return maintenance; }
        @Override public int forceGeneration() { return generation; }

        @Override public int offensiveBank(int forcePile, int forceGeneration) {
            offensiveBankRead = true;
            return bank;
        }

        @Override public boolean ordinaryStockForcePileDraw() {
            return ordinaryStockDraw;
        }

        @Override public boolean behindOnBoard() {
            return behindOnBoard;
        }

        @Override public PersistentResponsePolicy.ResponseBankDetails
                currentResponseBank() {
            return responseBank;
        }

        @Override public DrawPhasePolicy.HoldBack holdBack() { return holdBack; }

        @Override public DrawPhaseFactsReader.ExpensiveCards expensiveCards(int forcePile) {
            return expensive;
        }

        @Override public DrawPhaseFactsReader.ForceStarved forceStarved() { return starved; }
        @Override public boolean piettNeedsDig() { return piett; }

        @Override public int forceToReserve() {
            reserveRead = true;
            return forceReserve;
        }
    }
}
