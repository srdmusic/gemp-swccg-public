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
 * Batch Nineteen (2026-07-27): native engine contract for ISB Operations /
 * Empire's Sinister Agents (7_299, DARK). Card Java unchanged. NOT a twin
 * of the Local Uprising pair — batched by set adjacency only; its flip
 * machinery is a two-route anyOf that the twins do not share.
 *
 * Law (Card7_299.java L75-L76): flips when four ISB agents are spottable
 * anywhere on table OR ISB agents control two Rebel Base locations
 * (partOfSystem Yavin 4 or Hoth — a computed membership, battleground not
 * required). Keyword.ISB_AGENT exists only as this objective's own grant
 * to your characters with ISB/Rebel/Rebellion lore (both sides re-grant
 * it). Back (Card7_299_BACK.java L85): flips back when NO ISB agent is
 * spottable — pure absence, no location. Recorded hole, untested here:
 * the objective's own SPY grant makes agents undercover-capable, and
 * undercover agents are invisible to all legs including the back hold.
 * No hard-loss on either side. Fixture agents are Outer Rim Scouts
 * (7_195, non-unique, "employed by the ISB ... Rebel activity" lore);
 * stormtrooper fillers carry no qualifying lore and stay non-agents.
 */
public class IsbOperationsObjectiveEngineContractTest {

    private static final StartingSetup ISB_OPERATIONS = new StartingSetup() {
        @Override
        public HashMap<String, String> Cards() {
            return new HashMap<>() {{
                put("objective", "7_299");
                put("corusDb", "7_276");
            }};
        }

        @Override
        public void Setup(VirtualTableScenario scn) {
            // One required deploy: any Coruscant location, free.
            for (int i = 0; i < 6; i++) {
                if (scn.DSDecisionAvailable("On which side")) {
                    scn.DSChoose("Left");
                } else if (scn.DSDecisionAvailable("to deploy")) {
                    scn.DSChooseCard(scn.GetDSCard("corusDb"));
                }
            }
        }
    };

    private VirtualTableScenario isboScenario() {
        return new VirtualTableScenario(
                new HashMap<>() {{
                    put("blount", "9_21");
                }},
                new HashMap<>() {{
                    put("agent1", "7_195");
                    put("agent2", "7_195");
                    put("agent3", "7_195");
                    put("agent4", "7_195");
                    put("agent5", "7_195");
                    put("nonAgent", "4_105");
                    put("distractor", "1_194");
                    put("corusSquare", "7_278");
                    put("walker", "3_157");
                    put("yavinDb", "1_297");
                    put("yavinJungle", "1_298");
                    put("hothDb", "3_147");
                }},
                24,
                24,
                StartingSetup.DefaultLSGroundLocation,
                ISB_OPERATIONS,
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

    private com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction
            evaluateRandoDeploy(
                    VirtualTableScenario scn,
                    com.gempukku.swccgo.ai.models.rando.strategy
                            .ObjectiveAnalyzer analyzer,
                    String expectedActionId) {
        AwaitingDecision decision = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        var context = new com.gempukku.swccgo.ai.models.rando.evaluators
                .DecisionContext(
                        scn.gameState(), VirtualTableScenario.DS,
                        "CARD_ACTION_CHOICE", decision.getText(),
                        "isbo-deploy-diagnostic", Phase.DEPLOY);
        context.setGame(scn.game());
        context.setSide(Side.DARK);
        context.setObjectiveAnalyzer(analyzer);
        var planner = new com.gempukku.swccgo.ai.models.rando.strategy
                .DeployPhasePlanner();
        planner.setObjectiveAnalyzer(analyzer);
        context.setDeployPhasePlanner(planner);
        var params = decision.getDecisionParameters();
        context.setActionIds(java.util.Arrays.asList(params.get("actionId")));
        context.setActionTexts(java.util.Arrays.asList(params.get("actionText")));
        context.setCardIds(java.util.Arrays.asList(params.get("cardId")));
        context.setBlueprints(java.util.Arrays.asList(params.get("blueprintId")));
        String[] testingTexts = params.get("testingText");
        context.setTestingTexts(testingTexts != null
                ? java.util.Arrays.asList(testingTexts)
                : java.util.Arrays.asList(params.get("actionText")));
        String[] noPass = params.get("noPass");
        if (noPass != null && noPass.length > 0) {
            context.setNoPass(Boolean.parseBoolean(noPass[0]));
        }
        context.setExtra(
                com.gempukku.swccgo.ai.models.common.phase
                    .CaptureDeployBudgetFactsReader.ACTION_PAYMENTS_EXTRA,
                com.gempukku.swccgo.ai.models.common.phase
                    .CaptureDeployBudgetFactsReader
                    .snapshotExactNormalDeployPayments(
                        decision, scn.game(), VirtualTableScenario.DS));
        return new com.gempukku.swccgo.ai.models.rando.evaluators
                .DeployEvaluator().evaluate(context).stream()
                .filter(action -> expectedActionId.equals(
                        action.getActionId()))
                .findFirst().orElseThrow();
    }

    private com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction
            evaluateRandoMove(
                    VirtualTableScenario scn,
                    com.gempukku.swccgo.ai.models.rando.strategy
                            .ObjectiveAnalyzer analyzer,
                    String expectedActionId) {
        var context = randoMoveContext(scn, analyzer);
        return new com.gempukku.swccgo.ai.models.rando.evaluators
                .MoveEvaluator().evaluate(context).stream()
                .filter(action -> expectedActionId.equals(
                        action.getActionId()))
                .findFirst().orElseThrow();
    }

    private com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext
            randoMoveContext(
                    VirtualTableScenario scn,
                    com.gempukku.swccgo.ai.models.rando.strategy
                            .ObjectiveAnalyzer analyzer) {
        AwaitingDecision decision = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        var context = new com.gempukku.swccgo.ai.models.rando.evaluators
                .DecisionContext(
                        scn.gameState(), VirtualTableScenario.DS,
                        decision.getDecisionType().name(),
                        decision.getText(),
                        "isbo-move-diagnostic", Phase.MOVE);
        context.setGame(scn.game());
        context.setSide(Side.DARK);
        context.setObjectiveAnalyzer(analyzer);
        var params = decision.getDecisionParameters();
        context.setActionIds(java.util.Arrays.asList(params.get("actionId")));
        context.setActionTexts(java.util.Arrays.asList(params.get("actionText")));
        context.setCardIds(java.util.Arrays.asList(params.get("cardId")));
        context.setBlueprints(java.util.Arrays.asList(params.get("blueprintId")));
        String[] noPass = params.get("noPass");
        if (noPass != null && noPass.length > 0) {
            context.setNoPass(Boolean.parseBoolean(noPass[0]));
        }
        return context;
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
                    scn.gameState(), VirtualTableScenario.DS,
                    "CARD_SELECTION", prompt,
                    "isbo-loss", Phase.BATTLE);
        randoContext.setGame(scn.game());
        randoContext.setSide(Side.DARK);
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
                    scn.gameState(), VirtualTableScenario.DS,
                    "CARD_SELECTION", prompt,
                    "isbo-loss", Phase.BATTLE);
        chosenContext.setGame(scn.game());
        chosenContext.setSide(Side.DARK);
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
        assertEquals("Rando and Chosen One must choose the same loss",
                rando.getActionId(), chosen.getActionId());
        assertEquals(rando.getScore(), chosen.getScore(), 0.0f);
        return rando.getActionId();
    }

