package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.ai.models.common.phase.BattleForfeitPolicy;
import com.gempukku.swccgo.ai.models.common.phase.DeployBudgetPolicy;
import com.gempukku.swccgo.ai.models.common.phase.ForceLossFacts;
import com.gempukku.swccgo.ai.models.common.phase.ForceLossPolicy;
import com.gempukku.swccgo.ai.models.common.phase.MoveDestinationPolicy;
import com.gempukku.swccgo.ai.models.common.phase.MoveObjectiveGateHoldPolicy;
import com.gempukku.swccgo.ai.models.common.phase.ObjectiveBattlePolicy;
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
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Batch Twelve (2026-07-27): native engine contract for Imperial
 * Entanglements / No One To Stop Us This Time (201_39, DARK). Card Java
 * unchanged.
 *
 * Law (Card201_039.java L125-L145): flips when you control at least three
 * Tatooine sites while the opponent controls fewer than three. Back
 * (Card201_039_BACK.java L137-L158): flips back when the opponent controls
 * strictly more Tatooine sites than you; equal counts hold. All fixture
 * deploys by the owner respect the objective's own Imperial-only deploy ban
 * (fillers are stormtroopers), and all sites are dark printings to avoid
 * same-title conversion arithmetic.
 */
public class ImperialEntanglementsObjectiveEngineContractTest {

    private record DecisionChoice(
            String actionId, String cardId, String blueprintId, float score,
            List<String> reasoning) {
    }

    private static final StartingSetup IMPERIAL_ENTANGLEMENTS = new StartingSetup() {
        @Override
        public HashMap<String, String> Cards() {
            return new HashMap<>() {{
                put("objective", "201_39");
                put("system", "1_289");
                put("devastator", "1_301");
                put("mosEisley", "1_295");
            }};
        }

        @Override
        public void Setup(VirtualTableScenario scn) {
            // Required start deploys: Tatooine system, Devastator to it, and
            // one will-be-battleground Tatooine site. Side placement first
            // (its text also contains "deploy"), then site multi-match picks
            // the designated starting site.
            for (int i = 0; i < 8; i++) {
                if (scn.DSDecisionAvailable("On which side")) {
                    scn.DSChoose("Left");
                } else if (scn.DSDecisionAvailable("site to deploy")) {
                    scn.DSChooseCard(scn.GetDSCard("mosEisley"));
                } else if (scn.DSDecisionAvailable("to deploy")) {
                    scn.DSChooseCard(scn.GetDSCard("mosEisley"));
                }
            }
        }
    };

