package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.ai.models.common.phase.BattleForfeitPolicy;
import com.gempukku.swccgo.ai.models.common.phase.BattleDecisionPolicy;
import com.gempukku.swccgo.ai.models.common.phase.ForceLossFacts;
import com.gempukku.swccgo.ai.models.common.phase.ForceLossPolicy;
import com.gempukku.swccgo.ai.models.common.phase.MovePhysicalCardResolver;
import com.gempukku.swccgo.ai.models.common.phase.MoveObjectiveGateHoldPolicy;
import com.gempukku.swccgo.ai.models.common.phase.ObjectiveBattlePolicy;
import com.gempukku.swccgo.ai.models.common.phase.PullActionFacts;
import com.gempukku.swccgo.ai.models.common.phase.PullActionFactsReader;
import com.gempukku.swccgo.ai.models.common.phase.PullActionPolicy;
import com.gempukku.swccgo.ai.models.common.phase.PullOracleView;
import com.gempukku.swccgo.ai.models.common.phase.PullSelectionCandidatePolicy;
import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.set3.dark.Card3_158;
import com.gempukku.swccgo.cards.set222.dark.Card222_003;
import com.gempukku.swccgo.cards.set222.dark.Card222_009;
import com.gempukku.swccgo.cards.set222.dark.Card222_013;
import com.gempukku.swccgo.cards.set222.dark.Card222_014;
import com.gempukku.swccgo.cards.set222.dark.Card222_014_BACK;
import com.gempukku.swccgo.cards.set222.dark.Card222_030;
import com.gempukku.swccgo.cards.set222.dark.Card222_030_BACK;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.SpotOverride;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.framework.StartingSetup;
import com.gempukku.swccgo.framework.VirtualTableScenario;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.PhysicalCardImpl;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.game.state.WhileInPlayData;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import static com.gempukku.swccgo.framework.Assertions.assertInZone;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Card-source and engine proof for both printings of The Shield Will Be Down
 * In Moments. The tests use the Set 22 virtual Epic Event, not the classic
 * control-phase Event with the same title.
 */
public class ShieldTwinObjectiveEngineContractTest {
    private static final String ATTEMPT =
            "Attempt to 'blow away' Main Power Generators";

    private StartingSetup objectiveSetup(String objectiveBlueprintId) {
        return new StartingSetup() {
            @Override
            public HashMap<String, String> Cards() {
                return new HashMap<>() {{
                    put("objective", objectiveBlueprintId);
                    put("icePlains", "3_148");
                    put("northRidge", "217_12");
                    put("mainPowerGenerators", "222_9");
                    put("prepare", "209_42");
                }};
            }

            @Override
            public void Setup(VirtualTableScenario scn) {
                if (scn.DSDecisionAvailable("On which side")) {
                    scn.DSChoose("Left");
                }
                if (scn.DSDecisionAvailable("On which side")) {
                    scn.DSChoose("Left");
                }
            }
        };
    }

    private VirtualTableScenario scenario(String objectiveBlueprintId) {
        return scenario(objectiveBlueprintId, false);
    }

    private VirtualTableScenario scenario(
            String objectiveBlueprintId,
            boolean includeDuplicateBlizzard2) {
        HashMap<String, String> darkCards = new HashMap<>() {{
            put("thirdMarker", "3_144");
            put("otherHothSite", "3_146");
            put("target", "222_13");
            put("classicTarget", "3_115");
            put("blizzard2", "3_155");
            put("unpilotedAtAt", "3_154");
            put("pilot", "1_180");
            put("armedWeapon", "1_324");
            put("cannon", "222_3");
            put("cannon2", "222_3");
            put("classicCannon", "3_158");
            put("classicRangefinder", "3_95");
            put("classicPrepare", "13_82");
            put("blizzard4", "13_56");
            put("tableChangeTrigger", "3_110");
            put("offHothSite", "1_290");
        }};
        if (includeDuplicateBlizzard2) {
            darkCards.put("blizzard2Duplicate", "3_155");
        }
        return new VirtualTableScenario(
                new HashMap<>() {{
                    put("xwing", "1_146");
                    put("secondMarker", "3_63");
                }},
                darkCards,
                24,
                24,
                StartingSetup.DefaultLSGroundLocation,
                objectiveSetup(objectiveBlueprintId),
                StartingSetup.NoLSStartingInterrupts,
                StartingSetup.NoDSStartingInterrupts,
                StartingSetup.NoLSShields,
                StartingSetup.NoDSShields,
                VirtualTableScenario.Open
        );
    }

    @Test
    public void sourceTextNamesTheExactVirtualDestructionAndSurvivalLaws() {
        String frontText = new Card222_014().getGameText();
        String alternateFrontText = new Card222_030().getGameText();
        String backText = new Card222_014_BACK().getGameText();
        String alternateBackText = new Card222_030_BACK().getGameText();
        String targetText = new Card222_013().getGameText();

        assertEquals(frontText, alternateFrontText);
        assertTrue(frontText.contains(
                "Flip this card if Main Power Generators 'blown away.'"));
        assertEquals(backText, alternateBackText);
        assertTrue(backText.contains(
                "Place out of play if you do not occupy a Hoth site with an AT-AT, Imperial leader, or snowtrooper."));

        assertTrue(targetText.startsWith("Deploy on Ice Plains."));
        assertTrue(targetText.contains(
                "At the start of your deploy phase, if at 3rd Marker or lower"));
        assertTrue(targetText.contains(
                "Add 1 for each marker site you occupy (2 if you control)."));
        assertTrue(targetText.endsWith(
                "If total destiny > 8, Main Power Generators 'blown away.'"));
        String virtualCannonText = new Card222_003().getGameText();
        assertTrue(virtualCannonText.startsWith(
                "Deploy on your non-[Maintenance] AT-AT."));
        assertFalse(virtualCannonText.contains("Use 2 Force to deploy"));
        assertTrue(new Card3_158().getGameText().startsWith(
                "Use 2 Force to deploy on your AT-AT."));
        assertTrue(virtualCannonText.contains(
                "May target a character or vehicle at same or adjacent site"));
        assertTrue(virtualCannonText.contains(
                "When fired by Target The Main Generator, adds 1 to total."));
        assertTrue(new Card222_009().getLocationDarkSideGameText().contains(
                "If 'blown away,' Light Side loses 5 Force."));
    }

