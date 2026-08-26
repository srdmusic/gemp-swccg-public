package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.ai.models.common.phase.ForceLossFacts;
import com.gempukku.swccgo.ai.models.common.phase.ForceLossPolicy;
import com.gempukku.swccgo.ai.models.common.phase.SetYourCourseObjectivePolicy;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperationKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.framework.StartingSetup;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.PhysicalCardImpl;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Set Your Course For Alderaan / The Ultimate Power In The Universe
 * (111_6, DARK; no twin printing). Card Java is unchanged.
 *
 * The front flips only when Alderaan is blown away. The complete public-bot
 * contract therefore covers the objective pull, compatible Superlaser
 * deployment, movement and control Force reserves, battle restraint, exact
 * 0 to 1 to 2 Death Star movement, Alderaan orbit selection, route-card
 * retention during real Force loss, native Commence Primary Ignition, and
 * the unchanged objective's actual flip trigger. The back never flips by its
 * own text. Its Death Star blown-away out-of-play rule includes the native
 * Attack Run at the Trench threat relationship. The external An Inkling route
 * is outside this contract.
 */
public class SetYourCourseObjectiveEngineContractTest {

    private static final StartingSetup SYCFA = new StartingSetup() {
        @Override
        public HashMap<String, String> Cards() {
            return new HashMap<>() {{
                put("objective", "111_6");
                put("deathStar", "2_143");
                put("alderaan", "1_281");
                put("db327", "1_285");
            }};
        }

        @Override
        public void Setup(VirtualTableScenario scn) {
            // Three required free deploys: Death Star system (parsec 0),
            // the Deployable_By_SYCFA slot (DARK Alderaan here), and
            // Docking Bay 327.
            for (int i = 0; i < 10; i++) {
                if (scn.DSDecisionAvailable("On which side")) {
                    scn.DSChoose("Left");
                } else if (scn.DSDecisionAvailable(
                        "next to (or convert)")) {
                    scn.DSChooseCard(scn.GetDSCard("deathStar"));
                } else if (scn.DSDecisionAvailable("to deploy")
                        && scn.DSHasCardChoiceAvailable(
                            scn.GetDSCard("deathStar"))) {
                    scn.DSChooseCard(scn.GetDSCard("deathStar"));
                } else if (scn.DSDecisionAvailable("to deploy")
                        && scn.DSHasCardChoiceAvailable(
                            scn.GetDSCard("alderaan"))) {
                    scn.DSChooseCard(scn.GetDSCard("alderaan"));
                } else if (scn.DSDecisionAvailable("to deploy")
                        && scn.DSHasCardChoiceAvailable(
                            scn.GetDSCard("db327"))) {
                    scn.DSChooseCard(scn.GetDSCard("db327"));
                }
            }
        }
    };

    private static final StartingSetup SYCFA_OPEN_ALDERAAN_OR_JEDHA =
            new StartingSetup() {
        @Override
        public HashMap<String, String> Cards() {
            return new HashMap<>() {{
                put("objective", "111_6");
            }};
        }

        @Override
        public void Setup(VirtualTableScenario scn) {
            if (scn.DSDecisionAvailable("On which side")) {
                scn.DSChoose("Left");
            }
        }
    };

    private VirtualTableScenario sycfaScenario() {
        return new VirtualTableScenario(
                new HashMap<>() {{
                    put("battleOpponent", "1_19");
                    put("alderaanConverted", "1_121");
                    put("trench", "2_62");
                    put("attackRun", "2_42");
                }},
                new HashMap<>() {{
                    put("superlaser", "2_161");
                    put("virtualSuperlaser", "216_19");
                    put("cpi", "2_130");
                    put("cpiV", "209_45");
                    put("battleOrder", "13_54");
                    put("centralCore", "1_283");
                    put("kiffex", "2_147");
                    put("offPlanTrooper", "1_194");
                    put("battleBody", "1_179");
                }},
                24,
                24,
                StartingSetup.DefaultLSGroundLocation,
                SYCFA,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open
        );
    }

    private record PublicBots(
            com.gempukku.swccgo.ai.models.rando.RandoCalAi rando,
            com.gempukku.swccgo.ai.models.chosenone.TheChosenOneAi chosen) {
        private static PublicBots forGame(VirtualTableScenario scn) {
            var rando = new com.gempukku.swccgo.ai.models.rando.RandoCalAi();
            var chosen = new com.gempukku.swccgo.ai.models.chosenone
                    .TheChosenOneAi();
            rando.setGame(scn.game());
            chosen.setGame(scn.game());
            return new PublicBots(rando, chosen);
        }

        private String decideBoth(VirtualTableScenario scn) {
            AwaitingDecision decision =
                    scn.GetAwaitingDecision(VirtualTableScenario.DS);
            assertNotNull("Expected Dark Side decision", decision);
            String randoResponse = rando.decide(
                    VirtualTableScenario.DS,
                    decision, scn.gameState());
            String chosenResponse = chosen.decide(
                    VirtualTableScenario.DS,
                    decision, scn.gameState());
            assertEquals("Rando/Chosen parity for " + decision.getText(),
                    randoResponse, chosenResponse);
            return randoResponse;
        }
    }

