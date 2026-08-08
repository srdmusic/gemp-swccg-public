package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.ai.models.common.phase.AiActionSourceProvenance;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.framework.StartingSetup;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.PhysicalCardImpl;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Regression proof for the On The Verge mobile-system route touched by
 * the shared Set Your Course multiple-choice transport.
 */
public class OnTheVergeObjectiveRouteEngineContractTest {

    private static final StartingSetup ON_THE_VERGE = new StartingSetup() {
        @Override
        public HashMap<String, String> Cards() {
            return new HashMap<>() {{
                put("objective", "216_11");
                put("deathStar", "216_7");
                put("scarif", "216_13");
                put("citadelTower", "216_15");
                put("shieldGate", "216_18");
            }};
        }

        @Override
        public void Setup(VirtualTableScenario scn) {
            for (int guard = 0; guard < 20; guard++) {
                if (scn.DSHasCardChoiceAvailable(
                        scn.GetDSCard("deathStar"))) {
                    scn.DSChooseCard(scn.GetDSCard("deathStar"));
                } else if (scn.DSHasCardChoiceAvailable(
                        scn.GetDSCard("scarif"))) {
                    scn.DSChooseCard(scn.GetDSCard("scarif"));
                } else if (scn.DSHasCardChoiceAvailable(
                        scn.GetDSCard("citadelTower"))) {
                    scn.DSChooseCard(scn.GetDSCard("citadelTower"));
                } else if (scn.DSHasCardChoiceAvailable(
                        scn.GetDSCard("shieldGate"))) {
                    scn.DSChooseCard(scn.GetDSCard("shieldGate"));
                } else if (scn.DSDecisionAvailable("On which side")) {
                    scn.DSChoose("Left");
                } else {
                    break;
                }
            }
        }
    };

    private VirtualTableScenario scenario() {
        return scenario("207_20");
    }

    private VirtualTableScenario scenario(String krennicBlueprintId) {
        return scenario(krennicBlueprintId, "1_168");
    }

    private VirtualTableScenario scenario(
            String krennicBlueprintId, String vaderBlueprintId) {
        return scenario(krennicBlueprintId, vaderBlueprintId, false);
    }

    private VirtualTableScenario scenario(
            String krennicBlueprintId, String vaderBlueprintId,
            boolean includeLandingPad) {
        return scenario(
                krennicBlueprintId, vaderBlueprintId,
                includeLandingPad, Map.of());
    }

    private VirtualTableScenario scenario(
            String krennicBlueprintId, String vaderBlueprintId,
            boolean includeLandingPad,
            Map<String, String> extraDarkCards) {
        return new VirtualTableScenario(
                new HashMap<>() {{
                    put("convertedScarif", "209_23");
                }},
                new HashMap<>() {{
                    put("orbitDecoy", "3_151");
                    put("krennic", krennicBlueprintId);
                    put("scarifCommand", "216_16");
                    put("scarifBeach", "216_14");
                    if (includeLandingPad) {
                        put("scarifLandingPad", "216_17");
                    }
                    put("disposable", "1_182");
                    put("vader", vaderBlueprintId);
                    put("postFlipDeploy", "200_86");
                    putAll(extraDarkCards);
                }},
                16,
                16,
                StartingSetup.DefaultLSGroundLocation,
                ON_THE_VERGE,
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

        private TracedDecision decideBothWithRandoTrace(
                VirtualTableScenario scn) {
            var traces = new ArrayList<com.gempukku.swccgo.ai.models.common.trace.DecisionTrace>();
            try {
                var setter = rando.getClass().getDeclaredMethod(
                        "setDecisionTraceSinkForTesting",
                        com.gempukku.swccgo.ai.models.common.trace.TraceSink.class);
                setter.setAccessible(true);
                setter.invoke(rando,
                        new com.gempukku.swccgo.ai.models.common.trace.TraceSink() {
                            @Override
                            public boolean isEnabled() {
                                return true;
                            }

                            @Override
                            public void accept(
                                    com.gempukku.swccgo.ai.models.common.trace.DecisionTrace trace) {
                                traces.add(trace);
                            }
                        });
                String response = decideBoth(scn);
                assertEquals("Exactly one Rando trace must seal this decision",
                        1, traces.size());
                setter.invoke(rando,
                        com.gempukku.swccgo.ai.models.common.trace
                            .NoOpTraceSink.INSTANCE);
                return new TracedDecision(
                        response, traces.getFirst());
            } catch (ReflectiveOperationException e) {
                throw new AssertionError(e);
            }
        }
    }

    private record TracedDecision(
            String response,
            com.gempukku.swccgo.ai.models.common.trace.DecisionTrace trace) {
    }

    private static void chooseResultBoth(
            VirtualTableScenario scn, PublicBots bots,
            String textFragment, String expectedResult) {
        AwaitingDecision decision = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        assertNotNull(decision);
        assertTrue(decision.getText().contains(textFragment));
        String response = bots.decideBoth(scn);
        String[] results = decision.getDecisionParameters().get("results");
        assertNotNull(results);
        int index = Integer.parseInt(response);
        assertTrue(index >= 0 && index < results.length);
        assertEquals("results=" + Arrays.toString(results),
                expectedResult, results[index]);
        scn.DSDecided(response);
    }

    private static void leaveOneDarkForce(VirtualTableScenario scn) {
        leaveDarkForce(scn, 1);
    }

    private static void leaveDarkForce(
            VirtualTableScenario scn, int amount) {
        while (scn.GetDSForcePileCount() > amount) {
            scn.MoveCardsToTopOfDSUsedPile(scn.GetTopOfDSForcePile());
        }
        if (scn.GetDSForcePileCount() < amount) {
            scn.DSActivateForceCheat(
                    amount - scn.GetDSForcePileCount());
        }
    }

    private static PhysicalCard selectedPhysicalCard(
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
                    String[] blueprints = decision
                            .getDecisionParameters().get("blueprintId");
                    var reserve = scn.gameState().getReserveDeck(
                            VirtualTableScenario.DS);
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

    private static void keepOnlyDarkHandCards(
            VirtualTableScenario scn, PhysicalCard... keep) {
        Set<PhysicalCard> protectedCards = Set.of(keep);
        var remove = new ArrayList<PhysicalCardImpl>();
        for (PhysicalCard card : scn.gameState().getHand(
                VirtualTableScenario.DS)) {
            if (card instanceof PhysicalCardImpl physical
                    && !protectedCards.contains(card)) {
                remove.add(physical);
            }
        }
        for (PhysicalCardImpl card : remove) {
            scn.MoveOutOfPlay(card);
        }
    }

    private static void resolveScarifLocationDeployment(
            VirtualTableScenario scn, PublicBots bots,
            PhysicalCard expected) {
        for (int guard = 0; guard < 12; guard++) {
            if (scn.DSDecisionAvailable("On which side")) {
                scn.DSChoose("Left");
            } else if (expected.getZone() == Zone.LOCATIONS
                    && scn.AwaitingDSDeployPhaseActions()) {
                break;
            } else if (scn.GetAwaitingDecision(
                    VirtualTableScenario.DS) != null) {
                scn.DSDecided(bots.decideBoth(scn));
            } else if (scn.GetAwaitingDecision(
                    VirtualTableScenario.LS) != null) {
                scn.LSPass();
            } else {
                scn.PassAllResponses();
            }
        }
        assertSame(Zone.LOCATIONS, expected.getZone());
    }

    private static void moveLocationAdjacentTo(
            VirtualTableScenario scn, PhysicalCardImpl location,
            PhysicalCardImpl neighbor) {
        scn.RemoveCardZone(location);
        var placement = scn.gameState().getLocationPlacement(
                        scn.game(), location,
                        neighbor.getPartOfSystem(), null)
                .stream()
                .filter(candidate -> candidate.getOtherCard() == neighbor)
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Expected legal placement adjacent to "
                                + neighbor.getTitle()));
        scn.gameState().addLocationToTable(
                scn.game(), location, placement);
    }

