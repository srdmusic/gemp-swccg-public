package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ControlDrainAssessmentTest {

    @Test
    public void nonpositiveDrainStopsAfterPrimary() {
        Facts facts = new Facts();
        facts.primary = primary(0, 0, 10, 0);
        assertPlan(facts, List.of("primary"), op("V24.15", -9999, true));
    }

    @Test
    public void largeNetLossStopsAtV189() {
        Facts facts = new Facts();
        facts.primary = primary(1, 3, 10, 0);
        assertPlan(facts, List.of("primary"), op("V189", -2000, true));
    }

    @Test
    public void netMinusOneStopsWhenTurnPlanIsUnfunded() {
        Facts facts = new Facts();
        facts.primary = primary(2, 3, 5, 1);
        assertPlan(facts, List.of("primary"), op("V189", -2000, true));
    }

    @Test
    public void simpleTricksStopsBeforeEconomyQueries() {
        Facts facts = new Facts();
        facts.simpleTricks = true;
        assertPlan(facts, List.of("primary", "simpleTricks"),
                op("V25", -9999, true));
    }

    @Test
    public void battleOrderAffordabilityAndNoBodyAreTerminal() {
        Facts cannotAfford = new Facts();
        cannotAfford.economy = economy(true, 2, true, 1, 3);
        assertPlan(cannotAfford, List.of("primary", "simpleTricks", "economy"),
                op("BATTLE_ORDER", -50, true));

        Facts noBody = new Facts();
        noBody.economy = economy(true, 8, false, Integer.MAX_VALUE, 3);
        assertPlan(noBody, List.of("primary", "simpleTricks", "economy"),
                op("BATTLE_ORDER", 70, true));
    }

    @Test
    public void zeroCostBattleOrderDrainIsTerminal() {
        Facts facts = new Facts();
        facts.economy = economy(true, 8, true, 2, 3);
        facts.costWaived = true;
        assertPlan(facts,
                List.of("primary", "simpleTricks", "economy", "costWaived"),
                op("V140", 60, true));
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
                op("V104", -2000, false),
                op("V52", 200, false),
                op("V29.9", 40, false),
                op("V29.9", 30, false));
    }

    @Test
    public void battleOrderTurnLogicPreservesEarlyAndLateScores() {
        Facts early = new Facts();
        early.economy = economy(true, 8, true, 2, 2);
        assertPlan(early,
                List.of("primary", "simpleTricks", "economy", "costWaived",
                        "drainValue", "multi", "huntDown"),
                op("V48", -50, false));

        Facts late = new Facts();
        late.economy = economy(true, 8, true, 2, 3);
        assertPlan(late,
                List.of("primary", "simpleTricks", "economy", "costWaived",
                        "drainValue", "multi", "huntDown"),
                op("V52", 50, false));
    }

    @Test
    public void nonBattleOrderMultiDrainAndHuntDownStayOrdered() {
        Facts facts = new Facts();
        facts.multi = new ControlDrainAssessment.MultiDrain(3, 3, "site");
        facts.huntDown = new ControlDrainAssessment.HuntDown(true, 2);

        assertPlan(facts,
                List.of("primary", "simpleTricks", "economy", "multi", "huntDown"),
                op("CONTROL_BASE", 50, false),
                op("V52", 300, false),
                op("V29.9", 40, false),
                op("V29.9", 30, false));
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
        List<ControlDrainAssessment.Operation> operations =
                ControlDrainAssessment.assess(facts).operations();
        assertEquals(deltas.length, operations.size());
        for (int i = 0; i < deltas.length; i++) {
            assertEquals(Float.floatToRawIntBits(deltas[i]),
                    Float.floatToRawIntBits(operations.get(i).delta()));
        }
    }

    private static void assertPlan(Facts facts, List<String> expectedQueries,
                                   Expected... expected) {
        List<ControlDrainAssessment.Operation> actual =
                ControlDrainAssessment.assess(facts).operations();
        assertEquals(expectedQueries, facts.queries);
        assertEquals(expected.length, actual.size());
        for (int i = 0; i < expected.length; i++) {
            Expected e = expected[i];
            ControlDrainAssessment.Operation a = actual.get(i);
            assertEquals(e.ruleId, a.ruleId());
            assertEquals(Float.floatToRawIntBits(e.delta),
                    Float.floatToRawIntBits(a.delta()));
            assertEquals(e.terminal, a.terminal());
            if (e.terminal) {
                assertTrue(a.terminal());
            } else {
                assertFalse(a.terminal());
            }
        }
    }

    private static Expected op(String ruleId, float delta, boolean terminal) {
        return new Expected(ruleId, delta, terminal);
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

    private record Expected(String ruleId, float delta, boolean terminal) { }

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
