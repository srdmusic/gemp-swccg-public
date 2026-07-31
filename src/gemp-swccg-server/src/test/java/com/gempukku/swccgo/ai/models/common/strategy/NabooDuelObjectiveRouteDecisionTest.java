package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.TargetingReason;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgCardBlueprintLibrary;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** CombinedEvaluator route proof for the mirrored Naboo duel objectives. */
public class NabooDuelObjectiveRouteDecisionTest {
    private static final String PLAYER = "player";
    private static final String OPPONENT = "opponent";
    private static final int OBJECTIVE_ID = 1;
    private static final int GATE_ID = 100;
    private static final int SIBLING_ID = 101;
    private static final int STAGING_ID = 102;
    private static final int DUELIST_ID = 200;
    private static final int GENERIC_ID = 201;
    private static final int STRANDED_DUELIST_ID = 202;
    private static final int TARGET_ID = 300;
    private static final SwccgCardBlueprintLibrary CARDS =
            new SwccgCardBlueprintLibrary();

    private static final List<Family> FAMILIES = List.of(
            new Family(
                    "We'll Handle This", "13_46", "13_46_BACK",
                    Side.LIGHT, "13_31", "13_32",
                    "1_21", "1_168", "1_28", "1_194"),
            new Family(
                    "Let Them Make The First Move", "13_73", "13_73_BACK",
                    Side.DARK, "13_76", "13_77",
                    "1_168", "1_21", "1_194", "1_28"));

    private static final String FRONT_ROUTE_MARKER =
            "NABOO DUEL FRONT ROUTE";

    @Test
    public void frontPayoffFactsRecognizeTheExactLegalPairing() {
        for (Family family : FAMILIES) {
            for (Bot bot : Bot.values()) {
                Fixture fixture = frontFixture(
                        bot, family, null, 0.0f);
                PhysicalCard objective = fixture.gameState
                        .findCardById(OBJECTIVE_ID);
                assertTrue(label(bot, family),
                        Filters.interior_Theed_Palace_site.accepts(
                                fixture.gameState,
                                fixture.game.getModifiersQuerying(),
                                fixture.gate));
                assertTrue(label(bot, family),
                        Filters.canBeTargetedBy(
                                objective,
                                TargetingReason.TO_BE_LOST)
                            .accepts(
                                fixture.gameState,
                                fixture.game.getModifiersQuerying(),
                                fixture.target));
                assertTrue(label(bot, family), fixture.analyzer
                        .advancesNabooDuelFrontTargetRouteAt(
                                fixture.game, PLAYER,
                                fixture.duelist, fixture.gate));
            }
        }
    }

    @Test
    public void typedDuelistDeployParentBeatsGenericAndPass() {
        for (Family family : FAMILIES) {
            List<Outcome> parity = new ArrayList<>();
            for (Bot bot : Bot.values()) {
                Fixture fixture = fixture(bot, family, null, 0.0f);
                Decision decision = Decision.deployParent(fixture);
                List<Outcome> candidates = deployAdapter(
                        bot, fixture, decision);

                Outcome winner = combined(bot, fixture, decision);
                assertEquals(label(bot, family) + " candidates=" + candidates,
                        "deploy-duelist", winner.actionId);
                assertContains(only(candidates, "deploy-duelist"),
                        "V67ak KEY CHARACTER");
                parity.add(winner);
            }
            assertParity(parity);
        }
    }

    @Test
    public void backHandDuelistDeployParentReopensWhenDeployedCopyIsStranded() {
        assertHandDuelistDeployParentWinsWithStrandedCopy(true);
    }

    @Test
    public void frontHandDuelistDeployParentReopensWhenDeployedCopyIsStranded() {
        assertHandDuelistDeployParentWinsWithStrandedCopy(false);
    }

    @Test
    public void duelistDeploysToExactOpponentSiteInsteadOfSiblingNabooSite() {
        for (Family family : FAMILIES) {
            List<Outcome> parity = new ArrayList<>();
            for (Bot bot : Bot.values()) {
                Fixture fixture = fixture(bot, family, null, 0.0f);
                Decision decision = Decision.deployDestination(fixture);

                Outcome exact = only(
                        cardSelectionAdapter(bot, fixture, decision),
                        String.valueOf(GATE_ID));
                assertContains(exact,
                        "primary back-side payoff");
                Outcome winner = combined(bot, fixture, decision);
                assertEquals(label(bot, family),
                        String.valueOf(GATE_ID), winner.actionId);
                parity.add(winner);
            }
            assertParity(parity);
        }
    }

