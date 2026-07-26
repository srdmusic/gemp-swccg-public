package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.ai.models.common.phase.TdigwattObjectiveFacts;
import com.gempukku.swccgo.ai.models.common.phase.TdigwattObjectiveFactsReader;
import com.gempukku.swccgo.ai.models.common.phase.TdigwattObjectivePolicy;
import com.gempukku.swccgo.ai.models.common.phase.TdigwattObjectiveScoringPolicy;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.SpotOverride;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.framework.StartingSetup;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import com.gempukku.swccgo.game.PhysicalCardImpl;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.gempukku.swccgo.framework.Assertions.assertAtLocation;
import static com.gempukku.swccgo.framework.TestBase.DS;
import static com.gempukku.swccgo.framework.TestBase.LS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Persistent-board behavior proof for both mechanically distinct TDIGWATT
 * objectives.
 *
 * <p>Each test gives both no-argument CombinedEvaluators and both public bot
 * adapters the same live engine decision. Their shared winner is submitted to
 * that same game, then the native card source must pay its real cost and fire
 * its own flip trigger.
 */
public class TdigwattObjectivePersistentBoardChainTest {
    private static final String LANDO_MOVE =
            "Have your Lando make a regular move";

    private static final StartingSetup CLASSIC_TDIGWATT =
            new StartingSetup() {
                @Override
                public HashMap<String, String> Cards() {
                    return new HashMap<>() {{
                        put("objective", "109_12");
                        put("setupSite", "7_270");
                    }};
                }

                @Override
                public void Setup(VirtualTableScenario scn) {
                    // The setup filter has one matching site.
                }
            };

    private static final StartingSetup VIRTUAL_TDIGWATT =
            new StartingSetup() {
                @Override
                public HashMap<String, String> Cards() {
                    return new HashMap<>() {{
                        put("objective", "226_12");
                        put("setupSite", "7_270");
                        put("imSorry", "226_6");
                    }};
                }

                @Override
                public void Setup(VirtualTableScenario scn) {
                    // Both setup filters have one matching card.
                }
            };

    private static VirtualTableScenario classicScenario() {
        return new VirtualTableScenario(
                new HashMap<>(),
                new HashMap<>() {{
                    put("bespin", "5_164");
                    put("cloudCity", "5_165");
                    put("extraSite1", "5_166");
                    put("extraSite2", "5_167");
                    put("darkDeal", "5_115");
                    put("tieSystem", "1_304");
                    put("obsidianSector", "5_175");
                    put("storm1", "1_194");
                    put("storm2", "1_194");
                    put("storm3", "1_194");
                }},
                20,
                30,
                StartingSetup.DoNothingSetup,
                CLASSIC_TDIGWATT,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open);
    }

    private static VirtualTableScenario virtualScenario() {
        return new VirtualTableScenario(
                new HashMap<>(),
                new HashMap<>() {{
                    put("security", "601_202");
                    put("chasm", "5_167");
                    put("dining", "5_168");
                    put("lando", "5_99");
                    put("backup", "1_194");
                    put("secondController", "1_194");
                    put("seBespin", "223_8");
                    put("wrongBespin", "5_164");
                }},
                20,
                30,
                StartingSetup.DoNothingSetup,
                VIRTUAL_TDIGWATT,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open);
    }

