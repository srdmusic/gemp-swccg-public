package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.PlayersTurnCondition;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.IgnoresDeploymentRestrictionsFromCardModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.SuspendsCardModifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Set: Set 20
 * Type: Effect
 * Title: You Cannot Escape Your Destiny
 */
public class Card501_057 extends AbstractNormalEffect {
    public Card501_057() {
        super(Side.LIGHT, 4, PlayCardZoneOption.YOUR_SIDE_OF_TABLE,"You Cannot Escape Your Destiny", Uniqueness.UNIQUE);
        setGameText("If He Is The Chosen One on table, deploy on table. Amidala ignores your objective deployment restrictions. " +
                "During your turn, Emperor’s Power is suspended and may deploy His Destiny or a mobile docking bay from Reserve Deck; reshuffle. " +
                "[Immune to Alter.]");
        addIcons(Icon.VIRTUAL_SET_20);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("You Cannot Escape Your Destiny");
    }

    @Override
    protected boolean checkGameTextDeployRequirements(String playerId, SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.canSpot(game, self, Filters.He_Is_The_Chosen_One);
    }

    @Override
    protected List<Modifier> getGameTextAlwaysOnModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new ArrayList<>();
        modifiers.add(new IgnoresDeploymentRestrictionsFromCardModifier(self, Filters.Amidala,  null, self.getOwner(), Filters.and(Filters.your(self.getOwner()), Filters.Objective)));
        modifiers.add(new SuspendsCardModifier(self, Filters.Emperors_Power, new PlayersTurnCondition(self.getOwner())));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(String playerId, SwccgGame game, PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.YOU_CANNOT_ESCAPE_YOUR_DESTINY__DEPLOY_CARD_FROM_RESERVE_DECK;

        if(GameConditions.isDuringYourPhase(game, playerId, Phase.DEPLOY)
            && GameConditions.isOncePerTurn(game, self, gameTextSourceCardId, gameTextActionId)
            && GameConditions.canDeployCardFromReserveDeck(game, playerId, self, GameTextActionId.OTHER_CARD_ACTION_1)){
            TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerId, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy His Destiny or a Mobile Docking Bay");
            action.setActionMsg("Deploy His Destiny or a Mobile Docking Bay");
            action.appendUsage(
                    new OncePerTurnEffect(action)
            );
            action.appendEffect(
                    new DeployCardFromReserveDeckEffect(action, Filters.or(Filters.title(Title.His_Destiny), Filters.and(Filters.mobile_site, Filters.docking_bay)), true)
            );
            return Collections.singletonList(action);
        }

        return null;
    }
}