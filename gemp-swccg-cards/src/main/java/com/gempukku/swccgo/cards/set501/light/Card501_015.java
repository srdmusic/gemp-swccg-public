package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractDevice;
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
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.AttachCardFromTableEffect;
import com.gempukku.swccgo.logic.modifiers.KeywordModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotMoveAwayFromLocationModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotMoveFromLocationModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotMoveFromLocationToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.results.PlayCardResult;

import java.security.Key;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * Set: Set 23
 * Type: Device
 * Title: Bounty Puck
 */
public class Card501_015 extends AbstractDevice {
    public Card501_015() {
        super(Side.LIGHT, 0, PlayCardZoneOption.ATTACHED, Title.Bounty_Puck, Uniqueness.UNIQUE, ExpansionSet.SET_23, Rarity.V);
        setGameText("Deploy on Bounty Hunter's Guild. Unless on a character, if opponent just deployed a character of ability > 2 (except a Dark Jedi), relocate Bounty Puck to that character. While on a character, that character is \"The Asset\" and may not move away from same site as Din.");
        addIcons(Icon.VIRTUAL_SET_23);
        addKeywords(Keyword.DEPLOYS_ON_SITE);
        setTestingText("Bounty Puck");
    }

    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.Bounty_Hunters_Guild;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        Filter targetAttachedTo = Filters.and(Filters.hasAttached(self), Filters.character);
        modifiers.add(new KeywordModifier(self, targetAttachedTo, Keyword.THE_ASSET));
        modifiers.add(new MayNotMoveAwayFromLocationModifier(self, Filters.The_Asset, Filters.and(Filters.sameLocationAs(self, Filters.Din), Filters.site)));
        return modifiers;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        List<RequiredGameTextTriggerAction> actions = new LinkedList<>();
        final String opponent = game.getOpponent(self.getOwner());
        final PhysicalCard attactedTo = self.getAttachedTo();
        final Filter filter = Filters.and(Filters.opponents(self), Filters.character, Filters.abilityMoreThan(2), Filters.non_Dark_Jedi_character);

        if (Objects.equals(attactedTo.getTitle(), Title.Bounty_Hunters_Guild)
                && TriggerConditions.justDeployed(game, effectResult, opponent, filter)) {
            PhysicalCard deployedCard = ((PlayCardResult) effectResult).getPlayedCard();
            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Relocate to " + GameUtils.getFullName(deployedCard));
            action.setActionMsg("Relocate " + GameUtils.getCardLink(self) + " to " + GameUtils.getCardLink(deployedCard) +
                    "; " + GameUtils.getCardLink(deployedCard) + " is \"The Asset\" while " + GameUtils.getCardLink(self) + " attached");
            action.appendEffect(
                    new AttachCardFromTableEffect(action, self, deployedCard));
            actions.add(action);
        }

        return actions;
    }
}