    @Test
    public void classicBotDeploysLegalDarkDealPaysThreeAndNativeFlipFires()
            throws Exception {
        VirtualTableScenario scn = classicScenario();
        PhysicalCardImpl objective = scn.GetDSCard("objective");
        PhysicalCardImpl bespin = scn.GetDSCard("bespin");
        PhysicalCardImpl cloudCity = scn.GetDSCard("cloudCity");
        PhysicalCardImpl extraSite1 = scn.GetDSCard("extraSite1");
        PhysicalCardImpl extraSite2 = scn.GetDSCard("extraSite2");
        PhysicalCardImpl setupSite = scn.GetDSCard("setupSite");
        PhysicalCardImpl darkDeal = scn.GetDSCard("darkDeal");

        scn.MoveCardsToDSHand(
                bespin,
                cloudCity,
                extraSite1,
                extraSite2,
                darkDeal,
                scn.GetDSCard("tieSystem"),
                scn.GetDSCard("obsidianSector"),
                scn.GetDSCard("storm1"),
                scn.GetDSCard("storm2"),
                scn.GetDSCard("storm3"));
        scn.StartGame();
        scn.MoveLocationToTable(bespin);
        scn.MoveLocationToTable(cloudCity);
        scn.MoveLocationToTable(extraSite1);
        scn.MoveLocationToTable(extraSite2);
        scn.MoveCardsToLocation(
                bespin, scn.GetDSCard("tieSystem"));
        scn.MoveCardsToLocation(
                cloudCity, scn.GetDSCard("obsidianSector"));
        scn.MoveCardsToLocation(
                setupSite, scn.GetDSCard("storm1"));
        scn.MoveCardsToLocation(
                extraSite1, scn.GetDSCard("storm2"));
        scn.MoveCardsToLocation(
                extraSite2, scn.GetDSCard("storm3"));
        moveOtherDarkHandCardsToReserve(scn, darkDeal);
        enterDarkDeployWithExactForce(scn, 3);

        assertFalse(objective.isFlipped());
        assertEquals(3, scn.GetDSForcePileCount());
        assertTrue(GameConditions.controls(
                scn.game(), DS, cloudCity));
        assertTrue(GameConditions.controls(
                scn.game(), DS, 3,
                Filters.relatedSiteTo(
                        darkDeal, Filters.Bespin_Cloud_City)));
        assertTrue("Dark Deal must be legal by its own source",
                scn.DSDeployAvailable(darkDeal));

        TdigwattObjectiveFacts.ClassicState before =
                TdigwattObjectiveFactsReader
                    .readClassicFrontState(scn.game(), DS)
                    .orElseThrow();
        assertEquals(
                TdigwattObjectiveFacts.CLASSIC_BLUEPRINT_ID,
                before.objective().frontBlueprintId());
        assertEquals(objective.getPermanentCardId(),
                before.objective().physicalCardId());
        assertFalse(before.darkDealOnTable());
        assertTrue(before.darkOccupiesBespinSystem());
        assertTrue(before.darkOccupiesBespinCloudCity());
        assertFalse(TdigwattObjectivePolicy
                .classicFlipReady(before));
        assertTrue(
                "The exact legal deploy must complete the source flip law",
                TdigwattObjectivePolicy.classicFlipReady(
                    new TdigwattObjectiveFacts.ClassicState(
                        before.objective(),
                        true,
                        before.darkOccupiesBespinSystem(),
                        before.darkOccupiesBespinCloudCity(),
                        false,
                        before.opponentControlsBespinSystem(),
                        false)));

        var randoAnalyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer();
        randoAnalyzer.analyze(scn.game(), DS, Side.DARK);
        var chosenAnalyzer =
                new com.gempukku.swccgo.ai.models.chosenone.strategy
                    .ObjectiveAnalyzer();
        chosenAnalyzer.analyze(scn.game(), DS, Side.DARK);
        PublicBots bots = PublicBots.forGame(scn);

        AwaitingDecision decision = scn.GetAwaitingDecision(DS);
        assertNotNull(decision);
        assertTrue(scn.AwaitingDSDeployPhaseActions());
        assertFalse("Legal Pass must remain in the decision",
                raw(decision).noPass());
        ActionRef exactDeploy = exactAction(
                decision,
                darkDeal.getCardId(),
                "deploy");

        Choice combined = evaluateBoth(
                scn, randoAnalyzer, chosenAnalyzer, null);
        String publicResponse = bots.decideBoth(scn);
        assertEquals(
                "No-arg CombinedEvaluator and public adapter must agree",
                combined.actionId(), publicResponse);
        assertEquals(
                "The exact Dark Deal action must beat legal Pass",
                exactDeploy.actionId(), combined.actionId());
        assertEquals(
                "The winning action must retain exact card provenance",
                Integer.toString(darkDeal.getCardId()),
                combined.cardId());
        assertFalse(combined.hardVeto());

        int forceBefore = scn.GetDSForcePileCount();
        scn.DSDecided(combined.actionId());
        if (isDarkCardSelection(
                scn, "where to deploy", "location where to deploy")) {
            Choice destination = evaluateBoth(
                    scn,
                    randoAnalyzer,
                    chosenAnalyzer,
                    darkDeal.getPermanentCardId());
            String publicDestination = bots.decideBoth(scn);
            assertEquals(destination.actionId(), publicDestination);
            assertEquals(
                    Integer.toString(cloudCity.getCardId()),
                    destination.actionId());
            assertEquals(
                    Integer.toString(cloudCity.getCardId()),
                    destination.cardId());
            scn.DSDecided(destination.actionId());
        }
        scn.PassAllResponses();

        assertEquals(
                "Dark Deal's printed 3 is destiny, not a Force cost",
                forceBefore, scn.GetDSForcePileCount());
        assertTrue(scn.IsAttachedTo(cloudCity, darkDeal));
        assertTrue(
                "The real 109_12 source must flip after the bot deploy",
                objective.isFlipped());
    }

