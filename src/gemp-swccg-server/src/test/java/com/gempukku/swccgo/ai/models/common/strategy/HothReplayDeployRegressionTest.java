package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.filters.Filters;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Focused adapter regressions from replays jpjhvzkjczbx3ctm and
 * 70jll8yaavkpyy8h (game 72314).
 */
public class HothReplayDeployRegressionTest {
    private static final String PLAYER = "~Rando_Cal";

    @Test
    public void lowAbilityPilotBoardsBlizzardWithoutSiteOnlyLeakage() {
        PhysicalCard pilot = card(300, "Admiral Ozzel", "3_82",
                CardCategory.CHARACTER, true, 2.0f);
        PhysicalCard blizzard = card(301, "Blizzard 1", "217_4",
                CardCategory.VEHICLE, false, null);
        PhysicalCard site = card(302, "Hoth: North Ridge (4th Marker)", "217_12",
                CardCategory.LOCATION, false, null);
        GameState state = state(Map.of(301, blizzard, 302, site), pilot);
        String prompt = deployPrompt(pilot, false);

        for (CandidateSet candidates : List.of(
                evaluateRandoDeploy(state, prompt, blizzard, site),
                evaluateChosenDeploy(state, prompt, blizzard, site))) {
            Candidate aboard = candidates.byId("301");
            Candidate exposed = candidates.byId("302");
            assertTrue(aboard.score > exposed.score);
            assertTrue(aboard.reasoning.contains("V30 PILOT PROTECTION"));
            assertFalse(aboard.reasoning.contains("FORMATION SAFETY"));
            assertFalse(aboard.reasoning.contains("V24.15"));
            assertFalse(aboard.reasoning.contains("V22.2"));
        }
    }

    @Test
    public void electroRangefinderAttachedToBlizzardIsNotCargo() {
        PhysicalCard device = card(310, "Electro-Rangefinder", "223_11",
                CardCategory.DEVICE, false, null);
        PhysicalCard blizzard = card(311, "Blizzard 1", "217_4",
                CardCategory.VEHICLE, false, null);
        GameState state = state(Map.of(311, blizzard), device);
        String prompt = deployPrompt(device, true);

        for (CandidateSet candidates : List.of(
                evaluateRandoDeploy(state, prompt, blizzard),
                evaluateChosenDeploy(state, prompt, blizzard))) {
            Candidate attached = candidates.byId("311");
            assertFalse(attached.hardVeto);
            assertFalse(attached.reasoning.toLowerCase().contains("cargo"));
        }
    }

    @Test
    public void nonselectableWalkerDoesNotForcePilotAwayFromLegalSite() {
        PhysicalCard pilot = card(315, "Admiral Ozzel", "3_82",
                CardCategory.CHARACTER, true, 2.0f);
        PhysicalCard blizzard = card(316, "Blizzard 1", "217_4",
                CardCategory.VEHICLE, false, null);
        PhysicalCard site = card(317, "Hoth: North Ridge (4th Marker)", "217_12",
                CardCategory.LOCATION, false, null);
        GameState state = state(Map.of(316, blizzard, 317, site), pilot);
        String prompt = deployPrompt(pilot, false);

        for (CandidateSet candidates : List.of(
                evaluateRandoDeploy(state, prompt,
                        List.of(false, true), blizzard, site),
                evaluateChosenDeploy(state, prompt,
                        List.of(false, true), blizzard, site))) {
            Candidate legalSite = candidates.byId("317");
            assertFalse(legalSite.reasoning,
                    legalSite.reasoning.contains("V30 PILOT PROTECTION"));
            assertFalse(legalSite.reasoning, legalSite.hardVeto);
        }
    }

