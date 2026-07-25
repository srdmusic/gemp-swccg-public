package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.ai.models.common.phase.BattleDecisionPolicy;
import com.gempukku.swccgo.ai.models.common.phase.ObjectiveBattlePolicy;
import com.gempukku.swccgo.ai.models.common.trace.DecisionTrace;
import com.gempukku.swccgo.ai.models.common.trace.TraceSession;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.CardType;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.BattleState;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.apache.logging.log4j.LogManager;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Production-consumer proof for The First Order Reigns. Every assertion crosses
 * a bot evaluator or the shared battle adapter used by both bots.
 */
public class FirstOrderReignsEvaluatorBehaviorTest {
    private static final String PLAYER = "dark";
    private static final String OPPONENT = "light";

    private static final int HOST_ID = 101;
    private static final int RELOCATED_HOST_ID = 102;
    private static final int STAGE_ID = 103;
    private static final int ORIGIN_ID = 104;
    private static final int CLOSER_ID = 105;
    private static final int SAME_DISTANCE_ID = 106;
    private static final int FARTHER_ID = 107;
    private static final int ILLEGAL_ID = 108;
    private static final int SALT_ID = 109;
    private static final int OTHER_SALT_ID = 110;
    private static final int THREATENED_HOST_ID = 111;
    private static final int SAFE_HOST_ID = 112;
    private static final int BATTLE_SITE_ID = 113;
    private static final int THIRD_PARSEC_FIVE_ID = 114;
    private static final int CRAIT_CAVERN_ID = 115;
    private static final int BETTER_BATTLEGROUND_ID = 116;
    private static final int EMPTY_PAIR_SITE_ID = 117;
    private static final int FULL_PAIR_SITE_ID = 118;

    private static final int FLEET_ID = 201;
    private static final int SUPREMACY_ID = 202;
    private static final int KYLO_ID = 203;
    private static final int HAN_ID = 204;
    private static final int ALTERNATIVE_ID = 205;
    private static final int DEPLOYING_TROOPER_ID = 206;
    private static final int PAIR_MATE_ID = 207;
    private static final int THIRD_PAIR_MATE_ID = 208;
    private static final int DUPLICATE_LOST_PILE_TROOPER_ID =
            209;

    private static final String MOVE_START_RULE =
            "MOVE.OBJECTIVE.ACTOR_LOCATION_START";
    private static final String MOVE_DESTINATION_RULE =
            "MOVE.OBJECTIVE.ACTOR_LOCATION_DESTINATION";
    private static final String FORCE_LOSS_RULE =
            "OBJECTIVE CRITICAL";
    private static final String TERMINAL_FORFEIT_RULE =
            "BATTLE.OBJECTIVE.TERMINAL_ACTOR_HOLD";
    private static final String LOW_RESERVE_RULE =
            "V60 RESERVE RISK";
    private static final String DEPLOY_ROUTE_RESERVE_RULE =
            "OBJECTIVE.FIRST_ORDER_REIGNS.RESERVE_7";
    private static final String DEPLOY_STAGE_RULE =
            "Deploy the required actor to a live qualifying objective location";
    private static final String PENDING_FORFEIT_RULE =
            "BATTLE.OBJECTIVE.PENDING_TRIGGER_HOLD";
    private static final String PRIMARY_PAYOFF_RULE =
            "MOVE.OBJECTIVE.POST_FLIP_PRIMARY_PAYOFF";
    private static final String SECONDARY_PAYOFF_RULE =
            "MOVE.OBJECTIVE.POST_FLIP_SECONDARY_PAYOFF";
    private static final String PAYOFF_START_RULE =
            "MOVE.OBJECTIVE.POST_FLIP_PAYOFF_START";
    private static final String PAYOFF_HOLD_RULE =
            "MOVE.OBJECTIVE.POST_FLIP_PAYOFF_HOLD";
    private static final String DEPLOY_PRIMARY_PAYOFF =
            "primary back-side payoff";
    private static final String DEPLOY_SECONDARY_PAYOFF =
            "secondary back-side payoff";
    private static final String LOST_PILE_ROUTE_RULE =
            "DEPLOY.OBJECTIVE.LOST_PILE_ROUTE";
    private static final String LOST_PILE_NO_LEGAL_RULE =
            "DEPLOY.OBJECTIVE.LOST_PILE_NO_LEGAL_CARD";
    private static final String FIRST_ORDER_DRAIN_PAIR_RULE =
            "DEPLOY.OBJECTIVE.FIRST_ORDER_DRAIN_PAIR";
    private static final String FIRST_ORDER_DRAIN_PAIR_START_RULE =
            "MOVE.OBJECTIVE.FIRST_ORDER_DRAIN_PAIR_START";
    private static final String FIRST_ORDER_DRAIN_PAIR_DESTINATION_RULE =
            "MOVE.OBJECTIVE.FIRST_ORDER_DRAIN_PAIR";
    private static final String FIRST_ORDER_DRAIN_PAIR_HOLD_RULE =
            "MOVE.OBJECTIVE.FIRST_ORDER_DRAIN_PAIR_HOLD";
    private static final String TERMINAL_ESCAPE_START_RULE =
            "MOVE.OBJECTIVE.TERMINAL_ACTOR_ESCAPE_START";
    private static final String TERMINAL_ESCAPE_DESTINATION_RULE =
            "MOVE.OBJECTIVE.TERMINAL_ACTOR_ESCAPE_DESTINATION";
    private static final String TERMINAL_ESCAPE_R3_RULE =
            "LADDER: R3 SURVIVAL base";
    private static final String PULL_REQUIRED_LOCATION_RULE =
            "Pull a missing location required by the counted objective";

    @Test
    public void routeDownloadRequiresADeployableCandidateAtForceBoundary() {
        for (Variant variant : variants()) {
            Fixture paid = fixture(variant, false);
            paid.locations.add(paid.stage);
            paid.reserve.add(paid.supremacy);
            when(paid.gameState.getReserveDeckSize(PLAYER))
                    .thenReturn(1);
            setCandidateDeployableAtForce(
                    paid, paid.supremacy, 7);
            Decision download = Decision.objectiveDownload(
                    variant.blueprintId);

            when(paid.gameState.getForcePileSize(PLAYER))
                    .thenReturn(7);
            Outcome exactSeven = only(
                    actionTextAdapter(paid, download),
                    "download");
            assertNotContains(exactSeven, LOW_RESERVE_RULE);
            assertEquals("download",
                    combined(paid, download).actionId());

            when(paid.gameState.getForcePileSize(PLAYER))
                    .thenReturn(6);
            Outcome exactSix = only(
                    actionTextAdapter(paid, download),
                    "download");
            assertContains(exactSix, LOW_RESERVE_RULE);
            assertEquals("",
                    combined(paid, download).actionId());

            Fixture freeStage = fixture(variant, false);
            freeStage.reserve.add(freeStage.stage);
            when(freeStage.gameState
                    .getReserveDeckSize(PLAYER))
                    .thenReturn(1);
            when(freeStage.gameState
                    .getForcePileSize(PLAYER))
                    .thenReturn(0);
            setDeployable(freeStage.modifiers, false);
            setCandidateDeployable(
                    freeStage, freeStage.stage, true);

            Outcome zeroForceStage = only(
                    actionTextAdapter(
                            freeStage, download),
                    "download");
            assertNotContains(
                    zeroForceStage, LOW_RESERVE_RULE);
            assertEquals("download",
                    combined(freeStage, download)
                            .actionId());
        }
    }

    @Test
    public void deployedSupremacyNeedsAReachableCloserReserveBridge() {
        for (Variant variant : variants()) {
            Fixture unreachable = fixture(variant, false);
            configureDeployedSupremacyBridge(
                    unreachable);
            when(unreachable.farther.getZone())
                    .thenReturn(Zone.RESERVE_DECK);
            when(unreachable.modifiers.hasIcon(
                    unreachable.gameState,
                    unreachable.farther,
                    Icon.EPISODE_VII))
                    .thenReturn(true);
            when(unreachable.modifiers.isBattleground(
                    unreachable.gameState,
                    unreachable.farther, null))
                    .thenReturn(true);
            unreachable.reserve.add(unreachable.farther);
            when(unreachable.gameState
                    .getReserveDeckSize(PLAYER))
                    .thenReturn(1);
            when(unreachable.gameState
                    .getForcePileSize(PLAYER))
                    .thenReturn(7);

            Decision download = Decision.objectiveDownload(
                    variant.blueprintId);
            Outcome unreachableParent = only(
                    actionTextAdapter(
                            unreachable, download),
                    "download");
            assertContains(
                    unreachableParent, LOW_RESERVE_RULE);
            assertEquals(
                    "",
                    combined(unreachable, download)
                            .actionId());

            Fixture bridged = fixture(variant, false);
            configureDeployedSupremacyBridge(bridged);
            when(bridged.stage.getZone())
                    .thenReturn(Zone.RESERVE_DECK);
            when(bridged.farther.getZone())
                    .thenReturn(Zone.RESERVE_DECK);
            when(bridged.modifiers.hasIcon(
                    bridged.gameState,
                    bridged.farther,
                    Icon.EPISODE_VII))
                    .thenReturn(true);
            when(bridged.modifiers.isBattleground(
                    bridged.gameState,
                    bridged.farther, null))
                    .thenReturn(true);
            bridged.reserve.add(bridged.farther);
            bridged.reserve.add(bridged.stage);
            when(bridged.gameState
                    .getReserveDeckSize(PLAYER))
                    .thenReturn(2);
            when(bridged.gameState
                    .getForcePileSize(PLAYER))
                    .thenReturn(7);

            Outcome bridgeParent = only(
                    actionTextAdapter(
                            bridged, download),
                    "download");
            assertNotContains(
                    bridgeParent, LOW_RESERVE_RULE);
            assertEquals(
                    "download",
                    combined(bridged, download)
                            .actionId());

            Decision candidates = Decision.selection(
                    "Choose card to deploy from Reserve Deck",
                    Phase.DEPLOY, null,
                    bridged.farther,
                    bridged.stage);
            List<Outcome> scoredCandidates =
                    cardSelectionAdapter(
                            bridged, candidates);
            Outcome unreachableNine = only(
                    scoredCandidates,
                    id(bridged.farther));
            Outcome reachableSeven = only(
                    scoredCandidates,
                    id(bridged.stage));
            assertNotContains(
                    unreachableNine,
                    PULL_REQUIRED_LOCATION_RULE);
            assertContains(
                    reachableSeven,
                    PULL_REQUIRED_LOCATION_RULE);
            assertEquals(
                    id(bridged.stage),
                    combined(bridged, candidates)
                            .actionId());
        }
    }

    @Test
    public void deployEvaluatorPreservesSevenForceButExemptsSupremacy() {
        for (Variant variant : variants()) {
            Fixture fixture = fixture(variant, false);
            fixture.locations.add(fixture.stage);
            fixture.hand.add(fixture.supremacy);
            fixture.hand.add(fixture.alternative);
            when(fixture.supremacy.getZone())
                    .thenReturn(Zone.HAND);
            when(fixture.alternative.getZone())
                    .thenReturn(Zone.HAND);
            when(fixture.supremacy.getBlueprint()
                    .getDeployCost()).thenReturn(7.0f);
            when(fixture.alternative.getBlueprint()
                    .getDeployCost()).thenReturn(1.0f);

            Decision deploys = Decision.directDeploys(
                    fixture.alternative,
                    fixture.supremacy);

            when(fixture.gameState.getForcePileSize(PLAYER))
                    .thenReturn(7);
            List<Outcome> atSeven =
                    deployAdapter(fixture, deploys);
            Outcome competingAtSeven = only(
                    atSeven, "deploy-" + ALTERNATIVE_ID);
            Outcome supremacyAtSeven = only(
                    atSeven, "deploy-" + SUPREMACY_ID);
            assertTrue(competingAtSeven.hardVeto());
            assertContains(
                    competingAtSeven,
                    DEPLOY_ROUTE_RESERVE_RULE);
            assertFalse(supremacyAtSeven.hardVeto());
            assertNotContains(
                    supremacyAtSeven,
                    DEPLOY_ROUTE_RESERVE_RULE);

            when(fixture.gameState.getForcePileSize(PLAYER))
                    .thenReturn(8);
            Outcome competingAtEight = only(
                    deployAdapter(fixture, deploys),
                    "deploy-" + ALTERNATIVE_ID);
            assertFalse(competingAtEight.hardVeto());
            assertNotContains(
                    competingAtEight,
                    DEPLOY_ROUTE_RESERVE_RULE);
        }
    }

    @Test
    public void supremacyDeployDestinationChoosesTheLegalStagingSystem() {
        for (Variant variant : variants()) {
            Fixture fixture = fixture(variant, false);
            fixture.hand.add(fixture.supremacy);
            when(fixture.supremacy.getZone())
                    .thenReturn(Zone.HAND);
            fixture.locations.add(fixture.stage);
            fixture.locations.add(fixture.farther);

            Decision destinations = Decision.deployDestinations(
                    fixture.supremacy,
                    fixture.stage,
                    fixture.farther);
            List<Outcome> candidates =
                    cardSelectionAdapter(fixture, destinations);
            Outcome stage = only(
                    candidates, id(fixture.stage));
            Outcome nonAdvancing = only(
                    candidates, id(fixture.farther));

            assertContains(stage, DEPLOY_STAGE_RULE);
            assertNotContains(
                    nonAdvancing, DEPLOY_STAGE_RULE);
            assertEquals(id(fixture.stage),
                    combined(fixture, destinations)
                            .actionId());
        }
    }

