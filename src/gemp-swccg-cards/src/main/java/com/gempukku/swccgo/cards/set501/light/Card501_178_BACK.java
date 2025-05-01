package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractObjective;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.conditions.TrueCondition;
import com.gempukku.swccgo.logic.effects.RetrieveForceEffect;
import com.gempukku.swccgo.logic.modifiers.JediTestSuspendedInsteadOfLostModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.ModifyGameTextModifier;
import com.gempukku.swccgo.logic.modifiers.ModifyGameTextType;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * Set: Playtesting
 * Type: Objective
 * Title: Mind What You Have Learned / Save You It Can
 */
public class Card501_178_BACK extends AbstractObjective {
    public Card501_178_BACK() {
        super(Side.LIGHT, 7, Title.Save_You_It_Can, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("Immediately return Luke and any cards on him to owner's hand. While this side up, Luke may deploy -3 with a weapon as a 'react.' If Luke just won a battle, may place a card on Patience! out of play to retrieve 1 Force. When your [Cloud City] Rebel Force drains at a battleground site, unless a captive on table, lost Force must come from bottom of Reserve Deck if possible. Once per game, may place a completed Jedi Test out of play to take Luke into hand from Lost Pile.");
        addIcons(Icon.SPECIAL_EDITION, Icon.DAGOBAH, Icon.VIRTUAL_SET_25);
        setVirtualSuffix(true);
        setTestingText("Save You It Can (V)");
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(final SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        final String playerId = self.getOwner();

        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        // Check condition(s)
        if (TriggerConditions.cardFlipped(game, effectResult, self)) {

            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Return Luke and cards on him to hand");
            action.setActionMsg("Return Luke and any cards on him to owner's hand");
            // Perform result(s)
            action.appendEffect(
                    new RetrieveForceEffect(action, playerId, 10));
            return Collections.singletonList(action);
        }

        return null;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {

        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_2;

        return null;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new ModifyGameTextModifier(self, Filters.Jedi_Test_5, ModifyGameTextType.IT_IS_THE_FUTURE_YOU_SEE__STACK_DESTINY_CARD_ON_JEDI_TEST_5));
        modifiers.add(new JediTestSuspendedInsteadOfLostModifier(self, Filters.any, new TrueCondition()));
        return modifiers;
    }
}