    @Test
    public void moveParentAndDestinationReachTheExactDuelSite() {
        for (Family family : FAMILIES) {
            List<Outcome> parentParity = new ArrayList<>();
            List<Outcome> destinationParity = new ArrayList<>();
            for (Bot bot : Bot.values()) {
                Fixture fixture = fixture(
                        bot, family, STAGING_ID, 0.0f);

                Decision parent = Decision.moveParent(fixture);
                Outcome move = combined(bot, fixture, parent);
                assertEquals(label(bot, family),
                        "move-duelist", move.actionId);
                assertContains(move,
                        "MOVE.OBJECTIVE.POST_FLIP_PAYOFF_START");
                parentParity.add(move);

                Decision destination =
                        Decision.moveDestination(fixture);
                Outcome exact = combined(bot, fixture, destination);
                assertEquals(label(bot, family),
                        String.valueOf(GATE_ID), exact.actionId);
                assertContains(exact,
                        "MOVE.OBJECTIVE.POST_FLIP_PRIMARY_PAYOFF");
                destinationParity.add(exact);
            }
            assertParity(parentParity);
            assertParity(destinationParity);
        }
    }

    @Test
    public void doomedSoloMoveToThePayoffSiteRemainsHardVetoed() {
        for (Family family : FAMILIES) {
            List<Outcome> parity = new ArrayList<>();
            for (Bot bot : Bot.values()) {
                Fixture fixture = fixture(
                        bot, family, STAGING_ID, 20.0f);
                Decision decision = Decision.moveDestination(fixture);

                Outcome doomed = only(
                        cardSelectionAdapter(bot, fixture, decision),
                        String.valueOf(GATE_ID));
                assertContains(doomed,
                        "MOVE.OBJECTIVE.POST_FLIP_PRIMARY_PAYOFF");
                assertTrue(label(bot, family) + " " + doomed,
                        doomed.hardVeto);
                assertTrue(label(bot, family) + " " + doomed,
                        doomed.vetoReason != null
                                && doomed.vetoReason.contains(
                                    "L4 SOLO CHARGE"));

                Outcome winner = combined(bot, fixture, decision);
                assertEquals(label(bot, family),
                        String.valueOf(SIBLING_ID), winner.actionId);
                parity.add(winner);
            }
            assertParity(parity);
        }
    }

    @Test
    public void leavingAnEstablishedDuelPayoffLosesToPass() {
        for (Family family : FAMILIES) {
            List<Outcome> parity = new ArrayList<>();
            for (Bot bot : Bot.values()) {
                Fixture fixture = fixture(
                        bot, family, GATE_ID, 0.0f);
                Decision decision = Decision.moveParent(fixture);

                Outcome move = only(
                        moveAdapter(bot, fixture, decision),
                        "move-duelist");
                assertContains(move,
                        "MOVE.OBJECTIVE.POST_FLIP_PAYOFF_HOLD");
                Outcome winner = combined(bot, fixture, decision);
                assertEquals(label(bot, family),
                        "pass", winner.actionId);
                parity.add(winner);
            }
            assertParity(parity);
        }
    }

    @Test
    public void frontTypedDuelistDeployParentBeatsGenericAndPassWithoutAFlipActor() {
        for (Family family : FAMILIES) {
            List<Outcome> parity = new ArrayList<>();
            for (Bot bot : Bot.values()) {
                Fixture fixture = frontFixture(
                        bot, family, null, 0.0f);
                assertFrontFlipUnsatisfied(bot, fixture);
                Decision decision = Decision.deployParent(fixture);
                List<Outcome> candidates = deployAdapter(
                        bot, fixture, decision);

                Outcome winner = combined(bot, fixture, decision);
                assertEquals(label(bot, family) + " candidates=" + candidates,
                        "deploy-duelist", winner.actionId);
                assertContains(only(candidates, "deploy-duelist"),
                        "V67ak KEY CHARACTER");
                parity.add(winner);
            }
            assertParity(parity);
        }
    }

    @Test
    public void frontDuelistDeploysToTheExactGenericOpponentSite() {
        for (Family family : FAMILIES) {
            List<Outcome> parity = new ArrayList<>();
            for (Bot bot : Bot.values()) {
                Fixture fixture = frontFixture(
                        bot, family, null, 0.0f);
                assertFrontFlipUnsatisfied(bot, fixture);
                Decision decision = Decision.deployDestination(fixture);

                Outcome exact = combined(bot, fixture, decision);
                assertEquals(label(bot, family),
                        String.valueOf(GATE_ID), exact.actionId);
                assertContains(exact, FRONT_ROUTE_MARKER);
                parity.add(exact);
            }
            assertParity(parity);
        }
    }

    @Test
    public void frontMoveParentAndDestinationReachTheExactGenericOpponentSite() {
        for (Family family : FAMILIES) {
            List<Outcome> parentParity = new ArrayList<>();
            List<Outcome> destinationParity = new ArrayList<>();
            for (Bot bot : Bot.values()) {
                Fixture fixture = frontFixture(
                        bot, family, STAGING_ID, 0.0f);
                assertFrontFlipUnsatisfied(bot, fixture);

                Decision parentDecision = Decision.moveParent(fixture);
                List<Outcome> parentCandidates = moveAdapter(
                        bot, fixture, parentDecision);
                Outcome parent = combined(
                        bot, fixture, parentDecision);
                assertEquals(label(bot, family)
                                + " candidates=" + parentCandidates,
                        "move-duelist", parent.actionId);
                assertContains(parent, FRONT_ROUTE_MARKER);
                parentParity.add(parent);

                Outcome destination = combined(
                        bot, fixture,
                        Decision.moveDestination(fixture));
                assertEquals(label(bot, family),
                        String.valueOf(GATE_ID), destination.actionId);
                assertContains(destination, FRONT_ROUTE_MARKER);
                destinationParity.add(destination);
            }
            assertParity(parentParity);
            assertParity(destinationParity);
        }
    }

