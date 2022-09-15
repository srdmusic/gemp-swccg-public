package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractAlien;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.AtCondition;
import com.gempukku.swccgo.cards.conditions.WithCondition;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.ActivateForceEffect;
import com.gempukku.swccgo.logic.effects.LoseCardFromTableEffect;
import com.gempukku.swccgo.logic.modifiers.AddsDestinyToAttritionModifier;
import com.gempukku.swccgo.logic.modifiers.ForceDrainModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 20
 * Type: Character
 * Subtype: Alien
 * Title: Bib Fortuna, Heir to the Empire
 */
public class Card501_001 extends AbstractAlien {
    public Card501_001() {
        super(Side.DARK, 1, 4, 2, 1, 3, "Bib Fortuna, Heir to the Empire", Uniqueness.UNIQUE);
        setLore("Twi'lek leader. Gangster");
        setGameText("Jabba is lost. While with two other aliens, adds a destiny to attrition. While at the Audience Chamber, " +
                    "your Force Drains at other Tatooine battlegrounds are +1 and if opponent just deployed a character here, " +
                    "may activate 1 Force.");
        addPersona(Persona.BIB);
        setSpecies(Species.TWILEK);
        addKeywords(Keyword.LEADER, Keyword.GANGSTER);
        addIcons(Icon.VIRTUAL_SET_20);
        setTestingText("Bib Fortuna, Heir to the Empire");
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        // Check condition(s)
        if (TriggerConditions.isTableChanged(game, effectResult)
                && GameConditions.canSpot(game, self, Filters.Jabba)) {
            PhysicalCard jabba = Filters.findFirstActive(game, self, Filters.Jabba);

            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setSingletonTrigger(true);
            action.setText("Make " + GameUtils.getCardLink(jabba) + " lost");
            action.setActionMsg("Make " + GameUtils.getCardLink(jabba) + " lost");
            // Perform result(s)
            action.appendEffect(
                    new LoseCardFromTableEffect(action, jabba));
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new AddsDestinyToAttritionModifier(self, new WithCondition(self, 2, Filters.and(Filters.your(self), Filters.alien)), 1));
        modifiers.add(new ForceDrainModifier(self, Filters.and(Filters.other(self.getAtLocation()), Filters.Tatooine_battleground_site), new AtCondition(self, Filters.Audience_Chamber), 1, self.getOwner()));
        return modifiers;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(String playerId, SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        if(TriggerConditions.justDeployedToLocation(game, effectResult, game.getOpponent(playerId), Filters.character, Filters.here(self))
            && GameConditions.isAtLocation(game, self, Filters.Audience_Chamber)
            && GameConditions.canActivateForce(game, playerId)){
            OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId);
            action.appendEffect(
                    new ActivateForceEffect(action, playerId, 1)
            );
            return Collections.singletonList(action);
        }
        return null;
    }
}