    @Test
    public void moveEvaluatorsStartRouteAndChooseOnlyLegalCloserHop() {
        for (Variant variant : variants()) {
            Fixture fixture = fixture(variant, false);
            configurePaidRoute(fixture);

            Outcome move = only(
                    moveAdapter(
                            fixture,
                            Decision.topLevelHyperspace()),
                    "move-supremacy");
            assertContains(move, MOVE_START_RULE);

            Decision destinations = Decision.moveDestinations(
                    fixture.supremacy,
                    fixture.closer,
                    fixture.sameDistance,
                    fixture.farther,
                    fixture.illegal);
            Outcome closer = only(
                    cardSelectionAdapter(fixture, destinations),
                    id(fixture.closer));
            Outcome sameDistance = only(
                    cardSelectionAdapter(fixture, destinations),
                    id(fixture.sameDistance));
            Outcome farther = only(
                    cardSelectionAdapter(fixture, destinations),
                    id(fixture.farther));
            Outcome illegal = only(
                    cardSelectionAdapter(fixture, destinations),
                    id(fixture.illegal));

            assertContains(closer, MOVE_DESTINATION_RULE);
            assertNotContains(sameDistance, MOVE_DESTINATION_RULE);
            assertNotContains(farther, MOVE_DESTINATION_RULE);
            assertNotContains(illegal, MOVE_DESTINATION_RULE);
            assertEquals(id(fixture.closer),
                    combined(fixture, destinations).actionId());
        }
    }

    @Test
    public void equalParsecDestinationScoresOnlyThePhysicalFleetHost() {
        for (Variant variant : variants()) {
            Fixture fixture = fixture(variant, false);
            PhysicalCard third = system(
                    fixture, "Bespin", "test_bespin",
                    5, THIRD_PARSEC_FIVE_ID);
            fixture.locations.add(fixture.relocatedHost);
            fixture.locations.add(third);
            fixture.permanents.add(fixture.supremacy);
            when(fixture.trackedFleet.getAttachedTo())
                    .thenReturn(fixture.relocatedHost);
            when(fixture.relocatedHost.getParsec())
                    .thenReturn(5);
            place(fixture, fixture.supremacy, fixture.host);
            configureHyperspace(fixture, 10.0f);

            Decision destinations = Decision.moveDestinations(
                    fixture.supremacy,
                    fixture.relocatedHost, third);
            Outcome exactHost = only(
                    cardSelectionAdapter(fixture, destinations),
                    id(fixture.relocatedHost));
            Outcome thirdSystem = only(
                    cardSelectionAdapter(fixture, destinations),
                    id(third));

            assertContains(exactHost, MOVE_DESTINATION_RULE);
            assertNotContains(thirdSystem, MOVE_DESTINATION_RULE);
            assertEquals(id(fixture.relocatedHost),
                    combined(fixture, destinations).actionId());
        }
    }

    @Test
    public void battleAdapterPreservesPaidMoveForceAtTheExactBoundary() {
        for (Variant variant : variants()) {
            Fixture fixture = fixture(variant, false);
            configurePaidRoute(fixture);
            fixture.locations.add(fixture.battleSite);
            setBattlePower(fixture, 10.0f, 4.0f);
            when(fixture.modifiers.getInitiateBattleCost(
                    fixture.gameState, fixture.battleSite,
                    PLAYER, true)).thenReturn(1.0f);

            when(fixture.gameState.getForcePileSize(PLAYER))
                    .thenReturn(1);
            BattleDecisionPolicy.ScoredAction blocked =
                    onlyBattle(BattleDecisionPolicy.evaluate(
                            battleContext(fixture, 1)));
            assertTrue(hasBattleRule(
                    blocked,
                    ObjectiveBattlePolicy
                            .OBJECTIVE_MOVE_FORCE_RESERVE_RULE_ID));
            assertTrue(hasHardVeto(blocked));

            when(fixture.gameState.getForcePileSize(PLAYER))
                    .thenReturn(2);
            BattleDecisionPolicy.ScoredAction exactBoundary =
                    onlyBattle(BattleDecisionPolicy.evaluate(
                            battleContext(fixture, 2)));
            assertFalse(hasBattleRule(
                    exactBoundary,
                    ObjectiveBattlePolicy
                            .OBJECTIVE_MOVE_FORCE_RESERVE_RULE_ID));
            assertFalse(hasHardVeto(exactBoundary));
        }
    }

    @Test
    public void battleAdapterPrefersTheDynamicFleetHostContest() {
        for (Variant variant : variants()) {
            Fixture fixture = fixture(variant, false);
            fixture.locations.add(fixture.battleSite);
            setBattlePowerAt(
                    fixture, fixture.host,
                    10.0f, 4.0f);
            setBattlePowerAt(
                    fixture, fixture.battleSite,
                    10.0f, 4.0f);
            when(fixture.modifiers.getInitiateBattleCost(
                    fixture.gameState, fixture.host,
                    PLAYER, true)).thenReturn(0.0f);
            when(fixture.modifiers.getInitiateBattleCost(
                    fixture.gameState, fixture.battleSite,
                    PLAYER, true)).thenReturn(0.0f);

            List<BattleDecisionPolicy.ScoredAction> actions =
                    BattleDecisionPolicy.evaluate(
                            battleContext(
                                    fixture, 10,
                                    fixture.host,
                                    fixture.battleSite));
            BattleDecisionPolicy.ScoredAction host =
                    battle(actions, id(fixture.host));
            BattleDecisionPolicy.ScoredAction ordinary =
                    battle(actions, id(fixture.battleSite));

            assertTrue(hasBattleRule(
                    host,
                    ObjectiveBattlePolicy
                            .REQUIRED_LOCATION_CONTEST_RULE_ID));
            assertFalse(hasBattleRule(
                    ordinary,
                    ObjectiveBattlePolicy
                            .REQUIRED_LOCATION_CONTEST_RULE_ID));
            assertEquals(
                    ObjectiveBattlePolicy
                            .REQUIRED_LOCATION_CONTEST_BONUS,
                    battleScore(host)
                            - battleScore(ordinary),
                    0.0f);
            assertTrue(
                    battleScore(host)
                            > battleScore(ordinary));
        }
    }

    @Test
    public void forceLossEvaluatorProtectsSoleHandSupremacy() {
        for (Variant variant : variants()) {
            Fixture fixture = fixture(variant, false);
            fixture.locations.add(fixture.stage);
            fixture.hand.add(fixture.supremacy);
            fixture.hand.add(fixture.alternative);
            when(fixture.supremacy.getZone())
                    .thenReturn(Zone.HAND);
            when(fixture.alternative.getZone())
                    .thenReturn(Zone.HAND);

            Decision decision = Decision.forceLoss(
                    fixture.supremacy,
                    fixture.alternative);
            List<Outcome> candidates =
                    cardSelectionAdapter(fixture, decision);
            Outcome supremacy = only(
                    candidates, id(fixture.supremacy));
            Outcome expendable = only(
                    candidates, id(fixture.alternative));

            assertContains(supremacy, FORCE_LOSS_RULE);
            assertNotContains(
                    expendable, FORCE_LOSS_RULE);
            assertEquals(id(fixture.alternative),
                    combined(fixture, decision).actionId());
            assertFalse(
                    fixture.analyzer
                            .isPreferredFirstOrderReignsRouteForceLossCandidate(
                                fixture.game, PLAYER,
                                fixture.trackedFleet));
        }
    }

    @Test
    public void terminalDeployAndMoveVetoOnlyTheThreatenedPhysicalSalt() {
        for (Variant variant : variants()) {
            Fixture fixture = fixture(variant, true);
            armThreatenedSalt(fixture);
            fixture.hand.add(fixture.kylo);

            Decision deploy = Decision.deployKylo(
                    fixture.threatenedHost,
                    fixture.safeHost);
            Outcome threatenedDeploy = only(
                    cardSelectionAdapter(fixture, deploy),
                    id(fixture.threatenedHost));
            Outcome safeDeploy = only(
                    cardSelectionAdapter(fixture, deploy),
                    id(fixture.safeHost));
            assertTrue(threatenedDeploy.hardVeto());
            assertNotContains(
                    threatenedDeploy, DEPLOY_PRIMARY_PAYOFF);
            assertNotContains(
                    threatenedDeploy, DEPLOY_SECONDARY_PAYOFF);
            assertFalse(safeDeploy.hardVeto());
            assertEquals(id(fixture.safeHost),
                    combined(fixture, deploy).actionId());

            fixture.hand.clear();
            fixture.permanents.add(fixture.kylo);
            place(fixture, fixture.kylo, fixture.origin);
            Decision move = Decision.moveDestinations(
                    fixture.kylo,
                    fixture.salt,
                    fixture.otherSalt);
            Outcome threatenedMove = only(
                    cardSelectionAdapter(fixture, move),
                    id(fixture.salt));
            Outcome safeMove = only(
                    cardSelectionAdapter(fixture, move),
                    id(fixture.otherSalt));
            assertTrue(threatenedMove.hardVeto());
            assertFalse(safeMove.hardVeto());
            assertEquals(id(fixture.otherSalt),
                    combined(fixture, move).actionId());
        }
    }

    @Test
    public void postFlipDeployRanksSafeSaltBeforeSecondaryCrait() {
        for (Variant variant : variants()) {
            Fixture fixture = fixture(variant, true);
            PhysicalCard cavern = craitCavern(fixture);
            fixture.locations.add(fixture.salt);
            fixture.locations.add(cavern);
            fixture.locations.add(fixture.battleSite);
            fixture.hand.add(fixture.kylo);
            when(fixture.kylo.getZone())
                    .thenReturn(Zone.HAND);

            Decision bothPayoffs = Decision.deployKylo(
                    fixture.salt, cavern);
            List<Outcome> bothCandidates =
                    cardSelectionAdapter(
                            fixture, bothPayoffs);
            Outcome primary = only(
                    bothCandidates, id(fixture.salt));
            Outcome secondary = only(
                    bothCandidates, id(cavern));
            assertContains(
                    primary, DEPLOY_PRIMARY_PAYOFF);
            assertContains(
                    secondary, DEPLOY_SECONDARY_PAYOFF);
            assertEquals(id(fixture.salt),
                    combined(fixture, bothPayoffs)
                            .actionId());

            Decision saltUnavailable = Decision.deployKylo(
                    cavern, fixture.battleSite);
            Outcome fallback = only(
                    cardSelectionAdapter(
                            fixture, saltUnavailable),
                    id(cavern));
            assertContains(
                    fallback, DEPLOY_SECONDARY_PAYOFF);
            assertEquals(id(cavern),
                    combined(fixture, saltUnavailable)
                            .actionId());
        }
    }

    @Test
    public void postFlipMoveStartsPayoffAndStopsScoringDuplicates() {
        List<String> parentHoldFailures =
                new ArrayList<>();
        for (Variant variant : variants()) {
            Fixture open = fixture(variant, true);
            PhysicalCard openCavern = craitCavern(open);
            open.locations.add(open.salt);
            open.locations.add(openCavern);
            open.locations.add(open.battleSite);
            open.permanents.add(open.kylo);
            place(open, open.kylo, open.battleSite);
            configureLandspeed(
                    open, open.kylo, open.battleSite,
                    open.salt, openCavern);

            Outcome moveStart = only(
                    moveAdapter(
                            open,
                            Decision.topLevelLandspeed(
                                    open.kylo)),
                    "move-" + KYLO_ID);
            assertContains(
                    moveStart, PAYOFF_START_RULE);

            Decision payoffDestinations =
                    Decision.moveDestinationsUsingLandspeed(
                            open.kylo,
                            open.salt, openCavern);
            List<Outcome> openDestinations =
                    cardSelectionAdapter(
                            open, payoffDestinations);
            assertContains(
                    only(openDestinations, id(open.salt)),
                    PRIMARY_PAYOFF_RULE);
            assertContains(
                    only(openDestinations, id(openCavern)),
                    SECONDARY_PAYOFF_RULE);
            assertEquals(id(open.salt),
                    combined(open, payoffDestinations)
                            .actionId());

            Fixture satisfied = fixture(variant, true);
            PhysicalCard satisfiedCavern =
                    craitCavern(satisfied);
            satisfied.locations.add(satisfied.salt);
            satisfied.locations.add(satisfiedCavern);
            satisfied.locations.add(satisfied.battleSite);
            satisfied.permanents.add(satisfied.kylo);
            place(satisfied, satisfied.kylo, satisfied.salt);
            when(satisfied.modifiers.controlsLocation(
                    satisfied.gameState,
                    satisfied.salt, PLAYER))
                    .thenReturn(true);
            when(satisfied.modifiers.controlsLocation(
                    satisfied.gameState,
                    satisfied.salt, PLAYER,
                    (Map<com.gempukku.swccgo.common.InactiveReason,
                            Boolean>) null))
                    .thenReturn(true);
            when(satisfied.modifiers.occupiesLocation(
                    satisfied.gameState,
                    satisfied.salt, PLAYER))
                    .thenReturn(true);
            when(satisfied.modifiers.occupiesLocation(
                    satisfied.gameState,
                    satisfied.salt, PLAYER,
                    (Map<com.gempukku.swccgo.common.InactiveReason,
                            Boolean>) null))
                    .thenReturn(true);
            configureLandspeed(
                    satisfied, satisfied.kylo,
                    satisfied.salt,
                    satisfiedCavern,
                    satisfied.battleSite);

            Decision holdParent =
                    Decision.topLevelLandspeed(
                            satisfied.kylo);
            Outcome holdMove = only(
                    moveAdapter(satisfied, holdParent),
                    "move-" + KYLO_ID);
            Outcome holdPass = only(
                    passAdapter(satisfied, holdParent),
                    "pass");
            Outcome holdWinner =
                    combined(satisfied, holdParent);
            if (!"pass".equals(
                    holdWinner.actionId())) {
                parentHoldFailures.add(
                        variant + " move="
                                + holdMove.score()
                                + " pass=" + holdPass.score()
                                + " winner=" + holdWinner);
            }

            Outcome satisfiedStart = only(
                    moveAdapter(
                            satisfied,
                            holdParent),
                    "move-" + KYLO_ID);
            assertNotContains(
                    satisfiedStart, PAYOFF_START_RULE);
            Decision noDuplicate =
                    Decision.moveDestinationsUsingLandspeed(
                            satisfied.kylo,
                            satisfiedCavern,
                            satisfied.battleSite);
            List<Outcome> retainedPrimary =
                    cardSelectionAdapter(
                            satisfied, noDuplicate);
            for (Outcome destination : retainedPrimary) {
                assertNotContains(
                        destination, PRIMARY_PAYOFF_RULE);
                assertNotContains(
                        destination, SECONDARY_PAYOFF_RULE);
                assertContains(
                        destination, PAYOFF_HOLD_RULE);
                assertContains(
                        destination, "(-1600.0)");
                assertFalse(destination.hardVeto());
            }

            Fixture secondary = fixture(variant, true);
            PhysicalCard secondaryCavern =
                    craitCavern(secondary);
            secondary.locations.add(secondaryCavern);
            secondary.locations.add(secondary.battleSite);
            secondary.permanents.add(secondary.kylo);
            place(secondary, secondary.kylo,
                    secondaryCavern);
            when(secondary.modifiers.occupiesLocation(
                    secondary.gameState,
                    secondaryCavern, PLAYER))
                    .thenReturn(true);
            when(secondary.modifiers.occupiesLocation(
                    secondary.gameState,
                    secondaryCavern, PLAYER,
                    (Map<com.gempukku.swccgo.common.InactiveReason,
                            Boolean>) null))
                    .thenReturn(true);
            configureLandspeed(
                    secondary, secondary.kylo,
                    secondaryCavern,
                    secondary.battleSite);

            Outcome retainedSecondary = only(
                    cardSelectionAdapter(
                            secondary,
                            Decision.moveDestinationsUsingLandspeed(
                                    secondary.kylo,
                                    secondary.battleSite)),
                    id(secondary.battleSite));
            assertContains(
                    retainedSecondary, PAYOFF_HOLD_RULE);
            assertContains(
                    retainedSecondary, "(-900.0)");
            assertFalse(retainedSecondary.hardVeto());

            Fixture threatened = fixture(variant, true);
            PhysicalCard threatenedCavern =
                    craitCavern(threatened);
            armThreatenedSalt(threatened);
            threatened.locations.add(threatenedCavern);
            threatened.locations.add(
                    threatened.battleSite);
            threatened.permanents.add(threatened.kylo);
            place(threatened, threatened.kylo,
                    threatened.salt);
            when(threatened.modifiers.controlsLocation(
                    threatened.gameState,
                    threatened.salt, PLAYER))
                    .thenReturn(true);
            when(threatened.modifiers.controlsLocation(
                    threatened.gameState,
                    threatened.salt, PLAYER,
                    (Map<com.gempukku.swccgo.common.InactiveReason,
                            Boolean>) null))
                    .thenReturn(true);
            when(threatened.modifiers.occupiesLocation(
                    threatened.gameState,
                    threatened.salt, PLAYER))
                    .thenReturn(true);
            when(threatened.modifiers.occupiesLocation(
                    threatened.gameState,
                    threatened.salt, PLAYER,
                    (Map<com.gempukku.swccgo.common.InactiveReason,
                            Boolean>) null))
                    .thenReturn(true);
            configureLandspeed(
                    threatened, threatened.kylo,
                    threatened.salt,
                    threatenedCavern,
                    threatened.battleSite);

            Decision escapeParent =
                    Decision.topLevelLandspeed(
                            threatened.kylo);
            Outcome escapeMove = only(
                    moveAdapter(threatened, escapeParent),
                    "move-" + KYLO_ID);
            Outcome escapePass = only(
                    passAdapter(threatened, escapeParent),
                    "pass");
            Outcome escapeWinner =
                    combined(threatened, escapeParent);
            assertFalse(escapeMove.hardVeto());
            assertEquals(
                    "Terminal threat should keep the escape parent "
                            + "available above Pass; move="
                            + escapeMove
                            + " pass=" + escapePass.score()
                            + " winner=" + escapeWinner,
                    "move-" + KYLO_ID,
                    escapeWinner.actionId());

            for (Outcome escape :
                    cardSelectionAdapter(
                            threatened,
                            Decision.moveDestinationsUsingLandspeed(
                                    threatened.kylo,
                                    threatenedCavern,
                                    threatened.battleSite))) {
                assertNotContains(
                        escape, PAYOFF_HOLD_RULE);
                assertFalse(escape.hardVeto());
            }
        }
        assertTrue(
                "Safe primary payoff should hold instead of "
                        + "opening a lower-value child choice: "
                        + parentHoldFailures,
                parentHoldFailures.isEmpty());
    }