    @Test
    public void classicBotUsesRealPullParentAndTempChildToTakeLegalTarget() {
        VirtualTableScenario scn = classicScenario();
        PhysicalCardImpl objective = scn.GetDSCard("objective");
        PhysicalCardImpl darkDeal = scn.GetDSCard("darkDeal");

        scn.MoveCardsToDSHand(
                darkDeal,
                scn.GetDSCard("bespin"),
                scn.GetDSCard("cloudCity"),
                scn.GetDSCard("extraSite1"),
                scn.GetDSCard("extraSite2"));
        scn.StartGame();
        scn.MoveOutOfPlay(scn.GetDSCard("bespin"));
        scn.MoveOutOfPlay(scn.GetDSCard("cloudCity"));
        scn.MoveOutOfPlay(scn.GetDSCard("extraSite1"));
        scn.MoveOutOfPlay(scn.GetDSCard("extraSite2"));
        enterDarkControlWithEmptyReserve(scn);
        restoreDarkReserveFillers(scn, 9);
        scn.MoveCardsToBottomOfDSReserveDeck(darkDeal);
        scn.SkipToPhase(Phase.DEPLOY);
        assertTrue(scn.AwaitingDSDeployPhaseActions());

        PublicBots bots = PublicBots.forGame(scn);
        AwaitingDecision parent = scn.GetAwaitingDecision(DS);
        ActionRef exactPull = exactAction(
                parent,
                objective.getCardId(),
                "Take card into hand from Reserve Deck");
        assertEquals(objective,
                scn.gameState().findCardById(
                    Integer.parseInt(exactPull.cardId())));
        assertEquals(
                Integer.valueOf(objective.getPermanentCardId()),
                TdigwattObjectiveFactsReader
                    .readPullActionSources(
                        parent, scn.game(), DS)
                    .get(exactPull.actionId()));
        List<TdigwattObjectiveFacts.PullFacts> legal =
                TdigwattObjectiveFactsReader
                    .readSourceLegalPullCandidatesInReserve(
                        scn.game(), DS, objective)
                    .orElseThrow();
        assertEquals(1, legal.size());
        assertEquals("5_115",
                legal.get(0).candidateBlueprintId());
        var randoAnalyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer();
        randoAnalyzer.analyze(scn.game(), DS, Side.DARK);
        var chosenAnalyzer =
                new com.gempukku.swccgo.ai.models.chosenone.strategy
                    .ObjectiveAnalyzer();
        chosenAnalyzer.analyze(scn.game(), DS, Side.DARK);
        Choice combined = evaluateBoth(
                scn, randoAnalyzer, chosenAnalyzer, null);
        assertEquals(
                "No-arg evaluators must choose the exact pull",
                exactPull.actionId(), combined.actionId());
        assertTrue(combined.score()
                >= TdigwattObjectiveScoringPolicy
                    .PULL_PARENT_BONUS);
        assertEquals(
                Integer.toString(objective.getCardId()),
                combined.cardId());
        assertTrue(combined.reasoning().stream()
                .anyMatch(reason -> reason.contains(
                    "source-legal Reserve target")));
        String parentResponse = bots.decideBoth(scn);
        assertEquals(
                "The exact classic objective upload must beat Pass",
                exactPull.actionId(), parentResponse);
        scn.DSDecided(parentResponse);

        AwaitingDecision child = scn.GetAwaitingDecision(DS);
        assertNotNull(child);
        assertEquals("ARBITRARY_CARDS",
                child.getDecisionType().name());
        Raw offered = raw(child);
        int darkDealIndex =
                offered.blueprints().indexOf("5_115");
        assertTrue(
                "The real search must expose Dark Deal in Reserve Deck",
                darkDealIndex >= 0);
        assertTrue(
                "The exact classic target must be selectable",
                offered.selectable().get(darkDealIndex));
        assertEquals("temp" + darkDealIndex,
                offered.cardIds().get(darkDealIndex));

        String childResponse = bots.decideBoth(scn);
        assertEquals(
                "Both bots must choose the source-legal temp child",
                offered.cardIds().get(darkDealIndex),
                childResponse);
        scn.DSDecided(childResponse);
        scn.PassAllResponses();

        assertEquals(
                "The real classic effect must take the selected card",
                Zone.HAND, darkDeal.getZone());
    }

