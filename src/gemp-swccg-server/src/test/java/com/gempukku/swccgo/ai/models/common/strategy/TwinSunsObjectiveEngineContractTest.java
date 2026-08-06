package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.ai.models.common.phase.AiActionSourceProvenance;
import com.gempukku.swccgo.ai.models.common.phase.BattleForfeitFacts;
import com.gempukku.swccgo.ai.models.common.phase.BattleForfeitPolicy;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Batch Sixteen (2026-07-27): native engine contract for Twin Suns Of
 * Tatooine / Well Trained In The Jedi Arts (301_4, DARK — the takeover's
 * LIGHT listing was wrong). Card Java unchanged.
 *
 * Law (Card301_004.java L100-L122): flips when you control two Tatooine
 * battleground sites, at least one WITH a Dark Jedi (computed: dark
 * character of ability 6+), you occupy Tatooine system, and the opponent
 * controls zero Tatooine sites of any kind. Back (Card301_004_BACK.java
 * L97-L118): flips back when the opponent controls strictly more Tatooine
 * sites than you; ties hold.
 */
public class TwinSunsObjectiveEngineContractTest {

    private static final StartingSetup TWIN_SUNS = new StartingSetup() {
        @Override
        public HashMap<String, String> Cards() {
            return new HashMap<>() {{
                put("objective", "301_4");
                put("system", "1_289");
                put("mosEisley", "1_295");
            }};
        }

        @Override
        public void Setup(VirtualTableScenario scn) {
            for (int i = 0; i < 8; i++) {
                if (scn.DSDecisionAvailable("On which side")) {
                    scn.DSChoose("Left");
                } else if (scn.DSDecisionAvailable("to deploy")) {
                    scn.DSChooseCard(scn.GetDSCard("mosEisley"));
                }
            }
        }
    };

    private VirtualTableScenario tsotScenario() {
        return new VirtualTableScenario(
                new HashMap<>() {{
                    put("lsDockingBay", "1_129");
                    put("lsShip", "1_143");
                }},
                new HashMap<>() {{
                    put("cantina", "1_290");
                    put("vader", "1_168");
                    put("tie", "1_304");
                    put("tieTwo", "1_304");
                    put("gunner", "3_88");
                    put("occupation", "7_244");
                    put("peekHigh", "8_120");
                    put("peekLow", "1_194");
                }},
                24,
                24,
                StartingSetup.DefaultLSGroundLocation,
                TWIN_SUNS,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open
        );
    }

    private void moveSiteToTatooine(
            VirtualTableScenario scn, PhysicalCardImpl site) {
        scn.RemoveCardZone(site);
        var placements = scn.gameState().getLocationPlacement(
                scn.game(), site, Title.Tatooine, null);
        assertFalse("Expected a legal placement at Tatooine",
                placements.isEmpty());
        scn.gameState().addLocationToTable(
                scn.game(), site, placements.getFirst());
    }

    private void keepOnlyDarkHandCards(
            VirtualTableScenario scn, PhysicalCard... keep) {
        var protectedCards = java.util.Set.of(keep);
        var toReserve = new ArrayList<PhysicalCardImpl>();
        for (PhysicalCard card : scn.gameState().getHand(
                VirtualTableScenario.DS)) {
            if (card instanceof PhysicalCardImpl physical
                    && !protectedCards.contains(card)) {
                toReserve.add(physical);
            }
        }
        for (PhysicalCardImpl card : toReserve) {
            scn.MoveCardsToBottomOfDSReserveDeck(card);
        }
    }

    private void keepExactlyDarkForce(
            VirtualTableScenario scn, int amount) {
        while (scn.GetDSForcePileCount() > amount) {
            scn.MoveCardsToTopOfDSUsedPile(
                    scn.GetTopOfDSForcePile());
        }
        while (scn.GetDSForcePileCount() < amount) {
            scn.MoveCardsToTopOfDSForcePile(
                    scn.GetTopOfDSReserveDeck());
        }
        assertEquals(amount, scn.GetDSForcePileCount());
    }

