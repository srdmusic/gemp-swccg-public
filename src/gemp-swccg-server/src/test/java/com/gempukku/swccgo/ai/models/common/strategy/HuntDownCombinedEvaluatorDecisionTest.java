package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.ai.models.common.trace.DecisionTrace;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceSession;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.PhysicalCardVisitor;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgCardBlueprintLibrary;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.game.state.actions.GameTextActionState;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.actions.GameTextAction;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Public production-decision proof for Hunt Down. Each fixture uses the real
 * objective profile and both bots' no-argument CombinedEvaluator.
 */
public class HuntDownCombinedEvaluatorDecisionTest {
    private static final String PLAYER = "dark";
    private static final String OPPONENT = "light";

    private static final int OBJECTIVE_ID = 90;
    private static final int BATTLEGROUND_ID = 101;
    private static final int NON_BATTLEGROUND_ID = 102;
    private static final int BLOCKER_BATTLEGROUND_ID = 103;
    private static final int CASTLE_ID = 104;
    private static final int BATTLEGROUND_DOCKING_BAY_ID = 105;
    private static final int NON_BATTLEGROUND_DOCKING_BAY_ID = 106;
    private static final int DECOY_DOCKING_BAY_ID = 107;
    private static final int VADER_ID = 201;
    private static final int SECOND_VADER_ID = 202;
    private static final int DISTRACTOR_ID = 203;
    private static final int COSTLY_DEPLOY_ID = 601;
    private static final int CHEAP_DEPLOY_ID = 602;

    private static final String HUNT_DOWN_BP = "213_31";
    private static final String CLASSIC_HUNT_DOWN_BP = "7_297";
    private static final String VADER_BP = "1_168";
    private static final String SECOND_VADER_BP = "7_175";
    private static final String DISTRACTOR_BP = "1_169";
    private static final String DISTRACTOR_EFFECT_BP = "213_16";
    private static final String VADERS_BROKER_BP = "217_13";
    private static final String BATTLEGROUND_BP = "1_290";
    private static final String NON_BATTLEGROUND_BP = "4_163";
    private static final String BLOCKER_BATTLEGROUND_BP = "5_166";
    private static final String CASTLE_BP = "209_50";
    private static final String BATTLEGROUND_DOCKING_BAY_BP = "1_291";
    private static final String NON_BATTLEGROUND_DOCKING_BAY_BP =
            "7_276";
    private static final String DECOY_DOCKING_BAY_BP = "9_145";

    private static final SwccgCardBlueprintLibrary CARDS =
            new SwccgCardBlueprintLibrary();

    @Test
    public void virtualLocationDownloadActionBeatsPass() {
        List<Outcome> results = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot);
            fixture.addReserve(site(
                    BLOCKER_BATTLEGROUND_BP, 301, Zone.RESERVE_DECK));

            Outcome result = combined(
                    bot, fixture, Decision.locationDownload());

