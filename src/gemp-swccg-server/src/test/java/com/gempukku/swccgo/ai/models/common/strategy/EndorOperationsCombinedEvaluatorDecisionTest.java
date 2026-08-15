package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.ai.models.common.phase.BhbmForceDripUrgencyFactsReader;
import com.gempukku.swccgo.ai.models.common.phase.MovePhysicalCardResolver;
import com.gempukku.swccgo.ai.models.common.trace.DecisionTrace;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceSession;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.SpotOverride;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgCardBlueprintLibrary;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;
import com.gempukku.swccgo.logic.decisions.CardActionSelectionDecision;
import com.gempukku.swccgo.logic.decisions.DecisionResultInvalidException;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import com.gempukku.swccgo.logic.timing.Action;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Public production-decision proof for Endor Operations. Physical cards are
 * mocks backed by the real card-source blueprints.
 */
public class EndorOperationsCombinedEvaluatorDecisionTest {
    private static final String PLAYER = "dark";
    private static final String OPPONENT = "light";

    private static final int OBJECTIVE_ID = 90;
    private static final int ENDOR_ID = 101;
    private static final int BUNKER_ID = 102;
    private static final int PLATFORM_ID = 103;
    private static final int FOREST_ID = 104;
    private static final int CANTINA_ID = 105;
    private static final int FOREST_PULL_ID = 106;
    private static final int OMINOUS_ID = 201;
    private static final int SECRET_BASE_ID = 202;
    private static final int DISTRACTOR_ID = 203;
    private static final int DISTRACTOR_SOURCE_ID = 204;
    private static final int BUDGET_DISTRACTOR_ID = 205;
    private static final int BIKER_SCOUT_ID = 301;
    private static final int BUNKER_SCOUT_ID = 302;
    private static final int PLATFORM_SCOUT_ID = 303;
    private static final int FOREST_SCOUT_ID = 304;
    private static final int OPPONENT_CHARACTER_ID = 305;
    private static final int DISPOSABLE_DROID_ID = 306;
    private static final int REACTOR_CORE_ID = 307;
    private static final int DYER_ID = 308;
    private static final int READY_REQUIRED_ID = 309;
    private static final int AT_ST_ID = 310;
    private static final int AT_ST_PILOT_ID = 311;
    private static final int SECOND_REQUIRED_ID = 312;
    private static final int FORCE_LOSS_DISTRACTOR_ID = 313;
    private static final int DEACTIVATE_ID = 314;
    private static final int BACK_DOOR_ID = 315;
    private static final int MOVING_BIKER_SCOUT_ID = 316;
    private static final int ENCLOSED_BIKER_SCOUT_ID = 317;
    private static final int ENCLOSING_AT_ST_ID = 318;
    private static final int NON_ACTOR_CARRIER_ID = 319;
    private static final int CARRIED_BIKER_SCOUT_ID = 320;
    private static final int DUPLICATE_BIKER_SCOUT_ID = 321;
    private static final int ADMIRAL_OZZEL_ID = 322;
    private static final int GENERIC_ENDOR_SHIP_ID = 323;
    private static final int GENERIC_ENDOR_PILOT_ID = 324;
    private static final int GENERIC_DEPLOY_DISTRACTOR_ID = 325;
    private static final int REPLAY_SLAVE_I_ID = 326;
    private static final int REPLAY_BOBA_FETT_ID = 327;

    private static final String OBJECTIVE_BP = "8_167";
    private static final String ENDOR_BP = "8_157";
    private static final String BUNKER_BP = "8_160";
    private static final String PLATFORM_BP = "8_166";
    private static final String FOREST_BP = "8_164";
    private static final String CANTINA_BP = "1_290";
    private static final String OMINOUS_BP = "8_127";
    private static final String OMINOUS_DIRECT_BP = "223_19";
    private static final String OMINOUS_LEGACY_BP = "601_261";
    private static final String SECRET_BASE_CLASSIC_BP = "8_124";
    private static final String SECRET_BASE_V_BP = "207_25";
    private static final String SECRET_BASE_LEGACY_BP = "601_260";
    private static final String DISTRACTOR_BP = "213_16";
    private static final String BIKER_SCOUT_BP = "8_92";
    private static final String OPPONENT_CHARACTER_BP = "1_19";
    private static final String DISPOSABLE_DROID_BP = "1_175";
    private static final String REACTOR_CORE_BP = "9_146";
    private static final String DYER_BP = "8_93";
    private static final String AT_ST_BP = "8_172";
    private static final String AT_ST_PILOT_BP = "8_91";
    private static final String DEACTIVATE_BP = "8_43";
    private static final String BACK_DOOR_BP = "8_159";
    private static final String UNIQUE_BIKER_SCOUT_BP = "8_95";
    private static final String NON_ACTOR_CARRIER_BP = "8_169";
    private static final String ADMIRAL_OZZEL_BP = "3_82";
    private static final String GENERIC_ENDOR_SHIP_BP = "1_306";
    private static final String GENERIC_ENDOR_PILOT_BP = "1_174";
    private static final String REPLAY_SLAVE_I_BP = "201_40";
    private static final String REPLAY_BOBA_FETT_BP = "206_9";

    private static final String REQUIRED_PULL_RULE =
            "PULL.OBJECTIVE.REQUIRED_ON_TABLE_CARD";
    private static final String REQUIRED_PULL_ROUTE_BLOCKED_RULE =
            "PULL.OBJECTIVE.REQUIRED_ON_TABLE_CARD_ROUTE_BLOCKED";
    private static final String REQUIRED_DEPLOY_RULE =
            "DEPLOY.OBJECTIVE.REQUIRED_ON_TABLE_CARD";
    private static final String REQUIRED_ENABLER_DEPLOY_RULE =
            "DEPLOY.OBJECTIVE.REQUIRED_CARD_ENABLER";
    private static final String REQUIRED_BUDGET_RULE =
            "DEPLOY.BUDGET.OBJECTIVE_REQUIRED_CARD_RESERVE";
    private static final String REQUIRED_ENABLER_HOLD_RULE =
            "MOVE.OBJECTIVE.REQUIRED_CARD_ENABLER_HOLD";
    private static final String REQUIRED_RETENTION_MOVE_RULE =
            "MOVE.OBJECTIVE.REQUIRED_CARD_RETENTION_HOLD";
    private static final String REQUIRED_RETENTION_DEPLOY_RULE =
            "DEPLOY.OBJECTIVE.REQUIRED_CARD_RETENTION";
    private static final String REQUIRED_RETENTION_FORFEIT_RULE =
            "BATTLE.OBJECTIVE.REQUIRED_CARD_RETENTION_HOLD";
    private static final String REQUIRED_CARD_INACTIVATION_RULE =
            "DEPLOY.OBJECTIVE.REQUIRED_CARD_INACTIVATION";
    private static final String REQUIRED_FORMATION_HOLD_RULE =
            "MOVE.OBJECTIVE.REQUIRED_CARD_ENABLER_FORMATION_HOLD";
    private static final String REQUIRED_FORMATION_FORFEIT_RULE =
            "BATTLE.OBJECTIVE.FLIP_GATE_FORMATION_HOLD";
    private static final String FORCE_LOSS_OBJECTIVE_RULE =
            "V21-objective";
    private static final String HARD_LOSS_DEFENSE_RULE =
            "DEPLOY.OBJECTIVE.HARD_LOSS_DEFENSE";
    private static final String REQUIRED_ENABLER_MOVE_START_RULE =
            "MOVE.OBJECTIVE.REQUIRED_CARD_ENABLER_START";
    private static final String REQUIRED_ENABLER_MOVE_DESTINATION_RULE =
            "MOVE.OBJECTIVE.REQUIRED_CARD_ENABLER_DESTINATION";
    private static final String REQUIRED_ENABLER_EMBARK_START_RULE =
            "MOVE.OBJECTIVE.REQUIRED_CARD_ENABLER_EMBARK_START";
    private static final String REQUIRED_ENABLER_EMBARK_TARGET_RULE =
            "MOVE.OBJECTIVE.REQUIRED_CARD_ENABLER_EMBARK_TARGET";
    private static final String COUNTED_FORMATION_HOLD_RULE =
            "MOVE.OBJECTIVE.COUNTED_FORMATION_HOLD";
    private static final String
            HUNT_DOWN_RUNTIME_ACTOR_DESTINATION_HOLD =
            "OBJECTIVE.HUNT_DOWN.RUNTIME_ACTOR_DESTINATION_HOLD";

    private static final SwccgCardBlueprintLibrary CARDS =
            new SwccgCardBlueprintLibrary();

    @Test
    public void boundedBunkerPreferenceDoesNotOverrideProductivePlatform() {
        List<Outcome> winners = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot);
            fixture.addReserve(card(
                    SECRET_BASE_V_BP, SECRET_BASE_ID,
                    Zone.RESERVE_DECK, PLAYER));
            PhysicalCard ozzel = card(
                    ADMIRAL_OZZEL_BP, ADMIRAL_OZZEL_ID,
                    Zone.HAND, PLAYER);
            fixture.addHand(ozzel);
            when(fixture.modifiers().hasAbility(
                    fixture.gameState(), ozzel, true))
                    .thenReturn(true);

