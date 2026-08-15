package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.strategy.ObjectiveAnalyzer;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DeployPhaseScriptCharacterizationTest {
    @Test
    public void randoBucketOrderIsFrozen() {
        assertEquals("[LOCATIONS, OTHER_CHARACTERS, WEAPONS, DEVICES]",
                java.util.Arrays.toString(
                        com.gempukku.swccgo.ai.models.rando.strategy.DeployPhaseScript.Step.values()));
    }

    @Test
    public void chosenBucketOrderIsFrozen() {
        assertEquals("[LOCATIONS, OTHER_CHARACTERS, WEAPONS, DEVICES]",
                java.util.Arrays.toString(
                        com.gempukku.swccgo.ai.models.chosenone.strategy.DeployPhaseScript.Step.values()));
    }

    @Test
    public void bothBotsUseTheSharedDeployPhaseScriptOwner() {
        assertEquals(DeployPhaseScript.class,
                com.gempukku.swccgo.ai.models.rando.strategy.DeployPhaseScript.class.getSuperclass());
        assertEquals(DeployPhaseScript.class,
                com.gempukku.swccgo.ai.models.chosenone.strategy.DeployPhaseScript.class.getSuperclass());
    }

    @Test
    public void randoKeywordFallbackPreservesV179LocationParity() throws Exception {
        Object script = new com.gempukku.swccgo.ai.models.rando.strategy.DeployPhaseScript();
        assertEquals("LOCATIONS", classify(script, "deploy a farm from reserve deck"));
        assertEquals("OTHER_CHARACTERS", classify(script, "deploy a character"));
        assertEquals("WEAPONS", classify(script, "download a lightsaber"));
        assertEquals("DEVICES", classify(script, "deploy a device"));
    }

    @Test
    public void chosenKeywordFallbackMatchesRando() throws Exception {
        Object script = new com.gempukku.swccgo.ai.models.chosenone.strategy.DeployPhaseScript();
        assertEquals("LOCATIONS", classify(script, "deploy a farm from reserve deck"));
        assertEquals("OTHER_CHARACTERS", classify(script, "deploy a character"));
        assertEquals("WEAPONS", classify(script, "download a lightsaber"));
        assertEquals("DEVICES", classify(script, "deploy a device"));
    }

    @Test
    public void takeIntoHandAndNoOpinionStayOutsideDeployBuckets() throws Exception {
        Object script = new com.gempukku.swccgo.ai.models.rando.strategy.DeployPhaseScript();
        assertTrue(resolveSteps(script,
                "Take a weapon into hand from Reserve Deck").isEmpty());
        assertTrue(resolveSteps(script, "Use game text").isEmpty());
    }

    @Test
    public void bothBotsKeepFlipGateActorUploadOutOfDeployBuckets() throws Exception {
        AwaitingDecision decision = mock(AwaitingDecision.class);
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        PhysicalCard sidious = mock(PhysicalCard.class);
        ObjectiveAnalyzer objectiveAnalyzer = mock(ObjectiveAnalyzer.class);

        Map<String, String[]> parameters = new LinkedHashMap<>();
        parameters.put("actionId", new String[] {"upload", "security"});
        parameters.put("actionText", new String[] {
                "Take card into hand from Reserve Deck", "Deploy a character"
        });
        parameters.put("cardId", new String[] {"190", null});

        when(decision.getDecisionParameters()).thenReturn(parameters);
        when(gameState.getPlayersLatestTurnNumber("p")).thenReturn(3);
        when(gameState.findCardById(190)).thenReturn(sidious);
        when(objectiveAnalyzer.getStrategyCharacterTokens(game, "p"))
                .thenReturn(Collections.emptySet());
        when(objectiveAnalyzer.isFlipGateActorUploadIntoHandAction(
                game, "p", sidious, "Take card into hand from Reserve Deck"))
                .thenReturn(true);

        DeployPhaseScript.Result rando =
                new com.gempukku.swccgo.ai.models.rando.strategy.DeployPhaseScript()
                        .selectAllowedActions(
                                decision, gameState, game, "p", objectiveAnalyzer);
        DeployPhaseScript.Result chosen =
                new com.gempukku.swccgo.ai.models.chosenone.strategy.DeployPhaseScript()
                        .selectAllowedActions(
                                decision, gameState, game, "p", objectiveAnalyzer);

        assertEquals(
                "[OTHER_CHARACTERS:[[security]], "
                        + "OTHER_CHARACTERS:[[security]]]",
                java.util.Arrays.toString(new Object[] {
                        rando.step + ":" + rando.stepBuckets,
                        chosen.step + ":" + chosen.stepBuckets
                }));
        assertFalse(rando.allowedActionIds.contains("upload"));
        assertFalse(chosen.allowedActionIds.contains("upload"));
    }

    @Test
    public void firstOrderNavyRouteJoinsTheTopBucketOnlyWhenExecutable() {
        AwaitingDecision decision = mock(AwaitingDecision.class);
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        PhysicalCard navy = mock(PhysicalCard.class);
        ObjectiveAnalyzer objectiveAnalyzer = mock(ObjectiveAnalyzer.class);

        Map<String, String[]> parameters = new LinkedHashMap<>();
        parameters.put("actionId", new String[] {"navy", "throne", "ground"});
        parameters.put("actionText", new String[] {
                "Reveal starship or pilot from hand",
                "Deploy a location",
                "Deploy a character"
        });
        parameters.put("cardId", new String[] {"225", null, null});
        when(decision.getDecisionParameters()).thenReturn(parameters);
        when(gameState.getPlayersLatestTurnNumber("p")).thenReturn(3);
        when(gameState.findCardById(225)).thenReturn(navy);
        when(objectiveAnalyzer.getStrategyCharacterTokens(game, "p"))
                .thenReturn(Collections.emptySet());
        when(objectiveAnalyzer.isFirstOrderReignsNavyRouteAction(
                game, "p", navy, "Reveal starship or pilot from hand"))
                .thenReturn(true);

        for (DeployPhaseScript script : new DeployPhaseScript[] {
                new com.gempukku.swccgo.ai.models.rando.strategy.DeployPhaseScript(),
                new com.gempukku.swccgo.ai.models.chosenone.strategy.DeployPhaseScript()
        }) {
            DeployPhaseScript.Result result = script.selectAllowedActions(
                    decision, gameState, game, "p", objectiveAnalyzer);
            assertEquals("[LOCATIONS, OTHER_CHARACTERS]",
                    result.stepBucketLabels.toString());
            assertEquals("[[throne], [navy, ground]]",
                    result.stepBuckets.toString());
        }

        when(objectiveAnalyzer.isFirstOrderReignsNavyRouteAction(
                game, "p", navy, "Reveal starship or pilot from hand"))
                .thenReturn(false);
        DeployPhaseScript.Result wrongSource =
                new com.gempukku.swccgo.ai.models.rando.strategy.DeployPhaseScript()
                        .selectAllowedActions(
                                decision, gameState, game, "p", objectiveAnalyzer);
        assertEquals("[[throne], [ground]]",
                wrongSource.stepBuckets.toString());
    }

    @Test
    public void exactMassassiUploadStaysOutOfDeployBucketsForBothBots() {
        AwaitingDecision decision = mock(AwaitingDecision.class);
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        PhysicalCard objective = mock(PhysicalCard.class);
        PhysicalCard impostor = mock(PhysicalCard.class);
        ObjectiveAnalyzer analyzer = mock(ObjectiveAnalyzer.class);

        Map<String, String[]> parameters = new LinkedHashMap<>();
        parameters.put("actionId",
                new String[] {"mbo-upload", "impostor", "body"});
        parameters.put("actionText", new String[] {
                "Take card into hand from Reserve Deck",
                "Take card into hand from Reserve Deck",
                "Deploy a character"
        });
        parameters.put("cardId", new String[] {"111", "999", null});
        when(decision.getDecisionParameters()).thenReturn(parameters);
        when(gameState.getPlayersLatestTurnNumber("p")).thenReturn(3);
        when(gameState.findCardById(111)).thenReturn(objective);
        when(gameState.findCardById(999)).thenReturn(impostor);
        when(analyzer.getStrategyCharacterTokens(game, "p"))
                .thenReturn(Collections.emptySet());
        when(analyzer.isMassassiAttackRunPackageUploadAction(
                game, "p", objective,
                "Take card into hand from Reserve Deck"))
                .thenReturn(true);

        for (DeployPhaseScript script : new DeployPhaseScript[] {
                new com.gempukku.swccgo.ai.models.rando.strategy.DeployPhaseScript(),
                new com.gempukku.swccgo.ai.models.chosenone.strategy.DeployPhaseScript()
        }) {
            DeployPhaseScript.Result result = script.selectAllowedActions(
                    decision, gameState, game, "p", analyzer);
            assertEquals("[OTHER_CHARACTERS]",
                    result.stepBucketLabels.toString());
            assertEquals("[[body]]",
                    result.stepBuckets.toString());
            assertFalse(result.allowedActionIds.contains("mbo-upload"));
            assertFalse(result.allowedActionIds.contains("impostor"));
        }
    }

    @Test
    public void massassiEpicEventStaysOutWhileDeployableCardsUseNaturalBuckets() {
        AwaitingDecision decision = mock(AwaitingDecision.class);
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ObjectiveAnalyzer analyzer = mock(ObjectiveAnalyzer.class);
        PhysicalCard attackRun = mock(PhysicalCard.class);
        PhysicalCard carrier = mock(PhysicalCard.class);
        PhysicalCard torpedoes = mock(PhysicalCard.class);
        PhysicalCard body = mock(PhysicalCard.class);
        SwccgCardBlueprint attackRunBlueprint = mock(SwccgCardBlueprint.class);
        SwccgCardBlueprint carrierBlueprint = mock(SwccgCardBlueprint.class);
        SwccgCardBlueprint torpedoesBlueprint = mock(SwccgCardBlueprint.class);
        SwccgCardBlueprint bodyBlueprint = mock(SwccgCardBlueprint.class);

        Map<String, String[]> parameters = new LinkedHashMap<>();
        parameters.put("actionId", new String[] {
                "attack-run", "carrier", "torpedoes", "body"
        });
        parameters.put("actionText", new String[] {
                "Deploy", "Deploy", "Deploy", "Deploy"
        });
        parameters.put("cardId", new String[] {"201", "202", "203", "204"});
        when(decision.getDecisionParameters()).thenReturn(parameters);
        when(gameState.getPlayersLatestTurnNumber("p")).thenReturn(3);
        when(gameState.findCardById(201)).thenReturn(attackRun);
        when(gameState.findCardById(202)).thenReturn(carrier);
        when(gameState.findCardById(203)).thenReturn(torpedoes);
        when(gameState.findCardById(204)).thenReturn(body);
        when(attackRun.getBlueprint()).thenReturn(attackRunBlueprint);
        when(carrier.getBlueprint()).thenReturn(carrierBlueprint);
        when(torpedoes.getBlueprint()).thenReturn(torpedoesBlueprint);
        when(body.getBlueprint()).thenReturn(bodyBlueprint);
        when(attackRunBlueprint.getCardCategory()).thenReturn(
                com.gempukku.swccgo.common.CardCategory.EPIC_EVENT);
        when(carrierBlueprint.getCardCategory()).thenReturn(
                com.gempukku.swccgo.common.CardCategory.STARSHIP);
        when(torpedoesBlueprint.getCardCategory()).thenReturn(
                com.gempukku.swccgo.common.CardCategory.WEAPON);
        when(bodyBlueprint.getCardCategory()).thenReturn(
                com.gempukku.swccgo.common.CardCategory.CHARACTER);
        when(analyzer.getStrategyCharacterTokens(game, "p"))
                .thenReturn(Collections.emptySet());
        when(analyzer.isMassassiAttackRunPackageDeployCandidate(
                game, "p", attackRun)).thenReturn(true);
        when(analyzer.isMassassiAttackRunCarrierDeployCandidate(
                game, "p", carrier)).thenReturn(true);
        when(analyzer.isMassassiAttackRunPackageDeployCandidate(
                game, "p", torpedoes)).thenReturn(true);

        for (DeployPhaseScript script : new DeployPhaseScript[] {
                new com.gempukku.swccgo.ai.models.rando.strategy.DeployPhaseScript(),
                new com.gempukku.swccgo.ai.models.chosenone.strategy.DeployPhaseScript()
        }) {
            DeployPhaseScript.Result result = script.selectAllowedActions(
                    decision, gameState, game, "p", analyzer);
            assertEquals("[OTHER_CHARACTERS, WEAPONS]",
                    result.stepBucketLabels.toString());
            assertEquals("[[carrier, body], [torpedoes]]",
                    result.stepBuckets.toString());
            assertFalse(result.allowedActionIds.contains("attack-run"));
        }
    }

    @Test
    public void twinSunsOccupationEffectCannotPreemptACharacterDeploy() {
        AwaitingDecision decision = mock(AwaitingDecision.class);
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        PhysicalCard objective = mock(PhysicalCard.class);
        ObjectiveAnalyzer analyzer = mock(ObjectiveAnalyzer.class);

        Map<String, String[]> parameters = new LinkedHashMap<>();
        parameters.put("actionId", new String[] {"occupation", "body"});
        parameters.put("actionText", new String[] {
                "Deploy Tatooine Occupation from Reserve Deck",
                "Deploy a character"
        });
        parameters.put("cardId", new String[] {"301", null});
        when(decision.getDecisionParameters()).thenReturn(parameters);
        when(gameState.getPlayersLatestTurnNumber("p")).thenReturn(3);
        when(gameState.findCardById(301)).thenReturn(objective);
        when(analyzer.isTwinSunsOccupationPullAction(
                game, "p", objective,
                "Deploy Tatooine Occupation from Reserve Deck"))
                .thenReturn(true);

        for (DeployPhaseScript script : bothBotScripts()) {
            DeployPhaseScript.Result result = script.selectAllowedActions(
                    decision, gameState, game, "p", analyzer);
            assertEquals("[OTHER_CHARACTERS]",
                    result.stepBucketLabels.toString());
            assertEquals("[[body]]", result.stepBuckets.toString());
            assertFalse(result.allowedActionIds.contains("occupation"));
        }
    }

    @Test
    public void noMoneyTableActionsCannotPreemptACharacterDeploy() {
        AwaitingDecision decision = mock(AwaitingDecision.class);
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        PhysicalCard objective = mock(PhysicalCard.class);
        PhysicalCard opponentObjective = mock(PhysicalCard.class);
        ObjectiveAnalyzer analyzer = mock(ObjectiveAnalyzer.class);

        Map<String, String[]> parameters = new LinkedHashMap<>();
        parameters.put("actionId", new String[] {
                "back-gambit", "watto-removal", "body"
        });
        parameters.put("actionText", new String[] {
                "Place card face down on side of table",
                "Place Watto in Used Pile",
                "Deploy a character"
        });
        parameters.put("cardId", new String[] {"120", "121", null});
        when(decision.getDecisionParameters()).thenReturn(parameters);
        when(gameState.getPlayersLatestTurnNumber("p")).thenReturn(3);
        when(game.getGameState()).thenReturn(gameState);
        when(gameState.findCardById(120)).thenReturn(objective);
        when(gameState.findCardById(121)).thenReturn(opponentObjective);
        when(opponentObjective.getZone()).thenReturn(Zone.SIDE_OF_TABLE);
        when(opponentObjective.getOwner()).thenReturn("opponent");
        when(opponentObjective.getBlueprintId(gameState, false))
                .thenReturn("12_180_BACK");
        when(analyzer.isNoMoneyNoPartsBackGambitAction(
                game, "p", objective,
                "Place card face down on side of table"))
                .thenReturn(true);
        assertTrue(NoMoneyNoPartsObjectivePolicy
                .isExactOpponentWattoRemovalAction(
                        game, "p", opponentObjective,
                        "Place Watto in Used Pile"));

        for (DeployPhaseScript script : bothBotScripts()) {
            DeployPhaseScript.Result result = script.selectAllowedActions(
                    decision, gameState, game, "p", analyzer);
            assertEquals("[OTHER_CHARACTERS]",
                    result.stepBucketLabels.toString());
            assertEquals("[[body]]", result.stepBuckets.toString());
            assertFalse(result.allowedActionIds.contains("back-gambit"));
            assertFalse(result.allowedActionIds.contains("watto-removal"));
        }
    }

    @Test
    public void naturalObjectiveDeployActionsRemainCharacterActions() {
        AwaitingDecision decision = mock(AwaitingDecision.class);
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        PhysicalCard survivorSource = mock(PhysicalCard.class);
        PhysicalCard ralltiir = mock(PhysicalCard.class);
        PhysicalCard junkyard = mock(PhysicalCard.class);
        PhysicalCard commandCenter = mock(PhysicalCard.class);
        ObjectiveAnalyzer analyzer = mock(ObjectiveAnalyzer.class);

        Map<String, String[]> parameters = new LinkedHashMap<>();
        parameters.put("actionId", new String[] {
                "survivor", "ralltiir-actor", "watto", "krennic"
        });
        parameters.put("actionText", new String[] {
                "Deploy a Jedi Survivor stacked here",
                "Deploy card from Reserve Deck",
                "Deploy Watto from Reserve Deck",
                "Deploy Krennic from Reserve Deck"
        });
        parameters.put("cardId", new String[] {"226", "7300", "12178", "21616"});
        when(decision.getDecisionParameters()).thenReturn(parameters);
        when(gameState.getPlayersLatestTurnNumber("p")).thenReturn(3);
        when(gameState.findCardById(226)).thenReturn(survivorSource);
        when(gameState.findCardById(7300)).thenReturn(ralltiir);
        when(gameState.findCardById(12178)).thenReturn(junkyard);
        when(gameState.findCardById(21616)).thenReturn(commandCenter);
        when(analyzer.isHiddenPathSurvivorRouteAction(
                game, "p", survivorSource,
                "Deploy a Jedi Survivor stacked here"))
                .thenReturn(true);
        when(analyzer.isRalltiirFrontRouteAction(
                game, "p", ralltiir,
                "Deploy card from Reserve Deck"))
                .thenReturn(true);
        when(analyzer.hasRalltiirFrontSiteRouteCandidateInReserve(
                game, "p", ralltiir)).thenReturn(false);
        when(analyzer.isNoMoneyNoPartsWattoPullAction(
                game, "p", junkyard,
                "Deploy Watto from Reserve Deck"))
                .thenReturn(true);
        when(analyzer.isOnTheVergeKrennicDeployAction(
                game, "p", commandCenter,
                "Deploy Krennic from Reserve Deck"))
                .thenReturn(true);

        for (DeployPhaseScript script : bothBotScripts()) {
            DeployPhaseScript.Result result = script.selectAllowedActions(
                    decision, gameState, game, "p", analyzer);
            assertEquals("[OTHER_CHARACTERS]",
                    result.stepBucketLabels.toString());
            assertEquals("[[survivor, ralltiir-actor, watto, krennic]]",
                    result.stepBuckets.toString());
        }
    }

    @Test
    public void exactShieldRouteCannonUsesTheWeaponBucketForBothBots() {
        AwaitingDecision decision = mock(AwaitingDecision.class);
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        ObjectiveAnalyzer objectiveAnalyzer =
                mock(ObjectiveAnalyzer.class);
        PhysicalCard cannon = mock(PhysicalCard.class);
        PhysicalCard character = mock(PhysicalCard.class);
        var cannonBlueprint = mock(
                com.gempukku.swccgo.game.SwccgCardBlueprint.class);
        var characterBlueprint = mock(
                com.gempukku.swccgo.game.SwccgCardBlueprint.class);

        when(cannon.getBlueprint()).thenReturn(cannonBlueprint);
        when(character.getBlueprint()).thenReturn(characterBlueprint);
        when(cannonBlueprint.getCardCategory()).thenReturn(
                com.gempukku.swccgo.common.CardCategory.WEAPON);
        when(characterBlueprint.getCardCategory()).thenReturn(
                com.gempukku.swccgo.common.CardCategory.CHARACTER);
        when(gameState.findCardById(10)).thenReturn(cannon);
        when(gameState.findCardById(11)).thenReturn(character);
        when(gameState.getPlayersLatestTurnNumber("p")).thenReturn(3);
        when(objectiveAnalyzer.getStrategyCharacterTokens(game, "p"))
                .thenReturn(Collections.emptySet());
        when(objectiveAnalyzer.isStrategyKeyCharacter(
                game, "p", character)).thenReturn(false);
        when(objectiveAnalyzer
                .isShieldMainGeneratorPriorityCannonDeploy(
                    game, "p", cannon)).thenReturn(true);

        Map<String, String[]> parameters = new LinkedHashMap<>();
        parameters.put("actionId",
                new String[] {"cannon", "character"});
        parameters.put("actionText",
                new String[] {"Deploy", "Deploy"});
        parameters.put("cardId", new String[] {"10", "11"});
        when(decision.getDecisionParameters()).thenReturn(parameters);

        for (DeployPhaseScript script : new DeployPhaseScript[] {
                new com.gempukku.swccgo.ai.models.rando.strategy.DeployPhaseScript(),
                new com.gempukku.swccgo.ai.models.chosenone.strategy.DeployPhaseScript()
        }) {
            DeployPhaseScript.Result result =
                    script.selectAllowedActions(
                        decision, gameState, game, "p",
                        objectiveAnalyzer);
            assertEquals(
                    "[OTHER_CHARACTERS, WEAPONS]",
                    result.stepBucketLabels.toString());
            assertEquals("[[character], [cannon]]",
                    result.stepBuckets.toString());
        }

        when(objectiveAnalyzer
                .isShieldMainGeneratorPriorityCannonDeploy(
                    game, "p", cannon)).thenReturn(false);
        DeployPhaseScript.Result ordinary =
                new com.gempukku.swccgo.ai.models.rando.strategy.DeployPhaseScript()
                    .selectAllowedActions(
                        decision, gameState, game, "p",
                        objectiveAnalyzer);
        assertEquals("[OTHER_CHARACTERS, WEAPONS]",
                ordinary.stepBucketLabels.toString());
    }

    @Test
    public void objectiveLocationPullRunsFirstUntilItsTypedRouteIsExhausted() {
        AwaitingDecision decision = mock(AwaitingDecision.class);
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        PhysicalCard objective = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        ObjectiveAnalyzer analyzer = mock(ObjectiveAnalyzer.class);

        when(objective.getBlueprint()).thenReturn(blueprint);
        when(gameState.findCardById(109)).thenReturn(objective);
        when(gameState.getPlayersLatestTurnNumber("p")).thenReturn(3);
        when(gameState.getSide("p")).thenReturn(Side.LIGHT);
        when(analyzer.getStrategyCharacterTokens(game, "p"))
                .thenReturn(Collections.emptySet());
        when(analyzer.isFlipped()).thenReturn(false);
        when(analyzer.usesObjectiveLocationPullSequence())
                .thenReturn(true);
        when(analyzer.isCurrentObjectiveSourceCard(objective))
                .thenReturn(true);

        Map<String, String[]> parameters = new LinkedHashMap<>();
        parameters.put("actionId", new String[] {"qmc-pull", "body"});
        parameters.put("actionText", new String[] {
                "Deploy a site or cloud sector to Bespin from Reserve Deck",
                "Deploy a character"
        });
        parameters.put("cardId", new String[] {"109", null});
        when(decision.getDecisionParameters()).thenReturn(parameters);

        TestDeployPhaseScript script = new TestDeployPhaseScript();
        when(analyzer.hasObjectiveLocationRouteCandidateInReserve(
                game, "p", objective)).thenReturn(true);
        DeployPhaseScript.Result routeOpen = script.selectAllowedActions(
                decision, gameState, game, "p", analyzer);
        assertEquals("[LOCATIONS, OTHER_CHARACTERS]",
                routeOpen.stepBucketLabels.toString());
        assertEquals("[[qmc-pull], [body]]",
                routeOpen.stepBuckets.toString());

        when(analyzer.hasObjectiveLocationRouteCandidateInReserve(
                game, "p", objective)).thenReturn(false);
        DeployPhaseScript.Result routeExhausted = script.selectAllowedActions(
                decision, gameState, game, "p", analyzer);
        assertEquals("[OTHER_CHARACTERS]",
                routeExhausted.stepBucketLabels.toString());
        assertEquals("[[body]]", routeExhausted.stepBuckets.toString());
    }

    @Test
    public void unmodeledObjectiveNeverLosesItsNativeLocationAction() {
        AwaitingDecision decision = mock(AwaitingDecision.class);
        GameState gameState = mock(GameState.class);
        SwccgGame game = mock(SwccgGame.class);
        PhysicalCard objective = mock(PhysicalCard.class);
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        ObjectiveAnalyzer analyzer = mock(ObjectiveAnalyzer.class);

        when(objective.getBlueprint()).thenReturn(blueprint);
        when(gameState.findCardById(209)).thenReturn(objective);
        when(gameState.getPlayersLatestTurnNumber("p")).thenReturn(3);
        when(gameState.getSide("p")).thenReturn(Side.LIGHT);
        when(analyzer.getStrategyCharacterTokens(game, "p"))
                .thenReturn(Collections.emptySet());
        when(analyzer.isFlipped()).thenReturn(false);
        when(analyzer.isCurrentObjectiveSourceCard(objective))
                .thenReturn(true);
        when(analyzer.usesObjectiveLocationPullSequence())
                .thenReturn(false);
        when(analyzer.hasMissingPreFlipRequiredLocationInReserve(
                game, "p")).thenReturn(false);

        Map<String, String[]> parameters = new LinkedHashMap<>();
        parameters.put("actionId", new String[] {"native-location", "body"});
        parameters.put("actionText", new String[] {
                "Deploy a location from Reserve Deck",
                "Deploy a character"
        });
        parameters.put("cardId", new String[] {"209", null});
        when(decision.getDecisionParameters()).thenReturn(parameters);

        DeployPhaseScript.Result result = new TestDeployPhaseScript()
                .selectAllowedActions(
                        decision, gameState, game, "p", analyzer);
        assertEquals("[LOCATIONS, OTHER_CHARACTERS]",
                result.stepBucketLabels.toString());
        assertEquals("[[native-location], [body]]",
                result.stepBuckets.toString());
    }

    @SuppressWarnings("unchecked")
    private static Set<Object> resolveSteps(Object script, String text) throws Exception {
        Method method = findMethod(script.getClass(),
                "resolveSteps", String.class, String.class,
                com.gempukku.swccgo.game.state.GameState.class,
                com.gempukku.swccgo.game.SwccgGame.class,
                String.class, Set.class);
        method.setAccessible(true);
        return (Set<Object>) method.invoke(script, text, null,
                null, null, "p", Collections.emptySet());
    }

    private static String classify(Object script, String text) throws Exception {
        Method method = findMethod(script.getClass(),
                "classifyByKeywords", String.class);
        method.setAccessible(true);
        Object step = method.invoke(script, text);
        return step == null ? null : step.toString();
    }

    private static DeployPhaseScript[] bothBotScripts() {
        return new DeployPhaseScript[] {
                new com.gempukku.swccgo.ai.models.rando.strategy.DeployPhaseScript(),
                new com.gempukku.swccgo.ai.models.chosenone.strategy.DeployPhaseScript()
        };
    }

    private static Method findMethod(Class<?> type, String name, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredMethod(name, parameterTypes);
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchMethodException(name);
    }

    private static final class TestDeployPhaseScript extends DeployPhaseScript {
        private TestDeployPhaseScript() {
            super(org.apache.logging.log4j.LogManager.getLogger(
                    TestDeployPhaseScript.class));
        }

        @Override
        protected String sourceCardFullGameText(
                SwccgCardBlueprint blueprint, Side side) {
            return "May deploy a site or cloud sector to Bespin from Reserve Deck.";
        }

        @Override
        protected java.util.List<String> parseSourceCardPullTargets(
                String gameText) {
            return java.util.List.of("a site or cloud sector");
        }
    }
}
