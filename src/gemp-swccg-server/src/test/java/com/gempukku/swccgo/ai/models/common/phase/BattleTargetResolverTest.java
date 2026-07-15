package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.game.PhysicalCard;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class BattleTargetResolverTest {

    @Test
    public void stockCardIdResolvesGenericBattleText() {
        PhysicalCard north = location(101, "North Docking Bay");
        PhysicalCard south = location(202, "South Docking Bay");

        assertSame(south, BattleTargetResolver.resolve(
                List.of(north, south), "202", "Initiate battle"));
    }

    @Test
    public void alignedCardIdDominatesConflictingDisplayText() {
        PhysicalCard north = location(101, "North Docking Bay");
        PhysicalCard south = location(202, "South Docking Bay");

        assertSame(south, BattleTargetResolver.resolve(
                List.of(north, south), "202", "Initiate battle at North Docking Bay"));
    }

    @Test
    public void legacyNamedTextRemainsACompatibleFallback() {
        PhysicalCard north = location(101, "North Docking Bay");
        PhysicalCard south = location(202, "South Docking Bay");

        assertSame(north, BattleTargetResolver.resolve(
                List.of(north, south), null, "Initiate battle at North Docking Bay"));
        assertNull(BattleTargetResolver.resolve(
                List.of(north, south), "999", "Initiate battle"));
    }

    private static PhysicalCard location(int cardId, String title) {
        return (PhysicalCard) Proxy.newProxyInstance(
                PhysicalCard.class.getClassLoader(),
                new Class<?>[]{PhysicalCard.class},
                (proxy, method, args) -> {
                    if ("getCardId".equals(method.getName())) return cardId;
                    if ("getTitle".equals(method.getName())) return title;
                    if (method.getReturnType() == boolean.class) return false;
                    if (method.getReturnType() == int.class) return 0;
                    if (method.getReturnType() == float.class) return 0f;
                    return null;
                });
    }
}
