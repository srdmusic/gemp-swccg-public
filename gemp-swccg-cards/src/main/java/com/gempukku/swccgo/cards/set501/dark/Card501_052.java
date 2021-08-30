package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.LoseCardsFromTableEffect;
import com.gempukku.swccgo.logic.effects.LoseForceEffect;
import com.gempukku.swccgo.logic.effects.RelocateBetweenLocationsEffect;
import com.gempukku.swccgo.logic.effects.choose.DrawCardsIntoHandFromForcePileEffect;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 16
 * Type: Effect
 * Title: Unlimited Power!
 */
public class Card501_052 extends AbstractNormalEffect {
    public Card501_052() {
        super(Side.DARK, 1, PlayCardZoneOption.ATTACHED, "Unlimited Power!", Uniqueness.UNIQUE);
        setLore("Eliciting fear from the opponent gives the dark side a powerful advantage.");
        setGameText("Deploy on 500 Republica. Your Emperor, Maul, aliens, and [Independent] starships are lost. At the start of your control phase, if Sidious here, may draw two cards from Force Pile. Once per game, may lose 1 Force to relocate Sidious here. [Immune to Alter.]");
        addIcons(Icon.EPISODE_I, Icon.SIDIOUS, Icon.VIRTUAL_SET_16);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("Unlimited Power!");
    }

    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters._500_Republica;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(String playerId, SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        if (TriggerConditions.isStartOfYourPhase(game, self, effectResult, Phase.CONTROL)
                && GameConditions.isHere(game, self.getAttachedTo(), Filters.Sidious)
                && GameConditions.numCardsInForcePile(game, playerId) >= 2) {
            OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, playerId, gameTextSourceCardId, GameTextActionId.OTHER_CARD_ACTION_1);
            action.setText("Draw two cards from Force Pile");
            action.setActionMsg("Draw two cards from Force Pile");
            action.appendEffect(
                    new DrawCardsIntoHandFromForcePileEffect(action, playerId, 2)
            );
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(String playerId, SwccgGame game, PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.UNLIMITED_POWER__RELOCATE_SIDIOUS;
        Filter sidiousNotHereFilter = Filters.and(Filters.Sidious, Filters.not(Filters.here(self.getAttachedTo())), Filters.canBeRelocatedToLocation(self.getAttachedTo(), 0));

        if (GameConditions.canSpot(game, self, sidiousNotHereFilter)
                && GameConditions.isOncePerGame(game, self, gameTextActionId)) {
            PhysicalCard sidiousNotHere = Filters.findFirstActive(game, self, sidiousNotHereFilter);

            if (sidiousNotHere != null) {
                TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerId, gameTextSourceCardId, gameTextActionId);
                action.setText("Relocate Sidious here");
                action.setActionMsg("Relocate Sidious here");
                action.appendUsage(
                        new OncePerGameEffect(action)
                );
                action.appendCost(
                        new LoseForceEffect(action, playerId, 1)
                );
                action.appendEffect(
                        new RelocateBetweenLocationsEffect(action, sidiousNotHere, self.getAttachedTo())
                );
                return Collections.singletonList(action);
            }
        }
        return null;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {

        // Your Emperor, Maul, aliens, and [Independent] starships are lost.
        List<RequiredGameTextTriggerAction> actions = new LinkedList<>();

        // Check condition(s)
        if (TriggerConditions.isTableChanged(game, effectResult)) {
            Collection<PhysicalCard> emperorMaulAliensAndIndependentStarships =
                    Filters.filterActive(game, self, Filters.and(Filters.your(self.getOwner()), Filters.or(Filters.Emperor, Filters.Maul, Filters.alien, Filters.and(Filters.icon(Icon.INDEPENDENT), Filters.starship))));
            if (!emperorMaulAliensAndIndependentStarships.isEmpty()) {

                final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
                action.setSingletonTrigger(true);
                action.setText("Emperor, Maul, aliens, and [Independent] starships");
                action.setActionMsg("Make " + GameUtils.getAppendedNames(emperorMaulAliensAndIndependentStarships) + " lost");

                // Perform result(s)
                action.appendEffect(
                        new LoseCardsFromTableEffect(action, emperorMaulAliensAndIndependentStarships));
                actions.add(action);
            }
        }
        return actions;
    }
}
