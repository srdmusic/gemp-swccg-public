package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.ai.models.common.phase.DeployObjectiveSitingPolicy;
import com.gempukku.swccgo.ai.models.common.phase.MoveDestinationPolicy;
import com.gempukku.swccgo.ai.models.common.phase.NoMoneyNoPartsObjectivePolicy;
import com.gempukku.swccgo.ai.models.common.phase.ObjectiveBattlePolicy;
import com.gempukku.swccgo.ai.models.common.phase.PullActionPolicy;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
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
 * Batch Eighteen (2026-07-27): native engine contract for No Money, No
 * Parts, No Deal! / You're A Slave? (12_180, DARK). Card Java unchanged.
 * FALSE TWIN of 110_4 (opposite sides, structurally different laws);
 * batched together, tested separately.
 *
 * Law (Card12_180.java L163-L164): flips when Watto (a TITLE filter — no
 * Persona.WATTO exists) is PRESENT AT Watto's Junkyard AND you OCCUPY Mos
 * Espa. Occupy, not control: opponent presence at Mos Espa does not block
 * it. Back (Card12_180_BACK.java L225-L226): the exact De Morgan negation —
 * losing EITHER leg flips it back. No hard-loss handler on either side.
 */
public class NoMoneyNoPartsObjectiveEngineContractTest {

    private static final StartingSetup NO_MONEY = new StartingSetup() {
        @Override
        public HashMap<String, String> Cards() {
            return new HashMap<>() {{
                put("objective", "12_180");
                put("junkyard", "12_178");
                put("mosEspa", "11_93");
            }};
        }

        @Override
        public void Setup(VirtualTableScenario scn) {
            // Required pulls in order: Watto's Junkyard, then Mos Espa.
            // Answer "On which side" first (its text also contains
            // "deploy"); pick whichever site is still in Reserve on the
            // card-choice prompts.
            for (int i = 0; i < 8; i++) {
                if (scn.DSDecisionAvailable("On which side")) {
                    scn.DSChoose("Left");
                } else if (scn.DSDecisionAvailable("to deploy")) {
                    if (scn.GetDSCard("junkyard").getZone() == Zone.RESERVE_DECK) {
                        scn.DSChooseCard(scn.GetDSCard("junkyard"));
                    } else {
                        scn.DSChooseCard(scn.GetDSCard("mosEspa"));
                    }
                }
            }
        }
    };