    private static void chooseResultBoth(
            VirtualTableScenario scn, PublicBots bots,
            String textFragment, String expectedResult) {
        AwaitingDecision decision =
                scn.GetAwaitingDecision(VirtualTableScenario.DS);
        assertNotNull(decision);
        assertTrue(decision.getText().contains(textFragment));
        String response = bots.decideBoth(scn);
        String[] results =
                decision.getDecisionParameters().get("results");
        assertNotNull(results);
        int index = Integer.parseInt(response);
        assertTrue(index >= 0 && index < results.length);
        assertEquals(expectedResult, results[index]);
        scn.DSDecided(response);
    }

    private static void chooseReserveBlueprintBoth(
            VirtualTableScenario scn, PublicBots bots,
            String expectedBlueprint) {
        AwaitingDecision decision =
                scn.GetAwaitingDecision(VirtualTableScenario.DS);
        assertNotNull(decision);
        String response = bots.decideBoth(scn);
        String[] cardIds =
                decision.getDecisionParameters().get("cardId");
        String[] blueprints =
                decision.getDecisionParameters().get("blueprintId");
        assertNotNull(cardIds);
        assertNotNull(blueprints);
        int index = Arrays.asList(cardIds).indexOf(response);
        assertTrue(index >= 0);
        assertEquals(expectedBlueprint, blueprints[index]);
        scn.DSDecided(response);
    }

    private static void keepOnlyDarkHandCards(
            VirtualTableScenario scn, PhysicalCard... kept) {
        List<PhysicalCard> keep = List.of(kept);
        for (PhysicalCard card : new ArrayList<>(
                scn.gameState().getHand(VirtualTableScenario.DS))) {
            if (!keep.contains(card)) {
                scn.MoveCardsToBottomOfDSReserveDeck(
                        (PhysicalCardImpl) card);
            }
        }
    }

    private static String actionIdFor(
            AwaitingDecision decision, PhysicalCard card,
            String text) {
        String[] actionIds = decision.getDecisionParameters()
                .get("actionId");
        String[] actionTexts = decision.getDecisionParameters()
                .get("actionText");
        String[] cardIds = decision.getDecisionParameters()
                .get("cardId");
        for (int i = 0; i < actionIds.length; i++) {
            if (Integer.toString(card.getCardId()).equals(cardIds[i])
                    && actionTexts[i].contains(text)) {
                return actionIds[i];
            }
        }
        return null;
    }

    private static void moveDeathStarBoth(
            VirtualTableScenario scn, PublicBots bots,
            PhysicalCardImpl deathStar, String parsec,
            PhysicalCardImpl orbitTarget,
            PhysicalCardImpl orbitDecoy) {
        String moveAction = scn.GetCardActionId(
                VirtualTableScenario.DS, deathStar,
                "Move using hyperspeed");
        assertNotNull(moveAction);
        assertEquals(moveAction, bots.decideBoth(scn));
        scn.DSDecided(moveAction);
        chooseResultBoth(scn, bots,
                "Choose parsec to move to", parsec);
        if (orbitTarget != null) {
            chooseResultBoth(scn, bots,
                    "Choose destination for", "Orbit a system");
            assertTrue(scn.DSHasCardChoiceAvailable(orbitTarget));
            assertTrue(scn.DSHasCardChoiceAvailable(orbitDecoy));
            String orbitChoice = bots.decideBoth(scn);
            assertEquals(Integer.toString(orbitTarget.getCardId()),
                    orbitChoice);
            scn.DSDecided(orbitChoice);
        }
        scn.PassAllResponses();
    }

    private void moveSiteToSystem(
            VirtualTableScenario scn, PhysicalCardImpl site, String system) {
        scn.RemoveCardZone(site);
        var placements = scn.gameState().getLocationPlacement(
                scn.game(), site, system, null);
        assertFalse("Expected a legal placement at " + system,
                placements.isEmpty());
        scn.gameState().addLocationToTable(
                scn.game(), site, placements.getFirst());
    }

    @Test
    public void publicBotsChooseAlderaanOverJedhaCityDuringClassicSetup() {
        var scn = new VirtualTableScenario(
                new HashMap<>(),
                new HashMap<>() {{
                    put("deathStar", "2_143");
                    put("alderaan", "1_281");
                    put("jedhaCity", "209_49");
                    put("db327", "1_285");
                    put("superlaser", "2_161");
                    put("cpi", "2_130");
                }},
                24,
                24,
                StartingSetup.DefaultLSGroundLocation,
                SYCFA_OPEN_ALDERAAN_OR_JEDHA,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open);

        // Put the strategic decoy first so candidate order cannot prove the
        // route accidentally.
        scn.MoveCardsToTopOfDSReserveDeck(
                scn.GetDSCard("alderaan"),
                scn.GetDSCard("jedhaCity"));
        scn.StartGame(false);
        PublicBots bots = PublicBots.forGame(scn);

        assertTrue(scn.DSDecisionAvailable("location to deploy"));
        assertTrue(scn.DSHasCardChoiceAvailable(scn.GetDSCard("alderaan")));
        assertTrue(scn.DSHasCardChoiceAvailable(scn.GetDSCard("jedhaCity")));
        AwaitingDecision choice = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        String response = bots.decideBoth(scn);
        int selectedIndex = Arrays.asList(
                choice.getDecisionParameters().get("cardId"))
                .indexOf(response);
        assertTrue(selectedIndex >= 0);
        assertEquals("The classic CPI route requires Alderaan on table",
                "1_281",
                choice.getDecisionParameters().get("blueprintId")
                        [selectedIndex]);
    }

