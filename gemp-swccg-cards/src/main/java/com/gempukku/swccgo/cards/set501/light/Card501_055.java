package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractAlien;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.DestinyType;
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
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.ModifyDestinyEffect;
import com.gempukku.swccgo.logic.effects.ResetForfeitUntilEndOfTurnEffect;
import com.gempukku.swccgo.logic.modifiers.IgnoresDeploymentRestrictionsFromCardModifier;
import com.gempukku.swccgo.logic.modifiers.ImmuneToAttritionLessThanModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotBeTargetedByModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.results.DestinyDrawnResult;
import com.gempukku.swccgo.logic.timing.results.HitResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Set: Set 22
 * Type: Character
 * Subtype: Alien
 * Title: Ahsoka, Friend Of The Family
 */
public class Card501_055 extends AbstractAlien {
    public Card501_055() {
        super(Side.LIGHT, 1, 5, 5, 6, 7, "Ahsoka, Friend Of The Family", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Female Togruta.");
        setGameText("Ignores [Sk] Epic Event deployment restrictions. " +
                    "Once per turn, may subtract 1 from a non-weapon destiny targeting your character's ability or defense value. " +
                    "Characters she hits are forfeit = 0. Immune to non-lightsaber weapons and attrition < 5.");
        addIcons(Icon.PILOT, Icon.WARRIOR, Icon.VIRTUAL_SET_22);
        addKeywords(Keyword.FEMALE);
        setSpecies(Species.TOGRUTA);
        addPersona(Persona.AHSOKA);
        setTestingText("Ahsoka, Friend Of The Family");
    }

    @Override
    protected List<Modifier> getGameTextAlwaysOnModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new ArrayList<>();
        modifiers.add(new IgnoresDeploymentRestrictionsFromCardModifier(self, self,  null, self.getOwner(), Filters.and(Icon.SKYWALKER, Filters.Epic_Event)));
        return modifiers;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new ArrayList<>();
        modifiers.add(new MayNotBeTargetedByModifier(self, Filters.and(Filters.weapon, Filters.not(Filters.lightsaber))));
        modifiers.add(new ImmuneToAttritionLessThanModifier(self, 5));
        return modifiers;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(String playerId, SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;
        if(TriggerConditions.isDestinyJustDrawnTargetingAbilityManeuverOrDefenseValue(game, effectResult, Filters.and(Filters.your(playerId), Filters.character))
            && GameConditions.isOncePerTurn(game, self, gameTextSourceCardId, gameTextActionId)){

            DestinyDrawnResult destinyDrawnResult = (DestinyDrawnResult) effectResult;
            DestinyType destinyType = destinyDrawnResult.getDestinyType();

            if(destinyType != DestinyType.WEAPON_DESTINY){
                OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
                action.appendUsage(
                        new OncePerTurnEffect(action)
                );
                action.appendEffect(
                        new ModifyDestinyEffect(action, -1)
                );
                return Collections.singletonList(action);
            }
        }
        return null;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        if(TriggerConditions.justHit(game, effectResult, Filters.character)){
            final PhysicalCard card = ((HitResult) effectResult).getCardHit();

            RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Make " + GameUtils.getCardLink(card) + " forfeit = 0");
            action.setActionMsg("Make " + GameUtils.getCardLink(card) + " forfeit = 0");
            action.appendEffect(
                    new ResetForfeitUntilEndOfTurnEffect(action, self, 0)
            );
            return Collections.singletonList(action);
        }
        return null;
    }
}
