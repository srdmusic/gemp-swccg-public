package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.framework.StartingSetup;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import org.junit.Test;

import java.util.HashMap;

import static com.gempukku.swccgo.framework.Assertions.assertInZone;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EndorOperationsObjectiveEngineContractTest {
    private static final StartingSetup ENDOR_OPERATIONS = new StartingSetup() {
        @Override
        public HashMap<String, String> Cards() {
            return new HashMap<>() {{
                put("objective", "8_167");
                put("endor", "8_157");
                put("bunker", "8_160");
                put("platform", "8_166");
            }};
        }

        @Override
        public void Setup(VirtualTableScenario scn) {
            if (scn.DSDecisionAvailable("On which side")) {
                scn.DSChoose("Left");
            }
            if (scn.DSDecisionAvailable("On which side")) {
                scn.DSChoose("Right");
            }
        }
    };

    private VirtualTableScenario scenario() {
        return new VirtualTableScenario(
                new HashMap<>() {{
                    put("luke", "1_19");
                    put("xwing", "1_146");
                    put("deactivateShieldGenerator", "8_43");
                }},
                new HashMap<>() {{
                    put("stormtrooper", "1_194");
                    put("tableChangeTrigger", "1_194");
                    put("tableChangeTrigger2", "1_194");
                    put("forestClearing", "8_164");
                    put("darkForest", "8_161");
                    put("bikerScout1", "8_92");
                    put("bikerScout2", "8_92");
                    put("bikerScout3", "8_92");
                    put("ominousRumors", "8_127");
                    put("ominousRumorsV", "223_19");
                    put("ominousRumorsLegacy", "601_261");
                    put("baseEstablishSecretBase", "8_124");
                    put("establishSecretBase", "207_25");
                    put("legacyEstablishSecretBase", "601_260");
                    put("tempestScout", "8_171");
                    put("tempestScout1", "8_172");
                    put("tempestScout2", "8_173");
                    put("atstPilot1", "8_91");
                    put("atstPilot2", "8_91");
                    put("dyer", "8_93");
                    put("reactorCore", "9_146");
                }},
                24,
                24,
                StartingSetup.DefaultLSGroundLocation,
                ENDOR_OPERATIONS,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open
        );
    }

    @Test
    public void setupRequiredEffectsFlipAndFlipBackUseRealCardLogic() {
        var scn = scenario();
        var objective = scn.GetDSCard("objective");
        var endor = scn.GetDSCard("endor");
        var bunker = scn.GetDSCard("bunker");
        var platform = scn.GetDSCard("platform");
        var stormtrooper = scn.GetDSCard("stormtrooper");
        var tableChangeTrigger = scn.GetDSCard("tableChangeTrigger");
        var ominousRumors = scn.GetDSCard("ominousRumors");
        var establishSecretBase = scn.GetDSCard("establishSecretBase");
        var luke = scn.GetLSCard("luke");

        scn.MoveCardsToDSHand(stormtrooper, tableChangeTrigger, ominousRumors,
                establishSecretBase);
        scn.MoveCardsToLSHand(luke);
        scn.StartGame();

        assertInZone(Zone.SIDE_OF_TABLE, objective);
        assertInZone(Zone.LOCATIONS, endor, bunker, platform);
        assertFalse(objective.isFlipped());

        scn.DSActivateForceCheat(16);
        scn.SkipToPhase(Phase.DEPLOY);

        scn.DSDeployCardAndPassResponses(stormtrooper, bunker);
        assertFalse(objective.isFlipped());
        scn.LSPass();

        scn.DSDeployCardAndPassResponses(ominousRumors, endor);
        assertTrue(scn.IsAttachedTo(endor, ominousRumors));
        assertFalse("One required Effect must not flip Endor Operations",
                objective.isFlipped());
        scn.LSPass();

        scn.DSDeployCardAndPassResponses(establishSecretBase, bunker);
        assertTrue(scn.IsAttachedTo(bunker, establishSecretBase));
        assertTrue("Both real required Effects must fire the objective's front-side trigger",
                objective.isFlipped());
        scn.LSPass();

        // The Bunker has no Light Force icon, so direct Light deployment is
        // illegal without prior presence. Inject only the opponent-control
        // state, including removal of Dark presence, then make a legal
        // deployment create a real table-change event.
        scn.MoveCardsToLocation(bunker, luke);
        scn.MoveOutOfPlay(stormtrooper);
        assertTrue(GameConditions.controls(scn.game(), VirtualTableScenario.LS, bunker));
        scn.DSDeployCardAndPassResponses(tableChangeTrigger, scn.GetLSStartingLocation());

        assertInZone(Zone.USED_PILE, establishSecretBase);
        assertFalse("Establish Secret Base leaving table must fire the real flip-back trigger",
                objective.isFlipped());
    }

    @Test
    public void blowingAwayBunkerPlacesEndorOperationsOutOfPlay() {
        var scn = scenario();
        var objective = scn.GetDSCard("objective");
        var bunker = scn.GetDSCard("bunker");
        var luke = scn.GetLSCard("luke");
        var deactivateShieldGenerator = scn.GetLSCard("deactivateShieldGenerator");

        scn.MoveCardsToLSHand(deactivateShieldGenerator);
        scn.StartGame();
        scn.MoveCardsToLocation(bunker, luke);
        assertTrue(GameConditions.controls(scn.game(), VirtualTableScenario.LS, bunker));

        scn.SkipToLSTurn(Phase.DEPLOY);
        scn.LSDeployCardAndPassResponses(deactivateShieldGenerator, bunker);
        assertTrue(scn.IsAttachedTo(bunker, deactivateShieldGenerator));

        scn.SkipToLSTurn(Phase.CONTROL);
        assertTrue(scn.LSCardActionAvailable(deactivateShieldGenerator,
                "Attempt to 'blow away' Bunker"));
        scn.PrepareLSDestiny(6);
        scn.PrepareLSDestiny(7);
        int darkLifeBeforeBlowAway = scn.GetDSLifeForceRemaining();
        int darkLostPileBeforeBlowAway = scn.GetDSLostPileCount();
        scn.LSUseCardAction(deactivateShieldGenerator,
                "Attempt to 'blow away' Bunker");
        scn.PassAllResponses();
        scn.DSPayRemainingForceLossFromReserveDeck();
        scn.PassAllResponses();

        assertEquals("Deactivate The Shield Generator must cause exactly 8 Force loss",
                8, darkLifeBeforeBlowAway - scn.GetDSLifeForceRemaining());
        assertEquals("Paying the blow-away loss from Reserve Deck must lose exactly 8 cards",
                8, scn.GetDSLostPileCount() - darkLostPileBeforeBlowAway);
        assertTrue("A destiny total of 13 must actually blow away Bunker",
                bunker.isBlownAway());
        assertInZone(Zone.OUT_OF_PLAY, objective);
    }

    @Test
    public void controlPhaseTutorTakesOneRequiredEffectFromReserveDeck() {
        var scn = scenario();
        var objective = scn.GetDSCard("objective");
        var ominousRumors = scn.GetDSCard("ominousRumors");

        scn.StartGame();
        scn.MoveCardsToBottomOfDSReserveDeck(ominousRumors);
        scn.SkipToPhase(Phase.CONTROL);

        assertTrue(scn.DSCardActionAvailable(objective, "Take card from Reserve Deck"));
        scn.DSUseCardAction(objective, "Take card from Reserve Deck");
        scn.DSChooseCard(ominousRumors);
        scn.PassAllResponses();

        assertInZone(Zone.HAND, ominousRumors);
        scn.LSPass();
        assertFalse("The objective's tutor must be limited to once per control phase",
                scn.DSCardActionAvailable(objective, "Take card from Reserve Deck"));
    }

    @Test
    public void baseOminousRumorsCancelsAtThreeOpponentControlledEndorSites() {
        var scn = scenario();
        var endor = scn.GetDSCard("endor");
        var bunker = scn.GetDSCard("bunker");
        var platform = scn.GetDSCard("platform");
        var forestClearing = scn.GetDSCard("forestClearing");
        var ominousRumors = scn.GetDSCard("ominousRumors");
        var tableChangeTrigger = scn.GetDSCard("tableChangeTrigger");

        scn.MoveCardsToDSHand(ominousRumors, tableChangeTrigger);
        scn.StartGame();
        scn.MoveLocationToTable(forestClearing);
        scn.DSActivateForceCheat(8);
        scn.SkipToPhase(Phase.DEPLOY);

        scn.DSDeployCardAndPassResponses(ominousRumors, endor);
        assertTrue(scn.IsAttachedTo(endor, ominousRumors));
        scn.LSPass();

        scn.MoveCardsToLocation(bunker, scn.GetLSFiller(1));
        scn.MoveCardsToLocation(platform, scn.GetLSFiller(2));
        scn.MoveCardsToLocation(forestClearing, scn.GetLSFiller(3));
        assertTrue(GameConditions.controls(
                scn.game(), VirtualTableScenario.LS, bunker));
        assertTrue(GameConditions.controls(
                scn.game(), VirtualTableScenario.LS, platform));
        assertTrue(GameConditions.controls(
                scn.game(), VirtualTableScenario.LS, forestClearing));

        // Direct fixture movement does not emit a table-change result.
        scn.DSDeployCardAndPassResponses(
                tableChangeTrigger, scn.GetLSStartingLocation());
        if (scn.DSDecisionAvailable("Choose card to cancel")) {
            scn.DSChooseCard(ominousRumors);
            scn.PassAllResponses();
        }

        assertInZone(Zone.LOST_PILE, ominousRumors);
    }

    @Test
    public void baseEstablishSecretBaseRequiresThreeBikerScoutControlledEndorSites() {
        var scn = scenario();
        var endor = scn.GetDSCard("endor");
        var bunker = scn.GetDSCard("bunker");
        var platform = scn.GetDSCard("platform");
        var forestClearing = scn.GetDSCard("forestClearing");
        var bikerScout1 = scn.GetDSCard("bikerScout1");
        var bikerScout2 = scn.GetDSCard("bikerScout2");
        var bikerScout3 = scn.GetDSCard("bikerScout3");
        var tableChangeTrigger = scn.GetDSCard("tableChangeTrigger");
        var establishSecretBase = scn.GetDSCard("baseEstablishSecretBase");

        scn.MoveCardsToDSHand(tableChangeTrigger, establishSecretBase);
        scn.StartGame();
        scn.MoveLocationToTable(forestClearing);
        scn.MoveCardsToLocation(bunker, bikerScout1);
        scn.MoveCardsToLocation(platform, bikerScout2);

        assertTrue(GameConditions.controls(scn.game(), VirtualTableScenario.DS, bunker));
        assertTrue(GameConditions.controls(scn.game(), VirtualTableScenario.DS, platform));

        scn.DSActivateForceCheat(8);
        scn.SkipToPhase(Phase.DEPLOY);
        assertFalse("Two biker scout-controlled Endor sites must not satisfy 8_124",
                scn.DSDeployAvailable(establishSecretBase));

        scn.DSDeployCardAndPassResponses(tableChangeTrigger, scn.GetLSStartingLocation());
        scn.MoveCardsToLocation(forestClearing, bikerScout3);
        assertTrue(GameConditions.controls(scn.game(), VirtualTableScenario.DS,
                forestClearing));
        assertTrue(GameConditions.controlsWith(scn.game(), establishSecretBase,
                VirtualTableScenario.DS, 3, Filters.Endor_site, Filters.biker_scout));
        scn.LSPass();

        assertTrue("The third biker scout-controlled Endor site must unlock 8_124",
                scn.DSDeployAvailable(establishSecretBase));
        scn.DSDeployCardAndPassResponses(establishSecretBase, endor);
        assertTrue(scn.IsAttachedTo(endor, establishSecretBase));
    }

    @Test
    public void virtualOminousRumorsDeploysOnlyOnBunker() {
        var scn = scenario();
        var endor = scn.GetDSCard("endor");
        var bunker = scn.GetDSCard("bunker");
        var platform = scn.GetDSCard("platform");
        var ominousRumors = scn.GetDSCard("ominousRumorsV");

        scn.MoveCardsToDSHand(ominousRumors);
        scn.StartGame();
        scn.DSActivateForceCheat(8);
        scn.SkipToPhase(Phase.DEPLOY);

        assertTrue(scn.DSDeployAvailable(ominousRumors));
        scn.DSDeployCard(ominousRumors);
        assertTrue(scn.DSDecisionAvailable("Choose where to deploy")
                || scn.DSDecisionAvailable("Choose location where to deploy"));
        assertTrue(scn.DSHasCardChoiceAvailable(bunker));
        assertFalse(scn.DSHasCardChoiceAvailable(endor));
        assertFalse(scn.DSHasCardChoiceAvailable(platform));
        scn.DSChooseCard(bunker);
        scn.PassAllResponses();

        assertTrue(scn.IsAttachedTo(bunker, ominousRumors));
    }

    @Test
    public void legacyOminousRumorsDeploysOnlyOnBunker() {
        var scn = scenario();
        var endor = scn.GetDSCard("endor");
        var bunker = scn.GetDSCard("bunker");
        var platform = scn.GetDSCard("platform");
        var ominousRumors = scn.GetDSCard("ominousRumorsLegacy");

        scn.MoveCardsToDSHand(ominousRumors);
        scn.StartGame();
        scn.DSActivateForceCheat(8);
        scn.SkipToPhase(Phase.DEPLOY);

        assertTrue(scn.DSDeployAvailable(ominousRumors));
        scn.DSDeployCard(ominousRumors);
        assertTrue(scn.DSDecisionAvailable("Choose where to deploy")
                || scn.DSDecisionAvailable("Choose location where to deploy"));
        assertTrue(scn.DSHasCardChoiceAvailable(bunker));
        assertFalse(scn.DSHasCardChoiceAvailable(endor));
        assertFalse(scn.DSHasCardChoiceAvailable(platform));
        scn.DSChooseCard(bunker);
        scn.PassAllResponses();

        assertTrue(scn.IsAttachedTo(bunker, ominousRumors));
    }

    @Test
    public void legacyEstablishSecretBaseRequiresBunkerControlAndDeploysOnEndorSystem() {
        var scn = scenario();
        var endor = scn.GetDSCard("endor");
        var bunker = scn.GetDSCard("bunker");
        var platform = scn.GetDSCard("platform");
        var stormtrooper = scn.GetDSCard("stormtrooper");
        var tableChangeTrigger = scn.GetDSCard("tableChangeTrigger");
        var establishSecretBase = scn.GetDSCard("legacyEstablishSecretBase");

        scn.MoveCardsToDSHand(tableChangeTrigger, establishSecretBase);
        scn.StartGame();
        scn.DSActivateForceCheat(8);
        scn.SkipToPhase(Phase.DEPLOY);

        assertFalse("Bunker control is required to deploy 601_260",
                scn.DSDeployAvailable(establishSecretBase));

        scn.DSDeployCardAndPassResponses(
                tableChangeTrigger, scn.GetLSStartingLocation());
        scn.MoveCardsToLocation(bunker, stormtrooper);
        assertTrue(GameConditions.controls(
                scn.game(), VirtualTableScenario.DS, bunker));
        scn.LSPass();

        assertTrue("Controlling Bunker must unlock 601_260",
                scn.DSDeployAvailable(establishSecretBase));
        scn.DSDeployCard(establishSecretBase);
        assertTrue(scn.DSDecisionAvailable("Choose where to deploy")
                || scn.DSDecisionAvailable("Choose location where to deploy"));
        assertTrue(scn.DSHasCardChoiceAvailable(endor));
        assertFalse(scn.DSHasCardChoiceAvailable(bunker));
        assertFalse(scn.DSHasCardChoiceAvailable(platform));
        scn.DSChooseCard(endor);
        scn.PassAllResponses();

        assertTrue(scn.IsAttachedTo(endor, establishSecretBase));
    }

    @Test
    public void baseEstablishSecretBaseAcceptsThreePilotedAtStControlledEndorSites() {
        var scn = scenario();
        var endor = scn.GetDSCard("endor");
        var bunker = scn.GetDSCard("bunker");
        var platform = scn.GetDSCard("platform");
        var forestClearing = scn.GetDSCard("forestClearing");
        var tableChangeTrigger = scn.GetDSCard("tableChangeTrigger");
        var establishSecretBase = scn.GetDSCard("baseEstablishSecretBase");
        var tempestScout = scn.GetDSCard("tempestScout");
        var tempestScout1 = scn.GetDSCard("tempestScout1");
        var tempestScout2 = scn.GetDSCard("tempestScout2");
        var atstPilot1 = scn.GetDSCard("atstPilot1");
        var atstPilot2 = scn.GetDSCard("atstPilot2");

        scn.MoveCardsToDSHand(tableChangeTrigger, establishSecretBase);
        scn.StartGame();
        scn.MoveLocationToTable(forestClearing);
        scn.MoveCardsToLocation(bunker, tempestScout);
        scn.MoveCardsToLocation(platform, tempestScout1);
        scn.MoveCardsToLocation(forestClearing, tempestScout2);

        scn.DSActivateForceCheat(8);
        scn.SkipToPhase(Phase.DEPLOY);
        assertFalse("Only one piloted AT-ST must not satisfy 8_124",
                scn.DSDeployAvailable(establishSecretBase));

        scn.DSDeployCardAndPassResponses(
                tableChangeTrigger, scn.GetLSStartingLocation());
        scn.BoardAsPilot(tempestScout1, atstPilot1);
        scn.BoardAsPilot(tempestScout2, atstPilot2);
        scn.LSPass();

        assertTrue(GameConditions.controlsWith(
                scn.game(), establishSecretBase,
                VirtualTableScenario.DS, 3,
                Filters.Endor_site,
                Filters.and(Filters.piloted, Filters.AT_ST)));
        assertTrue("Three piloted AT-ST-controlled Endor sites must unlock 8_124",
                scn.DSDeployAvailable(establishSecretBase));
        scn.DSDeployCardAndPassResponses(establishSecretBase, endor);
        assertTrue(scn.IsAttachedTo(endor, establishSecretBase));
    }

    @Test
    public void baseEstablishSecretBaseGoesUsedWhenOpponentControlsEndorSystem() {
        var scn = scenario();
        var endor = scn.GetDSCard("endor");
        var bunker = scn.GetDSCard("bunker");
        var platform = scn.GetDSCard("platform");
        var forestClearing = scn.GetDSCard("forestClearing");
        var bikerScout1 = scn.GetDSCard("bikerScout1");
        var bikerScout2 = scn.GetDSCard("bikerScout2");
        var bikerScout3 = scn.GetDSCard("bikerScout3");
        var establishSecretBase = scn.GetDSCard("baseEstablishSecretBase");
        var tableChangeTrigger = scn.GetDSCard("tableChangeTrigger");
        var tableChangeTrigger2 = scn.GetDSCard("tableChangeTrigger2");
        var xwing = scn.GetLSCard("xwing");

        scn.MoveCardsToDSHand(
                establishSecretBase,
                tableChangeTrigger,
                tableChangeTrigger2);
        scn.StartGame();
        scn.MoveLocationToTable(forestClearing);
        scn.MoveCardsToLocation(bunker, bikerScout1);
        scn.MoveCardsToLocation(platform, bikerScout2);
        scn.MoveCardsToLocation(forestClearing, bikerScout3);
        assertTrue(GameConditions.controlsWith(
                scn.game(), establishSecretBase,
                VirtualTableScenario.DS, 3,
                Filters.Endor_site, Filters.biker_scout));

        scn.DSActivateForceCheat(8);
        scn.SkipToPhase(Phase.DEPLOY);
        scn.DSDeployCardAndPassResponses(establishSecretBase, endor);
        assertTrue(scn.IsAttachedTo(endor, establishSecretBase));
        scn.LSPass();

        scn.MoveOutOfPlay(bikerScout3);
        assertFalse(GameConditions.controlsWith(
                scn.game(), establishSecretBase,
                VirtualTableScenario.DS, 3,
                Filters.Endor_site,
                Filters.biker_scout));
        scn.DSDeployCardAndPassResponses(
                tableChangeTrigger,
                scn.GetLSStartingLocation());
        assertTrue("8_124 must remain after its deploy qualification later drops below three sites",
                scn.IsAttachedTo(endor, establishSecretBase));
        scn.LSPass();

        scn.MoveCardsToLocation(endor, xwing);
        assertTrue(GameConditions.controls(
                scn.game(), VirtualTableScenario.LS, endor));

        // Emit the real table-change trigger after the injected control state.
        scn.DSDeployCardAndPassResponses(
                tableChangeTrigger2, scn.GetLSStartingLocation());

        assertInZone(Zone.USED_PILE, establishSecretBase);
    }

    @Test
    public void virtualEstablishSecretBaseGoesUsedWhenOpponentControlsBunker() {
        var scn = scenario();
        var bunker = scn.GetDSCard("bunker");
        var stormtrooper = scn.GetDSCard("stormtrooper");
        var tableChangeTrigger = scn.GetDSCard("tableChangeTrigger");
        var tableChangeTrigger2 = scn.GetDSCard("tableChangeTrigger2");
        var establishSecretBase = scn.GetDSCard("establishSecretBase");
        var luke = scn.GetLSCard("luke");

        scn.MoveCardsToDSHand(
                stormtrooper,
                tableChangeTrigger,
                tableChangeTrigger2,
                establishSecretBase);
        scn.StartGame();
        scn.MoveCardsToLocation(bunker, stormtrooper);
        assertTrue(GameConditions.controls(
                scn.game(), VirtualTableScenario.DS, bunker));

        scn.DSActivateForceCheat(8);
        scn.SkipToPhase(Phase.DEPLOY);
        scn.DSDeployCardAndPassResponses(
                establishSecretBase, bunker);
        assertTrue(scn.IsAttachedTo(
                bunker, establishSecretBase));
        scn.LSPass();

        scn.MoveOutOfPlay(stormtrooper);
        assertFalse(GameConditions.controls(
                scn.game(), VirtualTableScenario.DS, bunker));
        assertFalse(GameConditions.controls(
                scn.game(), VirtualTableScenario.LS, bunker));
        scn.DSDeployCardAndPassResponses(
                tableChangeTrigger,
                scn.GetLSStartingLocation());
        assertTrue("207_25 must remain while Bunker is neutral",
                scn.IsAttachedTo(
                        bunker, establishSecretBase));
        scn.LSPass();

        scn.MoveCardsToLocation(bunker, luke);
        assertTrue(GameConditions.controls(
                scn.game(), VirtualTableScenario.LS, bunker));

        // Emit the real table-change trigger after the injected control state.
        scn.DSDeployCardAndPassResponses(
                tableChangeTrigger2, scn.GetLSStartingLocation());

        assertInZone(Zone.USED_PILE, establishSecretBase);
    }

    @Test
    public void legacyEstablishSecretBaseRemainsWhenOpponentControlsBunker() {
        var scn = scenario();
        var endor = scn.GetDSCard("endor");
        var bunker = scn.GetDSCard("bunker");
        var stormtrooper = scn.GetDSCard("stormtrooper");
        var tableChangeTrigger = scn.GetDSCard("tableChangeTrigger");
        var establishSecretBase =
                scn.GetDSCard("legacyEstablishSecretBase");
        var luke = scn.GetLSCard("luke");

        scn.MoveCardsToDSHand(
                establishSecretBase,
                tableChangeTrigger);
        scn.StartGame();
        scn.MoveCardsToLocation(
                bunker, stormtrooper);
        assertTrue(GameConditions.controls(
                scn.game(), VirtualTableScenario.DS,
                bunker));

        scn.DSActivateForceCheat(8);
        scn.SkipToPhase(Phase.DEPLOY);
        scn.DSDeployCardAndPassResponses(
                establishSecretBase, endor);
        assertTrue(scn.IsAttachedTo(
                endor, establishSecretBase));
        scn.LSPass();

        scn.MoveCardsToLocation(bunker, luke);
        scn.MoveOutOfPlay(stormtrooper);
        assertTrue(GameConditions.controls(
                scn.game(), VirtualTableScenario.LS,
                bunker));
        scn.DSDeployCardAndPassResponses(
                tableChangeTrigger,
                scn.GetLSStartingLocation());

        assertTrue("601_260 has no post-deploy Bunker-control removal text",
                scn.IsAttachedTo(
                        endor, establishSecretBase));
    }

    @Test
    public void virtualOminousRumorsRemainsAtThreeOpponentControlledSites() {
        var scn = scenario();
        var bunker = scn.GetDSCard("bunker");
        var platform = scn.GetDSCard("platform");
        var forestClearing =
                scn.GetDSCard("forestClearing");
        var darkForest = scn.GetDSCard("darkForest");
        var ominousRumors =
                scn.GetDSCard("ominousRumorsV");
        var tableChangeTrigger =
                scn.GetDSCard("tableChangeTrigger");

        scn.MoveCardsToDSHand(
                ominousRumors, tableChangeTrigger);
        scn.StartGame();
        scn.MoveLocationToTable(forestClearing);
        scn.MoveLocationToTable(darkForest);
        scn.DSActivateForceCheat(8);
        scn.SkipToPhase(Phase.DEPLOY);
        scn.DSDeployCardAndPassResponses(
                ominousRumors, bunker);
        assertTrue(scn.IsAttachedTo(
                bunker, ominousRumors));
        scn.LSPass();

        scn.MoveCardsToLocation(
                platform, scn.GetLSFiller(1));
        scn.MoveCardsToLocation(
                forestClearing, scn.GetLSFiller(2));
        scn.MoveCardsToLocation(
                darkForest, scn.GetLSFiller(3));
        assertTrue(GameConditions.controls(
                scn.game(), VirtualTableScenario.LS,
                platform));
        assertTrue(GameConditions.controls(
                scn.game(), VirtualTableScenario.LS,
                forestClearing));
        assertTrue(GameConditions.controls(
                scn.game(), VirtualTableScenario.LS,
                darkForest));

        scn.DSDeployCardAndPassResponses(
                tableChangeTrigger,
                scn.GetLSStartingLocation());

        assertTrue("223_19 has no three-site cancellation text",
                scn.IsAttachedTo(
                        bunker, ominousRumors));
    }

    @Test
    public void cancelableOminousPrintingsSurviveTwoAndCancelAtThreeSites() {
        assertRumorsSurvivesTwoAndCancelsAtThree(
                "ominousRumors", true);
        assertRumorsSurvivesTwoAndCancelsAtThree(
                "ominousRumorsLegacy", false);
    }

    private void assertRumorsSurvivesTwoAndCancelsAtThree(
            String cardKey, boolean deployOnEndor) {
        var scn = scenario();
        var endor = scn.GetDSCard("endor");
        var bunker = scn.GetDSCard("bunker");
        var platform = scn.GetDSCard("platform");
        var forestClearing =
                scn.GetDSCard("forestClearing");
        var ominousRumors = scn.GetDSCard(cardKey);
        var tableChangeTrigger =
                scn.GetDSCard("tableChangeTrigger");
        var tableChangeTrigger2 =
                scn.GetDSCard("tableChangeTrigger2");

        scn.MoveCardsToDSHand(
                ominousRumors,
                tableChangeTrigger,
                tableChangeTrigger2);
        scn.StartGame();
        scn.MoveLocationToTable(forestClearing);
        scn.DSActivateForceCheat(8);
        scn.SkipToPhase(Phase.DEPLOY);
        scn.DSDeployCardAndPassResponses(
                ominousRumors,
                deployOnEndor ? endor : bunker);
        assertTrue(scn.IsAttachedTo(
                deployOnEndor ? endor : bunker,
                ominousRumors));
        scn.LSPass();

        scn.MoveCardsToLocation(
                bunker, scn.GetLSFiller(1));
        scn.MoveCardsToLocation(
                platform, scn.GetLSFiller(2));
        scn.DSDeployCardAndPassResponses(
                tableChangeTrigger,
                scn.GetLSStartingLocation());
        assertTrue(cardKey + " must survive at two opponent-controlled Endor sites",
                scn.IsAttachedTo(
                        deployOnEndor ? endor : bunker,
                        ominousRumors));
        scn.LSPass();

        scn.MoveCardsToLocation(
                forestClearing, scn.GetLSFiller(3));
        scn.DSDeployCardAndPassResponses(
                tableChangeTrigger2,
                scn.GetLSStartingLocation());
        if (scn.DSDecisionAvailable(
                "Choose card to cancel")) {
            scn.DSChooseCard(ominousRumors);
            scn.PassAllResponses();
        }

        assertInZone(Zone.LOST_PILE, ominousRumors);
    }

    @Test
    public void realDyerPreventsThreeSiteOminousRumorsCancellation() {
        var scn = scenario();
        var endor = scn.GetDSCard("endor");
        var bunker = scn.GetDSCard("bunker");
        var platform = scn.GetDSCard("platform");
        var forestClearing =
                scn.GetDSCard("forestClearing");
        var darkForest = scn.GetDSCard("darkForest");
        var ominousRumors =
                scn.GetDSCard("ominousRumors");
        var dyer = scn.GetDSCard("dyer");
        var tableChangeTrigger =
                scn.GetDSCard("tableChangeTrigger");

        scn.MoveCardsToDSHand(
                ominousRumors, tableChangeTrigger);
        scn.StartGame();
        scn.MoveLocationToTable(forestClearing);
        scn.MoveLocationToTable(darkForest);
        scn.MoveCardsToLocation(bunker, dyer);
        scn.DSActivateForceCheat(8);
        scn.SkipToPhase(Phase.DEPLOY);
        scn.DSDeployCardAndPassResponses(
                ominousRumors, endor);
        assertTrue(scn.IsAttachedTo(
                endor, ominousRumors));
        assertFalse(GameConditions.canBeCanceled(
                scn.game(), ominousRumors));
        scn.LSPass();

        scn.MoveCardsToLocation(
                platform, scn.GetLSFiller(1));
        scn.MoveCardsToLocation(
                forestClearing, scn.GetLSFiller(2));
        scn.MoveCardsToLocation(
                darkForest, scn.GetLSFiller(3));
        scn.DSDeployCardAndPassResponses(
                tableChangeTrigger,
                scn.GetLSStartingLocation());

        assertFalse(GameConditions.canBeCanceled(
                scn.game(), ominousRumors));
        assertTrue("Colonel Dyer's real modifier must prevent the three-site cancellation",
                scn.IsAttachedTo(
                        endor, ominousRumors));
    }

    @Test
    public void realReactorCoreSuspendsRumorsAndFlipsObjectiveFront() {
        var scn = scenario();
        var objective = scn.GetDSCard("objective");
        var endor = scn.GetDSCard("endor");
        var bunker = scn.GetDSCard("bunker");
        var stormtrooper =
                scn.GetDSCard("stormtrooper");
        var tableChangeTrigger =
                scn.GetDSCard("tableChangeTrigger");
        var ominousRumors =
                scn.GetDSCard("ominousRumors");
        var establishSecretBase =
                scn.GetDSCard("establishSecretBase");
        var reactorCore =
                scn.GetDSCard("reactorCore");

        scn.MoveCardsToDSHand(
                stormtrooper,
                tableChangeTrigger,
                ominousRumors,
                establishSecretBase);
        scn.StartGame();
        scn.DSActivateForceCheat(16);
        scn.SkipToPhase(Phase.DEPLOY);
        scn.DSDeployCardAndPassResponses(
                stormtrooper, bunker);
        scn.LSPass();
        scn.DSDeployCardAndPassResponses(
                ominousRumors, endor);
        scn.LSPass();
        scn.DSDeployCardAndPassResponses(
                establishSecretBase, bunker);
        assertTrue(objective.isFlipped());
        assertTrue(scn.IsCardActive(ominousRumors));
        scn.LSPass();

        scn.MoveLocationToTable(reactorCore);
        scn.DSDeployCardAndPassResponses(
                tableChangeTrigger,
                scn.GetLSStartingLocation());

        assertFalse("Reactor Core's real Dark Side text must suspend Ominous Rumors",
                scn.IsCardActive(ominousRumors));
        assertFalse("Suspended Ominous Rumors no longer satisfies Imperial Outpost",
                objective.isFlipped());
    }
}