    @Test
    public void frontDuelistHoldsAnEstablishedTargetLossPairing() {
        for (Family family : FAMILIES) {
            List<Outcome> parity = new ArrayList<>();
            for (Bot bot : Bot.values()) {
                Fixture fixture = frontFixture(
                        bot, family, GATE_ID, 0.0f);
                assertFrontFlipUnsatisfied(bot, fixture);
                Decision decision = Decision.moveParent(fixture);

                Outcome move = only(
                        moveAdapter(bot, fixture, decision),
                        "move-duelist");
                assertContains(move, FRONT_ROUTE_MARKER);
                Outcome winner = combined(bot, fixture, decision);
                assertEquals(label(bot, family),
                        "pass", winner.actionId);
                parity.add(winner);
            }
            assertParity(parity);
        }
    }

    @Test
    public void frontDoomedSoloMoveRemainsHardVetoed() {
        for (Family family : FAMILIES) {
            List<Outcome> parity = new ArrayList<>();
            for (Bot bot : Bot.values()) {
                Fixture fixture = frontFixture(
                        bot, family, STAGING_ID, 20.0f);
                assertFrontFlipUnsatisfied(bot, fixture);
                Decision decision = Decision.moveDestination(fixture);

                Outcome doomed = only(
                        cardSelectionAdapter(bot, fixture, decision),
                        String.valueOf(GATE_ID));
                assertTrue(label(bot, family) + " " + doomed,
                        doomed.hardVeto);
                assertTrue(label(bot, family) + " " + doomed,
                        doomed.vetoReason != null
                                && doomed.vetoReason.contains(
                                    "L4 SOLO CHARGE"));
                Outcome winner = combined(bot, fixture, decision);
                assertEquals(label(bot, family),
                        String.valueOf(SIBLING_ID), winner.actionId);
                parity.add(winner);
            }
            assertParity(parity);
        }
    }

    private static void assertFrontFlipUnsatisfied(
            Bot bot, Fixture fixture) {
        var states = fixture.analyzer.assessFlipLocationRules(
                fixture.game, PLAYER, "preFlip", "flip");
        assertEquals(label(bot, fixture.family), 1, states.size());
        assertTrue(label(bot, fixture.family),
                !states.get(0).conditionSatisfied());
    }

    private static void assertHandDuelistDeployParentWinsWithStrandedCopy(
            boolean flipped) {
        List<String> failures = new ArrayList<>();
        for (Family family : FAMILIES) {
            for (Bot bot : Bot.values()) {
                Fixture fixture = flipped
                        ? fixture(bot, family, null, 0.0f)
                        : frontFixture(bot, family, null, 0.0f);
                PhysicalCard stranded = addStrandedDuelist(fixture);

                boolean strandedCanReachTarget =
                        Filters.canMoveToUsingLandspeed(
                                PLAYER, stranded,
                                false, false, false, 0.0f, null)
                            .accepts(
                                fixture.gameState,
                                fixture.game.getModifiersQuerying(),
                                fixture.gate);
                if (strandedCanReachTarget) {
                    failures.add(label(bot, family)
                            + " fixture error: stranded duelist can move to target");
                    continue;
                }

                Outcome destination = combined(
                        bot, fixture,
                        Decision.deployDestination(fixture));
                if (!String.valueOf(GATE_ID).equals(destination.actionId)) {
                    failures.add(label(bot, family)
                            + " hand deploy child did not choose exact target: "
                            + destination);
                    continue;
                }

                Decision parent = Decision
                        .deployParentWithObjectiveLocationDistraction(
                                fixture);
                List<Outcome> candidates = deployAdapter(
                        bot, fixture, parent);
                Outcome winner = combined(bot, fixture, parent);
                if (!"deploy-duelist".equals(winner.actionId)) {
                    failures.add(label(bot, family)
                            + " winner=" + winner
                            + " candidates=" + candidates);
                }
            }
        }
        assertTrue((flipped ? "back" : "front")
                        + " stranded-copy failures:\n"
                        + String.join("\n", failures),
                failures.isEmpty());
    }

    private static Outcome combined(
            Bot bot, Fixture fixture, Decision decision) {
        if (bot == Bot.RANDO) {
            return outcome(new com.gempukku.swccgo.ai.models.rando
                    .evaluators.CombinedEvaluator()
                    .evaluateDecision(randoContext(fixture, decision)));
        }
        return outcome(new com.gempukku.swccgo.ai.models.chosenone
                .evaluators.CombinedEvaluator()
                .evaluateDecision(chosenContext(fixture, decision)));
    }

