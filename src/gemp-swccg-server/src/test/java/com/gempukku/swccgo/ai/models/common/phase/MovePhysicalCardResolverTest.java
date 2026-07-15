package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.game.PhysicalCard;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class MovePhysicalCardResolverTest {

    @Test
    public void skipsOffTableCopyAndReturnsOnTablePhysicalMover() {
        PhysicalCard origin = card("site", "dark", null);
        PhysicalCard offTable = card("mover", "dark", null);
        PhysicalCard opponentCopy = card("mover", "light", origin);
        PhysicalCard onTable = card("mover", "dark", origin);

        MovePhysicalCardResolver.ResolvedMover resolved =
                MovePhysicalCardResolver.resolveOnTable(
                        List.of(offTable, opponentCopy, onTable), "dark", "mover");

        assertSame(onTable, resolved.card());
        assertSame(origin, resolved.origin());
    }

    @Test
    public void preservesFirstOnTableMatchInStockIterationOrder() {
        PhysicalCard firstOrigin = card("site-a", "dark", null);
        PhysicalCard secondOrigin = card("site-b", "dark", null);
        PhysicalCard first = card("mover", "dark", firstOrigin);
        PhysicalCard second = card("mover", "dark", secondOrigin);

        MovePhysicalCardResolver.ResolvedMover resolved =
                MovePhysicalCardResolver.resolveOnTable(
                        List.of(first, second), "dark", "mover");

        assertSame(first, resolved.card());
        assertSame(firstOrigin, resolved.origin());
    }

    @Test
    public void returnsNullWhenOnlyMatchingCopyIsOffTable() {
        assertNull(MovePhysicalCardResolver.resolveOnTable(
                List.of(card("mover", "dark", null)), "dark", "mover"));
    }

    @Test
    public void rejectsIncompleteLookupFacts() {
        assertNull(MovePhysicalCardResolver.resolveOnTable(null, "dark", "mover"));
        assertNull(MovePhysicalCardResolver.resolveOnTable(List.of(), null, "mover"));
        assertNull(MovePhysicalCardResolver.resolveOnTable(List.of(), "dark", null));
    }

    private static PhysicalCard card(String blueprintId, String owner, PhysicalCard origin) {
        return (PhysicalCard) Proxy.newProxyInstance(
                PhysicalCard.class.getClassLoader(),
                new Class<?>[]{PhysicalCard.class},
                (proxy, method, args) -> {
                    if ("getBlueprintId".equals(method.getName())) return blueprintId;
                    if ("getOwner".equals(method.getName())) return owner;
                    if ("getAtLocation".equals(method.getName())) return origin;
                    if (method.getReturnType() == boolean.class) return false;
                    if (method.getReturnType() == int.class) return 0;
                    if (method.getReturnType() == float.class) return 0f;
                    return null;
                });
    }
}