    private static void chooseScarifBattlegroundRoute(
            VirtualTableScenario scn, PublicBots bots,
            PhysicalCardImpl objective,
            PhysicalCardImpl commandCenter) {
        String route = scn.GetCardActionId(
                VirtualTableScenario.DS, objective,
                "Deploy a Scarif battleground");
        assertNotNull(route);
        TracedDecision traced = bots.decideBothWithRandoTrace(scn);
        assertEquals("The native objective pull must lead the deploy phase",
                route, traced.response());
        assertTrue("The source-specific Scarif route band must fire",
                traced.trace().getOperations().stream()
                    .anyMatch(operation -> route.equals(
                                operation.getActionId())
                            && "OBJECTIVE.OTVOG.SCARIF_ROUTE"
                                .equals(operation.getRuleId().id())));
        scn.DSDecided(route);
        scn.PassAllResponses();

        AwaitingDecision child = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        assertNotNull(child);
        assertEquals("ARBITRARY_CARDS", child.getDecisionType().name());
        assertEquals("Choose card to deploy from Reserve Deck",
                child.getText());
        assertEquals("1", child.getDecisionParameters().get("min")[0]);
        assertEquals("1", child.getDecisionParameters().get("max")[0]);
        var actionState = scn.gameState().getTopGameTextActionState();
        assertNotNull(actionState);
        var liveAction = actionState.getGameTextAction();
        assertNotNull(liveAction);
        assertSame(objective, liveAction.getActionSource());
        assertEquals("Deploy a Scarif battleground",
                liveAction.getText());
        assertEquals(GameTextActionId
                .ON_THE_VERGE_OF_GREATNESS__DEPLOY_SCARIF_BATTLEGROUND,
                liveAction.getGameTextActionId());
        String selected = bots.decideBoth(scn);
        assertSame("Command Center is the native Krennic route",
                commandCenter,
                selectedPhysicalCard(scn, child, selected));
        scn.DSDecided(selected);
        scn.PassAllResponses();
        resolveScarifLocationDeployment(
                scn, bots, commandCenter);
    }

    private static void deployKrennicFromCommandCenter(
            VirtualTableScenario scn, PublicBots bots,
            PhysicalCardImpl commandCenter,
            PhysicalCardImpl krennic) {
        String deploy = scn.GetCardActionId(
                VirtualTableScenario.DS, commandCenter,
                "Deploy Krennic from Reserve Deck");
        assertNotNull(deploy);
        assertEquals(deploy, bots.decideBoth(scn));
        scn.DSDecided(deploy);
        scn.PassAllResponses();

        AwaitingDecision child = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        assertNotNull(child);
        assertEquals("ARBITRARY_CARDS", child.getDecisionType().name());
        assertEquals("Choose card to deploy from Reserve Deck",
                child.getText());
        var actionState = scn.gameState().getTopGameTextActionState();
        assertNotNull(actionState);
        var liveAction = actionState.getGameTextAction();
        assertNotNull(liveAction);
        assertSame(commandCenter, liveAction.getActionSource());
        assertEquals(GameTextActionId
                .SCARIF_COMMAND_CENTER__DOWNLOAD_KRENNIC,
                liveAction.getGameTextActionId());
        assertEquals("Deploy Krennic from Reserve Deck",
                liveAction.getText());
        String selected = bots.decideBoth(scn);
        assertSame(krennic,
                selectedPhysicalCard(scn, child, selected));
        scn.DSDecided(selected);
        scn.PassAllResponses();

        assertSame(Zone.AT_LOCATION, krennic.getZone());
        assertSame(commandCenter,
                scn.game().getModifiersQuerying()
                    .getLocationThatCardIsAt(
                        scn.gameState(), krennic));
    }

    private static void moveDeathStarFromSixToScarif(
            VirtualTableScenario scn, PublicBots bots,
            PhysicalCardImpl deathStar,
            PhysicalCardImpl scarif,
            PhysicalCardImpl orbitDecoy) {
        scn.MoveLocationToTable(orbitDecoy);
        scn.SkipToPhase(Phase.MOVE);
        if (scn.AwaitingLSMovePhaseActions()) {
            scn.LSPass();
        }
        String move = scn.GetCardActionId(
                VirtualTableScenario.DS, deathStar,
                "Move using hyperspeed");
        assertNotNull(move);
        assertEquals(move, bots.decideBoth(scn));
        scn.DSDecided(move);
        chooseResultBoth(scn, bots,
                "Choose parsec to move to", "7");
        chooseResultBoth(scn, bots,
                "Choose destination for", "Orbit a system");
        assertTrue(scn.DSHasCardChoiceAvailable(scarif));
        assertTrue(scn.DSHasCardChoiceAvailable(orbitDecoy));
        assertEquals(Integer.toString(scarif.getCardId()),
                bots.decideBoth(scn));
        scn.DSDecided(Integer.toString(scarif.getCardId()));
        scn.PassAllResponses();
    }