    @Test
    public void lowAbilityPilotChoosesTheOpenSeatInsteadOfAFullWalkerPassengerSlot() {
        PhysicalCard pilot = card(330, "Admiral Ozzel", "3_82",
                CardCategory.CHARACTER, true, 2.0f);
        PhysicalCard fullWalker = card(331, "Blizzard 1", "217_4",
                CardCategory.VEHICLE, false, null);
        PhysicalCard openWalker = card(332, "Blizzard 2", "3_155",
                CardCategory.VEHICLE, false, null);
        GameState state = state(
                Map.of(331, fullWalker, 332, openWalker), pilot);
        when(state.getAvailablePilotCapacity(
                any(), org.mockito.ArgumentMatchers.same(fullWalker),
                org.mockito.ArgumentMatchers.same(pilot))).thenReturn(0);
        when(state.getAvailablePilotCapacity(
                any(), org.mockito.ArgumentMatchers.same(openWalker),
                org.mockito.ArgumentMatchers.same(pilot))).thenReturn(1);

        for (CandidateSet candidates : List.of(
                evaluateRandoDeploy(
                    state, deployPrompt(pilot, false),
                    fullWalker, openWalker),
                evaluateChosenDeploy(
                    state, deployPrompt(pilot, false),
                    fullWalker, openWalker))) {
            Candidate full = candidates.byId("331");
            Candidate open = candidates.byId("332");
            assertTrue(open.score > full.score);
            assertTrue(open.reasoning.contains(
                    "fills an offered open pilot slot"));
            assertTrue(full.reasoning.contains(
                    "must fill an offered open pilot slot"));
        }
    }

    @Test
    public void selectablePassengerDestinationDoesNotMasqueradeAsAnOpenPilotSeat() {
        PhysicalCard pilot = card(340, "Admiral Ozzel", "3_82",
                CardCategory.CHARACTER, true, 2.0f);
        PhysicalCard passengerOnlyWalker = card(
                341, "Restricted Walker", "fixture_341",
                CardCategory.VEHICLE, false, null);
        PhysicalCard openWalker = card(
                342, "Blizzard 2", "3_155",
                CardCategory.VEHICLE, false, null);
        when(passengerOnlyWalker.getBlueprint()
                .getValidPilotFilter(
                    any(), any(), any(), anyBoolean()))
                .thenReturn(Filters.none);
        GameState state = state(
                Map.of(
                    341, passengerOnlyWalker,
                    342, openWalker),
                pilot);

        for (CandidateSet candidates : List.of(
                evaluateRandoDeploy(
                    state, deployPrompt(pilot, false),
                    passengerOnlyWalker, openWalker),
                evaluateChosenDeploy(
                    state, deployPrompt(pilot, false),
                    passengerOnlyWalker, openWalker))) {
            Candidate passengerOnly =
                    candidates.byId("341");
            Candidate open = candidates.byId("342");
            assertTrue(open.score > passengerOnly.score);
            assertTrue(open.reasoning.contains(
                    "fills an offered open pilot slot"));
            assertTrue(passengerOnly.reasoning.contains(
                    "must fill an offered open pilot slot"));
        }
    }

    @Test
    public void tatooineReplayPilotedDevastatorDoesNotVetoTarkinsGroundRoute() {
        PhysicalCard tarkin = card(350, "Grand Moff Tarkin (V)", "200_82",
                CardCategory.CHARACTER, true, 4.0f);
        PhysicalCard devastator = card(351, "Devastator (V)", "216_8",
                CardCategory.STARSHIP, false, null);
        PhysicalCard jawaCamp = card(352, "Tatooine: Jawa Camp", "1_292",
                CardCategory.LOCATION, false, null);
        GameState permanentPilotState = state(
                Map.of(351, devastator, 352, jawaCamp), tarkin);

        PhysicalCard characterPilotedShip = card(
                353, "Imperial-Class Star Destroyer", "1_302",
                CardCategory.STARSHIP, false, null);
        PhysicalCard aboardPilot = card(
                354, "Admiral Ozzel", "3_82",
                CardCategory.CHARACTER, true, 2.0f);
        GameState characterPilotState = state(
                Map.of(352, jawaCamp, 353, characterPilotedShip), tarkin);
        when(characterPilotState.getAboardCards(characterPilotedShip, false))
                .thenReturn(List.of(aboardPilot));

        // Replay 70jll8yaavkpyy8h: an offered spare seat on a ship that is
        // already piloted, whether by a permanent or physical character pilot,
        // must not manufacture V30's -5000 veto on Tarkin's ground destination.
        for (CandidateSet candidates : List.of(
                evaluateRandoDeployWithPilotedAssets(
                        permanentPilotState, deployPrompt(tarkin, false),
                        Set.of(351), devastator, jawaCamp),
                evaluateChosenDeployWithPilotedAssets(
                        permanentPilotState, deployPrompt(tarkin, false),
                        Set.of(351), devastator, jawaCamp),
                evaluateRandoDeployWithPilotedAssets(
                        characterPilotState, deployPrompt(tarkin, false),
                        Set.of(353), characterPilotedShip, jawaCamp),
                evaluateChosenDeployWithPilotedAssets(
                        characterPilotState, deployPrompt(tarkin, false),
                        Set.of(353), characterPilotedShip, jawaCamp))) {
            Candidate ground = candidates.byId("352");
            assertFalse(ground.reasoning.contains("V30 PILOT PROTECTION"));
            assertTrue(ground.score > -100.0f);
        }
    }