    private static List<Outcome> deployAdapter(
            Bot bot, Fixture fixture, Decision decision) {
        if (bot == Bot.RANDO) {
            return new com.gempukku.swccgo.ai.models.rando.evaluators
                    .DeployEvaluator().evaluate(
                            randoContext(fixture, decision)).stream()
                    .map(NabooDuelObjectiveRouteDecisionTest::outcome)
                    .toList();
        }
        return new com.gempukku.swccgo.ai.models.chosenone.evaluators
                .DeployEvaluator().evaluate(
                        chosenContext(fixture, decision)).stream()
                .map(NabooDuelObjectiveRouteDecisionTest::outcome)
                .toList();
    }

    private static List<Outcome> moveAdapter(
            Bot bot, Fixture fixture, Decision decision) {
        if (bot == Bot.RANDO) {
            return new com.gempukku.swccgo.ai.models.rando.evaluators
                    .MoveEvaluator().evaluate(
                            randoContext(fixture, decision)).stream()
                    .map(NabooDuelObjectiveRouteDecisionTest::outcome)
                    .toList();
        }
        return new com.gempukku.swccgo.ai.models.chosenone.evaluators
                .MoveEvaluator().evaluate(
                        chosenContext(fixture, decision)).stream()
                .map(NabooDuelObjectiveRouteDecisionTest::outcome)
                .toList();
    }

    private static List<Outcome> cardSelectionAdapter(
            Bot bot, Fixture fixture, Decision decision) {
        if (bot == Bot.RANDO) {
            return new com.gempukku.swccgo.ai.models.rando.evaluators
                    .CardSelectionEvaluator().evaluate(
                            randoContext(fixture, decision)).stream()
                    .map(NabooDuelObjectiveRouteDecisionTest::outcome)
                    .toList();
        }
        return new com.gempukku.swccgo.ai.models.chosenone.evaluators
                .CardSelectionEvaluator().evaluate(
                        chosenContext(fixture, decision)).stream()
                .map(NabooDuelObjectiveRouteDecisionTest::outcome)
                .toList();
    }

    private static com.gempukku.swccgo.ai.models.rando.evaluators
            .DecisionContext randoContext(
                    Fixture fixture, Decision decision) {
        var context = new com.gempukku.swccgo.ai.models.rando.evaluators
                .DecisionContext(
                        fixture.gameState, PLAYER,
                        decision.type, decision.text,
                        "naboo-duel-route-decision", decision.phase);
        context.setGame(fixture.game);
        context.setSide(fixture.family.side);
        context.setObjectiveAnalyzer(
                (com.gempukku.swccgo.ai.models.rando.strategy
                        .ObjectiveAnalyzer) fixture.analyzer);
        apply(context, decision);
        return context;
    }

    private static com.gempukku.swccgo.ai.models.chosenone.evaluators
            .DecisionContext chosenContext(
                    Fixture fixture, Decision decision) {
        var context = new com.gempukku.swccgo.ai.models.chosenone.evaluators
                .DecisionContext(
                        fixture.gameState, PLAYER,
                        decision.type, decision.text,
                        "naboo-duel-route-decision", decision.phase);
        context.setGame(fixture.game);
        context.setSide(fixture.family.side);
        context.setObjectiveAnalyzer(
                (com.gempukku.swccgo.ai.models.chosenone.strategy
                        .ObjectiveAnalyzer) fixture.analyzer);
        apply(context, decision);
        return context;
    }

    private static void apply(
            com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext context,
            Decision decision) {
        context.setActionIds(decision.actionIds);
        context.setActionTexts(decision.actionTexts);
        context.setCardIds(decision.cardIds);
        context.setBlueprints(decision.blueprints);
        context.setTestingTexts(decision.testingTexts);
        context.setSelectable(decision.cardIds.stream()
                .map(ignored -> true).toList());
        context.setNoPass(decision.noPass);
        context.setMin(decision.min);
        context.setMax(1);
    }

    private static void apply(
            com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext context,
            Decision decision) {
        context.setActionIds(decision.actionIds);
        context.setActionTexts(decision.actionTexts);
        context.setCardIds(decision.cardIds);
        context.setBlueprints(decision.blueprints);
        context.setTestingTexts(decision.testingTexts);
        context.setSelectable(decision.cardIds.stream()
                .map(ignored -> true).toList());
        context.setNoPass(decision.noPass);
        context.setMin(decision.min);
        context.setMax(1);
    }

    private static Outcome outcome(
            com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction action) {
        assertNotNull(action);
        return new Outcome(
                action.getActionId(), action.getScore(),
                List.copyOf(action.getReasoning()),
                action.isHardVetoed(), action.getVetoReason());
    }

    private static Outcome outcome(
            com.gempukku.swccgo.ai.models.chosenone.evaluators.EvaluatedAction action) {
        assertNotNull(action);
        return new Outcome(
                action.getActionId(), action.getScore(),
                List.copyOf(action.getReasoning()),
                action.isHardVetoed(), action.getVetoReason());
    }

    private static Outcome only(
            List<Outcome> outcomes, String actionId) {
        return outcomes.stream()
                .filter(outcome -> actionId.equals(outcome.actionId))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Missing " + actionId + " in " + outcomes));
    }