    @Test
    public void virtualBotUsesExactLandoMovePaysOneAndNativeFlipFires()
            throws Exception {
        VirtualTableScenario scn = virtualScenario();
        PhysicalCardImpl objective = scn.GetDSCard("objective");
        PhysicalCardImpl setupSite = scn.GetDSCard("setupSite");
        PhysicalCardImpl imSorry = scn.GetDSCard("imSorry");
        PhysicalCardImpl security = scn.GetDSCard("security");
        PhysicalCardImpl chasm = scn.GetDSCard("chasm");
        PhysicalCardImpl dining = scn.GetDSCard("dining");
        PhysicalCardImpl lando = scn.GetDSCard("lando");
        PhysicalCardImpl backup = scn.GetDSCard("backup");
        PhysicalCardImpl secondController =
                scn.GetDSCard("secondController");

        scn.MoveCardsToDSHand(
                security,
                chasm,
                dining,
                lando,
                backup,
                secondController);
        scn.StartGame();

        // Remove the setup site's unrelated retrieval action. The objective
        // and I'm Sorry remain the exact cards deployed by the real setup.
        scn.MoveOutOfPlay(setupSite);
        scn.MoveLocationToTable(security);
        scn.MoveLocationToTable(chasm);
        scn.MoveLocationToTable(dining);
        scn.MoveCardsToLocation(dining, lando, backup);
        scn.MoveCardsToLocation(security, secondController);
        moveOtherDarkHandCardsToReserve(scn);
        enterDarkControlWithExactForce(scn, 1);

        assertFalse(objective.isFlipped());
        assertEquals(2, controlledBespinLocations(scn, DS));
        assertEquals(0, controlledBespinLocations(scn, LS));
        assertEquals(1, scn.GetDSForcePileCount());
        assertTrue(
                "The real objective must expose its exact Lando action",
                scn.DSCardActionAvailable(objective, LANDO_MOVE));

        TdigwattObjectiveFacts.VirtualState before =
                TdigwattObjectiveFactsReader
                    .readVirtualState(scn.game(), DS)
                    .orElseThrow();
        assertEquals(
                TdigwattObjectiveFacts.VIRTUAL_BLUEPRINT_ID,
                before.objective().frontBlueprintId());
        assertEquals(objective.getPermanentCardId(),
                before.objective().physicalCardId());
        assertEquals(2,
                before.darkControlledBespinLocations());
        assertEquals(0,
                before.lightControlledBespinLocations());
        assertFalse(TdigwattObjectivePolicy
                .virtualFlipReady(before));
        assertTrue(
                "The third control must complete the exact virtual flip law",
                TdigwattObjectivePolicy.virtualFlipReady(
                    new TdigwattObjectiveFacts.VirtualState(
                        before.objective(), 3, 0)));

        TdigwattObjectiveFacts.LandoMoveFacts route =
                TdigwattObjectiveFactsReader
                    .readVirtualLandoLandspeedRoute(
                        scn.game(),
                        DS,
                        objective,
                        lando,
                        dining,
                        chasm,
                        TdigwattObjectiveFactsReader.Proof.PROVEN,
                        TdigwattObjectiveFactsReader.Proof.PROVEN,
                        TdigwattObjectiveFactsReader.Proof.PROVEN)
                    .orElseThrow();
        assertEquals(
                "The route must retain exact objective-source provenance",
                objective.getPermanentCardId(),
                route.actionSourcePhysicalCardId());
        assertEquals(
                "The reader must preserve the current engine move cost",
                1, route.requiredForceCost());
        assertEquals(1,
                TdigwattObjectivePolicy
                    .virtualLandoMoveForceReserve(route));

        assertTrue(
                "I'm Sorry is a title-adjacent source, not the objective",
                TdigwattObjectiveFactsReader
                    .readVirtualLandoLandspeedRoute(
                        scn.game(),
                        DS,
                        imSorry,
                        lando,
                        dining,
                        chasm,
                        TdigwattObjectiveFactsReader.Proof.PROVEN,
                        TdigwattObjectiveFactsReader.Proof.PROVEN,
                        TdigwattObjectiveFactsReader.Proof.PROVEN)
                    .isEmpty());
        assertTrue(
                "An out-of-play Cloud City site is not a legal route",
                TdigwattObjectiveFactsReader
                    .readVirtualLandoLandspeedRoute(
                        scn.game(),
                        DS,
                        objective,
                        lando,
                        dining,
                        setupSite,
                        TdigwattObjectiveFactsReader.Proof.PROVEN,
                        TdigwattObjectiveFactsReader.Proof.PROVEN,
                        TdigwattObjectiveFactsReader.Proof.PROVEN)
                    .isEmpty());

        var randoAnalyzer =
                new com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer();
        randoAnalyzer.analyze(scn.game(), DS, Side.DARK);
        var chosenAnalyzer =
                new com.gempukku.swccgo.ai.models.chosenone.strategy
                    .ObjectiveAnalyzer();
        chosenAnalyzer.analyze(scn.game(), DS, Side.DARK);
        PublicBots bots = PublicBots.forGame(scn);

        AwaitingDecision decision = scn.GetAwaitingDecision(DS);
        assertNotNull(decision);
        assertTrue(scn.AwaitingDSControlPhaseActions());
        assertFalse("Legal Pass must remain in the decision",
                raw(decision).noPass());
        ActionRef exactMove = exactAction(
                decision,
                objective.getCardId(),
                LANDO_MOVE);

        Choice combined = evaluateBoth(
                scn, randoAnalyzer, chosenAnalyzer, null);
        String publicResponse = bots.decideBoth(scn);
        assertEquals(
                "No-arg CombinedEvaluator and public adapter must agree",
                combined.actionId(), publicResponse);
        assertEquals(
                "The exact objective move must beat legal Pass",
                exactMove.actionId(), combined.actionId());
        assertEquals(
                "The winner must retain exact objective-card provenance",
                Integer.toString(objective.getCardId()),
                combined.cardId());
        assertFalse(combined.hardVeto());

        int forceBefore = scn.GetDSForcePileCount();
        scn.DSDecided(combined.actionId());
        if (isDarkCardSelection(scn, "where to move")) {
            Choice destination = evaluateBoth(
                    scn, randoAnalyzer, chosenAnalyzer, null);
            String publicDestination = bots.decideBoth(scn);
            assertEquals(destination.actionId(), publicDestination);
            assertEquals(
                    "The exact third-control destination must win",
                    Integer.toString(chasm.getCardId()),
                    destination.actionId());
            assertEquals(
                    Integer.toString(chasm.getCardId()),
                    destination.cardId());
            scn.DSDecided(destination.actionId());
        }
        scn.PassAllResponses();

        assertEquals(
                "The source grants timing, not free movement",
                forceBefore - route.requiredForceCost(),
                scn.GetDSForcePileCount());
        assertAtLocation(chasm, lando);
        assertAtLocation(dining, backup);
        assertAtLocation(security, secondController);
        assertEquals(3, controlledBespinLocations(scn, DS));
        assertEquals(0, controlledBespinLocations(scn, LS));
        assertTrue(
                "The real 226_12 source must flip on the third control",
                objective.isFlipped());
    }

