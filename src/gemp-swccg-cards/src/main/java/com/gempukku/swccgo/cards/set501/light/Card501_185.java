package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.PlayCardOptionId;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.SpotOverride;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.PlaceCardOutOfPlayFromTableEffect;
import com.gempukku.swccgo.logic.effects.RetrieveCardIntoHandEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardToLocationFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.ResetPersonalForceGenerationModifier;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Effect
 * Title: Seeking An Audience (V)
 */
public class Card501_185 extends AbstractNormalEffect {
    public Card501_185() {
        super(Side.LIGHT, 4, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, Title.Seeking_An_Audience, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setVirtualSuffix(true);
        setLore("'With your wisdom, I'm sure that we can work out an arrangement which will be mutually beneficial and enable us to avoid any unpleasant confrontation.'");
        setGameText("If Han is frozen, deploy on table. Your personal Force generation = 2. Once per turn, may [download] C-3PO, R2-D2, Underworld Contacts, [Jabba's Palace] Lando, or [Jabba's Palace] Leia to a Jabba's Palace site. May place this Effect out of play to retrieve an alien or [Independent] starship into hand. [Immune to Alter.]");
        addIcons(Icon.PREMIUM, Icon.VIRTUAL_SET_1);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("Seeking An Audience (V) (ERRATA)");
    }

    @Override
    protected boolean checkGameTextDeployRequirements(String playerId, SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return GameConditions.canSpot(game, self, SpotOverride.INCLUDE_CAPTIVE,  Filters.and(Filters.Han, Filters.frozenCaptive));
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new ResetPersonalForceGenerationModifier(self, 2, self.getOwner()));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<TopLevelGameTextAction>();

        GameTextActionId gameTextActionId = GameTextActionId.SEEKING_AN_AUDIENCE__DOWNLOAD_NON_REFLECTIONS_III_CARD;

        // Check condition(s)
        if (GameConditions.isOncePerTurn(game, self, playerId, gameTextSourceCardId, gameTextActionId)
                && GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId, new HashSet<Persona>(Arrays.asList(Persona.C3PO, Persona.CHEWIE, Persona.LANDO, Persona.LEIA, Persona.R2D2)), Title.Underworld_Contacts)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy card from Reserve Deck");
            action.setActionMsg("Deploy C-3PO, R2-D2, Underworld Contacts, [Jabba's Palace] Lando, or [Jabba's Palace] Leia to a Jabba's Palace site from Reserve Deck");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerTurnEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new DeployCardToLocationFromReserveDeckEffect(action,
                            Filters.or(Filters.C3PO, Filters.R2D2, Filters.Underworld_Contacts, Filters.and(Icon.JABBAS_PALACE, Filters.Lando), Filters.and(Icon.JABBAS_PALACE, Filters.Leia)),
                            Filters.Jabbas_Palace_site, true));
            actions.add(action);
        }

        gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
        action.setText("Place out of play to retrieve card into hand");
        action.setActionMsg("Retrieve an alien or [Independent] starship into hand");
        // Pay cost(s)
        action.appendCost(
                new PlaceCardOutOfPlayFromTableEffect(action, self));
        // Perform result(s)
        action.appendEffect(
                new RetrieveCardIntoHandEffect(action, playerId, Filters.or(Filters.alien, Filters.and(Icon.INDEPENDENT, Filters.starship))));
        actions.add(action);

        return actions;
    }
}