package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.framework.StartingSetup;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import com.gempukku.swccgo.game.PhysicalCardImpl;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import static com.gempukku.swccgo.framework.TestBase.DS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BhbmSetupPayoffFactsReaderTest {

    @Test
    public void exactSetupEffectsInfluenceVaderStagingAndSafeBattle()
            throws Exception {
        VirtualTableScenario scn = scenario();
        scn.StartGame();
        var analyzer = analyzer(scn);
        PhysicalCardImpl vader = scn.GetDSCard("vader");
        PhysicalCardImpl battleground =
                scn.GetLSStartingLocation();

        assertTrue(Filters.battleground_site.accepts(
                scn.game(), battleground));
        assertTrue(BhbmSetupPayoffFactsReader
                .rewardsVaderAtBattleground(
                    scn.game(), DS, analyzer,
                    vader, battleground));
        PhysicalCardImpl bantha =
                scn.GetDSCard("bantha");
        PhysicalCardImpl blizzard =
                scn.GetDSCard("blizzard");
        scn.MoveCardsToLocation(
                battleground, bantha, blizzard);
        assertTrue("An open vehicle preserves presence at the site",
                BhbmSetupPayoffFactsReader
                    .rewardsVaderAtBattleground(
                        scn.game(), DS, analyzer,
                        vader, bantha));
        assertFalse("An enclosed vehicle does not",
                BhbmSetupPayoffFactsReader
                    .rewardsVaderAtBattleground(
                        scn.game(), DS, analyzer,
                        vader, blizzard));
        assertTrue(BhbmSetupPayoffFactsReader
                .insignificantRebellionActive(
                    scn.game(), DS, analyzer));

        PolicyOperation destiny = only(
                CaptureObjectivePolicy
                    .scoreBhbmYourDestiny(
                        new CaptureObjectivePolicy
                            .BhbmYourDestinyFacts(
                                "vader-to-bg", true)));
        assertEquals(
                "OBJECTIVE.BHBM.YOUR_DESTINY_BATTLEGROUND",
                destiny.ruleArmId().id());
        assertEquals(300.0f, destiny.delta(), 0.0f);

        PolicyOperation battle = only(
                CaptureObjectivePolicy
                    .scoreBhbmBattleWin(
                        new CaptureObjectivePolicy
                            .BhbmBattleWinFacts(
                                "safe-battle", true, true)));
        assertEquals(
                "BATTLE.OBJECTIVE.BHBM.INSIGNIFICANT_REBELLION",
                battle.ruleArmId().id());
        assertEquals(300.0f, battle.delta(), 0.0f);
    }

    @Test
    public void sourceSuppressionsRemoveThreeForceStagingCredit() {
        VirtualTableScenario targetAtBattleground = scenario();
        targetAtBattleground.StartGame();
        var targetAnalyzer = analyzer(
                targetAtBattleground);
        PhysicalCardImpl targetVader =
                targetAtBattleground.GetDSCard("vader");
        PhysicalCardImpl targetSite =
                targetAtBattleground.GetLSStartingLocation();
        targetAtBattleground.MoveCardsToLocation(
                targetSite,
                targetAtBattleground.GetLSCard("luke"));
        targetAtBattleground.SkipToPhase(Phase.DEPLOY);
        targetAtBattleground.PassAllResponses();
        assertFalse(BhbmSetupPayoffFactsReader
                .rewardsVaderAtBattleground(
                    targetAtBattleground.game(), DS,
                    targetAnalyzer, targetVader,
                    targetSite));

        VirtualTableScenario outOfPlay = scenario();
        outOfPlay.StartGame();
        var outOfPlayAnalyzer = analyzer(outOfPlay);
        PhysicalCardImpl outOfPlayVader =
                outOfPlay.GetDSCard("vader");
        PhysicalCardImpl outOfPlaySite =
                outOfPlay.GetLSStartingLocation();
        outOfPlay.MoveOutOfPlay(
                outOfPlay.GetLSCard("luke"));
        assertFalse(BhbmSetupPayoffFactsReader
                .rewardsVaderAtBattleground(
                    outOfPlay.game(), DS,
                    outOfPlayAnalyzer,
                    outOfPlayVader, outOfPlaySite));

        VirtualTableScenario inactiveEffect = scenario();
        inactiveEffect.StartGame();
        var inactiveAnalyzer = analyzer(inactiveEffect);
        inactiveEffect.MoveOutOfPlay(
                inactiveEffect.GetDSCard("destiny"));
        assertFalse(BhbmSetupPayoffFactsReader
                .rewardsVaderAtBattleground(
                    inactiveEffect.game(), DS,
                    inactiveAnalyzer,
                    inactiveEffect.GetDSCard("vader"),
                    inactiveEffect.GetLSStartingLocation()));
    }

    @Test
    public void projectedPresenceRecognizesOnlyDirectVaderOrOpenCarrier() {
        VirtualTableScenario direct = scenario();
        direct.StartGame();
        var directAnalyzer = analyzer(direct);
        PhysicalCardImpl directVader =
                direct.GetDSCard("vader");
        PhysicalCardImpl directSite =
                direct.GetLSStartingLocation();
        assertFalse(BhbmSetupPayoffFactsReader
                .currentlyRewardsVader(
                    direct.game(), DS,
                    directAnalyzer, directVader));
        direct.MoveCardsToLocation(
                directSite, directVader);
        assertTrue(BhbmSetupPayoffFactsReader
                .currentlyRewardsVader(
                    direct.game(), DS,
                    directAnalyzer, directVader));
        assertTrue(BhbmSetupPayoffFactsReader
                .projectedVaderMoveFormationSafe(
                    direct.game(), DS,
                    directVader, directSite));

        VirtualTableScenario openCarrier = scenario();
        openCarrier.StartGame();
        var openAnalyzer = analyzer(openCarrier);
        PhysicalCardImpl openSite =
                openCarrier.GetLSStartingLocation();
        PhysicalCardImpl bantha =
                openCarrier.GetDSCard("bantha");
        PhysicalCardImpl openVader =
                openCarrier.GetDSCard("vader");
        openCarrier.MoveCardsToLocation(
                openSite, bantha);
        openCarrier.MoveCardsToDSHand(
                openVader);
        for (int i = 0; i < 10; i++) {
            openCarrier.MoveCardsToTopOfDSForcePile(
                    openCarrier
                        .GetTopOfDSReserveDeck());
        }
        assertTrue(BhbmSetupPayoffFactsReader
                .rewardsVaderForDeployAt(
                    openCarrier.game(), DS,
                    openAnalyzer,
                    openVader, bantha));
        assertTrue(BhbmSetupPayoffFactsReader
                .hasLegalYourDestinyDeployDestination(
                    openCarrier.game(), DS,
                    openAnalyzer,
                    openVader));
        openCarrier.BoardAsPassenger(
                bantha, openVader);
        assertTrue(BhbmSetupPayoffFactsReader
                .rewardsVaderAtBattleground(
                    openCarrier.game(), DS,
                    openAnalyzer, bantha, openSite));
        assertTrue(BhbmSetupPayoffFactsReader
                .currentlyRewardsVader(
                    openCarrier.game(), DS,
                    openAnalyzer, bantha));
        assertTrue(BhbmSetupPayoffFactsReader
                .projectedVaderMoveFormationSafe(
                    openCarrier.game(), DS,
                    bantha, openSite));
        openCarrier.BoardAsPassenger(
                bantha,
                openCarrier.GetDSCard(
                    "stormtrooper"));
        assertFalse(BhbmSetupPayoffFactsReader
                .projectedVaderMoveFormationSafe(
                    openCarrier.game(), DS,
                    bantha, openSite));

        VirtualTableScenario enclosedCarrier = scenario();
        enclosedCarrier.StartGame();
        var enclosedAnalyzer =
                analyzer(enclosedCarrier);
        PhysicalCardImpl enclosedSite =
                enclosedCarrier.GetLSStartingLocation();
        PhysicalCardImpl blizzard =
                enclosedCarrier.GetDSCard("blizzard");
        PhysicalCardImpl enclosedVader =
                enclosedCarrier.GetDSCard("vader");
        enclosedCarrier.MoveCardsToLocation(
                enclosedSite, blizzard);
        enclosedCarrier.BoardAsPassenger(
                blizzard, enclosedVader);
        assertFalse(BhbmSetupPayoffFactsReader
                .rewardsVaderAtBattleground(
                    enclosedCarrier.game(), DS,
                    enclosedAnalyzer,
                    blizzard, enclosedSite));
        assertFalse(BhbmSetupPayoffFactsReader
                .currentlyRewardsVader(
                    enclosedCarrier.game(), DS,
                    enclosedAnalyzer, blizzard));
        assertFalse(BhbmSetupPayoffFactsReader
                .projectedVaderMoveFormationSafe(
                    enclosedCarrier.game(), DS,
                    blizzard, enclosedSite));
        assertFalse(BhbmSetupPayoffFactsReader
                .currentlyRewardsVader(
                    enclosedCarrier.game(), DS,
                    enclosedAnalyzer,
                    enclosedVader));
        assertTrue(BhbmSetupPayoffFactsReader
                .projectedVaderMoveFormationSafe(
                    enclosedCarrier.game(), DS,
                    enclosedVader, enclosedSite));
    }

    @Test
    public void payoffPoliciesFailClosedWithoutExactLiveFacts() {
        assertTrue(CaptureObjectivePolicy
                .scoreBhbmYourDestiny(
                    new CaptureObjectivePolicy
                        .BhbmYourDestinyFacts(
                            "not-ready", false))
                .operations().isEmpty());
        assertTrue(CaptureObjectivePolicy
                .scoreBhbmBattleWin(
                    new CaptureObjectivePolicy
                        .BhbmBattleWinFacts(
                            "no-effect", false, true))
                .operations().isEmpty());
        assertTrue(CaptureObjectivePolicy
                .scoreBhbmBattleWin(
                    new CaptureObjectivePolicy
                        .BhbmBattleWinFacts(
                            "unsafe", true, false))
                .operations().isEmpty());
    }

    @Test
    public void readerMatchesTheTwoPrintedSetupPayoffs()
            throws IOException {
        String rebellion = Files.readString(
                cardSource("set9/dark/Card9_127.java"));
        String destiny = Files.readString(
                cardSource("set9/dark/Card9_134.java"));
        String reader = Files.readString(
                repoRoot().resolve(
                    "src/gemp-swccg-server/src/main/java/"
                        + "com/gempukku/swccgo/ai/models/common/phase/"
                        + "BhbmSetupPayoffFactsReader.java"));

        assertTrue(rebellion.contains(
                "TriggerConditions.wonBattle("));
        assertTrue(rebellion.contains(
                "new LoseForceAndStackFaceDownEffect("));
        assertTrue(rebellion.contains(
                "new MultiplyEvaluator(3, "
                    + "new StackedEvaluator(self))"));
        assertTrue(destiny.contains(
                "Filters.Vader, "
                    + "Filters.presentAt(Filters.battleground_site)"));
        assertTrue(destiny.contains(
                "Filters.or(Filters.captive, "
                    + "Filters.presentAt(Filters.battleground_site))"));
        assertTrue(destiny.contains(
                "new LoseForceEffect(action, opponent, 3)"));
        assertTrue(reader.contains("\"9_127\""));
        assertTrue(reader.contains("\"9_134\""));
        assertTrue(reader.contains(
                "PhysicalCard yourDestiny"));
        assertTrue(reader.contains(
                "targetFilter(\n"
                    + "                    game, yourDestiny)"));
        assertFalse(reader.contains(
                "targetFilter(\n"
                    + "                    game, objective)"));
        assertTrue(reader.contains(
                "Filters.hasPermanentAboard("));
    }

    private static VirtualTableScenario scenario() {
        return new VirtualTableScenario(
                new HashMap<>() {{
                    put("luke", "9_24");
                }},
                new HashMap<>() {{
                    put("vader", "1_168");
                    put("bantha", "1_307");
                    put("blizzard", "3_154");
                    put("stormtrooper", "1_194");
                }},
                20,
                20,
                StartingSetup.DefaultLSGroundLocation,
                StartingSetup.BHBMObjective,
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open);
    }

    private static com.gempukku.swccgo.ai.models.rando.strategy
            .ObjectiveAnalyzer analyzer(
                VirtualTableScenario scn) {
        var analyzer =
                new com.gempukku.swccgo.ai.models.rando
                    .strategy.ObjectiveAnalyzer();
        analyzer.analyze(scn.game(), DS, Side.DARK);
        assertTrue(analyzer.isAnalyzed());
        assertEquals("9_151",
                analyzer.getObjectiveBlueprintId());
        return analyzer;
    }

    private static PolicyOperation only(
            com.gempukku.swccgo.ai.models.common.policy
                .PolicyResult result) {
        assertEquals(1, result.operations().size());
        return result.operations().get(0);
    }

    private static Path cardSource(String relative) {
        return repoRoot().resolve(
                "src/gemp-swccg-cards/src/main/java/"
                    + "com/gempukku/swccgo/cards/")
                .resolve(relative);
    }

    private static Path repoRoot() {
        Path cursor = Paths.get("")
                .toAbsolutePath().normalize();
        while (cursor != null) {
            if (Files.isDirectory(cursor.resolve(
                    "src/gemp-swccg-server/src/main/java"))
                    && Files.isDirectory(cursor.resolve(
                    "src/gemp-swccg-cards/src/main/java"))) {
                return cursor;
            }
            cursor = cursor.getParent();
        }
        throw new AssertionError(
                "Could not locate repository root");
    }
}