    private VirtualTableScenario ieScenario() {
        return new VirtualTableScenario(
                new HashMap<>() {{
                    put("routeBattleOpponent", "1_004");
                }},
                new HashMap<>() {{
                    put("cantina", "1_290");
                    put("dockingBay94", "1_291");
                    put("jawaCamp", "1_292");
                    put("jundland", "1_293");
                    put("larsFarm", "1_294");
                    put("bluffs", "2_150");
                    put("jabbasPalace", "6_171");
                    put("pilot", "1_180");
                    put("cpi", "2_130");
                    put("routeMover", "10_040");
                    put("routeDeployBody", "1_179");
                }},
                24,
                24,
                StartingSetup.DefaultLSGroundLocation,
                IMPERIAL_ENTANGLEMENTS,
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

    private boolean dsBlueprintSelectable(
            VirtualTableScenario scn, PhysicalCard card) {
        var blueprints = scn.DSGetBPChoices();
        var selectable = scn.DSGetADParam("selectable");
        for (int i = 0; i < blueprints.size()
                && i < selectable.length; i++) {
            if (card.getBlueprintId(true).equals(blueprints.get(i))
                    && Boolean.parseBoolean(selectable[i])) {
                return true;
            }
        }
        return false;
    }

    private DecisionChoice evaluateDecisionBoth(
            VirtualTableScenario scn,
            com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer
                    randoAnalyzer,
            com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer
                    chosenAnalyzer,
            AwaitingDecision decision) {
        return evaluateDecisionBoth(
                scn, randoAnalyzer, chosenAnalyzer,
                decision, null, null);
    }

    private DecisionChoice evaluateDecisionBoth(
            VirtualTableScenario scn,
            com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer
                    randoAnalyzer,
            com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer
                    chosenAnalyzer,
            AwaitingDecision decision,
            Integer deployingPermanentId,
            Integer moverCardId) {
        assertNotNull(decision);
        Map<String, String[]> params = decision.getDecisionParameters();
        List<String> actionIds = strings(params, "actionId");
        List<String> actionTexts = strings(params, "actionText");
        List<String> cardIds = strings(params, "cardId");
        List<String> blueprints = strings(params, "blueprintId");
        List<Boolean> selectable = strings(params, "selectable").stream()
                .map(Boolean::parseBoolean).toList();
        int min = integer(params, "min", 0);
        int max = integer(params, "max", 1);

        var randoContext =
                new com.gempukku.swccgo.ai.models.rando.evaluators
                    .DecisionContext(
                        scn.gameState(), VirtualTableScenario.DS,
                        decision.getDecisionType().name(),
                        decision.getText(),
                        Integer.toString(decision.getAwaitingDecisionId()),
                        scn.gameState().getCurrentPhase());
        randoContext.setGame(scn.game());
        randoContext.setSide(Side.DARK);
        randoContext.setObjectiveAnalyzer(randoAnalyzer);
        randoContext.setActionIds(actionIds);
        randoContext.setActionTexts(actionTexts);
        randoContext.setCardIds(cardIds);
        randoContext.setBlueprints(blueprints);
        randoContext.setSelectable(selectable);
        randoContext.setTestingTexts(strings(params, "testingText"));
        randoContext.setMin(min);
        randoContext.setMax(max);
        randoContext.setNoPass(bool(params, "noPass", true));
        if (deployingPermanentId != null) {
            randoContext.setExtra(
                    ObjectiveAnalyzer.OBJECTIVE_DEPLOYING_CARD_ID_EXTRA,
                    deployingPermanentId);
        }
        if (moverCardId != null) {
            randoContext.setExtra(
                    com.gempukku.swccgo.ai.models.common.phase
                        .MovePhysicalCardResolver.MOVER_CARD_ID_EXTRA,
                    moverCardId);
        }
        if (decision.getDecisionType().name().equals(
                    "CARD_ACTION_CHOICE")
                && scn.gameState().getCurrentPhase() == Phase.DEPLOY) {
            var script = new com.gempukku.swccgo.ai.models.rando.strategy
                    .DeployPhaseScript().selectAllowedActions(
                        decision, scn.gameState(), scn.game(),
                        VirtualTableScenario.DS, randoAnalyzer);
            randoContext.setAllowedActionIds(script.allowedActionIds);
            randoContext.setAllowedActionsReason(script.reason);
            randoContext.setStepBuckets(script.stepBuckets);
            randoContext.setStepBucketLabels(script.stepBucketLabels);
        }

        var chosenContext =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators
                    .DecisionContext(
                        scn.gameState(), VirtualTableScenario.DS,
                        decision.getDecisionType().name(),
                        decision.getText(),
                        Integer.toString(decision.getAwaitingDecisionId()),
                        scn.gameState().getCurrentPhase());
        chosenContext.setGame(scn.game());
        chosenContext.setSide(Side.DARK);
        chosenContext.setObjectiveAnalyzer(chosenAnalyzer);
        chosenContext.setActionIds(actionIds);
        chosenContext.setActionTexts(actionTexts);
        chosenContext.setCardIds(cardIds);
        chosenContext.setBlueprints(blueprints);
        chosenContext.setSelectable(selectable);
        chosenContext.setTestingTexts(strings(params, "testingText"));
        chosenContext.setMin(min);
        chosenContext.setMax(max);
        chosenContext.setNoPass(bool(params, "noPass", true));
        if (deployingPermanentId != null) {
            chosenContext.setExtra(
                    ObjectiveAnalyzer.OBJECTIVE_DEPLOYING_CARD_ID_EXTRA,
                    deployingPermanentId);
        }
        if (moverCardId != null) {
            chosenContext.setExtra(
                    com.gempukku.swccgo.ai.models.common.phase
                        .MovePhysicalCardResolver.MOVER_CARD_ID_EXTRA,
                    moverCardId);
        }
        if (decision.getDecisionType().name().equals(
                    "CARD_ACTION_CHOICE")
                && scn.gameState().getCurrentPhase() == Phase.DEPLOY) {
            var script = new com.gempukku.swccgo.ai.models.chosenone.strategy
                    .DeployPhaseScript().selectAllowedActions(
                        decision, scn.gameState(), scn.game(),
                        VirtualTableScenario.DS, chosenAnalyzer);
            chosenContext.setAllowedActionIds(script.allowedActionIds);
            chosenContext.setAllowedActionsReason(script.reason);
            chosenContext.setStepBuckets(script.stepBuckets);
            chosenContext.setStepBucketLabels(script.stepBucketLabels);
        }

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators
                .CombinedEvaluator().evaluateDecision(randoContext);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators
                .CombinedEvaluator().evaluateDecision(chosenContext);
        assertNotNull(rando);
        assertNotNull(chosen);
        assertEquals(rando.getActionId(), chosen.getActionId());
        assertEquals(rando.getScore(), chosen.getScore(), 0.0f);
        assertEquals(rando.getReasoning(), chosen.getReasoning());
        int winnerIndex = actionIds.indexOf(rando.getActionId());
        if (winnerIndex < 0) {
            winnerIndex = cardIds.indexOf(rando.getActionId());
        }
        String winnerCardId = winnerIndex >= 0 && winnerIndex < cardIds.size()
                ? cardIds.get(winnerIndex) : null;
        String winnerBlueprint = winnerIndex >= 0
                && winnerIndex < blueprints.size()
                ? blueprints.get(winnerIndex) : null;
        if (winnerBlueprint == null && winnerCardId != null) {
            try {
                PhysicalCard winnerCard = scn.gameState().findCardById(
                        Integer.parseInt(winnerCardId));
                if (winnerCard != null) {
                    winnerBlueprint = winnerCard.getBlueprintId(true);
                }
            } catch (NumberFormatException ignored) {
                // Reserve Deck temp ids require the offered blueprint array.
            }
        }
        return new DecisionChoice(
                rando.getActionId(), winnerCardId, winnerBlueprint,
                rando.getScore(),
                List.copyOf(rando.getReasoning()));
    }

    private List<String> strings(
            Map<String, String[]> params, String key) {
        String[] values = params != null ? params.get(key) : null;
        return values == null ? List.of() : Arrays.asList(values);
    }

    private int integer(
            Map<String, String[]> params, String key, int fallback) {
        List<String> values = strings(params, key);
        if (values.isEmpty()) return fallback;
        try {
            return Integer.parseInt(values.getFirst());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private boolean bool(
            Map<String, String[]> params, String key,
            boolean fallback) {
        List<String> values = strings(params, key);
        return values.isEmpty()
                ? fallback : Boolean.parseBoolean(values.getFirst());
    }

    @Test
    public void ieFrontRequiresThreeControlledTatooineSites() {
        var scn = ieScenario();
        var objective = scn.GetDSCard("objective");
        var mosEisley = scn.GetDSCard("mosEisley");
        var cantina = scn.GetDSCard("cantina");
        var jawaCamp = scn.GetDSCard("jawaCamp");
        var pulseOne = scn.GetDSFiller(4);
        var thirdTrooper = scn.GetDSFiller(5);

        scn.MoveCardsToDSHand(pulseOne, thirdTrooper);
        scn.StartGame();
        moveSiteToTatooine(scn, cantina);
        moveSiteToTatooine(scn, jawaCamp);
        scn.MoveCardsToLocation(mosEisley, scn.GetDSFiller(1));
        scn.MoveCardsToLocation(cantina, scn.GetDSFiller(2));

        scn.DSActivateForceCheat(12);
        scn.SkipToPhase(Phase.DEPLOY);

        scn.DSDeployCardAndPassResponses(
                pulseOne, scn.GetLSStartingLocation());
        assertFalse("Two controlled Tatooine sites must not flip",
                objective.isFlipped());
        scn.LSPass();

        scn.DSDeployCardAndPassResponses(thirdTrooper, jawaCamp);
        assertTrue("Three controlled Tatooine sites must flip",
                objective.isFlipped());
    }

    @Test
    public void ieFrontIsBlockedWhileOpponentAlsoControlsThreeSites() {
        var scn = ieScenario();
        var objective = scn.GetDSCard("objective");
        var mosEisley = scn.GetDSCard("mosEisley");
        var cantina = scn.GetDSCard("cantina");
        var jawaCamp = scn.GetDSCard("jawaCamp");
        var dockingBay94 = scn.GetDSCard("dockingBay94");
        var jundland = scn.GetDSCard("jundland");
        var larsFarm = scn.GetDSCard("larsFarm");
        var pulseOne = scn.GetDSFiller(4);
        var pulseTwo = scn.GetDSFiller(5);

        scn.MoveCardsToDSHand(pulseOne, pulseTwo);
        scn.StartGame();
        moveSiteToTatooine(scn, cantina);
        moveSiteToTatooine(scn, jawaCamp);
        moveSiteToTatooine(scn, dockingBay94);
        moveSiteToTatooine(scn, jundland);
        moveSiteToTatooine(scn, larsFarm);
        scn.MoveCardsToLocation(mosEisley, scn.GetDSFiller(1));
        scn.MoveCardsToLocation(cantina, scn.GetDSFiller(2));
        scn.MoveCardsToLocation(jawaCamp, scn.GetDSFiller(3));
        scn.MoveCardsToLocation(dockingBay94, scn.GetLSFiller(1));
        scn.MoveCardsToLocation(jundland, scn.GetLSFiller(2));
        scn.MoveCardsToLocation(larsFarm, scn.GetLSFiller(3));

        scn.DSActivateForceCheat(12);
        scn.SkipToPhase(Phase.DEPLOY);

        scn.DSDeployCardAndPassResponses(
                pulseOne, scn.GetLSStartingLocation());
        assertFalse("Opponent control of three Tatooine sites must block the flip",
                objective.isFlipped());
        scn.LSPass();

        scn.MoveOutOfPlay(scn.GetLSFiller(3));
        scn.DSDeployCardAndPassResponses(
                pulseTwo, scn.GetLSStartingLocation());
        assertTrue("Opponent dropping to two controlled sites must allow the flip",
                objective.isFlipped());
    }

    @Test
    public void ieBackHoldsAtEqualCountsAndFlipsBackWhenOutcontrolled() {
        var scn = ieScenario();
        var objective = scn.GetDSCard("objective");
        var mosEisley = scn.GetDSCard("mosEisley");
        var cantina = scn.GetDSCard("cantina");
        var jawaCamp = scn.GetDSCard("jawaCamp");
        var dockingBay94 = scn.GetDSCard("dockingBay94");
        var jundland = scn.GetDSCard("jundland");
        var larsFarm = scn.GetDSCard("larsFarm");
        var pulseOne = scn.GetDSFiller(4);
        var pulseTwo = scn.GetDSFiller(5);

        scn.MoveCardsToDSHand(pulseOne, pulseTwo);
        scn.StartGame();
        moveSiteToTatooine(scn, cantina);
        moveSiteToTatooine(scn, jawaCamp);
        moveSiteToTatooine(scn, dockingBay94);
        moveSiteToTatooine(scn, jundland);
        scn.MoveCardsToBottomOfDSReserveDeck(larsFarm);
        scn.MoveCardsToLocation(mosEisley, scn.GetDSFiller(1));
        scn.MoveCardsToLocation(cantina, scn.GetDSFiller(2));
        scn.MoveCardsToLocation(jawaCamp, scn.GetDSFiller(3));

        scn.DSActivateForceCheat(16);
        scn.SkipToPhase(Phase.DEPLOY);
        scn.DSDeployCardAndPassResponses(
                pulseOne, scn.GetLSStartingLocation());
        assertTrue("Three controlled sites must flip", objective.isFlipped());
        scn.LSPass();

        // Collapse to 1-1 equality before the back-side route decision. The
        // free site is now a real buffer-building play rather than decorative
        // geography while safely ahead 3-0.
        scn.MoveOutOfPlay(scn.GetDSFiller(2));
        scn.MoveOutOfPlay(scn.GetDSFiller(3));
        scn.MoveCardsToLocation(dockingBay94, scn.GetLSFiller(1));
        while (scn.GetDSReserveDeckCount() > 2) {
            scn.MoveCardsToTopOfDSUsedPile(
                    scn.GetTopOfDSReserveDeck());
        }
        assertEquals("The exact back route is tested at the V60 boundary",
                2, scn.GetDSReserveDeckCount());

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);
        var chosenAnalyzer =
                new com.gempukku.swccgo.ai.models.chosenone.strategy
                    .ObjectiveAnalyzer();
        chosenAnalyzer.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertTrue(analyzer.usesObjectiveLocationPullSequence());
        assertTrue(analyzer.hasObjectiveLocationRouteCandidateInReserve(
                scn.game(), VirtualTableScenario.DS));

        String backSiteAction = scn.GetCardActionId(
                VirtualTableScenario.DS, objective,
                "Deploy Tatooine battleground site from Reserve Deck");
        assertNotNull(backSiteAction);
        AwaitingDecision backDecision = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        assertTrue("Exact back route source mismatch: objectiveId="
                        + objective.getCardId() + ", objectiveBp="
                        + objective.getBlueprintId(true) + ", analyzerBp="
                        + analyzer.getObjectiveBlueprintId() + ", actionCards="
                        + strings(backDecision.getDecisionParameters(), "cardId"),
                analyzer.isImperialEntanglementsBackSiteRouteAction(
                    scn.game(), VirtualTableScenario.DS, objective,
                    "Deploy Tatooine battleground site from Reserve Deck"));
        assertEquals("The native action must identify the objective source",
                Integer.toString(objective.getCardId()),
                strings(backDecision.getDecisionParameters(), "cardId").get(0));
        DecisionChoice backSiteWinner = evaluateDecisionBoth(
                scn, analyzer, chosenAnalyzer, backDecision);
        assertNotEquals("The bounded back-side route must not preempt the"
                        + " stronger normal deploy; actions="
                        + strings(backDecision.getDecisionParameters(),
                            "actionText")
                        + "; cardIds=" + strings(
                            backDecision.getDecisionParameters(), "cardId")
                        + "; blueprints=" + strings(
                            backDecision.getDecisionParameters(), "blueprintId")
                        + "; reasoning=" + backSiteWinner.reasoning(),
                backSiteAction, backSiteWinner.actionId());
        assertTrue("The winning normal deploy must retain its ordinary urgency",
                backSiteWinner.reasoning().stream().anyMatch(
                    reason -> reason.contains("DEPLOY URGENCY")));
        assertFalse("A wrong source never receives the narrow V60 bypass",
                analyzer.isImperialEntanglementsBackSiteRouteAction(
                    scn.game(), VirtualTableScenario.DS,
                    scn.GetDSCard("cpi"),
                    "Deploy Tatooine battleground site from Reserve Deck"));
        assertFalse("A wrong action never receives the narrow V60 bypass",
                analyzer.isImperialEntanglementsBackSiteRouteAction(
                    scn.game(), VirtualTableScenario.DS, objective,
                    "Take card into hand from Reserve Deck"));
        try {
            assertFalse("A same-blueprint counterfeit is not the live objective",
                    analyzer.isImperialEntanglementsBackSiteRouteAction(
                        scn.game(), VirtualTableScenario.DS,
                        objective.clone(),
                        "Deploy Tatooine battleground site from Reserve Deck"));
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }

        int forceBeforeBackDownload = scn.GetDSForcePileCount();
        assertTrue("The flipped objective must keep its once-per-turn site route",
                scn.DSCardActionAvailable(
                    objective,
                    "Deploy Tatooine battleground site from Reserve Deck"));
        scn.DSUseCardAction(
                objective,
                "Deploy Tatooine battleground site from Reserve Deck");
        assertTrue(dsBlueprintSelectable(scn, larsFarm));
        DecisionChoice backSiteCandidate = evaluateDecisionBoth(
                scn, analyzer, chosenAnalyzer,
                scn.GetAwaitingDecision(VirtualTableScenario.DS));
        assertEquals(larsFarm.getBlueprintId(true),
                backSiteCandidate.blueprintId());
        assertTrue(backSiteCandidate.reasoning().stream().anyMatch(
                reason -> reason.contains(
                    "IMPERIAL ENTANGLEMENTS BACK")));
        scn.DSChooseCard(larsFarm);
        scn.PassAllResponses();
        assertTrue("The downloaded site must reach its adjacent-site placement decision",
                scn.DSDecisionAvailable("next to (or convert)"));
        assertFalse(scn.DSGetCardChoices().isEmpty());
        scn.DSDecided(scn.DSGetCardChoices().getFirst());
        scn.PassAllResponses();
        if (scn.DSDecisionAvailable("On which side")) {
            scn.DSChoose("Right");
        }
        scn.PassAllResponses();
        assertEquals("The back-side site route remains free",
                forceBeforeBackDownload, scn.GetDSForcePileCount());
        assertEquals(Zone.LOCATIONS, larsFarm.getZone());
        assertFalse("With no legal site left, the route no longer bypasses V60",
                analyzer.isImperialEntanglementsBackSiteRouteAction(
                    scn.game(), VirtualTableScenario.DS, objective,
                    "Deploy Tatooine battleground site from Reserve Deck"));
        scn.LSPass();

        assertTrue("Equal Tatooine site counts must hold the back",
                objective.isFlipped());
        scn.LSPass();

        // Opponent takes a second site: strictly more flips the back.
        scn.MoveCardsToLocation(jundland, scn.GetLSFiller(2));
        scn.DSDeployCardAndPassResponses(
                pulseTwo, scn.GetLSStartingLocation());
        assertFalse("Strictly more opponent-controlled Tatooine sites must flip back",
                objective.isFlipped());
    }

    @Test
    public void ieProfileRulesTrackTheEngineLaw() {
        var scn = ieScenario();
        var mosEisley = scn.GetDSCard("mosEisley");
        var cantina = scn.GetDSCard("cantina");
        var jawaCamp = scn.GetDSCard("jawaCamp");

        scn.StartGame();
        moveSiteToTatooine(scn, cantina);
        moveSiteToTatooine(scn, jawaCamp);
        scn.MoveCardsToLocation(mosEisley, scn.GetDSFiller(1));
        scn.MoveCardsToLocation(cantina, scn.GetDSFiller(2));

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertTrue("Profile must hydrate for 201_39", analyzer.isAnalyzed());

        var preFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.DS, "preFlip", "flip");
        assertEquals("IE front encodes one rule", 1, preFlip.size());
        assertFalse("Two controlled sites leave the rule unmet",
                preFlip.get(0).conditionSatisfied());

        scn.MoveCardsToLocation(jawaCamp, scn.GetDSFiller(3));
        var complete = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.DS, "preFlip", "flip");
        assertTrue("Three controlled sites complete the encoded law",
                complete.get(0).conditionSatisfied());

        var postFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.DS, "postFlip", "flipBack");
        assertEquals("The back encodes one relative-count rule", 1,
                postFlip.size());
    }