            TracedOutcome winner = tracedCombined(
                    bot, fixture,
                    Decision.deployCandidate(
                            ozzel, fixture.bunker(),
                            fixture.platform()));
            assertEquals(String.valueOf(PLATFORM_ID),
                    winner.outcome().actionId());
            assertHasTypedRuleDelta(
                    winner.trace(), String.valueOf(BUNKER_ID),
                    "V22-objective-location",
                    TraceDomainId.OBJECTIVE_INTENT,
                    300.0f);
            assertHasTypedRuleDelta(
                    winner.trace(), String.valueOf(PLATFORM_ID),
                    "V22-objective-location",
                    TraceDomainId.OBJECTIVE_INTENT,
                    300.0f);
            winners.add(winner.outcome());
        }
        assertParity(winners);
    }

    @Test
    public void bunkerObligationWinsTopLevelAndReservesOzzelFromPiloting() {
        List<Outcome> deployWinners = new ArrayList<>();
        List<Outcome> pilotWinners = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot);
            PhysicalCard ozzel = card(
                    ADMIRAL_OZZEL_BP, ADMIRAL_OZZEL_ID,
                    Zone.HAND, PLAYER);
            PhysicalCard ship = card(
                    REPLAY_SLAVE_I_BP, REPLAY_SLAVE_I_ID,
                    Zone.HAND, PLAYER);
            PhysicalCard alternatePilot = card(
                    REPLAY_BOBA_FETT_BP, REPLAY_BOBA_FETT_ID,
                    Zone.HAND, PLAYER);
            fixture.addHand(ozzel);
            fixture.addHand(ship);
            fixture.addHand(alternatePilot);

            TracedOutcome deploy =
                    tracedCombinedWithBunkerGarrisonPlan(
                            bot, fixture,
                            Decision.topLevelDeploy(ship, ozzel),
                            ozzel);
            assertEquals("deploy-" + ADMIRAL_OZZEL_ID,
                    deploy.outcome().actionId());
            assertContains(deploy.outcome(),
                    "EOP BUNKER GARRISON");
            deployWinners.add(deploy.outcome());

            TracedOutcome pilot =
                    tracedCombinedWithBunkerGarrisonPlan(
                            bot, fixture,
                            Decision.simultaneousPilotSelection(
                                    ship, ozzel, alternatePilot),
                            ozzel);
            assertEquals(String.valueOf(REPLAY_BOBA_FETT_ID),
                    pilot.outcome().actionId());
            pilotWinners.add(pilot.outcome());
        }
        assertParity(deployWinners);
        assertParity(pilotWinners);
    }

    @Test
    public void readySecretBaseBeatsOzzelAfterBunkerControlIsReady() {
        List<Outcome> winners = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot);
            PhysicalCard ominous = card(
                    OMINOUS_BP, OMINOUS_ID,
                    Zone.SIDE_OF_TABLE, PLAYER);
            PhysicalCard secretBase = card(
                    SECRET_BASE_V_BP, SECRET_BASE_ID,
                    Zone.HAND, PLAYER);
            PhysicalCard ozzel = card(
                    ADMIRAL_OZZEL_BP, ADMIRAL_OZZEL_ID,
                    Zone.HAND, PLAYER);
            fixture.addActivePermanent(ominous);
            fixture.addHand(secretBase);
            fixture.addHand(ozzel);
            setControls(
                    fixture, fixture.bunker(), PLAYER, true);
            when(fixture.modifiers().getDeployCost(
                    fixture.gameState(), secretBase))
                    .thenReturn(0.0f);
            when(fixture.modifiers().getDeployCost(
                    fixture.gameState(), ozzel))
                    .thenReturn(2.0f);

            TracedOutcome winner = tracedCombined(
                    bot, fixture,
                    Decision.topLevelDeploy(
                            ozzel, secretBase));
            assertEquals("deploy-" + SECRET_BASE_ID,
                    winner.outcome().actionId());
            assertContains(
                    winner.outcome(), REQUIRED_DEPLOY_RULE);
            winners.add(winner.outcome());
        }
        assertParity(winners);
    }

    @Test
    public void verifiedGenericEndorCrewPackageBeatsPassAndOffPlanDeploy() {
        List<Outcome> winners = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot, true);
            PhysicalCard ship = card(
                    GENERIC_ENDOR_SHIP_BP, GENERIC_ENDOR_SHIP_ID,
                    Zone.HAND, PLAYER);
            PhysicalCard pilot = card(
                    GENERIC_ENDOR_PILOT_BP, GENERIC_ENDOR_PILOT_ID,
                    Zone.HAND, PLAYER);
            PhysicalCard distractor = card(
                    DISPOSABLE_DROID_BP, GENERIC_DEPLOY_DISTRACTOR_ID,
                    Zone.HAND, PLAYER);
            fixture.addHand(ship);
            fixture.addHand(pilot);
            fixture.addHand(distractor);
            when(fixture.gameState().getForcePileSize(PLAYER))
                    .thenReturn(4);
            when(fixture.modifiers().getForceAvailableToUse(
                    fixture.gameState(), PLAYER)).thenReturn(4);
            when(fixture.modifiers().hasPermanentPilot(
                    fixture.gameState(), ship)).thenReturn(false);

            TracedOutcome winner = tracedCombinedWithVerifiedCrewPlan(
                    bot, fixture,
                    Decision.topLevelDeploy(distractor, ship),
                    ship, pilot);
            assertEquals("deploy-" + GENERIC_ENDOR_SHIP_ID,
                    winner.outcome().actionId());
            assertContains(winner.outcome(),
                    "IN DEPLOYMENT PLAN");
            assertNotContains(winner.outcome(),
                    "VEHICLE/SHIP NEEDS PILOT");
            winners.add(winner.outcome());
        }
        assertParity(winners);
    }

    @Test
    public void objectiveTutorParentAndChildChooseMissingRequiredEffect() {
        List<Outcome> parentResults = new ArrayList<>();
        List<Outcome> childResults = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot);
            PhysicalCard ominous = card(
                    OMINOUS_BP, OMINOUS_ID,
                    Zone.RESERVE_DECK, PLAYER);
            PhysicalCard distractor = card(
                    DISTRACTOR_BP, DISTRACTOR_ID,
                    Zone.RESERVE_DECK, PLAYER);
            PhysicalCard distractorSource = card(
                    DISTRACTOR_BP, DISTRACTOR_SOURCE_ID,
                    Zone.SIDE_OF_TABLE, PLAYER);
            fixture.addReserve(ominous);
            fixture.addReserve(distractor);
            fixture.addPermanent(distractorSource);

            TracedOutcome tracedParent = tracedCombined(
                    bot, fixture, Decision.parentPull());
            Outcome parent = tracedParent.outcome();
            assertEquals("objective-pull", parent.actionId());
            assertHasTypedRule(
                    tracedParent.trace(), "objective-pull",
                    REQUIRED_PULL_RULE);
            parentResults.add(parent);

            TracedOutcome tracedChild = tracedCombined(
                    bot, fixture, Decision.childPull());
            Outcome child = tracedChild.outcome();
            assertEquals("1", child.actionId());
            assertHasTypedRule(
                    tracedChild.trace(), "1", REQUIRED_PULL_RULE);
            childResults.add(child);
        }
        assertParity(parentResults);
        assertParity(childResults);
    }

    @Test
    public void printingAwareOminousRoutesShareOneObjectivePreference() {
        for (String readyBlueprint : List.of(
                OMINOUS_DIRECT_BP, OMINOUS_LEGACY_BP)) {
            List<Outcome> blockedCandidates =
                    new ArrayList<>();
            List<Outcome> readyCandidates =
                    new ArrayList<>();
            List<Outcome> winners = new ArrayList<>();

            for (Bot bot : Bot.values()) {
                Fixture fixture = fixture(bot);
                fixture.setOpponentControls(
                        fixture.platform(), true);
                PhysicalCard blocked = card(
                        OMINOUS_BP, OMINOUS_ID,
                        Zone.RESERVE_DECK, PLAYER);
                PhysicalCard ready = card(
                        readyBlueprint, READY_REQUIRED_ID,
                        Zone.RESERVE_DECK, PLAYER);
                fixture.addReserve(blocked);
                fixture.addReserve(ready);

                assertFalse(fixture.analyzer()
                        .isRequiredOnTableCardPullRouteReady(
                                fixture.game(), PLAYER,
                                blocked));
                assertTrue(fixture.analyzer()
                        .isRequiredOnTableCardPullRouteReady(
                                fixture.game(), PLAYER,
                                ready));

                Decision decision = Decision.pullChoices(
                        OMINOUS_BP, readyBlueprint);
                Outcome blockedCandidate =
                        cardSelectionAdapter(
                                bot, fixture, decision, "0");
                Outcome readyCandidate =
                        cardSelectionAdapter(
                                bot, fixture, decision, "1");
                assertEquals(0.0f,
                        readyCandidate.score()
                                - blockedCandidate.score(),
                        0.0f);

                TracedOutcome winner = tracedCombined(
                        bot, fixture, decision);
                assertEquals("0",
                        winner.outcome().actionId());
                assertHasTypedRule(
                        winner.trace(), "0",
                        REQUIRED_PULL_ROUTE_BLOCKED_RULE);
                assertHasTypedRule(
                        winner.trace(), "1",
                        REQUIRED_PULL_RULE);
                assertLacksTypedRule(
                        winner.trace(), "0",
                        REQUIRED_PULL_RULE);
                assertLacksTypedRule(
                        winner.trace(), "1",
                        REQUIRED_PULL_ROUTE_BLOCKED_RULE);

                blockedCandidates.add(
                        blockedCandidate);
                readyCandidates.add(readyCandidate);
                winners.add(winner.outcome());
            }

            assertParity(blockedCandidates);
            assertParity(readyCandidates);
            assertParity(winners);
        }
    }

    @Test
    public void printingAwareSecretBaseRoutesShareOneObjectivePreference() {
        for (String readyBlueprint : List.of(
                SECRET_BASE_V_BP,
                SECRET_BASE_LEGACY_BP)) {
            List<Outcome> blockedCandidates =
                    new ArrayList<>();
            List<Outcome> readyCandidates =
                    new ArrayList<>();
            List<Outcome> winners = new ArrayList<>();

            for (Bot bot : Bot.values()) {
                Fixture fixture = fixture(bot);
                setControls(
                        fixture, fixture.bunker(),
                        PLAYER, true);
                PhysicalCard blocked = card(
                        SECRET_BASE_CLASSIC_BP,
                        SECRET_BASE_ID,
                        Zone.RESERVE_DECK, PLAYER);
                PhysicalCard ready = card(
                        readyBlueprint, READY_REQUIRED_ID,
                        Zone.RESERVE_DECK, PLAYER);
                fixture.addReserve(blocked);
                fixture.addReserve(ready);

                assertFalse(fixture.analyzer()
                        .isRequiredOnTableCardPullRouteReady(
                                fixture.game(), PLAYER,
                                blocked));
                assertTrue(fixture.analyzer()
                        .isRequiredOnTableCardPullRouteReady(
                                fixture.game(), PLAYER,
                                ready));

                Decision decision = Decision.pullChoices(
                        SECRET_BASE_CLASSIC_BP,
                        readyBlueprint);
                Outcome blockedCandidate =
                        cardSelectionAdapter(
                                bot, fixture, decision, "0");
                Outcome readyCandidate =
                        cardSelectionAdapter(
                                bot, fixture, decision, "1");
                assertEquals(0.0f,
                        readyCandidate.score()
                                - blockedCandidate.score(),
                        0.0f);

                TracedOutcome winner = tracedCombined(
                        bot, fixture, decision);
                assertEquals("0",
                        winner.outcome().actionId());
                assertHasTypedRule(
                        winner.trace(), "0",
                        REQUIRED_PULL_ROUTE_BLOCKED_RULE);
                assertHasTypedRule(
                        winner.trace(), "1",
                        REQUIRED_PULL_RULE);
                assertLacksTypedRule(
                        winner.trace(), "0",
                        REQUIRED_PULL_RULE);
                assertLacksTypedRule(
                        winner.trace(), "1",
                        REQUIRED_PULL_ROUTE_BLOCKED_RULE);

                blockedCandidates.add(
                        blockedCandidate);
                readyCandidates.add(readyCandidate);
                winners.add(winner.outcome());
            }

            assertParity(blockedCandidates);
            assertParity(readyCandidates);
            assertParity(winners);
        }
    }

    @Test
    public void forceReserveUsesCheapestReadyPrintingRegardlessOfHandOrder() {
        for (boolean directFirst : List.of(false, true)) {
            for (Bot bot : Bot.values()) {
                Fixture fixture = fixture(bot);
                fixture.setOpponentControls(
                        fixture.platform(), true);
                PhysicalCard classic = card(
                        OMINOUS_BP, OMINOUS_ID,
                        Zone.HAND, PLAYER);
                PhysicalCard direct = card(
                        OMINOUS_DIRECT_BP,
                        READY_REQUIRED_ID,
                        Zone.HAND, PLAYER);
                if (directFirst) {
                    fixture.addHand(direct);
                    fixture.addHand(classic);
                } else {
                    fixture.addHand(classic);
                    fixture.addHand(direct);
                }
                when(fixture.modifiers().getDeployCost(
                        fixture.gameState(), classic))
                        .thenReturn(2.0f);
                when(fixture.modifiers().getDeployCost(
                        fixture.gameState(), direct))
                        .thenReturn(0.0f);

                assertFalse(fixture.analyzer()
                        .isRequiredOnTableCardPullRouteReady(
                                fixture.game(), PLAYER,
                                classic));
                assertTrue(fixture.analyzer()
                        .isRequiredOnTableCardPullRouteReady(
                                fixture.game(), PLAYER,
                                direct));
                assertEquals(0, fixture.analyzer()
                        .getRequiredOnTableCardForceReserve(
                                fixture.game(), PLAYER));
            }
        }
    }

    @Test
    public void finalRequiredEffectDeployBeatsDistractor() {
        List<Outcome> distractors = new ArrayList<>();
        List<Outcome> requirements = new ArrayList<>();
        List<Outcome> winners = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot);
            PhysicalCard established = card(
                    SECRET_BASE_V_BP, SECRET_BASE_ID,
                    Zone.SIDE_OF_TABLE, PLAYER);
            PhysicalCard ominous = card(
                    OMINOUS_BP, OMINOUS_ID, Zone.HAND, PLAYER);
            PhysicalCard distractor = card(
                    BIKER_SCOUT_BP, BUDGET_DISTRACTOR_ID,
                    Zone.HAND, PLAYER);
            fixture.addActivePermanent(established);
            fixture.addHand(ominous);
            fixture.addHand(distractor);
            when(fixture.gameState().getForcePileSize(PLAYER))
                    .thenReturn(2);
            when(fixture.modifiers().getForceAvailableToUse(
                    fixture.gameState(), PLAYER)).thenReturn(2);
            when(fixture.modifiers().getDeployCost(
                    fixture.gameState(), ominous)).thenReturn(2.0f);
            when(fixture.modifiers().getDeployCost(
                    fixture.gameState(), distractor)).thenReturn(1.0f);

            assertTrue(fixture.analyzer()
                    .isRequiredCardActiveOnTable(
                            fixture.game(), "Establish Secret Base"));
            assertTrue(fixture.analyzer()
                    .isRequiredCardForFlip(ominous.getTitle()));
            assertEquals(2, fixture.analyzer()
                    .getRequiredOnTableCardForceReserve(
                            fixture.game(), PLAYER, distractor));
            assertEquals(0, fixture.analyzer()
                    .getRequiredOnTableCardForceReserve(
                            fixture.game(), PLAYER, ominous));

            Decision decision = Decision.finalEffectDeploy();
            Outcome distractorResult = deployAdapter(
                    bot, fixture, decision, "deploy-distractor");
            assertContains(distractorResult,
                    "missing required objective card needs 2");
            distractors.add(distractorResult);

            Outcome requiredResult = deployAdapter(
                    bot, fixture, decision, "deploy-ominous");
            assertNotContains(requiredResult,
                    "missing required objective card needs 2");
            requirements.add(requiredResult);

            TracedOutcome traced = tracedCombined(
                    bot, fixture, decision);
            assertHasTypedRule(
                    traced.trace(), "deploy-distractor",
                    REQUIRED_BUDGET_RULE);
            assertLacksTypedRule(
                    traced.trace(), "deploy-ominous",
                    REQUIRED_BUDGET_RULE);
            Outcome result = traced.outcome();
            assertEquals("deploy-ominous", result.actionId());
            assertContains(result, REQUIRED_DEPLOY_RULE);
            winners.add(result);
        }
        assertParity(distractors);
        assertParity(requirements);
        assertParity(winners);
    }

    @Test
    public void classicSecretBaseRouteDeploysThirdScoutThenSelfCloses() {
        List<Outcome> openWinners = new ArrayList<>();
        List<Outcome> closedWinners = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot);
            PhysicalCard secretBase = card(
                    SECRET_BASE_CLASSIC_BP, SECRET_BASE_ID,
                    Zone.RESERVE_DECK, PLAYER);
            PhysicalCard forestPull = card(
                    FOREST_BP, FOREST_PULL_ID,
                    Zone.RESERVE_DECK, PLAYER);
            PhysicalCard candidateScout = card(
                    BIKER_SCOUT_BP, BIKER_SCOUT_ID,
                    Zone.HAND, PLAYER);
            PhysicalCard bunkerScout = card(
                    BIKER_SCOUT_BP, BUNKER_SCOUT_ID,
                    Zone.AT_LOCATION, PLAYER);
            PhysicalCard platformScout = card(
                    BIKER_SCOUT_BP, PLATFORM_SCOUT_ID,
                    Zone.AT_LOCATION, PLAYER);
            fixture.addReserve(secretBase);
            fixture.addReserve(forestPull);
            fixture.addHand(candidateScout);
            fixture.place(bunkerScout, fixture.bunker());
            fixture.place(platformScout, fixture.platform());
            setControls(fixture, fixture.bunker(), PLAYER, true);
            setControls(fixture, fixture.platform(), PLAYER, true);
            when(fixture.modifiers().getDeployCost(
                    fixture.gameState(), candidateScout)).thenReturn(1.0f);

            assertEquals(
                    ObjectiveAnalyzer.ObjectiveProgressCandidateRole
                            .REQUIRED_CARD_DEPLOY_ENABLER_LOCATION,
                    fixture.analyzer().classifyPreFlipProgressCandidate(
                            fixture.game(), PLAYER, forestPull));

            PhysicalCard forest = card(
                    FOREST_BP, FOREST_ID, Zone.LOCATIONS, PLAYER);
            fixture.addLocation(forest);

            assertEquals(
                    ObjectiveAnalyzer.ObjectiveProgressCandidateRole
                            .REQUIRED_CARD_DEPLOY_ENABLER_ACTOR,
                    fixture.analyzer().classifyPreFlipProgressCandidate(
                            fixture.game(), PLAYER, candidateScout));
            assertEquals(1, fixture.analyzer()
                    .getRequiredCardDeployEnablerForceReserve(
                            fixture.game(), PLAYER, null));
            assertTrue(fixture.analyzer()
                    .advancesRequiredCardDeployPrerequisiteAt(
                            fixture.game(), PLAYER,
                            candidateScout, forest));

            Decision decision = Decision.deployBikerScout(
                    forest, fixture.cantina());
            TracedOutcome open = tracedCombined(bot, fixture, decision);
            assertEquals(String.valueOf(FOREST_ID),
                    open.outcome().actionId());
            assertHasTypedRule(
                    open.trace(), String.valueOf(FOREST_ID),
                    REQUIRED_ENABLER_DEPLOY_RULE);
            openWinners.add(open.outcome());

            PhysicalCard forestScout = card(
                    BIKER_SCOUT_BP, FOREST_SCOUT_ID,
                    Zone.AT_LOCATION, PLAYER);
            fixture.place(forestScout, forest);
            setControls(fixture, forest, PLAYER, true);

            assertFalse(fixture.analyzer()
                    .advancesRequiredCardDeployPrerequisiteAt(
                            fixture.game(), PLAYER,
                            candidateScout, forest));
            TracedOutcome closed = tracedCombined(
                    bot, fixture, decision);
            assertLacksTypedRule(
                    closed.trace(), String.valueOf(FOREST_ID),
                    REQUIRED_ENABLER_DEPLOY_RULE);
            closedWinners.add(closed.outcome());
        }
        assertParity(openWinners);
        assertParity(closedWinners);
    }

    @Test
    public void classicPairRequiresAccessibleCounterpartAndReservesLivePairCost() {
        for (Bot bot : Bot.values()) {
            Fixture reserveOnly = fixture(bot);
            reserveOnly.addReserve(card(
                    SECRET_BASE_CLASSIC_BP, SECRET_BASE_ID,
                    Zone.RESERVE_DECK, PLAYER));
            PhysicalCard reserveAtSt = card(
                    AT_ST_BP, AT_ST_ID,
                    Zone.RESERVE_DECK, PLAYER);
            PhysicalCard reservePilot = card(
                    AT_ST_PILOT_BP, AT_ST_PILOT_ID,
                    Zone.RESERVE_DECK, PLAYER);
            reserveOnly.addReserve(reserveAtSt);
            reserveOnly.addReserve(reservePilot);

            assertEquals(
                    ObjectiveAnalyzer.ObjectiveProgressCandidateRole.NONE,
                    reserveOnly.analyzer()
                            .classifyPreFlipProgressCandidate(
                                    reserveOnly.game(), PLAYER,
                                    reserveAtSt));
            assertEquals(
                    ObjectiveAnalyzer.ObjectiveProgressCandidateRole.NONE,
                    reserveOnly.analyzer()
                            .classifyPreFlipProgressCandidate(
                                    reserveOnly.game(), PLAYER,
                                    reservePilot));
            assertEquals(0, reserveOnly.analyzer()
                    .getRequiredCardDeployEnablerForceReserve(
                            reserveOnly.game(), PLAYER, null));

            Fixture handPair = fixture(bot);
            handPair.addReserve(card(
                    SECRET_BASE_CLASSIC_BP, SECRET_BASE_ID,
                    Zone.RESERVE_DECK, PLAYER));
            PhysicalCard handAtSt = card(
                    AT_ST_BP, AT_ST_ID, Zone.HAND, PLAYER);
            PhysicalCard handPilot = card(
                    AT_ST_PILOT_BP, AT_ST_PILOT_ID,
                    Zone.HAND, PLAYER);
            handPair.addHand(handAtSt);
            handPair.addHand(handPilot);
            when(handPair.modifiers().getDeployCost(
                    handPair.gameState(), handAtSt)).thenReturn(3.0f);
            when(handPair.modifiers().getDeployCost(
                    handPair.gameState(), handPilot)).thenReturn(2.0f);

            assertTrue(Filters.AT_ST.accepts(
                    handPair.gameState(), handPair.modifiers(), handAtSt));
            assertTrue(Filters.pilot.accepts(
                    handPair.gameState(), handPair.modifiers(), handPilot));
            assertTrue(handPair.analyzer()
                    .hasActiveRequiredCardDeployActorRule(
                            handPair.game(), PLAYER));
            assertFalse(handPair.analyzer()
                    .isMissingRequiredCardDeployEnablerAt(
                            handPair.game(), PLAYER,
                            handPair.bunker()));
            assertEquals(
                    ObjectiveAnalyzer.ObjectiveProgressCandidateRole
                            .REQUIRED_CARD_DEPLOY_ENABLER_ACTOR,
                    handPair.analyzer()
                            .classifyPreFlipProgressCandidate(
                                    handPair.game(), PLAYER, handAtSt));
            assertEquals(
                    ObjectiveAnalyzer.ObjectiveProgressCandidateRole
                            .REQUIRED_CARD_DEPLOY_ENABLER_ACTOR,
                    handPair.analyzer()
                            .classifyPreFlipProgressCandidate(
                                    handPair.game(), PLAYER, handPilot));
            assertEquals(5, handPair.analyzer()
                    .getRequiredCardDeployEnablerForceReserve(
                            handPair.game(), PLAYER, null));

            Fixture directScout = fixture(bot);
            directScout.addReserve(card(
                    SECRET_BASE_CLASSIC_BP, SECRET_BASE_ID,
                    Zone.RESERVE_DECK, PLAYER));
            PhysicalCard forest = card(
                    FOREST_BP, FOREST_ID, Zone.LOCATIONS, PLAYER);
            directScout.addLocation(forest);
            PhysicalCard bikerScout = card(
                    BIKER_SCOUT_BP, BIKER_SCOUT_ID,
                    Zone.HAND, PLAYER);
            directScout.addHand(bikerScout);
            when(directScout.modifiers().getDeployCost(
                    directScout.gameState(), bikerScout))
                    .thenReturn(1.0f);

            assertEquals(
                    ObjectiveAnalyzer.ObjectiveProgressCandidateRole
                            .REQUIRED_CARD_DEPLOY_ENABLER_ACTOR,
                    directScout.analyzer()
                            .classifyPreFlipProgressCandidate(
                                    directScout.game(), PLAYER,
                                    bikerScout));
            assertEquals(1, directScout.analyzer()
                    .getRequiredCardDeployEnablerForceReserve(
                            directScout.game(), PLAYER, null));
            assertTrue(directScout.analyzer()
                    .advancesRequiredCardDeployPrerequisiteAt(
                            directScout.game(), PLAYER,
                            bikerScout, forest));
        }
    }

    @Test
    public void classicPairLegalityIgnoresCurrentForceButReservesFullCost() {
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot);
            fixture.addReserve(card(
                    SECRET_BASE_CLASSIC_BP, SECRET_BASE_ID,
                    Zone.RESERVE_DECK, PLAYER));
            PhysicalCard atSt = card(
                    AT_ST_BP, AT_ST_ID, Zone.HAND, PLAYER);
            PhysicalCard pilot = card(
                    AT_ST_PILOT_BP, AT_ST_PILOT_ID,
                    Zone.HAND, PLAYER);
            fixture.addHand(atSt);
            fixture.addHand(pilot);
            when(fixture.gameState().getForcePileSize(PLAYER))
                    .thenReturn(0);
            when(fixture.modifiers().getForceAvailableToUse(
                    fixture.gameState(), PLAYER)).thenReturn(0);
            when(fixture.modifiers().getDeployCost(
                    fixture.gameState(), atSt)).thenReturn(3.0f);
            when(fixture.modifiers().getDeployCost(
                    fixture.gameState(), pilot)).thenReturn(2.0f);

            assertEquals(
                    ObjectiveAnalyzer.ObjectiveProgressCandidateRole
                            .REQUIRED_CARD_DEPLOY_ENABLER_ACTOR,
                    fixture.analyzer()
                            .classifyPreFlipProgressCandidate(
                                    fixture.game(), PLAYER, atSt));
            assertEquals(
                    ObjectiveAnalyzer.ObjectiveProgressCandidateRole
                            .REQUIRED_CARD_DEPLOY_ENABLER_ACTOR,
                    fixture.analyzer()
                            .classifyPreFlipProgressCandidate(
                                    fixture.game(), PLAYER, pilot));
            assertEquals(5, fixture.analyzer()
                    .getRequiredCardDeployEnablerForceReserve(
                            fixture.game(), PLAYER, null));
        }
    }

    @Test
    public void bikerScoutPilotStillFundsPairWhenDirectDeployIsIllegal() {
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot);
            fixture.addReserve(card(
                    SECRET_BASE_CLASSIC_BP, SECRET_BASE_ID,
                    Zone.RESERVE_DECK, PLAYER));
            PhysicalCard atSt = card(
                    AT_ST_BP, AT_ST_ID,
                    Zone.HAND, PLAYER);
            PhysicalCard bikerPilot = card(
                    UNIQUE_BIKER_SCOUT_BP,
                    MOVING_BIKER_SCOUT_ID,
                    Zone.HAND, PLAYER);
            fixture.addHand(atSt);
            fixture.addHand(bikerPilot);
            fixture.blockDirectDeploy(bikerPilot);
            when(fixture.modifiers().getDeployCost(
                    fixture.gameState(), atSt))
                    .thenReturn(3.0f);
            when(fixture.modifiers().getDeployCost(
                    fixture.gameState(), bikerPilot))
                    .thenReturn(2.0f);

            assertTrue(Filters.biker_scout.accepts(
                    fixture.gameState(),
                    fixture.modifiers(), bikerPilot));
            assertTrue(Filters.pilot.accepts(
                    fixture.gameState(),
                    fixture.modifiers(), bikerPilot));
            assertFalse(Filters.deployableToLocation(
                    bikerPilot,
                    Filters.sameCardId(
                            fixture.bunker()),
                    true, 0.0f)
                    .accepts(
                            fixture.gameState(),
                            fixture.modifiers(),
                            bikerPilot));
            assertTrue(Filters
                    .deployableToLocationSimultaneouslyWith(
                            atSt, bikerPilot,
                            true, 0.0f,
                            Filters.sameCardId(
                                    fixture.bunker()),
                            true, 0.0f)
                    .accepts(
                            fixture.gameState(),
                            fixture.modifiers(), atSt));
            assertEquals(5, fixture.analyzer()
                    .getRequiredCardDeployEnablerForceReserve(
                            fixture.game(), PLAYER, null));
        }
    }

    @Test(timeout = 10000)
    public void classicPairReserveMatchingScalesToThirtyByThirty() {
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot);
            fixture.addReserve(card(
                    SECRET_BASE_CLASSIC_BP, SECRET_BASE_ID,
                    Zone.RESERVE_DECK, PLAYER));
            for (int index = 0; index < 30; index++) {
                PhysicalCard host = card(
                        AT_ST_BP, 1000 + index,
                        Zone.HAND, PLAYER);
                PhysicalCard pilot = card(
                        AT_ST_PILOT_BP, 2000 + index,
                        Zone.HAND, PLAYER);
                fixture.addHand(host);
                fixture.addHand(pilot);
                when(fixture.modifiers().getDeployCost(
                        fixture.gameState(), host))
                        .thenReturn((float) index + 1.0f);
                when(fixture.modifiers().getDeployCost(
                        fixture.gameState(), pilot))
                        .thenReturn((float) index + 1.0f);
            }

            assertEquals(6, fixture.analyzer()
                    .getRequiredCardDeployEnablerForceReserve(
                            fixture.game(), PLAYER, null));
        }
    }

    @Test
    public void classicPairReserveCountsOnlyDistinctViableOpenTargets() {
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot);
            fixture.addReserve(card(
                    SECRET_BASE_CLASSIC_BP, SECRET_BASE_ID,
                    Zone.RESERVE_DECK, PLAYER));
            PhysicalCard forest = card(
                    FOREST_BP, FOREST_ID,
                    Zone.LOCATIONS, PLAYER);
            fixture.addLocation(forest);
            PhysicalCard platformScout = card(
                    BIKER_SCOUT_BP, PLATFORM_SCOUT_ID,
                    Zone.AT_LOCATION, PLAYER);
            fixture.place(
                    platformScout, fixture.platform());
            setControls(
                    fixture, fixture.platform(),
                    PLAYER, true);
            fixture.blockDeployTarget(
                    fixture.bunker());

            for (int index = 0; index < 2; index++) {
                PhysicalCard host = card(
                        AT_ST_BP, 3000 + index,
                        Zone.HAND, PLAYER);
                PhysicalCard pilot = card(
                        AT_ST_PILOT_BP, 4000 + index,
                        Zone.HAND, PLAYER);
                fixture.addHand(host);
                fixture.addHand(pilot);
                when(fixture.modifiers().getDeployCost(
                        fixture.gameState(), host))
                        .thenReturn(1.0f + index * 3.0f);
                when(fixture.modifiers().getDeployCost(
                        fixture.gameState(), pilot))
                        .thenReturn(2.0f + index * 3.0f);
            }

            assertEquals(3, fixture.analyzer()
                    .getRequiredCardDeployEnablerForceReserve(
                            fixture.game(), PLAYER, null));
        }
    }

    @Test
    public void classicPairReserveCountsAtMostTwoDistinctOpenTargets() {
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot);
            fixture.addReserve(card(
                    SECRET_BASE_CLASSIC_BP, SECRET_BASE_ID,
                    Zone.RESERVE_DECK, PLAYER));
            PhysicalCard forest = card(
                    FOREST_BP, FOREST_ID,
                    Zone.LOCATIONS, PLAYER);
            PhysicalCard backDoor = card(
                    BACK_DOOR_BP, BACK_DOOR_ID,
                    Zone.LOCATIONS, PLAYER);
            fixture.addLocation(forest);
            fixture.addLocation(backDoor);
            for (PhysicalCard blockedTarget
                    : List.of(
                            fixture.bunker(),
                            fixture.platform())) {
                fixture.blockDeployTarget(
                        blockedTarget);
            }

            for (int index = 0; index < 3; index++) {
                PhysicalCard host = card(
                        AT_ST_BP, 5000 + index,
                        Zone.HAND, PLAYER);
                PhysicalCard pilot = card(
                        AT_ST_PILOT_BP, 6000 + index,
                        Zone.HAND, PLAYER);
                fixture.addHand(host);
                fixture.addHand(pilot);
                when(fixture.modifiers().getDeployCost(
                        fixture.gameState(), host))
                        .thenReturn(1.0f + index * 3.0f);
                when(fixture.modifiers().getDeployCost(
                        fixture.gameState(), pilot))
                        .thenReturn(2.0f + index * 3.0f);
            }

            assertEquals(12, fixture.analyzer()
                    .getRequiredCardDeployEnablerForceReserve(
                            fixture.game(), PLAYER, null));
        }
    }

    @Test
    public void classicPairReserveUsesTargetSpecificSimultaneousMinimum() {
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot);
            fixture.addReserve(card(
                    SECRET_BASE_CLASSIC_BP, SECRET_BASE_ID,
                    Zone.RESERVE_DECK, PLAYER));
            PhysicalCard forest = card(
                    FOREST_BP, FOREST_ID,
                    Zone.LOCATIONS, PLAYER);
            fixture.addLocation(forest);

            List<PhysicalCard> hosts = new ArrayList<>();
            List<PhysicalCard> pilots = new ArrayList<>();
            for (int index = 0; index < 3; index++) {
                PhysicalCard host = card(
                        AT_ST_BP, 7000 + index,
                        Zone.HAND, PLAYER);
                PhysicalCard pilot = card(
                        AT_ST_PILOT_BP, 7100 + index,
                        Zone.HAND, PLAYER);
                fixture.addHand(host);
                fixture.addHand(pilot);
                fixture.blockDirectDeploy(pilot);
                when(fixture.modifiers().getDeployCost(
                        fixture.gameState(), host))
                        .thenReturn(40.0f);
                when(fixture.modifiers().getDeployCost(
                        fixture.gameState(), pilot))
                        .thenReturn(40.0f);
                hosts.add(host);
                pilots.add(pilot);
            }
            when(fixture.modifiers().getDeployCost(
                    fixture.gameState(), hosts.get(0)))
                    .thenReturn(3.0f);
            when(fixture.modifiers().getDeployCost(
                    fixture.gameState(), pilots.get(0)))
                    .thenReturn(5.0f);

            setSimultaneousDeployCost(
                    fixture, hosts.get(0), pilots.get(0),
                    fixture.bunker(), 3.0f);
            setSimultaneousDeployCost(
                    fixture, hosts.get(0), pilots.get(0),
                    fixture.platform(), 50.0f);
            setSimultaneousDeployCost(
                    fixture, hosts.get(0), pilots.get(0),
                    forest, 50.0f);

            setSimultaneousDeployCost(
                    fixture, hosts.get(1), pilots.get(1),
                    fixture.bunker(), 20.0f);
            setSimultaneousDeployCost(
                    fixture, hosts.get(1), pilots.get(1),
                    fixture.platform(), 2.0f);
            setSimultaneousDeployCost(
                    fixture, hosts.get(1), pilots.get(1),
                    forest, 4.0f);

            setSimultaneousDeployCost(
                    fixture, hosts.get(2), pilots.get(2),
                    fixture.bunker(), 20.0f);
            setSimultaneousDeployCost(
                    fixture, hosts.get(2), pilots.get(2),
                    fixture.platform(), 3.0f);
            setSimultaneousDeployCost(
                    fixture, hosts.get(2), pilots.get(2),
                    forest, 9.0f);

            assertEquals(10, fixture.analyzer()
                    .getRequiredCardDeployEnablerForceReserve(
                            fixture.game(), PLAYER, null));
        }
    }

    @Test
    public void topLevelDeployUsesCrossedTargetReserveAndObjectiveActorSelfCloses() {
        List<Outcome> distractors = new ArrayList<>();
        List<Outcome> objectiveActors = new ArrayList<>();
        List<Outcome> winners = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot);
            PhysicalCard forest =
                    prepareTwoClassicActors(fixture);
            PhysicalCard backDoor = card(
                    BACK_DOOR_BP, BACK_DOOR_ID,
                    Zone.LOCATIONS, PLAYER);
            fixture.addLocation(backDoor);

            PhysicalCard firstHost = card(
                    AT_ST_BP, 7200,
                    Zone.HAND, PLAYER);
            PhysicalCard firstPilot = card(
                    AT_ST_PILOT_BP, 7201,
                    Zone.HAND, PLAYER);
            PhysicalCard secondHost = card(
                    AT_ST_BP, 7202,
                    Zone.HAND, PLAYER);
            PhysicalCard secondPilot = card(
                    AT_ST_PILOT_BP, 7203,
                    Zone.HAND, PLAYER);
            PhysicalCard objectiveActor = card(
                    BIKER_SCOUT_BP, BIKER_SCOUT_ID,
                    Zone.HAND, PLAYER);
            PhysicalCard distractor = card(
                    DYER_BP, DYER_ID,
                    Zone.HAND, PLAYER);
            for (PhysicalCard card : List.of(
                    firstHost, firstPilot,
                    secondHost, secondPilot,
                    objectiveActor, distractor)) {
                fixture.addHand(card);
            }
            for (PhysicalCard pairCard : List.of(
                    firstHost, firstPilot,
                    secondHost, secondPilot)) {
                when(fixture.modifiers().getDeployCost(
                        fixture.gameState(), pairCard))
                        .thenReturn(40.0f);
            }
            fixture.blockDirectDeploy(firstPilot);
            fixture.blockDirectDeploy(secondPilot);

            setSimultaneousDeployCost(
                    fixture, firstHost, firstPilot,
                    forest, 4.0f);
            setSimultaneousDeployCost(
                    fixture, firstHost, firstPilot,
                    backDoor, 20.0f);
            setSimultaneousDeployCost(
                    fixture, secondHost, secondPilot,
                    forest, 20.0f);
            setSimultaneousDeployCost(
                    fixture, secondHost, secondPilot,
                    backDoor, 4.0f);
            setTargetDeployCost(
                    fixture, objectiveActor,
                    forest, 50.0f);
            setTargetDeployCost(
                    fixture, objectiveActor,
                    backDoor, 50.0f);
            when(fixture.gameState().getForcePileSize(PLAYER))
                    .thenReturn(6);
            when(fixture.modifiers().getForceAvailableToUse(
                    fixture.gameState(), PLAYER))
                    .thenReturn(6);

            assertEquals(4, fixture.analyzer()
                    .getRequiredCardDeployEnablerForceReserve(
                            fixture.game(), PLAYER, null));
            assertEquals(4, fixture.analyzer()
                    .getRequiredCardDeployEnablerForceReserve(
                            fixture.game(), PLAYER, distractor));
            assertEquals(0, fixture.analyzer()
                    .getRequiredCardDeployEnablerForceReserve(
                            fixture.game(), PLAYER,
                            objectiveActor));

            Decision decision = Decision.topLevelDeploy(
                    distractor, objectiveActor);
            String distractorAction =
                    "deploy-" + distractor.getCardId();
            String objectiveAction =
                    "deploy-" + objectiveActor.getCardId();
            Outcome distractorResult = deployAdapter(
                    bot, fixture, decision,
                    distractorAction);
            Outcome objectiveResult = deployAdapter(
                    bot, fixture, decision,
                    objectiveAction);
            assertContains(
                    distractorResult,
                    "missing required objective card needs 4");
            assertContains(
                    distractorResult,
                    "leaves 3 Force");
            assertNotContains(
                    objectiveResult,
                    "missing required objective card needs 4");
            distractors.add(distractorResult);
            objectiveActors.add(objectiveResult);

            TracedOutcome traced = tracedCombined(
                    bot, fixture, decision);
            assertHasTypedRule(
                    traced.trace(), distractorAction,
                    REQUIRED_BUDGET_RULE);
            assertLacksTypedRule(
                    traced.trace(), objectiveAction,
                    REQUIRED_BUDGET_RULE);
            assertEquals(
                    objectiveAction,
                    traced.outcome().actionId());
            assertContains(
                    traced.outcome(),
                    REQUIRED_ENABLER_DEPLOY_RULE);
            winners.add(traced.outcome());
        }
        assertParity(distractors);
        assertParity(objectiveActors);
        assertParity(winners);
    }

    @Test
    public void directBikerScoutReserveUsesExactTargetDeployCost() {
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot);
            PhysicalCard forest =
                    prepareTwoClassicActors(fixture);
            PhysicalCard backDoor = card(
                    BACK_DOOR_BP, BACK_DOOR_ID,
                    Zone.LOCATIONS, PLAYER);
            fixture.addLocation(backDoor);
            PhysicalCard bikerScout = card(
                    BIKER_SCOUT_BP, BIKER_SCOUT_ID,
                    Zone.HAND, PLAYER);
            fixture.addHand(bikerScout);
            when(fixture.modifiers().getDeployCost(
                    fixture.gameState(), bikerScout))
                    .thenReturn(50.0f);
            setTargetDeployCost(
                    fixture, bikerScout,
                    forest, 7.0f);
            setTargetDeployCost(
                    fixture, bikerScout,
                    backDoor, 2.0f);

            assertEquals(2, fixture.analyzer()
                    .getRequiredCardDeployEnablerForceReserve(
                            fixture.game(), PLAYER, null));
        }
    }

    @Test
    public void classicPairRejectsBunkerWhenItIsTheOnlyOpenSite() {
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot);
            fixture.addReserve(card(
                    SECRET_BASE_CLASSIC_BP, SECRET_BASE_ID,
                    Zone.RESERVE_DECK, PLAYER));
            PhysicalCard forest = card(
                    FOREST_BP, FOREST_ID,
                    Zone.LOCATIONS, PLAYER);
            fixture.addLocation(forest);
            PhysicalCard platformScout = card(
                    BIKER_SCOUT_BP, PLATFORM_SCOUT_ID,
                    Zone.AT_LOCATION, PLAYER);
            PhysicalCard forestScout = card(
                    BIKER_SCOUT_BP, FOREST_SCOUT_ID,
                    Zone.AT_LOCATION, PLAYER);
            fixture.place(platformScout, fixture.platform());
            fixture.place(forestScout, forest);
            setControls(fixture, fixture.platform(), PLAYER, true);
            setControls(fixture, forest, PLAYER, true);

            PhysicalCard atSt = card(
                    AT_ST_BP, AT_ST_ID, Zone.HAND, PLAYER);
            PhysicalCard pilot = card(
                    AT_ST_PILOT_BP, AT_ST_PILOT_ID,
                    Zone.HAND, PLAYER);
            fixture.addHand(atSt);
            fixture.addHand(pilot);
            fixture.blockDeployTarget(
                    fixture.bunker());

            assertFalse(fixture.analyzer()
                    .advancesRequiredCardDeployPrerequisiteAt(
                            fixture.game(), PLAYER,
                            atSt, fixture.bunker()));
            assertEquals(
                    ObjectiveAnalyzer.ObjectiveProgressCandidateRole.NONE,
                    fixture.analyzer()
                            .classifyPreFlipProgressCandidate(
                                    fixture.game(), PLAYER, atSt));
            assertEquals(
                    ObjectiveAnalyzer.ObjectiveProgressCandidateRole.NONE,
                    fixture.analyzer()
                            .classifyPreFlipProgressCandidate(
                                    fixture.game(), PLAYER, pilot));
            assertEquals(0, fixture.analyzer()
                    .getRequiredCardDeployEnablerForceReserve(
                            fixture.game(), PLAYER, null));
        }
    }

    @Test
    public void classicPairDeployChildrenScoreAtStThenPilotRouteProgress() {
        List<Outcome> atStWinners = new ArrayList<>();
        List<Outcome> pilotWinners = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture atStFixture = fixture(bot);
            PhysicalCard atStForest = prepareTwoClassicActors(
                    atStFixture);
            PhysicalCard atSt = card(
                    AT_ST_BP, AT_ST_ID, Zone.HAND, PLAYER);
            PhysicalCard pilot = card(
                    AT_ST_PILOT_BP, AT_ST_PILOT_ID,
                    Zone.HAND, PLAYER);
            atStFixture.addHand(atSt);
            atStFixture.addHand(pilot);

            Decision atStDecision = Decision.deployCandidate(
                    atSt, atStForest, atStFixture.cantina());
            TracedOutcome atStResult = tracedCombined(
                    bot, atStFixture, atStDecision);
            assertEquals(String.valueOf(FOREST_ID),
                    atStResult.outcome().actionId());
            assertHasTypedRule(
                    atStResult.trace(), String.valueOf(FOREST_ID),
                    REQUIRED_ENABLER_DEPLOY_RULE);
            atStWinners.add(atStResult.outcome());

            Fixture pilotFixture = fixture(bot);
            PhysicalCard pilotForest = prepareTwoClassicActors(
                    pilotFixture);
            PhysicalCard stagedAtSt = card(
                    AT_ST_BP, AT_ST_ID,
                    Zone.AT_LOCATION, PLAYER);
            pilotFixture.place(stagedAtSt, pilotForest);
            PhysicalCard boardingPilot = card(
                    AT_ST_PILOT_BP, AT_ST_PILOT_ID,
                    Zone.HAND, PLAYER);
            pilotFixture.addHand(boardingPilot);
            when(pilotFixture.modifiers().isPiloted(
                    pilotFixture.gameState(), stagedAtSt, false))
                    .thenReturn(false);

            Decision pilotDecision = Decision.deployCandidate(
                    boardingPilot, stagedAtSt,
                    pilotFixture.cantina());
            TracedOutcome pilotResult = tracedCombined(
                    bot, pilotFixture, pilotDecision);
            assertEquals(String.valueOf(AT_ST_ID),
                    pilotResult.outcome().actionId());
            assertHasTypedRule(
                    pilotResult.trace(), String.valueOf(AT_ST_ID),
                    REQUIRED_ENABLER_DEPLOY_RULE);
            pilotWinners.add(pilotResult.outcome());
        }
        assertParity(atStWinners);
        assertParity(pilotWinners);
    }

    @Test
    public void classicPairDeployChildRejectsOpponentOccupiedSiteUntilControlled() {
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot);
            PhysicalCard forest = prepareTwoClassicActors(fixture);
            fixture.setOpponentOccupies(forest, true);
            PhysicalCard atSt = card(
                    AT_ST_BP, AT_ST_ID, Zone.HAND, PLAYER);
            PhysicalCard pilot = card(
                    AT_ST_PILOT_BP, AT_ST_PILOT_ID,
                    Zone.HAND, PLAYER);
            fixture.addHand(atSt);
            fixture.addHand(pilot);

            assertFalse(fixture.analyzer()
                    .advancesRequiredCardDeployPrerequisiteAt(
                            fixture.game(), PLAYER, atSt, forest));
            TracedOutcome blocked = tracedCombined(
                    bot, fixture,
                    Decision.deployCandidate(
                            atSt, forest, fixture.cantina()));
            assertLacksTypedRule(
                    blocked.trace(), String.valueOf(FOREST_ID),
                    REQUIRED_ENABLER_DEPLOY_RULE);

            setControls(fixture, forest, PLAYER, true);
            assertTrue(fixture.analyzer()
                    .advancesRequiredCardDeployPrerequisiteAt(
                            fixture.game(), PLAYER, atSt, forest));

            Fixture pilotFixture = fixture(bot);
            PhysicalCard pilotForest =
                    prepareTwoClassicActors(pilotFixture);
            pilotFixture.setOpponentOccupies(
                    pilotForest, true);
            PhysicalCard stagedAtSt = card(
                    AT_ST_BP, AT_ST_ID,
                    Zone.AT_LOCATION, PLAYER);
            pilotFixture.place(stagedAtSt, pilotForest);
            PhysicalCard boardingPilot = card(
                    AT_ST_PILOT_BP, AT_ST_PILOT_ID,
                    Zone.HAND, PLAYER);
            pilotFixture.addHand(boardingPilot);

            assertFalse(pilotFixture.analyzer()
                    .advancesRequiredCardDeployPrerequisiteAt(
                            pilotFixture.game(), PLAYER,
                            boardingPilot, stagedAtSt));
            setControls(
                    pilotFixture, pilotForest, PLAYER, true);
            assertTrue(pilotFixture.analyzer()
                    .advancesRequiredCardDeployPrerequisiteAt(
                            pilotFixture.game(), PLAYER,
                            boardingPilot, stagedAtSt));
        }
    }

    @Test
    public void classicPairRejectsIncompatibleFullInactiveAndCargoInputs() {
        for (Bot bot : Bot.values()) {
            Fixture incompatible = fixture(bot);
            prepareTwoClassicActors(incompatible);
            PhysicalCard incompatibleHost = card(
                    AT_ST_BP, AT_ST_ID, Zone.HAND, PLAYER);
            PhysicalCard incompatiblePilot = card(
                    AT_ST_PILOT_BP, AT_ST_PILOT_ID,
                    Zone.HAND, PLAYER);
            incompatible.addHand(incompatibleHost);
            incompatible.addHand(incompatiblePilot);
            when(incompatible.modifiers()
                    .isProhibitedFromTarget(
                            incompatible.gameState(),
                            incompatiblePilot,
                            incompatibleHost))
                    .thenReturn(true);
            assertEquals(
                    ObjectiveAnalyzer.ObjectiveProgressCandidateRole.NONE,
                    incompatible.analyzer()
                            .classifyPreFlipProgressCandidate(
                                    incompatible.game(), PLAYER,
                                    incompatibleHost));
            assertEquals(0, incompatible.analyzer()
                    .getRequiredCardDeployEnablerForceReserve(
                            incompatible.game(), PLAYER, null));

            Fixture prohibited = fixture(bot);
            prepareTwoClassicActors(prohibited);
            PhysicalCard prohibitedHost = card(
                    AT_ST_BP, AT_ST_ID, Zone.HAND, PLAYER);
            PhysicalCard prohibitedPilot = card(
                    AT_ST_PILOT_BP, AT_ST_PILOT_ID,
                    Zone.HAND, PLAYER);
            prohibited.addHand(prohibitedHost);
            prohibited.addHand(prohibitedPilot);
            when(prohibited.modifiers()
                    .prohibitedFromPiloting(
                            prohibited.gameState(),
                            prohibitedPilot,
                            prohibitedHost))
                    .thenReturn(true);
            assertEquals(
                    ObjectiveAnalyzer.ObjectiveProgressCandidateRole.NONE,
                    prohibited.analyzer()
                            .classifyPreFlipProgressCandidate(
                                    prohibited.game(), PLAYER,
                                    prohibitedHost));
            assertEquals(0, prohibited.analyzer()
                    .getRequiredCardDeployEnablerForceReserve(
                            prohibited.game(), PLAYER, null));

            Fixture full = fixture(bot);
            PhysicalCard fullForest =
                    prepareTwoClassicActors(full);
            PhysicalCard fullHost = card(
                    AT_ST_BP, AT_ST_ID,
                    Zone.AT_LOCATION, PLAYER);
            PhysicalCard fullPilot = card(
                    AT_ST_PILOT_BP, AT_ST_PILOT_ID,
                    Zone.HAND, PLAYER);
            full.place(fullHost, fullForest);
            full.addHand(fullPilot);
            when(full.gameState().getAvailablePilotCapacity(
                    full.modifiers(), fullHost, fullPilot))
                    .thenReturn(0);
            assertEquals(
                    ObjectiveAnalyzer.ObjectiveProgressCandidateRole.NONE,
                    full.analyzer()
                            .classifyPreFlipProgressCandidate(
                                    full.game(), PLAYER, fullPilot));
            assertFalse(full.analyzer()
                    .advancesRequiredCardDeployPrerequisiteAt(
                            full.game(), PLAYER,
                            fullPilot, fullHost));
            assertEquals(0, full.analyzer()
                    .getRequiredCardDeployEnablerForceReserve(
                            full.game(), PLAYER, null));

            Fixture inactive = fixture(bot);
            PhysicalCard inactiveForest =
                    prepareTwoClassicActors(inactive);
            PhysicalCard inactiveHost = card(
                    AT_ST_BP, AT_ST_ID,
                    Zone.AT_LOCATION, PLAYER);
            PhysicalCard inactivePilot = card(
                    AT_ST_PILOT_BP, AT_ST_PILOT_ID,
                    Zone.HAND, PLAYER);
            inactive.place(inactiveHost, inactiveForest);
            inactive.addHand(inactivePilot);
            setInactive(inactive.gameState(), inactiveHost);
            assertEquals(
                    ObjectiveAnalyzer.ObjectiveProgressCandidateRole.NONE,
                    inactive.analyzer()
                            .classifyPreFlipProgressCandidate(
                                    inactive.game(), PLAYER,
                                    inactivePilot));
            assertFalse(inactive.analyzer()
                    .advancesRequiredCardDeployPrerequisiteAt(
                            inactive.game(), PLAYER,
                            inactivePilot, inactiveHost));
            assertEquals(0, inactive.analyzer()
                    .getRequiredCardDeployEnablerForceReserve(
                            inactive.game(), PLAYER, null));

            Fixture cargo = fixture(bot);
            PhysicalCard cargoForest =
                    prepareTwoClassicActors(cargo);
            PhysicalCard cargoHost = card(
                    AT_ST_BP, AT_ST_ID,
                    Zone.AT_LOCATION, PLAYER);
            PhysicalCard cargoPilot = card(
                    AT_ST_PILOT_BP, AT_ST_PILOT_ID,
                    Zone.HAND, PLAYER);
            cargo.place(cargoHost, cargoForest);
            cargo.addHand(cargoPilot);
            PhysicalCard enclosingVehicle = card(
                    AT_ST_BP, SECOND_REQUIRED_ID,
                    Zone.AT_LOCATION, PLAYER);
            when(cargoHost.getAttachedTo())
                    .thenReturn(enclosingVehicle);
            assertEquals(
                    ObjectiveAnalyzer.ObjectiveProgressCandidateRole.NONE,
                    cargo.analyzer()
                            .classifyPreFlipProgressCandidate(
                                    cargo.game(), PLAYER,
                                    cargoPilot));
            assertFalse(cargo.analyzer()
                    .advancesRequiredCardDeployPrerequisiteAt(
                            cargo.game(), PLAYER,
                            cargoPilot, cargoHost));
            assertEquals(0, cargo.analyzer()
                    .getRequiredCardDeployEnablerForceReserve(
                            cargo.game(), PLAYER, null));
        }
    }

    @Test
    public void activePilotSupportsHandHostReserveAndMovementOnlyRoute() {
        for (Bot bot : Bot.values()) {
            Fixture reserveFixture = fixture(bot);
            PhysicalCard reserveForest =
                    prepareTwoClassicActors(reserveFixture);
            PhysicalCard activePilot = card(
                    AT_ST_PILOT_BP, AT_ST_PILOT_ID,
                    Zone.AT_LOCATION, PLAYER);
            PhysicalCard handHost = card(
                    AT_ST_BP, AT_ST_ID, Zone.HAND, PLAYER);
            reserveFixture.place(activePilot, reserveForest);
            reserveFixture.addHand(handHost);
            when(reserveFixture.modifiers().getDeployCost(
                    reserveFixture.gameState(), handHost))
                    .thenReturn(3.0f);
            when(reserveFixture.modifiers()
                    .isProhibitedFromDeployingTo(
                            reserveFixture.gameState(),
                            activePilot, handHost, null))
                    .thenReturn(true);

            assertEquals(
                    ObjectiveAnalyzer.ObjectiveProgressCandidateRole
                            .REQUIRED_CARD_DEPLOY_ENABLER_ACTOR,
                    reserveFixture.analyzer()
                            .classifyPreFlipProgressCandidate(
                                    reserveFixture.game(), PLAYER,
                                    handHost));
            assertTrue(reserveFixture.analyzer()
                    .advancesRequiredCardDeployPrerequisiteAt(
                            reserveFixture.game(), PLAYER,
                            handHost, reserveForest));
            assertEquals(3, reserveFixture.analyzer()
                    .getRequiredCardDeployEnablerForceReserve(
                            reserveFixture.game(), PLAYER, null));

            Fixture movementFixture = fixture(bot);
            PhysicalCard movementForest =
                    prepareTwoClassicActors(movementFixture);
            PhysicalCard stagedHost = card(
                    AT_ST_BP, AT_ST_ID,
                    Zone.AT_LOCATION, PLAYER);
            PhysicalCard movingPilot = card(
                    AT_ST_PILOT_BP, AT_ST_PILOT_ID,
                    Zone.AT_LOCATION, PLAYER);
            movementFixture.place(stagedHost, movementForest);
            movementFixture.place(movingPilot, movementForest);
            when(movementFixture.modifiers()
                    .isPresentWith(
                            movementFixture.gameState(),
                            movingPilot, stagedHost, true))
                    .thenReturn(true);
            when(movementFixture.modifiers()
                    .getLocationHere(
                            movementFixture.gameState(),
                            movingPilot))
                    .thenReturn(movementForest);
            when(movementFixture.modifiers()
                    .getLocationHere(
                            movementFixture.gameState(),
                            stagedHost))
                    .thenReturn(movementForest);
            when(movementFixture.modifiers()
                    .isProhibitedFromDeployingTo(
                            movementFixture.gameState(),
                            movingPilot, stagedHost, null))
                    .thenReturn(true);

            assertTrue(movementFixture.analyzer()
                    .advancesRequiredCardDeployPrerequisiteAt(
                            movementFixture.game(), PLAYER,
                            movingPilot, stagedHost));
        }
    }

    @Test
    public void classicPairEmbarkParentAndTargetCompleteExactAtStRoute() {
        List<Outcome> parentWinners =
                new ArrayList<>();
        List<Outcome> targetWinners =
                new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot);
            PhysicalCard forest =
                    prepareTwoClassicActors(fixture);
            PhysicalCard pilot = card(
                    AT_ST_PILOT_BP, AT_ST_PILOT_ID,
                    Zone.AT_LOCATION, PLAYER);
            PhysicalCard atSt = card(
                    AT_ST_BP, AT_ST_ID,
                    Zone.AT_LOCATION, PLAYER);
            PhysicalCard distractor = card(
                    NON_ACTOR_CARRIER_BP,
                    NON_ACTOR_CARRIER_ID,
                    Zone.AT_LOCATION, PLAYER);
            fixture.place(pilot, forest);
            fixture.place(atSt, forest);
            fixture.place(distractor, forest);

            when(fixture.modifiers().isPiloted(
                    fixture.gameState(), atSt, false))
                    .thenReturn(false);
            when(fixture.modifiers().isPiloted(
                    fixture.gameState(), distractor, false))
                    .thenReturn(false);
            when(fixture.modifiers().isPresentWith(
                    fixture.gameState(), pilot,
                    atSt, true)).thenReturn(true);
            when(fixture.modifiers().isPresentWith(
                    fixture.gameState(), pilot,
                    distractor, true)).thenReturn(true);
            when(fixture.modifiers().getLocationHere(
                    fixture.gameState(), pilot))
                    .thenReturn(forest);
            when(fixture.modifiers().getLocationHere(
                    fixture.gameState(), atSt))
                    .thenReturn(forest);
            when(fixture.modifiers().getLocationHere(
                    fixture.gameState(), distractor))
                    .thenReturn(forest);

            assertTrue(Filters.canEmbarkTo(
                    PLAYER, pilot, false, 0.0f)
                    .accepts(
                            fixture.gameState(),
                            fixture.modifiers(), atSt));
            assertTrue(Filters.canEmbarkTo(
                    PLAYER, pilot, false, 0.0f)
                    .accepts(
                            fixture.gameState(),
                            fixture.modifiers(),
                            distractor));
            assertTrue(fixture.analyzer()
                    .advancesRequiredCardDeployPrerequisiteAt(
                            fixture.game(), PLAYER,
                            pilot, atSt));
            assertFalse(fixture.analyzer()
                    .advancesRequiredCardDeployPrerequisiteAt(
                            fixture.game(), PLAYER,
                            pilot, distractor));

            Decision parent =
                    Decision.topLevelEmbark(pilot);
            String embarkActionId =
                    parent.actionIds().get(0);
            TracedOutcome tracedParent =
                    tracedCombined(bot, fixture, parent);
            assertEquals(
                    embarkActionId,
                    tracedParent.outcome().actionId());
            assertHasTypedRule(
                    tracedParent.trace(),
                    embarkActionId,
                    REQUIRED_ENABLER_EMBARK_START_RULE);
            assertLacksTypedRule(
                    tracedParent.trace(), "pass",
                    REQUIRED_ENABLER_EMBARK_START_RULE);
            parentWinners.add(
                    tracedParent.outcome());

            Decision target =
                    Decision.embarkTarget(
                            pilot, atSt, distractor);
            Outcome atStTarget =
                    cardSelectionAdapter(
                            bot, fixture, target,
                            String.valueOf(
                                    atSt.getCardId()));
            Outcome distractorTarget =
                    cardSelectionAdapter(
                            bot, fixture, target,
                            String.valueOf(
                                    distractor.getCardId()));
            assertContains(
                    atStTarget,
                    REQUIRED_ENABLER_EMBARK_TARGET_RULE);
            assertNotContains(
                    distractorTarget,
                    REQUIRED_ENABLER_EMBARK_TARGET_RULE);
            assertTrue(atStTarget.score()
                    > distractorTarget.score());

            TracedOutcome tracedTarget =
                    tracedCombined(bot, fixture, target);
            assertEquals(
                    String.valueOf(atSt.getCardId()),
                    tracedTarget.outcome().actionId());
            assertHasTypedRule(
                    tracedTarget.trace(),
                    String.valueOf(atSt.getCardId()),
                    REQUIRED_ENABLER_EMBARK_TARGET_RULE);
            assertLacksTypedRule(
                    tracedTarget.trace(),
                    String.valueOf(
                            distractor.getCardId()),
                    REQUIRED_ENABLER_EMBARK_TARGET_RULE);
            targetWinners.add(
                    tracedTarget.outcome());

            when(pilot.getAtLocation())
                    .thenReturn(null);
            when(pilot.getAttachedTo())
                    .thenReturn(atSt);
            when(pilot.isPilotOf())
                    .thenReturn(true);
            when(atSt.getCardsAttached())
                    .thenReturn(List.of(pilot));
            when(fixture.modifiers().isPiloted(
                    fixture.gameState(), atSt, false))
                    .thenReturn(true);

            assertFalse(fixture.analyzer()
                    .advancesRequiredCardDeployPrerequisiteAt(
                            fixture.game(), PLAYER,
                            pilot, atSt));
            TracedOutcome closedParent =
                    tracedCombined(bot, fixture, parent);
            assertLacksTypedRule(
                    closedParent.trace(),
                    embarkActionId,
                    REQUIRED_ENABLER_EMBARK_START_RULE);
            Outcome closedTarget =
                    cardSelectionAdapter(
                            bot, fixture, target,
                            String.valueOf(
                                    atSt.getCardId()));
            assertNotContains(
                    closedTarget,
                    REQUIRED_ENABLER_EMBARK_TARGET_RULE);
        }
        assertParity(parentWinners);
        assertParity(targetWinners);
    }

    @Test
    public void inPlayPairReservesLiveCrashedAtStEmbarkCost() {
        for (int availableForce : List.of(0, 1)) {
            for (Bot bot : Bot.values()) {
                Fixture fixture = fixture(bot);
                PhysicalCard forest =
                        prepareTwoClassicActors(fixture);
                PhysicalCard pilot = card(
                        AT_ST_PILOT_BP, AT_ST_PILOT_ID,
                        Zone.AT_LOCATION, PLAYER);
                PhysicalCard atSt = card(
                        AT_ST_BP, AT_ST_ID,
                        Zone.AT_LOCATION, PLAYER);
                fixture.place(pilot, forest);
                fixture.place(atSt, forest);
                when(atSt.isCrashed()).thenReturn(true);
                when(fixture.modifiers().isPiloted(
                        fixture.gameState(), atSt, false))
                        .thenReturn(false);
                when(fixture.modifiers().isPresentWith(
                        fixture.gameState(), pilot,
                        atSt, true)).thenReturn(true);
                when(fixture.modifiers().getLocationHere(
                        fixture.gameState(), pilot))
                        .thenReturn(forest);
                when(fixture.modifiers().getLocationHere(
                        fixture.gameState(), atSt))
                        .thenReturn(forest);
                when(fixture.modifiers().getForceAvailableToUse(
                        fixture.gameState(), PLAYER))
                        .thenReturn(availableForce);
                when(fixture.modifiers().getEmbarkingCost(
                        fixture.gameState(), pilot,
                        atSt, 0.0f)).thenReturn(1.0f);

                assertTrue(atSt.getBlueprint().hasKeyword(
                        com.gempukku.swccgo.common.Keyword.ENCLOSED));
                assertTrue(Filters.canEmbarkTo(
                        PLAYER, pilot, true, 0.0f)
                        .accepts(
                                fixture.gameState(),
                                fixture.modifiers(), atSt));
                assertEquals(
                        availableForce >= 1,
                        Filters.canEmbarkTo(
                                PLAYER, pilot,
                                false, 0.0f)
                                .accepts(
                                        fixture.gameState(),
                                        fixture.modifiers(),
                                        atSt));
                assertEquals(1, fixture.analyzer()
                        .getRequiredCardDeployEnablerForceReserve(
                                fixture.game(), PLAYER, null));
            }
        }
    }

    @Test
    public void embarkCapacityResultsPreserveEngineResponseIndexes()
            throws Exception {
        AwaitingDecision decision =
                mock(AwaitingDecision.class);
        GameState gameState = mock(GameState.class);
        when(decision.getDecisionType())
                .thenReturn(
                        AwaitingDecisionType
                                .MULTIPLE_CHOICE);
        when(decision.getText()).thenReturn(
                "Choose capacity slot for AT-ST Pilot aboard Tempest Scout 1");
        when(decision.getAwaitingDecisionId())
                .thenReturn(77);
        when(decision.getDecisionParameters())
                .thenReturn(Map.of(
                        "results",
                        new String[]{
                            "Passenger", "Pilot"}));
        when(gameState.getCurrentPhase())
                .thenReturn(Phase.MOVE);
        when(gameState.getCurrentPlayerId())
                .thenReturn(PLAYER);
        when(gameState.getPlayersLatestTurnNumber(
                PLAYER)).thenReturn(3);

        var randoAi =
                new com.gempukku.swccgo.ai.models.rando
                        .RandoCalAi();
        Method randoBuilder =
                randoAi.getClass().getDeclaredMethod(
                        "buildEvaluatorContext",
                        String.class,
                        AwaitingDecision.class,
                        GameState.class,
                        boolean.class);
        randoBuilder.setAccessible(true);
        var randoContext =
                (com.gempukku.swccgo.ai.models.rando.evaluators
                        .DecisionContext)
                        randoBuilder.invoke(
                                randoAi, PLAYER,
                                decision, gameState, false);

        var chosenAi =
                new com.gempukku.swccgo.ai.models.chosenone
                        .TheChosenOneAi();
        Method chosenBuilder =
                chosenAi.getClass().getDeclaredMethod(
                        "buildEvaluatorContext",
                        String.class,
                        AwaitingDecision.class,
                        GameState.class,
                        boolean.class);
        chosenBuilder.setAccessible(true);
        var chosenContext =
                (com.gempukku.swccgo.ai.models.chosenone.evaluators
                        .DecisionContext)
                        chosenBuilder.invoke(
                                chosenAi, PLAYER,
                                decision, gameState, false);

        assertEquals(
                List.of("0", "1"),
                randoContext.getActionIds());
        assertEquals(
                List.of(
                        "Passenger capacity slot",
                        "Pilot capacity slot"),
                randoContext.getActionTexts());
        assertEquals(
                randoContext.getActionIds(),
                chosenContext.getActionIds());
        assertEquals(
                randoContext.getActionTexts(),
                chosenContext.getActionTexts());
        assertEquals(
                "1",
                new com.gempukku.swccgo.ai.models.rando.evaluators
                        .CombinedEvaluator()
                        .evaluateDecision(randoContext)
                        .getActionId());
        assertEquals(
                "1",
                new com.gempukku.swccgo.ai.models.chosenone.evaluators
                        .CombinedEvaluator()
                        .evaluateDecision(chosenContext)
                        .getActionId());
    }

    @Test
    public void classicActorsMoveToOpenThirdSiteWithTypedProgress() {
        for (String moverBlueprint : List.of(
                UNIQUE_BIKER_SCOUT_BP, AT_ST_BP)) {
            List<Outcome> startWinners = new ArrayList<>();
            List<Outcome> destinationWinners =
                    new ArrayList<>();
            for (Bot bot : Bot.values()) {
                Fixture fixture = fixture(bot);
                PhysicalCard forest =
                        prepareTwoClassicActors(fixture);
                int moverId = AT_ST_BP.equals(moverBlueprint)
                        ? AT_ST_ID : MOVING_BIKER_SCOUT_ID;
                PhysicalCard mover = card(
                        moverBlueprint, moverId,
                        Zone.AT_LOCATION, PLAYER);
                fixture.place(mover, fixture.cantina());
                when(fixture.modifiers().getLandspeed(
                        fixture.gameState(), mover))
                        .thenReturn(1.0f);
                when(fixture.modifiers()
                        .getLandspeedRequired(
                                fixture.gameState(),
                                mover, forest))
                        .thenReturn(1);
                if (AT_ST_BP.equals(moverBlueprint)) {
                    var moverCardTypes =
                            mover.getBlueprint()
                                    .getCardTypes();
                    when(fixture.modifiers().getCardTypes(
                            fixture.gameState(), mover))
                            .thenReturn(moverCardTypes);
                    when(fixture.modifiers().isPiloted(
                            fixture.gameState(), mover, false))
                            .thenReturn(true);
                    when(fixture.modifiers()
                            .getHighestAbilityPiloting(
                                    fixture.gameState(),
                                    mover, false, false))
                            .thenReturn(1.0f);
                    assertTrue(Filters.piloted.accepts(
                            fixture.gameState(),
                            fixture.modifiers(), mover));
                    assertTrue(Filters.vehicle.accepts(
                            fixture.gameState(),
                            fixture.modifiers(), mover));
                } else {
                    assertTrue(Filters.biker_scout.accepts(
                            fixture.gameState(),
                            fixture.modifiers(), mover));
                }

                assertTrue(
                        moverBlueprint,
                        fixture.analyzer()
                                .advancesRequiredCardDeployPrerequisiteAt(
                                        fixture.game(), PLAYER,
                                        mover, forest));
                assertTrue(
                        moverBlueprint,
                        fixture.analyzer()
                                .advancesRequiredCardDeployPrerequisiteByMovingTo(
                                        fixture.game(), PLAYER,
                                        mover, forest));
                assertTrue(
                        moverBlueprint,
                        Filters.canMoveToUsingLandspeed(
                                PLAYER, mover,
                                false, false, false,
                                0.0f, null)
                                .accepts(
                                        fixture.gameState(),
                                        fixture.modifiers(),
                                        forest));

                Decision startDecision =
                        Decision.topLevelMove(mover);
                String moveActionId =
                        startDecision.actionIds().get(0);
                Outcome moveCandidate = moveAdapter(
                        bot, fixture, startDecision,
                        moveActionId);
                TracedOutcome start = tracedCombined(
                        bot, fixture, startDecision);
                assertEquals(
                        "Move candidate " + moveCandidate
                                + ", combined "
                                + start.outcome(),
                        moveActionId,
                        start.outcome().actionId());
                assertHasTypedRule(
                        start.trace(), moveActionId,
                        REQUIRED_ENABLER_MOVE_START_RULE);
                assertContains(
                        start.outcome(),
                        REQUIRED_ENABLER_MOVE_START_RULE);
                startWinners.add(start.outcome());

                Decision destinationDecision =
                        Decision.moveDestination(
                                mover, forest,
                                fixture.platform());
                Outcome forestCandidate =
                        cardSelectionAdapter(
                                bot, fixture,
                                destinationDecision,
                                String.valueOf(FOREST_ID));
                Outcome closedCandidate =
                        cardSelectionAdapter(
                                bot, fixture,
                                destinationDecision,
                                String.valueOf(PLATFORM_ID));
                assertContains(
                        forestCandidate,
                        REQUIRED_ENABLER_MOVE_DESTINATION_RULE);
                assertNotContains(
                        closedCandidate,
                        REQUIRED_ENABLER_MOVE_DESTINATION_RULE);
                assertTrue(forestCandidate.score()
                        > closedCandidate.score());

                TracedOutcome destination =
                        tracedCombined(
                                bot, fixture,
                                destinationDecision);
                assertEquals(String.valueOf(FOREST_ID),
                        destination.outcome().actionId());
                assertHasTypedRule(
                        destination.trace(),
                        String.valueOf(FOREST_ID),
                        REQUIRED_ENABLER_MOVE_DESTINATION_RULE);
                assertLacksTypedRule(
                        destination.trace(),
                        String.valueOf(PLATFORM_ID),
                        REQUIRED_ENABLER_MOVE_DESTINATION_RULE);
                destinationWinners.add(
                        destination.outcome());
            }
            assertParity(startWinners);
            assertParity(destinationWinners);
        }
    }

    @Test
    public void classicMoveDoesNotScoreWhenItOnlyRelocatesSoleActor() {
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot);
            fixture.addReserve(card(
                    SECRET_BASE_CLASSIC_BP, SECRET_BASE_ID,
                    Zone.RESERVE_DECK, PLAYER));
            PhysicalCard forest = card(
                    FOREST_BP, FOREST_ID,
                    Zone.LOCATIONS, PLAYER);
            fixture.addLocation(forest);
            PhysicalCard mover = card(
                    UNIQUE_BIKER_SCOUT_BP,
                    MOVING_BIKER_SCOUT_ID,
                    Zone.AT_LOCATION, PLAYER);
            PhysicalCard platformScout = card(
                    BIKER_SCOUT_BP, PLATFORM_SCOUT_ID,
                    Zone.AT_LOCATION, PLAYER);
            fixture.place(mover, fixture.bunker());
            fixture.place(
                    platformScout, fixture.platform());
            setControls(
                    fixture, fixture.bunker(),
                    PLAYER, true);
            setControls(
                    fixture, fixture.platform(),
                    PLAYER, true);
            when(fixture.modifiers().getLandspeed(
                    fixture.gameState(), mover))
                    .thenReturn(1.0f);
            when(fixture.modifiers()
                    .getLandspeedRequired(
                            fixture.gameState(),
                            mover, forest))
                    .thenReturn(1);

            assertTrue(fixture.analyzer()
                    .advancesRequiredCardDeployPrerequisiteAt(
                            fixture.game(), PLAYER,
                            mover, forest));
            assertFalse(fixture.analyzer()
                    .advancesRequiredCardDeployPrerequisiteByMovingTo(
                            fixture.game(), PLAYER,
                            mover, forest));

            Decision destination =
                    Decision.moveDestination(
                            mover, forest);
            TracedOutcome tracedDestination =
                    tracedCombined(
                            bot, fixture, destination);
            assertLacksTypedRule(
                    tracedDestination.trace(),
                    String.valueOf(FOREST_ID),
                    REQUIRED_ENABLER_MOVE_DESTINATION_RULE);

            Decision start = Decision.topLevelMove(mover);
            TracedOutcome tracedStart =
                    tracedCombined(bot, fixture, start);
            assertLacksTypedRule(
                    tracedStart.trace(),
                    start.actionIds().get(0),
                    REQUIRED_ENABLER_MOVE_START_RULE);
        }
    }

    @Test
    public void soleClassicActorCanSafelyRelocateWithoutBreakingThreeSiteCount() {
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot);
            CountedRelocation formation =
                    prepareThreeCountedClassicSites(fixture);
            when(fixture.modifiers().getLandspeedRequired(
                    fixture.gameState(), formation.mover(),
                    formation.openEndorSite())).thenReturn(1);
            when(fixture.modifiers().getLandspeedRequired(
                    fixture.gameState(), formation.mover(),
                    fixture.cantina())).thenReturn(1);

            assertEquals(
                    ObjectiveAnalyzer.FlipGateFormationRole
                            .LAST_REQUIRED_ACTOR,
                    fixture.analyzer()
                            .classifyGateFormationPieceIfRemoved(
                                    fixture.game(), PLAYER,
                                    formation.mover()));
            assertTrue(fixture.analyzer()
                    .preservesRequiredCardDeployPrerequisiteByMovingTo(
                            fixture.game(), PLAYER,
                            formation.mover(),
                            formation.openEndorSite()));
            assertFalse(fixture.analyzer()
                    .preservesRequiredCardDeployPrerequisiteByMovingTo(
                            fixture.game(), PLAYER,
                            formation.mover(),
                            fixture.cantina()));
            assertTrue(fixture.analyzer()
                    .hasSafeRequiredCardDeployActorLandspeedDestination(
                            fixture.game(), PLAYER,
                            formation.mover()));

            Decision parent =
                    Decision.topLevelMove(formation.mover());
            String parentMoveId = parent.actionIds().get(0);
            Outcome parentCandidate = moveAdapter(
                    bot, fixture, parent, parentMoveId);
            assertNotContains(
                    parentCandidate,
                    COUNTED_FORMATION_HOLD_RULE);
            TracedOutcome parentResult =
                    tracedCombined(bot, fixture, parent);
            assertLacksTypedRule(
                    parentResult.trace(), parentMoveId,
                    COUNTED_FORMATION_HOLD_RULE);

            Decision child = Decision.moveDestination(
                    formation.mover(),
                    formation.openEndorSite(),
                    fixture.cantina());
            Outcome preserving = cardSelectionAdapterForMover(
                    bot, fixture, child,
                    String.valueOf(
                            formation.openEndorSite()
                                    .getCardId()),
                    formation.mover());
            Outcome nonPreserving =
                    cardSelectionAdapterForMover(
                            bot, fixture, child,
                            String.valueOf(
                                    fixture.cantina()
                                            .getCardId()),
                            formation.mover());
            assertNotContains(
                    preserving,
                    COUNTED_FORMATION_HOLD_RULE);
            assertNotContains(
                    preserving,
                    HUNT_DOWN_RUNTIME_ACTOR_DESTINATION_HOLD);
            assertContains(
                    nonPreserving,
                    COUNTED_FORMATION_HOLD_RULE);

            TracedOutcome childResult =
                    tracedCombined(bot, fixture, child);
            assertEquals(
                    String.valueOf(
                            formation.openEndorSite()
                                    .getCardId()),
                    childResult.outcome().actionId());
            assertLacksTypedRule(
                    childResult.trace(),
                    String.valueOf(
                            formation.openEndorSite()
                                    .getCardId()),
                    COUNTED_FORMATION_HOLD_RULE);
            assertHasTypedRule(
                    childResult.trace(),
                    String.valueOf(
                            fixture.cantina().getCardId()),
                    COUNTED_FORMATION_HOLD_RULE);
        }
    }

    @Test
    public void soleClassicActorRemainsHeldWithoutSafeQualifyingRoute() {
        for (boolean unsafeQualifyingRoute
                : List.of(false, true)) {
            for (Bot bot : Bot.values()) {
                Fixture fixture = fixture(bot);
                CountedRelocation formation =
                        prepareThreeCountedClassicSites(fixture);
                PhysicalCard onlyRoute =
                        unsafeQualifyingRoute
                                ? formation.openEndorSite()
                                : fixture.cantina();
                when(fixture.modifiers()
                        .getLandspeedRequired(
                                fixture.gameState(),
                                formation.mover(),
                                onlyRoute)).thenReturn(1);
                if (unsafeQualifyingRoute) {
                    when(fixture.modifiers()
                            .getTotalPowerAtLocation(
                                    fixture.gameState(),
                                    onlyRoute, OPPONENT,
                                    false, false))
                            .thenReturn(12.0f);
                    assertTrue(fixture.analyzer()
                            .preservesRequiredCardDeployPrerequisiteByMovingTo(
                                    fixture.game(), PLAYER,
                                    formation.mover(),
                                    onlyRoute));
                    assertNotNull(
                            FormationSafety
                                    .vetoMoveDestination(
                                            fixture.game(),
                                            fixture.gameState(),
                                            PLAYER,
                                            formation.mover(),
                                            onlyRoute));
                } else {
                    assertFalse(fixture.analyzer()
                            .preservesRequiredCardDeployPrerequisiteByMovingTo(
                                    fixture.game(), PLAYER,
                                    formation.mover(),
                                    onlyRoute));
                }

                assertFalse(fixture.analyzer()
                        .hasSafeRequiredCardDeployActorLandspeedDestination(
                                fixture.game(), PLAYER,
                                formation.mover()));

                Decision parent =
                        Decision.topLevelMove(
                                formation.mover());
                Outcome parentCandidate = moveAdapter(
                        bot, fixture, parent,
                        parent.actionIds().get(0));
                assertContains(
                        parentCandidate,
                        COUNTED_FORMATION_HOLD_RULE);
                assertEquals(
                        "pass",
                        combined(
                                bot, fixture,
                                parent).actionId());
            }
        }
    }

    @Test
    public void duplicateBlueprintMoverResolutionBindsTheChosenPhysicalCopy() {
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot);
            PhysicalCard forest =
                    prepareTwoClassicActors(fixture);
            PhysicalCard chosenCopy = card(
                    BIKER_SCOUT_BP,
                    DUPLICATE_BIKER_SCOUT_ID,
                    Zone.AT_LOCATION, PLAYER);
            fixture.place(chosenCopy, fixture.cantina());

            MovePhysicalCardResolver.ResolvedMover resolved =
                    MovePhysicalCardResolver.resolveOnTable(
                            fixture.permanents(), PLAYER,
                            BIKER_SCOUT_BP,
                            chosenCopy.getCardId());

            assertNotNull(resolved);
            assertSame(chosenCopy, resolved.card());
            assertSame(fixture.cantina(), resolved.origin());

            Decision destination =
                    Decision.moveDestination(
                            chosenCopy, forest);
            Outcome forestChoice =
                    cardSelectionAdapterForMover(
                            bot, fixture, destination,
                            String.valueOf(
                                    forest.getCardId()),
                            chosenCopy);
            assertContains(
                    forestChoice,
                    REQUIRED_ENABLER_MOVE_DESTINATION_RULE);
        }
    }

    @Test
    public void botMoveLatchCarriesExactPhysicalCopyIntoChildScoring()
            throws Exception {
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot);
            PhysicalCard forest =
                    prepareTwoClassicActors(fixture);
            PhysicalCard chosenCopy = card(
                    BIKER_SCOUT_BP,
                    DUPLICATE_BIKER_SCOUT_ID,
                    Zone.AT_LOCATION, PLAYER);
            fixture.place(chosenCopy, fixture.cantina());
            when(fixture.gameState().getCurrentPhase())
                    .thenReturn(Phase.MOVE);

            Decision parent =
                    Decision.topLevelMove(chosenCopy);
            Object ai;
            Object parentContext;
            Object selected;
            if (bot == Bot.RANDO) {
                ai = new com.gempukku.swccgo.ai.models.rando
                        .RandoCalAi();
                var typedParent = randoContext(
                        fixture, parent);
                typedParent.setActionIds(
                        List.of("0", "pass"));
                parentContext = typedParent;
                selected =
                        new com.gempukku.swccgo.ai.models.rando
                                .evaluators.EvaluatedAction(
                                "0",
                                com.gempukku.swccgo.ai.models.rando
                                        .evaluators.ActionType.MOVE,
                                0.0f, "selected physical mover");
            } else {
                ai = new com.gempukku.swccgo.ai.models.chosenone
                        .TheChosenOneAi();
                var typedParent = chosenContext(
                        fixture, parent);
                typedParent.setActionIds(
                        List.of("0", "pass"));
                parentContext = typedParent;
                selected =
                        new com.gempukku.swccgo.ai.models.chosenone
                                .evaluators.EvaluatedAction(
                                "0",
                                com.gempukku.swccgo.ai.models.chosenone
                                        .evaluators.ActionType.MOVE,
                                0.0f, "selected physical mover");
            }

            Method remember = ai.getClass()
                    .getDeclaredMethod(
                            "rememberSelectedMoveCard",
                            parentContext.getClass(),
                            selected.getClass(),
                            AwaitingDecision.class);
            remember.setAccessible(true);
            remember.invoke(
                    ai, parentContext, selected,
                    engineRuleActionDecision(
                        chosenCopy,
                        "Move using landspeed"));

            Decision child = Decision.moveDestination(
                    chosenCopy, forest, fixture.cantina());
            AwaitingDecision childDecision =
                    awaitingDecision(child, 88);
            Method builder = ai.getClass()
                    .getDeclaredMethod(
                            "buildEvaluatorContext",
                            String.class,
                            AwaitingDecision.class,
                            GameState.class,
                            boolean.class);
            builder.setAccessible(true);
            Object built = builder.invoke(
                    ai, PLAYER, childDecision,
                    fixture.gameState(), false);
            Method getExtra = built.getClass()
                    .getMethod("getExtra", String.class);
            assertEquals(
                    chosenCopy.getCardId(),
                    getExtra.invoke(
                            built,
                            MovePhysicalCardResolver
                                    .MOVER_CARD_ID_EXTRA));
            assertEquals(
                    chosenCopy.getPermanentCardId(),
                    getExtra.invoke(
                            built,
                            BhbmForceDripUrgencyFactsReader
                                .ACTION_SOURCE_PERMANENT_CARD_ID_EXTRA));

            Outcome forestChoice =
                    builtCardSelectionOutcome(
                            bot, fixture, built,
                            String.valueOf(FOREST_ID));
            assertContains(
                    forestChoice,
                    REQUIRED_ENABLER_MOVE_DESTINATION_RULE);

            Object next = builder.invoke(
                    ai, PLAYER, childDecision,
                    fixture.gameState(), false);
            assertNull(getExtra.invoke(
                    next,
                    MovePhysicalCardResolver
                            .MOVER_CARD_ID_EXTRA));
            assertNull(getExtra.invoke(
                    next,
                    BhbmForceDripUrgencyFactsReader
                        .ACTION_SOURCE_PERMANENT_CARD_ID_EXTRA));
            assertNotContains(
                    builtCardSelectionOutcome(
                            bot, fixture, next,
                            String.valueOf(FOREST_ID)),
                    REQUIRED_ENABLER_MOVE_DESTINATION_RULE);
        }
    }

    @Test
    public void dyerPrefersProtectionSiteWithoutForbiddingOtherMoves() {
        for (boolean flipped : List.of(false, true)) {
            List<Outcome> safeWinners =
                    new ArrayList<>();
            for (Bot bot : Bot.values()) {
                Fixture fixture = fixture(bot, flipped);
                PhysicalCard forest = card(
                        FOREST_BP, FOREST_ID,
                        Zone.LOCATIONS, PLAYER);
                PhysicalCard backDoor = card(
                        BACK_DOOR_BP, BACK_DOOR_ID,
                        Zone.LOCATIONS, PLAYER);
                fixture.addLocation(forest);
                fixture.addLocation(backDoor);
                PhysicalCard rumors = card(
                        OMINOUS_BP, OMINOUS_ID,
                        Zone.SIDE_OF_TABLE, PLAYER);
                fixture.addActivePermanent(rumors);
                when(fixture.modifiers().mayNotBeCanceled(
                        fixture.gameState(), rumors))
                        .thenReturn(true);
                fixture.setOpponentControls(
                        fixture.platform(), true);
                fixture.setOpponentControls(
                        backDoor, true);
                PhysicalCard dyer = card(
                        DYER_BP, DYER_ID,
                        Zone.AT_LOCATION, PLAYER);
                fixture.place(dyer, fixture.bunker());
                when(fixture.modifiers().getLandspeed(
                        fixture.gameState(), dyer))
                        .thenReturn(1.0f);
                when(fixture.modifiers()
                        .getLandspeedRequired(
                                fixture.gameState(),
                                dyer, forest))
                        .thenReturn(1);

                assertEquals(
                        ObjectiveAnalyzer.FlipGateFormationRole
                                .REQUIRED_CARD_RETENTION_DEFENDER,
                        fixture.analyzer()
                                .classifyGateFormationPieceIfRemoved(
                                        fixture.game(), PLAYER,
                                        dyer));
                assertTrue(fixture.analyzer()
                        .preservesRequiredCardCancelPreventionAt(
                                fixture.game(), PLAYER,
                                dyer, forest));
                assertFalse(fixture.analyzer()
                        .preservesRequiredCardCancelPreventionAt(
                                fixture.game(), PLAYER,
                                dyer, fixture.cantina()));

                Decision destination =
                        Decision.moveDestination(
                                dyer, forest,
                                fixture.cantina());
                Outcome safe = cardSelectionAdapter(
                        bot, fixture, destination,
                        String.valueOf(FOREST_ID));
                assertFalse(safe.hardVeto());
                assertNotContains(
                        safe, REQUIRED_RETENTION_MOVE_RULE);

                Outcome leavesDefense =
                        cardSelectionAdapter(
                                bot, fixture,
                                destination,
                                String.valueOf(CANTINA_ID));
                assertFalse(leavesDefense.hardVeto());
                assertContains(
                        leavesDefense,
                        REQUIRED_RETENTION_MOVE_RULE);

                TracedOutcome winner = tracedCombined(
                        bot, fixture, destination);
                assertEquals(String.valueOf(FOREST_ID),
                        winner.outcome().actionId());
                safeWinners.add(winner.outcome());

                Decision start =
                        Decision.topLevelMove(dyer);
                Outcome startMove = moveAdapter(
                        bot, fixture, start,
                        start.actionIds().get(0));
                assertFalse(startMove.hardVeto());
                assertNotContains(
                        startMove,
                        REQUIRED_RETENTION_MOVE_RULE);
            }
            assertParity(safeWinners);
        }
    }

    @Test
    public void dyerParentMoveStaysHeldWhenOnlyProtectionRouteIsUnsafe() {
        for (boolean flipped : List.of(false, true)) {
            for (Bot bot : Bot.values()) {
                Fixture fixture = fixture(bot, flipped);
                DyerRelocationRoutes routes =
                        prepareDyerRelocationRoutes(
                                fixture, false);

                assertNotNull(FormationSafety
                        .vetoMoveDestination(
                                fixture.game(),
                                fixture.gameState(), PLAYER,
                                routes.dyer(),
                                routes.unsafeForest()));
                assertTrue(fixture.analyzer()
                        .preservesRequiredCardCancelPreventionAt(
                                fixture.game(), PLAYER,
                                routes.dyer(),
                                routes.unsafeForest()));

                Decision start =
                        Decision.topLevelMove(routes.dyer());
                Outcome move = moveAdapter(
                        bot, fixture, start,
                        start.actionIds().get(0));
                assertTrue(
                        move.toString(),
                        move.score() < -100.0f);
                assertContains(
                        move, REQUIRED_RETENTION_MOVE_RULE);
                assertEquals(
                        "pass",
                        combined(bot, fixture, start)
                                .actionId());
            }
        }
    }

    @Test
    public void dyerParentMoveRemainsAvailableWithMixedSafeAndUnsafeRoutes() {
        for (boolean flipped : List.of(false, true)) {
            List<Outcome> parentWinners =
                    new ArrayList<>();
            List<Outcome> childWinners =
                    new ArrayList<>();
            for (Bot bot : Bot.values()) {
                Fixture fixture = fixture(bot, flipped);
                DyerRelocationRoutes routes =
                        prepareDyerRelocationRoutes(
                                fixture, true);

                assertNotNull(FormationSafety
                        .vetoMoveDestination(
                                fixture.game(),
                                fixture.gameState(), PLAYER,
                                routes.dyer(),
                                routes.unsafeForest()));
                assertNull(FormationSafety
                        .vetoMoveDestination(
                                fixture.game(),
                                fixture.gameState(), PLAYER,
                                routes.dyer(),
                                routes.safeBackDoor()));
                assertTrue(fixture.analyzer()
                        .preservesRequiredCardCancelPreventionAt(
                                fixture.game(), PLAYER,
                                routes.dyer(),
                                routes.safeBackDoor()));

                Decision start =
                        Decision.topLevelMove(routes.dyer());
                Outcome parent = moveAdapter(
                        bot, fixture, start,
                        start.actionIds().get(0));
                assertFalse(parent.hardVeto());
                assertNotContains(
                        parent, REQUIRED_RETENTION_MOVE_RULE);
                parentWinners.add(parent);

                Decision destination =
                        Decision.moveDestination(
                                routes.dyer(),
                                routes.unsafeForest(),
                                routes.safeBackDoor());
                Outcome unsafe = cardSelectionAdapter(
                        bot, fixture, destination,
                        String.valueOf(
                                routes.unsafeForest()
                                        .getCardId()));
                assertTrue(unsafe.hardVeto());
                Outcome child = combined(
                        bot, fixture, destination);
                assertEquals(
                        String.valueOf(
                                routes.safeBackDoor()
                                        .getCardId()),
                        child.actionId());
                assertFalse(child.hardVeto());
                childWinners.add(child);
            }
            assertParity(parentWinners);
            assertParity(childWinners);
        }
    }

    @Test
    public void enclosedBikerScoutCountsAtItsLogicalControlWithSite() {
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot);
            PhysicalCard secretBase = card(
                    SECRET_BASE_CLASSIC_BP,
                    SECRET_BASE_ID,
                    Zone.RESERVE_DECK, PLAYER);
            fixture.addReserve(secretBase);
            PhysicalCard forest = card(
                    FOREST_BP, FOREST_ID,
                    Zone.LOCATIONS, PLAYER);
            fixture.addLocation(forest);
            PhysicalCard bunkerScout = card(
                    BIKER_SCOUT_BP, BUNKER_SCOUT_ID,
                    Zone.AT_LOCATION, PLAYER);
            PhysicalCard platformScout = card(
                    BIKER_SCOUT_BP, PLATFORM_SCOUT_ID,
                    Zone.AT_LOCATION, PLAYER);
            fixture.place(
                    bunkerScout, fixture.bunker());
            fixture.place(
                    platformScout, fixture.platform());
            PhysicalCard carrier = card(
                    AT_ST_BP, ENCLOSING_AT_ST_ID,
                    Zone.AT_LOCATION, PLAYER);
            fixture.place(carrier, forest);
            PhysicalCard enclosedScout = card(
                    UNIQUE_BIKER_SCOUT_BP,
                    ENCLOSED_BIKER_SCOUT_ID,
                    Zone.ATTACHED, PLAYER);
            fixture.addActivePermanent(enclosedScout);
            when(enclosedScout.getAttachedTo())
                    .thenReturn(carrier);
            when(fixture.modifiers()
                    .getLocationThatCardIsAt(
                            fixture.gameState(),
                            enclosedScout))
                    .thenReturn(forest);
            setControls(
                    fixture, fixture.bunker(),
                    PLAYER, true);
            setControls(
                    fixture, fixture.platform(),
                    PLAYER, true);
            setControls(
                    fixture, forest,
                    PLAYER, true);

            assertNull(enclosedScout.getAtLocation());
            assertNull(fixture.modifiers()
                    .getLocationThatCardIsPresentAt(
                            fixture.gameState(),
                            enclosedScout));
            assertSame(forest, fixture.modifiers()
                    .getLocationThatCardIsAt(
                            fixture.gameState(),
                            enclosedScout));
            assertFalse(fixture.analyzer()
                    .isMissingRequiredCardDeployEnablerAt(
                            fixture.game(), PLAYER,
                            forest));
            assertTrue(fixture.analyzer()
                    .isRequiredOnTableCardPullRouteReady(
                            fixture.game(), PLAYER,
                            secretBase));
        }
    }

    @Test
    public void bikerScoutPilotAndItsAtStAreOneRequiredFormationGroup() {
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot);
            PhysicalCard forest =
                    prepareTwoClassicActors(fixture);
            PhysicalCard atSt = card(
                    AT_ST_BP, AT_ST_ID,
                    Zone.AT_LOCATION, PLAYER);
            fixture.place(atSt, forest);
            PhysicalCard bikerPilot = card(
                    UNIQUE_BIKER_SCOUT_BP,
                    MOVING_BIKER_SCOUT_ID,
                    Zone.ATTACHED, PLAYER);
            fixture.addActivePermanent(bikerPilot);
            when(bikerPilot.isPilotOf()).thenReturn(true);
            when(bikerPilot.getAttachedTo())
                    .thenReturn(atSt);
            when(atSt.getCardsAttached())
                    .thenReturn(List.of(bikerPilot));
            when(fixture.modifiers()
                    .getLocationThatCardIsAt(
                            fixture.gameState(),
                            bikerPilot))
                    .thenReturn(forest);
            when(fixture.modifiers().isPiloted(
                    fixture.gameState(), atSt, false))
                    .thenReturn(true);
            when(fixture.modifiers().hasPermanentPilot(
                    fixture.gameState(), atSt))
                    .thenReturn(false);
            setControls(fixture, forest, PLAYER, true);
            PhysicalCard disposable = card(
                    DISPOSABLE_DROID_BP,
                    DISPOSABLE_DROID_ID,
                    Zone.AT_LOCATION, PLAYER);
            fixture.place(disposable, forest);

            assertEquals(
                    ObjectiveAnalyzer.FlipGateFormationRole
                            .LAST_REQUIRED_ACTOR,
                    fixture.analyzer()
                            .classifyGateFormationPieceIfRemoved(
                                    fixture.game(), PLAYER,
                                    bikerPilot));
            assertEquals(
                    ObjectiveAnalyzer.FlipGateFormationRole
                            .LAST_REQUIRED_ACTOR,
                    fixture.analyzer()
                            .classifyGateFormationPieceIfRemoved(
                                    fixture.game(), PLAYER,
                                    atSt));

            for (PhysicalCard protectedCard
                    : List.of(bikerPilot, atSt)) {
                Decision forfeit = Decision.forfeit(
                        protectedCard, disposable);
                TracedOutcome forfeitResult =
                        tracedCombined(
                                bot, fixture, forfeit);
                assertEquals(
                        String.valueOf(
                                DISPOSABLE_DROID_ID),
                        forfeitResult.outcome()
                                .actionId());
                assertHasTypedRuleDelta(
                        forfeitResult.trace(),
                        String.valueOf(
                                protectedCard.getCardId()),
                        REQUIRED_FORMATION_FORFEIT_RULE,
                        TraceDomainId.OBJECTIVE_INTENT,
                        -300.0f);
            }

            Decision disembark =
                    Decision.disembark(bikerPilot);
            TracedOutcome disembarkResult =
                    tracedCombined(
                            bot, fixture, disembark);
            assertEquals("pass",
                    disembarkResult.outcome()
                            .actionId());
            assertHasTypedRule(
                    disembarkResult.trace(),
                    "disembark",
                    REQUIRED_FORMATION_HOLD_RULE);

            Decision hostMove =
                    Decision.topLevelMove(atSt);
            Outcome movingHost = moveAdapter(
                    bot, fixture, hostMove,
                    hostMove.actionIds().get(0));
            assertTrue(
                    movingHost.toString(),
                    movingHost.score() < -100.0f);
            assertContains(
                    movingHost,
                    COUNTED_FORMATION_HOLD_RULE);
            assertEquals("pass",
                    combined(
                            bot, fixture,
                            hostMove).actionId());
        }
    }

    @Test
    public void carrierOfSoleEnclosedBikerScoutIsTheRequiredActorGroup() {
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot);
            PhysicalCard forest =
                    prepareTwoClassicActors(fixture);
            PhysicalCard carrier = card(
                    NON_ACTOR_CARRIER_BP,
                    NON_ACTOR_CARRIER_ID,
                    Zone.AT_LOCATION, PLAYER);
            fixture.place(carrier, forest);
            PhysicalCard enclosedScout = card(
                    UNIQUE_BIKER_SCOUT_BP,
                    CARRIED_BIKER_SCOUT_ID,
                    Zone.ATTACHED, PLAYER);
            fixture.addActivePermanent(enclosedScout);
            when(enclosedScout.getAttachedTo())
                    .thenReturn(carrier);
            when(carrier.getCardsAttached())
                    .thenReturn(List.of(enclosedScout));
            when(fixture.modifiers()
                    .getLocationThatCardIsAt(
                            fixture.gameState(),
                            enclosedScout))
                    .thenReturn(forest);
            setControls(fixture, forest, PLAYER, true);
            PhysicalCard disposable = card(
                    DISPOSABLE_DROID_BP,
                    DISPOSABLE_DROID_ID,
                    Zone.AT_LOCATION, PLAYER);
            fixture.place(disposable, forest);

            assertFalse(Filters.or(
                    Filters.biker_scout,
                    Filters.and(
                            Filters.AT_ST,
                            Filters.piloted))
                    .accepts(
                            fixture.gameState(),
                            fixture.modifiers(),
                            carrier));
            assertNull(enclosedScout.getAtLocation());
            assertNull(fixture.modifiers()
                    .getLocationThatCardIsPresentAt(
                            fixture.gameState(),
                            enclosedScout));
            assertSame(forest, fixture.modifiers()
                    .getLocationThatCardIsAt(
                            fixture.gameState(),
                            enclosedScout));
            assertEquals(
                    ObjectiveAnalyzer.FlipGateFormationRole
                            .LAST_REQUIRED_ACTOR,
                    fixture.analyzer()
                            .classifyGateFormationPieceIfRemoved(
                                    fixture.game(), PLAYER,
                                    carrier));

            Decision forfeit =
                    Decision.forfeit(
                            carrier, disposable);
            TracedOutcome forfeitResult =
                    tracedCombined(
                            bot, fixture, forfeit);
            assertEquals(
                    String.valueOf(
                            DISPOSABLE_DROID_ID),
                    forfeitResult.outcome()
                            .actionId());
                assertHasTypedRuleDelta(
                        forfeitResult.trace(),
                        String.valueOf(
                                NON_ACTOR_CARRIER_ID),
                        REQUIRED_FORMATION_FORFEIT_RULE,
                        TraceDomainId.OBJECTIVE_INTENT,
                        -300.0f);

            Decision carrierMove =
                    Decision.topLevelMove(carrier);
            Outcome movingCarrier = moveAdapter(
                    bot, fixture, carrierMove,
                    carrierMove.actionIds().get(0));
            assertTrue(
                    movingCarrier.toString(),
                    movingCarrier.score() < -100.0f);
            assertContains(
                    movingCarrier,
                    COUNTED_FORMATION_HOLD_RULE);
            assertEquals("pass",
                    combined(
                            bot, fixture,
                            carrierMove).actionId());
        }
    }

    @Test
    public void soleAttachedPilotIsHeldForCasualtyForfeitAndDisembark() {
        List<Outcome> forfeitWinners = new ArrayList<>();
        List<Outcome> disembarkWinners = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot);
            ClassicAtStFormation formation =
                    prepareClassicAtStFormation(fixture);
            PhysicalCard disposable = card(
                    DISPOSABLE_DROID_BP, DISPOSABLE_DROID_ID,
                    Zone.AT_LOCATION, PLAYER);
            fixture.place(disposable, formation.forest());

            assertNull(formation.pilot().getAtLocation());
            assertNull(fixture.modifiers()
                    .getLocationThatCardIsPresentAt(
                            fixture.gameState(),
                            formation.pilot()));
            assertSame(formation.forest(),
                    fixture.modifiers()
                            .getLocationThatCardIsAt(
                                    fixture.gameState(),
                                    formation.pilot()));
            assertEquals(
                    ObjectiveAnalyzer.FlipGateFormationRole
                            .LAST_REQUIRED_ACTOR,
                    fixture.analyzer()
                            .classifyGateFormationPieceIfRemoved(
                                    fixture.game(), PLAYER,
                                    formation.pilot()));

            Decision forfeit = Decision.forfeit(
                    formation.pilot(), disposable);
            Outcome protectedPilot = cardSelectionAdapter(
                    bot, fixture, forfeit,
                    String.valueOf(AT_ST_PILOT_ID));
            assertContains(
                    protectedPilot,
                    REQUIRED_FORMATION_FORFEIT_RULE);
            TracedOutcome forfeitResult = tracedCombined(
                    bot, fixture, forfeit);
            assertEquals(String.valueOf(DISPOSABLE_DROID_ID),
                    forfeitResult.outcome().actionId());
            assertHasTypedRuleDelta(
                    forfeitResult.trace(),
                    String.valueOf(AT_ST_PILOT_ID),
                    REQUIRED_FORMATION_FORFEIT_RULE,
                    TraceDomainId.OBJECTIVE_INTENT,
                    -300.0f);
            forfeitWinners.add(forfeitResult.outcome());

            Decision disembark = Decision.disembark(
                    formation.pilot());
            Outcome disembarkCandidate = actionTextAdapter(
                    bot, fixture, disembark, "disembark");
            assertFalse(disembarkCandidate.hardVeto());
            assertContains(
                    disembarkCandidate,
                    COUNTED_FORMATION_HOLD_RULE);
            TracedOutcome disembarkResult = tracedCombined(
                    bot, fixture, disembark);
            assertEquals("pass",
                    disembarkResult.outcome().actionId());
            assertHasTypedRule(
                    disembarkResult.trace(), "disembark",
                    REQUIRED_FORMATION_HOLD_RULE);
            disembarkWinners.add(disembarkResult.outcome());
        }
        assertParity(forfeitWinners);
        assertParity(disembarkWinners);
    }

    @Test
    public void objectiveTutorParentRequiresAReadyReserveRoute() {
        for (Bot bot : Bot.values()) {
            Fixture allBlocked = fixture(bot);
            allBlocked.setOpponentControls(
                    allBlocked.platform(), true);
            PhysicalCard blockedReserve = card(
                    OMINOUS_BP, OMINOUS_ID,
                    Zone.RESERVE_DECK, PLAYER);
            allBlocked.addReserve(blockedReserve);
            allBlocked.addPermanent(card(
                    DISTRACTOR_BP, DISTRACTOR_SOURCE_ID,
                    Zone.SIDE_OF_TABLE, PLAYER));

            assertFalse(allBlocked.analyzer()
                    .isRequiredOnTableCardPullRouteReady(
                            allBlocked.game(), PLAYER,
                            blockedReserve));
            assertFalse(allBlocked.analyzer()
                    .objectivePullAdvancesRequiredOnTableCard(
                            allBlocked.game(), PLAYER,
                            "Endor Operations"));
            TracedOutcome blockedParent = tracedCombined(
                    bot, allBlocked, Decision.parentPull());
            assertLacksTypedRule(
                    blockedParent.trace(), "objective-pull",
                    REQUIRED_PULL_RULE);

            Fixture readyAfterBlockedHand = fixture(bot);
            readyAfterBlockedHand.setOpponentControls(
                    readyAfterBlockedHand.platform(), true);
            PhysicalCard blockedHand = card(
                    OMINOUS_BP, OMINOUS_ID,
                    Zone.HAND, PLAYER);
            PhysicalCard readyReserve = card(
                    OMINOUS_DIRECT_BP, READY_REQUIRED_ID,
                    Zone.RESERVE_DECK, PLAYER);
            readyAfterBlockedHand.addHand(blockedHand);
            readyAfterBlockedHand.addReserve(readyReserve);
            readyAfterBlockedHand.addPermanent(card(
                    DISTRACTOR_BP, DISTRACTOR_SOURCE_ID,
                    Zone.SIDE_OF_TABLE, PLAYER));

            assertFalse(readyAfterBlockedHand.analyzer()
                    .isRequiredOnTableCardPullRouteReady(
                            readyAfterBlockedHand.game(), PLAYER,
                            blockedHand));
            assertTrue(readyAfterBlockedHand.analyzer()
                    .isRequiredOnTableCardPullRouteReady(
                            readyAfterBlockedHand.game(), PLAYER,
                            readyReserve));
            assertTrue(readyAfterBlockedHand.analyzer()
                    .objectivePullAdvancesRequiredOnTableCard(
                            readyAfterBlockedHand.game(), PLAYER,
                            "Endor Operations"));
            TracedOutcome readyParent = tracedCombined(
                    bot, readyAfterBlockedHand,
                    Decision.parentPull());
            assertEquals("objective-pull",
                    readyParent.outcome().actionId());
            assertHasTypedRule(
                    readyParent.trace(), "objective-pull",
                    REQUIRED_PULL_RULE);
        }
    }

    @Test
    public void forceLossProtectsOnlyThePreferredStillNeededPhysicalCopy() {
        for (Bot bot : Bot.values()) {
            Fixture sole = fixture(bot);
            PhysicalCard soleRequired = card(
                    OMINOUS_DIRECT_BP, OMINOUS_ID,
                    Zone.HAND, PLAYER);
            PhysicalCard soleDistractor = card(
                    DISTRACTOR_BP, FORCE_LOSS_DISTRACTOR_ID,
                    Zone.HAND, PLAYER);
            sole.addHand(soleRequired);
            sole.addHand(soleDistractor);
            assertTrue(sole.analyzer()
                    .isPreferredRequiredCardForceLossCandidate(
                            sole.game(), PLAYER, soleRequired));
            assertBoundedForceLossPreferenceForBothRoutes(
                    bot, sole, soleRequired, soleDistractor);

            Fixture readyPrinting = fixture(bot);
            readyPrinting.setOpponentControls(
                    readyPrinting.platform(), true);
            PhysicalCard blocked = card(
                    OMINOUS_BP, OMINOUS_ID,
                    Zone.HAND, PLAYER);
            PhysicalCard ready = card(
                    OMINOUS_DIRECT_BP, SECOND_REQUIRED_ID,
                    Zone.HAND, PLAYER);
            readyPrinting.addHand(blocked);
            readyPrinting.addHand(ready);
            when(readyPrinting.modifiers().getDeployCost(
                    readyPrinting.gameState(), blocked))
                    .thenReturn(0.0f);
            when(readyPrinting.modifiers().getDeployCost(
                    readyPrinting.gameState(), ready))
                    .thenReturn(5.0f);
            assertFalse(readyPrinting.analyzer()
                    .isPreferredRequiredCardForceLossCandidate(
                            readyPrinting.game(), PLAYER, blocked));
            assertTrue(readyPrinting.analyzer()
                    .isPreferredRequiredCardForceLossCandidate(
                            readyPrinting.game(), PLAYER, ready));
            assertBoundedForceLossPreferenceForBothRoutes(
                    bot, readyPrinting, ready, blocked);

            Fixture cheaperPrinting = fixture(bot);
            cheaperPrinting.setOpponentControls(
                    cheaperPrinting.platform(), true);
            PhysicalCard expensive = card(
                    OMINOUS_BP, OMINOUS_ID,
                    Zone.HAND, PLAYER);
            PhysicalCard cheaper = card(
                    OMINOUS_BP, SECOND_REQUIRED_ID,
                    Zone.HAND, PLAYER);
            cheaperPrinting.addHand(expensive);
            cheaperPrinting.addHand(cheaper);
            when(cheaperPrinting.modifiers().getDeployCost(
                    cheaperPrinting.gameState(), expensive))
                    .thenReturn(4.0f);
            when(cheaperPrinting.modifiers().getDeployCost(
                    cheaperPrinting.gameState(), cheaper))
                    .thenReturn(1.0f);
            assertTrue(cheaperPrinting.analyzer()
                    .isPreferredRequiredCardForceLossCandidate(
                            cheaperPrinting.game(), PLAYER, cheaper));
            assertFalse(cheaperPrinting.analyzer()
                    .isPreferredRequiredCardForceLossCandidate(
                            cheaperPrinting.game(), PLAYER, expensive));
            assertBoundedForceLossPreferenceForBothRoutes(
                    bot, cheaperPrinting, cheaper, expensive);
        }
    }

    @Test
    public void forceLossProtectsSoleRequiredEffectAcrossRecyclableZones() {
        for (Bot bot : Bot.values()) {
            for (Zone zone : List.of(
                    Zone.USED_PILE,
                    Zone.TOP_OF_UNRESOLVED_DESTINY_DRAW,
                    Zone.SABACC_HAND)) {
                Fixture fixture = fixture(bot);
                PhysicalCard required = card(
                        OMINOUS_DIRECT_BP, OMINOUS_ID,
                        zone, PLAYER);
                PhysicalCard disposableForce = card(
                        DISTRACTOR_BP,
                        FORCE_LOSS_DISTRACTOR_ID,
                        Zone.FORCE_PILE, PLAYER);
                fixture.cardsById().put(
                        required.getCardId(), required);
                fixture.cardsById().put(
                        disposableForce.getCardId(),
                        disposableForce);
                when(fixture.gameState().getForcePile(PLAYER))
                        .thenReturn(List.of(
                                disposableForce));
                when(fixture.gameState()
                        .getTopOfForcePile(PLAYER))
                        .thenReturn(disposableForce);
                if (zone == Zone.USED_PILE) {
                    when(fixture.gameState()
                            .getUsedPile(PLAYER))
                            .thenReturn(List.of(required));
                    when(fixture.gameState()
                            .getTopOfUsedPile(PLAYER))
                            .thenReturn(required);
                } else if (zone
                        == Zone.TOP_OF_UNRESOLVED_DESTINY_DRAW) {
                    when(fixture.gameState()
                            .getUnresolvedDestinyDraw(PLAYER))
                            .thenReturn(List.of(required));
                    when(fixture.gameState()
                            .getTopOfUnresolvedDestinyDraws(
                                    PLAYER))
                            .thenReturn(required);
                } else {
                    when(fixture.gameState()
                            .getSabaccHand(PLAYER))
                            .thenReturn(List.of(required));
                }

                assertTrue(fixture.analyzer()
                        .isPreferredRequiredCardForceLossCandidate(
                                fixture.game(), PLAYER,
                                required));
                assertBoundedForceLossPreferenceForBothRoutes(
                        bot, fixture,
                        required, disposableForce);
            }
        }
    }

    @Test
    public void forceLossMaySpendOfferedUsedCopyWhenBetterCopySurvives() {
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot);
            PhysicalCard offeredUsed = card(
                    OMINOUS_DIRECT_BP, OMINOUS_ID,
                    Zone.USED_PILE, PLAYER);
            PhysicalCard betterReserve = card(
                    OMINOUS_DIRECT_BP, SECOND_REQUIRED_ID,
                    Zone.RESERVE_DECK, PLAYER);
            PhysicalCard disposableForce = card(
                    DISTRACTOR_BP, FORCE_LOSS_DISTRACTOR_ID,
                    Zone.FORCE_PILE, PLAYER);
            fixture.addReserve(betterReserve);
            fixture.cardsById().put(
                    offeredUsed.getCardId(),
                    offeredUsed);
            fixture.cardsById().put(
                    disposableForce.getCardId(),
                    disposableForce);
            when(fixture.gameState().getUsedPile(PLAYER))
                    .thenReturn(List.of(offeredUsed));
            when(fixture.gameState().getForcePile(PLAYER))
                    .thenReturn(List.of(disposableForce));
            when(fixture.modifiers().getDeployCost(
                    fixture.gameState(), offeredUsed))
                    .thenReturn(1.0f);
            when(fixture.modifiers().getDeployCost(
                    fixture.gameState(), betterReserve))
                    .thenReturn(0.0f);

            assertFalse(fixture.analyzer()
                    .isPreferredRequiredCardForceLossCandidate(
                            fixture.game(), PLAYER,
                            offeredUsed));
            assertTrue(fixture.analyzer()
                    .isPreferredRequiredCardForceLossCandidate(
                            fixture.game(), PLAYER,
                            betterReserve));
            for (boolean combinedRoute
                    : List.of(false, true)) {
                Decision decision = Decision.forceLoss(
                        combinedRoute,
                        offeredUsed, disposableForce);
                assertFalse(decision.cardIds().contains(
                        String.valueOf(
                                betterReserve.getCardId())));
                TracedOutcome result = tracedCombined(
                        bot, fixture, decision);
                assertEquals(
                        String.valueOf(
                                offeredUsed.getCardId()),
                        result.outcome().actionId());
                assertLacksTypedRule(
                        result.trace(),
                        String.valueOf(
                                offeredUsed.getCardId()),
                        FORCE_LOSS_OBJECTIVE_RULE);
            }
        }
    }

    @Test
    public void forceLossFallbackDoesNotReprotectOnTableRolesOrClosedCopies() {
        for (Bot bot : Bot.values()) {
            Fixture blockedHandReadyReserve = fixture(bot);
            blockedHandReadyReserve.setOpponentControls(
                    blockedHandReadyReserve.platform(), true);
            PhysicalCard blockedHand = card(
                    OMINOUS_BP, OMINOUS_ID,
                    Zone.HAND, PLAYER);
            PhysicalCard readyReserve = card(
                    OMINOUS_DIRECT_BP, SECOND_REQUIRED_ID,
                    Zone.RESERVE_DECK, PLAYER);
            blockedHandReadyReserve.addHand(blockedHand);
            blockedHandReadyReserve.addReserve(readyReserve);

            assertEquals(
                    ObjectiveAnalyzer.ObjectiveProgressCandidateRole
                            .REQUIRED_ON_TABLE_CARD,
                    blockedHandReadyReserve.analyzer()
                            .classifyPreFlipProgressCandidate(
                                    blockedHandReadyReserve.game(),
                                    PLAYER, blockedHand));
            assertFalse(blockedHandReadyReserve.analyzer()
                    .isPreferredRequiredCardForceLossCandidate(
                            blockedHandReadyReserve.game(), PLAYER,
                            blockedHand));
            assertTrue(blockedHandReadyReserve.analyzer()
                    .isPreferredRequiredCardForceLossCandidate(
                            blockedHandReadyReserve.game(), PLAYER,
                            readyReserve));

            Decision standalone = Decision.forceLoss(
                    false, blockedHand, readyReserve);
            TracedOutcome standaloneResult = tracedCombined(
                    bot, blockedHandReadyReserve, standalone);
            assertLacksTypedRule(
                    standaloneResult.trace(),
                    String.valueOf(OMINOUS_ID),
                    FORCE_LOSS_OBJECTIVE_RULE);
            assertHasTypedRule(
                    standaloneResult.trace(),
                    String.valueOf(SECOND_REQUIRED_ID),
                    FORCE_LOSS_OBJECTIVE_RULE);

            Decision battle = Decision.forceLoss(
                    true, blockedHand, readyReserve);
            TracedOutcome battleResult = tracedCombined(
                    bot, blockedHandReadyReserve, battle);
            assertHasTypedRule(
                    battleResult.trace(),
                    String.valueOf(SECOND_REQUIRED_ID),
                    FORCE_LOSS_OBJECTIVE_RULE);
            assertLacksTypedRule(
                    battleResult.trace(),
                    String.valueOf(OMINOUS_ID),
                    FORCE_LOSS_OBJECTIVE_RULE);
            Fixture activeCopy = fixture(bot);
            PhysicalCard tableCopy = card(
                    OMINOUS_DIRECT_BP, READY_REQUIRED_ID,
                    Zone.SIDE_OF_TABLE, PLAYER);
            activeCopy.addActivePermanent(tableCopy);
            PhysicalCard handCopy = card(
                    OMINOUS_BP, OMINOUS_ID,
                    Zone.HAND, PLAYER);
            PhysicalCard reserveCopy = card(
                    OMINOUS_DIRECT_BP, SECOND_REQUIRED_ID,
                    Zone.RESERVE_DECK, PLAYER);
            activeCopy.addHand(handCopy);
            activeCopy.addReserve(reserveCopy);

            assertTrue(activeCopy.analyzer()
                    .isRequiredCardActiveOnTable(
                            activeCopy.game(), "Ominous Rumors"));
            assertFalse(activeCopy.analyzer()
                    .isPreferredRequiredCardForceLossCandidate(
                            activeCopy.game(), PLAYER, handCopy));
            assertFalse(activeCopy.analyzer()
                    .isPreferredRequiredCardForceLossCandidate(
                            activeCopy.game(), PLAYER, reserveCopy));
            for (boolean combinedRoute : List.of(false, true)) {
                Decision closed = Decision.forceLoss(
                        combinedRoute, handCopy, reserveCopy);
                TracedOutcome closedResult = tracedCombined(
                        bot, activeCopy, closed);
                assertLacksTypedRule(
                        closedResult.trace(),
                        String.valueOf(OMINOUS_ID),
                        FORCE_LOSS_OBJECTIVE_RULE);
                assertLacksTypedRule(
                        closedResult.trace(),
                        String.valueOf(SECOND_REQUIRED_ID),
                        FORCE_LOSS_OBJECTIVE_RULE);
            }
        }
    }

    @Test
    public void terminalBunkerDefenseDeployChildBeatsPassAndSelfCloses() {
        List<Outcome> threatWinners = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture threatened = fixture(bot);
            PhysicalCard defender = card(
                    BIKER_SCOUT_BP, BIKER_SCOUT_ID,
                    Zone.HAND, PLAYER);
            threatened.addHand(defender);
            when(threatened.modifiers().hasAbility(
                    threatened.gameState(), defender, true))
                    .thenReturn(true);
            PhysicalCard threat = card(
                    DEACTIVATE_BP, DEACTIVATE_ID,
                    Zone.ATTACHED, OPPONENT);
            when(threat.getAttachedTo())
                    .thenReturn(threatened.bunker());
            threatened.addActivePermanent(threat);

            assertTrue(threatened.analyzer()
                    .advancesObjectiveHardLossDefenseAt(
                            threatened.game(), PLAYER,
                            defender, threatened.bunker()));
            Decision threatenedDeploy =
                    Decision.optionalDeployCandidate(
                            defender, threatened.bunker());
            TracedOutcome threatenedResult = tracedCombined(
                    bot, threatened, threatenedDeploy);
            assertEquals(threatenedResult.trace().getOperations().toString(),
                    String.valueOf(BUNKER_ID),
                    threatenedResult.outcome().actionId());
            assertHasTypedRule(
                    threatenedResult.trace(),
                    String.valueOf(BUNKER_ID),
                    HARD_LOSS_DEFENSE_RULE);
            threatWinners.add(threatenedResult.outcome());

            Fixture noThreat = fixture(bot);
            PhysicalCard ordinaryDefender = card(
                    BIKER_SCOUT_BP, BIKER_SCOUT_ID,
                    Zone.HAND, PLAYER);
            noThreat.addHand(ordinaryDefender);
            when(noThreat.modifiers().hasAbility(
                    noThreat.gameState(), ordinaryDefender, true))
                    .thenReturn(true);
            assertFalse(noThreat.analyzer()
                    .advancesObjectiveHardLossDefenseAt(
                            noThreat.game(), PLAYER,
                            ordinaryDefender, noThreat.bunker()));
            TracedOutcome closed = tracedCombined(
                    bot, noThreat,
                    Decision.optionalDeployCandidate(
                            ordinaryDefender,
                            noThreat.bunker()));
            assertLacksTypedRule(
                    closed.trace(), String.valueOf(BUNKER_ID),
                    HARD_LOSS_DEFENSE_RULE);
        }
        assertParity(threatWinners);
    }

    @Test
    public void passBeatsMovingSoleBunkerControlSourceForVAndLegacyRoutes() {
        for (String secretBaseBlueprint : List.of(
                SECRET_BASE_V_BP, SECRET_BASE_LEGACY_BP)) {
            List<Outcome> winners = new ArrayList<>();
            List<Outcome> moveCandidates = new ArrayList<>();
            for (Bot bot : Bot.values()) {
                Fixture fixture = fixture(bot);
                PhysicalCard undeployedSecretBase = card(
                        secretBaseBlueprint, SECRET_BASE_ID,
                        Zone.HAND, PLAYER);
                PhysicalCard bikerScout = card(
                        BIKER_SCOUT_BP, BIKER_SCOUT_ID,
                        Zone.AT_LOCATION, PLAYER);
                fixture.addHand(undeployedSecretBase);
                fixture.place(bikerScout, fixture.bunker());

                when(fixture.modifiers().controlsLocation(
                        fixture.gameState(), fixture.bunker(), PLAYER,
                        SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE))
                        .thenReturn(true);
                when(fixture.modifiers().hasAbility(
                        fixture.gameState(), bikerScout, true))
                        .thenReturn(true);
                when(fixture.modifiers().getTotalAbilityAtLocation(
                        fixture.gameState(), PLAYER, fixture.bunker()))
                        .thenReturn(2.0f);
                when(fixture.modifiers().getTotalPowerAtLocation(
                        fixture.gameState(), fixture.bunker(), PLAYER,
                        false, false)).thenReturn(2.0f);
                when(fixture.modifiers().getLandspeed(
                        fixture.gameState(), bikerScout)).thenReturn(1.0f);

                assertTrue(secretBaseBlueprint,
                        fixture.analyzer()
                                .isActiveRequiredCardControlEnablerLocation(
                                        fixture.game(), PLAYER,
                                        fixture.bunker()));
                assertTrue(secretBaseBlueprint,
                        fixture.analyzer()
                                .isSoleControlSourceAtRequiredCardEnabler(
                                        fixture.game(), PLAYER,
                                        bikerScout, fixture.bunker()));

                Decision decision = Decision.topLevelMove();
                Outcome move = moveAdapter(
                        bot, fixture, decision, "move-biker-scout");
                assertContains(move, REQUIRED_ENABLER_HOLD_RULE);
                assertFalse(move.hardVeto());
                assertTrue(move.toString(),
                        move.score() > -90000.0f);
                moveCandidates.add(move);

                Outcome winner = combined(bot, fixture, decision);
                assertEquals("pass", winner.actionId());
                winners.add(winner);
            }
            assertParity(moveCandidates);
            assertParity(winners);
        }
    }

    @Test
    public void retentionMoveHoldAndPowerDeficitReleaseReachBothBots() {
        for (boolean flipped : List.of(false, true)) {
            for (String rumorsBlueprint : List.of(
                    OMINOUS_BP, OMINOUS_LEGACY_BP)) {
                List<Outcome> heldMoves = new ArrayList<>();
                List<Outcome> heldWinners = new ArrayList<>();
                List<Outcome> releasedMoves = new ArrayList<>();

                for (Bot bot : Bot.values()) {
                    Fixture held = fixture(bot, flipped);
                    prepareContestedThirdSite(
                            held, rumorsBlueprint,
                            8.0f, 14.0f);
                    Decision decision =
                            Decision.topLevelMove();
                    Outcome heldMove = moveAdapter(
                            bot, held, decision,
                            "move-biker-scout");
                    assertContains(
                            heldMove,
                            REQUIRED_RETENTION_MOVE_RULE);
                    assertFalse(heldMove.hardVeto());
                    assertTrue(heldMove.toString(),
                            heldMove.score() > -90000.0f);
                    Outcome heldWinner =
                            combined(bot, held, decision);
                    assertEquals("pass",
                            heldWinner.actionId());
                    heldMoves.add(heldMove);
                    heldWinners.add(heldWinner);

                    Fixture released = fixture(
                            bot, flipped);
                    prepareContestedThirdSite(
                            released, rumorsBlueprint,
                            8.0f, 15.0f);
                    Outcome releasedMove = moveAdapter(
                            bot, released, decision,
                            "move-biker-scout");
                    assertNotContains(
                            releasedMove,
                            REQUIRED_RETENTION_MOVE_RULE);
                    releasedMoves.add(releasedMove);
                }

                assertParity(heldMoves);
                assertParity(heldWinners);
                assertParity(releasedMoves);
            }
        }
    }

    @Test
    public void deployDestinationReinforcesUrgentThirdEndorSite() {
        for (boolean flipped : List.of(false, true)) {
            for (String rumorsBlueprint : List.of(
                    OMINOUS_BP, OMINOUS_LEGACY_BP)) {
                List<Outcome> urgentWinners =
                        new ArrayList<>();
                List<Outcome> urgentDestinations =
                        new ArrayList<>();
                List<Outcome> closedDestinations =
                        new ArrayList<>();

                for (Bot bot : Bot.values()) {
                    Fixture urgent = fixture(bot, flipped);
                    PhysicalCard forest = card(
                            FOREST_BP, FOREST_ID,
                            Zone.LOCATIONS, PLAYER);
                    urgent.addLocation(forest);
                    PhysicalCard rumors = card(
                            rumorsBlueprint, OMINOUS_ID,
                            Zone.SIDE_OF_TABLE, PLAYER);
                    urgent.addActivePermanent(rumors);
                    when(urgent.modifiers()
                            .mayNotBeCanceled(
                                    urgent.gameState(),
                                    rumors))
                            .thenReturn(false);
                    urgent.setOpponentControls(
                            urgent.bunker(), true);
                    urgent.setOpponentControls(
                            urgent.platform(), true);
                    PhysicalCard candidate = card(
                            BIKER_SCOUT_BP,
                            BIKER_SCOUT_ID,
                            Zone.HAND, PLAYER);
                    urgent.addHand(candidate);
                    when(urgent.modifiers().hasAbility(
                            urgent.gameState(),
                            candidate, true))
                            .thenReturn(true);

                    Decision decision =
                            Decision.deployBikerScout(
                                    forest,
                                    urgent.cantina());
                    Outcome forestChoice =
                            cardSelectionAdapter(
                                    bot, urgent,
                                    decision,
                                    String.valueOf(
                                            FOREST_ID));
                    assertTrue(forestChoice.reasoning()
                            .stream().anyMatch(reason ->
                                    reason.contains(
                                            "keep an active required objective card")));
                    Outcome cantinaChoice =
                            cardSelectionAdapter(
                                    bot, urgent,
                                    decision,
                                    String.valueOf(
                                            CANTINA_ID));
                    assertNotContains(
                            cantinaChoice,
                            REQUIRED_RETENTION_DEPLOY_RULE);
                    TracedOutcome winner =
                            tracedCombined(
                                    bot, urgent,
                                    decision);
                    assertEquals(
                            String.valueOf(FOREST_ID),
                            winner.outcome().actionId());
                    assertHasTypedRule(
                            winner.trace(),
                            String.valueOf(FOREST_ID),
                            REQUIRED_RETENTION_DEPLOY_RULE);
                    urgentDestinations.add(forestChoice);
                    urgentWinners.add(winner.outcome());

                    Fixture closed = fixture(bot, flipped);
                    PhysicalCard closedForest = card(
                            FOREST_BP, FOREST_ID,
                            Zone.LOCATIONS, PLAYER);
                    closed.addLocation(closedForest);
                    PhysicalCard closedRumors = card(
                            rumorsBlueprint, OMINOUS_ID,
                            Zone.SIDE_OF_TABLE, PLAYER);
                    closed.addActivePermanent(
                            closedRumors);
                    when(closed.modifiers()
                            .mayNotBeCanceled(
                                    closed.gameState(),
                                    closedRumors))
                            .thenReturn(false);
                    closed.setOpponentControls(
                            closed.bunker(), true);
                    PhysicalCard closedCandidate = card(
                            BIKER_SCOUT_BP,
                            BIKER_SCOUT_ID,
                            Zone.HAND, PLAYER);
                    closed.addHand(closedCandidate);
                    when(closed.modifiers().hasAbility(
                            closed.gameState(),
                            closedCandidate, true))
                            .thenReturn(true);
                    Outcome closedForestChoice =
                            cardSelectionAdapter(
                                    bot, closed,
                                    Decision.deployBikerScout(
                                            closedForest,
                                            closed.cantina()),
                                    String.valueOf(
                                            FOREST_ID));
                    assertNotContains(
                            closedForestChoice,
                            REQUIRED_RETENTION_DEPLOY_RULE);
                    closedDestinations.add(
                            closedForestChoice);
                }

                assertParity(urgentDestinations);
                assertParity(urgentWinners);
                assertParity(closedDestinations);
            }
        }
    }

    @Test
    public void deployReinforcesSoleDyerOnlyWhileHisProtectionIsUrgent() {
        for (boolean flipped : List.of(false, true)) {
            List<Outcome> protectedDestinations =
                    new ArrayList<>();
            List<Outcome> protectedWinners =
                    new ArrayList<>();
            List<Outcome> earlyDestinations =
                    new ArrayList<>();

            for (Bot bot : Bot.values()) {
                Fixture protectedFixture =
                        fixture(bot, flipped);
                PhysicalCard protectedForest = card(
                        FOREST_BP, FOREST_ID,
                        Zone.LOCATIONS, PLAYER);
                protectedFixture.addLocation(
                        protectedForest);
                PhysicalCard protectedRumors = card(
                        OMINOUS_BP, OMINOUS_ID,
                        Zone.SIDE_OF_TABLE, PLAYER);
                protectedFixture.addActivePermanent(
                        protectedRumors);
                when(protectedFixture.modifiers()
                        .mayNotBeCanceled(
                                protectedFixture
                                        .gameState(),
                                protectedRumors))
                        .thenReturn(true);
                protectedFixture.setOpponentControls(
                        protectedFixture.bunker(), true);
                protectedFixture.setOpponentControls(
                        protectedFixture.platform(), true);
                PhysicalCard dyer = card(
                        DYER_BP, DYER_ID,
                        Zone.AT_LOCATION, PLAYER);
                protectedFixture.place(
                        dyer, protectedForest);
                PhysicalCard reinforcement = card(
                        BIKER_SCOUT_BP,
                        BIKER_SCOUT_ID,
                        Zone.HAND, PLAYER);
                protectedFixture.addHand(
                        reinforcement);
                when(protectedFixture.modifiers()
                        .hasAbility(
                                protectedFixture
                                        .gameState(),
                                reinforcement, true))
                        .thenReturn(true);

                assertTrue(protectedFixture.analyzer()
                        .isRequiredCardCancelPreventerDefenseLocation(
                                protectedFixture.game(),
                                PLAYER,
                                protectedForest));
                Decision decision =
                        Decision.deployBikerScout(
                                protectedForest,
                                protectedFixture.cantina());
                Outcome protectedDestination =
                        cardSelectionAdapter(
                                bot, protectedFixture,
                                decision,
                                String.valueOf(
                                        FOREST_ID));
                assertTrue(protectedDestination.reasoning()
                        .stream().anyMatch(reason ->
                                reason.contains(
                                        "keep an active required objective card")));
                TracedOutcome protectedWinner =
                        tracedCombined(
                                bot, protectedFixture,
                                decision);
                assertEquals(
                        String.valueOf(FOREST_ID),
                        protectedWinner.outcome()
                                .actionId());
                assertHasTypedRule(
                        protectedWinner.trace(),
                        String.valueOf(FOREST_ID),
                        REQUIRED_RETENTION_DEPLOY_RULE);
                protectedDestinations.add(
                        protectedDestination);
                protectedWinners.add(
                        protectedWinner.outcome());

                Fixture early = fixture(bot, flipped);
                PhysicalCard earlyForest = card(
                        FOREST_BP, FOREST_ID,
                        Zone.LOCATIONS, PLAYER);
                early.addLocation(earlyForest);
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
                early.place(card(
                        DYER_BP, DYER_ID,
                        Zone.AT_LOCATION, PLAYER),
                        earlyForest);
                PhysicalCard earlyReinforcement = card(
                        BIKER_SCOUT_BP,
                        BIKER_SCOUT_ID,
                        Zone.HAND, PLAYER);
                early.addHand(earlyReinforcement);
                when(early.modifiers().hasAbility(
                        early.gameState(),
                        earlyReinforcement, true))
                        .thenReturn(true);

                assertFalse(early.analyzer()
                        .isRequiredCardCancelPreventerDefenseLocation(
                                early.game(), PLAYER,
                                earlyForest));
                Outcome earlyDestination =
                        cardSelectionAdapter(
                                bot, early,
                                Decision.deployBikerScout(
                                        earlyForest,
                                        early.cantina()),
                                String.valueOf(
                                        FOREST_ID));
                assertFalse(earlyDestination.reasoning()
                        .stream().anyMatch(reason ->
                                reason.contains(
                                        "keep an active required objective card")));
                earlyDestinations.add(
                        earlyDestination);
            }

            assertParity(protectedDestinations);
            assertParity(protectedWinners);
            assertParity(earlyDestinations);
        }
    }

    @Test
    public void avoidableForfeitPreservesRetentionDefender() {
        for (boolean flipped : List.of(false, true)) {
            for (String rumorsBlueprint : List.of(
                    OMINOUS_BP, OMINOUS_LEGACY_BP)) {
                List<Outcome> winners = new ArrayList<>();
                List<Outcome> protectedCandidates =
                        new ArrayList<>();
                List<Outcome> unavoidableCandidates =
                        new ArrayList<>();

                for (Bot bot : Bot.values()) {
                    Fixture fixture = fixture(bot, flipped);
                    RetentionFormation formation =
                            prepareContestedThirdSite(
                                    fixture,
                                    rumorsBlueprint,
                                    8.0f, 14.0f);
                    PhysicalCard disposable = card(
                            DISPOSABLE_DROID_BP,
                            DISPOSABLE_DROID_ID,
                            Zone.AT_LOCATION, PLAYER);
                    fixture.place(
                            disposable,
                            formation.forest());
                    when(fixture.modifiers().hasAbility(
                            fixture.gameState(),
                            disposable, true))
                            .thenReturn(false);

                    Decision avoidable =
                            Decision.forfeit(
                                    formation.defender(),
                                    disposable);
                    Outcome protectedCandidate =
                            cardSelectionAdapter(
                                    bot, fixture,
                                    avoidable,
                                    String.valueOf(
                                            BIKER_SCOUT_ID));
                    assertContains(
                            protectedCandidate,
                            REQUIRED_RETENTION_FORFEIT_RULE);
                    Outcome disposableCandidate =
                            cardSelectionAdapter(
                                    bot, fixture,
                                    avoidable,
                                    String.valueOf(
                                            DISPOSABLE_DROID_ID));
                    assertNotContains(
                            disposableCandidate,
                            REQUIRED_RETENTION_FORFEIT_RULE);
                    TracedOutcome winner =
                            tracedCombined(
                                    bot, fixture,
                                    avoidable);
                    assertEquals(
                            String.valueOf(
                                    DISPOSABLE_DROID_ID),
                            winner.outcome().actionId());
                    assertHasTypedRuleDelta(
                            winner.trace(),
                            String.valueOf(
                                    BIKER_SCOUT_ID),
                            REQUIRED_RETENTION_FORFEIT_RULE,
                            TraceDomainId.OBJECTIVE_INTENT,
                            -300.0f);
                    protectedCandidates.add(
                            protectedCandidate);
                    winners.add(winner.outcome());

                    Decision unavoidable =
                            Decision.forfeit(
                                    formation.defender());
                    Outcome unavoidableCandidate =
                            cardSelectionAdapter(
                                    bot, fixture,
                                    unavoidable,
                                    String.valueOf(
                                            BIKER_SCOUT_ID));
                    assertNotContains(
                            unavoidableCandidate,
                            REQUIRED_RETENTION_FORFEIT_RULE);
                    unavoidableCandidates.add(
                            unavoidableCandidate);
                }

                assertParity(winners);
                assertParity(protectedCandidates);
                assertParity(unavoidableCandidates);
            }
        }
    }

    @Test
    public void reactorCoreDeployGetsBoundedPenaltyOnlyForEndorObjective() {
        for (boolean flipped : List.of(false, true)) {
            for (boolean rumorsActive : List.of(false, true)) {
                List<Outcome> blockedCandidates =
                        new ArrayList<>();
                List<Outcome> blockedWinners =
                        new ArrayList<>();
                List<Outcome> inPlayCandidates =
                        new ArrayList<>();

                for (Bot bot : Bot.values()) {
                    Fixture fixture = fixture(bot, flipped);
                    if (rumorsActive) {
                        fixture.addActivePermanent(card(
                                OMINOUS_BP, OMINOUS_ID,
                                Zone.SIDE_OF_TABLE, PLAYER));
                    }
                    PhysicalCard reactor = card(
                            REACTOR_CORE_BP,
                            REACTOR_CORE_ID,
                            Zone.HAND, PLAYER);
                    fixture.addHand(reactor);
                    assertTrue(fixture.analyzer()
                            .isRequiredCardForFlip(
                                    "Ominous Rumors"));
                    assertTrue(fixture.analyzer()
                            .wouldDeployPreventRequiredCardActivity(
                                    fixture.game(), PLAYER,
                                    reactor));
                    Decision decision =
                            Decision.reactorCoreDeploy();
                    Outcome blocked = deployAdapter(
                            bot, fixture, decision,
                            "deploy-reactor-core");
                    assertTrue(blocked.reasoning()
                            .stream().anyMatch(reason ->
                                    reason.contains(
                                            "suspends an active card required by the objective")));
                    assertFalse(blocked.hardVeto());
                    TracedOutcome winner =
                            tracedCombined(
                                    bot, fixture,
                                    decision);
                    assertEquals("deploy-reactor-core",
                            winner.outcome().actionId());
                    assertHasTypedRule(
                            winner.trace(),
                            "deploy-reactor-core",
                            REQUIRED_CARD_INACTIVATION_RULE);
                    blockedCandidates.add(blocked);
                    blockedWinners.add(winner.outcome());

                    Fixture alreadyInPlay =
                            fixture(bot, flipped);
                    PhysicalCard tableReactor = card(
                            REACTOR_CORE_BP,
                            REACTOR_CORE_ID,
                            Zone.LOCATIONS, PLAYER);
                    alreadyInPlay.addActivePermanent(
                            tableReactor);
                    Outcome inPlay = deployAdapter(
                            bot, alreadyInPlay,
                            decision,
                            "deploy-reactor-core");
                    assertNotContains(
                            inPlay,
                            REQUIRED_CARD_INACTIVATION_RULE);
                    inPlayCandidates.add(inPlay);
                }

                assertParity(blockedCandidates);
                assertParity(blockedWinners);
                assertParity(inPlayCandidates);
            }
        }

        for (Bot bot : Bot.values()) {
            Fixture noObjective = fixture(bot);
            noObjective.removeObjective();
            PhysicalCard reactor = card(
                    REACTOR_CORE_BP,
                    REACTOR_CORE_ID,
                    Zone.HAND, PLAYER);
            noObjective.addHand(reactor);
            Outcome unblocked = deployAdapter(
                    bot, noObjective,
                    Decision.reactorCoreDeploy(),
                    "deploy-reactor-core");
            assertNotContains(
                    unblocked,
                    REQUIRED_CARD_INACTIVATION_RULE);
            assertFalse(unblocked.hardVeto());
        }
    }

    private static Outcome combined(
            Bot bot, Fixture fixture, Decision decision) {
        if (bot == Bot.RANDO) {
            return outcome(
                    new com.gempukku.swccgo.ai.models.rando.evaluators
                            .CombinedEvaluator()
                            .evaluateDecision(
                                    randoContext(fixture, decision)));
        }
        return outcome(
                new com.gempukku.swccgo.ai.models.chosenone.evaluators
                        .CombinedEvaluator()
                        .evaluateDecision(
                                chosenContext(fixture, decision)));
    }

    private static TracedOutcome tracedCombined(
            Bot bot, Fixture fixture, Decision decision) {
        List<String> rawCandidates = !decision.actionIds().isEmpty()
                ? decision.actionIds()
                : !decision.cardIds().isEmpty()
                    ? decision.cardIds()
                    : decision.blueprints();
        assertTrue(TraceSession.open(
                bot.name(),
                "endor-operations-production-decision",
                decision.type(), decision.text(),
                rawCandidates, null,
                List.of("focused evaluator fixture omits the bot-boundary snapshot"),
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

    private static TracedOutcome tracedCombinedWithVerifiedCrewPlan(
            Bot bot, Fixture fixture, Decision decision,
            PhysicalCard ship, PhysicalCard pilot) {
        List<String> rawCandidates = decision.actionIds();
        assertTrue(TraceSession.open(
                bot.name(),
                "endor-operations-verified-crew-decision",
                decision.type(), decision.text(),
                rawCandidates, null,
                List.of("focused evaluator fixture"),
                false));
        Outcome outcome;
        DecisionTrace trace;
        try {
            if (bot == Bot.RANDO) {
                var context = randoContext(fixture, decision);
                var plan = new com.gempukku.swccgo.ai.models.rando
                        .strategy.DeploymentPlan(
                                com.gempukku.swccgo.ai.models.rando
                                        .strategy.DeployStrategy.ESTABLISH,
                                "EOP: verified generic Endor crew package");
                var shipInstruction =
                        new com.gempukku.swccgo.ai.models.rando.strategy
                                .DeploymentInstruction(
                                        ship.getBlueprintId(true),
                                        ship.getTitle(),
                                        String.valueOf(
                                                fixture.endor().getCardId()),
                                        fixture.endor().getTitle(),
                                        1, "occupy Endor system");
                shipInstruction.setCardPermanentCardId(
                        ship.getPermanentCardId());
                shipInstruction.setCardCurrentCardId(ship.getCardId());
                shipInstruction.setDeployCost(4);
                shipInstruction.setVerifiedCrewPackage(true);
                plan.addInstruction(shipInstruction);
                var pilotInstruction =
                        new com.gempukku.swccgo.ai.models.rando.strategy
                                .DeploymentInstruction(
                                        pilot.getBlueprintId(true),
                                        pilot.getTitle(),
                                        String.valueOf(
                                                fixture.endor().getCardId()),
                                        fixture.endor().getTitle(),
                                        2, "crew Endor system ship");
                pilotInstruction.setCardPermanentCardId(
                        pilot.getPermanentCardId());
                pilotInstruction.setCardCurrentCardId(pilot.getCardId());
                pilotInstruction.setDeployCost(0);
                pilotInstruction.setAboardShipCardId(
                        String.valueOf(ship.getCardId()));
                plan.addInstruction(pilotInstruction);
                context.setDeployPhasePlanner(
                        new com.gempukku.swccgo.ai.models.rando.strategy
                                .DeployPhasePlanner() {
                            @Override
                            public com.gempukku.swccgo.ai.models.rando.strategy
                                    .DeploymentPlan createPlan(
                                            SwccgGame game, String playerId,
                                            Side side) {
                                return plan;
                            }
                        });
                outcome = outcome(
                        new com.gempukku.swccgo.ai.models.rando.evaluators
                                .CombinedEvaluator()
                                .evaluateDecision(context));
            } else {
                var context = chosenContext(fixture, decision);
                var plan = new com.gempukku.swccgo.ai.models.chosenone
                        .strategy.DeploymentPlan(
                                com.gempukku.swccgo.ai.models.chosenone
                                        .strategy.DeployStrategy.ESTABLISH,
                                "EOP: verified generic Endor crew package");
                var shipInstruction =
                        new com.gempukku.swccgo.ai.models.chosenone.strategy
                                .DeploymentInstruction(
                                        ship.getBlueprintId(true),
                                        ship.getTitle(),
                                        String.valueOf(
                                                fixture.endor().getCardId()),
                                        fixture.endor().getTitle(),
                                        1, "occupy Endor system");
                shipInstruction.setCardPermanentCardId(
                        ship.getPermanentCardId());
                shipInstruction.setCardCurrentCardId(ship.getCardId());
                shipInstruction.setDeployCost(4);
                shipInstruction.setVerifiedCrewPackage(true);
                plan.addInstruction(shipInstruction);
                var pilotInstruction =
                        new com.gempukku.swccgo.ai.models.chosenone.strategy
                                .DeploymentInstruction(
                                        pilot.getBlueprintId(true),
                                        pilot.getTitle(),
                                        String.valueOf(
                                                fixture.endor().getCardId()),
                                        fixture.endor().getTitle(),
                                        2, "crew Endor system ship");
                pilotInstruction.setCardPermanentCardId(
                        pilot.getPermanentCardId());
                pilotInstruction.setCardCurrentCardId(pilot.getCardId());
                pilotInstruction.setDeployCost(0);
                pilotInstruction.setAboardShipCardId(
                        String.valueOf(ship.getCardId()));
                plan.addInstruction(pilotInstruction);
                context.setDeployPhasePlanner(
                        new com.gempukku.swccgo.ai.models.chosenone.strategy
                                .DeployPhasePlanner() {
                            @Override
                            public com.gempukku.swccgo.ai.models.chosenone
                                    .strategy.DeploymentPlan createPlan(
                                            SwccgGame game, String playerId,
                                            Side side) {
                                return plan;
                            }
                        });
                outcome = outcome(
                        new com.gempukku.swccgo.ai.models.chosenone.evaluators
                                .CombinedEvaluator()
                                .evaluateDecision(context));
            }
        } finally {
            trace = TraceSession.close();
        }
        assertNotNull(trace);
        return new TracedOutcome(outcome, trace);
    }

    private static TracedOutcome tracedCombinedWithBunkerGarrisonPlan(
            Bot bot, Fixture fixture, Decision decision,
            PhysicalCard garrison) {
        List<String> rawCandidates = !decision.actionIds().isEmpty()
                ? decision.actionIds() : decision.cardIds();
        assertTrue(TraceSession.open(
                bot.name(),
                "endor-operations-bunker-garrison-decision",
                decision.type(), decision.text(),
                rawCandidates, null,
                List.of("replay-shaped EOP Bunker garrison fixture"),
                false));
        Outcome outcome;
        DecisionTrace trace;
        try {
            if (bot == Bot.RANDO) {
                var context = randoContext(fixture, decision);
                var plan = new com.gempukku.swccgo.ai.models.rando
                        .strategy.DeploymentPlan(
                                com.gempukku.swccgo.ai.models.rando
                                        .strategy.DeployStrategy.ESTABLISH,
                                EndorOperationsTacticalPolicy
                                        .bunkerGarrisonPlanReason());
                var instruction =
                        new com.gempukku.swccgo.ai.models.rando.strategy
                                .DeploymentInstruction(
                                        garrison.getBlueprintId(true),
                                        garrison.getTitle(),
                                        String.valueOf(
                                                fixture.bunker().getCardId()),
                                        fixture.bunker().getTitle(),
                                        1, "hold Endor Bunker");
                instruction.setCardPermanentCardId(
                        garrison.getPermanentCardId());
                instruction.setCardCurrentCardId(
                        garrison.getCardId());
                instruction.setDeployCost(2);
                plan.addInstruction(instruction);
                context.setDeployPhasePlanner(
                        new com.gempukku.swccgo.ai.models.rando.strategy
                                .DeployPhasePlanner() {
                            @Override
                            public com.gempukku.swccgo.ai.models.rando.strategy
                                    .DeploymentPlan createPlan(
                                            SwccgGame game, String playerId,
                                            Side side) {
                                return plan;
                            }

                            @Override
                            public com.gempukku.swccgo.ai.models.rando.strategy
                                    .DeploymentPlan getCurrentPlan() {
                                return plan;
                            }
                        });
                outcome = outcome(
                        new com.gempukku.swccgo.ai.models.rando.evaluators
                                .CombinedEvaluator()
                                .evaluateDecision(context));
            } else {
                var context = chosenContext(fixture, decision);
                var plan = new com.gempukku.swccgo.ai.models.chosenone
                        .strategy.DeploymentPlan(
                                com.gempukku.swccgo.ai.models.chosenone
                                        .strategy.DeployStrategy.ESTABLISH,
                                EndorOperationsTacticalPolicy
                                        .bunkerGarrisonPlanReason());
                var instruction =
                        new com.gempukku.swccgo.ai.models.chosenone.strategy
                                .DeploymentInstruction(
                                        garrison.getBlueprintId(true),
                                        garrison.getTitle(),
                                        String.valueOf(
                                                fixture.bunker().getCardId()),
                                        fixture.bunker().getTitle(),
                                        1, "hold Endor Bunker");
                instruction.setCardPermanentCardId(
                        garrison.getPermanentCardId());
                instruction.setCardCurrentCardId(
                        garrison.getCardId());
                instruction.setDeployCost(2);
                plan.addInstruction(instruction);
                context.setDeployPhasePlanner(
                        new com.gempukku.swccgo.ai.models.chosenone.strategy
                                .DeployPhasePlanner() {
                            @Override
                            public com.gempukku.swccgo.ai.models.chosenone
                                    .strategy.DeploymentPlan createPlan(
                                            SwccgGame game, String playerId,
                                            Side side) {
                                return plan;
                            }

                            @Override
                            public com.gempukku.swccgo.ai.models.chosenone
                                    .strategy.DeploymentPlan getCurrentPlan() {
                                return plan;
                            }
                        });
                outcome = outcome(
                        new com.gempukku.swccgo.ai.models.chosenone.evaluators
                                .CombinedEvaluator()
                                .evaluateDecision(context));
            }
        } finally {
            trace = TraceSession.close();
        }
        assertNotNull(trace);
        return new TracedOutcome(outcome, trace);
    }

    private static Outcome moveAdapter(
            Bot bot, Fixture fixture, Decision decision,
            String actionId) {
        if (bot == Bot.RANDO) {
            return new com.gempukku.swccgo.ai.models.rando.evaluators
                    .MoveEvaluator()
                    .evaluate(randoContext(fixture, decision))
                    .stream()
                    .filter(action -> actionId.equals(
                            action.getActionId()))
                    .findFirst()
                    .map(EndorOperationsCombinedEvaluatorDecisionTest::outcome)
                    .orElseThrow(() -> new AssertionError(
                            "Missing action " + actionId));
        }
        return new com.gempukku.swccgo.ai.models.chosenone.evaluators
                .MoveEvaluator()
                .evaluate(chosenContext(fixture, decision))
                .stream()
                .filter(action -> actionId.equals(
                        action.getActionId()))
                .findFirst()
                .map(EndorOperationsCombinedEvaluatorDecisionTest::outcome)
                .orElseThrow(() -> new AssertionError(
                        "Missing action " + actionId));
    }

    private static Outcome deployAdapter(
            Bot bot, Fixture fixture, Decision decision,
            String actionId) {
        if (bot == Bot.RANDO) {
            return new com.gempukku.swccgo.ai.models.rando.evaluators
                    .DeployEvaluator()
                    .evaluate(randoContext(fixture, decision))
                    .stream()
                    .filter(action -> actionId.equals(
                            action.getActionId()))
                    .findFirst()
                    .map(EndorOperationsCombinedEvaluatorDecisionTest::outcome)
                    .orElseThrow(() -> new AssertionError(
                            "Missing action " + actionId));
        }
        return new com.gempukku.swccgo.ai.models.chosenone.evaluators
                .DeployEvaluator()
                .evaluate(chosenContext(fixture, decision))
                .stream()
                .filter(action -> actionId.equals(
                        action.getActionId()))
                .findFirst()
                .map(EndorOperationsCombinedEvaluatorDecisionTest::outcome)
                    .orElseThrow(() -> new AssertionError(
                            "Missing action " + actionId));
    }

    private static Outcome actionTextAdapter(
            Bot bot, Fixture fixture, Decision decision,
            String actionId) {
        if (bot == Bot.RANDO) {
            return new com.gempukku.swccgo.ai.models.rando.evaluators
                    .ActionTextEvaluator()
                    .evaluate(randoContext(fixture, decision))
                    .stream()
                    .filter(action -> actionId.equals(
                            action.getActionId()))
                    .findFirst()
                    .map(EndorOperationsCombinedEvaluatorDecisionTest::outcome)
                    .orElseThrow(() -> new AssertionError(
                            "Missing action " + actionId));
        }
        return new com.gempukku.swccgo.ai.models.chosenone.evaluators
                .ActionTextEvaluator()
                .evaluate(chosenContext(fixture, decision))
                .stream()
                .filter(action -> actionId.equals(
                        action.getActionId()))
                .findFirst()
                .map(EndorOperationsCombinedEvaluatorDecisionTest::outcome)
                .orElseThrow(() -> new AssertionError(
                        "Missing action " + actionId));
    }

    private static Outcome cardSelectionAdapter(
            Bot bot, Fixture fixture, Decision decision,
            String actionId) {
        if (bot == Bot.RANDO) {
            return new com.gempukku.swccgo.ai.models.rando.evaluators
                    .CardSelectionEvaluator()
                    .evaluate(randoContext(fixture, decision))
                    .stream()
                    .filter(action -> actionId.equals(
                            action.getActionId()))
                    .findFirst()
                    .map(EndorOperationsCombinedEvaluatorDecisionTest::outcome)
                    .orElseThrow(() -> new AssertionError(
                            "Missing action " + actionId));
        }
        return new com.gempukku.swccgo.ai.models.chosenone.evaluators
                .CardSelectionEvaluator()
                .evaluate(chosenContext(fixture, decision))
                .stream()
                .filter(action -> actionId.equals(
                        action.getActionId()))
                .findFirst()
                .map(EndorOperationsCombinedEvaluatorDecisionTest::outcome)
                .orElseThrow(() -> new AssertionError(
                        "Missing action " + actionId));
    }

    private static Outcome cardSelectionAdapterForMover(
            Bot bot, Fixture fixture, Decision decision,
            String actionId, PhysicalCard mover) {
        if (bot == Bot.RANDO) {
            var context = randoContext(
                    fixture, decision);
            context.setExtra(
                    MovePhysicalCardResolver
                            .MOVER_CARD_ID_EXTRA,
                    mover.getCardId());
            return new com.gempukku.swccgo.ai.models.rando.evaluators
                    .CardSelectionEvaluator()
                    .evaluate(context)
                    .stream()
                    .filter(action -> actionId.equals(
                            action.getActionId()))
                    .findFirst()
                    .map(EndorOperationsCombinedEvaluatorDecisionTest::outcome)
                    .orElseThrow(() -> new AssertionError(
                            "Missing action " + actionId));
        }
        var context = chosenContext(
                fixture, decision);
        context.setExtra(
                MovePhysicalCardResolver
                        .MOVER_CARD_ID_EXTRA,
                mover.getCardId());
        return new com.gempukku.swccgo.ai.models.chosenone.evaluators
                .CardSelectionEvaluator()
                .evaluate(context)
                .stream()
                .filter(action -> actionId.equals(
                        action.getActionId()))
                .findFirst()
                .map(EndorOperationsCombinedEvaluatorDecisionTest::outcome)
                .orElseThrow(() -> new AssertionError(
                        "Missing action " + actionId));
    }

    private static Outcome builtCardSelectionOutcome(
            Bot bot, Fixture fixture,
            Object builtContext, String actionId) {
        if (bot == Bot.RANDO) {
            var context =
                    (com.gempukku.swccgo.ai.models.rando
                            .evaluators.DecisionContext)
                            builtContext;
            context.setGame(fixture.game());
            context.setSide(Side.DARK);
            context.setObjectiveAnalyzer(
                    (com.gempukku.swccgo.ai.models.rando
                            .strategy.ObjectiveAnalyzer)
                            fixture.analyzer());
            return new com.gempukku.swccgo.ai.models.rando
                    .evaluators.CardSelectionEvaluator()
                    .evaluate(context).stream()
                    .filter(action -> actionId.equals(
                            action.getActionId()))
                    .findFirst()
                    .map(EndorOperationsCombinedEvaluatorDecisionTest
                            ::outcome)
                    .orElseThrow(() -> new AssertionError(
                            "Missing action " + actionId));
        }
        var context =
                (com.gempukku.swccgo.ai.models.chosenone
                        .evaluators.DecisionContext)
                        builtContext;
        context.setGame(fixture.game());
        context.setSide(Side.DARK);
        context.setObjectiveAnalyzer(
                (com.gempukku.swccgo.ai.models.chosenone
                        .strategy.ObjectiveAnalyzer)
                        fixture.analyzer());
        return new com.gempukku.swccgo.ai.models.chosenone
                .evaluators.CardSelectionEvaluator()
                .evaluate(context).stream()
                .filter(action -> actionId.equals(
                        action.getActionId()))
                .findFirst()
                .map(EndorOperationsCombinedEvaluatorDecisionTest
                        ::outcome)
                .orElseThrow(() -> new AssertionError(
                        "Missing action " + actionId));
    }

    private static com.gempukku.swccgo.ai.models.rando.evaluators
            .DecisionContext randoContext(
                    Fixture fixture, Decision decision) {
        var context =
                new com.gempukku.swccgo.ai.models.rando.evaluators
                        .DecisionContext(
                                fixture.gameState(), PLAYER,
                                decision.type(), decision.text(),
                                "endor-operations-production-decision",
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
                                "endor-operations-production-decision",
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
        context.setBlueprints(decision.blueprints());
        context.setTestingTexts(decision.testingTexts());
        context.setSelectable(decision.selectionCount() == 0
                ? List.of()
                : java.util.Collections.nCopies(
                        decision.selectionCount(), true));
        context.setNoPass(decision.noPass());
        context.setMin(decision.min());
        context.setMax(1);
    }

    private static void apply(
            com.gempukku.swccgo.ai.models.chosenone.evaluators
                    .DecisionContext context,
            Decision decision) {
        context.setActionIds(decision.actionIds());
        context.setActionTexts(decision.actionTexts());
        context.setCardIds(decision.cardIds());
        context.setBlueprints(decision.blueprints());
        context.setTestingTexts(decision.testingTexts());
        context.setSelectable(decision.selectionCount() == 0
                ? List.of()
                : java.util.Collections.nCopies(
                        decision.selectionCount(), true));
        context.setNoPass(decision.noPass());
        context.setMin(decision.min());
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

    private static void assertContains(
            Outcome outcome, String marker) {
        assertTrue(
                "Expected '" + marker + "' in " + outcome.reasoning(),
                outcome.reasoning().stream()
                        .anyMatch(reason -> reason.contains(marker)));
    }

    private static void assertNotContains(
            Outcome outcome, String marker) {
        assertFalse(
                "Did not expect '" + marker + "' in "
                        + outcome.reasoning(),
                outcome.reasoning().stream()
                        .anyMatch(reason -> reason.contains(marker)));
    }

    private static void assertHasTypedRule(
            DecisionTrace trace, String actionId,
            String ruleId) {
        assertTrue(
                "Expected typed rule '" + ruleId + "' for "
                        + actionId + " in " + trace.getOperations(),
                trace.getOperations().stream()
                        .anyMatch(operation ->
                                actionId.equals(
                                        operation.getActionId())
                                && ruleId.equals(
                                        operation.getRuleId().id())));
    }

    private static void assertHasTypedRuleDelta(
            DecisionTrace trace, String actionId,
            String ruleId, TraceDomainId domainId,
            float delta) {
        int deltaBits = Float.floatToRawIntBits(delta);
        assertTrue(
                "Expected typed rule '" + ruleId + "' for "
                        + actionId + " in " + trace.getOperations(),
                trace.getOperations().stream()
                        .anyMatch(operation ->
                                actionId.equals(
                                        operation.getActionId())
                                && ruleId.equals(
                                        operation.getRuleId().id())
                                && domainId
                                        == operation.getDomainId()
                                && operation.getDeltaBits() != null
                                && deltaBits
                                        == operation.getDeltaBits()));
    }

    private static void assertLacksTypedRule(
            DecisionTrace trace, String actionId,
            String ruleId) {
        assertFalse(
                "Did not expect typed rule '" + ruleId + "' for "
                        + actionId + " in " + trace.getOperations(),
                trace.getOperations().stream()
                        .anyMatch(operation ->
                                actionId.equals(
                                        operation.getActionId())
                                && ruleId.equals(
                                        operation.getRuleId().id())));
    }

    private static void assertParity(List<Outcome> outcomes) {
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

    private static void assertBoundedForceLossPreferenceForBothRoutes(
            Bot bot, Fixture fixture,
            PhysicalCard protectedCard,
            PhysicalCard expendableCard) {
        for (boolean combinedRoute : List.of(false, true)) {
            Decision decision = Decision.forceLoss(
                    combinedRoute,
                    protectedCard, expendableCard);
            TracedOutcome traced = tracedCombined(
                    bot, fixture, decision);
            assertHasTypedRuleDelta(
                    traced.trace(),
                    String.valueOf(protectedCard.getCardId()),
                    FORCE_LOSS_OBJECTIVE_RULE,
                    TraceDomainId.OBJECTIVE_INTENT,
                    -300.0f);
            assertLacksTypedRule(
                    traced.trace(),
                    String.valueOf(expendableCard.getCardId()),
                    FORCE_LOSS_OBJECTIVE_RULE);
        }
    }

    private static PhysicalCard prepareTwoClassicActors(
            Fixture fixture) {
        fixture.addReserve(card(
                SECRET_BASE_CLASSIC_BP, SECRET_BASE_ID,
                Zone.RESERVE_DECK, PLAYER));
        PhysicalCard forest = card(
                FOREST_BP, FOREST_ID,
                Zone.LOCATIONS, PLAYER);
        fixture.addLocation(forest);
        PhysicalCard bunkerScout = card(
                BIKER_SCOUT_BP, BUNKER_SCOUT_ID,
                Zone.AT_LOCATION, PLAYER);
        PhysicalCard platformScout = card(
                BIKER_SCOUT_BP, PLATFORM_SCOUT_ID,
                Zone.AT_LOCATION, PLAYER);
        fixture.place(bunkerScout, fixture.bunker());
        fixture.place(platformScout, fixture.platform());
        setControls(fixture, fixture.bunker(), PLAYER, true);
        setControls(fixture, fixture.platform(), PLAYER, true);
        return forest;
    }

    private static CountedRelocation
            prepareThreeCountedClassicSites(
                    Fixture fixture) {
        fixture.addReserve(card(
                SECRET_BASE_CLASSIC_BP, SECRET_BASE_ID,
                Zone.RESERVE_DECK, PLAYER));
        PhysicalCard forest = card(
                FOREST_BP, FOREST_ID,
                Zone.LOCATIONS, PLAYER);
        PhysicalCard backDoor = card(
                BACK_DOOR_BP, BACK_DOOR_ID,
                Zone.LOCATIONS, PLAYER);
        fixture.addLocation(forest);
        fixture.addLocation(backDoor);

        PhysicalCard mover = card(
                UNIQUE_BIKER_SCOUT_BP,
                MOVING_BIKER_SCOUT_ID,
                Zone.AT_LOCATION, PLAYER);
        PhysicalCard platformScout = card(
                BIKER_SCOUT_BP, PLATFORM_SCOUT_ID,
                Zone.AT_LOCATION, PLAYER);
        PhysicalCard forestScout = card(
                BIKER_SCOUT_BP, FOREST_SCOUT_ID,
                Zone.AT_LOCATION, PLAYER);
        fixture.place(mover, fixture.bunker());
        fixture.place(
                platformScout, fixture.platform());
        fixture.place(forestScout, forest);
        setControls(
                fixture, fixture.bunker(),
                PLAYER, true);
        setControls(
                fixture, fixture.platform(),
                PLAYER, true);
        setControls(
                fixture, forest,
                PLAYER, true);
        when(fixture.modifiers().getLandspeed(
                fixture.gameState(), mover))
                .thenReturn(1.0f);
        for (PhysicalCard location : fixture.locations()) {
            when(fixture.modifiers()
                    .getLandspeedRequired(
                            fixture.gameState(),
                            mover, location))
                    .thenReturn(null);
        }
        return new CountedRelocation(
                mover, backDoor);
    }

    private static ClassicAtStFormation
            prepareClassicAtStFormation(Fixture fixture) {
        PhysicalCard forest = prepareTwoClassicActors(fixture);
        PhysicalCard atSt = card(
                AT_ST_BP, AT_ST_ID,
                Zone.AT_LOCATION, PLAYER);
        PhysicalCard pilot = card(
                AT_ST_PILOT_BP, AT_ST_PILOT_ID,
                Zone.ATTACHED, PLAYER);
        fixture.place(atSt, forest);
        fixture.addActivePermanent(pilot);
        when(pilot.isPilotOf()).thenReturn(true);
        when(pilot.getAttachedTo()).thenReturn(atSt);
        when(atSt.getCardsAttached())
                .thenReturn(List.of(pilot));
        when(fixture.modifiers().getLocationThatCardIsAt(
                fixture.gameState(), pilot)).thenReturn(forest);
        when(fixture.modifiers().isPiloted(
                fixture.gameState(), atSt, false))
                .thenReturn(true);
        when(fixture.modifiers().hasPermanentPilot(
                fixture.gameState(), atSt))
                .thenReturn(false);
        setControls(fixture, forest, PLAYER, true);
        when(fixture.modifiers().getTotalPowerAtLocation(
                fixture.gameState(), forest, PLAYER,
                false, false)).thenReturn(8.0f);
        when(fixture.modifiers().getTotalPowerAtLocation(
                fixture.gameState(), forest, OPPONENT,
                false, false)).thenReturn(14.0f);
        return new ClassicAtStFormation(atSt, pilot, forest);
    }

    private static DyerRelocationRoutes
            prepareDyerRelocationRoutes(
                    Fixture fixture,
                    boolean includeSafeBackDoor) {
        PhysicalCard unsafeForest = card(
                FOREST_BP, FOREST_ID,
                Zone.LOCATIONS, PLAYER);
        PhysicalCard safeBackDoor = card(
                BACK_DOOR_BP, BACK_DOOR_ID,
                Zone.LOCATIONS, PLAYER);
        fixture.addLocation(unsafeForest);
        fixture.addLocation(safeBackDoor);

        PhysicalCard rumors = card(
                OMINOUS_BP, OMINOUS_ID,
                Zone.SIDE_OF_TABLE, PLAYER);
        fixture.addActivePermanent(rumors);
        when(fixture.modifiers().mayNotBeCanceled(
                fixture.gameState(), rumors))
                .thenReturn(true);
        fixture.setOpponentControls(
                fixture.platform(), true);
        fixture.setOpponentControls(
                safeBackDoor, true);

        PhysicalCard dyer = card(
                DYER_BP, DYER_ID,
                Zone.AT_LOCATION, PLAYER);
        fixture.place(dyer, fixture.bunker());
        when(fixture.modifiers().getLandspeed(
                fixture.gameState(), dyer))
                .thenReturn(1.0f);
        for (PhysicalCard location
                : fixture.locations()) {
            when(fixture.modifiers()
                    .getLandspeedRequired(
                            fixture.gameState(), dyer,
                            location))
                    .thenReturn(null);
        }
        when(fixture.modifiers().getLandspeedRequired(
                fixture.gameState(), dyer,
                unsafeForest)).thenReturn(1);
        if (includeSafeBackDoor) {
            when(fixture.modifiers().getLandspeedRequired(
                    fixture.gameState(), dyer,
                    safeBackDoor)).thenReturn(1);
        }

        when(fixture.modifiers().getTotalPowerAtLocation(
                fixture.gameState(), unsafeForest,
                PLAYER, false, false)).thenReturn(0.0f);
        when(fixture.modifiers().getTotalPowerAtLocation(
                fixture.gameState(), unsafeForest,
                OPPONENT, false, false)).thenReturn(12.0f);
        when(fixture.modifiers().getTotalPowerAtLocation(
                fixture.gameState(), safeBackDoor,
                PLAYER, false, false)).thenReturn(0.0f);
        when(fixture.modifiers().getTotalPowerAtLocation(
                fixture.gameState(), safeBackDoor,
                OPPONENT, false, false)).thenReturn(0.0f);

        assertEquals(
                ObjectiveAnalyzer.FlipGateFormationRole
                        .REQUIRED_CARD_RETENTION_DEFENDER,
                fixture.analyzer()
                        .classifyGateFormationPieceIfRemoved(
                                fixture.game(), PLAYER,
                                dyer));
        return new DyerRelocationRoutes(
                dyer, unsafeForest, safeBackDoor);
    }

    private static RetentionFormation
            prepareContestedThirdSite(
                    Fixture fixture,
                    String rumorsBlueprint,
                    float friendlyPower,
                    float opponentPower) {
        PhysicalCard forest = card(
                FOREST_BP, FOREST_ID,
                Zone.LOCATIONS, PLAYER);
        fixture.addLocation(forest);
        PhysicalCard rumors = card(
                rumorsBlueprint, OMINOUS_ID,
                Zone.SIDE_OF_TABLE, PLAYER);
        fixture.addActivePermanent(rumors);
        when(fixture.modifiers().mayNotBeCanceled(
                fixture.gameState(), rumors))
                .thenReturn(false);
        fixture.setOpponentControls(
                fixture.bunker(), true);
        fixture.setOpponentControls(
                fixture.platform(), true);

        PhysicalCard defender = card(
                BIKER_SCOUT_BP, BIKER_SCOUT_ID,
                Zone.AT_LOCATION, PLAYER);
        PhysicalCard opponent = card(
                OPPONENT_CHARACTER_BP,
                OPPONENT_CHARACTER_ID,
                Zone.AT_LOCATION, OPPONENT);
        fixture.place(defender, forest);
        fixture.place(opponent, forest);
        when(fixture.modifiers().hasAbility(
                fixture.gameState(), defender, true))
                .thenReturn(true);
        when(fixture.modifiers().hasAbility(
                fixture.gameState(), opponent, true))
                .thenReturn(true);
        fixture.setOpponentOccupies(forest, true);
        when(fixture.modifiers().getTotalPowerAtLocation(
                fixture.gameState(), forest, PLAYER,
                false, false)).thenReturn(friendlyPower);
        when(fixture.modifiers().getTotalPowerAtLocation(
                fixture.gameState(), forest, OPPONENT,
                false, false)).thenReturn(opponentPower);
        when(fixture.modifiers().getTotalAbilityAtLocation(
                fixture.gameState(), PLAYER, forest))
                .thenReturn(2.0f);
        when(fixture.modifiers().getTotalAbilityAtLocation(
                fixture.gameState(), OPPONENT, forest))
                .thenReturn(6.0f);
        when(fixture.modifiers().getLandspeed(
                fixture.gameState(), defender))
                .thenReturn(1.0f);
        return new RetentionFormation(defender, forest);
    }

    private static Fixture fixture(Bot bot) {
        return fixture(bot, false);
    }

    private static Fixture fixture(
            Bot bot, boolean flipped) {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);

        List<PhysicalCard> hand = new ArrayList<>();
        List<PhysicalCard> reserve = new ArrayList<>();
        List<PhysicalCard> permanents = new ArrayList<>();
        Map<Integer, PhysicalCard> cardsById = new LinkedHashMap<>();
        Map<PhysicalCard, List<PhysicalCard>> cardsAt =
                new IdentityHashMap<>();

        PhysicalCard objective = objective(flipped);
        PhysicalCard endor = card(
                ENDOR_BP, ENDOR_ID, Zone.LOCATIONS, PLAYER);
        PhysicalCard bunker = card(
                BUNKER_BP, BUNKER_ID, Zone.LOCATIONS, PLAYER);
        PhysicalCard platform = card(
                PLATFORM_BP, PLATFORM_ID, Zone.LOCATIONS, PLAYER);
        PhysicalCard cantina = card(
                CANTINA_BP, CANTINA_ID, Zone.LOCATIONS, PLAYER);
        List<PhysicalCard> locations =
                new ArrayList<>(List.of(
                        endor, bunker, platform, cantina));

        permanents.addAll(List.of(
                objective, endor, bunker, platform, cantina));
        for (PhysicalCard card : permanents) {
            cardsById.put(card.getCardId(), card);
        }

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(game.getOpponent(PLAYER)).thenReturn(OPPONENT);
        when(gameState.getGame()).thenReturn(game);
        when(gameState.getOpponent(PLAYER)).thenReturn(OPPONENT);
        when(gameState.getOpponent(OPPONENT)).thenReturn(PLAYER);
        when(gameState.getSide(PLAYER)).thenReturn(Side.DARK);
        when(gameState.getSide(OPPONENT)).thenReturn(Side.LIGHT);
        when(gameState.getCurrentPlayerId()).thenReturn(PLAYER);
        when(gameState.getPlayersLatestTurnNumber(PLAYER))
                .thenReturn(3);
        when(gameState.getForcePileSize(PLAYER)).thenReturn(10);
        when(gameState.getForcePile(PLAYER)).thenReturn(List.of());
        when(gameState.getReserveDeckSize(PLAYER)).thenReturn(20);
        when(gameState.getPlayerLifeForce(PLAYER)).thenReturn(30);
        when(gameState.getPlayerLifeForce(OPPONENT)).thenReturn(30);
        when(gameState.getUsedPile(PLAYER)).thenReturn(List.of());
        when(gameState.getUnresolvedDestinyDraw(PLAYER))
                .thenReturn(List.of());
        when(gameState.getSabaccHand(PLAYER))
                .thenReturn(List.of());
        when(gameState.getHand(PLAYER)).thenReturn(hand);
        when(gameState.getReserveDeck(PLAYER)).thenReturn(reserve);
        when(gameState.getCardPile(
                PLAYER, Zone.RESERVE_DECK)).thenReturn(reserve);
        when(gameState.getAllStackedCards()).thenReturn(List.of());
        when(gameState.getAllPermanentCards()).thenReturn(permanents);
        when(gameState.getLocationsInOrder()).thenReturn(locations);
        when(gameState.getTopLocations()).thenReturn(locations);
        when(gameState.findCardById(anyInt())).thenAnswer(
                invocation -> cardsById.get(
                        invocation.getArgument(0, Integer.class)));
        when(gameState.findCardByPermanentId(anyInt())).thenAnswer(
                invocation -> cardsById.get(
                        invocation.getArgument(0, Integer.class)));
        when(gameState.getCardsAtLocation(
                any(PhysicalCard.class))).thenAnswer(
                    invocation -> cardsAt.getOrDefault(
                            invocation.getArgument(
                                    0, PhysicalCard.class),
                            List.of()));
        when(gameState.getAttachedCards(
                any(PhysicalCard.class))).thenReturn(List.of());
        when(gameState.getAllAttachedRecursively(
                any(PhysicalCard.class))).thenReturn(List.of());
        when(gameState.getCaptivesOfEscort(
                any(PhysicalCard.class))).thenReturn(List.of());
        when(gameState.getAvailablePilotCapacity(
                any(ModifiersQuerying.class),
                any(PhysicalCard.class),
                any(PhysicalCard.class))).thenReturn(1);

        when(modifiers.hasKeyword(
                any(GameState.class), any(PhysicalCard.class), any()))
                .thenAnswer(invocation -> {
                    PhysicalCard card =
                            invocation.getArgument(1);
                    com.gempukku.swccgo.common.Keyword keyword =
                            invocation.getArgument(2);
                    return card != null
                            && card.getBlueprint() != null
                            && card.getBlueprint().hasKeyword(keyword);
                });
        when(modifiers.hasIcon(
                any(GameState.class), any(PhysicalCard.class), any()))
                .thenAnswer(invocation -> {
                    PhysicalCard card =
                            invocation.getArgument(1);
                    Icon icon = invocation.getArgument(2);
                    return card != null
                            && card.getBlueprint() != null
                            && card.getBlueprint().hasIcon(icon);
                });
        when(modifiers.getForceAvailableToUse(
                gameState, PLAYER)).thenReturn(10);
        setTargetAwareDeployCosts(modifiers);
        setDeployable(modifiers);
        for (PhysicalCard card : permanents) {
            setActive(gameState, card);
        }
        for (PhysicalCard location : locations) {
            when(modifiers.getLocationHere(
                    gameState, location)).thenReturn(location);
            when(modifiers.isBattleground(
                    gameState, location, null)).thenReturn(true);
        }

        ObjectiveAnalyzer analyzer = bot == Bot.RANDO
                ? new com.gempukku.swccgo.ai.models.rando.strategy
                        .ObjectiveAnalyzer()
                : new com.gempukku.swccgo.ai.models.chosenone.strategy
                        .ObjectiveAnalyzer();
        analyzer.analyze(game, PLAYER, Side.DARK);
        assertEquals("Endor Operations",
                analyzer.getActivePlaybook().label);

        return new Fixture(
                analyzer, game, gameState, modifiers,
                hand, reserve, permanents, locations,
                cardsById, cardsAt,
                endor, bunker, platform, cantina);
    }

    private static void setDeployable(
            ModifiersQuerying modifiers) {
        when(modifiers.isDeployable(
                any(), any(), any(), anyBoolean(), any(),
                anyBoolean(), anyFloat(), any(), any(), any(),
                any(), any(), anyBoolean(), anyFloat()))
                .thenReturn(true);
        when(modifiers.isDeployableToTarget(
                any(), any(), any(), anyBoolean(), any(),
                anyBoolean(), anyFloat(), any(), any(), any(),
                any(), any(), any(), anyBoolean(), anyFloat()))
                .thenReturn(true);
    }

    private static void setTargetAwareDeployCosts(
            ModifiersQuerying modifiers) {
        when(modifiers.getDeployCost(
                any(GameState.class),
                any(PhysicalCard.class),
                any(PhysicalCard.class),
                any(PhysicalCard.class),
                anyBoolean(), any(), anyBoolean(),
                anyFloat(), any(), anyBoolean()))
                .thenAnswer(invocation ->
                        modifiers.getDeployCost(
                                invocation.getArgument(
                                        0, GameState.class),
                                invocation.getArgument(
                                        2, PhysicalCard.class)));
        when(modifiers.getSimultaneousDeployCost(
                any(GameState.class),
                any(PhysicalCard.class),
                any(PhysicalCard.class),
                anyBoolean(), anyFloat(),
                any(PhysicalCard.class),
                anyBoolean(), anyFloat(),
                any(PhysicalCard.class),
                any(), anyBoolean()))
                .thenAnswer(invocation -> {
                    GameState gameState =
                            invocation.getArgument(
                                    0, GameState.class);
                    PhysicalCard host =
                            invocation.getArgument(
                                    2, PhysicalCard.class);
                    PhysicalCard pilot =
                            invocation.getArgument(
                                    5, PhysicalCard.class);
                    return modifiers.getDeployCost(
                            gameState, host)
                            + modifiers.getDeployCost(
                                    gameState, pilot);
                });
    }

    private static void setSimultaneousDeployCost(
            Fixture fixture,
            PhysicalCard host, PhysicalCard pilot,
            PhysicalCard target, float cost) {
        when(fixture.modifiers().getSimultaneousDeployCost(
                same(fixture.gameState()),
                same(host), same(host),
                anyBoolean(), anyFloat(),
                same(pilot), anyBoolean(), anyFloat(),
                same(target), isNull(), anyBoolean()))
                .thenReturn(cost);
    }

    private static void setTargetDeployCost(
            Fixture fixture, PhysicalCard card,
            PhysicalCard target, float cost) {
        when(fixture.modifiers().getDeployCost(
                same(fixture.gameState()),
                same(card), same(card), same(target),
                anyBoolean(), isNull(), anyBoolean(),
                anyFloat(), isNull(), anyBoolean()))
                .thenReturn(cost);
    }

    private static AwaitingDecision awaitingDecision(
            Decision decision, int decisionId) {
        AwaitingDecision awaiting =
                mock(AwaitingDecision.class);
        when(awaiting.getDecisionType())
                .thenReturn(AwaitingDecisionType.valueOf(
                        decision.type()));
        when(awaiting.getText()).thenReturn(
                decision.text());
        when(awaiting.getAwaitingDecisionId())
                .thenReturn(decisionId);
        Map<String, String[]> parameters =
                new LinkedHashMap<>();
        parameters.put(
                "cardId",
                decision.cardIds().toArray(
                        new String[0]));
        parameters.put(
                "blueprintId",
                decision.blueprints().toArray(
                        new String[0]));
        parameters.put(
                "selectable",
                java.util.Collections.nCopies(
                        decision.selectionCount(),
                        "true").toArray(new String[0]));
        parameters.put(
                "noPass",
                new String[]{
                    String.valueOf(
                            decision.noPass())});
        parameters.put(
                "min",
                new String[]{
                    String.valueOf(
                            decision.min())});
        parameters.put(
                "max", new String[]{"1"});
        when(awaiting.getDecisionParameters())
                .thenReturn(parameters);
        return awaiting;
    }

    private static AwaitingDecision engineRuleActionDecision(
            PhysicalCard attached,
            String actionText) {
        Action action = mock(Action.class);
        when(action.getActionAttachedToCard())
                .thenReturn(attached);
        when(action.getActionSource())
                .thenReturn(null);
        when(action.getText())
                .thenReturn(actionText);
        return new CardActionSelectionDecision(
                87, "Choose action",
                List.of(action),
                true, false,
                false, false, false) {
            @Override
            public void decisionMade(String result)
                    throws DecisionResultInvalidException {
            }
        };
    }

    private static void setControls(
            Fixture fixture, PhysicalCard location,
            String playerId, boolean controls) {
        when(fixture.modifiers().controlsLocation(
                fixture.gameState(), location, playerId, null))
                .thenReturn(controls);
    }

    private static void setActive(
            GameState gameState, PhysicalCard card) {
        when(gameState.isCardInPlayActive(
                card, false, false, false, false,
                false, false, false, false)).thenReturn(true);
        when(gameState.isCardInPlayActive(
                card, true, false, false, false,
                false, false, false, false)).thenReturn(true);
        when(gameState.isCardInPlayActive(
                card, false, true, false, false,
                false, false, false, false)).thenReturn(true);
    }

    private static void setInactive(
            GameState gameState, PhysicalCard card) {
        when(gameState.isCardInPlayActive(
                card, false, false, false, false,
                false, false, false, false)).thenReturn(false);
        when(gameState.isCardInPlayActive(
                card, true, false, false, false,
                false, false, false, false)).thenReturn(false);
        when(gameState.isCardInPlayActive(
                card, false, true, false, false,
                false, false, false, false)).thenReturn(false);
    }

    private static PhysicalCard objective() {
        return objective(false);
    }

    private static PhysicalCard objective(boolean flipped) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint front = blueprint(OBJECTIVE_BP);
        SwccgCardBlueprint back = blueprint(OBJECTIVE_BP + "_BACK");
        when(card.getOwner()).thenReturn(PLAYER);
        when(card.getZone()).thenReturn(Zone.SIDE_OF_TABLE);
        when(card.getBlueprint())
                .thenReturn(flipped ? back : front);
        when(card.getOtherSideBlueprint())
                .thenReturn(flipped ? front : back);
        when(card.getBlueprintId(true)).thenReturn(OBJECTIVE_BP);
        when(card.getBlueprintId(false)).thenReturn(
                flipped ? OBJECTIVE_BP + "_BACK" : OBJECTIVE_BP);
        when(card.getTitle()).thenReturn(
                (flipped ? back : front).getTitle());
        when(card.getTitles()).thenReturn(
                List.of((flipped ? back : front).getTitle()));
        when(card.getPermanentCardId()).thenReturn(OBJECTIVE_ID);
        when(card.getCardId()).thenReturn(OBJECTIVE_ID);
        when(card.getAdditionalCardIds()).thenReturn(List.of());
        when(card.isFlipped()).thenReturn(flipped);
        return card;
    }

    private static PhysicalCard card(
            String blueprintId, int cardId,
            Zone zone, String owner) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = blueprint(blueprintId);
        when(card.getOwner()).thenReturn(owner);
        when(card.getZone()).thenReturn(zone);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.getBlueprintId(true)).thenReturn(blueprintId);
        when(card.getBlueprintId(false)).thenReturn(blueprintId);
        when(card.getTitle()).thenReturn(blueprint.getTitle());
        when(card.getTitles()).thenReturn(
                List.of(blueprint.getTitle()));
        when(card.getPartOfSystem()).thenReturn(
                blueprint.getCardCategory() == CardCategory.LOCATION
                        ? blueprint.getSystemName() : null);
        when(card.getPermanentCardId()).thenReturn(cardId);
        when(card.getCardId()).thenReturn(cardId);
        when(card.getAdditionalCardIds()).thenReturn(List.of());
        when(card.isBlownAway()).thenReturn(false);
        when(card.isCaptive()).thenReturn(false);
        when(card.isUndercover()).thenReturn(false);
        return card;
    }

    private static SwccgCardBlueprint blueprint(String id) {
        SwccgCardBlueprint blueprint =
                CARDS.getSwccgoCardBlueprint(id);
        assertNotNull("Missing real blueprint " + id, blueprint);
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

    private record RetentionFormation(
            PhysicalCard defender,
            PhysicalCard forest) {
    }

    private record ClassicAtStFormation(
            PhysicalCard atSt,
            PhysicalCard pilot,
            PhysicalCard forest) {
    }

    private record DyerRelocationRoutes(
            PhysicalCard dyer,
            PhysicalCard unsafeForest,
            PhysicalCard safeBackDoor) {
    }

    private record CountedRelocation(
            PhysicalCard mover,
            PhysicalCard openEndorSite) {
    }

    private record Decision(
            String type,
            String text,
            Phase phase,
            List<String> actionIds,
            List<String> actionTexts,
            List<String> cardIds,
            List<String> blueprints,
            List<String> testingTexts,
            boolean noPass,
            int min) {

        private static Decision parentPull() {
            return new Decision(
                    "CARD_ACTION_CHOICE",
                    "Choose control action",
                    Phase.CONTROL,
                    List.of(
                            "objective-pull",
                            "distractor-pull",
                            "pass"),
                    List.of(
                            "Take card from Reserve Deck",
                            "Take card from Reserve Deck",
                            "Pass"),
                    List.of(
                            String.valueOf(OBJECTIVE_ID),
                            String.valueOf(DISTRACTOR_SOURCE_ID),
                            ""),
                    List.of(
                            OBJECTIVE_BP, DISTRACTOR_BP, ""),
                    List.of(
                            blueprint(OBJECTIVE_BP).getTitle(),
                            blueprint(DISTRACTOR_BP).getTitle(),
                            "Pass"),
                    false, 0);
        }

        private static Decision childPull() {
            return pullChoices(
                    DISTRACTOR_BP, OMINOUS_BP);
        }

        private static Decision pullChoices(
                String... blueprintIds) {
            List<String> blueprints = new ArrayList<>();
            List<String> testingTexts =
                    new ArrayList<>();
            for (String blueprintId : blueprintIds) {
                blueprints.add(blueprintId);
                testingTexts.add(
                        blueprint(blueprintId)
                                .getTitle());
            }
            return new Decision(
                    "ARBITRARY_CARDS",
                    "Choose card to take into hand from Reserve Deck",
                    Phase.CONTROL,
                    List.of(), List.of(), List.of(),
                    blueprints, testingTexts,
                    true, 1);
        }

        private static Decision finalEffectDeploy() {
            return new Decision(
                    "CARD_ACTION_CHOICE",
                    "Choose deploy action",
                    Phase.DEPLOY,
                    List.of(
                            "deploy-distractor",
                            "deploy-ominous",
                            "pass"),
                    List.of(
                            deployAction(BIKER_SCOUT_BP),
                            deployAction(OMINOUS_BP),
                            "Pass"),
                    List.of(
                            String.valueOf(BUDGET_DISTRACTOR_ID),
                            String.valueOf(OMINOUS_ID),
                            ""),
                    List.of(
                            BIKER_SCOUT_BP, OMINOUS_BP, ""),
                    List.of(
                            blueprint(BIKER_SCOUT_BP).getTitle(),
                            blueprint(OMINOUS_BP).getTitle(),
                            "Pass"),
                    false, 0);
        }

        private static Decision topLevelDeploy(
                PhysicalCard... candidates) {
            List<String> actionIds = new ArrayList<>();
            List<String> actionTexts = new ArrayList<>();
            List<String> cardIds = new ArrayList<>();
            List<String> blueprints = new ArrayList<>();
            List<String> testingTexts = new ArrayList<>();
            for (PhysicalCard candidate : candidates) {
                actionIds.add(
                        "deploy-" + candidate.getCardId());
                actionTexts.add(deployAction(
                        candidate.getBlueprintId(true)));
                cardIds.add(String.valueOf(
                        candidate.getCardId()));
                blueprints.add(
                        candidate.getBlueprintId(true));
                testingTexts.add(candidate.getTitle());
            }
            actionIds.add("pass");
            actionTexts.add("Pass");
            cardIds.add("");
            blueprints.add("");
            testingTexts.add("Pass");
            return new Decision(
                    "CARD_ACTION_CHOICE",
                    "Choose deploy action",
                    Phase.DEPLOY,
                    actionIds, actionTexts,
                    cardIds, blueprints, testingTexts,
                    false, 0);
        }

        private static Decision simultaneousPilotSelection(
                PhysicalCard ship,
                PhysicalCard... pilots) {
            List<String> cardIds = new ArrayList<>();
            List<String> blueprints = new ArrayList<>();
            List<String> testingTexts = new ArrayList<>();
            for (PhysicalCard pilot : pilots) {
                cardIds.add(String.valueOf(pilot.getCardId()));
                blueprints.add(pilot.getBlueprintId(true));
                testingTexts.add(pilot.getTitle());
            }
            return new Decision(
                    "CARD_SELECTION",
                    "Choose a pilot from hand to simultaneously deploy aboard •"
                            + ship.getTitle(),
                    Phase.DEPLOY,
                    List.of(), List.of(), cardIds,
                    blueprints, testingTexts,
                    true, 1);
        }

        private static Decision deployBikerScout(
                PhysicalCard forest, PhysicalCard distractor) {
            PhysicalCard bikerScout = card(
                    BIKER_SCOUT_BP, BIKER_SCOUT_ID,
                    Zone.HAND, PLAYER);
            return deployCandidate(
                    bikerScout, forest, distractor);
        }

        private static Decision deployCandidate(
                PhysicalCard candidate,
                PhysicalCard... destinations) {
            List<String> cardIds = new ArrayList<>();
            List<String> blueprints = new ArrayList<>();
            List<String> testingTexts = new ArrayList<>();
            for (PhysicalCard destination : destinations) {
                cardIds.add(String.valueOf(
                        destination.getCardId()));
                blueprints.add(
                        destination.getBlueprintId(true));
                testingTexts.add(destination.getTitle());
            }
            return new Decision(
                    "CARD_SELECTION",
                    "Choose where to deploy "
                            + "<div class='cardHint' value='"
                            + candidate.getBlueprintId(true)
                            + "'>" + candidate.getTitle()
                            + "</div>",
                    Phase.DEPLOY,
                    List.of(), List.of(),
                    cardIds, blueprints, testingTexts,
                    true, 1);
        }

        private static Decision optionalDeployCandidate(
                PhysicalCard candidate,
                PhysicalCard destination) {
            return new Decision(
                    "CARD_SELECTION",
                    "Choose where to deploy "
                            + "<div class='cardHint' value='"
                            + candidate.getBlueprintId(true)
                            + "'>" + candidate.getTitle()
                            + "</div>",
                    Phase.DEPLOY,
                    List.of(), List.of(),
                    List.of(String.valueOf(
                            destination.getCardId())),
                    List.of(destination.getBlueprintId(true)),
                    List.of(destination.getTitle()),
                    false, 0);
        }

        private static Decision topLevelMove() {
            return new Decision(
                    "ACTION_CHOICE",
                    "Choose move action",
                    Phase.MOVE,
                    List.of("move-biker-scout", "pass"),
                    List.of("Move using landspeed", "Pass"),
                    List.of(
                            String.valueOf(BIKER_SCOUT_ID), ""),
                    List.of(), List.of(),
                    false, 0);
        }

        private static Decision topLevelMove(
                PhysicalCard mover) {
            String actionId =
                    "move-" + mover.getCardId();
            return new Decision(
                    "ACTION_CHOICE",
                    "Choose move action",
                    Phase.MOVE,
                    List.of(actionId, "pass"),
                    List.of("Move using landspeed", "Pass"),
                    List.of(
                            String.valueOf(mover.getCardId()),
                            ""),
                    List.of(), List.of(),
                    false, 0);
        }

        private static Decision topLevelEmbark(
                PhysicalCard pilot) {
            String actionId =
                    "embark-" + pilot.getCardId();
            return new Decision(
                    "ACTION_CHOICE",
                    "Choose move action",
                    Phase.MOVE,
                    List.of(actionId, "pass"),
                    List.of("Embark", "Pass"),
                    List.of(
                            String.valueOf(
                                    pilot.getCardId()),
                            ""),
                    List.of(
                            pilot.getBlueprintId(true), ""),
                    List.of(
                            pilot.getTitle(), "Pass"),
                    false, 0);
        }

        private static Decision embarkTarget(
                PhysicalCard pilot,
                PhysicalCard... targets) {
            List<String> cardIds = new ArrayList<>();
            List<String> blueprints =
                    new ArrayList<>();
            List<String> testingTexts =
                    new ArrayList<>();
            for (PhysicalCard target : targets) {
                cardIds.add(String.valueOf(
                        target.getCardId()));
                blueprints.add(
                        target.getBlueprintId(true));
                testingTexts.add(target.getTitle());
            }
            return new Decision(
                    "CARD_SELECTION",
                    "Choose where to embark "
                            + "<div class='cardHint' value='"
                            + pilot.getBlueprintId(true)
                            + "'>" + pilot.getTitle()
                            + "</div>",
                    Phase.MOVE,
                    List.of(), List.of(),
                    cardIds, blueprints,
                    testingTexts,
                    true, 1);
        }

        private static Decision moveDestination(
                PhysicalCard mover,
                PhysicalCard... destinations) {
            List<String> cardIds = new ArrayList<>();
            List<String> blueprints =
                    new ArrayList<>();
            List<String> testingTexts =
                    new ArrayList<>();
            for (PhysicalCard destination : destinations) {
                cardIds.add(String.valueOf(
                        destination.getCardId()));
                blueprints.add(
                        destination.getBlueprintId(true));
                testingTexts.add(destination.getTitle());
            }
            return new Decision(
                    "CARD_SELECTION",
                    "Choose where to move "
                            + "<div class='cardHint' value='"
                            + mover.getBlueprintId(true)
                            + "'>" + mover.getTitle()
                            + "</div> using landspeed",
                    Phase.MOVE,
                    List.of(), List.of(),
                    cardIds, blueprints,
                    testingTexts,
                    true, 1);
        }

        private static Decision forfeit(
                PhysicalCard... candidates) {
            List<String> cardIds = new ArrayList<>();
            List<String> blueprints = new ArrayList<>();
            List<String> testingTexts = new ArrayList<>();
            for (PhysicalCard candidate : candidates) {
                cardIds.add(String.valueOf(
                        candidate.getCardId()));
                blueprints.add(
                        candidate.getBlueprintId(true));
                testingTexts.add(candidate.getTitle());
            }
            return new Decision(
                    "CARD_SELECTION",
                    "Choose a card from battle to forfeit",
                    Phase.BATTLE,
                    List.of(), List.of(),
                    cardIds, blueprints, testingTexts,
                    true, 1);
        }

        private static Decision disembark(
                PhysicalCard pilot) {
            return new Decision(
                    "ACTION_CHOICE",
                    "Choose move action",
                    Phase.MOVE,
                    List.of("disembark", "pass"),
                    List.of("Disembark", "Pass"),
                    List.of(
                            String.valueOf(pilot.getCardId()),
                            ""),
                    List.of(), List.of(),
                    false, 0);
        }

        private static Decision forceLoss(
                boolean combinedBattle,
                PhysicalCard... candidates) {
            List<String> cardIds = new ArrayList<>();
            List<String> blueprints = new ArrayList<>();
            List<String> testingTexts = new ArrayList<>();
            for (PhysicalCard candidate : candidates) {
                cardIds.add(String.valueOf(
                        candidate.getCardId()));
                blueprints.add(
                        candidate.getBlueprintId(true));
                testingTexts.add(candidate.getTitle());
            }
            return new Decision(
                    "CARD_SELECTION",
                    combinedBattle
                            ? "Choose Force to lose or card to forfeit"
                            : "Choose Force to lose",
                    Phase.BATTLE,
                    List.of(), List.of(),
                    cardIds, blueprints, testingTexts,
                    true, 1);
        }

        private static Decision reactorCoreDeploy() {
            return new Decision(
                    "CARD_ACTION_CHOICE",
                    "Choose deploy action",
                    Phase.DEPLOY,
                    List.of(
                            "deploy-reactor-core",
                            "pass"),
                    List.of(
                            deployAction(REACTOR_CORE_BP),
                            "Pass"),
                    List.of(
                            String.valueOf(
                                    REACTOR_CORE_ID),
                            ""),
                    List.of(
                            REACTOR_CORE_BP, ""),
                    List.of(
                            blueprint(REACTOR_CORE_BP)
                                    .getTitle(),
                            "Pass"),
                    false, 0);
        }

        private static String deployAction(String blueprintId) {
            return "Deploy <div class='cardHint' value='"
                    + blueprintId + "'>"
                    + blueprint(blueprintId).getTitle()
                    + "</div>";
        }

        private int selectionCount() {
            return !cardIds.isEmpty()
                    ? cardIds.size() : blueprints.size();
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
            List<PhysicalCard> locations,
            Map<Integer, PhysicalCard> cardsById,
            Map<PhysicalCard, List<PhysicalCard>> cardsAt,
            PhysicalCard endor,
            PhysicalCard bunker,
            PhysicalCard platform,
            PhysicalCard cantina) {

        private void addHand(PhysicalCard card) {
            hand.add(card);
            cardsById.put(card.getCardId(), card);
        }

        private void addReserve(PhysicalCard card) {
            reserve.add(card);
            cardsById.put(card.getCardId(), card);
        }

        private void addPermanent(PhysicalCard card) {
            permanents.add(card);
            cardsById.put(card.getCardId(), card);
        }

        private void addActivePermanent(PhysicalCard card) {
            addPermanent(card);
            setActive(gameState, card);
        }

        private void addLocation(PhysicalCard location) {
            locations.add(location);
            addActivePermanent(location);
            when(modifiers.getLocationHere(
                    gameState, location)).thenReturn(location);
            when(modifiers.isBattleground(
                    gameState, location, null)).thenReturn(true);
        }

        private void place(
                PhysicalCard card, PhysicalCard location) {
            addActivePermanent(card);
            when(card.getAtLocation()).thenReturn(location);
            when(modifiers.getLocationThatCardIsPresentAt(
                    gameState, card)).thenReturn(location);
            when(modifiers.getLocationThatCardIsAt(
                    gameState, card)).thenReturn(location);
            cardsAt.computeIfAbsent(
                    location,
                    ignored -> new ArrayList<>())
                    .add(card);
        }

        private void setOpponentControls(
                PhysicalCard location, boolean controls) {
            when(modifiers.controlsLocation(
                    gameState, location, OPPONENT, null))
                    .thenReturn(controls);
            when(modifiers.controlsLocation(
                    gameState, location, OPPONENT,
                    SpotOverride
                            .INCLUDE_EXCLUDED_FROM_BATTLE))
                    .thenReturn(controls);
        }

        private void setOpponentOccupies(
                PhysicalCard location, boolean occupies) {
            when(modifiers.occupiesLocation(
                    gameState, location, OPPONENT, null))
                    .thenReturn(occupies);
            when(modifiers.occupiesLocation(
                    gameState, location, OPPONENT,
                    SpotOverride
                            .INCLUDE_EXCLUDED_FROM_BATTLE))
                    .thenReturn(occupies);
        }

        private void blockDeployTarget(
                PhysicalCard blockedTarget) {
            when(modifiers.isDeployableToTarget(
                    any(GameState.class), any(),
                    any(), anyBoolean(),
                    argThat(
                            (com.gempukku.swccgo.filters.Filter
                                    targetFilter) -> {
                                try {
                                    return targetFilter != null
                                            && targetFilter.accepts(
                                                gameState,
                                                modifiers,
                                                blockedTarget);
                                } catch (Exception e) {
                                    return false;
                                }
                            }),
                    anyBoolean(), anyFloat(), any(), any(),
                    any(), any(), any(), any(),
                    anyBoolean(), anyFloat()))
                    .thenReturn(false);
        }

        private void blockDirectDeploy(
                PhysicalCard blockedCard) {
            when(modifiers.isDeployableToTarget(
                    any(GameState.class), any(),
                    same(blockedCard), anyBoolean(), any(),
                    anyBoolean(), anyFloat(), any(), any(),
                    any(), any(), any(), isNull(),
                    anyBoolean(), anyFloat()))
                    .thenReturn(false);
        }

        private void removeObjective() {
            permanents.removeIf(candidate ->
                    candidate != null
                            && candidate.getBlueprint() != null
                            && candidate.getBlueprint()
                                    .getCardCategory()
                                    == CardCategory.OBJECTIVE);
            analyzer.analyze(game, PLAYER, Side.DARK);
        }
    }
}
