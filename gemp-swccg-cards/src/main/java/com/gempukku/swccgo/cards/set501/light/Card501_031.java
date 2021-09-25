package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.*;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.CancelDestinyAndCauseRedrawEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeDestinyCardIntoHandEffect;
import com.gempukku.swccgo.logic.modifiers.ImmuneToAttritionModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Set: Set 17
 * Type: Effect
 * Title: My Parents Were Strong
 */
public class Card501_031 extends AbstractNormalEffect {
    public Card501_031() {
        super(Side.LIGHT, 4, PlayCardZoneOption.ATTACHED, Title.My_Parents_Were_Strong, Uniqueness.UNIQUE);
        setLore("");
        setGameText("Deploy on Rey's Encampment. Once per turn, may deploy Hidden Recess or Jakku from Reserve Deck; reshuffle. If Rey drawn for destiny, may take her into hand to cancel and redraw that destiny. While alone (or with Kylo), Rey is immune to attrition. [Immune to Alter.]");
        addIcons(Icon.EPISODE_VII, Icon.VIRTUAL_SET_17);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("My Parents Were Strong");
    }

    @Override
    protected Filter getValidDeployTargetFilter(String playerId, SwccgGame game, PhysicalCard self, PhysicalCard sourceCard, PlayCardOption playCardOption, boolean forFree, float changeInCost, DeploymentRestrictionsOption deploymentRestrictionsOption, DeployAsCaptiveOption deployAsCaptiveOption, ReactActionOption reactActionOption, boolean isSimDeployAttached, boolean ignorePresenceOrForceIcons) {
        return Filters.Reys_Encampment;
    }

    @Override
    public List<Modifier> getWhileInPlayModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new ArrayList<>();
        modifiers.add(new ImmuneToAttritionModifier(self, Filters.and(Filters.Rey, Filters.or(Filters.alone, Filters.with(self, Filters.Kylo)))));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.MY_PARENTS_WERE_STRONG__DOWNLOAD_LOCATION;

        // Check condition(s)
        if (GameConditions.isOncePerTurn(game, self, gameTextSourceCardId, gameTextActionId)
                && (GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId, Title.Jakku)
                    || GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId, Title.Hidden_Recess))) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy location from Reserve Deck");
            action.setActionMsg("Deploy Hidden Recess or Jakku from Reserve Deck");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerTurnEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new DeployCardFromReserveDeckEffect(action, Filters.or(Filters.Jakku_system, Filters.title(Title.Hidden_Recess)), true));
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(final String playerId, final SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.ANY_CARD__CANCEL_AND_REDRAW_A_DESTINY;

        // Check condition(s)
        if (GameConditions.isDestinyCardMatchTo(game, Filters.Rey)
                && GameConditions.canTakeDestinyCardIntoHand(game, playerId)
                && GameConditions.canCancelDestinyAndCauseRedraw(game, playerId)) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Take into hand and cause re-draw");
            action.setActionMsg("Cancel destiny and cause re-draw");
            // Pay cost(s)
            action.appendEffect(
                    new TakeDestinyCardIntoHandEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new CancelDestinyAndCauseRedrawEffect(action));
            return Collections.singletonList(action);
        }
        return null;
    }
}
