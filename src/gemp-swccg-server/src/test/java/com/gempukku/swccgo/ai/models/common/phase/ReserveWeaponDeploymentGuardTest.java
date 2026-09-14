package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.*;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.game.state.actions.GameTextActionState;
import com.gempukku.swccgo.logic.actions.AbstractAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.decisions.CardActionSelectionDecision;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromPileEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardToTargetFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class ReserveWeaponDeploymentGuardTest {
    private static final String PLAYER = "~Rando_Cal";

    @Test
    public void abbreviatedNamedParentBlocksAlreadyArmedMara() {
        Fixture f = new Fixture();
        f.arm(f.mara);
        TopLevelGameTextAction action = f.named();
        assertTrue(ReserveWeaponDeploymentGuard.blocksAction(f.game, PLAYER, action));
        assertEquals(Set.of("0"), ReserveWeaponDeploymentGuard.readBlockedActions(decision(action), f.game, PLAYER));
    }

    @Test
    public void abbreviatedNamedParentStillEquipsUnarmedMara() {
        Fixture f = new Fixture();
        assertFalse(ReserveWeaponDeploymentGuard.blocksAction(f.game, PLAYER, f.named()));
    }

    @Test
    public void unrelatedUnarmedCharacterDoesNotRescueWrongExactTarget() {
        Fixture f = new Fixture();
        f.arm(f.mara);
        f.legalHosts.add(f.other);
        assertTrue(ReserveWeaponDeploymentGuard.blocksAction(f.game, PLAYER, f.named()));
    }

    @Test
    public void genericWeaponCanUseAnotherLegalUnarmedCharacter() {
        Fixture f = new Fixture();
        f.arm(f.mara);
        f.legalHosts.add(f.other);
        assertFalse(ReserveWeaponDeploymentGuard.blocksAction(f.game, PLAYER, f.generic(false)));
    }

    @Test
    public void genericMixedHallwayPullIsPreservedOnlyWhenHallwayIsLegal() {
        Fixture f = new Fixture();
        f.arm(f.mara);
        f.reserve.add(f.hallway);
        f.hallwayLegal = true;
        assertFalse(ReserveWeaponDeploymentGuard.blocksAction(f.game, PLAYER, f.generic(true)));
        f.hallwayLegal = false;
        assertTrue(ReserveWeaponDeploymentGuard.blocksAction(f.game, PLAYER, f.generic(true)));
    }

    @Test
    public void permanentWeaponAlsoOccupiesCharacterSlot() {
        Fixture f = new Fixture();
        when(f.modifiers.getPermanentWeapon(f.state, f.mara)).thenReturn(mock(SwccgBuiltInCardBlueprint.class));
        assertTrue(ReserveWeaponDeploymentGuard.blocksAction(f.game, PLAYER, f.named()));
    }

    @Test
    public void nonCharacterWeaponTargetsAreNotGivenNewRestriction() {
        Fixture f = new Fixture();
        when(f.mara.getBlueprint().getCardCategory()).thenReturn(CardCategory.STARSHIP);
        f.arm(f.mara);
        assertFalse(ReserveWeaponDeploymentGuard.blocksAction(f.game, PLAYER, f.named()));
    }

    @Test
    public void childUsesExactRouteInsteadOfCountingUnrelatedCharacters() {
        Fixture f = new Fixture();
        f.arm(f.mara);
        f.legalHosts.add(f.other);
        GameTextActionState namedState = new GameTextActionState(1, f.named());
        when(f.state.getTopGameTextActionState()).thenReturn(namedState);
        assertTrue(ReserveWeaponDeploymentGuard.candidateBlockReason(f.game, PLAYER, f.saber).orElseThrow().contains("no legal target"));
        GameTextActionState genericState = new GameTextActionState(2, f.generic(false));
        when(f.state.getTopGameTextActionState()).thenReturn(genericState);
        assertEquals("", ReserveWeaponDeploymentGuard.candidateBlockReason(f.game, PLAYER, f.saber).orElseThrow());
    }

    @Test
    public void otherPilesAndUnmaterializedActionsRemainOutsideParentGuard() {
        Fixture f = new Fixture();
        f.arm(f.mara);
        TopLevelGameTextAction action = f.action("Deploy weapon from hand");
        action.appendEffect(new DeployCardFromPileEffect(action, PLAYER, Zone.HAND, sameCard(f.saber),
                sameCard(f.mara), null, null, null, false, null, 0, null, null, null, false, false));
        assertFalse(ReserveWeaponDeploymentGuard.blocksAction(f.game, PLAYER, action));
        assertFalse(ReserveWeaponDeploymentGuard.blocksAction(f.game, PLAYER, f.action("Use game text")));
    }

    @Test
    public void inspectionDoesNotConsumeEffectsOrRewriteTheirFilters() throws Exception {
        Fixture f = new Fixture();
        f.arm(f.mara);
        TopLevelGameTextAction action = f.named();
        Field effectsField = AbstractAction.class.getDeclaredField("_effects");
        effectsField.setAccessible(true);
        List<?> before = new ArrayList<>((List<?>) effectsField.get(action));
        Field filterField = DeployCardFromPileEffect.class.getDeclaredField("_cardFilter");
        filterField.setAccessible(true);
        Object originalFilter = filterField.get(before.get(0));
        assertTrue(ReserveWeaponDeploymentGuard.blocksAction(f.game, PLAYER, action));
        assertEquals(before, effectsField.get(action));
        assertSame(originalFilter, filterField.get(before.get(0)));
    }

    @Test
    public void bothParentAdaptersApplyBlockBeforeTextBonuses() {
        Fixture f = new Fixture();
        f.arm(f.mara);
        Set<String> blocks = ReserveWeaponDeploymentGuard.readBlockedActions(decision(f.named()), f.game, PLAYER);
        var rando = new com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext(
                f.state, PLAYER, "CARD_ACTION_CHOICE", "Choose action", "1", Phase.DEPLOY);
        rando.setGame(f.game);
        rando.setActionIds(List.of("0"));
        rando.setActionTexts(List.of("Deploy Mara Jade's Lightsaber"));
        rando.setCardIds(List.of("227"));
        rando.setExtra(ReserveWeaponDeploymentGuard.BLOCKED_ACTIONS_EXTRA, blocks);
        var r = new com.gempukku.swccgo.ai.models.rando.evaluators.ActionTextEvaluator().evaluate(rando).get(0);
        var chosen = new com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext(
                f.state, PLAYER, "CARD_ACTION_CHOICE", "Choose action", "1", Phase.DEPLOY);
        chosen.setGame(f.game);
        chosen.setActionIds(List.of("0"));
        chosen.setActionTexts(List.of("Deploy Mara Jade's Lightsaber"));
        chosen.setCardIds(List.of("227"));
        chosen.setExtra(ReserveWeaponDeploymentGuard.BLOCKED_ACTIONS_EXTRA, blocks);
        var c = new com.gempukku.swccgo.ai.models.chosenone.evaluators.ActionTextEvaluator().evaluate(chosen).get(0);
        assertTrue(r.getReasoningString(), r.isDeferred());
        assertEquals(-9999, r.getScore(), 0.01);
        assertEquals(r.getScore(), c.getScore(), 0.01);
        // A later evaluator cannot buy its way around the categorical parent gate.
        r.addReasoning("Large unrelated bonus", 1_000_000);
        c.addReasoning("Large unrelated bonus", 1_000_000);
        rando.setNoPass(false);
        chosen.setNoPass(false);
        var re = mock(com.gempukku.swccgo.ai.models.rando.evaluators.ActionEvaluator.class);
        when(re.isEnabled()).thenReturn(true);
        when(re.canEvaluate(rando)).thenReturn(true);
        when(re.evaluate(rando)).thenReturn(List.of(r));
        var rc = new com.gempukku.swccgo.ai.models.rando.evaluators.CombinedEvaluator();
        rc.getEvaluators().clear();
        rc.addEvaluator(re);
        rc.addEvaluator(new com.gempukku.swccgo.ai.models.rando.evaluators.PassEvaluator());
        assertEquals("", rc.evaluateDecision(rando).getActionId());
        var ce = mock(com.gempukku.swccgo.ai.models.chosenone.evaluators.ActionEvaluator.class);
        when(ce.isEnabled()).thenReturn(true);
        when(ce.canEvaluate(chosen)).thenReturn(true);
        when(ce.evaluate(chosen)).thenReturn(List.of(c));
        var cc = new com.gempukku.swccgo.ai.models.chosenone.evaluators.CombinedEvaluator();
        cc.getEvaluators().clear();
        cc.addEvaluator(ce);
        cc.addEvaluator(new com.gempukku.swccgo.ai.models.chosenone.evaluators.PassEvaluator());
        assertEquals("", cc.evaluateDecision(chosen).getActionId());
    }

    private static CardActionSelectionDecision decision(TopLevelGameTextAction action) {
        return new CardActionSelectionDecision(1, "Choose action", List.of(action), true, false, false, false, false) {
            @Override public void decisionMade(String result) { }
        };
    }

    private static Filter sameCard(PhysicalCard expected) {
        return new Filter() {
            @Override public boolean accepts(GameState state, ModifiersQuerying modifiers, PhysicalCard card) {
                return card == expected;
            }
        };
    }

    private static PhysicalCard card(int id, String title, String blueprintId, CardCategory category, Zone zone) {
        PhysicalCard card = mock(PhysicalCard.class);
        SwccgCardBlueprint bp = mock(SwccgCardBlueprint.class);
        when(card.getBlueprint()).thenReturn(bp);
        when(card.getBlueprintId(anyBoolean())).thenReturn(blueprintId);
        when(card.getPermanentCardId()).thenReturn(id);
        when(card.getCardId()).thenReturn(id);
        when(card.getOwner()).thenReturn(PLAYER);
        when(card.getZone()).thenReturn(zone);
        when(card.getTitle()).thenReturn(title);
        when(bp.getTitle()).thenReturn(title);
        when(bp.getCardCategory()).thenReturn(category);
        return card;
    }

    private static final class Fixture {
        final SwccgGame game = mock(SwccgGame.class);
        final GameState state = mock(GameState.class);
        final ModifiersQuerying modifiers = mock(ModifiersQuerying.class);
        final PhysicalCard mara = card(227, "Mara Jade, The Emperor's Hand", "226_8", CardCategory.CHARACTER, Zone.AT_LOCATION);
        final PhysicalCard other = card(231, "Other character", "1_1", CardCategory.CHARACTER, Zone.AT_LOCATION);
        final PhysicalCard saber = card(229, "Mara Jade's Lightsaber", "110_11", CardCategory.WEAPON, Zone.RESERVE_DECK);
        final PhysicalCard hallway = card(232, "Mobile Hallway", "1_2", CardCategory.LOCATION, Zone.RESERVE_DECK);
        final List<PhysicalCard> reserve = new ArrayList<>(List.of(saber));
        final List<PhysicalCard> legalHosts = new ArrayList<>(List.of(mara));
        boolean hallwayLegal;

        Fixture() {
            when(game.getGameState()).thenReturn(state);
            when(game.getModifiersQuerying()).thenReturn(modifiers);
            when(state.getReserveDeck(PLAYER)).thenReturn(reserve);
            when(state.getAllPermanentCards()).thenReturn(List.of(mara, other, saber, hallway));
            when(state.findCardByPermanentId(anyInt())).thenAnswer(inv -> Map.of(227, mara, 231, other, 229, saber, 232, hallway).get(inv.getArgument(0)));
            when(state.findCardById(anyInt())).thenAnswer(inv -> Map.of(227, mara, 231, other, 229, saber, 232, hallway).get(inv.getArgument(0)));
            when(state.getCurrentPlayerId()).thenReturn(PLAYER);
            when(modifiers.isDeployableToTarget(same(state), same(mara), any(PhysicalCard.class), eq(false),
                    any(Filter.class), anyBoolean(), anyFloat(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(false), eq(0.0f)))
                    .thenAnswer(inv -> {
                        PhysicalCard candidate = inv.getArgument(2);
                        if (candidate == hallway) return hallwayLegal;
                        Filter targets = inv.getArgument(4);
                        return legalHosts.stream().anyMatch(host -> targets.accepts(game, host));
                    });
            when(modifiers.isDeployable(same(state), same(mara), any(PhysicalCard.class), eq(false),
                    isNull(), anyBoolean(), anyFloat(), isNull(), isNull(), isNull(), isNull(), isNull(), eq(false), eq(0.0f)))
                    .thenAnswer(inv -> inv.getArgument(2) == hallway ? hallwayLegal : !legalHosts.isEmpty());
        }

        void arm(PhysicalCard target) {
            PhysicalCard weapon = card(228, "Existing weapon", "221_9", CardCategory.WEAPON, Zone.ATTACHED);
            when(state.getAttachedCards(target)).thenReturn(List.of(weapon));
        }

        TopLevelGameTextAction action(String text) {
            TopLevelGameTextAction action = new TopLevelGameTextAction(mara, mara.getCardId());
            action.setText(text);
            return action;
        }

        TopLevelGameTextAction named() {
            TopLevelGameTextAction action = action("Deploy Mara Jade's Lightsaber");
            action.appendEffect(new DeployCardToTargetFromReserveDeckEffect(action, sameCard(saber), sameCard(mara), true));
            return action;
        }

        TopLevelGameTextAction generic(boolean mixed) {
            TopLevelGameTextAction action = action("Deploy card from Reserve Deck");
            action.appendEffect(new DeployCardFromReserveDeckEffect(action,
                    mixed ? Filters.or(sameCard(saber), sameCard(hallway)) : sameCard(saber), true));
            return action;
        }
    }
}
