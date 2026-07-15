package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.CardType;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgBuiltInCardBlueprint;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class BattleWeaponProfileTest {

    @Test
    public void permanentLightsaberUsesRetainedFivePointWeight() {
        Fixture fixture = fixture(permanentWeapon(true), List.of(), Set.of());
        assertEquals(new BattleWeaponProfile(5f, true),
                BattleWeaponProfile.assess(fixture.game(), fixture.gameState(), fixture.character()));
    }

    @Test
    public void permanentNonLightsaberUsesRetainedThreePointWeight() {
        Fixture fixture = fixture(permanentWeapon(false), List.of(), Set.of());
        assertEquals(new BattleWeaponProfile(3f, true),
                BattleWeaponProfile.assess(fixture.game(), fixture.gameState(), fixture.character()));
    }

    @Test
    public void activeAttachedWeaponCountsButInactiveWeaponDoesNot() {
        PhysicalCard active = attachedWeapon(true);
        PhysicalCard inactive = attachedWeapon(false);
        Fixture fixture = fixture(null, List.of(active, inactive), Set.of(active));

        assertEquals(new BattleWeaponProfile(5f, true),
                BattleWeaponProfile.assess(fixture.game(), fixture.gameState(), fixture.character()));
    }

    @Test
    public void missingPermanentWeaponRepresentsDisarmedCharacter() {
        Fixture fixture = fixture(null, List.of(), Set.of());
        assertEquals(new BattleWeaponProfile(0f, false),
                BattleWeaponProfile.assess(fixture.game(), fixture.gameState(), fixture.character()));
    }

    private static Fixture fixture(
            SwccgBuiltInCardBlueprint permanentWeapon,
            List<PhysicalCard> attachments,
            Set<PhysicalCard> activeAttachments) {
        PhysicalCard character = physicalCard(false);
        ModifiersQuerying querying = (ModifiersQuerying) Proxy.newProxyInstance(
                ModifiersQuerying.class.getClassLoader(),
                new Class<?>[]{ModifiersQuerying.class},
                (proxy, method, args) -> {
                    if ("getPermanentWeapon".equals(method.getName())) return permanentWeapon;
                    if ("getCardTypes".equals(method.getName())) return Set.of(CardType.WEAPON);
                    if ("hasKeyword".equals(method.getName())) {
                        return args != null && args.length == 3
                                && args[2] == Keyword.LIGHTSABER
                                && "lightsaber".equals(String.valueOf(args[1]));
                    }
                    return defaultValue(method.getReturnType());
                });
        SwccgGame game = (SwccgGame) Proxy.newProxyInstance(
                SwccgGame.class.getClassLoader(),
                new Class<?>[]{SwccgGame.class},
                (proxy, method, args) -> {
                    if ("getModifiersQuerying".equals(method.getName())) return querying;
                    return defaultValue(method.getReturnType());
                });
        GameState gameState = new GameState(game) {
            @Override
            public List<PhysicalCard> getAttachedCards(PhysicalCard card) {
                return card == character ? attachments : List.of();
            }

            @Override
            public boolean isCardInPlayActive(
                    PhysicalCard card,
                    boolean includeExcludedFromBattle,
                    boolean includeUndercover,
                    boolean includeCaptives,
                    boolean includeConcealed,
                    boolean includeWeaponsForStealing,
                    boolean includeMissing,
                    boolean includeBinaryOff,
                    boolean includeSuspended) {
                return activeAttachments.stream().anyMatch(active -> active == card);
            }
        };
        return new Fixture(game, gameState, character);
    }

    private static PhysicalCard attachedWeapon(boolean lightsaber) {
        return physicalCard(lightsaber);
    }

    private static PhysicalCard physicalCard(boolean lightsaber) {
        SwccgCardBlueprint blueprint = (SwccgCardBlueprint) Proxy.newProxyInstance(
                SwccgCardBlueprint.class.getClassLoader(),
                new Class<?>[]{SwccgCardBlueprint.class},
                (proxy, method, args) -> defaultValue(method.getReturnType()));
        return (PhysicalCard) Proxy.newProxyInstance(
                PhysicalCard.class.getClassLoader(),
                new Class<?>[]{PhysicalCard.class},
                (proxy, method, args) -> {
                    if ("getBlueprint".equals(method.getName())) return blueprint;
                    if ("isDejarikHologramAtHolosite".equals(method.getName())) return false;
                    if ("toString".equals(method.getName())) return lightsaber ? "lightsaber" : "weapon";
                    return defaultValue(method.getReturnType());
                });
    }

    private static SwccgBuiltInCardBlueprint permanentWeapon(boolean lightsaber) {
        return (SwccgBuiltInCardBlueprint) Proxy.newProxyInstance(
                SwccgBuiltInCardBlueprint.class.getClassLoader(),
                new Class<?>[]{SwccgBuiltInCardBlueprint.class},
                (proxy, method, args) -> {
                    if ("isWeapon".equals(method.getName())) return true;
                    if ("hasKeyword".equals(method.getName())) {
                        return lightsaber && args != null && args.length == 1
                                && args[0] == Keyword.LIGHTSABER;
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

    private record Fixture(SwccgGame game, GameState gameState, PhysicalCard character) {
    }
}