    @Test
    public void virtualBotUsesRealPullAndRejectsCloudCityBespinPrint() {
        VirtualTableScenario scn = virtualScenario();
        PhysicalCardImpl objective = scn.GetDSCard("objective");
        PhysicalCardImpl seBespin = scn.GetDSCard("seBespin");
        PhysicalCardImpl wrongBespin =
                scn.GetDSCard("wrongBespin");

        scn.MoveCardsToDSHand(
                seBespin,
                wrongBespin,
                scn.GetDSCard("security"),
                scn.GetDSCard("chasm"),
                scn.GetDSCard("dining"),
                scn.GetDSCard("lando"),
                scn.GetDSCard("backup"),
                scn.GetDSCard("secondController"));
        scn.StartGame();
        scn.MoveOutOfPlay(scn.GetDSCard("imSorry"));
        scn.MoveOutOfPlay(scn.GetDSCard("security"));
        scn.MoveOutOfPlay(scn.GetDSCard("chasm"));
        scn.MoveOutOfPlay(scn.GetDSCard("dining"));
        scn.MoveOutOfPlay(scn.GetDSCard("lando"));
        scn.MoveOutOfPlay(scn.GetDSCard("backup"));
        scn.MoveOutOfPlay(scn.GetDSCard("secondController"));
        enterDarkControlWithEmptyReserve(scn);
        restoreDarkReserveFillers(scn, 9);
        scn.MoveCardsToBottomOfDSReserveDeck(wrongBespin);
        scn.MoveCardsToBottomOfDSReserveDeck(seBespin);
        scn.SkipToPhase(Phase.DEPLOY);
        assertTrue(scn.AwaitingDSDeployPhaseActions());

        PublicBots bots = PublicBots.forGame(scn);
        AwaitingDecision parent = scn.GetAwaitingDecision(DS);
        ActionRef exactPull = exactAction(
                parent,
                objective.getCardId(),
                "Take card into hand from Reserve Deck");
        String parentResponse = bots.decideBoth(scn);
        assertEquals(
                "The exact virtual objective upload must beat Pass",
                exactPull.actionId(), parentResponse);
        scn.DSDecided(parentResponse);

        AwaitingDecision child = scn.GetAwaitingDecision(DS);
        assertNotNull(child);
        assertEquals("ARBITRARY_CARDS",
                child.getDecisionType().name());
        Raw offered = raw(child);
        int seIndex = offered.blueprints().indexOf("223_8");
        int wrongIndex =
                offered.blueprints().indexOf("5_164");
        assertTrue(
                "The [Special Edition] Bespin system must be in the real search",
                seIndex >= 0);
        assertTrue(
                "The Cloud City Bespin print must remain visible as a temp-order decoy",
                wrongIndex >= 0);
        assertTrue(
                "Card223_008 is the source-legal [Special Edition] print",
                offered.selectable().get(seIndex));
        assertFalse(
                "Card5_164 lacks the Special Edition icon and must not be selectable",
                offered.selectable().get(wrongIndex));
        assertEquals("temp" + seIndex,
                offered.cardIds().get(seIndex));
        assertEquals("temp" + wrongIndex,
                offered.cardIds().get(wrongIndex));

        String childResponse = bots.decideBoth(scn);
        assertEquals(
                "Both bots must choose exact [Special Edition] Bespin",
                offered.cardIds().get(seIndex),
                childResponse);
        scn.DSDecided(childResponse);
        scn.PassAllResponses();

        assertEquals(Zone.HAND, seBespin.getZone());
        assertTrue(
                "The wrong Bespin print must remain in Reserve Deck",
                wrongBespin.getZone() == Zone.RESERVE_DECK
                    || wrongBespin.getZone()
                        == Zone.TOP_OF_RESERVE_DECK);
    }

