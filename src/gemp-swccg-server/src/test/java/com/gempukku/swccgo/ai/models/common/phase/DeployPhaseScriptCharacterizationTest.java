package com.gempukku.swccgo.ai.models.common.phase;

import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
