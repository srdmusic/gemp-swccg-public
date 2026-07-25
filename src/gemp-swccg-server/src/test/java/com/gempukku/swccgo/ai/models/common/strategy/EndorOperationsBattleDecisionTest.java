package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.ai.models.common.phase.ObjectiveBattlePolicy;
import com.gempukku.swccgo.ai.models.common.trace.DecisionTrace;
import com.gempukku.swccgo.ai.models.common.trace.TraceSession;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.SpotOverride;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgCardBlueprintLibrary;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Production BattleEvaluator and CombinedEvaluator proof for Endor Operations.
 * Physical cards are mocks backed by the real card-source blueprints.
 */
public class EndorOperationsBattleDecisionTest {
    private static final String PLAYER = "dark";
    private static final String OPPONENT = "light";

    private static final int OBJECTIVE_ID = 90;
    private static final int ENDOR_ID = 101;
    private static final int BUNKER_ID = 102;
    private static final int PLATFORM_ID = 103;
    private static final int CANTINA_ID = 104;
    private static final int FOREST_ID = 105;
    private static final int OMINOUS_ID = 201;
    private static final int DEACTIVATE_ID = 202;
    private static final int OUR_CHARACTER_ID = 301;
    private static final int THEIR_CHARACTER_ID = 302;
    private static final int DYER_ID = 303;
    private static final int BUNKER_SCOUT_ID = 304;
    private static final int PLATFORM_SCOUT_ID = 305;
    private static final int ROUTE_ACTOR_ID = 306;
    private static final int AT_ST_PILOT_ID = 307;

    private static final String OBJECTIVE_BP = "8_167";
    private static final String ENDOR_BP = "8_157";
    private static final String BUNKER_BP = "8_160";
    private static final String PLATFORM_BP = "8_166";
    private static final String FOREST_BP = "8_164";
    private static final String CANTINA_BP = "1_128";
    private static final String OMINOUS_BP = "8_127";
    private static final String DEACTIVATE_BP = "8_43";
    private static final String OUR_CHARACTER_BP = "1_171";
    private static final String THEIR_CHARACTER_BP = "1_004";
    private static final String DYER_BP = "8_93";
    private static final String SECRET_BASE_CLASSIC_BP = "8_124";
    private static final String BIKER_SCOUT_BP = "8_92";
    private static final String UNIQUE_BIKER_SCOUT_BP = "8_95";
    private static final String AT_ST_BP = "8_172";
    private static final String AT_ST_PILOT_BP = "8_91";

    private static final String REQUIRED_ENABLER_RULE =
            ObjectiveBattlePolicy.REQUIRED_CARD_CONTROL_ENABLER_RULE_ID;
    private static final String HARD_LOSS_RULE =
            ObjectiveBattlePolicy.HARD_LOSS_LOCATION_RULE_ID;
    private static final String REQUIRED_RETENTION_RULE =
            ObjectiveBattlePolicy.REQUIRED_CARD_RETENTION_RULE_ID;

    private static final SwccgCardBlueprintLibrary CARDS =
            new SwccgCardBlueprintLibrary();

    @Test
    public void classicOminousOpponentControlBattleCarriesEnablerAndBeatsPass() {
        List<Outcome> directOpen = new ArrayList<>();
        List<Outcome> combinedOpen = new ArrayList<>();
        List<Outcome> directClosed = new ArrayList<>();

        for (Bot bot : Bot.values()) {
            Fixture open = fixture(bot, false);
            open.addReserve(card(
                    OMINOUS_BP, OMINOUS_ID,
                    Zone.RESERVE_DECK, PLAYER));
            open.prepareSafeBattle(open.bunker());
            open.setOpponentControls(open.bunker(), true);

            assertTrue(open.analyzer()
                    .isMissingRequiredCardDeployEnablerAt(
                            open.game(), PLAYER, open.bunker()));

            TracedOutcome openBattle = tracedBattle(
                    bot, open, Decision.at(open.bunker()));
            assertFalse(openBattle.outcome().hardVeto());
            assertHasTypedRule(
                    openBattle.trace(), "battle",
                    REQUIRED_ENABLER_RULE);
            directOpen.add(openBattle.outcome());

            TracedOutcome openCombined = tracedCombined(
                    bot, open, Decision.at(open.bunker()));
            assertEquals("battle", openCombined.outcome().actionId());
            assertHasTypedRule(
                    openCombined.trace(), "battle",
                    REQUIRED_ENABLER_RULE);
            combinedOpen.add(openCombined.outcome());

            Fixture closed = fixture(bot, false);
            closed.addReserve(card(
                    OMINOUS_BP, OMINOUS_ID,
                    Zone.RESERVE_DECK, PLAYER));
            closed.prepareSafeBattle(closed.bunker());

            assertFalse(closed.analyzer()
                    .isMissingRequiredCardDeployEnablerAt(
                            closed.game(), PLAYER, closed.bunker()));

            TracedOutcome closedBattle = tracedBattle(
                    bot, closed, Decision.at(closed.bunker()));
            assertNoTypedRule(
                    closedBattle.trace(), "battle",
                    REQUIRED_ENABLER_RULE);
            directClosed.add(closedBattle.outcome());

            TracedOutcome closedCombined = tracedCombined(
                    bot, closed, Decision.at(closed.bunker()));
            assertNoTypedRule(
                    closedCombined.trace(), "battle",
                    REQUIRED_ENABLER_RULE);

            assertEquals(
                    ObjectiveBattlePolicy
                            .REQUIRED_CARD_CONTROL_ENABLER_BONUS,
                    openBattle.outcome().score()
                            - closedBattle.outcome().score(),
                    0.0f);
        }

        assertParity(directOpen);
        assertParity(combinedOpen);
        assertParity(directClosed);
    }