    private record PublicBots(
            com.gempukku.swccgo.ai.models.rando.RandoCalAi rando,
            com.gempukku.swccgo.ai.models.chosenone.TheChosenOneAi chosen) {
        private static PublicBots forGame(VirtualTableScenario scn) {
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
                    VirtualTableScenario.DS);
            assertNotNull("Dark Side must own the bot decision", decision);
            String randoResponse = rando.decide(
                    VirtualTableScenario.DS,
                    decision, scn.gameState());
            String chosenResponse = chosen.decide(
                    VirtualTableScenario.DS,
                    decision, scn.gameState());
            assertEquals("Rando and Chosen One must match",
                    randoResponse, chosenResponse);
            return randoResponse;
        }
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
            if (response != null && response.startsWith("temp")) {
                try {
                    int index = Integer.parseInt(response.substring(4));
                    var reserve = scn.gameState().getReserveDeck(
                            VirtualTableScenario.DS);
                    String[] blueprints = decision.getDecisionParameters()
                            .get("blueprintId");
                    if (index >= 0 && index < reserve.size()
                            && blueprints != null
                            && index < blueprints.length) {
                        PhysicalCard candidate = reserve.get(index);
                        if (candidate != null
                                && blueprints[index].equals(
                                    candidate.getBlueprintId(true))) {
                            return candidate;
                        }
                    }
                } catch (NumberFormatException ignoredTempId) {
                    return null;
                }
            }
            return null;
        }
    }

    private void flipTwinSunsWithNativeTrigger(
            VirtualTableScenario scn) {
        var objective = scn.GetDSCard("objective");
        var system = scn.GetDSCard("system");
        var mosEisley = scn.GetDSCard("mosEisley");
        var cantina = scn.GetDSCard("cantina");
        var vader = scn.GetDSCard("vader");
        var tie = scn.GetDSCard("tie");
        var pulse = scn.GetDSFiller(6);

        scn.MoveCardsToDSHand(pulse);
        scn.StartGame();
        moveSiteToTatooine(scn, cantina);
        scn.MoveCardsToLocation(mosEisley, scn.GetDSFiller(1));
        scn.MoveCardsToLocation(
                cantina, vader, scn.GetDSFiller(2));
        scn.MoveCardsToLocation(system, tie);
        scn.DSActivateForceCheat(16);
        scn.SkipToDSTurn(Phase.DEPLOY);
        scn.DSDeployCardAndPassResponses(
                pulse, scn.GetLSStartingLocation());
        assertTrue("The real table-change trigger must flip Twin Suns",
                objective.isFlipped());
        if (scn.GetAwaitingDecision(VirtualTableScenario.LS) != null) {
            scn.LSPass();
        }
    }

    @Test
    public void tsotFrontRequiresAllFourLegs() {
        var scn = tsotScenario();
        var objective = scn.GetDSCard("objective");
        var system = scn.GetDSCard("system");
        var mosEisley = scn.GetDSCard("mosEisley");
        var cantina = scn.GetDSCard("cantina");
        var vader = scn.GetDSCard("vader");
        var tie = scn.GetDSCard("tie");
        var pulseOne = scn.GetDSFiller(3);
        var pulseTwo = scn.GetDSFiller(4);
        var pulseThree = scn.GetDSFiller(5);

        scn.MoveCardsToDSHand(pulseOne, pulseTwo, pulseThree);
        scn.StartGame();
        moveSiteToTatooine(scn, cantina);
        scn.MoveCardsToLocation(mosEisley, scn.GetDSFiller(1));
        scn.MoveCardsToLocation(cantina, scn.GetDSFiller(2));

        scn.DSActivateForceCheat(16);
        scn.SkipToPhase(Phase.DEPLOY);

        // Two controlled battleground sites, no Dark Jedi, no system: no flip.
        scn.DSDeployCardAndPassResponses(
                pulseOne, scn.GetLSStartingLocation());
        assertFalse("Two sites without a Dark Jedi and system occupation must not flip",
                objective.isFlipped());
        scn.LSPass();

        // Add the Dark Jedi at a controlled site; still no system occupation.
        scn.MoveCardsToLocation(cantina, vader);
        scn.DSDeployCardAndPassResponses(
                pulseTwo, scn.GetLSStartingLocation());
        assertFalse("A Dark Jedi without Tatooine system occupation must not flip",
                objective.isFlipped());
        scn.LSPass();

        // Occupy the system: all four legs complete.
        scn.MoveCardsToLocation(system, tie);
        scn.DSDeployCardAndPassResponses(
                pulseThree, scn.GetLSStartingLocation());
        assertTrue("All four legs together must flip",
                objective.isFlipped());
    }

    @Test
    public void tsotFrontIsBlockedByAnyOpponentControlledTatooineSite() {
        var scn = tsotScenario();
        var objective = scn.GetDSCard("objective");
        var system = scn.GetDSCard("system");
        var mosEisley = scn.GetDSCard("mosEisley");
        var cantina = scn.GetDSCard("cantina");
        var lsDockingBay = scn.GetLSCard("lsDockingBay");
        var vader = scn.GetDSCard("vader");
        var tie = scn.GetDSCard("tie");
        var pulseOne = scn.GetDSFiller(3);
        var pulseTwo = scn.GetDSFiller(4);

        scn.MoveCardsToDSHand(pulseOne, pulseTwo);
        scn.StartGame();
        moveSiteToTatooine(scn, cantina);
        moveSiteToTatooine(scn, lsDockingBay);
        scn.MoveCardsToLocation(mosEisley, scn.GetDSFiller(1));
        scn.MoveCardsToLocation(cantina, scn.GetDSFiller(2));
        scn.MoveCardsToLocation(cantina, vader);
        scn.MoveCardsToLocation(system, tie);
        // The opponent solely controls their light docking bay.
        scn.MoveCardsToLocation(lsDockingBay, scn.GetLSFiller(1));

        scn.DSActivateForceCheat(16);
        scn.SkipToPhase(Phase.DEPLOY);
        scn.DSDeployCardAndPassResponses(
                pulseOne, scn.GetLSStartingLocation());
        assertFalse("Any opponent-controlled Tatooine site must block the flip",
                objective.isFlipped());
        scn.LSPass();

        scn.MoveOutOfPlay(scn.GetLSFiller(1));
        scn.DSDeployCardAndPassResponses(
                pulseTwo, scn.GetLSStartingLocation());
        assertTrue("Clearing the opponent's control must allow the flip",
                objective.isFlipped());
    }

    @Test
    public void tsotBackHoldsAtTiesAndFlipsBackWhenOutcontrolled() {
        var scn = tsotScenario();
        var objective = scn.GetDSCard("objective");
        var system = scn.GetDSCard("system");
        var mosEisley = scn.GetDSCard("mosEisley");
        var cantina = scn.GetDSCard("cantina");
        var lsDockingBay = scn.GetLSCard("lsDockingBay");
        var vader = scn.GetDSCard("vader");
        var tie = scn.GetDSCard("tie");
        var pulseOne = scn.GetDSFiller(3);
        var pulseTwo = scn.GetDSFiller(4);
        var pulseThree = scn.GetDSFiller(5);

        scn.MoveCardsToDSHand(pulseOne, pulseTwo, pulseThree);
        scn.StartGame();
        moveSiteToTatooine(scn, cantina);
        moveSiteToTatooine(scn, lsDockingBay);
        scn.MoveCardsToLocation(mosEisley, scn.GetDSFiller(1));
        scn.MoveCardsToLocation(cantina, scn.GetDSFiller(2));
        scn.MoveCardsToLocation(cantina, vader);
        scn.MoveCardsToLocation(system, tie);

        scn.DSActivateForceCheat(16);
        scn.SkipToPhase(Phase.DEPLOY);
        scn.DSDeployCardAndPassResponses(
                pulseOne, scn.GetLSStartingLocation());
        assertTrue("All four legs must flip", objective.isFlipped());
        scn.LSPass();

        // Owner collapses to one site; opponent takes one: 1-1 tie holds.
        scn.MoveOutOfPlay(scn.GetDSFiller(2));
        scn.MoveOutOfPlay(vader);
        scn.MoveCardsToLocation(lsDockingBay, scn.GetLSFiller(1));
        scn.DSDeployCardAndPassResponses(
                pulseTwo, scn.GetLSStartingLocation());
        assertTrue("A 1-1 Tatooine site tie must hold the back",
                objective.isFlipped());
        scn.LSPass();

        var randoAnalyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer();
        var chosenAnalyzer =
                new com.gempukku.swccgo.ai.models.chosenone.strategy
                    .ObjectiveAnalyzer();
        randoAnalyzer.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);
        chosenAnalyzer.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);
        var randoRisk = randoAnalyzer.assessPostFlipLocationRisk(
                scn.game(), VirtualTableScenario.DS, mosEisley);
        var chosenRisk = chosenAnalyzer.assessPostFlipLocationRisk(
                scn.game(), VirtualTableScenario.DS, mosEisley);
        assertTrue("At the tie, losing the sole owned site flips back",
                randoRisk.criticalIfSelfControlLost());
        assertEquals("Both bots must read the same strict-count hold",
                randoRisk, chosenRisk);
        assertTrue("The sole site body must reach the flip-back hold",
                randoAnalyzer.wouldDepartureTriggerFlipBack(
                    scn.game(), VirtualTableScenario.DS,
                    scn.GetDSFiller(1)));
        assertEquals(
                ObjectiveAnalyzer.FlipGateFormationRole
                    .LAST_FLIP_BACK_BLOCKER,
                randoAnalyzer.classifyGateFormationPieceIfRemoved(
                    scn.game(), VirtualTableScenario.DS,
                    scn.GetDSFiller(1)));
        String anchorId = Integer.toString(
                scn.GetDSFiller(1).getCardId());
        String alternativeId = Integer.toString(tie.getCardId());
        var forfeitFacts = BattleForfeitFacts
                .readFlipGateFormationSelection(
                    java.util.List.of(anchorId, alternativeId),
                    scn.gameState(), scn.game(),
                    VirtualTableScenario.DS, randoAnalyzer,
                    false, 1);
        assertTrue("The system ship supplies a noncritical loss option",
                forfeitFacts.hasUnprotectedLegalAlternative());
        var hold = BattleForfeitPolicy
                .scoreFlipGateFormationProtection(
                    anchorId, forfeitFacts.roleFor(anchorId),
                    forfeitFacts.hasUnprotectedLegalAlternative());
        assertEquals("Twin's back-side fact must reach the forfeit hold",
                "BATTLE.OBJECTIVE.FLIP_GATE_FORMATION_HOLD",
                hold.operations().getFirst().ruleArmId().id());
        assertEquals(-9999.0f,
                hold.operations().getFirst().delta(), 0.0f);

        // Opponent overtakes: strictly more flips the back.
        scn.MoveOutOfPlay(scn.GetDSFiller(1));
        scn.DSDeployCardAndPassResponses(
                pulseThree, scn.GetLSStartingLocation());
        assertFalse("Strictly more opponent-controlled Tatooine sites must flip back",
                objective.isFlipped());
    }

    @Test
    public void tsotProfileRulesTrackTheEngineLaw() {
        var scn = tsotScenario();
        var system = scn.GetDSCard("system");
        var mosEisley = scn.GetDSCard("mosEisley");
        var cantina = scn.GetDSCard("cantina");
        var vader = scn.GetDSCard("vader");
        var tie = scn.GetDSCard("tie");

        scn.StartGame();
        moveSiteToTatooine(scn, cantina);
        scn.MoveCardsToLocation(mosEisley, scn.GetDSFiller(1));
        scn.MoveCardsToLocation(cantina, scn.GetDSFiller(2));

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertTrue("Profile must hydrate for 301_4", analyzer.isAnalyzed());

        var preFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.DS, "preFlip", "flip");
        assertEquals("TSOT front encodes one four-leg rule", 1,
                preFlip.size());
        assertFalse("Without the Dark Jedi and system the rule is unmet",
                preFlip.get(0).conditionSatisfied());

        scn.MoveCardsToLocation(cantina, vader);
        scn.MoveCardsToLocation(system, tie);
        var complete = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.DS, "preFlip", "flip");
        assertTrue("All four legs complete the encoded law",
                complete.get(0).conditionSatisfied());

        var postFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.DS, "postFlip", "flipBack");
        assertEquals("The back encodes one relative-count rule", 1,
                postFlip.size());
    }

    @Test
    public void tsotPublicBotsUseTheExactOneForceSiteRoute() {
        var scn = tsotScenario();
        var objective = scn.GetDSCard("objective");
        var cantina = scn.GetDSCard("cantina");
        var gunner = scn.GetDSCard("gunner");

        scn.MoveCardsToDSHand(gunner);
        scn.StartGame();
        scn.MoveCardsToBottomOfDSReserveDeck(cantina);
        keepOnlyDarkHandCards(scn, gunner);
        scn.SkipToDSTurn(Phase.DEPLOY);
        keepExactlyDarkForce(scn, 1);

        String siteRoute = scn.GetCardActionId(
                VirtualTableScenario.DS, objective,
                "Deploy Tatooine battleground site from Reserve Deck");
        assertNotNull(siteRoute);
        assertNotNull("The one-Force distractor deploy must be offered",
                scn.GetCardActionId(
                    VirtualTableScenario.DS, gunner, "Deploy"));

        var randoAnalyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer();
        var chosenAnalyzer =
                new com.gempukku.swccgo.ai.models.chosenone.strategy
                    .ObjectiveAnalyzer();
        randoAnalyzer.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);
        chosenAnalyzer.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertEquals("The unused native pull reserves exactly 1 Force",
                1, randoAnalyzer
                    .getTwinSunsCurrentSiteRouteForceReserve(
                        scn.game(), VirtualTableScenario.DS));
        assertEquals(
                randoAnalyzer.getTwinSunsCurrentSiteRouteForceReserve(
                    scn.game(), VirtualTableScenario.DS),
                chosenAnalyzer.getTwinSunsCurrentSiteRouteForceReserve(
                    scn.game(), VirtualTableScenario.DS));

        var bots = PublicBots.forGame(scn);
        assertEquals("The objective site route must beat the distractor",
                siteRoute, bots.decideBoth(scn));
        scn.DSDecided(siteRoute);
        scn.PassAllResponses();

        AwaitingDecision child = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        assertNotNull(child);
        String childResponse = bots.decideBoth(scn);
        assertSame("The route must select the exact legal battleground site",
                cantina,
                selectedPhysicalCard(scn, child, childResponse));
        scn.DSDecided(childResponse);
        scn.PassAllResponses();
        if (scn.DSDecisionAvailable("On which side")) {
            scn.DSDecided(bots.decideBoth(scn));
            scn.PassAllResponses();
        }

        assertEquals("The native objective action must spend exactly 1 Force",
                0, scn.GetDSForcePileCount());
        assertEquals(Zone.LOCATIONS, cantina.getZone());
        assertEquals("No site-pull reserve remains after its once-turn use",
                0, randoAnalyzer
                    .getTwinSunsCurrentSiteRouteForceReserve(
                        scn.game(), VirtualTableScenario.DS));
        assertFalse("A site alone is not the full four-leg formation",
                objective.isFlipped());
        if (scn.GetAwaitingDecision(VirtualTableScenario.LS) != null) {
            scn.LSPass();
        }
        assertFalse("The once-per-turn route must be exhausted",
                scn.DSCardActionAvailable(
                    objective,
                    "Deploy Tatooine battleground site from Reserve Deck"));
    }

    @Test
    public void tsotPublicBotsDeployTheDarkJediToCompleteTheGroundLegs() {
        var scn = tsotScenario();
        var objective = scn.GetDSCard("objective");
        var system = scn.GetDSCard("system");
        var mosEisley = scn.GetDSCard("mosEisley");
        var cantina = scn.GetDSCard("cantina");
        var vader = scn.GetDSCard("vader");
        var tie = scn.GetDSCard("tie");
        var gunner = scn.GetDSCard("gunner");

        scn.MoveCardsToDSHand(vader, gunner);
        scn.StartGame();
        moveSiteToTatooine(scn, cantina);
        scn.MoveCardsToLocation(mosEisley, scn.GetDSFiller(1));
        scn.MoveCardsToLocation(system, tie);
        keepOnlyDarkHandCards(scn, vader, gunner);
        scn.DSActivateForceCheat(6);
        scn.SkipToDSTurn(Phase.DEPLOY);
        keepExactlyDarkForce(scn, 6);

        String vaderDeploy = scn.GetCardActionId(
                VirtualTableScenario.DS, vader, "Deploy");
        assertNotNull(vaderDeploy);
        assertNotNull(scn.GetCardActionId(
                VirtualTableScenario.DS, gunner, "Deploy"));
        var bots = PublicBots.forGame(scn);
        assertEquals("The Dark Jedi route must beat the cheap distractor",
                vaderDeploy, bots.decideBoth(scn));
        scn.DSDecided(vaderDeploy);

        AwaitingDecision destination = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        assertNotNull(destination);
        String destinationResponse = bots.decideBoth(scn);
        assertSame("Vader must land at the empty second battleground",
                cantina,
                selectedPhysicalCard(
                    scn, destination, destinationResponse));
        scn.DSDecided(destinationResponse);
        scn.PassAllResponses();

        assertSame(cantina,
                scn.game().getModifiersQuerying()
                    .getLocationThatCardIsPresentAt(
                        scn.gameState(), vader));
        assertTrue("The real Vader deploy must trigger the native flip",
                objective.isFlipped());
    }

    @Test
    public void tsotPublicBotsDeployTheShipToCompleteSystemOccupation() {
        var scn = tsotScenario();
        var objective = scn.GetDSCard("objective");
        var system = scn.GetDSCard("system");
        var mosEisley = scn.GetDSCard("mosEisley");
        var cantina = scn.GetDSCard("cantina");
        var vader = scn.GetDSCard("vader");
        var tie = scn.GetDSCard("tie");
        var gunner = scn.GetDSCard("gunner");

        scn.MoveCardsToDSHand(tie, gunner);
        scn.StartGame();
        moveSiteToTatooine(scn, cantina);
        scn.MoveCardsToLocation(mosEisley, scn.GetDSFiller(1));
        scn.MoveCardsToLocation(cantina, vader);
        keepOnlyDarkHandCards(scn, tie, gunner);
        scn.DSActivateForceCheat(1);
        scn.SkipToDSTurn(Phase.DEPLOY);
        keepExactlyDarkForce(scn, 1);

        String tieDeploy = scn.GetCardActionId(
                VirtualTableScenario.DS, tie, "Deploy");
        assertNotNull(tieDeploy);
        assertNotNull(scn.GetCardActionId(
                VirtualTableScenario.DS, gunner, "Deploy"));
        var bots = PublicBots.forGame(scn);
        assertEquals("System occupation must beat another one-Force body",
                tieDeploy, bots.decideBoth(scn));
        scn.DSDecided(tieDeploy);

        AwaitingDecision destination = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        assertNotNull(destination);
        String destinationResponse = bots.decideBoth(scn);
        assertSame("The ship must deploy to the exact Tatooine system",
                system,
                selectedPhysicalCard(
                    scn, destination, destinationResponse));
        scn.DSDecided(destinationResponse);
        scn.PassAllResponses();

        assertSame(system,
                scn.game().getModifiersQuerying()
                    .getLocationThatCardIsPresentAt(
                        scn.gameState(), tie));
        assertTrue("The real ship deploy must trigger the native flip",
                objective.isFlipped());
    }

    @Test
    public void tsotPublicBotsPreserveDeployAndBattleForceThenMoveAndFlip() {
        var scn = tsotScenario();
        var objective = scn.GetDSCard("objective");
        var system = scn.GetDSCard("system");
        var mosEisley = scn.GetDSCard("mosEisley");
        var cantina = scn.GetDSCard("cantina");
        var vader = scn.GetDSCard("vader");
        var tie = scn.GetDSCard("tie");
        var tieTwo = scn.GetDSCard("tieTwo");
        var battleSite = scn.GetLSStartingLocation();

        scn.MoveCardsToDSHand(tieTwo);
        scn.StartGame();
        moveSiteToTatooine(scn, cantina);
        scn.MoveCardsToLocation(
                mosEisley, vader, scn.GetDSFiller(1));
        scn.MoveCardsToLocation(system, tie);
        scn.MoveCardsToLocation(
                battleSite,
                scn.GetDSFiller(2),
                scn.GetDSFiller(3),
                scn.GetDSFiller(4),
                scn.GetDSFiller(5),
                scn.GetLSFiller(1));
        keepOnlyDarkHandCards(scn, tieTwo);
        scn.DSActivateForceCheat(1);
        scn.SkipToDSTurn(Phase.DEPLOY);
        scn.game().getModifiersQuerying().regularMovePerformed(
                scn.GetDSFiller(1));
        keepExactlyDarkForce(scn, 1);

        var randoAnalyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer();
        var chosenAnalyzer =
                new com.gempukku.swccgo.ai.models.chosenone.strategy
                    .ObjectiveAnalyzer();
        randoAnalyzer.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);
        chosenAnalyzer.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertEquals("One Force must fund the exact net-progress move",
                1, randoAnalyzer.getTwinSunsCurrentMoveForceReserve(
                    scn.game(), VirtualTableScenario.DS));
        assertEquals(
                randoAnalyzer.getTwinSunsCurrentMoveForceReserve(
                    scn.game(), VirtualTableScenario.DS),
                chosenAnalyzer.getTwinSunsCurrentMoveForceReserve(
                    scn.game(), VirtualTableScenario.DS));

        String distractorDeploy = scn.GetCardActionId(
                VirtualTableScenario.DS, tieTwo, "Deploy");
        assertNotNull(distractorDeploy);
        var bots = PublicBots.forGame(scn);
        assertEquals("The final movement payment must beat the distractor",
                "", bots.decideBoth(scn));
        scn.DSPass();
        scn.LSPass();

        assertTrue(scn.AwaitingDSBattlePhaseActions());
        assertNotNull("A favorable unrelated battle must be offered",
                scn.GetCardActionId(
                    VirtualTableScenario.DS, battleSite,
                    "Initiate battle"));
        assertEquals("The movement payment must also beat that battle",
                "", bots.decideBoth(scn));
        scn.DSPass();
        scn.LSPass();

        assertTrue(scn.AwaitingDSMovePhaseActions());
        String vaderMove = scn.GetCardActionId(
                VirtualTableScenario.DS, vader,
                "Move using landspeed");
        assertNotNull(vaderMove);
        AwaitingDecision moveParent = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        String selectedMove = bots.decideBoth(scn);
        assertSame("The route move must use Vader; selected="
                        + selectedMove + "; parameters="
                        + moveParent.getDecisionParameters().entrySet()
                            .stream()
                            .map(entry -> entry.getKey() + "="
                                + java.util.Arrays.toString(
                                    entry.getValue()))
                            .toList(),
                vader, AiActionSourceProvenance.selectedActionSource(
                    moveParent, selectedMove));
        scn.DSDecided(selectedMove);
        assertEquals(Integer.toString(cantina.getCardId()),
                bots.decideBoth(scn));
        scn.DSDecided(Integer.toString(cantina.getCardId()));
        scn.PassAllResponses();

        assertSame(mosEisley,
                scn.game().getModifiersQuerying()
                    .getLocationThatCardIsPresentAt(
                        scn.gameState(), scn.GetDSFiller(1)));
        assertSame(cantina,
                scn.game().getModifiersQuerying()
                    .getLocationThatCardIsPresentAt(
                        scn.gameState(), vader));
        assertEquals(0, scn.GetDSForcePileCount());
        assertTrue("The unchanged card Java must perform the flip",
                objective.isFlipped());
    }

    @Test
    public void tsotDifferentLegDeployDoesNotWaiveRequiredMoveForce() {
        var scn = tsotScenario();
        var mosEisley = scn.GetDSCard("mosEisley");
        var cantina = scn.GetDSCard("cantina");
        var vader = scn.GetDSCard("vader");
        var tie = scn.GetDSCard("tie");

        scn.MoveCardsToDSHand(tie);
        scn.StartGame();
        moveSiteToTatooine(scn, cantina);
        scn.MoveCardsToLocation(
                mosEisley, vader, scn.GetDSFiller(1));
        scn.DSActivateForceCheat(8);
        scn.SkipToDSTurn(Phase.DEPLOY);

        var analyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer();
        analyzer.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);

        assertTrue("The hand ship must advance the separate system leg",
                analyzer.hasLegalPreFlipActorLocationDestination(
                    scn.game(), VirtualTableScenario.DS, tie));
        assertEquals("Vader still needs one Force to reach the second site",
                1, analyzer.getTwinSunsCurrentMoveForceReserve(
                    scn.game(), VirtualTableScenario.DS));
        assertEquals("A system deploy cannot waive Vader's ground move",
                1, analyzer.getTwinSunsCurrentMoveForceReserve(
                    scn.game(), VirtualTableScenario.DS, tie));
    }

    @Test
    public void tsotRealForceLossRetainsTheSoleSiteDarkJediAndShip() {
        var scn = tsotScenario();
        var cantina = scn.GetDSCard("cantina");
        var vader = scn.GetDSCard("vader");
        var tie = scn.GetDSCard("tie");
        var lossFodder = scn.GetDSCard("peekHigh");

        scn.MoveCardsToDSHand(cantina, vader, tie, lossFodder);
        scn.StartGame();
        scn.MoveCardsToDSHand(cantina, vader, tie, lossFodder);
        keepOnlyDarkHandCards(
                scn, cantina, vader, tie, lossFodder);
        scn.MoveCardsToLocation(
                scn.GetLSStartingLocation(), scn.GetLSFiller(1));
        scn.LSActivateForceCheat(4);
        scn.SkipToLSTurn(Phase.CONTROL);

        var analyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer();
        analyzer.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertTrue("The sole native battleground site must be retained",
                analyzer
                    .isPreferredCountedObjectiveLocationForceLossCandidate(
                        scn.game(), VirtualTableScenario.DS, cantina));
        assertEquals(ObjectiveAnalyzer.ObjectiveProgressCandidateRole
                        .REQUIRED_ACTOR,
                analyzer.classifyPreFlipProgressCandidate(
                    scn.game(), VirtualTableScenario.DS, vader));
        assertTrue("The sole system occupier must be retained",
                analyzer
                    .isPreferredCountedObjectivePresenceForceLossCandidate(
                        scn.game(), VirtualTableScenario.DS, tie));

        scn.LSForceDrainAt(scn.GetLSStartingLocation());
        scn.PassAllResponses();
        assertTrue(scn.DSDecisionAvailable("Choose Force to lose"));
        AwaitingDecision decision = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        var offered = java.util.Arrays.asList(
                decision.getDecisionParameters().get("cardId"));
        assertTrue(offered.contains(
                Integer.toString(cantina.getCardId())));
        assertTrue(offered.contains(
                Integer.toString(vader.getCardId())));
        assertTrue(offered.contains(
                Integer.toString(tie.getCardId())));

        String loss = PublicBots.forGame(scn).decideBoth(scn);
        assertFalse("The site must survive the real loss decision",
                Integer.toString(cantina.getCardId()).equals(loss));
        assertFalse("The Dark Jedi must survive the real loss decision",
                Integer.toString(vader.getCardId()).equals(loss));
        assertFalse("The system ship must survive the real loss decision",
                Integer.toString(tie.getCardId()).equals(loss));
        scn.DSDecided(loss);
        scn.PassAllResponses();

        assertEquals(Zone.HAND, cantina.getZone());
        assertEquals(Zone.HAND, vader.getZone());
        assertEquals(Zone.HAND, tie.getZone());
    }

    @Test
    public void tsotPublicBotsUseTheBackOccupationRoute() {
        var scn = tsotScenario();
        var objective = scn.GetDSCard("objective");
        var system = scn.GetDSCard("system");
        var occupation = scn.GetDSCard("occupation");

        flipTwinSunsWithNativeTrigger(scn);
        scn.MoveCardsToBottomOfDSReserveDeck(occupation);
        keepExactlyDarkForce(scn, 4);

        String occupationRoute = scn.GetCardActionId(
                VirtualTableScenario.DS, objective,
                "Deploy Tatooine Occupation from Reserve Deck");
        assertNotNull(occupationRoute);
        var bots = PublicBots.forGame(scn);
        assertEquals("The back-side payoff route must fire",
                occupationRoute, bots.decideBoth(scn));
        scn.DSDecided(occupationRoute);
        scn.PassAllResponses();

        AwaitingDecision child = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        assertNotNull(child);
        String childResponse = bots.decideBoth(scn);
        assertSame("The route must select Tatooine Occupation",
                occupation,
                selectedPhysicalCard(scn, child, childResponse));
        scn.DSDecided(childResponse);
        scn.PassAllResponses();

        AwaitingDecision destination = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        if (destination != null) {
            String destinationResponse = bots.decideBoth(scn);
            assertSame("Occupation must deploy on the Tatooine system",
                    system,
                    selectedPhysicalCard(
                        scn, destination, destinationResponse));
            scn.DSDecided(destinationResponse);
            scn.PassAllResponses();
        }

        assertSame("The sole legal destination may be engine-selected",
                system, occupation.getAttachedTo());
        if (scn.GetAwaitingDecision(VirtualTableScenario.LS) != null) {
            scn.LSPass();
        }
        assertFalse("The once-per-game route must be exhausted",
                scn.DSCardActionAvailable(
                    objective,
                    "Deploy Tatooine Occupation from Reserve Deck"));
    }

    @Test
    public void tsotPublicBotsUseTheBackPeekAndTakeTheBetterCard() {
        var scn = tsotScenario();
        var objective = scn.GetDSCard("objective");
        var system = scn.GetDSCard("system");
        var mosEisley = scn.GetDSCard("mosEisley");
        var cantina = scn.GetDSCard("cantina");
        var occupation = scn.GetDSCard("occupation");
        var high = scn.GetDSCard("peekHigh");
        var low = scn.GetDSCard("peekLow");

        flipTwinSunsWithNativeTrigger(scn);
        scn.MoveOutOfPlay(occupation);
        scn.SkipToDSTurn(Phase.CONTROL);
        scn.MoveCardsToLocation(mosEisley, scn.GetLSFiller(1));
        scn.MoveCardsToLocation(cantina, scn.GetLSFiller(2));
        scn.MoveCardsToLocation(system, scn.GetLSCard("lsShip"));
        scn.MoveCardsToTopOfDSReserveDeck(low, high);
        assertTrue("Contested sites leave the back on its tie-safe side",
                objective.isFlipped());

        String peekAction = scn.GetCardActionId(
                VirtualTableScenario.DS, objective,
                "Peek at top two cards of Reserve Deck");
        assertNotNull(peekAction);
        var bots = PublicBots.forGame(scn);
        assertEquals("The free control-phase card selection must fire",
                peekAction, bots.decideBoth(scn));
        scn.DSDecided(peekAction);
        scn.PassAllResponses();

        AwaitingDecision child = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        assertNotNull(child);
        String childResponse = bots.decideBoth(scn);
        PhysicalCard selected = selectedPhysicalCard(
                scn, child, childResponse);
        assertSame("The bot must take the destiny-7 card",
                high, selected);
        scn.DSDecided(childResponse);
        scn.PassAllResponses();

        assertEquals(Zone.HAND, high.getZone());
        if (scn.GetAwaitingDecision(VirtualTableScenario.LS) != null) {
            scn.LSPass();
        }
        assertFalse("The once-per-control-phase action must be exhausted",
                scn.DSCardActionAvailable(
                    objective,
                    "Peek at top two cards of Reserve Deck"));
    }
}