    @Test
    public void tatooineReplayTrulyUnpilotedShipStillForcesBoarding() {
        PhysicalCard tarkin = card(360, "Grand Moff Tarkin (V)", "200_82",
                CardCategory.CHARACTER, true, 4.0f);
        PhysicalCard unpilotedShip = card(
                361, "Imperial-Class Star Destroyer", "1_302",
                CardCategory.STARSHIP, false, null);
        PhysicalCard jawaCamp = card(362, "Tatooine: Jawa Camp", "1_292",
                CardCategory.LOCATION, false, null);
        GameState state = state(
                Map.of(361, unpilotedShip, 362, jawaCamp), tarkin);

        // Negative boundary for replay 70jll8yaavkpyy8h: removing the false
        // Devastator steer must preserve V30 when the offered ship is genuinely
        // unpiloted and the character can legally fill its open pilot slot.
        for (CandidateSet candidates : List.of(
                evaluateRandoDeploy(
                        state, deployPrompt(tarkin, false),
                        unpilotedShip, jawaCamp),
                evaluateChosenDeploy(
                        state, deployPrompt(tarkin, false),
                        unpilotedShip, jawaCamp))) {
            Candidate ship = candidates.byId("361");
            Candidate ground = candidates.byId("362");
            assertTrue(ship.score > ground.score);
            assertTrue(ship.reasoning.contains(
                    "fills an offered open pilot slot"));
            assertTrue(ground.reasoning.contains(
                    "must fill an offered open pilot slot"));
        }
    }

    @Test
    public void everyResolvedDeployZoneReceivesFormationSafety() {
        PhysicalCard pilot = card(370, "Admiral Ozzel", "3_82",
                CardCategory.CHARACTER, true, 2.0f);
        PhysicalCard opponentCopy = card(371, "Admiral Ozzel", "3_82",
                CardCategory.CHARACTER, true, 2.0f);
        when(opponentCopy.getOwner()).thenReturn("opponent");
        PhysicalCard site = card(372, "Hoth: North Ridge (4th Marker)", "217_12",
                CardCategory.LOCATION, false, null);

        GameState reserveState = state(
                Map.of(370, pilot, 371, opponentCopy, 372, site), pilot);
        when(reserveState.getHand(PLAYER)).thenReturn(List.of());
        when(reserveState.getCardPile(PLAYER, Zone.RESERVE_DECK))
                .thenReturn(List.of(pilot));
        when(reserveState.getAllStackedCards()).thenReturn(List.of(opponentCopy));

        GameState lostState = state(Map.of(370, pilot, 372, site), pilot);
        when(lostState.getHand(PLAYER)).thenReturn(List.of());
        when(lostState.getLostPile(PLAYER)).thenReturn(List.of(pilot));

        GameState stackedState = state(Map.of(370, pilot, 372, site), pilot);
        when(stackedState.getHand(PLAYER)).thenReturn(List.of());
        when(stackedState.getAllStackedCards()).thenReturn(List.of(pilot));

        GameState sourceProvenState = state(
                Map.of(370, pilot, 372, site), pilot);
        when(sourceProvenState.getHand(PLAYER)).thenReturn(List.of());

        for (GameState zoneState : List.of(
                reserveState, lostState, stackedState)) {
            for (CandidateSet candidates : List.of(
                    evaluateRandoDeploy(
                            zoneState, deployPrompt(pilot, false), site),
                    evaluateChosenDeploy(
                            zoneState, deployPrompt(pilot, false), site))) {
                Candidate ground = candidates.byId("372");
                assertTrue(ground.reasoning,
                        ground.reasoning.contains("L3 NO-PLAN SOLO"));
            }
        }

        for (CandidateSet candidates : List.of(
                evaluateRandoDeployWithProvenance(
                        sourceProvenState, deployPrompt(pilot, false),
                        pilot.getPermanentCardId(), site),
                evaluateChosenDeployWithProvenance(
                        sourceProvenState, deployPrompt(pilot, false),
                        pilot.getPermanentCardId(), site))) {
            Candidate ground = candidates.byId("372");
            assertTrue(ground.reasoning,
                    ground.reasoning.contains("L3 NO-PLAN SOLO"));
        }
    }