    private static void startFlippedAtCommandCenter(
            VirtualTableScenario scn) {
        var objective = scn.GetDSCard("objective");
        var deathStar = scn.GetDSCard("deathStar");
        var command = scn.GetDSCard("scarifCommand");
        var krennic = scn.GetDSCard("krennic");
        scn.MoveCardsToDSHand(krennic);
        scn.StartGame();
        scn.MoveLocationToTable(command);
        deathStar.setParsec(7);
        deathStar.setSystemOrbited(Title.Scarif);
        scn.MoveCardsToDSHand(krennic);
        scn.DSActivateForceCheat(3);
        scn.SkipToDSTurn(Phase.DEPLOY);
        scn.DSDeployCardAndPassResponses(krennic, command);
        assertTrue("A real table change must flip the objective",
                objective.isFlipped());
    }

    private static void loseDarkCardFromTableAndPass(
            VirtualTableScenario scn,
            PhysicalCardImpl source,
            PhysicalCardImpl target) {
        scn.SkipToDSTurn(Phase.BATTLE);
        assertTrue(scn.AwaitingDSBattlePhaseActions());
        var lossAction = new com.gempukku.swccgo.logic.actions
                .TopLevelGameTextAction(
                    source, VirtualTableScenario.DS,
                    source.getCardId());
        scn.DSExecuteAdHocEffect(
                source,
                new com.gempukku.swccgo.logic.effects
                    .LoseCardFromTableEffect(lossAction, target));
        scn.PassAllResponses();
    }

    private void publicBotsCompleteFundedKrennicChain(
            String krennicBlueprintId, int force) {
        var scn = scenario(krennicBlueprintId);
        var objective = scn.GetDSCard("objective");
        var deathStar = scn.GetDSCard("deathStar");
        var scarif = scn.GetDSCard("scarif");
        var command = scn.GetDSCard("scarifCommand");
        var beach = scn.GetDSCard("scarifBeach");
        var orbitDecoy = scn.GetDSCard("orbitDecoy");
        var krennic = scn.GetDSCard("krennic");

        scn.StartGame();
        deathStar.setParsec(6);
        deathStar.setSystemOrbited(null);
        // ADJUSTED 2026-08-08 (passivity fix, m01683): pin the orbit decoy into
        // the Reserve Deck with the other fixture pulls. Left to the shuffle, Ord
        // Mantell sometimes sat in the FORCE PILE when moveDeathStarFromSixToScarif
        // later cheats it onto the table (MoveLocationToTable pulls it from
        // whatever zone it occupies), silently shrinking the pinned five-Force
        // pile by one and flaking the force - 4 assert (~15-25% of runs, engine
        // shuffle randomness only — reproduced at HEAD with every decision
        // scripted and no AI in the loop). Bots spend exactly pull 0 + Krennic 3
        // + move 1 in ALL runs; the chain contract itself was never violated.
        // scn.MoveCardsToBottomOfDSReserveDeck(
        //         command, beach, krennic);
        scn.MoveCardsToBottomOfDSReserveDeck(
                command, beach, krennic, orbitDecoy);
        scn.SkipToDSTurn(Phase.DEPLOY);
        leaveDarkForce(scn, force);
        if (scn.AwaitingLSDeployPhaseActions()) {
            scn.LSPass();
        }
        PublicBots bots = PublicBots.forGame(scn);
        chooseScarifBattlegroundRoute(
                scn, bots, objective, command);
        deployKrennicFromCommandCenter(
                scn, bots, command, krennic);
        assertEquals("Krennic costs exactly 3 Force",
                force - 3, scn.GetDSForcePileCount());
        moveDeathStarFromSixToScarif(
                scn, bots, deathStar, scarif, orbitDecoy);

        assertEquals(force - 4, scn.GetDSForcePileCount());
        assertEquals(Title.Scarif, deathStar.getSystemOrbited());
        assertTrue("The complete native chain must flip",
                objective.isFlipped());
    }