    @Test
    public void classicSecretBaseBattleRequiresActorAlreadyAtControlWithSite() {
        for (String routeActorBlueprint : List.of(
                UNIQUE_BIKER_SCOUT_BP, AT_ST_BP)) {
            List<Outcome> directOpen =
                    new ArrayList<>();
            List<Outcome> combinedOpen =
                    new ArrayList<>();
            List<Outcome> directClosed =
                    new ArrayList<>();

            for (Bot bot : Bot.values()) {
                Fixture fixture = fixture(bot, false);
                fixture.addReserve(card(
                        SECRET_BASE_CLASSIC_BP,
                        OMINOUS_ID,
                        Zone.RESERVE_DECK, PLAYER));
                fixture.qualifyClassicSite(
                        fixture.bunker(),
                        BUNKER_SCOUT_ID);
                fixture.qualifyClassicSite(
                        fixture.platform(),
                        PLATFORM_SCOUT_ID);
                fixture.prepareSafeBattle(
                        fixture.forest());
                PhysicalCard routeActor = card(
                        routeActorBlueprint,
                        ROUTE_ACTOR_ID,
                        Zone.AT_LOCATION, PLAYER);
                fixture.place(
                        routeActor, fixture.forest());
                if (AT_ST_BP.equals(
                        routeActorBlueprint)) {
                    when(fixture.modifiers().isPiloted(
                            fixture.gameState(),
                            routeActor, false))
                            .thenReturn(true);
                }

                assertTrue(routeActorBlueprint,
                        fixture.analyzer()
                                .isMissingRequiredCardDeployEnablerAt(
                                        fixture.game(), PLAYER,
                                        fixture.forest()));
                TracedOutcome openBattle =
                        tracedBattle(
                                bot, fixture,
                                Decision.atWithPass(
                                        fixture.forest()));
                assertHasTypedRule(
                        openBattle.trace(), "battle",
                        REQUIRED_ENABLER_RULE);
                directOpen.add(
                        openBattle.outcome());

                TracedOutcome openCombined =
                        tracedCombined(
                                bot, fixture,
                                Decision.atWithPass(
                                        fixture.forest()));
                assertEquals("battle",
                        openCombined.outcome()
                                .actionId());
                assertHasTypedRule(
                        openCombined.trace(), "battle",
                        REQUIRED_ENABLER_RULE);
                combinedOpen.add(
                        openCombined.outcome());

                fixture.setSelfControls(
                        fixture.forest(), true);
                assertFalse(routeActorBlueprint,
                        fixture.analyzer()
                                .isMissingRequiredCardDeployEnablerAt(
                                        fixture.game(), PLAYER,
                                        fixture.forest()));
                TracedOutcome closedBattle =
                        tracedBattle(
                                bot, fixture,
                                Decision.atWithPass(
                                        fixture.forest()));
                assertNoTypedRule(
                        closedBattle.trace(), "battle",
                        REQUIRED_ENABLER_RULE);
                directClosed.add(
                        closedBattle.outcome());
            }

            assertParity(directOpen);
            assertParity(combinedOpen);
            assertParity(directClosed);
        }
    }

