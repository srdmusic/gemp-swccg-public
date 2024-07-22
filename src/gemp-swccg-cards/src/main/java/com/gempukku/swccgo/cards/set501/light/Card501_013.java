package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.AttachCardFromTableEffect;
import com.gempukku.swccgo.logic.effects.LoseForceAndStackFaceUpEffect;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.PassthruEffect;
import com.gempukku.swccgo.logic.timing.results.AboutToLeaveTableResult;

import java.util.Collections;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Location
 * Subtype: Site
 * Title: Nevarro City: Bounty Hunter's Guild
 */
public class Card501_013 extends AbstractSite {
    public Card501_013() {
        super(Side.LIGHT, Title.Bounty_Hunters_Guild, Title.Nevarro, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLocationLightSideGameText("If Holopuck about to leave table (for any reason, even if inactive), relocate it here.");
        setLocationDarkSideGameText("Whenever Holopuck relocates here, lose 1 force and stack it face up on opponent's [Mudhorn] Epic Event.");
        addIcon(Icon.LIGHT_FORCE, 2);
        addIcon(Icon.DARK_FORCE, 1);
        addIcons(Icon.INTERIOR_SITE, Icon.PLANET, Icon.MUDHORN, Icon.VIRTUAL_SET_24);
        addKeyword(Keyword.NEVARRO_CITY_SITE);
        setTestingText("Nevarro City: Bounty Hunter's Guild");
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextLightSideRequiredAfterTriggers(final String playerOnLightSideOfLocation, SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        if (TriggerConditions.isAboutToLeaveTable(game, effectResult, Filters.Holopuck)
                || TriggerConditions.isAboutToBeStolen(game, effectResult, Filters.Holopuck)) {
            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            final AboutToLeaveTableResult result = (AboutToLeaveTableResult) effectResult;
            final PhysicalCard holoPuck = result.getCardAboutToLeaveTable();
            final PhysicalCard mudhornEE = Filters.findFirstActive(game, self, Filters.and(Filters.icon(Icon.MUDHORN), Filters.Epic_Event));

            if (holoPuck != null
                    && mudhornEE != null) {
                action.setText("Relocate " + GameUtils.getCardLink(holoPuck));
                action.setPerformingPlayer(playerOnLightSideOfLocation);
                action.appendEffect(
                        new PassthruEffect(action) {
                            @Override
                            protected void doPlayEffect(SwccgGame game) {
                                result.getPreventableCardEffect().preventEffectOnCard(holoPuck);
                                action.appendEffect(
                                        new AttachCardFromTableEffect(action, holoPuck, self)
                                );
                                action.appendEffect(
                                        new LoseForceAndStackFaceUpEffect(action, game.getOpponent(playerOnLightSideOfLocation), 1, mudhornEE)
                                );
                            }
                        });
                return Collections.singletonList(action);
            }
        }
        return null;
    }
}