    @Test
    public void ieNativePullIsFreeExactAndOncePerTurn() {
        var scn = ieScenario();
        var objective = scn.GetDSCard("objective");
        var cantina = scn.GetDSCard("cantina");
        var bluffs = scn.GetDSCard("bluffs");
        var jabbasPalace = scn.GetDSCard("jabbasPalace");

        scn.StartGame();
        scn.MoveCardsToDSHand(
                cantina,
                scn.GetDSCard("dockingBay94"),
                scn.GetDSCard("jawaCamp"),
                scn.GetDSCard("jundland"),
                scn.GetDSCard("larsFarm"));
        scn.MoveCardsToBottomOfDSReserveDeck(bluffs, jabbasPalace);
        scn.SkipToPhase(Phase.DEPLOY);
        while (scn.GetDSForcePileCount() > 0) {
            scn.MoveCardsToHand(scn.GetTopOfDSForcePile());
        }

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertTrue(analyzer.usesObjectiveLocationPullSequence());
        assertFalse("The Tatooine system is not one of the three flip sites",
                analyzer.isObjectiveRelevantLocation("Tatooine"));
        assertTrue(analyzer.isObjectiveRelevantLocation(
                "Tatooine: Cantina"));
        assertEquals(ObjectiveAnalyzer.ObjectiveProgressCandidateRole
                .REQUIRED_LOCATION,
                analyzer.classifyPreFlipProgressCandidate(
                    scn.game(), VirtualTableScenario.DS, bluffs));
        assertEquals(ObjectiveAnalyzer.ObjectiveProgressCandidateRole
                .REQUIRED_LOCATION,
                analyzer.classifyPreFlipProgressCandidate(
                    scn.game(), VirtualTableScenario.DS, jabbasPalace));
        assertFalse("Bluffs counts if deployed, but the native route cannot fetch it",
                analyzer.isNativeObjectiveLocationRouteCandidate(
                    scn.game(), VirtualTableScenario.DS, bluffs));
        assertFalse("IE itself forbids deploying Jabba's Palace sites",
                analyzer.isNativeObjectiveLocationRouteCandidate(
                    scn.game(), VirtualTableScenario.DS, jabbasPalace));
        assertFalse("Unreachable Reserve sites must not keep the native action prioritized",
                analyzer.hasMissingPreFlipRequiredLocationInReserve(
                    scn.game(), VirtualTableScenario.DS));

        scn.MoveCardsToBottomOfDSReserveDeck(cantina);
        assertTrue(analyzer.isNativeObjectiveLocationRouteCandidate(
                scn.game(), VirtualTableScenario.DS, cantina));
        assertTrue(analyzer.hasMissingPreFlipRequiredLocationInReserve(
                scn.game(), VirtualTableScenario.DS));

        assertTrue(scn.DSCardActionAvailable(
                objective,
                "Deploy Tatooine battleground site from Reserve Deck"));
        scn.DSUseCardAction(
                objective,
                "Deploy Tatooine battleground site from Reserve Deck");
        assertTrue(dsBlueprintSelectable(scn, cantina));
        assertFalse("A non-battleground Tatooine site is not a legal pull",
                dsBlueprintSelectable(scn, bluffs));
        assertFalse("The objective's permanent ban blocks Jabba's Palace",
                dsBlueprintSelectable(scn, jabbasPalace));
        scn.DSChooseCard(cantina);
        scn.PassAllResponses();
        if (scn.DSDecisionAvailable("On which side")) {
            scn.DSChoose("Right");
            scn.PassAllResponses();
        }

        assertEquals(0, scn.GetDSForcePileCount());
        assertEquals(Zone.LOCATIONS, cantina.getZone());
        scn.LSPass();
        assertFalse("The once-per-turn download must be exhausted",
                scn.DSCardActionAvailable(
                    objective,
                    "Deploy Tatooine battleground site from Reserve Deck"));
    }

