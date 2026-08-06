package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.ai.models.common.phase.AiActionSourceProvenance;
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
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Batch Thirteen (2026-07-27): native engine contract for Old Allies / We
 * Need Your Help (204_32, LIGHT). Card Java unchanged.
 *
 * Law (Card204_032.java L130-L138): flips when you control Jakku system and
 * occupy two Jakku battleground sites, OR occupy the system and control two
 * such sites (cross-paired). Back (Card204_032_BACK.java L125-L127): flips
 * back when you occupy fewer than two battlegrounds ANYWHERE.
 */
public class OldAlliesObjectiveEngineContractTest {

    private static final StartingSetup OLD_ALLIES = new StartingSetup() {
        @Override
        public HashMap<String, String> Cards() {
            return new HashMap<>() {{
                put("objective", "204_32");
                put("system", "204_26");
                put("shipyard", "204_27");
                put("falcon", "206_8");
            }};
        }

        @Override
        public void Setup(VirtualTableScenario scn) {
            // Required start deploys: Jakku system, Niima Outpost Shipyard,
            // and the [EP VII] Falcon landed at the shipyard; the optional
            // Graveyard Of Giants has no match and skips.
            for (int i = 0; i < 8; i++) {
                if (scn.LSDecisionAvailable("On which side")) {
                    scn.LSChoose("Left");
                } else if (scn.LSDecisionAvailable("to deploy")) {
                    scn.LSChooseCard(scn.GetLSCard("shipyard"));
                }
            }
        }
    };

    private VirtualTableScenario oaScenario() {
        return new VirtualTableScenario(
                new HashMap<>() {{
                    put("ravager", "204_28");
                    put("reysCamp", "204_29");
                    put("tuanul", "204_31");
                    put("xwing", "1_146");
                    put("pilot", "204_12");
                    put("bodyOne", "1_28");
                    put("bodyTwo", "1_28");
                    put("bodyThree", "1_28");
                    put("distraction", "10_7");
                    put("lossFodder", "2_50");
                    put("valuablePassenger", "1_21");
                }},
                new HashMap<>() {{
                    put("tie", "1_304");
                    put("tieTwo", "1_304");
                    put("tieThree", "1_304");
                    put("stormtrooper", "1_194");
                    put("vader", "1_168");
                    put("maul", "11_55");
                }},
                24,
                24,
                OLD_ALLIES,
                StartingSetup.DefaultDSGroundLocation,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open
        );
    }

    private void moveLocationToJakku(
            VirtualTableScenario scn, PhysicalCardImpl location) {
        scn.RemoveCardZone(location);
        var placements = scn.gameState().getLocationPlacement(
                scn.game(), location, Title.Jakku, null);
        assertFalse("Expected a legal placement at Jakku",
                placements.isEmpty());
        scn.gameState().addLocationToTable(
                scn.game(), location, placements.getFirst());
    }

    private void keepOnlyLightHandCards(
            VirtualTableScenario scn, PhysicalCard... keep) {
        var protectedCards = java.util.Set.of(keep);
        var toReserve = new ArrayList<PhysicalCardImpl>();
        for (PhysicalCard card : scn.gameState().getHand(
                VirtualTableScenario.LS)) {
            if (card instanceof PhysicalCardImpl physical
                    && !protectedCards.contains(card)) {
                toReserve.add(physical);
            }
        }
        for (PhysicalCardImpl card : toReserve) {
            scn.MoveCardsToBottomOfLSReserveDeck(card);
        }
    }

    private record PublicBots(
            com.gempukku.swccgo.ai.models.rando.RandoCalAi rando,
            com.gempukku.swccgo.ai.models.chosenone.TheChosenOneAi chosen) {
        private static PublicBots forGame(
                VirtualTableScenario scn) {
            var rando = new com.gempukku.swccgo.ai.models.rando
                    .RandoCalAi();
            var chosen = new com.gempukku.swccgo.ai.models.chosenone
                    .TheChosenOneAi();
            rando.setGame(scn.game());
            chosen.setGame(scn.game());
            return new PublicBots(rando, chosen);
        }

        private String decideBoth(VirtualTableScenario scn) {
            AwaitingDecision decision = scn.GetAwaitingDecision(
                    VirtualTableScenario.LS);
            assertNotNull("Light Side must own the bot decision", decision);
            String randoResponse = rando.decide(
                    VirtualTableScenario.LS,
                    decision, scn.gameState());
            String chosenResponse = chosen.decide(
                    VirtualTableScenario.LS,
                    decision, scn.gameState());
            assertEquals("Rando and Chosen One must match",
                    randoResponse, chosenResponse);
            return randoResponse;
        }
    }

    private String selectedBlueprintId(
            AwaitingDecision decision, String response) {
        var params = decision.getDecisionParameters();
        var ids = java.util.Arrays.asList(params.get("cardId"));
        int index = ids.indexOf(response);
        assertTrue("Response was not an offered card: " + response
                        + "; decision=" + decision.getText()
                        + "; parameters=" + params,
                index >= 0);
        String[] blueprints = params.get("blueprintId");
        return blueprints != null && index < blueprints.length
                ? blueprints[index] : null;
    }