    @Test
    public void terminalKyloEscapeOverridesOnlyOriginFormationVeto() {
        for (Variant variant : variants()) {
            Fixture fixture = fixture(variant, true);
            PhysicalCard safeDestination = fixture.battleSite;
            PhysicalCard doomedDestination =
                    craitCavern(fixture);

            fixture.locations.add(fixture.salt);
            fixture.locations.add(safeDestination);
            fixture.locations.add(doomedDestination);
            fixture.permanents.add(fixture.kylo);
            fixture.permanents.add(fixture.alternative);
            fixture.permanents.add(fixture.han);
            place(
                    fixture, fixture.kylo,
                    fixture.salt);
            place(
                    fixture, fixture.alternative,
                    fixture.salt);
            place(
                    fixture, fixture.han,
                    fixture.salt);
            configureLandspeed(
                    fixture, fixture.kylo,
                    fixture.salt,
                    safeDestination,
                    doomedDestination);
            setBattlePowerAt(
                    fixture, fixture.salt,
                    8.0f, 13.0f);
            setBattlePowerAt(
                    fixture, safeDestination,
                    0.0f, 0.0f);
            setBattlePowerAt(
                    fixture, doomedDestination,
                    0.0f, 13.0f);

            assertTrue(
                    "Kylo must be in the exact physical Salt "
                            + "terminal-loss conjunction",
                    fixture.analyzer
                            .isFirstOrderReignsTerminalExposureAt(
                                    fixture.game, PLAYER,
                                    fixture.kylo,
                                    fixture.salt));
            assertFalse(
                    fixture.analyzer
                            .isFirstOrderReignsTerminalExposureAt(
                                    fixture.game, PLAYER,
                                    fixture.kylo,
                                    safeDestination));
            String originVeto =
                    com.gempukku.swccgo.ai.models.common
                            .strategy.FormationSafety
                            .vetoMoveOrigin(
                                    fixture.game,
                                    fixture.gameState,
                                    PLAYER,
                                    fixture.kylo,
                                    fixture.salt);
            assertNotNull(originVeto);
            assertTrue(originVeto.contains(
                    "L1 ABANDON SOLO"));
            assertEquals(
                    "The terminal fixture must stay below the "
                            + "ordinary doomed-origin exemption",
                    5.0f, 13.0f - 8.0f, 0.0f);

            Decision parent =
                    Decision.topLevelLandspeed(
                            fixture.kylo);
            TracedActions tracedParent =
                    tracedMoveAdapter(
                            fixture, parent);
            Outcome move = only(
                    tracedParent.actions(),
                    "move-" + KYLO_ID);
            Outcome pass = only(
                    passAdapter(fixture, parent),
                    "pass");
            Outcome winner =
                    combined(fixture, parent);
            assertHasTypedRule(
                    tracedParent.trace(),
                    "move-" + KYLO_ID,
                    TERMINAL_ESCAPE_START_RULE);
            assertContains(
                    move,
                    TERMINAL_ESCAPE_START_RULE);
            assertContains(
                    move,
                    TERMINAL_ESCAPE_R3_RULE);
            assertFalse(move.hardVeto());
            assertTrue(
                    "Terminal escape R3 must beat Pass; move="
                            + move + " pass=" + pass,
                    move.score() > pass.score());
            assertEquals(
                    "move-" + KYLO_ID,
                    winner.actionId());

            Decision children =
                    Decision
                        .moveDestinationsUsingLandspeed(
                            fixture.kylo,
                            safeDestination,
                            doomedDestination);
            TracedActions tracedChildren =
                    tracedCardSelectionAdapter(
                            fixture, children);
            Outcome safe = only(
                    tracedChildren.actions(),
                    id(safeDestination));
            Outcome doomed = only(
                    tracedChildren.actions(),
                    id(doomedDestination));
            assertHasTypedRule(
                    tracedChildren.trace(),
                    id(safeDestination),
                    TERMINAL_ESCAPE_DESTINATION_RULE);
            assertFalse(
                    "Terminal escape must bypass only the "
                            + "origin L1 veto: " + safe,
                    safe.hardVeto());
            assertTrue(
                    "Unsafe destination must retain L4: "
                            + doomed,
                    doomed.hardVeto());
            assertNotNull(doomed.vetoReason());
            assertTrue(
                    doomed.vetoReason().contains(
                            "L4 SOLO CHARGE"));
            assertEquals(
                    id(safeDestination),
                    combined(fixture, children)
                            .actionId());
        }
    }

    @Test
    public void backLostPileDeployBeatsImplicitPassWhenTargetExists() {
        for (Variant variant : variants()) {
            Fixture fixture = fixture(variant, true);
            when(fixture.alternative.getZone())
                    .thenReturn(Zone.LOST_PILE);
            when(fixture.modifiers.hasKeyword(
                    fixture.gameState,
                    fixture.alternative,
                    Keyword.STORMTROOPER))
                    .thenReturn(true);
            when(fixture.gameState.getLostPile(PLAYER))
                    .thenReturn(List.of(fixture.alternative));

            Decision decision =
                    Decision.backLostPileDeploy();
            TracedActions tracedLegal =
                    tracedActionTextAdapter(
                            fixture, decision);
            Outcome actionText = only(
                    tracedLegal.actions(),
                    "lost-pile-deploy");
            Outcome deploy = only(
                    deployAdapter(fixture, decision),
                    "lost-pile-deploy");
            Outcome pass = only(
                    passAdapter(fixture, decision), "");
            Outcome winner = combined(fixture, decision);
            assertHasTypedRule(
                    tracedLegal.trace(),
                    "lost-pile-deploy",
                    LOST_PILE_ROUTE_RULE);
            assertFalse(actionText.hardVeto());
            assertEquals(
                    "Legal Lost Pile route must beat Pass; "
                            + variant + " actionText="
                            + actionText.score()
                            + " deploy=" + deploy.score()
                            + " mergedCandidate="
                            + (actionText.score()
                                    + deploy.score())
                            + " pass=" + pass.score()
                            + " winner=" + winner,
                    "lost-pile-deploy",
                    winner.actionId());

            Fixture emptyRoute = fixture(variant, true);
            when(emptyRoute.kylo.getZone())
                    .thenReturn(Zone.LOST_PILE);
            when(emptyRoute.gameState.getLostPile(PLAYER))
                    .thenReturn(List.of(emptyRoute.kylo));
            Decision noLegalTarget =
                    Decision.backLostPileDeploy();
            TracedActions tracedNoMatch =
                    tracedActionTextAdapter(
                            emptyRoute, noLegalTarget);
            Outcome noLegalAction = only(
                    tracedNoMatch.actions(),
                    "lost-pile-deploy");
            assertHasTypedRule(
                    tracedNoMatch.trace(),
                    "lost-pile-deploy",
                    LOST_PILE_NO_LEGAL_RULE);
            assertTrue(noLegalAction.hardVeto());
            assertEquals(
                    "",
                    combined(emptyRoute, noLegalTarget)
                            .actionId());

            Fixture undeployable = fixture(variant, true);
            when(undeployable.alternative.getZone())
                    .thenReturn(Zone.LOST_PILE);
            when(undeployable.modifiers.hasKeyword(
                    undeployable.gameState,
                    undeployable.alternative,
                    Keyword.STORMTROOPER))
                    .thenReturn(true);
            when(undeployable.gameState.getLostPile(PLAYER))
                    .thenReturn(
                            List.of(undeployable.alternative));
            setDeployable(undeployable.modifiers, false);
            TracedActions tracedUndeployable =
                    tracedActionTextAdapter(
                            undeployable,
                            Decision.backLostPileDeploy());
            Outcome undeployableAction = only(
                    tracedUndeployable.actions(),
                    "lost-pile-deploy");
            assertHasTypedRule(
                    tracedUndeployable.trace(),
                    "lost-pile-deploy",
                    LOST_PILE_NO_LEGAL_RULE);
            assertTrue(undeployableAction.hardVeto());

            Fixture front = fixture(variant, false);
            when(front.alternative.getZone())
                    .thenReturn(Zone.LOST_PILE);
            when(front.modifiers.hasKeyword(
                    front.gameState,
                    front.alternative,
                    Keyword.STORMTROOPER))
                    .thenReturn(true);
            when(front.gameState.getLostPile(PLAYER))
                    .thenReturn(List.of(front.alternative));
            TracedActions tracedFront =
                    tracedActionTextAdapter(
                            front,
                            Decision.backLostPileDeploy());
            Outcome frontAction = only(
                    tracedFront.actions(),
                    "lost-pile-deploy");
            assertNoTypedRule(
                    tracedFront.trace(),
                    "lost-pile-deploy",
                    LOST_PILE_ROUTE_RULE);
            assertNoTypedRule(
                    tracedFront.trace(),
                    "lost-pile-deploy",
                    LOST_PILE_NO_LEGAL_RULE);

            Fixture wrongSource = fixture(variant, true);
            when(wrongSource.alternative.getZone())
                    .thenReturn(Zone.LOST_PILE);
            when(wrongSource.modifiers.hasKeyword(
                    wrongSource.gameState,
                    wrongSource.alternative,
                    Keyword.STORMTROOPER))
                    .thenReturn(true);
            when(wrongSource.gameState.getLostPile(PLAYER))
                    .thenReturn(
                            List.of(wrongSource.alternative));
            TracedActions tracedWrongSource =
                    tracedActionTextAdapter(
                            wrongSource,
                            Decision.backLostPileDeploy(
                                    "Deploy card from Lost Pile",
                                    id(wrongSource.kylo)));
            Outcome wrongSourceAction = only(
                    tracedWrongSource.actions(),
                    "lost-pile-deploy");
            assertNoTypedRule(
                    tracedWrongSource.trace(),
                    "lost-pile-deploy",
                    LOST_PILE_ROUTE_RULE);
            assertNoTypedRule(
                    tracedWrongSource.trace(),
                    "lost-pile-deploy",
                    LOST_PILE_NO_LEGAL_RULE);

            TracedActions tracedNearText =
                    tracedActionTextAdapter(
                            wrongSource,
                            Decision.backLostPileDeploy(
                                    "Deploy a card from Lost Pile",
                                    "1"));
            Outcome nearTextAction = only(
                    tracedNearText.actions(),
                    "lost-pile-deploy");
            assertNoTypedRule(
                    tracedNearText.trace(),
                    "lost-pile-deploy",
                    LOST_PILE_ROUTE_RULE);
            assertNoTypedRule(
                    tracedNearText.trace(),
                    "lost-pile-deploy",
                    LOST_PILE_NO_LEGAL_RULE);
        }
    }

