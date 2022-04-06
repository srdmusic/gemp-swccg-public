package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.ControlsCondition;
import com.gempukku.swccgo.cards.conditions.OccupiesCondition;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.conditions.OrCondition;
import com.gempukku.swccgo.logic.effects.ActivateForceEffect;
import com.gempukku.swccgo.logic.effects.RetrieveCardEffect;
import com.gempukku.swccgo.logic.modifiers.ForceGenerationModifier;
import com.gempukku.swccgo.logic.modifiers.ImmuneToTitleModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.results.ArtworkCardRevealedResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Set: Set 18
 * Type: Location
 * Subtype: Site
 * Title: Lothal: Imperial Strip Mines
 */
public class Card501_037 extends AbstractSite {
    public Card501_037() {
        super(Side.DARK, "Lothal: Imperial Strip Mines", Title.Lothal);
        setLocationDarkSideGameText("If you just Force drained here, may activate 1 Force (2 Force if your miner present).");
        setLocationLightSideGameText("If you control, Force generation +1 here.");
        addIcon(Icon.DARK_FORCE, 2);
        addIcon(Icon.LIGHT_FORCE, 1);
        addIcons(Icon.EXTERIOR_SITE, Icon.UNDERGROUND, Icon.PLANET, Icon.SCOMP_LINK, Icon.VIRTUAL_SET_18);
        setTestingText("[Set 19] Lothal: Imperial Strip Mines");
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextDarkSideOptionalAfterTriggers(String playerOnDarkSideOfLocation, final SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        // Check condition(s)
        if (TriggerConditions.forceDrainCompleted(game, effectResult, playerOnDarkSideOfLocation, self)
                && GameConditions.canActivateForce(game, playerOnDarkSideOfLocation)) {

            int toActivate = (GameConditions.canSpot(game, self, Filters.and(Filters.your(playerOnDarkSideOfLocation), Filters.miner, Filters.presentAt(Filters.here(self))))?2:1);

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, playerOnDarkSideOfLocation, gameTextSourceCardId);
            action.setText("Activate "+toActivate+" Force");
            action.setActionMsg("Activate "+toActivate+" Force");
            // Perform result(s)
            action.appendEffect(
                    new ActivateForceEffect(action, playerOnDarkSideOfLocation, toActivate));
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<Modifier> getGameTextLightSideWhileActiveModifiers(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new ForceGenerationModifier(self, new ControlsCondition(playerOnLightSideOfLocation, self), 1, playerOnLightSideOfLocation));
        return modifiers;
    }
}
