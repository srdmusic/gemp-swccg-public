package com.gempukku.swccgo.cards.set501.light;

import java.util.LinkedList;
import java.util.List;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
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
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.RetrieveCardEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.DefenseValueModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

/**
 * Set: Playtesting
 * Type: Effect
 * Title: Medal Ceremony
 */
public class Card501_167 extends AbstractNormalEffect {
    public Card501_167() {
        super(Side.LIGHT, 0, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "Medal Ceremony", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("With the destruction of the Death Star, the Rebel Alliance received new-found support throughout the galaxy.");
        setGameText("If Massassi Throne Room on table, deploy on table. Non-Jedi Rebels are defense value +1. Once per game, may [download] a Yavin 4 battleground. Once per game, may retrieve a non-[Maintenance], non-[Permanent Weapon] Rebel of ability < 5. [Immune to Alter.]");
        addIcons(Icon.A_NEW_HOPE, Icon.VIRTUAL_SET_25);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("Medal Ceremony");
    }

    @Override
    protected boolean checkGameTextDeployRequirements(String playerId, SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.canSpot(game, self, Filters.Massassi_Throne_Room);
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new DefenseValueModifier(self, Filters.and(Filters.not(Filters.Jedi), Filters.Rebel), 1));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<TopLevelGameTextAction>();

        GameTextActionId gameTextActionId = GameTextActionId.MEDAL_CEREMONY__DOWNLOAD_LOCATION;
        // Check condition(s)
        if (GameConditions.isOncePerGame(game, self, gameTextActionId)
                && GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy Yavin 4 battleground from Reserve Deck");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerGameEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new DeployCardFromReserveDeckEffect(action, Filters.and(Filters.Yavin_4_location, Filters.battleground), true));
            actions.add(action);
        }

        gameTextActionId = GameTextActionId.MEDAL_CEREMONY__RETRIEVE_REBEL;

        if (GameConditions.isOncePerGame(game, self, gameTextActionId)){

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Retrieve a Rebel");
            action.setActionMsg("Retrieve a non-[Maintenance], non-[Permanent Weapon] Rebel of ability < 5");
            // Update usage limit(s)
            action.appendUsage(
                new OncePerGameEffect(action));
            action.appendEffect(
                    new RetrieveCardEffect(action, playerId, Filters.and(Filters.not(Icon.MAINTENANCE), Filters.not(Icon.PERMANENT_WEAPON), Filters.Rebel, Filters.abilityLessThan(5))));
            actions.add(action);
        }
        return actions;
    }
}
