package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ObjectiveAnalyzerSideParsingTest {
    private static final String PLAYER_ID = "player";

    @Test
    public void randoUsesStableFrontIdentityAndRealBackTextWhileFlipped() throws Exception {
        verifyFlippedAnalyzer(new com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer());
    }

    @Test
    public void chosenOneUsesStableFrontIdentityAndRealBackTextWhileFlipped() throws Exception {
        verifyFlippedAnalyzer(new com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer());
    }

    private static void verifyFlippedAnalyzer(Object analyzer) throws Exception {
        SwccgCardBlueprint front = blueprint(
                "Front Objective",
                "Flip this card if you occupy Mos Espa.");
        SwccgCardBlueprint back = blueprint(
                "Back Objective",
                "Flip this card if you do not occupy Mos Espa.");
        PhysicalCard objective = card(back, front, true);
        GameState gameState = new GameState() {
            @Override
            public List<PhysicalCard> getAllPermanentCards() {
                return List.of(objective);
            }
        };

        Method analyze = analyzer.getClass().getMethod(
                "analyze", SwccgGame.class, String.class, Side.class);
        analyze.invoke(analyzer, game(gameState), PLAYER_ID, Side.LIGHT);

        assertTrue((Boolean) invoke(analyzer, "isAnalyzed"));
        assertTrue((Boolean) invoke(analyzer, "isFlipped"));
        assertEquals("Front Objective", invoke(analyzer, "getObjectiveTitle"));
        assertEquals("you do not occupy Mos Espa", invoke(analyzer, "getFlipBackConditionText"));
        assertTrue((Boolean) invoke(analyzer, "flipBackRequiresOccupy"));
        assertTrue((Boolean) invoke(analyzer, "isFlipBackProtectionLocation", "Mos Espa"));
    }

    private static Object invoke(Object target, String methodName, Object... args) throws Exception {
        Class<?>[] parameterTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
            parameterTypes[i] = args[i].getClass();
        }
        return target.getClass().getMethod(methodName, parameterTypes).invoke(target, args);
    }

    private static SwccgGame game(GameState gameState) {
        return (SwccgGame) Proxy.newProxyInstance(
                SwccgGame.class.getClassLoader(),
                new Class<?>[]{SwccgGame.class},
                (proxy, method, args) -> {
                    if ("getGameState".equals(method.getName())) return gameState;
                    return defaultValue(method.getReturnType());
                });
    }

    private static PhysicalCard card(
            SwccgCardBlueprint current,
            SwccgCardBlueprint opposite,
            boolean flipped) {
        return (PhysicalCard) Proxy.newProxyInstance(
                PhysicalCard.class.getClassLoader(),
                new Class<?>[]{PhysicalCard.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getBlueprint": return current;
                        case "getOtherSideBlueprint": return opposite;
                        case "getBlueprintId": return "test_front";
                        case "getOwner": return PLAYER_ID;
                        case "getZone": return Zone.SIDE_OF_TABLE;
                        case "isFlipped": return flipped;
                        default: return defaultValue(method.getReturnType());
                    }
                });
    }

    private static SwccgCardBlueprint blueprint(String title, String gameText) {
        return (SwccgCardBlueprint) Proxy.newProxyInstance(
                SwccgCardBlueprint.class.getClassLoader(),
                new Class<?>[]{SwccgCardBlueprint.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getTitle": return title;
                        case "getGameText": return gameText;
                        case "getCardCategory": return CardCategory.OBJECTIVE;
                        default: return defaultValue(method.getReturnType());
                    }
                });
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0f;
        if (type == double.class) return 0d;
        if (type == char.class) return '\0';
        return null;
    }
}