    private static void assertContains(
            Outcome outcome, String marker) {
        assertTrue("Expected '" + marker + "' in " + outcome.reasoning,
                outcome.reasoning.stream()
                        .anyMatch(reason -> reason.contains(marker)));
    }

    private static void assertParity(List<Outcome> outcomes) {
        assertEquals(2, outcomes.size());
        Outcome first = outcomes.get(0);
        Outcome second = outcomes.get(1);
        assertEquals(first.actionId, second.actionId);
        assertEquals(Float.floatToRawIntBits(first.score),
                Float.floatToRawIntBits(second.score));
        assertEquals(first.reasoning, second.reasoning);
        assertEquals(first.hardVeto, second.hardVeto);
        assertEquals(first.vetoReason, second.vetoReason);
    }

    private static Fixture fixture(
            Bot bot, Family family,
            Integer duelistOriginId,
            float opponentPowerAtGate) {
        return fixture(
                bot, family, true, false,
                duelistOriginId, opponentPowerAtGate);
    }

    private static Fixture frontFixture(
            Bot bot, Family family,
            Integer duelistOriginId,
            float opponentPowerAtGate) {
        return fixture(
                bot, family, false, true,
                duelistOriginId, opponentPowerAtGate);
    }

    private static Fixture fixture(
            Bot bot, Family family,
            boolean flipped,
            boolean genericOpponent,
            Integer duelistOriginId,
            float opponentPowerAtGate) {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard objective = objective(family, flipped);
        PhysicalCard gate = location(family.gate, GATE_ID);
        PhysicalCard sibling = location(family.sibling, SIBLING_ID);
        PhysicalCard staging = location("12_80", STAGING_ID);
        PhysicalCard duelist = character(
                family.duelist, PLAYER,
                duelistOriginId == null ? Zone.HAND : Zone.AT_LOCATION,
                DUELIST_ID);
        PhysicalCard generic = character(
                family.generic, PLAYER, Zone.HAND, GENERIC_ID);
        PhysicalCard target = character(
                genericOpponent
                        ? family.opponentGeneric : family.target,
                OPPONENT, Zone.AT_LOCATION, TARGET_ID);

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(game.getOpponent(PLAYER)).thenReturn(OPPONENT);
        when(gameState.getGame()).thenReturn(game);
        when(gameState.getOpponent(PLAYER)).thenReturn(OPPONENT);
        when(gameState.getOpponent(OPPONENT)).thenReturn(PLAYER);
        when(gameState.getSide(PLAYER)).thenReturn(family.side);
        when(gameState.getSide(OPPONENT)).thenReturn(
                family.side == Side.LIGHT ? Side.DARK : Side.LIGHT);
        when(gameState.getCurrentPlayerId()).thenReturn(PLAYER);
        when(gameState.getPlayersLatestTurnNumber(PLAYER)).thenReturn(3);
        when(gameState.getForcePileSize(PLAYER)).thenReturn(20);
        when(gameState.getReserveDeckSize(PLAYER)).thenReturn(20);
        when(gameState.getUsedPile(PLAYER)).thenReturn(List.of());
        when(gameState.getAllStackedCards()).thenReturn(List.of());
        when(gameState.getLocationsInOrder()).thenReturn(
                List.of(gate, sibling, staging));
        when(gameState.getTopLocations()).thenReturn(
                List.of(gate, sibling, staging));

        PhysicalCard origin = site(
                duelistOriginId, gate, sibling, staging);
        List<PhysicalCard> permanents = new ArrayList<>(
                List.of(objective, gate, sibling, staging, target));
        if (origin != null) permanents.add(duelist);
        when(gameState.getAllPermanentCards()).thenReturn(permanents);
        when(gameState.getHand(PLAYER)).thenReturn(
                origin == null
                        ? List.of(duelist, generic)
                        : List.of(generic));

        for (PhysicalCard card : List.of(
                objective, gate, sibling, staging,
                duelist, generic, target)) {
            when(gameState.findCardById(card.getCardId()))
                    .thenReturn(card);
            when(gameState.findCardByPermanentId(
                    card.getPermanentCardId())).thenReturn(card);
        }
        for (PhysicalCard card : List.of(duelist, generic, target)) {
            Float ability = card.getBlueprint().getAbility();
            when(modifiers.getAbility(gameState, card))
                    .thenReturn(ability);
            when(modifiers.hasAbility(
                    gameState, card, true)).thenReturn(true);
            when(gameState.getAttachedCards(card)).thenReturn(List.of());
            when(gameState.isCardInPlayActive(
                    card, false, false, false, false,
                    false, false, false, false)).thenReturn(true);
            when(gameState.isCardInPlayActive(
                    card, true, false, false, false,
                    false, false, false, false)).thenReturn(true);
        }

        when(target.getAtLocation()).thenReturn(gate);
        when(modifiers.getLocationThatCardIsAt(
                gameState, target)).thenReturn(gate);
        when(modifiers.getLocationThatCardIsPresentAt(
                gameState, target)).thenReturn(gate);
        when(modifiers.canBeTargetedBy(
                eq(gameState), any(PhysicalCard.class),
                eq(objective), anySet())).thenReturn(true);
        when(modifiers.canBeTargetedBy(
                eq(gameState), eq(target),
                eq(objective), anySet())).thenReturn(true);
        when(duelist.getAtLocation()).thenReturn(origin);
        when(modifiers.getLocationThatCardIsAt(
                gameState, duelist)).thenReturn(origin);
        when(modifiers.getLocationThatCardIsPresentAt(
                gameState, duelist)).thenReturn(origin);

        for (PhysicalCard location : List.of(gate, sibling, staging)) {
            when(modifiers.getLocationHere(
                    gameState, location)).thenReturn(location);
            when(modifiers.isBattleground(
                    gameState, location, null)).thenReturn(true);
        }
        when(modifiers.hasIcon(
                gameState, gate, Icon.INTERIOR_SITE)).thenReturn(true);
        when(modifiers.hasIcon(
                gameState, sibling, Icon.INTERIOR_SITE)).thenReturn(true);
        when(modifiers.hasKeyword(
                gameState, gate,
                Keyword.THEED_PALACE_SITE)).thenReturn(true);
        when(modifiers.hasKeyword(
                gameState, sibling,
                Keyword.THEED_PALACE_SITE)).thenReturn(true);

        List<PhysicalCard> gateCards =
                cardsAt(gate, origin, duelist, target);
        List<PhysicalCard> siblingCards =
                cardsAt(sibling, origin, duelist, null);
        List<PhysicalCard> stagingCards =
                cardsAt(staging, origin, duelist, null);
        when(gameState.getCardsAtLocation(gate)).thenReturn(gateCards);
        when(gameState.getCardsAtLocation(sibling)).thenReturn(siblingCards);
        when(gameState.getCardsAtLocation(staging)).thenReturn(stagingCards);

        for (PhysicalCard location : List.of(gate, sibling, staging)) {
            setPower(modifiers, gameState, location, PLAYER, 0.0f);
            setPower(modifiers, gameState, location, OPPONENT, 0.0f);
        }
        setPower(modifiers, gameState, gate,
                OPPONENT, opponentPowerAtGate);
        if (origin != null) {
            Float duelistAbility = duelist.getBlueprint().getAbility();
            setPower(modifiers, gameState, origin, PLAYER,
                    duelist.getBlueprint().getPower());
            when(modifiers.getTotalAbilityAtLocation(
                    gameState, PLAYER, origin)).thenReturn(
                            duelistAbility);
        }

        distance(modifiers, gameState, gate, sibling, 1);
        distance(modifiers, gameState, gate, staging, 1);
        distance(modifiers, gameState, sibling, staging, 1);
        for (PhysicalCard location : List.of(gate, sibling, staging)) {
            when(modifiers.getDistanceBetweenSites(
                    gameState, location, location)).thenReturn(0);
            when(modifiers.getSitesBetween(
                    gameState, location, gate)).thenReturn(List.of());
            when(modifiers.getSitesBetween(
                    gameState, location, sibling)).thenReturn(List.of());
            when(modifiers.getSitesBetween(
                    gameState, location, staging)).thenReturn(List.of());
        }
        when(modifiers.getLandspeed(
                gameState, duelist)).thenReturn(1.0f);
        when(modifiers.getForceAvailableToUse(
                gameState, PLAYER)).thenReturn(20);
        if (origin != null) {
            for (PhysicalCard destination
                    : List.of(gate, sibling, staging)) {
                int requiredLandspeed = origin == destination ? 0 : 1;
                when(modifiers.getLandspeedRequired(
                        gameState, duelist, destination)).thenReturn(
                                requiredLandspeed);
                when(modifiers.getMoveUsingLandspeedCost(
                        gameState, duelist, origin, destination,
                        false, 0.0f)).thenReturn(1.0f);
            }
        }
        setDeployable(modifiers);

        ObjectiveAnalyzer analyzer = bot == Bot.RANDO
                ? new com.gempukku.swccgo.ai.models.rando.strategy
                        .ObjectiveAnalyzer()
                : new com.gempukku.swccgo.ai.models.chosenone.strategy
                        .ObjectiveAnalyzer();
        analyzer.analyze(game, PLAYER, family.side);
        assertEquals(family.title, analyzer.getActivePlaybook().label);
        return new Fixture(
                family, analyzer, game, gameState,
                gate, sibling, staging, duelist, generic, target);
    }