    @Test
    public void veersRevealRequiresARealReserveTarget() {
        PhysicalCard veers = card(320, "Veers", "206_11",
                CardCategory.CHARACTER, true, 5.0f);
        GameState state = state(Map.of(320, veers), veers);
        String actionText = "Reveal to deploy 6th Marker or Blizzard 1";

        CandidateSet randoMissing = evaluateRandoAction(
                state, veers, actionText, false);
        CandidateSet chosenMissing = evaluateChosenAction(
                state, veers, actionText, false);
        CandidateSet randoPresent = evaluateRandoAction(
                state, veers, actionText, true);
        CandidateSet chosenPresent = evaluateChosenAction(
                state, veers, actionText, true);

        for (CandidateSet missing : List.of(randoMissing, chosenMissing)) {
            Candidate action = missing.byId("reveal");
            assertTrue(action.hardVeto || action.score < -100.0f);
        }
        for (CandidateSet present : List.of(randoPresent, chosenPresent)) {
            Candidate action = present.byId("reveal");
            assertFalse(action.hardVeto);
            assertTrue(action.score > -100.0f);
        }
    }

    @Test
    public void veersDoesNotAcceptMisleadingSixthMarkerTitle() {
        PhysicalCard veers = card(325, "Veers", "206_11",
                CardCategory.CHARACTER, true, 5.0f);
        PhysicalCard fake = card(326, "Training Prop (6th Marker)", "1_1",
                CardCategory.EFFECT, false, null);
        GameState state = state(Map.of(325, veers), veers);
        String actionText = "Reveal to deploy 6th Marker or Blizzard 1";

        for (CandidateSet candidates : List.of(
                evaluateRandoAction(state, veers, actionText, fake),
                evaluateChosenAction(state, veers, actionText, fake))) {
            Candidate action = candidates.byId("reveal");
            assertTrue(action.hardVeto || action.score < -100.0f);
        }
    }

    private static CandidateSet evaluateRandoDeploy(
            GameState state, String prompt, PhysicalCard... destinations) {
        return evaluateRandoDeploy(state, prompt,
                java.util.Collections.nCopies(destinations.length, true),
                destinations);
    }

    private static CandidateSet evaluateRandoDeploy(
            GameState state, String prompt, List<Boolean> selectable,
            PhysicalCard... destinations) {
        return evaluateRandoDeploy(
                state, prompt, selectable, Set.of(), destinations);
    }

    private static CandidateSet evaluateRandoDeployWithPilotedAssets(
            GameState state, String prompt, Set<Integer> pilotedAssetIds,
            PhysicalCard... destinations) {
        return evaluateRandoDeploy(
                state, prompt,
                java.util.Collections.nCopies(destinations.length, true),
                pilotedAssetIds, destinations);
    }

    private static CandidateSet evaluateRandoDeploy(
            GameState state, String prompt, List<Boolean> selectable,
            Set<Integer> pilotedAssetIds, PhysicalCard... destinations) {
        return evaluateRandoDeploy(
                state, prompt, selectable, pilotedAssetIds,
                null, destinations);
    }

    private static CandidateSet evaluateRandoDeployWithProvenance(
            GameState state, String prompt, Integer deployingPermanentId,
            PhysicalCard... destinations) {
        return evaluateRandoDeploy(
                state, prompt,
                java.util.Collections.nCopies(destinations.length, true),
                Set.of(), deployingPermanentId, destinations);
    }