    @Test
    public void publicBotsChooseScarifThroughGenericOrbitAndCardSelection() {
        var scn = scenario();
        var objective = scn.GetDSCard("objective");
        var deathStar = scn.GetDSCard("deathStar");
        var scarif = scn.GetDSCard("scarif");
        var convertedScarif = scn.GetLSCard("convertedScarif");
        var citadelTower = scn.GetDSCard("citadelTower");
        var shieldGate = scn.GetDSCard("shieldGate");
        var orbitDecoy = scn.GetDSCard("orbitDecoy");
        var krennic = scn.GetDSCard("krennic");
        var scarifCommand = scn.GetDSCard("scarifCommand");
        var postFlipDeploy = scn.GetDSCard("postFlipDeploy");

        scn.StartGame();
        assertTrue(scn.IsAttachedTo(scarif, shieldGate));
        assertEquals(Title.Scarif, citadelTower.getPartOfSystem());
        assertEquals(4, deathStar.getParsec());
        assertTrue(objective.getZone().isInPlay());

        scn.MoveLocationToTable(convertedScarif);
        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(
                scn.game(), VirtualTableScenario.DS,
                com.gempukku.swccgo.common.Side.DARK);
        assertTrue("Source-title routing must survive a converted Scarif system",
                analyzer.isOnTheVergeScarifOrbitCandidate(
                        convertedScarif));
        scn.MoveLocationToTable(orbitDecoy);
        scn.MoveCardsToLocation(citadelTower, krennic);
        scn.SkipToPhase(Phase.MOVE);
        leaveOneDarkForce(scn);
        if (scn.AwaitingLSMovePhaseActions()) {
            scn.LSPass();
        }

        PublicBots bots = PublicBots.forGame(scn);
        String firstMove = scn.GetCardActionId(
                VirtualTableScenario.DS, deathStar,
                "Move using hyperspeed");
        assertNotNull(firstMove);
        assertEquals(firstMove, bots.decideBoth(scn));
        scn.DSDecided(firstMove);
        chooseResultBoth(scn, bots,
                "Choose parsec to move to", "6");
        scn.PassAllResponses();
        assertEquals(6, deathStar.getParsec());
        assertEquals(null, deathStar.getSystemOrbited());
        assertEquals(0, scn.GetDSForcePileCount());

        scn.SkipToDSTurn(Phase.MOVE);
        leaveOneDarkForce(scn);
        if (scn.AwaitingLSMovePhaseActions()) {
            scn.LSPass();
        }
        String secondMove = scn.GetCardActionId(
                VirtualTableScenario.DS, deathStar,
                "Move using hyperspeed");
        assertNotNull(secondMove);
        assertEquals(secondMove, bots.decideBoth(scn));
        scn.DSDecided(secondMove);

        chooseResultBoth(scn, bots,
                "Choose parsec to move to", "7");
        chooseResultBoth(scn, bots,
                "Choose destination for", "Orbit a system");
        assertTrue(scn.DSHasCardChoiceAvailable(convertedScarif));
        assertTrue(scn.DSHasCardChoiceAvailable(orbitDecoy));
        assertEquals(Integer.toString(convertedScarif.getCardId()),
                bots.decideBoth(scn));
        scn.DSDecided(Integer.toString(convertedScarif.getCardId()));
        scn.PassAllResponses();

        assertEquals(7, deathStar.getParsec());
        assertEquals(Title.Scarif, deathStar.getSystemOrbited());
        assertEquals(0, scn.GetDSForcePileCount());
        assertTrue("The native actor plus orbit gate must flip On The Verge",
                objective.isFlipped());

        // The back no longer needs Death Star movement. With exactly the five
        // Force needed for Mara, both public bots must deploy her rather than
        // hoard one Force for the retired front-side Scarif route.
        scn.MoveCardsToDSHand(postFlipDeploy);
        scn.MoveCardsToBottomOfDSReserveDeck(scarifCommand);
        scn.SkipToDSTurn(Phase.DEPLOY);
        leaveDarkForce(scn, 5);
        if (scn.AwaitingLSDeployPhaseActions()) {
            scn.LSPass();
        }
        String postFlipObjectiveAction = scn.GetCardActionId(
                VirtualTableScenario.DS, objective,
                "Deploy a Scarif battleground");
        assertNotNull(postFlipObjectiveAction);
        scn.DSDecided(postFlipObjectiveAction);
        scn.PassAllResponses();
        if (scn.DSHasCardChoiceAvailable(scarifCommand)) {
            scn.DSChooseCard(scarifCommand);
            scn.PassAllResponses();
        }
        if (scn.DSDecisionAvailable("On which side")) {
            scn.DSChoose("Left");
        }
        scn.PassAllResponses();
        if (scn.AwaitingLSDeployPhaseActions()) {
            scn.LSPass();
        }
        assertTrue("phase=" + scn.gameState().getCurrentPhase()
                        + "; DS=" + (scn.GetAwaitingDecision(
                            VirtualTableScenario.DS) != null
                            ? scn.GetAwaitingDecision(
                                VirtualTableScenario.DS).getText()
                            : "none")
                        + "; LS=" + (scn.GetAwaitingDecision(
                            VirtualTableScenario.LS) != null
                            ? scn.GetAwaitingDecision(
                                VirtualTableScenario.LS).getText()
                            : "none"),
                scn.AwaitingDSDeployPhaseActions());
        String postFlipDeployAction = scn.GetCardActionId(
                VirtualTableScenario.DS, postFlipDeploy, "Deploy");
        assertNotNull(postFlipDeployAction);
        assertEquals("The back face must release the obsolete move reserve",
                postFlipDeployAction, bots.decideBoth(scn));
        scn.DSDecided(postFlipDeployAction);
        if (scn.DSHasCardChoiceAvailable(citadelTower)) {
            scn.DSChooseCard(citadelTower);
        }
        scn.PassAllResponses();

        // Once the native objective is on its back, Scarif orbit is no longer
        // part of its survival condition. Recreate the old adversarial 4 ->
        // {2, 6} child prompt and prove neither public bot keeps applying the
        // front-only V79 route score after the flip.
        deathStar.setSystemOrbited(null);
        deathStar.setParsec(4);
        scn.SkipToDSTurn(Phase.MOVE);
        leaveOneDarkForce(scn);
        if (scn.AwaitingLSMovePhaseActions()) {
            scn.LSPass();
        }
        String postFlipMove = scn.GetCardActionId(
                VirtualTableScenario.DS, deathStar,
                "Move using hyperspeed");
        assertNotNull(postFlipMove);
        assertEquals("The back face must not initiate obsolete Scarif movement",
                "", bots.decideBoth(scn));
        scn.DSDecided(postFlipMove);

        AwaitingDecision postFlipPrompt = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        assertNotNull(postFlipPrompt);
        assertTrue(postFlipPrompt.getText().contains(
                "Choose parsec to move to"));
        String[] postFlipResults = postFlipPrompt
                .getDecisionParameters().get("results");
        assertNotNull(postFlipResults);
        assertEquals("The fixture must keep the tempting parsec 6 away from option zero: "
                        + Arrays.toString(postFlipResults),
                "2", postFlipResults[0]);
        String postFlipResponse = bots.decideBoth(scn);
        assertEquals("The back face must release the old Scarif child score: "
                        + Arrays.toString(postFlipResults),
                "0", postFlipResponse);
    }

    @Test
    public void directorKrennicCompletesTheExactFourForceChain() {
        publicBotsCompleteFundedKrennicChain("207_20", 4);
    }

    @Test
    public void commandantKrennicCompletesTheFiveForceChain() {
        publicBotsCompleteFundedKrennicChain("209_36", 5);
    }

    @Test
    public void publicBotsDeferKrennicAtThreeAndMoveDeathStar() {
        var scn = scenario("207_20");
        var objective = scn.GetDSCard("objective");
        var deathStar = scn.GetDSCard("deathStar");
        var command = scn.GetDSCard("scarifCommand");
        var beach = scn.GetDSCard("scarifBeach");
        var krennic = scn.GetDSCard("krennic");

        scn.StartGame();
        deathStar.setParsec(6);
        deathStar.setSystemOrbited(null);
        scn.MoveCardsToBottomOfDSReserveDeck(
                command, beach, krennic);
        scn.SkipToDSTurn(Phase.DEPLOY);
        leaveDarkForce(scn, 3);
        if (scn.AwaitingLSDeployPhaseActions()) {
            scn.LSPass();
        }
        PublicBots bots = PublicBots.forGame(scn);
        chooseScarifBattlegroundRoute(
                scn, bots, objective, command);

        String krennicAction = scn.GetCardActionId(
                VirtualTableScenario.DS, command,
                "Deploy Krennic from Reserve Deck");
        assertNotNull("The engine must offer the exact-cost Krennic action",
                krennicAction);
        assertEquals("Three Force must preserve the later Death Star move",
                "", bots.decideBoth(scn));
        assertEquals(3, scn.GetDSForcePileCount());

        scn.SkipToPhase(Phase.MOVE);
        if (scn.AwaitingLSMovePhaseActions()) {
            scn.LSPass();
        }
        String move = scn.GetCardActionId(
                VirtualTableScenario.DS, deathStar,
                "Move using hyperspeed");
        assertNotNull(move);
        assertEquals(move, bots.decideBoth(scn));
        assertFalse(objective.isFlipped());
    }

