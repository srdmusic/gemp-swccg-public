package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.SpotOverride;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.cards.GameConditions;
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
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Native engine and public-bot contract for 213_32 / 213_32_BACK. */
public class ShadowCollectiveObjectiveEngineContractTest {
    private static final StartingSetup SHADOW_COLLECTIVE =
            new StartingSetup() {
        @Override
        public HashMap<String, String> Cards() {
            return new HashMap<>() {{
                put("objective", "213_32");
                put("chambers", "213_23");
            }};
        }

        @Override
        public void Setup(VirtualTableScenario scn) {
            // Maul's Chambers is the only legal setup target.
        }
    };

    private VirtualTableScenario scenario() {
        return new VirtualTableScenario(
                new HashMap<>() {{
                    put("beru", "1_2");
                }},
                new HashMap<>() {{
                    put("vigo1", "10_53");
                    put("vigo2", "10_53");
                    put("dryden", "213_4");
                    put("distractor", "1_194");
                    put("pulse", "1_194");
                    put("offPlan", "10_45");
                    put("coruscant", "12_166");
                    put("blaster", "1_317");
                    put("bar", "213_25");
                    put("study", "213_26");
                    put("cantina", "1_290");
                    put("dockingBay94", "1_291");
                    put("desertLanding", "11_92");
                }},
                24,
                24,
                StartingSetup.DefaultLSGroundLocation,
                SHADOW_COLLECTIVE,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open);
    }

    private void startWithCoruscant(VirtualTableScenario scn) {
        scn.StartGame();
        scn.MoveLocationToTable(scn.GetDSCard("coruscant"));
        assertTrue("Shadow Collective must deploy Maul's Chambers",
                scn.GetDSCard("chambers").getZone().isInPlay());
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
        Set<PhysicalCard> retained = Set.of(keep);
        List<PhysicalCardImpl> move = new ArrayList<>();
        for (PhysicalCard card : scn.gameState().getHand(
                VirtualTableScenario.DS)) {
            if (card instanceof PhysicalCardImpl physical
                    && !retained.contains(card)) {
                move.add(physical);
            }
        }
        for (PhysicalCardImpl card : move) {
            scn.MoveCardsToBottomOfDSReserveDeck(card);
        }
    }

    private ObjectiveAnalyzer[] analyzers(
            VirtualTableScenario scn) {
        ObjectiveAnalyzer[] analyzers = {
                new com.gempukku.swccgo.ai.models.rando.strategy
                        .ObjectiveAnalyzer(),
                new com.gempukku.swccgo.ai.models.chosenone.strategy
                        .ObjectiveAnalyzer()
        };
        for (ObjectiveAnalyzer analyzer : analyzers) {
            analyzer.analyze(
                    scn.game(), VirtualTableScenario.DS, Side.DARK);
        }
        return analyzers;
    }