    private static CandidateSet evaluateRandoDeploy(
            GameState state, String prompt, List<Boolean> selectable,
            Set<Integer> pilotedAssetIds, Integer deployingPermanentId,
            PhysicalCard... destinations) {
        var context = new com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext(
                state, PLAYER, "CARD_SELECTION", prompt,
                "hoth-deploy-child", Phase.DEPLOY);
        var game = mock(com.gempukku.swccgo.game.SwccgGame.class);
        var modifiers = mock(
                com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying.class);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(modifiers.isPiloted(
                org.mockito.ArgumentMatchers.same(state),
                any(PhysicalCard.class),
                org.mockito.ArgumentMatchers.eq(false)))
                .thenAnswer(call -> pilotedAssetIds.contains(
                        call.getArgument(1, PhysicalCard.class).getCardId()));
        context.setGame(game);
        if (deployingPermanentId != null) {
            context.setExtra(
                    ObjectiveAnalyzer.OBJECTIVE_DEPLOYING_CARD_ID_EXTRA,
                    deployingPermanentId);
        }
        setDeployCandidates(context, selectable, destinations);
        return new CandidateSet(
                new com.gempukku.swccgo.ai.models.rando.evaluators.CardSelectionEvaluator()
                        .evaluate(context).stream()
                        .map(a -> new Candidate(a.getActionId(), a.getScore(),
                                a.isHardVetoed(), a.getReasoningString()))
                        .toList());
    }

    private static CandidateSet evaluateChosenDeploy(
            GameState state, String prompt, PhysicalCard... destinations) {
        return evaluateChosenDeploy(state, prompt,
                java.util.Collections.nCopies(destinations.length, true),
                destinations);
    }

    private static CandidateSet evaluateChosenDeploy(
            GameState state, String prompt, List<Boolean> selectable,
            PhysicalCard... destinations) {
        return evaluateChosenDeploy(
                state, prompt, selectable, Set.of(), destinations);
    }

    private static CandidateSet evaluateChosenDeployWithPilotedAssets(
            GameState state, String prompt, Set<Integer> pilotedAssetIds,
            PhysicalCard... destinations) {
        return evaluateChosenDeploy(
                state, prompt,
                java.util.Collections.nCopies(destinations.length, true),
                pilotedAssetIds, destinations);
    }

    private static CandidateSet evaluateChosenDeploy(
            GameState state, String prompt, List<Boolean> selectable,
            Set<Integer> pilotedAssetIds, PhysicalCard... destinations) {
        return evaluateChosenDeploy(
                state, prompt, selectable, pilotedAssetIds,
                null, destinations);
    }

    private static CandidateSet evaluateChosenDeployWithProvenance(
            GameState state, String prompt, Integer deployingPermanentId,
            PhysicalCard... destinations) {
        return evaluateChosenDeploy(
                state, prompt,
                java.util.Collections.nCopies(destinations.length, true),
                Set.of(), deployingPermanentId, destinations);
    }

    private static CandidateSet evaluateChosenDeploy(
            GameState state, String prompt, List<Boolean> selectable,
            Set<Integer> pilotedAssetIds, Integer deployingPermanentId,
            PhysicalCard... destinations) {
        var context = new com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext(
                state, PLAYER, "CARD_SELECTION", prompt,
                "hoth-deploy-child", Phase.DEPLOY);
        var game = mock(com.gempukku.swccgo.game.SwccgGame.class);
        var modifiers = mock(
                com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying.class);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(modifiers.isPiloted(
                org.mockito.ArgumentMatchers.same(state),
                any(PhysicalCard.class),
                org.mockito.ArgumentMatchers.eq(false)))
                .thenAnswer(call -> pilotedAssetIds.contains(
                        call.getArgument(1, PhysicalCard.class).getCardId()));
        context.setGame(game);
        if (deployingPermanentId != null) {
            context.setExtra(
                    ObjectiveAnalyzer.OBJECTIVE_DEPLOYING_CARD_ID_EXTRA,
                    deployingPermanentId);
        }
        setDeployCandidates(context, selectable, destinations);
        return new CandidateSet(
                new com.gempukku.swccgo.ai.models.chosenone.evaluators.CardSelectionEvaluator()
                        .evaluate(context).stream()
                        .map(a -> new Candidate(a.getActionId(), a.getScore(),
                                a.isHardVetoed(), a.getReasoningString()))
                        .toList());
    }