    @Test
    public void sycfaFrontFlipsWhenAlderaanIsBlownAwayByCommencePrimaryIgnition() {
        var scn = sycfaScenario();
        var objective = scn.GetDSCard("objective");
        var deathStar = scn.GetDSCard("deathStar");
        var alderaan = scn.GetDSCard("alderaan");
        var superlaser = scn.GetDSCard("superlaser");
        var cpi = scn.GetDSCard("cpi");
        var centralCore = scn.GetDSCard("centralCore");

        scn.MoveCardsToDSHand(superlaser, cpi);
        scn.StartGame();
        assertFalse(objective.isFlipped());
        // Second opponent-free Death Star site: Y = 2, so a prepared
        // destiny of 7 makes (7 + 2 - 0) > 8. X = 0 (Alderaan has no
        // sites), and with no Rebel Base on table Z = 0.
        moveSiteToSystem(scn, centralCore, Title.Death_Star);
        // Raw orbit injection: the CPI availability check only reads the
        // mobile system's orbit string; the real epic event then runs
        // natively end to end.
        deathStar.setSystemOrbited(Title.Alderaan);

        scn.DSActivateForceCheat(12);
        scn.SkipToPhase(Phase.DEPLOY);
        scn.DSDeployCardAndPassResponses(superlaser, deathStar);
        assertTrue(scn.IsAttachedTo(deathStar, superlaser));

        scn.SkipToDSTurn(Phase.CONTROL);
        scn.PrepareDSDestiny(7);
        scn.DSPlayCardAndPassResponses(cpi, "Attempt to 'blow away'");
        scn.PassAllResponses();

        assertTrue("A CPI total of 9 must actually blow away Alderaan",
                alderaan.isBlownAway());
        assertTrue("Alderaan blown away must flip the front natively",
                objective.isFlipped());
    }