    private ObjectiveAnalyzer.FlipLocationRuleState routeState(
            ObjectiveAnalyzer analyzer,
            VirtualTableScenario scn) {
        List<ObjectiveAnalyzer.FlipLocationRuleState> states =
                analyzer.assessFlipLocationRules(
                        scn.game(), VirtualTableScenario.DS,
                        "preFlip", "flip");
        assertEquals(1, states.size());
        return states.getFirst();
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
            assertNotNull(decision);
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

        private TracedDecision decideBothWithRandoTrace(
                VirtualTableScenario scn) {
            var traces = new ArrayList<
                    com.gempukku.swccgo.ai.models.common.trace
                        .DecisionTrace>();
            try {
                var setter = rando.getClass().getDeclaredMethod(
                        "setDecisionTraceSinkForTesting",
                        com.gempukku.swccgo.ai.models.common.trace
                            .TraceSink.class);
                setter.setAccessible(true);
                setter.invoke(rando,
                        new com.gempukku.swccgo.ai.models.common.trace
                            .TraceSink() {
                            @Override
                            public boolean isEnabled() {
                                return true;
                            }

                            @Override
                            public void accept(
                                    com.gempukku.swccgo.ai.models.common
                                        .trace.DecisionTrace trace) {
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

    @Test
    public void routeBIsDarkBattlePhaseOnlyAndRejectsUndercoverGangsters() {
        var scn = scenario();
        var coruscant = scn.GetDSCard("coruscant");
        var chasm = scn.GetLSStartingLocation();
        var vigo1 = scn.GetDSCard("vigo1");
        var vigo2 = scn.GetDSCard("vigo2");

        startWithCoruscant(scn);
        scn.MoveCardsToLocation(coruscant, vigo1);
        scn.MoveCardsToLocation(chasm, vigo2);
        scn.SkipToPhase(Phase.DEPLOY);

        for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
            assertFalse("The formation is not live outside Dark's Battle phase",
                    routeState(analyzer, scn).conditionSatisfied());
        }

        scn.SkipToPhase(Phase.BATTLE);
        for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
            assertTrue("Two visible gangsters controlling battlegrounds satisfy route B",
                    routeState(analyzer, scn).conditionSatisfied());
        }

        vigo2.setUndercover(true);
        for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
            assertFalse("An undercover gangster supplies no route-B control",
                    routeState(analyzer, scn).conditionSatisfied());
        }
    }

    @Test
    public void nativeFlipRiderMakesOpponentLoseOneOnlyWithThreeOccupiedBattlegrounds() {
        var scn = scenario();
        var objective = scn.GetDSCard("objective");
        var coruscant = scn.GetDSCard("coruscant");
        var chasm = scn.GetLSStartingLocation();
        var cantina = scn.GetDSCard("cantina");

        startWithCoruscant(scn);
        moveSiteToTatooine(scn, cantina);
        scn.MoveCardsToLocation(
                coruscant, scn.GetDSCard("vigo1"));
        scn.MoveCardsToLocation(
                chasm, scn.GetDSCard("vigo2"));
        scn.SkipToPhase(Phase.DEPLOY);

        assertTrue(GameConditions.controlsWith(
                scn.game(), objective,
                VirtualTableScenario.DS, 2,
                Filters.battleground,
                SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE,
                Filters.gangster));
        assertFalse("Two occupied battlegrounds must not satisfy the rider",
                GameConditions.occupies(
                        scn.game(), VirtualTableScenario.DS,
                        3, Filters.battleground));

        scn.MoveCardsToLocation(
                cantina, scn.GetDSCard("dryden"));
        assertTrue("A third distinct battleground must arm the printed rider",
                GameConditions.occupies(
                        scn.game(), VirtualTableScenario.DS,
                        3, Filters.battleground));
        int lightLifeBeforeFlip = scn.GetLSLifeForceRemaining();
        int lightLostBeforeFlip = scn.GetLSLostPileCount();

        scn.SkipToPhase(Phase.BATTLE);
        scn.PassAllResponses();
        assertTrue("Route B must flip through the unchanged native trigger",
                objective.isFlipped());
        assertEquals("The rider fires when the back flips front, not on the initial flip",
                lightLifeBeforeFlip, scn.GetLSLifeForceRemaining());

        scn.SkipToPhase(Phase.DRAW);
        scn.PassDrawActions();
        scn.PassResponses("RECIRCULATED");
        scn.PassAllResponses();
        assertFalse("The back must flip front unconditionally at end of turn",
                objective.isFlipped());
        assertTrue("The three-battleground front-flip rider must require one Force loss",
                scn.AwaitingLSForceLossPayment());
        scn.LSPayRemainingForceLossFromReserveDeck();

        assertEquals(lightLifeBeforeFlip - 1,
                scn.GetLSLifeForceRemaining());
        assertEquals(lightLostBeforeFlip + 1,
                scn.GetLSLostPileCount());
    }

    @Test
    public void publicBotsBattleForTheSecondGangsterSiteAndNativeCardFlips() {
        var scn = scenario();
        var objective = scn.GetDSCard("objective");
        var coruscant = scn.GetDSCard("coruscant");
        var chasm = scn.GetLSStartingLocation();
        var beru = scn.GetLSCard("beru");
        var offPlan = scn.GetDSCard("offPlan");

        scn.MoveCardsToDSHand(offPlan);
        startWithCoruscant(scn);
        keepOnlyDarkHandCards(scn, offPlan);
        scn.MoveOutOfPlay(scn.GetDSCard("chambers"));
        scn.MoveOutOfPlay(scn.GetDSCard("blaster"));
        scn.MoveOutOfPlay(scn.GetDSCard("bar"));
        scn.MoveOutOfPlay(scn.GetDSCard("study"));
        scn.MoveCardsToLocation(
                coruscant, scn.GetDSCard("vigo1"));
        scn.MoveCardsToLocation(
                chasm, scn.GetDSCard("dryden"),
                scn.GetDSCard("distractor"),
                scn.GetDSCard("pulse"), beru);
        while (scn.GetDSForcePileCount() > 0) {
            scn.MoveCardsToTopOfDSUsedPile(
                    scn.GetTopOfDSForcePile());
        }
        scn.SkipToPhase(Phase.DEPLOY);
        while (scn.GetDSForcePileCount() > 4) {
            scn.MoveCardsToTopOfDSUsedPile(
                    scn.GetTopOfDSForcePile());
        }
        if (scn.GetDSForcePileCount() < 4) {
            scn.DSActivateForceCheat(
                    4 - scn.GetDSForcePileCount());
        }
        assertFalse(objective.isFlipped());
        for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
            assertEquals("The safe completing battle costs exactly one Force",
                    1, analyzer
                        .getShadowCollectiveCurrentBattleForceReserve(
                            scn.game(), VirtualTableScenario.DS,
                            offPlan, location -> true));
            assertEquals("An unsafe battle may not create a false reserve",
                    0, analyzer
                        .getShadowCollectiveCurrentBattleForceReserve(
                            scn.game(), VirtualTableScenario.DS,
                            offPlan, location -> false));
        }
        var bots = PublicBots.forGame(scn);
        var deployParams = scn.GetCurrentDecision().getDecisionParameters();
        assertEquals("The four-cost reinforcement may not spend the battle fee: "
                        + Arrays.toString(deployParams.get("actionId"))
                        + " / "
                        + Arrays.toString(deployParams.get("actionText"))
                        + " / "
                        + Arrays.toString(deployParams.get("blueprintId")),
                "", bots.decideBoth(scn));
        scn.DSPass();
        scn.PassDeployActions();
        assertEquals(Phase.BATTLE,
                scn.gameState().getCurrentPhase());
        assertEquals(4, scn.GetDSForcePileCount());
        scn.PrepareDSDestiny(7);
        scn.PrepareLSDestiny(0);
        assertNotNull("Dark must receive the first Battle action window; phase="
                        + scn.gameState().getCurrentPhase()
                        + ", current="
                        + scn.gameState().getCurrentPlayerId()
                        + ", LS="
                        + (scn.LSGetDecision() == null
                            ? "none"
                            : scn.LSGetDecision().getText()),
                scn.DSGetDecision());

        String battle = scn.GetCardActionId(
                VirtualTableScenario.DS, chasm, "Initiate battle");
        assertNotNull("Expected Chasm battle among "
                        + scn.DSGetDecision().getText()
                        + " / "
                        + Arrays.toString(scn.DSGetDecision()
                            .getDecisionParameters().get("actionText"))
                        + " / cardIds="
                        + Arrays.toString(scn.DSGetDecision()
                            .getDecisionParameters().get("cardId")),
                battle);
        assertEquals("The safe contested route site must beat Pass",
                battle, bots.decideBoth(scn));

        scn.DSInitiateBattle(chasm);
        scn.SkipToDamageSegment(true);
        scn.PassAllResponses();
        assertTrue(scn.AwaitingLSBattleDamagePayment());
        scn.LSPayBattleDamageFromCardInPlay(beru);
        if (scn.AwaitingLSBattleDamagePayment()) {
            scn.LSPayRemainingBattleDamageFromReserveDeck();
        }
        scn.PassAllResponses();
        assertEquals(Zone.TOP_OF_LOST_PILE, beru.getZone());
        assertEquals("The flip window must still be Dark's Battle phase",
                Phase.BATTLE, scn.gameState().getCurrentPhase());
        assertTrue("The native card's exact controls-with condition must be true",
                GameConditions.controlsWith(
                        scn.game(), objective,
                        VirtualTableScenario.DS, 2,
                        Filters.battleground,
                        SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE,
                        Filters.gangster));
        assertTrue(GameConditions.canBeFlipped(scn.game(), objective));
        assertFalse("The required native flip must still be resolved",
                objective.isFlipped());
        assertTrue("Removing the blocker must queue the card's required Flip action",
                scn.GetDSAvailableActions().stream()
                    .anyMatch(action -> action.contains("Flip")));
        scn.DSChooseAction("Flip");
        scn.PassAllResponses();
        assertTrue("Removing the blocker during Dark's Battle phase must fire the native flip",
                objective.isFlipped());
        if (scn.AwaitingLSBattleDamagePayment()) {
            scn.LSPayRemainingBattleDamageFromReserveDeck();
        }
        scn.PassAllResponses();
        assertEquals("The native flip must resolve before leaving Battle",
                Phase.BATTLE, scn.gameState().getCurrentPhase());

        scn.SkipToPhase(Phase.DRAW);
        scn.PassDrawActions();
        scn.PassResponses("RECIRCULATED");
        scn.PassAllResponses();
        assertFalse("The back side must flip front at the end of every turn",
                objective.isFlipped());
    }

    @Test
    public void shadowReserveUsesDistinctGangstersAndBattlegrounds() {
        var scn = scenario();
        var vigo1 = scn.GetDSCard("vigo1");
        var vigo2 = scn.GetDSCard("vigo2");
        var distractor = scn.GetDSCard("distractor");

        scn.MoveCardsToDSHand(vigo1, vigo2, distractor);
        startWithCoruscant(scn);
        keepOnlyDarkHandCards(scn, vigo1, vigo2, distractor);
        scn.SkipToPhase(Phase.DEPLOY);

        for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
            assertEquals("One Vigo is free on Coruscant; the second costs three at a distinct battleground",
                    3, analyzer.getCountedObjectivePresenceForceReserve(
                            scn.game(), VirtualTableScenario.DS, null));
            assertEquals("The selected Vigo can pay for one site while the other deploys free to Coruscant",
                    0, analyzer.getCountedObjectivePresenceForceReserve(
                            scn.game(), VirtualTableScenario.DS, vigo1));
            assertEquals("An unrelated deploy may not spend the three-Force route reserve",
                    3, analyzer.getCountedObjectivePresenceForceReserve(
                            scn.game(), VirtualTableScenario.DS, distractor));
        }

        vigo2.setUndercover(true);
        for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
            assertEquals("With only one live route actor, partial-progress reserve is the free Coruscant leg",
                    0, analyzer.getCountedObjectivePresenceForceReserve(
                            scn.game(), VirtualTableScenario.DS, vigo2));
        }
    }