    @Test
    public void orbitCompleteAllowsThreeForceCommandantAndFlips() {
        var scn = scenario("209_36");
        var objective = scn.GetDSCard("objective");
        var deathStar = scn.GetDSCard("deathStar");
        var command = scn.GetDSCard("scarifCommand");
        var krennic = scn.GetDSCard("krennic");

        scn.StartGame();
        scn.MoveLocationToTable(command);
        deathStar.setParsec(7);
        deathStar.setSystemOrbited(Title.Scarif);
        scn.MoveCardsToBottomOfDSReserveDeck(krennic);
        scn.SkipToDSTurn(Phase.DEPLOY);
        leaveDarkForce(scn, 3);
        if (scn.AwaitingLSDeployPhaseActions()) {
            scn.LSPass();
        }
        PublicBots bots = PublicBots.forGame(scn);
        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertTrue(analyzer.isOnTheVergeKrennicDeployAction(
                scn.game(), VirtualTableScenario.DS,
                command, "Deploy Krennic from Reserve Deck"));
        assertSame(krennic,
                analyzer.findOnTheVergeLegalKrennicInReserve(
                    scn.game(), VirtualTableScenario.DS, command));
        assertEquals(Integer.valueOf(3),
                analyzer.getOnTheVergeKrennicDeployCost(
                    scn.game(), VirtualTableScenario.DS, command));
        assertEquals(Integer.valueOf(3),
                analyzer.getOnTheVergeForceAvailable(
                    scn.game(), VirtualTableScenario.DS));
        assertEquals(Integer.valueOf(0),
                analyzer.getOnTheVergeCurrentMoveForceReserve(
                    scn.game(), VirtualTableScenario.DS));
        assertNotNull("The redundant Scarif pull must remain live for this regression",
                scn.GetCardActionId(
                    VirtualTableScenario.DS, objective,
                    "Deploy a Scarif battleground"));
        deployKrennicFromCommandCenter(
                scn, bots, command, krennic);

        assertEquals(0, scn.GetDSForcePileCount());
        assertTrue("No obsolete move reserve may block the native flip",
                objective.isFlipped());
    }

    @Test
    public void publicBotsDeployTarkinToTheScarifGateAndFlip() {
        var scn = scenario(
                "207_20", "1_168", false,
                Map.of("tarkin", "102_11"));
        var objective = scn.GetDSCard("objective");
        var deathStar = scn.GetDSCard("deathStar");
        var command = scn.GetDSCard("scarifCommand");
        var beach = scn.GetDSCard("scarifBeach");
        var krennic = scn.GetDSCard("krennic");
        var tarkin = scn.GetDSCard("tarkin");

        scn.StartGame();
        scn.MoveLocationToTable(command);
        deathStar.setParsec(7);
        deathStar.setSystemOrbited(Title.Scarif);
        scn.MoveOutOfPlay(beach);
        scn.MoveOutOfPlay(krennic);
        scn.MoveCardsToDSHand(tarkin);
        keepOnlyDarkHandCards(scn, tarkin);
        scn.SkipToDSTurn(Phase.DEPLOY);
        leaveDarkForce(scn, 4);
        if (scn.AwaitingLSDeployPhaseActions()) {
            scn.LSPass();
        }

        PublicBots bots = PublicBots.forGame(scn);
        String deployTarkin = scn.GetCardActionId(
                VirtualTableScenario.DS, tarkin, "Deploy");
        assertNotNull(deployTarkin);
        AwaitingDecision parent = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        TracedDecision traced = bots.decideBothWithRandoTrace(scn);
        assertEquals("Tarkin must beat Pass when orbit is complete; decision="
                        + (parent != null
                            ? parent.getDecisionParameters() : null)
                        + "; operations="
                        + traced.trace().getOperations(),
                deployTarkin, traced.response());
        scn.DSDecided(deployTarkin);

        AwaitingDecision destination = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        assertNotNull(destination);
        TracedDecision destinationTrace =
                bots.decideBothWithRandoTrace(scn);
        String response = destinationTrace.response();
        PhysicalCard selectedDestination =
                selectedPhysicalCard(scn, destination, response);
        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertTrue("A source-legal Scarif battleground must beat other sites",
                selectedDestination != null
                    && analyzer.isFlipGateLocation(
                        scn.game(), VirtualTableScenario.DS,
                        selectedDestination));
        assertTrue("The typed actor-to-gate child score must fire",
                destinationTrace.trace().getOperations().stream()
                    .anyMatch(operation -> response.equals(
                                operation.getActionId())
                            && "DEPLOY.OBJECTIVE.ACTOR_RUNTIME_LOCATION"
                                .equals(operation.getRuleId().id())));
        scn.DSDecided(response);
        scn.PassAllResponses();

        assertSame(selectedDestination,
                scn.game().getModifiersQuerying()
                    .getLocationThatCardIsAt(
                        scn.gameState(), tarkin));
        assertTrue("Tarkin is a source-legal alternative to Krennic",
                objective.isFlipped());
    }