    @Test
    public void ieContinuousFrontRouteRespectsDestinationSafety() {
        var scn = ieScenario();
        var objective = scn.GetDSCard("objective");
        var mosEisley = scn.GetDSCard("mosEisley");
        var cantina = scn.GetDSCard("cantina");
        var jawaCamp = scn.GetDSCard("jawaCamp");
        var mover = scn.GetDSCard("routeMover");
        var deployBody = scn.GetDSCard("routeDeployBody");
        var lossFodder = scn.GetDSCard("cpi");
        var anchor = scn.GetDSFiller(1);
        var battleOpponent = scn.GetLSCard("routeBattleOpponent");
        var drainBody = scn.GetLSFiller(2);

        scn.MoveCardsToDSHand(deployBody, lossFodder);
        scn.StartGame();
        scn.MoveOutOfPlay(scn.GetDSCard("dockingBay94"));
        scn.MoveOutOfPlay(scn.GetDSCard("jundland"));
        scn.MoveOutOfPlay(scn.GetDSCard("larsFarm"));
        scn.MoveCardsToLocation(mosEisley, mover, anchor);
        scn.MoveCardsToLocation(mosEisley, battleOpponent);
        scn.MoveCardsToLocation(scn.GetLSStartingLocation(), drainBody);

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        var chosenAnalyzer =
                new com.gempukku.swccgo.ai.models.chosenone.strategy
                    .ObjectiveAnalyzer();
        chosenAnalyzer.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);