    @Test
    public void botLostPileDeployLatchCarriesExactPhysicalCopy()
            throws Exception {
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(
                    new Variant(bot, "225_32"), true);
            PhysicalCard firstCopy = card(
                    "First Order Stormtrooper",
                    "204_40", PLAYER, Zone.LOST_PILE,
                    CardCategory.CHARACTER, null,
                    DEPLOYING_TROOPER_ID);
            PhysicalCard selectedCopy = card(
                    "First Order Stormtrooper",
                    "204_40", PLAYER, Zone.LOST_PILE,
                    CardCategory.CHARACTER, null,
                    DUPLICATE_LOST_PILE_TROOPER_ID);
            register(fixture.cards, firstCopy);
            register(fixture.cards, selectedCopy);
            when(fixture.modifiers.hasIcon(
                    fixture.gameState,
                    firstCopy,
                    Icon.FIRST_ORDER)).thenReturn(true);
            when(fixture.modifiers.hasIcon(
                    fixture.gameState,
                    selectedCopy,
                    Icon.FIRST_ORDER)).thenReturn(true);
            when(fixture.gameState.getLostPile(PLAYER))
                    .thenReturn(List.of(
                            firstCopy, selectedCopy));
            when(fixture.gameState.getCurrentPhase())
                    .thenReturn(Phase.DEPLOY);
            assertTrue(
                    com.gempukku.swccgo.filters.Filters
                            .First_Order_character.accepts(
                                    fixture.gameState,
                                    fixture.modifiers,
                                    firstCopy));
            assertTrue(
                    com.gempukku.swccgo.filters.Filters
                            .First_Order_character.accepts(
                                    fixture.gameState,
                                    fixture.modifiers,
                                    selectedCopy));

            Decision parent =
                    Decision.lostPileCardSelection(
                            firstCopy, selectedCopy);
            Object ai;
            Object parentContext;
            Object selected;
            if (bot == Bot.RANDO) {
                ai = new com.gempukku.swccgo.ai.models.rando
                        .RandoCalAi();
                parentContext =
                        randoContext(fixture, parent);
                selected =
                        new com.gempukku.swccgo.ai.models.rando
                                .evaluators.EvaluatedAction(
                                "temp1",
                                com.gempukku.swccgo.ai.models.rando
                                        .evaluators.ActionType
                                        .ARBITRARY,
                                0.0f,
                                "selected Lost Pile copy");
            } else {
                ai = new com.gempukku.swccgo.ai.models.chosenone
                        .TheChosenOneAi();
                parentContext =
                        chosenContext(fixture, parent);
                selected =
                        new com.gempukku.swccgo.ai.models.chosenone
                                .evaluators.EvaluatedAction(
                                "temp1",
                                com.gempukku.swccgo.ai.models
                                        .chosenone.evaluators
                                        .ActionType.ARBITRARY,
                                0.0f,
                                "selected Lost Pile copy");
            }

            Method remember = ai.getClass()
                    .getDeclaredMethod(
                            "rememberSelectedLostPileDeployCard",
                            parentContext.getClass(),
                            selected.getClass());
            remember.setAccessible(true);
            remember.invoke(
                    ai, parentContext, selected);

            AwaitingDecision child =
                    mock(AwaitingDecision.class);
            when(child.getDecisionType())
                    .thenReturn(
                            AwaitingDecisionType
                                    .CARD_SELECTION);
            when(child.getText()).thenReturn(
                    "Choose where to deploy "
                            + "<div class='cardHint' "
                            + "value='204_40'>"
                            + "First Order Stormtrooper</div>");
            when(child.getAwaitingDecisionId())
                    .thenReturn(91);
            when(child.getDecisionParameters())
                    .thenReturn(Map.of());

            Method builder = ai.getClass()
                    .getDeclaredMethod(
                            "buildEvaluatorContext",
                            String.class,
                            AwaitingDecision.class,
                            GameState.class,
                            boolean.class);
            builder.setAccessible(true);
            Object built = builder.invoke(
                    ai, PLAYER, child,
                    fixture.gameState, false);
            Method getExtra = built.getClass()
                    .getMethod(
                            "getExtra", String.class);
            Object latched = getExtra.invoke(
                    built,
                    ObjectiveAnalyzer
                            .OBJECTIVE_DEPLOYING_CARD_ID_EXTRA);
            assertEquals(
                    selectedCopy.getPermanentCardId(),
                    latched);
            assertFalse(
                    "Duplicate blueprint must not substitute "
                            + "the unselected physical card",
                    Integer.valueOf(
                            firstCopy.getPermanentCardId())
                            .equals(latched));

            Object next = builder.invoke(
                    ai, PLAYER, child,
                    fixture.gameState, false);
            assertNull(getExtra.invoke(
                    next,
                    ObjectiveAnalyzer
                            .OBJECTIVE_DEPLOYING_CARD_ID_EXTRA));
        }
    }

    @Test
    public void postFlipLostPileTrooperJoinsTheOneBodyDrainPair() {
        for (Variant variant : variants()) {
            Fixture fixture = fixture(variant, true);
            PhysicalCard cavern = craitCavern(fixture);
            PhysicalCard richerBattleground = card(
                    "Tatooine: Cantina",
                    "test_richer_battleground",
                    PLAYER, Zone.LOCATIONS,
                    CardCategory.LOCATION,
                    CardSubtype.SITE,
                    BETTER_BATTLEGROUND_ID);
            register(fixture.cards, richerBattleground);
            PhysicalCard fullPairBattleground = card(
                    "Tatooine: Mos Eisley",
                    "test_full_pair_battleground",
                    PLAYER, Zone.LOCATIONS,
                    CardCategory.LOCATION,
                    CardSubtype.SITE,
                    FULL_PAIR_SITE_ID);
            register(
                    fixture.cards,
                    fullPairBattleground);
            PhysicalCard deployingTrooper = card(
                    "First Order Stormtrooper",
                    "204_40", PLAYER, Zone.LOST_PILE,
                    CardCategory.CHARACTER, null,
                    DEPLOYING_TROOPER_ID);
            register(fixture.cards, deployingTrooper);
            PhysicalCard fullPairFirst = card(
                    "First Order Executioner",
                    "200_88", PLAYER, Zone.AT_LOCATION,
                    CardCategory.CHARACTER, null,
                    PAIR_MATE_ID);
            register(fixture.cards, fullPairFirst);
            PhysicalCard fullPairSecond = card(
                    "First Order Stormtrooper",
                    "test_second_pair_trooper",
                    PLAYER, Zone.AT_LOCATION,
                    CardCategory.CHARACTER, null,
                    THIRD_PAIR_MATE_ID);
            register(fixture.cards, fullPairSecond);
            setActive(
                    fixture.gameState,
                    fullPairFirst, true);
            setActive(
                    fixture.gameState,
                    fullPairSecond, true);

            when(fixture.alternative.getBlueprint()
                    .hasAbilityAttribute()).thenReturn(true);
            when(fixture.alternative.getBlueprint()
                    .getAbility()).thenReturn(2.0f);
            when(fixture.alternative.getBlueprint()
                    .hasPowerAttribute()).thenReturn(true);
            when(fixture.alternative.getBlueprint()
                    .getPower()).thenReturn(3.0f);
            when(deployingTrooper.getBlueprint()
                    .hasAbilityAttribute()).thenReturn(true);
            when(deployingTrooper.getBlueprint()
                    .getAbility()).thenReturn(2.0f);
            when(deployingTrooper.getBlueprint()
                    .hasPowerAttribute()).thenReturn(true);
            when(deployingTrooper.getBlueprint()
                    .getPower()).thenReturn(3.0f);
            when(deployingTrooper.getBlueprint()
                    .getDeployCost()).thenReturn(2.0f);
            when(fixture.modifiers.hasIcon(
                    fixture.gameState,
                    fixture.alternative,
                    Icon.FIRST_ORDER)).thenReturn(true);
            when(fixture.modifiers.hasIcon(
                    fixture.gameState,
                    deployingTrooper,
                    Icon.FIRST_ORDER)).thenReturn(true);
            when(fixture.modifiers.hasIcon(
                    fixture.gameState,
                    fullPairFirst,
                    Icon.FIRST_ORDER)).thenReturn(true);
            when(fixture.modifiers.hasIcon(
                    fixture.gameState,
                    fullPairSecond,
                    Icon.FIRST_ORDER)).thenReturn(true);

            fixture.locations.add(cavern);
            fixture.locations.add(fixture.battleSite);
            fixture.locations.add(richerBattleground);
            fixture.locations.add(
                    fullPairBattleground);
            fixture.permanents.add(fixture.kylo);
            fixture.permanents.add(fixture.alternative);
            fixture.permanents.add(fullPairFirst);
            fixture.permanents.add(fullPairSecond);
            place(fixture, fixture.kylo, cavern);
            place(fixture, fixture.alternative,
                    fixture.battleSite);
            place(
                    fixture, fullPairFirst,
                    fullPairBattleground);
            place(
                    fixture, fullPairSecond,
                    fullPairBattleground);
            when(fixture.gameState.getLostPile(PLAYER))
                    .thenReturn(List.of(deployingTrooper));
            when(fixture.modifiers.occupiesLocation(
                    fixture.gameState, cavern, PLAYER))
                    .thenReturn(true);
            when(fixture.modifiers.occupiesLocation(
                    fixture.gameState, cavern, PLAYER,
                    (Map<com.gempukku.swccgo.common.InactiveReason,
                            Boolean>) null))
                    .thenReturn(true);
            when(fixture.modifiers.isBattleground(
                    fixture.gameState,
                    fixture.battleSite, null))
                    .thenReturn(true);
            when(fixture.modifiers.isBattleground(
                    fixture.gameState,
                    richerBattleground, null))
                    .thenReturn(true);
            when(fixture.modifiers.isBattleground(
                    fixture.gameState,
                    fullPairBattleground, null))
                    .thenReturn(true);
            when(fixture.battleSite.getBlueprint()
                    .getIconCount(Icon.LIGHT_FORCE))
                    .thenReturn(1);
            when(richerBattleground.getBlueprint()
                    .getIconCount(Icon.LIGHT_FORCE))
                    .thenReturn(2);
            setBattlePowerAt(
                    fixture, fixture.battleSite,
                    3.0f, 0.0f);
            setBattlePowerAt(
                    fixture, richerBattleground,
                    0.0f, 0.0f);

            assertTrue(
                    com.gempukku.swccgo.filters.Filters
                            .First_Order_character.accepts(
                                    fixture.gameState,
                                    fixture.modifiers,
                                    fixture.alternative));
            assertTrue(
                    com.gempukku.swccgo.filters.Filters
                            .First_Order_character.accepts(
                                    fixture.gameState,
                                    fixture.modifiers,
                                    deployingTrooper));
            assertEquals(
                    ObjectiveAnalyzer.ObjectivePostFlipPayoffRole
                            .SECONDARY,
                    fixture.analyzer
                            .classifyPostFlipPayoffRoleAt(
                                    fixture.game, PLAYER,
                                    fixture.kylo, cavern));

            Decision destinations =
                    Decision.deployDestinations(
                            deployingTrooper,
                            fixture.battleSite,
                            richerBattleground);
            TracedActions tracedCandidates =
                    tracedCardSelectionAdapter(
                            fixture, destinations);
            List<Outcome> candidates =
                    tracedCandidates.actions();
            Outcome pair = only(
                    candidates, id(fixture.battleSite));
            Outcome richerSolo = only(
                    candidates, id(richerBattleground));
            assertHasTypedRule(
                    tracedCandidates.trace(),
                    id(fixture.battleSite),
                    FIRST_ORDER_DRAIN_PAIR_RULE);
            assertNoTypedRule(
                    tracedCandidates.trace(),
                    id(richerBattleground),
                    FIRST_ORDER_DRAIN_PAIR_RULE);
            assertTrue(
                    "Exactly-one-FO battleground must beat the "
                            + "higher-drain empty battleground; pair="
                            + pair + " richerSolo=" + richerSolo,
                    pair.score() > richerSolo.score());
            assertEquals(
                    id(fixture.battleSite),
                    combined(fixture, destinations)
                            .actionId());

            TracedActions overfilledPair =
                    tracedCardSelectionAdapter(
                            fixture,
                            Decision.deployDestinations(
                                    deployingTrooper,
                                    fullPairBattleground));
            assertNoTypedRule(
                    overfilledPair.trace(),
                    id(fullPairBattleground),
                    FIRST_ORDER_DRAIN_PAIR_RULE);
        }
    }

    @Test
    public void postFlipMovementCompletesAndKeepsExactFirstOrderDrainPair() {
        List<String> parentStartFailures =
                new ArrayList<>();
        List<String> parentHoldFailures =
                new ArrayList<>();
        for (Variant variant : variants()) {
            Fixture open = fixture(variant, true);
            PhysicalCard openCavern = craitCavern(open);
            PhysicalCard openOrigin = card(
                    "Tatooine: Cantina",
                    "test_pair_origin",
                    PLAYER, Zone.LOCATIONS,
                    CardCategory.LOCATION,
                    CardSubtype.SITE,
                    BETTER_BATTLEGROUND_ID);
            register(open.cards, openOrigin);
            PhysicalCard emptyPairSite = card(
                    "Tatooine: Jabba's Palace",
                    "test_empty_pair_site",
                    PLAYER, Zone.LOCATIONS,
                    CardCategory.LOCATION,
                    CardSubtype.SITE,
                    EMPTY_PAIR_SITE_ID);
            register(open.cards, emptyPairSite);
            PhysicalCard pairMate = card(
                    "First Order Executioner",
                    "200_88", PLAYER, Zone.AT_LOCATION,
                    CardCategory.CHARACTER, null,
                    PAIR_MATE_ID);
            register(open.cards, pairMate);
            setActive(open.gameState, pairMate, true);
            when(open.modifiers.hasIcon(
                    open.gameState,
                    open.alternative,
                    Icon.FIRST_ORDER)).thenReturn(true);
            when(open.modifiers.hasIcon(
                    open.gameState,
                    pairMate,
                    Icon.FIRST_ORDER)).thenReturn(true);
            when(open.modifiers.isBattleground(
                    open.gameState,
                    open.battleSite, null))
                    .thenReturn(true);
            when(open.modifiers.isBattleground(
                    open.gameState,
                    openOrigin, null))
                    .thenReturn(true);
            when(open.modifiers.isBattleground(
                    open.gameState,
                    emptyPairSite, null))
                    .thenReturn(true);
            when(open.modifiers.occupiesLocation(
                    open.gameState,
                    openCavern, PLAYER))
                    .thenReturn(true);
            when(open.modifiers.occupiesLocation(
                    open.gameState,
                    openCavern, PLAYER,
                    (Map<com.gempukku.swccgo.common.InactiveReason,
                            Boolean>) null))
                    .thenReturn(true);
            open.locations.add(openCavern);
            open.locations.add(open.battleSite);
            open.locations.add(openOrigin);
            open.locations.add(emptyPairSite);
            open.permanents.add(open.kylo);
            open.permanents.add(open.alternative);
            open.permanents.add(pairMate);
            place(open, open.kylo, openCavern);
            place(open, open.alternative, openOrigin);
            place(open, pairMate, open.battleSite);
            configureLandspeed(
                    open, open.alternative, openOrigin,
                    open.battleSite, emptyPairSite);
            assertTrue(
                    com.gempukku.swccgo.filters.Filters
                            .First_Order_character.accepts(
                                    open.gameState,
                                    open.modifiers,
                                    open.alternative));
            assertTrue(
                    com.gempukku.swccgo.filters.Filters
                            .First_Order_character.accepts(
                                    open.gameState,
                                    open.modifiers,
                                    pairMate));
            assertTrue(
                    open.analyzer
                            .hasFirstOrderReignsKyloCraitPayoff(
                                    open.game, PLAYER));
            assertTrue(
                    open.analyzer
                            .advancesFirstOrderReignsDrainPairAt(
                                    open.game, PLAYER,
                                    open.alternative,
                                    open.battleSite));

            Decision startParent =
                    Decision.topLevelLandspeed(
                            open.alternative);
            TracedActions tracedStart =
                    tracedMoveAdapter(
                            open, startParent);
            Outcome startMove = only(
                    tracedStart.actions(),
                    "move-" + ALTERNATIVE_ID);
            assertHasTypedRule(
                    tracedStart.trace(),
                    "move-" + ALTERNATIVE_ID,
                    FIRST_ORDER_DRAIN_PAIR_START_RULE);
            Outcome startPass = only(
                    passAdapter(open, startParent),
                    "pass");
            Outcome startWinner =
                    combined(open, startParent);
            if (!startMove.reasoning().stream()
                    .anyMatch(reason -> reason.contains(
                            FIRST_ORDER_DRAIN_PAIR_START_RULE))
                    || !("move-" + ALTERNATIVE_ID)
                            .equals(startWinner.actionId())) {
                parentStartFailures.add(
                        variant + " move=" + startMove
                                + " pass=" + startPass.score()
                                + " winner=" + startWinner);
            }

            TracedActions pairDestinations =
                    tracedCardSelectionAdapter(
                            open,
                            Decision
                                .moveDestinationsUsingLandspeed(
                                    open.alternative,
                                    open.battleSite,
                                    emptyPairSite));
            Outcome pair = only(
                    pairDestinations.actions(),
                    id(open.battleSite));
            Outcome noPair = only(
                    pairDestinations.actions(),
                    id(emptyPairSite));
            assertHasTypedRule(
                    pairDestinations.trace(),
                    id(open.battleSite),
                    FIRST_ORDER_DRAIN_PAIR_DESTINATION_RULE);
            assertNoTypedRule(
                    pairDestinations.trace(),
                    id(emptyPairSite),
                    FIRST_ORDER_DRAIN_PAIR_DESTINATION_RULE);
            assertTrue(
                    "Pair-completing move destination must beat "
                            + "the non-pair destination; pair="
                            + pair + " noPair=" + noPair,
                    pair.score() > noPair.score());
            assertEquals(
                    id(open.battleSite),
                    combined(
                            open,
                            Decision
                                .moveDestinationsUsingLandspeed(
                                    open.alternative,
                                    open.battleSite,
                                    emptyPairSite))
                            .actionId());

            Fixture exactPair = fixture(
                    variant, true);
            PhysicalCard holdCavern =
                    craitCavern(exactPair);
            PhysicalCard holdDestination = card(
                    "Tatooine: Cantina",
                    "test_pair_destination",
                    PLAYER, Zone.LOCATIONS,
                    CardCategory.LOCATION,
                    CardSubtype.SITE,
                    BETTER_BATTLEGROUND_ID);
            register(
                    exactPair.cards, holdDestination);
            PhysicalCard holdMate = card(
                    "First Order Executioner",
                    "200_88", PLAYER, Zone.AT_LOCATION,
                    CardCategory.CHARACTER, null,
                    PAIR_MATE_ID);
            register(exactPair.cards, holdMate);
            setActive(
                    exactPair.gameState,
                    holdMate, true);
            when(exactPair.modifiers.hasIcon(
                    exactPair.gameState,
                    exactPair.alternative,
                    Icon.FIRST_ORDER)).thenReturn(true);
            when(exactPair.modifiers.hasIcon(
                    exactPair.gameState,
                    holdMate,
                    Icon.FIRST_ORDER)).thenReturn(true);
            when(exactPair.modifiers.isBattleground(
                    exactPair.gameState,
                    exactPair.battleSite, null))
                    .thenReturn(true);
            when(exactPair.modifiers.isBattleground(
                    exactPair.gameState,
                    holdDestination, null))
                    .thenReturn(true);
            when(exactPair.modifiers.occupiesLocation(
                    exactPair.gameState,
                    holdCavern, PLAYER))
                    .thenReturn(true);
            when(exactPair.modifiers.occupiesLocation(
                    exactPair.gameState,
                    holdCavern, PLAYER,
                    (Map<com.gempukku.swccgo.common.InactiveReason,
                            Boolean>) null))
                    .thenReturn(true);
            exactPair.locations.add(holdCavern);
            exactPair.locations.add(
                    exactPair.battleSite);
            exactPair.locations.add(
                    holdDestination);
            exactPair.permanents.add(exactPair.kylo);
            exactPair.permanents.add(
                    exactPair.alternative);
            exactPair.permanents.add(holdMate);
            place(
                    exactPair, exactPair.kylo,
                    holdCavern);
            place(
                    exactPair, exactPair.alternative,
                    exactPair.battleSite);
            place(
                    exactPair, holdMate,
                    exactPair.battleSite);
            configureLandspeed(
                    exactPair,
                    exactPair.alternative,
                    exactPair.battleSite,
                    holdDestination);

            TracedActions heldDestination =
                    tracedCardSelectionAdapter(
                            exactPair,
                            Decision
                                .moveDestinationsUsingLandspeed(
                                    exactPair.alternative,
                                    holdDestination));
            Outcome held = only(
                    heldDestination.actions(),
                    id(holdDestination));
            assertHasTypedRule(
                    heldDestination.trace(),
                    id(holdDestination),
                    FIRST_ORDER_DRAIN_PAIR_HOLD_RULE);
            assertContains(
                    held,
                    FIRST_ORDER_DRAIN_PAIR_HOLD_RULE);
            assertContains(held, "(-900.0)");
            assertFalse(held.hardVeto());

            Decision holdParent =
                    Decision.topLevelLandspeed(
                            exactPair.alternative);
            TracedActions tracedHold =
                    tracedMoveAdapter(
                            exactPair, holdParent);
            Outcome holdMove = only(
                    tracedHold.actions(),
                    "move-" + ALTERNATIVE_ID);
            assertHasTypedRule(
                    tracedHold.trace(),
                    "move-" + ALTERNATIVE_ID,
                    FIRST_ORDER_DRAIN_PAIR_HOLD_RULE);
            Outcome holdPass = only(
                    passAdapter(exactPair, holdParent),
                    "pass");
            Outcome holdWinner =
                    combined(exactPair, holdParent);
            if (!"pass".equals(
                    holdWinner.actionId())) {
                parentHoldFailures.add(
                        variant + " move=" + holdMove
                                + " pass=" + holdPass.score()
                                + " winner=" + holdWinner);
            }

            PhysicalCard thirdPairMate = card(
                    "First Order Stormtrooper",
                    "test_third_pair_trooper",
                    PLAYER, Zone.AT_LOCATION,
                    CardCategory.CHARACTER, null,
                    THIRD_PAIR_MATE_ID);
            register(
                    exactPair.cards, thirdPairMate);
            setActive(
                    exactPair.gameState,
                    thirdPairMate, true);
            when(exactPair.modifiers.hasIcon(
                    exactPair.gameState,
                    thirdPairMate,
                    Icon.FIRST_ORDER)).thenReturn(true);
            exactPair.permanents.add(thirdPairMate);
            place(
                    exactPair, thirdPairMate,
                    exactPair.battleSite);
            assertFalse(
                    exactPair.analyzer
                            .isFirstOrderReignsDrainPairMemberAtExactPair(
                                    exactPair.game, PLAYER,
                                    exactPair.alternative));

            TracedActions releasedDestination =
                    tracedCardSelectionAdapter(
                            exactPair,
                            Decision
                                .moveDestinationsUsingLandspeed(
                                    exactPair.alternative,
                                    holdDestination));
            assertNoTypedRule(
                    releasedDestination.trace(),
                    id(holdDestination),
                    FIRST_ORDER_DRAIN_PAIR_HOLD_RULE);
            TracedActions releasedParent =
                    tracedMoveAdapter(
                            exactPair, holdParent);
            assertNoTypedRule(
                    releasedParent.trace(),
                    "move-" + ALTERNATIVE_ID,
                    FIRST_ORDER_DRAIN_PAIR_HOLD_RULE);
        }
        assertTrue(
                "Top-level move must pursue a legal exact-two "
                        + "First Order drain pair: "
                        + parentStartFailures,
                parentStartFailures.isEmpty());
        assertTrue(
                "Top-level move must not abandon an active "
                        + "exact-two First Order drain pair: "
                        + parentHoldFailures,
                parentHoldFailures.isEmpty());
    }

    @Test
    public void terminalForfeitEvaluatorKeepsKyloWhenAlternativeExists() {
        for (Variant variant : variants()) {
            Fixture fixture = fixture(variant, true);
            armThreatenedSalt(fixture);
            fixture.permanents.add(fixture.kylo);
            fixture.permanents.add(fixture.alternative);
            place(fixture, fixture.kylo, fixture.salt);
            place(fixture, fixture.alternative, fixture.salt);

            BattleState battleState = mock(BattleState.class);
            when(battleState.getBattleLocation())
                    .thenReturn(fixture.salt);
            when(battleState.isLoser(PLAYER))
                    .thenReturn(true);
            when(fixture.gameState.getBattleState())
                    .thenReturn(battleState);

            Decision forfeit = Decision.forfeit(
                    fixture.kylo, fixture.alternative);
            List<Outcome> candidates =
                    cardSelectionAdapter(fixture, forfeit);
            Outcome kylo = only(candidates, id(fixture.kylo));
            Outcome alternative = only(
                    candidates, id(fixture.alternative));
            assertContains(kylo, TERMINAL_FORFEIT_RULE);
            assertNotContains(alternative, TERMINAL_FORFEIT_RULE);
            assertEquals(id(fixture.alternative),
                    combined(fixture, forfeit).actionId());
        }
    }

    @Test
    public void pendingTriggerForfeitProtectsOnlyTheSoleController() {
        for (Variant variant : variants()) {
            Fixture supremacyFixture =
                    fixture(variant, false);
            armPendingHost(
                    supremacyFixture,
                    supremacyFixture.supremacy, null);
            Decision supremacyForfeit = Decision.forfeit(
                    supremacyFixture.supremacy,
                    supremacyFixture.alternative);
            Outcome soleSupremacy = only(
                    cardSelectionAdapter(
                            supremacyFixture,
                            supremacyForfeit),
                    id(supremacyFixture.supremacy));
            assertContains(
                    soleSupremacy, PENDING_FORFEIT_RULE);
            assertEquals(
                    id(supremacyFixture.alternative),
                    combined(
                            supremacyFixture,
                            supremacyForfeit).actionId());

            Fixture ordinaryFixture =
                    fixture(variant, false);
            armPendingHost(
                    ordinaryFixture,
                    ordinaryFixture.kylo, null);
            Decision ordinaryForfeit = Decision.forfeit(
                    ordinaryFixture.kylo,
                    ordinaryFixture.alternative);
            Outcome soleOrdinaryController = only(
                    cardSelectionAdapter(
                            ordinaryFixture,
                            ordinaryForfeit),
                    id(ordinaryFixture.kylo));
            assertContains(
                    soleOrdinaryController,
                    PENDING_FORFEIT_RULE);
            assertEquals(
                    id(ordinaryFixture.alternative),
                    combined(
                            ordinaryFixture,
                            ordinaryForfeit).actionId());

            Fixture releasedFixture =
                    fixture(variant, false);
            armPendingHost(
                    releasedFixture,
                    releasedFixture.supremacy,
                    releasedFixture.kylo);
            Decision releasedForfeit = Decision.forfeit(
                    releasedFixture.supremacy,
                    releasedFixture.alternative);
            Outcome releasedSupremacy = only(
                    cardSelectionAdapter(
                            releasedFixture,
                            releasedForfeit),
                    id(releasedFixture.supremacy));
            assertNotContains(
                    releasedSupremacy,
                    PENDING_FORFEIT_RULE);
        }
    }

    private static List<Variant> variants() {
        List<Variant> variants = new ArrayList<>();
        for (String blueprintId
                : List.of("225_32", "501_60")) {
            for (Bot bot : Bot.values()) {
                variants.add(new Variant(bot, blueprintId));
            }
        }
        return variants;
    }

    private static Fixture fixture(
            Variant variant, boolean flipped) {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers =
                mock(ModifiersQuerying.class);
        Map<Integer, PhysicalCard> cards =
                new LinkedHashMap<>();
        List<PhysicalCard> permanents =
                new ArrayList<>();
        List<PhysicalCard> locations =
                new ArrayList<>();
        List<PhysicalCard> hand = new ArrayList<>();
        List<PhysicalCard> reserve = new ArrayList<>();
        Map<PhysicalCard, List<PhysicalCard>> cardsAt =
                new LinkedHashMap<>();

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(game.getOpponent(PLAYER)).thenReturn(OPPONENT);
        when(gameState.getGame()).thenReturn(game);
        when(gameState.getOpponent(PLAYER)).thenReturn(OPPONENT);
        when(gameState.getOpponent(OPPONENT)).thenReturn(PLAYER);
        when(gameState.getCurrentPlayerId()).thenReturn(PLAYER);
        when(gameState.getPlayersLatestTurnNumber(PLAYER))
                .thenReturn(3);
        when(gameState.getReserveDeckSize(PLAYER))
                .thenReturn(20);
        when(gameState.getPlayerLifeForce(PLAYER))
                .thenReturn(20);
        when(gameState.getPlayerLifeForce(OPPONENT))
                .thenReturn(20);
        when(gameState.getForcePileSize(PLAYER))
                .thenReturn(10);
        when(gameState.getUsedPile(PLAYER))
                .thenReturn(List.of());
        when(gameState.getForcePile(PLAYER))
                .thenReturn(List.of());
        when(gameState.getAllStackedCards())
                .thenReturn(List.of());
        when(gameState.getAllPermanentCards())
                .thenAnswer(invocation ->
                        new ArrayList<>(permanents));
        when(gameState.getLocationsInOrder())
                .thenAnswer(invocation ->
                        new ArrayList<>(locations));
        when(gameState.getTopLocations())
                .thenAnswer(invocation ->
                        new ArrayList<>(locations));
        when(gameState.getHand(PLAYER))
                .thenAnswer(invocation ->
                        new ArrayList<>(hand));
        when(gameState.getReserveDeck(PLAYER))
                .thenAnswer(invocation ->
                        new ArrayList<>(reserve));
        when(gameState.getCardPile(
                PLAYER, Zone.RESERVE_DECK))
                .thenAnswer(invocation ->
                        new ArrayList<>(reserve));
        when(gameState.findCardById(anyInt()))
                .thenAnswer(invocation ->
                        cards.get(invocation.getArgument(0)));
        when(gameState.findCardByPermanentId(anyInt()))
                .thenAnswer(invocation ->
                        cards.get(invocation.getArgument(0)));
        when(gameState.getCardsAtLocation(any()))
                .thenAnswer(invocation ->
                        new ArrayList<>(cardsAt.getOrDefault(
                                invocation.getArgument(0),
                                List.of())));
        setDeployable(modifiers, true);

        PhysicalCard objective = objective(
                variant.blueprintId, flipped);
        register(cards, objective);
        permanents.add(objective);

        Fixture fixture = new Fixture(
                variant, game, gameState, modifiers,
                cards, permanents, locations, hand,
                reserve, cardsAt,
                null, null, null, null, null, null,
                null, null, null, null, null, null,
                null, null, null, null, null, null,
                null);

        PhysicalCard host = system(
                fixture, "D'Qar", "211_19",
                5, HOST_ID);
        PhysicalCard relocatedHost = system(
                fixture, "Takodana", "211_37",
                5, RELOCATED_HOST_ID);
        PhysicalCard stage = system(
                fixture, "Ahch-To", "test_stage",
                7, STAGE_ID);
        when(modifiers.hasIcon(
                gameState, stage, Icon.EPISODE_VII))
                .thenReturn(true);
        when(modifiers.isBattleground(
                gameState, stage, null))
                .thenReturn(true);
        PhysicalCard origin = system(
                fixture, "Crait", "225_15",
                8, ORIGIN_ID);
        PhysicalCard closer = system(
                fixture, "Kijimi", "214_6",
                6, CLOSER_ID);
        PhysicalCard sameDistance = system(
                fixture, "Bespin", "test_same",
                8, SAME_DISTANCE_ID);
        PhysicalCard farther = system(
                fixture, "Tatooine", "test_far",
                9, FARTHER_ID);
        PhysicalCard illegal = system(
                fixture, "Coruscant", "test_illegal",
                7, ILLEGAL_ID);
        PhysicalCard salt = card(
                "Crait: Salt Plateau", "225_17",
                PLAYER, Zone.LOCATIONS,
                CardCategory.LOCATION,
                CardSubtype.SITE, SALT_ID);
        when(salt.getPartOfSystem())
                .thenReturn("Crait");
        register(cards, salt);
        PhysicalCard otherSalt = card(
                "Crait: Salt Plateau", "test_other_salt",
                PLAYER, Zone.LOCATIONS,
                CardCategory.LOCATION,
                CardSubtype.SITE, OTHER_SALT_ID);
        when(otherSalt.getPartOfSystem())
                .thenReturn("Crait");
        register(cards, otherSalt);
        PhysicalCard threatenedHost = card(
                "First Order Shuttle", "test_host_one",
                PLAYER, Zone.AT_LOCATION,
                CardCategory.STARSHIP,
                null, THREATENED_HOST_ID);
        register(cards, threatenedHost);
        PhysicalCard safeHost = card(
                "First Order Transport", "test_host_two",
                PLAYER, Zone.AT_LOCATION,
                CardCategory.STARSHIP,
                null, SAFE_HOST_ID);
        register(cards, safeHost);
        PhysicalCard battleSite = card(
                "Tatooine: Mos Espa", "test_battle",
                PLAYER, Zone.LOCATIONS,
                CardCategory.LOCATION,
                CardSubtype.SITE, BATTLE_SITE_ID);
        register(cards, battleSite);

        PhysicalCard trackedFleet = card(
                "Tracked Fleet", "225_34",
                PLAYER, Zone.SIDE_OF_TABLE,
                CardCategory.EPIC_EVENT,
                null, FLEET_ID);
        register(cards, trackedFleet);
        when(trackedFleet.getAttachedTo())
                .thenReturn(host);
        PhysicalCard supremacy = card(
                "Supremacy", "225_27",
                PLAYER, Zone.AT_LOCATION,
                CardCategory.STARSHIP,
                null, SUPREMACY_ID);
        register(cards, supremacy);
        when(modifiers.getCardTypes(
                gameState, supremacy))
                .thenReturn(Set.of(CardType.STARSHIP));
        PhysicalCard kylo = card(
                "Kylo Ren", "204_43",
                PLAYER, Zone.HAND,
                CardCategory.CHARACTER,
                null, KYLO_ID);
        register(cards, kylo);
        PhysicalCard han = card(
                "Han Solo", "test_han",
                OPPONENT, Zone.AT_LOCATION,
                CardCategory.CHARACTER,
                null, HAN_ID);
        register(cards, han);
        PhysicalCard alternative = card(
                "First Order Stormtrooper", "204_40",
                PLAYER, Zone.AT_LOCATION,
                CardCategory.CHARACTER,
                null, ALTERNATIVE_ID);
        register(cards, alternative);
        when(modifiers.hasPersona(
                gameState, kylo, Persona.KYLO))
                .thenReturn(true);
        when(modifiers.hasPersona(
                gameState, han, Persona.HAN))
                .thenReturn(true);
        when(modifiers.hasAbility(
                gameState, kylo, true))
                .thenReturn(true);
        setActive(gameState, trackedFleet, true);
        setActive(gameState, supremacy, true);
        setActive(gameState, kylo, true);
        setActive(gameState, han, true);
        setActive(gameState, alternative, true);

        permanents.add(trackedFleet);
        locations.add(host);
        ObjectiveAnalyzer analyzer =
                variant.bot == Bot.RANDO
                ? new com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer()
                : new com.gempukku.swccgo.ai.models.chosenone.strategy
                    .ObjectiveAnalyzer();
        analyzer.analyze(game, PLAYER, Side.DARK);

        return new Fixture(
                variant, game, gameState, modifiers,
                cards, permanents, locations, hand,
                reserve, cardsAt,
                analyzer, host, relocatedHost, stage,
                origin, closer, sameDistance, farther,
                illegal, salt, otherSalt, threatenedHost,
                safeHost, battleSite, trackedFleet,
                supremacy, kylo, han, alternative);
    }

    private static void configurePaidRoute(Fixture fixture) {
        fixture.locations.add(fixture.origin);
        fixture.locations.add(fixture.closer);
        fixture.locations.add(fixture.sameDistance);
        fixture.locations.add(fixture.farther);
        fixture.locations.add(fixture.illegal);
        fixture.permanents.add(fixture.supremacy);
        place(fixture, fixture.supremacy, fixture.origin);
        configureHyperspace(fixture, 2.0f);
        when(fixture.modifiers
                .mayNotMoveFromLocationToLocationUsingHyperspeed(
                        fixture.gameState, fixture.supremacy,
                        fixture.origin, fixture.illegal, false))
                .thenReturn(true);
        when(fixture.modifiers.getMoveUsingHyperspeedCost(
                fixture.gameState, fixture.supremacy,
                fixture.origin, fixture.closer,
                false, 0.0f)).thenReturn(1.0f);
    }

    private static void configureDeployedSupremacyBridge(
            Fixture fixture) {
        fixture.locations.add(fixture.origin);
        fixture.permanents.add(fixture.supremacy);
        when(fixture.trackedFleet.getAttachedTo())
                .thenReturn(fixture.origin);
        place(fixture, fixture.supremacy, fixture.host);
        configureHyperspace(fixture, 2.0f);
    }

    private static void configureHyperspace(
            Fixture fixture, float hyperspeed) {
        when(fixture.modifiers.isPiloted(
                fixture.gameState,
                fixture.supremacy, false))
                .thenReturn(true);
        when(fixture.modifiers.hasAstromechOrNavComputer(
                fixture.gameState, fixture.supremacy))
                .thenReturn(true);
        when(fixture.modifiers.getHyperspeed(
                eq(fixture.gameState),
                eq(fixture.supremacy),
                any(PhysicalCard.class),
                any(PhysicalCard.class)))
                .thenReturn(hyperspeed);
        when(fixture.modifiers.getForceAvailableToUse(
                fixture.gameState, PLAYER))
                .thenReturn(10);
    }

    private static void configureLandspeed(
            Fixture fixture,
            PhysicalCard mover,
            PhysicalCard origin,
            PhysicalCard... destinations) {
        when(fixture.modifiers.getLandspeed(
                fixture.gameState, mover))
                .thenReturn(1.0f);
        when(fixture.modifiers.getForceAvailableToUse(
                fixture.gameState, PLAYER))
                .thenReturn(10);
        for (PhysicalCard destination : destinations) {
            when(fixture.modifiers.getLandspeedRequired(
                    fixture.gameState, mover,
                    destination)).thenReturn(1);
            when(fixture.modifiers
                    .getMoveUsingLandspeedCost(
                        fixture.gameState, mover,
                        origin, destination,
                        false, 0.0f)).thenReturn(0.0f);
        }
    }

    private static void armThreatenedSalt(Fixture fixture) {
        fixture.locations.add(fixture.salt);
        fixture.locations.add(fixture.otherSalt);
        fixture.permanents.add(fixture.han);
        fixture.permanents.add(fixture.threatenedHost);
        fixture.permanents.add(fixture.safeHost);
        place(fixture, fixture.han, fixture.salt);
        place(fixture, fixture.threatenedHost,
                fixture.salt);
        place(fixture, fixture.safeHost,
                fixture.otherSalt);
    }

    private static void armPendingHost(
            Fixture fixture,
            PhysicalCard controller,
            PhysicalCard secondController) {
        SwccgCardBlueprint sacrificeBlueprint =
                fixture.alternative.getBlueprint();
        when(sacrificeBlueprint.getCardCategory())
                .thenReturn(CardCategory.WEAPON);
        when(sacrificeBlueprint.hasAbilityAttribute())
                .thenReturn(false);
        when(fixture.modifiers.controlsLocation(
                fixture.gameState, fixture.host,
                PLAYER)).thenReturn(true);
        when(fixture.modifiers.hasIcon(
                fixture.gameState, controller,
                Icon.PRESENCE)).thenReturn(true);

        fixture.permanents.add(controller);
        fixture.permanents.add(fixture.alternative);
        place(fixture, controller, fixture.host);
        place(fixture, fixture.alternative, fixture.host);

        if (secondController != null) {
            when(fixture.modifiers.hasIcon(
                    fixture.gameState,
                    secondController,
                    Icon.PRESENCE)).thenReturn(true);
            fixture.permanents.add(secondController);
            place(fixture, secondController, fixture.host);
        }

        BattleState battleState = mock(BattleState.class);
        when(battleState.getBattleLocation())
                .thenReturn(fixture.host);
        when(battleState.isLoser(PLAYER))
                .thenReturn(true);
        when(fixture.gameState.getBattleState())
                .thenReturn(battleState);
    }

    private static void place(
            Fixture fixture, PhysicalCard card,
            PhysicalCard location) {
        CardCategory category =
                card.getBlueprint().getCardCategory();
        when(card.getZone()).thenReturn(
                category == CardCategory.LOCATION
                        ? Zone.LOCATIONS : Zone.AT_LOCATION);
        when(card.getAtLocation()).thenReturn(location);
        when(fixture.modifiers.getLocationThatCardIsAt(
                fixture.gameState, card)).thenReturn(location);
        when(fixture.modifiers.getLocationThatCardIsPresentAt(
                fixture.gameState, card)).thenReturn(location);
        fixture.cardsAt.computeIfAbsent(
                location, ignored -> new ArrayList<>());
        if (!fixture.cardsAt.get(location).contains(card)) {
            fixture.cardsAt.get(location).add(card);
        }
    }

    private static void setBattlePower(
            Fixture fixture, float ours, float theirs) {
        setBattlePowerAt(
                fixture, fixture.battleSite,
                ours, theirs);
    }

    private static void setBattlePowerAt(
            Fixture fixture, PhysicalCard location,
            float ours, float theirs) {
        when(fixture.modifiers.getTotalPowerAtLocation(
                fixture.gameState, location,
                PLAYER, false, false)).thenReturn(ours);
        when(fixture.modifiers.getTotalPowerAtLocation(
                fixture.gameState, location,
                OPPONENT, false, false)).thenReturn(theirs);
        when(fixture.modifiers.getTotalAbilityAtLocation(
                fixture.gameState, PLAYER,
                location)).thenReturn(4.0f);
        when(fixture.modifiers.getTotalAbilityAtLocation(
                fixture.gameState, OPPONENT,
                location)).thenReturn(2.0f);
    }

    private static BattleDecisionPolicy.Context battleContext(
            Fixture fixture, int forcePile) {
        return battleContext(
                fixture, forcePile,
                fixture.battleSite);
    }

    private static BattleDecisionPolicy.Context battleContext(
            Fixture fixture, int forcePile,
            PhysicalCard... targets) {
        return new BattleDecisionPolicy.Context() {
            @Override
            public String getDecisionType() {
                return "CARD_ACTION_CHOICE";
            }
            @Override
            public Phase getPhase() {
                return Phase.BATTLE;
            }
            @Override
            public String getDecisionText() {
                return "Choose battle action";
            }
            @Override
            public List<String> getActionIds() {
                return java.util.Arrays.stream(targets)
                        .map(FirstOrderReignsEvaluatorBehaviorTest::id)
                        .toList();
            }
            @Override
            public List<String> getActionTexts() {
                return java.util.Arrays.stream(targets)
                        .map(target ->
                                "Initiate battle at "
                                        + target.getTitle())
                        .toList();
            }
            @Override
            public List<String> getCardIds() {
                return java.util.Arrays.stream(targets)
                        .map(FirstOrderReignsEvaluatorBehaviorTest::id)
                        .toList();
            }
            @Override
            public GameState getGameState() {
                return fixture.gameState;
            }
            @Override
            public SwccgGame getGame() {
                return fixture.game;
            }
            @Override
            public String getPlayerId() {
                return PLAYER;
            }
            @Override
            public int getReserveDeckSize() {
                return 20;
            }
            @Override
            public int getLifeForce() {
                return 20;
            }
            @Override
            public int getForcePileSize() {
                return forcePile;
            }
            @Override
            public int getHandSize() {
                return 5;
            }
            @Override
            public ObjectiveAnalyzer getObjectiveAnalyzer() {
                return fixture.analyzer;
            }
            @Override
            public float getVaderExpendabilityFactor() {
                return 0.3f;
            }
            @Override
            public int getCriticalLifeForce() {
                return 6;
            }
            @Override
            public BattleDecisionPolicy.Prediction predictBattle(
                    int myPower, int myDestinyDraws,
                    int opponentPower, int opponentDestinyDraws) {
                return new BattleDecisionPolicy.Prediction(
                        0.9f, 5.0f, 0.0f);
            }
            @Override
            public org.apache.logging.log4j.Logger getLogger() {
                return LogManager.getLogger(
                        FirstOrderReignsEvaluatorBehaviorTest.class);
            }
        };
    }

    private static List<Outcome> actionTextAdapter(
            Fixture fixture, Decision decision) {
        if (fixture.variant.bot == Bot.RANDO) {
            return new com.gempukku.swccgo.ai.models.rando.evaluators
                    .ActionTextEvaluator()
                    .evaluate(randoContext(fixture, decision))
                    .stream()
                    .map(FirstOrderReignsEvaluatorBehaviorTest::outcome)
                    .toList();
        }
        return new com.gempukku.swccgo.ai.models.chosenone.evaluators
                .ActionTextEvaluator()
                .evaluate(chosenContext(fixture, decision))
                .stream()
                .map(FirstOrderReignsEvaluatorBehaviorTest::outcome)
                .toList();
    }

    private static TracedActions tracedActionTextAdapter(
            Fixture fixture, Decision decision) {
        assertTrue(TraceSession.open(
                fixture.variant.bot.name(),
                "first-order-reigns-action-text",
                decision.type, decision.text,
                decision.actionIds, null,
                List.of("focused evaluator fixture"),
                false));
        List<Outcome> actions;
        DecisionTrace trace;
        try {
            actions = actionTextAdapter(
                    fixture, decision);
        } finally {
            trace = TraceSession.close();
        }
        assertNotNull(trace);
        return new TracedActions(actions, trace);
    }

    private static List<Outcome> deployAdapter(
            Fixture fixture, Decision decision) {
        if (fixture.variant.bot == Bot.RANDO) {
            return new com.gempukku.swccgo.ai.models.rando.evaluators
                    .DeployEvaluator()
                    .evaluate(randoContext(fixture, decision))
                    .stream()
                    .map(FirstOrderReignsEvaluatorBehaviorTest::outcome)
                    .toList();
        }
        return new com.gempukku.swccgo.ai.models.chosenone.evaluators
                .DeployEvaluator()
                .evaluate(chosenContext(fixture, decision))
                .stream()
                .map(FirstOrderReignsEvaluatorBehaviorTest::outcome)
                .toList();
    }

    private static List<Outcome> moveAdapter(
            Fixture fixture, Decision decision) {
        if (fixture.variant.bot == Bot.RANDO) {
            return new com.gempukku.swccgo.ai.models.rando.evaluators
                    .MoveEvaluator()
                    .evaluate(randoContext(fixture, decision))
                    .stream()
                    .map(FirstOrderReignsEvaluatorBehaviorTest::outcome)
                    .toList();
        }
        return new com.gempukku.swccgo.ai.models.chosenone.evaluators
                .MoveEvaluator()
                .evaluate(chosenContext(fixture, decision))
                .stream()
                .map(FirstOrderReignsEvaluatorBehaviorTest::outcome)
                .toList();
    }

    private static TracedActions tracedMoveAdapter(
            Fixture fixture, Decision decision) {
        assertTrue(TraceSession.open(
                fixture.variant.bot.name(),
                "first-order-reigns-move",
                decision.type, decision.text,
                decision.actionIds, null,
                List.of("focused evaluator fixture"),
                false));
        List<Outcome> actions;
        DecisionTrace trace;
        try {
            actions = moveAdapter(
                    fixture, decision);
        } finally {
            trace = TraceSession.close();
        }
        assertNotNull(trace);
        return new TracedActions(actions, trace);
    }

    private static List<Outcome> cardSelectionAdapter(
            Fixture fixture, Decision decision) {
        if (fixture.variant.bot == Bot.RANDO) {
            return new com.gempukku.swccgo.ai.models.rando.evaluators
                    .CardSelectionEvaluator()
                    .evaluate(randoContext(fixture, decision))
                    .stream()
                    .map(FirstOrderReignsEvaluatorBehaviorTest::outcome)
                    .toList();
        }
        return new com.gempukku.swccgo.ai.models.chosenone.evaluators
                .CardSelectionEvaluator()
                .evaluate(chosenContext(fixture, decision))
                .stream()
                .map(FirstOrderReignsEvaluatorBehaviorTest::outcome)
                .toList();
    }

    private static TracedActions tracedCardSelectionAdapter(
            Fixture fixture, Decision decision) {
        assertTrue(TraceSession.open(
                fixture.variant.bot.name(),
                "first-order-reigns-card-selection",
                decision.type, decision.text,
                decision.actionIds, null,
                List.of("focused evaluator fixture"),
                false));
        List<Outcome> actions;
        DecisionTrace trace;
        try {
            actions = cardSelectionAdapter(
                    fixture, decision);
        } finally {
            trace = TraceSession.close();
        }
        assertNotNull(trace);
        return new TracedActions(actions, trace);
    }

    private static List<Outcome> passAdapter(
            Fixture fixture, Decision decision) {
        if (fixture.variant.bot == Bot.RANDO) {
            return new com.gempukku.swccgo.ai.models.rando.evaluators
                    .PassEvaluator()
                    .evaluate(randoContext(fixture, decision))
                    .stream()
                    .map(FirstOrderReignsEvaluatorBehaviorTest::outcome)
                    .toList();
        }
        return new com.gempukku.swccgo.ai.models.chosenone.evaluators
                .PassEvaluator()
                .evaluate(chosenContext(fixture, decision))
                .stream()
                .map(FirstOrderReignsEvaluatorBehaviorTest::outcome)
                .toList();
    }

    private static Outcome combined(
            Fixture fixture, Decision decision) {
        if (fixture.variant.bot == Bot.RANDO) {
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

    private static com.gempukku.swccgo.ai.models.rando.evaluators
            .DecisionContext randoContext(
                    Fixture fixture, Decision decision) {
        var context =
                new com.gempukku.swccgo.ai.models.rando.evaluators
                    .DecisionContext(
                        fixture.gameState, PLAYER,
                        decision.type, decision.text,
                        "first-order-reigns-evaluator",
                        decision.phase);
        context.setGame(fixture.game);
        context.setSide(Side.DARK);
        context.setObjectiveAnalyzer(
                (com.gempukku.swccgo.ai.models.rando.strategy
                        .ObjectiveAnalyzer) fixture.analyzer);
        apply(context, decision);
        return context;
    }

    private static com.gempukku.swccgo.ai.models.chosenone.evaluators
            .DecisionContext chosenContext(
                    Fixture fixture, Decision decision) {
        var context =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators
                    .DecisionContext(
                        fixture.gameState, PLAYER,
                        decision.type, decision.text,
                        "first-order-reigns-evaluator",
                        decision.phase);
        context.setGame(fixture.game);
        context.setSide(Side.DARK);
        context.setObjectiveAnalyzer(
                (com.gempukku.swccgo.ai.models.chosenone.strategy
                        .ObjectiveAnalyzer) fixture.analyzer);
        apply(context, decision);
        return context;
    }

    private static void apply(
            com.gempukku.swccgo.ai.models.rando.evaluators
                    .DecisionContext context,
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
        if (decision.mover != null) {
            context.setExtra(
                    "movePhysicalCardId",
                    decision.mover.getCardId());
        }
    }

    private static void apply(
            com.gempukku.swccgo.ai.models.chosenone.evaluators
                    .DecisionContext context,
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
        if (decision.mover != null) {
            context.setExtra(
                    "movePhysicalCardId",
                    decision.mover.getCardId());
        }
    }

    private static Outcome outcome(
            com.gempukku.swccgo.ai.models.rando.evaluators
                    .EvaluatedAction action) {
        assertNotNull(action);
        return new Outcome(
                action.getActionId(), action.getScore(),
                List.copyOf(action.getReasoning()),
                action.isHardVetoed(),
                action.getVetoReason());
    }

    private static Outcome outcome(
            com.gempukku.swccgo.ai.models.chosenone.evaluators
                    .EvaluatedAction action) {
        assertNotNull(action);
        return new Outcome(
                action.getActionId(), action.getScore(),
                List.copyOf(action.getReasoning()),
                action.isHardVetoed(),
                action.getVetoReason());
    }

    private static Outcome only(
            List<Outcome> actions, String actionId) {
        return actions.stream()
                .filter(action ->
                        actionId.equals(action.actionId))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Missing " + actionId + " in " + actions));
    }

    private static BattleDecisionPolicy.ScoredAction onlyBattle(
            List<BattleDecisionPolicy.ScoredAction> actions) {
        assertEquals(1, actions.size());
        return actions.get(0);
    }

    private static BattleDecisionPolicy.ScoredAction battle(
            List<BattleDecisionPolicy.ScoredAction> actions,
            String actionId) {
        return actions.stream()
                .filter(action ->
                        actionId.equals(action.actionId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "Missing battle " + actionId
                                + " in " + actions));
    }

    private static float battleScore(
            BattleDecisionPolicy.ScoredAction action) {
        float score = action.baseScore();
        for (BattleDecisionPolicy.Contribution contribution
                : action.contributions()) {
            score += contribution.delta();
        }
        return score;
    }

    private static boolean hasBattleRule(
            BattleDecisionPolicy.ScoredAction action,
            String ruleId) {
        return action.contributions().stream()
                .anyMatch(contribution ->
                        contribution.ruleArmId() != null
                        && ruleId.equals(
                            contribution.ruleArmId().id()));
    }

    private static boolean hasHardVeto(
            BattleDecisionPolicy.ScoredAction action) {
        return action.contributions().stream()
                .anyMatch(BattleDecisionPolicy
                        .Contribution::hardVeto);
    }

    private static void assertContains(
            Outcome outcome, String marker) {
        assertTrue(
                "Expected '" + marker + "' in "
                        + outcome.reasoning,
                outcome.reasoning.stream()
                    .anyMatch(reason ->
                        reason.contains(marker)));
    }

    private static void assertNotContains(
            Outcome outcome, String marker) {
        assertFalse(
                "Did not expect '" + marker + "' in "
                        + outcome.reasoning,
                outcome.reasoning.stream()
                    .anyMatch(reason ->
                        reason.contains(marker)));
    }

    private static void assertHasTypedRule(
            DecisionTrace trace, String actionId,
            String ruleId) {
        assertTrue(
                "Expected typed rule '" + ruleId
                        + "' for " + actionId + " in "
                        + trace.getOperations(),
                trace.getOperations().stream()
                        .anyMatch(operation ->
                                actionId.equals(
                                        operation.getActionId())
                                && operation.getRuleId() != null
                                && ruleId.equals(
                                        operation.getRuleId()
                                                .id())));
    }

    private static void assertNoTypedRule(
            DecisionTrace trace, String actionId,
            String ruleId) {
        assertFalse(
                "Did not expect typed rule '" + ruleId
                        + "' for " + actionId + " in "
                        + trace.getOperations(),
                trace.getOperations().stream()
                        .anyMatch(operation ->
                                actionId.equals(
                                        operation.getActionId())
                                && operation.getRuleId() != null
                                && ruleId.equals(
                                        operation.getRuleId()
                                                .id())));
    }

    private static PhysicalCard objective(
            String blueprintId, boolean flipped) {
        PhysicalCard objective = mock(PhysicalCard.class);
        SwccgCardBlueprint front =
                mock(SwccgCardBlueprint.class);
        SwccgCardBlueprint back =
                mock(SwccgCardBlueprint.class);
        when(objective.getOwner()).thenReturn(PLAYER);
        when(objective.getZone())
                .thenReturn(Zone.SIDE_OF_TABLE);
        when(objective.getBlueprint())
                .thenReturn(flipped ? back : front);
        when(objective.getOtherSideBlueprint())
                .thenReturn(flipped ? front : back);
        when(objective.getBlueprintId(true))
                .thenReturn(blueprintId);
        when(objective.getBlueprintId(false))
                .thenReturn(blueprintId);
        when(objective.getPermanentCardId()).thenReturn(1);
        when(objective.getCardId()).thenReturn(1);
        when(objective.getAdditionalCardIds())
                .thenReturn(List.of());
        when(objective.isFlipped()).thenReturn(flipped);
        when(front.getTitle())
                .thenReturn("The First Order Reigns");
        when(front.getGameText()).thenReturn(
                "Deploy D'Qar and Crait systems, Salt Plateau, "
                        + "and Tracked Fleet. Once per turn, may deploy "
                        + "Supremacy card or battleground. Flip this card "
                        + "if Tracked Fleet is 'annihilated'.");
        when(front.getCardCategory())
                .thenReturn(CardCategory.OBJECTIVE);
        when(back.getTitle())
                .thenReturn("The Resistance Is Doomed");
        when(back.getGameText()).thenReturn(
                "If you just lost a battle at Salt Plateau where "
                        + "opponent's Han, Leia, or Luke was present, "
                        + "and your Kylo was just forfeited, place this "
                        + "objective out of play.");
        when(back.getCardCategory())
                .thenReturn(CardCategory.OBJECTIVE);
        return objective;
    }

    private static PhysicalCard system(
            Fixture fixture, String title,
            String blueprintId, int parsec, int id) {
        PhysicalCard system = card(
                title, blueprintId, PLAYER,
                Zone.LOCATIONS,
                CardCategory.LOCATION,
                CardSubtype.SYSTEM, id);
        when(system.getParsec()).thenReturn(parsec);
        register(fixture.cards, system);
        return system;
    }

    private static PhysicalCard craitCavern(
            Fixture fixture) {
        PhysicalCard cavern = card(
                "Crait: Outpost Entrance Cavern",
                "225_16", PLAYER, Zone.LOCATIONS,
                CardCategory.LOCATION,
                CardSubtype.SITE, CRAIT_CAVERN_ID);
        when(cavern.getPartOfSystem())
                .thenReturn("Crait");
        register(fixture.cards, cavern);
        return cavern;
    }

    private static PhysicalCard card(
            String title, String blueprintId,
            String owner, Zone zone,
            CardCategory category,
            CardSubtype subtype, int id) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint =
                mock(SwccgCardBlueprint.class);
        when(card.getTitle()).thenReturn(title);
        when(card.getTitles()).thenReturn(List.of(title));
        when(card.getOwner()).thenReturn(owner);
        when(card.getZone()).thenReturn(zone);
        when(card.getBlueprintId(true))
                .thenReturn(blueprintId);
        when(card.getBlueprintId(false))
                .thenReturn(blueprintId);
        when(card.getPermanentCardId()).thenReturn(id);
        when(card.getCardId()).thenReturn(id);
        when(card.getAdditionalCardIds())
                .thenReturn(List.of());
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.isBlownAway()).thenReturn(false);
        when(card.isUndercover()).thenReturn(false);
        when(card.isCaptive()).thenReturn(false);
        when(blueprint.getCardCategory())
                .thenReturn(category);
        if (subtype != null) {
            when(blueprint.getCardSubtype())
                    .thenReturn(subtype);
        }
        if (category == CardCategory.CHARACTER) {
            when(blueprint.hasAbilityAttribute())
                    .thenReturn(true);
            when(blueprint.getAbility()).thenReturn(
                    id == KYLO_ID ? 5.0f : 1.0f);
            when(blueprint.hasPowerAttribute())
                    .thenReturn(true);
            when(blueprint.getPower()).thenReturn(
                    id == KYLO_ID ? 6.0f : 2.0f);
            when(blueprint.hasForfeitAttribute())
                    .thenReturn(true);
            when(blueprint.getForfeit()).thenReturn(
                    id == KYLO_ID ? 7.0f : 2.0f);
        }
        return card;
    }

    private static void register(
            Map<Integer, PhysicalCard> cards,
            PhysicalCard card) {
        cards.put(card.getCardId(), card);
    }

    private static void setActive(
            GameState gameState,
            PhysicalCard card,
            boolean active) {
        when(gameState.isCardInPlayActive(
                card, false, false, false,
                false, false, false, false, false))
                .thenReturn(active);
        when(gameState.isCardInPlayActive(
                card, true, false, false,
                false, false, false, false, false))
                .thenReturn(active);
    }

    private static void setDeployable(
            ModifiersQuerying modifiers,
            boolean deployable) {
        when(modifiers.isDeployable(
                any(), any(), any(), anyBoolean(), any(),
                anyBoolean(), anyFloat(), any(), any(), any(),
                any(), any(), anyBoolean(), anyFloat()))
                .thenReturn(deployable);
        when(modifiers.isDeployableToTarget(
                any(), any(), any(), anyBoolean(),
                any(), anyBoolean(), anyFloat(),
                any(), any(), any(), any(), any(),
                any(), anyBoolean(), anyFloat()))
                .thenReturn(deployable);
    }

    private static void setCandidateDeployableAtForce(
            Fixture fixture, PhysicalCard candidate,
            int minimumForce) {
        when(fixture.modifiers.isDeployable(
                eq(fixture.gameState),
                any(PhysicalCard.class), eq(candidate),
                anyBoolean(), any(), anyBoolean(),
                anyFloat(), any(), any(), any(), any(),
                any(), anyBoolean(), anyFloat()))
                .thenAnswer(invocation ->
                        fixture.gameState
                                .getForcePileSize(PLAYER)
                                >= minimumForce);
    }

    private static void setCandidateDeployable(
            Fixture fixture, PhysicalCard candidate,
            boolean deployable) {
        when(fixture.modifiers.isDeployable(
                eq(fixture.gameState),
                any(PhysicalCard.class), eq(candidate),
                anyBoolean(), any(), anyBoolean(),
                anyFloat(), any(), any(), any(), any(),
                any(), anyBoolean(), anyFloat()))
                .thenReturn(deployable);
    }

    private static String id(PhysicalCard card) {
        return String.valueOf(card.getCardId());
    }

    private enum Bot {
        RANDO,
        CHOSEN_ONE
    }

    private record Variant(Bot bot, String blueprintId) {
    }

    private record Outcome(
            String actionId,
            float score,
            List<String> reasoning,
            boolean hardVeto,
            String vetoReason) {
    }

    private record TracedActions(
            List<Outcome> actions,
            DecisionTrace trace) {
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
            int min,
            PhysicalCard mover) {

        private static Decision objectiveDownload(
                String objectiveBlueprintId) {
            return new Decision(
                    "CARD_ACTION_CHOICE",
                    "Choose deploy action",
                    Phase.DEPLOY,
                    List.of("download", ""),
                    List.of(
                            "Deploy Supremacy card or battleground",
                            "Pass"),
                    List.of("1", ""),
                    List.of(objectiveBlueprintId, ""),
                    List.of("The First Order Reigns", ""),
                    false, 0, null);
        }

        private static Decision backLostPileDeploy() {
            return backLostPileDeploy(
                    "Deploy card from Lost Pile", "1");
        }

        private static Decision backLostPileDeploy(
                String actionText, String sourceCardId) {
            return new Decision(
                    "CARD_ACTION_CHOICE",
                    "Choose deploy action",
                    Phase.DEPLOY,
                    List.of("lost-pile-deploy"),
                    List.of(actionText),
                    List.of(sourceCardId),
                    List.of("inPlay"),
                    List.of("The Resistance Is Doomed"),
                    false, 0, null);
        }

        private static Decision lostPileCardSelection(
                PhysicalCard... cards) {
            List<String> actionIds =
                    new ArrayList<>();
            List<String> actionTexts =
                    new ArrayList<>();
            List<String> cardIds =
                    new ArrayList<>();
            List<String> blueprints =
                    new ArrayList<>();
            List<String> titles =
                    new ArrayList<>();
            for (int index = 0;
                    index < cards.length; index++) {
                PhysicalCard card = cards[index];
                actionIds.add("temp" + index);
                actionTexts.add(card.getTitle());
                cardIds.add(id(card));
                blueprints.add(
                        card.getBlueprintId(true));
                titles.add(card.getTitle());
            }
            return new Decision(
                    "ARBITRARY_CARDS",
                    "Choose card to deploy from Lost Pile",
                    Phase.DEPLOY,
                    actionIds, actionTexts,
                    cardIds, blueprints, titles,
                    true, 1, null);
        }

        private static Decision directDeploys(
                PhysicalCard... cards) {
            List<String> actionIds = new ArrayList<>();
            List<String> actionTexts = new ArrayList<>();
            List<String> cardIds = new ArrayList<>();
            List<String> blueprints = new ArrayList<>();
            List<String> titles = new ArrayList<>();
            for (PhysicalCard card : cards) {
                actionIds.add("deploy-" + card.getCardId());
                actionTexts.add(
                        "Deploy <div class='cardHint' value='"
                                + card.getBlueprintId(true)
                                + "'>" + card.getTitle()
                                + "</div>");
                cardIds.add(id(card));
                blueprints.add(card.getBlueprintId(true));
                titles.add(card.getTitle());
            }
            actionIds.add("");
            actionTexts.add("Pass");
            cardIds.add("");
            blueprints.add("");
            titles.add("Pass");
            return new Decision(
                    "CARD_ACTION_CHOICE",
                    "Choose deploy action",
                    Phase.DEPLOY,
                    actionIds, actionTexts, cardIds,
                    blueprints, titles,
                    false, 0, null);
        }

        private static Decision topLevelHyperspace() {
            return new Decision(
                    "ACTION_CHOICE",
                    "Choose move action",
                    Phase.MOVE,
                    List.of("move-supremacy", "pass"),
                    List.of(
                            "Move using hyperspeed",
                            "Pass"),
                    List.of(
                            String.valueOf(SUPREMACY_ID),
                            ""),
                    List.of(), List.of(),
                    false, 0, null);
        }

        private static Decision topLevelLandspeed(
                PhysicalCard mover) {
            return new Decision(
                    "ACTION_CHOICE",
                    "Choose move action",
                    Phase.MOVE,
                    List.of(
                            "move-" + mover.getCardId(),
                            "pass"),
                    List.of(
                            "Move using landspeed",
                            "Pass"),
                    List.of(id(mover), ""),
                    List.of(), List.of(),
                    false, 0, null);
        }

        private static Decision deployDestinations(
                PhysicalCard deployingCard,
                PhysicalCard... destinations) {
            return selection(
                    "Choose where to deploy "
                            + "<div class='cardHint' value='"
                            + deployingCard.getBlueprintId(true)
                            + "'>" + deployingCard.getTitle()
                            + "</div>",
                    Phase.DEPLOY, null, destinations);
        }

        private static Decision moveDestinations(
                PhysicalCard mover,
                PhysicalCard... destinations) {
            return moveDestinations(
                    mover, "hyperspeed",
                    destinations);
        }

        private static Decision
                moveDestinationsUsingLandspeed(
                    PhysicalCard mover,
                    PhysicalCard... destinations) {
            return moveDestinations(
                    mover, "landspeed",
                    destinations);
        }

        private static Decision moveDestinations(
                PhysicalCard mover,
                String movementKind,
                PhysicalCard... destinations) {
            List<String> cardIds = new ArrayList<>();
            List<String> blueprints = new ArrayList<>();
            List<String> titles = new ArrayList<>();
            for (PhysicalCard destination : destinations) {
                cardIds.add(id(destination));
                blueprints.add(
                        destination.getBlueprintId(true));
                titles.add(destination.getTitle());
            }
            return new Decision(
                    "CARD_SELECTION",
                    "Choose where to move "
                            + "<div class='cardHint' value='"
                            + mover.getBlueprintId(true)
                            + "'>" + mover.getTitle()
                            + "</div> using "
                            + movementKind,
                    Phase.MOVE,
                    List.of(), List.of(),
                    cardIds, blueprints, titles,
                    true, 1, mover);
        }

        private static Decision forceLoss(
                PhysicalCard... candidates) {
            return selection(
                    "Choose Force to lose",
                    Phase.CONTROL, null, candidates);
        }

        private static Decision deployKylo(
                PhysicalCard... destinations) {
            Decision selection = selection(
                    "Choose where to deploy "
                            + "<div class='cardHint' value='204_43'>"
                            + "Kylo Ren</div>",
                    Phase.DEPLOY, null, destinations);
            return selection;
        }

        private static Decision forfeit(
                PhysicalCard... candidates) {
            return selection(
                    "Choose a card from battle to forfeit",
                    Phase.BATTLE, null, candidates);
        }

        private static Decision selection(
                String text, Phase phase,
                PhysicalCard mover,
                PhysicalCard... candidates) {
            List<String> ids = new ArrayList<>();
            List<String> blueprints = new ArrayList<>();
            List<String> titles = new ArrayList<>();
            for (PhysicalCard candidate : candidates) {
                ids.add(id(candidate));
                blueprints.add(
                        candidate.getBlueprintId(true));
                titles.add(candidate.getTitle());
            }
            return new Decision(
                    "CARD_SELECTION", text, phase,
                    List.of(), List.of(),
                    ids, blueprints, titles,
                    true, 1, mover);
        }
    }

    private record Fixture(
            Variant variant,
            SwccgGame game,
            GameState gameState,
            ModifiersQuerying modifiers,
            Map<Integer, PhysicalCard> cards,
            List<PhysicalCard> permanents,
            List<PhysicalCard> locations,
            List<PhysicalCard> hand,
            List<PhysicalCard> reserve,
            Map<PhysicalCard, List<PhysicalCard>> cardsAt,
            ObjectiveAnalyzer analyzer,
            PhysicalCard host,
            PhysicalCard relocatedHost,
            PhysicalCard stage,
            PhysicalCard origin,
            PhysicalCard closer,
            PhysicalCard sameDistance,
            PhysicalCard farther,
            PhysicalCard illegal,
            PhysicalCard salt,
            PhysicalCard otherSalt,
            PhysicalCard threatenedHost,
            PhysicalCard safeHost,
            PhysicalCard battleSite,
            PhysicalCard trackedFleet,
            PhysicalCard supremacy,
            PhysicalCard kylo,
            PhysicalCard han,
            PhysicalCard alternative) {
    }
}
