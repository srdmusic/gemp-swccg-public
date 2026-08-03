package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.ai.models.common.phase.BattleForfeitPolicy;
import com.gempukku.swccgo.ai.models.common.phase.DeployBudgetPolicy;
import com.gempukku.swccgo.ai.models.common.phase.ForceLossFacts;
import com.gempukku.swccgo.ai.models.common.phase.ForceLossPolicy;
import com.gempukku.swccgo.ai.models.common.phase.MoveDestinationPolicy;
import com.gempukku.swccgo.ai.models.common.phase.MoveObjectiveGateHoldPolicy;
import com.gempukku.swccgo.ai.models.common.phase.ObjectiveBattlePolicy;
import com.gempukku.swccgo.ai.models.common.phase.PullSelectionCandidatePolicy;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.framework.StartingSetup;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.PhysicalCardImpl;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Batch Ten (2026-07-27): native engine contract for Massassi Base
 * Operations / One In A Million (111_4, LIGHT). Card Java unchanged.
 *
 * Law (Card111_004.java L134-L147): flips when you control at least three
 * Yavin 4 sites while the opponent controls fewer than three. The back has
 * NO flip-back: once flipped it is permanent (no flip trigger exists in
 * Card111_004_BACK.java), so the profile deliberately omits any postFlip
 * rule.
 */
public class MassassiObjectiveEngineContractTest {

    private static final StartingSetup MASSASSI_BASE_OPERATIONS = new StartingSetup() {
        @Override
        public HashMap<String, String> Cards() {
            return new HashMap<>() {{
                put("objective", "111_4");
                put("system", "1_135");
                put("dockingBay", "1_136");
            }};
        }

        @Override
        public void Setup(VirtualTableScenario scn) {
            // Required start deploys: Yavin 4 system (promptless) and the
            // exact Yavin 4: Docking Bay (single match; answer placement).
            for (int i = 0; i < 6; i++) {
                if (scn.LSDecisionAvailable("to deploy")) {
                    scn.LSChooseCard(scn.GetLSCard("dockingBay"));
                }
                if (scn.LSDecisionAvailable("On which side")) {
                    scn.LSChoose("Left");
                }
            }
        }
    };

    private VirtualTableScenario mboScenario() {
        return new VirtualTableScenario(
                new HashMap<>() {{
                    put("jungle", "1_137");
                    put("warRoom", "1_139");
                    put("briefing", "2_67");
                    put("ruins", "2_68");
                    put("hq", "7_134");
                    put("rebelTech", "2_19");
                    put("trench", "2_62");
                    put("attackRun", "2_42");
                    put("torpedoes", "1_158");
                    put("torpedoes2", "1_158");
                    put("enhancedTorpedoes", "9_88");
                    put("nabooTorpedoes", "14_66");
                    put("deathStarPackage", "7_117");
                    put("xwing", "1_146");
                    put("remoteXwing", "1_146");
                    put("red1", "1_144");
                    put("n1", "14_58");
                    put("tatooine", "1_127");
                    put("routeMover", "1_004");
                    put("routeDeployBody", "1_007");
                }},
                new HashMap<>() {{
                    put("deathStarThreat", "2_143");
                    put("superlaser", "2_161");
                    put("cpi", "2_130");
                    put("centralCore", "1_283");
                    put("detention", "1_284");
                    put("db327", "1_285");
                    put("routeBattleOpponent", "10_040");
                }},
                24,
                24,
                MASSASSI_BASE_OPERATIONS,
                StartingSetup.DefaultDSGroundLocation,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open
        );
    }

    private void moveLocationToYavin(
            VirtualTableScenario scn, PhysicalCardImpl location) {
        scn.RemoveCardZone(location);
        var placements = scn.gameState().getLocationPlacement(
                scn.game(), location, Title.Yavin_4, null);
        assertFalse("Expected a legal placement at Yavin 4",
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

    private record PackageChoice(
            String blueprintId, float score,
            List<String> reasoning) {
    }

    private record DecisionChoice(
            String actionId, float score,
            List<String> reasoning) {
    }

    private record BattleForfeitChoice(
            String actionId,
            List<String> protectedReasoning) {
    }

    private record ForceLossChoice(
            String actionId,
            Map<String, List<String>> protectedReasoning) {
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
            assertNotNull("Light Side must own the public bot decision",
                    decision);
            String randoResponse = rando.decide(
                    VirtualTableScenario.LS,
                    decision, scn.gameState());
            String chosenResponse = chosen.decide(
                    VirtualTableScenario.LS,
                    decision, scn.gameState());
            assertEquals("Public Rando and Chosen One must match",
                    randoResponse, chosenResponse);
            return randoResponse;
        }
    }

    private PackageChoice evaluatePackageChoiceBoth(
            VirtualTableScenario scn,
            com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer
                    randoAnalyzer,
            com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer
                    chosenAnalyzer) {
        return evaluatePackageChoiceBoth(
                scn, randoAnalyzer, chosenAnalyzer,
                scn.GetAwaitingDecision(VirtualTableScenario.LS),
                null, null);
    }

    private PackageChoice evaluatePackageChoiceBoth(
            VirtualTableScenario scn,
            com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer
                    randoAnalyzer,
            com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer
                    chosenAnalyzer,
            AwaitingDecision decision) {
        return evaluatePackageChoiceBoth(
                scn, randoAnalyzer, chosenAnalyzer,
                decision, null, null);
    }

    private PackageChoice evaluatePackageChoiceBoth(
            VirtualTableScenario scn,
            com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer
                    randoAnalyzer,
            com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer
                    chosenAnalyzer,
            AwaitingDecision decision,
            Integer deployingCardId, Integer moverCardId) {
        assertNotNull(decision);
        Map<String, String[]> params = decision.getDecisionParameters();
        List<String> cardIds = strings(params, "cardId");
        List<String> blueprints = strings(params, "blueprintId");
        DecisionChoice choice = evaluateDecisionBoth(
                scn, randoAnalyzer, chosenAnalyzer, decision,
                deployingCardId, moverCardId);
        int index = cardIds.indexOf(choice.actionId());
        assertTrue("Chosen action " + choice.actionId()
                        + " was not a card candidate; cards=" + cardIds
                        + ", reasoning=" + choice.reasoning(),
                index >= 0);
        String blueprintId = index < blueprints.size()
                ? blueprints.get(index)
                : scn.gameState().findCardById(
                    Integer.parseInt(choice.actionId()))
                    .getBlueprintId(true);
        return new PackageChoice(
                blueprintId, choice.score(),
                choice.reasoning());
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
            Integer deployingCardId, Integer moverCardId) {
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
                        scn.gameState(), VirtualTableScenario.LS,
                        decision.getDecisionType().name(),
                        decision.getText(),
                        Integer.toString(decision.getAwaitingDecisionId()),
                        scn.gameState().getCurrentPhase());
        randoContext.setGame(scn.game());
        randoContext.setSide(Side.LIGHT);
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
        if (deployingCardId != null) {
            randoContext.setExtra(
                    ObjectiveAnalyzer.OBJECTIVE_DEPLOYING_CARD_ID_EXTRA,
                    deployingCardId);
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
                        VirtualTableScenario.LS, randoAnalyzer);
            randoContext.setAllowedActionIds(script.allowedActionIds);
            randoContext.setAllowedActionsReason(script.reason);
            randoContext.setStepBuckets(script.stepBuckets);
            randoContext.setStepBucketLabels(script.stepBucketLabels);
        }

