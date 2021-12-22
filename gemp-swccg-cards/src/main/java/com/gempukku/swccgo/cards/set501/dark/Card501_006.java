package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.*;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 17
 * Type: Effect
 * Title: Dark Deal (V)
 */
public class Card501_006 extends AbstractNormalEffect {
    public Card501_006() {
        super(Side.DARK, 3, PlayCardZoneOption.ATTACHED, Title.Dark_Deal, Uniqueness.UNIQUE);
        setVirtualSuffix(true);
        setLore("'Perhaps you think you're being treated unfairly?' 'No.' 'Good. It would be unfortunate if I had to leave a garrison here.'");
        setGameText("Deploy on Bespin: Cloud City if you occupy two other Bespin locations (with an alien/Imperial pair at each). Your aliens and TIEs deploy -1 to Bespin locations. Once per game, may take a [Cloud City] Imperial or [Cloud City] TIE into hand from Reserve Deck; reshuffle. [Immune to Alter.]");
        addIcons(Icon.CLOUD_CITY, Icon.VIRTUAL_SET_17);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("[Set 18] Dark Deal (V)");
    }

    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.Bespin_Cloud_City;
    }

    @Override
    protected boolean checkGameTextDeployRequirements(String playerId, SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return GameConditions.occupiesWith(game, self, playerId, 2, Filters.and(Filters.Bespin_location, Filters.not(Filters.Bespin_Cloud_City)),
                Filters.and(Filters.your(self), Filters.alien, Filters.with(self, Filters.and(Filters.your(self), Filters.Imperial))));
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new DeployCostToTargetModifier(self, Filters.and(Filters.your(self), Filters.or(Filters.alien, Filters.TIE)), -1, Filters.Bespin_location));
        return modifiers;
    }


    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<TopLevelGameTextAction>();

        GameTextActionId gameTextActionId = GameTextActionId.DARK_DEAL_V__UPLOAD_IMPERIAL_OR_TIE;

        // Check condition(s)
        if (GameConditions.isOncePerGame(game, self, gameTextActionId)
                && GameConditions.canTakeCardsIntoHandFromReserveDeck(game, playerId, self, gameTextActionId)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Take a card into hand from Reserve Deck");
            action.setActionMsg("Take a [Cloud City] Imperial or [Cloud City] TIE into hand from Reserve Deck");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerGameEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new TakeCardIntoHandFromReserveDeckEffect(action, playerId, Filters.and(Icon.CLOUD_CITY, Filters.or(Filters.Imperial, Filters.TIE)), true));
            actions.add(action);
        }
        return actions;
    }
}