        scn.LSActivateForceCheat(4);
        scn.SkipToLSTurn(Phase.CONTROL);
        scn.DSActivateForceCheat(4);
        scn.MoveCardsToDSHand(deployBody, lossFodder);
        keepOnlyDarkHandCards(scn, deployBody, lossFodder);
        scn.MoveCardsToTopOfDSReserveDeck(cantina);
        scn.MoveCardsToBottomOfDSReserveDeck(jawaCamp);
        scn.LSForceDrainAt(scn.GetLSStartingLocation());
        scn.PassAllResponses();
        assertTrue(scn.DSDecisionAvailable("Choose Force to lose"));
        DecisionChoice forceLoss = evaluateDecisionBoth(
                scn, analyzer, chosenAnalyzer,
                scn.GetAwaitingDecision(VirtualTableScenario.DS));
        List<String> offeredLosses = strings(
                scn.GetAwaitingDecision(VirtualTableScenario.DS)
                    .getDecisionParameters(), "cardId");
        assertTrue("The bounded loss choice must remain a legal offered card",
                offeredLosses.contains(forceLoss.actionId()));
        assertTrue("The fixture's unrelated loss must remain available so the"
                        + " rest of the native route can be exercised",
                offeredLosses.contains(
                    Integer.toString(lossFodder.getCardId())));
        scn.DSDecided(Integer.toString(lossFodder.getCardId()));
        scn.PassAllResponses();
        assertTrue(cantina.getZone() == Zone.RESERVE_DECK
                || cantina.getZone() == Zone.TOP_OF_RESERVE_DECK);
        assertEquals(Zone.HAND, deployBody.getZone());
        scn.MoveCardsToBottomOfDSReserveDeck(cantina);

