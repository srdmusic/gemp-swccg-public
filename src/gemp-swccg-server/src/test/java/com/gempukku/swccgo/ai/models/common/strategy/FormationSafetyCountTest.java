package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import org.junit.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;

/**
 * BATCH1b correction fixtures (Codex m00225 #2 / m00262 gate): the empty-site
 * presence test counts friendly non-undercover CHARACTERS — a present power-0
 * friendly is presence, an undercover-only site is not. Pure JUnit with
 * reflective proxy fakes; the helper takes the card collection directly so no
 * GameState is needed.
 */
public class FormationSafetyCountTest {

    private static final String ME = "dark_player";
    private static final String OPP = "light_player";

    /** Reflective PhysicalCard fake exposing only what the helper reads. */
    private static PhysicalCard card(final CardCategory category, final String owner, final boolean undercover) {
        final SwccgCardBlueprint bp = (SwccgCardBlueprint) Proxy.newProxyInstance(
            SwccgCardBlueprint.class.getClassLoader(),
            new Class<?>[]{SwccgCardBlueprint.class},
            new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) {
                    if ("getCardCategory".equals(method.getName())) return category;
                    throw new UnsupportedOperationException("test fake: " + method.getName());
                }
            });
        return (PhysicalCard) Proxy.newProxyInstance(
            PhysicalCard.class.getClassLoader(),
            new Class<?>[]{PhysicalCard.class},
            new InvocationHandler() {
                @Override
                public Object invoke(Object proxy, Method method, Object[] args) {
                    switch (method.getName()) {
                        case "getBlueprint": return bp;
                        case "getOwner": return owner;
                        case "isUndercover": return undercover;
                        default: throw new UnsupportedOperationException("test fake: " + method.getName());
                    }
                }
            });
    }

    @Test
    public void powerZeroFriendlyCharacter_isPresence() {
        // The m00225 case: a friendly character with printed power 0 must count —
        // total-power<=0 misread it as an empty site and fired the -800 penalty.
        // (The helper never reads power at all; category+owner+cover only.)
        int n = FormationSafety.countFriendlyNonUndercoverCharacters(
            Collections.singletonList(card(CardCategory.CHARACTER, ME, false)), ME);
        assertEquals(1, n);
    }

    @Test
    public void undercoverOnly_isNotPresence() {
        int n = FormationSafety.countFriendlyNonUndercoverCharacters(
            Collections.singletonList(card(CardCategory.CHARACTER, ME, true)), ME);
        assertEquals(0, n);
    }

    @Test
    public void opponentCharacters_doNotCount() {
        int n = FormationSafety.countFriendlyNonUndercoverCharacters(
            Arrays.asList(card(CardCategory.CHARACTER, OPP, false),
                          card(CardCategory.CHARACTER, OPP, false)), ME);
        assertEquals(0, n);
    }

    @Test
    public void nonCharacterFriendlies_doNotCount() {
        int n = FormationSafety.countFriendlyNonUndercoverCharacters(
            Arrays.asList(card(CardCategory.VEHICLE, ME, false),
                          card(CardCategory.EFFECT, ME, false)), ME);
        assertEquals(0, n);
    }

    @Test
    public void mixedBoard_countsOnlyFriendlyOvertCharacters() {
        int n = FormationSafety.countFriendlyNonUndercoverCharacters(
            Arrays.asList(
                card(CardCategory.CHARACTER, ME, false),   // counts
                card(CardCategory.CHARACTER, ME, true),    // undercover — no
                card(CardCategory.CHARACTER, OPP, false),  // enemy — no
                card(CardCategory.VEHICLE, ME, false),     // vehicle — no
                card(CardCategory.CHARACTER, ME, false)),  // counts
            ME);
        assertEquals(2, n);
    }

    @Test
    public void emptyOrNull_isZero() {
        assertEquals(0, FormationSafety.countFriendlyNonUndercoverCharacters(Collections.<PhysicalCard>emptyList(), ME));
        assertEquals(0, FormationSafety.countFriendlyNonUndercoverCharacters(null, ME));
        assertEquals(0, FormationSafety.countFriendlyNonUndercoverCharacters(
            Collections.singletonList(card(CardCategory.CHARACTER, ME, false)), null));
    }
}
