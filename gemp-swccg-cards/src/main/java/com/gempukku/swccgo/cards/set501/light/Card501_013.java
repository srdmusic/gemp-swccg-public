package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.AttachCardFromTableEffect;
import com.gempukku.swccgo.logic.modifiers.DeployCostToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.PassthruEffect;
import com.gempukku.swccgo.logic.timing.results.AboutToLeaveTableResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 23
 * Type: Location
 * Subtype: Site
 * Title: Nevarro City: Bounty Hunter's Guild
 */
public class Card501_013 extends AbstractSite {
    public Card501_013() {
        super(Side.LIGHT, Title.Bounty_Hunters_Guild, Title.Nevarro, Uniqueness.UNIQUE, ExpansionSet.SET_23, Rarity.V);
        setLocationLightSideGameText("If Bounty Puck about to be stolen or leave table (for any reason, even if inactive), relocate it here.");
        setLocationDarkSideGameText("Your bounty hunters deploy -1 here.");
        addIcon(Icon.LIGHT_FORCE, 2);
        addIcon(Icon.DARK_FORCE, 1);
        addIcons(Icon.INTERIOR_SITE, Icon.PLANET, Icon.MUDHORN);
        setTestingText("Nevarro City: Bounty Hunter's Guild");
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextLightSideRequiredAfterTriggers(String playerOnLightSideOfLocation, SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        if (TriggerConditions.isAboutToLeaveTable(game, effectResult, Filters.Bounty_Puck)
                || TriggerConditions.isAboutToBeStolen(game, effectResult, Filters.Bounty_Puck)) {
            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            final AboutToLeaveTableResult result = (AboutToLeaveTableResult) effectResult;
            final PhysicalCard bountyPuck = result.getCardAboutToLeaveTable();

            if (bountyPuck != null) {
                action.setText("Relocate Bounty Puck");
                action.setPerformingPlayer(playerOnLightSideOfLocation);
                action.appendEffect(
                        new PassthruEffect(action) {
                            @Override
                            protected void doPlayEffect(SwccgGame game) {
                                result.getPreventableCardEffect().preventEffectOnCard(bountyPuck);
                                action.appendEffect(
                                        new AttachCardFromTableEffect(action, bountyPuck, self)
                                );
                            }
                        });
                return Collections.singletonList(action);
            }

        }
        return null;
    }

    @Override
    protected List<Modifier> getGameTextDarkSideWhileActiveModifiers(String playerOnDarkSideOfLocation, SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();

        modifiers.add(new DeployCostToLocationModifier(self, Filters.and(Filters.your(playerOnDarkSideOfLocation), Filters.bounty_hunter), -1, self));
        return modifiers;
    }
}