    private static List<PhysicalCard> cardsAt(
            PhysicalCard location, PhysicalCard origin,
            PhysicalCard duelist, PhysicalCard target) {
        List<PhysicalCard> cards = new ArrayList<>();
        if (location == origin) cards.add(duelist);
        if (target != null) cards.add(target);
        return cards;
    }

    private static PhysicalCard addStrandedDuelist(Fixture fixture) {
        PhysicalCard stranded = character(
                fixture.family.duelist, PLAYER,
                Zone.AT_LOCATION, STRANDED_DUELIST_ID);
        GameState gameState = fixture.gameState;
        ModifiersQuerying modifiers = fixture.game.getModifiersQuerying();

        gameState.getAllPermanentCards().add(stranded);
        gameState.getCardsAtLocation(fixture.staging).add(stranded);
        when(gameState.findCardById(STRANDED_DUELIST_ID))
                .thenReturn(stranded);
        when(gameState.findCardByPermanentId(STRANDED_DUELIST_ID))
                .thenReturn(stranded);
        when(stranded.getAtLocation()).thenReturn(fixture.staging);
        when(modifiers.getLocationThatCardIsAt(
                gameState, stranded)).thenReturn(fixture.staging);
        when(modifiers.getLocationThatCardIsPresentAt(
                gameState, stranded)).thenReturn(fixture.staging);
        Float strandedAbility = stranded.getBlueprint().getAbility();
        when(modifiers.getAbility(gameState, stranded)).thenReturn(
                strandedAbility);
        when(modifiers.hasAbility(
                gameState, stranded, true)).thenReturn(true);
        when(gameState.getAttachedCards(stranded)).thenReturn(List.of());
        when(gameState.isCardInPlayActive(
                stranded, false, false, false, false,
                false, false, false, false)).thenReturn(true);
        when(gameState.isCardInPlayActive(
                stranded, true, false, false, false,
                false, false, false, false)).thenReturn(true);
        when(modifiers.getLandspeed(
                gameState, stranded)).thenReturn(0.0f);
        when(modifiers.getLandspeedRequired(
                gameState, stranded, fixture.gate)).thenReturn(1);
        when(modifiers.getLandspeedRequired(
                gameState, stranded, fixture.sibling)).thenReturn(1);
        return stranded;
    }