    @Test
    public void blizzardFourFreeWarriorActionBeatsWaitingForBothBots() {
        for (String objectiveBlueprintId : List.of("222_14", "222_30")) {
            VirtualTableScenario scn =
                    scenario(objectiveBlueprintId);
            var icePlains = scn.GetDSCard("icePlains");
            var blizzard4 = scn.GetDSCard("blizzard4");
            var cannon = scn.GetDSCard("cannon");
            var reserveWarrior = scn.GetDSCard("pilot");
            String actionText =
                    "Deploy an Imperial warrior from Reserve Deck";

            scn.StartGame();
            scn.MoveCardsToLocation(icePlains, blizzard4);
            assertEquals(0, scn.GetDSForcePileCount());
            assertInZone(Zone.RESERVE_DECK, reserveWarrior);
            for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
                assertTrue(analyzer
                        .isShieldBlizzardFourWarriorDeployAction(
                            scn.game(),
                            VirtualTableScenario.DS,
                            blizzard4, actionText));
                assertFalse(analyzer
                        .isShieldBlizzardFourWarriorDeployAction(
                            scn.game(),
                            VirtualTableScenario.DS,
                            cannon, actionText));
                List<EvaluatedCandidate> candidates =
                        actionTextAdapter(
                            analyzer, scn, blizzard4,
                            List.of("wait", "warrior"),
                            List.of(
                                "Take another action",
                                actionText));
                EvaluatedCandidate wait =
                        evaluatedCandidate(
                            candidates, "wait");
                EvaluatedCandidate warrior =
                        evaluatedCandidate(
                            candidates, "warrior");
                assertEquals(800.0f,
                        warrior.score() - wait.score(),
                        0.0f);
                assertTrue(warrior.reasoning().contains(
                        "HOTH SHIELD: take Blizzard 4's free legal Imperial warrior"));
                EvaluatedCandidate combined =
                        combinedActionAdapter(
                            analyzer, scn, blizzard4,
                            List.of("warrior"),
                            List.of(actionText));
                assertEquals("warrior",
                        combined.actionId());
                assertFalse(combined.reasoning(),
                        combined.reasoning().contains(
                            "Cannot afford"));
                assertFalse(combined.reasoning(),
                        combined.reasoning().contains(
                            "MAINTENANCE"));
            }

            for (PhysicalCard candidate
                    : scn.GetDSReserveDeck()
                        .stream().toList()) {
                if (Filters.and(
                            Filters.Imperial,
                            Filters.warrior)
                        .accepts(
                            scn.game(), candidate)) {
                    scn.MoveOutOfPlay(
                            (PhysicalCardImpl) candidate);
                }
            }
            for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
                assertFalse(analyzer
                        .isShieldBlizzardFourWarriorDeployAction(
                            scn.game(),
                            VirtualTableScenario.DS,
                            blizzard4, actionText));
                assertTrue(analyzer
                        .isShieldBlizzardFourWarriorDeployActionSource(
                            scn.game(),
                            VirtualTableScenario.DS,
                            blizzard4, actionText));
                List<EvaluatedCandidate> candidates =
                        actionTextAdapter(
                            analyzer, scn, blizzard4,
                            List.of("wait", "warrior"),
                            List.of(
                                "Take another action",
                                actionText));
                EvaluatedCandidate wait =
                        evaluatedCandidate(
                            candidates, "wait");
                EvaluatedCandidate noTarget =
                        evaluatedCandidate(
                            candidates, "warrior");
                assertFalse(noTarget.hardVeto());
                assertTrue(noTarget.score()
                        < wait.score() - 9000.0f);
                assertTrue(noTarget.reasoning(),
                        noTarget.reasoning().contains(
                            "HOTH SHIELD: no legal Imperial warrior in Reserve, do not enter the cancel path"));
                assertFalse(noTarget.reasoning().contains(
                        "HOTH SHIELD: take Blizzard 4's free legal Imperial warrior"));
                EvaluatedCandidate combined =
                        combinedActionAdapter(
                            analyzer, scn, blizzard4,
                            List.of("warrior", "pass"),
                            List.of(actionText, "Pass"));
                assertEquals("pass", combined.actionId());
            }
        }
    }

    @Test
    public void exactEightFailsForBothObjectivePrintings() {
        for (String objectiveBlueprintId : List.of("222_14", "222_30")) {
            VirtualTableScenario scn = readyToFire(
                    objectiveBlueprintId, 5);
            var objective = scn.GetDSCard("objective");
            var mainPowerGenerators =
                    scn.GetDSCard("mainPowerGenerators");
            var target = scn.GetDSCard("target");

            assertTrue(objectiveBlueprintId
                            + " decision="
                            + scn.GetCurrentDecision().getText()
                            + " dsActions=" + scn.GetDSAvailableActions()
                            + " lsActions=" + scn.GetLSAvailableActions(),
                    scn.DSCardActionAvailable(target, ATTEMPT));
            fireAtMainPowerGenerators(scn);

            assertFalse("Destiny 5 + controlled marker 2 + cannon 1 is exactly 8",
                    mainPowerGenerators.isBlownAway());
            assertFalse(objectiveBlueprintId, objective.isFlipped());
            assertInZone(Zone.SIDE_OF_TABLE, objective);
        }
    }

    @Test
    public void engineRangeFollowsTheLiveMarkerTopology() {
        for (String objectiveBlueprintId : List.of("222_14", "222_30")) {
            VirtualTableScenario canonicalScenario =
                    scenario(objectiveBlueprintId);
            var canonicalThird =
                    canonicalScenario.GetDSCard("thirdMarker");
            var canonicalTarget =
                    canonicalScenario.GetDSCard("target");
            var canonicalBlizzard =
                    canonicalScenario.GetDSCard("blizzard2");
            var canonicalCannon =
                    canonicalScenario.GetDSCard("cannon");

            canonicalScenario.StartGame();
            canonicalScenario.MoveLocationToTable(canonicalThird);
            canonicalScenario.MoveCardsToLocation(
                    canonicalThird, canonicalBlizzard);
            canonicalScenario.AttachCardsTo(
                    canonicalBlizzard, canonicalCannon);
            canonicalScenario.AttachCardsTo(
                    canonicalThird, canonicalTarget);
            canonicalScenario.SkipToPhase(Phase.DEPLOY);

            assertTrue("With no 2nd Marker printing on table, 3rd is adjacent to MPG",
                    canonicalScenario.DSCardActionAvailable(
                            canonicalTarget, ATTEMPT));

            VirtualTableScenario thirdMarkerScenario =
                    scenario(objectiveBlueprintId);
            var secondMarker =
                    thirdMarkerScenario.GetLSCard("secondMarker");
            var thirdMarker =
                    thirdMarkerScenario.GetDSCard("thirdMarker");
            var target = thirdMarkerScenario.GetDSCard("target");
            var blizzard2 =
                    thirdMarkerScenario.GetDSCard("blizzard2");
            var cannon = thirdMarkerScenario.GetDSCard("cannon");

            thirdMarkerScenario.StartGame();
            thirdMarkerScenario.MoveLocationToTable(secondMarker);
            thirdMarkerScenario.MoveLocationToTable(thirdMarker);
            thirdMarkerScenario.MoveCardsToLocation(
                    thirdMarker, blizzard2);
            thirdMarkerScenario.AttachCardsTo(blizzard2, cannon);
            thirdMarkerScenario.AttachCardsTo(thirdMarker, target);
            thirdMarkerScenario.SkipToPhase(Phase.DEPLOY);

            assertFalse("An inserted 2nd Marker extends the route beyond 3rd",
                    thirdMarkerScenario.DSCardActionAvailable(
                            target, ATTEMPT));

            VirtualTableScenario secondMarkerScenario =
                    readyToFire(objectiveBlueprintId, 5);
            assertTrue("With 2nd inserted, 222_3 reaches MPG there",
                    secondMarkerScenario.DSCardActionAvailable(
                            secondMarkerScenario.GetDSCard("target"),
                            ATTEMPT));
        }
    }

    @Test
    public void bothBotFacadesHydrateCanonicalAndExtendedFiringPackages() {
        for (String objectiveBlueprintId : List.of("222_14", "222_30")) {
            VirtualTableScenario canonical =
                    scenario(objectiveBlueprintId);
            var canonicalThird =
                    canonical.GetDSCard("thirdMarker");
            var canonicalTarget =
                    canonical.GetDSCard("target");
            var canonicalBlizzard =
                    canonical.GetDSCard("blizzard2");
            var canonicalCannon =
                    canonical.GetDSCard("cannon");
            canonical.StartGame();
            canonical.MoveLocationToTable(canonicalThird);
            canonical.MoveCardsToLocation(
                    canonicalThird, canonicalBlizzard);
            canonical.AttachCardsTo(
                    canonicalBlizzard, canonicalCannon);
            canonical.AttachCardsTo(
                    canonicalThird, canonicalTarget);
            for (ObjectiveAnalyzer analyzer : analyzers(canonical)) {
                assertTrue(onlyState(
                        analyzer, canonical,
                        "preFlip", "flip")
                        .conditionSatisfied());
            }

            VirtualTableScenario scn = scenario(objectiveBlueprintId);
            var secondMarker = scn.GetLSCard("secondMarker");
            var thirdMarker = scn.GetDSCard("thirdMarker");
            var target = scn.GetDSCard("target");
            var blizzard2 = scn.GetDSCard("blizzard2");
            var unpilotedAtAt = scn.GetDSCard("unpilotedAtAt");
            var cannon = scn.GetDSCard("cannon");

            scn.StartGame();
            scn.MoveLocationToTable(secondMarker);
            scn.MoveLocationToTable(thirdMarker);
            scn.MoveCardsToLocation(secondMarker, blizzard2);
            scn.AttachCardsTo(blizzard2, cannon);
            scn.AttachCardsTo(secondMarker, target);

            for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
                assertEquals("The Shield Will Be Down in Moments",
                        analyzer.getActivePlaybook().label);
                assertTrue(analyzer.getStartingLocationIds().containsAll(
                        List.of("3_148", "208_49", "217_12", "222_9")));
                assertTrue(analyzer.getStartingEffectIds()
                        .contains("209_42"));
                assertTrue(analyzer.isRequiredCardForFlip(
                        "Target The Main Generator (V)"));
                assertTrue(analyzer.isPullableCard(
                        "Target The Main Generator"));
                assertTrue(analyzer.isPullableCard("AT-AT Cannon"));
                assertTrue(analyzer.isPullableCard(
                        "Prepare For A Surface Attack"));
                assertTrue(onlyState(
                        analyzer, scn, "preFlip", "flip")
                        .conditionSatisfied());
            }

            scn.MoveCardsToLocation(secondMarker, unpilotedAtAt);
            scn.AttachCardsTo(unpilotedAtAt, cannon);
            assertFalse(Filters.piloted.accepts(
                    scn.game(), unpilotedAtAt));
            for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
                assertFalse(onlyState(
                        analyzer, scn, "preFlip", "flip")
                        .conditionSatisfied());
            }

            scn.MoveOutOfPlay(target);
            for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
                assertFalse(onlyState(
                        analyzer, scn, "preFlip", "flip")
                        .conditionSatisfied());
            }
        }
    }

    @Test
    public void eventAndFiringPackageMustShareOnePhysicalMarker() {
        for (String objectiveBlueprintId : List.of("222_14", "222_30")) {
            VirtualTableScenario scn = scenario(objectiveBlueprintId);
            var secondMarker = scn.GetLSCard("secondMarker");
            var thirdMarker = scn.GetDSCard("thirdMarker");
            var target = scn.GetDSCard("target");
            var blizzard2 = scn.GetDSCard("blizzard2");
            var cannon = scn.GetDSCard("cannon");

            scn.StartGame();
            scn.MoveLocationToTable(secondMarker);
            scn.MoveLocationToTable(thirdMarker);
            scn.AttachCardsTo(thirdMarker, target);
            scn.MoveCardsToLocation(secondMarker, blizzard2);
            scn.AttachCardsTo(blizzard2, cannon);

            for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
                assertFalse("Split sites cannot execute 222_13",
                        onlyState(
                            analyzer, scn, "preFlip", "flip")
                            .conditionSatisfied());
                assertTrue(analyzer
                        .isMissingPreFlipRequirementAt(
                            scn.game(),
                            VirtualTableScenario.DS,
                            thirdMarker));
                assertTrue(analyzer
                        .isMissingPreFlipRequirementAt(
                            scn.game(),
                            VirtualTableScenario.DS,
                            secondMarker));
            }

            scn.AttachCardsTo(secondMarker, target);
            for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
                assertTrue("Co-located event and package are executable",
                        onlyState(
                            analyzer, scn, "preFlip", "flip")
                            .conditionSatisfied());
            }
        }
    }

    @Test
    public void exactVirtualPreparePullSurvivesAllParentVetoBoundaries() {
        for (String objectiveBlueprintId : List.of("222_14", "222_30")) {
            VirtualTableScenario scn = scenario(objectiveBlueprintId);
            var prepare = scn.GetDSCard("prepare");
            var classicPrepare = scn.GetDSCard("classicPrepare");
            var objective = scn.GetDSCard("objective");
            var target = scn.GetDSCard("target");
            var cannon = scn.GetDSCard("cannon");
            var classicCannon =
                    scn.GetDSCard("classicCannon");
            var blizzard2 = scn.GetDSCard("blizzard2");
            var icePlains = scn.GetDSCard("icePlains");
            var northRidge = scn.GetDSCard("northRidge");
            var pilot = scn.GetDSCard("pilot");
            var armedWeapon = scn.GetDSCard("armedWeapon");

            scn.StartGame();
            scn.MoveCardsToDSHand(target);
            scn.MoveCardsToLocation(icePlains, blizzard2);
            scn.MoveCardsToLocation(northRidge, pilot);
            scn.AttachCardsTo(pilot, armedWeapon);
            scn.MoveCardsToDSSideOfTable(classicPrepare);
            trimDarkReserveTo(
                    scn, cannon, classicCannon);

            assertEquals(2, scn.gameState().getReserveDeckSize(
                    VirtualTableScenario.DS));
            assertEquals(0, scn.GetDSForcePileCount());
            assertTrue(scn.gameState()
                    .getAttachedCards(pilot)
                    .contains(armedWeapon));

            for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
                assertTrue(analyzer
                        .objectivePullAdvancesRequiredOnTableCard(
                                scn.game(), VirtualTableScenario.DS,
                                prepare));
                assertFalse(analyzer
                        .objectivePullAdvancesRequiredOnTableCard(
                                scn.game(), VirtualTableScenario.DS,
                                classicPrepare));
                assertFalse(analyzer
                        .objectivePullAdvancesRequiredOnTableCard(
                                scn.game(), VirtualTableScenario.DS,
                                objective));

                PullActionFactsReader.Context context =
                        pullContext(scn, analyzer);
                PullActionFacts.Parent virtualFacts =
                        PullActionFactsReader.readParent(
                                "virtual-prepare",
                                "Take card into hand from Reserve Deck",
                                String.valueOf(prepare.getCardId()),
                                context);
                PullActionFacts.Parent classicFacts =
                        PullActionFactsReader.readParent(
                                "classic-prepare",
                                "Take card into hand from Reserve Deck",
                                String.valueOf(
                                        classicPrepare.getCardId()),
                                context);

                assertTrue(virtualFacts.requiredOnTableCardPull());
                assertTrue(virtualFacts
                        .requiredOnTableCardPullVetoBypass());
                assertEquals(2, virtualFacts.reserveSize());
                assertTrue(virtualFacts.weaponPull());
                assertEquals(0, virtualFacts.unarmedCharacters());
                assertNull(virtualFacts.cheapestTargetCost());
                assertEquals(0, virtualFacts.availableForce());
                assertFalse(
                        virtualFacts.allReserveTargetsUnattachableWeapons());
                assertFalse(classicFacts.requiredOnTableCardPull());
                assertFalse(classicFacts
                        .requiredOnTableCardPullVetoBypass());

                PolicyResult virtualPolicy =
                        PullActionPolicy.evaluateParent(
                                virtualFacts).result();
                PolicyResult classicPolicy =
                        PullActionPolicy.evaluateParent(
                                classicFacts).result();
                PolicyOperation requiredPull = operation(
                        virtualPolicy,
                        "PULL.OBJECTIVE.REQUIRED_ON_TABLE_CARD");
                assertEquals(1000.0f, requiredPull.delta(), 0.0f);
                assertFalse(hasOperation(
                        virtualPolicy, "V60-reserve-risk"));
                assertFalse(hasOperation(
                        virtualPolicy, "V185-ate"));
                assertFalse(hasOperation(
                        virtualPolicy, "V67ac"));
                assertFalse(hasOperation(
                        classicPolicy,
                        "PULL.OBJECTIVE.REQUIRED_ON_TABLE_CARD"));
                assertTrue(hasOperation(
                        classicPolicy, "V60-reserve-risk"));

                List<PhysicalCard> reserveCandidates =
                        new ArrayList<>(
                                scn.gameState().getReserveDeck(
                                        VirtualTableScenario.DS));
                List<EvaluatedCandidate> childChoices =
                        temporaryPullCandidateAdapter(
                            analyzer, scn,
                            reserveCandidates.toArray(
                                    new PhysicalCard[0]));
                EvaluatedCandidate cannonChoice =
                        evaluatedCandidate(
                            childChoices,
                            "temp" + reserveCandidates.indexOf(
                                    cannon));
                EvaluatedCandidate blockedClassic =
                        evaluatedCandidate(
                            childChoices,
                            "temp" + reserveCandidates.indexOf(
                                    classicCannon));
                assertFalse(cannonChoice.vetoReason(),
                        cannonChoice.hardVeto());
                assertTrue(cannonChoice.reasoning(),
                        cannonChoice.reasoning().contains(
                            "active table presence is required to flip"));
                assertTrue(blockedClassic.reasoning(),
                        blockedClassic.reasoning().contains(
                            "V70 NO 2ND WEAPON"));
                assertFalse(blockedClassic.reasoning(),
                        blockedClassic.reasoning().contains(
                            "active table presence is required to flip"));
                assertTrue(blockedClassic.reasoning(),
                        blockedClassic.score()
                            < cannonChoice.score() - 9000.0f);
            }
        }
    }

    @Test
    public void hothLocationDownloadPrefersTheMissingThirdMarkerStage() {
        for (String objectiveBlueprintId : List.of("222_14", "222_30")) {
            VirtualTableScenario scn = scenario(objectiveBlueprintId);
            var thirdMarker = scn.GetDSCard("thirdMarker");
            var otherHothSite = scn.GetDSCard("otherHothSite");

            scn.StartGame();
            for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
                assertEquals(
                        ObjectiveAnalyzer
                            .ObjectiveProgressCandidateRole
                            .REQUIRED_LOCATION,
                        analyzer.classifyPreFlipProgressCandidate(
                                scn.game(),
                                VirtualTableScenario.DS,
                                thirdMarker));
                assertEquals(
                        ObjectiveAnalyzer
                            .ObjectiveProgressCandidateRole.NONE,
                        analyzer.classifyPreFlipProgressCandidate(
                                scn.game(),
                                VirtualTableScenario.DS,
                                otherHothSite));

                List<EvaluatedCandidate> candidates =
                        pullCandidateAdapter(
                                analyzer, scn,
                                thirdMarker, otherHothSite);
                EvaluatedCandidate routeStage =
                        evaluatedCandidate(
                                candidates, thirdMarker);
                EvaluatedCandidate decoy =
                        evaluatedCandidate(
                                candidates, otherHothSite);
                assertTrue(routeStage.reasoning(),
                        routeStage.reasoning().contains(
                                "missing location required by the counted objective"));
                assertFalse(decoy.reasoning(),
                        decoy.reasoning().contains(
                                "missing location required by the counted objective"));
                assertTrue(routeStage.score()
                        > decoy.score() + 250.0f);
            }
        }
    }

    @Test
    public void physicalPreparePrintingOwnsPullableAndForceLossProtection() {
        for (String objectiveBlueprintId : List.of("222_14", "222_30")) {
            VirtualTableScenario scn = scenario(objectiveBlueprintId);
            var prepare = scn.GetDSCard("prepare");
            var classicPrepare = scn.GetDSCard("classicPrepare");

            scn.StartGame();
            scn.MoveCardsToDSHand(prepare, classicPrepare);

            for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
                assertTrue(analyzer.isPullableCard(prepare));
                assertFalse(analyzer.isPullableCard(classicPrepare));

                PolicyResult virtualLoss = forceLossPolicy(
                        scn, prepare, false,
                        analyzer.isPullableCard(prepare));
                PolicyResult classicLoss = forceLossPolicy(
                        scn, classicPrepare, false,
                        analyzer.isPullableCard(classicPrepare));
                assertEquals(-9999.0f,
                        operation(
                                virtualLoss,
                                "V21-objective").delta(),
                        0.0f);
                assertFalse(hasOperation(
                        classicLoss, "V21-objective"));
            }
        }
    }

    @Test
    public void noWalkerOpeningSelectsAndBudgetsTheActionableHandPackage() {
        for (String objectiveBlueprintId : List.of("222_14", "222_30")) {
            VirtualTableScenario scn = scenario(objectiveBlueprintId);
            var icePlains = scn.GetDSCard("icePlains");
            var northRidge = scn.GetDSCard("northRidge");
            var prepare = scn.GetDSCard("prepare");
            var blizzard2 = scn.GetDSCard("blizzard2");
            var cannon = scn.GetDSCard("cannon");

            scn.StartGame();
            scn.MoveCardsToDSHand(blizzard2, cannon);
            assertFalse("Opening fixture must have no AT-AT on table",
                    scn.gameState().getAllPermanentCards().stream()
                            .anyMatch(card ->
                                    card.getZone() != null
                                    && card.getZone().isInPlay()
                                    && Filters.AT_AT.accepts(
                                        scn.game(), card)));

            int hostCost =
                    deployCostAt(scn, blizzard2, icePlains);
            int cannonCost =
                    deployCostAt(scn, cannon, blizzard2);
            for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
                assertTrue(analyzer.isPullableCard(cannon));
                assertTrue(analyzer.isPullableCard(prepare));
                assertTrue(analyzer
                        .objectivePullAdvancesRequiredOnTableCard(
                                scn.game(),
                                VirtualTableScenario.DS,
                                prepare));
                assertEquals(
                        ObjectiveAnalyzer
                            .ObjectiveProgressCandidateRole
                            .REQUIRED_ACTOR,
                        analyzer.classifyPreFlipProgressCandidate(
                                scn.game(),
                                VirtualTableScenario.DS,
                                blizzard2));
                assertTrue(analyzer.advancesPreFlipRequirementAt(
                        scn.game(), VirtualTableScenario.DS,
                        blizzard2, icePlains));
                List<EvaluatedCandidate> destinations =
                        deployDestinationAdapter(
                                analyzer, scn, blizzard2,
                                icePlains, northRidge);
                EvaluatedCandidate iceDeploy =
                        evaluatedCandidate(
                                destinations, icePlains);
                EvaluatedCandidate ridgeDeploy =
                        evaluatedCandidate(
                                destinations, northRidge);
                assertTrue(iceDeploy.reasoning(),
                        iceDeploy.reasoning().contains(
                                "advance a missing counted-objective location"));
                assertFalse(ridgeDeploy.reasoning(),
                        ridgeDeploy.reasoning().contains(
                                "advance a missing counted-objective location"));
                assertTrue(iceDeploy.score()
                        > ridgeDeploy.score() + 500.0f);

                assertEquals(hostCost + cannonCost,
                        analyzer
                            .getRequiredOnTableCardForceReserve(
                                scn.game(),
                                VirtualTableScenario.DS));
                assertEquals(cannonCost,
                        analyzer
                            .getRequiredOnTableCardForceReserve(
                                scn.game(),
                                VirtualTableScenario.DS,
                                blizzard2));
                assertEquals(hostCost,
                        analyzer
                            .getRequiredOnTableCardForceReserve(
                                scn.game(),
                                VirtualTableScenario.DS,
                                cannon));
            }
        }
    }

    @Test
    public void nondeployableUniqueHostDoesNotSuppressTheLegalHandPackage() {
        for (String objectiveBlueprintId : List.of("222_14", "222_30")) {
            VirtualTableScenario scn =
                    scenario(objectiveBlueprintId, true);
            var icePlains = scn.GetDSCard("icePlains");
            var offHothSite = scn.GetDSCard("offHothSite");
            var liveBlizzard2 = scn.GetDSCard("blizzard2");
            var deadBlizzard2 =
                    scn.GetDSCard("blizzard2Duplicate");
            var legalAtAt = scn.GetDSCard("unpilotedAtAt");
            var pilot = scn.GetDSCard("pilot");
            var cannon = scn.GetDSCard("cannon");

            scn.StartGame();
            scn.MoveLocationToTable(offHothSite);
            scn.MoveCardsToLocation(
                    offHothSite, liveBlizzard2);
            scn.MoveCardsToDSHand(
                    deadBlizzard2, legalAtAt,
                    pilot, cannon);

            assertFalse(Filters.deployableToLocation(
                        deadBlizzard2,
                        Filters.sameCardId(icePlains),
                        true, 0.0f)
                    .accepts(
                        scn.gameState(),
                        scn.game().getModifiersQuerying(),
                        deadBlizzard2));
            for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
                assertEquals(
                        ObjectiveAnalyzer
                            .ObjectiveProgressCandidateRole.NONE,
                        analyzer.classifyPreFlipProgressCandidate(
                                scn.game(),
                                VirtualTableScenario.DS,
                                deadBlizzard2));
                assertEquals(
                        ObjectiveAnalyzer
                            .ObjectiveProgressCandidateRole
                            .REQUIRED_ACTOR,
                        analyzer.classifyPreFlipProgressCandidate(
                                scn.game(),
                                VirtualTableScenario.DS,
                                legalAtAt));
            }
        }
    }

    @Test
    public void offRouteCannonDoesNotSuppressTheUsableReserveCopy() {
        for (String objectiveBlueprintId : List.of("222_14", "222_30")) {
            VirtualTableScenario scn = scenario(objectiveBlueprintId);
            var northRidge = scn.GetDSCard("northRidge");
            var prepare = scn.GetDSCard("prepare");
            var target = scn.GetDSCard("target");
            var offRouteHost = scn.GetDSCard("blizzard2");
            var offRouteCannon = scn.GetDSCard("cannon");
            var routeHost = scn.GetDSCard("unpilotedAtAt");
            var pilot = scn.GetDSCard("pilot");
            var reserveCannon = scn.GetDSCard("cannon2");

            scn.StartGame();
            scn.MoveCardsToLocation(
                    northRidge, offRouteHost);
            scn.AttachCardsTo(
                    offRouteHost, offRouteCannon);
            scn.MoveCardsToDSHand(
                    target, routeHost, pilot);
            assertInZone(
                    Zone.RESERVE_DECK, reserveCannon);

            for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
                assertFalse(analyzer
                        .isRequiredCardActiveOnTable(
                            scn.game(), "AT-AT Cannon"));
                assertTrue(analyzer
                        .objectivePullAdvancesRequiredOnTableCard(
                            scn.game(),
                            VirtualTableScenario.DS,
                            prepare));
                assertEquals(
                        ObjectiveAnalyzer
                            .ObjectiveProgressCandidateRole
                            .REQUIRED_ON_TABLE_CARD,
                        analyzer.classifyPreFlipProgressCandidate(
                            scn.game(),
                            VirtualTableScenario.DS,
                            reserveCannon));
                assertTrue(analyzer
                        .isPreferredRequiredCardForceLossCandidate(
                            scn.game(),
                            VirtualTableScenario.DS,
                            reserveCannon));
            }
        }
    }

    @Test
    public void cannonPullWaitsForAnExecutableHostAndPilotRoute() {
        for (String objectiveBlueprintId : List.of("222_14", "222_30")) {
            VirtualTableScenario scn = scenario(objectiveBlueprintId);
            var prepare = scn.GetDSCard("prepare");
            var target = scn.GetDSCard("target");
            var blizzard2 = scn.GetDSCard("blizzard2");
            var unpilotedAtAt = scn.GetDSCard("unpilotedAtAt");
            var pilot = scn.GetDSCard("pilot");
            var cannon = scn.GetDSCard("cannon");

            scn.StartGame();
            scn.MoveCardsToDSHand(target);
            scn.MoveOutOfPlay(blizzard2);
            scn.MoveOutOfPlay(unpilotedAtAt);
            scn.MoveOutOfPlay(pilot);
            assertInZone(Zone.RESERVE_DECK, cannon);

            for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
                assertFalse(analyzer
                        .isRequiredOnTableCardPullRouteReady(
                                scn.game(),
                                VirtualTableScenario.DS,
                                cannon));
                assertFalse(analyzer
                        .objectivePullAdvancesRequiredOnTableCard(
                                scn.game(),
                                VirtualTableScenario.DS,
                                prepare));
                assertEquals(
                        ObjectiveAnalyzer
                            .ObjectiveProgressCandidateRole.NONE,
                        analyzer.classifyPreFlipProgressCandidate(
                                scn.game(),
                                VirtualTableScenario.DS,
                                cannon));
            }
        }
    }

    @Test
    public void solePilotWaitsForItsHostThenTargetsThatHost() {
        for (String objectiveBlueprintId : List.of("222_14", "222_30")) {
            VirtualTableScenario scn = scenario(objectiveBlueprintId);
            var icePlains = scn.GetDSCard("icePlains");
            var northRidge = scn.GetDSCard("northRidge");
            var host = scn.GetDSCard("unpilotedAtAt");
            var pilot = scn.GetDSCard("pilot");
            var cannon = scn.GetDSCard("cannon");

            scn.StartGame();
            scn.MoveCardsToDSHand(host, pilot, cannon);

            for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
                assertEquals(
                        ObjectiveAnalyzer
                            .ObjectiveProgressCandidateRole
                            .REQUIRED_ACTOR,
                        analyzer.classifyPreFlipProgressCandidate(
                                scn.game(),
                                VirtualTableScenario.DS,
                                host));
                assertEquals(
                        ObjectiveAnalyzer
                            .ObjectiveProgressCandidateRole.NONE,
                        analyzer.classifyPreFlipProgressCandidate(
                                scn.game(),
                                VirtualTableScenario.DS,
                                pilot));
                assertFalse(analyzer.advancesPreFlipRequirementAt(
                        scn.game(), VirtualTableScenario.DS,
                        pilot, northRidge));
            }

            scn.MoveCardsToLocation(icePlains, host);
            for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
                assertEquals(
                        ObjectiveAnalyzer
                            .ObjectiveProgressCandidateRole
                            .REQUIRED_ACTOR,
                        analyzer.classifyPreFlipProgressCandidate(
                                scn.game(),
                                VirtualTableScenario.DS,
                                pilot));
                assertTrue(analyzer.advancesPreFlipRequirementAt(
                        scn.game(), VirtualTableScenario.DS,
                        pilot, host));
                assertFalse(analyzer.advancesPreFlipRequirementAt(
                        scn.game(), VirtualTableScenario.DS,
                        pilot, northRidge));
            }
        }
    }

    @Test
    public void selectedPilotWaitsForDeployButIsProtectedFromForceLoss() {
        for (String objectiveBlueprintId : List.of("222_14", "222_30")) {
            VirtualTableScenario scn = scenario(objectiveBlueprintId);
            var target = scn.GetDSCard("target");
            var host = scn.GetDSCard("unpilotedAtAt");
            var pilot = scn.GetDSCard("pilot");
            var cannon = scn.GetDSCard("cannon");
            var disposable =
                    scn.GetDSCard("tableChangeTrigger");

            scn.StartGame();
            scn.MoveCardsToDSHand(
                    target, host, pilot,
                    cannon, disposable);

            for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
                assertEquals(
                        ObjectiveAnalyzer
                            .ObjectiveProgressCandidateRole.NONE,
                        analyzer.classifyPreFlipProgressCandidate(
                                scn.game(),
                                VirtualTableScenario.DS,
                                pilot));
                assertTrue(analyzer
                        .isPreferredShieldRoutePackageForceLossCandidate(
                                scn.game(),
                                VirtualTableScenario.DS,
                                pilot));

                List<EvaluatedCandidate> lossCandidates =
                        forceLossAdapter(
                            analyzer, scn,
                            pilot, disposable);
                EvaluatedCandidate protectedPilot =
                        evaluatedCandidate(
                            lossCandidates, pilot);
                EvaluatedCandidate ordinaryLoss =
                        evaluatedCandidate(
                            lossCandidates, disposable);
                assertTrue(protectedPilot.reasoning(),
                        protectedPilot.reasoning().contains(
                            "OBJECTIVE CRITICAL IN HAND - NEVER LOSE"));
                assertFalse(ordinaryLoss.reasoning(),
                        ordinaryLoss.reasoning().contains(
                            "OBJECTIVE CRITICAL IN HAND - NEVER LOSE"));
                assertTrue(protectedPilot.reasoning(),
                        protectedPilot.score()
                            < ordinaryLoss.score() - 9000.0f);
            }
        }
    }

    @Test
    public void selectedPackageSurvivesTheReplayReserveBottomPrompt() {
        for (String objectiveBlueprintId : List.of("222_14", "222_30")) {
            VirtualTableScenario scn =
                    scenario(objectiveBlueprintId);
            var target = scn.GetDSCard("target");
            var host = scn.GetDSCard("unpilotedAtAt");
            var pilot = scn.GetDSCard("pilot");
            var cannon = scn.GetDSCard("cannon");
            var disposable =
                    scn.GetDSCard("tableChangeTrigger");

            scn.StartGame();
            scn.MoveCardsToDSHand(
                    target, host, pilot,
                    cannon, disposable);

            for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
                List<EvaluatedCandidate> candidates =
                        forceLossAdapter(
                            analyzer, scn,
                            "Choose card to put on bottom of Reserve Deck",
                            pilot, disposable);
                EvaluatedCandidate protectedPilot =
                        evaluatedCandidate(candidates, pilot);
                EvaluatedCandidate ordinaryLoss =
                        evaluatedCandidate(candidates, disposable);
                assertFalse(protectedPilot.hardVeto());
                assertTrue(protectedPilot.reasoning(),
                        protectedPilot.reasoning().contains(
                            "HOTH SHIELD PACKAGE: retain the selected host, pilot, and Cannon"));
                assertFalse(ordinaryLoss.hardVeto());
                assertTrue(protectedPilot.score()
                        < ordinaryLoss.score() - 9000.0f);
            }
        }
    }

    @Test
    public void incompleteTableHostDoesNotBeatTheActionableHandPackage() {
        for (String objectiveBlueprintId : List.of("222_14", "222_30")) {
            VirtualTableScenario scn = scenario(objectiveBlueprintId);
            var icePlains = scn.GetDSCard("icePlains");
            var blizzard2 = scn.GetDSCard("blizzard2");
            var incompleteHost = scn.GetDSCard("unpilotedAtAt");
            var pilot = scn.GetDSCard("pilot");
            var reserveCannon = scn.GetDSCard("cannon");
            var handCannon = scn.GetDSCard("cannon2");

            scn.StartGame();
            scn.MoveCardsToLocation(icePlains, incompleteHost);
            scn.MoveCardsToDSHand(blizzard2, handCannon);
            assertInZone(Zone.RESERVE_DECK, pilot, reserveCannon);
            assertFalse(Filters.piloted.accepts(
                    scn.game(), incompleteHost));

            int hostCost =
                    deployCostAt(scn, blizzard2, icePlains);
            int cannonCost =
                    deployCostAt(scn, handCannon, blizzard2);
            for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
                assertEquals(
                        ObjectiveAnalyzer
                            .ObjectiveProgressCandidateRole.NONE,
                        analyzer.classifyPreFlipProgressCandidate(
                                scn.game(),
                                VirtualTableScenario.DS,
                                incompleteHost));
                assertEquals(
                        ObjectiveAnalyzer
                            .ObjectiveProgressCandidateRole
                            .REQUIRED_ACTOR,
                        analyzer.classifyPreFlipProgressCandidate(
                                scn.game(),
                                VirtualTableScenario.DS,
                                blizzard2));
                assertTrue(analyzer.advancesPreFlipRequirementAt(
                        scn.game(), VirtualTableScenario.DS,
                        blizzard2, icePlains));
                assertEquals(hostCost + cannonCost,
                        analyzer
                            .getRequiredOnTableCardForceReserve(
                                scn.game(),
                                VirtualTableScenario.DS));
            }
        }
    }

    @Test
    public void childRouteAndCannonReserveUsePhysicalPrintings() {
        for (String objectiveBlueprintId : List.of("222_14", "222_30")) {
            VirtualTableScenario scn = scenario(objectiveBlueprintId);
            var target = scn.GetDSCard("target");
            var classicTarget = scn.GetDSCard("classicTarget");
            var cannon = scn.GetDSCard("cannon");
            var classicCannon = scn.GetDSCard("classicCannon");
            var disposable = scn.GetDSCard("tableChangeTrigger");
            var icePlains = scn.GetDSCard("icePlains");
            var blizzard2 = scn.GetDSCard("blizzard2");

            scn.StartGame();
            scn.MoveCardsToLocation(icePlains, blizzard2);
            scn.MoveCardsToDSHand(
                    target, classicTarget, cannon, disposable);

            for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
                assertTrue(analyzer.isRequiredCardForFlip(target));
                assertFalse(analyzer.isRequiredCardForFlip(
                        classicTarget));
                assertTrue(analyzer
                        .isRequiredOnTableCardPullRouteReady(
                                scn.game(), VirtualTableScenario.DS,
                                target));
                assertFalse(analyzer
                        .isRequiredOnTableCardPullRouteReady(
                                scn.game(), VirtualTableScenario.DS,
                                classicTarget));

                PolicyResult virtualChild =
                        PullSelectionCandidatePolicy
                                .scoreRequiredOnTableCard(
                                        "virtual-target",
                                        analyzer.isRequiredCardForFlip(
                                                target),
                                        analyzer
                                                .isRequiredOnTableCardPullRouteReady(
                                                        scn.game(),
                                                        VirtualTableScenario.DS,
                                                        target));
                PolicyResult classicChild =
                        PullSelectionCandidatePolicy
                                .scoreRequiredOnTableCard(
                                        "classic-target",
                                        analyzer.isRequiredCardForFlip(
                                                classicTarget),
                                        analyzer
                                                .isRequiredOnTableCardPullRouteReady(
                                                        scn.game(),
                                                        VirtualTableScenario.DS,
                                                        classicTarget));
                assertEquals(500.0f, operation(
                        virtualChild,
                        "PULL.OBJECTIVE.REQUIRED_ON_TABLE_CARD")
                        .delta(), 0.0f);
                assertTrue(classicChild.operations().isEmpty());
                assertEquals(0,
                        analyzer.getRequiredOnTableCardForceReserve(
                                scn.game(),
                                VirtualTableScenario.DS));

                assertTrue(analyzer
                        .isPreferredRequiredCardForceLossCandidate(
                                scn.game(),
                                VirtualTableScenario.DS,
                                target));
                assertTrue(analyzer
                        .isPreferredRequiredCardForceLossCandidate(
                                scn.game(),
                                VirtualTableScenario.DS,
                                cannon));
                assertFalse(analyzer
                        .isPreferredRequiredCardForceLossCandidate(
                                scn.game(),
                                VirtualTableScenario.DS,
                                classicTarget));
                assertFalse(analyzer
                        .isPreferredRequiredCardForceLossCandidate(
                                scn.game(),
                                VirtualTableScenario.DS,
                                disposable));
                assertFalse(analyzer
                        .isPreferredRequiredCardForceLossCandidate(
                                scn.game(),
                                VirtualTableScenario.DS,
                                classicCannon));

                assertEquals(-9999.0f,
                        operation(forceLossPolicy(
                                        scn, target, true),
                                "V21-objective").delta(),
                        0.0f);
                assertEquals(-9999.0f,
                        operation(forceLossPolicy(
                                        scn, cannon, true),
                                "V21-objective").delta(),
                        0.0f);
                assertFalse(hasOperation(
                        forceLossPolicy(
                                scn, classicTarget, false),
                        "V21-objective"));
                assertFalse(hasOperation(
                        forceLossPolicy(
                                scn, disposable, false),
                        "V21-objective"));
            }

            scn.MoveOutOfPlay(cannon);
            scn.MoveCardsToDSHand(classicCannon);
            for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
                assertEquals(1,
                        analyzer.getRequiredOnTableCardForceReserve(
                                scn.game(),
                                VirtualTableScenario.DS));
                assertTrue(analyzer
                        .isPreferredRequiredCardForceLossCandidate(
                                scn.game(),
                                VirtualTableScenario.DS,
                                classicCannon));
                assertEquals(-9999.0f,
                        operation(forceLossPolicy(
                                        scn, classicCannon, true),
                                "V21-objective").delta(),
                        0.0f);
            }
        }
    }

    @Test
    public void duplicateVirtualCannonsChooseOneDeterministicProgressCopy() {
        for (String objectiveBlueprintId : List.of("222_14", "222_30")) {
            VirtualTableScenario scn = scenario(objectiveBlueprintId);
            var cannon = scn.GetDSCard("cannon");
            var cannon2 = scn.GetDSCard("cannon2");
            var blizzard2 = scn.GetDSCard("blizzard2");

            scn.StartGame();
            scn.MoveCardsToDSHand(blizzard2);
            trimDarkReserveTo(
                    scn, cannon, cannon2);
            PhysicalCard preferred = cannon.getCardId()
                    < cannon2.getCardId() ? cannon : cannon2;
            PhysicalCard other = preferred == cannon
                    ? cannon2 : cannon;
            scn.MoveCardsToTopOfDSReserveDeck(
                    (PhysicalCardImpl) preferred,
                    (PhysicalCardImpl) other);
            List<PhysicalCard> reserveCandidates =
                    new ArrayList<>(
                            scn.gameState().getReserveDeck(
                                    VirtualTableScenario.DS));
            assertEquals(other, reserveCandidates.get(0));
            assertEquals(preferred, reserveCandidates.get(1));

            for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
                ObjectiveAnalyzer.ObjectiveProgressCandidateRole first =
                        analyzer.classifyPreFlipProgressCandidate(
                                scn.game(),
                                VirtualTableScenario.DS,
                                cannon);
                ObjectiveAnalyzer.ObjectiveProgressCandidateRole second =
                        analyzer.classifyPreFlipProgressCandidate(
                                scn.game(),
                                VirtualTableScenario.DS,
                                cannon2);
                long selected = List.of(first, second).stream()
                        .filter(role -> role
                                == ObjectiveAnalyzer
                                    .ObjectiveProgressCandidateRole
                                    .REQUIRED_ON_TABLE_CARD)
                        .count();

                assertEquals("Exactly one equal-cost Cannon copy must own objective progress",
                        1, selected);
                ObjectiveAnalyzer.ObjectiveProgressCandidateRole
                        preferredRole = preferred == cannon
                        ? first : second;
                assertEquals(
                        ObjectiveAnalyzer.ObjectiveProgressCandidateRole
                                .REQUIRED_ON_TABLE_CARD,
                        preferredRole);

                List<EvaluatedCandidate> adapterCandidates =
                        pullCandidateAdapter(
                                analyzer, scn,
                                cannon, cannon2);
                EvaluatedCandidate preferredCandidate =
                        evaluatedCandidate(
                                adapterCandidates, preferred);
                EvaluatedCandidate otherCandidate =
                        evaluatedCandidate(
                                adapterCandidates, other);
                assertTrue(preferredCandidate.reasoning(),
                        preferredCandidate.reasoning().contains(
                                "active table presence is required to flip"));
                assertFalse(otherCandidate.reasoning(),
                        otherCandidate.reasoning().contains(
                                "active table presence is required to flip"));
                assertEquals(500.0f,
                        preferredCandidate.score()
                                - otherCandidate.score(),
                        0.0f);

                List<EvaluatedCandidate> temporaryCandidates =
                        temporaryPullCandidateAdapter(
                                analyzer, scn,
                                reserveCandidates.toArray(
                                        new PhysicalCard[0]));
                EvaluatedCandidate temporaryPreferred =
                        evaluatedCandidate(
                                temporaryCandidates,
                                "temp" + reserveCandidates.indexOf(
                                        preferred));
                EvaluatedCandidate temporaryOther =
                        evaluatedCandidate(
                                temporaryCandidates,
                                "temp" + reserveCandidates.indexOf(
                                        other));
                assertTrue(temporaryPreferred.reasoning(),
                        temporaryPreferred.reasoning().contains(
                                "active table presence is required to flip"));
                assertFalse(temporaryOther.reasoning(),
                        temporaryOther.reasoning().contains(
                                "active table presence is required to flip"));
                assertEquals(500.0f,
                        temporaryPreferred.score()
                                - temporaryOther.score(),
                        0.0f);
            }
        }
    }

    @Test
    public void battleForfeitProtectsLiveAndSoleFutureRoutePackages() {
        for (String objectiveBlueprintId : List.of("222_14", "222_30")) {
            VirtualTableScenario scn = scenario(objectiveBlueprintId);
            var thirdMarker = scn.GetDSCard("thirdMarker");
            var target = scn.GetDSCard("target");
            var cannon = scn.GetDSCard("cannon");
            var blizzard2 = scn.GetDSCard("blizzard2");
            var unpilotedAtAt = scn.GetDSCard("unpilotedAtAt");

            scn.StartGame();
            scn.MoveLocationToTable(thirdMarker);
            scn.MoveCardsToLocation(thirdMarker, blizzard2);
            scn.AttachCardsTo(blizzard2, cannon);
            scn.AttachCardsTo(thirdMarker, target);

            for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
                for (PhysicalCard packageCard : List.of(
                        target, cannon, blizzard2)) {
                    ObjectiveAnalyzer.FlipGateFormationRole role =
                            analyzer.classifyGateFormationPieceIfRemoved(
                                    scn.game(),
                                    VirtualTableScenario.DS,
                                    packageCard);
                    assertEquals(
                            ObjectiveAnalyzer.FlipGateFormationRole
                                    .LAST_REQUIRED_ACTOR,
                            role);
                    PolicyResult avoidable =
                            BattleForfeitPolicy
                                    .scoreFlipGateFormationProtection(
                                            String.valueOf(
                                                    packageCard.getCardId()),
                                            role, true);
                    assertEquals(-9999.0f,
                            operation(
                                    avoidable,
                                    "BATTLE.OBJECTIVE.FLIP_GATE_FORMATION_HOLD")
                                    .delta(),
                            0.0f);
                    assertTrue(BattleForfeitPolicy
                            .scoreFlipGateFormationProtection(
                                    String.valueOf(
                                            packageCard.getCardId()),
                                    role, false)
                            .operations().isEmpty());
                }
            }

            scn.MoveCardsToLocation(thirdMarker, unpilotedAtAt);
            scn.AttachCardsTo(unpilotedAtAt, cannon);
            assertFalse(Filters.piloted.accepts(
                    scn.game(), unpilotedAtAt));
            for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
                assertEquals(
                        ObjectiveAnalyzer.FlipGateFormationRole
                                .LAST_REQUIRED_ACTOR,
                        analyzer.classifyGateFormationPieceIfRemoved(
                                scn.game(),
                                VirtualTableScenario.DS,
                                cannon));
                assertEquals(
                        ObjectiveAnalyzer.FlipGateFormationRole
                                .LAST_REQUIRED_ACTOR,
                        analyzer.classifyGateFormationPieceIfRemoved(
                                scn.game(),
                                VirtualTableScenario.DS,
                                unpilotedAtAt));
                assertFalse(analyzer
                        .advancesShieldMainGeneratorRoute(
                                scn.game(),
                                VirtualTableScenario.DS,
                                unpilotedAtAt,
                                scn.GetLSCard("secondMarker")));
            }
        }
    }

    @Test
    public void preEventStagingProtectsOnlyTheSelectedPackage() {
        for (String objectiveBlueprintId : List.of("222_14", "222_30")) {
            VirtualTableScenario scn = scenario(objectiveBlueprintId);
            var icePlains = scn.GetDSCard("icePlains");
            var northRidge = scn.GetDSCard("northRidge");
            var target = scn.GetDSCard("target");
            var blizzard2 = scn.GetDSCard("blizzard2");
            var cannon = scn.GetDSCard("cannon");
            var decoyHost = scn.GetDSCard("unpilotedAtAt");
            var decoyCannon = scn.GetDSCard("cannon2");
            var pilot = scn.GetDSCard("pilot");

            scn.StartGame();
            scn.MoveCardsToDSHand(target);
            scn.MoveCardsToLocation(icePlains, blizzard2);
            scn.AttachCardsTo(blizzard2, cannon);
            scn.MoveCardsToLocation(northRidge, decoyHost);
            scn.AttachCardsTo(
                    decoyHost, pilot, decoyCannon);

            for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
                for (PhysicalCard selected : List.of(
                        blizzard2, cannon)) {
                    ObjectiveAnalyzer.FlipGateFormationRole role =
                            analyzer
                                .classifyGateFormationPieceIfRemoved(
                                    scn.game(),
                                    VirtualTableScenario.DS,
                                    selected);
                    assertEquals(
                            ObjectiveAnalyzer
                                .FlipGateFormationRole
                                .LAST_REQUIRED_ACTOR,
                            role);
                    assertEquals(-9999.0f,
                            operation(BattleForfeitPolicy
                                    .scoreFlipGateFormationProtection(
                                            String.valueOf(
                                                selected.getCardId()),
                                            role, true),
                                    "BATTLE.OBJECTIVE.FLIP_GATE_FORMATION_HOLD")
                                .delta(),
                            0.0f);
                }
                assertEquals(
                        ObjectiveAnalyzer
                            .FlipGateFormationRole.NONE,
                        analyzer
                            .classifyGateFormationPieceIfRemoved(
                                scn.game(),
                                VirtualTableScenario.DS,
                                decoyHost));
                assertEquals(
                        ObjectiveAnalyzer
                            .FlipGateFormationRole.NONE,
                        analyzer
                            .classifyGateFormationPieceIfRemoved(
                                scn.game(),
                                VirtualTableScenario.DS,
                                decoyCannon));

                EvaluatedCandidate prematureMove =
                        evaluatedCandidate(
                            destinationAdapter(
                                analyzer, scn,
                                blizzard2, northRidge),
                            northRidge);
                assertTrue(prematureMove.reasoning(),
                        prematureMove.hardVeto());
                assertTrue(prematureMove.reasoning(),
                        prematureMove.reasoning().contains(
                            "MOVE.OBJECTIVE.COUNTED_FORMATION_HOLD"));
            }
        }
    }

    @Test
    public void partialStagingProtectsTheSelectedHostAndAttachedCannon() {
        for (String objectiveBlueprintId : List.of("222_14", "222_30")) {
            VirtualTableScenario scn = scenario(objectiveBlueprintId);
            var icePlains = scn.GetDSCard("icePlains");
            var target = scn.GetDSCard("target");
            var host = scn.GetDSCard("unpilotedAtAt");
            var pilot = scn.GetDSCard("pilot");
            var cannon = scn.GetDSCard("cannon");

            scn.StartGame();
            scn.MoveCardsToDSHand(
                    target, pilot, cannon);
            scn.MoveCardsToLocation(icePlains, host);

            for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
                assertEquals(
                        ObjectiveAnalyzer
                            .FlipGateFormationRole
                            .LAST_REQUIRED_ACTOR,
                        analyzer
                            .classifyGateFormationPieceIfRemoved(
                                scn.game(),
                                VirtualTableScenario.DS,
                                host));
            }

            scn.AttachCardsTo(host, cannon);
            for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
                assertEquals(
                        ObjectiveAnalyzer
                            .FlipGateFormationRole
                            .LAST_REQUIRED_ACTOR,
                        analyzer
                            .classifyGateFormationPieceIfRemoved(
                                scn.game(),
                                VirtualTableScenario.DS,
                                host));
                assertEquals(
                        ObjectiveAnalyzer
                            .FlipGateFormationRole
                            .LAST_REQUIRED_ACTOR,
                        analyzer
                            .classifyGateFormationPieceIfRemoved(
                                scn.game(),
                                VirtualTableScenario.DS,
                                cannon));
            }
        }
    }

    @Test
    public void splitPackageMovesOnlyTowardThePhysicalEvent() {
        for (String objectiveBlueprintId : List.of("222_14", "222_30")) {
            VirtualTableScenario scn = scenario(objectiveBlueprintId);
            var icePlains = scn.GetDSCard("icePlains");
            var northRidge = scn.GetDSCard("northRidge");
            var thirdMarker = scn.GetDSCard("thirdMarker");
            var target = scn.GetDSCard("target");
            var blizzard2 = scn.GetDSCard("blizzard2");
            var cannon = scn.GetDSCard("cannon");

            scn.StartGame();
            scn.MoveLocationToTable(thirdMarker);
            scn.AttachCardsTo(thirdMarker, target);
            scn.MoveCardsToLocation(northRidge, blizzard2);
            scn.AttachCardsTo(blizzard2, cannon);

            for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
                assertTrue(analyzer
                        .advancesShieldMainGeneratorRoute(
                            scn.game(),
                            VirtualTableScenario.DS,
                            blizzard2, thirdMarker));
                assertFalse(analyzer
                        .advancesShieldMainGeneratorRoute(
                            scn.game(),
                            VirtualTableScenario.DS,
                            blizzard2, icePlains));
                assertEquals(
                        ObjectiveAnalyzer
                            .FlipGateFormationRole
                            .LAST_REQUIRED_ACTOR,
                        analyzer
                            .classifyGateFormationPieceIfRemoved(
                                scn.game(),
                                VirtualTableScenario.DS,
                                blizzard2));
                assertEquals(
                        ObjectiveAnalyzer
                            .FlipGateFormationRole
                            .LAST_REQUIRED_ACTOR,
                        analyzer
                            .classifyGateFormationPieceIfRemoved(
                                scn.game(),
                                VirtualTableScenario.DS,
                                cannon));

                List<EvaluatedCandidate> destinations =
                        destinationAdapter(
                            analyzer, scn, blizzard2,
                            thirdMarker, icePlains);
                EvaluatedCandidate reunion =
                        evaluatedCandidate(
                            destinations, thirdMarker);
                EvaluatedCandidate reverse =
                        evaluatedCandidate(
                            destinations, icePlains);
                assertFalse(reunion.vetoReason(),
                        reunion.hardVeto());
                assertTrue(reunion.reasoning(),
                        reunion.reasoning().contains(
                            "MOVE.OBJECTIVE.ACTOR_ROUTE_DESTINATION"));
                assertTrue(reverse.reasoning(),
                        reverse.hardVeto());
            }
        }
    }

    @Test
    public void virtualEpicEventFollowsTheAtAtDownTheMarkerRoute() {
        for (String objectiveBlueprintId : List.of("222_14", "222_30")) {
            VirtualTableScenario scn = scenario(objectiveBlueprintId);
            var icePlains = scn.GetDSCard("icePlains");
            var northRidge = scn.GetDSCard("northRidge");
            var thirdMarker = scn.GetDSCard("thirdMarker");
            var secondMarker = scn.GetLSCard("secondMarker");
            var target = scn.GetDSCard("target");
            var blizzard2 = scn.GetDSCard("blizzard2");
            var cannon = scn.GetDSCard("cannon");

            scn.MoveCardsToDSHand(target);
            scn.StartGame();
            scn.MoveLocationToTable(secondMarker);
            scn.MoveLocationToTable(thirdMarker);
            scn.MoveCardsToLocation(icePlains, blizzard2);
            scn.AttachCardsTo(blizzard2, cannon);
            scn.DSActivateForceCheat(8);
            scn.SkipToPhase(Phase.DEPLOY);
            scn.DSDeployCardAndPassResponses(target, icePlains);
            assertEquals(icePlains, target.getAttachedTo());

            scn.SkipToPhase(Phase.MOVE);
            assertTrue(GameConditions.controls(
                    scn.game(), VirtualTableScenario.DS,
                    Filters.sameSite(target)));
            assertTrue(Filters.adjacentSite(target)
                    .accepts(scn.game(), northRidge));
            moveAndFollow(scn, blizzard2, northRidge, target);
            assertEquals(northRidge,
                    scn.game().getModifiersQuerying()
                            .getLocationThatCardIsAt(
                                    scn.gameState(), blizzard2));
            assertEquals(northRidge, target.getAttachedTo());

            scn.SkipToDSTurn(Phase.MOVE);
            assertTrue("The 4th Marker must use ordinary control before 222_13 may follow",
                    GameConditions.controls(
                            scn.game(), VirtualTableScenario.DS,
                            Filters.sameSite(target)));
            moveAndFollow(scn, blizzard2, thirdMarker, target);

            assertEquals(thirdMarker, target.getAttachedTo());
            assertEquals(thirdMarker,
                    scn.game().getModifiersQuerying()
                            .getLocationThatCardIsAt(
                                    scn.gameState(), blizzard2));
            for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
                assertFalse("3rd Marker starts the attempt but cannot put 222_3 in range of MPG",
                        onlyState(
                        analyzer, scn, "preFlip", "flip")
                        .conditionSatisfied());
            }

            scn.SkipToDSTurn(Phase.MOVE);
            assertTrue("The 3rd Marker must use ordinary control before the final follow",
                    GameConditions.controls(
                            scn.game(), VirtualTableScenario.DS,
                            Filters.sameSite(target)));
            moveAndFollow(scn, blizzard2, secondMarker, target);

            assertEquals(secondMarker, target.getAttachedTo());
            assertEquals(secondMarker,
                    scn.game().getModifiersQuerying()
                            .getLocationThatCardIsAt(
                                    scn.gameState(), blizzard2));
            for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
                assertTrue(onlyState(
                        analyzer, scn, "preFlip", "flip")
                        .conditionSatisfied());
            }
        }
    }

    @Test
    public void canonicalRouteStopsAtThirdAndNativelyFlips() {
        for (String objectiveBlueprintId : List.of("222_14", "222_30")) {
            VirtualTableScenario scn = scenario(objectiveBlueprintId);
            var objective = scn.GetDSCard("objective");
            var icePlains = scn.GetDSCard("icePlains");
            var northRidge = scn.GetDSCard("northRidge");
            var thirdMarker = scn.GetDSCard("thirdMarker");
            var target = scn.GetDSCard("target");
            var blizzard2 = scn.GetDSCard("blizzard2");
            var cannon = scn.GetDSCard("cannon");

            scn.MoveCardsToDSHand(target);
            scn.StartGame();
            scn.MoveLocationToTable(thirdMarker);
            scn.MoveCardsToLocation(icePlains, blizzard2);
            scn.AttachCardsTo(blizzard2, cannon);
            scn.DSActivateForceCheat(8);
            scn.SkipToPhase(Phase.DEPLOY);
            scn.DSDeployCardAndPassResponses(target, icePlains);

            scn.SkipToPhase(Phase.MOVE);
            moveAndFollow(scn, blizzard2, northRidge, target);
            scn.SkipToDSTurn(Phase.MOVE);
            assertTrue(GameConditions.controls(
                    scn.game(), VirtualTableScenario.DS,
                    Filters.sameSite(target)));
            moveAndFollow(scn, blizzard2, thirdMarker, target);

            for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
                assertTrue("Canonical DS topology is executable at 3rd Marker",
                        onlyState(
                                analyzer, scn,
                                "preFlip", "flip")
                                .conditionSatisfied());
            }
            scn.SkipToDSTurn(Phase.DEPLOY);
            scn.PrepareDSDestiny(6);
            assertTrue(scn.DSCardActionAvailable(target, ATTEMPT));
            fireAtMainPowerGenerators(scn);

            assertTrue(objectiveBlueprintId, objective.isFlipped());
            assertTrue(scn.GetDSCard("mainPowerGenerators")
                    .isBlownAway());
        }
    }

    @Test
    public void onlyTheCompletePackageGetsForwardRouteScoresAndReverseIsHeld() {
        for (String objectiveBlueprintId : List.of("222_14", "222_30")) {
            VirtualTableScenario scn = scenario(objectiveBlueprintId);
            var icePlains = scn.GetDSCard("icePlains");
            var northRidge = scn.GetDSCard("northRidge");
            var thirdMarker = scn.GetDSCard("thirdMarker");
            var target = scn.GetDSCard("target");
            var blizzard2 = scn.GetDSCard("blizzard2");
            var decoy = scn.GetDSCard("unpilotedAtAt");
            var cannon = scn.GetDSCard("cannon");

            scn.StartGame();
            scn.MoveLocationToTable(thirdMarker);
            scn.MoveCardsToLocation(northRidge, blizzard2, decoy);
            scn.AttachCardsTo(blizzard2, cannon);
            scn.AttachCardsTo(northRidge, target);

            assertTrue(Filters.piloted.accepts(scn.game(), blizzard2));
            assertFalse(Filters.piloted.accepts(scn.game(), decoy));
            for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
                assertTrue(analyzer.advancesShieldMainGeneratorRoute(
                        scn.game(), VirtualTableScenario.DS,
                        blizzard2, thirdMarker));
                assertFalse(analyzer.advancesShieldMainGeneratorRoute(
                        scn.game(), VirtualTableScenario.DS,
                        decoy, thirdMarker));
                assertTrue(analyzer.hasShieldMainGeneratorRouteDestination(
                        scn.game(), VirtualTableScenario.DS,
                        blizzard2));
                assertFalse(analyzer.hasShieldMainGeneratorRouteDestination(
                        scn.game(), VirtualTableScenario.DS,
                        decoy));

                MovementCandidate routeStart = movementCandidate(
                        movementAdapter(
                                analyzer, scn, blizzard2,
                                List.of("complete-package"),
                                List.of("Move using landspeed")),
                        "complete-package");
                MovementCandidate decoyStart = movementCandidate(
                        movementAdapter(
                                analyzer, scn, decoy,
                                List.of("decoy"),
                                List.of("Move using landspeed")),
                        "decoy");
                assertTrue(routeStart.reasoning().contains(
                        "MOVE.OBJECTIVE.ACTOR_ROUTE_START"));
                assertFalse(decoyStart.reasoning().contains(
                        "MOVE.OBJECTIVE.ACTOR_ROUTE_START"));

                List<EvaluatedCandidate> destinations =
                        destinationAdapter(
                                analyzer, scn, blizzard2,
                                thirdMarker, icePlains);
                EvaluatedCandidate forward =
                        evaluatedCandidate(
                                destinations, thirdMarker);
                EvaluatedCandidate reverse =
                        evaluatedCandidate(
                                destinations, icePlains);
                assertFalse(forward.hardVeto());
                assertTrue(forward.reasoning().contains(
                        "MOVE.OBJECTIVE.ACTOR_ROUTE_DESTINATION"));
                assertTrue(reverse.hardVeto());
                assertTrue(reverse.reasoning().contains(
                        "MOVE.OBJECTIVE.COUNTED_FORMATION_HOLD"));
            }
        }
    }

    @Test
    public void selectedPilotedHostMayStartForwardBeforeItsRetainedCannonAttaches() {
        for (String objectiveBlueprintId : List.of("222_14", "222_30")) {
            VirtualTableScenario scn = scenario(objectiveBlueprintId);
            var icePlains = scn.GetDSCard("icePlains");
            var northRidge = scn.GetDSCard("northRidge");
            var thirdMarker = scn.GetDSCard("thirdMarker");
            var target = scn.GetDSCard("target");
            var blizzard2 = scn.GetDSCard("blizzard2");
            var cannon = scn.GetDSCard("cannon");

            scn.StartGame();
            scn.MoveLocationToTable(thirdMarker);
            scn.MoveCardsToLocation(northRidge, blizzard2);
            scn.AttachCardsTo(northRidge, target);
            scn.MoveCardsToDSHand(cannon);
            scn.DSActivateForceCheat(2);

            assertTrue(Filters.piloted.accepts(
                    scn.game(), blizzard2));
            for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
                int moveCost = Math.max(
                        0,
                        (int) Math.ceil(
                            scn.game()
                                .getModifiersQuerying()
                                .getMoveUsingLandspeedCost(
                                    scn.gameState(),
                                    blizzard2,
                                    northRidge,
                                    thirdMarker,
                                    false, 0.0f)));
                assertEquals(0, moveCost);
                assertEquals(0,
                        analyzer
                            .getShieldMainGeneratorRouteMoveForceReserve(
                                scn.game(),
                                VirtualTableScenario.DS));
                assertEquals(0,
                        analyzer
                            .getRequiredOnTableCardForceReserve(
                                scn.game(),
                                VirtualTableScenario.DS,
                                cannon));
                assertTrue(analyzer
                        .isShieldMainGeneratorPriorityCannonDeploy(
                            scn.game(),
                            VirtualTableScenario.DS,
                            cannon));
                assertTrue(analyzer
                        .advancesShieldMainGeneratorRoute(
                            scn.game(),
                            VirtualTableScenario.DS,
                            blizzard2, thirdMarker));
                assertFalse(analyzer
                        .advancesShieldMainGeneratorRoute(
                            scn.game(),
                            VirtualTableScenario.DS,
                            blizzard2, icePlains));
                assertTrue(analyzer
                        .hasShieldMainGeneratorRouteDestination(
                            scn.game(),
                            VirtualTableScenario.DS,
                            blizzard2));

                MovementCandidate routeStart =
                        movementCandidate(
                            movementAdapter(
                                analyzer, scn, blizzard2,
                                List.of("prepared-package"),
                                List.of("Move using landspeed")),
                            "prepared-package");
                assertFalse(routeStart.hardVeto());
                assertFalse(routeStart.reasoning().contains(
                        "MOVE.OBJECTIVE.COUNTED_FORMATION_HOLD"));
            }
            scn.MoveCardsToTopOfDSReserveDeck(cannon);
            for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
                assertTrue(analyzer
                        .advancesShieldMainGeneratorRoute(
                            scn.game(),
                            VirtualTableScenario.DS,
                            blizzard2, thirdMarker));
                assertEquals(0, analyzer
                        .getShieldMainGeneratorRouteMoveForceReserve(
                            scn.game(),
                            VirtualTableScenario.DS));
            }
            for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
                assertForwardFollow(
                        analyzer, scn, target, blizzard2,
                        northRidge, thirdMarker);
            }
        }
    }

    @Test
    public void priorityCannonReleaseRequiresAnActuallyLegalDeploy() {
        for (String objectiveBlueprintId : List.of("222_14", "222_30")) {
            VirtualTableScenario scn =
                    scenario(objectiveBlueprintId);
            var northRidge =
                    scn.GetDSCard("northRidge");
            var target = scn.GetDSCard("target");
            var routeHost =
                    scn.GetDSCard("blizzard2");
            var virtualCannon =
                    scn.GetDSCard("cannon");
            var secondVirtualCannon =
                    scn.GetDSCard("cannon2");
            var classicCannon =
                    scn.GetDSCard("classicCannon");

            scn.StartGame();
            scn.MoveOutOfPlay(virtualCannon);
            scn.MoveOutOfPlay(secondVirtualCannon);
            scn.MoveCardsToLocation(
                    northRidge, routeHost);
            scn.AttachCardsTo(northRidge, target);
            scn.MoveCardsToDSHand(classicCannon);

            assertTrue(Filters.piloted.accepts(
                    scn.game(), routeHost));
            assertEquals(0, scn.GetDSForcePileCount());
            assertTrue(deployCostAt(
                    scn, classicCannon, routeHost) > 0);
            assertFalse(Filters.deployableToTarget(
                        classicCannon,
                        Filters.sameCardId(routeHost),
                        false, 0.0f)
                    .accepts(
                        scn.game(), classicCannon));

            for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
                assertFalse(analyzer
                        .isShieldMainGeneratorPriorityCannonDeploy(
                            scn.game(),
                            VirtualTableScenario.DS,
                            classicCannon));
            }
        }
    }

    @Test
    public void selectedWalkerMoveForceDominatesAnAffordableDistractorDeploy() {
        for (String objectiveBlueprintId : List.of("222_14", "222_30")) {
            VirtualTableScenario scn =
                    scenario(objectiveBlueprintId);
            var northRidge =
                    scn.GetDSCard("northRidge");
            var thirdMarker =
                    scn.GetDSCard("thirdMarker");
            var secondMarker =
                    scn.GetLSCard("secondMarker");
            var target = scn.GetDSCard("target");
            var routeHost =
                    scn.GetDSCard("blizzard2");
            var cannon = scn.GetDSCard("cannon");
            var distractor = scn.GetDSCard("pilot");

            scn.StartGame();
            scn.MoveLocationToTable(thirdMarker);
            scn.MoveLocationToTable(secondMarker);
            scn.MoveCardsToLocation(
                    thirdMarker, routeHost);
            scn.AttachCardsTo(routeHost, cannon);
            scn.AttachCardsTo(thirdMarker, target);
            scn.MoveCardsToDSHand(distractor);
            int deployCost = Math.max(
                    1,
                    distractor.getBlueprint()
                        .getDeployCost().intValue());
            scn.DSActivateForceCheat(deployCost);

            for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
                assertTrue(analyzer
                        .getShieldMainGeneratorRouteMoveForceReserve(
                            scn.game(),
                            VirtualTableScenario.DS) > 0);
                assertEquals("The dedicated movement gate owns this reserve exactly once",
                        0,
                        analyzer.getRequiredOnTableCardForceReserve(
                                scn.game(),
                                VirtualTableScenario.DS,
                                distractor));
                EvaluatedCandidate deploy =
                        evaluatedCandidate(
                            deployActionAdapter(
                                analyzer, scn,
                                distractor,
                                List.of("distractor"),
                                List.of("Deploy")),
                            "distractor");
                assertTrue(deploy.hardVeto());
                assertTrue(deploy.vetoReason(),
                        deploy.vetoReason().contains(
                            "HOTH.SHIELD.MOVE_FORCE_RESERVE"));

                EvaluatedCandidate winner =
                        combinedActionAdapter(
                            analyzer, scn,
                            distractor,
                            List.of("distractor", "pass"),
                            List.of("Deploy", "Pass"));
                assertEquals("pass", winner.actionId());
            }
        }
    }

    @Test
    public void classicRangefinderStopsTheRouteAtTheFirstActuallyInRangeMarker() {
        for (String objectiveBlueprintId : List.of("222_14", "222_30")) {
            VirtualTableScenario scn = scenario(objectiveBlueprintId);
            var thirdMarker = scn.GetDSCard("thirdMarker");
            var secondMarker = scn.GetLSCard("secondMarker");
            var mainPowerGenerators =
                    scn.GetDSCard("mainPowerGenerators");
            var target = scn.GetDSCard("target");
            var routeHost = scn.GetDSCard("blizzard2");
            var classicCannon = scn.GetDSCard("classicCannon");
            var classicRangefinder =
                    scn.GetDSCard("classicRangefinder");

            scn.StartGame();
            scn.MoveLocationToTable(secondMarker);
            scn.MoveLocationToTable(thirdMarker);
            scn.MoveCardsToLocation(thirdMarker, routeHost);
            scn.AttachCardsTo(routeHost, classicCannon);
            scn.AttachCardsTo(thirdMarker, target);

            assertEquals(Integer.valueOf(2),
                    scn.game().getModifiersQuerying()
                            .getDistanceBetweenSites(
                                    scn.gameState(), thirdMarker,
                                    mainPowerGenerators));
            assertFalse(Filters.canBeFiredAtLocationInRange(
                            mainPowerGenerators)
                    .accepts(scn.game(), classicCannon));
            for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
                assertTrue(analyzer.advancesShieldMainGeneratorRoute(
                        scn.game(), VirtualTableScenario.DS,
                        routeHost, secondMarker));
            }

            scn.AttachCardsTo(classicCannon, classicRangefinder);
            assertTrue(Filters.canBeFiredAtLocationInRange(
                            mainPowerGenerators)
                    .accepts(scn.game(), classicCannon));
            for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
                assertFalse("Do not walk past an actual legal firing site",
                        analyzer.advancesShieldMainGeneratorRoute(
                                scn.game(), VirtualTableScenario.DS,
                                routeHost, secondMarker));
            }
        }
    }

    @Test
    public void sparePilotedWalkerHoldsOnlyWhenMeaningfullySupported() {
        for (String objectiveBlueprintId : List.of("222_14", "222_30")) {
            VirtualTableScenario scn = scenario(objectiveBlueprintId, true);
            var icePlains = scn.GetDSCard("icePlains");
            var northRidge = scn.GetDSCard("northRidge");
            var carrier = scn.GetDSCard("blizzard2");
            var spare = scn.GetDSCard("blizzard2Duplicate");
            var cannon = scn.GetDSCard("cannon");
            var support = scn.GetDSCard("pilot");

            scn.StartGame();
            scn.MoveCardsToLocation(northRidge, carrier);
            scn.AttachCardsTo(carrier, cannon);
            scn.MoveCardsToLocation(icePlains, spare);

            assertTrue(Filters.piloted.accepts(scn.game(), spare));
            for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
                assertFalse(analyzer.shouldHoldSecondaryShieldMarkerWalker(
                        scn.game(), VirtualTableScenario.DS, spare));
                assertFalse(analyzer.shouldHoldSecondaryShieldMarkerWalker(
                        scn.game(), VirtualTableScenario.DS, carrier));
            }
            scn.MoveCardsToLocation(icePlains, support);
            for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
                assertTrue(analyzer.shouldHoldSecondaryShieldMarkerWalker(
                        scn.game(), VirtualTableScenario.DS, spare));
            }
        }
    }

    @Test
    public void bothMoveAdaptersScoreEveryExtendedForwardHopOnly() {
        for (String objectiveBlueprintId : List.of("222_14", "222_30")) {
            VirtualTableScenario scn = scenario(objectiveBlueprintId);
            var icePlains = scn.GetDSCard("icePlains");
            var northRidge = scn.GetDSCard("northRidge");
            var thirdMarker = scn.GetDSCard("thirdMarker");
            var secondMarker = scn.GetLSCard("secondMarker");
            var mainPowerGenerators =
                    scn.GetDSCard("mainPowerGenerators");
            var target = scn.GetDSCard("target");
            var blizzard2 = scn.GetDSCard("blizzard2");
            var cannon = scn.GetDSCard("cannon");

            scn.StartGame();
            scn.MoveLocationToTable(secondMarker);
            scn.MoveLocationToTable(thirdMarker);
            scn.DSActivateForceCheat(8);
            scn.AttachCardsTo(blizzard2, cannon);
            List<PhysicalCard> origins = List.of(
                    icePlains, northRidge, thirdMarker);
            List<PhysicalCard> forward = List.of(
                    northRidge, thirdMarker, secondMarker);
            List<PhysicalCard> nonForward = List.of(
                    thirdMarker, icePlains, northRidge);

            for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
                for (int index = 0;
                        index < origins.size(); index++) {
                    PhysicalCard origin = origins.get(index);
                    PhysicalCard destination = forward.get(index);
                    PhysicalCard rejected = nonForward.get(index);
                    scn.MoveCardsToLocation(
                            (PhysicalCardImpl) origin,
                            blizzard2);
                    scn.AttachCardsTo(
                            (PhysicalCardImpl) origin,
                            target);
                    scn.AttachCardsTo(blizzard2, cannon);

                    assertTrue(analyzer
                            .advancesShieldMainGeneratorRoute(
                                    scn.game(),
                                    VirtualTableScenario.DS,
                                    blizzard2,
                                    destination));
                    assertFalse(analyzer
                            .advancesShieldMainGeneratorRoute(
                                    scn.game(),
                                    VirtualTableScenario.DS,
                                    blizzard2,
                                    rejected));

                    MovementCandidate start =
                            movementCandidate(
                                    movementAdapter(
                                            analyzer, scn,
                                            blizzard2,
                                            List.of("forward-hop"),
                                            List.of(
                                                "Move using landspeed")),
                                    "forward-hop");
                    assertTrue(
                            analyzer.getClass().getName()
                                    + " should score "
                                    + origin.getTitle() + " -> "
                                    + destination.getTitle()
                                    + "; reasoning="
                                    + start.reasoning(),
                            start.reasoning().contains(
                                    "MOVE.OBJECTIVE.ACTOR_ROUTE_START"));

                    List<EvaluatedCandidate> candidates =
                            destinationAdapter(
                                    analyzer, scn, blizzard2,
                                    destination, rejected);
                    EvaluatedCandidate forwardHop =
                            evaluatedCandidate(
                                    candidates, destination);
                    EvaluatedCandidate rejectedHop =
                            evaluatedCandidate(
                                    candidates, rejected);
                    assertFalse(forwardHop.hardVeto());
                    assertTrue(forwardHop.reasoning().contains(
                            "MOVE.OBJECTIVE.ACTOR_ROUTE_DESTINATION"));
                    assertFalse(rejectedHop.reasoning().contains(
                            "MOVE.OBJECTIVE.ACTOR_ROUTE_DESTINATION"));
                    assertTrue(rejectedHop.hardVeto());
                    assertTrue(rejectedHop.reasoning().contains(
                            "MOVE.OBJECTIVE.COUNTED_FORMATION_HOLD"));
                }

                scn.MoveCardsToLocation(secondMarker, blizzard2);
                scn.AttachCardsTo(secondMarker, target);
                scn.AttachCardsTo(blizzard2, cannon);
                assertFalse("Once the Cannon is in range, do not route into MPG",
                        analyzer.advancesShieldMainGeneratorRoute(
                                scn.game(),
                                VirtualTableScenario.DS,
                                blizzard2,
                                mainPowerGenerators));
            }
        }
    }

    @Test
    public void liveAdjacencyAllowsFourthToSecondWhenThirdIsAbsent() {
        for (String objectiveBlueprintId : List.of("222_14", "222_30")) {
            VirtualTableScenario scn = scenario(objectiveBlueprintId);
            var icePlains = scn.GetDSCard("icePlains");
            var northRidge = scn.GetDSCard("northRidge");
            var secondMarker = scn.GetLSCard("secondMarker");
            var target = scn.GetDSCard("target");
            var blizzard2 = scn.GetDSCard("blizzard2");
            var cannon = scn.GetDSCard("cannon");

            scn.StartGame();
            scn.MoveLocationToTable(secondMarker);
            scn.DSActivateForceCheat(8);
            assertTrue("With no 3rd Marker deployed, 4th and 2nd are adjacent",
                    scn.game().getModifiersQuerying().isAdjacentSites(
                            scn.gameState(), northRidge, secondMarker));

            for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
                scn.MoveCardsToLocation(northRidge, blizzard2);
                scn.AttachCardsTo(blizzard2, cannon);
                scn.AttachCardsTo(northRidge, target);

                assertTrue(analyzer.advancesShieldMainGeneratorRoute(
                        scn.game(), VirtualTableScenario.DS,
                        blizzard2, secondMarker));
                assertFalse(analyzer.advancesShieldMainGeneratorRoute(
                        scn.game(), VirtualTableScenario.DS,
                        blizzard2, icePlains));

                MovementCandidate routeStart = movementCandidate(
                        movementAdapter(
                                analyzer, scn, blizzard2,
                                List.of("live-adjacent-route"),
                                List.of("Move using landspeed")),
                        "live-adjacent-route");
                assertTrue(routeStart.reasoning().contains(
                        "MOVE.OBJECTIVE.ACTOR_ROUTE_START"));

                List<EvaluatedCandidate> destinations =
                        destinationAdapter(
                                analyzer, scn, blizzard2,
                                secondMarker, icePlains);
                EvaluatedCandidate forward =
                        evaluatedCandidate(destinations, secondMarker);
                EvaluatedCandidate reverse =
                        evaluatedCandidate(destinations, icePlains);
                assertFalse(forward.hardVeto());
                assertTrue(forward.reasoning().contains(
                        "MOVE.OBJECTIVE.ACTOR_ROUTE_DESTINATION"));
                assertTrue(reverse.hardVeto());
                assertFalse(reverse.reasoning().contains(
                        "MOVE.OBJECTIVE.ACTOR_ROUTE_DESTINATION"));

                assertForwardFollow(
                        analyzer, scn, target, blizzard2,
                        northRidge, secondMarker);
            }
        }
    }

    @Test
    public void shieldControlContestUsesOrdinaryControlThroughBattlePolicy() {
        for (String objectiveBlueprintId : List.of("222_14", "222_30")) {
            VirtualTableScenario scn = scenario(objectiveBlueprintId);
            var northRidge = scn.GetDSCard("northRidge");
            var thirdMarker = scn.GetDSCard("thirdMarker");
            var target = scn.GetDSCard("target");

            scn.StartGame();
            scn.MoveLocationToTable(thirdMarker);
            scn.AttachCardsTo(northRidge, target);
            assertFalse(GameConditions.controls(
                    scn.game(), VirtualTableScenario.DS,
                    Filters.sameCardId(northRidge)));
            for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
                assertTrue(analyzer
                        .preFlipRequirementUsesOrdinaryControl(
                                scn.game(),
                                VirtualTableScenario.DS,
                                northRidge));
                assertTrue(analyzer.isMissingPreFlipRequirementAt(
                        scn.game(), VirtualTableScenario.DS,
                        northRidge));
            }
        }

        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        PhysicalCard location = mock(PhysicalCard.class);
        ObjectiveAnalyzer analyzer = mock(ObjectiveAnalyzer.class);
        Logger logger = mock(Logger.class);
        String playerId = "dark";
        String opponentId = "light";

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(location.getCardId()).thenReturn(700);
        when(location.getTitle()).thenReturn(
                "Hoth: North Ridge (4th Marker)");
        when(gameState.getTopLocations()).thenReturn(List.of(location));
        when(gameState.getOpponent(playerId)).thenReturn(opponentId);
        when(gameState.getPlayerLifeForce(opponentId)).thenReturn(20);
        when(gameState.getCardsAtLocation(location))
                .thenReturn(List.of());
        when(gameState.getHand(playerId)).thenReturn(List.of());
        when(modifiers.getTotalPowerAtLocation(
                gameState, location, playerId,
                false, false)).thenReturn(8.0f);
        when(modifiers.getTotalPowerAtLocation(
                gameState, location, opponentId,
                false, false)).thenReturn(8.0f);
        when(modifiers.getTotalAbilityAtLocation(
                gameState, playerId, location)).thenReturn(4.0f);
        when(modifiers.getTotalAbilityAtLocation(
                gameState, opponentId, location)).thenReturn(4.0f);
        when(modifiers.controlsLocation(
                gameState, location, playerId)).thenReturn(false);
        when(modifiers.controlsLocation(
                gameState, location, playerId,
                SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE))
                .thenReturn(true);
        when(analyzer.isMissingPreFlipRequirementAt(
                game, playerId, location)).thenReturn(true);
        when(analyzer.preFlipRequirementUsesOrdinaryControl(
                game, playerId, location)).thenReturn(true);

        List<BattleDecisionPolicy.ScoredAction> actions =
                BattleDecisionPolicy.evaluate(
                        battleContext(
                                gameState, game, analyzer,
                                logger, playerId, location));
        BattleDecisionPolicy.ScoredAction battle =
                actions.stream().filter(action ->
                        "contest-third".equals(
                                action.actionId()))
                        .findFirst().orElseThrow();
        assertTrue("Override-only power must not close Shield's ordinary-control contest",
                battle.contributions().stream()
                        .anyMatch(contribution ->
                                contribution.ruleArmId() != null
                                && ObjectiveBattlePolicy
                                    .REQUIRED_LOCATION_CONTEST_RULE_ID
                                    .equals(
                                        contribution
                                            .ruleArmId().id())
                                && contribution.delta()
                                    == ObjectiveBattlePolicy
                                        .REQUIRED_LOCATION_CONTEST_BONUS));
        verify(modifiers).controlsLocation(
                gameState, location, playerId);
        verify(modifiers, never()).controlsLocation(
                gameState, location, playerId,
                SpotOverride.INCLUDE_EXCLUDED_FROM_BATTLE);
    }

    @Test
    public void v160ScoresOnlyForwardFollowsAndExecutableSecondMarkerFire() {
        for (String objectiveBlueprintId : List.of("222_14", "222_30")) {
            VirtualTableScenario scn = scenario(objectiveBlueprintId);
            var icePlains = scn.GetDSCard("icePlains");
            var northRidge = scn.GetDSCard("northRidge");
            var thirdMarker = scn.GetDSCard("thirdMarker");
            var secondMarker = scn.GetLSCard("secondMarker");
            var target = scn.GetDSCard("target");
            var blizzard2 = scn.GetDSCard("blizzard2");
            var decoy = scn.GetDSCard("unpilotedAtAt");
            var cannon = scn.GetDSCard("cannon");

            scn.StartGame();
            scn.MoveLocationToTable(thirdMarker);
            scn.MoveLocationToTable(secondMarker);
            scn.AttachCardsTo(blizzard2, cannon);

            for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
                assertForwardFollow(
                        analyzer, scn, target, blizzard2,
                        icePlains, northRidge);
                assertForwardFollow(
                        analyzer, scn, target, blizzard2,
                        northRidge, thirdMarker);
                assertForwardFollow(
                        analyzer, scn, target, blizzard2,
                        thirdMarker, secondMarker);

                positionFollowFixture(
                        scn, target, blizzard2,
                        secondMarker, thirdMarker);
                assertNoV160RoutePush(
                        analyzer, scn, target,
                        "reverse-follow",
                        "Follow vehicle moving from same site");

                positionFollowFixture(
                        scn, target, decoy,
                        thirdMarker, secondMarker);
                assertNoV160RoutePush(
                        analyzer, scn, target,
                        "decoy-follow",
                        "Follow vehicle moving from same site");

                scn.MoveCardsToLocation(secondMarker, blizzard2);
                scn.AttachCardsTo(blizzard2, cannon);
                scn.AttachCardsTo(secondMarker, target);
                List<EvaluatedCandidate> fireCandidates =
                        actionTextAdapter(
                                analyzer, scn, target,
                                List.of("wait", "second-marker-fire"),
                                List.of("Take another action", ATTEMPT));
                EvaluatedCandidate wait =
                        evaluatedCandidate(
                                fireCandidates, "wait");
                EvaluatedCandidate fire =
                        evaluatedCandidate(
                                fireCandidates,
                                "second-marker-fire");
                assertEquals(800.0f,
                        fire.score() - wait.score(), 0.0f);
                assertTrue(fire.reasoning().contains(
                        "V160 PUSH TARGET THE MAIN GENERATOR"));
            }
        }
    }

    @Test
    public void pilotedCompetingWalkerCannotBorrowTheSelectedCannonFollow() {
        for (String objectiveBlueprintId : List.of("222_14", "222_30")) {
            VirtualTableScenario scn =
                    scenario(objectiveBlueprintId, true);
            var thirdMarker = scn.GetDSCard("thirdMarker");
            var secondMarker = scn.GetLSCard("secondMarker");
            var target = scn.GetDSCard("target");
            var firstWalker = scn.GetDSCard("blizzard2");
            var secondWalker =
                    scn.GetDSCard("blizzard2Duplicate");
            var cannon = scn.GetDSCard("cannon");
            var secondCannon = scn.GetDSCard("cannon2");
            var classicCannon =
                    scn.GetDSCard("classicCannon");

            scn.StartGame();
            scn.MoveLocationToTable(thirdMarker);
            scn.MoveLocationToTable(secondMarker);
            scn.MoveOutOfPlay(secondCannon);
            scn.MoveOutOfPlay(classicCannon);
            scn.MoveCardsToLocation(
                    thirdMarker, firstWalker, secondWalker);
            scn.AttachCardsTo(thirdMarker, target);
            scn.MoveCardsToDSHand(cannon);

            assertTrue(Filters.piloted.accepts(
                    scn.game(), firstWalker));
            assertTrue(Filters.piloted.accepts(
                    scn.game(), secondWalker));
            List<ObjectiveAnalyzer> analyzers = analyzers(scn);
            PhysicalCard selected = firstWalker.getCardId()
                    < secondWalker.getCardId()
                    ? firstWalker : secondWalker;
            PhysicalCard decoy = selected == firstWalker
                    ? secondWalker : firstWalker;
            for (ObjectiveAnalyzer analyzer : analyzers) {
                assertTrue(analyzer.advancesShieldMainGeneratorRoute(
                        scn.game(), VirtualTableScenario.DS,
                        selected, secondMarker));
                assertFalse(analyzer.advancesShieldMainGeneratorRoute(
                        scn.game(), VirtualTableScenario.DS,
                        decoy, secondMarker));
            }

            positionFollowFixture(
                    scn, target, decoy,
                    thirdMarker, secondMarker);
            for (ObjectiveAnalyzer analyzer : analyzers) {
                assertNoV160RoutePush(
                        analyzer, scn, target,
                        "piloted-decoy-follow",
                        "Follow vehicle moving from same site");
            }
        }
    }

    @Test
    public void nineBlowsGeneratorsAndNativelyFlipsBothObjectivePrintings() {
        for (String objectiveBlueprintId : List.of("222_14", "222_30")) {
            VirtualTableScenario scn = readyToFire(
                    objectiveBlueprintId, 6);
            var objective = scn.GetDSCard("objective");
            var mainPowerGenerators =
                    scn.GetDSCard("mainPowerGenerators");
            var target = scn.GetDSCard("target");
            int lightLifeBefore = scn.GetLSLifeForceRemaining();
            int lightLostBefore = scn.GetLSLostPileCount();

            assertTrue(objectiveBlueprintId,
                    scn.DSCardActionAvailable(target, ATTEMPT));
            fireAtMainPowerGenerators(scn);

            assertTrue("Destiny 6 + controlled marker 2 + cannon 1 is 9",
                    mainPowerGenerators.isBlownAway());
            assertEquals("MPG must make Light Side lose exactly 5 Force",
                    5, lightLifeBefore - scn.GetLSLifeForceRemaining());
            assertEquals("The automatic Reserve payment must lose 5 cards",
                    5, scn.GetLSLostPileCount() - lightLostBefore);
            assertTrue(objectiveBlueprintId, objective.isFlipped());
            assertInZone(Zone.SIDE_OF_TABLE, objective);
        }
    }

    @Test
    public void pilotedAtAtKeepsBackSideAliveButUnpilotedAtAtDoesNot() {
        for (String objectiveBlueprintId : List.of("222_14", "222_30")) {
            VirtualTableScenario scn = readyToFire(
                    objectiveBlueprintId, 6);
            var objective = scn.GetDSCard("objective");
            var target = scn.GetDSCard("target");
            var blizzard2 = scn.GetDSCard("blizzard2");
            var unpilotedAtAt = scn.GetDSCard("unpilotedAtAt");
            var tableChangeTrigger =
                    scn.GetDSCard("tableChangeTrigger");

            fireAtMainPowerGenerators(scn);

            assertTrue(objectiveBlueprintId, objective.isFlipped());
            assertInZone(Zone.SIDE_OF_TABLE, objective);

            scn.MoveCardsToDSHand(tableChangeTrigger);
            scn.DSActivateForceCheat(2);
            scn.SkipToDSTurn(Phase.DEPLOY);

            List<ObjectiveAnalyzer> analyzers = analyzers(scn);
            for (ObjectiveAnalyzer analyzer : analyzers) {
                assertTrue(onlyState(
                        analyzer, scn, "postFlip", "stayFlipped")
                        .conditionSatisfied());
                assertTrue(analyzer.assessFlipLocationRules(
                        scn.game(), VirtualTableScenario.DS,
                        "postFlip", "flipBack").isEmpty());
            }

            scn.MoveCardsToLocation(
                    scn.GetLSCard("secondMarker"), unpilotedAtAt);
            assertFalse(Filters.piloted.accepts(
                    scn.game(), unpilotedAtAt));
            scn.MoveOutOfPlay(blizzard2);
            for (ObjectiveAnalyzer analyzer : analyzers) {
                assertFalse(onlyState(
                        analyzer, scn, "postFlip", "stayFlipped")
                        .conditionSatisfied());
            }
            if (scn.AwaitingLSDeployPhaseActions()) {
                scn.LSPass();
            }
            assertTrue(decisionText(scn),
                    scn.AwaitingDSDeployPhaseActions());
            assertTrue(scn.DSCardPlayAvailable(tableChangeTrigger));
            scn.DSPlayCardAndPassResponses(tableChangeTrigger);

            assertInZone(Zone.OUT_OF_PLAY, objective);
        }
    }

    @Test
    public void postFlipForceLossDoesNotProtectTheSpentEventRoute() {
        for (String objectiveBlueprintId : List.of("222_14", "222_30")) {
            VirtualTableScenario scn = readyToFire(
                    objectiveBlueprintId, 6);
            var objective = scn.GetDSCard("objective");
            var target = scn.GetDSCard("target");
            var cannon = scn.GetDSCard("cannon");
            var prepare = scn.GetDSCard("prepare");
            var disposable =
                    scn.GetDSCard("tableChangeTrigger");

            fireAtMainPowerGenerators(scn);
            assertTrue(objective.isFlipped());
            scn.MoveCardsToDSHand(
                    target, cannon, prepare,
                    disposable);

            for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
                assertFalse(analyzer
                        .isPreferredRequiredCardForceLossCandidate(
                                scn.game(),
                                VirtualTableScenario.DS,
                                target));
                assertFalse(analyzer
                        .isPreferredRequiredCardForceLossCandidate(
                                scn.game(),
                                VirtualTableScenario.DS,
                                cannon));
                assertFalse(analyzer.isPullableCard(prepare));
                for (EvaluatedCandidate candidate
                        : forceLossAdapter(
                            analyzer, scn,
                            target, cannon, prepare,
                            disposable)) {
                    assertFalse(candidate.reasoning(),
                            candidate.reasoning().contains(
                                "OBJECTIVE CRITICAL IN HAND - NEVER LOSE"));
                    assertFalse(candidate.reasoning(),
                            candidate.reasoning().contains(
                                "OBJECTIVE PULLABLE IN HAND - NEVER LOSE"));
                }
            }
        }
    }

    @Test
    public void lastPostFlipAtAtMayRelocateOnlyToAnotherHothSite() {
        for (String objectiveBlueprintId : List.of("222_14", "222_30")) {
            VirtualTableScenario scn = readyToFire(
                    objectiveBlueprintId, 6);
            var objective = scn.GetDSCard("objective");
            var secondMarker = scn.GetLSCard("secondMarker");
            var thirdMarker = scn.GetDSCard("thirdMarker");
            var offHothSite = scn.GetDSCard("offHothSite");
            var target = scn.GetDSCard("target");
            var blizzard2 = scn.GetDSCard("blizzard2");

            fireAtMainPowerGenerators(scn);
            assertTrue(objectiveBlueprintId, objective.isFlipped());
            scn.MoveLocationToTable(thirdMarker);
            scn.MoveLocationToTable(offHothSite);
            scn.SkipToPhase(Phase.MOVE);

            String safeText = "Move from "
                    + secondMarker.getTitle() + " to "
                    + thirdMarker.getTitle();
            String unsafeText = "Move from "
                    + secondMarker.getTitle() + " to "
                    + offHothSite.getTitle();
            for (ObjectiveAnalyzer analyzer : analyzers(scn)) {
                assertTrue(analyzer
                        .wouldDepartureTriggerStayFlippedFailure(
                                scn.game(), VirtualTableScenario.DS,
                                blizzard2));
                assertTrue(analyzer
                        .preservesStayFlippedRequirementByMovingTo(
                                scn.game(), VirtualTableScenario.DS,
                                blizzard2, thirdMarker));
                assertFalse(analyzer
                        .preservesStayFlippedRequirementByMovingTo(
                                scn.game(), VirtualTableScenario.DS,
                                blizzard2, offHothSite));

                MoveObjectiveGateHoldPolicy.Evaluation safe =
                        MoveObjectiveGateHoldPolicy
                                .evaluatePostFlipSurvivalActor(
                                        true, 10.0f, 0.0f,
                                        true);
                MoveObjectiveGateHoldPolicy.Evaluation unsafe =
                        MoveObjectiveGateHoldPolicy
                                .evaluatePostFlipSurvivalActor(
                                        true, 10.0f, 0.0f,
                                        false);
                assertFalse(safe.hardVeto());
                assertTrue(unsafe.hardVeto());
                assertEquals(
                        MoveObjectiveGateHoldPolicy.Branch
                                .HOLD_POST_FLIP_SURVIVAL_ACTOR,
                        unsafe.branch());

                List<MovementCandidate> candidates =
                        movementAdapter(
                                analyzer, scn, blizzard2,
                                List.of("safe-hoth", "unsafe-off-hoth"),
                                List.of(safeText, unsafeText));
                MovementCandidate safeMove = movementCandidate(
                        candidates, "safe-hoth");
                MovementCandidate unsafeMove = movementCandidate(
                        candidates, "unsafe-off-hoth");
                assertFalse(safeMove.reasoning().contains(
                        "MOVE.OBJECTIVE.POST_FLIP_SURVIVAL_HOLD"));
                assertTrue(unsafeMove.reasoning().contains(
                        "MOVE.OBJECTIVE.POST_FLIP_SURVIVAL_HOLD"));
                assertTrue(unsafeMove.score()
                        < safeMove.score() - 50000.0f);

                List<EvaluatedCandidate> destinations =
                        destinationAdapter(
                                analyzer, scn, blizzard2,
                                thirdMarker, offHothSite);
                EvaluatedCandidate safeDestination =
                        evaluatedCandidate(
                                destinations, thirdMarker);
                EvaluatedCandidate unsafeDestination =
                        evaluatedCandidate(
                                destinations, offHothSite);
                assertFalse(safeDestination.hardVeto());
                assertTrue(unsafeDestination.hardVeto());
                assertTrue(unsafeDestination.vetoReason().startsWith(
                        "MOVE.OBJECTIVE.POST_FLIP_SURVIVAL_HOLD"));
                assertTrue(unsafeDestination.reasoning().contains(
                        "MOVE.OBJECTIVE.POST_FLIP_SURVIVAL_HOLD"));
            }

            moveAndFollow(scn, blizzard2, thirdMarker, target);
            assertEquals(thirdMarker,
                    scn.game().getModifiersQuerying()
                            .getLocationThatCardIsAt(
                                    scn.gameState(), blizzard2));
            assertEquals(thirdMarker, target.getAttachedTo());
            assertTrue(objectiveBlueprintId, objective.isFlipped());
            assertInZone(Zone.SIDE_OF_TABLE, objective);
        }
    }

    private VirtualTableScenario readyToFire(
            String objectiveBlueprintId, int destiny) {
        VirtualTableScenario scn = scenario(objectiveBlueprintId);
        var secondMarker = scn.GetLSCard("secondMarker");
        var target = scn.GetDSCard("target");
        var blizzard2 = scn.GetDSCard("blizzard2");
        var cannon = scn.GetDSCard("cannon");

        scn.StartGame();
        assertInZone(Zone.LOCATIONS,
                scn.GetDSCard("icePlains"),
                scn.GetDSCard("northRidge"),
                scn.GetDSCard("mainPowerGenerators"));
        assertInZone(Zone.SIDE_OF_TABLE, scn.GetDSCard("prepare"));

        scn.MoveLocationToTable(secondMarker);
        scn.MoveCardsToLocation(secondMarker, blizzard2);
        scn.AttachCardsTo(blizzard2, cannon);
        scn.AttachCardsTo(secondMarker, target);
        assertTrue(GameConditions.isAtLocation(
                scn.game(), target, Filters.Second_Marker));
        assertTrue("Blizzard 2 must be piloted by its permanent pilot",
                Filters.piloted.accepts(scn.game(), blizzard2));
        assertTrue("The cannon must be here with the virtual Epic Event",
                Filters.here(target).accepts(scn.game(), cannon));
        assertTrue("The virtual cannon must reach MPG from 2nd Marker",
                Filters.canBeFiredAtLocationInRange(
                        scn.GetDSCard("mainPowerGenerators"))
                        .accepts(scn.game(), cannon));
        scn.SkipToPhase(Phase.DEPLOY);
        scn.PrepareDSDestiny(destiny);

        assertEquals("The virtual path must fire from the 2nd Marker",
                secondMarker, target.getAttachedTo());
        assertEquals("The cannon must be on the piloted AT-AT",
                blizzard2, cannon.getAttachedTo());
        return scn;
    }

    private static void moveAndFollow(
            VirtualTableScenario scn,
            com.gempukku.swccgo.game.PhysicalCardImpl vehicle,
            com.gempukku.swccgo.game.PhysicalCardImpl destination,
            com.gempukku.swccgo.game.PhysicalCardImpl target) {
        scn.DSMoveCard(vehicle, destination);
        String lastDecision = "<none>";
        for (int responses = 0; responses < 10; responses++) {
            if (scn.GetCurrentDecision() == null) {
                scn.game().carryOutPendingActionsUntilDecisionNeeded();
            }
            if (scn.GetCurrentDecision() == null) {
                break;
            }

            lastDecision = decisionText(scn);
            if (scn.GetDSAvailableActions().stream()
                    .anyMatch(text -> text.contains(
                            "Follow vehicle moving from same site"))) {
                assertTrue(scn.DSCardActionAvailable(
                        target, "Follow vehicle moving from same site"));
                scn.DSUseCardAction(
                        target, "Follow vehicle moving from same site");
                scn.PassAllResponses();
                return;
            }
            if (scn.DSGetDecision() != null) {
                assertTrue("Unexpected Dark Side movement decision: "
                                + lastDecision,
                        lastDecision.toLowerCase().contains(
                                "optional response"));
                scn.DSPass();
            } else if (scn.LSGetDecision() != null) {
                assertTrue("Unexpected Light Side movement decision: "
                                + lastDecision,
                        lastDecision.toLowerCase().contains(
                                "optional response"));
                scn.LSPass();
            } else {
                break;
            }
        }
        fail("V Epic Event follow action never appeared; last decision="
                + lastDecision
                + ", dsActions=" + scn.GetDSAvailableActions()
                + ", lsActions=" + scn.GetLSAvailableActions()
                + ", vehicleLocation="
                + scn.game().getModifiersQuerying()
                        .getLocationThatCardIsAt(
                                scn.gameState(), vehicle)
                        .getBlueprint().getTitle()
                + ", rememberedVehicle="
                + (target.getWhileInPlayData() != null));
    }

    private static void fireAtMainPowerGenerators(
            VirtualTableScenario scn) {
        var target = scn.GetDSCard("target");
        var cannon = scn.GetDSCard("cannon");

        scn.DSUseCardAction(target, ATTEMPT);
        assertTrue(scn.DSDecisionAvailable("Choose AT-AT Cannon"));
        scn.DSChooseCard(cannon);
        scn.PassAllResponses();
        if (scn.LSDecisionAvailable("Choose Force to lose")) {
            scn.LSPayRemainingForceLossFromReserveDeck();
            scn.PassAllResponses();
        }
    }

    private static String decisionText(VirtualTableScenario scn) {
        return scn.GetCurrentDecision() == null
                ? "<none>"
                : scn.GetCurrentDecision().getText();
    }

    private static void trimDarkReserveTo(
            VirtualTableScenario scn,
            PhysicalCard... requiredCards) {
        List<PhysicalCard> reserve = new ArrayList<>(
                scn.gameState().getReserveDeck(
                        VirtualTableScenario.DS));
        List<PhysicalCard> keep = List.of(requiredCards);
        for (PhysicalCard card : reserve) {
            if (!keep.contains(card)) {
                scn.MoveOutOfPlay((PhysicalCardImpl) card);
            }
        }
    }

    private static PullActionFactsReader.Context pullContext(
            VirtualTableScenario scn,
            ObjectiveAnalyzer analyzer) {
        return new PullActionFactsReader.Context(
                scn.game(),
                scn.gameState(),
                VirtualTableScenario.DS,
                Side.DARK,
                Phase.CONTROL,
                SHIELD_PULL_ORACLE,
                new PullActionFactsReader.ObjectiveView() {
                    @Override
                    public boolean isAnalyzed() {
                        return analyzer.isAnalyzed();
                    }

                    @Override
                    public boolean isFlipped() {
                        return analyzer.isFlipped();
                    }

                    @Override
                    public String flipConditionText() {
                        return analyzer.getFlipConditionText();
                    }

                    @Override
                    public Set<String> strategyCharacterTokens(
                            SwccgGame game, String playerId) {
                        return analyzer.getStrategyCharacterTokens(
                                game, playerId);
                    }

                    @Override
                    public boolean hasTypedStrategyKeyCharacter() {
                        return analyzer.hasTypedStrategyKeyCharacter();
                    }

                    @Override
                    public boolean isStrategyKeyCharacter(
                            SwccgGame game, String playerId,
                            PhysicalCard candidate) {
                        return analyzer.isStrategyKeyCharacter(
                                game, playerId, candidate);
                    }

                    @Override
                    public boolean objectivePullAdvancesRequiredOnTableCard(
                            SwccgGame game, String playerId,
                            PhysicalCard source) {
                        return analyzer
                                .objectivePullAdvancesRequiredOnTableCard(
                                        game, playerId, source);
                    }
                },
                new PullActionFactsReader.LateView() {
                    @Override
                    public List<PhysicalCard> hand() {
                        return scn.gameState().getHand(
                                VirtualTableScenario.DS);
                    }

                    @Override
                    public boolean battlePlausible() {
                        return false;
                    }
                });
    }

    private static PolicyOperation operation(
            PolicyResult result, String ruleId) {
        return result.operations().stream()
                .filter(operation -> ruleId.equals(
                        operation.ruleArmId().id()))
                .findFirst()
                .orElseThrow();
    }

    private static BattleDecisionPolicy.Context battleContext(
            GameState gameState,
            SwccgGame game,
            ObjectiveAnalyzer analyzer,
            Logger logger,
            String playerId,
            PhysicalCard location) {
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
                return List.of("contest-third");
            }

            @Override
            public List<String> getActionTexts() {
                return List.of("Initiate battle at "
                        + location.getTitle());
            }

            @Override
            public List<String> getCardIds() {
                return List.of(String.valueOf(
                        location.getCardId()));
            }

            @Override
            public GameState getGameState() {
                return gameState;
            }

            @Override
            public SwccgGame getGame() {
                return game;
            }

            @Override
            public String getPlayerId() {
                return playerId;
            }

            @Override
            public int getReserveDeckSize() {
                return 10;
            }

            @Override
            public int getLifeForce() {
                return 20;
            }

            @Override
            public int getForcePileSize() {
                return 10;
            }

            @Override
            public int getHandSize() {
                return 0;
            }

            @Override
            public ObjectiveAnalyzer getObjectiveAnalyzer() {
                return analyzer;
            }

            @Override
            public float getVaderExpendabilityFactor() {
                return 1.0f;
            }

            @Override
            public int getCriticalLifeForce() {
                return 7;
            }

            @Override
            public BattleDecisionPolicy.Prediction predictBattle(
                    int myPower, int myDestinyDraws,
                    int opponentPower,
                    int opponentDestinyDraws) {
                return new BattleDecisionPolicy.Prediction(
                        0.8f, 4.0f, 2.0f);
            }

            @Override
            public Logger getLogger() {
                return logger;
            }
        };
    }

    private static PolicyResult forceLossPolicy(
            VirtualTableScenario scn,
            PhysicalCard card,
            boolean requiredForFlip) {
        return forceLossPolicy(
                scn, card, requiredForFlip, false);
    }

    private static PolicyResult forceLossPolicy(
            VirtualTableScenario scn,
            PhysicalCard card,
            boolean requiredForFlip,
            boolean pullableForObjective) {
        return ForceLossPolicy.score(
                String.valueOf(card.getCardId()),
                ForceLossPolicy.Route.STANDALONE,
                ForceLossFacts.readDecision(
                        scn.gameState(),
                        VirtualTableScenario.DS,
                        1),
                ForceLossFacts.readCandidate(
                        scn.gameState(),
                        VirtualTableScenario.DS,
                        card),
                new ForceLossPolicy.ObjectiveFlags(
                        false, false,
                        requiredForFlip,
                        pullableForObjective));
    }

    private static int deployCostAt(
            VirtualTableScenario scn,
            PhysicalCard card,
            PhysicalCard target) {
        float cost = scn.game().getModifiersQuerying()
                .getDeployCost(
                        scn.gameState(), card, card,
                        target, false, null, false,
                        0.0f, null, true);
        return (int) Math.ceil(Math.max(0.0f, cost));
    }

    private static void positionFollowFixture(
            VirtualTableScenario scn,
            PhysicalCard target,
            PhysicalCard mover,
            PhysicalCard origin,
            PhysicalCard destination) {
        scn.MoveCardsToLocation(
                (PhysicalCardImpl) destination,
                (PhysicalCardImpl) mover);
        scn.AttachCardsTo(
                (PhysicalCardImpl) origin,
                (PhysicalCardImpl) target);
        target.setWhileInPlayData(new WhileInPlayData(mover));
    }

    private static void assertForwardFollow(
            ObjectiveAnalyzer analyzer,
            VirtualTableScenario scn,
            PhysicalCard target,
            PhysicalCard mover,
            PhysicalCard origin,
            PhysicalCard destination) {
        positionFollowFixture(
                scn, target, mover, origin, destination);
        assertTrue(analyzer.isShieldMainGeneratorRouteAction(
                scn.game(), VirtualTableScenario.DS,
                String.valueOf(target.getCardId()),
                "Follow vehicle moving from same site"));

        List<EvaluatedCandidate> candidates =
                actionTextAdapter(
                        analyzer, scn, target,
                        List.of("wait", "forward-follow"),
                        List.of(
                                "Take another action",
                                "Follow vehicle moving from same site"));
        EvaluatedCandidate wait =
                evaluatedCandidate(candidates, "wait");
        EvaluatedCandidate follow =
                evaluatedCandidate(
                        candidates, "forward-follow");
        assertEquals(800.0f,
                follow.score() - wait.score(), 0.0f);
        assertTrue(follow.reasoning().contains(
                "V160 PUSH TARGET THE MAIN GENERATOR"));
    }

    private static void assertNoV160RoutePush(
            ObjectiveAnalyzer analyzer,
            VirtualTableScenario scn,
            PhysicalCard target,
            String actionId,
            String actionText) {
        assertFalse(analyzer.isShieldMainGeneratorRouteAction(
                scn.game(), VirtualTableScenario.DS,
                String.valueOf(target.getCardId()),
                actionText));
        List<EvaluatedCandidate> candidates =
                actionTextAdapter(
                        analyzer, scn, target,
                        List.of("wait", actionId),
                        List.of("Take another action", actionText));
        EvaluatedCandidate wait =
                evaluatedCandidate(candidates, "wait");
        EvaluatedCandidate route =
                evaluatedCandidate(candidates, actionId);
        assertEquals(0.0f,
                route.score() - wait.score(), 0.0f);
        assertFalse(route.reasoning().contains(
                "V160 PUSH TARGET THE MAIN GENERATOR"));
    }

    private record MovementCandidate(
            String actionId, float score,
            boolean hardVeto, String vetoReason,
            String reasoning) {
    }

    private record EvaluatedCandidate(
            String actionId, float score,
            boolean hardVeto, String vetoReason,
            String reasoning) {
    }

    private static List<MovementCandidate> movementAdapter(
            ObjectiveAnalyzer analyzer,
            VirtualTableScenario scn,
            PhysicalCard mover,
            List<String> actionIds,
            List<String> actionTexts) {
        List<String> cardIds = actionIds.stream()
                .map(actionId ->
                        String.valueOf(mover.getCardId()))
                .toList();
        if (analyzer instanceof
                com.gempukku.swccgo.ai.models.rando.strategy
                        .ObjectiveAnalyzer randoAnalyzer) {
            var context =
                    new com.gempukku.swccgo.ai.models.rando.evaluators
                            .DecisionContext(
                                    scn.gameState(),
                                    VirtualTableScenario.DS,
                                    "CARD_ACTION_CHOICE",
                                    "Choose movement action",
                                    "shield-post-flip-move",
                                    Phase.MOVE);
            context.setGame(scn.game());
            context.setSide(Side.DARK);
            context.setObjectiveAnalyzer(randoAnalyzer);
            context.setActionIds(actionIds);
            context.setActionTexts(actionTexts);
            context.setCardIds(cardIds);
            return new com.gempukku.swccgo.ai.models.rando.evaluators
                    .MoveEvaluator()
                    .evaluate(context).stream()
                    .map(action -> new MovementCandidate(
                            action.getActionId(),
                            action.getScore(),
                            action.isHardVetoed(),
                            action.getVetoReason(),
                            action.getReasoningString()))
                    .toList();
        }
        if (analyzer instanceof
                com.gempukku.swccgo.ai.models.chosenone.strategy
                        .ObjectiveAnalyzer chosenAnalyzer) {
            var context =
                    new com.gempukku.swccgo.ai.models.chosenone.evaluators
                            .DecisionContext(
                                    scn.gameState(),
                                    VirtualTableScenario.DS,
                                    "CARD_ACTION_CHOICE",
                                    "Choose movement action",
                                    "shield-post-flip-move",
                                    Phase.MOVE);
            context.setGame(scn.game());
            context.setSide(Side.DARK);
            context.setObjectiveAnalyzer(chosenAnalyzer);
            context.setActionIds(actionIds);
            context.setActionTexts(actionTexts);
            context.setCardIds(cardIds);
            return new com.gempukku.swccgo.ai.models.chosenone.evaluators
                    .MoveEvaluator()
                    .evaluate(context).stream()
                    .map(action -> new MovementCandidate(
                            action.getActionId(),
                            action.getScore(),
                            action.isHardVetoed(),
                            action.getVetoReason(),
                            action.getReasoningString()))
                    .toList();
        }
        throw new IllegalArgumentException(
                "Unsupported Shield analyzer adapter");
    }

    private static List<EvaluatedCandidate> actionTextAdapter(
            ObjectiveAnalyzer analyzer,
            VirtualTableScenario scn,
            PhysicalCard source,
            List<String> actionIds,
            List<String> actionTexts) {
        List<String> cardIds = actionIds.stream()
                .map(actionId ->
                        String.valueOf(source.getCardId()))
                .toList();
        Phase phase = actionTexts.stream()
                .anyMatch(text -> text.contains("Follow vehicle"))
                ? Phase.MOVE : Phase.DEPLOY;
        if (analyzer instanceof
                com.gempukku.swccgo.ai.models.rando.strategy
                        .ObjectiveAnalyzer randoAnalyzer) {
            var context =
                    new com.gempukku.swccgo.ai.models.rando.evaluators
                            .DecisionContext(
                                    scn.gameState(),
                                    VirtualTableScenario.DS,
                                    "CARD_ACTION_CHOICE",
                                    "Choose Shield route action",
                                    "shield-action-text",
                                    phase);
            context.setGame(scn.game());
            context.setSide(Side.DARK);
            context.setObjectiveAnalyzer(randoAnalyzer);
            context.setActionIds(actionIds);
            context.setActionTexts(actionTexts);
            context.setCardIds(cardIds);
            return new com.gempukku.swccgo.ai.models.rando.evaluators
                    .ActionTextEvaluator()
                    .evaluate(context).stream()
                    .map(action -> new EvaluatedCandidate(
                            action.getActionId(),
                            action.getScore(),
                            action.isHardVetoed(),
                            action.getVetoReason(),
                            action.getReasoningString()))
                    .toList();
        }
        if (analyzer instanceof
                com.gempukku.swccgo.ai.models.chosenone.strategy
                        .ObjectiveAnalyzer chosenAnalyzer) {
            var context =
                    new com.gempukku.swccgo.ai.models.chosenone.evaluators
                            .DecisionContext(
                                    scn.gameState(),
                                    VirtualTableScenario.DS,
                                    "CARD_ACTION_CHOICE",
                                    "Choose Shield route action",
                                    "shield-action-text",
                                    phase);
            context.setGame(scn.game());
            context.setSide(Side.DARK);
            context.setObjectiveAnalyzer(chosenAnalyzer);
            context.setActionIds(actionIds);
            context.setActionTexts(actionTexts);
            context.setCardIds(cardIds);
            return new com.gempukku.swccgo.ai.models.chosenone.evaluators
                    .ActionTextEvaluator()
                    .evaluate(context).stream()
                    .map(action -> new EvaluatedCandidate(
                            action.getActionId(),
                            action.getScore(),
                            action.isHardVetoed(),
                            action.getVetoReason(),
                            action.getReasoningString()))
                    .toList();
        }
        throw new IllegalArgumentException(
                "Unsupported Shield analyzer adapter");
    }

    private static EvaluatedCandidate combinedActionAdapter(
            ObjectiveAnalyzer analyzer,
            VirtualTableScenario scn,
            PhysicalCard source,
            List<String> actionIds,
            List<String> actionTexts) {
        List<String> cardIds = actionIds.stream()
                .map(actionId ->
                        String.valueOf(source.getCardId()))
                .toList();
        if (analyzer instanceof
                com.gempukku.swccgo.ai.models.rando.strategy
                        .ObjectiveAnalyzer randoAnalyzer) {
            var context =
                    new com.gempukku.swccgo.ai.models.rando.evaluators
                            .DecisionContext(
                                scn.gameState(),
                                VirtualTableScenario.DS,
                                "CARD_ACTION_CHOICE",
                                "Choose Shield route action",
                                "shield-combined-action",
                                Phase.DEPLOY);
            context.setGame(scn.game());
            context.setSide(Side.DARK);
            context.setObjectiveAnalyzer(randoAnalyzer);
            context.setActionIds(actionIds);
            context.setActionTexts(actionTexts);
            context.setCardIds(cardIds);
            var action =
                    new com.gempukku.swccgo.ai.models.rando.evaluators
                            .CombinedEvaluator()
                            .evaluateDecision(context);
            return new EvaluatedCandidate(
                    action.getActionId(),
                    action.getScore(),
                    action.isHardVetoed(),
                    action.getVetoReason(),
                    action.getReasoningString());
        }
        if (analyzer instanceof
                com.gempukku.swccgo.ai.models.chosenone.strategy
                        .ObjectiveAnalyzer chosenAnalyzer) {
            var context =
                    new com.gempukku.swccgo.ai.models.chosenone.evaluators
                            .DecisionContext(
                                scn.gameState(),
                                VirtualTableScenario.DS,
                                "CARD_ACTION_CHOICE",
                                "Choose Shield route action",
                                "shield-combined-action",
                                Phase.DEPLOY);
            context.setGame(scn.game());
            context.setSide(Side.DARK);
            context.setObjectiveAnalyzer(chosenAnalyzer);
            context.setActionIds(actionIds);
            context.setActionTexts(actionTexts);
            context.setCardIds(cardIds);
            var action =
                    new com.gempukku.swccgo.ai.models.chosenone.evaluators
                            .CombinedEvaluator()
                            .evaluateDecision(context);
            return new EvaluatedCandidate(
                    action.getActionId(),
                    action.getScore(),
                    action.isHardVetoed(),
                    action.getVetoReason(),
                    action.getReasoningString());
        }
        throw new IllegalArgumentException(
                "Unsupported Shield analyzer adapter");
    }

    private static List<EvaluatedCandidate> deployActionAdapter(
            ObjectiveAnalyzer analyzer,
            VirtualTableScenario scn,
            PhysicalCard source,
            List<String> actionIds,
            List<String> actionTexts) {
        List<String> cardIds = actionIds.stream()
                .map(actionId ->
                        String.valueOf(source.getCardId()))
                .toList();
        List<String> blueprintIds = actionIds.stream()
                .map(actionId ->
                        source.getBlueprintId(true))
                .toList();
        List<String> titles = actionIds.stream()
                .map(actionId -> source.getTitle())
                .toList();
        if (analyzer instanceof
                com.gempukku.swccgo.ai.models.rando.strategy
                        .ObjectiveAnalyzer randoAnalyzer) {
            var context =
                    new com.gempukku.swccgo.ai.models.rando.evaluators
                            .DecisionContext(
                                scn.gameState(),
                                VirtualTableScenario.DS,
                                "CARD_ACTION_CHOICE",
                                "Choose Deploy action or Pass",
                                "shield-deploy-action",
                                Phase.DEPLOY);
            context.setGame(scn.game());
            context.setSide(Side.DARK);
            context.setObjectiveAnalyzer(randoAnalyzer);
            context.setActionIds(actionIds);
            context.setActionTexts(actionTexts);
            context.setCardIds(cardIds);
            context.setBlueprints(blueprintIds);
            context.setTestingTexts(titles);
            return new com.gempukku.swccgo.ai.models.rando.evaluators
                    .DeployEvaluator()
                    .evaluate(context).stream()
                    .map(action -> new EvaluatedCandidate(
                            action.getActionId(),
                            action.getScore(),
                            action.isHardVetoed(),
                            action.getVetoReason(),
                            action.getReasoningString()))
                    .toList();
        }
        if (analyzer instanceof
                com.gempukku.swccgo.ai.models.chosenone.strategy
                        .ObjectiveAnalyzer chosenAnalyzer) {
            var context =
                    new com.gempukku.swccgo.ai.models.chosenone.evaluators
                            .DecisionContext(
                                scn.gameState(),
                                VirtualTableScenario.DS,
                                "CARD_ACTION_CHOICE",
                                "Choose Deploy action or Pass",
                                "shield-deploy-action",
                                Phase.DEPLOY);
            context.setGame(scn.game());
            context.setSide(Side.DARK);
            context.setObjectiveAnalyzer(chosenAnalyzer);
            context.setActionIds(actionIds);
            context.setActionTexts(actionTexts);
            context.setCardIds(cardIds);
            context.setBlueprints(blueprintIds);
            context.setTestingTexts(titles);
            return new com.gempukku.swccgo.ai.models.chosenone.evaluators
                    .DeployEvaluator()
                    .evaluate(context).stream()
                    .map(action -> new EvaluatedCandidate(
                            action.getActionId(),
                            action.getScore(),
                            action.isHardVetoed(),
                            action.getVetoReason(),
                            action.getReasoningString()))
                    .toList();
        }
        throw new IllegalArgumentException(
                "Unsupported Shield analyzer adapter");
    }

    private static List<EvaluatedCandidate> destinationAdapter(
            ObjectiveAnalyzer analyzer,
            VirtualTableScenario scn,
            PhysicalCard mover,
            PhysicalCard... destinations) {
        List<String> cardIds = new ArrayList<>();
        List<String> blueprints = new ArrayList<>();
        List<String> testingTexts = new ArrayList<>();
        List<Boolean> selectable = new ArrayList<>();
        for (PhysicalCard destination : destinations) {
            cardIds.add(String.valueOf(destination.getCardId()));
            blueprints.add(destination.getBlueprintId(true));
            testingTexts.add(destination.getTitle());
            selectable.add(true);
        }
        String decisionText = "Choose where to move "
                + "<div class='cardHint' value='"
                + mover.getBlueprintId(true) + "'>"
                + mover.getTitle()
                + "</div> using landspeed";
        if (analyzer instanceof
                com.gempukku.swccgo.ai.models.rando.strategy
                        .ObjectiveAnalyzer randoAnalyzer) {
            var context =
                    new com.gempukku.swccgo.ai.models.rando.evaluators
                            .DecisionContext(
                                    scn.gameState(),
                                    VirtualTableScenario.DS,
                                    "CARD_SELECTION",
                                    decisionText,
                                    "shield-move-destination",
                                    Phase.MOVE);
            context.setGame(scn.game());
            context.setSide(Side.DARK);
            context.setObjectiveAnalyzer(randoAnalyzer);
            setDestinationContext(
                    context, mover, cardIds,
                    blueprints, testingTexts, selectable);
            return new com.gempukku.swccgo.ai.models.rando.evaluators
                    .CardSelectionEvaluator()
                    .evaluate(context).stream()
                    .map(action -> new EvaluatedCandidate(
                            action.getActionId(),
                            action.getScore(),
                            action.isHardVetoed(),
                            action.getVetoReason(),
                            action.getReasoningString()))
                    .toList();
        }
        if (analyzer instanceof
                com.gempukku.swccgo.ai.models.chosenone.strategy
                        .ObjectiveAnalyzer chosenAnalyzer) {
            var context =
                    new com.gempukku.swccgo.ai.models.chosenone.evaluators
                            .DecisionContext(
                                    scn.gameState(),
                                    VirtualTableScenario.DS,
                                    "CARD_SELECTION",
                                    decisionText,
                                    "shield-move-destination",
                                    Phase.MOVE);
            context.setGame(scn.game());
            context.setSide(Side.DARK);
            context.setObjectiveAnalyzer(chosenAnalyzer);
            setDestinationContext(
                    context, mover, cardIds,
                    blueprints, testingTexts, selectable);
            return new com.gempukku.swccgo.ai.models.chosenone.evaluators
                    .CardSelectionEvaluator()
                    .evaluate(context).stream()
                    .map(action -> new EvaluatedCandidate(
                            action.getActionId(),
                            action.getScore(),
                            action.isHardVetoed(),
                            action.getVetoReason(),
                            action.getReasoningString()))
                    .toList();
        }
        throw new IllegalArgumentException(
                "Unsupported Shield analyzer adapter");
    }

    private static List<EvaluatedCandidate> pullCandidateAdapter(
            ObjectiveAnalyzer analyzer,
            VirtualTableScenario scn,
            PhysicalCard... candidates) {
        return pullCandidateAdapter(
                analyzer, scn, false,
                candidates);
    }

    private static List<EvaluatedCandidate>
            temporaryPullCandidateAdapter(
                    ObjectiveAnalyzer analyzer,
                    VirtualTableScenario scn,
                    PhysicalCard... candidates) {
        return pullCandidateAdapter(
                analyzer, scn, true,
                candidates);
    }

    private static List<EvaluatedCandidate> pullCandidateAdapter(
            ObjectiveAnalyzer analyzer,
            VirtualTableScenario scn,
            boolean temporaryIds,
            PhysicalCard... candidates) {
        List<String> cardIds = new ArrayList<>();
        List<String> blueprints = new ArrayList<>();
        List<String> testingTexts = new ArrayList<>();
        List<Boolean> selectable = new ArrayList<>();
        for (int index = 0;
                index < candidates.length; index++) {
            PhysicalCard candidate = candidates[index];
            cardIds.add(temporaryIds
                    ? "temp" + index
                    : String.valueOf(
                        candidate.getCardId()));
            blueprints.add(candidate.getBlueprintId(true));
            testingTexts.add(candidate.getTitle());
            selectable.add(true);
        }
        if (analyzer instanceof
                com.gempukku.swccgo.ai.models.rando.strategy
                        .ObjectiveAnalyzer randoAnalyzer) {
            var context =
                    new com.gempukku.swccgo.ai.models.rando.evaluators
                            .DecisionContext(
                                    scn.gameState(),
                                    VirtualTableScenario.DS,
                                    "ARBITRARY_CARDS",
                                    "Choose card to take into hand",
                                    "shield-pull-candidate",
                                    Phase.CONTROL);
            context.setGame(scn.game());
            context.setSide(Side.DARK);
            context.setObjectiveAnalyzer(randoAnalyzer);
            setPullCandidateContext(
                    context, cardIds, blueprints,
                    testingTexts, selectable);
            return new com.gempukku.swccgo.ai.models.rando.evaluators
                    .CardSelectionEvaluator()
                    .evaluate(context).stream()
                    .map(action -> new EvaluatedCandidate(
                            action.getActionId(),
                            action.getScore(),
                            action.isHardVetoed(),
                            action.getVetoReason(),
                            action.getReasoningString()))
                    .toList();
        }
        if (analyzer instanceof
                com.gempukku.swccgo.ai.models.chosenone.strategy
                        .ObjectiveAnalyzer chosenAnalyzer) {
            var context =
                    new com.gempukku.swccgo.ai.models.chosenone.evaluators
                            .DecisionContext(
                                    scn.gameState(),
                                    VirtualTableScenario.DS,
                                    "ARBITRARY_CARDS",
                                    "Choose card to take into hand",
                                    "shield-pull-candidate",
                                    Phase.CONTROL);
            context.setGame(scn.game());
            context.setSide(Side.DARK);
            context.setObjectiveAnalyzer(chosenAnalyzer);
            setPullCandidateContext(
                    context, cardIds, blueprints,
                    testingTexts, selectable);
            return new com.gempukku.swccgo.ai.models.chosenone.evaluators
                    .CardSelectionEvaluator()
                    .evaluate(context).stream()
                    .map(action -> new EvaluatedCandidate(
                            action.getActionId(),
                            action.getScore(),
                            action.isHardVetoed(),
                            action.getVetoReason(),
                            action.getReasoningString()))
                    .toList();
        }
        throw new IllegalArgumentException(
                "Unsupported Shield analyzer adapter");
    }

    private static List<EvaluatedCandidate> forceLossAdapter(
            ObjectiveAnalyzer analyzer,
            VirtualTableScenario scn,
            PhysicalCard... candidates) {
        return forceLossAdapter(
                analyzer, scn,
                "Choose Force to lose",
                candidates);
    }

    private static List<EvaluatedCandidate> forceLossAdapter(
            ObjectiveAnalyzer analyzer,
            VirtualTableScenario scn,
            String decisionText,
            PhysicalCard... candidates) {
        List<String> cardIds = new ArrayList<>();
        List<Boolean> selectable = new ArrayList<>();
        for (PhysicalCard candidate : candidates) {
            cardIds.add(String.valueOf(
                    candidate.getCardId()));
            selectable.add(true);
        }
        if (analyzer instanceof
                com.gempukku.swccgo.ai.models.rando.strategy
                        .ObjectiveAnalyzer randoAnalyzer) {
            var context =
                    new com.gempukku.swccgo.ai.models.rando.evaluators
                            .DecisionContext(
                                    scn.gameState(),
                                    VirtualTableScenario.DS,
                                    "CARD_SELECTION",
                                    decisionText,
                                    "shield-force-loss",
                                    Phase.CONTROL);
            context.setGame(scn.game());
            context.setSide(Side.DARK);
            context.setObjectiveAnalyzer(randoAnalyzer);
            context.setCardIds(cardIds);
            context.setSelectable(selectable);
            return new com.gempukku.swccgo.ai.models.rando.evaluators
                    .CardSelectionEvaluator()
                    .evaluate(context).stream()
                    .map(action -> new EvaluatedCandidate(
                            action.getActionId(),
                            action.getScore(),
                            action.isHardVetoed(),
                            action.getVetoReason(),
                            action.getReasoningString()))
                    .toList();
        }
        if (analyzer instanceof
                com.gempukku.swccgo.ai.models.chosenone.strategy
                        .ObjectiveAnalyzer chosenAnalyzer) {
            var context =
                    new com.gempukku.swccgo.ai.models.chosenone.evaluators
                            .DecisionContext(
                                    scn.gameState(),
                                    VirtualTableScenario.DS,
                                    "CARD_SELECTION",
                                    decisionText,
                                    "shield-force-loss",
                                    Phase.CONTROL);
            context.setGame(scn.game());
            context.setSide(Side.DARK);
            context.setObjectiveAnalyzer(chosenAnalyzer);
            context.setCardIds(cardIds);
            context.setSelectable(selectable);
            return new com.gempukku.swccgo.ai.models.chosenone.evaluators
                    .CardSelectionEvaluator()
                    .evaluate(context).stream()
                    .map(action -> new EvaluatedCandidate(
                            action.getActionId(),
                            action.getScore(),
                            action.isHardVetoed(),
                            action.getVetoReason(),
                            action.getReasoningString()))
                    .toList();
        }
        throw new IllegalArgumentException(
                "Unsupported Shield analyzer adapter");
    }

    private static List<EvaluatedCandidate> deployDestinationAdapter(
            ObjectiveAnalyzer analyzer,
            VirtualTableScenario scn,
            PhysicalCard deployingCard,
            PhysicalCard... destinations) {
        List<String> cardIds = new ArrayList<>();
        List<String> blueprints = new ArrayList<>();
        List<String> testingTexts = new ArrayList<>();
        List<Boolean> selectable = new ArrayList<>();
        for (PhysicalCard destination : destinations) {
            cardIds.add(String.valueOf(destination.getCardId()));
            blueprints.add(destination.getBlueprintId(true));
            testingTexts.add(destination.getTitle());
            selectable.add(true);
        }
        String decisionText = "Choose where to deploy "
                + "<div class='cardHint' value='"
                + deployingCard.getBlueprintId(true) + "'>"
                + deployingCard.getTitle() + "</div>";
        if (analyzer instanceof
                com.gempukku.swccgo.ai.models.rando.strategy
                        .ObjectiveAnalyzer randoAnalyzer) {
            var context =
                    new com.gempukku.swccgo.ai.models.rando.evaluators
                            .DecisionContext(
                                    scn.gameState(),
                                    VirtualTableScenario.DS,
                                    "CARD_SELECTION",
                                    decisionText,
                                    "shield-deploy-destination",
                                    Phase.DEPLOY);
            context.setGame(scn.game());
            context.setSide(Side.DARK);
            context.setObjectiveAnalyzer(randoAnalyzer);
            setPullCandidateContext(
                    context, cardIds, blueprints,
                    testingTexts, selectable);
            return new com.gempukku.swccgo.ai.models.rando.evaluators
                    .CardSelectionEvaluator()
                    .evaluate(context).stream()
                    .map(action -> new EvaluatedCandidate(
                            action.getActionId(),
                            action.getScore(),
                            action.isHardVetoed(),
                            action.getVetoReason(),
                            action.getReasoningString()))
                    .toList();
        }
        if (analyzer instanceof
                com.gempukku.swccgo.ai.models.chosenone.strategy
                        .ObjectiveAnalyzer chosenAnalyzer) {
            var context =
                    new com.gempukku.swccgo.ai.models.chosenone.evaluators
                            .DecisionContext(
                                    scn.gameState(),
                                    VirtualTableScenario.DS,
                                    "CARD_SELECTION",
                                    decisionText,
                                    "shield-deploy-destination",
                                    Phase.DEPLOY);
            context.setGame(scn.game());
            context.setSide(Side.DARK);
            context.setObjectiveAnalyzer(chosenAnalyzer);
            setPullCandidateContext(
                    context, cardIds, blueprints,
                    testingTexts, selectable);
            return new com.gempukku.swccgo.ai.models.chosenone.evaluators
                    .CardSelectionEvaluator()
                    .evaluate(context).stream()
                    .map(action -> new EvaluatedCandidate(
                            action.getActionId(),
                            action.getScore(),
                            action.isHardVetoed(),
                            action.getVetoReason(),
                            action.getReasoningString()))
                    .toList();
        }
        throw new IllegalArgumentException(
                "Unsupported Shield analyzer adapter");
    }

    private static void setPullCandidateContext(
            com.gempukku.swccgo.ai.models.rando.evaluators
                    .DecisionContext context,
            List<String> cardIds,
            List<String> blueprints,
            List<String> testingTexts,
            List<Boolean> selectable) {
        context.setCardIds(cardIds);
        context.setBlueprints(blueprints);
        context.setTestingTexts(testingTexts);
        context.setSelectable(selectable);
        context.setNoPass(true);
        context.setMin(1);
        context.setMax(1);
    }

    private static void setPullCandidateContext(
            com.gempukku.swccgo.ai.models.chosenone.evaluators
                    .DecisionContext context,
            List<String> cardIds,
            List<String> blueprints,
            List<String> testingTexts,
            List<Boolean> selectable) {
        context.setCardIds(cardIds);
        context.setBlueprints(blueprints);
        context.setTestingTexts(testingTexts);
        context.setSelectable(selectable);
        context.setNoPass(true);
        context.setMin(1);
        context.setMax(1);
    }

    private static void setDestinationContext(
            com.gempukku.swccgo.ai.models.rando.evaluators
                    .DecisionContext context,
            PhysicalCard mover,
            List<String> cardIds,
            List<String> blueprints,
            List<String> testingTexts,
            List<Boolean> selectable) {
        context.setCardIds(cardIds);
        context.setBlueprints(blueprints);
        context.setTestingTexts(testingTexts);
        context.setSelectable(selectable);
        context.setNoPass(true);
        context.setMin(1);
        context.setMax(1);
        context.setExtra(
                MovePhysicalCardResolver.MOVER_CARD_ID_EXTRA,
                mover.getCardId());
    }

    private static void setDestinationContext(
            com.gempukku.swccgo.ai.models.chosenone.evaluators
                    .DecisionContext context,
            PhysicalCard mover,
            List<String> cardIds,
            List<String> blueprints,
            List<String> testingTexts,
            List<Boolean> selectable) {
        context.setCardIds(cardIds);
        context.setBlueprints(blueprints);
        context.setTestingTexts(testingTexts);
        context.setSelectable(selectable);
        context.setNoPass(true);
        context.setMin(1);
        context.setMax(1);
        context.setExtra(
                MovePhysicalCardResolver.MOVER_CARD_ID_EXTRA,
                mover.getCardId());
    }

    private static MovementCandidate movementCandidate(
            List<MovementCandidate> candidates,
            String actionId) {
        return candidates.stream()
                .filter(candidate ->
                        actionId.equals(candidate.actionId()))
                .findFirst()
                .orElseThrow();
    }

    private static EvaluatedCandidate evaluatedCandidate(
            List<EvaluatedCandidate> candidates,
            String actionId) {
        return candidates.stream()
                .filter(candidate ->
                        actionId.equals(candidate.actionId()))
                .findFirst()
                .orElseThrow();
    }

    private static EvaluatedCandidate evaluatedCandidate(
            List<EvaluatedCandidate> candidates,
            PhysicalCard card) {
        return evaluatedCandidate(
                candidates,
                String.valueOf(card.getCardId()));
    }

    private static boolean hasOperation(
            PolicyResult result, String ruleId) {
        return result.operations().stream()
                .anyMatch(operation -> ruleId.equals(
                        operation.ruleArmId().id()));
    }

    private static final PullOracleView SHIELD_PULL_ORACLE =
            new PullOracleView() {
                private final Validation succeeds =
                        new Validation(
                                Outcome.WILL_SUCCEED,
                                "AT-AT Cannon remains in Reserve Deck");

                @Override
                public boolean isAvailable() {
                    return true;
                }

                @Override
                public boolean isAnalyzed() {
                    return true;
                }

                @Override
                public boolean hasTargetInReserve(String... keywords) {
                    return true;
                }

                @Override
                public boolean hasTargetInZone(
                        Zone zone, String target) {
                    return zone == Zone.RESERVE_DECK;
                }

                @Override
                public boolean isGenericTypeWord(String word) {
                    return false;
                }

                @Override
                public Zone parseSourceZone(String actionText) {
                    return Zone.RESERVE_DECK;
                }

                @Override
                public List<String> parseSourceCardPullTargets(
                        String gameText) {
                    return List.of("at-at cannon");
                }

                @Override
                public String sourceCardFullGameText(
                        SwccgCardBlueprint blueprint, Side side) {
                    return blueprint != null
                            ? blueprint.getGameText() : null;
                }

                @Override
                public Validation validatePull(
                        Zone zone, String... keywords) {
                    return succeeds;
                }

                @Override
                public Validation validatePullFromSourceCard(
                        Zone zone, String gameText) {
                    return succeeds;
                }

                @Override
                public TypedReserveMatch typedReserveMatch(
                        SwccgGame game, String playerId,
                        String noun) {
                    return TypedReserveMatch.MATCH;
                }

                @Override
                public boolean reserveTargetsAreAllUnattachableWeapons(
                        SwccgGame game, String playerId,
                        List<String> targets) {
                    return true;
                }

                @Override
                public boolean reservePullFetchesOnlyStarships(
                        String gameText) {
                    return false;
                }

                @Override
                public boolean spaceLocationOnTable() {
                    return false;
                }

                @Override
                public int countMatchingInDeck(
                        SwccgGame game, String playerId,
                        String noun) {
                    return 1;
                }

                @Override
                public int countMatchingInHandOrTable(
                        SwccgGame game, String playerId,
                        String noun) {
                    return 0;
                }

                @Override
                public List<NamedDeckCard> namedDeckCardsInText(
                        String gameText,
                        String sourceBlueprintId) {
                    return List.of();
                }

                @Override
                public boolean personaNamedInText(
                        Set<?> personas, String text) {
                    return false;
                }
            };

    private static List<ObjectiveAnalyzer> analyzers(
            VirtualTableScenario scn) {
        List<ObjectiveAnalyzer> analyzers = List.of(
                new com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer(),
                new com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer());
        for (ObjectiveAnalyzer analyzer : analyzers) {
            analyzer.analyze(
                    scn.game(), VirtualTableScenario.DS,
                    com.gempukku.swccgo.common.Side.DARK);
        }
        return analyzers;
    }

    private static ObjectiveAnalyzer.FlipLocationRuleState onlyState(
            ObjectiveAnalyzer analyzer, VirtualTableScenario scn,
            String phase, String purpose) {
        List<ObjectiveAnalyzer.FlipLocationRuleState> states =
                analyzer.assessFlipLocationRules(
                        scn.game(), VirtualTableScenario.DS,
                        phase, purpose);
        assertEquals(1, states.size());
        return states.get(0);
    }
}
