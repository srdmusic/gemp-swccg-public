package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.framework.StartingSetup;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import com.gempukku.swccgo.game.PhysicalCardImpl;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class RegionalObjectiveEngineContractTest {
    private static final StartingSetup RALLTIIR_OPERATIONS = new StartingSetup() {
        @Override
        public HashMap<String, String> Cards() {
            return new HashMap<>() {{
                put("objective", "7_300");
                put("system", "2_148");
            }};
        }

        @Override
        public void Setup(VirtualTableScenario scn) {
            // The objective has one matching Ralltiir system, so setup auto-resolves.
        }
    };

    private static final StartingSetup DANTOOINE_BASE_OPERATIONS = new StartingSetup() {
        @Override
        public HashMap<String, String> Cards() {
            return new HashMap<>() {{
                put("objective", "7_135");
                put("system", "1_122");
            }};
        }

        @Override
        public void Setup(VirtualTableScenario scn) {
            // The objective has one matching Dantooine system, so setup auto-resolves.
        }
    };

    private VirtualTableScenario ralltiirScenario() {
        return new VirtualTableScenario(
                new HashMap<>() {{
                    put("xwing", "1_146");
                }},
                new HashMap<>() {{
                    put("forest", "7_284");
                    put("jungle", "7_285");
                    put("desert", "7_281");
                    put("jawa", "1_182");
                    put("tie", "1_304");
                }},
                24,
                24,
                StartingSetup.DefaultLSGroundLocation,
                RALLTIIR_OPERATIONS,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open
        );
    }

    private VirtualTableScenario dantooineScenario() {
        return new VirtualTableScenario(
                new HashMap<>() {{
                    put("forest", "7_121");
                    put("jungle", "7_122");
                    put("desert", "7_119");
                    put("jawa", "1_12");
                    put("operationsCenter", "601_257");
                    put("xwing", "1_146");
                }},
                new HashMap<>() {{
                    put("tie", "1_304");
                }},
                24,
                24,
                DANTOOINE_BASE_OPERATIONS,
                StartingSetup.DefaultDSGroundLocation,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open
        );
    }

    private void moveSiteToSystem(
            VirtualTableScenario scn,
            PhysicalCardImpl site,
            String systemTitle) {
        scn.RemoveCardZone(site);
        var placements = scn.gameState().getLocationPlacement(scn.game(), site, systemTitle, null);
        assertFalse("Expected a legal placement at " + systemTitle, placements.isEmpty());
        scn.gameState().addLocationToTable(scn.game(), site, placements.getFirst());
    }

    private void addRalltiirSites(VirtualTableScenario scn) {
        moveSiteToSystem(scn, scn.GetDSCard("forest"), Title.Ralltiir);
        moveSiteToSystem(scn, scn.GetDSCard("jungle"), Title.Ralltiir);
        moveSiteToSystem(scn, scn.GetDSCard("desert"), Title.Ralltiir);
    }

    private void addDantooineSites(VirtualTableScenario scn) {
        moveSiteToSystem(scn, scn.GetLSCard("forest"), Title.Dantooine);
        moveSiteToSystem(scn, scn.GetLSCard("jungle"), Title.Dantooine);
        moveSiteToSystem(scn, scn.GetLSCard("desert"), Title.Dantooine);
    }

    @Test
    public void ralltiirFrontRequiresThreeSitesEachControlledWithAnImperial() {
        var scn = ralltiirScenario();
        var objective = scn.GetDSCard("objective");
        var system = scn.GetDSCard("system");
        var forest = scn.GetDSCard("forest");
        var jungle = scn.GetDSCard("jungle");
        var desert = scn.GetDSCard("desert");
        var jawa = scn.GetDSCard("jawa");
        var tie = scn.GetDSCard("tie");
        var firstImperial = scn.GetDSFiller(1);
        var secondImperial = scn.GetDSFiller(2);
        var systemTrigger = scn.GetDSFiller(3);
        var affiliationTrigger = scn.GetDSFiller(4);
        var thirdImperial = scn.GetDSFiller(5);

        scn.MoveCardsToDSHand(systemTrigger, affiliationTrigger, thirdImperial);
        scn.StartGame();
        addRalltiirSites(scn);
        scn.MoveCardsToLocation(forest, firstImperial);
        scn.MoveCardsToLocation(jungle, secondImperial);
        scn.MoveCardsToLocation(system, tie);

        scn.DSActivateForceCheat(16);
        scn.SkipToPhase(Phase.DEPLOY);

        assertTrue(GameConditions.controls(scn.game(), VirtualTableScenario.DS, system));
        scn.DSDeployCardAndPassResponses(systemTrigger, scn.GetLSStartingLocation());
        assertFalse("Two sites plus the system must not satisfy the three-site law", objective.isFlipped());
        scn.LSPass();

        scn.MoveCardsToLocation(desert, jawa);
        assertTrue(GameConditions.controls(scn.game(), VirtualTableScenario.DS, desert));
        scn.DSDeployCardAndPassResponses(affiliationTrigger, scn.GetLSStartingLocation());
        assertFalse("Controlling the third site without an Imperial there must not flip", objective.isFlipped());
        scn.LSPass();

        scn.DSDeployCardAndPassResponses(thirdImperial, desert);
        assertTrue("The third Imperial-controlled Ralltiir site must complete the flip", objective.isFlipped());
    }

    @Test
    public void ralltiirFrontIsBlockedByOpponentControlOfTheSystem() {
        var scn = ralltiirScenario();
        var objective = scn.GetDSCard("objective");
        var system = scn.GetDSCard("system");
        var forest = scn.GetDSCard("forest");
        var jungle = scn.GetDSCard("jungle");
        var desert = scn.GetDSCard("desert");
        var xwing = scn.GetLSCard("xwing");
        var finalImperial = scn.GetDSFiller(3);
        var unblockTrigger = scn.GetDSFiller(4);

        scn.MoveCardsToDSHand(finalImperial, unblockTrigger);
        scn.StartGame();
        addRalltiirSites(scn);
        scn.MoveCardsToLocation(forest, scn.GetDSFiller(1));
        scn.MoveCardsToLocation(jungle, scn.GetDSFiller(2));
        scn.MoveCardsToLocation(system, xwing);

        scn.DSActivateForceCheat(12);
        scn.SkipToPhase(Phase.DEPLOY);

        assertTrue(GameConditions.controls(scn.game(), VirtualTableScenario.LS, system));
        scn.DSDeployCardAndPassResponses(finalImperial, desert);
        assertFalse("Opponent control of any Ralltiir location must block the front-side flip", objective.isFlipped());
        scn.LSPass();

        scn.MoveOutOfPlay(xwing);
        scn.DSDeployCardAndPassResponses(unblockTrigger, scn.GetLSStartingLocation());
        assertTrue("Removing the opponent's system control must allow the real trigger to flip", objective.isFlipped());
    }

    @Test
    public void ralltiirObjectiveDeploysTheFinalImperialForExactCostAndFlips() {
        var scn = ralltiirScenario();
        var objective = scn.GetDSCard("objective");
        var forest = scn.GetDSCard("forest");
        var jungle = scn.GetDSCard("jungle");
        var desert = scn.GetDSCard("desert");
        var finalImperial = scn.GetDSFiller(3);

        scn.StartGame();
        addRalltiirSites(scn);
        scn.MoveCardsToLocation(forest, scn.GetDSFiller(1));
        scn.MoveCardsToLocation(jungle, scn.GetDSFiller(2));
        for (int i = 4; i <= 24; i++) {
            scn.MoveOutOfPlay(scn.GetDSFiller(i));
        }
        scn.MoveCardsToBottomOfDSReserveDeck(finalImperial);
        scn.SkipToPhase(Phase.DEPLOY);
        while (scn.GetDSForcePileCount() > 0) {
            scn.MoveCardsToHand(scn.GetTopOfDSForcePile());
        }

        int deployCost = finalImperial.getBlueprint().getDeployCost().intValue();
        int forceBudget = 3;
        scn.MoveCardsToTopOfDSReserveDeck(
                scn.GetDSDestiny(0), scn.GetDSDestiny(1), scn.GetDSDestiny(2));
        scn.DSActivateForceCheat(forceBudget);
        assertEquals(forceBudget, scn.GetDSForcePileCount());
        assertTrue(scn.DSCardActionAvailable(
                objective, "Deploy card from Reserve Deck"));
        scn.DSUseCardAction(
                objective, "Deploy card from Reserve Deck");
        assertTrue(scn.DSHasCardChoiceAvailable(finalImperial));
        scn.DSChooseCard(finalImperial);
        scn.PassAllResponses();
        assertTrue(scn.DSDecisionAvailable("Choose where to deploy")
                || scn.DSDecisionAvailable("Choose location where to deploy"));
        scn.DSChooseCard(desert);
        scn.PassAllResponses();

        assertEquals("Objective Reserve Deck deployment must spend exact deploy cost",
                forceBudget - deployCost, scn.GetDSForcePileCount());
        assertSame(Zone.AT_LOCATION, finalImperial.getZone());
        assertTrue("The downloaded final Imperial must complete the real flip",
                objective.isFlipped());
    }

    @Test
    public void ralltiirBackNeedsTwoOpponentControlledLocations() {
        var scn = startRalltiirFlipped();
        var objective = scn.GetDSCard("objective");
        var forest = scn.GetDSCard("forest");
        var jungle = scn.GetDSCard("jungle");
        var firstRebel = scn.GetLSFiller(1);
        var secondRebel = scn.GetLSFiller(2);

        scn.MoveOutOfPlay(scn.GetDSFiller(1));
        scn.MoveOutOfPlay(scn.GetDSFiller(2));
        scn.MoveCardsToLSHand(firstRebel, secondRebel);
        scn.LSActivateForceCheat(10);
        scn.SkipToLSTurn(Phase.DEPLOY);

        scn.LSDeployCardAndPassResponses(firstRebel, forest);
        assertTrue("One opponent-controlled Ralltiir location must not flip back", objective.isFlipped());
        scn.DSPass();

        scn.LSDeployCardAndPassResponses(secondRebel, jungle);
        assertFalse("Two opponent-controlled Ralltiir locations must flip back", objective.isFlipped());
    }

    @Test
    public void dantooineFrontRequiresThreeSitesEachControlledWithARebel() {
        var scn = dantooineScenario();
        var objective = scn.GetLSCard("objective");
        var system = scn.GetLSCard("system");
        var forest = scn.GetLSCard("forest");
        var jungle = scn.GetLSCard("jungle");
        var desert = scn.GetLSCard("desert");
        var jawa = scn.GetLSCard("jawa");
        var xwing = scn.GetLSCard("xwing");
        var firstRebel = scn.GetLSFiller(1);
        var secondRebel = scn.GetLSFiller(2);
        var systemTrigger = scn.GetLSFiller(3);
        var affiliationTrigger = scn.GetLSFiller(4);
        var thirdRebel = scn.GetLSFiller(5);

        scn.MoveCardsToLSHand(systemTrigger, affiliationTrigger, thirdRebel);
        scn.StartGame();
        addDantooineSites(scn);
        scn.MoveCardsToLocation(forest, firstRebel);
        scn.MoveCardsToLocation(jungle, secondRebel);
        scn.MoveCardsToLocation(system, xwing);

        scn.LSActivateForceCheat(16);
        scn.SkipToLSTurn(Phase.DEPLOY);

        assertTrue(GameConditions.controls(scn.game(), VirtualTableScenario.LS, system));
        scn.LSDeployCardAndPassResponses(systemTrigger, scn.GetDSStartingLocation());
        assertFalse("Two sites plus the system must not satisfy the three-site law", objective.isFlipped());
        scn.DSPass();

        scn.MoveCardsToLocation(desert, jawa);
        assertTrue(GameConditions.controls(scn.game(), VirtualTableScenario.LS, desert));
        scn.LSDeployCardAndPassResponses(affiliationTrigger, scn.GetDSStartingLocation());
        assertFalse("Controlling the third site without a Rebel there must not flip", objective.isFlipped());
        scn.DSPass();

        scn.LSDeployCardAndPassResponses(thirdRebel, desert);
        assertTrue("The third Rebel-controlled Dantooine site must complete the flip", objective.isFlipped());
    }

    @Test
    public void dantooineFrontIsBlockedByOpponentControlOfTheSystem() {
        var scn = dantooineScenario();
        var objective = scn.GetLSCard("objective");
        var system = scn.GetLSCard("system");
        var forest = scn.GetLSCard("forest");
        var jungle = scn.GetLSCard("jungle");
        var desert = scn.GetLSCard("desert");
        var tie = scn.GetDSCard("tie");
        var finalRebel = scn.GetLSFiller(3);
        var unblockTrigger = scn.GetLSFiller(4);

        scn.MoveCardsToLSHand(finalRebel, unblockTrigger);
        scn.StartGame();
        addDantooineSites(scn);
        scn.MoveCardsToLocation(forest, scn.GetLSFiller(1));
        scn.MoveCardsToLocation(jungle, scn.GetLSFiller(2));
        scn.MoveCardsToLocation(system, tie);

        scn.LSActivateForceCheat(12);
        scn.SkipToLSTurn(Phase.DEPLOY);

        assertTrue(GameConditions.controls(scn.game(), VirtualTableScenario.DS, system));
        scn.LSDeployCardAndPassResponses(finalRebel, desert);
        assertFalse("Opponent control of any Dantooine location must block the front-side flip", objective.isFlipped());
        scn.DSPass();

        scn.MoveOutOfPlay(tie);
        scn.LSDeployCardAndPassResponses(unblockTrigger, scn.GetDSStartingLocation());
        assertTrue("Removing the opponent's system control must allow the real trigger to flip", objective.isFlipped());
    }

    @Test
    public void dantooineObjectiveDeploysTheFinalRebelForExactCostAndFlips() {
        var scn = dantooineScenario();
        var objective = scn.GetLSCard("objective");
        var forest = scn.GetLSCard("forest");
        var jungle = scn.GetLSCard("jungle");
        var desert = scn.GetLSCard("desert");
        var finalRebel = scn.GetLSFiller(3);

        scn.StartGame();
        addDantooineSites(scn);
        scn.MoveCardsToLocation(forest, scn.GetLSFiller(1));
        scn.MoveCardsToLocation(jungle, scn.GetLSFiller(2));
        for (int i = 4; i <= 24; i++) {
            scn.MoveOutOfPlay(scn.GetLSFiller(i));
        }
        scn.MoveCardsToBottomOfLSReserveDeck(finalRebel);
        scn.SkipToLSTurn(Phase.DEPLOY);
        while (scn.GetLSForcePileCount() > 0) {
            scn.MoveCardsToHand(scn.GetTopOfLSForcePile());
        }

        int deployCost = finalRebel.getBlueprint().getDeployCost().intValue();
        int forceBudget = 3;
        scn.MoveCardsToTopOfLSReserveDeck(
                scn.GetLSDestiny(0), scn.GetLSDestiny(1), scn.GetLSDestiny(2));
        scn.LSActivateForceCheat(forceBudget);
        assertEquals(forceBudget, scn.GetLSForcePileCount());
        assertTrue(scn.LSCardActionAvailable(
                objective, "Deploy card from Reserve Deck"));
        scn.LSUseCardAction(
                objective, "Deploy card from Reserve Deck");
        assertTrue(scn.LSHasCardChoiceAvailable(finalRebel));
        scn.LSChooseCard(finalRebel);
        scn.PassAllResponses();
        assertTrue(scn.LSDecisionAvailable("Choose where to deploy"));
        scn.LSChooseCard(desert);
        scn.PassAllResponses();

        assertEquals("Objective Reserve Deck deployment must spend exact deploy cost",
                forceBudget - deployCost, scn.GetLSForcePileCount());
        assertSame(Zone.AT_LOCATION, finalRebel.getZone());
        assertTrue("The downloaded final Rebel must complete the real flip",
                objective.isFlipped());
    }

    @Test
    public void dantooineBackNormallyNeedsTwoOpponentControlledLocations() {
        var scn = startDantooineFlipped();
        var objective = scn.GetLSCard("objective");
        var forest = scn.GetLSCard("forest");
        var jungle = scn.GetLSCard("jungle");
        var firstImperial = scn.GetDSFiller(1);
        var secondImperial = scn.GetDSFiller(2);

        scn.MoveOutOfPlay(scn.GetLSFiller(1));
        scn.MoveOutOfPlay(scn.GetLSFiller(2));
        scn.MoveCardsToDSHand(firstImperial, secondImperial);
        scn.DSActivateForceCheat(10);
        scn.SkipToDSTurn(Phase.DEPLOY);

        scn.DSDeployCardAndPassResponses(firstImperial, forest);
        assertTrue("One opponent-controlled Dantooine location must not flip back", objective.isFlipped());
        scn.LSPass();

        scn.DSDeployCardAndPassResponses(secondImperial, jungle);
        assertFalse("Two opponent-controlled Dantooine locations must normally flip back", objective.isFlipped());
    }

    @Test
    public void dantooineLegacyOperationsCenterRaisesBackThresholdToThree() {
        var scn = startDantooineFlipped();
        var objective = scn.GetLSCard("objective");
        var forest = scn.GetLSCard("forest");
        var jungle = scn.GetLSCard("jungle");
        var desert = scn.GetLSCard("desert");
        var operationsCenter = scn.GetLSCard("operationsCenter");
        var firstImperial = scn.GetDSFiller(1);
        var secondImperial = scn.GetDSFiller(2);
        var thirdImperial = scn.GetDSFiller(3);

        moveSiteToSystem(scn, operationsCenter, Title.Dantooine);
        scn.MoveOutOfPlay(scn.GetLSFiller(1));
        scn.MoveOutOfPlay(scn.GetLSFiller(2));
        scn.MoveOutOfPlay(scn.GetLSFiller(3));
        scn.MoveCardsToDSHand(firstImperial, secondImperial, thirdImperial);
        scn.DSActivateForceCheat(12);
        scn.SkipToDSTurn(Phase.DEPLOY);

        scn.DSDeployCardAndPassResponses(firstImperial, forest);
        assertTrue(objective.isFlipped());
        scn.LSPass();

        scn.DSDeployCardAndPassResponses(secondImperial, jungle);
        assertTrue("Operations Center must prevent two locations from flipping the objective back", objective.isFlipped());
        scn.LSPass();

        scn.DSDeployCardAndPassResponses(thirdImperial, desert);
        assertFalse("Operations Center must still allow three locations to flip the objective back", objective.isFlipped());
    }

    private VirtualTableScenario startRalltiirFlipped() {
        var scn = ralltiirScenario();
        var forest = scn.GetDSCard("forest");
        var jungle = scn.GetDSCard("jungle");
        var desert = scn.GetDSCard("desert");
        var finalImperial = scn.GetDSFiller(3);

        scn.MoveCardsToDSHand(finalImperial);
        scn.StartGame();
        addRalltiirSites(scn);
        scn.MoveCardsToLocation(forest, scn.GetDSFiller(1));
        scn.MoveCardsToLocation(jungle, scn.GetDSFiller(2));
        scn.DSActivateForceCheat(8);
        scn.SkipToPhase(Phase.DEPLOY);

        scn.DSDeployCardAndPassResponses(finalImperial, desert);
        assertTrue(scn.GetDSCard("objective").isFlipped());
        scn.LSPass();
        return scn;
    }

    private VirtualTableScenario startDantooineFlipped() {
        var scn = dantooineScenario();
        var forest = scn.GetLSCard("forest");
        var jungle = scn.GetLSCard("jungle");
        var desert = scn.GetLSCard("desert");
        var finalRebel = scn.GetLSFiller(3);

        scn.MoveCardsToLSHand(finalRebel);
        scn.StartGame();
        addDantooineSites(scn);
        scn.MoveCardsToLocation(forest, scn.GetLSFiller(1));
        scn.MoveCardsToLocation(jungle, scn.GetLSFiller(2));
        scn.LSActivateForceCheat(8);
        scn.SkipToLSTurn(Phase.DEPLOY);

        scn.LSDeployCardAndPassResponses(finalRebel, desert);
        assertTrue(scn.GetLSCard("objective").isFlipped());
        scn.DSPass();
        return scn;
    }
}
