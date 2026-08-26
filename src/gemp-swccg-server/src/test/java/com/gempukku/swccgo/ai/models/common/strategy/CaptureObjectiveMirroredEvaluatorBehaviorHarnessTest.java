package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.ai.models.common.phase.BhbmForceDripUrgencyFactsReader;
import com.gempukku.swccgo.ai.models.common.phase.CaptureMovementMechanismFactsReader;
import com.gempukku.swccgo.ai.models.common.phase.MovePhysicalCardResolver;
import com.gempukku.swccgo.ai.models.common.trace.DecisionTrace;
import com.gempukku.swccgo.ai.models.common.trace.TraceSession;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.TargetingReason;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.PhysicalCardVisitor;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgCardBlueprintLibrary;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.BattleState;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.game.state.actions.GameTextActionState;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.actions.GameTextAction;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;
import com.gempukku.swccgo.logic.decisions.CardActionSelectionDecision;
import com.gempukku.swccgo.logic.decisions.DecisionResultInvalidException;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import com.gempukku.swccgo.logic.timing.Action;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.IdentityHashMap;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Production-adapter behavior proof for There Is Good In Him and Bring Him
 * Before Me. Every case executes both bots' public evaluator adapters against
 * real card-source blueprints and controlled engine state.
 */
public class CaptureObjectiveMirroredEvaluatorBehaviorHarnessTest {
    private static final String TIGIH = "9_61";
    private static final String BHBM = "9_151";
    private static final String INSIGNIFICANT_REBELLION = "9_127";
    private static final String VIRTUAL_INSIGNIFICANT_REBELLION =
            "210_047";
    private static final String YOUR_DESTINY = "9_134";
    private static final String VIRTUAL_HUT = "214_19";
    private static final String CLASSIC_HUT = "8_71";
    private static final String LANDING_PLATFORM = "8_76";
    private static final String THRONE_ROOM = "9_147";
    private static final String DSII_DOCKING_BAY = "9_145";
    private static final String DSII_CHASM_WALKWAY = "213_56";
    private static final String DSII_TURBOLIFT_WALKWAY = "218_18";
    private static final String TATOOINE_SYSTEM = "1_289";
    private static final String LUKE = "1_19";
    private static final String VADER = "1_168";
    private static final String EMPEROR = "9_109";
    private static final String IFTC = "9_34";
    private static final String IMPERIAL = "1_170";
    private static final String DISTRACTOR = "1_169";
    private static final String BANTHA = "1_307";
    private static final String BLIZZARD_ONE = "3_154";
    private static final String DEVASTATOR = "1_301";
    private static final String STAR_DESTROYER = "1_302";
    private static final String TATOOINE_JAWA_CAMP = "1_292";
    private static final String TATOOINE_JUNDLAND_WASTES = "1_293";
    private static final String OTHER_SITE = "1_290";
    private static final String YOUR_DESTINY_RULE =
            "OBJECTIVE.BHBM.YOUR_DESTINY_BATTLEGROUND";
    private static final String INSIGNIFICANT_REBELLION_RULE =
            "BATTLE.OBJECTIVE.BHBM.INSIGNIFICANT_REBELLION";

    private static final int OBJECTIVE_ID = 90;
    private static final int OPPONENT_OBJECTIVE_ID = 91;
    private static final int HUT_ID = 101;
    private static final int CLASSIC_HUT_ID = 102;
    private static final int LANDING_ID = 103;
    private static final int OTHER_SITE_ID = 104;
    private static final int THRONE_ID = 105;
    private static final int TATOOINE_SYSTEM_ID = 106;
    private static final int DSII_DOCKING_BAY_ID = 107;
    private static final int DSII_CHASM_WALKWAY_ID = 108;
    private static final int DSII_TURBOLIFT_WALKWAY_ID = 109;
    private static final int TATOOINE_JAWA_CAMP_ID = 110;
    private static final int TATOOINE_JUNDLAND_WASTES_ID = 111;
    private static final int LUKE_ID = 201;
    private static final int VADER_ID = 202;
    private static final int EMPEROR_ID = 203;
    private static final int IMPERIAL_ID = 204;
    private static final int DISTRACTOR_ID = 205;
    private static final int IFTC_ID = 206;
    private static final int VADER_DUPLICATE_ID = 207;
    private static final int LUKE_DUPLICATE_ID = 208;
    private static final int CARRIER_ID = 209;
    private static final int ENCLOSED_CARRIER_ID = 210;
    private static final int SHUTTLE_DESTINATION_ID = 211;
    private static final int TARGET_CARRIER_ID = 212;
    private static final int YOUR_DESTINY_ID = 213;
    private static final int INSIGNIFICANT_REBELLION_ID = 214;
    private static final int IMPERIAL_DUPLICATE_ID = 215;

    private static final SwccgCardBlueprintLibrary CARDS =
            new SwccgCardBlueprintLibrary();

