package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.CancelsGameTextModifier;
import com.gempukku.swccgo.logic.modifiers.ImmuneToAttritionLessThanModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 18
 * Type: Effect
 * Title: Crush The Rebellion (V)
 */
public class Card501_009 extends AbstractNormalEffect {
    public Card501_009() {
        super(Side.DARK, 4, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "Crush The Rebellion", Uniqueness.UNIQUE);
        setVirtualSuffix(true);
        setLore("After dueling his son and seizing control of a city in the clouds, Vader resumed his quest to destroy the Alliance.");
        setGameText("If your Executor (or Scarif) site on table, deploy on table. Once per turn, may deploy Devastator, Mustafar, or a private platform from Reserve Deck; reshuffle. While Devastator at Mustafar or Scarif, it is immune to attrition < 6. [Immune to Alter.]");
        addIcons(Icon.PREMIUM, Icon.VIRTUAL_SET_18);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("Crush The Rebellion (V)");
    }

    @Override
    protected boolean checkGameTextDeployRequirements(String playerId, SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.canSpot(game, self, Filters.and(Filters.your(self), Filters.or(Filters.Executor_site, Filters.Scarif_site)));
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new ImmuneToAttritionLessThanModifier(self, Filters.and(Filters.Devastator, Filters.at(Filters.or(Filters.Mustafar_system, Filters.Scarif_system))), 6));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<TopLevelGameTextAction>();

        GameTextActionId gameTextActionId = GameTextActionId.CRUSH_THE_REBELLION_V__DOWNLOAD_CARD;

        // Check condition(s)
        if (GameConditions.isOncePerTurn(game, self, playerId, gameTextSourceCardId, gameTextActionId)
                && GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy card from Reserve Deck");
            action.setActionMsg("Deploy Devastator, Mustafar, or a private platform from Reserve Deck");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerTurnEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new DeployCardFromReserveDeckEffect(action, Filters.or(Filters.Devastator, Filters.Mustafar_system, Filters.titleContains("Private Platform")), true));
            actions.add(action);
        }

        return actions;
    }
}