    @Test
    public void isboRouteAFlipsOnFourAgentsAnywhere() {
        var scn = isboScenario();
        var objective = scn.GetDSCard("objective");
        var corusDb = scn.GetDSCard("corusDb");
        var pulseOne = scn.GetDSFiller(1);
        var pulseTwo = scn.GetDSFiller(2);

        scn.MoveCardsToDSHand(pulseOne, pulseTwo);
        scn.StartGame();
        // Three lore-qualified agents: the objective's own keyword grant
        // makes them ISB agents, one short of route A.
        scn.MoveCardsToLocation(corusDb, scn.GetDSCard("agent1"),
                scn.GetDSCard("agent2"), scn.GetDSCard("agent3"));

        scn.DSActivateForceCheat(12);
        scn.SkipToPhase(Phase.DEPLOY);

        scn.DSDeployCardAndPassResponses(
                pulseOne, scn.GetLSStartingLocation());
        assertFalse("Three ISB agents must not flip (stormtrooper pulses do not count)",
                objective.isFlipped());
        scn.LSPass();

        scn.MoveCardsToLocation(corusDb, scn.GetDSCard("agent4"));
        scn.DSDeployCardAndPassResponses(
                pulseTwo, scn.GetLSStartingLocation());
        assertTrue("A fourth ISB agent anywhere must flip via route A",
                objective.isFlipped());
    }

    @Test
    public void isboRouteBFlipsOnTwoRebelBaseLocationsControlledWithAgents() {
        var scn = isboScenario();
        var objective = scn.GetDSCard("objective");
        var corusDb = scn.GetDSCard("corusDb");
        var yavinDb = scn.GetDSCard("yavinDb");
        var yavinJungle = scn.GetDSCard("yavinJungle");
        var agent2 = scn.GetDSCard("agent2");
        var pulseOne = scn.GetDSFiller(1);
        var pulseTwo = scn.GetDSFiller(2);

        scn.MoveCardsToDSHand(pulseOne, pulseTwo);
        scn.StartGame();
        moveSiteToSystem(scn, yavinDb, Title.Yavin_4);
        moveSiteToSystem(scn, yavinJungle, Title.Yavin_4);
        // Two agents only — route A stays far out of reach; one Rebel Base
        // site is controlled, the other still empty.
        scn.MoveCardsToLocation(yavinDb, scn.GetDSCard("agent1"));
        scn.MoveCardsToLocation(corusDb, agent2);

        scn.DSActivateForceCheat(12);
        scn.SkipToPhase(Phase.DEPLOY);

        scn.DSDeployCardAndPassResponses(
                pulseOne, scn.GetLSStartingLocation());
        assertFalse("One agent-controlled Rebel Base location must not flip",
                objective.isFlipped());
        scn.LSPass();

        scn.MoveCardsToLocation(yavinJungle, agent2);
        scn.DSDeployCardAndPassResponses(
                pulseTwo, scn.GetLSStartingLocation());
        assertTrue("Two agent-controlled Rebel Base locations must flip via route B with only two agents on table",
                objective.isFlipped());
    }

    @Test
    public void isboBackFlipsBackWhenNoAgentRemains() {
        var scn = isboScenario();
        var objective = scn.GetDSCard("objective");
        var corusDb = scn.GetDSCard("corusDb");
        var pulseOne = scn.GetDSFiller(1);
        var pulseTwo = scn.GetDSFiller(2);

        scn.MoveCardsToDSHand(pulseOne, pulseTwo);
        scn.StartGame();
        scn.MoveCardsToLocation(corusDb, scn.GetDSCard("agent1"),
                scn.GetDSCard("agent2"), scn.GetDSCard("agent3"));

        scn.DSActivateForceCheat(12);
        scn.SkipToPhase(Phase.DEPLOY);

        scn.MoveCardsToLocation(corusDb, scn.GetDSCard("agent4"));
        scn.DSDeployCardAndPassResponses(
                pulseOne, scn.GetLSStartingLocation());
        assertTrue("Four agents must flip", objective.isFlipped());
        scn.LSPass();

        scn.MoveOutOfPlay(scn.GetDSCard("agent1"));
        scn.MoveOutOfPlay(scn.GetDSCard("agent2"));
        scn.MoveOutOfPlay(scn.GetDSCard("agent3"));
        scn.MoveOutOfPlay(scn.GetDSCard("agent4"));
        scn.DSDeployCardAndPassResponses(
                pulseTwo, scn.GetLSStartingLocation());
        assertFalse("No spottable ISB agent must flip the back to front (non-agent troopers do not hold it)",
                objective.isFlipped());
    }

    @Test
    public void isboProfileRulesTrackTheEngineLaw() {
        var scn = isboScenario();
        var corusDb = scn.GetDSCard("corusDb");

        scn.StartGame();

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertTrue("Profile must hydrate for 7_299", analyzer.isAnalyzed());

        var preFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.DS, "preFlip", "flip");
        assertEquals("ISBO front encodes the route rule and Blount blocker", 2,
                preFlip.size());
        var routeRule = preFlip.stream()
                .filter(state -> "isbo-front-four-agents-or-two-rebel-base"
                        .equals(state.ruleId()))
                .findFirst().orElseThrow();
        var blountRule = preFlip.stream()
                .filter(state -> "isbo-front-lieutenant-blount-absent"
                        .equals(state.ruleId()))
                .findFirst().orElseThrow();
        assertFalse("With no agents the encoded law is unmet",
                routeRule.conditionSatisfied());
        assertTrue("With no Blount at Coruscant the external blocker is absent",
                blountRule.conditionSatisfied());

