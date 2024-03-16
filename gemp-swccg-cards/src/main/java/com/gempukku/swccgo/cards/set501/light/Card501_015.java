package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.InPlayDataSetCondition;
import com.gempukku.swccgo.cards.effects.SetWhileInPlayDataEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.PlayCardOptionId;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.WhileInPlayData;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.AttachCardFromTableEffect;
import com.gempukku.swccgo.logic.modifiers.KeywordModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.results.PlayCardResult;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 23
 * Type: Effect
 * Title: Holopuck
 */
public class Card501_015 extends AbstractNormalEffect {
    public Card501_015() {
        super(Side.LIGHT, 0, PlayCardZoneOption.ATTACHED, Title.Holopuck, Uniqueness.UNIQUE, ExpansionSet.SET_23, Rarity.V);
        setGameText("Deploy on Bounty Hunter's Guild. " +
                "Unless on a character, if opponent just deployed a non-Dark Jedi character of ability > 2 (or a leader) to a site, " +
                "relocate Bounty Puck to that character. That character is 'The Asset' for as long as that character remains on table.");
        addIcons(Icon.VIRTUAL_SET_23);
        addKeywords(Keyword.DEPLOYS_ON_SITE);
        setTestingText("Holopuck");
    }

    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.Bounty_Hunters_Guild;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new KeywordModifier(self, Filters.isInCardInPlayData(self), new InPlayDataSetCondition(self), Keyword.THE_ASSET));
        return modifiers;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        List<RequiredGameTextTriggerAction> actions = new LinkedList<>();
        final String opponent = game.getOpponent(self.getOwner());
        final Filter filter = Filters.and(Filters.opponents(self), Filters.character, Filters.or(Filters.abilityMoreThan(2), Filters.leader), Filters.non_Dark_Jedi_character);

        if (!GameConditions.isAttachedTo(game, self, Filters.character)
                && TriggerConditions.justDeployedToLocation(game, effectResult, opponent, filter, Filters.site)) {
            PhysicalCard deployedCard = ((PlayCardResult) effectResult).getPlayedCard();
            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Make " + GameUtils.getCardLink(deployedCard) + " 'The Asset'");
            action.setActionMsg("Make " + GameUtils.getCardLink(deployedCard) + " 'The Asset'");
            action.appendEffect(
                    new AttachCardFromTableEffect(action, self, deployedCard));
            action.appendEffect(
                    new SetWhileInPlayDataEffect(action, self, new WhileInPlayData(deployedCard)));
            actions.add(action);
        }

        return actions;
    }

    @Override
    public String getDisplayableInformation(SwccgGame game, PhysicalCard self) {
        if (self.getWhileInPlayData() != null
                && self.getWhileInPlayData().getPhysicalCard() != null) {
            return GameUtils.getCardLink(self.getWhileInPlayData().getPhysicalCard()) + " is 'The Asset'";
        }
        return null;
    }
}
