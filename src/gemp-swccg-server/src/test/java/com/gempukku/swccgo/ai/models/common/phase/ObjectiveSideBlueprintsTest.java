package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import org.junit.Test;

import java.lang.reflect.Proxy;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class ObjectiveSideBlueprintsTest {

    @Test
    public void unflippedCardKeepsCurrentBlueprintAsFront() {
        SwccgCardBlueprint front = blueprint();
        SwccgCardBlueprint back = blueprint();

        ObjectiveSideBlueprints sides =
                ObjectiveSideBlueprints.resolve(card(front, back, false));

        assertSame(front, sides.front());
        assertSame(back, sides.back());
    }

    @Test
    public void flippedCardRestoresOppositeBlueprintAsFront() {
        SwccgCardBlueprint front = blueprint();
        SwccgCardBlueprint back = blueprint();

        ObjectiveSideBlueprints sides =
                ObjectiveSideBlueprints.resolve(card(back, front, true));

        assertSame(front, sides.front());
        assertSame(back, sides.back());
    }

    @Test
    public void singleSidedCardFallsBackWithoutDroppingFrontAnalysis() {
        SwccgCardBlueprint front = blueprint();

        ObjectiveSideBlueprints sides =
                ObjectiveSideBlueprints.resolve(card(front, null, false));

        assertSame(front, sides.front());
        assertNull(sides.back());
    }

    @Test
    public void missingCurrentBlueprintCannotBeResolved() {
        assertNull(ObjectiveSideBlueprints.resolve(card(null, blueprint(), false)));
    }

    private static PhysicalCard card(
            SwccgCardBlueprint current,
            SwccgCardBlueprint opposite,
            boolean flipped) {
        return (PhysicalCard) Proxy.newProxyInstance(
                PhysicalCard.class.getClassLoader(),
                new Class<?>[]{PhysicalCard.class},
                (proxy, method, args) -> {
                    if ("getBlueprint".equals(method.getName())) return current;
                    if ("getOtherSideBlueprint".equals(method.getName())) return opposite;
                    if ("isFlipped".equals(method.getName())) return flipped;
                    if (method.getReturnType() == boolean.class) return false;
                    if (method.getReturnType() == int.class) return 0;
                    if (method.getReturnType() == float.class) return 0f;
                    return null;
                });
    }

    private static SwccgCardBlueprint blueprint() {
        return (SwccgCardBlueprint) Proxy.newProxyInstance(
                SwccgCardBlueprint.class.getClassLoader(),
                new Class<?>[]{SwccgCardBlueprint.class},
                (proxy, method, args) -> {
                    if (method.getReturnType() == boolean.class) return false;
                    if (method.getReturnType() == int.class) return 0;
                    if (method.getReturnType() == float.class) return 0f;
                    return null;
                });
    }
}
