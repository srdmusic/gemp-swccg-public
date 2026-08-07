package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.ai.models.common.phase.AiActionSourceProvenance;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.framework.StartingSetup;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import org.junit.Test;

import java.util.HashMap;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/** Native engine and public-bot contract for Agents Of Black Sun (10_29). */
public class AgentsOfBlackSunObjectiveEngineContractTest {

    private static final StartingSetup AGENTS_OF_BLACK_SUN =
            new StartingSetup() {
        @Override
        public HashMap<String, String> Cards() {
            return new HashMap<>() {{
                put("objective", "10_29");
                put("imperialCity", "7_277");
                put("xizor", "10_45");
                put("coruscant", "7_275");
            }};
        }

        @Override
        public void Setup(VirtualTableScenario scn) {
            for (int guard = 0; guard < 20; guard++) {
                if (scn.DSHasCardChoiceAvailable(
                        scn.GetDSCard("imperialCity"))) {
                    scn.DSChooseCard(scn.GetDSCard("imperialCity"));
                } else if (scn.DSHasCardChoiceAvailable(
                        scn.GetDSCard("xizor"))) {
                    scn.DSChooseCard(scn.GetDSCard("xizor"));
                } else if (scn.DSHasCardChoiceAvailable(
                        scn.GetDSCard("coruscant"))) {
                    scn.DSChooseCard(scn.GetDSCard("coruscant"));
                } else if (scn.DSDecisionAvailable("On which side")) {
                    scn.DSChoose("Left");
                } else {
                    break;
                }
            }
        }
    };

