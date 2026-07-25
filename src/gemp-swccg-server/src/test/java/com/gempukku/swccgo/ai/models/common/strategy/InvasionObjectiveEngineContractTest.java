package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.framework.StartingSetup;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class InvasionObjectiveEngineContractTest {
    private static final StartingSetup INVASION = new StartingSetup() {
        @Override
        public HashMap<String, String> Cards() {
            return new HashMap<>() {{
                put("invasion", "14_113");
                put("naboo", "12_169");
                put("flagship", "14_114");
                put("swamp", "12_171");
                put("racks", "14_96");
            }};
        }

        @Override
        public void Setup(VirtualTableScenario scn) {
            // Each required starting card has one matching candidate, so setup auto-resolves.
        }
    };

    private VirtualTableScenario scenario() {
        return new VirtualTableScenario(
                new HashMap<>() {{
                    put("xwing", "1_146");
                    put("swampTrooper", "1_28");
                    put("throneTrooper", "1_28");
                }},
                new HashMap<>() {{
                    put("throne", "12_174");
                    put("neimoidian", "12_111");
                    put("sidious", "208_35");
                }},
                20,
                20,
                StartingSetup.DefaultLSGroundLocation,
                INVASION,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open
        );
    }

    private VirtualTableScenario startFlipped() {
        var scn = scenario();
        var invasion = scn.GetDSCard("invasion");
        var throne = scn.GetDSCard("throne");
        var neimoidian = scn.GetDSCard("neimoidian");
        var sidious = scn.GetDSCard("sidious");

        scn.MoveCardsToDSHand(sidious);
        scn.MoveCardsToLSHand(
                scn.GetLSCard("xwing"),
                scn.GetLSCard("swampTrooper"),
                scn.GetLSCard("throneTrooper"));
        scn.StartGame();

        scn.MoveLocationToTable(throne);
        scn.MoveCardsToLocation(throne, neimoidian);
        scn.DSActivateForceCheat(8);
        scn.SkipToPhase(Phase.DEPLOY);

        // Real deployment supplies the table-change EffectResult required by the card.
        scn.DSDeployCardAndPassResponses(sidious, throne);
        assertTrue(invasion.isFlipped());

        // Restore Dark Side's top-level action decision.
        scn.LSPass();
        return scn;
    }

    @Test
    public void frontRequiresBothExactConditions() {
        var scn = scenario();
        var invasion = scn.GetDSCard("invasion");
        var naboo = scn.GetDSCard("naboo");
        var flagship = scn.GetDSCard("flagship");
        var throne = scn.GetDSCard("throne");
        var neimoidian = scn.GetDSCard("neimoidian");
        var sidious = scn.GetDSCard("sidious");

        scn.MoveCardsToDSHand(sidious, neimoidian);
        scn.StartGame();

        scn.MoveLocationToTable(throne);
        scn.DSActivateForceCheat(16);
        scn.SkipToPhase(Phase.DEPLOY);

        // Naboo controlled and Throne controlled, but no Neimoidian there.
        scn.DSDeployCardAndPassResponses(sidious, throne);
        assertFalse(invasion.isFlipped());
        scn.LSPass();

        // Neimoidian now at Throne, but Naboo system is no longer controlled.
        scn.MoveCardsToDSHand(flagship);
        scn.DSDeployCardAndPassResponses(neimoidian, throne);
        assertFalse(invasion.isFlipped());
        scn.LSPass();

        // Actual Flagship redeployment restores system control and fires the real trigger.
        scn.DSDeployCardAndPassResponses(flagship, naboo);
        assertTrue(invasion.isFlipped());
    }

    @Test
    public void backFlipsWhenOpponentControlsNabooSystem() {
        var scn = startFlipped();
        var invasion = scn.GetDSCard("invasion");
        var naboo = scn.GetDSCard("naboo");
        var flagship = scn.GetDSCard("flagship");
        var xwing = scn.GetLSCard("xwing");

        scn.LSActivateForceCheat(5);
        scn.SkipToLSTurn(Phase.DEPLOY);
        scn.MoveOutOfPlay(flagship);

        scn.LSDeployCardAndPassResponses(xwing, naboo);
        assertFalse(invasion.isFlipped());
    }

    @Test
    public void backIgnoresOtherNabooSitesButFlipsForThroneRoom() {
        var scn = startFlipped();
        var invasion = scn.GetDSCard("invasion");
        var swamp = scn.GetDSCard("swamp");
        var throne = scn.GetDSCard("throne");
        var neimoidian = scn.GetDSCard("neimoidian");
        var sidious = scn.GetDSCard("sidious");
        var swampTrooper = scn.GetLSCard("swampTrooper");
        var throneTrooper = scn.GetLSCard("throneTrooper");

        scn.LSActivateForceCheat(5);
        scn.SkipToLSTurn(Phase.DEPLOY);
        scn.MoveOutOfPlay(neimoidian);
        scn.MoveOutOfPlay(sidious);

        scn.LSDeployCardAndPassResponses(swampTrooper, swamp);
        assertTrue(invasion.isFlipped());

        // Restore Light Side's top-level action decision.
        scn.DSPass();

        scn.LSDeployCardAndPassResponses(throneTrooper, throne);
        assertFalse(invasion.isFlipped());
    }
}
