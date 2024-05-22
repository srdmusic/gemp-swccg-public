package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractRebel;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.WithCondition;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Species;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.conditions.OrCondition;
import com.gempukku.swccgo.logic.effects.LoseForceEffect;
import com.gempukku.swccgo.logic.effects.PutStackedCardInUsedPileEffect;
import com.gempukku.swccgo.logic.effects.choose.ChooseStackedCardEffect;
import com.gempukku.swccgo.logic.modifiers.ImmuneToAttritionLessThanModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotBeTargetedByPermanentWeaponsModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.TotalBattleDestinyModifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
 * Set: Set 23
 * Type: Character
 * Subtype: Rebel
 * Title: Sabine, Padawan Learner
 */
public class Card501_191 extends AbstractRebel {
    public Card501_191() {
        super(Side.LIGHT, 2, 4, 4, 4, 6, "Sabine, Padawan Learner", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Female Mandalorian Padawan.");
        setGameText("If Sabine just deployed (or won a battle), may place an artwork card in owner's Used Pile. Characters Sabine 'hits' are forfeit -4. While with Ahsoka or Ezra, your battle destiny here is +1 (+2 if both). Immune to [Permanent Weapon] and attrition <3");
        setArmor(5);
        addIcons(Icon.PILOT, Icon.WARRIOR, Icon.VIRTUAL_SET_23);
        addKeywords(Keyword.FEMALE, Keyword.PADAWAN);
        setSpecies(Species.MANDALORIAN);
        addPersona(Persona.SABINE);
        setTestingText("Sabine, Padawan Learner");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        String playerId = self.getOwner();
        List<Modifier> modifiers = new ArrayList<>();
        Condition withAhsoka = new WithCondition(self, Filters.Ahsoka);
        Condition withEzra = new WithCondition(self, Filters.Ezra);
        modifiers.add(new TotalBattleDestinyModifier(self, Filters.here(self), new OrCondition(withAhsoka, withEzra), 1, playerId));
        modifiers.add(new MayNotBeTargetedByPermanentWeaponsModifier(self));
        modifiers.add(new ImmuneToAttritionLessThanModifier(self, 3));
        return modifiers;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.SABINE_PADAWAN__TWO_FORCE;
        if (TriggerConditions.justHitBy(game, effectResult, Filters.character, self)
                && GameConditions.isOncePerGame(game, self, gameTextActionId)) {
            String opponent = game.getOpponent(self.getOwner());
            RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Make opponent lose 2 Force");
            action.appendUsage(
                new OncePerGameEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new LoseForceEffect(action, opponent, 2));
            return Collections.singletonList(action);        
        }
        return null;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(final String playerId, final SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        if ((TriggerConditions.justDeployed(game, effectResult, self)
                || TriggerConditions.wonBattleAt(game, effectResult, playerId, Filters.sameLocation(self)))
                && GameConditions.canSpot(game, self, Filters.and(Filters.Thrawns_Art_Collection, Filters.hasStacked(Filters.any)))) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Place artwork card in Used Pile");
            action.setActionMsg("Place card stacked on Thrawn's Art Collection in Used Pile");

            action.appendEffect(new ChooseStackedCardEffect(action, playerId, Filters.Thrawns_Art_Collection) {
                @Override
                protected void cardSelected(final PhysicalCard selectedCard) {
                    action.appendEffect(
                            new PutStackedCardInUsedPileEffect(action, playerId, selectedCard, false));
                }
            });
            return Collections.singletonList(action);
        }
        return null;
    }
}