        var chosenContext =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators
                    .DecisionContext(
                        scn.gameState(), VirtualTableScenario.LS,
                        decision.getDecisionType().name(),
                        decision.getText(),
                        Integer.toString(decision.getAwaitingDecisionId()),
                        scn.gameState().getCurrentPhase());
        chosenContext.setGame(scn.game());
        chosenContext.setSide(Side.LIGHT);
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
        if (deployingCardId != null) {
            chosenContext.setExtra(
                    ObjectiveAnalyzer.OBJECTIVE_DEPLOYING_CARD_ID_EXTRA,
                    deployingCardId);
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
                        VirtualTableScenario.LS, chosenAnalyzer);
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
        return new DecisionChoice(
                rando.getActionId(), rando.getScore(),
                List.copyOf(rando.getReasoning()));
    }

    private BattleForfeitChoice evaluateBattleForfeitChoiceBoth(
            VirtualTableScenario scn,
            com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer
                    randoAnalyzer,
            com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer
                    chosenAnalyzer,
            PhysicalCard protectedCard,
            PhysicalCard fodder) {
        List<String> cardIds = List.of(
                Integer.toString(protectedCard.getCardId()),
                Integer.toString(fodder.getCardId()));
        List<String> blueprints = List.of(
                protectedCard.getBlueprintId(true),
                fodder.getBlueprintId(true));
        List<String> titles = List.of(
                protectedCard.getTitle(), fodder.getTitle());

        var randoContext =
                new com.gempukku.swccgo.ai.models.rando.evaluators
                    .DecisionContext(
                        scn.gameState(), VirtualTableScenario.LS,
                        "CARD_SELECTION",
                        "Choose a card from battle to forfeit",
                        "massassi-battle-forfeit",
                        Phase.BATTLE);
        randoContext.setGame(scn.game());
        randoContext.setSide(Side.LIGHT);
        randoContext.setObjectiveAnalyzer(randoAnalyzer);
        randoContext.setCardIds(cardIds);
        randoContext.setBlueprints(blueprints);
        randoContext.setTestingTexts(titles);
        randoContext.setSelectable(List.of(true, true));
        randoContext.setNoPass(true);
        randoContext.setMin(1);
        randoContext.setMax(1);

        var chosenContext =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators
                    .DecisionContext(
                        scn.gameState(), VirtualTableScenario.LS,
                        "CARD_SELECTION",
                        "Choose a card from battle to forfeit",
                        "massassi-battle-forfeit",
                        Phase.BATTLE);
        chosenContext.setGame(scn.game());
        chosenContext.setSide(Side.LIGHT);
        chosenContext.setObjectiveAnalyzer(chosenAnalyzer);
        chosenContext.setCardIds(cardIds);
        chosenContext.setBlueprints(blueprints);
        chosenContext.setTestingTexts(titles);
        chosenContext.setSelectable(List.of(true, true));
        chosenContext.setNoPass(true);
        chosenContext.setMin(1);
        chosenContext.setMax(1);

        var randoCandidates =
                new com.gempukku.swccgo.ai.models.rando.evaluators
                    .CardSelectionEvaluator().evaluate(randoContext);
        var chosenCandidates =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators
                    .CardSelectionEvaluator().evaluate(chosenContext);
        String protectedId = cardIds.getFirst();
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

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators
                .CombinedEvaluator().evaluateDecision(randoContext);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators
                .CombinedEvaluator().evaluateDecision(chosenContext);
        assertNotNull(rando);
        assertNotNull(chosen);
        assertEquals(rando.getActionId(), chosen.getActionId());
        assertEquals(rando.getScore(), chosen.getScore(), 0.0f);
        return new BattleForfeitChoice(
                rando.getActionId(),
                List.copyOf(randoProtected.getReasoning()));
    }

    private ForceLossChoice evaluateForceLossChoiceBoth(
            VirtualTableScenario scn,
            com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer
                    randoAnalyzer,
            com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer
                    chosenAnalyzer,
            List<PhysicalCard> protectedCards,
            PhysicalCard fodder) {
        List<PhysicalCard> cards = new ArrayList<>(protectedCards);
        cards.add(fodder);
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
                        scn.gameState(), VirtualTableScenario.LS,
                        "CARD_SELECTION", "Choose Force to lose",
                        "massassi-force-loss", Phase.CONTROL);
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

        var chosenContext =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators
                    .DecisionContext(
                        scn.gameState(), VirtualTableScenario.LS,
                        "CARD_SELECTION", "Choose Force to lose",
                        "massassi-force-loss", Phase.CONTROL);
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

        var randoCandidates =
                new com.gempukku.swccgo.ai.models.rando.evaluators
                    .CardSelectionEvaluator().evaluate(randoContext);
        var chosenCandidates =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators
                    .CardSelectionEvaluator().evaluate(chosenContext);
        Map<String, List<String>> protectedReasoning = new HashMap<>();
        for (PhysicalCard protectedCard : protectedCards) {
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
            protectedReasoning.put(
                    protectedId,
                    List.copyOf(randoProtected.getReasoning()));
        }

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators
                .CombinedEvaluator().evaluateDecision(randoContext);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators
                .CombinedEvaluator().evaluateDecision(chosenContext);
        assertNotNull(rando);
        assertNotNull(chosen);
        assertEquals(rando.getActionId(), chosen.getActionId());
        assertEquals(rando.getScore(), chosen.getScore(), 0.0f);
        return new ForceLossChoice(
                rando.getActionId(), Map.copyOf(protectedReasoning));
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
                ? fallback
                : Boolean.parseBoolean(values.getFirst());
    }

    private long protectedMassassiPackageCards(
            VirtualTableScenario scn,
            com.gempukku.swccgo.ai.models.common.strategy.ObjectiveAnalyzer
                    analyzer) {
        List<PhysicalCard> candidates = new ArrayList<>();
        candidates.addAll(scn.gameState().getHand(VirtualTableScenario.LS));
        candidates.addAll(scn.gameState().getReserveDeck(
                VirtualTableScenario.LS));
        candidates.addAll(scn.gameState().getForcePile(
                VirtualTableScenario.LS));
        candidates.addAll(scn.gameState().getUsedPile(
                VirtualTableScenario.LS));
        return candidates.stream().filter(card -> analyzer
                .isPreferredMassassiAttackRunPackageForceLossCandidate(
                    scn.game(), VirtualTableScenario.LS, card)).count();
    }

    private void finishBlowAwayResolution(VirtualTableScenario scn) {
        for (int attempt = 0; attempt < 30; attempt++) {
            if (scn.AwaitingDSForceLossPayment()) {
                scn.DSPayRemainingForceLossFromReserveDeck();
            } else if (scn.AwaitingLSForceLossPayment()) {
                scn.LSPayRemainingForceLossFromReserveDeck();
            } else if (scn.DSDecisionAvailable(
                    "Choose card to put on Lost Pile")) {
                scn.DSDecided(scn.DSGetCardChoices().getFirst());
            } else if (scn.LSDecisionAvailable(
                    "Choose card to put on Lost Pile")) {
                scn.LSDecided(scn.LSGetCardChoices().getFirst());
            } else if (scn.GetCurrentDecision().getText()
                    .toLowerCase().contains("optional response")) {
                scn.PassAllResponses();
            } else {
                return;
            }
        }
        throw new AssertionError("Yavin blow-away resolution did not finish");
    }

    @Test
    public void mboFrontRequiresThreeControlledYavinSites() {
        var scn = mboScenario();
        var objective = scn.GetLSCard("objective");
        var dockingBay = scn.GetLSCard("dockingBay");
        var jungle = scn.GetLSCard("jungle");
        var warRoom = scn.GetLSCard("warRoom");
        var pulseOne = scn.GetLSFiller(4);
        var thirdBody = scn.GetLSFiller(5);

        scn.MoveCardsToLSHand(pulseOne, thirdBody);
        scn.StartGame();
        moveLocationToYavin(scn, jungle);
        moveLocationToYavin(scn, warRoom);
        scn.MoveCardsToLocation(dockingBay, scn.GetLSFiller(1));
        scn.MoveCardsToLocation(jungle, scn.GetLSFiller(2));

        scn.LSActivateForceCheat(12);
        scn.SkipToLSTurn(Phase.DEPLOY);

        scn.LSDeployCardAndPassResponses(
                pulseOne, scn.GetDSStartingLocation());
        assertFalse("Two controlled Yavin 4 sites must not flip",
                objective.isFlipped());
        scn.DSPass();

        scn.LSDeployCardAndPassResponses(thirdBody, warRoom);
        assertTrue("Three controlled Yavin 4 sites must flip",
                objective.isFlipped());
    }

    @Test
    public void mboFrontIsBlockedWhileOpponentAlsoControlsThreeSites() {
        var scn = mboScenario();
        var objective = scn.GetLSCard("objective");
        var dockingBay = scn.GetLSCard("dockingBay");
        var jungle = scn.GetLSCard("jungle");
        var warRoom = scn.GetLSCard("warRoom");
        var briefing = scn.GetLSCard("briefing");
        var ruins = scn.GetLSCard("ruins");
        var hq = scn.GetLSCard("hq");
        var pulseOne = scn.GetLSFiller(4);
        var pulseTwo = scn.GetLSFiller(5);

        scn.MoveCardsToLSHand(pulseOne, pulseTwo);
        scn.StartGame();
        moveLocationToYavin(scn, jungle);
        moveLocationToYavin(scn, warRoom);
        moveLocationToYavin(scn, briefing);
        moveLocationToYavin(scn, ruins);
        moveLocationToYavin(scn, hq);
        scn.MoveCardsToLocation(dockingBay, scn.GetLSFiller(1));
        scn.MoveCardsToLocation(jungle, scn.GetLSFiller(2));
        scn.MoveCardsToLocation(warRoom, scn.GetLSFiller(3));
        scn.MoveCardsToLocation(briefing, scn.GetDSFiller(1));
        scn.MoveCardsToLocation(ruins, scn.GetDSFiller(2));
        scn.MoveCardsToLocation(hq, scn.GetDSFiller(3));

        scn.LSActivateForceCheat(12);
        scn.SkipToLSTurn(Phase.DEPLOY);

        scn.LSDeployCardAndPassResponses(
                pulseOne, scn.GetDSStartingLocation());
        assertFalse("Opponent control of three Yavin 4 sites must block the flip",
                objective.isFlipped());
        scn.DSPass();

        scn.MoveOutOfPlay(scn.GetDSFiller(3));
        scn.LSDeployCardAndPassResponses(
                pulseTwo, scn.GetDSStartingLocation());
        assertTrue("Opponent dropping to two controlled sites must allow the flip",
                objective.isFlipped());
    }

    @Test
    public void mboBackIsPermanentOnceFlipped() {
        var scn = mboScenario();
        var objective = scn.GetLSCard("objective");
        var dockingBay = scn.GetLSCard("dockingBay");
        var jungle = scn.GetLSCard("jungle");
        var warRoom = scn.GetLSCard("warRoom");
        var briefing = scn.GetLSCard("briefing");
        var ruins = scn.GetLSCard("ruins");
        var hq = scn.GetLSCard("hq");
        var pulseOne = scn.GetLSFiller(4);
        var dsPulse = scn.GetDSFiller(4);

        scn.MoveCardsToLSHand(pulseOne);
        scn.MoveCardsToDSHand(dsPulse);
        scn.StartGame();
        moveLocationToYavin(scn, jungle);
        moveLocationToYavin(scn, warRoom);
        moveLocationToYavin(scn, briefing);
        moveLocationToYavin(scn, ruins);
        moveLocationToYavin(scn, hq);
        scn.MoveCardsToLocation(dockingBay, scn.GetLSFiller(1));
        scn.MoveCardsToLocation(jungle, scn.GetLSFiller(2));
        scn.MoveCardsToLocation(warRoom, scn.GetLSFiller(3));

        scn.LSActivateForceCheat(12);
        scn.SkipToLSTurn(Phase.DEPLOY);
        scn.LSDeployCardAndPassResponses(
                pulseOne, scn.GetDSStartingLocation());
        assertTrue("Three controlled sites must flip the objective",
                objective.isFlipped());
        scn.DSPass();

        // Collapse the owner's board and hand the opponent three sites: the
        // back must hold anyway.
        scn.MoveOutOfPlay(scn.GetLSFiller(2));
        scn.MoveOutOfPlay(scn.GetLSFiller(3));
        scn.DSActivateForceCheat(16);
        scn.SkipToDSTurn(Phase.DEPLOY);
        // Raw placement sidesteps the light sites' deploy restrictions;
        // the separate legal deploy below supplies the table-changed pulse.
        scn.MoveCardsToLocation(briefing, scn.GetDSFiller(1));
        scn.MoveCardsToLocation(ruins, scn.GetDSFiller(2));
        scn.MoveCardsToLocation(hq, scn.GetDSFiller(3));
        scn.DSDeployCardAndPassResponses(dsPulse, scn.GetDSStartingLocation());
        assertTrue("One In A Million has no flip-back: the back must be permanent",
                objective.isFlipped());
    }

    @Test
    public void mboProfileRulesTrackTheEngineLaw() {
        var scn = mboScenario();
        var dockingBay = scn.GetLSCard("dockingBay");
        var jungle = scn.GetLSCard("jungle");
        var warRoom = scn.GetLSCard("warRoom");

        scn.StartGame();
        moveLocationToYavin(scn, jungle);
        moveLocationToYavin(scn, warRoom);
        scn.MoveCardsToLocation(dockingBay, scn.GetLSFiller(1));
        scn.MoveCardsToLocation(jungle, scn.GetLSFiller(2));

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        assertTrue("Profile must hydrate for 111_4", analyzer.isAnalyzed());

        var preFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.LS, "preFlip", "flip");
        assertEquals("MBO front encodes one rule", 1, preFlip.size());
        assertFalse("Two controlled sites leave the rule unmet",
                preFlip.get(0).conditionSatisfied());

        scn.MoveCardsToLocation(warRoom, scn.GetLSFiller(3));
        var complete = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.LS, "preFlip", "flip");
        assertTrue("Three controlled sites complete the encoded law",
                complete.get(0).conditionSatisfied());

        var postFlip = analyzer.assessFlipLocationRules(
                scn.game(), VirtualTableScenario.LS, "postFlip", "flipBack");
        assertTrue("The permanent back must expose no flip-back rule",
                postFlip.isEmpty());
    }

    @Test
    public void mboNativeSitePullIsFreeAndWarRoomLeadsTheRoute() {
        var scn = mboScenario();
        var objective = scn.GetLSCard("objective");
        var warRoom = scn.GetLSCard("warRoom");

        scn.StartGame();
        scn.MoveCardsToBottomOfLSReserveDeck(warRoom);
        scn.SkipToLSTurn(Phase.DEPLOY);
        while (scn.GetLSForcePileCount() > 0) {
            scn.MoveCardsToHand(scn.GetTopOfLSForcePile());
        }

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        assertTrue(analyzer.usesObjectiveLocationPullSequence());
        assertTrue(analyzer.isPreferredMassassiWarRoomPullCandidate(
                scn.game(), VirtualTableScenario.LS, warRoom));
        assertEquals(350.0f,
                PullSelectionCandidatePolicy.scoreMassassiWarRoom(
                        "war-room", true).operations().getFirst().delta(),
                0.0f);
        assertFalse("The Yavin system is not one of the three flip sites",
                analyzer.isObjectiveRelevantLocation("Yavin 4"));
        assertTrue(analyzer.isObjectiveRelevantLocation(
                "Yavin 4: Massassi War Room"));

        assertTrue(scn.LSCardActionAvailable(
                objective, "Deploy Yavin 4 site from Reserve Deck"));
        scn.LSUseCardAction(
                objective, "Deploy Yavin 4 site from Reserve Deck");
        assertTrue(scn.LSHasCardChoiceAvailable(warRoom));
        scn.LSChooseCard(warRoom);
        scn.PassAllResponses();
        if (scn.LSDecisionAvailable("On which side")) {
            scn.LSChoose("Left");
            scn.PassAllResponses();
        }

        assertEquals(0, scn.GetLSForcePileCount());
        assertEquals(Zone.LOCATIONS, warRoom.getZone());
    }

    @Test
    public void mboBothBotsPlayTheContinuousFrontFlipRoute() {
        var scn = mboScenario();
        var objective = scn.GetLSCard("objective");
        var dockingBay = scn.GetLSCard("dockingBay");
        var warRoom = scn.GetLSCard("warRoom");
        var jungle = scn.GetLSCard("jungle");
        var mover = scn.GetLSCard("routeMover");
        var deployBody = scn.GetLSCard("routeDeployBody");
        var lossFodder = scn.GetLSCard("attackRun");
        var anchor = scn.GetLSFiller(1);
        var battleOpponent = scn.GetDSCard("routeBattleOpponent");
        var drainBody = scn.GetDSFiller(2);

        scn.MoveCardsToLSHand(deployBody, lossFodder);
        scn.StartGame();
        scn.MoveCardsToLocation(dockingBay, mover, anchor);
        scn.MoveCardsToLocation(dockingBay, battleOpponent);
        scn.MoveCardsToLocation(
                scn.GetDSStartingLocation(), drainBody);

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        var chosenAnalyzer =
                new com.gempukku.swccgo.ai.models.chosenone.strategy
                    .ObjectiveAnalyzer();
        chosenAnalyzer.analyze(
                scn.game(), VirtualTableScenario.LS, Side.LIGHT);

        scn.DSActivateForceCheat(4);
        scn.SkipToDSTurn(Phase.CONTROL);
        scn.LSActivateForceCheat(8);
        scn.MoveCardsToLSHand(deployBody, lossFodder);
        keepOnlyLightHandCards(scn, deployBody, lossFodder);
        scn.MoveCardsToTopOfLSReserveDeck(warRoom);
        scn.MoveCardsToBottomOfLSReserveDeck(jungle);
        scn.DSForceDrainAt(scn.GetDSStartingLocation());
        scn.PassAllResponses();
        assertTrue(scn.LSDecisionAvailable("Choose Force to lose"));
        DecisionChoice forceLoss = evaluateDecisionBoth(
                scn, analyzer, chosenAnalyzer,
                scn.GetAwaitingDecision(VirtualTableScenario.LS));
        assertFalse("Both bots preserve the next route site during real Force loss",
                Integer.toString(warRoom.getCardId())
                    .equals(forceLoss.actionId()));
        assertFalse("Both bots preserve the future deploy body during real Force loss",
                Integer.toString(deployBody.getCardId())
                    .equals(forceLoss.actionId()));
        scn.LSDecided(forceLoss.actionId());
        scn.PassAllResponses();
        assertTrue(warRoom.getZone() == Zone.RESERVE_DECK
                || warRoom.getZone() == Zone.TOP_OF_RESERVE_DECK);
        assertEquals(Zone.HAND, deployBody.getZone());
        scn.MoveCardsToBottomOfLSReserveDeck(warRoom);

        scn.SkipToLSTurn(Phase.DEPLOY);
        String firstPullAction = scn.GetCardActionId(
                VirtualTableScenario.LS, objective,
                "Deploy Yavin 4 site from Reserve Deck");
        assertNotNull(firstPullAction);
        DecisionChoice firstPull = evaluateDecisionBoth(
                scn, analyzer, chosenAnalyzer,
                scn.GetAwaitingDecision(VirtualTableScenario.LS));
        assertEquals("Both bots start the native location route",
                firstPullAction, firstPull.actionId());
        scn.LSDecided(firstPullAction);
        AwaitingDecision firstSiteDecision =
                scn.GetAwaitingDecision(VirtualTableScenario.LS);
        assertTrue("The preserved War Room remains in the real pull decision",
                strings(firstSiteDecision.getDecisionParameters(),
                        "blueprintId")
                    .contains(warRoom.getBlueprintId(true)));
        PackageChoice firstSite = evaluatePackageChoiceBoth(
                scn, analyzer, chosenAnalyzer,
                firstSiteDecision);
        assertEquals("The War Room leads the real pull sequence",
                warRoom.getBlueprintId(true),
                firstSite.blueprintId());
        assertTrue(firstSite.reasoning().stream().anyMatch(
                reason -> reason.contains("MASSASSI: pull the War Room")));
        scn.LSChooseCard(warRoom);
        scn.PassAllResponses();
        if (scn.LSDecisionAvailable("On which side")) {
            scn.LSChoose("Left");
            scn.PassAllResponses();
        }

        scn.PrepareLSDestiny(7);
        scn.PrepareDSDestiny(0);
        scn.SkipToPhase(Phase.BATTLE);
        String battleAction = scn.GetCardActionId(
                VirtualTableScenario.LS, dockingBay,
                "Initiate battle");
        assertNotNull(battleAction);
        DecisionChoice battle = evaluateDecisionBoth(
                scn, analyzer, chosenAnalyzer,
                scn.GetAwaitingDecision(VirtualTableScenario.LS));
        assertEquals("Both bots contest the exact still-uncontrolled flip site",
                battleAction, battle.actionId());
        assertTrue(battle.reasoning().stream().anyMatch(
                reason -> reason.contains(
                    "exact unmet pre-flip objective control location")));
        scn.LSInitiateBattle(dockingBay);
        scn.SkipToDamageSegment(true);
        assertTrue(scn.AwaitingDSBattleDamagePayment());
        scn.DSPayBattleDamageFromCardInPlay(battleOpponent);
        if (scn.AwaitingDSBattleDamagePayment()) {
            scn.DSPayRemainingBattleDamageFromReserveDeck();
        }
        assertTrue(scn.GetDSLostPile().contains(battleOpponent));

        scn.SkipToPhase(Phase.MOVE);
        AwaitingDecision moveDecision =
                scn.GetAwaitingDecision(VirtualTableScenario.LS);
        DecisionChoice move = evaluateDecisionBoth(
                scn, analyzer, chosenAnalyzer,
                moveDecision);
        List<String> moveActionIds = strings(
                moveDecision.getDecisionParameters(), "actionId");
        int selectedMoveIndex = moveActionIds.indexOf(move.actionId());
        assertTrue("Both bots move one of the two route actors instead of passing",
                selectedMoveIndex >= 0);
        List<String> movingCardIds = strings(
                moveDecision.getDecisionParameters(), "cardId");
        PhysicalCard selectedMover = scn.gameState().findCardById(
                Integer.parseInt(movingCardIds.get(selectedMoveIndex)));
        assertTrue(selectedMover == mover || selectedMover == anchor);
        assertTrue(move.reasoning().stream().anyMatch(reason ->
                reason.contains("MOVE.OBJECTIVE.ACTOR_LOCATION_START")));
        scn.LSDecided(move.actionId());
        PackageChoice moveDestination = evaluatePackageChoiceBoth(
                scn, analyzer, chosenAnalyzer,
                scn.GetAwaitingDecision(VirtualTableScenario.LS),
                null, selectedMover.getCardId());
        assertEquals("Both bots move to the pulled empty War Room",
                warRoom.getBlueprintId(true),
                moveDestination.blueprintId());
        scn.LSChooseCard(warRoom);
        scn.PassAllResponses();
        assertEquals(warRoom,
                scn.game().getModifiersQuerying().getLocationThatCardIsAt(
                    scn.gameState(), selectedMover));
        assertFalse(objective.isFlipped());

        scn.MoveCardsToBottomOfLSReserveDeck(jungle);
        scn.SkipToLSTurn(Phase.DEPLOY);
        PhysicalCardImpl protectedFinalSite = (PhysicalCardImpl) scn.gameState()
                .getReserveDeck(VirtualTableScenario.LS).stream()
                .filter(card -> analyzer.isNativeObjectiveLocationRouteCandidate(
                    scn.game(), VirtualTableScenario.LS, card))
                .findFirst().orElseThrow();
        scn.MoveCardsToBottomOfLSReserveDeck(protectedFinalSite);
        while (scn.GetLSReserveDeckCount() > 2) {
            scn.MoveCardsToTopOfLSUsedPile(
                    scn.GetTopOfLSReserveDeck());
        }
        assertTrue("The final MBO pull is tested at the V60 boundary",
                scn.GetLSReserveDeckCount() <= 2);
        String secondPullAction = scn.GetCardActionId(
                VirtualTableScenario.LS, objective,
                "Deploy Yavin 4 site from Reserve Deck");
        assertNotNull(secondPullAction);
        assertTrue("The exact live objective source receives the narrow V60 bypass",
                analyzer.isMassassiFrontSiteRouteAction(
                    scn.game(), VirtualTableScenario.LS, objective,
                    "Deploy Yavin 4 site from Reserve Deck"));
        assertFalse("A wrong source never receives the narrow V60 bypass",
                analyzer.isMassassiFrontSiteRouteAction(
                    scn.game(), VirtualTableScenario.LS, mover,
                    "Deploy Yavin 4 site from Reserve Deck"));
        assertFalse("A wrong action never receives the narrow V60 bypass",
                analyzer.isMassassiFrontSiteRouteAction(
                    scn.game(), VirtualTableScenario.LS, objective,
                    "Take card into hand from Reserve Deck"));
        try {
            assertFalse("A same-blueprint counterfeit is not the live objective",
                    analyzer.isMassassiFrontSiteRouteAction(
                        scn.game(), VirtualTableScenario.LS,
                        objective.clone(),
                        "Deploy Yavin 4 site from Reserve Deck"));
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
        AwaitingDecision secondPullDecision =
                scn.GetAwaitingDecision(VirtualTableScenario.LS);
        DecisionChoice secondPull = evaluateDecisionBoth(
                scn, analyzer, chosenAnalyzer, secondPullDecision);
        assertEquals("Both bots pull the final site before unrelated deploys; "
                        + "actions=" + strings(
                            secondPullDecision.getDecisionParameters(),
                            "actionText")
                        + "; cards=" + strings(
                            secondPullDecision.getDecisionParameters(),
                            "cardId")
                        + "; reasoning=" + secondPull.reasoning(),
                secondPullAction, secondPull.actionId());
        assertFalse("The exact live route bypasses V60 reserve risk",
                secondPull.reasoning().stream().anyMatch(
                    reason -> reason.contains("V60 RESERVE RISK")));
        scn.LSDecided(secondPullAction);
        PackageChoice secondSite = evaluatePackageChoiceBoth(
                scn, analyzer, chosenAnalyzer,
                scn.GetAwaitingDecision(VirtualTableScenario.LS));
        PhysicalCardImpl finalSite = (PhysicalCardImpl) scn.gameState()
                .getReserveDeck(VirtualTableScenario.LS).stream()
                .filter(card -> secondSite.blueprintId().equals(
                    card.getBlueprintId(true)))
                .findFirst().orElseThrow();
        assertTrue("Both bots choose a real remaining native Yavin site",
                analyzer.isNativeObjectiveLocationRouteCandidate(
                    scn.game(), VirtualTableScenario.LS, finalSite));
        assertTrue(secondSite.reasoning().stream().anyMatch(reason ->
                reason.contains(
                    "Pull a missing location required by the counted objective")));
        scn.LSChooseCard(finalSite);
        scn.PassAllResponses();
        if (scn.LSDecisionAvailable("On which side")) {
            scn.LSChoose("Left");
            scn.PassAllResponses();
        }
        scn.DSPass();

        String deployAction = scn.GetCardActionId(
                VirtualTableScenario.LS, deployBody, "Deploy");
        assertNotNull(deployAction);
        DecisionChoice deploy = evaluateDecisionBoth(
                scn, analyzer, chosenAnalyzer,
                scn.GetAwaitingDecision(VirtualTableScenario.LS));
        assertEquals("Both bots deploy the final counted-site body",
                deployAction, deploy.actionId());
        scn.LSDecided(deployAction);
        PackageChoice deployDestination = evaluatePackageChoiceBoth(
                scn, analyzer, chosenAnalyzer,
                scn.GetAwaitingDecision(VirtualTableScenario.LS),
                deployBody.getPermanentCardId(), null);
        assertEquals("Both bots choose the remaining empty Yavin site",
                finalSite.getBlueprintId(true),
                deployDestination.blueprintId());
        scn.LSChooseCard(finalSite);
        scn.PassAllResponses();

        assertTrue("The real third-site deployment fires MBO's native flip trigger",
                objective.isFlipped());
    }

    @Test
    public void mboCountedFormationBudgetsMovesBattlesAndSurvivesLoss() {
        var scn = mboScenario();
        var dockingBay = scn.GetLSCard("dockingBay");
        var jungle = scn.GetLSCard("jungle");
        var warRoom = scn.GetLSCard("warRoom");
        var firstBody = scn.GetLSFiller(4);
        var secondBody = scn.GetLSFiller(5);
        var unrelated = scn.GetLSCard("xwing");

        scn.MoveCardsToLSHand(firstBody, secondBody, unrelated);
        scn.StartGame();
        keepOnlyLightHandCards(scn, firstBody, secondBody, unrelated);

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        var chosenAnalyzer =
                new com.gempukku.swccgo.ai.models.chosenone.strategy
                    .ObjectiveAnalyzer();
        chosenAnalyzer.analyze(
                scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        var protectedSite = scn.gameState()
                .getReserveDeck(VirtualTableScenario.LS).stream()
                .filter(card -> analyzer
                    .isPreferredCountedObjectiveLocationForceLossCandidate(
                        scn.game(), VirtualTableScenario.LS, card))
                .findFirst().orElseThrow();
        assertEquals("Exactly two still-needed Yavin sites survive Force loss",
                2, scn.gameState()
                    .getReserveDeck(VirtualTableScenario.LS).stream()
                    .filter(card -> analyzer
                        .isPreferredCountedObjectiveLocationForceLossCandidate(
                            scn.game(), VirtualTableScenario.LS, card))
                    .count());
        var forceLoss = ForceLossPolicy.score(
                "mbo-site", ForceLossPolicy.Route.STANDALONE,
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
                operation.delta() == -9999.0f));

        moveLocationToYavin(scn, jungle);
        moveLocationToYavin(scn, warRoom);
        scn.MoveCardsToLocation(dockingBay, scn.GetLSFiller(1));
        scn.LSActivateForceCheat(12);
        scn.SkipToLSTurn(Phase.DEPLOY);

        assertTrue(analyzer
                .isPreferredCountedObjectivePresenceForceLossCandidate(
                    scn.game(), VirtualTableScenario.LS, firstBody));
        assertTrue(analyzer
                .isPreferredCountedObjectivePresenceForceLossCandidate(
                    scn.game(), VirtualTableScenario.LS, secondBody));
        ForceLossChoice bodyLoss = evaluateForceLossChoiceBoth(
                scn, analyzer, chosenAnalyzer,
                List.of(firstBody, secondBody), unrelated);
        assertEquals("Both bots keep both still-needed site bodies",
                Integer.toString(unrelated.getCardId()),
                bodyLoss.actionId());
        assertTrue(bodyLoss.protectedReasoning().values().stream()
                .allMatch(reasons -> reasons.stream().anyMatch(
                    reason -> reason.contains("OBJECTIVE CRITICAL"))));

        int unrelatedReserve =
                analyzer.getCountedObjectivePresenceForceReserve(
                    scn.game(), VirtualTableScenario.LS, unrelated);
        int routeReserve = analyzer.getCountedObjectivePresenceForceReserve(
                scn.game(), VirtualTableScenario.LS, firstBody);
        assertTrue("Both missing site bodies receive an executable Force budget",
                unrelatedReserve > routeReserve && routeReserve > 0);
        var starvingDeploy = DeployBudgetPolicy.futureObligations(
                new DeployBudgetPolicy.FutureObligationFacts(
                        "unrelated", unrelatedReserve, 1,
                        0, 0, 0, false, 0,
                        false, false, 0, unrelatedReserve, 0));
        assertTrue(starvingDeploy.result().operations().stream()
                .anyMatch(operation -> operation.delta() == -500.0f));

        assertTrue(analyzer.advancesPreFlipPlainPresenceAtRequiredLocation(
                scn.game(), VirtualTableScenario.LS,
                firstBody, jungle));
        assertEquals(1000.0f,
                MoveDestinationPolicy.objectiveActorLocationDestination(
                        analyzer
                            .advancesPreFlipPlainPresenceAtRequiredLocation(
                                scn.game(), VirtualTableScenario.LS,
                                firstBody, jungle),
                        firstBody.getTitle(), jungle.getTitle()).delta(),
                0.0f);

        scn.MoveCardsToLocation(jungle, firstBody);
        var role = analyzer.classifyGateFormationPieceIfRemoved(
                scn.game(), VirtualTableScenario.LS, firstBody);
        assertEquals(ObjectiveAnalyzer.FlipGateFormationRole
                .LAST_REQUIRED_ACTOR, role);
        assertTrue(MoveObjectiveGateHoldPolicy.evaluateCountedFormation(
                true, role, 5.0f, 0.0f).hardVeto());
        assertEquals(-9999.0f,
                BattleForfeitPolicy.scoreFlipGateFormationProtection(
                        "mbo-jungle", role, true)
                    .operations().getFirst().delta(), 0.0f);

        var safeBattle = ObjectiveBattlePolicy.evaluate(
                new ObjectiveBattlePolicy.Facts(
                        "mbo-war-room", true, true,
                        true, false, true,
                        0.0f, 5, 7.0f, 5.0f));
        assertEquals(ObjectiveBattlePolicy.REQUIRED_LOCATION_CONTEST_BONUS,
                safeBattle.operations().getFirst().delta(), 0.0f);
        assertTrue(ObjectiveBattlePolicy.evaluate(
                new ObjectiveBattlePolicy.Facts(
                        "mbo-war-room-suicide", true, true,
                        true, false, true,
                        -18.0f, 5, 2.0f, 20.0f))
                .operations().isEmpty());

        long protectedSites = scn.gameState()
                .getReserveDeck(VirtualTableScenario.LS).stream()
                .filter(card -> analyzer
                    .isPreferredCountedObjectiveLocationForceLossCandidate(
                        scn.game(), VirtualTableScenario.LS, card))
                .count();
        assertEquals("All three sites are already physical, so no duplicate is protected",
                0, protectedSites);
    }

    @Test
    public void mboBackUploadsAndRetainsTheAttackRunPackageInOrder() {
        var scn = mboScenario();
        var objective = scn.GetLSCard("objective");
        var yavin = scn.GetLSCard("system");
        var dockingBay = scn.GetLSCard("dockingBay");
        var jungle = scn.GetLSCard("jungle");
        var warRoom = scn.GetLSCard("warRoom");
        var rebelTech = scn.GetLSCard("rebelTech");
        var trench = scn.GetLSCard("trench");
        var deathStar = scn.GetLSCard("deathStarPackage");
        var attackRun = scn.GetLSCard("attackRun");
        var torpedoes = scn.GetLSCard("torpedoes");
        var torpedoes2 = scn.GetLSCard("torpedoes2");
        var xwing = scn.GetLSCard("xwing");
        var remoteXwing = scn.GetLSCard("remoteXwing");
        var red1 = scn.GetLSCard("red1");
        var tatooine = scn.GetLSCard("tatooine");
        var pulse = scn.GetLSFiller(4);
        var lossFodder = scn.GetLSFiller(5);

        scn.MoveCardsToLSHand(pulse, lossFodder);
        scn.StartGame();
        scn.MoveCardsToBottomOfLSReserveDeck(
                rebelTech, trench, deathStar, attackRun, torpedoes);
        moveLocationToYavin(scn, jungle);
        moveLocationToYavin(scn, warRoom);
        scn.MoveCardsToLocation(dockingBay, scn.GetLSFiller(1));
        scn.MoveCardsToLocation(jungle, scn.GetLSFiller(2));
        scn.MoveCardsToLocation(warRoom, scn.GetLSFiller(3));
        scn.LSActivateForceCheat(8);
        scn.SkipToLSTurn(Phase.DEPLOY);
        scn.LSDeployCardAndPassResponses(
                pulse, scn.GetDSStartingLocation());
        assertTrue(objective.isFlipped());
        scn.DSPass();

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        var chosenAnalyzer =
                new com.gempukku.swccgo.ai.models.chosenone.strategy
                    .ObjectiveAnalyzer();
        chosenAnalyzer.analyze(
                scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        assertEquals(4, analyzer.getMassassiAttackRunPackagePullPriority(
                scn.game(), VirtualTableScenario.LS, rebelTech));
        assertEquals("Only the current frontier receives package weight",
                0, analyzer.getMassassiAttackRunPackagePullPriority(
                scn.game(), VirtualTableScenario.LS, deathStar));
        assertEquals(0, analyzer.getMassassiAttackRunPackagePullPriority(
                scn.game(), VirtualTableScenario.LS, attackRun));
        assertEquals(0, analyzer.getMassassiAttackRunPackagePullPriority(
                scn.game(), VirtualTableScenario.LS, torpedoes));
        assertTrue(analyzer.isPreferredMassassiAttackRunPackageForceLossCandidate(
                scn.game(), VirtualTableScenario.LS, rebelTech));
        assertEquals("One executable copy of every unfinished package leg survives",
                6, protectedMassassiPackageCards(scn, analyzer));
        ForceLossChoice multiPointLoss = evaluateForceLossChoiceBoth(
                scn, analyzer, chosenAnalyzer,
                List.of(deathStar, attackRun), lossFodder);
        assertEquals("Both bots lose junk before either distinct package leg",
                Integer.toString(lossFodder.getCardId()),
                multiPointLoss.actionId());
        assertTrue(multiPointLoss.protectedReasoning().values().stream()
                .allMatch(reasons -> reasons.stream().anyMatch(
                    reason -> reason.contains("OBJECTIVE CRITICAL"))));

        assertTrue(scn.LSCardActionAvailable(
                objective, "Take card into hand from Reserve Deck"));
        scn.LSUseCardAction(
                objective, "Take card into hand from Reserve Deck");
        assertTrue(scn.LSHasCardChoiceAvailable(rebelTech));
        AwaitingDecision exactPackageDecision = scn.GetAwaitingDecision(
                VirtualTableScenario.LS);
        PackageChoice choice = evaluatePackageChoiceBoth(
                scn, analyzer, chosenAnalyzer, exactPackageDecision);
        assertEquals("2_19", choice.blueprintId());
        assertTrue(choice.reasoning().stream().anyMatch(reason ->
                reason.contains("MASSASSI:")));
        scn.LSChooseCard(rebelTech);
        scn.PassAllResponses();
        scn.DSPass();

        PackageChoice outsideExactSource = evaluatePackageChoiceBoth(
                scn, analyzer, chosenAnalyzer, exactPackageDecision);
        assertFalse("The package weight must disappear without the live MBO action",
                outsideExactSource.reasoning().stream().anyMatch(
                    reason -> reason.contains("MASSASSI:")));

        assertEquals(Zone.HAND, rebelTech.getZone());
        assertEquals(0, analyzer.getMassassiAttackRunPackagePullPriority(
                scn.game(), VirtualTableScenario.LS, rebelTech));
        assertEquals(3, analyzer.getMassassiAttackRunPackagePullPriority(
                scn.game(), VirtualTableScenario.LS, deathStar));
        assertTrue(analyzer.isPreferredMassassiAttackRunPackageForceLossCandidate(
                scn.game(), VirtualTableScenario.LS, rebelTech));
        assertEquals(6, protectedMassassiPackageCards(scn, analyzer));
        assertTrue(analyzer.advancesMassassiRebelTechPackageAt(
                scn.game(), VirtualTableScenario.LS,
                rebelTech, warRoom));
        assertTrue(analyzer.getMassassiAttackRunPackageForceReserve(
                scn.game(), VirtualTableScenario.LS, pulse) > 0);
        assertFalse("The objective upload is once per deploy phase",
                scn.LSCardActionAvailable(
                    objective, "Take card into hand from Reserve Deck"));

        scn.LSDeployCardAndPassResponses(rebelTech, warRoom);
        assertEquals(Zone.AT_LOCATION, rebelTech.getZone());
        assertEquals(
                ObjectiveAnalyzer.FlipGateFormationRole
                    .MASSASSI_ATTACK_RUN_ENABLER,
                analyzer.classifyGateFormationPieceIfRemoved(
                    scn.game(), VirtualTableScenario.LS,
                    rebelTech));
        BattleForfeitChoice techForfeit =
                evaluateBattleForfeitChoiceBoth(
                    scn, analyzer, chosenAnalyzer,
                    rebelTech, pulse);
        assertEquals("Both bots forfeit fodder before the sole Trench tutor",
                Integer.toString(pulse.getCardId()),
                techForfeit.actionId());
        assertTrue(techForfeit.protectedReasoning().stream()
                .anyMatch(reason -> reason.contains(
                    "MASSASSI_ATTACK_RUN_HOLD")));
        scn.SkipToLSTurn(Phase.CONTROL);
        scn.MoveCardsToBottomOfLSReserveDeck(trench);
        assertEquals("The Tech tutor requires Trench to remain in Reserve Deck",
                Zone.RESERVE_DECK, trench.getZone());
        assertTrue(scn.LSCardActionAvailable(
                rebelTech, "Take card into hand from Reserve Deck"));
        scn.LSUseCardAction(
                rebelTech, "Take card into hand from Reserve Deck");
        scn.PassAllResponses();
        assertTrue(scn.LSHasCardChoiceAvailable(trench));
        scn.LSChooseCard(trench);
        scn.PassAllResponses();

        assertEquals(Zone.TOP_OF_USED_PILE, rebelTech.getZone());
        assertEquals(Zone.HAND, trench.getZone());
        scn.MoveCardsToBottomOfLSReserveDeck(
                deathStar, attackRun, torpedoes);
        analyzer.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        assertEquals("Trench closes the Tech stage even after Tech enters Used",
                0, analyzer.getMassassiAttackRunPackagePullPriority(
                    scn.game(), VirtualTableScenario.LS, rebelTech));
        assertEquals(3, analyzer.getMassassiAttackRunPackagePullPriority(
                scn.game(), VirtualTableScenario.LS, deathStar));
        assertFalse(analyzer.isPreferredMassassiAttackRunPackageForceLossCandidate(
                scn.game(), VirtualTableScenario.LS, rebelTech));
        assertTrue(analyzer.isPreferredMassassiAttackRunPackageForceLossCandidate(
                scn.game(), VirtualTableScenario.LS, deathStar));
        assertTrue("The fetched Trench remains one protected package leg",
                analyzer.isPreferredMassassiAttackRunPackageForceLossCandidate(
                    scn.game(), VirtualTableScenario.LS, trench));
        assertEquals(5, protectedMassassiPackageCards(scn, analyzer));

        scn.MoveCardsToLSHand(
                deathStar, attackRun, torpedoes, xwing, red1);
        scn.MoveLocationToTable(tatooine);
        scn.SkipToPhase(Phase.DEPLOY);
        scn.LSDeployLocation(deathStar);
        scn.PassAllResponses();
        scn.DSPass();
        assertTrue(analyzer.isMassassiTrenchDeployCandidate(
                scn.game(), VirtualTableScenario.LS, trench));
        scn.LSDeployLocation(trench);
        scn.PassAllResponses();
        scn.DSPass();

        assertTrue("Trench on table opens the Attack Run deployment",
                analyzer.isMassassiAttackRunPackageDeployCandidate(
                    scn.game(), VirtualTableScenario.LS, attackRun));
        scn.LSDeployCardAndPassResponses(attackRun, trench);
        scn.DSPass();

        assertEquals(Zone.HAND, xwing.getZone());
        assertEquals(Zone.ATTACHED, attackRun.getZone());
        assertTrue(scn.LSDeployAvailable(xwing));
        assertTrue("The missing compatible carrier becomes the next deploy",
                analyzer.isMassassiAttackRunCarrierDeployCandidate(
                    scn.game(), VirtualTableScenario.LS, xwing));
        assertFalse("An unpiloted X-wing cannot become a dead-end lead carrier",
                analyzer.isMassassiAttackRunCarrierDeployCandidate(
                    scn.game(), VirtualTableScenario.LS, red1));
        scn.MoveCardsToLocation(tatooine, remoteXwing);
        assertTrue("A piloted carrier stranded off-route cannot block a deployable one",
                analyzer.isMassassiAttackRunCarrierDeployCandidate(
                    scn.game(), VirtualTableScenario.LS, xwing));
        assertTrue("The carrier's exact deploy cost remains reserved",
                analyzer.getMassassiAttackRunPackageForceReserve(
                    scn.game(), VirtualTableScenario.LS, pulse) >= 2);
        assertTrue("The unfetchable carrier survives with its matching torpedoes",
                analyzer.isPreferredMassassiAttackRunPackageForceLossCandidate(
                    scn.game(), VirtualTableScenario.LS, xwing));
        assertTrue(analyzer.isPreferredMassassiAttackRunPackageForceLossCandidate(
                scn.game(), VirtualTableScenario.LS, torpedoes));
        assertEquals(2, protectedMassassiPackageCards(scn, analyzer));

        scn.LSDeployCard(xwing);
        assertTrue(scn.LSHasCardChoiceAvailable(yavin));
        assertTrue("Tatooine is a legal but off-route deploy destination",
                scn.LSHasCardChoiceAvailable(tatooine));
        PackageChoice carrierDestination = evaluatePackageChoiceBoth(
                scn, analyzer, chosenAnalyzer,
                scn.GetAwaitingDecision(VirtualTableScenario.LS),
                xwing.getPermanentCardId(), null);
        assertEquals("Both bots deploy the Attack Run carrier within one move of Death Star",
                yavin.getBlueprintId(true),
                carrierDestination.blueprintId());
        assertTrue(carrierDestination.reasoning().stream().anyMatch(
                reason -> reason.contains("one move of Death Star")));

        scn.LSChooseCard(yavin);
        scn.PassAllResponses();
        scn.MoveCardsToBottomOfLSReserveDeck(torpedoes2);
        scn.DSPass();
        assertTrue(scn.LSCardActionAvailable(
                objective, "Take card into hand from Reserve Deck"));
        scn.LSUseCardAction(
                objective, "Take card into hand from Reserve Deck");
        assertTrue(scn.LSHasCardChoiceAvailable(torpedoes2));
        scn.LSChooseCard(torpedoes2);
        scn.PassAllResponses();
        keepOnlyLightHandCards(scn, torpedoes);
        while (scn.GetLSForcePileCount() > 1) {
            scn.MoveCardsToTopOfLSUsedPile(
                    scn.GetTopOfLSForcePile());
        }
        scn.DSPass();
        assertTrue("Only a torpedo with its exact carrier in play is executable",
                analyzer.isMassassiAttackRunPackageDeployCandidate(
                    scn.game(), VirtualTableScenario.LS, torpedoes));
        assertEquals("The exact torpedo deploy cost remains reserved",
                1, analyzer.getMassassiAttackRunPackageForceReserve(
                    scn.game(), VirtualTableScenario.LS, pulse));
        assertTrue("The Yavin fighter is the exact route-ready payment target",
                analyzer.isMassassiAttackRunTorpedoesAttachmentTarget(
                    scn.game(), VirtualTableScenario.LS,
                    torpedoes, xwing));
        assertFalse("A compatible but stranded fighter cannot source the budget",
                analyzer.isMassassiAttackRunTorpedoesAttachmentTarget(
                    scn.game(), VirtualTableScenario.LS,
                    torpedoes, remoteXwing));
        assertEquals("The target-aware payment reads the card's real modifier cost",
                1, analyzer.getMassassiAttackRunPackageDeployForcePayment(
                    scn.game(), VirtualTableScenario.LS, torpedoes));
        assertEquals("One Force must survive for the Yavin-to-Death-Star move",
                1, analyzer.getMassassiAttackRunCarrierMoveForceReserve(
                    scn.game(), VirtualTableScenario.LS));

        String torpedoesDeployAction = scn.GetCardActionId(
                VirtualTableScenario.LS, torpedoes, "Deploy");
        assertNotNull(torpedoesDeployAction);
        DecisionChoice oneForceChoice = evaluateDecisionBoth(
                scn, analyzer, chosenAnalyzer,
                scn.GetAwaitingDecision(VirtualTableScenario.LS));
        assertEquals("With only the weapon payment available, both bots pass",
                "", oneForceChoice.actionId());

        scn.LSActivateForceCheat(1);
        assertEquals(2, scn.GetLSForcePileCount());
        DecisionChoice twoForceChoice = evaluateDecisionBoth(
                scn, analyzer, chosenAnalyzer,
                scn.GetAwaitingDecision(VirtualTableScenario.LS));
        assertEquals("Two Force funds both torpedoes and the route move; "
                        + twoForceChoice.reasoning(),
                torpedoesDeployAction, twoForceChoice.actionId());
        PublicBots publicBots = PublicBots.forGame(scn);
        assertEquals("Both public bots preserve deploy-parent provenance",
                torpedoesDeployAction, publicBots.decideBoth(scn));

        scn.LSDecided(torpedoesDeployAction);
        assertTrue(scn.LSHasCardChoiceAvailable(xwing));
        assertTrue("The stranded matching X-wing remains a legal decoy target",
                scn.LSHasCardChoiceAvailable(remoteXwing));
        DecisionChoice torpedoesTarget = evaluateDecisionBoth(
                scn, analyzer, chosenAnalyzer,
                scn.GetAwaitingDecision(VirtualTableScenario.LS),
                torpedoes.getPermanentCardId(), null);
        assertEquals("Both bots arm the piloted fighter on the Death Star route",
                Integer.toString(xwing.getCardId()),
                torpedoesTarget.actionId());
        assertTrue(torpedoesTarget.reasoning().stream().anyMatch(
                reason -> reason.contains(
                    "Attach Proton Torpedoes")));
        assertEquals("Both public bots choose the exact torpedo target child",
                Integer.toString(xwing.getCardId()),
                publicBots.decideBoth(scn));
        scn.LSChooseCard(xwing);
        scn.PassAllResponses();
        assertTrue(scn.IsAttachedTo(xwing, torpedoes));
        assertEquals("Torpedoes spend one Force and preserve the move payment",
                1, scn.GetLSForcePileCount());
        scn.SkipToPhase(Phase.MOVE);

        String moveAction = scn.GetCardActionId(
                VirtualTableScenario.LS, xwing, "Move");
        assertNotNull(moveAction);
        DecisionChoice moveParent = evaluateDecisionBoth(
                scn, analyzer, chosenAnalyzer,
                scn.GetAwaitingDecision(VirtualTableScenario.LS));
        assertEquals("Both bots start the exact Attack Run carrier move",
                moveAction, moveParent.actionId());
        assertTrue(moveParent.reasoning().stream().anyMatch(reason ->
                reason.contains("MOVE.OBJECTIVE.POST_FLIP_PAYOFF_START")));
        assertEquals("Both public bots preserve move-parent provenance",
                moveAction, publicBots.decideBoth(scn));

        scn.LSDecided(moveAction);
        assertTrue("Tatooine remains a real off-route hyperspeed option",
                scn.LSHasCardChoiceAvailable(tatooine));
        assertTrue(scn.LSHasCardChoiceAvailable(deathStar));
        PackageChoice moveDestination = evaluatePackageChoiceBoth(
                scn, analyzer, chosenAnalyzer,
                scn.GetAwaitingDecision(VirtualTableScenario.LS),
                null, xwing.getCardId());
        assertEquals("Both bots move the armed carrier to Death Star",
                deathStar.getBlueprintId(true),
                moveDestination.blueprintId());
        assertTrue(moveDestination.reasoning().stream().anyMatch(reason ->
                reason.contains("MOVE.OBJECTIVE.POST_FLIP_PRIMARY_PAYOFF")));
        assertEquals("Both public bots choose Death Star as the move child",
                Integer.toString(deathStar.getCardId()),
                publicBots.decideBoth(scn));
        scn.LSChooseCard(deathStar);
        scn.PassAllResponses();
        scn.DSPass();

        assertEquals("The reserved Force pays the real hyperspeed move",
                0, scn.GetLSForcePileCount());
        assertEquals(deathStar,
                scn.game().getModifiersQuerying().getLocationThatCardIsAt(
                    scn.gameState(), xwing));
        assertEquals(ObjectiveAnalyzer.ObjectivePostFlipPayoffRole.PRIMARY,
                analyzer.classifyPostFlipPayoffRoleAt(
                    scn.game(), VirtualTableScenario.LS,
                    xwing, deathStar));
        assertEquals(
                ObjectiveAnalyzer.FlipGateFormationRole
                    .MASSASSI_ATTACK_RUN_ENABLER,
                analyzer.classifyGateFormationPieceIfRemoved(
                    scn.game(), VirtualTableScenario.LS,
                    xwing));
        BattleForfeitChoice carrierForfeit =
                evaluateBattleForfeitChoiceBoth(
                    scn, analyzer, chosenAnalyzer,
                    xwing, pulse);
        assertEquals("Both bots forfeit fodder before the armed lead carrier",
                Integer.toString(pulse.getCardId()),
                carrierForfeit.actionId());
        assertTrue(carrierForfeit.protectedReasoning().stream()
                .anyMatch(reason -> reason.contains(
                    "MASSASSI_ATTACK_RUN_HOLD")));

        // A starfighter that used hyperspeed cannot also enter the Trench in
        // that move phase. The route holds it at Death Star and fires on the
        // next Light move phase.
        scn.SkipToLSTurn(Phase.MOVE);
        String attackRunAction = scn.GetCardActionId(
                VirtualTableScenario.LS, attackRun,
                "Attempt to 'blow away' Death Star");
        assertNotNull(attackRunAction);
        assertTrue("The exact source and ready carrier must match the action policy",
                analyzer.isMassassiAttackRunAction(
                    scn.game(), VirtualTableScenario.LS,
                    attackRun,
                    "Attempt to 'blow away' Death Star"));
        DecisionChoice attackRunWinner = evaluateDecisionBoth(
                scn, analyzer, chosenAnalyzer,
                scn.GetAwaitingDecision(VirtualTableScenario.LS));
        assertEquals("Both bots begin the real Attack Run instead of passing; "
                        + attackRunWinner.reasoning(),
                attackRunAction, attackRunWinner.actionId());
        assertTrue(attackRunWinner.reasoning().stream()
                .anyMatch(reason -> reason.contains(
                    "MASSASSI R3 PAYOFF")));
        assertTrue("The completed package must expose the real Attack Run action",
                scn.LSCardActionAvailable(
                    attackRun, "Attempt to 'blow away' Death Star"));

        assertFalse(deathStar.isBlownAway());
        scn.PrepareLSDestiny(6);
        scn.PrepareLSDestiny(7);
        scn.LSDecided(attackRunAction);
        scn.PassResponses("MOVING_AT_START_OF_ATTACK_RUN");
        scn.PassResponses("MOVED_AT_START_OF_ATTACK_RUN");
        assertEquals("The real epic event moves the armed lead into the Trench",
                trench,
                scn.game().getModifiersQuerying().getLocationThatCardIsAt(
                    scn.gameState(), xwing));
        assertTrue(scn.LSDecisionAvailable(
                "Choose provide cover action to play or Pass"));
        scn.PassResponses("Choose provide cover action to play or Pass");
        scn.PassAllResponses();
        assertTrue("This Special Edition Death Star fixture creates eight Force loss; "
                        + "current=" + scn.GetCurrentDecision().getText()
                        + "; blownAway=" + deathStar.isBlownAway()
                        + "; carrierZone=" + xwing.getZone()
                        + "; carrierAt="
                        + scn.game().getModifiersQuerying()
                            .getLocationThatCardIsAt(
                                scn.gameState(), xwing),
                scn.DSDecisionAvailable("Choose Force to lose"));
        assertEquals("Pull Up returns the lead before blow-away resolution",
                deathStar,
                scn.game().getModifiersQuerying().getLocationThatCardIsAt(
                    scn.gameState(), xwing));
        finishBlowAwayResolution(scn);
        assertTrue("The native epic-event resolution blows away Death Star",
                deathStar.isBlownAway());
    }

    @Test
    public void mboPackageSkipsDuplicateDeathStarAndIncompatibleTorpedoes() {
        var scn = mboScenario();
        var objective = scn.GetLSCard("objective");
        var dockingBay = scn.GetLSCard("dockingBay");
        var jungle = scn.GetLSCard("jungle");
        var warRoom = scn.GetLSCard("warRoom");
        var trench = scn.GetLSCard("trench");
        var deathStar = scn.GetLSCard("deathStarPackage");
        var attackRun = scn.GetLSCard("attackRun");
        var torpedoes = scn.GetLSCard("torpedoes");
        var enhancedTorpedoes = scn.GetLSCard("enhancedTorpedoes");
        var nabooTorpedoes = scn.GetLSCard("nabooTorpedoes");
        var n1 = scn.GetLSCard("n1");
        var opponentDeathStar = scn.GetDSCard("deathStarThreat");
        var pulse = scn.GetLSFiller(4);

        scn.MoveCardsToLSHand(pulse, trench, attackRun);
        scn.StartGame();
        scn.MoveCardsToBottomOfLSReserveDeck(
                deathStar, torpedoes, enhancedTorpedoes, nabooTorpedoes);
        moveLocationToYavin(scn, jungle);
        moveLocationToYavin(scn, warRoom);
        scn.MoveCardsToLocation(dockingBay, scn.GetLSFiller(1));
        scn.MoveCardsToLocation(jungle, scn.GetLSFiller(2));
        scn.MoveCardsToLocation(warRoom, scn.GetLSFiller(3));
        scn.LSActivateForceCheat(8);
        scn.SkipToLSTurn(Phase.DEPLOY);
        scn.LSDeployCardAndPassResponses(
                pulse, scn.GetDSStartingLocation());
        assertTrue(objective.isFlipped());
        scn.DSPass();
        scn.MoveOutOfPlay(n1);
        scn.MoveLocationToTable(opponentDeathStar);

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        assertEquals("Any Death Star already on table closes the system stage",
                0, analyzer.getMassassiAttackRunPackagePullPriority(
                    scn.game(), VirtualTableScenario.LS, deathStar));
        assertFalse(analyzer.isPreferredMassassiAttackRunPackageForceLossCandidate(
                scn.game(), VirtualTableScenario.LS, deathStar));
        assertEquals("Classic torpedoes match the deck's X-wing carrier",
                1, analyzer.getMassassiAttackRunPackagePullPriority(
                    scn.game(), VirtualTableScenario.LS, torpedoes));
        assertEquals("Enhanced torpedoes share the X-wing carrier route",
                1, analyzer.getMassassiAttackRunPackagePullPriority(
                    scn.game(), VirtualTableScenario.LS,
                    enhancedTorpedoes));
        assertEquals("Naboo torpedoes cannot deploy without an N-1 carrier",
                0, analyzer.getMassassiAttackRunPackagePullPriority(
                    scn.game(), VirtualTableScenario.LS, nabooTorpedoes));
    }

    @Test
    public void mboTorpedoStageRequiresItsExactAttachedCarrier() {
        var scn = mboScenario();
        var objective = scn.GetLSCard("objective");
        var dockingBay = scn.GetLSCard("dockingBay");
        var jungle = scn.GetLSCard("jungle");
        var warRoom = scn.GetLSCard("warRoom");
        var trench = scn.GetLSCard("trench");
        var deathStar = scn.GetLSCard("deathStarPackage");
        var attackRun = scn.GetLSCard("attackRun");
        var torpedoes = scn.GetLSCard("torpedoes");
        var torpedoes2 = scn.GetLSCard("torpedoes2");
        var enhancedTorpedoes = scn.GetLSCard("enhancedTorpedoes");
        var nabooTorpedoes = scn.GetLSCard("nabooTorpedoes");
        var xwing = scn.GetLSCard("xwing");
        var pilot = scn.GetLSCard("routeMover");
        var n1 = scn.GetLSCard("n1");
        var pulse = scn.GetLSFiller(4);

        scn.MoveCardsToLSHand(
                pulse, torpedoes, enhancedTorpedoes, nabooTorpedoes);
        scn.StartGame();
        moveLocationToYavin(scn, jungle);
        moveLocationToYavin(scn, warRoom);
        scn.MoveCardsToLocation(dockingBay, scn.GetLSFiller(1));
        scn.MoveCardsToLocation(jungle, scn.GetLSFiller(2));
        scn.MoveCardsToLocation(warRoom, scn.GetLSFiller(3));
        scn.LSActivateForceCheat(8);
        scn.SkipToLSTurn(Phase.DEPLOY);
        scn.LSDeployCardAndPassResponses(
                pulse, scn.GetDSStartingLocation());
        assertTrue(objective.isFlipped());
        scn.DSPass();

        scn.MoveLocationToTable(deathStar);
        scn.MoveLocationToTable(trench);
        scn.AttachCardsTo(trench, attackRun);
        scn.MoveCardsToLocation(deathStar, n1);
        scn.MoveCardsToBottomOfLSReserveDeck(torpedoes2);

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        assertFalse("Classic torpedoes cannot use an N-1 as their deploy host",
                analyzer.isMassassiAttackRunPackageDeployCandidate(
                    scn.game(), VirtualTableScenario.LS, torpedoes));
        assertFalse("Enhanced torpedoes cannot use an N-1 as their deploy host",
                analyzer.isMassassiAttackRunPackageDeployCandidate(
                    scn.game(), VirtualTableScenario.LS,
                    enhancedTorpedoes));
        assertTrue("The Theed Palace torpedoes are valid with an N-1",
                analyzer.isMassassiAttackRunPackageDeployCandidate(
                    scn.game(), VirtualTableScenario.LS, nabooTorpedoes));

        scn.MoveOutOfPlay(nabooTorpedoes);
        scn.MoveCardsToLocation(deathStar, xwing);
        scn.BoardAsPilot(xwing, pilot);
        assertTrue("Enhanced torpedoes become executable with a piloted X-wing",
                analyzer.isMassassiAttackRunPackageDeployCandidate(
                    scn.game(), VirtualTableScenario.LS,
                    enhancedTorpedoes));
        scn.AttachCardsTo(xwing, enhancedTorpedoes);
        assertTrue("Enhanced torpedoes arm the exact Attack Run action",
                analyzer.isMassassiAttackRunAction(
                    scn.game(), VirtualTableScenario.LS,
                    attackRun, "Attempt to 'blow away' Death Star"));
        scn.MoveOutOfPlay(enhancedTorpedoes);
        scn.MoveCardsToLocation(deathStar, torpedoes);
        assertEquals("Unattached torpedoes do not complete the Attack Run package",
                1, analyzer.getMassassiAttackRunPackagePullPriority(
                    scn.game(), VirtualTableScenario.LS, torpedoes2));
        scn.AttachCardsTo(xwing, torpedoes);
        assertEquals("Torpedoes attached to their exact carrier complete the stage",
                0, analyzer.getMassassiAttackRunPackagePullPriority(
                    scn.game(), VirtualTableScenario.LS, torpedoes2));
    }

    @Test
    public void mboRebelTechTutorFallsBackWhenWarRoomIsUnavailable() {
        var scn = mboScenario();
        var objective = scn.GetLSCard("objective");
        var dockingBay = scn.GetLSCard("dockingBay");
        var jungle = scn.GetLSCard("jungle");
        var warRoom = scn.GetLSCard("warRoom");
        var rebelTech = scn.GetLSCard("rebelTech");
        var trench = scn.GetLSCard("trench");
        var pulse = scn.GetLSFiller(4);

        scn.MoveCardsToLSHand(pulse, rebelTech);
        scn.StartGame();
        scn.MoveCardsToBottomOfLSReserveDeck(trench);
        moveLocationToYavin(scn, jungle);
        moveLocationToYavin(scn, warRoom);
        scn.MoveCardsToLocation(dockingBay, scn.GetLSFiller(1));
        scn.MoveCardsToLocation(jungle, scn.GetLSFiller(2));
        scn.MoveCardsToLocation(warRoom, scn.GetLSFiller(3));
        scn.LSActivateForceCheat(8);
        scn.SkipToLSTurn(Phase.DEPLOY);
        scn.LSDeployCardAndPassResponses(
                pulse, scn.GetDSStartingLocation());
        assertTrue(objective.isFlipped());
        scn.DSPass();
        scn.MoveOutOfPlay(warRoom);
        scn.MoveOutOfPlay(jungle);

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        assertTrue("A controlled site is the tutor fallback when War Room is gone",
                analyzer.advancesMassassiRebelTechPackageAt(
                    scn.game(), VirtualTableScenario.LS,
                    rebelTech, dockingBay));
        assertTrue("The fallback Tech remains an executable package deploy",
                analyzer.isMassassiAttackRunPackageDeployCandidate(
                    scn.game(), VirtualTableScenario.LS, rebelTech));

        scn.LSDeployCardAndPassResponses(rebelTech, dockingBay);
        scn.SkipToLSTurn(Phase.CONTROL);
        scn.MoveCardsToBottomOfLSReserveDeck(trench);
        assertTrue("Rebel Tech's printed tutor works away from a war room",
                scn.LSCardActionAvailable(
                    rebelTech, "Take card into hand from Reserve Deck"));
        scn.LSUseCardAction(
                rebelTech, "Take card into hand from Reserve Deck");
        scn.PassAllResponses();
        assertTrue(scn.LSHasCardChoiceAvailable(trench));
    }

    @Test
    public void mboFrontRecognizesAndNativelySuffersTheYavinHardLoss() {
        var scn = mboScenario();
        var objective = scn.GetLSCard("objective");
        var yavin = scn.GetLSCard("system");
        var deathStar = scn.GetDSCard("deathStarThreat");
        var superlaser = scn.GetDSCard("superlaser");
        var cpi = scn.GetDSCard("cpi");

        scn.MoveCardsToDSHand(cpi);
        scn.StartGame();
        scn.MoveLocationToTable(deathStar);
        scn.MoveLocationToTable(scn.GetDSCard("centralCore"));
        scn.MoveLocationToTable(scn.GetDSCard("detention"));
        scn.MoveLocationToTable(scn.GetDSCard("db327"));
        assertEquals(3, Filters.countTopLocationsOnTable(
                scn.game(), Filters.Death_Star_site));
        assertEquals(1, Filters.countTopLocationsOnTable(
                scn.game(), Filters.Yavin_4_site));

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        assertTrue(analyzer.isObjectiveHardLossLocation(
                scn.game(), VirtualTableScenario.LS, yavin));
        assertFalse("A Death Star parked away from Yavin is not a potential CPI setup",
                analyzer.isObjectiveHardLossDefenseLocation(
                    scn.game(), VirtualTableScenario.LS, yavin));
        deathStar.setSystemOrbited(Title.Yavin_4);
        assertFalse("Orbit without a fireable superlaser is not a potential CPI setup",
                analyzer.isObjectiveHardLossDefenseLocation(
                    scn.game(), VirtualTableScenario.LS, yavin));
        scn.AttachCardsTo(deathStar, superlaser);
        assertTrue("A Death Star orbiting Yavin with a fireable superlaser arms defense",
                analyzer.isObjectiveHardLossDefenseLocation(
                        scn.game(), VirtualTableScenario.LS, yavin));

        scn.DSActivateForceCheat(8);
        scn.SkipToDSTurn(Phase.CONTROL);
        scn.PrepareDSDestiny(7);
        assertTrue(scn.DSCardPlayAvailable(
                cpi, "Attempt to 'blow away' Yavin 4"));
        scn.DSPlayCard(cpi, "Attempt to 'blow away' Yavin 4");
        scn.PassAllResponses();
        if (scn.LSDecisionAvailable("Choose value for Z")) {
            scn.LSChoose("Total sites at Yavin 4: 1");
            scn.PassAllResponses();
        }
        finishBlowAwayResolution(scn);

        assertTrue("Yavin was not blown away; current decision="
                        + scn.GetCurrentDecision().getText()
                        + ", CPI zone=" + cpi.getZone(),
                yavin.isBlownAway());
        assertEquals("The actual front trigger must remove MBO",
                Zone.OUT_OF_PLAY, objective.getZone());
    }

    @Test
    public void mboBackSurvivesTheActualYavinBlowAway() {
        var scn = mboScenario();
        var objective = scn.GetLSCard("objective");
        var yavin = scn.GetLSCard("system");
        var dockingBay = scn.GetLSCard("dockingBay");
        var jungle = scn.GetLSCard("jungle");
        var warRoom = scn.GetLSCard("warRoom");
        var pulse = scn.GetLSFiller(4);
        var deathStar = scn.GetDSCard("deathStarThreat");
        var superlaser = scn.GetDSCard("superlaser");
        var cpi = scn.GetDSCard("cpi");

        scn.MoveCardsToLSHand(pulse);
        scn.MoveCardsToDSHand(cpi);
        scn.StartGame();
        moveLocationToYavin(scn, jungle);
        moveLocationToYavin(scn, warRoom);
        scn.MoveCardsToLocation(dockingBay, scn.GetLSFiller(1));
        scn.MoveCardsToLocation(jungle, scn.GetLSFiller(2));
        scn.MoveCardsToLocation(warRoom, scn.GetLSFiller(3));
        scn.LSActivateForceCheat(12);
        scn.SkipToLSTurn(Phase.DEPLOY);
        scn.LSDeployCardAndPassResponses(
                pulse, scn.GetDSStartingLocation());
        assertTrue(objective.isFlipped());
        scn.DSPass();

        scn.MoveLocationToTable(deathStar);
        scn.MoveLocationToTable(scn.GetDSCard("centralCore"));
        scn.MoveLocationToTable(scn.GetDSCard("detention"));
        scn.MoveLocationToTable(scn.GetDSCard("db327"));
        deathStar.setSystemOrbited(Title.Yavin_4);
        scn.AttachCardsTo(deathStar, superlaser);

        var analyzer = new com.gempukku.swccgo.ai.models.rando.strategy
                .ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), VirtualTableScenario.LS, Side.LIGHT);
        assertFalse("The permanent back has no Yavin hard-loss rule",
                analyzer.isObjectiveHardLossDefenseLocation(
                    scn.game(), VirtualTableScenario.LS, yavin));

        scn.DSActivateForceCheat(12);
        scn.SkipToDSTurn(Phase.CONTROL);
        scn.PrepareDSDestiny(7);
        assertTrue(scn.DSCardPlayAvailable(
                cpi, "Attempt to 'blow away' Yavin 4"));
        scn.DSPlayCard(cpi, "Attempt to 'blow away' Yavin 4");
        scn.PassAllResponses();
        if (scn.LSDecisionAvailable("Choose value for Z")) {
            scn.LSChoose("Total sites at Hoth: 0");
            scn.PassAllResponses();
        }
        finishBlowAwayResolution(scn);

        assertTrue(yavin.isBlownAway());
        assertTrue("One In A Million remains flipped after Yavin is gone",
                objective.isFlipped());
        assertEquals("The permanent back remains beside the table",
                Zone.SIDE_OF_TABLE, objective.getZone());
    }
}