    @Test
    public void convertedAlderaanRemainsTheSourceTrueOrbitTarget() {
        var scn = sycfaScenario();
        var deathStar = scn.GetDSCard("deathStar");
        var superlaser = scn.GetDSCard("superlaser");
        var convertedAlderaan = scn.GetLSCard("alderaanConverted");

        scn.StartGame();
        scn.AttachCardsTo(deathStar, superlaser);
        deathStar.setParsec(1);
        scn.MoveLocationToTable(convertedAlderaan);

        var randoAnalyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer();
        randoAnalyzer.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);
        var chosenAnalyzer =
                new com.gempukku.swccgo.ai.models.chosenone.strategy
                    .ObjectiveAnalyzer();
        chosenAnalyzer.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);

        assertEquals(SetYourCourseObjectivePolicy.Stage.READY_AT_ONE,
                randoAnalyzer.getSetYourCourseRouteStage(
                        scn.game(), VirtualTableScenario.DS));
        assertEquals(SetYourCourseObjectivePolicy.Stage.READY_AT_ONE,
                chosenAnalyzer.getSetYourCourseRouteStage(
                        scn.game(), VirtualTableScenario.DS));
        assertTrue(randoAnalyzer.isSetYourCourseAlderaanOrbitCandidate(
                scn.game(), VirtualTableScenario.DS,
                convertedAlderaan));
        assertTrue(chosenAnalyzer.isSetYourCourseAlderaanOrbitCandidate(
                scn.game(), VirtualTableScenario.DS,
                convertedAlderaan));
    }

    @Test
    public void publicBotsUseVirtualSuperlaserToRecoverTheClassicRouteAwayFromZero() {
        var scn = sycfaScenario();
        var deathStar = scn.GetDSCard("deathStar");
        var classicLaser = scn.GetDSCard("superlaser");
        var virtualLaser = scn.GetDSCard("virtualSuperlaser");
        var cpi = scn.GetDSCard("cpi");
        var centralCore = scn.GetDSCard("centralCore");

        scn.MoveCardsToTopOfDSLostPile(classicLaser, centralCore);
        scn.MoveCardsToDSHand(virtualLaser, cpi);
        scn.StartGame();
        keepOnlyDarkHandCards(scn, virtualLaser, cpi);
        deathStar.setParsec(1);
        scn.SkipToPhase(Phase.DEPLOY);
        while (scn.GetDSForcePileCount() > 1) {
            scn.MoveCardsToTopOfDSUsedPile(
                    scn.GetTopOfDSForcePile());
        }
        if (scn.GetDSForcePileCount() < 1) {
            scn.DSActivateForceCheat(1);
        }
        if (scn.AwaitingLSDeployPhaseActions()) scn.LSPass();

        PublicBots bots = PublicBots.forGame(scn);
        var analyzer = new com.gempukku.swccgo.ai.models.rando
                .strategy.ObjectiveAnalyzer();
        analyzer.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertEquals(
                com.gempukku.swccgo.ai.models.common.phase
                    .SetYourCourseObjectivePolicy.Stage
                    .WAITING_FOR_SUPERLASER,
                analyzer.getSetYourCourseRouteStage(
                    scn.game(), VirtualTableScenario.DS));

        AwaitingDecision deployDecision = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        String deploy = actionIdFor(
                deployDecision, virtualLaser, "Deploy");
        assertNotNull("Superlaser (V) must be legally offered at parsec 1",
                deploy);
        assertEquals(deploy, bots.decideBoth(scn));
        scn.DSDecided(deploy);
        assertEquals(Integer.toString(deathStar.getCardId()),
                bots.decideBoth(scn));
        scn.DSDecided(Integer.toString(deathStar.getCardId()));
        scn.PassAllResponses();

        assertTrue(scn.IsAttachedTo(deathStar, virtualLaser));
        analyzer.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertEquals(
                com.gempukku.swccgo.ai.models.common.phase
                    .SetYourCourseObjectivePolicy.Stage.READY_AT_ONE,
                analyzer.getSetYourCourseRouteStage(
                    scn.game(), VirtualTableScenario.DS));
    }

    @Test
    public void cpiVirtualDoesNotMasqueradeAsTheAlderaanFlipRoute() {
        var scn = sycfaScenario();
        var classicCpi = scn.GetDSCard("cpi");
        var cpiV = scn.GetDSCard("cpiV");
        var superlaser = scn.GetDSCard("superlaser");

        scn.MoveCardsToTopOfDSLostPile(classicCpi);
        scn.MoveCardsToDSHand(cpiV, superlaser);
        scn.StartGame();

        var analyzer = new com.gempukku.swccgo.ai.models.rando
                .strategy.ObjectiveAnalyzer();
        analyzer.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertEquals(
                com.gempukku.swccgo.ai.models.common.phase
                    .SetYourCourseObjectivePolicy.Stage
                    .INACTIVE_OR_UNSUPPORTED,
                analyzer.getSetYourCourseRouteStage(
                    scn.game(), VirtualTableScenario.DS));
        assertFalse(analyzer
                .isSetYourCourseCompatibleSuperlaserDeployCandidate(
                    scn.game(), VirtualTableScenario.DS, superlaser));
    }

    @Test
    public void publicBotsExecuteCompleteClassicAlderaanRouteAndFlipNatively() {
        var scn = sycfaScenario();
        var objective = scn.GetDSCard("objective");
        var deathStar = scn.GetDSCard("deathStar");
        var alderaan = scn.GetDSCard("alderaan");
        var db327 = scn.GetDSCard("db327");
        var centralCore = scn.GetDSCard("centralCore");
        var superlaser = scn.GetDSCard("superlaser");
        var cpi = scn.GetDSCard("cpi");
        var battleOrder = scn.GetDSCard("battleOrder");
        var kiffex = scn.GetDSCard("kiffex");
        var trooper = scn.GetDSCard("offPlanTrooper");
        var battleBody = scn.GetDSCard("battleBody");
        var battleOpponent = scn.GetLSCard("battleOpponent");

        scn.MoveCardsToDSHand(superlaser, cpi, trooper);
        scn.StartGame();
        keepOnlyDarkHandCards(scn, superlaser, cpi, trooper);
        scn.MoveCardsToBottomOfDSReserveDeck(centralCore);
        scn.MoveLocationToTable(kiffex);
        PublicBots bots = PublicBots.forGame(scn);
        assertFalse(objective.isFlipped());
        var routeAnalyzer = new com.gempukku.swccgo.ai.models.rando
                .strategy.ObjectiveAnalyzer();
        routeAnalyzer.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertEquals(
                com.gempukku.swccgo.ai.models.common.phase
                    .SetYourCourseObjectivePolicy.Stage
                    .WAITING_FOR_SUPERLASER,
                routeAnalyzer.getSetYourCourseRouteStage(
                    scn.game(), VirtualTableScenario.DS));
        assertTrue(routeAnalyzer.isSetYourCourseDeathStarSitePullAction(
                scn.game(), VirtualTableScenario.DS,
                objective, "Take card into hand from Reserve Deck"));

        scn.SkipToPhase(Phase.DEPLOY);
        while (scn.GetDSForcePileCount() > 0) {
            scn.MoveCardsToTopOfDSUsedPile(
                    scn.GetTopOfDSForcePile());
        }
        scn.MoveCardsToBottomOfDSReserveDeck(battleBody);
        scn.DSActivateForceCheat(1);
        // Put the preferred route card on top only after activation. Native
        // search decisions expose that card as TOP_OF_RESERVE_DECK, and the
        // public bots must still recognize it without activating it away.
        scn.MoveCardsToTopOfDSReserveDeck(centralCore);
        if (scn.AwaitingLSDeployPhaseActions()) scn.LSPass();

        String pull = scn.GetCardActionId(
                VirtualTableScenario.DS, objective,
                "Take card into hand from Reserve Deck");
        assertNotNull(pull);
        AwaitingDecision pullDecision = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        assertEquals("texts=" + Arrays.toString(
                    pullDecision.getDecisionParameters().get("actionText"))
                + " ids=" + Arrays.toString(
                    pullDecision.getDecisionParameters().get("actionId"))
                + " cards=" + Arrays.toString(
                    pullDecision.getDecisionParameters().get("cardId"))
                + " bps=" + Arrays.toString(
                    pullDecision.getDecisionParameters().get("blueprintId")),
                pull, bots.decideBoth(scn));
        scn.DSDecided(pull);
        assertEquals("The engine exposes the searched top card through its transient zone",
                Zone.TOP_OF_RESERVE_DECK, centralCore.getZone());
        chooseReserveBlueprintBoth(scn, bots, "1_283");
        scn.PassAllResponses();
        scn.LSPass();
        assertEquals(Zone.HAND, centralCore.getZone());

        boolean coreDeployed = false;
        boolean laserDeployed = false;
        for (int guard = 0; guard < 2; guard++) {
            AwaitingDecision deployDecision = scn.GetAwaitingDecision(
                    VirtualTableScenario.DS);
            assertNotNull(deployDecision);
            String coreAction = centralCore.getZone() == Zone.HAND
                    ? actionIdFor(
                        deployDecision, centralCore, "Deploy")
                    : null;
            String laserAction = superlaser.getZone() == Zone.HAND
                    ? actionIdFor(
                        deployDecision, superlaser, "Deploy")
                    : null;
            String response = bots.decideBoth(scn);
            if (response.equals(coreAction)) {
                scn.DSDecided(response);
                if (scn.DSDecisionAvailable("next to (or convert)")) {
                    scn.DSChooseCard(db327);
                }
                if (scn.DSDecisionAvailable("On which side")) {
                    scn.DSChoose("Left");
                }
                scn.PassAllResponses();
                scn.LSPass();
                coreDeployed = true;
            } else if (response.equals(laserAction)) {
                scn.DSDecided(response);
                String target = bots.decideBoth(scn);
                assertEquals(Integer.toString(deathStar.getCardId()),
                        target);
                scn.DSDecided(target);
                scn.PassAllResponses();
                scn.LSPass();
                laserDeployed = true;
            } else {
                fail("Both bots abandoned a required SYC route deploy: "
                        + response + "; core=" + coreAction
                        + "; laser=" + laserAction
                        + "; texts=" + Arrays.toString(
                            deployDecision.getDecisionParameters()
                                .get("actionText"))
                        + "; ids=" + Arrays.toString(
                            deployDecision.getDecisionParameters()
                                .get("actionId"))
                        + "; cards=" + Arrays.toString(
                            deployDecision.getDecisionParameters()
                                .get("cardId")));
            }
        }

        assertTrue(coreDeployed);
        assertTrue(laserDeployed);
        assertTrue(scn.IsAttachedTo(deathStar, superlaser));
        assertEquals(1, scn.GetDSForcePileCount());

        String offPlan = scn.GetCardActionId(
                VirtualTableScenario.DS, trooper, "Deploy");
        assertNotNull("The one-Force distraction must really be offered",
                offPlan);
        assertEquals("Both bots must preserve the hyperspeed payment",
                "", bots.decideBoth(scn));

        var battleSite = scn.GetLSStartingLocation();
        scn.MoveCardsToLocation(battleSite,
                battleBody, battleOpponent);
        assertTrue("Dark Side must have battle presence at "
                + battleSite.getTitle(),
                scn.game().getModifiersQuerying().hasPresenceAt(
                    scn.gameState(), VirtualTableScenario.DS,
                    battleSite, true, VirtualTableScenario.DS, null));
        assertTrue("Light Side must have battle presence at "
                + battleSite.getTitle(),
                scn.game().getModifiersQuerying().hasPresenceAt(
                    scn.gameState(), VirtualTableScenario.LS,
                    battleSite, true, VirtualTableScenario.DS, null));
        scn.DSDecided("");
        scn.LSPass();
        assertEquals(Phase.BATTLE, scn.gameState().getCurrentPhase());
        AwaitingDecision battleDecision = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        assertNotNull("Expected Dark Side battle-phase decision",
                battleDecision);
        String offRouteBattle = scn.GetCardActionId(
                VirtualTableScenario.DS, battleSite,
                "Initiate battle");
        assertNotNull("A legal off-route one-Force battle must be offered; decision="
                + battleDecision.getText() + "; type="
                + battleDecision.getDecisionType() + "; darkZone="
                + battleBody.getZone() + "; lightZone="
                + battleOpponent.getZone() + "; force="
                + scn.GetDSForcePileCount() + "; cost="
                + scn.game().getModifiersQuerying()
                    .getInitiateBattleCost(
                        scn.gameState(), battleSite,
                        VirtualTableScenario.DS, true)
                + "; current=" + scn.gameState().getCurrentPlayerId()
                + "; usable="
                + com.gempukku.swccgo.cards.GameConditions
                    .forceAvailableToUse(
                        scn.game(), VirtualTableScenario.DS)
                + "; legal="
                + com.gempukku.swccgo.cards.GameConditions
                    .canInitiateBattleAtLocation(
                        VirtualTableScenario.DS, scn.game(),
                        battleSite)
                + "; texts="
                + Arrays.toString(battleDecision.getDecisionParameters()
                    .get("actionText"))
                + "; ids=" + Arrays.toString(
                    battleDecision.getDecisionParameters().get("actionId"))
                + "; cards=" + Arrays.toString(
                    battleDecision.getDecisionParameters().get("cardId")),
                offRouteBattle);
        assertEquals("Both bots must save the last Force for hyperspeed",
                "", bots.decideBoth(scn));
        scn.DSDecided("");
        scn.LSPass();

        assertEquals(Phase.MOVE, scn.gameState().getCurrentPhase());
        moveDeathStarBoth(scn, bots, deathStar, "1", null, null);
        assertEquals(1, deathStar.getParsec());
        assertEquals(null, deathStar.getSystemOrbited());
        assertEquals(0, scn.GetDSForcePileCount());

        scn.MoveCardsToTopOfLSLostPile(battleOpponent);
        scn.MoveCardsToTopOfDSLostPile(trooper);
        scn.MoveCardsToDSSideOfTable(battleOrder);
        scn.SkipToDSTurn(Phase.CONTROL);
        while (scn.GetDSForcePileCount() > 3) {
            scn.MoveCardsToTopOfDSUsedPile(
                    scn.GetTopOfDSForcePile());
        }
        if (scn.GetDSForcePileCount() < 3) {
            scn.DSActivateForceCheat(
                    3 - scn.GetDSForcePileCount());
        }
        assertEquals(3.0f,
                scn.game().getModifiersQuerying()
                    .getInitiateForceDrainCost(
                        scn.gameState(), battleSite,
                        VirtualTableScenario.DS),
                0.0f);
        String taxedDrain = scn.GetCardActionId(
                VirtualTableScenario.DS, battleSite,
                "Force drain");
        assertNotNull("The Battle Order drain must really be offered",
                taxedDrain);
        assertEquals("Both bots must preserve the next hyperspeed payment during control",
                "", bots.decideBoth(scn));
        scn.DSDecided("");
        scn.LSPass();
        assertEquals(Phase.DEPLOY,
                scn.gameState().getCurrentPhase());
        scn.DSPass();
        scn.LSPass();
        assertEquals(Phase.BATTLE,
                scn.gameState().getCurrentPhase());
        scn.DSPass();
        scn.LSPass();
        assertEquals(Phase.MOVE,
                scn.gameState().getCurrentPhase());
        moveDeathStarBoth(
                scn, bots, deathStar, "2", alderaan, kiffex);
        assertEquals(2, deathStar.getParsec());
        assertEquals(Title.Alderaan, deathStar.getSystemOrbited());
        assertEquals(2, scn.GetDSForcePileCount());

        scn.SkipToDSTurn(Phase.CONTROL);
        scn.PrepareDSDestiny(7);
        String ignition = scn.GetCardActionId(
                VirtualTableScenario.DS, cpi,
                "Attempt to 'blow away' Alderaan");
        assertNotNull(ignition);
        assertEquals(ignition, bots.decideBoth(scn));
        scn.DSDecided(ignition);
        scn.PassAllResponses();

        assertTrue(alderaan.isBlownAway());
        assertTrue(objective.isFlipped());
        var postFlipAnalyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer();
        postFlipAnalyzer.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertTrue("The back-side Death Star loss rule arms only after the native flip",
                postFlipAnalyzer.isObjectiveHardLossLocation(
                    scn.game(), VirtualTableScenario.DS, deathStar));
        var trench = scn.GetLSCard("trench");
        var attackRun = scn.GetLSCard("attackRun");
        scn.MoveLocationToTable(trench);
        scn.MoveCardsToLocation(trench, attackRun);
        assertTrue("Native Attack Run at the Trench must arm Death Star defense",
                postFlipAnalyzer.isObjectiveHardLossDefenseLocation(
                    scn.game(), VirtualTableScenario.DS, deathStar));
    }

    @Test
    public void manualForceLossProgressionPreservesTheArmedRouteAfterPublicBotObservation() {
        var scn = sycfaScenario();
        var superlaser = scn.GetDSCard("superlaser");
        var virtualSuperlaser = scn.GetDSCard("virtualSuperlaser");
        var cpi = scn.GetDSCard("cpi");
        var centralCore = scn.GetDSCard("centralCore");
        var routeForce = scn.GetDSFiller(1);
        var fodder = scn.GetDSCard("offPlanTrooper");
        var drainBody = scn.GetLSCard("battleOpponent");

        scn.StartGame();
        scn.MoveCardsToTopOfDSLostPile(virtualSuperlaser);
        scn.MoveCardsToLocation(scn.GetLSStartingLocation(), drainBody);
        scn.LSActivateForceCheat(4);
        scn.SkipToLSTurn(Phase.CONTROL);
        while (scn.GetDSForcePileCount() > 0) {
            scn.MoveCardsToTopOfDSUsedPile(
                    scn.GetTopOfDSForcePile());
        }
        scn.MoveCardsToBottomOfDSReserveDeck(cpi, centralCore);
        scn.MoveCardsToDSHand(superlaser);
        scn.MoveCardsToTopOfDSForcePile(routeForce);
        scn.MoveCardsToTopOfDSReserveDeck(fodder);

        PublicBots bots = PublicBots.forGame(scn);
        scn.LSForceDrainAt(scn.GetLSStartingLocation());
        scn.PassAllResponses();

        assertTrue(scn.DSDecisionAvailable("Choose Force to lose"));
        assertTrue(scn.DSHasCardChoiceAvailable(superlaser));
        assertTrue(scn.DSHasCardChoiceAvailable(routeForce));
        assertTrue(scn.DSHasCardChoiceAvailable(fodder));
        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertTrue(analyzer.isPreferredSetYourCourseForceLossCandidate(
                scn.game(), VirtualTableScenario.DS, superlaser));
        assertTrue(analyzer
                .isSetYourCourseMovementForceLossReserveCandidate(
                    scn.game(), VirtualTableScenario.DS, routeForce));
        for (PhysicalCard protectedCard : List.of(
                superlaser, routeForce)) {
            var retention = ForceLossPolicy.score(
                    Integer.toString(protectedCard.getCardId()),
                    ForceLossPolicy.Route.STANDALONE,
                    ForceLossFacts.readDecision(
                            scn.gameState(), VirtualTableScenario.DS,
                            scn.gameState().getPlayersLatestTurnNumber(
                                VirtualTableScenario.DS)),
                    ForceLossFacts.readCandidate(
                            scn.gameState(), VirtualTableScenario.DS,
                            protectedCard),
                    new ForceLossPolicy.ObjectiveFlags(
                            false, false, true, false));
            assertTrue("Expected bounded objective retention for "
                            + protectedCard.getTitle(),
                    retention.operations().stream().anyMatch(operation ->
                            operation.domainId()
                                == TraceDomainId.OBJECTIVE_INTENT
                                    && operation.delta() == -300.0f));
            assertFalse(retention.operations().stream().anyMatch(operation ->
                    operation.kind() == PolicyOperationKind.HARD_VETO));
        }
        AwaitingDecision lossDecision = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        String publicLoss = bots.decideBoth(scn);
        assertTrue("The mirrored public-bot observation must be an offered card",
                Arrays.asList(lossDecision.getDecisionParameters()
                        .get("cardId")).contains(publicLoss));
        String fodderLoss = Integer.toString(fodder.getCardId());
        assertTrue("The harness must continue with offered fodder",
                Arrays.asList(lossDecision.getDecisionParameters()
                        .get("cardId")).contains(fodderLoss));
        scn.DSDecided(fodderLoss);
        scn.PassAllResponses();

        assertEquals(Zone.HAND, superlaser.getZone());
        assertTrue(routeForce.getZone() == Zone.FORCE_PILE
                || routeForce.getZone() == Zone.TOP_OF_FORCE_PILE);
        assertTrue(fodder.getZone() == Zone.LOST_PILE
                || fodder.getZone() == Zone.TOP_OF_LOST_PILE);
    }

    @Test
    public void publicBotsLoseFodderBeforeClassicAssetsOrLastMoveForce() {
        var scn = sycfaScenario();
        var superlaser = scn.GetDSCard("superlaser");
        var cpi = scn.GetDSCard("cpi");
        var centralCore = scn.GetDSCard("centralCore");
        var routeForce = scn.GetDSFiller(1);
        var fodder = scn.GetDSCard("offPlanTrooper");

        scn.MoveCardsToDSHand(superlaser, cpi);
        scn.StartGame();
        keepOnlyDarkHandCards(scn, superlaser, cpi);
        while (scn.GetDSForcePileCount() > 0) {
            scn.MoveCardsToTopOfDSUsedPile(
                    scn.GetTopOfDSForcePile());
        }
        scn.MoveCardsToTopOfDSForcePile(routeForce);
        scn.MoveCardsToBottomOfDSReserveDeck(fodder);

        var randoAnalyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer();
        randoAnalyzer.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);
        var chosenAnalyzer =
                new com.gempukku.swccgo.ai.models.chosenone.strategy
                    .ObjectiveAnalyzer();
        chosenAnalyzer.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertTrue("stage=" + randoAnalyzer.getSetYourCourseRouteStage(
                    scn.game(), VirtualTableScenario.DS)
                + "; zone=" + routeForce.getZone()
                + "; force=" + scn.GetDSForcePileCount()
                + "; reserve="
                + randoAnalyzer.getSetYourCourseNextRouteForceReserve(
                    scn.game(), VirtualTableScenario.DS),
                randoAnalyzer
                    .isSetYourCourseMovementForceLossReserveCandidate(
                        scn.game(), VirtualTableScenario.DS, routeForce));

        List<PhysicalCard> cards = List.of(
                superlaser, cpi, centralCore, routeForce, fodder);
        List<String> cardIds = cards.stream()
                .map(card -> Integer.toString(card.getCardId()))
                .toList();
        List<String> blueprints = cards.stream()
                .map(card -> card.getBlueprintId(true)).toList();
        List<String> titles = cards.stream()
                .map(PhysicalCard::getTitle).toList();

        var randoContext =
                new com.gempukku.swccgo.ai.models.rando.evaluators
                    .DecisionContext(
                        scn.gameState(), VirtualTableScenario.DS,
                        "CARD_SELECTION", "Choose Force to lose",
                        "syc-force-loss", Phase.BATTLE);
        randoContext.setGame(scn.game());
        randoContext.setSide(Side.DARK);
        randoContext.setObjectiveAnalyzer(randoAnalyzer);
        randoContext.setCardIds(cardIds);
        randoContext.setBlueprints(blueprints);
        randoContext.setTestingTexts(titles);
        randoContext.setSelectable(
                List.of(true, true, true, true, true));
        randoContext.setNoPass(true);
        randoContext.setMin(1);
        randoContext.setMax(1);

        var chosenContext =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators
                    .DecisionContext(
                        scn.gameState(), VirtualTableScenario.DS,
                        "CARD_SELECTION", "Choose Force to lose",
                        "syc-force-loss", Phase.BATTLE);
        chosenContext.setGame(scn.game());
        chosenContext.setSide(Side.DARK);
        chosenContext.setObjectiveAnalyzer(chosenAnalyzer);
        chosenContext.setCardIds(cardIds);
        chosenContext.setBlueprints(blueprints);
        chosenContext.setTestingTexts(titles);
        chosenContext.setSelectable(
                List.of(true, true, true, true, true));
        chosenContext.setNoPass(true);
        chosenContext.setMin(1);
        chosenContext.setMax(1);

        var randoCandidates =
                new com.gempukku.swccgo.ai.models.rando.evaluators
                    .CardSelectionEvaluator().evaluate(randoContext);
        var chosenCandidates =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators
                    .CardSelectionEvaluator().evaluate(chosenContext);
        for (PhysicalCard protectedCard : List.of(
                superlaser, cpi, centralCore, routeForce)) {
            String protectedId = Integer.toString(
                    protectedCard.getCardId());
            var randoProtected = randoCandidates.stream()
                    .filter(candidate -> protectedId.equals(
                        candidate.getActionId()))
                    .findFirst().orElseThrow();
            var chosenProtected = chosenCandidates.stream()
                    .filter(candidate -> protectedId.equals(
                        candidate.getActionId()))
                    .findFirst().orElseThrow();
            assertEquals(randoProtected.getScore(),
                    chosenProtected.getScore(), 0.0f);
            assertEquals(randoProtected.getReasoning(),
                    chosenProtected.getReasoning());
            assertTrue(randoProtected.getReasoning().stream()
                    .anyMatch(reason -> reason.contains(
                        "OBJECTIVE CRITICAL")));
        }

        var randoWinner =
                new com.gempukku.swccgo.ai.models.rando.evaluators
                    .CombinedEvaluator().evaluateDecision(randoContext);
        var chosenWinner =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators
                    .CombinedEvaluator().evaluateDecision(chosenContext);
        assertNotNull(randoWinner);
        assertNotNull(chosenWinner);
        assertEquals(Integer.toString(fodder.getCardId()),
                randoWinner.getActionId());
        assertEquals(randoWinner.getActionId(),
                chosenWinner.getActionId());
    }

    @Test
    public void sycfaProfileEncodesTheHardLossAndDeliberatelyNoFlipRules() {
        var scn = sycfaScenario();
        var deathStar = scn.GetDSCard("deathStar");
        var alderaan = scn.GetDSCard("alderaan");

        scn.StartGame();

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertTrue("Profile must hydrate for 111_6", analyzer.isAnalyzed());

        var preFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.DS, "preFlip", "flip");
        assertTrue("SYCFA deliberately encodes NO flip rules — the flip is the deferred blown-away primitive",
                preFlip.isEmpty());

        assertFalse("The back-only Death Star loss rule must not leak onto the front",
                analyzer.isObjectiveHardLossLocation(
                        scn.game(), VirtualTableScenario.DS, deathStar));
        assertFalse("Alderaan must not be a hard-loss location",
                analyzer.isObjectiveHardLossLocation(
                        scn.game(), VirtualTableScenario.DS, alderaan));
    }
}