    private static CandidateSet evaluateRandoAction(
            GameState state, PhysicalCard source,
            String actionText, boolean targetPresent) {
        PhysicalCard target = card(321, "Blizzard 1", "217_4",
                CardCategory.VEHICLE, false, null);
        return evaluateRandoAction(
                state, source, actionText, targetPresent ? target : null);
    }

    private static CandidateSet evaluateRandoAction(
            GameState state, PhysicalCard source,
            String actionText, PhysicalCard reserveTarget) {
        when(state.getCardPile(PLAYER, Zone.RESERVE_DECK)).thenReturn(
                reserveTarget != null ? List.of(reserveTarget) : List.of());
        var context = new com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext(
                state, PLAYER, "CARD_ACTION_CHOICE",
                "Choose Deploy action or Pass", "hoth-veers-reveal", Phase.DEPLOY);
        var game = mock(com.gempukku.swccgo.game.SwccgGame.class);
        var modifiers = mock(
                com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying.class);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        context.setGame(game);
        context.setSide(Side.DARK);
        context.setCardIds(List.of(String.valueOf(source.getCardId())));
        context.setActionIds(List.of("reveal"));
        context.setActionTexts(List.of(actionText));
        var oracle = mock(com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle.class);
        when(oracle.isAnalyzed()).thenReturn(true);
        context.setDeckOracle(oracle);
        return new CandidateSet(
                new com.gempukku.swccgo.ai.models.rando.evaluators.ActionTextEvaluator()
                        .evaluate(context).stream()
                        .map(a -> new Candidate(a.getActionId(), a.getScore(),
                                a.isHardVetoed(), a.getReasoningString()))
                        .toList());
    }

    private static CandidateSet evaluateChosenAction(
            GameState state, PhysicalCard source,
            String actionText, boolean targetPresent) {
        PhysicalCard target = card(321, "Blizzard 1", "217_4",
                CardCategory.VEHICLE, false, null);
        return evaluateChosenAction(
                state, source, actionText, targetPresent ? target : null);
    }

    private static CandidateSet evaluateChosenAction(
            GameState state, PhysicalCard source,
            String actionText, PhysicalCard reserveTarget) {
        when(state.getCardPile(PLAYER, Zone.RESERVE_DECK)).thenReturn(
                reserveTarget != null ? List.of(reserveTarget) : List.of());
        var context = new com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext(
                state, PLAYER, "CARD_ACTION_CHOICE",
                "Choose Deploy action or Pass", "hoth-veers-reveal", Phase.DEPLOY);
        var game = mock(com.gempukku.swccgo.game.SwccgGame.class);
        var modifiers = mock(
                com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying.class);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        context.setGame(game);
        context.setSide(Side.DARK);
        context.setCardIds(List.of(String.valueOf(source.getCardId())));
        context.setActionIds(List.of("reveal"));
        context.setActionTexts(List.of(actionText));
        var oracle = mock(com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle.class);
        when(oracle.isAnalyzed()).thenReturn(true);
        context.setDeckOracle(oracle);
        return new CandidateSet(
                new com.gempukku.swccgo.ai.models.chosenone.evaluators.ActionTextEvaluator()
                        .evaluate(context).stream()
                        .map(a -> new Candidate(a.getActionId(), a.getScore(),
                                a.isHardVetoed(), a.getReasoningString()))
                        .toList());
    }

    private static void setDeployCandidates(
            com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext context,
            PhysicalCard... destinations) {
        setDeployCandidates(context,
                java.util.Collections.nCopies(destinations.length, true),
                destinations);
    }

    private static void setDeployCandidates(
            com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext context,
            List<Boolean> selectable, PhysicalCard... destinations) {
        context.setSide(Side.DARK);
        context.setCardIds(ids(destinations));
        context.setBlueprints(blueprints(destinations));
        context.setTestingTexts(titles(destinations));
        context.setSelectable(selectable);
        context.setNoPass(true);
        context.setMin(1);
        context.setMax(1);
    }

    private static void setDeployCandidates(
            com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext context,
            PhysicalCard... destinations) {
        setDeployCandidates(context,
                java.util.Collections.nCopies(destinations.length, true),
                destinations);
    }

    private static void setDeployCandidates(
            com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext context,
            List<Boolean> selectable, PhysicalCard... destinations) {
        context.setSide(Side.DARK);
        context.setCardIds(ids(destinations));
        context.setBlueprints(blueprints(destinations));
        context.setTestingTexts(titles(destinations));
        context.setSelectable(selectable);
        context.setNoPass(true);
        context.setMin(1);
        context.setMax(1);
    }