        scn.SkipToDSTurn(Phase.DEPLOY);
        String firstPullAction = scn.GetCardActionId(
                VirtualTableScenario.DS, objective,
                "Deploy Tatooine battleground site from Reserve Deck");
        assertNotNull(firstPullAction);
        DecisionChoice firstPull = evaluateDecisionBoth(
                scn, analyzer, chosenAnalyzer,
                scn.GetAwaitingDecision(VirtualTableScenario.DS));
        assertEquals("Both bots start the native Tatooine site route",
                firstPullAction, firstPull.actionId());
        scn.DSDecided(firstPullAction);
        DecisionChoice firstSiteChoice = evaluateDecisionBoth(
                scn, analyzer, chosenAnalyzer,
                scn.GetAwaitingDecision(VirtualTableScenario.DS));
        PhysicalCardImpl firstSite = firstSiteChoice.blueprintId()
                .equals(cantina.getBlueprintId(true))
                    ? cantina : jawaCamp;
        PhysicalCardImpl finalSite = firstSite == cantina
                ? jawaCamp : cantina;
        assertTrue(firstSiteChoice.reasoning().stream().anyMatch(reason ->
                reason.contains(
                    "Pull a missing location required by the counted objective")));
        scn.DSChooseCard(firstSite);
        scn.PassAllResponses();
        if (scn.DSDecisionAvailable("next to (or convert)")) {
            scn.DSDecided(scn.DSGetCardChoices().getFirst());
            scn.PassAllResponses();
        }
        if (scn.DSDecisionAvailable("On which side")) {
            scn.DSChoose("Right");
            scn.PassAllResponses();
        }

        scn.PrepareDSDestiny(7);
        scn.PrepareLSDestiny(0);
        scn.SkipToPhase(Phase.BATTLE);
        String battleAction = scn.GetCardActionId(
                VirtualTableScenario.DS, mosEisley,
                "Initiate battle");
        assertNotNull(battleAction);
        DecisionChoice battle = evaluateDecisionBoth(
                scn, analyzer, chosenAnalyzer,
                scn.GetAwaitingDecision(VirtualTableScenario.DS));
        assertEquals("Both bots contest the exact still-uncontrolled flip site",
                battleAction, battle.actionId());
        assertTrue(battle.reasoning().stream().anyMatch(reason ->
                reason.contains(
                    "exact unmet pre-flip objective control location")));
        scn.DSInitiateBattle(mosEisley);
        scn.SkipToDamageSegment(false);
        assertTrue(scn.AwaitingLSBattleDamagePayment());
        scn.LSPayBattleDamageFromCardInPlay(battleOpponent);
        if (scn.AwaitingLSBattleDamagePayment()) {
            scn.LSPayRemainingBattleDamageFromReserveDeck();
        }
        assertTrue(scn.GetLSLostPile().contains(battleOpponent));

        scn.SkipToPhase(Phase.MOVE);
        AwaitingDecision moveDecision =
                scn.GetAwaitingDecision(VirtualTableScenario.DS);
        DecisionChoice move = evaluateDecisionBoth(
                scn, analyzer, chosenAnalyzer, moveDecision);
        int selectedMoveIndex = strings(
                moveDecision.getDecisionParameters(), "actionId")
                    .indexOf(move.actionId());
        assertTrue("Both bots move one of the two route actors instead of passing",
                selectedMoveIndex >= 0);
        String movingCardId = strings(
                moveDecision.getDecisionParameters(), "cardId")
                    .get(selectedMoveIndex);
        PhysicalCard selectedMover = scn.gameState().findCardById(
                Integer.parseInt(movingCardId));
        assertTrue(selectedMover == mover || selectedMover == anchor);
        assertTrue(move.reasoning().stream().anyMatch(reason ->
                reason.contains("MOVE.OBJECTIVE.ACTOR_LOCATION_START")));
        scn.DSDecided(move.actionId());
        DecisionChoice moveDestination = evaluateDecisionBoth(
                scn, analyzer, chosenAnalyzer,
                scn.GetAwaitingDecision(VirtualTableScenario.DS),
                null, selectedMover.getCardId());
        if (moveDestination.blueprintId() == null) {
            assertEquals("Destination evaluation may safely cancel the bounded"
                            + " objective move",
                    "", moveDestination.actionId());
            scn.DSDecided(moveDestination.actionId());
            scn.PassAllResponses();
            assertEquals("Canceling the destination must leave the mover at"
                            + " its origin",
                    mosEisley,
                    scn.game().getModifiersQuerying().getLocationThatCardIsAt(
                            scn.gameState(), selectedMover));
            assertFalse("A canceled destination must not manufacture a flip",
                    objective.isFlipped());
            return;
        }
        assertEquals("Both bots move to the pulled empty Tatooine site",
                firstSite.getBlueprintId(true),
                moveDestination.blueprintId());
        scn.DSChooseCard(firstSite);
        scn.PassAllResponses();
        assertEquals(firstSite,
                scn.game().getModifiersQuerying().getLocationThatCardIsAt(
                    scn.gameState(), selectedMover));
        assertFalse(objective.isFlipped());
        scn.MoveCardsToBottomOfDSReserveDeck(finalSite);

        scn.SkipToDSTurn(Phase.DEPLOY);
        String secondPullAction = scn.GetCardActionId(
                VirtualTableScenario.DS, objective,
                "Deploy Tatooine battleground site from Reserve Deck");
        assertNotNull(secondPullAction);
        AwaitingDecision secondPullDecision =
                scn.GetAwaitingDecision(VirtualTableScenario.DS);
        DecisionChoice secondPull = evaluateDecisionBoth(
                scn, analyzer, chosenAnalyzer, secondPullDecision);
        assertEquals("Both bots pull the final site before deploying Tarkin; "
                        + "reserve=" + scn.GetDSReserveDeckCount()
                        + "; actions=" + strings(
                            secondPullDecision.getDecisionParameters(),
                            "actionText")
                        + "; cards=" + strings(
                            secondPullDecision.getDecisionParameters(),
                            "cardId")
                        + "; reasoning=" + secondPull.reasoning(),
                secondPullAction, secondPull.actionId());
        scn.DSDecided(secondPullAction);
        DecisionChoice secondSiteChoice = evaluateDecisionBoth(
                scn, analyzer, chosenAnalyzer,
                scn.GetAwaitingDecision(VirtualTableScenario.DS));
        assertEquals(finalSite.getBlueprintId(true),
                secondSiteChoice.blueprintId());
        scn.DSChooseCard(finalSite);
        scn.PassAllResponses();
        if (scn.DSDecisionAvailable("next to (or convert)")) {
            scn.DSDecided(scn.DSGetCardChoices().getFirst());
            scn.PassAllResponses();
        }
        if (scn.DSDecisionAvailable("On which side")) {
            scn.DSChoose("Left");
            scn.PassAllResponses();
        }
        if (scn.GetAwaitingDecision(VirtualTableScenario.LS) != null) {
            scn.LSPass();
        }

