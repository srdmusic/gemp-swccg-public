package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.PlaceCardsInUsedPileFromTableEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardToTargetFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.DrawsBattleDestinyIfUnableToOtherwiseModifier;
import com.gempukku.swccgo.logic.modifiers.ImmuneToAttritionLessThanModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotBeDisarmedModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.PassthruEffect;
import com.gempukku.swccgo.logic.timing.results.AboutToLeaveTableResult;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 23
 * Type: Effect
 * Title: Weapons Are Part Of My Religion
 */
public class Card501_009 extends AbstractNormalEffect {
    public Card501_009() {
        super(Side.LIGHT, 2, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, Title.Weapons_Are_Part_Of_My_Religion, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("Deploy on table. " +
                "While armed, Din draws one battle destiny if unable to otherwise, and is immune to Disarmed and attrition <4. " +
                "Once per turn, may [download] one unique character weapon or device on your Mandalorian; reshuffle. " +
                "If Din about to leave table, your cards on him go to Used Pile. [Immune to Alter.]");
        addIcons(Icon.VIRTUAL_SET_24);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("Weapons Are Part Of My Religion");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        Filter armedDin = Filters.and(Filters.Din, Filters.armedWith(Filters.weapon));
        modifiers.add(new MayNotBeDisarmedModifier(self, Filters.Din));
        modifiers.add(new DrawsBattleDestinyIfUnableToOtherwiseModifier(self, armedDin, 1));
        modifiers.add(new ImmuneToAttritionLessThanModifier(self, armedDin, 4));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<>();
        GameTextActionId gameTextActionId = GameTextActionId.WEAPONS_ARE_PART_OF_MY_RELIGION__DOWNLOAD_WEAPON_OR_DEVICE;
        Filter characterFilter = Filters.and(Filters.your(playerId), Filters.Mandalorian);


        // Check condition(s)
        if (GameConditions.isOncePerTurn(game, self, playerId, gameTextSourceCardId, gameTextActionId)
                && GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId)
                && GameConditions.canSpot(game, self, characterFilter)) {
            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerId, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy a weapon or device from Reserve Deck");
            action.setActionMsg("Deploy one unique character weapon or device on your Mandalorian from Reserve Deck");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerTurnEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new DeployCardToTargetFromReserveDeckEffect(action, Filters.or(Filters.and(Filters.unique, Filters.weapon), Filters.device), characterFilter, true)
            );
            actions.add(action);
        }

        return actions;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, final EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        List<RequiredGameTextTriggerAction> actions = new LinkedList<>();
        final String playerId = self.getOwner();

        // Check condition(s)
        if (TriggerConditions.isAboutToLeaveTable(game, effectResult, Filters.Din)) {
            final AboutToLeaveTableResult aboutToLeaveTableResult = (AboutToLeaveTableResult) effectResult;
            final PhysicalCard cardToBeLost = aboutToLeaveTableResult.getCardAboutToLeaveTable();
            final Collection<PhysicalCard> yourCardsOnDin = Filters.filter(cardToBeLost.getCardsAttached(), game, Filters.your(playerId));
            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setActionMsg("Place cards on " + GameUtils.getCardLink(cardToBeLost) + " in Used Pile");
            // Perform result(s)
            action.appendEffect(
                    new PassthruEffect(action) {
                        @Override
                        protected void doPlayEffect(SwccgGame game) {
                            action.appendEffect(
                                    new PlaceCardsInUsedPileFromTableEffect(action, playerId, yourCardsOnDin)
                            );
                        }
                    });
            actions.add(action);
        }

        return actions;
    }
}