        scn.MoveCardsToLocation(corusDb, scn.GetDSCard("agent1"),
                scn.GetDSCard("agent2"), scn.GetDSCard("agent3"),
                scn.GetDSCard("agent4"));
        var complete = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.DS, "preFlip", "flip");
        assertTrue("Four agents complete route A of the encoded law",
                complete.stream()
                        .filter(state -> "isbo-front-four-agents-or-two-rebel-base"
                                .equals(state.ruleId()))
                        .findFirst().orElseThrow().conditionSatisfied());

        var postFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.DS, "postFlip", "flipBack");
        assertEquals("The back encodes one pure-absence rule", 1,
                postFlip.size());
        assertFalse("With agents on table the flip-back condition is unmet",
                postFlip.get(0).conditionSatisfied());
    }

    @Test
    public void isboTargetsLieutenantBlountBeforeTheNativeFlipCanFire() {
        var scn = isboScenario();
        var objective = scn.GetDSCard("objective");
        var corusDb = scn.GetDSCard("corusDb");
        var yavinDb = scn.GetDSCard("yavinDb");
        var yavinJungle = scn.GetDSCard("yavinJungle");
        var blount = scn.GetLSCard("blount");
        var finalRouteAgent = scn.GetDSCard("agent5");
        var pulseOne = scn.GetDSFiller(1);

        scn.MoveCardsToDSHand(finalRouteAgent, pulseOne);
        scn.StartGame();
        moveSiteToSystem(scn, yavinDb, Title.Yavin_4);
        moveSiteToSystem(scn, yavinJungle, Title.Yavin_4);
        scn.MoveCardsToLocation(corusDb, blount,
                scn.GetDSCard("agent2"),
                scn.GetDSCard("agent3"),
                scn.GetDSCard("agent4"));
        scn.MoveCardsToLocation(yavinDb, scn.GetDSCard("agent1"));

        var rando = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.strategy
                .ObjectiveAnalyzer();
        rando.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        chosen.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        blount.setUndercover(true);
        assertFalse("Undercover Blount does not occupy Coruscant for his source condition",
                rando.isPreFlipGlobalBlockerAt(
                        scn.game(), VirtualTableScenario.DS, corusDb));
        blount.setUndercover(false);
        assertTrue("Rando must identify Blount at Coruscant as the live blocker",
                rando.isPreFlipGlobalBlockerAt(
                        scn.game(), VirtualTableScenario.DS, corusDb));
        assertTrue("Blount is a legal battle-removable blocker",
                rando.isPreFlipBattleRemovableGlobalBlockerAt(
                        scn.game(), VirtualTableScenario.DS, corusDb));
        assertEquals(
                rando.isPreFlipGlobalBlockerAt(
                        scn.game(), VirtualTableScenario.DS, corusDb),
                chosen.isPreFlipGlobalBlockerAt(
                        scn.game(), VirtualTableScenario.DS, corusDb));
        assertFalse("A route-B deploy is not a true completion while Blount blocks the objective",
                rando.isIsbRebelBaseRouteCompletionDeployCandidate(
                        scn.game(), VirtualTableScenario.DS,
                        finalRouteAgent));
        assertFalse("The child destination must not receive the flip-now score while Blount blocks",
                rando.wouldCompleteIsbPreFlipRequirementAt(
                        scn.game(), VirtualTableScenario.DS,
                        finalRouteAgent, yavinJungle));
        assertEquals(
                rando.isIsbRebelBaseRouteCompletionDeployCandidate(
                        scn.game(), VirtualTableScenario.DS,
                        finalRouteAgent),
                chosen.isIsbRebelBaseRouteCompletionDeployCandidate(
                        scn.game(), VirtualTableScenario.DS,
                        finalRouteAgent));

        scn.DSActivateForceCheat(12);
        scn.SkipToPhase(Phase.DEPLOY);
        scn.DSDeployCardAndPassResponses(
                pulseOne, scn.GetLSStartingLocation());
        assertFalse("Four agents cannot flip while Blount occupies Coruscant",
                objective.isFlipped());
        scn.SkipToDSTurn(Phase.BATTLE);
        scn.PrepareDSDestiny(7);
        scn.PrepareLSDestiny(0);
        String battle = scn.GetCardActionId(
                VirtualTableScenario.DS, corusDb,
                "Initiate battle");
        assertNotNull(battle);
        assertEquals("Both public bots must attack the exact global blocker",
                battle, PublicBots.forGame(scn).decideBoth(scn));
        scn.DSInitiateBattle(corusDb);
        scn.SkipToDamageSegment(true);
        assertTrue(scn.AwaitingLSBattleDamagePayment());
        scn.LSPayBattleDamageFromCardInPlay(blount);
        if (scn.AwaitingLSBattleDamagePayment()) {
            scn.LSPayRemainingBattleDamageFromReserveDeck();
        }
        scn.PassAllResponses();
        assertTrue("Battling Blount away must let the unchanged objective flip",
                objective.isFlipped());
    }

    @Test
    public void isboBackNativelyFlipsFrontWhenBlountArrivesAtCoruscant() {
        var scn = isboScenario();
        var objective = scn.GetDSCard("objective");
        var corusDb = scn.GetDSCard("corusDb");
        var blount = scn.GetLSCard("blount");
        var pulseOne = scn.GetDSFiller(1);
        var pulseTwo = scn.GetDSFiller(2);

        scn.MoveCardsToDSHand(pulseOne, pulseTwo);
        scn.StartGame();
        scn.MoveCardsToLocation(corusDb,
                scn.GetDSCard("agent1"),
                scn.GetDSCard("agent2"),
                scn.GetDSCard("agent3"),
                scn.GetDSCard("agent4"));
        scn.DSActivateForceCheat(12);
        scn.SkipToPhase(Phase.DEPLOY);
        scn.DSDeployCardAndPassResponses(
                pulseOne, scn.GetLSStartingLocation());
        assertTrue("Four agents must first flip ISB normally",
                objective.isFlipped());
        scn.LSPass();

        scn.MoveCardsToLocation(corusDb, blount);
        scn.DSDeployCardAndPassResponses(
                pulseTwo, scn.GetLSStartingLocation());
        assertFalse("Blount arriving at Coruscant must fire his native forced flip-back",
                objective.isFlipped());
    }

    @Test
    public void isboPublicBotsDeployTheFourthAgentBeforeEqualCostNonAgent() {
        var scn = isboScenario();
        var corusDb = scn.GetDSCard("corusDb");
        var agent4 = scn.GetDSCard("agent4");
        var nonAgent = scn.GetDSCard("nonAgent");

        scn.MoveCardsToDSHand(agent4, nonAgent);
        scn.StartGame();
        scn.MoveCardsToLocation(corusDb,
                scn.GetDSCard("agent1"),
                scn.GetDSCard("agent2"),
                scn.GetDSCard("agent3"));
        keepOnlyDarkHandCards(scn, agent4, nonAgent);
        scn.DSActivateForceCheat(2);
        scn.SkipToPhase(Phase.DEPLOY);
        while (scn.GetDSForcePileCount() > 2) {
            scn.MoveCardsToTopOfDSUsedPile(
                    scn.GetTopOfDSForcePile());
        }

        String agentDeploy = scn.GetCardActionId(
                VirtualTableScenario.DS, agent4, "Deploy");
        String nonAgentDeploy = scn.GetCardActionId(
                VirtualTableScenario.DS, nonAgent, "Deploy");
        assertTrue("The fourth agent must have a legal deploy action",
                agentDeploy != null);
        assertTrue("The equal-cost distractor must compete legally",
                nonAgentDeploy != null);
        assertEquals("The objective's fourth agent must beat the distractor and Pass",
                agentDeploy, PublicBots.forGame(scn).decideBoth(scn));
    }

    @Test
    public void isboRouteAFinalAgentMaySpendLegacyBattleReserveAndFlip() {
        var scn = isboScenario();
        var objective = scn.GetDSCard("objective");
        var corusDb = scn.GetDSCard("corusDb");
        var agent4 = scn.GetDSCard("agent4");
        var nonAgent = scn.GetDSCard("nonAgent");
        var rebel = scn.GetLSFiller(1);

        scn.MoveCardsToDSHand(agent4, nonAgent);
        scn.StartGame();
        scn.MoveCardsToLocation(corusDb,
                scn.GetDSCard("agent1"),
                scn.GetDSCard("agent2"),
                scn.GetDSCard("agent3"), rebel);
        keepOnlyDarkHandCards(scn, agent4, nonAgent);
        scn.DSActivateForceCheat(2);
        scn.SkipToDSTurn(Phase.DEPLOY);
        while (scn.GetDSForcePileCount() > 2) {
            scn.MoveCardsToTopOfDSUsedPile(
                    scn.GetTopOfDSForcePile());
        }

        String agentDeploy = scn.GetCardActionId(
                VirtualTableScenario.DS, agent4, "Deploy");
        assertNotNull(agentDeploy);
        var bots = PublicBots.forGame(scn);
        assertEquals("The fourth agent must beat Pass despite the legacy winnable-battle reserve",
                agentDeploy, bots.decideBoth(scn));
        scn.DSDecided(agentDeploy);
        String destination = bots.decideBoth(scn);
        assertFalse("The final agent must receive a legal destination",
                destination.isBlank());
        scn.DSDecided(destination);
        scn.PassAllResponses();
        assertTrue("The Route A deploy must fire the unchanged native flip",
                objective.isFlipped());
    }

    @Test
    public void isboPublicBotsFundAndDeployTheExactTwoAgentRoute() {
        var scn = isboScenario();
        var objective = scn.GetDSCard("objective");
        var corusDb = scn.GetDSCard("corusDb");
        var agent3 = scn.GetDSCard("agent3");
        var agent4 = scn.GetDSCard("agent4");
        var nonAgent = scn.GetDSCard("nonAgent");

        scn.MoveCardsToDSHand(agent3, agent4, nonAgent);
        scn.StartGame();
        scn.MoveCardsToLocation(corusDb,
                scn.GetDSCard("agent1"),
                scn.GetDSCard("agent2"));
        keepOnlyDarkHandCards(scn, agent3, agent4, nonAgent);

        var rando = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.strategy
                .ObjectiveAnalyzer();
        rando.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        chosen.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertEquals("Two remaining two-Force agents require four Force",
                4, rando.getCountedObjectivePresenceForceReserve(
                        scn.game(), VirtualTableScenario.DS, null));
        assertEquals("Selecting one agent leaves exactly two Force reserved",
                2, rando.getCountedObjectivePresenceForceReserve(
                        scn.game(), VirtualTableScenario.DS, agent3));
        assertEquals("An unrelated deploy must not consume route Force",
                4, rando.getCountedObjectivePresenceForceReserve(
                        scn.game(), VirtualTableScenario.DS, nonAgent));
        assertEquals(
                rando.getCountedObjectivePresenceForceReserve(
                        scn.game(), VirtualTableScenario.DS, null),
                chosen.getCountedObjectivePresenceForceReserve(
                        scn.game(), VirtualTableScenario.DS, null));

        scn.DSActivateForceCheat(4);
        scn.SkipToDSTurn(Phase.DEPLOY);
        while (scn.GetDSForcePileCount() > 4) {
            scn.MoveCardsToTopOfDSUsedPile(
                    scn.GetTopOfDSForcePile());
        }
        var bots = PublicBots.forGame(scn);
        var routeCards = java.util.Set.of(agent3, agent4);
        for (int deployment = 0; deployment < 2; deployment++) {
            if (scn.AwaitingLSDeployPhaseActions()) {
                scn.LSPass();
            }
            AwaitingDecision parent = scn.GetAwaitingDecision(
                    VirtualTableScenario.DS);
            assertNotNull("No DS deploy decision; decider="
                    + scn.GetDecidingPlayer() + "; current="
                    + (scn.GetCurrentDecision() != null
                        ? scn.GetCurrentDecision().getText() : "none"),
                    parent);
            String deployAction = bots.decideBoth(scn);
            PhysicalCard selected = AiActionSourceProvenance
                    .selectedActionSource(parent, deployAction);
            assertTrue("The public bot spent route Force on "
                            + (selected != null
                                ? selected.getTitle() : deployAction),
                    routeCards.contains(selected));
            scn.DSDecided(deployAction);
            AwaitingDecision destination = scn.GetAwaitingDecision(
                    VirtualTableScenario.DS);
            assertNotNull(destination);
            String destinationResponse = bots.decideBoth(scn);
            scn.DSDecided(destinationResponse);
            scn.PassAllResponses();
            assertFalse("Selected " + selected.getTitle()
                            + " stayed in hand after response "
                            + destinationResponse + "; params="
                            + destination.getText() + "; "
                            + destination.getDecisionParameters()
                                .entrySet().stream()
                                .map(entry -> entry.getKey() + "="
                                    + java.util.Arrays.toString(
                                        entry.getValue()))
                                .toList(),
                    selected.getZone() == Zone.HAND);
        }
        assertEquals("The exact four-Force route must be paid",
                0, scn.GetDSForcePileCount());
        assertTrue("The unchanged objective must flip after the fourth agent deploys",
                objective.isFlipped());
    }

    @Test
    public void isboProtectsTheCheapestLastAgentFromForceLoss() {
        var scn = isboScenario();
        var corusDb = scn.GetDSCard("corusDb");
        var agent4 = scn.GetDSCard("agent4");
        var nonAgent = scn.GetDSCard("nonAgent");

        scn.MoveCardsToDSHand(agent4, nonAgent);
        scn.StartGame();
        scn.MoveCardsToLocation(corusDb,
                scn.GetDSCard("agent1"),
                scn.GetDSCard("agent2"),
                scn.GetDSCard("agent3"));
        keepOnlyDarkHandCards(scn, agent4, nonAgent);

        var rando = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.strategy
                .ObjectiveAnalyzer();
        rando.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        chosen.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertTrue("The last legal agent must be loss-protected",
                rando.isPreferredCountedObjectivePresenceForceLossCandidate(
                        scn.game(), VirtualTableScenario.DS, agent4));
        assertFalse("An equal-cost non-agent is expendable",
                rando.isPreferredCountedObjectivePresenceForceLossCandidate(
                        scn.game(), VirtualTableScenario.DS, nonAgent));
        assertEquals("Both public bots must spend the non-agent first",
                Integer.toString(nonAgent.getCardId()),
                chooseCardBoth(
                        scn, rando, chosen,
                        "Choose Force to lose",
                        List.of(agent4, nonAgent)));
    }

    @Test
    public void isboRouteBProtectsOnlyTheCheapestExecutableAgentFromForceLoss() {
        var scn = isboScenario();
        var yavinDb = scn.GetDSCard("yavinDb");
        var yavinJungle = scn.GetDSCard("yavinJungle");
        var agent1 = scn.GetDSCard("agent1");
        var agent2 = scn.GetDSCard("agent2");
        var agent3 = scn.GetDSCard("agent3");
        var agent4 = scn.GetDSCard("agent4");

        scn.MoveCardsToDSHand(agent2, agent3, agent4);
        scn.StartGame();
        moveSiteToSystem(scn, yavinDb, Title.Yavin_4);
        moveSiteToSystem(scn, yavinJungle, Title.Yavin_4);
        scn.MoveCardsToLocation(yavinDb, agent1);
        keepOnlyDarkHandCards(scn, agent2, agent3, agent4);

        var rando = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.strategy
                .ObjectiveAnalyzer();
        rando.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        chosen.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);

        List<PhysicalCard> agents = List.of(agent2, agent3, agent4);
        List<PhysicalCard> protectedAgents = agents.stream()
                .filter(card -> rando
                        .isPreferredCountedObjectivePresenceForceLossCandidate(
                            scn.game(), VirtualTableScenario.DS, card))
                .toList();
        assertEquals("Route B needs one more location, not all three Route A bodies",
                1, protectedAgents.size());
        for (PhysicalCard card : agents) {
            assertEquals("Rando and Chosen One loss protection must match",
                    rando.isPreferredCountedObjectivePresenceForceLossCandidate(
                            scn.game(), VirtualTableScenario.DS, card),
                    chosen.isPreferredCountedObjectivePresenceForceLossCandidate(
                            scn.game(), VirtualTableScenario.DS, card));
        }
        PhysicalCard protectedAgent = protectedAgents.getFirst();
        PhysicalCard expendableAgent = agents.stream()
                .filter(card -> card != protectedAgent)
                .findFirst().orElseThrow();
        assertEquals("Both public bots must lose the surplus agent first",
                Integer.toString(expendableAgent.getCardId()),
                chooseCardBoth(
                        scn, rando, chosen,
                        "Choose Force to lose",
                        List.of(protectedAgent, expendableAgent)));
    }

    @Test
    public void isboRouteAForfeitsANonAgentBeforeAnOnTableAgent() {
        var scn = isboScenario();
        var corusDb = scn.GetDSCard("corusDb");
        var agent1 = scn.GetDSCard("agent1");
        var nonAgent = scn.GetDSCard("nonAgent");
        var walker = scn.GetDSCard("walker");

        scn.StartGame();
        scn.MoveCardsToLocation(corusDb, walker,
                scn.GetDSCard("agent2"),
                scn.GetDSCard("agent3"),
                scn.GetDSCard("agent4"), nonAgent);
        scn.BoardAsPassenger(walker, agent1);
        assertTrue(scn.IsAboardAsPassenger(walker, agent1));
        assertEquals("An enclosed passenger is at, but not present at, its site",
                null, scn.game().getModifiersQuerying()
                        .getLocationThatCardIsPresentAt(
                                scn.gameState(), agent1));
        assertEquals(corusDb, scn.game().getModifiersQuerying()
                .getLocationThatCardIsAt(
                        scn.gameState(), agent1));

        var rando = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.strategy
                .ObjectiveAnalyzer();
        rando.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        chosen.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertEquals("An aboard on-table Route A agent is live flip progress",
                ObjectiveAnalyzer.FlipGateFormationRole.LAST_REQUIRED_ACTOR,
                rando.classifyGateFormationPieceIfRemoved(
                        scn.game(), VirtualTableScenario.DS, agent1));
        assertEquals(ObjectiveAnalyzer.FlipGateFormationRole.NONE,
                rando.classifyGateFormationPieceIfRemoved(
                        scn.game(), VirtualTableScenario.DS, nonAgent));
        assertEquals(
                rando.classifyGateFormationPieceIfRemoved(
                        scn.game(), VirtualTableScenario.DS, agent1),
                chosen.classifyGateFormationPieceIfRemoved(
                        scn.game(), VirtualTableScenario.DS, agent1));
        assertEquals("Both bots must forfeit the non-agent before losing Route A progress",
                Integer.toString(nonAgent.getCardId()),
                chooseCardBoth(
                        scn, rando, chosen,
                        "Choose card to forfeit",
                        List.of(agent1, nonAgent)));
    }

    @Test
    public void isboRouteBUsesTheCheapestAnyOfDeployBudgetAndFlips() {
        var scn = isboScenario();
        var objective = scn.GetDSCard("objective");
        var yavinDb = scn.GetDSCard("yavinDb");
        var yavinJungle = scn.GetDSCard("yavinJungle");
        var agent2 = scn.GetDSCard("agent2");
        var agent3 = scn.GetDSCard("agent3");
        var agent4 = scn.GetDSCard("agent4");
        var nonAgent = scn.GetDSCard("nonAgent");

        scn.MoveCardsToDSHand(agent2, agent3, agent4, nonAgent);
        scn.StartGame();
        moveSiteToSystem(scn, yavinDb, Title.Yavin_4);
        moveSiteToSystem(scn, yavinJungle, Title.Yavin_4);
        scn.MoveCardsToLocation(yavinDb, scn.GetDSCard("agent1"));
        keepOnlyDarkHandCards(
                scn, agent2, agent3, agent4, nonAgent);

        var rando = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.strategy
                .ObjectiveAnalyzer();
        rando.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        chosen.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertEquals("Route B needs one two-Force agent, not three for route A",
                2, rando.getCountedObjectivePresenceForceReserve(
                        scn.game(), VirtualTableScenario.DS, null));
        assertEquals("Selecting that agent completes the cheaper branch",
                0, rando.getCountedObjectivePresenceForceReserve(
                        scn.game(), VirtualTableScenario.DS, agent2));
        assertTrue("The hand agent must advance route B at the empty Rebel Base site",
                rando.advancesPreFlipActorAtRuntimeLocation(
                        scn.game(), VirtualTableScenario.DS,
                        agent2, yavinJungle));
        assertFalse("An unrelated site advances only route A, not the cheaper route B",
                rando.advancesPreFlipActorAtRuntimeLocation(
                        scn.game(), VirtualTableScenario.DS,
                        agent2, scn.GetDSCard("corusDb")));
        assertEquals(
                rando.getCountedObjectivePresenceForceReserve(
                        scn.game(), VirtualTableScenario.DS, null),
                chosen.getCountedObjectivePresenceForceReserve(
                        scn.game(), VirtualTableScenario.DS, null));

        scn.DSActivateForceCheat(2);
        scn.SkipToDSTurn(Phase.DEPLOY);
        while (scn.GetDSForcePileCount() > 2) {
            scn.MoveCardsToTopOfDSUsedPile(
                    scn.GetTopOfDSForcePile());
        }
        var bots = PublicBots.forGame(scn);
        AwaitingDecision parent = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        String deploy = bots.decideBoth(scn);
        PhysicalCard selected = AiActionSourceProvenance
                .selectedActionSource(parent, deploy);
        assertTrue("The bots must buy the cheaper route-B agent",
                java.util.Set.of(agent2, agent3, agent4)
                    .contains(selected));
        scn.DSDecided(deploy);
        assertTrue("The selected prospective agent must remain identifiable at the child prompt; zone="
                    + selected.getZone(),
                rando.advancesPreFlipActorAtRuntimeLocation(
                        scn.game(), VirtualTableScenario.DS,
                        selected, yavinJungle));
        assertTrue("That exact child deployment must complete route B now",
                rando.wouldCompletePreFlipRequirementAt(
                        scn.game(), VirtualTableScenario.DS,
                        selected, yavinJungle));
        AwaitingDecision destination = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        String destinationChoice = bots.decideBoth(scn);
        assertEquals("The agent must deploy to the missing Rebel Base site; prompt="
                    + destination.getText() + "; params="
                    + destination.getDecisionParameters(),
                Integer.toString(yavinJungle.getCardId()),
                destinationChoice);
        scn.DSDecided(destinationChoice);
        scn.PassAllResponses();
        assertEquals(0, scn.GetDSForcePileCount());
        assertTrue("The cheaper branch must fire unchanged card Java",
                objective.isFlipped());
    }

    @Test
    public void isboBackPayoffRoutesAnAgentToABattlegroundSite() {
        var scn = isboScenario();
        var objective = scn.GetDSCard("objective");
        var corusDb = scn.GetDSCard("corusDb");
        var yavinJungle = scn.GetDSCard("yavinJungle");
        var agent5 = scn.GetDSCard("agent5");
        var pulse = scn.GetDSFiller(1);

        scn.MoveCardsToDSHand(agent5, pulse);
        scn.StartGame();
        moveSiteToSystem(scn, yavinJungle, Title.Yavin_4);
        scn.MoveCardsToLocation(corusDb,
                scn.GetDSCard("agent1"),
                scn.GetDSCard("agent2"),
                scn.GetDSCard("agent3"),
                scn.GetDSCard("agent4"));
        scn.DSActivateForceCheat(4);
        scn.SkipToPhase(Phase.DEPLOY);
        scn.DSDeployCardAndPassResponses(
                pulse, scn.GetLSStartingLocation());
        assertTrue(objective.isFlipped());

        var rando = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.strategy
                .ObjectiveAnalyzer();
        rando.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        chosen.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertEquals(ObjectiveAnalyzer.ObjectivePostFlipPayoffRole.PRIMARY,
                rando.classifyPostFlipPayoffAt(
                        scn.game(), VirtualTableScenario.DS,
                        agent5, yavinJungle));
        assertEquals(ObjectiveAnalyzer.ObjectivePostFlipPayoffRole.NONE,
                rando.classifyPostFlipPayoffAt(
                        scn.game(), VirtualTableScenario.DS,
                        agent5, corusDb));
        agent5.setUndercover(true);
        assertEquals("Undercover agents do not receive the source-defined payoff",
                ObjectiveAnalyzer.ObjectivePostFlipPayoffRole.NONE,
                rando.classifyPostFlipPayoffAt(
                        scn.game(), VirtualTableScenario.DS,
                        agent5, yavinJungle));
        assertEquals(
                rando.classifyPostFlipPayoffAt(
                        scn.game(), VirtualTableScenario.DS,
                        agent5, yavinJungle),
                chosen.classifyPostFlipPayoffAt(
                        scn.game(), VirtualTableScenario.DS,
                        agent5, yavinJungle));
    }

    @Test
    public void isboRouteBPublicBotsPreserveMoveForceMoveAndFlip() {
        var scn = isboScenario();
        var objective = scn.GetDSCard("objective");
        var yavinDb = scn.GetDSCard("yavinDb");
        var yavinJungle = scn.GetDSCard("yavinJungle");
        var agent1 = scn.GetDSCard("agent1");
        var agent2 = scn.GetDSCard("agent2");
        var distractor = scn.GetDSCard("distractor");

        scn.MoveCardsToDSHand(distractor);
        scn.StartGame();
        moveSiteToSystem(scn, yavinDb, Title.Yavin_4);
        moveSiteToSystem(scn, yavinJungle, Title.Yavin_4);
        scn.MoveCardsToLocation(yavinDb, agent1, agent2);
        keepOnlyDarkHandCards(scn, distractor);

        scn.DSActivateForceCheat(1);
        scn.SkipToDSTurn(Phase.DEPLOY);
        while (scn.GetDSForcePileCount() > 1) {
            scn.MoveCardsToTopOfDSUsedPile(
                    scn.GetTopOfDSForcePile());
        }

        var rando = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.strategy
                .ObjectiveAnalyzer();
        rando.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        chosen.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertTrue("Moving one of two agents must complete the two-location route",
                rando.advancesPreFlipActorAtRuntimeLocation(
                        scn.game(), VirtualTableScenario.DS,
                        agent2, yavinJungle));
        assertEquals(
                rando.advancesPreFlipActorAtRuntimeLocation(
                        scn.game(), VirtualTableScenario.DS,
                        agent2, yavinJungle),
                chosen.advancesPreFlipActorAtRuntimeLocation(
                        scn.game(), VirtualTableScenario.DS,
                        agent2, yavinJungle));
        assertEquals("One Force must be preserved for the exact safe route move",
                1, rando.getIsbRebelBaseMoveForceReserve(
                        scn.game(), VirtualTableScenario.DS,
                        distractor));
        assertEquals(
                rando.getIsbRebelBaseMoveForceReserve(
                        scn.game(), VirtualTableScenario.DS,
                        distractor),
                chosen.getIsbRebelBaseMoveForceReserve(
                        scn.game(), VirtualTableScenario.DS,
                        distractor));

        String distractorDeploy = scn.GetCardActionId(
                VirtualTableScenario.DS, distractor, "Deploy");
        assertNotNull(distractorDeploy);
        var bots = PublicBots.forGame(scn);
        assertEquals("Both public bots must pass instead of spending the move payment",
                "", bots.decideBoth(scn));
        scn.DSPass();
        assertTrue(scn.AwaitingLSDeployPhaseActions());
        scn.LSPass();
        assertTrue(scn.AwaitingDSBattlePhaseActions());
        scn.DSPass();
        assertTrue(scn.AwaitingLSBattlePhaseActions());
        scn.LSPass();
        assertTrue(scn.AwaitingDSMovePhaseActions());
        assertEquals(1, scn.GetDSForcePileCount());
        String move1 = scn.GetCardActionId(
                VirtualTableScenario.DS, agent1,
                "Move using landspeed");
        String move2 = scn.GetCardActionId(
                VirtualTableScenario.DS, agent2,
                "Move using landspeed");
        assertNotNull(move1);
        assertNotNull(move2);
        String selectedMove = bots.decideBoth(scn);
        assertTrue("The public bots must start the source-defined movement route",
                selectedMove.equals(move1) || selectedMove.equals(move2));
        scn.DSDecided(selectedMove);
        assertEquals("The movement child must choose the missing Rebel Base site",
                Integer.toString(yavinJungle.getCardId()),
                bots.decideBoth(scn));
        scn.DSDecided(Integer.toString(yavinJungle.getCardId()));
        scn.PassAllResponses();

        PhysicalCard moved = selectedMove.equals(move1) ? agent1 : agent2;
        assertEquals(yavinJungle,
                scn.game().getModifiersQuerying().getLocationThatCardIsAt(
                        scn.gameState(), moved));
        assertTrue("The unchanged objective must flip after the real move",
                objective.isFlipped());
    }

    @Test
    public void isboPublicBotsReserveForceAndMoveAnAgentToHuntBlount() {
        var scn = isboScenario();
        var corusDb = scn.GetDSCard("corusDb");
        var corusSquare = scn.GetDSCard("corusSquare");
        var blount = scn.GetLSCard("blount");
        var agent1 = scn.GetDSCard("agent1");
        var distractor = scn.GetDSCard("distractor");

        scn.MoveCardsToDSHand(distractor);
        scn.StartGame();
        moveSiteToSystem(scn, corusSquare, Title.Coruscant);
        scn.MoveCardsToLocation(corusSquare, blount);
        scn.MoveCardsToLocation(corusDb, agent1);
        scn.MoveCardsToLocation(scn.GetLSStartingLocation(),
                scn.GetDSCard("agent2"),
                scn.GetDSCard("agent3"),
                scn.GetDSCard("agent4"));
        keepOnlyDarkHandCards(scn, distractor);

        scn.DSActivateForceCheat(1);
        scn.SkipToDSTurn(Phase.DEPLOY);
        while (scn.GetDSForcePileCount() > 1) {
            scn.MoveCardsToTopOfDSUsedPile(
                    scn.GetTopOfDSForcePile());
        }

        var rando = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.strategy
                .ObjectiveAnalyzer();
        rando.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        chosen.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertTrue("Blount must be the live Coruscant flip blocker",
                rando.isPreFlipGlobalBlockerAt(
                        scn.game(), VirtualTableScenario.DS,
                        corusSquare));
        assertTrue("The ISB agent must qualify for the exact Blount chase",
                rando.isIsbBlountBlockerChaseActorAt(
                        scn.game(), VirtualTableScenario.DS,
                        agent1, corusSquare));
        assertEquals(
                rando.isIsbBlountBlockerChaseActorAt(
                        scn.game(), VirtualTableScenario.DS,
                        agent1, corusSquare),
                chosen.isIsbBlountBlockerChaseActorAt(
                        scn.game(), VirtualTableScenario.DS,
                        agent1, corusSquare));
        assertEquals("One Force must be preserved for the legal Blount chase",
                1, rando.getIsbRebelBaseMoveForceReserve(
                        scn.game(), VirtualTableScenario.DS,
                        distractor));
        assertEquals(
                rando.getIsbRebelBaseMoveForceReserve(
                        scn.game(), VirtualTableScenario.DS,
                        distractor),
                chosen.getIsbRebelBaseMoveForceReserve(
                        scn.game(), VirtualTableScenario.DS,
                        distractor));

        var bots = PublicBots.forGame(scn);
        assertEquals("Both public bots must bank the Blount-chase payment",
                "", bots.decideBoth(scn));
        scn.DSPass();
        scn.LSPass();
        scn.DSPass();
        scn.LSPass();
        assertTrue(scn.AwaitingDSMovePhaseActions());

        String move = scn.GetCardActionId(
                VirtualTableScenario.DS, agent1,
                "Move using landspeed");
        assertNotNull(move);
        var evaluatedMove = evaluateRandoMove(scn, rando, move);
        assertFalse("The exact Blount chase must remain admissible: "
                        + evaluatedMove.getReasoningString(),
                evaluatedMove.isHardVetoed());
        assertTrue("The exact Blount chase must receive objective scoring: "
                        + evaluatedMove.getReasoningString(),
                evaluatedMove.getReasoningString()
                        .contains("BLOCKER_CHASE"));
        var combinedWinner = new com.gempukku.swccgo.ai.models.rando
                .evaluators.CombinedEvaluator().evaluateDecision(
                        randoMoveContext(scn, rando));
        assertEquals("The combined lane must select the Blount chase. Winner: "
                        + combinedWinner.getActionId() + " score="
                        + combinedWinner.getScore() + " reasons="
                        + combinedWinner.getReasoningString()
                        + "; move score=" + evaluatedMove.getScore()
                        + " reasons="
                        + evaluatedMove.getReasoningString(),
                move, combinedWinner.getActionId());
        assertEquals("Both public bots must start the Blount chase",
                move, bots.decideBoth(scn));
        scn.DSDecided(move);
        assertEquals("The movement child must select Blount's exact site",
                Integer.toString(corusSquare.getCardId()),
                bots.decideBoth(scn));
        scn.DSDecided(Integer.toString(corusSquare.getCardId()));
        scn.PassAllResponses();
        assertEquals("The unchanged movement engine must move the agent to Blount",
                corusSquare,
                scn.game().getModifiersQuerying()
                        .getLocationThatCardIsAt(
                                scn.gameState(), agent1));
    }

    @Test
    public void isboRouteBDoesNotReserveForRelocatingTheOnlyAgent() {
        var scn = isboScenario();
        var yavinDb = scn.GetDSCard("yavinDb");
        var yavinJungle = scn.GetDSCard("yavinJungle");
        var agent1 = scn.GetDSCard("agent1");
        var distractor = scn.GetDSCard("distractor");

        scn.MoveCardsToDSHand(distractor);
        scn.StartGame();
        moveSiteToSystem(scn, yavinDb, Title.Yavin_4);
        moveSiteToSystem(scn, yavinJungle, Title.Yavin_4);
        scn.MoveCardsToLocation(yavinDb, agent1);
        keepOnlyDarkHandCards(scn, distractor);
        scn.DSActivateForceCheat(2);
        scn.SkipToDSTurn(Phase.DEPLOY);
        while (scn.GetDSForcePileCount() > 2) {
            scn.MoveCardsToTopOfDSUsedPile(
                    scn.GetTopOfDSForcePile());
        }

        var rando = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.strategy
                .ObjectiveAnalyzer();
        rando.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        chosen.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertTrue("The destination is locally relevant despite no net count gain",
                rando.advancesPreFlipActorAtRuntimeLocation(
                        scn.game(), VirtualTableScenario.DS,
                        agent1, yavinJungle));
        assertEquals("Relocating the only agent leaves Route B at one location",
                0, rando.getIsbRebelBaseMoveForceReserve(
                        scn.game(), VirtualTableScenario.DS,
                        distractor));
        assertEquals(
                rando.getIsbRebelBaseMoveForceReserve(
                        scn.game(), VirtualTableScenario.DS,
                        distractor),
                chosen.getIsbRebelBaseMoveForceReserve(
                        scn.game(), VirtualTableScenario.DS,
                        distractor));
    }

    @Test
    public void isboRouteBPublicBotsPreserveBattleForceClearAndFlip() {
        var scn = isboScenario();
        var objective = scn.GetDSCard("objective");
        var yavinDb = scn.GetDSCard("yavinDb");
        var yavinJungle = scn.GetDSCard("yavinJungle");
        var agent1 = scn.GetDSCard("agent1");
        var agent2 = scn.GetDSCard("agent2");
        var agent3 = scn.GetDSCard("agent3");
        var rebel = scn.GetLSFiller(1);
        var distractor = scn.GetDSCard("distractor");

        scn.MoveCardsToDSHand(distractor);
        scn.StartGame();
        moveSiteToSystem(scn, yavinDb, Title.Yavin_4);
        moveSiteToSystem(scn, yavinJungle, Title.Yavin_4);
        scn.MoveCardsToLocation(yavinDb, agent1);
        scn.MoveCardsToLocation(
                yavinJungle, agent2, agent3, rebel);
        keepOnlyDarkHandCards(scn, distractor);
        assertFalse(objective.isFlipped());

        scn.DSActivateForceCheat(1);
        scn.SkipToDSTurn(Phase.DEPLOY);
        while (scn.GetDSForcePileCount() > 1) {
            scn.MoveCardsToTopOfDSUsedPile(
                    scn.GetTopOfDSForcePile());
        }

        var rando = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.strategy
                .ObjectiveAnalyzer();
        rando.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        chosen.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertEquals("One Force must be preserved for the exact winnable route battle",
                1, rando.getIsbRebelBaseBattleForceReserve(
                        scn.game(), VirtualTableScenario.DS));
        assertEquals(
                rando.getIsbRebelBaseBattleForceReserve(
                        scn.game(), VirtualTableScenario.DS),
                chosen.getIsbRebelBaseBattleForceReserve(
                        scn.game(), VirtualTableScenario.DS));

        String distractorDeploy = scn.GetCardActionId(
                VirtualTableScenario.DS, distractor, "Deploy");
        assertNotNull(distractorDeploy);
        var bots = PublicBots.forGame(scn);
        assertEquals("Both public bots must pass instead of spending the battle payment",
                "", bots.decideBoth(scn));
        scn.DSPass();
        assertTrue(scn.AwaitingLSDeployPhaseActions());
        scn.LSPass();
        assertTrue(scn.AwaitingDSBattlePhaseActions());
        assertEquals(1, scn.GetDSForcePileCount());
        scn.PrepareDSDestiny(7);
        scn.PrepareLSDestiny(0);
        String battle = scn.GetCardActionId(
                VirtualTableScenario.DS, yavinJungle,
                "Initiate battle");
        assertNotNull(battle);
        assertEquals("The public bots must clear the exact contested route site",
                battle, bots.decideBoth(scn));

        scn.DSInitiateBattle(yavinJungle);
        scn.SkipToDamageSegment(true);
        assertTrue(scn.AwaitingLSBattleDamagePayment());
        scn.LSPayBattleDamageFromCardInPlay(rebel);
        if (scn.AwaitingLSBattleDamagePayment()) {
            scn.LSPayRemainingBattleDamageFromReserveDeck();
        }
        scn.PassAllResponses();
        assertTrue("Clearing the second location must fire unchanged card Java",
                objective.isFlipped());
    }

    @Test
    public void isboRouteBFinalDeployMaySpendBattleReserveAndFlip() {
        var scn = isboScenario();
        var objective = scn.GetDSCard("objective");
        var yavinDb = scn.GetDSCard("yavinDb");
        var yavinJungle = scn.GetDSCard("yavinJungle");
        var hothDb = scn.GetDSCard("hothDb");
        var agent4 = scn.GetDSCard("agent4");
        var rebel = scn.GetLSFiller(1);

        scn.MoveCardsToDSHand(agent4);
        scn.StartGame();
        moveSiteToSystem(scn, yavinDb, Title.Yavin_4);
        moveSiteToSystem(scn, yavinJungle, Title.Yavin_4);
        moveSiteToSystem(scn, hothDb, Title.Hoth);
        scn.MoveCardsToLocation(yavinDb, scn.GetDSCard("agent1"));
        scn.MoveCardsToLocation(yavinJungle,
                scn.GetDSCard("agent2"),
                scn.GetDSCard("nonAgent"), rebel);
        keepOnlyDarkHandCards(scn, agent4);
        assertFalse(objective.isFlipped());

        scn.DSActivateForceCheat(2);
        scn.SkipToDSTurn(Phase.DEPLOY);
        while (scn.GetDSForcePileCount() > 2) {
            scn.MoveCardsToTopOfDSUsedPile(
                    scn.GetTopOfDSForcePile());
        }
        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertEquals("The contested site exposes a one-Force battle route",
                1, analyzer.getIsbRebelBaseBattleForceReserve(
                        scn.game(), VirtualTableScenario.DS));
        assertTrue("The hand agent can complete Route B at the open Hoth site",
                analyzer.wouldCompletePreFlipRequirementAt(
                        scn.game(), VirtualTableScenario.DS,
                        agent4, hothDb));
        assertEquals("The final deploy replaces the battle route",
                0, analyzer.getIsbRebelBaseBattleForceReserve(
                        scn.game(), VirtualTableScenario.DS, agent4));

        String deploy = scn.GetCardActionId(
                VirtualTableScenario.DS, agent4, "Deploy");
        assertNotNull(deploy);
        var evaluatedDeploy = evaluateRandoDeploy(
                scn, analyzer, deploy);
        assertFalse("The direct final deploy must not be vetoed: reason="
                        + evaluatedDeploy.getVetoReason()
                        + ", reasoning=" + evaluatedDeploy.getReasoning(),
                evaluatedDeploy.isHardVetoed());
        var bots = PublicBots.forGame(scn);
        assertEquals("The direct final deploy must replace, not fund, the battle route; score="
                        + evaluatedDeploy.getScore()
                        + ", reasoning=" + evaluatedDeploy.getReasoning(),
                deploy, bots.decideBoth(scn));
        scn.DSDecided(deploy);
        assertEquals(Integer.toString(hothDb.getCardId()),
                bots.decideBoth(scn));
        scn.DSDecided(Integer.toString(hothDb.getCardId()));
        scn.PassAllResponses();

        assertTrue("The final direct deploy must fire unchanged card Java",
                objective.isFlipped());
        assertEquals(0, scn.GetDSForcePileCount());
    }

    @Test
    public void isboBackPublicBotsUseTheNativeAgentRetrieval() {
        var scn = isboScenario();
        var objective = scn.GetDSCard("objective");
        var corusDb = scn.GetDSCard("corusDb");
        var agent4 = scn.GetDSCard("agent4");
        var pulse = scn.GetDSFiller(1);

        scn.MoveCardsToDSHand(pulse);
        scn.StartGame();
        scn.MoveCardsToLocation(corusDb,
                scn.GetDSCard("agent1"),
                scn.GetDSCard("agent2"),
                scn.GetDSCard("agent3"), agent4);
        scn.DSActivateForceCheat(4);
        scn.SkipToPhase(Phase.DEPLOY);
        scn.DSDeployCardAndPassResponses(
                pulse, scn.GetLSStartingLocation());
        assertTrue(objective.isFlipped());
        scn.MoveCardsToTopOfDSLostPile(agent4);

        scn.SkipToDSTurn(Phase.DRAW);
        String retrieveAction = scn.GetCardActionId(
                VirtualTableScenario.DS, objective,
                "Retrieve an ISB Agent");
        assertNotNull(retrieveAction);
        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.DS, Side.DARK);
        assertTrue(analyzer.isISBBackAgentRetrievalAction(
                scn.game(), VirtualTableScenario.DS,
                objective, "Retrieve an ISB Agent"));
        assertFalse("A different source must retain generic retrieval scoring",
                analyzer.isISBBackAgentRetrievalAction(
                        scn.game(), VirtualTableScenario.DS,
                        corusDb, "Retrieve an ISB Agent"));
        assertFalse("A near-match action must retain generic retrieval scoring",
                analyzer.isISBBackAgentRetrievalAction(
                        scn.game(), VirtualTableScenario.DS,
                        objective, "Retrieve a card"));
        var bots = PublicBots.forGame(scn);
        assertEquals("Both public bots must use the back-side retrieval before Pass",
                retrieveAction, bots.decideBoth(scn));
        scn.DSDecided(retrieveAction);
        scn.PassAllResponses();
        AwaitingDecision target = scn.GetAwaitingDecision(
                VirtualTableScenario.DS);
        assertNotNull("The native retrieval must offer its ISB agent target",
                target);
        scn.DSDecided(bots.decideBoth(scn));
        scn.PassAllResponses();
        assertEquals(Zone.TOP_OF_USED_PILE, agent4.getZone());
        assertFalse("Exact scoring disarms when no eligible agent remains Lost",
                analyzer.isISBBackAgentRetrievalAction(
                        scn.game(), VirtualTableScenario.DS,
                        objective, "Retrieve an ISB Agent"));
        assertTrue(objective.isFlipped());
    }
}