    @Test
    public void aboardActorsUseAtForTheFrontAndStableBack() {
        var scn = scenario(
                "207_20", "9_113", false,
                Map.of(
                    "tarkin", "102_11",
                    "walker", "3_157",
                    "trooper", "1_194"));
        var objective = scn.GetDSCard("objective");
        var deathStar = scn.GetDSCard("deathStar");
        var command = scn.GetDSCard("scarifCommand");
        var tarkin = scn.GetDSCard("tarkin");
        var vader = scn.GetDSCard("vader");
        var walker = scn.GetDSCard("walker");
        var trooper = scn.GetDSCard("trooper");

        scn.StartGame();
        scn.MoveLocationToTable(command);
        deathStar.setParsec(7);
        deathStar.setSystemOrbited(Title.Scarif);
        scn.MoveCardsToLocation(command, walker, tarkin, vader);
        scn.BoardAsPassenger(walker, tarkin, vader);

        assertEquals("An enclosed passenger is not present at its site",
                null,
                scn.game().getModifiersQuerying()
                    .getLocationThatCardIsPresentAt(
                        scn.gameState(), tarkin));
        assertSame("The card source uses at, which includes aboard actors",
                command,
                scn.game().getModifiersQuerying()
                    .getLocationThatCardIsAt(
                        scn.gameState(), tarkin));

        var front = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        front.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertEquals("Only Tarkin qualifies for the front actor gate",
                1,
                front.countFlipGateActorsAtLocation(
                    scn.game(), VirtualTableScenario.DS, command));

        scn.MoveCardsToDSHand(trooper);
        scn.DSActivateForceCheat(1);
        scn.SkipToDSTurn(Phase.DEPLOY);
        scn.DSDeployCardAndPassResponses(trooper, command);
        assertTrue("A real table change must honor aboard Tarkin",
                objective.isFlipped());

        loseDarkCardFromTableAndPass(scn, objective, tarkin);
        assertTrue("Aboard leader Vader must keep the back stable",
                objective.isFlipped());

        var rando = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.strategy
                .ObjectiveAnalyzer();
        rando.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        chosen.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertTrue("Moving the sole aboard leader would break the hold",
                rando.wouldDepartureTriggerStayFlippedFailure(
                    scn.game(), VirtualTableScenario.DS, vader));
        assertEquals(
                rando.wouldDepartureTriggerStayFlippedFailure(
                    scn.game(), VirtualTableScenario.DS, vader),
                chosen.wouldDepartureTriggerStayFlippedFailure(
                    scn.game(), VirtualTableScenario.DS, vader));
    }

    @Test
    public void opponentDuplicateDeathStarReleasesHardLossRetention() {
        var scn = scenario(
                "207_20", "1_168", false,
                Map.of("duplicateDeathStar", "2_143"));
        var objective = scn.GetDSCard("objective");
        var deathStar = scn.GetDSCard("deathStar");
        var command = scn.GetDSCard("scarifCommand");
        var krennic = scn.GetDSCard("krennic");
        var duplicate = scn.GetDSCard("duplicateDeathStar");
        scn.StartGame();
        scn.MoveLocationToTable(command);
        scn.MoveCardsToLocation(command, krennic);
        scn.SkipToDSTurn(Phase.BATTLE);
        var flipAction = new com.gempukku.swccgo.logic.actions
                .TopLevelGameTextAction(
                    objective, VirtualTableScenario.DS,
                    objective.getCardId());
        scn.DSExecuteAdHocEffect(
                objective,
                new com.gempukku.swccgo.logic.effects.FlipCardEffect(
                    flipAction, objective));
        scn.PassAllResponses();
        assertTrue(objective.isFlipped());
        duplicate.setOwner(VirtualTableScenario.LS);
        scn.MoveLocationToTable(duplicate);

        var rando = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.strategy
                .ObjectiveAnalyzer();
        rando.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        chosen.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertFalse("Source canSpot is owner-agnostic",
                rando.isOnTheVergeHardLossDependency(
                    scn.game(), VirtualTableScenario.DS, deathStar));
        assertEquals(
                rando.isOnTheVergeHardLossDependency(
                    scn.game(), VirtualTableScenario.DS, deathStar),
                chosen.isOnTheVergeHardLossDependency(
                    scn.game(), VirtualTableScenario.DS, deathStar));

        loseDarkCardFromTableAndPass(scn, objective, deathStar);
        assertTrue("The opponent copy still satisfies native canSpot",
                objective.isFlipped());
        assertTrue(objective.getZone().isInPlay());
    }

    @Test
    public void inactiveDependencyDoesNotMasqueradeAsNativeSpot() {
        var scn = scenario("207_20");
        var objective = scn.GetDSCard("objective");
        var deathStar = scn.GetDSCard("deathStar");
        startFlippedAtCommandCenter(scn);
        deathStar.setSuspended(true);

        assertFalse("Ordinary source spotting excludes a suspended Death Star",
                com.gempukku.swccgo.cards.GameConditions.canSpot(
                    scn.game(), objective, Filters.Death_Star_system));
        var rando = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.strategy
                .ObjectiveAnalyzer();
        rando.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        chosen.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertFalse("An inactive card is not the dependency keeping the back alive",
                rando.isOnTheVergeHardLossDependency(
                    scn.game(), VirtualTableScenario.DS, deathStar));
        assertEquals(
                rando.isOnTheVergeHardLossDependency(
                    scn.game(), VirtualTableScenario.DS, deathStar),
                chosen.isOnTheVergeHardLossDependency(
                    scn.game(), VirtualTableScenario.DS, deathStar));
    }

    @Test
    public void exactKrennicChildOutranksAlertMyStarDestroyerRouting() {
        var scn = scenario(
                "207_20", "1_168", false,
                Map.of("amsd", "208_38"));
        var objective = scn.GetDSCard("objective");
        var deathStar = scn.GetDSCard("deathStar");
        var command = scn.GetDSCard("scarifCommand");
        var beach = scn.GetDSCard("scarifBeach");
        var krennic = scn.GetDSCard("krennic");
        var amsd = scn.GetDSCard("amsd");

        scn.StartGame();
        scn.MoveLocationToTable(command);
        scn.MoveCardsToDSSideOfTable(amsd);
        scn.MoveOutOfPlay(beach);
        deathStar.setParsec(7);
        deathStar.setSystemOrbited(Title.Scarif);
        scn.MoveCardsToBottomOfDSReserveDeck(krennic);
        scn.SkipToDSTurn(Phase.DEPLOY);
        leaveDarkForce(scn, 3);
        if (scn.AwaitingLSDeployPhaseActions()) {
            scn.LSPass();
        }

        PublicBots bots = PublicBots.forGame(scn);
        deployKrennicFromCommandCenter(
                scn, bots, command, krennic);
        assertTrue("AMSD must not hijack the exact Krennic child",
                objective.isFlipped());
    }

