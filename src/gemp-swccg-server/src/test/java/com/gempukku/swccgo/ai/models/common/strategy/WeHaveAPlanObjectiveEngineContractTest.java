package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.framework.StartingSetup;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import org.junit.Test;

import java.util.HashMap;

import static com.gempukku.swccgo.framework.Assertions.assertAtLocation;
import static com.gempukku.swccgo.framework.Assertions.assertInZone;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WeHaveAPlanObjectiveEngineContractTest {
    private static final StartingSetup WE_HAVE_A_PLAN = new StartingSetup() {
        @Override
        public HashMap<String, String> Cards() {
            return new HashMap<>() {{
                put("objective", "14_52");
                put("throne", "12_83");
                put("hallway", "14_51");
                put("courtyard", "12_81");
            }};
        }

        @Override
        public void Setup(VirtualTableScenario scn) {
            if (scn.LSDecisionAvailable("On which side")) {
                scn.LSChoose("Right");
            }
        }
    };

    private VirtualTableScenario scenario() {
        return new VirtualTableScenario(
                new HashMap<>() {{
                    put("padme", "11_8");
                }},
                new HashMap<>() {{
                    put("vader", "1_168");
                }},
                24,
                24,
                WE_HAVE_A_PLAN,
                StartingSetup.DefaultDSGroundLocation,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open
        );
    }

    private void emptyLSForcePile(VirtualTableScenario scn) {
        while (scn.GetLSForcePileCount() > 0) {
            scn.MoveCardsToHand(scn.GetTopOfLSForcePile());
        }
    }

    private void emptyDSForcePile(VirtualTableScenario scn) {
        while (scn.GetDSForcePileCount() > 0) {
            scn.MoveCardsToHand(scn.GetTopOfDSForcePile());
        }
    }

    @Test
    public void amidalaMustDeployOutsideAndMoveThroughThePalaceBeforeVaderBattlesHerAway() {
        var scn = scenario();
        var objective = scn.GetLSCard("objective");
        var throne = scn.GetLSCard("throne");
        var hallway = scn.GetLSCard("hallway");
        var courtyard = scn.GetLSCard("courtyard");
        var padme = scn.GetLSCard("padme");
        var vader = scn.GetDSCard("vader");

        scn.MoveCardsToLSHand(padme);
        scn.MoveCardsToDSHand(vader);
        scn.StartGame();

        scn.SkipToLSTurn(Phase.DEPLOY);
        emptyLSForcePile(scn);
        scn.LSActivateForceCheat(2);
        assertEquals(2, scn.GetLSForcePileCount());

        scn.LSDeployCard(padme);
        assertTrue(scn.LSDecisionAvailable("Choose where to deploy"));
        assertTrue("The exterior Courtyard must be a legal deployment target",
                scn.LSHasCardChoiceAvailable(courtyard));
        assertFalse("The front side prohibits direct character deployment to the interior Hallway",
                scn.LSHasCardChoiceAvailable(hallway));
        assertFalse("The front side prohibits direct character deployment to the interior Throne Room",
                scn.LSHasCardChoiceAvailable(throne));
        scn.LSChooseCard(courtyard);
        scn.PassAllResponses();

        assertEquals("Padme's deploy must spend the exact two-Force budget",
                0, scn.GetLSForcePileCount());
        assertAtLocation(courtyard, padme);
        assertFalse(objective.isFlipped());

        scn.SkipToPhase(Phase.MOVE);
        assertEquals(0, scn.GetLSForcePileCount());
        assertFalse("Padme cannot move without the required Force",
                scn.LSMoveAvailable(padme));

        assertTrue(scn.LSCardActionAvailable(objective, "Activate 1 Force"));
        scn.LSUseCardAction(objective, "Activate 1 Force");
        scn.PassAllResponses();
        assertEquals(1, scn.GetLSForcePileCount());
        scn.DSPass();
        scn.LSMoveCard(padme, hallway);
        scn.PassAllResponses();

        assertEquals("The first regular move must spend the activated Force",
                0, scn.GetLSForcePileCount());
        assertAtLocation(hallway, padme);
        assertFalse("Moving one site closer must not satisfy the Throne Room gate",
                objective.isFlipped());

        scn.SkipToLSTurn(Phase.MOVE);
        emptyLSForcePile(scn);
        assertEquals(0, scn.GetLSForcePileCount());
        assertTrue(scn.LSCardActionAvailable(objective, "Activate 1 Force"));
        scn.LSUseCardAction(objective, "Activate 1 Force");
        scn.PassAllResponses();
        assertEquals(1, scn.GetLSForcePileCount());
        scn.DSPass();
        scn.LSMoveCard(padme, throne);
        scn.PassAllResponses();

        assertEquals("The second regular move must spend the activated Force",
                0, scn.GetLSForcePileCount());
        assertAtLocation(throne, padme);
        assertTrue("Control of the exact Throne Room with Amidala there must fire the real flip trigger",
                objective.isFlipped());

        scn.SkipToDSTurn(Phase.DEPLOY);
        emptyDSForcePile(scn);
        scn.DSActivateForceCheat(7);
        assertEquals(7, scn.GetDSForcePileCount());
        scn.DSDeployCardAndPassResponses(vader, throne);

        assertEquals("Vader's deployment must leave exactly the Force required to battle",
                1, scn.GetDSForcePileCount());
        assertAtLocation(throne, padme, vader);
        assertTrue("Both players occupying the Throne Room means Dark Side does not yet control it",
                objective.isFlipped());

        scn.PrepareDSDestiny(0);
        scn.SkipToPhase(Phase.BATTLE);
        scn.DSInitiateBattle(throne);
        assertEquals("Battle initiation must spend the final Force",
                0, scn.GetDSForcePileCount());
        scn.SkipToDamageSegment(true);

        assertEquals("Vader's six power plus his destiny bonus defeats Padme by four",
                4, scn.GetUnpaidLSBattleDamage());
        scn.LSPayBattleDamageFromCardInPlay(padme);

        assertInZone(Zone.LOST_PILE, padme);
        assertFalse("Once Vader alone controls the exact Throne Room, the real flip-back trigger must fire",
                objective.isFlipped());
    }
}
