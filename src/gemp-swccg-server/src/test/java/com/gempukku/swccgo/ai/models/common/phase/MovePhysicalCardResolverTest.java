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
    public void ambiguousBlueprintOnlyLookupFailsOpen() {
        PhysicalCard firstOrigin = card("site-a", "dark", null);
        PhysicalCard secondOrigin = card("site-b", "dark", null);
        PhysicalCard first = card("mover", "dark", firstOrigin);
        PhysicalCard second = card("mover", "dark", secondOrigin);

        assertNull(MovePhysicalCardResolver.resolveOnTable(
                List.of(first, second), "dark", "mover"));
    }

    @Test
    public void exactPhysicalCardIdDisambiguatesDuplicateBlueprints() {
        PhysicalCard firstOrigin = card("site-a", "dark", null);
        PhysicalCard secondOrigin = card("site-b", "dark", null);
        PhysicalCard first = card("mover", "dark", firstOrigin, 11);
        PhysicalCard second = card("mover", "dark", secondOrigin, 22);

        MovePhysicalCardResolver.ResolvedMover resolved =
                MovePhysicalCardResolver.resolveOnTable(
                        List.of(first, second), "dark", "mover", 22);

        assertSame(second, resolved.card());
        assertSame(secondOrigin, resolved.origin());
    }

    @Test
    public void exactPhysicalCardIdWorksWithoutBlueprintHint() {
        PhysicalCard origin = card("mover", "dark", null);
        PhysicalCard mover = card("mover", "dark", origin, 22);

        MovePhysicalCardResolver.ResolvedMover resolved =
                MovePhysicalCardResolver.resolveOnTable(
                        List.of(mover), "dark", null, 22);

        assertSame(mover, resolved.card());
        assertSame(origin, resolved.origin());
        assertNull(MovePhysicalCardResolver.resolveOnTable(
                List.of(mover), "dark", "wrong-blueprint", 22));
    }

    @Test
    public void attachedCopyCountsForAmbiguityAndExactIdResolution() {
        PhysicalCard firstOrigin = card("site-a", "dark", null);
        PhysicalCard carrierOrigin = card("site-b", "dark", null);
        PhysicalCard carrier = card(
                "carrier", "dark", carrierOrigin, 30);
        PhysicalCard first = card(
                "mover", "dark", firstOrigin, 11);
        PhysicalCard attached = card(
                "mover", "dark", null, 22, carrier);

        assertNull(MovePhysicalCardResolver.resolveOnTable(
                List.of(first, carrier, attached),
                "dark", "mover"));

        MovePhysicalCardResolver.ResolvedMover resolved =
                MovePhysicalCardResolver.resolveOnTable(
                        List.of(first, carrier, attached),
                        "dark", "mover", 22);
        assertSame(attached, resolved.card());
        assertSame(carrierOrigin, resolved.origin());
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
        return card(blueprintId, owner, origin, 0);
    }

    private static PhysicalCard card(
            String blueprintId, String owner,
            PhysicalCard origin, int cardId) {
        return card(
                blueprintId, owner,
                origin, cardId, null);
    }

    private static PhysicalCard card(
            String blueprintId, String owner,
            PhysicalCard origin, int cardId,
            PhysicalCard attachedRoot) {
        return (PhysicalCard) Proxy.newProxyInstance(
                PhysicalCard.class.getClassLoader(),
                new Class<?>[]{PhysicalCard.class},
                (proxy, method, args) -> {
                    if ("getBlueprintId".equals(method.getName())) return blueprintId;
                    if ("getOwner".equals(method.getName())) return owner;
                    if ("getAtLocation".equals(method.getName())) return origin;
                    if ("getCardAttachedToAtLocation".equals(
                            method.getName())) {
                        return attachedRoot;
                    }
                    if ("getCardId".equals(method.getName())) return cardId;
                    if (method.getReturnType() == boolean.class) return false;
                    if (method.getReturnType() == int.class) return 0;
                    if (method.getReturnType() == float.class) return 0f;
                    return null;
                });
    }
}