    @Test
    public void classicSecretBaseBunkerBattleRejectsUnusableAtStButAcceptsScout() {
        List<Outcome> scoutBattles =
                new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture unusable = fixture(bot, false);
            unusable.addReserve(card(
                    SECRET_BASE_CLASSIC_BP,
                    OMINOUS_ID,
                    Zone.RESERVE_DECK, PLAYER));
            unusable.qualifyClassicSite(
                    unusable.platform(),
                    PLATFORM_SCOUT_ID);
            unusable.qualifyClassicSite(
                    unusable.forest(),
                    BUNKER_SCOUT_ID);
            unusable.prepareSafeBattle(
                    unusable.bunker());
            PhysicalCard unpilotedAtSt = card(
                    AT_ST_BP, ROUTE_ACTOR_ID,
                    Zone.AT_LOCATION, PLAYER);
            unusable.place(
                    unpilotedAtSt, unusable.bunker());
            unusable.addHand(card(
                    AT_ST_PILOT_BP, AT_ST_PILOT_ID,
                    Zone.HAND, PLAYER));
            when(unusable.modifiers().isPiloted(
                    unusable.gameState(),
                    unpilotedAtSt, false))
                    .thenReturn(false);

            assertFalse(unusable.analyzer()
                    .isMissingRequiredCardDeployEnablerAt(
                            unusable.game(), PLAYER,
                            unusable.bunker()));
            TracedOutcome unusableBattle =
                    tracedBattle(
                            bot, unusable,
                            Decision.atWithPass(
                                    unusable.bunker()));
            assertNoTypedRule(
                    unusableBattle.trace(), "battle",
                    REQUIRED_ENABLER_RULE);

            Fixture scout = fixture(bot, false);
            scout.addReserve(card(
                    SECRET_BASE_CLASSIC_BP,
                    OMINOUS_ID,
                    Zone.RESERVE_DECK, PLAYER));
            scout.qualifyClassicSite(
                    scout.platform(),
                    PLATFORM_SCOUT_ID);
            scout.qualifyClassicSite(
                    scout.forest(),
                    BUNKER_SCOUT_ID);
            scout.prepareSafeBattle(
                    scout.bunker());
            scout.place(card(
                    UNIQUE_BIKER_SCOUT_BP,
                    ROUTE_ACTOR_ID,
                    Zone.AT_LOCATION, PLAYER),
                    scout.bunker());

            assertTrue(scout.analyzer()
                    .isMissingRequiredCardDeployEnablerAt(
                            scout.game(), PLAYER,
                            scout.bunker()));
            TracedOutcome scoutBattle =
                    tracedBattle(
                            bot, scout,
                            Decision.atWithPass(
                                    scout.bunker()));
            assertHasTypedRule(
                    scoutBattle.trace(), "battle",
                    REQUIRED_ENABLER_RULE);
            scoutBattles.add(
                    scoutBattle.outcome());
        }
        assertParity(scoutBattles);
    }

    @Test
    public void activeDeactivateThreatAtBunkerCarriesHardLossDefenseOnBothSides() {
        for (boolean flipped : List.of(false, true)) {
            List<Outcome> directThreat = new ArrayList<>();
            List<Outcome> combinedThreat = new ArrayList<>();
            List<Outcome> directNoThreat = new ArrayList<>();

            for (Bot bot : Bot.values()) {
                Fixture fixture = fixture(bot, flipped);
                fixture.prepareSafeBattle(fixture.bunker());

                assertTrue(fixture.analyzer()
                        .isObjectiveHardLossLocation(
                                fixture.game(), PLAYER,
                                fixture.bunker()));
                assertFalse(fixture.analyzer()
                        .isObjectiveHardLossDefenseLocation(
                                fixture.game(), PLAYER,
                                fixture.bunker()));

                TracedOutcome noThreat = tracedBattle(
                        bot, fixture,
                        Decision.at(fixture.bunker()));
                assertNoTypedRule(
                        noThreat.trace(), "battle",
                        HARD_LOSS_RULE);
                directNoThreat.add(noThreat.outcome());

                fixture.addDeactivateThreatAtBunker();
                assertTrue(fixture.analyzer()
                        .isObjectiveHardLossDefenseLocation(
                                fixture.game(), PLAYER,
                                fixture.bunker()));

                TracedOutcome threatBattle = tracedBattle(
                        bot, fixture,
                        Decision.at(fixture.bunker()));
                assertFalse(threatBattle.outcome().hardVeto());
                assertHasTypedRule(
                        threatBattle.trace(), "battle",
                        HARD_LOSS_RULE);
                directThreat.add(threatBattle.outcome());

                TracedOutcome threatCombined = tracedCombined(
                        bot, fixture,
                        Decision.at(fixture.bunker()));
                assertEquals(
                        "battle",
                        threatCombined.outcome().actionId());
                assertHasTypedRule(
                        threatCombined.trace(), "battle",
                        HARD_LOSS_RULE);
                combinedThreat.add(threatCombined.outcome());

                assertEquals(
                        ObjectiveBattlePolicy
                                .HARD_LOSS_LOCATION_BONUS,
                        threatBattle.outcome().score()
                                - noThreat.outcome().score(),
                        0.0f);
            }

            assertParity(directThreat);
            assertParity(combinedThreat);
            assertParity(directNoThreat);
        }
    }

    @Test
    public void hardLossDefenseClosesAwayFromBunkerAndOutsideEndor() {
        for (boolean flipped : List.of(false, true)) {
            for (Bot bot : Bot.values()) {
                Fixture endorOther = fixture(bot, flipped);
                endorOther.addDeactivateThreatAtBunker();
                endorOther.prepareSafeBattle(
                        endorOther.platform());

                assertTrue(endorOther.analyzer()
                        .isObjectiveHardLossLocation(
                                endorOther.game(), PLAYER,
                                endorOther.platform()));
                assertFalse(endorOther.analyzer()
                        .isObjectiveHardLossDefenseLocation(
                                endorOther.game(), PLAYER,
                                endorOther.platform()));
                TracedOutcome endorOtherBattle = tracedBattle(
                        bot, endorOther,
                        Decision.at(endorOther.platform()));
                assertNoTypedRule(
                        endorOtherBattle.trace(), "battle",
                        HARD_LOSS_RULE);

                Fixture outsideEndor = fixture(bot, flipped);
                outsideEndor.addDeactivateThreatAtBunker();
                outsideEndor.prepareSafeBattle(
                        outsideEndor.cantina());

                assertFalse(outsideEndor.analyzer()
                        .isObjectiveHardLossLocation(
                                outsideEndor.game(), PLAYER,
                                outsideEndor.cantina()));
                assertFalse(outsideEndor.analyzer()
                        .isObjectiveHardLossDefenseLocation(
                                outsideEndor.game(), PLAYER,
                                outsideEndor.cantina()));
                TracedOutcome outsideBattle = tracedBattle(
                        bot, outsideEndor,
                        Decision.at(outsideEndor.cantina()));
                assertNoTypedRule(
                        outsideBattle.trace(), "battle",
                        HARD_LOSS_RULE);
            }
        }
    }

    @Test
    public void urgentRumorsRetentionBattleBeatsPassOnBothSidesAndPrintings() {
        for (boolean flipped : List.of(false, true)) {
            for (String rumorsBlueprint : List.of(
                    "8_127", "601_261")) {
                List<Outcome> directUrgent = new ArrayList<>();
                List<Outcome> combinedUrgent = new ArrayList<>();
                List<Outcome> directClosed = new ArrayList<>();
                List<Outcome> combinedClosed = new ArrayList<>();

                for (Bot bot : Bot.values()) {
                    Fixture urgent = fixture(bot, flipped);
                    PhysicalCard rumors = card(
                            rumorsBlueprint, OMINOUS_ID,
                            Zone.SIDE_OF_TABLE, PLAYER);
                    urgent.addActivePermanent(rumors);
                    when(urgent.modifiers().mayNotBeCanceled(
                            urgent.gameState(), rumors))
                            .thenReturn(false);
                    urgent.setOpponentControls(
                            urgent.bunker(), true);
                    urgent.setOpponentControls(
                            urgent.platform(), true);
                    urgent.prepareMarginalBattle(
                            urgent.forest());

                    assertTrue(urgent.analyzer()
                            .isRequiredCardRetentionBattleLocation(
                                    urgent.game(), PLAYER,
                                    urgent.forest()));
                    TracedOutcome urgentBattle = tracedBattle(
                            bot, urgent,
                            Decision.atWithPass(
                                    urgent.forest()));
                    assertHasTypedRule(
                            urgentBattle.trace(), "battle",
                            REQUIRED_RETENTION_RULE);
                    directUrgent.add(
                            urgentBattle.outcome());

                    TracedOutcome urgentCombined =
                            tracedCombined(
                                    bot, urgent,
                                    Decision.atWithPass(
                                            urgent.forest()));
                    assertEquals("battle",
                            urgentCombined.outcome()
                                    .actionId());
                    assertHasTypedRule(
                            urgentCombined.trace(), "battle",
                            REQUIRED_RETENTION_RULE);
                    combinedUrgent.add(
                            urgentCombined.outcome());

                    Fixture closed = fixture(bot, flipped);
                    PhysicalCard closedRumors = card(
                            rumorsBlueprint, OMINOUS_ID,
                            Zone.SIDE_OF_TABLE, PLAYER);
                    closed.addActivePermanent(closedRumors);
                    when(closed.modifiers().mayNotBeCanceled(
                            closed.gameState(), closedRumors))
                            .thenReturn(false);
                    closed.setOpponentControls(
                            closed.bunker(), true);
                    closed.prepareMarginalBattle(
                            closed.forest());

                    assertFalse(closed.analyzer()
                            .isRequiredCardRetentionBattleLocation(
                                    closed.game(), PLAYER,
                                    closed.forest()));
                    TracedOutcome closedBattle = tracedBattle(
                            bot, closed,
                            Decision.atWithPass(
                                    closed.forest()));
                    assertNoTypedRule(
                            closedBattle.trace(), "battle",
                            REQUIRED_RETENTION_RULE);
                    directClosed.add(
                            closedBattle.outcome());

                    TracedOutcome closedCombined =
                            tracedCombined(
                                    bot, closed,
                                    Decision.atWithPass(
                                            closed.forest()));
                    assertNoTypedRule(
                            closedCombined.trace(), "battle",
                            REQUIRED_RETENTION_RULE);
                    combinedClosed.add(
                            closedCombined.outcome());

                    assertEquals(
                            ObjectiveBattlePolicy
                                    .REQUIRED_CARD_RETENTION_BONUS,
                            urgentBattle.outcome().score()
                                    - closedBattle.outcome()
                                            .score(),
                            0.0f);
                }

                assertParity(directUrgent);
                assertParity(combinedUrgent);
                assertParity(directClosed);
                assertParity(combinedClosed);
            }
        }
    }

    @Test
    public void battleAtSoleDyerClosesWhenCancellationIsNotOneStepAway() {
        for (boolean flipped : List.of(false, true)) {
            List<Outcome> directUrgent =
                    new ArrayList<>();
            List<Outcome> combinedUrgent =
                    new ArrayList<>();
            List<Outcome> directEarly =
                    new ArrayList<>();

            for (Bot bot : Bot.values()) {
                Fixture urgent = fixture(bot, flipped);
                PhysicalCard urgentRumors = card(
                        OMINOUS_BP, OMINOUS_ID,
                        Zone.SIDE_OF_TABLE, PLAYER);
                urgent.addActivePermanent(
                        urgentRumors);
                when(urgent.modifiers()
                        .mayNotBeCanceled(
                                urgent.gameState(),
                                urgentRumors))
                        .thenReturn(true);
                urgent.setOpponentControls(
                        urgent.bunker(), true);
                urgent.setOpponentControls(
                        urgent.platform(), true);
                urgent.prepareMarginalBattle(
                        urgent.forest());
                urgent.place(card(
                        DYER_BP, DYER_ID,
                        Zone.AT_LOCATION, PLAYER),
                        urgent.forest());

                assertTrue(urgent.analyzer()
                        .isRequiredCardCancelPreventerDefenseLocation(
                                urgent.game(), PLAYER,
                                urgent.forest()));
                TracedOutcome urgentBattle =
                        tracedBattle(
                                bot, urgent,
                                Decision.atWithPass(
                                        urgent.forest()));
                assertHasTypedRule(
                        urgentBattle.trace(), "battle",
                        REQUIRED_RETENTION_RULE);
                directUrgent.add(
                        urgentBattle.outcome());

                TracedOutcome urgentCombined =
                        tracedCombined(
                                bot, urgent,
                                Decision.atWithPass(
                                        urgent.forest()));
                assertEquals("battle",
                        urgentCombined.outcome()
                                .actionId());
                assertHasTypedRule(
                        urgentCombined.trace(), "battle",
                        REQUIRED_RETENTION_RULE);
                combinedUrgent.add(
                        urgentCombined.outcome());

                Fixture early = fixture(bot, flipped);
                PhysicalCard earlyRumors = card(
                        OMINOUS_BP, OMINOUS_ID,
                        Zone.SIDE_OF_TABLE, PLAYER);
                early.addActivePermanent(earlyRumors);
                when(early.modifiers()
                        .mayNotBeCanceled(
                                early.gameState(),
                                earlyRumors))
                        .thenReturn(true);
                early.setOpponentControls(
                        early.bunker(), true);
                early.prepareMarginalBattle(
                        early.forest());
                early.place(card(
                        DYER_BP, DYER_ID,
                        Zone.AT_LOCATION, PLAYER),
                        early.forest());

                assertFalse(early.analyzer()
                        .isRequiredCardCancelPreventerDefenseLocation(
                                early.game(), PLAYER,
                                early.forest()));
                TracedOutcome earlyBattle =
                        tracedBattle(
                                bot, early,
                                Decision.atWithPass(
                                        early.forest()));
                assertNoTypedRule(
                        earlyBattle.trace(), "battle",
                        REQUIRED_RETENTION_RULE);
                directEarly.add(
                        earlyBattle.outcome());

                assertEquals(
                        ObjectiveBattlePolicy
                                .REQUIRED_CARD_RETENTION_BONUS,
                        urgentBattle.outcome().score()
                                - earlyBattle.outcome()
                                        .score(),
                        0.0f);
            }

            assertParity(directUrgent);
            assertParity(combinedUrgent);
            assertParity(directEarly);
        }
    }

    private static TracedOutcome tracedBattle(
            Bot bot, Fixture fixture, Decision decision) {
        assertTrue(TraceSession.open(
                bot.name(),
                "endor-operations-battle-adapter",
                decision.type(), decision.text(),
                decision.actionIds(), null,
                List.of(
                        "focused evaluator fixture omits "
                                + "the bot-boundary snapshot"),
                false));
        Outcome outcome;
        DecisionTrace trace;
        try {
            outcome = battle(bot, fixture, decision);
        } finally {
            trace = TraceSession.close();
        }
        assertNotNull(trace);
        return new TracedOutcome(outcome, trace);
    }

    private static TracedOutcome tracedCombined(
            Bot bot, Fixture fixture, Decision decision) {
        assertTrue(TraceSession.open(
                bot.name(),
                "endor-operations-battle-combined",
                decision.type(), decision.text(),
                decision.actionIds(), null,
                List.of(
                        "focused evaluator fixture omits "
                                + "the bot-boundary snapshot"),
                false));
        Outcome outcome;
        DecisionTrace trace;
        try {
            outcome = combined(bot, fixture, decision);
        } finally {
            trace = TraceSession.close();
        }
        assertNotNull(trace);
        return new TracedOutcome(outcome, trace);
    }

    private static Outcome battle(
            Bot bot, Fixture fixture, Decision decision) {
        if (bot == Bot.RANDO) {
            return new com.gempukku.swccgo.ai.models.rando.evaluators
                    .BattleEvaluator()
                    .evaluate(randoContext(fixture, decision))
                    .stream()
                    .filter(action -> "battle".equals(
                            action.getActionId()))
                    .findFirst()
                    .map(EndorOperationsBattleDecisionTest::outcome)
                    .orElseThrow(() -> new AssertionError(
                            "Missing Rando battle action"));
        }
        return new com.gempukku.swccgo.ai.models.chosenone.evaluators
                .BattleEvaluator()
                .evaluate(chosenContext(fixture, decision))
                .stream()
                .filter(action -> "battle".equals(
                        action.getActionId()))
                .findFirst()
                .map(EndorOperationsBattleDecisionTest::outcome)
                .orElseThrow(() -> new AssertionError(
                        "Missing Chosen One battle action"));
    }

    private static Outcome combined(
            Bot bot, Fixture fixture, Decision decision) {
        if (bot == Bot.RANDO) {
            return outcome(
                    new com.gempukku.swccgo.ai.models.rando.evaluators
                            .CombinedEvaluator()
                            .evaluateDecision(
                                    randoContext(
                                            fixture, decision)));
        }
        return outcome(
                new com.gempukku.swccgo.ai.models.chosenone.evaluators
                        .CombinedEvaluator()
                        .evaluateDecision(
                                chosenContext(
                                        fixture, decision)));
    }

    private static com.gempukku.swccgo.ai.models.rando.evaluators
            .DecisionContext randoContext(
                    Fixture fixture, Decision decision) {
        var context =
                new com.gempukku.swccgo.ai.models.rando.evaluators
                        .DecisionContext(
                                fixture.gameState(), PLAYER,
                                decision.type(), decision.text(),
                                "endor-operations-battle",
                                decision.phase());
        context.setGame(fixture.game());
        context.setSide(Side.DARK);
        context.setObjectiveAnalyzer(
                (com.gempukku.swccgo.ai.models.rando.strategy
                        .ObjectiveAnalyzer) fixture.analyzer());
        apply(context, decision);
        return context;
    }

    private static com.gempukku.swccgo.ai.models.chosenone.evaluators
            .DecisionContext chosenContext(
                    Fixture fixture, Decision decision) {
        var context =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators
                        .DecisionContext(
                                fixture.gameState(), PLAYER,
                                decision.type(), decision.text(),
                                "endor-operations-battle",
                                decision.phase());
        context.setGame(fixture.game());
        context.setSide(Side.DARK);
        context.setObjectiveAnalyzer(
                (com.gempukku.swccgo.ai.models.chosenone.strategy
                        .ObjectiveAnalyzer) fixture.analyzer());
        apply(context, decision);
        return context;
    }

    private static void apply(
            com.gempukku.swccgo.ai.models.rando.evaluators
                    .DecisionContext context,
            Decision decision) {
        context.setActionIds(decision.actionIds());
        context.setActionTexts(decision.actionTexts());
        context.setCardIds(decision.cardIds());
        context.setSelectable(
                java.util.Collections.nCopies(
                        decision.actionIds().size(), true));
        context.setNoPass(false);
        context.setMin(0);
        context.setMax(1);
    }

    private static void apply(
            com.gempukku.swccgo.ai.models.chosenone.evaluators
                    .DecisionContext context,
            Decision decision) {
        context.setActionIds(decision.actionIds());
        context.setActionTexts(decision.actionTexts());
        context.setCardIds(decision.cardIds());
        context.setSelectable(
                java.util.Collections.nCopies(
                        decision.actionIds().size(), true));
        context.setNoPass(false);
        context.setMin(0);
        context.setMax(1);
    }

    private static Outcome outcome(
            com.gempukku.swccgo.ai.models.rando.evaluators
                    .EvaluatedAction action) {
        assertNotNull(action);
        return new Outcome(
                action.getActionId(), action.getScore(),
                List.copyOf(action.getReasoning()),
                action.isHardVetoed(), action.getVetoReason());
    }

    private static Outcome outcome(
            com.gempukku.swccgo.ai.models.chosenone.evaluators
                    .EvaluatedAction action) {
        assertNotNull(action);
        return new Outcome(
                action.getActionId(), action.getScore(),
                List.copyOf(action.getReasoning()),
                action.isHardVetoed(), action.getVetoReason());
    }

    private static void assertHasTypedRule(
            DecisionTrace trace, String actionId,
            String ruleId) {
        assertTrue(
                "Expected typed rule '" + ruleId + "' for "
                        + actionId + " in "
                        + trace.getOperations(),
                trace.getOperations().stream()
                        .anyMatch(operation ->
                                actionId.equals(
                                        operation.getActionId())
                                && ruleId.equals(
                                        operation.getRuleId().id())));
    }

    private static void assertNoTypedRule(
            DecisionTrace trace, String actionId,
            String ruleId) {
        assertTrue(
                "Did not expect typed rule '" + ruleId
                        + "' for " + actionId + " in "
                        + trace.getOperations(),
                trace.getOperations().stream()
                        .noneMatch(operation ->
                                actionId.equals(
                                        operation.getActionId())
                                && ruleId.equals(
                                        operation.getRuleId().id())));
    }

    private static void assertParity(
            List<Outcome> outcomes) {
        assertEquals(2, outcomes.size());
        Outcome rando = outcomes.get(0);
        Outcome chosen = outcomes.get(1);
        assertEquals(rando.actionId(), chosen.actionId());
        assertEquals(
                Float.floatToRawIntBits(rando.score()),
                Float.floatToRawIntBits(chosen.score()));
        assertEquals(rando.reasoning(), chosen.reasoning());
        assertEquals(rando.hardVeto(), chosen.hardVeto());
        assertEquals(rando.vetoReason(), chosen.vetoReason());
    }

    private static Fixture fixture(
            Bot bot, boolean flipped) {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers =
                mock(ModifiersQuerying.class);

        List<PhysicalCard> hand = new ArrayList<>();
        List<PhysicalCard> reserve = new ArrayList<>();
        List<PhysicalCard> permanents = new ArrayList<>();
        Map<Integer, PhysicalCard> cardsById =
                new LinkedHashMap<>();
        Map<PhysicalCard, List<PhysicalCard>> cardsAt =
                new IdentityHashMap<>();

        PhysicalCard objective = objective(flipped);
        PhysicalCard endor = location(
                ENDOR_BP, ENDOR_ID, "Endor");
        PhysicalCard bunker = location(
                BUNKER_BP, BUNKER_ID, "Endor");
        PhysicalCard platform = location(
                PLATFORM_BP, PLATFORM_ID, "Endor");
        PhysicalCard forest = location(
                FOREST_BP, FOREST_ID, "Endor");
        PhysicalCard cantina = location(
                CANTINA_BP, CANTINA_ID, "Tatooine");
        List<PhysicalCard> locations =
                new ArrayList<>(List.of(
                        endor, bunker, platform,
                        forest, cantina));

        permanents.addAll(List.of(
                objective, endor, bunker, platform, forest,
                cantina));
        for (PhysicalCard card : permanents) {
            cardsById.put(card.getCardId(), card);
        }

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(game.getOpponent(PLAYER)).thenReturn(OPPONENT);
        when(gameState.getGame()).thenReturn(game);
        when(gameState.getOpponent(PLAYER))
                .thenReturn(OPPONENT);
        when(gameState.getOpponent(OPPONENT))
                .thenReturn(PLAYER);
        when(gameState.getSide(PLAYER))
                .thenReturn(Side.DARK);
        when(gameState.getSide(OPPONENT))
                .thenReturn(Side.LIGHT);
        when(gameState.getCurrentPlayerId())
                .thenReturn(PLAYER);
        when(gameState.getPlayersLatestTurnNumber(PLAYER))
                .thenReturn(3);
        when(gameState.getForcePileSize(PLAYER))
                .thenReturn(10);
        when(gameState.getReserveDeckSize(PLAYER))
                .thenReturn(20);
        when(gameState.getPlayerLifeForce(PLAYER))
                .thenReturn(30);
        when(gameState.getPlayerLifeForce(OPPONENT))
                .thenReturn(30);
        when(gameState.getHand(PLAYER)).thenReturn(hand);
        when(gameState.getReserveDeck(PLAYER))
                .thenReturn(reserve);
        when(gameState.getCardPile(
                PLAYER, Zone.RESERVE_DECK))
                .thenReturn(reserve);
        when(gameState.getUsedPile(PLAYER))
                .thenReturn(List.of());
        when(gameState.getLostPile(PLAYER))
                .thenReturn(List.of());
        when(gameState.getAllStackedCards())
                .thenReturn(List.of());
        when(gameState.getAllPermanentCards())
                .thenReturn(permanents);
        when(gameState.getLocationsInOrder())
                .thenReturn(locations);
        when(gameState.getTopLocations())
                .thenReturn(locations);
        when(gameState.findCardById(anyInt()))
                .thenAnswer(invocation ->
                        cardsById.get(invocation.getArgument(
                                0, Integer.class)));
        when(gameState.findCardByPermanentId(anyInt()))
                .thenAnswer(invocation ->
                        cardsById.get(invocation.getArgument(
                                0, Integer.class)));
        when(gameState.getCardsAtLocation(
                any(PhysicalCard.class)))
                .thenAnswer(invocation ->
                        cardsAt.getOrDefault(
                                invocation.getArgument(
                                        0, PhysicalCard.class),
                                List.of()));
        when(gameState.getAttachedCards(
                any(PhysicalCard.class)))
                .thenReturn(List.of());

        when(modifiers.hasKeyword(
                any(GameState.class),
                any(PhysicalCard.class), any()))
                .thenAnswer(invocation -> {
                    PhysicalCard card =
                            invocation.getArgument(1);
                    com.gempukku.swccgo.common.Keyword keyword =
                            invocation.getArgument(2);
                    return card != null
                            && card.getBlueprint() != null
                            && card.getBlueprint()
                                    .hasKeyword(keyword);
                });
        when(modifiers.getForceAvailableToUse(
                gameState, PLAYER)).thenReturn(10);
        for (PhysicalCard card : permanents) {
            setActive(gameState, card);
        }
        for (PhysicalCard location : locations) {
            when(modifiers.getLocationHere(
                    gameState, location))
                    .thenReturn(location);
            when(modifiers.isBattleground(
                    gameState, location, null))
                    .thenReturn(true);
        }

        ObjectiveAnalyzer analyzer = bot == Bot.RANDO
                ? new com.gempukku.swccgo.ai.models.rando.strategy
                        .ObjectiveAnalyzer()
                : new com.gempukku.swccgo.ai.models.chosenone.strategy
                        .ObjectiveAnalyzer();
        analyzer.analyze(game, PLAYER, Side.DARK);
        assertEquals(
                "Endor Operations",
                analyzer.getActivePlaybook().label);

        return new Fixture(
                analyzer, game, gameState, modifiers,
                hand, reserve, permanents, cardsById,
                cardsAt, bunker, platform, forest, cantina);
    }

    private static void setActive(
            GameState gameState, PhysicalCard card) {
        when(gameState.isCardInPlayActive(
                card, false, false, false, false,
                false, false, false, false))
                .thenReturn(true);
        when(gameState.isCardInPlayActive(
                card, true, false, false, false,
                false, false, false, false))
                .thenReturn(true);
        when(gameState.isCardInPlayActive(
                card, false, true, false, false,
                false, false, false, false))
                .thenReturn(true);
    }

    private static PhysicalCard objective(
            boolean flipped) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint front =
                blueprint(OBJECTIVE_BP);
        SwccgCardBlueprint back =
                blueprint(OBJECTIVE_BP + "_BACK");
        when(card.getOwner()).thenReturn(PLAYER);
        when(card.getZone()).thenReturn(Zone.SIDE_OF_TABLE);
        when(card.getBlueprint())
                .thenReturn(flipped ? back : front);
        when(card.getOtherSideBlueprint())
                .thenReturn(flipped ? front : back);
        when(card.getBlueprintId(true))
                .thenReturn(OBJECTIVE_BP);
        when(card.getBlueprintId(false))
                .thenReturn(flipped
                        ? OBJECTIVE_BP + "_BACK"
                        : OBJECTIVE_BP);
        when(card.getTitle())
                .thenReturn((flipped ? back : front)
                        .getTitle());
        when(card.getTitles()).thenReturn(List.of(
                (flipped ? back : front).getTitle()));
        when(card.getPermanentCardId())
                .thenReturn(OBJECTIVE_ID);
        when(card.getCardId()).thenReturn(OBJECTIVE_ID);
        when(card.getAdditionalCardIds())
                .thenReturn(List.of());
        when(card.isFlipped()).thenReturn(flipped);
        return card;
    }

    private static PhysicalCard location(
            String blueprintId, int cardId,
            String partOfSystem) {
        PhysicalCard card = card(
                blueprintId, cardId,
                Zone.LOCATIONS, PLAYER);
        when(card.getPartOfSystem())
                .thenReturn(partOfSystem);
        return card;
    }

    private static PhysicalCard card(
            String blueprintId, int cardId,
            Zone zone, String owner) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint =
                blueprint(blueprintId);
        when(card.getOwner()).thenReturn(owner);
        when(card.getZone()).thenReturn(zone);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.getBlueprintId(true))
                .thenReturn(blueprintId);
        when(card.getBlueprintId(false))
                .thenReturn(blueprintId);
        when(card.getTitle())
                .thenReturn(blueprint.getTitle());
        when(card.getTitles()).thenReturn(
                List.of(blueprint.getTitle()));
        when(card.getPermanentCardId())
                .thenReturn(cardId);
        when(card.getCardId()).thenReturn(cardId);
        when(card.getAdditionalCardIds())
                .thenReturn(List.of());
        when(card.isBlownAway()).thenReturn(false);
        when(card.isCaptive()).thenReturn(false);
        when(card.isUndercover()).thenReturn(false);
        return card;
    }

    private static SwccgCardBlueprint blueprint(
            String id) {
        SwccgCardBlueprint blueprint =
                CARDS.getSwccgoCardBlueprint(id);
        assertNotNull(
                "Missing real blueprint " + id,
                blueprint);
        return blueprint;
    }

    private enum Bot {
        RANDO,
        CHOSEN_ONE
    }

    private record Outcome(
            String actionId,
            float score,
            List<String> reasoning,
            boolean hardVeto,
            String vetoReason) {
    }

    private record TracedOutcome(
            Outcome outcome,
            DecisionTrace trace) {
    }

    private record Decision(
            String type,
            String text,
            Phase phase,
            List<String> actionIds,
            List<String> actionTexts,
            List<String> cardIds) {

        private static Decision at(
                PhysicalCard location) {
            return new Decision(
                    "CARD_ACTION_CHOICE",
                    "Choose battle action",
                    Phase.BATTLE,
                    List.of("battle"),
                    List.of("Initiate battle"),
                    List.of(String.valueOf(
                            location.getCardId())));
        }

        private static Decision atWithPass(
                PhysicalCard location) {
            return new Decision(
                    "CARD_ACTION_CHOICE",
                    "Choose battle action",
                    Phase.BATTLE,
                    List.of("battle", "pass"),
                    List.of("Initiate battle", "Pass"),
                    List.of(
                            String.valueOf(
                                    location.getCardId()),
                            ""));
        }
    }

    private record Fixture(
            ObjectiveAnalyzer analyzer,
            SwccgGame game,
            GameState gameState,
            ModifiersQuerying modifiers,
            List<PhysicalCard> hand,
            List<PhysicalCard> reserve,
            List<PhysicalCard> permanents,
            Map<Integer, PhysicalCard> cardsById,
            Map<PhysicalCard, List<PhysicalCard>> cardsAt,
            PhysicalCard bunker,
            PhysicalCard platform,
            PhysicalCard forest,
            PhysicalCard cantina) {

        private void addReserve(
                PhysicalCard card) {
            reserve.add(card);
            cardsById.put(card.getCardId(), card);
        }

        private void addHand(
                PhysicalCard card) {
            hand.add(card);
            cardsById.put(card.getCardId(), card);
        }

        private void addActivePermanent(
                PhysicalCard card) {
            permanents.add(card);
            cardsById.put(card.getCardId(), card);
            setActive(gameState, card);
        }

        private void place(
                PhysicalCard card,
                PhysicalCard location) {
            addActivePermanent(card);
            cardsAt.computeIfAbsent(
                    location,
                    ignored -> new ArrayList<>())
                    .add(card);
            when(card.getAtLocation())
                    .thenReturn(location);
            when(modifiers
                    .getLocationThatCardIsPresentAt(
                            gameState, card))
                    .thenReturn(location);
            when(modifiers.getLocationThatCardIsAt(
                    gameState, card))
                    .thenReturn(location);
            when(modifiers.getLocationHere(
                    gameState, card))
                    .thenReturn(location);
        }

        private void qualifyClassicSite(
                PhysicalCard location,
                int scoutId) {
            place(card(
                    BIKER_SCOUT_BP, scoutId,
                    Zone.AT_LOCATION, PLAYER),
                    location);
            setSelfControls(location, true);
        }

        private void setSelfControls(
                PhysicalCard location,
                boolean controls) {
            when(modifiers.controlsLocation(
                    gameState, location,
                    PLAYER, null))
                    .thenReturn(controls);
            when(modifiers.controlsLocation(
                    gameState, location, PLAYER,
                    SpotOverride
                            .INCLUDE_EXCLUDED_FROM_BATTLE))
                    .thenReturn(controls);
        }

        private void prepareSafeBattle(
                PhysicalCard location) {
            PhysicalCard ours = card(
                    OUR_CHARACTER_BP,
                    OUR_CHARACTER_ID,
                    Zone.AT_LOCATION, PLAYER);
            PhysicalCard theirs = card(
                    THEIR_CHARACTER_BP,
                    THEIR_CHARACTER_ID,
                    Zone.AT_LOCATION, OPPONENT);
            place(ours, location);
            place(theirs, location);

            // Every closed safety gate stays live:
            // formation ability 4, deterministic predictor win,
            // effective diff -0.5, reserve 20, no suicide ratio.
            when(modifiers.getTotalPowerAtLocation(
                    gameState, location, PLAYER,
                    false, false)).thenReturn(12.0f);
            when(modifiers.getTotalPowerAtLocation(
                    gameState, location, OPPONENT,
                    false, false)).thenReturn(5.0f);
            when(modifiers.getTotalAbilityAtLocation(
                    gameState, PLAYER, location))
                    .thenReturn(4.0f);
            when(modifiers.getTotalAbilityAtLocation(
                    gameState, OPPONENT, location))
                    .thenReturn(7.0f);
            when(modifiers.controlsLocation(
                    gameState, location, PLAYER,
                    SpotOverride
                            .INCLUDE_EXCLUDED_FROM_BATTLE))
                    .thenReturn(false);
        }

        private void prepareMarginalBattle(
                PhysicalCard location) {
            PhysicalCard ours = card(
                    OUR_CHARACTER_BP,
                    OUR_CHARACTER_ID,
                    Zone.AT_LOCATION, PLAYER);
            PhysicalCard theirs = card(
                    THEIR_CHARACTER_BP,
                    THEIR_CHARACTER_ID,
                    Zone.AT_LOCATION, OPPONENT);
            place(ours, location);
            place(theirs, location);

            when(modifiers.getTotalPowerAtLocation(
                    gameState, location, PLAYER,
                    false, false)).thenReturn(7.0f);
            when(modifiers.getTotalPowerAtLocation(
                    gameState, location, OPPONENT,
                    false, false)).thenReturn(5.0f);
            when(modifiers.getTotalAbilityAtLocation(
                    gameState, PLAYER, location))
                    .thenReturn(4.0f);
            when(modifiers.getTotalAbilityAtLocation(
                    gameState, OPPONENT, location))
                    .thenReturn(4.0f);
            when(modifiers.controlsLocation(
                    gameState, location, PLAYER,
                    SpotOverride
                            .INCLUDE_EXCLUDED_FROM_BATTLE))
                    .thenReturn(false);
        }

        private void setOpponentControls(
                PhysicalCard location,
                boolean controls) {
            when(modifiers.controlsLocation(
                    gameState, location,
                    OPPONENT, null))
                    .thenReturn(controls);
            when(modifiers.controlsLocation(
                    gameState, location, OPPONENT,
                    SpotOverride
                            .INCLUDE_EXCLUDED_FROM_BATTLE))
                    .thenReturn(controls);
        }

        private void addDeactivateThreatAtBunker() {
            PhysicalCard threat = card(
                    DEACTIVATE_BP, DEACTIVATE_ID,
                    Zone.ATTACHED, OPPONENT);
            when(threat.getAttachedTo())
                    .thenReturn(bunker);
            addActivePermanent(threat);
        }
    }
}
