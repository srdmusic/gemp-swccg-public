package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractSystem;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.FlipCardEffect;
import com.gempukku.swccgo.logic.modifiers.DeployCostToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.PowerModifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 21
 * Type: Location
 * Subtype: System
 * Title: Christophsis
 */
public class Card501_026_BACK extends AbstractSystem {
    public Card501_026_BACK() {
        super(Side.DARK, Title.Christophsis, 6, ExpansionSet.PLAYTESTING, Rarity.V);
        setLocationDarkSideGameText("Dooku, Quinlan Vos, and Ventress deploy -1 (and are power +1) at Christophsis sites.");
        setLocationLightSideGameText("If you just Force drained (or won a battle) at a Christophsis site, 'conquer' (flip) Christophsis.");
        addIcon(Icon.DARK_FORCE, 2);
        addIcon(Icon.LIGHT_FORCE, 1);
        addIcons(Icon.SEPARATIST, Icon.EPISODE_I, Icon.PLANET, Icon.VIRTUAL_SET_21);
        setTestingText("Christophsis (Separatist controlled)");
    }

    @Override
    protected List<Modifier> getGameTextDarkSideWhileActiveModifiers(String playerOnDarkSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new DeployCostToLocationModifier(self, Filters.or(Persona.VENTRESS, Persona.DOOKU, Filters.title("Quinlan Vos")), -1, Filters.Christophsis_site));
        modifiers.add(new PowerModifier(self, Filters.and(Filters.or(Persona.VENTRESS, Persona.DOOKU, Filters.title("Quinlan Vos")), Filters.at(Filters.Christophsis_site)), 1));
        return modifiers;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextLightSideRequiredAfterTriggers(String playerOnLightSideOfLocation, SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        if (GameConditions.canBeFlipped(game, self)
                && (TriggerConditions.forceDrainCompleted(game, effectResult,  playerOnLightSideOfLocation, Filters.Christophsis_site)
                || TriggerConditions.wonBattleAt(game, effectResult, playerOnLightSideOfLocation, Filters.Christophsis_site))) {

            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Conquer Christophsis");
            action.appendEffect(new FlipCardEffect(action, self));
            return Collections.singletonList(action);
        }
        return null;
    }
}