            assertEquals("download-site", result.actionId());
            assertContains(result, "Use the objective action to deploy an eligible battleground site");
            results.add(result);
        }
        assertParity(results);
    }

    @Test
    public void requiredVaderPullBeatsEqualCharacterDistractor() {
        List<Outcome> results = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot);
            PhysicalCard distractor = character(
                    DISTRACTOR_BP, DISTRACTOR_ID, Zone.RESERVE_DECK,
                    PLAYER);
            PhysicalCard vader = character(
                    VADER_BP, VADER_ID, Zone.RESERVE_DECK, PLAYER);
            fixture.addReserve(distractor);
            fixture.addReserve(vader);
            fixture.markPersona(vader, Persona.VADER);

            Outcome result = combined(
                    bot, fixture, Decision.pullVader());

            assertEquals("1", result.actionId());
            assertContains(result, "typed actor required");
            results.add(result);
        }
        assertParity(results);
    }

    @Test
    public void remoteBlockerDoesNotMakeRequiredVaderDisposableToForceLoss() {
        List<Outcome> results = new ArrayList<>();
        List<Outcome> awareCandidates = new ArrayList<>();
        List<Outcome> baselineCandidates = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot);
            PhysicalCard vader = character(
                    VADER_BP, VADER_ID, Zone.HAND, PLAYER);
            PhysicalCard disposable = effect(
                    DISTRACTOR_EFFECT_BP,
                    DISTRACTOR_ID, Zone.HAND, PLAYER);
            PhysicalCard remoteJedi = character(
                    "1_021", 405,
                    Zone.AT_LOCATION, OPPONENT);
            fixture.addHand(vader);
            fixture.addHand(disposable);
            fixture.markPersona(vader, Persona.VADER);
            fixture.place(
                    remoteJedi,
                    fixture.blockerBattleground);
            when(fixture.modifiers.getAbility(
                    fixture.gameState, remoteJedi))
                    .thenReturn(6.0f);

            Outcome result = combined(
                    bot, fixture, Decision.forceLoss());
            assertEquals(
                    String.valueOf(DISTRACTOR_ID),
                    result.actionId());

            Outcome aware = cardSelectionAdapter(
                    bot, fixture, Decision.forceLoss(),
                    true, String.valueOf(VADER_ID));
            Outcome baseline = cardSelectionAdapter(
                    bot, fixture, Decision.forceLoss(),
                    false, String.valueOf(VADER_ID));
            assertContains(
                    aware,
                    "OBJECTIVE CRITICAL IN HAND");
            assertNotContains(
                    baseline,
                    "OBJECTIVE CRITICAL IN HAND");
            assertTrue(aware.score() < baseline.score());
            results.add(result);
            awareCandidates.add(aware);
            baselineCandidates.add(baseline);
        }
        assertParity(results);
        assertParity(awareCandidates);
        assertParity(baselineCandidates);
    }

    @Test
    public void mandatoryForfeitPreservesSoleBattlegroundVaderWhenBuddyCanGo() {
        List<Outcome> results = new ArrayList<>();
        List<Outcome> awareCandidates = new ArrayList<>();
        List<Outcome> baselineCandidates = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot);
            PhysicalCard vader = character(
                    VADER_BP, VADER_ID,
                    Zone.AT_LOCATION, PLAYER);
            PhysicalCard buddy = character(
                    DISTRACTOR_BP, DISTRACTOR_ID,
                    Zone.AT_LOCATION, PLAYER);
            fixture.place(vader, fixture.battleground);
            fixture.place(buddy, fixture.battleground);
            fixture.markPersona(vader, Persona.VADER);

            Outcome result = combined(
                    bot, fixture, Decision.forfeit());
            assertEquals(
                    String.valueOf(DISTRACTOR_ID),
                    result.actionId());

            Outcome aware = cardSelectionAdapter(
                    bot, fixture, Decision.forfeit(),
                    true, String.valueOf(VADER_ID));
            Outcome baseline = cardSelectionAdapter(
                    bot, fixture, Decision.forfeit(),
                    false, String.valueOf(VADER_ID));
            assertContains(
                    aware,
                    "BATTLE.OBJECTIVE.FLIP_GATE_FORMATION_HOLD");
            assertNotContains(
                    baseline,
                    "BATTLE.OBJECTIVE.FLIP_GATE_FORMATION_HOLD");
            assertTrue(aware.score() < baseline.score());
            results.add(result);
            awareCandidates.add(aware);
            baselineCandidates.add(baseline);
        }
        assertParity(results);
        assertParity(awareCandidates);
        assertParity(baselineCandidates);
    }

    @Test
    public void mandatoryForfeitPreservesSoleAboardBattlegroundVader() {
        List<Outcome> results = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot);
            PhysicalCard vader = character(
                    VADER_BP, VADER_ID,
                    Zone.AT_LOCATION, PLAYER);
            PhysicalCard buddy = character(
                    DISTRACTOR_BP, DISTRACTOR_ID,
                    Zone.AT_LOCATION, PLAYER);
            fixture.place(vader, fixture.battleground);
            fixture.place(buddy, fixture.battleground);
            fixture.markPersona(vader, Persona.VADER);
            when(fixture.modifiers
                    .getLocationThatCardIsPresentAt(
                        fixture.gameState, vader))
                    .thenReturn(null);
            when(fixture.modifiers
                    .getLocationThatCardIsAt(
                        fixture.gameState, vader))
                    .thenReturn(fixture.battleground);

            Outcome result = combined(
                    bot, fixture, Decision.forfeit());
            assertEquals(
                    String.valueOf(DISTRACTOR_ID),
                    result.actionId());
            Outcome vaderCandidate =
                    cardSelectionAdapter(
                        bot, fixture, Decision.forfeit(),
                        true, String.valueOf(VADER_ID));
            assertContains(
                    vaderCandidate,
                    "BATTLE.OBJECTIVE.FLIP_GATE_FORMATION_HOLD");
            results.add(result);
        }
        assertParity(results);
    }

    @Test
    public void legalVaderDeployActionBeatsPass() {
        List<Outcome> results = new ArrayList<>();
        List<Outcome> awareCandidates = new ArrayList<>();
        List<Outcome> baselineCandidates = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot);
            PhysicalCard vader = character(
                    VADER_BP, VADER_ID, Zone.HAND, PLAYER);
            fixture.addHand(vader);
            fixture.markPersona(vader, Persona.VADER);

            Outcome result = combined(
                    bot, fixture, Decision.topLevelDeploy());

            assertEquals("deploy-vader", result.actionId());
            assertContains(result, "OBJECTIVE ACTOR LOCATION");
            Outcome aware = deployAdapter(
                    bot, fixture, Decision.topLevelDeploy(),
                    true, "deploy-vader");
            Outcome baseline = deployAdapter(
                    bot, fixture, Decision.topLevelDeploy(),
                    false, "deploy-vader");
            assertContains(aware, "OBJECTIVE ACTOR LOCATION");
            assertNotContains(
                    baseline, "OBJECTIVE ACTOR LOCATION");
            assertTrue(aware.score() > baseline.score());
            results.add(result);
            awareCandidates.add(aware);
            baselineCandidates.add(baseline);
        }
        assertParity(results);
        assertParity(awareCandidates);
        assertParity(baselineCandidates);
    }

    @Test
    public void castleReserveChildChoosesTheVaderThatPreservesTheMove() {
        List<Outcome> results = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot);
            PhysicalCard strandedVader = character(
                    VADER_BP, VADER_ID,
                    Zone.RESERVE_DECK, PLAYER);
            PhysicalCard routeVader = character(
                    SECOND_VADER_BP, SECOND_VADER_ID,
                    Zone.RESERVE_DECK, PLAYER);
            fixture.addReserve(strandedVader);
            fixture.addReserve(routeVader);
            fixture.markPersona(
                    strandedVader, Persona.VADER);
            fixture.markPersona(
                    routeVader, Persona.VADER);
            when(fixture.modifiers.mayNotMove(
                    fixture.gameState, strandedVader))
                    .thenReturn(true);
            fixture.prepareCastleLocationText(
                    routeVader, 1.0f);
            fixture.setLiveAction(
                    fixture.castle,
                    "Deploy Vader from Reserve Deck");

            Decision decision =
                    Decision.castleVaderReserveChoice();
            Outcome result = combined(
                    bot, fixture, decision);
            assertEquals("1", result.actionId());
            assertContains(
                    result,
                    "DEPLOY.OBJECTIVE.VADERS_CASTLE_CANDIDATE");
            Outcome stranded = cardSelectionAdapter(
                    bot, fixture, decision, true, "0");
            Outcome safe = cardSelectionAdapter(
                    bot, fixture, decision, true, "1");
            assertFalse(stranded.hardVeto());
            assertContains(
                    stranded,
                    "VADERS_CASTLE_CANDIDATE_HOLD");
            assertTrue(safe.score() > stranded.score());
            results.add(result);
        }
        assertParity(results);
    }

    @Test
    public void v48MoveReserveUsesPersonaAndActiveHuntScope() {
        List<Outcome> realVaderResults = new ArrayList<>();
        List<Outcome> unawareResults = new ArrayList<>();
        List<Outcome> titleImpostorResults = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture realVader = fixture(bot);
            when(realVader.gameState.getForcePileSize(PLAYER))
                    .thenReturn(6);
            PhysicalCard deployedVader = character(
                    VADER_BP, 501, Zone.AT_LOCATION, PLAYER);
            PhysicalCard secondDeployedVader = character(
                    VADER_BP, 504, Zone.AT_LOCATION, PLAYER);
            PhysicalCard handVader = character(
                    VADER_BP, VADER_ID, Zone.HAND, PLAYER);
            realVader.place(
                    deployedVader,
                    realVader.castle);
            realVader.place(
                    secondDeployedVader,
                    realVader.castle);
            realVader.addHand(handVader);
            realVader.markPersona(
                    deployedVader, Persona.VADER);
            realVader.markPersona(
                    secondDeployedVader, Persona.VADER);
            realVader.markPersona(handVader, Persona.VADER);
            realVader.prepareCastleLocationText(deployedVader, 1.0f);
            realVader.prepareCastleLocationText(
                    secondDeployedVader, 1.0f);
            Outcome personaCandidate = deployAdapter(
                    bot, realVader, Decision.topLevelDeploy(),
                    true, "deploy-vader");
            assertContains(
                    personaCandidate,
                    "V48 VADER MOVE RESERVE");
            realVaderResults.add(personaCandidate);
            Outcome objectiveUnawareCandidate = deployAdapter(
                    bot, realVader, Decision.topLevelDeploy(),
                    false, "deploy-vader");
            assertNotContains(
                    objectiveUnawareCandidate,
                    "V48 VADER MOVE RESERVE");
            unawareResults.add(objectiveUnawareCandidate);

            Fixture impostor = fixture(bot);
            when(impostor.gameState.getForcePileSize(PLAYER))
                    .thenReturn(6);
            PhysicalCard broker = mockCharacter(
                    "Vader's Broker", 502,
                    Zone.AT_LOCATION, PLAYER);
            PhysicalCard candidate = character(
                    VADER_BP, VADER_ID, Zone.HAND, PLAYER);
            impostor.place(
                    broker, impostor.castle);
            impostor.addHand(candidate);
            impostor.markPersona(candidate, Persona.VADER);
            Outcome impostorCandidate = deployAdapter(
                    bot, impostor, Decision.topLevelDeploy(),
                    true, "deploy-vader");
            assertNotContains(
                    impostorCandidate,
                    "V48 VADER MOVE RESERVE");
            titleImpostorResults.add(impostorCandidate);

            Fixture unrelated = fixture(bot);
            when(unrelated.gameState.getForcePileSize(PLAYER))
                    .thenReturn(6);
            PhysicalCard offRouteVader = character(
                    VADER_BP, 503, Zone.AT_LOCATION, PLAYER);
            PhysicalCard offRouteCandidate = character(
                    VADER_BP, VADER_ID, Zone.HAND, PLAYER);
            unrelated.place(
                    offRouteVader, unrelated.nonBattleground);
            unrelated.addHand(offRouteCandidate);
            unrelated.markPersona(
                    offRouteVader, Persona.VADER);
            unrelated.markPersona(
                    offRouteCandidate, Persona.VADER);
            Outcome unrelatedCandidate = deployAdapter(
                    bot, unrelated, Decision.topLevelDeploy(),
                    true, "deploy-vader");
            assertNotContains(
                    unrelatedCandidate,
                    "V48 VADER MOVE RESERVE");
        }
        assertParity(realVaderResults);
        assertParity(unawareResults);
        assertParity(titleImpostorResults);
    }

    @Test
    public void v48MoveReserveChangesTheProductionDeployWinner() {
        List<Outcome> baselineResults = new ArrayList<>();
        List<Outcome> objectiveResults = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot);
            when(fixture.gameState.getForcePileSize(PLAYER))
                    .thenReturn(8);
            PhysicalCard vader = character(
                    VADER_BP, 603, Zone.AT_LOCATION, PLAYER);
            fixture.place(vader, fixture.castle);
            fixture.markPersona(vader, Persona.VADER);
            fixture.prepareCastleLocationText(vader, 2.25f);

            PhysicalCard costly = deployTestCharacter(
                    COSTLY_DEPLOY_ID, 6.0f, 5.0f);
            PhysicalCard cheap = deployTestCharacter(
                    CHEAP_DEPLOY_ID, 5.0f, 0.0f);
            fixture.addHand(costly);
            fixture.addHand(cheap);

            Decision decision = Decision.deployBudgetChoice();
            Outcome baseline = combined(
                    bot, fixture, decision, false);
            Outcome objectiveAware = combined(
                    bot, fixture, decision, true);

            assertEquals("deploy-costly", baseline.actionId());
            assertEquals("deploy-cheap", objectiveAware.actionId());
            assertEquals(15.0f,
                    deployAdapter(
                            bot, fixture, decision, false,
                            "deploy-costly").score()
                    - deployAdapter(
                            bot, fixture, decision, false,
                            "deploy-cheap").score(),
                    0.0f);
            Outcome held = deployAdapter(
                    bot, fixture, decision, true,
                    "deploy-costly");
            assertContains(held, "V48 VADER MOVE RESERVE");
            baselineResults.add(baseline);
            objectiveResults.add(objectiveAware);
        }
        assertParity(baselineResults);
        assertParity(objectiveResults);
    }

    @Test
    public void duplicateVaderBlueprintStillChoosesBattlegroundDestination() {
        List<Outcome> results = new ArrayList<>();
        List<Outcome> awareCandidates = new ArrayList<>();
        List<Outcome> baselineCandidates = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot);
            PhysicalCard firstVader = character(
                    VADER_BP, VADER_ID, Zone.HAND, PLAYER);
            PhysicalCard secondVader = character(
                    VADER_BP, SECOND_VADER_ID, Zone.HAND, PLAYER);
            fixture.addHand(firstVader);
            fixture.addHand(secondVader);
            fixture.markPersona(firstVader, Persona.VADER);
            fixture.markPersona(secondVader, Persona.VADER);

            Outcome result = combined(
                    bot, fixture, Decision.deployDestination());

            assertEquals(
                    String.valueOf(BATTLEGROUND_ID),
                    result.actionId());
            assertContains(
                    result,
                    "LIVE BATTLEGROUND = ADVANCES FLIP!");
            Outcome aware = cardSelectionAdapter(
                    bot, fixture, Decision.deployDestination(),
                    true, String.valueOf(BATTLEGROUND_ID));
            Outcome baseline = cardSelectionAdapter(
                    bot, fixture, Decision.deployDestination(),
                    false, String.valueOf(BATTLEGROUND_ID));
            assertContains(
                    aware,
                    "LIVE BATTLEGROUND = ADVANCES FLIP!");
            assertNotContains(
                    baseline,
                    "LIVE BATTLEGROUND = ADVANCES FLIP!");
            assertTrue(aware.score() > baseline.score());
            results.add(result);
            awareCandidates.add(aware);
            baselineCandidates.add(baseline);
        }
        assertParity(results);
        assertParity(awareCandidates);
        assertParity(baselineCandidates);
    }

    @Test
    public void safeVaderMoveActionBeatsPass() {
        List<Outcome> results = new ArrayList<>();
        List<Outcome> awareCandidates = new ArrayList<>();
        List<Outcome> baselineCandidates = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot);
            PhysicalCard vader = character(
                    VADER_BP, VADER_ID, Zone.AT_LOCATION, PLAYER);
            fixture.place(vader, fixture.nonBattleground);
            fixture.markPersona(vader, Persona.VADER);
            fixture.prepareLandspeed(vader);

            Outcome result = combined(
                    bot, fixture, Decision.topLevelMove());

            assertEquals("move-vader", result.actionId());
            assertContains(
                    result,
                    "MOVE.OBJECTIVE.ACTOR_LOCATION_START");
            Outcome aware = moveAdapter(
                    bot, fixture, Decision.topLevelMove(),
                    true, "move-vader");
            Outcome baseline = moveAdapter(
                    bot, fixture, Decision.topLevelMove(),
                    false, "move-vader");
            assertContains(
                    aware,
                    "MOVE.OBJECTIVE.ACTOR_LOCATION_START");
            assertNotContains(
                    baseline,
                    "MOVE.OBJECTIVE.ACTOR_LOCATION_START");
            assertTrue(aware.score() > baseline.score());
            results.add(result);
            awareCandidates.add(aware);
            baselineCandidates.add(baseline);
        }
        assertParity(results);
        assertParity(awareCandidates);
        assertParity(baselineCandidates);
    }

    @Test
    public void vaderMoveDestinationPrefersBattleground() {
        List<Outcome> results = new ArrayList<>();
        List<Outcome> awareCandidates = new ArrayList<>();
        List<Outcome> baselineCandidates = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot);
            PhysicalCard vader = character(
                    VADER_BP, VADER_ID, Zone.AT_LOCATION, PLAYER);
            fixture.place(vader, fixture.nonBattleground);
            fixture.markPersona(vader, Persona.VADER);
            fixture.prepareLandspeed(vader);

            Outcome result = combined(
                    bot, fixture, Decision.moveDestination());

            assertEquals(
                    String.valueOf(BATTLEGROUND_ID),
                    result.actionId());
            assertContains(
                    result,
                    "MOVE.OBJECTIVE.ACTOR_LOCATION_DESTINATION");
            Outcome aware = cardSelectionAdapter(
                    bot, fixture, Decision.moveDestination(),
                    true, String.valueOf(BATTLEGROUND_ID));
            Outcome baseline = cardSelectionAdapter(
                    bot, fixture, Decision.moveDestination(),
                    false, String.valueOf(BATTLEGROUND_ID));
            assertContains(
                    aware,
                    "MOVE.OBJECTIVE.ACTOR_LOCATION_DESTINATION");
            assertNotContains(
                    baseline,
                    "MOVE.OBJECTIVE.ACTOR_LOCATION_DESTINATION");
            assertTrue(aware.score() > baseline.score());
            results.add(result);
            awareCandidates.add(aware);
            baselineCandidates.add(baseline);
        }
        assertParity(results);
        assertParity(awareCandidates);
        assertParity(baselineCandidates);
    }

    @Test
    public void soleBattlegroundVaderCanChaseBlockerWithoutLeavingTheActorLeg() {
        List<Outcome> moveResults = new ArrayList<>();
        List<Outcome> destinationResults = new ArrayList<>();
        List<Outcome> heldDestinations = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot);
            PhysicalCard vader = character(
                    VADER_BP, VADER_ID,
                    Zone.AT_LOCATION, PLAYER);
            PhysicalCard remoteJedi = character(
                    "1_021", 451,
                    Zone.AT_LOCATION, OPPONENT);
            fixture.place(vader, fixture.battleground);
            fixture.place(
                    remoteJedi,
                    fixture.blockerBattleground);
            fixture.markPersona(vader, Persona.VADER);
            when(fixture.modifiers.getAbility(
                    fixture.gameState, remoteJedi))
                    .thenReturn(6.0f);
            fixture.prepareLandspeed(
                    vader, fixture.battleground);

            Outcome move = combined(
                    bot, fixture, Decision.topLevelMove());
            assertEquals("move-vader", move.actionId());
            assertContains(
                move,
                "MOVE.OBJECTIVE.BLOCKER_CHASE_START");

            Decision destinationDecision =
                    Decision.moveDestinationFromBattleground();
            Outcome destination = combined(
                    bot, fixture, destinationDecision);
            assertEquals(
                    String.valueOf(
                            BLOCKER_BATTLEGROUND_ID),
                    destination.actionId());
            assertContains(
                destination,
                "MOVE.OBJECTIVE.BLOCKER_CHASE_DESTINATION");

            Outcome nonBattleground = cardSelectionAdapter(
                    bot, fixture, destinationDecision,
                    true,
                    String.valueOf(NON_BATTLEGROUND_ID));
            assertFalse(nonBattleground.hardVeto());
            assertContains(
                    nonBattleground,
                    "RUNTIME_ACTOR_DESTINATION_HOLD");
            moveResults.add(move);
            destinationResults.add(destination);
            heldDestinations.add(nonBattleground);
        }
        assertParity(moveResults);
        assertParity(destinationResults);
        assertParity(heldDestinations);
    }

    @Test
    public void dockingTransitParentBoundsSoleVaderObjectiveHold() {
        List<Outcome> winners = new ArrayList<>();
        List<Outcome> heldActions = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot);
            PhysicalCard source = fixture.addDockingBay(
                    BATTLEGROUND_DOCKING_BAY_BP,
                    BATTLEGROUND_DOCKING_BAY_ID, true);
            PhysicalCard destination = fixture.addDockingBay(
                    NON_BATTLEGROUND_DOCKING_BAY_BP,
                    NON_BATTLEGROUND_DOCKING_BAY_ID, false);
            PhysicalCard vader = character(
                    VADER_BP, VADER_ID,
                    Zone.AT_LOCATION, PLAYER);
            fixture.place(vader, source);
            fixture.markPersona(vader, Persona.VADER);
            fixture.enableActiveIteration();
            assertTrue(
                    com.gempukku.swccgo.filters.Filters
                        .docking_bay.accepts(
                            fixture.gameState,
                            fixture.modifiers, source));
            assertTrue(
                    com.gempukku.swccgo.filters.Filters
                        .atLocation(source).accepts(
                            fixture.gameState,
                            fixture.modifiers, vader));
            assertTrue(
                    com.gempukku.swccgo.filters.Filters
                        .canMoveToUsingDockingBayTransit(
                            vader, false, 0.0f)
                        .accepts(
                            fixture.gameState,
                            fixture.modifiers,
                            destination));
            assertTrue(
                    com.gempukku.swccgo.filters.Filters
                        .filterActive(
                            fixture.game, null,
                            com.gempukku.swccgo.common
                                .SpotOverride
                                .INCLUDE_UNDERCOVER,
                            com.gempukku.swccgo.filters.Filters
                                .and(
                                    com.gempukku.swccgo.filters
                                        .Filters.your(PLAYER),
                                    com.gempukku.swccgo.filters
                                        .Filters.atLocation(
                                            source)))
                        .contains(vader));
            var routes = fixture.analyzer
                    .assessDockingBayTransitRoutes(
                        fixture.game, PLAYER, source);
            assertFalse("Expected an engine-legal transit route",
                    routes.isEmpty());
            assertTrue(
                    "Expected every route to hold sole Vader: "
                        + routes,
                    routes.stream()
                        .noneMatch(route ->
                            route.admissible()));
            assertTrue(
                    "The legal route must be held only by objective intent: "
                        + routes,
                    routes.stream().allMatch(route ->
                        route.safetyVeto() == null
                            && route.objectiveHold()));

            Decision decision =
                    Decision.dockingTransitTop(source);
            Outcome winner = combined(
                    bot, fixture, decision);
            assertEquals("", winner.actionId());
            TracedOutcome held = tracedMoveAdapter(
                    bot, fixture, decision,
                    true, "0");
            assertFalse(held.outcome().hardVeto());
            assertEquals(-310.0f, held.outcome().score(), 0.0f);
            assertContains(
                    held.outcome(),
                    "RUNTIME_ACTOR_TRANSIT_HOLD");
            assertObjectiveDelta(
                    held,
                    "MOVE.OBJECTIVE.RUNTIME_ACTOR_TRANSIT_HOLD",
                    -300.0f);
            winners.add(winner);
            heldActions.add(held.outcome());
        }
        assertParity(winners);
        assertParity(heldActions);
    }

    @Test
    public void dockingTransitParentKeepsFormationSafetyCategorical() {
        List<Outcome> heldActions = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot);
            PhysicalCard source = fixture.addDockingBay(
                    BATTLEGROUND_DOCKING_BAY_BP,
                    BATTLEGROUND_DOCKING_BAY_ID, true);
            PhysicalCard destination = fixture.addDockingBay(
                    NON_BATTLEGROUND_DOCKING_BAY_BP,
                    NON_BATTLEGROUND_DOCKING_BAY_ID, false);
            PhysicalCard vader = character(
                    VADER_BP, VADER_ID,
                    Zone.AT_LOCATION, PLAYER);
            fixture.place(vader, source);
            fixture.markPersona(vader, Persona.VADER);
            when(fixture.modifiers.getTotalPowerAtLocation(
                    fixture.gameState, destination,
                    OPPONENT, false, false)).thenReturn(20.0f);
            fixture.enableActiveIteration();

            var routes = fixture.analyzer
                    .assessDockingBayTransitRoutes(
                        fixture.game, PLAYER, source);
            assertFalse(routes.isEmpty());
            assertTrue(
                    "Every offered transit must fail FormationSafety: "
                        + routes,
                    routes.stream().allMatch(route ->
                        route.safetyVeto() != null
                            && route.safetyVeto().contains(
                                "L4 SOLO CHARGE")));

            Outcome held = moveAdapter(
                    bot, fixture,
                    Decision.dockingTransitTop(source),
                    true, "0");
            assertFalse(
                    "The legacy move ladder represents this categorical stop "
                        + "with its veto score, not EvaluatedAction.hardVeto",
                    held.hardVeto());
            assertTrue(held.score() <= -10000.0f);
            assertContains(
                    held,
                    "RUNTIME_ACTOR_TRANSIT_SAFETY");
            heldActions.add(held);
        }
        assertParity(heldActions);
    }

    @Test
    public void dockingTransitCarriesSourceIntoBoundedDestinationScoring() {
        List<List<Outcome>> results = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot);
            PhysicalCard source = fixture.addDockingBay(
                    NON_BATTLEGROUND_DOCKING_BAY_BP,
                    NON_BATTLEGROUND_DOCKING_BAY_ID, false);
            PhysicalCard destination = fixture.addDockingBay(
                    BATTLEGROUND_DOCKING_BAY_BP,
                    BATTLEGROUND_DOCKING_BAY_ID, true);
            PhysicalCard decoy = fixture.addDockingBay(
                    DECOY_DOCKING_BAY_BP,
                    DECOY_DOCKING_BAY_ID, false);
            PhysicalCard vader = character(
                    VADER_BP, VADER_ID,
                    Zone.AT_LOCATION, PLAYER);
            PhysicalCard buddy = character(
                    DISTRACTOR_BP, DISTRACTOR_ID,
                    Zone.AT_LOCATION, PLAYER);
            fixture.place(vader, source);
            fixture.place(buddy, source);
            fixture.markPersona(vader, Persona.VADER);
            fixture.enableActiveIteration();
            var routes = fixture.analyzer
                    .assessDockingBayTransitRoutes(
                        fixture.game, PLAYER, source);
            assertTrue(
                    "Expected an advancing transit route: "
                        + routes,
                    routes.stream().anyMatch(route ->
                        route.admissible()
                        && route.objectiveAdvance()));

            Decision destinationDecision = Decision.dockingDestination(
                    List.of(decoy, destination));
            TracedOutcome advancingDestination =
                    tracedDockingCardSelectionAdapter(
                            bot, fixture, destinationDecision,
                            source, null,
                            String.valueOf(
                                    BATTLEGROUND_DOCKING_BAY_ID));
            assertContains(
                    advancingDestination.outcome(),
                    "MOVE.OBJECTIVE.ACTOR_LOCATION_DESTINATION");
            assertObjectiveDelta(
                    advancingDestination,
                    "MOVE.OBJECTIVE.DOCKING_TRANSIT",
                    300.0f);
            List<Outcome> sequence = combinedSequence(
                    bot, fixture,
                    Decision.dockingTransitTop(source),
                    destinationDecision);
            assertEquals(
                    "0",
                    sequence.get(0).actionId());
            assertContains(
                    sequence.get(0),
                    "MOVE.OBJECTIVE.ACTOR_LOCATION_START");
            assertEquals(
                    String.valueOf(
                        BATTLEGROUND_DOCKING_BAY_ID),
                    sequence.get(1).actionId());
            assertContains(
                    sequence.get(1),
                    "MOVE.OBJECTIVE.ACTOR_LOCATION_DESTINATION");
            results.add(sequence);
        }
        assertSequenceParity(results);
    }

    @Test
    public void dockingTransitCanMoveBuddyButNotSoleRequiredVader() {
        List<Outcome> parentChoices = new ArrayList<>();
        List<Outcome> transitCandidates = new ArrayList<>();
        List<Outcome> safeDestinations = new ArrayList<>();
        List<Outcome> safeBuddies = new ArrayList<>();
        List<Outcome> heldVaders = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot);
            PhysicalCard source = fixture.addDockingBay(
                    BATTLEGROUND_DOCKING_BAY_BP,
                    BATTLEGROUND_DOCKING_BAY_ID, true);
            PhysicalCard destination = fixture.addDockingBay(
                    NON_BATTLEGROUND_DOCKING_BAY_BP,
                    NON_BATTLEGROUND_DOCKING_BAY_ID, false);
            PhysicalCard vader = character(
                    VADER_BP, VADER_ID,
                    Zone.AT_LOCATION, PLAYER);
            PhysicalCard buddy = character(
                    DISTRACTOR_BP, DISTRACTOR_ID,
                    Zone.AT_LOCATION, PLAYER);
            fixture.place(vader, source);
            fixture.place(buddy, source);
            fixture.markPersona(vader, Persona.VADER);
            fixture.enableActiveIteration();
            var routes = fixture.analyzer
                    .assessDockingBayTransitRoutes(
                        fixture.game, PLAYER, source);
            assertTrue(
                    "Expected buddy route to remain admissible: "
                        + routes,
                    routes.stream().anyMatch(route ->
                        route.admissible()
                        && route.mover() == buddy));
            assertTrue(
                    "Expected sole Vader route to be held: "
                        + routes,
                    routes.stream().noneMatch(route ->
                        route.mover() == vader
                        && route.admissible()));

            Decision movers = Decision.dockingMovers(
                    destination, List.of(vader, buddy));
            Decision top =
                    Decision.dockingTransitTop(source);
            Outcome parent = combined(
                    bot, fixture, top);
            assertEquals("", parent.actionId());
            Outcome transit = moveAdapter(
                    bot, fixture, top,
                    true, "0");
            assertTrue(transit.score() > -10000.0f);
            TracedOutcome safeDestination =
                    tracedDockingCardSelectionAdapter(
                        bot, fixture,
                        Decision.dockingDestination(
                            List.of(destination)),
                        source, null,
                        String.valueOf(
                            NON_BATTLEGROUND_DOCKING_BAY_ID));
            assertNoObjectiveContribution(safeDestination);
            assertEquals(
                    0.0f, safeDestination.outcome().score(), 0.0f);
            TracedOutcome safeBuddy =
                    tracedDockingCardSelectionAdapter(
                        bot, fixture, movers, source,
                        destination,
                        String.valueOf(DISTRACTOR_ID));
            assertNoObjectiveContribution(safeBuddy);
            assertEquals(0.0f, safeBuddy.outcome().score(), 0.0f);
            TracedOutcome heldVader =
                    tracedDockingCardSelectionAdapter(
                        bot, fixture, movers, source,
                        destination,
                        String.valueOf(VADER_ID));
            assertFalse(heldVader.outcome().hardVeto());
            assertEquals(
                    -300.0f,
                    heldVader.outcome().score(), 0.0f);
            assertContains(
                    heldVader.outcome(),
                    "RUNTIME_ACTOR_TRANSIT_HOLD");
            assertObjectiveDelta(
                    heldVader,
                    "MOVE.OBJECTIVE.RUNTIME_ACTOR_TRANSIT_HOLD",
                    -300.0f);
            parentChoices.add(parent);
            transitCandidates.add(transit);
            safeDestinations.add(safeDestination.outcome());
            safeBuddies.add(safeBuddy.outcome());
            heldVaders.add(heldVader.outcome());
        }
        assertParity(parentChoices);
        assertParity(transitCandidates);
        assertParity(safeDestinations);
        assertParity(safeBuddies);
        assertParity(heldVaders);
    }

    @Test
    public void dockingTransitBetweenBattlegroundsDoesNotClaimNewActorProgress() {
        List<Outcome> results = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot);
            PhysicalCard source = fixture.addDockingBay(
                    BATTLEGROUND_DOCKING_BAY_BP,
                    BATTLEGROUND_DOCKING_BAY_ID, true);
            fixture.addDockingBay(
                    "12_173",
                    NON_BATTLEGROUND_DOCKING_BAY_ID, true);
            PhysicalCard vader = character(
                    VADER_BP, VADER_ID,
                    Zone.AT_LOCATION, PLAYER);
            fixture.place(vader, source);
            fixture.markPersona(vader, Persona.VADER);
            fixture.enableActiveIteration();

            Outcome transit = moveAdapter(
                    bot, fixture,
                    Decision.dockingTransitTop(source),
                    true, "0");
            assertFalse(transit.hardVeto());
            assertNotContains(
                    transit,
                    "MOVE.OBJECTIVE.ACTOR_LOCATION_START");
            assertNotContains(
                    transit,
                    "MOVE.OBJECTIVE.BLOCKER_CHASE_START");
            results.add(transit);
        }
        assertParity(results);
    }

    @Test
    public void classicRecallChildChoosesTheSafePhysicalVader() {
        List<Outcome> winners = new ArrayList<>();
        List<Outcome> heldCandidates = new ArrayList<>();
        List<Outcome> safeCandidates = new ArrayList<>();
        List<Outcome> wrongSourceCandidates = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(
                    bot, CLASSIC_HUNT_DOWN_BP, false);
            PhysicalCard battlegroundVader = character(
                    VADER_BP, VADER_ID,
                    Zone.AT_LOCATION, PLAYER);
            PhysicalCard safeVader = character(
                    VADER_BP, SECOND_VADER_ID,
                    Zone.AT_LOCATION, PLAYER);
            fixture.place(
                    battlegroundVader,
                    fixture.battleground);
            fixture.place(
                    safeVader,
                    fixture.nonBattleground);
            fixture.markPersona(
                    battlegroundVader, Persona.VADER);
            fixture.markPersona(
                    safeVader, Persona.VADER);
            when(fixture.modifiers.controlsLocation(
                    fixture.gameState,
                    fixture.battleground, PLAYER))
                    .thenReturn(true);
            when(fixture.modifiers.controlsLocation(
                    fixture.gameState,
                    fixture.nonBattleground, PLAYER))
                    .thenReturn(true);
            fixture.setLiveAction(
                    actionSource(
                        "Hunt Down And Destroy The Jedi",
                        CLASSIC_HUNT_DOWN_BP),
                    "Take Vader into hand");

            Outcome winner = combined(
                    bot, fixture, Decision.recallTarget());
            assertEquals(
                    String.valueOf(SECOND_VADER_ID),
                    winner.actionId());
            assertContains(
                    winner,
                    "RECALL_TARGET_SAFE");

            Outcome held = cardSelectionAdapter(
                    bot, fixture, Decision.recallTarget(),
                    true, String.valueOf(VADER_ID));
            assertFalse(held.hardVeto());
            assertContains(held, "RECALL_TARGET_HOLD");
            assertTrue(held.score() <= -250.0f);
            Outcome safe = cardSelectionAdapter(
                    bot, fixture, Decision.recallTarget(),
                    true, String.valueOf(SECOND_VADER_ID));
            assertFalse(safe.hardVeto());
            assertContains(safe, "RECALL_TARGET_SAFE");

            fixture.setLiveAction(
                    actionSource(
                        "Rise, My Friend", "9_140"),
                    "Take Vader into hand");
            Outcome wrongSource = cardSelectionAdapter(
                    bot, fixture, Decision.recallTarget(),
                    true, String.valueOf(VADER_ID));
            assertNotContains(
                    wrongSource, "OBJECTIVE.HUNT_DOWN.RECALL_TARGET");
            winners.add(winner);
            heldCandidates.add(held);
            safeCandidates.add(safe);
            wrongSourceCandidates.add(wrongSource);
        }
        assertParity(winners);
        assertParity(heldCandidates);
        assertParity(safeCandidates);
        assertParity(wrongSourceCandidates);
    }

    @Test
    public void virtualRecallChildUsesTheBroadRealTargetLaw() {
        List<Outcome> uncontrolledCandidates = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(
                    bot, HUNT_DOWN_BP, true);
            PhysicalCard first = character(
                    VADER_BP, VADER_ID,
                    Zone.AT_LOCATION, PLAYER);
            PhysicalCard uncontrolled = character(
                    VADER_BP, SECOND_VADER_ID,
                    Zone.AT_LOCATION, PLAYER);
            fixture.place(first, fixture.battleground);
            fixture.place(
                    uncontrolled,
                    fixture.nonBattleground);
            fixture.markPersona(first, Persona.VADER);
            fixture.markPersona(
                    uncontrolled, Persona.VADER);
            fixture.setLiveAction(
                    actionSource(
                        "Hunt Down And Destroy The Jedi (V)",
                        HUNT_DOWN_BP),
                    "Take Vader into hand");

            Outcome candidate = cardSelectionAdapter(
                    bot, fixture, Decision.recallTarget(),
                    true,
                    String.valueOf(SECOND_VADER_ID));
            assertFalse(candidate.hardVeto());
            assertContains(candidate, "RECALL_TARGET_SAFE");
            uncontrolledCandidates.add(candidate);
        }
        assertParity(uncontrolledCandidates);
    }

    @Test
    public void castlePromptsKeepDuplicateVadersAndRejectUnsafeChoices() {
        List<Outcome> parentResults = new ArrayList<>();
        List<Outcome> originResults = new ArrayList<>();
        List<Outcome> destinationResults = new ArrayList<>();
        List<Outcome> moverResults = new ArrayList<>();
        List<Outcome> impostorCandidates = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot);
            PhysicalCard firstVader = character(
                    VADER_BP, VADER_ID,
                    Zone.AT_LOCATION, PLAYER);
            PhysicalCard secondVader = character(
                    VADER_BP, SECOND_VADER_ID,
                    Zone.AT_LOCATION, PLAYER);
            PhysicalCard broker = character(
                    VADERS_BROKER_BP, 452,
                    Zone.AT_LOCATION, PLAYER);
            PhysicalCard dangerousJedi = character(
                    "1_021", 453,
                    Zone.AT_LOCATION, OPPONENT);
            fixture.place(firstVader, fixture.castle);
            fixture.place(secondVader, fixture.castle);
            fixture.place(broker, fixture.castle);
            fixture.place(
                    dangerousJedi,
                    fixture.blockerBattleground);
            fixture.markPersona(firstVader, Persona.VADER);
            fixture.markPersona(secondVader, Persona.VADER);
            fixture.prepareCastleLocationText(
                    firstVader, 1.0f);
            fixture.prepareCastleLocationText(
                    secondVader, 1.0f);
            when(fixture.modifiers.getTotalPowerAtLocation(
                    fixture.gameState,
                    fixture.blockerBattleground,
                    OPPONENT, false, false))
                    .thenReturn(20.0f);
            fixture.setLiveAction(
                    fixture.castle,
                    "Move from here to other battleground site");

            assertEquals(4, fixture.analyzer
                    .assessVaderCastleRoutes(
                        fixture.game, PLAYER,
                        fixture.castle, true)
                    .size());
            Outcome parent = combined(
                    bot, fixture,
                    Decision.castleTopLevelMove());
            assertEquals("castle-move", parent.actionId());
            assertContains(
                    parent,
                    "MOVE.OBJECTIVE.ACTOR_LOCATION_START");

            Decision originDecision =
                    Decision.castleChoice(
                        "Choose card to move from",
                        List.of(String.valueOf(CASTLE_ID)),
                        List.of(CASTLE_BP),
                        List.of(fixture.castle.getTitle()));
            Outcome origin = combined(
                    bot, fixture, originDecision);
            assertEquals(
                    String.valueOf(CASTLE_ID),
                    origin.actionId());
            assertContains(
                    origin,
                    "OBJECTIVE.HUNT_DOWN.CASTLE_ROUTE");

            Decision destinationDecision =
                    Decision.castleChoice(
                        "Choose card to move to",
                        List.of(
                            String.valueOf(BATTLEGROUND_ID),
                            String.valueOf(
                                BLOCKER_BATTLEGROUND_ID)),
                        List.of(
                            BATTLEGROUND_BP,
                            BLOCKER_BATTLEGROUND_BP),
                        List.of(
                            fixture.battleground.getTitle(),
                            fixture.blockerBattleground
                                .getTitle()));
            Outcome destination = combined(
                    bot, fixture, destinationDecision);
            assertEquals(
                    String.valueOf(BATTLEGROUND_ID),
                    destination.actionId());
            assertContains(
                    destination,
                    "OBJECTIVE.HUNT_DOWN.CASTLE_ROUTE");
            Outcome unsafeDestination =
                    cardSelectionAdapter(
                        bot, fixture, destinationDecision,
                        true,
                        String.valueOf(
                            BLOCKER_BATTLEGROUND_ID));
            assertTrue(unsafeDestination.hardVeto());
            assertTrue(
                    "The rejected Castle destination must be a FormationSafety failure",
                    fixture.analyzer.assessVaderCastleRoutes(
                            fixture.game, PLAYER,
                            fixture.castle, true)
                        .stream().anyMatch(route ->
                            route.destination()
                                == fixture.blockerBattleground
                            && !route.formationSafe()));

            String moverPrompt =
                    "Choose card to move to "
                    + GameUtils.getCardLink(
                        fixture.battleground);
            Decision moverDecision =
                    Decision.castleChoice(
                        moverPrompt,
                        List.of("452",
                            String.valueOf(VADER_ID),
                            String.valueOf(SECOND_VADER_ID)),
                        List.of(
                            VADERS_BROKER_BP,
                            VADER_BP, VADER_BP),
                        List.of(
                            broker.getTitle(),
                            firstVader.getTitle(),
                            secondVader.getTitle()));
            Outcome mover = combined(
                    bot, fixture, moverDecision);
            assertEquals(
                    String.valueOf(VADER_ID),
                    mover.actionId());
            assertContains(
                    mover,
                    "OBJECTIVE.HUNT_DOWN.CASTLE_ROUTE");
            Outcome impostor = cardSelectionAdapter(
                    bot, fixture, moverDecision,
                    true, "452");
            assertTrue(impostor.hardVeto());
            assertContains(
                    impostor,
                    "CASTLE_ROUTE_HOLD");
            parentResults.add(parent);
            originResults.add(origin);
            destinationResults.add(destination);
            moverResults.add(mover);
            impostorCandidates.add(impostor);
        }
        assertParity(parentResults);
        assertParity(originResults);
        assertParity(destinationResults);
        assertParity(moverResults);
        assertParity(impostorCandidates);
    }

    @Test
    public void castleReturnHoldIsBoundedForOriginAndMoverChildren() {
        List<Outcome> origins = new ArrayList<>();
        List<Outcome> movers = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot);
            PhysicalCard vader = character(
                    VADER_BP, VADER_ID,
                    Zone.AT_LOCATION, PLAYER);
            fixture.place(vader, fixture.battleground);
            fixture.markPersona(vader, Persona.VADER);
            when(fixture.modifiers.getMoveUsingLocationTextCost(
                    fixture.gameState, vader,
                    fixture.battleground, fixture.castle,
                    1.0f, 0.0f)).thenReturn(1.0f);
            fixture.setLiveAction(
                    fixture.castle,
                    "Move from other battleground site to here");

            var routes = fixture.analyzer
                    .assessVaderCastleRoutes(
                        fixture.game, PLAYER,
                        fixture.castle, false);
            assertTrue(
                    "Expected a legal, formation-safe Castle return held only by objective intent: "
                        + routes,
                    routes.stream().anyMatch(route ->
                        route.mover() == vader
                            && route.formationSafe()
                            && route.returnHeld()
                            && !route.admissible()));

            Decision originDecision = Decision.castleChoice(
                    "Choose card to move from",
                    List.of(String.valueOf(BATTLEGROUND_ID)),
                    List.of(BATTLEGROUND_BP),
                    List.of(fixture.battleground.getTitle()));
            TracedOutcome origin = tracedCardSelectionAdapter(
                    bot, fixture, originDecision,
                    true, String.valueOf(BATTLEGROUND_ID));
            assertFalse(origin.outcome().hardVeto());
            assertEquals(-300.0f, origin.outcome().score(), 0.0f);
            assertObjectiveDelta(
                    origin,
                    "SELECT.OBJECTIVE.HUNT_DOWN.CASTLE_ROUTE_HOLD",
                    -300.0f);

            Decision moverDecision = Decision.castleChoice(
                    "Choose card to move to "
                        + GameUtils.getCardLink(fixture.castle),
                    List.of(String.valueOf(VADER_ID)),
                    List.of(VADER_BP),
                    List.of(vader.getTitle()));
            TracedOutcome mover = tracedCardSelectionAdapter(
                    bot, fixture, moverDecision,
                    true, String.valueOf(VADER_ID));
            assertFalse(mover.outcome().hardVeto());
            assertEquals(-300.0f, mover.outcome().score(), 0.0f);
            assertObjectiveDelta(
                    mover,
                    "SELECT.OBJECTIVE.HUNT_DOWN.CASTLE_ROUTE_HOLD",
                    -300.0f);
            origins.add(origin.outcome());
            movers.add(mover.outcome());
        }
        assertParity(origins);
        assertParity(movers);
    }

    @Test
    public void boundedObjectiveBlockerPreferenceDoesNotForceTheBattle() {
        List<Outcome> results = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = fixture(bot);
            PhysicalCard ourFirst = character(
                    DISTRACTOR_BP, 401, Zone.AT_LOCATION, PLAYER);
            PhysicalCard theirNonblocker = character(
                    "1_004", 402, Zone.AT_LOCATION, OPPONENT);
            PhysicalCard ourSecond = character(
                    DISTRACTOR_BP, 403, Zone.AT_LOCATION, PLAYER);
            PhysicalCard jedi = character(
                    "1_021", 404, Zone.AT_LOCATION, OPPONENT);

            fixture.place(ourFirst, fixture.battleground);
            fixture.place(
                    theirNonblocker, fixture.battleground);
            fixture.place(
                    ourSecond, fixture.blockerBattleground);
            fixture.place(jedi, fixture.blockerBattleground);
            when(fixture.modifiers.getAbility(
                    fixture.gameState, theirNonblocker))
                    .thenReturn(4.0f);
            when(fixture.modifiers.getAbility(
                    fixture.gameState, jedi)).thenReturn(6.0f);
            fixture.equalSafeBattle(fixture.battleground);
            fixture.equalSafeBattle(
                    fixture.blockerBattleground);

            Outcome result = combined(
                    bot, fixture, Decision.battle());

            assertEquals("nonblocker", result.actionId());
            assertNotContains(
                    result,
                    "Remove an opponent actor blocking the objective");
            results.add(result);
        }
        assertParity(results);
    }

    private static Outcome combined(
            Bot bot, Fixture fixture, Decision decision) {
        return combined(bot, fixture, decision, true);
    }

    private static Outcome combined(
            Bot bot, Fixture fixture, Decision decision,
            boolean objectiveAware) {
        if (bot == Bot.RANDO) {
            return outcome(
                    new com.gempukku.swccgo.ai.models.rando.evaluators
                            .CombinedEvaluator()
                            .evaluateDecision(
                                    randoContext(
                                            fixture, decision,
                                            objectiveAware)));
        }
        return outcome(
                new com.gempukku.swccgo.ai.models.chosenone.evaluators
                        .CombinedEvaluator()
                        .evaluateDecision(
                                chosenContext(
                                        fixture, decision,
                                        objectiveAware)));
    }

    private static List<Outcome> combinedSequence(
            Bot bot, Fixture fixture,
            Decision... decisions) {
        List<Outcome> outcomes = new ArrayList<>();
        if (bot == Bot.RANDO) {
            var evaluator =
                    new com.gempukku.swccgo.ai.models.rando
                        .evaluators.CombinedEvaluator();
            for (Decision decision : decisions) {
                outcomes.add(outcome(
                    evaluator.evaluateDecision(
                        randoContext(
                            fixture, decision, true))));
            }
            return outcomes;
        }
        var evaluator =
                new com.gempukku.swccgo.ai.models.chosenone
                    .evaluators.CombinedEvaluator();
        for (Decision decision : decisions) {
            outcomes.add(outcome(
                evaluator.evaluateDecision(
                    chosenContext(
                        fixture, decision, true))));
        }
        return outcomes;
    }

    private static Outcome deployAdapter(
            Bot bot, Fixture fixture, Decision decision,
            boolean objectiveAware, String actionId) {
        if (bot == Bot.RANDO) {
            return find(
                    new com.gempukku.swccgo.ai.models.rando.evaluators
                            .DeployEvaluator()
                            .evaluate(randoContext(
                                    fixture, decision,
                                    objectiveAware)),
                    actionId);
        }
        return findChosen(
                new com.gempukku.swccgo.ai.models.chosenone.evaluators
                        .DeployEvaluator()
                        .evaluate(chosenContext(
                                fixture, decision,
                                objectiveAware)),
                actionId);
    }

    private static Outcome moveAdapter(
            Bot bot, Fixture fixture, Decision decision,
            boolean objectiveAware, String actionId) {
        if (bot == Bot.RANDO) {
            return find(
                    new com.gempukku.swccgo.ai.models.rando.evaluators
                            .MoveEvaluator()
                            .evaluate(randoContext(
                                    fixture, decision,
                                    objectiveAware)),
                    actionId);
        }
        return findChosen(
                new com.gempukku.swccgo.ai.models.chosenone.evaluators
                        .MoveEvaluator()
                        .evaluate(chosenContext(
                                fixture, decision,
                                objectiveAware)),
                actionId);
    }

    private static TracedOutcome tracedMoveAdapter(
            Bot bot, Fixture fixture, Decision decision,
            boolean objectiveAware, String actionId) {
        assertTrue(TraceSession.open(
                bot.name(), "hunt-down-focused-move",
                decision.type(), decision.text(),
                decision.actionIds(), null,
                List.of("objective route cap"), false));
        Outcome outcome;
        DecisionTrace trace;
        try {
            outcome = moveAdapter(
                    bot, fixture, decision,
                    objectiveAware, actionId);
        } finally {
            trace = TraceSession.close();
        }
        assertNotNull(trace);
        return new TracedOutcome(outcome, trace);
    }

    private static Outcome cardSelectionAdapter(
            Bot bot, Fixture fixture, Decision decision,
            boolean objectiveAware, String actionId) {
        if (bot == Bot.RANDO) {
            return find(
                    new com.gempukku.swccgo.ai.models.rando.evaluators
                            .CardSelectionEvaluator()
                            .evaluate(randoContext(
                                    fixture, decision,
                                    objectiveAware)),
                    actionId);
        }
        return findChosen(
                new com.gempukku.swccgo.ai.models.chosenone.evaluators
                        .CardSelectionEvaluator()
                        .evaluate(chosenContext(
                                fixture, decision,
                                objectiveAware)),
                actionId);
    }

    private static TracedOutcome tracedCardSelectionAdapter(
            Bot bot, Fixture fixture, Decision decision,
            boolean objectiveAware, String actionId) {
        assertTrue(TraceSession.open(
                bot.name(), "hunt-down-focused-selection",
                decision.type(), decision.text(),
                decision.cardIds(), null,
                List.of("objective route cap"), false));
        Outcome outcome;
        DecisionTrace trace;
        try {
            outcome = cardSelectionAdapter(
                    bot, fixture, decision,
                    objectiveAware, actionId);
        } finally {
            trace = TraceSession.close();
        }
        assertNotNull(trace);
        return new TracedOutcome(outcome, trace);
    }

    private static Outcome dockingCardSelectionAdapter(
            Bot bot, Fixture fixture, Decision decision,
            PhysicalCard source,
            PhysicalCard destination,
            String actionId) {
        String sourceId =
                String.valueOf(source.getCardId());
        String extraKey =
                com.gempukku.swccgo.ai.models.common.strategy
                    .ObjectiveAnalyzer
                    .DOCKING_TRANSIT_SOURCE_CARD_ID_EXTRA;
        String destinationExtraKey =
                com.gempukku.swccgo.ai.models.common.strategy
                    .ObjectiveAnalyzer
                    .DOCKING_TRANSIT_DESTINATION_CARD_ID_EXTRA;
        if (bot == Bot.RANDO) {
            var context = randoContext(
                    fixture, decision, true);
            context.setExtra(extraKey, sourceId);
            if (destination != null) {
                context.setExtra(
                        destinationExtraKey,
                        String.valueOf(
                            destination.getCardId()));
            }
            return find(
                    new com.gempukku.swccgo.ai.models.rando
                        .evaluators.CardSelectionEvaluator()
                        .evaluate(context),
                    actionId);
        }
        var context = chosenContext(
                fixture, decision, true);
        context.setExtra(extraKey, sourceId);
        if (destination != null) {
            context.setExtra(
                    destinationExtraKey,
                    String.valueOf(
                        destination.getCardId()));
        }
        return findChosen(
                new com.gempukku.swccgo.ai.models.chosenone
                    .evaluators.CardSelectionEvaluator()
                    .evaluate(context),
                actionId);
    }

    private static TracedOutcome tracedDockingCardSelectionAdapter(
            Bot bot, Fixture fixture, Decision decision,
            PhysicalCard source,
            PhysicalCard destination,
            String actionId) {
        assertTrue(TraceSession.open(
                bot.name(), "hunt-down-focused-docking-selection",
                decision.type(), decision.text(),
                decision.cardIds(), null,
                List.of("objective route cap"), false));
        Outcome outcome;
        DecisionTrace trace;
        try {
            outcome = dockingCardSelectionAdapter(
                    bot, fixture, decision,
                    source, destination, actionId);
        } finally {
            trace = TraceSession.close();
        }
        assertNotNull(trace);
        return new TracedOutcome(outcome, trace);
    }

    private static com.gempukku.swccgo.ai.models.rando.evaluators
            .DecisionContext randoContext(
                    Fixture fixture, Decision decision) {
        return randoContext(fixture, decision, true);
    }

    private static com.gempukku.swccgo.ai.models.rando.evaluators
            .DecisionContext randoContext(
                    Fixture fixture, Decision decision,
                    boolean objectiveAware) {
        var context =
                new com.gempukku.swccgo.ai.models.rando.evaluators
                        .DecisionContext(
                                fixture.gameState, PLAYER,
                                decision.type(), decision.text(),
                                "hunt-down-public-decision",
                                decision.phase());
        context.setGame(fixture.game);
        context.setSide(Side.DARK);
        if (objectiveAware) {
            context.setObjectiveAnalyzer(
                    (com.gempukku.swccgo.ai.models.rando.strategy
                            .ObjectiveAnalyzer) fixture.analyzer);
        }
        apply(context, decision);
        return context;
    }

    private static com.gempukku.swccgo.ai.models.chosenone.evaluators
            .DecisionContext chosenContext(
                    Fixture fixture, Decision decision) {
        return chosenContext(fixture, decision, true);
    }

    private static com.gempukku.swccgo.ai.models.chosenone.evaluators
            .DecisionContext chosenContext(
                    Fixture fixture, Decision decision,
                    boolean objectiveAware) {
        var context =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators
                        .DecisionContext(
                                fixture.gameState, PLAYER,
                                decision.type(), decision.text(),
                                "hunt-down-public-decision",
                                decision.phase());
        context.setGame(fixture.game);
        context.setSide(Side.DARK);
        if (objectiveAware) {
            context.setObjectiveAnalyzer(
                    (com.gempukku.swccgo.ai.models.chosenone.strategy
                            .ObjectiveAnalyzer) fixture.analyzer);
        }
        apply(context, decision);
        return context;
    }

    private static void apply(
            com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext
                    context,
            Decision decision) {
        context.setActionIds(decision.actionIds());
        context.setActionTexts(decision.actionTexts());
        context.setCardIds(decision.cardIds());
        context.setBlueprints(decision.blueprints());
        context.setTestingTexts(decision.testingTexts());
        if (decision.emitsSelectable()) {
            context.setSelectable(Collections.nCopies(
                    decision.selectionCount(), true));
        }
        context.setNoPass(decision.noPass());
        context.setMin(decision.min());
        context.setMax(decision.maximum());
    }

    private static void apply(
            com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext
                    context,
            Decision decision) {
        context.setActionIds(decision.actionIds());
        context.setActionTexts(decision.actionTexts());
        context.setCardIds(decision.cardIds());
        context.setBlueprints(decision.blueprints());
        context.setTestingTexts(decision.testingTexts());
        if (decision.emitsSelectable()) {
            context.setSelectable(Collections.nCopies(
                    decision.selectionCount(), true));
        }
        context.setNoPass(decision.noPass());
        context.setMin(decision.min());
        context.setMax(decision.maximum());
    }

    private static Outcome outcome(
            com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction
                    action) {
        assertNotNull(action);
        return new Outcome(
                action.getActionId(), action.getScore(),
                List.copyOf(action.getReasoning()),
                action.isHardVetoed(), action.getVetoReason());
    }

    private static Outcome outcome(
            com.gempukku.swccgo.ai.models.chosenone.evaluators.EvaluatedAction
                    action) {
        assertNotNull(action);
        return new Outcome(
                action.getActionId(), action.getScore(),
                List.copyOf(action.getReasoning()),
                action.isHardVetoed(), action.getVetoReason());
    }

    private static Outcome find(
            List<com.gempukku.swccgo.ai.models.rando.evaluators
                    .EvaluatedAction> actions,
            String actionId) {
        return actions.stream()
                .filter(action -> actionId.equals(
                        action.getActionId()))
                .findFirst()
                .map(HuntDownCombinedEvaluatorDecisionTest::outcome)
                .orElseThrow(() -> new AssertionError(
                        "Missing action " + actionId));
    }

    private static Outcome findChosen(
            List<com.gempukku.swccgo.ai.models.chosenone.evaluators
                    .EvaluatedAction> actions,
            String actionId) {
        return actions.stream()
                .filter(action -> actionId.equals(
                        action.getActionId()))
                .findFirst()
                .map(HuntDownCombinedEvaluatorDecisionTest::outcome)
                .orElseThrow(() -> new AssertionError(
                        "Missing action " + actionId));
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
        assertTrue(
                "Did not expect '" + marker + "' in "
                        + outcome.reasoning(),
                outcome.reasoning().stream()
                        .noneMatch(reason -> reason.contains(marker)));
    }

    private static void assertObjectiveDelta(
            TracedOutcome traced,
            String ruleId, float delta) {
        assertTrue(
                "Expected typed objective operation '" + ruleId
                    + "' at " + delta + " in "
                    + traced.trace().getOperations(),
                traced.trace().getOperations().stream()
                    .anyMatch(operation ->
                        traced.outcome().actionId().equals(
                            operation.getActionId())
                        && operation.getRuleId() != null
                        && ruleId.equals(
                            operation.getRuleId().id())
                        && operation.getDomainId()
                            == TraceDomainId.OBJECTIVE_INTENT
                        && operation.getOutputKind()
                            == TraceOutputKind.BANDED
                        && operation.getDeltaBits() != null
                        && operation.getDeltaBits()
                            == Float.floatToRawIntBits(delta)
                        && !operation.isVetoed()));
    }

    private static void assertNoObjectiveContribution(
            TracedOutcome traced) {
        assertTrue(
                "Expected no nonzero objective contribution for "
                    + traced.outcome().actionId() + " in "
                    + traced.trace().getOperations(),
                traced.trace().getOperations().stream()
                    .noneMatch(operation ->
                        traced.outcome().actionId().equals(
                            operation.getActionId())
                        && operation.getDomainId()
                            == TraceDomainId.OBJECTIVE_INTENT
                        && operation.getDeltaBits() != null
                        && Float.intBitsToFloat(
                            operation.getDeltaBits()) != 0.0f
                        && !operation.isVetoed()));
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

    private static void assertSequenceParity(
            List<List<Outcome>> sequences) {
        assertEquals(2, sequences.size());
        assertEquals(
                sequences.get(0).size(),
                sequences.get(1).size());
        for (int i = 0;
                i < sequences.get(0).size(); i++) {
            assertParity(List.of(
                    sequences.get(0).get(i),
                    sequences.get(1).get(i)));
        }
    }

    private static Fixture fixture(Bot bot) {
        return fixture(bot, HUNT_DOWN_BP, false);
    }

    private static Fixture fixture(
            Bot bot, String objectiveBlueprintId,
            boolean flipped) {
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);

        List<PhysicalCard> hand = new ArrayList<>();
        List<PhysicalCard> reserve = new ArrayList<>();
        List<PhysicalCard> permanents = new ArrayList<>();
        List<PhysicalCard> stacked = new ArrayList<>();
        Map<Integer, PhysicalCard> cardsById =
                new LinkedHashMap<>();
        Map<PhysicalCard, List<PhysicalCard>> cardsAt =
                new IdentityHashMap<>();

        PhysicalCard objective = objective(
                objectiveBlueprintId, flipped);
        PhysicalCard battleground = site(
                BATTLEGROUND_BP, BATTLEGROUND_ID, Zone.LOCATIONS);
        PhysicalCard nonBattleground = site(
                NON_BATTLEGROUND_BP,
                NON_BATTLEGROUND_ID, Zone.LOCATIONS);
        PhysicalCard blockerBattleground = site(
                BLOCKER_BATTLEGROUND_BP,
                BLOCKER_BATTLEGROUND_ID, Zone.LOCATIONS);
        PhysicalCard castle = site(
                CASTLE_BP, CASTLE_ID, Zone.LOCATIONS);
        List<PhysicalCard> locations =
                new ArrayList<>(List.of(
                    battleground,
                    nonBattleground,
                    blockerBattleground,
                    castle));

        permanents.addAll(List.of(
                objective, battleground, nonBattleground,
                blockerBattleground, castle));
        for (PhysicalCard card : List.of(
                objective, battleground,
                nonBattleground, blockerBattleground, castle)) {
            cardsById.put(card.getCardId(), card);
        }

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(modifiers.hasKeyword(
                any(GameState.class),
                any(PhysicalCard.class),
                any(com.gempukku.swccgo.common.Keyword.class)))
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
        when(gameState.getReserveDeckSize(PLAYER)).thenReturn(20);
        when(gameState.getUsedPile(PLAYER)).thenReturn(List.of());
        when(gameState.getHand(PLAYER)).thenReturn(hand);
        when(gameState.getReserveDeck(PLAYER)).thenReturn(reserve);
        when(gameState.getCardPile(PLAYER, Zone.RESERVE_DECK))
                .thenReturn(reserve);
        when(gameState.getAllStackedCards()).thenReturn(stacked);
        when(gameState.getAllPermanentCards()).thenReturn(permanents);
        when(gameState.getLocationsInOrder()).thenReturn(locations);
        when(gameState.getTopLocations()).thenReturn(locations);
        when(gameState.getPlayerLifeForce(OPPONENT)).thenReturn(20);
        when(gameState.findCardById(anyInt())).thenAnswer(
                invocation -> cardsById.get(
                        invocation.getArgument(0, Integer.class)));
        when(gameState.findCardByPermanentId(anyInt())).thenAnswer(
                invocation -> cardsById.get(
                        invocation.getArgument(0, Integer.class)));
        when(gameState.getCardsAtLocation(
                any(PhysicalCard.class))).thenAnswer(
                    invocation -> cardsAt.getOrDefault(
                            invocation.getArgument(0, PhysicalCard.class),
                            List.of()));
        when(gameState.getAttachedCards(any(PhysicalCard.class)))
                .thenReturn(List.of());

        when(modifiers.isBattleground(
                gameState, battleground, null)).thenReturn(true);
        when(modifiers.isBattleground(
                gameState, blockerBattleground, null))
                .thenReturn(true);
        when(modifiers.isBattleground(
                gameState, nonBattleground, null))
                .thenReturn(false);
        for (PhysicalCard location : locations) {
            when(modifiers.getLocationHere(
                    gameState, location)).thenReturn(location);
            setActive(gameState, location);
        }
        distance(
                modifiers, gameState,
                nonBattleground, battleground, 1);
        distance(
                modifiers, gameState,
                nonBattleground, blockerBattleground, 1);
        distance(
                modifiers, gameState,
                battleground, blockerBattleground, 1);
        distance(
                modifiers, gameState,
                battleground, battleground, 0);
        distance(
                modifiers, gameState,
                nonBattleground, nonBattleground, 0);
        distance(
                modifiers, gameState,
                blockerBattleground, blockerBattleground, 0);
        when(modifiers.getSitesBetween(
                any(), any(), any())).thenReturn(List.of());
        when(modifiers.getForceAvailableToUse(
                gameState, PLAYER)).thenReturn(10);
        setDeployable(modifiers, true);
        setActive(gameState, objective);

        ObjectiveAnalyzer analyzer = bot == Bot.RANDO
                ? new com.gempukku.swccgo.ai.models.rando.strategy
                        .ObjectiveAnalyzer()
                : new com.gempukku.swccgo.ai.models.chosenone.strategy
                        .ObjectiveAnalyzer();
        analyzer.analyze(game, PLAYER, Side.DARK);
        assertEquals(
                "Hunt Down And Destroy The Jedi",
                analyzer.getActivePlaybook().label);

        return new Fixture(
                analyzer, game, gameState, modifiers,
                hand, reserve, permanents, locations,
                cardsById, cardsAt,
                battleground, nonBattleground,
                blockerBattleground, castle);
    }

    private static void setDeployable(
            ModifiersQuerying modifiers, boolean deployable) {
        when(modifiers.isDeployable(
                any(), any(), any(), anyBoolean(), any(),
                anyBoolean(), anyFloat(), any(), any(), any(),
                any(), any(), anyBoolean(), anyFloat()))
                .thenReturn(deployable);
        when(modifiers.isDeployableToTarget(
                any(), any(), any(), anyBoolean(), any(),
                anyBoolean(), anyFloat(), any(), any(), any(),
                any(), any(), any(), anyBoolean(), anyFloat()))
                .thenReturn(deployable);
    }

    private static void distance(
            ModifiersQuerying modifiers,
            GameState gameState,
            PhysicalCard first,
            PhysicalCard second,
            int distance) {
        when(modifiers.getDistanceBetweenSites(
                gameState, first, second)).thenReturn(distance);
        when(modifiers.getDistanceBetweenSites(
                gameState, second, first)).thenReturn(distance);
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

    private static PhysicalCard objective(
            String blueprintId, boolean flipped) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint front = blueprint(blueprintId);
        SwccgCardBlueprint back =
                blueprint(blueprintId + "_BACK");
        when(card.getOwner()).thenReturn(PLAYER);
        when(card.getZone()).thenReturn(Zone.SIDE_OF_TABLE);
        when(card.getBlueprint()).thenReturn(
                flipped ? back : front);
        when(card.getOtherSideBlueprint()).thenReturn(
                flipped ? front : back);
        when(card.getBlueprintId(true)).thenReturn(blueprintId);
        when(card.getBlueprintId(false)).thenReturn(
                flipped ? blueprintId + "_BACK" : blueprintId);
        when(card.getTitle()).thenReturn(
                (flipped ? back : front).getTitle());
        when(card.getTitles()).thenReturn(List.of(
                (flipped ? back : front).getTitle()));
        when(card.getPermanentCardId()).thenReturn(OBJECTIVE_ID);
        when(card.getCardId()).thenReturn(OBJECTIVE_ID);
        when(card.getAdditionalCardIds()).thenReturn(List.of());
        when(card.isFlipped()).thenReturn(flipped);
        return card;
    }

    private static PhysicalCard site(
            String blueprintId, int cardId, Zone zone) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = blueprint(blueprintId);
        when(card.getOwner()).thenReturn(PLAYER);
        when(card.getZone()).thenReturn(zone);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.getBlueprintId(true)).thenReturn(blueprintId);
        when(card.getBlueprintId(false)).thenReturn(blueprintId);
        when(card.getTitle()).thenReturn(blueprint.getTitle());
        when(card.getTitles()).thenReturn(
                List.of(blueprint.getTitle()));
        when(card.getPermanentCardId()).thenReturn(cardId);
        when(card.getCardId()).thenReturn(cardId);
        when(card.getAdditionalCardIds()).thenReturn(List.of());
        when(card.isBlownAway()).thenReturn(false);
        return card;
    }

    private static PhysicalCard character(
            String blueprintId, int cardId, Zone zone,
            String owner) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = blueprint(blueprintId);
        assertEquals(CardCategory.CHARACTER,
                blueprint.getCardCategory());
        when(card.getOwner()).thenReturn(owner);
        when(card.getZone()).thenReturn(zone);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.getBlueprintId(true)).thenReturn(blueprintId);
        when(card.getBlueprintId(false)).thenReturn(blueprintId);
        when(card.getTitle()).thenReturn(blueprint.getTitle());
        when(card.getTitles()).thenReturn(
                List.of(blueprint.getTitle()));
        when(card.getPermanentCardId()).thenReturn(cardId);
        when(card.getCardId()).thenReturn(cardId);
        when(card.getAdditionalCardIds()).thenReturn(List.of());
        when(card.isCaptive()).thenReturn(false);
        when(card.isUndercover()).thenReturn(false);
        return card;
    }

    private static PhysicalCard effect(
            String blueprintId, int cardId, Zone zone,
            String owner) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = blueprint(blueprintId);
        assertEquals(CardCategory.EFFECT,
                blueprint.getCardCategory());
        when(card.getOwner()).thenReturn(owner);
        when(card.getZone()).thenReturn(zone);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.getBlueprintId(true)).thenReturn(blueprintId);
        when(card.getBlueprintId(false)).thenReturn(blueprintId);
        when(card.getTitle()).thenReturn(blueprint.getTitle());
        when(card.getTitles()).thenReturn(
                List.of(blueprint.getTitle()));
        when(card.getPermanentCardId()).thenReturn(cardId);
        when(card.getCardId()).thenReturn(cardId);
        when(card.getAdditionalCardIds()).thenReturn(List.of());
        return card;
    }

    private static PhysicalCard mockCharacter(
            String title, int cardId, Zone zone,
            String owner) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint =
                mock(SwccgCardBlueprint.class);
        when(blueprint.getTitle()).thenReturn(title);
        when(blueprint.getCardCategory())
                .thenReturn(CardCategory.CHARACTER);
        when(card.getOwner()).thenReturn(owner);
        when(card.getZone()).thenReturn(zone);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.getTitle()).thenReturn(title);
        when(card.getTitles()).thenReturn(List.of(title));
        when(card.getPermanentCardId()).thenReturn(cardId);
        when(card.getCardId()).thenReturn(cardId);
        when(card.getAdditionalCardIds()).thenReturn(List.of());
        return card;
    }

    private static PhysicalCard deployTestCharacter(
            int cardId, float deployCost, float destiny) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint =
                mock(SwccgCardBlueprint.class);
        when(blueprint.getTitle())
                .thenReturn("Neutral Budget Character");
        when(blueprint.getCardCategory())
                .thenReturn(CardCategory.CHARACTER);
        when(blueprint.getDeployCost()).thenReturn(deployCost);
        when(blueprint.hasPowerAttribute()).thenReturn(true);
        when(blueprint.getPower()).thenReturn(6.0f);
        when(blueprint.hasAbilityAttribute()).thenReturn(true);
        when(blueprint.getAbility()).thenReturn(6.0f);
        when(blueprint.getDestiny()).thenReturn(destiny);
        when(card.getOwner()).thenReturn(PLAYER);
        when(card.getZone()).thenReturn(Zone.HAND);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.getTitle())
                .thenReturn("Neutral Budget Character");
        when(card.getTitles())
                .thenReturn(List.of("Neutral Budget Character"));
        when(card.getPermanentCardId()).thenReturn(cardId);
        when(card.getCardId()).thenReturn(cardId);
        when(card.getAdditionalCardIds()).thenReturn(List.of());
        return card;
    }

    private static PhysicalCard actionSource(
            String title, String blueprintId) {
        PhysicalCard source = mock(PhysicalCard.class);
        when(source.getTitle()).thenReturn(title);
        when(source.getBlueprintId(true))
                .thenReturn(blueprintId);
        return source;
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

        private static Decision locationDownload() {
            return new Decision(
                    "CARD_ACTION_CHOICE",
                    "Choose deploy action",
                    Phase.DEPLOY,
                    List.of("download-site", "pass"),
                    List.of(
                            "Deploy a location from Reserve Deck",
                            "Pass"),
                    List.of(String.valueOf(OBJECTIVE_ID), ""),
                    List.of(HUNT_DOWN_BP, ""),
                    List.of(
                            "Hunt Down And Destroy The Jedi (V)",
                            "Pass"),
                    false, 0);
        }

        private static Decision pullVader() {
            return new Decision(
                    "ARBITRARY_CARDS",
                    "Choose card to deploy from Reserve Deck",
                    Phase.DEPLOY,
                    List.of(), List.of(), List.of(),
                    List.of(DISTRACTOR_BP, VADER_BP),
                    List.of("Dathcha", "Darth Vader"),
                    true, 1);
        }

        private static Decision topLevelDeploy() {
            return new Decision(
                    "CARD_ACTION_CHOICE",
                    "Choose deploy action",
                    Phase.DEPLOY,
                    List.of("deploy-vader", "pass"),
                    List.of(
                            "Deploy <div class='cardHint' value='"
                                    + VADER_BP
                                    + "'>Darth Vader</div>",
                            "Pass"),
                    List.of(String.valueOf(VADER_ID), ""),
                    List.of(VADER_BP, ""),
                    List.of("Darth Vader", "Pass"),
                    false, 0);
        }

        private static Decision deployBudgetChoice() {
            return new Decision(
                    "CARD_ACTION_CHOICE",
                    "Choose deploy action",
                    Phase.DEPLOY,
                    List.of(
                            "deploy-costly",
                            "deploy-cheap",
                            "pass"),
                    List.of(
                            "Deploy Neutral Budget Character",
                            "Deploy Neutral Budget Character",
                            "Pass"),
                    List.of(
                            String.valueOf(COSTLY_DEPLOY_ID),
                            String.valueOf(CHEAP_DEPLOY_ID),
                            ""),
                    List.of("", "", ""),
                    List.of(
                            "Neutral Budget Character",
                            "Neutral Budget Character",
                            "Pass"),
                    false, 0);
        }

        private static Decision forceLoss() {
            return new Decision(
                    "CARD_SELECTION",
                    "Choose Force to lose",
                    Phase.CONTROL,
                    List.of(), List.of(),
                    List.of(
                            String.valueOf(VADER_ID),
                            String.valueOf(DISTRACTOR_ID)),
                    List.of(
                            VADER_BP,
                            DISTRACTOR_EFFECT_BP),
                    List.of(
                            blueprint(VADER_BP).getTitle(),
                            blueprint(DISTRACTOR_EFFECT_BP)
                                    .getTitle()),
                    true, 1);
        }

        private static Decision castleVaderReserveChoice() {
            return new Decision(
                    "CARD_SELECTION",
                    "Choose card to deploy from Reserve Deck",
                    Phase.DEPLOY,
                    List.of(), List.of(),
                    List.of(), List.of(
                        VADER_BP, SECOND_VADER_BP),
                    List.of(
                        blueprint(VADER_BP).getTitle(),
                        blueprint(SECOND_VADER_BP)
                            .getTitle()),
                    true, 1);
        }

        private static Decision forfeit() {
            return new Decision(
                    "CARD_SELECTION",
                    "Choose a card from battle to forfeit",
                    Phase.BATTLE,
                    List.of(), List.of(),
                    List.of(
                            String.valueOf(VADER_ID),
                            String.valueOf(DISTRACTOR_ID)),
                    List.of(
                            VADER_BP,
                            DISTRACTOR_BP),
                    List.of(
                            blueprint(VADER_BP).getTitle(),
                            blueprint(DISTRACTOR_BP).getTitle()),
                    true, 1);
        }

        private static Decision deployDestination() {
            return new Decision(
                    "CARD_SELECTION",
                    "Choose where to deploy "
                            + "<div class='cardHint' value='"
                            + VADER_BP + "'>Darth Vader</div>",
                    Phase.DEPLOY,
                    List.of(), List.of(),
                    List.of(
                            String.valueOf(BATTLEGROUND_ID),
                            String.valueOf(NON_BATTLEGROUND_ID)),
                    List.of(
                            BATTLEGROUND_BP,
                            NON_BATTLEGROUND_BP),
                    List.of(
                            blueprint(BATTLEGROUND_BP).getTitle(),
                            blueprint(NON_BATTLEGROUND_BP)
                                    .getTitle()),
                    true, 1);
        }

        private static Decision topLevelMove() {
            return new Decision(
                    "ACTION_CHOICE",
                    "Choose move action",
                    Phase.MOVE,
                    List.of("move-vader", "pass"),
                    List.of("Move using landspeed", "Pass"),
                    List.of(String.valueOf(VADER_ID), ""),
                    List.of(), List.of(),
                    false, 0);
        }

        private static Decision moveDestination() {
            return new Decision(
                    "CARD_SELECTION",
                    "Choose where to move "
                            + "<div class='cardHint' value='"
                            + VADER_BP
                            + "'>Darth Vader</div> using landspeed",
                    Phase.MOVE,
                    List.of(), List.of(),
                    List.of(
                            String.valueOf(BATTLEGROUND_ID),
                            String.valueOf(NON_BATTLEGROUND_ID)),
                    List.of(
                            BATTLEGROUND_BP,
                            NON_BATTLEGROUND_BP),
                    List.of(
                            blueprint(BATTLEGROUND_BP).getTitle(),
                            blueprint(NON_BATTLEGROUND_BP)
                                    .getTitle()),
                    true, 1);
        }

        private static Decision moveDestinationFromBattleground() {
            return new Decision(
                    "CARD_SELECTION",
                    "Choose where to move "
                            + "<div class='cardHint' value='"
                            + VADER_BP
                            + "'>Darth Vader</div> using landspeed",
                    Phase.MOVE,
                    List.of(), List.of(),
                    List.of(
                            String.valueOf(
                                    BLOCKER_BATTLEGROUND_ID),
                            String.valueOf(
                                    NON_BATTLEGROUND_ID)),
                    List.of(
                            BLOCKER_BATTLEGROUND_BP,
                            NON_BATTLEGROUND_BP),
                    List.of(
                            blueprint(BLOCKER_BATTLEGROUND_BP)
                                    .getTitle(),
                            blueprint(NON_BATTLEGROUND_BP)
                                    .getTitle()),
                    true, 1);
        }

        private static Decision recallTarget() {
            return new Decision(
                    "CARD_SELECTION",
                    "Choose Vader to take into hand",
                    Phase.MOVE,
                    List.of(), List.of(),
                    List.of(
                            String.valueOf(VADER_ID),
                            String.valueOf(SECOND_VADER_ID)),
                    List.of(VADER_BP, VADER_BP),
                    List.of("Darth Vader", "Darth Vader"),
                    true, 1);
        }

        private static Decision castleChoice(
                String text, List<String> cardIds,
                List<String> blueprints,
                List<String> testingTexts) {
            return new Decision(
                    "CARD_SELECTION", text, Phase.MOVE,
                    List.of(), List.of(), cardIds,
                    blueprints, testingTexts,
                    true, 1);
        }

        private static Decision castleTopLevelMove() {
            return new Decision(
                    "CARD_ACTION_CHOICE",
                    "Choose move action",
                    Phase.MOVE,
                    List.of("castle-move", "pass"),
                    List.of(
                        "Move from here to other battleground site",
                        "Pass"),
                    List.of(String.valueOf(CASTLE_ID), ""),
                    List.of(CASTLE_BP, ""),
                    List.of(
                        "Mustafar: Vader's Castle", "Pass"),
                    false, 0);
        }

        private static Decision dockingTransitTop(
                PhysicalCard source) {
            return new Decision(
                    "CARD_ACTION_CHOICE",
                    "Choose Move action or Pass",
                    Phase.MOVE,
                    List.of("0"),
                    List.of("Docking bay transit"),
                    List.of(
                        String.valueOf(source.getCardId())),
                    List.of("inPlay"), List.of(),
                    false, 0);
        }

        private static Decision dockingDestination(
                List<PhysicalCard> destinations) {
            return new Decision(
                    "CARD_SELECTION",
                    "Choose docking bay to transit to, "
                        + "or click 'Done' to cancel",
                    Phase.MOVE,
                    List.of(), List.of(),
                    destinations.stream()
                        .map(card -> String.valueOf(
                            card.getCardId()))
                        .toList(),
                    List.of(), List.of(),
                    true, 0);
        }

        private static Decision dockingMovers(
                PhysicalCard destination,
                List<PhysicalCard> movers) {
            return new Decision(
                    "CARD_SELECTION",
                    "Choose cards to docking bay transit to "
                        + GameUtils.getCardLink(destination)
                        + ", or click 'Done' to cancel",
                    Phase.MOVE,
                    List.of(), List.of(),
                    movers.stream()
                        .map(card -> String.valueOf(
                            card.getCardId()))
                        .toList(),
                    List.of(), List.of(),
                    true, 0);
        }

        private static Decision battle() {
            return new Decision(
                    "CARD_ACTION_CHOICE",
                    "Choose battle action",
                    Phase.BATTLE,
                    List.of("nonblocker", "blocker"),
                    List.of("Initiate battle", "Initiate battle"),
                    List.of(
                            String.valueOf(BATTLEGROUND_ID),
                            String.valueOf(BLOCKER_BATTLEGROUND_ID)),
                    List.of(), List.of(),
                    true, 1);
        }

        private int selectionCount() {
            return !cardIds.isEmpty()
                    ? cardIds.size() : blueprints.size();
        }

        private int maximum() {
            return text.toLowerCase(java.util.Locale.ROOT)
                    .startsWith(
                        "choose cards to docking bay transit to ")
                    ? Math.max(1, cardIds.size())
                    : 1;
        }

        private boolean emitsSelectable() {
            String lower =
                    text.toLowerCase(
                        java.util.Locale.ROOT);
            return !lower.contains(
                        "docking bay transit")
                    && !lower.contains(
                        "docking bay to transit")
                    && !actionTexts.contains(
                        "Docking bay transit");
        }
    }

    private static final class Fixture {
        private final ObjectiveAnalyzer analyzer;
        private final SwccgGame game;
        private final GameState gameState;
        private final ModifiersQuerying modifiers;
        private final List<PhysicalCard> hand;
        private final List<PhysicalCard> reserve;
        private final List<PhysicalCard> permanents;
        private final List<PhysicalCard> locations;
        private final Map<Integer, PhysicalCard> cardsById;
        private final Map<PhysicalCard, List<PhysicalCard>> cardsAt;
        private final PhysicalCard battleground;
        private final PhysicalCard nonBattleground;
        private final PhysicalCard blockerBattleground;
        private final PhysicalCard castle;

        private Fixture(
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
                PhysicalCard battleground,
                PhysicalCard nonBattleground,
                PhysicalCard blockerBattleground,
                PhysicalCard castle) {
            this.analyzer = analyzer;
            this.game = game;
            this.gameState = gameState;
            this.modifiers = modifiers;
            this.hand = hand;
            this.reserve = reserve;
            this.permanents = permanents;
            this.locations = locations;
            this.cardsById = cardsById;
            this.cardsAt = cardsAt;
            this.battleground = battleground;
            this.nonBattleground = nonBattleground;
            this.blockerBattleground =
                    blockerBattleground;
            this.castle = castle;
        }

        private PhysicalCard addDockingBay(
                String blueprintId, int cardId,
                boolean battlegroundValue) {
            PhysicalCard location = site(
                    blueprintId, cardId,
                    Zone.LOCATIONS);
            locations.add(location);
            permanents.add(location);
            register(location);
            when(modifiers.isBattleground(
                    gameState, location, null))
                    .thenReturn(battlegroundValue);
            when(modifiers.getLocationHere(
                    gameState, location))
                    .thenReturn(location);
            setActive(gameState, location);
            return location;
        }

        private void enableActiveIteration() {
            when(gameState.iterateActiveCards(
                    any(PhysicalCardVisitor.class),
                    any(ModifiersQuerying.class),
                    any(), any(), any()))
                    .thenAnswer(invocation -> {
                        PhysicalCardVisitor visitor =
                                invocation.getArgument(0);
                        for (PhysicalCard card
                                : new ArrayList<>(
                                    permanents)) {
                            if (visitor.visitPhysicalCard(card)) {
                                return true;
                            }
                        }
                        return false;
                    });
        }

        private void addHand(PhysicalCard card) {
            hand.add(card);
            register(card);
        }

        private void addReserve(PhysicalCard card) {
            reserve.add(card);
            register(card);
            if (BLOCKER_BATTLEGROUND_BP.equals(
                    card.getBlueprintId(true))) {
                when(modifiers.isBattleground(
                        gameState, card, null)).thenReturn(true);
                when(modifiers.hasIcon(
                        gameState, card, Icon.CLOUD_CITY))
                        .thenReturn(true);
            }
        }

        private void register(PhysicalCard card) {
            cardsById.put(card.getCardId(), card);
        }

        private void markPersona(
                PhysicalCard card, Persona persona) {
            when(modifiers.hasPersona(
                    gameState, card, persona)).thenReturn(true);
        }

        private void place(
                PhysicalCard card, PhysicalCard location) {
            register(card);
            if (!permanents.contains(card)) {
                permanents.add(card);
            }
            cardsAt.computeIfAbsent(
                    location, ignored -> new ArrayList<>())
                    .add(card);
            when(card.getAtLocation()).thenReturn(location);
            when(modifiers.getLocationThatCardIsPresentAt(
                    gameState, card)).thenReturn(location);
            when(modifiers.getLocationThatCardIsAt(
                    gameState, card)).thenReturn(location);
            when(modifiers.getLocationHere(
                    gameState, card)).thenReturn(location);
            setActive(gameState, card);
        }

        private void prepareCastleLocationText(
                PhysicalCard mover, float cost) {
            for (PhysicalCard destination : List.of(
                    battleground, blockerBattleground)) {
                when(modifiers.getMoveUsingLocationTextCost(
                        gameState, mover, castle, destination,
                        1.0f, 0.0f)).thenReturn(cost);
            }
        }

        private void prepareLandspeed(PhysicalCard mover) {
            prepareLandspeed(mover, nonBattleground);
        }

        private void prepareLandspeed(
                PhysicalCard mover, PhysicalCard origin) {
            when(modifiers.getLandspeed(
                    gameState, mover)).thenReturn(1.0f);
            for (PhysicalCard destination : List.of(
                    battleground,
                    nonBattleground,
                    blockerBattleground)) {
                int required =
                        modifiers.getDistanceBetweenSites(
                                gameState,
                                origin,
                                destination);
                when(modifiers.getLandspeedRequired(
                        gameState, mover, destination))
                        .thenReturn(required);
                when(modifiers.getMoveUsingLandspeedCost(
                        gameState, mover,
                        origin, destination,
                        false, 0.0f)).thenReturn(1.0f);
            }
        }

        private void setLiveAction(
                PhysicalCard source, String text) {
            GameTextActionState state =
                    mock(GameTextActionState.class);
            GameTextAction action =
                    mock(GameTextAction.class);
            when(action.getActionSource()).thenReturn(source);
            when(action.getText()).thenReturn(text);
            when(state.getGameTextAction()).thenReturn(action);
            when(gameState.getTopGameTextActionState())
                    .thenReturn(state);
        }

        private void equalSafeBattle(PhysicalCard location) {
            when(modifiers.getTotalPowerAtLocation(
                    gameState, location, PLAYER,
                    false, false)).thenReturn(30.0f);
            when(modifiers.getTotalPowerAtLocation(
                    gameState, location, OPPONENT,
                    false, false)).thenReturn(5.0f);
            when(modifiers.getTotalAbilityAtLocation(
                    gameState, PLAYER, location))
                    .thenReturn(4.0f);
            when(modifiers.getTotalAbilityAtLocation(
                    gameState, OPPONENT, location))
                    .thenReturn(4.0f);
        }
    }
}