    private static int controlledBespinLocations(
            VirtualTableScenario scn,
            String playerId) {
        return Filters.countTopLocationsOnTable(
                scn.game(),
                Filters.and(
                    Filters.Bespin_location,
                    Filters.controls(
                        playerId,
                        SpotOverride
                            .INCLUDE_EXCLUDED_FROM_BATTLE)));
    }

    private static void enterDarkControlWithExactForce(
            VirtualTableScenario scn,
            int force) {
        scn.SkipToDSTurn();
        clearDarkForce(scn);
        if (force > 0) {
            scn.MoveCardsToBottomOfDSReserveDeck(
                    scn.GetDSFiller(1));
            scn.DSActivateForceCheat(force);
        }
        emptyDarkReserveIntoUsed(scn);
        scn.PassActivateActions();
        if (scn.DSDecisionAvailable(
                "You have not activated Force. Do you want to Pass?")) {
            scn.DSChooseYes();
            scn.PassActivateActions();
        }
        assertTrue(
                "Expected Dark Side control actions, got "
                    + describe(scn.GetCurrentDecision()),
                scn.AwaitingDSControlPhaseActions());
        assertEquals(force, scn.GetDSForcePileCount());
    }

    private static void enterDarkDeployWithExactForce(
            VirtualTableScenario scn,
            int force) {
        setExactDarkForceAndEmptyReserve(scn, force);
        scn.PassActivateActions();
        if (scn.DSDecisionAvailable(
                "You have not activated Force. Do you want to Pass?")) {
            scn.DSChooseYes();
            scn.PassActivateActions();
        }
        scn.SkipToPhase(Phase.DEPLOY);
        assertTrue(
                "Expected Dark Side deploy actions, got "
                    + describe(scn.GetCurrentDecision()),
                scn.AwaitingDSDeployPhaseActions());
        assertEquals(force, scn.GetDSForcePileCount());
    }

    private static void enterDarkControlWithEmptyReserve(
            VirtualTableScenario scn) {
        setExactDarkForceAndEmptyReserve(scn, 0);
        scn.PassActivateActions();
        if (scn.DSDecisionAvailable(
                "You have not activated Force. Do you want to Pass?")) {
            scn.DSChooseYes();
            scn.PassActivateActions();
        }
        assertTrue(
                "Expected Dark Side control actions, got "
                    + describe(scn.GetCurrentDecision()),
                scn.AwaitingDSControlPhaseActions());
    }

    private static void restoreDarkReserveFillers(
            VirtualTableScenario scn,
            int count) {
        for (int index = 1; index <= count; index++) {
            scn.MoveCardsToBottomOfDSReserveDeck(
                    scn.GetDSFiller(index));
        }
    }

    private static void setExactDarkForceAndEmptyReserve(
            VirtualTableScenario scn,
            int force) {
        clearDarkForce(scn);
        scn.DSActivateForceCheat(force);
        emptyDarkReserveIntoUsed(scn);
        assertEquals(force, scn.GetDSForcePileCount());
        assertEquals(0, scn.GetDSReserveDeckCount());
    }

    private static void clearDarkForce(
            VirtualTableScenario scn) {
        while (scn.GetDSForcePileCount() > 0) {
            scn.MoveCardsToHand(
                    scn.GetTopOfDSForcePile());
        }
    }

    private static void emptyDarkReserveIntoUsed(
            VirtualTableScenario scn) {
        while (scn.GetDSReserveDeckCount() > 0) {
            scn.MoveCardsToTopOfDSUsedPile(
                    scn.GetTopOfDSReserveDeck());
        }
    }

    private static void moveOtherDarkHandCardsToReserve(
            VirtualTableScenario scn,
            PhysicalCardImpl... keep) {
        List<PhysicalCardImpl> kept = Arrays.asList(keep);
        List<PhysicalCardImpl> hand = new ArrayList<>();
        for (var card : scn.gameState().getHand(DS)) {
            if (card instanceof PhysicalCardImpl physical
                    && !kept.contains(physical)) {
                hand.add(physical);
            }
        }
        for (PhysicalCardImpl card : hand) {
            scn.MoveCardsToBottomOfDSReserveDeck(card);
        }
    }

    private static boolean isDarkCardSelection(
            VirtualTableScenario scn,
            String... textFragments) {
        AwaitingDecision decision =
                scn.GetAwaitingDecision(DS);
        if (decision == null
                || !"CARD_SELECTION".equals(
                    decision.getDecisionType().name())) {
            return false;
        }
        String text = decision.getText()
                .toLowerCase(Locale.ROOT);
        return Arrays.stream(textFragments)
                .map(fragment ->
                    fragment.toLowerCase(Locale.ROOT))
                .anyMatch(text::contains);
    }

