package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.PlayCardOptionId;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.RelocateDeviceOrWeaponBetweenCharactersEffect;
import com.gempukku.swccgo.logic.effects.UnrespondableEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.timing.Action;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Effect
 * Title: A Good Friend
 */
public class Card501_173 extends AbstractNormalEffect {
    public Card501_173() {
        super(Side.LIGHT, 6, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "A Good Friend", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("");
        setGameText("If a [Skywalker] Epic Event on table, deploy on table. May [download] Be With Me, Jedi Village, or Leia's Lightsaber. Once per turn, you may relocate Anakin's Lightsaber between Rey and a Skywalker. Once per game, may exchange a Skywalker from hand with Ben Solo in Lost Pile. [Immune to Alter.]");
        addIcons(Icon.EPISODE_VII, Icon.SKYWALKER, Icon.VIRTUAL_SET_25);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("A Good Friend");
    }

    @Override
    protected boolean checkGameTextDeployRequirements(String playerId, SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.canSpot(game, self, Filters.and(Filters.your(self), Icon.SKYWALKER, Filters.Epic_Event));
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<>();

        GameTextActionId gameTextActionId = GameTextActionId.A_GOOD_FRIEND__DEPLOY_JEDI_VILLAGE_OR_BE_WITH_ME;
        if (GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId, Arrays.asList(Title.Be_With_Me, Title.AhchTo_Jedi_Village))) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy card from Reserve Deck");
            action.setActionMsg("Deploy Be With Me or Jedi Village from Reserve Deck");
            // Perform result(s)
            action.appendEffect(
                    new DeployCardFromReserveDeckEffect(action, Filters.or(Filters.Be_With_Me, Filters.AhchTo_Jedi_Village), true));
            actions.add(action);
        }

        GameTextActionId gameTextActionId1 = GameTextActionId.OTHER_CARD_ACTION_1;
        if (GameConditions.isOncePerTurn(game, self, playerId, gameTextSourceCardId, gameTextActionId1)
                && GameConditions.canTarget(game, self, Filters.Ben_Solo)
                && GameConditions.canTarget(game, self, Filters.Rey)
                && GameConditions.canTarget(game, self, Filters.Anakins_Lightsaber)) {

            final PhysicalCard benSolo = Filters.findFirstActive(game, self, Filters.Ben_Solo);
            final PhysicalCard rey = Filters.findFirstActive(game, self, Filters.Rey);
            final PhysicalCard anakinsLightsaber = Filters.findFirstActive(game, self, Filters.Anakins_Lightsaber);

            if (GameConditions.isArmedWith(game, benSolo, Filters.Anakins_Lightsaber)) {
                final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId1);
                action.setText("Relocate Anakin's Lightsaber");
                action.addAnimationGroup(anakinsLightsaber);
                action.appendUsage(
                        new OncePerTurnEffect(action));

                // Allow response(s)
                action.allowResponses("Relocate " + GameUtils.getCardLink(anakinsLightsaber) + " to " + GameUtils.getCardLink(rey),
                        new UnrespondableEffect(action) {
                            @Override
                            protected void performActionResults(Action targetingAction) {
                                // Perform result(s)
                                action.appendEffect(
                                        new RelocateDeviceOrWeaponBetweenCharactersEffect(action, anakinsLightsaber, benSolo, rey));
                            }
                        }
                );
                actions.add(action);
            } else if (GameConditions.isArmedWith(game, rey, Filters.Anakins_Lightsaber)) {
                final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId1);
                action.setText("Relocate Anakin's Lightsaber");
                action.addAnimationGroup(anakinsLightsaber);
                action.appendUsage(
                        new OncePerTurnEffect(action));

                // Allow response(s)
                action.allowResponses("Relocate " + GameUtils.getCardLink(anakinsLightsaber) + " to " + GameUtils.getCardLink(benSolo),
                        new UnrespondableEffect(action) {
                            @Override
                            protected void performActionResults(Action targetingAction) {
                                // Perform result(s)
                                action.appendEffect(
                                        new RelocateDeviceOrWeaponBetweenCharactersEffect(action, anakinsLightsaber, rey, benSolo));
                            }
                        }
                );
                actions.add(action);

            }
        }

        return actions;
    }
}
