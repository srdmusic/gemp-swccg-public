package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperationKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ControlDrainAssessmentTest {

    @Test
    public void classicHuntExecutorDrainStopsBeforeAllGenericScoring() {
        Facts facts = new Facts();
        facts.classicHuntExecutorHardLoss = true;
        List<PolicyOperation> operations =
                ControlDrainAssessment.assess("A", facts).operations();
        assertEquals(List.of(), facts.queries);
        assertEquals(1, operations.size());
        PolicyOperation operation = operations.get(0);
        assertEquals(
                "OBJECTIVE.HARD_LOSS.CLASSIC_HUNT_EXECUTOR_DRAIN",
                operation.ruleArmId().id());
        assertEquals(TraceDomainId.OBJECTIVE_INTENT,
                operation.domainId());
        assertEquals(TraceOutputKind.VETO, operation.outputKind());
        assertEquals(PolicyOperationKind.HARD_VETO, operation.kind());
        assertEquals(Float.floatToRawIntBits(0.0f),
                Float.floatToRawIntBits(operation.delta()));
    }

    @Test
    public void nonpositiveDrainStopsAfterPrimary() {
        Facts facts = new Facts();
        facts.primary = primary(0, 0, 10, 0);
        assertPlan(facts, List.of("primary"),
                op("V24.15-drain", -9999, TraceOutputKind.VETO));
    }

    @Test
    public void largeNetLossStopsAtV189() {
        Facts facts = new Facts();
        facts.primary = primary(1, 3, 10, 0);
        assertPlan(facts, List.of("primary"),
                op("V189", -2000, TraceOutputKind.VETO));
    }

    @Test
    public void netMinusOneStopsWhenTurnPlanIsUnfunded() {
        Facts facts = new Facts();
        facts.primary = primary(2, 3, 5, 1);
        assertPlan(facts, List.of("primary"),
                op("V189", -2000, TraceOutputKind.VETO));
    }

    @Test
    public void simpleTricksStopsBeforeEconomyQueries() {
        Facts facts = new Facts();
        facts.simpleTricks = true;
        assertPlan(facts, List.of("primary", "simpleTricks"),
                op("V25-SimpleTricks", -9999, TraceOutputKind.VETO));
    }

    @Test
    public void battleOrderAffordabilityAndNoBodyAreTerminal() {
        Facts cannotAfford = new Facts();
        cannotAfford.economy = economy(true, 2, true, 1, 3);
        assertPlan(cannotAfford, List.of("primary", "simpleTricks", "economy"),
                op("CONTROL-battle-order-afford", -50, TraceOutputKind.BANDED));

        Facts noBody = new Facts();
        noBody.economy = economy(true, 8, false, Integer.MAX_VALUE, 3);
        assertPlan(noBody, List.of("primary", "simpleTricks", "economy"),
                op("CONTROL-battle-order-only-pressure", 70, TraceOutputKind.BANDED));
    }

    @Test
    public void zeroCostBattleOrderDrainIsTerminal() {
        Facts facts = new Facts();
        facts.economy = economy(true, 8, true, 2, 3);
        facts.costWaived = true;
        assertPlan(facts,
                List.of("primary", "simpleTricks", "economy", "costWaived"),
                op("V140", 60, TraceOutputKind.ORDERING));
    }

    @Test
    public void v104SuppressesTurnLogicButContinuesLaterContributions() {
        Facts facts = new Facts();
        facts.economy = economy(true, 8, true, 2, 4);
        facts.drainValue = new ControlDrainAssessment.DrainValue(1, "site");
        facts.multi = new ControlDrainAssessment.MultiDrain(2, 2, "site");
        facts.huntDown = new ControlDrainAssessment.HuntDown(true, 2);

        assertPlan(facts,
                List.of("primary", "simpleTricks", "economy", "costWaived",
                        "drainValue", "multi", "huntDown"),
                op("V104", -2000, TraceOutputKind.VETO),
                op("V52-multi-drain", 200, TraceOutputKind.BANDED),
                op("V29.9-HuntDown-high", 40, TraceOutputKind.BANDED),
                op("V29.9-HuntDown-drain", 30, TraceOutputKind.BANDED));
    }

    @Test
    public void battleOrderTurnLogicPreservesEarlyAndLateScores() {
        Facts early = new Facts();
        early.economy = economy(true, 8, true, 2, 2);
        assertPlan(early,
                List.of("primary", "simpleTricks", "economy", "costWaived",
                        "drainValue", "multi", "huntDown"),
                op("V48-drain", -50, TraceOutputKind.BANDED));

        Facts late = new Facts();
        late.economy = economy(true, 8, true, 2, 3);
        assertPlan(late,
                List.of("primary", "simpleTricks", "economy", "costWaived",
                        "drainValue", "multi", "huntDown"),
                op("V52-drain-anyway", 50, TraceOutputKind.BANDED));
    }

    @Test
    public void nonBattleOrderMultiDrainAndHuntDownStayOrdered() {
        Facts facts = new Facts();
        facts.multi = new ControlDrainAssessment.MultiDrain(3, 3, "site");
        facts.huntDown = new ControlDrainAssessment.HuntDown(true, 2);

        assertPlan(facts,
                List.of("primary", "simpleTricks", "economy", "multi", "huntDown"),
                op("CONTROL-base-drain", 50, TraceOutputKind.BANDED),
                op("V52-multi-drain", 300, TraceOutputKind.BANDED),
                op("V29.9-HuntDown-high", 40, TraceOutputKind.BANDED),
                op("V29.9-HuntDown-drain", 30, TraceOutputKind.BANDED));
    }

    @Test
    public void allMultiDrainThresholdsRemainDistinct() {
        Facts two = new Facts();
        two.multi = new ControlDrainAssessment.MultiDrain(2, 1, "site");
        assertDeltas(two, 50, 200);

        Facts many = new Facts();
        many.multi = new ControlDrainAssessment.MultiDrain(1, 2, "site");
        assertDeltas(many, 50, 100);

        Facts one = new Facts();
        one.multi = new ControlDrainAssessment.MultiDrain(1, 1, "site");
        assertDeltas(one, 50);
    }

    private static void assertDeltas(Facts facts, float... deltas) {
        List<PolicyOperation> operations =
                ControlDrainAssessment.assess("A", facts).operations();
        assertEquals(deltas.length, operations.size());
        for (int i = 0; i < deltas.length; i++) {
            assertEquals(Float.floatToRawIntBits(deltas[i]),
                    Float.floatToRawIntBits(operations.get(i).delta()));
        }
    }

    private static void assertPlan(Facts facts, List<String> expectedQueries,
                                   Expected... expected) {
        List<PolicyOperation> actual =
                ControlDrainAssessment.assess("A", facts).operations();
        assertEquals(expectedQueries, facts.queries);
        assertEquals(expected.length, actual.size());
        for (int i = 0; i < expected.length; i++) {
            Expected e = expected[i];
            PolicyOperation a = actual.get(i);
            assertEquals("A", a.actionId());
            assertEquals(e.ruleId, a.ruleArmId().id());
            assertEquals(TraceDomainId.DRAIN_CONTROL, a.domainId());
            assertEquals(e.outputKind, a.outputKind());
            assertEquals(PolicyOperationKind.ADD, a.kind());
            assertEquals(Float.floatToRawIntBits(e.delta),
                    Float.floatToRawIntBits(a.delta()));
        }
    }

    private static Expected op(String ruleId, float delta, TraceOutputKind outputKind) {
        return new Expected(ruleId, delta, outputKind);
    }

    private static ControlDrainAssessment.Primary primary(float amount, float cost,
                                                           int force, int plannedSpend) {
        return new ControlDrainAssessment.Primary(
                amount, cost, "site", force, plannedSpend, 2);
    }

    private static ControlDrainAssessment.Economy economy(boolean battleOrder,
                                                           int force, boolean deployable,
                                                           int cheapest, int turn) {
        return new ControlDrainAssessment.Economy(
                battleOrder, force, deployable, cheapest, turn);
    }

    private record Expected(String ruleId, float delta, TraceOutputKind outputKind) { }

    private static final class Facts implements ControlDrainAssessment.Facts {
        private final List<String> queries = new ArrayList<>();
        private ControlDrainAssessment.Primary primary =
                ControlDrainAssessmentTest.primary(2, 0, 10, 0);
        private boolean simpleTricks;
        private ControlDrainAssessment.Economy economy =
                ControlDrainAssessmentTest.economy(false, 10, true, 2, 3);
        private boolean costWaived;
        private ControlDrainAssessment.DrainValue drainValue =
                new ControlDrainAssessment.DrainValue(2, "site");
        private ControlDrainAssessment.MultiDrain multi;
        private ControlDrainAssessment.HuntDown huntDown =
                new ControlDrainAssessment.HuntDown(false, 0);
        private boolean classicHuntExecutorHardLoss;

        @Override public boolean classicHuntExecutorHardLoss() {
            return classicHuntExecutorHardLoss;
        }

        @Override public ControlDrainAssessment.Primary primary() {
            queries.add("primary");
            return primary;
        }

        @Override public boolean simpleTricksBlocks() {
            queries.add("simpleTricks");
            return simpleTricks;
        }

        @Override public ControlDrainAssessment.Economy economy() {
            queries.add("economy");
            return economy;
        }

        @Override public boolean battleOrderCostWaived() {
            queries.add("costWaived");
            return costWaived;
        }

        @Override public ControlDrainAssessment.DrainValue battleOrderDrainValue() {
            queries.add("drainValue");
            return drainValue;
        }

        @Override public ControlDrainAssessment.MultiDrain multiDrain() {
            queries.add("multi");
            return multi;
        }

        @Override public ControlDrainAssessment.HuntDown huntDown() {
            queries.add("huntDown");
            return huntDown;
        }
    }
}
