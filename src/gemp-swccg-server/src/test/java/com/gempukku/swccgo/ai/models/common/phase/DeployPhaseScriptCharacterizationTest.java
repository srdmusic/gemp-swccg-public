package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.strategy.ObjectiveAnalyzer;
import com.gempukku.swccgo.game.PhysicalCard;
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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DeployPhaseScriptCharacterizationTest {
    @Test
    public void randoBucketOrderIsFrozen() {
        assertEquals("[LOCATIONS, KEY_CHARACTERS, OTHER_CHARACTERS, WEAPONS, DEVICES]",
                java.util.Arrays.toString(
                        com.gempukku.swccgo.ai.models.rando.strategy.DeployPhaseScript.Step.values()));
    }

    @Test
    public void chosenBucketOrderIsFrozen() {
        assertEquals("[LOCATIONS, KEY_CHARACTERS, OTHER_CHARACTERS, WEAPONS, DEVICES]",
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
    public void bothBotsPutMissingFlipGateActorUploadBeforeOtherCharacters() throws Exception {
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
                "[KEY_CHARACTERS:[[upload], [security]], "
                        + "KEY_CHARACTERS:[[upload], [security]]]",
                java.util.Arrays.toString(new Object[] {
                        rando.step + ":" + rando.stepBuckets,
                        chosen.step + ":" + chosen.stepBuckets
                }));
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
}
