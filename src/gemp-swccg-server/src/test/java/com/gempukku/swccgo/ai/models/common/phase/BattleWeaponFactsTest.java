package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import org.junit.Test;

import java.lang.reflect.Proxy;

import static org.junit.Assert.assertEquals;

/** Permanent-weapon ownership, hit capability, and zero-forfeit remain separate. */
public class BattleWeaponFactsTest {

    @Test
    public void truePermanentWeaponCanHitAndSetForfeitToZero() {
        assertEquals(new BattleWeaponFacts(true, true, true),
                BattleWeaponFacts.from(card("1_1", true)));
    }

    @Test
    public void nonHitPermanentWeaponRetainsOwnershipOnly() {
        assertEquals(new BattleWeaponFacts(true, false, false),
                BattleWeaponFacts.from(card("109_6", true)));
    }

    @Test
    public void iconlessCharacterCannotInheritHitEconomics() {
        assertEquals(new BattleWeaponFacts(false, false, false),
                BattleWeaponFacts.from(card("1_1", false)));
        assertEquals(new BattleWeaponFacts(false, false, false),
                BattleWeaponFacts.from(null));
    }

    private static PhysicalCard card(String blueprintId, boolean permanentWeapon) {
        SwccgCardBlueprint blueprint = (SwccgCardBlueprint) Proxy.newProxyInstance(
                SwccgCardBlueprint.class.getClassLoader(),
                new Class<?>[]{SwccgCardBlueprint.class},
                (proxy, method, args) -> {
                    if ("hasIcon".equals(method.getName())) {
                        return permanentWeapon && args != null && args.length == 1
                                && args[0] == Icon.PERMANENT_WEAPON;
                    }
                    return defaultValue(method.getReturnType());
                });
        return (PhysicalCard) Proxy.newProxyInstance(
                PhysicalCard.class.getClassLoader(),
                new Class<?>[]{PhysicalCard.class},
                (proxy, method, args) -> {
                    if ("getBlueprint".equals(method.getName())) {
                        return blueprint;
                    }
                    if ("getBlueprintId".equals(method.getName())) {
                        return blueprintId;
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0f;
        if (type == double.class) return 0d;
        return null;
    }
}