    private VirtualTableScenario noMoneyScenario() {
        return new VirtualTableScenario(
                new HashMap<>() {{
                }},
                new HashMap<>() {{
                    put("watto", "11_65");
                    put("wattoTwo", "11_66");
                    put("televan", "12_120");
                    put("sebulba", "211_6");
                    put("gunner", "3_88");
                    put("blendin", "5_103");
                    put("tie", "1_304");
                    put("system", "12_175");
                }},
                24,
                24,
                StartingSetup.DefaultLSGroundLocation,
                NO_MONEY,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open
        );
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

    private void keepExactlyLightForce(
            VirtualTableScenario scn, int amount) {
        while (scn.GetLSForcePileCount() > amount) {
            scn.MoveCardsToTopOfLSUsedPile(
                    scn.GetTopOfLSForcePile());
        }
        while (scn.GetLSForcePileCount() < amount) {
            scn.MoveCardsToTopOfLSForcePile(
                    scn.GetTopOfLSReserveDeck());
        }
        assertEquals(amount, scn.GetLSForcePileCount());
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

        private TracedDecision decideBothWithRandoTrace(
                VirtualTableScenario scn) {
            var traces = new ArrayList<com.gempukku.swccgo.ai.models.common
                    .trace.DecisionTrace>();
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
                return new TracedDecision(response, traces.getFirst());
            } catch (ReflectiveOperationException e) {
                throw new AssertionError(e);
            }
        }

        private String decideLightBoth(VirtualTableScenario scn) {
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

    private record TracedDecision(
            String response,
            com.gempukku.swccgo.ai.models.common.trace.DecisionTrace trace) {
    }

    private static void assertPolicyDelta(
            PolicyResult result, String ruleId, float delta) {
        assertEquals(1, result.operations().size());
        assertEquals(ruleId,
                result.operations().getFirst().ruleArmId().id());
        assertEquals(delta,
                result.operations().getFirst().delta(), 0.0f);
    }

    private void flipWithFormation(
            VirtualTableScenario scn,
            PhysicalCardImpl watto, PhysicalCardImpl occupier) {
        var pulse = scn.GetDSFiller(6);
        scn.MoveCardsToDSHand(pulse);
        scn.StartGame();
        scn.MoveCardsToLocation(scn.GetDSCard("junkyard"), watto);
        scn.MoveCardsToLocation(scn.GetDSCard("mosEspa"), occupier);
        scn.DSActivateForceCheat(8);
        scn.SkipToDSTurn(Phase.DEPLOY);
        scn.DSDeployCardAndPassResponses(
                pulse, scn.GetLSStartingLocation());
        assertTrue("The source-defined two-leg formation must flip",
                scn.GetDSCard("objective").isFlipped());
        scn.MoveOutOfPlay(pulse);
    }

    @Test
    public void nmnpndFrontRequiresBothLegs() {
        var scn = noMoneyScenario();
        var objective = scn.GetDSCard("objective");
        var junkyard = scn.GetDSCard("junkyard");
        var mosEspa = scn.GetDSCard("mosEspa");
        var watto = scn.GetDSCard("watto");
        var pulseOne = scn.GetDSFiller(2);
        var pulseTwo = scn.GetDSFiller(3);

        scn.MoveCardsToDSHand(pulseOne, pulseTwo);
        scn.StartGame();
        // Watto present at the Junkyard, Mos Espa empty: leg 2 open, so the
        // board is not flip-complete before the phase skip.
        scn.MoveCardsToLocation(junkyard, watto);

        scn.DSActivateForceCheat(12);
        scn.SkipToPhase(Phase.DEPLOY);

        scn.DSDeployCardAndPassResponses(
                pulseOne, scn.GetLSStartingLocation());
        assertFalse("Watto at the Junkyard without Mos Espa occupation must not flip",
                objective.isFlipped());
        scn.LSPass();

        scn.MoveCardsToLocation(mosEspa, scn.GetDSFiller(1));
        scn.DSDeployCardAndPassResponses(
                pulseTwo, scn.GetLSStartingLocation());
        assertTrue("Watto at the Junkyard plus Mos Espa occupation must flip",
                objective.isFlipped());
    }

    @Test
    public void nmnpndWattoMustBePresentAtTheJunkyardSpecifically() {
        var scn = noMoneyScenario();
        var objective = scn.GetDSCard("objective");
        var junkyard = scn.GetDSCard("junkyard");
        var mosEspa = scn.GetDSCard("mosEspa");
        var watto = scn.GetDSCard("watto");
        var pulseOne = scn.GetDSFiller(2);
        var pulseTwo = scn.GetDSFiller(3);

        scn.MoveCardsToDSHand(pulseOne, pulseTwo);
        scn.StartGame();
        // Watto at the WRONG site (Mos Espa, which he also occupies for the
        // owner): leg 1 is site-exact and must hold the flip.
        scn.MoveCardsToLocation(mosEspa, watto);

        scn.DSActivateForceCheat(12);
        scn.SkipToPhase(Phase.DEPLOY);

        scn.DSDeployCardAndPassResponses(
                pulseOne, scn.GetLSStartingLocation());
        assertFalse("Watto away from the Junkyard must not flip, even with Mos Espa occupied",
                objective.isFlipped());
        scn.LSPass();

        scn.MoveCardsToLocation(junkyard, watto);
        scn.MoveCardsToLocation(mosEspa, scn.GetDSFiller(1));
        scn.DSDeployCardAndPassResponses(
                pulseTwo, scn.GetLSStartingLocation());
        assertTrue("Watto restored to the Junkyard must flip",
                objective.isFlipped());
    }

    @Test
    public void nmnpndMosEspaLegIsOccupyNotControl() {
        var scn = noMoneyScenario();
        var objective = scn.GetDSCard("objective");
        var junkyard = scn.GetDSCard("junkyard");
        var mosEspa = scn.GetDSCard("mosEspa");
        var watto = scn.GetDSCard("watto");
        var pulseOne = scn.GetDSFiller(2);

        scn.MoveCardsToDSHand(pulseOne);
        scn.StartGame();
        // Opponent presence at Mos Espa denies CONTROL but not OCCUPATION.
        // Junkyard leg ready, Mos Espa contested but empty of ours: still
        // not flip-complete before the skip.
        scn.MoveCardsToLocation(junkyard, watto);
        scn.MoveCardsToLocation(mosEspa, scn.GetLSFiller(1));

        scn.DSActivateForceCheat(12);
        scn.SkipToPhase(Phase.DEPLOY);

        scn.MoveCardsToLocation(mosEspa, scn.GetDSFiller(1));
        scn.DSDeployCardAndPassResponses(
                pulseOne, scn.GetLSStartingLocation());
        assertTrue("Occupation under opponent presence must still flip (occupy, not control)",
                objective.isFlipped());
    }

    @Test
    public void nmnpndBackFlipsBackOnLosingEitherLeg() {
        var scn = noMoneyScenario();
        var objective = scn.GetDSCard("objective");
        var junkyard = scn.GetDSCard("junkyard");
        var mosEspa = scn.GetDSCard("mosEspa");
        var watto = scn.GetDSCard("watto");
        var occupier = scn.GetDSFiller(1);
        var pulseOne = scn.GetDSFiller(2);
        var pulseTwo = scn.GetDSFiller(3);
        var pulseThree = scn.GetDSFiller(4);
        var pulseFour = scn.GetDSFiller(5);

        scn.MoveCardsToDSHand(pulseOne, pulseTwo, pulseThree, pulseFour);
        scn.StartGame();
        scn.MoveCardsToLocation(junkyard, watto);

        scn.DSActivateForceCheat(16);
        scn.SkipToPhase(Phase.DEPLOY);

        scn.MoveCardsToLocation(mosEspa, occupier);
        scn.DSDeployCardAndPassResponses(
                pulseOne, scn.GetLSStartingLocation());
        assertTrue("Both legs together must flip", objective.isFlipped());
        scn.LSPass();

        // Leg 1 lost: Watto leaves the Junkyard.
        scn.MoveOutOfPlay(watto);
        scn.DSDeployCardAndPassResponses(
                pulseTwo, scn.GetLSStartingLocation());
        assertFalse("Losing Watto at the Junkyard must flip the back to front",
                objective.isFlipped());
        scn.LSPass();

        // Both legs restored: the front flips again.
        scn.MoveCardsToLocation(junkyard, watto);
        scn.DSDeployCardAndPassResponses(
                pulseThree, scn.GetLSStartingLocation());
        assertTrue("Restoring both legs must flip again",
                objective.isFlipped());
        scn.LSPass();

        // Leg 2 lost: Mos Espa occupation ends.
        scn.MoveOutOfPlay(occupier);
        scn.DSDeployCardAndPassResponses(
                pulseFour, scn.GetLSStartingLocation());
        assertFalse("Losing Mos Espa occupation must flip the back to front",
                objective.isFlipped());
    }

    @Test
    public void nmnpndProfileRulesTrackTheEngineLaw() {
        var scn = noMoneyScenario();
        var junkyard = scn.GetDSCard("junkyard");
        var mosEspa = scn.GetDSCard("mosEspa");
        var watto = scn.GetDSCard("watto");

        scn.StartGame();

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertTrue("Profile must hydrate for 12_180", analyzer.isAnalyzed());

        var preFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.DS, "preFlip", "flip");
        assertEquals("NMNPND front encodes one two-leg rule", 1,
                preFlip.size());
        assertFalse("With Watto off-site and Mos Espa empty the law is unmet",
                preFlip.get(0).conditionSatisfied());

        scn.MoveCardsToLocation(junkyard, watto);
        scn.MoveCardsToLocation(mosEspa, scn.GetDSFiller(1));
        var complete = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.DS, "preFlip", "flip");
        assertTrue("Both legs complete the encoded law",
                complete.get(0).conditionSatisfied());

        var postFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.DS, "postFlip", "flipBack");
        assertEquals("The back encodes one De Morgan anyOf rule", 1,
                postFlip.size());
        assertFalse("With both legs held the flip-back condition is unmet",
                postFlip.get(0).conditionSatisfied());
    }

    @Test
    public void nmnpndWattoInHandIsTypedRuntimeActorForTheJunkyard() {
        var scn = noMoneyScenario();
        var watto = scn.GetDSCard("watto");
        var junkyard = scn.GetDSCard("junkyard");

        scn.MoveCardsToDSHand(watto);
        scn.StartGame();
        keepOnlyDarkHandCards(scn, watto);

        var rando = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.strategy
                .ObjectiveAnalyzer();
        rando.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        chosen.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);

        assertTrue(rando.isNoMoneyNoPartsObjectiveFamily());
        assertEquals(ObjectiveAnalyzer.ObjectiveProgressCandidateRole
                        .REQUIRED_ACTOR,
                rando.classifyPreFlipProgressCandidate(
                    scn.game(), VirtualTableScenario.DS, watto));
        assertTrue(rando.advancesPreFlipActorAtRuntimeLocation(
                scn.game(), VirtualTableScenario.DS, watto, junkyard));
        assertEquals(
                rando.classifyPreFlipProgressCandidate(
                        scn.game(), VirtualTableScenario.DS, watto),
                chosen.classifyPreFlipProgressCandidate(
                        scn.game(), VirtualTableScenario.DS, watto));
        assertEquals(
                rando.advancesPreFlipActorAtRuntimeLocation(
                        scn.game(), VirtualTableScenario.DS,
                        watto, junkyard),
                chosen.advancesPreFlipActorAtRuntimeLocation(
                        scn.game(), VirtualTableScenario.DS,
                        watto, junkyard));
    }

    @Test
    public void nmnpndCheapestMosEspaBodyIsFundedAndProtectedInHand() {
        var scn = noMoneyScenario();
        var gunner = scn.GetDSCard("gunner");
        var blendin = scn.GetDSCard("blendin");

        scn.MoveCardsToDSHand(gunner, blendin);
        scn.StartGame();
        keepOnlyDarkHandCards(scn, gunner, blendin);
        scn.DSActivateForceCheat(2);
        scn.SkipToDSTurn(Phase.DEPLOY);

        var rando = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.strategy
                .ObjectiveAnalyzer();
        rando.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        chosen.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);

        assertEquals("One Force funds the cheapest legal Mos Espa body",
                1, rando.getCountedObjectivePresenceForceReserve(
                        scn.game(), VirtualTableScenario.DS, null));
        assertTrue(rando
                .isPreferredCountedObjectivePresenceForceLossCandidate(
                        scn.game(), VirtualTableScenario.DS, gunner));
        assertFalse("Cloud City-only Blendin cannot occupy Mos Espa",
                rando.isPreferredCountedObjectivePresenceForceLossCandidate(
                        scn.game(), VirtualTableScenario.DS, blendin));
        assertEquals(
                rando.getCountedObjectivePresenceForceReserve(
                        scn.game(), VirtualTableScenario.DS, null),
                chosen.getCountedObjectivePresenceForceReserve(
                        scn.game(), VirtualTableScenario.DS, null));
        assertEquals(
                rando.isPreferredCountedObjectivePresenceForceLossCandidate(
                        scn.game(), VirtualTableScenario.DS, gunner),
                chosen.isPreferredCountedObjectivePresenceForceLossCandidate(
                        scn.game(), VirtualTableScenario.DS, gunner));
    }

    @Test
    public void nmnpndPostFlipBothExactLegsAreRetentionAnchors() {
        var scn = noMoneyScenario();
        var junkyard = scn.GetDSCard("junkyard");
        var mosEspa = scn.GetDSCard("mosEspa");
        var watto = scn.GetDSCard("watto");
        var televan = scn.GetDSCard("televan");

        flipWithFormation(scn, watto, televan);

        var rando = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.strategy
                .ObjectiveAnalyzer();
        rando.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        chosen.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);

        assertEquals(ObjectiveAnalyzer.FlipGateFormationRole
                        .LAST_REQUIRED_ON_TABLE_ACTOR,
                rando.classifyGateFormationPieceIfRemoved(
                        scn.game(), VirtualTableScenario.DS, watto));
        assertEquals(ObjectiveAnalyzer.FlipGateFormationRole
                        .LAST_FLIP_BACK_BLOCKER,
                rando.classifyGateFormationPieceIfRemoved(
                        scn.game(), VirtualTableScenario.DS, televan));
        assertTrue(rando.wouldDepartureTriggerFlipBack(
                scn.game(), VirtualTableScenario.DS, watto));
        assertTrue(rando.wouldDepartureTriggerFlipBack(
                scn.game(), VirtualTableScenario.DS, televan));
        assertTrue("The Watto site itself must be a typed retention location",
                rando.isFlipBackProtectionLocation(
                        junkyard, scn.game(), VirtualTableScenario.DS));
        assertTrue("Mos Espa is the other exact retention location",
                rando.isFlipBackProtectionLocation(
                        mosEspa, scn.game(), VirtualTableScenario.DS));
        assertEquals(
                rando.classifyGateFormationPieceIfRemoved(
                        scn.game(), VirtualTableScenario.DS, watto),
                chosen.classifyGateFormationPieceIfRemoved(
                        scn.game(), VirtualTableScenario.DS, watto));
        assertEquals(
                rando.classifyGateFormationPieceIfRemoved(
                        scn.game(), VirtualTableScenario.DS, televan),
                chosen.classifyGateFormationPieceIfRemoved(
                        scn.game(), VirtualTableScenario.DS, televan));
        assertEquals(
                rando.isFlipBackProtectionLocation(
                        junkyard, scn.game(), VirtualTableScenario.DS),
                chosen.isFlipBackProtectionLocation(
                        junkyard, scn.game(), VirtualTableScenario.DS));
    }

    @Test
    public void nmnpndBoundedWattoParentEvidenceAndManualLegalProgressionFlipsNatively() {
        var scn = noMoneyScenario();
        var objective = scn.GetDSCard("objective");
        var junkyard = scn.GetDSCard("junkyard");
        var mosEspa = scn.GetDSCard("mosEspa");
        var watto = scn.GetDSCard("watto");
        var sebulba = scn.GetDSCard("sebulba");

        scn.MoveCardsToDSHand(sebulba);
        scn.MoveCardsToBottomOfDSReserveDeck(watto);
        scn.StartGame();
        keepOnlyDarkHandCards(scn, sebulba);
        scn.SkipToDSTurn(Phase.DEPLOY);
        keepExactlyDarkForce(scn, 0);
        assertEquals("Watto's printed deploy 2 is reduced to 0 at the Junkyard",
                0.0f,
                scn.game().getModifiersQuerying().getDeployCost(
                    scn.gameState(), junkyard, watto, junkyard,
                    false, null, false, 0.0f,
                    null, false),
                0.0f);

        var bots = PublicBots.forGame(scn);
        boolean pulledWatto = false;
        boolean deployedSebulba = false;
        PhysicalCard pulledWattoCard = null;
        for (int routeStep = 0; routeStep < 2; routeStep++) {
            AwaitingDecision parent = scn.GetAwaitingDecision(
                    VirtualTableScenario.DS);
            assertNotNull("Dark Side route action missing; current="
                            + (scn.GetCurrentDecision() != null
                                ? scn.GetCurrentDecision().getText() : "none")
                            + "; deciding=" + scn.GetDecidingPlayer(),
                    parent);
            bots.decideBoth(scn);
            PhysicalCardImpl selectedSource = routeStep == 0
                    ? junkyard : sebulba;
            String selectedAction = scn.GetCardActionId(
                    VirtualTableScenario.DS, selectedSource,
                    routeStep == 0 ? "Deploy Watto" : "Deploy");
            assertNotNull(selectedAction);
            if (routeStep == 0) {
                assertPolicyDelta(
                        PullActionPolicy.scoreNoMoneyNoPartsWattoRoute(
                                selectedAction, true),
                        "OBJECTIVE.NO_MONEY.WATTO_ROUTE", 300.0f);
            }
            scn.DSDecided(selectedAction);

            AwaitingDecision child = scn.GetAwaitingDecision(
                    VirtualTableScenario.DS);
            assertNotNull(child);
            String childResponse = bots.decideBoth(scn);
            if (selectedSource == junkyard) {
                String[] offeredIds = child.getDecisionParameters()
                        .get("cardId");
                String[] offeredBlueprints = child.getDecisionParameters()
                        .get("blueprintId");
                String[] selectable = child.getDecisionParameters()
                        .get("selectable");
                assertNotNull(offeredIds);
                assertNotNull(offeredBlueprints);
                assertNotNull(selectable);
                assertEquals("The native Watto child must require one selection",
                        "1", child.getDecisionParameters().get("min")[0]);
                assertEquals("1",
                        child.getDecisionParameters().get("max")[0]);
                assertTrue("The mirrored public response must be an offered token; response="
                                + childResponse + "; offered="
                                + java.util.Arrays.toString(offeredIds),
                        !childResponse.isBlank()
                                && java.util.Arrays.asList(offeredIds)
                                    .contains(childResponse));
                int offeredIndex = -1;
                var reserve = scn.gameState().getReserveDeck(
                        VirtualTableScenario.DS);
                for (int index = 0; index < offeredIds.length; index++) {
                    PhysicalCard candidate = reserve.get(index);
                    if ("true".equals(selectable[index])
                            && offeredBlueprints[index].equals(
                                candidate.getBlueprintId(true))
                            && com.gempukku.swccgo.filters.Filters.Watto
                                .accepts(
                                    scn.gameState(),
                                    scn.game().getModifiersQuerying(),
                                    candidate)) {
                        offeredIndex = index;
                        break;
                    }
                }
                assertTrue("The native child must offer a legal Watto token",
                        offeredIndex >= 0);
                assertEquals("true", selectable[offeredIndex]);
                String wattoResponse = offeredIds[offeredIndex];
                assertTrue("Reserve Deck selection must use its offered temp token",
                        wattoResponse.startsWith("temp"));
                PhysicalCard selectedWatto = reserve.get(offeredIndex);
                assertEquals(selectedWatto.getBlueprintId(true),
                        offeredBlueprints[offeredIndex]);
                assertTrue("Manual legal native progression must select a Watto printing",
                        com.gempukku.swccgo.filters.Filters.Watto.accepts(
                            scn.gameState(),
                            scn.game().getModifiersQuerying(),
                            selectedWatto));
                pulledWattoCard = selectedWatto;
                pulledWatto = true;
                scn.DSDecided(wattoResponse);
            } else {
                assertTrue("Manual legal Watto progression must precede the Mos Espa body; parent="
                                + parent.getDecisionParameters().entrySet()
                                    .stream()
                                    .map(entry -> entry.getKey() + "="
                                        + java.util.Arrays.toString(
                                            entry.getValue()))
                                    .toList(),
                        pulledWatto);
                assertSame("Watto must already be present at the Junkyard",
                        junkyard,
                        scn.game().getModifiersQuerying()
                            .getLocationThatCardIsPresentAt(
                                scn.gameState(), pulledWattoCard));
                var analyzer = new com.gempukku.swccgo.ai.models.rando
                        .strategy.ObjectiveAnalyzer();
                analyzer.analyze(scn.game(), VirtualTableScenario.DS,
                        Side.DARK);
                assertTrue("Sebulba at manual legal Mos Espa must be the final native flip leg",
                        analyzer.wouldCompletePreFlipRequirementAt(
                            scn.game(), VirtualTableScenario.DS,
                            sebulba, mosEspa));
                assertPolicyDelta(
                        DeployObjectiveSitingPolicy
                            .scoreActorRuntimeLocation(
                                Integer.toString(mosEspa.getCardId()), true),
                        "DEPLOY.OBJECTIVE.ACTOR_RUNTIME_LOCATION",
                        300.0f);
                deployedSebulba = true;
                scn.DSChooseCard(mosEspa);
            }
            scn.PassAllResponses();
            if (routeStep == 0) {
                assertFalse("One leg alone must leave the objective front up",
                        objective.isFlipped());
                if (scn.GetAwaitingDecision(
                        VirtualTableScenario.LS) != null) {
                    scn.LSPass();
                }
            }
        }

        assertTrue(pulledWatto);
        assertTrue(deployedSebulba);
        assertSame(junkyard,
                scn.game().getModifiersQuerying()
                    .getLocationThatCardIsPresentAt(
                        scn.gameState(), pulledWattoCard));
        assertSame(mosEspa,
                scn.game().getModifiersQuerying()
                    .getLocationThatCardIsPresentAt(
                        scn.gameState(), sebulba));
        assertEquals("Both source-defined deployments are free",
                0, scn.GetDSForcePileCount());
        assertTrue("After manual legal Watto and Sebulba progression, the unchanged objective Java must flip",
                objective.isFlipped());
    }

    @Test
    public void nmnpndPublicBotsMoveSebulbaNotWattoAndNativelyFlip() {
        var scn = noMoneyScenario();
        var objective = scn.GetDSCard("objective");
        var junkyard = scn.GetDSCard("junkyard");
        var mosEspa = scn.GetDSCard("mosEspa");
        var watto = scn.GetDSCard("watto");
        var sebulba = scn.GetDSCard("sebulba");

        scn.StartGame();
        scn.MoveCardsToLocation(junkyard, watto, sebulba);
        scn.DSActivateForceCheat(1);
        scn.SkipToDSTurn(Phase.MOVE);
        keepExactlyDarkForce(scn, 1);

        String wattoMove = scn.GetCardActionId(
                VirtualTableScenario.DS, watto,
                "Move using landspeed");
        String sebulbaMove = scn.GetCardActionId(
                VirtualTableScenario.DS, sebulba,
                "Move using landspeed");
        assertNotNull(wattoMove);
        assertNotNull(sebulbaMove);

        var bots = PublicBots.forGame(scn);
        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertTrue(analyzer.advancesPreFlipPlainPresenceAtRequiredLocation(
                scn.game(), VirtualTableScenario.DS,
                sebulba, mosEspa));
        TracedDecision tracedMove = bots.decideBothWithRandoTrace(scn);
        long startOperations = tracedMove.trace().getOperations().stream()
                .filter(operation -> sebulbaMove.equals(
                            operation.getActionId())
                        && "MOVE.OBJECTIVE.ACTOR_LOCATION_START"
                            .equals(operation.getRuleId().id())
                        && operation.getDeltaBits() != null
                        && operation.getDeltaBits()
                            == Float.floatToRawIntBits(300.0f))
                .count();
        assertEquals("Sebulba's offered move must carry one bounded objective preference",
                1, startOperations);
        scn.DSDecided(sebulbaMove);
        bots.decideBoth(scn);
        assertTrue(scn.DSHasCardChoiceAvailable(mosEspa));
        scn.DSChooseCard(mosEspa);
        scn.PassAllResponses();

        assertSame(junkyard,
                scn.game().getModifiersQuerying()
                    .getLocationThatCardIsPresentAt(
                        scn.gameState(), watto));
        assertSame(mosEspa,
                scn.game().getModifiersQuerying()
                    .getLocationThatCardIsPresentAt(
                        scn.gameState(), sebulba));
        assertEquals(0, scn.GetDSForcePileCount());
        assertTrue("The unchanged objective Java must flip after the move",
                objective.isFlipped());
    }

    @Test
    public void nmnpndAnalyzerReservesOnlyTheExecutableMosEspaMove() {
        var scn = noMoneyScenario();
        var junkyard = scn.GetDSCard("junkyard");
        var mosEspa = scn.GetDSCard("mosEspa");
        var watto = scn.GetDSCard("watto");
        var sebulba = scn.GetDSCard("sebulba");
        var gunner = scn.GetDSCard("gunner");

        scn.MoveCardsToDSHand(gunner);
        scn.StartGame();
        scn.MoveCardsToLocation(junkyard, watto, sebulba);
        keepOnlyDarkHandCards(scn, gunner);
        scn.DSActivateForceCheat(1);
        scn.SkipToDSTurn(Phase.DEPLOY);
        keepExactlyDarkForce(scn, 1);

        var rando = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.strategy
                .ObjectiveAnalyzer();
        rando.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        chosen.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);

        assertEquals("One Force must fund Sebulba's exact Mos Espa move",
                1, rando.getNoMoneyNoPartsCurrentMoveForceReserve(
                        scn.game(), VirtualTableScenario.DS));
        assertEquals("A direct Mos Espa occupier replaces the move",
                0, rando.getNoMoneyNoPartsCurrentMoveForceReserve(
                        scn.game(), VirtualTableScenario.DS, gunner));
        assertEquals(
                rando.getNoMoneyNoPartsCurrentMoveForceReserve(
                        scn.game(), VirtualTableScenario.DS),
                chosen.getNoMoneyNoPartsCurrentMoveForceReserve(
                        scn.game(), VirtualTableScenario.DS));

        scn.MoveCardsToLocation(mosEspa, sebulba);
        assertEquals("No reserve remains after Mos Espa is occupied",
                0, rando.getNoMoneyNoPartsCurrentMoveForceReserve(
                        scn.game(), VirtualTableScenario.DS));
    }

    @Test
    public void nmnpndPublicBotsPreserveDeployAndBattleForceThenMoveAndFlip() {
        var scn = noMoneyScenario();
        var objective = scn.GetDSCard("objective");
        var junkyard = scn.GetDSCard("junkyard");
        var mosEspa = scn.GetDSCard("mosEspa");
        var system = scn.GetDSCard("system");
        var watto = scn.GetDSCard("watto");
        var sebulba = scn.GetDSCard("sebulba");
        var tie = scn.GetDSCard("tie");
        var battleSite = scn.GetLSStartingLocation();

        scn.MoveCardsToDSHand(tie);
        scn.StartGame();
        scn.MoveLocationToTable(system);
        scn.MoveCardsToLocation(junkyard, watto, sebulba);
        scn.MoveCardsToLocation(
                battleSite,
                scn.GetDSFiller(1),
                scn.GetDSFiller(2),
                scn.GetDSFiller(3),
                scn.GetDSFiller(4),
                scn.GetLSFiller(1));
        keepOnlyDarkHandCards(scn, tie);
        scn.DSActivateForceCheat(1);
        scn.SkipToDSTurn(Phase.DEPLOY);
        keepExactlyDarkForce(scn, 1);

        String tieDeploy = scn.GetCardActionId(
                VirtualTableScenario.DS, tie, "Deploy");
        assertNotNull("The unrelated one-Force TIE deploy must be offered",
                tieDeploy);
        var bots = PublicBots.forGame(scn);
        assertPolicyDelta(
                NoMoneyNoPartsObjectivePolicy
                    .preserveMoveForceForOrdinaryDeploy(
                        tieDeploy, true, 1, 1, 1, 1),
                "OBJECTIVE.NO_MONEY.MOVE_FORCE_RESERVE", -300.0f);
        bots.decideBoth(scn);
        scn.DSPass();
        assertTrue(scn.AwaitingLSDeployPhaseActions());
        scn.LSPass();

        assertTrue(scn.AwaitingDSBattlePhaseActions());
        String battle = scn.GetCardActionId(
                VirtualTableScenario.DS, battleSite,
                "Initiate battle");
        assertNotNull("A favorable unrelated battle must be available",
                battle);
        assertPolicyDelta(
                ObjectiveBattlePolicy.preserveObjectiveMoveForce(
                        battle, 1, 1, 1.0f),
                ObjectiveBattlePolicy.OBJECTIVE_MOVE_FORCE_RESERVE_RULE_ID,
                -300.0f);
        bots.decideBoth(scn);
        scn.DSPass();
        assertTrue(scn.AwaitingLSBattlePhaseActions());
        scn.LSPass();

        assertTrue(scn.AwaitingDSMovePhaseActions());
        assertEquals(1, scn.GetDSForcePileCount());
        String sebulbaMove = scn.GetCardActionId(
                VirtualTableScenario.DS, sebulba,
                "Move using landspeed");
        assertNotNull(sebulbaMove);
        MoveDestinationPolicy.Contribution startPreference =
                MoveDestinationPolicy.objectiveActorLocationStart(
                        true, sebulba.getTitle());
        assertTrue(startPreference.applies());
        assertEquals(300.0f, startPreference.delta(), 0.0f);
        bots.decideBoth(scn);
        scn.DSDecided(sebulbaMove);
        MoveDestinationPolicy.Contribution destinationPreference =
                MoveDestinationPolicy.objectiveActorLocationDestination(
                        true, sebulba.getTitle(), mosEspa.getTitle());
        assertTrue(destinationPreference.applies());
        assertEquals(300.0f, destinationPreference.delta(), 0.0f);
        bots.decideBoth(scn);
        scn.DSDecided(Integer.toString(mosEspa.getCardId()));
        scn.PassAllResponses();

        assertEquals(0, scn.GetDSForcePileCount());
        assertSame(junkyard,
                scn.game().getModifiersQuerying()
                    .getLocationThatCardIsPresentAt(
                        scn.gameState(), watto));
        assertSame(mosEspa,
                scn.game().getModifiersQuerying()
                    .getLocationThatCardIsPresentAt(
                        scn.gameState(), sebulba));
        assertTrue("The unchanged objective Java must flip after the funded move",
                objective.isFlipped());
    }

    @Test
    public void nmnpndRealForceLossPreservesTheCheapestMosEspaBody() {
        var scn = noMoneyScenario();
        var gunner = scn.GetDSCard("gunner");
        var tie = scn.GetDSCard("tie");

        scn.MoveCardsToDSHand(gunner, tie);
        scn.StartGame();
        scn.MoveCardsToLocation(
                scn.GetLSStartingLocation(),
                scn.GetLSFiller(1));
        scn.MoveCardsToDSHand(gunner, tie);
        keepOnlyDarkHandCards(scn, gunner, tie);
        scn.LSActivateForceCheat(4);
        scn.SkipToLSTurn(Phase.CONTROL);

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertTrue("The sole cheapest Mos Espa body must be loss-protected",
                analyzer.isPreferredCountedObjectivePresenceForceLossCandidate(
                        scn.game(), VirtualTableScenario.DS, gunner));

        scn.LSForceDrainAt(scn.GetLSStartingLocation());
        scn.PassAllResponses();
        assertTrue("The real drain must open Dark Side Force loss",
                scn.DSDecisionAvailable("Choose Force to lose"));
        AwaitingDecision decision = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        var offered = java.util.Arrays.asList(
                decision.getDecisionParameters().get("cardId"));
        assertTrue(offered.contains(
                Integer.toString(gunner.getCardId())));
        assertTrue(offered.contains(
                Integer.toString(tie.getCardId())));

        String loss = PublicBots.forGame(scn).decideBoth(scn);
        assertFalse("The public bots must not lose the sole cheap Mos Espa body; selected="
                        + loss,
                Integer.toString(gunner.getCardId()).equals(loss));
        scn.DSDecided(loss);
        scn.PassAllResponses();

        assertEquals(Zone.HAND, gunner.getZone());
    }

    @Test
    public void nmnpndBackGambitUsesADeployableCardAndNativeOpponentChoice() {
        var scn = noMoneyScenario();
        var objective = scn.GetDSCard("objective");
        var watto = scn.GetDSCard("watto");
        var duplicateWatto = scn.GetDSCard("wattoTwo");
        var televan = scn.GetDSCard("televan");
        var gunner = scn.GetDSCard("gunner");
        var tie = scn.GetDSCard("tie");

        flipWithFormation(scn, watto, televan);
        scn.MoveCardsToDSHand(duplicateWatto, gunner, tie);
        keepOnlyDarkHandCards(scn, duplicateWatto, gunner, tie);
        keepExactlyDarkForce(scn, 1);
        keepExactlyLightForce(scn, 2);
        if (scn.GetAwaitingDecision(VirtualTableScenario.LS) != null) {
            scn.LSPass();
        }

        var bots = PublicBots.forGame(scn);
        String gambit = scn.GetCardActionId(
                VirtualTableScenario.DS, objective,
                "Place card face down on side of table");
        String gunnerDeploy = scn.GetCardActionId(
                VirtualTableScenario.DS, gunner, "Deploy");
        assertNotNull(gambit);
        assertNotNull(gunnerDeploy);
        assertPolicyDelta(
                NoMoneyNoPartsObjectivePolicy.scoreBackGambitParent(
                        gambit, true, true),
                "OBJECTIVE.NO_MONEY.GAMBIT_PARENT", 300.0f);
        bots.decideBoth(scn);
        scn.DSDecided(gambit);

        AwaitingDecision cardChoice = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        assertNotNull(cardChoice);
        assertPolicyDelta(
                NoMoneyNoPartsObjectivePolicy.scoreBackGambitCandidate(
                        Integer.toString(gunner.getCardId()),
                        true, true, true),
                "OBJECTIVE.NO_MONEY.GAMBIT_CARD_SAFE", 300.0f);
        bots.decideBoth(scn);
        scn.DSDecided(Integer.toString(gunner.getCardId()));

        AwaitingDecision response = scn.GetAwaitingDecision(
                VirtualTableScenario.LS);
        assertNotNull(response);
        int lightUsedBefore = scn.GetLSUsedPileCount();
        int lightLostBefore = scn.GetLSLostPileCount();
        assertEquals("Native Light Side evaluation should use, not lose, two Force",
                "1", bots.decideLightBoth(scn));
        scn.LSDecided("1");
        scn.PassAllResponses();

        AwaitingDecision destination = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        assertNotNull("The free Gunner deploy must request a destination",
                destination);
        scn.DSDecided(bots.decideBoth(scn));
        scn.PassAllResponses();

        assertTrue(gunner.getZone().isInPlay());
        assertEquals("The gambit spends no Dark Side Force",
                1, scn.GetDSForcePileCount());
        assertEquals("Using two Force moves exactly two Light cards to Used",
                lightUsedBefore + 2, scn.GetLSUsedPileCount());
        assertEquals("The native use branch causes no Light Side life loss",
                lightLostBefore, scn.GetLSLostPileCount());
        assertTrue("Both retention legs remain live after the gambit",
                objective.isFlipped());
    }

    @Test
    public void nmnpndOpponentWattoRemovalBonusDoesNotFireOnTheFront() {
        var scn = noMoneyScenario();
        var objective = scn.GetDSCard("objective");

        scn.StartGame();

        assertFalse(NoMoneyNoPartsObjectivePolicy
                .isExactOpponentWattoRemovalAction(
                    scn.game(), VirtualTableScenario.LS,
                    objective, "Place Watto in Used Pile"));
    }

    @Test
    public void nmnpndOpponentPublicBotsRemoveWattoOnlyFromTheBackAndRetrieveMax() {
        var scn = noMoneyScenario();
        var objective = scn.GetDSCard("objective");
        var watto = scn.GetDSCard("watto");
        var televan = scn.GetDSCard("televan");

        flipWithFormation(scn, watto, televan);
        scn.MoveCardsToTopOfDSLostPile(
                scn.GetDSFiller(10),
                scn.GetDSFiller(11),
                scn.GetDSFiller(12),
                scn.GetDSFiller(13));
        scn.SkipToLSTurn(Phase.DEPLOY);
        keepExactlyLightForce(scn, 8);

        var bots = PublicBots.forGame(scn);
        String removal = scn.GetCardActionId(
                VirtualTableScenario.LS, objective,
                "Place Watto in Used Pile");
        assertNotNull(removal);
        assertEquals("The exact back-side counter must beat Pass",
                removal, bots.decideLightBoth(scn));
        scn.LSDecided(removal);

        assertEquals(Integer.toString(watto.getCardId()),
                bots.decideLightBoth(scn));
        scn.LSDecided(Integer.toString(watto.getCardId()));
        scn.PassAllResponses();

        assertEquals("The native retrieval decision must choose the maximum",
                "4", bots.decideBoth(scn));
        scn.DSDecided("4");
        scn.PassAllResponses();

        assertEquals(0, scn.GetLSForcePileCount());
        assertEquals(Zone.USED_PILE, watto.getZone());
        assertEquals("Removing Watto must trigger the unchanged flip-back law",
                false, objective.isFlipped());
        assertEquals("Four retrieved Force leave the Lost Pile",
                0, scn.GetDSLostPileCount());
    }
}