        String deployAction = scn.GetCardActionId(
                VirtualTableScenario.DS, deployBody, "Deploy");
        assertNotNull(deployAction);
        DecisionChoice deploy = evaluateDecisionBoth(
                scn, analyzer, chosenAnalyzer,
                scn.GetAwaitingDecision(VirtualTableScenario.DS));
        assertEquals("Both bots deploy the final counted-site body",
                deployAction, deploy.actionId());
        scn.DSDecided(deployAction);
        DecisionChoice deployDestination = evaluateDecisionBoth(
                scn, analyzer, chosenAnalyzer,
                scn.GetAwaitingDecision(VirtualTableScenario.DS),
                deployBody.getPermanentCardId(), null);
        assertEquals("Both bots choose the remaining empty Tatooine site",
                finalSite.getBlueprintId(true),
                deployDestination.blueprintId());
        assertTrue(deployDestination.reasoning().stream().anyMatch(reason ->
                reason.contains(
                    "Complete the final controlled site for the three-site objective")));
        scn.DSChooseCard(finalSite);
        scn.PassAllResponses();

        assertTrue("The real third-site deployment fires IE's native flip trigger",
                objective.isFlipped());
    }

    @Test
    public void ieCountedGroundRouteUsesBoundedLossAndBudgetPreferences() {
        var scn = ieScenario();
        var mosEisley = scn.GetDSCard("mosEisley");
        var cantina = scn.GetDSCard("cantina");
        var jawaCamp = scn.GetDSCard("jawaCamp");
        var devastator = scn.GetDSCard("devastator");
        var bluffs = scn.GetDSCard("bluffs");
        var jabbasPalace = scn.GetDSCard("jabbasPalace");
        var firstBody = scn.GetDSFiller(4);
        var secondBody = scn.GetDSFiller(5);
        var unrelated = scn.GetDSCard("cpi");

        scn.MoveCardsToDSHand(firstBody, secondBody, unrelated);
        scn.StartGame();
        keepOnlyDarkHandCards(scn, firstBody, secondBody, unrelated);

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        var chosenAnalyzer =
                new com.gempukku.swccgo.ai.models.chosenone.strategy
                    .ObjectiveAnalyzer();
        chosenAnalyzer.analyze(
                scn.game(), VirtualTableScenario.DS, Side.DARK);
        var protectedSite = scn.gameState()
                .getReserveDeck(VirtualTableScenario.DS).stream()
                .filter(card -> analyzer
                    .isPreferredCountedObjectiveLocationForceLossCandidate(
                        scn.game(), VirtualTableScenario.DS, card))
                .findFirst().orElseThrow();
        assertEquals("Exactly two still-needed Tatooine sites survive Force loss",
                2, scn.gameState()
                    .getReserveDeck(VirtualTableScenario.DS).stream()
                    .filter(card -> analyzer
                        .isPreferredCountedObjectiveLocationForceLossCandidate(
                            scn.game(), VirtualTableScenario.DS, card))
                    .count());
        assertFalse("A non-battleground Reserve site is not a native route asset",
                analyzer.isPreferredCountedObjectiveLocationForceLossCandidate(
                    scn.game(), VirtualTableScenario.DS, bluffs));
        assertFalse("An objective-forbidden site is not a native route asset",
                analyzer.isPreferredCountedObjectiveLocationForceLossCandidate(
                    scn.game(), VirtualTableScenario.DS, jabbasPalace));
        var forceLoss = ForceLossPolicy.score(
                "ie-site", ForceLossPolicy.Route.STANDALONE,
                new ForceLossFacts.DecisionFacts(
                        2, 10, 15, 0, 2, false),
                new ForceLossFacts.CandidateFacts(
                        protectedSite.getTitle(), "RESERVE_DECK",
                        ForceLossFacts.ZoneBand.RESERVE,
                        com.gempukku.swccgo.common.CardCategory.LOCATION,
                        false, false, false, false, false),
                new ForceLossPolicy.ObjectiveFlags(
                        false, false, true, false));
        assertTrue(forceLoss.operations().stream().anyMatch(operation ->
                operation.delta() == -300.0f));

        moveSiteToTatooine(scn, cantina);
        moveSiteToTatooine(scn, jawaCamp);
        scn.MoveCardsToLocation(mosEisley, scn.GetDSFiller(1));
        scn.DSActivateForceCheat(12);
        scn.SkipToDSTurn(Phase.DEPLOY);
        assertTrue(analyzer
                .isPreferredCountedObjectivePresenceForceLossCandidate(
                    scn.game(), VirtualTableScenario.DS, firstBody));
        assertTrue(analyzer
                .isPreferredCountedObjectivePresenceForceLossCandidate(
                    scn.game(), VirtualTableScenario.DS, secondBody));
        AwaitingDecision bodyLossDecision = mock(AwaitingDecision.class);
        Map<String, String[]> bodyLossParameters = new HashMap<>();
        bodyLossParameters.put("cardId", new String[] {
                Integer.toString(firstBody.getCardId()),
                Integer.toString(secondBody.getCardId()),
                Integer.toString(unrelated.getCardId())
        });
        bodyLossParameters.put("blueprintId", new String[] {
                firstBody.getBlueprintId(true),
                secondBody.getBlueprintId(true),
                unrelated.getBlueprintId(true)
        });
        bodyLossParameters.put("testingText", new String[] {
                firstBody.getTitle(), secondBody.getTitle(),
                unrelated.getTitle()
        });
        bodyLossParameters.put("selectable",
                new String[] {"true", "true", "true"});
        bodyLossParameters.put("min", new String[] {"1"});
        bodyLossParameters.put("max", new String[] {"1"});
        bodyLossParameters.put("noPass", new String[] {"true"});
        when(bodyLossDecision.getDecisionParameters())
                .thenReturn(bodyLossParameters);
        when(bodyLossDecision.getDecisionType())
                .thenReturn(AwaitingDecisionType.CARD_SELECTION);
        when(bodyLossDecision.getText())
                .thenReturn("Choose Force to lose");
        when(bodyLossDecision.getAwaitingDecisionId()).thenReturn(90210);
        DecisionChoice bodyLossWinner = evaluateDecisionBoth(
                scn, analyzer, chosenAnalyzer, bodyLossDecision);
        assertEquals("Normal loss scoring may override the bounded hold on a"
                        + " still-needed site body",
                Integer.toString(firstBody.getCardId()),
                bodyLossWinner.actionId());
        var bodyHold = ForceLossPolicy.score(
                "ie-body", ForceLossPolicy.Route.STANDALONE,
                new ForceLossFacts.DecisionFacts(
                        3, 10, 15, 0, 3, false),
                ForceLossFacts.readCandidate(
                        scn.gameState(), VirtualTableScenario.DS, firstBody),
                new ForceLossPolicy.ObjectiveFlags(
                        false, false, true, false));
        var bodyObjectiveHold = bodyHold.operations().stream()
                .filter(operation -> operation.ruleArmId().id()
                        .equals("V21-objective"))
                .findFirst().orElseThrow();
        assertEquals(-300.0f, bodyObjectiveHold.delta(), 0.0f);
        assertEquals(TraceDomainId.OBJECTIVE_INTENT,
                bodyObjectiveHold.domainId());
        assertFalse(bodyHold.operations().stream().anyMatch(
                operation -> operation.kind()
                        == PolicyOperationKind.HARD_VETO));

        scn.DSActivateForceCheat(12);
        int unrelatedReserve =
                analyzer.getCountedObjectivePresenceForceReserve(
                    scn.game(), VirtualTableScenario.DS, unrelated);
        int routeReserve = analyzer.getCountedObjectivePresenceForceReserve(
                scn.game(), VirtualTableScenario.DS, firstBody);
        assertTrue("The complete two-body route is funded before an unrelated play",
                unrelatedReserve > routeReserve && routeReserve > 0);
        var starvingDeploy = DeployBudgetPolicy.futureObligations(
                new DeployBudgetPolicy.FutureObligationFacts(
                        "unrelated", unrelatedReserve, 1,
                        0, 0, 0, false, 0,
                        false, false, 0, unrelatedReserve, 0));
        assertTrue(starvingDeploy.result().operations().stream()
                .anyMatch(operation -> operation.delta() == -300.0f
                        && operation.domainId()
                            == TraceDomainId.OBJECTIVE_INTENT
                        && operation.ruleArmId().id().equals(
                            "DEPLOY.BUDGET.OBJECTIVE_REQUIRED_CARD_RESERVE")));

        assertTrue(analyzer.advancesPreFlipPlainPresenceAtRequiredLocation(
                scn.game(), VirtualTableScenario.DS,
                firstBody, cantina));
        assertTrue(analyzer.getDeployObjectiveAdjustments(
                scn.game(), scn.gameState(), VirtualTableScenario.DS,
                firstBody, firstBody.getBlueprint(), "Deploy")
                .stream().anyMatch(note -> note.score == 300.0f));
        assertEquals(300.0f,
                MoveDestinationPolicy.objectiveActorLocationDestination(
                        analyzer
                            .advancesPreFlipPlainPresenceAtRequiredLocation(
                                scn.game(), VirtualTableScenario.DS,
                                firstBody, cantina),
                        firstBody.getTitle(), cantina.getTitle()).delta(),
                0.0f);

        boolean exactTarget = analyzer.isPreFlipFlipRequirementLocation(
                scn.game(), VirtualTableScenario.DS, jawaCamp);
        boolean missingControl = analyzer.isMissingPreFlipRequirementAt(
                scn.game(), VirtualTableScenario.DS, jawaCamp);
        assertTrue(exactTarget);
        assertTrue(missingControl);
        assertEquals(ObjectiveBattlePolicy.REQUIRED_LOCATION_CONTEST_BONUS,
                ObjectiveBattlePolicy.evaluate(
                    new ObjectiveBattlePolicy.Facts(
                        "ie-jawa", exactTarget, missingControl,
                        true, false, true,
                        0.0f, 5, 7.0f, 5.0f))
                    .operations().getFirst().delta(), 0.0f);
        assertTrue(ObjectiveBattlePolicy.evaluate(
                new ObjectiveBattlePolicy.Facts(
                        "ie-jawa-suicide", exactTarget, missingControl,
                        true, false, true,
                        -18.0f, 5, 2.0f, 20.0f))
                .operations().isEmpty());

        scn.MoveCardsToLocation(cantina, firstBody);
        var role = analyzer.classifyGateFormationPieceIfRemoved(
                scn.game(), VirtualTableScenario.DS, firstBody);
        assertEquals(ObjectiveAnalyzer.FlipGateFormationRole
                .LAST_REQUIRED_ACTOR, role);
        assertTrue(MoveObjectiveGateHoldPolicy.evaluateCountedFormation(
                true, role, 5.0f, 0.0f).hardVeto());
        assertEquals(-300.0f,
                BattleForfeitPolicy.scoreFlipGateFormationProtection(
                        "ie-cantina", role, true)
                    .operations().getFirst().delta(), 0.0f);

        assertEquals("Devastator is useful setup, but not a fake flip requirement",
                ObjectiveAnalyzer.FlipGateFormationRole.NONE,
                analyzer.classifyGateFormationPieceIfRemoved(
                    scn.game(), VirtualTableScenario.DS, devastator));
    }
}
