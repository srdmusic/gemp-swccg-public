package com.gempukku.swccgo.cards.set501.light;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerPhaseEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.LoseForceEffect;
import com.gempukku.swccgo.logic.modifiers.InitiateBattlesForFreeModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotDeployModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.ResetPersonalForceGenerationModifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

/**
 * Set: Playtesting
 * Type: Effect
 * Title: Commando Training (V)
 */
public class Card501_201 extends AbstractNormalEffect {
    public Card501_201() {
        super(Side.LIGHT, 4, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, Title.Commando_Training, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Han's Rebel strike team on the forest moon of Endor was highly trained in the use of blasters and explosives.");
        setGameText("Deploy on table. Your personal Force generation = 2. You initiate battles for free. During your control phase, if you occupy four battlegrounds and/or opponent's locations, opponent loses 1 Force. You may not deploy characters except Rebels or droids. (Immune to Alter.)");
        addIcons(Icon.ENDOR, Icon.VIRTUAL_SET_27);
        addImmuneToCardTitle(Title.Alter);
        setVirtualSuffix(true);
        setTestingText("Commando Training (V)");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        String playerId = self.getOwner();
        modifiers.add(new ResetPersonalForceGenerationModifier(self, 2, self.getOwner()));
        modifiers.add(new InitiateBattlesForFreeModifier(self, playerId));
        Filter characterFilter = Filters.and(Filters.character, Filters.not(Filters.or(Filters.Rebel, Filters.droid)));
        modifiers.add(new MayNotDeployModifier(self, characterFilter, playerId));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        final String opponent = game.getOpponent(playerId);

        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;
        Filter siteFilter = Filters.or(Filters.battleground_site, Filters.and(Filters.opponents(self), Filters.location));

        // Check condition(s)
        if (GameConditions.isOnceDuringYourPhase(game, self, playerId, gameTextSourceCardId, gameTextActionId, Phase.CONTROL)
                && GameConditions.occupies(game, playerId, 4, siteFilter)) {
            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Make opponent lose 1 Force");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerPhaseEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new LoseForceEffect(action, opponent, 1));
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        final String playerId = self.getOwner();
        final String opponent = game.getOpponent(playerId);

        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;
        Filter siteFilter = Filters.or(Filters.battleground_site, Filters.and(Filters.opponents(self), Filters.location));

        // Check condition(s)
        if (TriggerConditions.isEndOfYourPhase(game, self, effectResult, Phase.CONTROL)
                && GameConditions.occupies(game, playerId, 4, siteFilter)) {

            RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Make opponent lose 1 Force");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerPhaseEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new LoseForceEffect(action, opponent, 1));
            return Collections.singletonList(action);
        }
        return null;
    }
}