    @Test
    public void bothAnalyzersProtectTheCheapestNeededGangsterFromLoss() {
        var scn = scenario();
        var coruscant = scn.GetDSCard("coruscant");
        var vigo1 = scn.GetDSCard("vigo1");
        var vigo2 = scn.GetDSCard("vigo2");
        var distractor = scn.GetDSCard("distractor");

        scn.MoveCardsToDSHand(vigo2, distractor);
        startWithCoruscant(scn);
        scn.MoveCardsToLocation(coruscant, vigo1);
        keepOnlyDarkHandCards(scn, vigo2, distractor);
        scn.SkipToPhase(Phase.DEPLOY);

        assertEquals(Zone.HAND, vigo2.getZone());
        assertTrue(Filters.gangster.accepts(
                scn.gameState(), scn.game().getModifiersQuerying(), vigo2));
        assertTrue(Filters.battleground.accepts(
                scn.gameState(), scn.game().getModifiersQuerying(),
                scn.GetLSStartingLocation()));

        for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
            assertTrue("The hand Vigo must have a legal second-site route",
                    analyzer.hasLegalPreFlipActorLocationDestination(
                            scn.game(), VirtualTableScenario.DS, vigo2));
            assertTrue(analyzer
                    .isPreferredCountedObjectivePresenceForceLossCandidate(
                            scn.game(), VirtualTableScenario.DS, vigo2));
            assertFalse(analyzer
                    .isPreferredCountedObjectivePresenceForceLossCandidate(
                            scn.game(), VirtualTableScenario.DS, distractor));
        }
    }

    @Test
    public void publicBotsSpendExactForceOnFinalVigoAndNativeBattleStartFlips() {
        var scn = scenario();
        var objective = scn.GetDSCard("objective");
        var chambers = scn.GetDSCard("chambers");
        var coruscant = scn.GetDSCard("coruscant");
        var chasm = scn.GetLSStartingLocation();
        var vigo1 = scn.GetDSCard("vigo1");
        var vigo2 = scn.GetDSCard("vigo2");
        var blaster = scn.GetDSCard("blaster");

        scn.MoveCardsToDSHand(vigo2, blaster);
        startWithCoruscant(scn);
        scn.MoveCardsToLocation(coruscant, vigo1);
        keepOnlyDarkHandCards(scn, vigo2, blaster);
        scn.MoveOutOfPlay(scn.GetDSCard("bar"));
        scn.MoveOutOfPlay(scn.GetDSCard("study"));
        scn.SkipToDSTurn(Phase.DEPLOY);
        keepExactlyDarkForce(scn, 3);

        int exactVigoCost = (int) Math.ceil(
                scn.game().getModifiersQuerying().getDeployCost(
                    scn.gameState(), vigo2, vigo2, chasm,
                    false, null, false, 0.0f,
                    null, true));
        assertEquals("The final Vigo costs the entire exact budget",
                3, exactVigoCost);
        for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
            assertTrue(analyzer
                    .advancesPreFlipActorAtRuntimeLocation(
                        scn.game(), VirtualTableScenario.DS,
                        vigo2, chasm));
            assertTrue(analyzer.wouldCompletePreFlipRequirementAt(
                    scn.game(), VirtualTableScenario.DS,
                    vigo2, chasm));
            assertFalse(analyzer.wouldCompletePreFlipRequirementAt(
                    scn.game(), VirtualTableScenario.DS,
                    vigo2, coruscant));
            assertFalse(analyzer.wouldCompletePreFlipRequirementAt(
                    scn.game(), VirtualTableScenario.DS,
                    vigo2, chambers));
            assertFalse("Route B is not live until Dark's Battle phase",
                    routeState(analyzer, scn).conditionSatisfied());
            assertFalse("No legal native pull remains to distract the parent decision",
                    analyzer.hasShadowCollectiveNativePullCandidateInReserve(
                        scn.game(), VirtualTableScenario.DS,
                        objective));
        }

        String vigoDeploy = scn.GetCardActionId(
                VirtualTableScenario.DS, vigo2, "Deploy");
        String blasterDeploy = scn.GetCardActionId(
                VirtualTableScenario.DS, blaster, "Deploy");
        assertNotNull(vigoDeploy);
        assertNotNull("A legal cheaper distraction must be real",
                blasterDeploy);
        var bots = PublicBots.forGame(scn);
        assertEquals("The final route Vigo must beat the cheaper weapon",
                vigoDeploy, bots.decideBoth(scn));
        scn.DSDecided(vigoDeploy);

        AwaitingDecision destination = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        assertNotNull(destination);
        assertTrue(destination.getText().startsWith(
                "Choose where to deploy"));
        assertTrue(scn.DSHasCardChoiceAvailable(chasm));
        String chasmResponse = Integer.toString(chasm.getCardId());
        TracedDecision tracedDestination =
                bots.decideBothWithRandoTrace(scn);
        assertEquals(chasmResponse, tracedDestination.response());
        long completionOperations = tracedDestination.trace()
                .getOperations().stream()
                .filter(operation -> chasmResponse.equals(
                            operation.getActionId())
                        && operation.getRuleId() != null
                        && "DEPLOY.OBJECTIVE.SHADOW_COLLECTIVE.ROUTE_COMPLETION"
                            .equals(operation.getRuleId().id())
                        && operation.getDeltaBits() != null
                        && operation.getDeltaBits()
                            == Float.floatToRawIntBits(8000.0f))
                .count();
        assertEquals("The exact completion score must reach the public child decision",
                1, completionOperations);
        scn.DSDecided(chasmResponse);
        scn.PassAllResponses();

        assertEquals(0, scn.GetDSForcePileCount());
        assertEquals(chasm,
                scn.game().getModifiersQuerying()
                    .getLocationThatCardIsAt(
                        scn.gameState(), vigo2));
        assertTrue(GameConditions.controlsWith(
                scn.game(), objective,
                VirtualTableScenario.DS, 2,
                Filters.battleground,
                SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE,
                Filters.gangster));
        assertFalse("The deploy itself must not flip a Battle-timed route",
                objective.isFlipped());

        scn.DSPass();
        scn.PassDeployActions();
        assertEquals(Phase.BATTLE,
                scn.gameState().getCurrentPhase());
        assertTrue("START_OF_PHASE must fire the native Battle-phase flip",
                objective.isFlipped());
    }

    @Test
    public void publicBotsMoveOneOfTwoVigosToTheSecondBattlegroundAndFlipNextBattlePhase() {
        var scn = scenario();
        var objective = scn.GetDSCard("objective");
        var cantina = scn.GetDSCard("cantina");
        var dockingBay94 = scn.GetDSCard("dockingBay94");
        var desertLanding = scn.GetDSCard("desertLanding");
        var vigo1 = scn.GetDSCard("vigo1");
        var vigo2 = scn.GetDSCard("vigo2");

        scn.StartGame();
        moveSiteToTatooine(scn, cantina);
        moveSiteToTatooine(scn, dockingBay94);
        moveSiteToTatooine(scn, desertLanding);
        scn.MoveCardsToLocation(dockingBay94, vigo1, vigo2);
        keepOnlyDarkHandCards(scn);
        scn.SkipToPhase(Phase.MOVE);
        keepExactlyDarkForce(scn, 1);
        assertEquals(Phase.MOVE,
                scn.gameState().getCurrentPhase());
        assertFalse(objective.isFlipped());

        String firstMove = scn.GetCardActionId(
                VirtualTableScenario.DS, vigo1, "Move");
        String secondMove = scn.GetCardActionId(
                VirtualTableScenario.DS, vigo2, "Move");
        assertNotNull(firstMove);
        assertNotNull(secondMove);
        var bots = PublicBots.forGame(scn);
        String selectedMove = bots.decideBoth(scn);
        assertTrue("A Vigo must move while its partner holds the first battleground",
                Set.of(firstMove, secondMove).contains(selectedMove));
        scn.DSDecided(selectedMove);

        AwaitingDecision destination = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        assertNotNull(destination);
        assertTrue(scn.DSHasCardChoiceAvailable(cantina));
        assertTrue("The non-battleground decoy must be a real legal option",
                scn.DSHasCardChoiceAvailable(desertLanding));
        String cantinaResponse = Integer.toString(cantina.getCardId());
        assertEquals("The distinct battleground must beat the legal decoy",
                cantinaResponse, bots.decideBoth(scn));
        scn.DSDecided(cantinaResponse);
        scn.PassAllResponses();

        assertEquals(0, scn.GetDSForcePileCount());
        assertTrue("One Vigo must remain at the first battleground",
                Filters.at(dockingBay94).accepts(
                    scn.gameState(),
                    scn.game().getModifiersQuerying(), vigo1)
                || Filters.at(dockingBay94).accepts(
                    scn.gameState(),
                    scn.game().getModifiersQuerying(), vigo2));
        assertTrue("The other Vigo must establish the second battleground",
                Filters.at(cantina).accepts(
                    scn.gameState(),
                    scn.game().getModifiersQuerying(), vigo1)
                || Filters.at(cantina).accepts(
                    scn.gameState(),
                    scn.game().getModifiersQuerying(), vigo2));
        assertTrue(GameConditions.controlsWith(
                scn.game(), objective,
                VirtualTableScenario.DS, 2,
                Filters.battleground,
                SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE,
                Filters.gangster));
        assertFalse("Movement occurs after the current turn's Battle phase",
                objective.isFlipped());

        scn.SkipToDSTurn(Phase.BATTLE);
        assertEquals(Phase.BATTLE,
                scn.gameState().getCurrentPhase());
        assertTrue("The next Dark Battle phase must fire the native flip",
                objective.isFlipped());
    }

    @Test
    public void publicBotsPullDeployAndFireBlasterToTriggerNativeHitFlip() {
        var scn = scenario();
        var objective = scn.GetDSCard("objective");
        var coruscant = scn.GetDSCard("coruscant");
        var vigo = scn.GetDSCard("vigo1");
        var secondVigo = scn.GetDSCard("vigo2");
        var blaster = scn.GetDSCard("blaster");
        var beru = scn.GetLSCard("beru");

        startWithCoruscant(scn);
        scn.MoveCardsToBottomOfDSReserveDeck(blaster);
        scn.MoveCardsToLocation(
                coruscant, vigo, secondVigo, beru);
        keepOnlyDarkHandCards(scn);
        scn.MoveOutOfPlay(scn.GetDSCard("bar"));
        scn.MoveOutOfPlay(scn.GetDSCard("study"));
        scn.SkipToPhase(Phase.DEPLOY);
        keepExactlyDarkForce(scn, 3);
        var bots = PublicBots.forGame(scn);

        String pull = scn.GetCardActionId(
                VirtualTableScenario.DS, objective,
                "Deploy a card from Reserve Deck");
        assertNotNull(pull);
        assertEquals("The only legal Route-A enabler must beat Pass and the Maul action",
                pull, bots.decideBoth(scn));
        scn.DSDecided(pull);

        AwaitingDecision reserveChoice = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        assertNotNull(reserveChoice);
        String pulled = bots.decideBoth(scn);
        String[] reserveIds = reserveChoice.getDecisionParameters()
                .get("cardId");
        String[] reserveBlueprints = reserveChoice
                .getDecisionParameters().get("blueprintId");
        int pulledIndex = List.of(reserveIds).indexOf(pulled);
        assertTrue(pulledIndex >= 0);
        assertEquals(blaster.getBlueprintId(true),
                reserveBlueprints[pulledIndex]);
        scn.DSDecided(pulled);

        scn.PassAllResponses();
        assertTrue("The pull must advance to the weapon target prompt",
                scn.DSDecisionAvailable("Choose where to deploy"));
        String wielder = bots.decideBoth(scn);
        assertTrue("The weapon must deploy on one of the two legal Vigos",
                Set.of(Integer.toString(vigo.getCardId()),
                        Integer.toString(secondVigo.getCardId()))
                    .contains(wielder));
        scn.DSChooseCard(wielder.equals(
                        Integer.toString(vigo.getCardId()))
                ? vigo : secondVigo);
        scn.PassAllResponses();
        assertTrue(blaster.getAttachedTo() == vigo
                || blaster.getAttachedTo() == secondVigo);
        assertEquals("Deploying the blaster must leave battle plus fire Force",
                2, scn.GetDSForcePileCount());
        assertFalse(objective.isFlipped());

        scn.DSPass();
        scn.PassDeployActions();
        assertEquals(Phase.BATTLE,
                scn.gameState().getCurrentPhase());
        scn.PrepareDSDestiny(7);
        String battle = scn.GetCardActionId(
                VirtualTableScenario.DS, coruscant,
                "Initiate battle");
        assertNotNull(battle);
        assertEquals(battle, bots.decideBoth(scn));
        scn.DSInitiateBattle(coruscant);
        scn.PassAllResponses();
        assertTrue(scn.AwaitingDSWeaponsSegmentActions());

        String fire = scn.GetCardActionId(
                VirtualTableScenario.DS, blaster, "Fire");
        assertNotNull(fire);
        assertEquals("Generic safe weapon behavior must execute the armed objective route",
                fire, bots.decideBoth(scn));
        scn.DSDecided(fire);
        AwaitingDecision target = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        assertNotNull(target);
        assertEquals(Integer.toString(beru.getCardId()),
                bots.decideBoth(scn));
        scn.DSChooseCard(beru);
        scn.PassAllResponses();

        assertTrue("Prepared destiny must hit the opponent's character",
                beru.isHit());
        assertTrue("Route A must flip through the unchanged native card trigger",
                objective.isFlipped());
    }

    @Test
    public void nativePullRejectsBlasterWithoutALegalWarriorHost() {
        var scn = scenario();
        var objective = scn.GetDSCard("objective");
        var blaster = scn.GetDSCard("blaster");

        startWithCoruscant(scn);
        keepOnlyDarkHandCards(scn);
        scn.MoveCardsToBottomOfDSReserveDeck(blaster);
        scn.MoveOutOfPlay(scn.GetDSCard("bar"));
        scn.MoveOutOfPlay(scn.GetDSCard("study"));
        scn.SkipToPhase(Phase.DEPLOY);
        keepExactlyDarkForce(scn, 3);

        for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
            assertFalse("A blaster with no legal warrior host is not a live native pull",
                    analyzer.hasShadowCollectiveNativePullCandidateInReserve(
                            scn.game(), VirtualTableScenario.DS,
                            objective));
        }
        String pull = scn.GetCardActionId(
                VirtualTableScenario.DS, objective,
                "Deploy a card from Reserve Deck");
        assertNotNull("The source exposes the parent before searching its pile",
                pull);
        assertFalse("The bots must reject the parent when its only candidate has no host",
                pull.equals(PublicBots.forGame(scn).decideBoth(scn)));
    }

    @Test
    public void publicBotsUseTheNativePullAndPreferAFirstLightBattleground() {
        var scn = scenario();
        var objective = scn.GetDSCard("objective");
        var blaster = scn.GetDSCard("blaster");
        var bar = scn.GetDSCard("bar");
        var study = scn.GetDSCard("study");

        startWithCoruscant(scn);
        scn.MoveCardsToLocation(
                scn.GetDSCard("coruscant"),
                scn.GetDSCard("vigo1"));
        keepOnlyDarkHandCards(scn);
        scn.MoveCardsToBottomOfDSReserveDeck(blaster, bar, study);
        scn.DSActivateForceCheat(8);
        scn.SkipToPhase(Phase.DEPLOY);
        String pull = scn.GetCardActionId(
                VirtualTableScenario.DS, objective,
                "Deploy a card from Reserve Deck");
        assertNotNull(pull);
        for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
            assertTrue("The exact objective source must classify as the Shadow route pull",
                    analyzer.isShadowCollectiveRoutePullAction(
                            VirtualTableScenario.DS, objective,
                            "Deploy a card from Reserve Deck"));
        }
        var bots = PublicBots.forGame(scn);
        String selectedParent = bots.decideBoth(scn);
        var parentParams = scn.GetAwaitingDecision(
                VirtualTableScenario.DS).getDecisionParameters();
        assertEquals("The objective's native pull must beat Pass; ids="
                        + Arrays.toString(parentParams.get("actionId"))
                        + ", texts="
                        + Arrays.toString(parentParams.get("actionText"))
                        + ", cardIds="
                        + Arrays.toString(parentParams.get("cardId"))
                        + ", sources="
                        + Arrays.toString(parentParams.get("testingText")),
                pull, selectedParent);

        scn.DSDecided(pull);
        AwaitingDecision child = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        assertNotNull(child);
        String[] cardIds = child.getDecisionParameters().get("cardId");
        String[] blueprints = child.getDecisionParameters()
                .get("blueprintId");
        assertTrue("The battleground First Light card must be offered; bps="
                        + Arrays.toString(blueprints),
                List.of(blueprints).contains(bar.getBlueprintId(true)));
        String selectedChild = bots.decideBoth(scn);
        int selectedIndex = List.of(cardIds).indexOf(selectedChild);
        assertTrue(selectedIndex >= 0);
        assertEquals("Bar advances Route B; Study and a blaster do not",
                bar.getBlueprintId(true), blueprints[selectedIndex]);
    }
}