    private static ActionRef exactAction(
            AwaitingDecision decision,
            int sourceCardId,
            String actionText) {
        Raw raw = raw(decision);
        String expectedCardId =
                Integer.toString(sourceCardId);
        String expectedText =
                actionText.toLowerCase(Locale.ROOT);
        for (int index = 0;
                index < raw.actionIds().size();
                index++) {
            String cardId = value(
                    raw.cardIds(), index);
            String text = value(
                    raw.actionTexts(), index);
            if (expectedCardId.equals(cardId)
                    && text.toLowerCase(Locale.ROOT)
                        .contains(expectedText)) {
                return new ActionRef(
                        raw.actionIds().get(index),
                        text,
                        cardId);
            }
        }
        throw new AssertionError(
                "Exact source action not offered: cardId="
                    + expectedCardId + " text='"
                    + actionText + "' in "
                    + raw.actionTexts());
    }

    private static String value(
            List<String> values,
            int index) {
        return index >= 0 && index < values.size()
                ? values.get(index) : "";
    }

    private static Choice evaluateBoth(
            VirtualTableScenario scn,
            com.gempukku.swccgo.ai.models.rando.strategy
                    .ObjectiveAnalyzer randoAnalyzer,
            com.gempukku.swccgo.ai.models.chosenone.strategy
                    .ObjectiveAnalyzer chosenAnalyzer,
            Integer deployingPermanentId) {
        AwaitingDecision decision =
                scn.GetAwaitingDecision(DS);
        assertNotNull(
                "Dark Side must own the live decision",
                decision);
        Map<String, Integer> pullActionSources =
                TdigwattObjectiveFactsReader
                    .readPullActionSources(
                        decision, scn.game(), DS);

        var randoContext =
                new com.gempukku.swccgo.ai.models.rando.evaluators
                    .DecisionContext(
                        scn.gameState(),
                        DS,
                        decision.getDecisionType().name(),
                        decision.getText(),
                        Integer.toString(
                            decision.getAwaitingDecisionId()),
                        scn.gameState().getCurrentPhase());
        populate(randoContext, decision);
        randoContext.setGame(scn.game());
        randoContext.setSide(Side.DARK);
        randoContext.setObjectiveAnalyzer(randoAnalyzer);
        randoContext.setDeckOracle(mock(
                com.gempukku.swccgo.ai.models.rando.strategy
                    .DeckOracle.class));
        if (!pullActionSources.isEmpty()) {
            randoContext.setExtra(
                    TdigwattObjectiveFactsReader
                        .PULL_ACTION_SOURCES_EXTRA,
                    pullActionSources);
        }
        if (deployingPermanentId != null) {
            randoContext.setExtra(
                    ObjectiveAnalyzer
                        .OBJECTIVE_DEPLOYING_CARD_ID_EXTRA,
                    deployingPermanentId);
        }

        var chosenContext =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators
                    .DecisionContext(
                        scn.gameState(),
                        DS,
                        decision.getDecisionType().name(),
                        decision.getText(),
                        Integer.toString(
                            decision.getAwaitingDecisionId()),
                        scn.gameState().getCurrentPhase());
        populate(chosenContext, decision);
        chosenContext.setGame(scn.game());
        chosenContext.setSide(Side.DARK);
        chosenContext.setObjectiveAnalyzer(chosenAnalyzer);
        chosenContext.setDeckOracle(mock(
                com.gempukku.swccgo.ai.models.chosenone.strategy
                    .DeckOracle.class));
        if (!pullActionSources.isEmpty()) {
            chosenContext.setExtra(
                    TdigwattObjectiveFactsReader
                        .PULL_ACTION_SOURCES_EXTRA,
                    pullActionSources);
        }
        if (deployingPermanentId != null) {
            chosenContext.setExtra(
                    ObjectiveAnalyzer
                        .OBJECTIVE_DEPLOYING_CARD_ID_EXTRA,
                    deployingPermanentId);
        }

        var rando =
                new com.gempukku.swccgo.ai.models.rando.evaluators
                    .CombinedEvaluator()
                    .evaluateDecision(randoContext);
        var chosen =
                new com.gempukku.swccgo.ai.models.chosenone.evaluators
                    .CombinedEvaluator()
                    .evaluateDecision(chosenContext);
        assertNotNull(rando);
        assertNotNull(chosen);
        Choice randoChoice = choice(
                decision,
                rando.getActionId(),
                rando.getActionType().name(),
                rando.getScore(),
                rando.isHardVetoed(),
                rando.getReasoning(),
                rando.getVetoReason());
        Choice chosenChoice = choice(
                decision,
                chosen.getActionId(),
                chosen.getActionType().name(),
                chosen.getScore(),
                chosen.isHardVetoed(),
                chosen.getReasoning(),
                chosen.getVetoReason());
        assertEquals(
                "Rando and Chosen One no-arg evaluators must match",
                randoChoice,
                chosenChoice);
        return randoChoice;
    }

