package com.gempukku.swccgo.cards.set501.dark;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.ConvertLocationByRaisingToTopEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
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
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.modifiers.MayNotBeTargetedByModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

/**
 * Set: Playtesting
 * Type: Effect
  * Title: Jabba's Influence (V)
 */
public class Card501_034 extends AbstractNormalEffect {
    public Card501_034() {
        super(Side.DARK, 3, PlayCardZoneOption.ATTACHED, Title.Jabbas_Influence, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Jabba makes offers one cannot refuse. Smugglers, thieves and competitors who do not acquiesce have been rumored to wake up with a bantha's head in their bed.");
        setGameText("Deploy on Audience Chamber. Opponent may not target this site with I Must Be Allowed To Speak. Once per turn, may raise your converted Audience Chamber to the top. If opponent just deployed a character here, may place a card from hand on Force Pile. [Immune to Alter.]");
        addKeywords(Keyword.DEPLOYS_ON_SITE);
        addIcons(Icon.SPECIAL_EDITION, Icon.JABBAS_PALACE, Icon.VIRTUAL_SET_25);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("Jabba's Influence (V)");
        setVirtualSuffix(true);
    }

    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.Audience_Chamber;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new MayNotBeTargetedByModifier(self, Filters.hasAttached(self), Filters.title(Title.I_Must_Be_Allowed_To_Speak)));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(String playerId, SwccgGame game, PhysicalCard self, int gameTextSourceCardId) {
        Filter raisableAudienceChamber = Filters.and(Filters.canBeConvertedByRaisingYourLocationToTop(playerId), Filters.Audience_Chamber);

        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;
        // Check condition(s)
        if (GameConditions.canTarget(game, self, raisableAudienceChamber)
                && GameConditions.isOncePerTurn(game, self, playerId, gameTextSourceCardId, gameTextActionId)) {
                    
            final PhysicalCard audienceChamberCard = Filters.findFirstActive(game, self, raisableAudienceChamber);
            if (audienceChamberCard != null) {

                final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
                action.setText("Raise Audience Chamber to the top");
                action.setActionMsg("Raise converted Audience Chamber to the top");
                action.addAnimationGroup(audienceChamberCard);
                // Update usage limit(s)
                action.appendUsage(
                        new OncePerTurnEffect(action));
                // Perform result(s)
                action.appendEffect(
                        new ConvertLocationByRaisingToTopEffect(action, audienceChamberCard, true));
                return Collections.singletonList(action);
            }
        }
        return null;
    }
}