    private static String deployPrompt(PhysicalCard card, boolean attached) {
        return "Choose where to deploy <div class='cardHint' value='"
                + card.getBlueprintId(true) + "'>" + card.getTitle()
                + "</div>" + (attached ? " as attached" : "")
                + ", or click 'Done' to cancel";
    }

    private static GameState state(
            Map<Integer, PhysicalCard> cards, PhysicalCard handCard) {
        GameState state = mock(GameState.class);
        when(state.getCurrentPlayerId()).thenReturn(PLAYER);
        when(state.getPlayersLatestTurnNumber(PLAYER)).thenReturn(3);
        when(state.findCardById(anyInt())).thenAnswer(
                call -> cards.get(call.getArgument(0, Integer.class)));
        when(state.findCardByPermanentId(anyInt())).thenAnswer(call -> {
            int permanentId =
                    call.getArgument(0, Integer.class);
            PhysicalCard mapped = cards.get(permanentId);
            if (mapped != null) {
                return mapped;
            }
            return handCard.getPermanentCardId()
                    == permanentId ? handCard : null;
        });
        when(state.getCaptivesOfEscort(
                any(PhysicalCard.class))).thenReturn(List.of());
        when(state.getHand(PLAYER)).thenReturn(List.of(handCard));
        when(state.getCardPile(PLAYER, Zone.RESERVE_DECK)).thenReturn(List.of());
        when(state.getLostPile(PLAYER)).thenReturn(List.of());
        when(state.getAllStackedCards()).thenReturn(List.of());
        when(state.getAvailablePilotCapacity(
                any(), any(PhysicalCard.class),
                any(PhysicalCard.class))).thenAnswer(call -> {
                    PhysicalCard destination =
                            call.getArgument(1, PhysicalCard.class);
                    CardCategory category = destination != null
                            && destination.getBlueprint() != null
                            ? destination.getBlueprint()
                                .getCardCategory() : null;
                    return category == CardCategory.VEHICLE
                            || category == CardCategory.STARSHIP
                            ? 1 : 0;
                });
        return state;
    }

    private static PhysicalCard card(
            int id, String title, String blueprintId,
            CardCategory category, boolean pilot, Float ability) {
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(blueprint.getCardCategory()).thenReturn(category);
        when(blueprint.hasIcon(Icon.PILOT)).thenReturn(pilot);
        when(blueprint.hasAbilityAttribute()).thenReturn(ability != null);
        if (ability != null) {
            when(blueprint.getAbility()).thenReturn(ability);
        }
        PhysicalCard card = mock(PhysicalCard.class);
        when(card.getCardId()).thenReturn(id);
        when(card.getPermanentCardId()).thenReturn(id);
        when(card.getTitle()).thenReturn(title);
        when(card.getTitles()).thenReturn(List.of(title));
        when(card.isBlownAway()).thenReturn(false);
        when(card.getOwner()).thenReturn(PLAYER);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.getBlueprintId(true)).thenReturn(blueprintId);
        if (category == CardCategory.VEHICLE
                || category == CardCategory.STARSHIP) {
            when(blueprint.getValidPilotFilter(
                    any(), any(), any(), anyBoolean()))
                    .thenReturn(Filters.any);
        }
        return card;
    }

    private static List<String> ids(PhysicalCard[] cards) {
        return java.util.Arrays.stream(cards)
                .map(card -> String.valueOf(card.getCardId())).toList();
    }

    private static List<String> blueprints(PhysicalCard[] cards) {
        return java.util.Arrays.stream(cards)
                .map(card -> card.getBlueprintId(true)).toList();
    }

    private static List<String> titles(PhysicalCard[] cards) {
        return java.util.Arrays.stream(cards)
                .map(PhysicalCard::getTitle).toList();
    }

    private record Candidate(
            String id, float score, boolean hardVeto, String reasoning) {
    }

    private record CandidateSet(List<Candidate> candidates) {
        private Candidate byId(String id) {
            return candidates.stream()
                    .filter(candidate -> id.equals(candidate.id))
                    .findFirst()
                    .orElseThrow();
        }
    }
}