    private static void populate(
            com.gempukku.swccgo.ai.models.rando.evaluators
                    .DecisionContext context,
            AwaitingDecision decision) {
        Raw raw = raw(decision);
        context.setActionIds(raw.actionIds());
        context.setActionTexts(raw.actionTexts());
        context.setCardIds(raw.cardIds());
        context.setBlueprints(raw.blueprints());
        context.setTestingTexts(raw.testingTexts());
        context.setSelectable(raw.selectable());
        context.setNoPass(raw.noPass());
        context.setMin(raw.min());
        context.setMax(raw.max());
    }

    private static void populate(
            com.gempukku.swccgo.ai.models.chosenone.evaluators
                    .DecisionContext context,
            AwaitingDecision decision) {
        Raw raw = raw(decision);
        context.setActionIds(raw.actionIds());
        context.setActionTexts(raw.actionTexts());
        context.setCardIds(raw.cardIds());
        context.setBlueprints(raw.blueprints());
        context.setTestingTexts(raw.testingTexts());
        context.setSelectable(raw.selectable());
        context.setNoPass(raw.noPass());
        context.setMin(raw.min());
        context.setMax(raw.max());
    }

    private static Raw raw(
            AwaitingDecision decision) {
        Map<String, String[]> params =
                decision.getDecisionParameters();
        return new Raw(
                strings(params, "actionId"),
                strings(params, "actionText"),
                strings(params, "cardId"),
                strings(params, "blueprintId"),
                strings(params, "testingText"),
                booleans(params, "selectable"),
                bool(params, "noPass", true),
                integer(params, "min", 0),
                integer(params, "max", 1));
    }

    private static Choice choice(
            AwaitingDecision decision,
            String actionId,
            String actionType,
            float score,
            boolean hardVeto,
            List<String> reasoning,
            String vetoReason) {
        Raw raw = raw(decision);
        int index = raw.actionIds().indexOf(actionId);
        if (index < 0) {
            index = raw.cardIds().indexOf(actionId);
        }
        return new Choice(
                actionId,
                actionType,
                score,
                hardVeto,
                value(raw.actionTexts(), index),
                value(raw.cardIds(), index),
                value(raw.blueprints(), index),
                List.copyOf(reasoning),
                vetoReason);
    }

    private static List<String> strings(
            Map<String, String[]> params,
            String key) {
        String[] values = params != null
                ? params.get(key) : null;
        return values == null
                ? List.of() : Arrays.asList(values);
    }

    private static List<Boolean> booleans(
            Map<String, String[]> params,
            String key) {
        return strings(params, key).stream()
                .map(Boolean::parseBoolean)
                .toList();
    }

    private static boolean bool(
            Map<String, String[]> params,
            String key,
            boolean fallback) {
        List<String> values = strings(params, key);
        return values.isEmpty()
                ? fallback
                : Boolean.parseBoolean(values.get(0));
    }

    private static int integer(
            Map<String, String[]> params,
            String key,
            int fallback) {
        List<String> values = strings(params, key);
        if (values.isEmpty()) {
            return fallback;
        }
        try {
            return Integer.parseInt(values.get(0));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String describe(
            AwaitingDecision decision) {
        return decision == null
                ? "no decision"
                : decision.getDecisionType() + " '"
                    + decision.getText() + "'";
    }

    private record PublicBots(
            com.gempukku.swccgo.ai.models.rando.RandoCalAi rando,
            com.gempukku.swccgo.ai.models.chosenone.TheChosenOneAi chosen) {
        private static PublicBots forGame(
                VirtualTableScenario scn) {
            var rando =
                    new com.gempukku.swccgo.ai.models.rando
                        .RandoCalAi();
            var chosen =
                    new com.gempukku.swccgo.ai.models.chosenone
                        .TheChosenOneAi();
            rando.setGame(scn.game());
            chosen.setGame(scn.game());
            return new PublicBots(rando, chosen);
        }

        private String decideBoth(
                VirtualTableScenario scn) {
            AwaitingDecision decision =
                    scn.GetAwaitingDecision(DS);
            assertNotNull(
                    "Dark Side must own the public bot decision",
                    decision);
            String randoResponse = rando.decide(
                    DS, decision, scn.gameState());
            String chosenResponse = chosen.decide(
                    DS, decision, scn.gameState());
            assertEquals(
                    "Public Rando and Chosen One must match",
                    randoResponse,
                    chosenResponse);
            return randoResponse;
        }
    }

    private record ActionRef(
            String actionId,
            String text,
            String cardId) {
    }

    private record Raw(
            List<String> actionIds,
            List<String> actionTexts,
            List<String> cardIds,
            List<String> blueprints,
            List<String> testingTexts,
            List<Boolean> selectable,
            boolean noPass,
            int min,
            int max) {
    }

    private record Choice(
            String actionId,
            String actionType,
            float score,
            boolean hardVeto,
            String text,
            String cardId,
            String blueprintId,
            List<String> reasoning,
            String vetoReason) {
    }
}
