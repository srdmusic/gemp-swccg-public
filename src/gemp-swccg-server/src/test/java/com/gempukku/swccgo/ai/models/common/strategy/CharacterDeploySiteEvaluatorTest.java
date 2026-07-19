package com.gempukku.swccgo.ai.models.common.strategy;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgBuiltInCardBlueprint;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CharacterDeploySiteEvaluatorTest {

    private static final String ME = "me";
    private static final String OPPONENT = "opponent";

    private SwccgGame game;
    private GameState gameState;
    private ModifiersQuerying modifiers;

    @Before
    public void setUp() {
        game = mock(SwccgGame.class);
        gameState = mock(GameState.class);
        modifiers = mock(ModifiersQuerying.class);

        when(game.getGameState()).thenReturn(gameState);
        when(game.getModifiersQuerying()).thenReturn(modifiers);
        when(game.getOpponent(ME)).thenReturn(OPPONENT);
        when(gameState.getGame()).thenReturn(game);
        when(gameState.getCardsAtLocation(any(PhysicalCard.class))).thenReturn(Collections.emptyList());
        when(gameState.getAllPermanentCards()).thenReturn(Collections.emptyList());
        when(gameState.getLocationsInOrder()).thenReturn(Collections.emptyList());
        when(gameState.getAttachedCards(any(PhysicalCard.class))).thenReturn(Collections.emptyList());
        when(modifiers.isBattleground(eq(gameState), any(PhysicalCard.class), isNull())).thenReturn(false);
    }

    @Test
    public void nullAndMissingEngineFactsFailOpenToZero() {
        PhysicalCard character = character("Character", ME, 4f, 4f, 2f, 4f);
        PhysicalCard site = site("Generic Site");

        assertScore(0f, CharacterDeploySiteEvaluator.evaluateSite(
                null, character, site, ME, false, Collections.emptyList(), 8, 4, 0, false));
        assertScore(0f, CharacterDeploySiteEvaluator.evaluateSite(
                game, null, site, ME, false, Collections.emptyList(), 8, 4, 0, false));
        assertScore(0f, CharacterDeploySiteEvaluator.evaluateSite(
                game, character, null, ME, false, Collections.emptyList(), 8, 4, 0, false));
        assertScore(0f, CharacterDeploySiteEvaluator.evaluateSite(
                game, character, site, null, false, Collections.emptyList(), 8, 4, 0, false));

        when(game.getGameState()).thenReturn(null);
        assertScore(0f, CharacterDeploySiteEvaluator.evaluateSite(
                game, character, site, ME, false, Collections.emptyList(), 8, 4, 0, false));
    }

    @Test
    public void v151CoordinatedAttackReturnsExactFourHundred() {
        PhysicalCard site = site("Contested Site");
        PhysicalCard deploying = character("Lead", ME, 4f, 2f, 2f, 4f);
        PhysicalCard reinforcement = character("Reinforcement", ME, 1f, 4f, 2f, 3f);
        setPower(site, 0f, 6f);

        float score = evaluate(deploying, site, false,
                Collections.singletonList(reinforcement), 10, 4, 0, true);

        assertScore(400f, score);
    }

    @Test
    public void contestedLowAbilityCharacterWithBuddyReturnsExactMinusTwoHundred() {
        PhysicalCard site = site("Contested Site");
        PhysicalCard deploying = character("Low Ability", ME, 2f, 2f, 2f, 3f);
        PhysicalCard buddy = character("Buddy", ME, 2f, 2f, 2f, 3f);
        setPower(site, 0f, 6f);

        float score = evaluate(deploying, site, false,
                Collections.singletonList(buddy), 10, 4, 0, true);

        assertScore(-200f, score);
    }

    @Test
    public void v181CloseFairFightReturnsDrainWeightedTwoHundred() {
        PhysicalCard site = site("Drain Two Site");
        PhysicalCard deploying = character("Fair Fighter", ME, 4f, 5f, 2f, 4f);
        setPower(site, 0f, 7f);
        when(modifiers.getForceDrainAmount(gameState, site, OPPONENT)).thenReturn(2f);

        float score = evaluate(deploying, site, false,
                Collections.emptyList(), 8, 4, 0, true);

        assertScore(200f, score);
    }

    @Test
    public void v156WeakSoloHoldsAndStrongSoloPrefersAvailableBuddy() {
        PhysicalCard site = site("Battleground Site");
        PhysicalCard buddy = character("Buddy", ME, 1f, 1f, 2f, 2f);
        when(modifiers.isBattleground(gameState, site, null)).thenReturn(true);

        PhysicalCard weak = character("Weak Solo", ME, 3f, 3f, 2f, 3f);
        assertScore(-500f, evaluate(weak, site, false,
                Collections.singletonList(buddy), 8, 4, 0, false));

        PhysicalCard strong = character("Strong Solo", ME, 6f, 6f, 2f, 6f);
        assertScore(350f, evaluate(strong, site, false,
                Collections.singletonList(buddy), 8, 2, 0, false));
    }

    @Test
    public void strategicPositionOverstackPenaltyProducesExactCompositeTotal() {
        PhysicalCard site = site("Overstacked Battleground");
        PhysicalCard deploying = character("Additional Body", ME, 6f, 1f, 2f, 5f);
        when(modifiers.isBattleground(gameState, site, null)).thenReturn(true);
        setPower(site, 15f, 0f);

        float score = evaluate(deploying, site, false,
                Collections.emptyList(), 8, 4, 0, false);

        // Section A +500, section B +100 battleground -700 overstack, sections C/D 0.
        assertScore(-100f, score);
    }

    @Test
    public void friendlyPermanentWeaponAddsExactSectionCModifier() {
        PhysicalCard site = site("Effect-Protected Site");
        PhysicalCard deploying = character("Deploying Character", ME, 6f, 5f, 2f, 5f);
        PhysicalCard armedFriendly = character("Armed Friendly", ME, 1f, 1f, 2f, 2f);
        SwccgBuiltInCardBlueprint permanentWeapon = mock(SwccgBuiltInCardBlueprint.class);
        when(permanentWeapon.isWeapon()).thenReturn(true);
        when(modifiers.getPermanentWeapon(gameState, armedFriendly)).thenReturn(permanentWeapon);
        when(gameState.getCardsAtLocation(site)).thenReturn(Collections.singletonList(armedFriendly));

        float score = evaluate(deploying, site, false,
                Collections.emptyList(), 8, 4, 0, true);

        // Section A +500, section C +10, sections B/D 0.
        assertScore(510f, score);
    }

    @Test
    public void earlyThirdGroundBattlegroundAppliesExactSectionDGate() {
        PhysicalCard candidate = site("Third Battleground");
        PhysicalCard first = site("First Battleground");
        PhysicalCard second = site("Second Battleground");
        PhysicalCard firstOccupant = character("First Occupant", ME, 4f, 4f, 2f, 4f);
        PhysicalCard secondOccupant = character("Second Occupant", ME, 4f, 4f, 2f, 4f);
        PhysicalCard deploying = character("Strong Solo", ME, 6f, 5f, 2f, 5f);

        when(modifiers.isBattleground(eq(gameState), any(PhysicalCard.class), isNull())).thenReturn(true);
        when(gameState.getCardsAtLocation(candidate)).thenReturn(Collections.emptyList());
        when(gameState.getCardsAtLocation(first)).thenReturn(Collections.singletonList(firstOccupant));
        when(gameState.getCardsAtLocation(second)).thenReturn(Collections.singletonList(secondOccupant));
        when(gameState.getLocationsInOrder()).thenReturn(List.of(first, second));

        float score = evaluate(deploying, candidate, false,
                Collections.emptyList(), 8, 1, 0, false);

        // Section A +500, section B +100, section D -700, section C 0.
        assertScore(-100f, score);
    }

    private float evaluate(PhysicalCard deploying, PhysicalCard site,
                           boolean objectiveRelevant, List<PhysicalCard> hand,
                           int force, int turn, int deckShipCount, boolean perSiteEffectActive) {
        return CharacterDeploySiteEvaluator.evaluateSite(
                game, deploying, site, ME, objectiveRelevant, hand,
                force, turn, deckShipCount, perSiteEffectActive);
    }

    private void setPower(PhysicalCard site, float ourPower, float opponentPower) {
        when(modifiers.getTotalPowerAtLocation(gameState, site, ME, false, false)).thenReturn(ourPower);
        when(modifiers.getTotalPowerAtLocation(gameState, site, OPPONENT, false, false)).thenReturn(opponentPower);
    }

    private static PhysicalCard character(String title, String owner, float ability,
                                          float power, float deployCost, float forfeit) {
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(blueprint.getCardCategory()).thenReturn(CardCategory.CHARACTER);
        when(blueprint.getAbility()).thenReturn(ability);
        when(blueprint.hasAbilityAttribute()).thenReturn(true);
        when(blueprint.getPower()).thenReturn(power);
        when(blueprint.hasPowerAttribute()).thenReturn(true);
        when(blueprint.getDeployCost()).thenReturn(deployCost);
        when(blueprint.getForfeit()).thenReturn(forfeit);
        when(blueprint.hasForfeitAttribute()).thenReturn(true);
        when(blueprint.getTitle()).thenReturn(title);
        when(blueprint.getTitles()).thenReturn(Collections.singletonList(title));

        PhysicalCard card = mock(PhysicalCard.class);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.getTitle()).thenReturn(title);
        when(card.getOwner()).thenReturn(owner);
        return card;
    }

    private static PhysicalCard site(String title) {
        SwccgCardBlueprint blueprint = mock(SwccgCardBlueprint.class);
        when(blueprint.getCardCategory()).thenReturn(CardCategory.LOCATION);
        when(blueprint.getCardSubtype()).thenReturn(CardSubtype.SITE);
        when(blueprint.getTitle()).thenReturn(title);
        when(blueprint.getTitles()).thenReturn(Collections.singletonList(title));

        PhysicalCard card = mock(PhysicalCard.class);
        when(card.getBlueprint()).thenReturn(blueprint);
        when(card.getTitle()).thenReturn(title);
        return card;
    }

    private static void assertScore(float expected, float actual) {
        assertEquals(expected, actual, 0.001f);
    }
}