    @Test
    public void publicForceLossPreservesTheOnlyKrennicRoute() {
        var scn = scenario("207_20");
        var krennic = scn.GetDSCard("krennic");
        var fodder = scn.GetDSCard("disposable");

        scn.MoveCardsToDSHand(krennic, fodder);
        scn.StartGame();
        scn.MoveCardsToDSHand(krennic, fodder);
        keepOnlyDarkHandCards(scn, krennic, fodder);
        scn.MoveCardsToLocation(
                scn.GetLSStartingLocation(), scn.GetLSFiller(1));
        scn.LSActivateForceCheat(4);
        scn.SkipToLSTurn(Phase.CONTROL);

        var rando = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.strategy
                .ObjectiveAnalyzer();
        rando.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        chosen.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertEquals(ObjectiveAnalyzer.ObjectiveProgressCandidateRole
                        .REQUIRED_ACTOR,
                rando.classifyPreFlipProgressCandidate(
                    scn.game(), VirtualTableScenario.DS, krennic));
        assertEquals(
                rando.classifyPreFlipProgressCandidate(
                    scn.game(), VirtualTableScenario.DS, krennic),
                chosen.classifyPreFlipProgressCandidate(
                    scn.game(), VirtualTableScenario.DS, krennic));

        scn.LSForceDrainAt(scn.GetLSStartingLocation());
        scn.PassAllResponses();
        AwaitingDecision loss = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        assertNotNull(loss);
        assertTrue(loss.getText().contains("Choose Force to lose"));
        assertTrue(Arrays.asList(
                loss.getDecisionParameters().get("cardId"))
            .contains(Integer.toString(krennic.getCardId())));
        String response = PublicBots.forGame(scn).decideBoth(scn);
        assertFalse("The only Krennic route must survive real Force loss",
                Integer.toString(krennic.getCardId()).equals(response));
        scn.DSDecided(response);
        scn.PassAllResponses();
        assertSame(Zone.HAND, krennic.getZone());
    }

    @Test
    public void publicBattleForfeitPreservesTheLastScarifLeader() {
        var scn = scenario("207_20");
        var objective = scn.GetDSCard("objective");
        var command = scn.GetDSCard("scarifCommand");
        var krennic = scn.GetDSCard("krennic");
        var disposable = scn.GetDSCard("disposable");
        startFlippedAtCommandCenter(scn);
        scn.MoveCardsToLocation(command, disposable);
        scn.MoveCardsToLocation(command,
                scn.GetLSFillerRange(1, 8));

        var rando = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.strategy
                .ObjectiveAnalyzer();
        rando.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        chosen.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertEquals(ObjectiveAnalyzer.FlipGateFormationRole
                        .LAST_OBJECTIVE_SURVIVAL_ACTOR,
                rando.classifyGateFormationPieceIfRemoved(
                    scn.game(), VirtualTableScenario.DS, krennic));
        assertEquals(ObjectiveAnalyzer.FlipGateFormationRole.NONE,
                rando.classifyGateFormationPieceIfRemoved(
                    scn.game(), VirtualTableScenario.DS, disposable));
        assertEquals(
                rando.classifyGateFormationPieceIfRemoved(
                    scn.game(), VirtualTableScenario.DS, krennic),
                chosen.classifyGateFormationPieceIfRemoved(
                    scn.game(), VirtualTableScenario.DS, krennic));

        scn.LSActivateForceCheat(1);
        scn.SkipToLSTurn(Phase.BATTLE);
        scn.LSInitiateBattle(command);
        scn.SkipToDamageSegment(false);
        assertTrue(scn.AwaitingDSAttritionPayment()
                || scn.AwaitingDSBattleDamagePayment());
        AwaitingDecision forfeit = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        assertNotNull(forfeit);
        assertTrue(forfeit.getText().contains("battle to forfeit"));
        var offered = Arrays.asList(
                forfeit.getDecisionParameters().get("cardId"));
        assertTrue(offered.contains(
                Integer.toString(krennic.getCardId())));
        assertTrue(offered.contains(
                Integer.toString(disposable.getCardId())));
        var formation = com.gempukku.swccgo.ai.models.common.phase
                .BattleForfeitFacts.readFlipGateFormationSelection(
                    offered, scn.gameState(), scn.game(),
                    VirtualTableScenario.DS, rando, false, 1);
        assertEquals(ObjectiveAnalyzer.FlipGateFormationRole
                        .LAST_OBJECTIVE_SURVIVAL_ACTOR,
                formation.roleFor(Integer.toString(krennic.getCardId())));
        assertTrue(formation.hasUnprotectedLegalAlternative());
        String selected = PublicBots.forGame(scn).decideBoth(scn);
        assertFalse("The last Scarif leader must survive while another legal loss exists",
                Integer.toString(krennic.getCardId()).equals(selected));
        assertTrue(objective.isFlipped());
    }

    @Test
    public void backUsesOneCardRetrievalAndReleasesFrontMoveReserve() {
        var scn = scenario("207_20");
        var objective = scn.GetDSCard("objective");
        var deathStar = scn.GetDSCard("deathStar");
        var lost = scn.GetDSCard("disposable");
        startFlippedAtCommandCenter(scn);
        scn.MoveCardsToTopOfDSLostPile(lost);
        deathStar.setSystemOrbited(null);
        deathStar.setParsec(4);

        assertFalse("The back does not require another Scarif move",
                ForceReserveService.compute(
                    scn.game(), scn.gameState(),
                    VirtualTableScenario.DS)
                    .vergeNeedsDeathStarMove);
        assertFalse(CharacterDeploySiteEvaluator
                .isV156FlipNotReady(
                    scn.gameState(), VirtualTableScenario.DS));

        scn.SkipToDSTurn(Phase.DRAW);
        String retrieve = scn.GetCardActionId(
                VirtualTableScenario.DS, objective,
                "Retrieve a non-unique card with ability");
        assertNotNull(retrieve);
        PublicBots bots = PublicBots.forGame(scn);
        assertEquals("One useful Lost card must beat Pass",
                retrieve, bots.decideBoth(scn));
        scn.DSDecided(retrieve);
        scn.PassAllResponses();
        if (scn.GetAwaitingDecision(
                VirtualTableScenario.DS) != null) {
            scn.DSDecided(bots.decideBoth(scn));
            scn.PassAllResponses();
        }
        assertSame(Zone.TOP_OF_USED_PILE, lost.getZone());
        assertTrue(objective.isFlipped());
    }

