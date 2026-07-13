package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.DecisionOrigin;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.framework.StartingSetup;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Real engine decision shapes and phase transitions used by the migration. */
public class ActivateControlEngineContractTest {

    @Test
    public void realActivateFlowExposesAmountAllowanceAndInterruptionAckShapes() {
        VirtualTableScenario scenario = scenario();
        scenario.StartGame();

        assertTopLevel(scenario, scenario.DSGetDecision(), Phase.ACTIVATE);
        scenario.DSChooseAction("Activate Force");

        AwaitingDecision amount = scenario.DSGetDecision();
        assertEquals(AwaitingDecisionType.INTEGER, amount.getDecisionType());
        assertEquals("Choose amount of Force to activate", amount.getText());
        assertOrigin(amount, DecisionOrigin.ACTIVATE_AMOUNT);
        assertEquals("0", amount.getDecisionParameters().get("min")[0]);
        int maximum = Integer.parseInt(amount.getDecisionParameters().get("max")[0]);
        assertTrue("fixture needs the real opponent-allowance branch", maximum > 1);
        assertEquals(String.valueOf(maximum), amount.getDecisionParameters().get("defaultValue")[0]);

        scenario.DSDecided(maximum);
        AwaitingDecision allowance = scenario.LSGetDecision();
        assertEquals(AwaitingDecisionType.INTEGER, allowance.getDecisionType());
        assertEquals("Choose amount of Force to allow opponent to activate without you performing a top-level action",
                allowance.getText());
        assertOrigin(allowance, DecisionOrigin.ACTIVATE_ALLOWANCE);
        assertEquals("1", allowance.getDecisionParameters().get("min")[0]);
        assertEquals(String.valueOf(maximum), allowance.getDecisionParameters().get("max")[0]);
        assertEquals(String.valueOf(maximum), allowance.getDecisionParameters().get("defaultValue")[0]);

        scenario.LSDecided(1);
        AwaitingDecision acknowledgement = scenario.DSGetDecision();
        assertEquals(AwaitingDecisionType.MULTIPLE_CHOICE, acknowledgement.getDecisionType());
        assertTrue(acknowledgement.getText().startsWith(
                "Opponent chose to interrupt Force activation"));
        assertOrigin(acknowledgement, DecisionOrigin.ACTIVATE_INTERRUPTION_ACK);
        assertArrayEquals(new String[]{"OK"}, acknowledgement.getDecisionParameters().get("results"));
    }

    @Test
    public void realActivateAndControlPassLifecycleKeepsCurrentTransitions() {
        VirtualTableScenario scenario = scenario();
        scenario.StartGame();

        assertTopLevel(scenario, scenario.DSGetDecision(), Phase.ACTIVATE);
        scenario.DSPass();

        AwaitingDecision confirmation = scenario.DSGetDecision();
        assertEquals(AwaitingDecisionType.MULTIPLE_CHOICE, confirmation.getDecisionType());
        assertEquals("You have not activated Force. Do you want to Pass?", confirmation.getText());
        assertOrigin(confirmation, DecisionOrigin.ACTIVATE_ZERO_CONFIRM);
        assertArrayEquals(new String[]{"Yes", "No"},
                confirmation.getDecisionParameters().get("results"));

        scenario.DSChooseNo();
        assertTopLevel(scenario, scenario.DSGetDecision(), Phase.ACTIVATE);

        scenario.DSPass();
        scenario.DSChooseYes();
        assertTopLevel(scenario, scenario.LSGetDecision(), Phase.ACTIVATE);

        scenario.LSPass();
        assertTopLevel(scenario, scenario.DSGetDecision(), Phase.CONTROL);

        scenario.DSPass();
        assertTopLevel(scenario, scenario.LSGetDecision(), Phase.CONTROL);
        scenario.LSPass();
        assertTopLevel(scenario, scenario.DSGetDecision(), Phase.DEPLOY);
    }

    private static void assertTopLevel(VirtualTableScenario scenario,
                                       AwaitingDecision decision, Phase phase) {
        assertNotNull(decision);
        assertEquals(phase, scenario.GetCurrentPhase());
        assertEquals(AwaitingDecisionType.CARD_ACTION_CHOICE, decision.getDecisionType());
        assertEquals("Choose " + phase.getHumanReadable() + " action or Pass", decision.getText());
        assertNotNull(decision.getDecisionParameters().get("actionId"));
        assertNotNull(decision.getDecisionParameters().get("actionText"));
        // Every top-level phase-action decision carries the PHASE_ACTION stamp
        // (both ACTIVATE and CONTROL flow through this helper).
        assertOrigin(decision, DecisionOrigin.PHASE_ACTION);
    }

    /**
     * Proves both halves of a stamp site: the exact "decisionOrigin" wire value AND
     * that the decision's real wire type equals the origin's required wire type.
     */
    private static void assertOrigin(AwaitingDecision decision, DecisionOrigin expected) {
        String[] stamped = decision.getDecisionParameters().get(DecisionOrigin.WIRE_PARAMETER);
        assertNotNull("decision missing decisionOrigin stamp", stamped);
        assertEquals(1, stamped.length);
        assertEquals(expected.name(), stamped[0]);
        assertEquals(expected.requiredWireTypeName(), decision.getDecisionType().name());
    }

    private static VirtualTableScenario scenario() {
        return new VirtualTableScenario(new HashMap<>(), new HashMap<>(),
                20, 20,
                StartingSetup.DefaultLSGroundLocation, StartingSetup.DefaultDSGroundLocation,
                StartingSetup.NoLSStartingInterrupts, StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields, StartingSetup.NoDSShields,
                VirtualTableScenario.Open);
    }
}
