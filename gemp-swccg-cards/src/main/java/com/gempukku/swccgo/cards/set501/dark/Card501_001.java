package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractAlien;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfGameModifierEffect;
import com.gempukku.swccgo.logic.effects.PutStackedCardInUsedPileEffect;
import com.gempukku.swccgo.logic.effects.RetrieveCardEffect;
import com.gempukku.swccgo.logic.effects.choose.ChooseStackedCardEffect;
import com.gempukku.swccgo.logic.modifiers.DefinedByGameTextDeployCostModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.SpeciesModifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 15
 * Type: Character
 * Subtype: Alien
 * Title: Alien Mob
 */
public class Card501_001 extends AbstractAlien {
    public Card501_001() {
        super(Side.DARK, 3, null, 6, 2, 6, "Alien Mob", Uniqueness.DIAMOND_1);
        setLore("");
        setGameText("* Replaces any 3 of your aliens at same Jabba’s Palace site (aliens go to Used Pile) or deploys for 5 Force. When deployed, may retrieve your Rep OR place your Rep stacked on your objective in Used pile. This alien assumes your Rep's species (if any).");
        addIcons(Icon.WARRIOR, Icon.WARRIOR, Icon.WARRIOR, Icon.VIRTUAL_SET_15);
        setTestingText("Alien Mob");
        setReplacementForSquadron(3, Filters.and(Filters.alien, Filters.at(Filters.Jabbas_Palace_site)));
    }

    @Override
    protected List<Modifier> getGameTextAlwaysOnModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new DefinedByGameTextDeployCostModifier(self, 5));
        return modifiers;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        PhysicalCard rep = game.getGameState().getRep(self.getOwner());

        if (TriggerConditions.justDeployed(game, effectResult, self)
                && rep != null) {
            Species repSpecies = rep.getBlueprint().getSpecies();
            RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.appendEffect(
                    new AddUntilEndOfGameModifierEffect(action, new SpeciesModifier(self, repSpecies), " assumes species: " + repSpecies.getHumanReadable()));
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(final String playerId, final SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        List<OptionalGameTextTriggerAction> actions = new ArrayList<>();
        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;
        PhysicalCard rep = game.getGameState().getRep(self.getOwner());

        // Check condition(s)
        if (TriggerConditions.justDeployed(game, effectResult, self)
                && rep != null) {

            if (GameConditions.hasLostPile(game, playerId)) {
                OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
                action.setText("Retrieve " + GameUtils.getCardLink(rep));
                action.setActionMsg("Retrieve " + GameUtils.getCardLink(rep));
                // Perform result(s)
                action.appendEffect(
                        new RetrieveCardEffect(action, playerId, Filters.sameTitle(rep)));
                actions.add(action);
            }

            if (GameConditions.canSpot(game, self, Filters.and(Filters.your(playerId), Filters.Objective, Filters.hasStacked(Filters.sameTitle(rep))))) {
                final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
                action.setText("Place stacked " + GameUtils.getCardLink(rep) + " in Used Pile");
                action.setActionMsg("Place stacked " + GameUtils.getCardLink(rep) + " in Used Pile");
                // Perform result(s)
                action.appendTargeting(
                        new ChooseStackedCardEffect(action, playerId, Filters.and(Filters.your(playerId), Filters.Objective), Filters.sameTitle(rep)) {
                            @Override
                            protected void cardSelected(PhysicalCard selectedCard) {
                                action.appendEffect(
                                        new PutStackedCardInUsedPileEffect(action, playerId, selectedCard, false));
                            }
                        }
                );
                actions.add(action);
            }
        }
        return actions;
    }
}