    @Test
    public void publicBotsUseTheNativeVaderBattleReaction() {
        var scn = scenario("207_20");
        var objective = scn.GetDSCard("objective");
        var command = scn.GetDSCard("scarifCommand");
        var citadel = scn.GetDSCard("citadelTower");
        var vader = scn.GetDSCard("vader");
        startFlippedAtCommandCenter(scn);
        scn.MoveCardsToLocation(citadel, vader);
        scn.MoveCardsToLocation(command, scn.GetLSFiller(1));

        scn.LSActivateForceCheat(1);
        scn.SkipToLSTurn(Phase.BATTLE);
        scn.LSInitiateBattle(command);

        String moveVader = scn.GetCardActionId(
                VirtualTableScenario.DS, objective,
                "Move Vader to the battle");
        assertNotNull(moveVader);
        PublicBots bots = PublicBots.forGame(scn);
        assertEquals(moveVader, bots.decideBoth(scn));
        scn.DSDecided(moveVader);

        AwaitingDecision chooseVader = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        assertNotNull(chooseVader);
        assertEquals("CARD_SELECTION",
                chooseVader.getDecisionType().name());
        assertEquals("Choose Vader, or click 'Done' to cancel",
                chooseVader.getText());
        assertEquals("0",
                chooseVader.getDecisionParameters().get("min")[0]);
        assertEquals("1",
                chooseVader.getDecisionParameters().get("max")[0]);
        assertTrue(Arrays.asList(
                chooseVader.getDecisionParameters().get("cardId"))
            .contains(Integer.toString(vader.getCardId())));
        String vaderChoice = bots.decideBoth(scn);
        assertEquals(Integer.toString(vader.getCardId()),
                vaderChoice);
        scn.DSDecided(Integer.toString(vader.getCardId()));
        scn.PassAllResponses();

        assertSame(command,
                scn.game().getModifiersQuerying()
                    .getLocationThatCardIsAt(
                        scn.gameState(), vader));
        assertTrue(objective.isFlipped());
    }

    @Test
    public void publicBotsDeclineVaderReactionThatBreaksTheLeaderHold() {
        var scn = scenario("207_20", "216_6", true);
        var objective = scn.GetDSCard("objective");
        var command = scn.GetDSCard("scarifCommand");
        var landingPad = scn.GetDSCard("scarifLandingPad");
        var krennic = scn.GetDSCard("krennic");
        var vader = scn.GetDSCard("vader");
        var disposable = scn.GetDSCard("disposable");
        startFlippedAtCommandCenter(scn);
        scn.MoveCardsToLocation(command, vader);
        loseDarkCardFromTableAndPass(
                scn, objective, krennic);
        assertTrue("Vader now supplies the only Scarif leader hold",
                objective.isFlipped());

        moveLocationAdjacentTo(scn, landingPad, command);
        scn.MoveCardsToLocation(
                landingPad, disposable, scn.GetLSFiller(1));
        scn.LSActivateForceCheat(1);
        scn.SkipToLSTurn(Phase.BATTLE);
        scn.LSInitiateBattle(landingPad);

        String moveVader = scn.GetCardActionId(
                VirtualTableScenario.DS, objective,
                "Move Vader to the battle");
        assertNotNull("The native reaction remains legally offered",
                moveVader);
        PublicBots bots = PublicBots.forGame(scn);
        assertFalse("Moving the only Scarif leader must lose to Pass",
                moveVader.equals(bots.decideBoth(scn)));

        scn.DSDecided(moveVader);
        AwaitingDecision chooseVader = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        assertNotNull(chooseVader);
        assertTrue(Arrays.asList(
                chooseVader.getDecisionParameters().get("cardId"))
            .contains(Integer.toString(vader.getCardId())));
        scn.DSDecided(Integer.toString(vader.getCardId()));
        scn.PassAllResponses();

        assertSame(landingPad,
                scn.game().getModifiersQuerying()
                    .getLocationThatCardIsAt(
                        scn.gameState(), vader));
        assertFalse("The forced counterfactual proves the native flip-back hazard",
                objective.isFlipped());
    }

    @Test
    public void nativeBackFlipsWhenTheLastOwnScarifLeaderLeavesTable() {
        var scn = scenario("207_20");
        var objective = scn.GetDSCard("objective");
        var krennic = scn.GetDSCard("krennic");
        startFlippedAtCommandCenter(scn);

        loseDarkCardFromTableAndPass(
                scn, objective, krennic);

        assertFalse("Losing the last own Scarif leader must flip to the front",
                objective.isFlipped());
        assertTrue(objective.getZone().isInPlay());
    }

    @Test
    public void nativeBackHardLossOutranksLeaderHold() {
        var missingShield = scenario("207_20");
        startFlippedAtCommandCenter(missingShield);
        var shieldObjective = missingShield.GetDSCard("objective");
        var shieldGate = missingShield.GetDSCard("shieldGate");
        var shieldAnalyzer = new com.gempukku.swccgo.ai.models.rando
                .strategy.ObjectiveAnalyzer();
        shieldAnalyzer.analyze(
                missingShield.game(), VirtualTableScenario.DS, Side.DARK);
        assertEquals(ObjectiveAnalyzer.FlipGateFormationRole
                        .REQUIRED_CARD_RETENTION_DEFENDER,
                shieldAnalyzer.classifyGateFormationPieceIfRemoved(
                    missingShield.game(), VirtualTableScenario.DS,
                    shieldGate));
        loseDarkCardFromTableAndPass(
                missingShield, shieldObjective, shieldGate);
        assertSame("Missing Shield Gate must place the back out of play",
                Zone.OUT_OF_PLAY, shieldObjective.getZone());

        var missingDeathStar = scenario("207_20");
        startFlippedAtCommandCenter(missingDeathStar);
        var deathStarObjective = missingDeathStar.GetDSCard("objective");
        var deathStar = missingDeathStar.GetDSCard("deathStar");
        var deathStarAnalyzer = new com.gempukku.swccgo.ai.models.rando
                .strategy.ObjectiveAnalyzer();
        deathStarAnalyzer.analyze(
                missingDeathStar.game(), VirtualTableScenario.DS,
                Side.DARK);
        assertEquals(ObjectiveAnalyzer.FlipGateFormationRole
                        .REQUIRED_CARD_RETENTION_DEFENDER,
                deathStarAnalyzer.classifyGateFormationPieceIfRemoved(
                    missingDeathStar.game(), VirtualTableScenario.DS,
                    deathStar));
        loseDarkCardFromTableAndPass(
                missingDeathStar, deathStarObjective, deathStar);
        assertSame("Missing Death Star must place the back out of play",
                Zone.OUT_OF_PLAY, deathStarObjective.getZone());
    }
}
