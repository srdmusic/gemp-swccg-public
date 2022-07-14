package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractDevice;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.*;
import com.gempukku.swccgo.logic.modifiers.ExcludedFromBattleModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotBeExcludedFromBattle;
import com.gempukku.swccgo.logic.modifiers.MayNotTargetToBeLostModifier;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Set: Set 19
 * Type: Device
 * Title: Laser Gate (V)
 */
public class Card501_093 extends AbstractDevice {
    public Card501_093() {
        super(Side.DARK, 4, PlayCardZoneOption.ATTACHED, "Laser Gate", Uniqueness.RESTRICTED_2);
        hasVirtualSuffix();
        setLore("Security corridors are guarded by a grid of laser emplacements which can be activated upon demand to seal off sensitive areas from intrusion.");
        setGameText("Deploy on a non-exterior site. Lost if you control this site. If a battle was just initiated here, " +
                "each player targets up to two of their characters here; they cannot be excluded from battle or lost " +
                "before the damage segment. Other characters here excluded from battle.");
        addKeywords(Keyword.DEPLOYS_ON_SITE);
        setTestingText("Laser Gate (V)");
    }

    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.and(Filters.site, Filters.not(Filters.exterior_site));
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(final SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        List<RequiredGameTextTriggerAction> actions = new ArrayList<>();

        String playerId = self.getOwner();
        String opponent = game.getOpponent(playerId);
        PhysicalCard siteAttachedTo = self.getAttachedTo();

        // Check condition(s)
        if (TriggerConditions.isTableChanged(game, effectResult)
                && GameConditions.controls(game, playerId, siteAttachedTo)) {
            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText(GameUtils.getCardLink(self) + " is lost");
            // Perform result(s)
            action.appendEffect(
                    new LoseCardFromTableEffect(action, self));
            actions.add(action);
        }

        if(TriggerConditions.battleInitiatedAt(game, effectResult, siteAttachedTo)){
            final RequiredGameTextTriggerAction yourAction = getAction(game, self, gameTextSourceCardId, playerId, GameTextActionId.OTHER_CARD_ACTION_1);
            actions.add(yourAction);

            final RequiredGameTextTriggerAction opponentsAction = getAction(game, self, gameTextSourceCardId, opponent,  GameTextActionId.OTHER_CARD_ACTION_2);
            actions.add(opponentsAction);
        }

        return actions;
    }

    private RequiredGameTextTriggerAction getAction(final SwccgGame game, final PhysicalCard self, int gameTextSourceCardId, final String playerId, GameTextActionId gameTextActionId){
        final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
        // Perform result(s)
        action.setText(playerId + " chooses target characters");
        action.setActionMsg(playerId + " chooses target characters");
        action.appendTargeting(
                new TargetCardsOnTableEffect(action, playerId, "Target up to two of your characters", 1, 2, Filters.and(Filters.your(playerId), Filters.character, Filters.inBattleWith(self))) {
                    @Override
                    protected void cardsTargeted(int targetGroupId, Collection<PhysicalCard> targetedCards) {
                        Filter cardsAffected = Filters.in(targetedCards);
                        final Collection<PhysicalCard> cardsToExclude = Filters.filterActive(game, self, Filters.and(Filters.your(playerId), Filters.character, Filters.inBattleWith(self), Filters.not(cardsAffected)));

                        action.appendEffect(
                                new AddUntilDamageSegmentOfBattleModifierEffect(action,
                                        new MayNotBeExcludedFromBattle(self, cardsAffected),
                                        "Makes " + GameUtils.getAppendedTextNames(targetedCards) + " not able to be excluded"));
                        action.appendEffect(
                                new AddUntilDamageSegmentOfBattleModifierEffect(action,
                                        new MayNotTargetToBeLostModifier(self, cardsAffected),
                                        "Makes " + GameUtils.getAppendedTextNames(targetedCards) + " not able to be lost"));
                        // Allow response(s)
                        action.allowResponses("Exclude " + GameUtils.getAppendedNames(cardsToExclude) + " from battle",
                                new RespondableEffect(action) {
                                    @Override
                                    protected void performActionResults(Action targetingAction) {
                                        // Perform result(s)
                                        action.appendEffect(
                                                new ExcludeFromBattleEffect(action, cardsToExclude));
                                        action.appendEffect(
                                                new AddUntilEndOfBattleModifierEffect(action,
                                                        new ExcludedFromBattleModifier(self, Filters.in(cardsToExclude)), null));
                                    }
                                }
                        );
                    }
                }
        );
        return action;
    }
}