    private VirtualTableScenario scenario() {
        return new VirtualTableScenario(
                new HashMap<>() {{
                    put("bountyTarget", "1_28");
                    put("secondBountyTarget", "1_28");
                }},
                new HashMap<>() {{
                    put("palace", "203_32");
                    put("dockingBay", "12_166");
                    put("carrier", "3_155");
                    put("snoova", "10_48");
                    put("bounty", "5_113");
                    put("secondBounty", "5_113");
                }},
                24,
                24,
                StartingSetup.DefaultLSGroundLocation,
                AGENTS_OF_BLACK_SUN,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open);
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
            AwaitingDecision decision = scn.GetAwaitingDecision(
                    VirtualTableScenario.DS);
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

    @Test
    public void publicBotsDeployPalaceMoveXizorAndNativeFlip() {
        var scn = scenario();
        var objective = scn.GetDSCard("objective");
        var imperialCity = scn.GetDSCard("imperialCity");
        var xizor = scn.GetDSCard("xizor");
        var palace = scn.GetDSCard("palace");
        scn.MoveCardsToDSHand(palace);
        scn.StartGame();

        assertSame(imperialCity,
                scn.game().getModifiersQuerying()
                    .getLocationThatCardIsAt(scn.gameState(), xizor));
        assertFalse(objective.isFlipped());

        scn.DSActivateForceCheat(12);
        scn.SkipToDSTurn(Phase.DEPLOY);
        var bots = PublicBots.forGame(scn);
        AwaitingDecision deployDecision = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        String deploy = bots.decideBoth(scn);
        assertSame("The authored Coruscant battleground route must deploy Palace",
                palace, AiActionSourceProvenance.selectedActionSource(
                        deployDecision, deploy));
        scn.DSDecided(deploy);
        if (scn.DSDecisionAvailable("On which side")) {
            scn.DSChoose("Left");
        }
        scn.PassAllResponses();
        assertEquals(Zone.LOCATIONS, palace.getZone());

        var routeAnalyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer();
        routeAnalyzer.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertEquals("Xizor's Palace makes this Black Sun Agent move free",
                0, routeAnalyzer
                    .getAgentsOfBlackSunCurrentMoveForceReserve(
                        scn.game(), VirtualTableScenario.DS, null));

        scn.SkipToPhase(Phase.MOVE);
        assertTrue(scn.AwaitingDSMovePhaseActions());
        AwaitingDecision moveDecision = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        String move = bots.decideBoth(scn);
        assertSame("Xizor must take the real landspeed action",
                xizor, AiActionSourceProvenance.selectedActionSource(
                        moveDecision, move));
        scn.DSDecided(move);
        assertEquals("The destination decision must finish at Xizor's Palace",
                Integer.toString(palace.getCardId()),
                bots.decideBoth(scn));
        scn.DSDecided(Integer.toString(palace.getCardId()));
        scn.PassAllResponses();

        assertSame(palace,
                scn.game().getModifiersQuerying()
                    .getLocationThatCardIsAt(scn.gameState(), xizor));
        assertTrue("The unchanged objective Java must fire the real flip",
                objective.isFlipped());
    }

    @Test
    public void publicBotsMoveCarrierWithXizorAboardAndNativeFlip() {
        var scn = scenario();
        var objective = scn.GetDSCard("objective");
        var imperialCity = scn.GetDSCard("imperialCity");
        var xizor = scn.GetDSCard("xizor");
        var palace = scn.GetDSCard("palace");
        var carrier = scn.GetDSCard("carrier");
        scn.StartGame();
        scn.MoveLocationToTable(palace);
        scn.MoveCardsToLocation(imperialCity, carrier);
        scn.BoardAsPassenger(carrier, xizor);
        assertTrue(scn.IsAboardAsPassenger(carrier, xizor));
        assertFalse(objective.isFlipped());

        scn.DSActivateForceCheat(2);
        scn.SkipToDSTurn(Phase.MOVE);
        var analyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer();
        analyzer.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertTrue("The carrier projects its aboard objective actor",
                analyzer.advancesPreFlipActorAtRuntimeLocation(
                        scn.game(), VirtualTableScenario.DS,
                        carrier, palace));
        assertEquals("Reserve the carrier's exact one-Force move",
                1, analyzer.getAgentsOfBlackSunCurrentMoveForceReserve(
                        scn.game(), VirtualTableScenario.DS, null));

        var bots = PublicBots.forGame(scn);
        AwaitingDecision moveDecision = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        String move = bots.decideBoth(scn);
        assertSame("The moving root is the carrier, not its passenger",
                carrier, AiActionSourceProvenance.selectedActionSource(
                        moveDecision, move));
        scn.DSDecided(move);
        assertEquals(Integer.toString(palace.getCardId()),
                bots.decideBoth(scn));
        scn.DSDecided(Integer.toString(palace.getCardId()));
        scn.PassAllResponses();

        assertSame(palace,
                scn.game().getModifiersQuerying()
                    .getLocationThatCardIsAt(scn.gameState(), carrier));
        assertSame(palace,
                scn.game().getModifiersQuerying()
                    .getLocationThatCardIsAt(scn.gameState(), xizor));
        assertTrue("Native at-semantics must flip with Xizor aboard",
                objective.isFlipped());
    }

    @Test
    public void publicBotsCompleteTheSourceDefinedPaidBountyMoveChain() {
        var scn = scenario();
        var objective = scn.GetDSCard("objective");
        var imperialCity = scn.GetDSCard("imperialCity");
        var palace = scn.GetDSCard("palace");
        var dockingBay = scn.GetDSCard("dockingBay");
        var snoova = scn.GetDSCard("snoova");
        var bounty = scn.GetDSCard("bounty");
        var secondBounty = scn.GetDSCard("secondBounty");
        var bountyTarget = scn.GetLSCard("bountyTarget");
        var secondBountyTarget = scn.GetLSCard("secondBountyTarget");
        scn.StartGame();
        scn.MoveLocationToTable(palace);
        scn.MoveLocationToTable(dockingBay);
        var routeSites = new ArrayList<>(List.of(
                imperialCity, palace, dockingBay));
        routeSites.sort(Comparator.comparingInt(
                scn.gameState().getLocationsInOrder()::indexOf));
        var firstDestination = routeSites.get(0);
        var origin = routeSites.get(1);
        var secondDestination = routeSites.get(2);
        scn.MoveCardsToLocation(origin, snoova);
        scn.MoveCardsToLocation(firstDestination, bountyTarget);
        scn.MoveCardsToLocation(secondDestination, secondBountyTarget);
        scn.AttachCardsTo(bountyTarget, bounty);
        scn.AttachCardsTo(secondBountyTarget, secondBounty);
        scn.DSActivateForceCheat(1);
        scn.SkipToDSTurn(Phase.CONTROL);
        var bots = PublicBots.forGame(scn);
        var routeAnalyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer();
        routeAnalyzer.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertTrue("The native offered route must also pass formation safety",
                routeAnalyzer.hasSafeAgentsOfBlackSunBountyMoveRoute(
                        scn.game(), VirtualTableScenario.DS));
        assertTrue("Snoova must be a safe objective mover",
                routeAnalyzer.isSafeAgentsOfBlackSunBountyMover(
                        scn.game(), VirtualTableScenario.DS, snoova));

        AwaitingDecision parentDecision = null;
        String parent = null;
        PhysicalCard selectedParentSource = null;
        for (int drainGuard = 0; drainGuard < 4; drainGuard++) {
            parentDecision = scn.GetAwaitingDecision(
                    VirtualTableScenario.DS);
            parent = bots.decideBoth(scn);
            selectedParentSource =
                    AiActionSourceProvenance.selectedActionSource(
                        parentDecision, parent);
            if (selectedParentSource == objective) break;
            assertTrue("Only an affordable site drain may precede the move",
                    routeSites.contains(selectedParentSource));
            scn.DSDecided(parent);
            scn.PassForceDrainStartResponses();
            if (scn.GetForceDrainRemaining() > 0) {
                scn.LSPayRemainingForceLossFromReserveDeck();
            }
            scn.PassForceDrainEndResponses();
            if (scn.AwaitingLSControlPhaseActions()) {
                scn.LSPass();
            }
            assertTrue(scn.AwaitingDSControlPhaseActions());
        }
        assertSame("The objective must own the paid bounty-move action; selected="
                        + (selectedParentSource != null
                            ? selectedParentSource.getTitle() : "none")
                        + "; actions="
                        + Arrays.toString(parentDecision
                            .getDecisionParameters().get("actionText")),
                objective, selectedParentSource);
        int forceBeforeMove = scn.GetDSForcePileCount();
        scn.DSDecided(parent);

        assertTrue(scn.DSDecisionAvailable(
                "Choose bounty hunter to move"));
        assertTrue("The exact objective action must remain active through targeting",
                routeAnalyzer.isActiveAgentsOfBlackSunBountyMoveAction(
                        scn.game(), VirtualTableScenario.DS));
        AwaitingDecision hunterDecision = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        assertEquals("CARD_SELECTION",
                hunterDecision.getDecisionType().name());
        assertEquals("Choose bounty hunter to move, or click 'Done' to cancel",
                hunterDecision.getText());
        var hunterContext = new com.gempukku.swccgo.ai.models.rando
                .evaluators.DecisionContext(
                    scn.gameState(), VirtualTableScenario.DS,
                    hunterDecision.getDecisionType().name(),
                    hunterDecision.getText(), "aobs-hunter",
                    Phase.CONTROL);
        hunterContext.setGame(scn.game());
        hunterContext.setSide(Side.DARK);
        hunterContext.setObjectiveAnalyzer(routeAnalyzer);
        hunterContext.setCardIds(Arrays.asList(
                hunterDecision.getDecisionParameters().get("cardId")));
        hunterContext.setBlueprints(java.util.List.of());
        hunterContext.setSelectable(java.util.Collections.nCopies(
                hunterContext.getCardIds().size(), true));
        var hunterCandidates = new com.gempukku.swccgo.ai.models.rando
                .evaluators.CardSelectionEvaluator()
                .evaluate(hunterContext);
        assertEquals(1, hunterCandidates.size());
        assertFalse(hunterCandidates.getFirst().getReasoningString(),
                hunterCandidates.getFirst().isHardVetoed());
        assertTrue(hunterCandidates.getFirst().getReasoningString(),
                hunterCandidates.getFirst().getScore() >= 600.0f);
        assertEquals("The safe physical hunter must be retained",
                Integer.toString(snoova.getCardId()),
                bots.decideBoth(scn));
        scn.DSDecided(Integer.toString(snoova.getCardId()));
        scn.PassAllResponses();

        AwaitingDecision nextDecision = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        assertNotNull("Current decision after hunter selection: "
                        + (scn.GetCurrentDecision() != null
                            ? scn.GetCurrentDecision().getText() : "none"),
                nextDecision);
        if (nextDecision.getText().contains(
                "Choose regular move action")) {
            String mechanism = bots.decideBoth(scn);
            String[] results = nextDecision
                    .getDecisionParameters().get("results");
            assertNotNull(results);
            int mechanismIndex = Integer.parseInt(mechanism);
            assertTrue("The exact paid mechanism must be landspeed; results="
                            + Arrays.toString(results),
                    mechanismIndex >= 0
                        && mechanismIndex < results.length);
            assertEquals("Move using landspeed", results[mechanismIndex]);
            scn.DSDecided(mechanism);
        }

        assertTrue(scn.DSDecisionAvailable("Choose where to move"));
        String destination = bots.decideBoth(scn);
        assertTrue("The retained hunter must finish at an offered bounty site",
                destination.equals(
                    Integer.toString(firstDestination.getCardId()))
                    || destination.equals(
                        Integer.toString(secondDestination.getCardId())));
        scn.DSDecided(destination);
        scn.PassAllResponses();

        assertTrue("The real move must end at the selected bounty site",
                scn.game().getModifiersQuerying()
                    .getLocationThatCardIsAt(scn.gameState(), snoova)
                    == firstDestination
                || scn.game().getModifiersQuerying()
                    .getLocationThatCardIsAt(scn.gameState(), snoova)
                    == secondDestination);
        assertFalse("Snoova must leave the origin",
                origin ==
                scn.game().getModifiersQuerying()
                    .getLocationThatCardIsAt(scn.gameState(), snoova));
        assertEquals("The regular move must pay exactly one Force",
                forceBeforeMove - 1, scn.GetDSForcePileCount());
    }

    @Test
    public void alreadyMovedHunterDoesNotCreateAFalseRouteOrReserve() {
        var scn = scenario();
        var imperialCity = scn.GetDSCard("imperialCity");
        var palace = scn.GetDSCard("palace");
        var dockingBay = scn.GetDSCard("dockingBay");
        var snoova = scn.GetDSCard("snoova");
        var bounty = scn.GetDSCard("bounty");
        var bountyTarget = scn.GetLSCard("bountyTarget");
        scn.StartGame();
        scn.MoveLocationToTable(palace);
        scn.MoveLocationToTable(dockingBay);
        var routeSites = new ArrayList<>(List.of(
                imperialCity, palace, dockingBay));
        routeSites.sort(Comparator.comparingInt(
                scn.gameState().getLocationsInOrder()::indexOf));
        var origin = routeSites.get(1);
        var destination = routeSites.get(0);
        scn.MoveCardsToLocation(origin, snoova);
        scn.MoveCardsToLocation(destination, bountyTarget);
        scn.AttachCardsTo(bountyTarget, bounty);

        var analyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer();
        analyzer.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertTrue("The legal unmoved hunter must establish the route",
                analyzer.hasSafeAgentsOfBlackSunBountyMoveRoute(
                        scn.game(), VirtualTableScenario.DS));

        scn.game().getModifiersQuerying().regularMovePerformed(snoova);

        assertFalse("A hunter that already moved may not support the action",
                analyzer.hasSafeAgentsOfBlackSunBountyMoveRoute(
                        scn.game(), VirtualTableScenario.DS));
        assertEquals("An impossible move must reserve no Force",
                0, analyzer.getAgentsOfBlackSunBountyMoveForceReserve(
                        scn.game(), VirtualTableScenario.DS));
    }

    @Test
    public void alreadyMovedXizorDoesNotCreateAFalseFlipMoveReserve() {
        var scn = scenario();
        var xizor = scn.GetDSCard("xizor");
        var dockingBay = scn.GetDSCard("dockingBay");
        scn.StartGame();
        scn.MoveLocationToTable(dockingBay);
        scn.DSActivateForceCheat(2);
        scn.SkipToDSTurn(Phase.DEPLOY);

        var analyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer();
        analyzer.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertEquals("The legal Coruscant battleground move costs one Force",
                1, analyzer.getAgentsOfBlackSunCurrentMoveForceReserve(
                        scn.game(), VirtualTableScenario.DS, null));

        scn.game().getModifiersQuerying().regularMovePerformed(xizor);

        assertEquals("An unavailable Xizor move must reserve no Force",
                0, analyzer.getAgentsOfBlackSunCurrentMoveForceReserve(
                        scn.game(), VirtualTableScenario.DS, null));
    }
}
