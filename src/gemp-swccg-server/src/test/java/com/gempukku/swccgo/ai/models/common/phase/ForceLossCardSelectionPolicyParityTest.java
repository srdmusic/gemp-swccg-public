package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.BattleState;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static com.gempukku.swccgo.ai.models.common.strategy.ObjectiveAnalyzer.ObjectiveProgressCandidateRole.REQUIRED_ACTOR;
import static com.gempukku.swccgo.ai.models.common.strategy.ObjectiveAnalyzer.ObjectiveProgressCandidateRole.REQUIRED_LOCATION;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ForceLossCardSelectionPolicyParityTest {

    private static final Map<GameState, SwccgGame> GAME_BY_STATE =
            new java.util.IdentityHashMap<>();

    @Test
    public void standaloneRouteKeepsExactScoresReasonsAndBotParity() {
        PhysicalCard handCharacter = card(
                "Hand Character", Zone.HAND, CardCategory.CHARACTER);
        PhysicalCard reserveEffect = card(
                "Reserve Effect", Zone.RESERVE_DECK, CardCategory.EFFECT);
        Map<Integer, PhysicalCard> candidates = new LinkedHashMap<>();
        candidates.put(7, handCharacter);
        candidates.put(8, reserveEffect);

        List<PhysicalCard> hand = new ArrayList<>();
        hand.add(handCharacter);
        hand.add(card("Hand Two", Zone.HAND, CardCategory.EFFECT));
        hand.add(card("Hand Three", Zone.HAND, CardCategory.EFFECT));
        hand.add(card("Hand Four", Zone.HAND, CardCategory.EFFECT));
        hand.add(card("Hand Five", Zone.HAND, CardCategory.EFFECT));
        GameState gameState = gameState(candidates, hand, List.of(), 11, 0, 2);

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.CardSelectionEvaluator()
                .evaluate(randoContext(gameState, "Choose Force to lose",
                        List.of("7", "8"), Phase.BATTLE));
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.CardSelectionEvaluator()
                .evaluate(chosenContext(gameState, "Choose Force to lose",
                        List.of("7", "8"), Phase.BATTLE));

        assertActionParity(rando, chosen);
        assertEquals(2, rando.size());
        assertBits(150.0f, action(rando, "7").getScore());
        assertBits(450.0f, action(rando, "8").getScore());
        assertReasons(action(rando, "7").getReasoning(),
                "V153 ZONE (HAND, lifeForce=11, protectChars=true) (+100.0)");
        assertReasons(action(rando, "8").getReasoning(),
                "V153 ZONE (RESERVE_DECK, lifeForce=11, protectChars=true) (+400.0)");
    }

    @Test
    public void combinedPromptPrecedesStandaloneAndKeepsPolicyOrderOncePerBot() {
        PhysicalCard usedPriority = card("Houjix", Zone.USED_PILE,
                CardCategory.INTERRUPT);
        Map<Integer, PhysicalCard> candidates = Map.of(9, usedPriority);
        GameState gameState = gameState(
                candidates, List.of(), List.of(usedPriority), 5, 0, 2);
        String prompt = "Choose Force to lose or choose a card from battle to forfeit";

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.CardSelectionEvaluator()
                .evaluate(randoContext(gameState, prompt, List.of("9"), Phase.BATTLE));
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.CardSelectionEvaluator()
                .evaluate(chosenContext(gameState, prompt, List.of("9"), Phase.BATTLE));

        assertActionParity(rando, chosen);
        assertEquals(1, rando.size());
        assertBits(750.0f, rando.get(0).getScore());
        assertReasons(rando.get(0).getReasoning(),
                "V153 ZONE (USED_PILE, lifeForce=6, protectChars=true) (+800.0)",
                "V153 PRIORITY CARD: protect 'Houjix' -100 (-100.0)");
    }

    @Test
    public void combinedPromptForfeitsCrewBeforeLoadedSupremacyWithBotParity() {
        PhysicalCard supremacy = forfeitCard(
                "Supremacy", Zone.AT_LOCATION, CardCategory.STARSHIP, 16.0f);
        PhysicalCard cardo = forfeitCard(
                "Cardo", Zone.AT_LOCATION, CardCategory.CHARACTER, 6.0f);
        when(cardo.getAttachedTo()).thenReturn(supremacy);
        when(cardo.isPilotOf()).thenReturn(true);

        Map<Integer, PhysicalCard> candidates = new LinkedHashMap<>();
        candidates.put(273, supremacy);
        candidates.put(276, cardo);
        GameState gameState = battleGameState(
                candidates, List.of(), List.of(), 11, 0, 4, 11, 0);
        when(gameState.getAboardCards(supremacy, true))
                .thenReturn(List.of(cardo));
        String prompt = "Choose Force to lose or a card from battle to forfeit";

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.CardSelectionEvaluator()
                .evaluate(randoContext(gameState, prompt,
                        List.of("273", "276"), Phase.BATTLE));
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.CardSelectionEvaluator()
                .evaluate(chosenContext(gameState, prompt,
                        List.of("273", "276"), Phase.BATTLE));

        assertActionParity(rando, chosen);
        assertBits(-7519.0f, action(rando, "273").getScore());
        assertBits(2230.0f, action(rando, "276").getScore());
        assertTrue(action(rando, "273").isHardVetoed());
        assertTrue(hasReason(action(rando, "273"),
                "V48 SHIP WITH CREW"));
        assertTrue(hasReason(action(rando, "273"),
                "V159 FORFEIT (attr=0 dmg=11 fv=16 hit=false)"));
        assertTrue(action(rando, "276").getScore()
                > action(rando, "273").getScore());
    }

    @Test
    public void standaloneAndOptionalPromptsAlsoProtectLoadedShips() {
        for (String prompt : List.of(
                "Choose a card from battle to forfeit",
                "Choose a card from battle to forfeit (if desired)")) {
            PhysicalCard ship = forfeitCard(
                    "Executor", Zone.AT_LOCATION,
                    CardCategory.STARSHIP, 12.0f);
            PhysicalCard crew = forfeitCard(
                    "Admiral Piett", Zone.AT_LOCATION,
                    CardCategory.CHARACTER, 6.0f);
            when(crew.getAttachedTo()).thenReturn(ship);
            when(crew.isPilotOf()).thenReturn(true);

            Map<Integer, PhysicalCard> candidates = new LinkedHashMap<>();
            candidates.put(300, ship);
            candidates.put(301, crew);
            GameState gameState = battleGameState(
                    candidates, List.of(), List.of(), 11, 0, 4, 11, 0);
            when(gameState.getAboardCards(ship, true))
                    .thenReturn(List.of(crew));

            var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.CardSelectionEvaluator()
                    .evaluate(randoContext(gameState, prompt,
                            List.of("300", "301"), Phase.BATTLE));
            var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.CardSelectionEvaluator()
                    .evaluate(chosenContext(gameState, prompt,
                            List.of("300", "301"), Phase.BATTLE));

            assertActionParity(rando, chosen);
            assertTrue(action(rando, "300").isHardVetoed());
            assertTrue(hasReason(action(rando, "300"),
                    "V48 SHIP WITH CREW"));
            assertFalse(action(rando, "301").isHardVetoed());
            assertTrue(action(rando, "301").getScore()
                    > action(rando, "300").getScore());
        }
    }

    @Test
    public void optionalSoleLoadedShipIsProtectedBecausePassIsSafe() {
        PhysicalCard ship = forfeitCard(
                "Executor", Zone.AT_LOCATION,
                CardCategory.STARSHIP, 12.0f);
        PhysicalCard crew = forfeitCard(
                "Admiral Piett", Zone.AT_LOCATION,
                CardCategory.CHARACTER, 6.0f);
        when(crew.getAttachedTo()).thenReturn(ship);
        when(crew.isPilotOf()).thenReturn(true);

        GameState gameState = battleGameState(
                Map.of(300, ship), List.of(), List.of(), 11, 0, 4, 11, 0);
        when(gameState.getAboardCards(ship, true))
                .thenReturn(List.of(crew));
        String prompt = "Choose a card from battle to forfeit (if desired)";

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.CardSelectionEvaluator()
                .evaluate(randoContext(gameState, prompt,
                        List.of("300"), Phase.BATTLE));
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.CardSelectionEvaluator()
                .evaluate(chosenContext(gameState, prompt,
                        List.of("300"), Phase.BATTLE));

        assertActionParity(rando, chosen);
        assertTrue(action(rando, "300").isHardVetoed());
        assertTrue(hasReason(action(rando, "300"),
                "V48 SHIP WITH CREW"));
    }

    @Test
    public void combinedPureDamageProtectsLoadedShipWhenForceLossIsSafe() {
        PhysicalCard shuttle = forfeitCard(
                "Kylo Ren's Command Shuttle", Zone.AT_LOCATION,
                CardCategory.STARSHIP, 5.0f);
        PhysicalCard hux = forfeitCard(
                "General Hux", Zone.AT_LOCATION, CardCategory.CHARACTER, 5.0f);
        PhysicalCard reserveEffect = card(
                "Reserve Effect", Zone.RESERVE_DECK, CardCategory.EFFECT);
        when(hux.getAttachedTo()).thenReturn(shuttle);
        when(hux.isPassengerOf()).thenReturn(true);

        Map<Integer, PhysicalCard> candidates = new LinkedHashMap<>();
        candidates.put(229, shuttle);
        candidates.put(242, reserveEffect);
        GameState gameState = battleGameState(
                candidates, List.of(), List.of(), 11, 0, 1, 2, 0);
        when(gameState.getAboardCards(shuttle, true))
                .thenReturn(List.of(hux));
        String prompt = "Choose Force to lose or a card from battle to forfeit";

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.CardSelectionEvaluator()
                .evaluate(randoContext(gameState, prompt,
                        List.of("229", "242"), Phase.BATTLE));
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.CardSelectionEvaluator()
                .evaluate(chosenContext(gameState, prompt,
                        List.of("229", "242"), Phase.BATTLE));

        assertActionParity(rando, chosen);
        assertBits(-12949.0f, action(rando, "229").getScore());
        assertTrue(action(rando, "229").isHardVetoed());
        assertTrue(hasReason(action(rando, "229"),
                "V48 SHIP WITH CREW"));
    }

    @Test
    public void combinedAttritionAllowsLoadedShipWhenNoOtherForfeitIsLegal() {
        PhysicalCard shuttle = forfeitCard(
                "Kylo Ren's Command Shuttle", Zone.AT_LOCATION,
                CardCategory.STARSHIP, 5.0f);
        PhysicalCard hux = forfeitCard(
                "General Hux", Zone.AT_LOCATION, CardCategory.CHARACTER, 5.0f);
        PhysicalCard reserveEffect = card(
                "Reserve Effect", Zone.RESERVE_DECK, CardCategory.EFFECT);
        when(hux.getAttachedTo()).thenReturn(shuttle);
        when(hux.isPassengerOf()).thenReturn(true);

        Map<Integer, PhysicalCard> candidates = new LinkedHashMap<>();
        candidates.put(229, shuttle);
        candidates.put(242, reserveEffect);
        GameState gameState = battleGameState(
                candidates, List.of(), List.of(), 11, 0, 1, 2, 2);
        when(gameState.getAboardCards(shuttle, true))
                .thenReturn(List.of(hux));
        String prompt = "Choose Force to lose or a card from battle to forfeit";

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.CardSelectionEvaluator()
                .evaluate(randoContext(gameState, prompt,
                        List.of("229", "242"), Phase.BATTLE));
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.CardSelectionEvaluator()
                .evaluate(chosenContext(gameState, prompt,
                        List.of("229", "242"), Phase.BATTLE));

        assertActionParity(rando, chosen);
        assertBits(2250.0f, action(rando, "229").getScore());
        assertFalse(action(rando, "229").isHardVetoed());
        assertFalse(hasReason(action(rando, "229"),
                "V48 SHIP WITH CREW"));
        assertTrue(hasReason(action(rando, "229"),
                "V159 FORFEIT (attr=2 dmg=2"));
    }

    @Test
    public void nonSelectableForceLossDoesNotCreateASafeAlternative() {
        PhysicalCard shuttle = forfeitCard(
                "Kylo Ren's Command Shuttle", Zone.AT_LOCATION,
                CardCategory.STARSHIP, 5.0f);
        PhysicalCard hux = forfeitCard(
                "General Hux", Zone.AT_LOCATION, CardCategory.CHARACTER, 5.0f);
        PhysicalCard reserveEffect = card(
                "Reserve Effect", Zone.RESERVE_DECK, CardCategory.EFFECT);
        when(hux.getAttachedTo()).thenReturn(shuttle);
        when(hux.isPassengerOf()).thenReturn(true);

        Map<Integer, PhysicalCard> candidates = new LinkedHashMap<>();
        candidates.put(229, shuttle);
        candidates.put(242, reserveEffect);
        GameState gameState = battleGameState(
                candidates, List.of(), List.of(), 11, 0, 1, 2, 0);
        when(gameState.getAboardCards(shuttle, true))
                .thenReturn(List.of(hux));
        String prompt = "Choose Force to lose or a card from battle to forfeit";
        var randoContext = randoContext(
                gameState, prompt, List.of("229", "242"), Phase.BATTLE);
        randoContext.setSelectable(List.of(true, false));
        var chosenContext = chosenContext(
                gameState, prompt, List.of("229", "242"), Phase.BATTLE);
        chosenContext.setSelectable(List.of(true, false));

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.CardSelectionEvaluator()
                .evaluate(randoContext);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.CardSelectionEvaluator()
                .evaluate(chosenContext);

        assertActionParity(rando, chosen);
        assertFalse(action(rando, "229").isHardVetoed());
        assertFalse(hasReason(action(rando, "229"),
                "V48 SHIP WITH CREW"));
    }

    @Test
    public void nonSelectableCrewDoesNotCreateAFalseForfeitAlternative() {
        PhysicalCard shuttle = forfeitCard(
                "Kylo Ren's Command Shuttle", Zone.AT_LOCATION,
                CardCategory.STARSHIP, 5.0f);
        PhysicalCard hux = forfeitCard(
                "General Hux", Zone.AT_LOCATION, CardCategory.CHARACTER, 5.0f);
        when(hux.getAttachedTo()).thenReturn(shuttle);
        when(hux.isPassengerOf()).thenReturn(true);

        Map<Integer, PhysicalCard> candidates = new LinkedHashMap<>();
        candidates.put(229, shuttle);
        candidates.put(230, hux);
        GameState gameState = battleGameState(
                candidates, List.of(), List.of(), 11, 0, 1, 2, 0);
        when(gameState.getAboardCards(shuttle, true))
                .thenReturn(List.of(hux));
        String prompt = "Choose Force to lose or a card from battle to forfeit";

        var randoContext = randoContext(
                gameState, prompt, List.of("229", "230"), Phase.BATTLE);
        randoContext.setSelectable(List.of(true, false));
        var chosenContext = chosenContext(
                gameState, prompt, List.of("229", "230"), Phase.BATTLE);
        chosenContext.setSelectable(List.of(true, false));

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.CardSelectionEvaluator()
                .evaluate(randoContext);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.CardSelectionEvaluator()
                .evaluate(chosenContext);

        assertActionParity(rando, chosen);
        assertFalse(action(rando, "229").isHardVetoed());
        assertFalse(hasReason(action(rando, "229"),
                "V48 SHIP WITH CREW"));
    }

    @Test
    public void turnFourBroadWielderProtectionIsAppliedOnceWithBotParity() {
        PhysicalCard weapon = card("Unmatched Blaster", Zone.HAND,
                CardCategory.WEAPON);
        PhysicalCard unrelatedCharacter = card("Unrelated Character", Zone.HAND,
                CardCategory.CHARACTER);
        Map<Integer, PhysicalCard> candidates = Map.of(10, weapon);
        List<PhysicalCard> hand = List.of(
                weapon,
                unrelatedCharacter,
                card("Hand Three", Zone.HAND, CardCategory.EFFECT),
                card("Hand Four", Zone.HAND, CardCategory.EFFECT),
                card("Hand Five", Zone.HAND, CardCategory.EFFECT));
        GameState gameState = gameState(candidates, hand, List.of(), 9, 0, 4);

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.CardSelectionEvaluator()
                .evaluate(randoContext(gameState, "Choose Force to lose",
                        List.of("10"), Phase.BATTLE));
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.CardSelectionEvaluator()
                .evaluate(chosenContext(gameState, "Choose Force to lose",
                        List.of("10"), Phase.BATTLE));

        assertActionParity(rando, chosen);
        assertEquals(1, rando.size());
        assertBits(200.0f, rando.get(0).getScore());
        assertReasons(rando.get(0).getReasoning(),
                "V153 ZONE (HAND, lifeForce=9, protectChars=true) (+600.0)",
                "V178 PROTECT WEAPON: 'Unmatched Blaster' — we have a wielder; lose it near-last, like a character (-450.0)");
    }

    @Test
    public void v154HitHostAndLooseWeaponKeepEarlyContinueAndBotParity() {
        PhysicalCard hitHost = mock(PhysicalCard.class);
        when(hitHost.isHit()).thenReturn(true);
        PhysicalCard hitWeapon = card("Hit Weapon", Zone.AT_LOCATION,
                CardCategory.WEAPON);
        when(hitWeapon.getAttachedTo()).thenReturn(hitHost);
        PhysicalCard looseWeapon = card("Loose Weapon", Zone.AT_LOCATION,
                CardCategory.WEAPON);
        Map<Integer, PhysicalCard> candidates = new LinkedHashMap<>();
        candidates.put(11, hitWeapon);
        candidates.put(12, looseWeapon);
        GameState gameState = gameState(candidates, List.of(), List.of(), 5, 0, 4);
        String prompt = "Choose Force to lose or choose a card from battle to forfeit";

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.CardSelectionEvaluator()
                .evaluate(randoContext(gameState, prompt, List.of("11", "12"), Phase.BATTLE));
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.CardSelectionEvaluator()
                .evaluate(chosenContext(gameState, prompt, List.of("11", "12"), Phase.BATTLE));

        assertActionParity(rando, chosen);
        assertBits(2250.0f, action(rando, "11").getScore());
        assertReasons(action(rando, "11").getReasoning(),
                "V154 WEAPON-LOSS: strip battle weapon for extra damage coverage (host is HIT — lost anyway) (+2200.0)");
        assertBits(2050.0f, action(rando, "12").getScore());
        assertReasons(action(rando, "12").getReasoning(),
                "V154 WEAPON-LOSS: strip battle weapon for extra damage coverage (+2000.0)");
    }

    @Test
    public void combinedHitCharactersVehiclesAndStarshipsPrecedeEveryLaterLossStage() {
        PhysicalCard hitCharacter = forfeitCard(
                "Hit Character", Zone.AT_LOCATION, CardCategory.CHARACTER, 4.0f);
        PhysicalCard hitVehicle = forfeitCard(
                "Hit Vehicle", Zone.AT_LOCATION, CardCategory.VEHICLE, 6.0f);
        PhysicalCard hitStarship = forfeitCard(
                "Hit Starship", Zone.AT_LOCATION, CardCategory.STARSHIP, 8.0f);
        PhysicalCard attachedWeapon = card(
                "Attached Weapon", Zone.AT_LOCATION, CardCategory.WEAPON);
        PhysicalCard nonHitCharacter = forfeitCard(
                "Non-Hit Character", Zone.AT_LOCATION, CardCategory.CHARACTER, 3.0f);
        PhysicalCard reserveEffect = card(
                "Reserve Effect", Zone.RESERVE_DECK, CardCategory.EFFECT);
        when(hitCharacter.isHit()).thenReturn(true);
        when(hitVehicle.isHit()).thenReturn(true);
        when(hitStarship.isHit()).thenReturn(true);
        when(attachedWeapon.getAttachedTo()).thenReturn(hitCharacter);

        Map<Integer, PhysicalCard> candidates = new LinkedHashMap<>();
        candidates.put(30, hitCharacter);
        candidates.put(31, hitVehicle);
        candidates.put(32, hitStarship);
        candidates.put(33, attachedWeapon);
        candidates.put(34, nonHitCharacter);
        candidates.put(35, reserveEffect);
        GameState gameState = battleGameState(
                candidates, List.of(), List.of(), 20, 0, 4, 22, 11);
        String prompt = "Choose Force to lose or a card from battle to forfeit";

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.CardSelectionEvaluator()
                .evaluate(randoContext(gameState, prompt,
                        List.of("30", "31", "32", "33", "34", "35"),
                        Phase.BATTLE));
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.CardSelectionEvaluator()
                .evaluate(chosenContext(gameState, prompt,
                        List.of("30", "31", "32", "33", "34", "35"),
                        Phase.BATTLE));

        assertActionParity(rando, chosen);
        assertEquals(3, rando.size());
        assertFalse(action(rando, "30").isDeferred());
        assertFalse(action(rando, "31").isDeferred());
        assertFalse(action(rando, "32").isDeferred());
        assertFalse(hasAction(rando, "33"));
        assertFalse(hasAction(rando, "34"));
        assertFalse(hasAction(rando, "35"));
    }

    @Test
    public void combinedAttritionForfeitsSoleVengeanceBeforeReserveWeapon() {
        PhysicalCard vengeance = forfeitCard(
                "Vengeance", Zone.AT_LOCATION, CardCategory.STARSHIP, 8.0f);
        when(vengeance.getBlueprint().getCardSubtype())
                .thenReturn(CardSubtype.CAPITAL);
        PhysicalCard reserveWeapon = card(
                "Reserve Weapon", Zone.RESERVE_DECK, CardCategory.WEAPON);
        Map<Integer, PhysicalCard> candidates = new LinkedHashMap<>();
        candidates.put(36, vengeance);
        candidates.put(37, reserveWeapon);
        GameState gameState = battleGameState(
                candidates, List.of(), List.of(), 20, 0, 4, 22, 11);
        String prompt = "Choose Force to lose or a card from battle to forfeit";

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.CardSelectionEvaluator()
                .evaluate(randoContext(gameState, prompt,
                        List.of("36", "37"), Phase.BATTLE));
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.CardSelectionEvaluator()
                .evaluate(chosenContext(gameState, prompt,
                        List.of("36", "37"), Phase.BATTLE));

        assertActionParity(rando, chosen);
        assertEquals(1, rando.size());
        assertFalse(action(rando, "36").isDeferred());
        assertFalse(action(rando, "36").isHardVetoed());
        assertEquals("Forfeit Vengeance", action(rando, "36").getDisplayText());
        assertFalse(hasAction(rando, "37"));
    }

    @Test
    public void attritionStageUsesNonImmuneActorsBeforeWeaponsAndForce() {
        PhysicalCard character = forfeitCard(
                "Character", Zone.AT_LOCATION, CardCategory.CHARACTER, 4.0f);
        PhysicalCard vehicle = forfeitCard(
                "Vehicle", Zone.AT_LOCATION, CardCategory.VEHICLE, 6.0f);
        PhysicalCard starship = forfeitCard(
                "Starship", Zone.AT_LOCATION, CardCategory.STARSHIP, 8.0f);
        PhysicalCard immuneCharacter = forfeitCard(
                "Immune Character", Zone.AT_LOCATION, CardCategory.CHARACTER, 5.0f);
        PhysicalCard cannotSatisfyVehicle = forfeitCard(
                "Cannot Satisfy Vehicle", Zone.AT_LOCATION, CardCategory.VEHICLE, 5.0f);
        PhysicalCard battleWeapon = card(
                "Battle Weapon", Zone.AT_LOCATION, CardCategory.WEAPON);
        PhysicalCard reserveEffect = card(
                "Reserve Effect", Zone.RESERVE_DECK, CardCategory.EFFECT);
        Map<Integer, PhysicalCard> candidates = new LinkedHashMap<>();
        candidates.put(38, character);
        candidates.put(39, vehicle);
        candidates.put(40, starship);
        candidates.put(41, immuneCharacter);
        candidates.put(42, cannotSatisfyVehicle);
        candidates.put(43, battleWeapon);
        candidates.put(44, reserveEffect);
        GameState gameState = battleGameState(
                candidates, List.of(), List.of(), 20, 0, 4, 12, 5);
        ModifiersQuerying modifiers = GAME_BY_STATE.get(gameState)
                .getModifiersQuerying();
        when(modifiers.getImmunityToAttritionLessThan(
                gameState, immuneCharacter)).thenReturn(6.0f);
        when(modifiers.cannotSatisfyAttrition(
                gameState, cannotSatisfyVehicle)).thenReturn(true);
        String prompt = "Choose Force to lose or a card from battle to forfeit";

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.CardSelectionEvaluator()
                .evaluate(randoContext(gameState, prompt,
                        List.of("38", "39", "40", "41", "42", "43", "44"),
                        Phase.BATTLE));
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.CardSelectionEvaluator()
                .evaluate(chosenContext(gameState, prompt,
                        List.of("38", "39", "40", "41", "42", "43", "44"),
                        Phase.BATTLE));

        assertActionParity(rando, chosen);
        assertEquals(3, rando.size());
        assertFalse(action(rando, "38").isDeferred());
        assertFalse(action(rando, "39").isDeferred());
        assertFalse(action(rando, "40").isDeferred());
        assertFalse(hasAction(rando, "41"));
        assertFalse(hasAction(rando, "42"));
        assertFalse(hasAction(rando, "43"));
        assertFalse(hasAction(rando, "44"));
    }

    @Test
    public void allImmuneParticipantsMoveToRemainingDamageTier() {
        PhysicalCard immuneCharacter = forfeitCard(
                "Immune Character", Zone.AT_LOCATION, CardCategory.CHARACTER, 5.0f);
        PhysicalCard reserveEffect = card(
                "Reserve Effect", Zone.RESERVE_DECK, CardCategory.EFFECT);
        GameState gameState = battleGameState(
                Map.of(45, immuneCharacter, 46, reserveEffect),
                List.of(), List.of(), 20, 0, 4, 10, 5);
        ModifiersQuerying modifiers = GAME_BY_STATE.get(gameState)
                .getModifiersQuerying();
        when(modifiers.getImmunityToAttritionLessThan(
                gameState, immuneCharacter)).thenReturn(6.0f);
        String prompt = "Choose Force to lose or a card from battle to forfeit";

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.CardSelectionEvaluator()
                .evaluate(randoContext(gameState, prompt,
                        List.of("45", "46"), Phase.BATTLE));
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.CardSelectionEvaluator()
                .evaluate(chosenContext(gameState, prompt,
                        List.of("45", "46"), Phase.BATTLE));

        assertActionParity(rando, chosen);
        assertEquals(2, rando.size());
        assertTrue(hasAction(rando, "45"));
        assertTrue(hasAction(rando, "46"));
        assertTrue(hasReason(action(rando, "45"),
                "V159 FORFEIT (attr=0 dmg=10 fv=5 hit=false)"));
        assertFalse(hasReason(action(rando, "46"), "V150"));
        assertTrue(hasReason(action(rando, "46"), "10 damage left"));
    }

    @Test
    public void allCannotSatisfyParticipantsMoveToPureDamageScoring() {
        PhysicalCard cannotSatisfyVehicle = forfeitCard(
                "Cannot Satisfy Vehicle", Zone.AT_LOCATION,
                CardCategory.VEHICLE, 5.0f);
        PhysicalCard reserveEffect = card(
                "Reserve Effect", Zone.RESERVE_DECK, CardCategory.EFFECT);
        GameState gameState = battleGameState(
                Map.of(54, cannotSatisfyVehicle, 55, reserveEffect),
                List.of(), List.of(), 20, 0, 4, 10, 5);
        ModifiersQuerying modifiers = GAME_BY_STATE.get(gameState)
                .getModifiersQuerying();
        when(modifiers.cannotSatisfyAttrition(
                gameState, cannotSatisfyVehicle)).thenReturn(true);
        String prompt = "Choose Force to lose or a card from battle to forfeit";

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.CardSelectionEvaluator()
                .evaluate(randoContext(gameState, prompt,
                        List.of("54", "55"), Phase.BATTLE));
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.CardSelectionEvaluator()
                .evaluate(chosenContext(gameState, prompt,
                        List.of("54", "55"), Phase.BATTLE));

        assertActionParity(rando, chosen);
        assertEquals(2, rando.size());
        assertTrue(hasReason(action(rando, "54"),
                "V159 FORFEIT (attr=0 dmg=10 fv=5 hit=false)"));
        assertFalse(hasReason(action(rando, "55"), "V150"));
        assertTrue(hasReason(action(rando, "55"), "10 damage left"));
    }

    @Test
    public void attritionImmunityUsesFrozenTotalRatherThanRemainingAmount() {
        PhysicalCard character = forfeitCard(
                "Character", Zone.AT_LOCATION, CardCategory.CHARACTER, 5.0f);
        PhysicalCard reserveEffect = card(
                "Reserve Effect", Zone.RESERVE_DECK, CardCategory.EFFECT);
        GameState gameState = battleGameState(
                Map.of(52, character, 53, reserveEffect),
                List.of(), List.of(), 20, 0, 4, 14, 11);
        gameState.getBattleState().increaseAttritionSatisfied("tester", 8.0f);
        ModifiersQuerying modifiers = GAME_BY_STATE.get(gameState)
                .getModifiersQuerying();
        when(modifiers.getImmunityToAttritionLessThan(
                gameState, character)).thenReturn(5.0f);
        assertBits(3.0f, com.gempukku.swccgo.logic.timing.GuiUtils
                .getBattleAttritionRemaining(GAME_BY_STATE.get(gameState), "tester"));
        String prompt = "Choose Force to lose or a card from battle to forfeit";

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.CardSelectionEvaluator()
                .evaluate(randoContext(gameState, prompt,
                        List.of("52", "53"), Phase.BATTLE));
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.CardSelectionEvaluator()
                .evaluate(chosenContext(gameState, prompt,
                        List.of("52", "53"), Phase.BATTLE));

        assertActionParity(rando, chosen);
        assertEquals(1, rando.size());
        assertTrue(hasAction(rando, "52"));
        assertFalse(hasAction(rando, "53"));
    }

    @Test
    public void nonSelectableHitParticipantDoesNotControlTheLossTier() {
        PhysicalCard unavailableHit = forfeitCard(
                "Unavailable Hit", Zone.AT_LOCATION, CardCategory.CHARACTER, 5.0f);
        when(unavailableHit.isHit()).thenReturn(true);
        PhysicalCard reserveEffect = card(
                "Reserve Effect", Zone.RESERVE_DECK, CardCategory.EFFECT);
        GameState gameState = battleGameState(
                Map.of(47, unavailableHit, 48, reserveEffect),
                List.of(), List.of(), 20, 0, 4, 10, 5);
        String prompt = "Choose Force to lose or a card from battle to forfeit";
        var randoContext = randoContext(
                gameState, prompt, List.of("47", "48"), Phase.BATTLE);
        randoContext.setSelectable(List.of(false, true));
        var chosenContext = chosenContext(
                gameState, prompt, List.of("47", "48"), Phase.BATTLE);
        chosenContext.setSelectable(List.of(false, true));

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.CardSelectionEvaluator()
                .evaluate(randoContext);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.CardSelectionEvaluator()
                .evaluate(chosenContext);

        assertActionParity(rando, chosen);
        assertEquals(1, rando.size());
        assertFalse(hasAction(rando, "47"));
        assertEquals("Lose Force from pile", action(rando, "48").getDisplayText());
        assertFalse(hasReason(action(rando, "48"), "V150"));
        assertTrue(hasReason(action(rando, "48"), "10 damage left"));
    }

    @Test
    public void soleHitLoadedStarshipCannotBeBlockedByLaterLosses() {
        PhysicalCard hitShip = forfeitCard(
                "Hit Loaded Ship", Zone.AT_LOCATION, CardCategory.STARSHIP, 8.0f);
        PhysicalCard passenger = forfeitCard(
                "Passenger", Zone.AT_LOCATION, CardCategory.CHARACTER, 4.0f);
        PhysicalCard reserveEffect = card(
                "Reserve Effect", Zone.RESERVE_DECK, CardCategory.EFFECT);
        when(hitShip.isHit()).thenReturn(true);
        when(passenger.getAttachedTo()).thenReturn(hitShip);
        when(passenger.isPassengerOf()).thenReturn(true);
        GameState gameState = battleGameState(
                Map.of(49, hitShip, 50, reserveEffect),
                List.of(), List.of(), 20, 0, 4, 22, 11);
        when(gameState.getAboardCards(hitShip, true)).thenReturn(List.of(passenger));
        String prompt = "Choose Force to lose or a card from battle to forfeit";

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.CardSelectionEvaluator()
                .evaluate(randoContext(gameState, prompt,
                        List.of("49", "50"), Phase.BATTLE));
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.CardSelectionEvaluator()
                .evaluate(chosenContext(gameState, prompt,
                        List.of("49", "50"), Phase.BATTLE));

        assertActionParity(rando, chosen);
        assertEquals(1, rando.size());
        assertFalse(action(rando, "49").isHardVetoed());
        assertFalse(hasReason(action(rando, "49"), "V48 SHIP WITH CREW"));
    }

    @Test
    public void residualAttritionDisappearsWhenNoBattleParticipantRemains() {
        PhysicalCard reserveWeapon = card(
                "Reserve Weapon", Zone.RESERVE_DECK, CardCategory.WEAPON);
        GameState gameState = battleGameState(
                Map.of(51, reserveWeapon), List.of(), List.of(),
                20, 0, 4, 14, 3);
        SwccgGame game = GAME_BY_STATE.get(gameState);
        assertBits(0.0f, com.gempukku.swccgo.logic.timing.GuiUtils
                .getBattleAttritionRemaining(game, "tester"));
        String prompt = "Choose Force to lose or a card from battle to forfeit";

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.CardSelectionEvaluator()
                .evaluate(randoContext(gameState, prompt,
                        List.of("51"), Phase.BATTLE));
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.CardSelectionEvaluator()
                .evaluate(chosenContext(gameState, prompt,
                        List.of("51"), Phase.BATTLE));

        assertActionParity(rando, chosen);
        assertFalse(action(rando, "51").isDeferred());
        assertEquals("Lose Force from pile", action(rando, "51").getDisplayText());
        assertFalse(hasReason(action(rando, "51"), "V150"));
        assertFalse(hasReason(action(rando, "51"), "V154 WEAPON-LOSS"));
        assertTrue(hasReason(action(rando, "51"), "14 damage left"));
    }

    @Test
    public void v118SmallDamageRunsBeforeCombinedPolicyWithBotParity() {
        PhysicalCard reserveEffect = card("Reserve Effect", Zone.RESERVE_DECK,
                CardCategory.EFFECT);
        Map<Integer, PhysicalCard> candidates = Map.of(13, reserveEffect);
        GameState gameState = battleGameState(
                candidates, List.of(), List.of(), 11, 0, 4, 2, 0);
        String prompt = "Choose Force to lose or choose a card from battle to forfeit";

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.CardSelectionEvaluator()
                .evaluate(randoContext(gameState, prompt, List.of("13"), Phase.BATTLE));
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.CardSelectionEvaluator()
                .evaluate(chosenContext(gameState, prompt, List.of("13"), Phase.BATTLE));

        assertActionParity(rando, chosen);
        assertEquals(rando.get(0).getReasoning().toString(), 3,
                rando.get(0).getReasoning().size());
        assertTrue(rando.get(0).getReasoning().get(0).startsWith("V118 SMALL DAMAGE:"));
        assertTrue(rando.get(0).getReasoning().get(1).startsWith("V153 ZONE (RESERVE_DECK"));
        assertTrue(rando.get(0).getReasoning().get(2).contains("V22.3"));
    }

    @Test
    public void v223DamageTiersStayAfterCombinedPolicyWithBotParity() {
        assertCombinedDamageTier(3, -40.0f, "3 damage left");
        assertCombinedDamageTier(6, -80.0f, "6 damage left");
        assertCombinedDamageTier(11, -120.0f, "11 damage left");
    }

    @Test
    public void lostPileAliasUsesStandaloneRouteWhileUnknownStaysOutsideForceLoss() {
        PhysicalCard candidate = card("Lost Pile Candidate", Zone.HAND,
                CardCategory.EFFECT);
        Map<Integer, PhysicalCard> candidates = Map.of(14, candidate);
        GameState gameState = gameState(candidates, List.of(candidate), List.of(), 5, 0, 2);

        var lostPileRando = new com.gempukku.swccgo.ai.models.rando.evaluators.CardSelectionEvaluator()
                .evaluate(randoContext(gameState, "Choose card to put on lost pile",
                        List.of("14"), Phase.CONTROL));
        var lostPileChosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.CardSelectionEvaluator()
                .evaluate(chosenContext(gameState, "Choose card to put on lost pile",
                        List.of("14"), Phase.CONTROL));
        assertActionParity(lostPileRando, lostPileChosen);
        assertTrue(hasReason(lostPileRando.get(0), "V153 ZONE"));

        var unknownRando = new com.gempukku.swccgo.ai.models.rando.evaluators.CardSelectionEvaluator()
                .evaluate(randoContext(gameState, "Choose a card",
                        List.of("14"), Phase.CONTROL));
        var unknownChosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.CardSelectionEvaluator()
                .evaluate(chosenContext(gameState, "Choose a card",
                        List.of("14"), Phase.CONTROL));
        assertActionParity(unknownRando, unknownChosen);
        assertEquals(1, unknownRando.size());
        assertFalse(hasReason(unknownRando.get(0), "V153"));
    }

    @Test
    public void unknownLossCategoryMatrixKeepsBaseThirtyAndBotParity() {
        Map<Integer, PhysicalCard> candidates = new LinkedHashMap<>();
        candidates.put(20, card("Effect", Zone.HAND, CardCategory.EFFECT));
        candidates.put(21, card("Character", Zone.HAND, CardCategory.CHARACTER));
        candidates.put(22, card("Starship", Zone.HAND, CardCategory.STARSHIP));
        candidates.put(23, card("Vehicle", Zone.HAND, CardCategory.VEHICLE));
        candidates.put(24, card("Location", Zone.HAND, CardCategory.LOCATION));
        candidates.put(25, card("Weapon", Zone.HAND, CardCategory.WEAPON));
        List<String> ids = List.of("20", "21", "22", "23", "24", "25");
        GameState gameState = gameState(candidates, List.of(), List.of(), 5, 0, 2);
        String prompt = "Choose a card to place in Used Pile";

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.CardSelectionEvaluator()
                .evaluate(randoContext(gameState, prompt, ids, Phase.CONTROL));
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.CardSelectionEvaluator()
                .evaluate(chosenContext(gameState, prompt, ids, Phase.CONTROL));

        assertActionParity(rando, chosen);
        assertBits(55.0f, action(rando, "20").getScore());
        assertBits(15.0f, action(rando, "21").getScore());
        assertBits(15.0f, action(rando, "22").getScore());
        assertBits(20.0f, action(rando, "23").getScore());
        assertBits(10.0f, action(rando, "24").getScore());
        assertBits(30.0f, action(rando, "25").getScore());
        assertReasons(action(rando, "20").getReasoning(),
                "Effect/Interrupt - OK to lose (+25.0)");
        assertReasons(action(rando, "25").getReasoning());
    }

    @Test
    public void huntDownUnknownLossKeepsCategoryBeforeV25WithBotParity() {
        PhysicalCard lightsaber = card("Test Lightsaber", Zone.HAND,
                CardCategory.EFFECT);
        GameState gameState = gameState(Map.of(26, lightsaber),
                List.of(), List.of(), 5, 0, 2);
        String prompt = "Choose a card to place in Used Pile";

        var randoContext = randoContext(gameState, prompt,
                List.of("26"), Phase.CONTROL);
        var randoObjective = mock(
                com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer.class);
        when(randoObjective.isAnalyzed()).thenReturn(true);
        when(randoObjective.isHuntDownV()).thenReturn(true);
        randoContext.setObjectiveAnalyzer(randoObjective);

        var chosenContext = chosenContext(gameState, prompt,
                List.of("26"), Phase.CONTROL);
        var chosenObjective = mock(
                com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer.class);
        when(chosenObjective.isAnalyzed()).thenReturn(true);
        when(chosenObjective.isHuntDownV()).thenReturn(true);
        chosenContext.setObjectiveAnalyzer(chosenObjective);

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.CardSelectionEvaluator()
                .evaluate(randoContext);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.CardSelectionEvaluator()
                .evaluate(chosenContext);

        assertActionParity(rando, chosen);
        assertBits(-245.0f, rando.get(0).getScore());
        assertReasons(rando.get(0).getReasoning(),
                "Effect/Interrupt - OK to lose (+25.0)",
                "V25 HUNT DOWN: PROTECT LIGHTSABER from loss! (-300.0)");
    }

    @Test
    public void preFlipActorFilterProtectsDuplicateNuteWithBotParity() {
        PhysicalCard handNute = card("Nute Gunray", Zone.HAND,
                CardCategory.CHARACTER);
        PhysicalCard tableNute = card("Nute Gunray", Zone.AT_LOCATION,
                CardCategory.CHARACTER);
        List<PhysicalCard> hand = List.of(
                handNute,
                card("Hand Two", Zone.HAND, CardCategory.EFFECT),
                card("Hand Three", Zone.HAND, CardCategory.EFFECT),
                card("Hand Four", Zone.HAND, CardCategory.EFFECT),
                card("Hand Five", Zone.HAND, CardCategory.EFFECT));
        GameState gameState = gameState(
                Map.of(27, handNute), hand, List.of(), 11, 0, 4);
        when(gameState.getAllPermanentCards()).thenReturn(List.of(tableNute));
        SwccgGame game = mock(SwccgGame.class);
        when(game.getGameState()).thenReturn(gameState);
        gameStateWithGame(gameState, game);

        var randoContext = randoContext(gameState, "Choose Force to lose",
                List.of("27"), Phase.CONTROL);
        var randoObjective = mock(
                com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer.class);
        when(randoObjective.isAnalyzed()).thenReturn(true);
        when(randoObjective.matchesFlipGateActorRequirement(
                game, "tester", handNute)).thenReturn(true);
        randoContext.setObjectiveAnalyzer(randoObjective);

        var chosenContext = chosenContext(gameState, "Choose Force to lose",
                List.of("27"), Phase.CONTROL);
        var chosenObjective = mock(
                com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer.class);
        when(chosenObjective.isAnalyzed()).thenReturn(true);
        when(chosenObjective.matchesFlipGateActorRequirement(
                game, "tester", handNute)).thenReturn(true);
        chosenContext.setObjectiveAnalyzer(chosenObjective);

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.CardSelectionEvaluator()
                .evaluate(randoContext);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.CardSelectionEvaluator()
                .evaluate(chosenContext);

        assertActionParity(rando, chosen);
        assertBits(750.0f, rando.get(0).getScore());
        assertReasons(rando.get(0).getReasoning(),
                "V153 ZONE (HAND, lifeForce=11, protectChars=true) (+1000.0)",
                "OBJECTIVE CRITICAL IN HAND: prefer to retain (-300.0)");
    }

    @Test
    public void countedObjectiveActorAndLocationInHandReceiveForceLossProtectionWithBotParity() {
        PhysicalCard requiredActor = card(
                "Required Actor", Zone.HAND, CardCategory.CHARACTER);
        PhysicalCard requiredLocation = card(
                "Required Location", Zone.HAND, CardCategory.LOCATION);
        List<PhysicalCard> hand = List.of(
                requiredActor,
                requiredLocation,
                card("Hand Three", Zone.HAND, CardCategory.EFFECT),
                card("Hand Four", Zone.HAND, CardCategory.EFFECT),
                card("Hand Five", Zone.HAND, CardCategory.EFFECT));
        GameState gameState = gameState(
                Map.of(28, requiredActor, 29, requiredLocation),
                hand, List.of(), 11, 0, 4);
        SwccgGame game = mock(SwccgGame.class);
        when(game.getGameState()).thenReturn(gameState);
        gameStateWithGame(gameState, game);

        var randoContext = randoContext(gameState, "Choose Force to lose",
                List.of("28", "29"), Phase.CONTROL);
        var randoObjective = mock(
                com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer.class);
        when(randoObjective.isAnalyzed()).thenReturn(true);
        when(randoObjective.classifyPreFlipProgressCandidate(
                game, "tester", requiredActor)).thenReturn(REQUIRED_ACTOR);
        when(randoObjective.classifyPreFlipProgressCandidate(
                game, "tester", requiredLocation)).thenReturn(REQUIRED_LOCATION);
        randoContext.setObjectiveAnalyzer(randoObjective);

        var chosenContext = chosenContext(gameState, "Choose Force to lose",
                List.of("28", "29"), Phase.CONTROL);
        var chosenObjective = mock(
                com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer.class);
        when(chosenObjective.isAnalyzed()).thenReturn(true);
        when(chosenObjective.classifyPreFlipProgressCandidate(
                game, "tester", requiredActor)).thenReturn(REQUIRED_ACTOR);
        when(chosenObjective.classifyPreFlipProgressCandidate(
                game, "tester", requiredLocation)).thenReturn(REQUIRED_LOCATION);
        chosenContext.setObjectiveAnalyzer(chosenObjective);

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.CardSelectionEvaluator()
                .evaluate(randoContext);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.CardSelectionEvaluator()
                .evaluate(chosenContext);

        assertActionParity(rando, chosen);
        assertBits(-150.0f, action(rando, "28").getScore());
        assertBits(350.0f, action(rando, "29").getScore());
        assertReasons(action(rando, "28").getReasoning(),
                "V153 ZONE (HAND, lifeForce=11, protectChars=true) (+100.0)",
                "OBJECTIVE CRITICAL IN HAND: prefer to retain (-300.0)");
        assertReasons(action(rando, "29").getReasoning(),
                "V153 ZONE (HAND, lifeForce=11, protectChars=true) (+600.0)",
                "OBJECTIVE CRITICAL IN HAND: prefer to retain (-300.0)");
    }

    private static void assertCombinedDamageTier(int damageRemaining,
                                                 float expectedPenalty,
                                                 String damageText) {
        PhysicalCard reserveEffect = card("Reserve Effect", Zone.RESERVE_DECK,
                CardCategory.EFFECT);
        GameState gameState = battleGameState(
                Map.of(15, reserveEffect), List.of(), List.of(), 11, 0, 4,
                damageRemaining, 0);
        String prompt = "Choose Force to lose or choose a card from battle to forfeit";

        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.CardSelectionEvaluator()
                .evaluate(randoContext(gameState, prompt, List.of("15"), Phase.BATTLE));
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.CardSelectionEvaluator()
                .evaluate(chosenContext(gameState, prompt, List.of("15"), Phase.BATTLE));

        assertActionParity(rando, chosen);
        assertEquals(rando.get(0).getReasoning().toString(), 2,
                rando.get(0).getReasoning().size());
        assertTrue(rando.get(0).getReasoning().get(0).startsWith("V153 ZONE (RESERVE_DECK"));
        assertTrue(rando.get(0).getReasoning().get(1).contains("V22.3"));
        assertTrue(rando.get(0).getReasoning().get(1).contains(damageText));
        assertTrue(rando.get(0).getReasoning().get(1)
                .contains(String.format("(%.1f)", expectedPenalty)));
    }

    private static boolean hasReason(
            com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction action,
            String text) {
        return action.getReasoning().stream().anyMatch(reason -> reason.contains(text));
    }

    private static boolean hasAction(
            List<com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction> actions,
            String actionId) {
        return actions.stream().anyMatch(action -> actionId.equals(action.getActionId()));
    }

    private static GameState gameState(Map<Integer, PhysicalCard> candidates,
                                       List<PhysicalCard> hand,
                                       List<PhysicalCard> usedPile,
                                       int reserveDeckSize,
                                       int forcePileSize,
                                       int turnNumber) {
        GameState gameState = mock(GameState.class);
        when(gameState.getPlayersLatestTurnNumber("tester")).thenReturn(turnNumber);
        when(gameState.getCurrentPlayerId()).thenReturn("tester");
        when(gameState.getHand("tester")).thenReturn(hand);
        when(gameState.getReserveDeckSize("tester")).thenReturn(reserveDeckSize);
        when(gameState.getUsedPile("tester")).thenReturn(usedPile);
        when(gameState.getForcePileSize("tester")).thenReturn(forcePileSize);
        when(gameState.getAllPermanentCards()).thenReturn(List.of());
        when(gameState.findCardById(anyInt())).thenAnswer(
                invocation -> candidates.get(invocation.getArgument(0, Integer.class)));
        return gameState;
    }

    private static GameState battleGameState(Map<Integer, PhysicalCard> candidates,
                                             List<PhysicalCard> hand,
                                             List<PhysicalCard> usedPile,
                                             int reserveDeckSize,
                                             int forcePileSize,
                                             int turnNumber,
                                             int damageRemaining,
                                             int attritionRemaining) {
        GameState gameState = gameState(candidates, hand, usedPile, reserveDeckSize,
                forcePileSize, turnNumber);
        SwccgGame game = mock(SwccgGame.class);
        ModifiersQuerying modifiersQuerying = mock(ModifiersQuerying.class);
        BattleState battleState = new BattleState();
        battleState.reachedDamageSegment();
        battleState.setBaseBattleDamage("tester", damageRemaining);
        if (attritionRemaining > 0) {
            battleState.baseAttritionCalculated();
            battleState.setAttritionTotal("tester", attritionRemaining);
            for (PhysicalCard candidate : candidates.values()) {
                if (candidate.getZone() == Zone.AT_LOCATION) {
                    battleState.addDarkCardParticipant(candidate);
                    when(modifiersQuerying.mayBeForfeitedInBattle(
                            gameState, candidate)).thenReturn(true);
                }
            }
        }
        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiersQuerying);
        when(modifiersQuerying.getTotalBattleDamage(gameState, "tester"))
                .thenReturn((float) damageRemaining);
        when(gameState.getBattleState()).thenReturn(battleState);
        return gameStateWithGame(gameState, game);
    }

    private static GameState gameStateWithGame(GameState gameState, SwccgGame game) {
        GAME_BY_STATE.put(gameState, game);
        return gameState;
    }

    private static PhysicalCard card(String title, Zone zone, CardCategory category) {
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(blueprint.getTitle()).thenReturn(title);
        when(blueprint.getCardCategory()).thenReturn(category);

        PhysicalCard card = mock(PhysicalCard.class);
        when(card.getTitle()).thenReturn(title);
        when(card.getZone()).thenReturn(zone);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.getOwner()).thenReturn("tester");
        return card;
    }

    private static PhysicalCard forfeitCard(
            String title, Zone zone, CardCategory category, float forfeit) {
        PhysicalCard card = card(title, zone, category);
        when(card.getBlueprint().hasForfeitAttribute()).thenReturn(true);
        when(card.getBlueprint().getForfeit()).thenReturn(forfeit);
        return card;
    }

    private static com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext randoContext(
            GameState gameState, String prompt, List<String> cardIds, Phase phase) {
        var context = new com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext(
                gameState, "tester", "CARD_SELECTION", prompt, "force-loss", phase);
        context.setGame(GAME_BY_STATE.get(gameState));
        context.setCardIds(cardIds);
        context.setSelectable(java.util.Collections.nCopies(cardIds.size(), true));
        return context;
    }

    private static com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext chosenContext(
            GameState gameState, String prompt, List<String> cardIds, Phase phase) {
        var context = new com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext(
                gameState, "tester", "CARD_SELECTION", prompt, "force-loss", phase);
        context.setGame(GAME_BY_STATE.get(gameState));
        context.setCardIds(cardIds);
        context.setSelectable(java.util.Collections.nCopies(cardIds.size(), true));
        return context;
    }

    private static com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction action(
            List<com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction> actions,
            String actionId) {
        return actions.stream()
                .filter(action -> actionId.equals(action.getActionId()))
                .findFirst()
                .orElseThrow();
    }

    private static void assertActionParity(
            List<com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction> rando,
            List<com.gempukku.swccgo.ai.models.chosenone.evaluators.EvaluatedAction> chosen) {
        assertEquals(rando.size(), chosen.size());
        for (int i = 0; i < rando.size(); i++) {
            assertEquals(rando.get(i).getActionId(), chosen.get(i).getActionId());
            assertBits(rando.get(i).getScore(), chosen.get(i).getScore());
            assertEquals(rando.get(i).getReasoning(), chosen.get(i).getReasoning());
            assertEquals(rando.get(i).isHardVetoed(),
                    chosen.get(i).isHardVetoed());
            assertEquals(rando.get(i).getVetoReason(),
                    chosen.get(i).getVetoReason());
            assertEquals(rando.get(i).isDeferred(),
                    chosen.get(i).isDeferred());
            assertEquals(rando.get(i).getDeferReason(),
                    chosen.get(i).getDeferReason());
        }
    }

    private static void assertReasons(List<String> actual, String... expected) {
        assertEquals(List.of(expected), actual);
    }

    private static void assertBits(float expected, float actual) {
        assertEquals(Float.floatToRawIntBits(expected), Float.floatToRawIntBits(actual));
    }
}