    @Test
    public void setupPrefersVirtualHutOnlyWhenBothPrintingsSelectable() {
        List<Outcome> preferred = new ArrayList<>();
        List<Outcome> unopposed = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = new Fixture(bot, Kind.TIGIH, false);
            when(fixture.gameState.getPlayersLatestTurnNumber(
                    fixture.player)).thenReturn(0);

            Decision both = Decision.cards(
                    "Choose Chief Chirpa's Hut to deploy",
                    Phase.PLAY_STARTING_CARDS,
                    List.of(String.valueOf(HUT_ID),
                            String.valueOf(CLASSIC_HUT_ID)),
                    List.of(VIRTUAL_HUT, CLASSIC_HUT),
                    List.of(true, true));
            Outcome virtual = only(
                    cardSelectionAdapter(fixture, both),
                    String.valueOf(HUT_ID));
            Outcome classic = only(
                    cardSelectionAdapter(fixture, both),
                    String.valueOf(CLASSIC_HUT_ID));
            assertContains(virtual, "TIGIH SETUP");
            assertTrue(virtual.score() > classic.score());
            assertEquals(String.valueOf(HUT_ID),
                    combined(fixture, both).actionId());
            preferred.add(virtual);

            Decision onlyVirtual = Decision.cards(
                    "Choose Chief Chirpa's Hut to deploy",
                    Phase.PLAY_STARTING_CARDS,
                    List.of(String.valueOf(HUT_ID),
                            String.valueOf(CLASSIC_HUT_ID)),
                    List.of(VIRTUAL_HUT, CLASSIC_HUT),
                    List.of(true, false));
            Outcome neutral = only(
                    cardSelectionAdapter(fixture, onlyVirtual),
                    String.valueOf(HUT_ID));
            assertNotContains(neutral, "TIGIH SETUP");
            unopposed.add(neutral);
        }
        assertParity(preferred);
        assertParity(unopposed);
    }

    @Test
    public void exactOwnedVirtualHutSourceAloneGetsParentCaptureCredit() {
        List<Outcome> exacts = new ArrayList<>();
        List<Outcome> impostors = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = virtualHutFixture(bot);
            PhysicalCard classic = fixture.addLocation(
                    CLASSIC_HUT, CLASSIC_HUT_ID,
                    fixture.player);
            Decision decision = Decision.actions(
                    "Choose action", Phase.MOVE,
                    List.of("exact-hut", "classic-hut"),
                    List.of("Move Luke to Landing Platform",
                            "Move Luke to Landing Platform"),
                    List.of(String.valueOf(HUT_ID),
                            String.valueOf(CLASSIC_HUT_ID)),
                    true, 1);

            Outcome exact = only(
                    actionTextAdapter(fixture, decision),
                    "exact-hut");
            Outcome impostor = only(
                    actionTextAdapter(fixture, decision),
                    "classic-hut");
            assertEquals(CLASSIC_HUT,
                    classic.getBlueprintId(true));
            assertContains(exact, "CAPTURE ROUTE");
            assertNotContains(impostor, "CAPTURE ROUTE");
            assertTrue(exact.score() > impostor.score());
            exacts.add(exact);
            impostors.add(impostor);
        }
        assertParity(exacts);
        assertParity(impostors);
    }

    @Test
    public void virtualHutBindsOriginDestinationAndMoverChildren() {
        for (Bot bot : Bot.values()) {
            Fixture fixture = virtualHutFixture(bot);
            PhysicalCard other = fixture.addLocation(
                    OTHER_SITE, OTHER_SITE_ID,
                    fixture.player);
            PhysicalCard wrongMover = fixture.addActive(
                    DISTRACTOR, DISTRACTOR_ID,
                    fixture.player, other);
            fixture.setLiveAction(
                    fixture.card(HUT_ID),
                    "Move Luke to Landing Platform");

            assertChildChoice(fixture, Decision.cards(
                    "Choose card to move from", Phase.MOVE,
                    List.of(String.valueOf(HUT_ID),
                            String.valueOf(OTHER_SITE_ID)),
                    List.of(VIRTUAL_HUT, OTHER_SITE),
                    List.of(true, true)),
                    HUT_ID, OTHER_SITE_ID);
            assertChildChoice(fixture, Decision.cards(
                    "Choose card to move to", Phase.MOVE,
                    List.of(String.valueOf(LANDING_ID),
                            String.valueOf(OTHER_SITE_ID)),
                    List.of(LANDING_PLATFORM, OTHER_SITE),
                    List.of(true, true)),
                    LANDING_ID, OTHER_SITE_ID);
            assertChildChoice(fixture, Decision.cards(
                    "Choose card to move to "
                            + GameUtils.getCardLink(
                                fixture.card(LANDING_ID)),
                    Phase.MOVE,
                    List.of(String.valueOf(LUKE_ID),
                            String.valueOf(DISTRACTOR_ID)),
                    List.of(LUKE, DISTRACTOR),
                    List.of(true, true)),
                    LUKE_ID, DISTRACTOR_ID);
            assertNotNull(wrongMover);
        }
    }

    @Test
    public void ordinaryCaptureMoveAndDeployScoreParentAndDestination() {
        List<Outcome> moveParents = new ArrayList<>();
        List<Outcome> moveDestinations = new ArrayList<>();
        List<Outcome> deployParents = new ArrayList<>();
        List<Outcome> deployDestinations = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture move = ordinaryTigihMoveFixture(bot);
            Decision moveParent = Decision.actions(
                    "Choose move action", Phase.MOVE,
                    List.of("move-luke"),
                    List.of("Move using landspeed"),
                    List.of(String.valueOf(LUKE_ID)),
                    true, 1);
            Outcome moveAction = only(
                    moveAdapter(move, moveParent),
                    "move-luke");
            assertContains(moveAction, "CAPTURE ROUTE");
            moveParents.add(moveAction);

            Decision moveChild = Decision.cards(
                    "Choose where to move "
                            + "<div class='cardHint' value='"
                            + LUKE + "'>Luke Skywalker</div>"
                            + " using landspeed",
                    Phase.MOVE,
                    List.of(String.valueOf(LANDING_ID),
                            String.valueOf(OTHER_SITE_ID)),
                    List.of(LANDING_PLATFORM, OTHER_SITE),
                    List.of(true, true)).withMover(LUKE_ID);
            Outcome landing = only(
                    cardSelectionAdapter(move, moveChild),
                    String.valueOf(LANDING_ID));
            Outcome wrong = only(
                    cardSelectionAdapter(move, moveChild),
                    String.valueOf(OTHER_SITE_ID));
            assertContains(landing, "CAPTURE ROUTE");
            assertNotContains(wrong, "CAPTURE ROUTE");
            assertTrue(landing.score() > wrong.score());
            moveDestinations.add(landing);

            Fixture deploy = bhbmDeployFixture(bot);
            Decision deployParent = Decision.actions(
                    "Choose deploy action", Phase.DEPLOY,
                    List.of("deploy-vader"),
                    List.of("Deploy "
                            + deploy.card(VADER_ID).getTitle()
                            + " to "
                            + deploy.card(OTHER_SITE_ID).getTitle()),
                    List.of(String.valueOf(VADER_ID)),
                    true, 1);
            Outcome deployAction = only(
                    deployAdapter(deploy, deployParent),
                    "deploy-vader");
            assertContains(deployAction, "CAPTURE DEPLOY");
            deployParents.add(deployAction);

            Decision deployChild = Decision.cards(
                    "Choose where to deploy "
                            + "<div class='cardHint' value='"
                            + VADER + "'>Darth Vader</div>",
                    Phase.DEPLOY,
                    List.of(String.valueOf(OTHER_SITE_ID),
                            String.valueOf(THRONE_ID)),
                    List.of(OTHER_SITE, THRONE_ROOM),
                    List.of(true, true)).withDeploying(VADER_ID);
            Outcome targetSite = only(
                    cardSelectionAdapter(deploy, deployChild),
                    String.valueOf(OTHER_SITE_ID));
            Outcome throne = only(
                    cardSelectionAdapter(deploy, deployChild),
                    String.valueOf(THRONE_ID));
            assertContains(targetSite, "CAPTURE DEPLOY");
            assertNotContains(throne, "CAPTURE DEPLOY");
            assertTrue(targetSite.score() > throne.score());
            deployDestinations.add(targetSite);
        }
        assertParity(moveParents);
        assertParity(moveDestinations);
        assertParity(deployParents);
        assertParity(deployDestinations);
    }

    @Test
    public void deployParentClaimsCaptureOnlyForFormationSafeDestination() {
        List<Outcome> blockedParents = new ArrayList<>();
        List<Outcome> safeParents = new ArrayList<>();
        List<Outcome> winners = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = bhbmDeployFixture(bot);
            PhysicalCard blockedVader =
                    fixture.card(VADER_ID);
            PhysicalCard safeVader =
                    fixture.addHand(
                        VADER, VADER_DUPLICATE_ID);
            fixture.markPersona(
                    safeVader, Persona.VADER);
            fixture.setPrintedAbility(
                    blockedVader, 3.0f);
            fixture.setPrintedAbility(
                    safeVader, 4.0f);
            Decision decision =
                    captureDeployParentsOrPass(fixture);
            Outcome blockedParent = only(
                    deployAdapter(fixture, decision),
                    "blocked-deploy");
            Outcome safeParent = only(
                    deployAdapter(fixture, decision),
                    "safe-deploy");
            Outcome winner =
                    combined(fixture, decision);
            assertNotContains(blockedParent,
                    "CAPTURE DEPLOY");
            assertContains(safeParent,
                    "CAPTURE DEPLOY");
            assertTrue(safeParent.score()
                    > blockedParent.score());
            assertEquals("safe-deploy",
                    winner.actionId());
            blockedParents.add(blockedParent);
            safeParents.add(safeParent);
            winners.add(winner);
        }
        assertParity(blockedParents);
        assertParity(safeParents);
        assertParity(winners);
    }

    @Test
    public void enclosedEmbarkAndDifferentShipShuttleCannotClaimCapture() {
        List<Outcome> embarkParents = new ArrayList<>();
        List<Outcome> embarkWinners = new ArrayList<>();
        List<Outcome> shuttleParents = new ArrayList<>();
        List<Outcome> shuttleWinners = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture embark =
                    enclosedEmbarkFixture(bot);
            Decision embarkDecision =
                    specialMoveParentOrPass("Embark");
            Outcome embarkParent = only(
                    captureMovementAdapter(
                        embark, embarkDecision),
                    "special-move");
            Outcome embarkWinner =
                    combined(embark, embarkDecision);
            assertNotContains(embarkParent,
                    "CAPTURE ROUTE");
            assertNotContains(embarkParent,
                    "ACTOR_ROUTE");
            assertEquals("",
                    embarkWinner.actionId());
            embarkParents.add(embarkParent);
            embarkWinners.add(embarkWinner);

            Fixture shuttle =
                    differentShipShuttleFixture(bot);
            Decision shuttleDecision =
                    specialMoveParentOrPass("Shuttle");
            Outcome shuttleParent = only(
                    captureMovementAdapter(
                        shuttle, shuttleDecision),
                    "special-move");
            Outcome shuttleWinner =
                    combined(shuttle, shuttleDecision);
            assertNotContains(shuttleParent,
                    "CAPTURE ROUTE");
            assertNotContains(shuttleParent,
                    "ACTOR_ROUTE");
            assertEquals("",
                    shuttleWinner.actionId());
            shuttleParents.add(shuttleParent);
            shuttleWinners.add(shuttleWinner);
        }
        assertParity(embarkParents);
        assertParity(embarkWinners);
        assertParity(shuttleParents);
        assertParity(shuttleWinners);
    }

    @Test
    public void exactActiveYourDestinyAddsBoundedVaderDeployPreference() {
        List<Outcome> exactWinners = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture exact =
                    bhbmYourDestinyDeployFixture(
                        bot, YOUR_DESTINY, true);
            PhysicalCard vader =
                    exact.card(VADER_ID);
            PhysicalCard battleground =
                    exact.card(OTHER_SITE_ID);
            assertTrue(
                    com.gempukku.swccgo.ai.models
                        .common.phase
                        .BhbmSetupPayoffFactsReader
                        .rewardsVaderForDeployAt(
                            exact.game, exact.player,
                            exact.analyzer, vader,
                            battleground));
            assertTrue(
                    "Open Bantha keeps Vader present at the battleground",
                    com.gempukku.swccgo.ai.models
                        .common.phase
                        .BhbmSetupPayoffFactsReader
                        .rewardsVaderAtBattleground(
                            exact.game, exact.player,
                            exact.analyzer, vader,
                            exact.card(CARRIER_ID)));
            assertFalse(
                    "Enclosed Blizzard 1 does not",
                    com.gempukku.swccgo.ai.models
                        .common.phase
                        .BhbmSetupPayoffFactsReader
                        .rewardsVaderAtBattleground(
                            exact.game, exact.player,
                            exact.analyzer, vader,
                            exact.card(
                                ENCLOSED_CARRIER_ID)));

            Decision decision =
                    yourDestinyDeployParentDecision(
                        exact);
            Outcome exactVader = only(
                    deployAdapter(exact, decision),
                    "deploy-vader");
            TracedOutcome exactResult =
                    tracedCombined(exact, decision);
            assertEquals(
                    "Normal deploy scoring may override the bounded"
                        + " objective preference",
                    "deploy-neutral",
                    exactResult.outcome().actionId());
            assertContains(exactVader,
                    "BHBM YOUR DESTINY");
            assertTypedContribution(
                    exactResult.trace(),
                    "deploy-vader",
                    YOUR_DESTINY_RULE,
                    1, 300.0f);
            assertNoTypedRule(
                    exactResult.trace(),
                    "deploy-neutral",
                    YOUR_DESTINY_RULE);
            exactWinners.add(
                    exactResult.outcome());

            Fixture inactive =
                    bhbmYourDestinyDeployFixture(
                        bot, YOUR_DESTINY, false);
            Decision inactiveDecision =
                    yourDestinyDeployParentDecision(
                        inactive);
            Outcome inactiveVader = only(
                    deployAdapter(
                        inactive, inactiveDecision),
                    "deploy-vader");
            assertEquals(
                    "Your Destiny must share the same +300 ceiling with"
                        + " the generic objective-key-character match",
                    inactiveVader.score(),
                    exactVader.score(), 0.0f);
            TracedOutcome inactiveResult =
                    tracedCombined(
                        inactive,
                        inactiveDecision);
            assertNoTypedRule(
                    inactiveResult.trace(),
                    "deploy-vader",
                    YOUR_DESTINY_RULE);
            assertTrue(
                    "Without active Your Destiny, the "
                        + "same-board Vader deploy must "
                        + "lose to neutral deploy or Pass",
                    List.of("deploy-neutral", "pass")
                        .contains(
                            inactiveResult.outcome()
                                .actionId()));

            Fixture wrong =
                    bhbmYourDestinyDeployFixture(
                        bot, IFTC, true);
            TracedOutcome wrongResult =
                    tracedCombined(
                        wrong,
                        yourDestinyDeployParentDecision(
                            wrong));
            assertNoTypedRule(
                    wrongResult.trace(),
                    "deploy-vader",
                    YOUR_DESTINY_RULE);
            assertTrue(
                    "A wrong payoff source must not make "
                        + "the Vader deploy win",
                    List.of("deploy-neutral", "pass")
                        .contains(
                            wrongResult.outcome()
                                .actionId()));

            Decision carrierDecision =
                    yourDestinyDeployParentToCarrierDecision(
                        exact);
            TracedOutcome carrierExact =
                    tracedCombined(
                        exact, carrierDecision);
            assertTypedContribution(
                    carrierExact.trace(),
                    "deploy-vader-carrier",
                    YOUR_DESTINY_RULE,
                    1, 300.0f);
            Outcome exactCarrierVader = only(
                    deployAdapter(
                        exact, carrierDecision),
                    "deploy-vader-carrier");

            Decision inactiveCarrierDecision =
                    yourDestinyDeployParentToCarrierDecision(
                        inactive);
            TracedOutcome carrierInactive =
                    tracedCombined(
                        inactive,
                        inactiveCarrierDecision);
            assertNoTypedRule(
                    carrierInactive.trace(),
                    "deploy-vader-carrier",
                    YOUR_DESTINY_RULE);
            assertTrue(
                    "Inactive Your Destiny must make the "
                        + "same-board open-carrier Vader "
                        + "deploy lose",
                    List.of(
                            "deploy-neutral-carrier",
                            "pass")
                        .contains(
                            carrierInactive.outcome()
                                .actionId()));
            Outcome inactiveCarrierVader = only(
                    deployAdapter(
                        inactive,
                        inactiveCarrierDecision),
                    "deploy-vader-carrier");
            assertEquals(
                    "Your Destiny must share the same +300 ceiling with"
                        + " the generic objective-key-character match",
                    inactiveCarrierVader.score(),
                    exactCarrierVader.score(), 0.0f);
        }
        assertParity(exactWinners);
    }

    @Test
    public void hostileSiteAndOpenCarrierDeployChildrenDoNotArmYourDestiny() {
        List<Outcome> children =
                new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture hostile =
                    bhbmYourDestinyDeployFixture(
                        bot, YOUR_DESTINY, true);
            PhysicalCard battleground =
                    hostile.card(OTHER_SITE_ID);
            hostile.addActive(
                    DISTRACTOR,
                    DISTRACTOR_ID,
                    hostile.opponent,
                    battleground);
            hostile.setBattle(
                    battleground,
                    0.0f, 20.0f,
                    0.0f, 4.0f,
                    hostile.cardsAt(battleground));
            assertFalse(
                    "The exact open-vehicle deploy target "
                        + "must retain character formation "
                        + "safety",
                    com.gempukku.swccgo.ai.models
                        .common.phase
                        .BhbmSetupPayoffFactsReader
                        .rewardsVaderForDeployAt(
                            hostile.game,
                            hostile.player,
                            hostile.analyzer,
                            hostile.card(VADER_ID),
                            hostile.card(CARRIER_ID)));
            assertFalse(
                    "The direct battleground deploy target "
                        + "must retain character formation "
                        + "safety",
                    com.gempukku.swccgo.ai.models
                        .common.phase
                        .BhbmSetupPayoffFactsReader
                        .rewardsVaderForDeployAt(
                            hostile.game,
                            hostile.player,
                            hostile.analyzer,
                            hostile.card(VADER_ID),
                            battleground));

            Decision decision = Decision.cards(
                    "Choose where to deploy "
                        + "<div class='cardHint' value='"
                        + VADER
                        + "'>Darth Vader</div>",
                    Phase.DEPLOY,
                    List.of(
                        String.valueOf(CARRIER_ID),
                        String.valueOf(
                            OTHER_SITE_ID)),
                    List.of(BANTHA, OTHER_SITE),
                    List.of(true, true))
                    .withDeploying(VADER_ID);
            TracedOutcome result =
                    tracedCombined(hostile, decision);
            assertNoTypedRule(
                    result.trace(),
                    String.valueOf(CARRIER_ID),
                    YOUR_DESTINY_RULE);
            assertNoTypedRule(
                    result.trace(),
                    String.valueOf(OTHER_SITE_ID),
                    YOUR_DESTINY_RULE);
            assertNotContains(
                    result.outcome(),
                    "BHBM YOUR DESTINY");
            children.add(result.outcome());
        }
        assertParity(children);
    }

    @Test
    public void openCarrierDeployGetsOneBoundedCapturePreference() {
        List<Outcome> parents = new ArrayList<>();
        List<Outcome> destinations = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture =
                    bhbmYourDestinyDeployFixture(
                        bot, YOUR_DESTINY, false);
            PhysicalCard battleground =
                    fixture.card(OTHER_SITE_ID);
            PhysicalCard luke = fixture.addActive(
                    LUKE, LUKE_ID,
                    fixture.opponent, battleground);
            fixture.markPersona(luke, Persona.LUKE);
            fixture.allowTargetByObjective(luke);

            Decision parent =
                    yourDestinyDeployParentToCarrierDecision(
                        fixture);
            Outcome parentVader = only(
                    deployAdapter(fixture, parent),
                    "deploy-vader-carrier");
            TracedOutcome parentResult =
                    tracedCombined(fixture, parent);
            assertContains(parentVader,
                    "CAPTURE DEPLOY");
            assertFalse(parentVader.hardVeto());
            assertTypedContribution(
                    parentResult.trace(),
                    "deploy-vader-carrier",
                    "DEPLOY.OBJECTIVE.CAPTURE_ROUTE_PARENT",
                    1, 300.0f);
            parents.add(parentResult.outcome());

            Decision child = Decision.cards(
                    "Choose where to deploy "
                        + "<div class='cardHint' value='"
                        + VADER
                        + "'>Darth Vader</div>",
                    Phase.DEPLOY,
                    List.of(
                        String.valueOf(CARRIER_ID),
                        String.valueOf(THRONE_ID)),
                    List.of(BANTHA, THRONE_ROOM),
                    List.of(true, true))
                    .withDeploying(VADER_ID);
            Outcome carrier = only(
                    cardSelectionAdapter(
                        fixture, child),
                    String.valueOf(CARRIER_ID));
            Outcome throne = only(
                    cardSelectionAdapter(
                        fixture, child),
                    String.valueOf(THRONE_ID));
            TracedOutcome childResult =
                    tracedCombined(fixture, child);
            assertContains(carrier,
                    "CAPTURE DEPLOY");
            assertNotContains(throne,
                    "CAPTURE DEPLOY");
            assertFalse(carrier.hardVeto());
            assertTypedContribution(
                    childResult.trace(),
                    String.valueOf(CARRIER_ID),
                    "DEPLOY.OBJECTIVE.CAPTURE_ROUTE_DESTINATION",
                    1, 300.0f);
            destinations.add(childResult.outcome());
        }
        assertParity(parents);
        assertParity(destinations);
    }

    @Test
    public void yourDestinyMoveParentRewardsOnlyArmingTheClock() {
        List<Outcome> armedWinners =
                new ArrayList<>();
        List<Outcome> alreadyLiveWinners =
                new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture unarmed =
                    bhbmYourDestinyMoveFixture(
                        bot, false);
            Decision move =
                    specialMoveParentOrPass(
                        "Move using landspeed");
            TracedOutcome armed =
                    tracedCombined(unarmed, move);
            assertEquals("special-move",
                    armed.outcome().actionId());
            assertContains(armed.outcome(),
                    "BHBM YOUR DESTINY");
            assertTypedContribution(
                    armed.trace(), "special-move",
                    YOUR_DESTINY_RULE,
                    1, 300.0f);
            armedWinners.add(armed.outcome());

            Fixture alreadyLive =
                    bhbmYourDestinyMoveFixture(
                        bot, true);
            TracedOutcome preserved =
                    tracedCombined(
                        alreadyLive,
                        specialMoveParentOrPass(
                            "Move using landspeed"));
            assertNoTypedRule(
                    preserved.trace(),
                    "special-move",
                    YOUR_DESTINY_RULE);
            assertEquals("",
                    preserved.outcome().actionId());
            assertNotContains(
                    preserved.outcome(),
                    "BHBM YOUR DESTINY");
            alreadyLiveWinners.add(
                    preserved.outcome());
        }
        assertParity(armedWinners);
        assertParity(alreadyLiveWinners);
    }

    @Test
    public void openCarrierArmsYourDestinyButEnclosedCarrierDoesNot() {
        List<Outcome> openWinners =
                new ArrayList<>();
        List<Outcome> enclosedWinners =
                new ArrayList<>();
        List<Outcome> hostileWinners =
                new ArrayList<>();
        List<Outcome> groupedWinners =
                new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture open =
                    bhbmYourDestinyCarrierMoveFixture(
                        bot, false, true);
            PhysicalCard openCarrier =
                    open.card(CARRIER_ID);
            PhysicalCard openOrigin =
                    open.card(
                        TATOOINE_JAWA_CAMP_ID);
            PhysicalCard openDestination =
                    open.card(
                        TATOOINE_JUNDLAND_WASTES_ID);
            assertTrue(
                    com.gempukku.swccgo.ai.models
                        .common.phase
                        .BhbmSetupPayoffFactsReader
                        .rewardsVaderAtBattleground(
                            open.game, open.player,
                            open.analyzer,
                            openCarrier,
                            openDestination));
            assertEquals(openOrigin,
                    openCarrier.getAtLocation());
            assertEquals(
                    com.gempukku.swccgo.common
                        .CardSubtype.SITE,
                    openOrigin.getBlueprint()
                        .getCardSubtype());
            assertEquals(
                    com.gempukku.swccgo.common
                        .CardSubtype.SITE,
                    openDestination.getBlueprint()
                        .getCardSubtype());
            assertFalse(
                    com.gempukku.swccgo.filters
                        .Filters.sameCardId(
                            openOrigin)
                        .accepts(
                            open.gameState,
                            open.modifiers,
                            openDestination));
            assertTrue(
                    com.gempukku.swccgo.filters
                        .Filters.vehicle.accepts(
                            open.gameState,
                            open.modifiers,
                            openCarrier));
            assertTrue(
                    open.modifiers.isPiloted(
                        open.gameState,
                        openCarrier, false));
            assertEquals(
                    Integer.valueOf(1),
                    open.modifiers
                        .getLandspeedRequired(
                            open.gameState,
                            openCarrier,
                            openDestination));
            assertEquals(
                    1.0f,
                    open.modifiers.getLandspeed(
                        open.gameState,
                        openCarrier),
                    0.0f);
            assertFalse(
                    open.modifiers
                        .mayNotMoveFromLocationToLocationUsingLandspeed(
                            open.gameState,
                            openCarrier,
                            openOrigin,
                            openDestination,
                            false));
            assertTrue(
                    open.modifiers
                        .getForceAvailableToUse(
                            open.gameState,
                            open.player)
                        >= open.modifiers
                            .getMoveUsingLandspeedCost(
                                open.gameState,
                                openCarrier,
                                openOrigin,
                                openDestination,
                                false, 0.0f));
            assertTrue(
                    "The synthetic stock move must pass "
                        + "the engine landspeed filter",
                    com.gempukku.swccgo.filters
                        .Filters
                        .canMoveToUsingLandspeed(
                            open.player,
                            openCarrier,
                            false, false,
                            false, 0.0f, null)
                        .accepts(
                            open.gameState,
                            open.modifiers,
                            openDestination));
            CaptureMovementMechanismFactsReader.Assessment
                    openAssessment =
                        CaptureMovementMechanismFactsReader
                            .assess(
                                open.game,
                                open.player,
                                open.analyzer,
                                CaptureMovementMechanismFactsReader
                                    .Mechanism.LANDSPEED,
                                openCarrier,
                                "Move using landspeed");
            assertTrue(openAssessment.factsKnown());
            assertTrue(
                    "The exact piloted open Bantha must "
                        + "have an admissible engine-legal "
                        + "landspeed route to the "
                        + "battleground; routes="
                        + openAssessment.routes(),
                    openAssessment.routes().stream()
                        .anyMatch(route ->
                            route.admissible()
                                && route.mover()
                                    == openCarrier
                                && route.destination()
                                    == openDestination));
            Decision openMove =
                    moveParentOrPass(CARRIER_ID);
            TracedOutcome openResult =
                    tracedCombined(open, openMove);
            assertTypedContribution(
                    openResult.trace(),
                    "move-parent",
                    YOUR_DESTINY_RULE,
                    1, 300.0f);
            assertEquals("move-parent",
                    openResult.outcome().actionId());
            assertContains(openResult.outcome(),
                    "BHBM YOUR DESTINY");
            openWinners.add(openResult.outcome());

            Fixture inactiveOpen =
                    bhbmYourDestinyCarrierMoveFixture(
                        bot, false, false);
            Decision inactiveOpenMove =
                    moveParentOrPass(CARRIER_ID);
            TracedOutcome inactiveOpenResult =
                    tracedCombined(
                        inactiveOpen,
                        inactiveOpenMove);
            assertNoTypedRule(
                    inactiveOpenResult.trace(),
                    "move-parent",
                    YOUR_DESTINY_RULE);
            assertEquals("",
                    inactiveOpenResult.outcome()
                        .actionId());
            Outcome openParent = only(
                    captureMovementAdapter(
                        open, openMove),
                    "move-parent");
            Outcome inactiveOpenParent = only(
                    captureMovementAdapter(
                        inactiveOpen,
                        inactiveOpenMove),
                    "move-parent");
            assertEquals(
                    "Your Destiny must contribute exactly "
                        + "300 to the same-board open "
                        + "carrier move parent",
                    inactiveOpenParent.score()
                        + 300.0f,
                    openParent.score(), 0.0f);

            Fixture grouped =
                    bhbmYourDestinyCarrierMoveFixture(
                        bot, false, true);
            PhysicalCard groupedCarrier =
                    grouped.card(CARRIER_ID);
            PhysicalCard groupedVader =
                    grouped.card(VADER_ID);
            PhysicalCard extraPassenger =
                    grouped.addActive(
                        IMPERIAL,
                        DISTRACTOR_ID,
                        grouped.player,
                        grouped.card(
                            TATOOINE_JAWA_CAMP_ID));
            grouped.aboard(
                    extraPassenger,
                    groupedCarrier,
                    grouped.card(
                        TATOOINE_JAWA_CAMP_ID),
                    groupedCarrier,
                    grouped.card(
                        TATOOINE_JAWA_CAMP_ID));
            grouped.allowAboardGroup(
                    groupedCarrier,
                    groupedVader,
                    extraPassenger);
            assertFalse(
                    "Open-carrier credit must fail "
                        + "neutral when another active "
                        + "friendly character is aboard",
                    com.gempukku.swccgo.ai.models
                        .common.phase
                        .BhbmSetupPayoffFactsReader
                        .projectedVaderMoveFormationSafe(
                            grouped.game,
                            grouped.player,
                            groupedCarrier,
                            grouped.card(
                                TATOOINE_JUNDLAND_WASTES_ID)));
            Decision groupedMove =
                    moveParentOrPass(CARRIER_ID);
            TracedOutcome groupedResult =
                    tracedCombined(
                        grouped, groupedMove);
            assertNoTypedRule(
                    groupedResult.trace(),
                    "move-parent",
                    YOUR_DESTINY_RULE);
            assertEquals("",
                    groupedResult.outcome()
                        .actionId());
            Outcome groupedParent = only(
                    captureMovementAdapter(
                        grouped, groupedMove),
                    "move-parent");
            assertEquals(
                    "The sole-Vader open-carrier proof "
                        + "must account for exactly the "
                        + "300-point payoff difference",
                    groupedParent.score() + 300.0f,
                    openParent.score(), 0.0f);
            groupedWinners.add(
                    groupedResult.outcome());

            Fixture enclosed =
                    bhbmYourDestinyCarrierMoveFixture(
                        bot, true, true);
            Decision enclosedMove =
                    moveParentOrPass(
                        ENCLOSED_CARRIER_ID);
            TracedOutcome enclosedResult =
                    tracedCombined(
                        enclosed, enclosedMove);
            assertNoTypedRule(
                    enclosedResult.trace(),
                    "move-parent",
                    YOUR_DESTINY_RULE);
            assertEquals("",
                    enclosedResult.outcome()
                        .actionId());
            enclosedWinners.add(
                    enclosedResult.outcome());

            Fixture hostile =
                    bhbmYourDestinyCarrierMoveFixture(
                        bot, false, true, true);
            assertFalse(
                    "Vader aboard an open Bantha must "
                        + "inherit character formation "
                        + "safety at the hostile destination",
                    com.gempukku.swccgo.ai.models
                        .common.phase
                        .BhbmSetupPayoffFactsReader
                        .projectedVaderMoveFormationSafe(
                            hostile.game,
                            hostile.player,
                            hostile.card(CARRIER_ID),
                            hostile.card(
                                TATOOINE_JUNDLAND_WASTES_ID)));
            TracedOutcome hostileParent =
                    tracedCombined(
                        hostile,
                        moveParentOrPass(CARRIER_ID));
            assertNoTypedRule(
                    hostileParent.trace(),
                    "move-parent",
                    YOUR_DESTINY_RULE);
            assertEquals("",
                    hostileParent.outcome()
                        .actionId());
            hostileWinners.add(
                    hostileParent.outcome());

            Decision hostileChild =
                    landspeedDestinationDecision(
                        hostile, CARRIER_ID,
                        TATOOINE_JUNDLAND_WASTES_ID);
            TracedOutcome hostileDestination =
                    tracedCombined(
                        hostile, hostileChild);
            assertNoTypedRule(
                    hostileDestination.trace(),
                    String.valueOf(
                        TATOOINE_JUNDLAND_WASTES_ID),
                    YOUR_DESTINY_RULE);
            assertNotContains(
                    hostileDestination.outcome(),
                    "BHBM YOUR DESTINY");
        }
        assertParity(openWinners);
        assertParity(enclosedWinners);
        assertParity(hostileWinners);
        assertParity(groupedWinners);
    }

    @Test
    public void openCarrierWithVaderGetsOneBoundedCaptureRoutePreference() {
        List<Outcome> parentWinners =
                new ArrayList<>();
        List<Outcome> childWinners =
                new ArrayList<>();
        List<Outcome> enclosedWinners =
                new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture open =
                    bhbmYourDestinyCarrierMoveFixture(
                        bot, false, false);
            PhysicalCard destination =
                    open.card(
                        TATOOINE_JUNDLAND_WASTES_ID);
            PhysicalCard luke = open.addActive(
                    LUKE, LUKE_ID,
                    open.opponent, destination);
            open.markPersona(luke, Persona.LUKE);
            open.allowTargetByObjective(luke);

            CaptureMovementMechanismFactsReader.Assessment
                    assessment =
                        CaptureMovementMechanismFactsReader
                            .assess(
                                open.game,
                                open.player,
                                open.analyzer,
                                CaptureMovementMechanismFactsReader
                                    .Mechanism.LANDSPEED,
                                open.card(CARRIER_ID),
                                "Move using landspeed");
            assertTrue(assessment
                    .hasAdmissibleCaptureRoute());

            Decision parent =
                    moveParentOrPass(CARRIER_ID);
            Outcome parentRoute = only(
                    moveAdapter(
                        open, parent),
                    "move-parent");
            TracedOutcome parentResult =
                    tracedCombined(open, parent);
            assertContains(parentRoute,
                    "CAPTURE ROUTE");
            assertFalse(parentRoute.hardVeto());
            assertTypedContribution(
                    parentResult.trace(),
                    "move-parent",
                    "MOVE.OBJECTIVE.CAPTURE_ROUTE_PARENT",
                    1, 300.0f);
            parentWinners.add(parentResult.outcome());

            Decision child =
                    landspeedDestinationDecision(
                        open, CARRIER_ID,
                        TATOOINE_JUNDLAND_WASTES_ID);
            Outcome childRoute = only(
                    cardSelectionAdapter(
                        open, child),
                    String.valueOf(
                        TATOOINE_JUNDLAND_WASTES_ID));
            TracedOutcome childResult =
                    tracedCombined(open, child);
            assertContains(childRoute,
                    "CAPTURE ROUTE");
            assertFalse(childRoute.hardVeto());
            assertTypedContribution(
                    childResult.trace(),
                    String.valueOf(
                        TATOOINE_JUNDLAND_WASTES_ID),
                    "MOVE.OBJECTIVE.CAPTURE_ROUTE_DESTINATION",
                    1, 300.0f);
            childWinners.add(childResult.outcome());

            Fixture enclosed =
                    bhbmYourDestinyCarrierMoveFixture(
                        bot, true, false);
            PhysicalCard enclosedDestination =
                    enclosed.card(
                        TATOOINE_JUNDLAND_WASTES_ID);
            PhysicalCard enclosedLuke =
                    enclosed.addActive(
                        LUKE, LUKE_ID,
                        enclosed.opponent,
                        enclosedDestination);
            enclosed.markPersona(
                    enclosedLuke, Persona.LUKE);
            enclosed.allowTargetByObjective(
                    enclosedLuke);
            Decision enclosedParent =
                    moveParentOrPass(
                        ENCLOSED_CARRIER_ID);
            Outcome enclosedRoute = only(
                    moveAdapter(
                        enclosed,
                        enclosedParent),
                    "move-parent");
            Outcome enclosedWinner =
                    combined(
                        enclosed,
                        enclosedParent);
            assertNotContains(enclosedRoute,
                    "CAPTURE ROUTE");
            enclosedWinners.add(
                    enclosedWinner);
        }
        assertParity(parentWinners);
        assertParity(childWinners);
        assertParity(enclosedWinners);
    }

    @Test
    public void disembarkDestinationHasOneYourDestinyOwnerInCombined() {
        List<Outcome> winners =
                new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture =
                    bhbmYourDestinyDisembarkFixture(
                        bot);
            Decision decision =
                    disembarkDestinationDecision();
            TracedOutcome result =
                    tracedCombined(fixture, decision);
            assertEquals(
                    String.valueOf(OTHER_SITE_ID),
                    result.outcome().actionId());
            assertTypedContribution(
                    result.trace(),
                    String.valueOf(OTHER_SITE_ID),
                    YOUR_DESTINY_RULE,
                    1, 300.0f);
            assertReasoningCount(
                    result.outcome(),
                    "BHBM YOUR DESTINY", 1);
            winners.add(result.outcome());
        }
        assertParity(winners);
    }

    @Test
    public void enclosedDisembarkIntoOverwhelmingEnemyDoesNotArmYourDestiny() {
        List<Outcome> parentWinners =
                new ArrayList<>();
        List<Outcome> childWinners =
                new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture hostile =
                    bhbmYourDestinyDisembarkFixture(
                        bot, true);
            assertFalse(
                    "Disembarking changes Vader from "
                        + "enclosed to present even when "
                        + "the physical site is unchanged",
                    com.gempukku.swccgo.ai.models
                        .common.phase
                        .BhbmSetupPayoffFactsReader
                        .projectedVaderMoveFormationSafe(
                            hostile.game,
                            hostile.player,
                            hostile.card(VADER_ID),
                            hostile.card(
                                OTHER_SITE_ID)));
            CaptureMovementMechanismFactsReader.Assessment
                    captureAssessment =
                        CaptureMovementMechanismFactsReader
                            .assess(
                                hostile.game,
                                hostile.player,
                                hostile.analyzer,
                                CaptureMovementMechanismFactsReader
                                    .Mechanism.DISEMBARK,
                                hostile.card(VADER_ID),
                                "Disembark");
            CaptureMovementMechanismFactsReader.Route
                    captureRoute =
                        captureAssessment.routes()
                            .stream()
                            .filter(route ->
                                route.destination()
                                    == hostile.card(
                                        OTHER_SITE_ID))
                            .findFirst()
                            .orElseThrow();
            assertTrue(
                    captureRoute
                        .guaranteesImmediateCapture());
            assertTrue(captureRoute.formationBlocked());
            assertFalse(
                    captureAssessment
                        .hasAdmissibleCaptureRoute());

            TracedOutcome parent =
                    tracedCombined(
                        hostile,
                        specialMoveParentOrPass(
                            "Disembark"));
            assertNoTypedRule(
                    parent.trace(),
                    "special-move",
                    YOUR_DESTINY_RULE);
            assertNotContains(
                    parent.outcome(),
                    "CAPTURE ROUTE");
            assertEquals("",
                    parent.outcome().actionId());
            parentWinners.add(parent.outcome());

            TracedOutcome child =
                    tracedCombined(
                        hostile,
                        disembarkDestinationDecision());
            assertNoTypedRule(
                    child.trace(),
                    String.valueOf(OTHER_SITE_ID),
                    YOUR_DESTINY_RULE);
            assertNotContains(
                    child.outcome(),
                    "BHBM YOUR DESTINY");
            childWinners.add(child.outcome());
        }
        assertParity(parentWinners);
        assertParity(childWinners);
    }

    @Test
    public void exactActiveInsignificantRebellionMakesSafeBattleWin() {
        List<Outcome> exactWinners =
                new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture exact =
                    bhbmInsignificantBattleFixture(
                        bot,
                        INSIGNIFICANT_REBELLION,
                        true);
            Decision exactDecision =
                    battleOrPassAt(
                        exact, OTHER_SITE_ID);
            Outcome exactBattle = only(
                    battleAdapter(
                        exact, exactDecision),
                    "battle");
            TracedOutcome exactResult =
                    tracedCombined(
                        exact, exactDecision);
            assertEquals("battle",
                    exactResult.outcome().actionId());
            assertContains(
                    exactResult.outcome(),
                    "BHBM INSIGNIFICANT REBELLION");
            assertTypedContribution(
                    exactResult.trace(),
                    "battle",
                    INSIGNIFICANT_REBELLION_RULE,
                    1, 300.0f);
            exactWinners.add(
                    exactResult.outcome());

            Fixture inactive =
                    bhbmInsignificantBattleFixture(
                        bot,
                        INSIGNIFICANT_REBELLION,
                        false);
            Decision inactiveDecision =
                    battleOrPassAt(
                        inactive, OTHER_SITE_ID);
            Outcome inactiveBattle = only(
                    battleAdapter(
                        inactive,
                        inactiveDecision),
                    "battle");
            assertEquals(
                    "Insignificant Rebellion must be the "
                        + "only difference in the same-board "
                        + "battle score",
                    inactiveBattle.score() + 300.0f,
                    exactBattle.score(), 0.0f);
            TracedOutcome inactiveResult =
                    tracedCombined(
                        inactive,
                        inactiveDecision);
            assertNoTypedRule(
                    inactiveResult.trace(),
                    "battle",
                    INSIGNIFICANT_REBELLION_RULE);
            assertEquals(
                    "Inactive same-board battle must lose "
                        + "to Pass; selected="
                        + inactiveResult.outcome()
                        + " trace="
                        + inactiveResult.trace()
                            .getOperations(),
                    "pass",
                    inactiveResult.outcome()
                        .actionId());

            Fixture wrong =
                    bhbmInsignificantBattleFixture(
                        bot,
                        VIRTUAL_INSIGNIFICANT_REBELLION,
                        true);
            TracedOutcome wrongResult =
                    tracedCombined(
                        wrong,
                        battleOrPassAt(
                            wrong,
                            OTHER_SITE_ID));
            assertNoTypedRule(
                    wrongResult.trace(),
                    "battle",
                    INSIGNIFICANT_REBELLION_RULE);
            assertEquals("pass",
                    wrongResult.outcome()
                        .actionId());
        }
        assertParity(exactWinners);
    }

    @Test
    public void bhbmVaderTakesSafeTwoHopRouteTowardLuke() {
        List<Outcome> parents = new ArrayList<>();
        List<Outcome> halfwayDestinations = new ArrayList<>();
        List<Outcome> wrongDestinations = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = bhbmTwoHopMoveFixture(bot);
            assertDeathStarIiRouteChain(fixture);

            Decision parent = Decision.actions(
                    "Choose move action", Phase.MOVE,
                    List.of("move-vader"),
                    List.of("Move using landspeed"),
                    List.of(String.valueOf(VADER_ID)),
                    true, 1);
            Outcome move = only(
                    moveAdapter(fixture, parent),
                    "move-vader");
            assertContains(move,
                    "MOVE.OBJECTIVE.ACTOR_ROUTE_START");
            parents.add(move);

            Decision child = Decision.cards(
                    "Choose where to move "
                            + "<div class='cardHint' value='"
                            + VADER + "'>Darth Vader</div>"
                            + " using landspeed",
                    Phase.MOVE,
                    List.of(
                            String.valueOf(
                                DSII_CHASM_WALKWAY_ID),
                            String.valueOf(
                                DSII_DOCKING_BAY_ID)),
                    List.of(DSII_CHASM_WALKWAY,
                            DSII_DOCKING_BAY),
                    List.of(true, true)).withMover(VADER_ID);
            Outcome halfway = only(
                    cardSelectionAdapter(fixture, child),
                    String.valueOf(
                        DSII_CHASM_WALKWAY_ID));
            Outcome wrong = only(
                    cardSelectionAdapter(fixture, child),
                    String.valueOf(
                        DSII_DOCKING_BAY_ID));
            assertContains(halfway,
                    "MOVE.OBJECTIVE.ACTOR_ROUTE_DESTINATION");
            assertNotContains(wrong,
                    "MOVE.OBJECTIVE.ACTOR_ROUTE_DESTINATION");
            assertTrue(halfway.score() > wrong.score());
            assertEquals(
                    String.valueOf(
                        DSII_CHASM_WALKWAY_ID),
                    combined(fixture, child).actionId());
            halfwayDestinations.add(halfway);
            wrongDestinations.add(wrong);
        }
        assertParity(parents);
        assertParity(halfwayDestinations);
        assertParity(wrongDestinations);
    }

    @Test
    public void deployParentPrefersExactSiteOverPrefixingSystemTitle() {
        List<Outcome> captureSites = new ArrayList<>();
        List<Outcome> prefixSystems = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture =
                    bhbmPrefixCollisionDeployFixture(bot);
            assertTrue(
                    fixture.card(OTHER_SITE_ID).getTitle()
                        .startsWith(
                            fixture.card(TATOOINE_SYSTEM_ID)
                                .getTitle()));
            assertEquals(
                    fixture.card(TATOOINE_SYSTEM_ID),
                    fixture.gameState.getTopLocations().get(0));
            Decision decision = Decision.actions(
                    "Choose deploy action", Phase.DEPLOY,
                    List.of("deploy-vader-site",
                            "deploy-vader-system"),
                    List.of("Deploy "
                                    + fixture.card(VADER_ID)
                                        .getTitle()
                                    + " to "
                                    + fixture.card(OTHER_SITE_ID)
                                        .getTitle(),
                            "Deploy "
                                    + fixture.card(VADER_ID)
                                        .getTitle()
                                    + " to "
                                    + fixture.card(
                                        TATOOINE_SYSTEM_ID)
                                        .getTitle()),
                    List.of(String.valueOf(VADER_ID),
                            String.valueOf(VADER_ID)),
                    true, 1);

            Outcome site = only(
                    deployAdapter(fixture, decision),
                    "deploy-vader-site");
            Outcome system = only(
                    deployAdapter(fixture, decision),
                    "deploy-vader-system");
            TracedOutcome result =
                    tracedCombined(fixture, decision);
            assertContains(site, "CAPTURE DEPLOY");
            assertNotContains(system, "CAPTURE DEPLOY");
            assertTypedContribution(
                    result.trace(),
                    "deploy-vader-site",
                    "DEPLOY.OBJECTIVE.CAPTURE_ROUTE_PARENT",
                    1, 300.0f);
            assertFalse(site.hardVeto());
            assertFalse(system.hardVeto());
            captureSites.add(site);
            prefixSystems.add(system);
        }
        assertParity(captureSites);
        assertParity(prefixSystems);
    }

    @Test
    public void duplicateVaderParentSelectionCarriesExactPhysicalIdToChild()
            throws Exception {
        List<Outcome> selectedDestinations = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = bhbmDeployFixture(bot);
            PhysicalCard firstCopy = fixture.card(VADER_ID);
            PhysicalCard selectedCopy = fixture.addHand(
                    VADER, VADER_DUPLICATE_ID);
            fixture.markPersona(selectedCopy, Persona.VADER);
            when(fixture.gameState.getCurrentPhase())
                    .thenReturn(Phase.DEPLOY);

            Decision parent = Decision.actions(
                    "Choose deploy action", Phase.DEPLOY,
                    List.of("deploy-first-vader",
                            "deploy-selected-vader"),
                    List.of("Deploy " + firstCopy.getTitle(),
                            "Deploy " + selectedCopy.getTitle()),
                    List.of(String.valueOf(VADER_ID),
                            String.valueOf(VADER_DUPLICATE_ID)),
                    true, 1);
            Decision child = Decision.cards(
                    "Choose where to deploy "
                            + "<div class='cardHint' value='"
                            + VADER + "'>Darth Vader</div>",
                    Phase.DEPLOY,
                    List.of(String.valueOf(OTHER_SITE_ID),
                            String.valueOf(THRONE_ID)),
                    List.of(OTHER_SITE, THRONE_ROOM),
                    List.of(true, true));

            List<Outcome> outcomes =
                    liveDeployChildAfterParentSelection(
                        fixture, parent,
                        "deploy-selected-vader",
                        selectedCopy, firstCopy, child);
            Outcome targetSite = only(
                    outcomes,
                    String.valueOf(OTHER_SITE_ID));
            Outcome throne = only(
                    outcomes,
                    String.valueOf(THRONE_ID));
            assertContains(targetSite, "CAPTURE DEPLOY");
            assertNotContains(throne, "CAPTURE DEPLOY");
            assertTrue(targetSite.score() > throne.score());
            selectedDestinations.add(targetSite);
        }
        assertParity(selectedDestinations);
    }

    @Test
    public void offeredEmperorDownloadGetsBoundedScoringOnFrontAndBack() {
        Decision decision = Decision.actions(
                "Choose action", Phase.DEPLOY,
                List.of("emperor-download", "pass"),
                List.of("Deploy Emperor from Reserve Deck",
                        "Pass"),
                List.of(String.valueOf(OBJECTIVE_ID),
                        ""),
                false, 0);

        for (boolean flipped : List.of(false, true)) {
            Map<Integer, List<Outcome>> blockedByForce =
                    new LinkedHashMap<>();
            Map<Integer, List<Outcome>> passesByForce =
                    new LinkedHashMap<>();
            List<Outcome> affordable = new ArrayList<>();
            List<Outcome> winners = new ArrayList<>();
            for (int force : List.of(0, 2)) {
                blockedByForce.put(force, new ArrayList<>());
                passesByForce.put(force, new ArrayList<>());
            }

            for (Bot bot : Bot.values()) {
                for (int force : List.of(0, 2)) {
                    Fixture blockedFixture = new Fixture(
                            bot, Kind.BHBM, flipped);
                    PhysicalCard unavailableEmperor =
                            blockedFixture.addReserve(
                                EMPEROR, EMPEROR_ID);
                    blockedFixture.markPersona(
                            unavailableEmperor,
                            Persona.SIDIOUS);
                    blockedFixture
                        .setBhbmEmperorDeployability(
                            unavailableEmperor);
                    when(blockedFixture.gameState
                            .getForcePileSize(
                                blockedFixture.player))
                        .thenReturn(force);

                    Outcome blocked = only(
                            actionTextAdapter(
                                blockedFixture, decision),
                            "emperor-download");
                    assertFalse(blocked.hardVeto());
                    assertContains(blocked,
                            "BHBM EMPEROR RESERVE");
                    assertNotContains(blocked,
                            "BHBM PAYOFF");
                    assertNotContains(blocked,
                            "BHBM SETUP");
                    if (force == 0) {
                        assertContains(blocked,
                                "V192 PULL SCORER");
                    }
                    Outcome normalWinner = combined(
                            blockedFixture, decision);
                    assertFalse(normalWinner.hardVeto());
                    blockedByForce.get(force).add(blocked);
                    passesByForce.get(force).add(normalWinner);
                }

                Fixture ready = new Fixture(
                        bot, Kind.BHBM, flipped);
                PhysicalCard deployableEmperor =
                        ready.addReserve(
                            EMPEROR, EMPEROR_ID);
                ready.markPersona(
                        deployableEmperor,
                        Persona.SIDIOUS);
                ready.setBhbmEmperorDeployability(
                        deployableEmperor);
                when(ready.gameState.getForcePileSize(
                        ready.player)).thenReturn(3);
                assertEquals(0,
                        com.gempukku.swccgo.ai.models
                            .common.phase.CaptureObjectiveFacts
                            .nextCaptureMoveForceReserve(
                                ready.game,
                                ready.player,
                                ready.analyzer,
                                deployableEmperor));

                Outcome exact = only(
                        actionTextAdapter(ready, decision),
                        "emperor-download");
                assertFalse(exact.hardVeto());
                if (flipped) {
                    assertContains(exact,
                            "BHBM PAYOFF");
                    assertNotContains(exact,
                            "BHBM SETUP");
                } else {
                    assertContains(exact,
                            "BHBM SETUP");
                    assertNotContains(exact,
                            "BHBM PAYOFF");
                }
                TracedOutcome result = tracedCombined(
                        ready, decision);
                Outcome winner = result.outcome();
                assertTypedContribution(
                        result.trace(),
                        "emperor-download",
                        flipped
                            ? "PULL.OBJECTIVE.BHBM.EMPEROR"
                            : "PULL.OBJECTIVE.BHBM.EMPEROR_SETUP",
                        1, 300.0f);
                assertEquals("emperor-download",
                        winner.actionId());
                affordable.add(exact);
                winners.add(winner);
            }

            for (int force : List.of(0, 2)) {
                assertParity(blockedByForce.get(force));
                assertParity(passesByForce.get(force));
            }
            assertParity(affordable);
            assertParity(winners);
        }
    }

    @Test
    public void opponentBhbmTargetDownloadRequiresExactPhysicalSourceAndText() {
        List<Outcome> exactDownloads = new ArrayList<>();
        List<Outcome> wrongTexts = new ArrayList<>();
        List<Outcome> wrongSources = new ArrayList<>();
        List<Outcome> winners = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = new Fixture(
                    bot, Kind.TIGIH, false);
            PhysicalCard opponentBhbm =
                    Fixture.objective(
                        BHBM, OPPONENT_OBJECTIVE_ID,
                        fixture.opponent, false);
            fixture.add(opponentBhbm, true);
            when(fixture.gameState
                    .isCardInPlayActive(opponentBhbm))
                    .thenReturn(true);

            Decision decision = Decision.actions(
                    "Choose action", Phase.DEPLOY,
                    List.of("exact-download",
                            "wrong-text",
                            "wrong-source",
                            "pass"),
                    List.of(
                            "Deploy Luke from Reserve Deck",
                            "Deploy Luke from Reserve Deck now",
                            "Deploy Luke from Reserve Deck",
                            "Pass"),
                    List.of(
                            String.valueOf(
                                OPPONENT_OBJECTIVE_ID),
                            String.valueOf(
                                OPPONENT_OBJECTIVE_ID),
                            String.valueOf(OBJECTIVE_ID),
                            ""),
                    false, 0);

            Outcome exact = only(
                    actionTextAdapter(fixture, decision),
                    "exact-download");
            Outcome wrongText = only(
                    actionTextAdapter(fixture, decision),
                    "wrong-text");
            Outcome wrongSource = only(
                    actionTextAdapter(fixture, decision),
                    "wrong-source");
            Outcome winner = combined(fixture, decision);
            assertContains(exact,
                    "BHBM OPPONENT TEXT");
            assertNotContains(wrongText,
                    "BHBM OPPONENT TEXT");
            assertNotContains(wrongSource,
                    "BHBM OPPONENT TEXT");
            assertEquals(
                    "The exact BHBM match must not stack beyond the +300"
                        + " already applied by the objective actor match",
                    wrongText.score(),
                    exact.score(), 0.0f);
            assertEquals(
                    "The exact source match contributes one bounded +300"
                        + " when the wrong source has no objective signal",
                    wrongSource.score() + 300.0f,
                    exact.score(), 0.0f);
            assertEquals("exact-download",
                    winner.actionId());
            exactDownloads.add(exact);
            wrongTexts.add(wrongText);
            wrongSources.add(wrongSource);
            winners.add(winner);
        }
        assertParity(exactDownloads);
        assertParity(wrongTexts);
        assertParity(wrongSources);
        assertParity(winners);
    }

    @Test
    public void captureMoveForceReserveIsBoundedForUnrelatedDeployAndBattle() {
        List<Outcome> reserveBreakingDeploys = new ArrayList<>();
        List<Outcome> reserveBreakingWinners = new ArrayList<>();
        List<Outcome> fundedDeploys = new ArrayList<>();
        List<Outcome> fundedWinners = new ArrayList<>();
        List<Outcome> battles = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = ordinaryTigihMoveFixture(bot);
            PhysicalCard unrelated = fixture.addHand(
                    IMPERIAL, DISTRACTOR_ID);
            PhysicalCard battleSite = fixture.addLocation(
                    THRONE_ROOM, THRONE_ID,
                    fixture.player);
            assertEquals(1,
                    com.gempukku.swccgo.ai.models
                        .common.phase.CaptureObjectiveFacts
                        .nextCaptureMoveForceReserve(
                            fixture.game,
                            fixture.player,
                            fixture.analyzer,
                            unrelated));

            when(fixture.gameState.getForcePileSize(
                    fixture.player)).thenReturn(2);

            Decision deployDecision = Decision.actions(
                    "Choose deploy action", Phase.DEPLOY,
                    List.of("deploy-unrelated", "pass"),
                    List.of("Deploy "
                            + unrelated.getTitle()
                            + " to "
                            + fixture.card(OTHER_SITE_ID).getTitle(),
                            "Pass"),
                    List.of(String.valueOf(DISTRACTOR_ID), ""),
                    false, 0);
            Outcome reserveBreakingDeploy = only(
                    deployAdapter(fixture, deployDecision),
                    "deploy-unrelated");
            assertFalse(reserveBreakingDeploy.hardVeto());
            assertContains(reserveBreakingDeploy,
                    "exact capture move needs 1");
            assertContains(reserveBreakingDeploy, "(-300.0)");
            Outcome reserveBreakingWinner = combined(
                    fixture, deployDecision);
            assertEquals("deploy-unrelated",
                    reserveBreakingWinner.actionId());
            assertEquals("DEPLOY",
                    reserveBreakingWinner.actionType());
            assertFalse(reserveBreakingWinner.hardVeto());
            reserveBreakingDeploys.add(
                    reserveBreakingDeploy);
            reserveBreakingWinners.add(
                    reserveBreakingWinner);

            when(fixture.gameState.getForcePileSize(
                    fixture.player)).thenReturn(3);
            Outcome fundedDeploy = only(
                    deployAdapter(fixture, deployDecision),
                    "deploy-unrelated");
            assertFalse(fundedDeploy.hardVeto());
            assertNotContains(fundedDeploy,
                    "exact capture move needs 1");
            assertTrue(fundedDeploy.score()
                    >= -100.0f);
            Outcome fundedWinner = combined(
                    fixture, deployDecision);
            assertEquals("deploy-unrelated",
                    fundedWinner.actionId());
            assertEquals("DEPLOY",
                    fundedWinner.actionType());
            assertFalse(fundedWinner.hardVeto());
            fundedDeploys.add(fundedDeploy);
            fundedWinners.add(fundedWinner);

            when(fixture.gameState.getForcePileSize(
                    fixture.player)).thenReturn(3);
            fixture.setBattle(
                    battleSite,
                    12.0f, 3.0f, 4.0f, 1.0f,
                    List.of(unrelated,
                            fixture.addActive(
                                DISTRACTOR, EMPEROR_ID,
                                fixture.opponent,
                                battleSite)));
            when(fixture.modifiers.getInitiateBattleCost(
                    fixture.gameState,
                    battleSite,
                    fixture.player, true)).thenReturn(3.0f);
            Decision battleDecision = Decision.actions(
                    "Choose battle", Phase.BATTLE,
                    List.of("battle"),
                    List.of("Initiate battle at "
                            + battleSite.getTitle()),
                    List.of(String.valueOf(THRONE_ID)),
                    true, 1);
            Outcome battle = only(
                    battleAdapter(fixture, battleDecision),
                    "battle");
            assertFalse(battle.hardVeto());
            assertContains(battle,
                    "Preserve the exact Force required");
            assertContains(battle, "(-300.0)");
            battles.add(battle);
        }
        assertParity(reserveBreakingDeploys);
        assertParity(reserveBreakingWinners);
        assertParity(fundedDeploys);
        assertParity(fundedWinners);
        assertParity(battles);
    }

    @Test
    public void captureReserveUsesCeiledLiveTargetDeployPayment() {
        List<Outcome> reserveBreaking = new ArrayList<>();
        List<Outcome> reserveWinners = new ArrayList<>();
        List<Outcome> funded = new ArrayList<>();
        List<Outcome> fundedWinners = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = ordinaryTigihMoveFixture(bot);
            PhysicalCard unrelated = fixture.addHand(
                    IMPERIAL, DISTRACTOR_ID);
            fixture.setNormalDeployPayment(
                    unrelated,
                    fixture.card(OTHER_SITE_ID),
                    2.5f, 1);
            Decision decision = Decision.actions(
                    "Choose deploy action", Phase.DEPLOY,
                    List.of("deploy-unrelated", "pass"),
                    List.of("Deploy "
                            + unrelated.getTitle(),
                            "Pass"),
                    List.of(String.valueOf(
                            DISTRACTOR_ID), ""),
                    false, 0);

            when(fixture.gameState.getForcePileSize(
                    fixture.player)).thenReturn(4);
            Outcome blocked = only(
                    deployAdapter(fixture, decision),
                    "deploy-unrelated");
            assertFalse(blocked.hardVeto());
            assertContains(blocked,
                    "exact capture move needs 1");
            Outcome blockedWinner =
                    combined(fixture, decision);
            assertEquals("PASS",
                    blockedWinner.actionType());
            reserveBreaking.add(blocked);
            reserveWinners.add(blockedWinner);

            when(fixture.gameState.getForcePileSize(
                    fixture.player)).thenReturn(5);
            Outcome allowed = only(
                    deployAdapter(fixture, decision),
                    "deploy-unrelated");
            assertFalse(allowed.hardVeto());
            assertNotContains(allowed,
                    "exact capture move needs 1");
            Outcome allowedWinner =
                    combined(fixture, decision);
            assertEquals("deploy-unrelated",
                    allowedWinner.actionId());
            funded.add(allowed);
            fundedWinners.add(allowedWinner);
        }
        assertParity(reserveBreaking);
        assertParity(reserveWinners);
        assertParity(funded);
        assertParity(fundedWinners);
    }

    @Test
    public void unknownCaptureDeployPaymentFailsClosed() {
        List<Outcome> unknowns = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = ordinaryTigihMoveFixture(bot);
            PhysicalCard unrelated = fixture.addHand(
                    IMPERIAL, DISTRACTOR_ID);
            fixture.setDeployable(false);
            Decision decision = Decision.actions(
                    "Choose deploy action", Phase.DEPLOY,
                    List.of("deploy-unrelated", "pass"),
                    List.of("Deploy "
                            + unrelated.getTitle(),
                            "Pass"),
                    List.of(String.valueOf(
                            DISTRACTOR_ID), ""),
                    false, 0);

            Outcome unknown = only(
                    deployAdapter(fixture, decision),
                    "deploy-unrelated");
            assertTrue(unknown.hardVeto());
            assertContains(unknown,
                    "CAPTURE.BUDGET.UNKNOWN");
            assertEquals("PASS",
                    combined(fixture, decision)
                        .actionType());
            unknowns.add(unknown);
        }
        assertParity(unknowns);
    }

    @Test
    public void preflipEmperorDownloadReserveIsBounded() {
        Decision decision = Decision.actions(
                "Choose action", Phase.DEPLOY,
                List.of("emperor-download", "pass"),
                List.of("Deploy Emperor from Reserve Deck",
                        "Pass"),
                List.of(String.valueOf(OBJECTIVE_ID),
                        ""),
                false, 0);
        List<Outcome> reserveBreakingDownloads =
                new ArrayList<>();
        List<Outcome> reserveBreakingWinners =
                new ArrayList<>();
        List<Outcome> fundedDownloads =
                new ArrayList<>();
        List<Outcome> fundedWinners =
                new ArrayList<>();

        for (Bot bot : Bot.values()) {
            Fixture fixture =
                    bhbmEmperorCaptureReserveFixture(bot);
            PhysicalCard emperor = fixture.addReserve(
                    EMPEROR, EMPEROR_ID);
            fixture.markPersona(
                    emperor, Persona.SIDIOUS);
            fixture.setBhbmEmperorDeployability(
                    emperor);
            assertEquals(1,
                    com.gempukku.swccgo.ai.models
                        .common.phase.CaptureObjectiveFacts
                        .nextCaptureMoveForceReserve(
                            fixture.game,
                            fixture.player,
                            fixture.analyzer,
                            emperor));

            when(fixture.gameState.getForcePileSize(
                    fixture.player)).thenReturn(3);
            Outcome reserveBreakingDownload = only(
                    actionTextAdapter(fixture, decision),
                    "emperor-download");
            assertFalse(reserveBreakingDownload.hardVeto());
            assertContains(reserveBreakingDownload,
                    "BHBM EMPEROR RESERVE");
            Outcome reserveBreakingWinner = combined(
                    fixture, decision);
            assertEquals("emperor-download",
                    reserveBreakingWinner.actionId());
            assertEquals("DEPLOY",
                    reserveBreakingWinner.actionType());
            assertFalse(reserveBreakingWinner.hardVeto());
            reserveBreakingDownloads.add(
                    reserveBreakingDownload);
            reserveBreakingWinners.add(
                    reserveBreakingWinner);

            when(fixture.gameState.getForcePileSize(
                    fixture.player)).thenReturn(4);
            Outcome fundedDownload = only(
                    actionTextAdapter(fixture, decision),
                    "emperor-download");
            assertFalse(fundedDownload.hardVeto());
            assertContains(fundedDownload,
                    "BHBM SETUP");
            Outcome fundedWinner = combined(
                    fixture, decision);
            assertEquals("emperor-download",
                    fundedWinner.actionId());
            assertEquals("DEPLOY",
                    fundedWinner.actionType());
            assertFalse(fundedWinner.hardVeto());
            fundedDownloads.add(fundedDownload);
            fundedWinners.add(fundedWinner);
        }

        assertParity(reserveBreakingDownloads);
        assertParity(reserveBreakingWinners);
        assertParity(fundedDownloads);
        assertParity(fundedWinners);
    }

    @Test
    public void conflictBattleRewardsOnlySafeProjectionAndHoldsSoleEnabler() {
        List<Outcome> safeResults = new ArrayList<>();
        List<Outcome> unsafeResults = new ArrayList<>();
        List<Outcome> soleEnablerResults = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture safe = conflictBattleFixture(bot, false);
            safe.setBattle(
                    safe.card(OTHER_SITE_ID),
                    20.0f, 2.0f, 6.0f, 1.0f,
                    safe.cardsAt(safe.card(OTHER_SITE_ID)));
            Outcome safeBattle = only(
                    battleAdapter(safe,
                        battleAt(safe, OTHER_SITE_ID)),
                    "battle");
            assertContains(safeBattle,
                    "I FEEL THE CONFLICT");
            safeResults.add(safeBattle);

            Fixture unsafe = conflictBattleFixture(bot, false);
            unsafe.setBattle(
                    unsafe.card(OTHER_SITE_ID),
                    2.0f, 20.0f, 1.0f, 6.0f,
                    unsafe.cardsAt(unsafe.card(OTHER_SITE_ID)));
            Outcome unsafeBattle = only(
                    battleAdapter(unsafe,
                        battleAt(unsafe, OTHER_SITE_ID)),
                    "battle");
            assertNotContains(unsafeBattle,
                    "I FEEL THE CONFLICT");
            unsafeResults.add(unsafeBattle);

            Fixture sole = conflictBattleFixture(bot, true);
            sole.setBattle(
                    sole.card(LANDING_ID),
                    20.0f, 2.0f, 6.0f, 1.0f,
                    sole.cardsAt(sole.card(LANDING_ID)));
            Outcome soleBattle = only(
                    battleAdapter(sole,
                        battleAt(sole, LANDING_ID)),
                    "battle");
            assertFalse(soleBattle.hardVeto());
            assertContains(soleBattle,
                    "sole Imperial enabling Luke");
            soleEnablerResults.add(soleBattle);
        }
        assertParity(safeResults);
        assertParity(unsafeResults);
        assertParity(soleEnablerResults);
    }

    @Test
    public void powerZeroCaptureBattlesStillHonorReserveEnablerAndConflict() {
        List<Outcome> safeBattles = new ArrayList<>();
        List<Outcome> safeWinners = new ArrayList<>();
        List<Outcome> soleEnablerBattles = new ArrayList<>();
        List<Outcome> soleEnablerWinners = new ArrayList<>();
        List<Outcome> reserveBreakingBattles = new ArrayList<>();
        List<Outcome> reserveBreakingWinners = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture safe = conflictBattleFixture(bot, false);
            safe.setBattle(
                    safe.card(OTHER_SITE_ID),
                    20.0f, 0.0f, 6.0f, 0.0f,
                    safe.cardsAt(safe.card(OTHER_SITE_ID)));
            Decision safeDecision =
                    battleOrPassAt(safe, OTHER_SITE_ID);
            Outcome safeBattle = only(
                    battleAdapter(safe, safeDecision),
                    "battle");
            assertContains(safeBattle,
                    "I FEEL THE CONFLICT");
            Outcome safeWinner =
                    combined(safe, safeDecision);
            assertEquals("battle",
                    safeWinner.actionId());
            safeBattles.add(safeBattle);
            safeWinners.add(safeWinner);

            Fixture sole = conflictBattleFixture(bot, true);
            sole.setBattle(
                    sole.card(LANDING_ID),
                    20.0f, 0.0f, 6.0f, 0.0f,
                    sole.cardsAt(sole.card(LANDING_ID)));
            Decision soleDecision =
                    battleOrPassAt(sole, LANDING_ID);
            Outcome soleBattle = only(
                    battleAdapter(sole, soleDecision),
                    "battle");
            assertFalse(soleBattle.hardVeto());
            assertContains(soleBattle,
                    "sole Imperial enabling Luke");
            Outcome soleWinner =
                    combined(sole, soleDecision);
            assertFalse(soleWinner.hardVeto());
            soleEnablerBattles.add(soleBattle);
            soleEnablerWinners.add(soleWinner);

            Fixture reserve = ordinaryTigihMoveFixture(bot);
            PhysicalCard zeroPowerOpponent =
                    reserve.addActive(
                        IMPERIAL, EMPEROR_ID,
                        reserve.opponent,
                        reserve.card(OTHER_SITE_ID));
            when(reserve.gameState.getForcePileSize(
                    reserve.player)).thenReturn(1);
            reserve.setBattle(
                    reserve.card(OTHER_SITE_ID),
                    12.0f, 0.0f, 4.0f, 0.0f,
                    List.of(zeroPowerOpponent));
            when(reserve.modifiers.getInitiateBattleCost(
                    reserve.gameState,
                    reserve.card(OTHER_SITE_ID),
                    reserve.player, true)).thenReturn(1.0f);
            Decision reserveDecision =
                    battleOrPassAt(reserve, OTHER_SITE_ID);
            Outcome reserveBattle = only(
                    battleAdapter(reserve, reserveDecision),
                    "battle");
            assertFalse(reserveBattle.hardVeto());
            assertContains(reserveBattle,
                    "Preserve the exact Force required");
            Outcome reserveWinner =
                    combined(reserve, reserveDecision);
            assertEquals("pass", reserveWinner.actionId());
            assertFalse(reserveWinner.hardVeto());
            reserveBreakingBattles.add(reserveBattle);
            reserveBreakingWinners.add(reserveWinner);
        }
        assertParity(safeBattles);
        assertParity(safeWinners);
        assertParity(soleEnablerBattles);
        assertParity(soleEnablerWinners);
        assertParity(reserveBreakingBattles);
        assertParity(reserveBreakingWinners);
    }

    @Test
    public void preferredExecutableObjectivePiecesGetBoundedLossPreference() {
        List<Outcome> forceLossCritical = new ArrayList<>();
        List<Outcome> forfeitCritical = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture forceLoss = new Fixture(
                    bot, Kind.BHBM, false);
            PhysicalCard emperor = forceLoss.addReserve(
                    EMPEROR, EMPEROR_ID);
            PhysicalCard disposable = forceLoss.addHand(
                    DISTRACTOR, DISTRACTOR_ID);
            forceLoss.markPersona(emperor, Persona.SIDIOUS);
            forceLoss.setBhbmEmperorDeployability(emperor);
            when(forceLoss.gameState.getForcePileSize(
                    forceLoss.player)).thenReturn(4);
            assertEquals(forceLoss.player, emperor.getOwner());
            assertEquals(Zone.RESERVE_DECK, emperor.getZone());
            assertTrue(forceLoss.permanents.contains(emperor));
            assertTrue(
                    com.gempukku.swccgo.filters.Filters.Emperor
                        .accepts(
                            forceLoss.gameState,
                            forceLoss.modifiers,
                            emperor));
            assertEquals(
                    com.gempukku.swccgo.ai.models.common.phase
                        .CaptureObjectivePolicy.CriticalRole
                        .PAYOFF_CARD,
                    com.gempukku.swccgo.ai.models.common.phase
                        .CaptureObjectiveFacts
                        .preferredCriticalLossRole(
                            forceLoss.game,
                            forceLoss.player,
                            forceLoss.analyzer,
                            emperor));
            Decision loss = Decision.cards(
                    "Choose Force to lose", Phase.DEPLOY,
                    List.of(String.valueOf(EMPEROR_ID),
                            String.valueOf(DISTRACTOR_ID)),
                    List.of(EMPEROR, DISTRACTOR),
                    List.of(true, true));
            Outcome keepEmperor = only(
                    cardSelectionAdapter(forceLoss, loss),
                    String.valueOf(EMPEROR_ID));
            Outcome loseOther = only(
                    cardSelectionAdapter(forceLoss, loss),
                    String.valueOf(DISTRACTOR_ID));
            assertContains(keepEmperor,
                    "BHBM CRITICAL");
            assertNotContains(loseOther,
                    "BHBM CRITICAL");
            assertFalse(keepEmperor.hardVeto());
            assertFalse(loseOther.hardVeto());
            assertEquals(
                    "Generic Force-loss value may override the"
                        + " bounded Emperor-retention preference",
                    String.valueOf(EMPEROR_ID),
                    combined(forceLoss, loss).actionId());
            forceLossCritical.add(keepEmperor);
            assertNotNull(emperor);
            assertNotNull(disposable);

            Fixture forfeit = bhbmTwoHopMoveFixture(bot);
            PhysicalCard site = forfeit.card(THRONE_ID);
            PhysicalCard vader = forfeit.card(VADER_ID);
            PhysicalCard buddy = forfeit.addActive(
                    IMPERIAL, IMPERIAL_ID,
                    forfeit.player, site);
            assertEquals(
                    com.gempukku.swccgo.ai.models.common.phase
                        .CaptureObjectivePolicy.CriticalRole
                        .CAPTURE_PIECE,
                    com.gempukku.swccgo.ai.models.common.phase
                        .CaptureObjectiveFacts
                        .preferredCriticalLossRole(
                            forfeit.game,
                            forfeit.player,
                            forfeit.analyzer,
                            vader));
            Decision forfeitDecision = Decision.cards(
                    "Choose a card from battle to forfeit",
                    Phase.BATTLE,
                    List.of(String.valueOf(VADER_ID),
                            String.valueOf(IMPERIAL_ID)),
                    List.of(VADER, IMPERIAL),
                    List.of(true, true));
            Outcome keepVader = only(
                    cardSelectionAdapter(
                        forfeit, forfeitDecision),
                    String.valueOf(VADER_ID));
            Outcome loseBuddy = only(
                    cardSelectionAdapter(
                        forfeit, forfeitDecision),
                    String.valueOf(IMPERIAL_ID));
            assertContains(keepVader,
                    "BHBM CRITICAL");
            assertNotContains(loseBuddy,
                    "BHBM CRITICAL");
            assertTrue(keepVader.score()
                    < loseBuddy.score());
            assertEquals(String.valueOf(IMPERIAL_ID),
                    combined(
                        forfeit, forfeitDecision)
                        .actionId());
            forfeitCritical.add(keepVader);
            assertNotNull(vader);
            assertNotNull(buddy);
        }
        assertParity(forceLossCritical);
        assertParity(forfeitCritical);
    }

    @Test
    public void tigihVirtualHutLukeAndIftcSurviveLossAndForfeit() {
        List<Outcome> forceLossCritical =
                new ArrayList<>();
        List<Outcome> forfeitCritical =
                new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture forceLoss =
                    virtualHutFixture(bot);
            PhysicalCard iftc = forceLoss.addHand(
                    IFTC, IFTC_ID);
            PhysicalCard disposable =
                    forceLoss.addHand(
                        DISTRACTOR, DISTRACTOR_ID);
            assertEquals(
                    com.gempukku.swccgo.ai.models.common.phase
                        .CaptureObjectivePolicy.CriticalRole
                        .PAYOFF_CARD,
                    com.gempukku.swccgo.ai.models.common.phase
                        .CaptureObjectiveFacts
                        .preferredCriticalLossRole(
                            forceLoss.game,
                            forceLoss.player,
                            forceLoss.analyzer,
                            iftc));
            Decision loss = Decision.cards(
                    "Choose Force to lose", Phase.MOVE,
                    List.of(String.valueOf(IFTC_ID),
                            String.valueOf(DISTRACTOR_ID)),
                    List.of(IFTC, DISTRACTOR),
                    List.of(true, true));
            Outcome keepIftc = only(
                    cardSelectionAdapter(
                        forceLoss, loss),
                    String.valueOf(IFTC_ID));
            assertContains(keepIftc,
                    "TIGIH CRITICAL");
            assertEquals(
                    "Generic Force-loss value may override the"
                        + " bounded objective retention preference",
                    String.valueOf(IFTC_ID),
                    combined(forceLoss, loss)
                        .actionId());
            forceLossCritical.add(keepIftc);
            assertNotNull(disposable);

            Fixture forfeit =
                    virtualHutFixture(bot);
            PhysicalCard hut =
                    forfeit.card(HUT_ID);
            PhysicalCard exactLuke =
                    forfeit.card(LUKE_ID);
            PhysicalCard duplicateLuke =
                    forfeit.addActive(
                        LUKE, LUKE_DUPLICATE_ID,
                        forfeit.player, hut);
            PhysicalCard buddy =
                    forfeit.addActive(
                        DISTRACTOR, DISTRACTOR_ID,
                        forfeit.player, hut);
            forfeit.markPersona(
                    duplicateLuke, Persona.LUKE);
            assertEquals(
                    com.gempukku.swccgo.ai.models.common.phase
                        .CaptureObjectivePolicy.CriticalRole
                        .CAPTURE_PIECE,
                    com.gempukku.swccgo.ai.models.common.phase
                        .CaptureObjectiveFacts
                        .preferredCriticalLossRole(
                            forfeit.game,
                            forfeit.player,
                            forfeit.analyzer,
                            exactLuke));
            assertNull(
                    com.gempukku.swccgo.ai.models.common.phase
                        .CaptureObjectiveFacts
                        .preferredCriticalLossRole(
                            forfeit.game,
                            forfeit.player,
                            forfeit.analyzer,
                            duplicateLuke));
            Decision forfeitDecision = Decision.cards(
                    "Choose a card from battle to forfeit",
                    Phase.BATTLE,
                    List.of(String.valueOf(LUKE_ID),
                            String.valueOf(DISTRACTOR_ID)),
                    List.of(LUKE, DISTRACTOR),
                    List.of(true, true));
            Outcome keepLuke = only(
                    cardSelectionAdapter(
                        forfeit, forfeitDecision),
                    String.valueOf(LUKE_ID));
            assertContains(keepLuke,
                    "TIGIH CRITICAL");
            assertEquals(
                    String.valueOf(DISTRACTOR_ID),
                    combined(
                        forfeit, forfeitDecision)
                        .actionId());
            forfeitCritical.add(keepLuke);
            assertNotNull(buddy);
        }
        assertParity(forceLossCritical);
        assertParity(forfeitCritical);
    }

    @Test
    public void stableBackHoldCountsExcludedTargetAndVader() {
        List<Outcome> held = new ArrayList<>();
        List<Outcome> unstablyFree = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture stable = stableBhhmFixture(bot, true);
            assertDeathStarIiLocationOrder(
                    stable,
                    List.of(THRONE_ID,
                            DSII_CHASM_WALKWAY_ID));
            assertTrue(
                    com.gempukku.swccgo.ai.models.common.phase
                        .CaptureObjectiveFacts.stableBackState(
                            stable.game,
                            stable.player,
                            stable.analyzer));
            assertEquals(
                    ObjectiveAnalyzer.FlipGateFormationRole
                        .LAST_OBJECTIVE_SURVIVAL_ACTOR,
                    com.gempukku.swccgo.ai.models.common.phase
                        .CaptureObjectiveFacts
                        .classifyStableBackRemoval(
                            stable.game,
                            stable.player,
                            stable.analyzer,
                            stable.card(VADER_ID)));
            assertFalse(
                    com.gempukku.swccgo.ai.models.common.phase
                        .CaptureObjectiveFacts
                        .hasLegalStableBackMoveDestination(
                            stable.game,
                            stable.player,
                            stable.analyzer,
                            stable.card(VADER_ID)));
            Outcome hold = only(
                    moveAdapter(stable,
                        vaderMoveDecision()),
                    "move-vader");
            assertFalse(hold.hardVeto());
            assertContains(hold,
                    "STABLE_BACK_HOLD");
            held.add(hold);

            Fixture unstable = stableBhhmFixture(bot, false);
            assertDeathStarIiLocationOrder(
                    unstable,
                    List.of(THRONE_ID,
                            DSII_CHASM_WALKWAY_ID));
            Outcome allowed = only(
                    moveAdapter(unstable,
                        vaderMoveDecision()),
                    "move-vader");
            assertNotContains(allowed,
                    "STABLE_BACK_HOLD");
            unstablyFree.add(allowed);
        }
        assertParity(held);
        assertParity(unstablyFree);
    }

    @Test
    public void lastVaderAboardCarrierSurvivesLossForfeitAndMovement() {
        List<Outcome> forceLossHeld = new ArrayList<>();
        List<Outcome> forceLossWinners = new ArrayList<>();
        List<Outcome> forfeitHeld = new ArrayList<>();
        List<Outcome> forfeitWinners = new ArrayList<>();
        List<Outcome> movementHeld = new ArrayList<>();
        List<Outcome> movementWinners = new ArrayList<>();
        List<Outcome> duplicateForceLoss = new ArrayList<>();
        List<Outcome> duplicateForfeit = new ArrayList<>();
        List<Outcome> duplicateMovement = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture sole = stableBhbmCarrierFixture(
                    bot, false);
            Decision forceLoss = Decision.cards(
                    "Choose Force to lose", Phase.MOVE,
                    List.of(String.valueOf(CARRIER_ID),
                            String.valueOf(DISTRACTOR_ID)),
                    List.of(BANTHA, DISTRACTOR),
                    List.of(true, true));
            Outcome heldForce = only(
                    cardSelectionAdapter(sole, forceLoss),
                    String.valueOf(CARRIER_ID));
            Outcome forceWinner = combined(sole, forceLoss);
            assertContains(heldForce, "BHBM CRITICAL");
            assertEquals(String.valueOf(DISTRACTOR_ID),
                    forceWinner.actionId());
            forceLossHeld.add(heldForce);
            forceLossWinners.add(forceWinner);

            Decision forfeit = Decision.cards(
                    "Choose a card from battle to forfeit",
                    Phase.BATTLE,
                    List.of(String.valueOf(CARRIER_ID),
                            String.valueOf(DISTRACTOR_ID)),
                    List.of(BANTHA, DISTRACTOR),
                    List.of(true, true));
            Outcome heldForfeit = only(
                    cardSelectionAdapter(sole, forfeit),
                    String.valueOf(CARRIER_ID));
            Outcome forfeitWinner =
                    combined(sole, forfeit);
            assertContains(heldForfeit, "BHBM CRITICAL");
            assertEquals(String.valueOf(DISTRACTOR_ID),
                    forfeitWinner.actionId());
            forfeitHeld.add(heldForfeit);
            forfeitWinners.add(forfeitWinner);

            Decision movement =
                    carrierMoveOrPassDecision();
            Outcome heldMove = only(
                    moveAdapter(sole, movement),
                    "move-carrier");
            Outcome movementWinner =
                    combined(sole, movement);
            assertFalse(heldMove.hardVeto());
            assertContains(heldMove,
                    "STABLE_BACK_HOLD");
            assertEquals("pass",
                    movementWinner.actionId());
            movementHeld.add(heldMove);
            movementWinners.add(movementWinner);

            Fixture duplicate = stableBhbmCarrierFixture(
                    bot, true);
            Outcome allowedForce = only(
                    cardSelectionAdapter(
                        duplicate, forceLoss),
                    String.valueOf(CARRIER_ID));
            Outcome allowedForfeit = only(
                    cardSelectionAdapter(
                        duplicate, forfeit),
                    String.valueOf(CARRIER_ID));
            Outcome allowedMove = only(
                    moveAdapter(duplicate, movement),
                    "move-carrier");
            assertNotContains(allowedForce,
                    "BHBM CRITICAL");
            assertNotContains(allowedForfeit,
                    "BHBM CRITICAL");
            assertFalse(heldForce.hardVeto());
            assertFalse(heldForfeit.hardVeto());
            assertFalse(allowedForce.hardVeto());
            assertFalse(allowedForfeit.hardVeto());
            assertFalse(allowedMove.hardVeto());
            assertNotContains(allowedMove,
                    "STABLE_BACK_HOLD");
            assertTrue(allowedMove.score()
                    > heldMove.score());
            duplicateForceLoss.add(allowedForce);
            duplicateForfeit.add(allowedForfeit);
            duplicateMovement.add(allowedMove);
        }
        assertParity(forceLossHeld);
        assertParity(forceLossWinners);
        assertParity(forfeitHeld);
        assertParity(forfeitWinners);
        assertParity(movementHeld);
        assertParity(movementWinners);
        assertParity(duplicateForceLoss);
        assertParity(duplicateForfeit);
        assertParity(duplicateMovement);
    }

    @Test
    public void hitLoadedCarrierResolvesBeforeBoundedVaderHold() {
        List<Outcome> carriers = new ArrayList<>();
        List<Outcome> winners = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = stableBhbmCarrierFixture(
                    bot, false);
            PhysicalCard carrier =
                    fixture.card(CARRIER_ID);
            PhysicalCard vader =
                    fixture.card(VADER_ID);
            when(fixture.gameState.getAboardCards(
                    carrier, true))
                    .thenReturn(List.of(vader));
            when(carrier.isHit()).thenReturn(true);

            BattleState battleState =
                    new BattleState();
            battleState.reachedDamageSegment();
            battleState.setBaseBattleDamage(
                    fixture.player, 2.0f);
            when(fixture.gameState.getBattleState())
                    .thenReturn(battleState);
            when(fixture.modifiers.getTotalBattleDamage(
                    fixture.gameState, fixture.player))
                    .thenReturn(2.0f);

            Decision forfeit = Decision.cards(
                    "Choose Force to lose or a card from battle to forfeit",
                    Phase.BATTLE,
                    List.of(String.valueOf(CARRIER_ID),
                            String.valueOf(VADER_ID)),
                    List.of(BANTHA, VADER),
                    List.of(true, true));
            List<Outcome> actions =
                    cardSelectionAdapter(
                        fixture, forfeit);
            assertEquals(1, actions.size());
            Outcome hitCarrier = only(
                    actions,
                    String.valueOf(CARRIER_ID));
            assertFalse(actions.stream().anyMatch(
                    outcome -> String.valueOf(VADER_ID)
                            .equals(outcome.actionId())));
            Outcome winner =
                    combined(fixture, forfeit);

            assertFalse(hitCarrier.hardVeto());
            assertNotContains(hitCarrier,
                    "V48 SHIP WITH CREW");
            assertContains(hitCarrier,
                    "BHBM CRITICAL");
            assertEquals(String.valueOf(CARRIER_ID),
                    winner.actionId());
            carriers.add(hitCarrier);
            winners.add(winner);
        }
        assertParity(carriers);
        assertParity(winners);
    }

    @Test
    public void bhbmStagesVaderAndEmperorAtThroneAndRewardsExactDuel() {
        List<Outcome> vaderStages = new ArrayList<>();
        List<Outcome> emperorStages = new ArrayList<>();
        List<Outcome> duels = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = bhbmPayoffFixture(bot);
            assertDeathStarIiLocationOrder(
                    fixture,
                    List.of(THRONE_ID,
                            DSII_CHASM_WALKWAY_ID,
                            DSII_TURBOLIFT_WALKWAY_ID));
            Outcome vader = only(
                    cardSelectionAdapter(
                        fixture,
                        payoffMoveDecision(VADER_ID, VADER)),
                    String.valueOf(THRONE_ID));
            assertContains(vader,
                    "POST_FLIP_PRIMARY_PAYOFF");
            vaderStages.add(vader);

            Outcome emperor = only(
                    cardSelectionAdapter(
                        fixture,
                        payoffMoveDecision(
                            EMPEROR_ID, EMPEROR)),
                    String.valueOf(THRONE_ID));
            assertContains(emperor,
                    "POST_FLIP_SECONDARY_PAYOFF");
            emperorStages.add(emperor);

            PhysicalCard wrongSource = fixture.addActive(
                    DISTRACTOR, DISTRACTOR_ID,
                    fixture.player, null);
            Decision duelDecision = Decision.actions(
                    "Choose action", Phase.CONTROL,
                    List.of("exact-duel", "wrong-duel"),
                    List.of("Initiate a Luke/Vader duel",
                            "Initiate a Luke/Vader duel"),
                    List.of(String.valueOf(OBJECTIVE_ID),
                            String.valueOf(DISTRACTOR_ID)),
                    true, 1);
            Outcome exact = only(
                    actionTextAdapter(
                        fixture, duelDecision),
                    "exact-duel");
            Outcome wrong = only(
                    actionTextAdapter(
                        fixture, duelDecision),
                    "wrong-duel");
            assertContains(exact,
                    "initiate the legal Vader duel");
            assertNotContains(wrong,
                    "initiate the legal Vader duel");
            assertTrue(exact.score() > wrong.score());
            duels.add(exact);
            assertNotNull(wrongSource);
        }
        assertParity(vaderStages);
        assertParity(emperorStages);
        assertParity(duels);
    }

    @Test
    public void bhbmObservedForceDripDoesNotStackPastMoveObjectiveCap() {
        List<Outcome> urgentMoves = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = bhbmPayoffFixture(bot);
            fixture.place(
                    fixture.card(EMPEROR_ID),
                    fixture.card(THRONE_ID));
            Decision decision =
                    payoffMoveDecision(VADER_ID, VADER);

            Outcome ageZero = only(
                    cardSelectionAdapter(
                        fixture, decision),
                    String.valueOf(THRONE_ID));
            assertNotContains(
                    ageZero,
                    "source-legal duel trio");

            fixture.advanceObservedFlipAgeTo(7);
            Outcome urgent = only(
                    cardSelectionAdapter(
                        fixture, decision),
                    String.valueOf(THRONE_ID));
            Outcome wrong = only(
                    cardSelectionAdapter(
                        fixture, decision),
                    String.valueOf(
                        DSII_TURBOLIFT_WALKWAY_ID));

            assertContains(
                    urgent,
                    "source-legal duel trio");
            assertNotContains(
                    wrong,
                    "source-legal duel trio");
            assertEquals(
                    "Urgency is visible but must not stack a second"
                        + " objective bonus above +300",
                    ageZero.score(),
                    urgent.score(), 0.0f);
            assertEquals(
                    "Normal movement scoring may override the saturated"
                        + " objective urgency signal",
                    String.valueOf(
                        DSII_TURBOLIFT_WALKWAY_ID),
                    combined(fixture, decision)
                        .actionId());
            urgentMoves.add(urgent);
        }
        assertParity(urgentMoves);
    }

    @Test
    public void bhbmRealLandspeedParentCarriesSourceIntoUrgentChild()
            throws Exception {
        List<Outcome> urgentMoves =
                new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = bhbmPayoffFixture(bot);
            fixture.place(
                    fixture.card(EMPEROR_ID),
                    fixture.card(THRONE_ID));
            fixture.advanceObservedFlipAgeTo(7);
            when(fixture.gameState.getCurrentPhase())
                    .thenReturn(Phase.MOVE);

            Outcome throne = only(
                    liveMoveChildAfterParentSelection(
                        fixture,
                        vaderMoveDecision(),
                        payoffMoveDecision(
                            VADER_ID, VADER)),
                    String.valueOf(THRONE_ID));

            assertContains(
                    throne,
                    "source-legal duel trio");
            urgentMoves.add(throne);
        }
        assertParity(urgentMoves);
    }

    @Test
    public void bhbmObservedForceDripDoesNotStackPastDeployObjectiveCap() {
        List<Outcome> urgentDeploys = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture =
                    bhbmForceDripDeployFixture(bot);
            Decision decision =
                    bhbmEmperorDeployDecision();

            Outcome ageZero = only(
                    cardSelectionAdapter(
                        fixture, decision),
                    String.valueOf(THRONE_ID));
            assertNotContains(
                    ageZero,
                    "source-legal duel trio");

            fixture.advanceObservedFlipAgeTo(7);
            Outcome urgent = only(
                    cardSelectionAdapter(
                        fixture, decision),
                    String.valueOf(THRONE_ID));
            Outcome wrong = only(
                    cardSelectionAdapter(
                        fixture, decision),
                    String.valueOf(
                        DSII_CHASM_WALKWAY_ID));

            assertContains(
                    urgent,
                    "source-legal duel trio");
            assertNotContains(
                    wrong,
                    "source-legal duel trio");
            assertEquals(
                    "Urgency is visible but must not stack a second"
                        + " objective bonus above +300",
                    ageZero.score(),
                    urgent.score(), 0.0f);
            assertEquals(
                    String.valueOf(THRONE_ID),
                    combined(fixture, decision)
                        .actionId());
            urgentDeploys.add(urgent);
        }
        assertParity(urgentDeploys);
    }

    @Test
    public void bhbmObjectiveReserveDeployCarriesTrueSourceIntoUrgentChild()
            throws Exception {
        List<Outcome> urgentDeploys =
                new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture =
                    bhbmForceDripReserveDeployFixture(
                        bot);
            fixture.advanceObservedFlipAgeTo(7);
            when(fixture.gameState.getCurrentPhase())
                    .thenReturn(Phase.DEPLOY);
            Decision parent = Decision.actions(
                    "Choose deploy action",
                    Phase.DEPLOY,
                    List.of("deploy-emperor"),
                    List.of(
                        "Deploy Emperor from Reserve Deck"),
                    List.of(
                        String.valueOf(EMPEROR_ID)),
                    true, 1);
            Decision child = Decision.cards(
                    "Choose where to deploy "
                        + "<div class='cardHint' value='"
                        + EMPEROR
                        + "'>Emperor Palpatine</div>",
                    Phase.DEPLOY,
                    List.of(
                        String.valueOf(THRONE_ID),
                        String.valueOf(
                            DSII_CHASM_WALKWAY_ID)),
                    List.of(
                        THRONE_ROOM,
                        DSII_CHASM_WALKWAY),
                    List.of(true, true));

            Outcome throne = only(
                    liveReserveDeployChildAfterObjectiveAction(
                        fixture, parent,
                        fixture.card(EMPEROR_ID),
                        fixture.card(OBJECTIVE_ID),
                        child),
                    String.valueOf(THRONE_ID));

            assertContains(
                    throne,
                    "source-legal duel trio");
            urgentDeploys.add(throne);
        }
        assertParity(urgentDeploys);
    }

    @Test
    public void bhbmForeignActionSourceCannotClaimTrioUrgency() {
        List<Outcome> rejected = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture =
                    bhbmForceDripDeployFixture(bot);
            fixture.advanceObservedFlipAgeTo(7);
            Decision foreignSource =
                    bhbmEmperorDeployDecision()
                        .withActionSource(LUKE_ID);

            Outcome throne = only(
                    cardSelectionAdapter(
                        fixture, foreignSource),
                    String.valueOf(THRONE_ID));

            assertNotContains(
                    throne,
                    "source-legal duel trio");
            rejected.add(throne);
        }
        assertParity(rejected);
    }

    @Test
    public void tigihCrossoverUsesSafePhaseOrStrictlyGreaterThanFourteen() {
        List<Outcome> deferred = new ArrayList<>();
        List<Outcome> highTotal = new ArrayList<>();
        List<Outcome> safePhase = new ArrayList<>();
        for (Bot bot : Bot.values()) {
            Fixture fixture = tigihCrossoverFixture(bot);
            when(fixture.modifiers.getCrossoverAttemptTotal(
                    fixture.gameState,
                    fixture.card(VADER_ID), 0.0f))
                    .thenReturn(14.0f);
            Outcome atFourteen = only(
                    actionTextAdapter(fixture,
                        crossoverDecision(Phase.DEPLOY)),
                    "crossover");
            assertContains(atFourteen,
                    "wait for remaining safe");
            deferred.add(atFourteen);

            when(fixture.modifiers.getCrossoverAttemptTotal(
                    fixture.gameState,
                    fixture.card(VADER_ID), 0.0f))
                    .thenReturn(14.01f);
            Outcome aboveFourteen = only(
                    actionTextAdapter(fixture,
                        crossoverDecision(Phase.DEPLOY)),
                    "crossover");
            assertContains(aboveFourteen,
                    "attempt now");
            highTotal.add(aboveFourteen);

            when(fixture.modifiers.getCrossoverAttemptTotal(
                    fixture.gameState,
                    fixture.card(VADER_ID), 0.0f))
                    .thenReturn(0.0f);
            Outcome movePhase = only(
                    actionTextAdapter(fixture,
                        crossoverDecision(Phase.MOVE)),
                    "crossover");
            assertContains(movePhase,
                    "attempt now");
            safePhase.add(movePhase);
        }
        assertParity(deferred);
        assertParity(highTotal);
        assertParity(safePhase);
    }

    private static void assertChildChoice(
            Fixture fixture, Decision decision,
            int preferredId, int rejectedId) {
        List<Outcome> actions =
                cardSelectionAdapter(fixture, decision);
        Outcome preferred = only(
                actions, String.valueOf(preferredId));
        Outcome rejected = only(
                actions, String.valueOf(rejectedId));
        assertContains(preferred, "CAPTURE ROUTE");
        assertFalse("A reversible wrong capture-route child is only"
                + " an objective preference, not an engine veto: "
                + rejected, rejected.hardVeto());
        assertContains(rejected, "exact Hut");
        assertTrue(preferred.score() > rejected.score());
        assertEquals(String.valueOf(preferredId),
                combined(fixture, decision).actionId());
    }

    private static Decision battleAt(
            Fixture fixture, int locationId) {
        return Decision.actions(
                "Choose battle", Phase.BATTLE,
                List.of("battle"),
                List.of("Initiate battle at "
                        + fixture.card(locationId).getTitle()),
                List.of(String.valueOf(locationId)),
                true, 1);
    }

    private static Decision captureDeployParentsOrPass(
            Fixture fixture) {
        return Decision.actions(
                "Choose deploy action", Phase.DEPLOY,
                List.of("blocked-deploy",
                        "safe-deploy", "pass"),
                List.of("Deploy "
                            + fixture.card(VADER_ID)
                                .getTitle()
                            + " to "
                            + fixture.card(OTHER_SITE_ID)
                                .getTitle(),
                        "Deploy "
                            + fixture.card(
                                VADER_DUPLICATE_ID)
                                .getTitle()
                            + " to "
                            + fixture.card(OTHER_SITE_ID)
                                .getTitle(),
                        "Pass"),
                List.of(String.valueOf(VADER_ID),
                        String.valueOf(
                            VADER_DUPLICATE_ID),
                        ""),
                false, 0);
    }

    private static Decision
            yourDestinyDeployParentDecision(
                    Fixture fixture) {
        return Decision.actions(
                "Choose deploy action", Phase.DEPLOY,
                List.of("deploy-vader",
                        "deploy-neutral", "pass"),
                List.of(
                        "Deploy "
                            + fixture.card(VADER_ID)
                                .getTitle()
                            + " to "
                            + fixture.card(
                                OTHER_SITE_ID)
                                .getTitle(),
                        "Deploy "
                            + fixture.card(EMPEROR_ID)
                                .getTitle()
                            + " to "
                            + fixture.card(
                                OTHER_SITE_ID)
                                .getTitle(),
                        "Pass"),
                List.of(
                        String.valueOf(VADER_ID),
                        String.valueOf(EMPEROR_ID),
                        ""),
                false, 0);
    }

    private static Decision
            yourDestinyDeployParentToCarrierDecision(
                    Fixture fixture) {
        return Decision.actions(
                "Choose deploy action", Phase.DEPLOY,
                List.of(
                        "deploy-vader-carrier",
                        "deploy-neutral-carrier",
                        "pass"),
                List.of(
                        "Deploy "
                            + fixture.card(VADER_ID)
                                .getTitle()
                            + " to "
                            + fixture.card(CARRIER_ID)
                                .getTitle(),
                        "Deploy "
                            + fixture.card(EMPEROR_ID)
                                .getTitle()
                            + " to "
                            + fixture.card(CARRIER_ID)
                                .getTitle(),
                        "Pass"),
                List.of(
                        String.valueOf(VADER_ID),
                        String.valueOf(EMPEROR_ID),
                        ""),
                false, 0);
    }

    private static Decision specialMoveParentOrPass(
            String actionText) {
        return Decision.actions(
                "Choose move action", Phase.MOVE,
                List.of("special-move", "pass"),
                List.of(actionText, "Pass"),
                List.of(String.valueOf(VADER_ID), ""),
                false, 0);
    }

    private static Decision moveParentOrPass(
            int moverId) {
        return Decision.actions(
                "Choose move action", Phase.MOVE,
                List.of("move-parent", "pass"),
                List.of(
                        "Move using landspeed",
                        "Pass"),
                List.of(
                        String.valueOf(moverId),
                        ""),
                false, 0);
    }

    private static Decision landspeedDestinationDecision(
            Fixture fixture,
            int moverId,
            int destinationId) {
        PhysicalCard mover = fixture.card(moverId);
        PhysicalCard destination =
                fixture.card(destinationId);
        return Decision.cards(
                "Choose where to move "
                    + "<div class='cardHint' value='"
                    + mover.getBlueprintId(true)
                    + "'>" + mover.getTitle()
                    + "</div> using landspeed",
                Phase.MOVE,
                List.of(String.valueOf(
                    destinationId)),
                List.of(
                    destination.getBlueprintId(true)),
                List.of(true))
                .withMover(moverId);
    }

    private static Decision
            disembarkDestinationDecision() {
        return Decision.cards(
                "Choose where to disembark "
                    + "<div class='cardHint' value='"
                    + VADER
                    + "'>Darth Vader</div>",
                Phase.MOVE,
                List.of(String.valueOf(
                    OTHER_SITE_ID)),
                List.of(OTHER_SITE),
                List.of(true))
                .withMover(VADER_ID);
    }

    private static Decision battleOrPassAt(
            Fixture fixture, int locationId) {
        return Decision.actions(
                "Choose battle", Phase.BATTLE,
                List.of("battle", "pass"),
                List.of("Initiate battle at "
                            + fixture.card(locationId).getTitle(),
                        "Pass"),
                List.of(String.valueOf(locationId), ""),
                false, 1);
    }

    private static Decision vaderMoveDecision() {
        return Decision.actions(
                "Choose move action", Phase.MOVE,
                List.of("move-vader"),
                List.of("Move using landspeed"),
                List.of(String.valueOf(VADER_ID)),
                true, 1);
    }

    private static Decision carrierMoveOrPassDecision() {
        return Decision.actions(
                "Choose move action", Phase.MOVE,
                List.of("move-carrier", "pass"),
                List.of("Move using landspeed", "Pass"),
                List.of(String.valueOf(CARRIER_ID), ""),
                false, 1);
    }

    private static Decision payoffMoveDecision(
            int moverId, String blueprintId) {
        return Decision.cards(
                "Choose where to move "
                        + "<div class='cardHint' value='"
                        + blueprintId + "'>actor</div>"
                        + " using landspeed",
                Phase.MOVE,
                List.of(String.valueOf(THRONE_ID),
                        String.valueOf(
                            DSII_TURBOLIFT_WALKWAY_ID)),
                List.of(THRONE_ROOM,
                        DSII_TURBOLIFT_WALKWAY),
                List.of(true, true)).withMover(moverId);
    }

    private static Decision bhbmEmperorDeployDecision() {
        return Decision.cards(
                "Choose where to deploy "
                        + "<div class='cardHint' value='"
                        + EMPEROR
                        + "'>Emperor Palpatine</div>",
                Phase.DEPLOY,
                List.of(String.valueOf(THRONE_ID),
                        String.valueOf(
                            DSII_CHASM_WALKWAY_ID)),
                List.of(THRONE_ROOM,
                        DSII_CHASM_WALKWAY),
                List.of(true, true))
                .withDeploying(EMPEROR_ID);
    }

    private static Decision crossoverDecision(Phase phase) {
        return Decision.actions(
                "Choose action", phase,
                List.of("crossover"),
                List.of("Shuffle Reserve Deck and draw destiny"),
                List.of(String.valueOf(OBJECTIVE_ID)),
                true, 1);
    }

    private static Fixture virtualHutFixture(Bot bot) {
        Fixture fixture = new Fixture(bot, Kind.TIGIH, false);
        PhysicalCard hut = fixture.addLocation(
                VIRTUAL_HUT, HUT_ID, fixture.player);
        PhysicalCard landing = fixture.addLocation(
                LANDING_PLATFORM, LANDING_ID,
                fixture.player);
        PhysicalCard luke = fixture.addActive(
                LUKE, LUKE_ID, fixture.player, hut);
        PhysicalCard imperial = fixture.addActive(
                IMPERIAL, IMPERIAL_ID,
                fixture.opponent, landing);
        fixture.markPersona(luke, Persona.LUKE);
        fixture.allowEscort(imperial, luke);
        fixture.allowLocationTextMove(luke, hut, landing);
        return fixture;
    }

    private static Fixture ordinaryTigihMoveFixture(Bot bot) {
        Fixture fixture = new Fixture(bot, Kind.TIGIH, false);
        PhysicalCard origin = fixture.addLocation(
                OTHER_SITE, OTHER_SITE_ID,
                fixture.player);
        PhysicalCard landing = fixture.addLocation(
                LANDING_PLATFORM, LANDING_ID,
                fixture.player);
        PhysicalCard luke = fixture.addActive(
                LUKE, LUKE_ID, fixture.player, origin);
        PhysicalCard imperial = fixture.addActive(
                IMPERIAL, IMPERIAL_ID,
                fixture.opponent, landing);
        fixture.markPersona(luke, Persona.LUKE);
        fixture.allowEscort(imperial, luke);
        fixture.allowLandspeedMove(
                luke, origin, landing, 1.0f);
        return fixture;
    }

    private static Fixture bhbmYourDestinyDeployFixture(
            Bot bot,
            String payoffBlueprint,
            boolean payoffActive) {
        Fixture fixture =
                new Fixture(bot, Kind.BHBM, false);
        PhysicalCard battleground =
                fixture.addLocation(
                    OTHER_SITE, OTHER_SITE_ID,
                    fixture.player);
        fixture.addLocation(
                THRONE_ROOM, THRONE_ID,
                fixture.player);
        fixture.addActive(
                BANTHA, CARRIER_ID,
                fixture.player, battleground);
        fixture.addActive(
                BLIZZARD_ONE,
                ENCLOSED_CARRIER_ID,
                fixture.player, battleground);
        PhysicalCard vader = fixture.addHand(
                VADER, VADER_ID);
        PhysicalCard emperor = fixture.addHand(
                EMPEROR, EMPEROR_ID);
        fixture.markPersona(
                vader, Persona.VADER);
        fixture.markPersona(
                emperor, Persona.SIDIOUS);
        fixture.addSideEffect(
                payoffBlueprint,
                YOUR_DESTINY_ID,
                payoffActive);
        fixture.markBattleground(
                battleground);
        return fixture;
    }

    private static Fixture bhbmYourDestinyMoveFixture(
            Bot bot,
            boolean clockAlreadyLive) {
        Fixture fixture =
                new Fixture(bot, Kind.BHBM, false);
        PhysicalCard origin;
        PhysicalCard destination;
        if (clockAlreadyLive) {
            origin = fixture.addLocation(
                    OTHER_SITE, OTHER_SITE_ID,
                    fixture.player);
            destination = fixture.addLocation(
                    LANDING_PLATFORM, LANDING_ID,
                    fixture.player);
            fixture.markBattleground(origin);
            fixture.markBattleground(destination);
        } else {
            origin = fixture.addLocation(
                    THRONE_ROOM, THRONE_ID,
                    fixture.player);
            destination = fixture.addLocation(
                    OTHER_SITE, OTHER_SITE_ID,
                    fixture.player);
            fixture.markBattleground(destination);
        }
        PhysicalCard vader = fixture.addActive(
                VADER, VADER_ID,
                fixture.player, origin);
        fixture.markPersona(
                vader, Persona.VADER);
        fixture.addSideEffect(
                YOUR_DESTINY,
                YOUR_DESTINY_ID, true);
        fixture.allowLandspeedMove(
                vader, origin,
                destination, 1.0f);
        return fixture;
    }

    private static Fixture
            bhbmYourDestinyCarrierMoveFixture(
                    Bot bot,
                    boolean enclosed,
                    boolean payoffActive) {
        return bhbmYourDestinyCarrierMoveFixture(
                bot, enclosed, payoffActive,
                false);
    }

    private static Fixture
            bhbmYourDestinyCarrierMoveFixture(
                    Bot bot,
                    boolean enclosed,
                    boolean payoffActive,
                    boolean hostileDestination) {
        Fixture fixture =
                new Fixture(bot, Kind.BHBM, false);
        PhysicalCard origin = fixture.addLocation(
                TATOOINE_JAWA_CAMP,
                TATOOINE_JAWA_CAMP_ID,
                fixture.player);
        PhysicalCard destination =
                fixture.addLocation(
                    TATOOINE_JUNDLAND_WASTES,
                    TATOOINE_JUNDLAND_WASTES_ID,
                    fixture.player);
        fixture.markBattleground(destination);
        int carrierId = enclosed
                ? ENCLOSED_CARRIER_ID : CARRIER_ID;
        PhysicalCard carrier =
                fixture.addActive(
                    enclosed
                        ? BLIZZARD_ONE : BANTHA,
                    carrierId,
                    fixture.player, origin);
        var carrierTypes =
                carrier.getBlueprint()
                    .getCardTypes();
        when(fixture.modifiers.getCardTypes(
                fixture.gameState, carrier))
                .thenReturn(carrierTypes);
        PhysicalCard vader = fixture.addActive(
                VADER, VADER_ID,
                fixture.player, origin);
        fixture.markPersona(
                vader, Persona.VADER);
        fixture.aboard(
                vader, carrier, origin,
                carrier, origin);
        fixture.allowAboard(
                vader, carrier);
        fixture.addSideEffect(
                YOUR_DESTINY,
                YOUR_DESTINY_ID,
                payoffActive);
        fixture.allowLandspeedMove(
                carrier, origin,
                destination, 1.0f);
        if (hostileDestination) {
            fixture.addActive(
                    DISTRACTOR,
                    DISTRACTOR_ID,
                    fixture.opponent,
                    destination);
            fixture.setBattle(
                    destination,
                    0.0f, 20.0f,
                    0.0f, 4.0f,
                    fixture.cardsAt(destination));
        }
        return fixture;
    }

    private static Fixture
            bhbmYourDestinyDisembarkFixture(
                    Bot bot) {
        return bhbmYourDestinyDisembarkFixture(
                bot, false);
    }

    private static Fixture
            bhbmYourDestinyDisembarkFixture(
                    Bot bot,
                    boolean hostileDestination) {
        Fixture fixture =
                new Fixture(bot, Kind.BHBM, false);
        PhysicalCard battleground =
                fixture.addLocation(
                    OTHER_SITE, OTHER_SITE_ID,
                    fixture.player);
        fixture.markBattleground(
                battleground);
        PhysicalCard carrier =
                fixture.addActive(
                    BLIZZARD_ONE,
                    ENCLOSED_CARRIER_ID,
                    fixture.player,
                    battleground);
        PhysicalCard vader = fixture.addActive(
                VADER, VADER_ID,
                fixture.player,
                battleground);
        fixture.markPersona(
                vader, Persona.VADER);
        fixture.aboard(
                vader, carrier,
                battleground, carrier, null);
        fixture.allowAboard(
                vader, carrier);
        fixture.addSideEffect(
                YOUR_DESTINY,
                YOUR_DESTINY_ID, true);
        if (hostileDestination) {
            PhysicalCard luke =
                    fixture.addActive(
                        LUKE, LUKE_ID,
                        fixture.opponent,
                        battleground);
            fixture.markPersona(
                    luke, Persona.LUKE);
            fixture.allowTargetByObjective(luke);
            fixture.addActive(
                    DISTRACTOR,
                    DISTRACTOR_ID,
                    fixture.opponent,
                    battleground);
            fixture.setBattle(
                    battleground,
                    0.0f, 20.0f,
                    0.0f, 4.0f,
                    fixture.cardsAt(battleground));
        }
        return fixture;
    }

    private static Fixture
            bhbmInsignificantBattleFixture(
                    Bot bot,
                    String payoffBlueprint,
                    boolean payoffActive) {
        Fixture fixture =
                new Fixture(bot, Kind.BHBM, false);
        PhysicalCard battleSite =
                fixture.addLocation(
                    OTHER_SITE, OTHER_SITE_ID,
                    fixture.player);
        PhysicalCard vader =
                fixture.addActive(
                    VADER, VADER_ID,
                    fixture.player, battleSite);
        fixture.addActive(
                IMPERIAL, IMPERIAL_ID,
                fixture.player, battleSite);
        fixture.addActive(
                IMPERIAL, IMPERIAL_DUPLICATE_ID,
                fixture.player, battleSite);
        fixture.addActive(
                DISTRACTOR, DISTRACTOR_ID,
                fixture.opponent, battleSite);
        fixture.markPersona(
                vader, Persona.VADER);
        fixture.addSideEffect(
                payoffBlueprint,
                INSIGNIFICANT_REBELLION_ID,
                payoffActive);
        when(fixture.gameState.getForcePileSize(
                fixture.player)).thenReturn(1);
        when(fixture.gameState.getReserveDeckSize(
                fixture.player)).thenReturn(1);
        when(fixture.gameState.getPlayerLifeForce(
                fixture.opponent)).thenReturn(0);
        fixture.setBattle(
                battleSite,
                13.0f, 8.0f,
                8.0f, 6.0f,
                fixture.cardsAt(battleSite));
        return fixture;
    }

    private static Fixture enclosedEmbarkFixture(Bot bot) {
        Fixture fixture =
                new Fixture(bot, Kind.BHBM, false);
        PhysicalCard site = fixture.addLocation(
                OTHER_SITE, OTHER_SITE_ID,
                fixture.player);
        PhysicalCard luke = fixture.addActive(
                LUKE, LUKE_ID,
                fixture.opponent, site);
        PhysicalCard vader = fixture.addActive(
                VADER, VADER_ID,
                fixture.player, site);
        PhysicalCard carrier = fixture.addActive(
                BLIZZARD_ONE, ENCLOSED_CARRIER_ID,
                fixture.player, site);
        fixture.markPersona(luke, Persona.LUKE);
        fixture.markPersona(vader, Persona.VADER);
        fixture.allowTargetByObjective(luke);
        fixture.setPresentPlace(luke, site);
        fixture.setPresentPlace(carrier, carrier);
        fixture.allowEmbark(vader, carrier);
        return fixture;
    }

    private static Fixture differentShipShuttleFixture(
            Bot bot) {
        Fixture fixture =
                new Fixture(bot, Kind.BHBM, false);
        PhysicalCard system = fixture.addLocation(
                TATOOINE_SYSTEM, TATOOINE_SYSTEM_ID,
                fixture.player);
        PhysicalCard site = fixture.addLocation(
                TATOOINE_JAWA_CAMP,
                TATOOINE_JAWA_CAMP_ID,
                fixture.player);
        PhysicalCard shuttleDestination =
                fixture.addActive(
                    DEVASTATOR,
                    SHUTTLE_DESTINATION_ID,
                    fixture.player, system);
        PhysicalCard targetCarrier =
                fixture.addActive(
                    STAR_DESTROYER,
                    TARGET_CARRIER_ID,
                    fixture.opponent, system);
        PhysicalCard luke = fixture.addActive(
                LUKE, LUKE_ID,
                fixture.opponent, system);
        PhysicalCard vader = fixture.addActive(
                VADER, VADER_ID,
                fixture.player, site);
        fixture.markPersona(luke, Persona.LUKE);
        fixture.markPersona(vader, Persona.VADER);
        fixture.allowTargetByObjective(luke);
        when(luke.getAttachedTo())
                .thenReturn(targetCarrier);
        fixture.setPresentPlace(
                shuttleDestination, system);
        fixture.setPresentPlace(
                targetCarrier, system);
        fixture.setPresentPlace(
                luke, targetCarrier);
        fixture.allowShuttle(
                vader, shuttleDestination);
        return fixture;
    }

    private static Fixture bhbmDeployFixture(Bot bot) {
        Fixture fixture = new Fixture(bot, Kind.BHBM, false);
        PhysicalCard targetSite = fixture.addLocation(
                OTHER_SITE, OTHER_SITE_ID,
                fixture.player);
        fixture.addLocation(
                THRONE_ROOM, THRONE_ID,
                fixture.player);
        PhysicalCard luke = fixture.addActive(
                LUKE, LUKE_ID,
                fixture.opponent, targetSite);
        PhysicalCard vader = fixture.addHand(
                VADER, VADER_ID);
        fixture.markPersona(luke, Persona.LUKE);
        fixture.markPersona(vader, Persona.VADER);
        fixture.allowTargetByObjective(luke);
        fixture.setDeployable(true);
        return fixture;
    }

    private static Fixture bhbmEmperorCaptureReserveFixture(
            Bot bot) {
        Fixture fixture = new Fixture(
                bot, Kind.BHBM, false);
        PhysicalCard origin = fixture.addLocation(
                THRONE_ROOM, THRONE_ID,
                fixture.player);
        PhysicalCard captureSite = fixture.addLocation(
                OTHER_SITE, OTHER_SITE_ID,
                fixture.opponent);
        PhysicalCard luke = fixture.addActive(
                LUKE, LUKE_ID,
                fixture.opponent, captureSite);
        PhysicalCard vader = fixture.addActive(
                VADER, VADER_ID,
                fixture.player, origin);
        fixture.markPersona(luke, Persona.LUKE);
        fixture.markPersona(vader, Persona.VADER);
        fixture.allowTargetByObjective(luke);
        fixture.allowLandspeedMove(
                vader, origin, captureSite, 1.0f);
        return fixture;
    }

    private static Fixture bhbmTwoHopMoveFixture(Bot bot) {
        Fixture fixture = new Fixture(bot, Kind.BHBM, false);
        // DeathStarIILayout permits the reverse site-group order: docking
        // bay at the edge, followed by the deployed interior-site chain.
        PhysicalCard wrong = fixture.addLocation(
                DSII_DOCKING_BAY,
                DSII_DOCKING_BAY_ID,
                fixture.player);
        PhysicalCard origin = fixture.addLocation(
                THRONE_ROOM, THRONE_ID,
                fixture.player);
        PhysicalCard halfway = fixture.addLocation(
                DSII_CHASM_WALKWAY,
                DSII_CHASM_WALKWAY_ID,
                fixture.player);
        PhysicalCard captureSite = fixture.addLocation(
                DSII_TURBOLIFT_WALKWAY,
                DSII_TURBOLIFT_WALKWAY_ID,
                fixture.opponent);
        PhysicalCard luke = fixture.addActive(
                LUKE, LUKE_ID,
                fixture.opponent, captureSite);
        PhysicalCard vader = fixture.addActive(
                VADER, VADER_ID,
                fixture.player, origin);
        fixture.markPersona(luke, Persona.LUKE);
        fixture.markPersona(vader, Persona.VADER);
        fixture.allowTargetByObjective(luke);
        fixture.allowLandspeedMove(
                vader, origin, halfway, 1.0f);
        fixture.allowLandspeedMove(
                vader, origin, wrong, 1.0f);
        when(fixture.modifiers.getSitesBetween(
                fixture.gameState,
                origin, captureSite))
                .thenReturn(List.of(halfway));
        return fixture;
    }

    private static Fixture bhbmPrefixCollisionDeployFixture(
            Bot bot) {
        Fixture fixture = new Fixture(bot, Kind.BHBM, false);
        fixture.addLocation(
                TATOOINE_SYSTEM, TATOOINE_SYSTEM_ID,
                fixture.player);
        PhysicalCard captureSite = fixture.addLocation(
                OTHER_SITE, OTHER_SITE_ID,
                fixture.player);
        PhysicalCard luke = fixture.addActive(
                LUKE, LUKE_ID,
                fixture.opponent, captureSite);
        PhysicalCard vader = fixture.addHand(
                VADER, VADER_ID);
        fixture.markPersona(luke, Persona.LUKE);
        fixture.markPersona(vader, Persona.VADER);
        fixture.allowTargetByObjective(luke);
        fixture.setDeployable(true);
        return fixture;
    }

    private static Fixture conflictBattleFixture(
            Bot bot, boolean soleVirtualEnabler) {
        Fixture fixture = new Fixture(bot, Kind.TIGIH, false);
        PhysicalCard battleSite = fixture.addLocation(
                OTHER_SITE, OTHER_SITE_ID,
                fixture.player);
        PhysicalCard ourCharacter = fixture.addActive(
                DISTRACTOR, DISTRACTOR_ID,
                fixture.player, battleSite);
        PhysicalCard opponent = fixture.addActive(
                IMPERIAL, EMPEROR_ID,
                fixture.opponent, battleSite);
        fixture.addActive(
                IFTC, IFTC_ID,
                fixture.player, null);
        if (soleVirtualEnabler) {
            PhysicalCard hut = fixture.addLocation(
                    VIRTUAL_HUT, HUT_ID,
                    fixture.player);
            PhysicalCard landing = fixture.addLocation(
                    LANDING_PLATFORM, LANDING_ID,
                    fixture.player);
            PhysicalCard luke = fixture.addActive(
                    LUKE, LUKE_ID,
                    fixture.player, hut);
            PhysicalCard imperial = fixture.addActive(
                    IMPERIAL, IMPERIAL_ID,
                    fixture.opponent, landing);
            fixture.markPersona(luke, Persona.LUKE);
            fixture.allowEscort(imperial, luke);
            fixture.allowLocationTextMove(
                    luke, hut, landing);
            fixture.place(opponent, landing);
        }
        assertNotNull(ourCharacter);
        return fixture;
    }

    private static Fixture stableBhhmFixture(
            Bot bot, boolean vaderPresent) {
        Fixture fixture = new Fixture(bot, Kind.BHBM, true);
        PhysicalCard destination = fixture.addLocation(
                THRONE_ROOM, THRONE_ID,
                fixture.player);
        PhysicalCard origin = fixture.addLocation(
                DSII_CHASM_WALKWAY,
                DSII_CHASM_WALKWAY_ID,
                fixture.player);
        PhysicalCard luke = fixture.addActive(
                LUKE, LUKE_ID,
                fixture.opponent, origin);
        PhysicalCard vader = fixture.addActive(
                VADER, VADER_ID,
                fixture.player, origin);
        fixture.markPersona(luke, Persona.LUKE);
        fixture.markPersona(vader, Persona.VADER);
        fixture.excludeFromBattle(luke);
        fixture.excludeFromBattle(vader);
        fixture.allowLandspeedMove(
                vader, origin, destination, 1.0f);
        if (!vaderPresent) {
            fixture.place(vader, destination);
        }
        return fixture;
    }

    private static Fixture stableBhbmCarrierFixture(
            Bot bot, boolean duplicateVaderPresent) {
        Fixture fixture =
                new Fixture(bot, Kind.BHBM, true);
        PhysicalCard destination = fixture.addLocation(
                THRONE_ROOM, THRONE_ID,
                fixture.player);
        PhysicalCard origin = fixture.addLocation(
                DSII_CHASM_WALKWAY,
                DSII_CHASM_WALKWAY_ID,
                fixture.player);
        PhysicalCard luke = fixture.addActive(
                LUKE, LUKE_ID,
                fixture.opponent, origin);
        // Bantha is an open vehicle, so Vader aboard remains present with
        // Luke outside at the same site. An enclosed carrier would not.
        PhysicalCard carrier = fixture.addActive(
                BANTHA, CARRIER_ID,
                fixture.player, origin);
        PhysicalCard vader = fixture.addActive(
                VADER, VADER_ID,
                fixture.player, origin);
        fixture.addActive(
                DISTRACTOR, DISTRACTOR_ID,
                fixture.player, origin);
        fixture.markPersona(luke, Persona.LUKE);
        fixture.markPersona(vader, Persona.VADER);
        when(vader.getAttachedTo())
                .thenReturn(carrier);
        fixture.allowLandspeedMove(
                carrier, origin, destination, 1.0f);
        if (duplicateVaderPresent) {
            PhysicalCard duplicate =
                    fixture.addActive(
                        VADER, VADER_DUPLICATE_ID,
                        fixture.player, origin);
            fixture.markPersona(
                    duplicate, Persona.VADER);
        }
        return fixture;
    }

    private static Fixture bhbmPayoffFixture(Bot bot) {
        Fixture fixture = new Fixture(bot, Kind.BHBM, true);
        PhysicalCard throne = fixture.addLocation(
                THRONE_ROOM, THRONE_ID,
                fixture.player);
        PhysicalCard origin = fixture.addLocation(
                DSII_CHASM_WALKWAY,
                DSII_CHASM_WALKWAY_ID,
                fixture.player);
        PhysicalCard wrong = fixture.addLocation(
                DSII_TURBOLIFT_WALKWAY,
                DSII_TURBOLIFT_WALKWAY_ID,
                fixture.player);
        PhysicalCard luke = fixture.addActive(
                LUKE, LUKE_ID,
                fixture.opponent, origin);
        PhysicalCard vader = fixture.addActive(
                VADER, VADER_ID,
                fixture.player, origin);
        PhysicalCard emperor = fixture.addActive(
                EMPEROR, EMPEROR_ID,
                fixture.player, origin);
        fixture.markPersona(luke, Persona.LUKE);
        fixture.markPersona(vader, Persona.VADER);
        fixture.markPersona(emperor, Persona.SIDIOUS);
        fixture.allowDuelTargetByObjective(luke);
        when(luke.isCaptive()).thenReturn(true);
        when(luke.getEscort()).thenReturn(vader);
        when(vader.getCardsEscorting()).thenReturn(
                List.of(luke));
        fixture.allowLandspeedMove(
                vader, origin, throne, 1.0f);
        fixture.allowLandspeedMove(
                vader, origin, wrong, 1.0f);
        fixture.allowLandspeedMove(
                emperor, origin, throne, 1.0f);
        fixture.allowLandspeedMove(
                emperor, origin, wrong, 1.0f);
        return fixture;
    }

    private static Fixture bhbmForceDripDeployFixture(
            Bot bot) {
        Fixture fixture =
                new Fixture(bot, Kind.BHBM, true);
        PhysicalCard throne = fixture.addLocation(
                THRONE_ROOM, THRONE_ID,
                fixture.player);
        fixture.addLocation(
                DSII_CHASM_WALKWAY,
                DSII_CHASM_WALKWAY_ID,
                fixture.player);
        PhysicalCard vader = fixture.addActive(
                VADER, VADER_ID,
                fixture.player, throne);
        PhysicalCard luke = fixture.addActive(
                LUKE, LUKE_ID,
                fixture.opponent, throne);
        PhysicalCard emperor = fixture.addHand(
                EMPEROR, EMPEROR_ID);
        fixture.markPersona(vader, Persona.VADER);
        fixture.markPersona(luke, Persona.LUKE);
        fixture.markPersona(emperor, Persona.SIDIOUS);
        when(luke.isCaptive()).thenReturn(true);
        when(luke.getEscort()).thenReturn(vader);
        when(vader.getCardsEscorting()).thenReturn(
                List.of(luke));
        fixture.allowDuelTargetByObjective(luke);
        fixture.setDeployable(true);
        return fixture;
    }

    private static Fixture bhbmForceDripReserveDeployFixture(
            Bot bot) {
        Fixture fixture =
                new Fixture(bot, Kind.BHBM, true);
        PhysicalCard throne = fixture.addLocation(
                THRONE_ROOM, THRONE_ID,
                fixture.player);
        fixture.addLocation(
                DSII_CHASM_WALKWAY,
                DSII_CHASM_WALKWAY_ID,
                fixture.player);
        PhysicalCard vader = fixture.addActive(
                VADER, VADER_ID,
                fixture.player, throne);
        PhysicalCard luke = fixture.addActive(
                LUKE, LUKE_ID,
                fixture.opponent, throne);
        PhysicalCard emperor = fixture.addReserve(
                EMPEROR, EMPEROR_ID);
        fixture.markPersona(vader, Persona.VADER);
        fixture.markPersona(luke, Persona.LUKE);
        fixture.markPersona(emperor, Persona.SIDIOUS);
        when(luke.isCaptive()).thenReturn(true);
        when(luke.getEscort()).thenReturn(vader);
        when(vader.getCardsEscorting()).thenReturn(
                List.of(luke));
        fixture.allowDuelTargetByObjective(luke);
        fixture.setDeployable(true);
        return fixture;
    }

    private static Fixture tigihCrossoverFixture(Bot bot) {
        Fixture fixture = new Fixture(bot, Kind.TIGIH, true);
        PhysicalCard site = fixture.addLocation(
                OTHER_SITE, OTHER_SITE_ID,
                fixture.player);
        PhysicalCard luke = fixture.addActive(
                LUKE, LUKE_ID,
                fixture.player, site);
        PhysicalCard vader = fixture.addActive(
                VADER, VADER_ID,
                fixture.opponent, site);
        fixture.markPersona(luke, Persona.LUKE);
        fixture.markPersona(vader, Persona.VADER);
        return fixture;
    }

    private static Outcome combined(
            Fixture fixture, Decision decision) {
        if (fixture.bot == Bot.RANDO) {
            return outcome(
                    new com.gempukku.swccgo.ai.models.rando
                            .evaluators.CombinedEvaluator()
                            .evaluateDecision(
                                randoContext(
                                    fixture, decision)));
        }
        return outcome(
                new com.gempukku.swccgo.ai.models.chosenone
                        .evaluators.CombinedEvaluator()
                        .evaluateDecision(
                            chosenContext(
                                fixture, decision)));
    }

    private static TracedOutcome tracedCombined(
            Fixture fixture,
            Decision decision) {
        List<String> rawCandidates =
                !decision.actionIds().isEmpty()
                    ? decision.actionIds()
                    : decision.cardIds();
        assertTrue(TraceSession.open(
                fixture.bot.name(),
                "capture-objective-production-decision",
                decision.type(), decision.text(),
                rawCandidates, null,
                List.of(
                    "focused evaluator fixture omits "
                        + "the bot-boundary snapshot"),
                false));
        Outcome outcome;
        DecisionTrace trace;
        try {
            outcome = combined(
                    fixture, decision);
        } finally {
            trace = TraceSession.close();
        }
        assertNotNull(trace);
        return new TracedOutcome(
                outcome, trace);
    }

    private static List<Outcome> actionTextAdapter(
            Fixture fixture, Decision decision) {
        if (fixture.bot == Bot.RANDO) {
            return new com.gempukku.swccgo.ai.models.rando
                    .evaluators.ActionTextEvaluator()
                    .evaluate(randoContext(fixture, decision))
                    .stream()
                    .map(CaptureObjectiveMirroredEvaluatorBehaviorHarnessTest
                            ::outcome)
                    .toList();
        }
        return new com.gempukku.swccgo.ai.models.chosenone
                .evaluators.ActionTextEvaluator()
                .evaluate(chosenContext(fixture, decision))
                .stream()
                .map(CaptureObjectiveMirroredEvaluatorBehaviorHarnessTest
                        ::outcome)
                .toList();
    }

    private static List<Outcome> cardSelectionAdapter(
            Fixture fixture, Decision decision) {
        if (fixture.bot == Bot.RANDO) {
            return new com.gempukku.swccgo.ai.models.rando
                    .evaluators.CardSelectionEvaluator()
                    .evaluate(randoContext(fixture, decision))
                    .stream()
                    .map(CaptureObjectiveMirroredEvaluatorBehaviorHarnessTest
                            ::outcome)
                    .toList();
        }
        return new com.gempukku.swccgo.ai.models.chosenone
                .evaluators.CardSelectionEvaluator()
                .evaluate(chosenContext(fixture, decision))
                .stream()
                .map(CaptureObjectiveMirroredEvaluatorBehaviorHarnessTest
                        ::outcome)
                .toList();
    }

    private static List<Outcome> moveAdapter(
            Fixture fixture, Decision decision) {
        if (fixture.bot == Bot.RANDO) {
            return new com.gempukku.swccgo.ai.models.rando
                    .evaluators.MoveEvaluator()
                    .evaluate(randoContext(fixture, decision))
                    .stream()
                    .map(CaptureObjectiveMirroredEvaluatorBehaviorHarnessTest
                            ::outcome)
                    .toList();
        }
        return new com.gempukku.swccgo.ai.models.chosenone
                .evaluators.MoveEvaluator()
                .evaluate(chosenContext(fixture, decision))
                .stream()
                .map(CaptureObjectiveMirroredEvaluatorBehaviorHarnessTest
                        ::outcome)
                .toList();
    }

    private static List<Outcome> captureMovementAdapter(
            Fixture fixture, Decision decision) {
        if (fixture.bot == Bot.RANDO) {
            return new com.gempukku.swccgo.ai.models.rando
                    .evaluators.CaptureMovementEvaluator()
                    .evaluate(randoContext(fixture, decision))
                    .stream()
                    .map(CaptureObjectiveMirroredEvaluatorBehaviorHarnessTest
                            ::outcome)
                    .toList();
        }
        return new com.gempukku.swccgo.ai.models.chosenone
                .evaluators.CaptureMovementEvaluator()
                .evaluate(chosenContext(fixture, decision))
                .stream()
                .map(CaptureObjectiveMirroredEvaluatorBehaviorHarnessTest
                        ::outcome)
                .toList();
    }

    private static List<Outcome> deployAdapter(
            Fixture fixture, Decision decision) {
        if (fixture.bot == Bot.RANDO) {
            return new com.gempukku.swccgo.ai.models.rando
                    .evaluators.DeployEvaluator()
                    .evaluate(randoContext(fixture, decision))
                    .stream()
                    .map(CaptureObjectiveMirroredEvaluatorBehaviorHarnessTest
                            ::outcome)
                    .toList();
        }
        return new com.gempukku.swccgo.ai.models.chosenone
                .evaluators.DeployEvaluator()
                .evaluate(chosenContext(fixture, decision))
                .stream()
                .map(CaptureObjectiveMirroredEvaluatorBehaviorHarnessTest
                        ::outcome)
                .toList();
    }

    private static List<Outcome> battleAdapter(
            Fixture fixture, Decision decision) {
        if (fixture.bot == Bot.RANDO) {
            return new com.gempukku.swccgo.ai.models.rando
                    .evaluators.BattleEvaluator()
                    .evaluate(randoContext(fixture, decision))
                    .stream()
                    .map(CaptureObjectiveMirroredEvaluatorBehaviorHarnessTest
                            ::outcome)
                    .toList();
        }
        return new com.gempukku.swccgo.ai.models.chosenone
                .evaluators.BattleEvaluator()
                .evaluate(chosenContext(fixture, decision))
                .stream()
                .map(CaptureObjectiveMirroredEvaluatorBehaviorHarnessTest
                        ::outcome)
                .toList();
    }

    private static AwaitingDecision engineCardActionDecision(
            Fixture fixture,
            Decision decision,
            boolean ruleAction,
            PhysicalCard trueActionSource) {
        List<Action> actions =
                new ArrayList<>();
        for (int i = 0;
                i < decision.cardIds().size(); i++) {
            PhysicalCard attached =
                    fixture.card(Integer.parseInt(
                        decision.cardIds().get(i)));
            Action action = mock(Action.class);
            when(action.getActionAttachedToCard())
                    .thenReturn(attached);
            when(action.getActionSource())
                    .thenReturn(ruleAction
                        ? null
                        : trueActionSource != null
                            ? trueActionSource : attached);
            when(action.getText())
                    .thenReturn(
                        decision.actionTexts().get(i));
            actions.add(action);
        }
        return new CardActionSelectionDecision(
                91, decision.text(), actions,
                true, false,
                decision.noPass(), false, false) {
            @Override
            public void decisionMade(String result)
                    throws DecisionResultInvalidException {
            }
        };
    }

    private static List<Outcome>
            liveDeployChildAfterParentSelection(
                    Fixture fixture,
                    Decision parent,
                    String selectedActionId,
                    PhysicalCard selectedCopy,
                    PhysicalCard firstCopy,
                    Decision child)
                    throws Exception {
        int selectedIndex =
                parent.actionIds().indexOf(
                    selectedActionId);
        assertTrue(selectedIndex >= 0);
        List<String> wireActionIds =
                new ArrayList<>();
        for (int i = 0;
                i < parent.actionIds().size(); i++) {
            wireActionIds.add(String.valueOf(i));
        }
        AwaitingDecision engineParent =
                engineCardActionDecision(
                    fixture, parent, false, null);
        Object ai;
        Object parentContext;
        Object selected;
        if (fixture.bot == Bot.RANDO) {
            ai = new com.gempukku.swccgo.ai.models.rando
                    .RandoCalAi();
            var typedParent = randoContext(
                    fixture, parent);
            typedParent.setActionIds(wireActionIds);
            parentContext = typedParent;
            selected =
                    new com.gempukku.swccgo.ai.models.rando
                        .evaluators.EvaluatedAction(
                            String.valueOf(selectedIndex),
                            com.gempukku.swccgo.ai.models.rando
                                .evaluators.ActionType.DEPLOY,
                            0.0f,
                            "selected duplicate Vader");
        } else {
            ai = new com.gempukku.swccgo.ai.models.chosenone
                    .TheChosenOneAi();
            var typedParent = chosenContext(
                    fixture, parent);
            typedParent.setActionIds(wireActionIds);
            parentContext = typedParent;
            selected =
                    new com.gempukku.swccgo.ai.models.chosenone
                        .evaluators.EvaluatedAction(
                            String.valueOf(selectedIndex),
                            com.gempukku.swccgo.ai.models.chosenone
                                .evaluators.ActionType.DEPLOY,
                            0.0f,
                            "selected duplicate Vader");
        }

        Method remember = ai.getClass().getDeclaredMethod(
                "rememberSelectedDeployCard",
                parentContext.getClass(),
                selected.getClass(),
                AwaitingDecision.class);
        remember.setAccessible(true);
        remember.invoke(
                ai, parentContext, selected,
                engineParent);

        AwaitingDecision childDecision =
                mock(AwaitingDecision.class);
        when(childDecision.getDecisionType())
                .thenReturn(
                    AwaitingDecisionType.CARD_SELECTION);
        when(childDecision.getText())
                .thenReturn(child.text());
        when(childDecision.getAwaitingDecisionId())
                .thenReturn(92);
        when(childDecision.getDecisionParameters())
                .thenReturn(Map.of());

        Method builder = ai.getClass().getDeclaredMethod(
                "buildEvaluatorContext",
                String.class,
                AwaitingDecision.class,
                GameState.class,
                boolean.class);
        builder.setAccessible(true);
        Object built = builder.invoke(
                ai, fixture.player,
                childDecision, fixture.gameState, false);
        Method getExtra = built.getClass().getMethod(
                "getExtra", String.class);
        Object latched = getExtra.invoke(
                built,
                ObjectiveAnalyzer
                    .OBJECTIVE_DEPLOYING_CARD_ID_EXTRA);
        assertEquals(
                selectedCopy.getPermanentCardId(),
                latched);
        assertFalse(
                "Duplicate blueprint must not substitute"
                        + " the unselected physical Vader",
                Integer.valueOf(
                    firstCopy.getPermanentCardId())
                    .equals(latched));
        assertEquals(
                selectedCopy.getPermanentCardId(),
                getExtra.invoke(
                    built,
                    BhbmForceDripUrgencyFactsReader
                        .ACTION_SOURCE_PERMANENT_CARD_ID_EXTRA));

        List<Outcome> outcomes;
        if (fixture.bot == Bot.RANDO) {
            var context =
                    (com.gempukku.swccgo.ai.models.rando
                        .evaluators.DecisionContext) built;
            context.setGame(fixture.game);
            context.setSide(fixture.side);
            context.setObjectiveAnalyzer(
                    (com.gempukku.swccgo.ai.models.rando
                        .strategy.ObjectiveAnalyzer)
                        fixture.analyzer);
            apply(context, child);
            outcomes =
                    new com.gempukku.swccgo.ai.models.rando
                        .evaluators.CardSelectionEvaluator()
                        .evaluate(context).stream()
                        .map(CaptureObjectiveMirroredEvaluatorBehaviorHarnessTest
                            ::outcome)
                        .toList();
        } else {
            var context =
                    (com.gempukku.swccgo.ai.models.chosenone
                        .evaluators.DecisionContext) built;
            context.setGame(fixture.game);
            context.setSide(fixture.side);
            context.setObjectiveAnalyzer(
                    (com.gempukku.swccgo.ai.models.chosenone
                        .strategy.ObjectiveAnalyzer)
                        fixture.analyzer);
            apply(context, child);
            outcomes =
                    new com.gempukku.swccgo.ai.models.chosenone
                        .evaluators.CardSelectionEvaluator()
                        .evaluate(context).stream()
                        .map(CaptureObjectiveMirroredEvaluatorBehaviorHarnessTest
                            ::outcome)
                        .toList();
        }

        Object next = builder.invoke(
                ai, fixture.player,
                childDecision, fixture.gameState, false);
        assertNull(getExtra.invoke(
                next,
                ObjectiveAnalyzer
                    .OBJECTIVE_DEPLOYING_CARD_ID_EXTRA));
        assertNull(getExtra.invoke(
                next,
                BhbmForceDripUrgencyFactsReader
                    .ACTION_SOURCE_PERMANENT_CARD_ID_EXTRA));
        return outcomes;
    }

    private static List<Outcome>
            liveReserveDeployChildAfterObjectiveAction(
                    Fixture fixture,
                    Decision parent,
                    PhysicalCard attachedCandidate,
                    PhysicalCard trueActionSource,
                    Decision child)
                    throws Exception {
        AwaitingDecision engineParent =
                engineCardActionDecision(
                    fixture, parent, false,
                    trueActionSource);
        Object ai;
        Object parentContext;
        Object selected;
        if (fixture.bot == Bot.RANDO) {
            ai = new com.gempukku.swccgo.ai.models.rando
                    .RandoCalAi();
            var typedParent = randoContext(
                    fixture, parent);
            typedParent.setActionIds(List.of("0"));
            parentContext = typedParent;
            selected =
                    new com.gempukku.swccgo.ai.models.rando
                        .evaluators.EvaluatedAction(
                            "0",
                            com.gempukku.swccgo.ai.models.rando
                                .evaluators.ActionType.DEPLOY,
                            0.0f,
                            "objective deploy source");
        } else {
            ai = new com.gempukku.swccgo.ai.models.chosenone
                    .TheChosenOneAi();
            var typedParent = chosenContext(
                    fixture, parent);
            typedParent.setActionIds(List.of("0"));
            parentContext = typedParent;
            selected =
                    new com.gempukku.swccgo.ai.models.chosenone
                        .evaluators.EvaluatedAction(
                            "0",
                            com.gempukku.swccgo.ai.models.chosenone
                                .evaluators.ActionType.DEPLOY,
                            0.0f,
                            "objective deploy source");
        }

        Method remember = ai.getClass().getDeclaredMethod(
                "rememberSelectedDeployCard",
                parentContext.getClass(),
                selected.getClass(),
                AwaitingDecision.class);
        remember.setAccessible(true);
        remember.invoke(
                ai, parentContext, selected,
                engineParent);

        AwaitingDecision childDecision =
                mock(AwaitingDecision.class);
        when(childDecision.getDecisionType())
                .thenReturn(
                    AwaitingDecisionType.CARD_SELECTION);
        when(childDecision.getText())
                .thenReturn(child.text());
        when(childDecision.getAwaitingDecisionId())
                .thenReturn(94);
        when(childDecision.getDecisionParameters())
                .thenReturn(Map.of());

        Method builder = ai.getClass().getDeclaredMethod(
                "buildEvaluatorContext",
                String.class,
                AwaitingDecision.class,
                GameState.class,
                boolean.class);
        builder.setAccessible(true);
        Object built = builder.invoke(
                ai, fixture.player,
                childDecision, fixture.gameState, false);
        Method getExtra = built.getClass().getMethod(
                "getExtra", String.class);
        assertNull(getExtra.invoke(
                built,
                ObjectiveAnalyzer
                    .OBJECTIVE_DEPLOYING_CARD_ID_EXTRA));
        assertEquals(
                trueActionSource.getPermanentCardId(),
                getExtra.invoke(
                    built,
                    BhbmForceDripUrgencyFactsReader
                        .ACTION_SOURCE_PERMANENT_CARD_ID_EXTRA));
        assertFalse(
                "Engine attached card must remain distinct"
                        + " from the true deploy source",
                attachedCandidate == trueActionSource);

        List<Outcome> outcomes;
        if (fixture.bot == Bot.RANDO) {
            var context =
                    (com.gempukku.swccgo.ai.models.rando
                        .evaluators.DecisionContext) built;
            context.setGame(fixture.game);
            context.setSide(fixture.side);
            context.setObjectiveAnalyzer(
                    (com.gempukku.swccgo.ai.models.rando
                        .strategy.ObjectiveAnalyzer)
                        fixture.analyzer);
            apply(context, child);
            outcomes =
                    new com.gempukku.swccgo.ai.models.rando
                        .evaluators.CardSelectionEvaluator()
                        .evaluate(context).stream()
                        .map(CaptureObjectiveMirroredEvaluatorBehaviorHarnessTest
                            ::outcome)
                        .toList();
        } else {
            var context =
                    (com.gempukku.swccgo.ai.models.chosenone
                        .evaluators.DecisionContext) built;
            context.setGame(fixture.game);
            context.setSide(fixture.side);
            context.setObjectiveAnalyzer(
                    (com.gempukku.swccgo.ai.models.chosenone
                        .strategy.ObjectiveAnalyzer)
                        fixture.analyzer);
            apply(context, child);
            outcomes =
                    new com.gempukku.swccgo.ai.models.chosenone
                        .evaluators.CardSelectionEvaluator()
                        .evaluate(context).stream()
                        .map(CaptureObjectiveMirroredEvaluatorBehaviorHarnessTest
                            ::outcome)
                        .toList();
        }

        Object next = builder.invoke(
                ai, fixture.player,
                childDecision, fixture.gameState, false);
        assertNull(getExtra.invoke(
                next,
                BhbmForceDripUrgencyFactsReader
                    .ACTION_SOURCE_PERMANENT_CARD_ID_EXTRA));
        return outcomes;
    }

    private static List<Outcome>
            liveMoveChildAfterParentSelection(
                    Fixture fixture,
                    Decision parent,
                    Decision child)
                    throws Exception {
        AwaitingDecision engineParent =
                engineCardActionDecision(
                    fixture, parent, true, null);
        Object ai;
        Object parentContext;
        Object selected;
        if (fixture.bot == Bot.RANDO) {
            ai = new com.gempukku.swccgo.ai.models.rando
                    .RandoCalAi();
            var typedParent = randoContext(
                    fixture, parent);
            typedParent.setActionIds(List.of("0"));
            parentContext = typedParent;
            selected =
                    new com.gempukku.swccgo.ai.models.rando
                        .evaluators.EvaluatedAction(
                            "0",
                            com.gempukku.swccgo.ai.models.rando
                                .evaluators.ActionType.MOVE,
                            0.0f,
                            "selected landspeed mover");
        } else {
            ai = new com.gempukku.swccgo.ai.models.chosenone
                    .TheChosenOneAi();
            var typedParent = chosenContext(
                    fixture, parent);
            typedParent.setActionIds(List.of("0"));
            parentContext = typedParent;
            selected =
                    new com.gempukku.swccgo.ai.models.chosenone
                        .evaluators.EvaluatedAction(
                            "0",
                            com.gempukku.swccgo.ai.models.chosenone
                                .evaluators.ActionType.MOVE,
                            0.0f,
                            "selected landspeed mover");
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
                engineParent);

        AwaitingDecision childDecision =
                mock(AwaitingDecision.class);
        when(childDecision.getDecisionType())
                .thenReturn(
                    AwaitingDecisionType.CARD_SELECTION);
        when(childDecision.getText())
                .thenReturn(child.text());
        when(childDecision.getAwaitingDecisionId())
                .thenReturn(93);
        when(childDecision.getDecisionParameters())
                .thenReturn(Map.of());

        Method builder = ai.getClass().getDeclaredMethod(
                "buildEvaluatorContext",
                String.class,
                AwaitingDecision.class,
                GameState.class,
                boolean.class);
        builder.setAccessible(true);
        Object built = builder.invoke(
                ai, fixture.player,
                childDecision, fixture.gameState, false);
        Method getExtra = built.getClass().getMethod(
                "getExtra", String.class);
        assertEquals(
                VADER_ID,
                getExtra.invoke(
                    built,
                    MovePhysicalCardResolver
                        .MOVER_CARD_ID_EXTRA));
        assertEquals(
                VADER_ID,
                getExtra.invoke(
                    built,
                    BhbmForceDripUrgencyFactsReader
                        .ACTION_SOURCE_PERMANENT_CARD_ID_EXTRA));

        List<Outcome> outcomes;
        if (fixture.bot == Bot.RANDO) {
            var context =
                    (com.gempukku.swccgo.ai.models.rando
                        .evaluators.DecisionContext) built;
            context.setGame(fixture.game);
            context.setSide(fixture.side);
            context.setObjectiveAnalyzer(
                    (com.gempukku.swccgo.ai.models.rando
                        .strategy.ObjectiveAnalyzer)
                        fixture.analyzer);
            apply(context, child);
            outcomes =
                    new com.gempukku.swccgo.ai.models.rando
                        .evaluators.CardSelectionEvaluator()
                        .evaluate(context).stream()
                        .map(CaptureObjectiveMirroredEvaluatorBehaviorHarnessTest
                            ::outcome)
                        .toList();
        } else {
            var context =
                    (com.gempukku.swccgo.ai.models.chosenone
                        .evaluators.DecisionContext) built;
            context.setGame(fixture.game);
            context.setSide(fixture.side);
            context.setObjectiveAnalyzer(
                    (com.gempukku.swccgo.ai.models.chosenone
                        .strategy.ObjectiveAnalyzer)
                        fixture.analyzer);
            apply(context, child);
            outcomes =
                    new com.gempukku.swccgo.ai.models.chosenone
                        .evaluators.CardSelectionEvaluator()
                        .evaluate(context).stream()
                        .map(CaptureObjectiveMirroredEvaluatorBehaviorHarnessTest
                            ::outcome)
                        .toList();
        }

        Object next = builder.invoke(
                ai, fixture.player,
                childDecision, fixture.gameState, false);
        assertNull(getExtra.invoke(
                next,
                MovePhysicalCardResolver
                    .MOVER_CARD_ID_EXTRA));
        assertNull(getExtra.invoke(
                next,
                BhbmForceDripUrgencyFactsReader
                    .ACTION_SOURCE_PERMANENT_CARD_ID_EXTRA));
        return outcomes;
    }

    private static com.gempukku.swccgo.ai.models.rando
            .evaluators.DecisionContext randoContext(
                    Fixture fixture, Decision decision) {
        var context =
                new com.gempukku.swccgo.ai.models.rando
                    .evaluators.DecisionContext(
                        fixture.gameState,
                        fixture.player,
                        decision.type(),
                        decision.text(),
                        "capture-behavior",
                        decision.phase());
        context.setGame(fixture.game);
        context.setSide(fixture.side);
        context.setObjectiveAnalyzer(
                (com.gempukku.swccgo.ai.models.rando
                    .strategy.ObjectiveAnalyzer)
                    fixture.analyzer);
        context.setDeckOracle(
                mock(com.gempukku.swccgo.ai.models.rando
                    .strategy.DeckOracle.class));
        apply(context, decision);
        context.setExtra(
                com.gempukku.swccgo.ai.models.common.phase
                    .CaptureDeployBudgetFactsReader
                    .ACTION_PAYMENTS_EXTRA,
                fixture.exactNormalDeployActionPayments(
                    decision));
        return context;
    }

    private static com.gempukku.swccgo.ai.models.chosenone
            .evaluators.DecisionContext chosenContext(
                    Fixture fixture, Decision decision) {
        var context =
                new com.gempukku.swccgo.ai.models.chosenone
                    .evaluators.DecisionContext(
                        fixture.gameState,
                        fixture.player,
                        decision.type(),
                        decision.text(),
                        "capture-behavior",
                        decision.phase());
        context.setGame(fixture.game);
        context.setSide(fixture.side);
        context.setObjectiveAnalyzer(
                (com.gempukku.swccgo.ai.models.chosenone
                    .strategy.ObjectiveAnalyzer)
                    fixture.analyzer);
        context.setDeckOracle(
                mock(com.gempukku.swccgo.ai.models.chosenone
                    .strategy.DeckOracle.class));
        apply(context, decision);
        context.setExtra(
                com.gempukku.swccgo.ai.models.common.phase
                    .CaptureDeployBudgetFactsReader
                    .ACTION_PAYMENTS_EXTRA,
                fixture.exactNormalDeployActionPayments(
                    decision));
        return context;
    }

    private static void apply(
            com.gempukku.swccgo.ai.models.rando
                    .evaluators.DecisionContext context,
            Decision decision) {
        context.setActionIds(decision.actionIds());
        context.setActionTexts(decision.actionTexts());
        context.setCardIds(decision.cardIds());
        context.setBlueprints(decision.blueprints());
        context.setTestingTexts(decision.testingTexts());
        context.setSelectable(decision.selectable());
        context.setNoPass(decision.noPass());
        context.setMin(decision.min());
        context.setMax(1);
        if (decision.moverId() != null) {
            context.setExtra(
                    MovePhysicalCardResolver
                        .MOVER_CARD_ID_EXTRA,
                    decision.moverId());
        }
        if (decision.deployingId() != null) {
            context.setExtra(
                    ObjectiveAnalyzer
                        .OBJECTIVE_DEPLOYING_CARD_ID_EXTRA,
                    decision.deployingId());
        }
        if (decision.actionSourceId() != null) {
            context.setExtra(
                    BhbmForceDripUrgencyFactsReader
                        .ACTION_SOURCE_PERMANENT_CARD_ID_EXTRA,
                    decision.actionSourceId());
        }
    }

    private static void apply(
            com.gempukku.swccgo.ai.models.chosenone
                    .evaluators.DecisionContext context,
            Decision decision) {
        context.setActionIds(decision.actionIds());
        context.setActionTexts(decision.actionTexts());
        context.setCardIds(decision.cardIds());
        context.setBlueprints(decision.blueprints());
        context.setTestingTexts(decision.testingTexts());
        context.setSelectable(decision.selectable());
        context.setNoPass(decision.noPass());
        context.setMin(decision.min());
        context.setMax(1);
        if (decision.moverId() != null) {
            context.setExtra(
                    MovePhysicalCardResolver
                        .MOVER_CARD_ID_EXTRA,
                    decision.moverId());
        }
        if (decision.deployingId() != null) {
            context.setExtra(
                    ObjectiveAnalyzer
                        .OBJECTIVE_DEPLOYING_CARD_ID_EXTRA,
                    decision.deployingId());
        }
        if (decision.actionSourceId() != null) {
            context.setExtra(
                    BhbmForceDripUrgencyFactsReader
                        .ACTION_SOURCE_PERMANENT_CARD_ID_EXTRA,
                    decision.actionSourceId());
        }
    }

    private static Outcome outcome(
            com.gempukku.swccgo.ai.models.rando
                    .evaluators.EvaluatedAction action) {
        assertNotNull(action);
        return new Outcome(
                action.getActionId(),
                action.getActionType().name(),
                action.getScore(),
                List.copyOf(action.getReasoning()),
                action.isHardVetoed(),
                action.getVetoReason());
    }

    private static Outcome outcome(
            com.gempukku.swccgo.ai.models.chosenone
                    .evaluators.EvaluatedAction action) {
        assertNotNull(action);
        return new Outcome(
                action.getActionId(),
                action.getActionType().name(),
                action.getScore(),
                List.copyOf(action.getReasoning()),
                action.isHardVetoed(),
                action.getVetoReason());
    }

    private static Outcome only(
            List<Outcome> actions, String actionId) {
        return actions.stream()
                .filter(action ->
                    actionId.equals(action.actionId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                    "Missing action " + actionId
                            + " in " + actions));
    }

    private static void assertContains(
            Outcome outcome, String marker) {
        assertTrue(
                "Expected '" + marker + "' in "
                        + outcome,
                outcome.allText().contains(marker));
    }

    private static void assertNotContains(
            Outcome outcome, String marker) {
        assertFalse(
                "Did not expect '" + marker + "' in "
                        + outcome,
                outcome.allText().contains(marker));
    }

    private static void assertReasoningCount(
            Outcome outcome,
            String marker,
            long expectedCount) {
        long actual = outcome.reasoning()
                .stream()
                .filter(reason ->
                    reason.contains(marker))
                .count();
        assertEquals(
                "Unexpected reasoning count for '"
                    + marker + "' in " + outcome,
                expectedCount, actual);
    }

    private static void assertTypedContribution(
            DecisionTrace trace,
            String actionId,
            String ruleId,
            int expectedCount,
            float expectedTotal) {
        var contributions =
                trace.getOperations().stream()
                    .filter(operation ->
                        actionId.equals(
                            operation.getActionId())
                        && ruleId.equals(
                            operation.getRuleId().id())
                        && operation.getDeltaBits()
                            != null)
                    .toList();
        assertEquals(
                "Unexpected contribution count for "
                    + ruleId + " on " + actionId
                    + " in " + trace.getOperations(),
                expectedCount,
                contributions.size());
        float total = 0.0f;
        for (var contribution
                : contributions) {
            total += Float.intBitsToFloat(
                    contribution.getDeltaBits());
        }
        assertEquals(
                "Unexpected contribution total for "
                    + ruleId + " on " + actionId,
                expectedTotal, total, 0.0f);
    }

    private static void assertNoTypedRule(
            DecisionTrace trace,
            String actionId,
            String ruleId) {
        assertTrue(
                "Did not expect typed rule '"
                    + ruleId + "' for "
                    + actionId + " in "
                    + trace.getOperations(),
                trace.getOperations().stream()
                    .noneMatch(operation ->
                        actionId.equals(
                            operation.getActionId())
                        && ruleId.equals(
                            operation.getRuleId()
                                .id())));
    }

    private static void assertDeathStarIiRouteChain(
            Fixture fixture) {
        assertDeathStarIiLocationOrder(
                fixture,
                List.of(
                    DSII_DOCKING_BAY_ID,
                    THRONE_ID,
                    DSII_CHASM_WALKWAY_ID,
                    DSII_TURBOLIFT_WALKWAY_ID));
        assertEquals(
                List.of(
                    fixture.card(
                        DSII_CHASM_WALKWAY_ID)),
                fixture.modifiers.getSitesBetween(
                    fixture.gameState,
                    fixture.card(THRONE_ID),
                    fixture.card(
                        DSII_TURBOLIFT_WALKWAY_ID)));
    }

    private static void assertDeathStarIiLocationOrder(
            Fixture fixture,
            List<Integer> ids) {
        List<PhysicalCard> expected =
                new ArrayList<>();
        for (int id : ids) {
            expected.add(fixture.card(id));
        }
        assertEquals(expected,
                fixture.gameState
                    .getLocationsInOrder());
        for (int index = 0; index < ids.size(); index++) {
            int id = ids.get(index);
            assertEquals("Death Star II",
                    fixture.card(id).getPartOfSystem());
            assertEquals(index,
                    fixture.card(id)
                        .getLocationZoneIndex());
        }
    }

    private static void assertParity(
            List<Outcome> outcomes) {
        assertEquals(2, outcomes.size());
        assertEquals(outcomes.get(0), outcomes.get(1));
    }

    private static SwccgCardBlueprint blueprint(
            String blueprintId) {
        SwccgCardBlueprint blueprint =
                CARDS.getSwccgoCardBlueprint(
                    blueprintId);
        assertNotNull(
                "Missing real blueprint " + blueprintId,
                blueprint);
        return blueprint;
    }

    private enum Bot {
        RANDO,
        CHOSEN_ONE
    }

    private enum Kind {
        TIGIH("light", "dark", Side.LIGHT,
                CaptureObjectiveMirroredEvaluatorBehaviorHarnessTest.TIGIH),
        BHBM("dark", "light", Side.DARK,
                CaptureObjectiveMirroredEvaluatorBehaviorHarnessTest.BHBM);

        private final String player;
        private final String opponent;
        private final Side side;
        private final String blueprintId;

        Kind(String player, String opponent,
             Side side, String blueprintId) {
            this.player = player;
            this.opponent = opponent;
            this.side = side;
            this.blueprintId = blueprintId;
        }
    }

    private record Outcome(
            String actionId,
            String actionType,
            float score,
            List<String> reasoning,
            boolean hardVeto,
            String vetoReason) {
        private String allText() {
            return String.join(" | ", reasoning)
                    + " | "
                    + (vetoReason != null
                        ? vetoReason : "");
        }
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
            List<Boolean> selectable,
            boolean noPass,
            int min,
            Integer moverId,
            Integer deployingId,
            Integer actionSourceId) {

        private static Decision actions(
                String text,
                Phase phase,
                List<String> actionIds,
                List<String> actionTexts,
                List<String> sourceCardIds,
                boolean noPass,
                int min) {
            return new Decision(
                    "CARD_ACTION_CHOICE",
                    text, phase,
                    actionIds, actionTexts,
                    sourceCardIds,
                    List.of(), List.of(),
                    List.of(), noPass, min,
                    null, null, null);
        }

        private static Decision cards(
                String text,
                Phase phase,
                List<String> cardIds,
                List<String> blueprints,
                List<Boolean> selectable) {
            List<String> titles =
                    new ArrayList<>();
            for (String id : blueprints) {
                titles.add(
                        blueprint(id).getTitle());
            }
            return new Decision(
                    "CARD_SELECTION",
                    text, phase,
                    List.of(), List.of(),
                    cardIds, blueprints,
                    titles, selectable,
                    true, 1, null, null, null);
        }

        private Decision withMover(int cardId) {
            return new Decision(
                    type, text, phase,
                    actionIds, actionTexts,
                    cardIds, blueprints,
                    testingTexts, selectable,
                    noPass, min,
                    cardId, deployingId, cardId);
        }

        private Decision withDeploying(int cardId) {
            return new Decision(
                    type, text, phase,
                    actionIds, actionTexts,
                    cardIds, blueprints,
                    testingTexts, selectable,
                    noPass, min,
                    moverId, cardId, cardId);
        }

        private Decision withActionSource(
                int permanentCardId) {
            return new Decision(
                    type, text, phase,
                    actionIds, actionTexts,
                    cardIds, blueprints,
                    testingTexts, selectable,
                    noPass, min,
                    moverId, deployingId,
                    permanentCardId);
        }
    }

    private static final class Fixture {
        private final Bot bot;
        private final String player;
        private final String opponent;
        private final Side side;
        private final GameState gameState;
        private final SwccgGame game;
        private final ModifiersQuerying modifiers;
        private final ObjectiveAnalyzer analyzer;
        private final List<PhysicalCard> hand =
                new ArrayList<>();
        private final List<PhysicalCard> reserve =
                new ArrayList<>();
        private final List<PhysicalCard> lost =
                new ArrayList<>();
        private final List<PhysicalCard> permanents =
                new ArrayList<>();
        private final List<PhysicalCard> stacked =
                new ArrayList<>();
        private final List<PhysicalCard> locations =
                new ArrayList<>();
        private final Map<Integer, PhysicalCard> cards =
                new LinkedHashMap<>();
        private final Map<PhysicalCard, PhysicalCard>
                locationsByCard =
                    new IdentityHashMap<>();
        private final Map<PhysicalCard, Set<Persona>>
                personas =
                    new IdentityHashMap<>();
        private final Set<PhysicalCard> active =
                Collections.newSetFromMap(
                    new IdentityHashMap<>());
        private final Set<PhysicalCard> excluded =
                Collections.newSetFromMap(
                    new IdentityHashMap<>());
        private final Set<String> legalLandspeed =
                new java.util.HashSet<>();
        private final Map<String, Float> landspeedCosts =
                new LinkedHashMap<>();

        private Fixture(
                Bot bot, Kind kind, boolean flipped) {
            this.bot = bot;
            player = kind.player;
            opponent = kind.opponent;
            side = kind.side;
            gameState = mock(GameState.class);
            game = mock(SwccgGame.class);
            modifiers = mock(ModifiersQuerying.class);

            PhysicalCard objective =
                    objective(
                        kind.blueprintId,
                        OBJECTIVE_ID,
                        player,
                        flipped);
            add(objective, true);

            when(game.getGameState())
                    .thenReturn(gameState);
            when(game.getModifiersQuerying())
                    .thenReturn(modifiers);
            when(game.getOpponent(player))
                    .thenReturn(opponent);
            when(gameState.getGame())
                    .thenReturn(game);
            when(gameState.getOpponent(player))
                    .thenReturn(opponent);
            when(gameState.getOpponent(opponent))
                    .thenReturn(player);
            when(gameState.getSide(player))
                    .thenReturn(side);
            when(gameState.getSide(opponent))
                    .thenReturn(side == Side.LIGHT
                        ? Side.DARK : Side.LIGHT);
            when(gameState.getCurrentPlayerId())
                    .thenReturn(player);
            when(gameState.getPlayersLatestTurnNumber(
                    player)).thenReturn(3);
            when(gameState.getForcePileSize(player))
                    .thenReturn(10);
            when(gameState.getReserveDeckSize(player))
                    .thenReturn(20);
            when(gameState.getPlayerLifeForce(opponent))
                    .thenReturn(20);
            when(gameState.getHand(player))
                    .thenReturn(hand);
            when(gameState.getReserveDeck(player))
                    .thenReturn(reserve);
            when(gameState.getCardPile(
                    player, Zone.RESERVE_DECK))
                    .thenReturn(reserve);
            when(gameState.getLostPile(player))
                    .thenReturn(lost);
            when(gameState.getUsedPile(player))
                    .thenReturn(List.of());
            when(gameState.getAllPermanentCards())
                    .thenReturn(permanents);
            when(gameState.getAllStackedCards())
                    .thenReturn(stacked);
            when(gameState.getAllOutOfPlayCards())
                    .thenReturn(List.of());
            when(gameState.getLocationsInOrder())
                    .thenReturn(locations);
            when(gameState.getTopLocations())
                    .thenReturn(locations);
            when(gameState.findCardById(anyInt()))
                    .thenAnswer(invocation ->
                        cards.get(invocation.getArgument(
                            0, Integer.class)));
            when(gameState.findCardByPermanentId(
                    anyInt()))
                    .thenAnswer(invocation ->
                        cards.get(invocation.getArgument(
                            0, Integer.class)));
            when(gameState.getCardsAtLocation(
                    any(PhysicalCard.class)))
                    .thenAnswer(invocation ->
                        cardsAt(invocation.getArgument(
                            0, PhysicalCard.class)));
            when(gameState.getAttachedCards(
                    any(PhysicalCard.class)))
                    .thenReturn(List.of());
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
                            if (active.contains(card)
                                    && visitor
                                        .visitPhysicalCard(
                                            card)) {
                                return true;
                            }
                        }
                        return false;
                    });
            when(gameState.isCardInPlayActive(
                    any(PhysicalCard.class),
                    anyBoolean(), anyBoolean(),
                    anyBoolean(), anyBoolean(),
                    anyBoolean(), anyBoolean(),
                    anyBoolean(), anyBoolean()))
                    .thenAnswer(invocation -> {
                        PhysicalCard card =
                                invocation.getArgument(0);
                        boolean includeExcluded =
                                invocation.getArgument(
                                    1, Boolean.class);
                        boolean includeCaptives =
                                invocation.getArgument(
                                    3, Boolean.class);
                        return active.contains(card)
                                && (!excluded.contains(card)
                                    || includeExcluded)
                                && (!card.isCaptive()
                                    || includeCaptives);
                    });

            when(modifiers.hasPersona(
                    any(GameState.class),
                    any(PhysicalCard.class),
                    any(Persona.class)))
                    .thenAnswer(invocation ->
                        personas.getOrDefault(
                                invocation.getArgument(1),
                                Set.of())
                            .contains(
                                invocation.getArgument(2)));
            when(modifiers.hasIcon(
                    any(GameState.class),
                    any(PhysicalCard.class),
                    any(Icon.class)))
                    .thenAnswer(invocation -> {
                        PhysicalCard card =
                                invocation.getArgument(1);
                        Icon icon =
                                invocation.getArgument(2);
                        return card != null
                                && card.getBlueprint() != null
                                && card.getBlueprint()
                                    .hasIcon(icon);
                    });
            when(modifiers.hasKeyword(
                    any(GameState.class),
                    any(PhysicalCard.class),
                    any(Keyword.class)))
                    .thenAnswer(invocation -> {
                        PhysicalCard card =
                                invocation.getArgument(1);
                        Keyword keyword =
                                invocation.getArgument(2);
                        return card != null
                                && card.getBlueprint()
                                    != null
                                && card.getBlueprint()
                                    .hasKeyword(keyword);
                    });
            when(modifiers.canEscortCaptive(
                    any(GameState.class),
                    any(PhysicalCard.class),
                    any(PhysicalCard.class),
                    anyBoolean(), anyBoolean(),
                    anyBoolean()))
                    .thenReturn(false);
            when(modifiers.getLocationThatCardIsPresentAt(
                    any(GameState.class),
                    any(PhysicalCard.class)))
                    .thenAnswer(invocation ->
                        locationsByCard.get(
                            invocation.getArgument(1)));
            when(modifiers.getLocationThatCardIsAt(
                    any(GameState.class),
                    any(PhysicalCard.class)))
                    .thenAnswer(invocation ->
                        locationsByCard.get(
                            invocation.getArgument(1)));
            when(modifiers.getLocationHere(
                    any(GameState.class),
                    any(PhysicalCard.class)))
                    .thenAnswer(invocation -> {
                        PhysicalCard card =
                                invocation.getArgument(1);
                        return locations.contains(card)
                                ? card
                                : locationsByCard.get(card);
                    });
            when(modifiers.isPresentWith(
                    any(GameState.class),
                    any(PhysicalCard.class),
                    any(PhysicalCard.class)))
                    .thenAnswer(invocation -> {
                        PhysicalCard first =
                                invocation.getArgument(1);
                        PhysicalCard second =
                                invocation.getArgument(2);
                        PhysicalCard firstLocation =
                                locationsByCard.get(first);
                        PhysicalCard secondLocation =
                                locationsByCard.get(second);
                        return firstLocation != null
                                && firstLocation
                                    == secondLocation;
                    });
            when(modifiers.getLandspeedRequired(
                    any(GameState.class),
                    any(PhysicalCard.class),
                    any(PhysicalCard.class)))
                    .thenAnswer(invocation ->
                        legalLandspeed.contains(
                            moveKey(
                                invocation.getArgument(1),
                                invocation.getArgument(2)))
                                ? 1 : null);
            when(modifiers.getLandspeed(
                    any(GameState.class),
                    any(PhysicalCard.class)))
                    .thenReturn(1.0f);
            when(modifiers.getMoveUsingLandspeedCost(
                    any(GameState.class),
                    any(PhysicalCard.class),
                    any(PhysicalCard.class),
                    any(PhysicalCard.class),
                    anyBoolean(), anyFloat()))
                    .thenAnswer(invocation ->
                        landspeedCosts.getOrDefault(
                            moveKey(
                                invocation.getArgument(1),
                                invocation.getArgument(3)),
                            1.0f));
            when(modifiers.getForceAvailableToUse(
                    gameState, player))
                    .thenAnswer(invocation ->
                        gameState.getForcePileSize(
                            player));
            when(modifiers.getForceToLoseFromCardLimit(
                    any(GameState.class),
                    any(String.class),
                    any(PhysicalCard.class)))
                    .thenReturn(Float.MAX_VALUE);
            when(modifiers.getSitesBetween(
                    any(), any(), any()))
                    .thenReturn(List.of());
            when(modifiers.canBeTargetedBy(
                    any(GameState.class),
                    any(PhysicalCard.class),
                    any(PhysicalCard.class),
                    any()))
                    .thenReturn(false);
            setDeployable(true);
            when(modifiers.getDeployCost(
                    any(GameState.class),
                    any(PhysicalCard.class),
                    any(PhysicalCard.class),
                    any(PhysicalCard.class),
                    anyBoolean(), any(),
                    anyBoolean(), anyFloat(),
                    any(), anyBoolean()))
                    .thenAnswer(invocation -> {
                        PhysicalCard deploying =
                                invocation.getArgument(2);
                        Float printed =
                                deploying.getBlueprint()
                                    .getDeployCost();
                        return printed != null
                                ? printed : 0.0f;
                    });

            analyzer = bot == Bot.RANDO
                    ? new com.gempukku.swccgo.ai.models.rando
                        .strategy.ObjectiveAnalyzer()
                    : new com.gempukku.swccgo.ai.models.chosenone
                        .strategy.ObjectiveAnalyzer();
            analyzer.analyze(game, player, side);
            assertTrue(analyzer.isAnalyzed());
            assertEquals(kind.blueprintId,
                    analyzer.getObjectiveBlueprintId());
            assertEquals(flipped,
                    analyzer.isFlipped());
        }

        private PhysicalCard card(int id) {
            return cards.get(id);
        }

        private void add(
                PhysicalCard card, boolean activeCard) {
            cards.put(
                    card.getPermanentCardId(), card);
            permanents.add(card);
            if (activeCard) {
                active.add(card);
            }
        }

        private PhysicalCard addLocation(
                String blueprintId, int id,
                String owner) {
            PhysicalCard location =
                    physical(
                        blueprintId, id,
                        Zone.LOCATIONS, owner);
            int locationIndex = locations.size();
            String systemName =
                    location.getBlueprint()
                        .getSystemName();
            when(location.getPartOfSystem())
                    .thenReturn(systemName);
            when(location.getLocationZoneIndex())
                    .thenReturn(locationIndex);
            add(location, true);
            locations.add(location);
            locationsByCard.put(
                    location, location);
            return location;
        }

        private PhysicalCard addActive(
                String blueprintId, int id,
                String owner,
                PhysicalCard location) {
            PhysicalCard card = physical(
                    blueprintId, id,
                    location != null
                        ? Zone.AT_LOCATION
                        : Zone.SIDE_OF_TABLE,
                    owner);
            add(card, true);
            if (location != null) {
                place(card, location);
            }
            return card;
        }

        private PhysicalCard addHand(
                String blueprintId, int id) {
            PhysicalCard card = physical(
                    blueprintId, id,
                    Zone.HAND, player);
            add(card, false);
            hand.add(card);
            return card;
        }

        private PhysicalCard addReserve(
                String blueprintId, int id) {
            PhysicalCard card = physical(
                    blueprintId, id,
                    Zone.RESERVE_DECK, player);
            add(card, false);
            reserve.add(card);
            return card;
        }

        private PhysicalCard addSideEffect(
                String blueprintId,
                int id,
                boolean activeCard) {
            PhysicalCard card = physical(
                    blueprintId, id,
                    Zone.SIDE_OF_TABLE,
                    player);
            add(card, activeCard);
            return card;
        }

        private void place(
                PhysicalCard card,
                PhysicalCard location) {
            locationsByCard.put(card, location);
            when(card.getAtLocation())
                    .thenReturn(location);
            when(modifiers.getCardIsPresentAt(
                    gameState, card))
                    .thenReturn(location);
        }

        private List<PhysicalCard> cardsAt(
                PhysicalCard location) {
            if (location == null) {
                return List.of();
            }
            List<PhysicalCard> result =
                    new ArrayList<>();
            for (PhysicalCard card : permanents) {
                if (locationsByCard.get(card)
                        == location
                        && card != location) {
                    result.add(card);
                }
            }
            return result;
        }

        private void markPersona(
                PhysicalCard card,
                Persona persona) {
            personas.computeIfAbsent(
                    card,
                    ignored -> EnumSet.noneOf(
                        Persona.class))
                    .add(persona);
        }

        private void excludeFromBattle(
                PhysicalCard card) {
            excluded.add(card);
        }

        private void allowEscort(
                PhysicalCard escort,
                PhysicalCard captive) {
            when(modifiers.canEscortCaptive(
                    gameState, escort, captive,
                    true, false, false))
                    .thenReturn(true);
        }

        private void allowTargetByObjective(
                PhysicalCard target) {
            when(modifiers.canBeTargetedBy(
                    gameState, target,
                    card(OBJECTIVE_ID),
                    Collections.singleton(
                        TargetingReason.TO_BE_CAPTURED)))
                    .thenReturn(true);
        }

        private void allowDuelTargetByObjective(
                PhysicalCard target) {
            when(modifiers.canBeTargetedBy(
                    gameState, target,
                    card(OBJECTIVE_ID),
                    Collections.singleton(
                        TargetingReason.TO_BE_DUELED)))
                    .thenReturn(true);
        }

        private void advanceObservedFlipAgeTo(
                int ownTurn) {
            when(gameState.getPlayersLatestTurnNumber(
                    player)).thenReturn(ownTurn);
            analyzer.refreshFlipStatus(
                    gameState, player);
        }

        private void allowLandspeedMove(
                PhysicalCard mover,
                PhysicalCard origin,
                PhysicalCard destination,
                float cost) {
            place(mover, origin);
            legalLandspeed.add(
                    moveKey(mover, destination));
            landspeedCosts.put(
                    moveKey(mover, destination),
                    cost);
        }

        private void allowLocationTextMove(
                PhysicalCard mover,
                PhysicalCard origin,
                PhysicalCard destination) {
            place(mover, origin);
            when(modifiers
                    .mayNotMoveFromLocationToLocationUsingLocationText(
                        gameState, mover,
                        origin, destination))
                    .thenReturn(false);
        }

        private void setPresentPlace(
                PhysicalCard card,
                PhysicalCard place) {
            when(modifiers.getCardIsPresentAt(
                    gameState, card))
                    .thenReturn(place);
        }

        private void markBattleground(
                PhysicalCard location) {
            when(modifiers.isBattleground(
                    gameState, location, null))
                    .thenReturn(true);
        }

        private void aboard(
                PhysicalCard card,
                PhysicalCard carrier,
                PhysicalCard outerLocation,
                PhysicalCard presentPlace,
                PhysicalCard presentLocation) {
            when(card.getAtLocation())
                    .thenReturn(null);
            when(card.getAttachedTo())
                    .thenReturn(carrier);
            when(card.getCardAttachedToAtLocation())
                    .thenReturn(carrier);
            locationsByCard.put(
                    card, outerLocation);
            when(modifiers.getCardIsPresentAt(
                    gameState, card))
                    .thenReturn(presentPlace);
            when(modifiers
                    .getLocationThatCardIsPresentAt(
                        gameState, card))
                    .thenReturn(presentLocation);
        }

        private void allowAboard(
                PhysicalCard card,
                PhysicalCard carrier) {
            when(gameState.getAttachedCards(
                    carrier))
                    .thenReturn(List.of(card));
            when(modifiers.isAboard(
                    gameState, card, carrier,
                    false, true))
                    .thenReturn(true);
            when(modifiers.isPiloted(
                    gameState, carrier, false))
                    .thenReturn(true);
        }

        private void allowAboardGroup(
                PhysicalCard carrier,
                PhysicalCard... cardsAboard) {
            when(gameState.getAttachedCards(
                    carrier))
                    .thenReturn(
                        List.of(cardsAboard));
            when(gameState.getAboardCards(
                    carrier, false))
                    .thenReturn(
                        List.of(cardsAboard));
            for (PhysicalCard card
                    : cardsAboard) {
                var cardTypes =
                        card.getBlueprint()
                            .getCardTypes();
                when(modifiers.getCardTypes(
                        gameState, card))
                        .thenReturn(cardTypes);
                when(modifiers.isAboard(
                        gameState, card, carrier,
                        false, true))
                        .thenReturn(true);
            }
            when(modifiers.isPiloted(
                    gameState, carrier, false))
                    .thenReturn(true);
        }

        private void allowEmbark(
                PhysicalCard mover,
                PhysicalCard carrier) {
            when(modifiers.isPresentWith(
                    gameState, mover,
                    carrier, true))
                    .thenReturn(true);
            allowBoarding(mover, carrier);
        }

        private void allowShuttle(
                PhysicalCard mover,
                PhysicalCard carrier) {
            allowBoarding(mover, carrier);
        }

        private void allowBoarding(
                PhysicalCard mover,
                PhysicalCard carrier) {
            when(gameState.getAllAttachedRecursively(
                    mover)).thenReturn(List.of());
            when(gameState.getCaptivesOfEscort(
                    mover)).thenReturn(List.of());
            when(gameState.getAvailablePassengerCapacity(
                    modifiers, carrier, mover))
                    .thenReturn(1);
        }

        private void setDeployable(boolean deployable) {
            when(modifiers.isDeployable(
                    any(), any(), any(),
                    anyBoolean(), any(),
                    anyBoolean(), anyFloat(),
                    any(), any(), any(),
                    any(), any(),
                    anyBoolean(), anyFloat()))
                    .thenReturn(deployable);
            when(modifiers.isDeployableToTarget(
                    any(), any(), any(),
                    anyBoolean(), any(),
                    anyBoolean(), anyFloat(),
                    any(), any(), any(),
                    any(), any(), any(),
                    anyBoolean(), anyFloat()))
                    .thenReturn(deployable);
        }

        private void setNormalDeployPayment(
                PhysicalCard card,
                PhysicalCard target,
                float base,
                int extra) {
            when(modifiers.getDeployCost(
                    gameState, card, card,
                    target, false, null,
                    false, 0.0f, null, false))
                    .thenReturn(base);
            when(modifiers
                    .getExtraForceRequiredToDeployToTarget(
                        gameState, card, target,
                        null, card, false))
                    .thenReturn(extra);
        }

        private void setPrintedAbility(
                PhysicalCard card, float ability) {
            SwccgCardBlueprint printed =
                    card.getBlueprint();
            SwccgCardBlueprint adjusted =
                    mock(
                        SwccgCardBlueprint.class,
                        org.mockito.AdditionalAnswers
                            .delegatesTo(printed));
            when(adjusted.hasAbilityAttribute())
                    .thenReturn(true);
            when(adjusted.getAbility())
                    .thenReturn(ability);
            when(card.getBlueprint())
                    .thenReturn(adjusted);
        }

        private Map<String, Integer>
                exactNormalDeployActionPayments(
                Decision decision) {
            if (decision == null
                    || decision.actionIds() == null
                    || decision.cardIds() == null) {
                return Map.of();
            }
            Map<String, Integer> result =
                    new LinkedHashMap<>();
            int count = Math.min(
                    decision.actionIds().size(),
                    decision.cardIds().size());
            for (int index = 0;
                    index < count; index++) {
                String cardId =
                        decision.cardIds().get(index);
                if (cardId == null
                        || cardId.isBlank()) {
                    continue;
                }
                try {
                    PhysicalCard candidate =
                            gameState.findCardById(
                                Integer.parseInt(cardId));
                    Integer payment =
                            com.gempukku.swccgo.ai.models
                                .common.phase
                                .CaptureDeployBudgetFactsReader
                                .maximumExactNormalDeployPayment(
                                    game, player,
                                    candidate);
                    if (payment != null) {
                        result.put(
                            decision.actionIds()
                                .get(index),
                            payment);
                    }
                } catch (NumberFormatException ignored) {
                    // Synthetic non-card actions have no deploy payment.
                }
            }
            return Map.copyOf(result);
        }

        private void setBhbmEmperorDeployability(
                PhysicalCard emperor) {
            when(modifiers.isDeployable(
                    any(), any(), any(),
                    anyBoolean(), any(),
                    anyBoolean(), anyFloat(),
                    any(), any(), any(),
                    any(), any(),
                    anyBoolean(), anyFloat()))
                    .thenAnswer(invocation ->
                        invocation.getArgument(1)
                                == card(OBJECTIVE_ID)
                        && invocation.getArgument(2)
                                == emperor
                        && Float.compare(
                                -2.0f,
                                invocation.getArgument(
                                    6, Float.class)) == 0
                        && modifiers.getForceAvailableToUse(
                                gameState, player) >= 3);
            when(modifiers.getDeployCost(
                    gameState, card(OBJECTIVE_ID),
                    emperor, null, false, null,
                    false, -2.0f, null, true))
                    .thenReturn(3.0f);
        }

        private void setLiveAction(
                PhysicalCard source,
                String text) {
            GameTextActionState state =
                    mock(GameTextActionState.class);
            GameTextAction action =
                    mock(GameTextAction.class);
            when(state.getGameTextAction())
                    .thenReturn(action);
            when(action.getActionSource())
                    .thenReturn(source);
            when(action.getText())
                    .thenReturn(text);
            when(gameState.getTopGameTextActionState())
                    .thenReturn(state);
        }

        private void setBattle(
                PhysicalCard location,
                float ourPower,
                float theirPower,
                float ourAbility,
                float theirAbility,
                List<PhysicalCard> cardsHere) {
            when(modifiers.getTotalPowerAtLocation(
                    gameState, location,
                    player, false, false))
                    .thenReturn(ourPower);
            when(modifiers.getTotalPowerAtLocation(
                    gameState, location,
                    opponent, false, false))
                    .thenReturn(theirPower);
            when(modifiers.getTotalAbilityAtLocation(
                    gameState, player, location))
                    .thenReturn(ourAbility);
            when(modifiers.getTotalAbilityAtLocation(
                    gameState, opponent, location))
                    .thenReturn(theirAbility);
            when(gameState.getCardsAtLocation(location))
                    .thenReturn(cardsHere);
        }

        private static String moveKey(
                PhysicalCard mover,
                PhysicalCard destination) {
            return mover.getPermanentCardId()
                    + ">"
                    + destination.getPermanentCardId();
        }

        private static PhysicalCard objective(
                String blueprintId,
                int id,
                String owner,
                boolean flipped) {
            PhysicalCard card =
                    mock(PhysicalCard.class);
            SwccgCardBlueprint front =
                    blueprint(blueprintId);
            SwccgCardBlueprint back =
                    blueprint(blueprintId
                        + "_BACK");
            when(card.getOwner())
                    .thenReturn(owner);
            when(card.getZone())
                    .thenReturn(
                        Zone.SIDE_OF_TABLE);
            when(card.getBlueprint())
                    .thenReturn(
                        flipped ? back : front);
            when(card.getOtherSideBlueprint())
                    .thenReturn(
                        flipped ? front : back);
            when(card.getBlueprintId(true))
                    .thenReturn(blueprintId);
            when(card.getBlueprintId(false))
                    .thenReturn(
                        flipped
                            ? blueprintId + "_BACK"
                            : blueprintId);
            when(card.getTitle())
                    .thenReturn(
                        (flipped ? back : front)
                            .getTitle());
            when(card.getTitles())
                    .thenReturn(List.of(
                        (flipped ? back : front)
                            .getTitle()));
            when(card.getPermanentCardId())
                    .thenReturn(id);
            when(card.getCardId())
                    .thenReturn(id);
            when(card.getAdditionalCardIds())
                    .thenReturn(List.of());
            when(card.isFlipped())
                    .thenReturn(flipped);
            return card;
        }

        private static PhysicalCard physical(
                String blueprintId,
                int id,
                Zone zone,
                String owner) {
            PhysicalCard card =
                    mock(PhysicalCard.class);
            SwccgCardBlueprint blueprint =
                    blueprint(blueprintId);
            when(card.getOwner())
                    .thenReturn(owner);
            when(card.getZone())
                    .thenReturn(zone);
            when(card.getBlueprint())
                    .thenReturn(blueprint);
            when(card.getBlueprintId(true))
                    .thenReturn(blueprintId);
            when(card.getBlueprintId(false))
                    .thenReturn(blueprintId);
            when(card.getTitle())
                    .thenReturn(blueprint.getTitle());
            when(card.getTitles())
                    .thenReturn(List.of(
                        blueprint.getTitle()));
            when(card.getPermanentCardId())
                    .thenReturn(id);
            when(card.getCardId())
                    .thenReturn(id);
            when(card.getAdditionalCardIds())
                    .thenReturn(List.of());
            when(card.getCardsEscorting())
                    .thenReturn(List.of());
            when(card.isLeavingTable())
                    .thenReturn(false);
            when(card.isCaptive())
                    .thenReturn(false);
            when(card.isFrozen())
                    .thenReturn(false);
            when(card.isUndercover())
                    .thenReturn(false);
            return card;
        }
    }
}
