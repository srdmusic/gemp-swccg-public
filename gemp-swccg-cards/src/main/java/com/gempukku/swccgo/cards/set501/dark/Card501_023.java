package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractSystem;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.FlipCardEffect;
import com.gempukku.swccgo.logic.modifiers.ImmuneToAttritionModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 20
 * Type: Location
 * Subtype: System
 * Title: Muunilinst
 */
public class Card501_023 extends AbstractSystem {
    public Card501_023() {
        super(Side.DARK, Title.Muunilinst, 6);
        setFrontOfDoubleSidedCard(true);
        setLocationDarkSideGameText("If you just Force drained (or won a battle) at a Muunilinst site, 'conquer' (flip) Muunilinst.");
        setLocationLightSideGameText("Starships piloted by Anakin or Obi-Wan are immune to attrition here.");
        addIcon(Icon.DARK_FORCE, 2);
        addIcon(Icon.LIGHT_FORCE, 1);
        addIcons(Icon.CLONE_ARMY, Icon.EPISODE_I, Icon.PLANET, Icon.VIRTUAL_SET_20);
        setTestingText("Muunilinst (Republic controlled)");
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextDarkSideRequiredAfterTriggers(String playerOnDarkSideOfLocation, SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        if (GameConditions.canBeFlipped(game, self)
                && (TriggerConditions.forceDrainCompleted(game, effectResult,  playerOnDarkSideOfLocation, Filters.Muunilinst_site)
                || TriggerConditions.wonBattleAt(game, effectResult, playerOnDarkSideOfLocation, Filters.Muunilinst_site))) {

            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Conquer Muunilinst");
            action.appendEffect(new FlipCardEffect(action, self));
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<Modifier> getGameTextLightSideWhileActiveModifiers(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new ImmuneToAttritionModifier(self, Filters.and(Filters.here(self), Filters.starship, Filters.hasPiloting(self, Filters.or(Filters.Anakin, Filters.ObiWan)))));
        return modifiers;
    }
}