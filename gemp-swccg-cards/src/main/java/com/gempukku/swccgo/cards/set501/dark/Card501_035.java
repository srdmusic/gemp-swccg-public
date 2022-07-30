package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.ControlsWithCondition;
import com.gempukku.swccgo.cards.effects.usage.OncePerBattleEffect;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.conditions.AndCondition;
import com.gempukku.swccgo.logic.effects.choose.DeployCardToLocationFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.DeployCostToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.MayDeployOtherCardsAsReactToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 19
 * Type: Location
 * Subtype: Site
 * Title: Lothal: Imperial Complex
 */
public class Card501_035 extends AbstractSite {
    public Card501_035() {
        super(Side.DARK, Title.Lothal_Imperial_Complex, Title.Lothal);
        setLocationDarkSideGameText("If you control with a leader, once per battle at a related site, you may deploy a card as a ‘react’");
        setLocationLightSideGameText("Your non-Rebel characters deploy +1 here.");
        addIcon(Icon.DARK_FORCE, 2);
        addIcon(Icon.LIGHT_FORCE, 0);
        addIcons(Icon.INTERIOR_SITE, Icon.PLANET, Icon.SCOMP_LINK, Icon.VIRTUAL_SET_19);
        setTestingText("Lothal: Imperial Complex");
    }

    @Override
    protected List<Modifier> getGameTextDarkSideWhileActiveModifiers(String playerOnDarkSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new MayDeployOtherCardsAsReactToLocationModifier(self, "Deploy a card as a 'react'", new AndCondition(new ControlsWithCondition(playerOnDarkSideOfLocation, self, Filters.leader)), playerOnDarkSideOfLocation, Filters.any, Filters.battleLocation));
        return modifiers;
    }

    @Override
    protected List<Modifier> getGameTextLightSideWhileActiveModifiers(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new DeployCostToLocationModifier(self, Filters.and(Filters.your(playerOnLightSideOfLocation), Filters.character, Filters.not(Filters.Rebel)),1, Filters.here(self)));
        return modifiers;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextDarkSideOptionalAfterTriggers(String playerOnDarkSideOfLocation, SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        if(TriggerConditions.battleInitiatedAt(game, effectResult, Filters.relatedSite(self))
            && GameConditions.controlsWith(game, playerOnDarkSideOfLocation, self, Filters.leader)
            && GameConditions.isOncePerBattle(game, self, playerOnDarkSideOfLocation, gameTextSourceCardId, gameTextActionId)){
            OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, playerOnDarkSideOfLocation, gameTextSourceCardId, gameTextActionId);
            action.appendUsage(
                    new OncePerBattleEffect(action)
            );
            action.appendEffect(
                    new DeployCardToLocationFromReserveDeckEffect(action, Filters.any, Filters.battleLocation, false, true, true)
            );
            return Collections.singletonList(action);
       }
       return null;
    }
}