    private static void setPower(
            ModifiersQuerying modifiers, GameState gameState,
            PhysicalCard location, String playerId, float power) {
        when(modifiers.getTotalPowerAtLocation(
                gameState, location, playerId,
                false, false)).thenReturn(power);
    }

    private static void distance(
            ModifiersQuerying modifiers, GameState gameState,
            PhysicalCard first, PhysicalCard second, int distance) {
        when(modifiers.getDistanceBetweenSites(
                gameState, first, second)).thenReturn(distance);
        when(modifiers.getDistanceBetweenSites(
                gameState, second, first)).thenReturn(distance);
    }

    private static void setDeployable(ModifiersQuerying modifiers) {
        when(modifiers.isDeployableToTarget(
                any(), any(), any(), anyBoolean(), any(),
                anyBoolean(), anyFloat(), any(), any(), any(),
                any(), any(), any(), anyBoolean(), anyFloat()))
                .thenReturn(true);
    }

    private static PhysicalCard objective(
            Family family, boolean flipped) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint front = blueprint(family.front);
        SwccgCardBlueprint back = blueprint(family.back);
        when(card.getOwner()).thenReturn(PLAYER);
        when(card.getZone()).thenReturn(Zone.SIDE_OF_TABLE);
        when(card.getBlueprint()).thenReturn(flipped ? back : front);
        when(card.getOtherSideBlueprint()).thenReturn(
                flipped ? front : back);
        when(card.getBlueprintId(true)).thenReturn(family.front);
        when(card.getBlueprintId(false)).thenReturn(
                flipped ? family.back : family.front);
        when(card.getTitle()).thenReturn(
                (flipped ? back : front).getTitle());
        when(card.getTitles()).thenReturn(
                List.of(front.getTitle(), back.getTitle()));
        when(card.getCardId()).thenReturn(OBJECTIVE_ID);
        when(card.getPermanentCardId()).thenReturn(OBJECTIVE_ID);
        when(card.isFlipped()).thenReturn(flipped);
        return card;
    }

    private static PhysicalCard location(
            String blueprintId, int id) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = blueprint(blueprintId);
        when(card.getOwner()).thenReturn(PLAYER);
        when(card.getZone()).thenReturn(Zone.LOCATIONS);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.getBlueprintId(true)).thenReturn(blueprintId);
        when(card.getBlueprintId(false)).thenReturn(blueprintId);
        when(card.getTitle()).thenReturn(blueprint.getTitle());
        when(card.getTitles()).thenReturn(List.of(blueprint.getTitle()));
        when(card.getPartOfSystem()).thenReturn("Naboo");
        when(card.getCardId()).thenReturn(id);
        when(card.getPermanentCardId()).thenReturn(id);
        when(card.getAdditionalCardIds()).thenReturn(List.of());
        when(card.isBlownAway()).thenReturn(false);
        return card;
    }

    private static PhysicalCard character(
            String blueprintId, String owner,
            Zone zone, int id) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = blueprint(blueprintId);
        when(card.getOwner()).thenReturn(owner);
        when(card.getZone()).thenReturn(zone);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.getBlueprintId(true)).thenReturn(blueprintId);
        when(card.getBlueprintId(false)).thenReturn(blueprintId);
        when(card.getTitle()).thenReturn(blueprint.getTitle());
        when(card.getTitles()).thenReturn(List.of(blueprint.getTitle()));
        when(card.getCardId()).thenReturn(id);
        when(card.getPermanentCardId()).thenReturn(id);
        when(card.getAdditionalCardIds()).thenReturn(List.of());
        when(card.isCaptive()).thenReturn(false);
        when(card.isUndercover()).thenReturn(false);
        return card;
    }

    private static SwccgCardBlueprint blueprint(String id) {
        SwccgCardBlueprint blueprint = CARDS.getSwccgoCardBlueprint(id);
        assertNotNull("Missing real blueprint " + id, blueprint);
        return blueprint;
    }

    private static PhysicalCard site(
            Integer id, PhysicalCard gate,
            PhysicalCard sibling, PhysicalCard staging) {
        if (id == null) return null;
        return switch (id) {
            case GATE_ID -> gate;
            case SIBLING_ID -> sibling;
            case STAGING_ID -> staging;
            default -> throw new IllegalArgumentException(
                    "Unknown fixture site " + id);
        };
    }

    private static String label(Bot bot, Family family) {
        return bot + " " + family.title;
    }

    private enum Bot {
        RANDO,
        CHOSEN_ONE
    }

    private record Family(
            String title, String front, String back, Side side,
            String gate, String sibling,
            String duelist, String target,
            String generic, String opponentGeneric) {
    }

    private record Fixture(
            Family family, ObjectiveAnalyzer analyzer,
            SwccgGame game, GameState gameState,
            PhysicalCard gate, PhysicalCard sibling,
            PhysicalCard staging, PhysicalCard duelist,
            PhysicalCard generic, PhysicalCard target) {
    }

    private record Outcome(
            String actionId, float score,
            List<String> reasoning,
            boolean hardVeto, String vetoReason) {
    }

    private record Decision(
            String type, String text, Phase phase,
            List<String> actionIds,
            List<String> actionTexts,
            List<String> cardIds,
            List<String> blueprints,
            List<String> testingTexts,
            boolean noPass, int min) {

        private static Decision deployParent(Fixture fixture) {
            return new Decision(
                    "CARD_ACTION_CHOICE", "Choose deploy action",
                    Phase.DEPLOY,
                    List.of("deploy-duelist", "deploy-generic", "pass"),
                    List.of(
                            deployText(fixture.duelist),
                            deployText(fixture.generic),
                            "Pass"),
                    List.of(String.valueOf(DUELIST_ID),
                            String.valueOf(GENERIC_ID), ""),
                    List.of(fixture.family.duelist,
                            fixture.family.generic, ""),
                    List.of(fixture.duelist.getTitle(),
                            fixture.generic.getTitle(), "Pass"),
                    false, 0);
        }

        private static Decision
                deployParentWithObjectiveLocationDistraction(
                        Fixture fixture) {
            return new Decision(
                    "CARD_ACTION_CHOICE", "Choose deploy action",
                    Phase.DEPLOY,
                    List.of("deploy-duelist", "deploy-generic", "pass"),
                    List.of(
                            deployText(fixture.duelist),
                            deployText(fixture.generic) + " to "
                                    + fixture.gate.getTitle(),
                            "Pass"),
                    List.of(String.valueOf(DUELIST_ID),
                            String.valueOf(GENERIC_ID), ""),
                    List.of(fixture.family.duelist,
                            fixture.family.generic, ""),
                    List.of(fixture.duelist.getTitle(),
                            fixture.generic.getTitle() + " to "
                                    + fixture.gate.getTitle(),
                            "Pass"),
                    false, 0);
        }

        private static Decision deployDestination(Fixture fixture) {
            return new Decision(
                    "CARD_SELECTION",
                    "Choose where to deploy " + hint(fixture.duelist),
                    Phase.DEPLOY,
                    List.of(), List.of(),
                    List.of(String.valueOf(GATE_ID),
                            String.valueOf(SIBLING_ID)),
                    List.of(fixture.family.gate,
                            fixture.family.sibling),
                    List.of(fixture.gate.getTitle(),
                            fixture.sibling.getTitle()),
                    true, 1);
        }

        private static Decision moveParent(Fixture fixture) {
            return new Decision(
                    "ACTION_CHOICE", "Choose move action",
                    Phase.MOVE,
                    List.of("move-duelist", "pass"),
                    List.of("Move using landspeed", "Pass"),
                    List.of(String.valueOf(DUELIST_ID), ""),
                    List.of(), List.of(), false, 0);
        }

        private static Decision moveDestination(Fixture fixture) {
            return new Decision(
                    "CARD_SELECTION",
                    "Choose where to move " + hint(fixture.duelist)
                            + " using landspeed",
                    Phase.MOVE,
                    List.of(), List.of(),
                    List.of(String.valueOf(GATE_ID),
                            String.valueOf(SIBLING_ID)),
                    List.of(fixture.family.gate,
                            fixture.family.sibling),
                    List.of(fixture.gate.getTitle(),
                            fixture.sibling.getTitle()),
                    true, 1);
        }

        private static String deployText(PhysicalCard card) {
            return "Deploy " + hint(card);
        }

        private static String hint(PhysicalCard card) {
            return "<div class='cardHint' value='"
                    + card.getBlueprintId(true) + "'>"
                    + card.getTitle() + "</div>";
        }
    }
}