    private PhysicalCard selectedPhysicalCard(
            VirtualTableScenario scn,
            AwaitingDecision decision, String response) {
        PhysicalCard selected = AiActionSourceProvenance
                .selectedActionSource(decision, response);
        if (selected != null) return selected;
        try {
            return scn.gameState().findCardById(
                    Integer.parseInt(response));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String chooseCardBoth(
            VirtualTableScenario scn,
            com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer randoAnalyzer,
            com.gempukku.swccgo.ai.models.chosenone.strategy
                    .ObjectiveAnalyzer chosenAnalyzer,
            String prompt, List<PhysicalCard> cards) {
        List<String> cardIds = cards.stream()
                .map(card -> Integer.toString(card.getCardId()))
                .toList();
        List<String> blueprints = cards.stream()
                .map(card -> card.getBlueprintId(true)).toList();
        List<String> titles = cards.stream()
                .map(PhysicalCard::getTitle).toList();

        var randoContext = new com.gempukku.swccgo.ai.models.rando
                .evaluators.DecisionContext(
                    scn.gameState(), VirtualTableScenario.LS,
                    "CARD_SELECTION", prompt,
                    "old-allies-loss", Phase.BATTLE);
        randoContext.setGame(scn.game());
        randoContext.setSide(Side.LIGHT);
        randoContext.setObjectiveAnalyzer(randoAnalyzer);
        randoContext.setCardIds(cardIds);
        randoContext.setBlueprints(blueprints);
        randoContext.setTestingTexts(titles);
        randoContext.setSelectable(cards.stream()
                .map(ignored -> true).toList());
        randoContext.setNoPass(true);
        randoContext.setMin(1);
        randoContext.setMax(1);

        var chosenContext = new com.gempukku.swccgo.ai.models.chosenone
                .evaluators.DecisionContext(
                    scn.gameState(), VirtualTableScenario.LS,
                    "CARD_SELECTION", prompt,
                    "old-allies-loss", Phase.BATTLE);
        chosenContext.setGame(scn.game());
        chosenContext.setSide(Side.LIGHT);
        chosenContext.setObjectiveAnalyzer(chosenAnalyzer);
        chosenContext.setCardIds(cardIds);
        chosenContext.setBlueprints(blueprints);
        chosenContext.setTestingTexts(titles);
        chosenContext.setSelectable(cards.stream()
                .map(ignored -> true).toList());
        chosenContext.setNoPass(true);
        chosenContext.setMin(1);
        chosenContext.setMax(1);

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators
                .CombinedEvaluator().evaluateDecision(randoContext);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators
                .CombinedEvaluator().evaluateDecision(chosenContext);
        assertNotNull(rando);
        assertNotNull(chosen);
        assertEquals("Rando and Chosen One must choose the same casualty",
                rando.getActionId(), chosen.getActionId());
        assertEquals(rando.getScore(), chosen.getScore(), 0.0f);
        return rando.getActionId();
    }

    @Test
    public void oaFrontLegAFlipsOnControlledSystemWithTwoOccupiedSites() {
        var scn = oaScenario();
        var objective = scn.GetLSCard("objective");
        var system = scn.GetLSCard("system");
        var shipyard = scn.GetLSCard("shipyard");
        var ravager = scn.GetLSCard("ravager");
        var xwing = scn.GetLSCard("xwing");
        var pulseOne = scn.GetLSFiller(3);

        scn.MoveCardsToLSHand(pulseOne);
        scn.StartGame();
        moveLocationToJakku(scn, ravager);
        // Both battleground sites merely OCCUPIED (contested by the
        // opponent); the system stays empty until after the phase skip so
        // the flip completes only at the assertion pulse.
        scn.MoveCardsToLocation(shipyard, scn.GetLSFiller(1));
        scn.MoveCardsToLocation(shipyard, scn.GetDSFiller(1));
        scn.MoveCardsToLocation(ravager, scn.GetLSFiller(2));
        scn.MoveCardsToLocation(ravager, scn.GetDSFiller(2));

        scn.LSActivateForceCheat(12);
        scn.SkipToLSTurn(Phase.DEPLOY);
        scn.MoveCardsToLocation(system, xwing);
        scn.LSDeployCardAndPassResponses(
                pulseOne, scn.GetDSStartingLocation());
        assertTrue("Controlled system plus two occupied battleground sites must flip (leg A)",
                objective.isFlipped());
    }

    @Test
    public void oaFrontLegBFlipsOnOccupiedSystemWithTwoControlledSites() {
        var scn = oaScenario();
        var objective = scn.GetLSCard("objective");
        var system = scn.GetLSCard("system");
        var shipyard = scn.GetLSCard("shipyard");
        var ravager = scn.GetLSCard("ravager");
        var xwing = scn.GetLSCard("xwing");
        var tie = scn.GetDSCard("tie");
        var pulseOne = scn.GetLSFiller(3);

        scn.MoveCardsToLSHand(pulseOne);
        scn.StartGame();
        moveLocationToJakku(scn, ravager);
        // Both battleground sites solely CONTROLLED; the contested system
        // (occupy without control) is assembled after the phase skip.
        scn.MoveCardsToLocation(shipyard, scn.GetLSFiller(1));
        scn.MoveCardsToLocation(ravager, scn.GetLSFiller(2));

        scn.LSActivateForceCheat(12);
        scn.SkipToLSTurn(Phase.DEPLOY);
        scn.MoveCardsToLocation(system, xwing);
        scn.MoveCardsToLocation(system, tie);
        scn.LSDeployCardAndPassResponses(
                pulseOne, scn.GetDSStartingLocation());
        assertTrue("Occupied system plus two controlled battleground sites must flip (leg B)",
                objective.isFlipped());
    }

    @Test
    public void oaFrontIgnoresNonBattlegroundSitesAndSingleSites() {
        var scn = oaScenario();
        var objective = scn.GetLSCard("objective");
        var system = scn.GetLSCard("system");
        var shipyard = scn.GetLSCard("shipyard");
        var reysCamp = scn.GetLSCard("reysCamp");
        var xwing = scn.GetLSCard("xwing");
        var pulseOne = scn.GetLSFiller(3);

        scn.MoveCardsToLSHand(pulseOne);
        scn.StartGame();
        moveLocationToJakku(scn, reysCamp);
        // One battleground site controlled plus Rey's Encampment (light-only
        // icons, NOT a battleground) occupied: the count-2 leg stays unmet.
        scn.MoveCardsToLocation(shipyard, scn.GetLSFiller(1));
        scn.MoveCardsToLocation(reysCamp, scn.GetLSFiller(2));
        scn.MoveCardsToLocation(system, xwing);

        scn.LSActivateForceCheat(12);
        scn.SkipToLSTurn(Phase.DEPLOY);
        scn.LSDeployCardAndPassResponses(
                pulseOne, scn.GetDSStartingLocation());
        assertFalse("A non-battleground site must not satisfy the two-site leg",
                objective.isFlipped());
    }

    @Test
    public void oaBackHoldsAtTwoBattlegroundsAndFlipsBackBelow() {
        var scn = oaScenario();
        var objective = scn.GetLSCard("objective");
        var system = scn.GetLSCard("system");
        var shipyard = scn.GetLSCard("shipyard");
        var ravager = scn.GetLSCard("ravager");
        var xwing = scn.GetLSCard("xwing");
        var reysCamp = scn.GetLSCard("reysCamp");
        var pulseOne = scn.GetLSFiller(3);
        var pulseTwo = scn.GetLSFiller(4);
        var pulseThree = scn.GetLSFiller(5);

        scn.MoveCardsToLSHand(pulseOne, pulseTwo, pulseThree);
        scn.StartGame();
        moveLocationToJakku(scn, ravager);
        // Pulses must land on a NON-battleground (Rey's Encampment) or the
        // pulse bodies themselves keep the occupy-2 hold satisfied.
        moveLocationToJakku(scn, reysCamp);
        scn.MoveCardsToLocation(shipyard, scn.GetLSFiller(1));

        scn.LSActivateForceCheat(16);
        scn.SkipToLSTurn(Phase.DEPLOY);
        scn.MoveCardsToLocation(ravager, scn.GetLSFiller(2));
        scn.MoveCardsToLocation(system, xwing);
        scn.LSDeployCardAndPassResponses(pulseOne, reysCamp);
        assertTrue("Leg B must flip", objective.isFlipped());
        scn.DSPass();

        // Occupying exactly two battlegrounds (shipyard + ravager) after the
        // system empties still holds the back.
        scn.MoveOutOfPlay(xwing);
        scn.LSDeployCardAndPassResponses(pulseTwo, reysCamp);
        assertTrue("Two occupied battlegrounds must hold the back",
                objective.isFlipped());
        scn.DSPass();

        // Dropping to one occupied battleground flips the back to front.
        scn.MoveOutOfPlay(scn.GetLSFiller(2));
        scn.LSDeployCardAndPassResponses(pulseThree, reysCamp);
        assertFalse("Occupying fewer than two battlegrounds must flip back",
                objective.isFlipped());
    }

    @Test
    public void oaProfileRulesTrackTheEngineLaw() {
        var scn = oaScenario();
        var system = scn.GetLSCard("system");
        var shipyard = scn.GetLSCard("shipyard");
        var ravager = scn.GetLSCard("ravager");
        var xwing = scn.GetLSCard("xwing");

        scn.StartGame();
        moveLocationToJakku(scn, ravager);
        scn.MoveCardsToLocation(shipyard, scn.GetLSFiller(1));
        scn.MoveCardsToLocation(system, xwing);

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        assertTrue("Profile must hydrate for 204_32", analyzer.isAnalyzed());

        var preFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.LS, "preFlip", "flip");
        assertEquals("OA front encodes the two cross-paired legs", 2,
                preFlip.size());
        assertEquals("One site plus the system satisfies neither leg", 0,
                preFlip.stream().filter(
                        state -> state.conditionSatisfied()).count());

        scn.MoveCardsToLocation(ravager, scn.GetLSFiller(2));
        var complete = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.LS, "preFlip", "flip");
        assertTrue("Sole control of everything satisfies at least leg B",
                complete.stream().anyMatch(
                        state -> state.conditionSatisfied()));

        var postFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.LS, "postFlip", "flipBack");
        assertEquals("The back encodes one hold rule", 1, postFlip.size());
    }

    @Test
    public void oaRouteBudgetFundsPilotFalconAndTwoGroundBodiesAsOnePlan() {
        var scn = oaScenario();
        var ravager = scn.GetLSCard("ravager");
        var pilot = scn.GetLSCard("pilot");
        var bodyOne = scn.GetLSCard("bodyOne");
        var bodyTwo = scn.GetLSCard("bodyTwo");
        var distraction = scn.GetLSCard("distraction");

        scn.MoveCardsToLSHand(
                pilot, bodyOne, bodyTwo, distraction);
        scn.StartGame();
        moveLocationToJakku(scn, ravager);
        keepOnlyLightHandCards(
                scn, pilot, bodyOne, bodyTwo, distraction);

        var rando = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.strategy
                .ObjectiveAnalyzer();
        rando.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        chosen.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);

        assertTrue("The setup Falcon must accept the route pilot; falconZone="
                        + scn.GetLSCard("falcon").getZone()
                        + ", falconLocation="
                        + scn.game().getModifiersQuerying()
                            .getLocationThatCardIsAt(
                                scn.gameState(),
                                scn.GetLSCard("falcon")),
                rando.isOldAlliesFalconPilotDeployCandidate(
                    scn.game(), VirtualTableScenario.LS, pilot));
        assertTrue(rando.advancesPreFlipRequirementAt(
                scn.game(), VirtualTableScenario.LS,
                bodyOne, scn.GetLSCard("shipyard")));
        assertTrue(rando.advancesPreFlipRequirementAt(
                scn.game(), VirtualTableScenario.LS,
                bodyTwo, ravager));
        assertEquals(4, rando.getOldAlliesFutureRouteForceReserve(
                scn.game(), VirtualTableScenario.LS, null));
        assertEquals(2, rando.getOldAlliesFutureRouteForceReserve(
                scn.game(), VirtualTableScenario.LS, pilot));
        assertEquals(3, rando.getOldAlliesFutureRouteForceReserve(
                scn.game(), VirtualTableScenario.LS, bodyOne));
        assertEquals(4, rando.getOldAlliesFutureRouteForceReserve(
                scn.game(), VirtualTableScenario.LS, distraction));
        assertTrue(rando.isOldAlliesRouteDeployCandidate(
                scn.game(), VirtualTableScenario.LS, pilot));
        assertTrue(rando.isOldAlliesRouteDeployCandidate(
                scn.game(), VirtualTableScenario.LS, bodyOne));
        assertFalse(rando.isOldAlliesRouteDeployCandidate(
                scn.game(), VirtualTableScenario.LS, distraction));
        assertEquals(
                rando.getOldAlliesFutureRouteForceReserve(
                    scn.game(), VirtualTableScenario.LS, null),
                chosen.getOldAlliesFutureRouteForceReserve(
                    scn.game(), VirtualTableScenario.LS, null));
        assertEquals(
                rando.getOldAlliesFutureRouteForceReserve(
                    scn.game(), VirtualTableScenario.LS, pilot),
                chosen.getOldAlliesFutureRouteForceReserve(
                    scn.game(), VirtualTableScenario.LS, pilot));
    }

    @Test
    public void oaKeepsTheLandedFalconWhenAnotherShipCoversJakku() {
        var scn = oaScenario();
        var system = scn.GetLSCard("system");
        var ravager = scn.GetLSCard("ravager");
        var xwing = scn.GetLSCard("xwing");
        var pilot = scn.GetLSCard("pilot");
        var body = scn.GetLSCard("bodyOne");

        scn.StartGame();
        moveLocationToJakku(scn, ravager);
        scn.BoardAsPilot(scn.GetLSCard("falcon"), pilot);
        scn.MoveCardsToLSHand(body);
        scn.MoveCardsToLocation(system, xwing);
        keepOnlyLightHandCards(scn, body);

        var rando = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.strategy
                .ObjectiveAnalyzer();
        rando.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        chosen.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);

        assertEquals("The Falcon remains Niima's body when space is covered",
                1, rando.getOldAlliesFutureRouteForceReserve(
                    scn.game(), VirtualTableScenario.LS, null));
        assertTrue(rando.isOldAlliesRouteDeployCandidate(
                scn.game(), VirtualTableScenario.LS, body));
        assertEquals(
                rando.getOldAlliesFutureRouteForceReserve(
                    scn.game(), VirtualTableScenario.LS, null),
                chosen.getOldAlliesFutureRouteForceReserve(
                    scn.game(), VirtualTableScenario.LS, null));
        assertFalse("Covered space must not route the Falcon away from Niima",
                rando.isOldAlliesFalconTakeOffDestination(
                    scn.game(), VirtualTableScenario.LS,
                    scn.GetLSCard("falcon"), system));

        scn.LSActivateForceCheat(2);
        scn.SkipToLSTurn(Phase.MOVE);
        String takeOff = scn.GetCardActionId(
                VirtualTableScenario.LS,
                scn.GetLSCard("falcon"), "Take off");
        assertNotNull(takeOff);
        assertFalse("Both public bots must keep the Falcon as Niima's body",
                takeOff.equals(PublicBots.forGame(scn).decideBoth(scn)));
    }

    @Test
    public void oaReleasesTheFalconHoldWhenNiimaIsOverwhelmed() {
        var scn = oaScenario();
        var system = scn.GetLSCard("system");
        var shipyard = scn.GetLSCard("shipyard");

        scn.StartGame();
        scn.BoardAsPilot(
                scn.GetLSCard("falcon"), scn.GetLSCard("pilot"));
        scn.MoveCardsToLocation(system, scn.GetLSCard("xwing"));
        scn.MoveCardsToLocation(
                shipyard,
                scn.GetDSCard("vader"),
                scn.GetDSCard("maul"));

        scn.LSActivateForceCheat(2);
        scn.SkipToLSTurn(Phase.MOVE);
        String takeOff = scn.GetCardActionId(
                VirtualTableScenario.LS,
                scn.GetLSCard("falcon"), "Take off");
        assertNotNull(takeOff);
        assertEquals("A hopeless Niima defense must release the objective hold",
                takeOff, PublicBots.forGame(scn).decideBoth(scn));
    }

    @Test
    public void oaRealForceLossPreservesTheSelectedFourForceRoute() {
        var scn = oaScenario();
        var ravager = scn.GetLSCard("ravager");
        var pilot = scn.GetLSCard("pilot");
        var bodyOne = scn.GetLSCard("bodyOne");
        var bodyTwo = scn.GetLSCard("bodyTwo");
        var lossFodder = scn.GetLSCard("lossFodder");
        var stormtrooper = scn.GetDSCard("stormtrooper");

        scn.MoveCardsToLSHand(
                pilot, bodyOne, bodyTwo, lossFodder);
        scn.StartGame();
        moveLocationToJakku(scn, ravager);
        keepOnlyLightHandCards(
                scn, pilot, bodyOne, bodyTwo, lossFodder);
        scn.MoveCardsToLocation(
                scn.GetDSStartingLocation(), stormtrooper);

        var rando = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.strategy
                .ObjectiveAnalyzer();
        rando.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        chosen.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        for (PhysicalCard routeCard : List.of(
                pilot, bodyOne, bodyTwo)) {
            assertTrue(routeCard.getTitle() + " must be loss-protected",
                    rando.isPreferredCountedObjectivePresenceForceLossCandidate(
                        scn.game(), VirtualTableScenario.LS,
                        routeCard));
        }
        assertFalse(rando
                .isPreferredCountedObjectivePresenceForceLossCandidate(
                    scn.game(), VirtualTableScenario.LS,
                    lossFodder));

        var blankRando = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        var blankChosen = new com.gempukku.swccgo.ai.models.chosenone.strategy
                .ObjectiveAnalyzer();
        assertEquals("Without objective facts, generic loss scoring spends the duplicate Trooper",
                Integer.toString(bodyOne.getCardId()),
                chooseCardBoth(
                    scn, blankRando, blankChosen,
                    "Choose Force to lose",
                    List.of(bodyOne, lossFodder)));
        assertEquals("Old Allies facts must reverse that choice and retain the route body",
                Integer.toString(lossFodder.getCardId()),
                chooseCardBoth(
                    scn, rando, chosen,
                    "Choose Force to lose",
                    List.of(bodyOne, lossFodder)));

        scn.DSActivateForceCheat(4);
        scn.SkipToDSTurn(Phase.CONTROL);
        scn.DSForceDrainAt(scn.GetDSStartingLocation());
        scn.PassAllResponses();
        assertTrue(scn.LSDecisionAvailable("Choose Force to lose"));
        String loss = PublicBots.forGame(scn).decideBoth(scn);
        for (PhysicalCard routeCard : List.of(
                pilot, bodyOne, bodyTwo)) {
            assertFalse("The public bots must not lose "
                            + routeCard.getTitle(),
                    Integer.toString(routeCard.getCardId())
                        .equals(loss));
        }
        scn.LSDecided(loss);
        scn.PassAllResponses();
        assertEquals(Zone.HAND, pilot.getZone());
        assertEquals(Zone.HAND, bodyOne.getZone());
        assertEquals(Zone.HAND, bodyTwo.getZone());
    }

    @Test
    public void oaForfeitsPassengerBeforeTheSelectedFalconAndPilot() {
        var scn = oaScenario();
        var falcon = scn.GetLSCard("falcon");
        var ravager = scn.GetLSCard("ravager");
        var pilot = scn.GetLSCard("pilot");
        var passenger = scn.GetLSCard("valuablePassenger");
        var shipyardBody = scn.GetLSCard("bodyOne");
        var ravagerBody = scn.GetLSCard("bodyTwo");

        scn.StartGame();
        moveLocationToJakku(scn, ravager);
        scn.BoardAsPilot(falcon, pilot);
        scn.BoardAsPassenger(falcon, passenger);
        scn.MoveCardsToLocation(
                scn.GetLSCard("shipyard"), shipyardBody);
        scn.MoveCardsToLocation(ravager, ravagerBody);

        var rando = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.strategy
                .ObjectiveAnalyzer();
        rando.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        chosen.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        assertEquals(ObjectiveAnalyzer.FlipGateFormationRole
                        .LAST_REQUIRED_ACTOR,
                rando.classifyGateFormationPieceIfRemoved(
                    scn.game(), VirtualTableScenario.LS, falcon));
        assertEquals(ObjectiveAnalyzer.FlipGateFormationRole
                        .LAST_REQUIRED_ACTOR,
                rando.classifyGateFormationPieceIfRemoved(
                    scn.game(), VirtualTableScenario.LS, pilot));
        assertEquals(ObjectiveAnalyzer.FlipGateFormationRole.NONE,
                rando.classifyGateFormationPieceIfRemoved(
                    scn.game(), VirtualTableScenario.LS, passenger));
        var blankRando = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        var blankChosen = new com.gempukku.swccgo.ai.models.chosenone.strategy
                .ObjectiveAnalyzer();
        assertEquals("Without Old Allies facts, generic forfeit scoring spends Theron first",
                Integer.toString(pilot.getCardId()),
                chooseCardBoth(
                    scn, blankRando, blankChosen,
                    "Choose a card from battle to forfeit",
                    List.of(falcon, pilot, passenger)));
        assertEquals(Integer.toString(passenger.getCardId()),
                chooseCardBoth(
                    scn, rando, chosen,
                    "Choose a card from battle to forfeit",
                    List.of(falcon, pilot, passenger)));
    }

    @Test
    public void oaPublicBotsBattleAtTheSafeBlockedSiteAndFlip() {
        var scn = oaScenario();
        var objective = scn.GetLSCard("objective");
        var system = scn.GetLSCard("system");
        var shipyard = scn.GetLSCard("shipyard");
        var ravager = scn.GetLSCard("ravager");
        var xwing = scn.GetLSCard("xwing");
        var pilot = scn.GetLSCard("pilot");
        var bodyOne = scn.GetLSCard("bodyOne");
        var bodyTwo = scn.GetLSCard("bodyTwo");
        var distraction = scn.GetLSCard("distraction");
        var stormtrooper = scn.GetDSCard("stormtrooper");

        scn.MoveCardsToLSHand(distraction);
        scn.StartGame();
        moveLocationToJakku(scn, ravager);
        keepOnlyLightHandCards(scn, distraction);
        scn.MoveCardsToLocation(shipyard, scn.GetLSFiller(1));
        scn.MoveCardsToLocation(
                ravager, pilot, bodyOne, bodyTwo, stormtrooper);
        scn.MoveCardsToLocation(system, xwing,
                scn.GetDSCard("tie"),
                scn.GetDSCard("tieTwo"),
                scn.GetDSCard("tieThree"));
        scn.LSActivateForceCheat(1);
        scn.SkipToLSTurn(Phase.DEPLOY);
        while (scn.GetLSForcePileCount() > 1) {
            scn.MoveCardsToTopOfLSUsedPile(
                    scn.GetTopOfLSForcePile());
        }
        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(
                scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        assertEquals("The blocked site requires one battle Force",
                1, analyzer.getOldAlliesCurrentBattleForceReserve(
                    scn.game(), VirtualTableScenario.LS));
        String distractionDeploy = scn.GetCardActionId(
                VirtualTableScenario.LS, distraction, "Deploy");
        assertNotNull(distractionDeploy);
        var bots = PublicBots.forGame(scn);
        assertFalse("The last Force must remain available for the route battle",
                distractionDeploy.equals(bots.decideBoth(scn)));
        assertEquals(1, scn.GetLSForcePileCount());
        scn.PrepareLSDestiny(7);
        scn.PrepareDSDestiny(0);
        scn.SkipToLSTurn(Phase.BATTLE);

        String safeBattle = scn.GetCardActionId(
                VirtualTableScenario.LS, ravager,
                "Initiate battle");
        String unsafeBattle = scn.GetCardActionId(
                VirtualTableScenario.LS, system,
                "Initiate battle");
        assertNotNull(safeBattle);
        assertNotNull(unsafeBattle);
        String selected = bots.decideBoth(scn);
        assertEquals("The bots must clear the missing site control leg",
                safeBattle, selected);
        assertFalse("The non-advancing system stalemate must be rejected",
                unsafeBattle.equals(selected));

        scn.LSInitiateBattle(ravager);
        scn.SkipToDamageSegment(true);
        assertTrue(scn.AwaitingDSBattleDamagePayment());
        scn.DSPayBattleDamageFromCardInPlay(stormtrooper);
        if (scn.AwaitingDSBattleDamagePayment()) {
            scn.DSPayRemainingBattleDamageFromReserveDeck();
        }
        scn.PassAllResponses();
        assertTrue("Clearing Ravager must fire unchanged card Java",
                objective.isFlipped());
    }

    @Test
    public void oaBackRefusesToAbandonEitherOfTwoBattlegrounds() {
        var scn = oaScenario();
        var objective = scn.GetLSCard("objective");
        var system = scn.GetLSCard("system");
        var shipyard = scn.GetLSCard("shipyard");
        var ravager = scn.GetLSCard("ravager");
        var reysCamp = scn.GetLSCard("reysCamp");
        var falcon = scn.GetLSCard("falcon");
        var xwing = scn.GetLSCard("xwing");
        var bodyOne = scn.GetLSCard("bodyOne");
        var bodyTwo = scn.GetLSCard("bodyTwo");
        var pulse = scn.GetLSFiller(3);

        scn.MoveCardsToLSHand(pulse);
        scn.StartGame();
        moveLocationToJakku(scn, ravager);
        moveLocationToJakku(scn, reysCamp);
        scn.MoveCardsToLocation(shipyard, bodyOne);
        scn.LSActivateForceCheat(12);
        scn.SkipToLSTurn(Phase.DEPLOY);
        scn.MoveCardsToLocation(ravager, bodyTwo);
        scn.MoveCardsToLocation(system, xwing);
        scn.LSDeployCardAndPassResponses(pulse, reysCamp);
        assertTrue(objective.isFlipped());
        scn.MoveOutOfPlay(xwing);
        scn.MoveOutOfPlay(falcon);
        scn.MoveOutOfPlay(pulse);

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(
                scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        assertEquals(ObjectiveAnalyzer.FlipGateFormationRole
                        .LAST_FLIP_BACK_BLOCKER,
                analyzer.classifyGateFormationPieceIfRemoved(
                    scn.game(), VirtualTableScenario.LS, bodyOne));
        assertEquals(ObjectiveAnalyzer.FlipGateFormationRole
                        .LAST_FLIP_BACK_BLOCKER,
                analyzer.classifyGateFormationPieceIfRemoved(
                    scn.game(), VirtualTableScenario.LS, bodyTwo));

        scn.SkipToPhase(Phase.MOVE);
        String bodyOneMove = scn.GetCardActionId(
                VirtualTableScenario.LS, bodyOne,
                "Move using landspeed");
        String bodyTwoMove = scn.GetCardActionId(
                VirtualTableScenario.LS, bodyTwo,
                "Move using landspeed");
        assertNotNull("A tempting legal consolidation move must be offered",
                bodyOneMove);
        assertNotNull("The other singleton holder must also be movable",
                bodyTwoMove);
        String selected = PublicBots.forGame(scn).decideBoth(scn);
        assertFalse("Both bots must retain the Shipyard holder",
                bodyOneMove.equals(selected));
        assertFalse("Both bots must retain the Ravager holder",
                bodyTwoMove.equals(selected));
        assertEquals("With no harmless third mover, both bots must end movement",
                "", selected);
        scn.LSDecided(selected);
        scn.PassAllResponses();
        assertTrue(objective.isFlipped());
        assertEquals(shipyard,
                scn.game().getModifiersQuerying()
                    .getLocationThatCardIsAt(
                        scn.gameState(), bodyOne));
        assertEquals(ravager,
                scn.game().getModifiersQuerying()
                    .getLocationThatCardIsAt(
                        scn.gameState(), bodyTwo));
    }

    @Test
    public void oaPublicBotsPullCrewGroundTakeOffAndNativelyFlip() {
        var scn = oaScenario();
        var objective = scn.GetLSCard("objective");
        var system = scn.GetLSCard("system");
        var shipyard = scn.GetLSCard("shipyard");
        var falcon = scn.GetLSCard("falcon");
        var ravager = scn.GetLSCard("ravager");
        var pilot = scn.GetLSCard("pilot");
        var bodyOne = scn.GetLSCard("bodyOne");
        var bodyTwo = scn.GetLSCard("bodyTwo");
        var distraction = scn.GetLSCard("distraction");

        scn.MoveCardsToLSHand(
                pilot, bodyOne, bodyTwo, distraction);
        scn.StartGame();
        scn.MoveOutOfPlay(scn.GetLSCard("tuanul"));
        keepOnlyLightHandCards(
                scn, pilot, bodyOne, bodyTwo, distraction);
        scn.LSActivateForceCheat(4);
        scn.MoveCardsToBottomOfLSReserveDeck(
                ravager, scn.GetLSCard("reysCamp"));
        scn.SkipToLSTurn(Phase.DEPLOY);
        while (scn.GetLSForcePileCount() > 4) {
            scn.MoveCardsToTopOfLSUsedPile(
                    scn.GetTopOfLSForcePile());
        }
        assertEquals(4, scn.GetLSForcePileCount());
        if (scn.AwaitingDSDeployPhaseActions()) scn.DSPass();

        var bots = PublicBots.forGame(scn);
        var pullAnalyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        pullAnalyzer.analyze(
                scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        assertTrue("Ravager must be a legal native route candidate",
                pullAnalyzer.isNativeObjectiveLocationRouteCandidate(
                    scn.game(), VirtualTableScenario.LS, ravager));
        assertEquals("Ravager must classify as the missing route location",
                ObjectiveAnalyzer.ObjectiveProgressCandidateRole
                    .REQUIRED_LOCATION,
                pullAnalyzer.classifyPreFlipProgressCandidate(
                    scn.game(), VirtualTableScenario.LS, ravager));
        assertTrue("The objective must see a missing route location; zone="
                        + ravager.getZone() + "; reserve="
                        + scn.gameState().getReserveDeck(
                            VirtualTableScenario.LS).stream()
                            .map(card -> card.getTitle() + "@" + card.getZone())
                            .toList(),
                pullAnalyzer.hasObjectiveLocationRouteCandidateInReserve(
                    scn.game(), VirtualTableScenario.LS));
        String pullAction = scn.GetCardActionId(
                VirtualTableScenario.LS, objective,
                "Deploy Jakku location from Reserve Deck");
        assertNotNull(pullAction);
        var pullParams = scn.GetAwaitingDecision(VirtualTableScenario.LS)
                .getDecisionParameters();
        assertEquals("The native battleground pull starts the route; ids="
                        + java.util.Arrays.toString(
                            pullParams.get("actionId"))
                        + "; texts=" + java.util.Arrays.toString(
                            pullParams.get("actionText"))
                        + "; cards=" + java.util.Arrays.toString(
                            pullParams.get("cardId")),
                pullAction, bots.decideBoth(scn));
        scn.LSDecided(pullAction);
        AwaitingDecision pullChild = scn.GetAwaitingDecision(
                VirtualTableScenario.LS);
        String pulled = bots.decideBoth(scn);
        assertEquals("The pull must reject Rey's Encampment",
                ravager.getBlueprintId(true),
                selectedBlueprintId(pullChild, pulled));
        scn.LSDecided(pulled);
        scn.PassAllResponses();
        if (scn.LSDecisionAvailable("On which side")) {
            scn.LSChoose("Left");
            scn.PassAllResponses();
        }

        var routeCards = java.util.Set.of(pilot, bodyOne, bodyTwo);
        var groundSites = new java.util.HashSet<Integer>();
        int deployments = 0;
        while (deployments < 3) {
            if (scn.AwaitingDSDeployPhaseActions()) scn.DSPass();
            AwaitingDecision parent = scn.GetAwaitingDecision(
                    VirtualTableScenario.LS);
            assertNotNull(parent);
            String deployAction = bots.decideBoth(scn);
            PhysicalCard selected = AiActionSourceProvenance
                    .selectedActionSource(parent, deployAction);
            assertTrue("The public bot spent route Force on "
                            + (selected != null
                                ? selected.getTitle() : deployAction),
                    routeCards.contains(selected));
            scn.LSDecided(deployAction);

            AwaitingDecision destinationDecision =
                    scn.GetAwaitingDecision(VirtualTableScenario.LS);
            var childAnalyzer = new com.gempukku.swccgo.ai.models.rando
                    .strategy.ObjectiveAnalyzer();
            childAnalyzer.analyze(
                    scn.game(), VirtualTableScenario.LS, Side.LIGHT);
            if (selected != pilot) {
                assertTrue("Selected body " + selected.getTitle()
                                + " must remain in the funded ground route; zone="
                                + selected.getZone() + "; canonical="
                                + childAnalyzer.assessOldAlliesFutureRoute(
                                    scn.game(), VirtualTableScenario.LS,
                                    null) + "; reserve="
                                + childAnalyzer.assessOldAlliesFutureRoute(
                                    scn.game(), VirtualTableScenario.LS,
                                    selected) + "; hand="
                                + scn.gameState().getHand(
                                    VirtualTableScenario.LS).stream()
                                    .map(card -> card.getTitle() + "@"
                                        + card.getZone()).toList(),
                        childAnalyzer.isOldAlliesGroundRouteDeployCandidate(
                            scn.game(), VirtualTableScenario.LS, selected));
                assertTrue("The Falcon must be rejected as a ground-body destination",
                        childAnalyzer.isOldAlliesWrongGroundRouteDestination(
                            scn.game(), VirtualTableScenario.LS,
                            selected, falcon));
            }
            String destination = bots.decideBoth(scn);
            PhysicalCard destinationCard = selectedPhysicalCard(
                    scn, destinationDecision, destination);
            String destinationBlueprint = destinationCard != null
                    ? destinationCard.getBlueprintId(true)
                    : selectedBlueprintId(
                        destinationDecision, destination);
            assertNotNull("Destination must resolve; params="
                    + destinationDecision.getDecisionParameters()
                        .entrySet().stream()
                        .map(entry -> entry.getKey() + "="
                            + java.util.Arrays.toString(entry.getValue()))
                        .toList(), destinationBlueprint);
            if (selected == pilot) {
                assertEquals("The route pilot must board the setup Falcon",
                        falcon.getBlueprintId(true),
                        destinationBlueprint);
            } else {
                assertTrue("Ground body " + selected.getTitle()
                                + " chose " + destinationBlueprint
                                + " instead of a Jakku battleground; params="
                                + destinationDecision
                                    .getDecisionParameters().entrySet()
                                    .stream()
                                    .map(entry -> entry.getKey() + "="
                                        + java.util.Arrays.toString(
                                            entry.getValue()))
                                    .toList(),
                        destinationBlueprint.equals(
                            shipyard.getBlueprintId(true))
                            || destinationBlueprint.equals(
                                ravager.getBlueprintId(true)));
                groundSites.add(destinationBlueprint.equals(
                        shipyard.getBlueprintId(true))
                            ? shipyard.getCardId() : ravager.getCardId());
            }
            scn.LSDecided(destination);
            if (selected == pilot) {
                assertTrue("Pilot deployment must ask for a capacity slot",
                        scn.LSDecisionAvailable("capacity slot"));
                String role = bots.decideBoth(scn);
                scn.LSDecided(role);
            }
            scn.PassAllResponses();
            assertFalse("Selected " + selected.getTitle()
                            + " stayed in hand after destination "
                            + destinationBlueprint + "; current="
                            + (scn.GetCurrentDecision() != null
                                ? scn.GetCurrentDecision().getText()
                                : "none") + "; decider="
                            + scn.GetDecidingPlayer(),
                    selected.getZone() == Zone.HAND);
            deployments++;
        }
        assertEquals("One ground body must remain at each site",
                java.util.Set.of(
                    shipyard.getCardId(), ravager.getCardId()),
                groundSites);
        assertEquals("The exact four-Force route must be paid",
                0, scn.GetLSForcePileCount());
        assertEquals("Theron must be the Falcon's assigned pilot",
                falcon, scn.game().getModifiersQuerying().getIsPilotOf(
                    scn.gameState(), pilot));

        scn.SkipToPhase(Phase.MOVE);
        String takeOff = scn.GetCardActionId(
                VirtualTableScenario.LS, falcon, "Take off");
        assertNotNull(takeOff);
        assertEquals("The setup Falcon must take off for free",
                takeOff, bots.decideBoth(scn));
        scn.LSDecided(takeOff);
        AwaitingDecision takeOffDestination =
                scn.GetAwaitingDecision(VirtualTableScenario.LS);
        String systemChoice = bots.decideBoth(scn);
        PhysicalCard systemCard = selectedPhysicalCard(
                scn, takeOffDestination, systemChoice);
        String systemBlueprint = systemCard != null
                ? systemCard.getBlueprintId(true)
                : selectedBlueprintId(
                    takeOffDestination, systemChoice);
        assertEquals("The Falcon must complete the Jakku system leg",
                system.getBlueprintId(true),
                systemBlueprint);
        scn.LSDecided(systemChoice);
        scn.PassAllResponses();

        assertEquals("Niima's text makes the takeoff free",
                0, scn.GetLSForcePileCount());
        assertTrue("Unchanged card Java must fire the real flip",
                objective.isFlipped());
    }
}
